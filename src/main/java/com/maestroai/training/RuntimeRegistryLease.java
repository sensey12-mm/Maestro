package com.maestroai.training;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Read-only current public registry recheck. Once expired, a receipt cannot revive
 * after an equal-looking registry is restored. No actor/request data API exists here. */
public final class RuntimeRegistryLease implements BooleanSupplier {
    private final String digest;
    private final BooleanSupplier lifecycle;
    private final Supplier<String> observedDigest;
    private boolean expired;

    public RuntimeRegistryLease(String digest, BooleanSupplier lifecycle, Supplier<String> observedDigest) {
        if (digest == null || !digest.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("invalid registry receipt");
        this.digest = digest;
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.observedDigest = Objects.requireNonNull(observedDigest);
    }

    @Override public synchronized boolean getAsBoolean() {
        if (expired) return false;
        try {
            // Mutation/context/dispatcher/choice changes during the query also
            // withdraw the result, even if the data digest happens to match.
            expired = !lifecycle.getAsBoolean() || !digest.equals(observedDigest.get())
                    || !lifecycle.getAsBoolean();
        } catch (RuntimeException unavailable) { expired = true; }
        return !expired;
    }
}
