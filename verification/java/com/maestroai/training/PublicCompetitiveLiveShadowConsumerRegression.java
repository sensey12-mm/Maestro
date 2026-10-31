package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Phase 5 v2 actual source-backed live public diagnostics, synthetic tests. */
final class PublicCompetitiveLiveShadowConsumerRegression {
    private PublicCompetitiveLiveShadowConsumerRegression(){}
    private static int checks;
    private static final String SECRET="OPPONENT_PRIVATE_UUID_SECRET_521";
    private static void check(boolean x,String label){
        if(!x)throw new AssertionError("Phase5 live Shadow: "+label);
        checks++;
    }
    private static void reject(Runnable run,String reason){
        try{run.run();}
        catch(IllegalArgumentException ok){check(true,reason);return;}
        throw new AssertionError("Phase5 online request accepted "+reason);
    }
    private static String split(String side,String priv,String pub){
        return "update\n|split|"+side+"\n"+priv+"\n"+pub;
    }
    private static List<String> history(String... events){
        var out=new ArrayList<String>();
        out.add(split("p1","|switch|p1a: Own|Genesect, L100|70/100",
                "|switch|p1a: Own|Genesect, L100|100/100"));
        out.add(split("p2","|switch|p2a: "+SECRET+"|Glimmora, L100|25/100",
                "|switch|p2a: "+SECRET+"|Glimmora, L100|100/100"));
        out.addAll(List.of(events));
        out.add("update\n|turn|1");
        return List.copyOf(out);
    }
    private record Fixture(Object battle,Object own,Object opposite,List<Object> actors,
                           NativeSidedDispatchReachability.Session nativeOwner,
                           NativeSidedDispatchReachability.Report nativeReturn,
                           NativeOpponentSideProvenance.Session sideOwner,
                           NativeOpponentSideProvenance.Report nativeSide,
                           PublicSplitRoutingGate.Session routingOwner,
                           PublicSplitRoutingGate.Report routing,
                           PublicOpponentIdentityContinuity.Session identityOwner,
                           PublicOpponentIdentityContinuity.Report identity,
                           List<String> history){}
    private static Fixture fixture(Object battle,boolean ownP2,List<String> history){
        Object own=new Object(),opposite=new Object();
        var actors=List.of(own,opposite);
        var nativeOwner=new NativeSidedDispatchReachability.Session();
        var receipt=nativeOwner.nativeReturn(battle,actors,own,
                new Object(),new Object(),false,1,history.size(),true,false);
        var sideOwner=new NativeOpponentSideProvenance.Session();
        var nativeSide=sideOwner.observeSyntheticNativeReturn(
                battle,actors,own,opposite,own,ownP2?"p2":"p1",
                ownP2?"p1":"p2",new Object(),new Object(),receipt);
        var routingOwner=new PublicSplitRoutingGate.Session();
        var routing=routingOwner.inspect(battle,history,true);
        var identityOwner=new PublicOpponentIdentityContinuity.Session();
        var identity=identityOwner.inspect(battle,routing,history);
        check(receipt.current() && nativeSide.current() && routing.current()
                && identity.current() && nativeSide.verdict()
                    ==NativeOpponentSideProvenance.Verdict.NATIVE_SIDE_OBSERVED_UNLINKED,
                "original native/public sources accepted before online Shadow");
        return new Fixture(battle,own,opposite,actors,nativeOwner,receipt,sideOwner,
                nativeSide,routingOwner,routing,identityOwner,identity,history);
    }
    private static PublicCompetitiveLiveShadowConsumer.Report evaluate(
            PublicCompetitiveLiveShadowConsumer.Session c,Fixture f){
        return c.inspect(f.battle(),f.nativeReturn(),f.nativeSide(),
                f.routing(),f.identity(),f.history(),
                PublicCompetitiveLiveShadowConsumer.Request.pinned());
    }
    private static void bounded(PublicCompetitiveLiveShadowConsumer.Report r){
        check(!r.historicalBuildLinked()
                && !r.individualPokemonIdentityProven()
                && !r.opponentFutureResponsePredicted()
                && !r.calibratedOpponentProbabilities()
                && !r.hiddenItemsOrAbilitiesExposed()
                && !r.completeInstalledLegalActions()
                && !r.phase5OfflineConsumerModified()
                && !r.tacticalDecisionAuthorized()
                && !r.shadowSearchAuthorized()
                && !r.legacyDecisionAuthorized()
                && !r.livePromotionAuthorized()
                && !r.privatePayloadExportable(),
                "public capability never grants opponent builds or actions");
        String safe=r.toString();
        check(!safe.contains(SECRET) && !safe.contains("Glimmora")
                && !safe.contains("Genesect") && !safe.contains("p2a:")
                && !safe.contains("stealthrock") && !safe.contains("Stealth Rock")
                && !safe.contains("Swords Dance") && !safe.contains("Toxic Spikes")
                && !safe.contains("java.lang.Object@")
                && safe.contains("identity=UNKNOWN")
                && safe.contains("historicalBuild=UNBOUND")
                && safe.contains("probabilities=NONE")
                && safe.contains("authority=NONE"),
                "only fixed capability labels and bounded counters leave diagnostic");
    }
    /** Same synthetic source-owned Phase 6 native/public callback reports. */
    record SyntheticLiveFixture(PublicCompetitiveLiveShadowConsumer.Report live,
            PublicSplitRoutingGate.Session routing, Object physicalBattle,
            List<String> original){}
    static SyntheticLiveFixture syntheticLiveSource(){
        var original=history("update\n|move|p2a: "+SECRET+"|Stealth Rock|p1a: Own");
        var f=fixture(new Object(),false,original);
        var live=evaluate(new PublicCompetitiveLiveShadowConsumer.Session(),f);
        check(live.current(),"synthetic online source ready for historical role testing");
        return new SyntheticLiveFixture(live,f.routingOwner(),f.battle(),original);
    }

