# Phase 5.15 — Form inheritance and build compatibility reconciliation

Scope: **offline, source-bounded** reconciliation of Phase 5.14 with Cobblemon
1.7.3 Kotlin semantics. No real replay ingestion, installed-registry reading,
live tactics or runtime integration.

## Independently inspected resolver evidence

Public source mirror: `MayIHaveK/cobblemon-1.7.3` at fixed commit
`e428d34ea7f4aeca6dc9031488fb51fe0e315218`.

Relevant exact upstream Git blob SHA-1 values:

| Kotlin source | SHA-1 |
|---|---|
| `common/pokemon/FormData.kt` | `2dd3d7814c2538e0907e8ef2a0a048e96ee3886a` |
| `common/api/pokemon/moves/Learnset.kt` | `bdff7f678b4a0fdb4ea8138c0c2eea40fbcc3f7f` |
| `common/api/pokemon/moves/LearnsetQuery.kt` | `7a7a42db6543b4f928c9fb36ff80738b71ef3188` |
| `common/util/adapters/LearnsetAdapter.kt` | `ba6bcb82c49a9dc2e42bb9427337f89250a0426d` |
| `common/pokemon/SpeciesAdditions.kt` | `0e9549dbd086fb67690cbd27feef0e880c8b8293` |

All paths have prefix
`common/src/main/kotlin/com/cobblemon/mod/`.
The exact pinned source revision and blob IDs are validated as a
`ResolverPin` before any reconciliation. Source content is inspected in
the named immutable public revision, not a floating main branch.

## Confirmed **source** semantics

`FormData.moves` resolves `_moves ?: species.moves` and
`FormData.abilities` resolves `_abilities ?: species.abilities`.
Therefore:

- An **explicit** form move/ability field REPLACES the corresponding base
  field; source arrays are not merged or unioned.
- An **omitted** form field inherits the base value when the complete
  corresponding pinned base field is available.
- An unsupported, conflicting or incomplete species/form record must
  **abstain**; it cannot default to an apparently compatible base form.
- Hisuian Zoroark's explicit moves replace normal Zoroark's moves;
  its absent ability list inherits normal Zoroark's source ability pool.
- Mega Mewtwo Y inherits Mewtwo's moves and overrides abilities with its
  explicit form ability list. Base `pressure` must not be silently
  ascribed to the Mega Y form.
- A single ability can be listed in both normal and hidden slots (for
  example, `illusion` on Zoroark or `insomnia` on Mega Mewtwo Y).
  These are distinct valid eligibility entries, not a source conflict;
  the reconciler preserves a separate `NORMAL_AND_HIDDEN` finding.

`Learnset.kt` interprets numeric `level:move`, `egg`, `tm`, `tutor`,
`legacy`, `special` and `form_change`. `LearnsetQuery.LEGAL` includes
level, egg, tutor, TM, form-change and dynamically supplied evolution moves.
`LearnsetQuery.ANY` additionally includes legacy and special.
Consequently a `legacy`-only or `special`-only listing is **not**
`LearnsetQuery.LEGAL` evidence. Mixed methods are handled by exact
set membership rather than by an invented move priority.

**Important:** "LEGAL query member" is a description of the Kotlin
**query semantics**, not a legal current build. Species additions, runtime
move registry validity, Pokémon level, method availability, TMs, battle
format, held items and actual Cobbleverse datapacks are NOT verified.
The older Phase 5.14 source slice collapses numeric levels to `level`;
Phase 5.15 does not invent actual level thresholds or independently
reconstruct dynamic evolution/form-change moves.

## Evidence reconciliation and failure behavior

`PublicFormBuildReconciliation.reconcile` re-runs the signed-custody
Phase 5.14 intake against the exact source and resolver pins before
returning a private-constructor, immutable report.
Only per-appearance `p2a` public observations and bounded candidates
are considered. An untrusted `OverrideNotice` may only DOWNGRADE one
field: it cannot introduce a new purported eligible move or ability.
A notice on the base field downgrades inheriting forms but not an
explicitly replaced form field.

Each candidate is labeled as source-listed query-method, source-listed
non-LEGAL-query-only, source ability normal/hidden, source-not-listed,
observed-public-event/source discrepancy, incomplete-source abstention,
reported-override abstention, Transform abstention, or unknown item.
Conflicting public events are NOT proof of illegal use because Illusion,
form resolution, external mods and datapacks may explain them.

Unsupported forms do not inherit known base data. Repeated displays
never merge separate appearances. No probabilities, native build
confirmation, source-based current identity, runtime legality or
live/Shadow tactical authority are emitted.

The Phase 5.14 normalized species resource remains SHA-256-validated;
this change adds only a package-local read-only access to those already
verified base/form records. No caller-supplied snapshot can replace
them. Public game data is still not proof of the user's installed
registry; independent Phase 4 compatibility verification remains a
prerequisite for a stronger runtime-legal claim.

Synthetic tests cover explicit replacement, inherited fields,
missing form records, source/observation discrepancies, positive
and negative learn-method query membership, base versus form
override notices, Transform, replay appearance ambiguity, forged
and expired signed custody, private packet rejection, tampered
source pins and immutable outputs.

Legacy remains the sole live controller; Shadow is observational.
No merge to main and no runtime integration.
