package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Phase 7.11: versioned, independent Phase 4 public-mechanics adapter.
 * Only offline verification sources; no reference to unfinished Phase 4 code.
 * Signing verifies custody, not scientific correctness of the underlying mechanics.
 * All source authorization must be pinned via a separately trusted channel.
 */
public final class Phase7MechanicsAdapter {
    private Phase7MechanicsAdapter() {}
    public static final String SCHEMA="phase7.11.v1";
    public static final String POLICY_SCHEMA="phase7.11.policy.v1";
    public static final String BUNDLE_SCHEMA="phase7.11.mechanics.v1";
    public static final String DOMAIN="MaestroAI-Phase7.11-ReviewedPublicMechanics-v1\n";
    private static final int MAX_EDGES=128, MAX_POLICY_BYTES=8192, MAX_REPORT_GROUPS=4;
    private static final java.util.regex.Pattern NAME=
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern SHA=
            java.util.regex.Pattern.compile("[0-9a-f]{64}");

    public enum Source { PHASE4_REVIEWED_PUBLIC_V1, SYNTHETIC_TEST_ONLY }
    public enum Verdict { REVIEWED_PUBLIC, UNKNOWN, UNSUPPORTED }
    public enum Group { MOVE_MOVE, MOVE_SWITCH, SWITCH_MOVE, SWITCH_SWITCH }
    public enum Status {
        ELIGIBLE_OFFLINE_ONLY, SYNTHETIC_NOT_VERIFIED, UNTRUSTED_REVIEWER,
        INVALID_SIGNATURE, UNBOUND_SOURCE, POLICY_MISMATCH, VERSION_INCOMPATIBLE,
        TRANSITION_DRIFT, DUPLICATE_RECORD, MISSING_EVIDENCE, ILLEGAL_ACTION,
        UNKNOWN_MECHANICS, UNSUPPORTED_MECHANICS, COVERAGE_INSUFFICIENT,
        REGRESSION, SOURCE_DRIFT
    }

    /** Public mechanics evidence for one fully pinned public-state transition. */
    public record Entry(String edgeSha256,Group group,Verdict verdict,
                        String mechanicsReceiptSha256) {
        public Entry {
            if(!validSha(edgeSha256)||group==null||verdict==null
                    ||!validSha(mechanicsReceiptSha256))
                throw new IllegalArgumentException("invalid reviewed public edge");
        }
    }
    /**
     * Source commit and reviewed mechanics corpus are independently pinned by
     * the local policy; this data object never grants itself authenticity.
     */
    public record Bundle(String schema,int version,String revision,Source source,
                         String sourceCommitSha256,String sourceCorpusSha256,
                         String sourceContract,String artifactSha256,
                         String transitionsSha256,List<Entry> entries) {
        public Bundle {
            if(!BUNDLE_SCHEMA.equals(schema)||version!=1||!validName(revision)
                    ||source==null||!validSha(sourceCommitSha256)
                    ||!validSha(sourceCorpusSha256)
                    ||!"PHASE4_PUBLIC_MECHANICS_V1".equals(sourceContract)
                    ||!validSha(artifactSha256)||!validSha(transitionsSha256)
                    ||entries==null||entries.isEmpty()||entries.size()>MAX_EDGES)
                throw new IllegalArgumentException("unrecognized mechanics evidence bundle");
            entries=List.copyOf(entries);
        }
        public String json(){
            JsonObject j=new JsonObject();
            j.addProperty("schema",schema);j.addProperty("version",version);
            j.addProperty("revision",revision);j.addProperty("source",source.name());
            j.addProperty("sourceCommitSha256",sourceCommitSha256);
            j.addProperty("sourceCorpusSha256",sourceCorpusSha256);
            j.addProperty("sourceContract",sourceContract);
            j.addProperty("artifactSha256",artifactSha256);
            j.addProperty("transitionsSha256",transitionsSha256);
            JsonArray records=new JsonArray();
            for(Entry r:entries.stream().sorted(Comparator.comparing(Entry::edgeSha256)).toList()){
                JsonObject v=new JsonObject();
                v.addProperty("edgeSha256",r.edgeSha256());v.addProperty("group",r.group().name());
                v.addProperty("verdict",r.verdict().name());
                v.addProperty("mechanicsReceiptSha256",r.mechanicsReceiptSha256());
                records.add(v);
            }
            j.add("entries",records);
            return j.toString();
        }
    }
    public record Attestation(String publicKeyDerBase64,String signatureBase64) {
        public Attestation {
            if(publicKeyDerBase64==null||signatureBase64==null
                    ||publicKeyDerBase64.length()>2048||signatureBase64.length()>256)
                throw new IllegalArgumentException("invalid mechanics attestation envelope");
        }
    }

