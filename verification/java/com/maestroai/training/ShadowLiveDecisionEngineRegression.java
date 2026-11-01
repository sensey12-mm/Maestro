package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Independent decision-policy regressions: no Legacy score, no opponent
 * private state, no synthetic probability, no live promotion authorization.
 */
final class ShadowLiveDecisionEngineRegression {
    private ShadowLiveDecisionEngineRegression(){}
    private static int checks;
    private static void check(boolean good,String why){
        if(!good)throw new AssertionError("Shadow live engine: "+why);
        checks++;
    }
    private static BattleSnapshot snapshot(double hp,List<String> moves,
            List<String> switches,List<BattleSnapshot.Combatant> reserves,
            Map<String,Integer> boosts,List<String> publiclyRevealed){
        var self=new BattleSnapshot.Combatant("self",hp,List.of("normal"),
                boosts,moves,null,null,false);
        var opponent=new BattleSnapshot.Combatant("UNKNOWN",-1,List.of(),
                Map.of(),publiclyRevealed,null,null,false);
        return new BattleSnapshot(6,self,opponent,reserves,List.of(),
                moves,switches,Map.of(),false,null);
    }
    private static BattleSnapshot.Combatant reserve(double hp){
        return new BattleSnapshot.Combatant("owned",hp,List.of(),Map.of(),
                List.of(),null,null,false);
    }
    static void run()throws Exception{
        var attack=snapshot(.85,List.of("tackle","quickattack"),
                List.of(),List.of(),Map.of(),List.of());
        var p=ShadowLiveDecisionEngine.ordinary(attack,false,false,true);
        check(p!=null && "move:quickattack".equals(p.action())
                && p.reason()==ShadowLiveDecisionEngine.Reason.MOVE,
                "source-known priority move outranks plain same-power hit");
        check(!p.safeLine().contains("quickattack")
                && !p.safeLine().contains("UNKNOWN")
                && p.safeLine().contains("calibration=UNPROVEN"),
                "safe telemetry does not leak move or opponent identity");
        check(ShadowLiveDecisionEngine.ordinary(attack,true,false,true)==null
                && ShadowLiveDecisionEngine.ordinary(attack,false,false,false)==null,
                "unsupported force or stale source abstains");
        var healing=snapshot(.20,List.of("recover","swift"),
                List.of(),List.of(),Map.of(),List.of());
        var heal=ShadowLiveDecisionEngine.ordinary(healing,false,false,true);
        check(heal!=null && "move:recover".equals(heal.action())
                && heal.reason()==ShadowLiveDecisionEngine.Reason.RECOVERY,
                "owned critically low HP supports annotated recovery");
        var fullHp=snapshot(.90,List.of("recover","swift"),
                List.of(),List.of(),Map.of(),List.of());
        check("move:swift".equals(ShadowLiveDecisionEngine.ordinary(
                fullHp,false,false,true).action()),
                "healthy own state avoids unnecessary healing");
        var setup=snapshot(.90,List.of("tackle","swordsdance"),
                List.of(),List.of(),Map.of("atk",0),List.of());
        var boost=ShadowLiveDecisionEngine.ordinary(setup,false,false,true);
        check(boost!=null && boost.reason()==ShadowLiveDecisionEngine.Reason.SETUP
                && "move:swordsdance".equals(boost.action()),
                "verified setup effect with current public boost stage");
        var unknownBoost=snapshot(.90,List.of("tackle","swordsdance"),
                List.of(),List.of(),Map.of(),List.of());
        check("move:tackle".equals(ShadowLiveDecisionEngine.ordinary(
                unknownBoost,false,false,true).action()),
                "UNKNOWN boost stage never becomes fabricated neutral stage");
        var statusOnly=snapshot(.75,List.of("stealthrock"),
                List.of(),List.of(),Map.of(),List.of());
        var hazard=ShadowLiveDecisionEngine.ordinary(statusOnly,false,false,true);
        check(hazard!=null && hazard.reason()==ShadowLiveDecisionEngine.Reason.STATUS
                && "move:stealthrock".equals(hazard.action()),
                "verified entry-hazard status action is independently selectable");
        var encore=snapshot(.7,List.of("encore"),
                List.of(),List.of(),Map.of(),List.of("scratch"));
        check("move:encore".equals(ShadowLiveDecisionEngine.ordinary(
                encore,false,false,true).action()),
                "Encore requires witnessed current opponent move");
        check(ShadowLiveDecisionEngine.ordinary(snapshot(.7,
                List.of("encore"),List.of(),List.of(),Map.of(),List.of()),
                false,false,true)==null,
                "Encore abstains when opponent has no publicly witnessed move");
        var typedOwn=new BattleSnapshot.Combatant("self",.8,
                List.of("normal"),Map.of(),List.of("tackle","shadowclaw"),
                null,null,false);
        var publicGhost=new BattleSnapshot.Combatant("public-ghost",.8,
                List.of("ghost"),Map.of(),List.of(),null,null,false);
        var publicTyped=new BattleSnapshot(6,typedOwn,publicGhost,
                List.of(),List.of(),List.of("tackle","shadowclaw"),
                List.of(),Map.of(),false,null);
        check("move:shadowclaw".equals(
                    ShadowLiveDecisionEngine.ordinary(
                        publicTyped,false,false,true).action()),
                "verified public Ghost immunity blocks Normal hit");
        var unknown=snapshot(.5,List.of("unverifiedeffect"),
                List.of(),List.of(),Map.of(),List.of());
        check(ShadowLiveDecisionEngine.ordinary(unknown,false,false,true)==null,
                "unknown move effect abstains instead of fabricating utility");
        var low=snapshot(.12,List.of("tackle"),
                List.of("owner-1"),List.of(reserve(.85)),Map.of(),List.of());
        var switching=ShadowLiveDecisionEngine.ordinary(low,false,false,true);
        check(switching!=null
                && switching.reason()==ShadowLiveDecisionEngine.Reason.VOLUNTARY_SWITCH
                && "switch:owner-1".equals(switching.action()),
                "only healthy own reserve can supply explicit emergency preservation");
        check(ShadowLiveDecisionEngine.ordinary(low,false,true,true)==null,
                "trapped active cannot use independently proposed voluntary switch");
        check(ShadowLiveDecisionEngine.ordinary(snapshot(.12,List.of("tackle"),
                List.of("owner-1"),List.of(),Map.of(),List.of()),
                false,false,true)==null,
                "mismatched native bench and own snapshot fail closed");
        check(ShadowLiveDecisionEngine.ordinary(snapshot(.8,
                List.of("tackle","tackle"),List.of(),List.of(),
                Map.of(),List.of()),false,false,true)==null,
                "duplicate request move ID cannot masquerade as legal coverage");
        // When no native ordinary move is available, a legal current own
        // switch still allows Shadow to decide without consulting Legacy.
        var outOfMoves=snapshot(.6,List.of(),List.of("own-safe","own-low"),
                List.of(reserve(.9),reserve(.3)),Map.of(),List.of());
        var forcedVoluntary=ShadowLiveDecisionEngine.ordinary(
                outOfMoves,false,false,true);
        check(forcedVoluntary!=null
                && "switch:own-safe".equals(forcedVoluntary.action())
                && forcedVoluntary.reason()
                    ==ShadowLiveDecisionEngine.Reason.VOLUNTARY_SWITCH,
                "exhausted moves retain an independent native legal switch");
        check(ShadowLiveDecisionEngine.ordinary(
                outOfMoves,false,true,true)==null,
                "a trapped no-move request cannot fabricate a switch");
        check(ShadowLiveDecisionEngine.ordinary(
                snapshot(.5,List.of(),List.of(),List.of(),Map.of(),List.of()),
                false,false,true)==null,
                "no legal moves and no switches abstains safely");
        var forced=ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("fainted",0,false),
                new ShadowLiveDecisionEngine.Reserve("own-1",.45,true),
                new ShadowLiveDecisionEngine.Reserve("own-2",.80,true)),true);
        check(forced!=null && forced.reason()
                ==ShadowLiveDecisionEngine.Reason.FORCED_REPLACEMENT
                && "switch:own-2".equals(forced.action())
                && forced.scoredCandidates()==2,
                "forced replacement uses only live own-party HP");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("own-1",.7,true),
                new ShadowLiveDecisionEngine.Reserve("own-2",.7,true)),true)
                .action().equals("switch:own-1"),
                "own-party order is deterministic tie breaker");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("dead",0,false)),true)==null,
                "no eligible forced reserve is a clean abstention");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("own",1,true),
                new ShadowLiveDecisionEngine.Reserve("own",.8,true)),true)==null,
                "duplicate native reserve cannot become an action");
        check(ShadowLiveDecisionEngine.forcedReplacement(List.of(
                new ShadowLiveDecisionEngine.Reserve("own",1,true)),false)==null,
                "normal turn cannot forge a forced-replacement proposal");
        // Ordinary attacks absent from the narrow Phase 4 damage oracle
        // remain selectable when their OWN Cobblemon template is authenticated.
        var nativeOnly=snapshot(.8,List.of("newphysicalattack"),
                List.of(),List.of(),Map.of(),List.of());
        var ownAttack=new ShadowLiveDecisionEngine.OwnMoveFacts(
                "newphysicalattack","steel","physical",95,90,0);
        check("move:newphysicalattack".equals(
                ShadowLiveDecisionEngine.ordinary(nativeOnly,false,false,true,
                        Map.of("newphysicalattack",ownAttack)).action()),
                "native first-party move metadata enables broad attacks without Legacy");
        check(ShadowLiveDecisionEngine.ordinary(
                nativeOnly,false,false,true,Map.of())==null,
                "unknown unattested move power is never invented");
        check(ShadowLiveDecisionEngine.ordinary(nativeOnly,false,false,true,
                Map.of("foreign",ownAttack))==null,
                "foreign move fact cannot enter current owned native legal root");
        var nativePriority=snapshot(.8,List.of("newphysicalattack","tackle"),
                List.of(),List.of(),Map.of(),List.of());
        check("move:newphysicalattack".equals(
                ShadowLiveDecisionEngine.ordinary(nativePriority,false,false,true,
                        Map.of("newphysicalattack",ownAttack)).action()),
                "source-backed noncatalogue move is independently ranked");
        // Exact native-gimmick eligibility is independent of Legacy and
        // never inferred from opponent private data or an attractive score.
        var absent=new ShadowNativeGimmickPolicy.Options(
                false,false,false,false,false,false,false,false,null);
        check(ShadowNativeGimmickPolicy.choose(absent,"ghost",90,.9)==null,
                "no installed gimmick option never fabricates activation");
        var mega=new ShadowNativeGimmickPolicy.Options(
                true,false,false,false,false,false,false,false,null);
        check("mega".equals(ShadowNativeGimmickPolicy.choose(mega,null,0,.9)),
                "native Mega capability permits compatible setup turn");
        var z=new ShadowNativeGimmickPolicy.Options(
                false,false,true,false,false,false,true,false,null);
        check("zmove".equals(ShadowNativeGimmickPolicy.choose(z,"ghost",90,.9))
                && ShadowNativeGimmickPolicy.choose(z,"ghost",0,.9)==null,
                "Z uses only current eligible damaging slot");
        var blocked=new ShadowNativeGimmickPolicy.Options(
                false,false,true,false,false,false,false,false,null);
        check(ShadowNativeGimmickPolicy.choose(blocked,"ghost",90,.9)==null,
                "disabled Z slot cannot authorize Z move");
        var tera=new ShadowNativeGimmickPolicy.Options(
                false,false,false,false,true,false,false,false,"ghost");
        check("terastal".equals(ShadowNativeGimmickPolicy.choose(tera,"ghost",90,.9))
                && ShadowNativeGimmickPolicy.choose(tera,"fairy",90,.9)==null
                && ShadowNativeGimmickPolicy.choose(tera,"ghost",90,.2)==null,
                "Tera requires exact known OWN type, damage and health");
        var max=new ShadowNativeGimmickPolicy.Options(
                false,false,false,true,false,false,false,true,null);
        check("max".equals(ShadowNativeGimmickPolicy.choose(max,"ghost",90,.8)),
                "Dynamax requires native maxMove availability");
        var activeMax=new ShadowNativeGimmickPolicy.Options(
                false,false,false,true,false,true,false,true,null);
        check(ShadowNativeGimmickPolicy.choose(activeMax,"ghost",90,.8)==null,
                "already dynamaxed never requests another activation");
        check(ShadowNativeGimmickPolicy.choose(mega,"ghost",90,Double.NaN)==null,
                "invalid first-party HP cannot authorize a gimmick");
        var ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int choose=ai.indexOf("public ShowdownActionResponse choose(");
        int legacyStart=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        int nativeOwn=ai.indexOf("captureIndependentShadowSnapshot(");
        int deferredFallback=ai.indexOf("()->chooseLegacy(",choose);
        check(choose>=0 && nativeOwn>choose && deferredFallback>nativeOwn
                && ai.substring(choose,deferredFallback).contains(
                        "captureIndependentOwnMoveFacts(")
                && !ai.substring(choose,deferredFallback).contains(
                        "ShowdownActionResponse legacy=chooseLegacy("),
                "Legacy tactical evaluator is lazy, never a Shadow input");
        check(choose>=0 && legacyStart>choose
                && ai.substring(choose,legacyStart).contains(
                    "ShadowLiveDecisionEngine.ordinary(")
                && ai.substring(choose,legacyStart).contains(
                    "prepareShadowForcedReplacement(")
                && ai.substring(choose,legacyStart).contains(
                    "currentNativeGimmickAllowed(")
                && ai.contains("moveset.getCanZMove()")
                && ai.contains("moveset.getCanDynamax()")
                && ai.contains("moveset.getCanTerastallize()")
                && ai.contains("moveset.getCanMegaEvo()")
                && ai.substring(choose,legacyStart).contains(
                    "true); // Owner explicitly approved live Shadow primary selection.")
                && !ai.substring(choose,legacyStart).contains(
                    "PublicCompetitiveConsumerContract.prepare("),
                "actual live chooser integrates owner-authorized independent policy");
        System.out.println("Shadow live decision engine PASS: "+checks
                +" adversarial standalone checks; Minecraft UNTESTED");
    }
}
