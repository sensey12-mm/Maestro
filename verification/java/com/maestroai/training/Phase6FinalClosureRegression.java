package com.maestroai.training;

import com.maestroai.ai.LivePromotionGate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 6.35 closure audit, not a new planner or live-policy implementation.
 * Exercises integrated Phase 4/5/6/7 authority boundaries and guards the
 * registered regression/documentation surface against accidental erosion.
 */
final class Phase6FinalClosureRegression {
    private Phase6FinalClosureRegression(){}
    private static int checks;
    private static void check(boolean ok,String why) {
        if(!ok)throw new AssertionError("Phase 6.35 closure: "+why);
        checks++;
    }
    private static void rejected(Runnable action,String why){
        try{action.run();}
        catch(IllegalArgumentException expected){check(true,why);return;}
        throw new AssertionError("Phase 6.35 accepted unauthorized "+why);
    }
    static void run() throws Exception {
        // Even when a local user sets the historical flag, the *actual*
        // installed chooser passes no promoted supplier or approval.
        String original=System.getProperty(LivePromotionGate.PROPERTY);
        try {
            var legacyCalls=new AtomicInteger();
            var promotedCalls=new AtomicInteger();
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            String legacy=LivePromotionGate.choose(
                    ()->{legacyCalls.incrementAndGet();return "legacy";},
                    ()->{promotedCalls.incrementAndGet();return "shadow";},
                    x->true,false);
            check("legacy".equals(legacy)
                    && legacyCalls.get()==1 && promotedCalls.get()==0,
                    "flag alone never authorizes a proposed Shadow action");
            check("legacy".equals(LivePromotionGate.choose(
                    ()->"legacy",null,null,false)),
                    "production null candidate always selects Legacy");
        }finally{
            if(original==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,original);
        }

        var phase5=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(phase5.purpose()
                    ==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && phase5.pin().mechanicsScope().equals(CobblemonMechanics.SOURCE),
                "Phase 5 signed consumer remains offline with Phase 4 source pin");
        rejected(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,
                PublicCompetitiveConsumerContract.ContractPin.pinned(),false,true),
                "Phase 5 tactical consumption");
        rejected(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.LIVE_CONTROL,
                PublicCompetitiveConsumerContract.ContractPin.pinned(),true,true),
                "Phase 5 online consumption");
        rejected(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.SHADOW_TACTICAL_USE,
                ShadowLiveCompetitiveConsumerSafety.ContractPin.pinned(),
                false,false,true),
                "Phase 6.32 live tactical authority");
        check(!ShadowLiveCompetitiveConsumerSafety.Request.preparationPinned()
                    .tacticalDecisionRequested(),
                "newer competitive consumer has no decision grant");

        // Phase 7's best possible locally constructed stage set still
        // represents an offline readiness report, never installed integration.
        var stages=new EnumMap<Phase7Readiness.Stage,Phase7Readiness.Gate>(
                Phase7Readiness.Stage.class);
        for(var stage:Phase7Readiness.Stage.values())
            stages.put(stage,Phase7Readiness.Gate.PASS);
        var synthetic=new Phase7Readiness.Report(
                Phase7Readiness.SCHEMA,
                Phase7Readiness.Status.AWAITING_INTEGRATED_PHASES_4_5_6,
                Phase7Readiness.EvidenceTier.EXTERNALLY_REVIEWED_OFFLINE,
                "a".repeat(64),"b".repeat(64),null,null,null,null,null,
                100,100,100,stages);
        check(!synthetic.integrated() &&
                    synthetic.json().contains("\"integrated\":false")
                    && synthetic.json().contains("minecraft-acceptance"),
                "even all-PASS Phase 7 synthetic stages cannot attest live integration");

        // Production topology is more important than a positive synthetic
        // evidence report: no Phase 6.28-6.34 inspector is in live choose().
        String ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int chooser=ai.indexOf("public ShowdownActionResponse choose(");
        int legacy=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        check(chooser>=0 && legacy>chooser
                    && ai.substring(chooser,legacy).contains(
                        "LivePromotionGate.choose(")
                    && ai.substring(chooser,legacy).contains("null, null, false);"),
                "actual live chooser remains Legacy-only, without an injected Shadow policy");
        String search=Files.readString(Path.of(
                "src/main/java/com/maestroai/training/ShadowSearchEngine.java"));
        check(search.contains("NOT a battle/damage simulator")
                    && !ai.substring(chooser,legacy).contains("ShadowSearchEngine.search("),
                "Phase 6's conditional search is not a verified live decision policy");

        String runner=Files.readString(Path.of(
                "verification/java/com/maestroai/training/StandaloneRegression.java"));
        String[] required={
                "OfflineShadowTacticalAdapterRegression",
                "OfflineShadowResponseScenariosRegression",
                "OfflineShadowProvenanceGateRegression",
                "NativeEvidenceEndToEndGateRegression",
                "RuntimeObservationAcceptanceHarnessRegression",
                "NativeOwnActionFusionRegression",
                "NativeSidedDispatchReachabilityRegression",
                "Integration4567Regression",
                "ShadowCompetitiveRuntimeBridgeRegression",
                "PublicOpponentIdentityContinuityRegression",
                "ShadowOpponentIdentityIntegrationRegression",
                "NativeOpponentSideProvenanceRegression",
                "ShadowLiveCompetitiveConsumerSafetyRegression",
                "NativeOpponentPokemonContinuityRegression",
                "NativeOpponentRevealRecipientEvidenceRegression",
                "PublicCompetitiveConsumerContractRegression",
                "Phase7SelfPlayRegression",
                "Phase6FinalClosureRegression"
        };
        for(String test:required)
            check(runner.contains(test+".run();"),
                    "all key offline/native/competitive/integration suites remain registered: "+test);
        for(int phase=1;phase<=34;phase++){
            String prefix="PHASE6"+phase+"_";
            try(var files=Files.list(Path.of("verification"))){
                check(files.anyMatch(p->p.getFileName().toString()
                            .startsWith(prefix)
                        && p.getFileName().toString().endsWith(".md")),
                        "Phase 6."+phase+" retains a public source milestone note");
            }
        }
        check(Files.exists(Path.of(
                    "src/main/java/com/maestroai/training/NativeOpponentRevealRecipientEvidence.java"))
                && Files.exists(Path.of(
                    "src/main/java/com/maestroai/training/ShadowMechanicsEvaluation.java"))
                && Files.exists(Path.of(
                    "src/main/java/com/maestroai/training/OfflineShadowResponseScenarios.java")),
                "native evidence cannot silently replace original tactical and mechanics foundation");

        System.out.println("Phase 6.35 final source boundary audit PASS: "+checks
                +" checks; live Shadow/online Phase 5 NOT AUTHORIZED;"
                +" Minecraft installed acceptance NOT TESTED");
    }
}
