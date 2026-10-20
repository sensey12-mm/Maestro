# Phase 5.9 — Signed offline custody and anti-rollback verification

This batch adds `PublicSignedCustody`: a **public-key-only Ed25519
verifier** for Phase 5.8's operator-maintained offline custody ledger.
Private signing keys are not bundled, required at runtime or stored in
the repository. Only synthetic ephemeral keys exist in regression tests.

## Signature and canonical encoding

Signed manifest v1 covers the full *ordered* Phase 5.8 decision history,
including replay IDs, exact content hashes, format, status, decision
timestamps, approval expiry and review digests. It also binds the
manifest version, signer key ID, signing timestamp, previous manifest
digest and ledger schema/revision. The canonical signature preimage is:

- fixed ASCII domain `MAESTROAI-PHASE5-OFFLINE-CUSTODY-V1\0`;
- network-order 32-bit integers and 64-bit long values;
- length-prefixed UTF-8 strings with strict bounded fields;
- all custody decisions in their validated chronological order.

Ed25519 signatures are encoded as canonical Base64 of 64 bytes.
Public keys are supplied as canonical Base64 X.509/SPKI Ed25519.
The manifest's SHA-256 identifies those canonical bytes; it is not
itself a signature or an independent authenticity claim. Unsupported
versions, alternate algorithms, wrong key IDs, noncanonical encodings,
invalid keys/signatures and modified fields are rejected.

## Explicit trust, key revocation and rollback

`TrustedPolicy` contains the explicit allowed public-key set,
keyring epoch and independently pinned minimum epoch, minimum ledger
revision, earliest acceptable signature timestamp and optional exact
manifest-digest checkpoint. The caller **must obtain this policy from a
trusted channel**. An untrusted caller who supplies their own keys,
a stale policy, or an artificial clock can defeat the intended trust
boundary; this class does not silently solve that problem.

The signed manifest is valid only when its trusted signer was active at
signing time and has **not been revoked as of verification time**.
Revoked keys cannot validate even previously signed evidence on or
after their revocation instant. Key rotation is supported through a
new trusted policy containing the replacement key; it is never learned
from an untrusted replay or manifest.

`verifySuccessor` enforces a direct, revision-by-revision transition:
the next revision is exactly previous+1, its signed predecessor digest
matches the previous verified manifest, and all previous decisions are
an immutable prefix. Intermediate transitions must be presented and
verified separately. A caller-pinned revision/checkpoint prevents
accepting older or alternate manifests **only if** that floor/checkpoint
is independently trusted and kept current. Signatures alone cannot
reveal an omitted later revocation.

## Phase 5.7 and 5.8 integration

`PublicReplayProvenance.ingestWithSignedCustody`,
`verifyWithSignedCustody` and `verifySplitWithSignedCustody`
require a verified signed manifest, a trusted key policy and explicit
evaluation time **as well as** the existing complete replay
recomputation and Phase 5.8 expiry/revocation/format/hash checks.
All train and test replays require individual eligible entries.

Only receipts created through the signed entrypoints return true from
`signedCustodyCurrentlyEligible`. Every recheck verifies the new
manifest signature, current trusted policy and current source custody.
Prior content-only Phase 5.7 receipts and unsigned Phase 5.8 receipts
cannot be upgraded by this method. No signed receipt gives a tactical
recommendation, legal action, current battle binding or live control.

## Limits and regression verification

- The signed ledger records REVIEWER-ATTESTED statements. Ed25519 proves
  that the ledger was signed by the holder of a configured private key;
  it does **not** independently establish replay ownership, legal usage
  rights, author identity, or the integrity of the original public
  Showdown listing. Private review of approvals still matters.
- A revoked *signing key* is treated differently from a revoked *replay
  approval*: both are rejected by distinct gates. Neither reappears
  merely because an old local receipt exists.
- No private headquarters records, signing keys, real replay logs,
  secrets, personal information or real battle observations were added.
  All regression logs and key pairs are synthetic.
- The signed custody validation is offline only. No runtime integration
  or Minecraft battle testing is required for Phase 5.9. Legacy remains
  the sole live controller; Shadow remains observational.

Adversarial tests cover deterministic canonical bytes, wrong keys,
invalid signatures, tampered approvals, changed timestamps/digests,
unsupported schemas, revoked keys, trust-epoch and revision downgrades,
rotating signers, invalid successor histories, stale rechecks and full
held-out split custody. No merge to main or new phase was started.
