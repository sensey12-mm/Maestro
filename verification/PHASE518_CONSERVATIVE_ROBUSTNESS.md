# Phase 5.18 — Conservative candidate robustness and false-elimination gates

Phase 5.18 provides a **version-pinned, offline, privacy-minimized**
robustness layer over the Phase 5.17 partial build candidate filter.
It is *not* a live opponent model, a calibrated probability,
a real-world false-elimination measurement, or a legal-moveset oracle.

## Frozen protocol and provenance

`PublicBuildCandidateRobustness` uses policy version 1 with an explicit
SHA-256 fingerprint over its fixed scenarios, Cobblemon 1.7.3 pinned
source revision and non-elimination rules. Unknown versions and wrong
fingerprints fail closed. The implementation does not accept caller
chosen thresholds, scenario subsets, source compatibility assumptions
or externally constructed Phase 5.17 classifications.

Each evaluation re-runs Phase 5.17 on the complete original replay,
which revalidates the Phase 5.13–5.16 evidence chain and original
Ed25519-signed custody under the operator-provided trusted keyring and
evaluation timestamp. This establishes compliance with the pinned
**offline admission contract**. It does not prove the replay's actual
copyright permission, installed registry, real game mechanics or the
identity of the Pokémon behind Illusion.

## Deterministic sensitivity scenarios

Every candidate receives **all nine** scenarios (including the reference
assessment). These are *counterfactual reductions in knowledge* based
on source-verified evidence, not modifications or re-ingestions of
signed replay logs:

1. Full reference evidence.
2. Omit publicly witnessed move evidence.
3. Blind the displayed form.
4. Omit publicly revealed ability evidence.
5. Omit publicly revealed item evidence.
6. Treat Illusion/disguise as an unresolved possibility.
7. Treat Transform/copied native moves as an unresolved possibility.
8. Treat item or ability state changes as unresolved.
9. Treat pinned species/form source compatibility as unavailable.

The fixed conservative policy NEVER authorizes eliminating a real
opposing Pokémon or its undisclosed native build. A Phase 5.17
`CONTRADICTED` result is retained as
`ASSERTION_CONTRADICTION_ONLY` **solely** when its specific
public/source assertion still holds. If the sole observed excluded move
is hidden, the result is downgraded to `UNRESOLVED`.
Unknown form, Illusion, Transform or incompatible source produce
`ABSTAINED`. An observed ability being masked cannot invent a
different ability; a source-listed move remains source-conditional
without implying that it was carried. Items remain unverified.

There is no fabricated replay signature: all scenarios are comparisons
against the originally verified evidence. All counterfactual scenario
findings are **hypothetical diagnostics**, not newly observed events.

## Privacy-safe failure and readiness reporting

Reports contain only fixed scenario labels, per-input ordinal and
appearance index, fixed findings, bounded blockers, evidence-weakened
flags, and group-level counts. They do not include original logs,
player names, nicknames, source replay IDs, guessed move names,
item/ability values, custody signatures, or private trainer data.
These aggregate outputs are minimized, **not guaranteed anonymous**.

`brittleContradiction` flags a classification that loses its source
assertion under a weaker evidence assumption. This is a **possible
false-elimination risk**, NOT a measured false positive: there is no
ground-truth opponent build. The readiness report distinguishes
scenario coverage, abstentions, contradiction counts and those
counterfactual risks. Real-world false-elimination measurement,
installed compatibility, mechanics attestation and live/Shadow
promotion flags all remain explicitly FALSE.

If even the source claim depends on incomplete, incompatible,
transformed or conflicting evidence, the evaluator does not
certify a legal set. None of the scenarios infer unseen moves,
tie reappearances to a unique party member, assume unobserved means
impossible, or output move-choice probabilities.

## Adversarial test scope

Synthetic, ephemeral-key tests exercise all nine variants, source-only
contradictions versus exclusions contradicted by a witnessed public
move, item/ability reveal removal, source and expected form loss,
Illusion/Transform, unknown forms, same-nickname re-entries,
public source disagreement, item/ability transitions, partial records,
overrides, permutation stability, corrupted logs, private packet
injection, expired signing custody, invalid version pins,
missing signatures, candidate input limits, immutable outputs, and
zero authorized opponent eliminations.

No real public data was ingested, no Minecraft battle performed,
no private Headquarters state changed, no runtime integration made,
and no merge to `main` authorized. Legacy retains live control;
Shadow remains observational.
