# Phase 6.32 — Live Competitive Intelligence Consumer Safety Contract

**Branch:** `Maestro4567` · **Version:** 1.0.77 · **Status:** *source-level preparation only*, not an online Phase 5 implementation.

## Scope and source evidence

The existing `PublicCompetitiveConsumerContract.ConsumerRequest` only accepts `OFFLINE_SHADOW_REVIEW`, forbidding current-battle binding and tactical usage. Its signed-custody historical export is reduced by Phase 6.28 to `ShadowCompetitiveRuntimeBridge.HistoricalDescriptor`: source/schema metadata only; **no historical Pokémon identity, candidate build, probabilities, item, ability or moveset**.

- Phase 6.28: original current Phase 6.24/6.16 observations; **not** a public actor or client-delivery certificate.
- Phase 6.29: exact original public/private history custody; active positions, switching, re-entry, Illusion and Transform cannot authenticate an individual Pokémon.
- Phase 6.30: same **original Phase 6.16 routing report**, not merely same battle/turn/revision.
- Phase 6.31: source-owned native opponent side p1/p2, verified through the original 6.24 callback and 6.29/6.30 chain; **not** a public actor's species, party slot, persistent Pokémon identity or historical replay appearance. The positive 6.31 report is `OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND`.

Phase 6.32 only assesses this existing evidence chain. It does not add live game behavior, expose historical Phase 5 candidates, introduce a new Phase 5 tactical consumer, or change the chooser.

## New fail-closed contract

`ShadowLiveCompetitiveConsumerSafety` is a versioned, source-pinned, diagnostic-only safety interface.

- `ContractPin.pinned()` validates the Phase 5 consumer pin and source revision indirectly through the original Phase 5 contract, pinned installed Cobblemon source, and exact Phase 5.21 / 6.28 / 6.29 / 6.31 schema revisions. Pins are **compatibility**, not authorization.
- `Request.preparationPinned()` is the *only* accepted purpose. Constructor rejects `LIVE_COMPETITIVE_CONSUMPTION`, `SHADOW_TACTICAL_USE`, `LEGACY_CHOICE_CONTROL`, current-opponent binding, historical-candidate export or tactical decision flags. Phase 5's existing offline-only request type remains unchanged.
- `NativeOpponentSideProvenance.Report.sharesOriginalBridge(battle, bridge)` is package-private and requires the **identical original source-issued Phase 6.30 bridge report** that 6.31 accepted, plus original physical battle identity and a *current* original source chain. Matching public side, turn, source revision, callback ordinal, or synthetic lookalike report cannot substitute this.
- `Session.inspect` only reviews original source-issued 6.31 side and 6.30 bridge reports plus a genuine Phase 6.28 historical **offline descriptor** and pinned preparation request. Absent inputs abstain; unpinned, stale, foreign, mismatched, ambiguous or replayed inputs fail closed. Reports are dynamically invalidated on upstream history advancement, callbacks, battle end and withdrawal.
- A positive `SOURCE_EPOCH_AUDITED_OFFLINE_ONLY` report exposes only the already source-aligned **public p1/p2 opponent side**, numeric original epoch metadata and *missing capability* labels. It exposes **no** active Pokémon, historical build candidate, replay appearance, request, packet or private payload.
- Even a positive, current report **always returns false** for authenticated Pokémon identity, historical opponent matching, probability calibration, installed legal actions, private payload export, online Phase 5 consumption, Shadow search, tactical decisions, Legacy choice authority and Shadow promotion.
- `MissingCapability` explicitly lists prerequisites: persistent Pokémon identity joining, approved calibrated opponent responses, installed legal actions, verified runtime/private delivery evidence, a separately approved *new versioned* Phase 5 online consumer, Shadow tactical authorization, and Legacy promotion authorization. None is supplied by Phase 6.32.

## Adversarial regression verification

`ShadowLiveCompetitiveConsumerSafetyRegression`, added to `StandaloneRegression`, uses **genuine synthetic signed Phase 5 source-custody fixtures**, and original Phase 6.16 routing, 6.24 native RETURN, integrated runtime gate, 6.29 public continuity, 6.30 exact bridge, and 6.31 native side source reports.

Covered scenarios: pinned source/schema mismatch, future versions, prohibited live/tactical/Legacy purposes and binding flags, missing offline and native evidence, original exact-chain success with both p1/p2 side orientations, replay revocation, forged same-metadata bridge receipt from a different owner, foreign physical battle, native-only unlinked side, public-history revision expiry, foreign/actual battle end, Illusion reveal, Transform, temporary/permanent form changes, slot reuse and multi-active uncertainty. Every report is asserted to remain non-authoritative and redacted. Source inspection asserts the Minecraft chooser does **not** invoke the new contract. Fixtures are deterministic and synthetic-only.

## Acceptance / future work

**GO** for version-pinned read-only *safety contract* review only after full exact-commit public CI; **NO-GO** for Phase 5 online activation, matching a history hypothesis to a current Pokémon, calibrated opponent predictions, installed legal move evidence, Shadow actions and live promotion. The earlier phases' source audits and Java tests are not Minecraft acceptance. Actual installed runtime/source callbacks, side evidence, Illusion/Transform/re-entry must be validated separately **in Minecraft**, and online Phase 5 requires a new separately approved contract and independent integration review.

This batch does not merge into `main`, enable online Phase 5, modify trainer configuration or Legacy chooser actions, consume private battle logs, run private Headquarters CI, or promote Shadow.
