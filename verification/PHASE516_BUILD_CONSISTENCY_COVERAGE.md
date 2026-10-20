# Phase 5.16 — Opponent build evidence consistency and coverage

Phase 5.16 audits **offline, public, signed-custody-verified** build
evidence from Phases 5.13–5.15, with **no current-battle binding, hidden
opponent information, probabilities, or tactical promotion**.

## Trusted inputs and source scope

`PublicBuildEvidenceConsistency.audit` accepts an original, explicitly
attested spectator log, candidate *possibilities* scoped to individual
opponent appearances, exact pinned Cobblemon 1.7.3 species and form
resolver sources, optional conservative override notices, current
Ed25519-signed custody and explicit verification time. It independently
recomputes the Phase 5.13 observation and Phase 5.15 reconciliation
from that same original log. Arbitrarily constructed upstream report
objects are never accepted as authority.

The public-source revision is
`e428d34ea7f4aeca6dc9031488fb51fe0e315218`.
All other versions fail closed. The pinned source slice covers four
base species and two forms; it does NOT establish installed server
datapacks, legal actions, item eligibility, dynamic learning or
the identity behind an Illusion.

## What is a contradiction?

The audit distinguishes:

- **Public-observation/source disagreement**: an observed move or
  ability was not listed for the displayed exact species/form in the
  pinned source snapshot. This is a discrepancy needing investigation,
  **not** proof of cheating or an impossible gameplay action.
- **Unobserved, unsupported hypothetical candidate**: an optional
  suggested move/ability does not appear in the pinned source.
  This is explicitly not a contradictory observation.
- **Non-LEGAL-query source method**: a move is listed under only
  `legacy` or `special`; the source does not establish that it is
  available to the current `LearnsetQuery.LEGAL` query.
- **Missing eligibility coverage**: items, unknown forms, absent
  overrides/learnset methods, and Transform-copied behavior abstain.
- **Multiple public ability/item values**: the same appearance showed
  changes over time, which may be perfectly legitimate; do not
  collapse transitions into one native build. Repeated displays are
  handled as separate appearances.
- **Public form transition**: a mid-appearance form/display update
  requires additional evidence before claiming static form legality.
- **More than four public moves**: mark the native-set inference as
  uncertain rather than declaring a Pokémon illegally equipped five
  moves; copied/temporarily changed moves may explain it.
- **Conditional role clues**: boost/priority signals from Phase 5.13
  remain hypotheses, not confirmed roles or future-intent predictions.

The report never merges repeated nicknames, species or appearances.
Evidence from `p1a` cannot alter `p2a` transition/consistency
counts. The source can support the *displayed* form without proving
a unique underlying combatant. All appearances remain identity
uncertain, and all live/Shadow promotion and full-game legality flags
remain false.

## Coverage and abstention report

For each appearance and each of five fixed fields
(MOVE, ABILITY, ITEM, FORM and ROLE), the audit emits counts for:

- public observations;
- total considered possibilities (observed + optional candidates);
- source-listed possibilities;
- unsupported unobserved possibilities;
- observed/source disagreements;
- unresolved/abstained possibilities;
- standardized privacy-safe issue classes.

Separate report-wide totals additionally count fields without
observations, appearances containing source disagreements and
appearances requiring abstention. The report contains no raw log,
replay ID, player nickname, signing key, signature, move/item/ability
names, or opponent history linkage. It retains appearance indices and
opaque categories only. Since aggregate counts can potentially be
recognizable in a small dataset, it is privacy-**minimized**, not
irrevocably anonymized.

`SOURCE_OBSERVATIONS_CONSISTENT_ONLY` is a narrow descriptor, never a
positive build certificate. Every appearance has an identity-uncertainty
gate, so even an otherwise well-supported source snapshot cannot
authorize tactical or gameplay claims. A source conflict blocks
source-consistent classification, while missing input and signed-custody
failures reject evaluation outright.

## Adversarial verification and remaining prerequisites

Synthetic regressions cover confirmed versus proposed move categories,
observed unsupported moves and abilities, unsupported hypothetical
items, inherited/replaced forms, reported overrides, unknown forms,
Transform, appearance reuse, multiple public item/ability values,
p1-side isolation, mid-appearance form changes, five observed moves,
tampered logs, private packet injection, expired custody, incompatible
source pins, immutable reports, and privacy-safe serialization.

This remains an offline evidence classifier, not a generative legal
moveset validator. Independent Phase 4 integration and verified
real-world benchmark evidence remain prerequisites for stronger claims.
No unapproved real replay, installed Minecraft testing, runtime
integration, main merge or private Headquarters data is part of this
batch. Legacy retains live control and Shadow remains observational.
