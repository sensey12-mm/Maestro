package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Independent MaestroAI Phase 3.35 Shadow/owning evidence checks, NOT Minecraft. */
public final class AcceptanceGateRegression {
    private AcceptanceGateRegression() {}
    private static int checks;
    private static void yes(boolean condition, String message) {
        if (!condition) throw new AssertionError("Phase 3.35: " + message);
        checks++;
    }
    private static void rejects(Runnable action, String why) {
        boolean rejected=false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected=true; }
        yes(rejected,why);
    }
    private static PublicProtocolEvidence protocol(String... events) {
        List<String> raw=new ArrayList<>();
        raw.add("update\n|gen|9\n|gametype|singles\n|start\n"
            +"|switch|p1a: Own|Mew, L100|100/100\n"
            +"|switch|p2a: Foe|Mew, L100|100/100\n|turn|1");
        for(String event:events) raw.add("update\n"+event);
        var result=new PublicProtocolEvidence(); result.update(raw); return result;
    }
    private static BattleSnapshot snapshot(int turn,List<String> moves,List<String> switches) {
        var combatant=new BattleSnapshot.Combatant("Mew",1.0,List.of("normal"),Map.of(),
                List.of("tackle","quickattack"),null,null,false);
        return new BattleSnapshot(turn,combatant,combatant,List.of(),List.of(),moves,switches,
                Map.of(),false,null);
    }
    private static PublicReplyCoverage.Audit audit(BattleSnapshot snapshot,
                    PublicMechanics mechanics, PublicProtocolEvidence protocol) {
        return PublicReplyCoverage.evaluate(snapshot,mechanics,Map.of(),null,protocol,
                "p2a",Known.unknown(),CobblemonMechanics.Critical.BOTH);
    }
    public static void run() {
        String secret="SECRET_OPPONENT_HELD_ITEM_01234";
        var observed=protocol("|move|p2a: Foe|Tackle|p1a: Own",
                "|move|p2a: Foe|Quick Attack|p1a: Own");
        yes(observed.publicTurn()==1 && observed.framed(),"public turn from complete event");
        yes(PublicReplyCoverage.currentReplies(observed,"p2a").equals(List.of("quickattack","tackle")),
                "two real public replies");
        var ordinary=new PublicMechanics(Map.of(),Map.of(),Set.of(),false,false,false,1);
        var current=snapshot(1,List.of("tackle","quickattack"),List.of("reserve"));
        var noField=audit(current,ordinary,observed);
        yes(noField.candidateCount()==3 && noField.replyCount()==2
                && noField.reasons().contains("NO_CURRENT_FIELD_CERTIFICATE"),
                "all own candidates remain visible but unsupported without lease");
        yes(noField.safeLine().contains("responseProbabilities=NONE")
                && !noField.safeLine().contains("quickattack")
                && !noField.safeLine().contains("reserve"),
                "diagnostics expose only safe vocabulary");
        yes(audit(snapshot(2,current.legalMoves(),current.legalSwitches()),ordinary,observed)
                .reasons().contains("PUBLIC_TURN_MISMATCH"),"stale turn certified a new choice");
        var later=protocol("|move|p2a: Foe|Tackle|p1a: Own","|turn|2");
        yes(later.publicTurn()==2 && audit(current,ordinary,later)
                .reasons().contains("PUBLIC_TURN_MISMATCH"),"future spectator state used for prior own choice");
        var privatePacket=new PublicProtocolEvidence();
        privatePacket.update(List.of(
            "update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Foe|Mew, L100|100/100\n|turn|1",
            "update\n|move|p2a: Foe|Tackle|p1a: Own",
            "sideupdate\np2\n|move|p2a: Foe|Quick Attack|p1a: Own\n"+secret));
        yes(PublicReplyCoverage.currentReplies(privatePacket,"p2a").equals(List.of("tackle")),
                "private sideupdate added a hypothetical response");
        yes(!audit(current,ordinary,privatePacket).safeLine().contains(secret),
                "private payload leaked through diagnostic");
        var illusion=protocol("|replace|p2a: Foe|Zoroark, L100",
                "|move|p2a: Foe|Tackle|p1a: Own");
        yes(PublicReplyCoverage.currentReplies(illusion,"p2a").isEmpty()
                && !audit(current,ordinary,illusion).compared(),
                "Illusion appearance identity confused with prior public state");
        var transform=protocol("|-transform|p2a: Foe|p1a: Own",
                "|move|p2a: Foe|Tackle|p1a: Own");
        yes(PublicReplyCoverage.currentReplies(transform,"p2a").isEmpty(),
                "Transform used ordinary pre-transform response");
        var poisoned=protocol("|move|p2a: Foe|Tackle|p1a: Own");
        poisoned.update(List.of("update\n|turn|1"));
        yes(poisoned.publicTurn()==-1 && !audit(current,ordinary,poisoned).compared(),
                "non-append-only replay retained turn proof");
        poisoned.reset();
        yes(poisoned.publicTurn()==-1,"reset revived previous turn proof");
        var unframed=new PublicProtocolEvidence();
        unframed.update(List.of("|switch|p2a: Foe|Mew, L100|100/100","|turn|1"));
        yes(unframed.publicTurn()==-1,"unframed history certified public turn");

        var trap=new PublicMechanics(Map.of(),Map.of(),Set.of(),false,false,true,1);
        yes(audit(current,trap,observed).candidateCount()==2,
                "trapped voluntary switch incorrectly legal");
        var forced=new PublicMechanics(Map.of(),Map.of(),Set.of(),true,false,false,1);
        yes(audit(snapshot(1,List.of("tackle"),List.of("reserve")),forced,observed)
                .reasons().contains("FORCED_ACTION_UNSUPPORTED"),
                "single forced action scored as optional");
        yes(audit(current,forced,observed).reasons().contains("LEGAL_ROOT_UNVERIFIED"),
                "invalid forced request misreported or treated as discretionary");
        var replacement=new PublicMechanics(Map.of(),Map.of(),Set.of(),false,true,true,1);
        yes(audit(snapshot(1,List.of(),List.of("reserve")),replacement,observed)
                .reasons().contains("FORCED_ACTION_UNSUPPORTED"),
                "forced replacement treated as voluntary");
        yes(audit(snapshot(1,List.of(),List.of()),ordinary,observed)
                .reasons().contains("LEGAL_ROOT_UNVERIFIED"),
                "empty owning request generated candidates");

        var prereq=ShadowPrerequisiteDiagnostics.inspect(null,null,null,observed,null);
        yes(!prereq.preferred() && !prereq.sourceAndRegistryCurrent(),
                "missing prerequisite certified readiness");
        rejects(()->new ShadowPrerequisiteDiagnostics.Summary(false,false,false,false,false,
                1,1,true,Set.of(secret)),"untrusted diagnostic reason allowed");

        String digest="a".repeat(64);
        var observedHash=new String[]{digest};
        var active=new boolean[]{true};
        var lease=new RuntimeRegistryLease(digest,()->active[0],()->observedHash[0]);
        yes(lease.getAsBoolean(),"fresh registry proof refused");
        observedHash[0]="b".repeat(64);
        yes(!lease.getAsBoolean(),"registry mutation ignored");
        observedHash[0]=digest;
        yes(!lease.getAsBoolean(),"restoration revived expired proof");
        var fresh=new RuntimeRegistryLease(digest,()->active[0],()->digest);
        yes(fresh.getAsBoolean(),"separate new registry lease invalid");
        active[0]=false;
        yes(!fresh.getAsBoolean(),"context/choice expiry ignored");
        active[0]=true;
        yes(!fresh.getAsBoolean(),"old choice revived after new battle");

        yes(new ShadowMechanicsEvaluation.Plan("move:bulkup",List.of(
                    List.of(new ShadowMechanicsEvaluation.Turn("move:bulkup","tackle",null)),
                    List.of(new ShadowMechanicsEvaluation.Turn("move:bulkup","quickattack",null))),
                "public-two-reply-conditional").scenarios().size()==2,
                "setup reply paths not preserved independently");
        rejects(()->new ShadowMechanicsEvaluation.Plan("move:bulkup",
                    List.of(List.of(new ShadowMechanicsEvaluation.Turn("move:tackle","tackle",null))),
                "source"),"unmatched plan root accepted");
        System.out.println("MaestroAI Phase 3.35 acceptance prerequisite regressions PASS: "
                +checks+" checks (synthetic; live Shadow unchanged)");
    }
}
