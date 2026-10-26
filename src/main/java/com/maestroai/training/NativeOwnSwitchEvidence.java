package com.maestroai.training;

import com.google.gson.*;
import java.util.*;
import java.util.regex.*;

/**
 * Phase 6.11: read-only native own-party and request switch witness.
 * Neither a request nor canBeSentOut proves all executable switch choices:
 * unsupported volatile restrictions, gimmicks and installed mechanics abstain.
 */
public final class NativeOwnSwitchEvidence {
    public static final String SCHEMA="phase6.11.own-switch-witness.v1";
    public static final String SOURCE=OwnRequestTransport.SOURCE;
    private static final String VERSION="1.7.3+1.21.1";
    private static final Pattern CONDITION=Pattern.compile(
            "([0-9]{1,5})/([1-9][0-9]{0,4})(?: (brn|par|psn|tox|slp|frz|fnt))?");
    private final Object battle,actor,requestToken;
    private final String side;
    private final OwnRequestTransport.Request admitted;
    private final UUID activeUuid;
    private final int dispatchTurn;
    private final boolean forced,trapped;
    private final List<UUID> partyIds;
    private final List<Integer> ownHp;
    private final List<Boolean> requestEligible;
    private boolean spent,withdrawn;

    private NativeOwnSwitchEvidence(Object battle,Object actor,Object token,String side,
            OwnRequestTransport.Request admitted,UUID activeUuid,int dispatchTurn,
            boolean forced,boolean trapped,List<UUID> ids,List<Integer> hp,List<Boolean> eligibility){
        this.battle=battle;this.actor=actor;this.requestToken=token;this.side=side;
        this.admitted=admitted;this.activeUuid=activeUuid;this.dispatchTurn=dispatchTurn;
        this.forced=forced;this.trapped=trapped;partyIds=List.copyOf(ids);
        ownHp=List.copyOf(hp);requestEligible=List.copyOf(eligibility);
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.11 own switch evidence unavailable");
    }

    /**
     * Runs ONLY from Maestro's owning RequestInstruction dispatch, with the
     * same parsed DTO already delivered to the owning ledger. Retains only
     * private internal party IDs and HP, never raw request text or names.
     */
    public static NativeOwnSwitchEvidence capture(Object battle,Object actor,Object request,
            String side,int battleTurn,String raw,OwnRequestTransport.Request admitted,
            String cobblemonVersion,OwnRequestTransport ledger){
        if(battle==null||actor==null||request==null||!VERSION.equals(cobblemonVersion)
                ||!Set.of("p1","p2").contains(side)||battleTurn<0||battleTurn>10000
                ||ledger==null||admitted==null||admitted.mode()==OwnRequestTransport.Mode.WAIT
                ||!side.equals(admitted.side())||raw==null||!raw.startsWith("|request|{")
                ||raw.length()>131072||raw.indexOf('\n')>=0)throw invalid();
        try{
            if(!admitted.equals(OwnRequestJson.parse(raw))
                    ||!ledger.matchesReadOnlyRaw(battle,actor,request,side,raw,admitted))
                throw invalid();
            JsonObject root=StrictOwnJson.read(raw.substring(9));
            boolean forced=admitted.mode()==OwnRequestTransport.Mode.FORCED;
            boolean trapped=false;
            if(forced){
                if(root.has("wait")||root.has("active")||!root.has("forceSwitch")
                        ||root.getAsJsonArray("forceSwitch").size()!=1
                        ||!root.getAsJsonArray("forceSwitch").get(0).getAsBoolean())
                    throw invalid();
            }else{
                if(root.has("wait")||root.has("forceSwitch")
                        ||!root.has("active")||root.getAsJsonArray("active").size()!=1)
                    throw invalid();
                var active=root.getAsJsonArray("active").get(0).getAsJsonObject();
                if(!active.keySet().equals(Set.of("moves","trapped"))
                        ||!active.get("trapped").getAsJsonPrimitive().isBoolean()
                        ||admitted.selection()==null
                        ||!admitted.selection().options().isEmpty()
                        ||admitted.selection().optionSlots()!=0)
                    throw invalid();
                trapped=active.get("trapped").getAsBoolean();
            }
            var ps=admitted.pokemon();
            if(ps.isEmpty()||ps.size()>6 || ps.stream().filter(p->p.dto().active()).count()!=1)
                throw invalid();
            List<UUID> ids=new ArrayList<>();List<Integer> hp=new ArrayList<>();
            List<Boolean> candidates=new ArrayList<>();UUID own=null;
            for(var p:ps){
                var d=p.dto();
                if(d.uuid()==null||!d.ident().startsWith(side+": ")
                        ||ids.contains(d.uuid())||d.condition()==null)
                    throw invalid();
                var match=CONDITION.matcher(d.condition());
                if(!match.matches())throw invalid();
                int current=Integer.parseInt(match.group(1));
                int max=Integer.parseInt(match.group(2));
                if(max>10000||current>max
                        ||("fnt".equals(match.group(3)) && current!=0))throw invalid();
                if(d.active())own=d.uuid();
                ids.add(d.uuid());hp.add(current);
                candidates.add(!d.active()&&!p.special()&&current>0);
            }
            if(own==null)throw invalid();
            return new NativeOwnSwitchEvidence(battle,actor,request,side,admitted,
                    own,battleTurn,forced,trapped,ids,hp,candidates);
        }catch(RuntimeException unsupported){
            throw invalid();
        }
    }

