package com.maestroai.training;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Phase 6.31: source-owned, native/public OPPONENT-SIDE provenance.
 * The pinned Cobblemon BattleActor.showdownId is a protocol-side ID; it is
 * not a Pokemon identity, party slot, public nickname or recipient delivery.
 *
 * Native intake reads actor refs, side refs and showdownIds directly from
 * PokemonBattle during the optional sendSidedUpdate RETURN hook. A later
 * read-only inspection requires exact ORIGINAL Phase 6.24/6.29/6.30 reports.
 * Neither method touches the chooser or enables Phase 5 online consumption.
 */
public final class NativeOpponentSideProvenance {
    private NativeOpponentSideProvenance(){}
    public static final String SCHEMA="phase6.31.native-opponent-side.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    public enum Side { UNKNOWN, P1, P2 }
    public enum Verdict {
        NATIVE_SIDE_OBSERVED_UNLINKED,
        OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND,
        MISSING_NATIVE_RETURN, MISSING_CONTINUITY,
        MISSING_BRIDGE, SOURCE_UNAVAILABLE, WRONG_BATTLE,
        ACTOR_SET_UNTRUSTED, SIDE_TOPOLOGY_AMBIGUOUS,
        SOURCE_CUSTODY_MISMATCH, SOURCE_STALE,
        IDENTITY_UNCERTAIN, REPLAYED_OR_REGRESSED, WITHDRAWN
    }

