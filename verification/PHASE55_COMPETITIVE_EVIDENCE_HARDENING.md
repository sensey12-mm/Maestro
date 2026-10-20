# Phase 5.5 - Offline competitive evidence hardening

Scope: synthetic end-to-end audit of permitted public spectator evidence.
No live control changes, no external replay retrieval, no new phase.

Replay parsing now fails closed on protocol kinds outside the supported
public-event subset or limited metadata list. It enforces a single
start, sequential turns, one terminal outcome, and no events after
the terminal result. This may reject otherwise legitimate Showdown
replays using newer or unmodeled protocol events; these remain out
of scope until researched and explicitly supported.

Held-out evaluation now rejects identical ordered public-event
sequences even where nicknames, player labels, chat and replay
IDs differ. This is conservative: two independent matches with
the same public sequence may also be rejected. Different
public sequences do not prove different human players, and
no cross-replay player identity is inferred or used.

Accuracy receipts reject NaN and invented hit fractions.
Empirical descriptive rates remain withheld below 20 eligible
events and do not imply calibrated future probabilities.

Replay IDs within the 5.1-5.3 records can link back to public
players. These records are minimized, NOT irreversibly
anonymous. Permission attestations are operator supplied and
must be verified outside the parser. The 5.4 aggregate report
contains no raw replay content or individual replay IDs.

Pinned source compatibility is not installed Minecraft runtime
verification. Offline synthetic testing does not require
Minecraft battles. Minecraft testing would only become
necessary after actual runtime integration.

Legacy is still live-authoritative, Shadow is observational,
and this branch is not merged to main.
