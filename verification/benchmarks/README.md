# Phase 7.1 — deterministic synthetic benchmark

This is a **verification-only** evaluation harness, not a combat simulator or live policy.
It is compiled under the existing Gradle \`verification\` source set, excluded from
the distributable JAR, and never called by \`MaestroBattleAI\`.

## Reproduce

From the Maestro repository root, with Java 21 and the pinned Gradle dependencies:

\`\`\`sh
bash gradlew --no-daemon -I verification/verification.init.gradle clean build standaloneRegression phase7Benchmark
\`\`\`

\`phase7Benchmark\` prints one machine-readable JSON object. To run another
explicitly synthetic fixture, set \`-Pphase7Fixture=path/to/file.json\`. The
configuration fingerprint is SHA-256 of the exact UTF-8 fixture bytes. A
fixed suite seed is mixed with a stable scenario ID/version hash and passed
to an elementary seeded, **legal-action-only** baseline. No randomness comes
from wall-clock time, unordered maps or live runtime state.

## Fixture schema

\`phase7_1_synthetic.json\` is version \`phase7.1.v1\`. Each scenario has a
stable lowercase ID, version 1, turn, legal move/switch IDs, an explicit
synthetic utility and uncertainty in [-1,1] and [0,1] respectively for
**every** legal action, and evidence gates (\`publicOnly\`, \`fresh\`,
\`mechanicsSupported\`). It intentionally has *no opponent mechanics,
moveset, identity, held-item or HP fields*. Unknown keys and duplicate
JSON keys are rejected, as are unbounded values, incomplete criteria
and duplicate actions/IDs.

The \`Phase7Benchmark.run(Suite, DecisionPolicy)\` Java API accepts an optional
offline Shadow decision callback. A null callback or null decision means
**abstain**. Unsupported/stale evidence prevents the callback from being
invoked and all synthetic outcome scoring is withheld. Illegal action
choices or policy exceptions are not scored; exceptions are reported by
fixed status without emitting their messages. The runner never selects
a live battle action.

A caller with a real Legacy capture can supply \`LegacyRecord\` with
capture ID, public-snapshot fingerprint, turn, legal action and an
**externally verified** provenance flag. Phase 7.1 checks the bound turn,
state, legal action and flag, but **does not establish that a raw capture
is genuine**: the trusted capture importer / evidence custodian must verify
authenticity upstream. There are no bundled Legacy capture records.
A synthetic fixture can never be interpreted as one. A matched Legacy
record reports *action disagreement* with the elementary baseline, not
the real Legacy policy's win rate.

Machine results include schema, seed, configuration and per-state hashes,
scenario statuses, baseline and candidate choices, per-fixture criterion
regrets and uncertainties, provenance rejections, abstention totals, illegal
choices and criterion-relative regressions. Results intentionally omit actor,
player, packet and raw capture contents.

## Interpretation

All fixture utilities are explicitly invented **test criteria**. Lower
regret means the chosen action was closer to that fixture's declared
highest utility, **not** that the action would win in Cobblemon. A reported
regression means that an optional candidate scored worse than the seeded
baseline under these synthetic numbers. No damage rolls, opponents, full
turn simulation, empirical Legacy decisions, Minecraft integration or
win-rate evidence are claimed.

Future phases may wire in separately validated and version-scoped sources
only after review. Legacy remains the sole live battle controller and
Shadow remains observational. Phase 7 final integration must wait until
Phases 4–6 are ready and integrated.
