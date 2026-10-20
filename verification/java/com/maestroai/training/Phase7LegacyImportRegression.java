package com.maestroai.training;

import java.nio.file.*;
import java.util.*;

/** Synthetic adversarial contracts, NEVER genuine captured Legacy evidence. */
public final class Phase7LegacyImportRegression {
    private Phase7LegacyImportRegression() {}
    private static int checks;
    private static void check(boolean valid,String explanation) {
        if(!valid)throw new AssertionError("Phase 7.2: "+explanation);
        checks++;
    }
    private static void reject(Runnable action,String explanation) {
        boolean rejected=false;
        try{action.run();}catch(RuntimeException expected){rejected=true;}
        check(rejected,explanation);
    }
    private static Phase7LegacyImport.Capture bundle(
            Phase7Benchmark.Scenario s,List<String> raw,String action) {
        String publicHash=Phase7LegacyImport.publicDigest(PublicProtocolFilter.publicEvents(raw));
        var receipt=new Phase7LegacyImport.ChoiceReceipt("synthetic-capture-001",
                "LEGACY_CHOOSE_RETURN",Phase7LegacyImport.sha256(List.of("synthetic-artifact")),
                Phase7LegacyImport.sha256(List.of("synthetic-own-request")),
                publicHash,Phase7Benchmark.stateFingerprint(s),s.snapshot().turn(),action);
        return new Phase7LegacyImport.Capture(new UUID(1L,2L),raw,receipt);
    }
    private static List<String> history() {
        return List.of("update\n|switch|p1a: Own|Mew, L100|100/100"
                       +"\n|switch|p2a: Foe|Mew, L100|100/100\n|turn|1",
                       "update\n|turn|2");
    }
    private static Phase7LegacyImport.CustodyReviewer checkedReview(List<String> raw){
        String pinned=Phase7LegacyImport.sha256(raw);
        return request->request.rawHistorySha256().equals(pinned)
                && request.artifactSha256().equals(
                        Phase7LegacyImport.sha256(List.of("synthetic-artifact")))
                && request.ownRequestSha256().equals(
                        Phase7LegacyImport.sha256(List.of("synthetic-own-request")))
                && request.choiceReceiptSha256().matches("[a-f0-9]{64}");
    }
    private static Phase7LegacyImport.Status status(Phase7LegacyImport.Capture c,
            Phase7Benchmark.Scenario s,Phase7LegacyImport.CustodyReviewer r){
        return Phase7LegacyImport.importCapture(c,s,r).status();
    }
    public static void run(){
        try{
            String fixture=Files.readString(Path.of("verification/benchmarks/phase7_1_synthetic.json"));
            var scenario=Phase7Benchmark.parseFixture(fixture).scenarios().getFirst();
            List<String> raw=history();
            var capture=bundle(scenario,raw,"move:attack");
            var verifier=checkedReview(raw);
            var imported=Phase7LegacyImport.importCapture(capture,scenario,verifier);
            check(imported.accepted(),"synthetic trusted-custody stub exercises approved route");
            check(imported.legacy()!=null && imported.legacy().externallyVerified(),
                    "a reviewed result can bind into Phase 7.1");
            check(imported.publicSha256().equals(
                    Phase7LegacyImport.publicDigest(PublicProtocolFilter.publicEvents(raw))),
                    "independent public stream digest");
            check(imported.publicSha256().matches("[a-f0-9]{64}"),"bounded digest");
            var same=Phase7LegacyImport.importCapture(capture,scenario,verifier);
            check(same.equals(imported),"deterministic import");
            var compared=Phase7LegacyImport.compare(scenario,imported,33L,
                    (snapshot,seed)->"move:attack");
            check(compared.status()==Phase7LegacyImport.Status.ACCEPTED
                    && Boolean.TRUE.equals(compared.shadowAgreement()),
                    "actual action can agree with offline candidate");
            check(compared.legacyAction().equals("move:attack")
                    && compared.baselineAction()!=null
                    && compared.shadowAction().equals("move:attack"),
                    "explicit legal actions returned");
            check(!compared.json().contains("utility")
                    && !compared.json().contains("Regret")
                    && compared.json().contains("no-win-rate"),
                    "no synthetic criteria misreported as empirical quality");
            check(compared.json().equals(Phase7LegacyImport.compare(
                    scenario,imported,33L,(snapshot,seed)->"move:attack").json()),
                    "machine report reproducible");

            var disagree=Phase7LegacyImport.compare(scenario,imported,33L,
                    (snapshot,seed)->"move:guard");
            check(Boolean.FALSE.equals(disagree.shadowAgreement()),"candidate disagreement");
            var illegal=Phase7LegacyImport.compare(scenario,imported,33L,
                    (snapshot,seed)->"move:PRIVATE_OPPONENT_SECRET");
            check(illegal.shadowIllegal() && illegal.shadowAbstained()
                    && illegal.shadowAction()==null,"illegal candidate rejected");
            check(!illegal.json().contains("PRIVATE_OPPONENT_SECRET"),"candidate contents redacted");
            var failure=Phase7LegacyImport.compare(scenario,imported,33L,
                    (snapshot,seed)->{throw new IllegalStateException("SECRET_ACTOR_IDENTIFIER");});
            check(failure.shadowIllegal() && !failure.json().contains("SECRET_ACTOR_IDENTIFIER"),
                    "policy failure details not emitted");
            var abstain=Phase7LegacyImport.compare(scenario,imported,33L,null);
            check(abstain.shadowAbstained() && abstain.shadowAgreement()==null,
                    "missing offline Shadow action abstains");

            check(status(capture,scenario,null)==Phase7LegacyImport.Status.NO_CUSTODY_REVIEW,
                    "no review never accepted");
            check(status(capture,scenario,request->false)==Phase7LegacyImport.Status.NO_CUSTODY_REVIEW,
                    "self-declared receipt is not custody");
            check(status(capture,scenario,request->{throw new IllegalStateException("PRIVATE_LOG");})
                    ==Phase7LegacyImport.Status.NO_CUSTODY_REVIEW,
                    "reviewer exception redacted");
            check(status(bundle(scenario,raw,"move:unseen"),scenario,verifier)
                    ==Phase7LegacyImport.Status.ILLEGAL_ACTION,"unavailable Legacy choice rejected");

            var alteredScenario=Phase7Benchmark.parseFixture(fixture.replace(
                    "\"seed\": 1729","\"seed\": 1730")).scenarios().getFirst();
            // Fixture seed does not change the canonical public state by design.
            check(Phase7Benchmark.stateFingerprint(scenario).equals(
                    Phase7Benchmark.stateFingerprint(alteredScenario)),
                    "state binding intentionally independent of random seed");
            var differentState=Phase7Benchmark.parseFixture(fixture.replace(
                    "\"turn\": 2","\"turn\": 3")).scenarios().getFirst();
            check(status(capture,differentState,verifier)
                    ==Phase7LegacyImport.Status.MISMATCHED_SCENARIO,
                    "different public state cannot inherit receipt");
            var stale=new Phase7Benchmark.Scenario(scenario.id(),scenario.version(),
                    scenario.snapshot(),scenario.criteria(),
                    new Phase7Benchmark.Evidence(true,false,true,scenario.snapshot().turn()),null);
            check(status(capture,stale,verifier)==Phase7LegacyImport.Status.STALE_TURN,
                    "expired snapshot rejected");
            var unsupported=new Phase7Benchmark.Scenario(scenario.id(),scenario.version(),
                    scenario.snapshot(),scenario.criteria(),
                    new Phase7Benchmark.Evidence(true,true,false,scenario.snapshot().turn()),null);
            check(status(capture,unsupported,verifier)
                    ==Phase7LegacyImport.Status.UNKNOWN_PUBLIC_EVIDENCE,
                    "unsupported mechanics gate");
            check(status(bundle(scenario,List.of("update\n|switch|p1a: Own|Mew, L100|100/100"
                    +"\n|switch|p2a: Foe|Mew, L100|100/100\n|turn|1"),
                    "move:attack"),scenario,request->true)
                    ==Phase7LegacyImport.Status.STALE_TURN,
                    "past spectator turn rejected");
            var gap=List.of(raw.getFirst(),"update\n|split|p2\n|-damage|p2a: SECRET|1/1");
            check(status(bundle(scenario,gap,"move:attack"),scenario,request->true)
                    !=Phase7LegacyImport.Status.ACCEPTED,"incomplete split rejected");
            var ambiguous=List.of("update\n|switch|p1a: Own|Mew, L100|100/100\n"
                    +"|switch|p2a: Foe|Mew, L100|100/100\n|replace|p2a: Foe|Zoroark, L100\n|turn|1",
                    "update\n|turn|2");
            check(status(bundle(scenario,ambiguous,"move:attack"),scenario,request->true)
                    ==Phase7LegacyImport.Status.AMBIGUOUS_IDENTITY,
                    "Illusion identity ambiguity rejected");
            var repeated=List.of("update\n|switch|p1a: Own|Mew, L100|100/100\n"
                    +"|switch|p2a: Foe|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L100|100/100\n|turn|1",
                    "update\n|turn|2");
            check(status(bundle(scenario,repeated,"move:attack"),scenario,request->true)
                    ==Phase7LegacyImport.Status.AMBIGUOUS_IDENTITY,
                    "ambiguous reentry rejected");

            var modifiedPrivate=new ArrayList<>(raw);
            modifiedPrivate.add(1,"sideupdate\np2\n|move|p2a: PRIVATE_OPPONENT_ID|Secret Power");
            var privateCapture=bundle(scenario,modifiedPrivate,"move:attack");
            var privateResult=Phase7LegacyImport.importCapture(privateCapture,scenario,verifier);
            check(privateResult.status()==Phase7LegacyImport.Status.NO_CUSTODY_REVIEW,
                    "private source mutation fails independently pinned custody hash");
            check(privateResult.publicSha256()==null && privateResult.legacy()==null
                    && !privateResult.toString().contains("PRIVATE_OPPONENT_ID"),
                    "raw private data never serialized");
            check(status(privateCapture,scenario,checkedReview(modifiedPrivate))
                    ==Phase7LegacyImport.Status.ACCEPTED,
                    "private sideupdate ignored by public evidence but still custody-bound");

            var changedPublic=new ArrayList<>(raw);
            changedPublic.set(0,changedPublic.getFirst().replace("Mew, L100","Pikachu, L100"));
            check(status(new Phase7LegacyImport.Capture(capture.battleId(),changedPublic,
                    capture.choice()),scenario,request->true)
                    ==Phase7LegacyImport.Status.INVALID_PROVENANCE,
                    "changed spectator state invalidates digest");
            check(status(bundle(scenario,List.of(
                    "update\n|switch|p1a: Own|Mew, L100|100/100\n"
                    +"|switch|p2a: Foe|Mew, L100|100/100\n|turn|1",
                    "update\n|turn|2\n|move|p2a: Foe|Hidden Power|p1a: Own"),
                    "move:attack"),scenario,request->true)
                    ==Phase7LegacyImport.Status.STALE_TURN,
                    "post-turn events cannot retrospectively certify choice timing");
            check(Phase7LegacyImport.compare(scenario,
                    new Phase7LegacyImport.ImportResult(Phase7LegacyImport.Status.STALE_TURN,null,null),
                    33L,(snap,seed)->{throw new AssertionError("must not invoke shadow");})
                    .shadowAction()==null,"reject never invokes offline candidate");

            var batch=Phase7LegacyImport.importBatch(List.of(capture,capture),
                    Map.of(capture.choice().captureId(),scenario),verifier);
            check(batch.getFirst().accepted()
                    && batch.getLast().status()==Phase7LegacyImport.Status.DUPLICATE_CAPTURE,
                    "same capture cannot count twice");
            check(Phase7LegacyImport.importBatch(List.of(capture),
                    Map.of(),verifier).getFirst().status()
                    ==Phase7LegacyImport.Status.MISMATCHED_SCENARIO,
                    "absent scenario binding rejected");
            reject(()->new Phase7LegacyImport.ChoiceReceipt("synthetic","REPLAYED_MOVE",
                    capture.choice().artifactSha256(),capture.choice().ownRequestSha256(),
                    capture.choice().publicSha256(),capture.choice().scenarioFingerprint(),
                    capture.choice().turn(),"move:attack"),
                    "executed moves do not attest selected action");
            reject(()->new Phase7LegacyImport.ChoiceReceipt("synthetic","LEGACY_CHOOSE_RETURN",
                    "bad",capture.choice().ownRequestSha256(),capture.choice().publicSha256(),
                    capture.choice().scenarioFingerprint(),capture.choice().turn(),"move:attack"),
                    "nonhash custody fingerprints rejected");
            check(!compared.json().contains("capture-001")
                    && !compared.json().contains("Mew, L100"),
                    "public comparison suppresses identifying capture data");
            System.out.println("Phase 7.2 synthetic intake regressions PASS: "+checks+" checks"
                    +" (genuine Legacy captures evaluated: 0)");
        } catch(java.io.IOException e){throw new AssertionError("missing synthetic benchmark fixture",e);}
    }
}
