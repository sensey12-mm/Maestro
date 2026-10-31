# Phase 5 — Trusted Historical Archive Activation and Minecraft Readiness

**Branch:** `Maestro4567` · **Scope:** operator opt-in, historical roles *unbound* and Shadow-only. Legacy still selects every action. **Minecraft not yet tested.** This implementation does not import public replay data automatically or issue trusted replay approvals.

## Operator installation contract

The installed JAR can now use the existing signed Phase 5.21 archive pipeline without a second Java plugin injecting an `Archive` manually. The operator must supply:

1. A **separately rights-reviewed public Showdown spectator replay** in a local, access-controlled UTF-8 file (public event lines only, no private `sideupdate`, client requests or raw owner battle logs).
2. A **signed, append-only Phase 5 custody ledger** prepared *offline* with `PublicCustodyTrustStore.initialize` and independently approved replay hash and expiry. The signing private key stays outside the game. `custody-anchor.state` belongs in a local-only directory, never the repository or mod JAR.
3. The **Ed25519 public bootstrap root and exact state checkpoint** independently anchored outside that directory (for example in the operator-controlled launcher configuration, NOT copied from the ledger). The external state SHA-256, revision and keyring epoch must match the verified committed journal.
4. A verified clock/time floor and the operator's *out-of-band* check that the launcher host clock is accurate. The Java system clock is **not independently attested by this mod**; the `operatorVerifiedHostClock` flag is the operator's responsibility, not proof of time trust. If that cannot be established, leave this feature OFF.
5. Owner-set JVM launch properties. No keys, approvals or historical data are bundled in the released JAR; do not put player data or secret keys in the launch arguments.

All JVM properties below start with `maestroai.phase5Archive.`. The installation requires `-Dmaestroai.phase5HistoricalRolesObserve=true`, then these required **independently set** properties:

| Name | Meaning |
| --- | --- |
| `directory` | Absolute directory containing the offline verified custody journal |
| `replayLog` | Absolute UTF-8 spectator replay path, ≤2 MB; no symlink |
| `replayId`, `format` | Reviewed replay ID and `gen9ou` or `gen9ubers` |
| `roleQueries` | Comma-separated *untrusted proposal* triples `appearanceIndex:displayedForm:moveid`, e.g. `2:zoroark:nastyplot` (max 20). These cannot manufacture unsupported historical role evidence. |
| `replayRightsIndependentlyApproved` | Literal `true` **only after** independent rights review |
| `operatorVerifiedHostClock` | Literal `true` **only after** operator validates clock accuracy |
| `operatorVerifiedTimeFloor` | Independently established minimum Unix UTC epoch second |
| `bootstrapEd25519Spki`, `bootstrapKeyId` | Independently anchored base64 Ed25519 public SPKI and key identifier |
| `bootstrapKeyActiveFrom`, `bootstrapKeyringEpoch`, `bootstrapMinimumRevision`, `bootstrapEarliestSignature` | Independent bootstrap policy fields; Unix seconds where applicable |
| `checkpointStateSha256`, `checkpointRevision`, `checkpointKeyringEpoch` | Exact externally pinned custody state checkpoint |

`bootstrapManifestDigest` is optional and pins the canonical signed manifest digest if provided. **Never** treat unreviewed local files or properties obtained from the same untrusted download as a trusted independent root. The operator first prepares a signed journal offline, stores its returned checkpoint independently, then sets launch properties. There are deliberately no generated keys, replay downloaders, automatic approval switches, or built-in trust bypasses.

When `maestroai.phase5HistoricalRolesObserve=true` is set, `MaestroBattleAI` attempts a single initial install on construction. Missing directory means historical source is absent and emits `UNPROVISIONED` when observation was explicitly enabled (Phase 5 v2 current-public diagnostic may still run). Partial/invalid/tampered input emits only a redacted `ABSTAIN` notice. A verified archive is **not** a Pokemon identity proof.

