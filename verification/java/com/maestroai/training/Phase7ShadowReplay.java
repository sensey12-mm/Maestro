package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.8: verification-only deterministic ShadowSearchEngine replay.
 *
 * A runtime Legacy action is NEVER inferred from a public |move| event:
 * only Phase7TrustedIntake's signed local custody/replay path supplies the
 * accepted comparison cohort. This class has no production battle caller.
 *
 * Graph values are caller-supplied OFFLINE conditional proxies, NOT a
 * calibrated simulator or source of opponent moves/hidden state.
 */
public final class Phase7ShadowReplay {
    private Phase7ShadowReplay() {}
    public static final String SCHEMA="phase7.8.v1";
    public static final String PIN_SCHEMA="phase7.8.policy.v1";
    private static final java.util.regex.Pattern ID=
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern HASH=
            java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final int MAX_FILE=8192;
    private static final int MAX_SCENARIOS=100;
    private static final int MAX_GRAPH_NODES=64;
    private static final int MAX_GRAPH_EDGES=256;

    /** Immutable, versioned, explicitly offline evaluation configuration. */
    public record Policy(String schema,int version,String revision,
                         String engine,String modelSha256,String scenarioSha256,
                         String installedArtifactSha256,long seed,
                         int nodeBudget,int maxTraceEntries,
                         double uncertaintyPenalty,boolean requireCompleteSearch) {
        public Policy {
            if(!PIN_SCHEMA.equals(schema)||version!=1||!validId(revision)
                    ||!"SHADOW_SEARCH_V1".equals(engine)||!validHash(modelSha256)
                    ||!validHash(scenarioSha256)||!validHash(installedArtifactSha256)
                    ||nodeBudget<1||nodeBudget>256||maxTraceEntries<1
                    ||maxTraceEntries>100||!Double.isFinite(uncertaintyPenalty)
                    ||uncertaintyPenalty<0||uncertaintyPenalty>100
                    ||!requireCompleteSearch)
                throw new IllegalArgumentException("unsupported offline Shadow policy");
        }
        public String json(){
            JsonObject o=new JsonObject();
            o.addProperty("schema",schema);o.addProperty("version",version);
            o.addProperty("revision",revision);o.addProperty("engine",engine);
            o.addProperty("modelSha256",modelSha256);
            o.addProperty("scenarioSha256",scenarioSha256);
            o.addProperty("installedArtifactSha256",installedArtifactSha256);
            o.addProperty("seed",seed);o.addProperty("nodeBudget",nodeBudget);
            o.addProperty("maxTraceEntries",maxTraceEntries);
            o.addProperty("uncertaintyPenalty",uncertaintyPenalty);
            o.addProperty("requireCompleteSearch",requireCompleteSearch);
            return o.toString();
        }
    }
    public static final class PinnedPolicy {
        private final Policy policy;private final String fileSha256;
        private PinnedPolicy(Policy policy,String sha) {
            this.policy=policy;this.fileSha256=sha;
        }
        public Policy value(){return policy;}
        public String fileSha256(){return fileSha256;}
        @Override public String toString(){return "PinnedOfflineShadowPolicy[redacted]";}
    }

    /**
     * A provided conditional, offline search graph. Only the root's legal
     * actions are executable. The graph is NOT a predicted opponent model.
     */
    public record Graph(String scenarioId,String root,
                        Map<String,ShadowSearchEngine.Position> positions) {
        public Graph {
            if(!validId(scenarioId)||!validId(root)||positions==null
                    ||positions.isEmpty()||positions.size()>MAX_GRAPH_NODES
                    ||!positions.containsKey(root))
                throw new IllegalArgumentException("invalid offline graph");
            TreeMap<String,ShadowSearchEngine.Position> ordered=new TreeMap<>();
            int edgeCount=0;
            for(var entry:positions.entrySet()){
                if(!validId(entry.getKey())||!entry.getKey().equals(entry.getValue().key()))
                    throw new IllegalArgumentException("graph state key mismatch");
                if(entry.getValue().branches().size()>32)
                    throw new IllegalArgumentException("too many branches");
                edgeCount+=entry.getValue().branches().size();
                for(var branch:entry.getValue().branches()){
                    if(!validAction(branch.id())||branch.outcomes().size()>16)
                        throw new IllegalArgumentException("invalid bounded branch");
                    for(var outcome:branch.outcomes()){
                        // No user free-text in reports; restrict internal state
                        // identifiers so private packets cannot be smuggled.
                        if(!validId(outcome.response())||!validId(outcome.nextState())
                                ||!Double.isFinite(outcome.immediateValue())
                                ||Math.abs(outcome.immediateValue())>100
                                ||outcome.uncertainty()>100)
                            throw new IllegalArgumentException("unsupported graph evidence");
                    }
                }
                ordered.put(entry.getKey(),entry.getValue());
            }
            if(edgeCount<1||edgeCount>MAX_GRAPH_EDGES)
                throw new IllegalArgumentException("graph budget exceeded");
            positions=Collections.unmodifiableMap(ordered);
        }
    }

