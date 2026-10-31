package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.28: adversarial Shadow-only integration preparation (synthetic). */
final class ShadowCompetitiveRuntimeBridgeRegression {
    private ShadowCompetitiveRuntimeBridgeRegression(){}
    private static int checks;
    private static final String SECRET="PRIVATE_SIMULATED_OPPONENT_NEVER_EXPORT";
    private static void check(boolean value,String explanation){
        if(!value)throw new AssertionError("Phase 6.28: "+explanation);
        checks++;
    }
    private static void reject(Runnable f,String why){
        try{f.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains(SECRET),why+" redacted rejection");
            return;
        }
        throw new AssertionError("Phase 6.28: accepted "+why);
    }
    private static List<String> history(){
        var frames=new ArrayList<>(OfflineShadowResponseScenariosRegression.frames("Scratch"));
        frames.add("update\n|split|p1\n|-damage|p1a: "+SECRET+"|56/100\n"
                +"|-damage|p1a: "+SECRET+"|60/100");
        return List.copyOf(frames);
    }
    private static void denyAll(ShadowCompetitiveRuntimeBridge.Report r){
        check(!r.historicalActorMatchedToCurrentOpponent()
                && !r.publicOpponentActorAuthenticated()
                && !r.displayAppearanceIsPersistentIdentity()
                && !r.historicalCandidatesExposed()
                && !r.opponentActionsKnown()
                && !r.calibratedOpponentProbabilities()
                && !r.phase5OnlineAuthorized()
                && !r.completeInstalledLegalSetProven()
                && !r.privatePayloadExportable()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized(),
                "bridge NEVER provides live Phase 5/tactical authority");
        var safe=r.toString();
        check(!safe.contains(SECRET) && !safe.contains("p1a:")
                && !safe.contains("java.lang.Object@")
                && !safe.contains("nastyplot")
                && safe.contains("actor=UNAUTHENTICATED")
                && safe.contains("historicalJoin=BLOCKED")
                && safe.contains("phase5=OFFLINE_ONLY")
                && safe.contains("authority=NONE"),
                "reports never disclose replay, actor, nickname or candidate");
    }
    static void run() throws Exception {
        var phase5=PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport();
        var pin=PublicShadowBuildHandoff.Pin.pinned();
        var offlineOnly=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        var descriptor=ShadowCompetitiveRuntimeBridge.describeOffline(
                phase5,pin,offlineOnly);
        check(descriptor.schema().equals(PublicShadowBuildHandoff.SCHEMA)
                && descriptor.offlineOnly()
                && !descriptor.currentBattleIdentityEstablished()
                && !descriptor.appearanceOrdinalIsPartyIdentity()
                && !descriptor.liveOpponentCandidatesAvailable()
                && !descriptor.calibratedResponseProbabilities()
                && !descriptor.phase5OnlineAuthorized()
                && !descriptor.shadowSearchAuthorized()
                && !descriptor.livePromotionAuthorized(),
                "signed Phase 5 source reduced to nonidentifying offline contract");
        check(!descriptor.toString().contains("zoroark")
                && !descriptor.toString().contains("nastyplot")
                && !descriptor.toString().contains(SECRET),
                "no Phase 5 build guesses, appearances or secrets exported");
        reject(()->ShadowCompetitiveRuntimeBridge.describeOffline(
                null,pin,offlineOnly),"fabricated historical source");
        reject(()->ShadowCompetitiveRuntimeBridge.describeOffline(
                phase5,null,offlineOnly),"missing historical pin");
        reject(()->ShadowCompetitiveRuntimeBridge.describeOffline(
                phase5,pin,null),"missing offline-only request");
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,
                offlineOnly.pin(),false,false),"unapproved online Phase 5 purpose");
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                offlineOnly.pin(),true,false),"attempt to bind Phase 5 to current battle");
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                offlineOnly.pin(),false,true),"attempt to use Phase 5 tactical inputs");

        // Original source-owned Cobblemon structural history + native RETURN.
        // No caller can invent a winning certificate through source hashes.
        Object battle=new Object(), actor1=new Object(), actor2=new Object();
        var history=history();
        var routingSession=new PublicSplitRoutingGate.Session();
        var nativeSession=new NativeSidedDispatchReachability.Session();
        var readinessSession=new IntegrationRuntimeReadinessGate.Session();
        var routing=routingSession.inspect(battle,history,true);
        check(routing.current() && routing.splitPairs()==1,
                "native synthetic history has source-verified structural split");
        var nativeReceipt=nativeSession.nativeReturn(
                battle,List.of(actor1,actor2),actor1,new Object(),new Object(),
                false,1,history.size(),true,false);
        check(nativeReceipt.current()
                && !nativeReceipt.originalShowdownSplitLinked(),
                "sided RETURN is source-owned, never split event identity");
        var readiness=readinessSession.inspect(battle,nativeReceipt,routing);
        check(readiness.current()
                && !readiness.publicActorAuthenticated()
                && !readiness.phase5OnlineAuthorized(),
                "source gate aligns metadata but proves no opponent actor");
        var bridge=new ShadowCompetitiveRuntimeBridge.Session();
        check(bridge.inspect(battle,null,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.MISSING_OFFLINE_CONTRACT,
                "missing offline source abstains without revocation");
        check(bridge.inspect(battle,descriptor,null,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.MISSING_RUNTIME_SOURCE,
                "missing original Phase 6 runtime observation abstains");
        var positive=bridge.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.PUBLIC_DISPLAY_ONLY);
        check(positive.current()
                && positive.verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.PREPARED_UNBOUND_OFFLINE_ONLY
                && positive.publicRevision()==readiness.publicRevision()
                && positive.nativeReturnOrdinal()==readiness.nativeReturnOrdinal(),
                "fresh source can prepare offline-only boundary, not join identities");
        denyAll(positive);

        var repeated=bridge.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN);
        check(repeated.verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.REPLAYED_OR_REGRESSED
                && bridge.withdrawn() && !positive.current(),
                "replaying same Phase 6 source epoch revokes prepared session");
        check(bridge.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.WITHDRAWN,
                "stale bridge cannot recover after replay withdrawal");
        denyAll(repeated);

        var forged=new ShadowCompetitiveRuntimeBridge.Session();
        var claimed=forged.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.CALLER_ASSERTED_STABLE_OPPONENT);
        check(claimed.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.UNTRUSTED_ACTOR_ASSERTION
                && forged.withdrawn(),
                "matching displayed species or replay ordinal cannot assert actor identity");
        denyAll(claimed);
        var illusion=new ShadowCompetitiveRuntimeBridge.Session();
        var ambiguous=illusion.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.ILLUSION_OR_TRANSFORM_UNRESOLVED);
        check(ambiguous.current()
                && !ambiguous.historicalActorMatchedToCurrentOpponent()
                && !ambiguous.shadowSearchAuthorized(),
                "Illusion/Transform appearance always remains unknown");
        denyAll(ambiguous);
        var wrongBattle=new ShadowCompetitiveRuntimeBridge.Session();
        var foreign=wrongBattle.inspect(new Object(),descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.PUBLIC_DISPLAY_ONLY);
        check(foreign.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.WRONG_BATTLE
                && wrongBattle.withdrawn(),
                "same numeric epoch in foreign physical battle fails closed");

        var noIdentity=new ShadowCompetitiveRuntimeBridge.Session();
        check(noIdentity.inspect(battle,descriptor,readiness,null).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.PUBLIC_ACTOR_NOT_AUTHENTICATED
                && noIdentity.withdrawn(),
                "missing identity uncertainty classification cannot be bypassed");

        var oldSource=new ShadowCompetitiveRuntimeBridge.Session();
        var old=oldSource.inspect(battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN);
        check(old.current(),"source receipt remains current before history advances");
        var longer=new ArrayList<>(history);
        longer.add("update\n|move|p2a: Opp|Scratch|p1a: Own");
        routingSession.inspect(battle,longer,true);
        nativeSession.status(battle,1,longer.size(),true,false);
        check(!old.current(),"public history/return epoch changes revoke prepared report");
        var stale=new ShadowCompetitiveRuntimeBridge.Session().inspect(
                battle,descriptor,readiness,
                ShadowCompetitiveRuntimeBridge.IdentityEvidence.PUBLIC_DISPLAY_ONLY);
        check(stale.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.SOURCE_STALE
                && !stale.current(),"stale original source cannot revive bridge");
        denyAll(stale);
        illusion.withdraw();
        check(!ambiguous.current(),"explicit lifecycle withdrawal revokes all reports");

        // Read-only integration contract is not called by Minecraft chooser.
        var ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("LivePromotionGate.choose(")
                && ai.contains("null, null, false);")
                && !ai.contains("ShadowCompetitiveRuntimeBridge.")
                && !ai.contains("PublicShadowBuildHandoff.prepare(")
                && !ai.contains("PublicCompetitiveConsumerContract.prepare("),
                "Legacy live choice has no Phase 5/6 online binding or Shadow call");
        System.out.println("Phase 6.28 Shadow-only runtime bridge preparation PASS: "
                +checks+" synthetic/source-only checks; Minecraft unverified");
    }
}
