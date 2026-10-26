# Phase 5.20 — End-to-end competitive intelligence handoff and acceptance

**Assessment scope:** independent, deterministic offline **Phase 5.1–5.19** pipeline on `Maestro-5`. This is an internal audit and a **handoff for independent review**, not integrated Phase 4/5/6/7 acceptance, an installed-Cobbleverse test, a public real-replay benchmark, or permission to change live tactics.

## End-to-end evidence chain

`PublicCompetitiveHandoffAudit.evaluate` checks one selected spectator replay against a **whole-replay held-out split**, with original bytes, a separately trusted Ed25519 public-key policy and a currently eligible signed-custody decision. The selected replay must occur **exactly once** in **HELD_OUT**, with identical source descriptor and log. Its source ID, log bytes or outcome cannot be silently changed, transferred to a different participant, or reused as TRAIN. All split records are re-ingested and checked for renamed/near-duplicate evidence under prior Phase 5 gates.

The audit invokes the existing **current signed-custody checks**, then independently recomputes:

| Stage | Built on | Acceptance evidence |
| --- | --- | --- |
| Signed replay provenance and normalization | 5.1–5.3, 5.5–5.9 | Public-only replay admission; source/target-scoped normalization and chronological opponent observations; current signed custody; existing observational Shadow contract still denies tactical/live use |
| Strict split and held-out baselines | 5.4–5.9 | Whole-replay split, duplicate guards, recomputed baselines, present train and test partitions and current signed-custody receipts for every input |
| Stratified coverage | 5.11 | Fixed per-format, move/switch, uncertainty strata; unsupported/underpowered strata recorded, not silently removed |
| Frozen distribution-shift hypotheses | 5.12 | Existing version-pinned protocol SHA-256; 20 strata, 30 scenario checks and split digest consistency; caller's registration is a structural pin, **not independently proven pre-registration** |
| Source-bounded build consistency and hypotheses | 5.13–5.16 | Recomputed public observations, a pinned subset of Cobblemon 1.7.3 species/form move and ability source listings, form inheritance and eligibility/abstention counts |
| Candidate classifications and counterfactual stability | 5.17–5.18 | Appearance-local partial assertions, source-only verdicts, nine non-elimination loss-of-information scenarios per candidate |
| Consumer safety contract | 5.19 | Fixed source/robustness schema, verified ordinal ↔ appearance matching, immutable privacy-minimized counts, no tactical/live integration or legal action certification |
| Final handoff | 5.20 | Fourteen explicit gate statuses and fixed cross-phase integration blockers; **offline recomputation alone is NOT integration readiness** |

Phase 5.10's durable local custody *implementation* remains tested by its own regressions, but this handoff does **not** receive an externally anchored filesystem checkpoint or independent bootstrap trust-root attestation. Consequently **DURABLE_TRUST_HANDOFF = NOT_ATTESTED** even when the caller-signed manifest is valid. An independent reviewer must verify durable checkpoint continuity before any stronger custody claim.

All input-source/version mismatches, missing/wrong-side appearances, invalid candidate input, replay clones, tampering, expired/revoked or incorrect signed custody, incompatible frozen policy or source pins, and train/test overlap **reject execution**. A nonnegative test outcome may only mark `OFFLINE_RECOMPUTED`; it cannot change a cross-phase gate.

## Actual inspected integration snapshots (2026-10-20)

The following commit IDs are **inspection checkpoints**, not cryptographic claims that a future branch or JAR has the same code. They are pinned into the offline handoff request; any change requires new inspection/reconciliation. The Phase 5 class DOES NOT import or run the other branches.

