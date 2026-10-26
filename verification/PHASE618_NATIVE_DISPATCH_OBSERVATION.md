# Phase 6.18 — Trusted Native Dispatch Observation Interface

**Repository:** `sensey12-mm/Maestro` · **Branch:** `Maestro-6` · **Version:** `1.0.63` · **Default:** off · **Authority:** none.

## Source audit and the exact trust boundary

The installed Cobblemon 1.7.3 dependency is pinned by the existing Gradle/source-oracle infrastructure to `curse.maven/cobblemon-687131/7553235`, compatible version `1.7.3+1.21.1`. The pinned project handoff and native API inspection identify these distinct operations:

- `ShowdownInterpreter` consumes two split messages (private first; public second), resolves the target `BattleActor`, constructs an interpreter instruction, and queues execution. Phase 6.16 verifies this **structural** history, not client delivery.
- `PokemonBattle.sendSplitUpdate(privateActor, publicPacket, privatePacket)` calls `sendUpdate` on each battle actor, supplying the private packet **only to the target actor**, and the public packet to other actors; it calls `sendSpectatorUpdate(publicPacket)`. This is the native recipient-routing *intent* boundary. Network delivery and client consumption happen beyond this method's observable return, and are not proven by it.
- Other paths, including `sendSidedUpdate`, `sendToActors`, and `sendSpectatorUpdate`, have distinct audience semantics. Phase 6.18 does **not** conflate those methods with a split return. Native `NetworkPacket` references must not be inspected or serialized for the audit.

**Important verification limit:** The Java build can verify native callable types against the installed dependency, but neither bytecode compilation nor a successfully returned dispatch method is a remote acknowledgment. Actual method injection matching and in-game callbacks require **Minecraft testing**. Until a genuinely trusted recipient-specific transport confirmation exists, recipient delivery is **UNVERIFIED**.

## Implemented, source-pinned observation

An optional `NativeSplitDispatchObservationMixin` observes `PokemonBattle.sendSplitUpdate` at `RETURN` with `require=0`: an incompatible/native-missing hook cannot break Legacy or native packet dispatch. It delegates only for the owning `AIBattleActor` whose AI is `MaestroBattleAI`. The new `ownNativeSplitDispatchReturned` callback is behind `-Dmaestroai.phase618Audit=true`, **off by default**. It never reads `NetworkPacket` contents, player UUIDs, spectator identities, opponent requests, private/public split lines, or tactical action state. Exceptions are swallowed only within this observational path.

`NativeSplitDispatchObservation.Session.observeNativeReturn` validates version pin, battle object identity, native turn and raw history-count monotonicity, active battle lifecycle premise, a distinct roster of 2–8 native actors, the private target occurring **exactly once** by object identity, separate nonnull public/private packet **references**, and a bounded spectator count. The session binds to the original battle object and issues only sequential, short-lived, per-return redacted metadata: dispatch ordinal, turn, historical chunk count, private-target actor count (1), other-actor count, and spectator count. No original actor/packet references, names, private text, UUIDs, raw events, packet contents or digests leave the call stack into the report or session. Prior reports expire on any new observed return, history/turn regression, lifecycle withdrawal, or reset; an invalid callback permanently withdraws the session. The installed metadata is not a signed transport attestation.

**What `NATIVE_METHOD_RETURN_OBSERVED` means:** the send method returned and the observed actor/packet arguments matched conservative source expectations. It DOES NOT mean individual actor `sendUpdate` calls succeeded, a socket sent or received a packet, a spectator viewed the packet, a client processed it, or that the method invocation corresponds to a particular Showdown split-history entry. Every report therefore unconditionally returns `networkSendAcknowledged=false`, `recipientDeliveryAuthenticated=false`, `clientMessageProcessed=false`, `originalSplitPayloadLinked=false`, `publicReadinessLeaseAuthorized=false`, `privatePayloadExportable=false`, `shadowSearchAuthorized=false`, `tacticalDecisionAuthorized=false`, and `livePromotionAuthorized=false`.

**Safety isolation:** Phase 6.9, 6.13, 6.16 and 6.17 remain structurally separate. No new recipient delivery trust is promoted into their readiness outputs. Legacy remains the only actual chooser, and Phase 6.18 does not call any tactical consumer. No game logic changes, destination packet rewrites, injected redirects, or additional private information access were introduced. The mixin is optional and observer-only.

## Synthetic regression and acceptance

`NativeSplitDispatchObservationRegression` is included in `StandaloneRegression`, preserving all prior suites. It exercises correct audience counts, source/ordinal freshness, old-record invalidation, separate physical battles, foreign/missing/duplicate actors, missing or aliased packets, source-version drift, invalid/negative/retrograde turns and history indexes, stopped battle, invalid spectator count, terminal withdrawal, synthetic private-text redaction, and non-joinability of Phase 6.17 structural split claims. Tests are synthetic Java contracts, **not** Minecraft recipient delivery tests.

**CI:** full exact-commit public GitHub Actions Java, standalone regressions, JAR safeguards and pinned-source oracle required before complete CI acceptance. A running oracle is not success.

**Still required:** installed Minecraft test to verify optional `sendSplitUpdate` mixin fires on the exact 1.7.3 runtime, that the native routing metadata represents actual method invocation, that lifecycle withdrawal happens reliably, and that no private text reaches logs. For true client receipt proof, an independent source-pinned acknowledged transport event (not currently observed) is additionally needed. Do not assert verified delivery without it.

**Safety:** commit only to `Maestro-6`, no `main` merge, no Shadow promotion, no trainer-config changes, no private Headquarters CI (0 minutes), FrAIL final review at end of Phase 6.
