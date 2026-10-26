package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.7: privacy-safe, fixed-strata coverage and robustness auditing.
 * Offline verification only: trust remains with independently pinned 7.5/7.6
 * evidence. Results measure agreement and evidence quality, NEVER win rates.
 */
public final class Phase7StratifiedAudit {
    private Phase7StratifiedAudit() {}
    public static final String SCHEMA="phase7.7.v1";
    public static final String POLICY_SCHEMA="phase7.7.policy.v1";
    private static final int MAX_POLICY_BYTES=8192;
    private static final java.util.regex.Pattern ID=java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern HASH=java.util.regex.Pattern.compile("[a-f0-9]{64}");

    /** Fixed categories supported by Phase 7.4's public action and evidence fields. */
    public enum Group {
        ACTION_MOVE, ACTION_SWITCH, QUALITY_ACCEPTED, QUALITY_UNKNOWN,
        QUALITY_DUPLICATE, QUALITY_REPLAY, QUALITY_PROVENANCE, QUALITY_INTAKE_REJECTED
    }
    /** All tolerances are counts, not probabilities or battle quality scores. */
    public record Rule(boolean required,int minimumReference,int minimumCandidate,
                       int maximumCountLoss,int maximumCountIncrease,
                       int maximumAgreementLoss,int maximumAbstentionIncrease,
                       int maximumIllegalIncrease) {
        public Rule {
            if(minimumReference<0||minimumCandidate<0||maximumCountLoss<0
                    ||maximumCountIncrease<0||maximumAgreementLoss<0
                    ||maximumAbstentionIncrease<0||maximumIllegalIncrease<0
                    ||minimumReference>100||minimumCandidate>100||maximumCountLoss>100
                    ||maximumCountIncrease>100||maximumAgreementLoss>100
                    ||maximumAbstentionIncrease>100||maximumIllegalIncrease>100
                    ||required&&(minimumReference<1||minimumCandidate<1))
                throw new IllegalArgumentException("invalid stratification rule");
        }
    }
    /**
     * A policy is independent from the baseline and bound to its exact
     * reference report. Mandatory stratum keys prevent silently ignoring
     * switch actions or a provenance category.
     */
    public record Policy(String schema,int version,String revision,
                         String referenceReportSha256,int minimumDisclosureCount,
                         int maximumDominantActionBasisPoints,
                         int maximumEvaluatedMixDriftBasisPoints,
                         Map<Group,Rule> rules) {
        public Policy {
            if(!POLICY_SCHEMA.equals(schema)||version!=1||!validId(revision)
                    ||!validHash(referenceReportSha256)||minimumDisclosureCount<3
                    ||minimumDisclosureCount>100
                    ||maximumDominantActionBasisPoints<5000
                    ||maximumDominantActionBasisPoints>10000
                    ||maximumEvaluatedMixDriftBasisPoints<0
                    ||maximumEvaluatedMixDriftBasisPoints>10000
                    ||rules==null||rules.size()!=Group.values().length)
                throw new IllegalArgumentException("invalid stratification policy");
            EnumMap<Group,Rule> copy=new EnumMap<>(Group.class);
            copy.putAll(rules);
            for(Group g:Group.values())if(copy.get(g)==null)
                throw new IllegalArgumentException("missing stratification rule");
            rules=Collections.unmodifiableMap(copy);
        }
        public String json() {
            JsonObject o=new JsonObject();
            o.addProperty("schema",schema);
            o.addProperty("version",version);
            o.addProperty("revision",revision);
            o.addProperty("referenceReportSha256",referenceReportSha256);
            o.addProperty("minimumDisclosureCount",minimumDisclosureCount);
            o.addProperty("maximumDominantActionBasisPoints",maximumDominantActionBasisPoints);
            o.addProperty("maximumEvaluatedMixDriftBasisPoints",
                    maximumEvaluatedMixDriftBasisPoints);
            JsonObject all=new JsonObject();
            for(Group g:Group.values()) {
                Rule r=rules.get(g);
                JsonObject j=new JsonObject();
                j.addProperty("required",r.required());
                j.addProperty("minimumReference",r.minimumReference());
                j.addProperty("minimumCandidate",r.minimumCandidate());
                j.addProperty("maximumCountLoss",r.maximumCountLoss());
                j.addProperty("maximumCountIncrease",r.maximumCountIncrease());
                j.addProperty("maximumAgreementLoss",r.maximumAgreementLoss());
                j.addProperty("maximumAbstentionIncrease",r.maximumAbstentionIncrease());
                j.addProperty("maximumIllegalIncrease",r.maximumIllegalIncrease());
                all.add(g.name(),j);
            }
            o.add("rules",all);
            return o.toString();
        }
    }

