# Phase 6.8 — Offline evidence producer trust and lease contracts

**Branch:** `Maestro-6`  **Version:** `1.0.53`  **Purpose:** capability/expiry semantics for future observation adapters, **not** installed-runtime attestation.

## Implementation

`OfflineEvidenceProducerLease` is a new independent source-pinned synthetic producer boundary layered over Phase 6.7's `OfflineRuntimeEvidenceContract` and its original Phase 6.2–6.3 public-event checks. A `Registry` accepts only the exact `CobblemonMechanics.SOURCE` string and explicit test-controlled logical clock and source-lifecycle suppliers. It may mint one in-process `Producer` capability per 64-character opaque synthetic battle token. Tokens cannot be reused in that registry, including after revocation. Producer tickets have **private constructors**, binding by object identity to their issuing producer and registry, fixed synthetic token and ordinal, and emission time.

`Registry.inspect` only accepts its own tickets, checks exact next consumption ordinal, finite lease lifetime and maximum receipt age, and delegates the event to Phase 6.7's existing sequence, appearance, source-pin, action-legality and append-only frame verification. It checks the clock and source lifecycle **again after** evaluation, preventing in-flight withdrawal from returning an accepted report. A known producer's bad receipt permanently revokes that producer. A foreign ticket fails without destroying another registry's valid producer. Source withdrawal, logical clock rollback or callback exception permanently shuts down that entire synthetic registry. Re-establishing the former boolean or clock value never revives it. Public strings never include the token or raw event, and no separate signed proof is fabricated.

The only accepted result is a Phase 6.7 offline Review with `runtimeActorBound=false`, `installedLegalActionsAttested=false`, `shadowSearchAuthorized=false`, `tacticalDecisionAuthorized=false` and `livePromotionAuthorized=false`. The wrapper cannot choose a move, authorize Shadow search, bypass Legacy live control, or introduce opponent probabilities. No trainer config, Shadow runtime path, `main` merge or other source branch is changed.

## Trust boundary / what remains unknown

This proves **synthetic in-process ownership and deterministic lifetime**, not origin authenticity or signatures. The registry is constructed with caller-provided predicates; untrusted callers can instantiate their own registries. A second registry/process can validate an independent newly minted synthetic transcript; no cross-process anti-replay, persistent counter ledger, secure storage or actual Cobblemon battle binding is claimed. A future trusted host component must supply source-authenticated immutable observation receipts and a verified monotonic lifecycle, and tests in Minecraft will be needed for installed acceptance. Source-pinned simulator mechanics do not authenticate a live game event.

## Adversarial tests

`OfflineEvidenceProducerLeaseRegression` is integrated into `StandaloneRegression`. Checks include: in-order receipts; consumed receipt replay; skipped/out-of-order issue and acceptance; cross-registry ticket rejection even with an equal-looking battle token and source; independent concurrent battle scopes and cross-battle token spoof; issuance and acceptance expiry; stale in-flight receipt; explicit producer withdrawal; source withdrawal and attempted restoration; clock rollback and restoration; source release during the post-evaluation check; schema/source mismatch; illegal synthetic identifier; spoofed actor in original public frames; irrevocable denial and privacy-safe diagnostics. Existing Phase 6.1–6.7 synthetic tests remain enabled.

**Acceptance status:** Requires exact-commit public GitHub Actions Java/standalone/JAR checks plus full pinned-source oracle. Do not represent pending oracle status as a pass or claim Minecraft testing. Final independent FrAIL review happens at the end of Phase 6, not as a gate for intermediate batches. Headquarters CI use: **0 minutes**; none triggered.
