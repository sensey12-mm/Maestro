package com.maestroai.training;

import java.util.*;

/**
 * Independent, shadow-only switch decision comparison.
 * Inputs are externally estimated utility components, NOT verified Cobblemon
 * damage or inferred hidden opponent information.
 */
public final class ShadowSwitchingEvaluator {
    private ShadowSwitchingEvaluator() {}

    public record Candidate(String action, double immediateUtility, double incomingRisk,
                            double entryHazardCost, double setupOpportunity,
                            double preservationValue, double repeatSwitchCost,
                            boolean legal) {
        public Candidate {
            if (action == null || action.isBlank()) throw new IllegalArgumentException("blank action");
            for (double value : new double[]{immediateUtility,incomingRisk,entryHazardCost,
                    setupOpportunity,preservationValue,repeatSwitchCost})
                if (!Double.isFinite(value)) throw new IllegalArgumentException("nonfinite score");
            if (incomingRisk < 0 || entryHazardCost < 0 || repeatSwitchCost < 0)
                throw new IllegalArgumentException("negative risk/cost");
        }
    }
    public record Ranking(String action, double score) {}
    public record Evaluation(List<Ranking> rankings, String preferredAction,
                             String legacyAction, boolean disagreement) {}

    /** A missing value stays UNKNOWN; it is never silently treated as zero. */
    public record Estimate(Double value, String provenance) {
        public Estimate {
            if (value != null && !Double.isFinite(value))
                throw new IllegalArgumentException("nonfinite estimate");
            if (provenance == null || provenance.isBlank())
                throw new IllegalArgumentException("missing provenance");
        }
        public static Estimate unknown(String reason) { return new Estimate(null, reason); }
        public static Estimate observed(double value, String source) { return new Estimate(value, source); }
    }
    public record ObservedCandidate(String action, Estimate immediateUtility,
                                    Estimate incomingRisk, Estimate entryHazardCost,
                                    Estimate setupOpportunity, Estimate preservationValue,
                                    Estimate repeatSwitchCost, boolean legal) {
        public ObservedCandidate {
            Objects.requireNonNull(action);
            Objects.requireNonNull(immediateUtility);
            Objects.requireNonNull(incomingRisk);
            Objects.requireNonNull(entryHazardCost);
            Objects.requireNonNull(setupOpportunity);
            Objects.requireNonNull(preservationValue);
            Objects.requireNonNull(repeatSwitchCost);
        }
    }
    public record EvidenceRanking(String action, Double score, Set<String> unknownFields,
                                  Map<String, String> provenance) {}
    public record EvidenceEvaluation(List<EvidenceRanking> rankings, String preferredAction,
                                     String legacyAction, boolean disagreement,
                                     boolean fullyComparable) {}

