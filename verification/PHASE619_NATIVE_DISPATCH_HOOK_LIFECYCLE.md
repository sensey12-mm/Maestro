# Phase 6.19 — Exact-Bytecode Native Hook and Recipient Lifecycle

**Branch:** `Maestro-6` · **Version:** `1.0.64` · **Default:** observation opt-in only · **Authority:** none.

## Pinned binary verification

The canonical Java 21/Fabric dependency remains `curse.maven:cobblemon-687131:7553235`, source/oracle SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`. Existing `verification/runtime_hook_oracle.py` previously checked **method names only**. Phase 6.19 adds a deterministic JVM classfile constant-pool/member-table descriptor check directly against these checksum-verified bytes:

- `PokemonBattle.sendSplitUpdate(BattleActor,NetworkPacket,NetworkPacket):void`; `NativeSplitDispatchObservationMixin` now specifies the **full exact JVM method descriptor** at `RETURN`.
- `PokemonBattle.sendUpdate(NetworkPacket):void` for the native `BattleEndPacket` broadcast. The same optional mixin observes that method at `HEAD`, withdrawing the read-only observer before the native battle closes.
- The oracle checks the corresponding exact Mixin selectors and that both injections remain `require=0`. A signature change fails the public source gate rather than being mistaken for evidence. No synthetic class or unchecked remote download can replace the checksum-verified JAR.
- `verification/check_native_split_hooks.py` runs that binary check **early** in public GitHub Actions, immediately after JAR safeguards, and the full source oracle independently repeats the check. Synthetic classfile tests exercise descriptor drift, truncation, and modified UTF-8 constants.

This verifies binary **method declarations and handler selectors**, not Mixin injection success, callback frequency or recipient delivery in Minecraft. In-game runtime observation is still mandatory before claiming that both optional injections applied correctly.

## Read-only lifecycle hardening

`NativeSplitDispatchObservation.Session` now:
- Binds an original battle object and privately retains the initial actor **object-identity roster only while that battle is observed**, clearing references on withdrawal. Any lost/substituted actor invalidates the recipient observation even when the count is unchanged. No identities, original packet references, packet contents or player UUIDs enter a report or log.
- Tracks monotonic native turn/history count and a sequential single-current dispatch ordinal; all older reports expire after a new accepted return.
- **Fails closed on same-turn/same-history return ambiguity.** This could be a genuine second native packet, a duplicated Mixin callback or a replay. Without a distinct source-attested dispatch sequence, the observer cannot safely tell. It withdraws instead of counting apparent duplicates as independent recipient evidence.
- Provides `status(...)` so absence of any optional native callback is explicitly `NO_NATIVE_CALLBACK_YET`, never a successful delivery/custody report. The existing read-only Showdown-history observation path can log `dispatch-hook=UNOBSERVED`; no battle tactics use the result.
- Withdraws on `BattleEndPacket` broadcast, source-version drift, battle/roster mismatch, turn/history rollback, battle memory reset or transport-unavailable. The native battle-end observer reads **only the packet's class simple name**, not serialized or private data. The callback uses an optional Mixin so failure to apply never interferes with native battle termination.
- Keeps `networkSendAcknowledged`, `recipientDeliveryAuthenticated`, `clientMessageProcessed`, `originalSplitPayloadLinked`, `publicReadinessLeaseAuthorized`, `shadowSearchAuthorized`, `tacticalDecisionAuthorized` and `livePromotionAuthorized` permanently false, including after a valid method return.

## Regression and verification acceptance

`NativeSplitDispatchObservationRegression` now tests source/ordinal advancement, ambiguous same-epoch repeats, allowed same-turn *new-history* returns, roster replacement without count change, order-insensitive **same-identity** rosters, battle-end withdrawal both before and after any dispatch callback, stale lifecycle polling, absent hook abstention, private-data redaction and all previous Phase 6.18 source/recipient mismatch scenarios. The pinned binary hook oracle tests signature drift and malformed class bytes. All earlier standalone regression suites remain registered.

**CI acceptance:** exact-commit public GitHub Actions must pass Java/standalone, JAR, the new checksum-pinned native Mixin ABI preflight and the full external-source oracle. The long oracle may remain running; do not call that full success.

**Minecraft testing required:** verify the two `PokemonBattle` optional injections apply at runtime in the installed modpack, correct startup class transformations, split packets trigger return callbacks with correct target identity, actual battle-end packet triggers observer withdrawal, normal multiple packets are conservatively abstained on, no hidden request/private packet leaks, and Legacy battle behavior remains unchanged. Neither static bytecode verification nor synthetic tests can prove client reception; a separate authenticated recipient transport acknowledgment is still unavailable.

**Safety:** Legacy live control only; Shadow cannot choose or promote. No `main` merge, no trainer configuration edits and no Headquarters private CI. FrAIL independent full Phase 6 integration review remains at Phase 6 completion.