| Interface | Inspected branch commit | Concrete observation / blocker |
| --- | --- | --- |
| Phase 4: public mechanics / effects | `Maestro-4` `6a1b101e575361cfa844f982be86f7370a5a433a` | `PublicMechanicsEvidence` exposes source-bounded `Known<T>`/entry bindings; `ShadowMechanicsEvaluation` returns conditional utility ranges with unsupported coverage. Phase 5 provides neither a verified installed move/item/form registry nor a scoped Phase 4 current-action evidence receipt. **Runtime mechanics remain unverified**. |
| Phase 6: tactical search | `Maestro-6` `520d436110ab5c3eeced99a16148b03ab50fe034` | Baseline `ShadowSearchEngine.Outcome` requires caller-supplied response **probability**, immediate value and linked position. Phase 5's candidate labels/uncertainties are not probabilities or legal action receipts and MUST NOT be fed into search as if calibrated. Branch is a starting baseline, not approved active integration. |
| Phase 7: simulation and acceptance | `Maestro-7` `e26be18526385d7177eb51dcd80b2efa276b472e` | `Phase7Readiness` schema `phase7.12.v1` explicitly marks `integrated=false` and requires independent mechanics, provenance, artifact and transition evidence; `Phase7MechanicsAdapter` uses separate, externally pinned mechanics policies. There is **no shared, independently verified integrated Phase 4–6 pipeline** to validate yet. |

An inspection of three branch commits does not establish compatible current compiled modules, equivalent actor/turn fingerprints, interchangeable provenance, verified legal actions, or source/installed-game parity. Only the designated independent reviewer and owner can authorize subsequent integration scope.

## Readiness gates and blocked prerequisites

**OFFLINE_RECOMPUTED** (eight stages): signed replay, pinned normalization, signed whole-replay split, held-out baselines, stratified coverage, frozen distribution-shift checks, build candidate/robustness analysis, and offline consumer contract. **This means execution and integrity checks passed on submitted evidence. It does not mean every held-out stratum is sufficiently powered or performance improves.** Inadequate stratum counts are retained in the report.

**NOT_ATTESTED** (two stages): externally pinned durable custody / independently rights-verified real replay.

**CROSS_PHASE_BLOCKED** (four stages): Phase 4 mechanics, Phase 6 search, Phase 7 simulator integration, installed gameplay.

Fixed blockers include: independent replay rights; meaningful permitted public real-replay held-out evaluation and stability; calibrated opponent response predictions; durable trust checkpoint from independent policy; installed registry/form/move/item compatibility; Phase 4 execution evidence; Phase 6's need for legal actions and externally justified response probabilities; Phase 7's not-yet-integrated chain; live turn/actor freshness and Legacy/Shadow boundary; independent review and owner merge authorization.

**Handoff decision:** **INDEPENDENT_PHASE5_DEVELOPMENT_READY_FOR_REVIEW** when the offline chain recomputes, **NOT READY FOR CROSS-PHASE INTEGRATION OR MAIN MERGE**. The report deliberately retains all specified blockers. No real replay or production data is consumed by the synthetic acceptance tests. A true real-replay confidence/skill result requires independently authorized data and a separate scoped approval.

## Privacy, tests and preservation

The `Report` returns only fixed statuses, coverage counts, a source scope and frozen protocol digest. It contains no raw logs, source replay IDs, input hashes, player names, candidate move/item/ability values, key material or signing signatures. Small aggregate counts can still be distinguishing and therefore are *privacy-minimized*, not guaranteed anonymous. Reports have private constructors; a caller cannot construct an accepted receipt and pass it back for promotion.

Regression fixtures use only synthetic spectator transcripts and ephemeral test signing keys. Adversarial cases cover shuffled splits, copied or wrong-partition selected replay, duplicate public sequences, tampered replay bytes, injected private protocol packets, expired signatures, wrong actor indices, missing candidate evidence, swapped version/policy/frozen protocol identities, incompatible hypothetical form eligibility, and immutable reports. The exact-commit CI is the acceptance record for this implementation **only**.

This work changes no installed runtime code path, does not acquire real replays, and neither exports private coordination material nor merges into `main`. **Legacy remains authoritative and Shadow remains observational only.** Phase 5.20 documents blockers for later owner-authorized independent cross-phase review; no automatic next phase, tactical activation, or release is implied.
