# Phase 7 — offline verification benchmarks

This folder contains the **public, runnable Phase 7 verification harness**, not a Cobblemon battle simulator, approved integration test, or live trainer decision policy. The code is compiled in the Gradle `verification` source set and excluded from the distributable mod JAR. Live battles remain under Legacy control; Shadow is observational.

## Run from the Maestro repository root

Requires Java 21 and the repository's pinned Gradle dependencies.

```sh
python3 verification/check_repository.py --self-test
python3 verification/check_repository.py
bash gradlew --no-daemon -I verification/verification.init.gradle clean build standaloneRegression phase7Benchmark --stacktrace
```

The `phase7Benchmark` task defaults to `verification/benchmarks/phase7_1_synthetic.json`, configurable with `-Pphase7Fixture=path/to/file.json`. This checked-in file contains **invented synthetic utility and uncertainty criteria**, not actual opponent moves or real battle outcomes. It includes stale/unsupported cases to test abstention.

## Test interfaces

| Scope | Verification-only Java API | What it verifies, and what it does not |
| --- | --- | --- |
| 7.1 | `Phase7Benchmark` | Seeded, deterministic legal-action benchmark; synthetic regret is not a battle win rate |
| 7.2–7.3 | `Phase7LegacyImport` | Public-only, actor/turn/own-request-bound Legacy action receipt intake; no self-attested authenticity |
| 7.4 | `Phase7Corpus` | Local reviewed-capture comparison, strict provenance and private-corpus boundaries |
| 7.5 | `Phase7TrustedIntake` | Detached Ed25519 intake with separately trusted custody review; a signature is not proof that mechanics are true |
| 7.6–7.7 | `Phase7RegressionGate`, `Phase7StratifiedAudit` | Pinned baseline and stratified, disclosure-safe offline regressions |
| 7.8–7.9 | `Phase7ShadowReplay`, `Phase7ReplayMatrix` | Pinned Shadow replay and cross-version comparison; search graphs are not physical battle simulations |
| 7.10 | `Phase7SelfPlay` | Bounded *observational* dual-agent continuation over an externally supplied transition graph |
| 7.11 | `Phase7MechanicsAdapter` | Versioned public mechanics-evidence interface and full-graph transition coverage gates |
| 7.12–7.13 | `Phase7Readiness` | End-to-end evidence chain, explicit abstention and source/coverage checks; always reports `integrated=false` here |

These Java types live under `verification/java/com/maestroai/training/` and have matching standalone regression coverage. Their schemas, typed statuses, constructors and validation rules are authoritative in source; this README is a navigation guide, not a replacement contract.

## Evidence and local data boundaries

- Public-state fingerprints, action legality, turn/actor identity, version scope and custody pins must match. Stale, incomplete, ambiguous or unsupported evidence **fails closed**.
- Mock Phase 4 mechanics, invented graph transitions and synthetic trusted-review stubs exist only to test the verifier. They **must not** be promoted to production evidence, confirmed opponent knowledge or externally validated battle mechanics.
- Trusted Legacy originals, receipts, signatures, reviewer trust anchors, baseline reports and replay data are local inputs and outputs under Git-ignored `run/phase7-corpus/`, with separate owner-approved custody. Do not commit or publish them.
- Diagnostics suppress raw capture data, private identifiers and small cohorts. A complete offline trace does not itself establish a selected legal action or an accurate predicted opponent reply.
- The benchmark does not yet establish a real competitive win rate, faithful installed Cobblemon simulation, fully integrated Phase 4/5/6 intelligence or Minecraft compatibility.

## Stop and integration boundary

Independent Phase 7 infrastructure stabilized through 7.13. **Do not turn synthetic-only tests into invented follow-up feature batches.** Final Phase 7 acceptance requires source-verified and owner-authorized Phase 4/5/6 integration, FrAIL's independent technical review, version-matched real mechanics evidence and separate Minecraft validation. No merge into `main`, live tactical promotion or additional battle campaign is authorized by these offline checks.
