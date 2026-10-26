# Phase 5.1 - Offline public replay foundation

Scope: permission-gated ingestion in PublicReplayCorpus. No network fetching, corpus
scraping, empirical battle modeling, live tactical control or Showdown-to-Cobblemon
mechanics certification.

Official documented replay formats:
https://github.com/smogon/pokemon-showdown-client/blob/master/WEB-API.md
The replay service distinguishes public and private listings:
https://replay.pokemonshowdown.com/
The Showdown server is MIT licensed; client software is AGPLv3. Software licenses
do NOT independently grant blanket reuse rights over player replay content:
https://github.com/smogon/pokemon-showdown/blob/master/README.md
https://github.com/smogon/pokemon-showdown-client/blob/master/README.md

Only use individual, independently verified public-listed spectator .log replays
with explicit replay-use permission. The operator MUST verify the rights and
visibility externally before attesting via Source; boolean metadata cannot
authenticate either. Private replays, inputlogs, hidden teams, player request
payloads, automatic downloads, bulk harvesting and real replay redistribution
are not authorized or implemented. No real replay was ingested in this batch.

Narrow implemented format scope: gen9ou/gen9ubers singles with matching
generation, tier, a start, turn(s), and terminal result. Other formats UNKNOWN.
Input is rejected before extraction if it contains split/private transport,
sideupdate, request, inputlog, team export, seed or PP synchronization.

Accepted events pass through existing PublicProtocolFilter and PublicBattleObserver
and PublicOpponentIntelligence. Output contains occurrence-scoped public moves,
item/ability revelations and canonical displayed species only; no raw lines,
nicknames, players, chat or player-name maps. Appearance continuity is ALWAYS
unconfirmed, even with repeated names or species. Transform/Illusion retain
uncertainty and must not create confirmed hidden sets.

Every result is marked COBBLEMON_TRANSFER_UNVERIFIED; showdowns rules and public
set observations are NOT mechanics proofs, tactical probabilities or verified
Minecraft gameplay. Current testing consists only of synthetic standalone
regressions; no permission enforcement from the remote service is claimed.
