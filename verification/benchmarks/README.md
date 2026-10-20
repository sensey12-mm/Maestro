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

## Phase 7.9 — cross-version Shadow replay robustness matrix

`Phase7ReplayMatrix.evaluateLocal(...)` is a substantial **offline-only**
comparison layer. It accepts an ordered baseline Shadow policy plus 1–7
candidate policy revisions, **all independently pinned** through Phase 7.8,
and re-runs exactly the **same signed private local Legacy bundles** through
the full Phase 7.5 → 7.4 → 7.2 provenance chain for every revision. Only
one public-state scenario set, source artifact SHA-256 and seed are allowed;
different supported conditional graphs and search parameters are expected
between revisions. Unsupported or mismatched scenario, build, model pin,
seed or duplicate revision is refused BEFORE reading the private bundles.
No fabricated Legacy action or simulation outcome substitutes for custody.

Every pair runs the existing **Phase 7.6 pinned aggregate regression gate**
AND **Phase 7.7 independently pinned stratified audit**, with no weakening
or feature bypass. It also requires complete, consistent, untruncated
per-row Phase 7.8 traces, matching signed intake statuses, exact public
state and Legacy-action identities, and at least one genuinely eligible
reviewed case. Across the same evidence, the matrix counts changes in
selected Shadow actions, Legacy action-agreement gains/losses, new and
recovered abstentions, illegal root decisions, search-budget exhaustion,
unsupported decisions and increases in uncertainty.

**Uncertainty is NOT a calibrated probability or forecast.** The matrix
uses only the maximum *declared public conditional root-outcome
uncertainty* for the selected action and changes in the pinned
`uncertaintyPenalty`. Those are explicit deterministic proxy checks, not
accuracy or win-rate evidence. New illegal decisions, exhausted search,
increased abstention/unsupported decisions or uncertainty proxy each
fail closed, regardless of whether aggregate agreement improves.
All Phase 7.6 and 7.7 statuses remain visible even when a more-specific
diagnostic takes priority. Evidence drift or incomplete traces cannot pass.

Results are machine-readable, reproducible `phase7.9.v1` JSON.
Reports include only sanitized policy/model/scenario/report digests,
revision IDs, fixed comparison statuses and bounded summary deltas.
No capture IDs, Pokémon/action identifiers, raw histories, actor
details, hidden opponent data, private file paths or exceptions are
exported. When fewer than **3** accepted cases exist, differential
counts are suppressed in exported JSON (but still evaluated internally).
This deliberately matches the earlier strata small-cell boundary.

The regression suite adds **three distinct synthetic public-state
scenarios**, each with a separate Ed25519-signed private local
capture. It uses the same Phase 7.8 genuine offline replay adapter and
verifies identical input/policy determinism; changed legal actions;
global and per-stratum regression-gate enforcement; new illegal
actions; inadequate node budgets; trace truncation; increased
uncertainty/penalty; invalid artifact or seed boundaries; duplicate
revisions; missing custody; small-cell disclosure; source-baseline
mismatch and privacy-safe output. Duplicate public-state identities
remain forbidden by Phase 7.6. There is **no real Legacy capture
approval or Minecraft test**, and Shadow remains **offline-only**.
Do not use these results as real win-rate or simulator validation.

## Phase 7.10 — evidence-gated observational simulation and bounded self-play

`Phase7SelfPlay` is a **verification-only, offline dual-agent harness**
which reuses the real bounded `ShadowSearchEngine` and version-pinned
`Phase7ShadowReplay.PinnedPolicy` for **each** simulated participant.
It is NOT a predictive Cobblemon simulator and contains no damage formula,
field-effect model, hidden-opponent inference or simulated win-rate estimator.

The harness only follows **separately reviewed, cryptographically signed,
public-observation transitions**. Each party supplies its own version-pinned,
public-projected `Phase7Benchmark.Scenario` and legal action universe:
the two Shadow policies never receive each other's hidden request payload.
Every transition fixes both selected legal actions, originating state,
successor state, digest of a reviewed public event and an explicit
`PUBLIC_OBSERVED_TRANSITION` versus `UNSUPPORTED_MECHANICS` outcome.
The entire sorted graph binds each state's public-state fingerprints and
all transition edges; a duplicate joint action, terminal-state continuation,
unsupported/unverifiable public snapshot, illegal action, stale/skip-turn
successor or changed model hash cannot silently advance the replay.

### Independent mechanical evidence, not self-attested fixtures