    /** Source-derived installed party values. Read only from Maestro's actor. */
    public record PartySlot(UUID uuid,double health,boolean gone,
                            boolean sentOut,boolean canBeSentOut){}
    public record InstalledView(Object battle,Object actor,Object request,String side,
            UUID activeUuid,int turn,boolean forcedChoice,Boolean trapped,
            List<PartySlot> party){
        public InstalledView{party=List.copyOf(party);}
    }

    public static final class Review {
        private final NativeOwnSwitchEvidence source;
        private final OwnRequestTransport.Lease lease;
        private final String fingerprint;
        private final int turn,count;
        private Review(NativeOwnSwitchEvidence source,OwnRequestTransport.Lease lease,
                       String fingerprint,int turn,int count){
            this.source=source;this.lease=lease;this.fingerprint=fingerprint;
            this.turn=turn;this.count=count;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public int ownTurn(){return turn;}
        public int observedSwitchCount(){return count;}
        public String candidateFingerprint(){return fingerprint;}
        /** Internal source-owner seal; no opponent or raw request getters. */
        boolean sameChoice(OwnRequestTransport.Lease expected,Object battle,Object actor,
                Object request,String side,UUID active,int turn){
            return lease==expected && currentlyObserved()
                    && source.battle==battle && source.actor==actor
                    && source.requestToken==request && source.side.equals(side)
                    && source.activeUuid.equals(active) && this.turn==turn
                    && SOURCE.equals(source());
        }
        boolean forced(){return source.forced;}
        boolean trapped(){return source.trapped;}
        public boolean currentlyObserved(){return !source.withdrawn&&lease.current();}
        public boolean forcedReplacementWitnessed(){return currentlyObserved()&&source.forced;}
        public boolean trappingWitnessed(){return currentlyObserved()&&!source.forced&&source.trapped;}
        public boolean completeLegalSetProven(){return false;}
        public boolean executableSwitchSetCertified(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase611OwnSwitch[turn="+turn+";count="+count
                    +";forced="+source.forced+";completeLegalSet=false;authority=NONE]";
        }
    }

    /** Consume a SINGLE choice-phase lease. Never choose or execute switches. */
    public synchronized Review attest(OwnRequestTransport.Lease lease,InstalledView installed){
        if(withdrawn||spent)throw invalid();
        spent=true;
        try{
            if(lease==null||!lease.current()||installed==null
                    ||battle!=installed.battle()||actor!=installed.actor()
                    ||requestToken!=installed.request()||!side.equals(installed.side())
                    ||!activeUuid.equals(installed.activeUuid())||installed.forcedChoice()!=forced
                    ||installed.turn()!=lease.turn()
                    ||lease.turn()!=dispatchTurn+(forced?0:1)
                    ||!lease.matchesOwnSwitchRequest(battle,actor,requestToken,
                            side,activeUuid,admitted,forced)
                    ||(!forced && !Boolean.valueOf(trapped).equals(installed.trapped()))
                    ||installed.party().size()!=partyIds.size())
                throw invalid();
            List<String> candidates=new ArrayList<>();
            Set<UUID> actualIds=new HashSet<>();
            for(int i=0;i<partyIds.size();i++){
                PartySlot slot=installed.party().get(i);
                if(slot==null||slot.uuid()==null||!actualIds.add(slot.uuid())
                        ||!partyIds.get(i).equals(slot.uuid())||!Double.isFinite(slot.health())
                        ||slot.health()!=ownHp.get(i))throw invalid();
                boolean isOwnActive=activeUuid.equals(slot.uuid());
                if(isOwnActive!=admitted.pokemon().get(i).dto().active())throw invalid();
                if(!isOwnActive){
                    boolean nativeEligible=slot.health()>0&&!slot.gone()
                            &&!slot.sentOut()&&slot.canBeSentOut();
                    if(nativeEligible!=requestEligible.get(i))throw invalid();
                    if(nativeEligible&&!trapped || nativeEligible&&forced)
                        candidates.add("switch:slot"+(i+1));
                }
            }
            if(forced&&candidates.isEmpty())throw invalid();
            if(!lease.current())throw invalid();
            String hash=candidates.isEmpty()?"NONE":
                    OfflineRuntimeEvidenceContract.actionFingerprint(candidates);
            return new Review(this,lease,hash,lease.turn(),candidates.size());
        }catch(RuntimeException failure){
            withdrawn=true;
            throw invalid();
        }
    }
    public synchronized void withdraw(){withdrawn=true;}
    public synchronized boolean withdrawn(){return withdrawn;}
}