    public enum Reason {
        SELECTED, EVIDENCE_REJECTED, NO_CUSTODY_ACCEPTED,
        POLICY_MISMATCH, SCENARIO_UNSUPPORTED, AMBIGUOUS_SNAPSHOT,
        MODEL_UNSUPPORTED, SEARCH_INCOMPLETE, NO_DECISION,
        ILLEGAL_DECISION, NONDETERMINISTIC, ENGINE_EXCEPTION,
        TRACE_BUDGET_EXCEEDED
    }
    public enum Attribution {
        AGREED_MOVE, AGREED_SWITCH, MOVE_TO_MOVE, MOVE_TO_SWITCH,
        SWITCH_TO_MOVE, SWITCH_TO_SWITCH, ABSTAINED, REJECTED_EVIDENCE
    }
    public record Trace(int ordinal,Reason reason,String group,
                        Attribution attribution,int visitedNodes,int rankedActions,
                        boolean abstained,boolean legal,Boolean legacyAgreement) {
        public Trace {
            if(ordinal<0||reason==null||attribution==null
                    ||visitedNodes<0||visitedNodes>256||rankedActions<0
                    ||rankedActions>64||group==null||!Set.of(
                    "MOVE","SWITCH","REJECTED").contains(group))
                throw new IllegalArgumentException("invalid trace");
        }
    }
    public record Count(int eligible,int agreed,int disagreed,int abstained,
                        int illegal,int unsupported) {}
    public record Report(String schema,Reason status,String policyRevision,
                         String pinnedPolicySha256,String modelSha256,
                         String scenarioSha256,String intakeReportSha256,
                         String replayFingerprint,List<Trace> traces,
                         Map<String,Count> attribution,boolean traceTruncated) {
        public Report {
            if(!SCHEMA.equals(schema)||status==null||traces==null
                    ||attribution==null||!validHash(replayFingerprint))
                throw new IllegalArgumentException("invalid replay report");
            traces=List.copyOf(traces);
            attribution=Collections.unmodifiableMap(new TreeMap<>(attribution));
        }
        public String json(){
            JsonObject root=new JsonObject();
            root.addProperty("schema",schema);
            root.addProperty("status",status.name());
            root.addProperty("policyRevision",policyRevision);
            root.addProperty("pinnedPolicySha256",pinnedPolicySha256);
            root.addProperty("modelSha256",modelSha256);
            root.addProperty("scenarioSha256",scenarioSha256);
            root.addProperty("intakeReportSha256",intakeReportSha256);
            root.addProperty("replayFingerprint",replayFingerprint);
            root.addProperty("traceTruncated",traceTruncated);
            root.addProperty("scope","offline-conditional-proxy;action-agreement-only;not-minecraft;not-win-rate");
            JsonArray list=new JsonArray();
            for(Trace t:traces){
                JsonObject item=new JsonObject();
                item.addProperty("ordinal",t.ordinal());
                item.addProperty("group",t.group());
                // Small cohorts cannot leak individual disagreements, even
                // via an otherwise anonymous fixed trace ordinal.
                boolean disclose=attribution.get(t.group()).eligible()>=3;
                item.addProperty("suppressed",!disclose);
                if(disclose){
                    item.addProperty("reason",t.reason().name());
                    item.addProperty("attribution",t.attribution().name());
                    item.addProperty("visitedNodes",t.visitedNodes());
                    item.addProperty("rankedActions",t.rankedActions());
                    item.addProperty("abstained",t.abstained());
                    item.addProperty("legal",t.legal());
                    if(t.legacyAgreement()==null)item.add("legacyAgreement",JsonNull.INSTANCE);
                    else item.addProperty("legacyAgreement",t.legacyAgreement());
                }
                list.add(item);
            }
            root.add("traces",list);
            JsonObject summaries=new JsonObject();
            for(var e:attribution.entrySet()){
                JsonObject c=new JsonObject();
                c.addProperty("suppressed",e.getValue().eligible()<3);
                if(e.getValue().eligible()>=3){
                    c.addProperty("eligible",e.getValue().eligible());
                    c.addProperty("agreed",e.getValue().agreed());
                    c.addProperty("disagreed",e.getValue().disagreed());
                    c.addProperty("abstained",e.getValue().abstained());
                    c.addProperty("illegal",e.getValue().illegal());
                    c.addProperty("unsupported",e.getValue().unsupported());
                }
                summaries.add(e.getKey(),c);
            }
            root.add("attribution",summaries);
            return root.toString();
        }
    }
    public record Evaluation(Phase7TrustedIntake.Report custodyReport,Report replay) {
        public Evaluation {
            Objects.requireNonNull(custodyReport);Objects.requireNonNull(replay);
        }
    }

