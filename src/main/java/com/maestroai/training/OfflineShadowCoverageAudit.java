package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Phase 6.4: offline synthetic scenario coverage and conditional sensitivity.
 * NEVER a probability, policy preference, installed-game effect certificate,
 * complete opponent response set or live/Shadow tactical adapter.
 */
public final class OfflineShadowCoverageAudit {
    private OfflineShadowCoverageAudit() {}
    public static final String SCHEMA="phase6.4.offline-coverage-audit.v1";
    public enum Abstention {
        OFFLINE_FIXTURE_NOT_AUTHENTICATED,
        OPPONENT_RESPONSE_SET_OPEN,
        NO_CALIBRATED_PROBABILITIES,
        INSTALLED_EFFECTS_UNVERIFIED,
        PHASE5_HISTORY_NOT_CURRENT_IDENTITY,
        PROFILE_VALUES_ARE_CONDITIONAL_ASSUMPTIONS,
        OWN_ACTION_COVERAGE_INCOMPLETE,
        WITNESSED_RESPONSE_COVERAGE_INCOMPLETE,
        NO_WITNESSED_RESPONSES,
        NO_CONDITIONAL_CASES,
        EFFECTS_OR_BRANCH_MAPPING_UNRESOLVED,
        CASE_BUDGET_EXCEEDED,
        CONDITIONALLY_RESOLVED_NOT_TACTICAL
    }
    public record Input(String schema, OfflineShadowResponseScenarios.Input scenarios,
                        Map<String,CobblemonMechanics.Profile> profiles,
                        Known<Boolean> trickRoom, CobblemonMechanics.Critical critical) {
        public Input {
            if (!SCHEMA.equals(schema) || scenarios==null || profiles==null
                    || trickRoom==null || critical==null
                    || !profiles.keySet().equals(Set.of("self","opponent")))
                throw invalid();
            profiles=Map.copyOf(profiles);
            if (!"self".equals(profiles.get("self").actor())
                    || !"opponent".equals(profiles.get("opponent").actor()))
                throw invalid();
        }
    }
    /** An interval ONLY over the listed witnessed source-conditioned moves.
     * Null means no fully resolved interval, NEVER neutral or 0 damage. */
    public record OwnAction(String action, int witnessedResponses,
                            int modeledCases, int resolvedCases,
                            Double conditionalWorstLow, Double conditionalBestHigh,
                            Set<String> unresolvedEffects) {
        public OwnAction {
            if (action==null || !action.matches("(move|switch):[a-z0-9]{1,48}")
                    || witnessedResponses<0 || modeledCases<0 || resolvedCases<0
                    || resolvedCases>modeledCases || modeledCases>witnessedResponses
                    || unresolvedEffects==null
                    || (conditionalWorstLow==null)!=(conditionalBestHigh==null)
                    || conditionalWorstLow!=null && (
                          !Double.isFinite(conditionalWorstLow)
                          || !Double.isFinite(conditionalBestHigh)
                          || conditionalWorstLow>conditionalBestHigh
                          || resolvedCases!=witnessedResponses))
                throw invalid();
            unresolvedEffects=Set.copyOf(unresolvedEffects);
        }
        public int unmodeledWitnessedCases() { return witnessedResponses-modeledCases; }
        public int unresolvedModeledCases() { return modeledCases-resolvedCases; }
        public boolean conditionalIntervalResolved() { return conditionalWorstLow!=null; }
    }
    public static final class Report {
        private final List<OwnAction> actions;
        private final Set<Abstention> reasons;
        private final int own,sourceOwn,witnessed,sourceWitnessed,fixturePairs,modeled,resolved;
        private Report(List<OwnAction> actions,Set<Abstention> reasons,
                       int own,int sourceOwn,int witnessed,int sourceWitnessed,
                       int fixturePairs,int modeled,int resolved) {
            this.actions=List.copyOf(actions);
            this.reasons=Collections.unmodifiableSet(EnumSet.copyOf(reasons));
            this.own=own;this.sourceOwn=sourceOwn;this.witnessed=witnessed;
            this.sourceWitnessed=sourceWitnessed;
            this.fixturePairs=fixturePairs;this.modeled=modeled;this.resolved=resolved;
        }
        public String schema(){return SCHEMA;}
        public String mechanicsScope(){return CobblemonMechanics.SOURCE;}
        public List<OwnAction> actions(){return actions;}
        public Set<Abstention> abstentions(){return reasons;}
        public int fixtureLegalOwnActions(){return own;}
        public int sourceListedOwnMoves(){return sourceOwn;}
        public int witnessedReplyIds(){return witnessed;}
        public int sourceListedWitnessedReplies(){return sourceWitnessed;}
        /** Denominator is a synthetic fixture product, NOT all possible replies. */
        public int fixturePairCount(){return fixturePairs;}
        public int modeledPairCount(){return modeled;}
        public int conditionallyResolvedPairCount(){return resolved;}
        public int unresolvedFixturePairCount(){return fixturePairs-resolved;}
        public boolean fixturePairsAllModeled(){return fixturePairs>0&&modeled==fixturePairs;}
        public boolean modeledEffectsAllResolved(){return modeled>0&&resolved==modeled;}
        public boolean opponentResponseSetComplete(){return false;}
        public boolean installedEffectsCertified(){return false;}
        public boolean calibratedResponseProbabilities(){return false;}
        public boolean recommendedActionAvailable(){return false;}
        public boolean phase5IdentityBound(){return false;}
        public boolean currentBattleBound(){return false;}
        public boolean legalActionsRuntimeAttested(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase64OfflineCoverage[fixturePairs="+fixturePairs+";modeled="+modeled
                    +";resolved="+resolved+";abstentions="+reasons+";no-tactics]";
        }
    }
    /** Recomputes Phase 6.2 provenance, Phase 6.3 witness set and Phase 4 root.
     * The caller cannot supply a favorable Phase 6.3 report or a comparison. */
    public static Report inspect(Input input) {
        Objects.requireNonNull(input);
        var request=input.scenarios();
        var scenarios=OfflineShadowResponseScenarios.construct(request);
        var root=PublicStateSearchAdapter.project(request.snapshot(),request.mechanics())
                .states().get("root");
        var legal=new TreeSet<String>();
        root.legalMoves().forEach(m->legal.add("move:"+m));
        root.legalSwitches().forEach(m->legal.add("switch:"+m));
        int witnessed=scenarios.witnessedReplyCount();
        int total=Math.multiplyExact(legal.size(),witnessed);
        int modeled=scenarios.conditionalCases().size();
        if(modeled>total)throw invalid();
        int supportedOwn=(int)legal.stream().filter(a->a.startsWith("move:")
                && CobblemonMechanics.move(a.substring(5))!=null).count();
        int supportedReplies=(int)request.snapshot().opponent().revealedMoves()
                .stream().filter(m->CobblemonMechanics.move(m)!=null).count();
        var reasons=EnumSet.of(Abstention.OFFLINE_FIXTURE_NOT_AUTHENTICATED,
                Abstention.OPPONENT_RESPONSE_SET_OPEN,
                Abstention.NO_CALIBRATED_PROBABILITIES,
                Abstention.INSTALLED_EFFECTS_UNVERIFIED,
                Abstention.PHASE5_HISTORY_NOT_CURRENT_IDENTITY,
                Abstention.PROFILE_VALUES_ARE_CONDITIONAL_ASSUMPTIONS,
                Abstention.CONDITIONALLY_RESOLVED_NOT_TACTICAL);
        if(supportedOwn<legal.size())
            reasons.add(Abstention.OWN_ACTION_COVERAGE_INCOMPLETE);
        if(supportedReplies<witnessed)
            reasons.add(Abstention.WITNESSED_RESPONSE_COVERAGE_INCOMPLETE);
        if(witnessed==0)reasons.add(Abstention.NO_WITNESSED_RESPONSES);
        if(modeled==0)reasons.add(Abstention.NO_CONDITIONAL_CASES);
        if(modeled<total){
            reasons.add(Abstention.OWN_ACTION_COVERAGE_INCOMPLETE);
            reasons.add(Abstention.WITNESSED_RESPONSE_COVERAGE_INCOMPLETE);
        }
        if(scenarios.abstentions().contains(
                OfflineShadowResponseScenarios.Abstention.REPLY_BUDGET_EXCEEDED)
            || scenarios.abstentions().contains(
                OfflineShadowResponseScenarios.Abstention.OWN_ACTION_BUDGET_EXCEEDED))
            reasons.add(Abstention.CASE_BUDGET_EXCEEDED);

        Map<String,ShadowMechanicsEvaluation.Ranking> rankings=Map.of();
        if(!scenarios.conditionalPlans().isEmpty()) {
            // Only conditional line intervals are consumed; the Phase 4
            // preferredConditional, baseline and policy values are discarded.
            var comparison=ShadowMechanicsEvaluation.compare(request.snapshot(),
                    request.mechanics(),input.profiles(),
                    scenarios.conditionalPlans(),input.trickRoom(),input.critical(),null);
            var values=new TreeMap<String,ShadowMechanicsEvaluation.Ranking>();
            for(var ranking:comparison.rankings())
                if(values.put(ranking.action(),ranking)!=null)throw invalid();
            rankings=Map.copyOf(values);
        }
        List<OwnAction> byAction=new ArrayList<>();
        int resolved=0;
        for(String action:legal) {
            int count=(int)scenarios.conditionalCases().stream()
                    .filter(c->action.equals(c.ownAction())).count();
            var rank=rankings.get(action);
            int known=0;
            Double low=null,high=null;
            Set<String> unknown=new TreeSet<>();
            if(rank!=null) {
                // A comparison may branch on hit/miss/order. Without an
                // individually signed branch-to-witness mapping, group counting
                // is unsafe. Require exactly one line per declared reply.
                if(rank.lines().size()!=count)
                    unknown.add("BRANCH_TO_WITNESSED_REPLY_NOT_CERTIFIED");
                else {
                    for(var line:rank.lines()) {
                        if(!line.utility().known() || !line.unknown().isEmpty())
                            unknown.addAll(line.unknown());
                        else {
                            known++;
                            var utility=line.utility().value();
                            low=low==null?utility.low():Math.min(low,utility.low());
                            high=high==null?utility.high():Math.max(high,utility.high());
                        }
                    }
                }
            }
            if(count!=witnessed || known!=witnessed){low=null;high=null;}
            if(count!=known)
                reasons.add(Abstention.EFFECTS_OR_BRANCH_MAPPING_UNRESOLVED);
            resolved+=known;
            byAction.add(new OwnAction(action,witnessed,count,known,low,high,unknown));
        }
        return new Report(byAction,reasons,legal.size(),supportedOwn,witnessed,
                supportedReplies,total,modeled,resolved);
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.4 rejects unpinned or inconsistent offline audit");
    }
}
