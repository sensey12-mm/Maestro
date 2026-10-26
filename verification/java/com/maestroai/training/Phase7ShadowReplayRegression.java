package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Phase 7.8 end-to-end synthetic signed local evidence and offline search.
 * No real Legacy choice, player data, calibrated simulator or Minecraft test.
 */
public final class Phase7ShadowReplayRegression {
    private Phase7ShadowReplayRegression(){}
    private static int checks;
    private static final String PRIVATE="PRIVATE_OPPONENT_NEVER_SERIALIZE";
    private static final String ARTIFACT="a".repeat(64);
    private static final String KEY_ID="shadow_replay_test";
    private static final UUID BATTLE=UUID.fromString("01010101-1111-2222-3333-444444444444");
    private static final String FIRST="update\n|switch|p1a: Own|Mew, L100|100/100\n"
            +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1";
    private static final String SECOND="update\n|turn|2";
    private static void check(boolean pass,String why) {
        if(!pass)throw new AssertionError("Phase 7.8: "+why);
        checks++;
    }
    private static void reject(Runnable call,String why) {
        boolean caught=false;
        try{call.run();}catch(RuntimeException expected){caught=true;}
        check(caught,why);
    }
    private static String digest(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static String digest(byte[] input) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));}
        catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private record Fixture(Path root,KeyPair signer,String keyPin,
                           Phase7Benchmark.Scenario scenario) {}
    private static Fixture create() throws Exception {
        Path root=Files.createTempDirectory("phase78-replay-");
        Path trust=root.resolve("run/phase7-corpus/trust");
        Files.createDirectories(trust);
        Files.createDirectories(root.resolve("run/phase7-corpus/private-input"));
        var keyGen=KeyPairGenerator.getInstance("Ed25519");
        KeyPair signer=keyGen.generateKeyPair();
        byte[] pub=signer.getPublic().getEncoded();
        Files.writeString(trust.resolve(KEY_ID+".pub"),
                Base64.getEncoder().encodeToString(pub));
        var scenario=Phase7Benchmark.parseFixture(Files.readString(
                Path.of("verification/benchmarks/phase7_1_synthetic.json")))
                .scenarios().getFirst();
        return new Fixture(root,signer,digest(pub),scenario);
    }
    private static List<String> history(){return List.of(FIRST,SECOND);}
    private static String capture(Fixture f,String slug,int index,String action,String artifact)
            throws Exception {
        return capture(f,slug,index,action,artifact,f.scenario());
    }
    private static String capture(Fixture f,String slug,int index,String action,String artifact,
                                  Phase7Benchmark.Scenario scenario) throws Exception {
        String captureId="synthetic"+index;
        String own=digest("synthetic-private-own-request-"+index);
        String battle=new UUID(1,index+1).toString();
        List<String> history=history();
        String publicSha=Phase7LegacyImport.publicDigest(
                PublicProtocolFilter.publicEvents(history));
        String fp=Phase7Benchmark.stateFingerprint(scenario);
        String receiptHash=Phase7LegacyImport.sha256(List.of(
                "LEGACY_CHOOSE_RETURN",artifact,own,publicSha,fp,
                "2",action,captureId));
        JsonObject manifest=new JsonObject();
        manifest.addProperty("schema",Phase7TrustedIntake.SCHEMA);
        manifest.addProperty("scenarioId",scenario.id());
        manifest.addProperty("scenarioFingerprint",fp);
        manifest.addProperty("captureId",captureId);
        manifest.addProperty("battleId",battle);
        manifest.addProperty("turn",2);manifest.addProperty("action",action);
        manifest.addProperty("artifactSha256",artifact);
        manifest.addProperty("ownRequestSha256",own);
        manifest.addProperty("publicSha256",publicSha);
        manifest.addProperty("rawHistorySha256",Phase7LegacyImport.sha256(history));
        manifest.addProperty("choiceReceiptSha256",receiptHash);
        JsonArray checkpoints=new JsonArray();
        Path dir=f.root().resolve("run/phase7-corpus/private-input/"+slug);
        Files.createDirectories(dir);
        int i=1;
        for(List<String> entries:List.of(List.of(FIRST),history)) {
            String file=String.format(Locale.ROOT,"checkpoint-%02d.json",i++);
            JsonObject j=new JsonObject();
            JsonArray chunks=new JsonArray();
            entries.forEach(chunks::add);j.add("chunks",chunks);
            String data=j.toString();Files.writeString(dir.resolve(file),data);
            JsonObject item=new JsonObject();item.addProperty("file",file);
            item.addProperty("sha256",digest(data));checkpoints.add(item);
        }
        manifest.add("checkpoints",checkpoints);
        Files.writeString(dir.resolve("manifest.json"),manifest.toString());
        sign(f,dir);
        return slug;
    }
    private static void sign(Fixture f,Path dir) throws Exception {
        String text=Files.readString(dir.resolve("manifest.json"));
        String sha=digest(text);
        Signature signer=Signature.getInstance("Ed25519");
        signer.initSign(f.signer().getPrivate());
        signer.update(Phase7TrustedIntake.signaturePayload(sha));
        JsonObject signed=new JsonObject();
        signed.addProperty("schema",Phase7TrustedIntake.CUSTODY);
        signed.addProperty("keyId",KEY_ID);
        signed.addProperty("manifestSha256",sha);
        signed.addProperty("signature",Base64.getEncoder().encodeToString(signer.sign()));
        Files.writeString(dir.resolve("custody.json"),signed.toString());
    }
    private static ShadowSearchEngine.Branch branch(String action,double utility) {
        String kind=action.substring(0,action.indexOf(':'));
        String name=action.substring(action.indexOf(':')+1);
        return new ShadowSearchEngine.Branch(kind,name,List.of(
                new ShadowSearchEngine.Outcome("unknown",1,utility,0,"leaf")));
    }
    private static Phase7ShadowReplay.Graph graph(String scenarioId,String preferred) {
        double attack="move:attack".equals(preferred)?3:1;
        double guard="move:guard".equals(preferred)?3:1;
        double switchValue="switch:reserve".equals(preferred)?3:1;
        var root=new ShadowSearchEngine.Position("root",List.of(
                branch("move:guard",guard),branch("switch:reserve",switchValue),
                branch("move:attack",attack)),0);
        var leaf=new ShadowSearchEngine.Position("leaf",List.of(),0);
        return new Phase7ShadowReplay.Graph(scenarioId,"root",Map.of("root",root,"leaf",leaf));
    }
    private static Phase7ShadowReplay.Graph illegalGraph(String id) {
        var root=new ShadowSearchEngine.Position("root",List.of(
                branch("move:private_unsupported",3)),0);
        return new Phase7ShadowReplay.Graph(id,"root",Map.of(
                "root",root,"leaf",new ShadowSearchEngine.Position("leaf",List.of(),0)));
    }
    private static void cleanup(Path root)throws Exception {
        try(var walk=Files.walk(root)){
            walk.sorted(Comparator.reverseOrder()).forEach(p->{
                try{Files.deleteIfExists(p);}
                catch(java.io.IOException failure){throw new AssertionError("temp cleanup",failure);}
            });
        }
    }
    private static Phase7ShadowReplay.PinnedPolicy install(Fixture f,String revision,
            Map<String,Phase7ShadowReplay.Graph> graphs,
            Map<String,Phase7Benchmark.Scenario> scenarios,int budget,int limit)
            throws Exception {
        var policy=Phase7ShadowReplay.define(revision,ARTIFACT,921,graphs,scenarios,
                budget,limit,0);
        Path file=Phase7ShadowReplay.writeLocal(f.root(),policy);
        String sha=digest(Files.readString(file)); // Synthetic test pin, not genuine custody.
        return Phase7ShadowReplay.load(f.root(),revision,sha);
    }
    private static Phase7ShadowReplay.Evaluation run(Fixture f,List<String> bundles,
            Phase7ShadowReplay.PinnedPolicy pinned,
            Map<String,Phase7ShadowReplay.Graph> graphs) {
        return Phase7ShadowReplay.replayLocal(f.root(),bundles,
                Map.of(f.scenario().id(),f.scenario()),KEY_ID,f.keyPin(),pinned,graphs);
    }
    public static void run() {
        Fixture f=null;
        try {
            f=create();
            Fixture local=f;
            String scenarioId=f.scenario().id();
            Map<String,Phase7Benchmark.Scenario> scenarios=Map.of(scenarioId,f.scenario());
            var model=Map.of(scenarioId,graph(scenarioId,"switch:reserve"));
            var modelReordered=Map.of(scenarioId,new Phase7ShadowReplay.Graph(
                    scenarioId,"root",Map.of(
                    "leaf",model.get(scenarioId).positions().get("leaf"),
                    "root",new ShadowSearchEngine.Position("root",List.of(
                            branch("move:attack",1),branch("move:guard",1),
                            branch("switch:reserve",3)),0))));
            check(Phase7ShadowReplay.graphSha256(model).equals(
                    Phase7ShadowReplay.graphSha256(modelReordered)),
                    "graph canonicalization independent of map and branch ordering");
            check(!Phase7ShadowReplay.graphSha256(model).equals(
                    Phase7ShadowReplay.graphSha256(Map.of(
                            scenarioId,graph(scenarioId,"move:attack")))),
                    "changed Shadow policy graph changes model fingerprint");
            check(Phase7ShadowReplay.scenarioSha256(scenarios).matches("[a-f0-9]{64}"),
                    "reviewed synthetic scenario manifest has stable hash");
            var pin=install(f,"shadow_v1",model,scenarios,20,8);
            check(pin.value().engine().equals("SHADOW_SEARCH_V1")
                    && pin.value().installedArtifactSha256().equals(ARTIFACT)
                    && pin.fileSha256().matches("[a-f0-9]{64}"),
                    "version, artifact and pinned config retained");
            var pinRoundtrip=Phase7ShadowReplay.load(f.root(),"shadow_v1",pin.fileSha256());
            check(pin.value().equals(pinRoundtrip.value()),"exact policy round-trip");
            check(!pin.toString().contains(ARTIFACT),"pinned policy toString redacted");

            List<String> ids=new ArrayList<>();
            for(int i=0;i<3;i++)ids.add(capture(f,"signed"+i,i,
                    "move:attack",ARTIFACT));
            var first=run(f,ids,pin,model);
            var again=run(f,ids,pin,model);
            check(first.replay().json().equals(again.replay().json())
                    && first.custodyReport().json().equals(again.custodyReport().json()),
                    "exact repeatable signed-custody replay and attribution");
            check(first.custodyReport().corpus().coverage().accepted()==3,
                    "all three independently signed synthetic captures admitted");
            check(first.replay().status()==Phase7ShadowReplay.Reason.SELECTED
                    && first.replay().traces().size()==3
                    && !first.replay().traceTruncated(),
                    "complete bounded decision traces for accepted cohort");
            var totals=first.replay().attribution().get("MOVE");
            check(totals.eligible()==3&&totals.disagreed()==3
                    && totals.agreed()==0&&totals.abstained()==0,
                    "per-move disagreement attribution detects Shadow switch recommendation");
            check(first.replay().traces().stream().allMatch(t->
                    t.attribution()==Phase7ShadowReplay.Attribution.MOVE_TO_SWITCH
                    && t.reason()==Phase7ShadowReplay.Reason.SELECTED
                    && t.visitedNodes()>0&&t.rankedActions()==3),
                    "real ShadowSearchEngine ranking drives captured trace");
            String report=first.replay().json();
            check(report.contains("\"MOVE_TO_SWITCH\"")
                    && JsonParser.parseString(report).isJsonObject(),
                    "privacy-safe machine-readable disagreement attribution");
            check(!report.contains("synthetic0")&&!report.contains("Mew")
                    && !report.contains(PRIVATE)&&!report.contains("01010101")
                    && !report.contains("move:attack")&&!report.contains("switch:reserve"),
                    "machine reports exclude capture IDs, raw history and action IDs");
            check(first.replay().intakeReportSha256().equals(
                    digest(first.custodyReport().json())),
                    "replay binds exact sanitized custodian report bytes");
            check(first.replay().replayFingerprint().matches("[a-f0-9]{64}"),
                    "bounded deterministic replay digest");
            var shuffled=new ArrayList<>(ids);Collections.reverse(shuffled);
            check(run(f,shuffled,pin,model).replay().json().equals(report),
                    "reordered bundle input has no effect on replay output");

            // Phase 7.9: a genuinely unique three-scenario synthetic cohort
            // keeps Phase 7.6 duplicate-state guards intact.
            var multiScenarios=new TreeMap<String,Phase7Benchmark.Scenario>();
            var multiGraphs=new TreeMap<String,Phase7ShadowReplay.Graph>();
            var matrixIds=new ArrayList<String>();
            for(int i=0;i<3;i++){
                var old=f.scenario();
                var snapshot=old.snapshot();
                var self=snapshot.self();
                var own=new BattleSnapshot.Combatant("synthetic-matrix-own-"+i,
                        self.hpFraction(),self.types(),self.knownBoosts(),
                        self.revealedMoves(),self.revealedItem(),self.revealedAbility(),false);
                var distinct=new BattleSnapshot(snapshot.turn(),own,snapshot.opponent(),
                        snapshot.selfBench(),snapshot.opponentBench(),snapshot.legalMoves(),
                        snapshot.legalSwitches(),snapshot.knownOpponentMoveCounts(),
                        snapshot.opponentTerastallized(),snapshot.opponentTeraType());
                var scenario=new Phase7Benchmark.Scenario("matrix-scenario-"+i,1,
                        distinct,old.criteria(),old.evidence(),null);
                multiScenarios.put(scenario.id(),scenario);
                multiGraphs.put(scenario.id(),graph(scenario.id(),"switch:reserve"));
                matrixIds.add(capture(f,"matrix_signed_"+i,20+i,
                        "move:attack",ARTIFACT,scenario));
            }
            var matrixPin=install(f,"shadow_matrix_ref",multiGraphs,multiScenarios,20,8);
            Phase7ReplayMatrixRegression.run(f.root(),KEY_ID,f.keyPin(),ARTIFACT,
                    multiScenarios,matrixIds,matrixPin,multiGraphs);

            var noCaptures=run(f,List.of(),pin,model);
            check(noCaptures.replay().status()==Phase7ShadowReplay.Reason.NO_CUSTODY_ACCEPTED
                    && noCaptures.replay().traces().isEmpty(),
                    "empty intake cannot fabricate genuine Shadow agreement");

            String altered=capture(f,"other_artifact",9,"move:attack","f".repeat(64));
            var incompatible=run(f,List.of(altered),pin,model);
            check(incompatible.custodyReport().intakeStatuses().getFirst()
                    ==Phase7TrustedIntake.Status.ARTIFACT_MISMATCH
                    && incompatible.custodyReport().corpus().coverage().accepted()==0
                    && incompatible.replay().status()==Phase7ShadowReplay.Reason.NO_CUSTODY_ACCEPTED,
                    "authentic signature for wrong installed build still fails pin");
            Path edited=f.root().resolve("run/phase7-corpus/private-input/signed0/checkpoint-02.json");
            Files.writeString(edited,Files.readString(edited)+PRIVATE);
            var damaged=run(f,List.of(ids.getFirst()),pin,model);
            check(damaged.custodyReport().corpus().coverage().accepted()==0
                    && damaged.replay().traces().isEmpty()
                    && !damaged.replay().json().contains(PRIVATE),
                    "altered private checkpoint rejected with no Shadow invocation");

            reject(()->run(local,ids,pinRoundtrip,Map.of(
                    scenarioId,graph(scenarioId,"move:attack"))),
                    "changed evaluation model cannot bypass externally pinned graph");
            var differentScenario=Phase7Benchmark.parseFixture(
                    Files.readString(Path.of("verification/benchmarks/phase7_1_synthetic.json")))
                    .scenarios().get(1);
            reject(()->Phase7ShadowReplay.replayLocal(local.root(),ids,
                    Map.of(differentScenario.id(),differentScenario),
                    KEY_ID,local.keyPin(),pin,model),
                    "mismatched scenario manifest rejected before touching custody");

            var low=install(f,"shadow_budget",model,scenarios,1,8);
            var limited=run(f,List.of(ids.get(1),ids.get(2)),low,model);
            check(limited.replay().status()==Phase7ShadowReplay.Reason.NO_DECISION,
                    "all accepted but abstained decisions cannot claim any selection");
            check(limited.custodyReport().corpus().coverage().accepted()==2
                    && limited.replay().attribution().get("MOVE").abstained()==2
                    && limited.replay().traces().stream().allMatch(t->
                       t.reason()==Phase7ShadowReplay.Reason.SEARCH_INCOMPLETE),
                    "exhausted bounded search never promotes partial ranking");

            var illegal=Map.of(scenarioId,illegalGraph(scenarioId));
            var illegalPin=install(f,"shadow_illegal",illegal,scenarios,20,8);
            var illegalResult=run(f,List.of(ids.get(1),ids.get(2)),illegalPin,illegal);
            check(illegalResult.replay().traces().stream().allMatch(t->
                    t.reason()==Phase7ShadowReplay.Reason.ILLEGAL_DECISION),
                    "nonlegal graph root action rejected before search");
            var compact=install(f,"shadow_trace",model,scenarios,20,1);
            var bounded=run(f,List.of(ids.get(1),ids.get(2)),compact,model);
            check(bounded.replay().traceTruncated()
                    && bounded.replay().status()==Phase7ShadowReplay.Reason.TRACE_BUDGET_EXCEEDED
                    && bounded.replay().traces().size()==1,
                    "trace capacity is enforced and explicitly reported");

            var one=run(f,List.of(ids.get(1)),pin,model);
            JsonObject sparse=JsonParser.parseString(one.replay().json()).getAsJsonObject();
            check(sparse.getAsJsonObject("attribution").getAsJsonObject("MOVE")
                    .get("suppressed").getAsBoolean()
                    && sparse.getAsJsonArray("traces").get(0).getAsJsonObject()
                    .get("suppressed").getAsBoolean(),
                    "small groups suppress individual details and aggregate counters");
            check(!one.replay().json().contains("\"MOVE_TO_SWITCH\""),
                    "single disagreement must not leak through a trace");

            Path policyFile=f.root().resolve("run/phase7-corpus/shadow-replay/phase7_shadow_v1.json");
            reject(()->{
                try{Phase7ShadowReplay.writeLocal(local.root(),pin.value());}
                catch(java.io.IOException expected){throw new IllegalStateException(expected);}
            },"approved policy revision cannot be silently overwritten");
            for(String bad:List.of(
                    pin.value().json().replace("\"version\":1","\"version\":2"),
                    pin.value().json().replace("\"nodeBudget\":20","\"nodeBudget\":0"),
                    pin.value().json().replace("\"modelSha256\":",
                            "\""+PRIVATE+"\":\"secret\",\"modelSha256\":"),
                    pin.value().json().replace("\"seed\":921","\"seed\":921,\"seed\":921"),
                    pin.value().json().replace("\"engine\":\"SHADOW_SEARCH_V1\"",
                            "\"engine\":\"UNKNOWN_ENGINE\""))) {
                Files.writeString(policyFile,bad);
                reject(()->{
                    try{Phase7ShadowReplay.load(local.root(),"shadow_v1",digest(bad));}
                    catch(java.io.IOException expected){throw new IllegalStateException(expected);}
                },"even re-pinned malformed/unsupported policy rejected");
            }
            Files.writeString(policyFile,pin.value().json());
            reject(()->{
                try{Phase7ShadowReplay.load(local.root(),"shadow_v1","0".repeat(64));}
                catch(java.io.IOException expected){throw new IllegalStateException(expected);}
            },"wrong independent policy file fingerprint rejected");
            check(Phase7ShadowReplay.load(local.root(),"shadow_v1",pin.fileSha256())
                    .value().equals(pin.value()),"valid policy restored unchanged");
            try{
                Files.createSymbolicLink(f.root().resolve(
                        "run/phase7-corpus/shadow-replay/phase7_link.json"),policyFile);
                reject(()->{
                    try{Phase7ShadowReplay.load(local.root(),"link",pin.fileSha256());}
                    catch(java.io.IOException expected){throw new IllegalStateException(expected);}
                },"symlink policy rejected");
            }catch(UnsupportedOperationException | java.nio.file.FileSystemException unsupported){
                // Synthetic filesystem may disable symbolic links.
            }
            reject(()->Phase7ShadowReplay.define("../private",ARTIFACT,921,
                    model,scenarios,20,3,0),"traversal-like revision rejected");
            reject(()->Phase7ShadowReplay.define("unknown",ARTIFACT,921,
                    model,scenarios,0,3,0),"zero-node budget rejected");
            reject(()->Phase7ShadowReplay.define("unsupported",ARTIFACT,921,
                    model,scenarios,20,101,0),"unbounded trace count rejected");
            System.out.println("Phase 7.8 synthetic signed Shadow replay regressions PASS: "
                    +checks+" checks (authentic Legacy decisions: 0; Minecraft runs: 0)");
        }catch(Exception e){throw new AssertionError("Phase 7.8 regression failure",e);}
        finally {if(f!=null)try{cleanup(f.root());}
            catch(Exception e){throw new AssertionError("temp cleanup",e);}}
    }
}
