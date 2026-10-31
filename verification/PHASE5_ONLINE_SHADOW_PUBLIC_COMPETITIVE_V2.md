# Phase 5 Online Integration — Shadow-Only Public Competitive Intelligence (v2)

**Repository:** `sensey12-mm/Maestro` · **Branch:** `Maestro4567` · **Version:** 1.0.81 · **Status:** online *public capability diagnostics* integrated in native observation callback, **disabled by default**, awaiting exact-commit public CI and Minecraft runtime evidence.

## What is genuinely online

New `PublicCompetitiveLiveShadowConsumer` implements a distinct pinned `phase5.online-shadow-public-evidence.v2` contract. The original signed-historical Phase 5.19/5.21 offline-only consumer has **not been changed** or turned into an online source of opponent movesets.

Opt-in `-Dmaestroai.phase5LiveShadowObserve=true` on the installed Java process activates existing Phase 6.24 and 6.31 **read-only native callback observation** and the Phase 5 v2 diagnostic on Cobblemon `sendSidedUpdate` RETURN. On each callback, Maestro uses the actual original `PokemonBattle.getShowdownMessages()` source history, constructs a fresh original Phase 6.16 structural routing report, a 6.29 public appearance uncertainty report, and the exact native-owned 6.24/6.31 actor-side report. Only a source-pinned, original history-and-battle-consistent callback can produce a positive diagnostic. No move selection call or response is changed.

The public Phase 5 v2 diagnostic recognizes ONLY **already witnessed public opponent moves** as possible capabilities. It uses the source-pinned `MoveEffectModel` whitelist, never fabricated learnsets or opponent moveset guesses. Outputs are categorical, not private names: witnessed hazard-set, hazard-clear, setup, pivot, recovery, forced-switch-move, move control, defense, other verified effect, or `PUBLIC_MOVE_EFFECT_UNKNOWN`. For example, seeing `Stealth Rock` publicly can support `HAZARD_SET_OBSERVED`; the unmodeled `Toxic Spikes` effect is explicitly `UNKNOWN` instead of being mis-scored as harmless. Unseen moves/items/abilities are *not* inferred.

### Original source and privacy gates

- `NativeOpponentSideProvenance.Report.nativeOnlyOpponentPublicSide(battle,original6.24)` returns native `p1/p2` only for its **identical original native callback** and actor set. This verifies the opposite Showdown side *only*, NOT true Pokémon identity, a party slot or a 6.32 fully accepted source receipt.
- `PublicSplitRoutingGate.Report.matchesOriginalHistory` compares original source-owned salted full-history digests, including private split halves, before any public projection is analyzed. The projection is extracted only by `PublicProtocolFilter.publicEvents` and `PublicEnvelopeProvenance.reconcile`. Private branch and opponent request are never a live input or exported text.
- `PublicOpponentIdentityContinuity.Report` must be current, admit the exact original 6.16 routing instance and return `PUBLIC_POSITIONS_ONLY_UNBOUND` with only `PUBLIC_DISPLAY_ONLY` uncertainty. Illusion, Transform, re-entry, form changes, multiple active positions and public event/side inconsistencies ABSTAIN. If source evidence is withdrawn, old positive reports dynamically expire.
- The per-battle consumer additionally checks the native callback ordinal, history chunk/turn monotonicity, exact physical battle identity, bounded frames and registered purpose. No duplicate source epoch can mint another positive observation. A missing initial source yields a non-authoritative abstention; missing subsequent source or contradictory/foreign evidence withdraws for the battle.
- Diagnostic reports retain only original source receipts (not private event bytes), categorical capability labels, bounded counts and numeric epochs. No Pokémon names, species, UUID, opponent hidden item, raw event, move name, candidate build, historical replay identity or private packet escapes.

### Deliberate boundary: what is NOT yet online

This is **live Phase 5 public-information capability observation**, not full competitive opponent model activation. Phase 5's previously signed offline historical **candidate builds, roles and replay tendencies remain OFFLINE and UNBOUND**. No current opponent stable Pokémon identity or authorized online import of those historical hypotheses exists; Phase 6.32/6.33 properly refuse to invent it. A diagnostic category from an *observed* move is not a future-move forecast or calibrated probability. This distinction matters for Kingambit/Glimmora tests. If publicly observed capabilities are unsupported, report `UNKNOWN`; never silently assume a move cannot happen.

No Shadow action recommendation, Phase 6 search candidate, tactical score, Legacy priority, trainer configuration, promotion switch, or runtime mechanics assumption is added. The existing `LivePromotionGate.choose(legacy, null, null, false)` remains intact.

## Adversarial verification

`PublicCompetitiveLiveShadowConsumerRegression`, registered in `StandaloneRegression`, covers original native side p1/p2 orientation, supported publicly observed Stealth Rock/set-up/recovery capabilities, unsupported Toxic Spikes effect UNKNOWN, no hidden predictions, missing original source, version/purpose/hidden-state/history-join refusal, same-battle lookalike routing report substitution, original private-half mutations, foreign physical battle, replay/duplicate source revocation, historical append freshness, battle-end withdrawal, Illusion reveal, Transform, form changes, repeated switches and multi-active uncertainty, bounded redacted outputs and source-level Legacy choice isolation.

Regressions use **synthetic source-owned fixtures only**. Public exact-commit CI must pass Java, JAR, native Mixin ABI and pinned simulator-source oracle at the exact version 1.0.81 SHA. A native callback in the source is not evidence that an optional Mixin really executes in Minecraft.

## Minecraft acceptance required

Install the exact successfully verified JAR on existing Minecraft 1.21.1 / Cobblemon 1.7.3 / RCT 0.15.2-beta / Fabric / Java 21. On an authorized test, observe a real opponent **public** move against Maestro; enable diagnostic opt-in through the JVM property and confirm the Phase 5 v2 source report's correct opponent p1/p2 orientation. Verify supported capabilities, unknown effects, turn/epoch freshness, public/private filtering, and battle-end withdrawal. Test the existing Unovan Zoroark disguised as Greninja (Greninja last eligible), Transform, repeated switches, multi-active and unsafe/private-framed events and confirm explicit abstention. Repeat once without diagnostics and verify Legacy choices are identical in equivalent fixtures. Never claim clients received a packet based on method return.

**GO:** source-level, non-authoritative online public capability diagnostics when exact commit CI succeeds. **NO-GO:** historical Phase 5 builds online, hidden opponent sets, calibrated probabilities, independent Shadow tactical decisions, Shadow authority or live promotion. Source-only CI is not Minecraft acceptance or proof Shadow now beats Legacy.

No `main` merge, Headquarters private CI, manual Cobblemon updates, trainer roster modification or Shadow authority change.
