package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Public weather timing, native integer HP and current actor/source receipts. */
final class SandstormRegression {
    private static final String SOURCE="declared native sandstorm conditions";
    private static int checks;
    private static <T> Known<T> known(T value){return Known.of(value,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case storm(){return change(change(change(shield(),"weather","sandstorm"),"move","swift"),"targetAbility","runaway");}
    private static Case number(Case c,String key,int n){var d=c.data().deepCopy();d.addProperty(key,n);return new Case(d);}
    private static Case types(Case c,String key,String... types){var d=c.data().deepCopy();var a=new JsonArray();for(String t:types)a.add(t);d.add(key,a);return new Case(d);}
    private static ShadowMechanicsEvaluation.Ranking residual(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking residual(Fixture f){return residual(f,f.execution());}
    static void run(){
        for(boolean enemy:List.of(false,true)){
            Case c=types(enemy?enemy(storm()):storm(),"targetTypes","rock");var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&residual(f).utility().known(),"Proven sandstorm remains UNKNOWN");
            check(damage(f,Critical.BOTH).points().value().high()<damage(fixture(change(c,"weather","none"),""),Critical.BOTH).points().value().high(),"Rock SpD did not reduce special damage");
            var physical=change(c,"move","psyshock");
            check(damage(fixture(physical,""),Critical.BOTH).points().equals(damage(fixture(change(physical,"weather","none"),""),Critical.BOTH).points()),"Psyshock incorrectly used sandstorm SpD");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope",
                    "types","target-types","attacker-types","level","attack-stat","target-max","status","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!residual(fixture(c,missing)).utility().known(),"Sandstorm bypassed evidence: "+missing);
            check(residual(fixture(c,"entry-scope")).utility().known(),"Surviving storm required unrelated entry receipt");
            var uncertain=change(c,"weatherMode","upkeep");
            check(damage(fixture(uncertain,""),Critical.BOTH).points().known()&&!residual(fixture(uncertain,"")).utility().known(),"Current damage and unproved weather duration were conflated");
            check(residual(fixture(number(uncertain,"weatherDuration",1),"")).equals(residual(fixture(number(uncertain,"weatherDuration",8),""))),"Oracle counter leaked into forecast");
            var expired=fixture(c,"");var cert=coverage(expired);expired.owner().endChoice();
            check(!cert.supported()&&!residual(expired).utility().known(),"Expired owner retained sandstorm");
            var source=fixture(c,"");var sourceCert=coverage(source);source.current().set(false);
            check(!sourceCert.supported()&&!residual(source).utility().known(),"Expired source retained sandstorm");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{residual(field);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field retained sandstorm");
            var closing=fixture(c,"");var expiring=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=residual(closing,expiring);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt leaked weather states");
            var witnessed=c.data().deepCopy();witnessed.addProperty("witnessWeather",true);var past=new Case(witnessed);
            check(residual(fixture(past,"")).utility().known(),"Witnessed ordinary weather setter poisoned source coverage");
            check(!residual(fixture(past,"indirect-setter")).utility().known(),"Indirect weather setter became certified");
            for(String ability:List.of("sandforce","sandrush","sandveil","airlock","cloudnine","neutralizinggas"))
                check(!residual(fixture(change(c,"ability",ability),"")).utility().known(),"Unsupported weather ability gained scope: "+ability);
            for(String item:List.of("smoothrock","safetygoggles","utilityumbrella"))
                check(!residual(fixture(change(c,"item",item),"")).utility().known(),"Unsupported weather item gained scope: "+item);
        }
        var setup=change(change(storm(),"move","bulkup"),"item","leftovers");
        var doomed=number(setup,"userHp",1);
        check(!residual(fixture(doomed,"")).utility().known(),"Empty own reserves proved weather continuation");
        var continued=residual(fixture(number(doomed,"ownBench",1),""));
        check(continued.utility().known()&&continued.lines().stream().allMatch(l->l.state().self().hp().value().high()==0
                &&l.state().forcedReplacement()&&!l.state().nextRequestKnown()),"Weather KO revived via Leftovers or lost pending own replacement");
        var both=residual(fixture(number(number(doomed,"targetHp",1),"ownBench",1),""));
        check(!both.utility().known()&&both.lines().stream().allMatch(l->l.state().self().hp().value().high()==0&&l.state().opponent().hp().value().high()==0),"Weather failed to process both actives before faint/termination checks");
        check(!residual(fixture(number(doomed,"ownBench",1),"reserve-binding")).utility().known(),"Missing reserve binding proved weather continuation");
        check(!residual(fixture(setup,"",known(new Interval(.001,.1)))).utility().known(),"Unproved sandstorm survival became known");
        check(!CobblemonMechanics.supportedMoveIds().contains("sandstorm"),"Weather dependency became selectable");
        lifecycle(setup);
        System.out.println("Sandstorm evidence regressions PASS: "+checks+" checks; weather lifetime, suppression, HP/survival, public identity and receipt expiration");
    }
    private static PublicFieldEvidence.Capture append(Fixture f,String event){
        f.history().add("update\n"+event);f.protocol().update(f.history());f.provider().update(f.protocol());return f.fields().update(f.protocol(),f.provider());
    }
    private static void lifecycle(Case c){
        var f=fixture(c,"");var old=coverage(f);
        for(int turn=3;turn<=6;turn++){
            var field=append(f,"|-weather|Sandstorm|[upkeep]\n|turn|"+turn);
            check(field.sandstormAtResidual().known()==(turn<=5),"Upkeep refreshed weather or minimum duration counted incorrectly: "+turn);
        }
        check(!append(f,"|-weather|Sandstorm").sandstormAtResidual().known(),"Repeated active weather reset duration");
        var ended=append(f,"|-weather|none");check(ended.sandstormAtResidual().known()&&!ended.sandstormAtResidual().value(),"Ended weather retained chip scope");
        var restarted=append(f,"|-weather|Sandstorm");check(restarted.sandstormAtResidual().known()&&restarted.sandstormAtResidual().value(),"Fresh weather restart did not reset minimum duration");
        boolean rejected=false;try{old.supported();}catch(IllegalArgumentException stale){rejected=true;}check(rejected,"Old weather receipt survived public lifecycle changes");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/sandstorm_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var durations=data.getAsJsonArray("durations");check(durations.size()==8,"Native duration factory inventory changed");
        for(var value:durations){var row=value.getAsJsonObject();check(row.get("duration").getAsInt()==(row.get("item").getAsString().equals("smoothrock")&&!row.get("room").getAsBoolean()?8:5),"Native minimum duration or suppression changed");}
        var configs=data.getAsJsonArray("damage");check(configs.size()==96,"Native weather damage inventory changed");int rolls=0;
        for(var value:configs){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(Critical critical:Critical.values()){
                var outcomes=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var r=v.getAsJsonObject();if(critical==Critical.BOTH||r.get("crit").getAsBoolean()==(critical==Critical.CRITICAL))outcomes.add(r.get("damage").getAsInt());}
                var actual=damage(f,critical);int low=Collections.min(outcomes),high=Collections.max(outcomes);
                check(actual.points().known()&&actual.points().value().equals(new IntRange(low,high)),"Native weather stat/critical rounding differs: "+c.data()+" / "+critical+" / "+actual+" / "+outcomes);
            }rolls+=row.getAsJsonArray("values").size();
        }
        check(rolls==3072,"Native sandstorm roll inventory changed");
        var rows=data.getAsJsonArray("exchanges");check(rows.size()==212,"Native weather residual inventory changed");int bounded=0,unknown=0;
        var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:rows){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));
            var result=residual(fixture(c,c.data().has("protect")&&c.b("protect")?"protect":""));
            // The counter, termination and opponent bench are oracle assertions,
            // never source evidence. Unknown duration withdraws residual scoring.
            boolean supported=!c.s("weatherMode").equals("upkeep")&&row.get("foeHp").getAsDouble()>0
                    &&(row.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
            if(supported){bounded++;check(result.utility().known()&&result.lines().stream().anyMatch(l->matches(l,row)),"Native sandstorm residual excluded: "+c.data()+" / "+row+" / "+result);
                if(row.get("selfHp").getAsDouble()==0)check(result.lines().stream().allMatch(l->l.state().forcedReplacement()&&!l.state().nextRequestKnown()),"Native weather replacement became a future executable request");
            }else{unknown++;check(!result.utility().known(),"Unproved weather duration/party closure became known: "+c.data());}
            if(c.s("id").equals("weather-knockout")){
                String key=c.b("enemy")+":"+c.userHp()+":"+c.n("targetHp")+":"+c.n("ownBench");
                if(c.n("foeBench")==0)hidden.put(key,result);else check(result.equals(hidden.get(key)),"Hidden opponent reserves changed weather evaluation");
            }
        }
        System.out.println("Sandstorm native differential PASS: 8 duration factories, 96 damage configurations, 3072 critical/roll outcomes, 212 queued outcomes; "+bounded+" bounded, "+unknown+" UNKNOWN; both roles, expiry, immunity, recovery and fainting");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        if(!line.utility().known())return false;double utility=row.get("utility").getAsDouble();
        if(utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var a=role.equals("self")?line.state().self():line.state().opponent();double hp=row.get(role+"Hp").getAsDouble();
            if(hp<a.hp().value().low()-1e-10||hp>a.hp().value().high()+1e-10||!a.status().value().equals(row.get(role+"Status").getAsString()))return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(a.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
    public static void main(String[] args) throws Exception {
        run();
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false")
                .option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
