package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.30: adversarial integration of ORIGINAL Phase 6.28/6.29 receipts. */
final class ShadowOpponentIdentityIntegrationRegression {
    private ShadowOpponentIdentityIntegrationRegression(){}
    private static int checks;
    private static final String PRIVATE="PRIVATE_NEVER_EXPORT_630";
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError("Phase 6.30: "+message);
        checks++;
    }
    private static String split(String side,String priv,String pub){
        return "update\n|split|"+side+"\n"+priv+"\n"+pub;
    }
    private static List<String> history(){
        return List.of(
                split("p1","|switch|p1a: Own|Genesect, L100|80/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: Opp|Greninja, L100|50/100",
                        "|switch|p2a: Opp|Greninja, L100|100/100"),
                "update\n|turn|1");
    }
    private static List<String> with(String extra){
        var list=new ArrayList<>(history());
        list.add(extra);
        return List.copyOf(list);
    }
    private record Sources(
            PublicSplitRoutingGate.Session routingOwner,
            NativeSidedDispatchReachability.Session nativeOwner,
            IntegrationRuntimeReadinessGate.Session runtimeOwner,
            PublicOpponentIdentityContinuity.Session identityOwner,
            PublicSplitRoutingGate.Report routing,
            IntegrationRuntimeReadinessGate.Report runtime,
            PublicOpponentIdentityContinuity.Report identity) {}
    private static Sources validSources(Object battle,List<String> original){
        var routingOwner=new PublicSplitRoutingGate.Session();
        var nativeOwner=new NativeSidedDispatchReachability.Session();
        var runtimeOwner=new IntegrationRuntimeReadinessGate.Session();
        var identityOwner=new PublicOpponentIdentityContinuity.Session();
        var routing=routingOwner.inspect(battle,original,true);
        Object a=new Object(),b=new Object();
        var nativeReport=nativeOwner.nativeReturn(battle,
                List.of(a,b),a,new Object(),new Object(),
                false,1,original.size(),true,false);
        var runtime=runtimeOwner.inspect(battle,nativeReport,routing);
        var identity=identityOwner.inspect(battle,routing,original);
        check(routing.current() && runtime.current() && identity.current(),
                "original Phase 6.16 source/6.24 native/6.29 continuity valid");
        return new Sources(routingOwner,nativeOwner,runtimeOwner,identityOwner,
                routing,runtime,identity);
    }
    private static void noAuthority(ShadowCompetitiveRuntimeBridge.Report report){
        check(!report.publicAppearanceIdentityEstablished()
                && !report.historicalActorMatchedToCurrentOpponent()
                && !report.publicOpponentActorAuthenticated()
                && !report.displayAppearanceIsPersistentIdentity()
                && !report.historicalCandidatesExposed()
                && !report.opponentActionsKnown()
                && !report.calibratedOpponentProbabilities()
                && !report.phase5OnlineAuthorized()
                && !report.completeInstalledLegalSetProven()
                && !report.privatePayloadExportable()
                && !report.shadowSearchAuthorized()
                && !report.tacticalDecisionAuthorized()
                && !report.livePromotionAuthorized(),
                "source agreement never becomes Pokemon identity or tactical permission");
        String r=report.toString();
        check(!r.contains(PRIVATE) && !r.contains("p2a:")
                && !r.contains("Greninja") && !r.contains("Zoroark")
                && !r.contains("java.lang.Object@") && !r.contains("nastyplot")
                && r.contains("historicalJoin=BLOCKED")
                && r.contains("phase5=OFFLINE_ONLY") && r.contains("authority=NONE"),
                "source-only diagnostic cannot export private or historical data");
    }
    static void run() throws Exception {
        var descriptor=ShadowCompetitiveRuntimeBridge.describeOffline(
                PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport(),
                PublicShadowBuildHandoff.Pin.pinned(),
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());
        Object battle=new Object();
        var original=history();
        var originalSources=validSources(battle,original);
        check(originalSources.identity().verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                && originalSources.identity().sharesRuntimeRouting(battle,originalSources.runtime())
                && !originalSources.identity().persistentPokemonIdentityConfirmed(),
                "exact native routing receipt shared; no stable opponent asserted");
        var gate=new ShadowCompetitiveRuntimeBridge.Session();
        check(gate.inspectWithContinuity(battle,descriptor,
                        originalSources.runtime(),null).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.MISSING_CONTINUITY
                && !gate.withdrawn(),
                "missing continuity abstains, never falls back to caller identity");
        var admitted=gate.inspectWithContinuity(battle,descriptor,
                originalSources.runtime(),originalSources.identity());
        check(admitted.current() && admitted.publicContinuitySourceAligned()
                && admitted.verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.PREPARED_UNBOUND_OFFLINE_ONLY
                && admitted.observedTurn()==1
                && admitted.observedHistoryChunks()==original.size()
                && admitted.publicRevision()==originalSources.routing().revision(),
                "only exact original source may prepare unbound Shadow bridge");
        noAuthority(admitted);
        check(admitted.toString().contains("continuity=SOURCE_ALIGNED_UNBOUND"),
                "source agreement reported WITHOUT opponent authentication");

        check(gate.inspectWithContinuity(battle,descriptor,
                    originalSources.runtime(),originalSources.identity()).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.REPLAYED_OR_REGRESSED
                && gate.withdrawn() && !admitted.current(),
                "replayed same callback/identity pair revokes bridge permanently");
        check(gate.inspectWithContinuity(battle,descriptor,
                    originalSources.runtime(),originalSources.identity()).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.WITHDRAWN,
                "withdrawn bridge cannot be revived");

        // Different original Phase 6.16 SOURCE sessions with identical bytes,
        // same battle, revision and turn are not interchangeable.
        var lookalike=validSources(battle,original);
        var crossed=new ShadowCompetitiveRuntimeBridge.Session();
        var mismatch=crossed.inspectWithContinuity(battle,descriptor,
                originalSources.runtime(),lookalike.identity());
        check(mismatch.verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.SOURCE_CUSTODY_MISMATCH
                && crossed.withdrawn()
                && originalSources.runtime().publicRevision()==lookalike.identity().sourceRevision()
                && originalSources.runtime().observedTurn()==lookalike.identity().publicTurn(),
                "same-looking turn/revision but independent source custody fails closed");
        noAuthority(mismatch);

        var otherBattle=new ShadowCompetitiveRuntimeBridge.Session();
        var wrong=otherBattle.inspectWithContinuity(new Object(),descriptor,
                lookalike.runtime(),lookalike.identity());
        check(wrong.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.WRONG_BATTLE
                && otherBattle.withdrawn(),"foreign physical battle cannot borrow identity");

        var lostSource=new ShadowCompetitiveRuntimeBridge.Session();
        var staleIdentity=lookalike.identity();
        var extended=new ArrayList<>(original);
        extended.add("update\n|move|p2a: Opp|Scratch|p1a: Own");
        lookalike.routingOwner().inspect(battle,extended,true);
        var stale=lostSource.inspectWithContinuity(battle,descriptor,
                lookalike.runtime(),staleIdentity);
        check(!staleIdentity.current() && !lookalike.runtime().current()
                && stale.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.SOURCE_STALE
                && lostSource.withdrawn(),
                "new history immediately withdraws both original receipts");
        noAuthority(stale);

        // Identity ambiguities MUST NOT be accepted even with the exact
        // same source-owned routing used by native and public observers.
        String[] ambiguous={
                "update\n|replace|p2a: Opp|Zoroark, L100|100/100",
                "update\n|-transform|p2a: Opp|p1a: Own",
                "update\n|detailschange|p2a: Opp|Greninja-Ash, L100|100/100",
                "update\n|-formechange|p2a: Opp|Greninja-Ash",
                "update\n|-end|p2a: Opp|Illusion",
                split("p2","|switch|p2a: Opp|Zoroark, L100|70/100",
                        "|switch|p2a: Opp|Zoroark, L100|100/100")};
        for(int i=0;i<ambiguous.length;i++){
            Object anotherBattle=new Object();
            var source=validSources(anotherBattle,with(ambiguous[i]));
            check(source.identity().verdict()
                        ==PublicOpponentIdentityContinuity.Verdict.IDENTITY_AMBIGUOUS,
                    "ambiguous public re-entry/Illusion/Transform/form event stays UNKNOWN "+i);
            var newBridge=new ShadowCompetitiveRuntimeBridge.Session();
            var result=newBridge.inspectWithContinuity(anotherBattle,descriptor,
                    source.runtime(),source.identity());
            check(result.verdict()==ShadowCompetitiveRuntimeBridge.Verdict.IDENTITY_UNCERTAIN
                    && newBridge.withdrawn() && !result.current()
                    && !result.publicContinuitySourceAligned(),
                    "ambiguous identity rejected even with current exact source "+i);
            noAuthority(result);
        }

        // Source-owned actor and public opponent identity are distinct:
        // even a no-ambiguity outcome can never supply Phase 5 live builds.
        var ownerCheck=validSources(new Object(),original);
        var noOnline=new ShadowCompetitiveRuntimeBridge.Session();
        check(noOnline.inspectWithContinuity(new Object(),descriptor,
                    ownerCheck.runtime(),ownerCheck.identity()).verdict()
                    ==ShadowCompetitiveRuntimeBridge.Verdict.WRONG_BATTLE,
                "matching timeline with mismatched native battle never binds");
        var request=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(request.purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !descriptor.phase5OnlineAuthorized(),
                "Phase 5 remains strictly offline");
        String chooser=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(chooser.contains("LivePromotionGate.choose(")
                && chooser.contains("null, null, false);")
                && !chooser.contains("inspectWithContinuity(")
                && !chooser.contains("ShadowCompetitiveRuntimeBridge.")
                && !chooser.contains("PublicOpponentIdentityContinuity."),
                "production Legacy choice does not invoke any Shadow runtime bridge");
        System.out.println("Phase 6.30 source-identity integration PASS: "
                +checks+" synthetic/adversarial checks; Minecraft untested");
    }
}
