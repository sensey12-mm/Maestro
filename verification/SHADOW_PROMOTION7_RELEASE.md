# Shadow Promotion 7 (v1.1.5) — public opponent awareness and CI efficiency

Branch: Maestro4567. Shadow remains the independent live primary AI; Legacy is emergency recovery only. Do not modify Phase 7 or merge main.

## Opponent awareness
- Source-checked native spectator history still binds the original physical battle, owning request/turn lease, public split-routing custody, actor slot and unmodified original event list.
- A normal `switch` or `drag` displays an apparent species. It is NOT actual species identity: Illusion can disguise it. The live adapter looks up that *display* in the currently loaded public Cobblemon 1.7.3 species registry (no opponent Pokemon, actor, request, team, stats or hidden metadata read), then returns only a **conditional type matchup hypothesis**.
- The BattleSnapshot opponent species stays `UNVERIFIED` and its confirmed types remain empty for mere displayed appearances. Shadow's independent tactical heuristic uses conditional typing as a bounded preference, never as a hard immunity, exclusion, certainty or probability claim.
- Actual public `replace` after Illusion is an explicit current-appearance identity disclosure. Once the original public source and Phase 4 observation agree and no later transform/form invalidation occurred, the recognized species and its installed public base types can populate confirmed facts. The revelation cannot bind a party ID across switches or restore unknown HP.
- Explicit publicly announced Tera or type changes take precedence over display hypotheses. Transform, unverified form transitions and stale/rewritten source histories still fail closed.
- Current HP remains quantized spectator evidence; revealed moves/items/abilities remain observed-only. Hidden opponent builds and probabilities are never introduced.
- The public catalog may vary with installed modpack datapacks. Unmatched display IDs return no hypothesis rather than inventing a species. The existing frozen static 14-species catalog provides synthetic regression fixtures only; live registry is version-gated.
- Opponent identity coverage remains limited to source-confirmed public events or conditional displayed matchups. Never claim ordinary display proves true identity.

## CI investigation and optimization
- The previous successful runs 41314771285 (2026-11-01) took roughly 85 minutes, 41354075638 roughly 36 minutes and 41432259644 roughly 37 minutes. They used the same workflow (two separate native simulator profiles).
- GitHub Actions already caches Gradle, but the old concurrency group was **per SHA**, defeating branch `cancel-in-progress`. Every corrective push launched another long native job pair, allowing superseded commits to overlap and consume public runners.
- Change concurrency group to stable branch/PR; retain `cancel-in-progress` for Maestro4567, the separate `verify` and `pack_source` native profiles and `publish.needs: [verify, pack_source]`. Do not skip simulator checks, loosen source pins, grant release from old SHAs or change CI timeouts.
- CI execution-time improvement is **not claimed** until an exact run is measured after this change. Shorter resource contention does not guarantee faster native simulator wall-clock duration.

## Verification and release conditions
- Deterministic standalone regressions must prove conditional vs confirmed type boundaries, real public identity reveal/Illusion/Transform/re-entry/Tera behavior, tactical choice changes, stale-source abstention, native-legal gating, Phase 5 replay custody and workflow safety.
- Required exact-SHA public GitHub Actions must pass Java, checker, ABI, standalone and both native simulator profiles before a verified 1.1.5 JAR is released.
- Minecraft has **not** been tested for 1.1.5. Installed-game acceptance must confirm common opponent appearances actually produce `displayHypothesis=CONDITIONAL` or evidenced `types=EXPLICIT` where justified; supported forced replacements and ordinary decisions should be Shadow-selected without routine Legacy fallback. Unexpected `UNKNOWN` and emergency fallback require investigation, not test loosening.
