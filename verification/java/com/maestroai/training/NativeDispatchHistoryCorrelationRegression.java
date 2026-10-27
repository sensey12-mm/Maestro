package com.maestroai.training;

import java.util.*;

/** Phase 6.20: adversarial source-scoped dispatch/history correspondence. */
final class NativeDispatchHistoryCorrelationRegression {
    private NativeDispatchHistoryCorrelationRegression(){}
    private static int checks;
    private static void check(boolean b,String reason){
        if(!b)throw new AssertionError("Phase 6.20 "+reason);
        checks++;
    }
    private static final String PRIVATE="PRIVATE_DO_NOT_REPORT_ACTOR_REQUEST";
    private static List<String> base(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> append(List<String> base,String... events){
        var result=new ArrayList<>(base);
        Collections.addAll(result,events);
        return List.copyOf(result);
    }
    private static String split(int side){
        String prefix=side==1?"p1a: Own":"p2a: Opp";
        return "update\n|split|p"+side+"\n|-damage|"+prefix+"|52/100\n"
                +"|-damage|"+prefix+"|60/100";
    }
    private static final class Source {
        final Object battle=new Object(),p1=new Object(),p2=new Object();
        final Object hidden=new Object(),publicPacket=new Object();
        final NativeSplitDispatchObservation.Session dispatch=
                new NativeSplitDispatchObservation.Session();
        final PublicSplitRoutingGate.Session history=
                new PublicSplitRoutingGate.Session();
        final NativeDispatchHistoryCorrelation.Session gate=
                new NativeDispatchHistoryCorrelation.Session();
        NativeSplitDispatchObservation.Report send(int turn,int chunks){
            return dispatch.observeNativeReturn(battle,List.of(p1,p2),
                    p1,hidden,publicPacket,turn,chunks,0,true,false);
        }
        NativeSplitDispatchObservation.Report noHook(int turn,int chunks){
            return dispatch.status(battle,turn,chunks,true,false);
        }
        PublicSplitRoutingGate.Report read(List<String> chunks){
            return history.inspect(battle,chunks,true);
        }
        NativeDispatchHistoryCorrelation.Report correlate(
                PublicSplitRoutingGate.Report pub,
                NativeSplitDispatchObservation.Report nativeReturn){
            return gate.inspect(battle,pub,nativeReturn);
        }
    }
    static void run(){
        var one=new Source();
        var evidence=append(base(),split(1));
        var native1=one.send(1,5);
        var public1=one.read(evidence);
        var ok=one.correlate(public1,native1);
        check(ok.verdict()==NativeDispatchHistoryCorrelation.Verdict.PLAUSIBLE_WINDOW_UNVERIFIED
                && ok.current() && ok.publicRevision()==1 && ok.nativeReturnOrdinal()==1
                && ok.structurallyObservedSplitCount()==1
                && ok.historyChunkCount()==5 && ok.observedTurn()==1,
                "one exact-battle native return near one observed split history chunk");
        check(ok.schema().equals(NativeDispatchHistoryCorrelation.SCHEMA)
                && ok.source().equals(PublicSplitRoutingGate.SOURCE)
                && !ok.sameUnderlyingEventProven()
                && !ok.exactSplitPacketLinked()
                && !ok.recipientDeliveryAuthenticated()
                && !ok.clientMessageProcessed()
                && !ok.privatePayloadExportable()
                && !ok.publicReadinessLeaseAuthorized()
                && !ok.tacticalDecisionAuthorized()
                && !ok.shadowSearchAuthorized()
                && !ok.livePromotionAuthorized(),
                "temporal correspondence never grants native/event/recipient/tactical proof");
        check(!ok.toString().contains(PRIVATE)
                && !ok.toString().contains("p1a:")
                && !ok.toString().contains("Own")
                && !ok.toString().contains("52/100")
                && !ok.toString().contains("60/100"),
                "only numeric and verdict metadata exported");
        var continuation=append(evidence,"update\n|turn|2",split(2));
        var native2=one.send(2,7);
        var public2=one.read(continuation);
        var ok2=one.correlate(public2,native2);
        check(ok2.current() && ok2.nativeReturnOrdinal()==2
                && ok2.publicRevision()==2 && !ok.current()
                && ok2.structurallyObservedSplitCount()==2
                && ok2.observedTurn()==2,
                "new public revision and native ordinal advances invalidate old window");
        check(one.correlate(public2,native2).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.REPLAYED_OR_STALE
                && one.gate.withdrawn() && !ok2.current(),
                "duplicate same source window is not counted twice");

        var nearby=new Source();
        check(nearby.correlate(nearby.read(evidence),nearby.send(1,4)).current(),
                "one-chunk ambiguity tolerated ONLY as unverified correlation");
        var absent=new Source();
        var absentPublic=absent.read(evidence);
        var missing=absent.correlate(absentPublic,absent.noHook(1,5));
        check(missing.verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.MISSING_NATIVE_OBSERVATION
                && !missing.current() && !absent.gate.withdrawn(),
                "missing optional Mixin callback never pretends dispatch happened");
        check(absent.correlate(absentPublic,absent.send(1,5)).current(),
                "subsequent real method return can be examined without inventing history");
        var onlyPublic=new Source();
        check(onlyPublic.correlate(onlyPublic.read(base()),onlyPublic.send(1,4))
                .verdict()==NativeDispatchHistoryCorrelation.Verdict.MISSING_SPLIT_HISTORY
                && !onlyPublic.gate.withdrawn(),
                "native method return without a split remains unlinked");

        var wrongHistory=new Source();
        var wrong=wrongHistory.correlate(wrongHistory.read(evidence),
                wrongHistory.send(1,3));
        check(wrong.verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.HISTORY_WINDOW_CONTRADICTION
                && wrongHistory.gate.withdrawn(),
                "distant original history position cannot correlate with split");
        var ahead=new Source();
        var aheadReport=ahead.correlate(ahead.read(evidence),ahead.send(1,6));
        check(aheadReport.verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.HISTORY_WINDOW_CONTRADICTION,
                "native history count cannot exceed observed source length");
        var turnWrong=new Source();
        check(turnWrong.correlate(turnWrong.read(evidence),turnWrong.send(2,5))
                .verdict()==NativeDispatchHistoryCorrelation.Verdict.TURN_MISMATCH
                && turnWrong.gate.withdrawn(),
                "native turn must match parsed source public turn");
        var many=new Source();
        var doubleSplit=append(base(),split(1),split(2));
        check(many.correlate(many.read(doubleSplit),many.send(1,5)).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.AMBIGUOUS_MULTIPLE_EVENTS
                && many.gate.withdrawn(),
                "multiple split records with one return cannot be one-to-one matched");
        var missedNative=new Source();
        missedNative.send(1,4);
        var latest=missedNative.send(1,5);
        check(missedNative.correlate(missedNative.read(evidence),latest).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.AMBIGUOUS_MULTIPLE_EVENTS,
                "unobserved first native return cannot be silently discarded");

        var mismatch=new Source();
        var sourceElsewhere=new Source();
        var other=mismatch.gate.inspect(mismatch.battle,
                sourceElsewhere.read(evidence),mismatch.send(1,5));
        check(other.verdict()==NativeDispatchHistoryCorrelation.Verdict.BATTLE_MISMATCH
                && mismatch.gate.withdrawn(),
                "foreign public battle source rejected despite identical turn/count");
        var foreignNative=new Source();
        var originPublic=foreignNative.read(evidence);
        var fromOther=new Source();
        check(foreignNative.gate.inspect(foreignNative.battle,originPublic,
                fromOther.send(1,5)).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.BATTLE_MISMATCH,
                "foreign native dispatch source rejected");
        var expiredNative=new Source();
        var onceNative=expiredNative.send(1,5);
        expiredNative.dispatch.withdraw();
        check(expiredNative.correlate(expiredNative.read(evidence),onceNative).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.BAD_SOURCE,
                "source withdrawal prevents correlation");
        var expiredHistory=new Source();
        var oldPublic=expiredHistory.read(evidence);
        expiredHistory.read(continuation);
        check(expiredHistory.correlate(oldPublic,expiredHistory.send(1,5)).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.BAD_SOURCE,
                "superseded public revision cannot be correlated");
        var rewritten=new Source();
        var beforeRewrite=rewritten.read(evidence);
        var rewrittenHistory=append(base(),"update\n|split|p1\n"
                +"|-damage|p1a: Own|"+PRIVATE+"\n"
                +"|-damage|p1a: Own|60/100");
        var rejectedHistory=rewritten.read(rewrittenHistory);
        check(rejectedHistory.verdict()==PublicSplitRoutingGate.Verdict.HISTORY_REWRITTEN
                && rewritten.correlate(beforeRewrite,rewritten.send(1,5)).verdict()
                    ==NativeDispatchHistoryCorrelation.Verdict.BAD_SOURCE,
                "private half rewrite immediately invalidates structural history");

        var missingBoth=new Source();
        check(missingBoth.gate.inspect(missingBoth.battle,null,null).verdict()
                ==NativeDispatchHistoryCorrelation.Verdict.BAD_SOURCE,
                "no original source handles cannot manufacture a link");
        var close=new Source();
        var proof=close.correlate(close.read(evidence),close.send(1,5));
        close.dispatch.battleEnded(close.battle);
        check(!proof.current() && !proof.recipientDeliveryAuthenticated(),
                "native battle teardown expires correlation instantly");
        var publicClosed=new Source();
        var publicProof=publicClosed.correlate(publicClosed.read(evidence),
                publicClosed.send(1,5));
        publicClosed.history.withdraw();
        check(!publicProof.current(),"public source withdrawal expires correlation");
        var gateClosed=new Source();
        var gateProof=gateClosed.correlate(gateClosed.read(evidence),
                gateClosed.send(1,5));
        gateClosed.gate.withdraw();
        check(!gateProof.current()
                && gateClosed.correlate(gateClosed.read(evidence),
                    gateClosed.noHook(1,5)).verdict()
                    ==NativeDispatchHistoryCorrelation.Verdict.WITHDRAWN,
                "correlation gate lifetime withdrawal is terminal");

        System.out.println("Phase 6.20 source-only dispatch/history PASS: "
                +checks+" adversarial checks");
    }
}
