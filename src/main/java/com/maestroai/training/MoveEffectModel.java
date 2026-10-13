package com.maestroai.training;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Conservative, data-driven shadow annotations for public moves.
 *
 * Base stats (power, accuracy, type, category and priority) come from the
 * caller's Cobblemon MoveTemplate. These annotations describe only effects
 * that have been explicitly verified here, not the full Showdown script.
 * Unknown effects remain unknown; they are never scored as "no effect".
 */
public final class MoveEffectModel {
    private MoveEffectModel() {}

    public enum Effect {
        BOOST_SELF, LOWER_TARGET, FORCE_SWITCH, PIVOT, HEAL_SELF,
        SET_HAZARD, REMOVE_HAZARD, LOCK_TARGET_MOVE, STEAL_BOOSTS,
        PROTECT, SECONDARY_STATUS, SECONDARY_STAT_DROP, SECONDARY_FLINCH, UNKNOWN
    }

    public record MoveFacts(String id, String type, String category,
                            double basePower, double accuracy, int priority,
                            Effect effect, String effectDetail) {
        public MoveFacts {
            if (id == null || id.isBlank() || type == null || category == null
                    || !Double.isFinite(basePower) || basePower < 0
                    || !Double.isFinite(accuracy) || accuracy < 0) {
                throw new IllegalArgumentException("invalid move metadata");
            }
            if (effect == null || effectDetail == null) {
                throw new IllegalArgumentException("missing effect metadata");
            }
        }
    }

    private record Annotation(Effect effect, String detail) {}

    private static final Map<String, Annotation> VERIFIED = Map.ofEntries(
            Map.entry("icebeam", new Annotation(Effect.SECONDARY_STATUS, "10% freeze on successful hit; immunity and modifiers apply")),
            Map.entry("flamethrower", new Annotation(Effect.SECONDARY_STATUS, "10% burn on successful hit; immunity and modifiers apply")),
            Map.entry("fireblast", new Annotation(Effect.SECONDARY_STATUS, "10% burn on successful hit; immunity and modifiers apply")),
            Map.entry("thunder", new Annotation(Effect.SECONDARY_STATUS, "30% paralysis on successful hit; immunity and modifiers apply")),
            Map.entry("moonblast", new Annotation(Effect.SECONDARY_STAT_DROP, "30% target special attack -1 on successful hit; modifiers apply")),
            Map.entry("ironhead", new Annotation(Effect.SECONDARY_FLINCH, "30% flinch if target has not acted; modifiers apply")),
            Map.entry("focusblast", new Annotation(Effect.SECONDARY_STAT_DROP, "10% target special defense -1 on successful hit; modifiers apply")),
            Map.entry("swordsdance", new Annotation(Effect.BOOST_SELF, "atk+2")),
            Map.entry("nastyplot", new Annotation(Effect.BOOST_SELF, "spa+2")),
            Map.entry("geomancy", new Annotation(Effect.BOOST_SELF, "two-turn:spa+2,spd+2,spe+2;Power Herb may skip charge")),
            Map.entry("dragondance", new Annotation(Effect.BOOST_SELF, "atk+1,spe+1")),
            Map.entry("calmmind", new Annotation(Effect.BOOST_SELF, "spa+1,spd+1")),
            Map.entry("bulkup", new Annotation(Effect.BOOST_SELF, "atk+1,def+1")),
            Map.entry("uturn", new Annotation(Effect.PIVOT, "switch user after hit")),
            Map.entry("voltswitch", new Annotation(Effect.PIVOT, "switch user after hit if target not immune")),
            Map.entry("flipturn", new Annotation(Effect.PIVOT, "switch user after hit")),
            Map.entry("stealthrock", new Annotation(Effect.SET_HAZARD, "rock entry hazard")),
            Map.entry("spikes", new Annotation(Effect.SET_HAZARD, "spikes entry hazard")),
            Map.entry("rapidspin", new Annotation(Effect.REMOVE_HAZARD, "removes user's side hazards on successful hit")),
            Map.entry("defog", new Annotation(Effect.REMOVE_HAZARD, "clears hazards and screens under applicable conditions")),
            Map.entry("encore", new Annotation(Effect.LOCK_TARGET_MOVE, "temporarily locks target into its last move")),
            Map.entry("spectralthief", new Annotation(Effect.STEAL_BOOSTS, "steals positive stat stages before damage")),
            Map.entry("recover", new Annotation(Effect.HEAL_SELF, "restores user HP")),
            Map.entry("roost", new Annotation(Effect.HEAL_SELF, "restores user HP; temporary flying-type interaction")),
            Map.entry("protect", new Annotation(Effect.PROTECT, "blocks many attacks; consecutive use may fail"))
    );

    public static MoveFacts describe(String id, String type, String category,
                                     double power, double accuracy, int priority) {
        String key = canonical(id);
        Annotation a = VERIFIED.getOrDefault(key,
                new Annotation(Effect.UNKNOWN, "secondary effects not modeled"));
        return new MoveFacts(key, type, category, power, accuracy, priority,
                a.effect(), a.detail());
    }

    public static boolean knownEffect(String id) {
        return VERIFIED.containsKey(canonical(id));
    }

    private static String canonical(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