On every inspection and on prior-report `current()` checks, the trusted source re-reads and verifies the **exact pinned** local custody journal, the unchanged original rights-reviewed replay file (bytes/hash, size and no symlink), and trusted clock. Post-install replay alteration, removal or substitution permanently withdraws that battle session. A revoked/missing/tampered/expired/mismatched journal, invalid signature or clock rollback revokes that battle session. An operator must explicitly provide a **new independently pinned checkpoint and restart the JVM** for approved keyring/ledger successors; on-disk changes alone can never silently become trusted. No live policy reload from an untrusted file.

## Minecraft acceptance — owner-authorized installation only

Use the **exact successful public Actions JAR** in the existing Minecraft 1.21.1/Fabric/Cobblemon 1.7.3/RCT 0.15.2-beta/Java 21 setup. First verify opt-in OFF has no historical diagnostic. Then provision the real rights-reviewed archive and independent pins, opt in and use the existing reusable BrAI test teams (do not request new teams): a publicly witnessed Stealth Rock or Swords Dance should yield the *live public capability* separately from any conditional *historical role*; historical labels stay **unbound to current Pokémon**. In Team 4, Unovan Zoroark must disguise as Greninja (Greninja last eligible) and Transform/Illusion must not allow identity mapping. Check expiry, revoked/missing/corrupt journal, old checkpoint, wrong signer, unknown role, changed replay, wrong physical battle, repeated callback, turn advance and battle end all abstain/withdraw; test diagnostics OFF against Legacy choice. These are **instructions, not performed tests**.

**Remaining acceptance:** a real independently authorized replay, operator keyring and pinned checkpoint, verified clock source and installed Minecraft run. Source-level regression/CI alone cannot prove runtime native callback delivery or tactical value. Never promote Shadow or merge into `main` from this activation work.

## Source-level acceptance record (following installation readiness audit)

The installed-game entrypoint still requires Java 21, Fabric, Cobblemon 1.7.3 and the unmodified pack. The operator activation is opt-in and anchored to the same immutable approved replay. The regression explicitly checks a publicly witnessed hazard plus a separate *unbound* historical role, report withdrawal after replay alteration/removal and signed-approval expiry, and no resurrection after the evidence returns. Earlier overlay and native source regressions cover wrong physical battles, repeated callbacks, contradictory public history, and battle-end withdrawal. Java/CI checks do **not** substitute for real Minecraft observations.

**Status:** Minecraft installation not accessible to the developer; no actual battle has been run or witnessed in this batch. Record the installed JAR version, exact SHA, native callback observations, and diagnostic-only/Legacy-off comparison only after authorized in-game testing. Never publish raw battle logs or private IDs.

## Read-only operator preflight (v1.0.85)

Before installing the JAR, the operator can execute a **local preflight** in the source checkout using Java 21 and the existing Gradle verification classpath:

```text
gradlew.bat --no-daemon -I verification/verification.init.gradle phase5ArchivePreflight -Dmaestroai.phase5HistoricalRolesObserve=true -Dmaestroai.phase5Archive.directory=... [the other independently provisioned -Dmaestroai.phase5Archive.* settings]
```

On Linux use `./gradlew`. The `...` notation above is an instruction placeholder, **not** a runnable literal; the full property names are listed in the table earlier. A plain unprovisioned invocation reports `OBSERVER_DISABLED` or `ARCHIVE_UNPROVISIONED` and exits with a nonzero code. The preflight checks the same original replay, trusted public-key root, signed journal, pinned checkpoint, and freshness as the live installer. It prints one redacted verdict:

- `SOURCE_READY_FOR_MINECRAFT_TEST`: local **operator-declared** source prerequisites verified, **NOT** independently proven replay rights, an attested clock or a Minecraft acceptance.
- `CUSTODY_OR_ARCHIVE_REJECTED`: malformed/expired/substituted or otherwise unsupported archive configuration; no local paths, source IDs or private data are printed.
- `OBSERVER_DISABLED` / `ARCHIVE_UNPROVISIONED`: no historical source was installed.

