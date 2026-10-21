package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Native hit/miss branches use current public/own evidence, never inferred opponent builds. */
final class AccuracyMissRegression {
    private static final String SOURCE="declared native accuracy/miss conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case rock(){return change(change(shield(),"move","rocktomb"),"targetAbility","runaway");}
    private static Case number(Case c,String key,int n){var d=c.data().deepCopy();d.addProperty(key,n);return new Case(d);}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Actor target(Fixture f){return f.config().b("enemy")?f.state().self():f.state().opponent();}
    private static Known<Integer> accuracy(Fixture f){var c=f.config();return f.execution().accuracy(c.s("move"),f.profiles().get(c.user()),user(f),f.profiles().get(c.target()),target(f));}
    private static ExecutionCoverage.Certificate receipt(Fixture f){var c=f.config();return f.execution().accuracyCoverage(c.s("move"),f.profiles().get(c.user()),user(f),f.profiles().get(c.target()),target(f));}
    private static ShadowMechanicsEvaluation.Ranking evaluated(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    static void run(){
        for(boolean enemy:List.of(false,true)){
            Case c=enemy?enemy(rock()):rock();var fresh=fixture(c,"");var result=evaluate(fresh);
            check(accuracy(fresh).known()&&accuracy(fresh).value()==95&&result.utility().known(),"Proven accuracy remains UNKNOWN");
            check(result.lines().stream().anyMatch(l->(enemy?l.state().self():l.state().opponent()).hp().value().equals(target(fresh).hp().value())),"Miss branch omitted");
            check(result.lines().stream().anyMatch(l->(enemy?l.state().self():l.state().opponent()).stages().get("spe")==-1),"Rock Tomb hit branch omitted its effect");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope",
                    "accuracy-stage","evasion-stage","status","suppression","ambiguous","forged-opponent","target-max","attack-stat","level","types",
                    "gravity","magnet-rise","unsupported-terrain"))
                check(!evaluate(fixture(c,missing)).utility().known(),"Hit/miss bypassed evidence: "+missing);
            check(!evaluated(fresh,null).utility().known(),"Unattested hit/miss execution became known");
            check(evaluate(fixture(c,"entry-scope")).utility().known(),"Accuracy required an unrelated entry receipt");
            for(String missing:List.of("accuracy-stage","evasion-stage")){
                var blocked=fixture(c,"protect-"+missing);
                check(!accuracy(blocked).known()&&evaluate(blocked).utility().known(),"Native early protection incorrectly required accuracy evidence: "+missing);
                check(damage(fixture(c,missing),Critical.BOTH).points().known(),"Unknown accuracy stage poisoned successful-hit damage arithmetic: "+missing);
            }
            var expired=fixture(c,"");var cert=receipt(expired);expired.owner().endChoice();
            check(!cert.supported()&&!accuracy(expired).known()&&!evaluate(expired).utility().known(),"Expired own receipt retained miss forecast");
            var source=fixture(c,"");var sourceCert=receipt(source);source.current().set(false);
            check(!sourceCert.supported()&&!accuracy(source).known()&&!evaluate(source).utility().known(),"Expired source receipt retained miss forecast");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{accuracy(field);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field receipt retained accuracy");
            var closing=fixture(c,"");var expiring=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=evaluated(closing,expiring);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing expiration leaked hit/miss states");
            check(!evaluate(fixture(c,"",known(new Interval(0,.5)))).utility().known(),"Unproved survival certified accuracy execution");
            for(String unsupported:List.of("blunderpolicy","widelens","brightpowder"))check(!evaluate(fixture(change(c,"item",unsupported),"")).utility().known(),"Unsupported miss/accuracy item became known: "+unsupported);
            for(String ability:List.of("compoundeyes","noguard","sandveil"))check(!evaluate(fixture(change(c,"ability",ability),"")).utility().known(),"Unsupported accuracy ability became known: "+ability);
            check(!evaluate(fixture(change(c,"weather","hail"),"")).utility().known(),"Unsupported weather became known");
            var swift=number(number(change(c,"move","swift"),"accuracyStage",-6),"targetEvasionStage",6);
            check(accuracy(fixture(swift,"accuracy-stage")).value()==100&&evaluate(fixture(swift,"accuracy-stage")).utility().known(),"Always-hit Swift incorrectly required accuracy stages");
            var rain=number(number(change(change(c,"move","thunder"),"weather","raindance"),"accuracyStage",-6),"targetEvasionStage",6);
            check(accuracy(fixture(rain,"evasion-stage")).value()==100&&evaluate(fixture(rain,"evasion-stage")).utility().known(),"Rain Thunder lost its native always-hit exemption");
            check(accuracy(fixture(change(rain,"weather","sunnyday"),"")).value()==16,"Sun Thunder stage rounding/order differs");
            var guaranteed=number(c,"accuracyStage",1);check(accuracy(fixture(guaranteed,"")).value()==126,"Native accuracy threshold flooring differs");
            check(evaluate(fixture(guaranteed,"")).lines().stream().allMatch(l->(enemy?l.state().self():l.state().opponent()).stages().get("spe")==-1),"Guaranteed accuracy retained a fabricated miss");
        }
        System.out.println("Accuracy/miss evidence regressions PASS: "+checks+" checks; stage/weather thresholds, miss guards, missing evidence and receipt withdrawal");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/accuracy_miss_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var thresholds=data.getAsJsonArray("thresholds");check(thresholds.size()==84,"Native threshold inventory changed");
        for(var value:thresholds){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var actual=accuracy(fixture(c,""));
            check(actual.known()&&actual.value()==row.get("threshold").getAsInt(),"Native accuracy differs: "+c.data()+" / "+actual+" / "+row);
        }
        var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==100,"Native exchange inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var result=evaluate(fixture(c,c.data().has("protect")&&c.b("protect")?"protect":""));
            if(c.s("move").equals("uturn")){
                String reason=c.b("enemy")?"OPPONENT_U_TURN_PARTY_HIDDEN":"U_TURN_REPLACEMENT_CHOICE_NOT_DETERMINED";
                check(!result.utility().known()&&result.unknown().contains(reason),"Unproved pivot replacement gained scope: "+result.unknown());
                if(row.get("ownMiss").getAsBoolean())check(result.lines().stream().anyMatch(line->matches(line,row)),"Native pivot miss failed to preserve the outgoing actor/state");
                continue;
            }
            check(result.utility().known()&&result.lines().stream().anyMatch(line->matches(line,row)),"Native hit/miss exchange excluded: "+c.data()+" / "+row+" / "+result);
        }
        System.out.println("Accuracy/miss native differential PASS: 84 thresholds, 100 queued exchanges; exact integer thresholds, early blocking, paralysis, priority, miss follow-ups and both roles");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(!line.utility().known()||utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
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
