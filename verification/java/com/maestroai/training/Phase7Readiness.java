package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.12 — offline end-to-end evidence chain and simulation readiness.
 * All work remains verification-only. Green synthetic tests NEVER establish
 * authentic Cobblemon mechanics, opponent predictions, live intelligence or
 * Minecraft compatibility. The Phase 4–6 integrated validation gate remains
 * physically unavailable in this subsystem, by design.
 */
public final class Phase7Readiness {
    private Phase7Readiness() {}
    public static final String SCHEMA="phase7.12.v1";
    private static final String BLOCKERS="phase4-reviewed-mechanics,phase5-opponent-model,"
            +"phase6-integrated-search,minecraft-acceptance";
    private static final java.util.regex.Pattern SHA=
            java.util.regex.Pattern.compile("[0-9a-f]{64}");

    /** This label must come from independently assessed evidence provenance. */
    public enum EvidenceTier { SYNTHETIC_TEST_ONLY, EXTERNALLY_REVIEWED_OFFLINE }
    public enum Status {
        SYNTHETIC_ONLY, CONFIGURATION_INCOMPATIBLE, CUSTODY_REJECTED,
        PUBLIC_IDENTITY_MISMATCH, CROSS_VERSION_BLOCKED, MECHANICS_REJECTED,
        OBSERVATIONAL_REPLAY_BLOCKED, AWAITING_INTEGRATED_PHASES_4_5_6
    }
    public enum Stage { TRUSTED_LEGACY, PINNED_SHADOW_REPLAY, VERSION_MATRIX,
                        PUBLIC_MECHANICS, SIGNED_OBSERVATIONAL_SELF_PLAY }
    public enum Gate { PASS, FAIL, NOT_RUN, SYNTHETIC_ONLY }

    /**
     * Every cryptographic credential stays in memory or the ignored local
     * custody tree. Inputs are not serialized and should not be logged.
     */
    public record Input(Path root,List<String> bundleNames,
                        Map<String,Phase7Benchmark.Scenario> legacyScenarios,
                        String custodianKeyId,String custodianKeySha256,
                        Phase7RegressionGate.PinnedBaseline aggregate,
                        Phase7StratifiedAudit.PinnedPolicy strata,
                        List<Phase7ReplayMatrix.Version> versions,
                        Phase7SelfPlay.Transitions transitions,
                        Phase7SelfPlay.Participant first,
                        Phase7SelfPlay.Participant second,
                        Phase7SelfPlay.PinnedPolicy selfPlayPolicy,
                        Phase7SelfPlay.Attestation transitionSignature,
                        String transitionReviewerKeySha256,
                        Phase7MechanicsAdapter.Bundle mechanics,
                        Phase7MechanicsAdapter.Attestation mechanicsSignature,
                        Phase7MechanicsAdapter.PinnedPolicy mechanicsPolicy,
                        EvidenceTier tier) {
        public Input {
            Objects.requireNonNull(root);Objects.requireNonNull(bundleNames);
            Objects.requireNonNull(legacyScenarios);Objects.requireNonNull(custodianKeyId);
            Objects.requireNonNull(custodianKeySha256);
            Objects.requireNonNull(aggregate);Objects.requireNonNull(strata);
            Objects.requireNonNull(versions);Objects.requireNonNull(transitions);
            Objects.requireNonNull(first);Objects.requireNonNull(second);
            Objects.requireNonNull(selfPlayPolicy);Objects.requireNonNull(transitionReviewerKeySha256);
            Objects.requireNonNull(mechanics);Objects.requireNonNull(mechanicsPolicy);
            Objects.requireNonNull(tier);
            bundleNames=List.copyOf(bundleNames);
            versions=List.copyOf(versions);
            legacyScenarios=Map.copyOf(legacyScenarios);
            if(bundleNames.isEmpty()||bundleNames.size()>100||versions.size()<2
                    ||versions.size()>8||!validSha(custodianKeySha256)
                    ||!validSha(transitionReviewerKeySha256))
                throw new IllegalArgumentException("invalid readiness scope");
        }
    }

