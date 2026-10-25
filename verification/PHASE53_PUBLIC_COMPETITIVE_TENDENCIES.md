# Phase 5.3 — Descriptive public competitive tendencies

PublicCompetitiveTendencies.analyze requires the Phase 5.1 permitted
spectator log and operator rights attestation, and a Phase 5.2 target.
The log is revalidated by PublicReplayCorpus.ingest, then normalized
using CompetitiveFormatNormalizer. The second ephemeral pass through
PublicProtocolFilter and PublicOpponentIntelligence obtains native
move-use counts per *appearance*, without exporting raw protocol lines.

Output is de-identified: only source replay ID, SHA-256, format scope,
public side slots, appearance indices, canonical displayed species,
native observed move IDs and their past public counts. No player names,
nicknames, chat, private packets, internal opponent sets or server logs.
Identity is always unconfirmed. Matching displayed species on a later
entry only yields a visual match candidate, never a party-member link.

Switch announcements give historical counts, not proven voluntary
switch decisions; they may reflect a faint replacement or pivot.
Only a public drag event is classified as forced displacement.
Zero observed switches does not prove passivity. Transform-copied
moves and simulator-injected [from]/[still] move overrides are
not counted as observed native moves.

All records explicitly carry OBSERVATION_ONLY_NOT_FORECAST and
forecastProduced=false, with voluntarySwitchesKnown=false. Count
ordering is deterministic descriptive sorting, not ranking of the
next action. No probabilities, role-based hidden sets, priors,
calibration or tactical recommendation are introduced.

Format provenance and source-scoped Cobblemon compatibility follow
Phase 5.2. No source-scope marker is a Minecraft runtime attestation.
Tests are synthetic source/Java regressions only. Legacy remains
authoritative over live choices; Shadow remains observational.
No private logs were collected and no Phase 5.4 evaluation was run.
