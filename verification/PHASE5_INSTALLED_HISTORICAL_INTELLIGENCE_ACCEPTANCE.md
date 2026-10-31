# Phase 5 — Installed Historical Intelligence Acceptance Matrix

**Branch:** `Maestro4567` · **JAR:** 1.0.93 · **Evidence status:** pending independent operator provisioning, complete exact-commit CI and **actual Minecraft observation**. This matrix is an *owner-side execution plan*, not a test receipt. No raw owner logs are stored in public Maestro.

## Obtain the exact verified JAR

The public GitHub Actions run must finish **SUCCESS**, including the pinned
simulator step. Then open that same run's **Artifacts** section and download
`maestroai-verified-<exact 40-character commit SHA>` (a ZIP archive).
Extract `maestroai-1.0.93.jar` and `sha256.txt`; compare the JAR's
SHA-256 against the exported checksum before installing. The archive name
ties the build to the GitHub checkout commit, not the user's local file name.
For a second, independent checksum comparison, read the JAR SHA-256 from that same
successful workflow's **Check JAR and checker regressions** log (the JSON
field `jar_sha256` printed by `verification/check_jar.py`). Download the ZIP
and run from the Maestro source checkout:

```text
python verification/phase5_verify_download.py path/to/downloaded-artifact.zip 1.0.93 <independently-copied-jar_sha256>
```

The final argument is a placeholder, not a real digest. The checker reads the
ZIP **without extracting it**, rejects duplicate, unrelated, or symlinked
payloads, compares the checksum to the separately obtained workflow-log
value, and checks the mod ID and version inside the JAR. A checksum included
in the same ZIP alone cannot prove authentic GitHub provenance: confirm the
successful *exact-commit* run and its artifact name separately.

Do not treat an artifact from a different commit, failed workflow or
unverified local rebuild as acceptance evidence. The artifact contains
the compiled mod and checksum only, never the historical replay, keyring
or private operator inputs.

## Preconditions (all mandatory)

- [ ] Exact 1.0.93 SHA and public GitHub Actions run have every required check marked **SUCCESS**. Reject a JAR from a different commit even if it has the same filename/version.
- [ ] Singleplayer Minecraft Java 1.21.1, Fabric, Java 21, Cobblemon 1.7.3, RCT 0.15.2-beta and unchanged Cobbleverse mods are confirmed.
- [ ] Operator has genuinely independently approved a public spectator replay for this specific use, with explicit permission, signer and unexpired ledger decision. Neither a source listing nor a JVM `true` flag grants rights.
- [ ] Offline operator has prepared `custody-anchor.state` and stored the **exact** SHA-256 checkpoint, revision, keyring root, and clock floor independently from the replay/state directory.
- [ ] On the test instance only, use `-Dmaestroai.phase5HistoricalRolesObserve=true` and the exact `maestroai.phase5Archive.*` properties in [operator installation guide](PHASE5_TRUSTED_HISTORICAL_ARCHIVE_ACTIVATION.md). Never copy private signer keys or raw battle transcripts into the JAR, command line or public reports.

**If any item is missing:** test only ordinary live public Shadow diagnostics, report **historical source UNPROVISIONED**, and **do not claim installed historical acceptance**. Never install a synthetic signed regression fixture as if it were an independently approved real archive.

## Required startup verdict before any battle

With historical observation enabled, read one **redacted** startup status from
the new trainer constructor. These statuses do not themselves establish
real replay-use permission or Minecraft acceptance:

- `Phase 5 historical operator=PROVISIONED;authority=NONE`: the local signed
  archive passed initial checks; verify actual live historical reports and
  native callbacks separately.
- `Phase 5 historical operator=UNPROVISIONED;authority=NONE`: no external
  archive is configured; do **not** treat later ordinary live-Shadow diagnostics
  as historical intelligence.
- `Phase 5 historical operator=ABSTAIN;authority=NONE`: missing/invalid partial
  provisioning, signature, approval, checkpoint, clock or archive; never
  bypass this gate by weakening or fabricating evidence.

For all three, **Legacy still chooses every action**. The archive status
must never print replay IDs, filesystem paths, signing keys or raw logs.
No successful status has been observed in an installed game in this batch.

## Existing BrAI team reuse, no new builds

The same four teams and known builds can be reused. No need to reissue unchanged `/pokegive` commands. Begin with diagnostics enabled except the specified OFF control. The following are objectives, not fabricated native results.

| Existing battle | What to check | Required safety acceptance |
| --- | --- | --- |
| Team 1 / B1 | Shadow Tag, Encore, rejected switches/recovery | Native source/event identities stay current; failed choices cannot refresh historical roles |
| Team 1 / B2 | Trapping, boosts, faint and forced replacement | Faint/end/forced replacement cannot reuse withdrawn source or bind historical roles to the replacement |
| Team 2 / B3 | Public moves, setup/status and normal actions | A publicly witnessed setup/hazard is logged as a **live capability**; signed historical possibility remains a **separate unbound category** |
| Team 2 / B4 | Repeat with all Phase 5 diagnostics OFF | Legacy action control remains unchanged; no historical diagnostic or Shadow promotion |
| Team 3 / B5 | Hazards, weather/terrain and pivot | Current native sided RETURN callback is observed if actually invoked; do not confuse with sendSplitUpdate, an ACK or recipient delivery proof |
| Team 3 / B6 | Forced switches, fainting, replacement and cleanup | Actor/physical battle/turn/source epochs change correctly; stale reports withdraw |
| Team 4 / B7 | Illusion, Transform and public reveals | Unovan Zoroark disguises as **Greninja**, with Greninja **last eligible** in party. Never join historical identities or hidden builds to live display |
| Team 4 / B8 | Re-entry, form changes, uncertainty and battle end | Identity ambiguity abstains; battle end withdraws all old positive historical reports |

