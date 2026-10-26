package com.maestroai.training;

import java.util.*;

/** Phase 6.10 owning-request, fresh-choice and privacy adversarial tests. */
final class NativeOwnActionEvidenceRegression {
    private NativeOwnActionEvidenceRegression(){}
    private static int checks;
    private static final UUID OWN=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static void check(boolean ok,String what){
        if(!ok)throw new AssertionError("Phase 6.10: "+what);
        checks++;
    }
    private static void reject(Runnable action,String what){
        try {action.run();}
        catch(IllegalArgumentException ex){
            check("Phase 6.10 native own-move evidence unavailable".equals(ex.getMessage())
                    && !ex.getMessage().contains("PRIVATE"),what+" privacy safe");
            return;
        }
        throw new AssertionError("Phase 6.10 accepted "+what);
    }
    private static String raw(){
        return "|request|{"
            +"\"side\":{\"id\":\"p1\",\"pokemon\":[{"
            +"\"details\":\"Genesect, "+OWN+", L50\",\"ident\":\"p1: Own\","
            +"\"active\":true,\"condition\":\"100/100\",\"ability\":\"Download\","
            +"\"types\":[\"bug\",\"steel\"],\"baseTypes\":[\"bug\",\"steel\"],"
            +"\"stats\":{\"atk\":90,\"def\":90,\"spa\":90,\"spd\":90,\"spe\":90},"
            +"\"item\":\"\",\"terastallized\":\"\","
            +"\"commanding\":false,\"reviving\":false}]},"
            +"\"active\":[{\"trapped\":false,\"moves\":["
            +"{\"move\":\"Tackle\",\"id\":\"tackle\",\"pp\":10,"
            +"\"maxpp\":20,\"target\":\"normal\",\"disabled\":false},"
            +"{\"move\":\"Scratch\",\"id\":\"scratch\",\"pp\":0,"
            +"\"maxpp\":20,\"target\":\"normal\",\"disabled\":true}]}]}";
    }
    private static final class Harness {
        final Object battle=new Object(),actor=new Object(),token=new Object();
        final OwnRequestTransport ledger=new OwnRequestTransport();
        final OwnRequestTransport.Request parsed=OwnRequestJson.parse(raw());
        NativeOwnActionEvidence witness;
        OwnRequestTransport.Lease lease;
        Harness(){
            ledger.owner(battle,actor,"p1");
            ledger.dispatched(battle,actor,"p1",token,()->parsed);
            witness=NativeOwnActionEvidence.capture(
                    battle,actor,token,"p1",0,raw(),parsed,"1.7.3+1.21.1");
            ledger.turnDispatched(battle,1);
            lease=ledger.beginChoice(battle,actor,token,OWN,false,()->true);
            check(lease.current(),"current native owner choice lease");
        }
        NativeOwnActionEvidence.InstalledView view(){
            return new NativeOwnActionEvidence.InstalledView(
                    battle,actor,token,"p1",OWN,1,List.of("tackle"),false);
        }
        NativeOwnActionEvidence.Review attest(){
            return witness.attest(lease,view());
        }
    }
    static void run(){
        var accepted=new Harness();
        var proof=accepted.attest();
        check(proof.currentOwnMoveAgreement() && proof.runtimeOwnMovesObserved()
                && proof.observedMoveCount()==1 && proof.ownTurn()==1
                && !proof.completeLegalSetProven()
                && !proof.opponentActionsKnown()
                && !proof.shadowSearchAuthorized()
                && !proof.tacticalDecisionAuthorized()
                && !proof.livePromotionAuthorized(),
                "native and owning request agree on ordinary enabled moves only");
        check(proof.moveFingerprint().equals(
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle"))),
                "own move set is canonical and never includes disabled/zero PP move");
        check(!proof.toString().contains(OWN.toString())
                && !proof.toString().contains("PRIVATE"),
                "own UUID, raw requests and nicknames redacted");
        var b=new ReadOnlyShadowObservationBridge();
        b.observe(accepted.battle,
                OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        check(b.observeOwnActionEvidence(accepted.battle,proof)
                && b.witnessedOwnMoveCount()==1,
                "6.9 read-only bridge records currently attested own move count");
        check(!b.observeOwnActionEvidence(new Object(),proof),
                "6.9 rejects foreign battle object");
        reject(()->accepted.attest(),"attested request is one-shot");
        accepted.ledger.endChoice();
        check(!proof.currentOwnMoveAgreement() && !proof.runtimeOwnMovesObserved(),
                "own action receipt expires as soon as choice closes");
        check(!b.observeOwnActionEvidence(accepted.battle,proof),
                "6.9 refuses stale own receipt");

        var tokenSwap=new Harness();
        reject(()->tokenSwap.witness.attest(tokenSwap.lease,
                new NativeOwnActionEvidence.InstalledView(
                    tokenSwap.battle,tokenSwap.actor,new Object(),"p1",OWN,1,
                    List.of("tackle"),false)),"installed request identity swap");
        check(tokenSwap.witness.withdrawn(),"token substitution irrevocably rejects");
        var actorSwap=new Harness();
        reject(()->actorSwap.witness.attest(actorSwap.lease,
                new NativeOwnActionEvidence.InstalledView(
                    actorSwap.battle,new Object(),actorSwap.token,"p1",OWN,1,
                    List.of("tackle"),false)),"foreign actor");
        var battleSwap=new Harness();
        reject(()->battleSwap.witness.attest(battleSwap.lease,
                new NativeOwnActionEvidence.InstalledView(
                    new Object(),battleSwap.actor,battleSwap.token,"p1",OWN,1,
                    List.of("tackle"),false)),"different battle");
        var sideSwap=new Harness();
        reject(()->sideSwap.witness.attest(sideSwap.lease,
                new NativeOwnActionEvidence.InstalledView(
                    sideSwap.battle,sideSwap.actor,sideSwap.token,"p2",OWN,1,
                    List.of("tackle"),false)),"opposing side");
        var uuidSwap=new Harness();
        reject(()->uuidSwap.witness.attest(uuidSwap.lease,
                new NativeOwnActionEvidence.InstalledView(
                    uuidSwap.battle,uuidSwap.actor,uuidSwap.token,"p1",
                    UUID.randomUUID(),1,List.of("tackle"),false)),
                "wrong current own Pokémon");
        var wrongTurn=new Harness();
        reject(()->wrongTurn.witness.attest(wrongTurn.lease,
                new NativeOwnActionEvidence.InstalledView(
                    wrongTurn.battle,wrongTurn.actor,wrongTurn.token,"p1",OWN,2,
                    List.of("tackle"),false)),"stale/mismatched source turn");
        var missingAction=new Harness();
        reject(()->missingAction.witness.attest(missingAction.lease,
                new NativeOwnActionEvidence.InstalledView(
                    missingAction.battle,missingAction.actor,missingAction.token,"p1",OWN,1,
                    List.of(),false)),"installed moves missing");
        var extraAction=new Harness();
        reject(()->extraAction.witness.attest(extraAction.lease,
                new NativeOwnActionEvidence.InstalledView(
                    extraAction.battle,extraAction.actor,extraAction.token,"p1",OWN,1,
                    List.of("tackle","scratch"),false)),
                "request-disabled action not native-usable");
        var forgedNative=new Harness();
        reject(()->forgedNative.witness.attest(forgedNative.lease,
                new NativeOwnActionEvidence.InstalledView(
                    forgedNative.battle,forgedNative.actor,forgedNative.token,"p1",OWN,1,
                    List.of("tackle","tackle"),false)),"duplicate installed move IDs");
        var orderMismatch=new Harness();
        reject(()->orderMismatch.witness.attest(orderMismatch.lease,
                new NativeOwnActionEvidence.InstalledView(
                    orderMismatch.battle,orderMismatch.actor,orderMismatch.token,"p1",OWN,1,
                    List.of("scratch"),false)),"native/request move mismatch");
        var trapMismatch=new Harness();
        reject(()->trapMismatch.witness.attest(trapMismatch.lease,
                new NativeOwnActionEvidence.InstalledView(
                    trapMismatch.battle,trapMismatch.actor,trapMismatch.token,"p1",OWN,1,
                    List.of("tackle"),true)),"native/request trapped mismatch");
        var expired=new Harness();
        expired.ledger.endChoice();
        reject(()->expired.attest(),"own lease revoked before observation");
        var sourceUpdated=new Harness();
        sourceUpdated.ledger.turnDispatched(sourceUpdated.battle,2);
        reject(()->sourceUpdated.attest(),"future turn invalidates receipt");
        var dispatchSubstituted=new Harness();
        // A valid-looking alternative request cannot be inserted into the
        // ledger while retaining a previously minted choice lease.
        var different=OwnRequestJson.parse(raw().replace("\"pp\":10","\"pp\":9"));
        dispatchSubstituted.ledger.dispatched(dispatchSubstituted.battle,
                dispatchSubstituted.actor,"p1",new Object(),()->different);
        reject(()->dispatchSubstituted.attest(),"displaced owning dispatch");

        var ownParsed=OwnRequestJson.parse(raw());
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw(),ownParsed,"unverified-version"),
                "mismatched Cobblemon source pin");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p2",0,raw(),ownParsed,"1.7.3+1.21.1"),
                "wrong own side");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw().replace("\"trapped\":false,",""),
                ownParsed,"1.7.3+1.21.1"),
                "no explicit trapped source data");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw().replace("\"trapped\":false",
                    "\"maybeTrapped\":true,\"trapped\":false"),
                ownParsed,"1.7.3+1.21.1"),
                "uncertain native trapped mode");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw().replace("\"trapped\":false",
                    "\"canMegaEvo\":true,\"trapped\":false"),
                ownParsed,"1.7.3+1.21.1"),
                "unsupported gimmick choice");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw().replace("\"id\":\"tackle\"",
                    "\"id\":\"PRIVATE_SECRET\""),
                ownParsed,"1.7.3+1.21.1"),
                "private data cannot be rendered in errors");
        reject(()->NativeOwnActionEvidence.capture(new Object(),new Object(),
                new Object(),"p1",0,raw().replace("\"active\":[",
                    "\"forceSwitch\":[true],\"active\":["),
                ownParsed,"1.7.3+1.21.1"),
                "forced replacement is not an ordinary move set");

        var denied=new Harness();
        denied.witness.withdraw();
        reject(()->denied.attest(),"explicitly withdrawn own evidence");
        System.out.println("Phase 6.10 own native evidence PASS: "+checks+" checks");
    }
}
