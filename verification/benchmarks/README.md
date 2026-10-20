# Phase 7.1 — deterministic synthetic benchmark

This is a **verification-only** evaluation harness, not a combat simulator or live policy.
It is compiled under the existing Gradle `verification` source set, excluded from
the distributable JAR, and never called by `MaestroBattleAI`.

## Reproduce

From the Maestro repository root, with Java 21 and the pinned Gradle dependencies:

```sh
bash gradlew --no-daemon -I verification/verification.init.gradle clean build standaloneRegression phase7Benchmark
```

`phase7Benchmark` prints one machine-readable JSON object. To run another
explicitly synthetic fixture, set `-Pphase7Fixture=path/to/file.json`. The
configuration fingerprint is SHA-256 of the exact UTF-8 fixture bytes. A
fixed suite seed is mixed with a stable scenario ID/version hash and passed
to an elementary seeded, **legal-action-only** baseline. No randomness comes
from wall-clock time, unordered maps or live runtime state.

## Fixture schema

`phase7_1_synthetic.json` is version `phase7.1.v1`. Each scenario has a
stable lowercase ID, version 1, turn, legal move/switch IDs, an explicit
synthetic utility and uncertainty in [-1,1] and [0,1] respectively for
**every** legal action, and evidence gates (`publicOnly`, `fresh`,
`mechanicsSupported`). It intentionally has *no opponent mechanics,
moveset, identity, held-item or HP fields*. Unknown keys and duplicate
JSON keys are rejected, as are unbounded values, incomplete criteria
and duplicate actions/IDs.

The `Phase7Benchmark.run(Suite, DecisionPolicy)` Java API accepts an optional
offline Shadow decision callback. A null callback or null decision means
**abstain**. Unsupported/stale evidence prevents the callback from being
invoked and all synthetic outcome scoring is withheld. Illegal action
choices or policy exceptions are not scored; exceptions are reported by
fixed status without emitting their messages. The runner never selects
a live battle action.

A caller with a real Legacy capture can supply `LegacyRecord` with
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

## Phase 7.2 — reviewed Legacy decision intake

The offline Java API `Phase7LegacyImport.importCapture` reconstructs public events through
`PublicProtocolFilter`, `PublicEnvelopeProvenance`, `PublicProtocolEvidence` and
`PublicBattleCapture`. It also requires an externally reviewed `LEGACY_CHOOSE_RETURN`
action receipt bound to the own-request hash, capture origin, artifact hash,
public-stream hash, benchmark state and exact turn. The `CustodyReviewer`
callback receives only opaque IDs and SHA-256 digests. It must consult an
independently retained original and verify that the receipt really records
the returned Legacy action and current own request. The importer does not
authenticate that original by itself; hashes and caller-authored flags are
**not signatures**. No review means **rejection**. Original raw/private
battle data remain outside the repository and never enter reports.

The `importBatch` API rejects repeated capture IDs. Evidence checks fail closed
on unframed/partial public envelopes, split errors, missing public turns,
identity ambiguity, stale turns, unsupported mechanics, legal-action mismatches,
tampered history, missing custody review and malformed receipts. Successful
offline comparisons return only Legacy-vs-baseline and Legacy-vs-Shadow **action
agreement**, uncertainty/abstention and fixed statuses in machine-readable
JSON; they do not claim independent decision quality or real match outcomes.

The older Phase 7.1 `LegacyRecord.externallyVerified` boolean is intentionally
downgraded to `SELF_ASSERTED_UNVERIFIED` in its generic runner. It cannot
manufacture a `MATCHED_CAPTURE`. Use the reviewed importer for genuinely
provenance-gated action comparisons.

**Current limitation:** the existing controlled envelope capture contains public
events and terminal metadata, not an authenticated `chooseLegacy` return
receipt. This batch adds the strict importer/comparison contract but does not
retroactively convert public moves or synthetic selections into actual Legacy
decisions. A separately reviewed, privacy-safe own-choice receipt source is
needed before any empirical comparisons can be reported. The tests use
synthetic receipts and an explicitly synthetic trusted-review stub; **zero
real Legacy decisions were evaluated**. No Minecraft testing occurred.

