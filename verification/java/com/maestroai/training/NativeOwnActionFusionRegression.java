package com.maestroai.training;

import java.util.*;

/** Adversarial Phase 6.12 evidence fusion: source identity and freshness. */
final class NativeOwnActionFusionRegression {
    private NativeOwnActionFusionRegression(){}
    private static int checks;
    private static final UUID OWN=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BENCH=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static void check(boolean ok,String name){
        if(!ok)throw new AssertionError("Phase 6.12 "+name);
        checks++;
    }
    private static void reject(Runnable run,String name){
        try { run.run(); }
        catch(IllegalArgumentException e){
            check("Phase 6.12 own-action coverage abstained".equals(e.getMessage())
                    && !e.getMessage().contains("PRIVATE"),
                    name+" redacted");
            return;
        }
        throw new AssertionError("Phase 6.12 accepted "+name);
    }
    private static String pokemon(UUID id,boolean active,int hp){
        return "{\"details\":\"Genesect, "+id+", L50\","
            +"\"ident\":\"p1: PRIVATE_ALIAS\",\"active\":"+active+","
            +"\"condition\":\""+hp+"/100"+(hp==0?" fnt":"")+"\","
            +"\"ability\":\"Download\",\"types\":[\"bug\",\"steel\"],"
            +"\"baseTypes\":[\"bug\",\"steel\"],"
            +"\"stats\":{\"atk\":90,\"def\":90,\"spa\":90,\"spd\":90,\"spe\":90},"
            +"\"item\":\"\",\"terastallized\":\"\","
            +"\"commanding\":false,\"reviving\":false}";
    }
    private static String raw(boolean forced,boolean trapped){
        return "|request|{\"side\":{\"id\":\"p1\",\"pokemon\":["
            +pokemon(OWN,true,forced?0:100)+","
            +pokemon(BENCH,false,70)+"]},"
            +(forced?"\"forceSwitch\":[true]":
                "\"active\":[{\"trapped\":"+trapped+",\"moves\":["
                +"{\"move\":\"Tackle\",\"id\":\"tackle\",\"pp\":10,"
                +"\"maxpp\":20,\"target\":\"normal\",\"disabled\":false}]}]")+"}";
    }
    private static final class Fixture {
        final Object battle=new Object(),actor=new Object(),request=new Object();
        final boolean forced,trapped;
        final OwnRequestTransport ledger=new OwnRequestTransport();
        final OwnRequestTransport.Request parsed;
        final OwnRequestTransport.Lease lease;
        final NativeOwnActionEvidence moveSource;
        final NativeOwnSwitchEvidence switchSource;
        Fixture(boolean forced,boolean trapped){
            this.forced=forced;this.trapped=trapped;
            String text=raw(forced,trapped);
            parsed=OwnRequestJson.parse(text);
            ledger.owner(battle,actor,"p1");
            if(forced)ledger.turnDispatched(battle,1);
            ledger.dispatched(battle,actor,"p1",request,()->parsed);
            ledger.bindReadOnlyRaw(battle,actor,request,"p1",text,parsed);
            moveSource=forced?null:NativeOwnActionEvidence.capture(
                    battle,actor,request,"p1",0,text,parsed,"1.7.3+1.21.1");
            switchSource=NativeOwnSwitchEvidence.capture(
                    battle,actor,request,"p1",forced?1:0,text,parsed,
                    "1.7.3+1.21.1",ledger);
            if(!forced)ledger.turnDispatched(battle,1);
            lease=ledger.beginChoice(battle,actor,request,OWN,forced,()->true);
            check(lease.current(),"own installed source lease");
        }
        NativeOwnActionEvidence.Review moves(){
            if(forced)return null;
            return moveSource.attest(lease,new NativeOwnActionEvidence.InstalledView(
                    battle,actor,request,"p1",OWN,1,List.of("tackle"),trapped));
        }
        NativeOwnSwitchEvidence.Review switches(){
            return switchSource.attest(lease,new NativeOwnSwitchEvidence.InstalledView(
                    battle,actor,request,"p1",OWN,1,forced,
                    forced?null:trapped,List.of(
                        new NativeOwnSwitchEvidence.PartySlot(
                                OWN,forced?0:100,false,true,false),
                        new NativeOwnSwitchEvidence.PartySlot(
                                BENCH,70,false,false,true))));
        }
        NativeOwnActionFusion.Review fuse(NativeOwnActionFusion.Session session,
                NativeOwnActionEvidence.Review move,
                NativeOwnSwitchEvidence.Review change){
            return session.inspect(lease,battle,actor,request,"p1",OWN,1,forced,
                    move,change);
        }
    }
    static void run(){
        var ordinary=new Fixture(false,false);
        var m=ordinary.moves();var s=ordinary.switches();
        var session=new NativeOwnActionFusion.Session();
        var result=ordinary.fuse(session,m,s);
        check(result.current() && result.ownTurn()==1
                && result.mode()==NativeOwnActionFusion.Mode.ORDINARY_MOVE_AND_SWITCH
                && result.ownMoveCount()==1 && result.ownSwitchCandidateCount()==1,
                "source-identical native move+switch evidence fused");
        check(result.schema().equals(NativeOwnActionFusion.SCHEMA)
                && result.source().equals(OwnRequestTransport.SOURCE)
                && result.coverageFingerprint().matches("[0-9a-f]{64}")
                && !result.completeLegalSetProven()
                && !result.allInstalledSwitchesCertified()
                && !result.opponentActionsKnown()
                && !result.shadowSearchAuthorized()
                && !result.tacticalDecisionAuthorized()
                && !result.livePromotionAuthorized(),
                "stable schema/pin and permanently non-tactical coverage");
        check(!result.toString().contains(OWN.toString())
                && !result.toString().contains(BENCH.toString())
                && !result.toString().contains("PRIVATE")
                && !result.toString().contains(m.moveFingerprint()),
                "identity and move hashes not logged");
        var second=new Fixture(false,false);
        var secondReport=second.fuse(new NativeOwnActionFusion.Session(),
                second.moves(),second.switches());
        check(result.coverageFingerprint().equals(secondReport.coverageFingerprint()),
                "deterministic synthetic fingerprint for identical source evidence");
        var bridge=new ReadOnlyShadowObservationBridge();
        bridge.observe(ordinary.battle,
                OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        check(bridge.observeOwnFusedEvidence(ordinary.battle,result)
                && bridge.witnessedFusedMoveCount()==1
                && bridge.witnessedFusedSwitchCount()==1
                && !bridge.witnessedFusedForced(),
                "read-only Phase 6.9 same-battle observation sidecar");
        check(!bridge.observeOwnFusedEvidence(second.battle,secondReport),
                "foreign battle fusion is never accepted into sidecar");
        reject(()->ordinary.fuse(session,m,s),"duplicate consumed choice lease");
        check(session.withdrawn() && !result.current(),
                "replayed evidence poisons session and prior report");
        bridge.withdraw();
        check(bridge.witnessedFusedMoveCount()==0
                && bridge.witnessedFusedSwitchCount()==0,
                "bridge withdrawal erases fused diagnostics");

        var force=new Fixture(true,false);
        var forcedMove=force.moves();var forcedSwitch=force.switches();
        var forcedReport=force.fuse(new NativeOwnActionFusion.Session(),
                forcedMove,forcedSwitch);
        check(forcedReport.mode()==NativeOwnActionFusion.Mode.FORCED_REPLACEMENT_ONLY
                && forcedReport.ownMoveCount()==0
                && forcedReport.ownSwitchCandidateCount()==1
                && forcedReport.current() && !forcedReport.completeLegalSetProven(),
                "forced mode accepts switch-only source evidence");
        check(!forcedReport.coverageFingerprint().equals(result.coverageFingerprint()),
                "forced vs voluntary fingerprints are distinct");
        var bridgeForce=new ReadOnlyShadowObservationBridge();
        bridgeForce.observe(force.battle,
                OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        check(bridgeForce.observeOwnFusedEvidence(force.battle,forcedReport)
                && bridgeForce.witnessedFusedForced()
                && bridgeForce.witnessedFusedMoveCount()==0,
                "forced report exposed as count only");

        var trapped=new Fixture(false,true);
        var trappedResult=trapped.fuse(new NativeOwnActionFusion.Session(),
                trapped.moves(),trapped.switches());
        check(trappedResult.current()
                && trappedResult.ownMoveCount()==1
                && trappedResult.ownSwitchCandidateCount()==0
                && trappedResult.mode()==NativeOwnActionFusion.Mode.ORDINARY_MOVE_AND_SWITCH,
                "trapped choice has moves, zero switch candidates");
        check(!trappedResult.coverageFingerprint().equals(result.coverageFingerprint()),
                "trapped voluntary and open voluntary distinct fingerprints");
        trapped.ledger.endChoice();
        check(!trappedResult.current(),"owning choice completion expires fused report");

        var missingMove=new Fixture(false,false);
        reject(()->missingMove.fuse(new NativeOwnActionFusion.Session(),
                null,missingMove.switches()),"ordinary request without move evidence");
        var missingSwitch=new Fixture(false,false);
        reject(()->missingSwitch.fuse(new NativeOwnActionFusion.Session(),
                missingSwitch.moves(),null),"ordinary request without party evidence");
        var noForceSwitch=new Fixture(true,false);
        reject(()->noForceSwitch.fuse(new NativeOwnActionFusion.Session(),
                null,null),"forced request without switch receipt");
        var unwantedMove=new Fixture(true,false);
        var moveFromElsewhere=new Fixture(false,false);
        reject(()->unwantedMove.fuse(new NativeOwnActionFusion.Session(),
                moveFromElsewhere.moves(),unwantedMove.switches()),
                "forced mode must reject injected normal moves");

        var conflictingA=new Fixture(false,false);
        var conflictingB=new Fixture(false,false);
        reject(()->conflictingA.fuse(new NativeOwnActionFusion.Session(),
                conflictingA.moves(),conflictingB.switches()),
                "cross battle request with matching turn and action counts");
        var wrongLease=new Fixture(false,false);
        var alienLease=new Fixture(false,false);
        reject(()->wrongLease.fuse(new NativeOwnActionFusion.Session(),
                alienLease.moves(),wrongLease.switches()),
                "different owning choice generation rejects fused move");
        var differentTrap=new Fixture(false,true);
        reject(()->differentTrap.fuse(new NativeOwnActionFusion.Session(),
                differentTrap.moves(),new Fixture(false,false).switches()),
                "trapping contradiction cannot borrow another source receipt");

        var badActor=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badActor.lease,
                badActor.battle,new Object(),badActor.request,"p1",OWN,1,false,
                badActor.moves(),badActor.switches()),"actor spoof");
        var badToken=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badToken.lease,
                badToken.battle,badToken.actor,new Object(),"p1",OWN,1,false,
                badToken.moves(),badToken.switches()),"installed request token spoof");
        var badSide=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badSide.lease,
                badSide.battle,badSide.actor,badSide.request,"p2",OWN,1,false,
                badSide.moves(),badSide.switches()),"opponent side spoof");
        var badUuid=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badUuid.lease,
                badUuid.battle,badUuid.actor,badUuid.request,"p1",BENCH,1,false,
                badUuid.moves(),badUuid.switches()),"wrong active own identity");
        var badTurn=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badTurn.lease,
                badTurn.battle,badTurn.actor,badTurn.request,"p1",OWN,2,false,
                badTurn.moves(),badTurn.switches()),"stale or future source turn");
        var badMode=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badMode.lease,
                badMode.battle,badMode.actor,badMode.request,"p1",OWN,1,true,
                badMode.moves(),badMode.switches()),"contradictory forced flag");
        var badBattle=new Fixture(false,false);
        reject(()->new NativeOwnActionFusion.Session().inspect(badBattle.lease,
                new Object(),badBattle.actor,badBattle.request,"p1",OWN,1,false,
                badBattle.moves(),badBattle.switches()),"foreign source battle");
        var stale=new Fixture(false,false);
        var staleMoves=stale.moves();var staleSwitch=stale.switches();
        stale.ledger.endChoice();
        reject(()->stale.fuse(new NativeOwnActionFusion.Session(),
                staleMoves,staleSwitch),"expired choice lease");
        var revoked=new Fixture(false,false);
        var revMove=revoked.moves();var revSwitch=revoked.switches();
        revoked.moveSource.withdraw();
        reject(()->revoked.fuse(new NativeOwnActionFusion.Session(),revMove,revSwitch),
                "source withdrawal");
        var revokedSwitch=new Fixture(false,false);
        var revM=revokedSwitch.moves();var revS=revokedSwitch.switches();
        revokedSwitch.switchSource.withdraw();
        reject(()->revokedSwitch.fuse(new NativeOwnActionFusion.Session(),revM,revS),
                "party source withdrawal");

        var poison=new NativeOwnActionFusion.Session();
        var noEvidence=new Fixture(false,false);
        reject(()->noEvidence.fuse(poison,null,null),"no witnesses fail closed");
        var fresh=new Fixture(false,false);
        reject(()->fresh.fuse(poison,fresh.moves(),fresh.switches()),
                "poisoned fusion cannot be restarted in the same session");

        System.out.println("Phase 6.12 own-action fusion PASS: "+checks+" synthetic checks");
    }
}
