# Phase 5.21 — Shadow-ready competitive build evidence handoff

**Status: offline interface implemented for future independent Phase 6 review; NOT runtime integration or tactical authorization.**

Phase 5.20 accepted independent offline Phase 5.1–5.19 recomputation, not cross-phase integration. This batch makes the underlying **named evidence** useful to a future Shadow interface while preserving the Phase 5.19 privacy- and source-limited consumer boundaries.

## The new contract: `PublicShadowBuildHandoff`

- **Schema:** `phase5.21.shadow-build-evidence.v1`; requires pinned `PublicCompetitiveConsumerContract.ContractPin` and Phase 5.20's *inspected*, not integrated, `CrossPhaseSnapshot`. Source revision `e428d34ea7f4aeca6dc9031488fb51fe0e315218`, pinned normalized species slice, pinned form resolver, mechanics source and frozen robustness policy are cross-checked.
- **Authority:** Input consists of the **original source-attested public spectator replay**, current signed-custody manifest/policy, explicit timestamp, bounded appearance-specific candidate assertions, conservative override notices, and optional verified-subset constraints. The class **recomputes** Phase 5.13 observations, Phase 5.15 form evidence and the entire Phase 5.19 consumer contract (itself rerunning Phase 5.16–5.18). It never accepts a caller-minted evidence report as authority.
- **Output:** An immutable `Report` with chronological opponent **appearance** ordinal, public *displayed* form ID, evidence-graded `Signal` records (moves, abilities, items and conditional role clues), and ordinal `Possibility` records keyed to a caller-proposed partial build. A proposed set of moves is **not** a closed four-move set. Source-listed eligibility is **not** installed-game legality.
- **Privacy:** No replay ID, player name, nickname, raw private/public replay lines, signing keys, signatures, or chat is retained. Source-relevant public species, move, ability and item IDs *are intentionally exposed* to make the interface useful, but with provenance grades and strict scope. This makes it more informative and somewhat less minimized than Phase 5.19's count-only export; an owner-authorized privacy review is required before any public exposure of derived per-replay evidence. No external network request, upload or runtime integration is implemented.
- **No false authority:** `currentBattleEvidenceBound`, `legalActionsAvailable`, `calibratedOpponentProbabilities`, `speciesIdentityProven`, `completeLegalSetProven`, `phase6SearchAuthorized`, `shadowTacticalReady` and `livePromotionAuthorized` are **always false**.

### Evidence grades consumers may interpret

| Grade | Permitted meaning | Not permitted |
| --- | --- | --- |
| `PUBLIC_EVENT_ONLY` | A move/item/ability was recorded in the original signed public replay; source legality is not established | Claim it remains equipped or belongs to a native build |
| `PUBLIC_EVENT_SOURCE_ALIGNED` | Same public event and pinned species/form source eligibility agree | Claim an installed-game legal set or an identified hidden combatant |
| `SOURCE_SUPPORTED_POSSIBILITY` | An unobserved **caller-proposed** move/ability appears under source-listed learn methods/ability slots; conditional roles are source-backed behavioral signals | Claim the move is carried, the ability activated or the role will be chosen |
| `SOURCE_LISTED_NONLEGAL_QUERY_ONLY` | Listed under a method such as legacy/special that the pinned Cobblemon `LEGAL` query does not include | Promote to currently learnable |
| `UNVERIFIED_PROPOSAL` | Caller-proposed item/ability/move lacks matching pinned eligibility proof; items stay unknown | Eliminate the Pokémon or treat a guess as evidence |
| `SOURCE_DISAGREEMENT` | Public event and the narrow source slice disagree | Declare cheating, a verified impossible set or a definite hidden identity |
| `ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS` | Transform, unsupported source, override or other untrustworthy native-build assumptions | Use it as verified opponent build information |

Every grade carries explicit limits on native-build proof, installed registry, calibration, and identity where relevant. Publicly observed events are kept separate from source-only guesses. Method labels such as `level` or `tm` are only source category names, not proof of actual move teachability.

## Observation constraints: exactly what is safe now

`PublicObservationConstraint(appearanceIndex, witnessedMoves, revealedAbilities, revealedItems)` is a **subset selection from evidence ALREADY authenticated in the same signed offline replay and same chronological opponent appearance**. All requested IDs must be contained in the corresponding public-event sets. A fabricated move, wrong-side index, misbound repeated nickname, source discrepancy, Transform or ambiguous item/ability/form transition **fails closed**. It cannot create a new observed move or boost confidence beyond the original signed source.

**For a future *current* public-battle adapter, not implemented here:** Phase 6 must obtain only currently public data through a separately authenticated source/turn/actor/appearance receipt, verify monotonic chronology, field revelation and form-state transitions, and reconcile source IDs with Phase 4 mechanics at the installed registry version. Its inputs must never include private `|request|` payloads or inferred party identity. Fresh public observations may narrow the *set of conditional hypotheses* only through a new reviewed interface, not by changing existing offline receipts or making unseen moves impossible. Missing evidence means abstain or retain alternatives. This batch does not establish live/Shadow tactical input eligibility.

## Phase 6 integration contract and unresolved prerequisites

Phase 6 may later consume **only after independent owner-authorized adapter review**:

1. Exact pinned schema/source/mechanics/policy metadata, and locally verified rights/custody/provenance.
2. Appearance-local observed-public event IDs labeled as *events*, not native/current slots.
3. Source-backed possible moves/abilities with learn-method categories, conditional role signals and explicit unknown items, each with uncertainty/abstention codes.
4. Caller-proposed partial build classifications and conservative robustness findings, but **not probabilities, legal actions or elimination orders**.

Phase 6 **must not**:

- Equate chronological `appearanceIndex` or displayed species with a stable opponent team member (Illusion and re-entry), merge distinct appearances, or assume a four-move set.
- Extrapolate `SOURCE_SUPPORTED_POSSIBILITY` into a revealed move, treat an unlisted item as impossible, or regard a contradiction as cheating/verified disproof of a native set.
- Insert candidate grades as response probabilities into `ShadowSearchEngine.Outcome`; Phase 6's searched branches still require independently justified legal actions, response models and state transitions.
- Attach this report to a current battle, supply tactical recommendations or grant Shadow policy authority. Legacy remains the only live decision-maker.

**Still blocked:** externally pinned durable trust checkpoint continuity (Phase 5.10), independently verified replay-use rights, permitted real-replay held-out predictive validation and calibration, complete installed Cobbleverse datapack/source/mechanics evidence (Phase 4), current turn/actor public snapshot binding and legal-action authority (Phase 6), end-to-end integration and benchmarking (Phase 7), FrAIL independent cross-phase compatibility review and separate owner permission. The public Phase 5.20 readiness gate remains blocked for those dependencies; no change in this batch overrides it.

## Synthetic acceptance

The adversarial suite uses synthetic replay-only events and ephemeral signing keys. It checks observation/source/unknown grades, source-listed method categories and unsupported item proposals, repeated appearances, source/form inheritance, Transform and unsupported forms, public/source discrepancies, staged overrides, forged observation constraints, illegal private packet injection, source/version drifts, replay tampering, expiry, immutable outputs and permanently denied tactical/legal authority. **No Minecraft installation or permitted real replay corpus was tested in this batch.**

Production code and public-safe specification live in public Maestro branch `Maestro-5`. Private release/approval coordination and independent integration decisions belong in private Headquarters, not in public artifacts. No merge to `main`, public replay ingestion, tactical promotion or runtime wiring is included.
