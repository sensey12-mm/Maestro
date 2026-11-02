package com.maestroai.training;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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
        private final List<String> displayedTypeHypothesis;
        private final Object battle;
        private final PublicSplitRoutingGate.Report routing;
        private final PublicOpponentIdentityContinuity.Report continuity;
        private final Supplier<List<String>> originalHistory;
        private final BooleanSupplier nativeOwnCurrent;
        private Capture(BattleSnapshot snapshot,List<String> hypothesis,Object battle,
                PublicSplitRoutingGate.Report routing,
                PublicOpponentIdentityContinuity.Report continuity,
                Supplier<List<String>> history,BooleanSupplier own) {
            this.snapshot=snapshot;
            this.displayedTypeHypothesis=List.copyOf(hypothesis);
            this.battle=battle;this.routing=routing;
            this.continuity=continuity;this.originalHistory=history;
            this.nativeOwnCurrent=own;
        }
        public BattleSnapshot snapshot(){return snapshot;}
        /** Conditional public appearance; NEVER actual identity or confirmed typing. */
        public List<String> displayedTypeHypothesis(){return displayedTypeHypothesis;}
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
                    ?"UNKNOWN":"EXPLICIT")
                +";displayHypothesis="+(displayedTypeHypothesis.isEmpty()?"UNKNOWN":"CONDITIONAL")
                +";authority=NONE]";
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
        return capture(battle,ownSide,turn,ownOnly,pinned,originalHistory,
                ownerCurrent,PublicSpeciesCatalog::displayedTypeHypothesis);
    }

    /**
     * Live integration may use the *installed public species registry* for
     * hypothetical display-match types. No opponent object enters this API.
     */
    public static Capture capture(Object battle,String ownSide,int turn,
            BattleSnapshot ownOnly,boolean pinned,
            Supplier<List<String>> originalHistory,BooleanSupplier ownerCurrent,
            Function<String,List<String>> displayedCatalog){
        if(displayedCatalog==null)return null;
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
            if(!continuity.current())return null;
            var publicEvents=new PublicProtocolEvidence();
            publicEvents.update(history);
            if(!publicEvents.complete() || !publicEvents.framed()
                    || publicEvents.corrupted() || !publicEvents.coverageUnknown().isEmpty()
                    || publicEvents.publicTurn()!=routing.publicTurn()
                    || (routing.publicTurn()!=turn && routing.publicTurn()!=turn-1))
                return null;
            String foe=ownSide.equals("p1")?"p2a":"p1a";
            String trueReveal=PublicOpponentAppearance.witnessedRevealSpecies(
                    foe,publicEvents.mechanicsEvents());
            // A public replace can *positively reveal* the current true
            // species. It never binds identity across a switch or Transform.
            if(!continuity.uncertainty().stream().allMatch(risk->
                    risk==PublicOpponentIdentityContinuity.Uncertainty.PUBLIC_DISPLAY_ONLY
                    || risk==PublicOpponentIdentityContinuity.Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY
                    || trueReveal!=null && risk==PublicOpponentIdentityContinuity.Uncertainty.ILLUSION_REVEAL_UNRESOLVED))
                return null;
            boolean reused=publicEvents.reentryAmbiguous(foe)
                    || continuity.uncertainty().contains(
                        PublicOpponentIdentityContinuity.Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY);
            var mechanics=new PublicMechanicsEvidence(ignored->null);
            mechanics.update(publicEvents);
            var observed=mechanics.observation(foe);
            // Reused nicknames are not stable identities. However, a fresh
            // explicit current type notice remains valid for this active
            // position. HP and species continue to be UNKNOWN on reuse.
            if(observed==null || observed.ambiguous()
                    && !reused && trueReveal==null)return null;
            // A named switch could be Illusion. Only positive public revelations
            // can supply real species and ONLY explicit type announcements can
            // supply current attack-effectiveness typing.
            String species=trueReveal!=null && observed.confirmedSpecies().known()
                    && trueReveal.equals(observed.confirmedSpecies().value())
                    && !observed.unsupportedEffects()
                    ?trueReveal:"UNVERIFIED";
            List<String> types=observed.types().known()
                    ?observed.types().value():List.of();
            if(types.isEmpty() && !"UNVERIFIED".equals(species)) {
                // Public species metadata is valid only after actual public
                // Illusion removal, never after an ordinary displayed switch.
                types=PublicOpponentAppearance.registryTypes(species,displayedCatalog);
            }
            var hypothetical=types.isEmpty() && "UNVERIFIED".equals(species)
                    ?PublicOpponentAppearance.displayedTypeHypothesis(foe,
                            publicEvents.mechanicsEvents(),displayedCatalog)
                    :List.<String>of();
            if(reused && types.isEmpty() && hypothetical.isEmpty()
                    && "UNVERIFIED".equals(species))return null;
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
                    && item==null && ability==null && hypothetical.isEmpty())return null;
            boolean tera=isTera(publicEvents.mechanicsEvents(),foe)
                    && types.size()==1;
            var opponent=new BattleSnapshot.Combatant(species,hp,types,
                    Map.of(),revealed,item,ability,false);
            var snapshot=new BattleSnapshot(turn,ownOnly.self(),opponent,
                    ownOnly.selfBench(),List.of(),ownOnly.legalMoves(),
                    ownOnly.legalSwitches(),Map.of(),tera,tera?types.getFirst():null);
            var bound=new Capture(snapshot,hypothetical,battle,routing,continuity,
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
