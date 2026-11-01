# Shadow Promotion 3 — MaestroAI 1.1.1 (source implementation checkpoint)

**Development branch:** `Maestro4567`. **Owner authorization:** Shadow leads real battles; Legacy is emergency fallback. **No main merge.** This is a source acceptance note, not an installed Minecraft acceptance certificate.

## Authority and execution
- The native owning actor and per-battle emergency control are checked before an action may become authoritative. `maestroai.livePromotion.enabled` defaults to true. Operator disable is sticky for the current battle and resets only for a distinct battle ID.
- The promoted native decision pipeline first captures an independently sourced exact ordinary move/switch window. It then builds **its own** first-party BattleSnapshot and own move-template facts from the installed Cobblemon request and owning party. It does **not** call the Legacy tactical chooser to construct that state or rank candidates. Opponent private objects, held items, unrevealed moves, and unverified identities are not tactical inputs. Unsupported public opponent states remain UNKNOWN.
- Shadow ranks native-legal attacks, recovery, setup, source-verified self/field status, emergency voluntary switches and forced replacements. Exhausted ordinary move slots can still select an available first-party switch. A FORCED native own request can bind an empty fainted active slot through a unique, source-checked own active UUID; only one choice can consume the lease.
- Independent gimmick activation is sourced only from the current own `ShowdownMoveset` and chosen ordinary move slot: Mega, Ultra, Z, Tera, Dynamax. Native conversion target lists are inspected; the materialized `MoveActionResponse` and special availability are revalidated. No gimmick token is synthesized from Legacy's response.
- Only when a source/proposal is unavailable, expired, unsupported, or fails independent legality may **Legacy's chooser execute** as fallback. `LivePromotionGate` returns the actual authority; only returned LEGACY actions may enter LegacyChoiceCapture.

## Explicit limits
- The independent policy is a conservative first-party heuristic plus verified move metadata, **not a proven calibrated optimal battle planner**. Phase 4 supports source-verified mechanics metadata; Phase 5 historical hypotheses remain conditional and cannot be guessed from a hidden set; Phase 6 supplies native source custody; Phase 7 supplies nonauthoritative tests/attribution. Missing public evidence never becomes fabricated opponent certainty.
- Unattested special targets, multiple active slots, unsupported continuations and missing native requests fail closed to Legacy. Status effects dependent on a witnessed opponent action (such as Encore) do not use an unknown opponent.
- Retain all original safety/privacy gates; do not silently relax them to maximize the number of SHADOW log lines. Log authority markers explicitly and treat all Java/simulator tests as different from Minecraft evidence.

## Release
- Version: **1.1.1**, JAR: `maestroai-1.1.1.jar`, build property `archives_base_name=maestroai`.
- Exact-head run must pass **verify**, **pack_source**, **publish**, then the `maestroai-verified-<commit SHA>` GitHub Actions artifact must contain the versioned JAR and matching checksum. A candidate or failed run is not a verified release.
- Owner Minecraft battles occur **after** the fully promoted, verified JAR is delivered. No claim of Minecraft acceptance is made here.
