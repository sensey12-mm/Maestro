package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Source-bound sun/rain speed, native callback partitions and public participation. */
final class WeatherSpeedRegression {
    private static final String SOURCE="declared native sun/rain speed conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static String weather(String ability){return ability.equals("chlorophyll")?"sunnyday":"raindance";}
    private static Case number(Case c,String key,int n){var d=c.data().deepCopy();d.addProperty(key,n);return new Case(d);}
    private static Case flag(Case c,String key,boolean b){var d=c.data().deepCopy();d.addProperty(key,b);return new Case(d);}
    private static Case base(String ability,boolean enemy){
        var c=number(number(change(change(change(change(shield(),"ability",ability),"targetAbility","runaway"),"weather",weather(ability)),"move","rocktomb"),"userSpeed",101),"targetSpeed",201);
        return enemy?enemy(c):c;
    }
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Known<IntRange> speed(Fixture f){return f.execution().speed(f.profiles().get(f.config().user()),user(f));}
    private static Order order(Fixture f){return f.execution().order("move:"+f.config().action(),f.config().reply(),f.profiles().get("self"),f.state().self(),f.profiles().get("opponent"),f.state().opponent());}
    private static ShadowMechanicsEvaluation.Ranking evaluated(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    static void run(){
        for(String ability:List.of("chlorophyll","swiftswim"))for(boolean enemy:List.of(false,true)){
            var c=base(ability,enemy);var f=fixture(c,"");
            check(speed(f).value().equals(IntRange.exact(202))&&evaluate(f).utility().known(),"Supported weather speed stayed UNKNOWN");
            var dry=fixture(change(c,"weather","none"),"");check(speed(dry).value().equals(IntRange.exact(101)),"Weather ability boosted outside its weather");
            var odd=number(change(flag(c,"wind",true),"item","choicescarf"),"userSpeed",3);
            check(speed(fixture(odd,"")).value().equals(IntRange.exact(18)),"Weather, Scarf and Tailwind chain differs");
            check(speed(fixture(change(odd,"status","par"),"")).known(),"Paralysis callback partition lost coverage");
            check(speed(fixture(c,"damage-scope")).known(),"Speed required unrelated damage scope");
            check(evaluate(fixture(change(c,"move","bulkup"),"damage-scope")).utility().known(),"Setup weather ordering required damage scope");
            check(evaluate(fixture(change(c,"move","quickattack"),"speed-scope")).utility().known(),"Priority required irrelevant speed scope");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","speed-scope","speed-stat","status","suppression","ambiguous","forged-opponent","gravity","magnet-rise"))
                check(!speed(fixture(c,missing)).known()&&!evaluate(fixture(c,missing)).utility().known(),"Weather speed bypassed evidence: "+missing);
            for(String missing:List.of("order-scope","damage-scope"))check(!evaluate(fixture(c,missing)).utility().known(),"Weather speed replaced action proof: "+missing);
            for(String suppressor:List.of("airlock","cloudnine")){
                var suppressed=change(c,"targetAbility",suppressor);check(speed(fixture(suppressed,"")).value().equals(IntRange.exact(101)),"Suppression retained weather speed");
                var uncertain=fixture(suppressed,"",known(new Interval(0,.5)));
                check(order(uncertain)==Order.UNKNOWN&&!evaluate(uncertain).utility().known(),"Unproved suppressor participation supplied ordering");
                var reserve=fixture(change(number(c,"ownBench",1),"ownBenchAbility",suppressor),"");
                check(speed(reserve).value().equals(IntRange.exact(202)),"Inactive reserve suppressed weather speed");
            }
            for(String mode:List.of("ended","restart","changed","upkeep")){
                var changed=change(change(change(c,"weatherMode",mode),"restartWeather",weather(ability)),"weatherNext",ability.equals("chlorophyll")?"raindance":"sunnyday");
                int expected=Set.of("ended","changed").contains(mode)?101:202;
                check(speed(fixture(changed,"")).value().equals(IntRange.exact(expected)),"Current weather transition differs: "+mode);
            }
            for(String setter:List.of("sunnyday","raindance"))check(!evaluate(fixture(change(c,"move",setter),"")).utility().known(),"Unmodeled weather action gained selectable scope");
            String action="move:"+c.action();var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);
            var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            check(!ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(future),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst().utility().known(),"Current weather inferred a later request/duration");
            for(String status:List.of("slp","frz","brn","psn","tox")){
                var unresolved=fixture(change(c,"status",status),"");check(speed(unresolved).known()&&!evaluate(unresolved).utility().known(),"Speed arithmetic certified unresolved status execution");
            }
            for(String weather:List.of("deltastream","desolateland","primordialsea"))check(!speed(fixture(change(c,"weather",weather),"")).known(),"Unsupported weather gained speed scope");
            for(String item:List.of("heatrock","damprock"))check(!speed(fixture(change(c,"item",item),"")).known(),"Unsupported weather item gained scope");
            check(!speed(fixture(change(c,"targetAbility","neutralizinggas"),"")).known(),"Unsupported ability suppression gained scope");
            var expired=fixture(c,"");var receipt=expired.execution().speedCoverage(expired.profiles().get(c.user()),user(expired));check(receipt.supported(),"Fresh speed receipt unavailable");expired.owner().endChoice();
            check(!receipt.supported()&&!speed(expired).known()&&!evaluate(expired).utility().known(),"Expired own proof retained weather speed");
            var source=fixture(c,"");source.current().set(false);check(!speed(source).known()&&!evaluate(source).utility().known(),"Expired runtime proof retained weather speed");
            var field=fixture(c,"");var old=field.execution().speedCoverage(field.profiles().get(c.user()),user(field));
            for(String next:List.of("none",weather(ability))){field.history().add("update\n|-weather|"+next);field.protocol().update(field.history());field.provider().update(field.protocol());field.fields().update(field.protocol(),field.provider());
                boolean stale=false;try{stale=!old.supported();}catch(IllegalArgumentException expiredField){stale=true;}check(stale,"Restored weather revived old receipt");}
            boolean rejected=false;try{speed(field);}catch(IllegalArgumentException stale){rejected=true;}check(rejected,"Stale field supplied speed");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.SPEED)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=evaluated(closing,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing speed receipt leaked projected states");
            check(!evaluate(fixture(c,"",known(new Interval(0,.5)))).utility().known(),"Unproved survival gained a forecast");
        }
        System.out.println("Sun/rain speed evidence regressions PASS: "+checks+" checks; weather transitions, priority, suppression, public roles and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/weather_speed_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==852,"Speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();
            check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native sun/rain speed differs: "+c.data()+" native="+low+".."+high+" actual="+actual);
        }
        var boundaries=data.getAsJsonArray("boundaries");check(boundaries.size()==24,"Boundary inventory changed");
        for(var value:boundaries){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));
            check(c.s("item").equals("utilityumbrella")?actual.known()&&actual.value().equals(new IntRange(row.get("low").getAsInt(),row.get("high").getAsInt())):!actual.known(),"Native weather/item/suppression boundary differs");}
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==304,"Exchange inventory changed");int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");var actual=evaluate(f);
            boolean alive=row.get("foeHp").getAsDouble()>0&&(row.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
            if(alive&&!c.s("id").equals("order-wrap")){
                bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,row)),"Native weather-speed exchange excluded: "+c.data()+" / "+row+" / "+actual);
                var order=order(f);if(order!=Order.BOTH)check(order!=Order.UNKNOWN&&row.getAsJsonArray("order").get(0).getAsString().equals(order==Order.SELF_FIRST?"self":"opponent"),"Native weather turn order differs: "+c.data());
            }else{unknown++;check(!actual.utility().known(),"Unproved survival, party or speed wrap gained scope: "+c.data());}
            if(c.s("id").equals("recoil-ko")){String key=c.b("enemy")+":"+c.s("ability")+":"+c.n("ownBench")+":"+c.b("nativeTie");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party changed weather-speed forecast");}
        }
        System.out.println("Sun/rain speed native differential PASS: 852 speed configurations, 24 weather/item controls, 304 queued exchanges; "+bounded+" bounded, "+unknown+" UNKNOWN; both roles, callback rounding, weather/suppression, priority, Trick Room and hidden-party isolation");
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
