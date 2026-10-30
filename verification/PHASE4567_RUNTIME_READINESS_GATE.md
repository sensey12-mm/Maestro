# Maestro4567 — Independent Runtime-Readiness Source Gate

**Scope:** source-level, synthetic-only acceptance. The full Stage 2 exact-commit
CI for `94dcde46bb1903739972641ff06094e7d486fe34` passed
([Actions run 41104581324](https://github.com/sensey12-mm/Maestro/actions/runs/41104581324)).
This file does **not** claim installed Minecraft compatibility.

## Independent checks

- `IntegrationRuntimeReadinessGate` consumes the original in-process
  `NativeSidedDispatchReachability.Report` and
  `PublicSplitRoutingGate.Report` for one physical battle. The native
  source actor must have been an original member of the native battle
  actor set; this **does not** authenticate the public Showdown actor.
- A positive result requires source-pinned, current reports, the exact same
  battle object, equal turn and history-chunk epoch, a real native sided
  RETURN observation, and structurally valid private-first/public-second
  Showdown split frames. Both source revisions/ordinals must advance.
- Missing callbacks or split frames **abstain**. Foreign battle, invalid
  actor, stale/rewritten source, turn/chunk disagreement, replay or duplicate
  **fail closed**. Native source progression, public history revision and
  battle end revoke old results dynamically.
- Source-derived diagnostics contain only numeric metadata and bounded
  verdicts. No private packet, raw history, UUID, nickname, request payload,
  recipient identity, or actor object is exported.
- Adversarial synthetic regressions exercise absence, wrong battle, stale
  epoch, replay, source mismatch, invalid actor, malformed routing,
  lifecycle withdrawal, redaction, and explicit Legacy chooser isolation.

## Strict NO-GO boundaries

A `sendSidedUpdate` RETURN is **not** `sendSplitUpdate`, a network
acknowledgement, per-recipient delivery, or evidence that the callback is
installed in Minecraft. Equal source windows do **not** link the native
sided event to a Showdown split frame or prove event/actor identity.
Source-history structural public/private parsing is not client delivery.
The gate always denies Phase 5 online use, Shadow tactical search, live
promotion, and installed mechanics/legal-action readiness. The live
`MaestroBattleAI` chooser remains Legacy-only; this gate is not wired
into the chooser. Phase 5 remains offline-only, Shadow non-authoritative.

**Further runtime acceptance:** separately authorized, privacy-safe
Minecraft native hook and lifecycle evidence, authenticated actor/turn
source custody, recipient routing/delivery evidence, installed legal-action
coverage and independent real-evidence Phase 7 evaluation are still needed.
No `main` merge, Shadow promotion or private Headquarters CI.
