package com.maestroai.training;

import com.google.gson.*;
import java.util.*;

/**
 * Phase 6.10: a read-only, owning-dispatch and installed-moveset agreement
 * witness. It is NOT a complete action-legality certificate, a Shadow
 * recommendation or an opponent-request API.
 */
public final class NativeOwnActionEvidence {
    public static final String SCHEMA="phase6.10.own-native-move-witness.v1";
    public static final String SOURCE=OwnRequestTransport.SOURCE;
    private static final String COBBLEMON_VERSION="1.7.3+1.21.1";

    private final Object battle,actor,requestToken;
    private final String side;
    private final UUID activeUuid;
    private final OwnRequestTransport.Request admittedRequest;
    private final int requestTurn;
    private final List<String> enabledMoves;
    private final boolean trapped;
    private boolean spent,withdrawn;

    private NativeOwnActionEvidence(Object battle,Object actor,Object requestToken,
            String side,UUID activeUuid,OwnRequestTransport.Request admittedRequest,int requestTurn,
            List<String> enabledMoves,boolean trapped) {
        this.battle=battle;this.actor=actor;this.requestToken=requestToken;
        this.side=side;this.activeUuid=activeUuid;this.admittedRequest=admittedRequest;this.requestTurn=requestTurn;
        this.enabledMoves=List.copyOf(enabledMoves);this.trapped=trapped;
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 6.10 native own-move evidence unavailable");
    }

    /**
     * Called only from the existing owning RequestInstruction dispatch
     * (after setRequest). The caller has already checked the real actor and
     * selected side, and OwnRequestTransport parsed exactly this raw message.
     * The original private request text never escapes this method.
     */
    public static NativeOwnActionEvidence capture(Object battle,Object actor,
            Object installedRequest,String side,int observedTurn,String raw,
            OwnRequestTransport.Request accepted,String installedVersion) {
        if(battle==null || actor==null || installedRequest==null
                || !COBBLEMON_VERSION.equals(installedVersion)
                || !Set.of("p1","p2").contains(side)
                || observedTurn<0 || observedTurn>9999
                || accepted==null || accepted.mode()!=OwnRequestTransport.Mode.MOVE
                || !side.equals(accepted.side())
                || raw==null || !raw.startsWith("|request|{")
                || raw.length()>131072 || raw.indexOf('\n')>=0)
            throw invalid();
        try {
            // Independently parse strict native request data, then compare with
            // the DTO already admitted to the *owning* transport ledger.
            if(!accepted.equals(OwnRequestJson.parse(raw)))throw invalid();
            var root=StrictOwnJson.read(raw.substring(9));
            if(root.has("forceSwitch") || root.has("wait")
                    || !root.has("active")
                    || root.getAsJsonArray("active").size()!=1)
                throw invalid();
            var active=root.getAsJsonArray("active").get(0).getAsJsonObject();
            // Unsupported gimmicks, uncertain trapping, dynamax and extra
            // active flags abstain; they cannot be silently ignored.
            if(!active.keySet().equals(Set.of("moves","trapped"))
                    || !active.get("trapped").getAsJsonPrimitive().isBoolean())
                throw invalid();
            boolean trapped=active.get("trapped").getAsBoolean();
            var list=active.getAsJsonArray("moves");
            if(list==null || list.isEmpty() || list.size()>4
                    || accepted.selection()==null
                    || accepted.selection().moves().size()!=list.size()
                    || !accepted.selection().options().isEmpty()
                    || accepted.selection().optionSlots()!=0)
                throw invalid();
            var activeOwn=accepted.pokemon().stream()
                    .filter(p->p.dto().active()).toList();
            if(activeOwn.size()!=1 || activeOwn.getFirst().dto().uuid()==null
                    || !activeOwn.getFirst().dto().ident().startsWith(side+": "))
                throw invalid();
            var enabled=new ArrayList<String>();
            var seen=new HashSet<String>();
            for(int i=0;i<list.size();i++){
                var entry=list.get(i).getAsJsonObject();
                if(!entry.keySet().equals(Set.of("move","id","pp","maxpp","target","disabled"))
                        || !entry.get("disabled").getAsJsonPrimitive().isBoolean())
                    throw invalid();
                var slot=accepted.selection().moves().get(i);
                if(!slot.id().matches("[a-z0-9]{1,48}") || !seen.add(slot.id())
                        || slot.pp()<0 || slot.pp()>99)
                    throw invalid();
                if(!slot.disabled() && slot.pp()>0)enabled.add(slot.id());
            }
            if(enabled.isEmpty())throw invalid(); // Struggle not inferred
            return new NativeOwnActionEvidence(battle,actor,installedRequest,
                    side,activeOwn.getFirst().dto().uuid(),accepted,observedTurn,enabled,trapped);
        }catch(RuntimeException parsingFailure){
            throw invalid(); // redact all raw request material
        }
    }

