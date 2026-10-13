package com.maestroai.ai;

import java.util.Set;

/**
 * Fail-closed request legality rules shared by the live decision path and
 * dependency-free regression tests. The active request's trapped flag takes
 * precedence over any attractive bench score.
 */
public final class LiveActionLegality {
    private LiveActionLegality() {}

    private static final Set<String> CHARGING_MOVES = Set.of(
            "geomancy", "solarbeam", "solarblade", "skullbash", "razorwind",
            "fly", "dig", "dive", "bounce", "phantomforce", "shadowforce",
            "freezeshock", "iceburn", "meteorbeam", "electroshot");

    public static boolean forcedContinuation(String previousMove, String onlyMove,
                                              int requestMoveCount) {
        return requestMoveCount == 1 && previousMove != null
                && previousMove.equals(onlyMove)
                && CHARGING_MOVES.contains(previousMove);
    }

    public static boolean voluntarySwitchAllowed(boolean forceSwitch,
                                                  boolean requestTrapped,
                                                  boolean forcedContinuation) {
        return forceSwitch || (!requestTrapped && !forcedContinuation);
    }
}
