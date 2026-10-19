package com.maestroai.training;

import java.util.*;
import java.util.function.BooleanSupplier;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.ShadowMechanicsEvaluation.*;

/**
 * Shadow-only universal envelope across independently evaluated public-rule scenarios.
 *
 * This does NOT infer an opponent's hidden item/ability or prove an exhaustive set
 * of possibilities. A source-/configuration-bound caller must establish that every
 * materially different current modifier state is covered. UNKNOWN is mandatory
 * if that finite exhaustiveness cannot be established. Scenarios never gain
 * authority over live Legacy decisions.
 */
public final class ModifierScenarioEnvelope {
    private ModifierScenarioEnvelope() {}

    public static final int MAX_SCENARIOS = 16;

    public record Scenario(String label, Comparison comparison) {
        public Scenario {
            if (label == null || label.isBlank() || comparison == null)
                throw new IllegalArgumentException("missing conditional scenario");
        }
    }

    public enum Reason {
        DOMAIN_NOT_EXHAUSTIVELY_CERTIFIED,
        MISSING_OR_DUPLICATE_SCENARIO,
        INCOMPLETE_ACTION_OR_OUTCOME,
        ACTION_UNIVERSE_MISMATCH,
        SOURCE_LEASE_EXPIRED,
        INTERVALS_OVERLAP_OR_TIE
    }

    /** Bounds are resource-exchange intervals over ALL certified scenarios,
     * not an expectation, guessed modifier distribution or win probability. */
    public record Audit(Map<String, Utility> bounds, String preferredConditional, Set<Reason> reasons) {
        public Audit {
            bounds = Map.copyOf(bounds);
            reasons = Set.copyOf(reasons);
            if (preferredConditional != null && (!bounds.containsKey(preferredConditional) || !reasons.isEmpty()))
                throw new IllegalArgumentException("preference without universal proof");
        }
        public boolean complete() { return reasons.isEmpty(); }
        public String safeLine() {
            return "ModifierScenarioEnvelope actions=" + bounds.size()
                    + " complete=" + complete()
                    + " strictPreference=" + (preferredConditional != null)
                    + " unresolved=" + new TreeSet<>(reasons)
                    + " hiddenModifierProbability=NONE advisoryOnly=true livePromotion=false";
        }
    }

    private static Audit unknown(Reason reason) {
        return new Audit(Map.of(), null, Set.of(reason));
    }

    private static boolean current(BooleanSupplier sourceCurrent) {
        try { return sourceCurrent.getAsBoolean(); }
        catch (RuntimeException invalid) { return false; }
    }

    /**
     * Each Comparison must come from source-scoped conditional evaluation of
     * the identical current legal root actions and witnessed public replies.
     * For real unrevealed opponents, callers do not possess an exhaustive domain
     * certificate by default; this method must then return UNKNOWN.
     */
    public static Audit compare(List<Scenario> scenarios, Known<Boolean> exhaustiveDomain,
                                BooleanSupplier sourceCurrent) {
        Objects.requireNonNull(exhaustiveDomain);
        Objects.requireNonNull(sourceCurrent);
        if (!current(sourceCurrent)) return unknown(Reason.SOURCE_LEASE_EXPIRED);
        if (!exhaustiveDomain.known() || !exhaustiveDomain.value())
            return unknown(Reason.DOMAIN_NOT_EXHAUSTIVELY_CERTIFIED);
        if (scenarios == null || scenarios.isEmpty() || scenarios.size() > MAX_SCENARIOS)
            return unknown(Reason.MISSING_OR_DUPLICATE_SCENARIO);
        Set<String> names = new HashSet<>();
        Map<String, Utility> hull = new TreeMap<>();
        Set<String> firstActions = null;
        for (Scenario scenario : scenarios) {
            if (scenario == null || !names.add(scenario.label()))
                return unknown(Reason.MISSING_OR_DUPLICATE_SCENARIO);
            Comparison comparison = scenario.comparison();
            if (!comparison.candidateCoverageComplete() || comparison.rankings().size() < 2
                    || comparison.evidence() == null || !comparison.evidence().contains("advisoryOnly=true"))
                return unknown(Reason.INCOMPLETE_ACTION_OR_OUTCOME);
            Set<String> actions = new HashSet<>();
            for (Ranking rank : comparison.rankings()) {
                if (rank == null || rank.action() == null || !actions.add(rank.action())
                        || !rank.utility().known() || !rank.unknown().isEmpty()
                        || rank.lines().isEmpty()
                        || rank.lines().stream().anyMatch(l -> l == null || !l.utility().known() || !l.unknown().isEmpty()))
                    return unknown(Reason.INCOMPLETE_ACTION_OR_OUTCOME);
                Utility u = rank.utility().value();
                if (firstActions != null && !firstActions.contains(rank.action()))
                    return unknown(Reason.ACTION_UNIVERSE_MISMATCH);
                Utility past = hull.get(rank.action());
                hull.put(rank.action(), past == null ? u
                        : new Utility(Math.min(past.low(), u.low()), Math.max(past.high(), u.high())));
            }
            if (firstActions == null) firstActions = Set.copyOf(actions);
            else if (!firstActions.equals(actions)) return unknown(Reason.ACTION_UNIVERSE_MISMATCH);
        }
        if (!current(sourceCurrent)) return unknown(Reason.SOURCE_LEASE_EXPIRED);
        String preferred = null;
        for (var candidate : hull.entrySet()) {
            if (hull.entrySet().stream().filter(e -> !e.getKey().equals(candidate.getKey()))
                    .allMatch(e -> candidate.getValue().low() > e.getValue().high())) {
                preferred = candidate.getKey();
                break;
            }
        }
        return new Audit(hull, preferred,
                preferred == null ? Set.of(Reason.INTERVALS_OVERLAP_OR_TIE) : Set.of());
    }
}
