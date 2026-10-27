# Phase 6.20 — Native Dispatch-to-Public-History Correlation Safety

**Repository:** `sensey12-mm/Maestro` · **Branch:** `Maestro-6` · **Version:** `1.0.65` · **Default:** observation disabled · **Authority:** none.

## Exact source and non-authority boundary

Cobblemon 1.7.3 is pinned in Gradle and checksum-verified by public CI. The Phase 6.19 exact-JVM-descriptor oracle verifies native `PokemonBattle.sendSplitUpdate(BattleActor,NetworkPacket,NetworkPacket)` and the separate `sendUpdate(NetworkPacket)` lifecycle hook. The only available Phase 6.18 native evidence is a **method-RETURN observation** with the original battle-object identity, monotonic turn, history-chunk count and native dispatch ordinal. It does **not** identify a specific Showdown split packet, packet bytes, or actual recipient delivery.

Phase 6.16's `PublicSplitRoutingGate` independently parses bounded, original, complete private-first/public-second Showdown split records and checks them against the separate public filter and provenance validator. It now also records **the 1-based original chunk ordinal of each structurally accepted split**, plus the monotonic public `|turn|` ordinal. Only these numeric fields are available to the new package-private correlator; no original packet, private branch, public text, nickname or UUID is retained in a correlation report. Rewriting history causes the source session to withdraw.

**Critical limitation:** There is no source-attested dispatch/event linkage ID in either existing witness. Comparable turn and history positions permit only `PLAUSIBLE_WINDOW_UNVERIFIED`. An apparent match could be coincidental. It must NEVER become a claim of identical event provenance, transport delivery, network acknowledgment, installed legal-action completeness, real-time tactical readiness, or authorization for Shadow.

## New fail-closed contract

`NativeDispatchHistoryCorrelation.Session.inspect(originalBattle, structuralReport, nativeReturnReport)` takes only original **session-issued reports**. It requires:
- Reference-equal original battle objects for both source-owning sessions, supported source pins, dynamically current leases, and exactly matching parsed public/native turn.
- An original native history count **not beyond** the observed public history length and a split event located at most one original chunk from the source-observed native history count. Even this one-chunk tolerance carries **zero identity authority**.
- Exactly **one newly accepted split** and **one newly observed dispatch ordinal** since the last positive window. More than one, skipped returns, repeated source revisions, or reusing a window are fail-closed ambiguous/replay verdicts.
- Sticky withdrawal on mismatched identities, unexpected source loss, turn mismatch, contradictory counts, ambiguous multiple splits or duplicate window reuse. Legitimately absent optional native hooks or an observed native return without a public split are **non-positive abstentions**, not false verified delivery.
- A dynamically expiring positive report: any new native dispatch, amended public history, prior-source withdrawal, native battle teardown or gate revocation immediately makes old results non-current.

Reports export only source schema, verdict, source revision, native return ordinal, turn, aggregate structurally validated split count and observed history chunk count. **All** `sameUnderlyingEventProven`, `exactSplitPacketLinked`, `recipientDeliveryAuthenticated`, `clientMessageProcessed`, `privatePayloadExportable`, `publicReadinessLeaseAuthorized`, `tacticalDecisionAuthorized`, `shadowSearchAuthorized`, and `livePromotionAuthorized` are permanently `false`.

### Runtime separation

`-Dmaestroai.phase620Audit=true` (off by default) opts in the already-existing read-only Phase 6.16 original history parser and Phase 6.18 native RETURN observer. It inspects their **original still-current results** only from the existing `MaestroBattleAI.syncPublicProtocolObservation` site, after Legacy has selected, and logs just the redacted correlation verdict. The hook creates no public source lease for split packets, reads no private requests, modifies no native packet, calls no tactical consumer and has no effect on Legacy's chosen response. Phase 6.13 continues to abstain on split traffic.

## Regression and verification

`NativeDispatchHistoryCorrelationRegression` remains in the standalone suite together with all earlier phases. It covers monotonic same-battle two-turn windows, non-authoritative flags, redacted output, one-chunk tentative timing, no-hook abstention, dispatch without split, distant and future native history positions, mismatched turn, multiple split ambiguity, skipped native return, source-window duplication/replay, cross-battle substitution in either stream, private-half history rewrite, stale native/public witnesses, native battle-end source withdrawal, public source withdrawal and terminal gate withdrawal.

**Public exact-commit acceptance:** Java build, standalone regressions, JAR/checker safeguards, exact pinned Cobblemon Mixin ABI and full pinned-source oracle must pass on the final SHA. A still-running oracle does not qualify as a complete pass.

**Minecraft required:** verify both 6.19 optional mixins actually fire against the installed Cobblemon 1.7.3 modpack, inspect native dispatch/history relative timing in controlled real battles (including multiple split events per turn), verify battle-end withdrawal and no private text in logs, and verify zero changes to Legacy behavior. Real client/recipient delivery remains unverified without a separately authenticated source-level acknowledgment. **No in-game testing has been performed** for this batch.

**Safety:** work only on `Maestro-6`, no `main` merge or trainer config edits; Legacy remains authoritative, Shadow remains non-tactical and unpromoted; no Headquarters CI usage. Independent FrAIL acceptance remains at the end of Phase 6.
