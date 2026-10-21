package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** End-of-turn outcomes retain explicit public/own provenance and party uncertainty. */
final class ResidualRecoveryRegression {
    private static final String SOURCE="declared native residual/recovery conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case base(){return change(change(change(change(shield(),"move","bulkup"),"item","leftovers"),"status","brn"),"targetAbility","runaway");}
    private static Case number(Case c,String key,int n){var d=c.data().deepCopy();d.addProperty(key,n);return new Case(d);}
    private static ShadowMechanicsEvaluation.Ranking evaluated(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking residual(Fixture f){return evaluated(f,f.execution());}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static ExecutionCoverage.Certificate receipt(Fixture f){return f.execution().residualCoverage(f.profiles().get(f.config().user()),user(f));}
    static void run(){
        for(boolean enemy:List.of(false,true)){
            Case c=enemy?enemy(base()):base();var fresh=fixture(c,"");var result=residual(fresh);
            check(receipt(fresh).supported()&&result.utility().known(),"Proven residual remains UNKNOWN: "+result.unknown());
            check(result.lines().stream().allMatch(l->(enemy?l.state().opponent():l.state().self()).hp().value().equals(new Interval(.5,.5))),"Burn/Leftovers order or rounding differs");
            check(!evaluate(fresh).utility().known(),"Pre-residual initial-status contract silently changed");
            var cap=residual(fixture(number(c,"userHp",1000),""));
            check(cap.lines().stream().allMatch(l->Math.abs((enemy?l.state().opponent():l.state().self()).hp().value().low()-.938)<1e-10),"Recovery cap applied after burn");
            var saved=residual(fixture(number(c,"userHp",1),""));
            check(saved.utility().known()&&saved.lines().stream().allMatch(l->(enemy?l.state().opponent():l.state().self()).hp().value().low()>0),"Leftovers failed to precede fatal burn");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","setup-scope",
                    "target-max","bounded-target-max","status","suppression","ambiguous","forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!residual(fixture(c,missing)).utility().known(),"Residual bypassed evidence: "+missing);
            check(residual(fixture(c,"entry-scope")).utility().known(),"Surviving residual required unrelated reserve entry");
            check(!evaluated(fresh,null).utility().known(),"Unattested residual became known");
            check(!evaluated(fixture(change(c,"status","none"),""),null).utility().known(),"Healthy action bypassed residual attestation");
            String action="move:"+c.action();var turn=new ShadowMechanicsEvaluation.Turn(action,c.reply(),null);
            var future=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(turn,turn)),SOURCE);
            var twoTurns=ShadowMechanicsEvaluation.compareEndOfTurn(fresh.snapshot(),fresh.mechanics(),fresh.profiles(),List.of(future),
                    known(false),Critical.BOTH,action,fresh.execution()).rankings().getFirst();
            check(!twoTurns.utility().known()&&twoTurns.unknown().contains("FUTURE_FIELD_AND_VOLATILE_DURATION_UNRESOLVED"),"Completed residual inferred future effect duration/request");
            for(String status:List.of("tox","slp","frz"))check(!residual(fixture(change(c,"status",status),"")).utility().known(),"Unproved counter/duration/recovery certified: "+status);
            check(!residual(fixture(change(change(c,"status","tox"),"ability","magicguard"),"")).utility().known(),"Magic Guard guessed a Toxic counter");
            var guarded=residual(fixture(change(c,"ability","magicguard"),""));
            check(guarded.utility().known()&&guarded.lines().stream().allMatch(l->Math.abs((enemy?l.state().opponent():l.state().self()).hp().value().low()-.562)<1e-10),"Magic Guard suppressed healing or allowed burn");
            var room=c.data().deepCopy();room.addProperty("room",true);
            check(residual(fixture(new Case(room),"")).lines().stream().allMatch(l->Math.abs((enemy?l.state().opponent():l.state().self()).hp().value().low()-.438)<1e-10),"Magic Room failed to suppress Leftovers");
            for(String weather:List.of("snow","hail"))check(!residual(fixture(change(c,"weather",weather),"")).utility().known(),"Unsupported weather residual became known");
            var uncertain=change(c,"targetStatus","psn");
            check(!residual(fixture(uncertain,"",known(new Interval(.001,.2)))).utility().known(),"Poison survival threshold certified");
            check(!residual(fixture(c,"",known(new Interval(.5001,.5001)))).utility().known(),"Fractional point HP outside the native integer domain became known");
            var expired=fixture(c,"");var cert=receipt(expired);expired.owner().endChoice();
            check((enemy||!cert.supported())&&!residual(expired).utility().known(),"Expired own receipt retained residual comparison");
            var source=fixture(c,"");var sourceCert=receipt(source);source.current().set(false);
            check(!sourceCert.supported()&&!residual(source).utility().known(),"Expired source retained residual");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{residual(field);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field retained residual");
            var closing=fixture(c,"");var expiring=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=evaluated(closing,expiring);
            check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt leaked residual states");
        }
        var fatal=number(change(base(),"status","psn"),"userHp",1);
        check(!residual(fixture(fatal,"")).utility().known(),"Empty own switch list proved party closure");
        var reserve=number(fatal,"ownBench",1);var continued=residual(fixture(reserve,""));
        check(continued.utility().known()&&continued.lines().stream().allMatch(l->l.state().forcedReplacement()&&!l.state().nextRequestKnown()
                &&l.state().self().hp().value().high()==0&&l.state().self().stages().values().stream().allMatch(n->n==0)),"Certified own reserve failed to retain pending replacement/cleared stages");
        for(String missing:List.of("reserve-binding","entry-scope"))check(!residual(fixture(reserve,missing)).utility().known(),"Fainting continuation bypassed reserve proof: "+missing);
        System.out.println("Residual/recovery evidence regressions PASS: "+checks+" checks; boundary, rounding, survival, provenance and receipt withdrawal");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/residual_recovery_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==228,"Native residual inventory changed");int bounded=0,unknown=0;
        var hidden=new HashMap<Boolean,ShadowMechanicsEvaluation.Ranking>();
        for(var value:rows){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));
            // Native ended/request/foeBench labels are assertions, never evidence.
            var result=residual(fixture(c,c.data().has("protect")&&c.b("protect")?"protect":""));
            boolean supported=!c.s("status").equals("tox")&&row.get("foeHp").getAsDouble()>0
                    &&(row.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
            if(supported){bounded++;check(result.utility().known()&&result.lines().stream().anyMatch(l->matches(l,row)),"Native end-of-turn excluded: "+c.data()+" / "+row+" / "+result);
                check(result.lines().stream().allMatch(l->!l.state().unresolved().contains("RESIDUALS_AND_REPLACEMENTS_UNMODELED")),"Completed residual mislabeled as pre-residual");
                if(row.get("selfHp").getAsDouble()==0)check(result.lines().stream().allMatch(l->l.state().forcedReplacement()&&!l.state().nextRequestKnown()),"Native replacement became an executable next request");
            }else{unknown++;check(!result.utility().known(),"Unproved duration/party closure became known: "+c.data());}
            if(c.s("id").equals("residual-terminal"))hidden.put(c.b("enemy"),result);
            if(c.s("id").equals("residual-hidden-bench"))check(result.equals(hidden.get(c.b("enemy"))),"Hidden opponent reserves changed evaluation");
        }
        System.out.println("Residual/recovery native differential PASS: 228 queued outcomes; "+bounded+" bounded, "+unknown+" UNKNOWN; native HP/status/stages/utility, both roles and hidden-party isolation");
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
