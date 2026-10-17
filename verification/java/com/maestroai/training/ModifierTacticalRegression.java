package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.JsonParser;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Held-out, source-oracle comparisons through fresh production providers.
 * Legal ordinary actions only; not end-turn residual, match wins or promotion. */
final class ModifierTacticalRegression {
    private static int checks;
    private static final UUID OWN=new UUID(0,11), BENCH=new UUID(0,12);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private record Fixture(OwnRequestTransport ledger,OwnRequestTransport.Lease lease,PublicProtocolEvidence protocol,
                           PublicFieldEvidence.Capture field,PublicMechanicsEvidence.Observation foe,
                           Map<String,Profile> profiles,ScopedExecution execution,BattleSnapshot snapshot,PublicMechanics mechanics) {}
    private static OwnRequestTransport.Pokemon pokemon(UUID id,boolean active,int hp,int max,boolean marshadow,String item,String ability) {
        var types=marshadow?List.of("fighting","ghost"):List.of("normal");
        var stats=marshadow?Map.of("atk",286,"def",196,"spa",216,"spd",216,"spe",286)
                :Map.of("atk",220,"def",372,"spa",276,"spd",276,"spe",220);
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Fixture",hp+"/"+max,active,ability,types),
                100,stats,item,types,"",false);
    }
    private static BattleSnapshot.Combatant facts(String species,OwnRequestTransport.Effective value,String item,String ability) {
        var e=value.evidence();return new BattleSnapshot.Combatant(species,e.hp().value().low(),e.profile().types().value(),
                value.stages(),List.of(),item,ability,false);
    }
    private static Fixture fixture(RuntimeDomain domain,int hp,boolean marshadow,String item,String ability,boolean consumed,List<String> legal) {
        Object b=new Object(),a=new Object(),token=new Object();var ledger=new OwnRequestTransport();ledger.owner(b,a,"p1");
        int max=marshadow?321:381;
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(
                pokemon(OWN,true,hp,max,marshadow,item,ability),pokemon(BENCH,false,321,321,true,"marshadiumz","technician")));
        ledger.dispatched(b,a,"p1",token,()->request);ledger.turnDispatched(b,1);ledger.turnFinished(b,1);
        token=new Object();ledger.dispatched(b,a,"p1",token,()->request);ledger.turnDispatched(b,2);
        var lease=ledger.beginChoice(b,a,token,OWN,false,()->true);
        var protocol=new PublicProtocolEvidence();
        protocol.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: "+OWN+"|"+(marshadow?"Marshadow":"Arceus")+", L100|"+((hp*100+max-1)/max)+"/100\n"
                +"|switch|p2a: Public|Mewtwo, L100|100/100\n|-ability|p2a: Public|Pressure\n|turn|1\n"
                +(consumed?"|-enditem|p2a: Public|Sitrus Berry\n":"")
                +"|move|p2a: Public|Swift|p1a: "+OWN+"\n|turn|2"));
        var provider=new PublicMechanicsEvidence(id->id.equals("mewtwo")?new PublicMechanicsEvidence.Species(
                Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null):null);
        provider.update(protocol);var field=new PublicFieldEvidence().update(protocol,provider);
        var seen=provider.observation("p1a");var foe=provider.observation("p2a");
        var own=ledger.effective(lease,"self",OWN,false,domain,protocol,seen,field);
        var reserve=ledger.effective(lease,"reserve",BENCH,true,domain,protocol,seen,field);
        check(own.missing().isEmpty()&&reserve.missing().isEmpty(),"fresh own effective provider failed");
        var target=provider.bind("opponent",foe,new PublicMechanicsEvidence.Scope(domain.effectiveState(),field.trickRoom()));
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(seen,own.evidence().item(),own.evidence().ability(),own.evidence().fresh(),lease),
                "reserve",new ScopedExecution.Binding(null,reserve.evidence().item(),reserve.evidence().ability(),reserve.evidence().fresh(),lease),
                "opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability())),domain.scopes());
        var ownFacts=facts(marshadow?"Marshadow":"Arceus",own,item,ability);
        var bench=facts("Marshadow",reserve,"marshadiumz","technician");
        var observer=new PublicBattleObserver();protocol.mechanicsEvents().forEach(observer::accept);
        var snapshot=new BattleSnapshot(2,ownFacts,PublicShadowSnapshot.opponent(observer,"p2a"),List.of(bench),List.of(),legal,List.of("reserve"),Map.of(),false,null);
        var hazards=field.hazards("p1",domain.effectiveState());
        var entry=execution.entry(hazards,Known.of(321,lease.source()),reserve.evidence().profile());
        check(entry.damage().known()&&entry.damage().value().high()==0,"known crystal/Technician reserve entry failed");
        var context=new PublicTransitionState.Context(own.evidence().hp(),foe.hp(),own.evidence().status(),foe.status(),hazards,
                protocol.sideHazards("p2"),Map.of("reserve",new SwitchTarget(bench,reserve.evidence().status(),entry.damage(),entry.speedDelta())),Map.of(),Set.of("APPEARANCE_ONLY"));
        var mechanics=new PublicMechanics(Map.of(),field.stages(foe,domain.effectiveState()),Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
        return new Fixture(ledger,lease,protocol,field,foe,Map.of("self",own.evidence().profile(),"reserve",reserve.evidence().profile(),"opponent",target.profile()),execution,snapshot,mechanics);
    }
    private static ShadowMechanicsEvaluation.Comparison compare(Fixture f,String legacy) {
        var actions=new ArrayList<String>();f.snapshot.legalMoves().forEach(m->actions.add("move:"+m));actions.add("switch:reserve");
        var plans=actions.stream().map(action->new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(
                new ShadowMechanicsEvaluation.Turn(action,"swift",null))),"independent fixed public Swift reply")).toList();
        return ShadowMechanicsEvaluation.compare(f.snapshot,f.mechanics,f.profiles,plans,f.field.trickRoom(),Critical.BOTH,legacy,f.execution);
    }
    static IntRange oracle(Context engine) {
        var oracle=JsonParser.parseString(engine.eval("js","""
            (()=>{
              const b=new (require('./sim/battle').Battle)({formatid:'gen9customgame',seed:[7,11,13,17]});
              const set=(species,ability,moves,item='')=>({species,ability,moves,item,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
              b.setPlayer('p1',{team:[set('Marshadow','technician',['shadowsneak'],'marshadiumz')]});
              b.setPlayer('p2',{team:[set('Mewtwo','pressure',['swift'])]});b.makeChoices('team 1','team 1');
              const a=b.p1.active[0],t=b.p2.active[0],values=[];a.storedStats.atk=286;
              for(const def of [166,306])for(const critical of [false,true])for(let roll=0;roll<16;roll++){
                t.storedStats.def=def;b.random=()=>roll;const move=b.dex.getActiveMove('shadowsneak');move.willCrit=critical;
                values.push(b.actions.getDamage(a,t,move,true));
              }
              return JSON.stringify([Math.min(...values),Math.max(...values)]);
            })()
            """).asString()).getAsJsonArray();
        return new IntRange(oracle.get(0).getAsInt(),oracle.get(1).getAsInt());
    }
    static void invalidation(RuntimeDomain domain,Runnable change) {
        var f=fixture(domain,200,false,"leftovers","multitype",true,List.of("recover","swordsdance"));
        var once=new java.util.concurrent.atomic.AtomicBoolean();
        Map<String,Profile> profiles=new AbstractMap<>() {
            @Override public Set<Entry<String,Profile>> entrySet(){return f.profiles.entrySet();}
            @Override public Profile get(Object key){if(once.compareAndSet(false,true))change.run();return f.profiles.get(key);}
        };
        var plans=List.of("move:recover","move:swordsdance","switch:reserve").stream()
                .map(action->new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(
                        new ShadowMechanicsEvaluation.Turn(action,"swift",null))),"controlled closing-receipt holdout")).toList();
        var result=ShadowMechanicsEvaluation.compare(f.snapshot,f.mechanics,profiles,plans,f.field.trickRoom(),Critical.BOTH,"move:recover",f.execution);
        check(once.get() && result.preferredConditional()==null && result.rankings().stream().allMatch(r->
                !r.utility().known() && r.unknown().contains("SOURCE_CHANGED_DURING_COMPARISON")
                && r.lines().stream().allMatch(l->!l.utility().known() && l.state().self().hp().value().equals(new Interval(200.0/381,200.0/381)))),
                "closing receipt retained a source-dependent forecast");
        System.out.println("Closing configuration mutation regression PASS: all calculated states/utilities withdrawn");
    }
    static void run(RuntimeDomain domain,IntRange oracle) throws Exception {
        checks=0;
        // All three ordinary Arceus actions remain legal in the request; Extreme
        // Speed can be exhausted/disabled. No production trainer/team edit exists.
        var dying=fixture(domain,1,false,"leftovers","multitype",true,List.of("shadowclaw","swordsdance","recover"));
        String legacy=LegacyScorerRegression.choose(1,381,220,416,416,394,100,
                List.of("shadowclaw","swordsdance","recover"));
        check(legacy.equals("move:shadowclaw"),"compiled legacy move scorer baseline changed");
        var switched=compare(dying,legacy);
        check(switched.rankings().stream().allMatch(r->r.utility().known()),"fixed-pack switch outcomes unknown: "+switched.rankings());
        check("switch:reserve".equals(switched.preferredConditional()),"normal immunity must dominate three slower lethal stay actions");
        check(switched.disagreement()&&"move:shadowclaw".equals(switched.simpleAction()),"switch comparison lost supplied legacy/nominal baselines");
        var healing=fixture(domain,200,false,"leftovers","multitype",true,List.of("swordsdance","recover"));
        String recoveryLegacy=LegacyScorerRegression.choose(200,381,220,416,416,394,100,List.of("swordsdance","recover"));
        check(recoveryLegacy.equals("move:swordsdance"),"compiled legacy recovery/setup ranking changed");
        var recovered=compare(healing,recoveryLegacy);
        check("move:recover".equals(recovered.preferredConditional()),"bounded Recover must dominate setup and idle switch in this exchange: "+recovered.rankings());
        var setup=recovered.rankings().stream().filter(r->r.action().equals("move:swordsdance")).findFirst().orElseThrow();
        check(setup.lines().stream().allMatch(l->l.state().self().stages().get("atk")==2),"setup must alter source-supported stages");
        var recoveredLine=recovered.rankings().stream().filter(r->r.action().equals("move:recover")).findFirst().orElseThrow().lines().getFirst();
        var setupLine=setup.lines().getFirst();
        check(Math.abs(recoveredLine.state().self().hp().value().low()-Math.min(1,setupLine.state().self().hp().value().low()+191.0/381))<1e-12,
                "odd-HP Recover disagrees with independently source-executed 191-point healing");
        var tech=fixture(domain,321,true,"marshadiumz","technician",true,List.of("shadowsneak"));
        var state=PublicStateSearchAdapter.project(tech.snapshot,tech.mechanics).states().get("root");
        var actual=tech.execution.damage("shadowsneak",tech.profiles.get("self"),state.self(),tech.profiles.get("opponent"),state.opponent(),Critical.BOTH);
        check(actual.points().known()&&actual.points().value().equals(oracle),"Technician base-power arithmetic differs from native oracle: "+actual+" / "+oracle);
        var missing=fixture(domain,200,false,"leftovers","multitype",false,List.of("swordsdance","recover"));
        check(compare(missing,"move:recover").preferredConditional()==null,"unseen opponent item certified via missing announcement");
        for(String ability:List.of("shadowshield","asonespectrier","unknown")) {
            var unsupported=fixture(domain,200,false,"leftovers",ability,true,List.of("recover"));
            check(compare(unsupported,"move:recover").preferredConditional()==null,"unverified ability certified: "+ability);
        }
        var life=fixture(domain,200,false,"lifeorb","multitype",true,List.of("recover"));
        check(compare(life,"move:recover").preferredConditional()==null,"unsupported item recoil was ignored");
        tech.ledger.endChoice();
        check(!tech.execution.damageCoverage("shadowsneak",tech.profiles.get("self"),state.self(),tech.profiles.get("opponent"),state.opponent()).supported(),"expired own transport retained Technician certificate");
        System.out.println("Fixed-pack modifier/tactical proof PASS: "+checks+" checks; source Technician extrema, useful switch/recover/setup states, compiled legacy move-scorer and simple baselines; no win-rate claim");
    }
}