## Phase 7.3 — opt-in actual Legacy return capture

The production-only observer is disabled by default. To explicitly opt in
for a **controlled local installation**, start the JVM with
`-Dmaestroai.legacyChoiceCapture=true`. The ordinary
`MaestroBattleAI.choose` return value remains the Legacy selection; the
observer is called after that selection and cannot replace it. A capture
failure is isolated and must not interrupt gameplay.

A capture requires all of these, *at the moment Legacy returns*:
a current owning `OwnRequestTransport.ChoiceProof` lease bound to the exact
battle UUID and observed turn, an installed Maestro JAR SHA-256, an
append-only, complete, framed public Showdown envelope with the same turn,
no unsupported mechanics or ambiguous appearance (including Illusion and
Transform), a source-validated response, and an eligible own move or
own-party-slot action. Any missing/mismatched proof **abstains**.

The opt-in line `LEGACY_CHOICE_RECEIPT` includes only a random opaque
capture ID, an own move ID or positional `switch:slotN`, turn, public
stream digest, process-salted own-request correlation digest and the
installed artifact hash. It **never** includes player/opponent names,
Pokémon UUIDs, raw request or battle packets, hidden builds, or raw public
event text. Capture continuity hashes private history chunks under an
ephemeral random salt but does not retain or export those chunks. The
observer retains at most 64 receipts per battle; a second choice in the
same turn or a rewritten prior history cannot create another accepted
receipt. Capturing switches by slot is intentionally limited to current
own-party ordering.

`Phase7LegacyImport.importObserved` is the verification-only bridge
into the Phase 7.2 importer. It additionally needs the private original
history, a benchmark scenario and an **independent custody reviewer**
that checks the actual choice-return evidence and source boundaries.
The runtime line alone is NOT authentication; hashes are NOT signatures.
Never put unreviewed private capture material or raw log files into
Maestro or public GitHub. The existing Phase 7.1 utility criteria remain
synthetic. Imported observations support only **action agreement**, not
real win rate or demonstrated tactical improvement.

Tests use a synthetic own-request ledger and public envelopes. They
cannot establish that Cobblemon/RCT, the specific datapack or installed
Minecraft wiring is functioning. No actual battles were requested.
Runtime acceptance and independent custody assessment are pending.

## Phase 7.4 — private local benchmark corpus

`Phase7Corpus.evaluate(submissions, seed, independentCustodyReviewer, offlineShadow)`
provides a reusable, deterministic, bounded verification-only corpus (100
entries, maximum 32 complete replay checkpoints per entry). Each submission
contains a **locally retained** original, its existing
`Phase7LegacyImport.Capture`, a versioned `Phase7Benchmark.Scenario`,
and sequential complete raw-history prefixes. Private inputs are passed in
memory and are not exported, uploaded, copied into Git or written to the
report. Partial protocol splits and unsupported event envelopes are rejected;
snapshots must be append-only, have explicit public turns, finish at the
captured choice turn and exactly match the imported raw source.

The corpus performs **independent replay checks** using the public protocol
filter, provenance reconciliation, public evidence model and incremental
`PublicBattleCapture` before delegating to the Phase 7.2 importer. A
separate custody reviewer must attest the owning Legacy choice and held
original; SHA strings, a runtime receipt, or synthetic fixtures alone do not
prove authenticity. Null/missing or rejecting reviewers mean **zero accepted
Legacy results**. To avoid first-wins bias, *all* submissions with a repeated
capture ID, same source battle/turn, or same own-request/public-action
fingerprints are rejected before the review callback. Every rejected row
abstains; offline Shadow is never invoked for it.

