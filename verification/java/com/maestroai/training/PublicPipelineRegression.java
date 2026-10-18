package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;

/** Exercises runtime providers together. SourceVerification supplies the actual
 * initialized Graal domain; all actors/requests here are artificial fixtures.
 */
final class PublicPipelineRegression {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    private static final UUID OWN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESERVE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static OwnRequestTransport.Pokemon pokemon(UUID id, boolean active) {
        var types=active ? List.of("ghost","poison") : List.of("psychic");
        int hp=active ? 261 : 341;
        var stats=active ? Map.of("atk",166,"def",156,"spa",296,"spd",186,"spe",256)
                : Map.of("atk",236,"def",236,"spa",236,"spd",236,"spe",236);
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Fixture",hp+"/"+hp,
                active,"Run Away",types),100,stats,"none",types,"",false);
    }
    private static BattleSnapshot.Combatant ownFacts(String species, OwnRequestTransport.Effective effective) {
        var e=effective.evidence();
        return new BattleSnapshot.Combatant(species,e.hp().value().low(),e.profile().types().value(),
                effective.stages(),List.of(),"none","runaway",false);
    }
    static void run(RuntimeDomain domain) {
        check(domain.effectiveState().known(),"pipeline needs an independently verified domain");
        Object battle=new Object(), actor=new Object(), firstToken=new Object(), token=new Object();
        var ledger=new OwnRequestTransport(); ledger.owner(battle,actor,"p1");
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
                List.of(pokemon(OWN,true),pokemon(RESERVE,false)));
        ledger.dispatched(battle,actor,"p1",firstToken,()->request);
        ledger.turnDispatched(battle,1); ledger.turnFinished(battle,1);
        ledger.turnDispatched(battle,2); ledger.turnFinished(battle,2);
        ledger.dispatched(battle,actor,"p1",token,()->request);
        ledger.turnDispatched(battle,3);
        var lease=ledger.beginChoice(battle,actor,token,OWN,false,()->true);
        check(lease.current(),"fresh own dispatch must reach turn3");
        var protocol=new PublicProtocolEvidence();
        String start="update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: "+OWN+"|Gengar, L100|100/100\n"
                +"|switch|p2a: Public|Mew, L100|100/100\n|-ability|p2a: Public|Pressure\n"
                +"|turn|1\n|-enditem|p2a: Public|Sitrus Berry\n"
                +"|move|p2a: Public|Tackle|p1a: "+OWN+"\n|turn|2\n"
                +"|move|p2a: Public|Quick Attack|p1a: "+OWN+"\n|turn|3";
        protocol.update(List.of(start));
        var provider=new PublicMechanicsEvidence(id->id.equals("mew")
                ? new PublicMechanicsEvidence.Species(Map.of("hp",100,"atk",100,"def",100,"spa",100,"spd",100,"spe",100),List.of("psychic"),null) : null);
        provider.update(protocol); var fields=new PublicFieldEvidence(); var field=fields.update(protocol,provider);
        var selfSeen=provider.observation("p1a"); var foeSeen=provider.observation("p2a");
        var self=ledger.effective(lease,"self",OWN,false,domain,protocol,selfSeen,field);
        var reserve=ledger.effective(lease,"reserve",RESERVE,true,domain,protocol,selfSeen,field);
        check(self.missing().isEmpty() && reserve.missing().isEmpty(),"later never-used reserve lost construction proof: "+reserve.missing());
        var scope=new PublicMechanicsEvidence.Scope(domain.effectiveState(),field.trickRoom());
        var foe=provider.bind("opponent",foeSeen,scope);
        check(foe.unknown().isEmpty(),"public provider failed source-supported positive entry: "+foe.unknown());
        var stages=field.stages(foeSeen,domain.effectiveState());
        check(stages.size()==7 && stages.values().stream().allMatch(n->n==0),"witnessed clearVolatile did not bind zero stages");
        check(field.stages(foeSeen,Known.unknown()).isEmpty(),"unknown source inferred neutral stages");
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(selfSeen,self.evidence().item(),self.evidence().ability(),self.evidence().fresh(),lease),
                "reserve",new ScopedExecution.Binding(null,reserve.evidence().item(),reserve.evidence().ability(),reserve.evidence().fresh(),lease),
                "opponent",new ScopedExecution.Binding(foeSeen,foeSeen.item(),foeSeen.ability())),domain.scopes());
        var own=ownFacts("Gengar",self); var bench=ownFacts("Mew",reserve);
        var observer=new PublicBattleObserver(); protocol.mechanicsEvents().forEach(observer::accept);
        var opponent=PublicShadowSnapshot.opponent(observer,"p2a");
        var snapshot=new BattleSnapshot(3,own,opponent,List.of(bench),List.of(),
                List.of("scratch","shadowclaw","swordsdance"),List.of("reserve"),Map.of(),false,null);
        var hazards=field.hazards("p1",domain.effectiveState());
        check(!field.hazards("p1",Known.unknown()).complete(),"unknown source inferred absent hazards");
        var entry=execution.entry(hazards,Known.of(341,lease.source()),reserve.evidence().profile());
        check(entry.damage().known() && entry.damage().value().high()==0,"empty witnessed field must justify zero entry damage: "+entry+" / "+execution.entryCoverage(hazards,Known.of(341,lease.source()),reserve.evidence().profile()).missing());
        var target=new SwitchTarget(bench,reserve.evidence().status(),entry.damage(),entry.speedDelta());
        var context=new Context(self.evidence().hp(),foeSeen.hp(),self.evidence().status(),foeSeen.status(),
                hazards,protocol.sideHazards("p2"),Map.of("reserve",target),Map.of(),Set.of("APPEARANCE_ONLY"));
        var mechanics=new PublicMechanics(Map.of(),stages,Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
        var profiles=Map.of("self",self.evidence().profile(),"reserve",reserve.evidence().profile(),"opponent",foe.profile());
        // Source-known post-boot definitions do not imply this limited tactical
        // model supports the selected actor's item or its effects.
        for(String unsupported:List.of("eviolite","legendplate","pikaniumz","pikashuniumz","zygardite","tatsugirinite","ange")) {
            var limited=new ScopedExecution(field,Map.of("self",new ScopedExecution.Binding(selfSeen,
                    Known.of(unsupported,"explicit own item fixture"),self.evidence().ability(),self.evidence().fresh(),lease)),domain.scopes());
            var ownState=PublicStateSearchAdapter.project(snapshot,mechanics).states().get("root").self();
            var certificate=limited.setupCoverage(self.evidence().profile(),ownState,"swordsdance");
            check(!certificate.supported()&&certificate.missing().contains("self:CURRENT_ITEM"),"source-known unsupported item gained tactical scope: "+unsupported);
        }
        for(String unsupported:List.of("megasol","dragonize","piercingdrill","powerconstruct","spicyspray")){
            var limited=new ScopedExecution(field,Map.of("self",new ScopedExecution.Binding(selfSeen,
                    self.evidence().item(),Known.of(unsupported,"explicit own ability fixture"),self.evidence().fresh(),lease)),domain.scopes());
            var ownState=PublicStateSearchAdapter.project(snapshot,mechanics).states().get("root").self();
            var certificate=limited.setupCoverage(self.evidence().profile(),ownState,"swordsdance");
            check(!certificate.supported()&&certificate.missing().contains("self:CURRENT_ABILITY"),"source-known unsupported ability gained tactical scope: "+unsupported);
        }
        var audit=PublicReplyCoverage.evaluate(snapshot,mechanics,profiles,execution,protocol,"p2a",field.trickRoom(),Critical.BOTH);
        check(audit.compared() && audit.allCandidates() && audit.replyCount()==2,"production reply coverage not reached: "+audit.reasons());
        check(audit.result().rankings().stream().allMatch(r->r.utility().known()),"supported provider pipeline still unbounded: "+audit.result().rankings());
        // Fixed independent Node oracle extrema over defense184..328, HP310..404,
        // both critical states and all sixteen rolls, not Java-generated values.
        for (var rank:audit.result().rankings()) {
            if (rank.action().equals("move:scratch")) check(rank.utility().value().low()==16.0/404
                    && rank.utility().value().high()==48.0/310,"scratch source-oracle extrema");
            if (rank.action().equals("move:shadowclaw")) check(rank.utility().value().low()==78.0/404
                    && rank.utility().value().high()==246.0/310,"Shadow Claw source-oracle extrema");
        }
        check("move:shadowclaw".equals(audit.result().preferredConditional()),"source-bounded immediate exchange should dominate scratch, setup and switch");
        var plans=audit.result().rankings().stream().map(r->new ShadowMechanicsEvaluation.Plan(r.action(),
                List.of(List.of(new ShadowMechanicsEvaluation.Turn(r.action(),"tackle",null)),
                        List.of(new ShadowMechanicsEvaluation.Turn(r.action(),"quickattack",null))),"paired public fixture")).toList();
        var comparison=ShadowMechanicsEvaluation.compare(snapshot,mechanics,profiles,plans,field.trickRoom(),Critical.BOTH,"move:swordsdance",execution);
        check(comparison.disagreement() && comparison.simpleAction().equals("move:shadowclaw"),"fixed legacy/setup and nominal baselines not recorded independently");
        var setup=comparison.rankings().stream().filter(r->r.action().equals("move:swordsdance")).findFirst().orElseThrow();
        check(setup.lines().stream().allMatch(l->l.state().self().stages().get("atk")==2),"setup must change actual conditional state, not receive an arbitrary bonus");
        var unknownDomain=new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,Map.of()),Map.of(),false,()->true);
        check(ledger.effective(lease,"reserve",RESERVE,true,unknownDomain,protocol,selfSeen,field).evidence().profile().stats().isEmpty(),"unsupported pack source certified reserve stats");
        // A reserve first appearing in a later request did not witness initial
        // construction. It must not borrow the other ledger's positive proof.
        var late=new OwnRequestTransport(); late.owner(battle,actor,"p1");
        late.dispatched(battle,actor,"p1",firstToken,()->new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(pokemon(OWN,true))));
        late.turnDispatched(battle,1); late.turnFinished(battle,1); late.turnDispatched(battle,2); late.turnFinished(battle,2);
        late.dispatched(battle,actor,"p1",token,()->request); late.turnDispatched(battle,3);
        var lateLease=late.beginChoice(battle,actor,token,OWN,false,()->true);
        check(late.effective(lateLease,"reserve",RESERVE,true,domain,protocol,selfSeen,field).missing().contains("OWN_RESERVE_CLEAR_LIFECYCLE_UNJUSTIFIED"),"new reserve inherited another actor/ledger's construction proof");
        late.owner(new Object(),actor,"p1");
        check(!lateLease.current(),"new battle revived an old owning receipt");
        // A full source receipt cannot repair a partial or unsupported field.
        for (String event:List.of("|-swapsideconditions", "|-transform|p2a: Public|p1a: "+OWN,
                "|move|p2a: Public|Spikes|p1a: "+OWN+"|[from] move: Metronome",
                "|-start|p2a: Public|Custom Effect", "|win|Public fixture")) {
            var changed=new PublicProtocolEvidence(); changed.update(List.of(start,"update\n"+event));
            var p=new PublicMechanicsEvidence(id->null);p.update(changed);
            var c=new PublicFieldEvidence().update(changed,p);
            check(!c.hazards("p1",domain.effectiveState()).complete(),"unsupported/terminal event certified a complete hazard scope");
            check(c.stages(p.observation("p2a"),domain.effectiveState()).isEmpty(),"unsupported/terminal event certified initial stages");
        }
        var layered=new PublicProtocolEvidence(); layered.update(List.of(start,"update\n|move|p2a: Public|Spikes|p1a: "+OWN+"\n|-sidestart|p1|Spikes\n|move|p2a: Public|Spikes|p1a: "+OWN+"\n|-sidestart|p1|Spikes\n|move|p2a: Public|Stealth Rock|p1a: "+OWN+"\n|-sidestart|p1|Stealth Rock"));
        var layerEvidence=new PublicMechanicsEvidence(id->null);layerEvidence.update(layered);
        var layeredField=new PublicFieldEvidence().update(layered,layerEvidence);
        var layerHazards=layeredField.hazards("p1",domain.effectiveState());
        check(layerHazards.complete() && layerHazards.layers().equals(Map.of("spikes",2,"stealthrock",1)),"explicit layer transitions were lost when closing the hazard scope");
        var hazardExecution=new ScopedExecution(layeredField,Map.of("reserve",
                new ScopedExecution.Binding(null,reserve.evidence().item(),reserve.evidence().ability(),reserve.evidence().fresh(),lease)),domain.scopes());
        var hazardEntry=hazardExecution.entry(layerHazards,Known.of(341,lease.source()),reserve.evidence().profile());
        // Independent pinned-engine oracle: floor(341/6)+floor(341/8)=98.
        check(hazardEntry.damage().known() && hazardEntry.damage().value().low()==98.0/341
                && hazardEntry.damage().value().high()==98.0/341,"source-oracle two-layer entry mismatch");
        ledger.endChoice();
        check(!execution.setupCoverage(self.evidence().profile(),PublicStateSearchAdapter.project(snapshot,mechanics).states().get("root").self(),"swordsdance").supported(),"consumed lease retained a setup certificate");
        fields.reset();
        boolean expired=false; try { field.stages(foeSeen,domain.effectiveState()); } catch(IllegalArgumentException expected){expired=true;}
        check(expired,"old stage receipt survived field reset");
        System.out.println("Public provider pipeline PASS: fresh owning dispatch, turn3 reserve, Pressure identity, bounded four-action/two-reply comparison, fixed baselines and withdrawal");
    }
}
