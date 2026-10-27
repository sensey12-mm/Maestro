package com.maestroai.training;

/**
 * Phase 6.21: non-authoritative, mutually exclusive native evidence tracks.
 *
 * The direct public/own-request track (6.9–6.13) DOES NOT certify split
 * traffic (6.16–6.20). A same-battle mix of positive tracks is ambiguous,
 * not a single complete end-to-end attestation. No tactical consumers.
 */
public final class NativeEvidenceEndToEndGate {
    private NativeEvidenceEndToEndGate(){}
    public static final String SCHEMA="phase6.21.native-evidence-safety-gate.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    public enum Verdict {
        DIRECT_ONLY_OBSERVATIONAL,
        SPLIT_TIMING_UNVERIFIED,
        MISSING_DIRECT_EVIDENCE,
        MISSING_SPLIT_EVIDENCE,
        STALE_SOURCE,
        WRONG_BATTLE,
        WRONG_SPLIT_CUSTODY,
        CROSS_MODE_CONFLICT,
        REPLAY_OR_DUPLICATE,
        WITHDRAWN
    }
    public enum Track { NONE, DIRECT_OWN, SPLIT_UNVERIFIED }
    public static final class Report {
        private final Session session;
        private final Verdict verdict;
        private final Track track;
        private final CrossStreamEvidenceReadiness.Report direct;
        private final NativeDispatchHistoryCorrelation.Report split;
        private final NativeSplitRecipientCustody.Report recipient;
        private final int turn;
        private final long revision;
        private Report(Session session,Verdict verdict,
                       CrossStreamEvidenceReadiness.Report direct,
                       NativeDispatchHistoryCorrelation.Report split,
                       NativeSplitRecipientCustody.Report recipient) {
            this.session=session;this.verdict=verdict;
            this.direct=verdict==Verdict.DIRECT_ONLY_OBSERVATIONAL?direct:null;
            this.split=verdict==Verdict.SPLIT_TIMING_UNVERIFIED?split:null;
            this.recipient=verdict==Verdict.SPLIT_TIMING_UNVERIFIED?recipient:null;
            track=this.direct!=null?Track.DIRECT_OWN:
                    this.split!=null?Track.SPLIT_UNVERIFIED:Track.NONE;
            turn=this.direct!=null?direct.publicTurn():
                    this.split!=null?split.observedTurn():0;
            revision=this.direct!=null?direct.publicRevision():
                    this.split!=null?split.publicRevision():0;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        /** Original source-owned battle lineage, not runtime authentication. */
        boolean fromBattle(Object originalBattle){
            return originalBattle!=null && current() && session.battle==originalBattle;
        }
        public Track track(){return track;}
        public int publicTurn(){return turn;}
        public long publicRevision(){return revision;}
        public boolean current(){
            synchronized(session){
                if(session.withdrawn || session.track!=track
                        || track==Track.NONE || verdict!=session.lastVerdict
                        || revision!=session.lastRevision || turn!=session.lastTurn)
                    return false;
                if(direct!=null) return direct.current();
                return split!=null && split.current() && recipient!=null && recipient.current()
                        && split.sharesSplitSource(recipient);
            }
        }
        public boolean completeInstalledLegalSetProven(){return false;}
        public boolean publicActorAuthenticated(){return false;}
        public boolean eventIdentityAuthenticated(){return false;}
        public boolean recipientDeliveryAuthenticated(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase621Safety[verdict="+verdict+";track="+track
                    +";turn="+turn+";revision="+revision+";authority=NONE]";
        }
    }
    /**
     * One instance per battle. No unrelated witness can mint a positive
     * result by replaying public counts, source hashes or event names.
     */
    public static final class Session {
        private Object battle;
        private Track track=Track.NONE;
        private Verdict lastVerdict;
        private long lastRevision;
        private int lastTurn;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        private Report abstain(Verdict status){
            return new Report(this,status,null,null,null);
        }
        private Report fail(Verdict status) {
            withdraw();
            return abstain(status);
        }
        private boolean foreign(Object supplied){
            return supplied==null || battle!=null && battle!=supplied;
        }
        private boolean used(Track proposed,long revision,int turn){
            return track==proposed && revision<=lastRevision && turn<=lastTurn;
        }
        public synchronized Report inspectDirect(Object originalBattle,
                CrossStreamEvidenceReadiness.Report direct) {
            if(withdrawn)return abstain(Verdict.WITHDRAWN);
            if(foreign(originalBattle))return fail(Verdict.WRONG_BATTLE);
            if(direct==null)return abstain(Verdict.MISSING_DIRECT_EVIDENCE);
            if(!direct.current())return fail(Verdict.STALE_SOURCE);
            if(!direct.fromBattle(originalBattle))return fail(Verdict.WRONG_BATTLE);
            if(track==Track.SPLIT_UNVERIFIED)return fail(Verdict.CROSS_MODE_CONFLICT);
            if(direct.publicTurn()<1 || direct.ownTurn()!=direct.publicTurn()
                    || direct.publicRevision()<1 || direct.publicEventCount()<1
                    || !direct.nativeSource().equals(OwnRequestTransport.SOURCE)
                    || !direct.publicSource().equals(ReadOnlyShadowObservationBridge.SCHEMA)
                    || direct.completeLegalSetProven()
                    || direct.nativeActorAuthenticatedByPublic()
                    || direct.sourceDeliveryAuthenticated()
                    || direct.tacticalDecisionAuthorized()
                    || direct.shadowSearchAuthorized()
                    || direct.livePromotionAuthorized())
                return fail(Verdict.STALE_SOURCE);
            if(used(Track.DIRECT_OWN,direct.publicRevision(),direct.publicTurn()))
                return fail(Verdict.REPLAY_OR_DUPLICATE);
            battle=originalBattle;track=Track.DIRECT_OWN;
            lastRevision=direct.publicRevision();lastTurn=direct.publicTurn();
            lastVerdict=Verdict.DIRECT_ONLY_OBSERVATIONAL;
            return new Report(this,lastVerdict,direct,null,null);
        }
        public synchronized Report inspectSplit(Object originalBattle,
                NativeDispatchHistoryCorrelation.Report correlation,
                NativeSplitRecipientCustody.Report recipient) {
            if(withdrawn)return abstain(Verdict.WITHDRAWN);
            if(foreign(originalBattle))return fail(Verdict.WRONG_BATTLE);
            if(correlation==null || recipient==null)
                return abstain(Verdict.MISSING_SPLIT_EVIDENCE);
            if(!correlation.current() || !recipient.current())
                return fail(Verdict.STALE_SOURCE);
            if(!correlation.fromBattle(originalBattle) || !recipient.fromBattle(originalBattle))
                return fail(Verdict.WRONG_BATTLE);
            if(track==Track.DIRECT_OWN)return fail(Verdict.CROSS_MODE_CONFLICT);
            // The two positive claims MUST reference exactly one original
            // Phase 6.16 structural receipt; count/revision equality alone
            // would allow substitution of a foreign custody session.
            if(!correlation.sharesSplitSource(recipient)
                    || recipient.verdict()!=NativeSplitRecipientCustody.Verdict.DELIVERY_UNOBSERVED
                    || correlation.verdict()
                        !=NativeDispatchHistoryCorrelation.Verdict.PLAUSIBLE_WINDOW_UNVERIFIED
                    || recipient.structurallyMatchedPairs()
                        !=correlation.structurallyObservedSplitCount()
                    || recipient.sourceRevision()!=correlation.publicRevision())
                return fail(Verdict.WRONG_SPLIT_CUSTODY);
            if(correlation.observedTurn()<1 || correlation.publicRevision()<1
                    || correlation.structurallyObservedSplitCount()<1
                    || correlation.recipientDeliveryAuthenticated()
                    || correlation.sameUnderlyingEventProven()
                    || correlation.exactSplitPacketLinked()
                    || recipient.nativeRecipientDeliveryAuthenticated()
                    || recipient.nativeObserverDeliveryAuthenticated()
                    || recipient.publicReadinessLeaseAuthorized()
                    || correlation.tacticalDecisionAuthorized()
                    || correlation.shadowSearchAuthorized()
                    || correlation.livePromotionAuthorized())
                return fail(Verdict.STALE_SOURCE);
            if(used(Track.SPLIT_UNVERIFIED,
                    correlation.publicRevision(),correlation.observedTurn()))
                return fail(Verdict.REPLAY_OR_DUPLICATE);
            battle=originalBattle;track=Track.SPLIT_UNVERIFIED;
            lastRevision=correlation.publicRevision();lastTurn=correlation.observedTurn();
            lastVerdict=Verdict.SPLIT_TIMING_UNVERIFIED;
            return new Report(this,lastVerdict,null,correlation,recipient);
        }
    }
}