    /** Runtime-owned view built ONLY from the current active ShowdownMoveset. */
    public record InstalledView(Object battle,Object actor,Object installedRequest,
            String side,UUID activeUuid,int actualTurn,
            List<String> usableOrdinaryMoves,boolean trapped) {
        public InstalledView {
            usableOrdinaryMoves=List.copyOf(usableOrdinaryMoves);
        }
    }

    public static final class Review {
        private final OwnRequestTransport.Lease lease;
        private final NativeOwnActionEvidence owner;
        private final String fingerprint;
        private final int turn,count;
        private Review(OwnRequestTransport.Lease lease,NativeOwnActionEvidence owner,
                       String fingerprint,int turn,int count) {
            this.lease=lease;this.owner=owner;this.fingerprint=fingerprint;
            this.turn=turn;this.count=count;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public int ownTurn(){return turn;}
        public int observedMoveCount(){return count;}
        /** Synthetic canonical hash, not an externally signed legal set. */
        public String moveFingerprint(){return fingerprint;}
        public boolean currentOwnMoveAgreement(){
            return !owner.withdrawn && lease.current();
        }
        public boolean runtimeOwnMovesObserved(){return currentOwnMoveAgreement();}
        public boolean completeLegalSetProven(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase610OwnMoves[turn="+turn+";count="+count
                    +";completeLegalSet=false;authority=NONE]";
        }
    }

    /** One-use, actor-bound, choice-turn freshness check. Never influences the chooser. */
    public synchronized Review attest(OwnRequestTransport.Lease lease,InstalledView installed) {
        if(withdrawn || spent)throw invalid();
        spent=true;
        try {
            if(lease==null || !lease.current() || installed==null
                    || battle!=installed.battle() || actor!=installed.actor()
                    || requestToken!=installed.installedRequest()
                    || !side.equals(installed.side())
                    || !activeUuid.equals(installed.activeUuid())
                    || !lease.matchesOwnReadOnlyRequest(battle,actor,requestToken,
                          side,activeUuid,admittedRequest)
                    || lease.turn()!=requestTurn+1
                    || installed.actualTurn()!=lease.turn()
                    || installed.trapped()!=trapped
                    || installed.usableOrdinaryMoves().isEmpty()
                    || installed.usableOrdinaryMoves().size()>4
                    || installed.usableOrdinaryMoves().stream().distinct().count()
                        !=installed.usableOrdinaryMoves().size()
                    || !enabledMoves.equals(installed.usableOrdinaryMoves()))
                throw invalid();
            var ids=enabledMoves.stream().map(m->"move:"+m).toList();
            var fingerprint=OfflineRuntimeEvidenceContract.actionFingerprint(ids);
            if(!lease.current())throw invalid();
            return new Review(lease,this,fingerprint,lease.turn(),ids.size());
        }catch(RuntimeException failure) {
            withdrawn=true;
            throw invalid();
        }
    }
    public synchronized void withdraw(){withdrawn=true;}
    public synchronized boolean withdrawn(){return withdrawn;}
}
