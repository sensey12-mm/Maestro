# Phase 5.13 — Evidence-bounded opponent build hypotheses

Phase 5.13 adds `PublicOpponentBuildHypotheses` as an **offline,
appearance-local interpretation** of permitted competitive spectator
replays. It makes no current-battle, identity, legality, frequency or
winning-probability claims.

## Evidence levels and strict source boundaries

The report distinguishes four non-interchangeable categories:

- **PUBLICLY_OBSERVED_EVENT:** a native move was publicly used in that
  particular appearance, or an item/ability was shown by a public replay
  event. An item/ability event is **not** proof of a Pokémon's originally
  selected build or its current held item/ability after later changes.
- **SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN:** a caller-suggested move ID
  is present in the exact pinned Cobblemon simulator move slice
  (`CobblemonMechanics.SOURCE`). This shows **global move existence
  only**, NOT a verified species learnset, format legality or even that
  this appearance can use the move. This is a *conditional possibility*,
  never a confirmed set slot.
- **SOURCE_ROLE_SIGNAL_CONDITIONAL:** a publicly observed move has a
  source-covered positive Attack/Sp. Attack boost or positive priority.
  Reports `attack-boost-signal`, `special-attack-boost-signal` or
  `priority-move-signal` **without** assigning a full strategic role,
  predicted EVs or tactical recommendation. Runtime effects remain
  unverified.
- **UNKNOWN_UNVERIFIED:** a caller-proposed move absent from the pinned
  move slice or **any unobserved item or ability**. No item/ability
  eligibility, possible choices, legal sets or hidden probabilities
  are inferred.

The pinned `PublicSpeciesCatalog` establishes source-backed base stats
and types for selected species, NOT the species-specific learnsets,
hidden ability eligibility, held-item catalog/availability, Megas,
formation rules or online player build frequencies. Consequently,
`speciesLegalityVerified` and `currentBuildConfirmed` stay FALSE
for every hypothesis. No invented species/ability/item roster is added.

Only a *specific, signed-custody-admitted original replay* is accepted.
The ingestion rechecks the original replay, Phase 5.2 normalization,
Phase 5.3 tendencies, Phase 5.6 source consistency, and current Phase 5.9
Ed25519 custody. The signed operator attestation is still not
independent proof of the replay's origin or licensing rights.
Unsupported or out-of-version targets fail closed.

## Appearance and uncertainty discipline

Rows are keyed to each **chronological p2a appearance index**, not a
nickname, species, reappearance, team position or unique party member.
No p1a observations enter the opponent report. A caller's optional
candidate guesses are **untrusted** and must target one existing p2a
appearance; duplicates or cross-side indices are rejected.

Revealed facts are never transferred to a later same-species/same-nickname
appearance. Transformed appearances suppress item/ability confirmations,
conditional move possibilities and role inference. Non-native copied
moves are not treated as native legal set slots. A repeated display
remains a possible Illusion/re-entry ambiguity, not a party-member
identity proof.

A bounded, immutable report includes only canonical species/move/ability/
item IDs and fixed role signals. It contains no raw lines, nickname,
player string, public replay ID, signature, or per-player probability;
however, these gameplay facts still may be recognizable and should
**not** be redistributed as irreversibly anonymous.

## Adversarial regression fixtures and unfinished work

Synthetic tests cover public move/item/ability reveals; known and
unknown move candidate IDs; narrow boost and priority signals; repeated
species re-entry; Transform; p1/p2 separation; private event injection;
forged source, malformed candidates, unsupported versions, expired
custody and wrong signed replay content. They assert no synthetic
candidate upgrades into a verified species set, underlying ability,
held item legality or tactical action.

To advance genuine possible opponent builds, an independent Phase 4
and integration review must provide version-pinned **species/form-specific
legal learnsets, ability eligibility, item mechanics and format
restrictions**, validated against the actual installed registry.
Historical replay observations alone cannot establish them.

This batch contains no unapproved replay ingestion, no real player
corpus, no probabilistic prediction calibration, no live policy change,
no runtime integration, no Minecraft testing, and no merge to main.
Legacy remains authoritative; Shadow remains observational.