Reports contain schema `phase7.4.v1`, seed, deterministic sanitized digest,
fixed eligibility statuses, verified Legacy/baseline/Shadow *action agreement*,
abstentions, illegal-action counts, duplicate/replay/provenance rejections
and scenario coverage metrics. They omit capture IDs, battle/party UUIDs,
raw histories, actor names, private requests, effects and outcome claims.
No hypothetical utility, score, win rate or Minecraft compatibility result
is created.

`Phase7Corpus.writeLocalReport(repositoryRoot, "phase7_<id>.json", report)`
persists **only the sanitized report** atomically to the gitignored
`run/phase7-corpus/` directory. Unsafe traversal and symlink targets are
rejected. The original raw evidence stays in a separately controlled
private local location and must be resupplied for each replay. The corpus
does not create an authenticated capture reviewer or permanently archive
private originals; both require an owner-approved trusted process.
The tests use synthetic receipts and an explicitly synthetic independent
reviewer stub. **No real Legacy decisions were imported or tested in
Minecraft.** No new battles are required for this development batch.

## Phase 7.5 — detached-signature trusted local intake

The verification-only Java API `Phase7TrustedIntake.evaluateLocal(...)` reads
privately held capture bundles solely from ignored
`run/phase7-corpus/private-input/<bundle-id>/`, then delegates fully reviewed
input to Phase 7.4. No upload, network call, server requirement, public file
creation, runtime policy change or data export is performed.

### Local layout

Under the repository root, all of the following are local and Git-ignored:

```text
run/phase7-corpus/
  trust/independent_review.pub      # base64 SPKI DER Ed25519 PUBLIC key only
  private-input/bundle001/
    manifest.json                   # signed exact-byte manifest
    custody.json                    # detached Ed25519 custody approval
    checkpoint-01.json             # {"chunks":["update\\n|turn|1", ...]}
    checkpoint-02.json             # later COMPLETE append-only history
```

`manifest.json` requires exactly these fields:

- `schema`: `phase7.5.v1`
- `scenarioId`, `scenarioFingerprint`: externally supplied scenario binding
- `captureId`, `battleId`, `turn`, `action`: actual Legacy return identity and own action
- `artifactSha256`, `ownRequestSha256`, `publicSha256`: source-bound receipt values
- `rawHistorySha256`, `choiceReceiptSha256`: hashes defined by `Phase7LegacyImport`
- `checkpoints`: ordered objects `{"file":"checkpoint-01.json","sha256":"..."}`
  referring to SHA-256 of the *exact UTF-8 JSON bytes* of each checkpoint.

Each checkpoint is an object with exactly a `chunks` array of raw history
chunks. It is PRIVATE input and must not be committed or included in reports.
The final checkpoint must be byte-for-byte equivalent as an array to the raw
history used by Phase 7.2, and previous checkpoints must be complete
append-only prefixes.

`custody.json` contains exactly `schema` = `phase7.5.custody.v1`,
`keyId`, `manifestSha256`, and `signature` (base64 64-byte Ed25519
signature). The custodian signs the UTF-8 bytes produced by
`Phase7TrustedIntake.signaturePayload(manifestSha256)`, which are
`MaestroAI-Phase7.5-Custody-v1\\n` + the lowercase exact-byte manifest
SHA-256 + `\\n`. **Only an independently authorized custodian who
actually reviewed the original Legacy return, battle actor, own request,
artifact and capture chronology should sign.** Do not distribute a signing
private key or put a private key in the repo.

Call the API with the *separately obtained trusted SHA-256 of the SPKI DER
public key*, a trusted scenario map, approved bundle IDs, fixed seed and
optional OFFLINE Shadow callback. A public key bundled alongside a capture
is never trusted, and a signature by an arbitrary key does not suffice.
Signing a statement proves the configured custodian attested it, **not that
an unreviewed runtime receipt is genuine**. Changing a checkpoint, scenario,
action, public hash or manifest invalidates its binding. All signed inputs
still pass Phase 7.4 replay and Phase 7.2 independent provenance gates.

