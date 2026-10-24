package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Own dispatch + spectator pipeline; oracle exact opposing stats are used only
 * in a separately declared arithmetic scenario, never in the public provider. */
final class OwnZMoveRegression {
    private static final UUID OWN=new UUID(0,1);
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static <T> Known<T> known(T value){return Known.of(value,"declared synthetic Z fixture");}
    private static RuntimeDomain domain(AtomicBoolean live){
        var pins=Map.of("core","1".repeat(64),"tables","2".repeat(64),"speciesHooks","3".repeat(64),"catalog","4".repeat(64));
        return new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,false,pins),pins,true,live::get);
    }
    private static OwnZMoveEvidence.Choice choice(){return new OwnZMoveEvidence.Choice(
        List.of(new OwnZMoveEvidence.MoveSlot("spectralthief",32,false,"normal")),
        Map.of(0,new OwnZMoveEvidence.ZSlot(OwnZMoveEvidence.NAME,"normal",false)),1);}
    private static OwnRequestTransport.Request request(OwnZMoveEvidence.Choice choice,String species,String item){
        return new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(new OwnRequestTransport.Pokemon(
            new OwnRequestEvidence.Sample(OWN,"p1: Declared","321/321",true,"technician",List.of("Fighting","Ghost")),100,
            Map.of("atk",286,"def",196,"spa",216,"spd",216,"spe",286),item,List.of("Fighting","Ghost"),"",false,species)),choice);
    }
    private static String history(){return "update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: "+OWN+"|Marshadow, "+OWN+"|100/100\n"
        +"|switch|p2a: Foe|Mewtwo|100/100\n|-ability|p2a: Foe|Pressure\n|-enditem|p2a: Foe|Sitrus Berry\n|turn|1\n|move|p2a: Foe|Bulk Up|p2a: Foe";}
    private record Fixture(OwnRequestTransport ledger,OwnRequestTransport.Lease lease,OwnZMoveEvidence proof,
            AtomicBoolean live,AtomicReference<OwnZMoveEvidence.Choice> ui,PublicProtocolEvidence protocol,
            PublicMechanicsEvidence provider,PublicFieldEvidence.Capture field,OwnRequestTransport.Effective effective,
            ScopedExecution execution,Map<String,Profile> profiles,BattleSnapshot snapshot,PublicMechanics mechanics,State root){}
    private static Fixture fixture(String history,OwnRequestTransport.Request request,int turn,RuntimeDomain nativeDomain){
        String side=request.side();var live=new AtomicBoolean(true);var domain=nativeDomain==null?domain(live):nativeDomain;
        var ledger=new OwnRequestTransport();Object battle=new Object(),actor=new Object(),token=new Object();ledger.owner(battle,actor,side);
        ledger.dispatched(battle,actor,side,token,()->request);
        for(int t=1;t<turn;t++){
            ledger.turnDispatched(battle,t);ledger.turnFinished(battle,t);token=new Object();ledger.dispatched(battle,actor,side,token,()->request);
        }
        ledger.turnDispatched(battle,turn);var lease=ledger.beginChoice(battle,actor,token,OWN,false,()->true);
        var protocol=new PublicProtocolEvidence();protocol.update(List.of(history));
        var provider=new PublicMechanicsEvidence(id->switch(id){
            case "mewtwo"->new PublicMechanicsEvidence.Species(Map.of("hp",106,"atk",110,"def",90,"spa",154,"spd",90,"spe",130),List.of("psychic"),null);
            case "blissey"->new PublicMechanicsEvidence.Species(Map.of("hp",255,"atk",10,"def",10,"spa",75,"spd",135,"spe",55),List.of("normal"),null);
            default->null;});
        provider.update(protocol);var field=new PublicFieldEvidence().update(protocol,provider);
        field=ledger.rebindOwnZUse(lease,domain,protocol,field);
        var own=provider.observation(side+"a");var foe=provider.observation(side.equals("p1")?"p2a":"p1a");
        var ui=new AtomicReference<>(request.selection());var proof=ledger.ownZMove(lease,domain,own,field,ui::get);
        var effective=ledger.effective(lease,"self",OWN,false,domain,protocol,own,field);var e=effective.evidence();
        var target=provider.bind("opponent",foe,new PublicMechanicsEvidence.Scope(domain.effectiveState(),field.trickRoom()));
        var execution=new ScopedExecution(field,Map.of("self",new ScopedExecution.Binding(own,e.item(),e.ability(),e.fresh(),lease),
            "opponent",new ScopedExecution.Binding(foe,foe.item(),foe.ability())),domain.scopes()).withOwnZMove(proof);
        var observer=new PublicBattleObserver();protocol.mechanicsEvents().forEach(observer::accept);
        var self=new BattleSnapshot.Combatant("Marshadow",e.hp().known()?e.hp().value().low():1,List.of("fighting","ghost"),effective.stages(),List.of(),"marshadiumz","technician",false);
        var snapshot=new BattleSnapshot(turn,self,PublicShadowSnapshot.opponent(observer,foe.slot()),List.of(),List.of(),List.of("spectralthief",OwnZMoveEvidence.MOVE),List.of(),Map.of(),false,null);
        var context=new PublicTransitionState.Context(e.hp(),foe.hp(),e.status(),foe.status(),field.hazards(own.slot().substring(0,2),domain.effectiveState()),
            field.hazards(foe.slot().substring(0,2),domain.effectiveState()),Map.of(),Map.of(),Set.of("APPEARANCE_ONLY"));
        var mechanics=new PublicMechanics(Map.of(),field.stages(foe,domain.effectiveState()),Set.of(),false,false,false,1,new Evidence(context,Map.of(),Map.of()));
        var profiles=Map.of("self",e.profile(),"opponent",target.profile());
        return new Fixture(ledger,lease,proof,live,ui,protocol,provider,field,effective,execution,profiles,snapshot,mechanics,project(snapshot,mechanics).states().get("root"));
    }
    private static Damage damage(Fixture f){return f.execution.damage(OwnZMoveEvidence.MOVE,f.profiles.get("self"),f.root.self(),f.profiles.get("opponent"),f.root.opponent(),Critical.BOTH);}
    private static ShadowMechanicsEvaluation.Comparison compare(Fixture f,ScopedExecution execution){
        var plan=new ShadowMechanicsEvaluation.Plan("move:"+OwnZMoveEvidence.MOVE,List.of(List.of(new ShadowMechanicsEvaluation.Turn("move:"+OwnZMoveEvidence.MOVE,"bulkup",null))),"source-certified own choice");
        return ShadowMechanicsEvaluation.compare(f.snapshot,f.mechanics,f.profiles,List.of(plan),f.field.trickRoom(),Critical.BOTH,"move:spectralthief",execution);
    }
    static void run(){
        var good=fixture(history(),request(choice(),"Marshadow","marshadiumz"),1,null);
        check(good.proof.activation().orElseThrow().equals(new OwnZMoveEvidence.Activation("spectralthief",1,"zmove")),"Wrong native base-slot mapping");
        check(good.effective.missing().isEmpty(),"Own effective evidence failed: "+good.effective.missing());
        check(damage(good).fraction().known(),"Certified own Z remained UNKNOWN: "+damage(good).unknown());
        check(!CobblemonMechanics.damage(OwnZMoveEvidence.MOVE,good.profiles.get("self"),good.root.self(),good.profiles.get("opponent"),good.root.opponent(),Critical.BOTH).fraction().known(),"Raw calculator bypassed activation proof");
        var noProof=new ScopedExecution(good.field,Map.of(),known(true));
        check(!noProof.priority(OwnZMoveEvidence.MOVE,"self").known(),"Unbound signature action gained priority");
        check(compare(good,null).preferredConditional()==null,"Stateless comparison certified a Z action");
        var declared=request(choice(),"Marshadow","marshadiumz").pokemon().getFirst();
        var smallStats=new TreeMap<>(declared.baseStats());smallStats.put("atk",1);
        var small=new OwnRequestTransport.Request("p1",OwnRequestTransport.Mode.MOVE,List.of(new OwnRequestTransport.Pokemon(
            declared.dto(),100,smallStats,declared.item(),declared.baseTypes(),"",false,"Marshadow")),choice());
        var bounded=fixture(history()+"\n|-boost|p2a: Foe|def|2",small,1,null);
        var tactical=compare(bounded,bounded.execution);
        check(tactical.rankings().stream().filter(r->r.action().equals("move:"+OwnZMoveEvidence.MOVE)).allMatch(r->r.utility().known()),"Certified bounded Z tactical comparison remained UNKNOWN: "+tactical.rankings());
        check(tactical.rankings().stream().flatMap(r->r.lines().stream()).allMatch(l->l.state().self().stages().get("def")==0),"Z action stole Spectral Thief boosts");
        for(var bad:List.of(request(choice(),"Mew","marshadiumz"),request(choice(),"Marshadow","lifeorb"),request(null,"Marshadow","marshadiumz"),
                request(new OwnZMoveEvidence.Choice(List.of(new OwnZMoveEvidence.MoveSlot("closecombat",32,false,"normal")),choice().options(),1),"Marshadow","marshadiumz"),
                request(new OwnZMoveEvidence.Choice(List.of(new OwnZMoveEvidence.MoveSlot("spectralthief",0,false,"normal")),choice().options(),1),"Marshadow","marshadiumz"),
                request(new OwnZMoveEvidence.Choice(List.of(new OwnZMoveEvidence.MoveSlot("spectralthief",32,true,"normal")),choice().options(),1),"Marshadow","marshadiumz"),
                request(new OwnZMoveEvidence.Choice(choice().moves(),Map.of(0,new OwnZMoveEvidence.ZSlot("Unproved Z","normal",false)),1),"Marshadow","marshadiumz"))){
            var f=fixture(history(),bad,1,null);check(f.proof.activation().isEmpty()&&!damage(f).fraction().known(),"Unsupported eligibility gained Z damage");
            check(!f.execution.priority("spectralthief","self").known(),"Unproved advertised Z silently left exhaustive ordinary choices");
        }
        var absent=new OwnZMoveEvidence.Choice(choice().moves(),Map.of(),0);var empty=fixture(history(),request(absent,"Marshadow","marshadiumz"),1,null);
        check(empty.proof.current()&&empty.proof.activation().isEmpty()&&empty.execution.priority("spectralthief","self").known(),"No current advertised option damaged ordinary actions");
        var certificate=good.execution.priorityCoverage(OwnZMoveEvidence.MOVE,"self");good.ui.set(absent);
        check(!certificate.supported()&&!damage(good).fraction().known(),"In-place UI change retained Z receipt");
        good.ui.set(choice());check(!good.proof.current(),"UI restoration revived withdrawn receipt");
        good.ledger.endChoice();check(!good.proof.current()&&!damage(good).fraction().known(),"Expired own request retained Z outcome");
        var protect=fixture(history()+"\n|-singleturn|p2a: Foe|Protect",request(choice(),"Marshadow","marshadiumz"),1,null);
        var sparse=new TreeMap<>(protect.profiles.get("opponent").stats());sparse.remove("def");var target=protect.profiles.get("opponent");
        var missingDefense=new Profile("opponent",target.level(),target.maxHp(),sparse,target.types(),target.ordinaryEffects());
        check(!protect.execution.damage(OwnZMoveEvidence.MOVE,protect.profiles.get("self"),protect.root.self(),missingDefense,protect.root.opponent(),Critical.BOTH).fraction().known(),"Protect skipped required Z defense evidence");
        var source=fixture(history(),request(choice(),"Marshadow","marshadiumz"),1,null);source.live.set(false);
        check(!damage(source).fraction().known(),"Expired runtime source retained Z damage");
        var opponent=fixture(history(),request(choice(),"Marshadow","marshadiumz"),1,null);
        check(!opponent.execution.damage(OwnZMoveEvidence.MOVE,opponent.profiles.get("opponent"),opponent.root.opponent(),opponent.profiles.get("self"),opponent.root.self(),Critical.BOTH).fraction().known(),"Own receipt certified opponent Z eligibility");
        for(String badHistory:List.of(history().replace("|Marshadow, "+OWN,"|Marshadow-Mega, "+OWN),history()+"\n|-zpower|p1a: "+OWN,
                history()+"\n|move|p1a: "+OWN+"|Soul-Stealing 7-Star Strike|p2a: Foe|[zeffect]",history()+"\n|-start|p1a: "+OWN+"|Substitute")){
            var f=fixture(badHistory,request(choice(),"Marshadow","marshadiumz"),1,null);check(!damage(f).fraction().known(),"Unproved history gained Z execution");
        }
        System.out.println("Own Marshadow Z evidence regressions PASS: "+checks+" checks; activation, missing/foreign/source/owner/UI evidence and unchanged ordinary actions");
    }
    static void differential(Context engine)throws Exception{
        var source=Files.readString(Path.of(System.getProperty("maestroai.verificationRoot","."),"verification/own_z_move_oracle.cjs"));
        var rows=JsonParser.parseString(engine.eval("js","(()=>{const module={exports:{}};"+source+";return JSON.stringify(module.exports(require('./sim/battle').Battle));})()").asString()).getAsJsonArray();
        check(rows.size()==48,"Native Z inventory changed");int rolls=0;
        for(var value:rows){
            var row=value.getAsJsonObject();var request=OwnRequestJson.parse("|request|"+row.get("request"));
            var f=fixture(row.get("public").getAsString(),request,1,null);
            check(f.proof.activation().isPresent(),"Native choice failed eligibility: "+row.get("boundary")+" "+f.field.unknown());
            check(f.execution.accuracy(OwnZMoveEvidence.MOVE,f.profiles.get("self"),f.root.self(),f.profiles.get("opponent"),f.root.opponent()).value()==100,"Native always-hit Z gained an accuracy roll");
            var bounded=damage(f);check(bounded.fraction().known(),"Native public pipeline Z damage UNKNOWN: "+bounded.unknown());
            // Independent explicit arithmetic scenario. These exact synthetic
            // opponent stats never enter f.provider or the production binding.
            var data=row.getAsJsonObject("foe");var stats=new TreeMap<String,Known<IntRange>>();
            data.getAsJsonObject("stats").entrySet().forEach(e->{if(!e.getKey().equals("hp"))stats.put(e.getKey(),known(IntRange.exact(e.getValue().getAsInt())));});
            var types=new ArrayList<String>();data.getAsJsonArray("types").forEach(v->types.add(v.getAsString().toLowerCase(Locale.ROOT)));
            var exact=new Profile("opponent",known(row.get("level").getAsInt()),known(IntRange.exact(data.get("maxHp").getAsInt())),stats,known(types),known(true));
            for(boolean critical:new boolean[]{false,true}){
                var calculated=f.execution.damage(OwnZMoveEvidence.MOVE,f.profiles.get("self"),f.root.self(),exact,f.root.opponent(),critical?Critical.CRITICAL:Critical.NON_CRITICAL);
                int low=Integer.MAX_VALUE,high=0;
                for(var roll:row.getAsJsonArray("values")){var r=roll.getAsJsonObject();if(r.get("crit").getAsBoolean()==critical){int n=r.get("damage").getAsInt();low=Math.min(low,n);high=Math.max(high,n);rolls++;}}
                if(high==0)check(calculated.fraction().known()&&calculated.fraction().value().high()==0,"Z resurrected type immunity");
                else check(calculated.points().known()&&calculated.points().value().low()==low&&calculated.points().value().high()==high,"Z native rounding/order differs: "+row+" vs "+calculated);
                if(high>0)check(bounded.points().value().low()<=low&&bounded.points().value().high()>=high,"Public-only bounds excluded native Z outcome");
            }
            var after=OwnRequestJson.parse("|request|"+row.get("after"));
            check(after.selection()!=null&&after.selection().options().isEmpty(),"Native consumed choice not parsed");
            var stale=fixture(row.get("afterPublic").getAsString(),request,2,null);
            check(stale.proof.activation().isEmpty(),"Prior Z activation allowed a repeated advertised Z");
            var consumed=fixture(row.get("afterPublic").getAsString(),after,2,null);
            check(consumed.field.hasOwnedZUse()&&consumed.field.unknown().isEmpty(),"Certified own Z history did not rebind: "+consumed.field.unknown());
            check(consumed.proof.current()&&consumed.proof.activation().isEmpty(),"Consumed own Z history invented new eligibility");
            check(consumed.execution.priority("spectralthief","self").known(),"Certified prior Z history poisoned ordinary next actions");
        }
        System.out.println("Own Marshadow Z native differential PASS: "+rows.size()+" own choices, "+rolls+" exact critical/roll outcomes, public-only opposing bounds; both owner sides, Protect/immunity, burn and Magic Room");
    }
    static void runtime(RuntimeDomain domain){
        var f=fixture(history(),request(choice(),"Marshadow","marshadiumz"),1,domain);
        check(f.proof.activation().isPresent()&&damage(f).fraction().known(),"Actual initialized profile did not certify owned Z");
        f.ledger.endChoice();check(!damage(f).fraction().known(),"Actual runtime own receipt did not expire");
        System.out.println("Own Marshadow Z runtime profile PASS: initialized source scope, own choice and expiration; synthetic, no installed acceptance claim");
    }
    public static void main(String[] args)throws Exception{
        run();
        if(args.length>1)DispatchIntegrationRegression.run(Path.of(args[1]));
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false")
                .option("js.commonjs-require","true").option("js.commonjs-require-cwd",Path.of(args[0]).toAbsolutePath().toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()){
            engine.eval("js","var process={cwd:()=>''};");engine.eval("js",Files.readString(Path.of(args[0],"index.js")));differential(engine);
        }
    }

}
