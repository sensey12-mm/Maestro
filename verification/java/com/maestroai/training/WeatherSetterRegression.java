package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PivotOutcomeRegression.*;

/** Declared own entry/weather counterfactuals; never hidden opponent knowledge. */
final class WeatherSetterRegression {
    private static final String SOURCE="declared native weather setter entry";
    private static final Map<String,String> WEATHER=Map.of("drought","sunnyday","drizzle","raindance","sandstream","sandstorm","snowwarning","snow");
    private static final Map<String,String> ROCK=Map.of("drought","heatrock","drizzle","damprock","sandstream","smoothrock","snowwarning","icyrock");
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case change(Case c,String k,Object v){var d=c.data().deepCopy();if(v instanceof String a)d.addProperty(k,a);else if(v instanceof Integer a)d.addProperty(k,a);else if(v instanceof Boolean a)d.addProperty(k,a);return new Case(d);}
    private static Case base(String ability){return change(change(change(ordinary(),"reserveAbility",ability),"weather","none"),"userAbility","runaway");}
    private static State root(Fixture f){return project(f.snapshot(),f.mechanics()).states().get("root");}
    private static ShadowMechanicsEvaluation.Plan plan(String action,String reply){return new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,reply,null))),SOURCE);}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean switching,boolean end){
        String action=switching?"switch:reserve0":"move:"+f.config().action();var plans=List.of(plan(action,f.config().reply()));
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),plans,known(false),Critical.BOTH,action,f.execution()):ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),plans,known(false),Critical.BOTH,action,f.execution())).rankings().getFirst();
    }
    private static ScopedExecution projected(Fixture f){
        var target=f.mechanics().transitions().context().switchTargets().get("reserve0");
        var a=new Actor("reserve0",target.facts(),known(new Interval(target.facts().hpFraction(),target.facts().hpFraction())),target.facts().knownBoosts(),target.status());
        var frame=f.execution().conditionalParticipants("reserve0",false);
        var weather=frame.entryWeather(f.profiles().get("reserve0"),a,root(f).opponent());
        check(weather.known(),"Declared supported entry has no weather projection");
        return frame.conditionalWeather(weather.value().orElse(null));
    }
    static void run(){
        for(String ability:WEATHER.keySet())for(boolean switching:List.of(false,true)){
            var c=base(ability);var f=fixture(c,"");
            check(rank(f,switching,true).utility().known(),"Supported weather setter stayed UNKNOWN: "+ability);
            check(f.execution().field().weather().value().equals("none")&&f.execution().rawWeather().value().equals("none"),"Entry mutated observed/root weather");
            check(!f.execution().profile(f.profiles().get("reserve0"),true).ordinaryEffects().known(),"Raw reserve certified weather activation");
            var frame=projected(f);check(frame.rawWeather().value().equals(WEATHER.get(ability))&&frame.rawWeather().source().contains("conditional-weather-entry"),"Projected weather lacks separate provenance");
            check(frame.projectedWeatherDuration().value()==5&&frame.weatherAtResidual().value(),"Fresh setter duration not certified");
            for(var operation:List.of(ExecutionCoverage.Operation.ENTRY,ExecutionCoverage.Operation.DAMAGE)){
                var blockedScope=frame.withScopes(ExecutionCoverage.Scopes.receipt(op->op==operation?Known.unknown():known(true),()->true));
                check(!blockedScope.rawWeather().known()&&!blockedScope.projectedWeatherDuration().known()&&!blockedScope.weatherAtResidual().known(),"Withdrawing an entry/source scope retained weather proof");
            }
            String action=switching?"switch:reserve0":"move:uturn";
            var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);
            var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            check(!ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(future),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst().utility().known(),"Known weather duration certified a future request");
            for(boolean room:List.of(false,true)){
                var rock=projected(fixture(change(change(c,"reserveItem",ROCK.get(ability)),"magicRoom",room),""));
                check(rock.projectedWeatherDuration().value()==(room?5:8),"Weather rock ignored item suppression");
                var umbrella=projected(fixture(change(change(c,"reserveItem","utilityumbrella"),"magicRoom",room),""));
                check(umbrella.rawWeather().value().equals(WEATHER.get(ability))&&umbrella.projectedWeatherDuration().value()==5,"Umbrella prevented global weather activation");
            }
            for(String suppressor:List.of("airlock","cloudnine")){
                var g=fixture(change(c,"targetAbility",suppressor),"");var h=projected(g);
                var target=g.mechanics().transitions().context().switchTargets().get("reserve0");
                var a=new Actor("reserve0",target.facts(),known(new Interval(1,1)),target.facts().knownBoosts(),target.status());
                check(h.effectiveWeather(a,root(g).opponent()).value().equals("none")&&h.rawWeather().value().equals(WEATHER.get(ability)),"Suppressor stopped raw weather or its clock");
                check(rank(g,switching,true).utility().known(),"Supported suppressed weather entry stayed UNKNOWN");
            }
            for(String missing:List.of("own-item","opponent-item","opponent-ability","entry-scope","damage-scope","reserve-binding","reserve-profile","reserve-target","foreign-reserve","foreign-profile","stale-reserve","reserve-types","reserve-max","reserve-hp","reserve-item","reserve-ability","reserve-status","partial-hazards","entry-damage","entry-speed","suppression","ambiguous","gravity","wonder-room"))
                check(!rank(fixture(c,missing),switching,true).utility().known(),"Weather setter bypassed evidence: "+missing);
            var same=change(change(change(c,"weather",WEATHER.get(ability)),"weatherMode","upkeep"),"targetAbility",Map.of("drought","solarpower","drizzle","raindish","sandstream","runaway","snowwarning","icebody").get(ability));
            var old=fixture(same,"");check(rank(old,switching,false).utility().known()&&!rank(old,switching,true).utility().known(),"Same-weather entry refreshed an unproved timer");
            var different=fixture(change(same,"weather",ability.equals("drought")?"raindance":"sunnyday"),"");
            check(rank(different,switching,true).utility().known(),"A real weather replacement retained the old unknown clock");
            var source=fixture(c,"");var projected=projected(source);source.current().set(false);
            check(!projected.projectedWeatherDuration().known()&&!rank(source,switching,true).utility().known(),"Expired source retained projected weather");
            var owner=fixture(c,"");var owned=projected(owner);owner.transport().endChoice();
            check(!owned.projectedWeatherDuration().known()&&!rank(owner,switching,true).utility().known(),"Expired own receipt retained weather projection");
            var expired=fixture(c,"");var oldFrame=projected(expired);expired.fields().reset();check(!oldFrame.projectedWeatherDuration().known(),"Expired field retained projected weather");
            var closing=fixture(c,"");var armed=new java.util.concurrent.atomic.AtomicBoolean();var reads=new java.util.concurrent.atomic.AtomicInteger();
            Map<String,Profile> fading=new AbstractMap<>(){
                @Override public Set<Map.Entry<String,Profile>> entrySet(){return closing.profiles().entrySet();}
                @Override public Profile get(Object key){if(key.equals("reserve0"))armed.set(true);if(key.equals("opponent")&&armed.get()&&reads.incrementAndGet()==2)closing.current().set(false);return closing.profiles().get(key);}
            };
            var withdrawn=rank(new Fixture(c,closing.snapshot(),closing.mechanics(),fading,closing.execution(),closing.transport(),closing.current(),closing.fields()),switching,true);
            check(!closing.current().get()&&!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().self().id().equals("self")),"Closing receipt leaked a weather-entry forecast");
            var plans=List.of(plan("move:uturn",c.reply()),plan("switch:reserve0",c.reply()));var blocked=fixture(change(c,"protect",true),"");
            var first=ShadowMechanicsEvaluation.compareEndOfTurn(blocked.snapshot(),blocked.mechanics(),blocked.profiles(),plans,known(false),Critical.BOTH,"move:uturn",blocked.execution());
            var reversed=ShadowMechanicsEvaluation.compareEndOfTurn(blocked.snapshot(),blocked.mechanics(),blocked.profiles(),List.of(plans.getLast(),plans.getFirst()),known(false),Critical.BOTH,"move:uturn",blocked.execution());
            check(first.rankings().equals(reversed.rankings())&&first.rankings().stream().allMatch(r->r.utility().known()),"Weather projection leaked across candidate order");
        }
        for(String ability:WEATHER.keySet()){
            String start="update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L100|100/100\n|turn|1\n";
            String event="|-weather|"+WEATHER.get(ability)+"|[from] ability: "+ability+"|[of] p2a: Foe";
            for(String suffix:List.of("","|unexpected")){
                var history=new ArrayList<>(List.of(start+event+suffix+"\n|turn|2"));var p=new PublicProtocolEvidence();p.update(history);
                var evidence=new PublicMechanicsEvidence(id->null);evidence.update(p);var fields=new PublicFieldEvidence();var capture=fields.update(p,evidence);
                check(capture.weatherAtResidual().known()==suffix.isEmpty(),"Unproved weather attribution established a clock");
                check(evidence.observation("p2a").ability().known()==suffix.isEmpty(),"Unproved weather attribution revealed ability");
                check(!evidence.observation("p2a").item().known(),"Weather attribution inferred a hidden item");
                if(suffix.isEmpty())for(int turn=3;turn<=6;turn++){
                    history.add("update\n"+event+"\n|turn|"+turn);p.update(history);evidence.update(p);capture=fields.update(p,evidence);
                    check(capture.weatherAtResidual().known()==(turn<=4),"Repeated ability announcement refreshed minimum clock");
                }
            }
        }
        for(String alteration:List.of("wrong-weather","wrong-source","extra-tag","unknown-ability")){
            String weather=alteration.equals("wrong-weather")?"snow":"sunnyday";
            String source=alteration.equals("wrong-source")?"p2a: Different":"p2a: Foe";
            String ability=alteration.equals("unknown-ability")?"unknownweather":"Drought";
            String event="|-weather|"+weather+"|[from] ability: "+ability+"|[of] "+source+(alteration.equals("extra-tag")?"|unproved":"");
            var p=new PublicProtocolEvidence();p.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L100|100/100\n|turn|1\n"+event+"\n|turn|2"));
            var evidence=new PublicMechanicsEvidence(id->null);evidence.update(p);var capture=new PublicFieldEvidence().update(p,evidence);
            check(!capture.unknown().isEmpty()&&!capture.weatherAtResidual().known(),"Unproved weather attribution retained field coverage");
            check(!evidence.observation("p2a").ability().known(),"Unproved weather attribution certified an ability");
        }
        System.out.println("Weather setter evidence regressions PASS: "+checks+" checks; entry, provenance, duration, suppression, branch isolation, public attribution and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        WeatherEntryRegression.setterDifferential(engine);
        String source=Files.readString(Path.of("verification/weather_entry_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.setterClocks(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==64,"Native setter clock inventory changed");
        for(var value:rows){var row=value.getAsJsonObject();int expected=row.get("gen").getAsInt()==5?0:!row.get("item").getAsString().isEmpty()&&!row.get("room").getAsBoolean()?8:5;
            check(row.get("duration").getAsInt()==expected&&row.get("repeated").getAsInt()==(expected==0?0:2),"Native generation, rock or refresh boundary differs");
            check(!row.get("ability").getAsString().equals("snowwarning")||row.get("weather").getAsString().equals(row.get("gen").getAsInt()==9?"snow":"hail"),"Snow Warning generation boundary differs");
        }
        var primal=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.setterPrimal(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(primal.size()==4,"Native Primal queue inventory changed");
        for(var value:primal){var row=value.getAsJsonObject();check(row.get("weather").getAsString().equals(row.get("queued").getAsBoolean()?"none":WEATHER.get(row.get("ability").getAsString())),"Native Primal queue weather deferral differs");}
        System.out.println("Weather setter generation/duration native differential PASS: 64 controls; Gen5 permanent weather, Gen6+ finite clocks, Snow Warning hail/snow, item suppression and no same-weather refresh; four native Primal queue controls");
    }
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString()).option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
