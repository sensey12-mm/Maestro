# Phase 6.11 — Native own switch, trapping and forced-choice witnesses

**Branch:** `Maestro-6` · **Version:** `1.0.56` · **Status:** source-integrated read-only observation; installed Minecraft acceptance NOT claimed.

## Exact existing source interfaces

This batch extends the existing Cobblemon 1.7.3 own `RequestInstruction.invoke$lambda$0` callback already connected to `MaestroBattleAI.ownRequestDispatched` and `OwnRequestTransport`. The owning-only request is parsed **once** and passed into both opt-in native witnesses (6.10 MOVE and 6.11 MOVE/FORCED), never taken from an opponent request, Showdown raw history or a constructed guessing interface.

`NativeOwnSwitchEvidence.capture` conservatively admits source version `1.7.3+1.21.1`, exact own `p1` or `p2` side, exact `OwnRequestJson.parse` equality to the admitted DTO, a unique active Pokémon, and an ordered roster of 1–6 UUIDs with well-formed native request HP conditions. Ordinary MOVE mode requires a *single active* request with an explicit boolean `trapped`, no unknown action/gimmick flags and no advertised Z options. Forced-choice mode requires exactly `forceSwitch:[true]` and no `active` or `wait` payload. WAIT, unexplained condition formats, unknown flags, uncertain trapping, duplicate party IDs and unauthorized request revisions fail closed.

`OwnRequestTransport.Lease.matchesOwnSwitchRequest` binds the original owning actor, battle object, installed request **object identity**, exact admitted DTO, side, active UUID and current choice lease/dispatch epoch. MOVE requests must precede their choice-turn receipt by one turn; forced replacements require that same turn's owning request. Captured observations cannot survive dispatch replacement, turn rollover, completed choice or source withdrawal.

At the existing **post-Legacy-decision read-only observation point**, the bridge cross-checks every side-party entry against the actual installed `actor.getPokemonList()` order, native `BattlePokemon.getUuid()`, `getHealth()`, `getGone()`, `isSentOut()` and `canBeSentOut()`. Candidate bench entries must be healthy, not gone/sent out, not special, and agree with the exact owning request condition and roster; otherwise the entire observation abstains. Active actor and request-turn identities must be current. A matched trapped ordinary choice yields zero voluntary switch candidates; a forced request demands at least one consistent replacement candidate. Only redacted **party-slot candidate counts**, canonical fingerprints, and boolean forced/trapped observation state may enter the opt-in Phase 6.9 sidecar. No raw party UUIDs, names or private request fields are logged.

The opt-in flag is **`-Dmaestroai.phase611Observe=true`**; it is **off by default**. Phase 6.10's prior `phase610Observe` setting remains independent. Both share the existing once-parsed own request when enabled; the no-opt-in branch preserves the original owning dispatch behavior. No mixin, live selection/scoring logic, trainer configuration or other phase branch is modified.

## Critical authority limitations

A native roster candidate is NOT an independently proven exhaustive set of executable switches. Game-specific transfer restrictions, changing battle conditions and unsupported plugins are not globally certified. All `Review` instances expose `completeLegalSetProven=false`, `executableSwitchSetCertified=false`, `opponentActionsKnown=false`, `shadowSearchAuthorized=false`, `tacticalDecisionAuthorized=false` and `livePromotionAuthorized=false`. The current result may only describe a conservative, source-matched **own switch witness**; it never chooses or executes a move/switch and cannot authenticate opponent decisions. The optional Phase 6.9 observer can receive count/forced status but cannot promote Shadow. Legacy remains authoritative.

## Regression coverage / acceptance

`NativeOwnSwitchEvidenceRegression` is wired to `StandaloneRegression` alongside all prior Phase 6.1–6.10 suites. Adversarial synthetic scenarios cover: ordinary eligible bench, trapped no-switch, forced replacement, one-shot ticket, expired lease, future turn, new owning dispatch, wrong battle/actor/request token/side/current active UUID, forced-vs-voluntary mismatch, stale/ambiguous trapping, wrong party order/size and duplicate UUID, stale HP, unavailable/gone/already-out reserves, fake revived fainted member, forced request inconsistency, unsupported gimmick or missing trapped field, source-pin drift, sidecar foreign-battle denial, irreversible withdrawal, canonical redaction and permanently non-tactical output.

**CI verification:** Only the exact final public GitHub Actions commit qualifies. Verify standalone Java/JAR checks and complete pinned simulator/source oracle, and report pending independently from success. **Minecraft testing is required later** for installed native party field and callback-timing acceptance; no Minecraft testing is claimed here. FrAIL's independent overall integration review remains for the **end of Phase 6**, not this batch.

**Safety:** never merge `main`, promote Shadow, change Legacy choices, trigger private Headquarters CI, or introduce opponent-private-data reads. Headquarters CI minutes used: **0**.
