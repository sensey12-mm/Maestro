# Automatic Developer Battle Reports (MaestroAI 1.1.9)

**Install the mod JAR and play. No Python or after-battle command is required.**

On Minecraft startup, MaestroAI automatically creates a private report folder under
**the currently running Minecraft instance's game directory**:

```text
<minecraft-instance>/
  config/
    maestroai-battle-reports.properties
  battle-reports/
    issues.jsonl
    B01/
      summary.md
      battle.jsonl
    B02/
      summary.md
      battle.jsonl
```

For example, if the launcher uses `<instance-root>` as its game directory,
reports appear at `<instance-root>/battle-reports/`.
**The directory is NOT inside the mods folder, mod JAR, Git checkout or Headquarters.**

Reports are created automatically **when MaestroAI actually observes or
chooses in a battle**. The Java mod writes a minimized replay/provenance record
for each observed public history increment, a decision receipt for each
selected action, and a terminal record on Cobblemon's actual
`BATTLE_VICTORY` callback. The global `issues.jsonl` indexes diagnostics
and any detected gaps. The folder persists across Minecraft restarts;
previously created B numbers are never reused.

## Configuration

Auto reporting is **enabled by default**. The mod creates
`config/maestroai-battle-reports.properties` at startup:

```properties
enabled=true
```

Set `enabled=false` and restart Minecraft to disable automatic reports.
Alternatively launch with `-Dmaestroai.autoBattleReports=false` to override
the file. Other legacy observation JVM flags are **not required** to generate
these reports. The optional console diagnostics
(`-Dmaestroai.developerDiagnostics=true`,
`-Dmaestroai.envelopeCapture=true`,
`-Dmaestroai.legacyChoiceCapture=true`) are separate. With reporting enabled,
Java automatically gathers the safe portions of those receipt streams without
requiring their old log opt-ins. In v1.1.9, the selected-action receipt runs
whenever reporting is enabled, **even when all three optional JVM flags are
explicitly false**. The flags control optional console diagnostics, not the
reporter's decisions, replay projections, or terminal callbacks.

This is **local-only**: there is no uploader, external service, GUI or telemetry
transmission. Reporting never feeds a tactical decision.

## Contents and evidence limitations

- `summary.md`: active, completed-but-unverified, completed-with-gaps, or
  interrupted status; event/decision/issue counts; last seen turn; version,
  artifact hash, observed event-kind totals, recent decision chronology,
  source-computed evaluation excerpts, producer-family coverage and top issues.
  Event or legal-choice gaps are explicitly classified. A completed callback
  does not establish complete spectator delivery.
- `battle.jsonl`: typed JSONL records for `public_event`, `identity`,
  `provenance`, `decision`, `shadow_reason`, `legacy_runtime_authority`,
  `fallback`, `legacy_receipt`, `diagnostic_presence`, `issue`,
  `terminal`, `producer`, `legacy_evaluation` and `metadata`.
  They have a per-battle B identifier, monotonic order, installed-artifact SHA
  when available, and explicit `UNKNOWN` source offsets/hashes for live reports.
  Each decision retains the already-selected action, authority, forced-choice
  status, proposal and safe rationale/fallback codes. The `legal_evidence`
  field distinguishes `OWN_NATIVE_CERTIFIED` from `UNKNOWN`; it never
  fabricates a legal-move witness. When the own-lease turn is unavailable, a
  returned fallback action is retained with `turn=null` and
  `turn_evidence=UNKNOWN` instead of inventing a native turn or discarding
  the entire choice.
- `issues.jsonl`: append-only, global fixed-code issue index.

The report writer receives **only allowlisted, minimized output from the existing
public capture and decision receipts**. When a public move or identity field
is outside the reviewed vocabulary it stays `UNKNOWN`, with an
`UNKNOWN_PUBLIC_FIELD` issue, not a guess. Missing replay-envelope pairs are
reported as `ENVELOPE_SEQUENCE_MISMATCH` and marked incomplete. It rejects free-form or unrecognized
values. It does not write players' names, Pokémon nicknames, full UUIDs,
raw Cobblemon requests, split-private payloads, HP figures, arbitrary log
strings or filesystem paths. Known public species/move/effect projections
remain bounded by source-reviewed allowlists. Unrecognized values stay UNKNOWN.

