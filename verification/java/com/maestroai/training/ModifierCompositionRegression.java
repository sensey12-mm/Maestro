package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Explicit public-only conditional inputs; synthetic stats are not inferred builds. */
final class ModifierCompositionRegression {
    private static final String SOURCE="declared modifier composition native conditions";
    private static final Known<Boolean> YES=known(true);
    private static int checks;
    private static <T> Known<T> known(T v){return Known.of(v,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    record Case(JsonObject data) {
        String s(String key){return data.get(key).getAsString();}
        int n(String key){return data.get(key).getAsInt();}
        boolean b(String key){return data.get(key).getAsBoolean();}
        List<String> types(String key){var out=new ArrayList<String>();data.getAsJsonArray(key).forEach(v->out.add(v.getAsString()));return out;}
        String user(){return b("enemy")?"opponent":"self";}
        String target(){return b("enemy")?"self":"opponent";}
        String otherMove(){return data.has("replyMove")?s("replyMove"):"bulkup";}
        int userHp(){return data.has("userHp")?n("userHp"):500;}
        int userSpeed(){return data.has("userSpeed")?n("userSpeed"):300;}
        int targetSpeed(){return data.has("targetSpeed")?n("targetSpeed"):100;}
        String targetStatus(){return data.has("targetStatus")?s("targetStatus"):"none";}
        int speedStage(boolean user){String key=user?"speedStage":"targetSpeedStage";return data.has(key)?n(key):0;}
        String action(){return b("enemy")?otherMove():s("move");}
        String reply(){return b("enemy")?s("move"):otherMove();}
    }
    record Fixture(Case config,ScopedExecution execution,Map<String,Profile> profiles,State state,
                           BattleSnapshot snapshot,PublicMechanics mechanics,OwnRequestTransport owner,
                           AtomicBoolean current,PublicFieldEvidence fields,OwnRequestTransport.Lease lease,
                           PublicProtocolEvidence protocol,PublicMechanicsEvidence provider,List<String> history) {}
    static Case shield(){return new Case(JsonParser.parseString("""
        {"id":"declared","move":"psyshock","ability":"runaway","targetAbility":"shadowshield","item":"lifeorb","targetItem":"none",
         "attack":220,"defense":220,"attackStage":0,"defenseStage":0,"level":100,"types":["normal"],"targetTypes":["normal"],
         "status":"none","targetMax":1000,"targetHp":1000,"room":false,"weather":"none","enemy":false}
        """).getAsJsonObject());}
    static Case change(Case c,String key,String value){var d=c.data().deepCopy();d.addProperty(key,value);return new Case(d);}
    static Case enemy(Case c){var d=c.data().deepCopy();d.addProperty("enemy",true);return new Case(d);}
    private static Case aura(){return change(change(change(shield(),"move","drainingkiss"),"ability","technician"),"targetAbility","fairyaura");}
    private static Map<String,Integer> stages(int atk,int def){var s=new TreeMap<String,Integer>();STATS.forEach(k->s.put(k,0));s.put("atk",atk);s.put("spa",atk);s.put("def",def);s.put("spd",def);return s;}
    static Fixture fixture(Case c,String missing,Known<Interval> targetHp){
        var stats=new HashMap<String,Map<String,Integer>>();var profiles=new HashMap<String,Profile>();
        for(String role:List.of("self","opponent")){
            boolean user=role.equals(c.user());var values=new TreeMap<String,Integer>();
            values.put("atk",user?c.n("attack"):220);values.put("spa",user?c.n("attack"):220);
            values.put("def",user?220:c.n("defense"));values.put("spd",user?220:c.n("defense"));values.put("spe",user?c.userSpeed():c.targetSpeed());
            stats.put(role,values);var bounded=new TreeMap<String,Known<IntRange>>();values.forEach((key,n)->bounded.put(key,known(IntRange.exact(n))));
            if(missing.equals("speed-stat")&&user)bounded.remove("spe");
            if(missing.equals("attack-stat")&&user){var move=CobblemonMechanics.move(c.s("move"));bounded.remove(move==null?"spa":move.attackingStat());}
            var max=missing.equals("target-max")&&!user?Known.<IntRange>unknown():known(IntRange.exact(user?1000:c.n("targetMax")));
            if(missing.equals("bounded-target-max")&&!user)max=known(new IntRange(c.n("targetMax")-100,c.n("targetMax")+100));
            boolean missingTypes=missing.equals("types")||missing.equals("attacker-types")&&user||missing.equals("target-types")&&!user;
            profiles.put(role,new Profile(role,missing.equals("level")&&user?Known.unknown():known(user?c.n("level"):100),max,bounded,
                    missingTypes?Known.unknown():known(c.types(user?"types":"targetTypes")),YES));
        }
        String ownItem=c.b("enemy")?c.s("targetItem"):c.s("item"),ownAbility=c.b("enemy")?c.s("targetAbility"):c.s("ability");
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),first=new Object(),second=new Object();
        var active=new UUID(0,47);ledger.owner(battle,actor,"p1");boolean ownUser=!c.b("enemy");
        var pokemon=new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(active,"p1: Own",
                ownUser?c.userHp()+"/1000":c.n("targetHp")+"/"+c.n("targetMax"),true,ownAbility,c.types(ownUser?"types":"targetTypes")),
                ownUser?c.n("level"):100,stats.get("self"),ownItem,c.types(ownUser?"types":"targetTypes"),ownUser?c.s("status"):c.targetStatus(),false);
        var request=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(pokemon));
        ledger.dispatched(battle,actor,"p1",first,()->request);ledger.turnDispatched(battle,1);ledger.turnFinished(battle,1);
        ledger.dispatched(battle,actor,"p1",second,()->request);ledger.turnDispatched(battle,2);
        var lease=ledger.beginChoice(battle,actor,second,active,false,()->true);
        if(!lease.current())throw new AssertionError("Modifier fixture did not issue a real owner receipt");
        String foeItem=c.b("enemy")?c.s("item"):c.s("targetItem"),foeAbility=c.b("enemy")?c.s("ability"):c.s("targetAbility");
        var events=new StringBuilder("update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L")
                .append(ownUser?c.n("level"):100).append("|100/100\n|switch|p2a: Foe|Mew, L")
                .append(ownUser?100:c.n("level")).append("|100/100\n");
        if(!missing.equals("opponent-item"))events.append(foeItem.equals("none")?"|-enditem|p2a: Foe|Sitrus Berry\n":"|-item|p2a: Foe|"+foeItem+"\n");
        if(!missing.equals("opponent-ability"))events.append("|-ability|p2a: Foe|").append(foeAbility).append('\n');
        events.append("|turn|1");
        if(c.data().has("pastPar")&&c.b("pastPar"))events.append("\n|cant|").append(ownUser?"p1a: Own":"p2a: Foe").append("|par");
        if(missing.equals("unsupported-cant"))events.append("\n|cant|").append(ownUser?"p1a: Own":"p2a: Foe").append("|frz");
        if(c.data().has("witnessTailwind")&&c.b("witnessTailwind")){
            String slot=ownUser?"p1a: Own":"p2a: Foe";
            events.append("\n|move|").append(slot).append("|Tailwind|").append(slot);
            if(missing.equals("indirect-setter"))events.append("|[from] ability: Copycat");
            events.append("\n|-sidestart|").append(ownUser?"p1: Own":"p2: Foe").append("|move: Tailwind");
        }
        if(c.data().has("witnessSetter")&&c.b("witnessSetter")){
            events.append("\n|move|p2a: Foe|Psychic Terrain|p2a: Foe");
            if(missing.equals("indirect-setter"))events.append("|[from] ability: Copycat");
            events.append("\n|-fieldstart|move: Psychic Terrain");
        }
        events.append("\n|turn|2");
        for(String key:List.of("wind","targetWind"))if(c.data().has(key)&&c.b(key)){
            boolean own=key.equals("wind")?ownUser:!ownUser;
            if(!key.equals("wind")||!c.data().has("witnessTailwind")||!c.b("witnessTailwind"))
                events.append("\n|-sidestart|").append(own?"p1: Own":"p2: Foe").append("|move: Tailwind");
            if(c.data().has("windEnded")&&c.b("windEnded"))events.append("\n|-sideend|").append(own?"p1: Own":"p2: Foe").append("|move: Tailwind");
        }
        if(c.data().has("trickRoom")&&c.b("trickRoom"))events.append("\n|-fieldstart|move: Trick Room");
        if(c.b("room"))events.append("\n|-fieldstart|move: Magic Room");
        if(!c.s("weather").equals("none"))events.append("\n|-weather|").append(c.s("weather"));
        if(c.data().has("terrain")&&!c.s("terrain").equals("none")&&!(c.data().has("witnessSetter")&&c.b("witnessSetter")))events.append("\n|-fieldstart|move: ").append(c.s("terrain"));
        if(c.data().has("terrainMode")&&Set.of("ended","restart").contains(c.s("terrainMode")))events.append("\n|-fieldend|move: Psychic Terrain");
        if(c.data().has("terrainMode")&&c.s("terrainMode").equals("restart"))events.append("\n|-fieldstart|move: Psychic Terrain");
        if(missing.equals("gravity"))events.append("\n|-fieldstart|move: Gravity");
        if(missing.equals("magnet-rise"))events.append("\n|-start|p2a: Foe|Magnet Rise");
        if(missing.equals("unsupported-terrain"))events.append("\n|-fieldstart|move: Electric Terrain");
        if(missing.equals("suppression"))events.append("\n|-endability|p2a: Foe|Shadow Shield");
        if(missing.equals("ambiguous"))events.append("\n|replace|p2a: Foe|Zoroark, L100");
        if(Set.of("protect","protect-accuracy-stage","protect-evasion-stage").contains(missing))events.append("\n|-singleturn|").append(c.b("enemy")?"p1a: Own":"p2a: Foe").append("|Protect");
        var history=new ArrayList<>(List.of(events.toString()));
        var protocol=new PublicProtocolEvidence();protocol.update(history);
        var provider=new PublicMechanicsEvidence(id->null);provider.update(protocol);
        var fields=new PublicFieldEvidence();var field=fields.update(protocol,provider);var current=new AtomicBoolean(true);
        var scopes=ExecutionCoverage.Scopes.receipt(op->missing.equals(op.name().toLowerCase(Locale.ROOT)+"-scope")?Known.unknown():YES,current::get);
        var foe=provider.observation("p2a");
        var execution=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(provider.observation("p1a"),missing.equals("own-item")?Known.unknown():known(ownItem),
                        missing.equals("own-ability")?Known.unknown():known(ownAbility),missing.equals("own-transport")?Known.unknown():YES,lease),
                "opponent",new ScopedExecution.Binding(foe,foe.item(),missing.equals("forged-opponent")?known("technician"):foe.ability())),scopes);
        var userHp=known(new Interval(c.userHp()/1000.0,c.userHp()/1000.0));var selfHp=c.b("enemy")?targetHp:userHp;var foeHp=c.b("enemy")?userHp:targetHp;
        var selfStages=stages(c.b("enemy")?0:c.n("attackStage"),c.b("enemy")?c.n("defenseStage"):0);selfStages.put("spe",c.speedStage(!c.b("enemy")));
        var foeStages=stages(c.b("enemy")?c.n("attackStage"):0,c.b("enemy")?0:c.n("defenseStage"));foeStages.put("spe",c.speedStage(c.b("enemy")));
        for(String role:List.of("self","opponent"))for(String stat:List.of("accuracy","evasion")){
            boolean user=role.equals(c.user());var values=role.equals("self")?selfStages:foeStages;
            String key=user?stat+"Stage":"target"+Character.toUpperCase(stat.charAt(0))+stat.substring(1)+"Stage";
            if(c.data().has(key))values.put(stat,c.n(key));
            if(Set.of("accuracy-stage","protect-accuracy-stage").contains(missing)&&user&&stat.equals("accuracy")
                    ||Set.of("evasion-stage","protect-evasion-stage").contains(missing)&&!user&&stat.equals("evasion"))values.remove(stat);
        }
        var selfFacts=facts(selfHp,c.types(c.b("enemy")?"targetTypes":"types"),selfStages,List.of());
        var foeFacts=facts(foeHp,c.types(c.b("enemy")?"types":"targetTypes"),foeStages,List.of(c.reply()));
        var snapshot=new BattleSnapshot(2,selfFacts,foeFacts,List.of(),List.of(),List.of(c.action()),List.of(),Map.of(),false,null);
        var hazards=new Hazards(Map.of(),true,SOURCE);var healthy=known(c.targetStatus());var status=missing.equals("status")?Known.<String>unknown():known(c.s("status"));
        var context=new PublicTransitionState.Context(selfHp,foeHp,c.b("enemy")?healthy:status,c.b("enemy")?status:healthy,hazards,hazards,Map.of(),Map.of(),Set.of());
        var mechanics=new PublicMechanics(Map.of(),foeFacts.knownBoosts(),Set.of(),false,false,false,0,new Evidence(context,Map.of(),Map.of()));
        var state=PublicStateSearchAdapter.project(snapshot,mechanics).states().get("root");
        return new Fixture(c,execution,Map.copyOf(profiles),state,snapshot,mechanics,ledger,current,fields,lease,protocol,provider,history);
    }
    private static BattleSnapshot.Combatant facts(Known<Interval> hp,List<String> types,Map<String,Integer> stages,List<String> moves){
        return new BattleSnapshot.Combatant("Mew",hp.known()&&hp.value().low()==hp.value().high()?hp.value().low():BattleSnapshot.UNKNOWN_HP_FRACTION,types,stages,moves,null,null,false);
    }
    static Fixture fixture(Case c,String missing){double hp=c.n("targetHp")/(double)c.n("targetMax");return fixture(c,missing,known(new Interval(hp,hp)));}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Actor target(Fixture f){return f.config().b("enemy")?f.state().self():f.state().opponent();}
    static Damage damage(Fixture f,Critical critical){return f.execution().damage(f.config().s("move"),f.profiles().get(f.config().user()),user(f),f.profiles().get(f.config().target()),target(f),critical);}
    static ExecutionCoverage.Certificate coverage(Fixture f){return f.execution().damageCoverage(f.config().s("move"),f.profiles().get(f.config().user()),user(f),f.profiles().get(f.config().target()),target(f));}
    static ShadowMechanicsEvaluation.Ranking evaluate(Fixture f){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,f.execution()).rankings().getFirst();
    }
    static void run(){
        for(Case c:List.of(aura(),shield(),enemy(aura()),enemy(shield()))){
            var fresh=fixture(c,"");check(damage(fresh,Critical.BOTH).points().known(),"Source-proven modifier composition remains UNKNOWN");
            check(evaluate(fresh).utility().known(),"Composed modifier failed conditional exchange");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope","target-max","types","level","attack-stat","status","suppression","ambiguous","forged-opponent"))
                check(!damage(fixture(c,missing),Critical.BOTH).fraction().known(),"Composed damage bypassed evidence: "+missing);
            var expired=fixture(c,"");var receipt=coverage(expired);check(receipt.supported(),"Fresh composition certificate missing");expired.owner().endChoice();
            check(!receipt.supported()&&!damage(expired,Critical.BOTH).fraction().known()&&!evaluate(expired).utility().known(),"Expired real owner receipt retained composed damage");
            var source=fixture(c,"");var cert=coverage(source);source.current().set(false);
            check(!cert.supported()&&!damage(source,Critical.BOTH).fraction().known(),"Expired source receipt retained composition");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{damage(field,Critical.BOTH);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Expired field receipt retained composition");
            var blocked=damage(fixture(c,"protect"),Critical.BOTH);check(blocked.fraction().known()&&blocked.fraction().value().equals(new Interval(0,0)),"Protect did not block composed modifiers");
        }
        check(!damage(fixture(shield(),"",known(new Interval(.999,1))),Critical.BOTH).fraction().known(),"Uncertain full-HP threshold certified Shadow Shield");
        check(!damage(fixture(shield(),"",Known.unknown()),Critical.BOTH).fraction().known(),"Missing full-HP evidence certified Shadow Shield");
        check(damage(fixture(shield(),"",known(new Interval(.8,.999))),Critical.BOTH).points().known(),"Proven below-full HP interval lost coverage");
        check(damage(fixture(shield(),"bounded-target-max"),Critical.BOTH).fraction().known(),"Justified public max-HP bounds lost coverage");
        for(String ability:List.of("hugepower","aurabreak","neutralizinggas"))check(!damage(fixture(change(shield(),"targetAbility",ability),""),Critical.BOTH).fraction().known(),"Unsupported ability silently certified: "+ability);
        for(String item:List.of("expertbelt","choiceband"))check(!damage(fixture(change(shield(),"item",item),""),Critical.BOTH).fraction().known(),"Unsupported item silently certified: "+item);
        check(!damage(fixture(change(shield(),"move","shadowball"),""),Critical.BOTH).fraction().known(),"Unsupported move gained composition coverage");
        var terrain=change(shield(),"terrain","psychicterrain");
        check(damage(fixture(terrain,""),Critical.BOTH).points().known(),"Source-proven grounded Psychic Terrain boost lost coverage");
        check(damage(fixture(change(terrain,"ability","levitate"),""),Critical.BOTH).points().known(),"Source-proven Levitate terrain exception lost coverage");
        var stopped=damage(fixture(terrain,"protect"),Critical.BOTH);
        check(stopped.fraction().known()&&stopped.fraction().value().high()==0,"Proven Protect required an irrelevant terrain boost");
        System.out.println("Modifier composition evidence regressions PASS: "+checks+" checks; public bindings, HP thresholds, unsupported effects and real receipt expiration");
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/modifier_composition_oracle.cjs"));
        var nativeData=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";const Battle=require('./sim/battle').Battle,rows=exports.run(Battle);return JSON.stringify({rows,exchanges:exports.exchanges(Battle,rows)});})()").asString()).getAsJsonObject();
        var rows=nativeData.getAsJsonArray("rows");
        check(rows.size()==66,"Native composition configuration inventory changed");int outcomes=0;
        for(var value:rows){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(Critical critical:Critical.values()){
                var values=new ArrayList<Integer>();
                for(var sample:row.getAsJsonArray("values")){var draw=sample.getAsJsonObject();boolean crit=draw.get("crit").getAsBoolean();
                    if(critical==Critical.BOTH||crit==(critical==Critical.CRITICAL))values.add(draw.get("damage").getAsInt());}
                int low=Collections.min(values),high=Collections.max(values);var actual=damage(f,critical);
                check(actual.fraction().known(),"Native composition became UNKNOWN: "+c.s("id")+" "+actual.unknown());
                if(high==0)check(actual.fraction().value().equals(new Interval(0,0)),"Immunity ran composed damage");
                else check(actual.points().known()&&actual.points().value().equals(new IntRange(low,high)),"Native composition extrema differ: "+c.s("id")+" enemy="+c.b("enemy")+" "+critical+" "+actual+" native="+values);
            }
            outcomes+=row.getAsJsonArray("values").size();
        }
        check(outcomes==2112,"Native critical/roll inventory changed");
        var exchanges=nativeData.getAsJsonArray("exchanges");check(exchanges.size()==8,"Native queued composition inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var result=evaluate(fixture(c,""));
            check(result.utility().known()&&result.lines().stream().anyMatch(line->matches(line,row)),"Composed queued native exchange excluded: "+c.s("id")+" enemy="+c.b("enemy")+" "+result+" / "+row);
        }
        System.out.println("Modifier composition native differential PASS: 66 configurations, 2112 critical/roll outcomes, 8 queued exchanges; exact supported extrema including grounded Psychic Terrain for both roles");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var actor=role.equals("self")?line.state().self():line.state().opponent();var hp=actor.hp().value();double nativeHp=row.get(role+"Hp").getAsDouble();
            if(nativeHp<hp.low()-1e-10||nativeHp>hp.high()+1e-10)return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(actor.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
}
