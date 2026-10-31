# MaestroAI — Shadow Promotion Preparation, v1.0.97

**Repository:** `sensey12-mm/Maestro` · **Branch:** `Maestro4567`
**Owner authority:** NOT GRANTED · **Runtime:** Legacy authoritative; Shadow candidate producer is non-authoritative; no main merge.

## Integrated chooser boundary

The installed `MaestroBattleAI.choose` calls the unchanged Legacy chooser exactly once. The current turn's source-backed Phase 4 mechanics comparison, constructed during existing Shadow-only diagnostics, may independently propose a different move or switch using `ShadowPromotionPreparation.inspect`. This does **not** use a historical replay appearance to identify the opponent, import private Pokémon builds, assume a complete opponent action distribution, or claim a global tactical advantage. It is a *conditional* evidence-bounded recommendation, not a simulated real battle or independently accepted Shadow win-rate.

The preparer requires the **same current owning request/turn lease**, original in-process battle/request identities, current original runtime source/domain, current Phase 4 scoped execution, complete public protocol evidence, full own candidate coverage, known resource-exchange bounds and a **strict interval-dominating independent preference**. It carries a withdrawing source lease tied to that original battle/request/turn. Partial, unavailable, uncertain, tied or stale evidence yields an explicit non-action verdict. `Phase5` online observed role possibilities never become hidden builds, tactical probabilities or action authority. `Phase7Readiness` remains unproven for integrated installed advantage; it is **not** used as an approval token.

The prepared proposal flows into `LivePromotionGate.choosePrepared`, with an independently validated materializer for source-owned legal moves or eligible, live-owning bench UUIDs. Forced switching, trapped voluntary switching, charging continuation, missing/duplicate/unusable moves, foreign reserve, changed source/request/turn and invalid actions all reject. The gate rechecks currentness before and after materialization and immediately before returning, and catches runtime errors; all rejected, missing or stale proposals fall back to Legacy **once**, without changing Legacy's prior selected action. The gate has no manual replay/history override.

**Most important:** In production `MaestroBattleAI.choose`, the `ownerApproved` argument is the literal Java constant **`false`**. The JVM `-Dmaestroai.livePromotion.enabled=true` flag **alone cannot enable promotion**. No external file, historic signer or system property can change that literal. Any actual future activation requires separate explicit owner authorization and an audited code change; the current build will always return Legacy's response.

## Deterministic adversarial tests

`ShadowPromotionPreparationRegression`, registered in the existing standalone Java test entrypoint, covers strict independent ranking, all-root-action coverage, UNKNOWN damage/mechanics, no strict preference, duplicate/illegal moves, stale source, forged physical battle/request/turn, payload-free telemetry, no operator approval even with `enabled=true`, illegal candidate, failed materializer, retracted source, and synthetic *future* approved-path selection. It source-audits the production literal `false`. The existing Phase 4/5/6/7 regressions, native ABI, pinned simulator and JAR checks remain unchanged and required.

## Post-development owner acceptance (NOT performed in this batch)

When the full exact-head GitHub Actions run succeeds and its final `maestroai-verified-<40 character SHA>` artifact exists, replace the older MaestroAI JAR in the Minecraft Fabric 1.21.1 / Cobblemon 1.7.3 environment (never co-install versions). Initially use diagnostics only. Reuse the existing four BrAI teams / eight battles after development; compare the independent proposals against Legacy decisions and observe stale native callbacks, forced replacements, trapping, item/ability uncertainty, Illusion, Transform, battle termination and forced fallback. No unverified private data in CI or public logs.

**Do not claim live Shadow control or a Minecraft test from this stage.** After evidence-based Minecraft acceptance and independent Phase 7 tactical evaluation, a *separately authorized* promotion activation change may enable this gate with a reversible Legacy fallback. Until then, authorization is **DENIED** by default and in source.
