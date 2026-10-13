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
            String expression = Files.readString(resources.resolve("runtime-domain-probe.js"))
                    + "(verificationStream.battle," + moduleExpression + ")";
            var clean = JsonParser.parseString(engine.eval("js", expression).asString()).getAsJsonObject();
            var observed = new TreeMap<String,String>();
            for (var value : clean.getAsJsonObject("components").entrySet()) {
                byte[] data = value.getValue().getAsString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                observed.put(value.getKey(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
            }
            if (!observed.equals(expected)) throw new AssertionError("Initialized source components differ");
            engine.eval("js", "['p1','p2','sides','log','inputLog','activePokemon','activeTarget'].forEach(key=>"
                    + "Object.defineProperty(verificationStream.battle,key,{get(){throw Error('private state touched');}}));");
            if (!clean.equals(JsonParser.parseString(engine.eval("js", expression).asString())))
                throw new AssertionError("Private-state guards changed public configuration");
            engine.eval("js", "Cobblemon.registries.ability.register({name:'Run Away',onModifyPriority(){return 8;}},'runaway');");
            var modified = JsonParser.parseString(engine.eval("js", expression).asString()).getAsJsonObject();
            if (modified.get("registryEquivalent").getAsBoolean())
                throw new AssertionError("Unverified registry callback accepted");
            engine.eval("js", "Cobblemon.registries.ability.reset();");
            if (!clean.equals(JsonParser.parseString(engine.eval("js", expression).asString())))
                throw new AssertionError("Reset did not restore source configuration");
            System.out.println("Relocated Graal source verification PASS: exact component hashes, private-state guards, changed callback rejection and reset");
        }
    }
}