    public record Report(String schema,Status status,EvidenceTier tier,
                         String chainFingerprint,String artifactSha256,
                         String custodyDigest,String shadowDigest,String matrixDigest,
                         String mechanicsDigest,String observationalDigest,
                         int authenticatedCases,int reviewedPublicTransitions,
                         int consistentSourceBindings,Map<Stage,Gate> stages) {
        public Report {
            if(!SCHEMA.equals(schema)||status==null||tier==null
                    ||!validSha(chainFingerprint)||stages==null
                    ||stages.size()!=Stage.values().length)
                throw new IllegalArgumentException("invalid readiness report");
            EnumMap<Stage,Gate> copy=new EnumMap<>(Stage.class);
            copy.putAll(stages);
            for(Stage stage:Stage.values())Objects.requireNonNull(copy.get(stage));
            stages=Collections.unmodifiableMap(copy);
        }
        public boolean integrated(){return false;} // no Phase 4–6 integration exists here
        public String json(){
            JsonObject out=new JsonObject();
            out.addProperty("schema",schema);out.addProperty("status",status.name());
            out.addProperty("evidenceTier",tier.name());
            out.addProperty("integrated",false);
            out.addProperty("chainFingerprint",chainFingerprint);
            out.addProperty("artifactSha256",artifactSha256);
            nullable(out,"custodyDigest",custodyDigest);
            nullable(out,"shadowDigest",shadowDigest);
            nullable(out,"matrixDigest",matrixDigest);
            nullable(out,"mechanicsDigest",mechanicsDigest);
            nullable(out,"observationalDigest",observationalDigest);
            out.addProperty("scope","offline-evidence-readiness;not-minecraft;"
                    +"not-win-rate;no-live-shadow-promotion");
            out.addProperty("blockedPrerequisites",BLOCKERS);
            // Keep low-cell counts masked even if evidence is valid.
            boolean disclosable=authenticatedCases>=3&&reviewedPublicTransitions>=3;
            out.addProperty("countsSuppressed",!disclosable);
            if(disclosable){
                out.addProperty("authenticatedCases",authenticatedCases);
                out.addProperty("reviewedPublicTransitions",reviewedPublicTransitions);
                out.addProperty("consistentSourceBindings",consistentSourceBindings);
            }
            JsonObject gates=new JsonObject();
            for(Stage stage:Stage.values())gates.addProperty(stage.name(),stages.get(stage).name());
            out.add("stages",gates);
            return out.toString();
        }
        private static void nullable(JsonObject out,String key,String value){
            if(value==null)out.add(key,JsonNull.INSTANCE);else out.addProperty(key,value);
        }
    }

    /**
     * Independently pinned identities are checked BEFORE opening raw local
     * custody data. Every first-player root scenario must have at least one
     * accepted Legacy public-state fingerprint later in the chain.
     */
    private static boolean compatible(Input input) {
        var baseline=input.versions().getFirst().policy().value();
        var first=input.first().policy().value();
        var second=input.second().policy().value();
        var own=input.selfPlayPolicy().value();
        var reviewed=input.mechanicsPolicy().value();
        var bundle=input.mechanics();
        if(!baseline.installedArtifactSha256().equals(first.installedArtifactSha256())
                ||!baseline.installedArtifactSha256().equals(second.installedArtifactSha256())
                ||!baseline.installedArtifactSha256().equals(own.artifactSha256())
                ||!baseline.installedArtifactSha256().equals(bundle.artifactSha256())
                ||!baseline.installedArtifactSha256().equals(reviewed.pinnedArtifactSha256())
                ||baseline.seed()!=first.seed()||baseline.seed()!=second.seed()
                ||baseline.seed()!=own.seed())
            return false;
        String transitionSha=Phase7SelfPlay.transitionsSha256(input.transitions(),
                input.first().scenarios(),input.second().scenarios());
        if(!own.transitionsSha256().equals(transitionSha)
                ||!bundle.transitionsSha256().equals(transitionSha)
                ||!reviewed.pinnedTransitionSha256().equals(transitionSha)
                ||!own.firstPolicySha256().equals(input.first().policy().fileSha256())
                ||!own.secondPolicySha256().equals(input.second().policy().fileSha256())
                ||!own.firstScenarioSha256().equals(
                        Phase7ShadowReplay.scenarioSha256(input.first().scenarios()))
                ||!own.secondScenarioSha256().equals(
                        Phase7ShadowReplay.scenarioSha256(input.second().scenarios())))
            return false;
        String legacySha=Phase7ShadowReplay.scenarioSha256(input.legacyScenarios());
        for(var version:input.versions()){
            if(!version.policy().value().scenarioSha256().equals(legacySha)
                    ||version.policy().value().seed()!=baseline.seed()
                    ||!version.policy().value().installedArtifactSha256()
                    .equals(baseline.installedArtifactSha256())
                    ||!version.graphs().keySet().equals(input.legacyScenarios().keySet()))
                return false;
        }
        return true;
    }

