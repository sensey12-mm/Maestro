package com.maestroai.training;

import java.util.*;

/** Phase 6.11: adversarial owning-switch, trapping, forced-party evidence. */
final class NativeOwnSwitchEvidenceRegression {
    private NativeOwnSwitchEvidenceRegression() {}
    private static int checks;
    private static final UUID ACTIVE=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BENCH=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID FAINTED=UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static void check(boolean yes,String name){
        if(!yes)throw new AssertionError("Phase 6.11 "+name);
        checks++;
    }
    private static void reject(Runnable r,String name){
        try{r.run();}
        catch(IllegalArgumentException good){
            check("Phase 6.11 own switch evidence unavailable".equals(good.getMessage()),
                    name+" redacted");
            return;
        }
        throw new AssertionError("Phase 6.11 accepted "+name);
    }
    private static String pokemon(UUID uuid,String condition,boolean active,boolean commanding){
        return "{\"details\":\"Genesect, "+uuid+", L50\","
                +"\"ident\":\"p1: PRIVATE_NICKNAME\","
                +"\"active\":"+active+",\"condition\":\""+condition+"\","
                +"\"ability\":\"Download\",\"types\":[\"bug\",\"steel\"],"
                +"\"baseTypes\":[\"bug\",\"steel\"],"
                +"\"stats\":{\"atk\":90,\"def\":90,\"spa\":90,\"spd\":90,\"spe\":90},"
                +"\"item\":\"\",\"terastallized\":\"\","
                +"\"commanding\":"+commanding+",\"reviving\":false}";
    }
    private static String raw(boolean forced,boolean trapped){
        return "|request|{\"side\":{\"id\":\"p1\",\"pokemon\":["
                +pokemon(ACTIVE,forced?"0/100 fnt":"100/100",true,false)+","
                +pokemon(BENCH,"70/100",false,false)+","
                +pokemon(FAINTED,"0/100 fnt",false,false)+"]},"
                +(forced?"\"forceSwitch\":[true]":
                    "\"active\":[{\"trapped\":"+trapped+",\"moves\":["
                        +"{\"move\":\"Tackle\",\"id\":\"tackle\",\"pp\":10,"
                        +"\"maxpp\":20,\"target\":\"normal\",\"disabled\":false}]}]")+"}";
    }
    private static final class Fixture {
        final Object battle=new Object(),actor=new Object(),request=new Object();
        final OwnRequestTransport ledger=new OwnRequestTransport();
        final boolean forced,trapped;
        final String raw;
        final OwnRequestTransport.Request parsed;
        final NativeOwnSwitchEvidence witness;
        final OwnRequestTransport.Lease lease;
        Fixture(boolean forced,boolean trapped){
            this.forced=forced;this.trapped=trapped;raw=raw(forced,trapped);
            parsed=OwnRequestJson.parse(raw);
            ledger.owner(battle,actor,"p1");
            if(forced)ledger.turnDispatched(battle,1);
            ledger.dispatched(battle,actor,"p1",request,()->parsed);
            witness=NativeOwnSwitchEvidence.capture(
                    battle,actor,request,"p1",forced?1:0,raw,parsed,"1.7.3+1.21.1");
            if(!forced)ledger.turnDispatched(battle,1);
            lease=ledger.beginChoice(battle,actor,request,ACTIVE,forced,()->true);
            check(lease.current(),"actual own choice lease opens");
        }
        List<NativeOwnSwitchEvidence.PartySlot> party(){
            return List.of(
                new NativeOwnSwitchEvidence.PartySlot(ACTIVE,forced?0:100,false,true,false),
                new NativeOwnSwitchEvidence.PartySlot(BENCH,70,false,false,true),
                new NativeOwnSwitchEvidence.PartySlot(FAINTED,0,false,false,false));
        }
        NativeOwnSwitchEvidence.InstalledView view(){
            return with(battle,actor,request,"p1",ACTIVE,1,forced,
                    forced?null:trapped,party());
        }
        NativeOwnSwitchEvidence.Review attest(){return witness.attest(lease,view());}
    }
    private static NativeOwnSwitchEvidence.InstalledView with(
            Object battle,Object actor,Object request,String side,UUID uuid,
            int turn,boolean forced,Boolean trapped,
            List<NativeOwnSwitchEvidence.PartySlot> party){
        return new NativeOwnSwitchEvidence.InstalledView(
                battle,actor,request,side,uuid,turn,forced,trapped,party);
    }
    private static List<NativeOwnSwitchEvidence.PartySlot> changed(
            Fixture f,int index,NativeOwnSwitchEvidence.PartySlot replacement){
        var l=new ArrayList<>(f.party());l.set(index,replacement);return l;
    }

