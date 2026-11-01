# Shadow Promotion 2 — Primary Live Selection (MaestroAI v1.1.0)

**Branch:** `Maestro4567`. **Owner authorization:** full live Shadow promotion. **Main merge:** not authorized. **Minecraft testing:** not performed in this code batch.

## Effective live decision

Shadow is now the **primary** choice source for a supported, current legal action. Its independent `ShadowLiveDecisionEngine` chooses moves (attacks/priority/recovery/setup) or eligible first-party switches, and selects forced replacements independently from eligible own-party HP. Legacy is calculated once as a fallback and does not supply Shadow's move ranking.

The selection boundary `LivePromotionGate.choosePreparedWithSource` requires an explicit owner-approved production argument, a non-revoked battle control, native owning request/actor/turn lease, and current legal response. A current native `ShadowNativeLegalWindow` must exactly cover the ordinary roots in the snapshot; forced replacement has a separate current own actor/party check. `ShowdownActionResponse.isValid` checks the materialized target and response against Cobblemon's current request. Duplicate, trapped, continuation-locked, stale, missing, malformed, ambiguous or otherwise unsupported requests fall back to Legacy. No unsupported action is fabricated.

The native dispatch lease's turn, not a potentially lagging `PokemonBattle.getTurn()`, is used for proposal binding, native-root proof and materialization. Source expiry, materialization error and illegal attempted promotion revoke the current battle's Shadow control. A fresh control requires a genuinely different battle ID.

Incomplete opponent public evidence does **not** imply private knowledge. The new first-party-only fallback strips every opponent species, HP, type, reveal, ability, item, status/reply and history-dependent feature. It considers only owned states, legal ordinary moves and native legal reserves, and refuses unverified opponent-dependent status logic. No historic Phase 5 hypotheses enter live action selection.

## Gimmick and target boundaries

Normal move targets are materialized using Cobblemon's legal `getTargets` path and are validated with `isValid`. This is not a general multi-target optimizer. Independent Mega/Ultra Burst, Z, Tera and Dynamax activation now uses *only* the currently owning, native-advertised `ShowdownMoveset` and the selected ordinary move slot. A pure `ShadowNativeGimmickPolicy` chooses an activation conservatively: Mega/Ultra whenever the current native request allows them; Z only with a current enabled converted slot and sufficiently powerful own move; Tera only when the selected attack matches the known own Tera type and own HP supports it; Dynamax only when the selected slot has an enabled max move. Already-active Dynamax is never retriggered. Native response target options are checked, including Z/max target differences, and the special ID is independently rechecked after materialization because Cobblemon's `isValid` by itself does not certify that a special token was advertised. Unsupported, stale or ambiguous gimmick options fall back to ordinary action or Legacy; no gimmick is inferred from Legacy's choice or opponent private state. This enables *source-verified special actuation*, not perfect gimmick timing or proven advantage. Nonordinary/multi-active battles and unsupported continuations still fall back.

## Runtime operation

Promotion is **enabled by default** in v1.1.0. To disable it in a new run, add `-Dmaestroai.livePromotion.enabled=false` to the Minecraft server JVM arguments and restart. An invalid promoted attempt, lost native hook or battle-end lifecycle latches Legacy for the same battle even if the operator property changes back to true; the next battle ID may rearm. The runtime prints a fixed-schema `Live decision authority=SHADOW` or `LEGACY` marker with no request bytes or opponent secrets.

Only **LEGACY** outcomes are fed to `LegacyChoiceCapture`. The actual outcome origin is returned by the gate and never reconstructed from move IDs, allowing Phase 7 to avoid recording a Shadow selection as a Legacy one. Shadow's heuristic is not calibrated win probability or proof of superiority over Legacy.

## CI release gate

For a candidate source SHA, require all `Verify MaestroAI` jobs **verify**, **pack_source**, and **publish** to complete successfully at that identical SHA. The installable artifact must have the exact name `maestroai-verified-<full SHA>`; intermediate `maestroai-candidate-<full SHA>` is not approved for Minecraft. The checksum file in the verified artifact must match the enclosed v1.1.0 JAR. No success can be claimed until checked on the final commit.

## Required Minecraft acceptance

Reuse the existing BrAI four teams (eight battles, two per team) in the owner's Cobblemon 1.7.3 / Fabric / RCT singleplayer environment. Use the verified JAR for the final SHA; do **not** install a prior intermediate artifact. Check live `authority=SHADOW` markers while legal native actions exist, ordinary moves/targets, legal native Mega, Z, Tera and Dynamax activation, forced faint replacements, deliberate voluntary switches, trapping/Encore/rejected-switch recovery, unsupported gimmick fallback, battle-end safety, and owner-disable restart. Team 4 Unovan Zoroark must disguise as Greninja by putting Greninja last eligible. Use privacy-sanitized diagnostic receipts; no raw private requests or logs go to public GitHub.

**Acceptance is still pending:** CI alone cannot establish a real Cobblemon Shadow-selected action or its competitive success. Do not call this Minecraft-tested before the owner actually plays.
