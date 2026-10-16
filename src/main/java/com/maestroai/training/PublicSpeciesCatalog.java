package com.maestroai.training;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Factual public rules values covered by the initialized catalog receipt.
 * A similarly named species or a changed datapack value cannot borrow it.
 */
public final class PublicSpeciesCatalog {
    private PublicSpeciesCatalog() {}
    private static final Map<String,PublicMechanicsEvidence.Species> ENTRIES=load();
    private static Map<String,PublicMechanicsEvidence.Species> load() {
        var result=new TreeMap<String,PublicMechanicsEvidence.Species>();
        try(var stream=PublicSpeciesCatalog.class.getResourceAsStream("/com/maestroai/training/public-species-catalog.tsv")) {
            if(stream==null) return Map.of();
            for(String line:new String(stream.readAllBytes(),StandardCharsets.UTF_8).lines().toList()) {
                var p=line.split("\t",-1); if(p.length!=9 || !p[0].matches("[a-z0-9]+")) return Map.of();
                var stats=new TreeMap<String,Integer>();var keys=List.of("hp","atk","def","spa","spd","spe");
                for(int i=0;i<6;i++) stats.put(keys.get(i),Integer.parseInt(p[i+1]));
                var species=new PublicMechanicsEvidence.Species(stats,List.of(p[7].split(",")),p[8].equals("-")?null:Integer.valueOf(p[8]));
                if(result.put(p[0],species)!=null) return Map.of();
            }
            return Map.copyOf(result);
        } catch(IOException | RuntimeException unsupported) { return Map.of(); }
    }
    public static Set<String> ids(){return ENTRIES.keySet();}
    public static PublicMechanicsEvidence.Species verified(String id,PublicMechanicsEvidence.Species actual) {
        var expected=ENTRIES.get(id); return expected!=null && expected.equals(actual)?actual:null;
    }
}
