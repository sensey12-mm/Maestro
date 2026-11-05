package com.maestroai.training;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Synthetic filesystem/privacy/runtime-hook regressions; not Minecraft testing. */
public final class AutomaticBattleReportsRegression {
    private static int checks;
    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError("Automatic battle reports: " + why);
        checks++;
    }
    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
    private static final String REPLAY1 = "[MaestroAI] SHADOW_REPLAY battle=REDACTED "
            + "capture=4 seq=1 event=|switch|p1a|mew";
    private static final String ENVELOPE1 = "[MaestroAI] SHADOW_ENVELOPE "
            + "capture=4 seq=1 route=DIRECT_UPDATE split_target=NONE "
            + "spectator=EXPECTED_NOT_OBSERVED branch_attested=true";
    private static final String TERMINAL4 = "[MaestroAI] SHADOW_TERMINAL capture=4 "
            + "after=1 event=COBBLEMON_BATTLE_VICTORY winner=REDACTED "
            + "beginning=UNKNOWN delivery=UNVERIFIED coverage=START_UNKNOWN";

    public static void run() throws Exception {
        Path tmp = Files.createTempDirectory("maestro-auto-report-regression-");
        try {
            Path config = tmp.resolve("config").resolve("maestroai-battle-reports.properties");
            check(AutomaticBattleReports.readEnabled(config), "default config enables reports");
            check(read(config).contains("enabled=true"), "default config is human-editable");
            Files.writeString(config,"enabled=false\n",StandardCharsets.UTF_8);
            check(!AutomaticBattleReports.readEnabled(config), "disabled config is respected");
            Files.writeString(config,"enabled=maybe\n",StandardCharsets.UTF_8);
            boolean rejected=false;
            try { AutomaticBattleReports.readEnabled(config); }
            catch(IOException expected){rejected=true;}
            check(rejected,"invalid config cannot silently enable reports");

            Path root=tmp.resolve("case1");
            try (var writer=AutomaticBattleReports.synthetic(root,"1.1.7")) {
                writer.testLines(4,List.of(REPLAY1,ENVELOPE1));
                writer.testLines(4,List.of(REPLAY1,ENVELOPE1)); // duplicates are idempotent
                String decision=DeveloperBattleDecisionCapture.safeLine(
                        4,1,false,"SHADOW","move:icebeam","move:uturn",
                        "MOVE","UNKNOWN","UNKNOWN",List.of("move:icebeam","move:uturn"));
                writer.testDecision(4,decision);
                // Adversarial input: public kind and detail must be allowlisted.
                writer.testLines(4,List.of("[MaestroAI] SHADOW_REPLAY battle=REDACTED "
                        + "capture=4 seq=2 event=|chat|p1a|PRIVATE_CHAT_SECRET"));
                writer.testLines(4,List.of("[MaestroAI] SHADOW_REPLAY_GAP "
                        + "battle=REDACTED capture=4 after=2"));
                writer.testTerminal(4,List.of(TERMINAL4));
                Path battle=root.resolve("battle-reports/B01");
                String json=read(battle.resolve("battle.jsonl"));
                String summary=read(battle.resolve("summary.md"));
                check(json.contains("\"kind\":\"public_event\"") && json.contains("\"detail\":\"mew\""),
                        "public event materialized automatically from minimized source");
                check(json.contains("\"kind\":\"decision\"")
                        && json.contains("\"selected_action\":\"move:icebeam\"")
                        && json.contains("\"kind\":\"shadow_reason\""),
                        "native selection and Shadow rationale recorded automatically");
                check(json.contains("\"kind\":\"provenance\"")
                        && json.contains("\"spectator_delivery\":\"UNVERIFIED\""),
                        "source envelope and uncertainty retained");
                check(json.contains("\"kind\":\"terminal\"")
                        && json.contains("\"kind\":\"metadata\"")
                        && json.contains("\"kind\":\"diagnostic_presence\"")
                        && summary.contains("COMPLETED_WITH_GAPS"),
                        "real callback terminal retains detected incompleteness");
                check(!Files.exists(battle.resolve("active.marker")),
                        "terminal clears crash marker");
                check(json.contains("\"kind\":\"issue\"")
                        && read(root.resolve("battle-reports/issues.jsonl"))
                                .contains("PUBLIC_HISTORY_GAP"),
                        "issues exist in battle and global index");
                check(!json.contains("PRIVATE_CHAT_SECRET")
                        && !json.contains("uuid") && !json.contains("100/100")
                        && !json.contains("C:\\Users"),
                        "privacy refuses free-form content, paths, HP and identity");
                check(json.split("\"kind\":\"public_event\"", -1).length == 3,
                        "duplicate replay retained once and novel unknown event recorded once");
            }
            // Simulate crash in the narrow window after terminal fsync but
            // before the active marker could be deleted.
            Path completeFolder=root.resolve("battle-reports/B01");
            Files.writeString(completeFolder.resolve("active.marker"),"ACTIVE\\n",
                    StandardCharsets.UTF_8);
            try (var completedRecovery=AutomaticBattleReports.synthetic(root,"1.1.7")) {
                check(read(completeFolder.resolve("summary.md"))
                        .contains("COMPLETED_CALLBACK_RECOVERED"),
                        "durable victory terminal survives interrupted marker cleanup");
                check(!Files.exists(completeFolder.resolve("active.marker")),
                        "recovery clears marker after persisted terminal");
                check(!read(root.resolve("battle-reports/issues.jsonl"))
                        .contains("UNCLEAN_RESTART"),
                        "recovered terminal is not falsely classified as crash loss");
            }
            // Crash: unfinished marker remains and next launch labels it conservatively.
            Path crashRoot=tmp.resolve("case2");
            var first=AutomaticBattleReports.synthetic(crashRoot,"1.1.7");
            first.testLines(9,List.of(REPLAY1.replace("capture=4","capture=9")));
            check(Files.exists(crashRoot.resolve("battle-reports/B01/active.marker")),
                    "active marker persisted while game is live");
            first.simulateCrashForTest();
            try(var second=AutomaticBattleReports.synthetic(crashRoot,"1.1.7")){
                check(read(crashRoot.resolve("battle-reports/B01/summary.md"))
                        .contains("INTERRUPTED_RESTART"),"restart recovers incomplete battle");
                check(!Files.exists(crashRoot.resolve("battle-reports/B01/active.marker")),
                        "recovery removes orphan marker");
                check(read(crashRoot.resolve("battle-reports/issues.jsonl"))
                        .contains("UNCLEAN_RESTART"),"global issue indexes crashed session");
                second.testDecision(10,DeveloperBattleDecisionCapture.safeLine(
                        10,2,true,"LEGACY","switch:slot2","UNKNOWN",
                        "UNKNOWN","NO_NATIVE_FORCED_PROPOSAL","NO_OWN_LEASE",null));
                check(Files.exists(crashRoot.resolve("battle-reports/B02/battle.jsonl")),
                        "new session never overwrites recovered battle");
                check(read(crashRoot.resolve("battle-reports/B02/battle.jsonl"))
                        .contains("\"kind\":\"legacy_runtime_authority\""),
                        "Legacy return authority retains explicit independent classification");
                boolean locked=false;
                try(var another=AutomaticBattleReports.synthetic(crashRoot,"1.1.7")){
                    throw new AssertionError("concurrent writer unexpectedly acquired lock");
                }catch(IOException expected){locked=true;}
                check(locked,"second writer prohibited from corrupting report root");
            }
            check(read(crashRoot.resolve("battle-reports/B02/summary.md"))
                    .contains("INTERRUPTED_SHUTDOWN"),"normal shutdown marks unfinished battles");
            Path privacyRoot=tmp.resolve("privacy");
            try(var safe=AutomaticBattleReports.synthetic(privacyRoot,"1.1.7")){
                String malicious=DeveloperBattleDecisionCapture.safeLine(
                        1,1,false,"SHADOW","move:PRIVATE_SECRET","UNKNOWN",
                        "INVALID","UNKNOWN","UNKNOWN",null);
                safe.testDecision(1,malicious);
                safe.testLines(1,List.of("[MaestroAI] SHADOW_REPLAY battle=REDACTED "
                        + "capture=1 seq=1 event=|-transform|p2a|p1a"));
                check(read(privacyRoot.resolve("battle-reports/B01/battle.jsonl"))
                        .contains("\"kind\":\"identity\"")
                        && read(privacyRoot.resolve("battle-reports/B01/battle.jsonl"))
                        .contains("\"true_identity\":\"UNKNOWN\""),
                        "transform stays appearance-only, never true concealed identity");
                check(!read(privacyRoot.resolve("battle-reports/B01/battle.jsonl"))
                        .contains("PRIVATE_SECRET"),"selected action gate prevents private data");
            }
            // Acceptance regression: use the real public projector, the real
            // developer receipt entry point and the real terminal handler with
            // every optional console flag explicitly OFF.
            String[] flags = {"maestroai.developerDiagnostics",
                    "maestroai.envelopeCapture","maestroai.legacyChoiceCapture"};
            java.util.Map<String,String> previous = new java.util.HashMap<>();
            for (String flag : flags) {
                previous.put(flag,System.getProperty(flag));
                System.setProperty(flag,"false");
            }
            try {
                String source = read(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
                check(source.contains("if(DeveloperBattleDecisionCapture.captureEnabled() && battle!=null"),
                        "real chooser hook must be reachable with reporter enabled and console OFF");
                check(!DeveloperBattleDecisionCapture.enabled()
                        && !DeveloperBattleDecisionCapture.captureEnabled(),
                        "console OFF without reporter does not generate receipts");
                Path live = tmp.resolve("no-flags");
                var auto = AutomaticBattleReports.attachSyntheticForTest(live,"1.1.8");
                try {
                    check(DeveloperBattleDecisionCapture.captureEnabled()
                            && !DeveloperBattleDecisionCapture.enabled()
                            && ControlledBattleCaptureRegistry.enabled()
                            && !ControlledBattleCaptureRegistry.envelopeEnabled(),
                            "writer enables decisions and terminal callback with three console flags false");
                    var id = java.util.UUID.fromString("20000000-0000-0000-0000-000000000099");
                    var capture = new PublicBattleCapture();
                    long number = capture.forBattle(id);
                    List<String> history = List.of("update\n"
                            + "|switch|p1a: SECRET_OWN_NICK|Mew, L100|100/100\n"
                            + "|switch|p2a: SECRET_FOE_NICK|Zoroark, L100|100/100\n"
                            + "|turn|1");
                    var projection = capture.observeWithProvenance(id,history);
                    List<String> safe = java.util.stream.Stream.concat(
                            projection.replay().stream(),projection.envelopes().stream()).toList();
                    check(safe.size() == 6 && number > 0,
                            "public source produces attested replay and metadata");
                    AutomaticBattleReports.recordLines(number,safe);
                    DeveloperBattleDecisionCapture.record(number,1,false,"SHADOW",
                            "move:icebeam","move:uturn","MOVE","UNKNOWN","UNKNOWN",
                            List.of("move:icebeam","move:uturn"));
                    DeveloperBattleDecisionCapture.record(number,2,true,"LEGACY",
                            "switch:slot2","UNKNOWN","UNKNOWN","NO_NATIVE_FORCED_PROPOSAL",
                            "NO_OWN_LEASE",null);
                    DeveloperBattleDecisionCapture.record(number,3,false,"LEGACY",
                            "move:ironhead","UNKNOWN","UNKNOWN","NO_INDEPENDENT_PROPOSAL",
                            "UNKNOWN",null);
                    String legacy = "[MaestroAI] LEGACY_CHOICE_RECEIPT "
                            + "schema=phase7.3.v1 capture=REDACTED "
                            + "origin=LEGACY_CHOOSE_RETURN turn=3 action=move:ironhead "
                            + "artifact_sha256=UNKNOWN spectator_delivery=UNVERIFIED "
                            + "custody=REVIEW_REQUIRED";
                    AutomaticBattleReports.recordLegacy(number,legacy);
                    // Console flags may change stdout, never record identity.
                    // The same source receipt and replay must not be doubled.
                    for(String flag:flags) System.setProperty(flag,"true");
                    DeveloperBattleDecisionCapture.record(number,1,false,"SHADOW",
                            "move:icebeam","move:uturn","MOVE","UNKNOWN","UNKNOWN",
                            List.of("move:icebeam","move:uturn"));
                    AutomaticBattleReports.recordLines(number,safe);
                    AutomaticBattleReports.recordLegacy(number,legacy);
                    ControlledBattleCaptureRegistry.finishObserved(id,capture,history);
                    ControlledBattleCaptureRegistry.finishObserved(id,capture,history);
                    Path b=live.resolve("battle-reports/B01");
                    String content=read(b.resolve("battle.jsonl"));
                    String overview=read(b.resolve("summary.md"));
                    check(content.split("\"kind\":\"decision\"",-1).length == 4,
                            "three actual unique selections, never duplicate despite flags");
                    check(content.split("\"kind\":\"public_event\"",-1).length == 4
                            && content.split("\"kind\":\"provenance\"",-1).length == 4
                            && content.split("\"kind\":\"legacy_receipt\"",-1).length == 2,
                            "events, envelopes and optional Legacy receipts recorded once");
                    check(content.contains("\"selected_action\":\"switch:slot2\"")
                            && content.contains("\"forced\":true")
                            && content.contains("\"fallback_stage\":\"NO_NATIVE_FORCED_PROPOSAL\""),
                            "forced replacement and exact fallback authority retained");
                    check(content.contains("\"fallback_stage\":\"NO_INDEPENDENT_PROPOSAL\"")
                            && content.contains("\"legal_evidence\":\"OWN_NATIVE_CERTIFIED\"")
                            && content.contains("\"legal_evidence\":\"UNKNOWN\""),
                            "ordinary fallback and independently proven versus missing legal evidence distinguished");
                    check(content.contains("\"kind\":\"terminal\"")
                            && content.contains("\"kind\":\"metadata\"")
                            && overview.contains("COMPLETED_UNVERIFIED")
                            && overview.contains("Decisions: 3"),
                            "actual terminal handler closes automatic report without asserting completeness");
                    check(!content.contains("SECRET_OWN_NICK")
                            && !content.contains("SECRET_FOE_NICK")
                            && !content.contains("100/100")
                            && !content.contains(id.toString()),
                            "real source projection never exports player-visible private identifiers");
                    check(!Files.exists(live.resolve("battle-reports/B02")),
                            "terminal and console retries do not create phantom battles");
                } finally {
                    AutomaticBattleReports.detachSyntheticForTest(auto);
                }
                // Deliberately malformed and mismatched evidence is an issue,
                // not forged selected action or a public statement.
                Path bad = tmp.resolve("malformed");
                try(var malformed=AutomaticBattleReports.synthetic(bad,"1.1.8")) {
                    malformed.testDecision(22,
                            "[MaestroAI] DEV_DIAG_REJECTED code=INVALID_SOURCE");
                    malformed.testLines(22,List.of(
                            "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=22 seq=1 "
                            + "event=|move|p2a|SECRET_LEAK"));
                    String issues=read(bad.resolve("battle-reports/issues.jsonl"));
                    check(issues.contains("INVALID_DECISION_RECEIPT")
                            && issues.contains("UNKNOWN_PUBLIC_FIELD"),
                            "malformed decisions and unreviewed public fields are explicitly classified");
                    check(read(bad.resolve("battle-reports/B01/battle.jsonl"))
                            .contains("\"detail\":\"UNKNOWN\"")
                            && !read(bad.resolve("battle-reports/B01/battle.jsonl"))
                                    .contains("SECRET_LEAK"),
                            "unrecognized public move is UNKNOWN, never exported raw");
                }
                Path missingEnvelope=tmp.resolve("unverified");
                try(var incomplete=AutomaticBattleReports.synthetic(missingEnvelope,"1.1.8")) {
                    incomplete.testLines(7,List.of(
                            "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=7 "
                            + "seq=1 event=|turn|1"));
                    incomplete.testTerminal(7,List.of(
                            TERMINAL4.replace("capture=4","capture=7")));
                    check(read(missingEnvelope.resolve("battle-reports/issues.jsonl"))
                                .contains("ENVELOPE_SEQUENCE_MISMATCH")
                            && read(missingEnvelope.resolve("battle-reports/B01/summary.md"))
                                .contains("COMPLETED_WITH_GAPS"),
                            "missing public envelope evidence cannot be fabricated at terminal");
                }
                // Event and issue retention bounds must emit explicit reasons.
                Path caps=tmp.resolve("limits");
                try(var limited=AutomaticBattleReports.synthetic(caps,"1.1.8")) {
                    for(int i=0;i<300;i++)
                        limited.testLines(30,List.of(
                                "[MaestroAI] SHADOW_ENVELOPE_FAILURE capture=30 "
                                + "reason=INCOMPLETE_SPLIT status=UNKNOWN"));
                    String issues=read(caps.resolve("battle-reports/issues.jsonl"));
                    check(issues.contains("ISSUE_LIMIT_REACHED")
                            && issues.split("\n",-1).length <= 260,
                            "malformed flood produces bounded issue index with explicit truncation");
                    for(int start=1;start<=4200;start+=100){
                        List<String> events=new java.util.ArrayList<>();
                        for(int seq=start;seq<start+100;seq++)
                            events.add("[MaestroAI] SHADOW_REPLAY battle=REDACTED "
                                    + "capture=31 seq="+seq+" event=|turn|1");
                        limited.testLines(31,events);
                    }
                    check(read(caps.resolve("battle-reports/issues.jsonl"))
                            .contains("EVENT_LIMIT_REACHED")
                            && read(caps.resolve("battle-reports/B02/summary.md"))
                            .contains("Completeness: INCOMPLETE"),
                            "bounded event overflow is detected, never silently claimed complete");
                }
                // Storage failures cannot escape the receipt's return path.
                Path blocked=tmp.resolve("storage-failure");
                var failing=AutomaticBattleReports.attachSyntheticForTest(blocked,"1.1.8");
                try {
                    DeveloperBattleDecisionCapture.record(41,1,false,"SHADOW",
                            "move:icebeam","move:uturn","MOVE","UNKNOWN","UNKNOWN",null);
                    Path eventFile=blocked.resolve("battle-reports/B01/battle.jsonl");
                    Files.delete(eventFile);
                    Files.createDirectory(eventFile);
                    DeveloperBattleDecisionCapture.record(41,2,true,"LEGACY",
                            "switch:slot2","UNKNOWN","UNKNOWN",
                            "NO_NATIVE_FORCED_PROPOSAL","UNKNOWN",null);
                    check(!AutomaticBattleReports.enabled(),
                            "fatal report I/O disables writer without throwing into selected response");
                } finally {
                    AutomaticBattleReports.detachSyntheticForTest(failing);
                }
            } finally {
                for(String flag:flags) {
                    String value=previous.get(flag);
                    if(value==null) System.clearProperty(flag);
                    else System.setProperty(flag,value);
                }
            }
            System.out.println("AutomaticBattleReportsRegression PASS checks="+checks);
        } finally {
            // All paths are owned by this synthetic temp directory.
            try(var walk=Files.walk(tmp)){
                for(Path path:walk.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
