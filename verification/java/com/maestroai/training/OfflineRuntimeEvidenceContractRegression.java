package com.maestroai.training;

import java.util.*;

/** Synthetic-only adversarial acceptance of the Phase 6.7 interface. */
final class OfflineRuntimeEvidenceContractRegression {
    private OfflineRuntimeEvidenceContractRegression() {}
    private static int checks;
    private static final String TOKEN="a".repeat(64);
    private static final String OTHER="b".repeat(64);
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError("Phase 6.7: "+why);
        checks++;
    }
    private static void reject(Runnable action,String why){
        try{action.run();}
        catch(IllegalArgumentException error){
            check(error.getMessage().equals("Phase 6.7 untrusted synthetic evidence")
                    && !error.getMessage().contains("PRIVATE"),why+" redacted");
            return;
        }
        throw new AssertionError("Phase 6.7 accepted "+why);
    }
    private static BattleSnapshot snapshot(int turn,List<String> moves,List<String> replies){
        return OfflineShadowResponseScenariosRegression.snapshot(
                turn,moves,replies,List.of());
    }
    private static OfflineShadowResponseScenarios.Input scenario(
            List<String> frames,int turn,List<String> moves,List<String> replies){
        return OfflineShadowResponseScenariosRegression.input(frames,
                snapshot(turn,moves,replies));
    }
    private static OfflineShadowResponseScenarios.Input original(){
        return scenario(OfflineShadowResponseScenariosRegression.frames("Scratch"),
                1,List.of("tackle"),List.of("scratch"));
    }
    private static OfflineRuntimeEvidenceContract.Envelope envelope(
            String token,long seq, OfflineShadowResponseScenarios.Input source,
            int turn,int appearance,List<String> actions,String fingerprint){
        return new OfflineRuntimeEvidenceContract.Envelope(
                OfflineRuntimeEvidenceContract.SCHEMA,CobblemonMechanics.SOURCE,
                token,seq,turn,appearance,"p1a","p2a",actions,fingerprint,source);
    }
    private static OfflineRuntimeEvidenceContract.Envelope request(
            String token,long seq,OfflineShadowResponseScenarios.Input source){
        var actions=new ArrayList<String>();
        source.snapshot().legalMoves().forEach(m->actions.add("move:"+m));
        source.snapshot().legalSwitches().forEach(m->actions.add("switch:"+m));
        return envelope(token,seq,source,source.snapshot().turn(),
                source.fixture().opponentAppearanceIndex(),actions,
                OfflineRuntimeEvidenceContract.actionFingerprint(actions));
    }
    private static OfflineRuntimeEvidenceContract.Session session(){
        return new OfflineRuntimeEvidenceContract.Session(TOKEN);
    }
    static void run(){
        var original=original();
        var first=request(TOKEN,1,original);
        var a=session();
        var result=a.inspect(first);
        check(result.syntheticConsistencyOnly()
                && result.syntheticActionCount()==1 && result.witnessedReplyCount()==1
                && result.syntheticSequence()==1 && !a.poisoned(),
                "root/public fixture consistently reconstructed");
        check(!result.runtimeActorBound() && !result.runtimeTurnFreshnessAttested()
                && !result.installedLegalActionsAttested()
                && !result.opponentResponseSetComplete()
                && !result.shadowSearchAuthorized()
                && !result.livePromotionAuthorized()
                && !result.tacticalDecisionAuthorized()
                && result.limitations().contains(
                    OfflineRuntimeEvidenceContract.Limitation.ORIGINAL_EVENTS_NOT_SOURCE_AUTHENTICATED),
                "no actor, turn, legality, response or Shadow authority");
        check(result.toString().equals(session().inspect(first).toString()),
                "repeatable deterministic synthetic review");
        check(!result.toString().contains(TOKEN)
                && !first.toString().contains(TOKEN),
                "privacy-safe token redaction");
        try{result.limitations().clear();throw new AssertionError("mutable report");}
        catch(UnsupportedOperationException good){checks++;}
        var second=request(TOKEN,2,original);
        check(a.inspect(second).syntheticSequence()==2,"ordered continuation");
        reject(()->a.inspect(second),"duplicate sequence replay");
        check(a.poisoned(),"replay permanently poisons the session");
        reject(()->a.inspect(request(TOKEN,3,original)),
                "fresh-looking request cannot revive poisoned session");

        var missing=session();
        reject(()->missing.inspect(request(TOKEN,2,original)),
                "skipped initial receipt");
        check(missing.poisoned(),"skipped sequence permanently rejected");
        var changed=session();
        reject(()->changed.inspect(request(OTHER,1,original)),
                "foreign battle token");
        var wrongSource=session();
        reject(()->wrongSource.inspect(
                new OfflineRuntimeEvidenceContract.Envelope(
                    OfflineRuntimeEvidenceContract.SCHEMA,CobblemonMechanics.SOURCE,
                    TOKEN,1,1,1,"p2a","p1a",List.of("move:tackle"),
                    OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle")),
                    original)),
                "reversed actor sides despite internally consistent source");

        reject(()->new OfflineRuntimeEvidenceContract.Envelope(
                "future-schema",CobblemonMechanics.SOURCE,TOKEN,1,1,1,
                "p1a","p2a",List.of("move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle")),
                original),"schema drift");
        reject(()->new OfflineRuntimeEvidenceContract.Envelope(
                OfflineRuntimeEvidenceContract.SCHEMA,"modified simulator hash",
                TOKEN,1,1,1,"p1a","p2a",List.of("move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle")),
                original),"mechanics pin drift");
        reject(()->new OfflineRuntimeEvidenceContract.Session("PRIVATE trainer UUID"),
                "raw trainer ID is prohibited");
        reject(()->envelope(TOKEN,1,original,1,1,
                List.of("move:tackle","move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle"))),
                "duplicate legal action");
        reject(()->envelope(TOKEN,1,original,1,1,
                List.of("move:PRIVATE_SECRET"),
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("move:tackle"))),
                "private or malformed action");
        check(OfflineRuntimeEvidenceContract.actionFingerprint(
                    List.of("move:tackle","move:swift")).equals(
                  OfflineRuntimeEvidenceContract.actionFingerprint(
                    List.of("move:swift","move:tackle"))),
                "fingerprint canonical across action ordering");

        var extraAction=session();
        reject(()->extraAction.inspect(envelope(TOKEN,1,original,1,1,
                    List.of("move:tackle","move:swift"),
                    OfflineRuntimeEvidenceContract.actionFingerprint(
                        List.of("move:tackle","move:swift")))),
                "invented legal action even with matching new digest");
        var forgedDigest=session();
        reject(()->forgedDigest.inspect(envelope(TOKEN,1,original,1,1,
                    List.of("move:tackle"),"c".repeat(64))),
                "non-source legal fingerprint");

        var forgedFrames=new ArrayList<>(
                OfflineShadowResponseScenariosRegression.frames("Scratch"));
        forgedFrames.set(3,"update\n|move|p2a: PRIVATE_SECRET|Scratch|p1a: Own");
        var forged=scenario(forgedFrames,1,List.of("tackle"),List.of("scratch"));
        var forgedSession=session();
        reject(()->forgedSession.inspect(request(TOKEN,1,forged)),
                "re-signed spectator actor spoof");
        check(forgedSession.poisoned(),"privacy spoof irrevocably rejected");

        var exposed=new BattleSnapshot.Combatant(
                "synthetic",1,List.of(),Map.of(),List.of("scratch"),
                "PRIVATE_HIDDEN_ITEM",null,false);
        var raw=snapshot(1,List.of("tackle"),List.of("scratch"));
        var privateSnapshot=new BattleSnapshot(1,raw.self(),exposed,
                List.of(),List.of(),List.of("tackle"),List.of(),
                Map.of(),false,null);
        var privateSource=OfflineShadowResponseScenariosRegression.input(
                OfflineShadowResponseScenariosRegression.frames("Scratch"),
                privateSnapshot);
        reject(()->session().inspect(request(TOKEN,1,privateSource)),
                "unrevealed private opponent item must not cross boundary");

        // Session continuity must reject a hash-repinned fork of earlier
        // spectator frames even when it parses cleanly by itself.
        var forked=scenario(
                OfflineShadowResponseScenariosRegression.frames("Tackle"),1,
                List.of("tackle"),List.of("tackle"));
        check(OfflineShadowResponseScenarios.construct(forked)
                .witnessedReplyCount()==1,
                "fork independently forms a structurally valid fixture");
        var timeline=session();
        timeline.inspect(request(TOKEN,1,original));
        reject(()->timeline.inspect(request(TOKEN,2,forked)),
                "forked append-only history and newly forged SHA");
        check(timeline.poisoned(),"fork cannot rejoin timeline");

        var wrongTurn=session();
        reject(()->wrongTurn.inspect(envelope(TOKEN,1,original,2,1,
                List.of("move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(
                    List.of("move:tackle")))),
                "future turn with stale original fixture");
        var wrongAppearance=session();
        reject(()->wrongAppearance.inspect(envelope(TOKEN,1,original,1,2,
                List.of("move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(
                    List.of("move:tackle")))),
                "fabricated appearance increment");
        var missingTurn=session();
        reject(()->missingTurn.inspect(envelope(TOKEN,1,original,3,1,
                List.of("move:tackle"),
                OfflineRuntimeEvidenceContract.actionFingerprint(
                    List.of("move:tackle")))),
                "skipped turn outside continuity bound");

        var laterFrames=new ArrayList<>(
                OfflineShadowResponseScenariosRegression.frames("Scratch"));
        laterFrames.add("update\n|turn|2");
        var later=scenario(laterFrames,2,List.of("tackle"),List.of("scratch"));
        var liveSequence=session();
        liveSequence.inspect(request(TOKEN,1,original));
        var advanced=liveSequence.inspect(request(TOKEN,2,later));
        check(advanced.syntheticTurn()==2 && advanced.syntheticSequence()==2
                && !advanced.runtimeActorBound(),
                "continuous turn transition uses append-only original source");
        var stale=session();
        stale.inspect(request(TOKEN,1,original));
        stale.inspect(request(TOKEN,2,later));
        reject(()->stale.inspect(request(TOKEN,3,original)),
                "return to stale source turn");
        System.out.println("Phase 6.7 offline runtime interface PASS: "
                +checks+" synthetic checks");
    }
}
