package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.6. Offline, verification-only regression gates. Compares sanitized
 * reports produced by Phase 7.5; does not authenticate custody on its own.
 * A baseline requires an independently PINNED exact-file SHA-256. No private
 * event, capture, actor or raw Pokémon data enters the baseline or output.
 */
public final class Phase7RegressionGate {
    private Phase7RegressionGate() {}
    public static final String SCHEMA = "phase7.6.v1";
    public static final String BASELINE_SCHEMA = "phase7.6.baseline.v1";
    private static final int MAX_BASELINE_BYTES = 4096;
    private static final java.util.regex.Pattern ID =
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern HEX =
            java.util.regex.Pattern.compile("[0-9a-f]{64}");

    public record Limits(int minimumAccepted, int maximumCoverageLoss,
                         int maximumAgreementLoss, int maximumAbstentionIncrease,
                         int maximumIllegalIncrease, int maximumProvenanceRejectionIncrease) {
        public Limits {
            if(minimumAccepted<1 || minimumAccepted>100
                    || maximumCoverageLoss<0 || maximumCoverageLoss>100
                    || maximumAgreementLoss<0 || maximumAgreementLoss>100
                    || maximumAbstentionIncrease<0 || maximumAbstentionIncrease>100
                    || maximumIllegalIncrease<0 || maximumIllegalIncrease>100
                    || maximumProvenanceRejectionIncrease<0
                    || maximumProvenanceRejectionIncrease>100)
                throw new IllegalArgumentException("invalid gate thresholds");
        }
    }

    /** A public sanitized contract; not evidence of independent custody. */
    public record Baseline(String schema, int version, String revision, long seed,
                           String referenceReportSha256, Limits limits) {
        public Baseline {
            if(!BASELINE_SCHEMA.equals(schema) || version!=1 || !validId(revision)
                    || !validHash(referenceReportSha256) || limits==null)
                throw new IllegalArgumentException("invalid baseline");
        }
        public String json() {
            JsonObject b=new JsonObject();
            b.addProperty("schema",schema);
            b.addProperty("version",version);
            b.addProperty("revision",revision);
            b.addProperty("seed",seed);
            b.addProperty("referenceReportSha256",referenceReportSha256);
            JsonObject t=new JsonObject();
            t.addProperty("minimumAccepted",limits.minimumAccepted());
            t.addProperty("maximumCoverageLoss",limits.maximumCoverageLoss());
            t.addProperty("maximumAgreementLoss",limits.maximumAgreementLoss());
            t.addProperty("maximumAbstentionIncrease",limits.maximumAbstentionIncrease());
            t.addProperty("maximumIllegalIncrease",limits.maximumIllegalIncrease());
            t.addProperty("maximumProvenanceRejectionIncrease",
                    limits.maximumProvenanceRejectionIncrease());
            b.add("limits",t);
            return b.toString();
        }
    }

    /**
     * Only a baseline read from ignored local storage and matched against a
     * separately pinned SHA can enter a passing comparison. Do not pin hashes
     * computed from unreviewed bundles; the external custodian owns this pin.
     */
    public static final class PinnedBaseline {
        private final Baseline value;
        private final String fileSha256;
        private PinnedBaseline(Baseline baseline,String fileSha256) {
            this.value=baseline;this.fileSha256=fileSha256;
        }
        public Baseline value(){return value;}
        public String fileSha256(){return fileSha256;}
        @Override public String toString(){return "PinnedBenchmarkBaseline[metadata redacted]";}
    }

    public enum Status {
        PASS, BASELINE_MISMATCH, SCHEMA_MISMATCH, INVALID_REPORT, INSUFFICIENT_EVIDENCE,
        COHORT_DRIFT, EVIDENCE_REGRESSION, ACTION_REGRESSION
    }

    /** Signed deltas are current minus reference, never win-rate improvements. */
    public record Changes(int accepted, int submitted, int comparisons,
                          int shadowAgreements, int shadowDisagreements,
                          int shadowAbstentions, int illegalDecisions,
                          int provenanceRejections, int replayRejections,
                          int duplicates, int unknownEvidence,
                          int intakeRejections, int baselineAgreements) {}

