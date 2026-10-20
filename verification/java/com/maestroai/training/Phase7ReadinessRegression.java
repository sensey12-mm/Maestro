package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Phase 7.12 END-TO-END adversarial tests. Uses the existing Phase 7.9
 * Ed25519-signed Legacy corpus and independent synthetic Phase 7.10/7.11
 * signatures. No real Phase 4 source or integrated intelligent AI is claimed.
 */
public final class Phase7ReadinessRegression {
    private Phase7ReadinessRegression() {}
    private static int checks;
    private static final String SECRET="PRIVATE_OPPONENT_UNREVEALED_DO_NOT_EXPORT";
    private static void check(boolean truth,String why){
        if(!truth)throw new AssertionError("Phase 7.12: "+why);
        checks++;
    }
    private static String sha(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static String sha(byte[] value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value));}
        catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static KeyPair pair()throws Exception {
        var gen=KeyPairGenerator.getInstance("Ed25519");
        return gen.generateKeyPair();
    }
    private static Phase7Benchmark.Scenario next(Phase7Benchmark.Scenario original,
            String scenarioId,int turn){
        var s=original.snapshot();
        var snapshot=new BattleSnapshot(turn,s.self(),s.opponent(),s.selfBench(),
                s.opponentBench(),s.legalMoves(),s.legalSwitches(),
                s.knownOpponentMoveCounts(),s.opponentTerastallized(),s.opponentTeraType());
        return new Phase7Benchmark.Scenario(scenarioId,1,snapshot,original.criteria(),
                new Phase7Benchmark.Evidence(true,true,true,turn),null);
    }
    private static Map<String,Phase7Benchmark.Scenario> path(
            Phase7Benchmark.Scenario original,String prefix){
        var result=new TreeMap<String,Phase7Benchmark.Scenario>();
        result.put(original.id(),original);
        for(int t=3;t<=5;t++){
            var scenario=next(original,prefix+"_turn_"+t,t);
            result.put(scenario.id(),scenario);
        }
        return Map.copyOf(result);
    }
    private static Phase7ShadowReplay.Graph graph(String id,String preferred){
        var moves=new ArrayList<ShadowSearchEngine.Branch>();
        for(String action:List.of("move:attack","move:guard","switch:reserve")){
            String kind=action.substring(0,action.indexOf(':'));
            String name=action.substring(action.indexOf(':')+1);
            moves.add(new ShadowSearchEngine.Branch(kind,name,List.of(
                    new ShadowSearchEngine.Outcome("observed",1,
                            action.equals(preferred)?5:1,0,"leaf"))));
        }
        return new Phase7ShadowReplay.Graph(id,"root",Map.of(
                "root",new ShadowSearchEngine.Position("root",moves,0),
                "leaf",new ShadowSearchEngine.Position("leaf",List.of(),0)));
    }
    private static Phase7SelfPlay.Participant participant(Path repo,
            String revision,Map<String,Phase7Benchmark.Scenario> scenarios,
            String artifact,long seed)throws Exception{
        var graphs=new TreeMap<String,Phase7ShadowReplay.Graph>();
        for(String id:scenarios.keySet())graphs.put(id,graph(id,"move:attack"));
        var policy=Phase7ShadowReplay.define(revision,artifact,seed,graphs,scenarios,32,16,0);
        Path local=Phase7ShadowReplay.writeLocal(repo,policy);
        var verified=Phase7ShadowReplay.load(repo,revision,sha(Files.readString(local)));
        return new Phase7SelfPlay.Participant(verified,graphs,scenarios);
    }
    private static Phase7SelfPlay.Transitions transitions(
            Phase7Benchmark.Scenario root,String left,String rightRoot,String rightPrefix){
        var nodes=new TreeMap<String,Phase7SelfPlay.Node>();
        for(int i=0;i<=3;i++){
            int turn=2+i;
            nodes.put("step"+i,new Phase7SelfPlay.Node("step"+i,
                    i==0?root.id():left+"_turn_"+turn,
                    i==0?rightRoot:rightPrefix+"_turn_"+turn,i==3));
        }
        var edges=new ArrayList<Phase7SelfPlay.Edge>();
        for(int i=0;i<3;i++)
            edges.add(new Phase7SelfPlay.Edge("step"+i,"move:attack",
                    "move:attack","step"+(i+1),sha("synthetic-approved-event-"+i),
                    Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION));
        return new Phase7SelfPlay.Transitions("step0",nodes,edges);
    }
    private static Phase7SelfPlay.PinnedPolicy selfPolicy(Path root,
            Phase7SelfPlay.Transitions t,Phase7SelfPlay.Participant first,
            Phase7SelfPlay.Participant second,long seed)throws Exception {
        var p=Phase7SelfPlay.define("phase712_self",t,first,second,seed,3,24);
        var file=Phase7SelfPlay.writeLocal(root,p);
        return Phase7SelfPlay.load(root,p.revision(),sha(Files.readString(file)));
    }
    private static Phase7MechanicsAdapter.Bundle bundle(
            Phase7SelfPlay.Transitions t,Phase7SelfPlay.Participant first,
            Phase7SelfPlay.Participant second,
            Phase7MechanicsAdapter.Source origin,
            Phase7MechanicsAdapter.Verdict verdict,String artifact){
        List<Phase7MechanicsAdapter.Entry> rows=new ArrayList<>();
        for(var edge:t.edges())
            rows.add(new Phase7MechanicsAdapter.Entry(
                    Phase7MechanicsAdapter.edgeSha256(edge),
                    Phase7MechanicsAdapter.group(edge),verdict,
                    sha("synthetic-phase4-receipt-"+Phase7MechanicsAdapter.edgeSha256(edge))));
        return new Phase7MechanicsAdapter.Bundle(Phase7MechanicsAdapter.BUNDLE_SCHEMA,
                1,"phase712_mechanics",origin,"b".repeat(64),"c".repeat(64),
                "PHASE4_PUBLIC_MECHANICS_V1",artifact,
                Phase7SelfPlay.transitionsSha256(t,first.scenarios(),second.scenarios()),
                rows);
    }
    private static Phase7MechanicsAdapter.PinnedPolicy mechanicsPolicy(
            Path root,Phase7MechanicsAdapter.Bundle evidence,String pin)throws Exception{
        var p=Phase7MechanicsAdapter.define("phase712_mechanics",evidence,pin,
                new Phase7MechanicsAdapter.Gate(3,0,0,0,3,0,0));
        var file=Phase7MechanicsAdapter.writeLocal(root,p);
        return Phase7MechanicsAdapter.load(root,p.revision(),sha(Files.readString(file)));
    }
    private static byte[] signature(KeyPair keys,byte[] payload)throws Exception{
        var s=Signature.getInstance("Ed25519");
        s.initSign(keys.getPrivate());s.update(payload);return s.sign();
    }
    private static Phase7SelfPlay.Attestation signTransitions(KeyPair keys,String digest)
            throws Exception{
        return new Phase7SelfPlay.Attestation(Base64.getEncoder().encodeToString(
                keys.getPublic().getEncoded()),Base64.getEncoder().encodeToString(
                signature(keys,Phase7SelfPlay.signaturePayload(digest))));
    }
    private static Phase7MechanicsAdapter.Attestation signMechanics(KeyPair keys,
            Phase7MechanicsAdapter.Bundle bundle)throws Exception{
        return new Phase7MechanicsAdapter.Attestation(Base64.getEncoder().encodeToString(
                keys.getPublic().getEncoded()),Base64.getEncoder().encodeToString(
                signature(keys,Phase7MechanicsAdapter.signaturePayload(
                        Phase7MechanicsAdapter.bundleSha256(bundle)))));
    }
    private record Fixture(Path root,List<String> bundles,
            Map<String,Phase7Benchmark.Scenario> scenarios,String custodianId,
            String custodianSha,Phase7RegressionGate.PinnedBaseline aggregate,
            Phase7StratifiedAudit.PinnedPolicy strata,
            List<Phase7ReplayMatrix.Version> versions,
            Phase7SelfPlay.Transitions transitions,Phase7SelfPlay.Participant first,
            Phase7SelfPlay.Participant second,Phase7SelfPlay.PinnedPolicy self,
            Phase7SelfPlay.Attestation transitionSig,String transitionSha,
            Phase7MechanicsAdapter.Bundle mechanics,
            Phase7MechanicsAdapter.Attestation mechanicsSig,
            Phase7MechanicsAdapter.PinnedPolicy mechanicsPolicy) {}
    private static Phase7Readiness.Input input(Fixture f,
            List<String> bundles,Map<String,Phase7Benchmark.Scenario> scenarios,
            List<Phase7ReplayMatrix.Version> versions,
            Phase7SelfPlay.Participant first,Phase7SelfPlay.Participant second,
            Phase7SelfPlay.PinnedPolicy self,
            Phase7SelfPlay.Attestation transitionSig,String transitionSha,
            Phase7MechanicsAdapter.Bundle mechanics,
            Phase7MechanicsAdapter.Attestation mechanicsSig,
            Phase7MechanicsAdapter.PinnedPolicy mechanicsPolicy,
            Phase7Readiness.EvidenceTier tier){
        return new Phase7Readiness.Input(f.root(),bundles,scenarios,
                f.custodianId(),f.custodianSha(),f.aggregate(),f.strata(),
                versions,f.transitions(),first,second,self,transitionSig,transitionSha,
                mechanics,mechanicsSig,mechanicsPolicy,tier);
    }
    private static Phase7Readiness.Input normal(Fixture f,Phase7Readiness.EvidenceTier tier){
        return input(f,f.bundles(),f.scenarios(),f.versions(),f.first(),f.second(),
                f.self(),f.transitionSig(),f.transitionSha(),f.mechanics(),
                f.mechanicsSig(),f.mechanicsPolicy(),tier);
    }

    /**
     * Called by Phase 7.9's EXISTING end-to-end signed local bundle fixture.
     * This tests real custody → Shadow replay → matrix → mechanics → self-play.
     */
    public static void run(Path root,String custodianId,String custodianSha,
            String artifact,Map<String,Phase7Benchmark.Scenario> legacyScenarios,
            List<String> signedBundles,Phase7RegressionGate.PinnedBaseline aggregate,
            Phase7StratifiedAudit.PinnedPolicy strata,
            Phase7ReplayMatrix.Version baseline,Phase7ReplayMatrix.Version candidate){
        try{
            var firstScenario=legacyScenarios.get(new TreeSet<>(legacyScenarios.keySet()).first());
            var firstStates=path(firstScenario,"first");
            var original=legacyScenarios.get(new TreeSet<>(legacyScenarios.keySet()).last());
            var secondRoot=next(original,"second_root",2);
            var secondStates=path(secondRoot,"second");
            var first=participant(root,"phase712_first",firstStates,artifact,921);
            var second=participant(root,"phase712_second",secondStates,artifact,921);
            var t=transitions(firstScenario,"first",secondRoot.id(),"second");
            var self=selfPolicy(root,t,first,second,921);
            KeyPair mechanicSigner=pair();
            KeyPair transitionSigner=pair();
            var mechanics=bundle(t,first,second,
                    Phase7MechanicsAdapter.Source.PHASE4_REVIEWED_PUBLIC_V1,
                    Phase7MechanicsAdapter.Verdict.REVIEWED_PUBLIC,artifact);
            var policy=mechanicsPolicy(root,mechanics,sha(mechanicSigner.getPublic().getEncoded()));
            var mSignature=signMechanics(mechanicSigner,mechanics);
            var tSignature=signTransitions(transitionSigner,self.value().transitionsSha256());
            var fixture=new Fixture(root,List.copyOf(signedBundles),legacyScenarios,
                    custodianId,custodianSha,aggregate,strata,List.of(baseline,candidate),
                    t,first,second,self,tSignature,sha(transitionSigner.getPublic().getEncoded()),
                    mechanics,mSignature,policy);
            var normal=normal(fixture,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            var report=Phase7Readiness.evaluate(normal);
            check(report.status()==Phase7Readiness.Status.SYNTHETIC_ONLY,
                    "synthetic signed chain cannot be promoted to verified gameplay");
            check(report.stages().get(Phase7Readiness.Stage.TRUSTED_LEGACY)==Phase7Readiness.Gate.PASS
                    &&report.stages().get(Phase7Readiness.Stage.VERSION_MATRIX)==Phase7Readiness.Gate.PASS
                    &&report.stages().get(Phase7Readiness.Stage.PUBLIC_MECHANICS)==Phase7Readiness.Gate.PASS
                    &&report.stages().get(Phase7Readiness.Stage.SIGNED_OBSERVATIONAL_SELF_PLAY)
                    ==Phase7Readiness.Gate.SYNTHETIC_ONLY,
                    "all five real offline verification stages run end to end");
            check(report.authenticatedCases()==3&&report.reviewedPublicTransitions()==3
                    &&report.consistentSourceBindings()==1,
                    "signed Legacy/public mechanics root-state and source identities align");
            check(report.json().equals(Phase7Readiness.evaluate(normal).json()),
                    "exact-byte deterministic end-to-end report");
            check(report.chainFingerprint().matches("[0-9a-f]{64}")
                    &&JsonParser.parseString(report.json()).isJsonObject(),
                    "reproducible fingerprinted machine-readable readiness");
            check(!report.integrated()&&report.json().contains("\"integrated\":false")
                    &&report.json().contains("phase4-reviewed-mechanics")
                    &&report.json().contains("phase6-integrated-search"),
                    "unintegrated Phase 4–6 prerequisites are explicit and non-overridable");
            check(!report.json().contains("move:attack")
                    &&!report.json().contains("Mew")
                    &&!report.json().contains("matrix_signed")
                    &&!report.json().contains("synthetic20")
                    &&!report.json().contains(SECRET),
                    "privacy-safe reporting never exports raw actors or capture IDs");
            check(report.json().contains("not-minecraft")&&report.json().contains("not-win-rate"),
                    "no fake Minecraft or simulated success claim");
            check(report.custodyDigest()!=null&&report.shadowDigest()!=null
                    &&report.matrixDigest()!=null&&report.mechanicsDigest()!=null
                    &&report.observationalDigest()!=null,
                    "all five evidence chain stages carry deterministic content fingerprints");
            var external=Phase7Readiness.evaluate(normal(fixture,
                    Phase7Readiness.EvidenceTier.EXTERNALLY_REVIEWED_OFFLINE));
            check(external.status()==Phase7Readiness.Status.AWAITING_INTEGRATED_PHASES_4_5_6
                    &&!external.integrated(),
                    "even externally approved offline evidence does not bypass integration gate");
            var empty=input(fixture,List.of("missing_private_bundle"),legacyScenarios,
                    fixture.versions(),first,second,self,tSignature,fixture.transitionSha(),
                    mechanics,mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            var rejected=Phase7Readiness.evaluate(empty);
            check(rejected.status()==Phase7Readiness.Status.CUSTODY_REJECTED
                    &&rejected.stages().get(Phase7Readiness.Stage.VERSION_MATRIX)
                        ==Phase7Readiness.Gate.NOT_RUN,
                    "untrusted Legacy bundle blocks all downstream stages");
            var partial=input(fixture,List.of(signedBundles.getFirst()),legacyScenarios,
                    fixture.versions(),first,second,self,tSignature,fixture.transitionSha(),
                    mechanics,mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            var single=Phase7Readiness.evaluate(partial);
            check(single.json().contains("\"countsSuppressed\":true")
                    &&!single.json().contains("\"authenticatedCases\""),
                    "low-cell single receipt can never leak exact source count");

            var wrongFirst=participant(root,"phase712_wrong_first",
                    path(original,"other_first"),artifact,921);
            var wrongRoot=input(fixture,signedBundles,legacyScenarios,fixture.versions(),
                    wrongFirst,second,self,tSignature,fixture.transitionSha(),
                    mechanics,mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            check(Phase7Readiness.evaluate(wrongRoot).status()
                    ==Phase7Readiness.Status.CONFIGURATION_INCOMPATIBLE,
                    "modified public scenario/participant fingerprint rejected pre-custody");

            var staleMatrix=List.of(baseline,new Phase7ReplayMatrix.Version(
                    candidate.policy(),candidate.graphs()));
            var wrongKey=input(fixture,signedBundles,legacyScenarios,staleMatrix,
                    first,second,self,tSignature,fixture.transitionSha(),mechanics,
                    mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            check(Phase7Readiness.evaluate(wrongKey).status()==Phase7Readiness.Status.SYNTHETIC_ONLY,
                    "legitimate same-pinned version projections remain stable");

            var badModelGraphs=new TreeMap<>(candidate.graphs());
            for(String id:badModelGraphs.keySet()){
                badModelGraphs.put(id,graph(id,"move:guard"));break;
            }
            var altered=Phase7ShadowReplay.define("phase712_regressing",artifact,921,
                    badModelGraphs,legacyScenarios,20,10,.9);
            Path modifiedFile=Phase7ShadowReplay.writeLocal(root,altered);
            var modifiedPin=Phase7ShadowReplay.load(root,altered.revision(),
                    sha(Files.readString(modifiedFile)));
            var regressedVersions=List.of(baseline,
                    new Phase7ReplayMatrix.Version(modifiedPin,badModelGraphs));
            var regressedInput=input(fixture,signedBundles,legacyScenarios,regressedVersions,
                    first,second,self,tSignature,fixture.transitionSha(),mechanics,
                    mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            check(Phase7Readiness.evaluate(regressedInput).status()
                    ==Phase7Readiness.Status.CROSS_VERSION_BLOCKED,
                    "Phase 7.9 disagreement/uncertainty regression blocks mechanics promotion");

            var fake=new Phase7MechanicsAdapter.Bundle(
                    Phase7MechanicsAdapter.BUNDLE_SCHEMA,1,"phase712_mechanics",
                    Phase7MechanicsAdapter.Source.SYNTHETIC_TEST_ONLY,
                    mechanics.sourceCommitSha256(),mechanics.sourceCorpusSha256(),
                    mechanics.sourceContract(),mechanics.artifactSha256(),
                    mechanics.transitionsSha256(),mechanics.entries());
            var fakeInput=input(fixture,signedBundles,legacyScenarios,fixture.versions(),
                    first,second,self,tSignature,fixture.transitionSha(),fake,
                    mSignature,policy,Phase7Readiness.EvidenceTier.EXTERNALLY_REVIEWED_OFFLINE);
            var refused=Phase7Readiness.evaluate(fakeInput);
            check(refused.status()==Phase7Readiness.Status.MECHANICS_REJECTED
                    &&refused.stages().get(Phase7Readiness.Stage.SIGNED_OBSERVATIONAL_SELF_PLAY)
                        ==Phase7Readiness.Gate.NOT_RUN,
                    "synthetic-marked mechanics rejected even if tier falsely claims external review");

            var unsupportedEntries=new ArrayList<>(mechanics.entries());
            var e=unsupportedEntries.getFirst();
            unsupportedEntries.set(0,new Phase7MechanicsAdapter.Entry(e.edgeSha256(),
                    e.group(),Phase7MechanicsAdapter.Verdict.UNSUPPORTED,
                    e.mechanicsReceiptSha256()));
            var unsupported=new Phase7MechanicsAdapter.Bundle(
                    Phase7MechanicsAdapter.BUNDLE_SCHEMA,1,"phase712_mechanics",
                    mechanics.source(),mechanics.sourceCommitSha256(),
                    mechanics.sourceCorpusSha256(),mechanics.sourceContract(),
                    mechanics.artifactSha256(),mechanics.transitionsSha256(),unsupportedEntries);
            var unsupportedInput=input(fixture,signedBundles,legacyScenarios,fixture.versions(),
                    first,second,self,tSignature,fixture.transitionSha(),unsupported,
                    mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            check(Phase7Readiness.evaluate(unsupportedInput).status()
                    ==Phase7Readiness.Status.MECHANICS_REJECTED,
                    "unsupported reviewed mechanics cannot advance to self-play");
            var noTransitionSignature=input(fixture,signedBundles,legacyScenarios,
                    fixture.versions(),first,second,self,null,fixture.transitionSha(),
                    mechanics,mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            var noSig=Phase7Readiness.evaluate(noTransitionSignature);
            check(noSig.status()==Phase7Readiness.Status.OBSERVATIONAL_REPLAY_BLOCKED
                    &&noSig.stages().get(Phase7Readiness.Stage.PUBLIC_MECHANICS)
                    ==Phase7Readiness.Gate.PASS,
                    "missing transition signature fails only after mechanics checks");

            var mismatchedPolicy=Phase7SelfPlay.define("phase712_mismatch",
                    t,first,second,921,1,24);
            Path mismatchFile=Phase7SelfPlay.writeLocal(root,mismatchedPolicy);
            var badSelf=Phase7SelfPlay.load(root,mismatchedPolicy.revision(),
                    sha(Files.readString(mismatchFile)));
            var tinyHorizon=input(fixture,signedBundles,legacyScenarios,fixture.versions(),
                    first,second,badSelf,tSignature,fixture.transitionSha(),
                    mechanics,mSignature,policy,Phase7Readiness.EvidenceTier.SYNTHETIC_TEST_ONLY);
            check(Phase7Readiness.evaluate(tinyHorizon).status()
                    ==Phase7Readiness.Status.OBSERVATIONAL_REPLAY_BLOCKED,
                    "bounded incomplete simulation cannot be reported as integration ready");
            System.out.println("Phase 7.12 synthetic end-to-end readiness regressions PASS: "
                    +checks+" checks (real integrated Phases 4–6: 0; Minecraft: 0)");
        }catch(Exception failure){throw new AssertionError("Phase 7.12 E2E fixture failed",failure);}
    }
}
