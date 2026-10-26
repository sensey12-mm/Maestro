package com.maestroai.training;

import java.util.*;

/** Phase 6.18: native dispatch method boundary vs actual client delivery. */
final class NativeSplitDispatchObservationRegression {
    private NativeSplitDispatchObservationRegression(){}
    private static int checks;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.18 "+why);
        checks++;
    }
    private static final String SECRET="PRIVATE_RECIPIENT_UUID_NEVER_LOG";
    private static final class Fixture {
        final Object battle=new Object(),p1=new Object(),p2=new Object(),p3=new Object();
        final Object privatePacket=new Object(),publicPacket=new Object();
        final NativeSplitDispatchObservation.Session session=
                new NativeSplitDispatchObservation.Session();
        final List<Object> actors=List.of(p1,p2);
        NativeSplitDispatchObservation.Report attempt(int turn,int history){
            return session.observeNativeReturn(battle,actors,p1,privatePacket,
                    publicPacket,turn,history,2,true,false);
        }
    }
    private static void deny(List<?> actors,Object battle,Object target,
            Object privatePacket,Object publicPacket,int turn,int history,
            int spectators,boolean pinned,boolean ended,
            NativeSplitDispatchObservation.Verdict expected,String why){
        var s=new NativeSplitDispatchObservation.Session();
        var r=s.observeNativeReturn(battle,actors,target,privatePacket,publicPacket,
                turn,history,spectators,pinned,ended);
        check(r.verdict()==expected && !r.current() && s.withdrawn()
                && !r.networkSendAcknowledged() && !r.recipientDeliveryAuthenticated(),
                why+" denied");
        check(!r.toString().contains(SECRET)
                && !r.toString().contains("PRIVATE")
                && !r.toString().contains("p1a:"),
                why+" redacted");
    }
    static void run(){
        var f=new Fixture();
        var one=f.attempt(1,4);
        check(one.verdict()==NativeSplitDispatchObservation.Verdict.NATIVE_METHOD_RETURN_OBSERVED
                && one.current() && one.sendMethodReturned()
                && one.intendedRecipientRolesObserved()
                && one.nativeDispatchOrdinal()==1 && one.observedTurn()==1
                && one.historyChunksAtReturn()==4 && one.privateTargetActorCount()==1
                && one.publicActorCount()==1 && one.spectatorCount()==2,
                "real method RETURN observability with redacted recipient-role counts");
        check(one.schema().equals(NativeSplitDispatchObservation.SCHEMA)
                && one.source().equals(PublicSplitRoutingGate.SOURCE)
                && !one.networkSendAcknowledged()
                && !one.recipientDeliveryAuthenticated()
                && !one.clientMessageProcessed()
                && !one.originalSplitPayloadLinked()
                && !one.publicReadinessLeaseAuthorized()
                && !one.privatePayloadExportable()
                && !one.shadowSearchAuthorized()
                && !one.tacticalDecisionAuthorized()
                && !one.livePromotionAuthorized(),
                "native invocation return never becomes client delivery/tactics");
        check(!one.toString().contains(SECRET)
                && !one.toString().contains("p1a:")
                && !one.toString().contains("UUID")
                && !one.toString().contains("packet"),
                "reports do not expose private identities or packet data");
        var repeated=f.attempt(1,4);
        check(repeated.nativeDispatchOrdinal()==2 && repeated.current() && !one.current(),
                "separate native method returns create fresh single-use observation ordinals");
        var progression=f.attempt(2,5);
        check(progression.nativeDispatchOrdinal()==3 && progression.current()
                && !repeated.current(),
                "turn and history progress, previous report expires");
        f.session.withdraw();
        check(!progression.current()
                && f.attempt(3,6).verdict()==NativeSplitDispatchObservation.Verdict.WITHDRAWN,
                "lifecycle revocation is terminal");

        var g=new Fixture();
        var prior=g.attempt(3,10);
        var rewind=g.attempt(2,10);
        check(rewind.verdict()==NativeSplitDispatchObservation.Verdict.STALE_SOURCE
                && g.session.withdrawn() && !prior.current(),
                "native turn rewind permanently poisons observer");
        var h=new Fixture();
        h.attempt(3,10);
        check(h.attempt(3,9).verdict()
                ==NativeSplitDispatchObservation.Verdict.STALE_SOURCE
                && h.session.withdrawn(),
                "native history truncation permanently poisons observer");
        var swapped=new Fixture();
        swapped.attempt(1,2);
        check(swapped.session.observeNativeReturn(new Object(),swapped.actors,
                swapped.p1,swapped.privatePacket,swapped.publicPacket,
                1,3,0,true,false).verdict()
                ==NativeSplitDispatchObservation.Verdict.CROSS_BATTLE
                && swapped.session.withdrawn(),
                "separate battle cannot reuse source observation session");
        var changedSource=new Fixture();
        changedSource.attempt(1,2);
        check(changedSource.session.observeNativeReturn(changedSource.battle,
                changedSource.actors,changedSource.p1,changedSource.privatePacket,
                changedSource.publicPacket,1,3,0,false,false).verdict()
                ==NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE
                && changedSource.session.withdrawn(),
                "version drift revokes an otherwise current receipt");
        var sourceGone=new Fixture();
        sourceGone.attempt(1,2);
        check(sourceGone.session.observeNativeReturn(sourceGone.battle,
                sourceGone.actors,sourceGone.p1,sourceGone.privatePacket,
                sourceGone.publicPacket,1,3,0,true,true).verdict()
                ==NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE
                && sourceGone.session.withdrawn(),
                "ended battle cannot mint dispatch evidence");

        var base=new Fixture();
        deny(base.actors,base.battle,new Object(),
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.FOREIGN_RECIPIENT,
                "foreign target actor");
        deny(List.of(base.p1,base.p1),base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.FOREIGN_RECIPIENT,
                "duplicate same actor reference");
        deny(List.of(base.p2,base.p3),base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.FOREIGN_RECIPIENT,
                "private recipient absent from roster");
        deny(List.of(base.p1),base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.FOREIGN_RECIPIENT,
                "single-actor ambiguity");
        deny(Arrays.asList(base.p1,null),base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.FOREIGN_RECIPIENT,
                "null actor in original roster");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.privatePacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.INVALID_PACKETS,
                "same public and private packet object cannot prove separation");
        deny(base.actors,base.battle,base.p1,
                null,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.INVALID_PACKETS,
                "missing private packet");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,null,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.INVALID_PACKETS,
                "missing public packet");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.publicPacket,-1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "invalid native turn");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,-1,0,true,false,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "invalid native history epoch");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,-1,true,false,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "invalid spectator metadata");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,false,false,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "unsupported native source version");
        deny(base.actors,null,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,false,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "no battle source object");
        deny(base.actors,base.battle,base.p1,
                base.privatePacket,base.publicPacket,1,4,0,true,true,
                NativeSplitDispatchObservation.Verdict.SOURCE_UNAVAILABLE,
                "ended battle source");
        var untrusted=new Fixture();
        var pseudo=untrusted.attempt(0,0);
        check(pseudo.current() && !pseudo.originalSplitPayloadLinked()
                && !pseudo.recipientDeliveryAuthenticated(),
                "zero history confirms only method-return boundary, not source custody");

        // Package-private synthetic fixture claims from Phase 6.17 must
        // never be reusable as true native delivery or packet fingerprints.
        var history=OfflineShadowResponseScenariosRegression.frames("Scratch");
        var split=new ArrayList<>(history);
        split.add("update\n|split|p1\n|-damage|p1a: Own|41/100\n"
                +"|-damage|p1a: Own|50/100");
        var structural=new PublicSplitRoutingGate.Session().inspect(base.battle,split,true);
        var recipient=new NativeSplitRecipientCustody.Session().inspect(base.battle,structural);
        check(recipient.verdict()==NativeSplitRecipientCustody.Verdict.DELIVERY_UNOBSERVED
                && !recipient.nativeRecipientDeliveryAuthenticated()
                && !pseudo.recipientDeliveryAuthenticated(),
                "native source and 6.17 structural evidence remain non-joined");

        System.out.println("Phase 6.18 native dispatch observation PASS: "
                +checks+" adversarial checks");
    }
}
