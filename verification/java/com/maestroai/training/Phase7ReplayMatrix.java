package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.9 — deterministic, verification-only cross-version Shadow robustness.
 *
 * Re-runs exactly the same signed local Legacy bundles for each pinned version.
 * Phase 7.6 and 7.7 approvals remain mandatory. No live battle class invokes
 * this adapter; no raw logs, private Pokémon facts or capture IDs are exported.
 * Conditional graph uncertainties are NOT calibrated probabilities.
 */
public final class Phase7ReplayMatrix {
    private Phase7ReplayMatrix() {}
    public static final String SCHEMA = "phase7.9.v1";
    private static final int MAX_VERSIONS = 8;
    private static final int MAX_SAMPLES = 100;
    private static final java.util.regex.Pattern SHA =
            java.util.regex.Pattern.compile("[a-f0-9]{64}");

    public record Version(Phase7ShadowReplay.PinnedPolicy policy,
                          Map<String,Phase7ShadowReplay.Graph> graphs) {
        public Version {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(graphs);
            if(graphs.isEmpty() || graphs.size()>100)
                throw new IllegalArgumentException("invalid replay graph collection");
            graphs=Map.copyOf(graphs);
            if(!policy.value().modelSha256().equals(Phase7ShadowReplay.graphSha256(graphs)))
                throw new IllegalArgumentException("unverified policy/model fingerprint");
        }
    }

    public enum Status {
        PASS, INCOMPATIBLE_CONFIGURATION, INCOMPLETE_TRACE, EVIDENCE_DRIFT,
        GLOBAL_REGRESSION, STRATIFIED_REGRESSION, NEW_ILLEGAL_DECISION,
        SEARCH_EXHAUSTION_REGRESSION, ABSTENTION_REGRESSION,
        UNCERTAINTY_REGRESSION
    }

    /** Fixed, aggregate counters; never per-sample private decision details. */
    public record Deltas(int accepted, int changedActions, int newAbstentions,
                         int recoveredAbstentions, int newlyIllegal,
                         int newlyExhausted, int newlyUnsupported,
                         int uncertaintyIncreases,int penaltyIncreases,
                         int legacyAgreementLoss,int legacyAgreementGain) {
        public Deltas {
            if(changedActions<0||newAbstentions<0||recoveredAbstentions<0
                    ||newlyIllegal<0||newlyExhausted<0||newlyUnsupported<0
                    ||uncertaintyIncreases<0||penaltyIncreases<0
                    ||legacyAgreementLoss<0||legacyAgreementGain<0)
                throw new IllegalArgumentException("invalid differential counters");
        }
    }

    public record VersionResult(String revision,String policySha256,String graphSha256,
                                String custodyReportSha256,String replaySha256,
                                Status status,String aggregateGate,String strataGate,
                                Deltas changes) {
        public VersionResult {
            Objects.requireNonNull(status);Objects.requireNonNull(changes);
        }
    }

