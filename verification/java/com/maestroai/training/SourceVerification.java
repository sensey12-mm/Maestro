package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

/** Source-engine configuration contracts, with artificial state and no Minecraft claim. */
public final class SourceVerification {
    public static void main(String[] args) throws Exception {
        Path simulator = Path.of(args[0]).toAbsolutePath();
        Path resources = Path.of("src/main/resources/com/maestroai/training");
        var expected = new TreeMap<String,String>();
        for (String line : Files.readAllLines(resources.resolve("runtime-domain-components.tsv"))) {
            String[] columns = line.split("\t"); expected.put(columns[0], columns[1]);
        }
        try (var engine = Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true)
                .option("engine.WarnInterpreterOnly", "false")
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", simulator.toString())
                .option("js.commonjs-core-modules-replacements", "buffer:buffer/,crypto:crypto-browserify,path:path-browserify")
                .build()) {
            engine.eval("js", "var process={cwd:()=>''};");
            engine.eval("js", Files.readString(simulator.resolve("index.js")));
            engine.eval("js", "var verificationStream=new BS.BattleStream(); battleMap.set('synthetic-verification',verificationStream);"
                    + "verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,"
                    + "ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));");
            String moduleExpression = "{Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,"
                    + "BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,"
                    + "Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,"
                    + "BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,"
                    + "Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon}";
            var probe=engine.eval("js",Files.readString(resources.resolve("runtime-domain-probe.js")));
            var publicBattle=engine.eval("js","verificationStream.battle");
            var modules=engine.eval("js","("+moduleExpression+")");
            var clean = JsonParser.parseString(probe.execute(publicBattle,modules,false,null).asString()).getAsJsonObject();
            var observed = new TreeMap<String,String>();
            for (var value : clean.getAsJsonObject("components").entrySet()) {
                byte[] data = value.getValue().getAsString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                observed.put(value.getKey(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
            }
            if (!observed.equals(expected)) throw new AssertionError("Initialized source components differ: "+observed);
            var sourceDomain = new RuntimeDomain(new RuntimeDomain.Snapshot(
                    clean.get("mod").getAsString().equals("cobblemon") && clean.get("gen").getAsInt()==9
                            && clean.get("gameType").getAsString().equals("singles"),
                    clean.get("extraEvents").getAsBoolean(),clean.get("overrides").getAsBoolean(),
                    clean.get("registryEquivalent").getAsBoolean(),observed),expected,true,()->true);
            // Actual normalized standard overlays must remain usable. These
            // registrations preserve the pinned callbacks by object identity;
            // no function text is evaluated or treated as closure provenance.
            engine.eval("js", "for(const id of ['scratch','tackle','quickattack','shadowclaw','swordsdance'])"
                    + "Cobblemon.registries.move.register({name:id,...verificationStream.battle.dex.dataCache.Moves[id]},id);"
                    + "for(const id of ['runaway','pressure'])"
                    + "Cobblemon.registries.ability.register({name:id,...verificationStream.battle.dex.dataCache.Abilities[id]},id);");
            var nativeRefs=NativeRegistryRegression.prepare(engine,probe,publicBattle,modules,resources);
            var capture=(java.util.function.Supplier<com.google.gson.JsonObject>)()->JsonParser
                    .parseString(probe.execute(publicBattle,modules,false,nativeRefs).asString()).getAsJsonObject();
            var overlay=capture.get();
            if(!overlay.get("registryEquivalent").getAsBoolean()) throw new AssertionError("Exact source overlays refused");
            for(var value:overlay.getAsJsonObject("components").entrySet())
                if(!hash(value.getValue().getAsString()).equals(expected.get(value.getKey())))
                    throw new AssertionError("Standard registry changed source component");
            var overlayComponents=new TreeMap<>(observed);
            String digest=hash(overlay.get("registryReceipt").getAsString());overlayComponents.put("registryReceipt",digest);
            var registryLease=new RuntimeRegistryLease(digest,()->true,()->{
                var fresh=JsonParser.parseString(probe.execute(publicBattle,modules,true,nativeRefs).asString()).getAsJsonObject();
                return fresh.get("registryEquivalent").getAsBoolean()?hash(fresh.get("registryReceipt").getAsString()):null;
            });
            var overlayDomain=new RuntimeDomain(new RuntimeDomain.Snapshot(sourceDomain.effectiveState().known(),
                    overlay.get("extraEvents").getAsBoolean(),true,true,overlayComponents),expected,true,registryLease);
            PublicPipelineRegression.run(overlayDomain);
            NaturalAcceptanceRegression.run(overlayDomain);
            engine.eval("js", "['p1','p2','sides','log','inputLog','activePokemon','activeTarget'].forEach(key=>"
                    + "Object.defineProperty(verificationStream.battle,key,{get(){throw Error('private state touched');}}));");
            if (!overlay.equals(capture.get()))
                throw new AssertionError("Private-state guards changed public configuration");
            engine.eval("js", "Cobblemon.registries.ability.register({name:'Run Away',onModifyPriority(){return 8;}},'runaway');");
            var modified = capture.get();
            if (modified.get("registryEquivalent").getAsBoolean())
                throw new AssertionError("Unverified registry callback accepted");
            if (overlayDomain.effectiveState().known()) throw new AssertionError("Live source lease survived registry change");
            engine.eval("js", "Cobblemon.registries.ability.reset();Cobblemon.registries.move.reset();"
                    +"Cobblemon.registries.bagItem.reset();Cobblemon.registries.heldItem.reset();");
            if (!clean.equals(capture.get()))
                throw new AssertionError("Reset did not restore source configuration");
            if(overlayDomain.effectiveState().known()) throw new AssertionError("Restoration revived old registry receipt");
            // Equal function text with a foreign closure is still foreign code.
            engine.eval("js", "let genuine=verificationStream.battle.dex.dataCache.Abilities.pressure;"
                    + "Cobblemon.registries.ability.register({...genuine,onStart:eval('({'+genuine.onStart.toString()+'}).onStart')},'pressure');");
            if(capture.get().get("registryEquivalent").getAsBoolean())
                throw new AssertionError("Foreign closure accepted from equal source text");
            System.out.println("Relocated Graal source verification PASS: exact component hashes, private-state guards, changed callback rejection and reset");
        }
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
