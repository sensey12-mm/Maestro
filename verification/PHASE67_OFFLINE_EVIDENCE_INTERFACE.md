# Phase 6.7 — Source-pinned offline runtime-evidence interface (future boundary)

**Branch:** `Maestro-6`. **Version:** 1.0.52. **Intended consumer:** future independent, source-attested runtime observation adapter. **Current state:** offline synthetic interface ONLY. No runtime wiring, native legal-move certificate, Shadow tactic activation, live control or `main` merge.

## Contract

`OfflineRuntimeEvidenceContract` exposes a fixed schema, pinned Cobblemon 1.7.3/Showdown mechanics revision, an opaque 64-hex synthetic battle token, monotonically increasing per-session sequence, source-recomputed synthetic turn and appearance, fixed side slots, and a canonical SHA256 fingerprint over legal fixture moves/switches. It takes `OfflineShadowResponseScenarios.Input` containing the original Phase 6.2 spectator frames and original synthetic snapshot rather than accepting any caller-minted provenance receipts.

`Session.inspect` reconstructs Phase 6.2 frame integrity, Phase 6.3 source-conditional scenarios and Phase 4 root action legality. It enforces strict next sequence (no skipped, duplicate or reordered receipts), nondecreasing bounded turn/appearance, an append-only raw original-frame prefix and unchanged own legal-action fingerprint within the same synthetic turn+appearance. Any rejected attempt irreversibly poisons that session; an existing token cannot revive it. Wrong source pins, actor slots, schema, turn, appearance, action IDs, digest or fixture are rejected. Arbitrary trainer names, UUIDs, items, abilities and private opponent data never appear in the receipt or exception. The public review exposes only numeric synthetic counts and permanent explicit limitations; it has no mutable state or original private payload.

Critically **all "runtime attested," "current battle bound," "opponent response complete," "Shadow search authorized," "tactical decision authorized," and "live promotion authorized" accessors are permanently false.** A caller-selected token or digest does not establish external custody. A fresh Session after process restart also cannot prove that a previous source stream was replayed: authenticated producer custody, persisted counter continuity, runtime-side observation, specific Cobblemon API version pinning, and independent Minecraft acceptance are future work. No real runtime actor or legal action adapter is provided now.

The current offline prototype uses source-conditional witness fixtures only; historical Phase 5 guessed opponent builds cannot become observed replies or probabilities. It neither picks a move nor upgrades Legacy/Shadow control.

## Adversarial regression acceptance

`OfflineRuntimeEvidenceContractRegression` is wired to `StandaloneRegression`. It verifies deterministic original-frame rechecking and synthetic reporting; no live authority; immutable, redacted receipt; sequential valid continuation; duplicate sequence replay; missing counter; synthetic token mismatch; fixed source/schema drift; actor slot swap; malformed/private names; duplicate, fabricated or source-disagreed legal actions; canonical action hashing; actor spoofing with newly recomputed fixture digest; private opponent item leakage; append-only history forks; mismatched or missing turn/appearance evidence; valid synthetic turn advance and rejected stale turn rollback. Poisoned sessions cannot be revived.

Regressions are synthetic; they do not prove real Cobblemon runtime request ordering, installed mechanics, current opponent legality, trusted producer identity, native visibility or Minecraft compatibility. A complete source-oracle CI result is required before full exact-commit verification may be claimed.

## Review and phase boundary

FrAIL is the **final Phase 6 integration reviewer**, not a mandatory approver of each intermediate batch. Any final GO/NO-GO handoff must independently inspect this interface together with Phase 6.1–6.6 and Phase 4/5/7 compatibility. No Phase 6.7 changes authorize live tactical use.

**Remaining work:** verified public Actions closure for any still-running earlier exact commits, full Phase 6.7 exact-commit run, trusted runtime source-authentication/lease design, later Shadow observation-only integration with explicit user authorization, Minecraft testing for installed acceptance. Headquarters CI usage for this batch: **zero** (no Headquarters workflows triggered).
