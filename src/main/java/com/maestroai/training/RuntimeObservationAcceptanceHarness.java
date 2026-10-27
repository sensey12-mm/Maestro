package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Phase 6.22: diagnostic-only source/hook/lifecycle acceptance instrumentation.
 *
 * NOT a tactical adapter or installed Minecraft acceptance result. A native
 * RETURN is not network receipt, a matched source timeline is not packet
 * identity, and Showdown history is not proof of public visibility.
 *
 * Never retains raw private/public history, packet objects, actor identities,
 * names or UUIDs in a report. Source chunks exist only on the caller's stack
 * during hashing; retained digests are randomly salted per session.
 */
public final class RuntimeObservationAcceptanceHarness {
    private RuntimeObservationAcceptanceHarness() {}
    public static final String SCHEMA="phase6.22.runtime-observation-harness.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final int MAX_CHUNKS=2048, MAX_CHARS=4*1024*1024;
    private static final int MAX_CHUNK_CHARS=65536;

    public enum Verdict {
        NO_HISTORY_YET,
        HOOK_UNOBSERVED,
        NATIVE_RETURN_OBSERVED_UNVERIFIED,
        ORDERED_SOURCE_AND_HOOK_UNVERIFIED,
        SOURCE_UNAVAILABLE,
        WRONG_BATTLE,
        STALE_OR_REWRITTEN,
        ORDER_CONFLICT,
        INVALID_NATIVE_WITNESS,
        WITHDRAWN,
        BATTLE_END_HOOK_OBSERVED
    }
    public enum Phase621Coverage {
        NOT_OBSERVED, DIRECT_ONLY_OBSERVATIONAL, SPLIT_TIMING_UNVERIFIED, ABSTAINED
    }

    /** Only safe numeric/bounded enums are exposed; authority is always NONE. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final int nativeReturns, historyChunks, turn;
        private final Phase621Coverage phase621;
        private Report(Session owner,Verdict verdict){
            this.owner=owner;
            this.verdict=verdict;
            nativeReturns=owner.nativeReturns;
            historyChunks=owner.lastHistoryChunks;
            turn=owner.lastTurn;
            phase621=owner.coverage;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public int nativeReturnCallbacks(){return nativeReturns;}
        public int observedHistoryChunks(){return historyChunks;}
        public int observedTurn(){return turn;}
        public Phase621Coverage phase621Coverage(){return phase621;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && verdict==owner.lastVerdict
                        && nativeReturns==owner.nativeReturns
                        && historyChunks==owner.lastHistoryChunks
                        && turn==owner.lastTurn && phase621==owner.coverage;
            }
        }
        public boolean minecraftAccepted(){return false;}
        public boolean nativeHookInstalledProven(){return false;}
        public boolean sourcePublicVisibilityCertified(){return false;}
        public boolean eventIdentityAuthenticated(){return false;}
        public boolean recipientDeliveryAuthenticated(){return false;}
        public boolean completeLegalActionsProven(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase622Runtime[verdict="+verdict
                    +";nativeReturns="+nativeReturns
                    +";historyChunks="+historyChunks+";turn="+turn
                    +";phase621="+phase621
                    +";minecraft=UNTESTED;delivery=UNVERIFIED;authority=NONE]";
        }
    }

    /** An in-process, single physical battle observer; not an attestor. */
    public static final class Session {
        private final byte[] salt=new byte[32];
        private Object battle;
        private List<String> digests=List.of();
        private int lastHistoryChunks,lastTurn=-1;
        private long lastNativeOrdinal;
        private int lastNativeTurn=-1,lastNativeHistoryChunks;
        private int nativeReturns;
        private boolean withdrawn;
        private Verdict lastVerdict;
        private Phase621Coverage coverage=Phase621Coverage.NOT_OBSERVED;

        public Session(){new SecureRandom().nextBytes(salt);}
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;
            battle=null;
            digests=List.of();
            Arrays.fill(salt,(byte)0);
            lastVerdict=Verdict.WITHDRAWN;
        }
        private Report result(Verdict verdict){
            lastVerdict=verdict;
            return new Report(this,verdict);
        }
        private Report fail(Verdict verdict){
            withdraw();
            return new Report(this,verdict);
        }
        private boolean wrongBattle(Object supplied){
            return supplied==null || battle!=null && battle!=supplied;
        }
        private void bind(Object originalBattle){
            if(battle==null)battle=originalBattle;
        }
        private List<String> digest(List<String> chunks){
            try{
                MessageDigest d=MessageDigest.getInstance("SHA-256");
                List<String> result=new ArrayList<>(chunks.size());
                for(String chunk:chunks){
                    d.update(salt);
                    d.update(chunk.getBytes(StandardCharsets.UTF_8));
                    result.add(HexFormat.of().formatHex(d.digest()));
                    d.reset();
                }
                return List.copyOf(result);
            }catch(NoSuchAlgorithmException missing){
                throw new IllegalStateException("SHA-256 unavailable",missing);
            }
        }

