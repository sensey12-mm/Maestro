package com.maestroai.ai;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fail-closed live promotion boundary. A flag alone never authorizes a
 * shadow experiment: a candidate must also be independently promoted and
 * validated as a legal action. No candidate is registered in Phase 3.25.
 */
public final class LivePromotionGate {
    public static final String PROPERTY = "maestroai.livePromotion.enabled";

    private LivePromotionGate() {}

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "false"));
    }

    public static <T> T choose(Supplier<T> legacy, Supplier<T> promoted,
                               Predicate<T> legal, boolean approved) {
        if (!enabled() || !approved || promoted == null || legal == null)
            return legacy.get();
        try {
            T candidate = promoted.get();
            if (candidate != null && legal.test(candidate)) return candidate;
        } catch (RuntimeException ignored) {
            // A broken experiment must never prevent the legacy action.
        }
        return legacy.get();
    }
}
