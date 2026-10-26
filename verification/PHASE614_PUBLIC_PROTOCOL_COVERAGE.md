# Phase 6.14 — Direct-public protocol coverage expansion and safety gates

**Branch:** `Maestro-6` · **Version:** `1.0.59` · **Boundary:** read-only, Shadow non-authoritative, Legacy controls choices.

## Source inspection and scope

Existing `PublicProtocolFilter` and `PublicEnvelopeProvenance` recognize a broad Showdown protocol allowlist, but the Phase 6.9 native observation bridge had admitted only direct one-event `switch`, `drag`, `turn` and `move` frames. This batch inspected those source files, `PublicProtocolEvidence`, the Cobblemon 1.7.3 pinned source-check conventions and upstream Showdown `sim/SIM-PROTOCOL.md` descriptions of `switch/drag`, `-damage/-heal`, `-status/-curestatus`, `faint`, `replace` (Illusion ending), `detailschange/-formechange` and `-transform`. The contract remains pinned to installed `1.7.3+1.21.1` and requires independent filter/provenance agreement.

`ExpandedPublicEventGuard` adds a deterministic, bounded parser for **single direct `update\n|event|...` instruction chunks**. Recognized event types:

- Switching and forced drag with observed actor, species details, and bounded HP+status.
- Ordered turn transition and on-field move, only by a currently observed non-fainted actor.
- Damage/heal with bounded current HP and unchanged observed denominator; matching revealed actor and non-inverted direction. Recognized `[from]`, `[of]`, `[silent]`, `[still]` source annotations are permitted only as public tags.
- Explicit status/curestatus transitions with exact known public status and active on-field actor.
- Faint only after a direct/public zero-HP observation for the same appearance.
- Illusion `replace`, `detailschange`, `-formechange` and `-transform` are structurally recognized but mark the **entire battle public-appearance identity ambiguous**. Once ambiguous, Phase 6.13 cannot obtain a new current public lease or a cross-stream readiness certificate, even if the source later shows another apparent actor. This avoids equating a public nickname/species/form with an installed Pokémon UUID.

Unknown events, multiple events in a chunk, private `sideupdate`, `split` with private/public halves, nested framing, partial packets, malformed actor/HP/status, reordered turn, previously unobserved actor, tampered append-only history, unsupported annotations, quota overflow and bad source-version checks **fail closed**. Public raw frames retained in the bridge are direct-allowlisted originals only (never sideupdate/split-private), already bounded by bridge limits. The source never emits raw frames, nicknames, IDs, item details or hidden requests in diagnostics.

**This is an intentional incremental subset of normal Showdown output.** Many real battle events (`upkeep`, weather, boost, multi-line batches, hazards, damage split packets and alternative HP encodings) still abstain and need separate source-level validation and Minecraft observations. A failed observation is not grounds to weaken routing restrictions. The native producer/source authenticity and completeness of actual installed Cobblemon event delivery are **not proven**.

## Verified regression hardening

The first exact-commit Java run failed because an older Phase 6.9 regression expected a forged on-field nickname at an already observed slot to remain syntactically eligible. That behavior is now correctly rejected **at the public observation boundary**, before downstream synthetic custody or Phase 6.13 readiness. The older Phase 6.9 and 6.13 tests are updated to assert the stricter fail-closed outcome rather than weakening the new parser. This is still local stream consistency, not cryptographic actor authentication.

## Preservation of previous trust boundaries

Phase 6.9's `probeSynthetic` for Phase 6.7–6.8 original fixture custody stays limited to the original narrow `switch/drag/turn/move` syntax; newly accepted public damage/status must NOT create an offline tactical receipt. Phase 6.13 source leases remain short-lived, bound to original battle object, revision, turn and currently unambiguous public appearances. The gate continues to offer only `CONSISTENT_READ_ONLY` or abstentions and never certifies public identity, native installed complete legality, tactical Shadow readiness, opponent decisions or live promotion.

Runtime behavior is opt-in only via existing `-Dmaestroai.phase613Observe=true` / `-Dmaestroai.phase69Observe=true`, off by default. All observations occur downstream of Legacy selection. The batch introduces no action chooser, simulator fallback, trainer edits, mixin, `main` merge, permission elevation or private Headquarters CI.

## Adversarial regression

`ExpandedPublicEventGuardRegression` covers direct public status/damage/heal/cure and ordered turn continuation, immutable old public-source lease, actor/HP/status mismatch, malformed or missing fields, invalid direction, faint-before-zero and move-after-faint, duplicate/skipped turns, nested update, unknown/private tags, unsupported public types, Illusion replace and actor swap, forme/transform ambiguity, private sideupdate/split/partial packet, append-only rewrite and redacted diagnostics. Previous Phase 6.9 regression is updated to recognize direct public damage while retaining its original synthetic-probe boundary. All Phase 6.1–6.13 regressions remain registered.

**CI gate:** exact-commit public GitHub Actions standalone regressions, JAR/safeguards, and full pinned source oracle must complete; a pending oracle is not success. **Minecraft tests remain required** to claim installed-game compatibility, native packet shapes or real battle acceptance. FrAIL audits only at the end of Phase 6.

**Headquarters CI:** no runs; zero minutes. **No `main` merge or Shadow tactical promotion.**
