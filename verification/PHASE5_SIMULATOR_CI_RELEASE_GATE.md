# Phase 5 — Pinned Simulator CI Bottleneck / Verified Release Gate (v1.0.94)

## Observed baseline (public successful CI, not a projected speedup)

Run [41230983907](https://github.com/sensey12-mm/Maestro/actions/runs/41230983907), exact source commit `0faecfb1f8002153c86629e18ec00b14ac6f3e55`, finished **success** and published a versioned SHA-pinned JAR. GitHub logs recorded three nested `sourceVerification` Gradle invocations: original pinned native/Graal differential suite **30m 41s**; post-install modified pack/Graal differential suite **15m 36s**; pack-layout-only verification **13s**. All three were serialized in one `source_oracle.py` invocation. This serial dependency, not the final upload (seconds), is the observed bottleneck.

## Preserved verification

`source_oracle.py` retains its original full default mode, and exposes only explicit `--native` / `--pack` split profiles. Each independently checks the exact cached Cobblemon JAR SHA-256, embedded simulator SHA-256, public native inventory, pin tables, addon JARs and hook ABI before running any Java/node checks. The native profile runs **all** original Node oracles and the complete native Graal differential suite. The pack profile runs **all** pack/addon/method/source/pack-item Node oracles, all modified-pack Graal differential tests, and the layout-only final check. Unknown/mixed modes reject. Subprocesses are bounded by 40-minute child timeout; workflow subprocesses by 48-minute GNU timeout, killing stalled children, and any failure or timeout exits nonzero.

The workflow executes the profiles in parallel on independent GitHub Actions runners. On every supported branch/PR verification, `verify` still builds, runs all Java/packaging/mixin regressions, and runs the `--native` profile. `pack_source` separately checks out the **same commit**, checks repository safeguards, prepares Java21 dependency classes and runs the `--pack` profile. The `publish` job with `needs: [verify, pack_source]` cannot run successfully unless both completed successfully.

## Artifact integrity

Only `verify` can upload `maestroai-candidate-<SHA>`, labeled *not release-approved*, with exactly one named JAR and `sha256.txt`. This intermediate download has a 1-day retention and must not be installed. Only `publish`, after the two jobs, downloads the SHA-pinned candidate, checks exact two-file inventory, checksum and `fabric.mod.json` metadata with `verification/check_publish_artifact.py`, and uploads `maestroai-verified-<SHA>` (7-day retention). All GitHub Actions references are pinned by full immutable action SHA.

Adversarial Python regressions cover source-profile partition, unknown profile, subprocess timeout, missing pack job, premature publication, missing source pin checks, hash corruption, stale JAR version, symlink, added private files and malformed artifacts. The repository safeguard suite fails CI if the release gate weakens. The checker labels the verified candidate bytes but cannot independently certify CI or Minecraft acceptance: that comes from the gated workflow state and owner installed evidence.

**Acceptance:** exact-commit public CI and final verified artifact must both exist. No CI pass or wall-clock improvement is assumed before the new run finishes. Minecraft tests and historical archive rights remain independently required. Legacy remains authoritative and Shadow remains non-authoritative.

## Observed first parallel execution and installed-JAR handoff

The first parallel run [41237741889](https://github.com/sensey12-mm/Maestro/actions/runs/41237741889) independently completed `pack_source` **successfully**. Its source oracle started at 20:13:15 UTC and completed at 20:34:35 UTC (approximately **21m 20s**, including the required 20m 48s pack-source Graal verification), and the entire pack job took approximately **23 minutes**. The native `verify` job had **not finished** at this review. This is evidence that the split pack profile runs successfully, not a full parallel CI pass or proven wall-clock speedup.

The earlier serial source-verification run [41230983907](https://github.com/sensey12-mm/Maestro/actions/runs/41230983907) completed successfully with a JAR artifact after approximately **51 minutes overall**; a later serial run [41232877080](https://github.com/sensey12-mm/Maestro/actions/runs/41232877080), pinned to `b98cd74134b57436eadef674ccbebcd61b36eccc` (**MaestroAI 1.0.93**), completed successfully and published [its verified JAR](https://github.com/sensey12-mm/Maestro/actions/runs/41232877080/artifacts/13056505992). This is the **latest fully verified installed-build baseline established by this audit**, not proof of Minecraft runtime success.

For the 1.0.93 baseline: download the exact source-run's `maestroai-verified-b98cd74134b57436eadef674ccbebcd61b36eccc` artifact (not a candidate), extract `maestroai-1.0.93.jar`, and verify its `sha256.txt` against the independent JAR SHA-256 printed by that **successful run's** `Check JAR and checker regressions` log. Replace the previous MaestroAI JAR in `mods` (never co-install). This baseline can be used for source-equivalent Minecraft diagnostics, but the latest changes to the CI-only script/packaging pipeline are **not attested** by its earlier run. For 1.0.94, follow the equivalent procedure **only if all three jobs in that exact run succeed and the final `maestroai-verified-<SHA>` artifact exists**.

No historical replay rights or signed spectator archive were created by CI. The operator must independently verify the approved replay, signed manifest/keyring, clock and separate checkpoint before historical observation is enabled. Use the existing four BrAI teams/eight battles for owner-side Minecraft acceptance; the CI result alone does not test Minecraft. No changes to Legacy authority or Shadow promotion are made here.

**Pending acceptance at this review:** exact-commit 1.0.94 `verify`, `pack_source`, and `publish` success; release artifact and measured end-to-end time. Do not call the split workflow complete or faster until those checks finish.
