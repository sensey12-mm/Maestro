# Verification architecture: baseline, registration and custody

This is public-safe engineering documentation of this repository, not private project material.

## Reproducible starting point

- `Maestro-2.0` parent: `8d6f193fec55554c41b4d16dbfc226d9b88e83ff`.
- Original `verification/` inventory: **256 files** (134 Java, 33 Node CJS, 11 Python, 75 Markdown, one JSON, one TSV and one Gradle initialisation script).
- The original paths and immutable Git blob identities are preserved in `verification_registry.json` and the 256-entry historical mapping is independently SHA-256 locked by the registration gate. These are historical identities, **not** hashes constraining future legitimate edits.
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

The workflow targets active `Maestro4567` and isolated `Maestro-2.0` pushes,
main pushes, and pull requests to main or the verification-review branch.
Deleted historical development branch triggers are intentionally retired.
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

All Python verification files are parsed for syntax, checked for missing
local oracle imports, and required to remain reachable from an actual CI
entry point through the static import graph. Twelve of twelve Python scripts
are currently registered by that rule; deliberate disconnections fail CI. CI also syntax-checks all independent JavaScript files,
including Java-hosted oracles, in both verification jobs. These controls do not
pretend to replace the executable adversarial or pinned-source checks.

## Java regression runner by responsibility

`StandaloneRegression` retains every original adversarial regression class and
its exact execution order, now grouped into own mechanics, public competitive
evidence, synthetic benchmarking, live-public integration and runtime provenance.
The baseline built-in synthetic assertions remain at their original position.
The registration guard checks that each group is invoked from `main`, and
the 103 originally executed Java regression calls remain registered in
`verification_registry.json` in their source-proven order. Negative tests
reject a disconnected group, a missing class invocation, or an unreviewed reorder.

## Existing manual or not-yet-CI-reachable probes

**Only optional, not CI-reachable Java host probe:** `FabricModeRegression`. This fixture depends on a suitable Fabric/raid host and cannot be treated as installed Minecraft acceptance.

**Full-pack CI-hosted Java regressions:** `PackSourceRegression` launches `BootProfileRegression` and `RuntimeCaptureRegression` in separate Java subprocesses and checks their exit status. `RuntimeCaptureRegression` then exercises `AddonReloadRegression` and `RaidReloadRegression`. All four were confirmed by source analysis as reachable from the existing required full pack-source job, without adding redundant executions.

**Java-embedded Node oracles (CI-exercised indirectly):** `geomancy_continuity_oracle.cjs` through `GeomancyContinuityRegression`, `own_first_web_oracle.cjs` through `OwnFirstWebRegression`, `own_xerneas_oracle.cjs` through `OwnXerneasRegression`, and `policy_oracle.cjs` through `ModifierTacticalRegression`. They are loaded as source into the pinned simulator's Java/Graal harness, not launched directly from Python. The registration checker protects their Java-to-JavaScript links.

The registry models both direct Java test calls and the exact Java child-process
class-name launch path. It rejects missing boot/runtime host launches or missing
fail-closed exit status checks. `FabricModeRegression` remains explicitly
not runnable as part of the synthetic source-only CI and requires a suitable
installed runtime; this does not establish gameplay acceptance.

## Batch 2 — shared pinned public archive acquisition

The published Mega Showdown installer, ZA Mega addon and Raid Dens addon had
three repeated network/cache/hash implementations. The common plumbing now
lives in `pinned_public_archive.py`; each caller keeps its independent,
unchanged public URL, SHA-256, download limit (32/8/16 MiB respectively),
archive-class provenance checks, factory inventories, wire normalization and
full-pack behavioral oracle. The central transport enforces exact cached and
downloaded sizes, SHA-256, HTTPS, cache-symlink rejection, bounded reads,
atomic cache writes and no silent network recovery from a tampered cache.

Five new offline adversarial tests cover valid first download and reuse,
bad/oversized downloads, corrupt cached bytes, symlink/invalid specifications,
and exact pin/limit contracts for all three independent callers. The source
self-test suite executes them in both native and pack jobs; registration
tests reject disconnected tests, changed consumer pins or reintroduced
separate downloaders.