    public record Result(String schema, Status status, String baselineRevision,
                         String baselineFileSha256, String referenceReportSha256,
                         String candidateReportSha256, Changes changes,
                         int referenceAgreementBasisPoints, int candidateAgreementBasisPoints,
                         int referenceProvenanceRejectionBasisPoints,
                         int candidateProvenanceRejectionBasisPoints) {
        public Result {
            if(!SCHEMA.equals(schema))throw new IllegalArgumentException("invalid gate schema");
        }
        /** All output is aggregated and privacy-minimized. -1 becomes JSON null. */
        public String json() {
            JsonObject obj=new JsonObject();
            obj.addProperty("schema",schema);
            obj.addProperty("status",status.name());
            obj.addProperty("baselineRevision",baselineRevision);
            obj.addProperty("baselineFileSha256",baselineFileSha256);
            obj.addProperty("referenceReportSha256",referenceReportSha256);
            obj.addProperty("candidateReportSha256",candidateReportSha256);
            JsonObject diff=new JsonObject();
            diff.addProperty("accepted",changes.accepted());
            diff.addProperty("submitted",changes.submitted());
            diff.addProperty("comparisons",changes.comparisons());
            diff.addProperty("shadowAgreements",changes.shadowAgreements());
            diff.addProperty("shadowDisagreements",changes.shadowDisagreements());
            diff.addProperty("shadowAbstentions",changes.shadowAbstentions());
            diff.addProperty("illegalDecisions",changes.illegalDecisions());
            diff.addProperty("provenanceRejections",changes.provenanceRejections());
            diff.addProperty("replayRejections",changes.replayRejections());
            diff.addProperty("duplicates",changes.duplicates());
            diff.addProperty("unknownEvidence",changes.unknownEvidence());
            diff.addProperty("intakeRejections",changes.intakeRejections());
            diff.addProperty("baselineAgreements",changes.baselineAgreements());
            obj.add("delta",diff);
            JsonObject rates=new JsonObject();
            rate(rates,"referenceAgreementBasisPoints",referenceAgreementBasisPoints);
            rate(rates,"candidateAgreementBasisPoints",candidateAgreementBasisPoints);
            rate(rates,"referenceProvenanceRejectionBasisPoints",
                    referenceProvenanceRejectionBasisPoints);
            rate(rates,"candidateProvenanceRejectionBasisPoints",
                    candidateProvenanceRejectionBasisPoints);
            obj.add("rates",rates);
            obj.addProperty("scope","fixed-cohort-action-agreement-only;not-minecraft;not-win-rate");
            return obj.toString();
        }
        private static void rate(JsonObject obj,String name,int value) {
            if(value<0)obj.add(name,JsonNull.INSTANCE);
            else obj.addProperty(name,value);
        }
    }

    public static Baseline define(String revision,Phase7TrustedIntake.Report reviewed,
                                  Limits limits) {
        Objects.requireNonNull(reviewed);
        return new Baseline(BASELINE_SCHEMA,1,revision,reviewed.seed(),
                sha256(reviewed.json()),limits);
    }

