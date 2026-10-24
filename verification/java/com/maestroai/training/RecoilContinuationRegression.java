package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Native synthetic conditions only; the native opponent's bench never enters evaluation. */
final class RecoilContinuationRegression {
    private static final String SOURCE="declared recoil-continuation native conditions";
    private static final Known<Boolean> YES=known(true);
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private record Case(String id,String fast,String slow,int hp,int targetHp,int ownBench,boolean enemy,
                        String slowItem,String ability,boolean magicRoom,boolean protect,boolean rocks) {
        String action(){return enemy?slow:fast;}
        String reply(){return enemy?fast:slow;}
    }
    private record Fixture(Case config,BattleSnapshot snapshot,PublicMechanics mechanics,Map<String,Profile> profiles,
                           ScopedExecution execution,AtomicBoolean current,PublicFieldEvidence fields,OwnRequestTransport transport) {}
    private record Owner(OwnRequestTransport transport,OwnRequestTransport.Lease lease) {}
    private static Case own(String slow){return new Case(slow,"tackle",slow,1,1000,1,false,
            slow.equals("geomancy")?"powerherb":"none","runaway",false,false,false);}
    private static Map<String,Integer> stages(){var s=new TreeMap<String,Integer>();STATS.forEach(k->s.put(k,0));return s;}
    private static Profile profile(String role,int speed){
        var stats=new TreeMap<String,Known<IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))stats.put(stat,known(IntRange.exact(stat.equals("spe")?speed:220)));
        return new Profile(role,known(100),known(IntRange.exact(1000)),stats,known(List.of("normal")),YES);
    }
    private static BattleSnapshot.Combatant facts(double hp,List<String> moves){
        return new BattleSnapshot.Combatant("Mew",hp,List.of("normal"),stages(),moves,null,null,hp==0);
    }
    private static Owner owner(Case c){
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),first=new Object(),second=new Object();
        var active=new UUID(0,45);ledger.owner(battle,actor,"p1");
        var pokemon=new ArrayList<OwnRequestTransport.Pokemon>();
        for(int i=0;i<=c.ownBench();i++){
            String hp=(i==0?(c.enemy()?c.targetHp():c.hp()):1000)+"/1000";
            pokemon.add(new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(i==0?active:new UUID(0,45+i),
                    "p1: Own"+i,hp,i==0,i==0&&!c.enemy()?c.ability():"runaway",List.of("normal")),100,
                    Map.of("atk",220,"def",220,"spa",220,"spd",220,"spe",i==0?(c.enemy()?100:300):220),
                    i==0?(c.enemy()?c.slowItem():"lifeorb"):"none",List.of("normal"),"",false));
        }
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,pokemon);
        ledger.dispatched(battle,actor,"p1",first,()->request);ledger.turnDispatched(battle,1);ledger.turnFinished(battle,1);
        ledger.dispatched(battle,actor,"p1",second,()->request);ledger.turnDispatched(battle,2);
        var lease=ledger.beginChoice(battle,actor,second,active,false,()->true);
        if(!lease.current())throw new AssertionError("Owner fixture did not issue a real current lease");
        return new Owner(ledger,lease);
    }
    private static Fixture fixture(Case c,String missing){
        var owner=owner(c);
        var protocol=new PublicProtocolEvidence();
        String foeItem=c.enemy()?"lifeorb":c.slowItem();
        protocol.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L100|100/100\n"
                +"|switch|p2a: Foe|Mew, L100|100/100\n"
                +(missing.equals("opponent-ability")?"":"|-ability|p2a: Foe|Run Away\n")
                +(foeItem.equals("none")?"|-enditem|p2a: Foe|Sitrus Berry\n":"|-item|p2a: Foe|"+switch(foeItem){
                    case "lifeorb"->"Life Orb";case "powerherb"->"Power Herb";default->throw new AssertionError();}+"\n")
                +"|turn|1\n|turn|2"+(c.magicRoom()?"\n|-fieldstart|move: Magic Room":"")
                +(c.protect()?"\n|-singleturn|p2a: Foe|Protect":"")
                +(c.rocks()?"\n|-sidestart|p1: Own|Stealth Rock":"")));
        var provider=new PublicMechanicsEvidence(id->null);provider.update(protocol);
        var fields=new PublicFieldEvidence();var field=fields.update(protocol,provider);
        var current=new AtomicBoolean(true);
        var scopes=ExecutionCoverage.Scopes.receipt(op->missing.equals(op.name().toLowerCase(Locale.ROOT)+"-scope")?Known.unknown():YES,current::get);
        var bindings=new HashMap<String,ScopedExecution.Binding>();
        var own=provider.observation("p1a");var foe=provider.observation("p2a");
        bindings.put("self",new ScopedExecution.Binding(own,missing.equals("own-item")?Known.unknown():known(c.enemy()?c.slowItem():"lifeorb"),
                known(c.enemy()?"runaway":c.ability()),YES,owner.lease()));
        bindings.put("opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability()));
        double selfHp=(c.enemy()?c.targetHp():c.hp())/1000.0,foeHp=(c.enemy()?c.hp():c.targetHp())/1000.0;
        var self=facts(selfHp,List.of());var opponent=facts(foeHp,List.of(c.reply()));
        var targets=new HashMap<String,SwitchTarget>();var profiles=new HashMap<String,Profile>();
        profiles.put("self",profile("self",c.enemy()?100:300));profiles.put("opponent",profile("opponent",c.enemy()?300:100));
        var bench=new ArrayList<BattleSnapshot.Combatant>();var legal=new ArrayList<String>();
        for(int i=0;i<c.ownBench();i++){
            String id="reserve"+i;var reserve=facts(missing.equals("dead-reserve")?0:missing.equals("unknown-reserve-hp")?-1:1,List.of());
            bench.add(reserve);if(!reserve.fainted())legal.add(id);
            targets.put(id,new SwitchTarget(reserve,known("none"),known(new Interval(c.rocks()?.125:0,c.rocks()?.125:0)),known(0)));
            profiles.put(id,missing.equals("foreign-profile")?profile("opponent",220):profile(id,220));
            if(!missing.equals("reserve-binding"))bindings.put(id,new ScopedExecution.Binding(
                    missing.equals("foreign-reserve")?foe:null,known("none"),known("runaway"),
                    missing.equals("stale-reserve")?Known.unknown():YES,owner.lease()));
        }
        if(missing.equals("reserve-profile"))profiles.remove("reserve0");
        if(missing.equals("reserve-target"))targets.clear();
        boolean trapped=missing.equals("trapped");
        var snapshot=new BattleSnapshot(2,self,opponent,bench,List.of(),List.of(c.action()),trapped?List.of():legal,Map.of(),false,null);
        var hazards=new Hazards(c.rocks()?Map.of("stealthrock",1):Map.of(),true,SOURCE);
        var clear=new Hazards(Map.of(),true,SOURCE);
        var context=new PublicTransitionState.Context(known(new Interval(selfHp,selfHp)),known(new Interval(foeHp,foeHp)),
                known("none"),known("none"),hazards,clear,targets,Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),stages(),Set.of(),false,false,trapped,0,new Evidence(context,Map.of(),Map.of()));
        return new Fixture(c,snapshot,mechanics,Map.copyOf(profiles),new ScopedExecution(field,bindings,scopes),current,fields,owner.transport());
    }
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f){
        return evaluate(f,f.profiles());
    }
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f,Map<String,Profile> profiles){
        String action="move:"+f.config().action();
        var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),profiles,List.of(plan),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst();
    }
    static void run(){
        for(String move:List.of("bulkup","swordsdance","nastyplot","recover","geomancy","tackle","drainingkiss","uturn","closecombat")){
            var result=evaluate(fixture(own(move),""));
            check(result.utility().known(),"Certified own reserve did not allow recoil continuation: "+move+" "+result.unknown());
            for(var line:result.lines()){
                check(line.state().self().id().equals("self")&&line.state().self().hp().value().high()==0,
                        "Recoil KO revived or replaced the active before the queued action");
                check(line.state().forcedReplacement()&&!line.state().nextRequestKnown(),"Pending replacement became an executable future request");
                var stages=line.state().opponent().stages();
                int atk=move.equals("bulkup")?1:move.equals("swordsdance")?2:0;
                check(stages.get("atk")==atk,"Self-targeted setup was suppressed or no-target effect executed: "+move);
                if(move.equals("nastyplot")||move.equals("geomancy"))check(stages.get("spa")==2,"Queued special setup suppressed");
            }
        }
        for(String missing:List.of("own-item","opponent-ability","damage-scope","entry-scope","setup-scope",
                "reserve-binding","reserve-profile","reserve-target","stale-reserve","foreign-reserve","foreign-profile","dead-reserve","unknown-reserve-hp")){
            var result=evaluate(fixture(own("bulkup"),missing));
            check(!result.utility().known(),"Continuation bypassed required proof: "+missing);
        }
        var trapped=evaluate(fixture(own("bulkup"),"trapped"));
        check(trapped.utility().known()&&trapped.lines().getFirst().state().opponent().stages().get("atk")==1,
                "Voluntary trapping was mistaken for own-party exhaustion");
        var simultaneous=new Case("both-active-ko","closecombat","bulkup",1,3,1,false,"none","runaway",false,false,false);
        var both=evaluate(fixture(simultaneous,""));
        check(both.lines().stream().allMatch(l->l.state().self().hp().value().high()==0&&l.state().opponent().hp().value().high()==0
                &&!l.state().forcedReplacement()&&!l.state().nextRequestKnown()),
                "Simultaneous active KOs invented a replacement despite unknown opponent party closure");
        var expired=fixture(own("bulkup"),"");expired.current().set(false);
        var result=evaluate(expired);
        check(!result.utility().known()&&result.lines().getFirst().state().self().hp().value().equals(new Interval(.001,.001)),
                "Expired source retained calculated recoil state");
        var expiredOwn=fixture(own("bulkup"),"");expiredOwn.transport().endChoice();
        result=evaluate(expiredOwn);
        check(!result.utility().known()&&result.lines().getFirst().state().self().hp().value().equals(new Interval(.001,.001)),
                "Expired real owning-request receipt retained continuation");
        var closing=fixture(own("bulkup"),"");var armed=new AtomicBoolean();var changed=new AtomicBoolean();
        Map<String,Profile> expiring=new AbstractMap<>(){
            @Override public Set<Entry<String,Profile>> entrySet(){return closing.profiles().entrySet();}
            @Override public Profile get(Object key){
                if(key.equals("reserve0"))armed.set(true);
                if(key.equals("opponent")&&armed.get()&&changed.compareAndSet(false,true))closing.transport().endChoice();
                return closing.profiles().get(key);
            }
        };
        result=evaluate(closing,expiring);
        check(changed.get()&&!result.utility().known()&&result.lines().stream().allMatch(l->
                l.state().self().hp().value().equals(new Interval(.001,.001))&&!l.state().forcedReplacement()),
                "Lease expiration after reserve certification leaked a calculated state/request");
        var field=fixture(own("bulkup"),"");field.fields().reset();
        boolean rejected=false;try{evaluate(field);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Expired field receipt certified continuation");
        System.out.println("Recoil continuation evidence regressions PASS: "+checks+" checks; queued self/target effects, replacement, provenance, trapping and expiry");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/recoil_continuation_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source
                +";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==27,"Native recoil continuation inventory changed");
        ShadowMechanicsEvaluation.Ranking hidden=null;
        for(var value:rows){
            var row=value.getAsJsonObject();var raw=row.getAsJsonObject("config");
            var c=new Case(raw.get("id").getAsString(),raw.get("fast").getAsString(),raw.get("slow").getAsString(),
                    raw.get("hp").getAsInt(),raw.get("targetHp").getAsInt(),raw.get("ownBench").getAsInt(),raw.get("enemy").getAsBoolean(),
                    raw.get("slowItem").getAsString().isEmpty()?"none":raw.get("slowItem").getAsString(),raw.get("ability").getAsString(),
                    raw.get("magicRoom").getAsBoolean(),raw.get("protect").getAsBoolean(),raw.get("rocks").getAsBoolean());
            // foeBench, ended and requestState are oracle labels, never evaluator inputs.
            var result=evaluate(fixture(c,""));
            if(c.id().startsWith("terminal-")||c.enemy()){
                check(!result.utility().known(),"Unknown party closure certified continuation: "+c.id());
                if(c.id().equals("enemy-hidden-reserve"))hidden=result;
                if(c.id().equals("enemy-hidden-last"))check(result.equals(hidden),"Hidden opponent reserves changed the public forecast");
            }else{
                check(result.utility().known(),"Native continuation unexpectedly UNKNOWN: "+c.id()+" "+result.unknown());
                check(result.lines().stream().anyMatch(l->matches(l.state(),row)),"Native recoil continuation excluded: "+c.id()+" "+result.lines()+" / "+row);
                if(c.id().equals("hazard-reserve"))check(result.lines().stream().allMatch(l->l.state().self().id().equals("self")),
                        "Native replacement/hazard entry was applied before its new request");
            }
        }
        System.out.println("Recoil continuation native differential PASS: 27 exchanges; self-target setup/recovery, no-target effects, faint stages, pending entry and terminal/private-party abstention");
    }
    private static boolean matches(State state,JsonObject row){
        for(String role:List.of("self","foe")){
            var actor=role.equals("self")?state.self():state.opponent();var hp=actor.hp().value();double actual=row.get(role+"Hp").getAsDouble();
            if(actual<hp.low()-1e-10||actual>hp.high()+1e-10)return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())
                if(!Objects.equals(actor.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
}