    /**
     * Separate explicit minimum coverage for each action-pair stratum.
     * Zero means optional, but every listed public transition still requires
     * an evidence record, and ANY unsupported record blocks eligibility.
     */
    public record Gate(int moveMoveMinimum,int moveSwitchMinimum,
                       int switchMoveMinimum,int switchSwitchMinimum,
                       int minimumTotal,int maximumUnknown,int maximumUnsupported) {
        public Gate {
            if(moveMoveMinimum<0||moveSwitchMinimum<0||switchMoveMinimum<0
                    ||switchSwitchMinimum<0||minimumTotal<1||minimumTotal>MAX_EDGES
                    ||moveMoveMinimum>MAX_EDGES||moveSwitchMinimum>MAX_EDGES
                    ||switchMoveMinimum>MAX_EDGES||switchSwitchMinimum>MAX_EDGES
                    ||maximumUnknown<0||maximumUnknown>MAX_EDGES
                    ||maximumUnsupported<0||maximumUnsupported>MAX_EDGES)
                throw new IllegalArgumentException("invalid per-group coverage requirements");
        }
    }
    public record Policy(String schema,int version,String revision,
                         String pinnedSourceCommitSha256,String pinnedSourceCorpusSha256,
                         String pinnedArtifactSha256,String pinnedTransitionSha256,
                         String pinnedReviewerKeySha256,String originalBundleSha256,
                         Gate gate,boolean requireExternalReviewer) {
        public Policy {
            if(!POLICY_SCHEMA.equals(schema)||version!=1||!validName(revision)
                    ||!validSha(pinnedSourceCommitSha256)
                    ||!validSha(pinnedSourceCorpusSha256)
                    ||!validSha(pinnedArtifactSha256)
                    ||!validSha(pinnedTransitionSha256)
                    ||!validSha(pinnedReviewerKeySha256)
                    ||!validSha(originalBundleSha256)||gate==null
                    ||!requireExternalReviewer)
                throw new IllegalArgumentException("invalid trusted mechanics policy");
        }
        public String json(){
            JsonObject j=new JsonObject();
            j.addProperty("schema",schema);j.addProperty("version",version);
            j.addProperty("revision",revision);
            j.addProperty("pinnedSourceCommitSha256",pinnedSourceCommitSha256);
            j.addProperty("pinnedSourceCorpusSha256",pinnedSourceCorpusSha256);
            j.addProperty("pinnedArtifactSha256",pinnedArtifactSha256);
            j.addProperty("pinnedTransitionSha256",pinnedTransitionSha256);
            j.addProperty("pinnedReviewerKeySha256",pinnedReviewerKeySha256);
            j.addProperty("originalBundleSha256",originalBundleSha256);
            JsonObject g=new JsonObject();
            g.addProperty("moveMoveMinimum",gate.moveMoveMinimum());
            g.addProperty("moveSwitchMinimum",gate.moveSwitchMinimum());
            g.addProperty("switchMoveMinimum",gate.switchMoveMinimum());
            g.addProperty("switchSwitchMinimum",gate.switchSwitchMinimum());
            g.addProperty("minimumTotal",gate.minimumTotal());
            g.addProperty("maximumUnknown",gate.maximumUnknown());
            g.addProperty("maximumUnsupported",gate.maximumUnsupported());
            j.add("gate",g);
            j.addProperty("requireExternalReviewer",requireExternalReviewer);
            return j.toString();
        }
    }
    public static final class PinnedPolicy {
        private final Policy policy;
        private final String fileSha256;
        private PinnedPolicy(Policy p,String sha){this.policy=p;this.fileSha256=sha;}
        public Policy value(){return policy;}
        public String fileSha256(){return fileSha256;}
        @Override public String toString(){return "PinnedPhase4MechanicsAdapter[redacted]";}
    }

