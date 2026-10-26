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
    private static Map<String,CobblemonMechanics.Profile> fullSyntheticProfiles(){
        var stats=new TreeMap<String,Known<CobblemonMechanics.IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))
            stats.put(stat,Known.of(CobblemonMechanics.IntRange.exact(100),
                    "conditional stat, not native actor evidence"));
        var assumed=Known.of(true,"synthetic unmodified-effects precondition");
        return Map.of(
                "self",new CobblemonMechanics.Profile("self",
                    Known.of(50,"synthetic level"),
                    Known.of(CobblemonMechanics.IntRange.exact(150),"synthetic HP"),
                    stats,Known.of(List.of("bug","steel"),"synthetic typing"),assumed),
                "opponent",new CobblemonMechanics.Profile("opponent",
                    Known.of(50,"synthetic level"),
                    Known.of(CobblemonMechanics.IntRange.exact(150),"synthetic HP"),
                    stats,Known.of(List.of("normal"),"synthetic typing"),assumed));
    }
    private static OfflineShadowResponseScenarios.Input stagedFixture(
            List<String> witnessed, List<String> replies) {
        var root=battle(1,List.of("swift"),replies,List.of());
        var own=new BattleSnapshot.Combatant(root.self().species(),1,
                root.self().types(),
                Map.of("spa",0,"spd",0,"spe",0),List.of(),null,null,false);
        var staged=new BattleSnapshot(1,own,root.opponent(),List.of(),List.of(),
                root.legalMoves(),root.legalSwitches(),Map.of(),false,null);
        var basic=OfflineShadowResponseScenariosRegression.mechanics(false);
        var phase4=new PublicStateSearchAdapter.PublicMechanics(
                Map.of(),Map.of("spa",0,"spd",0,"spe",0),Set.of(),
                false,false,false,0,basic.transitions());
        return OfflineShadowResponseScenariosRegression.input(
                witnessed,staged,phase4,null);
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
        // Previously total flattened line-count was mistaken for per-reply
        // provenance. Identical speed means BOTH native conditional orders.
        var tie=stagedFixture(
                OfflineShadowResponseScenariosRegression.frames("Swift"),
                List.of("swift"));
        var tiedPlans=OfflineShadowResponseScenarios.construct(tie).conditionalPlans();
        var nativeConditional=ShadowMechanicsEvaluation.compare(
                tie.snapshot(),tie.mechanics(),fullSyntheticProfiles(),tiedPlans,
                Known.of(false,"synthetic order"),CobblemonMechanics.Critical.NON_CRITICAL,null);
        check(nativeConditional.rankings().getFirst().lines().size()>1,
                "speed-tie source conditional has multiple branches for ONE witnessed reply");
        var tiedAudit=OfflineShadowCoverageAudit.inspect(
                new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                    tie,fullSyntheticProfiles(),Known.of(false,"synthetic order"),
                    CobblemonMechanics.Critical.NON_CRITICAL));
        check(tiedAudit.fixturePairCount()==1 && tiedAudit.modeledPairCount()==1
                && tiedAudit.conditionallyResolvedPairCount()==1
                && tiedAudit.actions().getFirst().conditionalIntervalResolved(),
                "every source-resolved speed-order branch of a single witnessed reply counts once");
        check(tiedAudit.actions().getFirst().conditionalWorstLow()<=
                    tiedAudit.actions().getFirst().conditionalBestHigh()
                && !tiedAudit.recommendedActionAvailable()
                && !tiedAudit.opponentResponseSetComplete(),
                "sensitivity interval cannot become opponent certainty or recommendation");
        var mixed=stagedFixture(
                OfflineShadowResponseScenariosRegression.frames("Swift","Scratch"),
                List.of("swift","scratch"));
        var mixedAudit=OfflineShadowCoverageAudit.inspect(
                new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                    mixed,fullSyntheticProfiles(),Known.of(false,"synthetic order"),
                    CobblemonMechanics.Critical.NON_CRITICAL));
        check(mixedAudit.fixturePairCount()==2 && mixedAudit.modeledPairCount()==2
                && mixedAudit.conditionallyResolvedPairCount()<2
                && !mixedAudit.actions().getFirst().conditionalIntervalResolved(),
                "unknown branch in another witnessed reply suppresses overall interval");
        var unknownPolicy=OfflineShadowCoverageAudit.inspect(
                new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                    tie,unknown(),Known.of(false,"synthetic order"),
                    CobblemonMechanics.Critical.NON_CRITICAL));
        check(unknownPolicy.conditionallyResolvedPairCount()==0
                && !unknownPolicy.actions().getFirst().conditionalIntervalResolved()
                && unknownPolicy.abstentions().contains(
                    OfflineShadowCoverageAudit.Abstention.EFFECTS_OR_BRANCH_MAPPING_UNRESOLVED),
                "changing explicit Phase4 stat evidence to UNKNOWN cannot create certainty");
        var broadened=OfflineShadowCoverageAudit.inspect(
                new OfflineShadowCoverageAudit.Input(OfflineShadowCoverageAudit.SCHEMA,
                    tie,fullSyntheticProfiles(),Known.of(false,"synthetic order"),
                    CobblemonMechanics.Critical.BOTH));
        check(!broadened.recommendedActionAvailable()
                && !broadened.installedEffectsCertified()
                && broadened.conditionallyResolvedPairCount()<=1,
                "critical-range expansion never promotes source-conditional results");
        System.out.println("Phase 6.4 offline coverage PASS: "+checks+" synthetic checks");
    }
}
