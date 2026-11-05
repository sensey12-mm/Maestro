# Automatic Developer Battle Reports (MaestroAI 1.1.7)

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
requiring their old log opt-ins.

This is **local-only**: there is no uploader, external service, GUI or telemetry
transmission. Reporting never feeds a tactical decision.

## Contents and evidence limitations

- `summary.md`: active, completed-but-unverified, completed-with-gaps, or
  interrupted status; event/decision/issue counts; last seen turn; version.
- `battle.jsonl`: typed JSONL records for `public_event`, `identity`,
  `provenance`, `decision`, `shadow_reason`, `legacy_runtime_authority`,
  `fallback`, `legacy_receipt`, `diagnostic_presence`, `issue`,
  `terminal` and `metadata`.
  They have a per-battle B identifier, monotonic order, installed-artifact SHA
  when available, and explicit `UNKNOWN` source offsets/hashes for live reports.
- `issues.jsonl`: append-only, global fixed-code issue index.

The report writer receives **only allowlisted, minimized output from the existing
public capture and decision receipts**. It rejects free-form or unrecognized
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
and atomic summary replacement. Report storage I/O failures disable only
the reporter, never action selection. A per-instance lock prevents concurrent
writers from overwriting evidence.

## Optional historical log conversion

`tools/battle_diagnostics.py` is retained for **older** `latest.log` files.
This is an optional offline migration/research tool, not needed for battles
recorded with MaestroAI 1.1.7 or newer.

```cmd
python tools\battle_diagnostics.py "path\to\older\logs\latest.log"
```

The Python tool writes separate derived reports to its selected output directory
and keeps its own schema, provenance and filtering rules. Do not mix its output
with the live Java writer's instance directory.

## Verification

`AutomaticBattleReportsRegression` runs under the registered standalone Java
regression suite and exercises default/disabled/invalid configuration, real
safe receipt formats, duplicate suppression, terminal and gap handling,
privacy against malicious strings, restart recovery, monotonic B directories,
exclusive writer lock, and normal shutdown interruption. GitHub Actions
also runs the existing independent native and pack-source gates.

**Installed Minecraft testing is still necessary** to confirm that Cobblemon
delivers the expected live terminal event in this modpack, that the current
instance path is correct, that all real choices reach the opt-out-independent
hooks, and that performance and completeness are acceptable. Passing CI is
not an assertion that Minecraft was launched or tested.
