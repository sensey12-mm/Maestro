package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.31: synthetic native opponent-side custody and non-authority. */
final class NativeOpponentSideProvenanceRegression {
    private NativeOpponentSideProvenanceRegression(){}
    private static int checks;
    private static final String SECRET="PRIVATE_OPPONENT_631";
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.31: "+why);
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
    private static List<String> appended(String event){
        var x=new ArrayList<>(history());
        x.add(event);
        return List.copyOf(x);
    }
    private record Fixture(Object battle,Object a,Object b,Object src,
            Object ownSide,Object otherSide,List<Object> actors,
            List<String> history,
            NativeSidedDispatchReachability.Session nativeOwner,
            NativeSidedDispatchReachability.Report nativeReturn,
            PublicSplitRoutingGate.Session routingOwner,
            PublicSplitRoutingGate.Report routing,
            IntegrationRuntimeReadinessGate.Report readiness,
            PublicOpponentIdentityContinuity.Report identity,
            ShadowCompetitiveRuntimeBridge.Report bridge,
            NativeOpponentSideProvenance.Session sideSession,
            NativeOpponentSideProvenance.Report nativeOnly){}

    private static final ShadowCompetitiveRuntimeBridge.HistoricalDescriptor OFFLINE=
            ShadowCompetitiveRuntimeBridge.describeOffline(
                    PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport(),
                    PublicShadowBuildHandoff.Pin.pinned(),
                    PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());

