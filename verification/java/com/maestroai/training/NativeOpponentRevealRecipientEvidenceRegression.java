package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 6.34 synthetic native RETURN, source custody and privacy regressions. */
final class NativeOpponentRevealRecipientEvidenceRegression {
    private NativeOpponentRevealRecipientEvidenceRegression(){}
    private static int checks;
    private static final String PRIVATE="HIDDEN_OPPONENT_UUID634";
    private static void check(boolean condition,String explanation){
        if(!condition)throw new AssertionError("Phase 6.34: "+explanation);
        checks++;
    }
    private static String split(String side,String priv,String pub){
        return "update\n|split|"+side+"\n"+priv+"\n"+pub;
    }
    private static List<String> history(){
        return List.of(
                split("p1","|switch|p1a: Own|Genesect, L100|72/100",
                        "|switch|p1a: Own|Genesect, L100|100/100"),
                split("p2","|switch|p2a: "+PRIVATE+"|Greninja, L100|34/100",
                        "|switch|p2a: "+PRIVATE+"|Greninja, L100|100/100"),
                "update\n|turn|1");
    }
    private static List<String> appended(String event){
        var result=new ArrayList<>(history());
        result.add(event);
        return List.copyOf(result);
    }
    private static final ShadowCompetitiveRuntimeBridge.HistoricalDescriptor OFFLINE =
            ShadowCompetitiveRuntimeBridge.describeOffline(
                    PublicShadowBuildHandoffRegression.syntheticOfflineBridgeReport(),
                    PublicShadowBuildHandoff.Pin.pinned(),
                    PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());

