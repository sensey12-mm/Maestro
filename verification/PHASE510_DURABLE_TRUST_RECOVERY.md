# Phase 5.10 — Durable offline trust anchors and recovery

Scope: local-only persistence for the Phase 5.9 public-key verifier.
No real replay inputs or private Headquarters records are committed.
No Minecraft/runtime integration and no tactical policy changes.

## Trust roots and threat boundary

The caller must obtain the **bootstrap TrustedPolicy** and the **latest
exact Checkpoint** independently of the filesystem containing
`custody-anchor.state`. A checkpoint pins the SHA-256 of the entire
serialized state, custody revision and keyring epoch. The state file
is untrusted, even if it contains apparently valid keys and signatures.

The store **cannot** prove a checkpoint is current, withstand a trusted
caller submitting an older checkpoint, or authenticate its own root
of trust. It has no private signing keys. A full machine compromise,
malicious process with access to the trusted policy/checkpoint, rollback
of both the file and the independent anchor, or an untrusted clock is
outside this source-scoped guarantee.

## Atomic persistence and interrupted-update recovery

`initialize(directory, bootstrap, genesis, time)` accepts a preexisting
local directory and an independently trusted signer. It writes a
versioned canonical binary journal using a newly created pending file,
forces its content to disk, atomically renames to the committed file,
then syncs the containing directory. Atomic rename is **required**:
unsupported filesystems fail closed, not through unsafe copy fallback.
The default POSIX file mode is owner-read/write.

`recover(directory, bootstrap, anchoredCheckpoint, time)` verifies the
**exact** committed file hash, parses with strict size/format bounds,
verifies every signed keyring update from the immutable bootstrap and
verifies the final Ed25519 custody manifest. Only AFTER valid recovery
does it safely discard an interrupted pending file. Corrupt committed
files, mismatched anchors, future-dated signatures, unsupported formats,
symlinks, wrong keys or inconsistent revisions fail closed.

An interruption **before** atomic rename leaves the old committed
checkpoint readable and the incomplete pending file recoverable.
An interruption **after** rename but **before external checkpoint
publication** produces a legitimate new file that does NOT match the
old external checkpoint. This requires an independently authorized
reconciliation/re-pin: the implementation intentionally fails closed
instead of silently rolling back or accepting an unaudited advance.

## Signed keyring lifecycle

`KeyringUpdate` is signed using the already-trusted active Ed25519
key over a separate canonical domain and the exact prior-policy hash
plus complete next policy. `verifyKeyringUpdate` enforces:
- one-step keyring epoch advancement and no lower trust floor;
- no removal, substitution, unrevocation or resurrection of old keys;
- no erasure/change of an externally fixed manifest checkpoint;
- authorized signer active when the update was signed;
- explicit cryptographic verification and validated newly added keys.

`advance` takes the last externally pinned state, validates signed
updates, and accepts only the **direct append-only successor** custody
manifest (revision +1, exact predecessor signed digest and immutable
decision prefix). It returns a new checkpoint; the trusted caller must
publish/pin this new checkpoint independently before subsequent use.
Key revocations remain effective even after process restart.

## Offline Phase 5 provenance integration

`PublicReplayProvenance.ingestWithDurableCustody` and
`verifySplitWithDurableCustody` recover one validated snapshot and
then apply all prior replay/split permission, version, public-evidence,
identity-ambiguity, legal-knowledge and Shadow restrictions. They
do not create battle actions, trained opponent priors or runtime receipts.

All file contents and synthetic test artifacts stay on a temporary
local filesystem. Replayed state, modified bytes, removed/truncated
fields, pending symlinks, expired trust, key deletion, signer forgeries,
revoked approvals and stale checkpoints are rejected. The tests use
synthetic Java-generated keys with NO committed private key values.

No claim of actual Minecraft acceptance, real external custody
authorization or verified competitive skill is made. Legacy remains
the live authority, Shadow only observes. The branch remains isolated
from `main`, and no runtime integration is begun.