    /** Deterministic canonical graph hash (sorted states, actions and responses). */
    public static String graphSha256(Map<String,Graph> graphs){
        Objects.requireNonNull(graphs);
        if(graphs.isEmpty()||graphs.size()>MAX_SCENARIOS)
            throw new IllegalArgumentException("graph collection invalid");
        JsonObject all=new JsonObject();
        for(String id:new TreeSet<>(graphs.keySet())){
            Graph graph=graphs.get(id);
            if(!id.equals(graph.scenarioId()))throw new IllegalArgumentException("graph key mismatch");
            JsonObject group=new JsonObject();
            group.addProperty("root",graph.root());
            JsonObject positions=new JsonObject();
            for(String state:new TreeSet<>(graph.positions().keySet())){
                var pos=graph.positions().get(state);
                JsonObject p=new JsonObject();
                p.addProperty("terminal",pos.terminalValue());
                JsonArray branches=new JsonArray();
                for(var branch:pos.branches().stream()
                        .sorted(Comparator.comparing(ShadowSearchEngine.Branch::id)).toList()){
                    JsonObject b=new JsonObject();b.addProperty("id",branch.id());
                    JsonArray outcomes=new JsonArray();
                    for(var outcome:branch.outcomes().stream()
                            .sorted(Comparator.comparing(ShadowSearchEngine.Outcome::response)).toList()){
                        JsonObject o=new JsonObject();
                        o.addProperty("response",outcome.response());
                        o.addProperty("probability",outcome.probability());
                        o.addProperty("value",outcome.immediateValue());
                        o.addProperty("uncertainty",outcome.uncertainty());
                        o.addProperty("next",outcome.nextState());
                        outcomes.add(o);
                    }
                    b.add("outcomes",outcomes);branches.add(b);
                }
                p.add("branches",branches);positions.add(state,p);
            }
            group.add("positions",positions);all.add(id,group);
        }
        return sha256(all.toString());
    }

    /** Exact public fixture binding; unordered caller maps cannot change the pin. */
    public static String scenarioSha256(Map<String,Phase7Benchmark.Scenario> scenarios) {
        Objects.requireNonNull(scenarios);
        if(scenarios.isEmpty()||scenarios.size()>MAX_SCENARIOS)
            throw new IllegalArgumentException("invalid scenario collection");
        JsonArray out=new JsonArray();
        for(String id:new TreeSet<>(scenarios.keySet())){
            Phase7Benchmark.Scenario s=scenarios.get(id);
            if(!id.equals(s.id())||!s.evidence().publicOnly()
                    ||!s.evidence().fresh()||!s.evidence().mechanicsSupported()
                    ||s.snapshot().turn()<1||s.legacy()!=null)
                throw new IllegalArgumentException("unverifiable public scenario");
            JsonObject o=new JsonObject();
            o.addProperty("id",id);o.addProperty("version",s.version());
            o.addProperty("state",Phase7Benchmark.stateFingerprint(s));out.add(o);
        }
        return sha256(out.toString());
    }

    /** The policy file alone does not independently verify source/custody. */
    public static Policy define(String revision,String installedSha,long seed,
                                Map<String,Graph> graphs,
                                Map<String,Phase7Benchmark.Scenario> scenarios,
                                int nodeBudget,int maxTraces,double penalty){
        return new Policy(PIN_SCHEMA,1,revision,"SHADOW_SEARCH_V1",
                graphSha256(graphs),scenarioSha256(scenarios),installedSha,
                seed,nodeBudget,maxTraces,penalty,true);
    }

