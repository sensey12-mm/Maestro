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
