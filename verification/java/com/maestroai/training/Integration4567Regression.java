package com.maestroai.training;

import com.maestroai.ai.LivePromotionGate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Independent Phase 4/5/6/7 cross-phase acceptance checks for Maestro4567.
 * All fixtures are synthetic; no Minecraft hook or opponent receipt is certified.
 */
final class Integration4567Regression {
    private Integration4567Regression() {}
    private static int checks;

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError("Maestro4567: " + reason);
        checks++;
    }

    private static void reject(Runnable action, String reason) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Maestro4567 accepted " + reason);
    }

    static void run() throws Exception {
        // Phase 5's genuine reviewed consumer still cannot become a live or
        // current-battle tactical authority merely because Phase 6 is present.
        var offline = PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(offline.purpose() == PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                "Phase 5 stays an explicitly offline source");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,
                offline.pin(), false, false), "Phase 5 tactical purpose");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                offline.pin(), true, false), "Phase 5 current battle binding");

        // A valid public synthetic Phase 6 fixture passes Phase 4-conditional
        // analysis, but no conditional evidence becomes a recommendation.
        var snapshot = OfflineShadowResponseScenariosRegression.snapshot(
                1, List.of("tackle"), List.of("scratch"), List.of());
        var frames = OfflineShadowResponseScenariosRegression.frames("Scratch");
        var input = OfflineShadowResponseScenariosRegression.input(frames, snapshot);
        var review = OfflineShadowTacticalAdapter.inspectWithProvenance(
                OfflineShadowProvenanceGateRegression.request(snapshot), input.fixture());
        check(review.fixture().offlineFixtureMatched()
                        && !review.conditional().currentBattleBound()
                        && !review.conditional().shadowSearchAuthorized()
                        && !review.conditional().calibratedOpponentProbabilities()
                        && !review.conditional().livePromotionAuthorized(),
                "Phase 4/5/6 integrated fixture remains observational");
        var constructed = OfflineShadowResponseScenarios.construct(input);
        check(constructed.conditionalCases().size() == 1
                        && !constructed.opponentResponseSetComplete(),
                "publicly witnessed reply is not a complete opponent action set");
        var audit = OfflineShadowCoverageAudit.inspect(
                new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                        input, Map.of(
                                "self", CobblemonMechanics.Profile.unknown("self"),
                                "opponent", CobblemonMechanics.Profile.unknown("opponent")),
                        PublicTransitionState.Known.of(false, "synthetic fixture"),
                        CobblemonMechanics.Critical.NON_CRITICAL));
        check(audit.modeledPairCount() == 1
                        && !audit.opponentResponseSetComplete()
                        && !audit.calibratedResponseProbabilities()
                        && !audit.shadowSearchAuthorized()
                        && !audit.livePromotionAuthorized(),
                "Phase 6 cannot invent opponent probabilities from Phase 5");

        // A caller-rehashed forged public nickname must still fail source
        // actor custody before Phase 6 scenario generation.
        var forged = new ArrayList<>(frames);
        forged.set(3, "update\n|move|p2a: PRIVATE_SECRET|Scratch|p1a: Own");
        var forgedInput = OfflineShadowResponseScenariosRegression.input(forged, snapshot);
        reject(() -> OfflineShadowResponseScenarios.construct(forgedInput),
                "forged opponent display identity");

        // Phase 6.27 native method return is one source epoch, not delivery,
        // not packet identity and never a promoted Shadow decision.
        Object battle = new Object(), p1 = new Object(), p2 = new Object();
        var nativeSession = new NativeSidedDispatchReachability.Session();
        check(nativeSession.status(battle, 0, 1, true, false).verdict()
                        == NativeSidedDispatchReachability.Verdict.NO_NATIVE_SIDED_CALLBACK_YET,
                "missing native hook remains UNKNOWN");
        var returned = nativeSession.nativeReturn(battle, List.of(p1, p2), p1,
                new Object(), new Object(), false, 0, 1, true, false);
        check(returned.current()
                        && !returned.perRecipientSendAcknowledged()
                        && !returned.originalShowdownSplitLinked()
                        && !returned.shadowSearchAuthorized()
                        && !returned.livePromotionAuthorized(),
                "actual method return has no delivery or tactical authority");
        var stale = nativeSession.status(battle, 1, 2, true, false);
        check(stale.verdict()
                        == NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_PRIOR_EPOCH
                        && !stale.current() && !returned.current()
                        && !stale.clientDeliveryAuthenticated(),
                "source progression revokes stale native method-return evidence");
        check(new NativeEvidenceEndToEndGate.Session().inspectDirect(new Object(), null)
                        .verdict() == NativeEvidenceEndToEndGate.Verdict.MISSING_DIRECT_EVIDENCE,
                "empty end-to-end witness cannot create success");

        // Even hypothetical externally reviewed positive offline Phase 7
        // stages do not certify genuine joint Phase 4/5/6 readiness.
        var stages = new EnumMap<Phase7Readiness.Stage, Phase7Readiness.Gate>(
                Phase7Readiness.Stage.class);
        for (var stage : Phase7Readiness.Stage.values())
            stages.put(stage, Phase7Readiness.Gate.PASS);
        var readiness = new Phase7Readiness.Report(
                Phase7Readiness.SCHEMA,
                Phase7Readiness.Status.AWAITING_INTEGRATED_PHASES_4_5_6,
                Phase7Readiness.EvidenceTier.EXTERNALLY_REVIEWED_OFFLINE,
                "0".repeat(64), "1".repeat(64), null, null, null, null, null,
                100, 100, 100, stages);
        check(!readiness.integrated()
                        && readiness.json().contains("no-live-shadow-promotion"),
                "Phase 7 cannot launder synthetic success into runtime acceptance");
        check(new LegacyChoiceCapture(false, "UNKNOWN")
                        .observe(null, null, null, null).status()
                        == LegacyChoiceCapture.Status.DISABLED,
                "Phase 7 Legacy capture defaults disabled");

        // Source-gate plus compiled Legacy selector: an opt-in property
        // cannot promote a candidate without explicit independent approval.
        var original = System.getProperty(LivePromotionGate.PROPERTY);
        try {
            System.setProperty(LivePromotionGate.PROPERTY, "true");
            var calls = new AtomicInteger();
            var chosen = LivePromotionGate.choose(
                    () -> { calls.incrementAndGet(); return "legacy"; },
                    () -> "shadow", candidate -> true, false);
            check("legacy".equals(chosen) && calls.get() == 1,
                    "Legacy still exclusively selects actions even with opt-in flag");
        } finally {
            if (original == null) System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY, original);
        }
        var ai = Files.readString(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("LivePromotionGate.choose(")
                        && ai.contains("null, null, false);")
                        && !ai.contains("OfflineShadowTacticalAdapter.inspect(")
                        && !ai.contains("OfflineShadowResponseScenarios.construct(")
                        && !ai.contains("PublicShadowBuildHandoff.prepare("),
                "production chooser never executes offline Phase 5/6 tactical consumers");
        System.out.println("Maestro4567 independent cross-phase safety PASS: "
                + checks + " checks (synthetic/source only; Minecraft unverified)");
    }
}