A live capture is a **developer report, not a reviewed privacy-safe Battle
Evidence archive**. A victory callback proves only that Cobblemon delivered the
callback to MaestroAI, not who won or whether every packet reached an
independent spectator. Source beginning, spectator delivery, true concealed
Illusion identity, unreviewed evidence and unsupported details remain UNKNOWN
or UNVERIFIED. Diagnosed gaps are marked INCOMPLETE; hitting the bounded event
limit is an explicit issue.

Each active battle has a durable `active.marker`. On a clean shutdown,
unfinished battles become `INTERRUPTED_SHUTDOWN`. If Minecraft crashes,
on the next launch unfinished folders are marked `INTERRUPTED_RESTART`;
existing `battle.jsonl` is preserved, and a `UNCLEAN_RESTART` issue is
appended globally. If the Cobblemon terminal record was already durably written
before the crash, recovery instead marks `COMPLETED_CALLBACK_RECOVERED` and
does not invent an interruption. Reports are written incrementally with durable append
and atomic summary replacement. A battle retains at most 4,000 ordinary
JSONL records and 256 ordinary issue records, with explicit
`EVENT_LIMIT_REACHED` and `ISSUE_LIMIT_REACHED` signals on overflow;
a terminal callback still records when the event bound has been reached.
Duplicate safe decision receipts, replay events, and terminal callbacks are
not written twice. Historical reports are never overwritten. Report storage
I/O failures disable only the reporter, never action selection. A per-instance lock prevents concurrent
writers from overwriting evidence.

## Optional historical log conversion

`tools/battle_diagnostics.py` is retained for **older** `latest.log` files.
This is an optional offline migration/research tool, not needed for battles
recorded with MaestroAI 1.1.9 or newer.

```cmd
python tools\battle_diagnostics.py "path\to\older\logs\latest.log"
```

The Python tool writes separate derived reports to its selected output directory
and keeps its own schema, provenance and filtering rules. Do not mix its output
with the live Java writer's instance directory. Older v1.1.7 reports with
zero captured decisions cannot retroactively recover decision details that the
runtime did not record. They remain valid partial public-event evidence.

## Verification

`AutomaticBattleReportsRegression` runs under the registered standalone Java
regression suite and exercises default/disabled/invalid configuration, the
all-three-flags-OFF path through the real minimized projection and decision
record methods, duplicate suppression when flags are enabled, forced switches,
ordinary and forced fallbacks, missing evidence, terminal and gap handling,
privacy against malicious strings, bounded events and issues, restart recovery,
monotonic B directories, exclusive writer lock, safe writer failure isolation,
and normal shutdown interruption. GitHub Actions
also runs the existing independent native and pack-source gates.

**Installed Minecraft testing is still necessary** to confirm that Cobblemon
delivers the expected live terminal event in this modpack, that the current
instance path is correct, that all real choices reach the opt-out-independent
hooks, and that performance and completeness are acceptable. Passing CI is
not an assertion that Minecraft was launched or tested.

## v1.1.9 source-to-report coverage inventory

The following inventory is an **implementation scope and contract**, not an
assertion of installed Minecraft runtime acceptance or of unseen evidence.
All records carry an opaque local B-number capture, monotonically ordered
entry number and installed-artifact hash when source-attested. Native evidence
is never silently promoted to a spectator-delivery confirmation.

