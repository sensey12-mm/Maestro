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
                example(),null,null,null,null,null,null).verdict()
                ==ShadowPromotionPreparation.Verdict.MISSING_CURRENT_OWN_SOURCE,
                "offline fixture cannot fabricate a native owning lease");

        // First-party fallback strips EVERY opponent-derived tactical field.
        var ownOnly=ShadowPromotionPreparation.firstPartyOnly(snap);
        check("UNVERIFIED".equals(ownOnly.opponent().species())
                && ownOnly.opponent().types().isEmpty()
                && ownOnly.opponent().revealedMoves().isEmpty()
                && !ownOnly.opponent().hpKnown()
                && ownOnly.opponentBench().isEmpty()
                && ownOnly.legalMoves().equals(snap.legalMoves())
                && ownOnly.self()==snap.self(),
                "incomplete public history cannot contaminate independent own-only policy");
        check(ShadowPromotionPreparation.firstPartyOnly(null)==null,
                "missing source cannot create first-party policy");
        check(ShadowPromotionPreparation.inspectOwnOnly(
                battle,request,ownOnly,
                ShadowLiveDecisionEngine.ordinary(ownOnly,false,false,true),
                null,null).proposal()==null,
                "own-only choice without native actor lease is never promoted");
        // The independent *native* own request must enumerate exactly the
        // same ordinary roots as Shadow's snapshot, with its own live lease.
        var nativeWindow=ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack"),List.of(),
                false,false,false,true,source::get);
        check(nativeWindow!=null && nativeWindow.exactlyCovers(battle,request,snap)
                && !nativeWindow.promotionAuthorized(),
                "real owning native window exactly covers both ordinary moves");
        check(!nativeWindow.exactlyCovers(new Object(),request,snap)
                && !nativeWindow.exactlyCovers(battle,new Object(),snap)
                && !nativeWindow.exactlyCovers(battle,request,new BattleSnapshot(
                        8,snap.self(),snap.opponent(),snap.selfBench(),
                        snap.opponentBench(),snap.legalMoves(),snap.legalSwitches(),
                        snap.knownOpponentMoveCounts(),false,null)),
                "native request cannot be replayed in foreign battle/request/turn");
        var missing=ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle"),List.of(),false,false,false,true,source::get);
        check(missing!=null && !missing.exactlyCovers(battle,request,snap),
                "native action missing from alleged Shadow root rejects");
        check(ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","tackle"),List.of(),
                false,false,false,true,source::get)==null,
                "duplicate usable native move invalidates own root");
        check(ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack"),List.of(),
                true,false,false,true,source::get)==null
                && ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack"),List.of(),
                false,false,true,true,source::get)==null,
                "forced replacement and charging continuation abstain");
        check(ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack"),List.of("own-bench"),
                false,true,false,true,source::get)==null,
                "trapped request never certifies a voluntary switch");
        check(ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack"),List.of(),
                false,false,false,false,source::get)==null,
                "incomplete native request cannot certify root legality");
        var extraneous=ShadowNativeLegalWindow.capture(battle,request,7,
                List.of("tackle","quickattack","synthetic-zmove"),List.of(),
                false,false,false,true,source::get);
        check(extraneous!=null && !extraneous.exactlyCovers(battle,request,snap),
                "native special action absent from snapshot invalidates exhaustive coverage");
        source.set(false);
        check(!nativeWindow.exactlyCovers(battle,request,snap)
                && ShadowNativeLegalWindow.capture(battle,request,7,
                    List.of("tackle","quickattack"),List.of(),
                    false,false,false,true,source::get)==null,
                "withdrawn native own source revokes action legality");
        source.set(true);
        // Production source audit: no old unconstrained call is allowed.
        String actualAI=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(actualAI.contains("captureShadowNativeLegalWindow(")
                && actualAI.contains("phaseShadowLegalWindow.exactlyCovers(")
                && actualAI.contains("phaseShadowControls.revoke()")
                && actualAI.contains("phaseShadowLegalWindow=null;")
                && actualAI.contains("envelopedAudit.replies().result()")
                && actualAI.contains("phaseShadowLegalWindow,")
                && actualAI.contains("envelopedAudit);"),
                "actual chooser independently captures, rechecks and releases native action roots");

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
        // Both positive and rejected candidates must lose their source
        // identity after the owning chooser returns, independent of flags.
        // A changing source midway through materialization must fall back.
        source.set(true);
        var original2=System.getProperty(LivePromotionGate.PROPERTY);
        try {
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->{source.set(false);return "PROMOTED";},
                    action->true,true)),
                    "in-flight source withdrawal uses Legacy immediately");
        }finally{
            if(original2==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,original2);
        }
        // Per-trainer emergency kill switch: a concurrent operator stop
        // during materialization must abort the action, then latch Legacy.
        source.set(true);
        var control=new LivePromotionGate.Control();
        var priorEnabled=System.getProperty(LivePromotionGate.PROPERTY);
        try {
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->{control.revoke();return "PROMOTED";},
                    action->true,control,true)),
                    "operator revocation mid-materialization falls back immediately");
            check(!control.current()
                    && "LEGACY".equals(LivePromotionGate.choosePrepared(
                        ()->"LEGACY",proposal,battle,request,7,
                        action->"PROMOTED",action->true,control,true)),
                    "sticky failed/tripped trainer cannot reactivate by JVM setting");
            var invalidControl=new LivePromotionGate.Control();
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->false,invalidControl,true))
                    && !invalidControl.current(),
                    "illegal authorized candidate latches Legacy failsafe");
            var freshControl=new LivePromotionGate.Control();
            check("PROMOTED".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->true,freshControl,true))
                    && freshControl.current(),
                    "fresh explicitly approved future trainer can use same safe gate");
            source.set(false);
            check("LEGACY".equals(LivePromotionGate.choosePrepared(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"PROMOTED",action->true,freshControl,true))
                    && !freshControl.current(),
                    "revoked source disables current trainer even after future recovery");
        }finally{
            source.set(true);
            if(priorEnabled==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,priorEnabled);
        }
        // Structural source guard: live chooser's approval stays literal
        // false; no publicly observed Phase 5 role controls any action.
        String ai=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int choose=ai.indexOf("public ShowdownActionResponse choose(");
        int legacy=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        check(choose>=0 && legacy>choose
                && ai.substring(choose,legacy).contains("LivePromotionGate.choosePreparedWithSource(")
                && ai.substring(choose,legacy).contains(
                        "true); // Owner explicitly approved live Shadow primary selection.")
                && ai.contains("ShadowPromotionPreparation.inspect(")
                && ai.contains("LivePromotionGate.choosePreparedWithSource(")
                && ai.contains("action.isValid(activePokemon,moveset,forceSwitch)")
                && ai.contains("phaseShadowControls.forBattle(")
                && ai.contains("phaseShadowControls.battleEnded(")
                && ai.contains("phaseShadowPrepared=null;")
                && ai.contains("phaseShadowRequest=null;")
                && !ai.substring(choose,legacy).contains(
                        "PublicHistoricalRoleLiveOverlay.Session.choose("),
                "production gate explicitly promotes native-legal Shadow and attributes Legacy fallback");
        var currentFlag=System.getProperty(LivePromotionGate.PROPERTY);
        try{
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            var fresh=new LivePromotionGate.Control();
            var promoted=LivePromotionGate.choosePreparedWithSource(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"SHADOW",action->true,fresh,true);
            check(promoted.authority()==LivePromotionGate.Authority.SHADOW
                    && "SHADOW".equals(promoted.action()),"promoted decision attributed Shadow");
            check(LivePromotionGate.choosePreparedWithSource(
                    ()->"LEGACY",null,battle,request,7,
                    action->"SHADOW",action->true,fresh,true)
                    .authority()==LivePromotionGate.Authority.LEGACY && fresh.current(),
                    "missing proposal falls back without revocation");
            System.setProperty(LivePromotionGate.PROPERTY,"false");
            check(LivePromotionGate.choosePreparedWithSource(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"SHADOW",action->true,fresh,true)
                    .authority()==LivePromotionGate.Authority.LEGACY && !fresh.current(),
                    "operator stop latches Legacy for same battle");
            System.setProperty(LivePromotionGate.PROPERTY,"true");
            check(LivePromotionGate.choosePreparedWithSource(
                    ()->"LEGACY",proposal,battle,request,7,
                    action->"SHADOW",action->true,fresh,true)
                    .authority()==LivePromotionGate.Authority.LEGACY,
                    "reenabling flag cannot undo emergency revocation");
            var lifecycle=new LivePromotionGate.BattleControls();
            var first=java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
            var next=java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
            var old=lifecycle.forBattle(first);
            old.revoke();
            check(old==lifecycle.forBattle(first) && !old.current(),
                    "same-battle choice cannot reset shutdown");
            var replacement=lifecycle.forBattle(next);
            check(replacement!=old && replacement.current(),
                    "fresh battle may arm its own control");
            lifecycle.battleEnded(first);
            check(replacement.current(),"old end callback cannot kill new battle");
            lifecycle.battleEnded(next);
            check(!replacement.current() && lifecycle.forBattle(next)==replacement,
                    "battle completion disables stale Shadow actions");
            check(LivePromotionGate.choosePreparedWithSource(
                    ()->"LEGACY",proposal,battle,new Object(),7,
                    action->"SHADOW",action->true,new LivePromotionGate.Control(),true)
                    .authority()==LivePromotionGate.Authority.LEGACY,
                    "wrong original request cannot authorize Shadow");
        }finally{
            if(currentFlag==null)System.clearProperty(LivePromotionGate.PROPERTY);
            else System.setProperty(LivePromotionGate.PROPERTY,currentFlag);
        }
        System.out.println("Shadow promotion preparation PASS: "+checks+
                " adversarial checks; source-gated live authority; Minecraft UNTESTED");
    }
}
