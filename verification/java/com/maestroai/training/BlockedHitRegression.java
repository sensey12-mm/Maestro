package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Public, explicitly conditional fixture states; no real opponent internals or runtime claim. */
final class BlockedHitRegression {
    private static int checks;
    private static final String SOURCE="explicit native-oracle synthetic conditions";
    private static final Known<Boolean> YES=Known.of(true,SOURCE);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static <T> Known<T> known(T value){return Known.of(value,SOURCE);}
    private record Case(String id,String action,String reply,String protect,String selfType,String foeType,
                        boolean boosted,String item) {}
    private record Fixture(Case config,BattleSnapshot snapshot,PublicMechanics mechanics,Map<String,Profile> profiles,
                           ScopedExecution execution,PublicFieldEvidence fields,AtomicBoolean current) {}
    private static Case ordinary(String action,String protect,String foeType){
        return new Case(action+":"+protect+":"+foeType,action,"bulkup",protect,"normal",foeType,false,"none");
    }
    private static Map<String,Integer> stages(boolean boosted){
        var values=new TreeMap<String,Integer>();STATS.forEach(k->values.put(k,0));
        if(boosted){values.put("atk",2);values.put("def",2);}return values;
    }
    private static BattleSnapshot.Combatant facts(String type,double hp,Map<String,Integer> stages,List<String> revealed){
        return new BattleSnapshot.Combatant("Mew",hp,List.of(type),stages,revealed,null,null,false);
    }
    private static Profile profile(String id,String type,int speed){
        var stats=new TreeMap<String,Known<IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))stats.put(stat,known(IntRange.exact(stat.equals("spe")?speed:220)));
        return new Profile(id,known(100),known(IntRange.exact(1000)),stats,known(List.of(type)),YES);
    }
    private static OwnRequestTransport.Lease expiredOwnReceipt(){
        Object battle=new Object(),actor=new Object(),first=new Object(),second=new Object();
        var id=new UUID(0,43);var ledger=new OwnRequestTransport();ledger.owner(battle,actor,"p1");
        var pokemon=new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Own","500/1000",true,
                "runaway",List.of("normal")),100,Map.of("atk",220,"def",220,"spa",220,"spd",220,"spe",300),
                "none",List.of("normal"),"",false);
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(pokemon));
        ledger.dispatched(battle,actor,"p1",first,()->request);ledger.turnDispatched(battle,1);ledger.turnFinished(battle,1);
        ledger.dispatched(battle,actor,"p1",second,()->request);ledger.turnDispatched(battle,2);
        var lease=ledger.beginChoice(battle,actor,second,id,false,()->true);
        check(lease!=null&&lease.current(),"Own request expiry fixture did not begin with a fresh lease");
        ledger.endChoice();return lease;
    }
    private static Fixture fixture(Case c,String missing){
        var protocol=new PublicProtocolEvidence();
        String block=c.protect().equals("none")?"":"\n|-singleturn|"+(c.protect().equals("self")?"p1a: Own":"p2a: Foe")+"|Protect";
        protocol.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: Own|Mew, L100|50/100\n|switch|p2a: Foe|Mew, L100|100/100\n"
                +"|-enditem|p2a: Foe|Sitrus Berry\n"
                +(missing.equals("opponent-ability")?"":"|-ability|p2a: Foe|Run Away\n")
                +"|turn|1\n|turn|2"+block));
        var provider=new PublicMechanicsEvidence(id->null);provider.update(protocol);
        var fields=new PublicFieldEvidence();var field=fields.update(protocol,provider);
        var ownSeen=provider.observation("p1a");
        var foeSeen=provider.observation("p2a");
        var current=new AtomicBoolean(true);
        var scopes=ExecutionCoverage.Scopes.receipt(op->YES,current::get);
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(ownSeen,missing.equals("own-item")?Known.unknown():known(c.item()),known("runaway"),YES,
                        missing.equals("expired-own-request")?expiredOwnReceipt():null),
                "opponent",new ScopedExecution.Binding(foeSeen,foeSeen.item(),foeSeen.ability()),
                "reserve",new ScopedExecution.Binding(null,known("none"),known("runaway"),YES)),scopes);
        var own=facts(c.selfType(),.5,stages(false),List.of());
        var foe=facts(c.foeType(),1,stages(c.boosted()),List.of(c.reply()));
        var reserve=facts("normal",1,stages(false),List.of());
        var snapshot=new BattleSnapshot(2,own,foe,List.of(reserve),List.of(),List.of(c.action()),List.of("reserve"),Map.of(),false,null);
        var hazards=new Hazards(Map.of(),true,SOURCE);
        var context=new PublicTransitionState.Context(known(new Interval(.5,.5)),known(new Interval(1,1)),
                known("none"),known("none"),hazards,hazards,
                Map.of("reserve",new SwitchTarget(reserve,known("none"),known(new Interval(0,0)),known(0))),Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),stages(c.boosted()),Set.of(),false,false,false,0,new Evidence(context,Map.of(),Map.of()));
        Profile target=profile("opponent",c.foeType(),100);
        if(missing.equals("target-types"))target=new Profile(target.actor(),target.level(),target.maxHp(),target.stats(),Known.unknown(),YES);
        return new Fixture(c,snapshot,mechanics,Map.of("self",profile("self",c.selfType(),300),
                "opponent",target,"reserve",profile("reserve","normal",220)),execution,fields,current);
    }
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f){
        var action="move:"+f.config().action();
        var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(
                new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),
                known(false),Critical.BOTH,action,f.execution()).rankings().getFirst();
    }
    static void run(){
        for(Case c:List.of(ordinary("closecombat","opponent","normal"),ordinary("closecombat","none","ghost"),
                ordinary("drainingkiss","opponent","normal"),ordinary("uturn","opponent","normal"),
                ordinary("rocktomb","opponent","normal"),ordinary("icebeam","opponent","normal"))){
            var result=evaluate(fixture(c,""));
            check(result.utility().known(),"Certified blocked hit became UNKNOWN: "+c.id()+" "+result.unknown());
            check(result.lines().size()==1,"Blocked hit forked a secondary: "+c.id());
            for(var line:result.lines()){
                check(line.state().self().id().equals("self")&&line.state().self().hp().value().equals(new Interval(.5,.5)),
                        "Blocked hit healed, recoiled or pivoted: "+c.id());
                check(line.state().self().stages().get("def")==0&&line.state().self().stages().get("spd")==0,
                        "Blocked Close Combat applied self drops: "+c.id());
            }
        }
        for(String missing:List.of("own-item","opponent-ability","target-types","expired-own-request")){
            var result=evaluate(fixture(ordinary("closecombat","opponent","normal"),missing));
            check(!result.utility().known(),"Protect bypassed missing evidence: "+missing);
        }
        var stale=fixture(ordinary("closecombat","opponent","normal"),"");stale.current().set(false);
        var withdrawn=evaluate(stale);
        check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().self().stages().get("def")==0),
                "Expired source receipt certified blocked follow-ups");
        var expired=fixture(ordinary("closecombat","opponent","normal"),"");expired.fields().reset();
        boolean rejected=false;try{evaluate(expired);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Expired field receipt certified a blocked hit");
        System.out.println("Blocked-hit evidence regressions PASS: "+checks+" checks; missing modifiers/types and expired own/source/field receipts abstain");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/blocked_hit_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source
                +";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==18,"Native blocked-hit scenario inventory changed");
        for(var value:rows){
            var row=value.getAsJsonObject();var raw=row.getAsJsonObject("config");
            var c=new Case(raw.get("id").getAsString(),raw.get("action").getAsString(),raw.get("reply").getAsString(),
                    raw.get("protect").getAsString(),raw.get("selfType").getAsString(),raw.get("foeType").getAsString(),
                    raw.get("boosted").getAsBoolean(),raw.get("item").getAsString().isEmpty()?"none":raw.get("item").getAsString());
            var ranking=evaluate(fixture(c,""));
            check(ranking.utility().known(),"Native exchange left unbounded: "+c.id()+" "+ranking.unknown());
            check(ranking.lines().stream().anyMatch(line->matches(line.state(),row)),
                    "Shadow follow-ups exclude the native result: "+c.id()+" "+ranking.lines()+" / "+row);
        }
        System.out.println("Blocked-hit native differential PASS: 18 exchanges; Protect/immunity, successful drain/pivot, pre-hit theft, secondary suppression and Life Orb");
    }
    private static boolean matches(State state,JsonObject nativeResult){
        if(!state.self().id().equals(nativeResult.get("active").getAsString()))return false;
        for(String role:List.of("self","foe")){
            Actor actor=role.equals("self")?state.self():state.opponent();
            double hp=nativeResult.get(role+"Hp").getAsDouble();var range=actor.hp().value();
            if(hp<range.low()-1e-10||hp>range.high()+1e-10)return false;
            if(!actor.status().known()||!actor.status().value().equals(nativeResult.get(role+"Status").getAsString()))return false;
            for(var entry:nativeResult.getAsJsonObject(role+"Stages").entrySet())
                if(!Objects.equals(actor.stages().get(entry.getKey()),entry.getValue().getAsInt()))return false;
        }
        return true;
    }
}
