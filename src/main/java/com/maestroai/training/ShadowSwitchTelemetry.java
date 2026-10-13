package com.maestroai.training;

import java.util.*;

/** Structured shadow telemetry; no live battle decision authority. */
public final class ShadowSwitchTelemetry {
    public record TurnComparison(int turn, String opponentAppearance, String legacyAction,
            String shadowPreference, boolean comparable, boolean disagreement,
            Map<String, Double> scoredActions, Map<String, Set<String>> unknownByAction,
            Map<String, Map<String, String>> evidenceSources, int loopCandidates,
            boolean mechanicsComplete) {
        public TurnComparison {
            scoredActions = Map.copyOf(scoredActions);
            unknownByAction = Map.copyOf(unknownByAction);
            evidenceSources = Map.copyOf(evidenceSources);
        }
    }
    private final List<TurnComparison> comparisons = new ArrayList<>();
    public void reset() { comparisons.clear(); }
    public List<TurnComparison> comparisons() { return List.copyOf(comparisons); }

    public TurnComparison record(PublicBattleObserver observer, String opponentSlot,
            BattleSnapshot snapshot, List<ShadowSwitchingEvaluator.ObservedCandidate> candidates,
            String legacyAction) {
        Objects.requireNonNull(observer);
        var result = ShadowSwitchingEvaluator.comparePublic(observer, opponentSlot,
                observer.switchHistory().events().size(), snapshot, candidates, legacyAction);
        var evaluation = result.evaluation();
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Set<String>> unknown = new LinkedHashMap<>();
        Map<String, Map<String, String>> provenance = new LinkedHashMap<>();
        for (var ranking : evaluation.rankings()) {
            if (ranking.score() != null) scores.put(ranking.action(), ranking.score());
            unknown.put(ranking.action(), ranking.unknownFields());
            provenance.put(ranking.action(), ranking.provenance());
        }
        TurnComparison comparison = new TurnComparison(observer.publicTurn(),
                result.context().opponentIdentity(), legacyAction,
                evaluation.preferredAction(), evaluation.fullyComparable(),
                evaluation.disagreement(), scores, unknown, provenance,
                observer.switchHistory().candidateLoops().size(),
                result.publicDamageFactors().complete());
        comparisons.add(comparison);
        return comparison;
    }
}
