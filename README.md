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

## Runtime acceptance

Actual Minecraft/server and installed external RCT datapack compatibility is **not yet accepted**. In particular, dependency loading, the `maestro` selector, trainer/datapack identity and references, battle initiation and turns, forced-switch legality and final battle handling require controlled runtime testing. Source/build checks cannot substitute for those tests. Back up an existing installation before an independently authorized test; do not treat this repository as a verified drop-in deployment.

## Rights and third-party software

See `LICENSE` for the scoped All Rights Reserved notice. Public source visibility is not an open-source license. The Gradle wrapper retains its Apache-2.0 headers and embedded license. Dependencies and their names/assets remain governed by their own terms, including Cobblemon MPL-2.0, RCT's applicable custom license and the bundled simulator's MIT-covered components. These notices do not grant rights over third-party material or authorize redistributing dependency binaries/assets.

This project is not affiliated with or endorsed by Mojang, Microsoft, Nintendo, Game Freak, The Pokémon Company, Cobblemon or RCT maintainers.

## Reports and contributions

GitHub Issues and Discussions are disabled; this repository currently provides no general bug-report intake. Keep any reproduction notes minimal and synthetic. Do not post credentials, personal/player identities, private battle packets or raw server logs in pull requests or other public channels. For a sensitive security issue, use GitHub's private vulnerability reporting feature **only if it is enabled**. If no private reporting route is available, do not disclose sensitive details publicly.

An issue or pull request is a proposal, not write/merge permission. Maintainer assignment and review are required; review actual changed content, appropriate tests and third-party notices. Do not assume branch rules automatically enforce every check. Contributions must include sufficient rights to the proposed content; acceptance and licensing terms must be agreed before integration. No external contribution changes this project's source-use notice automatically.

## Phase 3.35 — Shadow acceptance prerequisites

The experimental public-reply comparator now requires its complete, uncorrupted spectator history to certify **the same public turn** as the currently evaluated battle snapshot. A stale or future spectator turn, invalid forced request, unknown runtime lease or hidden opponent effect must not supply a tactical recommendation. Existing live legacy decisions are unchanged.

The standalone native Java verification checks current-turn provenance, partial/private event contamination, forced continuations and replacements, trapped voluntary switches, identity ambiguity, sticky registry lease expiration and privacy-safe diagnostic reason codes. The pinned source simulator verifies priority against both publicly revealed move alternatives. These source/synthetic checks are not Minecraft or RCT acceptance, and do not grant Shadow control.

Runtime compatibility still requires controlled testing with the installed JAR checksum, dependency versions and representative priority, switching, setup and identity-change scenarios. Preserve original diagnostic logs securely for authorized review; do not publish raw battle packets, player details or server logs.
