package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.17 — deterministic, OFFLINE candidate-assertion filtering.
 * 
 * A candidate describes ONE public appearance and partial assertions, NOT
 * a closed/four-move legal set, current ability/item, or a stable party member.
 * Source-contradicted != gameplay-illegal; source-supported != currently held.
 * The entire signed replay/custody chain is reconstructed on every call.
 */
public final class PublicBuildCandidateFilter {
    private PublicBuildCandidateFilter() {}
    public static final int VERSION=1;
    public static final String SOURCE_REVISION=PublicSpeciesBuildLegality.SOURCE_REVISION;
    public static final int MAX_CANDIDATES=40;

    public enum Verdict { SUPPORTED, CONTRADICTED, UNRESOLVED, ABSTAINED }
    public enum Reason {
        SOURCE_SUPPORTED_MOVE, SOURCE_SUPPORTED_ABILITY,
        PUBLIC_MOVE_MATCH, PUBLIC_ABILITY_MATCH,
        DISPLAY_FORM_MATCH,
        EXPLICIT_EXCLUSION_VIOLATED,
        DISPLAY_FORM_MISMATCH,
        MOVE_NOT_LISTED_IN_PINNED_SOURCE,
        ABILITY_NOT_LISTED_IN_PINNED_SOURCE,
        NONLEGAL_QUERY_METHOD_ONLY,
        ITEM_ELIGIBILITY_NOT_ATTESTED,
        NO_POSITIVE_BUILD_ASSERTIONS,
        PUBLIC_SOURCE_DISAGREEMENT,
        INCOMPLETE_OR_OVERRIDDEN_FIELD,
        TRANSFORM_OR_UNSUPPORTED_FORM,
        FORM_DISPLAY_TRANSITION,
        PUBLIC_ABILITY_TRANSITION,
        PUBLIC_ITEM_TRANSITION,
        TOO_MANY_PUBLIC_NATIVE_MOVES,
        IDENTITY_REMAINS_UNCERTAIN
    }
    /** The proposed species is ONLY the expected public display at this
     * appearance, never a claim about the hidden species behind Illusion. */
    public record Candidate(int appearanceIndex,String expectedDisplayedForm,
                            Set<String> requiredMoves,Set<String> excludedPublicMoves,
                            Set<String> possibleAbilities,Set<String> possibleItems) {
        public Candidate {
            if(appearanceIndex<1 || expectedDisplayedForm==null
                    || !expectedDisplayedForm.matches("[a-z0-9]{1,40}")
                    || requiredMoves==null || excludedPublicMoves==null
                    || possibleAbilities==null || possibleItems==null
                    || requiredMoves.size()>4 || excludedPublicMoves.size()>4
                    || possibleAbilities.size()>1 || possibleItems.size()>1)
                throw rejected();
            requiredMoves=canonical(requiredMoves);
            excludedPublicMoves=canonical(excludedPublicMoves);
            possibleAbilities=canonical(possibleAbilities);
            possibleItems=canonical(possibleItems);
            if(!Collections.disjoint(requiredMoves,excludedPublicMoves))throw rejected();
        }
    }
    public record Result(int inputOrdinal,int appearanceIndex,Verdict verdict,
                         Set<Reason> reasons,
                         int sourceSupportedAssertions,int observedMatches,
                         int directContradictions,int unknownAssertions,
                         boolean opponentIdentityResolved,
                         boolean completeNativeMovesetVerified,
                         boolean installedLegalityVerified) {
        public Result {
            if(inputOrdinal<1 || appearanceIndex<1 || verdict==null || reasons==null
                    || !reasons.contains(Reason.IDENTITY_REMAINS_UNCERTAIN)
                    || sourceSupportedAssertions<0 || observedMatches<0
                    || directContradictions<0 || unknownAssertions<0
                    || opponentIdentityResolved || completeNativeMovesetVerified
                    || installedLegalityVerified
                    || (verdict==Verdict.SUPPORTED
                        && (directContradictions!=0 || unknownAssertions!=0
                            || sourceSupportedAssertions==0))
                    || (verdict==Verdict.CONTRADICTED && directContradictions==0)
                    || (verdict==Verdict.UNRESOLVED && unknownAssertions==0)
                    || (verdict==Verdict.ABSTAINED && !reasons.stream().anyMatch(r->
                        ABSTAIN_REASONS.contains(r))))
                throw rejected();
            reasons=Set.copyOf(reasons);
        }
    }
    private static final Set<Reason> ABSTAIN_REASONS=Set.of(
            Reason.PUBLIC_SOURCE_DISAGREEMENT,
            Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD,
            Reason.TRANSFORM_OR_UNSUPPORTED_FORM,
            Reason.FORM_DISPLAY_TRANSITION,
            Reason.PUBLIC_ABILITY_TRANSITION,
            Reason.PUBLIC_ITEM_TRANSITION,
            Reason.TOO_MANY_PUBLIC_NATIVE_MOVES);
    public static final class Report {
        private final List<Result> results;
        private final Map<Verdict,Integer> totals;
        private final String scope;
        private Report(List<Result> rows) {
            results=List.copyOf(rows);
            var mutable=new EnumMap<Verdict,Integer>(Verdict.class);
            for(Verdict v:Verdict.values())mutable.put(v,0);
            for(Result row:rows) mutable.put(row.verdict(),mutable.get(row.verdict())+1);
            totals=Map.copyOf(mutable);
            scope="phase5.17/source-"+SOURCE_REVISION;
        }
        public List<Result> results() {return results;}
        public Map<Verdict,Integer> totals(){return totals;}
        public String sourceScope(){return scope;}
        public boolean anyOpponentIdentityConfirmed(){return false;}
        public boolean anyCompleteBuildConfirmed(){return false;}
        public boolean calibratedProbabilitiesProduced(){return false;}
        public boolean installedModpackLegalitiesVerified(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean shadowTacticalReady(){return false;}
        @Override public String toString(){
            return "OfflineCandidateFilter[ordinal-counts-only;no-build-certification]";
        }
    }

