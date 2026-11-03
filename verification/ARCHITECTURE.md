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

The release `publish` job must continue to depend on **both** independent native and pack jobs, and verify an artifact from the exact same source commit. The registration audit checks direct/transitive Java reachability, independent Python-to-Node invocation and Java-embedded oracle source references; static reachability alone cannot establish actual runtime execution. The separate pinned native and pack CI profiles exercise the registrations. Required CI actually runs these profiles.

## First executable-architecture consolidation

The independent Node source checks now have a declarative `oracle_matrix.py`
execution registry rather than 31 duplicated Python process invocations.
The 25 native and six pack steps run in their original order with original
fixtures and time bounds. The independent ordered sequences are also pinned
in the reviewed registry: a silent reordering, removal or native-to-pack
reclassification fails even if the union of files remains identical. The weather-entry oracle still runs twice, with its
separate `--setters` profile. Static AST validation and negative tests reject
missing, malformed or unregistered source oracles and missing native/pack runner
invocations. The individual JavaScript oracles remain separate to preserve
independence and debugging.

## ABI preflight consolidation and migration custody

The former `check_native_split_hooks.py` duplicated entry-point plumbing
without providing an independent oracle: it pinned the dependency SHA and
called `runtime_hook_oracle.verify_pinned_hooks`. That behavior is now
`runtime_hook_oracle.py --verify-pinned`, while `--self-test` runs the
retained bytecode and descriptor adversarial tests. CI requires **both**.
`verification_registry.json` records the original location and Git blob,
the replacement, a reason and both positive/negative coverage commands.
Missing originals are rejected unless explicitly registered as reviewed
migrations; missing replacements and broken CI mappings also fail closed.

## Historical verification navigation and regression hygiene

[Verification history index](INDEX.md) groups all 75 original
public-safe reports by responsibility without changing their original text,
attribution or historical conclusions. It is checked against the exact origin
inventory, rejecting missing, duplicate and stale links. Newly added reports,
fixtures and scripts must be explicitly registered, not merely present on disk.

All Python verification files are parsed for syntax and checked for missing
local oracle imports. CI also syntax-checks all independent JavaScript files,
including Java-hosted oracles, in both verification jobs. These controls do not
pretend to replace the executable adversarial or pinned-source checks.

## Java regression runner by responsibility

`StandaloneRegression` retains every original adversarial regression class and
its exact execution order, now grouped into own mechanics, public competitive
evidence, synthetic benchmarking, live-public integration and runtime provenance.
The baseline built-in synthetic assertions remain at their original position.
The registration guard checks that each group is invoked from `main`, and
negative tests reject a disconnected group or missing class invocation.

## Existing manual or not-yet-CI-reachable probes

**Java:** `AddonReloadRegression`, `BootProfileRegression`, `FabricModeRegression`, `RaidReloadRegression`, `RuntimeCaptureRegression`.

**Java-embedded Node oracles (CI-exercised indirectly):** `geomancy_continuity_oracle.cjs` through `GeomancyContinuityRegression`, `own_first_web_oracle.cjs` through `OwnFirstWebRegression`, `own_xerneas_oracle.cjs` through `OwnXerneasRegression`, and `policy_oracle.cjs` through `ModifierTacticalRegression`. They are loaded as source into the pinned simulator's Java/Graal harness, not launched directly from Python. The registration checker protects their Java-to-JavaScript links.

The five Java files above remain outside required CI roots. `AddonReloadRegression` and `RaidReloadRegression` are subordinate to the currently unregistered `RuntimeCaptureRegression`; the remaining two are separate host probes. Their correct integration requires reviewing required inputs, positive and adversarial cases, and the independent simulator evidence boundary. Do not manufacture inputs merely to obtain a green registry.

## Future consolidation contract

1. Establish how each component is reached, its fixtures, and positive, privacy and adversarial coverage.
2. Connect new tests to the existing appropriate suite; review and update the inventory so new source files cannot silently appear.
3. Before relocating/removing an original file, extend the reviewed migration mapping with its original path and Git blob, replacement, preserved coverage and independent-oracle rationale. Unrecognized migrations still fail closed and require reviewed guard amendments and negative tests.
4. Run `python3 verification/check_registration.py --self-test` and `python3 verification/check_registration.py`, then all existing CI gates. Verify `verify`, `pack_source`, and `publish` for the **exact commit**.
5. Keep private HQ documentation, battle logs, credentials and custody evidence outside this public repository. The repository safeguard rejects private-repository URLs in public verification content, CI secrets, writable token permissions, and persisted checkout credentials. Source CI and simulator checks cannot certify Minecraft acceptance.

The original file count is a **baseline, not a reduction target**. Retain independent checks when they provide separate evidence or failure isolation.