This never loads a battle, writes private files, creates keys, downloads replay content or enables Shadow decisions. A successful preflight is a prerequisite for manual Minecraft acceptance, not a substitute. The operator must separately validate replay-use permission and clock trust; synthetic test fixtures cannot confer that approval.

## Offline signed-custody importer (v1.0.86)

This is an **offline local operator tool**, not a new intelligence engine. It imports a separately reviewed, externally **Ed25519-signed** manifest into the existing atomic Phase 5 custody journal. It never generates keys, signs statements, approves replays or downloads historical data.

Inputs:
- Existing **absolute** state directory (fresh and empty of custody state for `init`), e.g. `C:\MaestroLocal\custody`.
- Absolute independent bootstrap root `root.properties`, held **outside** the state directory and from an operator-verified trusted channel. Exact keys:
  `bootstrapKeyId`, `bootstrapEd25519Spki`, `bootstrapKeyActiveFrom`, `bootstrapKeyringEpoch`, `bootstrapMinimumRevision`, `bootstrapEarliestSignature`; optional `bootstrapManifestDigest`. All key material is **public**.
- Absolute externally signed `signed-review.properties` outside the state directory. Required:
  `schemaVersion=1`, `signerKeyId`, `signedAtEpochSecond`, `previousManifestDigest`, `signatureBase64`, `ledgerRevision`, `decisionCount`, plus for each zero-based decision `decision.0.replayId`, `decision.0.format`, `decision.0.inputSha256`, `decision.0.status`, `decision.0.decidedAtEpochSecond`, `decision.0.expiresAtEpochSecond`, `decision.0.reviewDigest`. For additional decisions use `decision.1.*` etc. These are **already signed fields**. The external signer must sign the exact bytes from `PublicSignedCustody.canonicalBytes` in a controlled offline signing process. This importer cannot construct a genuine approved decision on its own.
- A separately validated **trusted Unix UTC epoch second** (do not copy the current time from an untrusted replay or bundle). The tool accepts it as a supplied value; it **cannot** cryptographically attest clock accuracy.
- For `advance` only, absolute independently stored `prior-pin.properties` with `checkpointStateSha256`, `checkpointRevision`, `checkpointKeyringEpoch`. Do NOT use a pin supplied in the untrusted bundle or state directory.

The two arguments below are illustrative placeholders; they are not real approved inputs. On Windows use a CMD shell with paths without `|`, running from the public Maestro source checkout:

```text
gradlew.bat --no-daemon -I verification/verification.init.gradle phase5ArchiveImport "-Pphase5ImportArgs=init|C:\MaestroLocal\custody|C:\MaestroTrust\root.properties|C:\MaestroTrust\signed-review.properties|<verified-unix-second>"
```

To append **one already signed** later ledger decision (for example revocation), use:

```text
gradlew.bat --no-daemon -I verification/verification.init.gradle phase5ArchiveImport "-Pphase5ImportArgs=advance|C:\MaestroLocal\custody|C:\MaestroTrust\root.properties|C:\MaestroTrust\signed-review-next.properties|C:\MaestroTrust\prior-pin.properties|<verified-unix-second>"
```

The tool verifies the external Ed25519 root, signature, prior signed digest, appended chronological decision, external pin and state before writing. A rejected import prints only `Phase5Import[verdict=ABSTAIN;authority=NONE]`; it does not print file paths or replay IDs. A successful import prints the **new exact checkpoint** for the operator to independently anchor **outside** the custody directory. Store this checkpoint in trusted settings before accepting another update. This tool does **not** support keyring rotations: they require separately verified signed `KeyringUpdate` and a new checkpoint. For append-only advances, leave the optional root `bootstrapManifestDigest` empty unless an external authority has explicitly pinned the new exact manifest; a digest pin tied to the original manifest intentionally rejects every different successor.

### Exact Minecraft handoff

