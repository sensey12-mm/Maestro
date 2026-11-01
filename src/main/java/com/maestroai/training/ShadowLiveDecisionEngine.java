package com.maestroai.training;

import java.util.*;

/**
 * Independently authored source-catalogue Shadow live policy. This is a
 * deterministic, explicitly heuristic decision policy: move base power is NOT
 * damage, a move's accuracy is NOT a predicted opponent response, and these
 * scores do not represent win probabilities. It uses ONLY the owning legal
 * request and a public-filtered/own-team snapshot. Legacy never enters.
 *
 * All actions must still pass the original native request/custody and exact
 * Cobblemon ShowdownActionResponse.isValid gate before promotion. Until
 * authorized, this policy only contributes an advisory candidate.
 */
public final class ShadowLiveDecisionEngine {
    private ShadowLiveDecisionEngine(){}

    public enum Reason {
        MOVE, RECOVERY, SETUP, STATUS, VOLUNTARY_SWITCH,
        FORCED_REPLACEMENT, UNKNOWN_OR_UNSUPPORTED, NO_LEGAL_ACTION
    }
    public record Choice(String action, Reason reason, int scoredCandidates) {
        public Choice {
            if(action==null || action.isBlank() || reason==null || scoredCandidates<1)
                throw new IllegalArgumentException("invalid independent choice");
        }
        /** Telemetry never includes opponent identities, own UUIDs or moves. */
        public String safeLine(){
            return "ShadowLiveDecisionEngine[decision=INDEPENDENT;category="+reason
                +";candidateCount="+scoredCandidates
                +";prediction=NONE;calibration=UNPROVEN;authority=NONE]";
        }
    }

    /** Own-party health is legitimate first-party data, never opponent data. */
    public record Reserve(String ownUuid, double hpFraction, boolean eligible) {
        public Reserve {
            if(ownUuid==null || ownUuid.isBlank() || !Double.isFinite(hpFraction)
                    || hpFraction<0 || hpFraction>1)
                throw new IllegalArgumentException("invalid owning reserve");
        }
    }
    private record Scored(String action, double value, Reason reason) {}

