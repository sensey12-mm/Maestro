package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.2: verification-only, offline Legacy decision intake.
 *
 * Existing PublicBattleCapture attests PUBLIC envelope routing, NOT whether
 * Legacy chose a given action. An independently reviewed owning-choice
 * receipt is mandatory; user-provided flags or replayed |move| events never
 * authenticate decisions. No production caller, live control or raw logs.
 */
public final class Phase7LegacyImport {
    private Phase7LegacyImport() {}
    private static final int MAX_HISTORY_EVENTS = 512;
    private static final int MAX_HISTORY_BYTES = 65536;
    private static final String SOURCE = "LEGACY_CHOOSE_RETURN";
    private static final java.util.regex.Pattern ID =
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final java.util.regex.Pattern HASH =
            java.util.regex.Pattern.compile("[a-f0-9]{64}");

    /**
     * Provenance recorded at the true own-choice return boundary by an
     * externally reviewed capture provider, NOT inferred from executed moves.
     * The SHA fields are binding checks, not signatures.
     */
    public record ChoiceReceipt(String captureId, String origin, String artifactSha256,
                                String ownRequestSha256, String publicSha256,
                                String scenarioFingerprint, int turn, String action) {
        public ChoiceReceipt {
            if (!validId(captureId) || !SOURCE.equals(origin)
                    || !validHash(artifactSha256) || !validHash(ownRequestSha256)
                    || !validHash(publicSha256) || !validHash(scenarioFingerprint)
                    || turn < 1 || !validAction(action))
                throw new IllegalArgumentException("unsupported choice receipt");
        }
    }

    /** Raw private bytes are accepted IN MEMORY only and never serialized. */
    public record Capture(UUID battleId, List<String> rawHistory, ChoiceReceipt choice) {
        public Capture {
            Objects.requireNonNull(battleId);
            Objects.requireNonNull(rawHistory);
            Objects.requireNonNull(choice);
            rawHistory = List.copyOf(rawHistory);
        }
    }

    /**
     * ReviewRequest has only opaque IDs and digests. The reviewer must check
     * an independently held original artifact and actual Legacy return/own
     * request provenance, as well as capture continuity and actor identity.
     * Trust cannot be established from a caller-authored boolean or SHA alone.
     */
    public record ReviewRequest(String captureId, String artifactSha256,
                                String rawHistorySha256, String ownRequestSha256,
                                String publicSha256, String choiceReceiptSha256) {}
    @FunctionalInterface public interface CustodyReviewer {
        boolean verify(ReviewRequest request);
    }
    public enum Status {
        ACCEPTED, NO_CUSTODY_REVIEW, INVALID_PROVENANCE, UNKNOWN_PUBLIC_EVIDENCE,
        STALE_TURN, AMBIGUOUS_IDENTITY, MISMATCHED_SCENARIO, ILLEGAL_ACTION,
        DUPLICATE_CAPTURE
    }
    /**
     * Opaque proof-bearing result: callers cannot directly manufacture an
     * ACCEPTED result, even if they can construct a LegacyRecord themselves.
     */
    public static final class ImportResult {
        private final Status status;
        private final Phase7Benchmark.LegacyRecord legacy;
        private final String publicSha256;
        private ImportResult(Status status, Phase7Benchmark.LegacyRecord legacy,
                             String publicSha256) {
            this.status=Objects.requireNonNull(status);
            if (status != Status.ACCEPTED && legacy != null)
                throw new IllegalArgumentException("rejected import cannot contain a decision");
            if (status == Status.ACCEPTED && (legacy == null || !validHash(publicSha256)))
                throw new IllegalArgumentException("accepted import lacks evidence");
            this.legacy=legacy;
            this.publicSha256=publicSha256;
        }
        public Status status() { return status; }
        public Phase7Benchmark.LegacyRecord legacy() { return legacy; }
        public String publicSha256() { return publicSha256; }
        public boolean accepted() { return status == Status.ACCEPTED; }
        @Override public boolean equals(Object other) {
            return other instanceof ImportResult r && status==r.status
                    && Objects.equals(legacy,r.legacy)
                    && Objects.equals(publicSha256,r.publicSha256);
        }
        @Override public int hashCode() { return Objects.hash(status,legacy,publicSha256); }
        @Override public String toString() { return "LegacyImportResult[status="+status+"]"; }
    }
    private static ImportResult rejected(Status status) {
        return new ImportResult(status,null,null);
    }

