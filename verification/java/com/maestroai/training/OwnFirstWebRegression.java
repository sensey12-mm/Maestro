package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Native first-entry effects and fresh owner evidence; public observations
 * remain unchanged. These are source/profile tests, not installed acceptance. */
final class OwnFirstWebRegression {
    private static final UUID DONOR=new UUID(0,1),OWN=new UUID(0,2);
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static OwnRequestTransport.Pokemon mon(boolean incoming,boolean active,String item,String ability,List<String> types){
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(incoming?OWN:DONOR,"p1: Declared","300/400",active,
            incoming?ability:"download",incoming?types:List.of("Bug","Steel")),100,
            Map.of("atk",222,"def",200,"spa",250,"spd",200,"spe",201),incoming?item:"choicescarf",
            incoming?types:List.of("Bug","Steel"),"",false,incoming?"Mewtwo":"Genesect");
    }
    private static OwnRequestTransport.Request request(boolean entered,String item,String ability,List<String> types){
        return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
            List.of(mon(false,!entered,item,ability,types),mon(true,entered,item,ability,types)),new OwnZMoveEvidence.Choice(
                List.of(new OwnZMoveEvidence.MoveSlot(entered?"nastyplot":"uturn",20,false,entered?"self":"normal")),Map.of(),0));
    }
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n"
        +"|switch|p1a: "+DONOR+"|Genesect, "+DONOR+"|100/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
        +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n"
        +"|-sidestart|p1: Declared|move: Sticky Web\n|switch|p1a: "+OWN+"|Mewtwo, "+OWN+"|75/100\n"
        +"|-activate|p1a: "+OWN+"|move: Sticky Web\n|-unboost|p1a: "+OWN+"|spe|1\n|turn|2";}
    private static OwnXerneasRegression.Fixture fixture(String h,String item,String ability,List<String> types,boolean fresh,boolean catalog,RuntimeDomain domain){
        return OwnXerneasRegression.fixture(h,List.of(request(false,item,ability,types),request(true,item,ability,types)),2,"p1",OWN,fresh,"native-registered",catalog,domain);
    }
    private static OwnXerneasRegression.Fixture fixture(String h){return fixture(h,"mewtwonitey","pressure",List.of("Psychic"),true,true,null);}
    private static boolean supported(OwnXerneasRegression.Fixture f){return f.effective().missing().isEmpty()&&f.effective().evidence().profile().stats().size()==5;}
    private static Actor actor(OwnXerneasRegression.Fixture f){var e=f.effective();var o=e.evidence();
        return new Actor("self",new BattleSnapshot.Combatant("Declared",o.hp().value().low(),o.profile().types().value(),e.stages(),List.of(),o.item().value(),o.ability().value(),false),o.hp(),e.stages(),o.status());}
    static void run(){
        var before=fixture(history());var raw=before.fields().update(before.protocol(),before.provider());
        check(!before.ledger().effective(before.lease(),"self",OWN,false,before.domain(),before.protocol(),before.own(),raw).missing().isEmpty(),"Original first-Web own-state rejection was not reproduced");
        var good=fixture(history());check(supported(good)&&good.field().hasOwnedForm(),"Certified first Web entry stayed UNKNOWN: "+good.effective().missing());
        check(good.own().unsupportedEffects()&&!good.own().ambiguous()&&good.provider().observation("p1a")==good.own(),"Own entry changed spectator identity/effect caveats");
        check(good.effective().stages().get("spe")==-1&&good.effective().stages().get("atk")==0,"First entry erased Web or inherited donor stages");
        check(good.effective().evidence().profile().stat("spe").value().low()==201&&good.effective().evidence().hp().value().low()==.75,"First entry reused donor stats or HP");
        check(good.execution().priority("nastyplot","self").known()&&good.execution().speed(good.effective().evidence().profile(),actor(good)).value().low()==134,"First entry did not reach source-scoped speed/setup");
        int badCase=0;for(String bad:List.of(
            history().replace("|-sidestart|p1: Declared|move: Sticky Web\n",""),
            history().replace("|-sidestart|p1: Declared","|-sidestart|p2: Foe"),
            history().replace("|spe|1","|spe|2"),history().replace("|spe|1","|atk|1"),
            history().replace("|-unboost|p1a: "+OWN+"|spe|1\n",""),
            history().replace("|move: Sticky Web\n|-unboost", "|move: Sticky Web|[from] ability: Foreign\n|-unboost"),
            history().replace("|-activate|p1a: "+OWN,"|move|p1a: "+OWN+"|Nasty Plot|p1a: "+OWN+"\n|-activate|p1a: "+OWN),
            history().replace("|turn|2","|-activate|p1a: "+OWN+"|ability: Foreign\n|turn|2"),
            history().replace("|turn|2","|-activate|p1a: "+OWN+"|move: Sticky Web\n|-unboost|p1a: "+OWN+"|spe|1\n|turn|2"),
            history().replace("|turn|2","|-start|p1a: "+OWN+"|Gastro Acid\n|turn|2"),
            history().replace("|turn|2","|-start|p1a: "+OWN+"|Substitute\n|turn|2"),
            history().replace("|turn|2","|-damage|p1a: "+OWN+"|0/100\n|faint|p1a: "+OWN+"\n|turn|2"),
            history().replace("|Mewtwo, "+OWN+"|75/100","|Mewtwo, "+OWN+"|0 fnt"),
            history().replace("|Mewtwo, "+OWN,"|Mewtwo-Mega-Y, "+OWN),
            history().replace("|switch|p1a: "+DONOR+"|Genesect, "+DONOR+"|100/100\n",""),
            history().replace("|switch|p1a: "+OWN,"|move|p1a: "+DONOR+"|Baton Pass|p1a: "+DONOR+"\n|switch|p1a: "+OWN),
            history().replace("|turn|2","|switch|p2a: Foe|Mewtwo|100/100\n|turn|2"),
            history().replace("|turn|2","|-start|p2a: Foe|Substitute\n|turn|2")))
            check(!supported(fixture(bad)),"Missing/foreign/late Web, copied state, suppression, survival or opposing ambiguity gained coverage: case "+badCase++);
        var noActivation=fixture(history().replace("|-activate|p1a: "+OWN+"|move: Sticky Web\n",""));
        check(!noActivation.field().hasOwnedForm(),"A plain observed stage drop manufactured a first-entry Web witness");
        for(var types:List.of(List.of("Flying"),List.of("Psychic","Flying")))
            check(!supported(fixture(history(),"none","pressure",types,true,true,null)),"Flying acquired impossible first Web coverage");
        check(!supported(fixture(history(),"none","levitate",List.of("Psychic"),true,true,null)),"Levitate acquired impossible first Web coverage");
        check(!supported(fixture(history(),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Active Boots acquired impossible Web coverage");
        String room=history().replace("|-sidestart|p1", "|-fieldstart|move: Magic Room\n|-sidestart|p1");
        check(supported(fixture(room,"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Magic Room did not suppress Boots at entry");
        check(supported(fixture(room.replace("|-fieldstart|move: Magic Room", "|-fieldstart|move: Magic Room|[of] p2a: Foe"),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Original attributed Magic Room did not suppress Boots");
        check(!supported(fixture(room.replace("|-fieldstart|move: Magic Room", "|-fieldstart|move: Magic Room|[of] p2a: Foreign"),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Foreign Room attribution gained entry suppression");
        check(supported(fixture(room.replace("|turn|2","|-fieldend|move: Magic Room\n|turn|2"),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Expired Room erased proven past Web entry");
        check(!supported(fixture(history().replace("|turn|2","|-fieldstart|move: Magic Room\n|turn|2"),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Later Room start retroactively suppressed Boots at entry");
        check(!supported(fixture(room.replace("|switch|p1a: "+OWN,"|-fieldend|move: Magic Room\n|switch|p1a: "+OWN),"heavydutyboots","pressure",List.of("Psychic"),true,true,null)),"Room ended before entry still suppressed Boots");
        check(!supported(fixture(history(),"none","pressure",List.of("Psychic"),false,true,null)),"Stale owner gained first entry");
        check(!supported(fixture(history(),"none","pressure",List.of("Psychic"),true,false,null)),"Missing source catalog gained first entry");
        var foreign=fixture(history());check(foreign.ledger().rebindOwnReentry(good.lease(),foreign.domain(),foreign.protocol(),foreign.own(),foreign.base())==foreign.base(),"Foreign owning lease rebound first entry");
        for(int mode=0;mode<4;mode++){
            var f=fixture(history());var execution=f.execution();var cert=execution.speedCoverage(f.effective().evidence().profile(),actor(f));check(cert.supported(),"First Web entry could not issue a speed certificate");
            if(mode==0)f.ledger().endChoice();else if(mode==1)f.live().set(false);else if(mode==2)f.fields().reset();else f.protocol().reset();
            boolean denied=false;try{denied=!execution.priority("nastyplot","self").known()&&!cert.supported();}catch(IllegalArgumentException expired){denied=true;}
            check(denied,"Expired owner/source/field/protocol retained first-entry Web coverage");
        }
        var f=fixture(history());var scopes=ExecutionCoverage.Scopes.receipt(op->op==ExecutionCoverage.Operation.DAMAGE?Known.unknown():Known.of(true,"declared"),()->true);
        check(!f.execution().withScopes(scopes).priority("nastyplot","self").known(),"First Web entry bypassed its source catalog through priority");
        System.out.println("Own first Web evidence PASS: "+checks+" checks; exact entry effects, grounding, entry-time item suppression, provenance, privacy and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        boolean shift=engine.eval("js","!!require('./data/abilities').Abilities.fairyaura.onPreStart").asBoolean();
        String source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/own_first_web_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle,"+OwnXerneasRegression.definitions(shift)+"));})()").asString()).getAsJsonArray();
        check(rows.size()==216,"Native first-entry inventory changed");int bounded=0,guarded=0,caught=0;
        for(var value:rows){var row=value.getAsJsonObject();var requests=new ArrayList<OwnRequestTransport.Request>();
            row.getAsJsonArray("requests").forEach(q->requests.add(OwnRequestJson.parse("|request|"+q)));
            int turn=row.get("turn").getAsInt();String side=row.get("side").getAsString();UUID id=UUID.fromString(row.get("uuid").getAsString());
            var f=OwnXerneasRegression.fixture(row.get("public").getAsString(),requests,turn,side,id,true,shift?"shift-registered":"native-registered",true,null);
            if(row.get("forced").getAsBoolean()){
                check(!supported(f)&&!f.field().hasOwnedForm(),"Native lethal first entry gained a current MOVE witness");guarded++;continue;
            }
            check(supported(f),"Native first entry failed "+row.get("config")+": "+f.effective().missing()+" "+f.field().unknown());bounded++;
            var e=f.effective();var own=e.evidence();
            for(String stat:List.of("atk","def","spa","spd","spe"))check(own.profile().stat(stat).value().low()==row.getAsJsonObject("stats").get(stat).getAsInt(),"First-entry stat differs: "+stat);
            check(own.hp().value().low()==row.get("hp").getAsDouble()/row.get("maxHp").getAsDouble(),"First-entry HP/hazard rounding differs from native");
            check(own.status().value().equals(row.get("status").getAsString()),"First-entry poisoning differs from native");
            check(own.item().value().equals(row.get("item").getAsString())&&own.ability().value().equals(row.get("ability").getAsString()),"First-entry modifiers reused the donor");
            var stages=new TreeMap<String,Integer>();row.getAsJsonObject("stages").entrySet().forEach(a->stages.put(a.getKey(),a.getValue().getAsInt()));
            check(e.stages().equals(stages),"First entry lost native Web/Download or retained donor stages");
            var types=new ArrayList<String>();row.getAsJsonArray("types").forEach(t->types.add(t.getAsString().toLowerCase(Locale.ROOT)));
            check(own.profile().types().value().equals(types),"First-entry typing differs from native");
            check(f.field().magicRoom().known()&&f.field().magicRoom().value()==row.get("magicRoom").getAsBoolean(),"Current Room was replaced with past entry Room");
            var execution=f.execution();var speed=execution.speed(own.profile(),actor(f));
            check(speed.known()&&speed.value().low()==row.get("speed").getAsInt()&&speed.value().high()==row.get("speed").getAsInt(),"First-entry speed differs from native: "+row.get("config"));
            var ui=new AtomicReference<>(requests.getLast().selection());var z=f.ledger().ownZMove(f.lease(),f.domain(),f.own(),f.field(),ui::get);
            check(z!=null&&z.current(),"First entry lost current native UI receipt");
            if(row.get("caught").getAsBoolean()){
                caught++;check(f.field().hasOwnedForm()&&f.own().unsupportedEffects(),"First Web entry omitted its independent witness or erased public caveats");
                if(!row.get("species").getAsString().startsWith("Xerneas")){
                    var raw=f.fields().update(f.protocol(),f.provider());
                    var unbound=f.ledger().effective(f.lease(),"self",id,false,f.domain(),f.protocol(),f.own(),raw);
                    check(!unbound.missing().isEmpty()&&unbound.evidence().profile().stats().isEmpty(),"Original first Web rejection not reproduced by withholding the witness");
                }
            }
            check(f.provider().observation(f.own().slot())==f.own(),"First entry replaced a spectator observation");
        }
        System.out.println("Own first Web native differential PASS: "+rows.size()+" entries; "+bounded+" bounded / "+guarded+" UNKNOWN, "+caught+" Web activations; both owner sides, six roster members, Flying/Levitate, Boots/Room expiration, stacked hazards, pivots and lethal entries");
    }
    static void runtime(RuntimeDomain domain){
        var f=fixture(history(),"mewtwonitey","pressure",List.of("Psychic"),true,true,domain);
        check(supported(f)&&f.execution().speed(f.effective().evidence().profile(),actor(f)).known(),"Initialized native source failed first Web own-state/speed");
        var execution=f.execution();f.ledger().endChoice();check(!execution.sourceCurrent(),"Expired runtime owner retained first Web state");
        System.out.println("Own first Web native runtime profile PASS: actual initialized source, synthetic first entry and owner withdrawal; no installed acceptance claim");
    }
    static Runnable withdrawal(RuntimeDomain domain){
        var f=fixture(history(),"mewtwonitey","pressure",List.of("Psychic"),true,true,domain);
        check(supported(f),"Initialized source did not certify the first Web withdrawal control");
        var execution=f.execution();var certificate=execution.speedCoverage(f.effective().evidence().profile(),actor(f));
        return ()->check(!execution.priority("nastyplot","self").known()&&!certificate.supported()&&!f.field().ownedSourceCurrent(),"Changed initialized definition retained first-entry state or a closing certificate");
    }
    public static void main(String[] args)throws Exception{
        OwnXerneasRegression.prepare(Path.of(args[1]));run();
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false")
            .option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString())
            .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
