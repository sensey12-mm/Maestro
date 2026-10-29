# Phase 6.23 — Runtime Evidence Acceptance Review (source-only repair)

**Branch:** Maestro-6 · **Version:** 1.0.68 · **Status:** native split-dispatch acceptance still NO-GO.

## Evidence custody and limits

The owner supplied separate local diagnostics-enabled and diagnostics-disabled Minecraft logs. They were inspected **locally in the conversation**. This repository contains **no logs, timestamps, usernames, battle identifiers, private Pokémon details, request payloads, packet payloads, source hashes from the user's installation, or reproductions of private log lines**. The owner retains original private evidence; only synthetic hand-authored fixtures are committed here. A successful synthetic regression or public CI does not attest the optional Mixin's runtime activation.

### Findings and interpretation

- The installed build and server-side end-of-battle callback were witnessed in runtime logs. This is **narrow proof** of the battle-end hook path executing; it is not a delivery acknowledgment or evidence that the separate split-dispatch RETURN hook executed.
- The native split-dispatch RETURN callback was **not observed** in the provided runtime traces. This may mean no qualifying method calls happened, optional callback was not installed, or producer events followed another path. **No cause is attested** by an absent callback. Keep hook activation / network delivery / event identity as UNKNOWN.
- The strict Phase 6.16 `PublicSplitRoutingGate` can reject otherwise-public battle-opening events that Phase 6.6's independent `PublicProtocolFilter` already accepts. In particular, the existing source allowlist omitted public `-ability` and `-boost` lines after battle-opening split switches. Missing source attestation then causes Phase 6.20 and Phase 6.21 to fail closed for the entire battle, as designed.
- The Phase 6.22 history checker recorded append-only history independently of split source authentication. This does **not** establish a live public/recipient-delivery lease, canonical event identity, accepted legal moves, a tactical plan, or Shadow authority.
- The diagnostics-disabled comparison shows the Phase 6.22 output path is absent, as expected; it does **not** prove the same Legacy actions would be selected in two different stochastic battles.

## Source repair and regression scope

- **Two direct public types only**, `-ability` and `-boost`, are added to Phase 6.16's original-history grammar. This is a deliberately narrow subset of the existing independent public filter. The gate now validates actor-form, ability name syntax, boost stat/level and annotations for these types; any private/unknown/unrecognized data still causes terminal failure. No broader hidden sources are ingested.
- An adversarial synthetic opening includes two public/private split switches, public ability, public boost and turn 1; it checks correct original-event order, strict independent filter reconciliation, append-only lease expiration, battle mismatches and **permanent denial of gameplay/recipient authority**. Synthetic malformed ability payloads, illegal boost levels, private annotations, private `sideupdate` and unreviewed direct kinds are rejected.
- No modifications to NativeSplitDispatchObservationMixin, Legacy action selection, trainer JSON, Shadow tactical authority or Phase 6.17 delivery contract. Observational decisions remain separate: split structural validation does not convert a missing native hook into evidence.

## Follow-on Minecraft acceptance

Phase 6.23 still needs fresh owner-authorized runtime evidence from an installed exact commit of 1.0.68 to verify that Phase 6.16 now progresses past opening frames. Look for any later unreviewed event type; do not extend allowlists without independent public-source validation. Phase 6.18's native split-send hook remains independently unverified until an actual source-owned callback occurs. If none occurs, record UNKNOWN rather than PASS; do not ask for repeated battles without a targeted reason. The source version and installed mod JAR SHA-256 must be recorded privately for attribution.

**Boundaries:** all original logs stay private; no Headquarters CI triggered; no claims of recipient delivery, Minecraft acceptance, Shadow recommendation readiness or live promotion.
