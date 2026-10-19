package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.*;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Executes pristine upstream factories through the pinned receiveData API.
 * Their source stays in the external dependency; only hashes ship in the mod.
 */
final class NativeRegistryRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Value prepare(Context engine,Value probe,Value battle,Value modules,Path resources) throws Exception {
        var pins=new TreeMap<String,String>();
        for(String line:Files.readAllLines(resources.resolve("runtime-native-factories.tsv"))) {
            var p=line.split("\t");pins.put(p[0],p[1]);
        }
        check(pins.size()==12,"native factory scope changed without review");
        var nativeRefs=engine.eval("js","Object.create(null)");
        var snapshot=engine.eval("js",Files.readString(resources.resolve("runtime-native-snapshot.js")));
        for(String type:List.of("bagItem","heldItem")) {
            var scripts=new LinkedHashMap<String,String>();
            for(String key:pins.keySet()) if(key.startsWith(type+"/")) {
                String id=key.substring(type.length()+1),folder=type.equals("bagItem")?"bag_items":"held_items";
                try(var stream=NativeRegistryRegression.class.getResourceAsStream("/data/cobblemon/"+folder+"/"+id+".js")) {
                    if(stream==null)throw new AssertionError("Pinned native factory missing");
                    scripts.put(id,new String(stream.readAllBytes(),StandardCharsets.UTF_8));
                }
            }
            var dispatch=NativeRegistryEvidence.dispatch(type,scripts,pins);
            check(dispatch!=null,"original native source rejected");
            Object[] actual={dispatch.payload(),type};
            check(dispatch.matches(actual),"exact immutable transport refused");
            check(!dispatch.matches(new Object[]{dispatch.payload()+" ",type}),"changed transport blessed");
            check(!dispatch.matches(new Object[]{dispatch.payload(),"move"}),"foreign registration kind blessed");
            var changed=new LinkedHashMap<>(scripts);changed.replaceAll((k,v)->v+"\n");
            check(NativeRegistryEvidence.dispatch(type,changed,pins)==null,"changed source accepted from equivalent-looking code");
            changed=new LinkedHashMap<>(scripts);changed.put("unverified", "{use(){}} ");
            check(NativeRegistryEvidence.dispatch(type,changed,pins)==null,"known subset blessed an unknown initializer");
            engine.getBindings("js").getMember("receiveData").execute(actual);
            check(!JsonParser.parseString(probe.execute(battle,modules,true,nativeRefs).asString())
                    .getAsJsonObject().get("registryEquivalent").getAsBoolean(),"native registration accepted without dispatch provenance");
            var registry=engine.eval("js","Cobblemon.registries."+type);
            var ids=engine.eval("js",new com.google.gson.Gson().toJson(new TreeSet<>(dispatch.ids())));
            nativeRefs.putMember(type,snapshot.execute(registry,ids));
        }
        var poison=new AbstractMap<String,String>() {
            public Set<Entry<String,String>> entrySet(){throw new AssertionError("foreign payload inspected");}
        };
        check(NativeRegistryEvidence.dispatch("request",poison,pins)==null,"private channel accepted");
        var capture=(java.util.function.Supplier<com.google.gson.JsonObject>)()->JsonParser
                .parseString(probe.execute(battle,modules,true,nativeRefs).asString()).getAsJsonObject();
        check(capture.get().get("registryEquivalent").getAsBoolean(),"whole native registry remains blocked");
        String digest=SourceVerification.hash(capture.get().get("registryReceipt").getAsString());
        var lease=new RuntimeRegistryLease(digest,()->true,()->{
            var raw=capture.get();return raw.get("registryEquivalent").getAsBoolean()
                    ?SourceVerification.hash(raw.get("registryReceipt").getAsString()):null;
        });
        check(lease.getAsBoolean(),"native receipt unavailable");
        // Same source text, different closure identity: forbidden even after
        // a genuine prior registration. Restore from private observer snapshot.
        engine.eval("js","var nativeUse=Cobblemon.registries.bagItem.contents.get('potion').use;"
                +"Cobblemon.registries.bagItem.contents.get('potion').use=eval('({'+nativeUse.toString()+'}).use');");
        check(!capture.get().get("registryEquivalent").getAsBoolean(),"foreign native closure accepted");
        check(!lease.getAsBoolean(),"native callback mutation did not expire receipt");
        engine.eval("js","Cobblemon.registries.bagItem.contents.get('potion').use=nativeUse;");
        check(capture.get().get("registryEquivalent").getAsBoolean() && !lease.getAsBoolean(),"restoration revived consumed native receipt");
        engine.eval("js","Cobblemon.registries.bagItem.contents.get('potion').extraEffect=true;");
        check(!capture.get().get("registryEquivalent").getAsBoolean(),"native field mutation accepted");
        engine.eval("js","delete Cobblemon.registries.bagItem.contents.get('potion').extraEffect;"
                +"Cobblemon.registries.bagItem.register({use(){}},'unknownfixture');");
        check(!capture.get().get("registryEquivalent").getAsBoolean(),"custom bag effect accepted");
        engine.eval("js","Cobblemon.registries.bagItem.contents.delete('unknownfixture');");
        check(capture.get().get("registryEquivalent").getAsBoolean(),"independent new native receipt unavailable");
        System.out.println("Native registry source regressions PASS: 12 original factories, exact transport, private-channel guard, foreign closures/fields and irreversible withdrawal");
        return nativeRefs;
    }
}
