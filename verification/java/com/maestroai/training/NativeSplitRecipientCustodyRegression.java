package com.maestroai.training;

import java.util.*;

/** Adversarial Phase 6.17 recipient claims vs actual structural source. */
final class NativeSplitRecipientCustodyRegression {
    private NativeSplitRecipientCustodyRegression(){}
    private static int checks;
    private static final String PRIVATE_SENTINEL="PRIVATE_REQUEST_NEVER_PRINT";
    private static final String A="a".repeat(64), B="b".repeat(64),
                                C="c".repeat(64), D="d".repeat(64);
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.17: "+why);
        checks++;
    }
    private static List<String> frames(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static List<String> append(List<String> base,String... items){
        var copy=new ArrayList<>(base);
        Collections.addAll(copy,items);
        return List.copyOf(copy);
    }
    private static String split(String target,String kind,String privateHp,String publicHp){
        return "update\n|split|"+target+"\n"
                +"|"+kind+"|"+target+"a: Own|"+privateHp+"\n"
                +"|"+kind+"|"+target+"a: Own|"+publicHp;
    }
    private static PublicSplitRoutingGate.Report structure(Object battle,
                                                            List<String> frames){
        var s=new PublicSplitRoutingGate.Session();
        var r=s.inspect(battle,frames,true);
        check(r.current() && r.splitPairs()>0
                && r.splitPublicExtractionStructurallyMatched(),
                "Phase 6.16 structural source is current");
        return r;
    }
    private static NativeSplitRecipientCustody.SyntheticClaim claim(
            long rev,int position,NativeSplitRecipientCustody.Branch branch,
            NativeSplitRecipientCustody.Audience audience,long tick,String tag){
        return NativeSplitRecipientCustody.SyntheticClaim.fixture(
                rev,position,branch,audience,tick,tag);
    }
    private static List<NativeSplitRecipientCustody.SyntheticClaim> pair(
            long rev,NativeSplitRecipientCustody.Audience privateSide){
        return List.of(
            claim(rev,1,NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                    privateSide,8,A),
            claim(rev,2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                    NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,B));
    }
    private static void deny(PublicSplitRoutingGate.Report original,Object battle,
            List<NativeSplitRecipientCustody.SyntheticClaim> claims,long tick,String why){
        var gate=new NativeSplitRecipientCustody.Session();
        var r=gate.inspectSynthetic(battle,original,claims,tick);
        check((r.verdict()==NativeSplitRecipientCustody.Verdict.RECEIPT_MISMATCH
                || r.verdict()==NativeSplitRecipientCustody.Verdict.STALE_OR_REPLAYED
                || r.verdict()==NativeSplitRecipientCustody.Verdict.SOURCE_UNAVAILABLE)
                && gate.withdrawn() && !r.current(),why+" fail closed");
        check(!r.toString().contains(PRIVATE_SENTINEL)
                && !r.toString().contains(A)
                && !r.toString().contains("p1a:"),
                why+" privacy-safe rejection");
    }
    static void run(){
        Object battle=new Object();
        List<String> history=append(frames(),split("p1","-damage","53/100","60/100"));
        var source=structure(battle,history);
        var runtime=new NativeSplitRecipientCustody.Session();
        var unobserved=runtime.inspect(battle,source);
        check(unobserved.verdict()==NativeSplitRecipientCustody.Verdict.DELIVERY_UNOBSERVED
                && unobserved.current()
                && unobserved.structurallyMatchedPairs()==1
                && unobserved.publicPrivateSeparationStructurallyMatched(),
                "real original Showdown history allows structural audit only");
        check(!unobserved.nativeRecipientDeliveryAuthenticated()
                && !unobserved.nativeObserverDeliveryAuthenticated()
                && !unobserved.publicReadinessLeaseAuthorized()
                && !unobserved.privatePayloadExportable()
                && !unobserved.shadowSearchAuthorized()
                && !unobserved.tacticalDecisionAuthorized()
                && !unobserved.livePromotionAuthorized(),
                "never equate simulated receipts with native verified delivery");
        check(runtime.inspect(battle,source).verdict()
                ==NativeSplitRecipientCustody.Verdict.DELIVERY_UNOBSERVED,
                "idempotent native unobserved diagnostics");
        check(!unobserved.toString().contains("Own")
                && !unobserved.toString().contains("53/100")
                && !unobserved.toString().contains("60/100"),
                "native reports never leak any branch payload");
        var positive=new NativeSplitRecipientCustody.Session();
        var fixtures=pair(source.revision(),
                NativeSplitRecipientCustody.Audience.TARGET_P1);
        var synthetic=positive.inspectSynthetic(battle,source,fixtures,9);
        check(synthetic.verdict()
                    ==NativeSplitRecipientCustody.Verdict.SYNTHETIC_RECIPIENT_ORDER_MATCHED
                && synthetic.current() && synthetic.sourceRevision()==source.revision()
                && synthetic.structurallyMatchedPairs()==1,
                "synthetic recipient pair matches source ordering");
        check(!synthetic.nativeRecipientDeliveryAuthenticated()
                && !synthetic.nativeObserverDeliveryAuthenticated()
                && !synthetic.publicReadinessLeaseAuthorized()
                && !synthetic.privatePayloadExportable()
                && !synthetic.livePromotionAuthorized(),
                "synthetic success deliberately never grants custody authority");
        check(!fixtures.get(0).toString().contains(A)
                && !fixtures.get(1).toString().contains(B),
                "synthetic tag logging is redacted");
        var replay=positive.inspectSynthetic(battle,source,fixtures,9);
        check(replay.verdict()==NativeSplitRecipientCustody.Verdict.STALE_OR_REPLAYED
                && positive.withdrawn() && !synthetic.current(),
                "consumed synthetic receipt cannot be replayed");
        check(positive.inspect(battle,source).verdict()
                ==NativeSplitRecipientCustody.Verdict.WITHDRAWN,
                "replay terminally revokes session");

        deny(source,battle,List.of(),9,"missing both delivery claims");
        deny(source,battle,fixtures.subList(0,1),9,"missing public branch");
        deny(source,battle,List.of(fixtures.get(1),fixtures.get(0)),9,
                "reordered public before private");
        deny(source,battle,pair(source.revision(),
                NativeSplitRecipientCustody.Audience.TARGET_P2),9,
                "private receipt routed to wrong side");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.TARGET_P2,8,B)),9,
                "public branch sent as private owner");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,B)),9,
                "public recipient with private-branch label");
        deny(source,battle,List.of(
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                        NativeSplitRecipientCustody.Audience.TARGET_P1,8,A),
                fixtures.get(1)),9,"wrong native dispatch sequence");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision()+1,2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,B)),9,
                "cross-revision forged receipt");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,A)),9,
                "duplicate receipt token in two branches");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,
                        PRIVATE_SENTINEL)),9,
                "raw private identity substituted for opaque synthetic tag");
        deny(source,battle,fixtures,11,"stale synthetic receipt beyond short lease");
        deny(source,battle,fixtures,7,"future-dated receipt");
        deny(source,battle,List.of(fixtures.get(0),
                claim(source.revision(),2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,-1,B)),9,
                "invalid negative receipt clock");

        var two=append(history,split("p2","-heal","41/100","45/100"));
        Object twoBattle=new Object();
        var twoSource=structure(twoBattle,two);
        var routePair=List.of(
                claim(twoSource.revision(),1,NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                        NativeSplitRecipientCustody.Audience.TARGET_P1,8,A),
                claim(twoSource.revision(),2,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,B),
                claim(twoSource.revision(),3,NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                        NativeSplitRecipientCustody.Audience.TARGET_P2,8,C),
                claim(twoSource.revision(),4,NativeSplitRecipientCustody.Branch.PUBLIC_SECOND,
                        NativeSplitRecipientCustody.Audience.PUBLIC_SPECTATOR,8,D));
        var multi=new NativeSplitRecipientCustody.Session();
        var multiProof=multi.inspectSynthetic(twoBattle,twoSource,routePair,9);
        check(multiProof.verdict()
                ==NativeSplitRecipientCustody.Verdict.SYNTHETIC_RECIPIENT_ORDER_MATCHED
                && multiProof.structurallyMatchedPairs()==2,
                "two split targets keep distinct ordered recipient claims");
        var wrongSecond=new ArrayList<>(routePair);
        wrongSecond.set(2,claim(twoSource.revision(),3,
                NativeSplitRecipientCustody.Branch.PRIVATE_FIRST,
                NativeSplitRecipientCustody.Audience.TARGET_P1,8,C));
        deny(twoSource,twoBattle,wrongSecond,9,
                "second pair delivered to first pair's recipient");
        var shuffled=new ArrayList<>(routePair);
        Collections.swap(shuffled,1,2);
        deny(twoSource,twoBattle,shuffled,9,"cross-pair event reordering");
        deny(twoSource,twoBattle,routePair.subList(0,3),9,
                "incomplete final split pair");

        var otherBattle=new NativeSplitRecipientCustody.Session();
        var foreign=otherBattle.inspect(new Object(),source);
        check(foreign.verdict()==NativeSplitRecipientCustody.Verdict.BATTLE_MISMATCH
                && otherBattle.withdrawn(),"foreign battle source cannot be reused");
        var sourceOwner=new PublicSplitRoutingGate.Session();
        var prior=sourceOwner.inspect(battle,history,true);
        var ownSession=new NativeSplitRecipientCustody.Session();
        var ownReport=ownSession.inspect(battle,prior);
        var more=append(history,split("p2","-heal","40/100","44/100"));
        sourceOwner.inspect(battle,more,true);
        check(!ownReport.current() && sourceOwner.withdrawn()==false,
                "public structural revision advance expires prior delivery review");
        check(ownSession.inspect(battle,prior).verdict()
                ==NativeSplitRecipientCustody.Verdict.SOURCE_UNAVAILABLE,
                "old source revision cannot be reasserted");

        var alteredSource=new PublicSplitRoutingGate.Session();
        var baseline=alteredSource.inspect(battle,history,true);
        var stamped=new NativeSplitRecipientCustody.Session();
        var stampedReport=stamped.inspect(battle,baseline);
        var rewritten=append(frames(),split("p1","-damage","52/100","60/100"));
        alteredSource.inspect(battle,rewritten,true);
        check(!stampedReport.current() && alteredSource.withdrawn(),
                "private-half rewrite invalidates all recipient custody reports");
        var closed=new NativeSplitRecipientCustody.Session();
        var closedReport=closed.inspect(battle,source);
        closed.withdraw();
        check(!closedReport.current() &&
                closed.inspect(battle,source).verdict()
                    ==NativeSplitRecipientCustody.Verdict.WITHDRAWN,
                "explicit session withdrawal is sticky");

        var zeroBattle=new Object();
        var plain=new PublicSplitRoutingGate.Session().inspect(
                zeroBattle,frames(),true);
        var noSplit=new NativeSplitRecipientCustody.Session().inspect(zeroBattle,plain);
        check(noSplit.verdict()==NativeSplitRecipientCustody.Verdict.NO_SPLIT_SOURCE
                && !noSplit.current() && !noSplit.publicReadinessLeaseAuthorized(),
                "direct-only battle does not conjure split recipients");
        System.out.println("Phase 6.17 recipient custody PASS: "+checks
                +" synthetic and unverified-delivery checks");
    }
}
