package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Natural species/ability/learnset fixture, independently checked by Node.
 * This is a source-engine/provider regression, never Minecraft acceptance.
 */
final class NaturalAcceptanceRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final UUID OWN=UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID BENCH=UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static OwnRequestTransport.Pokemon pokemon(boolean active) {
        var types=active?List.of("dark","ice"):List.of("psychic"); int hp=active?145:181;
        var stats=active?Map.of("atk",112,"def",93,"spa",65,"spd",105,"spe",145)
                :Map.of("atk",103,"def",121,"spa",174,"spd",110,"spe",150);
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(active?OWN:BENCH,"p1: Fixture",
                hp+"/"+hp,active,"Pressure",types),50,stats,"none",types,"",false);
    }
    private static BattleSnapshot.Combatant facts(String name,OwnRequestTransport.Effective own){
        var e=own.evidence();return new BattleSnapshot.Combatant(name,1,e.profile().types().value(),own.stages(),List.of(),"none","pressure",false);
    }
    static void run(RuntimeDomain domain) {
        Object battle=new Object(),actor=new Object(),token=null;var ledger=new OwnRequestTransport();ledger.owner(battle,actor,"p1");
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(pokemon(true),pokemon(false)));
        for(int t=1;t<=6;t++) {
            token=new Object();ledger.dispatched(battle,actor,"p1",token,()->request);ledger.turnDispatched(battle,t);
            if(t<6)ledger.turnFinished(battle,t);
        }
        var lease=ledger.beginChoice(battle,actor,token,OWN,false,()->true);
        String head="update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: "+OWN+"|Weavile, L50|100/100\n"
                +"|-ability|p1a: "+OWN+"|Pressure\n|switch|p2a: Setter|Skarmory, L100|100/100\n|turn|1\n"
                +"|move|p2a: Setter|Spikes|p1a: "+OWN+"\n|-sidestart|p1|Spikes\n|turn|2\n"
                +"|move|p2a: Setter|Stealth Rock|p1a: "+OWN+"\n|-sidestart|p1|Stealth Rock\n|turn|3\n"
                +"|switch|p2a: Public|Mewtwo, L100|100/100\n|-ability|p2a: Public|Pressure\n"
                +"|move|p1a: "+OWN+"|Swords Dance|p1a: "+OWN+"\n|-boost|p1a: "+OWN+"|atk|2\n|turn|4\n"
                +"|move|p2a: Public|Psycho Cut|p1a: "+OWN+"\n|-immune|p1a: "+OWN+"\n"
                +"|move|p1a: "+OWN+"|Swords Dance|p1a: "+OWN+"\n|-boost|p1a: "+OWN+"|atk|2\n|turn|5\n"
                +"|move|p2a: Public|Bulk Up|p2a: Public\n|-boost|p2a: Public|atk|1\n|-boost|p2a: Public|def|1\n"
                +"|move|p1a: "+OWN+"|Swords Dance|p1a: "+OWN+"\n|-boost|p1a: "+OWN+"|atk|2\n";
        String removal="|-enditem|p2a: Public|Sitrus Berry|[eat]\n|-heal|p2a: Public|60/100|[from] item: Sitrus Berry\n";
        for(boolean consumed:List.of(false,true)) {
            var protocol=new PublicProtocolEvidence();protocol.update(List.of(head+(consumed?removal:"")+"|turn|6"));
            var provider=new PublicMechanicsEvidence(id->PublicSpeciesCatalog.verified(id,
                    new PublicMechanicsEvidence.Species(Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null)));
            provider.update(protocol);var field=new PublicFieldEvidence().update(protocol,provider);
            var a=provider.observation("p1a");var f=provider.observation("p2a");
            var self=ledger.effective(lease,"self",OWN,false,domain,protocol,a,field);
            var reserve=ledger.effective(lease,"reserve",BENCH,true,domain,protocol,a,field);
            check(self.missing().isEmpty()&&reserve.missing().isEmpty(),"natural field/own lifecycle rejected: "+self.missing()+reserve.missing());
            var foe=provider.bind("opponent",f,new PublicMechanicsEvidence.Scope(domain.effectiveState(),field.trickRoom()));
            check(foe.profile().ordinaryEffects().known()==consumed,"missing item announcement inferred absence");
            var stages=field.stages(f,domain.effectiveState());check(stages.get("def")==1,"public Bulk Up stage lost");
            var execution=new ScopedExecution(field,Map.of("self",new ScopedExecution.Binding(a,self.evidence().item(),self.evidence().ability(),self.evidence().fresh(),lease),
                    "reserve",new ScopedExecution.Binding(null,reserve.evidence().item(),reserve.evidence().ability(),reserve.evidence().fresh(),lease),
                    "opponent",new ScopedExecution.Binding(f,f.item(),f.ability())),domain.scopes());
            var observer=new PublicBattleObserver();protocol.mechanicsEvents().forEach(observer::accept);
            var own=facts("Weavile",self);var bench=facts("Mewtwo",reserve);
            var snapshot=new BattleSnapshot(6,own,PublicShadowSnapshot.opponent(observer,"p2a"),List.of(bench),List.of(),List.of("shadowclaw","swordsdance"),List.of("reserve"),Map.of(),false,null);
            var hazards=field.hazards("p1",domain.effectiveState());
            check(hazards.complete()&&hazards.layers().equals(Map.of("spikes",1,"stealthrock",1)),"actual hazard move poisoned later field coverage");
            var entry=execution.entry(hazards,Known.of(181,lease.source()),reserve.evidence().profile());
            check(entry.damage().known()&&entry.damage().value().high()==44.0/181,"natural reserve hazard entry oracle");
            var context=new Context(self.evidence().hp(),f.hp(),self.evidence().status(),f.status(),hazards,
                    protocol.sideHazards("p2"),Map.of("reserve",new SwitchTarget(bench,reserve.evidence().status(),entry.damage(),entry.speedDelta())),Map.of(),Set.of("APPEARANCE_ONLY"));
            var mechanics=new PublicMechanics(Map.of(),stages,Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
            var profiles=Map.of("self",self.evidence().profile(),"reserve",reserve.evidence().profile(),"opponent",foe.profile());
            if(consumed) {
                var root=PublicStateSearchAdapter.project(snapshot,mechanics).states().get("root");
                var damage=execution.damage("shadowclaw",self.evidence().profile(),root.self(),foe.profile(),root.opponent(),Critical.BOTH);
                check(damage.points().known()&&damage.points().value().equals(new IntRange(54,254)),"independent natural damage extrema differ");
            }
            var audit=PublicReplyCoverage.evaluate(snapshot,mechanics,profiles,execution,protocol,"p2a",field.trickRoom(),Critical.BOTH);
            check(audit.compared()&&audit.allCandidates()&&audit.replyCount()==2,"natural pipeline coverage: "+audit.reasons());
            if(!consumed) {
                check(audit.result().preferredConditional()==null,"unrevealed item produced a tactical preference");continue;
            }
            check(audit.result().rankings().stream().allMatch(r->r.utility().known()),"natural pipeline remains unbounded: "+audit.result().rankings());
            check("move:shadowclaw".equals(audit.result().preferredConditional()),"natural attack/setup/switch comparison lacks strict preference");
            var plans=audit.result().rankings().stream().map(r->new ShadowMechanicsEvaluation.Plan(r.action(),List.of(
                    List.of(new ShadowMechanicsEvaluation.Turn(r.action(),"psychocut",null)),
                    List.of(new ShadowMechanicsEvaluation.Turn(r.action(),"bulkup",null))),"natural paired fixture")).toList();
            var comparison=ShadowMechanicsEvaluation.compare(snapshot,mechanics,profiles,plans,field.trickRoom(),Critical.BOTH,"move:swordsdance",execution);
            check(comparison.disagreement()&&"move:shadowclaw".equals(comparison.simpleAction()),"fixed setup and simple baselines were not paired");
            var setup=comparison.rankings().stream().filter(r->r.action().equals("move:swordsdance")).findFirst().orElseThrow();
            check(setup.lines().stream().allMatch(l->l.state().self().stages().get("atk")==6),"capped setup exceeded +6");
        }
        check(PublicSpeciesCatalog.verified("weavile",null)==null,"out-of-catalog species borrowed numeric receipt");
        check(PublicSpeciesCatalog.verified("mewtwo",new PublicMechanicsEvidence.Species(Map.of("hp",106,"atk",110,"def",91,"spa",154,"spd",90,"spe",130),List.of("psychic"),null))==null,"changed public datapack stats borrowed canonical bounds");
        ledger.endChoice();
        System.out.println("Natural acceptance provider PASS: real learnsets, public item-removal gate, Pressure identity, observed hazards/stages, three-action/two-reply bounds and fixed baselines");
    }
}
