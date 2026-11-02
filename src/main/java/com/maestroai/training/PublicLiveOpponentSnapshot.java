package com.maestroai.training;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Shadow Promotion 5: a non-authoritative public-only input projection.
 * Reuses Phase 6.16 routing, Phase 6.29 continuity and Phase 4 public mechanics.
 * Public appearance is NOT actual Pokemon identity (Illusion).
 */
public final class PublicLiveOpponentSnapshot {
    private PublicLiveOpponentSnapshot() {}

    public static final class Capture {
        private final BattleSnapshot snapshot;
        private final Object battle;
        private final PublicSplitRoutingGate.Report routing;
        private final PublicOpponentIdentityContinuity.Report continuity;
        private final Supplier<List<String>> originalHistory;
        private final BooleanSupplier nativeOwnCurrent;
        private Capture(BattleSnapshot snapshot,Object battle,
                PublicSplitRoutingGate.Report routing,
                PublicOpponentIdentityContinuity.Report continuity,
                Supplier<List<String>> history,BooleanSupplier own) {
            this.snapshot=snapshot;this.battle=battle;this.routing=routing;
            this.continuity=continuity;this.originalHistory=history;
            this.nativeOwnCurrent=own;
        }
        public BattleSnapshot snapshot(){return snapshot;}
        public boolean current(){
            try {
                return nativeOwnCurrent.getAsBoolean() && routing.current()
                    && routing.sameBattle(battle)
                    && continuity.current()
                    && continuity.samePhysicalBattle(battle)
                    && continuity.sharesOriginalRouting(battle,routing)
                    && routing.matchesOriginalHistory(battle,originalHistory.get());
            }catch(RuntimeException unavailable){return false;}
        }
        /** Never emit public nicknames, UUIDs, source messages or opponent moves. */
        public String safeLine(){
            return "ShadowPublicOpponent[source=PHASE616_629_PUBLIC_MECHANICS"
                +";types="+(snapshot.opponent().types().isEmpty()?"UNKNOWN":"EXPLICIT")
                +";hp="+(snapshot.opponent().hpKnown()?"PUBLIC_DISPLAY":"UNKNOWN")
                +";identity="+("UNVERIFIED".equals(snapshot.opponent().species())
                    ?"UNKNOWN":"EXPLICIT")+";authority=NONE]";
        }
    }