The API returns deterministic, sanitized JSON with fixed intake rejections,
verified bundle counts, Phase 7.4 duplicate/replay/custody coverage and
Legacy-vs-baseline/Shadow *action-agreement* only. Raw histories, UUIDs,
capture IDs, nicknames, private requests, keys, file paths and exception
payloads are excluded. Input count is bounded to 100; checkpoint sizes
and JSON keys are tightly validated; traversal and symlinks are rejected.
Private keys and original evidence must stay under the owner's separately
controlled custody. The trust and input directories are intentionally
ignored by the Git repository.

The synthetic regression harness creates a temporary Ed25519 key and
bundle, signs synthetic manifest bytes **in memory for tests only**, and
checks tampering, forgery, unknown schemas, replay gaps, duplicate capture
events, symlinks and no-review failures. **No real Legacy captures have
been approved or Minecraft-tested.**

## Phase 7.6 — deterministic, pinned corpus regression gates

`Phase7RegressionGate` extends the **offline verification-only** Phase 7.5
signed-custody corpus. It compares two independently reviewed, sanitized 7.5
reports against a **versioned local baseline**. The baseline lives only at
`run/phase7-corpus/baselines/phase7_<revision>.json` (Git-ignored). There is
NO approved real Legacy baseline bundled in this public repository.

To establish a baseline after an **independent custody review**, call
`Phase7RegressionGate.define(revision, reviewedIntakeReport, limits)`, then
`writeLocal(repositoryRoot, definition)`. The custodian must separately
pin the exact-file SHA-256 using a trusted channel. Future regression checks
must use `Phase7RegressionGate.load(repositoryRoot,revision,pinnedSha256)`.
Never derive the pin from the untrusted file under evaluation. SHA-256 is an
integrity check, **not a signature or source-authenticity attestation**.
A newly generated file is *not* automatically a trusted baseline.

Compare using
`Phase7RegressionGate.compare(pinnedBaseline, referenceReport, candidateReport)`
where **both reports** come from separately approved Phase 7.5 local
replays. The reference report's sanitized JSON SHA-256 and fixed seed must
match the baseline; both reports must have coherent, versioned aggregate
counts. Only the *same* accepted public-state, public-envelope and Legacy
action cohort can be compared. A changed public fingerprint, source
eligibility, accepted-record count, Legacy decision, or ambiguous repeated
accepted-state identity blocks a passing comparison. Reordered rows do
not affect the cohort check.

The baseline includes explicit per-batch integer tolerances for minimum
accepted cases, coverage loss, action-agreement loss, increased Shadow
abstentions and illegal choices, and increased provenance rejection counts.
The gate reports actual signed changes to these counts, replay/duplicate/
unknown evidence changes, and action agreement and provenance rejection
rates in integer basis points. Undefined rates remain **JSON null**, not
zero. The status is one of: PASS, BASELINE_MISMATCH, SCHEMA_MISMATCH,
INVALID_REPORT, INSUFFICIENT_EVIDENCE, COHORT_DRIFT, EVIDENCE_REGRESSION
or ACTION_REGRESSION. A PASS is *only* a statement about invariant
evidence quality and observed **decision agreement**, never about
winning battles, simulator accuracy, or tactical superiority.

All baseline and gate reports are sanitized and contain **no private
histories, player identifiers, capture IDs or raw event data**. The
regression harness creates synthetic accepted/rejected reports and pinned
baselines in a temporary local directory, tests malformed/edited metadata,
private-data suppression, dropouts, duplicate cohorts, drift, abstention,
illegal choices, rejection-rate and threshold changes. It does **not**
authenticate a real custody approval or test in Minecraft. No new battles
are required for this batch; final integrated evaluation still waits for
Phases 4–6.

## Phase 7.7 — stratified coverage and robustness audits

`Phase7StratifiedAudit` is a verification-only layer on top of the
**exact-commit, independently pinned Phase 7.6 baseline**, consuming two
sanitized, independently reviewed Phase 7.5 reports. It introduces **eight
fixed, supported strata**:

