package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Independent Phase 6.6 adversarial integration review of 6.1–6.5. */
final class Phase66OfflineIntegrationRegression {
    private Phase66OfflineIntegrationRegression() {}
    private static int checks;
    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError("Phase 6.6: "+message);
        checks++;
    }
    private static void reject(Runnable r,String message){
        try{r.run();}catch(IllegalArgumentException expected){
            check(expected.getMessage()!=null
                    && !expected.getMessage().contains("PRIVATE_SECRET"),
                    message+" redacted rejection");
            return;
        }
        throw new AssertionError("Phase 6.6 accepted "+message);
    }
    private static BattleSnapshot snapshot(){
        return OfflineShadowResponseScenariosRegression.snapshot(
                1,List.of("tackle"),List.of("scratch"),List.of());
    }
    private static OfflineShadowResponseScenarios.Input fixture(List<String> frames){
        return OfflineShadowResponseScenariosRegression.input(frames,snapshot());
    }
    private static OfflineShadowCoverageAudit.Input auditInput(
            OfflineShadowResponseScenarios.Input input){
        return new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                input,Map.of("self",CobblemonMechanics.Profile.unknown("self"),
                        "opponent",CobblemonMechanics.Profile.unknown("opponent")),
                Known.of(false,"explicit synthetic condition"),
                CobblemonMechanics.Critical.NON_CRITICAL);
    }
    private static OfflineShadowProvenanceGate.Input evidence(List<String> events) {
        return fixture(events).fixture();
    }
    private static void rejectEveryLayer(List<String> invalid,String reason){
        var typed=fixture(invalid);
        reject(()->OfflineShadowProvenanceGate.check(
                typed.fixture(),typed.snapshot(),List.of()),
                "Phase 6.2 "+reason);
        reject(()->OfflineShadowTacticalAdapter.inspectWithProvenance(
                OfflineShadowProvenanceGateRegression.request(typed.snapshot()),
                typed.fixture()),"Phase 6.1/6.2 "+reason);
        reject(()->OfflineShadowResponseScenarios.construct(typed),
                "Phase 6.3 "+reason);
        reject(()->OfflineShadowCoverageAudit.inspect(auditInput(typed)),
                "Phase 6.4/6.5 "+reason);
    }
    static void run(){
        var frames=OfflineShadowResponseScenariosRegression.frames("Scratch");
        var base=fixture(frames);
        var accepted=OfflineShadowTacticalAdapter.inspectWithProvenance(
                OfflineShadowProvenanceGateRegression.request(snapshot()),base.fixture());
        check(accepted.fixture().offlineFixtureMatched()
                && !accepted.conditional().currentBattleBound()
                && !accepted.conditional().shadowSearchAuthorized()
                && !accepted.conditional().livePromotionAuthorized(),
                "6.1 and 6.2 valid fixture remains strictly offline");
        var situations=OfflineShadowResponseScenarios.construct(base);
        var audit=OfflineShadowCoverageAudit.inspect(auditInput(base));
        check(situations.conditionalCases().size()==1
                && audit.modeledPairCount()==1 && audit.fixturePairCount()==1
                && !situations.opponentResponseSetComplete()
                && !audit.opponentResponseSetComplete()
                && !audit.recommendedActionAvailable(),
                "6.3–6.5 preserve incomplete responses and no recommendation");
        check(audit.conditionallyResolvedPairCount()<=1
                && audit.unresolvedFixturePairCount()==1-audit.conditionallyResolvedPairCount()
                && !audit.calibratedResponseProbabilities()
                && !audit.shadowSearchAuthorized()
                && !audit.livePromotionAuthorized(),
                "6.5 interval counts cannot be promoted");
        // Valid, publicly witnessed own-side move is allowed and does not
        // affect the current appearance's opponent reply set.
        var legitimate=new ArrayList<>(frames);
        legitimate.add("update\n|move|p1a: Own|Tackle|p2a: Opp");
        var good=fixture(legitimate);
        check(OfflineShadowProvenanceGate.check(good.fixture(),snapshot(),List.of())
                .observedReplyCount()==1
                && OfflineShadowResponseScenarios.construct(good).conditionalCases().size()==1,
                "strict actor binding still accepts a real active own actor");
        // Independently recompute SHA-256 over the *forged* messages. A matching
        // caller-supplied digest must not bypass actor/appearance checks.
        var forgedOpponent=new ArrayList<>(frames);
        forgedOpponent.set(3,"update\n|move|p2a: PRIVATE_SECRET|Scratch|p1a: Own");
        rejectEveryLayer(forgedOpponent,"opponent same-slot actor swap");
        var forgedOwn=new ArrayList<>(legitimate);
        forgedOwn.set(4,"update\n|move|p1a: PRIVATE_SECRET|Tackle|p2a: Opp");
        rejectEveryLayer(forgedOwn,"own same-slot actor swap");
        var forgedTarget=new ArrayList<>(frames);
        forgedTarget.set(3,"update\n|move|p2a: Opp|Scratch|p1a: PRIVATE_SECRET");
        rejectEveryLayer(forgedTarget,"target nickname not active in side slot");
        var foreignTarget=new ArrayList<>(frames);
        foreignTarget.set(3,"update\n|move|p2a: Opp|Scratch|p3a: PRIVATE_SECRET");
        rejectEveryLayer(foreignTarget,"target on an unmodeled third side");
        var metadata=new ArrayList<>(frames);
        metadata.set(3,"update\n|move|p2a: Opp|Scratch|p1a: Own|[from] SECRET");
        rejectEveryLayer(metadata,"unsupported source modifier packet");
        var missingTarget=new ArrayList<>(frames);
        missingTarget.set(3,"update\n|move|p2a: Opp|Scratch");
        check(OfflineShadowResponseScenarios.construct(fixture(missingTarget))
                .conditionalCases().size()==1,
                "untargeted public event remains supported in narrow slice");
        // A formally signed Phase 5 hypothesis cannot grant the current actor
        // an additional legal response, a probability, or historical identity.
        var hist=OfflineShadowResponseScenariosRegression.signedHistorical();
        var historical=OfflineShadowResponseScenariosRegression.input(frames,snapshot(),
                OfflineShadowResponseScenariosRegression.mechanics(false),
                new OfflineShadowResponseScenarios.HistoricalScope(hist,2));
        var audited=OfflineShadowCoverageAudit.inspect(auditInput(historical));
        check(audited.modeledPairCount()==1 && audited.fixturePairCount()==1
                && !audited.phase5IdentityBound()
                && !audited.currentBattleBound(),
                "historical source candidates remain isolated from fixture");
        check(!audited.toString().contains("PRIVATE_SECRET")
                && !audited.toString().contains("PRIVATE_OWNER")
                && !accepted.toString().contains("PRIVATE_SECRET"),
                "privacy-safe final diagnostics");
        System.out.println("Phase 6.6 offline cross-phase integration PASS: "
                +checks+" synthetic checks");
    }
}
