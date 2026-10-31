# Phase 6.34 — Public Reveal and Recipient Evidence

**Branch:** `Maestro4567` · **Version:** 1.0.79 · **Scope:** optional read-only source-owned observation. **Minecraft untested.** No Phase 5 online consumption, tactical decisions, Shadow promotion or trainer changes.

## Native Cobblemon 1.7.3 source audit

From pinned `MayIHaveK/cobblemon-1.7.3`, independently inspected original Kotlin files:

1. `PokemonBattle.sendSidedUpdate(source, allyPacket, opponentPacket)` loops over **the source's own native side** with the ally packet, then over its **opposite native side** with the opponent packet. Spectators receive an additional branch. A method RETURN callback confirms only that the native method finished, **not actual client delivery, decoding, visibility, or acknowledgement**.
2. `SwitchInstruction` parses both public and private Showdown messages, resolves internal native Pokémon refs, and calls `sendSidedUpdate` with `BattleSwitchPokemonPacket` for each branch. It may display an **Illusion** Pokémon instead of the actual one. Neither public display nor an internally available Pokémon UUID is proof of a stable opponent Pokémon identity.
3. `ReplaceInstruction` ends Illusion by sending **`BattleReplacePokemonPacket(pnx, pokemon, true)`** to the originating side and **`BattleReplacePokemonPacket(pnx, pokemon, false)`** to the opponent side. Thus a native *public-branch intent* can be recognized by packet **class**, but packet content or hidden underlying Pokémon should not be exposed or treated as client-accepted public evidence.
4. `TransformInstruction` uses `BattleTransformPokemonPacket` analogously, subject to an actual native effect dispatch. `FormeChangeInstruction` uses form-change context and public chat; it does not independently authenticate the underlying individual across re-entry.
5. The existing Maestro `NativeSplitDispatchObservationMixin` already invokes `MaestroBattleAI.ownNativeSidedDispatchReturned` after native `sendSidedUpdate`. Reuse that callback **without any Mixin ABI change**. Phase 6.24 has original physical battle, source actor and native actor membership; Phase 6.31 separately anchors native opponent side; Phase 6.32/6.33 maintain read-only original public history and appearance freshness.

**Important limitation:** Even the exact native opposite-branch packet **was attempted/returned**, not independently received. No existing trusted per-recipient receipt on the installed source justifies a `true` for client delivery or actual revealed Pokémon identity. Do not infer this from the `false` visibility flag passed to a packet constructor.

## Implementation

`NativeOpponentRevealRecipientEvidence` introduces `SCHEMA = phase6.34.native-public-reveal-recipient.v1`, a battle-owned optional `BattleScope`, a native `Session.observeNativeReturn`, a source-binding `Session.inspect` and a redacted categorical `Report`.

- Opt-in `-Dmaestroai.phase634Observe=true` enables the existing Phase 6.24/6.31 observers and the Phase 6.34 **read-only** callback. No action/chooser hook was added. On error/choice-unavailable/battle end the observer withdraws. Nothing is activated by default.
- Accept **only** original source-owned callback references and a verified **two-actor, opposing-side `p1`/`p2`** native topology; the callback's source actor must be the *opponent*, so Maestro itself would be on the opposing/native public branch. Own-originated source callbacks abstain, and unrelated sided packet classes abstain rather than poisoning a later legitimate native event.
- Compare **exact packet classes**, without serialization, reflection into payloads or retention of packet objects. Categorize `SWITCH_PUBLIC_BRANCH`, `ILLUSION_END_REPLACE_PUBLIC_BRANCH`, `TRANSFORM_PUBLIC_BRANCH`, or abstain. This is source classification only, not proof of delivery.
- A callback Report `NATIVE_PUBLIC_BRANCH_RETURN_UNVERIFIED` asserts no Pokémon identity. Its currentness depends on the **original Phase 6.24 method-return receipt and source epoch**, not on a caller-supplied packet or matched turn number.
- Package-private `NativeOpponentSideProvenance.Report.sharesOriginalSidedReturn`, `ShadowLiveCompetitiveConsumerSafety.Report.sharesOriginalOpponentSide`, and `NativeOpponentPokemonContinuity.Report.sharesOriginalSide` require **exact original Phase 6.24/6.31/6.32/6.33 report object identity** and physical battle freshness. Reconstructed source reports, foreign battles, moved callbacks, replay, missing/stale identities and unresolved public appearances fail closed.
- **Only** a source-aligned ordinary **public switch appearance**, with 6.33's unambiguous-public-position-only receipt and matching 6.31 actor topology, can yield `PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED`. This is **not a persistent Pokémon, party or species identity**. A native `replace`/Illusion-ending reveal or Transform remains `REVEAL_IDENTITY_UNRESOLVED`, even if other source receipts match.
- All reports permanently deny client delivery, true Pokémon identity, Illusion/Transform source identity, historical Phase 5 joins, online Phase 5 use, Shadow search, actions, decision authorization and live promotion. Diagnostics export only enum classes and bounded numeric counters, no UUIDs, packet payload, actor names, species, replay identities, raw Showdown history, exact recipient identifiers or private event text.

## Adversarial verification

Registered `NativeOpponentRevealRecipientEvidenceRegression` exercises exact 6.24/6.29–6.33 source receipt chain, positive ordinary switch-public branch on both p1/p2 orientations, unsupported packet abstention, own-side source spoofing, foreign actor and multi-actor membership, foreign physical battle, same-clock/different-owner receipt mixing, missing original 6.33 evidence, duplicate/replayed report revocation, public history advance, lifecycle teardown and foreign end, private identifiers/packet export denial, actual native `replace`/Illusion and Transform intent remaining unverified and synthetic ambiguous Illusion/re-entry/form/multi-active withdrawals. It also asserts source-level Legacy chooser isolation and Phase 5 offline-only state.

The package-private `observeSyntheticReturn` deliberately accepts test-only categorical event kinds; it **is not** proof of a real Minecraft callback, real packet classes, client ACK, or visible species. The real production path classifies original native packet objects by exact installed classes.

## Promotion-readiness assessment for upcoming Minecraft test

The owner expressed interest in **promoting Shadow when the next Minecraft battle test happens**. This is intent to explore promotion, **not authorization to silently change live choice authority in Phase 6.34**. Before a production promotion, check exact-commit public CI, integration independence, installed Mixin activation, split/public privacy boundaries, fresh actor-side/identity evidence, complete installed move legality and fail-closed decision behavior, replay/mechanics compatibility and safety. Run the existing controlled Minecraft acceptance teams with Legacy authoritative and Shadow diagnostic first. If the runtime evidence is satisfactory and the owner **explicitly approves changing decision authority**, stage a separately authorized, reversible, feature-gated promotion with instant Legacy fallback, shadow-vs-legacy comparison, audit logs, rollback safety and adversarial tests. Do not assert Minecraft acceptance from synthetic tests.

**NO-GO:** online Phase 5, stable opponent Pokémon identity after Illusion/re-entry, authenticated client delivery, opponent build probabilities, Shadow tactical decisions or live promotion in this batch. These remain outstanding.

No `main` merge, private Headquarters CI, sensitive battle logs, trainer config changes, or Shadow authority switch.
