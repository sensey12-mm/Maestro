package com.maestroai.training;

import java.util.*;

/** Phase 6.9 bridge regressions: public-only capture plus offline-only handoff. */
final class ReadOnlyShadowObservationBridgeRegression {
    private ReadOnlyShadowObservationBridgeRegression() {}
    private static int checks;
    private static final String TOKEN="a".repeat(64);
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError("Phase 6.9: "+message);
        checks++;
    }
    private static void reject(Runnable action,String message){
        try{action.run();}
        catch(IllegalArgumentException ex){
            check("Phase 6.9 public bridge abstained".equals(ex.getMessage()),
                    message+" redacted");
            return;
        }
        throw new AssertionError("Phase 6.9 unexpectedly accepted "+message);
    }
    private static List<String> history(){
        return OfflineShadowResponseScenariosRegression.frames("Scratch");
    }
    private static OfflineRuntimeEvidenceContract.Envelope envelope(
            List<String> frames,long sequence,int turn){
        var snapshot=OfflineShadowResponseScenariosRegression.snapshot(
                turn,List.of("tackle"),List.of("scratch"),List.of());
        var source=OfflineShadowResponseScenariosRegression.input(frames,snapshot);
        var actions=List.of("move:tackle");
        return new OfflineRuntimeEvidenceContract.Envelope(
                OfflineRuntimeEvidenceContract.SCHEMA,CobblemonMechanics.SOURCE,
                TOKEN,sequence,turn,1,"p1a","p2a",actions,
                OfflineRuntimeEvidenceContract.actionFingerprint(actions),source);
    }
    static void run(){
        Object battle=new Object(), foreignBattle=new Object();
        var frames=history();
        var bridge=new ReadOnlyShadowObservationBridge();
        var first=bridge.observe(battle,frames,true);
        check(first.state()==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && first.revision()==1 && first.publicTurn()==1
                && first.publicEventCount()==4 && !first.currentBattleAuthenticated()
                && !first.legalActionsAttested() && !first.tacticalAuthority(),
                "existing read-only public update stream parsed without authority");
        check(bridge.observe(battle,frames,true).revision()==1,
                "repeat observations cannot mint a second sequence");
        check(!first.toString().contains("Opp") && !first.toString().contains(TOKEN)
                && !first.toString().contains("Own"),
                "no actor names or tokens in public bridge diagnostics");
        reject(()->bridge.probeSynthetic(foreignBattle,envelope(frames,1,1)),
                "other battle object cannot consume this source");
        var analyzed=bridge.probeSynthetic(battle,envelope(frames,1,1));
        check(analyzed.syntheticSequence()==1 && analyzed.witnessedReplyCount()==1
                && !analyzed.runtimeActorBound()
                && !analyzed.installedLegalActionsAttested()
                && !analyzed.shadowSearchAuthorized()
                && !analyzed.tacticalDecisionAuthorized()
                && !analyzed.livePromotionAuthorized(),
                "synthetic probe delegates Phase 6.7 and 6.8 but grants no live authority");
        reject(()->bridge.probeSynthetic(battle,envelope(frames,1,1)),
                "already consumed observation revision cannot replay");
        check(!bridge.withdrawn(),"duplicate probe abstains without poisoning");
        var later=new ArrayList<>(frames);
        later.add("update\n|turn|2");
        var advanced=bridge.observe(battle,later,true);
        check(advanced.revision()==2 && advanced.publicTurn()==2,
                "append-only real public-history continuation");
        check(bridge.probeSynthetic(battle,envelope(later,2,2)).syntheticSequence()==2,
                "Phase 6.7/6.8 accepts independent second synthetic actor turn");

        var actorSwap=new ReadOnlyShadowObservationBridge();
        actorSwap.observe(battle,frames,true);
        var swap=actorSwap.observe(foreignBattle,frames,true);
        check(swap.state()==ReadOnlyShadowObservationBridge.State.WRONG_BATTLE
                && actorSwap.withdrawn(),
                "source identity swap permanently withdraws old battle");
        check(actorSwap.observe(battle,frames,true).state()
                ==ReadOnlyShadowObservationBridge.State.WITHDRAWN,
                "old bridge cannot resurrect after source object returns");

        var rewrite=new ReadOnlyShadowObservationBridge();
        rewrite.observe(battle,frames,true);
        var fork=new ArrayList<>(frames);
        fork.set(3,"update\n|move|p2a: Opp|Tackle|p1a: Own");
        check(rewrite.observe(battle,fork,true).state()
                ==ReadOnlyShadowObservationBridge.State.REPLAY_OR_REWRITE
                && rewrite.withdrawn(),
                "modified history cannot be accepted via a re-pinned fixture");
        var trunc=new ReadOnlyShadowObservationBridge();
        trunc.observe(battle,frames,true);
        check(trunc.observe(battle,frames.subList(0,3),true).state()
                ==ReadOnlyShadowObservationBridge.State.REPLAY_OR_REWRITE,
                "history truncation fails closed");

        var noPin=new ReadOnlyShadowObservationBridge();
        check(noPin.observe(battle,frames,false).state()
                ==ReadOnlyShadowObservationBridge.State.NO_SOURCE_VERSION_PIN
                && noPin.withdrawn(),"unverified Cobblemon version blocks source bridge");
        check(new ReadOnlyShadowObservationBridge().observe(battle,null,true).state()
                ==ReadOnlyShadowObservationBridge.State.WRONG_BATTLE,
                "missing raw observation fails closed");

        var privateSide=new ArrayList<>(frames);
        privateSide.add("sideupdate\n|request|PRIVATE_HIDDEN_TEAM_DATA");
        var side=new ReadOnlyShadowObservationBridge();
        var sideResult=side.observe(battle,privateSide,true);
        check(sideResult.state()==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && side.withdrawn() && !sideResult.toString().contains("PRIVATE"),
                "private sideupdate payload is never retained as eligible spectator frame");

        var split=new ArrayList<>(frames);
        split.add("update\n|split|p1\n|damage|PRIVATE_HP\n|-damage|p1a: Own|80/100");
        var spl=new ReadOnlyShadowObservationBridge();
        check(spl.observe(battle,split,true).state()
                ==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES
                && spl.withdrawn(),"split private/public branch rejected by narrow Phase 6.2 bridge");
        var unsafe=new ArrayList<>(frames);
        unsafe.add("update\n|-damage|p1a: Own|20/100");
        var expanded=new ReadOnlyShadowObservationBridge();
        check(expanded.observe(battle,unsafe,true).state()
                ==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE
                && expanded.publicLease(battle)!=null,
                "direct public damage is observed but never legal-action evidence");

        var external=new ReadOnlyShadowObservationBridge();
        external.observe(battle,frames,true);
        var invalidFrames=new ArrayList<>(frames);
        invalidFrames.set(3,"update\n|move|p2a: PRIVATE_ALIAS|Scratch|p1a: Own");
        reject(()->external.probeSynthetic(battle,envelope(invalidFrames,1,1)),
                "fabricated original actor cannot bypass captured original frames");
        check(external.withdrawn(),"unsafe synthetic fixture irrevocably poisons bridge");

        var caught=new ReadOnlyShadowObservationBridge();
        var spoof=new ArrayList<>(frames);
        spoof.set(3,"update\n|move|p2a: PRIVATE_ALIAS|Scratch|p1a: Own");
        check(caught.observe(battle,spoof,true).state()
                ==ReadOnlyShadowObservationBridge.State.SYNTHETIC_FRAME_ELIGIBLE,
                "public framing alone cannot authenticate spoofed actor");
        reject(()->caught.probeSynthetic(battle,envelope(spoof,1,1)),
                "original Phase 6.2 actor-attribution verification still rejects spoof");
        check(caught.withdrawn(),
                "the downstream 6.2-6.8 rejection permanently withdraws the bridge");

        var malformed=new ReadOnlyShadowObservationBridge();
        var turnSkip=new ArrayList<>(frames);
        turnSkip.set(2,"update\n|turn|3");
        check(malformed.observe(battle,turnSkip,true).state()
                ==ReadOnlyShadowObservationBridge.State.NO_SAFE_PUBLIC_FRAMES,
                "incorrect turn sequence fails without passing Phase 6.7");

        var original=new ReadOnlyShadowObservationBridge();
        original.observe(battle,frames,true);
        original.withdraw();
        check(original.withdrawn()
                && original.observe(battle,frames,true).state()
                    ==ReadOnlyShadowObservationBridge.State.WITHDRAWN,
                "explicit lifecycle withdrawal cannot be reversed");
        reject(()->original.probeSynthetic(battle,envelope(frames,1,1)),
                "withdrawn source cannot create a new synthetic lease");

        System.out.println("Phase 6.9 read-only public bridge PASS: "+checks+" synthetic checks");
    }
}