    public static Report filter(PublicReplayCorpus.Source source,String publicLog,
                                List<Candidate> candidates,
                                PublicSpeciesBuildLegality.SourcePin sourcePin,
                                PublicFormBuildReconciliation.ResolverPin resolverPin,
                                Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                                PublicSignedCustody.SignedManifest signed,
                                PublicSignedCustody.TrustedPolicy policy,long evaluatedAt) {
        if(source==null || publicLog==null || sourcePin==null || resolverPin==null
                || candidates==null || overrides==null || signed==null || policy==null
                || candidates.size()>MAX_CANDIDATES)throw rejected();
        for(Candidate c:candidates)if(c==null)throw rejected();

        // Union guesses by appearance; the upstream API accepts exactly one
        // bounded CandidateQuery per observed slot entry, not per hypothesis.
        // The candidate's EXCLUDED moves are kept separate as a claim about
        // public observations, never supplied as native moves.
        var union=new TreeMap<Integer,MutableQuery>();
        for(Candidate c:candidates) {
            var q=union.computeIfAbsent(c.appearanceIndex(),unused->new MutableQuery());
            q.moves.addAll(c.requiredMoves());
            q.abilities.addAll(c.possibleAbilities());
            q.items.addAll(c.possibleItems());
        }
        var guesses=new ArrayList<PublicOpponentBuildHypotheses.CandidateQuery>();
        for(var e:union.entrySet()) {
            var q=e.getValue();
            guesses.add(new PublicOpponentBuildHypotheses.CandidateQuery(
                    e.getKey(),q.moves,q.abilities,q.items));
        }
        // Full original signed custody and Phase 5.13->5.16 consistency
        // are re-verified. Public report objects aren't accepted as proof.
        var observed=PublicOpponentBuildHypotheses.analyze(
                source,publicLog,sourcePin.target(),guesses,signed,policy,evaluatedAt);
        var reconciled=PublicFormBuildReconciliation.reconcile(
                source,publicLog,guesses,sourcePin,resolverPin,overrides,
                signed,policy,evaluatedAt);
        var audit=PublicBuildEvidenceConsistency.audit(
                source,publicLog,guesses,sourcePin,resolverPin,overrides,
                signed,policy,evaluatedAt);
        if(!CobblemonMechanics.SOURCE.equals(observed.sourceScope())
                || audit.installedGameLegalityVerified() || audit.completeOpponentBuildVerified()
                || observed.appearances().size()!=reconciled.appearances().size()
                || observed.appearances().size()!=audit.appearances().size())
            throw rejected();

        var byIndex=new TreeMap<Integer,Snapshot>();
        for(int i=0;i<observed.appearances().size();i++) {
            var a=observed.appearances().get(i);
            var b=reconciled.appearances().get(i);
            var s=audit.appearances().get(i);
            if(a.index()!=b.index() || a.index()!=s.index()
                    || !a.displayedSpecies().equals(b.displayedId())
                    || !a.identityUncertain() || !b.identityUncertain()
                    || !s.identityUncertain() || a.identityLinked())throw rejected();
            if(byIndex.putIfAbsent(a.index(),new Snapshot(a,b,s))!=null)
                throw rejected();
        }
        var rows=new ArrayList<Result>();
        int ordinal=0;
        for(Candidate candidate:candidates){
            ordinal++;
            Snapshot snapshot=byIndex.get(candidate.appearanceIndex());
            if(snapshot==null)throw rejected();
            rows.add(evaluate(ordinal,candidate,snapshot));
        }
        return new Report(rows);
    }

