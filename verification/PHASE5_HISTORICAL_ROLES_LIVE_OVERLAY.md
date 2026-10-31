# Phase 5 — Live historical-role integration (Shadow-only, v1 overlay)

**Branch:** `Maestro4567` · **Version:** 1.0.82 · **Mode:** disabled by default; synthetic Java acceptance only until exact-commit CI and installed Minecraft acceptance. **No Shadow promotion.**

## Intent and actual integration

This batch extends the source-backed Phase 5 online v2 public-capability observer with a **separate** `PublicHistoricalRoleLiveOverlay` that imports *only conditional historical strategic role signals* from Phase 5.21's original signed-replay pipeline. Historical roles are always **source-scoped and unbound**, and coexist diagnostically alongside separate current public witnessed move capabilities. A historical replay appearance is not a native Pokémon, an opponent side, or an installed moveset; a matching displayed name/species never joins one to another.

Implemented role labels are the pinned Phase 5.13 **source-supported** IDs `priority-move-signal`, `attack-boost-signal` and `special-attack-boost-signal`, converted to fixed `PRIORITY_MOVE_SIGNAL_POSSIBLE`, `ATTACK_BOOST_SIGNAL_POSSIBLE`, `SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE`. Unknown role IDs, unverified proposals, transformed/Illusion-ambiguous signals and other move/item/ability evidence are excluded. These are **NOT** opponent build predictions, legality certification, calibrated probabilities or claims about the currently visible Pokémon.

### Signed import and ongoing revocation

`PublicHistoricalRoleLiveOverlay.prepare` requires an explicitly supplied original public replay, proposed offline queries, consent-scoped observation constraints, override notices, a real signed Ed25519 manifest, independent operator-trusted keyring/policy and trusted current time. It re-runs the *existing* `PublicShadowBuildHandoff.prepare` (including original replay, Phase 5.19 signed custody, source/method/policy pins, rights ledger and historical-role derivation). Then it separately rechecks signed custody and source eligibility. Resulting private-constructor `Archive` keeps only role categories, an original replay-custody reference and verified digest/revision. It does **not** retain the raw replay or candidate builds.

A `TrustedSource.latest()` must be supplied by the operator **for each currentness check**, returning the latest signed manifest, fresh independently sourced keyring policy and clock. The overlay re-verifies Ed25519 and its original replay's custody eligibility, rejects a revoked or expired approval, keyring/policy regression, stale clocks, foreign signer, missing signed intermediate revision, manifest substitution or replay rollback. Successor revisions require the existing `verifySuccessor` chain; no revision-skipping is allowed. If source validation fails, the session withdraws irreversibly and earlier positive reports cease exposing role labels.

**Trust limitation:** The software validates a signature against the caller's independently provisioned keyring; it does not prove external ownership, rights or source freshness *by itself*. Supplying a synthetic keyring or an old clock is not a real-world independent authorization. A trusted operator must provision and maintain those inputs; this is an acceptance requirement, not implicit readiness.

### Production observation path

`MaestroBattleAI` now has a gated optional historical role diagnostic on the existing Phase 5 v2 callback:

- JVM property: `-Dmaestroai.phase5HistoricalRolesObserve=true` (also enables the public-only Phase 5 v2 observer). By default OFF.
- The operator/integrator must call `installPhase5HistoricalRoles(Archive, TrustedSource)` with a previously verified offline archive and a continuously refreshed trusted source. No archive/keyring/file is loaded by default, and without one only Phase 5 v2 public move capabilities can be observed. `clearPhase5HistoricalRoles()` revokes the installed archive and its active per-battle scope.
- Each actual source-owned native callback builds the current Phase 5 v2 diagnostic from original source history, opposite native side and Phase 6.29 public position uncertainty. Only a *current* and safe v2 report can join the historical overlay — as **two parallel evidence categories**, never a historical/current identity mapping.
- The live historical output contains **only categorical role names, current public witnessed capabilities, and fixed identity/authority denial flags**. Both previous reports and live source evidence dynamically expire. Missing, substituted, stale, unsupported or unsafe inputs abstain/fail closed.
- No Phase 5.21 build candidates, items, abilities, raw historical movesets, replay identifiers, raw logs, names, UUIDs, hidden Pokémon state, opponent action probabilities, complete installed legal set, live tactical chooser input or promotion are exposed.

### Adversarial regression coverage

`PublicHistoricalRoleLiveOverlayRegression` validates real synthetic Ed25519-signed Phase 5.21 source role import using the original Phase 5 regression fixture, publicly witnessed independent live Phase 6.24/6.29/6.31/v2 native/history receipts, readiness and abstention, historical/current separation, forged keyring, expired source approval, clock reversion, replayed live epochs, foreign physical battles, native/public history advance, after-end withdrawal, default-unprovisioned state, private data redaction and unmodified Legacy chooser. Existing Phase 5 v1/v2 and Phase 6.1–6.35 regressions continue running in the full standalone suite. CI must pass Java/Gradle, JAR, native ABI and pinned simulator oracle **on the exact final SHA** before acceptance.

## Installed Minecraft test / acceptance

Install the exact passing v1.0.82 JAR into the owner's unchanged Minecraft 1.21.1 / Cobblemon 1.7.3 / Fabric / RCT 0.15.2-beta / Java 21 environment. Use only a separately approved historical public replay and provision fresh signer policy, replay rights ledger and trusted clock through an operator integration before enabling the feature. Check the opt-in native callback actually runs; confirm a public Stealth Rock/Swords Dance use produces only witnessed live side capability, while historical source-supported role categories are listed **unbound**, not assigned to Glimmora, Kingambit, Zoroark or other current Pokémon. Check expiry/revocation, mismatched signer, missing input, replayed callback, battle-end and missing native report all revoke and abstain. For Illusion/Transform/reentry use the existing Team 4 with Unovan Zoroark disguised as Greninja (Greninja last eligible); confirm no history-to-Pokémon match. Compare diagnostics-off Legacy behavior; **Minecraft testing is outstanding and must not be assumed passed**.

**Scope verdict:** Phase 5's *role-level conditional historical possibilities* now have an integrated, expressly provisioned Shadow-only live diagnostic path, but **full historical build candidates and opponent response probabilities are not online** and cannot be safely inferred from current evidence. No Shadow independent live decision ranking or promotion has been authorized. No `main` merge, Headquarters private CI or trainer roster edit.