## Operator failure exercises (test-only archive copy)

- [ ] Missing/invalid/foreign signer, altered checkpoint, stale ledger revision, expired approval, or withdrawn replay permissions: fail closed without historical role data.
- [ ] After an accepted live report, edit **test-copy** spectator replay or remove it: existing report withdraws immediately on next inspection; restoring the file cannot resurrect it.
- [ ] Clock rollback/future-dated decisions and replay of an old callback: no fresh historical role grant.
- [ ] Wrong physical battle, contradictory public/private routing, Illusion, Transform and re-entry: no historical-to-current-Pokémon join.
- [ ] Compare a diagnostics-OFF control to the identical Legacy chooser path; no Shadow input affects the selected move.
- [ ] Native `sendSidedUpdate` RETURN, if visible, proves bounded method reachability only. Record no client ACK, stable Pokémon identity or delivery claims unless separately demonstrated.
- [ ] No opponent private `sideupdate`, hidden item, ability, unobserved moves, trainer UUID or raw archived replay is exported in public diagnostics.

## Evidence and decision

Record the **exact JAR SHA, public Actions URL, observed native callback, categorical diagnostic verdict, withdrawal case and diagnostics-OFF comparison** in a sanitized owner-controlled acceptance note. Any raw battle logs, if needed, remain private with LAIr/Headquarters rather than public commits.

**Current status:** Source-level Java, native ABI, JAR and simulator results must be recorded from *exact final commit* Actions; installed singleplayer observations are **NOT PERFORMED** here. Gate remains **NO-GO for full installed historical acceptance** until the real operator-approved archive and Minecraft evidence exist. Legacy remains authoritative. Shadow stays diagnostic-only.

## Redacted owner log inspection (v1.0.93)

After a **real** installed Minecraft session, the owner may run this local-only checker on an absolute private log path:

```text
python verification/phase5_minecraft_log_gate.py C:\path\to\private\latest.log
```

This reads at most 32 MiB, emits only bounded counts and categorical statuses (no raw player name, replay ID, key, UUID, or filesystem path), and requires a `PROVISIONED` startup, native Phase 6.24 diagnostic receipts, the live public Shadow diagnostic, and a *current unbound historical role* report. Contradictory identity/authority fields fail the report. The best result is `LOG_PATTERNS_OBSERVED`, **never** Minecraft acceptance: verify the signed archive and exact JAR provenance, all four teams, source validity, the owner-controlled battle context, and Legacy choice equivalence separately. Existing logs from a different JAR version are not acceptance evidence for 1.0.93. Keep original logs private with LAIr/Headquarters.

## Authenticated keyring rotation / signer-revocation acceptance (v1.0.93)

Only after the operator independently verifies a new Ed25519-signed keyring update, its **old active signer**, its new signer, fresh append-only signed manifest and **prior external checkpoint**, use the new `rotate` command in [the activation guide](PHASE5_TRUSTED_HISTORICAL_ARCHIVE_ACTIVATION.md). Preserve the old bootstrap root as independent historical trust evidence; **do not** overwrite it with keys carried by the update. Copy the returned **new** state checksum/revision/keyring epoch into a distinct trusted checkpoint. After restart with the approved latest pin, preflight must be `SOURCE_READY_FOR_MINECRAFT_TEST` or fail closed; it never independently proves replay-use permission.

On a test-only custody copy, exercise a revoked old signer, forged signature, altered/new-key SPKI, stale or incorrect external checkpoint, missed revision, and expired signer/approval. Expect categorical abstention and **no surviving historical-role report**. Check that a historical report issued under the old keyring is withdrawn immediately on trust-epoch change and never reappears from cached native callbacks; newly authorized evidence requires a fresh session/restart. Keep already approved archive files and raw logs private. Record observed source-run/JAR commit, redacted callback and withdrawal receipts, the diagnostic-OFF Legacy control, and the unchanged four-team/eight-battle matrix. These are *future Minecraft acceptance steps*: no installed tests are claimed from Java regressions.

## Unified commissioning audit (v1.0.93)

Before installing or running battles, run the local `phase5CommissionAudit` documented in [the trust activation guide](PHASE5_TRUSTED_HISTORICAL_ARCHIVE_ACTIVATION.md). Require the categorical `LOCALLY_READY_FOR_MINECRAFT_TEST` receipt plus independent verification that the exact source-commit CI **succeeded**, the replay was genuinely approved, and the clock/checkpoint trust roots are independently current. A ready receipt cannot replace an owner-side Minecraft test. Record only the categorical verdict and source-run SHA; keep all operator input files and original game logs private. In case of any other verdict, do not weaken the chain or silently use an old JAR; retain Legacy action control and Shadow abstention.
