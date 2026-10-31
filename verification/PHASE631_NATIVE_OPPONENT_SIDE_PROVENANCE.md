# Phase 6.31 — Native Opponent-Side Provenance

**Branch:** `Maestro4567` · **Version:** 1.0.76 · **Scope:** optional read-only native observation with synthetic integration tests, no tactical or online Phase 5 consumption. **Prerequisite:** exact-commit Phase 6.30 full public CI must pass *before* this branch is advanced.

## Cobblemon source audit

Pinned Cobblemon 1.7.3 `PokemonBattle` contains `side1`, `side2`, `getActors()`, and `getActor(showdownId)`. Native `BattleActor` stores `showdownId` and `getSide()`. The `ShowdownInterpreter` uses the actor's Showdown side ID and active slot PNX (e.g. p2a) for its public protocol position. `PokemonBattle.sendSidedUpdate` uses native `source.getSide()` to distinguish ally and opponent packet recipients. **This proves the meaning of a native source-side mapping, not delivery or Pokémon identity.** No opposite-side guess from public nicknames or history is permitted.

`NativeSplitDispatchObservationMixin` already observes `sendSidedUpdate` RETURN and forwards its original actor/battle references to Maestro's read-only Phase 6.24 observer. Phase 6.31 reuses this existing optional observer, without changing the Mixin ABI or live chooser.

## Implemented source-owned contract

- Phase 6.24 `NativeSidedDispatchReachability.Report.matchesOriginalNativeActors()` checks the **original bound in-process actor identities**, exact source actor and current native callback epoch. Actor names, UUIDs, native refs and packets never leave the observer report.
- `NativeOpponentSideProvenance.Session.observeFromNativeCallback()` gets actual `PokemonBattle` and the owning native AI actor from that callback; reads `BattleActor.showdownId` and `getSide()` directly from Cobblemon. Requires exact two-actor, opposite-side `p1`/`p2` topology; validates `battle.getActor(id) == actor`, original physical battle identity and the same original Phase 6.24 source actor list. Multi-actor/unsupported topology abstains terminally; this never assumes a particular opponent Pokémon, per-slot identity, or one-versus-one battle format. Multiple active Pokémon on a **single side** remain a public appearance ambiguity under 6.29.
- The observer is off by default, activated only by `maestroai.phase631Observe` (which also enables the existing 6.24 diagnostic source callback). When requested, its native callback captures only an opaque opponent side `P1` or `P2`, not an actor object or opponent Pokémon. The Phase 6.31 source receipt is `NATIVE_SIDE_OBSERVED_UNLINKED` and does **not** export a public opponent side before later independent source correlation.
- `Session.inspect()` demands exact original Phase 6.24 RETURN report, exact Phase 6.28 integrated runtime original native source, exact Phase 6.29 identity continuity, and the exact 6.30 source-bound bridge receipt. Mere turn/revision/string equality or an independently reconstructed actor set cannot substitute source custody. Missing/stale/foreign/duplicate receipts withdraw or abstain as defined. Illusion/Transform, form/slot changes, unsupported multi-active uncertainty must not receive a positive 6.30 bridge report.
- Only `OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND` exposes the **native-owned opponent public side**, with strict dynamic source freshness; this is still not proof of authenticated recipient delivery, persistent Pokémon identity, a historical Phase 5 build match, calibrated probabilities, installed move legality or live tactical permission.
- Phase 5 consumer remains `OFFLINE_SHADOW_REVIEW`, source-derived historical appearances remain unjoined, and Shadow cannot submit actions.

## Adversarial regression criteria

Registered `NativeOpponentSideProvenanceRegression` covers: native p1-owner / p2-opponent and reversed p2-owner / p1-opponent; fake native actor, foreign callback source, additional actor, identical side objects, duplicate native labels, source receipt substitution, public history advancement, exact-epoch replay, cross-battle/mismatched source evidence, explicit end-of-battle withdrawal, Illusion, Transform, temporary/permanent form changes, switch slot reuse and multiple active positions. Checks bounded redacted diagnostics and that Legacy's chooser is not wired to this observer.

The package-private synthetic test intake deliberately cannot prove Minecraft callback installation, BattleActor.showdownId runtime correctness or actual delivery. Installed source validation must be separately approved and performed in Minecraft.

## Constraints / outstanding gates

**GO only for source-level, non-authoritative side-provenance integration** after the exact-head GitHub Actions succeed. **NO-GO** for live Phase 5 use, opponent team/party identity, opponent actual Pokémon or build identity, native split-to-client correlation, calibrated opponent behavior, installed gameplay acceptance, Shadow control or promotion. Source-dependent Phase 5 online usage requires a separately authorized Phase 5 contract change and independent FrAIL 2 acceptance review.

No `main` merge, private Headquarters CI, private battle logs, or trainer configuration modifications.
