# Phase 5.11 — Signed held-out competitive coverage and readiness

Scope: **offline-only** synthetic verification of per-format, per-action,
per-uncertainty coverage. No real replay corpus, runtime binding, new tactical
consumer, future accuracy claim, or Minecraft test. The existing Phase 5.4
paired predict-before-observe and first-native-move/always-move baselines
remain unchanged.

## Provenance and leakage gates

`PublicCompetitiveCoverageAudit.audit` requires the **complete original
replay split**, exact Phase 5.4 report, current Ed25519-signed Phase 5.9
custody manifest, explicitly trusted key policy and explicit evaluation
timestamp. Phase 5.7–5.9 validation re-computes the corpus, verifies
**every training and held-out replay approval**, and rejects missing,
expired, revoked, tampered, inconsistent, unsupported or duplicate/renamed
replays before any group result is issued.

Every held-out replay is scored independently by a fresh existing
`PublicOpponentIntelligence`, on `p2a` public action events only.
All group totals must agree exactly with the independently reconstructed
Phase 5.4 paired benchmark. No cross-replay player, nickname, display
or party-member identity is linked. Observed opponent-identity ambiguity
remains unconfirmed, never proof of a unique combatant.

A private-constructor, immutable report contains only aggregate counts,
fixed strata labels and an opaque split digest. It has no player names,
raw logs, move lists, replay IDs, or one-replay metrics. Upstream replay
identifiers and hashes remain potentially linkable; audit output is
**minimized, not mathematically anonymous**.

## Mandatory reporting strata

Each held-out replay belongs to exactly one **format** (Gen 9 OU or
Gen 9 Ubers) and one **exclusive uncertainty category**:

1. `TRANSFORM_OBSERVED`
2. `UNKNOWN_MOVE_SOURCE`
3. `RETURN_DISPLAY_AMBIGUOUS`
4. `ORDINARY_IDENTITY_UNCONFIRMED`

The precedence above avoids treating one replay as several independent
samples. `ALL` totals are computed separately for each format.
Both action categories, `MOVE` and `SWITCH`, are reported on the
same pre-event scored horizons as the original Phase 5.4 benchmark.
Switch announcements are not proof of voluntary choice or a unique
destination; Transform/copy events remain excluded by the existing
observer. The evidence-loss/skipped count is reported separately.

The report has **20 deterministic rows** (2 formats x 2 actions x
5 uncertainty groups, including ALL), even when zero replay examples
exist. Every row specifies independent replay count, paired scored
horizons, candidate/baseline correct counts, skipped events, and one
explicit gate.

## Thresholds and caveats

Provisional audit thresholds (not empirical sufficiency proof):
- at least **3 independent whole replays** per reported stratum;
- at least **30 paired observed action horizons** per stratum;
- more candidate hits than the fixed paired baseline;
- no positive readiness if any required format/action/uncertainty row
  lacks those minimums or if a pooled gain masks an unrepresented or
  non-improving stratum.

Reported `OBSERVATIONAL_COVERAGE_ONLY` means those mechanical thresholds
were satisfied. It is **not** calibrated probability, robust generalization,
statistical significance, genuine real-world skill or permission to use
an action in battle. Replays may still share unknown real human opponents;
without approved identity evidence the evaluator cannot certify independent
human participants and never attempts linkage.

`Readiness` exposes explicit sufficiency conditions and permanently
separate flags for independent replay-rights verification, actual
permitted real-replay evaluation and tactical promotion. **All three
remain FALSE** for Phase 5.11. A strongly positive aggregate move rate
does not overrule poor Ubers results, absent switching horizons, or
missing ambiguity/Transform/unknown-mechanics strata.

## Independent real-replay evaluation prerequisites

A later owner-approved batch would require verified replay permissions,
an externally trusted updated signing key policy/checkpoint and trusted
clock, an explicit pre-committed non-overlapping split with adequate
per-group diversity and independent reviewer approval of thresholds.
Any real evaluation must preserve privacy, original custody and
appearance ambiguity, report abstentions and simple baselines for each
stratum, and avoid fabricating calibrated probabilities or claims of
out-of-sample improvement.

No new opponent priors, live recommendations, runtime adapters, shadow
promotion, or main-branch merge are part of this phase.
Legacy remains authoritative. Shadow remains observational.
