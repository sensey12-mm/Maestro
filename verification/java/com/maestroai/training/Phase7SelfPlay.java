package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Phase 7.10 — OFFLINE observational, evidence-gated two-agent transition replay.
 * NOT a predictive battle simulator. In particular, neither opponent actions
 * nor post-turn states may be extrapolated: each transition must be explicitly
 * attested by an independently pinned Ed25519 reviewer of PUBLIC mechanics.
 * Without a real trusted reviewer and Phase 4 mechanics, there are NO verified
 * Minecraft transitions. Synthetic test keys prove wiring only, not truth.
 */
public final class Phase7SelfPlay {
    private Phase7SelfPlay() {}
    public static final String SCHEMA="phase7.10.v1";
    public static final String POLICY_SCHEMA="phase7.10.policy.v1";
    public static final String DOMAIN="MaestroAI-Phase7.10-PublicTransition-v1\n";
    private static final java.util.regex.Pattern ID=
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern HASH=
            java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final int MAX_NODES=32,MAX_EDGES=128,MAX_TURNS=16,MAX_FILE=8192;

    public enum Mechanic {
        PUBLIC_OBSERVED_TRANSITION, UNSUPPORTED_MECHANICS
    }
    /**
     * Each side receives a distinct own-request view, never the other side's
     * hidden moves/items. The two views MUST be independently reviewed; the
     * mechanics signature is not a substitute for that review.
     */
    public record Node(String id,String firstScenario,String secondScenario,boolean terminal) {
        public Node {
            if(!validId(id)||!validId(firstScenario)||!validId(secondScenario))
                throw new IllegalArgumentException("invalid public node ID");
        }
    }
    public record Edge(String source,String firstAction,String secondAction,
                       String successor,String publicEventSha256,Mechanic mechanic) {
        public Edge {
            if(!validId(source)||!validId(successor)||!validHash(publicEventSha256)
                    ||!validAction(firstAction)||!validAction(secondAction)
                    ||mechanic==null)
                throw new IllegalArgumentException("invalid observational transition");
        }
    }
    public record Transitions(String start,Map<String,Node> nodes,List<Edge> edges) {
        public Transitions {
            if(!validId(start)||nodes==null||edges==null||nodes.isEmpty()
                    ||nodes.size()>MAX_NODES||edges.isEmpty()||edges.size()>MAX_EDGES
                    ||!nodes.containsKey(start))
                throw new IllegalArgumentException("invalid bounded transition suite");
            TreeMap<String,Node> copy=new TreeMap<>();
            for(var x:nodes.entrySet()){
                if(!x.getKey().equals(x.getValue().id()))
                    throw new IllegalArgumentException("node alias mismatch");
                copy.put(x.getKey(),x.getValue());
            }
            var unique=new HashSet<String>();
            for(Edge edge:edges){
                Node before=copy.get(edge.source());
                if(before==null||!copy.containsKey(edge.successor())||before.terminal()
                        ||!unique.add(edge.source()+"|"+edge.firstAction()+"|"+edge.secondAction()))
                    throw new IllegalArgumentException("missing state, terminal edge or ambiguous response");
            }
            nodes=Collections.unmodifiableMap(copy);
            edges=edges.stream().sorted(Comparator
                    .comparing(Edge::source).thenComparing(Edge::firstAction)
                    .thenComparing(Edge::secondAction)).toList();
        }
    }

