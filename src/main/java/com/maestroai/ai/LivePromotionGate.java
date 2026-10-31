package com.maestroai.ai;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;
import com.maestroai.training.ShadowPromotionPreparation;

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

    /**
     * Reversible candidate transport for an independently derived current-turn
     * Shadow proposal. Legacy is always the fallback on absent approval,
     * incomplete evidence, changed source, illegality or materializer failure.
     *
     * IMPORTANT: production calls with ownerApproved=false until the owner
     * explicitly authorizes a later activation change. A JVM property alone
     * cannot set this argument. This method does not certify Phase 7 advantage.
     */
    public static <T> T choosePrepared(
            Supplier<T> legacy,ShadowPromotionPreparation.Proposal candidate,
            Object battle,Object originalRequest,int turn,
            Function<String,T> materialize,Predicate<T> independentlyLegal,
            boolean ownerApproved) {
        if(!enabled() || !ownerApproved || candidate==null
                || materialize==null || independentlyLegal==null)
            return legacy.get();
        try {
            if(!candidate.current(battle,originalRequest,turn))
                return legacy.get();
            T action=materialize.apply(candidate.action());
            if(action!=null && candidate.current(battle,originalRequest,turn)
                    && independentlyLegal.test(action)
                    && candidate.current(battle,originalRequest,turn))
                return action;
        } catch (RuntimeException rejected) {
            // Never propagate experimental failures into a live action.
        }
        return legacy.get();
    }
}