    /**
     * Import one capture: status codes are fixed and omit names, private
     * payloads, UUIDs, request text and exception messages.
     */
    public static ImportResult importCapture(Capture capture, Phase7Benchmark.Scenario scenario,
                                             CustodyReviewer custody) {
        Objects.requireNonNull(capture);
        Objects.requireNonNull(scenario);
        ChoiceReceipt choice=capture.choice();
        if(custody==null) return rejected(Status.NO_CUSTODY_REVIEW);
        if(!scenario.evidence().fresh()) return rejected(Status.STALE_TURN);
        if(!scenario.evidence().publicOnly() || !scenario.evidence().mechanicsSupported())
            return rejected(Status.UNKNOWN_PUBLIC_EVIDENCE);
        if(!choice.scenarioFingerprint().equals(Phase7Benchmark.stateFingerprint(scenario)))
            return rejected(Status.MISMATCHED_SCENARIO);
        if(choice.turn()!=scenario.snapshot().turn())
            return rejected(Status.STALE_TURN);
        String action=choice.action();
        boolean legal=action.startsWith("move:")
                ? scenario.snapshot().legalMoves().contains(action.substring(5))
                : scenario.snapshot().legalSwitches().contains(action.substring(7));
        if(!legal)return rejected(Status.ILLEGAL_ACTION);

        // Fail before invoking the reviewer on incomplete/malformed evidence.
        try {
            List<String> raw=capture.rawHistory();
            if(raw.isEmpty() || raw.size()>MAX_HISTORY_EVENTS
                    || raw.stream().mapToInt(x->x.getBytes(StandardCharsets.UTF_8).length).sum()>MAX_HISTORY_BYTES)
                return rejected(Status.INVALID_PROVENANCE);
            List<String> publicEvents=PublicProtocolFilter.publicEvents(raw);
            PublicEnvelopeProvenance.Audit audit=PublicEnvelopeProvenance.reconcile(raw,publicEvents);
            if(!audit.complete() || publicEvents.isEmpty()
                    || audit.origins().size()!=publicEvents.size()
                    || audit.origins().stream().anyMatch(o->
                        o.route()==PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN))
                return rejected(Status.INVALID_PROVENANCE);

            var provenance=new PublicProtocolEvidence();
            provenance.update(raw);
            if(provenance.corrupted() || !provenance.complete() || !provenance.framed()
                    || !provenance.coverageUnknown().isEmpty())
                return rejected(Status.UNKNOWN_PUBLIC_EVIDENCE);
            if(provenance.publicTurn()!=choice.turn()
                    || !publicEvents.getLast().equals("|turn|"+choice.turn()))
                return rejected(Status.STALE_TURN);
            if(provenance.reentryAmbiguous("p1a") || provenance.reentryAmbiguous("p2a")
                    || publicEvents.stream().anyMatch(line->
                       line.startsWith("|replace|") || line.startsWith("|-transform|")
                       || line.startsWith("|detailschange|") || line.startsWith("|-formechange|")
                       || line.startsWith("|drag|")))
                return rejected(Status.AMBIGUOUS_IDENTITY);
            // Reconstruct through the existing diagnostic capture path too;
            // it never confirms actual spectator delivery or source authenticity.
            var replay=new PublicBattleCapture();
            var observed=replay.observeWithProvenance(capture.battleId(),raw);
            if(replay.incomplete() || observed.envelopes().size()!=publicEvents.size()
                    || replay.recordedEvents()!=publicEvents.size()
                    || observed.envelopes().stream().anyMatch(s->!s.contains("branch_attested=true")))
                return rejected(Status.INVALID_PROVENANCE);

            String publicHash=publicDigest(publicEvents);
            if(!choice.publicSha256().equals(publicHash))
                return rejected(Status.INVALID_PROVENANCE);
            String rawHash=sha256(raw);
            String choiceHash=sha256(List.of(SOURCE,choice.artifactSha256(),
                    choice.ownRequestSha256(),choice.publicSha256(),choice.scenarioFingerprint(),
                    Integer.toString(choice.turn()),choice.action(),choice.captureId()));
            var request=new ReviewRequest(choice.captureId(),choice.artifactSha256(),
                    rawHash,choice.ownRequestSha256(),publicHash,choiceHash);
            boolean reviewed;
            try { reviewed=custody.verify(request); }
            catch(RuntimeException unavailable) { reviewed=false; }
            if(!reviewed)return rejected(Status.NO_CUSTODY_REVIEW);
            return new ImportResult(Status.ACCEPTED,new Phase7Benchmark.LegacyRecord(
                    choice.captureId(),choice.turn(),choice.scenarioFingerprint(),action,true),publicHash);
        } catch(RuntimeException unexpected) {
            return rejected(Status.INVALID_PROVENANCE);
        }
    }

