package com.maestroai.training;

import java.util.List;

/** Phase 6.22 synthetic safety matrix: NOT installed Minecraft acceptance. */
final class RuntimeObservationAcceptanceHarnessRegression {
    private static int checks;
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError("Phase 6.22: "+why);
        checks++;
    }
    private static final String PRIVATE="PRIVATE_UUID_NAME_ITEM_MUST_NOT_LEAK";
    private static List<String> frames(){
        return List.of("update\n|turn|1\n|move|p1a: "+PRIVATE+"|Tackle");
    }
    private static NativeSplitDispatchObservation.Report nativeReturn(
            NativeSplitDispatchObservation.Session source,Object battle,
            Object own,Object foe,int turn,int chunks){
        return source.observeNativeReturn(battle,List.of(own,foe),
                own,new Object(),new Object(),turn,chunks,0,true,false);
    }
    private static void noAuthority(RuntimeObservationAcceptanceHarness.Report r){
        check(!r.minecraftAccepted() && !r.nativeHookInstalledProven()
                && !r.sourcePublicVisibilityCertified()
                && !r.eventIdentityAuthenticated()
                && !r.recipientDeliveryAuthenticated()
                && !r.completeLegalActionsProven()
                && !r.shadowSearchAuthorized()
                && !r.tacticalDecisionAuthorized()
                && !r.livePromotionAuthorized(),"each verdict denies authority");
        check(!r.toString().contains(PRIVATE)
                && !r.toString().contains("p1a:")
                && !r.toString().contains("Tackle"),
                "private source messages never enter diagnostics");
    }
    static void run(){
        Object battle=new Object(),own=new Object(),foe=new Object();
        var fresh=new RuntimeObservationAcceptanceHarness.Session();
        var before=fresh.publicHistory(battle,1,frames(),true);
        check(before.verdict()==RuntimeObservationAcceptanceHarness.Verdict.HOOK_UNOBSERVED
                && before.nativeReturnCallbacks()==0 && before.observedHistoryChunks()==1,
                "history without a callback is not Mixin activation");
        noAuthority(before);
        check(fresh.publicHistory(battle,1,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.HOOK_UNOBSERVED,
                "identical input stays source-idempotent");
        var nativeSource=new NativeSplitDispatchObservation.Session();
        var receipt=nativeReturn(nativeSource,battle,own,foe,1,1);
        var hooked=fresh.nativeReturnHook(battle,receipt);
        check(hooked.verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.NATIVE_RETURN_OBSERVED_UNVERIFIED
                && hooked.nativeReturnCallbacks()==1 && receipt.current(),
                "genuine source-owned hook return is still unverified delivery");
        noAuthority(hooked);
        var ordered=fresh.publicHistory(battle,1,List.of(
                frames().getFirst(),"update\n|turn|2"),true);
        check(ordered.verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.ORDERED_SOURCE_AND_HOOK_UNVERIFIED
                && ordered.current(),"append-only growth with current hook");
        noAuthority(ordered);
        check(!hooked.current() && !before.current(),
                "older reports expire as the original source advances");

        var phase621=new NativeEvidenceEndToEndGate.Session();
        var missing=phase621.inspectDirect(battle,null);
        var coverage=fresh.observePhase621(battle,missing);
        check(coverage.phase621Coverage()
                ==RuntimeObservationAcceptanceHarness.Phase621Coverage.ABSTAINED
                && !coverage.tacticalDecisionAuthorized(),
                "a missing Phase 6.21 source never grants readiness");
        noAuthority(coverage);

        var changed=new RuntimeObservationAcceptanceHarness.Session();
        changed.publicHistory(battle,1,frames(),true);
        var rewritten=changed.publicHistory(battle,1,List.of(
                "update\n|turn|1\n|move|p1a: "+PRIVATE+"|Earthquake"),true);
        check(rewritten.verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.STALE_OR_REWRITTEN
                && changed.withdrawn(),"same-count rewrite poisons source");
        noAuthority(rewritten);
        check(changed.publicHistory(battle,1,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.WITHDRAWN,
                "rewritten session never resurrects");

        var old=new RuntimeObservationAcceptanceHarness.Session();
        old.publicHistory(battle,3,List.of("update\n|turn|3"),true);
        check(old.publicHistory(battle,2,List.of("update\n|turn|3"),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.ORDER_CONFLICT,
                "turn regression fails closed");
        var truncated=new RuntimeObservationAcceptanceHarness.Session();
        truncated.publicHistory(battle,1,List.of("a","b"),true);
        check(truncated.publicHistory(battle,1,List.of("a"),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.ORDER_CONFLICT,
                "source truncation fails closed");
        var cross=new RuntimeObservationAcceptanceHarness.Session();
        cross.publicHistory(battle,1,frames(),true);
        check(cross.publicHistory(new Object(),2,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.WRONG_BATTLE,
                "cross-battle reuse fails closed");
        var pin=new RuntimeObservationAcceptanceHarness.Session();
        check(pin.publicHistory(battle,1,frames(),false).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.SOURCE_UNAVAILABLE
                && pin.withdrawn(),"pinned source mismatch fails closed");
        var tooBig=new RuntimeObservationAcceptanceHarness.Session();
        check(tooBig.publicHistory(battle,1,List.of("x".repeat(65537)),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.SOURCE_UNAVAILABLE,
                "oversized source not recorded");

        var noHook=new RuntimeObservationAcceptanceHarness.Session();
        check(noHook.nativeReturnHook(battle,null).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.INVALID_NATIVE_WITNESS,
                "caller cannot invent a null native witness");
        var foreign=new RuntimeObservationAcceptanceHarness.Session();
        check(foreign.nativeReturnHook(new Object(),receipt).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.INVALID_NATIVE_WITNESS,
                "source-owned report from another physical battle rejected");
        var stale=new RuntimeObservationAcceptanceHarness.Session();
        stale.publicHistory(battle,2,List.of("a","b"),true);
        var staleSource=new NativeSplitDispatchObservation.Session();
        var late=nativeReturn(staleSource,battle,own,foe,1,1);
        check(stale.nativeReturnHook(battle,late).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.ORDER_CONFLICT,
                "late native callback cannot backdate public source");
        var duplicate=new RuntimeObservationAcceptanceHarness.Session();
        var duplicateSource=new NativeSplitDispatchObservation.Session();
        var first=nativeReturn(duplicateSource,battle,own,foe,1,1);
        check(duplicate.nativeReturnHook(battle,first).nativeReturnCallbacks()==1,
                "one native callback counted");
        nativeReturn(duplicateSource,battle,own,foe,1,1);
        check(duplicate.nativeReturnHook(battle,first).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.INVALID_NATIVE_WITNESS
                && duplicate.withdrawn(),
                "ambiguous duplicate revokes native witness");

        var end=new RuntimeObservationAcceptanceHarness.Session();
        end.publicHistory(battle,1,frames(),true);
        var endReceipt=end.battleEndHook(battle);
        check(endReceipt.verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.BATTLE_END_HOOK_OBSERVED
                && end.withdrawn() && !endReceipt.current(),
                "battle-end Mixin invocation terminates observer");
        noAuthority(endReceipt);
        check(end.publicHistory(battle,2,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.WITHDRAWN,
                "ended session cannot resume");
        var next=new RuntimeObservationAcceptanceHarness.Session();
        Object nextBattle=new Object();
        check(next.publicHistory(nextBattle,1,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.HOOK_UNOBSERVED,
                "new physical battle starts with no hook evidence");
        check(next.publicHistory(new Object(),1,frames(),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.WRONG_BATTLE,
                "new battle identity cannot be replaced");

        var hookFirst=new RuntimeObservationAcceptanceHarness.Session();
        var hookSource=new NativeSplitDispatchObservation.Session();
        var hookReceipt=nativeReturn(hookSource,battle,own,foe,1,3);
        hookFirst.nativeReturnHook(battle,hookReceipt);
        check(hookFirst.publicHistory(battle,1,List.of("a","b"),true).verdict()
                ==RuntimeObservationAcceptanceHarness.Verdict.ORDER_CONFLICT,
                "source read lagging a witnessed native epoch abstains");
        System.out.println("Phase 6.22 synthetic harness regression PASS: "
                +checks+" assertions");
    }
}
