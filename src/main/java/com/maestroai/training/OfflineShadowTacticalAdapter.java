package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/**
 * Phase 6.1: offline, conditional Shadow input review. NOT a battle-state binder,
 * probability estimator, search policy, action recommender or live adapter.
 *
 * Phase 5 signed historical appearances are intentionally never mapped onto
 * a current actor. A caller-supplied snapshot is only an offline scenario and
 * cannot attest native legality, hidden opponent knowledge or runtime freshness.
 */
public final class OfflineShadowTacticalAdapter {
    private OfflineShadowTacticalAdapter() {}
    public static final String SCHEMA = "phase6.1.offline-shadow-input.v1";

    public enum Abstention {
        OFFLINE_SCENARIO_NOT_A_CURRENT_BATTLE,
        NO_CURRENT_ACTOR_TURN_OR_LEGAL_ACTION_RECEIPT,
        NO_CALIBRATED_OPPONENT_RESPONSE_PROBABILITIES,
        INSTALLED_MECHANICS_NOT_ATTESTED,
        PHASE5_SIGNED_HANDOFF_NOT_SUPPLIED,
        PHASE5_APPEARANCE_NOT_CURRENT_IDENTITY,
        AMBIGUOUS_OPPONENT_IDENTITY,
        INCOMPLETE_LEGAL_ACTION_COVERAGE,
        PHASE4_CONDITIONAL_EFFECTS_UNRESOLVED
    }

    /**
     * The Phase 5 report is optional and only adds historical uncertainty;
     * no signal, proposed build, appearance or ordinal enters the mechanics
     * evaluator. In particular, a Phase 5 report cannot validate legal moves,
     * furnish opponent responses or supply a probability distribution.
     */
    public record Request(
            String schema,
            PublicCompetitiveConsumerContract.ConsumerRequest phase5Purpose,
            PublicShadowBuildHandoff.Pin phase5Pin,
            String phase4MechanicsScope,
            BattleSnapshot snapshot,
            PublicStateSearchAdapter.PublicMechanics mechanics,
            Map<String, CobblemonMechanics.Profile> profiles,
            List<ShadowMechanicsEvaluation.Plan> plans,
            Known<Boolean> trickRoom,
            CobblemonMechanics.Critical critical,
            PublicShadowBuildHandoff.Report historicalPhase5,
            boolean currentBattleBindingRequested,
            boolean tacticalOrLiveUseRequested,
            boolean appearanceAsStableIdentityRequested) {
        public Request {
            if (!SCHEMA.equals(schema)
                    || !PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned().equals(phase5Purpose)
                    || !PublicShadowBuildHandoff.Pin.pinned().equals(phase5Pin)
                    || !CobblemonMechanics.SOURCE.equals(phase4MechanicsScope)
                    || snapshot == null || mechanics == null || profiles == null
                    || plans == null || plans.isEmpty() || plans.size() > 256
                    || trickRoom == null || critical == null
                    || currentBattleBindingRequested || tacticalOrLiveUseRequested
                    || appearanceAsStableIdentityRequested)
                throw new IllegalArgumentException("Phase 6.1 accepts only pinned offline review");
            profiles = Map.copyOf(profiles);
            plans = List.copyOf(plans);
            // BattleSnapshot itself has no public-event provenance certificate.
            // Avoid silently transporting opponent reserves, secret slot data or
            // claims of an opponent item/ability through this offline boundary.
            if (!snapshot.opponentBench().isEmpty()
                    || snapshot.opponent().revealedItem() != null
                    || snapshot.opponent().revealedAbility() != null
                    || snapshot.opponentTerastallized()
                    || snapshot.knownOpponentMoveCounts().entrySet().stream()
                        .anyMatch(e -> !snapshot.opponent().revealedMoves().contains(e.getKey())
                                || e.getValue() == null || e.getValue() < 0))
                throw new IllegalArgumentException("unbound opponent information");
            if (historicalPhase5 != null
                    && (!PublicShadowBuildHandoff.SCHEMA.equals(historicalPhase5.schema())
                        || !PublicSpeciesBuildLegality.SOURCE_REVISION.equals(historicalPhase5.sourceRevision())
                        || !CobblemonMechanics.SOURCE.equals(historicalPhase5.mechanicsScope())
                        || !PublicBuildCandidateRobustness.frozenPolicySha256().equals(historicalPhase5.policySha256())
                        || !historicalPhase5.originalReplaySignedCustodyRechecked()
                        || historicalPhase5.currentBattleEvidenceBound()
                        || historicalPhase5.legalActionsAvailable()
                        || historicalPhase5.calibratedOpponentProbabilities()
                        || historicalPhase5.speciesIdentityProven()
                        || historicalPhase5.completeLegalSetProven()
                        || historicalPhase5.phase6SearchAuthorized()
                        || historicalPhase5.shadowTacticalReady()
                        || historicalPhase5.livePromotionAuthorized()))
                throw new IllegalArgumentException("Phase 5 historical evidence exceeds offline authority");
        }
    }

