# Phase 6.17 — Native Split Recipient-Custody and Delivery Safety Contracts

**Repository:** `sensey12-mm/Maestro` · **Branch:** `Maestro-6` · **Version:** `1.0.62` · **Mode:** optional read-only audit; no tactical authority.

## Source-level dispatch and recipient finding

The installed Cobblemon version is pinned to **1.7.3** (upstream release tag `e6fda6137e00cfe6035c863a3736ba30fc236696`). Phase 6.16 and the upstream `ShowdownInterpreter.kt` source show that `updateInstructionParser["split"]` consumes `privateMessage = messages.next()` followed by `publicMessage = messages.next()`, resolves `targetActor = battle.getActor(splitTarget)`, selects the public message's type and dispatches an interpreter instruction for a supported `-damage`, `-heal`, `-sethp`, `switch` or `drag`. Separate native instructions may modify battle models or dispatch client-visible effects. This is **not an event-delivery acknowledgment** for each viewer and is not evidence that a particular player received the private or public string.

The existing opt-in observation source is `PokemonBattle.getShowdownMessages()`. That battle history contains original split framing, **not** verified per-recipient socket transmissions, observers, recipient epochs, network acknowledgments, or trusted client-side receipts. Thus no available in-repository hook can truthfully assert *genuine native recipient delivery*. This batch **does not invent an authenticity predicate** or call synthetic receipt claims native evidence.

The published source pin and referenced protocol behavior must still be validated in Minecraft with the installed modpack/runtime before anyone claims that a particular live recipient actually received a message.

## Implemented read-only contract

`NativeSplitRecipientCustody` is separate from Phase 6.16's `PublicSplitRoutingGate`. Phase 6.16 is extended with a **package-private, non-exported** ordered `p1/p2` split-target manifest and `sameBattle(Object)` binding on the original structural `Report`. The public report exposes only counts, revision, status and source version. No private or public message bytes are saved in Phase 6.17; no actor names, UUIDs or secret source text appear in public diagnostics.

The production `NativeSplitRecipientCustody.Session.inspect` requires an unexpired original Phase 6.16 report and the exact battle object. For split events it returns **`DELIVERY_UNOBSERVED`**, even after fully valid structural parsing; a direct-only input reports `NO_SPLIT_SOURCE`. Cross-battle, withdrawn, missing, truncated, replaced, or stale structural evidence fails closed. Session withdrawal is permanent, old reports dynamically expire as the structural revision changes, and repeated observations of the same valid source are idempotent. Crucially, **the production path has no API that asserts delivery-authenticated=true**.

An isolated **package-private test-only** `inspectSynthetic` checks a bounded list of paired synthetic claims against each ordered split target, source revision, exact branch order (private first, public second), audience (`TARGET_P1`/`TARGET_P2` then `PUBLIC_SPECTATOR`), increasing per-source sequence, opaque distinct identifiers, and short logical-clock freshness. Synthetic claims cannot be converted into runtime delivery certifications. Even a perfectly matching synthetic receipt results in `SYNTHETIC_RECIPIENT_ORDER_MATCHED`, with **`nativeRecipientDeliveryAuthenticated=false`**, **`nativeObserverDeliveryAuthenticated=false`**, **`publicReadinessLeaseAuthorized=false`** and all other tactical/live authority flags false. No original recipient token or raw message can be recovered from a public report.

The only new runtime opt-in is `-Dmaestroai.phase617Audit=true` (off by default), which reuses the existing Phase 6.16 original-history read site. It enables Phase 6.16 structural parsing, then logs a redacted 6.17 `DELIVERY_UNOBSERVED` report. Phase 6.9, Phase 6.13, Legacy and any Shadow tactical consumer remain **entirely disconnected** from these split recipient diagnostics. No new Cobblemon mixin or private opponent read is added.

## Adversarial regression criteria

`NativeSplitRecipientCustodyRegression` is registered in `StandaloneRegression` alongside all earlier suites. Cases: real-only structural report returns unverified, duplicate polling, synthetic private-first/public-second pair, independent p1/p2 recipient targets, wrong side, public-as-private confusion, wrong branch and sequence, missing half, reordered or cross-pair claims, duplicate opaque tags, forged private text masquerading as a tag, wrong source revision, future/stale synthetic tick, foreign battle, rewritten **private** branch of original structural history, replayed synthetic receipts, withdrawn sessions, structural revision advance, prior report expiry, and direct-only no-split abstention. All reports must redact source payloads, and all delivery/Shadow/live permissions must remain false regardless of verdict.

## What still needs to happen

Real recipient-delivery verification would require a separately authorized **trusted installed native dispatch/recipient acknowledgment adapter**, with explicit battle identity, recipient instance, branch assignment, immutable message digest/epoch, and expiry, verified in Minecraft. Until such evidence is available, the correct result is **delivery unobserved**, not a fake cryptographic proof or a confidence score. Even that future adapter must remain read-only and not influence Legacy without explicit permission.

**Public CI requirement:** verify Java compilation, standalone regressions, JAR safeguards and full pinned-source oracle on the **exact final commit**. Pending oracle = full acceptance pending. **Minecraft testing is required** to claim installed delivery. **Headquarters private CI: zero runs/minutes**. No `main` merge, no Shadow promotion, no trainer configuration change. Final FrAIL review remains at end of Phase 6.
