# Phase 5.17 — Evidence-bounded opponent build candidate filtering

This phase adds `PublicBuildCandidateFilter` as a deterministic, **offline,
signed-evidence-gated** classifier of partial build assertions. It is NOT
a set generator, a live battle component, an opponent identity resolver,
a calibrated probabilistic model or a complete legal-set validator.

## Trust and provenance

Every call reconstructs Phase 5.13 public appearance observations,
Phase 5.15 species/form reconciliation, and Phase 5.16 build consistency
from the original spectator log. Each underlying stage rechecks current,
operator-attested, Ed25519-signed custody under the separately trusted
policy and exact verification time. No publicly constructible prior
report or caller-supplied source eligibility record is accepted.

Only the pinned Cobblemon 1.7.3 public-source revision
`e428d34ea7f4aeca6dc9031488fb51fe0e315218` and the existing
pinned simulator target are supported. The source snapshot does **not**
verify the installed modpack registry, item legality, acquisition
methods, format restrictions, or permission to infer hidden state.

## Candidate assertions and verdict meaning

A `Candidate` binds to one **chronological p2a public appearance**
and specifies the expected *displayed* form plus optional required moves
(up to four), explicitly forbidden publicly observed moves, one
hypothetical ability and one hypothetical item. These are partial,
untrusted assertions, **not** claims that four chosen moves are the
opponent's exact moveset or that a held item is currently active.
Candidates may not link different appearances by nickname/species.

The output uses four descriptive statuses:

- **SUPPORTED**: every positively asserted move/ability is listed by
  the exact pinned source under an appropriate query/ability slot, and
  no reported conflict, transition or relevant unknown blocks that
  limited source claim. This says **source-conditional compatibility
  only**, never the candidate was actually selected.
- **CONTRADICTED**: the candidate explicitly excludes a native move
  publicly witnessed in that same appearance, its expected *display*
  differs, or a positively asserted move is absent from the complete
  pinned source slice. It contradicts the **specific asserted
  evidence/source scope**—not proof of cheating or real-world illegality.
- **UNRESOLVED**: item eligibility, an ability not in the narrow source
  slice, a move listed only under `legacy`/`special`, or an empty
  positive assertion cannot establish source-supported compatibility.
- **ABSTAINED**: observed/source discrepancies, Transform, unrecognized
  displayed forms, form transitions, multiple public items/abilities,
  more than four publicly observed native moves, or an override/
  incomplete source field relevant to the candidate prevent a reliable
  classification. This verdict takes priority over contradictions.

A public ability or item reveal is an **event**, not reliable proof of
original/current build-slot selection. For this reason unlisted
hypothetical abilities are **UNRESOLVED**, not convicted as illegal;
all items remain unknown, including items that were publicly revealed.
Public item/ability transitions force abstention rather than
inconsistent native set construction.

**Candidate independence:** unioned guessed IDs are forwarded once per
appearance to the existing bounded source pipeline, but judgments are
made per submitted candidate. Unsupported candidates do not downgrade
an unrelated source-supported candidate. Positive move hypotheses
may have source backing even when not witnessed; nothing claims they
were carried in the battle. An explicit negative assertion is tested
only against already witnessed native public move IDs from the same
entry. No negative inference is made from a move that was simply not
seen.

## Minimized output and restrictions

Reports contain ordinal candidate numbers, appearance indices,
fixed verdict/reason enums and counts (source-supported assertions,
observed matches, contradictions and unresolved assertions).
No input move/item/ability or candidate string is echoed; no raw
replay, player/nickname, signature, or replay ID enters the report.
These outputs minimize personal data but are NOT irreversibly
anonymous. Every result keeps `opponentIdentityResolved=false`,
`completeNativeMovesetVerified=false`, and
`installedLegalityVerified=false`.

The report always has `livePromotionAuthorized=false` and
`shadowTacticalReady=false`. This phase does not teach Maestro
probabilities or install predictions into any tactical pathway.
Legacy continues to control live battles; Shadow remains observational.

## Synthetic regression coverage

The adversarial suite tests supported, contradicted, unresolved and
abstained verdicts with multiple hypotheses on one appearance;
revealed versus hypothetical moves/abilities; unlisted moves,
unknown items and `legacy` methods; base/form inheritance, Mega and
Hisuian forms, mismatch expectations, wrong-side indices, Transform,
public form/ability/item transitions, source disagreements,
five observed native moves, override downgrades, repeated nickname
isolation, tampered/expired custody, private protocol injection,
malformed candidates, immutable reports, and privacy-minimized output.

No unapproved real replay data, private Headquarters content,
runtime integration, Minecraft acceptance test or merge to `main`
is included. A separate independent integration review and
installed-Cobbleverse compatibility validation remain prerequisites.
