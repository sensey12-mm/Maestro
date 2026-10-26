# Phase 6.12 — Native Own-Action Evidence Fusion and Safety Audit

**Source branch:** `Maestro-6` · **Version:** `1.0.57` · **Scope:** a read-only current-turn coverage record. No authority for Legacy selection or tactical Shadow.

## Reviewed source and integration

Phase 6.10's `NativeOwnActionEvidence.Review` is a fresh, own-actor, exact-installed-request, ordinary move-slot witness, and Phase 6.11's `NativeOwnSwitchEvidence.Review` is an independently source-pinned same-choice party/trapping/forced-replacement witness. Neither is an exhaustive Cobblemon legal-action certificate. Both already use `OwnRequestTransport.Lease` and have one-use native attestation; the Phase 6.11 ledger binds the exact already-parsed private raw request with a process-salted digest so dropped normalized request fields cannot be swapped.

Phase 6.12 adds a narrow **package-private owner seal** to both witness review implementations. `NativeOwnActionFusion.Session.inspect` requires each participating review to match the **same reference-equal, currently valid owning choice lease**, `PokemonBattle` object, actor object, installed request object, own side, active UUID, source turn and `OwnRequestTransport.SOURCE`. It requires consistent forced mode and trapping facts, strictly current input and a non-tainted Session. A new session can be created at a *new battle* boundary only. A failed fusion irrevocably withdraws the current Session; reusing a previously consumed lease is rejected. Expiration, source withdrawal or underlying owning transport change immediately invalidates an earlier returned Review.

### Explicit branch behavior

- **Ordinary MOVE request:** both native move witness and native switch witness are mandatory; no partial positive certificate. Trapped state must agree in both; trapped means zero observed voluntary switch candidates. The report contains only own move count, conservative observed switch count, source-turn, mode and a deterministic fingerprint over the already canonical, source-pinned candidate hashes.
- **Forced replacement:** only the native switch/forced witness is allowed; any move review is an error, and at least one consistent source-derived replacement candidate is necessary. This is NOT an independent proof that the full installed action set is executable.
- **Missing, contradictory, expired, foreign or withdrawn evidence:** no fused report is returned. Error text never includes request bytes, private player identity, Pokémon UUIDs, nickname or opponent information. The Session withdraws rather than guess.

All results permanently expose `completeLegalSetProven=false`, `allInstalledSwitchesCertified=false`, `opponentActionsKnown=false`, `tacticalDecisionAuthorized=false`, `shadowSearchAuthorized=false`, `livePromotionAuthorized=false`. A SHA-256 fingerprint is synthetic correlation, **not** remote runtime authentication.

## Read-only runtime site and opt-in

The existing `MaestroBattleAI.choose` flow still selects its response **first** through `LivePromotionGate.choose(() -> chooseLegacy(...),null,null,false)`. The new observation runs *after* Legacy selection and before `OwnRequestTransport.endChoice`. `-Dmaestroai.phase612Observe=true` (off by default) enables the two already existing own-only 6.10/6.11 witnesses, collects their reports and performs one deterministic fusion per owning request lease. A failure only prints `own-action fusion=UNKNOWN`; it never retries or changes Legacy's response, score or legal-option selection.

The optional Phase 6.9 public observation sidecar records only redacted counts and forced mode from a currently valid report bound to the same battle object. No private-own request, raw Showdown private split, opponent request, UUID, action string or nickname flows through that sidecar. It remains observational; a reported hash/count cannot promote tactical Shadow. Previously existing `phase610Observe` and `phase611Observe` flags continue working separately.

## Adversarial synthetic regression

`NativeOwnActionFusionRegression` is registered in `StandaloneRegression` with all previous Phase 6 suites retained. It verifies exact-source normal, trapped and forced cases; deterministic fingerprint; redacted diagnostic; permanent non-authority; battle-specific sidecar; forced-mode rejection of injected moves; missing components; source-turn mismatch; foreign actor/battle/request token/side/active UUID; same-looking cross-battle and cross-lease evidence; trapping contradiction; consumed lease replay; lease close; witness revocation; session poison permanence and bridge withdrawal.

These are **synthetic Java regressions** and source-level compilation, not installed modpack/Minecraft tests. Public exact-commit Java, JAR and pinned source-oracle GitHub Actions must finish before full CI may be claimed. In-game installed Cobblemon request shapes, dispatch timing and party state reconciliation remain acceptance prerequisites for runtime use.

**Safety:** branch `Maestro-6` only, no `main` merge, Legacy live control unchanged, no Shadow tactic authority or promotion, no trainer config modifications, no Headquarters CI runs (zero private minutes). Final independent FrAIL integration review stays at the **end of Phase 6**, not between batches.
