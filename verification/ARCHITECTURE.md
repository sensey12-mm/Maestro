# Verification architecture: baseline, registration and custody

This is public-safe engineering documentation of this repository, not private project material.

## Reproducible starting point

- `Maestro-2.0` parent: `8d6f193fec55554c41b4d16dbfc226d9b88e83ff`.
- Original `verification/` inventory: **256 files** (134 Java, 33 Node CJS, 11 Python, 75 Markdown, one JSON, one TSV and one Gradle initialisation script).
- The original paths and immutable Git blob identities are preserved in `verification_registry.json`. These are historical identities, **not** hashes constraining future legitimate edits.
- Original exact-commit [CI #41645647716](https://github.com/sensey12-mm/Maestro/actions/runs/41645647716): `verify`, `pack_source` and `publish` completed successfully. This is not installed Minecraft acceptance.

## Verification responsibility and isolation

| Responsibility | Main entry point |
| --- | --- |
| Synthetic/adversarial Java | `StandaloneRegression` via `verification.init.gradle` |
| Pinned native independent source oracles | `source_oracle.py --native` |
| Separate pinned pack-source checks | `source_oracle.py --pack` |
| Repository, packaging and release checks | `check_repository.py`, `check_jar.py`, `check_publish_artifact.py` |
| Required registration/path preservation | `check_registration.py` |
| Synthetic benchmark (not live games) | `benchmarks/phase7_1_synthetic.json` |

The release `publish` job must continue to depend on **both** independent native and pack jobs, and verify an artifact from the exact same source commit. The registration audit checks direct/transitive Java reachability and source-oracle references; it does not establish runtime execution from static text alone. Required CI actually runs these profiles.

## Existing manual or not-yet-CI-reachable probes

**Java:** `AddonReloadRegression`, `BootProfileRegression`, `FabricModeRegression`, `RaidReloadRegression`, `RuntimeCaptureRegression`.

**Node:** `geomancy_continuity_oracle.cjs`, `own_first_web_oracle.cjs`, `own_xerneas_oracle.cjs`, `policy_oracle.cjs`.

These files remain preserved and explicitly identified rather than falsely described as CI-executed. Some depend on separately provisioned native fixtures or on one another. Their correct integration requires reviewing real dependencies, positive and adversarial cases, and the independent simulator evidence boundary. Do not manufacture inputs merely to obtain a green registry.

## Future consolidation contract

1. Establish how each component is reached, its fixtures, and positive, privacy and adversarial coverage.
2. Connect new tests to the existing appropriate suite; review and update the inventory so new source files cannot silently appear.
3. Before relocating/removing an original file, map its original path and Git blob identity to its replacement, preserved coverage, and independent-oracle rationale. The current checker intentionally refuses missing original paths until an auditable migration mechanism is implemented.
4. Run `python3 verification/check_registration.py --self-test` and `python3 verification/check_registration.py`, then all existing CI gates. Verify `verify`, `pack_source`, and `publish` for the **exact commit**.
5. Keep private HQ documentation, battle logs, credentials and custody evidence outside this public repository. Source CI and simulator checks cannot certify Minecraft acceptance.

The original file count is a **baseline, not a reduction target**. Retain independent checks when they provide separate evidence or failure isolation.
