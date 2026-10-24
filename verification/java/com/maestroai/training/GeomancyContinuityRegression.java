package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;
import com.maestroai.training.PublicStateSearchAdapter.PublicMechanics;

/** Native past execution and independently fresh current own state. Synthetic
 * conditional domains are explicit; no opponent request enters the provider. */
final class GeomancyContinuityRegression {
    private static final UUID X=new UUID(0,1),M=new UUID(0,5);
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static OwnRequestTransport.Pokemon mon(UUID id,boolean active){boolean x=id.equals(X);
        return new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(id,"p1: Declared","400/400",active,
            x?"fairyaura":"pressure",x?List.of("Fairy"):List.of("Psychic")),100,
            Map.of("atk",200,"def",200,"spa",250,"spd",200,"spe",200),x?"none":"mewtwonitey",
            x?List.of("Fairy"):List.of("Psychic"),"",false,x?"Xerneas":"Mewtwo");}
    private static OwnRequestTransport.Request request(boolean x){return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,
        List.of(mon(X,x),mon(M,!x)),new OwnZMoveEvidence.Choice(List.of(new OwnZMoveEvidence.MoveSlot(x?"geomancy":"nastyplot",20,false,"self")),Map.of(),0));}
    private static String geo(){return "|move|p1a: "+X+"|Geomancy||[still]\n|-prepare|p1a: "+X+"|Geomancy\n|-enditem|p1a: "+X+"|Power Herb\n"
        +"|-boost|p1a: "+X+"|spa|2\n|-boost|p1a: "+X+"|spd|2\n|-boost|p1a: "+X+"|spe|2\n";}
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n"
        +"|switch|p1a: "+X+"|Xerneas, "+X+"|100/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
        +"|-formechange|p1a: "+X+"|Xerneas-Active|\n|-ability|p1a: "+X+"|Fairy Aura\n"
        +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n"+geo()
        +"|turn|2\n|switch|p1a: "+M+"|Mewtwo, "+M+"|100/100\n|turn|3";}
    private static OwnXerneasRegression.Fixture fixture(String h,List<OwnRequestTransport.Request> r,boolean fresh,String mode,boolean catalog,
            RuntimeDomain domain,boolean enabled){return OwnXerneasRegression.fixture(h,r,3,"p1",M,fresh,mode,catalog,domain,enabled);}
    private static OwnXerneasRegression.Fixture fixture(String h){return fixture(h,List.of(request(true),request(true),request(false)),true,"shift-registered",true,null,true);}
    private static boolean supported(OwnXerneasRegression.Fixture f){return f.effective().missing().isEmpty()&&f.effective().evidence().profile().stats().size()==5;}
    static void run(){
        var before=fixture(history(),List.of(request(true),request(true),request(false)),true,"shift-registered",true,null,false);
        check(!supported(before)&&before.field().unknown().contains("INDIRECT_EXECUTION_UNRESOLVED"),"Original completed-charge handoff rejection not reproduced");
        var good=fixture(history());check(supported(good)&&good.field().hasOwnedGeomancy(),"Completed own handoff stayed UNKNOWN: "+good.effective().missing()+" "+good.field().unknown());
        check(!good.field().hasOwnedForm()&&good.effective().stages().values().stream().allMatch(n->n==0),"Past charge granted identity coverage or moved boosts to the new actor");
        check(good.effective().evidence().ability().value().equals("pressure")&&good.effective().evidence().item().value().equals("mewtwonitey"),"Past Fairy Aura/Herb replaced fresh recipient modifiers");
        check(good.execution().priority("nastyplot","self").known(),"Own handoff did not reach existing operation scopes");
        for(String bad:List.of(
            history().replace("|-enditem|p1a: "+X+"|Power Herb\n",""),history().replace("|spe|2","|spe|1"),
            history().replace("|-prepare|p1a: "+X+"|Geomancy\n",""),history().replace("[still]","[from] ability: Foreign"),
            history().replace("|Xerneas, "+X+"|100/100","|Mewtwo, "+X+"|100/100"),
            history().replace("|-formechange|p1a: "+X+"|Xerneas-Active|\n",""),
            history().replace("Xerneas-Active|","xerneasactive|"),
            history().replace("|turn|1","|-start|p1a: "+X+"|Gastro Acid\n|turn|1"),
            history().replace("|turn|1","|-endability|p1a: "+X+"|Fairy Aura\n|turn|1"),
            history().replace("|turn|1","|-ability|p1a: "+X+"|Pressure\n|turn|1"),
            history().replace("|turn|2","|faint|p1a: "+X+"\n|turn|2"),
            history().replace("|turn|2","|-damage|p1a: "+X+"|0/100\n|turn|2"),
            history().replace("|turn|1","|-activate|p1a: "+X+"|ability: Foreign\n|turn|1"),
            history().replace("|turn|1","|-activate|p1a: "+X+"|move: Sticky Web\n|turn|1"),
            history().replace("|turn|2","|move|p1a: "+X+"|Baton Pass|p1a: "+X+"\n|turn|2")))
            check(!supported(fixture(bad)),"Unproved completion/identity/suppression/survival gained own continuity");
        for(String mode:List.of("shift-unregistered","unknown","native-registered","native-unregistered"))
            check(!supported(fixture(history(),List.of(request(true),request(true),request(false)),true,mode,true,null,true)),"Foreign/missing original form source gained continuity");
        check(!supported(fixture(history(),List.of(request(true),request(true),request(false)),false,"shift-registered",true,null,true)),"Stale owner packet gained past continuity");
        check(!supported(fixture(history(),List.of(request(true),request(true),request(false)),true,"shift-registered",false,null,true)),"Missing source catalog gained continuity");
        var current=request(false);var x=current.pokemon().getFirst();
        for(var bad:List.of(new OwnRequestTransport.Pokemon(x.dto(),100,x.baseStats(),x.item(),List.of("Ghost"),"",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(x.dto(),100,x.baseStats(),x.item(),x.baseTypes(),"Fairy",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(x.dto(),100,x.baseStats(),x.item(),x.baseTypes(),"",true,"Xerneas"),
            new OwnRequestTransport.Pokemon(x.dto(),100,Map.of("spa",250),x.item(),x.baseTypes(),"",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(X,"p1: Declared","0 fnt",false,"fairyaura",List.of("Fairy")),100,x.baseStats(),x.item(),x.baseTypes(),"",false,"Xerneas"),
            new OwnRequestTransport.Pokemon(x.dto(),100,x.baseStats(),x.item(),x.baseTypes(),"",false,"Xerneas-Active")))
            check(!supported(fixture(history(),List.of(request(true),request(true),new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(bad,current.pokemon().get(1)),current.selection())),true,"shift-registered",true,null,true)),"Missing/changed fresh own member evidence gained continuity");
        var missingMember=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(current.pokemon().get(1)),current.selection());
        check(!supported(fixture(history(),List.of(request(true),request(true),missingMember),true,"shift-registered",true,null,true)),"Foreign past UUID gained current roster membership");
        for(String bad:List.of(history().replace("|turn|3","|-start|p2a: Foe|Substitute\n|turn|3"),
            history().replace("|turn|3","|move|p2a: Foe|Geomancy||[still]\n|-prepare|p2a: Foe|Geomancy\n|turn|3"),
            history().replace("|turn|3","|switch|p2a: Foe|Mewtwo|100/100\n|turn|3"),
            history().replace("|turn|3","|-start|p1a: "+M+"|Gastro Acid\n|turn|3")))
            check(!supported(fixture(bad)),"Own past proof cleared unrelated/opposing state or identity uncertainty");
        var foreign=fixture(history());check(foreign.ledger().rebindOwnGeomancy(good.lease(),foreign.domain(),foreign.protocol(),foreign.base())==foreign.base(),"Foreign owner lease rebound past history");
        for(int mode=0;mode<4;mode++){
            var f=fixture(history());var execution=f.execution();
            var certificate=execution.setupCoverage(f.effective().evidence().profile(),actor(f),"nastyplot");check(certificate.supported(),"Completed charge could not certify current setup");
            if(mode==0)f.ledger().endChoice();else if(mode==1)f.live().set(false);else if(mode==2)f.fields().reset();else f.protocol().reset();
            boolean denied=false;try{denied=!execution.priority("nastyplot","self").known()&&!certificate.supported();}catch(IllegalArgumentException expired){denied=true;}
            check(denied,"Expired owner/source/field/protocol retained past-history execution");
        }
        var noDamage=fixture(history());var scopes=ExecutionCoverage.Scopes.receipt(op->op==ExecutionCoverage.Operation.DAMAGE?Known.unknown():Known.of(true,"declared scope"),()->true);
        check(!noDamage.execution().withScopes(scopes).priority("nastyplot","self").known(),"Past-history witness bypassed source catalog through another operation");
        pivotGuards();
        System.out.println("Completed Geomancy continuity evidence PASS: "+checks+" checks; original rejection, fresh recipient state, owned identity, composition, unsupported histories and receipt withdrawal");
    }
    private static Actor actor(OwnXerneasRegression.Fixture f){var e=f.effective();var own=e.evidence();
        return new Actor("self",new BattleSnapshot.Combatant("Declared",own.hp().value().low(),own.profile().types().value(),e.stages(),List.of(),own.item().value(),own.ability().value(),false),own.hp(),e.stages(),own.status());}
    private static void pivotGuards(){
        UUID donor=new UUID(0,2);String source="p1a: "+donor;
        var gene=new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(donor,"p1: Declared","300/300",true,"download",List.of("Bug","Steel")),100,
            Map.of("atk",200,"def",200,"spa",200,"spd",200,"spe",200),"choicescarf",List.of("Bug","Steel"),"",false,"Genesect");
        var outgoing=new OwnRequestTransport.Pokemon(new OwnRequestEvidence.Sample(donor,"p1: Declared","300/300",false,"download",List.of("Bug","Steel")),100,
            gene.baseStats(),gene.item(),gene.baseTypes(),"",false,"Genesect");
        var first=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(mon(X,false),gene,mon(M,false)),request(true).selection());
        var during=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(mon(X,true),outgoing,mon(M,false)),request(true).selection());
        var last=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(mon(X,false),outgoing,mon(M,true)),request(false).selection());
        String beginning="update\n|gen|9\n|gametype|singles\n|start\n|switch|"+source+"|Genesect, "+donor+"|100/100\n|switch|p2a: Foe|Mewtwo|100/100\n"
            +"|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n";
        String entry="|move|"+source+"|U-turn|p2a: Foe\n|-damage|p2a: Foe|75/100\n"
            +"|switch|p1a: "+X+"|Xerneas, "+X+"|100/100|[from] U-turn\n|-formechange|p1a: "+X+"|Xerneas-Active|\n|-ability|p1a: "+X+"|Fairy Aura\n";
        String ending="|turn|2\n"+geo()+"|turn|3\n|switch|p1a: "+M+"|Mewtwo, "+M+"|100/100\n|turn|4";
        var requests=List.of(first,during,during,last);
        var good=OwnXerneasRegression.fixture(beginning+entry+ending,requests,4,"p1",M,true,"shift-registered",true,null);
        check(supported(good),"Original owned direct-hit U-turn entry stayed UNKNOWN");
        for(String bad:List.of(entry.replace("[from] U-turn","[from] Baton Pass"),
            entry.replace("|U-turn|","|Tackle|"),entry.replace("|-damage|p2a: Foe|75/100\n",""),
            entry.replace("|-damage|p2a: Foe|75/100", "|-damage|p2a: Foe|75/100|[from] move: Stealth Rock"),
            entry.replace("|switch|p1a: "+X,"|faint|"+source+"\n|switch|p1a: "+X),
            entry.replace("|switch|p1a: "+X,"|-damage|"+source+"|0/100\n|switch|p1a: "+X),
            entry.replace("|move|"+source,"|move|p2a: Foe")))
            check(!supported(OwnXerneasRegression.fixture(beginning+bad+ending,requests,4,"p1",M,true,"shift-registered",true,null)),"Unproved pivot annotation, donor survival or direct hit gained own continuity");
    }
    static void differential(Context engine)throws Exception{
        boolean shift=engine.eval("js","!!require('./data/abilities').Abilities.fairyaura.onPreStart").asBoolean();
        String source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/geomancy_continuity_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle,"+OwnXerneasRegression.definitions(shift)+","+shift+"));})()").asString()).getAsJsonArray();
        check(rows.size()==140,"Native continuity inventory changed");int bounded=0,guarded=0;
        for(var value:rows){var row=value.getAsJsonObject();var requests=new ArrayList<OwnRequestTransport.Request>();
            for(var q:row.getAsJsonArray("requests"))requests.add(OwnRequestJson.parse("|request|"+q));
            int turn=row.get("turn").getAsInt();String side=row.get("side").getAsString(),h=row.get("public").getAsString();UUID uuid=UUID.fromString(row.get("uuid").getAsString());
            var f=OwnXerneasRegression.fixture(h,requests,turn,side,uuid,true,shift?"shift-registered":"native-registered",true,null);
            if(row.get("unsupported").getAsBoolean()){
                check(!supported(f),"Unproved native current entry/execution boundary gained current continuity");
                check(f.field().hasOwnedGeomancy()==row.get("unprovedEntry").getAsBoolean(),"Past proof granted a current-entry witness or survived a pending/lethal/incomplete boundary");
                guarded++;continue;
            }
            check(supported(f)&&f.field().hasOwnedGeomancy(),"Native own handoff failed: "+row.get("config")+" "+f.effective().missing()+" "+f.field().unknown());bounded++;
            var effective=f.effective();var e=effective.evidence();
            for(String stat:List.of("atk","def","spa","spd","spe"))check(e.profile().stat(stat).value().low()==row.getAsJsonObject("stats").get(stat).getAsInt(),"Recipient stat reused the former actor: "+stat);
            check(e.hp().value().low()==row.get("hp").getAsDouble()/row.get("maxHp").getAsDouble(),"Recipient HP/hazard survival differs from native");
            check(e.status().value().equals(row.get("status").getAsString()),"Recipient status inferred from departed Xerneas");
            check(e.item().value().equals(row.get("item").getAsString())&&e.ability().value().equals(row.get("ability").getAsString()),"Recipient modifiers reused Fairy Aura/Power Herb");
            var types=new ArrayList<String>();row.getAsJsonArray("types").forEach(t->types.add(t.getAsString().toLowerCase(Locale.ROOT)));
            check(e.profile().types().value().equals(types),"Recipient typing reused Xerneas");
            var stages=new TreeMap<String,Integer>();row.getAsJsonObject("stages").entrySet().forEach(a->stages.put(a.getKey(),a.getValue().getAsInt()));
            check(effective.stages().equals(stages),"Recipient inherited Geomancy boosts or lost entry/own boosts: "+row.get("config"));
            var ui=new AtomicReference<>(requests.getLast().selection());var z=f.ledger().ownZMove(f.lease(),f.domain(),f.own(),f.field(),ui::get);
            check(z!=null&&z.current(),"Current native UI did not bind after completed own history");var execution=f.execution().withOwnZMove(z);
            String move=row.get("move").getAsString();check(execution.priority(move,"self").known(),"Recipient ordinary action lost current source coverage");
            var observer=new PublicBattleObserver();f.protocol().mechanicsEvents().forEach(observer::accept);
            var target=f.provider().bind("opponent",f.foe(),new PublicMechanicsEvidence.Scope(f.domain().effectiveState(),f.field().trickRoom()));
            var foe=new Actor("opponent",PublicShadowSnapshot.opponent(observer,f.foe().slot()),f.foe().hp(),f.field().stages(f.foe(),f.domain().effectiveState()),f.foe().status());
            var damage=execution.damage(move,e.profile(),actor(f),target.profile(),foe,Critical.BOTH);var points=row.getAsJsonArray("points");
            if(points.get(1).getAsInt()==0)check(damage.fraction().known()&&damage.fraction().value().high()==0,"Native immunity gained handoff damage");
            else check(damage.points().known()&&damage.points().value().low()<=points.get(0).getAsInt()&&damage.points().value().high()>=points.get(1).getAsInt(),"Fresh recipient damage excludes native critical rolls: "+row.get("config"));
            if(row.get("spent").getAsBoolean())check(f.field().hasOwnedZUse()&&z.activation().isEmpty()&&!execution.priority(OwnZMoveEvidence.MOVE,"self").known(),"Completed Geomancy granted another Z use");
            if(row.get("species").getAsString().startsWith("Mewtwo")){
                var snapshot=new BattleSnapshot(f.lease().turn(),actor(f).facts(),foe.facts(),List.of(),List.of(),List.of("nastyplot"),List.of(),Map.of(),false,null);
                var context=new PublicTransitionState.Context(e.hp(),f.foe().hp(),e.status(),f.foe().status(),
                    f.field().hazards(side,f.domain().effectiveState()),f.field().hazards(side.equals("p1")?"p2":"p1",f.domain().effectiveState()),Map.of(),Map.of(),Set.of("APPEARANCE_ONLY"));
                var mechanics=new PublicMechanics(Map.of(),f.field().stages(f.foe(),f.domain().effectiveState()),Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
                var plan=new ShadowMechanicsEvaluation.Plan("move:nastyplot",List.of(List.of(new ShadowMechanicsEvaluation.Turn("move:nastyplot","bulkup",null))),"native own continuity / publicly witnessed reply");
                var comparison=ShadowMechanicsEvaluation.compare(snapshot,mechanics,Map.of("self",e.profile(),"opponent",target.profile()),List.of(plan),f.field().trickRoom(),Critical.BOTH,"move:nastyplot",execution);
                if(e.status().value().equals("psn")){
                    check(comparison.rankings().stream().allMatch(r->!r.utility().known()&&r.unknown().contains("STATUS_EXECUTION_OR_RESIDUAL_EFFECTS_UNRESOLVED")),"Past charge bypassed pre-residual poison boundary");
                    comparison=ShadowMechanicsEvaluation.compareEndOfTurn(snapshot,mechanics,Map.of("self",e.profile(),"opponent",target.profile()),List.of(plan),f.field().trickRoom(),Critical.BOTH,"move:nastyplot",execution);
                    check(comparison.rankings().stream().allMatch(r->!r.utility().known()&&r.unknown().contains("RESIDUAL_MAX_HP")
                        &&r.lines().stream().allMatch(l->l.state().self().stages().get("spa")==Math.min(6,effective.stages().get("spa")+2))),"Past charge supplied private opposing max HP or lost the certified setup");
                }else check(comparison.rankings().stream().anyMatch(r->r.action().equals("move:nastyplot")&&r.utility().known()),"Fresh handoff did not reach the full scoped setup comparator: "+row.get("config")+" "+comparison.rankings());
            }
            check(f.provider().observation(f.own().slot())==f.own(),"Past own history replaced spectator observations");
            if(row.getAsJsonObject("config").get("kind").getAsString().equals("switch")&&!row.getAsJsonObject("config").get("hazards").getAsBoolean()){
                var old=OwnXerneasRegression.fixture(h,requests,turn,side,uuid,true,shift?"shift-registered":"native-registered",true,null,false);
                check(!supported(old)&&old.field().unknown().contains("INDIRECT_EXECUTION_UNRESOLVED"),"Native original missing-handoff rejection was not reproduced");
            }
            if(row.getAsJsonObject("config").get("kind").getAsString().equals("z-mega-z")&&side.equals("p1")&&row.getAsJsonObject("config").get("level").getAsInt()==100){
                ui.set(new OwnZMoveEvidence.Choice(List.of(),Map.of(),0));
                check(!execution.priority(move,"self").known(),"Expired UI retained combined Geomancy/spent-Z/Mega-Y execution");
            }
        }
        System.out.println("Completed Geomancy native differential PASS: "+rows.size()+" histories; "+bounded+" bounded / "+guarded+" UNKNOWN; both owner sides, five roster recipients, fresh stats/modifiers/stages, critical rolls, pivots, Mega-Y and spent Z");
    }
    static void runtime(RuntimeDomain domain){
        var f=fixture(history(),List.of(request(true),request(true),request(false)),true,"",true,domain,true);
        check(supported(f)&&f.field().hasOwnedGeomancy()&&f.execution().priority("nastyplot","self").known(),"Actual registered runtime source did not admit completed own handoff");
        var unknown=new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,Map.of()),Map.of(),true,()->true);
        check(!supported(fixture(history(),List.of(request(true),request(true),request(false)),true,"",true,unknown,true)),"Missing runtime source granted continuity");
        var execution=f.execution();f.ledger().endChoice();check(!execution.sourceCurrent(),"Runtime handoff retained an expired owning choice");
        System.out.println("Completed Geomancy native runtime profile PASS: actual initialized source, synthetic own handoff, missing source and owner receipt withdrawal; no installed acceptance claim");
    }
    static Runnable withdrawal(RuntimeDomain domain){
        var f=fixture(history(),List.of(request(true),request(true),request(false)),true,"",true,domain,true);
        check(supported(f),"Actual registered definition did not support the pending withdrawal control");var execution=f.execution();
        return ()->check(!execution.priority("nastyplot","self").known()&&!f.field().ownedSourceCurrent(),"Changed original Xerneas definition retained a later actor's past-charge witness");
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