Other apparently related verification tools are intentionally **not merged**:
`check_jar.py` checks the locally built JAR against compiled classes and
resources; `check_publish_artifact.py` separately gates candidate integrity
after both jobs; `phase5_verify_download.py` independently verifies a ZIP
with an externally supplied SHA-256 without granting installation approval.
Similarly native-vs-pack Node oracles, Java/Graal source observations,
per-phase records and signed-custody tests require distinct failure domains.
No measured semantic duplication justified merging those checks, and no
historical report was renamed or deleted.

## CI performance optimization: measured baseline and guarded tuning

The last fully passing pre-tuning run was
[41696027339](https://github.com/sensey12-mm/Maestro/actions/runs/41696027339)
at `a5e579374b1fe40d75c774ac75727e03e413337a`. Its UTC
runner timestamps show:
- Native `verify`: ~34m23s overall; the `sourceVerification` Gradle
  subprocess took ~30m54s. A ~22m10s gap occurred between modifier
  composition completion and selected-move attestation completion.
- Independent `pack_source`: ~19m55s overall; the full first
  `sourceVerification` invocation took ~18m02s. A ~4m20s gap occurred in
  that profile's selected-move attestation.
- `publish` took seconds and was already gated by successful completion
  of both profiles; the independent 25 native and six pack Node oracles
  were not the main delay. Timings reflect CI wall clock, not CPU profiles.

A bounded `512m` initial / `3g` maximum heap is applied **only to
the disposable `sourceVerification` JavaExec**, not production Minecraft
or Shadow runtime. This targets potential interpreter/GC pressure but does
not prove its cause. `CI_SOURCE_PROFILE` timing markers now isolate native
bootstrap, complete differential suite, move attestation, and final
source checks, plus pack completion. The registration gate and its
negative-mutation tests require the original 26 differential calls in
order, full selected-move attestation, pack and native entry paths,
and the verified-only JVM scope. The existing repository safeguards
still mandate `publish.needs: [verify, pack_source]` and same-commit
artifact validation. Full source acceptance is never replaced by
timing instrumentation.

The first bounded-heap trial, [41703275931](https://github.com/sensey12-mm/Maestro/actions/runs/41703275931),
confirmed the full pack profile still succeeded but took approximately
34 minutes; the heap adjustment alone therefore **did not demonstrate a
speedup** relative to the older ~20-minute pack baseline. The next
controlled test uses GraalVM Community JDK 21 **only for the
`sourceVerification` JavaExec child**, leaving compilation, standalone
regressions, installer checks and the published mod build on Temurin 21.
Official GraalJS embedding documentation notes that suppressing the
interpreter-only warning does not enable runtime compilation:
https://github.com/oracle/graaljs/blob/master/docs/user/FAQ.md .
The verified action SHA sets up both JDKs; the shell restores the original
`JAVA_HOME` for Gradle, and `MAESTRO_SOURCE_JAVA` selects the isolated
GraalVM child. The repository gate rejects missing launcher isolation,
unexpected release-job Graal settings, and bypassed `publish` dependencies.

**Post-optimization durations must be compared from exact-commit CI logs**
before claiming a speedup. If the relocated Graal classes still cannot
use the host optimizer, the run must make that limitation explicit rather
than claiming compiler-based acceleration.

## Future consolidation contract

1. Establish how each component is reached, its fixtures, and positive, privacy and adversarial coverage.
2. Connect new tests to the existing appropriate suite; review and update the inventory so new source files cannot silently appear.
3. Before relocating/removing an original file, extend the reviewed migration mapping with its original path and Git blob, replacement, preserved coverage and independent-oracle rationale. Unrecognized migrations still fail closed and require reviewed guard amendments and negative tests.
4. Run `python3 verification/check_registration.py --self-test` and `python3 verification/check_registration.py`, then all existing CI gates. Verify `verify`, `pack_source`, and `publish` for the **exact commit**.
5. Keep private HQ documentation, battle logs, credentials and custody evidence outside this public repository. The repository safeguard rejects private-repository URLs in public verification content, CI secrets, writable token permissions, and persisted checkout credentials. Source CI and simulator checks cannot certify Minecraft acceptance.

The original file count is a **baseline, not a reduction target**. Retain independent checks when they provide separate evidence or failure isolation.
