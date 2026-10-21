package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PivotOutcomeRegression.*;

/** Entry/status proofs use owner receipts; native opponent parties stay labels. */
final class ToxicEntryRegression {
    private static final String SOURCE="declared native Toxic Spikes entry conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case change(Case c,String k,Object v){var j=c.data().deepCopy();if(v instanceof String a)j.addProperty(k,a);else if(v instanceof Integer a)j.addProperty(k,a);else if(v instanceof Boolean a)j.addProperty(k,a);else j.add(k,(JsonElement)v);return new Case(j);}
    private static Case base(int layers){var c=ordinary();c.data().getAsJsonObject("hazards").addProperty("toxicspikes",layers);return c;}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end,boolean switching,ScopedExecution execution){
        String action=switching?"switch:reserve0":"move:"+f.config().action();
        var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end,boolean switching){return rank(f,end,switching,f.execution());}
    static void run(){
        for(int layers:List.of(1,2))for(boolean switching:List.of(false,true)){
            var c=base(layers);var f=fixture(c,"");var result=rank(f,false,switching);
            check(result.utility().known()&&result.lines().stream().allMatch(l->l.state().self().status().value().equals(layers==1?"psn":"tox")),"Entry poisoning lost source support");
            check(rank(f,true,switching).utility().known()==(layers==1),"Unproved Toxic counter gained residual scope");
            var capped=rank(fixture(change(c,"reserveHp",1),""),true,switching);
            check(!capped.utility().known(),"Entered/fainted actor counted as its own remaining reserve");
            for(String missing:List.of("own-item","opponent-ability","entry-scope","reserve-binding","reserve-profile","reserve-target","foreign-reserve","foreign-profile","stale-reserve","reserve-types","reserve-max","reserve-hp","partial-hazards","entry-damage","entry-speed","altered-entry-damage","altered-entry-speed","no-choice","reserve-item","reserve-ability","reserve-status","gravity","ambiguous"))
            {
                var tested=new Case(c.data().deepCopy());
                if(missing.equals("altered-entry-damage"))tested.data().getAsJsonObject("hazards").addProperty("stealthrock",1);
                boolean rejected=false;
                try{rejected=!rank(fixture(tested,missing),false,switching).utility().known();}
                catch(IllegalArgumentException illegal){if(!missing.equals("no-choice")||!switching)throw illegal;rejected=true;}
                check(rejected,"Entry bypassed proof: "+missing);
            }
            var witnessed=change(c,"witnessToxic",true);
            check(rank(fixture(witnessed,""),false,switching).utility().known(),"Witnessed native Toxic Spikes setter poisoned entry coverage");
            check(!rank(fixture(witnessed,"indirect-setter"),false,switching).utility().known(),"Indirect hazard setter gained coverage");
            check(!CobblemonMechanics.supportedMoveIds().contains("toxicspikes"),"Entry dependency became a selectable calculator action");
            var source=fixture(c,"");source.current().set(false);check(!rank(source,false,switching).utility().known(),"Expired source retained poisoning");
            var own=fixture(c,"");own.transport().endChoice();check(!rank(own,false,switching).utility().known(),"Expired owner receipt retained entry status");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,false,switching);}catch(IllegalArgumentException e){stale=true;}check(stale,"Stale field retained hazard effect");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.ENTRY)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,false,switching,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().self().id().equals("self")),"Closing receipt leaked incoming status or absorption");
            var poison=change(c,"reserveTypes",JsonParser.parseString("[\"poison\"]"));var boots=rank(fixture(change(poison,"reserveItem","heavydutyboots"),""),false,switching);
            check(boots.utility().known()&&boots.lines().stream().allMatch(l->!l.state().selfHazards().layers().containsKey("toxicspikes")),"Boots prevented grounded Poison absorption");
            var guarded=rank(fixture(change(c,"reserveAbility","magicguard"),""),false,switching);
            check(guarded.utility().known()&&guarded.lines().stream().allMatch(l->!l.state().self().status().value().equals("none")),"Magic Guard prevented poisoning");
            var miss=fixture(c,"entry-scope");var p=miss.execution().entryEffects(new Hazards(c.hazards(),true,SOURCE),known(1000),miss.profiles().get("reserve0"),known("none"));
            check(!p.status().known()&&p.hazards().layers().equals(c.hazards()),"Unproved entry removed hazards");
        }
        System.out.println("Toxic Spikes evidence regressions PASS: "+checks+" checks; status, absorption, entry survival, ownership and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        var js=Files.readString(Path.of("verification/toxic_entry_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+js+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==904,"Native entry inventory changed");int entries=0,bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var v:rows){var row=v.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            if(!row.get("entry").isJsonNull()&&c.n("ownBench")>0){var e=row.getAsJsonObject("entry");var profile=f.profiles().get("reserve0");var hazards=new Hazards(c.hazards(),true,SOURCE);
                var cert=f.execution().entryCoverage(hazards,known(c.n("reserveMax")),profile);
                if(!c.s("reserveAbility").equals("download")&&e.get("hp").getAsInt()>0){entries++;check(cert.supported(),"Native entry lacks source coverage");
                    var arithmetic=f.execution().entry(hazards,known(c.n("reserveMax")),profile);double nativeLoss=(e.get("before").getAsDouble()-e.get("hp").getAsDouble())/e.get("max").getAsDouble();
                    check(arithmetic.damage().known()&&Math.abs(arithmetic.damage().value().low()-nativeLoss)<1e-10&&Math.abs(arithmetic.damage().value().high()-nativeLoss)<1e-10,"Native entry HP rounding differs: "+c.data());
                    check(arithmetic.speedDelta().known()&&arithmetic.speedDelta().value()==e.get("speed").getAsInt(),"Native Sticky Web delta differs");
                    var effects=f.execution().entryEffects(hazards,known(c.n("reserveMax")),profile,known(c.s("reserveStatus")));
                    check(effects.status().known()&&effects.status().value().equals(e.get("status").getAsString())&&effects.hazards().layers().equals(hazards(e.getAsJsonObject("hazards"))),"Native entry status/absorption differs: "+c.data()+" / "+effects+" / "+e);
                }else if(c.s("reserveAbility").equals("download"))check(!cert.supported(),"Unproved Download entry gained scope");
            }
            if(c.b("enemy")){
                if(c.s("mode").equals("pivot"))check(!rank(f,false,false).utility().known(),"Unknown opponent replacement was selected");
                continue;
            }
            boolean switching=c.s("mode").equals("switch");
            for(boolean end:List.of(false,true)){
                var actual=rank(f,end,switching);var expected=row.getAsJsonObject(end?"post":"pre");
                boolean alive=expected.get("foeHp").getAsDouble()>0&&expected.get("selfHp").getAsDouble()>0;
                boolean entered=!row.get("entry").isJsonNull()&&row.getAsJsonObject("entry").get("hp").getAsInt()>0;
                boolean supported=(switching||c.n("ownBench")==1)&&!c.s("reserveAbility").equals("download")&&alive&&entered
                    &&!(end&&Set.of("tox","slp","frz").contains(expected.get("selfStatus").getAsString()));
                if(c.s("id").equals("reserve-after-hit"))supported=true;
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)&&l.state().selfHazards().layers().equals(hazards(expected.getAsJsonObject("hazards")))),"Native composed entry excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved entry/continuation gained scope: "+c.data()+" / end="+end);}
                if(c.s("id").equals("party")){String key=switching+":"+end+":"+c.n("ownBench");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Private opponent reserves changed entry forecast");}
            }
        }
        System.out.println("Toxic Spikes native differential PASS: 904 queued entries; "+entries+" surviving owner-counterfactual entry checks across both native roles; "+bounded+" bounded comparisons, "+unknown+" UNKNOWN; all 24 hazard insertion orders, pivot/switch, status, immunity, suppression, weather and stale-party boundaries");
    }
    private static Map<String,Integer> hazards(JsonObject j){var h=new TreeMap<String,Integer>();j.entrySet().forEach(e->h.put(e.getKey(),e.getValue().getAsInt()));return h;}
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString()).option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));PivotOutcomeRegression.differential(engine);differential(engine);
        }
    }
}