`Phase7SelfPlay.transitionsSha256(transitionGraph,firstScenarios,secondScenarios)`
builds a canonical graph fingerprint. An **independently authorized
reviewer** must audit the public observations and mechanics before signing
`Phase7SelfPlay.signaturePayload(graphSha)` with Ed25519. The verifier
public-key SPKI DER digest must be obtained via an **independent trust
channel**; the `replay(...,attestation,externallyPinnedReviewerKeySha256)`
API requires this value **separately**, not from the untrusted attestation
object. An arbitrary self-generated key in the bundle does not establish
trust. Genuine public mechanics verification is NOT supplied by this
repository: there is currently no integrated Phase 4 verified transition
model. Consequently, signed synthetic fixtures are **not** real evidence
and no real turn-transition coverage is claimed.

The policy `phase7.10.policy.v1` pins the entire transition graph,
both public scenario manifests, both independently pinned Shadow policies,
installed artifact SHA, seed, maximum own-turn decisions and node-search
budget. Create using `define`, write once to ignored
`run/phase7-corpus/selfplay/phase7_<revision>.json`, obtain exact-file
SHA from a separate trusted channel and `load` with that pin. Unsupported
versions/fields, duplicates, unsafe local paths, symlinks, seed/model/build
drift and attempts to overwrite a revision fail closed.

For **each** signed public state, both Shadow agents independently run
deterministic, complete legal-action search twice. The harness chooses
only the uniquely attested joint-action transition and checks both
signed public continuations before advancing. A missing own branch is
`INCOMPLETE_TRANSITION_MODEL`; a missing opponent-response combination is
`UNKNOWN_OPPONENT_RESPONSE`; unknown mechanics,
partial search, illegal actions, changed outcomes, unverified signer,
non-monotonic turns and looped/unsupported observations explicitly abstain.
No fallback transition or invented opponent behavior is permitted.
Reaching an explicitly certified terminal means only the end of a
reviewed *transcript*, **not a battle victory**. Reaching the configured
turn limit is `HORIZON_REACHED`, never treated as a win.

Reports are deterministic, bounded `phase7.10.v1` JSON with fixed status,
public-model/policy/reviewer fingerprints, verified-step/attempt coverage,
missing and unsupported transition counts and optional fixed-category
step reasons. Traces for fewer than three attempts are suppressed. Nothing
exports capture IDs, actor identifiers, action IDs, Pokémon information,
raw events or private file paths. Graph and report IDs are content hashes
of independently public-projected metadata; no private raw history is
included. No live battle controller invokes the harness; verification
classes do not ship in the mod JAR.

The synthetic regression suite constructs two separately pinned Shadow
search policies, four turn-indexed public-only scenarios per participant,
a three-turn observed transition chain and a temporary test-only Ed25519
reviewer. It checks stable canonical hashing, deterministic replay, full
legal-action coverage, both sides' legality, public-turn continuity,
missing or ambiguous responses, unsupported effects, forged reviewers,
unsigned evidence, changed graphs, incomplete search, horizon exhaustion,
small-cohort suppression, immutable/tampered local policies, wrong
artifact/seed and symlink rejection. **This is only synthetic source-level
validation. Minecraft testing is still required after trusted mechanics
and Phases 4–6 are integrated. No additional battles are requested.**

## Phase 7.11 — independently pinned public mechanics adapter and transition gates

`Phase7MechanicsAdapter` is a **verification-only**, versioned interface
between the existing Phase 7.10 observational self-play harness and a
*future, separately approved* Phase 4 public-mechanics evidence source.
**No Phase 4 code has been imported or guessed in this branch.** The
adapter deliberately uses no Phase 4 implementation classes; it consumes
a typed `phase7.11.mechanics.v1` package containing the reviewed source
commit/corpus hashes, installation artifact hash, exactly bound public
transition fingerprint, mechanic-receipt fingerprints and per-edge
`REVIEWED_PUBLIC`, `UNKNOWN` or `UNSUPPORTED` status.

### Custody, evidence levels and trust

A source package **cannot authorize itself**. An Ed25519 detached approval
must verify against the reviewer key independently pinned in a
`phase7.11.policy.v1` local baseline. Separately, the pinned policy
must bind the exact reference bundle, source commit, source corpus,
artifact and canonical transition graph. The source contract must equal
`PHASE4_PUBLIC_MECHANICS_V1` with version 1. Never use an external
bundle's own key hash as the trusted key. Policies live only under
ignored `run/phase7-corpus/mechanics/phase7_<revision>.json`;
create via `define`, store with `writeLocal`, obtain its exact-file SHA
via a distinct custodian channel and load using
`load(repositoryRoot,revision,trustedSha)`. The loader refuses
duplicate/extra/missing JSON fields, tampering, unsupported versions,
symlinks, wrong revisions and policy overwrites.

