package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.19 — version-pinned OFFLINE interface for future Shadow consumers.
 *
 * This adapter exposes only source-conditional counts and diagnostic labels.
 * It never exposes actual replay inputs, opponent build sets, current-battle
 * state, probabilities, legal actions, recommendations, or promotion rights.
 * It does not accept preconstructed report objects as provenance.
 */
public final class PublicCompetitiveConsumerContract {
    private PublicCompetitiveConsumerContract() {}
    public static final int VERSION=1;
    public static final String SOURCE_REVISION=PublicSpeciesBuildLegality.SOURCE_REVISION;

    public enum Purpose { OFFLINE_SHADOW_REVIEW, SHADOW_TACTICAL, LIVE_CONTROL }
    public enum Classification {
        SOURCE_CONDITIONAL_ONLY,
        ASSERTION_DISAGREEMENT_ONLY,
        UNRESOLVED,
        ABSTAINED
    }
    public enum Uncertainty {
        APPEARANCE_NOT_UNIQUE_OPPONENT_IDENTITY,
        INSTALLED_MECHANICS_AND_LEGALITY_UNVERIFIED,
        OPERATOR_REPLAY_RIGHTS_NOT_INDEPENDENTLY_PROVEN,
        UNKNOWN_ITEM_LEGALITY,
        INCOMPLETE_FORM_OR_SOURCE,
        PUBLIC_SOURCE_DISAGREEMENT,
        TRANSFORM_OR_FORM_CHANGE,
        AMBIGUOUS_ITEM_OR_ABILITY_CHANGE,
        INCOMPLETE_MOVE_EVIDENCE,
        CONDITIONAL_ROLE_ONLY,
        ROBUSTNESS_SENSITIVE,
        SYNTHETIC_SENSITIVITY_NOT_GROUND_TRUTH
    }