    private static final class MutableQuery {
        final Set<String> moves=new TreeSet<>(),abilities=new TreeSet<>(),items=new TreeSet<>();
    }
    private record Snapshot(PublicOpponentBuildHypotheses.Appearance observed,
                            PublicFormBuildReconciliation.Appearance reconciled,
                            PublicBuildEvidenceConsistency.Appearance consistency) {}

    private static Result evaluate(int ordinal,Candidate candidate,Snapshot s) {
        var reasons=EnumSet.of(Reason.IDENTITY_REMAINS_UNCERTAIN);
        var obs=s.observed();
        var recon=s.reconciled();
        var check=s.consistency();

        // Global blockers refer to the public appearance, not any assumed
        // stable party member. Overrides are field-specific.
        if(obs.transformed() || !check.exactDisplayedSourceForm())
            reasons.add(Reason.TRANSFORM_OR_UNSUPPORTED_FORM);
        if(check.fields().get(PublicBuildEvidenceConsistency.Field.FORM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.PUBLIC_FORM_DISPLAY_TRANSITION_UNRESOLVED))
            reasons.add(Reason.FORM_DISPLAY_TRANSITION);
        if(check.fields().get(PublicBuildEvidenceConsistency.Field.ABILITY).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED))
            reasons.add(Reason.PUBLIC_ABILITY_TRANSITION);
        if(check.fields().get(PublicBuildEvidenceConsistency.Field.ITEM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED))
            reasons.add(Reason.PUBLIC_ITEM_TRANSITION);
        if(check.fields().get(PublicBuildEvidenceConsistency.Field.MOVE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET))
            reasons.add(Reason.TOO_MANY_PUBLIC_NATIVE_MOVES);
        if(check.gates().contains(PublicBuildEvidenceConsistency.Gate.ABSTAIN_SOURCE_DISAGREEMENT))
            reasons.add(Reason.PUBLIC_SOURCE_DISAGREEMENT);

        int supported=0,observedMatches=0,contradictions=0,unknown=0;
        // Only the public DISPLAY is compared. Disagreement does not
        // establish the hidden species/form behind a disguise.
        if(candidate.expectedDisplayedForm().equals(obs.displayedSpecies())) {
            reasons.add(Reason.DISPLAY_FORM_MATCH);
        } else {
            contradictions++;
            reasons.add(Reason.DISPLAY_FORM_MISMATCH);
        }