1. Verify the final exact-commit public CI/JAR, and independently verify real replay-use rights, signer authority and clock.
2. Import the externally signed manifest with the offline tool; save its checkpoint separately. The reviewed spectator replay file remains separate; the import does **not** download it or automatically grant its use.
3. Configure `-Dmaestroai.phase5HistoricalRolesObserve=true` and the existing `maestroai.phase5Archive.*` JVM properties using the same root, replay and independently pinned *latest* checkpoint. Restart Minecraft after every approved custody advance.
4. Run `phase5ArchivePreflight`. Only `SOURCE_READY_FOR_MINECRAFT_TEST` is locally provisioned; it still does **not** prove real rights or Minecraft runtime behavior.
5. Use the existing 4 BrAI teams/8 battles and the [installed acceptance matrix](PHASE5_INSTALLED_HISTORICAL_INTELLIGENCE_ACCEPTANCE.md); verify actual callbacks, historical/live separation, identity abstention and Legacy chooser isolation. No game acceptance is claimed until that evidence exists.

**Do not** put private signing keys, private gameplay logs, unreviewed replays or local operator trust files in GitHub. The tool's successful local signature check does not itself establish independent rights or latest-keyring custody.

## Public CI JAR installation handoff (v1.0.88)

After **all** exact-commit public GitHub Actions checks succeed, the workflow publishes one short-lived `maestroai-verified-<commit SHA>` artifact with the compiled JAR plus its SHA-256 checksum. Download from that workflow run's **Artifacts** section, extract the JAR and compare the SHA-256 locally (Windows: `certutil -hashfile maestroai-1.0.88.jar SHA256`). The workflow publishes nothing if the build, native ABI or simulator check fails. No operator configuration, public replay, private key or historical custody state is uploaded. The ZIP artifact expires after seven days. This proves the public build's provenance, **not** Minecraft runtime acceptance or permission to use a replay.

## Externally signed keyring rotation and signer revocation (v1.0.92)

The offline importer now supports a **single authenticated keyring update** with one append-only custody decision per operation. No signing key, approval or policy is created by Maestro. To use a new key or revoke an existing signer, an already trusted, non-revoked signer must independently sign the exact canonical `PublicCustodyTrustStore.updateBytes(...)` for an updated policy. A separately authorized signer must sign the successor ledger via `PublicSignedCustody.canonicalBytes(...)`. Both signed artifacts must be obtained from the operator's independent approval channel.

Create an external `signed-keyring-update.properties` file containing `signerKeyId`, `signedAtEpochSecond`, `previousPolicyDigest`, `signatureBase64`, `nextKeyringEpoch`, `nextMinimumKeyringEpoch`, `nextMinimumRevision`, `nextEarliestSignature`, `nextKeyCount`, and optional `nextManifestDigest`. For every zero-based key index include `nextKey.0.keyId`, `nextKey.0.ed25519Spki`, `nextKey.0.activeFrom`, `nextKey.0.revokedAt` (0 = not revoked). Preserve *all* existing keys exactly except an irreversible revocation timestamp; add new keys with independent public SPKIs. The next keyring epoch and minimum epoch must increase together by one. A signer revocation timestamp must be no earlier than the update signature time, and a successor ledger cannot be signed by a revoked key at evaluation time. Keyring changes never bypass the existing successor decision requirement.

```text
gradlew.bat --no-daemon -I verification/verification.init.gradle phase5ArchiveImport "-Pphase5ImportArgs=rotate|C:\MaestroLocal\custody|C:\MaestroTrust\root.properties|C:\MaestroTrust\signed-review-next.properties|C:\MaestroTrust\prior-pin.properties|C:\MaestroTrust\signed-keyring-update.properties|<verified-unix-second>"
```

Files must be absolute, distinct and outside the journal; the prior checkpoint and bootstrap public key must remain independently authenticated. The importer verifies the signed update using the **old** root/policy, the successor manifest using the **new** policy, and atomically writes an append-only journal. Copy the returned new checkpoint to a separate trusted location before trusting/restarting the new runtime. A successful import does not establish replay-use rights. Any live historical Shadow report is withdrawn if its observed trusted keyring epoch changes, even with a valid new signer; **restart with the independently pinned latest checkpoint** to admit newly scoped reports. Never reuse an old keyring lease or downgrade the root. Minecraft runtime acceptance remains unperformed.
