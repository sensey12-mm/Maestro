package com.maestroai.training;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Phase 6.12: one-use, read-only fusion of Phase 6.10 ordinary moves and
 * Phase 6.11 own-party switch/forced-choice witnesses.
 *
 * This checks in-process identity and current dispatch lineage only;
 * it never certifies complete installed legality or authorizes tactics.
 */
public final class NativeOwnActionFusion {
    private NativeOwnActionFusion(){}
    public static final String SCHEMA="phase6.12.own-action-coverage.v1";
    public static final String SOURCE=OwnRequestTransport.SOURCE;
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 6.12 own-action coverage abstained");
    }
    public enum Mode { ORDINARY_MOVE_AND_SWITCH, FORCED_REPLACEMENT_ONLY }

    public static final class Review {
        private final Session session;
        private final OwnRequestTransport.Lease lease;
        private final NativeOwnActionEvidence.Review moves;
        private final NativeOwnSwitchEvidence.Review switches;
        private final String digest;
        private final Mode mode;
        private final int turn,moveCount,switchCount;
        private Review(Session session,OwnRequestTransport.Lease lease,
                NativeOwnActionEvidence.Review moves,NativeOwnSwitchEvidence.Review switches,
                Mode mode,int turn,String digest) {
            this.session=session;this.lease=lease;this.moves=moves;this.switches=switches;
            this.mode=mode;this.turn=turn;this.digest=digest;
            moveCount=moves==null?0:moves.observedMoveCount();
            switchCount=switches.observedSwitchCount();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Mode mode(){return mode;}
        public int ownTurn(){return turn;}
        public int ownMoveCount(){return moveCount;}
        public int ownSwitchCandidateCount(){return switchCount;}
        /** Hash of *two independently checked fixture fingerprints*, not legal authority. */
        public String coverageFingerprint(){return digest;}
        public boolean current(){
            return !session.withdrawn && lease.current()
                    && switches.currentlyObserved()
                    && (moves==null || moves.currentOwnMoveAgreement());
        }
        public boolean completeLegalSetProven(){return false;}
        public boolean allInstalledSwitchesCertified(){return false;}
        public boolean opponentActionsKnown(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString() {
            return "Phase612OwnCoverage[mode="+mode+";turn="+turn+";moves="+moveCount
                    +";switches="+switchCount+";completeLegalSet=false;authority=NONE]";
        }
    }
    private static void frame(DataOutputStream out,String data)throws java.io.IOException {
        byte[] b=data.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);out.write(b);
    }
    private static String fingerprint(Mode mode,int turn,String moveHash,String switchHash) {
        try {
            var buf=new ByteArrayOutputStream();
            var out=new DataOutputStream(buf);
            frame(out,SCHEMA);frame(out,SOURCE);frame(out,mode.name());
            out.writeInt(turn);
            frame(out,moveHash);
            frame(out,switchHash);
            out.flush();
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(buf.toByteArray()));
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException ex){
            throw new IllegalStateException("Phase 6.12 hashing unavailable");
        }
    }

    /**
     * One session per Maestro AI battle, in the same synchronous choice path.
     * A foreign, expired or contradictory pair withdraws the session; it is
     * never replaced inside that battle. An explicitly new battle gets a new
     * Session. In-process identity is NOT cryptographic host attestation.
     */
    public static final class Session {
        private Object boundBattle,boundActor;
        private OwnRequestTransport.Lease consumed;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;}
        public synchronized Review inspect(OwnRequestTransport.Lease lease,
                Object battle,Object actor,Object installedRequest,String side,
                UUID activeUuid,int sourceTurn,boolean forced,
                NativeOwnActionEvidence.Review moves,NativeOwnSwitchEvidence.Review switches) {
            if(withdrawn)throw invalid();
            try {
                if(lease==null || !lease.current() || battle==null || actor==null
                        || installedRequest==null || activeUuid==null
                        || !Set.of("p1","p2").contains(side)
                        || sourceTurn<1 || sourceTurn>10000
                        || switches==null || lease==consumed
                        || boundBattle!=null && boundBattle!=battle
                        || boundActor!=null && boundActor!=actor
                        || !switches.sameChoice(lease,battle,actor,installedRequest,
                                side,activeUuid,sourceTurn)
                        || switches.forced()!=forced || switches.ownTurn()!=sourceTurn
                        || !SOURCE.equals(switches.source())
                        || switches.completeLegalSetProven()
                        || switches.shadowSearchAuthorized()
                        || switches.livePromotionAuthorized())
                    throw invalid();
                final Mode mode;
                final String moveDigest;
                if(forced) {
                    // Forced replacement has no ordinary move receipt by design.
                    if(moves!=null || switches.observedSwitchCount()<1
                            || switches.trapped())throw invalid();
                    mode=Mode.FORCED_REPLACEMENT_ONLY;
                    moveDigest="NO_MOVE_RECEIPT_EXPECTED";
                } else {
                    if(moves==null
                            || !moves.sameChoice(lease,battle,actor,installedRequest,
                                    side,activeUuid,sourceTurn)
                            || moves.ownTurn()!=sourceTurn
                            || !SOURCE.equals(moves.source())
                            || moves.observedMoveCount()<1
                            || moves.trapped()!=switches.trapped()
                            || switches.trapped()&&switches.observedSwitchCount()!=0
                            || moves.completeLegalSetProven()
                            || moves.shadowSearchAuthorized()
                            || moves.livePromotionAuthorized())
                        throw invalid();
                    mode=Mode.ORDINARY_MOVE_AND_SWITCH;
                    moveDigest=moves.moveFingerprint();
                }
                String digest=fingerprint(mode,sourceTurn,moveDigest,
                        switches.candidateFingerprint());
                if(!lease.current() || !switches.currentlyObserved()
                        || !forced && !moves.currentOwnMoveAgreement())throw invalid();
                // Register exact ownership only AFTER a completely verified pair.
                boundBattle=battle;boundActor=actor;consumed=lease;
                return new Review(this,lease,moves,switches,mode,sourceTurn,digest);
            }catch(RuntimeException failure){
                withdrawn=true;
                throw invalid(); // never echo request bytes or party identity
            }
        }
    }
}