    private record Sources(Object battle,Object own,Object opponent,
            List<Object> actors,PublicSplitRoutingGate.Session routingOwner,
            PublicSplitRoutingGate.Report originalRouting,
            NativeSidedDispatchReachability.Session nativeOwner,
            NativeSidedDispatchReachability.Report sided,
            NativeOpponentSideProvenance.Report side,
            NativeOpponentPokemonContinuity.Report appearance,
            ShadowLiveCompetitiveConsumerSafety.Report consumer,
            NativeOpponentRevealRecipientEvidence.Session revealOwner) {}
    private static Sources create(Object battle,boolean ownP2,List<String> h){
        Object own=new Object(),opponent=new Object();
        List<Object> actors=List.of(own,opponent);
        var nativeOwner=new NativeSidedDispatchReachability.Session();
        var sided=nativeOwner.nativeReturn(battle,actors,opponent,new Object(),
                new Object(),false,1,h.size(),true,false);
        var routingOwner=new PublicSplitRoutingGate.Session();
        var routing=routingOwner.inspect(battle,h,true);
        var runtime=new IntegrationRuntimeReadinessGate.Session()
                .inspect(battle,sided,routing);
        var identity=new PublicOpponentIdentityContinuity.Session()
                .inspect(battle,routing,h);
        var bridge=new ShadowCompetitiveRuntimeBridge.Session().inspectWithContinuity(
                battle,OFFLINE,runtime,identity);
        var sideOwner=new NativeOpponentSideProvenance.Session();
        var first=sideOwner.observeSyntheticNativeReturn(
                battle,actors,own,opponent,opponent,
                ownP2?"p2":"p1",ownP2?"p1":"p2",
                new Object(),new Object(),sided);
        var side=sideOwner.inspect(battle,runtime,identity,bridge);
        var safety=new ShadowLiveCompetitiveConsumerSafety.Session().inspect(
                battle,OFFLINE,side,bridge,
                ShadowLiveCompetitiveConsumerSafety.Request.preparationPinned());
        var appearance=new NativeOpponentPokemonContinuity.Session().inspect(
                battle,safety,routing,h);
        check(sided.current() && routing.current()
                && first.verdict()==NativeOpponentSideProvenance.Verdict
                        .NATIVE_SIDE_OBSERVED_UNLINKED
                && identity.current()
                && side.current()==bridge.current()
                && safety.current()==side.current()
                && appearance.current()==safety.current(),
                "original source chain only remains current when public identity is unambiguous");
        return new Sources(battle,own,opponent,actors,routingOwner,routing,
                nativeOwner,sided,side,appearance,safety,
                new NativeOpponentRevealRecipientEvidence.Session());
    }
    private static NativeOpponentRevealRecipientEvidence.Report witnessed(
            Sources s,NativeOpponentRevealRecipientEvidence.Kind kind){
        return s.revealOwner().observeSyntheticReturn(
                s.battle(),s.actors(),s.own(),s.opponent(),s.opponent(),
                kind,s.sided());
    }
    private static void noAuthority(NativeOpponentRevealRecipientEvidence.Report r){
        check(!r.authenticatedClientDelivery()
                && !r.installedPacketReceptionConfirmed()
                && !r.underlyingPokemonIdentityPublic()
                && !r.opponentPokemonReentryIdentityProven()
                && !r.illusionTruePokemonPubliclyRevealed()
                && !r.transformOriginalIdentityProven()
                && !r.publicPokemonUUIDAvailable()
                && !r.historicalOpponentBuildMatched()
                && !r.phase5OnlineAuthorized()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized()
                && !r.privatePayloadExportable(),
                "native RETURN or public appearance never becomes delivery, identity, or action");
        var msg=r.toString();
        check(!msg.contains(PRIVATE) && !msg.contains("Greninja")
                && !msg.contains("Genesect") && !msg.contains("Zoroark")
                && !msg.contains("BattlePokemon") && !msg.contains("p2a:")
                && !msg.contains("java.lang.Object@")
                && msg.contains("delivery=UNVERIFIED")
                && msg.contains("pokemon=UNKNOWN")
                && msg.contains("historicalJoin=BLOCKED")
                && msg.contains("phase5=OFFLINE_ONLY")
                && msg.contains("authority=NONE"),
                "redacted diagnostics cannot leak private packet contents or hidden Pokemon");
    }
    static void run() throws Exception {
        var s=create(new Object(),false,history());
        check(s.appearance().current()
                && s.appearance().sharesOriginalSide(s.battle(),s.side())
                && s.side().sharesOriginalSidedReturn(s.battle(),s.sided()),
                "6.31 and 6.33 independently bind to original native 6.24 RETURN");
        var sourceOnly=witnessed(s,NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        check(sourceOnly.current()
                && sourceOnly.verdict()==NativeOpponentRevealRecipientEvidence.Verdict
                        .NATIVE_PUBLIC_BRANCH_RETURN_UNVERIFIED
                && sourceOnly.opponentPublicBranchMethodReturned()
                && !sourceOnly.publicAppearanceSourceAligned(),
                "native method RETURN is not packet receipt or public Pokemon identity");
        noAuthority(sourceOnly);
        check(s.revealOwner().inspect(s.battle(),s.side(),null).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.MISSING_CONTINUITY
                && !s.revealOwner().withdrawn(),
                "missing Phase 6.33 evidence must abstain without fabricating proof");
        var accepted=s.revealOwner().inspect(s.battle(),s.side(),s.appearance());
        check(accepted.current()
                && accepted.verdict()==NativeOpponentRevealRecipientEvidence.Verdict
                        .PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED
                && accepted.publicBranchKind()==NativeOpponentRevealRecipientEvidence.Kind
                        .SWITCH_PUBLIC_BRANCH
                && accepted.publicAppearanceSourceAligned()
                && accepted.publicHistoryRevision()==s.originalRouting().revision(),
                "original source-linked public switch remains explicitly unverified delivery");
        noAuthority(accepted);
        check(s.revealOwner().inspect(s.battle(),s.side(),s.appearance()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.REPLAYED_OR_REGRESSED
                && s.revealOwner().withdrawn() && !accepted.current(),
                "duplicate consumer of same native return revokes previous source");
        check(s.revealOwner().inspect(s.battle(),s.side(),s.appearance()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.WITHDRAWN,
                "withdrawal is terminal");

        var r=create(new Object(),true,history());
        var right=witnessed(r,NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        var reverse=r.revealOwner().inspect(r.battle(),r.side(),r.appearance());
        check(right.current() && reverse.current()
                && reverse.publicAppearanceSourceAligned()
                && r.side().opponentPublicSide()==NativeOpponentSideProvenance.Side.P1,
                "public branch works when owning Maestro side is p2, not assumed p1");
        noAuthority(reverse);

        var reveal=create(new Object(),false,history());
        var revealReceipt=witnessed(reveal,
                NativeOpponentRevealRecipientEvidence.Kind.ILLUSION_END_REPLACE_PUBLIC_BRANCH);
        check(revealReceipt.current() && !revealReceipt.illusionTruePokemonPubliclyRevealed()
                && revealReceipt.publicBranchKind()==NativeOpponentRevealRecipientEvidence.Kind
                        .ILLUSION_END_REPLACE_PUBLIC_BRANCH,
                "native Illusion-ending public packet method RETURN does not reveal true UUID");
        check(reveal.revealOwner().inspect(reveal.battle(),reveal.side(),
                    reveal.appearance()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.REVEAL_IDENTITY_UNRESOLVED
                && reveal.revealOwner().withdrawn()
                && !revealReceipt.current(),
                "even complete 6.31–6.33 source chain cannot turn Illusion reveal into identity");
        noAuthority(revealReceipt);

        var transform=create(new Object(),false,history());
        var transReceipt=witnessed(transform,
                NativeOpponentRevealRecipientEvidence.Kind.TRANSFORM_PUBLIC_BRANCH);
        check(transReceipt.current() && !transReceipt.transformOriginalIdentityProven(),
                "native Transform routing does not authenticate pre-transform Pokemon");
        check(transform.revealOwner().inspect(transform.battle(),transform.side(),
                    transform.appearance()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.REVEAL_IDENTITY_UNRESOLVED,
                "Transform must fail closed");
        noAuthority(transReceipt);

        var unknown=create(new Object(),false,history());
        check(unknown.revealOwner().observeSyntheticReturn(
                unknown.battle(),unknown.actors(),unknown.own(),unknown.opponent(),
                unknown.opponent(),NativeOpponentRevealRecipientEvidence.Kind.UNSUPPORTED,
                unknown.sided()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.UNSUPPORTED_PACKET_PAIR
                && !unknown.revealOwner().withdrawn(),
                "unrelated sided packet is ignored rather than inventing opponent reveal");
        var next=witnessed(unknown,NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        check(next.current(),"legitimate callback can follow unrelated packet");
        noAuthority(next);

        var mine=create(new Object(),false,history());
        check(mine.revealOwner().observeSyntheticReturn(
                mine.battle(),mine.actors(),mine.own(),mine.opponent(),mine.own(),
                NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH,
                mine.sided()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.FOREIGN_SOURCE_ACTOR
                && mine.revealOwner().withdrawn(),
                "own-side Pokemon packet cannot be evidence of opponent's public Pokemon");
        var fake=create(new Object(),false,history());
        var swapped=new NativeOpponentRevealRecipientEvidence.Session();
        check(swapped.observeSyntheticReturn(fake.battle(),
                    List.of(fake.own(),fake.opponent(),new Object()),
                    fake.own(),fake.opponent(),fake.opponent(),
                    NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH,
                    fake.sided()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.FOREIGN_SOURCE_ACTOR,
                "multi-actor / foreign set cannot borrow original 6.24 native report");
        var foreignSource=create(new Object(),false,history());
        var borrowed=new NativeOpponentRevealRecipientEvidence.Session();
        check(borrowed.observeSyntheticReturn(foreignSource.battle(),
                    foreignSource.actors(),foreignSource.own(),foreignSource.opponent(),
                    new Object(),NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH,
                    foreignSource.sided()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.FOREIGN_SOURCE_ACTOR,
                "forged original native actor rejected");
        var foreignBattle=create(new Object(),false,history());
        var foreignSession=new NativeOpponentRevealRecipientEvidence.Session();
        check(foreignSession.observeSyntheticReturn(new Object(),
                    foreignBattle.actors(),foreignBattle.own(),foreignBattle.opponent(),
                    foreignBattle.opponent(),
                    NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH,
                    foreignBattle.sided()).verdict()
                    ==NativeOpponentRevealRecipientEvidence.Verdict.FOREIGN_SOURCE_ACTOR,
                "native callback cannot migrate to another physical battle");
        var crossed=create(new Object(),false,history());
        var other=create(crossed.battle(),false,history());
        var crossReceipt=witnessed(crossed,
                NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        check(crossReceipt.current(),"crossed fixture original initially valid");
        var conflict=crossed.revealOwner().inspect(crossed.battle(),other.side(),
                crossed.appearance());
        check(conflict.verdict()==NativeOpponentRevealRecipientEvidence.Verdict
                    .SOURCE_CUSTODY_MISMATCH
                && crossed.revealOwner().withdrawn(),
                "matching turn and battle cannot substitute foreign Phase 6.31 side session");
        noAuthority(conflict);

        var changed=create(new Object(),false,history());
        var initial=witnessed(changed,NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        var acceptedChanged=changed.revealOwner().inspect(
                changed.battle(),changed.side(),changed.appearance());
        changed.routingOwner().inspect(changed.battle(),
                appended("update\n|move|p2a: "+PRIVATE+"|Scratch|p1a: Own"),true);
        check(initial.current()==false && acceptedChanged.current()==false,
                "public history advance invalidates linked 6.33 evidence");
        noAuthority(acceptedChanged);
        var ended=create(new Object(),false,history());
        var beforeEnd=witnessed(ended,
                NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
        check(!ended.revealOwner().battleEnded(new Object()) && beforeEnd.current(),
                "late foreign battle end cannot withdraw current observer");
        check(ended.revealOwner().battleEnded(ended.battle())
                && !beforeEnd.current() && ended.revealOwner().withdrawn(),
                "actual battle end revokes native observer");
        noAuthority(beforeEnd);

        String[] ambiguous={
                "update\n|replace|p2a: "+PRIVATE+"|Zoroark, L100|100/100",
                "update\n|-transform|p2a: "+PRIVATE+"|p1a: Own",
                "update\n|detailschange|p2a: "+PRIVATE+"|Greninja-Ash, L100|100/100",
                "update\n|-formechange|p2a: "+PRIVATE+"|Greninja-Ash",
                split("p2","|switch|p2a: "+PRIVATE+"|Greninja, L100|80/100",
                        "|switch|p2a: "+PRIVATE+"|Greninja, L100|100/100"),
                split("p2","|switch|p2b: Other|Blissey, L100|80/100",
                        "|switch|p2b: Other|Blissey, L100|100/100")};
        for(int i=0;i<ambiguous.length;i++){
            var h=appended(ambiguous[i]);
            var f=create(new Object(),false,h);
            var nativeReceipt=witnessed(f,
                    NativeOpponentRevealRecipientEvidence.Kind.SWITCH_PUBLIC_BRANCH);
            check(nativeReceipt.current() && !f.appearance().current()
                    && !nativeReceipt.publicAppearanceSourceAligned(),
                    "native callback alone cannot override ambiguous public history "+i);
            var rejected=f.revealOwner().inspect(f.battle(),f.side(),f.appearance());
            check(rejected.verdict()!=NativeOpponentRevealRecipientEvidence.Verdict
                        .PUBLIC_APPEARANCE_SOURCE_ALIGNED_UNVERIFIED
                    && !rejected.current(),
                    "Illusion/Transform/form/re-entry/multi-slot cannot gain usable source "+i);
            noAuthority(rejected);
        }

        check(PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned().purpose()
                    ==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !OFFLINE.phase5OnlineAuthorized(),
                "Phase 5 offline-only remains enforced");
        var ai=Files.readString(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(ai.contains("NativeOpponentRevealRecipientEvidence.BattleScope")
                && ai.contains("maestroai.phase634Observe")
                && ai.contains(".observeNativeReturn(battle,own,sourceActor,")
                && ai.contains("LivePromotionGate.choose(")
                && ai.contains("null, null, false);")
                && !ai.contains("NativeOpponentRevealRecipientEvidence.Session.inspect("),
                "native hook is observational and Legacy live chooser remains isolated");
        System.out.println("Phase 6.34 native public branch/reveal safety PASS: "
                +checks+" synthetic regressions; Minecraft UNTESTED");
    }
}
