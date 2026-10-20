package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * Phase 5.18 — offline candidate false-elimination sensitivity checks.
 *
 * Counterfactuals ONLY WEAKEN a fully revalidated public observation.
 * They do not create replay events, fabricate signed custody, claim a
 * known opponent's true set, or alter Phase 5.17's diagnostic semantics.
 */
public final class PublicBuildCandidateRobustness {
    private PublicBuildCandidateRobustness() {}
    public static final int VERSION=1;
    private static final String POLICY_TEXT=
            "phase5.18|v1|"+PublicSpeciesBuildLegality.SOURCE_REVISION
            +"|base,omit_move,blind_form,omit_ability,omit_item,illusion,"
            +"transform,change_item_ability,incompatible_source"
            +"|never_eliminate_opponent|no_legal_set|no_probability"
            +"|signed_phase5.17_only|fixed_scenarios|shadow_observational";
    private static final String POLICY_SHA256=digest(POLICY_TEXT);

    public enum Perturbation {
        REFERENCE,
        OMIT_PUBLIC_MOVE,
        BLIND_DISPLAY_FORM,
        OMIT_PUBLIC_ABILITY,
        OMIT_PUBLIC_ITEM,
        POSSIBLE_ILLUSION,
        POSSIBLE_TRANSFORM,
        ITEM_OR_ABILITY_CHANGED,
        INCOMPATIBLE_SOURCE
    }
    /** These are source-level sensitivity labels, NOT game outcomes. */
    public enum Finding {
        SOURCE_CONDITIONAL_ONLY,
        ASSERTION_CONTRADICTION_ONLY,
        UNRESOLVED,
        ABSTAINED
    }
    public enum Blocker {
        REFERENCE_ONLY,
        MISSING_WITNESSED_MOVE,
        UNKNOWN_DISPLAY_FORM,
        MISSING_ABILITY_REVEAL,
        MISSING_ITEM_REVEAL,
        POSSIBLE_DISGUISE,
        POSSIBLE_COPIED_MOVES,
        UNSUPPORTED_ITEM_OR_ABILITY_TRANSITION,
        INCOMPATIBLE_OR_OVERRIDDEN_SOURCE,
        PREEXISTING_UNCERTAINTY
    }

    public record PolicyPin(int version,String sha256) {
        public PolicyPin {
            if(version!=VERSION || sha256==null || !POLICY_SHA256.equals(sha256))
                throw invalid();
        }
        public static PolicyPin pinned() {return new PolicyPin(VERSION,POLICY_SHA256);}
    }
    public static String frozenPolicySha256() {return POLICY_SHA256;}
    public static String frozenPolicyDefinition() {return POLICY_TEXT;}

    public record Scenario(int candidateOrdinal,int appearanceIndex,
                           Perturbation perturbation,Finding finding,
                           Set<Blocker> blockers,
                           boolean evidenceWeakened,
                           boolean brittleContradiction,
                           boolean opponentEliminationAllowed) {
        public Scenario {
            if(candidateOrdinal<1 || appearanceIndex<1 || perturbation==null
                    || finding==null || blockers==null || blockers.isEmpty()
                    || opponentEliminationAllowed
                    || (perturbation==Perturbation.REFERENCE && evidenceWeakened)
                    || (brittleContradiction
                        && (perturbation==Perturbation.REFERENCE
                            || finding==Finding.ASSERTION_CONTRADICTION_ONLY)))
                throw invalid();
            blockers=Set.copyOf(blockers);
        }
    }
    public record Readiness(int mandatoryScenarioKinds,int expectedScenarioCount,
                            int evaluatedScenarioCount,int possibleFalseEliminationRisks,
                            int abstentions,int assertionContradictions,
                            boolean signedSourceChainRevalidated,
                            boolean identityAndRealBuildUnproven,
                            boolean mechanicsAndInstalledRegistryCompatible,
                            boolean realWorldFalseEliminationsMeasured,
                            boolean liveOrShadowPromotionAuthorized) {
        public Readiness {
            if(mandatoryScenarioKinds!=Perturbation.values().length
                    || expectedScenarioCount<0
                    || evaluatedScenarioCount!=expectedScenarioCount
                    || possibleFalseEliminationRisks<0 || abstentions<0
                    || assertionContradictions<0 || !signedSourceChainRevalidated
                    || !identityAndRealBuildUnproven
                    || mechanicsAndInstalledRegistryCompatible
                    || realWorldFalseEliminationsMeasured
                    || liveOrShadowPromotionAuthorized) throw invalid();
        }
    }
    public static final class Report {
        private final String policyHash;
        private final String sourceScope;
        private final List<Scenario> scenarios;
        private final Readiness readiness;
        private Report(String policyHash,List<Scenario> values,Readiness readiness) {
            if(!POLICY_SHA256.equals(policyHash) || values==null || readiness==null
                    || values.size()!=readiness.evaluatedScenarioCount())
                throw invalid();
            this.policyHash=policyHash;
            this.sourceScope="cobblemon-1.7.3-source-"+PublicSpeciesBuildLegality.SOURCE_REVISION;
            this.scenarios=List.copyOf(values);
            this.readiness=readiness;
        }
        public String policySha256(){return policyHash;}
        public String sourceScope(){return sourceScope;}
        public List<Scenario> scenarios(){return scenarios;}
        public Readiness readiness(){return readiness;}
        public boolean opponentEliminationAuthorized(){return false;}
        public boolean independentGroundTruthAvailable(){return false;}
        public boolean predictionProbabilitiesProduced(){return false;}
        public boolean shadowTacticalReady(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "OfflineCandidateRobustness[synthetic-sensitivity;zero-elimination-authority]";
        }
    }

