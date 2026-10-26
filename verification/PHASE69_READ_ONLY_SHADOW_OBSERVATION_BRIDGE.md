# Phase 6.9 — Opt-in read-only Shadow public-observation bridge

**Branch:** `Maestro-6` **Version:** `1.0.54` **Mode:** read-only observer; **no live tactical permissions**.

## Actual source and runtime integration

The existing `MaestroBattleAI.syncPublicProtocolObservation(PokemonBattle)` already reads `PokemonBattle.getShowdownMessages()` for Shadow diagnostics, as pinned in the Cobblemon 1.7.3 integration. Phase 6.9 adds `ReadOnlyShadowObservationBridge` at that **existing** read site and withdraws it during battle-memory reset. It does not install new request/turn mixins, inspect private request payloads, modify a legal action or enter the Legacy chooser. It is **opt-in** behind Java system property `-Dmaestroai.phase69Observe=true`, off by default. The caller's exact installed-version compatibility predicate is the existing `ownTransportVersionSupported()` (`1.7.3+1.21.1`); its value is a local compatibility premise, NOT an independently authenticated spectator delivery receipt.

The bridge binds to the actual battle **object identity**, not nicknames or a guessable string token, and enforces append-only raw-history chunk SHA-256s. It checks `PublicProtocolFilter` against independently reconciled `PublicEnvelopeProvenance`. Only original **direct update** packets consisting of a single supported `|switch|`, `|drag|`, `|turn|` or `|move|` event can be retained as **synthetic-frame eligible**. All private `sideupdate` data, split packets, unframed messages, unsupported events, incomplete packets, history rewrites, battle swaps, source pin failures, quota exhaustion and lifecycle withdrawal **abstain permanently** for the bridge instance. Private raw chunks are never retained; only per-chunk SHA-256 values are kept for prefix checks, alongside the narrow public eligible frames. Diagnostic output includes only status, monotonically counted revisions, turns and event counts; never battle IDs, nicknames, raw requests or hidden information.

**Important runtime limitation:** Real battles routinely contain additional public protocol events. The narrow Phase 6.2 historical fixture model cannot prove that such full event streams are complete public-state evidence, so this bridge abstains on them, rather than discard unrecognized mechanics and pretend current decision/legality authority. A compatible source version and successful capture do **not** prove native actor identities, all legal own moves, or opponent next-move legality.

## Phase 6.7 / 6.8 integration boundary

The public observation path **does not automatically call** Phase 6.7 or mint Phase 6.8 producer receipts. A separate explicit `probeSynthetic` test entry point accepts a caller's one-turn offline fixture only if the original received and sanitized frames match **exactly**, the bridge observation's turn and source battle object match, and a new captured revision exists. It then uses the existing Phase 6.8 producer registry and Phase 6.7 original-frame + source-conditional legal-action checker. Probe-issued tickets are single-use, sequence checked, and permanently withdrawn on failure. All returned Phase 6.7 authority flags remain **false**. Since synthetic fixtures can be caller-minted, this does not establish real runtime source authentication; the producer/capture boundary still needs later native receipt transport and Minecraft testing.

## Regression and verification requirements

`ReadOnlyShadowObservationBridgeRegression` covers: public-only positive capture; repeat idempotency; object-identity battle mismatch and isolation; an append-only turn continuation through 6.7–6.8 in a synthetic fixture; repeated/foreign synthetic probe rejection; raw-history edits or truncation; missing/unpinned source; private sideupdate exclusion; split private/public packet denial; unsupported mechanics events; forged same-slot actor and mismatch between original capture and supplied synthetic fixture; turn gaps; sticky terminal withdrawal; redacted logging; and false tactical/live permissions. All prior Phase 6.1–6.8 regressions remain registered.

**No Minecraft testing performed.** Source-level Java/standalone/JAR and full pinned source oracle must be verified on the exact final commit; until then CI success is NOT claimed. Final Phase 6 review by FrAIL is deferred until the end, not repeated every batch.

**Headquarters private CI:** zero runs/minutes triggered. No `main` merge, no other branch change, no tactical promotion.

## Next practical limitation

A separately authorized phase should introduce **explicit source-verified live own-action/legal-receipt transport**, preserve spectator private-data boundaries, and abstain on missing/partial native evidence. Do not make this Phase 6.9 synthetic bridge an implicit tactical certificate.
