package com.maestroai.training;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, simulator-independent battle position.
 *
 * This is an observation, not omniscient battle state: opponent moves/items
 * must be included only if revealed. The training adapter and live Cobblemon
 * adapter will both populate this structure.
 */
public record BattleSnapshot(
        int turn,
        Combatant self,
        Combatant opponent,
        List<Combatant> selfBench,
        List<Combatant> opponentBench,
        List<String> legalMoves,
        List<String> legalSwitches,
        Map<String, Integer> knownOpponentMoveCounts,
        boolean opponentTerastallized,
        String opponentTeraType
) {
    /** Sentinel for opponent HP not measured from an allowlisted public event. */
    public static final double UNKNOWN_HP_FRACTION = -1.0;

    public BattleSnapshot {
        if (turn < 0) throw new IllegalArgumentException("turn must be nonnegative");
        Objects.requireNonNull(self, "self");
        Objects.requireNonNull(opponent, "opponent");
        selfBench = List.copyOf(selfBench);
        opponentBench = List.copyOf(opponentBench);
        legalMoves = List.copyOf(legalMoves);
        legalSwitches = List.copyOf(legalSwitches);
        knownOpponentMoveCounts = Map.copyOf(knownOpponentMoveCounts);
        if (opponentTerastallized && (opponentTeraType == null || opponentTeraType.isBlank())) {
            throw new IllegalArgumentException("active Tera requires a known type");
        }
        if (!opponentTerastallized) opponentTeraType = null;
    }

    public record Combatant(
            String species,
            double hpFraction,
            List<String> types,
            Map<String, Integer> knownBoosts,
            List<String> revealedMoves,
            String revealedItem,
            String revealedAbility,
            boolean fainted
    ) {
        public Combatant {
            Objects.requireNonNull(species, "species");
            if (hpFraction != UNKNOWN_HP_FRACTION
                    && (hpFraction < 0 || hpFraction > 1 || !Double.isFinite(hpFraction))) {
                throw new IllegalArgumentException("HP fraction must be in [0,1] or explicitly UNKNOWN");
            }
            types = List.copyOf(types);
            knownBoosts = Map.copyOf(knownBoosts);
            revealedMoves = List.copyOf(revealedMoves);
            if (fainted && hpFraction != 0) {
                throw new IllegalArgumentException("fainted combatant requires known zero HP");
            }
        }

        public boolean hpKnown() { return hpFraction != UNKNOWN_HP_FRACTION; }
    }

    public record Decision(
            String actionKind,
            String actionName,
            double predictedValue,
            Map<String, Double> opponentResponseProbabilities,
            String explanation
    ) {
        public Decision {
            if (!"move".equals(actionKind) && !"switch".equals(actionKind)) {
                throw new IllegalArgumentException("actionKind must be move or switch");
            }
            Objects.requireNonNull(actionName, "actionName");
            if (!Double.isFinite(predictedValue)) {
                throw new IllegalArgumentException("predictedValue must be finite");
            }
            opponentResponseProbabilities = Map.copyOf(opponentResponseProbabilities);
            double total = 0;
            for (double probability : opponentResponseProbabilities.values()) {
                if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
                    throw new IllegalArgumentException("invalid probability");
                }
                total += probability;
            }
            if (!opponentResponseProbabilities.isEmpty() && Math.abs(total - 1) > 0.001) {
                throw new IllegalArgumentException("opponent probabilities must sum to 1");
            }
            Objects.requireNonNull(explanation, "explanation");
        }
    }

    public void validateDecision(Decision decision) {
        Objects.requireNonNull(decision, "decision");
        if ("move".equals(decision.actionKind()) && !legalMoves.contains(decision.actionName())) {
            throw new IllegalArgumentException("illegal move: " + decision.actionName());
        }
        if ("switch".equals(decision.actionKind()) && !legalSwitches.contains(decision.actionName())) {
            throw new IllegalArgumentException("illegal switch: " + decision.actionName());
        }
    }
}
