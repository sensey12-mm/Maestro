package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Phase 6.2 self-contained offline synthetic provenance/authority regressions. */
final class OfflineShadowProvenanceGateRegression {
    private OfflineShadowProvenanceGateRegression() {}
    private static int checks;
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError("Phase 6.2: " + reason);
        checks++;
    }
    private static void reject(Runnable action, String reason) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Phase 6.2 should reject: " + reason);
    }
    private static final String OWN = "update\n|switch|p1a: Own|Genesect, L50|100/100";
    private static final String FOE = "update\n|switch|p2a: Opp|synthetic, L50|100/100";
    private static final String TURN = "update\n|turn|1";
    private static final String REPLY = "update\n|move|p2a: Opp|Scratch|p1a: Own";
    private static final List<String> HISTORY = List.of(OWN,FOE,TURN,REPLY);
    private static BattleSnapshot.Combatant self() {
        return new BattleSnapshot.Combatant("genesect",1,List.of("bug","steel"),
                Map.of(),List.of(),null,null,false);
    }
    private static BattleSnapshot.Combatant foe(List<String> replies) {
        return new BattleSnapshot.Combatant("synthetic",1,List.of(),
                Map.of(),replies,null,null,false);
    }
    private static BattleSnapshot snapshot(int turn, List<String> replies, List<String> moves) {
        return new BattleSnapshot(turn,self(),foe(replies),List.of(),List.of(),
                moves,List.of(),Map.of(),false,null);
    }
    private static List<ShadowMechanicsEvaluation.Plan> plans(String reply) {
        String ownAction="move:tackle";
        return List.of(new ShadowMechanicsEvaluation.Plan(ownAction,
                List.of(List.of(new ShadowMechanicsEvaluation.Turn(ownAction,reply,null))),
                "Phase 6.2 offline conditional fixture"));
    }
    private static OfflineShadowProvenanceGate.Input input(
            List<String> history,int turn,int appearance,List<String> actions) {
        return new OfflineShadowProvenanceGate.Input(history,
                OfflineShadowProvenanceGate.fixtureSha256(history,turn,"p1a","p2a",appearance,actions),
                turn,"p1a","p2a",appearance,actions);
    }
    static OfflineShadowTacticalAdapter.Request request(BattleSnapshot state) {
        var status=Known.of("none","synthetic status");
        var hp=Known.of(new PublicStateSearchAdapter.Interval(1,1),"synthetic public HP");
        var hazards=new Hazards(Map.of(),true,"synthetic field");
        var context=new Context(hp,hp,status,status,hazards,hazards,
                Map.of(),Map.of(),Set.of());
        var mechanics=new PublicStateSearchAdapter.PublicMechanics(
                Map.of(),Map.of(),Set.of(),false,false,false,0,
                new Evidence(context,Map.of(),Map.of()));
        var profiles=new TreeMap<String,CobblemonMechanics.Profile>();
        var stats=new TreeMap<String,Known<CobblemonMechanics.IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))
            stats.put(stat,Known.of(CobblemonMechanics.IntRange.exact(90),"synthetic stat"));
        for(String actor:List.of("self","opponent"))
            profiles.put(actor,new CobblemonMechanics.Profile(actor,
                    Known.of(50,"synthetic level"),
                    Known.of(CobblemonMechanics.IntRange.exact(150),"synthetic max HP"),
                    stats,Known.of(List.of(actor.equals("self")?"bug":"normal"),"synthetic types"),
                    Known.of(true,"source-conditional effects assumption")));
        return new OfflineShadowTacticalAdapter.Request(OfflineShadowTacticalAdapter.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,
                state,mechanics,profiles,plans("scratch"),Known.of(false,"synthetic order"),
                CobblemonMechanics.Critical.NON_CRITICAL,null,false,false,false);
    }
    static void run() {
        var snap=snapshot(1,List.of("scratch"),List.of("tackle"));
        var bound=input(HISTORY,1,1,List.of("move:tackle"));
        var receipt=OfflineShadowProvenanceGate.check(bound,snap,plans("scratch"));
        check(receipt.offlineFixtureMatched() && receipt.fixtureTurn()==1
                && receipt.appearanceOrdinal()==1 && receipt.ownActionCount()==1
                && receipt.observedReplyCount()==1,"first public appearance, own choice and reply matched");
        check(!receipt.sourceAuthenticated() && !receipt.liveTurnActorBound()
                && !receipt.installedLegalActionsAttested() && !receipt.uniqueOpponentIdentityProven()
                && !receipt.opponentResponseCalibrated() && !receipt.shadowTacticalReady()
                && !receipt.livePromotionAuthorized(),"historical hash grants zero authority");
        check(receipt.toString().equals(OfflineShadowProvenanceGate.check(bound,snap,plans("scratch")).toString()),
                "repeated processing deterministic");
        var integrated=OfflineShadowTacticalAdapter.inspectWithProvenance(request(snap),bound);
        check(integrated.fixture().offlineFixtureMatched()
                && !integrated.conditional().currentBattleBound()
                && !integrated.conditional().shadowSearchAuthorized()
                && !integrated.conditional().livePromotionAuthorized(),
                "Phase 4/5 integration stays offline and unpromoted");
        check(!integrated.toString().contains("Opp") && !integrated.toString().contains("Genesect"),
                "no player/actor names serialized");
        List<String> mutable=new ArrayList<>(HISTORY);
        var copy=input(mutable,1,1,List.of("move:tackle"));
        mutable.set(3,"update\n|move|p2a: Opp|Private Move|p1a: Own");
        check(OfflineShadowProvenanceGate.check(copy,snap,plans("scratch")).offlineFixtureMatched(),
                "request snapshots original immutable frames");

        var corrupted=new ArrayList<>(HISTORY);
        corrupted.set(3,"update\n|move|p2a: Opp|Hidden Move|p1a: Own");
        reject(() -> OfflineShadowProvenanceGate.check(
                new OfflineShadowProvenanceGate.Input(corrupted,bound.pinnedSha256(),1,
                        "p1a","p2a",1,List.of("move:tackle")),snap,plans("scratch")),
                "tampered fixture against frozen digest");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(HISTORY,2,1,List.of("move:tackle")),snap,plans("scratch")),
                "stale or future turn");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(HISTORY,1,2,List.of("move:tackle")),snap,plans("scratch")),
                "wrong appearance ordinal even under recomputed caller hash");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(HISTORY,1,1,List.of("move:swift")),snap,plans("scratch")),
                "forged legal action");
        reject(() -> OfflineShadowProvenanceGate.check(
                bound,snapshot(1,List.of("unseen"),List.of("tackle")),plans("scratch")),
                "unrevealed move attribution from other appearance");
        reject(() -> OfflineShadowProvenanceGate.check(
                bound,snapshot(1,List.of(),List.of("tackle")),plans("scratch")),
                "public witnessed moves must not be selectively removed");

        var privateRequest=new ArrayList<>(HISTORY);
        privateRequest.add("sideupdate\n|request|{\\\"private\\\":true}");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(privateRequest,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "private request, including recomputed attacker pin");
        var privateInUpdate=new ArrayList<>(HISTORY);
        privateInUpdate.add("update\n|request|{\\\"secret\\\":1}");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(privateInUpdate,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "private request masquerading as a spectator update");
        var leakedSplit=new ArrayList<>(HISTORY);
        leakedSplit.add("update\n|split|p2\n|move|p2a: Opp|Private|p1a: Own\n|move|p2a: Opp|Scratch|p1a: Own");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(leakedSplit,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "mixed private/public split withheld from narrow offline slice");
        var wrongSide=new ArrayList<>(HISTORY);
        wrongSide.set(3,"update\n|move|p3a: Opp|Scratch|p1a: Own");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(wrongSide,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "wrong-side actor");
        var transformed=new ArrayList<>(HISTORY);
        transformed.add("update\n|-transform|p2a: Opp|p1a: Own");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(transformed,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "Transform identity cannot be silently reattributed");
        var disguised=new ArrayList<>(HISTORY);
        disguised.add("update\n|replace|p2a: Opp|Ditto, L50");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(disguised,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "Illusion reveal cannot be silently reattributed");

        var repeated=new ArrayList<>(HISTORY);
        repeated.add("update\n|switch|p2a: Opp|synthetic, L50|100/100");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(repeated,1,2,List.of("move:tackle")),snap,plans("scratch")),
                "same nickname/species reentry remains uncertain");
        var another=new ArrayList<>(HISTORY);
        another.add("update\n|switch|p2a: New|synthetic, L50|100/100");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(another,1,2,List.of("move:tackle")),snap,plans("scratch")),
                "prior-appearance move cannot be reused after a switch");
        reject(() -> OfflineShadowProvenanceGate.check(bound,
                snapshot(1,List.of("scratch"),List.of("tackle","swift")),plans("scratch")),
                "unbound extra legal option cannot be silently certified");
        var otherTurn=new ArrayList<>(HISTORY);
        otherTurn.add("update\n|turn|3");
        reject(() -> OfflineShadowProvenanceGate.check(
                input(otherTurn,1,1,List.of("move:tackle")),snap,plans("scratch")),
                "nonmonotonic future turn");
        var forgedHp=snapshot(1,List.of("scratch"),List.of("tackle"));
        var altered=new BattleSnapshot(1,forgedHp.self(),
                new BattleSnapshot.Combatant("synthetic",.25,List.of(),Map.of(),
                    List.of("scratch"),null,null,false),
                List.of(),List.of(),List.of("tackle"),List.of(),Map.of(),false,null);
        reject(() -> OfflineShadowProvenanceGate.check(bound,altered,plans("scratch")),
                "forged exact HP against public HP observation");
        var injected=new BattleSnapshot(1,forgedHp.self(),
                new BattleSnapshot.Combatant("synthetic",1,List.of("normal"),Map.of(),
                    List.of("scratch"),null,null,false),
                List.of(),List.of(),List.of("tackle"),List.of(),Map.of(),false,null);
        reject(() -> OfflineShadowProvenanceGate.check(bound,injected,plans("scratch")),
                "unverified opponent typing cannot become public mechanics facts");
        var nextRequest=new PublicTransitionState.FollowUp("self",List.of("tackle"),
                List.of(),false,false,false,Map.of(),"forged future request");
        var future=new ShadowMechanicsEvaluation.Plan("move:tackle",
                List.of(List.of(new ShadowMechanicsEvaluation.Turn("move:tackle","scratch",null),
                        new ShadowMechanicsEvaluation.Turn("move:tackle","scratch",nextRequest))),
                "synthetic future");
        reject(() -> OfflineShadowProvenanceGate.check(bound,snap,List.of(future)),
                "future choice needs separately bound subsequent request");

        System.out.println("Phase 6.2 offline provenance PASS: " + checks + " synthetic checks");
    }
}
