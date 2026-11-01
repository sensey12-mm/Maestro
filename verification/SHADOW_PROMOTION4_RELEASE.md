# Shadow Promotion 4 — MaestroAI 1.1.2 (runtime-stabilization checkpoint)

**Branch:** `Maestro4567`. The first actual Minecraft 1.1.1 battle was run by the owner, NOT by CI. CI and source regressions must not be described as further Minecraft acceptance. This public note contains no raw battle evidence, identities or private data.

## Confirmed issues and changes
- Phase 6.22 public-history sampling was anchored to the Legacy chooser path. In Shadow-led turns, the historical frame count stayed at zero even while Phase 6.24 sided callbacks and authoritative Shadow choices progressed. The source observer now samples the *original* installed Cobblemon history **after** live selection, before the diagnostic Phase 6.21/6.22 report. It is telemetry-only and never enters the choice engine.
- Forced replacements in the first owner battle fell back to Legacy. A fallback is NOT by itself evidence of a broken safety guard. The code now emits bounded, privacy-safe source-readiness stages captured BEFORE invoking the lazy Legacy fallback: `NO_OWN_LEASE`, `STALE_OWN_LEASE`, `NO_NATIVE_FORCED_PROPOSAL`, or `NATIVE_FORCED_PROPOSAL_READY`. This distinguishes absent ownership proof from rejection after a proposal. **The available minimized battle log cannot establish which stage the original fallbacks reached. No native forced-switch guard is relaxed.**
- Phase 6.22's `sendSplitUpdate` hook and Phase 6.24's `sendSidedUpdate` hook are separate. An observed sided callback does not prove split dispatch. The `HOOK_UNOBSERVED` verdict remains correct for missing split callbacks even when source history is now populated.
- Tactical heuristics, opponent evidence UNKNOWN, repeated setup valuation and Phase 7 benchmarking are outside this runtime stabilization. Datapack warnings are operator-owned, not changed here.

## Source safeguards
- Independent Shadow decisions remain first-party/native-root bound; Legacy executes only on the existing fallback path.
- No opponent hidden inputs, raw request bodies or private packets enter the new diagnostics. The public-history session retains salted hashes and existing bounded-source/monotonicity checks; a stale or rewritten session withdraws.
- Tests check Shadow-only history advancement, absence of forged native witnesses, and that sampling occurs strictly after live selection. Tests also check that forced fallback stages are captured before Legacy can mutate evidence.

## Release and remaining acceptance
- Build version `1.1.2`; a verified release requires exact-commit public `verify`, `pack_source`, `publish` success and matching JAR/checksum in the published artifact.
- Source tests are not installed-game tests. **Minecraft acceptance remains required** to determine which forced replacement evidence stage occurs in real play and whether the specific replacement can safely become Shadow-authoritative. Never classify an unobserved native hook as successfully installed.
- No merge into `main`, no Phase 7 completion claim.
