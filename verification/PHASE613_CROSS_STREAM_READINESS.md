# Phase 6.13 — Cross-Stream Evidence Consistency and Readiness Gates

**Branch:** `Maestro-6` · **Version:** `1.0.58` · **Mode:** optional observation-only / non-tactical · **GitHub public CI:** exact final SHA required.

## Source-level interface and exact boundaries

Phase 6.9's `ReadOnlyShadowObservationBridge` already observes Cobblemon 1.7.3's `PokemonBattle.getShowdownMessages()`, retaining **only** a narrow, append-only `update` history. Its `publicTurn`, revision and event count are source-framing premises; they are NOT proof of native actor identities, public message delivery or a complete Cobblemon mechanics model. It permanently withdraws on unexpected split/private input, history rewrites, unsupported events and source-version failures. Phase 6.12's `NativeOwnActionFusion.Review` already requires the same active own actor, exact installed request token, native own Pokémon, current `OwnRequestTransport.Lease`, source turn and independently checked native move/party witnesses.

Phase 6.13 adds `CrossStreamEvidenceReadiness` with a fail-closed, per-battle `Session`. The bridge now issues an internal, non-exported `PublicSourceLease`, bound to **that exact battle object**, source-compatible original history revision, public turn and event count. Every lease is dynamically invalidated when history is extended, rewritten, withdrawn, or the bridge is replaced. The gate accepts the **original bridge**, not a caller-fabricated public observation DTO. Phase 6.12's fused Review now has a package-private `fromBattle` check bound to its actual native fusion session.

`Session.inspect` returns a deterministic, privacy-safe report with verdict and only public/own numerical metadata. **CONSISTENT_READ_ONLY** requires a currently valid public lease and a still-current native fused report from the same battle object, exact same turn, supported pinned source declarations and a strictly new public source revision. A repeated revision, stale turn, foreign battle, unavailable public source, absent native source, expired own choice or revoked origin makes the session abstain and withdraw permanently. A previously returned positive review dynamically ceases to be current on later public revision, choice close or session withdrawal. A new Session is created only at a new battle boundary, never to mask mismatched evidence.

**Verdicts:** `CONSISTENT_READ_ONLY`, `PUBLIC_UNAVAILABLE`, `NATIVE_UNAVAILABLE`, `BATTLE_MISMATCH`, `TURN_MISMATCH`, `STALE_OR_REPLAYED`, `WITHDRAWN`. Abstentions export no source hashes, side strings, Pokémon UUIDs, nickname, request body, actor object, privacy-sensitive frame or opponent information. A consistent report is *not* a move ranking, completeness certificate or permission to influence the battle.

## Actual read-only call site

The existing `MaestroBattleAI.choose` calls `LivePromotionGate.choose(() -> chooseLegacy(...), null, null, false)` **before** observing 6.10–6.13. Phase 6.13 adds opt-in `-Dmaestroai.phase613Observe=true`, off by default; this also enables Phase 6.9 public observation and 6.10–6.12 native-own observations so the gate can examine their reports. Immediately **after** Legacy has selected, the bridge rechecks the same `battle.getShowdownMessages()` history under the installed Cobblemon version pin and compares that source lease with the current fused own report. Errors cause `UNKNOWN` diagnostic only; Legacy's selection and emitted response remain untouched. The gate and its public lease are withdrawn on the existing battle reset / transport-unavailable pathways.

## Explicit non-authority and unresolved integration constraints

All positive and negative reports unconditionally return `nativeActorAuthenticatedByPublic=false`, `sourceDeliveryAuthenticated=false`, `completeLegalSetProven=false`, `opponentActionsKnown=false`, `shadowSearchAuthorized=false`, `tacticalDecisionAuthorized=false`, and `livePromotionAuthorized=false`. Public nicknames/side labels, even if structurally valid, can be impersonated; a positive cross-stream report cannot certify their authenticity. Runtime installed legal actions, opponent choices, event-delivery completeness, source-signature verification, hidden information and general Cobblemon mechanics remain unverified. Full real battles may emit unsupported public event frames: **abstaining is correct**, not a malfunction to bypass.

No new runtime action, choice mixin, request polling, opponent-private read, trainer configuration, Shadow tactic invocation, Legacy scorer input or live promotion was introduced.

## Adversarial tests and CI acceptance

`CrossStreamEvidenceReadinessRegression` is registered in `StandaloneRegression` and covers identical public/native battle-turn fusion, normal/trapped/forced choice branches, deterministic safe reporting, permanently false authority, public revision replay, own lease expiry, public history advance and turn mismatch, history truncation and withdrawal, absent native/public source, cross-battle and caller identity substitution, source freshness, private `sideupdate` rejection, forged publicly displayed actor names remaining **unauthenticated**, and sticky session withdrawal. Prior Phase 6.1–6.12 regressions remain enabled.

**Verification policy:** public exact-commit Java/standalone and JAR checks plus full pinned-source oracle; pending oracle is not a pass. Minecraft testing remains REQUIRED before claiming installed-game acceptance, native request timing, original Showdown packet compatibility, or runtime effectiveness. FrAIL reviews at **end of Phase 6**, not after each batch.

**Safety:** commit only on `Maestro-6`. No merge into `main`, no Shadow promotion, Legacy retains control, no private Headquarters CI run (0 minutes triggered).
