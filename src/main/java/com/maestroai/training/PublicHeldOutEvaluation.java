package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Offline, replay-group-held-out, predict-before-observe benchmark of existing
 * public-only hypotheses. No training, fitted opponent profiles, live actions
 * or calibrated probabilities. Training scores are diagnostics only.
 */
public final class PublicHeldOutEvaluation {
    private PublicHeldOutEvaluation() {}

    public enum Partition { TRAIN, HELD_OUT }
    public record Replay(PublicReplayCorpus.Source source, String spectatorLog, Partition partition) {
        public Replay {
            if (source == null || spectatorLog == null || partition == null)
                throw invalid();
        }
    }

    /** The observed hit fraction is withheld below 20 public-event horizons. */
    public record Metric(int evaluated, int candidateHits, int baselineHits,
                         Double observedCandidateRate, Double observedBaselineRate,
                         String baseline, String interpretation) {
        public Metric {
            if (evaluated < 0 || candidateHits < 0 || baselineHits < 0
                    || candidateHits > evaluated || baselineHits > evaluated
                    || baseline == null || interpretation == null) throw invalid();
            boolean enough = evaluated >= 20;
            if (!Set.of("FIRST_OBSERVED_NATIVE_MOVE","ALWAYS_MOVE").contains(baseline)
                    || !interpretation.equals(enough ? "EMPIRICAL_ONLY_NOT_CALIBRATED"
                    : "INSUFFICIENT_HORIZONS"))
                throw invalid();
            if (!enough && (observedCandidateRate != null || observedBaselineRate != null))
                throw invalid();
            if (enough && (observedCandidateRate == null || observedBaselineRate == null
                    || !Double.isFinite(observedCandidateRate)
                    || !Double.isFinite(observedBaselineRate)
                    || Math.abs(observedCandidateRate - (double) candidateHits / evaluated) > 1e-12
                    || Math.abs(observedBaselineRate - (double) baselineHits / evaluated) > 1e-12))
                throw invalid();
        }
        static Metric of(int n, int candidate, int baseline, String label) {
            boolean adequate = n >= 20;
            return new Metric(n, candidate, baseline,
                    adequate ? (double) candidate / n : null,
                    adequate ? (double) baseline / n : null,
                    label, adequate ? "EMPIRICAL_ONLY_NOT_CALIBRATED" : "INSUFFICIENT_HORIZONS");
        }
    }
    /** Abstentions are opaque unscored public-action audit decisions. */
    public record Segment(int acceptedReplays, int unsupportedTargetReplays,
                          int unscoredPublicDecisions, Metric moves, Metric switches) {
        public Segment {
            if (acceptedReplays < 0 || unsupportedTargetReplays < 0
                    || unscoredPublicDecisions < 0 || moves == null || switches == null)
                throw invalid();
        }
    }
    /** Receipt contains only de-identified source hashes, no original replay text. */
    public record Report(String splitSha256, String sourceScope, int trainReplays,
                         int heldOutReplays, Segment training, Segment heldOut,
                         boolean replayOverlapDetected, boolean crossReplayIdentityInferred,
                         boolean trainedParametersShared, boolean forecastProbabilityClaimed) {
        public Report {
            if (splitSha256 == null || !splitSha256.matches("[0-9a-f]{64}")
                    || sourceScope == null || trainReplays < 1 || heldOutReplays < 1
                    || training == null || heldOut == null
                    || !(sourceScope.equals(CobblemonMechanics.SOURCE)
                    || sourceScope.equals("OUTSIDE_PINNED_SOURCE_SCOPE"))
                    || replayOverlapDetected
                    || crossReplayIdentityInferred || trainedParametersShared
                    || forecastProbabilityClaimed) throw invalid();
        }
    }

    private record Verified(Replay input, PublicReplayCorpus.Evidence evidence,
                            CompetitiveFormatNormalizer.Result normalized) {}

