package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Explicit public-only conditional inputs; native team closure is an oracle label only. */
final class PivotOutcomeRegression {
    private static final String SOURCE="declared pivot-outcome native conditions";
    private static final Known<Boolean> YES=known(true);
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    record Case(JsonObject data) {
        String s(String key){return data.get(key).getAsString();}
        int n(String key){return data.get(key).getAsInt();}
        boolean b(String key){return data.get(key).getAsBoolean();}
        List<String> types(){var result=new ArrayList<String>();data.getAsJsonArray("reserveTypes").forEach(v->result.add(v.getAsString()));return result;}
        Map<String,Integer> hazards(){var result=new TreeMap<String,Integer>();data.getAsJsonObject("hazards").entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsInt()));return result;}
        String action(){return b("enemy")?s("reply"):"uturn";}
        String reply(){return b("enemy")?"uturn":s("reply");}
    }
    record Fixture(Case config,BattleSnapshot snapshot,PublicMechanics mechanics,Map<String,Profile> profiles,
                           ScopedExecution execution,OwnRequestTransport transport,AtomicBoolean current,PublicFieldEvidence fields) {}
    private record Owner(OwnRequestTransport transport,OwnRequestTransport.Lease lease) {}
    static Case ordinary(){return new Case(JsonParser.parseString("""
        {"id":"controlled","reply":"bulkup","hp":500,"targetHp":1000,"userSpeed":300,"targetSpeed":100,"item":"",
         "ownBench":1,"enemy":false,"reserveMax":1000,"reserveHp":1000,"reserveTypes":["normal"],"reserveItem":"",
         "reserveAbility":"runaway","hazards":{},"magicRoom":false,"protect":false}
        """).getAsJsonObject());}
    private static Case change(Case c,String key,int value){var d=c.data().deepCopy();d.addProperty(key,value);return new Case(d);}
    private static Case rocks(){var c=ordinary();c.data().getAsJsonObject("hazards").addProperty("stealthrock",1);return c;}
    private static String item(String raw){return raw.isEmpty()?"none":raw;}
    private static Map<String,Integer> stages(){var s=new TreeMap<String,Integer>();STATS.forEach(k->s.put(k,0));return s;}
    private static Profile profile(String role,int max,int speed,List<String> types){
        var stats=new TreeMap<String,Known<IntRange>>();
        for(String stat:List.of("atk","def","spa","spd","spe"))stats.put(stat,known(IntRange.exact(stat.equals("spe")?speed:220)));
        return new Profile(role,known(100),known(IntRange.exact(max)),stats,known(types),YES);
    }
    private static BattleSnapshot.Combatant facts(double hp,List<String> types,List<String> moves){
        return new BattleSnapshot.Combatant("Mew",hp,types,stages(),moves,null,null,hp==0);
    }
    private static Owner owner(Case c){
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),first=new Object(),second=new Object();
        var active=new UUID(0,46);ledger.owner(battle,actor,"p1");var pokemon=new ArrayList<OwnRequestTransport.Pokemon>();
        for(int i=0;i<=c.n("ownBench");i++){
            int hp=i==0?(c.b("enemy")?c.n("targetHp"):c.n("hp")):c.n("reserveHp"),max=i==0?1000:c.n("reserveMax");
            var types=i==0?List.of("normal"):c.types();String held=i==0?(c.b("enemy")?"none":item(c.s("item"))):item(c.s("reserveItem"));
            pokemon.add(new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(i==0?active:new UUID(0,46+i),"p1: Own"+i,
                    hp+"/"+max,i==0,i==0&&c.data().has("userAbility")&&!c.b("enemy")?c.s("userAbility"):i==0?"runaway":c.s("reserveAbility"),types),100,
                    Map.of("atk",220,"def",220,"spa",220,"spd",220,"spe",i==0?(c.b("enemy")?c.n("targetSpeed"):c.n("userSpeed")):220),held,types,i>0&&c.data().has("reserveStatus")?c.s("reserveStatus"):"none",false));
        }
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,pokemon);
        ledger.dispatched(battle,actor,"p1",first,()->request);ledger.turnDispatched(battle,1);ledger.turnFinished(battle,1);
        ledger.dispatched(battle,actor,"p1",second,()->request);ledger.turnDispatched(battle,2);
        var lease=ledger.beginChoice(battle,actor,second,active,false,()->true);
        if(!lease.current())throw new AssertionError("Pivot fixture did not issue a real current owning lease");
        return new Owner(ledger,lease);
    }
    static Fixture fixture(Case c,String missing){
        var owner=owner(c);var protocol=new PublicProtocolEvidence();
        var events=new StringBuilder("update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L100|100/100\n");
        if(!missing.equals("opponent-ability"))events.append("|-ability|p2a: Foe|").append(c.b("enemy")&&c.data().has("userAbility")?c.s("userAbility"):!c.b("enemy")&&c.data().has("targetAbility")?c.s("targetAbility"):"runaway").append('\n');
        if(!missing.equals("opponent-item"))events.append(c.b("enemy")&&c.s("item").equals("lifeorb")?"|-item|p2a: Foe|Life Orb\n":!c.b("enemy")&&c.data().has("targetItem")&&!c.s("targetItem").equals("none")?"|-item|p2a: Foe|"+c.s("targetItem")+"\n":"|-enditem|p2a: Foe|Sitrus Berry\n");
        events.append("|turn|1\n|turn|2");
        if(c.data().has("weather")&&!c.s("weather").equals("none")){
            events.append("\n|-weather|").append(c.s("weather"));
            if(c.data().has("weatherMode")&&c.s("weatherMode").equals("upkeep"))events.append("|[upkeep]");
            if(c.data().has("weatherMode")&&c.s("weatherMode").equals("ended"))events.append("\n|-weather|none");
        }
        if(c.data().has("targetWind")&&c.b("targetWind"))events.append("\n|-sidestart|p2: Foe|Tailwind");
        if(c.data().has("trickRoom")&&c.b("trickRoom"))events.append("\n|-fieldstart|move: Trick Room");
        if(missing.equals("gravity"))events.append("\n|-fieldstart|move: Gravity");
        if(missing.equals("ambiguous"))events.append("\n|replace|p2a: Foe|Zoroark, L100");
        if(missing.equals("suppression"))events.append("\n|-endability|p2a: Foe|Run Away");
        if(missing.equals("wonder-room"))events.append("\n|-fieldstart|move: Wonder Room");
        if(c.b("magicRoom"))events.append("\n|-fieldstart|move: Magic Room");
        if(c.b("protect"))events.append("\n|-singleturn|").append(c.b("enemy")?"p1a: Own":"p2a: Foe").append("|Protect");
        if(c.data().has("witnessToxic")&&c.b("witnessToxic")){
            events.append("\n|move|p2a: Foe|Toxic Spikes|p1a: Own");
            if(missing.equals("indirect-setter"))events.append("|[from] ability: Copycat");
        }
        for(var e:c.hazards().entrySet())for(int i=0;i<e.getValue();i++)events.append("\n|-sidestart|")
                .append(c.b("enemy")?"p2: Foe":"p1: Own").append('|').append(switch(e.getKey()){
                    case "stealthrock"->"Stealth Rock";case "spikes"->"Spikes";case "stickyweb"->"Sticky Web";case "toxicspikes"->"Toxic Spikes";default->throw new AssertionError();});
        protocol.update(List.of(events.toString()));var provider=new PublicMechanicsEvidence(id->null);provider.update(protocol);
        var fields=new PublicFieldEvidence();var field=fields.update(protocol,provider);var current=new AtomicBoolean(true);
        var scopes=ExecutionCoverage.Scopes.receipt(op->missing.equals(op.name().toLowerCase(Locale.ROOT)+"-scope")?Known.unknown():YES,current::get);
        var own=provider.observation("p1a");var foe=provider.observation("p2a");
        var bindings=new HashMap<String,ScopedExecution.Binding>();
        bindings.put("self",new ScopedExecution.Binding(own,missing.equals("own-item")?Known.unknown():known(c.b("enemy")?"none":item(c.s("item"))),known(c.data().has("userAbility")&&!c.b("enemy")?c.s("userAbility"):"runaway"),YES,owner.lease()));
        bindings.put("opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability()));
        var profiles=new HashMap<String,Profile>();
        profiles.put("self",profile("self",1000,c.b("enemy")?c.n("targetSpeed"):c.n("userSpeed"),List.of("normal")));
        profiles.put("opponent",profile("opponent",1000,c.b("enemy")?c.n("userSpeed"):c.n("targetSpeed"),List.of("normal")));
        String defender=c.b("enemy")?"self":"opponent";
        var targetProfile=profiles.get(defender);var targetStats=new TreeMap<>(targetProfile.stats());
        for(String stat:List.of("atk","spa")){
            String key=stat.equals("atk")?"targetAttack":"targetSpecialAttack";
            if(c.data().has(key))targetStats.put(stat,known(IntRange.exact(c.n(key))));
        }
        for(String stat:List.of("def","spd")){
            String key=stat.equals("def")?"targetDefense":"targetSpecialDefense";
            if(c.data().has(key))targetStats.put(stat,known(IntRange.exact(c.n(key))));
            if(c.data().has(stat+"Low"))targetStats.put(stat,known(new IntRange(c.n(stat+"Low"),c.n(stat+"High"))));
            if(missing.equals("foe-"+stat))targetStats.remove(stat);
        }
        var targetTypes=new ArrayList<String>();if(c.data().has("targetTypes"))c.data().getAsJsonArray("targetTypes").forEach(v->targetTypes.add(v.getAsString()));else targetTypes.add("normal");
        profiles.put(defender,new Profile(defender,targetProfile.level(),targetProfile.maxHp(),targetStats,known(targetTypes),YES));
        var bench=new ArrayList<BattleSnapshot.Combatant>();var choices=new ArrayList<String>();
        for(int i=0;i<c.n("ownBench");i++){
            String id="reserve"+i;double hp=c.n("reserveHp")/(double)c.n("reserveMax");
            bench.add(facts(missing.equals("reserve-hp")?BattleSnapshot.UNKNOWN_HP_FRACTION:hp,c.types(),List.of()));choices.add(id);
            var p=profile(id,c.n("reserveMax"),220,c.types());
            if(missing.equals("reserve-types"))p=new Profile(id,p.level(),p.maxHp(),p.stats(),Known.unknown(),YES);
            if(missing.equals("reserve-max"))p=new Profile(id,p.level(),Known.unknown(),p.stats(),p.types(),YES);
            profiles.put(id,p);
            if(!missing.equals("reserve-binding"))bindings.put(id,new ScopedExecution.Binding(missing.equals("foreign-reserve")?foe:null,
                    missing.equals("reserve-item")?Known.unknown():known(item(c.s("reserveItem"))),missing.equals("reserve-ability")?Known.unknown():known(c.s("reserveAbility")),missing.equals("stale-reserve")?Known.unknown():YES,owner.lease()));
        }
        var execution=new ScopedExecution(field,bindings,scopes);
        var hazards=new Hazards(c.b("enemy")?Map.of():c.hazards(),!missing.equals("partial-hazards"),SOURCE);
        var foeHazards=new Hazards(c.b("enemy")?c.hazards():Map.of(),true,SOURCE);
        var targets=new HashMap<String,SwitchTarget>();
        for(int i=0;i<bench.size();i++){
            String id="reserve"+i;Profile p=profiles.get(id);
            var max=p.maxHp().known()?known(p.maxHp().value().low()):Known.<Integer>unknown();
            var entry=execution.entry(hazards,max,p);
            Known<Interval> damage=missing.equals("entry-damage")?Known.unknown():missing.equals("altered-entry-damage")?known(new Interval(0,0)):entry.damage();
            Known<Integer> speed=missing.equals("entry-speed")?Known.unknown():missing.equals("altered-entry-speed")?known(-1):entry.speedDelta();
            targets.put(id,new SwitchTarget(bench.get(i),missing.equals("reserve-status")?Known.unknown():known(c.data().has("reserveStatus")?c.s("reserveStatus"):"none"),damage,speed));
        }
        if(missing.equals("reserve-profile"))profiles.remove("reserve0");
        if(missing.equals("foreign-profile"))profiles.put("reserve0",profile("opponent",1000,220,List.of("normal")));
        if(missing.equals("reserve-target"))targets.clear();
        double ownHp=(c.b("enemy")?c.n("targetHp"):c.n("hp"))/1000.0,foeHp=(c.b("enemy")?c.n("hp"):c.n("targetHp"))/1000.0;
        var self=facts(ownHp,List.of("normal"),List.of());var opponent=facts(foeHp,List.of("normal"),List.of(c.reply()));
        var targetStages=stages();
        for(String stat:List.of("def","spd")){
            String key=stat.equals("def")?"targetDefStage":"targetSpdStage";
            if(c.data().has(key))targetStages.put(stat,c.n(key));
            if(missing.equals("foe-"+stat+"-stage"))targetStages.remove(stat);
        }
        var old=c.b("enemy")?self:opponent;
        var targetFacts=new BattleSnapshot.Combatant(old.species(),old.hpFraction(),targetTypes,targetStages,old.revealedMoves(),null,null,old.fainted());
        if(c.b("enemy"))self=targetFacts;else opponent=targetFacts;
        boolean trapped=missing.equals("trapped");
        var snapshot=new BattleSnapshot(2,self,opponent,bench,List.of(),List.of(c.action()),missing.equals("no-choice")?List.of():choices,Map.of(),false,null);
        var targetStatus=known(c.data().has("targetStatus")?c.s("targetStatus"):"none");
        var context=new PublicTransitionState.Context(known(new Interval(ownHp,ownHp)),known(new Interval(foeHp,foeHp)),c.b("enemy")?targetStatus:known("none"),c.b("enemy")?known("none"):targetStatus,hazards,foeHazards,targets,Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),opponent.knownBoosts(),Set.of(),false,false,trapped,0,new Evidence(context,Map.of(),Map.of()));
        return new Fixture(c,snapshot,mechanics,Map.copyOf(profiles),execution,owner.transport(),current,fields);
    }
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f){return evaluate(f,f.profiles(),f.execution());}
    private static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f,Map<String,Profile> profiles,ScopedExecution execution){
        String action="move:"+f.config().action();
        var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),profiles,List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    static void run(){
        for(int hp:List.of(3,80)){
            var result=evaluate(fixture(change(ordinary(),"targetHp",hp),""));
            check(!result.utility().known(),"Target KO/possible KO certified a no-pivot result");
        }
        for(int hp:List.of(100,125)){
            var result=evaluate(fixture(change(rocks(),"reserveHp",hp),""));
            check(!result.utility().known(),"Lethal reserve entry certified a completed pivot");
        }
        var survives=evaluate(fixture(change(rocks(),"reserveHp",126),""));
        check(survives.utility().known()&&survives.lines().stream().allMatch(l->l.state().self().id().equals("reserve0")
                &&Math.abs(l.state().self().hp().value().low()-.001)<1e-10),"One-HP entry survival was lost");
        for(String missing:List.of("own-item","opponent-ability","damage-scope","entry-scope","reserve-binding","reserve-profile",
                "reserve-target","foreign-reserve","foreign-profile","stale-reserve","reserve-types","reserve-max","reserve-hp",
                "partial-hazards","entry-damage","entry-speed","altered-entry-damage","altered-entry-speed","trapped","no-choice")){
            var result=evaluate(fixture(rocks(),missing));
            check(!result.utility().known(),"Pivot bypassed required source/entry/choice proof: "+missing);
        }
        for(int count:List.of(0,2))check(!evaluate(fixture(change(ordinary(),"ownBench",count),"")).utility().known(),"Unproved replacement count was guessed");
        var unscoped=fixture(ordinary(),"");check(!evaluate(unscoped,unscoped.profiles(),null).utility().known(),"Missing execution receipt certified pivot entry");
        var blocked=ordinary();blocked.data().addProperty("protect",true);blocked.data().addProperty("item","lifeorb");
        var stop=evaluate(fixture(blocked,"entry-scope"));
        check(stop.utility().known()&&stop.lines().stream().allMatch(l->l.state().self().id().equals("self")&&l.state().self().hp().value().equals(new Interval(.5,.5))),
                "Protect required unrelated entry scope, pivoted or recoiled");
        var expired=fixture(ordinary(),"");expired.transport().endChoice();
        var result=evaluate(expired);
        check(!result.utility().known()&&result.lines().stream().allMatch(l->l.state().self().id().equals("self")),"Expired real owner receipt retained pivot");
        var source=fixture(ordinary(),"");source.current().set(false);
        check(!evaluate(source).utility().known(),"Expired source receipt retained pivot");
        var field=fixture(ordinary(),"");field.fields().reset();boolean rejected=false;
        try{evaluate(field);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Expired field receipt retained pivot");
        var closing=fixture(ordinary(),"");var armed=new AtomicBoolean();var changed=new AtomicBoolean();
        Map<String,Profile> expiring=new AbstractMap<>(){
            @Override public Set<Entry<String,Profile>> entrySet(){return closing.profiles().entrySet();}
            @Override public Profile get(Object key){
                if(key.equals("reserve0"))armed.set(true);
                if(key.equals("opponent")&&armed.get()&&changed.compareAndSet(false,true))closing.transport().endChoice();
                return closing.profiles().get(key);
            }
        };
        result=evaluate(closing,expiring,closing.execution());
        check(changed.get()&&!result.utility().known()&&result.lines().stream().allMatch(l->l.state().self().id().equals("self")
                &&l.state().self().hp().value().equals(new Interval(.5,.5))),"Expiry after entry proof leaked an incoming actor forecast");
        System.out.println("Pivot outcome evidence regressions PASS: "+checks+" checks; KO, choice, entry binding/survival, source scope and real receipt expiration");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/pivot_outcome_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source
                +";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==35,"Native pivot inventory changed");
        Set<String> abstentions=Set.of("target-ko","target-last","possible-target-ko","recoil-last","entry-ko","ambiguous-reserve",
                "no-reserve","opponent-pivot");
        ShadowMechanicsEvaluation.Ranking hidden=null;
        for(var value:rows){
            var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));
            // foeBench and ended are native labels, never passed to compare.
            var result=evaluate(fixture(c,""));
            if(abstentions.contains(c.s("id"))){
                check(!result.utility().known(),"Unproved pivot outcome became known: "+c.s("id"));
                if(c.s("id").equals("target-ko"))hidden=result;
                if(c.s("id").equals("target-last"))check(result.equals(hidden),"Hidden opponent party closure changed the forecast");
            }else{
                check(result.utility().known(),"Supported native pivot became UNKNOWN: "+c.s("id")+" "+result.unknown());
                check(result.lines().stream().anyMatch(l->matches(l,row)),"Native pivot excluded: "+c.s("id")+" "+result.lines()+" / "+row);
            }
        }
        System.out.println("Pivot outcome native differential PASS: 35 exchanges; timing, target KO abstention, recoil ownership, hazard rounding/immunity/suppression and reply routing");
    }
    static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        State state=line.state();if(!state.self().id().equals(row.get("active").getAsString()))return false;
        double utility=row.get("utility").getAsDouble();if(utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){
            Actor actor=role.equals("self")?state.self():state.opponent();double hp=row.get(role+"Hp").getAsDouble();var range=actor.hp().value();
            if(hp<range.low()-1e-10||hp>range.high()+1e-10||!actor.status().value().equals(row.get(role+"Status").getAsString()))return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(actor.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
}
