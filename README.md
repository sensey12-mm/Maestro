# MaestroAI

**MaestroAI** is an unofficial Fabric Java mod providing an RCT `maestro` trainer AI for Minecraft / Cobblemon. This is a **development/source repository**, not an assurance of complete installed-game compatibility or competitive performance.

## Current development status (3 November 2026)

- **Active development branch:** [`Maestro4567`](https://github.com/sensey12-mm/Maestro/tree/Maestro4567). **Protected default:** `main`; it is **not** the current promoted-Shadow release branch. No merge into `main` has been authorized.
- **Source version:** `1.1.5` at [`236669af71e0`](https://github.com/sensey12-mm/Maestro/commit/236669af71e0dd1a1b6064645c91dd2c6b2e0625), with [exact-commit Actions #41518874540 SUCCESS](https://github.com/sensey12-mm/Maestro/actions/runs/41518874540). Required `verify`, `pack_source` and `publish` succeeded; artifact name `maestroai-verified-236669af71e0dd1a1b6064645c91dd2c6b2e0625`.
- **Live choice:** Shadow is the owner-authorized independent **primary** AI. Legacy remains an emergency/safety fallback, **not** a routine tactical scorer. Live action legality and verified public/private evidence separation are mandatory.
- **Not yet proven:** Minecraft-installed **1.1.5** opponent recognition and zero routine Legacy fallback; no claimed win-rate benchmark or completed Phase 7 tactical simulator.
- **Historical branches removed:** `Maestro-3`, `Maestro-4`, `Maestro-5`, `Maestro-6`, `Maestro-7` and `Maestro457`. Historical implementation is retained by Git commit identity in the current branch. Phase 7's further development is independent and unfinished.

See [architecture](docs/architecture.md), [verification and Minecraft acceptance](docs/verification-and-acceptance.md), [documentation index](docs/README.md) and the [preserved historical development README](docs/archive/README_BEFORE_PROMOTION7.md).

## Compatibility and build

Pinned targets: **Minecraft Java 1.21.1**, **Java 21**, **Fabric**, **Cobblemon Fabric 1.7.3** and **Radical Cobblemon Trainers API 0.15.2-beta**. Check `gradle.properties` for precise build dependencies and version, and use the compatible external mods/data packs; these dependencies remain subject to their own licenses.

On Windows, with Java 21 selected, from a checked-out Maestro repository:

```bat
git fetch origin
git switch Maestro4567
git pull --ff-only origin Maestro4567
gradlew.bat --no-daemon build
```

The local build is **not** an installed-game test and does not by itself certify the GitHub release artifact. Run `python3 verification/check_repository.py --self-test` and `python3 verification/check_repository.py` for repository safeguard checks; comprehensive Java/native checks run in the pinned GitHub Actions workflow.

## Verification, releases and ownership

On `Maestro4567`, **release verification** requires success for the **same commit** across `verify` (Java/checkers, JAR, native simulator), `pack_source` (independent pinned simulator profile), then `publish` (verified SHA-matching JAR and checksum). A green CI run is **not proof of installed Minecraft acceptance**. See [acceptance guide](docs/verification-and-acceptance.md) for the checks and limits.

The mod's public Phase 4 mechanics and Phase 5/6/7 infrastructure have different scope and certainty. Publicly shown opponent names may be **Illusion disguises**; conditional display-based typing is not confirmed true identity. Historical Phase 5 role hypotheses cannot reveal live hidden opponent state. Further tactical experiments must remain shadow/non-authoritative until separately owner-authorized.

Source and third-party materials retain their original licenses and attribution. This repository does **not** contain or permit private Headquarters histories, personal battle logs, private opponent data or unrestricted copyrighted dependency code. Read [LICENSE](LICENSE) and the relevant dependency terms.

The older 46 KB milestone-by-milestone project README has been [preserved verbatim as dated historical source context](docs/archive/README_BEFORE_PROMOTION7.md) rather than silently deleted; its previous claims about source version, CI artifact publication, branches and Legacy authority are **not** current instructions.
