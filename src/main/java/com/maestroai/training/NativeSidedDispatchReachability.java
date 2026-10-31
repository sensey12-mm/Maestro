package com.maestroai.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Phase 6.24: read-only witness for PokemonBattle.sendSidedUpdate RETURN.
 *
 * This is NOT PokemonBattle.sendSplitUpdate or Showdown |split| custody.
 * Return proves only a native METHOD RETURN, never individual packet send,
 * actual client delivery, intended public Showdown frame or event identity.
 * Reports contain bounded counts/ordinals only, no actors, packet references,
 * payloads, request text, UUIDs, names, or personal identifiers.
 */
public final class NativeSidedDispatchReachability {
    private NativeSidedDispatchReachability(){}
    public static final String SCHEMA="phase6.24.native-sided-reachability.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;

    public enum Verdict {
        NO_NATIVE_SIDED_CALLBACK_YET, NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED,
        NATIVE_SIDED_RETURN_PRIOR_EPOCH,
        SOURCE_UNAVAILABLE, CROSS_BATTLE, FOREIGN_SOURCE_ACTOR,
        INVALID_PACKETS, REGRESSED_SOURCE, WITHDRAWN
    }
    public enum SpectatorRouting { OPPONENT_BRANCH, ALLY_BRANCH, NOT_OBSERVED }

    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final long ordinal;
        private final int turn,historyChunks,actorCount;
        private final SpectatorRouting spectators;
        private Report(Session session,Verdict verdict){
            owner=session;this.verdict=verdict;
            ordinal=session.ordinal;
            turn=session.lastTurn;historyChunks=session.lastHistory;
            actorCount=session.actorCount;spectators=session.spectatorRouting;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long observedSidedReturnOrdinal(){return ordinal;}
        public int observedTurn(){return turn;}
        public int observedHistoryChunks(){return historyChunks;}
        public int actorCount(){return actorCount;}
        public SpectatorRouting spectatorRouting(){return spectators;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn
                        && verdict==Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED
                        && ordinal>0 && ordinal==owner.ordinal
                        && owner.lastTurn==turn && owner.lastHistory==historyChunks
                        && owner.observedTurn==turn
                        && owner.observedHistory==historyChunks;
            }
        }
        /**
         * Exact actor references from the original native RETURN and same
         * callback source. Matching public nicknames or side labels alone
         * cannot substitute another native actor set. Never exports actors.
         */
        boolean matchesOriginalNativeActors(Object physicalBattle,
                List<?> candidateActors,Object callbackSourceActor){
            synchronized(owner){
                if(!fromBattle(physicalBattle) || candidateActors==null
                        || callbackSourceActor==null || owner.boundActors==null
                        || owner.sourceActor!=callbackSourceActor
                        || candidateActors.size()!=owner.boundActors.size())
                    return false;
                Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
                Set<Object> admitted=Collections.newSetFromMap(new IdentityHashMap<>());
                admitted.addAll(owner.boundActors);
                for(Object actor:candidateActors)
                    if(actor==null || !seen.add(actor)
                            || !admitted.contains(actor))return false;
                return seen.size()==admitted.size();
            }
        }
        /** Original in-process source identity only; not a public actor proof. */
        boolean fromBattle(Object originalBattle){
            synchronized(owner){
                return originalBattle!=null && current() && owner.battle==originalBattle;
            }
        }
        public boolean nativeSidedMethodReturned(){return current();}
        public boolean nativeSplitMethodReturnedProven(){return false;}
        public boolean originalShowdownSplitLinked(){return false;}
        public boolean perRecipientSendAcknowledged(){return false;}
        public boolean clientDeliveryAuthenticated(){return false;}
        public boolean spectatorDeliveryAuthenticated(){return false;}
        public boolean publicFrameAuthenticated(){return false;}
        public boolean eventIdentityAuthenticated(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean completeInstalledLegalSetProven(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase624Sided[verdict="+verdict+";returnOrdinal="+ordinal
                    +";turn="+turn+";historyChunks="+historyChunks
                    +";actors="+actorCount+";spectatorBranch="+spectators
                    +";delivery=UNVERIFIED;showdownSplit=UNLINKED;authority=NONE]";
        }
    }

    /** One physical battle and one observer owner; withdrawal is terminal. */
    public static final class Session {
        private Object battle;
        private List<Object> boundActors;
        private Object sourceActor;
        private long ordinal;
        private int lastTurn=-1,lastHistory=-1,actorCount;
        // Most recent checked source epoch, including polls without a callback.
        // A prior RETURN stays historical when the original source advances.
        private int observedTurn=-1,observedHistory=-1;
        private SpectatorRouting spectatorRouting=SpectatorRouting.NOT_OBSERVED;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;battle=null;boundActors=null;sourceActor=null;
        }
        private Report reply(Verdict verdict){return new Report(this,verdict);}
        private Report fail(Verdict verdict){
            withdraw();return reply(verdict);
        }

        /** Polls for missing optional Mixin callback; NEVER asserts activation. */
        public synchronized Report status(Object originalBattle,int turn,
                int historyChunks,boolean sourcePinned,boolean ended){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(originalBattle==null || !sourcePinned || ended
                    || turn<0 || turn>10000 || historyChunks<0
                    || historyChunks>100000)return fail(Verdict.SOURCE_UNAVAILABLE);
            if(battle!=null && battle!=originalBattle)return fail(Verdict.CROSS_BATTLE);
            // Even an initial status-only poll binds physical battle identity.
            if(turn<observedTurn || historyChunks<observedHistory)
                return fail(Verdict.REGRESSED_SOURCE);
            if(battle==null)battle=originalBattle;
            observedTurn=turn;observedHistory=historyChunks;
            if(ordinal==0)return reply(Verdict.NO_NATIVE_SIDED_CALLBACK_YET);
            // The old native callback still happened. But it is not a
            // CURRENT method-return witness for a newer history/turn.
            return reply(turn==lastTurn && historyChunks==lastHistory
                    ?Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED
                    :Verdict.NATIVE_SIDED_RETURN_PRIOR_EPOCH);
        }

        /** Called only from the optional native sendSidedUpdate RETURN mixin. */
        public synchronized Report nativeReturn(Object originalBattle,
                List<?> originalBattleActors,Object sourceActor,
                Object allyPacket,Object opponentPacket,
                boolean spectatorsAsAlly,int turn,int historyChunks,
                boolean sourcePinned,boolean ended){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(originalBattle==null || !sourcePinned || ended
                    || turn<0 || turn>10000 || historyChunks<0
                    || historyChunks>100000)
                return fail(Verdict.SOURCE_UNAVAILABLE);
            if(battle!=null && battle!=originalBattle)return fail(Verdict.CROSS_BATTLE);
            if(originalBattleActors==null || sourceActor==null
                    || originalBattleActors.size()<2 || originalBattleActors.size()>8)
                return fail(Verdict.FOREIGN_SOURCE_ACTOR);
            Set<Object> identities=Collections.newSetFromMap(new IdentityHashMap<>());
            int matches=0;
            for(Object actor:originalBattleActors){
                if(actor==null || !identities.add(actor))
                    return fail(Verdict.FOREIGN_SOURCE_ACTOR);
                if(actor==sourceActor)matches++;
            }
            if(matches!=1)return fail(Verdict.FOREIGN_SOURCE_ACTOR);
            if(boundActors!=null){
                if(boundActors.size()!=originalBattleActors.size())
                    return fail(Verdict.FOREIGN_SOURCE_ACTOR);
                for(Object actor:boundActors)
                    if(!identities.contains(actor))
                        return fail(Verdict.FOREIGN_SOURCE_ACTOR);
            }
            if(allyPacket==null || opponentPacket==null
                    || allyPacket==opponentPacket)
                return fail(Verdict.INVALID_PACKETS);
            // Multiple legitimate sided packets may return at the SAME
            // turn/history epoch. Count local callbacks, NOT events/delivery.
            if(turn<observedTurn || historyChunks<observedHistory
                    || ordinal==Long.MAX_VALUE)
                return fail(Verdict.REGRESSED_SOURCE);
            if(battle==null)battle=originalBattle;
            if(boundActors==null)
                boundActors=new ArrayList<>(originalBattleActors);
            ordinal++;
            lastTurn=turn;lastHistory=historyChunks;
            observedTurn=turn;observedHistory=historyChunks;
            actorCount=originalBattleActors.size();
            this.sourceActor=sourceActor;
            spectatorRouting=spectatorsAsAlly
                    ?SpectatorRouting.ALLY_BRANCH
                    :SpectatorRouting.OPPONENT_BRANCH;
            return reply(Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED);
        }
        /** Physical BattleEndPacket or new battle reset; no delivery authority. */
        public synchronized Report battleEnded(Object originalBattle){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(originalBattle==null || battle!=null && battle!=originalBattle)
                return fail(Verdict.CROSS_BATTLE);
            withdraw();
            return reply(Verdict.WITHDRAWN);
        }
    }
    /**
     * One physical-battle owner. Native RETURN callbacks may arrive BEFORE
     * the first AI choice resets Legacy memory: same-battle evidence survives
     * that reset. Withdrawn sessions cannot revive within that battle.
     */
    public static final class BattleScope {
        private Object activeBattle;
        private Session session=new Session();

        public synchronized Session forBattle(Object physicalBattle){
            if(physicalBattle==null){
                session.withdraw();
                return session;
            }
            if(activeBattle!=physicalBattle){
                session.withdraw();
                session=new Session();
                activeBattle=physicalBattle;
            }
            return session;
        }
        public synchronized Report end(Object physicalBattle){
            // Late end from a previous battle cannot revoke a newer source.
            if(physicalBattle==null || activeBattle!=physicalBattle)return null;
            // Keep identity bound: late callbacks in this ended battle MUST
            // NOT trigger a fresh session.
            return session.battleEnded(physicalBattle);
        }
        public synchronized void withdraw(){session.withdraw();}
    }

}
