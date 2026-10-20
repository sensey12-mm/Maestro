package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.20: conservative OFFLINE end-to-end Phase 5 handoff and gate audit.
 *
 * Same original replay must occur exactly once in a whole-replay train/test
 * split. All inputs are re-ingested through signed custody, source-pinned
 * normalization, held-out metrics, frozen robustness and bounded opponent
 * hypotheses. This is NOT a battle integration, calibrated forecast,
 * independent licensing check, or approval to merge.
 */
public final class PublicCompetitiveHandoffAudit {
    private PublicCompetitiveHandoffAudit() {}
    public static final int VERSION=1;
    public static final String SCHEMA="phase5.20.offline-handoff.v1";

    // Snapshot IDs refer to INDEPENDENT branch inspections only. They are not
    // source fingerprints, runtime module identities, or attestations of
    // compatibility. A branch changing requires a new reviewed handoff.
    public static final String PHASE4_AUDITED_COMMIT=
            "6a1b101e575361cfa844f982be86f7370a5a433a";
    public static final String PHASE6_AUDITED_COMMIT=
            "520d436110ab5c3eeced99a16148b03ab50fe034";
    public static final String PHASE7_AUDITED_COMMIT=
            "e26be18526385d7177eb51dcd80b2efa276b472e";

    public enum Stage {
        SIGNED_REPLAY_PROVENANCE, NORMALIZED_OBSERVATIONS,
        SIGNED_WHOLE_REPLAY_SPLIT, HELDOUT_BASELINES,
        STRATIFIED_COVERAGE, FROZEN_DISTRIBUTION_SHIFT,
        BUILD_CANDIDATES_AND_ROBUSTNESS, OFFLINE_CONSUMER_CONTRACT,
        DURABLE_TRUST_HANDOFF, PHASE4_MECHANICS,
        PHASE6_SEARCH_INPUT, PHASE7_SIMULATION,
        INDEPENDENT_REAL_REPLAY, INSTALLED_GAMEPLAY
    }
    public enum Gate { OFFLINE_RECOMPUTED, NOT_ATTESTED, CROSS_PHASE_BLOCKED }
    public enum Blocker {
        NO_EXTERNALLY_PINNED_DURABLE_CUSTODY_CHECKPOINT,
        NO_INDEPENDENT_REPLAY_RIGHTS_OR_REAL_HOLDOUT,
        NO_REAL_WORLD_CALIBRATED_COMPETITIVE_SKILL,
        PHASE4_RUNTIME_MECHANICS_SOURCE_NOT_ATTESTED,
        PHASE5_SPECIES_SLICE_NOT_INSTALLED_REGISTRY,
        PHASE6_REQUIRES_RESPONSES_AND_PROBABILITIES_NOT_PROVIDED,
        PHASE6_NO_APPROVED_TACTICAL_CONSUMER,
        PHASE7_NO_INTEGRATED_PHASE4_5_6_CHAIN,
        NO_CURRENT_TURN_ACTOR_OR_LEGAL_ACTION_BINDING,
        NO_INSTALLED_MINECRAFT_ACCEPTANCE,
        NO_INDEPENDENT_INTEGRATION_REVIEW_OR_OWNER_MERGE_APPROVAL
    }

