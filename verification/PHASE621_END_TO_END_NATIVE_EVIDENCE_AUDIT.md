# Phase 6.21 — End-to-End Native Evidence Consistency and Safety Audit

**Repo:** `sensey12-mm/Maestro` · **Branch:** `Maestro-6` · **Version:** `1.0.66` · **Operating posture:** Legacy live control; Shadow observation only.

## Audited actual source-level chain

Read the current original `Maestro-6` Java implementations of `OwnRequestTransport`, Phase 6.9 `ReadOnlyShadowObservationBridge`, Phase 6.10 move witness, Phase 6.11 switch witness, Phase 6.12 `NativeOwnActionFusion`, Phase 6.13 `CrossStreamEvidenceReadiness`, Phase 6.16 `PublicSplitRoutingGate`, Phase 6.17 `NativeSplitRecipientCustody`, Phase 6.18/6.19 `NativeSplitDispatchObservation` and Phase 6.20 `NativeDispatchHistoryCorrelation`. Inspected `MaestroBattleAI` integration, its battle-lifecycle withdrawal and pinned Cobblemon 1.7.3 ABI gate. Source commits are not proof that optional Mixin handlers actually execute in Minecraft.

**Verified cross-contract incompatibility:** Phase 6.9 intentionally fails closed upon any split-routed private/public chunk. Phase 6.13 requires a *current Phase 6.9 original public source lease*, so Phase 6.13 can never attest a split-containing source from the same original public observation. Conversely Phase 6.16–6.20 deliberately parse the original split source and can produce only an **unverified native/history correlation**. It is unsafe to combine both into one asserted *fully ready* stream. The new audit formalizes **two mutually exclusive, strictly observational tracks**, not a false merged runtime source.

## Implemented Phase 6.21 safety gate

`NativeEvidenceEndToEndGate` is disabled by default and has original battle-object binding, revocation on mismatch, and permanently false tactical/identity/delivery authority:

1. **Direct own/public path:** requires an original still-current Phase 6.13 `CrossStreamEvidenceReadiness.Report`, itself bound to the original Phase 6.9 public lease and Phase 6.12 native fused request. Adds source-owned, package-private `fromBattle` checks in the original Phase 6.9/6.13 report classes. A current result is `DIRECT_ONLY_OBSERVATIONAL`, **not** a complete native legal-set certificate, source delivery authentication or Shadow permission.
2. **Split/history path:** requires an original still-current Phase 6.20 native timing correlation report and Phase 6.17 runtime `DELIVERY_UNOBSERVED` report. They must share the **identical reference-equal Phase 6.16 structural report object**—matching SHA, count, turn, battle ID or separately parsed equivalent history is insufficient. Adds narrowly scoped private-origin package checks to Phase 6.17 and 6.20. A current result is `SPLIT_TIMING_UNVERIFIED`, **not** proof that a particular split caused the native send or reached a recipient.
3. A source-replayed epoch, expired original lease, old native dispatch, wrong physical battle, contrary split custody or attempted coexistence of both positive modes permanently withdraws the gate for that battle. Missing optional evidence returns a non-positive abstention without fabricating a witness. All previously returned positive results dynamically expire when their source is revoked/advanced or the gate is withdrawn.

All result strings contain only fixed verdict, mode, public turn and revision. `completeInstalledLegalSetProven`, `publicActorAuthenticated`, `eventIdentityAuthenticated`, `recipientDeliveryAuthenticated`, `privatePayloadExportable`, `opponentActionsKnown`, `shadowSearchAuthorized`, `tacticalDecisionAuthorized`, and `livePromotionAuthorized` are permanently **false**, regardless of positive-looking source alignment.

**Opt-in:** `-Dmaestroai.phase621Observe=true`. The existing post-Legacy Phase 6.13 path emits read-only direct review; the existing public-history Phase 6.16/6.17/6.20 path emits read-only split review. This opt-in also enables the independent underlying Phase 6.9–6.13 and 6.16–6.20 observation prerequisites. No original raw request, opponent private data or split half enters the new gate or logs. Legacy response selection remains identical, with no tactical consumer or side-effect callback reading an audit result. On battle reset, transport loss or native BattleEndPacket, the observer is withdrawn. Running these opt-in modes may legitimately yield many abstentions when normal Cobblemon event shapes are outside the conservative source pins.

