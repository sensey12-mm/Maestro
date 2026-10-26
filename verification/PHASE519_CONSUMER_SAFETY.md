# Phase 5.19 — Competitive intelligence consumer safety contract

**Scope:** deterministic offline preparation for possible future *Shadow*
consumers. This phase does **not** connect to a tactical consumer or to
a live battle, and does not promote Shadow decisions. Legacy retains
authoritative live control.

## Interface and trust boundaries

`PublicCompetitiveConsumerContract.prepare(...)` requires a
version-1 `ConsumerRequest` with `OFFLINE_SHADOW_REVIEW` purpose.
`SHADOW_TACTICAL` and `LIVE_CONTROL` requests, current-battle
binding, and tactical-use requests are rejected **even for otherwise
valid data**.

The `ContractPin` fixes the Phase 5.14 normalized species resource
SHA-256, Cobblemon 1.7.3 source revision, existing
`CobblemonMechanics.SOURCE` version scope, and the frozen Phase 5.18
robustness policy digest. Incompatible version/policy/species/source
inputs fail closed.

The entrypoint accepts only original replay inputs, bounded
candidate assertions, exact source pins, conservative override notices,
current Ed25519-signed custody and trusted verification time.
It **recomputes** Phase 5.16 consistency, Phase 5.17 filtering and
Phase 5.18 robustness, which reconstruct their own public observation
and source legality chain. No caller-constructed `Result`, `Report`,
`Scenario`, or `Export` is an acceptable authority input.

After recomputation the adapter checks:

- matching source revision and mechanics evidence scope across reports;
- identical chronological appearance indexes between candidate
  assertions, classifications, and all nine robustness scenarios;
- exactly one reference scenario and all eight prescribed
  perturbations per candidate, with no duplicates;
- the reference robustness finding equals the Phase 5.17 verdict;
- no result or scenario grants opponent elimination, verified unique
  identity, installed registry legality, or tactical promotion.

Missing custody, insufficient source scope, empty candidate requests,
invalid appearance indexes, replay modification, signature expiry,
replay/private protocol injections or unsupported compatibility fail
closed.

## Minimal consumer-visible schema

`Export` is immutable and **private-constructor-owned**. It exposes
only source/policy versions, chronological appearance indices,
per-field public/source/unsupported/conflicted/unresolved COUNTS,
candidate input ordinals, source-conditional verdict labels,
robustness sensitivity counts, and fixed uncertainty enums.

It does **not** serialize raw replay logs, replay IDs, trainer or
nickname strings, move/item/ability IDs, candidate sets, custody
signatures, private information or probability estimates. Such
count-based output is minimized, not irreversibly anonymized.

Every export reports:

- current signed custody **revalidated**, but replay rights
  independently verified = **false** (operator attestations are not
  independent proof);
- opponent identity and complete native build verified = **false**;
- installed registry/mechanics compatibility, item/ability slot
  selection and calibrated forecasts = **false**;
- Shadow tactical input/ready = **false** and Legacy live
  promotion = **false**.

**Important:** `SOURCE_CONDITIONAL_ONLY` indicates a narrow
source-based assertion, NOT that an opponent actually knows or carries
a move. `ASSERTION_DISAGREEMENT_ONLY` flags conflict with a particular
public/source assertion, NOT proof of an illegal Pokémon or authority
to eliminate a party member.

The uncertainty envelope conservatively includes non-unique opponent
identity, unverified installed mechanics, and operator-only replay
permission for *every* appearance; conditional role clues, unsupported
items/forms, public/source discrepancies, transitions and brittle
contradictions add further warnings.

## Adversarial synthetic verification

Tests exercise source/policy/version drift, forbidden LIVE and
SHADOW_TACTICAL requests, report immutability, privacy-safe
serialization, actual signed custody verification, item/ability
transitions, unsupported forms, Transform, source conflict, override
downgrades, multiple candidate verdicts on one appearance, repeated
same-nickname appearances, candidate/appearance ordinal alignment,
forged/expired evidence and private packet injection.

The implementation uses no online replay downloads and ingests no
unapproved real data. Minecraft runtime compatibility and actual
competitive accuracy remain unverified. No private Headquarters code
or archives were copied to public MaestroAI. No main merge or runtime
integration was performed.
