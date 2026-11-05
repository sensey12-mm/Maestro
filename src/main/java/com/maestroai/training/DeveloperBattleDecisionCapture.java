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

    public enum Source {
        PUBLIC_OPPONENT, OWN_ONLY, FORCED_NATIVE, LEGACY_FALLBACK, UNKNOWN
    }
    public enum Witness {
        OBSERVED, VERIFIED_OWN_REQUEST, UNVERIFIED, MISSING, UNKNOWN
    }
    public record Estimate(String action, String category, double score) {
        public Estimate {
            action=safeAction(action);
            category=safeReason(category);
            if(!Double.isFinite(score) || Math.abs(score)>1_000_000)
                throw new IllegalArgumentException("invalid diagnostic heuristic score");
            score=Math.rint(score*100)/100.0;
        }
    }
    /** A pure evidence packet from the already chosen action; no hidden sets. */
    public record Evidence(Source source, ShadowPromotionPreparation.Verdict preparation,
                           int candidateCount, List<Estimate> ranked,
                           List<String> rootAlternatives,
                           Witness ownRequest, Witness nativeLegality,
                           Witness publicOpponent) {
        public Evidence {
            if(source==null || candidateCount < -1 || candidateCount > 24
                    || ranked==null || ranked.size()>24
                    || rootAlternatives==null || rootAlternatives.size()>24
                    || ownRequest==null || nativeLegality==null || publicOpponent==null)
                throw new IllegalArgumentException("invalid diagnostic evidence");
            ranked=List.copyOf(ranked);
            List<String> safe=new ArrayList<>();
            for(String root:rootAlternatives) {
                if(!safeAction(root).equals(root) || safe.contains(root))
                    throw new IllegalArgumentException("unreviewed alternative root");
                safe.add(root);
            }
            rootAlternatives=List.copyOf(safe);
        }
        public static Evidence unavailable() {
            return new Evidence(Source.UNKNOWN,null,-1,List.of(),List.of(),
                    Witness.UNKNOWN,Witness.UNKNOWN,Witness.UNKNOWN);
        }
    }

    /** Copy existing heuristic scores once; never recalculate or alter policy.
     * UUID-based switch candidates are redacted instead of exported. */
    public static List<Estimate> safeEstimates(ShadowLiveDecisionEngine.Choice choice) {
        if(choice==null) return List.of();
        List<Estimate> values=new ArrayList<>();
        for(var entry:choice.evaluations()) {
            if(values.size()>=24)break;
            String action=safeAction(entry.action());
            values.add(new Estimate(action,entry.reason().name(),entry.heuristicScore()));
        }
        return List.copyOf(values);
    }

    public static boolean enabled() { return Boolean.getBoolean(PROPERTY); }

    /** Console logging is optional; installed local reports are not. */
    public static boolean captureEnabled() {
        return enabled() || AutomaticBattleReports.enabled();
    }

    public static String safeAction(String value) {
        if (value == null || !ACTION.matcher(value).matches()) return "UNKNOWN";
        if (value.startsWith("move:") && !MOVE_IDS.contains(value.substring(5))) {
            // Read-only installed Cobblemon move registry: public move IDs and
            // own legal choices are safe even outside the short test roster.
            // Standalone synthetic regressions may not have native registries.
            try {
                if (com.cobblemon.mod.common.api.moves.Moves.getByName(
                        value.substring(5)) == null) return "UNKNOWN";
            } catch (RuntimeException | LinkageError unavailable) {
                return "UNKNOWN";
            }
        }
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
        // -1 means the owning-native turn lease was unavailable. Preserve
        // the actually returned action with an explicitly unknown turn.
        if (capture <= 0 || turn < -1 || turn > 10000
                || !(authority.equals("SHADOW") || authority.equals("LEGACY")))
            return "[MaestroAI] DEV_DIAG_REJECTED code=INVALID_SOURCE";
        String available = safeLegal(legal);
        return "[MaestroAI] DEV_DIAG_DECISION "
                + "capture=" + capture
                + ";turn=" + (turn == -1 ? "UNKNOWN" : Integer.toString(turn))
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
        record(capture,turn,forced,authority,selected,proposal,reason,
                fallbackStage,fallbackReason,legal,Evidence.unavailable());
    }
    public static void record(long capture, int turn, boolean forced,
                              String authority, String selected, String proposal,
                              String reason, String fallbackStage,
                              String fallbackReason, Collection<String> legal,
                              Evidence evidence) {
        String line = safeLine(capture, turn, forced, authority, selected,
                proposal, reason, fallbackStage, fallbackReason, legal);
        AutomaticBattleReports.recordDecision(capture, line, evidence);
        if (enabled()) System.out.println(line);
    }
}