## Regression matrix

`NativeEvidenceEndToEndGateRegression`, invoked from `StandaloneRegression`, constructs authentic synthetic source-owned `OwnRequestTransport` and installed own move/party witnesses; uses Phase 6.9 original public source and Phase 6.13 native/public comparison; independently constructs Phase 6.16 structural private/public split history, Phase 6.17 unobserved recipient report, Phase 6.18 native dispatch return, and Phase 6.20 timing correlation. It checks:

- Fully aligned current direct and split-mode source chains **independently**, with every tactical/actor/delivery claim still false
- Original battle object substitution in either source, mismatch of **structural report reference** despite matching turn, count and battle, status/source compatibility
- Direct and split mode collisions in both orders; permanent session withdrawal without overriding Legacy
- Native choice lease closure, public source advancing or withdrawing, native battle end and recipient/dispatch withdrawal
- Replayed source epochs, missing optional witnesses, redacted original nickname/UUID/private request/private HP, and terminal non-resurrection

All Phase 6.1–6.20 regressions remain registered. This is a **synthetic Java and pinned-source safety acceptance only**; it does not certify Minecraft compatibility.

## Deterministic readiness criteria

| Observation case | Safe verdict | Tactical permission |
| --- | --- | --- |
| Current original direct-public source + own native choice report, same battle and turn | DIRECT_ONLY_OBSERVATIONAL | **None** |
| Current original structural split source + same original recipient-unobserved receipt + current native dispatch/history window | SPLIT_TIMING_UNVERIFIED | **None** |
| Missing optional native/public source | MISSING_DIRECT_EVIDENCE / MISSING_SPLIT_EVIDENCE | **None** |
| Expired, replayed, mismatched source or incompatible direct/split positive tracks | STALE_SOURCE / WRONG_BATTLE / WRONG_SPLIT_CUSTODY / CROSS_MODE_CONFLICT / REPLAY_OR_DUPLICATE; permanent withdrawal | **None** |

The two accepted verdicts are **mutually exclusive** for the same battle/session, and neither is a full integrated engine readiness gate.

## Explicit Minecraft acceptance checklist — not yet performed

- [ ] Start Minecraft Java 1.21.1 with exact Cobblemon 1.7.3+1.21.1 and Maestro Java/Fabric mod build, verify actual mod versions and Mixin application.
- [ ] Verify request dispatch and exact own actor/request identity, turn, forced/normal move/switch and party observations during actual battles.
- [ ] Verify optional `sendSplitUpdate` RETURN and `sendUpdate` battle-end observers fire in game; check repeated native sends in a single turn, no false authority or crash on duplicate handling.
- [ ] Capture privacy-safe diagnostic outcomes for common source-pinned direct updates, status/heal/damage, Illusion/transform and split events; note abstentions rather than bypassing them.
- [ ] Verify append-only history, order/turn, actor identity visibility, native dispatch timing and early/late history collection against installed Cobblemon's real packet format.
- [ ] Verify observer withdrawal after battle end, another battle, request replacement, source rewind, disconnect or reload. Compare Legacy behavior with observations disabled/enabled.
- [ ] Check no opponent private request, raw split private half, network packet payload, nicknames, UUIDs or sensitive player data appear in Phase 6.9–6.21 diagnostics.
- [ ] Separately design a trustworthy, installed per-recipient transport **acknowledgment** before any delivery-authenticated claim; none exists now.
- [ ] Run independent final FrAIL Phase 6 acceptance *when the phase is complete*, without merging to `main` or promoting Shadow.

**CI:** require exact-final-commit public Java/standalone tests, JAR checks, pinned Cobblemon JVM ABI and full pinned-source oracle. A pending oracle is not a pass. **Headquarters private CI:** 0 runs/minutes. No `main` merge, Shadow live promotion or trainer-config changes.