    public record Report(String schema,String referenceRevision,
                         String referencePolicySha256,String scenarioSha256,
                         String pinnedAggregateSha256,String pinnedStrataSha256,
                         String reportFingerprint,List<VersionResult> versions) {
        public Report {
            if(!SCHEMA.equals(schema)||!validSha(reportFingerprint))
                throw new IllegalArgumentException("invalid replay matrix");
            versions=List.copyOf(versions);
        }
        public boolean passed(){
            return !versions.isEmpty() && versions.stream()
                    .allMatch(v->v.status()==Status.PASS);
        }
        public String json(){
            JsonObject root=new JsonObject();
            root.addProperty("schema",schema);
            root.addProperty("referenceRevision",referenceRevision);
            root.addProperty("referencePolicySha256",referencePolicySha256);
            root.addProperty("scenarioSha256",scenarioSha256);
            root.addProperty("pinnedAggregateSha256",pinnedAggregateSha256);
            root.addProperty("pinnedStrataSha256",pinnedStrataSha256);
            root.addProperty("reportFingerprint",reportFingerprint);
            root.addProperty("allPassed",passed());
            root.addProperty("scope","pinned-offline-conditional-proxy;no-real-win-rate;not-minecraft");
            JsonArray versionsJson=new JsonArray();
            for(VersionResult v:versions){
                JsonObject item=new JsonObject();
                item.addProperty("revision",v.revision());
                item.addProperty("policySha256",v.policySha256());
                item.addProperty("graphSha256",v.graphSha256());
                item.addProperty("custodyReportSha256",v.custodyReportSha256());
                item.addProperty("replaySha256",v.replaySha256());
                item.addProperty("status",v.status().name());
                item.addProperty("aggregateGate",v.aggregateGate());
                item.addProperty("strataGate",v.strataGate());
                JsonObject d=new JsonObject();
                // Underlying exact comparisons still execute even when metrics
                // are withheld. Never expose single/private battle deltas.
                boolean disclose=v.changes().accepted()>=3;
                d.addProperty("suppressed",!disclose);
                if(disclose){
                    d.addProperty("accepted",v.changes().accepted());
                    d.addProperty("changedActions",v.changes().changedActions());
                    d.addProperty("newAbstentions",v.changes().newAbstentions());
                    d.addProperty("recoveredAbstentions",v.changes().recoveredAbstentions());
                    d.addProperty("newlyIllegal",v.changes().newlyIllegal());
                    d.addProperty("newlyExhausted",v.changes().newlyExhausted());
                    d.addProperty("newlyUnsupported",v.changes().newlyUnsupported());
                    d.addProperty("uncertaintyIncreases",v.changes().uncertaintyIncreases());
                    d.addProperty("penaltyIncreases",v.changes().penaltyIncreases());
                    d.addProperty("legacyAgreementLoss",v.changes().legacyAgreementLoss());
                    d.addProperty("legacyAgreementGain",v.changes().legacyAgreementGain());
                }
                item.add("deltas",d);
                versionsJson.add(item);
            }
            root.add("versions",versionsJson);
            return root.toString();
        }
    }

