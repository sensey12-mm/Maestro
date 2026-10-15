package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;

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
    /** Positive production evaluator path, under explicitly justified synthetic scopes.
     * No private opponent request, battle result, or runtime pack-override proof.
     */
    private static void positivePath() {
        var publicStream=new PublicProtocolEvidence();
        publicStream.update(List.of(
                "update\n|gen|9\n|gametype|singles\n|start\n"
                    +"|switch|p1a: Own|Mew, L100|100/100\n"
                    +"|switch|p2a: Foe|Mew, L100|100/100\n|turn|1",
                "update\n|-enditem|p2a: Foe|Sitrus Berry\n|-ability|p2a: Foe|Run Away",
                "update\n|move|p2a: Foe|Tackle|p1a: Own",
                "update\n|move|p2a: Foe|Quick Attack|p1a: Own"));
        var observations=new PublicMechanicsEvidence(species -> null);
        observations.update(publicStream);
        var fieldObserver=new PublicFieldEvidence();
        var field=fieldObserver.update(publicStream,observations);
        yes(field.turn()==1 && field.unknown().isEmpty() && field.allActorsCovered(),
                "complete public field observations required to compare");
        var ownSeen=observations.observation("p1a");
        var foeSeen=observations.observation("p2a");
        yes(foeSeen!=null && foeSeen.item().known() && foeSeen.ability().known()
                && foeSeen.item().value().equals("none")
                && foeSeen.ability().value().equals("runaway"),
                "public opponent item/ability must be explicitly observed");
        var yesScope=Known.of(true,"explicit synthetic verified engine scope");
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(ownSeen,
                        Known.of("none","own current item"),Known.of("runaway","own current ability"),yesScope),
                "opponent",new ScopedExecution.Binding(foeSeen,foeSeen.item(),foeSeen.ability())),
                ExecutionCoverage.Scopes.all(yesScope));
        Map<String,Known<CobblemonMechanics.IntRange>> stats=new TreeMap<>();
        for(String k:List.of("atk","def","spa","spd","spe"))
            stats.put(k,Known.of(CobblemonMechanics.IntRange.exact(220),"synthetic bounded stats"));
        var own=new CobblemonMechanics.Profile("self",Known.of(100,"own level"),
                Known.of(CobblemonMechanics.IntRange.exact(300),"own max HP"),stats,
                Known.of(List.of("normal"),"own types"),yesScope);
        var foe=new CobblemonMechanics.Profile("opponent",Known.of(100,"public level fixture"),
                Known.of(CobblemonMechanics.IntRange.exact(300),"public bounded HP fixture"),stats,
                Known.of(List.of("normal"),"public types fixture"),yesScope);
        var none=new Hazards(Map.of(),true,"synthetic no hazard certificate");
        var context=new Context(Known.of(new Interval(1,1),"own HP"),
                Known.of(new Interval(1,1),"public HP"),
                Known.of("none","own status"),Known.of("none","public status"),
                none,none,Map.of(),Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),Map.of(),Set.of(),false,false,false,1,
                new Evidence(context,Map.of(),Map.of()));
        var neutral=new BattleSnapshot.Combatant("Mew",1.0,List.of("normal"),
                Map.of("atk",0,"def",0,"spa",0,"spd",0,"spe",0),
                List.of("tackle","quickattack"),null,null,false);
        var scenario=new BattleSnapshot(1,neutral,neutral,List.of(),List.of(),
                List.of("tackle","quickattack"),List.of(),Map.of(),false,null);
        var audit=PublicReplyCoverage.evaluate(scenario,mechanics,
                Map.of("self",own,"opponent",foe),execution,publicStream,"p2a",
                field.trickRoom(),CobblemonMechanics.Critical.NON_CRITICAL);
        yes(audit.compared() && audit.allCandidates() && audit.replyCount()==2
                && audit.candidateCount()==2 && audit.result()!=null,
                "valid public witness did not reach production evaluator");
        yes(audit.result().candidateCoverageComplete()
                && audit.result().rankings().size()==2
                && audit.result().rankings().stream().allMatch(r->r.lines().size()>=2),
                "every legal root action must evaluate both witnessed replies");
        yes(audit.result().rankings().stream().allMatch(r->r.utility().known()),
                "explicit ordinary-source conditions failed to produce bounded exchange");
        yes(audit.safeLine().contains("responseProbabilities=NONE")
                && !audit.safeLine().contains("quickattack")
                && !audit.safeLine().contains("Foe"),
                "successful audit leaked opponent reply or invented probability");
        fieldObserver.reset();
        var expired=PublicReplyCoverage.evaluate(scenario,mechanics,
                Map.of("self",own,"opponent",foe),execution,publicStream,"p2a",
                Known.of(false,"prior field record"),CobblemonMechanics.Critical.NON_CRITICAL);
        yes(!expired.compared() && expired.reasons().contains("UNSUPPORTED_COMPARISON"),
                "reset field cert continued to rank previous turn");
    }

    public static void run() {
        positivePath();
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
        var fainted=protocol("|move|p2a: Foe|Tackle|p1a: Own",
                "|faint|p2a: Foe");
        yes(PublicReplyCoverage.currentReplies(fainted,"p2a").isEmpty(),
                "fainted public active retained a presently executable move");
        yes(!audit(current,ordinary,fainted).compared(),
                "fainted opponent generated executable conditional reply");
        var replaced=protocol("|move|p2a: Foe|Tackle|p1a: Own",
                "|faint|p2a: Foe",
                "|switch|p2a: New Foe|Mew, L100|100/100",
                "|move|p2a: New Foe|Quick Attack|p1a: Own");
        yes(PublicReplyCoverage.currentReplies(replaced,"p2a").equals(List.of("quickattack")),
                "new opponent appearance inherited an old fainted move");
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

        // Actual owning transport: never parse or accept a foreign actor.
        var ownLedger=new OwnRequestTransport();
        Object battle=new Object(), actor=new Object(), foreign=new Object(), token=new Object();
        ownLedger.owner(battle,actor,"p1");
        var validId=UUID.fromString("00000000-0000-0000-0000-000000000001");
        var owned=new OwnRequestEvidence.Sample(validId,"p1: Own","300/300",true,"Run Away",
                List.of("normal"));
        var ownedPokemon=new OwnRequestTransport.Pokemon(owned,100,
                Map.of("atk",220,"def",220,"spa",220,"spd",220,"spe",220),
                "none",List.of("normal"),"",false);
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
                List.of(ownedPokemon));
        var parserCalls=new int[]{0};
        ownLedger.dispatched(battle,foreign,"p2",token,()->{
            parserCalls[0]++; return request;
        });
        yes(parserCalls[0]==0,"foreign actor was allowed to read owning request");
        ownLedger.dispatched(battle,actor,"p1",token,()->{
            parserCalls[0]++; return request;
        });
        yes(parserCalls[0]==1,"own authorized dispatch was not parsed exactly once");
        ownLedger.turnDispatched(battle,1);
        yes(ownLedger.observedTurnAllowed(0),"source-covered queued turn transition refused");
        var ownLease=ownLedger.beginChoice(battle,actor,token,validId,false,
                ()->true);
        yes(ownLease.current() && ownLease.turn()==1,"own request did not bind current choice");
        var noPrivate=ownLedger.evidence(ownLease,"self",validId,false,false);
        yes(noPrivate.fresh().known() && noPrivate.unknown().contains("PACK_EFFECT_SCOPE_UNKNOWN"),
                "own-only receipt guessed effective modifiers");
        ownLedger.endChoice();
        yes(!ownLease.current(),"consumed own request lease resurrected");
        ownLedger.dispatched(battle,actor,"p1",token,()->request);
        var duplicate=ownLedger.beginChoice(battle,actor,token,validId,false,()->true);
        yes(!duplicate.current(),"duplicate own dispatch reused after choice");
        ownLedger.owner(new Object(),actor,"p1");
        yes(!ownLease.current(),"foreign battle overwrote old own lease");
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
