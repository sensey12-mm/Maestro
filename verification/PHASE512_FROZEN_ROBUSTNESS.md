# Phase 5.12 — Frozen hypotheses and distribution-shift robustness

Phase 5.12 extends the existing Phase 5.11 signed-custody coverage
evaluation. Scope is **synthetic, offline and diagnostic only**.

## Pre-registered protocol

`PublicCompetitiveRobustnessAudit` exposes a version-1 canonical
protocol description and SHA-256 fingerprint covering the supported
source target, opponent slot (`p2a`), two hypothesis/baseline pairs,
stratification order, minimum sample sizes, minimum paired improvement,
fixed format weights and abstention policy.

The two frozen, existing hypotheses are:

- Move: frequency-ranked **observed native moves** on the current
  appearance, compared to **first observed native move**.
- Switch: observed public return-display pattern compared to the
  **always-move** action-category baseline. It does NOT infer intent or
  destination and excludes forced drag from its eligible horizons.

Neither hypothesis is trained across replays or claims a probability.

The caller must supply `Registration(version,protocolHash,testSplitHash)`
matching the frozen code and Phase 5.4 split, separately pinned *before*
inspecting held-out outcomes. Changing baseline definitions, thresholds,
split membership or target version invalidates that registration.

**Caveat:** Registration is a publicly constructible record; this code
cannot prove external pre-registration timestamps or that an operator
did not choose the protocol/test set post hoc. The digest must be
independently committed through a trusted protocol outside the test
corpus to support a real preregistration claim.

## Conservative robustness and distribution shifts

The audit uses Phase 5.11's 20 signed-custody-verified format/action/
uncertainty rows and produces **30 fixed stress scenarios** (each action
x each uncertainty group including ALL x three format weightings):
90% OU / 10% Ubers, 50/50, and 10% OU / 90% Ubers.

Every scenario reweights **paired observed candidate-minus-baseline
fractions** exactly, using overflow-safe integer arithmetic. It reports
only a fixed finding (insufficient evidence, no observed gain, below
fixed margin, or observed margin). It is not a model of how a real
future tournament population will shift.

Fixed preliminary thresholds are **3 independent replay files,
30 paired scored horizons, and +500 basis points paired advantage**
in every required stratum. Missing groups and abstentions are never
treated as wins. Favorable pooled results cannot override an inadequate
Ubers group, an empty switch category, Transform/unknown-source gaps, or
a weak individual format. Format instability and pooled masking risks
are explicitly reported.

As in Phase 5.11, all replays—TRAIN and HELD_OUT—must be admitted with
current signed Ed25519 custody, operator-attested replay rights and
pinned source/version. Raw replay evidence and a supplied Phase 5.4
report are fully recomputed. The Phase 5.11 aggregate is also verified.
No opponent histories are joined across appearances or replay files.
Privacy-minimized outputs contain only grouped counts, fixed category
labels, protocol and split digests, and guarded booleans.

## Correctness fix and limitations

The Phase 5.11 exclusive uncertainty classifier now uses **opponent
slot p2a only**. A player's own Transform, own unknown move or own
return-display behavior cannot contaminate the opponent's uncertainty
group. Synthetic tests cover this regression.

This is **not** statistical significance testing, online sampling,
real competitive data ingestion, calibrated prediction, a causal
improvement claim or permission to promote Shadow tactics. Actual
permitted real-replay outcomes, external rights verification, and
independent registry/runtime compatibility remain unverified.
Legacy remains live-authoritative; Shadow observational.

No Minecraft tests, private Headquarters uploads, main merge, or
runtime integration are part of Phase 5.12.
