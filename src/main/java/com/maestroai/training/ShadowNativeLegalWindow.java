package com.maestroai.training;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Independent owning-native legal-root witness for promotion preparation.
 * Captured separately from the advisory BattleSnapshot, from the installed
 * single-actor request, usable move list and eligible own party. It does NOT
 * certify hypothetical opponent responses or special actions absent in the
 * original native request. Missing/uncertain request coverage must abstain.
 */
public final class ShadowNativeLegalWindow {
    private ShadowNativeLegalWindow(){}

    public static final class Proof {
        private final Object battle,request;
        private final int turn;
        private final Set<String> actions;
        private final boolean forced,trapped,continuation;
        private final BooleanSupplier live;
        private Proof(Object battle,Object request,int turn,Set<String> actions,
                boolean forced,boolean trapped,boolean continuation,BooleanSupplier live){
            this.battle=battle;this.request=request;this.turn=turn;
            this.actions=Set.copyOf(actions);
            this.forced=forced;this.trapped=trapped;
            this.continuation=continuation;this.live=live;
        }
        public boolean current(Object originalBattle,Object originalRequest,int originalTurn){
            if(originalBattle==null || originalBattle!=battle || originalRequest==null
                    || originalRequest!=request || originalTurn!=turn)return false;
            try {return live.getAsBoolean();}
            catch(RuntimeException absent){return false;}
        }
        public boolean exactlyCovers(Object originalBattle,Object originalRequest,
                                     BattleSnapshot snapshot){
            if(snapshot==null || !current(originalBattle,originalRequest,snapshot.turn()))
                return false;
            if(forced || snapshot.legalMoves().isEmpty()
                    || trapped && !snapshot.legalSwitches().isEmpty()
                    || continuation && !snapshot.legalSwitches().isEmpty())
                return false;
            Set<String> projected=new HashSet<>();
            for(String move:snapshot.legalMoves()){
                if(move==null || !projected.add("move:"+move))return false;
            }
            for(String target:snapshot.legalSwitches()){
                if(target==null || !projected.add("switch:"+target))return false;
            }
            return actions.equals(projected) &&
                    current(originalBattle,originalRequest,snapshot.turn());
        }
        /** A partial native request never becomes an action certificate. */
        public boolean promotionAuthorized(){return false;}
    }

    /**
     * The caller must collect these from the currently owned native request
     * and the SAME owning actor before Legacy chooses; never from Shadow.
     * Complete means all ordinary executable roots were independently
     * inspected, not that hidden opponent replies are known.
     */
    public static Proof capture(Object battle,Object request,int turn,
            List<String> nativeUsableMoves,List<String> nativeEligibleSwitches,
            boolean forceSwitch,boolean trapped,boolean continuation,
            boolean complete,BooleanSupplier live){
        if(battle==null || request==null || turn<0 || turn>10000 || !complete
                || live==null || nativeUsableMoves==null || nativeEligibleSwitches==null
                || nativeUsableMoves.size()>16 || nativeEligibleSwitches.size()>6)
            return null;
        // Unsupported forced-replacement requests and continuation are
        // categorically outside today's independently modeled comparison.
        if(forceSwitch || continuation || nativeUsableMoves.isEmpty())
            return null;
        if(trapped && !nativeEligibleSwitches.isEmpty())return null;
        Set<String> actions=new HashSet<>();
        for(String id:nativeUsableMoves){
            if(id==null || id.isBlank() || id.length()>80
                    || !actions.add("move:"+id))return null;
        }
        for(String id:nativeEligibleSwitches){
            if(id==null || id.isBlank() || id.length()>80
                    || !actions.add("switch:"+id))return null;
        }
        try{if(!live.getAsBoolean())return null;}
        catch(RuntimeException missing){return null;}
        return new Proof(battle,request,turn,actions,
                forceSwitch,trapped,continuation,live);
    }
}