    public record Counts(int total,int reviewed,int unknown,int unsupported,
                         int missing,int duplicates) {
        public Counts {
            if(total<0||reviewed<0||unknown<0||unsupported<0||missing<0||duplicates<0)
                throw new IllegalArgumentException("invalid coverage counters");
        }
    }
    public record Report(String schema,Status status,String policySha256,
                         String bundleSha256,String sourceCommitSha256,
                         String transitionsSha256,String reviewerKeySha256,
                         Map<Group,Counts> groups,Counts totals,String reportFingerprint) {
        public Report {
            if(!SCHEMA.equals(schema)||status==null||!validSha(reportFingerprint)
                    ||groups==null||groups.size()!=MAX_REPORT_GROUPS||totals==null)
                throw new IllegalArgumentException("invalid public mechanics audit");
            groups=Collections.unmodifiableMap(new EnumMap<>(groups));
        }
        public boolean eligible(){return status==Status.ELIGIBLE_OFFLINE_ONLY;}
        public String json(){
            JsonObject j=new JsonObject();
            j.addProperty("schema",schema);j.addProperty("status",status.name());
            j.addProperty("eligibleOffline",eligible());
            j.addProperty("policySha256",policySha256);
            j.addProperty("bundleSha256",bundleSha256);
            j.addProperty("sourceCommitSha256",sourceCommitSha256);
            j.addProperty("transitionsSha256",transitionsSha256);
            j.addProperty("reviewerKeySha256",reviewerKeySha256);
            j.addProperty("reportFingerprint",reportFingerprint);
            j.addProperty("scope","externally-reviewed-public-only;not-minecraft;not-win-rate;no-gameplay-promotion");
            JsonObject g=new JsonObject();
            for(Group group:Group.values()){
                Counts c=groups.get(group);
                JsonObject v=new JsonObject();
                boolean disclosed=c.total()>=3;
                v.addProperty("suppressed",!disclosed);
                if(disclosed){
                    v.addProperty("total",c.total());v.addProperty("reviewed",c.reviewed());
                    v.addProperty("unknown",c.unknown());v.addProperty("unsupported",c.unsupported());
                    v.addProperty("missing",c.missing());v.addProperty("duplicates",c.duplicates());
                }
                g.add(group.name(),v);
            }
            j.add("groups",g);
            JsonObject summary=new JsonObject();
            // The overall count can reveal a small single observation.
            // Complementary suppression: if a nonempty subgroup is too
            // small, publishing the grand total permits subtraction attacks.
            boolean smallChild=groups.values().stream().anyMatch(
                    count->count.total()>0&&count.total()<3);
            boolean discloseTotal=totals.total()>=3&&!smallChild;
            summary.addProperty("suppressed",!discloseTotal);
            if(discloseTotal){
                summary.addProperty("total",totals.total());
                summary.addProperty("reviewed",totals.reviewed());
                summary.addProperty("unknown",totals.unknown());
                summary.addProperty("unsupported",totals.unsupported());
                summary.addProperty("missing",totals.missing());
                summary.addProperty("duplicates",totals.duplicates());
            }
            j.add("summary",summary);
            return j.toString();
        }
    }
    public record Comparison(String schema,Status status,Status referenceStatus,
                             Status candidateStatus,String referenceBundleSha256,
                             String candidateBundleSha256,int regressed,int improved,
                             int unchanged,String fingerprint) {
        public Comparison {
            if(!SCHEMA.equals(schema)||status==null||!validSha(fingerprint))
                throw new IllegalArgumentException("invalid mechanics regression comparison");
        }
        public String json(){
            JsonObject o=new JsonObject();
            o.addProperty("schema",schema);
            o.addProperty("status",status.name());
            o.addProperty("referenceStatus",referenceStatus.name());
            o.addProperty("candidateStatus",candidateStatus.name());
            o.addProperty("referenceBundleSha256",referenceBundleSha256);
            o.addProperty("candidateBundleSha256",candidateBundleSha256);
            o.addProperty("fingerprint",fingerprint);
            o.addProperty("scope","fixed-public-cohort;no-gameplay-promotion");
            boolean mask=regressed+improved+unchanged<3;
            o.addProperty("suppressed",mask);
            if(!mask){
                o.addProperty("regressed",regressed);
                o.addProperty("improved",improved);
                o.addProperty("unchanged",unchanged);
            }
            return o.toString();
        }
    }

