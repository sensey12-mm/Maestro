package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Phase 7.10 synthetic signed-public-transcript self-play regressions.
 * Test keys and model transitions are NOT genuine Cobblemon mechanics proof.
 */
public final class Phase7SelfPlayRegression {
    private Phase7SelfPlayRegression() {}
    private static int checks;
    private static final String ARTIFACT="a".repeat(64);
    private static final String SECRET="PRIVATE_POKEMON_SHOULD_NOT_BE_IN_REPORT";
    private static final long SEED=333L;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 7.10: "+why);
        checks++;
    }
    private static void reject(Runnable r,String why){
        boolean thrown=false;
        try{r.run();}catch(RuntimeException expected){thrown=true;}
        check(thrown,why);
    }
    private static String sha(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String sha(byte[] value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static Phase7Benchmark.Scenario scene(Phase7Benchmark.Scenario original,
                                                   String role,int turn){
        var s=original.snapshot();
        var a=s.self();
        var own=new BattleSnapshot.Combatant(role+"-own",a.hpFraction(),a.types(),
                a.knownBoosts(),a.revealedMoves(),a.revealedItem(),
                a.revealedAbility(),a.fainted());
        var snapshot=new BattleSnapshot(turn,own,s.opponent(),s.selfBench(),
                s.opponentBench(),s.legalMoves(),s.legalSwitches(),
                s.knownOpponentMoveCounts(),s.opponentTerastallized(),
                s.opponentTeraType());
        return new Phase7Benchmark.Scenario(role+"-turn-"+turn,1,snapshot,
                original.criteria(),new Phase7Benchmark.Evidence(true,true,true,turn),null);
    }
    private static Map<String,Phase7Benchmark.Scenario> scenes(Phase7Benchmark.Scenario original,
                                                                String role){
        var result=new TreeMap<String,Phase7Benchmark.Scenario>();
        for(int turn=2;turn<=5;turn++){
            var s=scene(original,role,turn);
            result.put(s.id(),s);
        }
        return Map.copyOf(result);
    }
    private static Phase7ShadowReplay.Graph graph(String scenarioId,String action){
        var branches=new ArrayList<ShadowSearchEngine.Branch>();
        for(String choice:List.of("move:attack","move:guard","switch:reserve")){
            String kind=choice.substring(0,choice.indexOf(':'));
            String name=choice.substring(choice.indexOf(':')+1);
            branches.add(new ShadowSearchEngine.Branch(kind,name,List.of(
                    new ShadowSearchEngine.Outcome("observed",1,
                            choice.equals(action)?4:1,0,"leaf"))));
        }
        return new Phase7ShadowReplay.Graph(scenarioId,"root",Map.of(
                "root",new ShadowSearchEngine.Position("root",branches,0),
                "leaf",new ShadowSearchEngine.Position("leaf",List.of(),0)));
    }
    private static Phase7SelfPlay.Participant participant(Path root,String name,
                   Map<String,Phase7Benchmark.Scenario> scenarios,String preferred)throws Exception{
        var graphs=new TreeMap<String,Phase7ShadowReplay.Graph>();
        for(String id:scenarios.keySet())graphs.put(id,graph(id,preferred));
        var policy=Phase7ShadowReplay.define(name,ARTIFACT,SEED,graphs,scenarios,32,16,0);
        Path file=Phase7ShadowReplay.writeLocal(root,policy);
        var pin=Phase7ShadowReplay.load(root,name,sha(Files.readString(file)));
        return new Phase7SelfPlay.Participant(pin,graphs,scenarios);
    }
    private static Phase7SelfPlay.Transitions transcript(int length){
        var nodes=new TreeMap<String,Phase7SelfPlay.Node>();
        var edges=new ArrayList<Phase7SelfPlay.Edge>();
        for(int i=0;i<=length;i++){
            int turn=2+i;
            nodes.put("step"+i,new Phase7SelfPlay.Node("step"+i,
                    "first-turn-"+turn,"second-turn-"+turn,i==length));
        }
        for(int i=0;i<length;i++)
            edges.add(new Phase7SelfPlay.Edge("step"+i,"move:attack",
                    "move:attack","step"+(i+1),sha("synthetic-public-event-"+i),
                    Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION));
        return new Phase7SelfPlay.Transitions("step0",nodes,edges);
    }
    private static Phase7SelfPlay.Transitions modifyEdge(Phase7SelfPlay.Transitions t,
            String first,String other,String dest,Phase7SelfPlay.Mechanic mechanic){
        var e=t.edges().getFirst();
        var changed=new Phase7SelfPlay.Edge(e.source(),first,other,dest,
                e.publicEventSha256(),mechanic);
        var list=new ArrayList<>(t.edges());
        list.set(0,changed);
        return new Phase7SelfPlay.Transitions(t.start(),t.nodes(),list);
    }
    private record Fixture(Path root,KeyPair signer,Phase7SelfPlay.Participant first,
                           Phase7SelfPlay.Participant second,
                           Phase7SelfPlay.Transitions transitions) {}
    private static Fixture setup()throws Exception{
        var root=Files.createTempDirectory("phase710-selfplay-");
        var original=Phase7Benchmark.parseFixture(Files.readString(
                Path.of("verification/benchmarks/phase7_1_synthetic.json")))
                .scenarios().getFirst();
        var first=participant(root,"selfplay_first",scenes(original,"first"),"move:attack");
        var second=participant(root,"selfplay_second",scenes(original,"second"),"move:attack");
        var keygen=KeyPairGenerator.getInstance("Ed25519");
        return new Fixture(root,keygen.generateKeyPair(),first,second,transcript(3));
    }
    private static Phase7SelfPlay.PinnedPolicy policy(Fixture f,String revision,
                 Phase7SelfPlay.Transitions t,int maxTurns,int budget)throws Exception{
        var p=Phase7SelfPlay.define(revision,t,f.first(),f.second(),SEED,maxTurns,budget);
        Path file=Phase7SelfPlay.writeLocal(f.root(),p);
        return Phase7SelfPlay.load(f.root(),revision,sha(Files.readString(file)));
    }
    private static Phase7SelfPlay.Attestation attest(Fixture f,Phase7SelfPlay.Transitions t)
            throws Exception{
        String sha=Phase7SelfPlay.transitionsSha256(t,
                f.first().scenarios(),f.second().scenarios());
        var signer=Signature.getInstance("Ed25519");
        signer.initSign(f.signer().getPrivate());
        signer.update(Phase7SelfPlay.signaturePayload(sha));
        byte[] pub=f.signer().getPublic().getEncoded();
        return new Phase7SelfPlay.Attestation(Base64.getEncoder().encodeToString(pub),
                Base64.getEncoder().encodeToString(signer.sign()));
    }
    private static Phase7SelfPlay.Report run(Fixture f,Phase7SelfPlay.Transitions t,
                 Phase7SelfPlay.PinnedPolicy p,Phase7SelfPlay.Attestation a){
        return Phase7SelfPlay.replay(t,f.first(),f.second(),p,a,
                sha(f.signer().getPublic().getEncoded()));
    }
    private static void cleanup(Path root)throws Exception{
        try(var stream=Files.walk(root)){
            stream.sorted(Comparator.reverseOrder()).forEach(p->{
                try{Files.deleteIfExists(p);}
                catch(java.io.IOException e){throw new AssertionError("test cleanup",e);}
            });
        }
    }
    public static void run(){
        Fixture f=null;
        try{
            f=setup();
            Fixture local=f;
            var pin=policy(f,"observational_v1",f.transitions(),3,20);
            var attestation=attest(f,f.transitions());
            var report=run(f,f.transitions(),pin,attestation);
            check(report.status()==Phase7SelfPlay.Status.VERIFIED_OBSERVATIONAL_SEQUENCE
                    &&report.verified()&&report.endedAtExplicitTerminal(),
                    "fully signed three-step observational path reaches explicit terminal");
            check(report.verifiedSteps()==3&&report.attemptedSteps()==3
                    &&report.eligibleTransitions()==3&&report.missingTransitions()==0,
                    "bounded transition coverage and verified step accounting");
            check(report.stepReasons().equals(Collections.nCopies(3,
                    Phase7SelfPlay.Status.VERIFIED_OBSERVATIONAL_SEQUENCE)),
                    "reproducible typed continuation reasons");
            check(report.json().equals(run(f,f.transitions(),pin,attestation).json()),
                    "signed replay exactly reproducible with same pinned version");
            check(JsonParser.parseString(report.json()).isJsonObject()
                    &&report.json().contains("\"verifiedSequence\":true"),
                    "strict machine-readable observational report");
            check(!report.json().contains("first-turn")
                    &&!report.json().contains("second-turn")
                    &&!report.json().contains("move:attack")
                    &&!report.json().contains("Mew")
                    &&!report.json().contains(SECRET),
                    "no private states, opponent facts or action IDs in report");
            check(report.json().contains("not-calibrated-simulation")
                    &&report.json().contains("not-win-rate"),
                    "report does not claim simulation accuracy or competitive wins");
            check(pin.value().firstPolicySha256().equals(
                    f.first().policy().fileSha256())
                    &&pin.value().secondPolicySha256().equals(
                    f.second().policy().fileSha256()),"each separately pinned Shadow model bound");
            check(pin.exactFileSha256().matches("[a-f0-9]{64}")
                    &&pin.value().transitionsSha256().equals(report.transitionsSha256()),
                    "signed exact transition model matches independently pinned policy");
            check(!pin.toString().contains(ARTIFACT),"pinned metadata toString redacted");
            check(run(f,f.transitions(),pin,null).status()
                    ==Phase7SelfPlay.Status.UNSIGNED_EVIDENCE,
                    "no attested public mechanics means ZERO transition advances");
            check(run(f,f.transitions(),pin,null).verifiedSteps()==0,
                    "missing attestation never triggers self-play search");
            check(Phase7SelfPlay.replay(f.transitions(),f.first(),f.second(),
                    pin,attestation,"f".repeat(64)).status()
                    ==Phase7SelfPlay.Status.UNTRUSTED_REVIEWER,
                    "signer whose public key does not match external trust pin rejected");
            var badSig=new Phase7SelfPlay.Attestation(
                    attestation.reviewerKeyDerBase64(),
                    Base64.getEncoder().encodeToString(new byte[64]));
            check(run(f,f.transitions(),pin,badSig).status()
                    ==Phase7SelfPlay.Status.INVALID_ATTESTATION,
                    "invalid detached Ed25519 signature fails closed");
            // A forged certificate may be internally consistent with its own
            // key but must NOT bypass the key pin established out of band.
            var attackerKey=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var attackerSigner=Signature.getInstance("Ed25519");
            attackerSigner.initSign(attackerKey.getPrivate());
            attackerSigner.update(Phase7SelfPlay.signaturePayload(
                    pin.value().transitionsSha256()));
            var attackerAttestation=new Phase7SelfPlay.Attestation(
                    Base64.getEncoder().encodeToString(attackerKey.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(attackerSigner.sign()));
            check(run(f,f.transitions(),pin,attackerAttestation).status()
                    ==Phase7SelfPlay.Status.UNTRUSTED_REVIEWER,
                    "bundle self-signed by unknown key cannot declare itself trusted");
            reject(()->Phase7SelfPlay.replay(local.transitions(),local.first(),
                    local.second(),pin,attestation,"not-pinned"),
                    "external trust fingerprint required by API");
            var different=modifyEdge(f.transitions(),"move:attack","move:guard",
                    "step1",Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION);
            check(run(f,different,pin,attestation).status()
                    ==Phase7SelfPlay.Status.POLICY_MISMATCH,
                    "modified transition graph cannot pass old pinned policy");
            var wrongSigPolicy=policy(f,"modified_public_response",different,3,20);
            check(run(f,different,wrongSigPolicy,attestation).status()
                    ==Phase7SelfPlay.Status.INVALID_ATTESTATION,
                    "new pinned graph still requires independent public signature");
            var signedMissing=attest(f,different);
            var unknown=run(f,different,wrongSigPolicy,signedMissing);
            check(unknown.status()==Phase7SelfPlay.Status.UNKNOWN_OPPONENT_RESPONSE
                    &&unknown.verifiedSteps()==0&&unknown.missingTransitions()==1,
                    "missing certified opponent action yields explicit abstention");
            var notModeled=modifyEdge(f.transitions(),"move:guard","move:attack",
                    "step1",Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION);
            var notModeledPin=policy(f,"unmodeled_own_action",notModeled,3,20);
            check(run(f,notModeled,notModeledPin,attest(f,notModeled)).status()
                    ==Phase7SelfPlay.Status.INCOMPLETE_TRANSITION_MODEL,
                    "missing own-choice response never guessed");
            var unsupported=modifyEdge(f.transitions(),"move:attack","move:attack",
                    "step1",Phase7SelfPlay.Mechanic.UNSUPPORTED_MECHANICS);
            var unsupportedPin=policy(f,"unsupported_effect",unsupported,3,20);
            var unsupportedReport=run(f,unsupported,unsupportedPin,attest(f,unsupported));
            check(unsupportedReport.status()==Phase7SelfPlay.Status.UNSUPPORTED_MECHANICS
                    &&unsupportedReport.unsupportedTransitions()==1,
                    "signed but unsupported mechanics still explicitly abstain");
            var illegal=modifyEdge(f.transitions(),"move:unknown_hidden_move",
                    "move:attack","step1",
                    Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION);
            var illegalPin=policy(f,"illegal_choice",illegal,3,20);
            check(run(f,illegal,illegalPin,attest(f,illegal)).status()
                    ==Phase7SelfPlay.Status.ILLEGAL_ACTION,
                    "attested invalid own move rejected before self-play");
            var illegalOpponent=modifyEdge(f.transitions(),"move:attack",
                    "switch:unknown_private_slot","step1",
                    Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION);
            var illegalOpponentPin=policy(f,"illegal_opponent",illegalOpponent,3,20);
            check(run(f,illegalOpponent,illegalOpponentPin,attest(f,illegalOpponent)).status()
                    ==Phase7SelfPlay.Status.ILLEGAL_ACTION,
                    "opponent legal action must be independently attested");

            var stale=modifyEdge(f.transitions(),"move:attack","move:attack",
                    "step2",Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION);
            var stalePin=policy(f,"turn_skipping",stale,3,20);
            check(run(f,stale,stalePin,attest(f,stale)).status()
                    ==Phase7SelfPlay.Status.UNVERIFIABLE_SUCCESSOR,
                    "turn skips forbidden even with valid detached signature");
            var budget=policy(f,"tiny_search_budget",f.transitions(),3,1);
            check(run(f,f.transitions(),budget,attestation).status()
                    ==Phase7SelfPlay.Status.SEARCH_INCOMPLETE,
                    "partial Shadow search never acts");
            var horizon=policy(f,"bounded_horizon",f.transitions(),1,20);
            var partial=run(f,f.transitions(),horizon,attestation);
            check(partial.status()==Phase7SelfPlay.Status.HORIZON_REACHED
                    &&!partial.verified()&&partial.verifiedSteps()==1,
                    "bounded path cannot pretend a terminal or battle win");
            check(partial.json().contains("\"traceSuppressed\":true")
                    &&!partial.json().contains("\"verifiedSteps\""),
                    "short observational traces suppressed in public report");
            var other=Phase7ShadowReplay.define("wrong_artifact","f".repeat(64),
                    SEED,f.second().graphs(),f.second().scenarios(),32,16,0);
            Path otherPath=Phase7ShadowReplay.writeLocal(f.root(),other);
            var otherPin=Phase7ShadowReplay.load(f.root(),"wrong_artifact",
                    sha(Files.readString(otherPath)));
            var mixed=new Phase7SelfPlay.Participant(otherPin,
                    f.second().graphs(),f.second().scenarios());
            reject(()->Phase7SelfPlay.define("mixed_build",local.transitions(),
                    local.first(),mixed,SEED,3,20),
                    "different installed artifact hashes cannot share self-play policy");
            reject(()->Phase7SelfPlay.define("../escape",local.transitions(),
                    local.first(),local.second(),SEED,3,20),
                    "policy revision traversal rejected");
            reject(()->Phase7SelfPlay.define("bad_seed",local.transitions(),
                    local.first(),local.second(),999,3,20),
                    "pinned Shadow seed must match both participants");
            reject(()->Phase7SelfPlay.define("unbounded",local.transitions(),
                    local.first(),local.second(),SEED,17,20),
                    "turn horizon cannot exceed safety bound");
            reject(()->Phase7SelfPlay.define("zero_budget",local.transitions(),
                    local.first(),local.second(),SEED,3,0),
                    "zero Shadow node budget rejected");
            reject(()->new Phase7SelfPlay.Transitions("step0",
                    local.transitions().nodes(),List.of(local.transitions().edges().getFirst(),
                            local.transitions().edges().getFirst())),
                    "ambiguous duplicate joint response refused");
            reject(()->new Phase7SelfPlay.Transitions("step0",
                    local.transitions().nodes(),List.of(new Phase7SelfPlay.Edge("step3",
                            "move:attack","move:attack","step2",sha("event"),
                            Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION))),
                    "terminal observations cannot have outgoing simulated actions");
            var scrambled=new ArrayList<>(f.transitions().edges());
            Collections.reverse(scrambled);
            var permuted=new Phase7SelfPlay.Transitions("step0",
                    f.transitions().nodes(),scrambled);
            check(Phase7SelfPlay.transitionsSha256(permuted,
                    f.first().scenarios(),f.second().scenarios())
                    .equals(pin.value().transitionsSha256()),
                    "graph source ordering cannot alter canonical signed digest");

            var file=f.root().resolve("run/phase7-corpus/selfplay/phase7_observational_v1.json");
            String original=Files.readString(file);
            reject(()->{
                try{Phase7SelfPlay.writeLocal(local.root(),pin.value());}
                catch(java.io.IOException expected){throw new IllegalStateException(expected);}
            },"immutable local policy cannot be overwritten");
            for(String edited:List.of(
                    original.replace("\"version\":1","\"version\":2"),
                    original.replace("\"searchNodeBudget\":20","\"searchNodeBudget\":0"),
                    original.replace("\"requireSignedMechanics\":true",
                            "\"requireSignedMechanics\":false"),
                    original.replace("\"seed\":333","\"seed\":333,\"seed\":333"),
                    original.replace("\"artifactSha256\":",
                            "\""+SECRET+"\":\"private\",\"artifactSha256\":"))) {
                Files.writeString(file,edited);
                reject(()->{
                    try{Phase7SelfPlay.load(local.root(),"observational_v1",sha(edited));}
                    catch(java.io.IOException e){throw new IllegalStateException(e);}
                },"malformed even re-pinned policy never accepted");
            }
            Files.writeString(file,original);
            check(Phase7SelfPlay.load(local.root(),"observational_v1",
                    pin.exactFileSha256()).value().equals(pin.value()),
                    "trusted exact-byte local policy reproducible");
            reject(()->{
                try{Phase7SelfPlay.load(local.root(),"observational_v1","0".repeat(64));}
                catch(java.io.IOException e){throw new IllegalStateException(e);}
            },"untrusted local policy bytes rejected");
            try {
                var symlink=f.root().resolve("run/phase7-corpus/selfplay/phase7_link.json");
                Files.createSymbolicLink(symlink,file);
                reject(()->{
                    try{Phase7SelfPlay.load(local.root(),"link",pin.exactFileSha256());}
                    catch(java.io.IOException e){throw new IllegalStateException(e);}
                },"symlink policy rejected");
            }catch(UnsupportedOperationException | java.nio.file.FileSystemException unsupportedFs){
                // The test filesystem may not allow symlinks.
            }
            System.out.println("Phase 7.10 synthetic observed-transition self-play regressions PASS: "
                    +checks+" checks (real supported mechanics: 0; Minecraft runs: 0)");
        }catch(Exception failure){throw new AssertionError("Phase 7.10 synthetic harness failed",failure);}
        finally{if(f!=null)try{cleanup(f.root());}
                catch(Exception failedCleanup){throw new AssertionError("temp cleanup failed",failedCleanup);}}
    }
}
