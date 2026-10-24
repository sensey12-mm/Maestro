package com.maestroai.training;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.*;
import com.cobblemon.mod.relocations.graalvm.polyglot.proxy.ProxyExecutable;
import com.maestroai.ai.SimulatorDomainBridge;
import com.google.gson.*;
import java.io.*;
import java.util.stream.Collectors;
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
        OwnXerneasRegression.prepare(Path.of(GraalShowdownService.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        var service=new GraalShowdownService();
        // Execute the exact upstream constructor and restrictive module file
        // system; a generic Context builder is not the native runtime path.
        var create=GraalShowdownService.class.getDeclaredMethod("createContext");create.setAccessible(true);create.invoke(service);
        try(var engine=service.getContext()) {
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
            if(args.length>0&&args[0].equals("xerneas-only")){
                OwnXerneasRegression.runtime(service,engine,id,choice,domain);return;
            }
            var beforeReload=domain;
            OwnMegaYRegression.runtime(domain);
            OwnZMoveRegression.runtime(domain);
            OwnReentryRegression.runtime(domain);
            // Independently use the pinned Java loader's BufferedReader recipe
            // and original JS API, not production hashes to build expected input.
            var scripts=JsonParser.parseString(Files.readString(Path.of("pack-item-fixtures.json"))).getAsJsonObject();
            check(scripts.keySet().equals(Set.of("eviolite","legendplate","pikaniumz","pikashuniumz")),"public fixture scope differs");
            int reloaded=0;
            for(var entry:scripts.entrySet()) {
                String wire;
                try(var reader=new BufferedReader(new StringReader(entry.getValue().getAsString()))) {
                    wire=reader.lines().collect(Collectors.joining("\n")).replace("\n"," ");
                }
                engine.getBindings("js").getMember("receiveHeldItemData").execute(entry.getKey(),wire);
                if(++reloaded<scripts.size() && !(args.length>0 && args[0].equals("reload-old-rejects"))) {
                    var partial=SimulatorDomainBridge.capture(service,id,choice::get);
                    check(partial!=null&&!partial.effectiveState().known(),"partial reload gained a complete operation profile");
                }
            }
            domain=SimulatorDomainBridge.capture(service,id,choice::get);
            if(args.length>0 && args[0].equals("reload-old-rejects")) {
                check(domain==null,"old implementation did not reject actual post-boot item callbacks");
                check(SimulatorDomainBridge.unavailable(service).equals(Set.of("PUBLIC_CONFIGURATION_REJECTED")),"old captured failure differs");
                System.out.println("Original 1.0.3 post-boot pack item rejection independently reproduced");return;
            }
            check(!beforeReload.current(),"item reload retained old Java receipt");
            check(domain!=null&&domain.effectiveState().known(),"post-boot native item reload rejected: "
                    +(domain==null?SimulatorDomainBridge.unavailable(service):domain.missing()));
            PublicPipelineRegression.run(domain);
            OwnMegaYRegression.runtime(domain);
            OwnZMoveRegression.runtime(domain);
            OwnReentryRegression.runtime(domain);
            domain=AddonReloadRegression.run(service,engine,id,choice,domain);
            domain=RaidReloadRegression.run(service,engine,id,choice,domain);
            domain=OwnXerneasRegression.runtime(service,engine,id,choice,domain);
            engine.eval("js","globalThis.verificationDamage=BS.BattleStream?verificationStream.battle.actions.getDamage:null;"
                    +"const ap=Object.getPrototypeOf(verificationStream.battle.actions);ap.getDamage=eval('({'+ap.getDamage.toString()+'}).getDamage');");
            String priorProperty=System.getProperty("maestroai.envelopeCapture");
            var diagnosticBytes=new ByteArrayOutputStream();var originalOutput=System.out;
            try {
                System.setProperty("maestroai.envelopeCapture","true");System.setOut(new PrintStream(diagnosticBytes));
                SimulatorDomainBridge.mutationStarting(service);SimulatorDomainBridge.mutationFinished(service);
            } finally {
                System.setOut(originalOutput);
                if(priorProperty==null)System.clearProperty("maestroai.envelopeCapture");else System.setProperty("maestroai.envelopeCapture",priorProperty);
            }
            String diagnostic=diagnosticBytes.toString(StandardCharsets.UTF_8);
            check(diagnostic.contains("PUBLIC_METHOD_AUDIT")&&diagnostic.contains("ACTIONS/getDamage/IDENTITY")
                    &&diagnostic.contains("actualBattle=NOT_INSPECTED")&&diagnostic.contains("grantsScope=false"),"startup method diagnosis missing");
            check(SimulatorDomainBridge.capture(service,id,choice::get)==null,"foreign method gained a domain");
            var rejected=SimulatorDomainBridge.unavailable(service);
            check(rejected.containsAll(Set.of("PUBLIC_CONFIGURATION_METHODS_REJECTED","METHOD_GROUP:ACTIONS",
                    "METHOD_CHECK:ACTIONS/getDamage/IDENTITY","METHOD_FAILURE_KIND:GUEST_EXCEPTION")),"method rejection not independently localized: "+rejected);
            engine.eval("js","Object.getPrototypeOf(verificationStream.battle.actions).getDamage=verificationDamage;delete globalThis.verificationDamage;");
            check(!domain.current(),"method restoration revived consumed receipt");
            domain=SimulatorDomainBridge.capture(service,id,choice::get);check(domain!=null&&domain.effectiveState().known(),"restored method blocked a fresh receipt");
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
            check(SimulatorDomainBridge.unavailable(service).equals(Set.of("PUBLIC_CONFIGURATION_CACHE_REJECTED")),"unsafe exception details escaped");
            engine.eval("js","verificationStream.battle.dex.conditions.conditionCache.delete('syntheticprivate');"
                    +"['p1','p2','sides','log','inputLog','activePokemon','activeTarget'].forEach(k=>Object.defineProperty(verificationStream.battle,k,{get(){throw Error('private actor state touched');}}));");
            var fresh=SimulatorDomainBridge.capture(service,id,choice::get);
            check(fresh!=null&&fresh.effectiveState().known(),"private-state trap or restored configuration rejected");
            engine.eval("js","globalThis.verificationReceiver=receiveHeldItemData;receiveHeldItemData=function(){throw Error('NEVER_LOG_ME');};");
            check(!fresh.current(),"replaced public receiver retained current receipt");
            check(SimulatorDomainBridge.capture(service,id,choice::get)==null
                    && SimulatorDomainBridge.unavailable(service).equals(Set.of("PUBLIC_CONFIGURATION_REJECTED")),"foreign receiver bypassed provenance or leaked details");
            engine.eval("js","receiveHeldItemData=verificationReceiver;delete globalThis.verificationReceiver;");
            check(!fresh.current(),"receiver restoration revived old receipt");
            fresh=SimulatorDomainBridge.capture(service,id,choice::get);check(fresh!=null,"restored receiver blocked fresh proof");
            SimulatorDomainBridge.mutationStarting(service);check(!fresh.current(),"registry mutation retained receipt");
            SimulatorDomainBridge.mutationFinished(service);check(!fresh.current(),"finished mutation revived old receipt");
            var last=SimulatorDomainBridge.capture(service,id,choice::get);check(last!=null,"fresh independent recapture rejected");
            SimulatorDomainBridge.invalidate(service);check(!last.current(),"context closure retained receipt");
            System.out.println("Native Java/Graal runtime capture PASS: original boot, registry serialization, species init, actor entry, exact post-boot item reload, real domain/provider pipeline, privacy, fixed diagnostics and lifecycle withdrawal");
        }
    }
}
