package com.maestroai.training;

import java.util.*;
import java.util.function.BooleanSupplier;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.ShadowMechanicsEvaluation.*;

/**
 * Separate, source-conditional pairwise certificates from a complete legal-root
 * comparison. A bounded *subset* is not a global best action: the excluded
 * root candidates might be better and opposing unobserved moves are unknown.
 * Never use this diagnostic to select a Legacy move or certify hidden state.
 */
public final class TacticalPairwiseCoverage {
    private TacticalPairwiseCoverage() {}

    public enum Reason {
        SOURCE_EXPIRED, OPPONENT_MODIFIER_DOMAIN_UNKNOWN,
        PUBLIC_REPLIES_OR_ROOT_UNVERIFIED, NO_CERTIFIED_ACTION_PAIR
    }

    public record Audit(int legalActions, int boundedActions, int comparedPairs,
                        int strictPairs, int attackPairs, int attackSetupPairs,
                        int staySwitchPairs, boolean fullRootBounded,
                        Set<Reason> reasons) {
        public Audit {
            reasons=Set.copyOf(reasons);
            if(legalActions<0 || boundedActions<0 || boundedActions>legalActions
                    || strictPairs>comparedPairs || strictPairs<0 || comparedPairs<0)
                throw new IllegalArgumentException("invalid pairwise audit");
        }
        /** Counters and fixed source-safe vocabulary only: no moves, player IDs,
         * items, abilities, request payloads or inferred probabilities. */
        public String safeLine() {
            return "Phase352Pairwise legalActions="+legalActions
                +" boundedActions="+boundedActions
                +" pairs="+comparedPairs
                +" strictPairs="+strictPairs
                +" attackPairs="+attackPairs
                +" attackSetupPairs="+attackSetupPairs
                +" staySwitchPairs="+staySwitchPairs
                +" fullRootBounded="+fullRootBounded
                +" partialConditional=true"
                +" witnessedReplyOnly=true"
                +" unresolved="+new TreeSet<>(reasons)
                +" globalPreferred=UNDETERMINED hiddenModifierProbability=NONE"
                +" advisoryOnly=true livePromotion=false";
        }
    }

    private enum Kind { ATTACK, SETUP, SWITCH, UNKNOWN }
    private static Kind kind(String action) {
        if(action==null) return Kind.UNKNOWN;
        if(action.startsWith("switch:") && action.length()>7) return Kind.SWITCH;
        if(!action.startsWith("move:")) return Kind.UNKNOWN;
        var move=move(action.substring(5));
        return move==null ? Kind.UNKNOWN : move.power()>0 ? Kind.ATTACK : Kind.SETUP;
    }

    private static Audit none(Reason why, int actions) {
        return new Audit(actions,0,0,0,0,0,0,false,Set.of(why));
    }
    private static boolean current(BooleanSupplier sourceCurrent) {
        try{return sourceCurrent.getAsBoolean();}
        catch(RuntimeException invalid){return false;}
    }
    private static boolean bounded(Ranking rank, int replyCount) {
        if(rank==null || rank.action()==null || kind(rank.action())==Kind.UNKNOWN
                || rank.utility()==null || !rank.utility().known() || !rank.unknown().isEmpty()
                || rank.lines()==null || rank.lines().size()<replyCount) return false;
        Utility bounds=rank.utility().value();
        for(Line line:rank.lines()) {
            if(line==null || line.utility()==null || !line.utility().known()
                    || !line.unknown().isEmpty()
                    || bounds.low()>line.utility().value().low()
                    || bounds.high()<line.utility().value().high()) return false;
        }
        return true;
    }
    public static Audit inspect(PublicReplyCoverage.Audit audit,
                                Known<Boolean> currentModifierDomain,
                                BooleanSupplier sourceCurrent) {
        Objects.requireNonNull(currentModifierDomain);
        Objects.requireNonNull(sourceCurrent);
        int legal=audit==null?0:audit.candidateCount();
        if(!current(sourceCurrent)) return none(Reason.SOURCE_EXPIRED,legal);
        if(!currentModifierDomain.known() || !Boolean.TRUE.equals(currentModifierDomain.value()))
            return none(Reason.OPPONENT_MODIFIER_DOMAIN_UNKNOWN,legal);
        if(audit==null || !audit.compared() || !audit.allCandidates() || audit.result()==null
                || !audit.result().candidateCoverageComplete()
                || audit.replyCount()<1 || audit.result().rankings().size()!=legal
                || !audit.result().evidence().contains("advisoryOnly=true"))
            return none(Reason.PUBLIC_REPLIES_OR_ROOT_UNVERIFIED,legal);
        List<Ranking> valid=new ArrayList<>();
        Set<String> uniqueActions=new HashSet<>();
        for(Ranking rank:audit.result().rankings()) {
            if(rank==null || rank.action()==null || !uniqueActions.add(rank.action()))
                return none(Reason.PUBLIC_REPLIES_OR_ROOT_UNVERIFIED,legal);
            if(bounded(rank,audit.replyCount())) valid.add(rank);
        }
        int compared=0,strict=0,attack=0,attackSetup=0,staySwitch=0;
        for(int i=0;i<valid.size();i++)for(int j=i+1;j<valid.size();j++){
            Ranking a=valid.get(i), b=valid.get(j);
            Kind ak=kind(a.action()),bk=kind(b.action());
            compared++;
            if(ak==Kind.ATTACK && bk==Kind.ATTACK) attack++;
            if((ak==Kind.ATTACK && bk==Kind.SETUP)||(ak==Kind.SETUP && bk==Kind.ATTACK))
                attackSetup++;
            if((ak==Kind.SWITCH && bk!=Kind.SWITCH)||(bk==Kind.SWITCH && ak!=Kind.SWITCH))
                staySwitch++;
            if(a.utility().value().low()>b.utility().value().high()
                    || b.utility().value().low()>a.utility().value().high()) strict++;
        }
        if(!current(sourceCurrent)) return none(Reason.SOURCE_EXPIRED,legal);
        return new Audit(legal, valid.size(), compared,strict,attack,attackSetup,staySwitch,
                valid.size()==legal,
                compared==0?Set.of(Reason.NO_CERTIFIED_ACTION_PAIR):Set.of());
    }
}
