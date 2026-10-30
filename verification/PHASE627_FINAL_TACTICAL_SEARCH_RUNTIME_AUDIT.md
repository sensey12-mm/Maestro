# Phase 6.27 — Final Tactical Search and Runtime Evidence Audit

**Scope:** `Maestro-6` Phases 6.1–6.26 and the external Phase 4/5/7 handoff boundaries. **Target version:** 1.0.72. This is a public-safe **source-level audit**, not a claim of Minecraft, recipient delivery, or integrated gameplay acceptance. The owner's original Minecraft logs and all private custody details remain outside this repository.

## Definitive source-level verdicts and gates

| Question | Verdict | Evidence and remaining conditions |
| --- | --- | --- |
| Are Phases 6.1–6.26 represented in code and registered synthetic verification? | **YES, source-level only** | Standalone and integration runners cover public mechanics, fixed/search replay, own native choice/switch evidence, source provenance, split/public privacy, native split and sided callbacks, lifecycle, correlation, the end-to-end observational gate and source-framed history. Public exact-commit CI for the **final 6.27 commit** is mandatory before closing this assertion as verified. |
| Is Phase 6 ready for independent staging/review against `Maestro457`? | **CONDITIONAL GO for isolated source-level integration review ONLY** | Verify successful full exact-commit CI on both input heads; reconcile source overlaps, run integration-specific regressions and revalidate all original Phase 4 mechanics, Phase 5 offline safety and Phase 7 benchmark gates on a newly created review branch. No runtime promotion or `main` merge is implied. |
| Is Phase 6 currently an authenticated live evidence/tactical pipeline? | **NO-GO** | Phase 6.21 deliberately has mutually exclusive direct/public and split/history source tracks; neither certifies recipient delivery, canonical packet identity, full installed legal-action coverage, or tactical control. Phase 6.18 native split RETURN and recipient receipt remain empirically unverified. |
| Can Phase 5 be used as online opponent action probabilities? | **NO-GO** | `PublicCompetitiveConsumerContract` remains offline-only; `PublicShadowBuildHandoff` grades build possibilities, not calibrated current-battle response probabilities. Do not map uncalibrated rank/labels into `ShadowSearchEngine.Outcome`. |
| Is Phase 7 a proven integrated Cobblemon native simulator? | **NO-GO** | The actual `Phase7Readiness.Report.integrated()` remains `false` even when all synthetic/offline stages are marked PASS. Phase 7's graph replay/self-play is observational, not a source-verified installed-battle simulator. |
| Does Phase 4 certify complete Cobblemon 1.7.3/Cobbleverse mechanics? | **NO-GO** | Its bounded source-pinned mechanics evidence explicitly abstains for unsupported mechanics. Integration must not silently convert UNKNOWN to a modeled mechanic. |
| Does any Phase 6 source grant live Shadow choice/promotion or change Legacy decision selection? | **NO** | `NativeEvidenceEndToEndGate.Report`, native sided-return reports, split reports and original cross-stream leases expose permanently false tactical/live-promotion authorization. Integration457 static/source and runtime synthetic regressions verify offline consumers are absent from the live chooser. |

## Audit evidence and repairs

