package com.maestroai.training;

import java.util.List;
import java.util.Map;

/** Standalone synthetic contracts; not empirical battle or Minecraft acceptance. */
public final class StandaloneRegression {
    private static int checks;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Synthetic contract failed");
        checks++;
    }
    private static void reject(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (RuntimeException expected) { rejected = true; }
        check(rejected);
    }
    private static BattleSnapshot.Combatant combatant(double hp, boolean fainted) {
        return new BattleSnapshot.Combatant("Synthetic", hp, List.of("normal"), Map.of(),
                List.of(), null, null, fainted);
    }
    private static ShadowSearchEngine.Branch branch(String kind, String name, double value,
                                                    double uncertainty, String next) {
        return new ShadowSearchEngine.Branch(kind, name,
                List.of(new ShadowSearchEngine.Outcome("synthetic-response", 1, value, uncertainty, next)));
    }
    public static void main(String[] args) {
        check(StrictOwnJson.read("{\"x\":[1,true,null]}").getAsJsonArray("x").size() == 3);
        reject(() -> StrictOwnJson.read("{\"x\":1,\"x\":2}"));
        reject(() -> StrictOwnJson.read("{}{}"));
        reject(() -> StrictOwnJson.read("{\"x\":/*comment*/1}"));
        reject(() -> StrictOwnJson.read("{\"x\":".repeat(20) + "0" + "}".repeat(20)));

        String visible = "|-damage|p1a: Synthetic|42/100";
        check(PublicProtocolFilter.publicEvents(List.of("|split|p1",
                "|-damage|p1a: Synthetic|1/123", visible)).equals(List.of(visible)));
        check(PublicProtocolFilter.publicEvents(List.of("sideupdate\n|turn|999",
                "|request|{\"synthetic\":true}", "|player|p1|Synthetic Player",
                "|custom-effect|unverified")).isEmpty());
        check(PublicProtocolFilter.publicEvents(List.of("|split|p1", visible)).isEmpty());
        List<String> events = PublicProtocolFilter.publicEvents(List.of("update\n|turn|2"));
        check(events.equals(List.of("|turn|2")));
        reject(() -> events.add("|turn|3"));

        check(!combatant(BattleSnapshot.UNKNOWN_HP_FRACTION, false).hpKnown());
        reject(() -> combatant(Double.NaN, false));
        reject(() -> combatant(1.1, false));
        reject(() -> combatant(BattleSnapshot.UNKNOWN_HP_FRACTION, true));
        BattleSnapshot snapshot = new BattleSnapshot(1, combatant(1, false),
                combatant(BattleSnapshot.UNKNOWN_HP_FRACTION, false), List.of(), List.of(),
                List.of("attack", "setup"), List.of("reserve"), Map.of(), false, null);
        reject(() -> snapshot.validateDecision(new BattleSnapshot.Decision("move", "unavailable", 0, Map.of(), "synthetic")));
        reject(() -> snapshot.validateDecision(new BattleSnapshot.Decision("switch", "unavailable", 0, Map.of(), "synthetic")));

        // Independent scalar oracle: greedy immediate baseline =3; setup then attack =1+5=6.
        Map<String, ShadowSearchEngine.Position> graph = Map.of(
                "root", new ShadowSearchEngine.Position("root", List.of(
                        branch("move", "attack", 2, 0, "leaf"),
                        branch("switch", "reserve", 3, 0, "leaf"),
                        branch("move", "setup", 1, 3, "boosted")), 0),
                "boosted", new ShadowSearchEngine.Position("boosted", List.of(
                        branch("move", "attack", 5, 0, "leaf")), 0),
                "leaf", new ShadowSearchEngine.Position("leaf", List.of(), 0));
        var setup = ShadowSearchEngine.search(snapshot, "root", graph, 100, 0);
        check(setup.complete() && setup.preferred().equals("move:setup"));
        check(setup.ranking().getFirst().expectedValue() == 6 && 6 > 3);
        var uncertain = ShadowSearchEngine.search(snapshot, "root", graph, 100, 2);
        check(uncertain.preferred().equals("switch:reserve"));
        check(uncertain.ranking().getFirst().expectedValue() == 3);
        var limited = ShadowSearchEngine.search(snapshot, "root", graph, 1, 0);
        check(!limited.complete() && limited.nodes() == 1 && limited.preferred().equals("move:attack"));
        reject(() -> new ShadowSearchEngine.Branch("move", "attack", List.of(
                new ShadowSearchEngine.Outcome("synthetic", .5, 1, 0, "leaf"))));
        AcceptanceGateRegression.run();
        OwnChoiceRecoveryRegression.run();
        RuntimeEvidenceRegression.run();
        System.out.println("Standalone synthetic regressions PASS: " + checks + " checks");
    }
}
