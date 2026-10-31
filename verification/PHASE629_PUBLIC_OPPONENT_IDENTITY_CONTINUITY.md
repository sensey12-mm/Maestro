# Phase 6.29 — Public Opponent Identity Continuity

**Target:** `Maestro4567` · **Build version:** 1.0.74 · **Nature:** source-level, read-only, synthetic. **Phase 6.28 exact-commit CI must be completed and green before advancing the branch for this phase.**

## Source-verified semantics

Pinned Cobblemon 1.7.3's `ShowdownInterpreter` accepts `switch` and `drag` as split public/private events, and `replace`, `-transform`, `detailschange`, `-formechange` as update instructions.

- `SwitchInstruction`: public active `p1a`/`p2a` identifies a **battle position**, not the immutable Pokémon identity; the public appearance can be an Illusion. Private-first split branch must never be consumed to learn true identity.
- `ReplaceInstruction`: `|replace|POKEMON|DETAILS|HP STATUS` represents **Illusion ending**; it does not prove an original historical replay opponent's identity.
- `TransformInstruction`: `|-transform|POKEMON|POKEMON` changes an entity's battle appearance via `TransformEffect`; the transformed target is not the source Pokémon's stable identity.
- `FormeChangeInstruction`: `detailschange` and `-formechange` reflect permanent/temporary form changes, not cryptographic party continuity.
- Phase 6.16's original `PublicSplitRoutingGate.Report` has an append-only, per-session **salted digest of every original history chunk**, including private halves and sideupdates. It distinguishes original history from caller-reconstructed lookalikes.

Cobblemon source: `MayIHaveK/cobblemon-1.7.3` `ShowdownInterpreter.kt`, `SwitchInstruction.kt`, `ReplaceInstruction.kt`, `TransformInstruction.kt`, `FormeChangeInstruction.kt`. This is pinned source inspection, **not** installed Minecraft acceptance.

## Implemented source contract

`PublicSplitRoutingGate.Report.matchesOriginalHistory(battle, history)` is package-private, returns only a boolean, and compares the caller-supplied history with the ORIGINAL still-current salted digests inside the original session. No salt/digest/raw event is exported. A forged public form, identity/nickname, public event, or changed private split half fails the match, even if source revision, event count and turn are unchanged.

`PublicOpponentIdentityContinuity.Session.inspect` requires:
- One original physical battle object, one current original 6.16 source-issued report, exact original source history, valid Cobblemon version pin, monotonic source revision and non-regressing public turn/chunk count.
- Independent `PublicProtocolFilter` extraction and `PublicEnvelopeProvenance` reconciliation of public events. Never use `sideupdate` requests or a private split half. No public actor name, species, raw frame, private payload, actor object, UUID or historical Phase 5 appearance is retained.
- Both sides are kept as **public protocol positions only**, without declaring whether `p1` or `p2` is the owner's opponent; in doubles, each active `a/b/c` is separate.
- Switch/drag to the same position is `SLOT_REUSED_NOT_POKEMON_REENTRY`, **not** proof of a particular Pokémon returning. `replace`, Illusion `-ability`/`-end`, `-transform`, and form changes record permanent uncertainty, not identity restoration. Missing entry/unsupported sequences fail closed.
- Reports expose only bounded numeric counts and categorical uncertainty, expire upon source revision changes or session withdrawal, and ALWAYS deny persistent Pokémon identity, native/public actor authentication, Phase 5 historical replay joins, opponent legal-action knowledge, Phase 5 online use, Shadow tactical search or promotion.
- `shadowOnlyUncertainty()` can translate public form/Illusion/Transform ambiguity into a conservative Phase 6.28 **uncertainty label**. It never authorizes a live Phase 5 consumer or joins a historical appearance to this battle.

## Regression coverage

New `PublicOpponentIdentityContinuityRegression` is registered in the full standalone runner. Hand-authored synthetic fixtures cover original source-bound split switches, normal same-source idempotence and consumer replay revocation, forced switch/drag, slot reuse, Illusion reveal, Transform, both form-change kinds, Illusion termination, multiple active slots, malformed/no-entry history, source withdrawal and source revision expiry, wrong physical battle, wrong native version pin, forged public appearance with identical source metadata, private split-half mutation, redacted diagnostics and Legacy/Phase 5 offline-only separation.

**No-go** for live opponent identity, stable historical build matching, current Pokémon/party-slot assertions, a certified online Phase 5 consumer, calibrated opponent probabilities, installed legal choices, Shadow tactical actions, network delivery and gameplay authority.

**Remaining for later separately authorized validation:** original in-game native opponent actor identity and side mapping, Illusion/Transform/re-entry evidence after installed source verification, tagged public appearance continuity, runtime test in actual Minecraft, and FrAIL 2 independent integration review. No Minecraft test is claimed here. Legacy live control, Phase 5 existing offline-only restrictions and Shadow non-authoritative mode are unchanged.

**Governance:** No `main` merge, no Phase 5 contract relaxation, no private Headquarters CI and no owner private battle logs or identifiers committed.
