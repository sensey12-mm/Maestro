# Phase 5.6 — Competitive Intelligence Shadow Integration Readiness

This is a **diagnostic-only source/API audit** of Phase 5.1–5.5 versus
existing Maestro Shadow infrastructure, not runtime integration or approval.

## Source-interface inventory

| Phase 5 output | Existing Shadow interface | Compatibility / blocker |
| --- | --- | --- |
| Canonical shown species/forms and move IDs | `PublicShadowSnapshot.opponent`, `PublicOpponentIntelligence` | Same normalized ID style; a historical displayed form is never proof of true current identity or entry continuity |
| Appearance-local move counts | `PublicOpponentIntelligence` prediction audit | Counts are descriptive, past-only; no calibrated action distribution and no current battle/turn binding |
| Switch announcement and forced drag counts | `ShadowSwitchTelemetry`, `PublicStateSearchAdapter` | Switch causes and destination identity UNKNOWN; no legal current switch actions |
| Permission-attested replay and de-identified provenance | `PublicBattleCapture`, `PublicProtocolEvidence` | Replay ingestion is *offline* and independent of live capture envelopes; no equivalence, actor binding, freshness or live evidence lease |
| Pinned species and ordinary move source subset | `PublicMechanicsEvidence`, `RuntimeDomain` | Source presence is not an initialized runtime registry receipt or complete effect coverage; cannot supply current damage/typing/speed |
| Held-out accuracy and simple baselines | `ShadowDecisionComparator`, `PublicStateSearchAdapter.project` | Aggregate, not per actor/turn; never action scores, probabilities, source-certified outcomes or legal choices |
| All of Phase 5 | `MaestroBattleAI.choose` and `LivePromotionGate` | Legacy still chooses live actions. Phase 5 has **no registered/promoted candidate** and must not be wired into live selection |

## Contract and evidence limitations

`PublicCompetitiveShadowContract.audit` checks source/provenance equivalence,
generation/tier, version scope, identical appearance indices/slots,
canonical displayed IDs, Transform/identity ambiguity, observed move token
sets and side-entry counters. It emits only fixed blocker codes and
booleans—**no replay IDs, player nicknames, input hashes, raw records,
moveset history, action candidates, damage values or prediction scores**.

Even when offline schemas match, it ALWAYS blocks Shadow tactical
readiness, runtime binding and live promotion: historical replay
observations are not active public battle facts. Replay rights are
operator-attested, not independently authenticated. Upstream replay
IDs remain potentially re-identifiable and cannot be treated as
irreversibly anonymous. Record constructors can be called externally;
schema matching alone is not cryptographic custody authentication.

The audit does not produce a `BattleSnapshot`, `BattleSnapshot.Decision`,
`ShadowDecisionComparator.Candidate`, a `PublicStateSearchAdapter`
transition, a legal own request, or a `LivePromotionGate` flag.
The held-out aggregate has no actor/turn index and is not transferable
to a current match. The existing live `MaestroBattleAI`, all
`com.maestroai.ai` classes and runtime dispatch wiring are unchanged.

## Independent integration review prerequisites (not done here)

1. Agree across Phase 4/5/6 with the integration reviewer on exact
   source/version/format evidence and canonical IDs; never extrapolate
   from pinned Showdown or Cobblemon source to current runtime registry.
2. Separately establish public spectator provenance, actor/entry/turn
   freshness, causal ownership of current observations, current own
   legal requests and version/registry receipt. Offline replay hashes
   cannot supply this proof.
3. Independently test conditional mechanics/unknown opponent responses,
   ambiguity and anti-leakage, and require reviewer/owner approval
   before any future Shadow consumer is even considered.
4. Any eventual live integration requires separate authorization and
   real in-game verification. **No Minecraft test is required for this
   offline readiness-audit batch.**

Phase 5.6 is limited to code contracts, documentation and synthetic
regression tests. No new tactical behavior, no main merge, no
Phase 5.7 or runtime integration.
