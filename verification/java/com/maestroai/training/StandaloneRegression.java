package com.maestroai.training;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Public Mega/form transitions must survive diagnostics without turning new forms,
     * unseen items, private split data, or stale actor leases into tactical evidence. */
    private static void publicFormReplay() {
        String start = "update\n|gen|9\n|gametype|singles\n|start\n"
                + "|switch|p2a: Public|Mewtwo, L100|100/100\n"
                + "|-ability|p2a: Public|Pressure\n"
                + "|-item|p2a: Public|Mewtwonite Y\n|turn|1";
        String mega = "update\n|detailschange|p2a: Public|Mewtwo-Mega-Y, L100\n"
                + "|-mega|p2a: Public|Mewtwonite Y\n"
                + "|-formechange|p2a: Public|Mewtwo-Mega-Y";
        String privateName = "PRIVATE_OPPONENT_MUST_NOT_LEAK";
        List<String> history = List.of(start,
                "sideupdate\np2\n|detailschange|p2a: " + privateName + "|Hidden",
                mega,
                "update\n|split|p2\n|-mega|p2a: " + privateName + "|Hidden",
                "update\n|-mega|p2a: Public|Mewtwonite Y");
        List<String> filtered = PublicProtocolFilter.publicEvents(history);
        check(filtered.stream().anyMatch(line -> line.startsWith("|detailschange|")));
        check(filtered.stream().anyMatch(line -> line.startsWith("|-formechange|")));
        check(filtered.stream().filter(line -> line.startsWith("|-mega|")).count() == 2);
        check(filtered.stream().noneMatch(line -> line.contains(privateName) || line.contains("sideupdate")));

        var provenance = PublicEnvelopeProvenance.reconcile(history, filtered);
        check(provenance.complete() && provenance.origins().size() == filtered.size());
        check(provenance.origins().getLast().route() == PublicEnvelopeProvenance.Route.SPLIT_PUBLIC);
        check(provenance.origins().getLast().splitTarget().equals("p2"));
        var evidence = new PublicProtocolEvidence();
        evidence.update(history);
        check(evidence.complete() && evidence.framed() && evidence.publicTurn() == 1);
        check(evidence.mechanicsEvents().contains("|-formechange|p2a: Public|Mewtwo-Mega-Y"));
        check(evidence.mechanicsEvents().contains("|-mega|p2a: Public|Mewtwonite Y"));

        var replay = new PublicBattleCapture();
        var battle = java.util.UUID.fromString("00000000-0000-0000-0000-000000000048");
        var captured = replay.observeWithProvenance(battle, history);
        check(captured.replay().size() == filtered.size());
        check(captured.envelopes().size() == filtered.size());
        check(captured.replay().stream().anyMatch(line -> line.contains("event=|-formechange|")));
        check(captured.replay().stream().anyMatch(line -> line.contains("event=|detailschange|")));
        check(captured.envelopes().getLast().contains("route=SPLIT_PUBLIC"));
        check(!captured.toString().contains(privateName));
        check(replay.observeWithProvenance(battle, history).replay().isEmpty());

        // An incomplete split must never be logged as an attested public change.
        var unfinished = new PublicBattleCapture();
        var unfinishedCapture = unfinished.observeWithProvenance(battle,
                List.of(start, "update\n|split|p2\n|-mega|p2a: " + privateName + "|Hidden"));
        check(unfinished.incomplete() && unfinishedCapture.replay().stream()
                .noneMatch(line -> line.contains(privateName) || line.contains("event=|-mega|")));
        check(unfinishedCapture.envelopes().stream().anyMatch(line -> line.contains("status=UNKNOWN")));
        // A changed historical form is an append-only violation, never a corrected replay.
        var edited = replay.observeWithProvenance(battle, List.of(start,
                "sideupdate\np2\n|detailschange|p2a: " + privateName + "|Hidden",
                "update\n|detailschange|p2a: Public|Different-Mega, L100"));
        check(replay.incomplete() && edited.replay().stream().anyMatch(line -> line.contains("SHADOW_REPLAY_GAP")));
        check(replay.observeWithProvenance(battle, history).replay().isEmpty());


        // A public Mega trigger alone must withdraw past reply attribution;
        // a previous move is not proof that a newly changed form can reuse it.
        var oldReply = new PublicProtocolEvidence();
        oldReply.update(List.of(start, "update\n|move|p2a: Public|Psycho Cut|p1a: Own"));
        check(PublicReplyCoverage.currentReplies(oldReply, "p2a").equals(List.of("psychocut")));
        var changedReply = new PublicProtocolEvidence();
        changedReply.update(List.of(start, "update\n|move|p2a: Public|Psycho Cut|p1a: Own",
                "update\n|-mega|p2a: Public|Mewtwonite Y"));
        check(PublicReplyCoverage.currentReplies(changedReply, "p2a").isEmpty());

        var provider = new PublicMechanicsEvidence(species -> null);
        var before = new PublicProtocolEvidence();
        before.update(List.of(start));
        provider.update(before);
        check(provider.observation("p2a").item().known()
                && provider.observation("p2a").ability().known());
        var megaOnly = new PublicProtocolEvidence();
        megaOnly.update(List.of(start, "update\n|-mega|p2a: Public|Mewtwonite Y"));
        provider.update(megaOnly);
        var unknown = provider.observation("p2a");
        check(unknown.unsupportedEffects() && !unknown.item().known()
                && !unknown.ability().known() && !unknown.confirmedSpecies().known());
    }


    private static ShadowMechanicsEvaluation.Comparison syntheticWorld(
            Map<String, ShadowMechanicsEvaluation.Utility> outcomes, boolean complete) {
        var rankings = outcomes.entrySet().stream().map(e -> {
            var value = PublicTransitionState.Known.of(e.getValue(), "source-bounded synthetic fixture");
            return new ShadowMechanicsEvaluation.Ranking(e.getKey(), value,
                    List.of(new ShadowMechanicsEvaluation.Line(null, value, Set.of())), Set.of());
        }).toList();
        return new ShadowMechanicsEvaluation.Comparison(rankings,null,null,null,false,complete,
                "synthetic-source-scoped;advisoryOnly=true");
    }

    private static void modifierEnvelopeRegressions() {
        var strong = new ModifierScenarioEnvelope.Scenario("public source variant A", syntheticWorld(Map.of(
                "move:attack",new ShadowMechanicsEvaluation.Utility(.7,.9),
                "move:setup",new ShadowMechanicsEvaluation.Utility(-.5,-.3),
                "switch:reserve",new ShadowMechanicsEvaluation.Utility(0,.1)),true));
        var moderate = new ModifierScenarioEnvelope.Scenario("public source variant B", syntheticWorld(Map.of(
                "move:attack",new ShadowMechanicsEvaluation.Utility(.6,.7),
                "move:setup",new ShadowMechanicsEvaluation.Utility(-.2,-.1),
                "switch:reserve",new ShadowMechanicsEvaluation.Utility(.05,.15)),true));
        var certificate = PublicTransitionState.Known.of(true,"EXPLICIT_SYNTHETIC_COMPLETE_DOMAIN_ONLY");
        var valid = ModifierScenarioEnvelope.compare(List.of(strong,moderate),certificate,()->true);
        check(valid.complete() && "move:attack".equals(valid.preferredConditional()));
        check(valid.bounds().get("move:attack").equals(new ShadowMechanicsEvaluation.Utility(.6,.9)));
        check(valid.bounds().get("switch:reserve").equals(new ShadowMechanicsEvaluation.Utility(0,.15)));
        var missing = ModifierScenarioEnvelope.compare(List.of(strong,moderate),
                PublicTransitionState.Known.unknown(),()->true);
        check(missing.preferredConditional() == null && missing.bounds().isEmpty()
                && missing.reasons().contains(ModifierScenarioEnvelope.Reason.DOMAIN_NOT_EXHAUSTIVELY_CERTIFIED));
        check(ModifierScenarioEnvelope.compare(List.of(strong,moderate),
                PublicTransitionState.Known.of(false,"source incomplete"),()->true).bounds().isEmpty());
        check(ModifierScenarioEnvelope.compare(List.of(strong,moderate),certificate,()->false)
                .reasons().contains(ModifierScenarioEnvelope.Reason.SOURCE_LEASE_EXPIRED));
        var overlapping = new ModifierScenarioEnvelope.Scenario("source variant C", syntheticWorld(Map.of(
                "move:attack",new ShadowMechanicsEvaluation.Utility(.1,.4),
                "move:setup",new ShadowMechanicsEvaluation.Utility(-.5,-.2),
                "switch:reserve",new ShadowMechanicsEvaluation.Utility(.2,.45)),true));
        var ambiguous = ModifierScenarioEnvelope.compare(List.of(strong,overlapping),certificate,()->true);
        check(ambiguous.preferredConditional() == null
                && ambiguous.reasons().contains(ModifierScenarioEnvelope.Reason.INTERVALS_OVERLAP_OR_TIE));
        check(ModifierScenarioEnvelope.compare(List.of(strong,strong),certificate,()->true)
                .reasons().contains(ModifierScenarioEnvelope.Reason.MISSING_OR_DUPLICATE_SCENARIO));
        var mismatched = new ModifierScenarioEnvelope.Scenario("missing root action",syntheticWorld(Map.of(
                "move:attack",new ShadowMechanicsEvaluation.Utility(.9,1),
                "switch:reserve",new ShadowMechanicsEvaluation.Utility(0,0)),true));
        check(ModifierScenarioEnvelope.compare(List.of(strong,mismatched),certificate,()->true)
                .reasons().contains(ModifierScenarioEnvelope.Reason.ACTION_UNIVERSE_MISMATCH));
        var noCoverage = new ModifierScenarioEnvelope.Scenario("partial",syntheticWorld(Map.of(
                "move:attack",new ShadowMechanicsEvaluation.Utility(.9,1),
                "move:setup",new ShadowMechanicsEvaluation.Utility(-1,0),
                "switch:reserve",new ShadowMechanicsEvaluation.Utility(0,0)),false));
        check(ModifierScenarioEnvelope.compare(List.of(strong,noCoverage),certificate,()->true)
                .reasons().contains(ModifierScenarioEnvelope.Reason.INCOMPLETE_ACTION_OR_OUTCOME));
        // A forged narrow rank can hide a catastrophic line; never certify it.
        var forged = new ShadowMechanicsEvaluation.Comparison(List.of(
                new ShadowMechanicsEvaluation.Ranking("move:attack",
                        PublicTransitionState.Known.of(new ShadowMechanicsEvaluation.Utility(.9,1),"forged"),
                        List.of(new ShadowMechanicsEvaluation.Line(null,
                                PublicTransitionState.Known.of(new ShadowMechanicsEvaluation.Utility(-1,1),"real line"),Set.of())),
                        Set.of()),
                new ShadowMechanicsEvaluation.Ranking("switch:reserve",
                        PublicTransitionState.Known.of(new ShadowMechanicsEvaluation.Utility(0,0),"fixture"),
                        List.of(new ShadowMechanicsEvaluation.Line(null,
                                PublicTransitionState.Known.of(new ShadowMechanicsEvaluation.Utility(0,0),"fixture"),Set.of())),
                        Set.of())),null,null,null,false,true,"synthetic;advisoryOnly=true");
        check(ModifierScenarioEnvelope.compare(List.of(
                new ModifierScenarioEnvelope.Scenario("forged candidate",forged)),certificate,()->true)
                .reasons().contains(ModifierScenarioEnvelope.Reason.INCOMPLETE_ACTION_OR_OUTCOME));

        check(ModifierScenarioEnvelope.compare(List.of(),certificate,()->true)
                .reasons().contains(ModifierScenarioEnvelope.Reason.MISSING_OR_DUPLICATE_SCENARIO));
        String secret = "PRIVATE_OPPONENT_PAYLOAD_DO_NOT_LOG";
        var secretWorld = new ModifierScenarioEnvelope.Scenario(secret,strong.comparison());
        check(!ModifierScenarioEnvelope.compare(List.of(secretWorld,moderate),certificate,()->true)
                .safeLine().contains(secret));
        var seq = new java.util.concurrent.atomic.AtomicInteger();
        check(ModifierScenarioEnvelope.compare(List.of(strong,moderate),certificate,
                () -> seq.incrementAndGet() == 1).bounds().isEmpty());
        check(!valid.safeLine().contains("source variant") && valid.safeLine().contains("advisoryOnly=true"));
    }

    public static void main(String[] args) {
        modifierEnvelopeRegressions();
        publicFormReplay();
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
