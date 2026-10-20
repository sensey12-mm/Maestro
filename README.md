# MaestroAI

An unofficial Fabric trainer-AI mod integrating with Radical Cobblemon Trainers API and Cobblemon. This source repository is intended for development and verification; no compiled consumer release is provided.

## Compatibility and build

The pinned build targets Minecraft 1.21.1, Java 21, Cobblemon Fabric 1.7.3 (build 7553235), RCT API 0.15.2-beta (build 7952421) and the versions in `gradle.properties`. Dependencies are installed separately under their respective terms. The RCT AI selector registered by this mod is `maestro`.

```sh
python3 verification/check_repository.py --self-test
python3 verification/check_repository.py
bash gradlew --no-daemon -I verification/verification.init.gradle clean build standaloneRegression
python3 verification/check_jar.py --self-test
python3 verification/check_jar.py
python3 verification/source_oracle.py --self-test
python3 verification/source_oracle.py
```

Development builds use increasing patch versions (`1.0.1`, `1.0.2`, and so on). Update `version` in `gradle.properties` for each new mod development batch; rebuilding the same revision keeps its version. Gradle uses it for the JAR filename and Fabric metadata, and startup reports the loaded metadata version. The JAR checker derives its default path from that property. A version identifies a development revision; retain the commit/checksum for exact provenance.

The last command requires Node.js 20 or newer. It checks the exact downloaded dependency and bundled simulator hashes, executes 50 fixed synthetic damage/hazard/order/speed cases in that simulator, and checks initialized public configuration using Cobblemon's relocated Graal engine. It does not reproduce every scenario in the larger bundled corpus or establish Minecraft compatibility. Upstream sources are extracted only into a temporary local test directory and are not included in this repository or the mod JAR.

The repository safeguard checks exercise local-file ignore rules, required build inputs, LICENSE-trigger coverage and the retained Gradle Apache notices. Ignore rules help prevent accidental staging; they do not replace review of newly added files.

Verification classes are excluded from the mod JAR. CI checks Java21 classes, expected resources/notices and unwanted archive entries; it publishes no artifacts. Experimental Shadow analysis remains observational; these tests do not promote it to live decisions or demonstrate battle win rates.

Runtime move attestation covers every ID in the conditional mechanics calculator, including Geomancy, Thunder, Psystrike, Fire Blast and Spectral Thief. Native verification checks the calculator's complete ID set against the actual operation tables and exercises original registrations, altered definitions, foreign callbacks and irreversible receipt expiration. Unused custom moves may coexist without receiving operation coverage. These configuration checks preserve the separate actor, legality and evidence prerequisites; they do not establish additional move behavior or Minecraft acceptance.

Certified blocked or type-immune hits suppress on-hit follow-ups in the supported conditional evaluator: defensive drops, drain healing, pivoting, recoil and secondary branches do not occur after a proven blocked hit. Missing or expired evidence still yields UNKNOWN. Native differential checks cover successful and blocked exchanges and preserve Spectral Thief's pre-hit boost-theft ordering; they do not certify arbitrary failure mechanics or end-of-turn outcomes.

Successful Draining Kiss heals from actual removed HP with the pinned Gen9 rounding before Life Orb recoil, including HP caps and target KO. Missing exact user max HP or uncertain execution/survival stays UNKNOWN; justified opponent HP intervals remain bounded. Native differential tests cover both actor roles, recoil fainting, Magic Guard and Magic Room. These checks do not resolve arbitrary replacement continuations or establish Minecraft acceptance.

After a definite own Life Orb knockout, a current certified own reserve can establish that the queued exchange continues: surviving actors may use supported self-targeting setup/recovery, while attacks with no target produce no hit effects. Fainting clears stages before that continuation. Replacement and hazard entry remain pending until a new request; an empty switch list or hidden opponent bench never proves party exhaustion. Missing or expired continuation proof remains UNKNOWN, including uncertain survival and unproved terminal boundaries.

Conditional U-turn can enter a sole current certified own reserve before a slower reply. Life Orb recoil remains attached to the outgoing actor. Entry damage and Sticky Web stages must agree with source-scoped calculations, including hazard rounding, Boots, Magic Guard, Levitate, typing and Magic Room. Target knockout or uncertain target survival, ambiguous replacement choice, unsupported entry effects and unproved entry survival remain UNKNOWN; hidden opponent reserves never establish battle continuation. Native differential checks cover these boundaries without certifying later residuals, replacement requests or Minecraft acceptance.

The pinned pack-source check also executes the published MegaShowdown installer in a disposable Java host. Its exact version-marker bytes are allowed alongside the individually verified assets; a matching version string alone grants no source or operation scope. Altered markers, unlisted source files and unsupported runtime effects remain unverified.

