# Phase 6.30 — Opponent Identity Evidence Integration

**Branch:** `Maestro4567` · **Version:** 1.0.75 · **Architecture:** read-only Phase 6 source receipts + offline-only Phase 5 descriptor. Legacy chooser remains authoritative; Shadow remains non-authoritative.

## Verified source contract

Phase 6.28 `ShadowCompetitiveRuntimeBridge.Session.inspect` previously accepted a caller-selected public-identity *classification* as a sidecar to a valid runtime source epoch. Its classification was expressly non-authenticating. Phase 6.29 `PublicOpponentIdentityContinuity.Report` separately proved that the original Phase 6.16 split-routing source had examined the exact accepted public/private history, while never asserting a permanent Pokémon identity. Merely matching public turn, history chunk count, source revision, native actor membership, or battle object is not proof that both reports used the **same original routing receipt**.

## Implementation

- `IntegrationRuntimeReadinessGate.Report.sharesOriginalRouting(original)` requires **reference identity** of the original Phase 6.16 `PublicSplitRoutingGate.Report`, plus both receipts still current. Its scope is package-private; no original split text or private source is exported.
- `PublicOpponentIdentityContinuity.Report.sharesRuntimeRouting(battle, runtime)` requires that same original receipt, one **original physical battle object**, exact revision/turn/chunk epoch, current 6.29 continuity, and current 6.28 native source readiness. Matched metadata from independently created routing sessions is **rejected**.
- `ShadowCompetitiveRuntimeBridge.Session.inspectWithContinuity` accepts an original Phase 6.29 continuity report **instead of a caller-asserted actor classification**, verifies schema/pin, original battle identity, source freshness, shared source custody, and only then delegates to the existing 6.28 fail-closed proof gates. This does not invoke any online Phase 5 consumer.
- A source-aligned *ordinary public appearance* can yield `PREPARED_UNBOUND_OFFLINE_ONLY`; this **still never identifies an opponent Pokémon or establishes a historical build association**. An `IDENTITY_AMBIGUOUS` 6.29 report (Illusion reveal, Transform, form change, slot re-use/re-entry, unsupported/multi-slot uncertainty) yields terminal `IDENTITY_UNCERTAIN` fail-closed result. Missing/expired/mismatched/foreign/replayed source evidence is explicitly denied.
- The prepared report carries only original source-owned read-only references and bounded numeric/categorical public diagnostics. `Report.current()` dynamically rechecks both original 6.28 and 6.29 receipts. Both always deny historical-appearance matching, opponent identity authentication, current-battle Phase 5 availability, calibrated probabilities, installed legal actions, Phase 6 search, gameplay decisions and promotion.
- The older Phase 6.28 `inspect(..., IdentityEvidence)` method is preserved for backwards-compatible **unbound offline diagnostic** purposes; it does not produce a Phase 6.30 `publicContinuitySourceAligned()` receipt or grant authority. Actual source-bound integration requires the new `inspectWithContinuity` API.

## Synthetic adversarial regression matrix

`ShadowOpponentIdentityIntegrationRegression` uses a genuine synthetic signed-custody Phase 5.21 offline descriptor plus original 6.16 routing, 6.24 native RETURN, 4567 integrated runtime gate, and 6.29 continuity reports. It asserts: positive **source-only** compatibility without actor authority; missing continuity abstains; duplicate/replayed receipts withdraw; equal turn/revision from separate routing sessions cannot be mixed; foreign physical battles fail; source progression revokes reports dynamically; Illusion `replace`, Transform, two form-change variants, Illusion end and same-slot re-entry abstain; diagnostics redact all private and historical fields; and source-level inspection confirms Minecraft Legacy chooser remains disconnected. The standalone regression suite runs this alongside all Phase 4/5/6/7 suites.

## Required future work and NO-GO

NO-GO for live Phase 5 consumption, stable Pokémon identity, authenticated opponent-side/party-slot binding, real-time calibrated opponent predictions, native Showdown split-to-recipient proof, installed legal actions, tactical Shadow control and Legacy promotion. Any future online Phase 5 consumer requires **separate authorization and versioned, provenance-safe contract**. A successful CI run verifies only source/synthetic readiness. Minecraft testing and independent FrAIL 2 integration review remain outstanding for installed runtime acceptance.

No Phase 5 offline permission was relaxed; no public replay/private Headquarters payloads were committed. No trainer or Legacy chooser changes, no `main` merge, no private Headquarters CI, and no in-game acceptance claimed.