    /**
     * First-party Cobblemon move template. This is NOT an opponent move set,
     * a damage estimate or an inferred secondary-effect probability.
     */
    public record OwnMoveFacts(String id,String type,String category,
                               double power,double accuracy,int priority) {
        public OwnMoveFacts {
            if(id==null || id.isBlank() || type==null || type.isBlank()
                    || category==null || category.isBlank()
                    || !Double.isFinite(power) || power<0 || power>1000
                    || !Double.isFinite(accuracy) || accuracy<0 || accuracy>100
                    || priority<-7 || priority>7)
                throw new IllegalArgumentException("Invalid first-party template");
            type=type.toLowerCase(java.util.Locale.ROOT);
            category=category.toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Independent forced-replacement policy: highest known remaining own HP,
     * tie broken by own party order. No matchup value is inferred from hidden
     * opposing stats, item, typing or moves. No eligible reserve -> abstain.
     */
    public static Choice forcedReplacement(List<Reserve> owned, boolean forced) {
        if(!forced || owned==null || owned.size()>6)return null;
        Reserve best=null;int count=0;
        Set<String> seen=new HashSet<>();
        for(Reserve candidate:owned) {
            if(candidate==null || !seen.add(candidate.ownUuid()))return null;
            if(!candidate.eligible() || candidate.hpFraction()<=0)continue;
            count++;
            if(best==null || candidate.hpFraction()>best.hpFraction())best=candidate;
        }
        return best==null?null:new Choice("switch:"+best.ownUuid(),
                Reason.FORCED_REPLACEMENT,count);
    }

    /**
     * Ordinary live decision from the current OWN legal move IDs and the
     * public-filtered observation. Unverified effects are UNKNOWN and never
     * assigned imaginary utility. Scores order conservative action classes
     * rather than pretending to calculate a whole-turn battle outcome.
     *
     * This policy intentionally has no legacyAction input or probability map.
     * Independently attested native root and custody checks belong to caller.
     */
    public static Choice ordinary(BattleSnapshot snapshot,boolean requestForced,
                                   boolean requestTrapped,boolean sourceCurrent) {
        return ordinary(snapshot,requestForced,requestTrapped,sourceCurrent,Map.of());
    }

    public static Choice ordinary(BattleSnapshot snapshot,boolean requestForced,
                                   boolean requestTrapped,boolean sourceCurrent,
                                   Map<String,OwnMoveFacts> ownedMoveFacts) {
        if(!sourceCurrent || snapshot==null || requestForced
                || snapshot.self().fainted() || snapshot.opponent().fainted()
                || snapshot.legalMoves().size()>16
                || snapshot.legalSwitches().size()>6
                || snapshot.selfBench().size()!=snapshot.legalSwitches().size()
                || snapshot.legalMoves().isEmpty() && snapshot.legalSwitches().isEmpty())
            return null;
        if(ownedMoveFacts==null || ownedMoveFacts.size()>16
                || ownedMoveFacts.entrySet().stream().anyMatch(entry->
                    entry.getKey()==null || entry.getValue()==null
                    || !entry.getKey().equals(entry.getValue().id())
                    || !snapshot.legalMoves().contains(entry.getKey())))
            return null;
        // A changed or duplicated legal root cannot produce an executable
        // independent policy response.
        Set<String> seen=new HashSet<>();
        List<Scored> scores=new ArrayList<>();
        for(String id:snapshot.legalMoves()){
            if(id==null || id.isBlank() || !seen.add("move:"+id))return null;
            Scored candidate=scoreMove(id,snapshot,ownedMoveFacts.get(id));
            if(candidate!=null)scores.add(candidate);
        }
        for(int i=0;i<snapshot.legalSwitches().size();i++){
            String id=snapshot.legalSwitches().get(i);
            if(id==null || id.isBlank() || !seen.add("switch:"+id))return null;
            if(requestTrapped) return null;
            // Voluntary switching is deliberately constrained: without a
            // certified matchup/entry-hazard estimate this is NOT a generic
            // favorable switch. It is a preservation option at critical HP.
            var reserve=snapshot.selfBench().get(i);
            if(reserve.hpKnown() && !reserve.fainted()
                    && (snapshot.legalMoves().isEmpty()
                        || snapshot.self().hpKnown() && snapshot.self().hpFraction()<=0.20
                            && reserve.hpFraction()>=0.75))
                scores.add(new Scored("switch:"+id,
                        snapshot.legalMoves().isEmpty()
                            ?100+reserve.hpFraction()*10
                            :115+reserve.hpFraction()*10,
                        Reason.VOLUNTARY_SWITCH));
        }
        if(scores.isEmpty())return null;
        scores.sort(Comparator.comparingDouble(Scored::value).reversed()
                .thenComparing(Scored::action));
        Scored selected=scores.getFirst();
        return new Choice(selected.action(),selected.reason(),scores.size());
    }
    private static Scored scoreMove(String id,BattleSnapshot snapshot,
                                    OwnMoveFacts nativeFacts){
        CobblemonMechanics.Move mechanics=CobblemonMechanics.move(id);
        boolean nativeAttack=nativeFacts!=null && nativeFacts.power()>0
                && nativeFacts.accuracy()>0
                && ("physical".equals(nativeFacts.category())
                    || "special".equals(nativeFacts.category()));
        if(nativeAttack || nativeFacts==null && mechanics!=null && mechanics.power()>0){
            // Whitelisted simulator base power and source accuracy are known,
            // but predicted damage, matchup and opponent response remain UNKNOWN.
            double typeFactor=1.0;
            // Type matchups are consulted ONLY when current opposing types
            // were explicitly confirmed by the PUBLIC spectator projection.
            if(!snapshot.opponent().types().isEmpty()){
                int exponent=CobblemonMechanics.effectiveness(
                        nativeAttack?nativeFacts.type():mechanics.type(),
                        snapshot.opponent().types());
                if(exponent==Integer.MIN_VALUE)return null; // witnessed immunity
                typeFactor=Math.scalb(1.0,exponent);
            }
            String type=nativeAttack?nativeFacts.type():mechanics.type();
            double stab=snapshot.self().types().stream().anyMatch(t->
                    t.equalsIgnoreCase(type))?1.5:1;
            double base=(nativeAttack?nativeFacts.power():mechanics.power())
                    *(nativeAttack?nativeFacts.accuracy():mechanics.accuracy())
                    /100.0*typeFactor*stab
                    +Math.max(0,nativeAttack?nativeFacts.priority():mechanics.priority())*12;
            return new Scored("move:"+id,base,Reason.MOVE);
        }
        MoveEffectModel.Effect effect=MoveEffectModel.describe(
                id,nativeFacts==null?"unknown":nativeFacts.type(),
                nativeFacts==null?"status":nativeFacts.category(),
                nativeFacts==null?0:nativeFacts.power(),
                nativeFacts==null?100:nativeFacts.accuracy(),
                nativeFacts==null?0:nativeFacts.priority()).effect();
        return switch(effect){
            case HEAL_SELF -> snapshot.self().hpKnown()
                    && snapshot.self().hpFraction()<=0.60
                    ?new Scored("move:"+id,170*(1-snapshot.self().hpFraction()),
                            Reason.RECOVERY):null;
            case BOOST_SELF -> mechanics!=null && !mechanics.boosts().isEmpty()
                    && snapshot.self().hpKnown() && snapshot.self().hpFraction()>=0.65
                    && mechanics.boosts().keySet().stream().allMatch(
                        k->snapshot.self().knownBoosts().containsKey(k)
                          && snapshot.self().knownBoosts().get(k)<=2)
                    ?new Scored("move:"+id,80,Reason.SETUP):null;
            // Only witnessed, annotated effects are considered; no imagined
            // poison/burn chance, Encore target or hazard absence.
            case LOCK_TARGET_MOVE -> !snapshot.opponent().revealedMoves().isEmpty()
                    ?new Scored("move:"+id,32,Reason.STATUS):null;
            case SET_HAZARD -> new Scored("move:"+id,28,Reason.STATUS);
            case PROTECT -> snapshot.self().hpKnown()
                    && snapshot.self().hpFraction()<=0.55
                    ?new Scored("move:"+id,22,Reason.STATUS):null;
            case REMOVE_HAZARD -> null; // own hazard absence not proved here
            default -> null;
        };
    }
}
