package com.maestroai.training;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.*;
import com.cobblemon.mod.relocations.graalvm.polyglot.proxy.ProxyExecutable;
import com.maestroai.ai.SimulatorDomainBridge;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original native Java boot/transport + compiled bridge, with a real relocated
 * engine and public synthetic actors. Hook boundaries are explicitly hosted;
 * this is not Fabric/Minecraft application or an installed-pack acceptance.
 */
public final class RuntimeCaptureRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) throws Exception {
        var service=new GraalShowdownService();
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT).allowArrayAccess(true)
                        .allowIterableAccess(true).allowListAccess(true).allowMapAccess(true).build())
                .option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true")
                .option("js.commonjs-require-cwd",Path.of("showdown").toAbsolutePath().toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()) {
            SimulatorDomainBridge.invalidate(service);service.setContext(engine);
            engine.eval("js","globalThis.process={cwd:()=>''};");
            SimulatorDomainBridge.bootStarting(service);
            var boot=GraalShowdownService.class.getDeclaredMethod("boot");boot.setAccessible(true);
            SimulatorDomainBridge.invalidate(service); // Native boot sets its dispatcher through the observed setter.
            boot.invoke(service);SimulatorDomainBridge.bootFinished(service);
            check(SimulatorDomainBridge.unavailable(service).equals(Set.of("INITIALIZED_PUBLIC_PROBE_UNAVAILABLE")),"boot source/context rejected");
            // Observe the actual original sendRegistryData wire at the JS API
            // boundary; the native Kotlin joining/serialization is not mocked.
            int[] wires={0};
            engine.getBindings("js").putMember("verificationWire",(ProxyExecutable) values->{
                wires[0]++;
                SimulatorDomainBridge.registryDataPayload(service,new Object[]{values[0].asString(),values[1].asString()});return null;
            });
            engine.eval("js","globalThis.savedReceiveData=receiveData;receiveData=function(data,type){verificationWire(data,type);return savedReceiveData(data,type);};");
            for(String group:List.of("bagItem","heldItem")) {
                var scripts=new LinkedHashMap<String,String>();
                try(var list=RuntimeCaptureRegression.class.getResourceAsStream("/com/maestroai/training/runtime-native-factories.tsv")) {
                    for(String line:new String(list.readAllBytes(),StandardCharsets.UTF_8).split("\n")) {
                        String id=line.split("\t")[0];if(!id.startsWith(group+"/"))continue;
                        String name=id.substring(group.length()+1),folder=group.equals("bagItem")?"bag_items":"held_items";
                        try(var input=RuntimeCaptureRegression.class.getResourceAsStream("/data/cobblemon/"+folder+"/"+name+".js")) {
                            scripts.put(name,new String(input.readAllBytes(),StandardCharsets.UTF_8));
                        }
                    }
                }
                var pins=new TreeMap<String,String>();
                try(var input=RuntimeCaptureRegression.class.getResourceAsStream("/com/maestroai/training/runtime-native-factories.tsv")) {
                    for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).split("\n")) {
                        var fields=line.split("\t");pins.put(fields[0],fields[1]);
                    }
                }
                var expected=NativeRegistryEvidence.dispatch(group,scripts,pins);
                check(expected!=null,"native source bytes rejected");
                var serializer=GraalShowdownService.class.getDeclaredMethod("sendRegistryData$lambda$0",Map.Entry.class);
                serializer.setAccessible(true);
                var originalWire=new StringJoiner(", ","{","}");
                for(var entry:scripts.entrySet())originalWire.add(serializer.invoke(null,entry).toString());
                if(args.length>0 && args[0].equals("old-rejects")) {
                    check(!expected.matches(new Object[]{originalWire.toString(),group}),"old wire mismatch not reproduced");
                } else check(expected.matches(new Object[]{originalWire.toString(),group}),"original native serializer disagrees");
                SimulatorDomainBridge.registryDataStarting(service,scripts,group);
                service.sendRegistryData(scripts,group);
                SimulatorDomainBridge.registryDataFinished(service);
            }
            check(wires[0]==2,"native transport callbacks not executed");
            engine.eval("js","receiveData=savedReceiveData;delete globalThis.savedReceiveData;delete globalThis.verificationWire;");
            SimulatorDomainBridge.mutationStarting(service);service.indicateSpeciesInitialized();SimulatorDomainBridge.mutationFinished(service);
            UUID id=new UUID(0,1);
            engine.eval("js","globalThis.verificationStream=new BS.BattleStream();battleMap.set('"+id+"',verificationStream);"
                    +"verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',"
                    +"ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));"
                    +"const fixture=(species,ability,moves)=>({species,ability,moves,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});"
                    +"verificationStream.battle.setPlayer('p1',{team:[fixture('Mewtwo','pressure',['swift','recover'])]});"
                    +"verificationStream.battle.setPlayer('p2',{team:[fixture('Genesect','download',['ironhead','icebeam'])]});");
            var choice=new AtomicBoolean(true);
            var domain=SimulatorDomainBridge.capture(service,id,choice::get);
            if(args.length>0 && args[0].equals("old-rejects")) {
                check(domain==null,"old implementation did not reproduce actor-start failure");
                System.out.println("Original bridge actor-start rejection independently reproduced");return;
            }
            check(domain!=null&&domain.effectiveState().known(),"native boot/transport/actor-start capture rejected: "
                    +(domain==null?SimulatorDomainBridge.unavailable(service):domain.missing()));
            PublicPipelineRegression.run(domain);
            choice.set(false);check(!domain.current(),"own choice expiry retained domain");
            check(SimulatorDomainBridge.capture(service,id,choice::get)==null
                    &&SimulatorDomainBridge.unavailable(service).equals(Set.of("OWN_CHOICE_EXPIRED")),"own expiry diagnostic missing");
            choice.set(true);
            check(!domain.current(),"restored choice revived consumed receipt");
            int[] choiceChecks={0};
            check(SimulatorDomainBridge.capture(service,id,()->++choiceChecks[0]==1)==null
                    &&SimulatorDomainBridge.unavailable(service).equals(Set.of("OWN_CHOICE_EXPIRED")),"closing own-choice expiry accepted");
            check(SimulatorDomainBridge.capture(service,new UUID(0,2),choice::get)==null
                    &&SimulatorDomainBridge.unavailable(service).equals(Set.of("PUBLIC_BATTLE_LOOKUP_REJECTED")),"missing battle diagnostic wrong");
            engine.eval("js","verificationStream.battle.dex.conditions.conditionCache.set('syntheticprivate',Object.freeze({onStart(){throw Error('NEVER_LOG_ME');}}));");
            check(SimulatorDomainBridge.capture(service,id,choice::get)==null,"foreign condition accepted");
            check(SimulatorDomainBridge.unavailable(service).equals(Set.of("PUBLIC_CONFIGURATION_REJECTED")),"unsafe exception details escaped");
            engine.eval("js","verificationStream.battle.dex.conditions.conditionCache.delete('syntheticprivate');"
                    +"['p1','p2','sides','log','inputLog','activePokemon','activeTarget'].forEach(k=>Object.defineProperty(verificationStream.battle,k,{get(){throw Error('private actor state touched');}}));");
            var fresh=SimulatorDomainBridge.capture(service,id,choice::get);
            check(fresh!=null&&fresh.effectiveState().known(),"private-state trap or restored configuration rejected");
            SimulatorDomainBridge.mutationStarting(service);check(!fresh.current(),"registry mutation retained receipt");
            SimulatorDomainBridge.mutationFinished(service);check(!fresh.current(),"finished mutation revived old receipt");
            var last=SimulatorDomainBridge.capture(service,id,choice::get);check(last!=null,"fresh independent recapture rejected");
            SimulatorDomainBridge.invalidate(service);check(!last.current(),"context closure retained receipt");
            System.out.println("Native Java/Graal runtime capture PASS: original boot, registry serialization, species init, actor entry, real domain/provider pipeline, privacy, fixed diagnostics and lifecycle withdrawal");
        }
    }
}
