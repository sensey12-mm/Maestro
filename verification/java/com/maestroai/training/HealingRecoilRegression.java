package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Conditional public-only fixtures; synthetic HP/stat ranges are not observed opponent builds. */
final class HealingRecoilRegression {
    private static final String SOURCE="explicit healing/recoil native-oracle conditions";
    private static final Known<Boolean> YES=known(true);
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private record Case(String id,int hp,int targetHp,int maxHp,String item,String ability,
                        boolean enemy,boolean magicRoom,boolean protect) {
        String action(){return enemy?"bulkup":"drainingkiss";}
        String reply(){return enemy?"drainingkiss":"bulkup";}
    }
    private record Fixture(Case config,BattleSnapshot snapshot,PublicMechanics mechanics,
                           Map<String,Profile> profiles,ScopedExecution execution,
                           AtomicBoolean current,PublicFieldEvidence fields) {}
    private static Case own(int hp,int targetHp,String item){
        return new Case("own",hp,targetHp,1000,item,"runaway",false,false,false);
    }
    private static Map<String,Integer> stages(){
        var result=new TreeMap<String,Integer>();STATS.forEach(k->result.put(k,0));return result;
    }
    private static Profile profile(String role,int max,int speed){
        var stats=new TreeMap<String,Known<IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))
            stats.put(stat,known(IntRange.exact(stat.equals("spe")?speed:220)));
        return new Profile(role,known(100),known(IntRange.exact(max)),stats,known(List.of("normal")),YES);
    }
    private static OwnRequestTransport.Lease expiredOwnReceipt(){
        Object battle=new Object(),actor=new Object(),first=new Object(),second=new Object();
        var id=new UUID(0,44);var ledger=new OwnRequestTransport();ledger.owner(battle,actor,"p1");
        var pokemon=new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Own","980/1000",true,
                "runaway",List.of("normal")),100,Map.of("atk",220,"def",220,"spa",220,"spd",220,"spe",300),
                "lifeorb",List.of("normal"),"",false);
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(pokemon));
        ledger.dispatched(battle,actor,"p1",first,()->request);ledger.turnDispatched(battle,1);ledger.turnFinished(battle,1);
        ledger.dispatched(battle,actor,"p1",second,()->request);ledger.turnDispatched(battle,2);
        var lease=ledger.beginChoice(battle,actor,second,id,false,()->true);
        check(lease!=null&&lease.current(),"Expiry fixture did not start with a current request");
        ledger.endChoice();return lease;
    }
    private static Fixture fixture(Case c,String missing,Interval userHp,Interval targetHp){
        var protocol=new PublicProtocolEvidence();
        String publicItem=c.enemy()&&!c.item().equals("none")?"|-item|p2a: Foe|Life Orb\n":"|-enditem|p2a: Foe|Sitrus Berry\n";
        protocol.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L100|100/100\n"
                +(missing.equals("opponent-item")?"":publicItem)
                +(missing.equals("opponent-ability")?"":"|-ability|p2a: Foe|"+(c.enemy()&&c.ability().equals("magicguard")?"Magic Guard":"Run Away")+"\n")
                +"|turn|1\n|turn|2"
                +(c.magicRoom()?"\n|-fieldstart|move: Magic Room":"")
                +(c.protect()?"\n|-singleturn|"+(c.enemy()?"p1a: Own":"p2a: Foe")+"|Protect":"")));
        var provider=new PublicMechanicsEvidence(id->null);provider.update(protocol);
        var fields=new PublicFieldEvidence();var field=fields.update(protocol,provider);
        var foeSeen=provider.observation("p2a");var current=new AtomicBoolean(true);
        var scopes=ExecutionCoverage.Scopes.receipt(op->missing.equals("damage-scope")&&op==ExecutionCoverage.Operation.DAMAGE
                ?Known.unknown():YES,current::get);
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(provider.observation("p1a"),
                        missing.equals("own-item")?Known.unknown():known(c.enemy()?"none":c.item()),
                        known(c.enemy()?"runaway":c.ability()),YES,missing.equals("expired-own")?expiredOwnReceipt():null),
                "opponent",new ScopedExecution.Binding(foeSeen,foeSeen.item(),foeSeen.ability())),scopes);
        Interval selfHp=c.enemy()?targetHp:userHp,foeHp=c.enemy()?userHp:targetHp;
        var own=new BattleSnapshot.Combatant("Mew",selfHp.low()==selfHp.high()?selfHp.low():BattleSnapshot.UNKNOWN_HP_FRACTION,
                List.of("normal"),stages(),List.of(),null,null,false);
        var foe=new BattleSnapshot.Combatant("Mew",foeHp.low()==foeHp.high()?foeHp.low():BattleSnapshot.UNKNOWN_HP_FRACTION,
                List.of("normal"),stages(),List.of(c.reply()),null,null,false);
        var snapshot=new BattleSnapshot(2,own,foe,List.of(),List.of(),List.of(c.action()),List.of(),Map.of(),false,null);
        var hazards=new Hazards(Map.of(),true,SOURCE);
        var context=new PublicTransitionState.Context(known(selfHp),known(foeHp),known("none"),known("none"),
                hazards,hazards,Map.of(),Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),stages(),Set.of(),false,false,false,0,new Evidence(context,Map.of(),Map.of()));
        var profiles=new HashMap<String,Profile>();
        profiles.put("self",profile("self",c.enemy()?1000:c.maxHp(),c.enemy()?100:300));
        profiles.put("opponent",profile("opponent",c.enemy()?c.maxHp():1000,c.enemy()?300:100));
        String user=c.enemy()?"opponent":"self",target=c.enemy()?"self":"opponent";
        if(missing.equals("user-max")||missing.equals("bounded-user-max")){
            Profile p=profiles.get(user);profiles.put(user,new Profile(user,p.level(),missing.equals("user-max")
                    ?Known.unknown():known(new IntRange(c.maxHp()-1,c.maxHp()+1)),p.stats(),p.types(),YES));
        }
        if(missing.equals("bounded-target-max")){
            Profile p=profiles.get(target);profiles.put(target,new Profile(target,p.level(),known(new IntRange(900,1100)),p.stats(),p.types(),YES));
        }
        return new Fixture(c,snapshot,mechanics,Map.copyOf(profiles),execution,current,fields);
    }
    private static Fixture fixture(Case c,String missing){
        return fixture(c,missing,new Interval(c.hp()/(double)c.maxHp(),c.hp()/(double)c.maxHp()),
                new Interval(c.targetHp()/1000.0,c.targetHp()/1000.0));
    }
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f){
        String action="move:"+f.config().action();
        var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(
                new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),
                Critical.BOTH,action,f.execution()).rankings().getFirst();
    }
    private static Interval userHp(ShadowMechanicsEvaluation.Line l,Case c){
        return (c.enemy()?l.state().opponent():l.state().self()).hp().value();
    }
    private static void exact(Case c,double expected){
        var result=evaluate(fixture(c,""));
        check(result.utility().known(),"Resolved healing/recoil became UNKNOWN: "+c+" "+result.unknown());
        for(var line:result.lines()){
            Interval hp=userHp(line,c);
            check(Math.abs(hp.low()-expected)<1e-10&&Math.abs(hp.high()-expected)<1e-10,
                    "Wrong healing/recoil boundary: "+c+" "+hp+" expected "+expected);
            double foeBefore=c.targetHp()/1000.0,ownBefore=c.hp()/(double)c.maxHp();
            double foeAfter=(c.enemy()?line.state().self():line.state().opponent()).hp().value().high();
            double utility=c.enemy()?-(foeBefore-foeAfter+expected-ownBefore):foeBefore-foeAfter+expected-ownBefore;
            check(utility>=line.utility().value().low()-1e-10&&utility<=line.utility().value().high()+1e-10,
                    "Resource-exchange utility excludes the final HP delta");
        }
    }
    static void run(){
        exact(own(1000,1000,"lifeorb"),.9);
        exact(own(980,1000,"lifeorb"),.9);
        exact(own(99,3,"lifeorb"),.001);
        exact(own(98,3,"lifeorb"),0);
        exact(own(500,3,"none"),.502);
        exact(own(500,2,"none"),.502);
        for(String missing:List.of("own-item","opponent-item","opponent-ability","user-max","bounded-user-max","damage-scope","expired-own")){
            var result=evaluate(fixture(own(980,1000,"lifeorb"),missing));
            check(!result.utility().known(),"Healing/recoil bypassed evidence: "+missing);
        }
        var expired=fixture(own(980,1000,"lifeorb"),"");expired.current().set(false);
        var withdrawn=evaluate(expired);
        check(!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->userHp(l,expired.config()).equals(new Interval(.98,.98))),
                "Expired source receipt retained healing/recoil forecast");
        var field=fixture(own(980,1000,"lifeorb"),"");field.fields().reset();
        boolean rejected=false;try{evaluate(field);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Expired field receipt retained healing/recoil");
        var uncertainExecution=evaluate(fixture(own(99,3,"lifeorb"),"",new Interval(0,.099),new Interval(.003,.003)));
        check(!uncertainExecution.utility().known(),"Possibly fainted user received certain drain/recoil");
        var uncertainTarget=evaluate(fixture(own(99,3,"lifeorb"),"",new Interval(.099,.099),new Interval(0,.003)));
        check(!uncertainTarget.utility().known(),"Possibly fainted target guaranteed a successful drain");
        var uncertainSurvival=evaluate(fixture(own(99,3,"lifeorb"),"",new Interval(.097,.099),new Interval(.003,.003)));
        check(!uncertainSurvival.utility().known(),"Unresolved recoil survival certified a continuation");
        var lethal=evaluate(fixture(own(1,1000,"lifeorb"),""));
        check(!lethal.utility().known()&&lethal.lines().stream().allMatch(l->userHp(l,own(1,1000,"lifeorb")).high()==0),
                "Lethal recoil revived its user or certified unresolved replacement/continuation");
        var bounded=evaluate(fixture(own(500,1000,"lifeorb"),"bounded-target-max"));
        check(bounded.utility().known()&&bounded.lines().stream().allMatch(l->userHp(l,own(500,1000,"lifeorb")).low()>.4
                &&userHp(l,own(500,1000,"lifeorb")).high()<.5),"Public max-HP intervals lost bounded healing/recoil");
        System.out.println("Healing/recoil evidence regressions PASS: "+checks+" checks; HP caps, rounding, survival, public bounds and receipt expiration");
    }
    static void differential(Context engine) throws Exception {
        var source=Files.readString(Path.of("verification/healing_recoil_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source
                +";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==19,"Healing/recoil native scenario inventory changed");
        for(var value:rows){
            var row=value.getAsJsonObject();var raw=row.getAsJsonObject("config");
            var c=new Case(raw.get("id").getAsString(),raw.get("hp").getAsInt(),raw.get("targetHp").getAsInt(),
                    raw.get("maxHp").getAsInt(),raw.get("item").getAsString().isEmpty()?"none":raw.get("item").getAsString(),
                    raw.get("ability").getAsString(),raw.get("enemy").getAsBoolean(),raw.get("magicRoom").getAsBoolean(),raw.get("protect").getAsBoolean());
            var result=evaluate(fixture(c,""));
            if(c.id().equals("orb-lethal")){
                check(!result.utility().known(),"Post-recoil continuation must remain UNKNOWN");
                check(result.lines().stream().allMatch(l->userHp(l,c).high()==0),"Lethal native recoil resurrected its user");
            }else{
                check(result.utility().known(),"Native healing/recoil left unbounded: "+c.id()+" "+result.unknown());
                check(result.lines().stream().anyMatch(l->matches(l.state(),row)),
                        "Shadow excludes native healing/recoil: "+c.id()+" "+result.lines()+" / "+row);
            }
        }
        // The same native observation must remain covered without exact opponent max HP.
        var bounded=own(500,1000,"lifeorb");var result=evaluate(fixture(bounded,"bounded-target-max"));
        var row=rows.get(2).getAsJsonObject();
        check(result.utility().known()&&result.lines().stream().anyMatch(l->matches(l.state(),row)),"Bounded public opponent HP excludes native exchange");
        System.out.println("Healing/recoil native differential PASS: 19 exchanges; HP caps, rounded drain, survival, Magic Guard/Room, Protect and public opponent bounds");
    }
    private static boolean matches(State state,JsonObject row){
        for(String role:List.of("self","foe")){
            Actor actor=role.equals("self")?state.self():state.opponent();
            double hp=row.get(role+"Hp").getAsDouble();var range=actor.hp().value();
            if(hp<range.low()-1e-10||hp>range.high()+1e-10)return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())
                if(!Objects.equals(actor.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }
        return true;
    }
}
