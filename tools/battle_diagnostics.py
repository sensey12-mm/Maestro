#!/usr/bin/env python3
"""Offline, local, fail-closed developer view of approved MaestroAI log surfaces.

This is NOT a custody/publication tool. It does not reconstruct private packets,
prove spectator delivery, or infer a winner. Original logs remain authoritative.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import tempfile

SCHEMA = "maestro.developer-battle.v1"
UNKNOWN = "UNKNOWN"
UUID = re.compile(r"^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
HASH = re.compile(r"^[a-f0-9]{64}$")
NUM = re.compile(r"^[0-9]{1,6}$")
CAPTURE = re.compile(r"^[0-9]{1,16}$")
ACTION = re.compile(r"^(?:move:[a-z0-9_-]{1,64}|switch:slot[1-6])$")
REPLAY = re.compile(
    r"SHADOW_REPLAY battle=([^\s]+) (?:capture=([0-9]+) )?seq=([0-9]+) event=(\|.*)$"
)
ACTOR = re.compile(r"^(p[12][a-f])(?::.*)?$")
SAFE_ID = re.compile(r"^[a-z0-9_-]{1,48}$")
PUBLIC_KINDS = frozenset("""
turn move switch drag replace detailschange -formechange -mega faint
-boost -unboost -setboost -clearboost -clearallboost -copyboost -swapboost
-invertboost -item -enditem -ability -endability -status -curestatus
-transform -start -end -activate -terastallize -damage -heal -sethp
-sidestart -sideend -swapsideconditions
""".split())
MOVE_NAMES = frozenset("""
uturn ironhead icebeam flamethrower astralbarrage psyshock drainingkiss
energyball geomancy moonblast focusblast thunder swordsdance extremespeed
shadowclaw recover psystrike fireblast nastyplot spectralthief closecombat
shadowsneak rocktomb tackle transform protect substitute toxic thunderbolt
thunderwave surf earthquake quickattack psychic willowisp encore splash
hydropump darkpulse knockoff stealthrock spikes toxicspikes defog rapidspin
roost suckerpunch pursuit voltswitch taunt rest sleeptalk dragonclaw
shadowball trickroom calmmind leechseed seedbomb swift psychocut
""".split())
SPECIES = frozenset("""
zoroark zoroarkhisui ditto mew pikachu blissey shedinja shuckle genesect
calyrexshadow calyrexshadowrider arceus xerneas marshadow mewtwo mewtwomegay
greninja charizard charizardmegax charizardmegay gengar gengarmega
rayquaza zacian zamazenta kyogre groudon dialga palkia giratina
lucario dragapult gholdengo
""".split())
ITEMS = frozenset("""
leftovers choicescarf choicespecs choiceband lifeorb powerherb sitrusberry
focussash assaultvest blacksludge heavydutyboots mewtwonitey marshadiumz
""".split())
ABILITIES = frozenset("""
pressure download shadowshield fairy aura multitype technician
asone spectrier asonespectrier levitate intimidate prankster
""".replace("fairy aura", "fairyaura").split())
STATES = frozenset("""
brn psn tox par slp frz none atk def spa spd spe accuracy evasion
""".split())
FIELDS = frozenset("""
rain sandstorm hail snow sun sunnyday electricterrain psychicterrain
mistyterrain grassyterrain trickroom stealthrock spikes toxicspikes
reflect lightscreen auroraveil tailwind safeguard
""".split())
ISSUE_SEVERITY = {"GAP": "HIGH", "SEQUENCE_CONFLICT": "HIGH",
                  "PROVENANCE_FAILURE": "HIGH", "MALFORMED_RECORD": "MEDIUM",
                  "MISSING_TERMINAL": "MEDIUM", "UNKNOWN_EVENT_FIELD": "LOW",
                  "UNSUPPORTED_DIAGNOSTIC": "LOW", "ERROR_OBSERVED": "HIGH",
                  "AMBIGUOUS_CORRELATION": "HIGH", "MISSING_SEQUENCE": "MEDIUM",
                  "INVALID_DECISION": "HIGH"}


def digest_file(path: Path) -> str:
    sha = hashlib.sha256()
    with path.open("rb") as src:
        for block in iter(lambda: src.read(1024 * 1024), b""):
            sha.update(block)
    return sha.hexdigest()


def compact(raw: str) -> str:
    return re.sub(r"[^a-z0-9]", "", raw.casefold())


def safe_enum(raw: str, accepted: frozenset[str]) -> str:
    value = compact(raw)
    return value if value in accepted else UNKNOWN


def safe_actor(raw: str) -> str:
    found = ACTOR.fullmatch(raw.strip())
    return found.group(1) if found else UNKNOWN


def safe_number(raw: str, upper: int = 10000):
    return int(raw) if NUM.fullmatch(raw) and int(raw) <= upper else UNKNOWN


def allowed_action(value: str) -> bool:
    return bool(ACTION.fullmatch(value)) and (
        not value.startswith("move:") or value[5:] in MOVE_NAMES
    )


def public_projection(source: str) -> dict | None:
    """Never persist raw Showdown data, nickname, HP, UUID or free-form text."""
    parts = source.split("|")
    if len(parts) < 3 or parts[0] != "" or parts[1] not in PUBLIC_KINDS:
        return None
    kind = parts[1]
    args = parts[2:]
    result = {"event": kind}
    if kind == "turn":
        result["turn"] = safe_number(args[0], 10000)
        return result
    if kind in {"-sidestart", "-sideend", "-swapsideconditions"}:
        result["side"] = args[0][:2] if re.match(r"^p[12](?:\b|:)", args[0]) else UNKNOWN
        if len(args) > 1:
            result["condition"] = safe_enum(args[1], FIELDS)
        return result
    if kind in {"-clearallboost", "-weather", "-fieldstart", "-fieldend"}:
        return result
    result["actor"] = safe_actor(args[0])
    if kind in {"switch", "drag", "replace", "detailschange", "-formechange"}:
        result["displayed_species"] = safe_enum(args[1].split(",")[0], SPECIES) if len(args) > 1 else UNKNOWN
        result["identity_status"] = "UNCERTAIN" if kind in {"replace", "-formechange"} else "DISPLAY_ONLY"
    elif kind == "move":
        result["revealed_move"] = safe_enum(args[1], MOVE_NAMES) if len(args) > 1 else UNKNOWN
        result["target"] = safe_actor(args[2]) if len(args) > 2 else UNKNOWN
    elif kind in {"-item", "-enditem"}:
        result["revealed_item"] = safe_enum(args[1], ITEMS) if len(args) > 1 else UNKNOWN
    elif kind in {"-ability", "-endability"}:
        result["revealed_ability"] = safe_enum(args[1], ABILITIES) if len(args) > 1 else UNKNOWN
    elif kind in {"-boost", "-unboost", "-setboost"}:
        result["stat"] = safe_enum(args[1], STATES) if len(args) > 1 else UNKNOWN
        result["stages"] = safe_number(args[2], 6) if len(args) > 2 else UNKNOWN
    elif kind in {"-status", "-curestatus"}:
        result["status"] = safe_enum(args[1], STATES) if len(args) > 1 else UNKNOWN
    elif kind == "-terastallize":
        result["public_tera_type"] = safe_enum(args[1], frozenset(
            "normal fire water electric grass ice fighting poison ground flying psychic bug rock ghost dragon dark steel fairy stellar".split()
        )) if len(args) > 1 else UNKNOWN
    elif kind == "-transform":
        result["target"] = safe_actor(args[1]) if len(args) > 1 else UNKNOWN
        result["true_identity"] = UNKNOWN
    elif kind in {"-damage", "-heal", "-sethp"}:
        result["hp"] = UNKNOWN  # Never project percentages or private exact HP.
    elif kind == "-mega":
        result["form"] = "MEGA_OBSERVED"
    elif kind in {"-start", "-end", "-activate"}:
        result["effect"] = UNKNOWN  # Free-form move/effect strings are not safe.
    return result


def kv_fields(text: str) -> dict[str, str]:
    result = {}
    for part in text.split(";"):
        if "=" not in part:
            continue
        key, val = part.split("=", 1)
        if re.fullmatch(r"[a-z_]{1,32}", key) and len(val) <= 256:
            result[key] = val
    return result


def inline_fields(text: str) -> dict[str, str]:
    result = {}
    for key, val in re.findall(r"([a-z_]+)=([^\s]+)", text):
        if len(val) <= 200:
            result[key] = val
    return result


class Session:
    def __init__(self, key: str, index: int, first_line: int, spool: Path, source_hash: str, version: str):
        self.key, self.index, self.first_line = key, index, first_line
        self.name = f"B{index:02d}"
        self.stream = spool.open("w", encoding="utf-8", newline="\n")
        self.source_hash, self.reporter_version = source_hash, version
        self.version = UNKNOWN  # Runtime version must be evidenced by the log itself.
        self.order = 0
        self.last_event_seq = 0
        self.event_fingerprints = {}
        self.event_overflow = False
        self.decision_keys = {}
        self.turn = UNKNOWN
        self.last_turn = 0
        self.terminal = False
        self.gapped = False
        self.events = 0
        self.decisions = 0
        self.shadow = 0
        self.legacy = 0
        self.issues = []
        self.notable = []
        self.artifact = UNKNOWN
        self.observed = {}
        self.capture = UNKNOWN

    def write(self, kind: str, line: int, **fields):
        self.order += 1
        rec = {"schema": SCHEMA, "kind": kind, "battle": self.name,
               "order": self.order, "source_line": line,
               "source_sha256": self.source_hash, "turn": self.turn,
               "artifact_sha256": self.artifact, "version": self.version,
               "reporter_version": self.reporter_version}
        rec.update(fields)
        self.stream.write(json.dumps(rec, sort_keys=True, separators=(",", ":"), ensure_ascii=True) + "\n")

    def issue(self, code: str, line: int, detail: str = UNKNOWN):
        if code in {"GAP", "SEQUENCE_CONFLICT", "MISSING_SEQUENCE", "PROVENANCE_FAILURE"}:
            self.gapped = True
        key = f"{self.source_hash}:{self.name}:{line}:{code}:{detail}"
        issue_id = hashlib.sha256(key.encode()).hexdigest()[:20]
        record = {"schema": SCHEMA, "issue": issue_id, "battle": self.name,
                  "source_line": line, "code": code,
                  "severity": ISSUE_SEVERITY.get(code, "MEDIUM"), "detail": detail}
        self.issues.append(record)
        self.write("issue", line, code=code, issue=issue_id, detail=detail)

    def finish(self, target: Path, end_line: int):
        if not self.terminal:
            self.issue("MISSING_TERMINAL", end_line, "No attested terminal callback")
        completeness = "INCOMPLETE" if self.gapped else UNKNOWN
        self.write("metadata", end_line, result=UNKNOWN, completeness=completeness,
                   terminal="OBSERVED" if self.terminal else UNKNOWN,
                   spectator_delivery="UNVERIFIED", capture=self.capture,
                   public_events=self.events, decisions=self.decisions,
                   legacy=self.legacy, shadow=self.shadow)
        self.stream.close()
        text = [
            f"# {self.name} — Developer battle diagnostics",
            "",
            f"- Outcome: **UNKNOWN** (winner is not exposed by approved capture)",
            f"- Completeness: **{completeness}**; terminal: {'OBSERVED' if self.terminal else 'UNKNOWN'}",
            f"- Public records: {self.events}; decisions: {self.decisions}",
            f"- Decision authority: Shadow {self.shadow}, Legacy {self.legacy}",
            f"- Significant issues: {len(self.issues)}",
            f"- Last observed turn: {self.turn}",
            f"- Artifact SHA-256: {self.artifact}",
            f"- Observed runtime version: {self.version}",
            f"- Reporter version: {self.reporter_version}",
            f"- Original source SHA-256: {self.source_hash}",
            "- Missing evidence: start-of-battle custody and independent spectator delivery unverified",
            "- Decision candidates/reasoning absent from older logs are UNKNOWN",
            "- Source: original local log (not copied into this report)",
            "",
            "## Issues",
            "",
        ]
        if self.issues:
            for i in self.issues[:40]:
                text.append(f"- {i['code']} at source line {i['source_line']} ({i['detail']})")
            if len(self.issues) > 40:
                text.append(f"- Additional {len(self.issues) - 40} issues in global issues.jsonl")
        else:
            text.append("- No classified issues observed; this does not prove absence of bugs")
        text.extend(["", "## Significant decisions", ""])
        if self.notable:
            for turn, authority, action, category, stage in self.notable:
                text.append(f"- Turn {turn}: {authority} selected {action}; category={category}; fallback={stage}")
        else:
            text.append("- Selected actions and reasoning UNKNOWN in this capture")
        (target / "summary.md").write_text("\n".join(text) + "\n", encoding="utf-8")


class Processor:
    def __init__(self, source_hash: str, staging: Path, version: str):
        self.source_hash, self.staging, self.version = source_hash, staging, version
        self.sessions = []
        self.by_key = {}
        self.by_capture = {}
        self.global_issues = []
        self.last_session = None
        self.last_line = 0
        self.artifact = UNKNOWN

    def get(self, key: str, line: int):
        if key not in self.by_key:
            index = len(self.sessions) + 1
            directory = self.staging / f"B{index:02d}"
            directory.mkdir()
            session = Session(key, index, line, directory / "battle.jsonl", self.source_hash, self.version)
            session.artifact = self.artifact
            self.sessions.append(session)
            self.by_key[key] = session
        self.last_session = self.by_key[key]
        return self.last_session

    def global_issue(self, code: str, line: int, detail: str = UNKNOWN):
        issue_id = hashlib.sha256(f"{self.source_hash}:GLOBAL:{line}:{code}".encode()).hexdigest()[:20]
        self.global_issues.append({"schema": SCHEMA, "issue": issue_id,
                                   "battle": UNKNOWN, "source_line": line,
                                   "code": code, "severity": "HIGH", "detail": detail})

    def capture_session(self, capture: str, line: int):
        if not CAPTURE.fullmatch(capture):
            self.global_issue("AMBIGUOUS_CORRELATION", line)
            return None
        if capture in self.by_capture:
            return self.by_capture[capture]
        session = self.get("capture:" + capture, line)
        session.capture = capture
        self.by_capture[capture] = session
        return session

    def unique_open(self, line: int):
        active = [s for s in self.sessions if not s.terminal]
        if len(active) == 1:
            return active[0]
        self.global_issue("AMBIGUOUS_CORRELATION", line)
        return None

    def handle(self, line: str, number: int):
        self.last_line = number
        if "[MaestroAI]" not in line:
            if re.search(r"\b(?:\w*Exception|ERROR|FATAL|StackOverflowError|OutOfMemoryError)\b", line):
                session = self.unique_open(number) if self.sessions else None
                if session:
                    session.issue("ERROR_OBSERVED", number, "Source error; details withheld")
                else:
                    self.global_issue("ERROR_OBSERVED", number, "Source error; details withheld")
            return
        # Only Maestro-owned records are eligible. NEVER copy raw log content.
        body = line.split("[MaestroAI]", 1)[1].strip()
        if body.startswith("Installed artifact sha256="):
            value = body.split("sha256=", 1)[1].split()[0]
            if HASH.fullmatch(value):
                self.artifact = value
            return
        matched = REPLAY.fullmatch(body)
        if matched:
            battle, capture, seq_string, event = matched.groups()
            if not (UUID.fullmatch(battle) or battle == "REDACTED" and capture) or not NUM.fullmatch(seq_string):
                self.global_issue("MALFORMED_RECORD", number, "Invalid replay identity or sequence")
                return
            identity = ("capture:" + capture) if capture else "battle:" + battle.casefold()
            session = self.capture_session(capture, number) if capture else self.get(identity, number)
            if session is None:
                return
            seq = int(seq_string)
            projection = public_projection(event)
            if projection is None:
                session.issue("MALFORMED_RECORD", number, "Replay event rejected by public schema")
                return
            fingerprint = hashlib.sha256(event.encode("utf-8", "replace")).hexdigest()
            if seq in session.event_fingerprints:
                if session.event_fingerprints[seq] != fingerprint:
                    session.issue("SEQUENCE_CONFLICT", number, "Repeated sequence with different data")
                return
            if len(session.event_fingerprints) >= 100000:
                if not session.event_overflow:
                    session.issue("GAP", number, "Public evidence exceeded bounded per-battle limit")
                    session.event_overflow = True
                return
            if seq != session.last_event_seq + 1:
                session.issue("MISSING_SEQUENCE", number, "Noncontiguous public sequence")
            session.event_fingerprints[seq] = fingerprint
            session.last_event_seq = max(seq, session.last_event_seq)
            if projection["event"] == "turn":
                new_turn = projection["turn"]
                if new_turn == UNKNOWN or isinstance(session.turn, int) and new_turn <= session.turn:
                    session.issue("SEQUENCE_CONFLICT", number, "Invalid or repeated turn")
                elif isinstance(new_turn, int):
                    if session.last_turn and new_turn != session.last_turn + 1:
                        session.issue("GAP", number, "Turn number gap")
                    session.turn = new_turn
                    session.last_turn = new_turn
            if UNKNOWN in projection.values():
                session.issue("UNKNOWN_EVENT_FIELD", number, "Unreviewed or unsupported public field")
            if projection["event"] in {"replace", "-transform", "-terastallize", "-formechange"}:
                session.write("identity", number, evidence="PUBLIC_APPEARANCE_ONLY",
                              true_identity=UNKNOWN,
                              uncertainty="UNCERTAIN", event=projection["event"])
            session.write("public_event", number, event_sequence=seq, **projection)
            session.events += 1
            return
        if body.startswith("DEV_DIAG_DECISION "):
            data = kv_fields(body[len("DEV_DIAG_DECISION "):])
            capture = data.get("capture", "")
            session = self.capture_session(capture, number)
            if session is None:
                return
            authority = data.get("authority", UNKNOWN)
            if authority not in {"SHADOW", "LEGACY"}:
                session.issue("INVALID_DECISION", number, "Unknown decision authority")
                authority = UNKNOWN
            action = data.get("action", UNKNOWN)
            if not allowed_action(action):
                action = UNKNOWN
            proposal = data.get("proposal", UNKNOWN)
            if not allowed_action(proposal):
                proposal = UNKNOWN
            forced = data.get("forced") if data.get("forced") in {"true", "false"} else UNKNOWN
            reason = data.get("reason", UNKNOWN)
            if not re.fullmatch(r"[A-Z_]{2,64}", reason):
                reason = UNKNOWN
            stage = data.get("fallback_stage", UNKNOWN)
            if not re.fullmatch(r"[A-Z_]{2,80}", stage):
                stage = UNKNOWN
            legal = data.get("legal", UNKNOWN)
            if legal != UNKNOWN and legal:
                options = legal.split(",")
                if (len(options) > 24 or len(set(options)) != len(options)
                        or any(not allowed_action(opt) for opt in options)):
                    options = None
            else:
                options = None
            legal_count = len(options) if options is not None else UNKNOWN
            turn = safe_number(data.get("turn", ""), 10000)
            if isinstance(turn, int):
                if isinstance(session.turn, int) and turn < session.turn:
                    session.issue("INVALID_DECISION", number, "Decision turn stale against public stream")
                else:
                    session.turn = turn
            receipt_key = (turn, forced, authority)
            receipt_body = (action, proposal, reason, stage, tuple(options or ()))
            if receipt_key in session.decision_keys:
                if session.decision_keys[receipt_key] != receipt_body:
                    session.issue("SEQUENCE_CONFLICT", number, "Contradictory decision receipt")
                return
            session.decision_keys[receipt_key] = receipt_body
            if len(session.notable) < 16:
                session.notable.append((turn, authority, action, reason, stage))
            session.write("decision", number, authority=authority,
                          selected_action=action, proposal=proposal,
                          forced=forced, rationale=reason,
                          fallback_stage=stage, fallback_reason=data.get("fallback_reason", UNKNOWN)
                          if re.fullmatch(r"[A-Z_]{2,80}", data.get("fallback_reason", "")) else UNKNOWN,
                          legal_choice_count=legal_count,
                          legal_choices=options if options is not None else UNKNOWN,
                          confidence=UNKNOWN,
                          policy_calibration="UNPROVEN", source="OWN_NATIVE_RETURN")
            session.decisions += 1
            if authority == "SHADOW":
                session.shadow += 1
            if authority == "LEGACY":
                session.legacy += 1
            return
        if body.startswith("SHADOW_TERMINAL "):
            data = inline_fields(body)
            capture = data.get("capture", "")
            session = self.by_capture.get(capture)
            if session is None and CAPTURE.fullmatch(capture):
                open_sessions = [s for s in self.sessions if not s.terminal and s.capture == UNKNOWN]
                if len(open_sessions) == 1:
                    session = open_sessions[0]
                    session.capture = capture
                    self.by_capture[capture] = session
            if session is None and len([s for s in self.sessions if not s.terminal
                                        and s.capture == UNKNOWN]) > 1:
                self.global_issue("AMBIGUOUS_CORRELATION", number, "Terminal capture cannot be bound")
                return
            if session is None:
                session = self.capture_session(capture, number)
            if session is not None:
                session.terminal = True
                session.write("terminal", number, origin="COBBLEMON_BATTLE_VICTORY",
                              result=UNKNOWN, delivery="UNVERIFIED")
            return
        if body.startswith("SHADOW_ENVELOPE "):
            data = inline_fields(body)
            capture = data.get("capture", "")
            session = self.by_capture.get(capture)
            if session is None and CAPTURE.fullmatch(capture):
                seq = safe_number(data.get("seq", ""), 1000000)
                candidates = [s for s in self.sessions if s.capture == UNKNOWN
                              and seq in s.event_fingerprints]
                if len(candidates) == 1:
                    session = candidates[0]
                    session.capture = capture
                    self.by_capture[capture] = session
                elif len(candidates) > 1:
                    self.global_issue("AMBIGUOUS_CORRELATION", number,
                                      "Envelope matches multiple public captures")
                    return
            if session is None:
                session = self.capture_session(capture, number)
            if session:
                route = data.get("route", UNKNOWN)
                route = route if route in {"DIRECT_UPDATE", "SPLIT_PUBLIC", "UNFRAMED_UNKNOWN"} else UNKNOWN
                session.write("provenance", number, route=route, event_sequence=safe_number(data.get("seq", ""), 1000000),
                              spectator_delivery="UNVERIFIED", split_target=UNKNOWN)
            return
        if body.startswith("SHADOW_REPLAY_GAP "):
            data = inline_fields(body)
            battle = data.get("battle", "")
            capture = data.get("capture", "")
            session = self.by_capture.get(capture) if capture else None
            if session is None and UUID.fullmatch(battle):
                session = self.by_key.get("battle:" + battle.casefold())
            (session.issue("GAP", number, "Capture reported replay gap") if session
             else self.global_issue("GAP", number, "Capture reported replay gap"))
            return
        if body.startswith("SHADOW_ENVELOPE_FAILURE "):
            data = inline_fields(body)
            session = self.by_capture.get(data.get("capture"))
            code = data.get("reason", UNKNOWN)
            detail = code if re.fullmatch(r"[A-Z_]{2,64}", code) else UNKNOWN
            (session.issue("PROVENANCE_FAILURE", number, detail) if session
             else self.global_issue("PROVENANCE_FAILURE", number, detail))
            return
        if body.startswith("LEGACY_CHOICE_RECEIPT "):
            session = self.unique_open(number)
            if session:
                data = inline_fields(body)
                action = data.get("action", UNKNOWN)
                session.write("legacy_receipt", number,
                              selected_action=action if allowed_action(action) else UNKNOWN,
                              origin="LEGACY_CHOOSE_RETURN",
                              artifact_sha256=data.get("artifact_sha256", UNKNOWN)
                              if HASH.fullmatch(data.get("artifact_sha256", "")) else UNKNOWN,
                              custody="REVIEW_REQUIRED", confidence=UNKNOWN)
            return
        if body.startswith("Live decision authority="):
            session = self.unique_open(number)
            if session:
                authority = body.split("authority=", 1)[1].split(";", 1)[0]
                if authority in {"LEGACY", "SHADOW"}:
                    session.write("legacy_runtime_authority", number, authority=authority,
                                  selected_action=UNKNOWN, reason=UNKNOWN)
            return
        if body.startswith("Shadow forced fallback ") or body.startswith("Shadow ordinary fallback "):
            session = self.unique_open(number)
            if session:
                code = body.split("stage=", 1)[1].split(";", 1)[0] if "stage=" in body else UNKNOWN
                session.write("fallback", number,
                              stage=code if re.fullmatch(r"[A-Z_]{2,80}", code) else UNKNOWN,
                              reason=UNKNOWN)
            return
        if body.startswith("Shadow source=") or body.startswith("ShadowLiveDecisionEngine["):
            session = self.unique_open(number)
            if session:
                session.write("shadow_reason", number,
                              category=(re.search(r"category=([A-Z_]+)", body).group(1)
                                        if re.search(r"category=([A-Z_]+)", body) else UNKNOWN),
                              confidence=UNKNOWN)
            return
        if body.startswith(("Phase ", "Controlled envelope capture", "Phase 5 historical", "Phase 5 online")):
            # Existing phase diagnostics can contain unrestricted object strings:
            # preserve their existence and line references, never serialize them.
            session = self.last_session if self.last_session and not self.last_session.terminal else None
            if session:
                session.write("diagnostic_presence", number, category="EXISTING_PHASE_OBSERVATION",
                              content=UNKNOWN)
            return
        if any(token in body for token in ("Exception", "ERROR", "failed", "rejected", "UNKNOWN")):
            session = self.last_session if self.last_session and not self.last_session.terminal else None
            if session:
                session.issue("UNSUPPORTED_DIAGNOSTIC", number, "Unclassified Maestro diagnostic")
            else:
                self.global_issue("UNSUPPORTED_DIAGNOSTIC", number)
            return

    def finish(self):
        all_issues = list(self.global_issues)
        for session in self.sessions:
            directory = self.staging / session.name
            session.finish(directory, self.last_line)
            all_issues.extend(session.issues)
        if not self.sessions:
            self.global_issue("MISSING_SEQUENCE", self.last_line, "No supported battle capture in source")
            all_issues = list(self.global_issues)
        with (self.staging / "issues.jsonl").open("w", encoding="utf-8", newline="\n") as out:
            for issue in sorted(all_issues, key=lambda v: (v["source_line"], v["battle"], v["issue"])):
                out.write(json.dumps(issue, sort_keys=True, separators=(",", ":")) + "\n")
        return len(all_issues)


def publish_staging(staging: Path, output: Path):
    if output.is_symlink():
        raise ValueError("Refusing symlinked report destination")
    previous = None
    if output.exists():
        if not output.is_dir():
            raise ValueError("Report destination must be a directory")
        for child in output.iterdir():
            if child.name != "issues.jsonl" and not re.fullmatch(r"B[0-9]{2,}", child.name):
                raise ValueError("Report destination contains unrelated files")
        previous = output.with_name(output.name + ".old-" + next(tempfile._get_candidate_names()))
        output.rename(previous)
    try:
        staging.rename(output)
    except BaseException:
        if previous is not None and not output.exists():
            previous.rename(output)
        raise
    if previous is not None:
        shutil.rmtree(previous)


def process(source: Path, output: Path) -> tuple[int, int, str]:
    if not source.is_file() or source.is_symlink():
        raise ValueError("Source must be an existing regular log (no symlink)")
    if source.resolve() == output.resolve() or output.resolve() in source.resolve().parents:
        raise ValueError("Refusing report path overlapping original log")
    source_hash = digest_file(source)
    root = Path(__file__).resolve().parents[1]
    properties = root / "gradle.properties"
    version = UNKNOWN
    if properties.is_file():
        match = re.search(r"(?m)^version=([0-9]+\.[0-9]+\.[0-9]+)$", properties.read_text())
        if match:
            version = match.group(1)
    output.parent.mkdir(parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix=".battle-reports-", dir=output.parent))
    try:
        processor = Processor(source_hash, stage, version)
        with source.open("r", encoding="utf-8", errors="replace") as reader:
            for number, line in enumerate(reader, 1):
                if len(line) > 65536 or "\ufffd" in line:
                    processor.global_issue("MALFORMED_RECORD", number, "Oversized or invalid UTF-8 source line")
                    continue
                processor.handle(line.rstrip("\r\n"), number)
        count = processor.finish()
        publish_staging(stage, output)
        return len(processor.sessions), count, source_hash
    finally:
        if stage.exists():
            shutil.rmtree(stage)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Private offline MaestroAI battle diagnostics")
    parser.add_argument("log", type=Path, help="Original, locally retained Minecraft latest.log")
    parser.add_argument("--output", type=Path, default=Path("battle-reports"),
                        help="Ignored local report directory (default: battle-reports)")
    args = parser.parse_args(argv)
    try:
        battles, issues, fingerprint = process(args.log, args.output)
    except (OSError, ValueError) as exc:
        # Never echo the exception: it might include a private file path.
        print("Diagnostic processing refused: invalid or inaccessible input/output", file=sys.stderr)
        return 2
    print(f"Processed {battles} battle(s), {issues} issue(s); source SHA-256 {fingerprint}")
    print("Reports are private derived views. Original log retained; Minecraft acceptance UNVERIFIED.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
