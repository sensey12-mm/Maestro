package com.maestroai.training;

import java.util.*;

/** Conditional public state; never a claim about the unknown opponent's next action. */
public final class PublicTransitionState {
    private PublicTransitionState() {}
    public enum Kind { ATTACK, SETUP, SWITCH, FORCED_CONTINUATION, UNKNOWN }
    public static final Set<String> STATS = Set.of("atk", "def", "spa", "spd", "spe", "accuracy", "evasion");

    /** UNKNOWN is different from a known empty collection, zero, or healthy status. */
    public record Known<T>(T value, String source) {
        public Known {
            if (value == null && source != null || value != null && (source == null || source.isBlank()))
                throw new IllegalArgumentException("known evidence requires a source");
        }
        public boolean known() { return value != null; }
        public static <T> Known<T> unknown() { return new Known<>(null, null); }
        public static <T> Known<T> of(T value, String source) {
            return new Known<>(Objects.requireNonNull(value), source);
        }
    }

    /** Known layers may be partial; an empty partial map does not mean no hazards. */
    public record Hazards(Map<String, Integer> layers, boolean complete, String source) {
        public Hazards {
            layers = sorted(layers);
            if ((!layers.isEmpty() || complete) && (source == null || source.isBlank()))
                throw new IllegalArgumentException("hazards require public evidence");
            for (var e : layers.entrySet())
                if (e.getKey().isBlank() || e.getValue() < 0 || e.getValue() > 3)
                    throw new IllegalArgumentException("invalid hazard layers");
        }
        public static Hazards unknown() { return new Hazards(Map.of(), false, null); }
    }

    public record Actor(String id, BattleSnapshot.Combatant facts,
                        Known<PublicStateSearchAdapter.Interval> hp,
                        Map<String, Integer> stages, Known<String> status) {
        public Actor {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("missing public actor");
            Objects.requireNonNull(facts); Objects.requireNonNull(hp); Objects.requireNonNull(status);
            checkHp(hp); stages = PublicTransitionState.stages(stages); checkStatus(status);
            if (facts.fainted() && (!hp.known() || hp.value().high() != 0))
                throw new IllegalArgumentException("fainted actor requires zero public HP");
        }
    }

    /** Fields are conditional own-action effects/last observations, never a resolved end-of-turn position. */
    public record State(Actor self, Actor opponent, Hazards selfHazards, Hazards opponentHazards,
                        Set<String> ambiguity, Set<String> unresolved, Kind actionKind, String action,
                        List<String> legalMoves, List<String> legalSwitches,
                        boolean nextRequestKnown, boolean forcedAction,
                        boolean forcedReplacement, boolean trapped) {
        public State {
            Objects.requireNonNull(self); Objects.requireNonNull(opponent);
            Objects.requireNonNull(selfHazards); Objects.requireNonNull(opponentHazards);
            Objects.requireNonNull(actionKind); Objects.requireNonNull(action);
            ambiguity = sortedSet(ambiguity); unresolved = sortedSet(unresolved);
            legalMoves = names(legalMoves); legalSwitches = names(legalSwitches);
        }
    }

    /** Own reserve data, explicitly bound to the switch action, never an opponent party. */
    public record SwitchTarget(BattleSnapshot.Combatant facts, Known<String> status,
                               Known<PublicStateSearchAdapter.Interval> entryDamage,
                               Known<Integer> entrySpeedDelta) {
        public SwitchTarget {
            Objects.requireNonNull(facts); Objects.requireNonNull(status); Objects.requireNonNull(entryDamage); checkStatus(status);
            Objects.requireNonNull(entrySpeedDelta);
            if (entrySpeedDelta.known() && !Set.of(0, -1).contains(entrySpeedDelta.value()))
                throw new IllegalArgumentException("unsupported ordinary entry speed delta");
        }
        public SwitchTarget(BattleSnapshot.Combatant facts, Known<String> status,
                            Known<PublicStateSearchAdapter.Interval> entryDamage) {
            this(facts, status, entryDamage, Known.unknown());
        }
    }

    public record Context(Known<PublicStateSearchAdapter.Interval> selfHp,
                          Known<PublicStateSearchAdapter.Interval> opponentHp,
                          Known<String> selfStatus, Known<String> opponentStatus,
                          Hazards selfHazards, Hazards opponentHazards,
                          Map<String, SwitchTarget> switchTargets,
                          Map<String, Kind> moveKinds, Set<String> ambiguity) {
        public Context {
            Objects.requireNonNull(selfHp); Objects.requireNonNull(opponentHp);
            Objects.requireNonNull(selfStatus); Objects.requireNonNull(opponentStatus);
            Objects.requireNonNull(selfHazards); Objects.requireNonNull(opponentHazards);
            checkHp(selfHp); checkHp(opponentHp); checkStatus(selfStatus); checkStatus(opponentStatus);
            switchTargets = sorted(switchTargets); moveKinds = sorted(moveKinds); ambiguity = sortedSet(ambiguity);
        }
        public static Context unknown() {
            return new Context(Known.unknown(), Known.unknown(), Known.unknown(), Known.unknown(),
                    Hazards.unknown(), Hazards.unknown(), Map.of(), Map.of(), Set.of("APPEARANCE_ONLY"));
        }
    }