    public record Policy(String schema,int version,String revision,String transitionsSha256,
                         String firstScenarioSha256,String secondScenarioSha256,
                         String firstPolicySha256,String secondPolicySha256,
                         String artifactSha256,long seed,int maxTurns,int searchNodeBudget,
                         boolean requireSignedMechanics) {
        public Policy {
            if(!POLICY_SCHEMA.equals(schema)||version!=1||!validId(revision)
                    ||!validHash(transitionsSha256)||!validHash(firstScenarioSha256)
                    ||!validHash(secondScenarioSha256)||!validHash(firstPolicySha256)
                    ||!validHash(secondPolicySha256)||!validHash(artifactSha256)
                    ||maxTurns<1||maxTurns>MAX_TURNS
                    ||searchNodeBudget<1||searchNodeBudget>256||!requireSignedMechanics)
                throw new IllegalArgumentException("unsupported self-play policy");
        }
        public String json(){
            JsonObject o=new JsonObject();
            o.addProperty("schema",schema);o.addProperty("version",version);
            o.addProperty("revision",revision);o.addProperty("transitionsSha256",transitionsSha256);
            o.addProperty("firstScenarioSha256",firstScenarioSha256);
            o.addProperty("secondScenarioSha256",secondScenarioSha256);
            o.addProperty("firstPolicySha256",firstPolicySha256);
            o.addProperty("secondPolicySha256",secondPolicySha256);
            o.addProperty("artifactSha256",artifactSha256);
            o.addProperty("seed",seed);o.addProperty("maxTurns",maxTurns);
            o.addProperty("searchNodeBudget",searchNodeBudget);
            o.addProperty("requireSignedMechanics",requireSignedMechanics);
            return o.toString();
        }
    }
    public static final class PinnedPolicy {
        private final Policy policy;
        private final String exactFileSha256;
        private PinnedPolicy(Policy p,String sha){policy=p;exactFileSha256=sha;}
        public Policy value(){return policy;}
        public String exactFileSha256(){return exactFileSha256;}
        @Override public String toString(){return "PinnedSelfPlayPolicy[redacted]";}
    }
    public record Participant(Phase7ShadowReplay.PinnedPolicy policy,
                              Map<String,Phase7ShadowReplay.Graph> graphs,
                              Map<String,Phase7Benchmark.Scenario> scenarios) {
        public Participant {
            Objects.requireNonNull(policy);Objects.requireNonNull(graphs);
            Objects.requireNonNull(scenarios);
            if(graphs.isEmpty()||scenarios.isEmpty()
                    ||!graphs.keySet().equals(scenarios.keySet())
                    ||!policy.value().modelSha256().equals(Phase7ShadowReplay.graphSha256(graphs))
                    ||!policy.value().scenarioSha256().equals(
                            Phase7ShadowReplay.scenarioSha256(scenarios)))
                throw new IllegalArgumentException("unverified participant graph or scenario");
            graphs=Map.copyOf(graphs);scenarios=Map.copyOf(scenarios);
        }
    }
    /**
     * The caller supplies a trusted reviewer public key, independently pinned
     * SHA of its DER bytes, and detached signature. No bundle-provided key can
     * confer its own authority. A legitimate signature attests a reviewer's
     * approval but DOES NOT prove that review used genuine Minecraft evidence.
     */
    public record Attestation(String reviewerKeyDerBase64,
                              String signatureBase64) {
        public Attestation {
            if(reviewerKeyDerBase64==null
                    ||signatureBase64==null||reviewerKeyDerBase64.length()>2048
                    ||signatureBase64.length()>256)
                throw new IllegalArgumentException("invalid detached mechanics attestation");
        }
    }
    public enum Status {
        VERIFIED_OBSERVATIONAL_SEQUENCE, UNSIGNED_EVIDENCE, UNTRUSTED_REVIEWER,
        INVALID_ATTESTATION, POLICY_MISMATCH, INCOMPLETE_PUBLIC_EVIDENCE,
        UNVERIFIABLE_SUCCESSOR, UNSUPPORTED_MECHANICS,
        UNKNOWN_OPPONENT_RESPONSE, INCOMPLETE_TRANSITION_MODEL,
        ILLEGAL_ACTION, SEARCH_INCOMPLETE, SEARCH_EXCEPTION, CYCLIC_CONTINUATION,
        HORIZON_REACHED
    }
    public record Report(String schema,Status status,String policySha256,
                         String transitionsSha256,String reviewerSha256,
                         String firstModelSha256,String secondModelSha256,
                         String runFingerprint,int verifiedSteps,int attemptedSteps,
                         int eligibleTransitions,int missingTransitions,int unsupportedTransitions,
                         boolean endedAtExplicitTerminal,List<Status> stepReasons) {
        public Report {
            if(!SCHEMA.equals(schema)||status==null||!validHash(runFingerprint)
                    ||verifiedSteps<0||attemptedSteps<0||stepReasons==null
                    ||stepReasons.size()>MAX_TURNS)
                throw new IllegalArgumentException("invalid simulation report");
            stepReasons=List.copyOf(stepReasons);
        }
        public boolean verified(){return status==Status.VERIFIED_OBSERVATIONAL_SEQUENCE;}
        public String json(){
            JsonObject o=new JsonObject();
            o.addProperty("schema",schema);o.addProperty("status",status.name());
            o.addProperty("policySha256",policySha256);
            o.addProperty("transitionsSha256",transitionsSha256);
            o.addProperty("reviewerSha256",reviewerSha256);
            o.addProperty("firstModelSha256",firstModelSha256);
            o.addProperty("secondModelSha256",secondModelSha256);
            o.addProperty("runFingerprint",runFingerprint);
            o.addProperty("verifiedSequence",verified());
            o.addProperty("scope","public-attested-transcript-only;not-calibrated-simulation;not-minecraft;not-win-rate");
            o.addProperty("traceSuppressed",attemptedSteps<3);
            if(attemptedSteps>=3){
                o.addProperty("verifiedSteps",verifiedSteps);
                o.addProperty("attemptedSteps",attemptedSteps);
                o.addProperty("eligibleTransitions",eligibleTransitions);
                o.addProperty("missingTransitions",missingTransitions);
                o.addProperty("unsupportedTransitions",unsupportedTransitions);
                o.addProperty("endedAtExplicitTerminal",endedAtExplicitTerminal);
                JsonArray reasons=new JsonArray();
                for(Status reason:stepReasons)reasons.add(reason.name());
                o.add("stepReasons",reasons);
            }
            return o.toString();
        }
    }

