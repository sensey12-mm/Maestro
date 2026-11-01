package com.maestroai.ai;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;
import com.maestroai.training.ShadowPromotionPreparation;

/**
 * Fail-closed live promotion boundary. A flag alone never authorizes a
 * shadow experiment: a candidate must also be independently promoted and
 * validated against the current owning action. Phase 4/5/6/7 now prepares
 * conditional candidates only; no live promotion is approved.
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
     * Per-trainer, one-way emergency kill switch. Revocation cannot be undone
     * by JVM properties, a stale native receipt, or another candidate.
     * A future explicitly authorized trainer/battle may receive a fresh
     * control; this run never arms live Shadow authority.
     */
    public static final class Control {
        private volatile boolean revoked;
        public boolean current(){return !revoked;}
        public void revoke(){revoked=true;}
    }

    /**
     * Compatibility API for synthetic/out-of-game call sites. Real gameplay
     * supplies a durable per-trainer Control instead.
     */
    public static <T> T choosePrepared(
            Supplier<T> legacy,ShadowPromotionPreparation.Proposal candidate,
            Object battle,Object originalRequest,int turn,
            Function<String,T> materialize,Predicate<T> independentlyLegal,
            boolean ownerApproved) {
        return choosePrepared(legacy,candidate,battle,originalRequest,turn,
                materialize,independentlyLegal,new Control(),ownerApproved);
    }

    /**
     * Two independent operator factors plus current model evidence:
     * a live-enable setting, an EXPLICIT owner-approved source constant
     * (false in production), and a non-revoked per-trainer control.
     *
     * No candidate response survives a disabled gate, lost source, changed
     * original request/turn, failing native legality or materialization
     * exception. Any attempted invalid authorized candidate trips the
     * emergency control and permanently falls back to Legacy until reset.
     */
    public static <T> T choosePrepared(
            Supplier<T> legacy,ShadowPromotionPreparation.Proposal candidate,
            Object battle,Object originalRequest,int turn,
            Function<String,T> materialize,Predicate<T> independentlyLegal,
            Control control,boolean ownerApproved) {
        if(!enabled() || !ownerApproved || control==null || !control.current()
                || candidate==null || materialize==null || independentlyLegal==null)
            return legacy.get();
        try {
            if(!enabled() || !control.current()
                    || !candidate.current(battle,originalRequest,turn)) {
                control.revoke();
                return legacy.get();
            }
            T action=materialize.apply(candidate.action());
            if(action!=null && independentlyLegal.test(action)
                    && enabled() && control.current()
                    && candidate.current(battle,originalRequest,turn))
                return action;
        } catch (RuntimeException invalid) {
            // Broken speculative selection must not escape to Cobblemon.
        }
        control.revoke();
        return legacy.get();
    }
}
