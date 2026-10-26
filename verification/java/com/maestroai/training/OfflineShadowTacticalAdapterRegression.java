package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Phase 6.1 synthetic, adversarial, offline-only regression contract. */
final class OfflineShadowTacticalAdapterRegression {
    private OfflineShadowTacticalAdapterRegression() {}
    private static int checks;
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError("Phase 6.1: " + message);
        checks++;
    }
    private static void reject(Runnable action, String message) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Phase 6.1: " + message);
    }
    private static BattleSnapshot.Combatant actor(List<String> revealed) {
        return new BattleSnapshot.Combatant("synthetic", 1, List.of("normal"),
                Map.of(), revealed, null, null, false);
    }
    private static BattleSnapshot snapshot(List<String> own, List<String> replies) {
        return new BattleSnapshot(1, actor(List.of()), actor(replies),
                List.of(), List.of(), own, List.of(), Map.of(), false, null);
    }
    private static PublicStateSearchAdapter.PublicMechanics mechanics(Set<String> ambiguity,
                                                                       boolean trapped) {
        var known = Known.of(new PublicStateSearchAdapter.Interval(1,1), "synthetic public HP");
        var none = Known.of("none", "synthetic public status");
        var hazards = new Hazards(Map.of(), true, "synthetic field");
        var ctx = new Context(known, known, none, none, hazards, hazards,
                Map.of(), Map.of(), ambiguity);
        return new PublicStateSearchAdapter.PublicMechanics(Map.of(), Map.of(), Set.of(),
                false, false, trapped, 0, new Evidence(ctx, Map.of(), Map.of()));
    }
    private static Map<String, CobblemonMechanics.Profile> profiles() {
        var stats = new TreeMap<String, Known<CobblemonMechanics.IntRange>>();
        for (String name : List.of("atk","def","spa","spd","spe"))
            stats.put(name, Known.of(CobblemonMechanics.IntRange.exact(100), "synthetic stat"));
        var knownTypes = Known.of(List.of("normal"), "synthetic types");
        var ordinary = Known.of(true, "synthetic scenario assumption");
        return Map.of(
                "self", new CobblemonMechanics.Profile("self",
                    Known.of(50, "synthetic level"), Known.of(CobblemonMechanics.IntRange.exact(150), "synthetic HP"),
                    stats, knownTypes, ordinary),
                "opponent", new CobblemonMechanics.Profile("opponent",
                    Known.of(50, "synthetic level"), Known.of(CobblemonMechanics.IntRange.exact(150), "synthetic HP"),
                    stats, knownTypes, ordinary));
    }
    private static ShadowMechanicsEvaluation.Plan plan(String action, String reply) {
        return new ShadowMechanicsEvaluation.Plan(action,
                List.of(List.of(new ShadowMechanicsEvaluation.Turn(action, reply, null))),
                "synthetic source-conditional only");
    }
    private static OfflineShadowTacticalAdapter.Request request(
            BattleSnapshot snap, PublicStateSearchAdapter.PublicMechanics mechanics,
            List<ShadowMechanicsEvaluation.Plan> plans) {
        return new OfflineShadowTacticalAdapter.Request(OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(), CobblemonMechanics.SOURCE,
                snap, mechanics, profiles(), plans, Known.of(false,"synthetic order"),
                CobblemonMechanics.Critical.NON_CRITICAL, null, false, false, false);
    }
    static void run() {
        var snap = snapshot(List.of("tackle"), List.of("scratch"));
        var base = request(snap, mechanics(Set.of(), false),
                List.of(plan("move:tackle","scratch")));
        var result = OfflineShadowTacticalAdapter.inspect(base);
        check(result.actions().size()==1
                && result.actions().getFirst().ownLegalAction().equals("move:tackle")
                && result.actions().getFirst().sourceListedMove(),
                "Phase 4 source move integrated, not guessed");
        check(result.abstentions().contains(OfflineShadowTacticalAdapter.Abstention.NO_CALIBRATED_OPPONENT_RESPONSE_PROBABILITIES)
                && result.abstentions().contains(OfflineShadowTacticalAdapter.Abstention.PHASE5_SIGNED_HANDOFF_NOT_SUPPLIED),
                "missing response calibration and Phase 5 signed history remain explicit");
        check(!result.currentBattleBound() && !result.legalActionsRuntimeAttested()
                && !result.calibratedOpponentProbabilities() && !result.opponentIdentityProven()
                && !result.shadowSearchAuthorized() && !result.tacticalRecommendationAvailable()
                && !result.livePromotionAuthorized(), "all runtime and promotion gates closed");
        check(result.actions().equals(OfflineShadowTacticalAdapter.inspect(base).actions())
                && result.abstentions().equals(OfflineShadowTacticalAdapter.inspect(base).abstentions()),
                "deterministic repeated evaluation");
        try { result.actions().clear(); throw new AssertionError("mutable diagnostics"); }
        catch (UnsupportedOperationException expected) { checks++; }

        reject(() -> OfflineShadowTacticalAdapter.inspect(request(snap,
                mechanics(Set.of(),false), List.of(plan("move:shadowball","scratch")))),
                "illegal move rejected despite plausible species");
        reject(() -> OfflineShadowTacticalAdapter.inspect(request(snap,
                mechanics(Set.of(),false), List.of(plan("move:tackle","unobserved")))),
                "Phase 5 hypothetical/unobserved response may not enter Phase 4");
        reject(() -> OfflineShadowTacticalAdapter.inspect(request(snap,
                mechanics(Set.of(),false), List.of(plan("move:tackle","scratch"),
                        plan("move:tackle","scratch")))), "duplicate candidate rejected");
        var multi = snapshot(List.of("tackle","swift"), List.of("scratch"));
        var partial = OfflineShadowTacticalAdapter.inspect(request(multi,
                mechanics(Set.of(),false),List.of(plan("move:tackle","scratch"))));
        check(partial.abstentions().contains(OfflineShadowTacticalAdapter.Abstention.INCOMPLETE_LEGAL_ACTION_COVERAGE),
                "partial action coverage is not a ranking certificate");
        var unknown = OfflineShadowTacticalAdapter.inspect(request(
                snapshot(List.of("recover"),List.of("scratch")),
                mechanics(Set.of(),false),List.of(plan("move:recover","scratch"))));
        check(unknown.abstentions().contains(OfflineShadowTacticalAdapter.Abstention.PHASE4_CONDITIONAL_EFFECTS_UNRESOLVED),
                "unsupported status/recovery effects abstain");
        var ambiguous = OfflineShadowTacticalAdapter.inspect(request(snap,
                mechanics(Set.of("ILLUSION","TRANSFORMED"),false),
                List.of(plan("move:tackle","scratch"))));
        check(ambiguous.abstentions().contains(OfflineShadowTacticalAdapter.Abstention.AMBIGUOUS_OPPONENT_IDENTITY),
                "Illusion and Transform not resolved into unique identity");

        var switchFixture = new BattleSnapshot(1,actor(List.of()),actor(List.of("scratch")),
                List.of(),List.of(),List.of("tackle"),List.of("reserve"),
                Map.of(),false,null);
        var switchReview = OfflineShadowTacticalAdapter.inspect(request(
                switchFixture, mechanics(Set.of(),false),
                List.of(plan("switch:reserve","scratch"))));
        check(switchReview.actions().size()==1
                && !switchReview.actions().getFirst().sourceListedMove(),
                "switches are not misreported as Phase 4 source-listed moves");
        var trapped = new BattleSnapshot(1,actor(List.of()),actor(List.of("scratch")),
                List.of(),List.of(),List.of("tackle"),List.of("reserve"),
                Map.of(),false,null);
        reject(() -> OfflineShadowTacticalAdapter.inspect(request(trapped,
                mechanics(Set.of(),true),List.of(plan("switch:reserve","scratch")))),
                "trapped voluntary switch rejected");
        var forgedOpponent = new BattleSnapshot(1,actor(List.of()),
                new BattleSnapshot.Combatant("synthetic",1,List.of("normal"),
                        Map.of(),List.of("scratch"),"SECRET_OPPONENT_ITEM",null,false),
                List.of(),List.of(),List.of("tackle"),List.of(),Map.of(),false,null);
        reject(() -> request(forgedOpponent,mechanics(Set.of(),false),
                List.of(plan("move:tackle","scratch"))),
                "unbound item/ability cannot silently become public evidence");
        var identityJoin = new BattleSnapshot(1,actor(List.of()),actor(List.of("scratch")),
                List.of(),List.of(actor(List.of("scratch"))),List.of("tackle"),List.of(),
                Map.of(),false,null);
        reject(() -> request(identityJoin,mechanics(Set.of(),false),
                List.of(plan("move:tackle","scratch"))),
                "same appearance/nickname cannot map opponent reserves");
        var hiddenMoveCount = new BattleSnapshot(1,actor(List.of()),actor(List.of("scratch")),
                List.of(),List.of(),List.of("tackle"),List.of(),
                Map.of("PRIVATE_UNOBSERVED_MOVE",1),false,null);
        reject(() -> request(hiddenMoveCount,mechanics(Set.of(),false),
                List.of(plan("move:tackle","scratch"))), "hidden moves cannot be smuggled as counts");
        reject(() -> new OfflineShadowTacticalAdapter.Request(
                OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(), "wrong-mechanics-version",snap,
                mechanics(Set.of(),false),profiles(),List.of(plan("move:tackle","scratch")),
                Known.of(false,"synthetic"),CobblemonMechanics.Critical.NON_CRITICAL,
                null,false,false,false), "mechanics scope drift rejected");
        reject(() -> new OfflineShadowTacticalAdapter.Request(
                OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,snap,
                mechanics(Set.of(),false),profiles(),List.of(plan("move:tackle","scratch")),
                Known.of(false,"synthetic"),CobblemonMechanics.Critical.NON_CRITICAL,
                null,true,false,false), "current-battle provenance claims rejected");
        reject(() -> new OfflineShadowTacticalAdapter.Request(
                OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,snap,
                mechanics(Set.of(),false),profiles(),List.of(plan("move:tackle","scratch")),
                Known.of(false,"synthetic"),CobblemonMechanics.Critical.NON_CRITICAL,
                null,false,true,false), "tactical/live authority rejected");
        reject(() -> new OfflineShadowTacticalAdapter.Request(
                OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,snap,
                mechanics(Set.of(),false),profiles(),List.of(plan("move:tackle","scratch")),
                Known.of(false,"synthetic"),CobblemonMechanics.Critical.NON_CRITICAL,
                null,false,false,true), "appearance as stable identity rejected");
        check(!result.toString().contains("SECRET_OPPONENT_ITEM")
                && !result.toString().contains("PRIVATE_UNOBSERVED_MOVE"),
                "diagnostic serialization excludes private injected source strings");
        System.out.println("Phase 6.1 offline contract PASS: " + checks + " synthetic checks");
    }
}
