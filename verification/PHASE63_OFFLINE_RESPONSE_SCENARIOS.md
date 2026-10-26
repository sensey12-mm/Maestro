# Phase 6.3 — Offline source-conditional opponent-response scenarios

Strictly offline deterministic synthetic comparison inputs. Phase 6.2 original framed public fixture, actor/turn/appearance receipt, and claimed own-action set are recomputed and validated before scenario construction.

Only the intersection of that appearance's publicly witnessed move IDs and `CobblemonMechanics` source-listed moves may enter one-turn conditional scenarios. Pinned Phase 4 mechanics and forced/trapped own choices are checked; unsupported own/reply moves, unverified switch entry, zero observed replies, budgets and missing source effects explicitly abstain. The witnessed set is ALWAYS open to unobserved opponent responses, never a certified currently legal opponent response set or probability distribution. The module never assigns expected-value response weights.

Phase 5.21 is not bound to the same battle: a separately pinned, original signed and independently recomputed historical handoff may produce only per-historical-appearance grade COUNTS. No archived identity, move ID, candidate, form, source method or appearance index can be mapped to the fixture's active opponent. Neither a Phase 5 guess nor a Phase 5 public event supplies a new Phase 6 current-battle move. No private data, replay IDs or signatures appear in output. Missing Phase 5 is an explicit abstention, not a neutral build assumption.

Output contracts have private constructors, immutable collections, permanent runtime/legality/calibration/Shadow/promotion denials, and no live integration. `MaestroBattleAI` and `LivePromotionGate` are unchanged. This is for FrAIL review, not Minecraft acceptance or owner authorization to integrate. Exact-commit GitHub Actions and required source oracle must be verified separately.

Synthetic adversarial regression includes deterministic two-action/two-witness matrix, zero public evidence, unsupported own/reply moves, trapped switch, actor/turn mismatch, replay re-entry, private packet injection, schema/source drift, immutable output and Phase 5 tactical-purpose rejection.

Phase 6.2 public CI #40370499881 had source-oracle verification still running at Phase 6.3 inception; build, standalone and JAR steps were successful. Full Phase 6.2 acceptance must not be inferred from those partial checks.
