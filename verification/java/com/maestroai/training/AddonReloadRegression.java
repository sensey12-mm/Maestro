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

/** Original public addon factories, native receiver and production certificate.
 * All actors are synthetic; this is not installed Minecraft acceptance. */
final class AddonReloadRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static RuntimeDomain run(GraalShowdownService service,Context engine,UUID id,AtomicBoolean choice,RuntimeDomain before)throws Exception{
        var fixtures=JsonParser.parseString(Files.readString(Path.of("za-definition-fixtures.json"))).getAsJsonObject();
        check(fixtures.size()==58,"unexpected public addon inventory");
        var api=Map.of("script","receiveScriptData","ability","receiveAbilityData","heldItem","receiveHeldItemData",
                "move","receiveMoveData","condition","receiveConditionData");
        for(var entry:fixtures.entrySet()){
            var key=entry.getKey().split("/");String wire;
            // The pinned loader's actual BufferedReader.lines recipe, preserving
            // all whitespace except line boundaries. No eval/rebuilt callbacks.
            try(var reader=new BufferedReader(new StringReader(entry.getValue().getAsString()))){
                wire=reader.lines().collect(Collectors.joining("\n")).replace("\n"," ");
            }
            engine.getBindings("js").getMember(api.get(key[0])).execute(key[1],wire);
        }
        check(!before.current(),"addon dispatch retained the old Java lease");
        SimulatorDomainBridge.mutationStarting(service);service.indicateSpeciesInitialized();SimulatorDomainBridge.mutationFinished(service);
        engine.eval("js","verificationStream=new BS.BattleStream();battleMap.set('"+id+"',verificationStream);"
                +"verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));"
                +"verificationStream.battle.setPlayer('p1',{team:[fixture('Mewtwo','pressure',['swift','recover'])]});"
                +"verificationStream.battle.setPlayer('p2',{team:[fixture('Genesect','download',['ironhead','icebeam'])]});");
        var dex=engine.eval("js","verificationStream.battle.dex");
        // Species initialization replaces the dex. These four original pack
        // items must also admit newly constructed native caches after reload.
        for(String item:List.of("eviolite","legendplate","pikaniumz","pikashuniumz")){
            dex.getMember("items").invokeMember("getByID",item);
            dex.getMember("conditions").invokeMember("getByID","item:"+item);
        }
        var caches=Map.of("ability","abilities","heldItem","items","move","moves","condition","conditions");
        for(var key:fixtures.keySet()){
            var parts=key.split("/");
            if(caches.containsKey(parts[0]))dex.getMember(caches.get(parts[0])).invokeMember("getByID",parts[1]);
            if(parts[0].equals("ability")||parts[0].equals("heldItem"))
                dex.getMember("conditions").invokeMember("getByID",(parts[0].equals("ability")?"ability:":"item:")+parts[1]);
        }
        OwnMegaYRegression.differential(engine);
            OwnZMoveRegression.differential(engine);
        var domain=SimulatorDomainBridge.capture(service,id,choice::get);
        check(domain!=null&&domain.effectiveState().known(),"source-bound addon capture rejected: "
                +(domain==null?SimulatorDomainBridge.unavailable(service):domain.missing()));
        PublicPipelineRegression.run(domain);
        OwnMegaYRegression.runtime(domain);
            OwnZMoveRegression.runtime(domain);
        for(int turn=0;turn<3;turn++){
            engine.eval("js","verificationStream.battle.makeChoices('move 1','move 2');");
            var next=SimulatorDomainBridge.capture(service,id,choice::get);
            check(next!=null&&next.effectiveState().known(),"addon advancing-turn capture rejected");
        }
        System.out.println("Pinned addon native integration PASS: 58 original factories, reinitialized dex, exact profile, advancing turns, production provider comparisons and old-lease withdrawal");
        return SimulatorDomainBridge.capture(service,id,choice::get);
    }
}
