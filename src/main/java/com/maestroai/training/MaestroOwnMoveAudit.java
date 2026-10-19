package com.maestroai.training;

import java.util.*;

/** Public-source move effect caveats, never an opponent set or own legal move
 * provider. The trainer roster is kept ONLY in verification sources, not in
 * the shipped server-side mod / future publication artifact.
 */
public final class MaestroOwnMoveAudit {
    private MaestroOwnMoveAudit() {}
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
    public static boolean completelySimulated(String moveId) {
        var move=CobblemonMechanics.move(moveId);
        return move!=null && (move.damageOnly() || move.power()==0 && !UNMODELED_EFFECTS.containsKey(moveId));
    }
}
