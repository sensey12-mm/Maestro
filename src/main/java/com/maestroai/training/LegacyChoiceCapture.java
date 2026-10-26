package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Phase 7.3: opt-in, observation-only capture of an ACTUAL returned Legacy
 * action. Never provides actions to the battle AI. No opponent requests,
 * Pokémon UUIDs, player names, raw packets or private histories are exported.
 *
 * Source eligibility is checked using the existing current own-dispatch
 * lease and the full PUBLIC-envelope reconciliation. The result remains
 * an observation, not proof of installed Minecraft acceptance or downstream
 * independent custody; offline review is still mandatory.
 */
public final class LegacyChoiceCapture {
    public static final String PROPERTY = "maestroai.legacyChoiceCapture";
    public static final String ORIGIN = "LEGACY_CHOOSE_RETURN";
    private static final int MAX_CHUNKS = 512;
    private static final int MAX_BYTES = 65536;
    private static final int MAX_RECEIPTS = 64;
    private static final java.util.regex.Pattern HASH = java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final java.util.regex.Pattern ACTION =
            java.util.regex.Pattern.compile("(?:move:[a-z0-9][a-z0-9_-]{0,63}|switch:slot[1-6])");

    public enum Status {
        DISABLED, CAPTURED, UNKNOWN_ARTIFACT, EXPIRED_OWN_CHOICE, INVALID_ACTION,
        INCOMPLETE_PUBLIC_ENVELOPE, STALE_PUBLIC_TURN, AMBIGUOUS_PUBLIC_IDENTITY,
        HISTORY_MUTATION, DUPLICATE_TURN, CAPACITY
    }

    /** Only own move IDs or own party indices are exported. Never a Pokemon UUID. */
    public static final class Receipt {
        private final String id, artifactSha256, ownRequestSha256, publicSha256, action;
        private final int turn;
        private Receipt(String id,String artifact,String request,String publicHash,int turn,String action) {
            this.id=id; artifactSha256=artifact; ownRequestSha256=request;
            publicSha256=publicHash; this.turn=turn; this.action=action;
        }
        public String captureId() { return id; }
        public String origin() { return ORIGIN; }
        public String artifactSha256() { return artifactSha256; }
        public String ownRequestSha256() { return ownRequestSha256; }
        public String publicSha256() { return publicSha256; }
        public int turn() { return turn; }
        public String action() { return action; }

        /** A deliberately minimized, private-log-safe receipt; not a signature. */
        public String safeLine() {
            return "[MaestroAI] LEGACY_CHOICE_RECEIPT schema=phase7.3.v1"
                    +" capture="+id+" origin="+ORIGIN+" turn="+turn
                    +" action="+action+" artifact_sha256="+artifactSha256
                    +" own_request_sha256="+ownRequestSha256
                    +" public_sha256="+publicSha256
                    +" spectator_delivery=UNVERIFIED custody=REVIEW_REQUIRED";
        }
        @Override public String toString(){ return "LegacyChoiceReceipt[redacted]"; }
    }
    public record Observation(Status status, Receipt receipt) {
        public Observation {
            if ((status==Status.CAPTURED)!=(receipt!=null))
                throw new IllegalArgumentException("invalid capture outcome");
        }
        @Override public String toString(){return "LegacyChoiceObservation[status="+status+"]";}
    }

    private final boolean enabled;
    private final String artifactSha256;
    private final byte[] historySalt = new byte[32];
    private UUID battle;
    private boolean corrupt;
    private List<String> lastHistoryDigests = List.of();
    private final Set<Integer> usedTurns = new HashSet<>();
    private final List<Receipt> receipts = new ArrayList<>();

    /**
     * Standard production source uses the already-approved opt-in JVM
     * property and the actual installed-file receipt (not a claimed version).
     */
    public static LegacyChoiceCapture installed() {
        if(!Boolean.getBoolean(PROPERTY)) return new LegacyChoiceCapture(false, "UNKNOWN");
        return new LegacyChoiceCapture(true, InstalledArtifactReceipt.installedSha256());
    }

    /** Also enables synthetic testing without Fabric runtime; cannot confer custody. */
    public LegacyChoiceCapture(boolean enabled, String artifactSha256) {
        this.enabled=enabled;
        this.artifactSha256=artifactSha256;
        new SecureRandom().nextBytes(historySalt);
    }
    public boolean enabled(){ return enabled; }

