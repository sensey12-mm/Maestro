package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Native own requests and spectator histories; opposing oracle stats never
 * enter the production public provider or the certified target profile. */
final class OwnMegaYRegression {
    private static final UUID OWN=new UUID(0,1),BENCH=new UUID(0,2);
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static RuntimeDomain domain(AtomicBoolean live,boolean catalog){
        var expected=Map.of("core","1".repeat(64),"tables","2".repeat(64),"speciesHooks","3".repeat(64),"catalog","4".repeat(64));
        var actual=new TreeMap<>(expected);if(!catalog)actual.put("catalog","0".repeat(64));
        return new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,actual),expected,true,live::get);
    }
    private static OwnRequestTransport.Pokemon mon(boolean mega){
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(OWN,"p1: Declared","200/353",true,
                mega?"insomnia":"pressure",List.of("Psychic")),100,
                mega?Map.of("atk",336,"def",176,"spa",424,"spd",276,"spe",316):Map.of("atk",256,"def",216,"spa",344,"spd",216,"spe",296),
                "mewtwonitey",List.of("Psychic"),"",false);
    }
    private static OwnRequestTransport.Request request(boolean mega){return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(mon(mega)));}
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n"
            +"|switch|p1a: "+OWN+"|Mewtwo, "+OWN+"|57/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
            +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n"
            +"|-boost|p1a: "+OWN+"|spa|2\n|detailschange|p1a: "+OWN+"|Mewtwo-Mega-Y, "+OWN+"\n"
            +"|-mega|p1a: "+OWN+"|Mewtwo|Mewtwonite Y\n|move|p2a: Foe|Bulk Up|p2a: Foe\n|turn|2";}
    private record Fixture(OwnRequestTransport ledger,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            AtomicBoolean live,PublicProtocolEvidence protocol,PublicMechanicsEvidence provider,
            PublicFieldEvidence fields,PublicFieldEvidence.Capture base,PublicFieldEvidence.Capture field,
            PublicMechanicsEvidence.Observation own,PublicMechanicsEvidence.Observation foe,UUID uuid) {
        OwnRequestTransport.Effective effective(){return ledger.effective(lease,"self",uuid,false,domain,protocol,own,field);}
    }
    private static Fixture fixture(String history,List<OwnRequestTransport.Request> requests,int turn,String side,
            UUID uuid,AtomicBoolean live,boolean catalog,boolean fresh){
        return fixture(history,requests,turn,side,uuid,live,catalog,fresh,null);
    }
    private static Fixture fixture(String history,List<OwnRequestTransport.Request> requests,int turn,String side,
            UUID uuid,AtomicBoolean live,boolean catalog,boolean fresh,RuntimeDomain nativeDomain){
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),token=new Object();ledger.owner(battle,actor,side);
        ledger.dispatched(battle,actor,side,token,()->requests.getFirst());
        for(int t=1;t<turn;t++){
            ledger.turnDispatched(battle,t);ledger.turnFinished(battle,t);
            if(!fresh)continue;
            token=new Object();var r=requests.get(Math.min(t,requests.size()-1));ledger.dispatched(battle,actor,side,token,()->r);
        }
        ledger.turnDispatched(battle,turn);
        var lease=ledger.beginChoice(battle,actor,token,uuid,false,()->true);
        var protocol=new PublicProtocolEvidence();protocol.update(List.of(history));
        var provider=new PublicMechanicsEvidence(id->id.equals("mewtwo")?new PublicMechanicsEvidence.Species(
                Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null):null);
        provider.update(protocol);var fields=new PublicFieldEvidence();var base=fields.update(protocol,provider);
        var own=provider.observation(side+"a");var foe=provider.observation(side.equals("p1")?"p2a":"p1a");
        var domain=nativeDomain==null?domain(live,catalog):nativeDomain;var field=ledger.rebindOwnMegaY(lease,domain,protocol,own,base);
        return new Fixture(ledger,lease,domain,live,protocol,provider,fields,base,field,own,foe,uuid);
    }
    private static Fixture simple(String history,OwnRequestTransport.Request current,boolean catalog,boolean fresh){
        return fixture(history,List.of(request(false),current),2,"p1",OWN,new AtomicBoolean(true),catalog,fresh);
    }
    private static boolean supported(Fixture f){return f.effective().missing().isEmpty()&&!f.effective().evidence().profile().stats().isEmpty();}
    private static OwnRequestTransport.Request changed(OwnRequestTransport.Pokemon p){return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(p));}
    private static Map<String,Integer> stages(JsonObject o){var result=new TreeMap<String,Integer>();o.entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsInt()));return result;}
    private record Pipeline(Map<String,Profile> profiles,ScopedExecution execution,BattleSnapshot snapshot,PublicMechanics mechanics){}
    private static Pipeline pipeline(Fixture f){
        var effective=f.effective();var e=effective.evidence();
        var own=new BattleSnapshot.Combatant("Mewtwo-Mega-Y",e.hp().value().low(),e.profile().types().value(),
                effective.stages(),List.of(),e.item().value(),e.ability().value(),false);
        var observer=new PublicBattleObserver();f.protocol.mechanicsEvents().forEach(observer::accept);
        var foe=PublicShadowSnapshot.opponent(observer,f.foe.slot());
        var target=f.provider.bind("opponent",f.foe,new PublicMechanicsEvidence.Scope(f.domain.effectiveState(),f.field.trickRoom()));
        var execution=new ScopedExecution(f.field,Map.of("self",new ScopedExecution.Binding(f.own,e.item(),e.ability(),e.fresh(),f.lease),
                "opponent",new ScopedExecution.Binding(f.foe,f.foe.item(),f.foe.ability())),f.domain.scopes());
        var snapshot=new BattleSnapshot(f.lease.turn(),own,foe,List.of(),List.of(),List.of("psystrike","icebeam","fireblast","nastyplot"),List.of(),Map.of(),false,null);
        var context=new PublicTransitionState.Context(e.hp(),f.foe.hp(),e.status(),f.foe.status(),
                f.field.hazards(f.own.slot().substring(0,2),f.domain.effectiveState()),
                f.field.hazards(f.foe.slot().substring(0,2),f.domain.effectiveState()),Map.of(),Map.of(),Set.of("APPEARANCE_ONLY"));
        var mechanics=new PublicMechanics(Map.of(),f.field.stages(f.foe,f.domain.effectiveState()),Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
        return new Pipeline(Map.of("self",e.profile(),"opponent",target.profile()),execution,snapshot,mechanics);
    }
    private static ShadowMechanicsEvaluation.Comparison compare(Pipeline p){
        var plans=p.snapshot.legalMoves().stream().map(m->new ShadowMechanicsEvaluation.Plan("move:"+m,List.of(List.of(
                new ShadowMechanicsEvaluation.Turn("move:"+m,"bulkup",null))),"native own Mega-Y / witnessed public reply")).toList();
        return ShadowMechanicsEvaluation.compare(p.snapshot,p.mechanics,p.profiles,plans,p.execution.field().trickRoom(),Critical.BOTH,"move:nastyplot",p.execution);
    }
    static void run(){
        var good=simple(history(),request(true),true,true);
        check(supported(good),"Exact own Mega-Y did not rebind: "+good.effective().missing());
        check(good.own.unsupportedEffects()&&!good.own.item().known()&&!good.own.ability().known(),"Own request rewrote spectator observation");
        check(good.effective().evidence().profile().stat("spa").value().low()==424&&good.effective().stages().get("spa")==2,"Own stats or retained stages came from pre-Mega state");
        boolean old=false;try{good.base.requireCurrent(2);}catch(IllegalArgumentException expected){old=true;}check(old,"Rebinding retained prior field certificates");
        var goodComparison=compare(pipeline(good));
        check(goodComparison.rankings().stream().anyMatch(r->r.action().equals("move:nastyplot")&&r.utility().known()),"Certified Mega-Y own setup pipeline remained UNKNOWN: "+goodComparison.rankings());
        for(String broken:List.of(
                history().replace("|-mega|p1a: "+OWN+"|Mewtwo|Mewtwonite Y\n",""),
                history().replace("Mewtwonite Y","Mewtwonite X"),
                history().replace("Mewtwo-Mega-Y","Mewtwo-Mega-X"),
                history().replace("|detailschange|p1a: "+OWN,"|detailschange|p2a: Foe"),
                history().replace("|Mewtwo|Mewtwonite Y","|Mewtwo|Mewtwonite Y|unproved"),
                history().replace("|-mega|","|-formechange|"),
                history().replace("|-mega|","|turn|2\n|-mega|"),
                history().replace("|turn|2","|-start|p1a: "+OWN+"|Substitute\n|turn|2"),
                history().replace("|turn|2","|-transform|p1a: "+OWN+"|p2a: Foe\n|turn|2"),
                history().replace("|turn|2","|-mega|p2a: Foe|Mewtwo|Mewtwonite Y\n|turn|2"),
                history().replace("|turn|2","|move|p1a: "+OWN+"|Baton Pass|p1a: "+OWN+"\n|turn|2")))
            check(!supported(simple(broken,request(true),true,true)),"Unproved form/effect history gained own state");
        check(!supported(simple(history(),request(false),true,true)),"Old base ability became Mega-Y");
        check(!supported(simple(history(),request(true),false,true)),"Missing catalog/source scope gained own form");
        check(!supported(simple(history(),request(true),true,false)),"Old owner request gained post-Mega state");
        var wrong=mon(true);for(String item:List.of("none","mewtwonitex")){
            var p=new OwnRequestTransport.Pokemon(wrong.dto(),wrong.level(),wrong.baseStats(),item,wrong.baseTypes(),wrong.tera(),false);
            check(!supported(simple(history(),new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(p)),true,true)),"Wrong held item certified Mega-Y");
        }
        for(var p:List.of(
                new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(OWN,"p1: Declared","200/353",true,"pressure",List.of("Psychic")),100,wrong.baseStats(),wrong.item(),wrong.baseTypes(),"",false),
                new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(OWN,"p1: Declared","200/353",true,"insomnia",List.of("Ghost")),100,wrong.baseStats(),wrong.item(),List.of("Ghost"),"",false),
                new OwnRequestTransport.Pokemon(wrong.dto(),100,wrong.baseStats(),wrong.item(),List.of("Ghost"),"",false),
                new OwnRequestTransport.Pokemon(wrong.dto(),100,wrong.baseStats(),wrong.item(),wrong.baseTypes(),"Psychic",false),
                new OwnRequestTransport.Pokemon(wrong.dto(),100,wrong.baseStats(),wrong.item(),wrong.baseTypes(),"",true),
                new OwnRequestTransport.Pokemon(wrong.dto(),100,Map.of("spa",424),wrong.item(),wrong.baseTypes(),"",false),
                new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(OWN,"p1: Declared","400/353",true,"insomnia",List.of("Psychic")),100,wrong.baseStats(),wrong.item(),wrong.baseTypes(),"",false)))
            check(!supported(simple(history(),changed(p),true,true)),"Unproved own form request gained effective coverage");
        String cure="|-activate|p1a: "+OWN+"|ability: Insomnia\n|-curestatus|p1a: "+OWN+"|slp|[msg]\n";
        check(supported(simple(history().replace("|turn|2",cure+"|turn|2"),request(true),true,true)),"Exact public Insomnia Update cure did not bind fresh current status");
        for(String bad:List.of(cure.replace("Insomnia","Pressure"),cure.replace("|slp|","|par|"),cure.replace("|[msg]",""),cure.substring(0,cure.indexOf("|-curestatus"))))
            check(!supported(simple(history().replace("|turn|2",bad+"|turn|2"),request(true),true,true)),"Unproved activation/cure sequence gained effective coverage");
        var foreign=simple(history(),request(true),true,true);
        check(foreign.ledger.rebindOwnMegaY(good.lease,foreign.domain,foreign.protocol,foreign.own,foreign.field)==foreign.field,"Foreign owner lease rebound a field");
        boolean opponent=false;try{good.ledger.effective(good.lease,"opponent",OWN,false,good.domain,good.protocol,good.own,good.field);}catch(IllegalArgumentException expected){opponent=true;}
        check(opponent,"Own form witness became an opponent payload channel");
        for(int mode=0;mode<4;mode++){
            var f=simple(history(),request(true),true,true);var p=pipeline(f);
            if(mode==0)f.ledger.endChoice();else if(mode==1)f.live.set(false);else if(mode==2)f.fields.reset();else f.protocol.reset();
            boolean unknown=false;try{unknown=compare(p).rankings().stream().allMatch(r->!r.utility().known());}catch(IllegalArgumentException stale){unknown=true;}
            check(unknown,"Expired own/source/field/protocol retained a comparison");
        }
        var missing=simple(history(),request(true),true,true);var frame=pipeline(missing);
        var unknownScopes=ExecutionCoverage.Scopes.receipt(op->op==ExecutionCoverage.Operation.DAMAGE?Known.unknown():Known.of(true,"declared scope"),()->true);
        check(!frame.execution.withScopes(unknownScopes).setupCoverage(frame.profiles.get("self"),project(frame.snapshot,frame.mechanics).states().get("root").self(),"nastyplot").supported(),"Own form bypassed catalog/source scope for setup");
        System.out.println("Own Mega-Y evidence regressions PASS: "+checks+" checks; canonical own transition, unchanged spectator evidence, old-state withdrawal, foreign forms and receipt expiration");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/own_mega_y_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==146,"Native own Mega-Y inventory changed: "+rows.size());
        int rebound=0,unknown=0,rolls=0;
        for(var value:rows){
            var row=value.getAsJsonObject();String side=row.get("side").getAsString();UUID uuid=UUID.fromString(row.get("uuid").getAsString());
            var requests=new ArrayList<OwnRequestTransport.Request>();for(var r:row.getAsJsonArray("requests"))requests.add(OwnRequestJson.parse("|request|"+r));
            var f=fixture(row.get("public").getAsString(),requests,row.get("turn").getAsInt(),side,uuid,new AtomicBoolean(true),true,true);
            // Existing unproved freeze BeforeMove events remain outside scope.
            boolean expected=!row.get("public").getAsString().contains("|cant|");
            if(row.get("config").getAsJsonObject().get("status").getAsString().equals("par"))expected=true;
            if(row.get("config").getAsJsonObject().get("reentry").getAsBoolean())expected=true;
            if(!expected){check(!supported(f),"Unsupported execution history gained own form coverage");unknown++;continue;}
            check(supported(f),"Native own state failed: "+row.get("config")+" "+f.effective().missing()+" "+f.field.unknown());rebound++;
            var effective=f.effective();var e=effective.evidence();
            check(e.status().value().equals(row.get("status").getAsString()),"Own form invented status recovery");
            check(e.hp().value().low()==row.get("hp").getAsDouble()/row.get("maxHp").getAsDouble(),"Post-form HP came from stale request");
            check(effective.stages().equals(stages(row.getAsJsonObject("stages"))),"Mega retained/reset wrong stages");
            for(String stat:List.of("atk","def","spa","spd","spe"))check(e.profile().stat(stat).value().low()==row.getAsJsonObject("stats").get(stat).getAsInt(),"Post-Mega base stat differs: "+stat);
            var p=pipeline(f);var root=project(p.snapshot,p.mechanics).states().get("root");
            for(String move:List.of("psystrike","icebeam","fireblast")){
                var d=p.execution.damage(move,p.profiles.get("self"),root.self(),p.profiles.get("opponent"),root.opponent(),Critical.BOTH);
                var nativePoints=row.getAsJsonObject("points").getAsJsonArray(move);
                check(d.points().known()&&d.points().value().low()<=nativePoints.get(0).getAsInt()&&d.points().value().high()>=nativePoints.get(1).getAsInt(),"Public target bounds excluded native Mega-Y damage: "+move);rolls+=32;
            }
            var result=compare(p);boolean actionable=Set.of("none","par").contains(e.status().value());
            check(result.rankings().stream().filter(r->r.action().equals("move:nastyplot")).allMatch(r->r.utility().known()==actionable),"Status/action boundary changed after rebinding: "+e.status()+" "+result.rankings());
            check(f.provider.observation(side+"a")==f.own&&!f.own.confirmedSpecies().known(),"Owned form rewrote the spectator observation or promoted a public species identity");
            if(row.get("config").getAsJsonObject().get("reentry").getAsBoolean())
                check(f.own.ambiguous(),"Owned re-entry resolved spectator identity ambiguity");
        }
        System.out.println("Own Mega-Y native differential PASS: "+rows.size()+" transition/re-entry histories; "+rebound+" rebound / "+unknown+" UNKNOWN; "+rolls+" critical/roll outcomes within public-only target bounds; both owner sides, levels, shiny, status, stages and hazards");
    }
    static void runtime(RuntimeDomain domain){
        var f=fixture(history(),List.of(request(false),request(true)),2,"p1",OWN,new AtomicBoolean(true),true,true,domain);
        check(supported(f),"Verified runtime profile did not support owned Mega-Y: "+f.effective().missing());
        check(compare(pipeline(f)).rankings().stream().anyMatch(r->r.action().equals("move:nastyplot")&&r.utility().known()),"Actual runtime source scopes did not reach own Mega-Y setup comparison");
        f.ledger.endChoice();check(!supported(f),"Runtime own form retained expired owner evidence");
        System.out.println("Own Mega-Y runtime profile PASS: actual initialized source scopes, owned synthetic requests, public-only opponent and choice expiration");
    }
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString()).option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
