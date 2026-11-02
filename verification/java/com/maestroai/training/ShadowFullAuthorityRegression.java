package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Promotion 6: synthetic native-legal completion and fallback regressions. */
final class ShadowFullAuthorityRegression {
    private static int checks;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Shadow Promotion 6: "+why);
        checks++;
    }
    private static BattleSnapshot position(List<String> moves,List<String> switches){
        var own=new BattleSnapshot.Combatant("OWN",.65,List.of("normal"),
                Map.of("atk",0),List.of(),null,null,false);
        var unknown=new BattleSnapshot.Combatant("UNVERIFIED",-1,List.of(),
                Map.of(),List.of(),null,null,false);
        var reserves=switches.stream().map(id->new BattleSnapshot.Combatant(
                "OWN",.9,List.of("water"),Map.of(),List.of(),null,null,false)).toList();
        return new BattleSnapshot(17,own,unknown,reserves,List.of(),
                moves,switches,Map.of(),false,null);
    }
    static void run()throws Exception{
        var unsupported=position(List.of("unknownstatus"),List.of());
        check(ShadowLiveDecisionEngine.ordinary(unsupported,false,false,true)==null,
                "ordinary scoring refuses unsupported effect utility");
        var nativeChoice=ShadowLiveDecisionEngine.minimumNativeLegal(
                unsupported,false,false,true,Map.of());
        check(nativeChoice!=null && nativeChoice.action().equals("move:unknownstatus")
                && nativeChoice.reason()==ShadowLiveDecisionEngine.Reason.NATIVE_LEGAL_MINIMUM,
                "current native-legal unsupported scoring yields independent action");
        check(ShadowLiveDecisionEngine.minimumNativeLegal(
                unsupported,false,false,false,Map.of())==null
                && ShadowLiveDecisionEngine.minimumNativeLegal(
                    unsupported,true,false,true,Map.of())==null
                && ShadowLiveDecisionEngine.minimumNativeLegal(
                    null,false,false,true,Map.of())==null,
                "stale, forced, null own windows never authorize minimum move");
        var attack=position(List.of("unknownstatus","nativeattack"),List.of());
        var attackFacts=new ShadowLiveDecisionEngine.OwnMoveFacts(
                "nativeattack","normal","physical",70,100,0);
        check(ShadowLiveDecisionEngine.minimumNativeLegal(
                attack,false,false,true,Map.of("nativeattack",attackFacts))
                .action().equals("move:nativeattack"),
                "prefer source-attested own attack over unsupported effects");
        check(ShadowLiveDecisionEngine.minimumNativeLegal(
                position(List.of("repeat","repeat"),List.of()),
                false,false,true,Map.of())==null,
                "duplicated native legal roots abstain");
        check(ShadowLiveDecisionEngine.minimumNativeLegal(
                position(List.of(),List.of("legal-own-1")),
                false,false,true,Map.of()).action().equals("switch:legal-own-1")
                && ShadowLiveDecisionEngine.minimumNativeLegal(
                    position(List.of(),List.of("legal-own-1")),
                    false,true,true,Map.of())==null,
                "no usable moves selects a certified own reserve only if not trapped");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("fainted",0,false),
                new ShadowLiveDecisionEngine.Reserve("a",.55,true),
                new ShadowLiveDecisionEngine.Reserve("b",.9,true)),true)
                .action().equals("switch:b"),
                "fainted own active yields independent best eligible forced replacement");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("a",.5,true),
                new ShadowLiveDecisionEngine.Reserve("a",.3,true)),true)==null,
                "duplicated forced-own party identities fail closed");

        String ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int start=ai.indexOf("private ShadowPromotionPreparation.Report prepareShadowForcedReplacement(");
        int finish=ai.indexOf("private ShadowNativeLegalWindow.Proof captureShadowNativeLegalWindow(",start);
        String forced=ai.substring(start,finish);
        check(!forced.contains("!actor.getMustChoose()")
                && !forced.contains("!actor.getResponses().isEmpty()")
                && forced.contains("request.getForceSwitch().contains(true)")
                && forced.contains("ownChoiceLease.current()")
                && forced.contains("reserve.canBeSentOut()")
                && forced.contains("reserve.isSentOut()")
                && forced.contains("ShadowPromotionPreparation.inspectForced("),
                "forced path uses original owning native forced lease and own reserve legality "
                    +"without transient callbacks bookkeeping");
        int choose=ai.indexOf("public ShowdownActionResponse choose(");
        int legacy=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        String live=ai.substring(choose,legacy);
        check(live.contains("ShadowLiveDecisionEngine.minimumNativeLegal(")
                && live.contains("ShadowLiveDecisionEngine.ordinary(")
                && live.contains("prepareShadowForcedReplacement(")
                && live.contains("currentNativeGimmickAllowed(")
                && live.contains("action.isValid(")
                && live.contains("LivePromotionGate.choosePreparedWithSource(")
                && live.contains("forcedReadinessReason")
                && !live.contains("ShowdownActionResponse legacy=chooseLegacy("),
                "independent minimum policy and forced fallback diagnostic "
                    +"run before lazy emergency Legacy");
        System.out.println("Shadow Promotion 6 native authority PASS: "+checks+
                " synthetic assertions; Minecraft NOT TESTED");
    }
}