    private static Fixture setup(boolean ownIsP2,List<String> history,
                                  boolean bridgeRequired){
        Object battle=new Object(),a=new Object(),b=new Object();
        Object ownSide=new Object(),otherSide=new Object();
        var actors=List.of(a,b);
        Object src=ownIsP2?b:a;
        var nativeOwner=new NativeSidedDispatchReachability.Session();
        var nativeReceipt=nativeOwner.nativeReturn(battle,actors,src,
                new Object(),new Object(),false,1,history.size(),true,false);
        var routingOwner=new PublicSplitRoutingGate.Session();
        var routing=routingOwner.inspect(battle,history,true);
        var runtime=new IntegrationRuntimeReadinessGate.Session().inspect(
                battle,nativeReceipt,routing);
        var identity=new PublicOpponentIdentityContinuity.Session().inspect(
                battle,routing,history);
        ShadowCompetitiveRuntimeBridge.Report bridge=null;
        if(bridgeRequired){
            bridge=new ShadowCompetitiveRuntimeBridge.Session()
                    .inspectWithContinuity(battle,OFFLINE,runtime,identity);
        }
        var sideSession=new NativeOpponentSideProvenance.Session();
        var nativeOnly=sideSession.observeSyntheticNativeReturn(
                battle,actors,src,ownIsP2?a:b,src,
                ownIsP2?"p2":"p1",ownIsP2?"p1":"p2",
                ownSide,otherSide,nativeReceipt);
        check(nativeReceipt.current() && routing.current() && runtime.current()
                && identity.current() && nativeOnly.current()
                && nativeOnly.verdict()
                    ==NativeOpponentSideProvenance.Verdict.NATIVE_SIDE_OBSERVED_UNLINKED,
                "exact native callback with original actors forms unlinked side observation");
        return new Fixture(battle,a,b,src,ownSide,otherSide,
                actors,history,nativeOwner,nativeReceipt,
                routingOwner,routing,runtime,identity,bridge,sideSession,nativeOnly);
    }
    private static void noAuthority(NativeOpponentSideProvenance.Report r){
        check(!r.persistentPokemonIdentityAuthenticated()
                && !r.partySlotIdentityProven()
                && !r.historicalOpponentBuildMatched()
                && !r.publicActorPokemonIdentityProven()
                && !r.clientPacketDelivered()
                && !r.phase5OnlineAuthorized()
                && !r.calibratedOpponentProbabilities()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized()
                && !r.privatePayloadExportable(),
                "public side cannot identify Pokemon, authorize Phase 5 or choose");
        String text=r.toString();
        check(!text.contains(SECRET) && !text.contains("p2a:")
                && !text.contains("Genesect") && !text.contains("Greninja")
                && !text.contains("java.lang.Object@")
                && text.contains("pokemonIdentity=UNKNOWN")
                && text.contains("phase5=OFFLINE_ONLY")
                && text.contains("authority=NONE"),
                "no native/private/actor identifiers or replay data exported");
    }
    static void run() throws Exception {
        var left=setup(false,history(),true);
        var bound=left.sideSession().inspect(left.battle(),left.readiness(),
                left.identity(),left.bridge());
        check(bound.current() && bound.publicSideSourceAligned()
                && bound.opponentPublicSide()==NativeOpponentSideProvenance.Side.P2
                && bound.nativeReturnOrdinal()==left.nativeReturn()
                    .observedSidedReturnOrdinal()
                && bound.publicHistoryRevision()==left.routing().revision(),
                "native-owned p1 maps opponent public side p2 with original source");
        noAuthority(bound);
        check(left.nativeOnly().opponentPublicSide()
                    ==NativeOpponentSideProvenance.Side.UNKNOWN
                && !left.nativeOnly().publicSideSourceAligned(),
                "native callback without exact 6.29/6.30 cannot authorize public side");
        check(left.sideSession().inspect(left.battle(),left.readiness(),
                    left.identity(),left.bridge()).verdict()
                    ==NativeOpponentSideProvenance.Verdict.REPLAYED_OR_REGRESSED
                && left.sideSession().withdrawn() && !bound.current(),
                "replaying linked same source revokes previous side receipt");
        check(left.sideSession().inspect(left.battle(),left.readiness(),
                    left.identity(),left.bridge()).verdict()
                    ==NativeOpponentSideProvenance.Verdict.WITHDRAWN,
                "withdrawal terminal");

        var rotatedSource=setup(false,history(),false);
        var beforeOtherActor=rotatedSource.nativeOnly();
        var nextReturn=rotatedSource.nativeOwner().nativeReturn(
                rotatedSource.battle(),rotatedSource.actors(),
                rotatedSource.b(),new Object(),new Object(),false,
                1,history().size(),true,false);
        var renewed=rotatedSource.sideSession().observeSyntheticNativeReturn(
                rotatedSource.battle(),rotatedSource.actors(),
                rotatedSource.a(),rotatedSource.b(),rotatedSource.b(),
                "p1","p2",rotatedSource.ownSide(),rotatedSource.otherSide(),
                nextReturn);
        check(nextReturn.current() && renewed.current()
                && !beforeOtherActor.current()
                && renewed.nativeReturnOrdinal()>beforeOtherActor.nativeReturnOrdinal(),
                "source actor may legitimately alternate without side or Pokemon reassignment");
        noAuthority(renewed);

        var right=setup(true,history(),true);
        var reverse=right.sideSession().inspect(right.battle(),right.readiness(),
                right.identity(),right.bridge());
        check(reverse.current()
                && reverse.opponentPublicSide()==NativeOpponentSideProvenance.Side.P1,
                "native-owned p2 maps opponent to p1, never assuming p2 opponent");
        noAuthority(reverse);

        // A forged native actor may reuse the same displayed side or have
        // value-equal strings. Original identity, not string equality, wins.
        var v=setup(false,history(),true);
        var spoof=new NativeOpponentSideProvenance.Session();
        var forged=spoof.observeSyntheticNativeReturn(
                v.battle(),v.actors(),new Object(),v.b(),v.src(),
                "p1","p2",v.ownSide(),v.otherSide(),v.nativeReturn());
        check(forged.verdict()==NativeOpponentSideProvenance.Verdict.SIDE_TOPOLOGY_AMBIGUOUS
                && spoof.withdrawn(),
                "fabricated own actor cannot join original native actor set");
        var wrongSource=new NativeOpponentSideProvenance.Session();
        var wrongReceipt=wrongSource.observeSyntheticNativeReturn(
                v.battle(),v.actors(),v.a(),v.b(),new Object(),
                "p1","p2",v.ownSide(),v.otherSide(),v.nativeReturn());
        check(wrongReceipt.verdict()==NativeOpponentSideProvenance.Verdict.ACTOR_SET_UNTRUSTED,
                "foreign native callback source cannot reuse original return");
        var swapped=new NativeOpponentSideProvenance.Session();
        check(swapped.observeSyntheticNativeReturn(v.battle(),v.actors(),
                    v.a(),v.b(),v.src(),"p2","p1",
                    v.ownSide(),v.otherSide(),v.nativeReturn())
                    .verdict()==NativeOpponentSideProvenance.Verdict.NATIVE_SIDE_OBSERVED_UNLINKED,
                "synthetic labels alone are NOT a trusted native runtime side mapping");
        // Actual production reads showdown IDs from BattleActor and validates
        // battle.getActor(...) identity. This synthetic test cannot claim that.

        var sameSide=new NativeOpponentSideProvenance.Session();
        check(sameSide.observeSyntheticNativeReturn(v.battle(),v.actors(),v.a(),v.b(),
                    v.src(),"p1","p2",v.ownSide(),v.ownSide(),v.nativeReturn())
                    .verdict()==NativeOpponentSideProvenance.Verdict.SIDE_TOPOLOGY_AMBIGUOUS,
                "one native side for both actors is ambiguous");
        var sameLabel=new NativeOpponentSideProvenance.Session();
        check(sameLabel.observeSyntheticNativeReturn(v.battle(),v.actors(),v.a(),v.b(),
                    v.src(),"p1","p1",v.ownSide(),v.otherSide(),v.nativeReturn())
                    .verdict()==NativeOpponentSideProvenance.Verdict.SIDE_TOPOLOGY_AMBIGUOUS,
                "non-complementary p1/p2 labels cannot prove opposing side");
        var extra=new NativeOpponentSideProvenance.Session();
        check(extra.observeSyntheticNativeReturn(v.battle(),
                    List.of(v.a(),v.b(),new Object()),v.a(),v.b(),
                    v.src(),"p1","p2",v.ownSide(),v.otherSide(),v.nativeReturn())
                    .verdict()==NativeOpponentSideProvenance.Verdict.ACTOR_SET_UNTRUSTED,
                "unsupported multi-actor sides cannot be silently mapped");

        var different=setup(false,history(),true);
        // Source ownership: same revision/turn, new original receipts cannot
        // be mixed, even inside the same physical battle object.
        var foreign=left.sideSession().inspect(different.battle(),
                different.readiness(),different.identity(),different.bridge());
        check(foreign.verdict()==NativeOpponentSideProvenance.Verdict.WITHDRAWN,
                "withdrawn sessions cannot be borrowed across battles");

        var mismatched=setup(false,history(),true);
        var fakeRuntime=new IntegrationRuntimeReadinessGate.Session();
        Object x=new Object(),y=new Object();
        var otherNative=new NativeSidedDispatchReachability.Session()
                .nativeReturn(mismatched.battle(),List.of(x,y),x,
                        new Object(),new Object(),false,1,history().size(),true,false);
        var otherRouting=new PublicSplitRoutingGate.Session()
                .inspect(mismatched.battle(),history(),true);
        var similar=fakeRuntime.inspect(mismatched.battle(),otherNative,otherRouting);
        var mixed=mismatched.sideSession().inspect(mismatched.battle(),
                similar,mismatched.identity(),mismatched.bridge());
        check(mixed.verdict()==NativeOpponentSideProvenance.Verdict.SOURCE_CUSTODY_MISMATCH
                && mismatched.sideSession().withdrawn(),
                "same native metadata with different actor set/receipt fails closed");

        var stale=setup(false,history(),true);
        var old=stale.sideSession().inspect(stale.battle(),stale.readiness(),
                stale.identity(),stale.bridge());
        stale.routingOwner().inspect(stale.battle(),
                appended("update\n|move|p2a: Opp|Scratch|p1a: Own"),true);
        check(!old.current() && old.opponentPublicSide()
                    ==NativeOpponentSideProvenance.Side.UNKNOWN
                && stale.sideSession().inspect(stale.battle(),stale.readiness(),
                    stale.identity(),stale.bridge()).verdict()
                    ==NativeOpponentSideProvenance.Verdict.SOURCE_STALE,
                "public history advancement revokes opposite-side identification");
        noAuthority(old);

        String[] uncertainty={
                "update\n|replace|p2a: Opp|Zoroark, L100|100/100",
                "update\n|-transform|p2a: Opp|p1a: Own",
                "update\n|detailschange|p2a: Opp|Greninja-Ash, L100|100/100",
                "update\n|-formechange|p2a: Opp|Greninja-Ash",
                split("p2","|switch|p2a: Opp|Zoroark, L100|80/100",
                        "|switch|p2a: Opp|Zoroark, L100|100/100"),
                split("p2","|switch|p2b: Opp2|Blissey, L100|80/100",
                        "|switch|p2b: Opp2|Blissey, L100|100/100")};
        for(int i=0;i<uncertainty.length;i++){
            var fixture=setup(false,appended(uncertainty[i]),false);
            check(fixture.identity().verdict()
                    ==PublicOpponentIdentityContinuity.Verdict.IDENTITY_AMBIGUOUS,
                    "Illusion, transform, form, slot reuse/multi-active stays uncertain "+i);
            var refused=fixture.sideSession().inspect(fixture.battle(),
                    fixture.readiness(),fixture.identity(),null);
            check(refused.verdict()==NativeOpponentSideProvenance.Verdict.MISSING_BRIDGE
                    && !refused.publicSideSourceAligned(),
                    "ambiguous side observation cannot bypass Phase 6.30 "+i);
            noAuthority(refused);
        }
        var lifecycle=setup(false,history(),true);
        var oldScope=lifecycle.sideSession().inspect(lifecycle.battle(),
                lifecycle.readiness(),lifecycle.identity(),lifecycle.bridge());
        check(!lifecycle.sideSession().battleEnded(new Object()) && oldScope.current(),
                "late unrelated end does not revoke active physical battle");
        check(lifecycle.sideSession().battleEnded(lifecycle.battle())
                && !oldScope.current(),
                "actual native battle end revokes the side mapping");

        var offline=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(offline.purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                "Phase 5 online mode still rejected");
        var ai=Files.readString(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("LivePromotionGate.choose(")
                && ai.contains("null, null, false);")
                && !ai.contains("NativeOpponentSideProvenance.Session.inspect(")
                && !ai.contains("ShadowCompetitiveRuntimeBridge.Session.inspect("),
                "Legacy chooser still cannot use Shadow native-side evidence");
        System.out.println("Phase 6.31 native side provenance PASS: "+checks
                +" synthetic checks; Minecraft source and hook untested");
    }
}
