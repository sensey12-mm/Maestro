package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Actual native registrations and production configuration leases; no actor/request inputs. */
final class MoveAttestationRegression {
    private static final Set<String> FIELD_DEPENDENCIES=Set.of("psychicterrain","tailwind","sandstorm","hail","snowscape","spikes","stealthrock","stickyweb","toxicspikes");
    private static Value oracle(Context engine)throws Exception{
        String source=Files.readString(Path.of("verification/move_attestation_oracle.cjs"));
        return engine.eval("js","(()=>{const module={exports:{}};"+source+";return module.exports;})()");
    }
    private static void inventory(Value oracle){
        Value fields=oracle.getMember("fieldDependencies");var actual=new TreeSet<String>();
        if(fields==null||!fields.hasArrayElements())throw new AssertionError("Field dependency inventory missing");
        for(long i=0;i<fields.getArraySize();i++)if(!actual.add(fields.getArrayElement(i).asString()))throw new AssertionError("Duplicate field dependency");
        if(!actual.equals(FIELD_DEPENDENCIES)||actual.stream().anyMatch(CobblemonMechanics.supportedMoveIds()::contains))
            throw new AssertionError("Field dependency/action separation drifted: "+actual);
    }
    static void inventory()throws Exception{
        try(var engine=Context.newBuilder("js").allowExperimentalOptions(true).option("engine.WarnInterpreterOnly","false").build()){
            inventory(oracle(engine));
        }
        System.out.println("Move attestation inventory PASS: "+FIELD_DEPENDENCIES.size()+" field dependencies remain nonselectable; Java/JS inventory checked before native callbacks");
    }
    static void run(Context engine, Value probe, Value battle, Value modules, Value anchor,
                    Map<String,String> expected) throws Exception {
        Value oracle=oracle(engine);inventory(oracle);
        Value ids=engine.eval("js",new Gson().toJson(new TreeSet<>(CobblemonMechanics.supportedMoveIds())));
        var result=JsonParser.parseString(oracle.execute(modules,anchor,probe,battle,ids).asString()).getAsJsonObject();
        if(result.get("selected").getAsInt()!=CobblemonMechanics.supportedMoveIds().size())
            throw new AssertionError("Calculator move coverage drifted");
        if(result.get("fieldDependencies").getAsInt()!=FIELD_DEPENDENCIES.size())
            throw new AssertionError("Field dependency/action separation drifted");
        var components=new TreeMap<>(expected);
        components.put("registryReceipt","0".repeat(64));
        for(var value:result.getAsJsonArray("badRegistries")) {
            var altered=value.getAsJsonObject();
            var domain=new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,true,
                    altered.get("registryEquivalent").getAsBoolean(),
                    altered.get("scopedRegistryEquivalent").getAsBoolean(),components),expected,true,()->true);
            if(domain.effectiveState().known() || domain.scopes().get(ExecutionCoverage.Operation.DAMAGE).known() || domain.scopes().get(ExecutionCoverage.Operation.ENTRY).known())
                throw new AssertionError("Altered selected move retained an operation certificate");
        }
        System.out.println("Native selected-move attestation PASS: "+result.get("selected").getAsInt()+" IDs + "+result.get("fieldDependencies").getAsInt()+" field dependencies; "
                +result.get("checks").getAsInt()+" checks; altered definitions withdraw Java operation scopes");
    }
}