    /** Canonical exact public transition model (not a trusted signature). */
    public static String transitionsSha256(Transitions graph,
            Map<String,Phase7Benchmark.Scenario> first,
            Map<String,Phase7Benchmark.Scenario> second){
        Objects.requireNonNull(graph);Objects.requireNonNull(first);Objects.requireNonNull(second);
        JsonObject all=new JsonObject();
        all.addProperty("schema","phase7.10.public-transitions.v1");
        all.addProperty("start",graph.start());
        JsonArray ns=new JsonArray();
        for(var n:graph.nodes().values()){
            var a=first.get(n.firstScenario());
            var b=second.get(n.secondScenario());
            if(a==null||b==null)
                throw new IllegalArgumentException("unbound public continuation state");
            JsonObject j=new JsonObject();
            j.addProperty("node",n.id());
            j.addProperty("first",n.firstScenario());
            j.addProperty("firstFingerprint",Phase7Benchmark.stateFingerprint(a));
            j.addProperty("second",n.secondScenario());
            j.addProperty("secondFingerprint",Phase7Benchmark.stateFingerprint(b));
            j.addProperty("terminal",n.terminal());
            ns.add(j);
        }
        all.add("nodes",ns);
        JsonArray edges=new JsonArray();
        for(var e:graph.edges()){
            JsonObject j=new JsonObject();
            j.addProperty("source",e.source());j.addProperty("firstAction",e.firstAction());
            j.addProperty("secondAction",e.secondAction());
            j.addProperty("successor",e.successor());
            j.addProperty("publicEventSha256",e.publicEventSha256());
            j.addProperty("mechanic",e.mechanic().name());edges.add(j);
        }
        all.add("edges",edges);
        return sha256(all.toString());
    }

    public static byte[] signaturePayload(String transitionSha256) {
        if(!validHash(transitionSha256))throw new IllegalArgumentException("invalid signed digest");
        return (DOMAIN+transitionSha256+"\n").getBytes(StandardCharsets.UTF_8);
    }

    public static Policy define(String revision,Transitions transition,
            Participant first,Participant second,long seed,int maxTurns,int searchBudget){
        String artifact=first.policy().value().installedArtifactSha256();
        if(!artifact.equals(second.policy().value().installedArtifactSha256())
                ||first.policy().value().seed()!=second.policy().value().seed()
                ||seed!=first.policy().value().seed())
            throw new IllegalArgumentException("incompatible participant versions");
        return new Policy(POLICY_SCHEMA,1,revision,transitionsSha256(transition,
                first.scenarios(),second.scenarios()),
                Phase7ShadowReplay.scenarioSha256(first.scenarios()),
                Phase7ShadowReplay.scenarioSha256(second.scenarios()),
                first.policy().fileSha256(),second.policy().fileSha256(),artifact,
                seed,maxTurns,searchBudget,true);
    }