    /**
     * Caller must independently establish own native actor and Showdown side,
     * and bind ownerCurrent to the SAME choice/request/physical battle.
     * No opponent Cobblemon Pokemon or sideupdate request enters this API.
     */
    public static Capture capture(Object battle,String ownSide,int turn,
            BattleSnapshot ownOnly,boolean pinned,
            Supplier<List<String>> originalHistory,BooleanSupplier ownerCurrent){
        if(battle==null || ownOnly==null || turn<1 || ownOnly.turn()!=turn
                || !Set.of("p1","p2").contains(ownSide) || !pinned
                || originalHistory==null || ownerCurrent==null)return null;
        try {
            if(!ownerCurrent.getAsBoolean())return null;
            List<String> history=List.copyOf(originalHistory.get());
            var routing=new PublicSplitRoutingGate.Session().inspect(battle,history,true);
            if(!routing.current() || !routing.sameBattle(battle)
                    || !routing.matchesOriginalHistory(battle,history))return null;
            var continuity=new PublicOpponentIdentityContinuity.Session()
                    .inspect(battle,routing,history);
            // A reused public POSITION does not bind persistent Pokemon
            // identity. An explicitly witnessed current type is different.
            if(!continuity.current() || !continuity.uncertainty().stream()
                    .allMatch(risk->risk==PublicOpponentIdentityContinuity.Uncertainty.PUBLIC_DISPLAY_ONLY
                        || risk==PublicOpponentIdentityContinuity.Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY))
                return null;
            var publicEvents=new PublicProtocolEvidence();
            publicEvents.update(history);
            if(!publicEvents.complete() || !publicEvents.framed()
                    || publicEvents.corrupted() || !publicEvents.coverageUnknown().isEmpty()
                    || publicEvents.publicTurn()!=routing.publicTurn()
                    || (routing.publicTurn()!=turn && routing.publicTurn()!=turn-1))
                return null;
            String foe=ownSide.equals("p1")?"p2a":"p1a";
            boolean reused=publicEvents.reentryAmbiguous(foe)
                    || continuity.uncertainty().contains(
                        PublicOpponentIdentityContinuity.Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY);
            var mechanics=new PublicMechanicsEvidence(ignored->null);
            mechanics.update(publicEvents);
            var observed=mechanics.observation(foe);
            if(observed==null || observed.ambiguous())return null;
            // A named switch could be Illusion. Only positive public revelations
            // can supply real species and ONLY explicit type announcements can
            // supply current attack-effectiveness typing.
            String species=!reused && observed.confirmedSpecies().known()
                    && !observed.unsupportedEffects()
                    ?observed.confirmedSpecies().value():"UNVERIFIED";
            List<String> types=observed.types().known()
                    ?observed.types().value():List.of();
            if(reused && types.isEmpty())return null;
            var hpEvidence=publicEvents.hp(foe);
            double hp=!reused && hpEvidence.known()
                    ?Math.min(1.0,Math.max(0.0,hpEvidence.value().midpoint()))
                    :BattleSnapshot.UNKNOWN_HP_FRACTION;
            var revealed=publicMoves(publicEvents.mechanicsEvents(),foe);
            String item=observed.item().known()?observed.item().value():null;
            String ability=observed.ability().known()?observed.ability().value():null;
            if(isFainted(publicEvents.mechanicsEvents(),foe))return null;
            if(types.isEmpty() && hp==BattleSnapshot.UNKNOWN_HP_FRACTION
                    && "UNVERIFIED".equals(species) && revealed.isEmpty()
                    && item==null && ability==null)return null;
            boolean tera=isTera(publicEvents.mechanicsEvents(),foe)
                    && types.size()==1;
            var opponent=new BattleSnapshot.Combatant(species,hp,types,
                    Map.of(),revealed,item,ability,false);
            var snapshot=new BattleSnapshot(turn,ownOnly.self(),opponent,
                    ownOnly.selfBench(),List.of(),ownOnly.legalMoves(),
                    ownOnly.legalSwitches(),Map.of(),tera,tera?types.getFirst():null);
            var bound=new Capture(snapshot,battle,routing,continuity,
                    originalHistory,ownerCurrent);
            return bound.current()?bound:null;
        }catch(RuntimeException invalid){return null;}
    }

    private static boolean target(String[] e,String slot){
        return e.length>2 && e[2].startsWith(slot+": ");
    }
    private static List<String> publicMoves(List<String> events,String slot){
        var result=new ArrayList<String>();var seen=new HashSet<String>();
        boolean active=false;
        for(String line:events){
            String[] p=line.split("\\|",-1);
            if(!target(p,slot))continue;
            if(Set.of("switch","drag").contains(p[1])){
                active=true;result.clear();seen.clear();
            }else if(Set.of("replace","-transform","detailschange",
                    "-formechange","faint").contains(p[1])){
                active=false;result.clear();seen.clear();
            }else if(active && p[1].equals("move") && p.length>3){
                String id=p[3].toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]","");
                if(!id.isEmpty() && id.length()<=64 && seen.add(id))result.add(id);
            }
        }
        return List.copyOf(result);
    }
    private static boolean isTera(List<String> events,String slot){
        boolean active=false,tera=false;
        for(String line:events){
            String[] p=line.split("\\|",-1);
            if(!target(p,slot))continue;
            if(Set.of("switch","drag","replace","-transform",
                    "detailschange","-formechange").contains(p[1])){
                active=p[1].equals("switch")||p[1].equals("drag");
                tera=false;
            }else if(active && p[1].equals("-terastallize"))tera=true;
        }
        return tera;
    }
    private static boolean isFainted(List<String> events,String slot){
        for(int i=events.size()-1;i>=0;i--){
            String[] p=events.get(i).split("\\|",-1);
            if(!target(p,slot))continue;
            if(p[1].equals("faint"))return true;
            if(Set.of("switch","drag").contains(p[1]))return false;
        }
        return false;
    }
}
