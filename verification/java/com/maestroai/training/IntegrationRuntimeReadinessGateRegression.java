package com.maestroai.training;

import com.maestroai.ai.LivePromotionGate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic-only independent runtime gate adversarial tests. */
final class IntegrationRuntimeReadinessGateRegression {
    private IntegrationRuntimeReadinessGateRegression(){}
    private static int checks;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Maestro4567 runtime: "+why);
        checks++;
    }
    private static final String SECRET="PRIVATE_BATTLE_ACTOR_NEVER_LOG";
    private static List<String> history(){
        var source=new ArrayList<>(OfflineShadowResponseScenariosRegression.frames("Scratch"));
        source.add("update\n|split|p1\n|-damage|p1a: "+SECRET+"|56/100\n"
                +"|-damage|p1a: "+SECRET+"|60/100");
        return List.copyOf(source);
    }
    private static NativeSidedDispatchReachability.Report callback(
            NativeSidedDispatchReachability.Session session,Object battle,
            Object actor,List<?> actors,int turn,int chunks){
        return session.nativeReturn(battle,actors,actor,new Object(),new Object(),
                false,turn,chunks,true,false);
    }
    private static void denyAuthority(IntegrationRuntimeReadinessGate.Report report){
        check(!report.nativeHookInstalledProven()
                && !report.publicActorAuthenticated()
                && !report.nativeSplitEventLinked()
                && !report.perRecipientDeliveryAuthenticated()
                && !report.privatePayloadExportable()
                && !report.installedMinecraftAccepted()
                && !report.completeLegalActionsProven()
                && !report.phase5OnlineAuthorized()
                && !report.shadowSearchAuthorized()
                && !report.tacticalDecisionAuthorized()
                && !report.livePromotionAuthorized(),
                "no state can grant Shadow/Phase 5/Legacy promotion or delivery");
        String line=report.toString();
        check(!line.contains(SECRET) && !line.contains("p1a:")
                && !line.contains("java.lang.Object@")
                && line.contains("eventLink=UNVERIFIED")
                && line.contains("authority=NONE"),
                "redacted numeric-only diagnostics");
    }
    static void run() throws Exception {
        Object battle=new Object(), p1=new Object(), p2=new Object();
        var actors=List.of(p1,p2);
        var source=new PublicSplitRoutingGate.Session();
        var nativeSession=new NativeSidedDispatchReachability.Session();
        var gate=new IntegrationRuntimeReadinessGate.Session();
        var raw=history();
        var routing=source.inspect(battle,raw,true);
        check(routing.current() && routing.splitPairs()==1 && routing.publicTurn()==1,
                "independent structural private-first/public-second witness");
        var absent=nativeSession.status(battle,1,raw.size(),true,false);
        check(gate.inspect(battle,absent,routing).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.NO_NATIVE_RETURN,
                "missing optional native callback cannot be fabricated");
        var sided=callback(nativeSession,battle,p1,actors,1,raw.size());
        var positive=gate.inspect(battle,sided,routing);
        check(positive.current()
                && positive.verdict()==IntegrationRuntimeReadinessGate.Verdict.SOURCE_WINDOWS_ALIGNED_UNVERIFIED
                && positive.nativeActorMembershipObserved()
                && positive.publicPrivateStructureObserved()
                && positive.sourceEpochAligned()
                && positive.publicRevision()==routing.revision()
                && positive.nativeReturnOrdinal()==sided.observedSidedReturnOrdinal(),
                "original same-battle, same-epoch receipts are observational only");
        denyAuthority(positive);
        check(gate.inspect(battle,sided,routing).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.REPLAYED_OR_DUPLICATE
                && gate.withdrawn() && !positive.current(),
                "replaying original receipt poisons session and withdraws previous result");
        check(gate.inspect(battle,sided,routing).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.WITHDRAWN,
                "withdrawal is irreversible");

        var newGate=new IntegrationRuntimeReadinessGate.Session();
        var newer=source.inspect(battle,append(raw,
                "update\n|move|p2a: Opp|Scratch|p1a: Own"),true);
        check(!routing.current() && !newGate.inspect(battle,sided,routing).current()
                && newGate.withdrawn(),
                "public history advancing revokes stale structural receipt");
        check(!sided.current()
                || nativeSession.status(battle,1,raw.size()+1,true,false).verdict()
                    ==NativeSidedDispatchReachability.Verdict.NATIVE_SIDED_RETURN_PRIOR_EPOCH,
                "source advancement withdraws native RETURN freshness");
        check(!sided.current(),"old native report is stale after epoch polling");

        var wrongBattle=new IntegrationRuntimeReadinessGate.Session();
        var foreignSource=new PublicSplitRoutingGate.Session();
        var foreignRouting=foreignSource.inspect(new Object(),raw,true);
        check(wrongBattle.inspect(battle,sided,foreignRouting).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.WRONG_BATTLE
                && wrongBattle.withdrawn(),
                "foreign public session cannot be substituted");
        var foreignNative=new NativeSidedDispatchReachability.Session();
        var foreignReceipt=callback(foreignNative,new Object(),p1,actors,1,raw.size());
        var sameSource=new PublicSplitRoutingGate.Session();
        var sameRouting=sameSource.inspect(battle,raw,true);
        check(new IntegrationRuntimeReadinessGate.Session()
                    .inspect(battle,foreignReceipt,sameRouting).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.WRONG_BATTLE,
                "foreign native actor/battle receipt cannot be substituted");

        var turnMismatch=new NativeSidedDispatchReachability.Session();
        check(new IntegrationRuntimeReadinessGate.Session()
                    .inspect(battle,callback(turnMismatch,battle,p1,actors,2,raw.size()),sameRouting)
                    .verdict()==IntegrationRuntimeReadinessGate.Verdict.TURN_OR_EPOCH_MISMATCH,
                "turn mismatch is terminal");
        var countMismatch=new NativeSidedDispatchReachability.Session();
        check(new IntegrationRuntimeReadinessGate.Session()
                    .inspect(battle,callback(countMismatch,battle,p1,actors,1,raw.size()-1),sameRouting)
                    .verdict()==IntegrationRuntimeReadinessGate.Verdict.TURN_OR_EPOCH_MISMATCH,
                "history chunk count mismatch is terminal");

        var noSplitSource=new PublicSplitRoutingGate.Session();
        var noSplit=noSplitSource.inspect(battle,
                OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        var noSplitGate=new IntegrationRuntimeReadinessGate.Session();
        check(noSplitGate.inspect(battle,null,noSplit).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.NO_SPLIT_ROUTE
                && !noSplitGate.withdrawn(),
                "direct-only history never becomes private/public routing proof");
        var validAfterNoSplit=noSplitSource.inspect(battle,raw,true);
        var validNative=new NativeSidedDispatchReachability.Session();
        check(noSplitGate.inspect(battle,
                    callback(validNative,battle,p2,actors,1,raw.size()),validAfterNoSplit)
                    .current(),
                "missing-source abstention may recover only with original new source");

        var badActor=new NativeSidedDispatchReachability.Session();
        var invalid=callback(badActor,battle,new Object(),actors,1,raw.size());
        check(invalid.verdict()==NativeSidedDispatchReachability.Verdict.FOREIGN_SOURCE_ACTOR
                && new IntegrationRuntimeReadinessGate.Session()
                    .inspect(battle,invalid,sameRouting).verdict()
                    ==IntegrationRuntimeReadinessGate.Verdict.SOURCE_STALE,
                "native actor membership is required, not caller asserted");
        var badRoute=new PublicSplitRoutingGate.Session();
        var swapped=new ArrayList<>(raw);
        swapped.set(swapped.size()-1,"update\n|split|p1\n"
                +"|-damage|p1a: "+SECRET+"|60/100\n"
                +"|-heal|p1a: "+SECRET+"|56/100");
        var invalidRoute=badRoute.inspect(battle,swapped,true);
        check(!invalidRoute.current()
                && new IntegrationRuntimeReadinessGate.Session()
                    .inspect(battle,null,invalidRoute).verdict()
                    ==IntegrationRuntimeReadinessGate.Verdict.SOURCE_STALE,
                "malformed private/public split ordering cannot be laundered");

        var endedSource=new PublicSplitRoutingGate.Session();
        var endedNative=new NativeSidedDispatchReachability.Session();
        var endedGate=new IntegrationRuntimeReadinessGate.Session();
        var e=endedGate.inspect(battle,
                callback(endedNative,battle,p1,actors,1,raw.size()),
                endedSource.inspect(battle,raw,true));
        endedNative.battleEnded(battle);
        check(!e.current() && !e.nativeActorMembershipObserved(),
                "battle-end invalidates native source identity and gate freshness");
        endedGate.withdraw();
        check(endedGate.inspect(battle,null,null).verdict()
                ==IntegrationRuntimeReadinessGate.Verdict.WITHDRAWN,
                "ended session cannot revive");

        // Independent production chooser boundary; the gate never supplies
        // a promoted candidate or consumes Phase 5 offline hypotheses.
        String ai=Files.readString(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("LivePromotionGate.choose(")
                && ai.contains("null, null, false);")
                && !ai.contains("IntegrationRuntimeReadinessGate")
                && !ai.contains("PublicCompetitiveConsumerContract.ConsumerRequest")
                && !ai.contains("OfflineShadowTacticalAdapter.inspect("),
                "Legacy chooser remains isolated from observational gate and offline Phase 5");
        String before=System.getProperty(LivePromotionGate.PROPERTY);
        try {
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            var promoted=new AtomicInteger();
            var legacy=new AtomicInteger();
            String selected=LivePromotionGate.choose(
                    ()->{legacy.incrementAndGet();return "LEGACY";},
                    ()->{promoted.incrementAndGet();return "SHADOW";},
                    candidate->true,false);
            check("LEGACY".equals(selected) && legacy.get()==1 && promoted.get()==0,
                    "opt-in flag alone cannot execute Shadow chooser");
        } finally {
            if(before==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,before);
        }
        System.out.println("Maestro4567 runtime source-readiness safety PASS: "
                +checks+" synthetic checks; installed Minecraft UNTESTED");
    }
    private static List<String> append(List<String> existing,String frame){
        var copy=new ArrayList<>(existing);copy.add(frame);return List.copyOf(copy);
    }
}
