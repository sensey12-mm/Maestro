package com.maestroai.training;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Phase 7.1 verification-only deterministic and adversarial synthetic tests. */
public final class Phase7BenchmarkRegression {
    private Phase7BenchmarkRegression() {}
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError("Phase 7.1: " + message);
        checks++;
    }
    private static void reject(Runnable r, String reason) {
        boolean rejected=false;
        try { r.run(); } catch (RuntimeException expected) { rejected=true; }
        check(rejected, reason);
    }
    private static Phase7Benchmark.Row row(Phase7Benchmark.Report r,String id) {
        return r.rows().stream().filter(x->x.scenarioId().equals(id)).findFirst().orElseThrow();
    }

    public static void run() {
        try {
            String text=Files.readString(Path.of("verification/benchmarks/phase7_1_synthetic.json"));
            var suite=Phase7Benchmark.parseFixture(text);
            var first=Phase7Benchmark.run(suite,null);
            var again=Phase7Benchmark.run(Phase7Benchmark.parseFixture(text),null);
            check(first.json().equals(again.json()),"replays are byte-for-byte deterministic");
            check(first.rows().size()==3 && first.schema().equals(Phase7Benchmark.SCHEMA),
                    "fixture version and count");
            check(first.configurationFingerprint().matches("[0-9a-f]{64}"),"configuration hash");
            check(row(first,"synthetic-stale").status().equals("UNKNOWN_EVIDENCE"),"stale evidence abstains");
            check(row(first,"synthetic-stale").baseline()==null
                    && row(first,"synthetic-stale").baselineRegret()==null,
                    "unknown evidence has no invented quality");
            check(first.json().contains("\"scope\":\"synthetic-criteria-only;not-minecraft;not-win-rate\""),
                    "results visibly labeled synthetic");
            check(first.json().equals(JsonParser.parseString(first.json()).toString()),
                    "machine output is canonical JSON");
            check(first.json().contains("\"unknownEvidence\":1"),"unknown count");

            var score=Phase7Benchmark.run(suite,(snap,seed)->"move:attack");
            check(row(score,"synthetic-offense").candidateRegret()==0.0,"explicit offense criterion");
            check(row(score,"synthetic-offense").candidateUncertainty()==0.15,"uncertainty preserved");
            check(row(score,"synthetic-defensive").candidateRegret()>0,
                    "synthetic criteria distinguish defensive outcome");
            var invalid=Phase7Benchmark.run(suite,(snap,seed)->"move:unrevealed-secret");
            check(row(invalid,"synthetic-offense").illegalCandidate()
                    && row(invalid,"synthetic-offense").candidate()==null,
                    "illegal action cannot be scored");
            check(!invalid.json().contains("unrevealed-secret"),"invalid action redacted");
            var exception=Phase7Benchmark.run(suite,(snap,seed)-> {
                throw new IllegalStateException("PRIVATE_OPPONENT_PAYLOAD");
            });
            check(row(exception,"synthetic-offense").illegalCandidate()
                    && !exception.json().contains("PRIVATE_OPPONENT_PAYLOAD"),
                    "policy failures remain bounded and private");
            final int[] calls={0};
            Phase7Benchmark.run(suite,(snap,seed)->{
                calls[0]++;
                return null;
            });
            check(calls[0]==2,"unknown evidence never calls shadow policy");
            check(first.json().contains("\"abstentions\":3"),"absence is explicit abstention");

            var changed=Phase7Benchmark.parseFixture(text.replace("\"seed\": 1729","\"seed\": 1730"));
            check(!suite.configurationFingerprint().equals(changed.configurationFingerprint()),
                    "configuration seed changes exact fingerprint");
            var reordered=Phase7Benchmark.parseFixture(text.replace(
                    "[\"attack\", \"guard\"]", "[\"guard\", \"attack\"]"));
            check(Phase7Benchmark.stateFingerprint(suite.scenarios().getFirst()).equals(
                    Phase7Benchmark.stateFingerprint(reordered.scenarios().getFirst())),
                    "state fingerprint canonicalizes legal action order");
            check(Phase7Benchmark.simpleBaseline(suite.scenarios().getFirst().snapshot(),17)
                    .equals(Phase7Benchmark.simpleBaseline(reordered.scenarios().getFirst().snapshot(),17)),
                    "seeded baseline independent of action presentation order");

            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"schema\": \"phase7.1.v1\"", "\"schema\": \"phase7.0.v1\"")),
                    "unsupported schema rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"version\": 1", "\"version\": 2")),
                    "unsupported scenario version rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"seed\": 1729", "\"seed\": 1729, \"hiddenOpponentMoves\": [\"secret\"]")),
                    "hidden-field injection rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"seed\": 1729", "\"seed\": 1729, \"seed\": 1729")),
                    "duplicate JSON property rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"move:attack\": {\"utility\": 0.8, \"uncertainty\": 0.15},",
                    "\"move:attack\": {\"utility\": 0.8, \"uncertainty\": 0.15, \"private\": 1},")),
                    "unexpected criterion property rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"move:guard\": {\"utility\": 0.3, \"uncertainty\": 0.1},",
                    "")),
                    "incomplete action universe rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"utility\": 0.8", "\"utility\": 9.0")),
                    "out-of-range invented value rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"id\": \"synthetic-stale\"", "\"id\": \"synthetic-offense\"")),
                    "duplicate scenario identifiers rejected");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"fresh\": false", "\"fresh\": true, \"privateItem\": \"SECRET\"")),
                    "sensitive opponent field rejected without echo");
            reject(()->Phase7Benchmark.parseFixture(text.replace(
                    "\"moves\": [\"attack\", \"guard\"]", "\"moves\": [\"attack\", \"attack\"]")),
                    "duplicate legal actions rejected");

            var s=suite.scenarios().getFirst();
            var foe=new BattleSnapshot.Combatant("synthetic-opponent",
                    BattleSnapshot.UNKNOWN_HP_FRACTION,List.of(),Map.of(),
                    List.of("unseenmove"),null,null,false);
            var contaminated=new BattleSnapshot(s.snapshot().turn(),s.snapshot().self(),
                    foe,s.snapshot().selfBench(),List.of(),s.snapshot().legalMoves(),
                    s.snapshot().legalSwitches(),Map.of(),false,null);
            reject(()->new Phase7Benchmark.Scenario(s.id(),s.version(),contaminated,
                    s.criteria(),s.evidence(),null),"private opponent state cannot enter runner");
            var hpFoe=new BattleSnapshot.Combatant("synthetic-opponent",0.9,List.of(),
                    Map.of(),List.of(),null,null,false);
            reject(()->new Phase7Benchmark.Scenario(s.id(),s.version(),
                    new BattleSnapshot(s.snapshot().turn(),s.snapshot().self(),hpFoe,
                            s.snapshot().selfBench(),List.of(),s.snapshot().legalMoves(),
                            s.snapshot().legalSwitches(),Map.of(),false,null),
                    s.criteria(),s.evidence(),null),
                    "unattested enemy HP cannot enter runner");
            reject(()->new Phase7Benchmark.Scenario(s.id(),s.version(),s.snapshot(),
                    s.criteria(),new Phase7Benchmark.Evidence(true,true,true,999),null),
                    "stale turn evidence rejected");

            // No synthetic record is represented as an observed Legacy decision.
            check(first.rows().stream().allMatch(x->x.legacy().equals("ABSENT")),
                    "no fabricated Legacy captures");
            var record=new Phase7Benchmark.LegacyRecord("capture-001",s.snapshot().turn(),
                    Phase7Benchmark.stateFingerprint(s),"move:attack",true);
            var matched=new Phase7Benchmark.Scenario(s.id(),s.version(),s.snapshot(),
                    s.criteria(),s.evidence(),record);
            var withLegacy=new Phase7Benchmark.Suite(suite.schema(),suite.seed(),
                    suite.configurationFingerprint(),List.of(matched));
            check(row(Phase7Benchmark.run(withLegacy,null),s.id()).legacy().equals("SELF_ASSERTED_UNVERIFIED"),
                    "externally verified matching capture can be compared without claiming authenticity");
            var forged=new Phase7Benchmark.LegacyRecord("capture-001",s.snapshot().turn(),
                    Phase7Benchmark.stateFingerprint(s),"move:attack",false);
            var failed=new Phase7Benchmark.Scenario(s.id(),s.version(),s.snapshot(),
                    s.criteria(),s.evidence(),forged);
            check(row(Phase7Benchmark.run(new Phase7Benchmark.Suite(suite.schema(),
                    suite.seed(),suite.configurationFingerprint(),List.of(failed)),null),
                    s.id()).legacy().equals("REJECTED_PROVENANCE"),"unverified capture refused");
            var other=new Phase7Benchmark.LegacyRecord("capture-001",s.snapshot().turn(),
                    "0".repeat(64),"move:attack",true);
            check(row(Phase7Benchmark.run(new Phase7Benchmark.Suite(suite.schema(),
                    suite.seed(),suite.configurationFingerprint(),List.of(
                            new Phase7Benchmark.Scenario(s.id(),s.version(),s.snapshot(),
                                    s.criteria(),s.evidence(),other))),null),s.id())
                    .legacy().equals("REJECTED_PROVENANCE"),"mismatched state binding refused");
            var stale=Phase7Benchmark.run(suite,(snap,seed)->"switch:reserve");
            check(row(stale,"synthetic-stale").candidate()==null
                    && row(stale,"synthetic-stale").candidateRegret()==null,
                    "unsupported stale battle never promotes an apparent score");

            // Explicitly find a seed where a poorer externally supplied decision
            // is worse than the seeded baseline; metric is regret, NOT wins.
            boolean sawRegression=false;
            for(long seed=0;seed<50;seed++) {
                var replay=new Phase7Benchmark.Suite(suite.schema(),seed,
                        suite.configurationFingerprint(),List.of(s));
                var r=Phase7Benchmark.run(replay,(snap,ignored)->"move:guard");
                if(row(r,s.id()).regression()) {
                    check(row(r,s.id()).candidateRegret()>
                            row(r,s.id()).baselineRegret(),"regression is criterion-relative");
                    sawRegression=true;break;
                }
            }
            check(sawRegression,"synthetic regression detection exercised");
            System.out.println("Phase 7.1 synthetic benchmark regressions PASS: "+checks+" checks");
        } catch (java.io.IOException ex) {
            throw new AssertionError("missing Phase 7.1 fixture",ex);
        }
    }
}