    public record ContractPin(int version,String normalizedSpeciesSha256,
                              String resolverSourceRevision,String mechanicsScope,
                              String robustnessPolicySha256) {
        public ContractPin {
            if(version!=VERSION
                    || !PublicSpeciesBuildLegality.NORMALIZED_SHA256.equals(normalizedSpeciesSha256)
                    || !SOURCE_REVISION.equals(resolverSourceRevision)
                    || !CobblemonMechanics.SOURCE.equals(mechanicsScope)
                    || !PublicBuildCandidateRobustness.frozenPolicySha256()
                        .equals(robustnessPolicySha256))
                throw invalid();
        }
        public static ContractPin pinned() {
            return new ContractPin(VERSION,PublicSpeciesBuildLegality.NORMALIZED_SHA256,
                    SOURCE_REVISION,CobblemonMechanics.SOURCE,
                    PublicBuildCandidateRobustness.frozenPolicySha256());
        }
    }
    /**
     * A request permits ONLY offline review; never authorizes linking to a
     * current battle, trainer party, or decision-making pipeline.
     */
    public record ConsumerRequest(Purpose purpose,ContractPin pin,
                                  boolean currentBattleBindingRequested,
                                  boolean tacticalUseRequested) {
        public ConsumerRequest {
            if(purpose!=Purpose.OFFLINE_SHADOW_REVIEW || pin==null
                    || currentBattleBindingRequested || tacticalUseRequested)
                throw invalid();
        }
        public static ConsumerRequest offlinePinned() {
            return new ConsumerRequest(Purpose.OFFLINE_SHADOW_REVIEW,
                    ContractPin.pinned(),false,false);
        }
    }
    public record Coverage(int publiclyObserved,int considered,int sourceListed,
                           int unsupported,int contradictions,int unresolved) {
        public Coverage {
            if(publiclyObserved<0 || considered<0 || sourceListed<0
                    || unsupported<0 || contradictions<0 || unresolved<0
                    || publiclyObserved>considered
                    || sourceListed+unsupported+contradictions+unresolved>considered)
                throw invalid();
        }
    }
    /** Opaque appearance ordinal: NOT a stable Pokémon identity. */
    public record Appearance(int appearanceIndex,boolean identityUncertain,
                             Map<PublicBuildEvidenceConsistency.Field,Coverage> fields,
                             Set<Uncertainty> uncertainty) {
        public Appearance {
            if(appearanceIndex<1 || !identityUncertain || fields==null
                    || !fields.keySet().equals(
                        EnumSet.allOf(PublicBuildEvidenceConsistency.Field.class))
                    || uncertainty==null
                    || !uncertainty.contains(
                        Uncertainty.APPEARANCE_NOT_UNIQUE_OPPONENT_IDENTITY)
                    || !uncertainty.contains(
                        Uncertainty.INSTALLED_MECHANICS_AND_LEGALITY_UNVERIFIED))
                throw invalid();
            fields=Map.copyOf(fields);
            uncertainty=Set.copyOf(uncertainty);
        }
    }
    /**
     * A conditional source verdict is NEVER a predicted opponent set.
     * The ordinal refers only to the original untrusted request order.
     */
    public record CandidateView(int ordinal,int appearanceIndex,
                                Classification classification,
                                int sourceSupportedAssertions,int observedMatches,
                                int assertedContradictions,int unresolvedAssertions,
                                int evaluatedScenarios,int sensitiveContradictionScenarios,
                                Set<Uncertainty> uncertainty,
                                boolean actualOpponentEliminationAllowed) {
        public CandidateView {
            if(ordinal<1 || appearanceIndex<1 || classification==null
                    || sourceSupportedAssertions<0 || observedMatches<0
                    || assertedContradictions<0 || unresolvedAssertions<0
                    || evaluatedScenarios!=PublicBuildCandidateRobustness.Perturbation.values().length
                    || sensitiveContradictionScenarios<0
                    || sensitiveContradictionScenarios>evaluatedScenarios
                    || uncertainty==null || actualOpponentEliminationAllowed
                    || !uncertainty.contains(Uncertainty.APPEARANCE_NOT_UNIQUE_OPPONENT_IDENTITY)
                    || !uncertainty.contains(Uncertainty.SYNTHETIC_SENSITIVITY_NOT_GROUND_TRUTH))
                throw invalid();
            uncertainty=Set.copyOf(uncertainty);
        }
    }

    /** Cannot be minted externally as "trusted" report. Immutable counts only. */
    public static final class Export {
        private final int schemaVersion;
        private final String sourceRevision,mechanicsScope,policySha256;
        private final List<Appearance> appearances;
        private final List<CandidateView> candidates;
        private final boolean currentSignedCustodyRevalidated;
        private Export(ContractPin pin,List<Appearance> appearances,List<CandidateView> candidates) {
            if(pin==null || appearances==null || candidates==null)throw invalid();
            schemaVersion=VERSION;
            sourceRevision=SOURCE_REVISION;
            mechanicsScope=CobblemonMechanics.SOURCE;
            policySha256=PublicBuildCandidateRobustness.frozenPolicySha256();
            this.appearances=List.copyOf(appearances);
            this.candidates=List.copyOf(candidates);
            currentSignedCustodyRevalidated=true;
        }
        public int schemaVersion(){return schemaVersion;}
        public String sourceRevision(){return sourceRevision;}
        public String mechanicsScope(){return mechanicsScope;}
        public String policySha256(){return policySha256;}
        public List<Appearance> appearances(){return appearances;}
        public List<CandidateView> candidates(){return candidates;}
        public boolean currentSignedCustodyRevalidated(){return currentSignedCustodyRevalidated;}
        public boolean replayUseRightsIndependentlyVerified(){return false;}
        public boolean installedRegistryVerified(){return false;}
        public boolean currentBattleBound(){return false;}
        public boolean opponentIdentityConfirmed(){return false;}
        public boolean legalMovesetsConfirmed(){return false;}
        public boolean itemOrAbilityBuildConfirmed(){return false;}
        public boolean calibratedPredictionsAvailable(){return false;}
        public boolean isShadowTacticalInput(){return false;}
        public boolean shadowTacticalReady(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString() {
            return "OfflineCompetitiveContract[version=1;appearance-local;source-conditional;no-live]";
        }
    }

