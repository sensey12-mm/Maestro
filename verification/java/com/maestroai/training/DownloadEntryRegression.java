package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PivotOutcomeRegression.*;

/** Declared public stat bounds and owner entries, never hidden opponent builds. */
final class DownloadEntryRegression {
    private static final String SOURCE="declared native Download entry conditions";
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case change(Case c,String k,Object v){var d=c.data().deepCopy();if(v instanceof String s)d.addProperty(k,s);else if(v instanceof Integer n)d.addProperty(k,n);else if(v instanceof Boolean b)d.addProperty(k,b);else d.add(k,(JsonElement)v);return new Case(d);}
    private static Case base(){return change(ordinary(),"reserveAbility","download");}
    private static State root(Fixture f){return PublicStateSearchAdapter.project(f.snapshot(),f.mechanics()).states().get("root");}
    private static Actor arriving(Fixture f){var facts=f.mechanics().transitions().context().switchTargets().get("reserve0").facts();var stages=new TreeMap<String,Integer>();STATS.forEach(k->stages.put(k,0));return new Actor("reserve0",facts,known(new Interval(1,1)),stages,known("none"));}
    private static Known<Map<String,Integer>> boost(Fixture f){return f.execution().entryAbilityBoosts(f.profiles().get("reserve0"),arriving(f),f.profiles().get("opponent"),root(f).opponent());}
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean switching,boolean end,ScopedExecution execution){
        String action=switching?"switch:reserve0":"move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return (end?ShadowMechanicsEvaluation.compareEndOfTurn(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)
                :ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution)).rankings().getFirst();
    }
    private static ShadowMechanicsEvaluation.Ranking rank(Fixture f,boolean switching,boolean end){return rank(f,switching,end,f.execution());}
    static void run(){
        for(boolean switching:List.of(false,true)){
            var c=base();var f=fixture(c,"");var result=rank(f,switching,false);
            check(result.utility().known()&&result.lines().stream().allMatch(l->l.state().self().id().equals("reserve0")&&l.state().self().stages().get("spa")==1),"Positive native Download tie lost entry support");
            check(!f.execution().profile(f.profiles().get("reserve0"),true).ordinaryEffects().known(),"Raw reserve profile certified an ordinary future actor without entry evidence");
            for(String missing:List.of("own-item","opponent-item","opponent-ability","entry-scope","reserve-binding","reserve-profile","reserve-target","foreign-reserve","foreign-profile","stale-reserve","reserve-types","reserve-max","reserve-hp","reserve-item","reserve-ability","reserve-status","partial-hazards","entry-damage","entry-speed","foe-def","foe-spd","foe-def-stage","foe-spd-stage","suppression","ambiguous","wonder-room")){
                check(!rank(fixture(c,missing),switching,false).utility().known(),"Download entry bypassed proof: "+missing);
            }
            var noChoice=fixture(change(c,"ownBench",2),"");check(rank(noChoice,switching,false).utility().known()==switching,"Download guessed a pivot replacement or rejected an explicit legal switch");
            var fast=change(change(change(change(c,"targetDefense",150),"targetSpecialDefense",220),"userSpeed",100),"targetSpeed",300);
            var current=rank(fixture(fast,""),switching,false);
            check(current.utility().known()&&current.lines().stream().allMatch(l->l.state().self().stages().get(switching?"atk":"spa")==1),"Download used root stages instead of the entry-time defender stages");
            var missingSpd=fixture(c,"foe-spd");var cert=missingSpd.execution().entryCoverage(root(missingSpd).selfHazards(),known(1000),missingSpd.profiles().get("reserve0"));
            check(cert.supported()&&!boost(missingSpd).known(),"Entry HP/speed proof silently certified a Download stat selection");
            var source=fixture(c,"");var receipt=source.execution().entryCoverage(root(source).selfHazards(),known(1000),source.profiles().get("reserve0"));source.current().set(false);
            check(!receipt.supported()&&!rank(source,switching,false).utility().known()&&!boost(source).known(),"Expired source retained entry boost");
            var own=fixture(c,"");own.transport().endChoice();check(!rank(own,switching,false).utility().known()&&!boost(own).known(),"Expired own receipt retained Download");
            var field=fixture(c,"");field.fields().reset();boolean stale=false;try{rank(field,switching,false);}catch(IllegalArgumentException e){stale=true;}check(stale,"Expired public entry retained Download");
            var closing=fixture(c,"");var armed=new java.util.concurrent.atomic.AtomicBoolean();var changed=new java.util.concurrent.atomic.AtomicBoolean();
            Map<String,Profile> expiring=new AbstractMap<>(){
                @Override public Set<Map.Entry<String,Profile>> entrySet(){return closing.profiles().entrySet();}
                @Override public Profile get(Object key){if(key.equals("reserve0"))armed.set(true);if(key.equals("opponent")&&armed.get()){changed.set(true);closing.current().set(false);}return closing.profiles().get(key);}
            };
            var fading=new Fixture(closing.config(),closing.snapshot(),closing.mechanics(),expiring,closing.execution(),closing.transport(),closing.current(),closing.fields());
            var withdrawn=rank(fading,switching,false);check(changed.get()&&!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().self().id().equals("self")),"Closing source receipt leaked a Download entry state");
            for(int hp:List.of(125,126)){var h=change(c,"reserveHp",hp);h.data().getAsJsonObject("hazards").addProperty("stealthrock",1);var out=rank(fixture(h,""),switching,false);check(out.utility().known()==(hp==126),"Download crossed an unproved or lethal entry boundary");}
        }
        for(var bounds:List.of(new int[]{200,250,100,200,1},new int[]{100,199,200,250,2},new int[]{100,200,200,250,0},new int[]{100,200,100,200,0},new int[]{200,200,200,200,1})){
            var c=base();for(int i=0;i<4;i++)c=change(c,List.of("defLow","defHigh","spdLow","spdHigh").get(i),bounds[i]);var actual=boost(fixture(c,""));
            check(actual.known()==(bounds[4]!=0)&&(!actual.known()||actual.value().equals(Map.of(bounds[4]==1?"spa":"atk",1))),"Overlapping or tie-touching public stat bounds were resolved incorrectly");
        }
        var absent=fixture(change(base(),"targetHp",0),"");check(!boost(absent).known(),"Absent foe certified Download or party continuation");
        var uncertain=fixture(base(),"");var foe=root(uncertain).opponent();var maybe=new Actor(foe.id(),foe.facts(),known(new Interval(0,1)),foe.stages(),foe.status());
        check(!uncertain.execution().entryAbilityBoosts(uncertain.profiles().get("reserve0"),arriving(uncertain),uncertain.profiles().get("opponent"),maybe).known(),"Uncertain foe participation certified Download");
        check(!uncertain.execution().entryAbilityBoosts(uncertain.profiles().get("reserve0"),arriving(uncertain),null,foe).known(),"Missing defender profile certified Download");
        for(String ability:List.of("furcoat","neutralizinggas","asone"))check(!boost(fixture(change(base(),"targetAbility",ability),"")).known(),"Unsupported modifier or suppression gained Download scope");
        System.out.println("Download entry evidence regressions PASS: "+checks+" checks; public intervals, tie boundaries, current stages, survival and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        var js=Files.readString(Path.of("verification/download_entry_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+js+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==708,"Native Download inventory changed");int entries=0,bounded=0,unknown=0;var hidden=new HashMap<String,ShadowMechanicsEvaluation.Ranking>();
        for(var value:rows){var row=value.getAsJsonObject();var nativeCase=new Case(row.getAsJsonObject("config"));
            // Both native actor roles are replayed as DECLARED owner-side
            // counterfactuals. They do not certify an actual opponent reserve.
            var c=change(nativeCase,"enemy",false);boolean switching=c.s("mode").equals("switch");
            String missing=c.data().has("wonderRoom")&&c.b("wonderRoom")?"wonder-room":"";var f=fixture(c,missing);
            boolean unsupported=Set.of("unsupported-ability","unsupported-item","wonder-room").contains(c.s("id"));
            if(!row.get("activation").isJsonNull()&&!unsupported&&row.getAsJsonObject("activation").get("sameFoe").getAsBoolean()){var a=row.getAsJsonObject("activation");check(!row.get("entry").isJsonNull()&&row.getAsJsonObject("entry").get("hp").getAsInt()>0,"Native Download preceded entry survival");
                var defender=root(f).opponent();var stages=new TreeMap<>(defender.stages());int defStage=c.n("targetDefStage"),spdStage=c.n("targetSpdStage");
                if(!switching&&c.n("targetSpeed")>c.n("userSpeed")){if(c.s("reply").equals("bulkup"))defStage=Math.min(6,defStage+1);if(c.s("reply").equals("closecombat")){defStage=Math.max(-6,defStage-1);spdStage=Math.max(-6,spdStage-1);}}
                stages.put("def",defStage);stages.put("spd",spdStage);defender=new Actor(defender.id(),defender.facts(),known(new Interval(1,1)),stages,defender.status());
                var actual=f.execution().entryAbilityBoosts(f.profiles().get("reserve0"),arriving(f),f.profiles().get("opponent"),defender);
                int def=a.get("def").getAsInt(),spd=a.get("spd").getAsInt();var expected=def>0&&def>=spd?Map.of("spa",1):spd>0?Map.of("atk",1):Map.<String,Integer>of();
                check(actual.known()&&actual.value().equals(expected),"Native stage-adjusted Download comparison differs: "+c.data()+" / "+a+" / "+actual);entries++;
            }
            if(c.n("ownBench")==0&&switching)throw new AssertionError("Invalid native switch inventory");
            for(boolean end:List.of(false,true)){
                // With no own legal switch, pivot remains an abstention. Do not
                // fabricate a switch plan that the root request never supplied.
                var actual=rank(f,switching,end);var expected=row.getAsJsonObject(end?"post":"pre");
                boolean entered=!row.get("entry").isJsonNull()&&row.getAsJsonObject("entry").get("hp").getAsInt()>0;
                boolean survived=expected.get("selfHp").getAsDouble()>0&&expected.get("foeHp").getAsDouble()>0;
                boolean supported=!unsupported&&(switching||c.n("ownBench")==1)&&entered&&survived;
                if(c.s("mode").equals("pivot")&&c.n("targetDefense")<100)supported=false; // unsupported/KO-crossing damage domain
                if(end&&Set.of("tox","slp","frz").contains(expected.get("selfStatus").getAsString()))supported=false;
                if(end&&!c.s("weather").equals("none")&&Set.of("airlock","cloudnine").contains(c.s("targetAbility")))supported=false;
                if(c.s("id").equals("blocked")&&c.s("mode").equals("pivot"))supported=true;
                if(c.s("id").equals("queued-stage")&&c.s("reply").equals("ironhead")&&c.s("mode").equals("pivot"))supported=true; // native flinch is a bounded no-entry branch
                if(c.s("id").equals("recoil-boundary")&&c.n("hp")==100&&c.s("mode").equals("pivot"))supported=true;
                if(supported){bounded++;check(actual.utility().known()&&actual.lines().stream().anyMatch(l->matches(l,expected)),"Native Download composite excluded: "+c.data()+" / end="+end+" / "+expected+" / "+actual);}
                else{unknown++;check(!actual.utility().known(),"Unproved Download entry/continuation gained scope: "+c.data()+" / end="+end+" / "+actual);}
                if(c.s("id").equals("party")){String key=nativeCase.b("enemy")+":"+switching+":"+end+":"+c.n("ownBench")+":"+c.n("targetHp");if(c.n("foeBench")==0)hidden.put(key,actual);else check(actual.equals(hidden.get(key)),"Hidden opponent party changed Download forecast");}
            }
            if(nativeCase.b("enemy")&&c.s("mode").equals("pivot")){
                var opponent=rank(fixture(nativeCase,missing),false,false);
                check(!opponent.utility().known()||c.b("protect")&&opponent.lines().stream().allMatch(l->l.state().self().id().equals("self")&&l.state().opponent().id().equals("opponent")),"Actual hidden opponent pivot replacement was selected");
            }
        }
        System.out.println("Download entry native differential PASS: 708 queued entries across both native roles; "+entries+" owner-counterfactual ability comparisons; "+bounded+" bounded / "+unknown+" UNKNOWN at two boundaries; native floors/ties, hazard ordering, stages, modifiers and party privacy");
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
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));PivotOutcomeRegression.differential(engine);ToxicEntryRegression.differential(engine);differential(engine);
        }
    }
}
