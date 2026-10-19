package com.maestroai.training;

import java.util.*;

/** Own-team test coverage, NOT a hidden opponent set or a substitute for live
 * own-request moves. Names describe the owner-supplied fixed Maestro trainer;
 * every tactical action is still taken from the actual legal request.
 */
public final class MaestroOwnMoveAudit {
    private MaestroOwnMoveAudit() {}
    public static final Map<String, List<String>> FIXED_ROSTER = Map.of(
            "Genesect", List.of("uturn", "ironhead", "icebeam", "flamethrower")),
            "Calyrex-Shadow", List.of("astralbarrage", "psyshock", "drainingkiss", "energyball")),
            "Xerneas", List.of("geomancy", "moonblast", "focusblast", "thunder")),
            "Arceus", List.of("swordsdance", "extremespeed", "shadowclaw", "recover")),
            "Mewtwo-Mega-Y", List.of("psystrike", "icebeam", "fireblast", "nastyplot")),
            "Marshadow", List.of("spectralthief", "closecombat", "shadowsneak", "rocktomb"))
    );
    private static final Map<String,String> UNMODELED_EFFECTS = Map.ofEntries(
            Map.entry("uturn", "FORCED_PIVOT_AND_REPLACEMENT_UNKNOWN"),
            Map.entry("ironhead", "SECONDARY_FLINCH_BRANCH_UNKNOWN"),
            Map.entry("icebeam", "SECONDARY_FREEZE_BRANCH_UNKNOWN"),
            Map.entry("flamethrower", "SECONDARY_BURN_BRANCH_UNKNOWN"),
            Map.entry("drainingkiss", "POST_DAMAGE_DRAIN_HEAL_UNKNOWN"),
            Map.entry("energyball", "SECONDARY_SPECIAL_DEFENSE_DROP_UNKNOWN"),
            Map.entry("geomancy", "GEOMANCY_CHARGE_POWER_HERB_AND_CONSUMPTION_UNMODELED"),
            Map.entry("moonblast", "SECONDARY_SPECIAL_ATTACK_DROP_UNKNOWN"),
            Map.entry("focusblast", "SECONDARY_SPECIAL_DEFENSE_DROP_UNKNOWN"),
            Map.entry("thunder", "WEATHER_ACCURACY_AND_PARALYSIS_UNKNOWN"),
            Map.entry("fireblast", "SECONDARY_BURN_BRANCH_UNKNOWN"),
            Map.entry("spectralthief", "PRE_HIT_TARGET_BOOST_STEAL_UNKNOWN"),
            Map.entry("closecombat", "SELF_DEFENSE_DROPS_UNKNOWN"),
            Map.entry("rocktomb", "ON_HIT_TARGET_SPEED_DROP_UNKNOWN")
    );
    public static String unmodeledEffect(String moveId) {
        return UNMODELED_EFFECTS.getOrDefault(moveId, "SECONDARY_OR_OTHER_MOVE_EFFECTS_UNRESOLVED");
    }
    public static Set<String> ownMoveIds() {
        var result=new TreeSet<String>();
        FIXED_ROSTER.values().forEach(result::addAll);
        return Set.copyOf(result);
    }
    public static boolean completelySimulated(String moveId) {
        var move=CobblemonMechanics.move(moveId);
        return move!=null && (move.damageOnly() || move.power()==0 && !UNMODELED_EFFECTS.containsKey(moveId));
    }
}
