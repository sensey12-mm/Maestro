# Phase 6.4 — Offline coverage and robustness acceptance handoff

Branch: Maestro-6. Input: Phase 6.3 original pinned scenario Input, explicitly conditional Phase 4 actor profiles, turn-order assumption and critical flag. No source-authenticated native actor or trusted current battle state is claimed.

The audit reconstructs Phase 6.2 public frame provenance and Phase 6.3 witnessed-only conditional cases on every call. Denominators are computed for *the caller's synthetic fixture only*: declared own legal actions × observed current-appearance reply IDs. Source-listed own/reply counts, modeled conditional cases, conservatively resolved line outcomes and unresolved cases are reported separately. Unmodeled switches, unsupported attacks, absent replies and branch-to-reply ambiguity cannot be counted as resolved. A zero-case fixture is not vacuously complete.

When an action has one unambiguous Phase 4 result per witnessed reply and all line effects resolve, worst-low/best-high conditional HP-resource bounds can be shown. These are NOT expected value, win rate, best move, probability or genuine match robustness. Source conditional profile assumptions are not proof of installed mechanics. Complete fixture evaluation never closes the opponent's unknown possible response set.

Historical Phase 5.21 inputs remain separate appearance-local offline signals; no hypothesis or source-listed possibility becomes a witnessed move, current identity, legal response, or weight. Outputs are immutable; live-battle, policy, Shadow search and promotion flags always false. Legacy live chooser and main are untouched.

FrAIL independent review: audit OfflineShadowCoverageAudit and upstream Phase 6.2/6.3 provenance gates and run OfflineShadowCoverageAuditRegression through StandaloneRegression. Specifically review unsupported responses, empty fixtures, stale turn, fake public requests, same-name appearance reentry, forced/trapped switches, inability to map multi-branch outcomes to individual witnesses, version drift and historical Phase 5 identity. Synthetic source-only regression, not Minecraft acceptance.

Outstanding gates at start: Phase 6.2 run #40370499881 and Phase 6.3 #40374010406 had build, standalone and JAR passing, but the pinned source-oracle stage still running, so **not green final CI**. Headquarters CI usage from this agent: 0 minutes. The final Phase 6.4 exact-commit CI must be reported separately.
