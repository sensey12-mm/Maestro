package com.maestroai.training;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * Phase 6.7: design-only, source-pinned OFFLINE evidence interface.
 *
 * A synthetically consistent envelope is NOT a Cobblemon runtime receipt.
 * Neither this contract nor its session can authorize tactics, Shadow search,
 * live control, complete opponent responses or identity binding.
 */
public final class OfflineRuntimeEvidenceContract {
    private OfflineRuntimeEvidenceContract() {}
    public static final String SCHEMA = "phase6.7.offline-evidence-interface.v1";
    public static final String MECHANICS = CobblemonMechanics.SOURCE;

    public enum Limitation {
        ORIGINAL_EVENTS_NOT_SOURCE_AUTHENTICATED,
        NO_RUNTIME_ACTOR_OR_TURN_BINDING,
        INSTALLED_ACTION_LEGALITY_UNATTESTED,
        OPPONENT_RESPONSE_SET_OPEN,
        PRIVATE_FIELDS_NOT_ACCEPTED,
        NO_RUNTIME_PROMOTION
    }

    /**
     * Synthetic battle token is an opaque, caller-generated SHA256-sized ID,
     * NOT a live battle UUID, trainer name, signature or runtime lease.
     * The sequence belongs only to one Session instance.
     */
    public record Envelope(String schema, String mechanicsSource,
                           String syntheticBattleToken, long sequence,
                           int turn, int opponentAppearance,
                           String ownSlot, String opponentSlot,
                           List<String> claimedOwnActions, String actionFingerprint,
                           OfflineShadowResponseScenarios.Input scenarios) {
        public Envelope {
            if (!SCHEMA.equals(schema) || !MECHANICS.equals(mechanicsSource)
                    || !digest(syntheticBattleToken) || !digest(actionFingerprint)
                    || sequence < 1 || sequence > 1_000_000
                    || turn < 1 || turn > 10000
                    || opponentAppearance < 1 || opponentAppearance > 100
                    || !slots(ownSlot,opponentSlot) || scenarios == null
                    || claimedOwnActions == null)
                throw invalid();
            claimedOwnActions = List.copyOf(claimedOwnActions);
            if (claimedOwnActions.isEmpty() || claimedOwnActions.size() > 24
                    || new HashSet<>(claimedOwnActions).size()!=claimedOwnActions.size()
                    || claimedOwnActions.stream().anyMatch(a->
                            !a.matches("(move|switch):[a-z0-9]{1,48}")))
                throw invalid();
        }
        @Override public String toString() {
            return "Phase67Envelope[synthetic-only;sequence="+sequence
                    +";turn="+turn+";appearance="+opponentAppearance+"]";
        }
    }

    /** Canonical digest only identifies a synthetic action set, never authority.
     * Length-prefix encoding avoids delimiter collisions, ordering is canonical. */
    public static String actionFingerprint(Collection<String> actions) {
        if(actions == null || actions.isEmpty() || actions.size() > 24
                || new HashSet<>(actions).size() != actions.size()
                || actions.stream().anyMatch(a->a == null
                        || !a.matches("(move|switch):[a-z0-9]{1,48}")))
            throw invalid();
        try {
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            add(out,SCHEMA);
            add(out,MECHANICS);
            var ordered=new TreeSet<>(actions);
            out.writeInt(ordered.size());
            for(String id:ordered)add(out,id);
            out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Phase 6.7 digest unavailable");
        }
    }
    private static void add(DataOutputStream out,String value)throws IOException {
        byte[] b=value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }
    private static boolean digest(String v) {
        return v!=null && v.matches("[0-9a-f]{64}");
    }
    private static boolean slots(String own,String opponent) {
        return ("p1a".equals(own) && "p2a".equals(opponent))
            || ("p2a".equals(own) && "p1a".equals(opponent));
    }
    private static IllegalArgumentException invalid() {
        // NEVER interpolate names, payloads, UUIDs, guessed items or moves.
        return new IllegalArgumentException("Phase 6.7 untrusted synthetic evidence");
    }

