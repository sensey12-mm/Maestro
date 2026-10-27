# Phase 6.22 — Runtime Observation Acceptance Harness

**Code:** public Maestro / Maestro-6. **Build:** 1.0.67. **Environment pin:** Minecraft 1.21.1, Java 21, Fabric, Cobblemon 1.7.3+1.21.1, RCT 0.15.2-beta.

## Objective and integration

RuntimeObservationAcceptanceHarness introduces a diagnostic-only, per-physical-battle in-process session. Default is OFF. Opt in through the Minecraft launch JVM argument **-Dmaestroai.phase622Observe=true**. This enables the prerequisite 6.21/6.18 read-only observation chain, but **never changes LivePromotionGate, Shadow mode, the selected Legacy action, packets or their recipients**.

It receives actual native sendSplitUpdate RETURN callbacks via the existing optional NativeSplitDispatchObservationMixin, original Showdown history at MaestroBattleAI.syncPublicProtocolObservation, the already-source-owned 6.21 readiness report for *coverage only*, and BattleEndPacket hook notifications. All are advisory diagnostics and cannot be tactical evidence by themselves.

**Provenance and limits:** a native RETURN callback does not prove Minecraft client delivery; an ordered matching public-history window does not prove packet identity; structurally public split halves do not establish public client receipt. Existing 6.21 paths remain mutually exclusive, and DIRECT_ONLY_OBSERVATIONAL and SPLIT_TIMING_UNVERIFIED are coverage labels, never combined into permission. Absence of native callbacks yields HOOK_UNOBSERVED, not artificial acceptance. Every report permanently denies source visibility certification, legal action completeness, event identity, transport delivery, Shadow authorization and live promotion; all report objects contain explicit minecraft=UNTESTED and delivery=UNVERIFIED.

**Privacy:** original message strings are read only in memory; no raw public/private chunks, names, UUIDs, network payloads, hidden opponent requests or actor objects enter reports or the phase 6.22 log. For append-only detection only, history chunks are hashed using per-session randomly salted SHA-256 digests; no salt or digest is printed, and buffers are cleared on withdrawal. Hashing is integrity bookkeeping, not cryptographic proof of original public visibility.

**Lifecycle:** reports distinguish NO_HISTORY_YET, HOOK_UNOBSERVED, NATIVE_RETURN_OBSERVED_UNVERIFIED, ORDERED_SOURCE_AND_HOOK_UNVERIFIED, BATTLE_END_HOOK_OBSERVED and deterministic fail-closed reasons. Wrong physical battle, unsupported version/size, rewritten/truncated source, regressed turn/history or forged/stale native witness irreversibly withdraw that session. Transport failure and the existing new-battle reset also withdraw the observer. Native battle-end now invalidates old 6.9/6.13/6.16/6.17/6.20/6.21 observer sessions as well as the new observer. Missing runtime injection never results in fabricated evidence.

## Synthetic acceptance matrix and public CI

RuntimeObservationAcceptanceHarnessRegression is registered in StandaloneRegression and exercises absent native hooks, valid original source-owned return, monotone/unchanged original history, append-only growth, same-length rewrite, truncation, turn regression, foreign battle/receipt, bad source version, oversized input, stale/duplicate native callback, untrusted 6.21 verdict and terminal end-hook withdrawal. Assertions require that all apparent successes still have **zero tactical/delivery/Minecraft permissions**. Existing 6.1–6.21 source, JAR and pinned Cobblemon ABI gates remain in Verify MaestroAI. A passing compile/ABI does **not** mean mixins applied in Minecraft.

## Exact Minecraft acceptance checklist — NOT PERFORMED

This checklist requires a **separately owner-authorized** in-game acceptance run. Do not substitute source tests for gameplay.

1. On the owner's singleplayer Minecraft installation, verify and record Java 21, Minecraft 1.21.1, Fabric, Cobblemon 1.7.3+1.21.1, RCT 0.15.2-beta, and MaestroAI **1.0.67**. Record the exact source commit and installed mod JAR SHA-256 before gameplay; check Fabric Mixin application diagnostics for the native dispatch and battle-end hooks.
2. Launch once with **-Dmaestroai.phase622Observe=true** in the actual launcher JVM arguments. Verify the new redacted "[MaestroAI] Phase 6.22" lines. Absence of native callback must remain HOOK_UNOBSERVED; seeing a log from source tests alone is not evidence of the optional Mixin actually firing.
3. During an owner-approved battle, capture timestamped redacted phase 6.22 verdicts for ordinary move choices, voluntary switches, trapped/rejected and forced choices, direct status/heal/switch events, and split damage. Cover Illusion/Transform or forme ambiguity if those cases are available in existing test plans. A source-compatible unsupported shape must abstain, not be silently accepted. Do not log raw opponent sideupdate/private split data.
4. When a native RETURN callback genuinely occurs, expect NATIVE_RETURN_OBSERVED_UNVERIFIED. A later original monotone history can yield ORDERED_SOURCE_AND_HOOK_UNVERIFIED; neither status certifies receipt, packet/event identity, all legal actions, tactical readiness or Shadow control. Confirm counters/turns do not regress. Verify ambiguous duplicate callbacks do not count as two trustworthy sends.
5. Exercise battle end, fresh battle, reload/reconnect where relevant, and request correction using legal in-game interactions; verify prior session withdraws, old source never resurrects, the end-hook is actually seen when available, and no observer failure changes Legacy's move choice or disrupts gameplay.
6. Run a comparable owner-authorized fight with diagnostics **off** to compare Legacy responses and normal request handling. Check filtered diagnostic logs for accidentally exposed nicknames, UUIDs, raw side requests, raw private split HP, unknown opponent moves/items, packet payloads and other private data.
7. Supply only privacy-sanitized runtime evidence through an authorized private Headquarters custodian. Independent FrAIL review is necessary before reporting any installed hook/runtime acceptance. **Client delivery needs a new trusted recipient acknowledgment and remains unverified even if all above passes.**

**Safety:** no main merge, trainer changes, hidden-data access, promotion, unsolicited battle requests or Headquarters CI. Work and verification are exclusively public Maestro-6, with Minecraft acceptance pending.
