package com.maestroai.training;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/**
 * Phase 6.2: strict OFFLINE fixture-integrity and appearance-local provenance
 * contract. It is NOT a trusted capture, signature, current battle receipt,
 * installed-game legality proof, or opponent identity certificate.
 *
 * All framed messages are re-parsed. Caller-supplied digest only detects a
 * mismatch against an independently fixed fixture value; if both are minted
 * by the same untrusted caller no real-world provenance is established.
 */
public final class OfflineShadowProvenanceGate {
    private OfflineShadowProvenanceGate() {}
    public static final String SCHEMA = "phase6.2.offline-public-provenance.v1";
    private static final Set<String> SUPPORTED = Set.of("switch", "drag", "turn", "move");

    /** A caller's synthetic fixture pin. Neither record nor hash is authority
     * for a current battle; no public report IDs, UUIDs or player names escape. */
    public record Input(List<String> framedPublicHistory, String pinnedSha256,
                        int expectedTurn, String ownSlot, String opponentSlot,
                        int opponentAppearanceIndex, List<String> ownLegalActions) {
        public Input {
            if (framedPublicHistory == null || pinnedSha256 == null
                    || expectedTurn < 1 || expectedTurn > 10000
                    || !validSlots(ownSlot, opponentSlot)
                    || opponentAppearanceIndex < 1 || opponentAppearanceIndex > 100
                    || ownLegalActions == null || ownLegalActions.isEmpty()
                    || ownLegalActions.size() > 24
                    || !pinnedSha256.matches("[0-9a-f]{64}"))
                throw invalid();
            framedPublicHistory = List.copyOf(framedPublicHistory);
            ownLegalActions = List.copyOf(ownLegalActions);
            if (new HashSet<>(ownLegalActions).size() != ownLegalActions.size()
                    || ownLegalActions.stream().anyMatch(a -> !a.matches("(move|switch):[a-z0-9]{1,48}")))
                throw invalid();
        }
    }

