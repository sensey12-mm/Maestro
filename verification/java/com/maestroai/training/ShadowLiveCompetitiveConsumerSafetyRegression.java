package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6.32: synthetic, adversarial acceptance and authority gates.
 * No actual Minecraft observation or Phase 5 online data.
 */
final class ShadowLiveCompetitiveConsumerSafetyRegression {
    private ShadowLiveCompetitiveConsumerSafetyRegression(){}
    private static int checks;
    private static final String PRIVATE="SYNTHETIC_PRIVATE_632_NEVER_EXPORTED";
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.32: "+why);
        checks++;
    }
    private static void reject(Runnable action,String why){
        try{action.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains(PRIVATE)
                    && !expected.getMessage().contains("p2a:"),why);
            return;
        }
        throw new AssertionError("Phase 6.32 accepted "+why);
    }
    private static String split(String p,String hidden,String visible){
        return "update\n|split|"+p+"\n"+hidden+"\n"+visible;
    }
    private static List<String> history(){
        return List.of(
                split("p1","|switch|p1a: Own|Genesect, L100|80/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: Opp|Greninja, L100|40/100",
                        "|switch|p2a: Opp|Greninja, L100|100/100"),
                "update\n|turn|1");
    }
    private static List<String> append(String additional){
        var x=new ArrayList<>(history());
        x.add(additional);
        return List.copyOf(x);
    }
    private static final ShadowCompetitiveRuntimeBridge.HistoricalDescriptor OFFLINE=
            ShadowCompetitiveRuntimeBridge.describeOffline(
                    PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport(),
                    PublicShadowBuildHandoff.Pin.pinned(),
                    PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());
    private static final ShadowLiveCompetitiveConsumerSafety.Request REQUEST=
            ShadowLiveCompetitiveConsumerSafety.Request.preparationPinned();

    private record Fixture(Object battle,
                           PublicSplitRoutingGate.Session routingOwner,
                           NativeSidedDispatchReachability.Session nativeOwner,
                           NativeOpponentSideProvenance.Session sideOwner,
                           ShadowCompetitiveRuntimeBridge.Session bridgeOwner,
                           ShadowCompetitiveRuntimeBridge.Report bridge,
                           NativeOpponentSideProvenance.Report side,
                           NativeOpponentSideProvenance.Report nativeOnly) {}

    private static Fixture fixture(Object physicalBattle,boolean ownIsP2,
                                   List<String> original){
        Object own=new Object(),opponent=new Object(),ownSide=new Object(),otherSide=new Object();
        var actors=List.of(own,opponent);
        var nativeOwner=new NativeSidedDispatchReachability.Session();
        var nativeReturn=nativeOwner.nativeReturn(physicalBattle,actors,own,
                new Object(),new Object(),false,1,original.size(),true,false);
        var routingOwner=new PublicSplitRoutingGate.Session();
        var routing=routingOwner.inspect(physicalBattle,original,true);
        var runtime=new IntegrationRuntimeReadinessGate.Session()
                .inspect(physicalBattle,nativeReturn,routing);
        var identity=new PublicOpponentIdentityContinuity.Session()
                .inspect(physicalBattle,routing,original);
        var bridgeOwner=new ShadowCompetitiveRuntimeBridge.Session();
        var bridge=bridgeOwner.inspectWithContinuity(
                physicalBattle,OFFLINE,runtime,identity);
        var sideOwner=new NativeOpponentSideProvenance.Session();
        var nativeOnly=sideOwner.observeSyntheticNativeReturn(
                physicalBattle,actors,own,opponent,own,
                ownIsP2?"p2":"p1",ownIsP2?"p1":"p2",
                ownSide,otherSide,nativeReturn);
        var side=sideOwner.inspect(physicalBattle,runtime,identity,bridge);
        check(nativeReturn.current() && routing.current()
                && nativeOnly.current()
                && nativeOnly.verdict()==
                    NativeOpponentSideProvenance.Verdict.NATIVE_SIDE_OBSERVED_UNLINKED,
                "unlinked native original return exists with original routing source");
        return new Fixture(physicalBattle,routingOwner,nativeOwner,
                sideOwner,bridgeOwner,bridge,side,nativeOnly);
    }

    private static void noAuthority(ShadowLiveCompetitiveConsumerSafety.Report report){
        check(!report.historicalOpponentPokemonIdentified()
                && !report.publicDisplayIsStablePokemonIdentity()
                && !report.historicalReplayAppearanceMatched()
                && !report.historicalBuildCandidatesAvailable()
                && !report.opponentResponseProbabilitiesCalibrated()
                && !report.completeLegalActionsProven()
                && !report.privatePayloadExportable()
                && !report.phase5OnlineAuthorized()
                && !report.onlineConsumptionAuthorized()
                && !report.shadowSearchAuthorized()
                && !report.tacticalDecisionAuthorized()
                && !report.legacyDecisionAuthorized()
                && !report.livePromotionAuthorized(),
                "no condition grants online intelligence, legal moves or control");
        check(report.missingCapabilities().size()
                    ==ShadowLiveCompetitiveConsumerSafety.MissingCapability.values().length
                && report.missingCapabilities().contains(
                    ShadowLiveCompetitiveConsumerSafety.MissingCapability
                        .HISTORICAL_APPEARANCE_TO_CURRENT_POKEMON_IDENTITY)
                && report.missingCapabilities().contains(
                    ShadowLiveCompetitiveConsumerSafety.MissingCapability
                        .APPROVED_VERSIONED_PHASE5_ONLINE_CONSUMER),
                "positive source-compatible report still lists every missing capability");
        var diagnostics=report.toString();
        check(!diagnostics.contains(PRIVATE) && !diagnostics.contains("p1a:")
                && !diagnostics.contains("p2a:") && !diagnostics.contains("Greninja")
                && !diagnostics.contains("Zoroark")
                && !diagnostics.contains("nastyplot")
                && !diagnostics.contains("java.lang.Object@")
                && diagnostics.contains("pokemon=UNKNOWN")
                && diagnostics.contains("historicalJoin=BLOCKED")
                && diagnostics.contains("phase5=OFFLINE_ONLY")
                && diagnostics.contains("authority=NONE"),
                "diagnostics contain no names, private messages, actors or builds");
    }

    static void run() throws Exception {
        var pinned=ShadowLiveCompetitiveConsumerSafety.ContractPin.pinned();
        check(pinned.version()==1
                && pinned.installedSource().equals(
                    ReadOnlyShadowObservationBridge.COBBLEMON_VERSION)
                && pinned.historicalSchema().equals(PublicShadowBuildHandoff.SCHEMA)
                && pinned.historicalBridgeSchema().equals(
                    ShadowCompetitiveRuntimeBridge.SCHEMA)
                && pinned.identitySchema().equals(PublicOpponentIdentityContinuity.SCHEMA)
                && pinned.opponentSideSchema().equals(NativeOpponentSideProvenance.SCHEMA)
                && pinned.phase5Pin().equals(
                    PublicCompetitiveConsumerContract.ContractPin.pinned()),
                "all original Phase 5, 6.28, 6.29 and 6.31 pins are exact");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.ContractPin(
                2,pinned.installedSource(),pinned.historicalSchema(),
                pinned.historicalBridgeSchema(),pinned.identitySchema(),
                pinned.opponentSideSchema(),pinned.phase5Pin()),
                "future version cannot claim acceptance");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.ContractPin(
                1,"future-cobblemon",pinned.historicalSchema(),
                pinned.historicalBridgeSchema(),pinned.identitySchema(),
                pinned.opponentSideSchema(),pinned.phase5Pin()),
                "unverified installed Cobblemon source cannot claim compatibility");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.ContractPin(
                1,pinned.installedSource(),pinned.historicalSchema(),
                "future-bridge",pinned.identitySchema(),
                pinned.opponentSideSchema(),pinned.phase5Pin()),
                "unverified bridge schema not accepted");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.ContractPin(
                1,pinned.installedSource(),pinned.historicalSchema(),
                pinned.historicalBridgeSchema(),pinned.identitySchema(),
                "future-side",pinned.phase5Pin()),
                "unverified opponent side schema not accepted");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.LIVE_COMPETITIVE_CONSUMPTION,
                pinned,false,false,false),"live consumption explicitly forbidden");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.SHADOW_TACTICAL_USE,
                pinned,false,false,false),"tactical consumer forbidden");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.LEGACY_CHOICE_CONTROL,
                pinned,false,false,false),"legacy chooser forbidden");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.PREPARATION_REVIEW_ONLY,
                pinned,true,false,false),"persistent current opponent join forbidden");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.PREPARATION_REVIEW_ONLY,
                pinned,false,true,false),"historical candidate export forbidden");
        reject(()->new ShadowLiveCompetitiveConsumerSafety.Request(
                ShadowLiveCompetitiveConsumerSafety.Purpose.PREPARATION_REVIEW_ONLY,
                pinned,false,false,true),"decision authorization flag forbidden");

        var original=fixture(new Object(),false,history());
        var consumer=new ShadowLiveCompetitiveConsumerSafety.Session();
        check(consumer.inspect(original.battle(),null,original.side(),
                    original.bridge(),REQUEST).verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.MISSING_OFFLINE_CUSTODY,
                "no historical source descriptor cannot fabricate a signed source");
        check(consumer.inspect(original.battle(),OFFLINE,null,
                    original.bridge(),REQUEST).verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.MISSING_RUNTIME_EVIDENCE,
                "missing source side report abstains, not an online assumption");
        check(consumer.inspect(original.battle(),OFFLINE,original.side(),
                    original.bridge(),null).verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.UNAUTHORIZED_USE
                && consumer.withdrawn(),
                "missing consumer request is terminal and non-authoritative");

        var acceptedSession=new ShadowLiveCompetitiveConsumerSafety.Session();
        var positive=acceptedSession.inspect(original.battle(),OFFLINE,
                original.side(),original.bridge(),REQUEST);
        check(positive.current()
                && positive.verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict
                        .SOURCE_EPOCH_AUDITED_OFFLINE_ONLY
                && positive.nativePublicSourceChainAligned()
                && positive.sourceBoundOpponentSide()==NativeOpponentSideProvenance.Side.P2
                && positive.sourceRevision()==original.side().publicHistoryRevision(),
                "original signed offline source + linked native/public side yields review only");
        noAuthority(positive);
        var duplicate=acceptedSession.inspect(original.battle(),OFFLINE,
                original.side(),original.bridge(),REQUEST);
        check(duplicate.verdict()==ShadowLiveCompetitiveConsumerSafety.Verdict.REPLAYED_OR_REGRESSED
                && acceptedSession.withdrawn() && !positive.current()
                && positive.sourceBoundOpponentSide()==NativeOpponentSideProvenance.Side.UNKNOWN,
                "duplicate source is rejected and revokes earlier positive report");
        noAuthority(duplicate);

        var reverse=fixture(new Object(),true,history());
        var reversed=new ShadowLiveCompetitiveConsumerSafety.Session().inspect(
                reverse.battle(),OFFLINE,reverse.side(),reverse.bridge(),REQUEST);
        check(reversed.current()
                && reversed.sourceBoundOpponentSide()==NativeOpponentSideProvenance.Side.P1,
                "native p2 owner maps opponent side p1, not assumed p2");
        noAuthority(reversed);

        // A fresh separate original source chain with matching public
        // revision, turn, native ordinal, side or displayed species MUST NOT
        // substitute the exact original 6.30 bridge accepted by 6.31.
        var sharedBattle=new Object();
        var one=fixture(sharedBattle,false,history());
        var two=fixture(sharedBattle,false,history());
        var crossed=new ShadowLiveCompetitiveConsumerSafety.Session().inspect(
                sharedBattle,OFFLINE,one.side(),two.bridge(),REQUEST);
        check(crossed.verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.SOURCE_CUSTODY_MISMATCH
                && !crossed.current()
                && one.side().publicHistoryRevision()==two.side().publicHistoryRevision(),
                "separate original Phase 6.31 source receipt cannot be bridged by same metadata");
        noAuthority(crossed);
        var borrowed=new ShadowLiveCompetitiveConsumerSafety.Session().inspect(
                new Object(),OFFLINE,one.side(),one.bridge(),REQUEST);
        check(borrowed.verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.SOURCE_CUSTODY_MISMATCH,
                "wrong physical battle cannot borrow true source receipt");

        var nativeOnly=new ShadowLiveCompetitiveConsumerSafety.Session().inspect(
                one.battle(),OFFLINE,one.nativeOnly(),one.bridge(),REQUEST);
        check(nativeOnly.verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.UNBOUND_OPPONENT_SIDE
                && nativeOnly.sourceBoundOpponentSide()
                    ==NativeOpponentSideProvenance.Side.UNKNOWN,
                "native-only callback cannot become public-side consumer evidence");
        noAuthority(nativeOnly);

        var stale=fixture(new Object(),false,history());
        var staleConsumer=new ShadowLiveCompetitiveConsumerSafety.Session();
        var current=staleConsumer.inspect(stale.battle(),OFFLINE,
                stale.side(),stale.bridge(),REQUEST);
        var advanced=append("update\n|move|p2a: Opp|Scratch|p1a: Own");
        stale.routingOwner().inspect(stale.battle(),advanced,true);
        check(!current.current()
                && current.sourceBoundOpponentSide()==NativeOpponentSideProvenance.Side.UNKNOWN
                && staleConsumer.inspect(stale.battle(),OFFLINE,stale.side(),
                    stale.bridge(),REQUEST).verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.STALE_SOURCE
                && staleConsumer.withdrawn(),
                "public history advancement dynamically revokes consumer source lease");
        noAuthority(current);

        var lifecycle=fixture(new Object(),false,history());
        var session=new ShadowLiveCompetitiveConsumerSafety.Session();
        var active=session.inspect(lifecycle.battle(),OFFLINE,
                lifecycle.side(),lifecycle.bridge(),REQUEST);
        check(active.current() && !lifecycle.sideOwner().battleEnded(new Object())
                && active.current(),"late foreign battle end not accepted");
        check(lifecycle.sideOwner().battleEnded(lifecycle.battle())
                && !active.current(),"real battle end revokes source");
        session.withdraw();
        check(session.inspect(lifecycle.battle(),OFFLINE,
                    lifecycle.side(),lifecycle.bridge(),REQUEST).verdict()
                    ==ShadowLiveCompetitiveConsumerSafety.Verdict.WITHDRAWN,
                "withdrawal is irreversible");

        String[] uncertain={
                "update\n|replace|p2a: Opp|Zoroark, L100|100/100",
                "update\n|-transform|p2a: Opp|p1a: Own",
                "update\n|detailschange|p2a: Opp|Greninja-Ash, L100|100/100",
                "update\n|-formechange|p2a: Opp|Greninja-Ash",
                split("p2","|switch|p2a: Opp|Zoroark, L100|70/100",
                        "|switch|p2a: Opp|Zoroark, L100|100/100"),
                split("p2","|switch|p2b: Other|Blissey, L100|80/100",
                        "|switch|p2b: Other|Blissey, L100|100/100")};
        for(int i=0;i<uncertain.length;i++){
            var f=fixture(new Object(),false,append(uncertain[i]));
            var result=new ShadowLiveCompetitiveConsumerSafety.Session()
                    .inspect(f.battle(),OFFLINE,f.side(),f.bridge(),REQUEST);
            check(!result.current() && !result.onlineConsumptionAuthorized()
                    && !result.nativePublicSourceChainAligned()
                    && result.sourceBoundOpponentSide()
                        ==NativeOpponentSideProvenance.Side.UNKNOWN,
                    "Illusion/Transform/form/reentry/multi-active cannot be accepted "+i);
            noAuthority(result);
        }
        check(PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned()
                    .purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !OFFLINE.phase5OnlineAuthorized(),
                "Phase 5 source consumer has not acquired live permissions");
        String chooser=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(chooser.contains("LivePromotionGate.choose(")
                && chooser.contains("null, null, false);")
                && !chooser.contains("ShadowLiveCompetitiveConsumerSafety.")
                && !chooser.contains("ShadowLiveCompetitiveConsumerSafety.Session"),
                "Legacy chooser disconnected; no Phase 6.32 live calls or tactics");
        System.out.println("Phase 6.32 live consumer SAFETY contract PASS: "
                +checks+" synthetic/source checks; Minecraft UNTESTED");
    }
}
