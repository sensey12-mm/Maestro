package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Adversarial synthetic-only Phase 6.4 coverage/uncertainty contract. */
final class OfflineShadowCoverageAuditRegression {
    private OfflineShadowCoverageAuditRegression() {}
    private static int checks;
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError("Phase 6.4: "+message);
        checks++;
    }
    private static void reject(Runnable r,String message){
        try{r.run();}catch(IllegalArgumentException expected){checks++;return;}
        throw new AssertionError("Phase 6.4 accepted "+message);
    }
    private static Map<String,CobblemonMechanics.Profile> unknown(){
        return Map.of("self",CobblemonMechanics.Profile.unknown("self"),
                "opponent",CobblemonMechanics.Profile.unknown("opponent"));
    }
    private static OfflineShadowResponseScenarios.Input scenario(
            List<String> frames,BattleSnapshot snapshot){
        return OfflineShadowResponseScenariosRegression.input(frames,snapshot);
    }
    private static OfflineShadowCoverageAudit.Input request(
            OfflineShadowResponseScenarios.Input scenario){
        return new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                scenario,unknown(),Known.of(false,"synthetic order"),
                CobblemonMechanics.Critical.NON_CRITICAL);
    }
    private static BattleSnapshot battle(int turn,List<String> own,List<String> observed,List<String> switches){
        return OfflineShadowResponseScenariosRegression.snapshot(turn,own,observed,switches);
    }
    static void run(){
        var history=OfflineShadowResponseScenariosRegression.frames("Scratch","Tackle");
        var state=battle(1,List.of("swift","tackle"),List.of("scratch","tackle"),List.of());
        var base=scenario(history,state);
        var result=OfflineShadowCoverageAudit.inspect(request(base));
        check(result.fixtureLegalOwnActions()==2 && result.sourceListedOwnMoves()==2
                && result.witnessedReplyIds()==2 && result.sourceListedWitnessedReplies()==2
                && result.fixturePairCount()==4 && result.modeledPairCount()==4,
                "exact synthetic witness Cartesian product");
        check(result.actions().size()==2
                && result.actions().getFirst().action().equals("move:swift")
                && result.actions().getLast().action().equals("move:tackle"),
                "stable own action order");
        check(result.conditionallyResolvedPairCount()<=4
                && result.unresolvedFixturePairCount()==4-result.conditionallyResolvedPairCount()
                && result.actions().stream().allMatch(a->a.modeledCases()==2
                    && a.resolvedCases()<=a.modeledCases()),"resolution counters conservative");
        check(result.actions().equals(OfflineShadowCoverageAudit.inspect(request(base)).actions()),
                "repeated audit deterministic");
        check(!result.opponentResponseSetComplete() && !result.installedEffectsCertified()
                && !result.calibratedResponseProbabilities()
                && !result.recommendedActionAvailable() && !result.phase5IdentityBound()
                && !result.currentBattleBound() && !result.legalActionsRuntimeAttested()
                && !result.shadowSearchAuthorized() && !result.livePromotionAuthorized(),
                "all authority gates closed even if fixture appears covered");
        check(result.abstentions().contains(OfflineShadowCoverageAudit.Abstention.OPPONENT_RESPONSE_SET_OPEN)
                && result.abstentions().contains(OfflineShadowCoverageAudit.Abstention.NO_CALIBRATED_PROBABILITIES),
                "unknown opponent replies and calibration remain explicit");
        try{result.actions().clear();throw new AssertionError("mutable actions");}
        catch(UnsupportedOperationException expected){checks++;}
        var noReplies=OfflineShadowCoverageAudit.inspect(request(scenario(
                OfflineShadowResponseScenariosRegression.frames(),
                battle(1,List.of("tackle"),List.of(),List.of()))));
        check(noReplies.fixturePairCount()==0 && noReplies.modeledPairCount()==0
                && !noReplies.fixturePairsAllModeled()
                && noReplies.abstentions().contains(
                    OfflineShadowCoverageAudit.Abstention.NO_WITNESSED_RESPONSES),
                "zero witnesses never imply perfect coverage");
        var unsupported=OfflineShadowCoverageAudit.inspect(request(scenario(
                OfflineShadowResponseScenariosRegression.frames("Secret Move"),
                battle(1,List.of("tackle"),List.of("secretmove"),List.of()))));
        check(unsupported.fixturePairCount()==1 && unsupported.modeledPairCount()==0
                && unsupported.sourceListedWitnessedReplies()==0
                && unsupported.unresolvedFixturePairCount()==1,
                "source-unlisted public reply remains in denominator");
        var unsupportedOwn=OfflineShadowCoverageAudit.inspect(request(scenario(
                OfflineShadowResponseScenariosRegression.frames("Scratch"),
                battle(1,List.of("tackle","secretattack"),List.of("scratch"),List.of()))));
        check(unsupportedOwn.fixturePairCount()==2 && unsupportedOwn.modeledPairCount()==1
                && unsupportedOwn.sourceListedOwnMoves()==1
                && unsupportedOwn.abstentions().contains(
                   OfflineShadowCoverageAudit.Abstention.OWN_ACTION_COVERAGE_INCOMPLETE),
                "source-unlisted own move remains incomplete");
        var reserve=OfflineShadowCoverageAudit.inspect(request(scenario(
                OfflineShadowResponseScenariosRegression.frames("Scratch"),
                battle(1,List.of("tackle"),List.of("scratch"),List.of("reserve")))));
        check(reserve.fixturePairCount()==2 && reserve.modeledPairCount()==1
                && reserve.actions().stream().anyMatch(a->
                   a.action().equals("switch:reserve") && a.modeledCases()==0),
                "switch not modeled without entry certificate");
        reject(()->new OfflineShadowCoverageAudit.Input("future-6.5",
                base,unknown(),Known.of(false,"synthetic order"),
                CobblemonMechanics.Critical.NON_CRITICAL),"schema drift");
        reject(()->new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                base,Map.of("self",CobblemonMechanics.Profile.unknown("self")),
                Known.of(false,"synthetic order"),CobblemonMechanics.Critical.NON_CRITICAL),
                "missing opponent profile");
        reject(()->new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                base,Map.of("self",CobblemonMechanics.Profile.unknown("opponent"),
                            "opponent",CobblemonMechanics.Profile.unknown("self")),
                Known.of(false,"synthetic order"),CobblemonMechanics.Critical.NON_CRITICAL),
                "cross-actor profiles");
        reject(()->OfflineShadowCoverageAudit.inspect(request(scenario(history,
                battle(2,List.of("tackle","swift"),List.of("scratch","tackle"),List.of())))),
                "stale public turn");
        var privateHistory=new ArrayList<>(OfflineShadowResponseScenariosRegression.frames("Scratch"));
        privateHistory.add("update\n|request|HIDDEN_PRIVATE_DATA");
        reject(()->OfflineShadowCoverageAudit.inspect(request(scenario(privateHistory,
                battle(1,List.of("tackle"),List.of("scratch"),List.of())))),
                "private instruction disguised as public packet");
        var reentry=new ArrayList<>(OfflineShadowResponseScenariosRegression.frames("Scratch"));
        reentry.add("update\n|switch|p2a: Opp|synthetic, L50|100/100");
        reject(()->OfflineShadowCoverageAudit.inspect(request(scenario(reentry,
                battle(1,List.of("tackle"),List.of("scratch"),List.of())))),
                "ambiguous reused appearance");
        var archived=OfflineShadowResponseScenariosRegression.signedHistorical();
        var signed=OfflineShadowResponseScenariosRegression.input(
                OfflineShadowResponseScenariosRegression.frames("Scratch"),
                battle(1,List.of("tackle"),List.of("scratch"),List.of()),
                OfflineShadowResponseScenariosRegression.mechanics(false),
                new OfflineShadowResponseScenarios.HistoricalScope(archived,2));
        var isolated=OfflineShadowCoverageAudit.inspect(request(signed));
        check(isolated.fixturePairCount()==1 && isolated.modeledPairCount()==1
                && !isolated.phase5IdentityBound()
                && !isolated.toString().contains("PRIVATE_OWNER")
                && !isolated.toString().contains("PRIVATE_NICK"),
                "signed Phase 5 guesses never enter current fixture");
        System.out.println("Phase 6.4 offline coverage PASS: "+checks+" synthetic checks");
    }
}
