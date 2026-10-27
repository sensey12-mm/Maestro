package com.maestroai.training;

/**
 * Phase 6.20: correlate two independent observational source windows.
 * This is NEVER proof that native dispatch and a Showdown |split| are the
 * same event, nor proof that a packet reached any recipient.
 *
 * Inputs are original, still-current 6.16/6.19 session-owned reports,
 * not caller-recreated counts. Reports contain numbers/verdicts only.
 */
public final class NativeDispatchHistoryCorrelation {
    private NativeDispatchHistoryCorrelation() {}
    public static final String SCHEMA="phase6.20.native-history-correlation.v1";
    public static final String SOURCE=PublicSplitRoutingGate.SOURCE;
    public enum Verdict {
        PLAUSIBLE_WINDOW_UNVERIFIED,
        MISSING_NATIVE_OBSERVATION,
        MISSING_SPLIT_HISTORY,
        BAD_SOURCE,
        BATTLE_MISMATCH,
        TURN_MISMATCH,
        HISTORY_WINDOW_CONTRADICTION,
        AMBIGUOUS_MULTIPLE_EVENTS,
        REPLAYED_OR_STALE,
        WITHDRAWN
    }

    public static final class Report {
        private final Session owner;
        private final PublicSplitRoutingGate.Report history;
        private final NativeSplitDispatchObservation.Report nativeReturn;
        private final Verdict verdict;
        private final long revision, ordinal;
        private final int turn, splitCount, historyChunks;
        private Report(Session owner,Verdict verdict,
                       PublicSplitRoutingGate.Report history,
                       NativeSplitDispatchObservation.Report nativeReturn) {
            this.owner=owner;this.verdict=verdict;
            // Retain source handles only for positive dynamically-expiring
            // windows, not for abstentions and never retain packet/event bytes.
            this.history=verdict==Verdict.PLAUSIBLE_WINDOW_UNVERIFIED?history:null;
            this.nativeReturn=verdict==Verdict.PLAUSIBLE_WINDOW_UNVERIFIED?nativeReturn:null;
            revision=this.history==null?0:history.revision();
            ordinal=this.nativeReturn==null?0:nativeReturn.nativeDispatchOrdinal();
            turn=this.history==null?0:history.publicTurn();
            splitCount=this.history==null?0:history.splitPairs();
            historyChunks=this.history==null?0:history.chunkCount();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long publicRevision(){return revision;}
        public long nativeReturnOrdinal(){return ordinal;}
        public int observedTurn(){return turn;}
        public int structurallyObservedSplitCount(){return splitCount;}
        public int historyChunkCount(){return historyChunks;}
        public boolean current(){
            synchronized(owner) {
                return !owner.withdrawn && owner.lastRevision==revision
                        && owner.lastOrdinal==ordinal && revision>0 && ordinal>0
                        && verdict==Verdict.PLAUSIBLE_WINDOW_UNVERIFIED
                        && history!=null && history.current()
                        && nativeReturn!=null && nativeReturn.current();
            }
        }
        public boolean sameUnderlyingEventProven(){return false;}
        public boolean exactSplitPacketLinked(){return false;}
        public boolean recipientDeliveryAuthenticated(){return false;}
        public boolean clientMessageProcessed(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean publicReadinessLeaseAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase620DispatchHistory[verdict="+verdict
                    +";revision="+revision+";ordinal="+ordinal
                    +";turn="+turn+";splitCount="+splitCount
                    +";historyChunks="+historyChunks
                    +";link=UNVERIFIED;authority=NONE]";
        }
    }

    public static final class Session {
        private Object battle;
        private long lastRevision,lastOrdinal;
        private int lastSplitCount;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        private Report fail(Verdict verdict){
            withdraw();
            return new Report(this,verdict,null,null);
        }
        private Report abstain(Verdict verdict){
            return new Report(this,verdict,null,null);
        }
        /**
         * Called from the already-read native battle-history observation site.
         * All source-identity and version checks occur before any comparison;
         * no stream may be fabricated from counts alone.
         */
        public synchronized Report inspect(Object originalBattle,
                PublicSplitRoutingGate.Report structural,
                NativeSplitDispatchObservation.Report nativeReturn) {
            if(withdrawn)return abstain(Verdict.WITHDRAWN);
            if(originalBattle==null || structural==null || !structural.current()
                    || !SOURCE.equals(structural.source()))
                return fail(Verdict.BAD_SOURCE);
            if(battle!=null && battle!=originalBattle)
                return fail(Verdict.BATTLE_MISMATCH);
            if(!structural.sameBattle(originalBattle))
                return fail(Verdict.BATTLE_MISMATCH);
            if(structural.revision()<lastRevision
                    || structural.splitPairs()<lastSplitCount)
                return fail(Verdict.REPLAYED_OR_STALE);
            // An absent optional return hook is not an error in normal live
            // battles. It must not become evidence of correspondence.
            if(nativeReturn==null
                    || nativeReturn.verdict()
                        ==NativeSplitDispatchObservation.Verdict.NO_NATIVE_CALLBACK_YET)
                return abstain(Verdict.MISSING_NATIVE_OBSERVATION);
            if(nativeReturn.verdict()
                    !=NativeSplitDispatchObservation.Verdict.NATIVE_METHOD_RETURN_OBSERVED
                    || !nativeReturn.current()
                    || !SOURCE.equals(nativeReturn.source()))
                return fail(Verdict.BAD_SOURCE);
            if(!nativeReturn.fromBattle(originalBattle))
                return fail(Verdict.BATTLE_MISMATCH);
            if(nativeReturn.nativeReturnOrdinal()<lastOrdinal)
                return fail(Verdict.REPLAYED_OR_STALE);
            if(structural.splitPairs()==0)
                return abstain(Verdict.MISSING_SPLIT_HISTORY);
            if(structural.publicTurn()!=nativeReturn.observedTurn()
                    || structural.publicTurn()<1)
                return fail(Verdict.TURN_MISMATCH);
            if(nativeReturn.historyChunksAtReturn()>structural.chunkCount()
                    || nativeReturn.historyChunksAtReturn()<0)
                return fail(Verdict.HISTORY_WINDOW_CONTRADICTION);
            final int newSplits=structural.splitPairs()-lastSplitCount;
            final long newReturns=nativeReturn.nativeReturnOrdinal()-lastOrdinal;
            if(lastRevision!=0 && structural.revision()<=lastRevision
                    && newReturns>0)
                return fail(Verdict.REPLAYED_OR_STALE);
            if(lastRevision!=0 && newReturns==0
                    && structural.revision()==lastRevision)
                return fail(Verdict.REPLAYED_OR_STALE);
            if(newSplits!=1 || newReturns!=1)
                return fail(Verdict.AMBIGUOUS_MULTIPLE_EVENTS);
            // Original 1-based chunk index, not a deduced receipt:
            // one-chunk native/history ambiguity is tolerated *only*
            // as an unverified temporal window. It is NOT a packet join.
            int splitIndex=structural.splitChunkOrdinal(structural.splitPairs()-1);
            int nativeIndex=nativeReturn.historyChunksAtReturn();
            if(Math.abs(splitIndex-nativeIndex)>1)
                return fail(Verdict.HISTORY_WINDOW_CONTRADICTION);
            if(!structural.current() || !nativeReturn.current())
                return fail(Verdict.REPLAYED_OR_STALE);
            battle=originalBattle;
            lastRevision=structural.revision();
            lastOrdinal=nativeReturn.nativeReturnOrdinal();
            lastSplitCount=structural.splitPairs();
            return new Report(this,Verdict.PLAUSIBLE_WINDOW_UNVERIFIED,
                    structural,nativeReturn);
        }
    }
}