        var evidence=new EnumMap<PublicOpponentBuildHypotheses.Kind,
                Map<String,PublicFormBuildReconciliation.Evidence>>(
                PublicOpponentBuildHypotheses.Kind.class);
        for(var kind:PublicOpponentBuildHypotheses.Kind.values())
            evidence.put(kind,new TreeMap<>());
        for(var ev:recon.evidence()) {
            if(evidence.get(ev.kind()).putIfAbsent(ev.id(),ev)!=null)throw rejected();
        }
        for(String id:candidate.requiredMoves()) {
            var ev=require(evidence,PublicOpponentBuildHypotheses.Kind.MOVE,id);
            if(ev.publiclyObserved()){observedMatches++;reasons.add(Reason.PUBLIC_MOVE_MATCH);}
            switch(ev.finding()) {
                case SOURCE_LISTED_LEGAL_QUERY_METHOD -> {
                    supported++;reasons.add(Reason.SOURCE_SUPPORTED_MOVE);
                }
                case SOURCE_NOT_LISTED -> {
                    contradictions++;reasons.add(Reason.MOVE_NOT_LISTED_IN_PINNED_SOURCE);
                }
                case SOURCE_LISTED_NONLEGAL_QUERY_ONLY -> {
                    unknown++;reasons.add(Reason.NONLEGAL_QUERY_METHOD_ONLY);
                }
                default -> {
                    unknown++;reasons.add(Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD);
                }
            }
        }
        var witnessedMoves=new HashSet<String>();
        for(var h:obs.hypotheses())
            if(h.kind()==PublicOpponentBuildHypotheses.Kind.MOVE && h.publicEvent())
                witnessedMoves.add(h.id());
        for(String excluded:candidate.excludedPublicMoves()) {
            if(witnessedMoves.contains(excluded)) {
                contradictions++;
                reasons.add(Reason.EXPLICIT_EXCLUSION_VIOLATED);
            }
        }
        for(String id:candidate.possibleAbilities()) {
            var ev=require(evidence,PublicOpponentBuildHypotheses.Kind.ABILITY,id);
            if(ev.publiclyObserved()) {observedMatches++;reasons.add(Reason.PUBLIC_ABILITY_MATCH);}
            switch(ev.finding()) {
                case SOURCE_LISTED_NORMAL_ABILITY,
                     SOURCE_LISTED_HIDDEN_ABILITY,
                     SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY -> {
                    supported++;reasons.add(Reason.SOURCE_SUPPORTED_ABILITY);
                }
                case SOURCE_NOT_LISTED -> {
                    unknown++;reasons.add(Reason.ABILITY_NOT_LISTED_IN_PINNED_SOURCE);
                }
                default -> {
                    unknown++;reasons.add(Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD);
                }
            }
        }
        for(String id:candidate.possibleItems()) {
            var ev=require(evidence,PublicOpponentBuildHypotheses.Kind.ITEM,id);
            if(ev.finding()!=PublicFormBuildReconciliation.Finding.ITEM_LEGALITY_UNKNOWN)
                throw rejected();
            unknown++;reasons.add(Reason.ITEM_ELIGIBILITY_NOT_ATTESTED);
        }
        if(candidate.requiredMoves().isEmpty() && candidate.possibleAbilities().isEmpty()
                && candidate.possibleItems().isEmpty()) {
            unknown++;
            reasons.add(Reason.NO_POSITIVE_BUILD_ASSERTIONS);
        }
        if(reasons.contains(Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD)
                || (check.fields().get(PublicBuildEvidenceConsistency.Field.MOVE)
                        .issues().contains(PublicBuildEvidenceConsistency.Issue.UNRESOLVED_SOURCE_OR_OVERRIDE)
                        && !candidate.requiredMoves().isEmpty())
                || (check.fields().get(PublicBuildEvidenceConsistency.Field.ABILITY)
                        .issues().contains(PublicBuildEvidenceConsistency.Issue.UNRESOLVED_SOURCE_OR_OVERRIDE)
                        && !candidate.possibleAbilities().isEmpty()))
            reasons.add(Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD);

        Verdict verdict;
        if(!Collections.disjoint(reasons,ABSTAIN_REASONS))
            verdict=Verdict.ABSTAINED;
        else if(contradictions>0) verdict=Verdict.CONTRADICTED;
        else if(unknown>0) verdict=Verdict.UNRESOLVED;
        else verdict=Verdict.SUPPORTED;

        return new Result(ordinal,candidate.appearanceIndex(),verdict,reasons,
                supported,observedMatches,contradictions,unknown,false,false,false);
    }

    private static PublicFormBuildReconciliation.Evidence require(
            Map<PublicOpponentBuildHypotheses.Kind,
                Map<String,PublicFormBuildReconciliation.Evidence>> evidence,
            PublicOpponentBuildHypotheses.Kind kind,String id) {
        var candidate=evidence.get(kind).get(id);
        if(candidate==null || candidate.kind()!=kind)throw rejected();
        return candidate;
    }
    private static Set<String> canonical(Set<String> input){
        var out=new TreeSet<String>();
        for(String id:input) {
            if(id==null || !id.matches("[a-z0-9]{1,40}") || id.equals("unknown"))
                throw rejected();
            out.add(id);
        }
        return Collections.unmodifiableSet(out);
    }
    private static IllegalArgumentException rejected(){
        return new IllegalArgumentException("Unverified offline build candidate");
    }
}
