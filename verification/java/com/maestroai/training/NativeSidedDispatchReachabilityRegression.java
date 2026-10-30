package com.maestroai.training;

import java.util.List;

/** Phase 6.24: synthetic contract tests; NOT a native runtime claim. */
final class NativeSidedDispatchReachabilityRegression {
    private NativeSidedDispatchReachabilityRegression(){}
    private static int count;
    private static void require(boolean ok,String reason){
        if(!ok)throw new AssertionError("Phase 6.24: "+reason);
        count++;
    }
    private static final String SECRET="PRIVATE_PLAYER_UUID_AND_PAYLOAD_NEVER_EXPORT";
    private static NativeSidedDispatchReachability.Report returned(
            NativeSidedDispatchReachability.Session session,Object battle,
            Object source,List<?> actors,int turn,int history,boolean allyToSpectators){
        return session.nativeReturn(battle,actors,source,new Object(),
                new Object(),allyToSpectators,turn,history,true,false);
    }
    private static void denyAll(NativeSidedDispatchReachability.Report r){
        require(!r.nativeSplitMethodReturnedProven()
                && !r.originalShowdownSplitLinked()
                && !r.perRecipientSendAcknowledged()
                && !r.clientDeliveryAuthenticated()
                && !r.spectatorDeliveryAuthenticated()
                && !r.publicFrameAuthenticated()
                && !r.eventIdentityAuthenticated()
                && !r.privatePayloadExportable()
                && !r.completeInstalledLegalSetProven()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized(),
                "no native return authorizes split identity, delivery or tactics");
        String safe=r.toString();
        require(!safe.contains(SECRET) && !safe.contains("p1a:")
                && !safe.contains("NetworkPacket") && !safe.contains("java.lang.Object@")
                && safe.contains("delivery=UNVERIFIED")
                && safe.contains("showdownSplit=UNLINKED")
                && safe.contains("authority=NONE"),
                "no private request, raw packet or actor identity in report");
    }
    static void run(){
        Object battle=new Object(),one=new Object(),two=new Object();
        List<Object> actors=List.of(one,two);
        var session=new NativeSidedDispatchReachability.Session();
        var absent=session.status(battle,0,0,true,false);
        require(absent.verdict()
                ==NativeSidedDispatchReachability.Verdict.NO_NATIVE_SIDED_CALLBACK_YET
                && !absent.nativeSidedMethodReturned(),
                "optional Mixin has no fictional activation");
        denyAll(absent);

        var first=returned(session,battle,one,actors,0,2,false);
        require(first.verdict()
                ==NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED
                && first.current() && first.observedSidedReturnOrdinal()==1
                && first.spectatorRouting()
                    ==NativeSidedDispatchReachability.SpectatorRouting.OPPONENT_BRANCH,
                "native sided return only, not authenticated delivery");
        denyAll(first);
        // Pinned interpreter can send multiple distinct packets without
        // updating Showdown source chunks or turn between callbacks.
        var second=returned(session,battle,two,actors,0,2,true);
        require(second.current() && second.observedSidedReturnOrdinal()==2
                && second.spectatorRouting()
                    ==NativeSidedDispatchReachability.SpectatorRouting.ALLY_BRANCH
                && !first.current(),
                "same-epoch valid returns increment only a local callback ordinal");
        denyAll(second);
        var future=returned(session,battle,one,actors,1,3,false);
        require(future.observedSidedReturnOrdinal()==3 && future.current()
                && !second.current(),
                "forward turn/history advances local callback witness");
        require(session.status(battle,1,3,true,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED,
                "read-only status observes existing callback, not one more call");
        require(session.status(battle,1,3,true,false).observedSidedReturnOrdinal()==3,
                "repeated status cannot forge native callback ordinal");
        denyAll(future);

        var duplicateActors=new NativeSidedDispatchReachability.Session();
        require(duplicateActors.nativeReturn(battle,List.of(one,one),
                one,new Object(),new Object(),false,0,1,true,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.FOREIGN_SOURCE_ACTOR
                && duplicateActors.withdrawn(),
                "duplicate actor identity is terminal");
        var foreignActor=new NativeSidedDispatchReachability.Session();
        require(returned(foreignActor,battle,new Object(),actors,0,1,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.FOREIGN_SOURCE_ACTOR,
                "foreign source actor is refused");
        var badPackets=new NativeSidedDispatchReachability.Session();
        Object samePacket=new Object();
        require(badPackets.nativeReturn(battle,actors,one,samePacket,samePacket,
                false,0,1,true,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.INVALID_PACKETS,
                "same object masquerading as both sides denied");
        var nullPacket=new NativeSidedDispatchReachability.Session();
        require(nullPacket.nativeReturn(battle,actors,one,null,new Object(),
                false,0,1,true,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.INVALID_PACKETS,
                "missing packet object aborts callback observation");
        var sourcePin=new NativeSidedDispatchReachability.Session();
        require(sourcePin.nativeReturn(battle,actors,one,new Object(),new Object(),
                false,0,1,false,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.SOURCE_UNAVAILABLE
                && sourcePin.withdrawn(),
                "unsupported installed runtime source fails closed");
        var badEpoch=new NativeSidedDispatchReachability.Session();
        returned(badEpoch,battle,one,actors,2,5,false);
        require(returned(badEpoch,battle,one,actors,1,5,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.REGRESSED_SOURCE
                && badEpoch.withdrawn(),
                "out-of-order native epoch cannot claim freshness");
        var badHistory=new NativeSidedDispatchReachability.Session();
        returned(badHistory,battle,one,actors,2,5,false);
        require(returned(badHistory,battle,two,actors,2,4,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.REGRESSED_SOURCE,
                "history count regression on callback poisons lineage");
        var cross=new NativeSidedDispatchReachability.Session();
        returned(cross,battle,one,actors,1,1,false);
        require(returned(cross,new Object(),one,actors,1,2,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.CROSS_BATTLE,
                "foreign physical battle cannot reuse observation session");
        require(cross.status(battle,2,3,true,false).verdict()
                ==NativeSidedDispatchReachability.Verdict.WITHDRAWN,
                "cross battle withdrawal is terminal");
        var actorSwap=new NativeSidedDispatchReachability.Session();
        returned(actorSwap,battle,one,actors,1,1,false);
        require(returned(actorSwap,battle,one,List.of(one,new Object()),1,2,false)
                .verdict()==NativeSidedDispatchReachability.Verdict.FOREIGN_SOURCE_ACTOR,
                "actor set replacement is rejected");
        var statusOld=new NativeSidedDispatchReachability.Session();
        returned(statusOld,battle,one,actors,3,6,false);
        require(statusOld.status(battle,2,6,true,false).verdict()
                ==NativeSidedDispatchReachability.Verdict.REGRESSED_SOURCE,
                "stale polling does not preserve false current source");
        var malformedStatus=new NativeSidedDispatchReachability.Session();
        require(malformedStatus.status(battle,1,0,false,false).verdict()
                ==NativeSidedDispatchReachability.Verdict.SOURCE_UNAVAILABLE,
                "missing source-pin on status refused");
        var end=new NativeSidedDispatchReachability.Session();
        var endReturn=returned(end,battle,one,actors,1,2,false);
        require(end.battleEnded(battle).verdict()
                ==NativeSidedDispatchReachability.Verdict.WITHDRAWN
                && !endReturn.current() && end.withdrawn(),
                "battle-end callback revokes all earlier sided reports");
        require(end.nativeReturn(battle,actors,one,new Object(),new Object(),
                false,2,3,true,false).verdict()
                ==NativeSidedDispatchReachability.Verdict.WITHDRAWN,
                "ended observer cannot be resurrected");
        var fresh=new NativeSidedDispatchReachability.Session();
        require(fresh.status(new Object(),0,0,true,false).verdict()
                ==NativeSidedDispatchReachability.Verdict.NO_NATIVE_SIDED_CALLBACK_YET,
                "new physical battle has zero callback evidence");

        // 6.25: the initial native callbacks happen before first AI choice.
        // They must survive Legacy's first-choice memory reset.
        Object openingBattle=new Object();
        var scope=new NativeSidedDispatchReachability.BattleScope();
        var early=scope.forBattle(openingBattle);
        var e1=returned(early,openingBattle,one,actors,0,5,false);
        var e2=returned(early,openingBattle,two,actors,0,5,false);
        require(e1.observedSidedReturnOrdinal()==1
                && e2.observedSidedReturnOrdinal()==2,
                "native events before first choice have real ordinals");
        require(scope.forBattle(openingBattle)==early,
                "first choice preserves exact same-battle source");
        var firstChoice=scope.forBattle(openingBattle)
                .status(openingBattle,0,7,true,false);
        require(firstChoice.current()
                && firstChoice.observedSidedReturnOrdinal()==2
                && firstChoice.verdict()
                    ==NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_OBSERVED_UNVERIFIED,
                "first choice never reports NO callback after real early sends");
        denyAll(firstChoice);
        var later=returned(scope.forBattle(openingBattle),
                openingBattle,one,actors,1,8,false);
        require(later.current() && later.observedSidedReturnOrdinal()==3
                && !e2.current(),
                "post-choice callbacks extend existing ordinal");
        scope.withdraw();
        require(scope.forBattle(openingBattle)==early
                && scope.forBattle(openingBattle)
                    .status(openingBattle,1,8,true,false).verdict()
                    ==NativeSidedDispatchReachability.Verdict.WITHDRAWN,
                "same battle cannot revive withdrawn session at another choice");
        Object nextBattle=new Object();
        var next=scope.forBattle(nextBattle);
        require(next!=early && !later.current()
                && next.status(nextBattle,0,0,true,false).verdict()
                    ==NativeSidedDispatchReachability.Verdict.NO_NATIVE_SIDED_CALLBACK_YET,
                "new battle starts independent with no callbacks");
        require(scope.end(openingBattle)==null
                && scope.forBattle(nextBattle)==next,
                "late previous-battle end never withdraws new session");
        var nextFirst=returned(next,nextBattle,two,actors,0,2,true);
        require(nextFirst.current() && nextFirst.observedSidedReturnOrdinal()==1,
                "new battle's initial callback is ordinal one");
        var ended=scope.end(nextBattle);
        require(ended!=null
                && ended.verdict()==NativeSidedDispatchReachability.Verdict.WITHDRAWN
                && next.withdrawn() && !nextFirst.current(),
                "native battle-end revokes original source and returns redacted receipt");
        denyAll(ended);
        require(scope.forBattle(nextBattle)==next
                && scope.forBattle(nextBattle)
                    .nativeReturn(nextBattle,actors,one,new Object(),new Object(),
                            false,1,3,true,false).verdict()
                    ==NativeSidedDispatchReachability.Verdict.WITHDRAWN,
                "late same-battle callbacks after end stay withdrawn");
        Object futureBattle=new Object();
        var future=scope.forBattle(futureBattle);
        var futureFirst=returned(future,futureBattle,one,actors,0,2,false);
        require(futureFirst.current()
                && futureFirst.observedSidedReturnOrdinal()==1
                && scope.forBattle(futureBattle)==future,
                "future native dispatch precedes future first choice safely");

        System.out.println("Phase 6.24 native-sided reachability regression PASS: 
                +count+" synthetic assertions");
    }
}