    /**
     * Exactly one reference version followed by 1–7 candidate revisions.
     * All versions are pinned and use the SAME local bundle ID list, scenario
     * map, custodian key, installed build, random seed and verified actor data.
     * No report supplied by a caller can bypass Phase 7.5 custody intake.
     */
    public static Report evaluateLocal(Path repo,List<String> bundles,
            Map<String,Phase7Benchmark.Scenario> scenarios,
            String custodianKeyId,String externallyPinnedCustodianKeySha,
            Phase7RegressionGate.PinnedBaseline aggregate,
            Phase7StratifiedAudit.PinnedPolicy strata,List<Version> versions) {
        Objects.requireNonNull(repo); Objects.requireNonNull(bundles);
        Objects.requireNonNull(scenarios); Objects.requireNonNull(aggregate);
        Objects.requireNonNull(strata); Objects.requireNonNull(versions);
        if(versions.size()<2 || versions.size()>MAX_VERSIONS
                || bundles.isEmpty() || bundles.size()>MAX_SAMPLES)
            throw new IllegalArgumentException("invalid matrix dimensions");
        List<Version> inputs=List.copyOf(versions);
        Set<String> revisions=new HashSet<>();
        var reference=inputs.getFirst().policy().value();
        String scenarioHash=Phase7ShadowReplay.scenarioSha256(scenarios);
        boolean matrixCompatible=true;
        for(Version v:inputs) {
            var policy=v.policy().value();
            if(!revisions.add(policy.revision()))throw new IllegalArgumentException(
                    "duplicate policy revision");
            if(!scenarioHash.equals(policy.scenarioSha256())
                    ||!policy.installedArtifactSha256().equals(reference.installedArtifactSha256())
                    ||policy.seed()!=reference.seed()
                    ||!v.graphs().keySet().equals(scenarios.keySet()))
                matrixCompatible=false;
        }
        // Compatibility must be checked BEFORE opening any private bundle.
        if(!matrixCompatible)throw new IllegalArgumentException(
                "incompatible pinned evidence/model version boundary");

        List<Phase7ShadowReplay.Evaluation> evaluated=new ArrayList<>();
        for(Version v:inputs)
            evaluated.add(Phase7ShadowReplay.replayLocal(repo,bundles,scenarios,
                    custodianKeyId,externallyPinnedCustodianKeySha,v.policy(),v.graphs()));
        var referenceEval=evaluated.getFirst();
        var referenceIntake=referenceEval.custodyReport();
        if(referenceIntake.corpus().coverage().accepted()<1)
            throw new IllegalArgumentException("no independently authenticated accepted Legacy evidence");
        // A replay may not invent complete traces, silently truncate them,
        // or pair candidate cases with different public-state records.
        List<VersionResult> results=new ArrayList<>();
        for(int i=1;i<inputs.size();i++){
            var input=inputs.get(i);
            var compared=evaluated.get(i);
            var current=compared.custodyReport();
            var aggregateResult=Phase7RegressionGate.compare(aggregate,referenceIntake,current);
            var strataResult=Phase7StratifiedAudit.compare(aggregate,strata,referenceIntake,current);
            boolean evidenceSame=sameEvidence(referenceIntake,current);
            boolean traceComplete=traceMatches(referenceEval)&&traceMatches(compared);
            Deltas delta=diff(referenceEval,inputs.getFirst(),compared,input);
            Status status;
            if(!evidenceSame)status=Status.EVIDENCE_DRIFT;
            else if(!traceComplete)status=Status.INCOMPLETE_TRACE;
            // Preserve the most specific causal failure while still publishing
            // the independently computed 7.6/7.7 verdicts. NONE can pass.
            else if(delta.newlyIllegal()>0)status=Status.NEW_ILLEGAL_DECISION;
            else if(delta.newlyExhausted()>0)status=Status.SEARCH_EXHAUSTION_REGRESSION;
            else if(delta.newAbstentions()>0 || delta.newlyUnsupported()>0)
                status=Status.ABSTENTION_REGRESSION;
            else if(delta.uncertaintyIncreases()>0 || delta.penaltyIncreases()>0)
                status=Status.UNCERTAINTY_REGRESSION;
            else if(aggregateResult.status()!=Phase7RegressionGate.Status.PASS)
                status=Status.GLOBAL_REGRESSION;
            else if(strataResult.status()!=Phase7StratifiedAudit.Status.PASS)
                status=Status.STRATIFIED_REGRESSION;
            else status=Status.PASS;
            results.add(new VersionResult(input.policy().value().revision(),
                    input.policy().fileSha256(),input.policy().value().modelSha256(),
                    sha256(current.json()),compared.replay().replayFingerprint(),
                    status,aggregateResult.status().name(),strataResult.status().name(),delta));
        }
        String meta=SCHEMA+"|"+reference.revision()+"|"+
                inputs.getFirst().policy().fileSha256()+"|"+scenarioHash+"|"+
                aggregate.fileSha256()+"|"+strata.fileSha256()+"|"+results;
        return new Report(SCHEMA,reference.revision(),
                inputs.getFirst().policy().fileSha256(),scenarioHash,
                aggregate.fileSha256(),strata.fileSha256(),sha256(meta),results);
    }

    /** Identical source evidence, including rejection statuses and row order. */
    private static boolean sameEvidence(Phase7TrustedIntake.Report a,
                                        Phase7TrustedIntake.Report b){
        if(a.seed()!=b.seed() || !a.intakeStatuses().equals(b.intakeStatuses())
                ||a.corpus().rows().size()!=b.corpus().rows().size())return false;
        for(int i=0;i<a.corpus().rows().size();i++){
            var x=a.corpus().rows().get(i);
            var y=b.corpus().rows().get(i);
            if(!Objects.equals(x.scenario(),y.scenario())
                    ||x.status()!=y.status()
                    ||!Objects.equals(x.stateFingerprint(),y.stateFingerprint())
                    ||!Objects.equals(x.publicFingerprint(),y.publicFingerprint())
                    ||!Objects.equals(x.legacyAction(),y.legacyAction())
                    ||!Objects.equals(x.baselineAction(),y.baselineAction())
                    ||!Objects.equals(x.baselineAgreement(),y.baselineAgreement()))
                return false;
        }
        return true;
    }

