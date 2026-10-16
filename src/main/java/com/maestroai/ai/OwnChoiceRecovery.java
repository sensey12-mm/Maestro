package com.maestroai.ai;

import java.util.UUID;
import java.util.function.Supplier;

/** One rechoice after an owning simulator trapped-switch rejection.
 * No opponent state, guessing of abilities, timers or unbounded retry loop.
 */
public final class OwnChoiceRecovery {
    public static final String TRAPPED_ERROR =
            "|error|[Unavailable choice] Can't switch: The active Pokémon is trapped";
    private Object battle, actor, request;
    private UUID active;
    private int turn = -1;
    private boolean switched, pending, retried;

    public void submitted(Object battle, Object actor, Object request, int turn,
                          UUID active, boolean switched) {
        if (this.battle != battle || this.actor != actor || this.turn != turn) retried = false;
        this.battle = battle; this.actor = actor; this.request = request;
        this.turn = turn; this.active = active; this.switched = switched; pending = false;
    }

    /** Foreign/stale contexts are rejected before the lazy message is read. */
    public void rejected(Object battle, Object actor, Object request, int turn,
                         UUID active, Supplier<String> message) {
        if (matches(battle, actor, turn, active) && request != null && this.request == request
                && switched && !retried && TRAPPED_ERROR.equals(message.get())) pending = true;
    }

    public boolean corrected(Object battle, Object actor, Object request, int turn,
                             UUID active, boolean mustChoose, boolean trapped,
                             boolean ordinarySingleMoveRequest) {
        if (!matches(battle, actor, turn, active) || !pending || retried || request == null
                || request == this.request || !mustChoose || !trapped || !ordinarySingleMoveRequest) return false;
        pending = false; retried = true;
        return true;
    }

    private boolean matches(Object battle, Object actor, int turn, UUID active) {
        return battle != null && actor != null && active != null && this.battle == battle
                && this.actor == actor && this.turn == turn && active.equals(this.active);
    }
}