**Cryptographic review proves an authorized key signed these bytes,
not that a mechanics formula is correct.** Source code, runtime
compatibility, independent source verification and real Cobblemon
acceptance still require Phase 4 integration review. The adapter returns
`ELIGIBLE_OFFLINE_ONLY`, never a claim of verified Minecraft support.
Anything explicitly tagged `SYNTHETIC_TEST_ONLY` is ineligible even
when signed by the pinned test key. No generic or self-asserted
`verified=true` field exists.

### Whole-graph coverage and regression safety

Every public transition edge—**including unchosen branches**—must
have exactly one independently reviewed record with the same canonical
edge hash, recorded action-pair group, mechanic evidence receipt and
public-state graph binding. Joint-action groups:
`MOVE_MOVE`, `MOVE_SWITCH`, `SWITCH_MOVE`, `SWITCH_SWITCH`.
A pinned, explicit `Gate` requires minimum overall and per-group
reviewed evidence; zero group minima mark a group optional but never
excuse a missing record for a transition that actually exists.
Any unknown/unsupported effect prevents offline eligibility regardless
of configured tolerance. The auditor additionally rejects *every* illegal
action on both sides and every edge whose existing Phase 7.10 mechanic
marker says UNSUPPORTED, even when that branch is never selected and a
reviewer's record incorrectly claims it is REVIEWED_PUBLIC.
Missing, duplicate, stale, source-mismatched
or miscategorized records fail closed; no inferred opponent responses
or hidden information can substitute for absent public evidence.

`audit(...)` produces a deterministic `phase7.11.v1` aggregate JSON
report with fixed statuses and per-stratum counts. Any group with fewer
than three reviewed public transition candidates suppresses its
counts. `compare(...)` compares separately signed source revisions on
the same **exact** public graph, source corpus, source commit and
installed artifact; it detects reviewed-to-unknown/unsupported
downgrades, changed mechanics receipts, unauthorized source changes,
relabelled groups and unverifiable candidate signers.
`replay(...)` calls the mechanics audit **before** Phase 7.10 search.
If the audit fails, the returned `GatedReplay` contains no self-play
result; successful adapter review only grants permission for **offline
observational replay**, whose own signature/legality/turn checks remain
mandatory.

Phase 7.11 adversarial tests reuse Phase 7.10's privately generated
synthetic three-turn transcript and *extend it* to exercise all four
joint-action categories. These tests use new ephemeral keys and
a **deliberately fictional Phase 4 source**, strictly to verify contract
logic. They verify signature and trust pins, ignored local policy
lifecycle, public-transition integrity, duplicate/missing records,
source/corpus drift, support downgrades, complete-group minima,
changed mechanics receipts, actor legality, small-cell redaction,
tampered metadata and actual gated replay. **There are still zero
independently reviewed real Phase 4 mechanics, zero Minecraft runs and
zero empirical win-rate observations.** Legacy retains exclusive live
battle control; Shadow stays offline, and no new battles are requested.


## Phase 7.12 — offline end-to-end readiness

`Phase7Readiness` composes the existing Phase 7.5 signed local Legacy
intake, Phase 7.8 bounded Shadow replay, Phase 7.9 version matrix and
Phase 7.6/7.7 regression gates, Phase 7.11 mechanics review, and Phase
7.10 bounded observational self-play. It checks installed artifact,
seed, scenario and starting public-state identity, reviewer-approved
mechanics, legal actions and source fingerprints across all stages.

Every stage has an explicit fail-closed status. An early failure prevents
subsequent execution. Reports include fixed stage statuses, hashes and
minimum-size-suppressed counts; never raw battle data or individual
actions. Tests reuse three locally signed synthetic Legacy examples and
reviewed transition fixtures, and cover source drift, missing custody,
unsupported mechanics, unbound public states, version regressions and
unfinished observations.

**Phase 7 stopping boundary:** independent infrastructure development
is permitted. Final evaluation of the finished intelligent Shadow and
Phase 7 completion must wait for completed and integrated Phases 4, 5,
and 6, independent integration review, owner authorization and eventual
Minecraft acceptance. Phase 6 currently depends on Phase 4/5 readiness.
An offline readiness report always says `integrated=false`. Synthetic
evidence always produces `SYNTHETIC_ONLY` even if all test fixtures pass;
offline reviewed material remains
`AWAITING_INTEGRATED_PHASES_4_5_6`.

## Phase 7.13 — infrastructure stabilization and integration handoff

The public-safe [Phase 7.13 integration handoff](PHASE7_INTEGRATION_HANDOFF.md)
records the inspected evidence interfaces, the Phase 7.13 verified risk fixes,
privacy boundaries, and the independent integration acceptance criteria.
It defines the STOP boundary: further Phase 7 independent feature development
is paused after infrastructure stabilization; final intelligent-Shadow and
Minecraft validation requires completed and owner-authorized integrated
Phases 4, 5 and 6 and independent reviewer approval.
