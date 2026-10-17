package com.maestroai.training;

import com.maestroai.ai.OwnChoiceRecovery;
import com.maestroai.ai.LiveActionLegality;
import java.util.UUID;

/** Synthetic owning-receipt dispatch contracts, not Minecraft acceptance. */
final class OwnChoiceRecoveryRegression {
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("own rechoice contract check " + (checks+1));
        checks++;
    }
    static void run() {
        Object battle = new Object(), actor = new Object(), original = new Object(), corrected = new Object();
        UUID active = new UUID(0, 1);
        OwnChoiceRecovery r = new OwnChoiceRecovery();
        r.submitted(battle, actor, original, 12, active, true);
        // The simulator's fresh owning trapped request is sufficient. No error
        // message/hook, opponent payload, species or ability inference is needed.
        check(!r.corrected(new Object(), actor, corrected, 12, active, true, true, true));
        check(!r.corrected(battle, new Object(), corrected, 12, active, true, true, true));
        check(!r.corrected(battle, actor, original, 12, active, true, true, true));
        check(!r.corrected(battle, actor, null, 12, active, true, true, true));
        check(!r.corrected(battle, actor, corrected, 13, active, true, true, true));
        check(!r.corrected(battle, actor, corrected, 12, new UUID(0,2), true, true, true));
        // A correction arriving before mustChoose is restored must be retried
        // at the error-return callback, without parsing that error's payload.
        check(!r.corrected(battle, actor, corrected, 12, active, false, true, true));
        check(!r.corrected(battle, actor, corrected, 12, active, true, false, true));
        check(!r.corrected(battle, actor, corrected, 12, active, true, true, false));
        check(r.corrected(battle, actor, corrected, 12, active, true, true, true));
        check(!r.corrected(battle, actor, corrected, 12, active, true, true, true));
        check(!r.corrected(battle, actor, new Object(), 12, active, true, true, true));
        r.submitted(battle, actor, corrected, 12, active, true);
        check(!r.corrected(battle, actor, new Object(), 12, active, true, true, true));
        // Moves and ineligible original requests cannot arm switch recovery.
        r.submitted(battle, actor, original, 13, active, false);
        check(!r.corrected(battle, actor, corrected, 13, active, true, true, true));
        r.submitted(battle, actor, original, 14, active, true);
        check(r.corrected(battle, actor, corrected, 14, active, true, true, true));
        check(!LiveActionLegality.voluntarySwitchAllowed(false, true, false));
        check(LiveActionLegality.voluntarySwitchAllowed(true, true, false));
        // Even an ineligible intermediate submission cannot reset the budget.
        r.submitted(battle, actor, new Object(), 14, active, false);
        r.submitted(battle, actor, new Object(), 14, active, true);
        check(!r.corrected(battle, actor, new Object(), 14, active, true, true, true));
        System.out.println("Own rechoice synthetic regressions PASS: " + checks + " checks");
    }
}