    /**
     * Verified conditional effect, not a probability of successful execution.
     * completeOwnEffect means the caller verified all own-action effects under
     * this scenario's conditions; otherwise unspecified fields become UNKNOWN.
     * It never resolves turn order, the opponent response or future requests.
     */
    public record Effect(Kind kind, Known<PublicStateSearchAdapter.Interval> targetDamage,
                         Map<String, Integer> selfStageDeltas, Known<String> targetStatusAfter,
                         Hazards targetHazardsAfter, boolean completeOwnEffect, String source) {
        public Effect {
            Objects.requireNonNull(kind); Objects.requireNonNull(targetDamage); Objects.requireNonNull(targetStatusAfter); checkStatus(targetStatusAfter);
            if (source != null && source.isBlank()) throw new IllegalArgumentException("empty effect evidence");
            selfStageDeltas = sorted(selfStageDeltas);
            for (var e : selfStageDeltas.entrySet())
                if (!STATS.contains(e.getKey()) || e.getValue() < -12 || e.getValue() > 12)
                    throw new IllegalArgumentException("invalid stage delta");
            if ((targetDamage.known() || !selfStageDeltas.isEmpty() || targetStatusAfter.known()
                    || targetHazardsAfter != null || completeOwnEffect) && (source == null || source.isBlank()))
                throw new IllegalArgumentException("conditional effect requires verified evidence");
            if (targetDamage.known() && kind != Kind.ATTACK && kind != Kind.FORCED_CONTINUATION)
                throw new IllegalArgumentException("damage on unsupported action kind");
            if (kind == Kind.SWITCH && (!selfStageDeltas.isEmpty() || targetStatusAfter.known() || targetHazardsAfter != null))
                throw new IllegalArgumentException("unsupported switch patch; supply own reserve/entry evidence");
        }
        public Effect(Kind kind, Known<PublicStateSearchAdapter.Interval> damage, Map<String, Integer> deltas,
                      Known<String> status, Hazards hazards, String source) {
            this(kind, damage, deltas, status, hazards, false, source);
        }
        public static Effect unknown(Kind kind) {
            return new Effect(kind, Known.unknown(), Map.of(), Known.unknown(), null, false, null);
        }
    }

    /** A separate, actor-bound conditional next request. Root requests are never reused. */
    public record FollowUp(String selfId, List<String> legalMoves, List<String> legalSwitches,
                           boolean forcedAction, boolean forcedReplacement, boolean trapped,
                           Map<String, Effect> effects, String source) {
        public FollowUp {
            if (selfId == null || selfId.isBlank() || source == null || source.isBlank())
                throw new IllegalArgumentException("continuation requires actor and evidence");
            legalMoves = names(legalMoves); legalSwitches = names(legalSwitches); effects = sorted(effects);
            checkForced(forcedAction, forcedReplacement, forcedReplacement ? List.of() : legalMoves);
            for (String id : effects.keySet())
                if (!legalMoves.stream().anyMatch(m -> id.equals("move:" + m))
                        && !legalSwitches.stream().anyMatch(s -> id.equals("switch:" + s)))
                    throw new IllegalArgumentException("effect for an unavailable continuation");
        }
    }

    public record Evidence(Context context, Map<String, Effect> effects, Map<String, FollowUp> followUps) {
        public Evidence { Objects.requireNonNull(context); effects = sorted(effects); followUps = sorted(followUps); }
        public static Evidence unknown() { return new Evidence(Context.unknown(), Map.of(), Map.of()); }
    }

    static void checkForced(boolean forced, boolean replacement, List<String> moves) {
        if (forced && replacement) throw new IllegalArgumentException("conflicting forced states");
        if (forced && moves.size() != 1) throw new IllegalArgumentException("forced action requires exactly one legal move");
    }
    static void checkHp(Known<PublicStateSearchAdapter.Interval> hp) {
        if (hp.known() && hp.value().high() > 1) throw new IllegalArgumentException("HP interval outside [0,1]");
    }
    static Map<String, Integer> stages(Map<String, Integer> input) {
        for (var e : input.entrySet())
            if (!STATS.contains(e.getKey()) || e.getValue() < -6 || e.getValue() > 6)
                throw new IllegalArgumentException("invalid known stage");
        return sorted(input);
    }
    static void checkStatus(Known<String> status) {
        if (status.known() && !Set.of("none", "brn", "par", "psn", "tox", "slp", "frz").contains(status.value()))
            throw new IllegalArgumentException("unsupported known major status");
    }
    static <T> Map<String, T> sorted(Map<String, T> input) {
        input.forEach((key, value) -> { if (key == null || key.isBlank() || value == null)
            throw new IllegalArgumentException("invalid evidence map entry"); });
        return Collections.unmodifiableMap(new TreeMap<>(input));
    }
    static Set<String> sortedSet(Set<String> input) { return Collections.unmodifiableSet(new TreeSet<>(input)); }
    static List<String> names(List<String> input) {
        if (input.stream().anyMatch(s -> s == null || s.isBlank()) || new HashSet<>(input).size() != input.size())
            throw new IllegalArgumentException("invalid or duplicate action name");
        return input.stream().sorted().toList();
    }
}
