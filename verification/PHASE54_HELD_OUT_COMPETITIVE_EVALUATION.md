# Phase 5.4 — Held-out public prediction evaluation

Scope: `PublicHeldOutEvaluation.evaluate` is an **offline benchmark**
over individually permission-attested Phase 5.1 spectator `.log` replays,
using Phase 5.2 format/source normalization and the existing
`PublicOpponentIntelligence` predict-before-observe and paired benchmark.

## Reproducibility and leakage controls

- The operator assigns whole replays to `TRAIN` or `HELD_OUT` in an
  explicit *precommitted* split manifest. Partitions are not chosen from
  observed outcomes by this implementation. Results are reproducible for
  any input ordering; the report contains only a SHA-256 of the sorted
  replay-ID/content-hash/partition/version manifest.
- Distinct source replay IDs and unique SHA-256 hashes of original replay
  bytes are mandatory across **both** partitions. An ID re-used with
  different text and the same text under different IDs are rejected.
- Each replay passes permission/privacy admission and version-aware
  normalization BEFORE scoring. Every replay uses a fresh,
  appearance-scoped intelligence observer; training objects and their
  results cannot influence a held-out observer.
- No cross-replay opponent identity, nickname, team member, role, moveset,
  or private feature is ever joined or inferred. True matching humans
  across de-identified public replays **cannot independently be ruled
  out**, but identities are not modeled or used at all.
- Benchmarking restricts actions to the `p2a` opponent slot. No `p1a`
  actions, private inputs, copied Transform moves or move overrides score.
  The existing opponent observer audits a hypothesis BEFORE incorporating
  each new public move or switch event.
- Move hypothesis is the most-used already observed *native* move on the
  current appearance, compared with the first observed native move.
  Switch hypothesis is the existing observed-display-return candidate
  pattern, compared with always predicting `move`. Forced `drag` is
  excluded from switch scoring; switch announcements do not establish
  voluntary intent. A returning display is not a unique party identifier.

## Interpretation boundaries

The `TRAIN` segment is an independent diagnostic benchmark, **not a
fitted training stage**: no hyperparameters, weights or distributions are
trained or imported into held-out tests. The held-out comparison uses
the same frozen predictors and paired event horizons. Rates are
observed correctness fractions only and are withheld below 20 events.
The benchmarks report eligible paired comparisons, matches, simple
baselines, and skipped/abstained public decisions; no calibrated
probabilities, causal improvements or generalization guarantees.

Unrecognized target versions get an explicit unsupported-replay count
and no apparent Cobblemon-compatible evaluation. Source pins do not
attest installed runtime mechanics or Minecraft gameplay. The report
has no real player identifiers or raw log content; only aggregate
statistics and an opaque split digest. No real replay data was collected,
nor training/test data published in this batch. Synthetic regressions
validate guards and predictable fixtures; no empirical held-out
competitive accuracy has yet been established.

No new tactical chooser, Legacy modification, Shadow promotion,
main-branch merge or Phase 5.5 work is authorized by this batch.
