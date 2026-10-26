package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/**
 * Phase 6.3: OFFLINE witness-conditioned response scenarios, not a response
 * predictor, complete opponent set, source-authenticated battle, or live policy.
 * Phase 5 graded historical possibilities never become scenario replies.
 */
public final class OfflineShadowResponseScenarios {
    private OfflineShadowResponseScenarios() {}
    public static final String SCHEMA = "phase6.3.offline-response-scenarios.v1";
    public enum Abstention {
        OFFLINE_UNAUTHENTICATED_FIXTURE, NO_CURRENT_ACTOR_OR_LEGALITY_RECEIPT,
        OPEN_OPPONENT_RESPONSE_SET, NO_RESPONSE_PROBABILITIES,
        UNVERIFIED_INSTALLED_MECHANICS, WITNESSED_MOVE_NOT_CURRENT_LEGALITY,
        NO_PHASE5_HISTORICAL_EVIDENCE, HISTORICAL_APPEARANCE_NOT_CURRENT_IDENTITY,
        PHASE5_GUESSES_NOT_REPLIES, NO_WITNESSED_REPLIES,
        UNSUPPORTED_PUBLIC_REPLY, UNSUPPORTED_OWN_MOVE,
        UNCERTIFIED_OWN_SWITCH_ENTRY, REPLY_BUDGET_EXCEEDED,
        OWN_ACTION_BUDGET_EXCEEDED, NO_SOURCE_SUPPORTED_CASES
    }
    public record HistoricalScope(PublicShadowBuildHandoff.Report report,
                                  int archivedAppearance) {
        public HistoricalScope {
            if (report == null || archivedAppearance < 1
                    || !PublicShadowBuildHandoff.SCHEMA.equals(report.schema())
                    || !PublicSpeciesBuildLegality.SOURCE_REVISION.equals(report.sourceRevision())
                    || !CobblemonMechanics.SOURCE.equals(report.mechanicsScope())
                    || !PublicBuildCandidateRobustness.frozenPolicySha256().equals(report.policySha256())
                    || !report.originalReplaySignedCustodyRechecked()
                    || report.currentBattleEvidenceBound() || report.legalActionsAvailable()
                    || report.calibratedOpponentProbabilities() || report.speciesIdentityProven()
                    || report.completeLegalSetProven() || report.phase6SearchAuthorized()
                    || report.shadowTacticalReady() || report.livePromotionAuthorized()
                    || report.independentlyApprovedReplayRights()
                    || report.appearances().stream().filter(a -> a.index() == archivedAppearance).count() != 1)
                throw invalid();
        }
    }
    public record Input(String schema,
                        PublicCompetitiveConsumerContract.ConsumerRequest offlinePurpose,
                        PublicShadowBuildHandoff.Pin phase5Pin,
                        String phase4Source,
                        OfflineShadowProvenanceGate.Input fixture,
                        BattleSnapshot snapshot,
                        PublicStateSearchAdapter.PublicMechanics mechanics,
                        HistoricalScope historical) {
        public Input {
            if (!SCHEMA.equals(schema)
                    || !PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned().equals(offlinePurpose)
                    || !PublicShadowBuildHandoff.Pin.pinned().equals(phase5Pin)
                    || !CobblemonMechanics.SOURCE.equals(phase4Source)
                    || fixture == null || snapshot == null || mechanics == null)
                throw invalid();
        }
    }
    /** A witnessed event, NOT a confirmed currently executable opponent reply. */
    public record ConditionalCase(String ownAction, String witnessedOpponentMove) {
        public ConditionalCase {
            if (ownAction == null || !ownAction.matches("move:[a-z0-9]{1,48}")
                    || witnessedOpponentMove == null
                    || !witnessedOpponentMove.matches("[a-z0-9]{1,48}")
                    || CobblemonMechanics.move(ownAction.substring(5)) == null
                    || CobblemonMechanics.move(witnessedOpponentMove) == null)
                throw invalid();
        }
    }
    /** Counts ONLY from a separately selected archived appearance. */
    public record HistoricalUncertainty(int archivedAppearance,
                                        Map<PublicShadowBuildHandoff.Grade,Integer> moveGrades,
                                        int proposedCandidates,
                                        boolean nativeBuildIdentityProven) {
        public HistoricalUncertainty {
            if (archivedAppearance < 1 || proposedCandidates < 0
                    || nativeBuildIdentityProven || moveGrades == null
                    || moveGrades.values().stream().anyMatch(v -> v == null || v < 0))
                throw invalid();
            var copy = new EnumMap<PublicShadowBuildHandoff.Grade,Integer>(PublicShadowBuildHandoff.Grade.class);
            copy.putAll(moveGrades);
            moveGrades = Collections.unmodifiableMap(copy);
        }
        @Override public String toString() {
            return "OfflineHistoricalGrades[appearance=" + archivedAppearance
                    + ";signals=" + moveGrades.values().stream().mapToInt(Integer::intValue).sum()
                    + ";no-current-identity]";
        }
    }
    public static final class Report {
        private final OfflineShadowProvenanceGate.Receipt fixture;
        private final List<ConditionalCase> cases;
        private final List<ShadowMechanicsEvaluation.Plan> plans;
        private final Set<Abstention> abstentions;
        private final HistoricalUncertainty historical;
        private final int observedReplyCount;
        private Report(OfflineShadowProvenanceGate.Receipt fixture, List<ConditionalCase> cases,
                       List<ShadowMechanicsEvaluation.Plan> plans, Set<Abstention> reasons,
                       HistoricalUncertainty historical, int count) {
            this.fixture = fixture;
            this.cases = List.copyOf(cases);
            this.plans = List.copyOf(plans);
            this.abstentions = Collections.unmodifiableSet(EnumSet.copyOf(reasons));
            this.historical = historical;
            this.observedReplyCount = count;
        }
        public String schema() { return SCHEMA; }
        public String mechanicsScope() { return CobblemonMechanics.SOURCE; }
        public OfflineShadowProvenanceGate.Receipt fixture() { return fixture; }
        public List<ConditionalCase> conditionalCases() { return cases; }
        public List<ShadowMechanicsEvaluation.Plan> conditionalPlans() { return plans; }
        public Set<Abstention> abstentions() { return abstentions; }
        public HistoricalUncertainty historical() { return historical; }
        public int witnessedReplyCount() { return observedReplyCount; }
        public boolean opponentResponseSetComplete() { return false; }
        public boolean opponentNextMoveLegalOrKnown() { return false; }
        public boolean calibratedProbabilities() { return false; }
        public boolean archivedAppearanceBoundToFixture() { return false; }
        public boolean installedLegalityVerified() { return false; }
        public boolean currentBattleBound() { return false; }
        public boolean shadowSearchAuthorized() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "Phase63OfflineScenarios[cases=" + cases.size()
                    + ";observedReplies=" + observedReplyCount
                    + ";abstentions=" + abstentions + ";promotion=false]";
        }
    }
    public static Report construct(Input input) {
        Objects.requireNonNull(input);
        // Accept only original pinned spectator events and exactly the declared
        // own fixture action set. No caller-minted Phase 5 or mechanics result.
        var receipt = OfflineShadowProvenanceGate.check(input.fixture(), input.snapshot(), List.of());
        var root = PublicStateSearchAdapter.project(input.snapshot(), input.mechanics()).states().get("root");
        TreeSet<String> actions = new TreeSet<>();
        root.legalMoves().forEach(s -> actions.add("move:" + s));
        root.legalSwitches().forEach(s -> actions.add("switch:" + s));
        if (!actions.equals(new TreeSet<>(input.fixture().ownLegalActions())))
            throw invalid(); // forced/trapped or mismatched conditional legality

        var reasons = EnumSet.of(
                Abstention.OFFLINE_UNAUTHENTICATED_FIXTURE,
                Abstention.NO_CURRENT_ACTOR_OR_LEGALITY_RECEIPT,
                Abstention.OPEN_OPPONENT_RESPONSE_SET,
                Abstention.NO_RESPONSE_PROBABILITIES,
                Abstention.UNVERIFIED_INSTALLED_MECHANICS,
                Abstention.WITNESSED_MOVE_NOT_CURRENT_LEGALITY);
        HistoricalUncertainty historic = null;
        if (input.historical() == null) {
            reasons.add(Abstention.NO_PHASE5_HISTORICAL_EVIDENCE);
        } else {
            // This archived appearance is not joined to the fixture actor, even
            // if nickname, displayed form and ordinal happen to coincide.
            var selection = input.historical();
            var appearance = selection.report().appearances().stream()
                    .filter(a -> a.index() == selection.archivedAppearance()).findFirst().orElseThrow();
            var grades = new EnumMap<PublicShadowBuildHandoff.Grade,Integer>(PublicShadowBuildHandoff.Grade.class);
            for (var signal : appearance.signals()) {
                if (signal.kind() == PublicOpponentBuildHypotheses.Kind.MOVE)
                    grades.merge(signal.grade(), 1, Integer::sum);
            }
            int candidates = (int) selection.report().candidates().stream()
                    .filter(c -> c.appearanceIndex() == selection.archivedAppearance()).count();
            historic = new HistoricalUncertainty(selection.archivedAppearance(), grades, candidates, false);
            reasons.add(Abstention.HISTORICAL_APPEARANCE_NOT_CURRENT_IDENTITY);
            reasons.add(Abstention.PHASE5_GUESSES_NOT_REPLIES);
        }

        var observed = new TreeSet<>(input.snapshot().opponent().revealedMoves());
        if (observed.isEmpty()) reasons.add(Abstention.NO_WITNESSED_REPLIES);
        var supportedReplies = new TreeSet<String>();
        for (var reply : observed) {
            if (CobblemonMechanics.move(reply) == null) reasons.add(Abstention.UNSUPPORTED_PUBLIC_REPLY);
            else supportedReplies.add(reply);
        }
        var supportedOwn = new TreeSet<String>();
        for (var action : actions) {
            if (action.startsWith("switch:")) reasons.add(Abstention.UNCERTIFIED_OWN_SWITCH_ENTRY);
            else if (CobblemonMechanics.move(action.substring(5)) == null)
                reasons.add(Abstention.UNSUPPORTED_OWN_MOVE);
            else supportedOwn.add(action);
        }

        boolean bounded = actions.size() <= 24 && observed.size() <= PublicReplyCoverage.MAX_PUBLIC_REPLIES
                && (long) actions.size() * observed.size() <= 256;
        if (actions.size() > 24 || (long) actions.size() * observed.size() > 256)
            reasons.add(Abstention.OWN_ACTION_BUDGET_EXCEEDED);
        if (observed.size() > PublicReplyCoverage.MAX_PUBLIC_REPLIES)
            reasons.add(Abstention.REPLY_BUDGET_EXCEEDED);
        List<ConditionalCase> cases = new ArrayList<>();
        List<ShadowMechanicsEvaluation.Plan> plans = new ArrayList<>();
        if (bounded) {
            for (String action : supportedOwn) {
                var scenarios = new ArrayList<List<ShadowMechanicsEvaluation.Turn>>();
                for (String reply : supportedReplies) {
                    cases.add(new ConditionalCase(action, reply));
                    scenarios.add(List.of(new ShadowMechanicsEvaluation.Turn(action,reply,null)));
                }
                if (!scenarios.isEmpty())
                    plans.add(new ShadowMechanicsEvaluation.Plan(action,scenarios,
                            "phase6.3.offline-source-conditional-witnessed-only"));
            }
        }
        if (plans.isEmpty()) reasons.add(Abstention.NO_SOURCE_SUPPORTED_CASES);
        return new Report(receipt,cases,plans,reasons,historic,observed.size());
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 6.3 rejects unpinned or mismatched offline evidence");
    }
}
