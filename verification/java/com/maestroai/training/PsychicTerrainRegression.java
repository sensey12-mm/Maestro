package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.ModifierCompositionRegression.*;

/** Reuses current owner/spectator fixtures; conditional stats are not inferred builds. */
final class PsychicTerrainRegression {
    private static final String SOURCE="declared native Psychic Terrain conditions";
    private static final Known<Boolean> YES=known(true);
    private static int checks;
    private static <T> Known<T> known(T value){return Known.of(value,SOURCE);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Case terrain(){return change(shield(),"terrain","psychicterrain");}
    private static Case mode(Case c,String mode){var d=c.data().deepCopy();d.addProperty("terrainMode",mode);d.addProperty("terrain",mode.equals("none")?"none":"psychicterrain");return new Case(d);}
    private static Case types(Case c,String key,String... types){var d=c.data().deepCopy();var a=new JsonArray();for(String type:types)a.add(type);d.add(key,a);return new Case(d);}
    private static Actor user(Fixture f){return f.config().b("enemy")?f.state().opponent():f.state().self();}
    private static Actor target(Fixture f){return f.config().b("enemy")?f.state().self():f.state().opponent();}
    private static Damage damageWith(Fixture f,ScopedExecution execution){return execution.damage(f.config().s("move"),f.profiles().get(f.config().user()),user(f),f.profiles().get(f.config().target()),target(f),Critical.BOTH);}
    private static ShadowMechanicsEvaluation.Ranking evaluateWith(Fixture f,ScopedExecution execution){
        String action="move:"+f.config().action();var plan=new ShadowMechanicsEvaluation.Plan(action,List.of(List.of(new ShadowMechanicsEvaluation.Turn(action,f.config().reply(),null))),SOURCE);
        return ShadowMechanicsEvaluation.compare(f.snapshot(),f.mechanics(),f.profiles(),List.of(plan),known(false),Critical.BOTH,action,execution).rankings().getFirst();
    }
    static void run(){
        var priority=change(change(terrain(),"move","quickattack"),"targetAbility","runaway");
        for(Case c:List.of(terrain(),enemy(terrain()),priority,enemy(priority))){
            var f=fixture(c,"");check(damage(f,Critical.BOTH).fraction().known(),"Source-proven terrain outcome remains UNKNOWN");
            check(evaluate(f).utility().known(),"Source-proven terrain exchange remains UNKNOWN");
            for(String missing:List.of("own-item","own-ability","opponent-item","opponent-ability","own-transport","damage-scope",
                    "attacker-types","target-types","level","attack-stat","target-max","status","suppression","ambiguous",
                    "forged-opponent","gravity","magnet-rise","unsupported-terrain"))
                check(!damage(fixture(c,missing),Critical.BOTH).fraction().known(),"Terrain bypassed grounding/source/public proof: "+missing);
            var entry=fixture(c,"entry-scope");
            check(damage(entry,Critical.BOTH).fraction().known(),"Damage incorrectly required unrelated entry scope");
            check(!entry.execution().grounded(c.user(),entry.profiles().get(c.user()),false).known(),"Entry interface bypassed its own scope");
            var receipt=coverage(f);check(receipt.supported(),"Fresh terrain certificate unavailable");f.owner().endChoice();
            check(!receipt.supported()&&!damage(f,Critical.BOTH).fraction().known()&&!evaluate(f).utility().known(),"Expired real owner receipt retained terrain outcome");
            var source=fixture(c,"");var sourceReceipt=coverage(source);source.current().set(false);
            check(!sourceReceipt.supported()&&!damage(source,Critical.BOTH).fraction().known(),"Expired source receipt retained terrain outcome");
            var field=fixture(c,"");field.fields().reset();boolean rejected=false;try{damage(field,Critical.BOTH);}catch(IllegalArgumentException stale){rejected=true;}
            check(rejected,"Expired field receipt retained terrain outcome");
            var closing=fixture(c,"");
            var expiring=closing.execution().withScopes(ExecutionCoverage.Scopes.receipt(op->{
                if(op==ExecutionCoverage.Operation.SETUP)closing.current().set(false);return YES;},closing.current()::get));
            var withdrawn=evaluateWith(closing,expiring);
            check(!closing.current().get()&&!withdrawn.utility().known()&&withdrawn.lines().stream().allMatch(l->l.state().equals(closing.state())),"Closing receipt expiration leaked a terrain forecast");
        }
        for(boolean enemy:List.of(false,true)){
            Case c=enemy?enemy(terrain()):terrain();
            var active=damage(fixture(c,""),Critical.BOTH).points().value();
            var absent=damage(fixture(mode(c,"none"),""),Critical.BOTH).points().value();
            check(active.high()>absent.high(),"Grounded Psychic Terrain failed to boost damage");
            check(damage(fixture(mode(c,"ended"),""),Critical.BOTH).points().value().equals(absent),"Ended terrain retained its boost");
            check(damage(fixture(mode(c,"restart"),""),Critical.BOTH).points().value().equals(active),"Restarted terrain failed to reestablish its boost");
            for(Case airborne:List.of(types(c,"types","psychic","flying"),change(c,"ability","levitate")))
                check(damage(fixture(airborne,""),Critical.BOTH).points().value().equals(damage(fixture(mode(airborne,"none"),""),Critical.BOTH).points().value()),"Airborne attacker gained terrain boost");
            for(String item:List.of("ironball","airballoon"))check(!damage(fixture(change(c,"item",item),""),Critical.BOTH).fraction().known(),"Unsupported grounding item gained scope: "+item);
            check(!damage(fixture(change(c,"ability","neutralizinggas"),""),Critical.BOTH).fraction().known(),"Unsupported ability suppression gained scope");
            check(!damage(fixture(c,"",known(new Interval(.999,1))),Critical.BOTH).fraction().known(),"Unproved Shadow Shield threshold became known under terrain");
            var history=c.data().deepCopy();history.addProperty("witnessSetter",true);var witnessed=new Case(history);
            check(damage(fixture(witnessed,""),Critical.BOTH).points().value().equals(active),"Witnessed past terrain setter poisoned current coverage");
            check(!damage(fixture(witnessed,"indirect-setter"),Critical.BOTH).fraction().known(),"Indirect setter origin gained execution scope");
            var noScope=fixture(witnessed,"damage-scope");check(!damage(noScope,Critical.BOTH).fraction().known(),"Past setter replaced runtime operation proof");
            fieldTransitions(c);
        }
        var stall=fixture(change(priority,"ability","stall"),"");
        check(stall.execution().priority("quickattack","self").value()==.9&&damage(stall,Critical.BOTH).fraction().value().high()==0,
                "Fractional order priority disabled Psychic Terrain protection");
        check(!CobblemonMechanics.supportedMoveIds().contains("psychicterrain"),"Past field setter became a supported selectable action");
        System.out.println("Psychic Terrain evidence regressions PASS: "+checks+" checks; actor/target grounding, field reissue, unsupported suppression and receipt withdrawal");
    }
    private static void fieldTransitions(Case c){
        var f=fixture(c,"");var original=coverage(f);var active=damage(f,Critical.BOTH).points().value();
        appendProtocol(f,"|-fieldend|move: Psychic Terrain");
        var ended=f.fields().update(f.protocol(),f.provider());
        boolean rejected=false;try{original.supported();}catch(IllegalArgumentException stale){rejected=true;}
        check(rejected,"Old terrain receipt survived a witnessed field change");
        var execution=reissue(f,ended);
        check(ended.terrain().value().equals("none")&&damageWith(f,execution).points().value().equals(damage(fixture(mode(c,"none"),""),Critical.BOTH).points().value()),"Fresh field-end receipt retained stale terrain arithmetic");
        appendProtocol(f,"|-fieldstart|move: Psychic Terrain");
        var restarted=f.fields().update(f.protocol(),f.provider());
        check(damageWith(f,reissue(f,restarted)).points().value().equals(active),"Fresh restarted terrain receipt failed to restore arithmetic");
        rejected=false;try{damageWith(f,execution);}catch(IllegalArgumentException stale){rejected=true;}
        check(rejected,"Ended-field calculation survived a new field start");
    }
    private static void appendProtocol(Fixture f,String event){
        f.history().add("update\n"+event);
        f.protocol().update(f.history());f.provider().update(f.protocol());
    }
    private static ScopedExecution reissue(Fixture f,PublicFieldEvidence.Capture field){
        var c=f.config();var own=f.provider().observation("p1a");var foe=f.provider().observation("p2a");
        return new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(own,known(c.b("enemy")?c.s("targetItem"):c.s("item")),known(c.b("enemy")?c.s("targetAbility"):c.s("ability")),YES,f.lease()),
                "opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability())),ExecutionCoverage.Scopes.receipt(op->YES,f.current()::get));
    }
    static void differential(Context engine) throws Exception {
        String source=Files.readString(Path.of("verification/psychic_terrain_oracle.cjs"));
        var nativeData=JsonParser.parseString(engine.eval("js","(()=>{const exports={};"+source+";return JSON.stringify(exports.run(require('./sim/battle').Battle));})()").asString()).getAsJsonObject();
        var rows=nativeData.getAsJsonArray("damage");check(rows.size()==52,"Native terrain damage inventory changed");int outcomes=0;
        for(var value:rows){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var f=fixture(c,"");
            for(Critical critical:Critical.values()){
                var values=new ArrayList<Integer>();for(var sample:row.getAsJsonArray("values")){var draw=sample.getAsJsonObject();
                    if(critical==Critical.BOTH||draw.get("crit").getAsBoolean()==(critical==Critical.CRITICAL))values.add(draw.get("damage").getAsInt());}
                int low=Collections.min(values),high=Collections.max(values);var actual=damage(f,critical);
                check(actual.fraction().known(),"Native terrain arithmetic became UNKNOWN: "+c.s("id")+" "+actual.unknown());
                check(high==0?actual.fraction().value().high()==0:actual.points().known()&&actual.points().value().equals(new IntRange(low,high)),
                        "Native terrain extrema differ: "+c.s("id")+" enemy="+c.b("enemy")+" "+critical+" "+actual+" native="+values);
            }outcomes+=row.getAsJsonArray("values").size();
        }
        check(outcomes==1664,"Native terrain critical/roll inventory changed");
        var exchanges=nativeData.getAsJsonArray("exchanges");check(exchanges.size()==62,"Native terrain queued inventory changed");
        for(var value:exchanges){var row=value.getAsJsonObject();var c=new Case(row.getAsJsonObject("config"));var result=evaluate(fixture(c,c.b("protect")?"protect":""));
            check(result.utility().known()&&result.lines().stream().anyMatch(line->matches(line,row)),"Native terrain exchange excluded: "+c.s("id")+" enemy="+c.b("enemy")+" "+result+" / "+row);
        }
        System.out.println("Psychic Terrain native differential PASS: 52 configurations, 1664 critical/roll outcomes, 62 queued exchanges; exact extrema, field changes, independent grounding and priority/self-target behavior");
    }
    private static boolean matches(ShadowMechanicsEvaluation.Line line,JsonObject row){
        double utility=row.get("utility").getAsDouble();if(utility<line.utility().value().low()-1e-10||utility>line.utility().value().high()+1e-10)return false;
        for(String role:List.of("self","foe")){var actor=role.equals("self")?line.state().self():line.state().opponent();double nativeHp=row.get(role+"Hp").getAsDouble();var hp=actor.hp().value();
            if(nativeHp<hp.low()-1e-10||nativeHp>hp.high()+1e-10||!actor.status().value().equals(row.get(role+"Status").getAsString()))return false;
            for(var e:row.getAsJsonObject(role+"Stages").entrySet())if(!Objects.equals(actor.stages().get(e.getKey()),e.getValue().getAsInt()))return false;
        }return true;
    }
}