    /**
     * Strict exact-byte local baseline verifier; SHA must be obtained from an
     * independently trusted source, not copied from the file under evaluation.
     */
    public static PinnedBaseline load(Path repoRoot,String revision,String pinnedSha256)
            throws IOException {
        if(!validId(revision) || !validHash(pinnedSha256))
            throw new IllegalArgumentException("invalid externally pinned baseline");
        Path directory=safeBaselineDirectory(repoRoot);
        Path file=directory.resolve("phase7_"+revision+".json");
        if(Files.isSymbolicLink(file) || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                || Files.size(file)>MAX_BASELINE_BYTES)
            throw new IOException("baseline unavailable");
        byte[] data;
        try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)) {
            data=in.readNBytes(MAX_BASELINE_BYTES+1);
        }
        if(data.length>MAX_BASELINE_BYTES)throw new IOException("oversized baseline");
        String hash=sha256(data);
        if(!hash.equals(pinnedSha256))throw new IllegalArgumentException("untrusted baseline bytes");
        Baseline baseline=parse(new String(data,StandardCharsets.UTF_8));
        if(!baseline.revision().equals(revision))throw new IllegalArgumentException("baseline revision drift");
        return new PinnedBaseline(baseline,hash);
    }

    /** Local-only, atomic sanitized baseline creation, not a trust approval. */
    public static Path writeLocal(Path repoRoot,Baseline baseline) throws IOException {
        Objects.requireNonNull(baseline);
        Path directory=safeBaselineDirectory(repoRoot);
        Path target=directory.resolve("phase7_"+baseline.revision()+".json");
        if(Files.isSymbolicLink(target))throw new IOException("unsafe baseline path");
        Path temporary=Files.createTempFile(directory,".phase7-", ".tmp");
        try {
            Files.writeString(temporary,baseline.json(),StandardCharsets.UTF_8);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return target;
    }

    private static Path safeBaselineDirectory(Path repo) throws IOException {
        Path root=repo.toAbsolutePath().normalize();
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(root))
            throw new IOException("unsafe repository root");
        Path run=root.resolve("run");
        Path corpus=run.resolve("phase7-corpus");
        Path baselines=corpus.resolve("baselines");
        for(Path item:List.of(run,corpus,baselines)) {
            if(Files.isSymbolicLink(item))throw new IOException("unsafe baseline directory");
            if(!Files.exists(item,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(item);
            if(!Files.isDirectory(item,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe baseline directory");
        }
        return baselines;
    }

    private static Baseline parse(String input) {
        JsonObject root=StrictOwnJson.read(input);
        requireKeys(root,"schema","version","revision","seed","referenceReportSha256","limits");
        JsonObject limits=root.getAsJsonObject("limits");
        requireKeys(limits,"minimumAccepted","maximumCoverageLoss","maximumAgreementLoss",
                "maximumAbstentionIncrease","maximumIllegalIncrease",
                "maximumProvenanceRejectionIncrease");
        return new Baseline(string(root,"schema"),intValue(root,"version"),
                string(root,"revision"),longValue(root,"seed"),
                string(root,"referenceReportSha256"),
                new Limits(intValue(limits,"minimumAccepted"),intValue(limits,"maximumCoverageLoss"),
                        intValue(limits,"maximumAgreementLoss"),
                        intValue(limits,"maximumAbstentionIncrease"),
                        intValue(limits,"maximumIllegalIncrease"),
                        intValue(limits,"maximumProvenanceRejectionIncrease")));
    }
    private static void requireKeys(JsonObject x,String... fields) {
        if(!x.keySet().equals(Set.of(fields)))throw new IllegalArgumentException("baseline unknown/missing fields");
    }
    private static String string(JsonObject o,String key) {
        JsonElement x=o.get(key);
        if(x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid string");
        return x.getAsString();
    }
    private static long longValue(JsonObject o,String key) {
        String v=o.get(key).getAsString();
        if(!v.matches("-?(0|[1-9][0-9]{0,18})"))throw new IllegalArgumentException("invalid integer");
        return Long.parseLong(v);
    }
    private static int intValue(JsonObject o,String key) {
        long v=longValue(o,key);
        if(v<Integer.MIN_VALUE||v>Integer.MAX_VALUE)throw new IllegalArgumentException("invalid integer");
        return (int)v;
    }

    /**
     * Compare two independently reviewed 7.5 reports. The same accepted public
     * state AND real Legacy action are required, so dropping hard cases cannot
     * manufacture an "improvement". Changes are counts and basis points only;
     * no damage, victory or win-rate claim.
     */
    public static Result compare(PinnedBaseline pinned,Phase7TrustedIntake.Report reference,
                                 Phase7TrustedIntake.Report candidate) {
        Objects.requireNonNull(pinned);
        Objects.requireNonNull(reference);
        Objects.requireNonNull(candidate);
        Baseline baseline=pinned.value();
        Changes delta=difference(reference,candidate);
        var a=reference.corpus().coverage();
        var b=candidate.corpus().coverage();
        int aAgreement=rate(a.shadowAgreements(),a.accepted());
        int bAgreement=rate(b.shadowAgreements(),b.accepted());
        int deniedReference=reference.intakeStatuses().size()-(int)reference.intakeStatuses()
                .stream().filter(x->x==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS).count();
        int deniedCandidate=candidate.intakeStatuses().size()-(int)candidate.intakeStatuses()
                .stream().filter(x->x==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS).count();
        int aRejections=rate(a.provenanceRejected()+deniedReference,a.submitted()+deniedReference);
        int bRejections=rate(b.provenanceRejected()+deniedCandidate,b.submitted()+deniedCandidate);
        Status status;
        if(reference.seed()!=baseline.seed() || candidate.seed()!=baseline.seed()
                || !sha256(reference.json()).equals(baseline.referenceReportSha256())) {
            status=Status.BASELINE_MISMATCH;
        } else if(!Phase7TrustedIntake.SCHEMA.equals(reference.schema())
                || !Phase7TrustedIntake.SCHEMA.equals(candidate.schema())
                || !Phase7Corpus.SCHEMA.equals(reference.corpus().schema())
                || !Phase7Corpus.SCHEMA.equals(candidate.corpus().schema())) {
            status=Status.SCHEMA_MISMATCH;
        } else if(!consistent(reference) || !consistent(candidate)) {
            status=Status.INVALID_REPORT;
        } else if(a.accepted()<baseline.limits().minimumAccepted()
                || b.accepted()<baseline.limits().minimumAccepted()) {
            status=Status.INSUFFICIENT_EVIDENCE;
        } else if(!sameCohort(reference,candidate)) {
            status=Status.COHORT_DRIFT;
        } else if(delta.accepted() < -baseline.limits().maximumCoverageLoss()
                || delta.provenanceRejections()>baseline.limits().maximumProvenanceRejectionIncrease()
                || delta.intakeRejections()>baseline.limits().maximumProvenanceRejectionIncrease()
                || delta.unknownEvidence()>0 || delta.replayRejections()>0
                || delta.duplicates()>0) {
            status=Status.EVIDENCE_REGRESSION;
        } else if(delta.shadowAgreements() < -baseline.limits().maximumAgreementLoss()
                || delta.shadowAbstentions()>baseline.limits().maximumAbstentionIncrease()
                || delta.illegalDecisions()>baseline.limits().maximumIllegalIncrease()) {
            status=Status.ACTION_REGRESSION;
        } else status=Status.PASS;
        return new Result(SCHEMA,status,baseline.revision(),pinned.fileSha256(),
                sha256(reference.json()),sha256(candidate.json()),delta,aAgreement,bAgreement,
                aRejections,bRejections);
    }

    /** Refuse fabricated summary fields and internally inconsistent reports. */
    private static boolean consistent(Phase7TrustedIntake.Report r) {
        if(r.seed()!=r.corpus().seed() || r.corpus().coverage().submitted()!=r.corpus().rows().size())
            return false;
        int verified=(int)r.intakeStatuses().stream()
                .filter(x->x==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS).count();
        if(verified!=r.corpus().rows().size())return false;
        int accepted=0,duplicates=0,replay=0,provenance=0,comparisons=0,baselineYes=0,
                shadowYes=0,shadowNo=0,abstentions=0,illegal=0,unknown=0;
        for(Phase7Corpus.Row row:r.corpus().rows()) {
            if(row.scenario()==null || row.status()==null
                    || !validHash(row.stateFingerprint()))return false;
            if(row.status()==Phase7Corpus.Status.ACCEPTED) {
                accepted++;
                if(!validHash(row.publicFingerprint()) || !validAction(row.legacyAction())
                        || !validAction(row.baselineAction())
                        || row.baselineAgreement()==null
                        || row.baselineAgreement()!=row.legacyAction().equals(row.baselineAction()))
                    return false;
                comparisons++;
                if(row.baselineAgreement())baselineYes++;
                if(row.shadowAction()==null) {
                    if(!row.shadowAbstained() || row.shadowAgreement()!=null)return false;
                } else {
                    if(!validAction(row.shadowAction()) || row.shadowAbstained()
                            || row.shadowAgreement()==null
                            || row.shadowAgreement()!=row.legacyAction().equals(row.shadowAction()))
                        return false;
                }
                if(Boolean.TRUE.equals(row.shadowAgreement()))shadowYes++;
                if(Boolean.FALSE.equals(row.shadowAgreement()))shadowNo++;
            } else {
                if(row.publicFingerprint()!=null || row.legacyAction()!=null
                        || row.baselineAction()!=null || row.shadowAction()!=null
                        || row.baselineAgreement()!=null || row.shadowAgreement()!=null
                        || row.shadowIllegal() || !row.shadowAbstained())return false;
                if(row.status().name().startsWith("DUPLICATE_"))duplicates++;
                else if(row.status().name().startsWith("REPLAY_"))replay++;
                else provenance++;
                if(row.status()==Phase7Corpus.Status.UNKNOWN_PUBLIC_EVIDENCE
                        || row.status()==Phase7Corpus.Status.STALE_TURN
                        || row.status()==Phase7Corpus.Status.AMBIGUOUS_IDENTITY)unknown++;
            }
            if(row.shadowAbstained())abstentions++;
            if(row.shadowIllegal())illegal++;
        }
        var c=r.corpus().coverage();
        return c.accepted()==accepted && c.rejected()==c.submitted()-accepted
                && c.duplicates()==duplicates && c.replayRejected()==replay
                && c.provenanceRejected()==provenance && c.comparisons()==comparisons
                && c.baselineAgreements()==baselineYes && c.shadowAgreements()==shadowYes
                && c.shadowDisagreements()==shadowNo
                && c.shadowAbstentions()==abstentions && c.shadowIllegal()==illegal
                && c.unknownEvidence()==unknown;
    }
    private static boolean validAction(String x) {
        return x!=null && x.matches("(?:move|switch):[a-z0-9][a-z0-9_-]{0,63}");
    }

    private static boolean sameCohort(Phase7TrustedIntake.Report a,
                                      Phase7TrustedIntake.Report b) {
        // No cohort/quality "pass" when intake eligibility changes.
        if(!a.intakeStatuses().equals(b.intakeStatuses()))return false;
        List<Phase7Corpus.Row> ar=a.corpus().rows(),br=b.corpus().rows();
        if(ar.size()!=br.size())return false;
        if(a.corpus().coverage().submitted()!=b.corpus().coverage().submitted())return false;
        List<String> aStates=new ArrayList<>(),bStates=new ArrayList<>();
        for(Phase7Corpus.Row r:ar)aStates.add(identity(r));
        for(Phase7Corpus.Row r:br)bStates.add(identity(r));
        Collections.sort(aStates);Collections.sort(bStates);
        if(!aStates.equals(bStates))return false;
        // Ambiguous accepted state identity is not comparable without a private
        // index/capture key; never tie-break using array order.
        Set<String> unique=new HashSet<>();
        for(Phase7Corpus.Row row:ar) {
            if(row.status()==Phase7Corpus.Status.ACCEPTED && !unique.add(identity(row)))
                return false;
        }
        return true;
    }
    private static String identity(Phase7Corpus.Row r) {
        // No raw details; include exact Legacy/baseline decisions and eligibility.
        return r.status()+"|"+r.scenario()+"|"+r.stateFingerprint()+"|"+
                r.publicFingerprint()+"|"+r.legacyAction()+"|"+r.baselineAction()+
                "|"+r.baselineAgreement();
    }
    private static Changes difference(Phase7TrustedIntake.Report a,Phase7TrustedIntake.Report b) {
        var x=a.corpus().coverage();var y=b.corpus().coverage();
        int ai=a.intakeStatuses().size()-
                (int)a.intakeStatuses().stream()
                        .filter(s->s==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS).count();
        int bi=b.intakeStatuses().size()-
                (int)b.intakeStatuses().stream()
                        .filter(s->s==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS).count();
        return new Changes(y.accepted()-x.accepted(),y.submitted()-x.submitted(),
                y.comparisons()-x.comparisons(),y.shadowAgreements()-x.shadowAgreements(),
                y.shadowDisagreements()-x.shadowDisagreements(),
                y.shadowAbstentions()-x.shadowAbstentions(),y.shadowIllegal()-x.shadowIllegal(),
                y.provenanceRejected()-x.provenanceRejected(),
                y.replayRejected()-x.replayRejected(),y.duplicates()-x.duplicates(),
                y.unknownEvidence()-x.unknownEvidence(),bi-ai,
                y.baselineAgreements()-x.baselineAgreements());
    }
    private static int rate(int numerator,int denominator) {
        return denominator<=0?-1:(int)((10_000L*numerator)/denominator);
    }
    private static boolean validId(String x){return x!=null&&ID.matcher(x).matches();}
    private static boolean validHash(String x){return x!=null&&HEX.matcher(x).matches();}
    private static String sha256(String s){return sha256(s.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] b) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