    /**
     * Runs all five actual verification components. An entire chain can be
     * offline-consistent but remains BLOCKED for final Phase 4–6 integration.
     */
    public static Report evaluate(Input input) {
        Objects.requireNonNull(input);
        EnumMap<Stage,Gate> stages=new EnumMap<>(Stage.class);
        for(Stage stage:Stage.values())stages.put(stage,Gate.NOT_RUN);
        String artifact=input.versions().getFirst().policy().value().installedArtifactSha256();
        String custody=null,shadow=null,matrix=null,mechanics=null,observed=null;
        int cases=0,reviewedCount=0,bindings=0;
        Status status;
        boolean aligned;
        try { aligned=compatible(input); }
        catch(RuntimeException invalid) { aligned=false; }
        if(!aligned){
            status=Status.CONFIGURATION_INCOMPATIBLE;
            return report(input,status,stages,artifact,custody,shadow,matrix,mechanics,
                    observed,cases,reviewedCount,bindings);
        }
        // Phase 7.5: actual signed local Legacy custody. Phase 7.8: actual
        // bounded Shadow choice on the approved public-only scenario set.
        Phase7ShadowReplay.Evaluation replay;
        try {
            var first=input.versions().getFirst();
            replay=Phase7ShadowReplay.replayLocal(input.root(),input.bundleNames(),
                    input.legacyScenarios(),input.custodianKeyId(),input.custodianKeySha256(),
                    first.policy(),first.graphs());
        }catch(RuntimeException ex){
            stages.put(Stage.TRUSTED_LEGACY,Gate.FAIL);
            return report(input,Status.CUSTODY_REJECTED,stages,artifact,custody,shadow,
                    matrix,mechanics,observed,cases,reviewedCount,bindings);
        }
        var intake=replay.custodyReport();
        custody=sha256(intake.json());
        shadow=replay.replay().replayFingerprint();
        cases=intake.corpus().coverage().accepted();
        boolean allApproved=cases==input.bundleNames().size()
                &&intake.intakeStatuses().stream().allMatch(
                        s->s==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS);
        if(cases==0||!allApproved){
            stages.put(Stage.TRUSTED_LEGACY,Gate.FAIL);
            status=Status.CUSTODY_REJECTED;
            return report(input,status,stages,artifact,custody,shadow,matrix,mechanics,
                    observed,cases,reviewedCount,bindings);
        }
        stages.put(Stage.TRUSTED_LEGACY,Gate.PASS);
        // The root's own public state must be an actually approved Legacy
        // state with exact fingerprint, not merely a similarly named species.
        var root=input.transitions().nodes().get(input.transitions().start());
        var rootOwn=input.first().scenarios().get(root.firstScenario());
        String stateHash=Phase7Benchmark.stateFingerprint(rootOwn);
        for(var row:intake.corpus().rows())
            if(row.status()==Phase7Corpus.Status.ACCEPTED
                    &&stateHash.equals(row.stateFingerprint()))bindings++;
        if(bindings!=1){
            status=Status.PUBLIC_IDENTITY_MISMATCH;
            stages.put(Stage.PINNED_SHADOW_REPLAY,Gate.FAIL);
            return report(input,status,stages,artifact,custody,shadow,matrix,mechanics,
                    observed,cases,reviewedCount,bindings);
        }
        if(replay.replay().traceTruncated()
                ||replay.replay().traces().size()!=intake.corpus().rows().size()){
            status=Status.CROSS_VERSION_BLOCKED;
            stages.put(Stage.PINNED_SHADOW_REPLAY,Gate.FAIL);
            return report(input,status,stages,artifact,custody,shadow,matrix,mechanics,
                    observed,cases,reviewedCount,bindings);
        }
        stages.put(Stage.PINNED_SHADOW_REPLAY,Gate.PASS);
        try {
            var comparison=Phase7ReplayMatrix.evaluateLocal(input.root(),input.bundleNames(),
                    input.legacyScenarios(),input.custodianKeyId(),input.custodianKeySha256(),
                    input.aggregate(),input.strata(),input.versions());
            matrix=comparison.reportFingerprint();
            if(!comparison.passed()){
                stages.put(Stage.VERSION_MATRIX,Gate.FAIL);
                return report(input,Status.CROSS_VERSION_BLOCKED,stages,artifact,custody,
                        shadow,matrix,mechanics,observed,cases,reviewedCount,bindings);
            }
        } catch(RuntimeException failure) {
            stages.put(Stage.VERSION_MATRIX,Gate.FAIL);
            return report(input,Status.CROSS_VERSION_BLOCKED,stages,artifact,custody,
                    shadow,matrix,mechanics,observed,cases,reviewedCount,bindings);
        }
        stages.put(Stage.VERSION_MATRIX,Gate.PASS);
        // Phase 7.11 entire graph audit, followed by Phase 7.10 itself.
        var gated=Phase7MechanicsAdapter.replay(input.transitions(),input.first(),
                input.second(),input.selfPlayPolicy(),input.transitionSignature(),
                input.transitionReviewerKeySha256(),input.mechanics(),
                input.mechanicsSignature(),input.mechanicsPolicy());
        var audit=gated.mechanics();
        mechanics=audit.reportFingerprint();
        reviewedCount=audit.totals().reviewed();
        if(!audit.eligible()){
            stages.put(Stage.PUBLIC_MECHANICS,Gate.FAIL);
            return report(input,Status.MECHANICS_REJECTED,stages,artifact,custody,shadow,
                    matrix,mechanics,observed,cases,reviewedCount,bindings);
        }
        stages.put(Stage.PUBLIC_MECHANICS,Gate.PASS);
        if(gated.observed()==null||!gated.observed().verified()){
            stages.put(Stage.SIGNED_OBSERVATIONAL_SELF_PLAY,Gate.FAIL);
            observed=gated.observed()==null?null:gated.observed().runFingerprint();
            return report(input,Status.OBSERVATIONAL_REPLAY_BLOCKED,stages,artifact,custody,
                    shadow,matrix,mechanics,observed,cases,reviewedCount,bindings);
        }
        observed=gated.observed().runFingerprint();
        // Synthetic keys may pass cryptographic test checks, but no synthetic
        // fixture is ever "independently verified" for gameplay.
        status=input.tier()==EvidenceTier.SYNTHETIC_TEST_ONLY
                ?Status.SYNTHETIC_ONLY:Status.AWAITING_INTEGRATED_PHASES_4_5_6;
        stages.put(Stage.SIGNED_OBSERVATIONAL_SELF_PLAY,
                input.tier()==EvidenceTier.SYNTHETIC_TEST_ONLY?
                        Gate.SYNTHETIC_ONLY:Gate.PASS);
        return report(input,status,stages,artifact,custody,shadow,matrix,mechanics,
                observed,cases,reviewedCount,bindings);
    }

    private static Report report(Input input,Status status,Map<Stage,Gate> stages,
            String artifact,String custody,String shadow,String matrix,
            String mechanics,String observed,int cases,int reviewed,int bindings){
        String context=SCHEMA+"|"+status+"|"+input.tier()+"|"+artifact+"|"+
                input.aggregate().fileSha256()+"|"+input.strata().fileSha256()+"|"+
                input.selfPlayPolicy().exactFileSha256()+"|"+
                input.mechanicsPolicy().fileSha256()+"|"+
                custody+"|"+shadow+"|"+matrix+"|"+mechanics+"|"+observed+"|"+
                cases+"|"+reviewed+"|"+bindings+"|"+stages;
        return new Report(SCHEMA,status,input.tier(),sha256(context),artifact,
                custody,shadow,matrix,mechanics,observed,cases,reviewed,bindings,stages);
    }
    private static boolean validSha(String sha){return sha!=null&&SHA.matcher(sha).matches();}
    private static String sha256(String s){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
