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
                :Map.of("atk",245,"def",372,"spa",276,"spd",276,"spe",220);
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
    static com.google.gson.JsonArray policyOracle(Context engine) throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("verification/policy_oracle.cjs"));
        return JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};const exports=module.exports;"+source
                +";return JSON.stringify(module.exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
    }
    static void policyComparisons(RuntimeDomain domain,com.google.gson.JsonArray oracle) throws Exception {
        int agreements=0,disagreements=0,abstentions=0,cases=0;
        for(int scenario=0;scenario<3;scenario++) {
            int hp=scenario==0?1:200;
            var actions=scenario==1?List.of("swordsdance","recover"):List.of("shadowclaw","swordsdance","recover");
            var features=oracle.get(0).getAsJsonObject().getAsJsonObject("features");
            var attack=features.getAsJsonObject("attack");
            var choices=LegacyPolicyRegression.moves(actions.toArray(String[]::new));
            for(var move:choices)if(move.damageMove){move.score=move.averageDamage=attack.get("average").getAsDouble();move.estimatedDamage=attack.get("minimum").getAsInt();}
            var mine=new LegacyPolicyRegression.Mon(11,"Arceus",hp,381,220,attack.get("average").getAsDouble(),features.getAsJsonObject("incoming").get("average").getAsDouble());
            var reserve=new LegacyPolicyRegression.Mon(12,"Marshadow",321,321,286,features.getAsJsonObject("reserveAttack").get("average").getAsDouble(),0);reserve.incomingEffectiveness=0;reserve.effectiveness=2;
            var foe=new LegacyPolicyRegression.Mon(13,"Mewtwo",416,416,394,0,0);
            String legacy=LegacyPolicyRegression.choose(mine,reserve,foe,choices,false,false,true,null,null);
            var result=compare(fixture(domain,hp,false,"leftovers","multitype",true,actions),legacy);
            check(result.candidateCoverageComplete(),"policy candidate coverage incomplete");
            for(var o:oracle)if(o.getAsJsonObject().get("hp").getAsInt()==hp) {
                var row=o.getAsJsonObject();String id=row.get("action").getAsString();
                if(!id.equals("switch")&&!actions.contains(id))continue;
                String action=id.equals("switch")?"switch:reserve":"move:"+id;
                var ranked=result.rankings().stream().filter(r->r.action().equals(action)).findFirst().orElseThrow();
                check(ranked.utility().known(),"source outcome certified UNKNOWN action");
                double utility=row.get("utility").getAsDouble();var bounds=ranked.utility().value();
                check(utility>=bounds.low()-1e-10&&utility<=bounds.high()+1e-10,"source outcome outside Shadow utility: "+action+" "+utility+" / "+bounds);
                cases++;
            }
            String preferred=result.preferredConditional();
            if(scenario==2) {
                check(preferred==null&&result.rankings().stream().allMatch(r->r.utility().known()),"overlapping attack/heal intervals fabricated a preference");
                abstentions++;System.out.println("Policy-heldout hp=200 all actions: bounded overlap abstention; legacy="+legacy+"; no claimed win");continue;
            }
            check(preferred!=null,"known policy heldout abstained");
            double best=sourceUtility(oracle,hp,preferred),old=sourceUtility(oracle,hp,legacy),simple=sourceUtility(oracle,hp,result.simpleAction());
            check(best>=old-1e-10&&best>=simple-1e-10,"conditional recommendation failed independent exchange/baseline labels");
            if(preferred.equals(legacy))agreements++;else {disagreements++;check(best>old+1e-10,"disagreement did not improve source resource exchange");}
            System.out.println("Policy-heldout hp="+hp+" actions="+actions+" shadow="+preferred+" legacy="+legacy+" simple="+result.simpleAction()
                    +" source utilities="+best+"/"+old+"/"+simple);
        }
        check(cases==11&&agreements==1&&disagreements==1&&abstentions==1,"fixed policy agreement/disagreement/abstention coverage changed");
        var unknown=compare(fixture(domain,200,false,"leftovers","multitype",false,List.of("shadowclaw","swordsdance","recover")),"move:recover");
        check(unknown.preferredConditional()==null,"missing public item evidence claimed policy win");
        System.out.println("Policy-level comparison PASS: 8 independent source outcomes, 3 complete candidate sets, 1 legacy agreement, 1 strict resource-exchange improvement, 1 nominal-power improvement and 1 agreement, 1 overlap abstention; unseen item abstains; not a win-rate/runtime claim");
    }
    private static double sourceUtility(com.google.gson.JsonArray rows,int hp,String action) {
        String id=action.equals("switch:reserve")?"switch":action.substring(5);
        for(var row:rows){var o=row.getAsJsonObject();if(o.get("hp").getAsInt()==hp&&o.get("action").getAsString().equals(id))return o.get("utility").getAsDouble();}
        throw new AssertionError("Unlabelled policy action: "+action);
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
    static void complexMoveRegressions(RuntimeDomain domain) {
        var fixture=fixture(domain,200,true,"marshadiumz","technician",true,
                List.of("closecombat","rocktomb","spectralthief","drainingkiss","uturn"));
        var outcome=compare(fixture,"move:closecombat");
        var ranks=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var r:outcome.rankings())ranks.put(r.action(),r);
        var close=ranks.get("move:closecombat");
        check(close.utility().known(),"Close Combat conditional source damage unknown "+close.unknown());
        check(close.lines().stream().allMatch(l->l.state().self().stages().get("def")==-1
                &&l.state().self().stages().get("spd")==-1),"Close Combat missed defense debuff");
        var rock=ranks.get("move:rocktomb");
        check(rock.utility().known()&&!rock.lines().isEmpty()
                &&rock.lines().stream().allMatch(line->line.state().opponent().stages().get("spe")==-1),
                "Rock Tomb must lower target Speed exactly once in every speed-order branch: "+rock);
        check(ranks.get("move:spectralthief").utility().known(),"unboosted public Spectral Thief unknown");
        var kiss=ranks.get("move:drainingkiss");
        check(kiss.utility().known(),"Draining Kiss HP recovery unknown");
        check(kiss.lines().stream().anyMatch(l->l.state().self().hp().value().high()
                        >close.lines().stream().mapToDouble(x->x.state().self().hp().value().high()).max().orElseThrow()),
                "source-bounded Draining Kiss healing did not exceed equivalent pre-reply own HP");
        var pivot=ranks.get("move:uturn");
        check(pivot.utility().known()&&!pivot.lines().isEmpty()
                &&pivot.lines().stream().allMatch(line->line.state().self().id().equals("reserve")),
                "source-verified sole-reserve U-turn did not redirect observed reply: "+pivot);
        for(String move:List.of("ironhead","icebeam","flamethrower","energyball",
                "moonblast","focusblast","thunder","fireblast")) {
            var effect=fixture(domain,200,true,"marshadiumz","technician",true,List.of(move));
            var ranking=compare(effect,"move:"+move).rankings().stream()
                    .filter(x->x.action().equals("move:"+move)).findFirst().orElseThrow();
            check(ranking.utility().known()&&ranking.lines().size()==2,
                    "secondary proc/nonproc outcomes not branched "+move+": "+ranking.unknown());
        }
        // This is an intentionally synthetic Power Herb/Geomancy execution
        // fixture, NOT a claim that the species used in fixture knows Geomancy.
        var herb=fixture(domain,200,false,"powerherb","multitype",true,List.of("geomancy"));
        var gm=compare(herb,"move:geomancy").rankings().stream()
                .filter(x->x.action().equals("move:geomancy")).findFirst().orElseThrow();
        check(gm.utility().known()&&gm.lines().stream().allMatch(l->
                l.state().self().stages().get("spa")==2
                &&l.state().self().stages().get("spd")==2
                &&l.state().self().stages().get("spe")==2),"Power Herb Geomancy did not boost");
        var charged=fixture(domain,200,false,"leftovers","multitype",true,List.of("geomancy"));
        check(compare(charged,"move:geomancy").rankings().stream()
                        .anyMatch(x->x.action().equals("move:geomancy")&&!x.utility().known()),
                "unconsumed charge was treated as free same-turn Geomancy");
        System.out.println("Maestro tactical effect regressions PASS: secondary forks, stat drops, healing, boost theft, Power Herb and sole-reserve pivot");
    }
    static void realTeamModifierRegressions(RuntimeDomain domain){
        // Fully public opponent modifier domain; synthetic move attribution
        // is not claimed to be a real Arceus/Marshadow learnset.
        var plain=fixture(domain,200,false,"leftovers","multitype",true,List.of("moonblast"));
        var aura=fixture(domain,200,false,"leftovers","fairyaura",true,List.of("moonblast"));
        var p=compare(plain,"move:moonblast").rankings().stream()
                .filter(x->x.action().equals("move:moonblast")).findFirst().orElseThrow();
        var a=compare(aura,"move:moonblast").rankings().stream()
                .filter(x->x.action().equals("move:moonblast")).findFirst().orElseThrow();
        check(p.utility().known()&&a.utility().known()&&a.utility().value().high()>p.utility().value().high(),
                "source Fairy Aura base-power modifier was lost");
        var holder=fixture(domain,200,true,"lifeorb","asonespectrier",true,List.of("astralbarrage"));
        var orb=compare(holder,"move:astralbarrage").rankings().stream()
                .filter(x->x.action().equals("move:astralbarrage")).findFirst().orElseThrow();
        check(orb.utility().known(),"source Life Orb + As One opponent-domain outcome unknown: "+orb.unknown());
        var nonOrb=fixture(domain,200,true,"marshadiumz","technician",true,List.of("astralbarrage"));
        var basic=compare(nonOrb,"move:astralbarrage").rankings().stream()
                .filter(x->x.action().equals("move:astralbarrage")).findFirst().orElseThrow();
        check(basic.utility().known()&&orb.utility().value().high()>basic.utility().value().high(),
                "Life Orb final-damage power failed to exceed otherwise ordinary damage");
        var full=fixture(domain,381,false,"leftovers","shadowshield",true,List.of("recover"));
        var normal=fixture(domain,381,false,"leftovers","multitype",true,List.of("recover"));
        var shield=compare(full,"move:recover").rankings().stream()
                .filter(x->x.action().equals("move:recover")).findFirst().orElseThrow();
        var noShield=compare(normal,"move:recover").rankings().stream()
                .filter(x->x.action().equals("move:recover")).findFirst().orElseThrow();
        check(shield.utility().known()&&noShield.utility().known()
                        &&shield.utility().value().low()>noShield.utility().value().low(),
                "source full-HP Shadow Shield damage reduction not applied");
        var onlyStone=fixture(domain,200,false,"mewtwonitey","shadowshield",true,List.of("recover"));
        check(compare(onlyStone,"move:recover").rankings().stream()
                        .anyMatch(x->x.action().equals("move:recover")&&x.utility().known()),
                "held Mega Stone/current Shadow Shield shouldn't independently poison status move scope");
        var protectedOrb=fixture(domain,200,true,"lifeorb","magicguard",true,List.of("astralbarrage"));
        var protectedRank=compare(protectedOrb,"move:astralbarrage").rankings().stream()
                .filter(x->x.action().equals("move:astralbarrage")).findFirst().orElseThrow();
        check(protectedRank.utility().known()
                        &&protectedRank.lines().stream().allMatch(x->x.state().self().hp().value().low()>=0),
                "Magic Guard on Life Orb user must protect from post-hit recoil");
        System.out.println("Maestro held-item/ability tactical modifier regression PASS: Fairy Aura, Life Orb/As One, Shadow Shield, Mega Stone, Magic Guard");
    }
    static void run(RuntimeDomain domain,IntRange oracle) throws Exception {
        complexMoveRegressions(domain);
        realTeamModifierRegressions(domain);
        checks=0;
        LegacyPolicyRegression.run();
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
        // Phase 3.52: the actual fourth-slot Maestro Arceus move kit is a
        // viable end-of-phase supported tactical target once the opponent
        // publicly witnesses Pressure, item removal and a Swift reply.
        // This remains a source-fixture, not a claim an in-game turn occurred.
        var actualArceus=fixture(domain,200,false,"leftovers","multitype",true,
                List.of("extremespeed","shadowclaw","swordsdance","recover"));
        var fullKit=compare(actualArceus,"move:shadowclaw");
        check(fullKit.candidateCoverageComplete() && fullKit.rankings().size()==5,
                "actual Arceus move/switch root lost legal candidate coverage");
        check(fullKit.rankings().stream().allMatch(r->r.utility().known()),
                "actual Arceus move kit remained unbounded against publicly witnessed Swift: "+fullKit.rankings());
        check(fullKit.rankings().stream().anyMatch(r->r.action().equals("move:extremespeed"))
                        && fullKit.rankings().stream().anyMatch(r->r.action().equals("move:recover"))
                        && fullKit.rankings().stream().anyMatch(r->r.action().equals("move:swordsdance")),
                "actual Arceus normal-priority/setup/recovery candidate missing");
        check(TacticalPairwiseCoverage.inspect(
                new PublicReplyCoverage.Audit(5,1,true,true,
                        fullKit.preferredConditional()!=null,
                        fullKit.rankings().stream().allMatch(r->r.utility().known())
                                ? (fullKit.preferredConditional()==null ? Set.of("INTERVALS_OVERLAP_OR_TIE") : Set.of())
                                : Set.of("OUTCOME_PREREQUISITES_UNKNOWN"),fullKit),
                Known.of(true,"explicit source fixture public Pressure and consumed item"),
                actualArceus.execution::sourceCurrent).comparedPairs()==10,
                "actual Arceus kit source-supported pairwise coverage incomplete");
        var staleScope=TacticalPairwiseCoverage.inspect(
                new PublicReplyCoverage.Audit(5,1,true,true,false,Set.of("INTERVALS_OVERLAP_OR_TIE"),fullKit),
                Known.unknown(),actualArceus.execution::sourceCurrent);
        check(staleScope.boundedActions()==0 && staleScope.comparedPairs()==0,
                "missing actual public opponent modifier-proof became full-kit preference");
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
        for(String ability:List.of("unknown")) {
            var unsupported=fixture(domain,200,false,"leftovers",ability,true,List.of("recover"));
            check(compare(unsupported,"move:recover").preferredConditional()==null,"unverified ability certified: "+ability);
        }
        // A Life Orb does not recoil for a status move such as Recover.
        var life=fixture(domain,200,false,"lifeorb","multitype",true,List.of("recover"));
        check(compare(life,"move:recover").rankings().stream()
                        .anyMatch(x->x.action().equals("move:recover")&&x.utility().known()),
                "Life Orb incorrectly blocked non-damaging recovery");
        tech.ledger.endChoice();
        check(!tech.execution.damageCoverage("shadowsneak",tech.profiles.get("self"),state.self(),tech.profiles.get("opponent"),state.opponent()).supported(),"expired own transport retained Technician certificate");
        System.out.println("Fixed-pack modifier/tactical proof PASS: "+checks+" checks; source Technician extrema, useful switch/recover/setup states, compiled legacy move-scorer and simple baselines; no win-rate claim");
    }
}
