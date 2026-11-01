# MaestroAI — Shadow Promotion 1: Independent Live Decision Integration (v1.0.99)

**Branch:** `Maestro4567`; **source baseline:** `1.0.98`; **promotion authority:** disabled in production.

## Integrated source-backed controller

`ShadowLiveDecisionEngine` is a new independent choice policy, not a wrapper for Legacy's selected action, score, or opponent-internal knowledge. It is connected after the current Legacy fallback calculation to `MaestroBattleAI.choose` via `ShadowPromotionPreparation.inspectIndependent` and a native-owning source lease. It does **not** call `chooseLegacy`, consume Legacy's tactical scores, read a private opponent party, or construct opponent move probabilities.

The existing stricter Phase 4/6 witnessed-reply comparison has priority when it produces a complete source-certified strict preference. Otherwise, `ShadowLiveDecisionEngine.ordinary` uses a *transparent deterministic heuristic* over only source-verified `CobblemonMechanics.move` data, verified `MoveEffectModel` effect annotations, own public boost/HP observations, and publicly confirmed opposing types. It supports attack and priority choices, witnessed typing immunities, recovery, annotated boosts, some verified status/hazard actions, and conservatively gated voluntary switches. Unknown or unsupported move effects have **no fabricated utility** and are never selected as if modeled.

**Crucial distinction:** power, priority, native accuracy and known typing are heuristic ordering inputs, not an independently calibrated damage distribution or a proof of battle superiority. The opponent's unrevealed moves, ability, item, build or response policy remain UNKNOWN. A strong independent policy needs subsequent genuine Minecraft evidence and broad tactical evaluation to demonstrate competitive quality.

## Forced replacement

`ShadowLiveDecisionEngine.forcedReplacement` is an independent first-party own-HP preservation policy for currently eligible reserves. It picks the reserve with the greatest known own HP fraction, with party-order tie breaking. `MaestroBattleAI.prepareShadowForcedReplacement` binds this to the current owning actor, exact request identity, original battle/turn and `OwnRequestTransport.Lease`, then re-checks that the selected reserve remains eligible. No private opponent state or guessed favorable matchup participates. Missing request, empty party, stale receipt, duplicate eligible identity, ended battle or unsupported raid mode abstain to Legacy.

## Runtime security boundaries and action scope

* The normal policy only accepts a `ShadowNativeLegalWindow.Proof` where every move/switch matches the original Cobblemon request and a current owning lease. The public side must be complete, framed, not corrupted, current-turn, nonambiguous on reentry, and not have an opponent private bench.
* Forced replacements deliberately have their own independent native forced-request receipt, separate from the ordinary legal-root proof.
* All proposals remain bound to source freshness, original physical battle, original request object and turn, and are discarded when `choose` returns.
* The promotion gate remains `LivePromotionGate.choosePrepared(..., false)`. Legacy remains the live authority in this batch. Existing native response materialization and fail-safe emergency control are unchanged.
* Special gimmick activation is NOT automatically chosen by this new policy. Unsupported complex move effects, response distributions and disputed battle situations remain handled through Legacy fallback. No silent privacy/safety relaxation is permitted.

## Regression coverage

`ShadowLiveDecisionEngineRegression` is included in the existing `StandaloneRegression` entrypoint. It exercises independent move priority, verified public typing immunities, low-HP recovery, high-HP vs low-HP recovery restraint, boost-stage abstention, supported status/Encore and missing public reply, unknown effect abstention, trapped voluntary switch, bench mismatch, duplicate legal moves, forced replacement, tie ordering, invalid/fainted/duplicate own targets, source expiration and the hardcoded production promotion-off topology.

The existing Phase 4/5/6/7 source, online privacy, native ABI and fallback safety regressions remain mandatory. No local Minecraft runtime test or live Shadow choice is claimed.

## Verified public CI timeout investigation

The prior `1.0.98` verify job failed **only after** the native differential suite exhausted the `source_oracle.py` 2400-second subprocess cutoff. Earlier Java/JAR/Mixin checks succeeded and multiple pinned differential sections passed. The still-required native oracle was not skipped: its finite subprocess budget was expanded to 5400 seconds, with a 95-minute workflow guard. This correction requires exact-commit CI verification on `1.0.99` before treating the build as verified.

## Next owner-visible milestone

After exact-commit Java/source checks and native differential results are verified, finish full activation readiness: audit and separate current snapshot generation from Legacy's evaluation path, verify forced-faint and special action materialization with original simulator requests, ensure final native legal-action validation for every selected action, preserve phase7 Legacy/Shadow attribution, and only then intentionally change the source approval gate in the owner's already authorized next promotion batch.

**Do not describe this stage as a completed full promotion or an accepted Minecraft battle.**
