package com.maestroai.training;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Phase 7.3: synthetic source/lifetime/provenance regressions, NOT Minecraft. */
public final class LegacyChoiceCaptureRegression {
    private LegacyChoiceCaptureRegression() {}
    private static int checks;
    private static void check(boolean pass,String reason) {
        if(!pass)throw new AssertionError("Phase 7.3: "+reason);
        checks++;
    }
    private static void reject(Runnable op,String reason) {
        boolean rejected=false;
        try {op.run();}catch(RuntimeException expected){rejected=true;}
        check(rejected,reason);
    }
    private static final String ARTIFACT="a".repeat(64);
    private static final String HISTORY1="update\n|switch|p1a: Own|Mew, L100|100/100\n"
            +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1";
    private static final String HISTORY2="update\n|turn|2";

    private record Owned(OwnRequestTransport ledger,OwnRequestTransport.ChoiceProof proof,
                         Object battle,Object actor,UUID active) {}
    private static final UUID BATTLE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static Owned choice() { return choice(BATTLE_ID); }
    private static Owned choice(UUID battleId) {
        Object battle=new Object(),actor=new Object(),request=new Object();
        UUID active=UUID.fromString("10000000-0000-0000-0000-000000000001");
        var sample=new OwnRequestEvidence.Sample(active,"p1: Own","100/100",true,
                "pressure",List.of("normal"));
        var pokemon=new OwnRequestTransport.Pokemon(sample,100,Map.of("atk",100),
                "leftovers",List.of("normal"),"normal",false);
        var receipt=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
                List.of(pokemon));
        var ledger=new OwnRequestTransport();
        ledger.owner(battle,actor,"p1");
        ledger.dispatched(battle,actor,"p1",request,()->receipt);
        ledger.turnDispatched(battle,1);
        var lease=ledger.beginChoice(battle,actor,request,active,false,()->true);
        var proof=ledger.choiceProof(lease,battleId);
        check(proof!=null && proof.current() && proof.turn()==1,
                "actual owning-dispatch lease proves current turn");
        check(proof.requestDigest().matches("[0-9a-f]{64}"),
                "salted own-request digest is well formed");
        check(!proof.toString().contains(active.toString()),"own proof never logs UUID");
        return new Owned(ledger,proof,battle,actor,active);
    }
    private static List<String> history(){return List.of(HISTORY1);}
    private static Phase7Benchmark.Scenario syntheticScenario(){
        var self=new BattleSnapshot.Combatant("synthetic-own",1,List.of(),Map.of(),
                List.of(),null,null,false);
        var foe=new BattleSnapshot.Combatant("synthetic-opponent",
                BattleSnapshot.UNKNOWN_HP_FRACTION,List.of(),Map.of(),
                List.of(),null,null,false);
        var reserve=new BattleSnapshot.Combatant("synthetic-reserve",1,List.of(),
                Map.of(),List.of(),null,null,false);
        var snapshot=new BattleSnapshot(1,self,foe,List.of(reserve),List.of(),
                List.of("attack","guard"),List.of("slot2"),Map.of(),false,null);
        SortedMap<String,Phase7Benchmark.Criterion> criteria=new TreeMap<>();
        criteria.put("move:attack",new Phase7Benchmark.Criterion(.5,.2));
        criteria.put("move:guard",new Phase7Benchmark.Criterion(.7,.1));
        criteria.put("switch:slot2",new Phase7Benchmark.Criterion(.4,.3));
        return new Phase7Benchmark.Scenario("synthetic-captured-choice",1,
                snapshot,criteria,new Phase7Benchmark.Evidence(true,true,true,1),null);
    }
    public static void run() {
        var own=choice();
        var disabled=new LegacyChoiceCapture(false,ARTIFACT);
        var battleId=UUID.fromString("20000000-0000-0000-0000-000000000001");
        check(disabled.observe(battleId,own.proof(),history(),"move:attack").status()
                        ==LegacyChoiceCapture.Status.DISABLED
                && disabled.receipts().isEmpty(),"opt-in default is inactive");

        var recorder=new LegacyChoiceCapture(true,ARTIFACT);
        var accepted=recorder.observe(battleId,own.proof(),history(),"move:attack");
        check(accepted.status()==LegacyChoiceCapture.Status.CAPTURED,
                "valid synthetic public/own evidence accepted");
        check(recorder.receipts().size()==1
                && recorder.receipts().getFirst()==accepted.receipt(),
                "bounded in-memory capture returns original receipt");
        var receipt=accepted.receipt();
        check(receipt.turn()==1 && receipt.action().equals("move:attack")
                && receipt.origin().equals("LEGACY_CHOOSE_RETURN"),"origin and exact selection");
        check(receipt.captureId().matches("[a-f0-9-]{36}"),"random opaque capture token");
        check(receipt.publicSha256().equals(LegacyChoiceCapture.publicDigest(
                PublicProtocolFilter.publicEvents(history()))),"public-only digest");
        check(receipt.publicSha256().equals(Phase7LegacyImport.publicDigest(
                PublicProtocolFilter.publicEvents(history()))),"exact Phase 7.2 digest compatibility");
        check(receipt.ownRequestSha256().equals(own.proof().requestDigest()),
                "own request proof is bound without private content");
        check(receipt.artifactSha256().equals(ARTIFACT),"pinned installed artifact identity");
        check(!receipt.toString().contains("move:attack")
                && !accepted.toString().contains("Mew"),"implicit object string redacted");
        String export=receipt.safeLine();
        check(export.contains("action=move:attack") && export.contains("custody=REVIEW_REQUIRED"),
                "explicit opt-in output marks custody unverified");
        check(!export.contains("Opponent") && !export.contains("100/100")
                && !export.contains(battleId.toString())
                && !export.contains(own.active().toString())
                && !export.contains("leftovers"),"diagnostic excludes private payload, UUID and item");
        check(recorder.observe(battleId,own.proof(),history(),"move:attack").status()
                ==LegacyChoiceCapture.Status.DUPLICATE_TURN,"one choice per battle turn");

        var scenario=syntheticScenario();
        AtomicInteger invoked=new AtomicInteger();
        var privateHistory=history();
        check(Phase7LegacyImport.importObserved(receipt,battleId,privateHistory,scenario,null)
                .status()==Phase7LegacyImport.Status.NO_CUSTODY_REVIEW,
                "runtime receipt alone cannot forge trusted importer acceptance");
        // Independent reviewed-original stub deliberately synthetic: check the
        // pinned origin receipt's own request, file bytes and public snapshot.
        var reviewed=Phase7LegacyImport.importObserved(receipt,battleId,privateHistory,
                scenario,request->{
                    invoked.incrementAndGet();
                    return request.captureId().equals(receipt.captureId())
                            && request.artifactSha256().equals(ARTIFACT)
                            && request.ownRequestSha256().equals(receipt.ownRequestSha256())
                            && request.publicSha256().equals(receipt.publicSha256())
                            && request.rawHistorySha256().equals(
                                    Phase7LegacyImport.sha256(privateHistory));
                });
        check(reviewed.accepted() && reviewed.legacy().action().equals("move:attack"),
                "synthetic independent-custody stub reaches offline importer");
        check(invoked.get()==1,"offline custody review actually invoked");
        var comparison=Phase7LegacyImport.compare(scenario,reviewed,3L,(s,seed)->"move:guard");
        check(Boolean.FALSE.equals(comparison.shadowAgreement())
                && comparison.baselineAction()!=null,
                "offline comparison against Shadow and baseline");
        check(!comparison.json().contains("Mew") && !comparison.json().contains("100/100")
                && comparison.json().contains("no-win-rate"),
                "offline report never exports captured identities or win claims");

        var editedHistory=List.of(HISTORY1.replace("Mew, L100","Arceus, L100"));
        check(!Phase7LegacyImport.importObserved(receipt,battleId,editedHistory,scenario,
                request->true).accepted(),"public history mutation rejected by importer");
        check(!Phase7LegacyImport.importObserved(receipt,battleId,privateHistory,scenario,
                request->false).accepted(),"custody reviewer denial upheld");
        var mismatchScenario=new Phase7Benchmark.Scenario(scenario.id(),scenario.version(),
                new BattleSnapshot(2,scenario.snapshot().self(),scenario.snapshot().opponent(),
                        scenario.snapshot().selfBench(),List.of(),scenario.snapshot().legalMoves(),
                        scenario.snapshot().legalSwitches(),Map.of(),false,null),
                scenario.criteria(),new Phase7Benchmark.Evidence(true,true,true,2),null);
        check(Phase7LegacyImport.importObserved(receipt,battleId,privateHistory,mismatchScenario,
                request->true).status()==Phase7LegacyImport.Status.STALE_TURN,
                "stale turn or incompatible scenario never imports");

        var noArtifact=new LegacyChoiceCapture(true,"UNKNOWN");
        check(noArtifact.observe(battleId,own.proof(),history(),"move:attack").status()
                        ==LegacyChoiceCapture.Status.UNKNOWN_ARTIFACT,
                "unverified installed artifact cannot claim authentic receipt");
        var invalid=new LegacyChoiceCapture(true,ARTIFACT);
        check(invalid.observe(battleId,own.proof(),history(),"switch:"
                +own.active()).status()==LegacyChoiceCapture.Status.INVALID_ACTION,
                "raw Pokémon UUID cannot be exported as switch action");
        check(invalid.observe(battleId,own.proof(),history(),"move:SECRET_PRIVATE")
                .status()==LegacyChoiceCapture.Status.INVALID_ACTION,
                "unbounded/unknown action identifiers rejected");
        check(invalid.receipts().isEmpty(),"rejected actions never persisted");

        var split=new LegacyChoiceCapture(true,ARTIFACT);
        check(split.observe(battleId,own.proof(),List.of(
                "update\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Opponent|Mew, L100|100/100\n|split|p2\n"
                +"|-damage|p2a: PRIVATE|1/1"),"move:attack").status()
                ==LegacyChoiceCapture.Status.INCOMPLETE_PUBLIC_ENVELOPE,
                "incomplete spectator split blocks capture");
        var altered=new LegacyChoiceCapture(true,ARTIFACT);
        check(altered.observe(battleId,own.proof(),List.of(
                HISTORY1.replace("|turn|1","|turn|2")),"move:attack").status()
                ==LegacyChoiceCapture.Status.INCOMPLETE_PUBLIC_ENVELOPE,
                "missing earlier turn cannot certify turn 2");
        var replay=new LegacyChoiceCapture(true,ARTIFACT);
        check(replay.observe(battleId,own.proof(),List.of(
                "update\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Opponent|Mew, L100|100/100\n"
                +"|replace|p2a: Opponent|Zoroark, L100\n|turn|1"),
                "move:attack").status()==LegacyChoiceCapture.Status.AMBIGUOUS_PUBLIC_IDENTITY,
                "Illusion appearance cannot be treated as resolved");
        check(replay.receipts().isEmpty(),"ambiguous evidence never emits action");
        var unframed=new LegacyChoiceCapture(true,ARTIFACT);
        check(unframed.observe(battleId,own.proof(),List.of(
                "|switch|p1a: Own|Mew, L100|100/100\n|turn|1"),
                "move:attack").status()==LegacyChoiceCapture.Status.INCOMPLETE_PUBLIC_ENVELOPE,
                "unframed public history rejected");
        var wrongTurn=new LegacyChoiceCapture(true,ARTIFACT);
        check(wrongTurn.observe(battleId,own.proof(),
                List.of("update\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1",
                "update\n|move|p1a: Own|Attack|p2a: Opponent"),
                "move:attack").status()==LegacyChoiceCapture.Status.STALE_PUBLIC_TURN,
                "public history after turn announcement refuses retroactive receipt");

        own.ledger().endChoice();
        check(!own.proof().current(),"lease expires immediately after live action");
        var expired=new LegacyChoiceCapture(true,ARTIFACT);
        check(expired.observe(battleId,own.proof(),history(),"move:attack").status()
                ==LegacyChoiceCapture.Status.EXPIRED_OWN_CHOICE,
                "expired own-request receipt cannot certify action");
        check(expired.receipts().isEmpty(),"expired evidence leaves no retained capture");
        var changed=new LegacyChoiceCapture(true,ARTIFACT);
        var fresh=choice();
        var first=changed.observe(battleId,fresh.proof(),history(),"move:guard");
        check(first.status()==LegacyChoiceCapture.Status.CAPTURED,"fresh second ledger");
        // Mutate previous privately held history even without a new public turn.
        check(changed.observe(battleId,fresh.proof(),List.of(
                HISTORY1.replace("Opponent","Different")),"move:attack").status()
                ==LegacyChoiceCapture.Status.HISTORY_MUTATION,
                "same-turn rewritten public history permanently invalidates custody");
        check(changed.observe(battleId,fresh.proof(),history(),"move:attack").status()
                ==LegacyChoiceCapture.Status.HISTORY_MUTATION,
                "rewritten capture does not revive from restored input");
        // Switching to a new battle resets continuity, never reuses old receipts.
        var nextBattle=UUID.fromString("20000000-0000-0000-0000-000000000002");
        var secondOwner=choice(nextBattle);
        var next=changed.observe(nextBattle,
                secondOwner.proof(),history(),"switch:slot2");
        check(next.status()==LegacyChoiceCapture.Status.CAPTURED
                && changed.receipts().size()==1
                && changed.receipts().getFirst().action().equals("switch:slot2"),
                "new battle receives independent bounded capture");
        check(!next.receipt().captureId().equals(first.receipt().captureId()),
                "new capture ID avoids source battle UUID reuse");

        reject(()->new Phase7LegacyImport.ChoiceReceipt("synthetic","REPLAYED_MOVE",
                ARTIFACT,receipt.ownRequestSha256(),receipt.publicSha256(),
                Phase7Benchmark.stateFingerprint(scenario),1,receipt.action()),
                "replayed moves cannot impersonate selected Legacy action");
        System.out.println("Phase 7.3 synthetic choice-capture regressions PASS: "
                +checks+" checks (Minecraft captures: 0)");
    }
}
