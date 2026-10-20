package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Phase 7.9 adversarial cross-version regression, using the SAME synthetic
 * Ed25519-signed private bundles already created by Phase7ShadowReplayRegression.
 * Never a real Legacy observation, custody approval or Minecraft trial.
 */
public final class Phase7ReplayMatrixRegression {
    private Phase7ReplayMatrixRegression() {}
    private static int checks;
    private static final String SECRET="PRIVATE_OPPONENT_NICKNAME_NOT_FOR_GITHUB";
    private static void check(boolean b,String reason){
        if(!b)throw new AssertionError("Phase 7.9: "+reason);
        checks++;
    }
    private static void reject(Runnable call,String reason) {
        boolean failed=false;
        try{call.run();}catch(RuntimeException expected){failed=true;}
        check(failed,reason);
    }
    private static String sha(String data){
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static String fingerprint(String text){return sha(text);}
    private static Phase7ReplayMatrix.Version install(Path root,String revision,
           String artifact,long seed,int budget,int traces,double penalty,
           Map<String,Phase7ShadowReplay.Graph> graphs,
           Map<String,Phase7Benchmark.Scenario> scenarios) throws Exception {
        var p=Phase7ShadowReplay.define(revision,artifact,seed,graphs,scenarios,
                budget,traces,penalty);
        Path path=Phase7ShadowReplay.writeLocal(root,p);
        var verified=Phase7ShadowReplay.load(root,revision,sha(Files.readString(path)));
        return new Phase7ReplayMatrix.Version(verified,graphs);
    }
    private static Phase7ShadowReplay.Graph model(String id,String preferred,
                                                  double uncertainty){
        var branches=new ArrayList<ShadowSearchEngine.Branch>();
        for(String action:List.of("move:attack","move:guard","switch:reserve")){
            String kind=action.substring(0,action.indexOf(':'));
            String name=action.substring(action.indexOf(':')+1);
            double value=action.equals(preferred)?3:1;
            branches.add(new ShadowSearchEngine.Branch(kind,name,List.of(
                    new ShadowSearchEngine.Outcome("unknown",1,value,
                            action.equals(preferred)?uncertainty:0,"leaf"))));
        }
        return new Phase7ShadowReplay.Graph(id,"root",Map.of(
                "root",new ShadowSearchEngine.Position("root",branches,0),
                "leaf",new ShadowSearchEngine.Position("leaf",List.of(),0)));
    }
    private static Map<String,Phase7ShadowReplay.Graph> models(
            Collection<String> ids,String preferred,double uncertainty){
        var graphs=new TreeMap<String,Phase7ShadowReplay.Graph>();
        for(String id:ids)graphs.put(id,model(id,preferred,uncertainty));
        return Map.copyOf(graphs);
    }
    private static Map<String,Phase7ShadowReplay.Graph> illegalModels(
            Collection<String> ids){
        var graphs=new TreeMap<String,Phase7ShadowReplay.Graph>();
        for(String id:ids)graphs.put(id,illegal(id));
        return Map.copyOf(graphs);
    }
    private static Phase7ShadowReplay.Graph illegal(String id){
        return new Phase7ShadowReplay.Graph(id,"root",Map.of(
                "root",new ShadowSearchEngine.Position("root",List.of(
                        new ShadowSearchEngine.Branch("move","unavailable",List.of(
                                new ShadowSearchEngine.Outcome("unknown",1,5,0,"leaf")))),0),
                "leaf",new ShadowSearchEngine.Position("leaf",List.of(),0)));
    }
    private static Phase7RegressionGate.PinnedBaseline baseline(Path root,
            Phase7TrustedIntake.Report reference,String name)throws Exception{
        var limits=new Phase7RegressionGate.Limits(1,0,100,100,100,100);
        var p=Phase7RegressionGate.define(name,reference,limits);
        Phase7RegressionGate.writeLocal(root,p);
        return Phase7RegressionGate.load(root,name,sha(p.json()));
    }
    private static Phase7StratifiedAudit.PinnedPolicy strata(Path root,
            Phase7TrustedIntake.Report reference,String name)throws Exception{
        var rules=new EnumMap<Phase7StratifiedAudit.Group,Phase7StratifiedAudit.Rule>(
                Phase7StratifiedAudit.Group.class);
        for(var group:Phase7StratifiedAudit.Group.values()){
            // Move support is expected; unsupported evidence categories and
            // switch actions are optional but still audited when present.
            boolean required=group==Phase7StratifiedAudit.Group.ACTION_MOVE
                    ||group==Phase7StratifiedAudit.Group.QUALITY_ACCEPTED;
            rules.put(group,new Phase7StratifiedAudit.Rule(required,
                    required?1:0,required?1:0,100,100,100,100,100));
        }
        var p=Phase7StratifiedAudit.define(name,reference,3,10000,10000,rules);
        Phase7StratifiedAudit.writeLocal(root,p);
        return Phase7StratifiedAudit.load(root,name,sha(p.json()));
    }
    private static Phase7ReplayMatrix.Report run(Path root,List<String> ids,
            String kid,String keySha,Map<String,Phase7Benchmark.Scenario> scenarios,
            Phase7RegressionGate.PinnedBaseline gate,
            Phase7StratifiedAudit.PinnedPolicy audit,
            Phase7ReplayMatrix.Version... versions){
        return Phase7ReplayMatrix.evaluateLocal(root,ids,scenarios,kid,keySha,
                gate,audit,List.of(versions));
    }
    private static Phase7ReplayMatrix.Status status(Phase7ReplayMatrix.Report report) {
        return report.versions().getFirst().status();
    }

    public static void run(Path root,String keyId,String keySha,String artifact,
            Map<String,Phase7Benchmark.Scenario> scenarios,List<String> signedBundles,
            Phase7ShadowReplay.PinnedPolicy existingReference,
            Map<String,Phase7ShadowReplay.Graph> referenceGraphs) {
        try {
            check(scenarios.size()==3,"unique synthetic reference states");
            // The previously tested version 7.8 policy is the actual reference.
            var v0=new Phase7ReplayMatrix.Version(existingReference,referenceGraphs);
            var base=Phase7ShadowReplay.replayLocal(root,signedBundles,scenarios,keyId,
                    keySha,v0.policy(),v0.graphs());
            check(base.custodyReport().corpus().coverage().accepted()==3,
                    "reference uses three reviewed synthetic originals");
            var gate=baseline(root,base.custodyReport(),"matrix_gate");
            var audit=strata(root,base.custodyReport(),"matrix_strata");
            var unchanged=install(root,"matrix_equivalent",artifact,921,20,8,0,
                    referenceGraphs,scenarios);
            var changed=install(root,"matrix_better",artifact,921,20,8,0,
                    models(scenarios.keySet(),"move:attack",0),scenarios);
            var versions=List.of(v0,unchanged,changed);
            var report=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,
                    versions.toArray(new Phase7ReplayMatrix.Version[0]));
            check(report.passed()&&report.versions().size()==2,
                    "cross-version two-candidate deterministic robust matrix PASS");
            check(report.versions().getFirst().changes().changedActions()==0
                    && report.versions().get(1).changes().changedActions()==3,
                    "counts exact old/new legal action changes on same reviewed cohort");
            check(report.versions().get(1).changes().legacyAgreementGain()==3
                    && report.versions().get(1).changes().legacyAgreementLoss()==0,
                    "explicit agreement attribution, never a tactical win-rate improvement");
            check(report.versions().stream().allMatch(v->v.aggregateGate().equals("PASS")
                    &&v.strataGate().equals("PASS")),"both historical gates invoked and required");
            var same=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,
                    v0,unchanged,changed);
            check(report.json().equals(same.json())
                    &&report.reportFingerprint().equals(same.reportFingerprint()),
                    "repeated signed-custody differential replay is byte-reproducible");
            check(JsonParser.parseString(report.json()).getAsJsonObject()
                    .get("allPassed").getAsBoolean(),"machine-readable multi-version report");
            var shuffled=new ArrayList<>(signedBundles);Collections.reverse(shuffled);
            check(run(root,shuffled,keyId,keySha,scenarios,gate,audit,
                    v0,unchanged,changed).json().equals(report.json()),
                    "bundle-order changes do not alter comparison or fingerprint");
            check(!report.json().contains("Mew")&&!report.json().contains(SECRET)
                    &&!report.json().contains("synthetic0")
                    &&!report.json().contains("move:attack")
                    &&!report.json().contains("switch:reserve")
                    &&!report.json().contains("01010101"),
                    "matrix report exposes no private actor/capture/action content");
            check(report.json().contains("no-real-win-rate")
                    &&report.json().contains("not-minecraft"),
                    "no real performance or Minecraft acceptance claims");

            var budget=install(root,"matrix_budget",artifact,921,1,8,0,
                    referenceGraphs,scenarios);
            var exhausted=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,budget);
            check(status(exhausted)==Phase7ReplayMatrix.Status.SEARCH_EXHAUSTION_REGRESSION
                    &&exhausted.versions().getFirst().changes().newlyExhausted()==3
                    &&exhausted.versions().getFirst().changes().newAbstentions()==3,
                    "newly budget-exhausted Shadow decisions fail closed");
            check(!exhausted.passed(),"search regression cannot be called robust");

