package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Source-pinned engine holdouts. Artificial public actors; no Minecraft claim. */
final class PackSourceRegression {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Map<String,String> pins(Path file) throws Exception {
        var pins=new TreeMap<String,String>();
        for(String line:Files.readAllLines(file)) {var p=line.split("\t");pins.put(p[0],p[1]);}
        return pins;
    }
    private static boolean sameOperations(JsonObject a,JsonObject b) {
        return List.of("core","format","operationTables","speciesHooks","catalog").stream().allMatch(k->
                a.getAsJsonObject("components").get(k).equals(b.getAsJsonObject("components").get(k)));
    }
    static void run(Path simulator, Path dependency, boolean layoutOnly) throws Exception {
        Path resources=Path.of("src/main/resources/com/maestroai/training");
        var files=new TreeMap<String,Set<String>>();
        pins(resources.resolve("runtime-domain-files.tsv")).forEach((k,v)->files.put(k,Set.of(v)));
        for(String line:Files.readAllLines(resources.resolve("runtime-pack-files.tsv"))) {
            var p=line.split("\t");files.put(p[0],Set.of(p[1],p[2]));
        }
        var optional=new TreeMap<String,Set<String>>();
        for(String line:Files.readAllLines(resources.resolve("runtime-pack-optional.tsv"))) {
            var p=line.split("\t");optional.put(p[0],Set.of(p[1],p[2]));
        }
        try(var jar=new ZipFile(dependency.toFile())) {
            var report=RuntimeSourceAudit.inspectAlternatives(simulator,files,optional,
                    pins(resources.resolve("runtime-domain-classes.tsv")),name->{
                        try(var in=jar.getInputStream(jar.getEntry(name))){return in.readAllBytes();}
                        catch(Exception unavailable){return null;}
                    });
            check(report.verified(),"pack source inventory rejected: "+report);
        }
        try(var engine=Context.newBuilder("js").allowIO(true).allowExperimentalOptions(true)
                .option("engine.WarnInterpreterOnly","false").option("js.commonjs-require","true")
                .option("js.commonjs-require-cwd",simulator.toString())
                .option("js.commonjs-core-modules-replacements","buffer:buffer/,crypto:crypto-browserify,path:path-browserify").build()) {
            engine.eval("js","var process={cwd:()=>''};");
            engine.eval("js",Files.readString(simulator.resolve("index.js")));
            String moduleExpression="{Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,"
                    +"BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,"
                    +"Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,"
                    +"BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,"
                    +"Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon}";
            var modules=engine.eval("js","("+moduleExpression+")");
            var anchor=engine.eval("js",Files.readString(resources.resolve("runtime-source-anchor.js"))).execute(modules);
            var damageOracle=layoutOnly?null:ModifierTacticalRegression.oracle(engine);
            if (!layoutOnly) SourceVerification.verifyNativeRecovery(engine);
            engine.eval("js","var verificationStream=new BS.BattleStream();"
                    +"verificationStream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,"
                    +"ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));");
            var battle=engine.eval("js","verificationStream.battle");
            var probe=engine.eval("js",Files.readString(resources.resolve("runtime-domain-probe.js")));
            var capture=(java.util.function.Supplier<JsonObject>)()->JsonParser.parseString(probe.execute(battle,modules,false,null,anchor).asString()).getAsJsonObject();
            var clean=capture.get();
            var observed=new TreeMap<String,String>();
            clean.getAsJsonObject("components").entrySet().forEach(e->observed.put(e.getKey(),SourceVerification.hash(e.getValue().getAsString())));
            var expected=pins(resources.resolve("runtime-pack-components.tsv"));
            check(observed.equals(expected),"Pack initialized components differ: "+observed);
            if(layoutOnly){System.out.println("Pinned LF/CRLF initialized operation fingerprint equivalence PASS");return;}
            engine.eval("js","verificationStream.battle.dex.dataCache.Moves.unsupported={name:'Unsupported',onHit(){return 1;}};Cobblemon.registries.move.register({name:'Unsupported',onHit(){return 1;}},'unsupported');");
            var scoped=capture.get();
            check(!scoped.get("registryEquivalent").getAsBoolean() && scoped.get("scopedRegistryEquivalent").getAsBoolean(),"Unused custom move granted globally or blocked supported slice");
            var components=new TreeMap<>(observed);
            String receipt=SourceVerification.hash(scoped.get("registryReceipt").getAsString());components.put("registryReceipt",receipt);
            var guarded=probe.execute(battle,modules,false,null,anchor,true);
            var configurationLease=guarded.getMember("current");
            check(configurationLease.execute().asBoolean(),"initial configuration receipt unavailable");
            long[] timing={0,0};
            var lease=new RuntimeRegistryLease(receipt,()->true,()->{
                long start=System.nanoTime();boolean ok=configurationLease.execute().asBoolean();
                timing[0]++;timing[1]+=System.nanoTime()-start;return ok?receipt:null;
            });
            var domain=new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,true,false,true,components),expected,true,lease);
            check(domain.effectiveState().known(),"narrow pack receipt absent");
            long comparisonStart=System.nanoTime();
            PublicPipelineRegression.run(domain);
            NaturalAcceptanceRegression.run(domain);
            ModifierTacticalRegression.run(domain,damageOracle);
            System.out.println("Public configuration rechecks="+timing[0]+" totalMs="+timing[1]/1_000_000
                    +" providerMatrixMs="+(System.nanoTime()-comparisonStart)/1_000_000);
            engine.eval("js","['p1','p2','sides','log','inputLog','activePokemon','activeTarget'].forEach(key=>Object.defineProperty(verificationStream.battle,key,{get(){throw Error('private state touched');}}));");
            check(scoped.equals(capture.get()),"pack probe touched private actor state");
            ModifierTacticalRegression.invalidation(domain,()->engine.eval("js","verificationStream.battle.dex.dataCache.Aliases.unverified='mewtwo';"));
            engine.eval("js","delete verificationStream.battle.dex.dataCache.Aliases.unverified;");
            check(!domain.effectiveState().known(),"restoration revived comparison receipt");
            engine.eval("js","Cobblemon.registries.bagItem.contents.set('unverified',()=>{});");
            check(!capture.get().get("scopedRegistryEquivalent").getAsBoolean(),"foreign bag callback could forge a public witness");
            engine.eval("js","Cobblemon.registries.bagItem.reset();");
            engine.eval("js","Cobblemon.registries.ability.register({name:'Unverified',onStart(p){this.add('-ability',p,'Pressure');}},'unverified');");
            check(!capture.get().get("scopedRegistryEquivalent").getAsBoolean(),"foreign ability could forge public identity witness");
            check(!domain.effectiveState().known(),"foreign entry ability retained receipt");
            engine.eval("js","Cobblemon.registries.ability.reset();");
            engine.eval("js","let genuine=verificationStream.battle.dex.dataCache.Abilities.pressure;"
                    +"Cobblemon.registries.ability.register({...genuine,onStart:eval('({'+genuine.onStart.toString()+'}).onStart')},'pressure');");
            check(!capture.get().get("scopedRegistryEquivalent").getAsBoolean(),"equal-source foreign registry closure certified");
            check(!domain.effectiveState().known(),"changed registry retained old scoped receipt");
            engine.eval("js","Cobblemon.registries.ability.reset();");
            // Unselected dex abilities/items/aliases also affect public witnesses:
            // no registry API call is necessary to install a forged entry effect.
            for(String table:List.of("Abilities","Items","Conditions")) {
                engine.eval("js","verificationStream.battle.dex.dataCache[\""+table+"\"].unverified={onStart(){this.add('-ability','p2a: Public','Pressure');}};");
                boolean blocked=false;try{capture.get();}catch(RuntimeException unverified){blocked=true;}
                check(blocked,"unselected dex public-witness hook accepted: "+table);
                engine.eval("js","delete verificationStream.battle.dex.dataCache[\""+table+"\"].unverified;");
            }
            engine.eval("js","var savedPressure=verificationStream.battle.dex.abilities.abilityCache.get('pressure');"
                    +"verificationStream.battle.dex.abilities.abilityCache.set('pressure',Object.assign(Object.create(Object.getPrototypeOf(savedPressure)),savedPressure));");
            check(capture.get().get("scopedRegistryEquivalent").getAsBoolean(),"source-equivalent native cache reconstruction rejected");
            engine.eval("js","verificationStream.battle.dex.abilities.abilityCache.set('pressure',{...savedPressure,onStart(){this.add('-ability','p2a: Public','Pressure');}});");
            boolean cacheRejected=false;try{capture.get();}catch(RuntimeException unverified){cacheRejected=true;}
            check(cacheRejected,"direct cache callback bypassed source provenance");
            engine.eval("js","verificationStream.battle.dex.abilities.abilityCache.set('pressure',savedPressure);");
            engine.eval("js","Cobblemon.registries.species.contents.set('unverified',verificationStream.battle.dex.species.speciesCache.get('mewtwo'));");
            boolean speciesRejected=false;try{capture.get();}catch(RuntimeException unverified){speciesRejected=true;}
            check(speciesRejected,"foreign species key could spoof the supported public catalog identity");
            engine.eval("js","Cobblemon.registries.species.contents.delete('unverified');");
            // A direct pack extension can replace dex callbacks without using
            // Cobblemon's registry API. Equal text must not defeat the boot anchor.
            engine.eval("js","let d=verificationStream.battle.dex.dataCache.Abilities;let originalPressure=d.pressure;"
                    +"d.pressure={...originalPressure,onStart:eval('({'+originalPressure.onStart.toString()+'}).onStart')};");
            boolean rejected=false;try{capture.get();}catch(RuntimeException expectedFailure){rejected=true;}
            check(rejected,"unobserved equal-text dex closure bypassed boot source identity");
            System.out.println("Pack source/operation scopes PASS: exact files, boot callback identities, scoped custom isolation, privacy and permanent lease withdrawal");
        }
    }
}
