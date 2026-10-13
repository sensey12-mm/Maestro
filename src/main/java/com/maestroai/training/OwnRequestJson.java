package com.maestroai.training;

import com.google.gson.*;
import java.util.*;

/** Used only AFTER independent owning-actor dispatch filtering. Never scan raw history. */
public final class OwnRequestJson {
    private OwnRequestJson() {}
    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
        return e.getAsString();
    }
    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException();
        return e.getAsBoolean();
    }
    private static List<String> types(JsonObject o, String key) {
        var result = new ArrayList<String>();
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
            result.add(e.getAsString());
        }
        return List.copyOf(result);
    }
    public static OwnRequestTransport.Request parse(String raw) {
        try { return parseOwned(raw); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("unsupported own request schema"); }
    }
    private static OwnRequestTransport.Request parseOwned(String raw) {
        if (raw == null || !raw.startsWith("|request|{") || raw.length() > 131072 || raw.indexOf('\n') >= 0)
            throw new IllegalArgumentException();
        // Strict reader rejects duplicate keys, trailing data and excessive nesting.
        JsonObject r = StrictOwnJson.read(raw.substring(9));
        if (r.has("ally") || r.has("teamPreview")) throw new IllegalArgumentException();
        OwnRequestTransport.Mode mode = OwnRequestTransport.Mode.MOVE;
        if (r.has("wait") && bool(r, "wait")) mode = OwnRequestTransport.Mode.WAIT;
        else if (r.has("forceSwitch")) {
            JsonArray f = r.getAsJsonArray("forceSwitch");
            if (f.size() != 1 || !f.get(0).isJsonPrimitive() || !f.get(0).getAsJsonPrimitive().isBoolean() || !f.get(0).getAsBoolean()) throw new IllegalArgumentException();
            mode = OwnRequestTransport.Mode.FORCED;
        } else if (!r.has("active") || r.getAsJsonArray("active").size() != 1) throw new IllegalArgumentException();
        JsonObject side = r.getAsJsonObject("side");
        var pokemon = new ArrayList<OwnRequestTransport.Pokemon>();
        for (JsonElement element : side.getAsJsonArray("pokemon")) {
            JsonObject p = element.getAsJsonObject(); String[] details = string(p, "details").split(", ", -1);
            if (details.length < 2 || details[0].isBlank()) throw new IllegalArgumentException();
            UUID uuid = UUID.fromString(details[1]); if (!uuid.toString().equals(details[1])) throw new IllegalArgumentException();
            int level = 100; boolean found = false;
            for (int i = 2; i < details.length; i++) {
                if (details[i].matches("L[1-9][0-9]{0,2}")) {
                    if (found) throw new IllegalArgumentException(); found = true; level = Integer.parseInt(details[i].substring(1));
                } else if (!Set.of("M", "F", "shiny").contains(details[i])) throw new IllegalArgumentException();
            }
            if (level < 1 || level > 100) throw new IllegalArgumentException();
            var stats = new HashMap<String, Integer>(); JsonObject s = p.getAsJsonObject("stats");
            if (!s.keySet().equals(Set.of("atk", "def", "spa", "spd", "spe"))) throw new IllegalArgumentException();
            for (String stat : s.keySet()) {
                String n = s.get(stat).toString(); if (!n.matches("[1-9][0-9]{0,4}")) throw new IllegalArgumentException();
                int value = Integer.parseInt(n); if (value > 10000) throw new IllegalArgumentException(); stats.put(stat, value);
            }
            String item = string(p, "item"); if (!item.matches("[a-z0-9]{0,64}")) throw new IllegalArgumentException();
            var sample = new OwnRequestEvidence.Sample(uuid, string(p, "ident"), string(p, "condition"), bool(p, "active"), string(p, "ability"), types(p, "types"));
            pokemon.add(new OwnRequestTransport.Pokemon(sample, level, stats, item.isEmpty() ? "none" : item,
                    types(p, "baseTypes"), string(p, "terastallized"), bool(p, "commanding") || bool(p, "reviving")));
        }
        return new OwnRequestTransport.Request(string(side, "id"), mode, pokemon);
    }
}
