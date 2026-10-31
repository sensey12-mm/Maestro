# Phase 5 Online — Final Source-Level Implementation Closure (v1.0.96)

**Branch:** `Maestro4567` · **Mode:** diagnostic-only, opt-in; Legacy remains authoritative. **No Shadow promotion, no main merge.**

## What is complete in source

- The real `MaestroBattleAI.ownNativeSidedDispatchReturned` hook consumes **source-owned** Cobblemon 1.7.3 / Phase 6.24 native sided-return receipts. It validates single owning AI actor/side (Phase 6.31), salted original public/private split routing and full-history custody (6.16), and appearance uncertainty (6.29) before invoking `PublicCompetitiveLiveShadowConsumer`. Unknown/ambiguous actors now revoke **both** Phase 5 scopes immediately, rather than silently retaining a historical lease.
- `PublicCompetitiveLiveShadowConsumer` v2 categorizes only already witnessed, public, source-modeled opponent move effects. It does not infer hidden moves, items, abilities, builds or probabilities and never supplies tactical action choices. Fresh native callbacks **with unchanged public history** emit an explicitly negative, source-attested `NO_NEW_PUBLIC_EVIDENCE` receipt. The old positive report is invalidated by the new native ordinal, without making a later genuinely new public observation impossible.
- `PublicHistoricalRoleOperatorActivation` loads only a separately commissioned signed historical replay/archive when explicitly enabled. `PublicHistoricalRoleLiveOverlay` extracts only supported, historical **unbound source-scoped** role possibilities from the independently checked Phase 5.21 signed handoff. It does not associate historical replay appearances with the current battler or run private-build inference.
- Historical positive receipts are checked dynamically against signed Ed25519 custody, original source bytes, independently pinned keyring revision/checkpoint, rights validity, non-regressed trusted time and live native evidence. Old reports withdraw across native lease loss, stale source, signer revocation, replay alteration, Illusion/Transform ambiguity, foreign battle, and battle end. Rotation and stale receipts cannot resurrect positive reports.
- The fresh **no-new** negative receipt can conservatively pause historical visibility for multiple consecutive callbacks. A later independently verified public event can resume the **same physical battle's** observations without resurrecting any old positive report. At every pause historical roles are empty, not guessed.
- Historical role diagnostics use **one** lease/currentness check per printed status, avoiding contradictory `current=true` plus revoked/empty role summaries.
- `LivePromotionGate.choose(..., null, null, false)` remains the only choice gate; Phase 5 reports have no chooser API or authority. Initial installation, subsequent callbacks and battle-end withdrawal are diagnostic-only.

## Deterministic regressions

- Live native 6.24/6.31 callback progression: first positive observed move, multiple source-attested no-new public callbacks, then a new public move and terminal withdrawal.
- Historical role continuity: never leak a prior positive during a pause, resume only from new native/public evidence and retain old report invalidation.
- Changing signer, keyring epoch, signed ledger, original replay bytes, clock and public source revokes historical roles. Cross-battle, Illusion, Transform and form uncertainty refuse identity joins.
- Re-entrant source-level guard confirms ambiguous native own actors withdraw both Phase 5 scopes; synthetic trusted source revocation during diagnostic construction keeps a single consistent categorical receipt.
- Existing independent standalone, packaging, native Mixin ABI, source-pinned simulator and Phase 5 operator regressions remain in CI.

## Completion and limits

**Source implementation status: COMPLETE for the agreed opt-in Phase 5 Online Shadow *diagnostic* scope.** This statement means source-level integration and regressions have been implemented. It is **not** a claim of passing exact-commit v1.0.96 CI or of installed Minecraft behavior.

The implementation **does not** turn historical offline opponent builds into live, opponent-specific predicted builds. That would violate the current uncertain-identity/source-custody contracts and would require a separately authorized design—not an unfinished component of this diagnostic scope.

**Externally commissioned archive:** No genuine externally approved spectator replay/signature/key is fabricated in source. Historical-role activation requires real, independently authenticated rights/signatures and checkpoints. The live public observer runs without such an archive when `maestroai.phase5LiveShadowObserve=true`. When using `maestroai.phase5HistoricalRolesObserve=true`, historical roles fail closed until independent operator provisioning is valid.

**Verification still separate:** Exact-commit public GitHub Actions and the SHA-pinned JAR must complete for the final source head; Minecraft testing is an ensuing installed-runtime acceptance activity, not an implementation prerequisite. No Minecraft result is claimed. No private Headquarters CI is triggered.