    /**
     * Phase 7.3 adapter: actual opt-in production receipts carry a returned
     * Legacy action, but NO simulated benchmark state claim. Bind the observed
     * action to the supplied scenario only under the existing independent
     * custody callback. Never mistake the in-game diagnostic line itself for
     * proof of reviewer custody or Minecraft acceptance.
     */
    public static ImportResult importObserved(LegacyChoiceCapture.Receipt observed,
            UUID privateBattleId, List<String> privateHistory,
            Phase7Benchmark.Scenario scenario, CustodyReviewer custody) {
        if (observed==null || privateBattleId==null || privateHistory==null || scenario==null)
            return rejected(Status.INVALID_PROVENANCE);
        try {
            if (!SOURCE.equals(observed.origin()))
                return rejected(Status.INVALID_PROVENANCE);
            var choice = new ChoiceReceipt(observed.captureId(), observed.origin(),
                    observed.artifactSha256(), observed.ownRequestSha256(),
                    observed.publicSha256(), Phase7Benchmark.stateFingerprint(scenario),
                    observed.turn(), observed.action());
            return importCapture(new Capture(privateBattleId,privateHistory,choice),
                    scenario,custody);
        } catch(RuntimeException invalid) {
            // Never propagate private payloads or exception descriptions.
            return rejected(Status.INVALID_PROVENANCE);
        }
    }

    /** Duplicate capture IDs cannot independently contribute multiple rows. */
    public static List<ImportResult> importBatch(List<Capture> captures,
            Map<String,Phase7Benchmark.Scenario> scenarios, CustodyReviewer custody) {
        Objects.requireNonNull(captures);
        Objects.requireNonNull(scenarios);
        if(captures.size()>100)throw new IllegalArgumentException("batch exceeds limit");
        var seen=new HashSet<String>();
        List<ImportResult> output=new ArrayList<>();
        for(Capture capture:captures) {
            if(capture==null){output.add(rejected(Status.INVALID_PROVENANCE));continue;}
            if(!seen.add(capture.choice().captureId())) {
                output.add(rejected(Status.DUPLICATE_CAPTURE));
                continue;
            }
            var scenario=scenarios.get(capture.choice().captureId());
            output.add(scenario==null ? rejected(Status.MISMATCHED_SCENARIO)
                    : importCapture(capture,scenario,custody));
        }
        return List.copyOf(output);
    }

