package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Actual native requests/public returns; opposing private oracle state never enters the provider. */
final class OwnReentryRegression {
    private static final UUID OWN=new UUID(0,1),BENCH=new UUID(0,2);
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static RuntimeDomain domain(AtomicBoolean live,boolean catalog){
        var pins=Map.of("core","1".repeat(64),"tables","2".repeat(64),"speciesHooks","3".repeat(64),"catalog","4".repeat(64));
        var actual=new TreeMap<>(pins);if(!catalog)actual.put("catalog","0".repeat(64));
        return new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,actual),pins,true,live::get);
    }
    private static OwnRequestTransport.Pokemon mon(UUID id,boolean active,boolean old){
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Declared",
            id.equals(OWN)&&!old?"200/400":"400/400",active,id.equals(OWN)?"multitype":"pressure",
            id.equals(OWN)?List.of("Normal"):List.of("Psychic")),100,
            Map.of("atk",old?111:222,"def",200,"spa",200,"spd",200,"spe",200),
            id.equals(OWN)?"leftovers":"none",id.equals(OWN)?List.of("Normal"):List.of("Psychic"),"",false,
            id.equals(OWN)?"Arceus":"Mewtwo");
    }
    private static OwnRequestTransport.Request request(boolean old,boolean bench){
        return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
            List.of(mon(OWN,!bench,old),mon(BENCH,bench,old)),new OwnZMoveEvidence.Choice(
                List.of(new OwnZMoveEvidence.MoveSlot("swordsdance",20,false,"self")),Map.of(),0));
    }
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n"
        +"|switch|p1a: "+OWN+"|Arceus, "+OWN+"|100/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
        +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n|-boost|p1a: "+OWN+"|atk|4\n"
        +"|turn|2\n|switch|p1a: "+BENCH+"|Mewtwo, "+BENCH+"|100/100\n|turn|3\n"
        +"|switch|p1a: "+OWN+"|Arceus, "+OWN+"|50/100\n|turn|4";}
    private record Fixture(OwnRequestTransport ledger,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            AtomicBoolean live,AtomicReference<OwnZMoveEvidence.Choice> ui,PublicProtocolEvidence protocol,
            PublicMechanicsEvidence provider,PublicFieldEvidence fields,PublicFieldEvidence.Capture base,
            PublicFieldEvidence.Capture field,PublicMechanicsEvidence.Observation own,
            PublicMechanicsEvidence.Observation foe,OwnZMoveEvidence z,UUID uuid){
        OwnRequestTransport.Effective effective(){return ledger.effective(lease,"self",uuid,false,domain,protocol,own,field);}
        ScopedExecution execution(){
            var e=effective().evidence();var x=new ScopedExecution(field,Map.of(
                "self",new ScopedExecution.Binding(own,e.item(),e.ability(),e.fresh(),lease),
                "opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability())),domain.scopes());
            return z==null?x:x.withOwnZMove(z);
        }
    }
    private static Fixture fixture(String history,List<OwnRequestTransport.Request> requests,int turn,String side,
            UUID uuid,boolean fresh,boolean catalog,RuntimeDomain nativeDomain){
        var live=new AtomicBoolean(true);var domain=nativeDomain==null?domain(live,catalog):nativeDomain;
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),token=new Object();ledger.owner(battle,actor,side);
        ledger.dispatched(battle,actor,side,token,()->requests.getFirst());
        for(int t=1;t<turn;t++){
            ledger.turnDispatched(battle,t);ledger.turnFinished(battle,t);
            if(fresh){token=new Object();var r=requests.get(Math.min(t,requests.size()-1));ledger.dispatched(battle,actor,side,token,()->r);}
        }
        ledger.turnDispatched(battle,turn);var current=requests.getLast();boolean forced=current.mode()!=OwnRequestTransport.Mode.MOVE;
        if(forced){token=new Object();ledger.dispatched(battle,actor,side,token,()->current);}
        var lease=ledger.beginChoice(battle,actor,token,uuid,forced,()->true);
        var protocol=new PublicProtocolEvidence();protocol.update(List.of(history));
        var provider=new PublicMechanicsEvidence(id->switch(id){
            case "mewtwo"->new PublicMechanicsEvidence.Species(Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null);
            case "blissey"->new PublicMechanicsEvidence.Species(Map.of("hp",255,"atk",10,"def",10,"spa",75,"spd",135,"spe",55),List.of("normal"),null);
            default->null;});
        provider.update(protocol);var fields=new PublicFieldEvidence();var base=fields.update(protocol,provider);
        var own=provider.observation(side+"a");var foe=provider.observation(side.equals("p1")?"p2a":"p1a");
        var field=ledger.rebindOwnZUse(lease,domain,protocol,base);
        field=ledger.rebindOwnMegaY(lease,domain,protocol,own,field);
        field=ledger.rebindOwnReentry(lease,domain,protocol,own,field);
        var ui=new AtomicReference<>(current.selection());var z=ledger.ownZMove(lease,domain,own,field,ui::get);
        return new Fixture(ledger,lease,domain,live,ui,protocol,provider,fields,base,field,own,foe,z,uuid);
    }
    private static Fixture simple(String history,OwnRequestTransport.Request current,boolean fresh,boolean catalog){
        return fixture(history,List.of(request(true,false),request(true,false),request(true,true),current),4,"p1",OWN,fresh,catalog,null);
    }
    private static boolean supported(Fixture f){return f.effective().missing().isEmpty()&&f.effective().evidence().profile().stats().size()==5;}
    static void run(){
        var good=simple(history(),request(false,false),true,true);
        check(supported(good),"Certified own return stayed UNKNOWN: "+good.effective().missing());
        check(good.own.ambiguous()&&!good.own.confirmedSpecies().known()&&good.field.allActorsCovered(),"Owned witness rewrote public ambiguity or failed own-only coverage");
        check(good.effective().stages().get("atk")==0,"Return retained pre-exit boosts");
        check(good.effective().evidence().profile().stat("atk").value().low()==222,"Return reused old request stats");
        check(good.effective().evidence().hp().value().low()==.5,"Return reused old HP");
        check(good.execution().priority("swordsdance","self").known(),"Owned return did not reach scoped operations");
        check(good.field.stages(good.own,good.domain.effectiveState()).isEmpty(),"Own witness became a public identity/stage certificate");
        boolean stale=false;try{good.base.requireCurrent(4);}catch(IllegalArgumentException expected){stale=true;}check(stale,"Rebinding retained obsolete field evidence");
        var changed=simple(history().replace("|turn|4","|-boost|p1a: "+OWN+"|atk|1\n|turn|4"),request(false,false),true,true);
        check(supported(changed)&&changed.effective().stages().get("atk")==1,"Return erased a current observed entry boost");
        String web=history().replace("|turn|3","|-sidestart|p1: Declared|move: Sticky Web\n|turn|3")
            .replace("|turn|4","|-activate|p1a: "+OWN+"|move: Sticky Web\n|-unboost|p1a: "+OWN+"|spe|1\n|turn|4");
        var caught=simple(web,request(false,false),true,true);
        check(supported(caught)&&caught.own.unsupportedEffects()&&caught.effective().stages().get("spe")==-1,"Native paired entry Web event did not retain current own state and public caveat");
        for(String badWeb:List.of(
            web.replace("|-sidestart|p1: Declared|move: Sticky Web\n",""),
            web.replace("|spe|1","|atk|1"),web.replace("|spe|1","|spe|2"),
            web.replace("|-activate|p1a: "+OWN+"|move: Sticky Web\n", "|turn|4\n|-activate|p1a: "+OWN+"|move: Sticky Web\n"),
            web.replace("|turn|4","|-start|p1a: "+OWN+"|Substitute\n|turn|4")))
            check(!supported(simple(badWeb,request(false,false),true,true)),"Unproved or unrelated activation gained own entry coverage");
        for(String broken:List.of(
            history().replace("|switch|p1a: "+BENCH+"|Mewtwo, "+BENCH+"|100/100\n",""),
            history().replace(BENCH.toString(),new UUID(0,99).toString()),
            history().replace("|turn|2","|-damage|p1a: "+OWN+"|0/100\n|turn|2"),
            history().replace("|turn|2","|faint|p1a: "+OWN+"\n|turn|2"),
            history().replace("|turn|2","|detailschange|p1a: "+OWN+"|Arceus-Fire\n|turn|2"),
            history().replace("|turn|3","|move|p1a: "+BENCH+"|Baton Pass|p1a: "+BENCH+"\n|turn|3"),
            history().replace("|turn|4","|-start|p1a: "+OWN+"|Substitute\n|turn|4"),
            history().replace("|Arceus, "+OWN+"|50/100","|Arceus-Fire, "+OWN+"|50/100"),
            history().replace("|Arceus, "+OWN+"|50/100","|Arceus, "+OWN+"|50/100|[from] unproved"),
            history().replace("|turn|4","|switch|p2a: Foe|Mewtwo|100/100\n|turn|4"),
            history().replace("|turn|3","|turn|4")))
            check(!supported(simple(broken,request(false,false),true,true)),"Unproved exit/entry/foreign opponent gained own evidence");
        check(!supported(simple(history(),request(false,false),false,true)),"Stale owner request gained return evidence");
        check(!supported(simple(history(),request(false,false),true,false)),"Missing source/catalog gained return evidence");
        var current=request(false,false);var own=current.pokemon().getFirst();
        for(var bad:List.of(
            new OwnRequestTransport.Pokemon(own.dto(),100,own.baseStats(),own.item(),own.baseTypes(),"Normal",false,"Arceus"),
            new OwnRequestTransport.Pokemon(own.dto(),100,own.baseStats(),own.item(),List.of("Ghost"),"",false,"Arceus"),
            new OwnRequestTransport.Pokemon(own.dto(),100,own.baseStats(),own.item(),own.baseTypes(),"",true,"Arceus"),
            new OwnRequestTransport.Pokemon(own.dto(),100,Map.of("atk",222),own.item(),own.baseTypes(),"",false,"Arceus"),
            new OwnRequestTransport.Pokemon(own.dto(),100,own.baseStats(),own.item(),own.baseTypes(),"",false,"Arceus-Fire")))
            check(!supported(simple(history(),new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
                List.of(bad,current.pokemon().get(1)),current.selection()),true,true)),"Unsupported current own metadata gained return proof");
        for(var bad:List.of(
            new OwnRequestTransport.Pokemon(own.dto(),100,own.baseStats(),"heavydutyboots",own.baseTypes(),"",false,"Arceus"),
            new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(OWN,"p1: Declared","200/400",true,"levitate",List.of("Normal")),100,own.baseStats(),own.item(),own.baseTypes(),"",false,"Arceus")))
            check(!supported(simple(web,new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(bad,current.pokemon().get(1)),current.selection()),true,true)),"Impossible native Web grounding/item gained return evidence");
        var foreign=simple(history(),current,true,true);
        check(foreign.ledger.rebindOwnReentry(good.lease,foreign.domain,foreign.protocol,foreign.own,foreign.base)==foreign.base,"Foreign owner lease rebound a field");
        boolean opponent=false;try{good.ledger.effective(good.lease,"opponent",OWN,false,good.domain,good.protocol,good.own,good.field);}catch(IllegalArgumentException expected){opponent=true;}check(opponent,"Own return supplied opponent state");
        for(int mode=0;mode<5;mode++){
            var f=simple(history(),current,true,true);var execution=f.execution();
            if(mode==0)f.ledger.endChoice();else if(mode==1)f.live.set(false);else if(mode==2)f.fields.reset();else if(mode==3)f.protocol.reset();
            else f.ui.set(new OwnZMoveEvidence.Choice(List.of(),Map.of(),0));
            boolean unknown=false;try{unknown=!execution.priority("swordsdance","self").known();}catch(IllegalArgumentException expired){unknown=true;}
            check(unknown,"Expired owner/source/field/protocol/UI evidence retained an operation");
        }
        var missing=simple(history(),current,true,true);var x=missing.execution();
        var scopes=ExecutionCoverage.Scopes.receipt(op->op==ExecutionCoverage.Operation.DAMAGE?Known.unknown():Known.of(true,"declared"),()->true);
        check(!x.withScopes(scopes).priority("swordsdance","self").known(),"Own return bypassed source scope through another operation");
        System.out.println("Own roster re-entry evidence regressions PASS: "+checks+" checks; fresh own state, exit/entry identity, stage reset, untouched public ambiguity and withdrawal");
    }
    static void differential(Context engine)throws Exception{
        String source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/own_reentry_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==236,"Native own return inventory changed: "+rows.size());int bounded=0,guarded=0;
        for(var value:rows){
            var row=value.getAsJsonObject();var requests=new ArrayList<OwnRequestTransport.Request>();
            for(var r:row.getAsJsonArray("requests"))requests.add(OwnRequestJson.parse("|request|"+r));
            var f=fixture(row.get("public").getAsString(),requests,row.get("turn").getAsInt(),row.get("side").getAsString(),
                UUID.fromString(row.get("uuid").getAsString()),true,true,null);
            if(row.get("forced").getAsBoolean()||row.get("unprovedForm").getAsBoolean()){
                check(!supported(f)&&!f.field.hasOwnedForm(),"Native entry KO/forced request or unproved addon form gained a current own return");guarded++;continue;
            }
            check(supported(f),"Native own return failed: "+row.get("config")+" "+f.effective().missing()+" "+f.field.unknown());bounded++;
            var e=f.effective();
            for(String stat:List.of("atk","def","spa","spd","spe"))check(e.evidence().profile().stat(stat).value().low()==row.getAsJsonObject("stats").get(stat).getAsInt(),"Returned own stat differs from native: "+stat);
            check(e.evidence().hp().value().low()==row.get("hp").getAsDouble()/row.get("maxHp").getAsDouble(),"Returned HP/hazard survival differs");
            check(e.evidence().status().value().equals(row.get("status").getAsString()),"Returned status/recovery was inferred");
            var nativeTypes=new ArrayList<String>();for(var t:row.getAsJsonArray("types"))nativeTypes.add(t.getAsString().toLowerCase(Locale.ROOT));
            check(e.evidence().profile().types().value().equals(nativeTypes),"Returned typing differs from native current types");
            check(e.evidence().profile().maxHp().value().low()==row.get("maxHp").getAsInt(),"Returned maximum HP differs from native");
            check(e.evidence().ability().value().equals(row.get("ability").getAsString())&&e.evidence().item().value().equals(row.get("item").getAsString()),"Returned current ability/item differs from native");
            var stages=new TreeMap<String,Integer>();row.getAsJsonObject("stages").entrySet().forEach(a->stages.put(a.getKey(),a.getValue().getAsInt()));
            check(e.stages().equals(stages),"Return retained old boosts or lost native hazards/Download: "+row.get("config")+" "+e.stages()+" vs "+stages);
            check(f.own.ambiguous()&&!f.own.confirmedSpecies().known()&&f.provider.observation(f.own.slot())==f.own,"Owned return resolved spectator identity");
            var current=requests.getLast().pokemon().stream().filter(p->p.dto().active()).findFirst().orElseThrow();
            String move=requests.getLast().selection().moves().getFirst().id();
            check(f.execution().priority(move,"self").known(),"Native own return did not reach ordinary priority coverage");
            check(f.z!=null&&f.z.current(),"Native current own UI/eligibility could not bind");
            if(row.get("spent").getAsBoolean()){
                check(f.field.hasOwnedZUse()&&f.z.activation().isEmpty(),"Spent Z history lost continuity or granted reuse");
                check(!f.execution().priority(OwnZMoveEvidence.MOVE,"self").known(),"Spent own return granted a second Z action");
                var c=row.getAsJsonObject("config");
                if(!c.has("megaFirst")&&c.get("owner").getAsInt()==0&&c.get("boundary").getAsString().equals("hit")&&c.get("mixed").getAsBoolean()&&c.get("active").getAsString().equals("marshadow")){
                    var advertised=new ArrayList<>(requests);var last=advertised.getLast();
                    advertised.set(advertised.size()-1,new OwnRequestTransport.Request(last.side(),last.mode(),last.pokemon(),requests.getFirst().selection()));
                    var repeated=fixture(row.get("public").getAsString(),advertised,f.lease.turn(),last.side(),f.uuid,true,true,null);
                    check(repeated.z.activation().isEmpty()&&!repeated.execution().priority(move,"self").known(),"Spent history accepted repeated advertised Z or silently omitted it");
                    for(String bad:List.of(row.get("public").getAsString().replace("|-zpower|", "|-activate|"),
                            row.get("public").getAsString().replace("|-zpower|p1a: "+OWN,"|-zpower|p1a: "+new UUID(0,99)))){
                        var unproved=fixture(bad,requests,f.lease.turn(),last.side(),f.uuid,true,true,null);
                        check(!supported(unproved),"Unproved spent-Z activation history gained own continuity");
                    }
                    var old=fixture(row.get("public").getAsString(),requests,f.lease.turn(),last.side(),f.uuid,false,true,null);
                    check(!supported(old),"Stale owning request gained combined gimmick history");
                    var expired=f.execution();f.ledger.endChoice();
                    check(!expired.sourceCurrent(),"Combined gimmick history retained an expired owner receipt");
                }
            }else if(current.species().equals("Marshadow"))check(f.z.activation().isPresent(),"Unspent own return lost native Z eligibility");
        }
        System.out.println("Own roster re-entry native differential PASS: "+rows.size()+" histories; "+bounded+" certified / "+guarded+" guarded; both owner sides, fresh stats/HP/types/status, reset stages, hazards and combined spent-Z/Mega-Y histories");
    }
    static void runtime(RuntimeDomain domain){
        var f=fixture(history(),List.of(request(true,false),request(true,false),request(true,true),request(false,false)),4,"p1",OWN,true,true,domain);
        check(supported(f)&&f.execution().priority("swordsdance","self").known(),"Initialized profile did not reach owned return operations");
        f.ledger.endChoice();check(!supported(f),"Initialized profile retained an expired own return");
        System.out.println("Own roster re-entry runtime profile PASS: actual initialized source scope, synthetic owned returns and receipt expiration");
    }
    public static void main(String[] args)throws Exception{
        run();try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false")
                .option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }
}
