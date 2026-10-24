package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Native weather ability arithmetic, residual sequencing and evidence withdrawal. */
final class WeatherAbilityRegression {
    private static final String SOURCE="declared native weather ability conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case number(Case c,String k,int n){var d=c.data().deepCopy();d.addProperty(k,n);return new Case(d);}
    private static Case flag(Case c,String k,boolean b){var d=c.data().deepCopy();d.addProperty(k,b);return new Case(d);}
    private static Case base(String ability,boolean enemy){
        var c=number(number(change(change(change(change(shield(),"move","psyshock"),"ability",ability),"targetAbility","runaway"),"weather",ability.equals("raindish")?"raindance":"sunnyday"),"userSpeed",301),"targetSpeed",201);
        return enemy?enemy(c):c;
    }
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
        for(String ability:List.of("raindish","solarpower"))for(boolean enemy:List.of(false,true)){
            var c=base(ability,enemy);var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&rank(f,true).utility().known(),"Supported weather ability remains UNKNOWN");
            var dry=fixture(change(c,"weather","none"),"");
            check(ability.equals("solarpower")?!damage(f,Critical.BOTH).points().value().equals(damage(dry,Critical.BOTH).points().value()):damage(f,Critical.BOTH).points().value().equals(damage(dry,Critical.BOTH).points().value()),"Weather offensive stat modifier differs");
            var umbrella=fixture(change(c,"item","utilityumbrella"),"");var ordinary=fixture(change(change(c,"item","utilityumbrella"),"weather","none"),"");
            check(damage(umbrella,Critical.BOTH).points().value().equals(damage(ordinary,Critical.BOTH).points().value()),"Holder umbrella retained weather SpA");
            var physical=change(c,"move","tackle");check(damage(fixture(physical,""),Critical.BOTH).points().value().equals(damage(fixture(change(physical,"weather","none"),""),Critical.BOTH).points().value()),"Solar Power affected physical damage");
            var numeric=damage(fixture(number(c,"attack",3),""),Critical.BOTH);check(numeric.points().known(),"Odd stat arithmetic lost native scope");
            var upkeep=fixture(change(c,"weatherMode","upkeep"),"");
            check(damage(upkeep,Critical.BOTH).points().known()&&rank(upkeep,false).utility().known()&&!rank(upkeep,true).utility().known(),"Current action and unknown residual weather lifetime conflated");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "status","target-max","types","attack-stat","level","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!rank(fixture(c,missing),true).utility().known(),"Weather ability bypassed evidence: "+missing);
            for(String suppressor:List.of("airlock","cloudnine")){
                var suppressed=fixture(change(c,"targetAbility",suppressor),"");check(damage(suppressed,Critical.BOTH).points().value().equals(damage(dry,Critical.BOTH).points().value()),"Weather suppressor retained Solar Power modifier");
                var uncertain=fixture(change(c,"targetAbility",suppressor),"",known(new Interval(0,.5)));
                check(!damage(uncertain,Critical.BOTH).points().known()&&!rank(uncertain,true).utility().known(),"Unproved suppression participation gained scope");
            }
            for(String unsupported:List.of("desolateland","primordialsea","deltastream"))check(!rank(fixture(change(c,"weather",unsupported),""),true).utility().known(),"Unsupported weather gained ability scope");
            var source=fixture(c,"");var cert=source.execution().residualCoverage(source.profiles().get("self"),source.state().self());source.current().set(false);
            check(!cert.supported()&&!rank(source,true).utility().known(),"Expired source retained weather ability receipt");
            var expired=fixture(c,"");var old=expired.execution().residualCoverage(expired.profiles().get("self"),expired.state().self());expired.owner().endChoice();
            check(!old.supported()&&!rank(expired,true).utility().known(),"Expired owning request retained residual scope");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,true);}catch(IllegalArgumentException rejected){stale=true;}check(stale,"Expired weather capture retained forecast");
            for(boolean expire:List.of(false,true)) {
                var during=fixture(c,"");var queries=new java.util.concurrent.atomic.AtomicInteger();
                var fading=during.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{
                    if(op==ExecutionCoverage.Operation.DAMAGE&&queries.incrementAndGet()==2){
                        if(expire)during.current().set(false);
                        else return Known.unknown();
                    }
                    return known(true);
                },during.current()::get));
                var fadingActor=during.config().b("enemy")?during.state().opponent():during.state().self();
                var fadingTarget=during.config().b("enemy")?during.state().self():during.state().opponent();
                check(!fading.damage(c.s("move"),during.profiles().get(fadingActor.id()),fadingActor,during.profiles().get(fadingTarget.id()),fadingTarget,Critical.BOTH).fraction().known(),"Receipt/scope withdrawing during direct damage did not fail closed");
            }
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,true,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt leaked projected weather states");
            String action="move:"+c.action();var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            check(!ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(future),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst().utility().known(),"Weather ability inferred future request/duration");
            var setup=change(change(change(c,"move","bulkup"),"item","leftovers"),"targetItem","none");
            var near=fixture(number(number(setup,"userHp",125),"ownBench",1),"");
            var after=rank(near,true);
            if(ability.equals("solarpower"))check(enemy?!after.utility().known():after.utility().known()&&after.lines().stream().allMatch(l->l.state().self().hp().value().equals(new Interval(0,0))&&l.state().forcedReplacement()),"Leftovers rescued Solar Power KO or party closure was assumed");
            else {
                var poisoned=rank(fixture(change(number(setup,"userHp",125),"status","psn"),""),true);
                check(poisoned.utility().known()&&poisoned.lines().stream().allMatch(l->(enemy?l.state().opponent():l.state().self()).hp().value().equals(new Interval(.124,.124))),"Rain Dish / Leftovers / poison order differs");
            }
            var survival=rank(fixture(number(setup,"userHp",126),""),true);
            check(survival.utility().known(),"Known weather survival boundary lost scope");
            if(ability.equals("solarpower"))check(survival.lines().stream().allMatch(l->(enemy?l.state().opponent():l.state().self()).hp().value().equals(new Interval(.063,.063))),"Solar Power cost did not precede Leftovers");
            var uncertainTarget=change(change(change(setup,"ability","runaway"),"targetAbility","solarpower"),"weather","sunnyday");
            var unproved=fixture(number(uncertainTarget,"ownBench",1),"",known(new Interval(.124,.126)));
            check(!rank(unproved,true).utility().known(),"Uncertain weather KO survival gained scope");
            var clock=fixture(c,"");var receipt=coverage(clock);String weather=c.s("weather");
            for(int t=3;t<=6;t++)check(append(clock,"|-weather|"+weather+"|[upkeep]\n|turn|"+t).weatherAtResidual().known()==(t<=5),"Sun/rain minimum duration reset by upkeep");
            check(!append(clock,"|-weather|"+weather).weatherAtResidual().known(),"Repeated active sun/rain reset clock");
            check(!append(clock,"|-weather|none").weatherAtResidual().value(),"Ended weather retained ability residual");
            check(append(clock,"|-weather|"+weather).weatherAtResidual().value(),"Fresh restart lost minimum duration");
            boolean rejected=false;try{receipt.supported();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Weather lifecycle revived old receipt");
        }
        System.out.println("Rain Dish / Solar Power evidence regressions PASS: "+checks+" checks; actor effects, duration, suppression, privacy and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/weather_ability_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var damages=data.getAsJsonArray("damage");check(damages.size()==308,"Damage inventory changed");
        for(var value:damages){var row=value.getAsJsonObject();var f=fixture(new Case(row.getAsJsonObject("config")),"");
            for(Critical crit:Critical.values()){var values=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var d=v.getAsJsonObject();if(crit==Critical.BOTH||d.get("crit").getAsBoolean()==(crit==Critical.CRITICAL))values.add(d.get("damage").getAsInt());}
                var actual=damage(f,crit);check(actual.points().known()&&actual.points().value().equals(new IntRange(Collections.min(values),Collections.max(values))),"Native weather ability damage differs: "+f.config().data()+" / "+crit+" / "+actual);}
        }
        int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==432,"Exchange inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");
            for(boolean end:List.of(false,true)){var actual=rank(f,end);var expected=row.getAsJsonObject(end?"post":"pre");
                boolean alive=expected.get("foeHp").getAsDouble()>0&&(expected.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
                boolean supported=alive&&(end||!Set.of("brn","psn").contains(c.s("status")))&&!(c.s("id").equals("pivot-out")&&c.b("enemy"))&&!(end&&Set.of("upkeep-expiry","upkeep-ongoing").contains(c.s("id")));
                // An own faint with a certified living reserve proves native
                // continuation and restores Solar Power for residuals. Fainting
                // on the opponent's side never proves its hidden party closure.
                if(c.s("id").equals("suppression-faint"))supported=!end||c.b("enemy")&&c.n("ownBench")>0;
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native weather ability exchange excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved weather continuation/duration gained scope: "+c.data());}
                if(Set.of("weather-ko","recoil-ko","both-weather-ko","suppression-faint").contains(c.s("id"))){String key=end+":"+c.b("enemy")+":"+c.s("id")+":"+c.n("userHp")+":"+c.n("ownBench");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party affected weather ability forecast");}
            }
        }
        var durations=data.getAsJsonArray("durations");check(durations.size()==6,"Native duration inventory changed");
        for(var v:durations){var row=v.getAsJsonObject();var phases=row.getAsJsonArray("phases");int duration=row.get("duration").getAsInt();
            for(int i=0;i<duration;i++){
                var phase=phases.get(i).getAsJsonObject();boolean sun=row.get("weather").getAsString().equals("sunnyday");
                check(phase.get("weather").getAsString().equals(i==duration-1?"none":row.get("weather").getAsString()),"Native expiration did not precede Weather callbacks");
                check(phase.get("hp").getAsInt()==(sun?1000-125*Math.min(i+1,duration-1):500+62*Math.min(i+1,duration-1)),"Expired weather executed ability callback");
            }
        }
        System.out.println("Rain Dish / Solar Power native differential PASS: 308 damage configurations / 9856 critical-roll outcomes; 432 queued exchanges at two boundaries; "+bounded+" bounded, "+unknown+" UNKNOWN; six native weather clocks; rounding, recovery caps, residual order and fainting");
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