    static void run() throws Exception {
        var pin=PublicCompetitiveLiveShadowConsumer.Request.pinned();
        check(pin.version()==2
                && pin.installedSource().equals(ReadOnlyShadowObservationBridge.COBBLEMON_VERSION)
                && pin.offlinePin().equals(PublicCompetitiveConsumerContract.ContractPin.pinned()),
                "v2 pins exact public/native source and unchanged Phase5 v1");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                3,pin.installedSource(),pin.offlinePin(),
                pin.purpose(),false,false,false),"unknown version");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                2,"other-cobblemon",pin.offlinePin(),
                pin.purpose(),false,false,false),"mismatched installed source");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                2,pin.installedSource(),pin.offlinePin(),
                PublicCompetitiveLiveShadowConsumer.Purpose.OPPONENT_RESPONSE_PREDICTION,
                false,false,false),"fabricated response forecast");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                2,pin.installedSource(),pin.offlinePin(),
                PublicCompetitiveLiveShadowConsumer.Purpose.LIVE_ACTION_SELECTION,
                false,false,false),"live chooser request");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                2,pin.installedSource(),pin.offlinePin(),
                pin.purpose(),false,true,false),"historical appearance join");
        reject(()->new PublicCompetitiveLiveShadowConsumer.Request(
                2,pin.installedSource(),pin.offlinePin(),
                pin.purpose(),true,false,false),"hidden opponent input");
        var original=history("update\n|move|p2a: "+SECRET+"|Stealth Rock|p1a: Own",
                "update\n|move|p2a: "+SECRET+"|Swords Dance|p1a: Own",
                "update\n|move|p2a: "+SECRET+"|Toxic Spikes|p1a: Own");
        var f=fixture(new Object(),false,original);
        var c=new PublicCompetitiveLiveShadowConsumer.Session();
        check(c.inspect(f.battle(),null,null,null,null,null,pin).verdict()
                    ==PublicCompetitiveLiveShadowConsumer.Verdict.MISSING_SOURCE
                && !c.withdrawn(),"no positive from absent source");
        var positive=evaluate(c,f);
        check(positive.current() && positive.verdict()
                    ==PublicCompetitiveLiveShadowConsumer.Verdict.PUBLICLY_OBSERVED_CAPABILITY_ONLY
                && positive.publicOpponentSide()==NativeOpponentSideProvenance.Side.P2
                && positive.publiclyObservedMoveEvents()==3
                && positive.observedCapabilities().equals(Set.of(
                    PublicCompetitiveLiveShadowConsumer.Capability.HAZARD_SET_OBSERVED,
                    PublicCompetitiveLiveShadowConsumer.Capability.SETUP_OBSERVED,
                    PublicCompetitiveLiveShadowConsumer.Capability.PUBLIC_MOVE_EFFECT_UNKNOWN))
                && positive.conditionalPublicRoleHypotheses().equals(Set.of(
                    PublicCompetitiveLiveShadowConsumer.RoleHypothesis.HAZARD_SETTER_POSSIBLE,
                    PublicCompetitiveLiveShadowConsumer.RoleHypothesis.SETUP_USER_POSSIBLE)),
                "witnessed hazard, setup and unsupported Toxic Spikes are distinct");
        bounded(positive);
        check(evaluate(c,f).verdict()
                    ==PublicCompetitiveLiveShadowConsumer.Verdict.REPLAY_OR_REGRESSION
                && c.withdrawn() && !positive.current()
                && positive.observedCapabilities().isEmpty()
                && positive.conditionalPublicRoleHypotheses().isEmpty(),
                "duplicate original native epoch revokes previous public capability");
        bounded(positive);

        var reverse=fixture(new Object(),true,
                history("update\n|move|p1a: Own|Recover|p2a: "+SECRET));
        var out=evaluate(new PublicCompetitiveLiveShadowConsumer.Session(),reverse);
        check(out.current() && out.publicOpponentSide()
                    ==NativeOpponentSideProvenance.Side.P1
                && out.observedCapabilities().equals(Set.of(
                    PublicCompetitiveLiveShadowConsumer.Capability.RECOVERY_OBSERVED))
                && out.conditionalPublicRoleHypotheses().equals(Set.of(
                    PublicCompetitiveLiveShadowConsumer.RoleHypothesis.SUSTAIN_USER_POSSIBLE)),
                "reverse p2-owned scenario never assumes opponent p2");
        bounded(out);

        var forged=fixture(new Object(),false,original);
        var changed=new ArrayList<>(original);
        changed.set(1,split("p2",
                "|switch|p2a: "+SECRET+"|Glimmora, L100|95/100",
                "|switch|p2a: "+SECRET+"|Glimmora, L100|100/100"));
        var tampered=new PublicCompetitiveLiveShadowConsumer.Session()
                .inspect(forged.battle(),forged.nativeReturn(),forged.nativeSide(),
                        forged.routing(),forged.identity(),changed,pin);
        check(tampered.verdict()
                ==PublicCompetitiveLiveShadowConsumer.Verdict.SOURCE_CUSTODY_MISMATCH,
                "mutating private split half cannot forge public source");
        bounded(tampered);
        var second=fixture(forged.battle(),false,original);
        var crossover=new PublicCompetitiveLiveShadowConsumer.Session()
                .inspect(forged.battle(),forged.nativeReturn(),forged.nativeSide(),
                        second.routing(),forged.identity(),original,pin);
        check(crossover.verdict()
                ==PublicCompetitiveLiveShadowConsumer.Verdict.SOURCE_CUSTODY_MISMATCH,
                "same battle, equal public bytes but separate original history source");
        var foreign=new PublicCompetitiveLiveShadowConsumer.Session().inspect(
                new Object(),forged.nativeReturn(),forged.nativeSide(),
                forged.routing(),forged.identity(),original,pin);
        check(foreign.verdict()
                ==PublicCompetitiveLiveShadowConsumer.Verdict.SOURCE_STALE
                || foreign.verdict()==PublicCompetitiveLiveShadowConsumer.Verdict.SOURCE_CUSTODY_MISMATCH,
                "foreign physical battle cannot borrow native/public evidence");

        var stale=fixture(new Object(),false,original);
        var online=new PublicCompetitiveLiveShadowConsumer.Session();
        var accepted=evaluate(online,stale);
        var advanced=new ArrayList<>(original);
        advanced.add("update\n|move|p2a: "+SECRET+"|Recover|p1a: Own");
        stale.routingOwner().inspect(stale.battle(),advanced,true);
        check(!accepted.current() && accepted.observedCapabilities().isEmpty()
                && online.inspect(stale.battle(),stale.nativeReturn(),
                    stale.nativeSide(),stale.routing(),stale.identity(),
                    stale.history(),pin).verdict()
                    ==PublicCompetitiveLiveShadowConsumer.Verdict.SOURCE_STALE,
                "new public history invalidates old positive report dynamically");
        bounded(accepted);

        String[] ambiguities={
                "update\n|replace|p2a: "+SECRET+"|Zoroark, L100|100/100",
                "update\n|-transform|p2a: "+SECRET+"|p1a: Own",
                "update\n|detailschange|p2a: "+SECRET+"|Glimmora-Rare, L100|100/100",
                "update\n|-formechange|p2a: "+SECRET+"|Glimmora-Rare",
                split("p2","|switch|p2a: "+SECRET+"|Glimmora, L100|70/100",
                        "|switch|p2a: "+SECRET+"|Glimmora, L100|100/100"),
                split("p2","|switch|p2b: Other|Pikachu, L100|70/100",
                        "|switch|p2b: Other|Pikachu, L100|100/100")};
        for(int i=0;i<ambiguities.length;i++){
            var h=new ArrayList<>(original);
            h.add(ambiguities[i]);
            var af=fixture(new Object(),false,List.copyOf(h));
            var a=evaluate(new PublicCompetitiveLiveShadowConsumer.Session(),af);
            check(!a.current() && a.observedCapabilities().isEmpty()
                    && !a.shadowSearchAuthorized(),
                    "Illusion Transform form re-entry and doubles abstain "+i);
            bounded(a);
        }

        var ending=fixture(new Object(),false,original);
        var endingSession=new PublicCompetitiveLiveShadowConsumer.Session();
        var beforeEnd=evaluate(endingSession,ending);
        check(beforeEnd.current() && !endingSession.battleEnded(new Object())
                && beforeEnd.current(),"foreign battle end ignored");
        check(endingSession.battleEnded(ending.battle()) && !beforeEnd.current()
                && endingSession.withdrawn(),"actual battle end revokes observations");

        var legacy=Files.readString(Path.of(
                "src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        check(legacy.contains("maestroai.phase5LiveShadowObserve")
                && legacy.contains("Phase 5 online Shadow")
                && legacy.contains("LivePromotionGate.choose(")
                && legacy.contains("null, null, false);")
                && !legacy.contains("PublicCompetitiveLiveShadowConsumer.Session.choose(")
                && !legacy.contains("phase5LiveScope.choose("),
                "real callback uses Phase 5 v2 as diagnostic, not chooser");
        check(PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned()
                    .purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,
                "Phase 5 v1 signed-replay stays offline only");
        System.out.println("Phase 5 v2 online Shadow public diagnostic PASS: "+
                checks+" synthetic custody and authority checks; Minecraft UNTESTED");
    }
}
