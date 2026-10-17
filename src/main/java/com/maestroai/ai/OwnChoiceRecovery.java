package com.maestroai.ai;

import java.util.UUID;

/** One rechoice after a fresh owning request certifies a submitted switch illegal.
 * The fresh trapped flag is authoritative for our actor; error text delivery is
 * not a prerequisite. No opponent state, ability inference or unbounded retry.
 */
public final class OwnChoiceRecovery {
    private Object battle, actor, request;
    private UUID active;
    private int turn = -1;
    private boolean switched, retried;

    public void submitted(Object battle, Object actor, Object request, int turn,
                          UUID active, boolean switched) {
        if (this.battle != battle || this.actor != actor || this.turn != turn) retried = false;
        this.battle = battle; this.actor = actor; this.request = request;
        this.turn = turn; this.active = active; this.switched = switched;
    }

    public boolean corrected(Object battle, Object actor, Object request, int turn,
                             UUID active, boolean mustChoose, boolean trapped,
                             boolean ordinarySingleMoveRequest) {
        if (!matches(battle, actor, turn, active) || !switched || retried || request == null
                || request == this.request || !mustChoose || !trapped || !ordinarySingleMoveRequest) return false;
        retried = true;
        return true;
    }

    private boolean matches(Object battle, Object actor, int turn, UUID active) {
        return turn > 0 && battle != null && actor != null && active != null && this.battle == battle
                && this.actor == actor && this.turn == turn && active.equals(this.active);
    }
}
