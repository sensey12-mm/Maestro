package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Configured abilities, native post-faint ordering and evidence boundaries. */
final class RosterAbilityRegression {
    private static final String SOURCE="declared native roster ability conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case number(Case c,String k,int n){var d=c.data().deepCopy();d.addProperty(k,n);return new Case(d);}
    private static Case base(String ability,boolean enemy){
        var c=number(number(change(change(change(shield(),"ability",ability),"targetAbility","runaway"),"item","none"),"userSpeed",301),"targetSpeed",201);
        return enemy?enemy(c):c;
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean end){return rank(f,end,f.execution());}
    static void run(){
        for(String ability:List.of("insomnia","asonespectrier"))for(boolean enemy:List.of(false,true)){
            var c=base(ability,enemy);var f=fixture(c,"");
            check(damage(f,Critical.BOTH).points().known()&&rank(f,true).utility().known(),"Awake supported roster ability excluded");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","order-scope","speed-scope","status","target-max","types","attack-stat","level","suppression","ambiguous","forged-opponent"))
                check(!rank(fixture(c,missing),true).utility().known(),"Roster ability bypassed evidence: "+missing);
            for(String status:List.of("slp","frz","tox"))for(boolean end:List.of(false,true))
                check(!rank(fixture(change(c,"status",status),""),end).utility().known(),"Ability invented current status recovery/duration: "+status);
            for(String status:List.of("par","brn","psn"))check(rank(fixture(change(c,"status",status),""),true).utility().known(),"Insomnia/As One incorrectly immunized another status: "+status);
            for(String unknown:List.of("asone","unnerve","asoneglastrier","neutralizinggas"))check(!rank(fixture(change(c,"ability",unknown),""),true).utility().known(),"Unproved form/ability gained roster coverage");
            var source=fixture(c,"");var cert=coverage(source);source.current().set(false);
            check(!cert.supported()&&!rank(source,true).utility().known(),"Expired source retained roster receipt");
            var expired=fixture(c,"");var old=coverage(expired);expired.owner().endChoice();
            check(!old.supported()&&!rank(expired,true).utility().known(),"Expired own request retained roster receipt");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,true);}catch(IllegalArgumentException rejected){stale=true;}check(stale,"Expired public actor retained roster forecast");
            var closing=fixture(c,"");var frame=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{if(op==ExecutionCoverage.Operation.DAMAGE)closing.current().set(false);return known(true);},closing.current()::get));
            var withdrawn=rank(closing,true,frame);check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt leaked projected ability states");
        }
        for(boolean enemy:List.of(false,true))for(int stage:List.of(-6,0,5,6)){
            var c=number(number(base("asonespectrier",enemy),"targetHp",1),"attackStage",stage);
            check(!rank(fixture(c,""),false).utility().known(),"As One inferred battle continuation after a KO");
            var reserve=fixture(number(c,"ownBench",1),"");var actual=rank(reserve,false);
            check(actual.utility().known()==enemy,"Opponent party closure confused with certified own reserve");
            if(enemy)check(actual.lines().stream().allMatch(l->l.state().opponent().stages().get("spa")==Math.min(6,stage+1)
                    &&l.state().self().stages().values().stream().allMatch(v->v==0)&&l.state().forcedReplacement()),"Post-faint boost/cap/replacement ordering differs");
            check(!rank(fixture(number(c,"ownBench",1),"reserve-binding"),false).utility().known(),"Missing reserve receipt certified As One continuation");
            // A damaging reply can already bound nonexecution; it must not
            // hide a missing As One guard behind setup's survival abstention.
            var trigger=number(change(base("asonespectrier",enemy),"replyMove","swift"),"ownBench",1);
            var uncertain=rank(fixture(trigger,"",known(new Interval(.001,.5))),false);
            check(!uncertain.utility().known()&&uncertain.unknown().contains("AS_ONE_KNOCKOUT_OR_USER_SURVIVAL_UNKNOWN"),"Possible target KO silently omitted As One trigger");
            var absent=rank(fixture(trigger,"",known(new Interval(0,.001))),false);
            check(!absent.utility().known()&&absent.unknown().contains("AS_ONE_KNOCKOUT_OR_USER_SURVIVAL_UNKNOWN"),"Possibly absent victim certified an As One knockout");
            var choices=rank(fixture(number(c,"ownBench",2),""),false);
            check(choices.utility().known()==enemy&&(!enemy||choices.lines().stream().allMatch(l->l.state().forcedReplacement()&&l.state().self().id().equals("self"))),"Multiple own reserves invented a replacement choice or lost continuation proof");
            var stale=fixture(number(c,"ownBench",1),"");stale.owner().endChoice();check(!rank(stale,false).utility().known(),"Expired reserve certified post-faint boost");
            var source=fixture(number(c,"ownBench",1),"");source.current().set(false);check(!rank(source,false).utility().known(),"Expired source certified post-faint boost");
        }
        for(String ability:List.of("insomnia","asonespectrier")){
            var c=number(number(number(change(base(ability,false),"replyMove","psyshock"),"userHp",1),"targetSpeed",401),"ownBench",1);
            var fainted=rank(fixture(c,""),false);check(fainted.utility().known()&&fainted.lines().stream().allMatch(l->l.state().forcedReplacement()
                    &&l.state().self().hp().value().high()==0&&l.state().self().stages().values().stream().allMatch(v->v==0)),"Direct KO skipped pending own replacement or let fainted ability act");
        }
        for(boolean enemy:List.of(false,true)){
            var c=base("insomnia",enemy);var f=fixture(number(c,"ownBench",1),"");var reserve=f.profiles().get("reserve0");
            var bindings=Map.of("reserve0",new ScopedExecution.Binding(null,known("none"),known("insomnia"),known(true),f.lease()));
            var scoped=new ScopedExecution(f.fields().update(f.protocol(),f.provider()),bindings,ExecutionCoverage.Scopes.receipt(op->known(true),f.current()::get));
            var hazards=new Hazards(Map.of("toxicspikes",1),true,SOURCE);
            check(!scoped.entryEffects(hazards,known(1000),reserve,known("slp")).unknown().isEmpty(),"Sleeping reserve invented Insomnia Update cure");
            var poison=scoped.entryEffects(hazards,known(1000),reserve,known("none"));check(poison.unknown().isEmpty()&&poison.status().value().equals("psn"),"Insomnia incorrectly blocked Toxic Spikes");
        }
        System.out.println("Configured roster ability evidence regressions PASS: "+checks+" checks; status recovery, capped post-KO boosts, party privacy and stale receipts");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/roster_ability_oracle.cjs"));
        var data=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        check(data.getAsJsonArray("roster").size()==6&&data.getAsJsonArray("immunity").size()==32,"Native roster/status inventory changed");
        int bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();var exchanges=data.getAsJsonArray("exchanges");check(exchanges.size()==304,"Native lifecycle inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,c.data().has("protect")&&c.b("protect")?"protect":"");
            for(boolean end:List.of(false,true)){
                var actual=rank(f,end);var expected=row.getAsJsonObject(end?"post":"pre");
                var pre=row.getAsJsonObject("pre");double userHp=pre.get(c.b("enemy")?"foeHp":"selfHp").getAsDouble(),targetHp=pre.get(c.b("enemy")?"selfHp":"foeHp").getAsDouble();
                boolean supported=!Set.of("slp","frz","tox").contains(c.s("status"))&&(end||!Set.of("brn","psn").contains(c.s("status"))&&!Set.of("brn","psn").contains(c.s("targetStatus")));
                if(c.s("ability").equals("asonespectrier")&&targetHp==0&&userHp>0)supported&=c.b("enemy")&&c.n("ownBench")>0;
                if(userHp==0&&targetHp>0&&c.s("item").equals("lifeorb"))supported&=!c.b("enemy")&&c.n("ownBench")>0;
                if(end)supported&=pre.get("foeHp").getAsDouble()>0&&(pre.get("selfHp").getAsDouble()>0||c.n("ownBench")>0)
                        &&expected.get("foeHp").getAsDouble()>0&&(expected.get("selfHp").getAsDouble()>0||c.n("ownBench")>0);
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native roster outcome excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);
                    if(expected.get("selfHp").getAsDouble()==0&&expected.get("foeHp").getAsDouble()>0&&c.n("ownBench")>0)
                        check(actual.lines().stream().allMatch(l->l.state().forcedReplacement()),"Native pending own replacement omitted after direct/residual/recoil KO");
                }
                else{unknown++;check(!actual.utility().known(),"Unproved status/party continuation gained scope: "+c.data()+" / end="+end+" / "+actual);}
                if(c.s("id").equals("move-ko")){String key=end+":"+c.b("enemy")+":"+c.s("item")+":"+c.n("userHp")+":"+c.n("attackStage")+":"+c.n("ownBench");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent bench affected As One forecast");}
            }
        }
        System.out.println("Configured roster ability native differential PASS: six roster definitions, 32 status/Update controls, 304 queued exchanges at two boundaries; "+bounded+" bounded / "+unknown+" UNKNOWN; both roles, recoil, stage caps, termination and hidden-party invariance");
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
            if(args.length>1&&args[1].equals("mechanics-only"))return;
            engine.eval("js","var rosterStream=new BS.BattleStream();rosterStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));");
            var modules=engine.eval("js","({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})");
            var resources=Path.of("src/main/resources/com/maestroai/training");var pins=new TreeMap<String,String>();
            for(String line:Files.readAllLines(resources.resolve("runtime-domain-components.tsv"))){var fields=line.split("\t");pins.put(fields[0],fields[1]);}
            var probe=engine.eval("js",Files.readString(resources.resolve("runtime-domain-probe.js")));
            var anchor=engine.eval("js",Files.readString(resources.resolve("runtime-source-anchor.js"))).execute(modules);
            MoveAttestationRegression.run(engine,probe,engine.eval("js","rosterStream.battle"),modules,anchor,pins);
        }
    }
}
