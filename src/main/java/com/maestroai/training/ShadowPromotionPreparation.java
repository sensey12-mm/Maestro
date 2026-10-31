package com.maestroai.training;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

import com.maestroai.training.PublicTransitionState.Known;

/**
 * Phase 4/5/6/7 Shadow-promotion PREPARATION, not promotion.
 *
 * A Phase 4 mechanics comparison is an independent conditional tactical
 * preference. It is never a calibrated opponent policy, a complete native
 * legality certificate, a Phase 5 historical identity match, or a Phase 7
 * integrated gameplay advantage. Only current owning/source receipts can
 * produce a temporary proposal, and no proposal grants action authority.
 */
public final class ShadowPromotionPreparation {
    private ShadowPromotionPreparation(){}

    public enum Verdict {
        MISSING_CURRENT_OWN_SOURCE,
        MISSING_PUBLIC_OR_MECHANICS_SOURCE,
        INCOMPLETE_OR_UNBOUNDED_ACTIONS,
        NO_STRICT_INDEPENDENT_PREFERENCE,
        CONDITIONAL_PROPOSAL_NONAUTHORITATIVE
    }

    /** Deliberately stores a source lease; never retain raw public/private
     * packet bytes, opponent guesses, hidden builds or replay content. */
    public static final class Proposal {
        private final Object battle,ownRequest;
        private final int turn;
        private final String action;
        private final BooleanSupplier current;
        private Proposal(Object battle,Object ownRequest,int turn,String action,
                         BooleanSupplier source){
            this.battle=battle;this.ownRequest=ownRequest;
            this.turn=turn;this.action=action;this.current=source;
        }
        public String action(){return action;}
        public boolean current(Object physicalBattle,Object originalRequest,int currentTurn){
            if(physicalBattle==null || physicalBattle!=battle
                    || originalRequest==null || originalRequest!=ownRequest
                    || currentTurn!=turn)return false;
            try{return current.getAsBoolean();}
            catch(RuntimeException stale){return false;}
        }
        /** Neither a conditional calculation nor a source receipt grants
         * authority to choose. Requires future explicit owner approval. */
        public boolean promotionAuthorized(){return false;}
    }
    public record Report(Verdict verdict,Proposal proposal) {
        public Report {
            Objects.requireNonNull(verdict);
            if((verdict==Verdict.CONDITIONAL_PROPOSAL_NONAUTHORITATIVE)
                    !=(proposal!=null))
                throw new IllegalArgumentException("Invalid Shadow proposal verdict");
        }
        public boolean candidatePrepared(){return proposal!=null;}
        public boolean promotionAuthorized(){return false;}
        public String safeLine(){
            return "ShadowPromotionPreparation[verdict="+verdict
                    +";proposal="+candidatePrepared()
                    +";phase7Integrated=UNPROVEN"
                    +";opponentResponseDistribution=UNKNOWN"
                    +";legacyFallback=REQUIRED;authority=NONE]";
        }
    }
    private static Report abstain(Verdict v){return new Report(v,null);}

    /**
     * Source-only, conditional preference validation. Use this function
     * for synthetic regression fixtures; it is NOT an authorization gate.
     * Every legal root action must have an independently bounded comparable
     * outcome and the winning interval must strictly dominate the rest.
     */
    static Report compare(Object physicalBattle,Object originalRequest,
                          BattleSnapshot snapshot,
                          ShadowMechanicsEvaluation.Comparison comparison,
                          BooleanSupplier source) {
        if(physicalBattle==null || originalRequest==null || snapshot==null || source==null)
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        try {
            if(!source.getAsBoolean())
                return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }catch(RuntimeException unavailable){
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }
        if(comparison==null || snapshot.legalMoves().size()+snapshot.legalSwitches().size()<2
                || !comparison.candidateCoverageComplete()
                || comparison.rankings().size()!=snapshot.legalMoves().size()
                    +snapshot.legalSwitches().size()
                || !snapshot.opponentBench().isEmpty())
            return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
        var legal=new TreeSet<String>();
        for(String move:snapshot.legalMoves()){
            if(move==null || move.isBlank() || move.length()>80)
                return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
            legal.add("move:"+move);
        }
        for(String reserve:snapshot.legalSwitches()){
            if(reserve==null || reserve.isBlank() || reserve.length()>80)
                return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
            legal.add("switch:"+reserve);
        }
        if(legal.size()!=snapshot.legalMoves().size()+snapshot.legalSwitches().size())
            return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
        var seen=new HashSet<String>();
        String preferred=comparison.preferredConditional();
        ShadowMechanicsEvaluation.Utility choice=null;
        for(var ranked:comparison.rankings()){
            if(ranked==null || !legal.contains(ranked.action())
                    || !seen.add(ranked.action())
                    || ranked.utility()==null || !ranked.utility().known()
                    || !ranked.unknown().isEmpty())
                return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
            if(ranked.action().equals(preferred))choice=ranked.utility().value();
        }
        if(!seen.equals(legal) || preferred==null || choice==null)
            return abstain(Verdict.NO_STRICT_INDEPENDENT_PREFERENCE);
        for(var ranked:comparison.rankings()){
            if(!preferred.equals(ranked.action())
                    && !(choice.low()>ranked.utility().value().high()))
                return abstain(Verdict.NO_STRICT_INDEPENDENT_PREFERENCE);
        }
        // The original currentness lease stays attached: any future action
        // materialization must recheck the SAME physical battle/request/turn.
        try{
            if(!source.getAsBoolean())
                return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }catch(RuntimeException unavailable){
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }
        return new Report(Verdict.CONDITIONAL_PROPOSAL_NONAUTHORITATIVE,
                new Proposal(physicalBattle,originalRequest,snapshot.turn(),preferred,source));
    }

    /**
     * Production bridge into existing Phase 4 source-execution and Phase 6
     * *owning* request/source leases. It does not import Phase 5 history into
     * opponent identity or allow Phase 7 synthetic results to imply readiness.
     */
    public static Report inspect(Object physicalBattle,Object ownRequest,
                                 BattleSnapshot snapshot,
                                 ShadowMechanicsEvaluation.Comparison comparison,
                                 RuntimeDomain domain,OwnRequestTransport.Lease lease,
                                 ScopedExecution execution,
                                 PublicProtocolEvidence publicEvidence) {
        if(physicalBattle==null || ownRequest==null || lease==null || snapshot==null
                || !lease.current() || lease.turn()!=snapshot.turn())
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        if(domain==null || execution==null || publicEvidence==null)
            return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        BooleanSupplier receipt=()->{
            if(!lease.current() || lease.turn()!=snapshot.turn()
                    || !domain.current() || !domain.effectiveState().known()
                    || !execution.sourceCurrent() || !execution.unresolved().isEmpty()
                    || !publicEvidence.complete() || !publicEvidence.framed()
                    || publicEvidence.corrupted() || !publicEvidence.coverageUnknown().isEmpty())
                return false;
            execution.requireCurrent(snapshot.turn());
            return lease.current() && domain.current() && execution.sourceCurrent();
        };
        try {
            if(!receipt.getAsBoolean())
                return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        }catch(RuntimeException unsupported){
            return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        }
        return compare(physicalBattle,ownRequest,snapshot,comparison,receipt);
    }
}
