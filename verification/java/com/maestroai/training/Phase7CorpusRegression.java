package com.maestroai.training;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Phase 7.4 synthetic and adversarial corpus tests, never real battle evidence. */
public final class Phase7CorpusRegression {
    private Phase7CorpusRegression() {}
    private static int checks;
    private static final String PRIVATE="PRIVATE_OPPONENT_DO_NOT_EXPORT";
    private static final String FIRST="update\n|switch|p1a: Own|Mew, L100|100/100\n"
            +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1";
    private static final String SECOND="update\n|turn|2";
    private static final String A="a".repeat(64);
    private static final String B="b".repeat(64);
    private static final UUID BATTLE=UUID.fromString("01010101-1111-2222-3333-444444444444");

    private static void check(boolean condition,String why) {
        if(!condition)throw new AssertionError("Phase 7.4: "+why);
        checks++;
    }
    private static void reject(Runnable op,String why) {
        boolean threw=false;
        try{op.run();}catch(RuntimeException expected){threw=true;}
        check(threw,why);
    }
    private static Phase7Benchmark.Scenario scenario() {
        try {
            return Phase7Benchmark.parseFixture(Files.readString(
                    Path.of("verification/benchmarks/phase7_1_synthetic.json")))
                    .scenarios().getFirst();
        } catch(java.io.IOException missing) {
            throw new AssertionError("missing Phase 7.1 fixture",missing);
        }
    }
    private static Phase7Corpus.Submission submission(String captureId, UUID battle,
            String ownRequest, List<String> finalHistory,List<List<String>> checkpoints,
            Phase7Benchmark.Scenario scenario) {
        var receipt=new Phase7LegacyImport.ChoiceReceipt(captureId,"LEGACY_CHOOSE_RETURN",A,
                ownRequest,Phase7LegacyImport.publicDigest(PublicProtocolFilter.publicEvents(finalHistory)),
                Phase7Benchmark.stateFingerprint(scenario),scenario.snapshot().turn(),"move:attack");
        var capture=new Phase7LegacyImport.Capture(battle,finalHistory,receipt);
        return new Phase7Corpus.Submission(scenario,capture,checkpoints);
    }
    private static Phase7Corpus.Submission good(String captureId) {
        List<String> raw=List.of(FIRST,SECOND);
        return submission(captureId,BATTLE,B,raw,List.of(List.of(FIRST),raw),scenario());
    }
    private static Phase7LegacyImport.CustodyReviewer pinned(Phase7Corpus.Submission s) {
        // Synthetic stub standing in for an INDEPENDENTLY maintained trusted
        // original; do not reuse such a self-asserted stub in production.
        String pinnedRaw=Phase7LegacyImport.sha256(s.capture().rawHistory());
        return request->request.captureId().equals(s.capture().choice().captureId())
                && request.artifactSha256().equals(A)
                && request.ownRequestSha256().equals(B)
                && request.publicSha256().equals(s.capture().choice().publicSha256())
                && request.rawHistorySha256().equals(pinnedRaw);
    }
    private static Phase7Corpus.Report run(Phase7Corpus.Submission s,
            Phase7LegacyImport.CustodyReviewer reviewer,Phase7Benchmark.DecisionPolicy shadow) {
        return Phase7Corpus.evaluate(List.of(s),321L,reviewer,shadow);
    }
    private static Phase7Corpus.Row row(Phase7Corpus.Report report) {
        return report.rows().getFirst();
    }