    /** Each transition is canonicalized with its source/successor public IDs. */
    public static String edgeSha256(Phase7SelfPlay.Edge e){
        Objects.requireNonNull(e);
        return sha256("phase7.11.edge.v1|"+e.source()+"|"+e.firstAction()+"|"+
                e.secondAction()+"|"+e.successor()+"|"+e.publicEventSha256()+"|"+e.mechanic());
    }
    public static Group group(Phase7SelfPlay.Edge e){
        boolean a=e.firstAction().startsWith("move:");
        boolean b=e.secondAction().startsWith("move:");
        return a?(b?Group.MOVE_MOVE:Group.MOVE_SWITCH):
                (b?Group.SWITCH_MOVE:Group.SWITCH_SWITCH);
    }
    public static String bundleSha256(Bundle bundle){return sha256(bundle.json());}
    public static byte[] signaturePayload(String bundleSha){
        if(!validSha(bundleSha))throw new IllegalArgumentException("invalid signed mechanics digest");
        return (DOMAIN+bundleSha+"\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Source and trust pins MUST be issued externally to the bundle.
     * This builder merely assembles a candidate policy; writeLocal is not
     * a reviewer approval. Its hash must also be independently pinned.
     */
    public static Policy define(String revision,Bundle reference,
                                String independentlyTrustedReviewerSha,Gate gate){
        return new Policy(POLICY_SCHEMA,1,revision,reference.sourceCommitSha256(),
                reference.sourceCorpusSha256(),reference.artifactSha256(),
                reference.transitionsSha256(),independentlyTrustedReviewerSha,
                bundleSha256(reference),gate,true);
    }
    public static Path writeLocal(Path repo,Policy policy)throws IOException{
        Path dir=directory(repo),file=dir.resolve("phase7_"+policy.revision()+".json");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("policy revision immutable");
        Path tmp=Files.createTempFile(dir,".phase7-", ".tmp");
        try{
            Files.writeString(tmp,policy.json(),StandardCharsets.UTF_8);
            Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE);
        }finally{Files.deleteIfExists(tmp);}
        return file;
    }
    public static PinnedPolicy load(Path repo,String revision,String trustedSha)throws IOException{
        if(!validName(revision)||!validSha(trustedSha))
            throw new IllegalArgumentException("policy trust pin required");
        Path file=directory(repo).resolve("phase7_"+revision+".json");
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                ||Files.size(file)>MAX_POLICY_BYTES)
            throw new IOException("policy unavailable");
        byte[] bytes;
        try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){
            bytes=in.readNBytes(MAX_POLICY_BYTES+1);
        }
        if(bytes.length>MAX_POLICY_BYTES||!sha256(bytes).equals(trustedSha))
            throw new IllegalArgumentException("policy exact-byte mismatch");
        JsonObject j=StrictOwnJson.read(new String(bytes,StandardCharsets.UTF_8));
        fields(j,"schema","version","revision","pinnedSourceCommitSha256",
                "pinnedSourceCorpusSha256","pinnedArtifactSha256",
                "pinnedTransitionSha256","pinnedReviewerKeySha256",
                "originalBundleSha256","gate","requireExternalReviewer");
        JsonObject g=j.getAsJsonObject("gate");
        fields(g,"moveMoveMinimum","moveSwitchMinimum","switchMoveMinimum",
                "switchSwitchMinimum","minimumTotal","maximumUnknown","maximumUnsupported");
        var flag=j.get("requireExternalReviewer");
        if(!flag.isJsonPrimitive()||!flag.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("unsupported reviewer requirement");
        Gate gate=new Gate(number(g,"moveMoveMinimum"),number(g,"moveSwitchMinimum"),
                number(g,"switchMoveMinimum"),number(g,"switchSwitchMinimum"),
                number(g,"minimumTotal"),number(g,"maximumUnknown"),
                number(g,"maximumUnsupported"));
        Policy p=new Policy(string(j,"schema"),number(j,"version"),string(j,"revision"),
                string(j,"pinnedSourceCommitSha256"),
                string(j,"pinnedSourceCorpusSha256"),
                string(j,"pinnedArtifactSha256"),
                string(j,"pinnedTransitionSha256"),
                string(j,"pinnedReviewerKeySha256"),
                string(j,"originalBundleSha256"),gate,flag.getAsBoolean());
        if(!revision.equals(p.revision()))throw new IllegalArgumentException("revision mismatch");
        return new PinnedPolicy(p,trustedSha);
    }
    private static Path directory(Path repo)throws IOException{
        Path root=repo.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(root)||!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("unsafe project root");
        Path dir=root;
        for(String part:List.of("run","phase7-corpus","mechanics")){
            dir=dir.resolve(part);
            if(Files.isSymbolicLink(dir))throw new IOException("symlinked policy directory");
            if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir);
            if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe policy directory");
        }
        return dir;
    }
    private static void fields(JsonObject j,String... required){
        if(!j.keySet().equals(Set.of(required)))
            throw new IllegalArgumentException("unsupported or absent policy field");
    }
    private static String string(JsonObject j,String key){
        var v=j.get(key);
        if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid policy string");
        return v.getAsString();
    }
    private static int number(JsonObject j,String key){
        var v=j.get(key);
        if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber()
                ||!v.getAsString().matches("0|[1-9][0-9]{0,6}"))
            throw new IllegalArgumentException("invalid policy number");
        return Integer.parseInt(v.getAsString());
    }

    /** No signed bundle can authenticate itself; reviewer SHA comes from policy. */
    private static Status signature(Bundle b,Attestation a,Policy p){
        if(a==null)return Status.INVALID_SIGNATURE;
        try{
            byte[] der=Base64.getDecoder().decode(a.publicKeyDerBase64());
            if(der.length>1024||!sha256(der).equals(p.pinnedReviewerKeySha256()))
                return Status.UNTRUSTED_REVIEWER;
            byte[] sig=Base64.getDecoder().decode(a.signatureBase64());
            if(sig.length!=64)return Status.INVALID_SIGNATURE;
            var key=KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
            var verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(signaturePayload(bundleSha256(b)));
            return verifier.verify(sig)?null:Status.INVALID_SIGNATURE;
        }catch(Exception ignored){return Status.INVALID_SIGNATURE;}
    }

    /**
     * All transitions are covered, even unselected branches. A genuine
     * external Phase 4 integration must supply reviewed and pinned public
     * mechanics; synthetic-marked records NEVER become verified mechanics.
     */
    public static Report audit(Phase7SelfPlay.Transitions transitions,
                               Map<String,Phase7Benchmark.Scenario> firstPublic,
                               Map<String,Phase7Benchmark.Scenario> secondPublic,
                               Bundle bundle,Attestation attestation,PinnedPolicy pinned){
        return auditInternal(transitions,firstPublic,secondPublic,bundle,attestation,pinned,true);
    }
    private static Report auditInternal(Phase7SelfPlay.Transitions transitions,
                               Map<String,Phase7Benchmark.Scenario> firstPublic,
                               Map<String,Phase7Benchmark.Scenario> secondPublic,
                               Bundle bundle,Attestation attestation,PinnedPolicy pinned,
                               boolean exactReference){
        Objects.requireNonNull(transitions);Objects.requireNonNull(bundle);
        Objects.requireNonNull(firstPublic);Objects.requireNonNull(secondPublic);
        Objects.requireNonNull(pinned);
        Policy p=pinned.value();
        String bundleSha=bundleSha256(bundle);
        String transitionSha=bundle.transitionsSha256();
        String base=SCHEMA+"|"+pinned.fileSha256()+"|"+bundleSha+"|"+transitionSha;
        EnumMap<Group,int[]> counters=new EnumMap<>(Group.class);
        for(Group g:Group.values())counters.put(g,new int[6]);
        Status status=null;
        if(!p.pinnedSourceCommitSha256().equals(bundle.sourceCommitSha256())
                ||!p.pinnedSourceCorpusSha256().equals(bundle.sourceCorpusSha256())
                ||!p.pinnedArtifactSha256().equals(bundle.artifactSha256()))
            status=Status.UNBOUND_SOURCE;
        else if(!p.pinnedTransitionSha256().equals(bundle.transitionsSha256())
                ||!bundle.transitionsSha256().equals(
                        Phase7SelfPlay.transitionsSha256(transitions,firstPublic,secondPublic)))
            status=Status.TRANSITION_DRIFT;
        else if(exactReference && !p.originalBundleSha256().equals(bundleSha))
            status=Status.POLICY_MISMATCH;
        else if(bundle.source()!=Source.PHASE4_REVIEWED_PUBLIC_V1)
            status=Status.SYNTHETIC_NOT_VERIFIED;
        else status=signature(bundle,attestation,p);
        Map<String,Entry> lookup=new HashMap<>();
        Set<String> repeated=new HashSet<>();
        for(Entry r:bundle.entries()){
            if(lookup.putIfAbsent(r.edgeSha256(),r)!=null)repeated.add(r.edgeSha256());
        }
        if(status==null && !repeated.isEmpty())status=Status.DUPLICATE_RECORD;
        boolean illegal=false;
        // Validate every public edge, including branches Shadow will not select.
        for(var edge:transitions.edges()){
            Group group=group(edge);
            int[] v=counters.get(group);v[0]++;
            String key=edgeSha256(edge);
            Entry evidence=lookup.get(key);
            if(evidence==null){v[4]++;continue;}
            if(repeated.contains(key))v[5]++;
            if(evidence.group()!=group){v[4]++;continue;}
            var origin=transitions.nodes().get(edge.source());
            var first=firstPublic.get(origin.firstScenario()).snapshot();
            var second=secondPublic.get(origin.secondScenario()).snapshot();
            if(!legal(first,edge.firstAction())||!legal(second,edge.secondAction()))
                illegal=true;
            // Explicit unsupported mechanics cannot be overridden by a
            // signed but mistaken REVIEWED_PUBLIC entry.
            if(edge.mechanic()!=Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION){
                v[3]++;
                continue;
            }
            switch(evidence.verdict()){
                case REVIEWED_PUBLIC -> v[1]++;
                case UNKNOWN -> v[2]++;
                case UNSUPPORTED -> v[3]++;
            }
        }
        Counts overall=totals(counters);
        if(status==null){
            if(illegal)status=Status.ILLEGAL_ACTION;
            else if(lookup.size()!=transitions.edges().size()
                    ||overall.missing()>0||overall.duplicates()>0)
                status=Status.MISSING_EVIDENCE;
            else if(overall.unsupported()>p.gate().maximumUnsupported())
                status=Status.UNSUPPORTED_MECHANICS;
            else if(overall.unknown()>p.gate().maximumUnknown())
                status=Status.UNKNOWN_MECHANICS;
            else if(overall.reviewed()<p.gate().minimumTotal()
                    ||counters.get(Group.MOVE_MOVE)[1]<p.gate().moveMoveMinimum()
                    ||counters.get(Group.MOVE_SWITCH)[1]<p.gate().moveSwitchMinimum()
                    ||counters.get(Group.SWITCH_MOVE)[1]<p.gate().switchMoveMinimum()
                    ||counters.get(Group.SWITCH_SWITCH)[1]<p.gate().switchSwitchMinimum())
                status=Status.COVERAGE_INSUFFICIENT;
            else if(overall.unknown()>0||overall.unsupported()>0)
                status=Status.UNSUPPORTED_MECHANICS;
            else status=Status.ELIGIBLE_OFFLINE_ONLY;
        }
        EnumMap<Group,Counts> counts=new EnumMap<>(Group.class);
        for(var g:Group.values()){
            int[] x=counters.get(g);
            counts.put(g,new Counts(x[0],x[1],x[2],x[3],x[4],x[5]));
        }
        return new Report(SCHEMA,status,pinned.fileSha256(),bundleSha,
                bundle.sourceCommitSha256(),transitionSha,p.pinnedReviewerKeySha256(),
                counts,overall,sha256(base+"|"+status+"|"+counts));
    }

    private static Counts totals(Map<Group,int[]> groups){
        int[] sum=new int[6];
        for(int[] x:groups.values())for(int i=0;i<sum.length;i++)sum[i]+=x[i];
        return new Counts(sum[0],sum[1],sum[2],sum[3],sum[4],sum[5]);
    }

    /**
     * Revisions compare on exactly the same fixed transition cohort, source
     * commit, corpus and installed artifact. A reviewed→unknown/unsupported
     * downgrade always fails. No aggregate gain cancels a subgroup loss.
     */
    public static Comparison compare(Phase7SelfPlay.Transitions transitions,
            Map<String,Phase7Benchmark.Scenario> firstPublic,
            Map<String,Phase7Benchmark.Scenario> secondPublic,
            Bundle reference,Attestation referenceSignature,
            Bundle candidate,Attestation candidateSignature,PinnedPolicy pinned){
        Report a=auditInternal(transitions,firstPublic,secondPublic,
                reference,referenceSignature,pinned,true);
        Report b=auditInternal(transitions,firstPublic,secondPublic,
                candidate,candidateSignature,pinned,false);
        Map<String,Entry> earlier=new HashMap<>(),later=new HashMap<>();
        for(Entry r:reference.entries())earlier.put(r.edgeSha256(),r);
        for(Entry r:candidate.entries())later.put(r.edgeSha256(),r);
        int regressions=0,improvements=0,unchanged=0;
        boolean changedMechanicsReceipts=false;
        for(var edge:transitions.edges()){
            Entry x=earlier.get(edgeSha256(edge)),y=later.get(edgeSha256(edge));
            if(x==null||y==null)continue;
            if(!x.mechanicsReceiptSha256().equals(y.mechanicsReceiptSha256()))
                changedMechanicsReceipts=true;
            if(x.verdict()==y.verdict())unchanged++;
            else if(x.verdict()==Verdict.REVIEWED_PUBLIC)regressions++;
            else if(y.verdict()==Verdict.REVIEWED_PUBLIC)improvements++;
            else regressions++;
        }
        Status status;
        if(!Objects.equals(reference.sourceCommitSha256(),candidate.sourceCommitSha256())
                ||!Objects.equals(reference.sourceCorpusSha256(),candidate.sourceCorpusSha256())
                ||!Objects.equals(reference.artifactSha256(),candidate.artifactSha256())
                ||changedMechanicsReceipts)
            status=Status.SOURCE_DRIFT;
        else if(!Objects.equals(reference.transitionsSha256(),candidate.transitionsSha256())
                ||!earlier.keySet().equals(later.keySet()))
            status=Status.TRANSITION_DRIFT;
        else if(!a.eligible())status=a.status();
        else if(b.status()==Status.INVALID_SIGNATURE || b.status()==Status.UNTRUSTED_REVIEWER
                ||b.status()==Status.POLICY_MISMATCH
                ||b.status()==Status.MISSING_EVIDENCE
                ||b.status()==Status.DUPLICATE_RECORD
                ||b.status()==Status.UNBOUND_SOURCE
                ||b.status()==Status.SYNTHETIC_NOT_VERIFIED)
            status=b.status();
        else if(regressions>0)status=Status.REGRESSION;
        else if(!b.eligible())status=b.status();
        else status=Status.ELIGIBLE_OFFLINE_ONLY;
        String fingerprint=sha256(SCHEMA+"|"+a.reportFingerprint()+"|"+
                b.reportFingerprint()+"|"+status+"|"+regressions+"|"+improvements);
        return new Comparison(SCHEMA,status,a.status(),b.status(),
                a.bundleSha256(),b.bundleSha256(),regressions,improvements,
                unchanged,fingerprint);
    }

    /**
     * Integration entrypoint: when mechanics audit fails, do NOT invoke
     * Phase 7.10 search at all. "Eligible" is observational/offline only:
     * no Minecraft proof, real competition result or Legacy control change.
     */
    public record GatedReplay(Report mechanics,Phase7SelfPlay.Report observed){
        public GatedReplay{
            Objects.requireNonNull(mechanics);
            if(!mechanics.eligible()&&observed!=null)
                throw new IllegalArgumentException("rejected mechanics cannot advance replay");
        }
    }
    public static GatedReplay replay(Phase7SelfPlay.Transitions transitions,
            Phase7SelfPlay.Participant first,Phase7SelfPlay.Participant second,
            Phase7SelfPlay.PinnedPolicy selfPlayPolicy,
            Phase7SelfPlay.Attestation publicTransitionApproval,
            String independentPublicTransitionReviewerSha,
            Bundle mechanicRecords,Attestation mechanicsApproval,PinnedPolicy mechanicsPolicy){
        Objects.requireNonNull(selfPlayPolicy);
        var before=audit(transitions,first.scenarios(),second.scenarios(),
                mechanicRecords,mechanicsApproval,mechanicsPolicy);
        if(!before.eligible())return new GatedReplay(before,null);
        // Phase 7.10 independently rechecks the transition reviewer, both
        // pinned participants, legal-action menus, turn continuity and budgets.
        return new GatedReplay(before,Phase7SelfPlay.replay(transitions,first,second,
                selfPlayPolicy,publicTransitionApproval,independentPublicTransitionReviewerSha));
    }
    private static boolean legal(BattleSnapshot position,String action){
        if(action.startsWith("move:"))
            return position.legalMoves().contains(action.substring(5));
        return action.startsWith("switch:")
                &&position.legalSwitches().contains(action.substring(7));
    }
    private static boolean validName(String s){return s!=null&&NAME.matcher(s).matches();}
    private static boolean validSha(String s){return s!=null&&SHA.matcher(s).matches();}
    private static String sha256(String s){return sha256(s.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] b){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
