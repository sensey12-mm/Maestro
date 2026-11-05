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
                        && json.contains("\"selected_action\":\"move:icebeam\""),
                        "native selection recorded automatically");
                check(json.contains("\"kind\":\"provenance\"")
                        && json.contains("\"spectator_delivery\":\"UNVERIFIED\""),
                        "source envelope and uncertainty retained");
                check(json.contains("\"kind\":\"terminal\"")
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
                check(!read(privacyRoot.resolve("battle-reports/B01/battle.jsonl"))
                        .contains("PRIVATE_SECRET"),"selected action gate prevents private data");
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