    /**
     * Always rerun Phase 5.17 and Phase 5.18 on ORIGINAL replay bytes under
     * current signed custody and the exact source/robustness pins. No accepted
     * "previous report" argument, no live consumer mode, and no replay cache.
     */
    public static Export prepare(PublicReplayCorpus.Source source,String spectatorLog,
                                 List<PublicBuildCandidateFilter.Candidate> proposed,
                                 PublicSpeciesBuildLegality.SourcePin speciesPin,
                                 PublicFormBuildReconciliation.ResolverPin resolverPin,
                                 Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                                 PublicSignedCustody.SignedManifest signed,
                                 PublicSignedCustody.TrustedPolicy trustedKeys,
                                 long evaluatedAt,ConsumerRequest request) {
        if(source==null || spectatorLog==null || proposed==null
                || speciesPin==null || resolverPin==null || overrides==null
                || signed==null || trustedKeys==null || request==null
                || request.purpose()!=Purpose.OFFLINE_SHADOW_REVIEW
                || request.currentBattleBindingRequested()
                || request.tacticalUseRequested()
                || !request.pin().equals(ContractPin.pinned())
                || proposed.isEmpty()
                || proposed.size()>PublicBuildCandidateFilter.MAX_CANDIDATES)
            throw invalid();
        var observations=PublicBuildEvidenceConsistency.audit(source,spectatorLog,
                toQueries(proposed),speciesPin,resolverPin,overrides,
                signed,trustedKeys,evaluatedAt);
        var filtered=PublicBuildCandidateFilter.filter(source,spectatorLog,proposed,
                speciesPin,resolverPin,overrides,signed,trustedKeys,evaluatedAt);
        var stress=PublicBuildCandidateRobustness.evaluate(source,spectatorLog,proposed,
                speciesPin,resolverPin,overrides,signed,trustedKeys,evaluatedAt,
                PublicBuildCandidateRobustness.PolicyPin.pinned());
        if(!observations.everyOpponentIdentityUncertain()
                || observations.installedGameLegalityVerified()
                || !stress.readiness().signedSourceChainRevalidated()
                || stress.readiness().mechanicsAndInstalledRegistryCompatible()
                || stress.readiness().realWorldFalseEliminationsMeasured()
                || !stress.policySha256().equals(request.pin().robustnessPolicySha256())
                || !filtered.sourceScope().endsWith(SOURCE_REVISION)
                || !stress.sourceScope().endsWith(SOURCE_REVISION)
                || !observations.versionScope().endsWith(SOURCE_REVISION)
                || stress.scenarios().size()!=proposed.size()
                    *PublicBuildCandidateRobustness.Perturbation.values().length
                || filtered.results().size()!=proposed.size())throw invalid();

        var appearances=new ArrayList<Appearance>();
        var byAppearance=new HashMap<Integer,Appearance>();
        for(var a:observations.appearances()) {
            var coverage=new EnumMap<PublicBuildEvidenceConsistency.Field,Coverage>(
                    PublicBuildEvidenceConsistency.Field.class);
            var uncertainty=baseUncertainty();
            for(var entry:a.fields().entrySet()) {
                var f=entry.getValue();
                coverage.put(entry.getKey(),new Coverage(f.observed(),f.proposed(),
                        f.sourceListed(),f.sourceUnsupported(),f.conflicts(),f.unresolved()));
                if(f.conflicts()>0)uncertainty.add(Uncertainty.PUBLIC_SOURCE_DISAGREEMENT);
                if(f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .DISPLAYED_FORM_NOT_SOURCE_SUPPORTED)
                    || f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .UNRESOLVED_SOURCE_OR_OVERRIDE))
                    uncertainty.add(Uncertainty.INCOMPLETE_FORM_OR_SOURCE);
                if(f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .PUBLIC_FORM_DISPLAY_TRANSITION_UNRESOLVED))
                    uncertainty.add(Uncertainty.TRANSFORM_OR_FORM_CHANGE);
                if(f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED)
                    || f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED))
                    uncertainty.add(Uncertainty.AMBIGUOUS_ITEM_OR_ABILITY_CHANGE);
                if(f.issues().contains(PublicBuildEvidenceConsistency.Issue
                        .MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET))
                    uncertainty.add(Uncertainty.INCOMPLETE_MOVE_EVIDENCE);
                if(entry.getKey()==PublicBuildEvidenceConsistency.Field.ITEM)
                    uncertainty.add(Uncertainty.UNKNOWN_ITEM_LEGALITY);
                if(entry.getKey()==PublicBuildEvidenceConsistency.Field.ROLE
                        && f.proposed()>0)
                    uncertainty.add(Uncertainty.CONDITIONAL_ROLE_ONLY);
            }
            if(a.transformed())
                uncertainty.add(Uncertainty.TRANSFORM_OR_FORM_CHANGE);
            if(!a.exactDisplayedSourceForm())
                uncertainty.add(Uncertainty.INCOMPLETE_FORM_OR_SOURCE);
            if(coverage.size()!=PublicBuildEvidenceConsistency.Field.values().length)
                throw invalid();
            var row=new Appearance(a.index(),true,coverage,uncertainty);
            if(byAppearance.putIfAbsent(a.index(),row)!=null)throw invalid();
            appearances.add(row);
        }
        var scenarios=new HashMap<Integer,EnumMap<PublicBuildCandidateRobustness.Perturbation,
                PublicBuildCandidateRobustness.Scenario>>();
        for(var s:stress.scenarios()) {
            if(s.opponentEliminationAllowed() || s.candidateOrdinal()<1
                    || s.candidateOrdinal()>proposed.size())throw invalid();
            var map=scenarios.computeIfAbsent(s.candidateOrdinal(),ignored->
                new EnumMap<>(PublicBuildCandidateRobustness.Perturbation.class));
            if(map.putIfAbsent(s.perturbation(),s)!=null)throw invalid();
        }
        var views=new ArrayList<CandidateView>();
        for(int i=0;i<proposed.size();i++) {
            int ordinal=i+1;
            var c=proposed.get(i);
            var r=filtered.results().get(i);
            var group=scenarios.get(ordinal);
            var a=byAppearance.get(c.appearanceIndex());
            if(c==null || a==null || r.inputOrdinal()!=ordinal
                    || r.appearanceIndex()!=c.appearanceIndex()
                    || group==null
                    || group.size()!=PublicBuildCandidateRobustness.Perturbation.values().length)
                throw invalid();
            var reference=group.get(PublicBuildCandidateRobustness.Perturbation.REFERENCE);
            if(reference==null || reference.appearanceIndex()!=r.appearanceIndex()
                    || reference.candidateOrdinal()!=ordinal
                    || reference.finding()!=mapping(r.verdict())
                    || reference.evidenceWeakened()
                    || reference.opponentEliminationAllowed())throw invalid();
            int risky=0;
            for(var entry:group.entrySet()){
                var s=entry.getValue();
                if(s.appearanceIndex()!=r.appearanceIndex() || s.candidateOrdinal()!=ordinal
                        || s.opponentEliminationAllowed())throw invalid();
                if(s.brittleContradiction())risky++;
            }
            var uncertainty=EnumSet.copyOf(a.uncertainty());
            uncertainty.add(Uncertainty.SYNTHETIC_SENSITIVITY_NOT_GROUND_TRUTH);
            if(risky>0)uncertainty.add(Uncertainty.ROBUSTNESS_SENSITIVE);
            if(r.verdict()==PublicBuildCandidateFilter.Verdict.UNRESOLVED
                    || r.verdict()==PublicBuildCandidateFilter.Verdict.ABSTAINED)
                uncertainty.add(Uncertainty.INCOMPLETE_MOVE_EVIDENCE);
            views.add(new CandidateView(ordinal,r.appearanceIndex(),
                    classification(r.verdict()),r.sourceSupportedAssertions(),
                    r.observedMatches(),r.directContradictions(),r.unknownAssertions(),
                    group.size(),risky,uncertainty,false));
        }
        if(views.size()!=proposed.size())throw invalid();
        return new Export(request.pin(),appearances,views);
    }

    private static List<PublicOpponentBuildHypotheses.CandidateQuery> toQueries(
            List<PublicBuildCandidateFilter.Candidate> proposed){
        var grouped=new TreeMap<Integer,Map<PublicOpponentBuildHypotheses.Kind,Set<String>>>();
        for(var c:proposed) {
            if(c==null)throw invalid();
            var group=grouped.computeIfAbsent(c.appearanceIndex(),ignored->{
                var v=new EnumMap<PublicOpponentBuildHypotheses.Kind,Set<String>>(
                        PublicOpponentBuildHypotheses.Kind.class);
                v.put(PublicOpponentBuildHypotheses.Kind.MOVE,new TreeSet<>());
                v.put(PublicOpponentBuildHypotheses.Kind.ABILITY,new TreeSet<>());
                v.put(PublicOpponentBuildHypotheses.Kind.ITEM,new TreeSet<>());
                return v;
            });
            group.get(PublicOpponentBuildHypotheses.Kind.MOVE).addAll(c.requiredMoves());
            group.get(PublicOpponentBuildHypotheses.Kind.ABILITY).addAll(c.possibleAbilities());
            group.get(PublicOpponentBuildHypotheses.Kind.ITEM).addAll(c.possibleItems());
        }
        var out=new ArrayList<PublicOpponentBuildHypotheses.CandidateQuery>();
        for(var e:grouped.entrySet()){
            var fields=e.getValue();
            out.add(new PublicOpponentBuildHypotheses.CandidateQuery(e.getKey(),
                    fields.get(PublicOpponentBuildHypotheses.Kind.MOVE),
                    fields.get(PublicOpponentBuildHypotheses.Kind.ABILITY),
                    fields.get(PublicOpponentBuildHypotheses.Kind.ITEM)));
        }
        return out;
    }
    private static EnumSet<Uncertainty> baseUncertainty() {
        return EnumSet.of(Uncertainty.APPEARANCE_NOT_UNIQUE_OPPONENT_IDENTITY,
                Uncertainty.INSTALLED_MECHANICS_AND_LEGALITY_UNVERIFIED,
                Uncertainty.OPERATOR_REPLAY_RIGHTS_NOT_INDEPENDENTLY_PROVEN);
    }
    private static Classification classification(PublicBuildCandidateFilter.Verdict v){
        return switch(v) {
            case SUPPORTED->Classification.SOURCE_CONDITIONAL_ONLY;
            case CONTRADICTED->Classification.ASSERTION_DISAGREEMENT_ONLY;
            case UNRESOLVED->Classification.UNRESOLVED;
            case ABSTAINED->Classification.ABSTAINED;
        };
    }
    private static PublicBuildCandidateRobustness.Finding mapping(
            PublicBuildCandidateFilter.Verdict v){
        return switch(v) {
            case SUPPORTED->PublicBuildCandidateRobustness.Finding.SOURCE_CONDITIONAL_ONLY;
            case CONTRADICTED->PublicBuildCandidateRobustness.Finding.ASSERTION_CONTRADICTION_ONLY;
            case UNRESOLVED->PublicBuildCandidateRobustness.Finding.UNRESOLVED;
            case ABSTAINED->PublicBuildCandidateRobustness.Finding.ABSTAINED;
        };
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Unverified offline competitive consumer evidence");
    }
}