    /** Source-owned report, bounded and redacted. No native actor export. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final NativeSidedDispatchReachability.Report nativeReturn;
        private final IntegrationRuntimeReadinessGate.Report runtime;
        private final PublicOpponentIdentityContinuity.Report identity;
        private final ShadowCompetitiveRuntimeBridge.Report bridge;
        private final long nativeOrdinal,historyRevision;
        private final int turn,chunks;
        private final Side opponentSide;
        private Report(Session owner,Verdict verdict,
                NativeSidedDispatchReachability.Report nativeReturn,
                IntegrationRuntimeReadinessGate.Report runtime,
                PublicOpponentIdentityContinuity.Report identity,
                ShadowCompetitiveRuntimeBridge.Report bridge){
            this.owner=owner;this.verdict=verdict;
            boolean eligible=verdict==Verdict.NATIVE_SIDE_OBSERVED_UNLINKED
                    || verdict==Verdict.OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND;
            this.nativeReturn=eligible?nativeReturn:null;
            this.runtime=verdict==Verdict.OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND
                    ?runtime:null;
            this.identity=this.runtime==null?null:identity;
            this.bridge=this.runtime==null?null:bridge;
            nativeOrdinal=this.nativeReturn==null?0:nativeReturn.observedSidedReturnOrdinal();
            historyRevision=this.runtime==null?0:runtime.publicRevision();
            turn=this.nativeReturn==null?0:nativeReturn.observedTurn();
            chunks=this.nativeReturn==null?0:nativeReturn.observedHistoryChunks();
            opponentSide=eligible?owner.opponentSide:Side.UNKNOWN;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long nativeReturnOrdinal(){return nativeOrdinal;}
        public long publicHistoryRevision(){return historyRevision;}
        public int observedTurn(){return turn;}
        public int historyChunks(){return chunks;}
        public Side opponentPublicSide(){
            return current() && runtime!=null?opponentSide:Side.UNKNOWN;
        }
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.battle!=null
                        && nativeReturn!=null
                        && nativeReturn.fromBattle(owner.battle)
                        && nativeOrdinal==owner.lastNativeOrdinal
                        && turn==owner.lastTurn && chunks==owner.lastChunks
                        && owner.opponentSide==opponentSide
                        && (runtime==null || (
                            runtime.fromBattle(owner.battle)
                            && historyRevision==owner.lastHistoryRevision
                            && runtime.sharesOriginalNativeReturn(nativeReturn)
                            && identity!=null && bridge!=null
                            && identity.sharesRuntimeRouting(owner.battle,runtime)
                            && bridge.sharesOriginalSourceReceipts(owner.battle,
                                    runtime,identity)));
            }
        }
        /**
         * Phase 6.32 original receipt custody: the *same* original 6.30
         * source-owned report must reach the consumer. A matching battle,
         * turn, revision, or public-side value is not enough.
         */
        boolean sharesOriginalBridge(Object physicalBattle,
                ShadowCompetitiveRuntimeBridge.Report candidate){
            synchronized(owner){
                return physicalBattle!=null && owner.battle==physicalBattle
                        && bridge!=null && bridge==candidate && current()
                        && candidate.publicContinuitySourceAligned()
                        && !candidate.phase5OnlineAuthorized()
                        && !candidate.shadowSearchAuthorized();
            }
        }
        /** Same original native actor/topology observed, no Pokemon proof. */
        public boolean nativeSideMembershipObserved(){return current();}
        /** True ONLY for original 6.24/6.29/6.30 current receipt linkage. */
        public boolean publicSideSourceAligned(){
            return current() && runtime!=null;
        }
        public boolean persistentPokemonIdentityAuthenticated(){return false;}
        public boolean partySlotIdentityProven(){return false;}
        public boolean historicalOpponentBuildMatched(){return false;}
        public boolean publicActorPokemonIdentityProven(){return false;}
        public boolean clientPacketDelivered(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean calibratedOpponentProbabilities(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        @Override public String toString(){
            return "Phase631NativeSide[verdict="+verdict
                    +";nativeOrdinal="+nativeOrdinal
                    +";historyRevision="+historyRevision
                    +";turn="+turn+";chunks="+chunks
                    +";opponentSide="+opponentPublicSide()
                    +";pokemonIdentity=UNKNOWN;phase5=OFFLINE_ONLY;"
                    +"delivery=UNVERIFIED;authority=NONE]";
        }
    }

    /**
     * Retain a sided callback that arrives before Legacy's first chooser
     * initializes per-battle memory. A late ended battle cannot overwrite
     * the next physical battle's observer.
     */
    public static final class BattleScope {
        private Object active;
        private Session session=new Session();
        public synchronized Session forBattle(Object battle){
            if(battle==null){session.withdraw();return session;}
            if(active!=battle){
                session.withdraw();
                session=new Session();
                active=battle;
            }
            return session;
        }
        public synchronized boolean end(Object battle){
            return battle!=null && active==battle && session.battleEnded(battle);
        }
        public synchronized void withdraw(){session.withdraw();}
    }

    /** One physical battle; only source-issued refs, never actor names. */
    public static final class Session {
        private Object battle;
        private Object ownActor,opponentActor,sourceActor;
        private Side opponentSide=Side.UNKNOWN;
        private NativeSidedDispatchReachability.Report nativeReturn;
        private long lastNativeOrdinal,lastHistoryRevision;
        private int lastTurn=-1,lastChunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;battle=null;ownActor=null;opponentActor=null;
            sourceActor=null;nativeReturn=null;opponentSide=Side.UNKNOWN;
        }
        private Report reply(Verdict kind){
            return new Report(this,kind,null,null,null,null);
        }
        private Report fail(Verdict kind){
            withdraw();return reply(kind);
        }
        private Report nativeReceipt(){
            return new Report(this,Verdict.NATIVE_SIDE_OBSERVED_UNLINKED,
                    nativeReturn,null,null,null);
        }
        /**
         * Production: only call from the installed native sided RETURN path,
         * with the SAME source-owned Phase 6.24 callback receipt.
         * Read showdownId, native actor membership and getSide() from the
         * actual battle, not from client-provided text.
         */
        public synchronized Report observeFromNativeCallback(
                PokemonBattle originalBattle,AIBattleActor own,
                BattleActor callbackSource,
                NativeSidedDispatchReachability.Report receipt){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(originalBattle==null || own==null || callbackSource==null)
                return fail(Verdict.SOURCE_UNAVAILABLE);
            try{
                var actors=new ArrayList<BattleActor>();
                for(var actor:originalBattle.getActors())actors.add(actor);
                if(actors.size()!=2
                        || (actors.get(0)!=own && actors.get(1)!=own)
                        || own.getBattle()!=originalBattle
                        || originalBattle.getEnded())
                    return fail(Verdict.SIDE_TOPOLOGY_AMBIGUOUS);
                BattleActor other=actors.get(0)==own?actors.get(1):actors.get(0);
                if(other==own || other.getBattle()!=originalBattle
                        || originalBattle.getActor(own.getShowdownId())!=own
                        || originalBattle.getActor(other.getShowdownId())!=other
                        || own.getSide()==other.getSide())
                    return fail(Verdict.SIDE_TOPOLOGY_AMBIGUOUS);
                return observeSyntheticNativeReturn(originalBattle,
                        List.copyOf(actors),own,other,callbackSource,
                        own.getShowdownId(),other.getShowdownId(),
                        own.getSide(),other.getSide(),receipt);
            }catch(RuntimeException unavailable){
                return fail(Verdict.SOURCE_UNAVAILABLE);
            }
        }
        /**
         * Package-private test fixture path: same checks but no claim this
         * synthetic call was made by Minecraft. Never a live chooser API.
         */
        synchronized Report observeSyntheticNativeReturn(Object physicalBattle,
                List<?> originalActors,Object own,Object opposite,
                Object callbackSource,String ownShowdownSide,
                String opponentShowdownSide,Object ownNativeSide,
                Object opponentNativeSide,
                NativeSidedDispatchReachability.Report source){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(source==null)return reply(Verdict.MISSING_NATIVE_RETURN);
            if(!SOURCE.equals(source.source()) || !source.current()
                    || !source.matchesOriginalNativeActors(
                            physicalBattle,originalActors,callbackSource))
                return fail(Verdict.ACTOR_SET_UNTRUSTED);
            if(originalActors==null || originalActors.size()!=2
                    || own==null || opposite==null || own==opposite
                    || callbackSource==null || ownNativeSide==null
                    || opponentNativeSide==null
                    || ownNativeSide==opponentNativeSide
                    || (originalActors.get(0)!=own && originalActors.get(1)!=own)
                    || (originalActors.get(0)!=opposite && originalActors.get(1)!=opposite)
                    || originalActors.get(0)==originalActors.get(1))
                return fail(Verdict.SIDE_TOPOLOGY_AMBIGUOUS);
            if(!("p1".equals(ownShowdownSide)&&"p2".equals(opponentShowdownSide))
                    && !("p2".equals(ownShowdownSide)
                            && "p1".equals(opponentShowdownSide)))
                return fail(Verdict.SIDE_TOPOLOGY_AMBIGUOUS);
            if(source.observedSidedReturnOrdinal()<=lastNativeOrdinal
                    || source.observedTurn()<lastTurn
                    || source.observedHistoryChunks()<lastChunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            if(battle!=null && (ownActor!=own || opponentActor!=opposite
                    || opponentSide!=("p1".equals(opponentShowdownSide)
                            ?Side.P1:Side.P2)))
                return fail(Verdict.SIDE_TOPOLOGY_AMBIGUOUS);
            battle=physicalBattle;ownActor=own;opponentActor=opposite;
            sourceActor=callbackSource;
            opponentSide="p1".equals(opponentShowdownSide)?Side.P1:Side.P2;
            nativeReturn=source;
            lastNativeOrdinal=source.observedSidedReturnOrdinal();
            lastTurn=source.observedTurn();
            lastChunks=source.observedHistoryChunks();
            lastHistoryRevision=0;
            return nativeReceipt();
        }

        /**
         * Phase 6.31 independent read-only acceptance: EXACT native receipt
         * and the same original 6.16 routing used by 6.29 AND 6.30.
         * No Phase 5 online candidate can be constructed here.
         */
        public synchronized Report inspect(Object physicalBattle,
                IntegrationRuntimeReadinessGate.Report runtime,
                PublicOpponentIdentityContinuity.Report continuity,
                ShadowCompetitiveRuntimeBridge.Report bridge){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(nativeReturn==null || !nativeReturn.fromBattle(physicalBattle))
                return fail(Verdict.SOURCE_STALE);
            if(runtime==null || continuity==null)
                return reply(Verdict.MISSING_CONTINUITY);
            if(bridge==null)return reply(Verdict.MISSING_BRIDGE);
            if(!runtime.fromBattle(physicalBattle)
                    || !continuity.samePhysicalBattle(physicalBattle)
                    || !bridge.current())
                return fail(Verdict.SOURCE_STALE);
            if(!runtime.sharesOriginalNativeReturn(nativeReturn)
                    || !continuity.sharesRuntimeRouting(physicalBattle,runtime)
                    || !bridge.sharesOriginalSourceReceipts(
                            physicalBattle,runtime,continuity))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(continuity.verdict()
                    !=PublicOpponentIdentityContinuity.Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                    || !continuity.uncertainty().equals(
                            Set.of(PublicOpponentIdentityContinuity.Uncertainty.PUBLIC_DISPLAY_ONLY))
                    || !bridge.publicContinuitySourceAligned())
                return fail(Verdict.IDENTITY_UNCERTAIN);
            if(runtime.nativeReturnOrdinal()<=0
                    || runtime.nativeReturnOrdinal()!=lastNativeOrdinal
                    || runtime.publicRevision()<=lastHistoryRevision
                    || runtime.observedTurn()!=lastTurn
                    || runtime.historyChunks()!=lastChunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            lastHistoryRevision=runtime.publicRevision();
            return new Report(this,Verdict.OPPONENT_PUBLIC_SIDE_SOURCE_ALIGNED_UNBOUND,
                    nativeReturn,runtime,continuity,bridge);
        }
        /** Late unrelated end never revokes another active physical battle. */
        public synchronized boolean battleEnded(Object ended){
            if(battle==null || ended!=battle)return false;
            withdraw();return true;
        }
    }
}