            var illegal=install(root,"matrix_illegal",artifact,921,20,8,0,
                    illegalModels(scenarios.keySet()),scenarios);
            var unsafe=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,illegal);
            check(status(unsafe)==Phase7ReplayMatrix.Status.NEW_ILLEGAL_DECISION
                    &&unsafe.versions().getFirst().changes().newlyIllegal()==3,
                    "illegal actions cannot be hidden by aggregate agreement changes");

            var higherUncertainty=install(root,"matrix_uncertainty",artifact,921,20,8,0,
                    models(scenarios.keySet(),"switch:reserve",.25),scenarios);
            var uncertain=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,
                    v0,higherUncertainty);
            check(status(uncertain)==Phase7ReplayMatrix.Status.UNCERTAINTY_REGRESSION
                    &&uncertain.versions().getFirst().changes().uncertaintyIncreases()==3,
                    "declared public conditional uncertainty increase detected");
            var higherPenalty=install(root,"matrix_penalty",artifact,921,20,8,.5,
                    referenceGraphs,scenarios);
            var penalty=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,higherPenalty);
            check(status(penalty)==Phase7ReplayMatrix.Status.UNCERTAINTY_REGRESSION
                    &&penalty.versions().getFirst().changes().penaltyIncreases()==3,
                    "changed uncertainty aversion is reported and gated");

            var truncated=install(root,"matrix_trace",artifact,921,20,1,0,
                    referenceGraphs,scenarios);
            var incomplete=run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,truncated);
            check(status(incomplete)==Phase7ReplayMatrix.Status.INCOMPLETE_TRACE,
                    "short traces cannot support comprehensive paired attribution");

            var wrongSeed=install(root,"matrix_seed",artifact,922,20,8,0,
                    referenceGraphs,scenarios);
            reject(()->run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,wrongSeed),
                    "different policy seed refuses version pairing");
            var wrongArtifact=install(root,"matrix_artifact","f".repeat(64),921,20,8,0,
                    referenceGraphs,scenarios);
            reject(()->run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,wrongArtifact),
                    "different pinned installed build refuses evidence pairing");
            reject(()->run(root,signedBundles,keyId,keySha,scenarios,gate,audit,v0,v0),
                    "duplicate policy revisions refused");
            reject(()->run(root,List.of(),keyId,keySha,scenarios,gate,audit,v0,changed),
                    "zero approved observations cannot create a matrix");
            reject(()->run(root,signedBundles,keyId,"0".repeat(64),scenarios,
                    gate,audit,v0,changed),"unknown custody key cannot establish reference");

            // Explicit source drift: repinning 7.6 to a DIFFERENT reference
            // must block candidate comparison even if its apparent agreement is high.
            var newerEval=Phase7ShadowReplay.replayLocal(root,signedBundles,scenarios,
                    keyId,keySha,changed.policy(),changed.graphs());
            var wrongBaseline=baseline(root,newerEval.custodyReport(),"matrix_wrong_gate");
            var wrongGate=run(root,signedBundles,keyId,keySha,scenarios,
                    wrongBaseline,audit,v0,changed);
            check(status(wrongGate)==Phase7ReplayMatrix.Status.GLOBAL_REGRESSION
                    &&wrongGate.versions().getFirst().aggregateGate().equals("BASELINE_MISMATCH"),
                    "aggregate baseline mismatch blocks independent replay claims");

            var wrongStrata=strata(root,newerEval.custodyReport(),"matrix_wrong_strata");
            var wrongAudit=run(root,signedBundles,keyId,keySha,scenarios,
                    gate,wrongStrata,v0,changed);
            check(status(wrongAudit)==Phase7ReplayMatrix.Status.STRATIFIED_REGRESSION
                    &&wrongAudit.versions().getFirst().strataGate()
                            .equals("POLICY_BINDING_INVALID"),
                    "stratified policy from different evidence cannot bypass trust");

            // A tiny cohort is evaluated internally but exported statistics
            // are suppressed to avoid revealing a single decision.
            List<String> tiny=List.of(signedBundles.getFirst());
            var tinyEval=Phase7ShadowReplay.replayLocal(root,tiny,scenarios,
                    keyId,keySha,v0.policy(),v0.graphs());
            var tinyGate=baseline(root,tinyEval.custodyReport(),"matrix_tiny_gate");
            var tinyStrata=strata(root,tinyEval.custodyReport(),"matrix_tiny_strata");
            var tinyReport=run(root,tiny,keyId,keySha,scenarios,
                    tinyGate,tinyStrata,v0,changed);
            var deltas=JsonParser.parseString(tinyReport.json()).getAsJsonObject()
                    .getAsJsonArray("versions").get(0).getAsJsonObject()
                    .getAsJsonObject("deltas");
            check(deltas.get("suppressed").getAsBoolean()&&!deltas.has("changedActions")
                    &&!deltas.has("accepted"),
                    "single-person/small-cell version differential stays masked");
            check(tinyReport.versions().getFirst().changes().changedActions()==1,
                    "suppression never disables internal evidence gates");

            // Test failure to support any signed Legacy evidence without
            // touching or exposing the original local private bundle.
            reject(()->run(root,List.of("missing_bundle"),keyId,keySha,scenarios,
                    gate,audit,v0,changed),"unreviewed or missing bundle cannot be benchmarked");
            check(Phase7ReplayMatrix.SCHEMA.equals("phase7.9.v1"),
                    "explicit schema version maintained");
            System.out.println("Phase 7.9 synthetic cross-version matrix regressions PASS: "
                    +checks+" checks (genuine Legacy approvals: 0; Minecraft runs: 0)");
        } catch(Exception e) {
            throw new AssertionError("Phase 7.9 adversarial matrix setup failed",e);
        }
    }
}
