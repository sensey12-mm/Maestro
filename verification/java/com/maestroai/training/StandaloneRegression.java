package com.maestroai.training;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Standalone synthetic contracts; not empirical battle or Minecraft acceptance. */
public final class StandaloneRegression {
    private static final Map<String, List<String>> FIXED_ROSTER = Map.ofEntries(
            Map.entry("Genesect", List.of("uturn", "ironhead", "icebeam", "flamethrower")),
            Map.entry("Calyrex-Shadow", List.of("astralbarrage", "psyshock", "drainingkiss", "energyball")),
            Map.entry("Xerneas", List.of("geomancy", "moonblast", "focusblast", "thunder")),
            Map.entry("Arceus", List.of("swordsdance", "extremespeed", "shadowclaw", "recover")),
            Map.entry("Mewtwo-Mega-Y", List.of("psystrike", "icebeam", "fireblast", "nastyplot")),
            Map.entry("Marshadow", List.of("spectralthief", "closecombat", "shadowsneak", "rocktomb"))
    );
    private static int checks;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Synthetic contract failed");
        checks++;
    }
    private static void check(boolean condition,String why) {
        if (!condition) throw new AssertionError(why);
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

    /** Regression of actual Maestro Calyrex-Shadow move definitions against
     * the independent pinned Showdown source oracle (verification/source_oracle).
     * No assertion that this fixture bypasses real Life Orb / As One or proves
     * an in-game tactic.
     */
    private static void calyrexMoveCoverage() {
        var astro=CobblemonMechanics.move("astralbarrage");
        var shock=CobblemonMechanics.move("psyshock");
        check(astro != null && astro.power()==120 && astro.damageOnly()
                && astro.attackingStat().equals("spa") && astro.defendingStat().equals("spd"));
        check(shock != null && shock.power()==80 && shock.damageOnly()
                && shock.attackingStat().equals("spa") && shock.defendingStat().equals("def"));
        for(String id:List.of("ironhead","icebeam","moonblast","geomancy","uturn","drainingkiss"))
            check(CobblemonMechanics.move(id)!=null
                    && !MaestroOwnMoveAudit.completelySimulated(id),
                    "experimental side effects/pivot/charge must remain incompletely simulated: "+id);
        check(!CobblemonMechanics.move("flamethrower").damageOnly());

        var yes=PublicTransitionState.Known.of(true,"synthetic ordinary mechanics-only");
        var known=PublicTransitionState.Known.of(100,"synthetic level");
        var offense=Map.of("atk",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(50),"synthetic"),
                "spa",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(300),"synthetic"));
        var targetDef=Map.of("def",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(150),"synthetic"),
                "spd",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(450),"synthetic"));
        var atk=new CobblemonMechanics.Profile("self",known,
                PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(300),"synthetic"),offense,
                PublicTransitionState.Known.of(List.of("psychic"),"synthetic types"),yes);
        var def=new CobblemonMechanics.Profile("opponent",known,
                PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(300),"synthetic"),targetDef,
                PublicTransitionState.Known.of(List.of("psychic"),"synthetic types"),yes);
        var stages=Map.of("atk",0,"def",0,"spa",0,"spd",0,"spe",0);
        var good=PublicTransitionState.Known.of(new PublicStateSearchAdapter.Interval(1,1),"synthetic public HP");
        var status=PublicTransitionState.Known.of("none","synthetic status");
        var self=new PublicTransitionState.Actor("self",
                new BattleSnapshot.Combatant("Mew",1,List.of("psychic"),stages,List.of(),null,null,false),
                good,stages,status);
        var foe=new PublicTransitionState.Actor("opponent",
                new BattleSnapshot.Combatant("Mew",1,List.of("psychic"),stages,List.of(),null,null,false),
                good,stages,status);
        var astral=CobblemonMechanics.damage("astralbarrage",atk,self,def,foe,CobblemonMechanics.Critical.BOTH);
        var psycho=CobblemonMechanics.damage("psyshock",atk,self,def,foe,CobblemonMechanics.Critical.BOTH);
        check(astral.points().known() && psycho.points().known()
                && astral.points().value().low()>0 && psycho.points().value().low()>0);
        var highDef=new CobblemonMechanics.Profile("opponent",known,def.maxHp(),
                Map.of("def",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(300),"synthetic"),
                       "spd",targetDef.get("spd")),def.types(),yes);
        check(CobblemonMechanics.damage("psyshock",atk,self,highDef,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().high() <
                CobblemonMechanics.damage("psyshock",atk,self,def,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().high(), "Psyshock physical Defense ignored");
        check(CobblemonMechanics.damage("astralbarrage",atk,self,highDef,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().equals(CobblemonMechanics.damage("astralbarrage",atk,self,def,foe,
                        CobblemonMechanics.Critical.NON_CRITICAL).points().value()),
                "Astral Barrage illegally switched to Defense");
        var highSpd=new CobblemonMechanics.Profile("opponent",known,def.maxHp(),
                Map.of("def",targetDef.get("def"),
                       "spd",PublicTransitionState.Known.of(CobblemonMechanics.IntRange.exact(900),"synthetic")),def.types(),yes);
        check(CobblemonMechanics.damage("psyshock",atk,self,highSpd,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().equals(CobblemonMechanics.damage("psyshock",atk,self,def,foe,
                        CobblemonMechanics.Critical.NON_CRITICAL).points().value()),
                "Psyshock illegally used Special Defense");
        check(CobblemonMechanics.damage("astralbarrage",atk,self,highSpd,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().high() <
                CobblemonMechanics.damage("astralbarrage",atk,self,def,foe,CobblemonMechanics.Critical.NON_CRITICAL)
                .points().value().high(), "Astral Barrage ignored Special Defense");
        var missing=new CobblemonMechanics.Profile("opponent",known,def.maxHp(),
                Map.of("def",targetDef.get("def")),def.types(),yes);
        check(!CobblemonMechanics.damage("astralbarrage",atk,self,missing,foe,
                CobblemonMechanics.Critical.BOTH).fraction().known(),
                "missing required special-defense evidence was guessed");
        var noDef=new CobblemonMechanics.Profile("opponent",known,def.maxHp(),
                Map.of("spd",targetDef.get("spd")),def.types(),yes);
        check(!CobblemonMechanics.damage("psyshock",atk,self,noDef,foe,
                CobblemonMechanics.Critical.BOTH).fraction().known(),
                "missing physical-defense evidence was guessed");
        var unknown= new CobblemonMechanics.Profile("opponent",known,def.maxHp(),
                targetDef,def.types(),PublicTransitionState.Known.unknown());
        check(!CobblemonMechanics.damage("psyshock",atk,self,unknown,foe,
                CobblemonMechanics.Critical.BOTH).fraction().known(),
                "unknown opponent modifiers were assumed neutral");
    }

    /** Explicitly account for every fixed own-team move without incorrectly
     * upgrading secondary/pivot/charge/etc. to source-bounded forecasts. */
    private static void maestroWholeRosterCoverage() {
        var ids=new java.util.TreeSet<String>();
        FIXED_ROSTER.values().forEach(ids::addAll);
        check(FIXED_ROSTER.size()==6);
        check(ids.size()==23);
        var count=0;
        for(var e:FIXED_ROSTER.entrySet()) {
            check(e.getValue().size()==4 && e.getValue().stream().distinct().count()==4);
            for(var id:e.getValue()) {
                count++;
                var move=CobblemonMechanics.move(id);
                check(move!=null, "actual Maestro move has no known calculator properties: "+id);
                check(move.accuracy()>=1 && move.accuracy()<=100);
                if(!MaestroOwnMoveAudit.completelySimulated(id)) {
                    String label=MaestroOwnMoveAudit.unmodeledEffect(id);
                    check(!label.equals("SECONDARY_OR_OTHER_MOVE_EFFECTS_UNRESOLVED"),
                         "actual Maestro effect missing precise unsupported reason: "+id);
                }
            }
        }
        check(count==24);
        check(CobblemonMechanics.move("psystrike").attackingStat().equals("spa")
                && CobblemonMechanics.move("psystrike").defendingStat().equals("def"));
        check(CobblemonMechanics.move("geomancy").power()==0
                && !MaestroOwnMoveAudit.completelySimulated("geomancy"));
        check(CobblemonMechanics.move("uturn").power()==70
                && !MaestroOwnMoveAudit.completelySimulated("uturn"));
        check(!MaestroOwnMoveAudit.completelySimulated("icebeam"));
        check(MaestroOwnMoveAudit.completelySimulated("astralbarrage"));
        check(MaestroOwnMoveAudit.completelySimulated("swordsdance"));
        for(var t:List.of("normal","fire","water","electric","grass","ice","fighting","poison",
               "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"))
            check(CobblemonMechanics.effectiveness(t,List.of("normal"))<3);
        check(CobblemonMechanics.effectiveness("ground",List.of("flying"))==Integer.MIN_VALUE);
        check(CobblemonMechanics.effectiveness("fairy",List.of("dragon"))==1);
        check(CobblemonMechanics.effectiveness("steel",List.of("fairy"))==1);
    }

    private static void ownMechanicsExecutionAudit(){
        var special=CobblemonMechanics.move("drainingkiss");
        check(special.power()==50&&!special.damageOnly());
        check(CobblemonMechanics.move("closecombat").power()==120);
        check(CobblemonMechanics.move("rocktomb").accuracy()==95);
        check(CobblemonMechanics.move("spectralthief").power()==90);
        check(CobblemonMechanics.move("geomancy").boosts().equals(Map.of("spa",2,"spd",2,"spe",2)));
        check(!MaestroOwnMoveAudit.completelySimulated("uturn"));
    }
    public static void main(String[] args)throws Exception {
        MoveAttestationRegression.inventory();
        BlockedHitRegression.run();
        HealingRecoilRegression.run();
        RecoilContinuationRegression.run();
        PivotOutcomeRegression.run();
        AccuracyMissRegression.run();
        ResidualRecoveryRegression.run();
        SandstormRegression.run();
        WeatherSuppressionRegression.run();
        WeatherSpeedRegression.run();
        UtilityUmbrellaRegression.run();
        WeatherAbilityRegression.run();
        ToxicEntryRegression.run();
        RosterAbilityRegression.run();
        DownloadEntryRegression.run();
        WeatherEntryRegression.run();
        ColdWeatherRegression.run();
        WeatherSetterRegression.run();
        OwnMegaYRegression.run();
        OwnZMoveRegression.run();
        StatusSpeedRegression.run();
        PsychicTerrainRegression.run();
        ModifierCompositionRegression.run();
        ownMechanicsExecutionAudit();
        maestroWholeRosterCoverage();
        calyrexMoveCoverage();
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