    /** Immutable, redacted offline-only result; constructor cannot be called
     * by an external consumer to mint a Phase 4/5/current-battle certificate. */
    public static final class Receipt {
        private final int turn, appearance, ownLegalActionCount, publicReplyCount;
        private Receipt(int turn, int appearance, int actionCount, int replyCount) {
            this.turn = turn;
            this.appearance = appearance;
            this.ownLegalActionCount = actionCount;
            this.publicReplyCount = replyCount;
        }
        public String schema() { return SCHEMA; }
        public int fixtureTurn() { return turn; }
        public int appearanceOrdinal() { return appearance; }
        public int ownActionCount() { return ownLegalActionCount; }
        public int observedReplyCount() { return publicReplyCount; }
        public boolean offlineFixtureMatched() { return true; }
        public boolean sourceAuthenticated() { return false; }
        public boolean liveTurnActorBound() { return false; }
        public boolean installedLegalActionsAttested() { return false; }
        public boolean uniqueOpponentIdentityProven() { return false; }
        public boolean opponentResponseCalibrated() { return false; }
        public boolean shadowTacticalReady() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "Phase62OfflineReceipt[turn=" + turn + ";appearance=" + appearance
                    + ";actions=" + ownLegalActionCount + ";replies=" + publicReplyCount
                    + ";authority=NONE]";
        }
    }

    /** Deterministic fixture digest with length-prefixed strings; not a MAC
     * or signature. Use a separate, fixed expected digest to detect tampering. */
    public static String fixtureSha256(List<String> history, int turn,
                                       String ownSlot, String opponentSlot,
                                       int appearance, List<String> actions) {
        Objects.requireNonNull(history); Objects.requireNonNull(actions);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            part(out, SCHEMA);
            out.writeInt(turn); part(out, ownSlot); part(out, opponentSlot);
            out.writeInt(appearance);
            out.writeInt(history.size());
            for (String chunk : history) part(out, chunk);
            var sorted = new TreeSet<>(actions);
            if (sorted.size() != actions.size()) throw invalid();
            out.writeInt(sorted.size());
            for (String action : sorted) part(out, action);
            out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(buffer.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("fixture digest unavailable", ex);
        }
    }

    private static void part(DataOutputStream out, String value) throws IOException {
        if (value == null) throw invalid();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65536) throw invalid();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static boolean validSlots(String own, String foe) {
        return ("p1a".equals(own) && "p2a".equals(foe))
                || ("p2a".equals(own) && "p1a".equals(foe));
    }
    private static String id(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 6.2: untrusted or inconsistent offline public fixture");
    }

    /** Never accepts preconstructed provenance/custody reports or Phase 5 guesses.
     * An original framed fixture and pinned digest are recomputed every time. */
    public static Receipt check(Input input, BattleSnapshot snapshot,
                                List<ShadowMechanicsEvaluation.Plan> plans) {
        Objects.requireNonNull(input); Objects.requireNonNull(snapshot);
        Objects.requireNonNull(plans);
        if (input.expectedTurn() != snapshot.turn()
                || input.framedPublicHistory().size() > 128
                || input.framedPublicHistory().stream().mapToInt(String::length).sum() > 65536
                || !input.pinnedSha256().equals(fixtureSha256(
                        input.framedPublicHistory(), input.expectedTurn(), input.ownSlot(),
                        input.opponentSlot(), input.opponentAppearanceIndex(),
                        input.ownLegalActions())))
            throw invalid();

        Set<String> legal = new TreeSet<>();
        snapshot.legalMoves().forEach(a -> legal.add("move:" + a));
        snapshot.legalSwitches().forEach(a -> legal.add("switch:" + a));
        if (!legal.equals(new TreeSet<>(input.ownLegalActions()))
                || plans.stream().anyMatch(p -> p.scenarios().stream().anyMatch(
                    s -> s.size() != 1 || s.getFirst().request() != null)))
            throw invalid(); // future turns need new independent actor/request evidence

        // Never certify opponent types, stat stages or unrevealed bench data from
        // this narrow spectator slice. Phase 4 may analyze a separately provided
        // conditional profile, but it cannot acquire real runtime authority.
        if (!snapshot.opponentBench().isEmpty()
                || !snapshot.opponent().types().isEmpty()
                || !snapshot.opponent().knownBoosts().isEmpty()
                || snapshot.opponent().revealedItem() != null
                || snapshot.opponent().revealedAbility() != null)
            throw invalid();

        List<String> lines = new ArrayList<>();
        for (String chunk : input.framedPublicHistory()) {
            if (!chunk.startsWith("update\n")) throw invalid();
            String[] parts = chunk.split("\\R", -1);
            if (parts.length < 2) throw invalid();
            for (int i = 1; i < parts.length; i++) {
                String line = parts[i];
                if (line.isEmpty()) continue;
                String[] event = line.split("\\|", -1);
                if (event.length < 3 || !event[0].isEmpty()
                        || !SUPPORTED.contains(event[1])) throw invalid();
                lines.add(line);
            }
        }
        if (lines.isEmpty()) throw invalid();
        List<String> filtered = PublicProtocolFilter.publicEvents(input.framedPublicHistory());
        var audit = PublicEnvelopeProvenance.reconcile(input.framedPublicHistory(), filtered);
        if (!filtered.equals(lines) || !audit.complete()
                || audit.origins().size() != filtered.size()
                || audit.origins().stream().anyMatch(o -> o.route()
                    != PublicEnvelopeProvenance.Route.DIRECT_UPDATE))
            throw invalid();

        int ownAppearances = 0, opponentAppearances = 0, countTurns = 0;
        String lastDisplayedOpponent = null;
        // A slot alone is NOT an actor identity. Original spectator move
        // events must name the actor most recently switched into that slot.
        // The limited protocol contract never accepts Transform/Illusion
        // aliases, and this remains only a synthetic fixture consistency check.
        Map<String,String> activeActorBySlot = new HashMap<>();
        for (String line : filtered) {
            String[] p = line.split("\\|", -1);
            String kind = p[1];
            if ("turn".equals(kind)) {
                if (p.length != 3 || ++countTurns != parseTurn(p[2])) throw invalid();
                continue;
            }
            if (p.length < 4 || !p[2].matches("p[12]a: [^|\\r\\n]{1,80}"))
                throw invalid();
            String slot = p[2].substring(0, 3);
            if (!slot.equals(input.ownSlot()) && !slot.equals(input.opponentSlot()))
                throw invalid();
            if ("switch".equals(kind) || "drag".equals(kind)) {
                if (p.length < 5 || p[3].isBlank() || p[4].isBlank()
                        || countTurns > input.expectedTurn()) throw invalid();
                activeActorBySlot.put(slot, p[2].trim());
                if (slot.equals(input.ownSlot())) ownAppearances++;
                else {
                    opponentAppearances++;
                    lastDisplayedOpponent = id(p[3].split(",", 2)[0]);
                }
            } else if ("move".equals(kind)) {
                if (countTurns == 0 || p[3].isBlank() || p.length > 5
                        || ownAppearances < 1 || opponentAppearances < 1
                        || !p[2].trim().equals(activeActorBySlot.get(slot)))
                    throw invalid();
                // A claimed targeted single-battle move must address the
                // current active actor, not a forged nickname on a valid slot.
                // Untargeted public moves may omit the fifth field entirely.
                if (p.length == 5 && !p[4].isBlank()) {
                    String target = p[4].trim();
                    if (target.length() < 4 || !target.matches("p[12]a: [^|\\r\\n]{1,80}"))
                        throw invalid();
                    String targetSlot = target.substring(0, 3);
                    if (!target.equals(activeActorBySlot.get(targetSlot)))
                        throw invalid();
                }
            }
        }
        if (countTurns != input.expectedTurn() || ownAppearances < 1
                || opponentAppearances != input.opponentAppearanceIndex()
                || !id(snapshot.opponent().species()).equals(lastDisplayedOpponent))
            throw invalid();

        PublicProtocolEvidence publicHistory = new PublicProtocolEvidence();
        publicHistory.update(input.framedPublicHistory());
        if (!publicHistory.complete() || !publicHistory.framed()
                || publicHistory.corrupted() || !publicHistory.coverageUnknown().isEmpty()
                || publicHistory.publicTurn() != input.expectedTurn()
                || publicHistory.reentryAmbiguous(input.opponentSlot()))
            throw invalid();
        List<String> replies = PublicReplyCoverage.currentReplies(
                publicHistory, input.opponentSlot());
        if (!new TreeSet<>(replies).equals(new TreeSet<>(snapshot.opponent().revealedMoves())))
            throw invalid(); // no move carried over from another appearance
        var hp = publicHistory.hp(input.opponentSlot());
        if (snapshot.opponent().hpKnown()
                && (!hp.known() || hp.value().low() > snapshot.opponent().hpFraction()
                    || hp.value().high() < snapshot.opponent().hpFraction()))
            throw invalid();
        return new Receipt(input.expectedTurn(), input.opponentAppearanceIndex(),
                legal.size(), replies.size());
    }
    private static int parseTurn(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException malformed) { throw invalid(); }
    }
}
