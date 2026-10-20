package com.maestroai.training;

import com.cobblemon.mod.relocations.graalvm.polyglot.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Actual native registrations and production configuration leases; no actor/request inputs. */
final class MoveAttestationRegression {
    static void run(Context engine, Value probe, Value battle, Value modules, Value anchor,
                    Map<String,String> expected) throws Exception {
        String source=Files.readString(Path.of("verification/move_attestation_oracle.cjs"));
        Value oracle=engine.eval("js","(()=>{const module={exports:{}};"+source+";return module.exports;})()");
        Value ids=engine.eval("js",new Gson().toJson(new TreeSet<>(CobblemonMechanics.supportedMoveIds())));
        var result=JsonParser.parseString(oracle.execute(modules,anchor,probe,battle,ids).asString()).getAsJsonObject();
        if(result.get("selected").getAsInt()!=CobblemonMechanics.supportedMoveIds().size())
            throw new AssertionError("Calculator move coverage drifted");
        var components=new TreeMap<>(expected);
        components.put("registryReceipt","0".repeat(64));
        for(var value:result.getAsJsonArray("badRegistries")) {
            var altered=value.getAsJsonObject();
            var domain=new RuntimeDomain(new RuntimeDomain.Snapshot(true,false,true,
                    altered.get("registryEquivalent").getAsBoolean(),
                    altered.get("scopedRegistryEquivalent").getAsBoolean(),components),expected,true,()->true);
            if(domain.effectiveState().known() || domain.scopes().get(ExecutionCoverage.Operation.DAMAGE).known())
                throw new AssertionError("Altered selected move retained an operation certificate");
        }
        System.out.println("Native selected-move attestation PASS: "+result.get("selected").getAsInt()+" IDs; "
                +result.get("checks").getAsInt()+" checks; altered definitions withdraw Java operation scopes");
    }
}