    /**
     * Exact pinned policy and the complete original replay/signed custody are
     * independently checked by Phase 5.17 on every invocation. Variants are
     * privacy-safe, deterministic LOSS-OF-INFORMATION thought experiments
     * over its classifications; no spoofed event or new rights attestation.
     */
    public static Report evaluate(PublicReplayCorpus.Source source,String log,
                                  List<PublicBuildCandidateFilter.Candidate> candidates,
                                  PublicSpeciesBuildLegality.SourcePin sourcePin,
                                  PublicFormBuildReconciliation.ResolverPin resolverPin,
                                  Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                                  PublicSignedCustody.SignedManifest signed,
                                  PublicSignedCustody.TrustedPolicy trust,
                                  long evaluatedAt,PolicyPin policy) {
        if(policy==null || sourcePin==null || resolverPin==null || overrides==null
                || candidates==null || signed==null || trust==null)throw invalid();
        var baseline=PublicBuildCandidateFilter.filter(source,log,candidates,
                sourcePin,resolverPin,overrides,signed,trust,evaluatedAt);
        if(baseline.anyCompleteBuildConfirmed() || baseline.anyOpponentIdentityConfirmed()
                || baseline.livePromotionAuthorized()
                || baseline.results().size()!=candidates.size())throw invalid();
        var rows=new ArrayList<Scenario>();
        int risks=0,abstentions=0,contradictions=0;
        for(int i=0;i<baseline.results().size();i++) {
            var b=baseline.results().get(i);
            var c=candidates.get(i);
            if(b.inputOrdinal()!=i+1 || b.appearanceIndex()!=c.appearanceIndex())
                throw invalid();
            for(Perturbation perturbation:Perturbation.values()) {
                Scenario scenario=project(b,c,perturbation);
                rows.add(scenario);
                if(scenario.brittleContradiction())risks++;
                if(scenario.finding()==Finding.ABSTAINED)abstentions++;
                if(scenario.finding()==Finding.ASSERTION_CONTRADICTION_ONLY)
                    contradictions++;
            }
        }
        int expected=Math.multiplyExact(candidates.size(),Perturbation.values().length);
        if(rows.size()!=expected)throw invalid();
        var readiness=new Readiness(Perturbation.values().length,expected,
                rows.size(),risks,abstentions,contradictions,
                true,true,false,false,false);
        return new Report(POLICY_SHA256,rows,readiness);
    }

