# Phase 6.33 — Native Opponent Pokémon Continuity Evidence

**Branch:** `Maestro4567` · **Version:** 1.0.78 · **Goal:** identify *source-verified public appearance windows* without leaking native hidden Pokémon identities or inventing switch/re-entry continuity. Source- and CI-level work; Minecraft UNTESTED.

## Native source investigation

Pinned Cobblemon 1.7.3 source (`MayIHaveK/cobblemon-1.7.3`):

- `BattleMessage.pnxAndUuid()` parses an internal PNX + **UUID** identifier, and `BattleMessage.battlePokemon()` resolves it to the original native `BattlePokemon`. That is internal server knowledge, **NOT proof that an opposing player publicly learned the true Pokémon identity**. Never export, hash as a stable public ID, or join it with a historical replay identity.
- `SwitchInstruction` reads both `publicMessage` and `privateMessage`; it uses the private branch for HP. `publicMessage.battlePokemonFromOptional(..., "is")` identifies an Illusion; the outgoing packets have different ally/opponent contexts. `broadcastSwitch()` displays `(illusion ?: newPokemon)`, meaning the visible Pokémon may differ from the actual Pokémon.
- `ReplaceInstruction` ends Illusion; `TransformInstruction` constructs a Transform effect using the target Pokémon; `FormeChangeInstruction` updates form via `detailschange` or `-formechange`. These messages do **not** prove historical individual identity or persistence across switch and re-entry.
- A Showdown public `p1a/p2a` designation is an **active position**, not a Pokémon slot, nickname, ability, item, stable UUID or historical build fingerprint.

## Implemented contract

`NativeOpponentPokemonContinuity` adds a read-only `Session.inspect(battle, consumer632, originalRouting616, originalHistory)`, a `Report`, and a bounded, pure public-appearance event scanner:

- The **exact original 6.16 routing Report** must be the same object admitted by 6.29, 6.30, 6.31 and 6.32. New package-private original-receipt checks in `PublicOpponentIdentityContinuity.Report`, `NativeOpponentSideProvenance.Report` and `ShadowLiveCompetitiveConsumerSafety.Report` bridge this custody without exposing the original native actor, salted digest, private payload or UUID.
- `originalRouting.matchesOriginalHistory` checks the original salted full-chunk digests (including private chunks); public-only reconstructed data and forged private halves cannot claim source receipt identity. Both pre- and post-parse source checks are enforced.
- The opponent side comes **only** from the current native-owned 6.32 report; never from a caller-provided p1/p2 label or a heuristic about displayed Pokémon.
- Public event extraction remains `PublicProtocolFilter` plus `PublicEnvelopeProvenance.reconcile`. Private split halves, requests and sideupdate payloads are never treated as public observations. The parser counts relevant opponent-side active entries and public activity, but does not retain any raw event, native Pokémon object, UUID, nickname, Pokémon species, historical appearance ID or move name.
- A simple single-position appearance can produce `PUBLIC_APPEARANCE_POSITION_ONLY_UNBOUND`: **this confirms only a public active position during a fresh source epoch, not the Pokémon occupying it**. Source reports are dynamically invalidated on upstream source advancement, callback revision/ordinal change, battle end or withdrawal. Duplicate reports, wrong battles, caller-constructed history, stale/missing references fail closed.
- Repeated switches, forced switch/drag, re-entry into a slot, Illusion `replace`/`-end`, Transform, form changes, faint/replacement and multi-active positions are classified **uncertain**. Never link two appearances by matching nickname/species, hidden UUID, native Pokémon object, pnx or active slot. Without an authorized original 6.32 source chain, no positive 6.33 report is obtainable; even its pure scanner cannot mint original source custody.
- Every report permanently denies true Pokémon identity, cross-switch/re-entry link, Illusion/Transform source identity, historical build joins, Phase 5 online consumption, opponent tactical probabilities and live decision authority. No Phase 5 consumer, Legacy chooser or trainer configuration was changed.

## Regression coverage

`NativeOpponentPokemonContinuityRegression`, added to the standalone suite, checks original signed Phase 5 offline descriptor, exact original Phase 6.16/6.24/6.28–6.32 receipt chain, positive public p1/p2 appearance, same-position public activity, source replay, foreign physical battle, matched-but-independent routing receipts, private split-half mutation, source revision expiry, late battle ends, explicit withdrawal, forced switch, repeated switch/re-entry, Illusion reveal/end, Transform, permanent/temporary form change, doubles/multi-active position, faint, private/UUID/nickname redaction, and production chooser isolation.

## Acceptance and blockers

- **GO:** source-level, non-authoritative diagnostics of *public active-position observations* only, after passing exact-commit CI.
- **NO-GO:** authenticated native *opponent Pokémon* identity, cross-switch identity join, historical opponent build or appearance association, in-battle Phase 5 candidate predictions, live Shadow use, recipient-delivery proof, installed legal move knowledge or Legacy promotion.
- **Remaining source gap:** Installed Cobblemon callback/public recipient evidence does not yet independently establish that a native Pokémon UUID is opponent-public, including the Illusion/re-entry case. A future separately authorized, privacy-reviewed **public reveal/recipient witness** may enable a stronger identity contract; the current interface must abstain instead.
- **Minecraft testing needed** for installed native callback activation, correct opponent side/slot semantics, switches, Illusion, Transform, re-entry and lifecycle. No in-game test claimed. FrAIL 2 independent integration review remains outstanding.

No `main` merge, no Headquarters CI, no online Phase 5 permission change, no Shadow promotion, no private battle log or Pokémon trainer config modification.
