package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Pinned native cold-weather arithmetic, queued outcomes and evidence boundaries. */
final class ColdWeatherRegression {
    private static final String SOURCE="declared native cold weather conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case number(Case c,String k,int n){var d=c.data().deepCopy();d.addProperty(k,n);return new Case(d);}
    private static Case flag(Case c,String k,boolean b){var d=c.data().deepCopy();d.addProperty(k,b);return new Case(d);}
    private static Case base(String weather,String ability,boolean enemy){
        var c=number(number(change(change(change(change(shield(),"move","tackle"),"ability",ability),"targetAbility","runaway"),"weather",weather),"userSpeed",301),"targetSpeed",201);
        c=change(c,"item","none");return enemy?enemy(c):c;
    }
    private static Known<IntRange> speed(Fixture f){var a=f.config().b("enemy")?f.state().opponent():f.state().self();return f.execution().speed(f.profiles().get(a.id()),a);}
    private static Known<Integer> accuracy(Fixture f){var a=f.config().b("enemy")?f.state().opponent():f.state().self();var t=f.config().b("enemy")?f.state().self():f.state().opponent();return f.execution().accuracy(f.config().s("move"),f.profiles().get(a.id()),a,f.profiles().get(t.id()),t);}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end){return rank(f,end,f.execution());}
    private static PublicFieldEvidence.Capture append(Fixture f,String event){
        f.history().add("update\n"+event);f.protocol().update(f.history());f.provider().update(f.protocol());return f.fields().update(f.protocol(),f.provider());
    }
    static void run(){
        for(String weather:List.of("snow","hail"))for(boolean enemy:List.of(false,true)){
            var c=base(weather,"icebody",enemy);var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&rank(f,true).utility().known(),"Cold weather stayed UNKNOWN");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "status","target-max","types","attack-stat","level","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!rank(fixture(c,missing),true).utility().known(),"Cold weather bypassed evidence: "+missing);
            var slush=fixture(change(c,"ability","slushrush"),"");check(speed(slush).value().equals(IntRange.exact(602)),"Slush Rush did not double in both cold weathers");
            check(speed(fixture(change(slush.config(),"weather","none"),"")).value().equals(IntRange.exact(301)),"Slush Rush boosted dry speed");
            check(speed(fixture(change(c,"ability","sandrush"),"")).value().equals(IntRange.exact(301)),"Sand Rush boosted cold-weather speed");
            for(String missing:List.of("speed-stat","speed-scope"))check(!speed(fixture(slush.config(),missing)).known(),"Cold speed bypassed "+missing);
            var cloak=change(change(c,"move","rocktomb"),"targetAbility","snowcloak");
            check(accuracy(fixture(cloak,"")).value()==76,"Snow Cloak threshold differs");
            check(accuracy(fixture(change(cloak,"move","swift"),"")).value()==100,"Snow Cloak altered always-hit accuracy");
            for(String missing:List.of("accuracy-stage","evasion-stage"))check(!accuracy(fixture(cloak,missing)).known(),"Cold accuracy bypassed stages");
            var outgoing=change(change(cloak,"ability","snowcloak"),"targetAbility","runaway");
            check(accuracy(fixture(outgoing,"")).value()==95,"Outgoing Snow Cloak changed accuracy");
            for(String suppressor:List.of("airlock","cloudnine")){
                var suppressed=fixture(change(c,"targetAbility",suppressor),"");
                check(damage(suppressed,Critical.BOTH).points().value().equals(damage(fixture(change(suppressed.config(),"weather","none"),""),Critical.BOTH).points().value()),"Suppressor retained cold defensive modifier");
                check(speed(fixture(change(slush.config(),"targetAbility",suppressor),"")).value().equals(IntRange.exact(301)),"Suppressed Slush Rush boosted speed");
                check(accuracy(fixture(change(cloak,"ability",suppressor),"")).value()==95,"Suppressed Snow Cloak retained evasion");
                var uncertain=fixture(change(c,"targetAbility",suppressor),"",known(new Interval(0,.5)));
                check(!damage(uncertain,Critical.BOTH).points().known()&&!rank(uncertain,true).utility().known(),"Unproved suppressor participation gained scope");
            }
            for(String unsupported:List.of("desolateland","primordialsea","deltastream"))check(!rank(fixture(change(c,"weather",unsupported),""),true).utility().known(),"Extreme weather gained cold scope");
            for(String status:List.of("tox","slp","frz"))check(!rank(fixture(change(c,"status",status),""),true).utility().known(),"Unproved counter/recovery gained cold scope");
            var upkeep=fixture(change(c,"weatherMode","upkeep"),"");
            check(rank(upkeep,false).utility().known()&&!rank(upkeep,true).utility().known(),"Current cold weather certified residual duration");
            for(int generation:List.of(8,7))check(!damage(fixture(number(c,"generation",generation),""),Critical.BOTH).points().known(),"Gen9 witness accepted a different format");
            String setter=weather.equals("snow")?"Snowscape":"Hail";
            var witnessed=change(flag(c,"witnessWeather",true),"witnessWeatherMove",setter);
            check(rank(fixture(witnessed,""),true).utility().known(),"Past cold setter lost source-bound scope");
            check(!rank(fixture(witnessed,"indirect-setter"),true).utility().known(),"Indirect cold setter gained scope");
            check(CobblemonMechanics.move(weather.equals("snow")?"snowscape":"hail")==null,"Past setter became a selectable calculator action");
            var source=fixture(c,"");var cert=source.execution().residualCoverage(source.profiles().get("self"),source.state().self());source.current().set(false);
            check(!cert.supported()&&!rank(source,true).utility().known(),"Expired source retained cold receipt");
            var expired=fixture(c,"");var old=coverage(expired);expired.owner().endChoice();
            check(!old.supported()&&!rank(expired,true).utility().known(),"Expired owner retained cold scope");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,true);}catch(IllegalArgumentException rejected){stale=true;}check(stale,"Expired field retained cold forecast");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,true,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing cold receipt leaked projected states");
            String action="move:"+c.action();var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            check(!ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(future),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst().utility().known(),"Cold weather inferred future request");
            var clock=fixture(c,"");var receipt=coverage(clock);
            for(int t=3;t<=6;t++)check(append(clock,"|-weather|"+weather+"|[upkeep]\n|turn|"+t).weatherAtResidual().known()==(t<=5),"Cold weather upkeep reset minimum duration");
            check(!append(clock,"|-weather|"+weather).weatherAtResidual().known(),"Repeated cold weather reset clock");
            check(!append(clock,"|-weather|none").weatherAtResidual().value(),"Ended cold weather retained residual");
            check(append(clock,"|-weather|"+weather).weatherAtResidual().value(),"Cold weather restart lost clock");
            boolean rejected=false;try{receipt.supported();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Restart revived old cold receipt");
        }
        System.out.println("Snow / Hail evidence regressions PASS: "+checks+" checks; generation, duration, public actor roles, UNKNOWN and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/cold_weather_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var damages=data.getAsJsonArray("damage");check(damages.size()==332,"Damage inventory changed");
        int damageBounded=0,damageUnknown=0;
        for(var value:damages){var row=value.getAsJsonObject();var f=fixture(new Case(row.getAsJsonObject("config")),"");
            boolean outside=false;for(var v:row.getAsJsonArray("values"))outside|=v.getAsJsonObject().get("damage").getAsInt()>10000;
            if(outside){damageUnknown++;for(Critical crit:Critical.values()){var actual=damage(f,crit);check(!actual.points().known()&&actual.unknown().contains("DAMAGE_OVERFLOW_OUTSIDE_SLICE"),"Existing damage ceiling gained cold-weather scope");}continue;}
            damageBounded++;
            for(Critical crit:Critical.values()){var values=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var d=v.getAsJsonObject();if(crit==Critical.BOTH||d.get("crit").getAsBoolean()==(crit==Critical.CRITICAL))values.add(d.get("damage").getAsInt());}
                var actual=damage(f,crit);check(actual.points().known()&&actual.points().value().equals(new IntRange(Collections.min(values),Collections.max(values))),"Native cold-weather damage differs: "+f.config().data()+" / "+crit+" / "+actual);}
        }
        check(damageBounded==304&&damageUnknown==28,"Cold damage scope inventory changed");
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==624,"Speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();
            check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native Slush Rush speed differs: "+c.data()+" native="+low+".."+high+" actual="+actual);}
        var accuracies=data.getAsJsonArray("accuracy");check(accuracies.size()==192,"Accuracy inventory changed");
        for(var value:accuracies){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=accuracy(fixture(c,""));check(actual.known()&&actual.value()==row.get("threshold").getAsInt(),"Native Snow Cloak threshold differs: "+c.data()+" / "+row+" / "+actual);}
        int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==708,"Exchange inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");
            for(boolean end:List.of(false,true)){var actual=rank(f,end);var expected=row.getAsJsonObject(end?"post":"pre");
                boolean alive=expected.get("foeHp").getAsDouble()>0&&(expected.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
                boolean supported=alive&&(end||!Set.of("brn","psn").contains(c.s("status")))&&!(c.s("id").equals("pivot-out")&&c.b("enemy"))&&!(end&&c.s("weatherMode").equals("upkeep"));
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native cold-weather exchange excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved cold continuation/duration gained scope: "+c.data()+" / end="+end);}
                if(Set.of("weather-ko","both-weather-ko").contains(c.s("id"))){String key=end+":"+c.b("enemy")+":"+c.s("id")+":"+c.n("userHp")+":"+c.n("ownBench")+":"+c.s("weather");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party changed cold forecast");}
            }
        }
        var durations=data.getAsJsonArray("durations");check(durations.size()==6,"Clock inventory changed");
        for(var value:durations){var row=value.getAsJsonObject();int duration=row.get("duration").getAsInt();var phases=row.getAsJsonArray("phases");
            for(int i=0;i<duration;i++){var phase=phases.get(i).getAsJsonObject();check(phase.get("weather").getAsString().equals(i==duration-1?"none":row.get("weather").getAsString()),"Native cold expiration order differs");
                check(phase.get("hp").getAsInt()==Math.min(1000,500+62*Math.min(i+1,duration-1)),"Expired cold weather healed Ice Body");}
        }
        var generations=data.getAsJsonArray("generations");check(generations.size()==2,"Generation inventory changed");
        for(var value:generations){var row=value.getAsJsonObject();check(row.get("snowWarning").getAsString().equals(row.get("gen").getAsInt()==8?"hail":"snow"),"Native generation boundary differs");}
        WeatherEntryRegression.coldDifferential(engine);
        System.out.println("Snow / Hail native differential PASS: 332 damage configurations / 10624 critical-roll outcomes (304 bounded, 28 beyond the existing damage ceiling UNKNOWN); 624 speed configurations; 192 accuracy thresholds; 708 queued exchanges at two boundaries; "+bounded+" bounded, "+unknown+" UNKNOWN; six weather clocks and Gen8/Gen9 Snow Warning controls");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line l,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(!l.utility().known()||utility<l.utility().value().low()-1e-10||utility>l.utility().value().high()+1e-10)return false;
        if(!l.state().self().id().equals(row.get("active").getAsString()))return false;
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