    /** This is a REVIEW SNAPSHOT, never an independently signed authority.
     * All three commits must exactly match the code inspected for Phase 5.20.
     * Future branch changes fail closed until reviewed anew. */
    public record CrossPhaseSnapshot(String phase4Commit,String phase6Commit,
                                     String phase7Commit) {
        public CrossPhaseSnapshot {
            if(!PHASE4_AUDITED_COMMIT.equals(phase4Commit)
                    || !PHASE6_AUDITED_COMMIT.equals(phase6Commit)
                    || !PHASE7_AUDITED_COMMIT.equals(phase7Commit))
                throw invalid();
        }
        public static CrossPhaseSnapshot audited() {
            return new CrossPhaseSnapshot(PHASE4_AUDITED_COMMIT,
                    PHASE6_AUDITED_COMMIT,PHASE7_AUDITED_COMMIT);
        }
    }
    public record Request(int schemaVersion,
                          PublicCompetitiveConsumerContract.ContractPin consumerPin,
                          PublicBuildCandidateRobustness.PolicyPin robustnessPin,
                          CrossPhaseSnapshot crossPhase,
                          boolean runtimeOrTacticalAccessRequested,
                          boolean ownerMergeApprovalClaimed) {
        public Request {
            if(schemaVersion!=VERSION || consumerPin==null
                    || robustnessPin==null || crossPhase==null
                    || !consumerPin.equals(
                       PublicCompetitiveConsumerContract.ContractPin.pinned())
                    || !robustnessPin.equals(
                       PublicBuildCandidateRobustness.PolicyPin.pinned())
                    || runtimeOrTacticalAccessRequested || ownerMergeApprovalClaimed)
                throw invalid();
        }
        public static Request offlineReviewed() {
            return new Request(VERSION,
                    PublicCompetitiveConsumerContract.ContractPin.pinned(),
                    PublicBuildCandidateRobustness.PolicyPin.pinned(),
                    CrossPhaseSnapshot.audited(),false,false);
        }
    }
    /** Only fixed categories and counts; no player names, replay hashes,
     * source IDs, move/item names, signatures or candidate assertions. */
    public record StageReceipt(Stage stage,Gate gate) {
        public StageReceipt {
            if(stage==null || gate==null)throw invalid();
        }
    }
    public record CoverageSummary(int trainingReplays,int heldOutReplays,
                                  int eligibleAppearances,int candidateAssertions,
                                  int robustnessScenarios,
                                  int insufficientStrata,
                                  int shiftAbstentions) {
        public CoverageSummary {
            if(trainingReplays<1 || heldOutReplays<1 || eligibleAppearances<1
                    || candidateAssertions<1 || robustnessScenarios<9
                    || insufficientStrata<0 || shiftAbstentions<0)throw invalid();
        }
    }
    public static final class Report {
        private final String schema,sourceScope,robustnessPolicySha256;
        private final Map<Stage,Gate> stages;
        private final Set<Blocker> blockers;
        private final CoverageSummary coverage;
        private Report(Map<Stage,Gate> stages,Set<Blocker> blockers,CoverageSummary coverage) {
            if(stages==null || blockers==null || coverage==null
                    || !stages.keySet().equals(EnumSet.allOf(Stage.class))
                    || blockers.size()!=Blocker.values().length)
                throw invalid();
            this.stages=Map.copyOf(stages);
            this.blockers=Set.copyOf(blockers);
            this.coverage=coverage;
            schema=SCHEMA;
            sourceScope=CobblemonMechanics.SOURCE;
            robustnessPolicySha256=PublicCompetitiveRobustnessAudit.frozenProtocolSha256();
        }
        public String schema(){return schema;}
        public String sourceScope(){return sourceScope;}
        public String robustnessPolicySha256(){return robustnessPolicySha256;}
        public Map<Stage,Gate> stages(){return stages;}
        public Set<Blocker> blockers(){return blockers;}
        public CoverageSummary coverage(){return coverage;}
        public boolean offlineChainRecomputed(){
            return stages.entrySet().stream().allMatch(e->
                switch(e.getKey()) {
                    case SIGNED_REPLAY_PROVENANCE,NORMALIZED_OBSERVATIONS,
                         SIGNED_WHOLE_REPLAY_SPLIT,HELDOUT_BASELINES,
                         STRATIFIED_COVERAGE,FROZEN_DISTRIBUTION_SHIFT,
                         BUILD_CANDIDATES_AND_ROBUSTNESS,OFFLINE_CONSUMER_CONTRACT ->
                            e.getValue()==Gate.OFFLINE_RECOMPUTED;
                    default -> true;
                });
        }
        public boolean independentPhase5DevelopmentReadyForReview(){
            return offlineChainRecomputed() && !blockers.isEmpty();
        }
        public boolean handoffApprovedForIntegration(){return false;}
        public boolean realReplayCompetitiveSkillProven(){return false;}
        public boolean installedCompatibilityVerified(){return false;}
        public boolean approvedPhase6SearchInput(){return false;}
        public boolean phase7Integrated(){return false;}
        public boolean currentBattleBound(){return false;}
        public boolean tacticalOrLivePromotionAuthorized(){return false;}
        public boolean mainMergeAuthorized(){return false;}
        @Override public String toString() {
            return "Phase520OfflineHandoff[synthetic-only;review-pending;integration-blocked]";
        }
    }

