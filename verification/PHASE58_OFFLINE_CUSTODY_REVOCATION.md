# Phase 5.8 — Offline source custody and revocation gates

This is an **offline local decision-ledger** for permitted public Showdown
replay evidence. It is neither an external rights service nor an online
synchronization mechanism. No private custody records, players, histories,
logs, credentials or reviewer notes are shipped with the code.

## Custody rules and fail-closed behavior

`PublicReplayCustody.Ledger` is an immutable version-1 snapshot. Its
revision is exactly its number of decisions. Decisions must be
chronologically increasing and are keyed by **exact public replay ID,
format and original replay-byte SHA-256**. They carry one of:
PENDING, APPROVED, REVOKED; an event timestamp; a bounded approval expiry
(maximum 366 days); and a syntactically validated opaque review digest.

- **PENDING** cannot authorize ingestion, even when the replay content
  itself passes Phase 5.1. A subsequent explicit APPROVED event can allow
  it only before revocation.
- **APPROVED** is eligible only if the exact digest, format and replay ID
  agree with newly admitted evidence and the explicit evaluation timestamp
  is strictly before expiry. Expiry is exclusive.
- **REVOKED** is final for the same evidence within a ledger history.
  Later approvals for the same replay ID are rejected. Older approval
  events do not override the tombstone.
- Missing entries, changed digest, future-dated ledger events, unsupported
  schema, duplicate or reordered decisions, excessive lifetime, invalid
  review digests, impossible status transitions, and insufficient ledger
  revision all fail closed with constant errors.
- The **trusted caller** must provide current time and a **trusted
  minimum revision**. This is a required external anti-rollback input;
  a caller supplying an old ledger and an old revision floor can
  make old approval appear current. The offline module cannot
  independently detect an omitted later revocation, trust a clock,
  authenticate a reviewer's signature or verify source-use rights.

## Phase 5.7 integration

`PublicReplayProvenance.ingestWithCustody` and
`verifyWithCustody` require current local custody and recomputation.
`verifySplitWithCustody` applies the same requirements to **every
train AND test replay**, not merely to aggregate report counts.
They preserve the earlier deterministic ingestion/normalization,
identity ambiguity, replay deduplication, synthetic benchmark and
Shadow-readiness guards.

Ordinary Phase 5.7 `ingest`, `verify` and `verifySplit` are
intentionally retained for backward-compatible **content consistency
only**; they do not issue custody eligibility. A gated receipt also
does not possess permanent approval: callers must invoke
`custodyCurrentlyEligible(currentLedger,currentTime,minRevision)`
before relying on its *offline* custody status. Revoked or expired
receipts return false. These sealed in-process receipts do not expose
the replay ID, digest, player nickname, raw contents or decision text.

A syntactically well-formed review digest is **not** a cryptographic
signature. The ledger is operator-supplied, can be fabricated by an
untrusted operator and is not proof of independent replay permissions.
Version/revision checks establish deterministic local consistency only.
External review and private custody authority remain separate and
must be provided independently before operational deployment.

## Verification and forbidden promotions

Synthetic regressions exercise expiry boundaries, terminal
revocation, pending approval transitions, stale revision floors,
missing/malformed custody, digest mismatches, altered logs, delayed
revocations, immutable records, train/test completeness, duplicate
replays, report tampering and no live Shadow actions.

No real public replay was downloaded, no private evidence was copied,
and no installed Minecraft behavior was exercised. Source-pinned
Cobblemon compatibility remains scoped and unproven at runtime.
Legacy controls all live actions; Shadow remains observational.
This batch does not modify live selection, merge main or begin
runtime integration.