1. **Phase 6.26 closure prerequisite.** Before claiming Phase 6.27 successful, verify the **full final** public GitHub Actions conclusion for `f265857194987e4a75c908d2b90081a4d607fc23` at https://github.com/sensey12-mm/Maestro/actions/runs/41051642842 . At audit preparation the Gradle/regression, JAR and native Mixin ABI steps passed; the pinned simulator-source step remained **in_progress**, hence no claimed full PASS. A still-running or failed run is a **CI verification blocker**, not permission to assume success.
2. **Verified 6.27 source consistency defect.** `NativeSidedDispatchReachability.Session.status()` previously returned `NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED` and a `current()==true` receipt after battle history or turn advanced without another native RETURN. Although a prior callback really happened, calling it current for the new source epoch was inaccurate. The new `NATIVE_SIDED_RETURN_PRIOR_EPOCH` verdict preserves the witnessed ordinal but is non-current; `Report.current()` now checks the most recently observed turn/history. The same physical-battle identity is bound on the first status-only poll and a callback cannot rewind a source advanced by a poll.
3. **Adversarial source regression.** Updated `NativeSidedDispatchReachabilityRegression` checks source progression without a new callback, historical ordinals, identical-epoch idempotence, late native callbacks trying to regress a turn, cross-battle callbacks after early status ownership, oversized polling metadata, end/revocation and later valid callback renewal. All outcomes still deny split identity, client delivery and tactical authority.
4. **Cross-phase regression.** Extended `Integration457Regression` with a maximally positive-looking synthetic `Phase7Readiness.Report` (all offline stages PASS, EXTERNALLY_REVIEWED_OFFLINE). Assert `integrated()==false` and explicit remaining Phase 6 integration / live-promotion blockers in the report. Phase 4 own mechanics/Z and Phase 5 offline consumer gates remain independently registered and unchanged.
5. **Previous empirical evidence.** Owner-authorized Minecraft 1.0.69 diagnostics established that `sendSidedUpdate` RETURN is reachable during a real battle, but not individual client packet delivery; the original 6.25 audit documented first-choice reset losing early callbacks. The 1.0.70 source repair restored session lifetime and 1.0.71 added conservative mixed public/private history parsing; **neither is yet independently confirmed in installed Minecraft**, and 1.0.72 stale-epoch repair is not runtime tested.
6. **Privacy and source pin.** Original private `sideupdate` request bodies never become public events. Source frame boundaries, signed/digested append-only history, actor/battle identity, Mixin ABI and adversarial rejection must all remain intact. Synthetic test names/values are not transcriptions of user logs.

## Final independent Phase 4/5/6/7 review handoff

A future authorized reviewer should use explicit input heads `Maestro457` = `362204bfcd3510e1a40f05586365f484ad60c6c8` (historically integrated Phase 4/5/7) and this Phase 6.27 head **only after its final exact-commit CI is green**. Check for any branch advancement before integration; do not rely on a branch-name pin alone. Branch integration and its CI need separate authorization and review.

Acceptance prerequisites:
- Retain Phase 4's installed-source mechanics adapter and conservative UNKNOWN verdicts.
- Retain Phase 5's offline-only purpose, signed custody and impossibility of live/current-battle tactical promotion; no opponent probability fabrication.
- Retain Phase 7's read-only own-choice capture/benchmarks and `integrated()==false` until independent real evaluation.
- Confirm Phase 6's native observer remains `RETURN` read-only, source-bound and non-authoritative, with **separate** Cobblemon `sendSidedUpdate` and `sendSplitUpdate` semantics.
- Require exact-commit CI and independent integrated regression tests on a new branch; verify opt-in flags do not change Legacy response.
- For runtime gates **only when requested and justified**, use targeted Minecraft evidence on an exact pinned JAR to validate the 1.0.70 lifetime repair, 1.0.71 parser and 1.0.72 stale source epoch. No unnecessary rerun of the original eight-battle matrix.
- Treat absence of a native `sendSplitUpdate` RETURN or per-recipient ACK as UNKNOWN, not success; do not promote any Shadow tactical model or merge into `main` without explicit owner permission.

## Final decision

**CONDITIONAL GO — independent, isolated source-level integration *review* only, subject to both input branches' successful exact-commit CI and additional integration regressions.**  
**NO-GO — live Shadow recommendations, native recipient delivery claims, installed-game tactical acceptance, full mechanics simulation and promotion.**

**Not performed:** private Headquarters CI; final integrated-source test against `Maestro457`; Minecraft testing of 1.0.70–1.0.72; any live Shadow authority transfer. These remain explicit follow-up work.
