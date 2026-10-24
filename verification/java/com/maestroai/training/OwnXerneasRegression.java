package com.maestroai.training;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import com.maestroai.ai.SimulatorDomainBridge;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Original registration, native histories and finite own-only receipt checks. */
final class OwnXerneasRegression {
    private static final UUID OWN=new UUID(0,1),BENCH=new UUID(0,2);
    private static JsonObject definitions;
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void prepare(Path dependency)throws Exception {definitions=OwnXerneasRegistrationRegression.definitions(dependency);}
    private static RuntimeDomain domain(AtomicBoolean live,String mode,boolean catalog){
        var pins=Map.of("core","1".repeat(64),"tables","2".repeat(64),"speciesHooks","3".repeat(64),"catalog","4".repeat(64));
        var actual=new TreeMap<>(pins);actual.put("ownXerneas",SourceVerification.hash(mode));
        if(!catalog)actual.put("catalog","0".repeat(64));
        return new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,actual),pins,true,live::get);
    }
    private static OwnRequestTransport.Pokemon mon(UUID id,boolean active){
        boolean x=id.equals(OWN);return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Declared",
            "300/400",active,x?"fairyaura":"pressure",x?List.of("Fairy"):List.of("Psychic")),100,
            Map.of("atk",222,"def",200,"spa",250,"spd",200,"spe",200),x?"powerherb":"none",
            x?List.of("Fairy"):List.of("Psychic"),"",false,x?"Xerneas":"Mewtwo");
    }
    private static OwnRequestTransport.Request request(boolean bench){return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
        List.of(mon(OWN,!bench),mon(BENCH,bench)),new OwnZMoveEvidence.Choice(
            List.of(new OwnZMoveEvidence.MoveSlot(bench?"bulkup":"geomancy",20,false,"self")),Map.of(),0));}
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n"
        +"|switch|p1a: "+OWN+"|Xerneas, "+OWN+"|100/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
        +"|-formechange|p1a: "+OWN+"|Xerneas-Active|\n|-ability|p1a: "+OWN+"|Fairy Aura\n"
        +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n"
        +"|-boost|p1a: "+OWN+"|spa|2\n|turn|2\n|switch|p1a: "+BENCH+"|Mewtwo, "+BENCH+"|100/100\n|turn|3\n"
        +"|switch|p1a: "+OWN+"|Xerneas, "+OWN+"|75/100\n|-formechange|p1a: "+OWN+"|Xerneas-Active|\n"
        +"|-ability|p1a: "+OWN+"|Fairy Aura\n|turn|4";}
    private record Fixture(OwnRequestTransport ledger,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            AtomicBoolean live,PublicProtocolEvidence protocol,PublicMechanicsEvidence provider,PublicFieldEvidence fields,
            PublicFieldEvidence.Capture base,PublicFieldEvidence.Capture field,PublicMechanicsEvidence.Observation own,
            PublicMechanicsEvidence.Observation foe,OwnXerneasEvidence xerneas,UUID uuid){
        OwnRequestTransport.Effective effective(){return ledger.effective(lease,"self",uuid,false,domain,protocol,own,field);}
        ScopedExecution execution(){var e=effective().evidence();var x=new ScopedExecution(field,Map.of(
            "self",new ScopedExecution.Binding(own,e.item(),e.ability(),e.fresh(),lease),
            "opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability())),domain.scopes());
            return xerneas==null?x:x.withOwnXerneas(xerneas);}
    }
    private static Fixture fixture(String history,List<OwnRequestTransport.Request> requests,int turn,String side,UUID uuid,
            boolean fresh,String mode,boolean catalog,RuntimeDomain supplied){
        var live=new AtomicBoolean(true);var d=supplied==null?domain(live,mode,catalog):supplied;
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),token=new Object();ledger.owner(battle,actor,side);
        ledger.dispatched(battle,actor,side,token,()->requests.getFirst());
        for(int t=1;t<turn;t++){ledger.turnDispatched(battle,t);ledger.turnFinished(battle,t);
            if(fresh){token=new Object();var r=requests.get(Math.min(t,requests.size()-1));ledger.dispatched(battle,actor,side,token,()->r);}}
        ledger.turnDispatched(battle,turn);var current=requests.getLast();boolean forced=current.mode()!=OwnRequestTransport.Mode.MOVE;
        if(forced){token=new Object();ledger.dispatched(battle,actor,side,token,()->current);}
        var lease=ledger.beginChoice(battle,actor,token,uuid,forced,()->true);
        var protocol=new PublicProtocolEvidence();protocol.update(List.of(history));
        var provider=new PublicMechanicsEvidence(id->switch(id){
            case "mewtwo"->new PublicMechanicsEvidence.Species(Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null);
            case "xerneas"->new PublicMechanicsEvidence.Species(Map.of("hp",126,"atk",131,"def",95,"spa",131,"spd",98,"spe",99),List.of("fairy"),null);
            default->null;});
        provider.update(protocol);var fields=new PublicFieldEvidence();var base=fields.update(protocol,provider);
        var own=provider.observation(side+"a");var foe=provider.observation(side.equals("p1")?"p2a":"p1a");
        var field=ledger.rebindOwnZUse(lease,d,protocol,base);
        field=ledger.rebindOwnMegaY(lease,d,protocol,own,field);field=ledger.rebindOwnReentry(lease,d,protocol,own,field);
        var binding=ledger.bindOwnXerneas(lease,d,protocol,own,field);
        return new Fixture(ledger,lease,d,live,protocol,provider,fields,base,binding.field(),own,foe,binding.evidence(),uuid);
    }
    private static Fixture simple(String h,OwnRequestTransport.Request r,boolean fresh,String mode,boolean catalog){
        return fixture(h,List.of(request(false),request(false),request(true),r),4,"p1",OWN,fresh,mode,catalog,null);}
    private static Fixture simple(String h){return simple(h,request(false),true,"shift-registered",true);}
    private static boolean supported(Fixture f){return f.effective().missing().isEmpty()&&f.effective().evidence().profile().stats().size()==5;}
    static void run(){
        var good=simple(history());
        check(supported(good),"Source-bound Active return stayed UNKNOWN: "+good.effective().missing()+" "+good.field.unknown());
        check(good.field.hasOwnedForm()&&good.own.ambiguous()&&!good.own.confirmedSpecies().known(),"Owned Active proof changed spectator identity");
        check(good.effective().stages().get("spa")==0&&good.effective().evidence().profile().stat("spa").value().low()==250,"Return retained old stages/stats");
        check(good.execution().priority("geomancy","self").known(),"Owned Active did not reach existing Geomancy coverage");
        for(String mode:List.of("shift-unregistered","native-unregistered","native-registered","foreign"))
            check(!supported(simple(history(),request(false),true,mode,true)),"Unproved native form callback/registration gained Active witness: "+mode);
        for(String broken:List.of(
            history().replace("Xerneas-Active|","xerneasactive|"),history().replace("Xerneas-Active|","Xerneas-Neutral|"),
            history().replace("Xerneas-Active|","Xerneas-Active|[from] ability: Fairy Aura"),
            history().replace("|-formechange|p1a: "+OWN+"|Xerneas-Active|\n", ""),
            history().replace("|turn|4","|-formechange|p1a: "+OWN+"|Xerneas-Active|\n|turn|4"),
            history().replace("|-formechange|p1a: "+OWN+"|Xerneas-Active|", "|-damage|p1a: "+OWN+"|75/100\n|-formechange|p1a: "+OWN+"|Xerneas-Active|"),
            history().replace("|turn|3","|faint|p1a: "+OWN+"\n|turn|3"),
            history().replace("|turn|4","|-endability|p1a: "+OWN+"|Fairy Aura\n|turn|4"),
            history().replace("|turn|4","|-start|p1a: "+OWN+"|Gastro Acid\n|turn|4"),
            history().replace(BENCH.toString(),new UUID(0,99).toString()),
            history().replace("|Xerneas, "+OWN+"|75/100", "|Xerneas-Active, "+OWN+"|75/100")))
            check(!supported(simple(broken)),"Unproved form/identity/ordering/suppression gained own Active witness: "+broken);
        check(!supported(simple(history(),request(false),false,"shift-registered",true)),"Stale owner request gained Active evidence");
        check(!supported(simple(history(),request(false),true,"shift-registered",false)),"Missing species source gained Active evidence");
        var r=request(false);var p=r.pokemon().getFirst();
        for(var bad:List.of(
            new OwnRequestTransport.Pokemon(p.dto(),100,p.baseStats(),p.item(),List.of("Ghost"),"",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(p.dto(),100,p.baseStats(),p.item(),p.baseTypes(),"Fairy",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(p.dto(),100,p.baseStats(),p.item(),p.baseTypes(),"",true,"Xerneas"),
            new OwnRequestTransport.Pokemon(p.dto(),100,Map.of("spa",250),p.item(),p.baseTypes(),"",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(p.dto(),100,p.baseStats(),p.item(),p.baseTypes(),"",false,"Xerneas-Active")))
            check(!supported(simple(history(),new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(bad,r.pokemon().get(1)),r.selection()),true,"shift-registered",true)),"Unproved current metadata gained Active evidence");
        for(int mode=0;mode<4;mode++){
            var f=simple(history());var x=f.execution();
            if(mode==0)f.ledger.endChoice();else if(mode==1)f.live.set(false);else if(mode==2)f.fields.reset();else f.protocol.reset();
            boolean unknown=false;try{unknown=!x.priority("geomancy","self").known();}catch(IllegalArgumentException expired){unknown=true;}
            check(unknown,"Expired owner/source/field/protocol receipt retained Active operation");
        }
        var foreign=simple(history());check(foreign.ledger.bindOwnXerneas(good.lease,foreign.domain,foreign.protocol,foreign.own,foreign.base).evidence()==null,"Foreign lease gained Xerneas capability");
        String geo="|move|p1a: "+OWN+"|Geomancy||[still]\n|-prepare|p1a: "+OWN+"|Geomancy\n|-enditem|p1a: "+OWN+"|Power Herb\n"
            +"|-boost|p1a: "+OWN+"|spa|2\n|-boost|p1a: "+OWN+"|spd|2\n|-boost|p1a: "+OWN+"|spe|2\n";
        String completed=history().replace("|turn|4",geo+"|turn|4");
        check(supported(simple(completed)),"Completed native Herb charge did not restore owned evidence");
        for(String broken:List.of(geo.replace("|-enditem|p1a: "+OWN+"|Power Herb\n",""),
            geo.replace("[still]","[from] ability: Foreign"),geo.replace("|spe|2","|spe|1"),
            geo.replace("|-prepare|p1a: "+OWN+"|Geomancy\n",""),geo.replace("|Geomancy","|Nasty Plot")))
            check(!supported(simple(history().replace("|turn|4",broken+"|turn|4"))),"Incomplete/foreign charge gained owned evidence");
        for(String mode:List.of("native-unregistered","native-registered","shift-unregistered","shift-registered")){
            String h="update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: "+BENCH+"|Mewtwo, "+BENCH+"|75/100\n|switch|p2a: Foe|Mewtwo|100/100\n|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1";
            var f=fixture(h,List.of(request(true)),1,"p1",BENCH,true,mode,true,null);
            check(f.xerneas!=null&&f.xerneas.permitsEntry(BENCH.toString()).known(),"Other own species lost source entry capability");
            check(f.xerneas.permitsEntry(OWN.toString()).known()!=mode.equals("shift-unregistered"),"Future Xerneas entry ignored missing original registration");
            check(!f.xerneas.permitsEntry("opponent").known()&&!f.xerneas.permitsEntry(new UUID(0,99).toString()).known(),"Own capability inferred opponent/foreign reserve");
            var reserve=f.ledger.effective(f.lease,OWN.toString(),OWN,true,f.domain,f.protocol,f.own,f.field);
            check(reserve.missing().isEmpty(),"Declared own initial reserve was not established");
            var own=f.effective().evidence();var e=reserve.evidence();
            var bindings=Map.of("self",new ScopedExecution.Binding(f.own,own.item(),own.ability(),own.fresh(),f.lease),
                "opponent",new ScopedExecution.Binding(f.foe,f.foe.item(),f.foe.ability()),
                OWN.toString(),new ScopedExecution.Binding(null,e.item(),e.ability(),e.fresh(),f.lease));
            var execution=new ScopedExecution(f.field,bindings,f.domain.scopes()).withOwnXerneas(f.xerneas);
            var certificate=execution.entryCoverage(new Hazards(Map.of(),true,"declared no hazards"),Known.of(400,"fresh own max HP"),e.profile());
            check(certificate.supported()!=mode.equals("shift-unregistered"),"Scoped native entry bypassed original species registration: "+certificate.missing());
            if(certificate.supported()){
                f.ledger.endChoice();check(!certificate.supported(),"Expired entry source capability retained its certificate");
            }
        }
        System.out.println("Own Xerneas evidence regressions PASS: "+checks+" checks; exact form ordering, source registration, own identity, suppression, future entries and receipt withdrawal");
    }
    static void differential(Context engine)throws Exception{
        check(definitions!=null,"Original registration was not prepared");
        String source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/own_xerneas_oracle.cjs"));
        String mode=engine.eval("js","!!require('./data/abilities').Abilities.fairyaura.onPreStart").asBoolean()?"pack":"native";
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle,"+definitions.getAsJsonObject(mode)+","+mode.equals("pack")+"));})()").asString()).getAsJsonArray();
        check(rows.size()==36,"Native Xerneas lifecycle inventory changed");int bounded=0,guarded=0;
        for(var value:rows){var row=value.getAsJsonObject();var requests=new ArrayList<OwnRequestTransport.Request>();
            for(var q:row.getAsJsonArray("requests"))requests.add(OwnRequestJson.parse("|request|"+q));
            var f=fixture(row.get("public").getAsString(),requests,row.get("turn").getAsInt(),row.get("side").getAsString(),UUID.fromString(row.get("uuid").getAsString()),true,
                mode.equals("pack")?"shift-registered":"native-registered",true,null);
            if(row.get("forced").getAsBoolean()||row.getAsJsonObject("config").get("kind").getAsString().equals("charging")){
                check(!supported(f),"Native entry KO or unfinished charge gained current Active evidence");guarded++;continue;
            }
            check(supported(f),"Native Xerneas history failed: "+row.get("config")+" "+f.effective().missing()+" "+f.field.unknown());bounded++;
            var e=f.effective();for(String stat:List.of("atk","def","spa","spd","spe"))check(e.evidence().profile().stat(stat).value().low()==row.getAsJsonObject("stats").get(stat).getAsInt(),"Own Active stat differs: "+stat);
            check(e.evidence().profile().types().value().equals(List.of("fairy")),"Own Active typing differs");
            check(e.evidence().hp().value().low()==row.get("hp").getAsDouble()/row.get("maxHp").getAsDouble(),"Own Active HP/hazards differ");
            check(e.evidence().status().value().equals(row.get("status").getAsString()),"Own Active status was inferred");
            check(e.evidence().ability().value().equals("fairyaura")&&e.evidence().item().value().equals(row.get("item").getAsString()),"Own Active ability/herb differs");
            var stages=new TreeMap<String,Integer>();row.getAsJsonObject("stages").entrySet().forEach(a->stages.put(a.getKey(),a.getValue().getAsInt()));
            check(e.stages().equals(stages),"Own Active stage reset/Geomancy/Web differs: "+row.get("config")+" "+e.stages()+" vs "+stages);
            check(f.execution().priority("geomancy","self").known(),"Native own Active lost ordinary move operation");
            var observer=new PublicBattleObserver();f.protocol.mechanicsEvents().forEach(observer::accept);
            var ownFacts=new BattleSnapshot.Combatant("Xerneas",e.evidence().hp().value().low(),List.of("fairy"),e.stages(),List.of(),
                e.evidence().item().value(),"fairyaura",false);
            var ownActor=new Actor("self",ownFacts,e.evidence().hp(),e.stages(),e.evidence().status());
            var target=f.provider.bind("opponent",f.foe,new PublicMechanicsEvidence.Scope(f.domain.effectiveState(),f.field.trickRoom()));
            var foeActor=new Actor("opponent",PublicShadowSnapshot.opponent(observer,f.foe.slot()),f.foe.hp(),
                f.field.stages(f.foe,f.domain.effectiveState()),f.foe.status());
            var damage=f.execution().damage("moonblast",e.evidence().profile(),ownActor,target.profile(),foeActor,Critical.BOTH);
            check(damage.points().known()&&damage.points().value().low()<=row.getAsJsonArray("points").get(0).getAsInt()
                &&damage.points().value().high()>=row.getAsJsonArray("points").get(1).getAsInt(),"Current own Active/Fairy Aura/Geomancy excluded native critical rolls: "+row.get("config"));
            check(f.provider.observation(f.own.slot())==f.own,"Owned Active replaced public observation");
            if(mode.equals("pack"))check(f.field.hasOwnedForm()&&!f.own.confirmedSpecies().known(),"Owned Active promoted public identity");
        }
        System.out.println("Own Xerneas native differential PASS: "+rows.size()+" original-registration histories; "+bounded+" bounded / "+guarded+" UNKNOWN, both owner sides, fresh stats, Fairy Aura, Geomancy, hazards and re-entry");
    }
    static void attestation(Context engine)throws Exception{
        Path resources=Path.of(System.getProperty("maestroai.verificationRoot","."),"src/main/resources/com/maestroai/training");
        var test=engine.eval("js","""
            (probe,makeAnchor,definitions)=>{
              const modules={Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,
                BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,
                Side:require('./sim/side').Side,Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,
                DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,
                Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon};
              const anchor=makeAnchor(modules),registry=Cobblemon.registries.species,saved=[...registry.contents];
              const shift=!!require('./data/abilities').Abilities.fairyaura.onPreStart;
              let count=0,reads=0;
              const check=(ok,why)=>{count++;if(!ok)throw Error(why);};
              const battle=()=>new modules.Battle({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}});
              const capture=b=>probe(b,modules,false,null,anchor,true);
              try{
                let b=battle(),old=capture(b),base=JSON.parse(old.json);
                check(base.xerneasLifecycle===(shift?'shift-unregistered':'native-unregistered'),'missing Active source mode');
                receiveData(definitions,'species');afterSpeciesInit();
                check(!old.current(),'species registration retained old receipt');
                b=battle();let good=capture(b),raw=JSON.parse(good.json);
                check(good.current()&&raw.xerneasLifecycle===(shift?'shift-registered':'native-registered'),'registered source mode');
                check(raw.components.speciesHooks===base.components.speciesHooks,'original constructor definitions are not equivalent');
                const active=registry.contents.get('xerneasactive');
                for(const [object,key,value] of [[active.baseStats,'spa',132],[active,'types',['Ghost']],
                  [active,'forme','Neutral'],[active,'baseSpecies','Mewtwo'],[active,'abilities',{'0':'pressure'}],
                  [active,'name','xerneasactive']]){
                  const saved=object[key];object[key]=value;
                  check(!good.current(),'changed original definition retained lease');
                  check(JSON.parse(capture(b).json).components.speciesHooks!==raw.components.speciesHooks,'changed definition matched source fingerprint');
                  object[key]=saved;check(!good.current(),'restored definition revived old receipt');good=capture(b);
                }
                const original=Object.getOwnPropertyDescriptor(active,'baseStats');
                Object.defineProperty(active,'baseStats',{configurable:true,get(){reads++;throw Error('PRIVATE_VALUE');}});
                check(!good.current(),'accessor retained source receipt');let rejected=false;
                try{capture(b);}catch(expected){rejected=true;}check(rejected&&reads===0,'source accessor executed');
                Object.defineProperty(active,'baseStats',original);good=capture(b);
                const aura=b.dex.dataCache.Abilities.fairyaura,key=shift?'onPreStart':'onStart',callback=aura[key];
                aura[key]=eval('({'+callback.toString()+'}).'+key);rejected=false;
                try{capture(b);}catch(expected){rejected=true;}check(rejected&&!good.current(),'equal-text foreign Fairy Aura callback was attested');
                aura[key]=callback;check(capture(b).current(),'genuine callback restoration unavailable');
                // afterSpeciesInit replaced the dex after anchor construction.
                // Negative cache tests must obtain genuine lazy entries first.
                const pressure=b.dex.abilities.getByID('pressure');
                check(pressure&&typeof pressure.onStart==='function','fresh native ability lookup missing');
                b.dex.abilities.abilityCache.set('pressure',Object.assign(Object.create(Object.getPrototypeOf(pressure)),pressure));
                check(JSON.parse(capture(b).json).scopedRegistryEquivalent,'source-equivalent lazy cache reconstruction rejected');
                b.dex.abilities.abilityCache.set('pressure',{...pressure,onStart(){reads++;throw Error('PRIVATE_CALLBACK');}});
                rejected=false;try{capture(b);}catch(expected){rejected=true;}
                check(rejected&&reads===0,'foreign cache callback accepted or executed');
                b.dex.abilities.abilityCache.set('pressure',pressure);
                registry.contents.set('unverified',b.dex.species.getByID('mewtwo'));
                rejected=false;try{capture(b);}catch(expected){rejected=true;}
                check(rejected,'foreign species alias accepted after lazy lookup');registry.contents.delete('unverified');
                for(const name of ['p1','p2','sides','log','inputLog','activePokemon','activeTarget'])
                  Object.defineProperty(b,name,{get(){reads++;throw Error('PRIVATE_ACTOR');}});
                check(capture(b).current()&&reads===0,'Xerneas source probe touched private actors');
                return count;
              }finally{registry.contents.clear();for(const [k,v] of saved)registry.contents.set(k,v);registry.invalidate();afterSpeciesInit();}
            }
            """);
        boolean shift=engine.eval("js","!!require('./data/abilities').Abilities.fairyaura.onPreStart").asBoolean();
        int count=test.execute(engine.eval("js",Files.readString(resources.resolve("runtime-domain-probe.js"))),
            engine.eval("js",Files.readString(resources.resolve("runtime-source-anchor.js"))),definitions.get(shift?"pack":"native").toString()).asInt();
        check(count>=20,"Xerneas source negatives missing");
        System.out.println("Own Xerneas source attestation PASS: "+count+" checks; original definition equivalence, changed stats/types/forms/abilities, accessors, foreign callbacks, privacy and sticky receipt expiration");
    }
    public static void main(String[] args)throws Exception{
        prepare(Path.of(args[1]));run();
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false")
                .option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));
            differential(engine);attestation(engine);
        }
    }
    static RuntimeDomain runtime(GraalShowdownService service,Context engine,UUID id,AtomicBoolean choice,RuntimeDomain before)throws Exception{
        prepare(Path.of(GraalShowdownService.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        check(before.xerneasLifecycle().known()&&before.xerneasLifecycle().value()==RuntimeDomain.XerneasLifecycle.SHIFT_UNPROVED,"Missing native Active definition was not explicit");
        var defs=definitions.getAsJsonObject("pack");var data=new LinkedHashMap<String,String>();defs.entrySet().forEach(e->data.put(e.getKey(),e.getValue().toString()));
        SimulatorDomainBridge.mutationStarting(service);service.sendRegistryData(data,"species");service.indicateSpeciesInitialized();SimulatorDomainBridge.mutationFinished(service);
        check(!before.current(),"Original species dispatch retained old runtime receipt");
        engine.eval("js","verificationStream=new BS.BattleStream();battleMap.set('"+id+"',verificationStream);verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));"
            +"verificationStream.battle.setPlayer('p1',{team:[fixture('Mewtwo','pressure',['swift','recover'])]});verificationStream.battle.setPlayer('p2',{team:[fixture('Genesect','download',['ironhead','icebeam'])]});");
        var domain=SimulatorDomainBridge.capture(service,id,choice::get);
        check(domain!=null&&domain.effectiveState().known()&&domain.xerneasLifecycle().known()&&domain.xerneasLifecycle().value()==RuntimeDomain.XerneasLifecycle.SHIFT_REGISTERED,
            "Original registration did not reach production source receipt: "+(domain==null?SimulatorDomainBridge.unavailable(service):domain.missing()));
        for(String side:List.of("p1","p2")){
            String h=side.equals("p1")?history():history().replace("p1","P1").replace("p2","p1").replace("P1","p2");
            var requests=List.of(request(false),request(false),request(true),request(false)).stream()
                .map(r->new OwnRequestTransport.Request(side,r.mode(),r.pokemon().stream().map(p->{
                    var dto=p.dto();return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(dto.uuid(),
                        dto.ident().replace("p1:",side+":"),dto.condition(),dto.active(),dto.ability(),dto.types()),
                        p.level(),p.baseStats(),p.item(),p.baseTypes(),p.tera(),p.special(),p.species());
                }).toList(),r.selection())).toList();
            var f=fixture(h,requests,4,side,OWN,true,"",true,domain);
            check(supported(f)&&f.execution().priority("geomancy","self").known(),"Production registered profile did not reach own Active operations: "+side+" "+f.effective().missing()+" "+f.field.unknown());
        }
        engine.eval("js","Cobblemon.registries.species.contents.get('xerneasactive').baseStats.spa++;");
        var altered=SimulatorDomainBridge.capture(service,id,choice::get);
        check(!domain.current()&&(altered==null||!altered.effectiveState().known())
                &&(altered==null||!altered.xerneasLifecycle().known()),"Altered Active numbers retained runtime attestation");
        engine.eval("js","Cobblemon.registries.species.contents.get('xerneasactive').baseStats.spa--;");
        check(!domain.current(),"Restored Active numbers revived an old receipt");
        var restored=SimulatorDomainBridge.capture(service,id,choice::get);check(restored!=null&&restored.effectiveState().known(),"Restored source could not issue a fresh receipt");
        System.out.println("Own Xerneas native runtime integration PASS: original Java registration transport, actual source bridge, both owner sides, changed-definition rejection and sticky withdrawal; no installed-Mixin claim");
        return restored;
    }
}
