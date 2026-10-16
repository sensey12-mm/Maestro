package com.maestroai.training;

import com.maestroai.ai.OwnChoiceRecovery;
import com.maestroai.ai.LiveActionLegality;
import java.util.UUID;

/** Synthetic dispatch sequences; no claim that a Minecraft Mixin executed. */
final class OwnChoiceRecoveryRegression {
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("own rechoice contract");
        checks++;
    }
    static void run() {
        Object battle = new Object(), actor = new Object(), original = new Object(), corrected = new Object();
        UUID active = new UUID(0, 1);
        OwnChoiceRecovery r = new OwnChoiceRecovery();
        r.submitted(battle, actor, original, 13, active, true);
        // The foreign message supplier must never be read.
        r.rejected(battle, new Object(), original, 13, active, () -> { throw new AssertionError("foreign read"); });
        check(!r.corrected(battle, actor, corrected, 13, active, true, true, true));
        r.rejected(battle, actor, original, 13, active, () -> "|error|unrelated");
        check(!r.corrected(battle, actor, corrected, 13, active, true, true, true));
        r.rejected(battle, actor, original, 13, active, () -> OwnChoiceRecovery.TRAPPED_ERROR);
        check(!r.corrected(battle, actor, original, 13, active, true, true, true));
        check(!r.corrected(battle, actor, corrected, 14, active, true, true, true));
        check(!r.corrected(battle, actor, corrected, 13, new UUID(0,2), true, true, true));
        check(!r.corrected(battle, actor, corrected, 13, active, false, true, true));
        check(!r.corrected(battle, actor, corrected, 13, active, true, false, true));
        check(!r.corrected(battle, actor, corrected, 13, active, true, true, false));
        check(r.corrected(battle, actor, corrected, 13, active, true, true, true));
        check(!LiveActionLegality.voluntarySwitchAllowed(false, true, false));
        check(!r.corrected(battle, actor, new Object(), 13, active, true, true, true));
        // Even another rejected switch cannot create an unbounded same-turn loop.
        r.submitted(battle, actor, corrected, 13, active, true);
        r.rejected(battle, actor, corrected, 13, active, () -> OwnChoiceRecovery.TRAPPED_ERROR);
        check(!r.corrected(battle, actor, new Object(), 13, active, true, true, true));
        // A later turn is independent, but a move rejection never gets switch recovery.
        r.submitted(battle, actor, original, 14, active, false);
        r.rejected(battle, actor, original, 14, active, () -> OwnChoiceRecovery.TRAPPED_ERROR);
        check(!r.corrected(battle, actor, corrected, 14, active, true, true, true));
        r.submitted(battle, actor, original, 15, active, true);
        r.rejected(battle, actor, original, 15, active, () -> OwnChoiceRecovery.TRAPPED_ERROR);
        check(r.corrected(battle, actor, corrected, 15, active, true, true, true));
        check(LiveActionLegality.voluntarySwitchAllowed(true, true, false));
        System.out.println("Own rechoice synthetic regressions PASS: " + checks + " checks");
    }
}