    private static boolean traceMatches(Phase7ShadowReplay.Evaluation e) {
        var rows=e.custodyReport().corpus().rows();
        var trace=e.replay();
        if(trace.traceTruncated()||rows.size()!=trace.traces().size()
                ||trace.status()==Phase7ShadowReplay.Reason.TRACE_BUDGET_EXCEEDED
                ||!trace.intakeReportSha256().equals(sha256(e.custodyReport().json())))
            return false;
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);var t=trace.traces().get(i);
            boolean eligible=row.status()==Phase7Corpus.Status.ACCEPTED;
            if(t.ordinal()!=i ||!t.group().equals(!eligible?"REJECTED":
                    row.legacyAction().startsWith("move:")?"MOVE":"SWITCH")
                    ||eligible && !Objects.equals(t.legacyAgreement(),row.shadowAgreement())
                            && !(t.abstained() && row.shadowAgreement()==null)
                    ||!eligible && t.reason()!=Phase7ShadowReplay.Reason.EVIDENCE_REJECTED)
                return false;
        }
        return true;
    }

    /** Public-only ROOT uncertainty proxy: max declared conditional uncertainty. */
    private static double rootUncertainty(Version version,String scenario,String action) {
        if(action==null)return 0;
        var graph=version.graphs().get(scenario);
        if(graph==null)return Double.POSITIVE_INFINITY;
        var position=graph.positions().get(graph.root());
        for(var b:position.branches())
            if(b.id().equals(action))
                return b.outcomes().stream().mapToDouble(
                        ShadowSearchEngine.Outcome::uncertainty).max().orElse(0);
        return Double.POSITIVE_INFINITY;
    }

    private static Deltas diff(Phase7ShadowReplay.Evaluation reference,Version older,
                               Phase7ShadowReplay.Evaluation candidate,Version newer){
        var a=reference.custodyReport().corpus().rows();
        var b=candidate.custodyReport().corpus().rows();
        var traceOld=reference.replay().traces();
        var traceNew=candidate.replay().traces();
        int accepted=0,changed=0,abstain=0,recovered=0,illegal=0,
                exhausted=0,unsupported=0,uncertainty=0,penalty=0,
                agreementLoss=0,agreementGain=0;
        for(int i=0;i<Math.min(a.size(),b.size());i++){
            var x=a.get(i),y=b.get(i);
            if(x.status()!=Phase7Corpus.Status.ACCEPTED
                    ||y.status()!=Phase7Corpus.Status.ACCEPTED)continue;
            accepted++;
            if(x.shadowAction()!=null&&y.shadowAction()!=null
                    &&!x.shadowAction().equals(y.shadowAction()))changed++;
            if(x.shadowAction()!=null&&y.shadowAction()==null)abstain++;
            if(x.shadowAction()==null&&y.shadowAction()!=null)recovered++;
            if(Boolean.TRUE.equals(x.shadowAgreement())
                    &&!Boolean.TRUE.equals(y.shadowAgreement()))agreementLoss++;
            if(!Boolean.TRUE.equals(x.shadowAgreement())
                    &&Boolean.TRUE.equals(y.shadowAgreement()))agreementGain++;
            if(i<traceOld.size()&&i<traceNew.size()){
                var oldTrace=traceOld.get(i),newTrace=traceNew.get(i);
                if(newTrace.reason()==Phase7ShadowReplay.Reason.ILLEGAL_DECISION
                        &&oldTrace.reason()!=Phase7ShadowReplay.Reason.ILLEGAL_DECISION)illegal++;
                if(newTrace.reason()==Phase7ShadowReplay.Reason.SEARCH_INCOMPLETE
                        &&oldTrace.reason()!=Phase7ShadowReplay.Reason.SEARCH_INCOMPLETE)exhausted++;
                if(newTrace.reason()!=Phase7ShadowReplay.Reason.SELECTED
                        &&oldTrace.reason()==Phase7ShadowReplay.Reason.SELECTED
                        &&newTrace.reason()!=Phase7ShadowReplay.Reason.SEARCH_INCOMPLETE
                        &&newTrace.reason()!=Phase7ShadowReplay.Reason.ILLEGAL_DECISION)unsupported++;
            }
            if(y.shadowAction()!=null&&x.shadowAction()!=null
                    &&rootUncertainty(newer,y.scenario(),y.shadowAction())
                    >rootUncertainty(older,x.scenario(),x.shadowAction())+1e-9)
                uncertainty++;
            if(newer.policy().value().uncertaintyPenalty()
                    >older.policy().value().uncertaintyPenalty()+1e-9)penalty++;
        }
        return new Deltas(accepted,changed,abstain,recovered,illegal,exhausted,
                unsupported,uncertainty,penalty,agreementLoss,agreementGain);
    }
    private static boolean validSha(String x){return x!=null&&SHA.matcher(x).matches();}
    private static String sha256(String x){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(x.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
