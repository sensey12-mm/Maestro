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
                    DeveloperBattleDecisionCapture.record(number,-1,true,"LEGACY",
                            "switch:slot3","UNKNOWN","UNKNOWN","NO_OWN_LEASE",
                            "NO_OWN_LEASE",null);
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
                    check(content.split("\"kind\":\"decision\"",-1).length == 5,
                            "four actual unique selections, never duplicate despite flags");
                    check(content.split("\"kind\":\"public_event\"",-1).length == 4
                            && content.split("\"kind\":\"provenance\"",-1).length == 4
                            && content.split("\"kind\":\"legacy_receipt\"",-1).length == 2,
                            "events, envelopes and optional Legacy receipts recorded once");
                    check(content.contains("\"selected_action\":\"switch:slot2\"")
                            && content.contains("\"forced\":true")
                            && content.contains("\"fallback_stage\":\"NO_NATIVE_FORCED_PROPOSAL\""),
                            "forced replacement and exact fallback authority retained");
                    check(content.contains("\"fallback_stage\":\"NO_INDEPENDENT_PROPOSAL\"")
                            && content.contains("\"selected_action\":\"switch:slot3\"")
                            && content.contains("\"turn\":null,\"turn_evidence\":\"UNKNOWN\"")
                            && content.contains("\"legal_evidence\":\"OWN_NATIVE_CERTIFIED\"")
                            && content.contains("\"legal_evidence\":\"UNKNOWN\""),
                            "ordinary fallback and independently proven versus missing legal evidence distinguished");
                    check(content.contains("\"kind\":\"terminal\"")
                            && content.contains("\"kind\":\"metadata\"")
                            && overview.contains("COMPLETED_UNVERIFIED")
                            && overview.contains("Decisions: 4"),
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
            // Final diagnostic-coverage acceptance: use the exact live
            // Shadow heuristic source (no re-scoring or invented odds).
            var self = new BattleSnapshot.Combatant("self",.85,List.of("normal"),
                    java.util.Map.of(),List.of("tackle","quickattack"),
                    null,null,false);
            var foe = new BattleSnapshot.Combatant("UNKNOWN",
                    BattleSnapshot.UNKNOWN_HP_FRACTION,List.of(),
                    java.util.Map.of(),List.of(),null,null,false);
            var position=new BattleSnapshot(3,self,foe,List.of(),List.of(),
                    List.of("tackle","quickattack"),List.of(),java.util.Map.of(),false,null);
            var chosen=ShadowLiveDecisionEngine.ordinary(position,false,false,true);
            check(chosen!=null && chosen.evaluations().size()==chosen.scoredCandidates()
                    && chosen.evaluations().stream()
                            .allMatch(e->Double.isFinite(e.heuristicScore())),
                    "independent Shadow scorer retains its ACTUAL source-computed rankings");
            var details=new DeveloperBattleDecisionCapture.Evidence(
                    DeveloperBattleDecisionCapture.Source.OWN_ONLY,
                    ShadowPromotionPreparation.Verdict.INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE,
                    chosen.scoredCandidates(),
                    DeveloperBattleDecisionCapture.safeEstimates(chosen),
                    List.of("move:tackle","move:quickattack"),
                    DeveloperBattleDecisionCapture.Witness.VERIFIED_OWN_REQUEST,
                    DeveloperBattleDecisionCapture.Witness.VERIFIED_OWN_REQUEST,
                    DeveloperBattleDecisionCapture.Witness.UNVERIFIED);
            Path detailedRoot=tmp.resolve("detailed");
            try(var detailed=AutomaticBattleReports.synthetic(detailedRoot,"1.1.9")) {
                String choice=DeveloperBattleDecisionCapture.safeLine(77,3,false,"SHADOW",
                        chosen.action(),chosen.action(),chosen.reason().name(),
                        "UNKNOWN","UNKNOWN",List.of("move:tackle","move:quickattack"));
                detailed.testDecisionWithEvidence(77,choice,details);
                detailed.testDecisionWithEvidence(77,choice,details);
                detailed.testLines(77,List.of(
                        "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=77 seq=1 event=|turn|3",
                        "[MaestroAI] SHADOW_ENVELOPE capture=77 seq=1 route=DIRECT_UPDATE "
                                + "split_target=NONE spectator=EXPECTED_NOT_OBSERVED branch_attested=true"));
                String json=read(detailedRoot.resolve("battle-reports/B01/battle.jsonl"));
                String overview=read(detailedRoot.resolve("battle-reports/B01/summary.md"));
                check(json.contains("\"decision_source\":\"OWN_ONLY\"")
                        && json.contains("\"preparation_verdict\":\"INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE\"")
                        && json.contains("\"candidate_count\":2")
                        && json.contains("\"candidate_evaluations\":[")
                        && json.contains("\"heuristic_score\":")
                        && json.contains("\"score_kind\":\"SOURCE_HEURISTIC_NOT_WIN_PROBABILITY\""),
                        "source-calculated Shadow alternatives, score semantics and verdict preserved");
                check(json.contains("\"own_request_witness\":\"VERIFIED_OWN_REQUEST\"")
                        && json.contains("\"public_opponent_witness\":\"UNVERIFIED\"")
                        && json.contains("\"observed_root_alternatives\":[\"move:tackle\",\"move:quickattack\"]"),
                        "native witness and independently observed alternatives retained");
                check(json.split("\"kind\":\"decision\"",-1).length==2,
                        "source evidence does not duplicate the same chooser return");
                check(overview.contains("Maestro choices") && overview.contains("source=OWN_ONLY")
                        && overview.contains("Decisions: 1"),
                        "useful chronological summary retains reason and source");
                detailed.testInterrupt(77,"REPLACED");
                check(read(detailedRoot.resolve("battle-reports/B01/summary.md"))
                        .contains("INTERRUPTED_REPLACED"),
                        "interruption preserves meaningful diagnostics");
            }

            // Native-only public protocol kinds must reach the report despite
            // the frozen, narrower OFFLINE Phase 5 replay allowlist.
            Path nativeRoot=tmp.resolve("native-public");
            try(var nativeWriter=AutomaticBattleReports.synthetic(nativeRoot,"1.1.9")) {
                var battleId=java.util.UUID.fromString("20000000-0000-0000-0000-000000000444");
                var publicCapture=new PublicBattleCapture();
                long id=publicCapture.forBattle(battleId);
                List<String> history=List.of("update\n"
                        + "|switch|p1a: PRIVATE_NICK|Mew, L100|100/100\n"
                        + "|-weather|RainDance\n"
                        + "|-boost|p1a: PRIVATE_NICK|atk|2\n"
                        + "|-start|p1a: PRIVATE_NICK|move: Encore\n"
                        + "|-damage|p1a: PRIVATE_NICK|78/100\n"
                        + "|-crit|p2a: PRIVATE_NICK\n"
                        + "|-miss|p2a: PRIVATE_NICK|p1a: PRIVATE_NICK\n"
                        + "|turn|3");
                var observed=publicCapture.observeForReportsWithProvenance(battleId,history);
                check(observed.replay().size()==8 && observed.envelopes().size()==8,
                        "native filter retains weather, stat changes, status, damage, crit, miss and turn");
                check(PublicProtocolFilter.publicEvents(history).size()==5
                        && PublicProtocolFilter.nativePublicEvents(history).size()==6
                        && PublicProtocolFilter.reportPublicEvents(history).size()==8,
                        "report expansion changes neither frozen offline nor native tactical filters");
                nativeWriter.testLines(id,java.util.stream.Stream.concat(
                        observed.replay().stream(),observed.envelopes().stream()).toList());
                String json=read(nativeRoot.resolve("battle-reports/B01/battle.jsonl"));
                check(json.contains("\"event\":\"-weather\",\"actor\":\"UNKNOWN\",\"detail\":\"raindance\"")
                        && json.contains("\"event\":\"-boost\",\"actor\":\"p1a\",\"detail\":\"atk\"")
                        && json.contains("\"stage\":2")
                        && json.contains("\"event\":\"-crit\"")
                        && json.contains("\"event\":\"-miss\""),
                        "native-only public event types and safe structured effects retained");
                check(json.contains("\"detail\":\"REDACTED_HP\"")
                        && json.contains("\"detail_state\":\"PRIVACY_RESTRICTED\"")
                        && !json.contains("PRIVATE_NICK") && !json.contains("78/100"),
                        "HP and nicknames redacted without losing damage event");
                check(read(nativeRoot.resolve("battle-reports/B01/summary.md"))
                        .contains("## Observed public timeline"),
                        "summary includes event-kind chronology");
            }

            // All read-only native/phase producers share one typed, bounded sink.
            Path familyRoot=tmp.resolve("families");
            var family=AutomaticBattleReports.attachSyntheticForTest(familyRoot,"1.1.9");
            try {
                for(var producer:AutomaticBattleReports.Producer.values())
                    AutomaticBattleReports.recordProducer(88,producer,
                            AutomaticBattleReports.State.OBSERVED,6,2);
                AutomaticBattleReports.recordLegacyEstimate(88,false,"move:ironhead",
                        41.23,38.5,100,0,false,false,6);
                AutomaticBattleReports.recordLegacyEstimate(88,false,"move:ironhead",
                        41.23,38.5,100,0,false,false,6);
                AutomaticBattleReports.recordLegacyEstimate(88,true,"SWITCH_SECRET_UUID",
                        60,60,100,0,false,false,6);
                AutomaticBattleReports.recordFailure(88,
                        AutomaticBattleReports.Failure.NATIVE_ACTION_UNAVAILABLE);
                String journal=read(familyRoot.resolve("battle-reports/B01/battle.jsonl"));
                String overview=read(familyRoot.resolve("battle-reports/B01/summary.md"));
                for(var producer:AutomaticBattleReports.Producer.values())
                    check(journal.contains("\"producer\":\""+producer.name()+"\""),
                            "producer coverage "+producer+" reaches journal");
                check(journal.split("\"kind\":\"legacy_evaluation\"",-1).length==3
                        && journal.contains("\"base_heuristic\":41.23")
                        && journal.contains("\"final_heuristic\":38.5")
                        && journal.contains("\"candidate\":\"SWITCH_REDACTED\""),
                        "Legacy scores, dedup and private switch UUID redaction");
                check(journal.contains("\"kind\":\"issue\"")
                        && read(familyRoot.resolve("battle-reports/issues.jsonl"))
                            .contains("NATIVE_ACTION_UNAVAILABLE"),
                        "source failure codes reach battle and global issue stream");
                check(overview.contains("## Producer coverage")
                        && overview.contains("## Source-computed evaluations")
                        && overview.contains("NATIVE_ACTION_UNAVAILABLE"),
                        "developer summary includes producer inventory, comparisons and failures");
                check(!journal.contains("SWITCH_SECRET_UUID"),
                        "no hidden identifiers in source-computed candidates");
            } finally {
                AutomaticBattleReports.detachSyntheticForTest(family);
            }

            // Crash recovery must preserve the human-readable evidence too,
            // not replace it with an unhelpful empty template.
            Path retain=tmp.resolve("retain-restart");
            var before=AutomaticBattleReports.synthetic(retain,"1.1.9");
            before.testDecision(91,DeveloperBattleDecisionCapture.safeLine(
                    91,8,false,"SHADOW","move:quickattack","move:tackle",
                    "MOVE","UNKNOWN","UNKNOWN",null));
            String beforeSummary=read(retain.resolve("battle-reports/B01/summary.md"));
            before.simulateCrashForTest();
            try(var after=AutomaticBattleReports.synthetic(retain,"1.1.9")) {
                String afterSummary=read(retain.resolve("battle-reports/B01/summary.md"));
                check(beforeSummary.contains("Decisions: 1")
                        && afterSummary.contains("Decisions: 1")
                        && afterSummary.contains("Maestro choices")
                        && afterSummary.contains("INTERRUPTED_RESTART"),
                        "restart retains summary chronology, counts and accurate incomplete state");
            }

            // Regression contract: live chooser must wire source evidence, not
            // silently drop the available computation or replay categories.
            String liveSource=read(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
            String liveProjection=read(Path.of("src/main/java/com/maestroai/training/PublicBattleCapture.java"));
            check(liveSource.contains("developerDecisionReason=preference.reason().name();")
                    && liveSource.contains("DeveloperBattleDecisionCapture.safeEstimates(preference)")
                    && liveSource.contains("legalForDeveloper,developerEvidence")
                    && liveSource.contains("automaticProducer(battle,")
                    && liveSource.contains("recordLegacyEstimate("),
                    "source-based diagnostic producer registrations cannot go missing silently");
            check(liveProjection.contains("PublicProtocolFilter.reportPublicEvents(rawHistory)")
                    && liveProjection.contains("PublicEnvelopeProvenance.reconcileForReports(rawHistory,filtered)")
                    && liveProjection.contains("PublicProtocolFilter.publicEvents(rawHistory)")
                    && liveProjection.contains("PublicEnvelopeProvenance.reconcile(rawHistory, filtered)"),
                    "expanded reporter-only projection cannot alter original tactical observer");
            for(var producer:AutomaticBattleReports.Producer.values())
                check(liveSource.contains("Producer."+producer.name()),
                        "production source must wire the actual "+producer+" producer");
            // A battle containing public activity but no selected return is a
            // diagnostic coverage warning, not an invented choice.
            Path missingChoice=tmp.resolve("no-choice");
            try(var missing=AutomaticBattleReports.synthetic(missingChoice,"1.1.9")){
                missing.testLines(61,List.of(
                        "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=61 seq=1 event=|turn|1",
                        "[MaestroAI] SHADOW_ENVELOPE capture=61 seq=1 route=DIRECT_UPDATE "
                            +"split_target=NONE spectator=EXPECTED_NOT_OBSERVED branch_attested=true"));
                missing.testTerminal(61,List.of(TERMINAL4.replace("capture=4","capture=61")));
                check(read(missingChoice.resolve("battle-reports/issues.jsonl"))
                        .contains("NO_DECISION_RECEIPT_OBSERVED")
                        && read(missingChoice.resolve("battle-reports/B01/summary.md"))
                        .contains("Decisions: 0"),
                        "no-receipt diagnostic is explicit without fabricating a decision");
            }
            // Real Java-generated, sanitized, COMPLETE fixture files for the
            // GitHub verification artifact. Never derived from player logs.
            Path sampleRoot=tmp.resolve("ci-synthetic-sample");
            var sampleWriter=AutomaticBattleReports.attachSyntheticForTest(sampleRoot,"1.1.9");
            try {
                AutomaticBattleReports.recordLines(900,List.of(
                        "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=900 "
                                +"seq=1 event=|switch|p1a|mew",
                        "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=900 "
                                +"seq=2 event=|turn|3",
                        "[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=900 "
                                +"seq=3 event=|-weather|UNKNOWN|raindance",
                        "[MaestroAI] SHADOW_ENVELOPE capture=900 seq=1 "
                                +"route=DIRECT_UPDATE split_target=NONE "
                                +"spectator=EXPECTED_NOT_OBSERVED branch_attested=true",
                        "[MaestroAI] SHADOW_ENVELOPE capture=900 seq=2 "
                                +"route=DIRECT_UPDATE split_target=NONE "
                                +"spectator=EXPECTED_NOT_OBSERVED branch_attested=true",
                        "[MaestroAI] SHADOW_ENVELOPE capture=900 seq=3 "
                                +"route=DIRECT_UPDATE split_target=NONE "
                                +"spectator=EXPECTED_NOT_OBSERVED branch_attested=true"));
                AutomaticBattleReports.recordDecision(900,
                        DeveloperBattleDecisionCapture.safeLine(900,3,false,
                            "SHADOW",chosen.action(),chosen.action(),
                            chosen.reason().name(),"UNKNOWN","UNKNOWN",
                            List.of("move:tackle","move:quickattack")),details);
                AutomaticBattleReports.recordProducer(900,
                        AutomaticBattleReports.Producer.SHADOW_PREFERENCE,
                        AutomaticBattleReports.State.EVALUATED,3,chosen.scoredCandidates());
                AutomaticBattleReports.recordProducer(900,
                        AutomaticBattleReports.Producer.NATIVE_LEGAL_ROOT,
                        AutomaticBattleReports.State.NATIVE_WITNESSED,3,2);
                AutomaticBattleReports.recordFailure(900,
                        AutomaticBattleReports.Failure.NATIVE_SWITCH_UNAVAILABLE);
                AutomaticBattleReports.recordTerminal(900,List.of(
                        "[MaestroAI] SHADOW_TERMINAL capture=900 after=3 "
                        +"event=COBBLEMON_BATTLE_VICTORY winner=REDACTED "
                        +"beginning=UNKNOWN delivery=UNVERIFIED coverage=START_UNKNOWN"));
            } finally {
                AutomaticBattleReports.detachSyntheticForTest(sampleWriter);
            }
            Path sampleSource=sampleRoot.resolve("battle-reports");
            Path samplePublish=Path.of("build","diagnostics-sample");
            Files.createDirectories(samplePublish.resolve("B01"));
            for(String name:List.of("summary.md","battle.jsonl"))
                Files.copy(sampleSource.resolve("B01").resolve(name),
                        samplePublish.resolve("B01").resolve(name),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.copy(sampleSource.resolve("issues.jsonl"),
                    samplePublish.resolve("issues.jsonl"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(samplePublish.resolve("README.md"),
                    "# Synthetic MaestroAI Java regression fixture\n\n"
                    +"This data was generated automatically by "
                    +"AutomaticBattleReportsRegression using only synthetic "
                    +"public protocol and action receipts. It is not a "
                    +"Minecraft battle and is NOT evidence of spectator "
                    +"delivery or an actually selected in-game move.\n",
                    StandardCharsets.UTF_8);
            String sampleJson=read(samplePublish.resolve("B01/battle.jsonl"));
            String sampleSummary=read(samplePublish.resolve("B01/summary.md"));
            check(sampleJson.contains("\"candidate_evaluations\":[")
                    && sampleJson.contains("\"event\":\"-weather\"")
                    && sampleJson.contains("\"kind\":\"terminal\"")
                    && sampleJson.contains("\"kind\":\"issue\"")
                    && sampleSummary.contains("COMPLETED_UNVERIFIED")
                    && sampleSummary.contains("## Maestro choices")
                    && sampleSummary.contains("NATIVE_SWITCH_UNAVAILABLE")
                    && read(samplePublish.resolve("issues.jsonl"))
                        .contains("NATIVE_SWITCH_UNAVAILABLE"),
                    "full Java-generated CI sample includes events, decisions, sources and issues");
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
