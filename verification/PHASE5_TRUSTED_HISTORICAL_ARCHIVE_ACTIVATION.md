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

When `maestroai.phase5HistoricalRolesObserve=true` is set, `MaestroBattleAI` attempts a single initial install on construction. Missing directory means historical source is absent (Phase 5 v2 current-public diagnostic may still run). Partial/invalid/tampered input emits only a redacted `ABSTAIN` notice. A verified archive is **not** a Pokemon identity proof.

On every inspection and on prior-report `current()` checks, the trusted source re-reads and verifies the **exact pinned** local custody journal, the unchanged original rights-reviewed replay file (bytes/hash, size and no symlink), and trusted clock. Post-install replay alteration, removal or substitution permanently withdraws that battle session. A revoked/missing/tampered/expired/mismatched journal, invalid signature or clock rollback revokes that battle session. An operator must explicitly provide a **new independently pinned checkpoint and restart the JVM** for approved keyring/ledger successors; on-disk changes alone can never silently become trusted. No live policy reload from an untrusted file.

## Minecraft acceptance — owner-authorized installation only

Use the **exact successful public Actions JAR** in the existing Minecraft 1.21.1/Fabric/Cobblemon 1.7.3/RCT 0.15.2-beta/Java 21 setup. First verify opt-in OFF has no historical diagnostic. Then provision the real rights-reviewed archive and independent pins, opt in and use the existing reusable BrAI test teams (do not request new teams): a publicly witnessed Stealth Rock or Swords Dance should yield the *live public capability* separately from any conditional *historical role*; historical labels stay **unbound to current Pokémon**. In Team 4, Unovan Zoroark must disguise as Greninja (Greninja last eligible) and Transform/Illusion must not allow identity mapping. Check expiry, revoked/missing/corrupt journal, old checkpoint, wrong signer, unknown role, changed replay, wrong physical battle, repeated callback, turn advance and battle end all abstain/withdraw; test diagnostics OFF against Legacy choice. These are **instructions, not performed tests**.

**Remaining acceptance:** a real independently authorized replay, operator keyring and pinned checkpoint, verified clock source and installed Minecraft run. Source-level regression/CI alone cannot prove runtime native callback delivery or tactical value. Never promote Shadow or merge into `main` from this activation work.

## Source-level acceptance record (following installation readiness audit)

The installed-game entrypoint still requires Java 21, Fabric, Cobblemon 1.7.3 and the unmodified pack. The operator activation is opt-in and anchored to the same immutable approved replay. The regression explicitly checks a publicly witnessed hazard plus a separate *unbound* historical role, report withdrawal after replay alteration/removal and signed-approval expiry, and no resurrection after the evidence returns. Earlier overlay and native source regressions cover wrong physical battles, repeated callbacks, contradictory public history, and battle-end withdrawal. Java/CI checks do **not** substitute for real Minecraft observations.

**Status:** Minecraft installation not accessible to the developer; no actual battle has been run or witnessed in this batch. Record the installed JAR version, exact SHA, native callback observations, and diagnostic-only/Legacy-off comparison only after authorized in-game testing. Never publish raw battle logs or private IDs.
