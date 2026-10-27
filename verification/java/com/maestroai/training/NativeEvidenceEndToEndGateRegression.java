package com.maestroai.training;

import java.util.*;

/** Phase 6.21: cross-contract end-to-end safety and refusal matrix. */
final class NativeEvidenceEndToEndGateRegression {
    private NativeEvidenceEndToEndGateRegression(){}
    private static int checks;
    private static final UUID OWN=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BENCH=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static void check(boolean b,String what){
        if(!b)throw new AssertionError("Phase 6.21: "+what);
        checks++;
    }
    private static List<String> publicFrames(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> withSplit(){
        var result=new ArrayList<>(publicFrames());
        result.add("update\n|split|p1\n|-damage|p1a: Own|52/100\n"
                +"|-damage|p1a: Own|60/100");
        return List.copyOf(result);
    }
    private static String party(UUID id,boolean active) {
        return "{\"details\":\"Genesect, "+id+", L50\","
                +"\"ident\":\"p1: PRIVATE_ALIAS\",\"active\":"+active+","
                +"\"condition\":\"100/100\",\"ability\":\"Download\","
                +"\"types\":[\"bug\",\"steel\"],"
                +"\"baseTypes\":[\"bug\",\"steel\"],"
                +"\"stats\":{\"atk\":90,\"def\":90,\"spa\":90,\"spd\":90,\"spe\":90},"
                +"\"item\":\"\",\"terastallized\":\"\",\"commanding\":false,"
                +"\"reviving\":false}";
    }
    private static String request(){
        return "|request|{\"side\":{\"id\":\"p1\",\"pokemon\":["
                +party(OWN,true)+","+party(BENCH,false)+"]},"
                +"\"active\":[{\"trapped\":false,\"moves\":[{\"move\":\"Tackle\","
                +"\"id\":\"tackle\",\"pp\":10,\"maxpp\":20,"
                +"\"target\":\"normal\",\"disabled\":false}]}]}";
    }
    private static final class Own {
        final Object battle,actor=new Object(),requestToken=new Object();
        final OwnRequestTransport transport=new OwnRequestTransport();
        final OwnRequestTransport.Lease lease;
        final NativeOwnActionFusion.Review fused;
        final ReadOnlyShadowObservationBridge bridge=new ReadOnlyShadowObservationBridge();
        final CrossStreamEvidenceReadiness.Session gate=new CrossStreamEvidenceReadiness.Session();
        final CrossStreamEvidenceReadiness.Report review;
        Own(Object battle){
            this.battle=battle;
            var raw=request();var parsed=OwnRequestJson.parse(raw);
            transport.owner(battle,actor,"p1");
            transport.dispatched(battle,actor,"p1",requestToken,()->parsed);
            transport.bindReadOnlyRaw(battle,actor,requestToken,"p1",raw,parsed);
            var moves=NativeOwnActionEvidence.capture(battle,actor,requestToken,
                    "p1",0,raw,parsed,"1.7.3+1.21.1");
            var switchSource=NativeOwnSwitchEvidence.capture(
                    battle,actor,requestToken,"p1",0,raw,parsed,
                    "1.7.3+1.21.1",transport);
            transport.turnDispatched(battle,1);
            lease=transport.beginChoice(battle,actor,requestToken,OWN,false,()->true);
            check(lease.current(),"native request lease is current");
            var moveReview=moves.attest(lease,new NativeOwnActionEvidence.InstalledView(
                    battle,actor,requestToken,"p1",OWN,1,List.of("tackle"),false));
            var partyReview=switchSource.attest(lease,
                    new NativeOwnSwitchEvidence.InstalledView(
                            battle,actor,requestToken,"p1",OWN,1,false,false,
                            List.of(new NativeOwnSwitchEvidence.PartySlot(
                                    OWN,100,false,true,false),
                                    new NativeOwnSwitchEvidence.PartySlot(
                                    BENCH,100,false,false,true))));
            fused=new NativeOwnActionFusion.Session().inspect(
                    lease,battle,actor,requestToken,"p1",OWN,1,false,
                    moveReview,partyReview);
            check(fused.current(),"6.12 native own move/switch fusion");
            check(bridge.observe(battle,publicFrames(),true).state()
                    ==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE,
                    "6.9 public source eligible");
            review=gate.inspect(bridge,battle,fused);
            check(review.current()
                    && review.verdict()
                        ==CrossStreamEvidenceReadiness.Verdict.CONSISTENT_READ_ONLY,
                    "6.13 native own/public turn source consistent");
        }
        void end(){transport.endChoice();}
    }
    private static final class Split {
        final Object battle,p1=new Object(),p2=new Object();
        final PublicSplitRoutingGate.Session source=new PublicSplitRoutingGate.Session();
        final NativeSplitDispatchObservation.Session nativeSession=
                new NativeSplitDispatchObservation.Session();
        final NativeSplitRecipientCustody.Session recipientSession=
                new NativeSplitRecipientCustody.Session();
        final NativeDispatchHistoryCorrelation.Session correlationSession=
                new NativeDispatchHistoryCorrelation.Session();
        final PublicSplitRoutingGate.Report structural;
        final NativeSplitRecipientCustody.Report recipient;
        final NativeDispatchHistoryCorrelation.Report correlation;
        Split(Object battle){
            this.battle=battle;
            structural=source.inspect(battle,withSplit(),true);
            check(structural.current() && structural.splitPairs()==1
                    && structural.publicTurn()==1,
                    "6.16 complete original private/public split structure");
            recipient=recipientSession.inspect(battle,structural);
            check(recipient.current() && recipient.verdict()
                    ==NativeSplitRecipientCustody.Verdict.DELIVERY_UNOBSERVED,
                    "6.17 recipient still unverified");
            var nativeReturn=nativeSession.observeNativeReturn(battle,
                    List.of(p1,p2),p1,new Object(),new Object(),1,5,0,true,false);
            check(nativeReturn.current() && !nativeReturn.recipientDeliveryAuthenticated(),
                    "6.18 pinned native dispatch RETURN without delivery proof");
            correlation=correlationSession.inspect(battle,structural,nativeReturn);
            check(correlation.current() && correlation.verdict()
                    ==NativeDispatchHistoryCorrelation.Verdict.PLAUSIBLE_WINDOW_UNVERIFIED,
                    "6.20 original split/native timing plausibly correlated");
        }
    }

    static void run(){
        Object battle=new Object();
        var direct=new Own(battle);
        var safety=new NativeEvidenceEndToEndGate.Session();
        var directAudit=safety.inspectDirect(battle,direct.review);
        check(directAudit.current() && directAudit.track()
                ==NativeEvidenceEndToEndGate.Track.DIRECT_OWN
                && directAudit.verdict()
                ==NativeEvidenceEndToEndGate.Verdict.DIRECT_ONLY_OBSERVATIONAL
                && directAudit.publicTurn()==1 && directAudit.publicRevision()==1,
                "6.9–6.13 direct track records current own/native consistency");
        check(directAudit.schema().equals(NativeEvidenceEndToEndGate.SCHEMA)
                && directAudit.source().equals(ReadOnlyShadowObservationBridge.COBBLEMON_VERSION)
                && !directAudit.completeInstalledLegalSetProven()
                && !directAudit.publicActorAuthenticated()
                && !directAudit.eventIdentityAuthenticated()
                && !directAudit.recipientDeliveryAuthenticated()
                && !directAudit.privatePayloadExportable()
                && !directAudit.opponentActionsKnown()
                && !directAudit.shadowSearchAuthorized()
                && !directAudit.tacticalDecisionAuthorized()
                && !directAudit.livePromotionAuthorized(),
                "even strongest direct report never certifies tactics or identity");
        check(!directAudit.toString().contains(OWN.toString())
                && !directAudit.toString().contains("PRIVATE")
                && !directAudit.toString().contains("tackle")
                && !directAudit.toString().contains("p1a:"),
                "direct diagnostics reveal no request, nickname, UUID or raw event");
        check(safety.inspectDirect(battle,direct.review).verdict()
                    ==NativeEvidenceEndToEndGate.Verdict.REPLAY_OR_DUPLICATE
                && safety.withdrawn() && !directAudit.current(),
                "replayed direct source epoch permanently withdraws gate");

        var freshOwn=new Own(new Object());
        var closedAudit=new NativeEvidenceEndToEndGate.Session().inspectDirect(
                freshOwn.battle,freshOwn.review);
        freshOwn.end();
        check(!closedAudit.current()
                && !closedAudit.recipientDeliveryAuthenticated(),
                "closing native owning lease expires end-to-end direct report");
        var oldPublic=new Own(new Object());
        var publicSafety=new NativeEvidenceEndToEndGate.Session();
        var before=publicSafety.inspectDirect(oldPublic.battle,oldPublic.review);
        var later=new ArrayList<>(publicFrames());later.add("update\n|turn|2");
        oldPublic.bridge.observe(oldPublic.battle,later,true);
        check(!before.current(),
                "an advancing public source invalidates formerly matching native source");

        var missing=new NativeEvidenceEndToEndGate.Session();
        check(missing.inspectDirect(new Object(),null).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.MISSING_DIRECT_EVIDENCE
                && !missing.withdrawn(),
                "no native fused/public receipt means abstain, not certified readiness");

        Object splitBattle=new Object();
        var split=new Split(splitBattle);
        var splitSafety=new NativeEvidenceEndToEndGate.Session();
        var splitAudit=splitSafety.inspectSplit(splitBattle,
                split.correlation,split.recipient);
        check(splitAudit.current()
                && splitAudit.verdict()
                    ==NativeEvidenceEndToEndGate.Verdict.SPLIT_TIMING_UNVERIFIED
                && splitAudit.track()==NativeEvidenceEndToEndGate.Track.SPLIT_UNVERIFIED
                && splitAudit.publicTurn()==1 && splitAudit.publicRevision()==1,
                "6.16–6.20 split source is an unverified correlation track");
        check(!splitAudit.eventIdentityAuthenticated()
                && !splitAudit.recipientDeliveryAuthenticated()
                && !splitAudit.completeInstalledLegalSetProven()
                && !splitAudit.shadowSearchAuthorized()
                && !splitAudit.tacticalDecisionAuthorized()
                && !splitAudit.livePromotionAuthorized(),
                "no split result can authenticate delivery or promote Shadow");
        check(!splitAudit.toString().contains("p1a:")
                && !splitAudit.toString().contains("52/100")
                && !splitAudit.toString().contains("60/100"),
                "split branch payloads never exported");
        check(splitSafety.inspectSplit(splitBattle,
                split.correlation,split.recipient).verdict()
                    ==NativeEvidenceEndToEndGate.Verdict.REPLAY_OR_DUPLICATE
                && splitSafety.withdrawn() && !splitAudit.current(),
                "split history/dispatch window cannot be reused");

        var expired=new Split(new Object());
        var expiry=new NativeEvidenceEndToEndGate.Session().inspectSplit(
                expired.battle,expired.correlation,expired.recipient);
        expired.source.withdraw();
        check(!expiry.current(),"public split structural source withdrawal invalidates 6.21");
        var ended=new Split(new Object());
        var endedAudit=new NativeEvidenceEndToEndGate.Session().inspectSplit(
                ended.battle,ended.correlation,ended.recipient);
        ended.nativeSession.battleEnded(ended.battle);
        check(!endedAudit.current() && !endedAudit.recipientDeliveryAuthenticated(),
                "native battle teardown invalidates correlated report");

        var wrongBattleDirect=new Own(new Object());
        var wrongDirect=new NativeEvidenceEndToEndGate.Session();
        check(wrongDirect.inspectDirect(new Object(),wrongBattleDirect.review).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.WRONG_BATTLE
                && wrongDirect.withdrawn(),
                "foreign direct battle object rejected");
        var wrongBattleSplit=new Split(new Object());
        var wrongSplit=new NativeEvidenceEndToEndGate.Session();
        check(wrongSplit.inspectSplit(new Object(),
                wrongBattleSplit.correlation,wrongBattleSplit.recipient).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.WRONG_BATTLE
                && wrongSplit.withdrawn(),
                "foreign split battle object rejected");

        var foreignRecipient=new Split(new Object());
        var sameObject=new Object();
        var left=new Split(sameObject);
        var otherSource=new PublicSplitRoutingGate.Session().inspect(
                sameObject,withSplit(),true);
        var otherRecipient=new NativeSplitRecipientCustody.Session().inspect(
                sameObject,otherSource);
        var custodyGate=new NativeEvidenceEndToEndGate.Session();
        check(custodyGate.inspectSplit(sameObject,left.correlation,otherRecipient)
                .verdict()==NativeEvidenceEndToEndGate.Verdict.WRONG_SPLIT_CUSTODY
                && custodyGate.withdrawn(),
                "matching counts, turn, battle and source SHA cannot substitute "
                +"a different original structural receipt");
        check(!otherRecipient.nativeRecipientDeliveryAuthenticated()
                && !foreignRecipient.recipient.nativeRecipientDeliveryAuthenticated(),
                "two independent histories never certify network delivery");

        var directFirst=new Own(new Object());
        var splitSame=new Split(directFirst.battle);
        var exclusive=new NativeEvidenceEndToEndGate.Session();
        var positive=exclusive.inspectDirect(directFirst.battle,directFirst.review);
        check(positive.current(),"initial direct result is current");
        check(exclusive.inspectSplit(directFirst.battle,
                splitSame.correlation,splitSame.recipient).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.CROSS_MODE_CONFLICT
                && exclusive.withdrawn() && !positive.current(),
                "synthetic direct and split positives never create a combined "
                +"runtime certificate");
        var splitFirst=new Split(new Object());
        var directSame=new Own(splitFirst.battle);
        var reverse=new NativeEvidenceEndToEndGate.Session();
        var beforeConflict=reverse.inspectSplit(splitFirst.battle,
                splitFirst.correlation,splitFirst.recipient);
        check(beforeConflict.current()
                && reverse.inspectDirect(splitFirst.battle,directSame.review).verdict()
                    ==NativeEvidenceEndToEndGate.Verdict.CROSS_MODE_CONFLICT
                && !beforeConflict.current(),
                "reverse split/direct conflict is also terminal");

        var missingSplit=new NativeEvidenceEndToEndGate.Session();
        check(missingSplit.inspectSplit(new Object(),null,null).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.MISSING_SPLIT_EVIDENCE
                && !missingSplit.withdrawn(),
                "missing optional native dispatch simply abstains");
        var revoked=new Split(new Object());
        revoked.nativeSession.withdraw();
        var stale=new NativeEvidenceEndToEndGate.Session();
        check(stale.inspectSplit(revoked.battle,revoked.correlation,revoked.recipient)
                .verdict()==NativeEvidenceEndToEndGate.Verdict.STALE_SOURCE
                && stale.withdrawn(),
                "withdrawn native source prevents old witness replay");
        var terminal=new NativeEvidenceEndToEndGate.Session();
        terminal.withdraw();
        check(terminal.inspectDirect(new Object(),null).verdict()
                ==NativeEvidenceEndToEndGate.Verdict.WITHDRAWN
                && terminal.inspectSplit(new Object(),null,null).verdict()
                    ==NativeEvidenceEndToEndGate.Verdict.WITHDRAWN,
                "terminal gate cannot be resurrected by any track");
        System.out.println("Phase 6.21 end-to-end contract safety PASS: "
                +checks+" synthetic assertions");
    }
}
