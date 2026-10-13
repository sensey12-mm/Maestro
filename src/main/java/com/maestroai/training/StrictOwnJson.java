package com.maestroai.training;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;

/** Bound, strict syntax; Gson's default object reader silently accepts duplicate keys. */
final class StrictOwnJson {
    private StrictOwnJson() {}
    static JsonObject read(String text) {
        try (var reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            JsonElement result = value(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException();
            return result.getAsJsonObject();
        } catch (IOException invalid) { throw new IllegalArgumentException("invalid own request JSON"); }
    }
    private static JsonElement value(JsonReader r, int depth) throws IOException {
        if (depth > 16) throw new IllegalArgumentException();
        return switch (r.peek()) {
            case BEGIN_OBJECT -> {
                r.beginObject(); var o = new JsonObject();
                while (r.hasNext()) { String key = r.nextName(); if (o.has(key)) throw new IllegalArgumentException(); o.add(key, value(r, depth + 1)); }
                r.endObject(); yield o;
            }
            case BEGIN_ARRAY -> {
                r.beginArray(); var a = new JsonArray(); while (r.hasNext()) a.add(value(r, depth + 1)); r.endArray(); yield a;
            }
            case STRING -> new JsonPrimitive(r.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(r.nextString()));
            case BOOLEAN -> new JsonPrimitive(r.nextBoolean());
            case NULL -> { r.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException();
        };
    }
}
