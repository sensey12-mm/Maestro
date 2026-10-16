package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Byte provenance for complete native public registry dispatches only.
 * No source evaluation, request parser, version inference or text-only callback
 * equivalence. Runtime separately snapshots the resulting live callback identities.
 */
public final class NativeRegistryEvidence {
    private NativeRegistryEvidence() {}
    public record Dispatch(String type, Set<String> ids, String payload) {
        public Dispatch { ids=Set.copyOf(ids); }
        public boolean matches(Object[] actual) {
            return actual!=null && actual.length==2 && type.equals(actual[1]) && payload.equals(actual[0]);
        }
    }
    public static Dispatch dispatch(String type, Map<String,String> scripts, Map<String,String> pins) {
        if(type==null || !Set.of("bagItem","heldItem").contains(type) || scripts==null || scripts.size()>64) return null;
        try {
            var copy=new LinkedHashMap<>(scripts);
            Set<String> ids=approved(type,copy,pins);
            if(ids.isEmpty()) return null;
            // Exact pinned sendRegistryData$lambda$0 and joinToString recipe.
            var payload=new StringJoiner(", ","{","}");
            copy.forEach((key,code)->payload.add("\""+key+"\": "+code));
            return new Dispatch(type,ids,payload.toString());
        } catch(RuntimeException unavailable) { return null; }
    }
    public static Set<String> approved(String type, Map<String,String> scripts, Map<String,String> pins) {
        if (type==null || !Set.of("bagItem", "heldItem").contains(type) || scripts == null || scripts.isEmpty()
                || scripts.size()>64) return Set.of();
        var ids=new TreeSet<String>();
        try {
            for(var entry:scripts.entrySet()) {
                String key=entry.getKey(), code=entry.getValue();
                if(key==null || !key.matches("[a-z0-9_]+") || code==null || code.length()>65536) return Set.of();
                String expected=pins.get(type+"/"+key);
                String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(code.getBytes(StandardCharsets.UTF_8)));
                if(expected==null || !expected.equals(actual) || !ids.add(key.replace("_",""))) return Set.of();
            }
            return Set.copyOf(ids);
        } catch(NoSuchAlgorithmException | RuntimeException unsupported) { return Set.of(); }
    }
}