| Existing source producer | Local destination | Meaning / limitation |
| --- | --- | --- |
| Reporter-only `PublicBattleCapture.observeForReportsWithProvenance`, `PublicProtocolFilter.reportPublicEvents`, `PublicEnvelopeProvenance.reconcileForReports` | `public_event` + `provenance`, `identity`, `summary.md` timeline | Turn, move, switch, drag, form, ability, item, status, boosts, hazard, weather, terrain, activation, attack outcome, gimmick and terminal-recognized public kinds; direct or split-public origin, not private half or delivery ACK |
| `ShadowLiveDecisionEngine.Choice` (own-only/public conditional) | `decision.candidate_evaluations`, `candidate_count`, `shadow_reason` | Actual already-computed ranked heuristic scores/choice reasons from existing policy; **not** predicted damage, probability, or an authorized alternative |
| `ShadowPromotionPreparation.Report` | `decision.preparation_verdict`, `producer:SHADOW_PREPARATION` | Rejected/accepted conditional preparation verdict, distinct from final authority |
| `ShadowNativeLegalWindow.Proof`, `OwnRequestTransport.Lease` | `decision.native_legality_witness`, `own_request_witness`, `legal_choices`, `observed_root_alternatives`, `producer:NATIVE_LEGAL_ROOT` | Native source/lease freshness and independently witnessed own roots; private switch UUID becomes an own ordinal; an unsupported forced native window is not invented |
| `PublicLiveOpponentSnapshot.Capture`, `PublicOpponentIdentityContinuity` | `decision.public_opponent_witness`, `decision_source`, `producer:PUBLIC_OPPONENT_OBSERVATION`, `PUBLIC_IDENTITY_CONTINUITY` | Public-display observation versus hypothesis; never underlying hidden opponent identity |
| `LivePromotionGate` final response through `DeveloperBattleDecisionCapture` | `decision`, `fallback`, `legacy_runtime_authority`, decision chronology | Actual selected move/own slot; Shadow/Legacy authority; fallback reasons; conditional selected proposal; legal status and unknown-turn semantics; flag independent |
| `Legacy` move and switch evaluator, `ShadowDecisionComparator` | `legacy_evaluation`, `producer:LEGACY_EVALUATOR_MOVE`, `LEGACY_EVALUATOR_SWITCH`, `LEGACY_CROSS_ACTION`, `PHASE7_COMPARISON` | Existing (not re-computed) Legacy base/final score, candidate type, accuracy, priority, setup/pivot flags; internal uncalibrated estimate, never opponent truth |
| `NativeOwnActionEvidence`, `NativeOwnSwitchEvidence`, own-action fusion and cross-stream checks | `producer:NATIVE_OWN_ACTION`, `NATIVE_OWN_SWITCH`, `PHASE6_OWN_ACTION_FUSION`, `PHASE6_CROSS_STREAM`; `issue` | Source execution/availability classifications; status not fabricated when source is absent |
| Native sided/split dispatch, opponent-side/reveal, routing, custody/correlation, gate and runtime-acceptance observers | `producer:PHASE6_SIDED_DISPATCH`, `PHASE6_SPLIT_RETURN`, `PHASE6_NATIVE_SIDE`, `PHASE6_PUBLIC_REVEAL`, `PHASE6_SPLIT_ROUTING`, `PHASE6_RECIPIENT_CUSTODY`, `PHASE6_HISTORY_CORRELATION`, `PHASE6_NATIVE_GATE`, `PHASE6_RUNTIME_ACCEPTANCE`; `issue` | Emits typed source-lifecycle metadata **when those optional native observers actually run**; no private packet bytes, no delivery ACK, no false success when not run |
| Phase 5 online/historical read-only consumers and read-only shadow public-history bridge | `producer:PHASE5_ONLINE`, `PHASE5_HISTORICAL`, `SHADOW_PUBLIC_HISTORY` | Only existing observation metadata when independently enabled and provisioned; historical role records remain private and are not serialized |
| Phase 3 mechanics/prerequisite search and public-field source | `producer:PHASE3_PREREQUISITES`, `PUBLIC_FIELD_MECHANICS` | Observation-only count/status, hypotheses never treated as verified predictions |
| `ControlledBattleCaptureRegistry` victory callback, automatic writer lifespan | `terminal`, `metadata`, interrupted/restarted `summary.md`, `issues.jsonl` | Real Cobblemon callback versus interrupted/restarted/unknown beginning; preserves first safe report and replays global issues |

The `producer` record is deliberately a **typed existence/status/count
projection**, not a copy of the original optional console receipt's arbitrary
`toString()` output. Detailed ranking values are exported directly where
a bounded, safe typed source was inspected (the ordinary Shadow scorer and
the Legacy evaluator). Other optional native phase-specific internal
structures are not treated as certified values merely because their category
was observed.

### Interpretation of missing fields

