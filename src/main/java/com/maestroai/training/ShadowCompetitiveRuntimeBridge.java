package com.maestroai.training;

/**
 * Phase 6.28: Shadow-only preparation of the Phase 5 -> Phase 6 boundary.
 *
 * Historical competitive evidence can be checked OFFLINE for contract shape.
 * Only its non-identifying schema descriptor crosses into this read-only
 * source-epoch observer. Neither Phase 5 candidates nor replay identities,
 * ordinal appearances, move sets, probabilities, raw frames or packets do.
 *
 * IMPORTANT: matching source epochs cannot prove a public opponent actor;
 * a Phase 5 replay appearance is not a current Pokémon. No online Phase 5
 * consumer, tactical Shadow consumer or Legacy promotion is implemented.
 */
public final class ShadowCompetitiveRuntimeBridge {
    private ShadowCompetitiveRuntimeBridge(){}
    public static final String SCHEMA="phase6.28.shadow-competitive-boundary.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    /** Caller descriptions are NEVER an authenticated native/public identity. */
    public enum IdentityEvidence {
        UNKNOWN, PUBLIC_DISPLAY_ONLY, ILLUSION_OR_TRANSFORM_UNRESOLVED,
        CALLER_ASSERTED_STABLE_OPPONENT
    }
    public enum Verdict {
        PREPARED_UNBOUND_OFFLINE_ONLY,
        MISSING_OFFLINE_CONTRACT, MISSING_RUNTIME_SOURCE,
        INCOMPATIBLE_PIN, PUBLIC_ACTOR_NOT_AUTHENTICATED,
        UNTRUSTED_ACTOR_ASSERTION, WRONG_BATTLE,
        SOURCE_STALE, REPLAYED_OR_REGRESSED, WITHDRAWN
    }

    /**
     * Produced only by offline Phase 5 source-owned reports. Intentionally
     * retains NO reference to Report or historical candidate/appearance data.
     * No live battle/actor field or online-use flag exists.
     */
    public static final class HistoricalDescriptor {
        private final String schema;
        private final String sourceRevision;
        private final String mechanicsScope;
        private final String robustnessPolicy;
        private HistoricalDescriptor(PublicShadowBuildHandoff.Report source){
            schema=source.schema();
            sourceRevision=source.sourceRevision();
            mechanicsScope=source.mechanicsScope();
            robustnessPolicy=source.policySha256();
        }
        public String schema(){return schema;}
        public boolean offlineOnly(){return true;}
        public boolean currentBattleIdentityEstablished(){return false;}
        public boolean appearanceOrdinalIsPartyIdentity(){return false;}
        public boolean liveOpponentCandidatesAvailable(){return false;}
        public boolean calibratedResponseProbabilities(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        private boolean pinned(){
            return PublicShadowBuildHandoff.SCHEMA.equals(schema)
                    && PublicSpeciesBuildLegality.SOURCE_REVISION.equals(sourceRevision)
                    && CobblemonMechanics.SOURCE.equals(mechanicsScope)
                    && PublicBuildCandidateRobustness.frozenPolicySha256()
                        .equals(robustnessPolicy);
        }
        @Override public String toString(){
            return "Phase628Historical[offline-contract-only;identity=UNKNOWN;"
                    +"candidates=WITHHELD;authority=NONE]";
        }
    }

    /**
     * Offline preparation only. Callers cannot pass a current battle, source
     * turn, live actor or live observations into the Phase 5 consumer.
     * The original, private-constructor Phase 5 report is NOT retained.
     */
    public static HistoricalDescriptor describeOffline(
            PublicShadowBuildHandoff.Report historical,
            PublicShadowBuildHandoff.Pin pin,
            PublicCompetitiveConsumerContract.ConsumerRequest offlineRequest) {
        if(historical==null || pin==null || offlineRequest==null
                || !PublicShadowBuildHandoff.Pin.pinned().equals(pin)
                || offlineRequest.purpose()!=PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                || !PublicCompetitiveConsumerContract.ContractPin.pinned()
                        .equals(offlineRequest.pin())
                || offlineRequest.currentBattleBindingRequested()
                || offlineRequest.tacticalUseRequested()
                || !historical.originalReplaySignedCustodyRechecked()
                || historical.currentBattleEvidenceBound()
                || historical.legalActionsAvailable()
                || historical.calibratedOpponentProbabilities()
                || historical.speciesIdentityProven()
                || historical.completeLegalSetProven()
                || historical.phase6SearchAuthorized()
                || historical.shadowTacticalReady()
                || historical.livePromotionAuthorized())
            throw new IllegalArgumentException("Phase 6.28 offline-only contract unavailable");
        var descriptor=new HistoricalDescriptor(historical);
        if(!descriptor.pinned())
            throw new IllegalArgumentException("Phase 6.28 incompatible offline contract");
        return descriptor;
    }