    private static Scenario project(PublicBuildCandidateFilter.Result original,
                                    PublicBuildCandidateFilter.Candidate candidate,
                                    Perturbation shift) {
        var reasons=original.reasons();
        var blockers=EnumSet.noneOf(Blocker.class);
        Finding base=base(original.verdict());
        Finding finding=base;
        boolean weakened=false;
        boolean fragile=false;
        switch(shift) {
            case REFERENCE -> blockers.add(Blocker.REFERENCE_ONLY);
            case OMIT_PUBLIC_MOVE -> {
                if(reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_MOVE_MATCH)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.EXPLICIT_EXCLUSION_VIOLATED)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.TOO_MANY_PUBLIC_NATIVE_MOVES)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_SOURCE_DISAGREEMENT)) {
                    weakened=true;blockers.add(Blocker.MISSING_WITNESSED_MOVE);
                    boolean excluded=reasons.contains(
                            PublicBuildCandidateFilter.Reason.EXPLICIT_EXCLUSION_VIOLATED);
                    boolean otherContradiction=original.directContradictions()>1
                            || reasons.contains(PublicBuildCandidateFilter.Reason.DISPLAY_FORM_MISMATCH)
                            || reasons.contains(PublicBuildCandidateFilter.Reason.MOVE_NOT_LISTED_IN_PINNED_SOURCE);
                    if(base==Finding.ASSERTION_CONTRADICTION_ONLY
                            && excluded && !otherContradiction)
                        finding=Finding.UNRESOLVED;
                    // An unknown observed/source discrepancy cannot be cured
                    // by pretending the unseen move was legally impossible.
                    if(reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_SOURCE_DISAGREEMENT))
                        finding=Finding.ABSTAINED;
                }
            }
            case BLIND_DISPLAY_FORM -> {
                weakened=true;blockers.add(Blocker.UNKNOWN_DISPLAY_FORM);
                finding=Finding.ABSTAINED;
            }
            case OMIT_PUBLIC_ABILITY -> {
                if(reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ABILITY_MATCH)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ABILITY_TRANSITION)) {
                    weakened=true;blockers.add(Blocker.MISSING_ABILITY_REVEAL);
                    if(reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ABILITY_TRANSITION))
                        finding=Finding.ABSTAINED;
                    // Source listing remains source-only, no observed claim.
                }
            }
            case OMIT_PUBLIC_ITEM -> {
                if(reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ITEM_TRANSITION)
                        || !candidate.possibleItems().isEmpty()) {
                    weakened=true;blockers.add(Blocker.MISSING_ITEM_REVEAL);
                    if(base==Finding.SOURCE_CONDITIONAL_ONLY)
                        finding=Finding.UNRESOLVED;
                }
            }
            case POSSIBLE_ILLUSION -> {
                weakened=true;blockers.add(Blocker.POSSIBLE_DISGUISE);
                finding=Finding.ABSTAINED;
            }
            case POSSIBLE_TRANSFORM -> {
                weakened=true;blockers.add(Blocker.POSSIBLE_COPIED_MOVES);
                finding=Finding.ABSTAINED;
            }
            case ITEM_OR_ABILITY_CHANGED -> {
                if(!candidate.possibleAbilities().isEmpty()
                        || !candidate.possibleItems().isEmpty()
                        || reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ABILITY_MATCH)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ITEM_TRANSITION)
                        || reasons.contains(PublicBuildCandidateFilter.Reason.PUBLIC_ABILITY_TRANSITION)) {
                    weakened=true;blockers.add(Blocker.UNSUPPORTED_ITEM_OR_ABILITY_TRANSITION);
                    finding=Finding.ABSTAINED;
                }
            }
            case INCOMPATIBLE_SOURCE -> {
                weakened=true;blockers.add(Blocker.INCOMPATIBLE_OR_OVERRIDDEN_SOURCE);
                finding=Finding.ABSTAINED;
            }
        }
        if(blockers.isEmpty())blockers.add(Blocker.PREEXISTING_UNCERTAINTY);
        // A "risk" is sensitivity of the *assumed elimination* to missing
        // evidence, NOT a measured false elimination with known ground truth.
        if(base==Finding.ASSERTION_CONTRADICTION_ONLY
                && finding!=Finding.ASSERTION_CONTRADICTION_ONLY)
            fragile=true;
        return new Scenario(original.inputOrdinal(),original.appearanceIndex(),
                shift,finding,blockers,weakened,fragile,false);
    }

    private static Finding base(PublicBuildCandidateFilter.Verdict verdict) {
        return switch(verdict) {
            case SUPPORTED -> Finding.SOURCE_CONDITIONAL_ONLY;
            case CONTRADICTED -> Finding.ASSERTION_CONTRADICTION_ONLY;
            case UNRESOLVED -> Finding.UNRESOLVED;
            case ABSTAINED -> Finding.ABSTAINED;
        };
    }
    private static String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable",e);
        }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified offline candidate robustness evidence");
    }
}
