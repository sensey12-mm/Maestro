package com.maestroai.training;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Optional, bounded, observational developer decision receipt. Not a choice
 * provider or an evidence attestation. No UUIDs, actor names, opponent state,
 * packet/request bodies, item values or raw file paths may enter the output.
 */
public final class DeveloperBattleDecisionCapture {
    public static final String PROPERTY = "maestroai.developerDiagnostics";
    private static final Pattern ACTION =
            Pattern.compile("(?:move:[a-z0-9_-]{1,64}|switch:slot[1-6])");
    private static final Pattern REASON = Pattern.compile("[A-Z_]{2,80}");
    // Source-reviewed vocabulary, not a regex that can launder private strings.
    private static final Set<String> MOVE_IDS = Set.of(
            "uturn","ironhead","icebeam","flamethrower","astralbarrage",
            "psyshock","drainingkiss","energyball","geomancy","moonblast",
            "focusblast","thunder","swordsdance","extremespeed","shadowclaw",
            "recover","psystrike","fireblast","nastyplot","spectralthief",
            "closecombat","shadowsneak","rocktomb","swift","psychocut",
            "tackle","transform","protect","substitute","toxic",
            "thunderbolt","thunderwave","surf","earthquake","quickattack",
            "psychic","willowisp","encore","splash","hydropump","darkpulse",
            "knockoff","stealthrock","spikes","toxicspikes","defog",
            "rapidspin","roost","suckerpunch","pursuit","voltswitch",
            "taunt","rest","sleeptalk","dragonclaw","shadowball",
            "trickroom","calmmind","leechseed","seedbomb");
    private DeveloperBattleDecisionCapture() {}

    public static boolean enabled() { return Boolean.getBoolean(PROPERTY); }

    public static String safeAction(String value) {
        if (value == null || !ACTION.matcher(value).matches()) return "UNKNOWN";
        if (value.startsWith("move:") && !MOVE_IDS.contains(value.substring(5)))
            return "UNKNOWN";
        return value;
    }

    public static String safeReason(String value) {
        return value != null && REASON.matcher(value).matches() ? value : "UNKNOWN";
    }

    public static String safeLegal(Collection<String> choices) {
        if (choices == null || choices.size() > 24) return "UNKNOWN";
        List<String> accepted = new ArrayList<>();
        for (String choice : choices) {
            if (!safeAction(choice).equals(choice) || accepted.contains(choice))
                return "UNKNOWN";
            accepted.add(choice);
        }
        return String.join(",", accepted);
    }

    /** A single selected response, after native promotion gate execution. */
    public static String safeLine(long capture, int turn, boolean forced,
                                  String authority, String selected, String proposal,
                                  String reason, String fallbackStage,
                                  String fallbackReason, Collection<String> legal) {
        if (capture <= 0 || turn < 0 || turn > 10000
                || !(authority.equals("SHADOW") || authority.equals("LEGACY")))
            return "[MaestroAI] DEV_DIAG_REJECTED code=INVALID_SOURCE";
        String available = safeLegal(legal);
        return "[MaestroAI] DEV_DIAG_DECISION "
                + "capture=" + capture
                + ";turn=" + turn
                + ";forced=" + forced
                + ";authority=" + authority
                + ";action=" + safeAction(selected)
                + ";proposal=" + safeAction(proposal)
                + ";reason=" + safeReason(reason)
                + ";fallback_stage=" + safeReason(fallbackStage)
                + ";fallback_reason=" + safeReason(fallbackReason)
                + ";legal=" + available
                + ";confidence=UNKNOWN;custody=REVIEW_REQUIRED";
    }

    public static void record(long capture, int turn, boolean forced,
                              String authority, String selected, String proposal,
                              String reason, String fallbackStage,
                              String fallbackReason, Collection<String> legal) {
        if (!enabled()) return;
        System.out.println(safeLine(capture, turn, forced, authority, selected,
                proposal, reason, fallbackStage, fallbackReason, legal));
    }
}
