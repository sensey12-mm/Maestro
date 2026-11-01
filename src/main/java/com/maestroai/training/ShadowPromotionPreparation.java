package com.maestroai.training;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        MISSING_OR_STALE_NATIVE_LEGAL_WINDOW,
        INSUFFICIENT_PUBLIC_REPLY_OR_MODIFIER_COVERAGE,
        INCOMPLETE_OR_UNBOUNDED_ACTIONS,
        NO_STRICT_INDEPENDENT_PREFERENCE,
        CONDITIONAL_PROPOSAL_NONAUTHORITATIVE,
        INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE,
        INDEPENDENT_FORCED_REPLACEMENT_NONAUTHORITATIVE
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
            if((verdict==Verdict.CONDITIONAL_PROPOSAL_NONAUTHORITATIVE
                    || verdict==Verdict.INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE
                    || verdict==Verdict.INDEPENDENT_FORCED_REPLACEMENT_NONAUTHORITATIVE)
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
     * Source-owned independent live policy. Allows a fully disclosed
     * heuristic when the conditional damage comparator cannot prove a global
     * best action. Unlike the strict Phase 4 comparator this makes no claim
     * of bounded matchup superiority, status success or response probability.
     * Real selection remains blocked by LivePromotionGate until authorized.
     */
    public static Report inspectIndependent(Object battle,Object request,
            BattleSnapshot snapshot,ShadowLiveDecisionEngine.Choice chosen,
            OwnRequestTransport.Lease lease,ShadowNativeLegalWindow.Proof nativeRoot,
            PublicProtocolEvidence publicHistory,String publicOpponentSlot) {
        if(battle==null || request==null || lease==null || snapshot==null
                || !lease.current() || lease.turn()!=snapshot.turn())
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        if(chosen==null || nativeRoot==null
                || !nativeRoot.exactlyCovers(battle,request,snapshot))
            return abstain(Verdict.MISSING_OR_STALE_NATIVE_LEGAL_WINDOW);
        if(publicHistory==null || !publicHistory.complete()
                || !publicHistory.framed() || publicHistory.corrupted()
                || !publicHistory.coverageUnknown().isEmpty()
                || publicHistory.publicTurn()!=snapshot.turn()
                || publicOpponentSlot==null || publicOpponentSlot.isBlank()
                || publicHistory.reentryAmbiguous(publicOpponentSlot)
                || !snapshot.opponentBench().isEmpty())
            return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        var legal=new HashSet<String>();
        for(String move:snapshot.legalMoves()) legal.add("move:"+move);
        for(String reserve:snapshot.legalSwitches()) legal.add("switch:"+reserve);
        if(legal.size()!=snapshot.legalMoves().size()
                    +snapshot.legalSwitches().size()
                || !legal.contains(chosen.action()))
            return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
        BooleanSupplier current=()->lease.current()
                && lease.turn()==snapshot.turn()
                && nativeRoot.exactlyCovers(battle,request,snapshot)
                && publicHistory.complete() && publicHistory.framed()
                && !publicHistory.corrupted()
                && publicHistory.coverageUnknown().isEmpty()
                && publicHistory.publicTurn()==snapshot.turn();
        try {
            if(!current.getAsBoolean())
                return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }catch(RuntimeException stale){
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }
        return new Report(Verdict.INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE,
                new Proposal(battle,request,snapshot.turn(),chosen.action(),current));
    }

    /**
     * A conservative first-party-only position. When public opponent history
     * is incomplete, the independently owned request still permits a legal
     * attack, recovery, setup or emergency switch. No stale public type,
     * current identity, revealed move or hidden ability enters the policy.
     */
    public static BattleSnapshot firstPartyOnly(BattleSnapshot observed) {
        if(observed==null)return null;
        var unknownOpponent=new BattleSnapshot.Combatant(
                "UNVERIFIED",BattleSnapshot.UNKNOWN_HP_FRACTION,
                List.of(),Map.of(),List.of(),null,null,false);
        return new BattleSnapshot(observed.turn(),observed.self(),unknownOpponent,
                observed.selfBench(),List.of(),observed.legalMoves(),
                observed.legalSwitches(),Map.of(),false,null);
    }

    /**
     * Only native-legal moves whose verified effect requires NO opponent
     * hidden identity/last move/status may be chosen without public opponent
     * evidence. This does not certify an absent opposing hazard or guarantee
     * a protect outcome.
     */
    public static boolean sourceIndependentStatus(ShadowLiveDecisionEngine.Choice selected) {
        if(selected==null || selected.reason()!=ShadowLiveDecisionEngine.Reason.STATUS
                || !selected.action().startsWith("move:"))return false;
        String id=selected.action().substring(5);
        var effect=MoveEffectModel.describe(id,"unknown","status",0,100,0).effect();
        return effect==MoveEffectModel.Effect.SET_HAZARD
                || effect==MoveEffectModel.Effect.PROTECT;
    }

    /**
     * Promotion with ONLY owned state and a native legal-root lease.
     * Target-dependent status effects still require confirmed public
     * evidence; only verified field/self status may use the owned-only path.
     * This path is not evidence that an unknown matchup is advantageous.
     */
    public static Report inspectOwnOnly(Object battle,Object request,
            BattleSnapshot owned,ShadowLiveDecisionEngine.Choice chosen,
            OwnRequestTransport.Lease lease,ShadowNativeLegalWindow.Proof nativeRoot) {
        if(battle==null || request==null || owned==null || lease==null
                || !lease.current() || lease.turn()!=owned.turn())
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        if(nativeRoot==null || !nativeRoot.exactlyCovers(battle,request,owned))
            return abstain(Verdict.MISSING_OR_STALE_NATIVE_LEGAL_WINDOW);
        var opposing=owned.opponent();
        if(!"UNVERIFIED".equals(opposing.species())
                || opposing.hpKnown() || !opposing.types().isEmpty()
                || !opposing.knownBoosts().isEmpty()
                || !opposing.revealedMoves().isEmpty()
                || opposing.revealedItem()!=null || opposing.revealedAbility()!=null
                || !owned.opponentBench().isEmpty() || owned.opponentTerastallized()
                || !owned.knownOpponentMoveCounts().isEmpty()
                || chosen==null
                || chosen.reason()==ShadowLiveDecisionEngine.Reason.STATUS
                    && !sourceIndependentStatus(chosen)
                || chosen.reason()==ShadowLiveDecisionEngine.Reason.UNKNOWN_OR_UNSUPPORTED
                || chosen.reason()==ShadowLiveDecisionEngine.Reason.NO_LEGAL_ACTION)
            return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        var legal=new java.util.HashSet<String>();
        for(var move:owned.legalMoves())legal.add("move:"+move);
        for(var reserve:owned.legalSwitches())legal.add("switch:"+reserve);
        if(legal.size()!=owned.legalMoves().size()+owned.legalSwitches().size()
                || !legal.contains(chosen.action()))
            return abstain(Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS);
        BooleanSupplier current=()->lease.current() && lease.turn()==owned.turn()
                && nativeRoot.exactlyCovers(battle,request,owned);
        try{
            if(!current.getAsBoolean())return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }catch(RuntimeException stale){return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);}
        return new Report(Verdict.INDEPENDENT_LIVE_POLICY_NONAUTHORITATIVE,
                new Proposal(battle,request,owned.turn(),chosen.action(),current));
    }

    /**
     * Forced replacement is deliberately separate: no active opposing
     * matchup, invented next request or ordinary move list is required.
     * The caller must attest the original owning native forced request and
     * all eligible own reserves. A fresh physical request/turn is mandatory.
     */
    public static Report inspectForced(Object battle,Object request,int turn,
            ShadowLiveDecisionEngine.Choice chosen,
            OwnRequestTransport.Lease lease,BooleanSupplier nativeForcedCurrent) {
        if(battle==null || request==null || lease==null || chosen==null
                || chosen.reason()!=ShadowLiveDecisionEngine.Reason.FORCED_REPLACEMENT
                || !chosen.action().startsWith("switch:")
                || nativeForcedCurrent==null || !lease.current()
                || lease.turn()!=turn)
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        BooleanSupplier current=()->lease.current() && lease.turn()==turn
                && nativeForcedCurrent.getAsBoolean();
        try {
            if(!current.getAsBoolean())
                return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }catch(RuntimeException invalid){
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        }
        return new Report(Verdict.INDEPENDENT_FORCED_REPLACEMENT_NONAUTHORITATIVE,
                new Proposal(battle,request,turn,chosen.action(),current));
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
                                 PublicProtocolEvidence publicEvidence,
                                 ShadowNativeLegalWindow.Proof legalWindow,
                                 PublicReplyCoverage.Enveloped envelope) {
        if(physicalBattle==null || ownRequest==null || lease==null || snapshot==null
                || !lease.current() || lease.turn()!=snapshot.turn())
            return abstain(Verdict.MISSING_CURRENT_OWN_SOURCE);
        if(domain==null || execution==null || publicEvidence==null)
            return abstain(Verdict.MISSING_PUBLIC_OR_MECHANICS_SOURCE);
        if(legalWindow==null || !legalWindow.exactlyCovers(
                physicalBattle,ownRequest,snapshot))
            return abstain(Verdict.MISSING_OR_STALE_NATIVE_LEGAL_WINDOW);
        // This separate Phase 4/6 comparison covers ALL currently witnessed
        // public replies and the certified current modifier envelope.
        // It never pretends unseen opponent replies have probability zero.
        if(envelope==null || envelope.replies()==null
                || !envelope.replies().compared()
                || !envelope.replies().allCandidates()
                || !envelope.replies().reasons().isEmpty()
                || envelope.replies().result()!=comparison
                || envelope.replies().replyCount()<1
                || envelope.modifiers()==null
                || !envelope.modifiers().complete()
                || envelope.modifiers().preferredConditional()==null
                || !envelope.modifiers().preferredConditional().equals(
                        comparison.preferredConditional())
                || envelope.pairwise()==null
                || !envelope.pairwise().fullRootBounded()
                || !envelope.pairwise().reasons().isEmpty())
            return abstain(Verdict.INSUFFICIENT_PUBLIC_REPLY_OR_MODIFIER_COVERAGE);
        BooleanSupplier receipt=()->{
            if(!legalWindow.exactlyCovers(physicalBattle,ownRequest,snapshot))
                return false;
            if(!lease.current() || lease.turn()!=snapshot.turn()
                    || !domain.current() || !domain.effectiveState().known()
                    || !execution.sourceCurrent() || !execution.unresolved().isEmpty()
                    || !publicEvidence.complete() || !publicEvidence.framed()
                    || publicEvidence.corrupted() || !publicEvidence.coverageUnknown().isEmpty())
                return false;
            execution.requireCurrent(snapshot.turn());
            return lease.current() && domain.current() && execution.sourceCurrent()
                    && legalWindow.exactlyCovers(physicalBattle,ownRequest,snapshot);
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
