# Phase 5 — Installed Historical Intelligence Acceptance Matrix

**Branch:** `Maestro4567` · **JAR:** 1.0.84 · **Evidence status:** pending independent operator provisioning, complete exact-commit CI and **actual Minecraft observation**. This matrix is an *owner-side execution plan*, not a test receipt. No raw owner logs are stored in public Maestro.

## Preconditions (all mandatory)

- [ ] Exact 1.0.84 SHA and public GitHub Actions run have every required check marked **SUCCESS**. Reject a JAR from a different commit even if it has the same filename/version.
- [ ] Singleplayer Minecraft Java 1.21.1, Fabric, Java 21, Cobblemon 1.7.3, RCT 0.15.2-beta and unchanged Cobbleverse mods are confirmed.
- [ ] Operator has genuinely independently approved a public spectator replay for this specific use, with explicit permission, signer and unexpired ledger decision. Neither a source listing nor a JVM `true` flag grants rights.
- [ ] Offline operator has prepared `custody-anchor.state` and stored the **exact** SHA-256 checkpoint, revision, keyring root, and clock floor independently from the replay/state directory.
- [ ] On the test instance only, use `-Dmaestroai.phase5HistoricalRolesObserve=true` and the exact `maestroai.phase5Archive.*` properties in [operator installation guide](PHASE5_TRUSTED_HISTORICAL_ARCHIVE_ACTIVATION.md). Never copy private signer keys or raw battle transcripts into the JAR, command line or public reports.

**If any item is missing:** test only ordinary live public Shadow diagnostics, report **historical source UNPROVISIONED**, and **do not claim installed historical acceptance**. Never install a synthetic signed regression fixture as if it were an independently approved real archive.

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