    public static void run() {
        var good=good("capture-a");
        var reviewer=pinned(good);
        var first=run(good,reviewer,(snapshot,seed)->"move:attack");
        var repeated=run(good,reviewer,(snapshot,seed)->"move:attack");
        check(first.json().equals(repeated.json()) && first.fingerprint().equals(repeated.fingerprint()),
                "reproducible sorted corpus JSON and fingerprint");
        check(JsonParser.parseString(first.json()).getAsJsonObject()
                .get("schema").getAsString().equals(Phase7Corpus.SCHEMA),
                "machine JSON schema");
        check(row(first).status()==Phase7Corpus.Status.ACCEPTED
                && row(first).legacyAction().equals("move:attack"),"reviewed synthetic record admitted");
        check(Boolean.TRUE.equals(row(first).shadowAgreement())
                && row(first).baselineAction()!=null,"Shadow/baseline comparison produced");
        check(first.coverage().submitted()==1 && first.coverage().accepted()==1
                && first.coverage().rejected()==0 && first.coverage().comparisons()==1
                && first.coverage().shadowAgreements()==1,"coverage and agreement counts");
        check(first.json().contains("observed-action-agreement-only") == false
                && first.json().contains("reviewed-action-agreement-only"),
                "no empirical win rate or invented performance metric");
        check(!first.json().contains(PRIVATE) && !first.json().contains("Mew")
                && !first.json().contains("Opponent") && !first.json().contains(BATTLE.toString())
                && !first.json().contains("capture-a")
                && !first.json().contains(B),"privacy-preserving sanitized report");
        check(!good.toString().contains("Mew") && !good.toString().contains(PRIVATE),
                "input object cannot accidentally log raw state");

        var none=run(good,null,(snapshot,seed)->{throw new AssertionError("shadow must not run");});
        check(row(none).status()==Phase7Corpus.Status.NO_CUSTODY_REVIEW
                && none.coverage().accepted()==0 && none.coverage().shadowAbstentions()==1,
                "missing independent reviewer refuses all empirical comparisons");
        check(row(none).legacyAction()==null && row(none).shadowAction()==null,
                "rejected receipt leaves no action");
        check(run(good,request->false,null).coverage().accepted()==0,
                "custody refusal cannot be overruled");

        var exception=run(good,reviewer,(snapshot,seed)->{
            throw new IllegalStateException(PRIVATE);
        });
        check(row(exception).status()==Phase7Corpus.Status.ACCEPTED
                && row(exception).shadowIllegal() && row(exception).shadowAbstained()
                && !exception.json().contains(PRIVATE),"Shadow failure redacted and counted");
        var illegal=run(good,reviewer,(snapshot,seed)->"move:private_secret");
        check(row(illegal).shadowIllegal()
                && row(illegal).shadowAction()==null
                && !illegal.json().contains("private_secret"),
                "illegal candidate neither scored nor emitted");
        var abstain=run(good,reviewer,(snapshot,seed)->null);
        check(abstain.coverage().shadowAbstentions()==1
                && row(abstain).shadowAgreement()==null,"Shadow abstention explicit");
        var disagree=run(good,reviewer,(snapshot,seed)->"move:guard");
        check(Boolean.FALSE.equals(row(disagree).shadowAgreement())
                && disagree.coverage().shadowDisagreements()==1,"disagreement counted");

        var reordered=Phase7Corpus.evaluate(List.of(good("capture-b"),good("capture-c")),
                321L,reviewer,null);
        check(reordered.coverage().accepted()==0
                && reordered.coverage().duplicates()==2,
                "same actual battle-turn rejected independent of opaque capture IDs");
        var reversed=Phase7Corpus.evaluate(List.of(good("capture-c"),good("capture-b")),
                321L,reviewer,null);
        check(reordered.json().equals(reversed.json()),"input ordering cannot change dedup verdict");
        var duplicate=Phase7Corpus.evaluate(List.of(good,good),321L,reviewer,null);
        check(duplicate.coverage().duplicates()==2 && duplicate.coverage().accepted()==0,
                "both uses of the same capture ID rejected, not first-wins");

        var otherBattle=UUID.fromString("11111111-2222-3333-4444-555555555555");
        var differentBattle=submission("capture-d",otherBattle,B,List.of(FIRST,SECOND),
                List.of(List.of(FIRST),List.of(FIRST,SECOND)),scenario());
        var replayed=Phase7Corpus.evaluate(List.of(good,differentBattle),321L,reviewer,null);
        check(replayed.coverage().duplicates()==2,
                "same salted request/public action evidence cannot count as two captures");
        var unique=submission("capture-unique",otherBattle,A,List.of(FIRST,SECOND),
                List.of(List.of(FIRST),List.of(FIRST,SECOND)),scenario());
        var distinct=Phase7Corpus.evaluate(List.of(good,unique),321L,
                request->request.artifactSha256().equals(A),null);
        check(distinct.coverage().accepted()==2
                && distinct.coverage().duplicates()==0,
                "unique reviewed actor evidence can contribute independent rows");

        AtomicInteger custodyCalls=new AtomicInteger();
        var changed=List.of(FIRST.replace("Opponent","EXPOSE_SECRET"),SECOND);
        var rewritten=submission("capture-rewrite",BATTLE,B,List.of(FIRST,SECOND),
                List.of(List.of(changed.getFirst()),List.of(FIRST,SECOND)),scenario());
        var rewriteReport=run(rewritten,request->{custodyCalls.incrementAndGet();return true;},null);
        check(row(rewriteReport).status()==Phase7Corpus.Status.REPLAY_REWRITE
                && custodyCalls.get()==0,"rewritten raw prefix rejected before custody review");
        check(!rewriteReport.json().contains("EXPOSE_SECRET"),"raw rewrite payload hidden");

        var missingEnd=submission("capture-missing",BATTLE,B,List.of(FIRST,SECOND),
                List.of(List.of(FIRST)),scenario());
        check(row(run(missingEnd,request->true,null)).status()
                ==Phase7Corpus.Status.REPLAY_MISMATCH,"final raw snapshot must equal submitted capture");
        var split=List.of("update\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1"
                +"\n|split|p2\n|-damage|p2a: "+PRIVATE+"|1/1",SECOND);
        var interrupted=submission("capture-split",BATTLE,B,split,
                List.of(List.of(split.getFirst()),split),scenario());
        check(row(run(interrupted,request->true,null)).status()
                ==Phase7Corpus.Status.REPLAY_INCOMPLETE,
                "partial split cannot be promoted during replay");

        var changedPublic=List.of(FIRST.replace("Mew, L100","Pikachu, L100"),SECOND);
        var wrongDigest=submission("capture-public",BATTLE,B,List.of(FIRST,SECOND),
                List.of(changedPublic),scenario());
        check(row(run(wrongDigest,request->true,null)).status()
                ==Phase7Corpus.Status.REPLAY_MISMATCH,"public-history mismatch detected");

        var ambiguous=List.of("update\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Opponent|Mew, L100|100/100\n"
                +"|replace|p2a: Opponent|Zoroark, L100\n|turn|1",SECOND);
        var illusion=submission("capture-illusion",BATTLE,B,ambiguous,
                List.of(List.of(ambiguous.getFirst()),ambiguous),scenario());
        check(row(run(illusion,request->true,null)).status()
                ==Phase7Corpus.Status.AMBIGUOUS_IDENTITY,"illusion appearance remains ambiguous");

        var extra=List.of(FIRST,"update\n|turn|2\n|move|p2a: Opponent|"+PRIVATE+"|p1a: Own");
        var afterTurn=submission("capture-after",BATTLE,B,extra,
                List.of(List.of(FIRST),extra),scenario());
        check(row(run(afterTurn,request->true,null)).status()
                ==Phase7Corpus.Status.STALE_TURN,"events after turn marker cannot certify choice time");

        var unsupportedScenario=new Phase7Benchmark.Scenario(scenario().id(),1,scenario().snapshot(),
                scenario().criteria(),new Phase7Benchmark.Evidence(true,true,false,2),null);
        var unsupported=submission("capture-unsupported",BATTLE,B,List.of(FIRST,SECOND),
                List.of(List.of(FIRST),List.of(FIRST,SECOND)),unsupportedScenario);
        check(row(run(unsupported,request->true,null)).status()
                ==Phase7Corpus.Status.UNKNOWN_PUBLIC_EVIDENCE,"unsupported scenario cannot score");
        var staleScenario=new Phase7Benchmark.Scenario(scenario().id(),1,scenario().snapshot(),
                scenario().criteria(),new Phase7Benchmark.Evidence(true,false,true,2),null);
        var stale=submission("capture-stale",BATTLE,B,List.of(FIRST,SECOND),
                List.of(List.of(FIRST),List.of(FIRST,SECOND)),staleScenario);
        check(row(run(stale,request->true,null)).status()
                ==Phase7Corpus.Status.STALE_TURN,"stale evidence abstains");

        var secretHistory=List.of(FIRST,"sideupdate\np2\n|move|p2a: "+PRIVATE+"|SECRETITEM",SECOND);
        var secret=submission("capture-private",BATTLE,B,secretHistory,
                List.of(List.of(FIRST),secretHistory),scenario());
        check(row(run(secret,null,null)).status()==Phase7Corpus.Status.NO_CUSTODY_REVIEW
                && !run(secret,null,null).json().contains(PRIVATE),
                "private sideupdates never enable self-approved corpus entries");

        check(Phase7Corpus.evaluate(List.of(),321,null,null).coverage().submitted()==0,
                "empty corpus explicitly reports zero coverage");
        reject(()->Phase7Corpus.evaluate(Collections.nCopies(101,good),1,null,null),
                "bounded corpus rejects over-capacity");
        reject(()->new Phase7Corpus.Submission(scenario(),good.capture(),
                Collections.nCopies(33,List.of(FIRST))),"checkpoint count bounded");

        try {
            Path root=Files.createTempDirectory("phase7-corpus-");
            try {
                Path report=Phase7Corpus.writeLocalReport(root,"phase7_regression.json",first);
                check(Files.exists(report) && Files.readString(report).equals(first.json()),
                        "local-only sanitized atomic report persisted");
                check(report.startsWith(root.resolve("run").resolve("phase7-corpus")),
                        "report path restricted to ignored /run subtree");
                check(!Files.readString(report).contains(PRIVATE) && !Files.readString(report)
                        .contains(BATTLE.toString()),"disk artifact contains no raw private evidence");
                reject(()->{
                    try{Phase7Corpus.writeLocalReport(root,"../escape.json",first);}
                    catch(java.io.IOException e){throw new IllegalStateException(e);}
                },"path traversal refused");
                if(Files.isSymbolicLink(root.resolve("run")))throw new AssertionError("unexpected temp symlink");
                Path linked=root.resolve("run").resolve("phase7-corpus").resolve("phase7_link.json");
                try {
                    Files.createSymbolicLink(linked,root.resolve("outside.json"));
                    try {
                        Phase7Corpus.writeLocalReport(root,"phase7_link.json",first);
                        throw new AssertionError("symlink report destination allowed");
                    } catch(java.io.IOException expected) { checks++; }
                } catch(UnsupportedOperationException | java.nio.file.FileSystemException unsupported) {
                    // Some test filesystems disallow symlinks; not a failure.
                }
            } finally {
                try(var walk=Files.walk(root)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p->{
                        try{Files.deleteIfExists(p);}
                        catch(java.io.IOException ignored){throw new IllegalStateException("temp cleanup failed");}
                    });
                }
            }
        } catch(java.io.IOException failure) {throw new AssertionError("local report test failed",failure);}
        System.out.println("Phase 7.4 synthetic corpus regressions PASS: "+checks
                +" checks (real Legacy decisions: 0; Minecraft runs: 0)");
    }
}