    public static Path writeLocal(Path repository,Policy policy) throws IOException {
        Objects.requireNonNull(policy);
        Path directory=directory(repository);
        Path file=directory.resolve("phase7_"+policy.revision()+".json");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("immutable policy revision already exists");
        Path temp=Files.createTempFile(directory,".phase7-", ".tmp");
        try{
            Files.writeString(temp,policy.json(),StandardCharsets.UTF_8);
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE);
        }finally{Files.deleteIfExists(temp);}
        return file;
    }
    public static PinnedPolicy load(Path repository,String revision,String expectedSha)
            throws IOException {
        if(!validId(revision)||!validHash(expectedSha))
            throw new IllegalArgumentException("invalid independently pinned policy");
        Path file=directory(repository).resolve("phase7_"+revision+".json");
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                ||Files.size(file)>MAX_FILE)throw new IOException("policy unavailable");
        byte[] bytes;
        try(var stream=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){
            bytes=stream.readNBytes(MAX_FILE+1);
        }
        if(bytes.length>MAX_FILE||!sha256(bytes).equals(expectedSha))
            throw new IllegalArgumentException("untrusted offline policy");
        JsonObject obj=StrictOwnJson.read(new String(bytes,StandardCharsets.UTF_8));
        keys(obj,"schema","version","revision","engine","modelSha256",
                "scenarioSha256","installedArtifactSha256","seed","nodeBudget",
                "maxTraceEntries","uncertaintyPenalty","requireCompleteSearch");
        JsonElement strict=obj.get("requireCompleteSearch");
        if(!strict.isJsonPrimitive()||!strict.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("invalid search completeness rule");
        Policy policy=new Policy(string(obj,"schema"),integer(obj,"version"),
                string(obj,"revision"),string(obj,"engine"),string(obj,"modelSha256"),
                string(obj,"scenarioSha256"),string(obj,"installedArtifactSha256"),
                numberLong(obj,"seed"),integer(obj,"nodeBudget"),
                integer(obj,"maxTraceEntries"),doubleNumber(obj,"uncertaintyPenalty"),
                strict.getAsBoolean());
        if(!revision.equals(policy.revision()))throw new IllegalArgumentException("revision drift");
        return new PinnedPolicy(policy,expectedSha);
    }
    private static Path directory(Path repository) throws IOException {
        Path root=repository.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(root)||!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("unsafe repository root");
        Path dir=root;
        for(String part:List.of("run","phase7-corpus","shadow-replay")){
            dir=dir.resolve(part);
            if(Files.isSymbolicLink(dir))throw new IOException("unsafe local policy directory");
            if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir);
            if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe local policy directory");
        }
        return dir;
    }
    private static void keys(JsonObject o,String... names){
        if(!o.keySet().equals(Set.of(names)))
            throw new IllegalArgumentException("unexpected or missing policy fields");
    }
    private static String string(JsonObject o,String key){
        JsonElement e=o.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid policy value");
        return e.getAsString();
    }
    private static int integer(JsonObject o,String key){
        long v=numberLong(o,key);
        if(v<Integer.MIN_VALUE||v>Integer.MAX_VALUE)
            throw new IllegalArgumentException("invalid integer");
        return (int)v;
    }
    private static long numberLong(JsonObject o,String key){
        JsonElement e=o.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber()
                ||!e.getAsString().matches("-?(0|[1-9][0-9]{0,18})"))
            throw new IllegalArgumentException("invalid integer");
        return Long.parseLong(e.getAsString());
    }
    private static double doubleNumber(JsonObject o,String key){
        JsonElement e=o.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("invalid real");
        return Double.parseDouble(e.getAsString());
    }

    /**
     * End-to-end replay delegates custody to the REAL Phase 7.5 importer.
     * A provided synthetic graph never promotes unreviewed evidence.
     */
    public static Evaluation replayLocal(Path repository,List<String> bundles,
            Map<String,Phase7Benchmark.Scenario> scenarios,String custodianKeyId,
            String externallyPinnedKeySha256,PinnedPolicy policy,
            Map<String,Graph> graphs) {
        Objects.requireNonNull(policy);Objects.requireNonNull(graphs);
        Policy config=policy.value();
        boolean matches=scenarioSha256(scenarios).equals(config.scenarioSha256())
                && graphSha256(graphs).equals(config.modelSha256())
                && scenarios.keySet().equals(graphs.keySet());
        if(!matches)throw new IllegalArgumentException("incompatible pinned Shadow graph/scenarios");

        ReplaySession session=new ReplaySession(config,scenarios,graphs);
        Phase7TrustedIntake.Report custody=Phase7TrustedIntake.evaluateLocal(
                repository,bundles,scenarios,custodianKeyId,externallyPinnedKeySha256,
                config.seed(),session::choose,config.installedArtifactSha256());
        return new Evaluation(custody,session.finish(policy,custody));
    }

    private record Outcome(Reason reason,String action,int nodes,int ranking) {}

    /** The sole callback to Phase 7.5: never a live battle decision. */
    private static final class ReplaySession {
        final Policy policy;
        final Map<String,Phase7Benchmark.Scenario> scenarios;
        final Map<String,Graph> graphs;
        final Map<String,List<String>> snapshotKeys=new HashMap<>();
        final Map<String,Outcome> outcomes=new HashMap<>();
        ReplaySession(Policy policy,Map<String,Phase7Benchmark.Scenario> scenarios,
                      Map<String,Graph> graphs){
            this.policy=policy;this.scenarios=Map.copyOf(scenarios);this.graphs=Map.copyOf(graphs);
            for(var e:scenarios.entrySet()){
                String key=snapshotKey(e.getValue().snapshot());
                snapshotKeys.computeIfAbsent(key,k->new ArrayList<>()).add(e.getKey());
            }
        }
        String choose(BattleSnapshot snapshot,long seed) {
            String key=snapshotKey(snapshot);
            List<String> bindings=snapshotKeys.getOrDefault(key,List.of());
            if(bindings.size()!=1){
                outcomes.put(key,new Outcome(Reason.AMBIGUOUS_SNAPSHOT,null,0,0));
                return null;
            }
            String id=bindings.getFirst();
            Phase7Benchmark.Scenario s=scenarios.get(id);
            if(!s.snapshot().equals(snapshot)){
                outcomes.put(key,new Outcome(Reason.SCENARIO_UNSUPPORTED,null,0,0));
                return null;
            }
            Graph graph=graphs.get(id);
            Outcome first=run(snapshot,graph);
            Outcome second=run(snapshot,graph);
            if(!first.equals(second))first=new Outcome(Reason.NONDETERMINISTIC,null,0,0);
            outcomes.put(key,first);
            return first.action();
        }
        private Outcome run(BattleSnapshot snapshot,Graph graph) {
            if(graph==null)return new Outcome(Reason.MODEL_UNSUPPORTED,null,0,0);
            try {
                for(var rootAction:graph.positions().get(graph.root()).branches())
                    if(!legal(snapshot,rootAction.id()))
                        return new Outcome(Reason.ILLEGAL_DECISION,null,0,0);
                var search=ShadowSearchEngine.search(snapshot,graph.root(),
                        graph.positions(),policy.nodeBudget(),policy.uncertaintyPenalty());
                if(!search.complete())return new Outcome(Reason.SEARCH_INCOMPLETE,null,
                        Math.min(256,search.nodes()),search.ranking().size());
                String action=search.preferred();
                if(action==null)return new Outcome(Reason.NO_DECISION,null,
                        Math.min(256,search.nodes()),search.ranking().size());
                if(!legal(snapshot,action))return new Outcome(Reason.ILLEGAL_DECISION,null,
                        Math.min(256,search.nodes()),search.ranking().size());
                return new Outcome(Reason.SELECTED,action,Math.min(256,search.nodes()),
                        search.ranking().size());
            } catch(RuntimeException invalid) {
                return new Outcome(Reason.ENGINE_EXCEPTION,null,0,0);
            }
        }
        Report finish(PinnedPolicy pinned,Phase7TrustedIntake.Report custody){
            List<Phase7Corpus.Row> rows=custody.corpus().rows();
            List<Trace> traces=new ArrayList<>();
            Map<String,int[]> totals=new TreeMap<>();
            for(String group:List.of("MOVE","SWITCH","REJECTED"))
                totals.put(group,new int[6]);
            int ordinal=0;
            for(var row:rows){
                boolean accepted=row.status()==Phase7Corpus.Status.ACCEPTED;
                String group=!accepted?"REJECTED":
                        row.legacyAction().startsWith("move:")?"MOVE":"SWITCH";
                int[] counts=totals.get(group);
                if(accepted)counts[0]++;
                String key=accepted?sessionKeyFromFingerprint(row,scenarios):null;
                Outcome result=accepted?outcomes.get(key):null;
                Reason why=!accepted?Reason.EVIDENCE_REJECTED:
                        result==null?Reason.NO_CUSTODY_ACCEPTED:result.reason();
                if(accepted && row.shadowAction()!=null
                        && (result==null||!row.shadowAction().equals(result.action())))
                    why=Reason.NONDETERMINISTIC;
                boolean selected=accepted&&why==Reason.SELECTED
                        && result!=null&&result.action()!=null;
                boolean agreement=selected&&row.legacyAction().equals(result.action());
                if(selected){
                    if(agreement)counts[1]++;else counts[2]++;
                }else if(accepted){
                    counts[3]++;
                    if(why==Reason.ILLEGAL_DECISION)counts[4]++;
                    else counts[5]++;
                }
                Attribution attribution=!accepted?Attribution.REJECTED_EVIDENCE:
                        !selected?Attribution.ABSTAINED:attribute(row.legacyAction(),result.action());
                if(ordinal<policy.maxTraceEntries())
                    traces.add(new Trace(ordinal,why,group,attribution,
                            result==null?0:result.nodes(),
                            result==null?0:result.ranking(),!selected,
                            selected,selected?agreement:null));
                ordinal++;
            }
            Map<String,Count> groups=new TreeMap<>();
            for(var e:totals.entrySet()){
                int[] a=e.getValue();
                groups.put(e.getKey(),new Count(a[0],a[1],a[2],a[3],a[4],a[5]));
            }
            Reason status=rows.size()>policy.maxTraceEntries()
                    ?Reason.TRACE_BUDGET_EXCEEDED:rows.stream()
                    .noneMatch(row->row.status()==Phase7Corpus.Status.ACCEPTED)
                    ?Reason.NO_CUSTODY_ACCEPTED:Reason.SELECTED;
            // Hash sanitized metadata only; no raw event, UUID or action enters.
            String material=policy.json()+"|"+custody.json()+"|"+groups+"|"+traces;
            return new Report(SCHEMA,status,policy.revision(),pinned.fileSha256(),
                    policy.modelSha256(),policy.scenarioSha256(),sha256(custody.json()),
                    sha256(material),traces,groups,rows.size()>policy.maxTraceEntries());
        }
    }

    private static String sessionKeyFromFingerprint(Phase7Corpus.Row row,
                   Map<String,Phase7Benchmark.Scenario> scenarios){
        var s=scenarios.get(row.scenario());
        return s!=null&&Phase7Benchmark.stateFingerprint(s).equals(row.stateFingerprint())
                ?snapshotKey(s.snapshot()):null;
    }
    private static Attribution attribute(String legacy,String shadow){
        boolean leftMove=legacy.startsWith("move:");
        boolean rightMove=shadow.startsWith("move:");
        if(legacy.equals(shadow))return leftMove?Attribution.AGREED_MOVE:Attribution.AGREED_SWITCH;
        if(leftMove)return rightMove?Attribution.MOVE_TO_MOVE:Attribution.MOVE_TO_SWITCH;
        return rightMove?Attribution.SWITCH_TO_MOVE:Attribution.SWITCH_TO_SWITCH;
    }
    private static String snapshotKey(BattleSnapshot s) {
        // Canonical fields in the strict public-only scenario. Snapshot keys
        // are internal and never emitted or treated as custody signatures.
        List<String> moves=new ArrayList<>(s.legalMoves()),switches=new ArrayList<>(s.legalSwitches());
        Collections.sort(moves);Collections.sort(switches);
        return sha256(s.turn()+"|"+s.self().species()+"|"+s.opponent().species()
                +"|"+moves+"|"+switches);
    }
    private static boolean legal(BattleSnapshot snapshot,String action){
        if(!validAction(action))return false;
        if(action.startsWith("move:"))
            return snapshot.legalMoves().contains(action.substring(5));
        return snapshot.legalSwitches().contains(action.substring(7));
    }
    private static boolean validId(String s){return s!=null&&ID.matcher(s).matches();}
    private static boolean validHash(String s){return s!=null&&HASH.matcher(s).matches();}
    private static boolean validAction(String s){
        if(s==null)return false;
        return s.startsWith("move:")&&validId(s.substring(5))
                ||s.startsWith("switch:")&&validId(s.substring(7));
    }
    private static String sha256(String s){return sha256(s.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] b){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