    /**
     * Only score fully specified legal candidates. An unknown competitor means
     * there is no justified overall winner or disagreement assertion.
     */
    public static EvidenceEvaluation evaluateObserved(BattleSnapshot snapshot,
            List<ObservedCandidate> candidates, String legacyAction) {
        Objects.requireNonNull(snapshot);
        if (candidates == null || candidates.isEmpty())
            throw new IllegalArgumentException("no candidates");
        Set<String> seen = new HashSet<>();
        List<EvidenceRanking> results = new ArrayList<>();
        boolean unknownLegal = false;
        for (ObservedCandidate c : candidates) {
            if (c == null || !seen.add(c.action()))
                throw new IllegalArgumentException("duplicate/null candidate");
            if (!c.legal()) continue;
            String[] split = c.action().split(":", 2);
            if (split.length != 2 || !(split[0].equals("move") && snapshot.legalMoves().contains(split[1])
                    || split[0].equals("switch") && snapshot.legalSwitches().contains(split[1])))
                throw new IllegalArgumentException("action not legal in snapshot: " + c.action());
            String[] names = {"immediateUtility", "incomingRisk", "entryHazardCost",
                    "setupOpportunity", "preservationValue", "repeatSwitchCost"};
            Estimate[] values = {c.immediateUtility(), c.incomingRisk(), c.entryHazardCost(),
                    c.setupOpportunity(), c.preservationValue(), c.repeatSwitchCost()};
            Set<String> unknown = new LinkedHashSet<>();
            Map<String, String> provenance = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i++) {
                provenance.put(names[i], values[i].provenance());
                if (values[i].value() == null) unknown.add(names[i]);
                else if ((i == 1 || i == 2 || i == 5) && values[i].value() < 0)
                    throw new IllegalArgumentException("negative cost: " + names[i]);
            }
            if (!unknown.isEmpty()) unknownLegal = true;
            Double score = unknown.isEmpty() ? values[0].value() - values[1].value()
                    - values[2].value() + values[3].value() + values[4].value()
                    - values[5].value() : null;
            results.add(new EvidenceRanking(c.action(), score, Set.copyOf(unknown),
                    Map.copyOf(provenance)));
        }
        if (results.isEmpty()) throw new IllegalArgumentException("no legal candidates");
        results.sort(Comparator.comparing((EvidenceRanking e) -> e.score() == null)
                .thenComparing((EvidenceRanking e) -> e.score() == null ? 0 : -e.score())
                .thenComparing(EvidenceRanking::action));
        String preferred = unknownLegal ? null : results.get(0).action();
        return new EvidenceEvaluation(List.copyOf(results), preferred, legacyAction,
                preferred != null && legacyAction != null && !preferred.equals(legacyAction),
                !unknownLegal);
    }

    /** Observed facts only; utility remains UNKNOWN until a validated estimator supplies it. */
    public record PublicContext(String opponentIdentity, Set<String> observedOpponentMoves,
                                Set<String> observedRoles, boolean itemSuppressed,
                                String knownChoiceLock, int observedSwitches,
                                boolean opponentBoostsUncertain) {}
    public static PublicContext publicContext(PublicBattleObserver observer, String opponentSlot,
                                              int observedSwitches) {
        Objects.requireNonNull(observer);
        if (observedSwitches < 0) throw new IllegalArgumentException("negative switch count");
        String identity = observer.activeIdentity(opponentSlot);
        var assessment = observer.opponentIntelligence().activeAssessment(opponentSlot);
        Set<String> moves = assessment == null ? Set.of() : assessment.threat().moves();
        Set<String> roles = new LinkedHashSet<>();
        for (var role : observer.opponentIntelligence().activeRoles(opponentSlot))
            roles.add(role.role());
        return new PublicContext(identity, Set.copyOf(moves), Set.copyOf(roles),
                identity != null && observer.itemSuppressed(identity),
                identity == null ? null : observer.observedChoiceLock(identity),
                observedSwitches, identity != null && observer.stagesUncertain(identity));
    }
    public record ShadowComparison(PublicContext context, EvidenceEvaluation evaluation,
                                   ShadowMechanicsPredictor.DamagePrediction publicDamageFactors) {}
    /**
     * No exact incoming-damage or setup-risk estimate is derived from a revealed
     * move name alone. Missing estimates remain unknown, suppressing rankings.
     */
    public static ShadowComparison comparePublic(PublicBattleObserver observer,
            String opponentSlot, int observedSwitches, BattleSnapshot snapshot,
            List<ObservedCandidate> candidates, String legacyAction) {
        PublicContext context = publicContext(observer, opponentSlot, observedSwitches);
        String item = context.opponentIdentity() == null ? null
                : observer.currentItem(context.opponentIdentity());
        String ability = context.opponentIdentity() == null ? null
                : observer.revealedAbility(context.opponentIdentity());
        var factors = ShadowMechanicsPredictor.damageFactors(item, ability,
                "unknown", "unknown", !context.itemSuppressed(), true, false);
        return new ShadowComparison(context,
                evaluateObserved(snapshot, candidates, legacyAction), factors);
    }

    public static Evaluation evaluate(List<Candidate> candidates, String legacyAction) {
        if (candidates == null || candidates.isEmpty()) throw new IllegalArgumentException("no candidates");
        Set<String> seen = new HashSet<>();
        List<Ranking> ranked = new ArrayList<>();
        for (Candidate c : candidates) {
            if (c == null || !seen.add(c.action())) throw new IllegalArgumentException("duplicate/null candidate");
            if (!c.legal()) continue;
            double score = c.immediateUtility() - c.incomingRisk()
                    - c.entryHazardCost() + c.setupOpportunity()
                    + c.preservationValue() - c.repeatSwitchCost();
            ranked.add(new Ranking(c.action(), score));
        }
        if (ranked.isEmpty()) throw new IllegalArgumentException("no legal candidates");
        ranked.sort(Comparator.comparingDouble(Ranking::score).reversed()
                .thenComparing(Ranking::action));
        return new Evaluation(List.copyOf(ranked), ranked.get(0).action(),
                legacyAction, legacyAction != null && !legacyAction.equals(ranked.get(0).action()));
    }
}
