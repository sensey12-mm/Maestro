package com.maestroai.training;

import java.util.*;

/**
 * Bounded, deterministic two-decision shadow search over externally supplied,
 * public-information scenario payoffs. This is NOT a battle/damage simulator.
 * Branch utilities must come from observed or independently validated models;
 * missing opponent knowledge must not be filled with private battle data.
 * No live AI class references this engine.
 */
public final class ShadowSearchEngine {
    private ShadowSearchEngine() {}

    public record Outcome(String response, double probability, double immediateValue,
                          double uncertainty, String nextState) {
        public Outcome {
            if (response == null || response.isBlank() || nextState == null || nextState.isBlank()
                    || !Double.isFinite(probability) || probability < 0 || probability > 1
                    || !Double.isFinite(immediateValue) || !Double.isFinite(uncertainty)
                    || uncertainty < 0)
                throw new IllegalArgumentException("invalid outcome");
        }
    }
    public record Branch(String kind, String action, List<Outcome> outcomes) {
        public Branch {
            if (!"move".equals(kind) && !"switch".equals(kind) || action == null || action.isBlank())
                throw new IllegalArgumentException("invalid action");
            outcomes = List.copyOf(outcomes);
            if (outcomes.isEmpty()) throw new IllegalArgumentException("missing responses");
            double sum = 0;
            Set<String> seen = new HashSet<>();
            for (Outcome o : outcomes) {
                if (!seen.add(o.response())) throw new IllegalArgumentException("duplicate response");
                sum += o.probability();
            }
            if (Math.abs(sum - 1) > 1e-6) throw new IllegalArgumentException("probabilities must sum to one");
        }
        public String id() { return kind + ":" + action; }
    }
    public record Position(String key, List<Branch> branches, double terminalValue) {
        public Position {
            if (key == null || key.isBlank() || !Double.isFinite(terminalValue))
                throw new IllegalArgumentException("invalid position");
            branches = List.copyOf(branches);
            Set<String> seen = new HashSet<>();
            for (Branch branch : branches)
                if (!seen.add(branch.id())) throw new IllegalArgumentException("duplicate action");
        }
    }
    public record Ranked(String action, double expectedValue, double worstValue) {}
    public record Result(List<Ranked> ranking, String preferred, int nodes, int cacheHits,
                         boolean complete) {
        public Result { ranking = List.copyOf(ranking); }
    }
    private record CacheKey(String position, int depth) {}
    private record Value(double expected, double worst) {}
    private static final Comparator<Ranked> ORDER =
            Comparator.comparingDouble(Ranked::expectedValue).reversed()
                    .thenComparing(Comparator.comparingDouble(Ranked::worstValue).reversed())
                    .thenComparing(Ranked::action);

    /**
     * Depth two means two own decisions with an opponent response after each.
     * nodeBudget bounds evaluated actions (not wall time); cache lives only for
     * this invocation and is keyed by full public-state fixture ID and depth.
     */
    public static Result search(BattleSnapshot snapshot, String root,
                                Map<String, Position> positions, int nodeBudget,
                                double uncertaintyPenalty) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(positions, "positions");
        if (nodeBudget < 1 || !Double.isFinite(uncertaintyPenalty) || uncertaintyPenalty < 0)
            throw new IllegalArgumentException("invalid search limits");
        Map<String, Position> graph = Map.copyOf(positions);
        Position start = require(graph, root);
        if (start.branches().isEmpty()) throw new IllegalArgumentException("empty root");
        for (Position pos : graph.values())
            for (Branch branch : pos.branches())
                for (Outcome outcome : branch.outcomes())
                    require(graph, outcome.nextState());
        for (Branch branch : start.branches())
            snapshot.validateDecision(new BattleSnapshot.Decision(
                    branch.kind(), branch.action(), 0, Map.of(), "shadow search"));
        Runner runner = new Runner(graph, nodeBudget, uncertaintyPenalty);
        List<Ranked> ranked = new ArrayList<>();
        for (Branch branch : start.branches()) {
            if (runner.nodes >= nodeBudget) break;
            Value v = runner.branch(branch, 2);
            ranked.add(new Ranked(branch.id(), v.expected(), v.worst()));
        }
        ranked.sort(ORDER);
        return new Result(ranked, ranked.isEmpty() ? null : ranked.getFirst().action(),
                runner.nodes, runner.cacheHits, ranked.size() == start.branches().size()
                && !runner.exhausted);
    }
    private static Position require(Map<String, Position> graph, String key) {
        Position pos = graph.get(key);
        if (pos == null) throw new IllegalArgumentException("missing public state: " + key);
        return pos;
    }
    private static final class Runner {
        final Map<String, Position> graph;
        final int budget;
        final double penalty;
        final Map<CacheKey, Value> cache = new HashMap<>();
        int nodes, cacheHits;
        boolean exhausted;
        Runner(Map<String, Position> graph, int budget, double penalty) {
            this.graph = graph; this.budget = budget; this.penalty = penalty;
        }
        Value branch(Branch branch, int depth) {
            nodes++;
            double expected = 0, worst = Double.POSITIVE_INFINITY;
            for (Outcome outcome : branch.outcomes()) {
                if (outcome.probability() == 0) continue;
                double continuation = 0;
                if (depth > 1) continuation = value(outcome.nextState(), depth - 1).expected();
                double score = outcome.immediateValue() + continuation
                        - penalty * outcome.uncertainty();
                expected += outcome.probability() * score;
                worst = Math.min(worst, score);
            }
            return new Value(expected, worst);
        }
        Value value(String key, int depth) {
            CacheKey cacheKey = new CacheKey(key, depth);
            Value known = cache.get(cacheKey);
            if (known != null) { cacheHits++; return known; }
            Position position = require(graph, key);
            Value best = new Value(position.terminalValue(), position.terminalValue());
            if (depth > 0 && !position.branches().isEmpty()) {
                List<Branch> ordered = new ArrayList<>(position.branches());
                ordered.sort(Comparator.comparing(Branch::id));
                boolean found = false;
                for (Branch branch : ordered) {
                    if (nodes >= budget) { exhausted = true; break; }
                    Value candidate = branch(branch, depth);
                    if (!found || candidate.expected() > best.expected()
                            || candidate.expected() == best.expected()
                            && candidate.worst() > best.worst()) {
                        best = candidate; found = true;
                    }
                }
                if (!found) exhausted = true;
            }
            // Never cache a partially searched position as an exact result.
            if (!exhausted) cache.put(cacheKey, best);
            return best;
        }
    }
}
