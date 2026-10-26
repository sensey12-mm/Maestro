# Phase 6.6 — Independent Offline Integration Audit and FrAIL Handoff

**Repository / permitted branch:** `sensey12-mm/Maestro` / `Maestro-6`.  
**Starting exact commit:** `ecfa01c375e74bf345be1a68f1c03a2611723331` (1.0.50).  
**Result scope:** independent **source-level audit** of Phase 6.1–6.5 plus targeted Phase 6.6 repairs/tests. This document does NOT declare an all-green public CI result, installed Cobblemon behavior, multiplayer compatibility or runtime Shadow authority. All tests here are synthetic.

## Findings

**CONFIRMED defect in the Phase 6.2 provenance boundary.** The original public spectator gate validated the side slot `p2a` on `move` events, but did not verify that the actor's displayed nickname matched the last `switch`/`drag` actor occupying that slot. `PublicReplyCoverage.currentReplies` similarly extracts only a slot and moveset. A forged `p2a: different-nickname` event with a newly recomputed caller-controlled fixture hash could be counted as a move witnessed from the original active opponent and later enter Phase 6.3–6.5 conditional scenarios. The synthesized fixture hash was never designed to authenticate a real public source, so the parser itself must fail closed.

**Phase 6.6 repair:** a narrow per-slot last-switch/drag actor map now binds each observed `move` event to exactly the most recent displayed `p1a`/`p2a` actor. Where a targeted public move contains a target actor field, that target must also match the current actor in the referenced side slot. A move that targets an unknown third slot, includes unsupported arbitrary modifiers, spoofs a same-side actor or changes its displayed name without a new switch/drag is rejected without copying any private bytes to exceptions. Genuine untargeted moves remain allowed. This does NOT establish Pokémon party identity, authoritative delivery, installed move legality or proof against a replay whose untrusted origin is entirely forged. Illusion, Transform and true reentry still abstain in this narrow protocol slice.

**Regression:** `Phase66OfflineIntegrationRegression` constructs original framed synthetic events, re-pins SHA-256 over adversarially modified frames, and reuses the same synthetic Phase 6.1–6.5 implementation paths to assert every layer rejects forged same-slot opponent actors, forged own-side actors, wrong current target nicknames, unmodeled targets and unrecognized source tags. It positively checks valid current actors, untargeted public events, immutable sealed Phase 5 historical uncertainty, no reply-set completeness, conservative conditional counts and no Shadow/live authority. All preexisting standalone regressions remain enabled.

## Existing boundaries independently reviewed

- **Phase 6.1:** snapshots and Phase 4 profiles stay source-conditional, Phase 5 remains historical, and Shadow preference/live approval flags are hard-false. The public original-frame binding is only supplied through `inspectWithProvenance`; plain `inspect` remains explicitly unbound offline analysis. No runtime search promotion is authorized.
- **Phase 6.2:** digest is deterministic integrity of caller-pinned *synthetic* fixture, **not** a signature or custody proof; original framed public spectator routes, actor, turn and appearance must pass independent evidence parsers. The actor/target gap above was corrected. No opponent private bench or hidden item/ability claims are accepted.
- **Phase 6.3:** only appearance-local witnessed reply IDs with Phase 4 source support enter one-turn conditional cases; uncertain Phase 5 build proposals (even signed historical public evidence) never become current replies, legal action receipts or probabilities. Missing moves cannot be treated as zero-probability. Legal moves originate only from a synthetic root fixture.
- **Phase 6.4:** the denominator is **legal own fixture actions × witnessed reply IDs**, not all installed opponent choices. Unsupported own moves/switch entries and unknown replies remain unresolved. Zero witnessed replies never means complete.
- **Phase 6.5:** multiple hit/miss, speed-tie or critical execution branches are attributed by independently evaluating **one pinned witnessed reply** per Phase 4 plan. Every branch of that witness must resolve before its case count becomes known. Unknown branches suppress aggregate conditional HP-resource bounds. A source-conditional utility interval is NOT a win chance, recommendation, legal move set, or real-game robustness certificate.

## Required FrAIL GO/NO-GO inspection

1. Source review: `OfflineShadowProvenanceGate.java`, `OfflineShadowTacticalAdapter.java`, `OfflineShadowResponseScenarios.java`, `OfflineShadowCoverageAudit.java`, `ShadowMechanicsEvaluation.java`, and relevant Phase 5 handoff schemas. Inspect that actor/target validation cannot be bypassed through alternate public framing and that hypothetical builds never enter current identity.
2. Synthetic regressions: `OfflineShadowTacticalAdapterRegression`, `OfflineShadowProvenanceGateRegression`, `OfflineShadowResponseScenariosRegression`, `OfflineShadowCoverageAuditRegression` and `Phase66OfflineIntegrationRegression`, invoked by `StandaloneRegression`.
3. Public exact-commit GitHub Actions: full main Java/standalone run **AND** JAR safeguard **AND** entire pinned Cobblemon simulator/relocated engine source oracle must succeed on the **final** `Maestro-6` SHA. Validate the SHA itself, not just green status on older commits.
4. Verify Legacy chooser files and `LivePromotionGate` are unchanged; no `main` merge, no Shadow tactical enablement, no new real replay/permission intake and no Headquarters private CI consumption.
5. Minecraft acceptance is still required **for future runtime integration**, not represented by these synthetic offline tests.

## CI evidence, blockers and verdict

At Phase 6.6 commencement, four exact-commit public GitHub Actions runs were still listed as `in_progress` on their pinned-source oracle; each had already passed the repository safeguards, combined build/standalone suite and JAR checks:

| Phase | SHA prefix | Public GitHub run |
| --- | --- | --- |
| 6.2 | e2a8db5 | #40370499881 |
| 6.3 | 2cf76c9 | #40374010406 |
| 6.4 | cb816bd | #40375822093 |
| 6.5 | ecfa01c | #40377129940 |

The in-progress runs had not returned failure logs or a final oracle conclusion at that snapshot. A running job is not passing CI, and this document does not infer failure from runtime alone. CI cannot be declared closed without final exact-commit confirmation or a documented, independently approved waiver. **Source-level status at creation: offline code repair committed subject to verification; final CI gate = NO-GO (pending). Runtime/live tactical integration = NO-GO.** Any later CI status must be reported only after checking Actions again.

**Private Headquarters CI triggered by this Phase 6.6 development batch: 0 minutes.** Only public Maestro repo workflow is used. No merges to `main` and no protected source branches changed.
