package com.maestroai.training;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.JsonParser;
import com.maestroai.ai.SimulatorDomainBridge;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Original external factories and native LF dispatch; artificial actor evidence. */
final class RaidReloadRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static RuntimeDomain run(GraalShowdownService service,Context engine,UUID id,AtomicBoolean choice,RuntimeDomain before)throws Exception{
        var fixtures=JsonParser.parseString(Files.readString(Path.of("raid-condition-fixtures.json"))).getAsJsonObject();
        check(fixtures.keySet().equals(Set.of("cheerattack","cheerdefense","raidboss","shield")),"public raid inventory differs");
        var extra=JsonParser.parseString(Files.readString(Path.of("pack-native-fixtures.json"))).getAsJsonObject();
        check(extra.keySet().equals(Set.of("max_honey","max_mushroom","cheer_attack","cheer_defense","cheer_heal")),"public bag initializer inventory differs");
        var scripts=new LinkedHashMap<String,String>();
        var pins=new TreeMap<String,String>();
        for(String resource:List.of("runtime-native-factories.tsv","runtime-pack-native-factories.tsv"))
            try(var input=RaidReloadRegression.class.getResourceAsStream("/com/maestroai/training/"+resource)){
                for(String line:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().toList()){
                    var fields=line.split("\t");pins.put(fields[0],fields[1]);
                    if(resource.equals("runtime-native-factories.tsv")&&fields[0].startsWith("bagItem/")){
                        String name=fields[0].substring(8);
                        try(var source=RaidReloadRegression.class.getResourceAsStream("/data/cobblemon/bag_items/"+name+".js")){
                            scripts.put(name,new String(source.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        extra.entrySet().forEach(e->scripts.put(e.getKey(),e.getValue().getAsString()));
        check(NativeRegistryEvidence.dispatch("bagItem",scripts,pins)!=null,"known mixed pack dispatch rejected");
        var poisoned=new LinkedHashMap<>(scripts);poisoned.put("unknown", "{use(){throw Error('unverified');}}");
        check(NativeRegistryEvidence.dispatch("bagItem",poisoned,pins)==null,"known initializers blessed unknown companion code");
        int[] transports={0};
        engine.getBindings("js").putMember("verificationRaidWire",
                (com.cobblemon.mod.relocations.graalvm.polyglot.proxy.ProxyExecutable) values->{
                    transports[0]++;
                    SimulatorDomainBridge.registryDataPayload(service,new Object[]{values[0].asString(),values[1].asString()});
                    return null;
                });
        engine.eval("js","globalThis.verificationRaidOriginal=receiveData;receiveData=function(data,type){verificationRaidWire(data,type);return verificationRaidOriginal(data,type);};");
        SimulatorDomainBridge.registryDataStarting(service,scripts,"bagItem");
        try { service.sendRegistryData(scripts,"bagItem"); }
        finally {
            engine.eval("js","receiveData=verificationRaidOriginal;delete globalThis.verificationRaidOriginal;delete globalThis.verificationRaidWire;");
            SimulatorDomainBridge.registryDataFinished(service);
        }
        check(transports[0]==1,"original mixed native transport was not observed once");
        check(!before.current(),"mixed bag dispatch retained old lease");
        var mixed=SimulatorDomainBridge.capture(service,id,choice::get);
        check(mixed!=null&&mixed.effectiveState().known(),"source-verified mixed bag registry rejected: "
                +(mixed==null?SimulatorDomainBridge.unavailable(service):mixed.missing()));
        int count=0;
        for(var entry:fixtures.entrySet()){
            String wire;
            try(var reader=new BufferedReader(new StringReader(entry.getValue().getAsString()))){
                wire=reader.lines().collect(Collectors.joining("\n"));
            }
            engine.getBindings("js").getMember("receiveConditionData").execute(entry.getKey(),wire);
            check(!before.current(),"raid reload retained old domain lease");
            var partial=SimulatorDomainBridge.capture(service,id,choice::get);
            if(++count<4)check(partial==null||!partial.effectiveState().known(),"partial raid profile gained scope");
        }
        for(String condition:fixtures.keySet())engine.eval("js","verificationStream.battle.dex.conditions.getByID('"+condition+"')");
        OwnMegaYRegression.differential(engine);
            OwnZMoveRegression.differential(engine);
            OwnReentryRegression.differential(engine);
            OwnXerneasRegression.differential(engine);
            GeomancyContinuityRegression.differential(engine);
        var domain=SimulatorDomainBridge.capture(service,id,choice::get);
        check(domain!=null&&domain.effectiveState().known(),"source-verified raid definitions rejected: "
                +(domain==null?SimulatorDomainBridge.unavailable(service):domain.missing()));
        PublicPipelineRegression.run(domain);
        OwnMegaYRegression.runtime(domain);
            OwnZMoveRegression.runtime(domain);
            OwnReentryRegression.runtime(domain);
        var beforeMutation=domain;
        String original;
        try(var reader=new BufferedReader(new StringReader(fixtures.get("shield").getAsString()))){
            original=reader.lines().collect(Collectors.joining("\n"));
        }
        engine.getBindings("js").getMember("receiveConditionData").execute("shield",original+" ");
        check(!beforeMutation.current(),"changed raid wire retained old receipt");
        check(SimulatorDomainBridge.capture(service,id,choice::get)==null,"changed raid wire gained scope");
        engine.getBindings("js").getMember("receiveConditionData").execute("shield",original);
        check(!beforeMutation.current(),"restoration revived consumed raid lease");
        // Native species initialization creates a fresh dex; do not mutate
        // production caches to hide stale callbacks from the rejected reload.
        SimulatorDomainBridge.mutationStarting(service);service.indicateSpeciesInitialized();SimulatorDomainBridge.mutationFinished(service);
        engine.eval("js","verificationStream=new BS.BattleStream();battleMap.set('"+id+"',verificationStream);"
                +"verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));"
                +"verificationStream.battle.setPlayer('p1',{team:[fixture('Mewtwo','pressure',['swift','recover'])]});"
                +"verificationStream.battle.setPlayer('p2',{team:[fixture('Genesect','download',['ironhead','icebeam'])]});");
        domain=SimulatorDomainBridge.capture(service,id,choice::get);
        check(domain!=null&&domain.effectiveState().known(),"restored original raid source rejected");
        System.out.println("Pinned raid native integration PASS: four original LF conditions, five mixed native factories, partial-profile/unknown-source rejection, provider baselines, mutation and lease withdrawal");
        return domain;
    }
}