- Legacy decision type: `ACTION_MOVE` and `ACTION_SWITCH`, defined only
  from the observed Legacy action (not guessed Pokémon roles or strategy).
- Evidence outcome: `QUALITY_ACCEPTED`, `QUALITY_UNKNOWN` (unresolved
  public state/turn/identity), `QUALITY_DUPLICATE`, `QUALITY_REPLAY`,
  `QUALITY_PROVENANCE` and `QUALITY_INTAKE_REJECTED`.

These strata deliberately form **two different views**, not eight disjoint
battle situations: accepted samples appear once in an action category and
once in `QUALITY_ACCEPTED`. Other outcomes are counted only by their own
fixed evidence group. Rejected cases are never assigned synthetic actions.

### Versioned policy, evidence pinning and fail-closed behavior

Define a `phase7.7.policy.v1` policy through
`Phase7StratifiedAudit.define(revision, reviewedReport, minimumDisclosureCount,
maximumDominantActionBasisPoints, maximumEvaluatedMixDriftBasisPoints, rules)`.
Every group requires an explicit typed `Rule` specifying mandatory/optional
representation, minimum reference/candidate samples, count loss/increase
limits, agreement loss tolerance, and abstention/illegal-choice tolerance.
Required groups must have a positive minimum in **both** reports.
No hidden categories or arbitrary evaluator-supplied stratification is used.

Store the policy with `writeLocal(root, policy)` under the Git-ignored
`run/phase7-corpus/strata/phase7_<revision>.json`. Policy revisions are
immutable: a new policy must use a new revision, not silently overwrite an
existing file. Obtain the **exact policy file's SHA-256 from an independently
trusted channel**, then use `load(root,revision,pinnedSha256)`.
This SHA confirms file integrity, not genuine custody; never pin a hash
supplied by an untrusted bundle itself. Strict JSON validation rejects
duplicate fields, unexpected/missing strata, wrong versions, bounds,
malformed keys, and tampered policies. File traversal/symlinks are refused.

`compare(pinnedPhase76Baseline, pinnedStrataPolicy, reviewedReference,
reviewedCandidate)` first runs the **unchanged Phase 7.6 global gate**.
Neither a change in the verified public-state/Legacy-action cohort nor a
failed global evidence gate can be overridden by an attractive per-stratum
score. Every group then receives explicit support, agreement, abstention,
illegal-action and evidence-count checks. Net aggregate agreement can
remain constant while a deterioration in switch decisions still causes a
`GROUP_ACTION_REGRESSION`. Missing representation, sample dropouts, or
increased rejections produce their own fail-closed statuses. Dominant
Legacy action share and drift in the distribution of evaluated move/switch
decisions highlight sampling bias and selective abstention. A comparison
cannot pass with no evaluated decisions.

### Private-data disclosure

Output JSON contains only fixed group names, bounded aggregate counts,
basis-point agreement rates, fixed statuses and SHA-256 bindings of
already-sanitized reports/policies. It contains no raw histories, actor
details, Pokémon IDs, private moves or capture IDs. Groups with fewer
than `minimumDisclosureCount` samples (at least **3**) suppress the
affected counts, deltas and rates as JSON `null`; dominance and mix
percentages are suppressed if either action group falls below the same
threshold. Suppression does **not** bypass internal gates, which still
evaluate the actual counts. The report's PASS means only *the configured
evidence and decision-agreement invariants held*; it is not proof of
real competitive wins or Minecraft compatibility.

Phase 7.7's synthetic regression suite covers grouped agreement masking,
abstention shifts, sampling imbalance, insufficient strata, unknown/replay/
provenance/duplicate evidence grouping, round-trip/revision integrity,
tampered and re-pinned policies, disclosure suppression, cohort drift,
undefined rates, symlink rejection and strict threshold boundaries.
There is **no genuine Legacy corpus, no Minecraft testing, no tactical
Shadow promotion, and no request for new battles** in this batch.