| Classification | Meaning |
| --- | --- |
| `PUBLIC_WITNESSED` | Confirmed minimized public protocol kind/allowlisted value |
| `PUBLIC_DISPLAY_ONLY`, `PUBLIC_APPEARANCE_ONLY` | Revealed appearance; may not be true identity |
| `PUBLIC_HYPOTHESIS` or `LEGACY_INTERNAL_UNCALIBRATED` | Calculation/model estimate, not observed fact |
| `UNSUPPORTED_PUBLIC_CATALOG` | Public event exists but not safely recognized by installed source vocabulary |
| `PRIVACY_RESTRICTED` | Event kept but HP/name/private value intentionally omitted |
| `MISSING`, `UNAVAILABLE`, `SOURCE_UNVERIFIED` | Native/source receipt unavailable or unsupported; not fabricated |
| `UNKNOWN`, `UNVERIFIED` | No claimed certainty, e.g. entire spectator delivery, hidden opponent state, calibrated probabilities |

A battle with public activity and **zero** selected-action receipts now gets
`NO_DECISION_RECEIPT_OBSERVED` in `issues.jsonl`, clearly a visibility
warning rather than an invented action. Gaps, malformed records, unrecognized
fields, record limits, storage failures, unfinished/crashed sessions, and
terminal callback errors are not concealed.

### Representative synthetic report excerpt

The excerpts below describe the *registered Java regression fixtures*, not
a claim that Minecraft gameplay has been rerun for v1.1.9:

```text
# MaestroAI battle B01
- Status: ACTIVE_INCOMPLETE
- Public events: 1
- Decisions: 1
- Last observed turn: 3

## Maestro choices (last 14)

- T3 SHADOW move:quickattack; reason=MOVE; source=OWN_ONLY; candidates=2;
  preparation=INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE; fallback=UNKNOWN

## Observed public timeline

- turn: 1
```

```json
{"kind":"decision","authority":"SHADOW","selected_action":"move:quickattack","rationale":"MOVE","decision_source":"OWN_ONLY","candidate_count":2,"preparation_verdict":"INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE","own_request_witness":"VERIFIED_OWN_REQUEST","public_opponent_witness":"UNVERIFIED","candidate_evaluations":[{"action":"move:tackle","category":"MOVE","heuristic_score":40.0,"score_kind":"SOURCE_HEURISTIC_NOT_WIN_PROBABILITY"}]}
{"kind":"public_event","event":"-damage","detail":"REDACTED_HP","detail_state":"PRIVACY_RESTRICTED"}
{"kind":"producer","producer":"PHASE6_NATIVE_GATE","state":"OBSERVED","evidence_class":"SOURCE_TYPED_OBSERVATION"}
```

These are **shortened explanatory excerpts, not byte-for-byte actual output**.
The regression compares genuine Java-produced `battle.jsonl` and
`summary.md` against expected source values. Real installed Minecraft
acceptance requires starting the updated JAR, battling Maestro with all
optional diagnostic flags OFF, and reviewing the new local reports. No
Minecraft session has been performed by the developer for this batch.

### Full generated regression sample

The `verify` GitHub Actions job now uploads an additional
`maestroai-diagnostics-sample-<exact-commit>` artifact containing a **complete
and unmodified Java-writer-generated** `B01/summary.md`, `B01/battle.jsonl`,
`issues.jsonl`, and a README stating its synthetic provenance. Unlike the
shortened excerpts above, these files are generated by the registered Java
regression and tested for decision explanations, source-scored alternatives,
public event details, native witness categories, terminal evidence and issue
classification. The artifact is only accepted as a technical fixture, not
installed Minecraft testing. All fields are synthetic public protocol or
fixed enum values; no player or private opponent content is exported.

### Boundaries

The reporter deliberately excludes private `sideupdate`, packet contents,
names, UUIDs, raw precise HP, hidden opponent moves/identity, raw full
`latest.log`, other mods' logs, and any unproven spectator ACK. These remain
in separate local Minecraft logs or the original native runtime, not in
public Git history or the structured safe archive. Historical Python
conversion is optional and is never required to capture new battles.

### Isolation safeguard

The original `PublicBattleCapture.observeWithProvenance`, native tactical
`PublicProtocolFilter.nativePublicEvents` and
`PublicEnvelopeProvenance.reconcileNative` are **unchanged as tactical
inputs**. The expanded public kinds (including miss/critical-hit and similar
announcements) are handled by a second report-only capture cursor with its
own incremental sequence. The same opaque battle capture ID correlates the
writer records, but no report-owned projection or score is read by an action
chooser. Regression coverage explicitly checks that the old native and offline
filters remain frozen while the reporter preserves additional public events.
