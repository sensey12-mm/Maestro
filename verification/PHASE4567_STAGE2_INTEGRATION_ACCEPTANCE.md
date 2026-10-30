# Maestro4567 — Stage 2 Independent Phase 4/5/6/7 Source Integration

**Integration branch:** `Maestro4567`. **Scope:** source-level integration and synthetic verification only.

## Exact input ancestry

- `Maestro457`: `362204bfcd3510e1a40f05586365f484ad60c6c8` (previously accepted Phases 4, 5, 7).
- `Maestro-6`: `bdf385873b50982bf9b9ffe8ec8c8c8d84ba3d2b` (Phase 6.27).
- The Phase 6 head is **44 commits ahead and zero commits behind** the pinned `Maestro457` head. The merge-base is precisely the `Maestro457` SHA. This is a linear continuation, not an unrelated tree needing a second artificial merge.
- At intake, `Maestro-6` contained **all 276 files** from `Maestro457`, with zero removed, 70 new files and nine modified existing paths. All integration additions stay in `Maestro4567`; input branches and `main` remain unchanged.

## Source integration and regression gates

The existing `Integration457Regression` and each Phase 4, 5, 6, and 7 synthetic regression remain registered. Added `Integration4567Regression` to assert in the *same compiled integration*:

1. A pinned Phase 5 consumer cannot request current-battle use or tactical purpose.
2. A source-consistent Phase 6 synthetic fixture invokes Phase 4 conditional mechanics, but cannot authenticate the opponent's full response set, assign opponent probabilities or issue a recommendation.
3. A caller-rehashed spoofed public actor fails the Phase 6 provenance/response boundary.
4. A Phase 6 native `sendSidedUpdate` RETURN remains read-only; a newer public source epoch withdraws its freshness; split identity and client delivery stay unverified.
5. Even all-PASS externally reviewed *offline* Phase 7 stages never claim integrated installed-battle readiness.
6. An opt-in live-promotion property without separately approved candidate still returns Legacy's action; the production chooser contains no Phase 5/6 offline tactical invocation.

CI checks the dedicated branch's repository safeguards, full Java 21 build, standalone and Phase 7 synthetic regressions, JAR validation, exact native Mixin ABI, and pinned external source/differential oracles. **Only the completed exact-commit run may support a CI GO.**

## Limits and deferred acceptance

- **Source-level integration is not Minecraft validation.** Native `sendSplitUpdate` RETURN and per-client delivery are not proven; `sendSidedUpdate` RETURN does not imply delivery.
- The previously observed real-game 1.0.69 sided RETURN does **not** validate the later 1.0.70–1.0.72 lifecycle, history-parser and stale-epoch fixes.
- Phase 5 remains an *offline* source-conditional opponent knowledge system. It does not publish calibrated real-time response probabilities or authorize a live binder.
- Phase 7 is not a validated installed mechanics simulator and its `integrated()` readiness flag remains false.
- Phase 4's unsupported mechanic branches must continue yielding UNKNOWN.
- Legacy still chooses live actions. Shadow remains non-authoritative. No `main` merge, Shadow promotion, trainer modifications, Minecraft tests, or private Headquarters CI are authorized by Stage 2.

## Acceptance criteria

**GO for source integration** requires successful exact-head public GitHub Actions, all Phase 4/5/6/7 synthetic suites, native source oracle, and a separate, truthful list of unresolved runtime gates. A missing or failed CI conclusion means **PENDING/NO-GO** rather than assumed success.

**NO-GO for gameplay acceptance and Shadow promotion** until separately authorized and independently verified runtime evidence, public-origin/current-turn binding, legal-action coverage, supported mechanics and Phase 7 real-evidence evaluation exist.