    /**
     * Only call after LivePromotionGate returns the legacy-selected response,
     * before the own-choice lease is expired. Throws no untrusted exception to
     * the live caller, but caller also isolates it from choice return.
     */
    public synchronized Observation observe(UUID battleId, OwnRequestTransport.ChoiceProof proof,
                                            List<String> rawHistory, String legalOwnAction) {
        if(!enabled) return new Observation(Status.DISABLED,null);
        try { return observeChecked(battleId,proof,rawHistory,legalOwnAction); }
        catch(RuntimeException unavailable) {
            corrupt=true;
            return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        }
    }
    private Observation observeChecked(UUID battleId, OwnRequestTransport.ChoiceProof proof,
                                       List<String> rawHistory, String action) {
        if(battleId==null || rawHistory==null) return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        if(!battleId.equals(battle)) {
            battle=battleId; corrupt=false; lastHistoryDigests=List.of();
            usedTurns.clear(); receipts.clear();
        }
        if(corrupt)return new Observation(Status.HISTORY_MUTATION,null);
        if(!validHash(artifactSha256))return new Observation(Status.UNKNOWN_ARTIFACT,null);
        if(proof==null || !proof.currentFor(battleId) || !validHash(proof.requestDigest()) || proof.turn()<1)
            return new Observation(Status.EXPIRED_OWN_CHOICE,null);
        if(action==null || !ACTION.matcher(action).matches())
            return new Observation(Status.INVALID_ACTION,null);
        int turn=proof.turn();
        if(receipts.size()>=MAX_RECEIPTS)return new Observation(Status.CAPACITY,null);
        if(rawHistory.isEmpty() || rawHistory.size()>MAX_CHUNKS)
            return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        int size=0;
        for(String chunk:rawHistory) {
            if(chunk==null)return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
            size+=chunk.getBytes(StandardCharsets.UTF_8).length;
            if(size>MAX_BYTES)return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        }
        // Maintain a salted append-only check without retaining raw private chunks.
        List<String> hashes=new ArrayList<>();
        for(String chunk:rawHistory)
            hashes.add(sha256(historySalt,chunk.getBytes(StandardCharsets.UTF_8)));
        if(hashes.size()<lastHistoryDigests.size()
                || !hashes.subList(0,lastHistoryDigests.size()).equals(lastHistoryDigests)) {
            corrupt=true;
            return new Observation(Status.HISTORY_MUTATION,null);
        }
        lastHistoryDigests=List.copyOf(hashes);
        if(usedTurns.contains(turn))return new Observation(Status.DUPLICATE_TURN,null);

        List<String> publicEvents=PublicProtocolFilter.publicEvents(rawHistory);
        PublicEnvelopeProvenance.Audit provenance=
                PublicEnvelopeProvenance.reconcile(rawHistory,publicEvents);
        if(!provenance.complete() || publicEvents.isEmpty()
                || provenance.origins().size()!=publicEvents.size()
                || provenance.origins().stream().anyMatch(o->
                    o.route()==PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN))
            return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        PublicProtocolEvidence spectator=new PublicProtocolEvidence();
        spectator.update(rawHistory);
        if(!spectator.complete() || !spectator.framed() || spectator.corrupted()
                || !spectator.coverageUnknown().isEmpty())
            return new Observation(Status.INCOMPLETE_PUBLIC_ENVELOPE,null);
        if(spectator.publicTurn()!=turn
                || !publicEvents.getLast().equals("|turn|"+turn))
            return new Observation(Status.STALE_PUBLIC_TURN,null);
        if(spectator.reentryAmbiguous("p1a") || spectator.reentryAmbiguous("p2a")
                || publicEvents.stream().anyMatch(line->
                       line.startsWith("|replace|") || line.startsWith("|-transform|")
                       || line.startsWith("|detailschange|") || line.startsWith("|-formechange|")
                       || line.startsWith("|drag|")))
            return new Observation(Status.AMBIGUOUS_PUBLIC_IDENTITY,null);

        String id=UUID.randomUUID().toString();
        Receipt receipt=new Receipt(id,artifactSha256,proof.requestDigest(),
                publicDigest(publicEvents),turn,action);
        usedTurns.add(turn);
        receipts.add(receipt);
        return new Observation(Status.CAPTURED,receipt);
    }
    /** Bounded view for controlled local, private import only. */
    public synchronized List<Receipt> receipts(){return List.copyOf(receipts);}

    /** Identical length-delimited public stream digest to Phase7LegacyImport. */
    public static String publicDigest(List<String> events) {
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            for(String e:events) {
                byte[] bytes=e.getBytes(StandardCharsets.UTF_8);
                d.update(new byte[]{(byte)(bytes.length>>>24),(byte)(bytes.length>>>16),
                        (byte)(bytes.length>>>8),(byte)bytes.length});
                d.update(bytes);
            }
            return HexFormat.of().formatHex(d.digest());
        } catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String sha256(byte[] salt,byte[] data) {
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            d.update(salt);d.update(data);
            return HexFormat.of().formatHex(d.digest());
        } catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static boolean validHash(String s){return s!=null && HASH.matcher(s).matches();}
}
