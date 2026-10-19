package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;

/**
 * Shadow-only exhaustive root/action and witnessed-public-reply comparison.
 * No response probabilities, private requests, future requests, or legacy policy.
 * A source-executed *conditional* comparison is not a prediction of what a foe will do.
 */
public final class PublicReplyCoverage {
    private PublicReplyCoverage() {}
    public static final int MAX_PUBLIC_REPLIES = 8;
    private static final Set<String> REASONS = Set.of(
            "PUBLIC_HISTORY_NOT_ATTESTED", "PUBLIC_REPLY_IDENTITY_AMBIGUOUS",
            "NO_CURRENT_PUBLIC_REPLIES", "REPLY_BUDGET_EXCEEDED",
            "NO_LEGAL_CANDIDATES", "FORCED_ACTION_UNSUPPORTED",
            "NO_CURRENT_FIELD_CERTIFICATE", "UNSUPPORTED_COMPARISON",
            "PUBLIC_TURN_MISMATCH", "LEGAL_ROOT_UNVERIFIED", "CANDIDATE_COVERAGE_UNVERIFIED",
            "OUTCOME_PREREQUISITES_UNKNOWN", "INTERVALS_OVERLAP_OR_TIE");
    public record Audit(int candidateCount, int replyCount, boolean allCandidates,
                        boolean compared, boolean conditionalPreference,
                        Set<String> reasons, ShadowMechanicsEvaluation.Comparison result) {
        public Audit {
            reasons = Set.copyOf(reasons);
            if (!REASONS.containsAll(reasons) || candidateCount < 0 || replyCount < 0)
                throw new IllegalArgumentException("nonpublic coverage diagnostics");
        }
        /** Fixed vocabulary/counters only; never output any actor/reply payload. */
        public String safeLine() {
            return "Phase334 rootCandidates=" + candidateCount + " witnessedReplies=" + replyCount
                    + " complete=" + allCandidates + " compared=" + compared
                    + " conditionalPreferred=" + conditionalPreference
                    + " unresolved=" + new TreeSet<>(reasons)
                    + " responseProbabilities=NONE advisoryOnly=true livePromotion=false";
        }
    }

    private static Audit withheld(int candidates, int replies, String why) {
        return new Audit(candidates,replies,false,false,false,Set.of(why),null);
    }
    private static String moveId(String displayed) {
        return displayed == null ? "" : displayed.replaceAll("[^a-zA-Z0-9]","").toLowerCase(Locale.ROOT);
    }

    /** Only currently active, unambiguously observed spectator "move" events count.
     * A switch resets the current appearance; replace/Transform/re-entry cannot
     * transfer moves to a different identity. An absent event does not prove
     * that an opponent cannot choose any other move.
     */
    public static List<String> currentReplies(PublicProtocolEvidence publicHistory, String slot) {
        if (publicHistory == null || !publicHistory.complete() || !publicHistory.framed()
                || publicHistory.corrupted() || !publicHistory.coverageUnknown().isEmpty()
                || publicHistory.reentryAmbiguous(slot)) return List.of();
        var seen = new TreeSet<String>();
        boolean current = false, ambiguous = false;
        for (String line : publicHistory.mechanicsEvents()) {
            String[] p = line.split("\\|", -1);
            if (p.length < 3) continue;
            String event = p[1];
            String eventSlot = p[2].trim().split(":",2)[0];
            if (!eventSlot.equals(slot)) continue;
            if (event.equals("switch") || event.equals("drag")) {
                seen.clear(); current = true; ambiguous = false;
            } else if (event.equals("faint")) {
                // A witnessed faint ends this active appearance. Its formerly
                // revealed moves cannot be presented as presently executable
                // replies while a replacement is still pending.
                seen.clear(); current = false; ambiguous = false;
            } else if (event.equals("replace") || event.equals("-transform") || event.equals("-formechange")
                    || event.equals("detailschange") || event.equals("-mega")) {
                seen.clear(); ambiguous = true;
            } else if (event.equals("move") && current && !ambiguous && p.length > 3) {
                String move = moveId(p[3]);
                if (!move.isEmpty()) seen.add(move);
            }
        }
        return current && !ambiguous ? List.copyOf(seen) : List.of();
    }

