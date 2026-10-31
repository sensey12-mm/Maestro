package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.33 synthetic source/identity and privacy regression. */
final class NativeOpponentPokemonContinuityRegression {
    private NativeOpponentPokemonContinuityRegression(){}
    private static int checks;
    private static final String PRIVATE="PRIVATE_633_HIDDEN_UUID_123";
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Phase 6.33: "+why);
        checks++;
    }
    private static String split(String side,String priv,String visible){
        return "update\n|split|"+side+"\n"+priv+"\n"+visible;
    }
    private static List<String> history(){
        return List.of(
                split("p1","|switch|p1a: OWN_TRUE_PRIVATE_UUID|Genesect, L100|80/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: "+PRIVATE+"|Greninja, L100|35/100",
                        "|switch|p2a: PublicDisplay|Greninja, L100|100/100"),
                "update\n|turn|1");
    }
    private static List<String> append(List<String> previous,String chunk){
        var next=new ArrayList<>(previous);
        next.add(chunk);
        return List.copyOf(next);
    }
    private static final ShadowCompetitiveRuntimeBridge.HistoricalDescriptor OFFLINE=
            ShadowCompetitiveRuntimeBridge.describeOffline(
                    PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport(),
                    PublicShadowBuildHandoff.Pin.pinned(),
                    PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());

    private record Fixture(Object battle,PublicSplitRoutingGate.Session routingOwner,
                           PublicSplitRoutingGate.Report routing,
                           ShadowLiveCompetitiveConsumerSafety.Session safetyOwner,
                           ShadowLiveCompetitiveConsumerSafety.Report safety,
                           NativeOpponentSideProvenance.Session sideOwner,
                           NativeOpponentSideProvenance.Report side,
                           NativeOpponentSideProvenance.Side expectedOpposite){}

    private static Fixture source(Object battle,boolean ownIsP2,List<String> original){
        Object own=new Object(),other=new Object(),nativeOwnSide=new Object(),nativeOppSide=new Object();
        var actors=List.of(own,other);
        var nativeSource=new NativeSidedDispatchReachability.Session()
                .nativeReturn(battle,actors,own,new Object(),new Object(),
                        false,1,original.size(),true,false);
        var routingOwner=new PublicSplitRoutingGate.Session();
        var routing=routingOwner.inspect(battle,original,true);
        var runtime=new IntegrationRuntimeReadinessGate.Session().inspect(
                battle,nativeSource,routing);
        var identity=new PublicOpponentIdentityContinuity.Session().inspect(
                battle,routing,original);
        var bridge=new ShadowCompetitiveRuntimeBridge.Session().inspectWithContinuity(
                battle,OFFLINE,runtime,identity);
        var sideOwner=new NativeOpponentSideProvenance.Session();
        var nativeOnly=sideOwner.observeSyntheticNativeReturn(
                battle,actors,own,other,own,ownIsP2?"p2":"p1",
                ownIsP2?"p1":"p2",nativeOwnSide,nativeOppSide,nativeSource);
        var side=sideOwner.inspect(battle,runtime,identity,bridge);
        var safetyOwner=new ShadowLiveCompetitiveConsumerSafety.Session();
        var safety=safetyOwner.inspect(battle,OFFLINE,side,bridge,
                ShadowLiveCompetitiveConsumerSafety.Request.preparationPinned());
        check(nativeSource.current() && routing.current()
                && nativeOnly.verdict()==NativeOpponentSideProvenance.Verdict.NATIVE_SIDE_OBSERVED_UNLINKED
                && identity.current()
                && (bridge.current()==side.current())
                && (side.current()==safety.current()),
                "genuine original source chain never outlives an uncertain identity");
        return new Fixture(battle,routingOwner,routing,safetyOwner,safety,sideOwner,
                side,ownIsP2?NativeOpponentSideProvenance.Side.P1:
                        NativeOpponentSideProvenance.Side.P2);
    }
    private static void noAuthority(NativeOpponentPokemonContinuity.Report report){
        check(!report.publicOpponentPokemonIdentityProven()
                && !report.samePokemonAcrossSwitchesProven()
                && !report.samePokemonOnReentryProven()
                && !report.illusionTrueIdentityRevealed()
                && !report.transformSourceIdentityProven()
                && !report.hiddenNativeUuidExportable()
                && !report.historicalReplayAppearanceMatched()
                && !report.historicalBuildCandidatesAvailable()
                && !report.phase5OnlineAuthorized()
                && !report.shadowSearchAuthorized()
                && !report.tacticalDecisionAuthorized()
                && !report.livePromotionAuthorized()
                && !report.privatePayloadExportable(),
                "no source continuity can authorize a Pokémon identity or tactic");
        String text=report.toString();
        check(!text.contains(PRIVATE) && !text.contains("OWN_TRUE_PRIVATE_UUID")
                && !text.contains("PublicDisplay") && !text.contains("Greninja")
                && !text.contains("Genesect") && !text.contains("Zoroark")
                && !text.contains("p1a:") && !text.contains("p2a:")
                && !text.contains("java.lang.Object@")
                && text.contains("pokemon=UNKNOWN")
                && text.contains("reentry=UNLINKED")
                && text.contains("historicalJoin=BLOCKED")
                && text.contains("phase5=OFFLINE_ONLY")
                && text.contains("authority=NONE"),
                "original hidden identifiers, names and private frames never exported");
    }
    static void run() throws Exception {
        var original=history();
        var f=source(new Object(),false,original);
        check(f.safety().current()
                && f.safety().sharesOriginalRouting(f.battle(),f.routing()),
                "Phase 6.32 original source history attested");
        var session=new NativeOpponentPokemonContinuity.Session();
        check(session.inspect(f.battle(),null,f.routing(),original).verdict()
                    ==NativeOpponentPokemonContinuity.Verdict.MISSING_SIDE_CONTRACT
                && !session.withdrawn(),
                "missing source-owned side contract abstains");
        check(session.inspect(f.battle(),f.safety(),null,original).verdict()
                    ==NativeOpponentPokemonContinuity.Verdict.MISSING_ORIGINAL_SOURCE
                && !session.withdrawn(),
                "missing original source history abstains");
        var report=session.inspect(f.battle(),f.safety(),f.routing(),original);
        check(report.current()
                && report.verdict()==NativeOpponentPokemonContinuity.Verdict
                    .PUBLIC_APPEARANCE_POSITION_ONLY_UNBOUND
                && report.opponentPublicSide()==NativeOpponentSideProvenance.Side.P2
                && report.publicAppearanceEntries()==1
                && report.publiclyObservedActions()==0
                && report.nativeCallbackOrdinal()>0
                && report.uncertainty().equals(java.util.Set.of(
                    NativeOpponentPokemonContinuity.Uncertainty
                        .PUBLIC_POSITION_ONLY_NOT_POKEMON_IDENTITY)),
                "first public entry is an appearance only, never actual Pokémon identity");
        noAuthority(report);
        var duplicate=session.inspect(f.battle(),f.safety(),f.routing(),original);
        check(duplicate.verdict()==NativeOpponentPokemonContinuity.Verdict.REPLAYED_OR_REGRESSED
                && session.withdrawn() && !report.current()
                && report.opponentPublicSide()==NativeOpponentSideProvenance.Side.UNKNOWN,
                "replayed exact source epoch revokes old appearance receipt");
        noAuthority(duplicate);

        var reverse=source(new Object(),true,original);
        var rev=new NativeOpponentPokemonContinuity.Session().inspect(
                reverse.battle(),reverse.safety(),reverse.routing(),original);
        check(rev.current()
                && rev.opponentPublicSide()==NativeOpponentSideProvenance.Side.P1
                && rev.publicAppearanceEntries()==1,
                "native p2 owner must derive opponent side p1 from original source");
        noAuthority(rev);
        var withMove=append(original,"update\n|move|p2a: PublicDisplay|Scratch|p1a: Own");
        var actionFixture=source(new Object(),false,withMove);
        var action=new NativeOpponentPokemonContinuity.Session().inspect(
                actionFixture.battle(),actionFixture.safety(),
                actionFixture.routing(),withMove);
        check(action.current() && action.publiclyObservedActions()==1
                && !action.publicOpponentPokemonIdentityProven(),
                "a public move on same active position is not proof of stable Pokémon ID");
        noAuthority(action);

        // Distinct history source with same battle, count, public turn and
        // nickname is not the original Phase 6.16 receipt.
        var sourceOwner=source(new Object(),false,original);
        var copied=new PublicSplitRoutingGate.Session()
                .inspect(sourceOwner.battle(),original,true);
        var spoof=new NativeOpponentPokemonContinuity.Session();
        var spoofed=spoof.inspect(sourceOwner.battle(),sourceOwner.safety(),
                copied,original);
        check(spoofed.verdict()==NativeOpponentPokemonContinuity.Verdict.SOURCE_CUSTODY_MISMATCH
                && spoof.withdrawn() && !spoofed.current(),
                "replayed public bytes from different owner cannot join original source");
        noAuthority(spoofed);
        var changed=new ArrayList<>(original);
        changed.set(1,split("p2",
                "|switch|p2a: "+PRIVATE+"|Greninja, L100|25/100",
                "|switch|p2a: PublicDisplay|Greninja, L100|100/100"));
        var altered=new NativeOpponentPokemonContinuity.Session();
        var tampered=altered.inspect(sourceOwner.battle(),sourceOwner.safety(),
                sourceOwner.routing(),changed);
        check(tampered.verdict()==NativeOpponentPokemonContinuity.Verdict.SOURCE_CUSTODY_MISMATCH
                && altered.withdrawn(),
                "even private split-half changes fail original salted custody");
        noAuthority(tampered);
        var foreign=new NativeOpponentPokemonContinuity.Session();
        check(foreign.inspect(new Object(),sourceOwner.safety(),
                    sourceOwner.routing(),original).verdict()
                    ==NativeOpponentPokemonContinuity.Verdict.WRONG_BATTLE,
                "foreign physical battle is not a valid source");
        check(foreign.withdrawn(),"wrong battle terminal");
        var stale=source(new Object(),false,original);
        var still=new NativeOpponentPokemonContinuity.Session().inspect(
                stale.battle(),stale.safety(),stale.routing(),original);
        stale.routingOwner().inspect(stale.battle(),
                append(original,"update\n|move|p2a: PublicDisplay|Scratch|p1a: Own"),true);
        check(!still.current() &&
                still.opponentPublicSide()==NativeOpponentSideProvenance.Side.UNKNOWN,
                "history advance dynamically revokes current report");

        // A directly inspected public projection CAN classify appearances,
        // but without the original pinned chain it cannot produce an accepted
        // source Report. Public UUIDs are never compared as identity evidence.
        String[] ambiguous={
                "update\n|switch|p2a: PublicDisplay|Zoroark, L100|100/100",
                split("p2","|switch|p2a: "+PRIVATE+"|Zoroark, L100|95/100",
                        "|switch|p2a: PublicDisplay|Zoroark, L100|100/100"),
                split("p2","|drag|p2a: "+PRIVATE+"|Ditto, L100|95/100",
                        "|drag|p2a: PublicDisplay|Ditto, L100|100/100"),
                "update\n|replace|p2a: PublicDisplay|Zoroark, L100|100/100",
                "update\n|-end|p2a: PublicDisplay|Illusion",
                "update\n|-transform|p2a: PublicDisplay|p1a: Own",
                "update\n|detailschange|p2a: PublicDisplay|Zoroark-Hisui, L100|100/100",
                "update\n|-formechange|p2a: PublicDisplay|Zoroark-Hisui",
                split("p2","|switch|p2b: Second|Blissey, L100|80/100",
                        "|switch|p2b: Second|Blissey, L100|100/100"),
                "update\n|faint|p2a: PublicDisplay"};
        for(int i=0;i<ambiguous.length;i++){
            var h=append(original,ambiguous[i]);
            var scan=NativeOpponentPokemonContinuity.scanPublicHistory(h,
                    NativeOpponentSideProvenance.Side.P2);
            check(!scan.unambiguousPositionOnly(),
                    "event without stable public identity must abstain "+i);
            var uncertain=source(new Object(),false,h);
            var consumer=new NativeOpponentPokemonContinuity.Session();
            var rejected=consumer.inspect(uncertain.battle(),uncertain.safety(),
                    uncertain.routing(),h);
            check(!rejected.current() && !rejected.sourceBoundPublicAppearanceOnly()
                    && !rejected.samePokemonAcrossSwitchesProven()
                    && !rejected.samePokemonOnReentryProven(),
                    "ambiguous event never becomes verified opponent Pokémon "+i);
            noAuthority(rejected);
        }

        var unrelated=source(new Object(),false,original);
        var endedObserver=new NativeOpponentPokemonContinuity.Session();
        var beforeEnd=endedObserver.inspect(unrelated.battle(),
                unrelated.safety(),unrelated.routing(),original);
        check(!endedObserver.battleEnded(new Object()) && beforeEnd.current(),
                "late foreign end cannot withdraw active battle");
        check(endedObserver.battleEnded(unrelated.battle())
                && endedObserver.withdrawn() && !beforeEnd.current()
                && endedObserver.inspect(unrelated.battle(),
                    unrelated.safety(),unrelated.routing(),original).verdict()
                    ==NativeOpponentPokemonContinuity.Verdict.WITHDRAWN,
                "real battle end revokes and cannot revive");

        check(PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned()
                    .purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !OFFLINE.phase5OnlineAuthorized(),
                "Phase 5 only offline: no current replay or candidate consumer");
        String chooser=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(chooser.contains("LivePromotionGate.choose(")
                && chooser.contains("null, null, false);")
                && !chooser.contains("NativeOpponentPokemonContinuity.")
                && !chooser.contains("NativeOpponentPokemonContinuity.Session"),
                "production live chooser does not invoke Phase 6.33");
        System.out.println("Phase 6.33 source-bound opponent appearance PASS: "+
                checks+" synthetic/privacy checks; Minecraft UNTESTED");
    }
}