    /** Private constructor: no external caller can mint a runtime certificate. */
    public static final class Review {
        private final long sequence;
        private final int turn,appearance,fixtureActions,witnessedReplies;
        private final Set<Limitation> limitations;
        private Review(long sequence,int turn,int appearance,int actions,int replies) {
            this.sequence=sequence;this.turn=turn;this.appearance=appearance;
            this.fixtureActions=actions;this.witnessedReplies=replies;
            this.limitations=Collections.unmodifiableSet(EnumSet.allOf(Limitation.class));
        }
        public String schema(){return SCHEMA;}
        public String mechanicsSource(){return MECHANICS;}
        public long syntheticSequence(){return sequence;}
        public int syntheticTurn(){return turn;}
        public int syntheticAppearance(){return appearance;}
        public int syntheticActionCount(){return fixtureActions;}
        public int witnessedReplyCount(){return witnessedReplies;}
        public Set<Limitation> limitations(){return limitations;}
        public boolean syntheticConsistencyOnly(){return true;}
        public boolean runtimeActorBound(){return false;}
        public boolean runtimeTurnFreshnessAttested(){return false;}
        public boolean installedLegalActionsAttested(){return false;}
        public boolean opponentResponseSetComplete(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase67OfflineReview[sequence="+sequence+";turn="+turn
                    +";appearance="+appearance+";actions="+fixtureActions
                    +";replies="+witnessedReplies+";authority=NONE]";
        }
    }

    /**
     * Stateful offline sequence guard, not a live-session binder. A rejected
     * envelope permanently poisons THIS Session (no reseeding via equal token).
     * New Session instances cannot prove freshness across processes/restarts;
     * a future signed and source-attested runtime provider is still required.
     */
    public static final class Session {
        private final String token;
        private boolean poisoned;
        private long sequence;
        private int lastTurn,lastAppearance;
        private String lastActionFingerprint;
        private List<String> lastFramedHistory=List.of();
        public Session(String syntheticBattleToken) {
            if(!digest(syntheticBattleToken))throw invalid();
            this.token=syntheticBattleToken;
        }
        public synchronized boolean poisoned(){return poisoned;}
        public synchronized Review inspect(Envelope input) {
            if(poisoned)throw invalid();
            try {
                if(input==null || !token.equals(input.syntheticBattleToken())
                        || input.sequence()!=sequence+1
                        || input.turn()<lastTurn || input.turn()>lastTurn+1
                        || input.opponentAppearance()<lastAppearance
                        || input.opponentAppearance()>lastAppearance+1
                        || lastTurn==input.turn() && lastAppearance==input.opponentAppearance()
                          && lastActionFingerprint!=null
                          && !lastActionFingerprint.equals(input.actionFingerprint()))
                    throw invalid();

                var source=input.scenarios();
                var fixture=source.fixture();
                var history=fixture.framedPublicHistory();
                if(history.size()<lastFramedHistory.size()
                        || !history.subList(0,lastFramedHistory.size())
                                .equals(lastFramedHistory))
                    throw invalid(); // source history must be append-only
                if(input.turn()!=source.snapshot().turn()
                        || input.turn()!=fixture.expectedTurn()
                        || input.opponentAppearance()!=fixture.opponentAppearanceIndex()
                        || !input.ownSlot().equals(fixture.ownSlot())
                        || !input.opponentSlot().equals(fixture.opponentSlot()))
                    throw invalid();

                // Reparse ORIGINAL spectator frames, recheck source-pinned
                // mechanics and conditional root. Never trust prebuilt receipts.
                var report=OfflineShadowResponseScenarios.construct(source);
                var root=PublicStateSearchAdapter.project(source.snapshot(),
                        source.mechanics()).states().get("root");
                var actual=new TreeSet<String>();
                root.legalMoves().forEach(m->actual.add("move:"+m));
                root.legalSwitches().forEach(s->actual.add("switch:"+s));
                if (!actual.equals(new TreeSet<>(input.claimedOwnActions()))
                        || !actual.equals(new TreeSet<>(fixture.ownLegalActions()))
                        || !actionFingerprint(actual).equals(input.actionFingerprint()))
                    throw invalid();

                // Commit sequence only after all checks pass, including source
                // event provenance, exact turn and action-set reconstruction.
                sequence=input.sequence();
                lastTurn=input.turn();
                lastAppearance=input.opponentAppearance();
                lastActionFingerprint=input.actionFingerprint();
                lastFramedHistory=history;
                return new Review(sequence,lastTurn,lastAppearance,
                        actual.size(),report.witnessedReplyCount());
            } catch (RuntimeException invalidEvidence) {
                poisoned=true;
                throw invalid(); // fail closed, privacy-safe exception
            }
        }
    }
}
