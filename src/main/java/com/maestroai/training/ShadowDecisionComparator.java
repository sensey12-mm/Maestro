package com.maestroai.training;

import java.util.List;
import java.util.Objects;

/**
 * Diagnostic-only ranking of actions scored by an external evaluator.
 * This is not an independent simulator or a replacement for the live policy.
 */
public final class ShadowDecisionComparator {
    private ShadowDecisionComparator() {}

    public record Candidate(String kind, String id, double score) {
        public Candidate {
            if (!"move".equals(kind) && !"switch".equals(kind)) {
                throw new IllegalArgumentException("unknown action kind");
            }
            if (id == null || id.isBlank() || !Double.isFinite(score)) {
                throw new IllegalArgumentException("invalid candidate");
            }
        }
    }

    public record Comparison(Candidate actual, Candidate preferred, double scoreGap,
                             boolean disagreement) {}

    public static Comparison compare(BattleSnapshot snapshot, Candidate actual,
                                     List<Candidate> alternatives) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(alternatives, "alternatives");
        snapshot.validateDecision(asDecision(actual));
        Candidate preferred = actual;
        java.util.Set<String> unique = new java.util.HashSet<>();
        for (Candidate candidate : alternatives) {
            snapshot.validateDecision(asDecision(candidate));
            if (!unique.add(candidate.kind() + ":" + candidate.id())) {
                throw new IllegalArgumentException("duplicate candidate");
            }
            if (candidate.score() > preferred.score()) preferred = candidate;
        }
        boolean disagreement = !actual.kind().equals(preferred.kind())
                || !actual.id().equals(preferred.id());
        return new Comparison(actual, preferred,
                Math.max(0.0, preferred.score() - actual.score()), disagreement);
    }

    private static BattleSnapshot.Decision asDecision(Candidate candidate) {
        return new BattleSnapshot.Decision(candidate.kind(), candidate.id(),
                candidate.score(), java.util.Map.of(), "shadow diagnostic");
    }
}