    /** Public diagnostic: numeric source epoch only, never an identity join. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final IntegrationRuntimeReadinessGate.Report source;
        private final long revision, ordinal;
        private final int turn, chunks;
        private Report(Session owner,Verdict verdict,
                       IntegrationRuntimeReadinessGate.Report source){
            this.owner=owner;this.verdict=verdict;
            this.source=verdict==Verdict.PREPARED_UNBOUND_OFFLINE_ONLY?source:null;
            revision=this.source==null?0:source.publicRevision();
            ordinal=this.source==null?0:source.nativeReturnOrdinal();
            turn=this.source==null?0:source.observedTurn();
            chunks=this.source==null?0:source.historyChunks();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long publicRevision(){return revision;}
        public long nativeReturnOrdinal(){return ordinal;}
        public int observedTurn(){return turn;}
        public int observedHistoryChunks(){return chunks;}
        /** Only the runtime observational SOURCE remains current. */
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && source!=null && source.current()
                        && owner.battle!=null && source.fromBattle(owner.battle)
                        && revision==owner.lastRevision && ordinal==owner.lastOrdinal
                        && turn==owner.lastTurn && chunks==owner.lastChunks;
            }
        }
        public boolean historicalActorMatchedToCurrentOpponent(){return false;}
        public boolean publicOpponentActorAuthenticated(){return false;}
        public boolean displayAppearanceIsPersistentIdentity(){return false;}
        public boolean historicalCandidatesExposed(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean calibratedOpponentProbabilities(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean completeInstalledLegalSetProven(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase628Bridge[verdict="+verdict
                    +";revision="+revision+";ordinal="+ordinal
                    +";turn="+turn+";chunks="+chunks
                    +";actor=UNAUTHENTICATED;historicalJoin=BLOCKED;"
                    +"phase5=OFFLINE_ONLY;authority=NONE]";
        }
    }

    /**
     * Owned by a physical battle. This does NOT call the chooser or create a
     * current-battle Phase 5 ConsumerRequest (that remains prohibited).
     * Source progression and battle-end invalidate prior report freshness.
     */
    public static final class Session {
        private Object battle;
        private long lastRevision,lastOrdinal;
        private int lastTurn=-1,lastChunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        private Report verdict(Verdict kind){
            return new Report(this,kind,null);
        }
        private Report fail(Verdict kind){
            withdraw();return verdict(kind);
        }
        public synchronized Report inspect(Object physicalBattle,
                HistoricalDescriptor offline,
                IntegrationRuntimeReadinessGate.Report runtime,
                IdentityEvidence actorEvidence) {
            if(withdrawn)return verdict(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(offline==null)return verdict(Verdict.MISSING_OFFLINE_CONTRACT);
            if(!offline.pinned())return fail(Verdict.INCOMPATIBLE_PIN);
            if(actorEvidence==IdentityEvidence.CALLER_ASSERTED_STABLE_OPPONENT)
                return fail(Verdict.UNTRUSTED_ACTOR_ASSERTION);
            if(actorEvidence==null)return fail(Verdict.PUBLIC_ACTOR_NOT_AUTHENTICATED);
            if(runtime==null)return verdict(Verdict.MISSING_RUNTIME_SOURCE);
            if(!IntegrationRuntimeReadinessGate.SCHEMA.equals(runtime.schema())
                    || !SOURCE.equals(runtime.source())
                    || runtime.phase5OnlineAuthorized() || runtime.shadowSearchAuthorized()
                    || runtime.publicActorAuthenticated() || runtime.livePromotionAuthorized())
                return fail(Verdict.INCOMPATIBLE_PIN);
            if(!runtime.current())return fail(Verdict.SOURCE_STALE);
            if(!runtime.fromBattle(physicalBattle))return fail(Verdict.WRONG_BATTLE);
            if(runtime.verdict()
                    !=IntegrationRuntimeReadinessGate.Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED
                    || !runtime.nativeActorMembershipObserved()
                    || !runtime.publicPrivateStructureObserved()
                    || !runtime.sourceEpochAligned()
                    || runtime.nativeSplitEventLinked()
                    || runtime.perRecipientDeliveryAuthenticated()
                    || runtime.completeLegalActionsProven())
                return fail(Verdict.SOURCE_STALE);
            if(runtime.publicRevision()<=lastRevision
                    || runtime.nativeReturnOrdinal()<=lastOrdinal
                    || runtime.observedTurn()<lastTurn
                    || runtime.historyChunks()<lastChunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            // Only store numeric source epochs and the original battle object;
            // no Phase 5 candidate, appearance index, actor or replay text.
            battle=physicalBattle;
            lastRevision=runtime.publicRevision();
            lastOrdinal=runtime.nativeReturnOrdinal();
            lastTurn=runtime.observedTurn();
            lastChunks=runtime.historyChunks();
            return new Report(this,Verdict.PREPARED_UNBOUND_OFFLINE_ONLY,runtime);
        }
    }
}
