# Verification and Minecraft acceptance — current operating guide

**Source checkpoint:** MaestroAI `1.1.5`, public `Maestro4567` commit `236669af71e0dd1a1b6064645c91dd2c6b2e0625`. The [exact GitHub Actions run #41518874540](https://github.com/sensey12-mm/Maestro/actions/runs/41518874540) completed successfully for `verify`, `pack_source`, and `publish`. The resulting verified artifact is `maestroai-verified-236669af71e0dd1a1b6064645c91dd2c6b2e0625`; inspect its versioned `maestroai-1.1.5.jar` and SHA-256 checksum before deployment.

## Source and CI verification

1. Verify branch, `gradle.properties` and exact HEAD; never assume a previously noted checksum refers to a later commit.
2. Run `python3 verification/check_repository.py --self-test` and `python3 verification/check_repository.py`, plus Gradle build and `standaloneRegression`/appropriate synthetic benchmarks with Java 21 when checking locally.
3. The GitHub workflow must independently complete both pinned native simulator profiles (`verify` and `pack_source`). The `publish` job requires both to pass and validates exact-SHA artifact contents and checksum before publishing. Do not skip these checks even for documentation changes.
4. A cancelled or failed run is **not** a release; accept only the successful run from the **exact** branch commit.

## What source CI does not prove

**Installed Minecraft 1.1.5 acceptance remains required.** Only actual owner-run Minecraft with the correct 1.1.5 JAR and compatible mods/data packs can establish live opponent display/confirmed distinction, observed moves and types, forced replacement actions, safe switching, legal gimmicks and whether there are zero *routine* Legacy decisions. Record owner-reported evidence without exposing private logs or identities in this public repository.

The 1.1.3 owner log is historical evidence of 23 Shadow and six Legacy decisions, **not** a 1.1.5 acceptance result. A green source CI result also does not establish Phase 7 completion, broad Minecraft compatibility or a 9/10 battle win rate.

See [architecture](architecture.md), [project README](../README.md) and [Shadow Promotion 7 release note](../verification/SHADOW_PROMOTION7_RELEASE.md). Preserve source pins, tested policy limits and private/public evidence separation.