    public static Audit evaluate(BattleSnapshot snapshot, PublicMechanics mechanics,
                                 Map<String, Profile> profiles, ScopedExecution execution,
                                 PublicProtocolEvidence publicHistory, String opponentSlot,
                                 Known<Boolean> trickRoom, Critical critical) {
        Objects.requireNonNull(snapshot); Objects.requireNonNull(mechanics);
        Objects.requireNonNull(profiles); Objects.requireNonNull(trickRoom);
        // Projection checks the authoritative own request, trap and forced rules.
        // Never compensate for missing legal actions using a guessed bench/move.
        Projection projected;
        try { projected = project(snapshot,mechanics); }
        catch (IllegalArgumentException invalid) { return withheld(0,0,"LEGAL_ROOT_UNVERIFIED"); }
        State root = projected.states().get("root");
        int candidates = root.legalMoves().size()+root.legalSwitches().size();
        if (candidates == 0) return withheld(0,0,"NO_LEGAL_CANDIDATES");
        if (root.forcedReplacement() || root.forcedAction())
            return withheld(candidates,0,"FORCED_ACTION_UNSUPPORTED");
        if (publicHistory == null || !publicHistory.complete() || !publicHistory.framed()
                || publicHistory.corrupted() || !publicHistory.coverageUnknown().isEmpty())
            return withheld(candidates,0,"PUBLIC_HISTORY_NOT_ATTESTED");
        if (publicHistory.reentryAmbiguous(opponentSlot))
            return withheld(candidates,0,"PUBLIC_REPLY_IDENTITY_AMBIGUOUS");
        if (publicHistory.publicTurn() != snapshot.turn())
            return withheld(candidates,0,"PUBLIC_TURN_MISMATCH");
        List<String> publicReplies = currentReplies(publicHistory,opponentSlot);
        if (publicReplies.isEmpty()) return withheld(candidates,0,"NO_CURRENT_PUBLIC_REPLIES");
        if (publicReplies.size()>MAX_PUBLIC_REPLIES)
            return withheld(candidates,publicReplies.size(),"REPLY_BUDGET_EXCEEDED");
        if (execution == null) return withheld(candidates,publicReplies.size(),"NO_CURRENT_FIELD_CERTIFICATE");
        // Both the public spectator witness and the current Shadow snapshot
        // must agree. A raw opponent-private move cannot be added here.
        List<String> replies = publicReplies.stream().filter(snapshot.opponent().revealedMoves()::contains).toList();
        if (!replies.equals(publicReplies))
            return withheld(candidates,publicReplies.size(),"PUBLIC_REPLY_IDENTITY_AMBIGUOUS");
        List<String> actions = new ArrayList<>();
        root.legalMoves().forEach(m->actions.add("move:"+m));
        root.legalSwitches().forEach(s->actions.add("switch:"+s));
        Collections.sort(actions);
        List<ShadowMechanicsEvaluation.Plan> plans = actions.stream().map(action ->
                new ShadowMechanicsEvaluation.Plan(action,publicReplies.stream().map(reply ->
                        List.of(new ShadowMechanicsEvaluation.Turn(action,reply,null))).toList(),
                        "actual-current-public-reply;conditional-not-distribution")).toList();
        try {
            var comparison = ShadowMechanicsEvaluation.compare(snapshot,mechanics,profiles,plans,
                    trickRoom,critical,null,execution);
            boolean allBounded=comparison.rankings().stream().allMatch(rank->rank.utility().known());
            Set<String> blockers=!comparison.candidateCoverageComplete()
                    ? Set.of("CANDIDATE_COVERAGE_UNVERIFIED")
                    : allBounded
                        ? comparison.preferredConditional()==null ? Set.of("INTERVALS_OVERLAP_OR_TIE") : Set.of()
                        : Set.of("OUTCOME_PREREQUISITES_UNKNOWN");
            return new Audit(candidates,publicReplies.size(),comparison.candidateCoverageComplete(),true,
                    comparison.preferredConditional()!=null,blockers,comparison);
        } catch (RuntimeException rejected) {
            return withheld(candidates,publicReplies.size(),"UNSUPPORTED_COMPARISON");
        }
    }
}
