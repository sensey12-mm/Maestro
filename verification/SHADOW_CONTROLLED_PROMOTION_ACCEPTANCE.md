# MaestroAI 1.0.98 — Controlled Shadow Promotion, Failsafe and Minecraft Validation Plan

**Branch:** Maestro4567. **Code-only batch.** This document is a test plan, not a claim that tests happened inside Minecraft.

## Actual implemented policy

* Legacy still computes the authoritative decision once. In production the call to `LivePromotionGate.choosePrepared` ends with the literal `false` owner-approval argument, so no JVM flag, historical archive, test result or opponent data can authorize live Shadow choices.
* Independently of the Shadow snapshot, Maestro captures the exact own active request (own actor, original request object, current turn, trapped/forced/continuation status), available native moves and currently eligible own reserve UUIDs. Capture is restricted to source-pinned, ordinary, single-active Cobblemon 1.7.3 battles. Missing source, mismatched native trapped flag, forced replacement, request already answered, unsupported special action and stale actor/battle/turn fail closed.
* The new `ShadowNativeLegalWindow` separately compares the entire ordinary native-root set with the Shadow root. A separate current `OwnRequestTransport.Lease`, `RuntimeDomain`, `ScopedExecution`, complete public protocol and independent Phase 4/6 witnessed-reply **and current modifier-envelope** audits must all be valid. Ad hoc single-reply comparisons no longer qualify for promotion preparation. Missing original public modifier evidence or unmodeled game effects remain UNKNOWN; no fabricated replies or probabilities.
* Only an all-root-bounded **strictly dominating conditional** Shadow preference may become a request-leased *proposal*. This is NOT proof of its expected outcome against all hidden opponent choices, Phase 7 integrated performance superiority, or current opponent Pokémon identity. Phase 5 Online historical unbound roles never enter this chooser.
* The prepared proposal still cannot execute until **both** a separate owner-approved source change and the enabled runtime setting exist. The `LivePromotionGate.Control` is a per-trainer sticky fail-safe: a rejected attempted promoted action, changed source, invalid materialization or operator revocation permanently trips the control for that instance. A late disabled runtime flag also causes Legacy fallback. The gate revalidates current source and independent response legality immediately before returning a proposed action.
* Existing Cobblemon action materialization checks the current own active request, move canBeUsed, forced-choice/trapping and continuation rules, exact eligible bench UUID, and absence of duplicate candidates. Any mismatch retains Legacy's already selected action. Source-bound evidence and temporary request/candidate references are discarded on chooser exit. No unsupported mega/Z/tera/dynamax actuation is assumed to be certified.

## Verified by CI (when the final exact-commit run finishes)

The unchanged standalone regression registry now includes adversarial cases for missing/duplicate/extra native actions, forced switch/charge continuation, trapped reserve, stale battle/request/turn, legal-window expiry, missing mechanics/coverage, uncertain opponent/unknown outcomes, wrong branch, unstable sources, materializer exceptions, illegal candidates, emergency revocation **during** materialization, inability to restore a revoked control using a JVM flag, and fail-safe fallback. Existing Phase 4/5/6/7 suites, pinned simulator source oracles, Cobblemon Mixin ABI and SHA-verified final artifact publication remain mandatory.

**No exact-commit success is presumed for v1.0.98 in this note.** Check both `verify` and `pack_source` plus `publish`, and use only the final `maestroai-verified-<full SHA>` artifact for this commit—not the `candidate` artifact.

## Minecraft acceptance plan — reuse the existing 4 teams / 8 battles

For the first acceptance pass, install only the exact verified MaestroAI v1.0.98 JAR, retain Cobblemon 1.7.3 / Fabric / RCT compatibility, and **keep promotion disabled**. Use the existing BrAI teams/builds—do not make duplicate /pokegive commands when a Pokémon's build is unchanged. Capture privacy-sanitized server diagnostics and source/JAR SHA. Evaluate whether a conditional candidate is prepared, why it abstains and whether Legacy still chooses normally.

| Battles | Existing team | Purpose |
|---|---|---|
| 1–2 | Trapping & recovery | Shadow Tag / Encore, rejected switch, trapped move, faint and forced replacement; must not submit illegal alternatives |
| 3–4 | Normal actions | Compare attack/setup/recovery/switch proposals with Legacy; in battle 4 keep diagnostics OFF to confirm Legacy isolation |
| 5–6 | Field & switching | Hazards, weather/terrain, pivot and voluntary/forced switch, fresh request generations and lifecycle expiration |
| 7–8 | Identity & uncertainty | Unovan Zoroark must disguise as **Greninja**: place Greninja last eligible in party; test Illusion, Transform, re-entry, reveal and battle end; unknown state must abstain |

**Acceptance evidence:** the `ShadowPromotionPreparation[verdict=...;proposal=...;authority=NONE]` diagnostic, Phase 3 conditional preferred-vs-Legacy comparisons, legal-action safety, current-source withdrawal and Legacy fallback. A lack of positive proposals is an honest UNKNOWN or unsupported case, **not a test pass for tactical advantage**. Testing should not solicit battle-loss recaps.

**Actual Shadow choice testing must come later, after the user explicitly authorizes activation.** At that time the hardcoded owner approval and independent runtime acceptance policy can be reviewed and intentionally changed, with a kill switch and staged Legacy fallback. There is **no claim** that the current build has fought a battle as Shadow, and no change to `main`.
