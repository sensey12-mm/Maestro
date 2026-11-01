package com.maestroai.ai;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;
import com.maestroai.training.ShadowPromotionPreparation;

/**
 * Fail-closed live promotion boundary. A flag alone never authorizes a
 * shadow experiment: a candidate must also be independently promoted and
 * validated against the current owning action. Owner-approved Shadow may lead when current independent source and native legality prove a candidate. Legacy remains the fail-safe.
 */
public final class LivePromotionGate {
    public static final String PROPERTY = "maestroai.livePromotion.enabled";

    private LivePromotionGate() {}

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "true"));
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

    public enum Authority { SHADOW, LEGACY }
    public record Selection<T>(T action,Authority authority) {
        public Selection {
            if(authority==null)throw new IllegalArgumentException("Missing authority");
        }
    }
    /** A one-way emergency latch reset only by a NEW owning battle ID. */
    public static final class BattleControls {
        private java.util.UUID battleId;
        private Control control=new Control();
        public synchronized Control forBattle(java.util.UUID id) {
            if(id==null){control.revoke();return control;}
            if(!id.equals(battleId)){battleId=id;control=new Control();}
            return control;
        }
        public synchronized void revoke(){control.revoke();}
        public synchronized void battleEnded(java.util.UUID id){
            if(id!=null && id.equals(battleId))control.revoke();
        }
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
        return choosePreparedWithSource(legacy,candidate,battle,originalRequest,turn,
                materialize,independentlyLegal,control,ownerApproved).action();
    }

    /** Prevents Shadow return values being recorded as Legacy in Phase 7. */
    public static <T> Selection<T> choosePreparedWithSource(
            Supplier<T> legacy,ShadowPromotionPreparation.Proposal candidate,
            Object battle,Object originalRequest,int turn,
            Function<String,T> materialize,Predicate<T> independentlyLegal,
            Control control,boolean ownerApproved) {
        if(!ownerApproved || control==null || !control.current())
            return new Selection<>(legacy.get(),Authority.LEGACY);
        if(!enabled()){
            control.revoke(); // Emergency operator disable sticks for this battle.
            return new Selection<>(legacy.get(),Authority.LEGACY);
        }
        if(candidate==null || materialize==null || independentlyLegal==null)
            return new Selection<>(legacy.get(),Authority.LEGACY);
        try{
            if(!control.current() || !candidate.current(battle,originalRequest,turn)){
                control.revoke();
                return new Selection<>(legacy.get(),Authority.LEGACY);
            }
            T action=materialize.apply(candidate.action());
            if(action!=null && independentlyLegal.test(action)
                    && enabled() && control.current()
                    && candidate.current(battle,originalRequest,turn))
                return new Selection<>(action,Authority.SHADOW);
        }catch(RuntimeException invalid){
            // Stale, invalid or broken speculative response cannot escape.
        }
        control.revoke();
        return new Selection<>(legacy.get(),Authority.LEGACY);
    }
}
