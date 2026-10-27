package com.maestroai.training;

import java.util.*;

/**
 * Phase 6.13: source-pinned, in-process cross-stream observational audit.
 *
 * Cobblemon public Showdown history cannot certify owning actor identity,
 * all installed actions or opponent responses. A CONSISTENT report is NEVER
 * a tactical/Shadow approval or native source-authentication signature.
 */
public final class CrossStreamEvidenceReadiness {
    private CrossStreamEvidenceReadiness(){}
    public static final String SCHEMA="phase6.13.cross-stream-readiness.v1";
    public static final String NATIVE_SOURCE=OwnRequestTransport.SOURCE;
    public static final String PUBLIC_SOURCE=ReadOnlyShadowObservationBridge.SCHEMA;
    public static final String COBBLEMON_VERSION=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    public enum Verdict {
        CONSISTENT_READ_ONLY,
        PUBLIC_UNAVAILABLE,
        NATIVE_UNAVAILABLE,
        BATTLE_MISMATCH,
        TURN_MISMATCH,
        STALE_OR_REPLAYED,
        WITHDRAWN
    }

    /** Only source-derived numerical metadata. No UUID, actor, frame or action. */
    public static final class Report {
        private final Session session;
        private final ReadOnlyShadowObservationBridge.PublicSourceLease publicLease;
        private final NativeOwnActionFusion.Review nativeReview;
        private final Verdict verdict;
        private final long revision;
        private final int publicTurn,nativeTurn,publicEvents,moves,switches;
        private final boolean forced;
        private Report(Session session, Verdict verdict,
                ReadOnlyShadowObservationBridge.PublicSourceLease source,
                NativeOwnActionFusion.Review nativeEvidence) {
            this.session=session;
            this.verdict=verdict;
            publicLease=verdict==Verdict.CONSISTENT_READ_ONLY?source:null;
            nativeReview=verdict==Verdict.CONSISTENT_READ_ONLY?nativeEvidence:null;
            revision=publicLease==null?0:source.revision();
            publicTurn=publicLease==null?0:source.turn();
            nativeTurn=nativeReview==null?0:nativeEvidence.ownTurn();
            publicEvents=publicLease==null?0:source.eventCount();
            moves=nativeReview==null?0:nativeEvidence.ownMoveCount();
            switches=nativeReview==null?0:nativeEvidence.ownSwitchCandidateCount();
            forced=nativeReview!=null &&
                    nativeEvidence.mode()==NativeOwnActionFusion.Mode.FORCED_REPLACEMENT_ONLY;
        }
        /** Only an original public lease PLUS current own native source qualify. */
        boolean fromBattle(Object battle){
            return current() && publicLease.fromBattle(battle)
                    && nativeReview.fromBattle(battle);
        }
        public String schema(){return SCHEMA;}
        public String nativeSource(){return NATIVE_SOURCE;}
        public String publicSource(){return PUBLIC_SOURCE;}
        public Verdict verdict(){return verdict;}
        public long publicRevision(){return revision;}
        public int publicTurn(){return publicTurn;}
        public int ownTurn(){return nativeTurn;}
        public int publicEventCount(){return publicEvents;}
        public int witnessedOwnMoves(){return moves;}
        public int witnessedSwitchCandidates(){return switches;}
        public boolean forcedReplacement(){return forced;}
        /** Expiry is dynamic: later public events, native choice close or withdrawal. */
        public boolean current(){
            return verdict==Verdict.CONSISTENT_READ_ONLY
                    && !session.withdrawn
                    && publicLease!=null && publicLease.current()
                    && nativeReview!=null && nativeReview.current();
        }
        public boolean nativeActorAuthenticatedByPublic(){return false;}
        public boolean sourceDeliveryAuthenticated(){return false;}
        public boolean completeLegalSetProven(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase613Readiness[verdict="+verdict+";revision="+revision
                    +";publicTurn="+publicTurn+";ownTurn="+nativeTurn
                    +";events="+publicEvents+";moves="+moves+";switches="+switches
                    +";authority=NONE]";
        }
    }

    /**
     * One per battle. A mismatch permanently closes THIS observational gate,
     * not the Legacy chooser. New battles create fresh Session instances.
     */
    public static final class Session {
        private Object boundBattle;
        private long lastRevision;
        private int lastTurn;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;}
        private Report abstain(Verdict why){
            withdrawn=true;
            return new Report(this,why,null,null);
        }
        public synchronized Report inspect(ReadOnlyShadowObservationBridge publicStream,
                Object battle,NativeOwnActionFusion.Review nativeEvidence){
            if(withdrawn)return new Report(this,Verdict.WITHDRAWN,null,null);
            if(publicStream==null || battle==null)
                return abstain(Verdict.PUBLIC_UNAVAILABLE);
            if(boundBattle!=null && boundBattle!=battle)
                return abstain(Verdict.BATTLE_MISMATCH);
            // A public lease originates only from the original observer,
            // bound to the exact battle object, version gate and captured
            // immutable prefix. Callers cannot mint one here.
            var publicLease=publicStream.publicLease(battle);
            if(publicLease==null || !publicLease.current())
                return abstain(Verdict.PUBLIC_UNAVAILABLE);
            if(nativeEvidence==null || !nativeEvidence.current())
                return abstain(Verdict.NATIVE_UNAVAILABLE);
            if(!nativeEvidence.fromBattle(battle))
                return abstain(Verdict.BATTLE_MISMATCH);
            if(nativeEvidence.ownTurn()!=publicLease.turn())
                return abstain(Verdict.TURN_MISMATCH);
            if(publicLease.revision()<=lastRevision || publicLease.turn()<lastTurn)
                return abstain(Verdict.STALE_OR_REPLAYED);
            if(!NATIVE_SOURCE.equals(nativeEvidence.source())
                    || !COBBLEMON_VERSION.equals(ReadOnlyShadowObservationBridge.COBBLEMON_VERSION)
                    || nativeEvidence.completeLegalSetProven()
                    || nativeEvidence.shadowSearchAuthorized()
                    || nativeEvidence.livePromotionAuthorized()
                    || nativeEvidence.tacticalDecisionAuthorized())
                return abstain(Verdict.NATIVE_UNAVAILABLE);
            if(!publicLease.current() || !nativeEvidence.current())
                return abstain(Verdict.STALE_OR_REPLAYED);
            boundBattle=battle;lastRevision=publicLease.revision();lastTurn=publicLease.turn();
            return new Report(this,Verdict.CONSISTENT_READ_ONLY,publicLease,nativeEvidence);
        }
    }
}
