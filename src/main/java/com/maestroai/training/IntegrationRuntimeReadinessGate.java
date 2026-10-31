package com.maestroai.training;

/**
 * Independent Maestro4567 source-level runtime-readiness gate.
 *
 * Correlates the ORIGINAL Phase 6.24 sided RETURN observer and Phase 6.16
 * public/private STRUCTURAL history observer at one in-process source epoch.
 * A sided RETURN is NOT a sendSplitUpdate callback: even a positive result
 * cannot link it to a Showdown split event, prove public actor identity,
 * packet delivery, installed callback activation or legal actions.
 *
 * Reports expose bounded numeric metadata only; no packets, identities,
 * original history, private branches or callable tactical actions.
 */
public final class IntegrationRuntimeReadinessGate {
    private IntegrationRuntimeReadinessGate(){}
    public static final String SCHEMA="maestro4567.runtime-readiness-source-gate.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    public enum Verdict {
        SOURCE_WINDOWS_ALIGNED_UNVERIFIED, NO_NATIVE_RETURN, NO_SPLIT_ROUTE,
        SOURCE_UNAVAILABLE, WRONG_BATTLE, SOURCE_STALE,
        TURN_OR_EPOCH_MISMATCH, REPLAYED_OR_DUPLICATE, WITHDRAWN
    }

    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final NativeSidedDispatchReachability.Report sided;
        private final PublicSplitRoutingGate.Report routing;
        private final long revision, ordinal;
        private final int turn, chunks;
        private Report(Session owner,Verdict verdict,
                NativeSidedDispatchReachability.Report sided,
                PublicSplitRoutingGate.Report routing){
            this.owner=owner;this.verdict=verdict;
            this.sided=verdict==Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED?sided:null;
            this.routing=verdict==Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED?routing:null;
            revision=this.routing==null?0:routing.revision();
            ordinal=this.sided==null?0:sided.observedSidedReturnOrdinal();
            turn=this.routing==null?0:routing.publicTurn();
            chunks=this.routing==null?0:routing.chunkCount();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long publicRevision(){return revision;}
        public long nativeReturnOrdinal(){return ordinal;}
        public int observedTurn(){return turn;}
        public int historyChunks(){return chunks;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn
                        && verdict==Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED
                        && owner.battle!=null && owner.lastRevision==revision
                        && owner.lastOrdinal==ordinal && owner.lastTurn==turn
                        && owner.lastChunks==chunks && routing!=null && sided!=null
                        && routing.sameBattle(owner.battle)
                        && sided.fromBattle(owner.battle)
                        && routing.revision()==revision
                        && routing.publicTurn()==sided.observedTurn()
                        && routing.chunkCount()==sided.observedHistoryChunks()
                        && routing.splitPublicExtractionStructurallyMatched();
            }
        }
        /** Exact original in-process battle, not a public actor join. */
        boolean fromBattle(Object originalBattle){
            synchronized(owner){
                return originalBattle!=null && owner.battle==originalBattle && current();
            }
        }
        /**
         * Exact ORIGINAL Phase 6.16 routing receipt, not same-looking revision,
         * turn, split count, or a caller-created identity assertion.
         * Package-private: this is read-only source custody, not actor proof.
         */
        boolean sharesOriginalRouting(PublicSplitRoutingGate.Report original){
            synchronized(owner){
                return original!=null && routing==original && current()
                        && original.current();
            }
        }
        /** Exact original Phase 6.24 source report, not same return ordinal. */
        boolean sharesOriginalNativeReturn(NativeSidedDispatchReachability.Report source){
            synchronized(owner){
                return source!=null && sided==source && current()
                        && source.current();
            }
        }
        /** Source-side actor membership only, not public actor authentication. */
        public boolean nativeActorMembershipObserved(){return current();}
        /** Structural private-first/public-second parsing only. */
        public boolean publicPrivateStructureObserved(){return current();}
        public boolean sourceEpochAligned(){return current();}
        public boolean nativeHookInstalledProven(){return false;}
        public boolean publicActorAuthenticated(){return false;}
        public boolean nativeSplitEventLinked(){return false;}
        public boolean perRecipientDeliveryAuthenticated(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean installedMinecraftAccepted(){return false;}
        public boolean completeLegalActionsProven(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Maestro4567RuntimeGate[verdict="+verdict
                    +";revision="+revision+";ordinal="+ordinal
                    +";turn="+turn+";chunks="+chunks
                    +";eventLink=UNVERIFIED;delivery=UNVERIFIED;authority=NONE]";
        }
    }

    /** Per-battle, fail-closed, read-only observer. Never invokes the chooser. */
    public static final class Session {
        private Object battle;
        private long lastRevision,lastOrdinal;
        private int lastTurn=-1,lastChunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        private Report abstain(Verdict verdict){
            return new Report(this,verdict,null,null);
        }
        private Report fail(Verdict verdict){
            withdraw();
            return abstain(verdict);
        }
        public synchronized Report inspect(Object originalBattle,
                NativeSidedDispatchReachability.Report nativeReturn,
                PublicSplitRoutingGate.Report publicPrivate){
            if(withdrawn)return abstain(Verdict.WITHDRAWN);
            if(originalBattle==null || battle!=null && battle!=originalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(publicPrivate==null)return abstain(Verdict.SOURCE_UNAVAILABLE);
            if(!publicPrivate.current() || !SOURCE.equals(publicPrivate.source()))
                return fail(Verdict.SOURCE_STALE);
            if(!publicPrivate.sameBattle(originalBattle))
                return fail(Verdict.WRONG_BATTLE);
            if(publicPrivate.verdict()!=PublicSplitRoutingGate.Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                    || publicPrivate.splitPairs()<1)
                return abstain(Verdict.NO_SPLIT_ROUTE);
            if(nativeReturn==null
                    || nativeReturn.verdict()==NativeSidedDispatchReachability.Verdict.NO_NATIVE_SIDED_CALLBACK_YET)
                return abstain(Verdict.NO_NATIVE_RETURN);
            if(!nativeReturn.current() || !SOURCE.equals(nativeReturn.source())
                    || nativeReturn.verdict()!=NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED)
                return fail(Verdict.SOURCE_STALE);
            if(!nativeReturn.fromBattle(originalBattle))
                return fail(Verdict.WRONG_BATTLE);
            if(nativeReturn.actorCount()<2
                    || nativeReturn.spectatorRouting()==NativeSidedDispatchReachability.SpectatorRouting.NOT_OBSERVED
                    || nativeReturn.originalShowdownSplitLinked()
                    || nativeReturn.clientDeliveryAuthenticated()
                    || publicPrivate.liveSourceDeliveryAuthenticated()
                    || publicPrivate.privatePayloadExportable())
                return fail(Verdict.SOURCE_UNAVAILABLE);
            if(publicPrivate.publicTurn()<0 || publicPrivate.chunkCount()<1
                    || nativeReturn.observedTurn()!=publicPrivate.publicTurn()
                    || nativeReturn.observedHistoryChunks()!=publicPrivate.chunkCount())
                return fail(Verdict.TURN_OR_EPOCH_MISMATCH);
            if(publicPrivate.revision()<=lastRevision
                    || nativeReturn.observedSidedReturnOrdinal()<=lastOrdinal
                    || publicPrivate.publicTurn()<lastTurn
                    || publicPrivate.chunkCount()<lastChunks)
                return fail(Verdict.REPLAYED_OR_DUPLICATE);
            if(!nativeReturn.current() || !publicPrivate.current())
                return fail(Verdict.SOURCE_STALE);
            battle=originalBattle;
            lastRevision=publicPrivate.revision();
            lastOrdinal=nativeReturn.observedSidedReturnOrdinal();
            lastTurn=publicPrivate.publicTurn();
            lastChunks=publicPrivate.chunkCount();
            return new Report(this,Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED,
                    nativeReturn,publicPrivate);
        }
    }
}
