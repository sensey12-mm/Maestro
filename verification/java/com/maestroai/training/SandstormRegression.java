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
            for(String ability:List.of("neutralizinggas"))
                check(!residual(fixture(change(c,"ability",ability),"")).utility().known(),"Unsupported weather ability gained scope: "+ability);
            for(String item:List.of("expertbelt"))
                check(!residual(fixture(change(c,"item",item),"")).utility().known(),"Unsupported weather item gained scope: "+item);
            var umbrella=residual(fixture(change(c,"item","utilityumbrella"),""));var ordinary=residual(fixture(change(c,"item","none"),""));
            check(umbrella.utility().known()&&umbrella.utility().value().equals(ordinary.utility().value()),"Umbrella changed supported sandstorm outcomes");
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
        abilities();
        protection();
        System.out.println("Sandstorm evidence regressions PASS: "+checks+" checks; weather lifetime, suppression, HP/survival, public identity and receipt expiration");
    }
    private static void protection(){
        for(boolean enemy:List.of(false,true)){
            Case c=change(change(change(enemy?enemy(storm()):storm(),"move","bulkup"),"item","safetygoggles"),"targetItem","safetygoggles");
            var f=fixture(c,"");var result=residual(f);
            check(result.utility().known()&&result.lines().stream().allMatch(l->l.state().self().hp().value().equals(f.state().self().hp().value())
                    &&l.state().opponent().hp().value().equals(f.state().opponent().hp().value())),"Goggles lost weather protection on Normal types");
            var room=c.data().deepCopy();room.addProperty("room",true);var suppressed=new Case(room);var disabled=fixture(suppressed,"");
            check(residual(disabled).utility().known()&&residual(disabled).lines().stream().allMatch(l->l.state().self().hp().value().high()<disabled.state().self().hp().value().low()
                    &&l.state().opponent().hp().value().high()<disabled.state().opponent().hp().value().low()),"Magic Room retained Goggles protection");
            var veil=fixture(change(suppressed,"ability","sandveil"),"");
            check(residual(veil).utility().known()&&residual(veil).lines().stream().allMatch(l->{var user=enemy?l.state().opponent():l.state().self();var before=enemy?veil.state().opponent():veil.state().self();
                return user.hp().value().equals(before.hp().value());}),"Magic Room suppressed Sand Veil weather immunity");
            var poisoned=fixture(change(c,"status","psn"),"");
            check(residual(poisoned).utility().known()&&residual(poisoned).lines().stream().allMatch(l->{var user=enemy?l.state().opponent():l.state().self();return user.hp().value().equals(new Interval(.375,.375));}),"Goggles prevented poison damage");
            var magic=fixture(change(change(c,"status","psn"),"ability","magicguard"),"");
            check(residual(magic).utility().known()&&residual(magic).lines().stream().allMatch(l->{var user=enemy?l.state().opponent():l.state().self();return user.hp().value().equals(new Interval(.5,.5));}),"Goggles broke Magic Guard composition");
            var uncertain=change(c,"weatherMode","upkeep");
            check(!residual(fixture(uncertain,"")).utility().known(),"Protected actors inferred weather expiration");
            check(residual(fixture(number(uncertain,"weatherDuration",1),"")).equals(residual(fixture(number(uncertain,"weatherDuration",8),""))),"Private weather counter entered Goggles forecast");
            check(residual(fixture(number(suppressed,"roomDuration",1),"")).equals(residual(fixture(number(suppressed,"roomDuration",5),""))),"Oracle Magic Room counter entered current protection");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "types","target-types","attacker-types","target-max","status","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!residual(fixture(c,missing)).utility().known(),"Goggles/Sand Veil bypassed residual evidence: "+missing);
            check(!residual(fixture(c,"",known(new Interval(0,.5)))).utility().known(),"Goggles inferred survival/party closure");
            check(!residual(fixture(change(c,"weather","primordialsea"),"")).utility().known(),"Goggles extended unsupported extreme-weather outcomes");
            var expired=fixture(c,"");var cert=expired.execution().residualCoverage(expired.profiles().get("self"),expired.state().self());expired.owner().endChoice();
            check(!cert.supported()&&!residual(expired).utility().known(),"Expired owner retained Goggles residual protection");
            var source=fixture(c,"");var sourceCert=coverage(source);source.current().set(false);
            check(!sourceCert.supported()&&!residual(source).utility().known(),"Expired source retained Goggles residual protection");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{residual(field);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field retained Goggles residual protection");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=residual(closing,frame);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing Goggles receipt leaked residual states");
        }
        var room=change(change(change(storm(),"move","bulkup"),"item","safetygoggles"),"targetItem","safetygoggles").data().deepCopy();room.addProperty("room",true);room.addProperty("userHp",1);room.addProperty("ownBench",1);
        var continued=residual(fixture(new Case(room),""));
        check(continued.utility().known()&&continued.lines().stream().allMatch(l->l.state().self().hp().value().high()==0&&l.state().forcedReplacement()&&!l.state().nextRequestKnown()),"Suppressed Goggles KO lost certified own continuation");
        check(!residual(fixture(new Case(room),"reserve-binding")).utility().known(),"Suppressed Goggles inferred reserve binding");
    }
    private static Known<IntRange> speed(Fixture f){
        var c=f.config();var a=c.b("enemy")?f.state().opponent():f.state().self();
        return f.execution().speed(f.profiles().get(c.user()),a);
    }
    private static void abilities(){
        for(boolean enemy:List.of(false,true))for(String ability:List.of("sandrush","sandforce")){
            var c=change(change(enemy?enemy(storm()):storm(),"ability",ability),"move","ironhead");var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&speed(f).known()&&residual(f).utility().known(),"Supported sand ability remained UNKNOWN: "+ability);
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope",
                    "target-types","attacker-types","target-max","status","speed-stat","suppression","ambiguous","forged-opponent"))
                check(!residual(fixture(c,missing)).utility().known(),"Sand ability bypassed evidence: "+ability+" / "+missing);
            var unknownDuration=fixture(change(c,"weatherMode","upkeep"),"");
            check(damage(unknownDuration,Critical.BOTH).points().known()&&speed(unknownDuration).known()&&!residual(unknownDuration).utility().known(),"Weather lifetime was inferred from an active ability");
            var ended=fixture(change(c,"weatherMode","ended"),"");var none=fixture(change(c,"weather","none"),"");
            check(damage(ended,Critical.BOTH).points().equals(damage(none,Critical.BOTH).points())&&speed(ended).equals(speed(none)),"Ended weather retained ability modifier");
            var restarted=fixture(change(c,"weatherMode","restart"),"");
            check(damage(f,Critical.BOTH).points().value().equals(damage(restarted,Critical.BOTH).points().value())&&speed(f).value().equals(speed(restarted).value()),"Restarted weather lost ability modifier");
            var unsupported=fixture(change(c,"weather","desolateland"),"");
            check(!damage(unsupported,Critical.BOTH).points().known()&&(!ability.equals("sandrush")||!speed(unsupported).known()),"Unproved Sand Rush weather gained speed");
            var source=fixture(c,"");var cert=coverage(source);source.current().set(false);
            check(!cert.supported()&&!speed(source).known()&&!residual(source).utility().known(),"Expired sand ability source retained arithmetic");
            var own=fixture(c,"");own.owner().endChoice();
            check(!residual(own).utility().known()&&speed(own).known()==(enemy&&ability.equals("sandforce")),"Sand Rush must prove both active abilities; non-weather speed stays independent");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.SPEED)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=residual(closing,frame);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Expired closing sand ability receipt leaked branch states");
            var immunity=fixture(change(change(change(c,"move","bulkup"),"item","none"),"targetAbility","sandforce"),"");
            var immuneResult=residual(immunity);
            check(immuneResult.utility().known()&&immuneResult.lines().stream().allMatch(l->l.state().self().hp().value().equals(immunity.state().self().hp().value())
                    &&l.state().opponent().hp().value().equals(immunity.state().opponent().hp().value())),"Sand ability lost typeless sandstorm immunity");
            var doubled=fixture(change(c,"ability","sandrush"),"");var plain=fixture(change(change(c,"ability","sandrush"),"weather","none"),"");
            check(speed(doubled).value().low()==2*speed(plain).value().low(),"Sand Rush did not double current speed");
            if(ability.equals("sandforce"))check(damage(f,Critical.BOTH).points().value().high()>damage(none,Critical.BOTH).points().value().high(),"Sand Force did not boost Steel base power");
        }
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
        var configs=data.getAsJsonArray("damage");check(configs.size()==272,"Native weather damage inventory changed");int rolls=0;
        for(var value:configs){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(Critical critical:Critical.values()){
                var outcomes=new ArrayList<Integer>();for(var v:row.getAsJsonArray("values")){var r=v.getAsJsonObject();if(critical==Critical.BOTH||r.get("crit").getAsBoolean()==(critical==Critical.CRITICAL))outcomes.add(r.get("damage").getAsInt());}
                var actual=damage(f,critical);int low=Collections.min(outcomes),high=Collections.max(outcomes);
                check(actual.points().known()&&actual.points().value().equals(new IntRange(low,high)),"Native weather stat/critical rounding differs: "+c.data()+" / "+critical+" / "+actual+" / "+outcomes);
            }rolls+=row.getAsJsonArray("values").size();
        }
        check(rolls==8704,"Native sandstorm roll inventory changed");
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==396,"Native Sand Rush speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=speed(fixture(c,""));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();
            check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native Sand Rush callback ordering differs: "+c.data()+" / "+row+" / "+actual);
        }
        var rows=data.getAsJsonArray("exchanges");check(rows.size()==618,"Native weather residual inventory changed");int bounded=0,unknown=0;
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
            if(Set.of("weather-knockout","ability-recoil-continuation","goggles-knockout").contains(c.s("id"))){
                String key=c.s("id")+":"+c.s("ability")+":"+c.b("enemy")+":"+c.userHp()+":"+c.n("targetHp")+":"+c.n("ownBench");
                if(c.n("foeBench")==0)hidden.put(key,result);else check(result.equals(hidden.get(key)),"Hidden opponent reserves changed weather evaluation");
            }
        }
        System.out.println("Sandstorm native differential PASS: 8 duration factories, 272 damage configurations, 8704 critical/roll outcomes, 396 speed configurations, 618 queued outcomes; "+bounded+" bounded, "+unknown+" UNKNOWN; both roles, Sand Veil/Goggles, Magic Room expiry, immunity, recovery and fainting");
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
