# Phase 5.7 — Trusted Offline Evidence Provenance

Phase 5.7 introduces `PublicReplayProvenance`, an explicit local-input
reconstruction boundary layered on top of Phases 5.1 through 5.6.
It does not collect, fetch, retain or publish any replay.

## Trust model and receipts

- Public Phase 5 `record` constructors remain useful for immutable,
  self-contained projections, but arbitrary callers can construct records
  with misleading source-verification strings, versions, replay hashes,
  appearances, move counts or accuracy values. Their equality to another
  publicly constructed record is **NOT** authentication.
- `PublicReplayProvenance.verify` requires the **original operator-supplied
  spectator log**, replay source attestation, pinned target, and every
  submitted evidence layer. It re-runs public replay admission,
  source/version normalization and descriptive tendency extraction. Each
  recomputed record must equal its submitted counterpart, including the
  original replay-byte SHA-256. Any mismatch or invalid source fails closed
  with a fixed error that excludes user input.
- `ingest` offers an alternate no-submitted-records path, recomputing all
  three layers itself. Neither entrypoint exposes a raw input, player,
  nickname, public replay ID or input fingerprint from its receipt.
- `CheckedReplay` has a private constructor. Ordinary external code cannot
  instantiate it with arbitrary record fields; it can be issued only
  after full admission and consistency checks. Its only Shadow-facing view
  invokes Phase 5.6's diagnostic audit; it never authorizes tactical
  actions, runtime battle binding, or live promotion.
- `verifySplit` independently recomputes the **entire** held-out corpus
  from caller-supplied source/log/partition inputs and compares the
  deterministic Phase 5.4 report. It preserves anti-duplicate and
  replay-identity leakage gates. `CheckedSplit` also has a private
  constructor and exposes only replay counts and explicit restrictions.
- The older public `PublicCompetitiveShadowContract.audit` now ALWAYS
  marks its externally constructible record inputs as
  `EXTERNALLY_CONSTRUCTED_RECORDS_NOT_AUTHENTICATED`. Passing a schema
  consistency check remains useful but **does not upgrade provenance**.

## What is—and is not—verified

Positive results show **deterministic consistency with the specific
operator-supplied inputs**, using the currently pinned local source.
The SHA-256 is a change detector, not a digital signature or independent
origin certificate. Receipts are ordinary in-process Java object
boundaries, not cryptographic capabilities against code executing with
full privileges in the same JVM, reflection or unsafe instrumentation.

The tool CANNOT independently determine whether the replay was truly
published by Showdown, whether the operator has actual rights to use it,
whether a public replay was fabricated before intake, or whether the
replay corresponds to a Minecraft battle. Upstream public replay IDs
may be linkable to humans; minimized upstream records are not guaranteed
irreversibly anonymous.

No current battle actor/turn, own-request legal actions, runtime
Cobblemon registry/effect receipts, calibrated move probabilities, battle
win-rate claims or action promotion are issued. Legacy remains the
sole live controller; Shadow is observational. No real replays,
private Headquarters information, Minecraft battles, or runtime integration
are part of this synthetic-only batch.

## Adversarial regression coverage

Tests require deterministic acceptance for consistent admitted replay
and split inputs; reject modified SHA-256, replay ID, observed events,
turns, species, move set/count, appearance, normalized source/rights
claims, rules, version, switching counts, renamed/altered raw inputs,
malformed private payloads, changed held-out manifests, partition swaps,
duplicates and aggregate report tampering. Receipts must not expose
identifying strings and cannot be constructed through public constructors.

An independent integration reviewer must still inspect source and
permissions before any later development authorization. This batch
does not merge to main or start runtime integration.
