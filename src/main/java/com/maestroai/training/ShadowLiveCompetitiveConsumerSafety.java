package com.maestroai.training;

import java.util.EnumSet;
import java.util.Set;

/**
 * Phase 6.32: preparation-only consumer safety contract.
 *
 * This is NOT a live competitive intelligence consumer. The existing signed
 * Phase 5 report stays offline and is reduced by Phase 6.28 to a non-identifying
 * descriptor. Read-only Phase 6.31 side evidence is not a Pokémon identity.
 * Neither historical builds nor opponent probabilities are exposed here.
 *
 * The positive result means only that source-owned observational receipts are
 * compatible at the same original source epoch. Every authority remains false.
 */
public final class ShadowLiveCompetitiveConsumerSafety {
    private ShadowLiveCompetitiveConsumerSafety(){}
    public static final int VERSION=1;
    public static final String SCHEMA="phase6.32.shadow-live-consumer-safety.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    public enum Purpose {
        PREPARATION_REVIEW_ONLY, LIVE_COMPETITIVE_CONSUMPTION,
        SHADOW_TACTICAL_USE, LEGACY_CHOICE_CONTROL
    }
    public enum Verdict {
        SOURCE_EPOCH_AUDITED_OFFLINE_ONLY,
        MISSING_OFFLINE_CUSTODY, MISSING_RUNTIME_EVIDENCE,
        WRONG_BATTLE, INCOMPATIBLE_PIN, STALE_SOURCE,
        UNBOUND_OPPONENT_SIDE, SOURCE_CUSTODY_MISMATCH,
        IDENTITY_UNCERTAIN, REPLAYED_OR_REGRESSED,
        UNAUTHORIZED_USE, WITHDRAWN
    }
    /** Requirements NOT satisfied even when source receipt compatibility holds. */
    public enum MissingCapability {
        HISTORICAL_APPEARANCE_TO_CURRENT_POKEMON_IDENTITY,
        CALIBRATED_OPPONENT_RESPONSE_PROBABILITIES,
        COMPLETE_INSTALLED_LEGAL_ACTIONS,
        RUNTIME_OBSERVATION_AND_PRIVATE_DELIVERY_ACCEPTANCE,
        APPROVED_VERSIONED_PHASE5_ONLINE_CONSUMER,
        SHADOW_TACTICAL_AUTHORIZATION,
        LEGACY_PROMOTION_AUTHORIZATION
    }

    /** Version pin is a compatibility check, never an authorization token. */
    public record ContractPin(int version,String installedSource,
                              String historicalSchema,String historicalBridgeSchema,
                              String identitySchema,String opponentSideSchema,
                              PublicCompetitiveConsumerContract.ContractPin phase5Pin) {
        public ContractPin {
            if(version!=VERSION || !SOURCE.equals(installedSource)
                    || !PublicShadowBuildHandoff.SCHEMA.equals(historicalSchema)
                    || !ShadowCompetitiveRuntimeBridge.SCHEMA.equals(historicalBridgeSchema)
                    || !PublicOpponentIdentityContinuity.SCHEMA.equals(identitySchema)
                    || !NativeOpponentSideProvenance.SCHEMA.equals(opponentSideSchema)
                    || !PublicCompetitiveConsumerContract.ContractPin.pinned()
                            .equals(phase5Pin))
                throw invalid();
        }
        public static ContractPin pinned() {
            return new ContractPin(VERSION,SOURCE,PublicShadowBuildHandoff.SCHEMA,
                    ShadowCompetitiveRuntimeBridge.SCHEMA,
                    PublicOpponentIdentityContinuity.SCHEMA,
                    NativeOpponentSideProvenance.SCHEMA,
                    PublicCompetitiveConsumerContract.ContractPin.pinned());
        }
    }

    /**
     * Only preparation is representable. Explicit live or tactical requests
     * are rejected at creation; a pin or an opt-in system property does not
     * grant permission to weaken the existing Phase 5 offline contract.
     */
    public record Request(Purpose purpose,ContractPin pin,
                          boolean currentOpponentBindingRequested,
                          boolean historicalCandidateExportRequested,
                          boolean tacticalDecisionRequested) {
        public Request {
            if(purpose!=Purpose.PREPARATION_REVIEW_ONLY || pin==null
                    || currentOpponentBindingRequested
                    || historicalCandidateExportRequested
                    || tacticalDecisionRequested)throw invalid();
        }
        public static Request preparationPinned() {
            return new Request(Purpose.PREPARATION_REVIEW_ONLY,
                    ContractPin.pinned(),false,false,false);
        }
    }

    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.32 unauthorized or unpinned use");
    }
    private static final Set<MissingCapability> BLOCKERS=Set.copyOf(
            EnumSet.allOf(MissingCapability.class));

    /** Original source freshness only. NEVER an authenticated opponent build. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final NativeOpponentSideProvenance.Report nativeSide;
        private final ShadowCompetitiveRuntimeBridge.Report originalBridge;
        private final long sourceRevision,nativeReturnOrdinal;
        private final int turn,chunks;
        private Report(Session owner,Verdict verdict,
                       NativeOpponentSideProvenance.Report nativeSide,
                       ShadowCompetitiveRuntimeBridge.Report originalBridge){
            this.owner=owner;this.verdict=verdict;
            boolean eligible=verdict==Verdict.SOURCE_EPOCH_AUDITED_OFFLINE_ONLY;
            this.nativeSide=eligible?nativeSide:null;
            this.originalBridge=eligible?originalBridge:null;
            sourceRevision=this.nativeSide==null?0:nativeSide.publicHistoryRevision();
            nativeReturnOrdinal=this.nativeSide==null?0:nativeSide.nativeReturnOrdinal();
            turn=this.nativeSide==null?0:nativeSide.observedTurn();
            chunks=this.nativeSide==null?0:nativeSide.historyChunks();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long sourceRevision(){return sourceRevision;}
        public long nativeReturnOrdinal(){return nativeReturnOrdinal;}
        public int observedTurn(){return turn;}
        public int historyChunks(){return chunks;}
        public Set<MissingCapability> missingCapabilities(){return BLOCKERS;}

        /**
         * A positive report expires when EITHER original source session is
         * revoked, advanced, or its native/public custody linkage disappears.
         * "current" is NOT permission to consume historical candidate data.
         */
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.battle!=null
                        && nativeSide!=null && originalBridge!=null
                        && nativeSide.sharesOriginalBridge(owner.battle,originalBridge)
                        && nativeSide.publicSideSourceAligned()
                        && nativeSide.opponentPublicSide()!=
                            NativeOpponentSideProvenance.Side.UNKNOWN
                        && originalBridge.publicContinuitySourceAligned()
                        && sourceRevision==owner.lastRevision
                        && nativeReturnOrdinal==owner.lastNativeOrdinal
                        && turn==owner.lastTurn && chunks==owner.lastChunks;
            }
        }
        /** Source-owned p1/p2 ONLY, never a team/party-slot identity. */
        public NativeOpponentSideProvenance.Side sourceBoundOpponentSide(){
            return current()?nativeSide.opponentPublicSide():
                    NativeOpponentSideProvenance.Side.UNKNOWN;
        }
        public boolean nativePublicSourceChainAligned(){return current();}
        public boolean historicalOpponentPokemonIdentified(){return false;}
        public boolean publicDisplayIsStablePokemonIdentity(){return false;}
        public boolean historicalReplayAppearanceMatched(){return false;}
        public boolean historicalBuildCandidatesAvailable(){return false;}
        public boolean opponentResponseProbabilitiesCalibrated(){return false;}
        public boolean completeLegalActionsProven(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean onlineConsumptionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean legacyDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase632ConsumerSafety[verdict="+verdict
                    +";revision="+sourceRevision+";callback="+nativeReturnOrdinal
                    +";turn="+turn+";chunks="+chunks
                    +";publicSide="+sourceBoundOpponentSide()
                    +";pokemon=UNKNOWN;historicalJoin=BLOCKED"
                    +";phase5=OFFLINE_ONLY;authority=NONE]";
        }
    }

    /** One original physical battle; no chooser, action, or online Phase 5 API. */
    public static final class Session {
        private Object battle;
        private long lastRevision,lastNativeOrdinal;
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

        /**
         * Caller cannot fabricate a usable input from metadata. Must supply
         * the ORIGINAL source-issued Phase 6.31 Report AND the SAME original
         * Phase 6.30 Report that its owner accepted. Historical descriptor
         * carries no replay identity or candidate. No Phase 5 online call.
         */
        public synchronized Report inspect(Object physicalBattle,
                ShadowCompetitiveRuntimeBridge.HistoricalDescriptor historical,
                NativeOpponentSideProvenance.Report side,
                ShadowCompetitiveRuntimeBridge.Report originalBridge,
                Request request){
            if(withdrawn)return abstain(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(request==null || !Request.preparationPinned().equals(request))
                return fail(Verdict.UNAUTHORIZED_USE);
            if(historical==null)
                return battle==null?abstain(Verdict.MISSING_OFFLINE_CUSTODY)
                        :fail(Verdict.MISSING_OFFLINE_CUSTODY);
            if(!PublicShadowBuildHandoff.SCHEMA.equals(historical.schema())
                    || !historical.offlineOnly()
                    || historical.currentBattleIdentityEstablished()
                    || historical.appearanceOrdinalIsPartyIdentity()
                    || historical.liveOpponentCandidatesAvailable()
                    || historical.calibratedResponseProbabilities()
                    || historical.phase5OnlineAuthorized()
                    || historical.shadowSearchAuthorized()
                    || historical.livePromotionAuthorized())
                return fail(Verdict.INCOMPATIBLE_PIN);
            if(side==null || originalBridge==null)
                return battle==null?abstain(Verdict.MISSING_RUNTIME_EVIDENCE)
                        :fail(Verdict.MISSING_RUNTIME_EVIDENCE);
            if(!NativeOpponentSideProvenance.SCHEMA.equals(side.schema())
                    || !ShadowCompetitiveRuntimeBridge.SCHEMA
                        .equals(originalBridge.schema())
                    || !SOURCE.equals(side.source())
                    || !SOURCE.equals(originalBridge.source()))
                return fail(Verdict.INCOMPATIBLE_PIN);
            if(!side.current() || !originalBridge.current())
                return fail(Verdict.STALE_SOURCE);
            if(side.verdict()!=
                    NativeOpponentSideProvenance.Verdict
                            .OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND
                    || !side.publicSideSourceAligned()
                    || !side.nativeSideMembershipObserved()
                    || side.opponentPublicSide()==NativeOpponentSideProvenance.Side.UNKNOWN)
                return fail(Verdict.UNBOUND_OPPONENT_SIDE);
            if(!side.sharesOriginalBridge(physicalBattle,originalBridge))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(!originalBridge.publicContinuitySourceAligned())
                return fail(Verdict.IDENTITY_UNCERTAIN);
            if(side.persistentPokemonIdentityAuthenticated()
                    || side.partySlotIdentityProven()
                    || side.historicalOpponentBuildMatched()
                    || side.phase5OnlineAuthorized()
                    || side.shadowSearchAuthorized()
                    || originalBridge.historicalActorMatchedToCurrentOpponent()
                    || originalBridge.phase5OnlineAuthorized()
                    || originalBridge.shadowSearchAuthorized())
                return fail(Verdict.IDENTITY_UNCERTAIN);
            if(side.publicHistoryRevision()<=lastRevision
                    || side.nativeReturnOrdinal()<=lastNativeOrdinal
                    || side.observedTurn()<lastTurn
                    || side.historyChunks()<lastChunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);

            battle=physicalBattle;
            lastRevision=side.publicHistoryRevision();
            lastNativeOrdinal=side.nativeReturnOrdinal();
            lastTurn=side.observedTurn();
            lastChunks=side.historyChunks();
            var accepted=new Report(this,Verdict.SOURCE_EPOCH_AUDITED_OFFLINE_ONLY,
                    side,originalBridge);
            if(!accepted.current())return fail(Verdict.STALE_SOURCE);
            return accepted;
        }
    }
}
