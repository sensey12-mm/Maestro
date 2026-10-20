package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Native conditional status branches with current owner/public receipts, never hidden counters. */
final class StatusSpeedRegression {
    private static final String SOURCE="declared native status/speed conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case par(){return change(change(change(shield(),"status","par"),"item","choicescarf"),"targetAbility","runaway");}
    private static Case number(Case c,String key,int n){var d=c.data().deepCopy();d.addProperty(key,n);return new Case(d);}
    private static Case flag(Case c,String key,boolean b){var d=c.data().deepCopy();d.addProperty(key,b);return new Case(d);}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Known<IntRange> speedOf(Fixture f){return f.execution().speed(f.profiles().get(f.config().user()),user(f));}
    private static ShadowMechanicsEvaluation.Ranking evaluated(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    static void run(){
        for(boolean enemy:List.of(false,true)){
            Case c=enemy?enemy(par()):par();var f=fixture(c,"");
            check(speedOf(f).known()&&evaluate(f).utility().known(),"Source-proven paralysis remains UNKNOWN");
            check(evaluate(f).lines().size()>=2,"Paralysis nonexecution branch omitted");
            check(!evaluated(f,null).utility().known(),"Unattested status execution became supported");
            var witnessed=flag(flag(c,"wind",true),"witnessTailwind",true);
            check(evaluate(fixture(witnessed,"")).utility().known(),"Witnessed past Tailwind poisoned current status/speed coverage");
            check(!evaluate(fixture(witnessed,"indirect-setter")).utility().known(),"Indirect Tailwind setter gained execution scope");
            check(evaluate(fixture(flag(c,"pastPar",true),"")).utility().known(),"Past source-known paralysis nonexecution poisoned current coverage");
            check(!evaluate(fixture(c,"unsupported-cant")).utility().known(),"Unresolved previous recovery/nonexecution gained scope");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","order-scope",
                    "speed-scope","speed-stat","status","suppression","ambiguous","forged-opponent"))
                check(!evaluate(fixture(c,missing)).utility().known(),"Paralysis bypassed current source/public evidence: "+missing);
            check(!evaluate(fixture(c,"damage-scope")).utility().known(),"Nonexecution shortcut replaced damage proof");
            var priority=change(c,"move","quickattack");
            check(evaluate(fixture(priority,"speed-scope")).utility().known(),"Priority required irrelevant speed proof");
            check(!evaluate(fixture(priority,"order-scope")).utility().known(),"Priority bypassed native status/order proof");
            for(String move:List.of("bulkup","recover")){
                var setup=change(c,"move",move);
                check(evaluate(fixture(setup,"")).utility().known(),"Paralyzed setup/recovery failed: "+move);
                check(!evaluate(fixture(setup,"setup-scope")).utility().known(),"Nonexecution replaced setup proof");
                check(evaluate(fixture(setup,"damage-scope")).utility().known(),"Setup required unrelated damage proof");
            }
            for(String status:List.of("slp","frz","brn","psn","tox")){
                var unresolved=change(c,"status",status);
                check(speedOf(fixture(unresolved,"")).known(),"Status with ordinary speed lost isolated arithmetic: "+status);
                check(!evaluate(fixture(unresolved,"")).utility().known(),"Unproved recovery/residual forecast became known: "+status);
                check(!evaluate(fixture(change(unresolved,"move","quickattack"),"")).utility().known(),"Priority bypassed status duration/recovery: "+status);
            }
            var odd=number(number(c,"userSpeed",3),"speedStage",0);
            check(speedOf(fixture(odd,"")).value().equals(new IntRange(1,2)),"Tied Scarf/paralysis callbacks must retain both native orders");
            check(speedOf(fixture(flag(odd,"wind",true),"")).value().equals(new IntRange(3,4)),"Native callback-order bounds must include Tailwind");
            var zero=number(odd,"userSpeed",1);
            check(!speedOf(fixture(zero,"")).known(),"Native zero speed escaped positive IntRange domain");
            check(!speedOf(fixture(flag(zero,"wind",true),"")).known(),"Tailwind incorrectly restored a zero native speed");
            var expired=fixture(c,"");var receipt=expired.execution().speedCoverage(expired.profiles().get(c.user()),user(expired));expired.owner().endChoice();
            check(receipt.supported()==enemy&&speedOf(expired).known()==enemy&&!evaluate(expired).utility().known(),
                    "Owner receipt expiration must withdraw the exchange and own speed, preserving independently public foe speed");
            var source=fixture(c,"");source.current().set(false);
            check(!speedOf(source).known()&&!evaluate(source).utility().known(),"Expired runtime receipt retained status forecast");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{speedOf(field);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field receipt retained speed");
            var closing=fixture(c,"");var expiring=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{
                if(op==ExecutionCoverage.Operation.SPEED)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=evaluated(closing,expiring);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt expiration leaked paralysis branches");
            var uncertain=fixture(change(c,"item","lifeorb"),"",known(new Interval(0,.5)));
            check(!evaluate(uncertain).utility().known(),"Unproved statused execution/survival gained certainty");
        }
        check(!supportedMoveIds().contains("tailwind"),"Past Tailwind setter became a supported selectable action");
        System.out.println("Status/speed evidence regressions PASS: "+checks+" checks; status branches, priority, missing evidence and receipt withdrawal");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/status_speed_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var speeds=data.getAsJsonArray("speeds");check(speeds.size()==120,"Native speed inventory changed");
        for(var value:speeds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));int low=row.get("low").getAsInt(),high=row.get("high").getAsInt();var actual=speedOf(fixture(c,""));
            check(low==0?!actual.known():actual.known()&&actual.value().equals(new IntRange(low,high)),"Native status speed differs: "+c.data()+" native="+low+".."+high+" actual="+actual);
        }
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==224,"Native status exchange inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");var result=evaluate(f);
            check(result.utility().known()&&result.lines().stream().anyMatch(line->matches(line,row)),"Native status exchange excluded: "+c.data()+" / "+row+" / "+result);
            var order=f.execution().order("move:"+c.action(),c.reply(),f.profiles().get("self"),f.state().self(),f.profiles().get("opponent"),f.state().opponent());
            if(order!=Order.BOTH)check(row.getAsJsonArray("order").get(0).getAsString().equals(order==Order.SELF_FIRST?"self":"opponent"),"Native action ordering differs: "+c.data());
        }
        var boundaries=data.getAsJsonArray("boundaries");check(boundaries.size()==26,"Native recovery inventory changed");
        for(var value:boundaries){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));
            check(!evaluate(fixture(c,"")).utility().known(),"Hidden sleep counter or unresolved freeze recovery gained scope: "+c.data());
        }
        System.out.println("Status/speed native differential PASS: 120 speed configurations, 224 queued exchanges, 26 sleep/freeze boundaries; exact modifier-order extrema, zero-cache boundaries, secondary effects and both actor roles");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(!line.utility().known()||utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var a=role.equals("self")?line.state().self():line.state().opponent();double hp=row.get(role+"Hp").getAsDouble();
            if(hp<a.hp().value().low()-1e-10||hp>a.hp().value().high()+1e-10||!a.status().value().equals(row.get(role+"Status").getAsString()))return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(a.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
}
