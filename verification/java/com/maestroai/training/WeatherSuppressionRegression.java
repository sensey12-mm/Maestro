package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Public active participation, effective weather and conservative transitions. */
final class WeatherSuppressionRegression {
    private static final String SOURCE="declared native weather suppression conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case number(Case c,String k,int n){var d=c.data().deepCopy();d.addProperty(k,n);return new Case(d);}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Actor target(Fixture f){return f.config().b("enemy")?f.state().self():f.state().opponent();}
    private static Known<Integer> accuracy(Fixture f){var c=f.config();return f.execution().accuracy(c.s("move"),f.profiles().get(c.user()),user(f),f.profiles().get(c.target()),target(f));}
    private static Known<IntRange> speed(Fixture f){return f.execution().speed(f.profiles().get(f.config().user()),user(f));}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean residual,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (residual?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean residual){return rank(f,residual,f.execution());}
    private static Case base(String suppressor,boolean enemy){
        var c=change(change(change(change(shield(),"weather","sandstorm"),"move","rocktomb"),"ability","sandrush"),"targetAbility",suppressor);return enemy?enemy(c):c;
    }
    static void run(){
        for(String suppressor:List.of("airlock","cloudnine"))for(boolean enemy:List.of(false,true)){
            var c=base(suppressor,enemy);var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&rank(f,true).utility().known(),"Supported suppression stayed UNKNOWN");
            check(f.execution().field().weather().value().equals("sandstorm")&&f.execution().effectiveWeather(f.state().self(),f.state().opponent()).value().equals("none"),"Suppression cleared raw weather");
            check(speed(f).value().equals(IntRange.exact(300)),"Suppression failed to cancel Sand Rush");
            for(String weather:List.of("sandstorm","sunnyday","raindance")){
                var wet=fixture(change(change(c,"weather",weather),"move","fireblast"),"");var dry=fixture(change(change(c,"weather","none"),"move","fireblast"),"");
                check(damage(wet,Critical.BOTH).points().value().equals(damage(dry,Critical.BOTH).points().value()),"Suppression left fire weather modifiers active");
                var thunder=number(change(change(c,"weather",weather),"move","thunder"),"accuracyStage",-2);
                check(accuracy(fixture(thunder,"")).value()==42,"Suppressed Thunder used sun/rain accuracy hook");
            }
            var outgoing=change(change(c,"ability",suppressor),"targetAbility","sandveil");
            check(accuracy(fixture(outgoing,"")).value()==95,"Suppression failed to cancel Sand Veil");
            var rain=change(change(c,"move","thunder"),"weather","raindance");
            for(String missing:List.of("accuracy-stage","evasion-stage")){
                check(!accuracy(fixture(rain,missing)).known(),"Suppressed rain retained Thunder's stage exemption");
                check(accuracy(fixture(change(c,"move","swift"),missing)).value()==100,"Suppression removed Swift's exemption");
            }
            check(speed(fixture(c,"damage-scope")).known(),"Sand Rush weather required an unrelated damage receipt");
            check(damage(fixture(c,"speed-scope"),Critical.BOTH).points().known(),"Damage required an unrelated speed receipt");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "status","types","target-max","attack-stat","level","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!rank(fixture(c,missing),true).utility().known(),"Suppression bypassed evidence: "+missing);
            var unknown=fixture(c,"",known(new Interval(0,.5)));var uncertain=target(unknown);
            check(!unknown.execution().effectiveWeather(user(unknown),uncertain).known()&&!damage(unknown,Critical.BOTH).points().known(),"Unproved participation canceled weather");
            var zero=new Actor(uncertain.id(),uncertain.facts(),known(new Interval(0,0)),uncertain.stages(),uncertain.status());
            check(!unknown.execution().effectiveWeather(user(unknown),zero).known(),"HP zero invented native faint dispatch");
            var self=enemy?uncertain:user(unknown);var foe=enemy?user(unknown):uncertain;
            check(unknown.execution().order("move:"+c.action(),c.reply(),unknown.profiles().get("self"),self,unknown.profiles().get("opponent"),foe)==Order.UNKNOWN,"Order ignored projected suppressor participation");
            var expired=fixture(c,"");var cert=coverage(expired);check(cert.supported(),"Fresh weather receipt missing");expired.owner().endChoice();
            check(!cert.supported()&&!speed(expired).known()&&!rank(expired,true).utility().known(),"Expired own proof retained suppression");
            var source=fixture(c,"");source.current().set(false);
            check(!damage(source,Critical.BOTH).points().known()&&!rank(source,true).utility().known(),"Expired source retained suppression");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{rank(field,true);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field retained suppression");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,true,frame);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing weather receipt leaked states");
            var upkeep=change(c,"weatherMode","upkeep");
            check(rank(fixture(upkeep,""),false).utility().known()&&!rank(fixture(upkeep,""),true).utility().known(),"Suppression inferred weather lifetime");
            for(String weather:List.of("desolateland","primordialsea"))check(!rank(fixture(change(c,"weather",weather),""),true).utility().known(),"Suppression certified unsupported weather");
            var bench=change(number(change(c,"targetAbility","runaway"),"ownBench",1),"ownBenchAbility",suppressor);var reserve=fixture(bench,"");
            check(reserve.execution().effectiveWeather(reserve.state().self(),reserve.state().opponent()).value().equals("sandstorm"),"Inactive own reserve suppressed weather");
            check(reserve.execution().entryCoverage(reserve.state().selfHazards(),known(1000),reserve.profiles().get("reserve0")).supported()&&!reserve.execution().profile(reserve.profiles().get("reserve0"),true).ordinaryEffects().known(),"Entry arithmetic incorrectly certified active suppression");
        }
        System.out.println("Weather suppression evidence regressions PASS: "+checks+" checks; participation, modifier composition, duration, inactive reserves and expired receipts");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/weather_suppression_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var damage=data.getAsJsonArray("damage");check(damage.size()==288,"Damage inventory changed");
        for(var value:damage){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(Critical critical:Critical.values()){
                var values=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var r=v.getAsJsonObject();if(critical==Critical.BOTH||r.get("crit").getAsBoolean()==(critical==Critical.CRITICAL))values.add(r.get("damage").getAsInt());}
                var actual=damage(f,critical);check(actual.points().known()&&actual.points().value().equals(new IntRange(Collections.min(values),Collections.max(values))),"Native suppressed damage differs: "+c.data()+" / "+critical+" / "+actual);
            }
        }
        var thresholds=data.getAsJsonArray("accuracy");check(thresholds.size()==360,"Accuracy inventory changed");
        for(var value:thresholds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=accuracy(fixture(c,""));check(actual.known()&&actual.value()==row.get("threshold").getAsInt(),"Native suppressed accuracy differs: "+c.data()+" / "+actual);}
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==300,"Speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native suppressed speed differs: "+c.data()+" / "+actual);}
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==356,"Exchange inventory changed");int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(boolean end:List.of(false,true)){
                var expected=row.getAsJsonObject(end?"post":"pre");var actual=rank(f,end);
                boolean alive=expected.get("foeHp").getAsDouble()>0&&(expected.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
                boolean statusBoundary=end||Set.of("none","par").contains(c.s("status"))&&Set.of("none","par").contains(c.s("targetStatus"));
                boolean supported=alive&&statusBoundary&&!(end&&(c.s("weatherMode").equals("upkeep")||c.s("id").equals("pivot-out")||expected.get("selfHp").getAsDouble()==0));
                if(c.s("id").equals("pivot-out")){if(c.b("enemy")){unknown++;check(!actual.utility().known(),"Hidden opponent suppression pivot gained scope");}else {bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native suppression departure excluded");}continue;}
                if(end&&c.s("id").equals("recoil-ko")&&!c.b("enemy")&&c.n("ownBench")>0&&expected.get("foeHp").getAsDouble()>0)supported=true;
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native suppression exchange excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved suppressor survival/party/duration gained scope: "+c.data());}
                if(c.s("id").equals("recoil-ko")){String key=end+":"+c.b("enemy")+":"+c.s("ability")+":"+c.n("ownBench");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party changed suppression");}
            }
        }
        System.out.println("Weather suppression native differential PASS: 288 damage configurations / 9216 critical-roll outcomes, 360 accuracy thresholds, 300 speed configurations, 356 queued exchanges at two boundaries; "+bounded+" bounded, "+unknown+" UNKNOWN; active roles, expiry and hidden-party isolation");
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
