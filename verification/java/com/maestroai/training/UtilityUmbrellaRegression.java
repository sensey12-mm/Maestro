package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Public item ownership, per-actor weather, suppression and conditional boundaries. */
final class UtilityUmbrellaRegression {
    private static final String SOURCE="declared native umbrella conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case number(Case c,String k,int n){var d=c.data().deepCopy();d.addProperty(k,n);return new Case(d);}
    private static Case flag(Case c,String k,boolean b){var d=c.data().deepCopy();d.addProperty(k,b);return new Case(d);}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Actor target(Fixture f){return f.config().b("enemy")?f.state().self():f.state().opponent();}
    private static Known<IntRange> speed(Fixture f){return f.execution().speed(f.profiles().get(f.config().user()),user(f));}
    private static Known<Integer> accuracy(Fixture f){var c=f.config();return f.execution().accuracy(c.s("move"),f.profiles().get(c.user()),user(f),f.profiles().get(c.target()),target(f));}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end){return rank(f,end,f.execution());}
    private static Case base(String weather,boolean enemy){
        var c=number(change(change(change(change(shield(),"move","fireblast"),"targetItem","utilityumbrella"),"weather",weather),"ability",weather.equals("sunnyday")?"chlorophyll":"swiftswim"),"userSpeed",101);
        return enemy?enemy(c):c;
    }
    static void run(){
        for(String weather:List.of("sunnyday","raindance"))for(boolean enemy:List.of(false,true)){
            var c=base(weather,enemy);var f=fixture(c,"");var dry=fixture(change(c,"weather","none"),"");
            check(damage(f,Critical.BOTH).points().value().equals(damage(dry,Critical.BOTH).points().value()),"Defender umbrella retained sun/rain fire modifier");
            check(f.execution().effectiveWeather(f.state().self(),f.state().opponent()).value().equals(weather),"Umbrella cleared global weather");
            var own=change(change(c,"item","utilityumbrella"),"targetItem","none");var outgoing=fixture(own,"");
            check(!damage(outgoing,Critical.BOTH).points().value().equals(damage(fixture(change(own,"weather","none"),""),Critical.BOTH).points().value()),"Attacker umbrella canceled target weather damage");
            check(speed(outgoing).value().equals(IntRange.exact(101))&&speed(f).value().equals(IntRange.exact(202)),"Umbrella applied to the wrong speed actor");
            check(speed(fixture(flag(own,"room",true),"")).value().equals(IntRange.exact(202)),"Magic Room did not disable umbrella speed protection");
            var thunder=number(change(c,"move","thunder"),"accuracyStage",-2);
            check(accuracy(fixture(thunder,"")).value()==42,"Target umbrella did not restore ordinary Thunder accuracy");
            var noTarget=change(thunder,"targetItem","none");int expected=weather.equals("raindance")?100:30;
            check(accuracy(fixture(change(noTarget,"item","utilityumbrella"),"")).value()==expected,"Attacker umbrella changed Thunder's target weather");
            for(String missing:List.of("accuracy-stage","evasion-stage")){
                check(!accuracy(fixture(thunder,missing)).known(),"Umbrella bypassed numeric accuracy evidence");
                if(weather.equals("raindance"))check(accuracy(fixture(flag(thunder,"room",true),missing)).value()==100,"Suppressed umbrella removed rain's always-hit exemption");
            }
            for(String suppressor:List.of("airlock","cloudnine")){
                var suppressed=change(c,"ability",suppressor);check(damage(fixture(suppressed,""),Critical.BOTH).points().value().equals(damage(fixture(change(suppressed,"weather","none"),""),Critical.BOTH).points().value()),"Umbrella bypassed global weather suppression");
                var uncertain=fixture(change(c,"targetAbility",suppressor),"",known(new Interval(0,.5)));
                check(!damage(uncertain,Critical.BOTH).points().known()&&!rank(uncertain,true).utility().known(),"Umbrella proved uncertain suppressor participation");
            }
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "status","target-max","types","attack-stat","level","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!rank(fixture(c,missing),true).utility().known(),"Umbrella bypassed evidence: "+missing);
            check(speed(fixture(own,"damage-scope")).known(),"Umbrella speed required damage scope");
            check(damage(fixture(c,"speed-scope"),Critical.BOTH).points().known(),"Umbrella damage required speed scope");
            var reserve=fixture(change(number(change(c,"targetItem","none"),"ownBench",1),"ownBenchItem","utilityumbrella"),"");
            check(speed(reserve).value().equals(IntRange.exact(202)),"Inactive reserve umbrella filtered active weather");
            for(String unsupported:List.of("desolateland","primordialsea","deltastream"))check(!rank(fixture(change(c,"weather",unsupported),""),true).utility().known(),"Umbrella certified unsupported weather");
            for(String item:List.of("heatrock","damprock"))check(!damage(fixture(change(c,"targetItem",item),""),Critical.BOTH).points().known(),"Umbrella certified a duration item");
            var source=fixture(c,"");var cert=coverage(source);source.current().set(false);check(!cert.supported()&&!rank(source,true).utility().known(),"Expired source retained umbrella forecast");
            var expired=fixture(c,"");var receipt=coverage(expired);expired.owner().endChoice();check(!receipt.supported()&&!rank(expired,true).utility().known(),"Expired own request retained umbrella scope");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,true);}catch(IllegalArgumentException rejected){stale=true;}check(stale,"Expired field retained umbrella weather");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,true,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing umbrella receipt leaked states");
            String action="move:"+c.action();var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            check(!ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(future),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst().utility().known(),"Umbrella inferred future weather/request");
        }
        System.out.println("Utility Umbrella evidence regressions PASS: "+checks+" checks; holder roles, damage/accuracy/speed, suppression, privacy and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/utility_umbrella_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var damages=data.getAsJsonArray("damage");check(damages.size()==312,"Damage inventory changed");
        for(var value:damages){var row=value.getAsJsonObject();var f=fixture(new Case(row.getAsJsonObject("config")),"");
            for(Critical crit:Critical.values()){var values=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var d=v.getAsJsonObject();if(crit==Critical.BOTH||d.get("crit").getAsBoolean()==(crit==Critical.CRITICAL))values.add(d.get("damage").getAsInt());}
                var actual=damage(f,crit);check(actual.points().known()&&actual.points().value().equals(new IntRange(Collections.min(values),Collections.max(values))),"Native umbrella damage differs: "+f.config().data()+" / "+crit+" / "+actual);}
        }
        var accuracies=data.getAsJsonArray("accuracy");check(accuracies.size()==372,"Accuracy inventory changed");
        for(var value:accuracies){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=accuracy(fixture(c,""));check(actual.known()&&actual.value()==row.get("threshold").getAsInt(),"Native umbrella accuracy differs: "+c.data()+" / "+actual);}
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==372,"Speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native umbrella speed differs: "+c.data()+" native="+low+".."+high+" / "+actual);}
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==438,"Exchange inventory changed");int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");
            for(boolean end:List.of(false,true)){var actual=rank(f,end);var expected=row.getAsJsonObject(end?"post":"pre");
                boolean alive=expected.get("foeHp").getAsDouble()>0&&(expected.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
                boolean supported=alive&&!(end&&c.s("id").equals("sandstorm-upkeep"))&&!c.s("id").equals("pivot-in")&&!(c.s("id").equals("pivot-out")&&c.b("enemy"));
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native umbrella exchange excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved umbrella continuation/entry/duration gained scope: "+c.data());}
                if(c.s("id").equals("recoil-ko")||c.s("id").equals("sandstorm")){String key=end+":"+c.b("enemy")+":"+c.s("id")+":"+c.s("weather")+":"+c.n("userHp")+":"+c.n("ownBench");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden party affected umbrella forecast");}
            }
        }
        System.out.println("Utility Umbrella native differential PASS: 312 damage configurations / 9984 critical-roll outcomes, 372 accuracy thresholds, 372 speed configurations, 438 queued exchanges at two boundaries; "+bounded+" bounded, "+unknown+" UNKNOWN; holder roles, weather/suppression, room expiry, continuation and reserve boundaries");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line l,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(!l.utility().known()||utility<l.utility().value().low()-1e-10||utility>l.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var a=role.equals("self")?l.state().self():l.state().opponent();double hp=row.get(role+"Hp").getAsDouble();if(hp<a.hp().value().low()-1e-10||hp>a.hp().value().high()+1e-10||!a.status().value().equals(row.get(role+"Status").getAsString()))return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(a.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString()).option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