Runtime verification also exercises Cobblemon's original Java boot and registry serialization with a relocated engine and synthetic actors. Exact native empty-condition cache entries may be reconstructed after actor entry; modified entries and callbacks remain rejected. Original factory bytes and the actual normalized transport are verified separately. The pack check additionally replays four exact post-boot item factories through their public receiver. Exact transport bytes and newly created callback identities are required; altered factories and unobserved replacements remain unsupported. The observer preserves the original dispatch and does not grant support to an unsupported selected item. These integration checks do not attest an installed Minecraft instance. The addon check also replays the 58 public factories from the whole-hash-pinned [ZA Megas 1.7.3 Fabric artifact](https://www.curseforge.com/minecraft/mc-mods/navas-za-megas/files/7961235), including its post-boot action and weather methods. Exact receiver bytes, callback positions, full configuration fingerprints and fresh leases are required. This establishes provenance for the finite configuration; it does not add tactical support for custom abilities, items, moves or transformations. Altered or unobserved dispatches remain unverified.

When controlled envelope capture is enabled, `PUBLIC_METHOD_AUDIT` reports bounded source-known method checks at startup and public registry boundaries. It reads module/prototype/boot-dex configuration, never actors or requests, and grants no runtime scope. Actual-battle rejections separately report source-known method roles and fixed failure categories. A clean startup check does not establish battle-instance or Minecraft acceptance. Native source verification uses Cobblemon's original context constructor and restrictive module file system.

## Runtime acceptance

Actual Minecraft/server and installed external RCT datapack compatibility is **not yet accepted**. In particular, dependency loading, the `maestro` selector, trainer/datapack identity and references, battle initiation and turns, forced-switch legality and final battle handling require controlled runtime testing. Source/build checks cannot substitute for those tests. Back up an existing installation before an independently authorized test; do not treat this repository as a verified drop-in deployment.

## Rights and third-party software

See `LICENSE` for the scoped All Rights Reserved notice. Public source visibility is not an open-source license. The Gradle wrapper retains its Apache-2.0 headers and embedded license. Dependencies and their names/assets remain governed by their own terms, including Cobblemon MPL-2.0, RCT's applicable custom license and the bundled simulator's MIT-covered components. These notices do not grant rights over third-party material or authorize redistributing dependency binaries/assets.

This project is not affiliated with or endorsed by Mojang, Microsoft, Nintendo, Game Freak, The Pokémon Company, Cobblemon or RCT maintainers.

The finite compatibility checks also verify the four public condition factories from the pinned Raid Dens 0.11.3 artifact, their original LF-preserving dispatch, and five native bag-item initializers from the pinned pack addons. This establishes source provenance, not tactical support for raid effects or bag actions. Raid membership must pass the source-pinned public mode API; missing announcements cannot establish ordinary mode. Active raid conditions remain unsupported, and native item caches must retain verified fields and callback identities after a dex reload.

## Reports and contributions

GitHub Issues and Discussions are disabled; this repository currently provides no general bug-report intake. Keep any reproduction notes minimal and synthetic. Do not post credentials, personal/player identities, private battle packets or raw server logs in pull requests or other public channels. For a sensitive security issue, use GitHub's private vulnerability reporting feature **only if it is enabled**. If no private reporting route is available, do not disclose sensitive details publicly.

An issue or pull request is a proposal, not write/merge permission. Maintainer assignment and review are required; review actual changed content, appropriate tests and third-party notices. Do not assume branch rules automatically enforce every check. Contributions must include sufficient rights to the proposed content; acceptance and licensing terms must be agreed before integration. No external contribution changes this project's source-use notice automatically.

## Phase 3.35 — Shadow acceptance prerequisites

The experimental public-reply comparator now requires its complete, uncorrupted spectator history to certify **the same public turn** as the currently evaluated battle snapshot. A stale or future spectator turn, invalid forced request, unknown runtime lease or hidden opponent effect must not supply a tactical recommendation. Existing live legacy decisions are unchanged.

The standalone native Java verification checks current-turn provenance, partial/private event contamination, forced continuations and replacements, trapped voluntary switches, identity ambiguity, sticky registry lease expiration and privacy-safe diagnostic reason codes. The pinned source simulator verifies priority against both publicly revealed move alternatives. These source/synthetic checks are not Minecraft or RCT acceptance, and do not grant Shadow control.

Runtime compatibility still requires controlled testing with the installed JAR checksum, dependency versions and representative priority, switching, setup and identity-change scenarios. Preserve original diagnostic logs securely for authorized review; do not publish raw battle packets, player details or server logs.
