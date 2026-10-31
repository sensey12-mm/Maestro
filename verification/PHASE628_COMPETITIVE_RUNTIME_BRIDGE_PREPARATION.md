# Phase 6.28 — Competitive Intelligence Runtime Bridge Preparation

**Implementation branch:** `Maestro4567` · **Version:** 1.0.73 · **Posture:** Shadow-only, observational, non-authoritative.

## Source-verified constraints

- Phase 5.21 `PublicShadowBuildHandoff.Report` is built from signed replay custody through an **offline-only** consumer. Its `Appearance.index` represents one historical replay appearance, **not** the current opponent's party slot or stable actor identity. Its grades and source-listed candidate builds are *possibilities*, not proved live builds or calibrated response probabilities. A live `PublicCompetitiveConsumerContract.ConsumerRequest` is rejected by design.
- Phase 6's `IntegrationRuntimeReadinessGate.Report` proves source-owned alignment of a native `sendSidedUpdate` RETURN and a structurally parsed public/private history epoch within the same physical battle. It does **not** prove a publicly authenticated actor identity, a matching Showdown split event, packet delivery, a complete installed legal set, or authority to run Shadow search.
- A public species/display match, ordinal equality between different battles, and native actor-set membership cannot establish the identity of a historical opponent appearance in a current battle. Illusion, Transform, temporary forms and re-entry make this especially important.
- Phase 4 mechanics knowledge is conditional and incomplete; Phase 7 replay evaluation is offline/observational. Neither changes these constraints.

## Implemented contract

`ShadowCompetitiveRuntimeBridge` introduces two **separate** operations:

1. `describeOffline(Phase5Report, Phase5Pin, OfflineConsumerRequest)` is callable only with the real source-owned Phase 5 report. It validates existing version, source revision, mechanics scope, policy pin and negative authority flags. It emits a deliberately narrow `HistoricalDescriptor` with the fixed schema/policy identity; **no** candidate moves, items, abilities, species, appearance IDs, replay content, raw event lines, probabilities, or Phase 5 report reference survive into that descriptor.
2. `Session.inspect(physicalBattle, HistoricalDescriptor, Phase6RuntimeReport, IdentityEvidence)` is a **read-only contract compatibility check**. It requires the original `IntegrationRuntimeReadinessGate.Report`, not caller-reconstructed numeric fields, plus its matching original physical-battle reference, pinned Cobblemon version, current source epoch, and monotonic public-history revision/native callback ordinal. Invalid/replayed/stale/foreign battle observations fail closed; absent sources abstain. A caller assertion of stable actor identity is **rejected**, and a public display/Illusion/Transform classification cannot authenticate a historical/current identity match. The only positive report is `PREPARED_UNBOUND_OFFLINE_ONLY`—an observational *boundary*, not an online join.

Even the positive `Report.current()` means **only** that the original Phase 6 source epoch is still current. The original signed historical export stays offline. **No** Phase 5 hypotheses, builds, appearance ordinals or current-battle identities are joined or made available for battle choice. Every report API for actor authenticity, historical identity match, opponent action/probability availability, Phase 5 online consumption, private payload export, Shadow search, tactical action or live promotion returns **false**. The production Legacy chooser never calls the bridge.

## Adversarial synthetic verification

- Build a genuine synthetic signed Phase 5 handoff using existing Phase 5.21 custody fixtures. Confirm only schema/policy information survives the offline descriptor and no current-battle authority is granted.
- Verify purpose=SHADOW_TACTICAL, current-battle binding and tactical-use requests remain rejected by the **unmodified** Phase 5 contract.
- Produce original source-owned Phase 6 native callback, split routing and integrated runtime gate receipts; check the narrow read-only preparation result and persistent absence of opponent identity binding.
- Reject unsupported actor assertions, forged physical battle, missing identity classification, stale epoch, duplicate source usage and withdrawn sessions. Check automatic source freshness invalidation on public history advancement.
- Check Illusion/Transform and public display identity remain uncertain, raw/synthetic private markers do not appear in diagnostics, and the actual Minecraft chooser source remains disconnected.

All fixtures are hand-authored, fake-source and deterministic. No owner's battle logs, private replay records, personal identifiers or private Headquarters data are committed.

## Phase 5 contract changes **required before separately approved online consumption**

1. A **new explicitly versioned online-scoped contract**, separate from current `PublicCompetitiveConsumerContract.ConsumerRequest` and `PublicShadowBuildHandoff.Report`. Do not change or relax the existing offline-only enums, constructors, source pins or signed-custody checks in-place. The online contract must define a reviewed purpose and explicit user authorization.
2. A source-owned **current battle actor/appearance continuity** attestation with provenance: physical battle ID held privately, publicly revealed identity, slot re-entry, Illusion/Zoroark reveal, Transform and form changes, and explicit invalidation on ambiguity. It must never infer a current actor from a historical replay appearance index or displayed species.
3. A versioned **freshness/lifecycle lease** binding current public observation revision, turn, message history, actor identity evidence and both Phase 6 and Phase 5 source/custody versions; terminal invalidation for rewinds, split disagreements, actor replacement, battle end and source withdrawal.
4. An **uncertainty/coverage schema** preserving graded, incomplete historical hypotheses. Introduce calibrated opponent response probability distributions only after independent approved-data training/evaluation and explicit calibration/coverage validation; missing/unsupported options must abstain. Do not promote a historical move possibility to an installed legal action.
5. Privacy-preserving custody, access and revocation rules for any future historical/live bridge; rights verification is independent from cryptographic integrity. Never persist private requests or expose replay identities in runtime diagnostics.
6. External FrAIL 2 integration review, exact-commit public CI, and separately approved, privacy-safe Minecraft acceptance for the installed binary before any runtime consumption. Even then Legacy authority and Shadow-only tactical experiments require separate explicit authorization.

**Final readiness:** **GO** for source-level *preparation-only* interface review after exact-commit CI passes; **NO-GO** for online Phase 5 consumption, current-battle opponent matching, calibrated battle response probabilities, Shadow tactical search, gameplay authority, and promotion.

**Not performed:** Minecraft testing, any actual online Phase 5 consumer, live tactics changes, `main` merge, Phase 5 contract relaxation, or private Headquarters CI.
