package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PivotOutcomeRegression.*;

/** Synthetic owner-entry counterfactuals, never knowledge of opponent reserves. */
final class WeatherEntryRegression {
    private static final String SOURCE="declared native weather entry conditions";
    private static int checks;
    private static <T> Known<T> known(T value){return Known.of(value,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case change(Case c,String key,Object value){var d=c.data().deepCopy();if(value instanceof String s)d.addProperty(key,s);else if(value instanceof Integer n)d.addProperty(key,n);else if(value instanceof Boolean b)d.addProperty(key,b);else d.add(key,(JsonElement)value);return new Case(d);}
    private static Case base(String ability,boolean entering){return change(change(change(change(ordinary(),"weather","sandstorm"),"reply","tackle"),"userAbility",entering?"runaway":ability),"reserveAbility",entering?ability:"runaway");}
    private static State root(Fixture f){return project(f.snapshot(),f.mechanics()).states().get("root");}
    private static ShadowMechanicsEvaluation.Plan plan(String action,String reply){return new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,reply,null))),SOURCE);}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean switching,boolean end){
        String action=switching?"switch:reserve0":"move:"+f.config().action();var plans=List.of(plan(action,f.config().reply()));
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),plans,known(false),Critical.BOTH,action,f.execution())
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),plans,known(false),Critical.BOTH,action,f.execution())).rankings().getFirst();
    }
    static void run(){
        for(String ability:List.of("airlock","cloudnine"))for(boolean entering:List.of(false,true))for(boolean switching:List.of(false,true))for(String rawWeather:List.of("sandstorm","snow","hail")){
            var c=change(base(ability,entering),"weather",rawWeather);var f=fixture(c,"");var state=root(f);
            var before=f.execution().effectiveWeather(state.self(),state.opponent());
            check(before.known()&&before.value().equals(entering?rawWeather:"none"),"Inactive reserve changed root weather");
            var entry=f.execution().entryCoverage(state.selfHazards(),known(1000),f.profiles().get("reserve0"));
            check(entry.supported(),"Supported entry arithmetic stayed unknown");
            if(entering)check(!f.execution().profile(f.profiles().get("reserve0"),true).ordinaryEffects().known(),"Entry arithmetic certified an ordinary active suppressor");
            var actual=rank(f,switching,true);
            check(actual.utility().known()&&actual.lines().stream().allMatch(l->l.state().self().id().equals("reserve0")),"Certified weather entry/departure stayed UNKNOWN: "+actual);
            var after=actual.lines().getFirst().state();
            var frame=f.execution().conditionalParticipants("reserve0",false);var weather=frame.effectiveWeather(after.self(),after.opponent());
            check(weather.known()&&weather.value().equals(entering?"none":rawWeather),"Proven entry frame retained the departed ability");
            check(!frame.effectiveWeather(after.self(),state.self(),after.opponent()).known(),"Foreign active actor gained frame participation");
            check(!f.execution().effectiveWeather(after.self(),after.opponent()).known(),"Unentered raw reserve gained weather participation");
            check(f.execution().effectiveWeather(state.self(),state.opponent()).equals(before),"Conditional entry leaked into root weather");
            for(String missing:List.of("own-item","opponent-item","opponent-ability","entry-scope","reserve-binding","reserve-profile","reserve-target","foreign-reserve","foreign-profile","stale-reserve","reserve-types","reserve-max","reserve-hp","reserve-item","reserve-ability","reserve-status","partial-hazards","entry-damage","entry-speed","altered-entry-damage","altered-entry-speed","suppression","ambiguous","gravity","wonder-room")){
                var guarded=missing.equals("altered-entry-damage")||missing.equals("altered-entry-speed")?change(c,"hazards",JsonParser.parseString("{\"stealthrock\":1}")):c;
                check(!rank(fixture(guarded,missing),switching,true).utility().known(),"Weather entry bypassed evidence: "+missing);
            }
            check(rank(fixture(change(c,"ownBench",2),""),switching,false).utility().known()==switching,"Weather entry guessed an ambiguous pivot choice");
            for(int hp:List.of(125,126)){
                var hazard=change(change(change(c,"reply","bulkup"),"reserveHp",hp),"hazards",JsonParser.parseString("{\"stealthrock\":1}"));
                check(rank(fixture(hazard,""),switching,false).utility().known()==(hp==126),"Weather entry crossed lethal hazard boundary");
            }
            var age=fixture(change(c,"weatherMode","upkeep"),"");check(rank(age,switching,false).utility().known()&&!rank(age,switching,true).utility().known(),"Suppression proved weather duration");
            var source=fixture(c,"");var receipt=source.execution().entryCoverage(root(source).selfHazards(),known(1000),source.profiles().get("reserve0"));source.current().set(false);
            check(!receipt.supported()&&!rank(source,switching,true).utility().known(),"Expired source retained weather entry");
            var owner=fixture(c,"");owner.transport().endChoice();check(!rank(owner,switching,true).utility().known(),"Expired own entry receipt retained weather");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,switching,true);}catch(IllegalArgumentException expected){stale=true;}check(stale,"Expired public receipt retained weather");
            var closing=fixture(c,"");var armed=new java.util.concurrent.atomic.AtomicBoolean();var changed=new java.util.concurrent.atomic.AtomicBoolean();
            Map<String,Profile> expiring=new AbstractMap<>(){
                @Override public Set<Map.Entry<String,Profile>> entrySet(){return closing.profiles().entrySet();}
                @Override public Profile get(Object key){if(key.equals("reserve0"))armed.set(true);if(key.equals("opponent")&&armed.get()){changed.set(true);closing.current().set(false);}return closing.profiles().get(key);}
            };
            var fading=new Fixture(c,closing.snapshot(),closing.mechanics(),expiring,closing.execution(),closing.transport(),closing.current(),closing.fields());
            var withdrawn=rank(fading,switching,true);check(changed.get()&&!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().self().id().equals("self")),"Closing receipt leaked conditional weather state");
            // Candidate frames must be independent even when a blocked pivot
            // and a real switch are evaluated in the same comparison.
            var blocked=fixture(change(c,"protect",true),"");var plans=List.of(plan("move:uturn",c.reply()),plan("switch:reserve0",c.reply()));
            var first=ShadowMechanicsEvaluation.compareEndOfTurn(blocked.snapshot(),blocked.mechanics(),blocked.profiles(),plans,known(false),Critical.BOTH,"move:uturn",blocked.execution());
            var reversed=ShadowMechanicsEvaluation.compareEndOfTurn(blocked.snapshot(),blocked.mechanics(),blocked.profiles(),List.of(plans.getLast(),plans.getFirst()),known(false),Critical.BOTH,"move:uturn",blocked.execution());
            check(first.rankings().equals(reversed.rankings())&&first.rankings().stream().allMatch(r->r.utility().known()),"Candidate order leaked entry participation");
        }
        System.out.println("Weather entry evidence regressions PASS: "+checks+" checks; certified participation, branch isolation, hazards, missing evidence and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{differential(engine,"false");}
    static void coldDifferential(Context engine)throws Exception{differential(engine,"true");}
    static void setterDifferential(Context engine)throws Exception{differential(engine,"'setter'");}
    private static void differential(Context engine,String mode)throws Exception{
        boolean cold=mode.equals("true"),setter=mode.equals("'setter'");
        String source=Files.readString(Path.of("verification/weather_entry_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle,"+mode+"));})()").asString()).getAsJsonArray();
        check(rows.size()==(setter?2784:cold?432:1976),"Native weather entry inventory changed: "+rows.size());
        System.out.println((setter?"Weather setter native inventory captured: ":cold?"Cold weather entry native inventory captured: ":"Weather entry native inventory captured: ")+rows.size()+" queued cases");
        int bounded=0,unknown=0,probes=0,damageRolls=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:rows){var row=value.getAsJsonObject();var nativeCase=new Case(row.getAsJsonObject("config"));
            // Both native positions are DECLARED own-entry counterfactuals.
            // The real opponent replacement forecast remains UNKNOWN below.
            var c=change(nativeCase,"enemy",false);var f=fixture(c,"");boolean switching=c.s("mode").equals("switch"),unsupported=c.s("id").equals("unsupported");
            boolean entered=!row.get("entry").isJsonNull()&&row.getAsJsonObject("entry").get("selfHp").getAsDouble()>0;
            if(entered&&!row.get("probe").isJsonNull()&&!unsupported){
                var probe=row.getAsJsonObject("probe");var current=f.execution().conditionalParticipants("reserve0",false);
                var a=observed("reserve0",f.mechanics().transitions().context().switchTargets().get("reserve0").facts(),probe,"self");
                var t=observed("opponent",root(f).opponent().facts(),probe,"foe");
                if(setter){
                    var change=current.entryWeather(f.profiles().get("reserve0"),a,t);
                    check(change.known(),"Certified native setter entry lost weather proof: "+c.data());
                    current=current.conditionalWeather(change.value().orElse(null));
                    check(current.rawWeather().value().equals(probe.get("weather").getAsString()),"Native raw weather entry differs");
                    if(!c.s("direction").equals("depart")&&!c.s("weather").equals(probe.get("weather").getAsString()))
                        check(current.projectedWeatherDuration().known()&&current.projectedWeatherDuration().value()==probe.get("duration").getAsInt(),"Native weather rock/duration differs: "+c.data());
                }
                check(current.effectiveWeather(a,t).known()&&current.effectiveWeather(a,t).value().equals(probe.get("effective").getAsString()),"Native entry effective weather differs");
                var speed=current.speed(f.profiles().get("opponent"),t,a);int nativeSpeed=probe.get("speed").getAsInt();
                check(speed.known()&&speed.value().low()<=nativeSpeed&&speed.value().high()>=nativeSpeed,"Native entry speed excluded: "+c.data()+" / "+speed);
                var values=probe.getAsJsonArray("damage");
                if(!values.isEmpty()){
                    var accuracy=current.accuracy(c.reply(),f.profiles().get("opponent"),t,f.profiles().get("reserve0"),a);
                    check(accuracy.known()&&accuracy.value()==probe.get("threshold").getAsInt(),"Native entry accuracy differs: "+c.data()+" / "+accuracy+" / "+probe);
                    for(Critical critical:Critical.values()){
                        var rolls=new ArrayList<Integer>();for(var v:values){var r=v.getAsJsonObject();if(critical==Critical.BOTH||r.get("crit").getAsBoolean()==(critical==Critical.CRITICAL))rolls.add(r.get("damage").getAsInt());}
                        var actual=current.damage(c.reply(),f.profiles().get("opponent"),t,f.profiles().get("reserve0"),a,critical);
                        check(actual.points().known()&&actual.points().value().equals(new IntRange(Collections.min(rolls),Collections.max(rolls))),"Native entry damage extrema differ: "+c.data()+" / "+critical+" / "+actual+" / "+rolls);
                    }damageRolls+=values.size();
                }probes++;
            }
            for(boolean end:List.of(false,true)){
                var expected=row.getAsJsonObject(end?"post":"pre");var actual=rank(f,switching,end);
                boolean recoil=!switching&&c.s("id").equals("recoil")&&c.n("hp")==100&&c.n("ownBench")>0;
                boolean stopped=!switching&&c.b("protect");
                boolean supported=!unsupported&&(stopped||recoil||entered&&(switching||c.n("ownBench")==1&&c.n("targetHp")>80));
                boolean unknownClock=c.s("weatherMode").equals("upkeep");
                if(setter&&entered&&row.getAsJsonObject("entry").get("weather").getAsString().equals(c.s("weather"))==false)unknownClock=false;
                if(end&&(unknownClock||expected.get("foeHp").getAsDouble()==0
                        ||!recoil&&expected.get("selfHp").getAsDouble()==0
                        ||entered&&Set.of("tox","slp","frz").contains(expected.get("selfStatus").getAsString())))supported=false;
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native weather entry excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved entry/party/duration gained weather scope: "+c.data()+" / end="+end+" / "+actual);}
                if(c.s("id").equals("party")){String key=nativeCase.b("enemy")+":"+c.s("direction")+":"+c.s("suppressor")+":"+switching+":"+end+":"+c.n("ownBench")+":"+c.n("targetHp");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party changed weather entry");}
            }
            if(nativeCase.b("enemy")&&!switching){var other=rank(fixture(nativeCase,""),false,false);check(!other.utility().known()||nativeCase.b("protect"),"Actual hidden opponent replacement gained weather scope");}
        }
        System.out.println((setter?"Weather setter native differential PASS:":cold?"Cold weather entry native differential PASS:":"Weather entry native differential PASS:")+" "+rows.size()+" queued entries/departures across both native roles; "+probes+" declared owner-entry probes, "+damageRolls+" critical/roll outcomes; "+bounded+" bounded / "+unknown+" UNKNOWN at two boundaries; damage, accuracy, speed, residuals, hazards, clocks and party privacy");
    }
    private static Actor observed(String id,BattleSnapshot.Combatant facts,JsonObject row,String role){
        var stages=new TreeMap<String,Integer>();row.getAsJsonObject(role+"Stages").entrySet().forEach(e->stages.put(e.getKey(),e.getValue().getAsInt()));double hp=row.get(role+"Hp").getAsDouble();
        return new Actor(id,facts,known(new Interval(hp,hp)),stages,known(row.get(role+"Status").getAsString()));
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject expected){
        if(!line.utility().known()||!line.state().self().id().equals(expected.get("active").getAsString()))return false;
        double u=expected.get("utility").getAsDouble();if(u<line.utility().value().low()-1e-10||u>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var a=role.equals("self")?line.state().self():line.state().opponent();double hp=expected.get(role+"Hp").getAsDouble();if(hp<a.hp().value().low()-1e-10||hp>a.hp().value().high()+1e-10||!a.status().value().equals(expected.get(role+"Status").getAsString()))return false;
            for(var e:expected.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(a.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }
        var hazards=new TreeMap<String,Integer>();expected.getAsJsonObject("hazards").entrySet().forEach(e->hazards.put(e.getKey(),e.getValue().getAsInt()));return line.state().selfHazards().layers().equals(hazards);
    }
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString()).option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
