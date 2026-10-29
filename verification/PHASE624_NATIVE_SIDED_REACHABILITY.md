# Phase 6.24 — Native Split-Dispatch Reachability Audit

**Branch:** Maestro-6 · **Version:** 1.0.69 · **Status:** source-level call-path gap identified; separate native sided-return observation added, Minecraft retest pending. Legacy remains the only live decision-maker.

## Verified source-level finding

The Cobblemon 1.7.3 source contains **two distinct Kotlin native methods**:
- `PokemonBattle.sendSplitUpdate(BattleActor, NetworkPacket, NetworkPacket)`: routes one packet to the private actor and another to others. The existing optional Phase 6.18 Mixin attaches at this method's RETURN. The method declaration's existence was checked by the pinned 1.7.3 bytecode ABI oracle, but **its invocation in any particular fight was not established**.
- `PokemonBattle.sendSidedUpdate(BattleActor, NetworkPacket, NetworkPacket, boolean)`: routes an **ally-side** packet to source-side actors and an **opponent-side** packet to opposing actors; the boolean selects the spectator branch. The pinned 1.7.3 **source files** for `SwitchInstruction`, `DamageInstruction`, `HealInstruction` and `SetHpInstruction` invoke **this** method for switch and health presentation; their calls are not dispatches to `sendSplitUpdate`.

Upstream source reference (public source mirror, not user's private data):
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/api/battles/model/PokemonBattle.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/SwitchInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/DamageInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/HealInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/SetHpInstruction.kt

**What this explains and what it does not:** the owner's private diagnostics-enabled Minecraft log showed absence of Phase 6.18 sendSplitUpdate RETURN observations, while a separate battle-end callback was observed. That is **consistent** with battles using the different sided send path. It is not proof the old Mixin did or did not apply, nor proof sendSplitUpdate is globally unreachable: the absence of a witnessed native call is an *unknown*, not a failed packet delivery. A bytecode method declaration alone is not a call-site / applied-Mixin attestation.

## Source change: isolated diagnostic-only sided-return witness

- New opt-in flag: `-Dmaestroai.phase624Observe=true` (independent of Phase 6.22 and Phase 6.18 flags; can be enabled beside Phase 6.22 for comparison). Off by default.
- Optional pinned exact JVM ABI `sendSidedUpdate(BattleActor,NetworkPacket,NetworkPacket,Z)V` `RETURN` Mixin. It invokes `MaestroBattleAI.ownNativeSidedDispatchReturned` only for the existing AI battle actor. It never intercepts/cancels/sends/reorders original packets.
- `NativeSidedDispatchReachability.Session` records a source-owned **method-return callback ordinal**, observed native turn/history counts, actor count and a **boolean spectator branch label only**. Multiple legitimate sided calls at the same turn/history epoch are permitted; the ordinal means number of callbacks, **not** number of delivered events or public/private Showdown frames.
- Battle owner/actor **object identity** is checked only in-process; no object/payload/UUID/name escapes into a report or diagnostic. Cross-battle callbacks, foreign/duplicated actors, missing/equal packet object references, stale turn/history, source pin mismatch and lifecycle end fail closed. A new physical battle gets a fresh session.
- Existing `NativeSplitDispatchObservation`, Phase 6.20 correlation and Phase 6.21 coverage modes are **unchanged**. A sided RETURN **does not satisfy** any missing original split-return/recipient callback: those tracks stay UNKNOWN. `nativeSplitMethodReturnedProven()`, recipient delivery, actual Showdown split event identity, private payload export, opponent knowledge, tactical authorization and live promotion remain `false` for every 6.24 report.
- The 6.24 status poll says `NO_NATIVE_SIDED_CALLBACK_YET` until a real callback; after one, `NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED` describes the last source-owned local method return, not client receipt.

## Deterministic verification

- `NativeSidedDispatchReachabilityRegression` registered in `StandaloneRegression`: no-callback abstention, source-owned call with return ordinal, same-epoch multiple callbacks, either spectator branch, unchanged status, chronological growth, bad actor source, duplicate/missing/mismatched actors, equal/missing packets, version drift, stale native epoch, wrong battle, teardown and non-resurrection. Every fixture asserts no delivery / authority claim and no raw payloads.
- `verification/runtime_hook_oracle.py` now checks **both** exact method descriptors against the hash-checked pinned Cobblemon class and checks the optional new Mixin target. This preflight cannot attest actual Mixin application.
- GitHub Actions must pass all Java/build/regression, JAR/native ABI, and external pinned simulator source gates at the exact new commit. Public repository contains only source tests and this public-safe report, **not** the owner's raw Minecraft logs or identifying data.

## Targeted Minecraft retest — PENDING

Use **only the exact-commit verified 1.0.69 JAR** on the existing Minecraft 1.21.1 / Fabric / Cobblemon 1.7.3 / RCT 0.15.2-beta environment. One short owner-authorized fight with **one voluntary switch and one health-changing move** is enough to exercise the distinct source path; a second fight is necessary only to examine battle-end/reset behavior if not naturally included. No eight-battle repeat is requested.

1. Record the git commit, Maestro version and installed JAR SHA-256 privately. Enable `-Dmaestroai.phase624Observe=true` at JVM startup. Optionally leave `-Dmaestroai.phase622Observe=true` for existing history diagnostics; do **not** confuse its output with the new native return witness.
2. Capture only **redacted** `[MaestroAI] Phase 6.24` lines. Before a callback, expect `NO_NATIVE_SIDED_CALLBACK_YET`; after an actual callback, expect `NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED` with increasing return ordinal. If absent, record UNOBSERVED and check optional Mixin application before blaming delivery. If logged as withdrawn/source unavailable, audit the exact rejection using private evidence; never coerce positive values.
3. Finish the battle and start another, confirming terminal withdrawal and fresh no-callback initial state. Verify the result cannot modify Legacy's action selection or Shadow authority.
4. Keep Phase 6.18 `sendSplitUpdate` activation, recipient delivery, source event identity, packet ACK and Minecraft overall readiness as **separate, unverified questions** even if 6.24's sided callback fires. Do not publish raw owner logs or any actor identities.

**Explicit safety boundaries:** no gameplay control change, no trainer edits, no Shadow promotion, no main merge, no private Headquarters CI, no Minecraft claim until actually retested.