    public static final class PinnedPolicy {
        private final Policy policy;
        private final String fileSha256;
        private PinnedPolicy(Policy policy,String hash) {
            this.policy=policy;this.fileSha256=hash;
        }
        public Policy value(){return policy;}
        public String fileSha256(){return fileSha256;}
        @Override public String toString(){return "PinnedStratificationPolicy[redacted]";}
    }

    public enum Status {
        PASS, GLOBAL_GATE_BLOCKED, POLICY_BINDING_INVALID, INSUFFICIENT_GROUP_COVERAGE,
        GROUP_COHORT_DRIFT, GROUP_ACTION_REGRESSION, GROUP_EVIDENCE_REGRESSION,
        SAMPLING_BIAS
    }
    public enum GroupStatus {
        PASS, NOT_REPRESENTED, INSUFFICIENT_COVERAGE, DRIFT,
        ACTION_REGRESSION, EVIDENCE_REGRESSION
    }

    /** Counts and rates below disclosure threshold are JSON null, not zeros. */
    public record GroupResult(Group group,GroupStatus status,
                              int referenceCount,int candidateCount,
                              int referenceAgreements,int candidateAgreements,
                              int referenceEvaluated,int candidateEvaluated,
                              int referenceAbstentions,int candidateAbstentions,
                              int referenceIllegal,int candidateIllegal) {
        public GroupResult {
            Objects.requireNonNull(group);Objects.requireNonNull(status);
        }
    }

    public record Report(String schema, Status status,String globalGateStatus,
                         String policyRevision,String policySha256,
                         String referenceReportSha256,String candidateReportSha256,
                         int disclosureThreshold, int referenceDominanceBasisPoints,
                         int candidateDominanceBasisPoints,int evaluatedMixDriftBasisPoints,
                         List<GroupResult> groups) {
        public Report {
            if(!SCHEMA.equals(schema)||status==null||groups==null
                    ||groups.size()!=Group.values().length)
                throw new IllegalArgumentException("invalid stratified report");
            groups=List.copyOf(groups);
        }
        public boolean passed(){return status==Status.PASS;}
        public String json() {
            JsonObject root=new JsonObject();
            root.addProperty("schema",schema);
            root.addProperty("status",status.name());
            root.addProperty("globalGateStatus",globalGateStatus);
            root.addProperty("policyRevision",policyRevision);
            root.addProperty("policySha256",policySha256);
            root.addProperty("referenceReportSha256",referenceReportSha256);
            root.addProperty("candidateReportSha256",candidateReportSha256);
            root.addProperty("scope","offline-fixed-strata;action-agreement-only;not-minecraft;not-win-rate");
            JsonObject bias=new JsonObject();
            // Dominance fractions can indirectly reveal a small action group.
            // Apply cell suppression BEFORE exporting these derived shares.
            GroupResult moves=groups.stream().filter(g->g.group()==Group.ACTION_MOVE)
                    .findFirst().orElseThrow();
            GroupResult switches=groups.stream().filter(g->g.group()==Group.ACTION_SWITCH)
                    .findFirst().orElseThrow();
            boolean samplingDisclosable=moves.referenceCount()>=disclosureThreshold
                    && moves.candidateCount()>=disclosureThreshold
                    && switches.referenceCount()>=disclosureThreshold
                    && switches.candidateCount()>=disclosureThreshold;
            integerOrNull(bias,"referenceDominantActionBasisPoints",
                    samplingDisclosable?referenceDominanceBasisPoints:-1);
            integerOrNull(bias,"candidateDominantActionBasisPoints",
                    samplingDisclosable?candidateDominanceBasisPoints:-1);
            integerOrNull(bias,"evaluatedActionMixDriftBasisPoints",
                    samplingDisclosable?evaluatedMixDriftBasisPoints:-1);
            root.add("sampling",bias);
            JsonArray array=new JsonArray();
            for(GroupResult g:groups) {
                JsonObject o=new JsonObject();
                o.addProperty("group",g.group().name());
                o.addProperty("status",g.status().name());
                boolean showRef=g.referenceCount()>=disclosureThreshold;
                boolean showCandidate=g.candidateCount()>=disclosureThreshold;
                integerOrNull(o,"referenceCount",showRef?g.referenceCount():-1);
                integerOrNull(o,"candidateCount",showCandidate?g.candidateCount():-1);
                integerOrNull(o,"countDelta",showRef&&showCandidate?
                        g.candidateCount()-g.referenceCount():-1);
                integerOrNull(o,"referenceAgreementBasisPoints",
                        showRef&&g.referenceEvaluated()>0?
                            rate(g.referenceAgreements(),g.referenceCount()):-1);
                integerOrNull(o,"candidateAgreementBasisPoints",
                        showCandidate&&g.candidateEvaluated()>0?
                            rate(g.candidateAgreements(),g.candidateCount()):-1);
                integerOrNull(o,"referenceAbstentions",showRef?g.referenceAbstentions():-1);
                integerOrNull(o,"candidateAbstentions",showCandidate?g.candidateAbstentions():-1);
                integerOrNull(o,"referenceIllegal",showRef?g.referenceIllegal():-1);
                integerOrNull(o,"candidateIllegal",showCandidate?g.candidateIllegal():-1);
                o.addProperty("suppressed",!showRef||!showCandidate);
                array.add(o);
            }
            root.add("strata",array);
            return root.toString();
        }
        private static void integerOrNull(JsonObject o,String key,int value) {
            if(value<0)o.add(key,JsonNull.INSTANCE);
            else o.addProperty(key,value);
        }
    }

