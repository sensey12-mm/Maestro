# Phase 6.26 — Real Public-History Grammar Reconciliation

**Prepared against:** Maestro-6, Phase 6.25 exact source head `e3f1428c` · **Proposed version:** 1.0.71.

**Prerequisite:** Do not advance the public branch until Phase 6.25 exact-commit full GitHub Actions completes successfully. Build/standalone regressions and the pinned Cobblemon Mixin ABI step succeeded on Phase 6.25; the pinned simulator gate was not yet completed when this patch was prepared. If CI fails, diagnose and repair the prerequisite first.

## Provenance: independently grounded source and existing private evidence

Cobblemon 1.7.3's pinned `ShowdownInterpreter.interpretMessage` appends every accepted native message to `PokemonBattle.showdownMessages`. `ShowdownInterpreter.interpret` explicitly dispatches **both** `update` (public/protocol messages) and `sideupdate` (actor-specific `request` or `error`) frames. The private sideupdate grammar uses a side ID followed by a message body, which is not a public protocol event.

Public upstream references:
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/ShowdownInterpreter.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/RequestInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/AbilityInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/ActivateInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/SideStartInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/BoostInstruction.kt
- https://github.com/MayIHaveK/cobblemon-1.7.3/blob/master/common/src/main/kotlin/com/cobblemon/mod/common/battles/interpreter/instructions/EndInstruction.kt

In owner-authorized local 1.0.69 runtime evidence, Phase 6.16 failed closed on a battle-opening history whose independently filtered public stream was source-framed into two split-public entries plus three direct-public entries. The installed runtime's public ability mode included the exact final token `boost`; the strict Phase 6.23 parser accepted only bracketed public annotations. The same native history contained extra chunks not presented as public events, consistent with the verified native sideupdate path. **The specific raw contents of those extra native chunks were not directly available**, so their identity remains a source-grounded candidate, not a runtime-confirmed fact. Private raw logs and player identifiers are not copied here.

Later runtime public events contained `-activate`, `-sidestart`, `-unboost` and `-end` kinds that Phase 6.16 did not recognize, despite these already being allowlisted by the independent `PublicProtocolFilter` and `PublicEnvelopeProvenance`. This mismatch would cause later withdrawals even after a corrected opening.

## Deliberately constrained grammar changes

1. **Private native sideupdate frame:** Only exact three-line `sideupdate` / `p1` or `p2` / `|request|` (JSON-looking object or `null`) or `|error|` (nonempty) can be silently ignored by the public parser. No request contents are JSON-decoded, parsed, retained, displayed, forwarded, compared as public content, or made into a routing proof. Every such raw frame is still included in the existing **salted append-only history digest**, so rewriting it revokes the session. Oversized/malformed/multi-line/nested/unknown frame shapes fail closed.
2. **Public ability mode:** Only the exact single untagged `boost` suffix is recognized, exclusively for `-ability`. Arbitrary untagged modes and combinations with additional annotations remain invalid.
3. **Four verified public event kinds:** `-activate`, `-sidestart`, `-unboost` and `-end` enter Phase 6.16's already-independent public allowlist with narrow actor/side/condition/boost/annotation shape checks. The private/public split route grammar, order, target comparisons and private-half validation are unchanged.
4. **No authority changes:** This is structural public-history grammar only. It does not grant public-source lifetime authentication, native packet callback, recipient ACK, client delivery, Showdown event identity, complete tactics, Shadow recommendations or live promotion. Phase 6.18 split RETURN remains a separate, unobserved method. Phase 6.24 sided RETURN cannot substitute for it.

## Adversarial and positive synthetic regression criteria

- A seven-chunk mixed-history opening has two private `sideupdate` requests, two public/private switch pairs, direct ability-with-boost, direct boost and turn one. Only five **public** events appear through **both** independent public consumers in exactly the same order and route.
- Source-owned revision advances for valid newly supported `-activate`, `-sidestart`, `-unboost`, `-end`, while remaining completely non-authoritative.
- Any revision to a previously accepted private request revokes salted append-only custody.
- Malformed side recipient, private request in a public update, fake public move inside private sideupdate, nested public frame, missing private request/error, illegal ability extra mode, malformed effect, unsupported side, bad unboost level and unknown annotation terminate verification.
- A synthetic private JSON secret does not appear in `Report.toString()` or independent public events; no owner log bytes are committed.

## Phase 6 final integration readiness: explicit GO/NO-GO boundaries

**Can be accepted via public exact-commit CI:** grammar structure, synthetic preservation of privacy, replay source order, source-pin checks, regression coverage, and read-only Shadow separation—only if every full workflow step passes.

**Cannot be claimed from CI:** runtime application of native Mixins, empirical coverage of all Cobblemon-specific protocol types, recipient transmission/delivery, complete installed legal options, public-source event identity, Minecraft compatibility of version 1.0.71, Shadow tactical recommendation readiness, or Legacy parity.

**Targeted runtime acceptance (not requested in this source-only batch):** On an authorized installed exact-commit JAR, confirm a fresh Phase 6.16 report progresses through actual native opening into ordinary turn history, instead of terminal failure. Any remaining unsupported source type must be individually source-verified and tested, never auto-allowed. Even a structural Phase 6.16 PASS does not promote Phase 6.18/6.20/6.21 or Shadow.

**Governance:** no Headquarters CI, no raw owner logs or personal identifiers in public fixtures, no `main` merge, no changes to Legacy choice authority or trainer configuration. The real runtime evidence remains privately held.