    static void run(){
        var move=new Fixture(false,false);
        var result=move.attest();
        check(result.currentlyObserved() && !result.forcedReplacementWitnessed()
                && !result.trappingWitnessed() && result.observedSwitchCount()==1
                && result.ownTurn()==1 && !result.completeLegalSetProven()
                && !result.executableSwitchSetCertified()
                && !result.shadowSearchAuthorized()&&!result.livePromotionAuthorized()
                && !result.tacticalDecisionAuthorized(),"voluntary narrow party candidate only");
        check(result.candidateFingerprint().equals(
                OfflineRuntimeEvidenceContract.actionFingerprint(List.of("switch:slot2"))),
                "stable slot-only fingerprint no UUID");
        check(!result.toString().contains(BENCH.toString())
                && !result.toString().contains("PRIVATE_NICKNAME"),
                "privacy-safe diagnostic report");
        reject(move::attest,"one-time consumed switch witness");
        move.ledger.endChoice();
        check(!result.currentlyObserved(),"choice expiration revokes returned receipt");

        var forced=new Fixture(true,false);
        var forcedResult=forced.attest();
        check(forcedResult.currentlyObserved() && forcedResult.forcedReplacementWitnessed()
                && forcedResult.observedSwitchCount()==1
                && !forcedResult.completeLegalSetProven(),
                "source-pinned forced replacement with one viable bench");
        var trapped=new Fixture(false,true);
        var trappedResult=trapped.attest();
        check(trappedResult.trappingWitnessed() && trappedResult.observedSwitchCount()==0
                && "NONE".equals(trappedResult.candidateFingerprint()),
                "trapped choice exposes no voluntary switch proposals");
        var bridge=new ReadOnlyShadowObservationBridge();
        bridge.observe(forced.battle,OfflineShadowResponseScenariosRegression.frames("Scratch"),true);
        check(bridge.observeOwnSwitchEvidence(forced.battle,forcedResult)
                && bridge.witnessedForcedReplacement()
                && bridge.witnessedOwnSwitchCount()==1,
                "Phase 6.9 sidecar accepts only redacted forced own-party count");
        check(!bridge.observeOwnSwitchEvidence(new Object(),forcedResult),
                "foreign battle cannot attach another own switch witness");
        forced.ledger.endChoice();
        check(!bridge.observeOwnSwitchEvidence(forced.battle,forcedResult),
                "expired owning lease rejected by sidecar");
        bridge.withdraw();
        check(bridge.witnessedOwnSwitchCount()==0&&!bridge.witnessedForcedReplacement(),
                "bridge withdrawal forgets sidecar observation");

        var swap=new Fixture(false,false);
        reject(()->swap.witness.attest(swap.lease,with(
                swap.battle,swap.actor,new Object(),"p1",ACTIVE,1,false,false,swap.party())),
                "installed request object substitution");
        check(swap.witness.withdrawn(),"substituted request revokes witness");
        var alien=new Fixture(false,false);
        reject(()->alien.witness.attest(alien.lease,with(
                alien.battle,new Object(),alien.request,"p1",ACTIVE,1,false,false,alien.party())),
                "actor swap");
        var battle=new Fixture(false,false);
        reject(()->battle.witness.attest(battle.lease,with(
                new Object(),battle.actor,battle.request,"p1",ACTIVE,1,false,false,battle.party())),
                "cross battle request");
        var side=new Fixture(false,false);
        reject(()->side.witness.attest(side.lease,with(
                side.battle,side.actor,side.request,"p2",ACTIVE,1,false,false,side.party())),
                "wrong side");
        var uuid=new Fixture(false,false);
        reject(()->uuid.witness.attest(uuid.lease,with(
                uuid.battle,uuid.actor,uuid.request,"p1",BENCH,1,false,false,uuid.party())),
                "wrong active own Pokémon");
        var turn=new Fixture(false,false);
        reject(()->turn.witness.attest(turn.lease,with(
                turn.battle,turn.actor,turn.request,"p1",ACTIVE,2,false,false,turn.party())),
                "stale or future turn");
        var mode=new Fixture(true,false);
        reject(()->mode.witness.attest(mode.lease,with(
                mode.battle,mode.actor,mode.request,"p1",ACTIVE,1,false,false,mode.party())),
                "forced request pretended voluntary");
        var forceFalse=new Fixture(false,false);
        reject(()->forceFalse.witness.attest(forceFalse.lease,with(
                forceFalse.battle,forceFalse.actor,forceFalse.request,"p1",ACTIVE,1,true,null,forceFalse.party())),
                "voluntary request pretended forced");
        var trap=new Fixture(false,true);
        reject(()->trap.witness.attest(trap.lease,with(
                trap.battle,trap.actor,trap.request,"p1",ACTIVE,1,false,false,trap.party())),
                "trapped request/native mismatch");
        var trapUnknown=new Fixture(false,true);
        reject(()->trapUnknown.witness.attest(trapUnknown.lease,with(
                trapUnknown.battle,trapUnknown.actor,trapUnknown.request,"p1",ACTIVE,1,false,null,trapUnknown.party())),
                "trapping unknown at installed choice");

        var partySwap=new Fixture(false,false);
        var swapped=new ArrayList<>(partySwap.party());
        Collections.swap(swapped,1,2);
        reject(()->partySwap.witness.attest(partySwap.lease,with(
                partySwap.battle,partySwap.actor,partySwap.request,"p1",ACTIVE,1,false,false,swapped)),
                "same member roster but wrong party indexes");
        var duplicate=new Fixture(false,false);
        var dup=changed(duplicate,2,new NativeOwnSwitchEvidence.PartySlot(BENCH,0,false,false,false));
        reject(()->duplicate.witness.attest(duplicate.lease,with(
                duplicate.battle,duplicate.actor,duplicate.request,"p1",ACTIVE,1,false,false,dup)),
                "duplicate installed UUID");
        var missing=new Fixture(false,false);
        reject(()->missing.witness.attest(missing.lease,with(
                missing.battle,missing.actor,missing.request,"p1",ACTIVE,1,false,false,
                missing.party().subList(0,2))),"missing installed party record");
        var hp=new Fixture(false,false);
        var hpChanged=changed(hp,1,new NativeOwnSwitchEvidence.PartySlot(BENCH,60,false,false,true));
        reject(()->hp.witness.attest(hp.lease,with(
                hp.battle,hp.actor,hp.request,"p1",ACTIVE,1,false,false,hpChanged)),
                "stale request bench HP");
        var noSend=new Fixture(false,false);
        var unavailable=changed(noSend,1,new NativeOwnSwitchEvidence.PartySlot(BENCH,70,false,false,false));
        reject(()->noSend.witness.attest(noSend.lease,with(
                noSend.battle,noSend.actor,noSend.request,"p1",ACTIVE,1,false,false,unavailable)),
                "healthy bench cannot actually be sent out");
        var gone=new Fixture(false,false);
        var retired=changed(gone,1,new NativeOwnSwitchEvidence.PartySlot(BENCH,70,true,false,false));
        reject(()->gone.witness.attest(gone.lease,with(
                gone.battle,gone.actor,gone.request,"p1",ACTIVE,1,false,false,retired)),
                "gone bench is not a candidate");
        var sentOut=new Fixture(false,false);
        var alreadyOut=changed(sentOut,1,new NativeOwnSwitchEvidence.PartySlot(BENCH,70,false,true,false));
        reject(()->sentOut.witness.attest(sentOut.lease,with(
                sentOut.battle,sentOut.actor,sentOut.request,"p1",ACTIVE,1,false,false,alreadyOut)),
                "bench already sent out");
        var resurrect=new Fixture(false,false);
        var dead=changed(resurrect,2,new NativeOwnSwitchEvidence.PartySlot(FAINTED,5,false,false,true));
        reject(()->resurrect.witness.attest(resurrect.lease,with(
                resurrect.battle,resurrect.actor,resurrect.request,"p1",ACTIVE,1,false,false,dead)),
                "fainted party member gains HP without a new dispatch");

        var expired=new Fixture(false,false);
        expired.ledger.endChoice();
        reject(expired::attest,"expired choice lease");
        var displaced=new Fixture(false,false);
        displaced.ledger.dispatched(displaced.battle,displaced.actor,"p1",
                new Object(),()->displaced.parsed);
        reject(displaced::attest,"new owning dispatch revokes prior lease");
        var forcedStale=new Fixture(true,false);
        forcedStale.ledger.turnDispatched(forcedStale.battle,2);
        reject(forcedStale::attest,"forced choice turn rollover");
        var revoked=new Fixture(true,false);
        revoked.witness.withdraw();
        reject(revoked::attest,"explicitly withdrawn native source");

        var parsed=OwnRequestJson.parse(raw(false,false));
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",0,raw(false,false),parsed,"wrong-version"),"source version drift");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p2",0,raw(false,false),parsed,"1.7.3+1.21.1"),"opposite side identity");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",0,raw(false,true),parsed,"1.7.3+1.21.1"),"raw party/trapping substitution");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",0,raw(false,false).replace("\"trapped\":false,",""),parsed,
                "1.7.3+1.21.1"),"missing trapped field");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",0,raw(false,false).replace("\"trapped\":false",
                    "\"maybeTrapped\":true,\"trapped\":false"),parsed,
                "1.7.3+1.21.1"),"unsupported native flag");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",0,raw(false,false).replace("\"trapped\":false",
                    "\"canMegaEvo\":true,\"trapped\":false"),parsed,
                "1.7.3+1.21.1"),"unsupported gimmick flags");
        var parsedForced=OwnRequestJson.parse(raw(true,false));
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",1,raw(true,false).replace("\"forceSwitch\":[true]",
                    "\"forceSwitch\":[false]"),parsedForced,
                "1.7.3+1.21.1"),"forceSwitch not actually requested");
        reject(()->NativeOwnSwitchEvidence.capture(new Object(),new Object(),new Object(),
                "p1",1,raw(true,false).replace("\"forceSwitch\":[true]",
                    "\"forceSwitch\":[true],\"active\":[{}]"),parsedForced,
                "1.7.3+1.21.1"),"forced request with unsupported active payload");

        System.out.println("Phase 6.11 native switch evidence PASS: "+checks+" checks");
    }
}