## Phase 7.8 — version-pinned offline Shadow replay and disagreement attribution

`Phase7ShadowReplay` is a **verification-only** adapter to the existing
`ShadowSearchEngine.search`, not a new battle simulator. It requires:
a `phase7.8.policy.v1` versioned manifest, separately pinned exact-file
SHA-256, stable graph+scenario SHA-256, pinned installed Maestro artifact
SHA-256, explicit search-node and trace budgets, uncertainty penalty and
`requireCompleteSearch=true`. Its model input is an explicit public-only,
bounded conditional graph; game damage calibration and unknown opponent
responses are **not** invented. Conditional utilities remain proxies, not
real win probabilities or move-quality proofs.

The immutable policy lives **only** under the gitignored local
`run/phase7-corpus/shadow-replay/phase7_<revision>.json`. Create with
`Phase7ShadowReplay.define(...)`, store through `writeLocal(...)`, obtain
the exact-file policy SHA-256 via an **independent trusted channel**, and
reload through `load(..., pinnedSha)`. The policy binds `SHADOW_SEARCH_V1`,
the canonical sorted graph fingerprint, the public-only scenario manifest
and the configured installed artifact hash. Modifying a graph, response,
branch, scenario, limit or version without a matching independently
approved policy **fails closed**. No user-supplied arbitrary engine selector
is permitted. Policy revisions cannot be overwritten. Strict local path,
schema, size, key and symlink checks remain enforced.

`replayLocal(repositoryRoot,bundleIds,trustedScenarios,custodianKeyId,
pinnedCustodianSha,pinnedPolicy,graphs)` delegates to the **actual
Phase 7.5 Ed25519 signed-custody intake** and Phase 7.4/7.2 replay pipeline.
Before sending each accepted result to the offline evaluator, intake also
checks that its signed `artifactSha256` matches the policy's pinned build.
Unmatched artifacts yield `ARTIFACT_MISMATCH` and never trigger Shadow.
No public `|move|` event or self-asserted receipt can satisfy custody.

Every eligible Shadow evaluation runs through the real bounded
`ShadowSearchEngine` **twice**, using the same exact public-only snapshot,
versioned model and fixed configuration, and must return an identical
result. Unsupported models, nonlegal root actions, incomplete searches,
exceptions, unavailable/ambiguous public snapshots, missing custody
or nondeterminism cause explicit **abstentions**, not fallback actions.
There are no live battle hooks; the code is excluded from the mod JAR.

Reports are deterministic machine-readable `phase7.8.v1` JSON and bind
the signed intake report, exact policy, model graph, scenario manifest and
replay configuration. Bounded per-case traces record only ordinal, fixed
reason, move/switch group, fixed disagreement category, search node/rank
counts and legality/abstention flags — no raw names, capture UUIDs, game
events, opponent builds, private requests, unbounded exceptions or actual
action IDs. Summary attribution separates `MOVE_TO_MOVE`,
`MOVE_TO_SWITCH`, `SWITCH_TO_MOVE`, `SWITCH_TO_SWITCH`,
exact agreements, unsupported results and abstentions. Groups with
fewer than **three** accepted samples are suppressed in exported JSON,
including per-case details. Trace exhaustion is explicitly recorded as
`TRACE_BUDGET_EXCEEDED` instead of silently claiming a complete report.

Tests use synthetic Ed25519-signed local capture bundles, approved test-only
custody key, public-only Phase 7.1 scenarios and explicit conditional
Shadow search graphs. They verify exact replay determinism, real search
invocation, legal action rejection, exhaustion, configuration/graph/seed
pinning, differing artifacts, tampering, no-custody behavior, small-cell
privacy, bounded traces, immutable policies and malformed or re-pinned
metadata. These tests provide **zero empirical Legacy battles, zero
Minecraft acceptance claims and zero actual win-rate evidence**.
Final integrated Phase 7 validation still waits for Phases 4–6 integration.
