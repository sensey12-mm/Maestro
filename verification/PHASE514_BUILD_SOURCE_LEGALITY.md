# Phase 5.14 — Version-pinned public build eligibility evidence

This batch extends Phase 5.13 with a **fail-closed, read-only
species/form source-evidence adapter**, not a universal Cobblemon legality
validator, real opponent set detector or runtime connection.

## Independently inspected public source

Pinned public Cobblemon 1.7.3 source mirror:
`MayIHaveK/cobblemon-1.7.3` at exact commit
`e428d34ea7f4aeca6dc9031488fb51fe0e315218`.

Inspected complete JSON resources in
`common/src/main/resources/data/cobblemon/species/`:

| Species source | Exact Git blob SHA-1 |
|---|---|
| `generation5/zoroark.json` | `155605e52064164f59524f0ab427aec177b8ca22` |
| `generation1/ditto.json` | `902498a9744183b3d420432b7985e4ade1d7f696` |
| `generation1/pikachu.json` | `d434536ae152ae042fb6648e1306d01ee7eab79a` |
| `generation1/mewtwo.json` | `457721b23730989bc09883a9cd4a6669753c4878` |

The source slice contains **complete move and ability entries as
explicitly listed in those files**, not manually selected competitive
sets. It includes the base species above, Hisuian Zoroark's explicit
form moves, and Mega Mewtwo Y's explicitly overridden ability slots.

A deterministic normalized TSV retains source file Git blob identity,
exact form name, complete listed ability entries, move IDs and their
original learning-method prefixes. The pinned SHA-256 of **all normalized
TSV bytes** is
`76b216aa5f3f1f16e866403d7bcc6ff297d18a2a83010c83777ea6730eb23b0d`.
A changed, truncated, missing, duplicated or extra species record fails
closed. Supported species/forms are explicitly enumerated; the adapter
will not automatically inherit legality from base species or guess
unlisted forms.

**Trust boundary:** This source is an inspectable public mirror, not
independent attestation that the user's installed Cobbleverse datapacks
match its exact files. The mirror revision, normalized checksum and
source data are pinned from the inspection; they do **not** establish
production legal-move certification, modpack equivalence, current server
rules or permission to read private game data.

## What the adapter actually certifies

`PublicSpeciesBuildLegality.analyze` requires the complete signed replay,
current Phase 5.9 Ed25519 custody, source-pinned Cobblemon target, exact
local source checksum, Phase 5.13 appearance-local candidate report, and
only current source-form entries.

It labels each candidate as:

- **SOURCE_LISTED_LEVEL_MOVE:** species/form's explicit source list
  contains a `level:move` record. Exact level remains known from the
  source file but runtime level/ability/format legality is not proven.
- **SOURCE_LISTED_OTHER_METHOD:** source explicitly contains the move
  under `egg`, `tm`, `tutor`, `special`, or `legacy`. The list
  of present method categories is retained. A listed method does not
  establish that method is available on the installed game.
- **SOURCE_LISTED_NORMAL_ABILITY / SOURCE_LISTED_HIDDEN_ABILITY:**
  an ability appears in an explicit normal or `h:` source ability slot.
  This does not certify activation mechanics, actual slot selection, or
  the identity of the battle opponent.
- **UNKNOWN_IN_SOURCE_SLICE:** no exact entry is found, or item legality
  is not independently attested. Absence does **not** mean illegal in
  an altered datapack or future version.
- **SOURCE_FORM_METHOD_OR_ABILITY_UNSPECIFIED:** a form omits its own
  move or ability entries. The adapter refuses to infer inheritance
  without separately source-verified resolver semantics.
- **TRANSFORM_OR_IDENTITY_UNCERTAIN:** copied moves and transformed
  displays cannot be used as native build legality evidence.

Examples from pinned files: ordinary Zoroark explicitly lists
`nastyplot` as level-learned, and `swordsdance` under a TM method,
but does not list `shadowsneak`. Hisuian Zoroark explicitly lists
`shadowsneak` in the form, while its ability list is omitted.
Ditto lists `limber` and hidden `imposter`. Mega Mewtwo Y
lists `insomnia`, but has no explicit form moveset, so source
method inheritance remains unknown.

## Compatibility and privacy boundaries

All source entries use existing MaestroAI canonical IDs and the
`CompetitiveFormatNormalizer.Target.pinnedSource()` source revision.
An observation is still stronger evidence of a public event, while a
source listing is only **possible compatibility evidence**. Neither can
confirm an opponent's undisclosed move, actual held ability, form
mechanics, current equipped item or complete set legality.

The report is immutable, appearance-local, opponent-side only,
probability-free and Shadow-observational. Repeated identical nicknames
do not justify merging appearances. It includes no raw replay lines,
player names, or public replay IDs, though gameplay attributes may be
recognizable and should not be considered permanently anonymized.

Independent Phase 4 integration must still verify registry/datapack
overrides, inherited form rules, learn-method execution, ability effects,
held-item legality and the actual supported Cobblemon runtime before
stronger **gameplay legality** can be claimed.

Tests are synthetic only: pins, resource corruption, missing records,
version drift, forms, source learning methods, normal/hidden abilities,
unobserved candidates, unknown items, transformations, identity
ambiguity, privacy leakage and forged/expired custody. No unapproved
replay data was ingested. No Minecraft test, tactical promotion, main
merge or runtime integration was performed.
