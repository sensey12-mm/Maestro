# Phase 6.10 — Native own-move evidence attestation (read-only)

**Branch:** `Maestro-6` **Version:** `1.0.55` **Source pins:** Cobblemon `1.7.3+1.21.1`; `OwnRequestTransport.SOURCE`; Phase 6.7 canonical action fingerprints.

## Native call-site and trust boundary

The existing pinned `OwnRequestDispatchMixin` receives `RequestInstruction.invoke$lambda$0` **after `BattleActor.setRequest`**, and `MaestroBattleAI.ownRequestDispatched` already checks that the request belongs to Maestro's *own* battle actor and real battle. When explicitly enabled with `-Dmaestroai.phase610Observe=true`, this method parses the same owning raw `|request|` message **once** for the existing `OwnRequestTransport` ledger and the additional read-only `NativeOwnActionEvidence` witness. No private request body is persisted or printed by the witness. No opponent request is queried.

A read-only `OwnRequestTransport.Lease.matchesOwnReadOnlyRequest` verifies the exact admitted parsed DTO, installed request **object identity**, battle and owning actor objects, side, active UUID, current dispatch generation, request mode and choice-turn freshness. The witness rejects a separately minted matching-looking request that was not admitted by the ledger.

At the existing Legacy-return observation site, Phase 6.10 independently inspects the installed `ShowdownMoveset.getMoves()` / `InBattleMove.canBeUsed()` and `ShowdownMoveset.getTrapped()` values, and compares them against a narrowly admitted own `active[0].moves` list (IDs, PP, disabled) and explicit `trapped` field from the original strict request. It supports ordinary single-active MOVE requests only. Unknown request fields, unavailable move slots, duplicate IDs, unsupported gimmicks, ambiguous trapping, forced replacement, wait mode or a disagreement with installed usability causes an abstention. The witness is one-time and requires an active choice lease before and after verification; it cannot survive a turn update or choice completion.

The optional Phase 6.9 read-only public bridge can store only a redacted **count** from a currently fresh native own-move witness, after checking battle object identity. This sidecar is observational and cannot back-fill an incomplete spectator protocol, infer hidden opponent data or supply actions to a chooser. If Phase 6.9 itself has withdrawn, the native source witness may still abstain or report separately without modifying Legacy.

## What is and is not attested

The precise, current **ordinary own move candidates** that BOTH the owning dispatched request and the independently installed moveset agree on are fingerprinted with `OfflineRuntimeEvidenceContract.actionFingerprint`, with disabled/zero-PP candidates excluded. A witnessed move set is **not a complete legal action set**: switching/forced choices, targets, gimmicks, other conditional effects, residual native validation and opponent responses remain unknown. Every report retains `completeLegalSetProven=false`, `opponentActionsKnown=false`, `tacticalDecisionAuthorized=false`, `shadowSearchAuthorized=false` and `livePromotionAuthorized=false`. This never upgrades offline Phase 6.7–6.8 synthetic fixtures to native legal receipts; no tactical consumer takes this witness.

## Regression and verification

`NativeOwnActionEvidenceRegression` is registered in `StandaloneRegression`. Cases: successful owner-side MOVE dispatch and source-pinned installed agreement, disabled/zero-PP suppression, request/actor/battle/side/UUID spoofing, wrong turn, owner dispatch replacement, lease expiration, source drift, ambiguous trap, unknown gimmicks, forced mode, duplicate/extra/missing installed moves, forged raw request, one-use revocation, 6.9 bridge battle isolation, and diagnostics without private UUIDs or raw request bytes. All preceding standalone suites remain registered.

**Minecraft testing required later:** Exact native modpack request shapes and installed `ShowdownMoveset` availability/timing must be confirmed in game before calling this bridge runtime-proven. No Minecraft testing is claimed for this development batch. Exact-commit public CI must pass both Java/standalone/JAR safeguards and the pinned source oracle; pending is not success.

**Safety:** Legacy still chooses all moves; no Shadow promotion, trainer configuration change, branch merge or private Headquarters CI runs. FrAIL's integration acceptance is scheduled for **the end of Phase 6**, not after every incremental batch.
