# Phase 6.5 — Offline robustness evidence hardening and independent acceptance handoff

**Branch:** `Maestro-6`. Base commit: Phase 6.4 `cb816bd70b1538382b03de0fdf10ebb713a10c41`. **Scope:** synthetic offline-only provenance, mechanics uncertainty and coverage. No runtime tactics, Shadow promotion, or main merge.

## Source-level change

The Phase 6.4 coverage evaluator previously used the total number of flattened `ShadowMechanicsEvaluation.Ranking.lines` to attribute one outcome to one witnessed opponent reply. That attribution was not sound: equal-speed branches, hit/miss variations and other source-conditional execution paths can create multiple lines for a single reply. Merely matching total line count is not proof of branch provenance.

The hardened evaluator **recomputes** pinned Phase 6.2/6.3 public-fixture provenance and Phase 4 root legality, then isolates each source-supported `(own action, witnessed reply)` fixture into its **own, one-reply Phase 4 evaluation plan**. The reply index is obtained from the immutable validated plan itself, not by dividing a flattened comparison result. All generated Phase 4 branches must have known utility and no unresolved effects before that one witnessed case increments the resolved counter. Worst-low and best-high HP-resource bounds union all branches across all separately resolved witnesses. If any expected fixture reply is absent, unsupported or unresolved, interval bounds are null. Unknown effect labels are reduced to fixed, public, privacy-safe categories.

The result remains permanently incapable of providing an opponent legal-move certificate, calibrated probability, native modifiers/installed effects, Phase 5 stable identity, current turn binding, recommended action, Shadow tactical authorization or live promotion. Conditional bounds cannot be interpreted as probabilities, win rates, complete-game worst cases, or a real opponent's next move.

## Adversarial verification

`OfflineShadowCoverageAuditRegression` exercises a staged, explicitly **synthetic** same-speed Swift-vs-Swift scenario in which Phase 4 produces more than one execution branch for a **single** witnessed reply. This must resolve **at most one reply case**, even if the action has multiple conditional outcomes. Its interval must envelop all known execution branches. A mixed Swift/Scratch fixture checks that one unresolved witness withholds the aggregate bounds. Unknown profiles must not become known via the fixture, and a broader critical-hit interval must not confer action authority. Existing source-listed, missing/unsupported, trapped switch, stale/private public packet, replay reentry and historical signed Phase 5.21 identity isolation tests remain in the standalone regression run.

## FrAIL independent GO/NO-GO checklist

Review the committed version of `OfflineShadowCoverageAudit.java`, `OfflineShadowResponseScenarios.java`, `OfflineShadowProvenanceGate.java`, `ShadowMechanicsEvaluation.java`, `OfflineShadowCoverageAuditRegression.java`, and `StandaloneRegression.java`. Confirm each item:

- Actor/turn/appearance and own action set are recomputed from pinned synthetic original public frames. No Phase 5 candidate enters the reply fixture or supplies calibrated weights.
- Only source-supported, appearance-local witnessed moves create one-turn scenario plans. The complete opponent response set is **always unknown**.
- Every conditional comparison has **exactly one witnessed reply**, and **every branch** must resolve before counting that reply as conditionally covered. Mixed results never create a partial interval disguised as complete.
- Unmodeled source effects, private/forged evidence, unsupported moves and unproven switch entries fail closed. Counts represent a synthetic fixture denominator only, not installed battle mechanics coverage.
- Live chooser and Legacy execution remain untouched; Shadow remains observational and offline. No private Headquarters CI or player battle replay ingestion is required.
- Public exact-commit GitHub Actions must be inspected to completion, including full pinned Cobblemon simulator-source oracle, before any **final** CI GO. Minecraft runtime compatibility has **not** been tested or claimed.

## CI closure status

At inception, exact commits Phase 6.2 `e2a8db5` Actions #40370499881, Phase 6.3 `2cf76c9` Actions #40374010406, and Phase 6.4 `cb816bd` Actions #40375822093 each had successful repository checks, combined standalone tests, and JAR verification; all three were still **in progress** on their pinned-source oracle. Running job logs are not downloadable through the current GitHub connector (BlobNotFound) until GitHub publishes them. This is an **unresolved verification gate**, not an asserted green build and not evidence of a code defect. Exact-commit Phase 6.5 run must be checked separately.

**Headquarters CI minutes triggered by this development batch: 0.** No Headquarters workflow was called. No Minecraft acceptance, `main` merge, Shadow promotion or tactical-runtime wiring.