    public static Path writeLocal(Path repo,Policy policy)throws IOException{
        Objects.requireNonNull(policy);
        Path dir=directory(repo),file=dir.resolve("phase7_"+policy.revision()+".json");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("immutable policy revision exists");
        Path temp=Files.createTempFile(dir,".phase7-", ".tmp");
        try{
            Files.writeString(temp,policy.json(),StandardCharsets.UTF_8);
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE);
        }finally{Files.deleteIfExists(temp);}
        return file;
    }
    public static PinnedPolicy load(Path repo,String revision,String trustedSha)throws IOException{
        if(!validId(revision)||!validHash(trustedSha))
            throw new IllegalArgumentException("untrusted policy metadata");
        Path file=directory(repo).resolve("phase7_"+revision+".json");
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                ||Files.size(file)>MAX_FILE)throw new IOException("policy unavailable");
        byte[] data;
        try(var stream=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){
            data=stream.readNBytes(MAX_FILE+1);
        }
        if(data.length>MAX_FILE||!sha256(data).equals(trustedSha))
            throw new IllegalArgumentException("untrusted policy file");
        JsonObject j=StrictOwnJson.read(new String(data,StandardCharsets.UTF_8));
        requireFields(j,"schema","version","revision","transitionsSha256",
                "firstScenarioSha256","secondScenarioSha256","firstPolicySha256",
                "secondPolicySha256","artifactSha256","seed","maxTurns",
                "searchNodeBudget","requireSignedMechanics");
        JsonElement signed=j.get("requireSignedMechanics");
        if(!signed.isJsonPrimitive()||!signed.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("invalid signed-mechanics policy");
        Policy p=new Policy(string(j,"schema"),number(j,"version"),
                string(j,"revision"),string(j,"transitionsSha256"),
                string(j,"firstScenarioSha256"),string(j,"secondScenarioSha256"),
                string(j,"firstPolicySha256"),string(j,"secondPolicySha256"),
                string(j,"artifactSha256"),numberLong(j,"seed"),number(j,"maxTurns"),
                number(j,"searchNodeBudget"),signed.getAsBoolean());
        if(!revision.equals(p.revision()))throw new IllegalArgumentException("policy revision drift");
        return new PinnedPolicy(p,trustedSha);
    }
    private static Path directory(Path repo)throws IOException{
        Path root=repo.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(root)||!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("unsafe repository root");
        Path dir=root;
        for(String part:List.of("run","phase7-corpus","selfplay")){
            dir=dir.resolve(part);
            if(Files.isSymbolicLink(dir))throw new IOException("unsafe self-play directory");
            if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir);
            if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe self-play directory");
        }
        return dir;
    }
    private static void requireFields(JsonObject j,String... fields){
        if(!j.keySet().equals(Set.of(fields)))
            throw new IllegalArgumentException("unknown or missing policy field");
    }
    private static String string(JsonObject j,String key){
        var x=j.get(key);
        if(x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid policy string");
        return x.getAsString();
    }
    private static long numberLong(JsonObject j,String key){
        var x=j.get(key);
        if(x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isNumber()
                ||!x.getAsString().matches("-?(0|[1-9][0-9]{0,18})"))
            throw new IllegalArgumentException("invalid policy integer");
        return Long.parseLong(x.getAsString());
    }
    private static int number(JsonObject j,String key){
        long n=numberLong(j,key);
        if(n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)
            throw new IllegalArgumentException("invalid policy integer");
        return (int)n;
    }

    /**
     * Signed evidence controls every transition; no signature means ZERO
     * simulated transitions, even on a synthetic branch with legal actions.
     * End-to-end completeness requires reaching an explicitly attested terminal,
     * which is NOT a win/loss result.
     */
    public static Report replay(Transitions transitions,Participant first,Participant second,
                                PinnedPolicy policy,Attestation attestation,
                                String externallyPinnedReviewerKeySha256){
        Objects.requireNonNull(transitions);Objects.requireNonNull(first);
        Objects.requireNonNull(second);Objects.requireNonNull(policy);
        Policy p=policy.value();
        if(!validHash(externallyPinnedReviewerKeySha256))
            throw new IllegalArgumentException("independently pinned reviewer SHA required");
        String fp=transitionsSha256(transitions,first.scenarios(),second.scenarios());
        String constant=SCHEMA+"|"+policy.exactFileSha256()+"|"+fp+"|"+
                first.policy().fileSha256()+"|"+second.policy().fileSha256();
        Status bad=checkPolicy(p,policy,fp,first,second);
        if(bad!=null)return report(bad,policy,fp,externallyPinnedReviewerKeySha256,first,second,
                constant,0,0,transitions.edges().size(),0,0,false,List.of());
        Status verified=verifyAttestation(attestation,fp,externallyPinnedReviewerKeySha256);
        if(verified!=null)return report(verified,policy,fp,externallyPinnedReviewerKeySha256,first,second,
                constant,0,0,transitions.edges().size(),0,0,false,List.of());

        // Validate *all* states and edges, not only the lucky selected path.
        Status quality=validateEvidence(transitions,first.scenarios(),second.scenarios());
        if(quality!=null)return report(quality,policy,fp,externallyPinnedReviewerKeySha256,first,second,
                constant,0,0,transitions.edges().size(),0,0,false,List.of());

        String at=transitions.start();
        Set<String> visited=new HashSet<>();
        int verifiedSteps=0,attempts=0,missing=0,unsupported=0;
        List<Status> reasons=new ArrayList<>();
        boolean explicitTerminal=false;
        Status finalStatus=Status.HORIZON_REACHED;
        for(int step=0;step<p.maxTurns();step++){
            Node node=transitions.nodes().get(at);
            if(node.terminal()){
                finalStatus=Status.VERIFIED_OBSERVATIONAL_SEQUENCE;
                explicitTerminal=true;break;
            }
            if(!visited.add(at)){finalStatus=Status.CYCLIC_CONTINUATION;break;}
            attempts++;
            Decision left=choose(first,node.firstScenario(),p.searchNodeBudget());
            Decision right=choose(second,node.secondScenario(),p.searchNodeBudget());
            Status fail=left.status()!=null?left.status():right.status();
            if(fail!=null){
                finalStatus=fail;reasons.add(fail);break;
            }
            Edge selected=null;
            boolean firstKnown=false;
            for(Edge e:transitions.edges()){
                if(!e.source().equals(at))continue;
                if(e.firstAction().equals(left.action())){
                    firstKnown=true;
                    if(e.secondAction().equals(right.action()))selected=e;
                }
            }
            if(selected==null){
                fail=firstKnown?Status.UNKNOWN_OPPONENT_RESPONSE:
                        Status.INCOMPLETE_TRANSITION_MODEL;
                missing++;finalStatus=fail;reasons.add(fail);break;
            }
            if(selected.mechanic()!=Mechanic.PUBLIC_OBSERVED_TRANSITION){
                unsupported++;finalStatus=Status.UNSUPPORTED_MECHANICS;
                reasons.add(finalStatus);break;
            }
            verifiedSteps++;
            reasons.add(Status.VERIFIED_OBSERVATIONAL_SEQUENCE);
            at=selected.successor();
        }
        if(verifiedSteps==p.maxTurns()&&transitions.nodes().get(at).terminal()){
            finalStatus=Status.VERIFIED_OBSERVATIONAL_SEQUENCE;
            explicitTerminal=true;
        }
        return report(finalStatus,policy,fp,externallyPinnedReviewerKeySha256,first,second,constant,
                verifiedSteps,attempts,transitions.edges().size(),missing,unsupported,
                explicitTerminal,reasons);
    }
    private static Status checkPolicy(Policy p,PinnedPolicy pin,String fp,
                                     Participant first,Participant second){
        if(!p.transitionsSha256().equals(fp)
                ||!p.firstScenarioSha256().equals(Phase7ShadowReplay.scenarioSha256(first.scenarios()))
                ||!p.secondScenarioSha256().equals(Phase7ShadowReplay.scenarioSha256(second.scenarios()))
                ||!p.firstPolicySha256().equals(first.policy().fileSha256())
                ||!p.secondPolicySha256().equals(second.policy().fileSha256())
                ||!p.artifactSha256().equals(first.policy().value().installedArtifactSha256())
                ||!p.artifactSha256().equals(second.policy().value().installedArtifactSha256())
                ||p.seed()!=first.policy().value().seed()
                ||p.seed()!=second.policy().value().seed()
                ||!pin.exactFileSha256().matches("[a-f0-9]{64}"))
            return Status.POLICY_MISMATCH;
        return null;
    }
    private static Status verifyAttestation(Attestation a,String fingerprint,
                                            String trustedSha256){
        if(a==null)return Status.UNSIGNED_EVIDENCE;
        try{
            byte[] publicDer=Base64.getDecoder().decode(a.reviewerKeyDerBase64());
            if(publicDer.length>1024||!sha256(publicDer).equals(trustedSha256))
                return Status.UNTRUSTED_REVIEWER;
            byte[] sig=Base64.getDecoder().decode(a.signatureBase64());
            if(sig.length!=64)return Status.INVALID_ATTESTATION;
            var key=KeyFactory.getInstance("Ed25519").generatePublic(
                    new X509EncodedKeySpec(publicDer));
            var verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(signaturePayload(fingerprint));
            return verifier.verify(sig)?null:Status.INVALID_ATTESTATION;
        }catch(Exception bad){return Status.INVALID_ATTESTATION;}
    }
    private static Status validateEvidence(Transitions t,
            Map<String,Phase7Benchmark.Scenario> first,
            Map<String,Phase7Benchmark.Scenario> second){
        // The side's own-request menus are never cross-shared with the agent.
        for(Node n:t.nodes().values()){
            var a=first.get(n.firstScenario());
            var b=second.get(n.secondScenario());
            if(a==null||b==null
                    ||!eligible(a)||!eligible(b)
                    ||a.snapshot().turn()!=b.snapshot().turn())
                return Status.INCOMPLETE_PUBLIC_EVIDENCE;
        }
        for(Edge e:t.edges()){
            var before=t.nodes().get(e.source());
            var after=t.nodes().get(e.successor());
            var a=first.get(before.firstScenario());
            var b=second.get(before.secondScenario());
            var nextA=first.get(after.firstScenario());
            var nextB=second.get(after.secondScenario());
            if(nextA.snapshot().turn()!=a.snapshot().turn()+1
                    ||nextB.snapshot().turn()!=b.snapshot().turn()+1)
                return Status.UNVERIFIABLE_SUCCESSOR;
            if(!legal(a.snapshot(),e.firstAction())||!legal(b.snapshot(),e.secondAction()))
                return Status.ILLEGAL_ACTION;
        }
        return null;
    }
    private static boolean eligible(Phase7Benchmark.Scenario s){
        return s.legacy()==null && s.evidence().fresh()
                && s.evidence().publicOnly()&&s.evidence().mechanicsSupported()
                &&s.snapshot().turn()>0;
    }
    private record Decision(String action,Status status) {}
    private static Decision choose(Participant participant,String scenarioId,int budget){
        var scenario=participant.scenarios().get(scenarioId);
        var graph=participant.graphs().get(scenarioId);
        if(scenario==null||graph==null)
            return new Decision(null,Status.INCOMPLETE_PUBLIC_EVIDENCE);
        var model=participant.policy().value();
        try{
            for(var choice:graph.positions().get(graph.root()).branches()){
                if(!legal(scenario.snapshot(),choice.id()))
                    return new Decision(null,Status.ILLEGAL_ACTION);
            }
            var a=ShadowSearchEngine.search(scenario.snapshot(),graph.root(),
                    graph.positions(),budget,model.uncertaintyPenalty());
            var b=ShadowSearchEngine.search(scenario.snapshot(),graph.root(),
                    graph.positions(),budget,model.uncertaintyPenalty());
            if(!a.equals(b))return new Decision(null,Status.SEARCH_EXCEPTION);
            if(!a.complete()||a.preferred()==null)
                return new Decision(null,Status.SEARCH_INCOMPLETE);
            if(!legal(scenario.snapshot(),a.preferred()))
                return new Decision(null,Status.ILLEGAL_ACTION);
            return new Decision(a.preferred(),null);
        }catch(RuntimeException failure){return new Decision(null,Status.SEARCH_EXCEPTION);}
    }
    private static boolean legal(BattleSnapshot s,String action){
        if(!validAction(action))return false;
        if(action.startsWith("move:"))return s.legalMoves().contains(action.substring(5));
        return s.legalSwitches().contains(action.substring(7));
    }
    private static Report report(Status result,PinnedPolicy pin,String fingerprint,
                                 String reviewerSha,Participant left,Participant right,
                                 String material,int verified,int attempts,int eligible,
                                 int missing,int unsupported,boolean terminal,List<Status> reasons){
        String log=material+"|"+result+"|"+verified+"|"+attempts+"|"+reasons;
        return new Report(SCHEMA,result,pin.exactFileSha256(),fingerprint,
                reviewerSha,left.policy().value().modelSha256(),
                right.policy().value().modelSha256(),sha256(log),
                verified,attempts,eligible,missing,unsupported,terminal,reasons);
    }
    private static boolean validId(String s){return s!=null&&ID.matcher(s).matches();}
    private static boolean validHash(String s){return s!=null&&HASH.matcher(s).matches();}
    private static boolean validAction(String s){
        return s!=null&&(s.startsWith("move:")&&validId(s.substring(5))
                ||s.startsWith("switch:")&&validId(s.substring(7)));
    }
    private static String sha256(String s){return sha256(s.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