    /**
     * Whole-replay split: source IDs AND input hashes must be unique. Never
     * choose a partition from battle outcomes or an actor/nickname. A fresh
     * PublicOpponentIntelligence is constructed for EACH replay and consumes
     * only its opponent's public actions in chronological order.
     */
    public static Report evaluate(List<Replay> inputs, CompetitiveFormatNormalizer.Target target) {
        if (inputs == null || inputs.size() < 2 || inputs.size() > 1000 || target == null)
            throw invalid();
        Set<String> ids = new HashSet<>(), hashes = new HashSet<>(), publicSequences = new HashSet<>();
        List<Verified> training = new ArrayList<>(), testing = new ArrayList<>();
        List<String> manifest = new ArrayList<>();
        for (Replay replay : inputs) {
            if (replay == null) throw invalid();
            // Phase 5.1 rights/privacy and Phase 5.2 format/version/ambiguity
            // gates run before any event is eligible for benchmarking.
            var admitted = PublicReplayCorpus.ingest(replay.source(), replay.spectatorLog());
            var normalized = CompetitiveFormatNormalizer.normalize(admitted, target);
            if (!ids.add(admitted.replayId()) || !hashes.add(admitted.inputSha256())
                    || !publicSequences.add(publicSequenceDigest(replay.spectatorLog(), admitted.format())))
                throw invalid();
            manifest.add(replay.partition() + "|" + admitted.replayId() + "|"
                    + admitted.inputSha256() + "|" + normalized.targetSourceScope());
            (replay.partition() == Partition.TRAIN ? training : testing)
                    .add(new Verified(replay, admitted, normalized));
        }
        if (training.isEmpty() || testing.isEmpty()) throw invalid();
        // Sorting fixes traversal order; boundaries are caller-assigned, never
        // selected using match outcomes. Neither segment trains a model.
        Comparator<Verified> order = Comparator.comparing(v -> v.evidence().replayId());
        training.sort(order);
        testing.sort(order);
        Collections.sort(manifest);
        Segment train = score(training);
        Segment heldOut = score(testing);
        return new Report(sha256(String.join("\n", manifest)),
                target.equals(CompetitiveFormatNormalizer.Target.pinnedSource())
                        ? CobblemonMechanics.SOURCE : "OUTSIDE_PINNED_SOURCE_SCOPE",
                training.size(), testing.size(), train, heldOut,
                false, false, false, false);
    }

    private static Segment score(List<Verified> replays) {
        int skipped=0, unsupported=0;
        int moveN=0, moveCandidate=0, moveBaseline=0;
        int switchN=0, switchCandidate=0, switchBaseline=0;
        for (Verified replay : replays) {
            // Out-of-scope mechanics cannot be transferred to Cobblemon.
            // The input remains permitted and appears in the count, but no
            // predictive comparison is advertised for that target.
            if (!CobblemonMechanics.SOURCE.equals(replay.normalized().targetSourceScope())) {
                unsupported++;
                continue;
            }
            PublicOpponentIntelligence observer = new PublicOpponentIntelligence();
            for (String line : PublicProtocolFilter.publicEvents(
                    List.of(replay.input().spectatorLog()))) {
                String[] fields = line.split("\\|", -1);
                // p1 actions cannot contaminate opponent-model evaluations.
                // This is a single-target gen9 singles foundation.
                if (fields.length > 2 && "p2a".equals(slot(fields[2])))
                    observer.accept(line);
            }
            var paired = observer.predictionBenchmark();
            var move = paired.moves();
            var sw = paired.switches();
            moveN += move.evaluations();
            moveCandidate += move.shadowMatches();
            moveBaseline += move.baselineMatches();
            switchN += sw.evaluations();
            switchCandidate += sw.shadowMatches();
            switchBaseline += sw.baselineMatches();
            skipped += paired.skippedUnknown();
        }
        return new Segment(replays.size(), unsupported, skipped,
                Metric.of(moveN, moveCandidate, moveBaseline, "FIRST_OBSERVED_NATIVE_MOVE"),
                Metric.of(switchN, switchCandidate, switchBaseline, "ALWAYS_MOVE"));
    }

    /**
     * Conservative clone barrier: strips nicknames/chat/player metadata and
     * hashes an ordered public action/effect sequence. Two different battles
     * with indistinguishable public sequences are rejected, not identified
     * as the same opponent. The digest never leaves this method.
     */
    private static String publicSequenceDigest(String raw, String format) {
        StringBuilder key = new StringBuilder(format);
        for (String line : PublicProtocolFilter.publicEvents(List.of(raw))) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 2) continue;
            String kind = parts[1];
            String actor = parts.length > 2 ? slot(parts[2]) : "";
            key.append('\n').append(kind).append('|').append(actor);
            // A complete public replay event contains more than its nickname.
            // Preserve state-relevant move/details/HP/status/effect content, but
            // never actor nicknames or private protocol payloads in this key.
            if (parts.length > 3)
                key.append('|').append(parts[3].toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", ""));
            if (kind.equals("turn") && parts.length > 2)
                key.append('|').append(parts[2]);
        }
        return sha256(key.toString());
    }

    private static String slot(String raw) {
        int colon = raw.indexOf(':');
        if (colon < 0) return "";
        String value = raw.substring(0, colon).trim();
        return value.matches("p[12]a") ? value : "";
    }
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unsupported held-out public evidence manifest");
    }
}