    public static Report evaluate(PublicReplayCorpus.Source source,String spectatorLog,
                                  List<PublicHeldOutEvaluation.Replay> split,
                                  List<PublicBuildCandidateFilter.Candidate> candidates,
                                  PublicSpeciesBuildLegality.SourcePin speciesPin,
                                  PublicFormBuildReconciliation.ResolverPin resolverPin,
                                  Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                                  PublicSignedCustody.SignedManifest manifest,
                                  PublicSignedCustody.TrustedPolicy trusted,
                                  long evaluatedAt,Request request,
                                  PublicCompetitiveRobustnessAudit.Registration frozen) {
        if(source==null || spectatorLog==null || split==null
                || candidates==null || speciesPin==null || resolverPin==null
                || overrides==null || manifest==null || trusted==null
                || request==null || frozen==null
                || !request.equals(Request.offlineReviewed())
                || !speciesPin.equals(PublicSpeciesBuildLegality.SourcePin.pinned())
                || !resolverPin.equals(PublicFormBuildReconciliation.ResolverPin.pinned())
                || candidates.isEmpty() || split.size()<2 || split.size()>1000)
            throw invalid();

        // Explicit same-input identity check; do not equate different replays
        // merely because they share an actor name or public species.
        int matches=0;
        for(var entry:split) {
            if(entry==null)throw invalid();
            if(entry.source().replayId().equals(source.replayId())) {
                if(!entry.source().equals(source)
                        || !entry.spectatorLog().equals(spectatorLog)
                        || entry.partition()!=PublicHeldOutEvaluation.Partition.HELD_OUT)
                    throw invalid();
                matches++;
            }
        }
        if(matches!=1)throw invalid();

        // Phase 5.1–5.9: signed custody AND recomputed normalization and
        // appearance-local tendencies. No source credentials are embedded.
        var admitted=PublicReplayProvenance.ingestWithSignedCustody(source,spectatorLog,
                speciesPin.target(),manifest,trusted,evaluatedAt);
        if(!admitted.signedCustodyCurrentlyEligible(manifest,trusted,evaluatedAt)
                || admitted.independentlyAuthorized() || admitted.currentBattleBound()
                || admitted.tacticalDecisionReady())throw invalid();
        var shadow=admitted.shadowAudit();
        if(!shadow.sameUpstreamEvidence() || !shadow.offlineSchemaCompatible()
                || shadow.runtimeBindingAvailable() || shadow.shadowTacticalReady()
                || shadow.livePromotionAuthorized())throw invalid();

        // Whole-replay heldout audit: phase 5.4–5.5 duplicates/chronology,
        // phase 5.7–5.9 signed custody, phase 5.11 coverage, phase 5.12
        // pre-registered protocol and distribution-shift stress. The supplied
        // registration is NOT independently proven to predate the corpus.
        var heldout=PublicHeldOutEvaluation.evaluate(split,speciesPin.target());
        var checked=PublicReplayProvenance.verifySplitWithSignedCustody(split,
                speciesPin.target(),heldout,manifest,trusted,evaluatedAt);
        if(!checked.signedCustodyCurrentlyEligible(manifest,trusted,evaluatedAt)
                || checked.calibratedForecast() || checked.legalActionReceipt()
                || checked.trainingReplays()!=heldout.trainReplays()
                || checked.heldOutReplays()!=heldout.heldOutReplays())
            throw invalid();
        var coverage=PublicCompetitiveCoverageAudit.audit(split,speciesPin.target(),
                heldout,manifest,trusted,evaluatedAt);
        if(!heldout.splitSha256().equals(coverage.splitHash())
                || coverage.crossReplayIdentityUsed()
                || coverage.readyForRealWorldPerformanceClaims())throw invalid();
        if(!frozen.protocolSha256().equals(
                  PublicCompetitiveRobustnessAudit.frozenProtocolSha256())
                || !frozen.testSplitSha256().equals(heldout.splitSha256()))
            throw invalid();
        var robustness=PublicCompetitiveRobustnessAudit.audit(split,speciesPin.target(),
                heldout,manifest,trusted,evaluatedAt,frozen);
        if(!heldout.splitSha256().equals(robustness.testSplitSha256())
                || robustness.independentlyPreRegistered()
                || robustness.calibratedForecastProduced()
                || robustness.realWorldSkillVerified()
                || robustness.livePromotionAuthorized()
                || coverage.groups().size()!=20
                || robustness.strata().size()!=20
                || robustness.stressScenarios().size()!=30)
            throw invalid();

        // Phase 5.13–5.19: self-verifying build observations, source/form
        // evidence, candidate elimination protection, synthetic sensitivity
        // and immutable privacy-minimized consumer shape.
        var export=PublicCompetitiveConsumerContract.prepare(source,spectatorLog,
                candidates,speciesPin,resolverPin,overrides,manifest,trusted,
                evaluatedAt,PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());
        if(!export.currentSignedCustodyRevalidated()
                || export.replayUseRightsIndependentlyVerified()
                || export.installedRegistryVerified()
                || export.currentBattleBound()
                || export.opponentIdentityConfirmed()
                || export.legalMovesetsConfirmed()
                || export.itemOrAbilityBuildConfirmed()
                || export.calibratedPredictionsAvailable()
                || export.shadowTacticalReady()
                || export.livePromotionAuthorized()
                || export.candidates().size()!=candidates.size()
                || export.appearances().isEmpty()
                || !export.sourceRevision().equals(PublicSpeciesBuildLegality.SOURCE_REVISION)
                || !export.mechanicsScope().equals(CobblemonMechanics.SOURCE)
                || !export.policySha256().equals(
                        PublicBuildCandidateRobustness.frozenPolicySha256()))
            throw invalid();
        for(var row:export.candidates()) {
            if(row.actualOpponentEliminationAllowed()
                    || row.evaluatedScenarios()!=
                       PublicBuildCandidateRobustness.Perturbation.values().length
                    || row.appearanceIndex()<1)throw invalid();
        }
        int insufficient=0;
        for(var g:coverage.groups())if(!g.thresholdMet())insufficient++;
        var gates=new EnumMap<Stage,Gate>(Stage.class);
        for(Stage stage:Stage.values()){
            Gate result=switch(stage) {
                case SIGNED_REPLAY_PROVENANCE,NORMALIZED_OBSERVATIONS,
                     SIGNED_WHOLE_REPLAY_SPLIT,HELDOUT_BASELINES,
                     STRATIFIED_COVERAGE,FROZEN_DISTRIBUTION_SHIFT,
                     BUILD_CANDIDATES_AND_ROBUSTNESS,OFFLINE_CONSUMER_CONTRACT ->
                        Gate.OFFLINE_RECOMPUTED;
                case DURABLE_TRUST_HANDOFF,INDEPENDENT_REAL_REPLAY ->
                        Gate.NOT_ATTESTED;
                case PHASE4_MECHANICS,PHASE6_SEARCH_INPUT,PHASE7_SIMULATION,
                     INSTALLED_GAMEPLAY -> Gate.CROSS_PHASE_BLOCKED;
            };
            gates.put(stage,result);
        }
        var blockers=EnumSet.allOf(Blocker.class);
        return new Report(gates,blockers,new CoverageSummary(
                checked.trainingReplays(),checked.heldOutReplays(),
                export.appearances().size(),export.candidates().size(),
                export.candidates().size()
                    *PublicBuildCandidateRobustness.Perturbation.values().length,
                insufficient,robustness.abstainedStressScenarios()));
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 5.20 handoff requires coherent signed offline evidence");
    }
}
