package com.maestroai.training;

import java.util.*;

/**
 * Phase 6.18: read-only recipient-scoped observation of the actual
 * PokemonBattle.sendSplitUpdate RETURN call boundary.
 *
 * A returned method proves only that native send invocations returned,
 * NOT network delivery, client processing, or association with a
 * Showdown split frame. No packet bytes, recipient UUIDs, actor names,
 * UUIDs, names or packet objects are stored in the receipt.
 */
public final class NativeSplitDispatchObservation {
    public static final String SCHEMA="phase6.18.native-dispatch-attempt.v1";
    public static final String SOURCE=PublicSplitRoutingGate.SOURCE;

    public enum Verdict {
        NATIVE_METHOD_RETURN_OBSERVED,
        NO_NATIVE_CALLBACK_YET,
        AMBIGUOUS_DUPLICATE,
        SOURCE_UNAVAILABLE,
        CROSS_BATTLE,
        FOREIGN_RECIPIENT,
        INVALID_PACKETS,
        STALE_SOURCE,
        WITHDRAWN
    }

    private static final class Stamp {
        final int turn,historyCount;
        final long ordinal;
        final int actorCount,publicActorCount,spectatorCount;
        Stamp(int turn,int history,long ordinal,int actors,int publicActors,int spectators){
            this.turn=turn;historyCount=history;this.ordinal=ordinal;
            actorCount=actors;publicActorCount=publicActors;spectatorCount=spectators;
        }
    }
    /** Immutable redacted metadata, live only as long as its original session. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final long ordinal;
        private final int turn,historyCount,privateActors,publicActors,spectators;
        private Report(Session owner,Verdict result,Stamp at) {
            this.owner=owner;verdict=result;
            ordinal=at==null?0:at.ordinal;
            turn=at==null?0:at.turn;
            historyCount=at==null?0:at.historyCount;
            privateActors=at==null?0:1;
            publicActors=at==null?0:at.publicActorCount;
            spectators=at==null?0:at.spectatorCount;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long nativeDispatchOrdinal(){return ordinal;}
        public int observedTurn(){return turn;}
        public int historyChunksAtReturn(){return historyCount;}
        public int privateTargetActorCount(){return privateActors;}
        public int publicActorCount(){return publicActors;}
        public int spectatorCount(){return spectators;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.stamp!=null
                        && owner.stamp.ordinal==ordinal && ordinal>0
                        && verdict==Verdict.NATIVE_METHOD_RETURN_OBSERVED;
            }
        }
        public boolean sendMethodReturned(){return current();}
        public boolean intendedRecipientRolesObserved(){return current();}
        public boolean networkSendAcknowledged(){return false;}
        public boolean recipientDeliveryAuthenticated(){return false;}
        public boolean clientMessageProcessed(){return false;}
        public boolean originalSplitPayloadLinked(){return false;}
        public boolean publicReadinessLeaseAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase618NativeDispatch[verdict="+verdict
                    +";ordinal="+ordinal+";turn="+turn
                    +";privateActors="+privateActors+";publicActors="+publicActors
                    +";spectators="+spectators+";delivery=UNVERIFIED;authority=NONE]";
        }
    }

    /**
     * A Session is owned by one Maestro AI instance and reset on battle
     * teardown. A malformed callback poisons it irreversibly; another
     * physical battle must use a new Session.
     */
    public static final class Session {
        private Object boundBattle;
        // Native actor references remain in-process and only for this battle.
        // Never stored in a report or diagnostic; cleared on withdrawal.
        private List<Object> boundActors;
        private Stamp stamp;
        private boolean withdrawn;
        private long sequence;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;stamp=null;boundBattle=null;boundActors=null;
        }
        /** The choice or packet read site may ask for readiness even if
         * the optional native mixin NEVER fired. Absence is not an attestation.
         */
        public synchronized Report status(Object battle,int turn,int history,
                                         boolean pinned,boolean battleEnded) {
            if(withdrawn)return new Report(this,Verdict.WITHDRAWN,null);
            if(battle==null || !pinned || battleEnded || turn<0 || history<0)
                return fail(Verdict.SOURCE_UNAVAILABLE);
            if(boundBattle!=null && battle!=boundBattle)
                return fail(Verdict.CROSS_BATTLE);
            if(stamp==null)return new Report(this,Verdict.NO_NATIVE_CALLBACK_YET,null);
            if(turn<stamp.turn || history<stamp.historyCount)
                return fail(Verdict.STALE_SOURCE);
            return new Report(this,Verdict.NATIVE_METHOD_RETURN_OBSERVED,stamp);
        }
        /** Called only from the native BattleEndPacket broadcast path. */
        public synchronized void battleEnded(Object battle) {
            if(withdrawn)return;
            if(boundBattle!=null && battle!=boundBattle){fail(Verdict.CROSS_BATTLE);return;}
            withdraw();
        }
        private Report fail(Verdict why){
            withdraw();
            return new Report(this,why,null);
        }
        /**
         * Called ONLY from the installed native sendSplitUpdate return hook.
         * Args are source object references, never identities to export.
         * No direct invocation by speculative tactical consumers.
         */
        public synchronized Report observeNativeReturn(Object battle,
                List<?> battleActors,Object privateActor,
                Object privatePacket,Object publicPacket,
                int actualTurn,int historyChunkCount,int spectatorCount,
                boolean installedVersionPinned,boolean battleEnded) {
            if(withdrawn)return new Report(this,Verdict.WITHDRAWN,null);
            if(!installedVersionPinned || battleEnded || battle==null
                    || actualTurn<0 || actualTurn>10000
                    || historyChunkCount<0 || historyChunkCount>100000
                    || spectatorCount<0 || spectatorCount>10000)
                return fail(Verdict.SOURCE_UNAVAILABLE);
            if(boundBattle!=null && boundBattle!=battle)
                return fail(Verdict.CROSS_BATTLE);
            if(privateActor==null || battleActors==null
                    || battleActors.size()<2 || battleActors.size()>8)
                return fail(Verdict.FOREIGN_RECIPIENT);
            Set<Object> distinct=Collections.newSetFromMap(new IdentityHashMap<>());
            int ownTarget=0;
            for(Object actor:battleActors){
                if(actor==null || !distinct.add(actor))return fail(Verdict.FOREIGN_RECIPIENT);
                if(actor==privateActor)ownTarget++;
            }
            if(ownTarget!=1)return fail(Verdict.FOREIGN_RECIPIENT);
            if(boundActors!=null) {
                if(boundActors.size()!=battleActors.size())
                    return fail(Verdict.FOREIGN_RECIPIENT);
                for(Object expected:boundActors)
                    if(!distinct.contains(expected))
                        return fail(Verdict.FOREIGN_RECIPIENT);
            }
            // Both packet OBJECT references must exist and be different.
            // Payloads are not read or hashed, nor are packet refs retained.
            if(privatePacket==null || publicPacket==null || privatePacket==publicPacket)
                return fail(Verdict.INVALID_PACKETS);
            if(stamp!=null && (actualTurn<stamp.turn
                    || historyChunkCount<stamp.historyCount))
                return fail(Verdict.STALE_SOURCE);
            // A second same-epoch return could be a genuine extra packet,
            // a replay, or a duplicate Mixin callback. Cannot distinguish:
            // withdraw instead of manufacturing two trusted dispatches.
            if(stamp!=null && actualTurn==stamp.turn
                    && historyChunkCount==stamp.historyCount)
                return fail(Verdict.AMBIGUOUS_DUPLICATE);
            if(sequence==Long.MAX_VALUE)return fail(Verdict.STALE_SOURCE);
            boundBattle=battle;
            if(boundActors==null)boundActors=new ArrayList<>(battleActors);
            sequence++;
            stamp=new Stamp(actualTurn,historyChunkCount,sequence,
                    battleActors.size(),battleActors.size()-1,spectatorCount);
            return new Report(this,Verdict.NATIVE_METHOD_RETURN_OBSERVED,stamp);
        }
    }
}
