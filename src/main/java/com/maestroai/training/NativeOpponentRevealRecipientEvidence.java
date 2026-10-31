package com.maestroai.training;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleSwitchPokemonPacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleReplacePokemonPacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleTransformPokemonPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6.34: source-owned evidence of a NATIVE public packet BRANCH.
 *
 * Cobblemon sends one packet to the originating actor's side, and a separate
 * packet to the opposite side. A RETURN hook proves only that the native
 * method completed. It does not prove delivery, client decoding, displayed
 * species, true native Pokemon identity, public UUID or historical replay ID.
 * Never inspect packet contents, nicknames, active Pokemon or private requests.
 */
public final class NativeOpponentRevealRecipientEvidence {
    private NativeOpponentRevealRecipientEvidence(){}
    public static final String SCHEMA="phase6.34.native-public-reveal-recipient.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    public enum Kind {
        SWITCH_PUBLIC_BRANCH,
        ILLUSION_END_REPLACE_PUBLIC_BRANCH,
        TRANSFORM_PUBLIC_BRANCH,
        UNSUPPORTED
    }
    public enum Verdict {
        NATIVE_PUBLIC_BRANCH_RETURN_UNVERIFIED,
        PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED,
        REVEAL_IDENTITY_UNRESOLVED, NOT_OPPONENT_SOURCE,
        NO_NATIVE_RETURN, MISSING_CONTINUITY, WRONG_BATTLE,
        FOREIGN_SOURCE_ACTOR, INVALID_NATIVE_TOPOLOGY,
        UNSUPPORTED_PACKET_PAIR, STALE_SOURCE,
        SOURCE_CUSTODY_MISMATCH, REPLAYED_OR_REGRESSED, WITHDRAWN
    }

    /** Bounded metadata only; never public/hidden Pokemon or recipient. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final Kind kind;
        private final NativeSidedDispatchReachability.Report nativeSource;
        private final NativeOpponentSideProvenance.Report side;
        private final NativeOpponentPokemonContinuity.Report appearance;
        private final long ordinal,sourceReturnOrdinal,publicRevision;
        private final int turn,chunks;
        private Report(Session owner,Verdict verdict,
                NativeSidedDispatchReachability.Report source,
                NativeOpponentSideProvenance.Report side,
                NativeOpponentPokemonContinuity.Report appearance){
            this.owner=owner;this.verdict=verdict;
            boolean observational=verdict==Verdict.NATIVE_PUBLIC_BRANCH_RETURN_UNVERIFIED
                    || verdict==Verdict.PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED;
            nativeSource=observational?source:null;
            this.side=verdict==Verdict.PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED?side:null;
            this.appearance=this.side==null?null:appearance;
            kind=observational?owner.kind:Kind.UNSUPPORTED;
            ordinal=observational?owner.ordinal:0;
            sourceReturnOrdinal=nativeSource==null?0:nativeSource.observedSidedReturnOrdinal();
            publicRevision=this.appearance==null?0:appearance.originalRevision();
            turn=nativeSource==null?0:nativeSource.observedTurn();
            chunks=nativeSource==null?0:nativeSource.observedHistoryChunks();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public Kind publicBranchKind(){return current()?kind:Kind.UNSUPPORTED;}
        public long observedNativeReturnOrdinal(){return sourceReturnOrdinal;}
        public long publicHistoryRevision(){return publicRevision;}
        public int observedTurn(){return turn;}
        public int historyChunks(){return chunks;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.battle!=null
                        && nativeSource!=null
                        && ordinal>0 && owner.ordinal==ordinal
                        && kind==owner.kind
                        && nativeSource.fromBattle(owner.battle)
                        && sourceReturnOrdinal==owner.sourceReturnOrdinal
                        && turn==owner.lastTurn && chunks==owner.lastChunks
                        && (appearance==null || (
                            kind==Kind.SWITCH_PUBLIC_BRANCH
                            && side!=null && side.current()
                            && side.sharesOriginalSidedReturn(owner.battle,nativeSource)
                            && appearance.current()
                            && appearance.sharesOriginalSide(owner.battle,side)
                            && side.opponentPublicSide()==appearance.opponentPublicSide()
                            && publicRevision==owner.lastPublicRevision));
            }
        }
        public boolean opponentPublicBranchMethodReturned(){return current();}
        public boolean publicAppearanceSourceAligned(){
            return current() && appearance!=null;
        }
        public boolean authenticatedClientDelivery(){return false;}
        public boolean installedPacketReceptionConfirmed(){return false;}
        public boolean underlyingPokemonIdentityPublic(){return false;}
        public boolean opponentPokemonReentryIdentityProven(){return false;}
        public boolean illusionTruePokemonPubliclyRevealed(){return false;}
        public boolean transformOriginalIdentityProven(){return false;}
        public boolean publicPokemonUUIDAvailable(){return false;}
        public boolean historicalOpponentBuildMatched(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        @Override public String toString(){
            return "Phase634PublicRecipient[verdict="+verdict
                    +";kind="+publicBranchKind()
                    +";nativeOrdinal="+sourceReturnOrdinal
                    +";revision="+publicRevision
                    +";turn="+turn+";chunks="+chunks
                    +";delivery=UNVERIFIED;pokemon=UNKNOWN"
                    +";historicalJoin=BLOCKED;phase5=OFFLINE_ONLY;authority=NONE]";
        }
    }

    /** Lifecycle owner; source snapshots cannot outlive native callbacks. */
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

    /** One physical battle. A rejected source poisons the session. */
    public static final class Session {
        private Object battle;
        private NativeSidedDispatchReachability.Report originalReturn;
        private Kind kind=Kind.UNSUPPORTED;
        private long ordinal,sourceReturnOrdinal,lastPublicRevision;
        private int lastTurn=-1,lastChunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;battle=null;originalReturn=null;kind=Kind.UNSUPPORTED;
        }
        private Report reply(Verdict v){
            return new Report(this,v,null,null,null);
        }
        private Report fail(Verdict v){
            withdraw();return reply(v);
        }
        public synchronized boolean battleEnded(Object ended){
            if(battle==null || battle!=ended)return false;
            withdraw();return true;
        }
        /**
         * PRODUCTION entrypoint: invoked only after the existing optional
         * sendSidedUpdate RETURN hook issued its ORIGINAL Phase 6.24 receipt.
         * Opponent-originating source must be on the native opposite side
         * from Maestro's owning actor. No packet serialization or parsing.
         */
        public synchronized Report observeNativeReturn(
                PokemonBattle physicalBattle,AIBattleActor own,BattleActor source,
                NetworkPacket<?> allyPacket,NetworkPacket<?> opponentPacket,
                NativeSidedDispatchReachability.Report original){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || own==null || source==null
                    || allyPacket==null || opponentPacket==null)
                return fail(Verdict.INVALID_NATIVE_TOPOLOGY);
            if(source==own)return reply(Verdict.NOT_OPPONENT_SOURCE);
            try{
                var actors=new ArrayList<BattleActor>();
                for(var a:physicalBattle.getActors())actors.add(a);
                if(physicalBattle.getEnded() || actors.size()!=2
                        || (actors.get(0)!=own && actors.get(1)!=own)
                        || (actors.get(0)!=source && actors.get(1)!=source)
                        || source==own || own.getBattle()!=physicalBattle
                        || source.getBattle()!=physicalBattle
                        || own.getSide()==source.getSide()
                        || physicalBattle.getActor(own.getShowdownId())!=own
                        || physicalBattle.getActor(source.getShowdownId())!=source
                        || !((own.getShowdownId().equals("p1")
                            && source.getShowdownId().equals("p2"))
                            || (own.getShowdownId().equals("p2")
                            && source.getShowdownId().equals("p1"))))
                    return fail(Verdict.INVALID_NATIVE_TOPOLOGY);
                Kind observed=classify(allyPacket,opponentPacket);
                return observeSyntheticReturn(physicalBattle,List.copyOf(actors),
                        own,source,source,observed,original);
            }catch(RuntimeException bad){
                return fail(Verdict.INVALID_NATIVE_TOPOLOGY);
            }
        }
        private static Kind classify(Object ally,Object opposite){
            if(ally==null || opposite==null || ally==opposite
                    || ally.getClass()!=opposite.getClass())
                return Kind.UNSUPPORTED;
            if(ally.getClass()==BattleSwitchPokemonPacket.class)
                return Kind.SWITCH_PUBLIC_BRANCH;
            if(ally.getClass()==BattleReplacePokemonPacket.class)
                return Kind.ILLUSION_END_REPLACE_PUBLIC_BRANCH;
            if(ally.getClass()==BattleTransformPokemonPacket.class)
                return Kind.TRANSFORM_PUBLIC_BRANCH;
            return Kind.UNSUPPORTED;
        }
        /**
         * Package-private synthetic-only fixture path: event kind is supplied
         * by tests. It is NOT a native delivery proof or Minecraft test.
         */
        synchronized Report observeSyntheticReturn(Object physicalBattle,
                List<?> originalActors,Object ownActor,Object opponentActor,
                Object sourceActor,Kind observed,
                NativeSidedDispatchReachability.Report original){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(original==null)return reply(Verdict.NO_NATIVE_RETURN);
            if(!SOURCE.equals(original.source()) || !original.current()
                    || !original.fromBattle(physicalBattle)
                    || !original.matchesOriginalNativeActors(
                            physicalBattle,originalActors,sourceActor))
                return fail(Verdict.FOREIGN_SOURCE_ACTOR);
            if(originalActors==null || originalActors.size()!=2
                    || ownActor==null || opponentActor==null
                    || ownActor==opponentActor || sourceActor!=opponentActor
                    || (originalActors.get(0)!=ownActor && originalActors.get(1)!=ownActor)
                    || (originalActors.get(0)!=opponentActor
                        && originalActors.get(1)!=opponentActor))
                return fail(Verdict.INVALID_NATIVE_TOPOLOGY);
            // Other native sided callbacks are routine and are not
            // relevant to an opponent Pokemon reveal. Do not poison the
            // optional observer for an unrelated packet type.
            if(observed==null || observed==Kind.UNSUPPORTED)
                return reply(Verdict.UNSUPPORTED_PACKET_PAIR);
            if(original.observedSidedReturnOrdinal()<=sourceReturnOrdinal
                    || original.observedTurn()<lastTurn
                    || original.observedHistoryChunks()<lastChunks
                    || ordinal==Long.MAX_VALUE)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            battle=physicalBattle;originalReturn=original;
            kind=observed;ordinal++;
            sourceReturnOrdinal=original.observedSidedReturnOrdinal();
            lastTurn=original.observedTurn();lastChunks=original.observedHistoryChunks();
            lastPublicRevision=0;
            return new Report(this,Verdict.NATIVE_PUBLIC_BRANCH_RETURN_UNVERIFIED,
                    original,null,null);
        }

        /**
         * Exact original source chain through Phase 6.31/6.32/6.33.
         * An Illusion-ending replace or Transform can be observed as native
         * *routing intent*, but never as publicly authenticated true identity.
         */
        public synchronized Report inspect(Object physicalBattle,
                NativeOpponentSideProvenance.Report originalSide,
                NativeOpponentPokemonContinuity.Report appearance){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(originalReturn==null || !originalReturn.fromBattle(physicalBattle))
                return fail(Verdict.STALE_SOURCE);
            if(originalSide==null || appearance==null)
                return reply(Verdict.MISSING_CONTINUITY);
            if(!originalSide.current() || !appearance.current())
                return fail(Verdict.STALE_SOURCE);
            if(!originalSide.sharesOriginalSidedReturn(
                        physicalBattle,originalReturn)
                    || !appearance.sharesOriginalSide(physicalBattle,originalSide))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(kind!=Kind.SWITCH_PUBLIC_BRANCH)
                return fail(Verdict.REVEAL_IDENTITY_UNRESOLVED);
            if(originalSide.opponentPublicSide()!=appearance.opponentPublicSide()
                    || originalSide.opponentPublicSide()
                        ==NativeOpponentSideProvenance.Side.UNKNOWN)
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(appearance.originalRevision()<=lastPublicRevision
                    || appearance.nativeCallbackOrdinal()!=sourceReturnOrdinal
                    || appearance.observedTurn()!=lastTurn
                    || appearance.originalHistoryChunks()!=lastChunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            lastPublicRevision=appearance.originalRevision();
            Report accepted=new Report(this,
                    Verdict.PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED,
                    originalReturn,originalSide,appearance);
            if(!accepted.current())return fail(Verdict.STALE_SOURCE);
            return accepted;
        }
    }
}
