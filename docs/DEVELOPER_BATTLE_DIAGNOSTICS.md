# Developer Battle Diagnostics v1

**Local developer tool; derived evidence, not a battle archive, publication system, or gameplay feature.**

From the Maestro repository checkout, with Python 3.10+ installed:

```bash
python3 tools/battle_diagnostics.py "/path/to/minecraft/logs/latest.log"
```

Windows PowerShell: `py -3 tools/battle_diagnostics.py "C:\path\to\minecraft\logs\latest.log"`.

The command reads the original log without modifying it, computes its SHA-256, and replaces only the tool-owned ignored directory `battle-reports/` with deterministic derived views:

```text
battle-reports/
  B01/
    summary.md
    battle.jsonl
  B02/
    summary.md
    battle.jsonl
  issues.jsonl
```

Specify an alternate private location with `--output /private/battle-reports`. Do **not** point the output at the original log or a directory with other files. The tool uses temporary staging and restores a previous output directory if final replacement fails. Store original logs separately with an owner-controlled backup.

## Optional in-game capture

Developer decision receipts are **off by default**. Start Minecraft with:

```text
-Dmaestroai.developerDiagnostics=true
-Dmaestroai.envelopeCapture=true
-Dmaestroai.legacyChoiceCapture=true
```

The first flag logs the selected authority and safely representable action after the existing native decision gate, Shadow proposal category, forced-action metadata, certified legal roots **only when an independent native proof is available**, and exact available Legacy fallback code. It never changes the selected action. The second enables existing envelope/terminal provenance. The third enables existing independently checked Legacy return receipts. No runtime file writer, telemetry uploader, GUI or server is added.

## Schema and interpretation

Each `battle.jsonl` line is a typed record with `schema`, `kind`, `battle` (B-number, not UUID), `order`, `source_line`, `source_sha256`, `turn`, `artifact_sha256`, and `version`. Types include `public_event`, `identity`, `decision`, `legacy_runtime_authority`, `legacy_receipt`, `shadow_reason`, `fallback`, `provenance`, `terminal`, `diagnostic_presence`, `issue` and final `metadata`. The top-level `issues.jsonl` contains stable issue identifiers, originating B-number, source line, classification, severity and safe detail. B-number ordering follows first observed capture appearance in the source log.

The `decision` record includes `selected_action`, `proposal`, `authority`, `forced`, `rationale`, `fallback_stage`, `fallback_reason`, `legal_choices`, `legal_choice_count` and `confidence`. Only allowlisted move IDs and own-party slot ordinals may be represented. The legal list is `UNKNOWN` unless the live owning-native legal witness was certified at the decision; forced replacement may therefore have `UNKNOWN` legal choices even when the action itself was recorded. Shadow policy scores are not calibrated probabilities.

Public event projections strip nicknames, HP and free-form strings; only recognized public species, moves, forms, types, statuses and effects are emitted. Unreviewed values are `UNKNOWN` and may create issues. Illusion/Transform/Forme-change records are appearance evidence, **never proof of an opponent's hidden true identity**. `UNKNOWN` also covers unseen moves, missing turn context, missing or redacted winners, unconfirmed capture beginning and missing spectator receipt. `INCOMPLETE` means an actual detected gap or contradiction. Neither a clean conversion nor a victory callback independently establishes complete delivered spectator evidence.

Older logs lacking `DEV_DIAG_DECISION` retain authority-only and public evidence when they contain supported Maestro-owned lines. They cannot provide decision details, legality, or complete beginning/terminal data that they never recorded. Ambiguous correlation and dropped/malformed records are issues, not guessed associations.

## Privacy and custody

The parser never embeds raw Minecraft log lines in the reports. It does not output usernames, nicknames, full UUIDs, chat, paths, raw request/sideupdate/split payloads, HP figures or unreviewed opponent items and moves. Source line numbers and SHA-256 refer back to the separately retained original. All raw source files and generated reports stay local and Git-ignored. These reports are **not** reviewed Battle Evidence archive submissions: that workflow requires independent review and explicit publication approval.

The input is streamed and source-hashed in chunks; output records are spooled to private staging, with a fixed per-battle event retention bound. Excess events are explicitly flagged as a capture gap. No source data is silently converted to public archive evidence.

## Troubleshooting and verification

A missing B folder means no supported capture was found; inspect the global issue index. Missing terminal markers, unknown actions, or ambiguous simultaneous battle correlation are explicit limitations; enable the existing envelope capture for subsequent normal acceptance sessions. If all observations are `UNKNOWN`, inspect the original log privately and verify exact source/runtime provenance; never override by guessing.

Regression command:

```bash
python3 verification/test_battle_diagnostics.py
```

This runs synthetic and adversarial parser tests. GitHub Actions independently runs the tests alongside existing Java, native and pack-source verification. **Installed Minecraft testing is still required** to validate that the new optional receipt fires, correlates with a real capture, and yields useful output without disclosing private data. No new battle is needed during implementation.