    public static Policy define(String revision,Phase7TrustedIntake.Report reviewed,
                                int disclosure,int maxDominance,int maxMixDrift,
                                Map<Group,Rule> rules) {
        Objects.requireNonNull(reviewed);
        return new Policy(POLICY_SCHEMA,1,revision,sha256(reviewed.json()),
                disclosure,maxDominance,maxMixDrift,rules);
    }

    /** Local, Git-ignored only. Creating a file does not independently approve it. */
    public static Path writeLocal(Path root,Policy policy) throws IOException {
        Objects.requireNonNull(policy);
        Path directory=directory(root);
        Path file=directory.resolve("phase7_"+policy.revision()+".json");
        // A revision is immutable after publication. Rotate to a new revision
        // rather than silently overwriting an independently pinned policy.
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("policy revision already exists");
        Path temporary=Files.createTempFile(directory,".phase7-", ".tmp");
        try {
            Files.writeString(temporary,policy.json(),StandardCharsets.UTF_8);
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE);
        } finally {Files.deleteIfExists(temporary);}
        return file;
    }

    /**
     * exact bytes must match the SHA pinned via a separate, trusted process.
     * Caller cannot pass an arbitrary untrusted policy and get a PASS.
     */
    public static PinnedPolicy load(Path root,String revision,String expectedSha)
            throws IOException {
        if(!validId(revision)||!validHash(expectedSha))
            throw new IllegalArgumentException("invalid independently pinned policy");
        Path file=directory(root).resolve("phase7_"+revision+".json");
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                ||Files.size(file)>MAX_POLICY_BYTES)
            throw new IOException("policy unavailable");
        byte[] data;
        try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)) {
            data=in.readNBytes(MAX_POLICY_BYTES+1);
        }
        if(data.length>MAX_POLICY_BYTES)throw new IOException("oversized policy");
        String hash=sha256(data);
        if(!hash.equals(expectedSha))throw new IllegalArgumentException("untrusted policy file");
        Policy policy=parse(new String(data,StandardCharsets.UTF_8));
        if(!policy.revision().equals(revision))
            throw new IllegalArgumentException("policy revision mismatch");
        return new PinnedPolicy(policy,hash);
    }

    private static Path directory(Path base) throws IOException {
        Path root=base.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(root)||!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("unsafe repository root");
        Path parent=root;
        for(String name:List.of("run","phase7-corpus","strata")) {
            parent=parent.resolve(name);
            if(Files.isSymbolicLink(parent))throw new IOException("unsafe policy directory");
            if(!Files.exists(parent,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(parent);
            if(!Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe policy directory");
        }
        return parent;
    }

    private static Policy parse(String text) {
        JsonObject obj=StrictOwnJson.read(text);
        keys(obj,"schema","version","revision","referenceReportSha256",
                "minimumDisclosureCount","maximumDominantActionBasisPoints",
                "maximumEvaluatedMixDriftBasisPoints","rules");
        JsonObject rs=obj.getAsJsonObject("rules");
        if(rs.size()!=Group.values().length)throw new IllegalArgumentException("missing strata");
        EnumMap<Group,Rule> rules=new EnumMap<>(Group.class);
        for(Group g:Group.values()) {
            JsonElement el=rs.get(g.name());
            if(el==null||!el.isJsonObject())throw new IllegalArgumentException("unknown strata");
            JsonObject r=el.getAsJsonObject();
            keys(r,"required","minimumReference","minimumCandidate","maximumCountLoss",
                    "maximumCountIncrease","maximumAgreementLoss",
                    "maximumAbstentionIncrease","maximumIllegalIncrease");
            JsonElement required=r.get("required");
            if(!required.isJsonPrimitive()||!required.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("invalid required flag");
            rules.put(g,new Rule(required.getAsBoolean(),integer(r,"minimumReference"),
                    integer(r,"minimumCandidate"),integer(r,"maximumCountLoss"),
                    integer(r,"maximumCountIncrease"),integer(r,"maximumAgreementLoss"),
                    integer(r,"maximumAbstentionIncrease"),integer(r,"maximumIllegalIncrease")));
        }
        for(String key:rs.keySet())if(!rules.containsKey(Group.valueOf(key)))
            throw new IllegalArgumentException("unknown strata");
        return new Policy(string(obj,"schema"),integer(obj,"version"),
                string(obj,"revision"),string(obj,"referenceReportSha256"),
                integer(obj,"minimumDisclosureCount"),
                integer(obj,"maximumDominantActionBasisPoints"),
                integer(obj,"maximumEvaluatedMixDriftBasisPoints"),rules);
    }
    private static void keys(JsonObject o,String... expected) {
        if(!o.keySet().equals(Set.of(expected)))
            throw new IllegalArgumentException("unrecognized/missing field");
    }
    private static String string(JsonObject o,String key) {
        JsonElement e=o.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid string");
        return e.getAsString();
    }
    private static int integer(JsonObject o,String key) {
        JsonElement e=o.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("invalid number");
        String raw=e.getAsString();
        if(!raw.matches("0|[1-9][0-9]{0,5}"))
            throw new IllegalArgumentException("invalid integer");
        return Integer.parseInt(raw);
    }

    /**
     * The Phase 7.6 global gate must pass first. The same exact approved
     * evidence cohort is then stratified. A synthetic PASS is not win-rate
     * evidence. Rejected observations NEVER become action metrics.
     */
    public static Report compare(Phase7RegressionGate.PinnedBaseline baseline,
                                 PinnedPolicy policy,
                                 Phase7TrustedIntake.Report reference,
                                 Phase7TrustedIntake.Report candidate) {
        Objects.requireNonNull(baseline);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(reference);
        Objects.requireNonNull(candidate);
        var global=Phase7RegressionGate.compare(baseline,reference,candidate);
        Policy rules=policy.value();
        boolean policyMatches=rules.referenceReportSha256().equals(
                baseline.value().referenceReportSha256())
                && rules.referenceReportSha256().equals(sha256(reference.json()));
        EnumMap<Group,Counts> before=count(reference),after=count(candidate);
        List<GroupResult> groups=new ArrayList<>();
        Status status=!policyMatches?Status.POLICY_BINDING_INVALID:
                global.status()!=Phase7RegressionGate.Status.PASS?Status.GLOBAL_GATE_BLOCKED:Status.PASS;
        for(Group g:Group.values()) {
            Counts a=before.get(g),b=after.get(g);
            Rule rule=rules.rules().get(g);
            GroupStatus result=assess(g,rule,a,b);
            groups.add(new GroupResult(g,result,a.support,b.support,a.agreement,b.agreement,
                    a.evaluated,b.evaluated,a.abstain,b.abstain,a.illegal,b.illegal));
            if(policyMatches&&global.status()==Phase7RegressionGate.Status.PASS) {
                if(result==GroupStatus.INSUFFICIENT_COVERAGE)
                    status=Status.INSUFFICIENT_GROUP_COVERAGE;
                else if(result==GroupStatus.DRIFT&&status!=Status.INSUFFICIENT_GROUP_COVERAGE)
                    status=Status.GROUP_COHORT_DRIFT;
                else if(result==GroupStatus.ACTION_REGRESSION && status==Status.PASS)
                    status=Status.GROUP_ACTION_REGRESSION;
                else if(result==GroupStatus.EVIDENCE_REGRESSION && status==Status.PASS)
                    status=Status.GROUP_EVIDENCE_REGRESSION;
            }
        }
        int referenceDominance=dominant(before);
        int candidateDominance=dominant(after);
        int mixDrift=mixDrift(before,after);
        if(status==Status.PASS&&(referenceDominance<0||candidateDominance<0
                ||referenceDominance>rules.maximumDominantActionBasisPoints()
                ||candidateDominance>rules.maximumDominantActionBasisPoints()
                ||mixDrift<0||mixDrift>rules.maximumEvaluatedMixDriftBasisPoints()))
            status=Status.SAMPLING_BIAS;
        return new Report(SCHEMA,status,global.status().name(),rules.revision(),
                policy.fileSha256(),sha256(reference.json()),sha256(candidate.json()),
                rules.minimumDisclosureCount(),referenceDominance,
                candidateDominance,mixDrift,groups);
    }

    private static GroupStatus assess(Group group,Rule rule,Counts a,Counts b) {
        if(a.support<rule.minimumReference()||b.support<rule.minimumCandidate()
                ||rule.required()&&(a.support==0||b.support==0))
            return GroupStatus.INSUFFICIENT_COVERAGE;
        if(a.support==0&&b.support==0)return GroupStatus.NOT_REPRESENTED;
        if(a.support-b.support>rule.maximumCountLoss())
            return GroupStatus.DRIFT;
        if(b.support-a.support>rule.maximumCountIncrease())
            return GroupStatus.EVIDENCE_REGRESSION;
        if(group==Group.ACTION_MOVE || group==Group.ACTION_SWITCH
                ||group==Group.QUALITY_ACCEPTED) {
            if(a.agreement-b.agreement>rule.maximumAgreementLoss()
                    || b.abstain-a.abstain>rule.maximumAbstentionIncrease()
                    || b.illegal-a.illegal>rule.maximumIllegalIncrease())
                return GroupStatus.ACTION_REGRESSION;
        }
        return GroupStatus.PASS;
    }

    private static final class Counts {
        int support,agreement,evaluated,abstain,illegal;
        void add(Phase7Corpus.Row r) {
            support++;
            if(Boolean.TRUE.equals(r.shadowAgreement()))agreement++;
            if(r.shadowAgreement()!=null)evaluated++;
            if(r.shadowAbstained())abstain++;
            if(r.shadowIllegal())illegal++;
        }
        void increment(){support++;}
    }

    private static EnumMap<Group,Counts> count(Phase7TrustedIntake.Report report) {
        EnumMap<Group,Counts> result=new EnumMap<>(Group.class);
        for(Group g:Group.values())result.put(g,new Counts());
        for(Phase7Corpus.Row row:report.corpus().rows()) {
            Group quality=switch(row.status()) {
                case ACCEPTED -> Group.QUALITY_ACCEPTED;
                case DUPLICATE_CAPTURE,DUPLICATE_BATTLE_TURN,DUPLICATE_EVIDENCE -> Group.QUALITY_DUPLICATE;
                case REPLAY_REWRITE,REPLAY_INCOMPLETE,REPLAY_MISMATCH -> Group.QUALITY_REPLAY;
                case UNKNOWN_PUBLIC_EVIDENCE,STALE_TURN,AMBIGUOUS_IDENTITY -> Group.QUALITY_UNKNOWN;
                default -> Group.QUALITY_PROVENANCE;
            };
            result.get(quality).add(row);
            if(row.status()==Phase7Corpus.Status.ACCEPTED) {
                Group action=row.legacyAction().startsWith("move:")?
                        Group.ACTION_MOVE:Group.ACTION_SWITCH;
                result.get(action).add(row);
            }
        }
        for(var status:report.intakeStatuses())
            if(status!=Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS)
                result.get(Group.QUALITY_INTAKE_REJECTED).increment();
        return result;
    }

    private static int dominant(EnumMap<Group,Counts> map) {
        int moves=map.get(Group.ACTION_MOVE).support;
        int switches=map.get(Group.ACTION_SWITCH).support;
        return moves+switches==0?-1:rate(Math.max(moves,switches),moves+switches);
    }
    /** Compare action-category share of evaluated (non-abstained) decisions. */
    private static int mixDrift(EnumMap<Group,Counts> a,EnumMap<Group,Counts> b) {
        int am=a.get(Group.ACTION_MOVE).evaluated,as=a.get(Group.ACTION_SWITCH).evaluated,
                bm=b.get(Group.ACTION_MOVE).evaluated,bs=b.get(Group.ACTION_SWITCH).evaluated;
        if(am+as==0||bm+bs==0)return -1;
        return Math.abs(rate(am,am+as)-rate(bm,bm+bs));
    }
    private static int rate(int numerator,int denominator) {
        return denominator==0?-1:(int)(10_000L*numerator/denominator);
    }
    private static boolean validId(String s){return s!=null&&ID.matcher(s).matches();}
    private static boolean validHash(String s){return s!=null&&HASH.matcher(s).matches();}
    private static String sha256(String s){return sha256(s.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
