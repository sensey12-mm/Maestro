#!/usr/bin/env python3
"""Synthetic and adversarial offline diagnostics regressions; NOT Minecraft."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / "tools" / "battle_diagnostics.py"
spec = importlib.util.spec_from_file_location("battle_diagnostics", MODULE)
diagnostics = importlib.util.module_from_spec(spec)
spec.loader.exec_module(diagnostics)

BATTLE = "20000000-0000-0000-0000-000000000001"


def replay(capture, seq, event):
    return (f"[12:00:00] [main/INFO] [MaestroAI] "
            f"SHADOW_REPLAY battle=REDACTED capture={capture} seq={seq} event={event}")


def envelope(capture, seq, route="DIRECT_UPDATE"):
    return (f"[MaestroAI] SHADOW_ENVELOPE capture={capture} seq={seq} route={route} "
            "split_target=NONE spectator=EXPECTED_NOT_OBSERVED branch_attested=true")


def decision(capture, turn, authority="SHADOW", action="move:icebeam",
             forced="false", reason="MOVE", stage="UNKNOWN",
             fallback_reason="UNKNOWN", legal="move:icebeam,move:uturn"):
    return (f"[MaestroAI] DEV_DIAG_DECISION capture={capture};turn={turn}"
            f";forced={forced};authority={authority};action={action}"
            f";proposal={action};reason={reason};fallback_stage={stage}"
            f";fallback_reason={fallback_reason};legal={legal}"
            ";confidence=UNKNOWN;custody=REVIEW_REQUIRED")


def terminal(capture):
    return (f"[MaestroAI] SHADOW_TERMINAL capture={capture} after=3 "
            "event=COBBLEMON_BATTLE_VICTORY winner=REDACTED "
            "beginning=UNKNOWN delivery=UNVERIFIED coverage=START_UNKNOWN")


def read_jsonl(file):
    return [json.loads(line) for line in file.read_text(encoding="utf-8").splitlines()]


class DeveloperBattleDiagnosticsRegression(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.log = self.root / "latest.log"
        self.out = self.root / "battle-reports"

    def runlog(self, *lines):
        self.log.write_text("\n".join(lines) + "\n", encoding="utf-8")
        battles, issues, sha = diagnostics.process(self.log, self.out)
        return battles, issues, sha

    def rows(self, battle="B01"):
        return read_jsonl(self.out / battle / "battle.jsonl")

    def issues(self):
        return read_jsonl(self.out / "issues.jsonl")

    def test_ordinary_shadow_decision_and_terminal(self):
        count, issues, sha = self.runlog(
            "[MaestroAI] Installed artifact sha256=" + "a" * 64 + " runtimeTransformation=UNATTESTED",
            replay(1, 1, "|turn|1"),
            replay(1, 2, "|switch|p2a: Opponent secret|Zoroark, L100|100/100"),
            envelope(1, 2),
            decision(1, 1),
            terminal(1))
        self.assertEqual(count, 1)
        self.assertEqual(issues, 0)
        rows = self.rows()
        own = next(r for r in rows if r["kind"] == "decision")
        self.assertEqual(own["selected_action"], "move:icebeam")
        self.assertEqual(own["legal_choices"], ["move:icebeam", "move:uturn"])
        self.assertEqual(own["authority"], "SHADOW")
        self.assertEqual(own["confidence"], "UNKNOWN")
        self.assertTrue(any(r["kind"] == "provenance" for r in rows))
        self.assertTrue(any(r["kind"] == "terminal" for r in rows))
        self.assertEqual(rows[-1]["result"], "UNKNOWN")
        self.assertEqual(rows[-1]["artifact_sha256"], "a" * 64)
        self.assertEqual(rows[-1]["source_sha256"], sha)
        self.assertNotIn("Opponent secret", (self.out / "B01/battle.jsonl").read_text())

    def test_forced_legacy_emergency_and_reason(self):
        self.runlog(replay(2, 1, "|turn|3"),
                    decision(2, 3, "LEGACY", "switch:slot2", "true",
                             "UNKNOWN", "NO_NATIVE_FORCED_PROPOSAL",
                             "STALE_OWN_LEASE", "UNKNOWN"),
                    terminal(2))
        rows = self.rows()
        item = next(r for r in rows if r["kind"] == "decision")
        self.assertEqual(item["authority"], "LEGACY")
        self.assertEqual(item["forced"], "true")
        self.assertEqual(item["fallback_stage"], "NO_NATIVE_FORCED_PROPOSAL")
        self.assertEqual(item["fallback_reason"], "STALE_OWN_LEASE")
        self.assertEqual(item["legal_choices"], "UNKNOWN")
        self.assertIn("Legacy 1", (self.out / "B01/summary.md").read_text())

    def test_illusion_transform_tera_reentry_are_appearance_only(self):
        self.runlog(
            replay(3, 1, "|switch|p2a: Masked Player|Greninja, L100|100/100"),
            replay(3, 2, "|replace|p2a: Masked Player|Zoroark, L100"),
            replay(3, 3, "|-transform|p2a: Masked Player|p1a: Own"),
            replay(3, 4, "|-terastallize|p2a: Masked Player|Ghost"),
            replay(3, 5, "|switch|p2a: Masked Player|Ditto, L100|100/100"),
            replay(3, 6, "|detailschange|p2a: Masked Player|Ditto, L100"),
            terminal(3))
        identity = [r for r in self.rows() if r["kind"] == "identity"]
        self.assertEqual(len(identity), 3)
        self.assertTrue(all(r["uncertainty"] == "UNCERTAIN" for r in identity))
        self.assertEqual(
            next(r for r in self.rows() if r.get("event") == "-transform")["true_identity"],
            "UNKNOWN")
        text = (self.out / "B01/battle.jsonl").read_text()
        self.assertNotIn("Masked Player", text)
        self.assertNotIn("100/100", text)

    def test_multiple_battles_stable_correlation(self):
        count, issues, _ = self.runlog(
            replay(20, 1, "|turn|1"), replay(21, 1, "|turn|1"),
            decision(20, 1), decision(21, 1, "LEGACY", "move:uturn"),
            terminal(21), terminal(20))
        self.assertEqual(count, 2)
        self.assertEqual(issues, 0)
        self.assertEqual(next(r for r in self.rows("B01") if r["kind"] == "decision")["authority"], "SHADOW")
        self.assertEqual(next(r for r in self.rows("B02") if r["kind"] == "decision")["authority"], "LEGACY")

    def test_older_uuid_replay_correlates_with_envelope(self):
        self.runlog(f"[MaestroAI] SHADOW_REPLAY battle={BATTLE} seq=1 event=|turn|1",
                    envelope(7, 1), terminal(7))
        self.assertTrue(any(r["kind"] == "provenance" for r in self.rows()))
        self.assertEqual(len(list(self.out.glob("B*"))), 1)
        self.assertNotIn(BATTLE, (self.out / "B01/battle.jsonl").read_text())

    def test_duplicate_events_do_not_double_count(self):
        self.runlog(replay(1, 1, "|turn|1"), replay(1, 1, "|turn|1"),
                    decision(1, 1), decision(1, 1), terminal(1))
        rows = self.rows()
        self.assertEqual(len([r for r in rows if r["kind"] == "public_event"]), 1)
        self.assertEqual(len([r for r in rows if r["kind"] == "decision"]), 1)

    def test_conflicting_sequence_and_decision_detected(self):
        self.runlog(replay(1, 1, "|turn|1"), replay(1, 1, "|turn|2"),
                    decision(1, 1), decision(1, 1, action="move:uturn"),
                    terminal(1))
        self.assertGreaterEqual([i["code"] for i in self.issues()].count("SEQUENCE_CONFLICT"), 2)

    def test_missing_turn_and_terminal_fail_closed(self):
        self.runlog(replay(1, 1, "|turn|1"),
                    replay(1, 2, "|turn|3"),
                    replay(1, 4, "|move|p1a: Own|Ice Beam|p2a: Opponent"))
        codes = [row["code"] for row in self.issues()]
        self.assertIn("GAP", codes)
        self.assertIn("MISSING_SEQUENCE", codes)
        self.assertIn("MISSING_TERMINAL", codes)
        self.assertEqual(self.rows()[-1]["completeness"], "INCOMPLETE")

    def test_unverified_private_fields_never_pass_through(self):
        private = "PRIVATE_USERNAME_SECRET_TOKEN"
        self.runlog(
            replay(1, 1, f"|move|p2a: {private}|{private}|p1a: {private}"),
            replay(1, 2, f"|-item|p2a: {private}|{private}"),
            replay(1, 3, f"|-damage|p2a: {private}|3/100"),
            replay(1, 4, f"|-ability|p2a: {private}|{private}"),
            f"[MaestroAI] Phase 6.22 source=/home/{private}/secret.json",
            f"java.lang.RuntimeException: /home/{private}/password",
            terminal(1))
        out = "".join(path.read_text(encoding="utf-8")
                      for path in self.out.rglob("*") if path.is_file())
        self.assertNotIn(private, out)
        self.assertNotIn("3/100", out)
        self.assertNotIn("/home/", out)
        self.assertIn("ERROR_OBSERVED", out)
        self.assertIn("UNKNOWN", out)

    def test_invalid_adversarial_action_rejected(self):
        self.runlog(replay(1, 1, "|turn|1"),
                    decision(1, 1, action="move:PRIVATE_USERNAME", legal="move:PRIVATE_USERNAME"),
                    terminal(1))
        rec = next(r for r in self.rows() if r["kind"] == "decision")
        self.assertEqual(rec["selected_action"], "UNKNOWN")
        self.assertEqual(rec["legal_choices"], "UNKNOWN")

    def test_malformed_replay_is_reported_and_no_leak(self):
        self.runlog(replay(1, 1, "|turn|1"),
                    replay(1, 2, "|request|privateUser|item:secret"),
                    terminal(1))
        self.assertTrue(any(i["code"] == "MALFORMED_RECORD" for i in self.issues()))
        self.assertNotIn("privateUser", (self.out / "B01/battle.jsonl").read_text())

    def test_provenance_failure_retained(self):
        self.runlog(replay(1, 1, "|turn|1"),
                    "[MaestroAI] SHADOW_ENVELOPE_FAILURE capture=1 reason=NON_APPEND_ONLY_HISTORY status=UNKNOWN")
        self.assertIn("PROVENANCE_FAILURE", [i["code"] for i in self.issues()])
        self.assertEqual(self.rows()[-1]["completeness"], "INCOMPLETE")

    def test_unsourced_error_and_empty_log(self):
        self.runlog("Exception private/path: DO_NOT_COPY")
        self.assertEqual(len(list(self.out.glob("B*"))), 0)
        self.assertIn("ERROR_OBSERVED", [i["code"] for i in self.issues()])
        self.assertIn("MISSING_SEQUENCE", [i["code"] for i in self.issues()])

    def test_determinism_and_no_duplicate_folders(self):
        fixture = [replay(1, 1, "|turn|1"), decision(1, 1), terminal(1)]
        self.runlog(*fixture)
        before = {p.relative_to(self.out): p.read_bytes()
                  for p in self.out.rglob("*") if p.is_file()}
        self.runlog(*fixture)
        after = {p.relative_to(self.out): p.read_bytes()
                 for p in self.out.rglob("*") if p.is_file()}
        self.assertEqual(before, after)

    def test_refuse_unrelated_output(self):
        self.out.mkdir()
        (self.out / "dont-delete.txt").write_text("important")
        self.log.write_text(replay(1, 1, "|turn|1"))
        with self.assertRaisesRegex(ValueError, "unrelated"):
            diagnostics.process(self.log, self.out)
        self.assertTrue((self.out / "dont-delete.txt").is_file())

    def test_privacy_projections_and_fail_closed_catalog(self):
        self.assertIsNone(diagnostics.public_projection("|request|hidden"))
        self.assertEqual(diagnostics.public_projection(
            "|switch|p2a: nickname|UnknownPlayerName, L100|100/100"
        )["displayed_species"], "UNKNOWN")
        self.assertEqual(diagnostics.public_projection(
            "|move|p2a: nickname|Ice Beam|p1a: Own"
        )["revealed_move"], "icebeam")
        self.assertEqual(diagnostics.public_projection(
            "|-terastallize|p2a: nickname|Ghost")["public_tera_type"], "ghost")

    def test_large_stream_and_ordering(self):
        lines = [replay(4, 1, "|turn|1")]
        lines += [replay(4, i + 2, "|-damage|p2a: Enemy|1/100")
                  for i in range(2500)]
        lines.append(terminal(4))
        self.runlog(*lines)
        seq = [r["order"] for r in self.rows()]
        self.assertEqual(seq, list(range(1, len(seq) + 1)))
        self.assertEqual(len([r for r in self.rows() if r["kind"] == "public_event"]), 2501)


if __name__ == "__main__":
    unittest.main(verbosity=2)
