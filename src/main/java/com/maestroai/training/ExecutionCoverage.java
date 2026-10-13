package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Operation prerequisites, not a declaration that a public stream proves pack purity. */
public final class ExecutionCoverage {
    private ExecutionCoverage() {}
    public enum Operation { DAMAGE, PRIORITY, SPEED, ORDER, ENTRY, SETUP }
    /** Fixtures may supply explicit assumptions. Runtime receipts inspect actual
     * initialized public configuration and expire with their owning choice.
     */
    public static final class Scopes {
        private final Map<Operation, Known<Boolean>> supported;
        private final java.util.function.Function<Operation, Known<Boolean>> receipt;
        private final java.util.function.BooleanSupplier current;
        private Scopes(Map<Operation, Known<Boolean>> supported) { this.supported = Map.copyOf(supported); receipt = null; current = () -> true; }
        private Scopes(java.util.function.Function<Operation, Known<Boolean>> receipt, java.util.function.BooleanSupplier current) {
            supported = Map.of(); this.receipt = receipt; this.current = current;
        }
        static Scopes receipt(java.util.function.Function<Operation, Known<Boolean>> receipt, java.util.function.BooleanSupplier current) {
            return new Scopes(receipt, current);
        }
        public static Scopes conditional(Map<Operation, Known<Boolean>> supported) { return new Scopes(supported); }
        public static Scopes all(Known<Boolean> value) {
            var map = new EnumMap<Operation, Known<Boolean>>(Operation.class);
            for (var op : Operation.values()) map.put(op, value);
            return new Scopes(map);
        }
        public static Scopes unknown() { return all(Known.unknown()); }
        public Known<Boolean> get(Operation op) { return !current() ? Known.unknown() : receipt == null ? supported.getOrDefault(op, Known.unknown()) : receipt.apply(op); }
        public boolean current() { return current.getAsBoolean(); }
    }
    /** Only the checked execution provider can issue these immutable certificates.
     * Consumers recalculate against their exact operation inputs; certificates are
     * diagnostics, never an externally reusable permission to run another calculation.
     */
    public static final class Certificate {
        private final PublicFieldEvidence.Capture field;
        private final Operation operation;
        private final Set<String> prerequisites, missing;
        private final java.util.function.BooleanSupplier transportCurrent;
        Certificate(PublicFieldEvidence.Capture field, Operation operation, Set<String> prerequisites, Set<String> missing) {
            this(field, operation, prerequisites, missing, () -> true);
        }
        Certificate(PublicFieldEvidence.Capture field, Operation operation, Set<String> prerequisites, Set<String> missing,
                    java.util.function.BooleanSupplier transportCurrent) {
            this.field = field; this.operation = operation;
            this.prerequisites = Set.copyOf(prerequisites); this.missing = Set.copyOf(missing);
            this.transportCurrent = transportCurrent;
        }
        public Operation operation() { return operation; }
        public String source() { return field.token() + ":" + operation; }
        public Set<String> prerequisites() { return prerequisites; }
        public Set<String> missing() {
            if (transportCurrent.getAsBoolean()) return missing;
            var current = new TreeSet<>(missing); current.add("EXECUTION_RECEIPT_EXPIRED"); return Set.copyOf(current);
        }
        public boolean supported() { field.requireCurrent(field.turn()); return missing.isEmpty() && transportCurrent.getAsBoolean(); }
    }
}