    /** Compare actions only; never assign real tactical quality or win rates. */
    public record Comparison(Status status, String scenarioId, String publicSha256,
                             String legacyAction, String baselineAction, String shadowAction,
                             Boolean baselineAgreement, Boolean shadowAgreement,
                             boolean shadowIllegal, boolean shadowAbstained) {
        public String json() {
            // User-defined strings are validated strict IDs/actions or hashes.
            return "{\"schema\":\"phase7.2.v1\",\"status\":\""+status+"\",\"scenarioId\":"
                    +quote(scenarioId)+",\"publicSha256\":"+quote(publicSha256)
                    +",\"legacyAction\":"+quote(legacyAction)
                    +",\"baselineAction\":"+quote(baselineAction)
                    +",\"shadowAction\":"+quote(shadowAction)
                    +",\"baselineAgreement\":"+baselineAgreement
                    +",\"shadowAgreement\":"+shadowAgreement
                    +",\"shadowIllegal\":"+shadowIllegal
                    +",\"shadowAbstained\":"+shadowAbstained
                    +",\"scope\":\"observed-action-agreement-only;no-minecraft;no-win-rate\"}";
        }
    }
    public static Comparison compare(Phase7Benchmark.Scenario scenario, ImportResult imported,
                                     long seed, Phase7Benchmark.DecisionPolicy shadow) {
        Objects.requireNonNull(scenario);
        Objects.requireNonNull(imported);
        if(!imported.accepted())
            return new Comparison(imported.status(),scenario.id(),null,null,null,null,null,null,false,true);
        if(!imported.legacy().publicStateFingerprint().equals(Phase7Benchmark.stateFingerprint(scenario))
                || imported.legacy().turn()!=scenario.snapshot().turn())
            return new Comparison(Status.MISMATCHED_SCENARIO,scenario.id(),null,null,null,null,null,null,false,true);
        String baseline=Phase7Benchmark.simpleBaseline(scenario.snapshot(),seed);
        String shadowAction=null;
        boolean illegal=false;
        if(shadow!=null) {
            try {
                String action=shadow.choose(scenario.snapshot(),seed);
                if(action!=null && validAction(action) && (
                        action.startsWith("move:") && scenario.snapshot().legalMoves().contains(action.substring(5))
                        || action.startsWith("switch:") && scenario.snapshot().legalSwitches().contains(action.substring(7)))) {
                    shadowAction=action;
                } else if(action!=null)illegal=true;
            } catch(RuntimeException unavailable) { illegal=true; }
        }
        String actual=imported.legacy().action();
        return new Comparison(Status.ACCEPTED,scenario.id(),imported.publicSha256(),
                actual,baseline,shadowAction,Objects.equals(actual,baseline),
                shadowAction==null?null:Objects.equals(actual,shadowAction),illegal,shadowAction==null);
    }

    /** Public-only event stream fingerprint: delimiter-safe, independent of raw secret bytes. */
    public static String publicDigest(List<String> events) {
        Objects.requireNonNull(events);
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            for(String e:events) {
                byte[] bytes=e.getBytes(StandardCharsets.UTF_8);
                d.update(new byte[]{(byte)(bytes.length>>>24),(byte)(bytes.length>>>16),
                        (byte)(bytes.length>>>8),(byte)bytes.length});
                d.update(bytes);
            }
            return HexFormat.of().formatHex(d.digest());
        } catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    public static String sha256(List<String> parts) {
        Objects.requireNonNull(parts);
        return publicDigest(parts);
    }
    private static boolean validHash(String s){return s!=null && HASH.matcher(s).matches();}
    private static boolean validId(String s){return s!=null && ID.matcher(s).matches();}
    private static boolean validAction(String s){
        if(s==null)return false;
        return (s.startsWith("move:") && validId(s.substring(5)))
                || (s.startsWith("switch:") && validId(s.substring(7)));
    }
    private static String quote(String s){return s==null?"null":"\""+s+"\"";}
}