        /** Only the actual native RETURN observer calls this production hook. */
        public synchronized Report nativeReturnHook(Object originalBattle,
                NativeSplitDispatchObservation.Report receipt){
            if(withdrawn)return result(Verdict.WITHDRAWN);
            if(wrongBattle(originalBattle))return fail(Verdict.WRONG_BATTLE);
            if(receipt==null || !receipt.current()
                    || !receipt.fromBattle(originalBattle)
                    || receipt.verdict()!=NativeSplitDispatchObservation.Verdict.NATIVE_METHOD_RETURN_OBSERVED)
                return fail(Verdict.INVALID_NATIVE_WITNESS);
            long ordinal=receipt.nativeDispatchOrdinal();
            int turn=receipt.observedTurn(),chunks=receipt.historyChunksAtReturn();
            if(ordinal<=lastNativeOrdinal || ordinal<1
                    || turn<0 || turn>10000 || chunks<0 || chunks>100000
                    || turn<lastNativeTurn || chunks<lastNativeHistoryChunks
                    || turn<lastTurn || chunks<lastHistoryChunks)
                return fail(Verdict.ORDER_CONFLICT);
            bind(originalBattle);
            nativeReturns++;
            lastNativeOrdinal=ordinal;
            lastNativeTurn=turn;
            lastNativeHistoryChunks=chunks;
            return result(Verdict.NATIVE_RETURN_OBSERVED_UNVERIFIED);
        }

        /** Original history is ephemeral: only private, salted hashes persist. */
        public synchronized Report publicHistory(Object originalBattle,int turn,
                List<String> originalHistory,boolean pinned){
            if(withdrawn)return result(Verdict.WITHDRAWN);
            if(wrongBattle(originalBattle))return fail(Verdict.WRONG_BATTLE);
            if(!pinned || originalHistory==null || turn<0 || turn>10000
                    || originalHistory.size()>MAX_CHUNKS)
                return fail(Verdict.SOURCE_UNAVAILABLE);
            List<String> chunks;
            try{chunks=List.copyOf(originalHistory);}
            catch(RuntimeException unsupported){return fail(Verdict.SOURCE_UNAVAILABLE);}
            int chars=0;
            for(String chunk:chunks){
                if(chunk.length()>MAX_CHUNK_CHARS
                        || chars>MAX_CHARS-chunk.length())
                    return fail(Verdict.SOURCE_UNAVAILABLE);
                chars+=chunk.length();
            }
            if(turn<lastTurn || turn<lastNativeTurn
                    || chunks.size()<lastHistoryChunks
                    || chunks.size()<lastNativeHistoryChunks)
                return fail(Verdict.ORDER_CONFLICT);
            List<String> next=digest(chunks);
            if(next.size()<digests.size()
                    || !next.subList(0,digests.size()).equals(digests))
                return fail(Verdict.STALE_OR_REWRITTEN);
            bind(originalBattle);
            digests=next;
            lastHistoryChunks=chunks.size();
            lastTurn=turn;
            if(chunks.isEmpty())return result(Verdict.NO_HISTORY_YET);
            return result(nativeReturns==0?Verdict.HOOK_UNOBSERVED
                    :Verdict.ORDERED_SOURCE_AND_HOOK_UNVERIFIED);
        }

        /** Phase 6.21 is coverage only; positive names never grant permission. */
        public synchronized Report observePhase621(Object originalBattle,
                NativeEvidenceEndToEndGate.Report gate){
            if(withdrawn)return result(Verdict.WITHDRAWN);
            if(wrongBattle(originalBattle))return fail(Verdict.WRONG_BATTLE);
            bind(originalBattle);
            if(gate==null || !gate.current()){
                coverage=Phase621Coverage.ABSTAINED;
            }else if(!gate.fromBattle(originalBattle)){
                return fail(Verdict.WRONG_BATTLE);
            }else if(gate.shadowSearchAuthorized()
                    || gate.tacticalDecisionAuthorized()
                    || gate.livePromotionAuthorized()
                    || gate.recipientDeliveryAuthenticated()
                    || gate.eventIdentityAuthenticated()){
                return fail(Verdict.INVALID_NATIVE_WITNESS);
            }else{
                coverage=switch(gate.verdict()){
                    case DIRECT_ONLY_OBSERVATIONAL -> Phase621Coverage.DIRECT_ONLY_OBSERVATIONAL;
                    case SPLIT_TIMING_UNVERIFIED -> Phase621Coverage.SPLIT_TIMING_UNVERIFIED;
                    default -> Phase621Coverage.ABSTAINED;
                };
            }
            return result(lastVerdict==null?Verdict.HOOK_UNOBSERVED:lastVerdict);
        }

        /** Native battle-end callback witnesses only teardown invocation. */
        public synchronized Report battleEndHook(Object originalBattle){
            if(withdrawn)return result(Verdict.WITHDRAWN);
            if(wrongBattle(originalBattle))return fail(Verdict.WRONG_BATTLE);
            withdraw();
            return new Report(this,Verdict.BATTLE_END_HOOK_OBSERVED);
        }
    }
}