    /** Numerical values are source-conditional HP-resource intervals, NOT win
     * probabilities or action scores. Null bounds explicitly mean UNKNOWN. */
    public record ActionEvidence(String ownLegalAction, boolean sourceListedMove,
                                 Double conditionalLow, Double conditionalHigh,
                                 Set<String> unresolved) {
        public ActionEvidence {
            Objects.requireNonNull(ownLegalAction);
            unresolved = Set.copyOf(unresolved);
            if ((conditionalLow == null) != (conditionalHigh == null)
                    || conditionalLow != null && (!Double.isFinite(conditionalLow)
                        || !Double.isFinite(conditionalHigh)
                        || conditionalLow > conditionalHigh))
                throw new IllegalArgumentException("invalid conditional bounds");
        }
    }

    /** Only this adapter can produce a review. No ready/recommend/approval flag
     * exists; the same result always remains unusable for live or Shadow control. */
    public static final class Review {
        private final List<ActionEvidence> actions;
        private final Set<Abstention> abstentions;
        private Review(List<ActionEvidence> actions, Set<Abstention> abstentions) {
            this.actions = List.copyOf(actions);
            this.abstentions = Collections.unmodifiableSet(EnumSet.copyOf(abstentions));
        }
        public String schema() { return SCHEMA; }
        public String mechanicsScope() { return CobblemonMechanics.SOURCE; }
        public List<ActionEvidence> actions() { return actions; }
        public Set<Abstention> abstentions() { return abstentions; }
        public boolean currentBattleBound() { return false; }
        public boolean legalActionsRuntimeAttested() { return false; }
        public boolean calibratedOpponentProbabilities() { return false; }
        public boolean opponentIdentityProven() { return false; }
        public boolean shadowSearchAuthorized() { return false; }
        public boolean tacticalRecommendationAvailable() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "Phase61OfflineReview[actions=" + actions.size()
                    + ";abstentions=" + abstentions + ";not-tactical]";
        }
    }

    public static Review inspect(Request request) {
        Objects.requireNonNull(request, "request");
        // Project first: this verifies forced moves, trapped switches, root
        // conditional effects and the actual fixture's claimed legal choices.
        var projection = PublicStateSearchAdapter.project(request.snapshot(), request.mechanics());
        var root = projection.states().get("root");
        Set<String> legal = new TreeSet<>();
        root.legalMoves().forEach(m -> legal.add("move:" + m));
        root.legalSwitches().forEach(s -> legal.add("switch:" + s));
        Set<String> selected = new TreeSet<>();
        for (var plan : request.plans()) {
            String action = plan.action();
            if (action == null || !legal.contains(action) || !selected.add(action))
                throw new IllegalArgumentException("duplicate or illegal offline action");
            String kind = action.startsWith("move:") ? "move" : "switch";
            request.snapshot().validateDecision(new BattleSnapshot.Decision(
                    kind, action.substring(kind.length() + 1), 0, Map.of(), "offline synthetic"));
            for (var line : plan.scenarios()) {
                for (var turn : line) {
                    // Phase 5 proposed moves, regardless of grade, are NEVER
                    // accepted as replies. Only the explicitly revealed set
                    // of this synthetic snapshot may be evaluated.
                    if (!request.snapshot().opponent().revealedMoves().contains(turn.reply()))
                        throw new IllegalArgumentException("unrevealed opponent reply");
                }
            }
        }

        EnumSet<Abstention> reasons = EnumSet.of(
                Abstention.OFFLINE_SCENARIO_NOT_A_CURRENT_BATTLE,
                Abstention.NO_CURRENT_ACTOR_TURN_OR_LEGAL_ACTION_RECEIPT,
                Abstention.NO_CALIBRATED_OPPONENT_RESPONSE_PROBABILITIES,
                Abstention.INSTALLED_MECHANICS_NOT_ATTESTED);
        if (request.historicalPhase5() == null)
            reasons.add(Abstention.PHASE5_SIGNED_HANDOFF_NOT_SUPPLIED);
        else
            reasons.add(Abstention.PHASE5_APPEARANCE_NOT_CURRENT_IDENTITY);
        if (!root.ambiguity().isEmpty())
            reasons.add(Abstention.AMBIGUOUS_OPPONENT_IDENTITY);
        if (!selected.equals(legal))
            reasons.add(Abstention.INCOMPLETE_LEGAL_ACTION_COVERAGE);

        // Compute from Phase 4 source logic, NOT caller-minted Comparison,
        // Phase 5 candidate verdicts, probability guesses, or the old
        // ShadowSearchEngine's weighted outcomes.
        var computed = ShadowMechanicsEvaluation.compare(
                request.snapshot(), request.mechanics(), request.profiles(),
                request.plans(), request.trickRoom(), request.critical(), null);
        List<ActionEvidence> actions = new ArrayList<>();
        for (var ranking : computed.rankings()) {
            var known = ranking.utility().known() && ranking.unknown().isEmpty();
            if (!known) reasons.add(Abstention.PHASE4_CONDITIONAL_EFFECTS_UNRESOLVED);
            String id = ranking.action();
            boolean isMove = id.startsWith("move:");
            boolean sourceListed = isMove && CobblemonMechanics.move(id.substring(5)) != null;
            if (isMove && !sourceListed)
                reasons.add(Abstention.PHASE4_CONDITIONAL_EFFECTS_UNRESOLVED);
            actions.add(new ActionEvidence(id, sourceListed,
                    known ? ranking.utility().value().low() : null,
                    known ? ranking.utility().value().high() : null,
                    ranking.unknown()));
        }
        actions.sort(Comparator.comparing(ActionEvidence::ownLegalAction));
        return new Review(actions, reasons);
    }
}
