package com.maestroai.ai;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.google.gson.*;
import com.maestroai.training.RuntimeDomain;
import com.maestroai.training.RuntimeRegistryLease;
import com.maestroai.training.RuntimeSourceAudit;
import com.maestroai.training.NativeRegistryEvidence;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;
import com.cobblemon.mod.relocations.graalvm.polyglot.PolyglotException;
import com.cobblemon.mod.relocations.graalvm.polyglot.proxy.ProxyExecutable;
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
    private static final Set<String> METHOD_GROUPS=Set.of("REGISTRY_CONSTRUCTORS","REGISTRY_IMPLEMENTATION",
            "NORMALIZATION_HELPERS","REGISTRY_SERIALIZATION","ENGINE_HELPERS","ENGINE_CONSTRUCTORS",
            "BATTLE","ACTIONS","QUEUE","FIELD","POKEMON","DEX","DEX_MOVES","DEX_ITEMS","DEX_ABILITIES",
            "DEX_SPECIES","DEX_CONDITIONS","DEX_FORMATS");
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private enum CaptureFailure {
        INITIALIZED_PUBLIC_PROBE_UNAVAILABLE, OWN_CHOICE_EXPIRED, PUBLIC_CONTEXT_CHANGED,
        PUBLIC_MODULE_LOOKUP_REJECTED, PUBLIC_BATTLE_LOOKUP_REJECTED,
        PUBLIC_CONFIGURATION_REJECTED, PUBLIC_CONFIGURATION_LIMIT,
        PUBLIC_CONFIGURATION_ASSOCIATION_REJECTED, PUBLIC_CONFIGURATION_SPECIES_REJECTED,
        PUBLIC_CONFIGURATION_REGISTRY_REJECTED, PUBLIC_CONFIGURATION_METHODS_REJECTED,
        PUBLIC_CONFIGURATION_TABLES_REJECTED, PUBLIC_CONFIGURATION_CACHE_REJECTED, PUBLIC_CONFIGURATION_SERIALIZATION_REJECTED,
        PUBLIC_CONFIGURATION_DECODE_REJECTED
    }
    private static final class State {
        long epoch; boolean boot, source; int mutations; Object context, dispatcher;
        Value nativeFactories, sourceAnchor, itemObserver;
        boolean pack;
        RuntimeSourceAudit.Report packAudit;
        CaptureFailure captureFailure = CaptureFailure.INITIALIZED_PUBLIC_PROBE_UNAVAILABLE;
        NativePending nativePending;
        String lastMethodAudit, methodGroup, methodFailureKind;
        Set<String> methodFailures=Set.of();
        RuntimeSourceAudit.Report audit = new RuntimeSourceAudit.Report(RuntimeSourceAudit.Reason.AUDIT_UNAVAILABLE, "");
    }
    private record NativePending(NativeRegistryEvidence.Dispatch dispatch, long epoch, boolean payloadMatched) {}
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
    private static String publicModules() {
        return "{Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,"
                    + "BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,"
                    + "Pokemon:require('./sim/pokemon').Pokemon,"
                    + "Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,"
                    + "DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,"
                    + "Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon}";
    }
    private static Map<String,Set<String>> alternatives(String name) throws IOException {
        var result = new TreeMap<String,Set<String>>();
        for (String line : resource(name).lines().toList()) {
            String[] p = line.split("\t", -1);
            if (p.length != 3 || result.put(p[0], Set.of(p[1],p[2])) != null) throw new IOException("invalid profile pins");
        }
        return result;
    }
    private static RuntimeSourceAudit.Report verifySource(Object service, boolean pack) {
        try {
            var required = new TreeMap<String,Set<String>>();
            pins("runtime-domain-files.tsv").forEach((k,v)->required.put(k,Set.of(v)));
            if (pack) required.putAll(alternatives("runtime-pack-files.tsv"));
            var optional = new TreeMap<String,Set<String>>();
            if (pack) {
                optional.putAll(alternatives("runtime-pack-optional.tsv"));
                pins("runtime-pack-metadata.tsv").forEach((k,v)->optional.put(k,Set.of(v)));
            }
            return RuntimeSourceAudit.inspectAlternatives(Path.of("showdown"), required,
                    optional,
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
        s.nativeFactories = null; s.nativePending = null;
        s.pack = false; s.sourceAnchor = null; s.itemObserver = null; s.packAudit = null;
        s.captureFailure = CaptureFailure.INITIALIZED_PUBLIC_PROBE_UNAVAILABLE;
        s.lastMethodAudit=null; s.methodFailures=Set.of();
        s.audit = verifySource(service, false);
        if (!s.audit.verified()) {
            var profile = verifySource(service, true);
            s.packAudit = profile;
            if (profile.verified()) { s.audit = profile; s.pack = true; }
        }
        s.source = s.audit.verified();
    }
    public static synchronized void bootFinished(GraalShowdownService service) {
        var s = state(service); s.epoch++;
        var finished = verifySource(service, s.pack);
        if (!s.pack && s.packAudit != null) s.packAudit = verifySource(service, true);
        if (s.source || !finished.verified()) s.audit = finished;
        s.source &= finished.verified(); s.context = service.getContext(); s.dispatcher = service.getSendBattleMessageFunction();
        s.boot = s.source && s.context != null && s.dispatcher != null;
        s.nativeFactories = null; s.nativePending = null; s.sourceAnchor = null; s.itemObserver = null;
        if (s.boot) try {
            s.sourceAnchor = service.getContext().eval("js", resource("runtime-source-anchor.js"))
                    .execute(service.getContext().eval("js", "(" + publicModules() + ")"));
            if(s.pack) {
                var wires=pins("runtime-pack-item-wires.tsv");
                ProxyExecutable permitted=values->{
                    if(values.length!=2 || !values[0].isString() || !values[1].isString()) return false;
                    String id=values[0].asString(), code=values[1].asString();
                    return code.length()<=65536 && Objects.equals(wires.get(id),hash(code.getBytes(StandardCharsets.UTF_8)));
                };
                ProxyExecutable lifecycle=values->{
                    if(values[0].asBoolean()) mutationStarting(service);else mutationFinished(service);
                    return null;
                };
                s.itemObserver=service.getContext().eval("js",resource("runtime-pack-item-observer.js"))
                        .execute(s.sourceAnchor,permitted,lifecycle);
            }
        } catch (IOException | RuntimeException unavailable) { s.boot = false; }
        logSourceFingerprints(service);
        reportPublicMethods(service);
    }
    private static void logSourceFingerprints(Object service) {
        if(!Boolean.getBoolean("maestroai.envelopeCapture")) return;
        try {
            RuntimeSourceAudit.fingerprints(Path.of("showdown"),pins("runtime-domain-files.tsv"),
                    pins("runtime-domain-classes.tsv"),name->{
                try(var stream=service.getClass().getResourceAsStream("/"+name)) {
                    return stream==null?null:stream.readNBytes(8*1024*1024+1);
                } catch(IOException unavailable){return null;}
            }).forEach(System.out::println);
        } catch(IOException | RuntimeException unavailable) {
            System.out.println("[MaestroAI] PUBLIC_SOURCE_HASH status=UNAVAILABLE diagnosticOnly=true");
        }
    }
    public static synchronized void invalidate(Object service) {
        var s = state(service); s.epoch++; s.boot = false; s.context = null; s.dispatcher = null;
        s.nativeFactories = null; s.nativePending = null; s.sourceAnchor = null; s.itemObserver = null;
    }
    public static synchronized void mutationStarting(Object service) { var s = state(service); s.epoch++; s.mutations++; }
    public static synchronized void mutationFinished(Object service) {
        var s = state(service); s.epoch++; s.mutations = Math.max(0, s.mutations - 1);
        if(s.mutations==0 && service instanceof GraalShowdownService nativeService) reportPublicMethods(nativeService);
    }
    private record MethodAudit(boolean actualBattle, List<String> failures, boolean truncated) {
        private MethodAudit { failures=List.copyOf(failures); }
        String safeSummary() { return "scope="+(actualBattle?"PUBLIC_BATTLE_METHODS":"PUBLIC_MODULES_AND_BOOT_DEX")
                +" failures="+failures+" truncated="+truncated+" actualBattle="+(actualBattle?"INSPECTED_PUBLIC_METHODS_ONLY":"NOT_INSPECTED"); }
    }
    private static MethodAudit methodAudit(State s, Value modules, Value battle) {
        String json=(battle==null?s.sourceAnchor.getMember("auditMethods").execute(modules)
                :s.sourceAnchor.getMember("auditMethods").execute(modules,battle)).asString();
        if(json.length()>4096)throw new IllegalArgumentException("bounded public audit");
        var raw=JsonParser.parseString(json).getAsJsonObject();
        var failures=new ArrayList<String>();
        for(var value:raw.getAsJsonArray("failures")) {
            String code=value.getAsString();
            if(failures.size()>=8 || !s.sourceAnchor.getMember("diagnosticAllowed").execute(code).asBoolean())
                throw new IllegalArgumentException("unreviewed public diagnostic");
            failures.add(code);
        }
        return new MethodAudit(battle!=null,failures,raw.get("truncated").getAsBoolean());
    }
    /** Source-known module/prototype/boot-dex slots only. No battle, actor,
     * request, callback execution or constructor is used by this startup check.
     * Its result never grants or changes a runtime domain. */
    private static synchronized void reportPublicMethods(GraalShowdownService service) {
        if(!Boolean.getBoolean("maestroai.envelopeCapture"))return;
        var s=state(service);
        if(!s.boot || s.sourceAnchor==null || s.mutations!=0 || s.context!=service.getContext())return;
        String summary;
        try { summary=methodAudit(s,service.getContext().eval("js","("+publicModules()+")"),null).safeSummary(); }
        catch(RuntimeException unavailable){summary="status=UNAVAILABLE actualBattle=NOT_INSPECTED";}
        if(!summary.equals(s.lastMethodAudit)) {
            s.lastMethodAudit=summary;
            System.out.println("[MaestroAI] PUBLIC_METHOD_AUDIT "+summary+" grantsScope=false");
        }
    }
    /** Public registry API only. Never receives simulator message/team/request data.
     * A whole map must match pinned native factory bytes before any result is
     * attested. A partial known subset cannot bless unknown initializer code.
     */
    public static synchronized void registryDataStarting(GraalShowdownService service, Map<String,String> data, String type) {
        mutationStarting(service); var s=state(service); s.nativePending=null;
        if (!s.boot || !s.source || s.context!=service.getContext()) return;
        try {
            if(s.nativeFactories==null) s.nativeFactories=service.getContext().eval("js","Object.create(null)");
            if(Set.of("bagItem","heldItem").contains(type)) s.nativeFactories.removeMember(type);
            if(s.mutations!=1) return;
            var dispatch=NativeRegistryEvidence.dispatch(type,data,pins("runtime-native-factories.tsv"));
            if(dispatch!=null) s.nativePending=new NativePending(dispatch,s.epoch,false);
        } catch(IOException | RuntimeException unavailable) { s.nativeFactories=null; }
    }
    /** Observe the exact immutable transport arguments at Value.execute, not a
     * mutable Java map that could change between inspection and dispatch.
     */
    public static synchronized void registryDataPayload(Object service, Object[] actual) {
        var s=state(service); var pending=s.nativePending;
        if(pending==null) return;
        s.nativePending=pending.epoch()==s.epoch && pending.dispatch().matches(actual)
                ? new NativePending(pending.dispatch(),pending.epoch(),true) : null;
    }
    public static synchronized void registryDataFinished(GraalShowdownService service) {
        var s=state(service); var pending=s.nativePending; s.nativePending=null;
        try {
            if(pending!=null && pending.payloadMatched() && s.boot && s.source && s.mutations==1 && pending.epoch()==s.epoch
                    && s.context==service.getContext() && s.nativeFactories!=null) {
                Value snapshot=service.getContext().eval("js",resource("runtime-native-snapshot.js"));
                Value registry=service.getContext().eval("js","Cobblemon.registries."+pending.dispatch().type());
                Value ids=service.getContext().eval("js",new Gson().toJson(new TreeSet<>(pending.dispatch().ids())));
                s.nativeFactories.putMember(pending.dispatch().type(),snapshot.execute(registry,ids));
                if(Boolean.getBoolean("maestroai.envelopeCapture"))
                    System.out.println("[MaestroAI] Native public factory receipt group="+pending.dispatch().type()
                            +" entries="+pending.dispatch().ids().size()+" transportMatched=true runtimeAcceptance=UNVERIFIED");
            }
        } catch(IOException | RuntimeException unavailable) { s.nativeFactories=null; }
        finally { mutationFinished(service); }
    }
    public static synchronized void resetNative(Object service, String type) {
        var s=state(service); s.nativePending=null;
        try {
            if(type==null) s.nativeFactories=null;
            else if(s.nativeFactories!=null) s.nativeFactories.removeMember(type);
        } catch(RuntimeException unavailable) { s.nativeFactories=null; }
    }
    public static synchronized Set<String> unavailable(Object service) {
        State s = STATES.get(service);
        if (s == null) return Set.of("SIMULATOR_BOOT_RECEIPT_NOT_OBSERVED");
        if (!s.source) {
            var reasons = new TreeSet<String>(); reasons.add("LOADED_SIMULATOR_SOURCE_UNVERIFIED");
            reasons.add(s.audit.safeReason());
            if (!s.audit.reference().isEmpty()) reasons.add("SOURCE_REFERENCE:" + s.audit.reference());
            if (s.packAudit != null && !s.packAudit.verified()) {
                reasons.add("PACK_" + s.packAudit.safeReason());
                if (!s.packAudit.reference().isEmpty()) reasons.add("PACK_SOURCE_REFERENCE:" + s.packAudit.reference());
            }
            return Set.copyOf(reasons);
        }
        if (!s.boot) return Set.of("SIMULATOR_CONTEXT_NOT_ATTESTED");
        if (s.mutations != 0) return Set.of("PUBLIC_REGISTRY_MUTATION_NOT_FINISHED");
        var reasons=new TreeSet<String>();reasons.add(s.captureFailure.name());
        s.methodFailures.forEach(code->reasons.add("METHOD_CHECK:"+code));
        if(s.captureFailure==CaptureFailure.PUBLIC_CONFIGURATION_METHODS_REJECTED) {
            if(s.methodGroup!=null)reasons.add("METHOD_GROUP:"+s.methodGroup);
            if(s.methodFailureKind!=null)reasons.add("METHOD_FAILURE_KIND:"+s.methodFailureKind);
        }
        return Set.copyOf(reasons);
    }
    private static synchronized RuntimeDomain failed(GraalShowdownService service, CaptureFailure reason) {
        state(service).captureFailure=reason;
        return null;
    }
    private static synchronized boolean current(GraalShowdownService service, Object context, long epoch) {
        State s = STATES.get(service);
        return s != null && s.boot && s.source && s.mutations == 0 && s.epoch == epoch && s.context == context && service.getContext() == context
                && s.dispatcher == service.getSendBattleMessageFunction();
    }
    public static RuntimeDomain capture(GraalShowdownService service, UUID battleId, BooleanSupplier choiceCurrent) {
        CaptureFailure stage=CaptureFailure.OWN_CHOICE_EXPIRED;
        Value progress=null, auditedModules=null, auditedBattle=null;
        synchronized(SimulatorDomainBridge.class){var s=state(service);s.methodFailures=Set.of();s.methodGroup=null;s.methodFailureKind=null;}
        try {
            if (!choiceCurrent.getAsBoolean()) return failed(service,stage);
            Object context; long epoch; Value nativeFactories, sourceAnchor, itemObserver; boolean pack;
            synchronized (SimulatorDomainBridge.class) {
                var s = STATES.get(service);
                if (s == null || !s.boot || s.mutations != 0) return null;
                context = s.context; epoch = s.epoch; nativeFactories=s.nativeFactories; sourceAnchor=s.sourceAnchor; itemObserver=s.itemObserver; pack=s.pack;
            }
            if (sourceAnchor == null || !current(service, context, epoch)) return failed(service,CaptureFailure.PUBLIC_CONTEXT_CHANGED);
            // battleMap is the source-audited index.js lexical binding. Only the
            // public configuration fields named in the probe may be inspected.
            stage=CaptureFailure.PUBLIC_MODULE_LOOKUP_REJECTED;
            String modules = publicModules();
            String function = resource("runtime-domain-probe.js");
            Value probe=service.getContext().eval("js",function);
            Value publicModules=service.getContext().eval("js","("+modules+")");auditedModules=publicModules;
            stage=CaptureFailure.PUBLIC_BATTLE_LOOKUP_REJECTED;
            Value publicBattle=service.getContext().eval("js","battleMap.get('"+battleId+"').battle");auditedBattle=publicBattle;
            stage=CaptureFailure.PUBLIC_CONFIGURATION_REJECTED;
            if(itemObserver!=null && !itemObserver.getMember("current").execute().asBoolean()) return failed(service,stage);
            progress=service.getContext().eval("js","Object.seal({phase:'PUBLIC_CONFIGURATION_REJECTED',methodGroup:'NONE'})");
            Value captured = probe.execute(publicBattle,publicModules,false,nativeFactories,sourceAnchor,true,progress);
            String json = captured.getMember("json").asString();
            if (json.length() > 12_000_000) return failed(service,CaptureFailure.PUBLIC_CONFIGURATION_LIMIT);
            if (!current(service, context, epoch)) return failed(service,CaptureFailure.PUBLIC_CONTEXT_CHANGED);
            if (!choiceCurrent.getAsBoolean()) return failed(service,CaptureFailure.OWN_CHOICE_EXPIRED);
            stage=CaptureFailure.PUBLIC_CONFIGURATION_DECODE_REJECTED;
            JsonObject raw = JsonParser.parseString(json).getAsJsonObject();
            var components = new TreeMap<String,String>();
            for (var entry : raw.getAsJsonObject("components").entrySet())
                components.put(entry.getKey(), hash(entry.getValue().getAsString().getBytes(StandardCharsets.UTF_8)));
            // SHA-bound snapshot of the actual public registry contents,
            // including serialized callbacks. Exact callback identity is
            // checked inside the live initialized JS probe BEFORE serialization.
            String registry = raw.get("registryReceipt").getAsString();
            if (registry.length() > 4_000_000 || raw.get("registryCount").getAsInt() > 8192)
                return failed(service,CaptureFailure.PUBLIC_CONFIGURATION_LIMIT);
            components.put("registryReceipt", hash(registry.getBytes(StandardCharsets.UTF_8)));
            BooleanSupplier lifecycle = () -> choiceCurrent.getAsBoolean() && current(service, context, epoch)
                    && (itemObserver==null || itemObserver.getMember("current").execute().asBoolean());
            Value configurationLease = captured.getMember("current");
            var registryLease = new RuntimeRegistryLease(components.get("registryReceipt"), lifecycle, () ->
                    configurationLease.execute().asBoolean() ? components.get("registryReceipt") : null);
            boolean reloaded=pack && itemObserver!=null && itemObserver.getMember("complete").execute().asBoolean();
            var expected = pins(reloaded ? "runtime-pack-reload-components.tsv" : pack ? "runtime-pack-components.tsv" : "runtime-domain-components.tsv");
            boolean format = raw.get("mod").getAsString().equals("cobblemon") && raw.get("gen").getAsInt() == 9
                    && raw.get("gameType").getAsString().equals("singles") && Objects.equals(components.get("format"), expected.get("format"));
            return new RuntimeDomain(new RuntimeDomain.Snapshot(format, raw.get("extraEvents").getAsBoolean(),
                    raw.get("overrides").getAsBoolean(),
                    raw.get("registryEquivalent").getAsBoolean(), raw.get("scopedRegistryEquivalent").getAsBoolean(), components), expected, true,
                    registryLease);
        } catch (IOException | RuntimeException unavailable) {
            // Read only our sealed progress cell, never exception messages,
            // unknown identifiers, factory bodies or actor/private payloads.
            if(stage==CaptureFailure.PUBLIC_CONFIGURATION_REJECTED && progress!=null) try {
                var reason=CaptureFailure.valueOf(progress.getMember("phase").asString());
                if(reason.name().startsWith("PUBLIC_CONFIGURATION_")) stage=reason;
            } catch(RuntimeException ignored) { /* Retain the fixed fallback. */ }
            if(stage==CaptureFailure.PUBLIC_CONFIGURATION_METHODS_REJECTED && auditedModules!=null && auditedBattle!=null) try {
                synchronized(SimulatorDomainBridge.class) {
                    var s=state(service);
                    String group=progress.getMember("methodGroup").asString();
                    s.methodGroup=METHOD_GROUPS.contains(group)?group:"UNKNOWN_PUBLIC_GROUP";
                    s.methodFailureKind=unavailable instanceof PolyglotException polyglot
                            ? polyglot.isHostException()?"HOST_EXCEPTION":polyglot.isInternalError()?"ENGINE_INTERNAL_EXCEPTION":"GUEST_EXCEPTION"
                            : unavailable instanceof IllegalStateException?"HOST_STATE_EXCEPTION":"OTHER_EXCEPTION";
                    s.methodFailures=Set.copyOf(methodAudit(s,auditedModules,auditedBattle).failures());
                }
            } catch(RuntimeException ignored) { /* Fixed failure code remains; no exception text. */ }
            return failed(service,stage);
        }
    }
}
