# Phase 6.25 — Native Sided-Dispatch Runtime Evidence Review

**Branch:** Maestro-6 · **Target source revision:** 1.0.70 · **Input evidence:** owner-authorized local Minecraft 1.0.69 log; original retained privately and **not uploaded to GitHub**.

## Source-backed findings from the installed runtime

- Minecraft 1.21.1, Cobblemon 1.7.3, and installed MaestroAI **1.0.69** are declared in the runtime log. The installed artifact also self-reports an SHA-256; the exact value is retained only with private local evidence, not copied into public notes.
- The new Phase 6.24 sided RETURN observer **did fire** during the battle. It emitted native sided return ordinals, monotonically advancing per in-memory session, with two actors, a documented spectator-branch classification, and explicitly `delivery=UNVERIFIED;showdownSplit=UNLINKED;authority=NONE`.
- **Verified same-battle lifecycle defect:** before the first AI choice, two source-owned sided-return callbacks were already present (ordinals 1 and 2). The subsequent first-choice `resetMemoryForNewBattle` withdrew and replaced that same session, yielding `NO_NATIVE_SIDED_CALLBACK_YET;returnOrdinal=0` for a battle already witnessed. Later native callbacks started again at ordinal 1. This made per-battle callback counts incomplete and could mislead acceptance reports. The source branch had an unconditional `phase624Sided.withdraw(); new Session()` in first-choice initialization.
- After the reset, the observer reached a latest observed ordinal of **91**. This number is a local callback ordinal **only**, NOT client deliveries, Showdown original-event deliveries, unique packets or recipient acknowledgments. The two earlier callbacks are **not** included in that latest ordinal.
- The trace includes battle termination via the independent Phase 6.22 `BATTLE_END_HOOK_OBSERVED`. Phase 6.24's session was withdrawn by source code at end, but 1.0.69 emitted no dedicated Phase 6.24 teardown receipt, so **direct log confirmation of that session's withdrawal was unavailable**.
- Phase 6.16 still failed closed on the opening original-history grammar (later status WITHDRAWN), Phase 6.18 split RETURN remained UNOBSERVED, and Phases 6.20/6.21 did not get accepted source evidence. This is a **separate open issue**; positive sided callbacks must not be fed into the split/recipient attestation pipeline.
- The regular runtime log also contains identifying battle replay fields in **other** logging domains. The Phase 6.24 redacted entries themselves contain counts/ordinals/routing labels only. Never publish the full original log.

## Narrow confirmed defect repair

- Replace ad-hoc `NativeSidedDispatchReachability.Session` ownership with a per-physical-battle `BattleScope`. Both early native callbacks and first-choice initialization look up the **same identity-bound session**, preserving initial ordinal history.
- A **different physical battle** rotates to a fresh observer, but a **withdrawn/ended same-battle session** cannot resurrect. Late teardown from a previous physical battle does not withdraw the currently active new battle.
- Phase 6.24 now logs a dedicated **redacted teardown receipt** on physical battle end, so future runtime logs can witness its own withdrawal. The original Phase 6.22 teardown hook and Phase 6.18/6.20/6.21 gates are unchanged.
- Synthetic regression tests model early callbacks before first Legacy choice, choice-time reuse, later ordinal increments, terminal fail closed, safe cross-battle replacement, stale previous-battle teardown, battle end, and non-resurrection. No real battle payloads/names/IDs are used as regression fixtures.
- No trainer configuration or Legacy decision logic changed. All 6.24 proof/authorization flags still deny recipient delivery, packet identity, mechanics sufficiency, tactical authority and promotion.

## Verification and remaining acceptance

- GitHub CI and its pinned Cobblemon native Mixin ABI check must pass at **exact commit 1.0.70**. This is source/regression verification, not installed-game testing.
- **Observed in Minecraft:** sided-return execution on 1.0.69 and independent Phase 6.22 battle-end callback.
- **Not observed in Minecraft yet:** fixed 1.0.70 session preservation at first Legacy choice and the new dedicated 6.24 teardown report.
- **Separate remaining problem:** real original public-history grammar cannot yet establish Phase 6.16 source readiness. Fix it only with independently checked public parser shape and adversarial tests; do **not** grant split custody from a sided native callback.
- No broad new battery of battles is requested here. If a future integration acceptance gate requires 1.0.70 installed runtime evidence, reuse Team 3 in a minimal single test, rather than repeating the eight previous battles. Do not claim a successful retest until actually executed.

**Governance:** no private Headquarters CI, no merge into main, no Shadow promotion. All raw owner log data stays out of the public repository.
