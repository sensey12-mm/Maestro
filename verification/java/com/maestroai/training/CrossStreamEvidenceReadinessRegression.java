package com.maestroai.training;

import java.util.*;

/** Phase 6.13 adversarial native-own vs public-history readiness tests. */
final class CrossStreamEvidenceReadinessRegression {
    private CrossStreamEvidenceReadinessRegression(){}
    private static int checks;
    private static final UUID OWN=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BENCH=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError("Phase 6.13 "+reason);
        checks++;
    }
    private static String pokemon(UUID uuid,int hp,boolean active){
        return "{\"details\":\"Genesect, "+uuid+", L50\","
            +"\"ident\":\"p1: PRIVATE_ALIAS\",\"active\":"+active+","
            +"\"condition\":\""+hp+"/100"+(hp==0?" fnt":"")+"\","
            +"\"ability\":\"Download\",\"types\":[\"bug\",\"steel\"],"
            +"\"baseTypes\":[\"bug\",\"steel\"],"
            +"\"stats\":{\"atk\":90,\"def\":90,\"spa\":90,\"spd\":90,\"spe\":90},"
            +"\"item\":\"\",\"terastallized\":\"\","
            +"\"commanding\":false,\"reviving\":false}";
    }
    private static String raw(boolean forced,boolean trapped){
        return "|request|{\"side\":{\"id\":\"p1\",\"pokemon\":["
            +pokemon(OWN,forced?0:100,true)+","
            +pokemon(BENCH,70,false)+"]},"
            +(forced?"\"forceSwitch\":[true]":
                "\"active\":[{\"trapped\":"+trapped+",\"moves\":["
                +"{\"move\":\"Tackle\",\"id\":\"tackle\",\"pp\":10,"
                +"\"maxpp\":20,\"target\":\"normal\",\"disabled\":false}]}]")+"}";
    }
    private static final class Own {
        final Object battle,actor,request=new Object();
        final boolean forced,trapped;
        final OwnRequestTransport ledger=new OwnRequestTransport();
        final OwnRequestTransport.Lease lease;
        final NativeOwnActionFusion.Session fusion=new NativeOwnActionFusion.Session();
        final NativeOwnActionFusion.Review review;
        Own(Object battle,boolean forced,boolean trapped){
            this.battle=battle;this.actor=new Object();
            this.forced=forced;this.trapped=trapped;
            String original=raw(forced,trapped);
            var parsed=OwnRequestJson.parse(original);
            ledger.owner(battle,actor,"p1");
            if(forced)ledger.turnDispatched(battle,1);
            ledger.dispatched(battle,actor,"p1",request,()->parsed);
            ledger.bindReadOnlyRaw(battle,actor,request,"p1",original,parsed);
            var move=forced?null:NativeOwnActionEvidence.capture(
                    battle,actor,request,"p1",0,original,parsed,"1.7.3+1.21.1");
            var switchSource=NativeOwnSwitchEvidence.capture(
                    battle,actor,request,"p1",forced?1:0,original,parsed,
                    "1.7.3+1.21.1",ledger);
            if(!forced)ledger.turnDispatched(battle,1);
            lease=ledger.beginChoice(battle,actor,request,OWN,forced,()->true);
            check(lease.current(),"own request lease current");
            var ownMove=forced?null:move.attest(lease,
                    new NativeOwnActionEvidence.InstalledView(
                            battle,actor,request,"p1",OWN,1,List.of("tackle"),trapped));
            var ownSwitch=switchSource.attest(lease,
                    new NativeOwnSwitchEvidence.InstalledView(
                            battle,actor,request,"p1",OWN,1,forced,forced?null:trapped,
                            List.of(
                                new NativeOwnSwitchEvidence.PartySlot(
                                        OWN,forced?0:100,false,true,false),
                                new NativeOwnSwitchEvidence.PartySlot(
                                        BENCH,70,false,false,true))));
            review=fusion.inspect(lease,battle,actor,request,"p1",OWN,1,forced,
                    ownMove,ownSwitch);
            check(review.current(),"6.12 fusion current");
        }
        void close(){ledger.endChoice();}
    }
    private static ReadOnlyShadowObservationBridge bridge(Object battle){
        var b=new ReadOnlyShadowObservationBridge();
        var result=b.observe(battle,
                OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        check(result.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE,
                "narrow source-pinned public observation");
        return b;
    }
    private static CrossStreamEvidenceReadiness.Report inspect(
            CrossStreamEvidenceReadiness.Session gate,
            ReadOnlyShadowObservationBridge publicStream,Object battle,Own own){
        return gate.inspect(publicStream,battle,own==null?null:own.review);
    }
    static void run(){
        Object battle=new Object();
        var own=new Own(battle,false,false);
        var stream=bridge(battle);
        var gate=new CrossStreamEvidenceReadiness.Session();
        var accepted=inspect(gate,stream,battle,own);
        check(accepted.verdict()==CrossStreamEvidenceReadiness.Verdict.CONSISTENT_READ_ONLY
                && accepted.current() && accepted.publicRevision()==1
                && accepted.publicTurn()==1 && accepted.ownTurn()==1
                && accepted.publicEventCount()==4 && accepted.witnessedOwnMoves()==1
                && accepted.witnessedSwitchCandidates()==1 && !accepted.forcedReplacement(),
                "matching public/native current-turn observational report");
        check(accepted.schema().equals(CrossStreamEvidenceReadiness.SCHEMA)
                && accepted.nativeSource().equals(OwnRequestTransport.SOURCE)
                && accepted.publicSource().equals(ReadOnlyShadowObservationBridge.SCHEMA)
                && !accepted.nativeActorAuthenticatedByPublic()
                && !accepted.sourceDeliveryAuthenticated()
                && !accepted.completeLegalSetProven()
                && !accepted.opponentActionsKnown()
                && !accepted.shadowSearchAuthorized()
                && !accepted.tacticalDecisionAuthorized()
                && !accepted.livePromotionAuthorized(),
                "cross-stream consistency cannot authenticate actor or tactics");
        check(!accepted.toString().contains("PRIVATE")
                && !accepted.toString().contains(OWN.toString())
                && !accepted.toString().contains(BENCH.toString())
                && !accepted.toString().contains(own.review.coverageFingerprint()),
                "private source payloads and identifiers are never logged");
        var independent=new Own(new Object(),false,false);
        var independentReport=inspect(new CrossStreamEvidenceReadiness.Session(),
                bridge(independent.battle),independent.battle,independent);
        check(independentReport.verdict()==accepted.verdict()
                && independentReport.publicEventCount()==accepted.publicEventCount(),
                "same source evidence produces deterministic redacted verdict");

        var repeated=inspect(gate,stream,battle,own);
        check(repeated.verdict()==CrossStreamEvidenceReadiness.Verdict.STALE_OR_REPLAYED
                && gate.withdrawn() && !accepted.current(),
                "reusing identical public revision withdraws session and earlier report");
        check(inspect(gate,stream,battle,own).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.WITHDRAWN,
                "poisoned cross-stream session cannot be revived");

        var forced=new Own(new Object(),true,false);
        var forcedReport=inspect(new CrossStreamEvidenceReadiness.Session(),
                bridge(forced.battle),forced.battle,forced);
        check(forcedReport.current() && forcedReport.forcedReplacement()
                && forcedReport.witnessedOwnMoves()==0
                && forcedReport.witnessedSwitchCandidates()==1,
                "forced replacement contains no invented ordinary moves");
        var trapped=new Own(new Object(),false,true);
        var trappedReport=inspect(new CrossStreamEvidenceReadiness.Session(),
                bridge(trapped.battle),trapped.battle,trapped);
        check(trappedReport.current() && !trappedReport.forcedReplacement()
                && trappedReport.witnessedOwnMoves()==1
                && trappedReport.witnessedSwitchCandidates()==0,
                "trapped source retains own moves but no switches");

        var missingPublic=new Own(new Object(),false,false);
        var noStream=new ReadOnlyShadowObservationBridge();
        var noPublicGate=new CrossStreamEvidenceReadiness.Session();
        check(inspect(noPublicGate,noStream,missingPublic.battle,missingPublic).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.PUBLIC_UNAVAILABLE
                && noPublicGate.withdrawn(),
                "absent public observation fails closed");
        var missingNative=new Object();
        var noNative=bridge(missingNative);
        var noNativeGate=new CrossStreamEvidenceReadiness.Session();
        check(inspect(noNativeGate,noNative,missingNative,null).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.NATIVE_UNAVAILABLE
                && noNativeGate.withdrawn(),"absent fused request fails closed");

        var swapped=new Own(new Object(),false,false);
        var wrongBattle=bridge(new Object());
        check(inspect(new CrossStreamEvidenceReadiness.Session(),wrongBattle,
                swapped.battle,swapped).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.PUBLIC_UNAVAILABLE,
                "foreign observer object not accepted");
        var forged=new Own(new Object(),false,false);
        check(inspect(new CrossStreamEvidenceReadiness.Session(),bridge(forged.battle),
                new Object(),forged).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.PUBLIC_UNAVAILABLE,
                "caller cannot substitute public battle identity");
        var mixedOwn=new Own(new Object(),false,false);
        Object another=new Object();
        check(inspect(new CrossStreamEvidenceReadiness.Session(),bridge(another),
                another,mixedOwn).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.BATTLE_MISMATCH,
                "same public turn but native receipt from another battle");

        var advance=new Own(new Object(),false,false);
        var advanceStream=bridge(advance.battle);
        var advanceGate=new CrossStreamEvidenceReadiness.Session();
        var old=inspect(advanceGate,advanceStream,advance.battle,advance);
        var later=new ArrayList<>(OfflineShadowResponseScenariosRegression.frames("Scratch"));
        later.add("update\n|turn|2");
        var laterStatus=advanceStream.observe(advance.battle,later,true);
        check(laterStatus.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && laterStatus.revision()==2 && !old.current(),
                "append-only public history advancing expires old readiness");
        check(inspect(advanceGate,advanceStream,advance.battle,advance).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.TURN_MISMATCH,
                "current public turn cannot be reconciled with stale native turn");
        var rollback=new Own(new Object(),false,false);
        var rollbackStream=bridge(rollback.battle);
        var rollbackGate=new CrossStreamEvidenceReadiness.Session();
        var prior=inspect(rollbackGate,rollbackStream,rollback.battle,rollback);
        var truncated=OfflineShadowResponseScenariosRegression.frames("Scratch")
                .subList(0,3);
        check(rollbackStream.observe(rollback.battle,truncated,true).state()
                ==ReadOnlyShadowObservationBridge.State.REPLAY_OR_REWRITE
                && !prior.current(),
                "rewritten source permanently expires prior readiness");
        check(inspect(rollbackGate,rollbackStream,rollback.battle,rollback).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.PUBLIC_UNAVAILABLE,
                "withdrawn source cannot produce another readiness report");

        var nativeEnds=new Own(new Object(),false,false);
        var nativeEndsStream=bridge(nativeEnds.battle);
        var nativeEndsGate=new CrossStreamEvidenceReadiness.Session();
        var liveReport=inspect(nativeEndsGate,nativeEndsStream,
                nativeEnds.battle,nativeEnds);
        nativeEnds.close();
        check(!liveReport.current(),"native choice completion expires readiness");
        check(inspect(nativeEndsGate,nativeEndsStream,nativeEnds.battle,nativeEnds).verdict()
                ==CrossStreamEvidenceReadiness.Verdict.NATIVE_UNAVAILABLE,
                "expired owning lease fails closed");
        var closed=new Own(new Object(),false,false);
        var closedStream=bridge(closed.battle);
        var closedGate=new CrossStreamEvidenceReadiness.Session();
        var closedReview=inspect(closedGate,closedStream,closed.battle,closed);
        closedGate.withdraw();
        check(!closedReview.current()
                && inspect(closedGate,closedStream,closed.battle,closed).verdict()
                    ==CrossStreamEvidenceReadiness.Verdict.WITHDRAWN,
                "explicitly closed gate cannot issue or retain readiness");

        var privateOne=new Own(new Object(),false,false);
        var privateStream=new ReadOnlyShadowObservationBridge();
        var privateFrames=new ArrayList<>(
                OfflineShadowResponseScenariosRegression.frames("Scratch"));
        privateFrames.add("sideupdate\n|request|PRIVATE_OPPONENT_REQUEST");
        var privateStatus=privateStream.observe(privateOne.battle,privateFrames,true);
        check(privateStatus.state()==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && inspect(new CrossStreamEvidenceReadiness.Session(),privateStream,
                    privateOne.battle,privateOne).verdict()
                    ==CrossStreamEvidenceReadiness.Verdict.PUBLIC_UNAVAILABLE,
                "unapproved sideupdate never forms a cross-stream certificate");

        // Public line framing does not cryptographically authenticate actors.
        var actorLine=new Own(new Object(),false,false);
        var actorStream=new ReadOnlyShadowObservationBridge();
        var spoofed=new ArrayList<>(
                OfflineShadowResponseScenariosRegression.frames("Scratch"));
        spoofed.set(3,"update\n|move|p2a: PRIVATE_SPOOF|Scratch|p1a: Own");
        check(actorStream.observe(actorLine.battle,spoofed,true).state()
                ==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE,
                "public framing alone cannot certify actor display identity");
        var spoofReview=inspect(new CrossStreamEvidenceReadiness.Session(),actorStream,
                actorLine.battle,actorLine);
        check(spoofReview.verdict()==CrossStreamEvidenceReadiness.Verdict.CONSISTENT_READ_ONLY
                && !spoofReview.nativeActorAuthenticatedByPublic()
                && !spoofReview.sourceDeliveryAuthenticated(),
                "even syntactically consistent forged public actors remain non-authenticated");

        System.out.println("Phase 6.13 cross-stream readiness PASS: "
                +checks+" synthetic checks");
    }
}
