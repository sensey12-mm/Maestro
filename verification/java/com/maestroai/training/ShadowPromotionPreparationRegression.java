package com.maestroai.training;

import com.maestroai.ai.LivePromotionGate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.maestroai.training.PublicTransitionState.Known;

/**
 * Synthetic only. Exercises the genuine independent-conditional comparator
 * contract and the production fallback gate, NOT Minecraft promotion approval.
 */
final class ShadowPromotionPreparationRegression {
    private ShadowPromotionPreparationRegression(){}
    private static int checks;
    private static void check(boolean condition,String label){
        if(!condition)throw new AssertionError("Shadow promotion prep: "+label);
        checks++;
    }
    private static BattleSnapshot snapshot(){
        var self=new BattleSnapshot.Combatant("Own",1.0,List.of("normal"),
                Map.of(),List.of("tackle","quickattack"),null,null,false);
        var opponent=new BattleSnapshot.Combatant("PublicUnknown",
                BattleSnapshot.UNKNOWN_HP_FRACTION,List.of(),Map.of(),
                List.of("scratch"),null,null,false);
        return new BattleSnapshot(7,self,opponent,List.of(),List.of(),
                List.of("tackle","quickattack"),List.of(),Map.of(),false,null);
    }
    private static ShadowMechanicsEvaluation.Ranking ranked(String action,double lo,double hi){
        return new ShadowMechanicsEvaluation.Ranking(action,
                Known.of(new ShadowMechanicsEvaluation.Utility(lo,hi),
                        "synthetic source-conditional bounds"),
                List.of(),Set.of());
    }
    private static ShadowMechanicsEvaluation.Comparison compare(
            List<ShadowMechanicsEvaluation.Ranking> rankings,
            String preferred,boolean complete){
        return new ShadowMechanicsEvaluation.Comparison(
                rankings,preferred,"move:quickattack","move:quickattack",
                true,complete,"synthetic source-conditional; not real runtime permission");
    }
    private static ShadowMechanicsEvaluation.Comparison example(){
        return compare(List.of(ranked("move:tackle",12,18),
                ranked("move:quickattack",0,5)),"move:tackle",true);
    }
    static void run() throws Exception {
        Object battle=new Object(),request=new Object();
        var snap=snapshot();
        var source=new AtomicBoolean(true);
        var accepted=ShadowPromotionPreparation.compare(battle,request,snap,
                example(),source::get);
        var proposal=accepted.proposal();
        check(accepted.verdict()==ShadowPromotionPreparation.Verdict
                .CONDITIONAL_PROPOSAL_NONAUTHORITATIVE
                && proposal!=null && proposal.current(battle,request,7)
                && "move:tackle".equals(proposal.action())
                && !proposal.promotionAuthorized()
                && !accepted.promotionAuthorized(),
                "strict independent preference prepared, never approved");
        check(!proposal.current(new Object(),request,7)
                && !proposal.current(battle,new Object(),7)
                && !proposal.current(battle,request,8),
                "foreign battle/request/turn cannot borrow current proposal");
        check(accepted.safeLine().contains("legacyFallback=REQUIRED")
                && accepted.safeLine().contains("authority=NONE")
                && !accepted.safeLine().contains("move:tackle")
                && !accepted.safeLine().contains("PublicUnknown"),
                "telemetry contains no move, opponent or private identity");
        source.set(false);
        check(!proposal.current(battle,request,7)
                && ShadowPromotionPreparation.compare(
                    battle,request,snap,example(),source::get)
                    .verdict()==ShadowPromotionPreparation.Verdict
                            .MISSING_CURRENT_OWN_SOURCE,
                "source expiration immediately revokes speculative proposal");
        source.set(true);

        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18)), "move:tackle",true),
                source::get).verdict()==ShadowPromotionPreparation.Verdict
                        .INCOMPLETE_OR_UNBOUNDED_ACTIONS,
                "a missing request action invalidates conditional preference");
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18),
                        ranked("move:illegal",0,5)),"move:tackle",true),
                source::get).verdict()==ShadowPromotionPreparation.Verdict
                        .INCOMPLETE_OR_UNBOUNDED_ACTIONS,
                "invented root action is rejected");
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18),
                        ranked("move:tackle",0,5)),"move:tackle",true),
                source::get).verdict()==ShadowPromotionPreparation.Verdict
                        .INCOMPLETE_OR_UNBOUNDED_ACTIONS,
                "duplicated candidate ranking is rejected");
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18),
                        ranked("move:quickattack",15,21)),"move:tackle",true),
                source::get).verdict()==ShadowPromotionPreparation.Verdict
                        .NO_STRICT_INDEPENDENT_PREFERENCE,
                "overlapping utility intervals never pretend to be a win");
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18),
                        ranked("move:quickattack",0,5)),null,true),
                source::get).verdict()==ShadowPromotionPreparation.Verdict
                        .NO_STRICT_INDEPENDENT_PREFERENCE,
                "no endorsed strict preference is not a decision");
        var unknown=new ShadowMechanicsEvaluation.Ranking("move:quickattack",
                Known.unknown(),List.of(),Set.of("UNKNOWN"));
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(List.of(ranked("move:tackle",12,18),unknown),
                        "move:tackle",true),source::get).verdict()
                ==ShadowPromotionPreparation.Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS,
                "unresolved Phase 4 mechanic forces abstention");
        check(ShadowPromotionPreparation.compare(battle,request,snap,
                compare(example().rankings(),"move:tackle",false),source::get).verdict()
                ==ShadowPromotionPreparation.Verdict.INCOMPLETE_OR_UNBOUNDED_ACTIONS,
                "incomplete Phase 6 candidate coverage forces abstention");
        check(ShadowPromotionPreparation.inspect(battle,request,snap,
                example(),null,null,null,null).verdict()
                ==ShadowPromotionPreparation.Verdict.MISSING_CURRENT_OWN_SOURCE,
                "offline fixture cannot fabricate a native owning lease");

        // Only synthetic unit tests exercise ownerApproved=true. The real AI
        // passes a literal false, regardless of its JVM promotion flag.
        var original=System.getProperty(LivePromotionGate.PROPERTY);
        try {
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            var calls=new AtomicInteger();
            var promotedCalls=new AtomicInteger();
            var falseChoice=LivePromotionGate.choosePrepared(
                    ()->{calls.incrementAndGet();return "LEGACY";},
                    proposal,battle,request,7,
                    action->{promotedCalls.incrementAndGet();return action;},
                    action->true,false);
            check("LEGACY".equals(falseChoice) && calls.get()==1
                    && promotedCalls.get()==0,
                    "enabled JVM property without owner authorization NEVER runs candidate");
            source.set(false);
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->action,action->true,true)),
                    "expired owning source immediately uses Legacy fallback");
            source.set(true);
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->false,true)),
                    "an independently illegal promoted response uses Legacy fallback");
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->{throw new IllegalStateException("broken Shadow");},
                    action->true,true)),
                    "Shadow exception cannot interrupt Legacy choice");
            check("PROMOTED".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->true,true)),
                    "future explicitly approved, current, legal candidate can cross gate");
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,8,
                    action->"PROMOTED",action->true,true)),
                    "stale turn cannot become an authoritative action");
            source.set(false);
            System.setProperty(LivePromotionGate.PROPERTY,"false");
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->true,true)),
                    "independently disabled property still forces Legacy");
        }finally{
            if(original==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,original);
        }
        // Structural source guard: live chooser's approval stays literal
        // false; no publicly observed Phase 5 role controls any action.
        String ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int choose=ai.indexOf("public ShowdownActionResponse choose(");
        int legacy=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        check(choose>=0 && legacy>choose
                && ai.substring(choose,legacy).contains("LivePromotionGate.choosePrepared(")
                && ai.substring(choose,legacy).contains(
                        "false); // Owner has NOT authorized live Shadow selection.")
                && ai.contains("ShadowPromotionPreparation.inspect(")
                && ai.contains("LivePromotionGate.choosePrepared(")
                && !ai.substring(choose,legacy).contains(
                        "PublicHistoricalRoleLiveOverlay.Session.choose("),
                "production legacy-boundary has a real dormant choice materializer, no promotion");
        System.out.println("Shadow promotion preparation PASS: "+checks+
                " adversarial checks; no live authorization; Minecraft UNTESTED");
    }
}
