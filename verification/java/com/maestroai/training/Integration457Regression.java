package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 * Integrated Phase 4/5/7 boundary tests. Runs beside, not instead of, each
 * phase's own adversarial suite. All fixtures are synthetic and offline.
 */
final class Integration457Regression {
    private Integration457Regression() {}
    private static int checks;
    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError("Maestro457 integration: " + why);
        checks++;
    }
    private static void reject(Runnable action, String why) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Maestro457 integration: " + why);
    }
    static void run() throws Exception {
        // Source-pinned offline hypotheses cannot be promoted by their caller.
        var pin = PublicShadowBuildHandoff.Pin.pinned();
        check(pin.version() == 1
                && pin.schema().equals("phase5.21.shadow-build-evidence.v1"),
                "Phase 5.21 schema/pin preserved");
        check(pin.consumer().equals(PublicCompetitiveConsumerContract.ContractPin.pinned()),
                "Phase 5.19 evidence gate retained");
        var offline = PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(offline.purpose() == PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !offline.tacticalUseRequested() && !offline.currentBattleBindingRequested(),
                "offline-only default must deny battle binding and tactics");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,
                offline.pin(), false, false), "Phase 5 Shadow tactical consumer rejected");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.LIVE_CONTROL,
                offline.pin(), false, false), "Phase 5 live consumer rejected");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                offline.pin(), true, false), "Phase 5 current battle binding rejected");
        reject(() -> new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                offline.pin(), false, true), "Phase 5 tactical-use request rejected");
        check(Arrays.stream(PublicShadowBuildHandoff.Report.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("phase6SearchAuthorized")
                        && m.getReturnType() == boolean.class),
                "explicit Phase 6 prohibition preserved");
        check(Arrays.stream(PublicShadowBuildHandoff.Report.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("calibratedOpponentProbabilities")
                        && m.getReturnType() == boolean.class),
                "unverified probabilities remain explicit");

        // Both same-choice Z/form mechanics and Phase 7's salted receipt must
        // coexist on ONE transport rather than replacing either API.
        check(Arrays.stream(OwnRequestTransport.Pokemon.class.getRecordComponents())
                .anyMatch(c -> c.getName().equals("species")),
                "Phase 4 species/form ownership retained");
        check(Arrays.stream(OwnRequestTransport.Request.class.getRecordComponents())
                .anyMatch(c -> c.getName().equals("selection")),
                "Phase 4 own Z choice retained");
        check(OwnRequestTransport.class.getMethod("choiceProof",
                OwnRequestTransport.Lease.class, UUID.class)
                .getReturnType() == OwnRequestTransport.ChoiceProof.class,
                "Phase 7 choice proof merged");
        check(OwnRequestTransport.class.getMethod("rebindOwnReentry",
                OwnRequestTransport.Lease.class, RuntimeDomain.class,
                PublicProtocolEvidence.class, PublicMechanicsEvidence.Observation.class,
                PublicFieldEvidence.Capture.class) != null,
                "Phase 4 same-form reentry API retained");
        check(new OwnRequestTransport().choiceProof(null, UUID.randomUUID()) == null,
                "no Phase 7 receipt without current own lease");

        var disabled = new LegacyChoiceCapture(false, "UNKNOWN");
        check(disabled.observe(null, null, null, null).status()
                == LegacyChoiceCapture.Status.DISABLED,
                "Legacy capture opt-in defaults closed");
        var enabled = new LegacyChoiceCapture(true, "0".repeat(64));
        check(enabled.observe(UUID.randomUUID(), null, java.util.List.of("|turn|1"),
                "move:tackle").status() == LegacyChoiceCapture.Status.EXPIRED_OWN_CHOICE,
                "no capture without authenticated owning choice");
        check(Phase7Readiness.Status.AWAITING_INTEGRATED_PHASES_4_5_6 != null,
                "Phase 7's unmet integration state remains expressible");

        // Inspect the actual compiled branch's runtime entrypoint to guard
        // against dropping Phase 4 selection or inadvertently wiring Phase 5
        // into live decisions during manual source reconciliation.
        var ai = Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("LivePromotionGate.choose(")
                && ai.contains("null, null, false);"),
                "Legacy still solely selects live actions");
        check(ai.contains("ownZChoice(ShowdownMoveset moveset)")
                && ai.contains("withOwnZMove(ownZ)"),
                "Phase 4 own Z mechanic still observable");
        check(ai.contains("observeReturnedLegacyChoice(activePokemon, battle, moveset, forceSwitch, response)")
                && ai.contains("legacyChoiceCapture.enabled()"),
                "Phase 7 optional capture present, never selection input");
        check(!ai.contains("PublicShadowBuildHandoff.prepare(")
                && !ai.contains("PublicCompetitiveConsumerContract.prepare("),
                "Phase 5 offline exports absent from live chooser");
        var transport = Files.readString(Path.of(
                "src/main/java/com/maestroai/training/OwnRequestTransport.java"));
        check(transport.contains("ChoiceProof choiceProof(")
                && transport.contains("OwnZMoveEvidence ownZMove(")
                && transport.contains("bindOwnXerneas("),
                "Phase 4 and 7 provenance entrypoints preserved");
        System.out.println("Maestro457 integration safety regressions PASS: "
                + checks + " checks; synthetic/source-only, no Minecraft acceptance");
    }
}
