package com.maestroai.training;

import java.util.*;
import java.util.function.BooleanSupplier;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.ExecutionCoverage.*;

/** Initialized PUBLIC engine receipts, separate from battle actor evidence.
 * No version-only, missing-announcement or owner-supplied pack-purity flag.
 */
public final class RuntimeDomain {
    public static final String SOURCE = "cobblemon-7553235:initialized-domain-v1";
    public record Snapshot(boolean format, boolean extraEvents, boolean overrides,
                           boolean exactRegistryOverlay, Map<String,String> components) {
        public Snapshot { components = Map.copyOf(components); }
        /** Prior synthetic fixtures may construct a conservative unknown override. */
        public Snapshot(boolean format, boolean extraEvents, boolean overrides, Map<String,String> components) {
            this(format, extraEvents, overrides, false, components);
        }
    }
    private final Snapshot snapshot;
    private final Map<String,String> expected;
    private final BooleanSupplier current;
    private final boolean sourceFiles;
    public RuntimeDomain(Snapshot snapshot, Map<String,String> expected, boolean sourceFiles, BooleanSupplier current) {
        this.snapshot = Objects.requireNonNull(snapshot); this.expected = Map.copyOf(expected);
        this.sourceFiles = sourceFiles; this.current = Objects.requireNonNull(current);
    }
    public boolean current() { try { return current.getAsBoolean(); } catch (RuntimeException missing) { return false; } }
    private boolean matches(String component) {
        String pin = expected.get(component);
        return pin != null && pin.matches("[0-9a-f]{64}") && pin.equals(snapshot.components().get(component));
    }
    private boolean validRegistryReceipt() {
        String digest = snapshot.components().get("registryReceipt");
        return digest != null && digest.matches("[0-9a-f]{64}");
    }
    public Set<String> missing() {
        var reasons = new TreeSet<String>();
        if (!current()) reasons.add("RUNTIME_DOMAIN_RECEIPT_EXPIRED");
        if (!sourceFiles) reasons.add("LOADED_SIMULATOR_SOURCE_UNVERIFIED");
        if (!snapshot.format()) reasons.add("INITIALIZED_FORMAT_OR_DEX_UNSUPPORTED");
        if (snapshot.extraEvents()) reasons.add("DYNAMIC_FORMAT_HOOKS_UNVERIFIED");
        // A nonempty public registry is permitted only when its complete
        // data fields AND live callback object identities equal the exact
        // initialized pinned dex definitions, under a current receipt.
        if (snapshot.overrides() && (!snapshot.exactRegistryOverlay()
                || !validRegistryReceipt()))
            reasons.add("REGISTRY_CALLBACK_OR_FIELD_OVERLAY_UNVERIFIED");
        for (String component : List.of("core", "tables", "speciesHooks"))
            if (!matches(component)) reasons.add("INITIALIZED_" + component.toUpperCase(Locale.ROOT) + "_UNVERIFIED");
        return Set.copyOf(reasons);
    }
    public Known<Boolean> effectiveState() {
        return missing().isEmpty() ? Known.of(true, SOURCE + ":effective-state") : Known.unknown();
    }
    public Scopes scopes() {
        return Scopes.receipt(op -> {
            if (!missing().isEmpty()) return Known.unknown();
            // Numeric opponent bounds need separately matching PUBLIC catalog data.
            if (Set.of(Operation.DAMAGE, Operation.SPEED, Operation.ORDER).contains(op) && !matches("catalog"))
                return Known.unknown();
            return Known.of(true, SOURCE + ":" + op);
        }, this::current);
    }
}
