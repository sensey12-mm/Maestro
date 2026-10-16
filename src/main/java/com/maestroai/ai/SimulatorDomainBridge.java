package com.maestroai.ai;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.google.gson.*;
import com.maestroai.training.RuntimeDomain;
import com.maestroai.training.RuntimeRegistryLease;
import com.maestroai.training.RuntimeSourceAudit;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Observational public-configuration audit. Never receives a team/request payload.
 * Exact boot files + actual initialized public hooks, not an installed-version claim.
 */
public final class SimulatorDomainBridge {
    private SimulatorDomainBridge() {}
    private static final String ROOT = "/com/maestroai/training/";
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static final class State {
        long epoch; boolean boot, source; int mutations; Object context, dispatcher;
        RuntimeSourceAudit.Report audit = new RuntimeSourceAudit.Report(RuntimeSourceAudit.Reason.AUDIT_UNAVAILABLE, "");
    }
    private static State state(Object service) { return STATES.computeIfAbsent(service, key -> new State()); }
    private static String resource(String name) throws IOException {
        try (var stream = SimulatorDomainBridge.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IOException("missing source reference");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String hash(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static Map<String,String> pins(String name) throws IOException {
        var pins = new TreeMap<String,String>();
        for (String line : resource(name).lines().toList()) if (!line.isBlank() && !line.startsWith("#")) {
            String[] p = line.split("\t", -1);
            if (p.length != 2 || pins.put(p[0], p[1]) != null) throw new IOException("invalid source reference");
        }
        return pins;
    }
    private static RuntimeSourceAudit.Report verifySource(Object service) {
        try {
            return RuntimeSourceAudit.inspect(Path.of("showdown"), pins("runtime-domain-files.tsv"),
                    pins("runtime-domain-classes.tsv"), name -> {
                try (var stream = service.getClass().getResourceAsStream("/" + name)) {
                    return stream == null ? null : stream.readAllBytes();
                } catch (IOException unavailable) {
                    return null;
                }
            });
        } catch (IOException | RuntimeException unavailable) {
            return new RuntimeSourceAudit.Report(RuntimeSourceAudit.Reason.AUDIT_UNAVAILABLE, "");
        }
    }
    public static synchronized void bootStarting(Object service) {
        var s = state(service); s.epoch++; s.boot = false; s.context = null; s.dispatcher = null; s.mutations = 0;
        s.audit = verifySource(service); s.source = s.audit.verified();
    }
    public static synchronized void bootFinished(GraalShowdownService service) {
        var s = state(service); s.epoch++;
        var finished = verifySource(service);
        if (s.source || !finished.verified()) s.audit = finished;
        s.source &= finished.verified(); s.context = service.getContext(); s.dispatcher = service.getSendBattleMessageFunction();
        s.boot = s.source && s.context != null && s.dispatcher != null;
    }
    public static synchronized void invalidate(Object service) {
        var s = state(service); s.epoch++; s.boot = false; s.context = null; s.dispatcher = null;
    }
    public static synchronized void mutationStarting(Object service) { var s = state(service); s.epoch++; s.mutations++; }
    public static synchronized void mutationFinished(Object service) { var s = state(service); s.epoch++; s.mutations = Math.max(0, s.mutations - 1); }
    public static synchronized Set<String> unavailable(Object service) {
        State s = STATES.get(service);
        if (s == null) return Set.of("SIMULATOR_BOOT_RECEIPT_NOT_OBSERVED");
        if (!s.source) {
            var reasons = new TreeSet<String>(); reasons.add("LOADED_SIMULATOR_SOURCE_UNVERIFIED");
            reasons.add(s.audit.safeReason());
            if (!s.audit.reference().isEmpty()) reasons.add("SOURCE_REFERENCE:" + s.audit.reference());
            return Set.copyOf(reasons);
        }
        if (!s.boot) return Set.of("SIMULATOR_CONTEXT_NOT_ATTESTED");
        if (s.mutations != 0) return Set.of("PUBLIC_REGISTRY_MUTATION_NOT_FINISHED");
        return Set.of("INITIALIZED_PUBLIC_PROBE_UNAVAILABLE");
    }
    private static synchronized boolean current(GraalShowdownService service, Object context, long epoch) {
        State s = STATES.get(service);
        return s != null && s.boot && s.source && s.mutations == 0 && s.epoch == epoch && s.context == context && service.getContext() == context
                && s.dispatcher == service.getSendBattleMessageFunction();
    }
    public static RuntimeDomain capture(GraalShowdownService service, UUID battleId, BooleanSupplier choiceCurrent) {
        try {
            if (!choiceCurrent.getAsBoolean()) return null;
            Object context; long epoch;
            synchronized (SimulatorDomainBridge.class) {
                var s = STATES.get(service);
                if (s == null || !s.boot || s.mutations != 0) return null;
                context = s.context; epoch = s.epoch;
            }
            if (!current(service, context, epoch)) return null;
            // battleMap is the source-audited index.js lexical binding. Only the
            // public configuration fields named in the probe may be inspected.
            String modules = "{Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,"
                    + "BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,"
                    + "Pokemon:require('./sim/pokemon').Pokemon,"
                    + "Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,"
                    + "DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,"
                    + "Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon}";
            String function = resource("runtime-domain-probe.js");
            String invocation = "(battleMap.get('" + battleId + "').battle," + modules;
            String script = function + invocation + ")";
            String json = service.getContext().eval("js", script).asString();
            if (json.length() > 12_000_000 || !current(service, context, epoch)) return null;
            JsonObject raw = JsonParser.parseString(json).getAsJsonObject();
            var components = new TreeMap<String,String>();
            for (var entry : raw.getAsJsonObject("components").entrySet())
                components.put(entry.getKey(), hash(entry.getValue().getAsString().getBytes(StandardCharsets.UTF_8)));
            // SHA-bound snapshot of the actual public registry contents,
            // including serialized callbacks. Exact callback identity is
            // checked inside the live initialized JS probe BEFORE serialization.
            String registry = raw.get("registryReceipt").getAsString();
            if (registry.length() > 4_000_000 || raw.get("registryCount").getAsInt() > 8192) return null;
            components.put("registryReceipt", hash(registry.getBytes(StandardCharsets.UTF_8)));
            BooleanSupplier lifecycle = () -> choiceCurrent.getAsBoolean() && current(service, context, epoch);
            var registryLease = new RuntimeRegistryLease(components.get("registryReceipt"), lifecycle, () -> {
                // Recheck only global public configuration, never actor events,
                // moves/PP, teams or private requests. Registry changes outside
                // the observed service API must also withdraw an old receipt.
                String observed = service.getContext().eval("js", function + invocation + ",true)").asString();
                if (observed.length() > 4_500_000) return null;
                JsonObject fresh = JsonParser.parseString(observed).getAsJsonObject();
                if (!fresh.get("registryEquivalent").getAsBoolean()) return null;
                return hash(fresh.get("registryReceipt").getAsString().getBytes(StandardCharsets.UTF_8));
            });
            var expected = pins("runtime-domain-components.tsv");
            boolean format = raw.get("mod").getAsString().equals("cobblemon") && raw.get("gen").getAsInt() == 9
                    && raw.get("gameType").getAsString().equals("singles") && Objects.equals(components.get("format"), expected.get("format"));
            return new RuntimeDomain(new RuntimeDomain.Snapshot(format, raw.get("extraEvents").getAsBoolean(),
                    raw.get("overrides").getAsBoolean(),
                    raw.get("registryEquivalent").getAsBoolean(), components), expected, true,
                    registryLease);
        } catch (IOException | RuntimeException unavailable) { return null; }
    }
}
