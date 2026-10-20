# Phase 5.2 — Version-scoped competitive observation normalization

Implementation: CompetitiveFormatNormalizer. It consumes Phase 5.1's sanitized
PublicReplayCorpus.Evidence, not replay text or internal battle state.
Supported Showdown format metadata: gen9ou and gen9ubers, generation 9 singles.
Rules and banlist contents are UNKNOWN (not captured by the Phase 5.1
observation projection); no OU/Ubers clauses are transferred to Cobblemon.

The only positive source presence scope is the pinned Cobblemon 1.7.3 build
7553235, Minecraft 1.21.1, and the exact CobblemonMechanics.SOURCE string.
Species/form presence is checked with existing PublicSpeciesCatalog.ids()
using an EXACT displayed form; a verified base species never certifies a
Mega/Hisui/other form. Move presence uses CobblemonMechanics.move(id); only
its damageOnly() && power()>0 narrow conditional ordinary single-hit
subset receives a verified *source-scoped* mechanics label. In-battle
outcomes, modifiers and runtime effect validity remain UNKNOWN.
Ability/item IDs are canonical public observations but their source/runtime
mechanics remain UNKNOWN without a separate verified registry receipt.
No supplied Target claims an installed registry or Minecraft runtime attestation.

The explicit form-to-base IDs are taxonomic aliases only. They do not imply
party identity, transfer previously seen moves, or prove underlying type,
stats, ability, held item, game-mechanic compatibility or form activation.
Repeated appearances stay separate and identityUncertain=true. Unknown
tokens cannot inject nicknames/user data into normalization results.
De-identified replay ID, input SHA-256 and observation counts survive as
provenance; replay-use authorization remains an *upstream operator
attestation*, never independently reverified by this layer.

Cross-version inputs are OUTSIDE_VERIFIED_SCOPE, not "absent from Cobblemon".
Out-of-catalog terms are UNKNOWN, never inferred illegal or unimplemented.
Showdown tier bans/clauses/format rules are not inferred from tier names.
This layer has NO tactical caller, NO downloader and NO prediction weighting;
Legacy live control and Shadow-only safeguards remain unchanged.

Primary upstream documentation:
https://github.com/smogon/pokemon-showdown/blob/master/sim/SIM-PROTOCOL.md
https://github.com/smogon/pokemon-showdown/blob/master/data/FORMES.md
https://github.com/smogon/pokemon-showdown/blob/master/sim/dex-formats.ts
This batch uses synthetic regression fixtures, not real replay ingestion,
new replay permissions or Minecraft acceptance.
