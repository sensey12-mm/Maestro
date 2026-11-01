package com.maestroai.training;

import java.util.Locale;

/**
 * Conservative independent choice of an ACTIVATION from the owning current
 * ShowdownMoveset. The caller authenticates the native request/actor/turn and
 * option-slot identity, and validates the resulting native response. Neither
 * Legacy's selected gimmick nor the opponent's private team is an input.
 */
public final class ShadowNativeGimmickPolicy {
    private ShadowNativeGimmickPolicy() {}

    public record Options(boolean mega,boolean ultra,boolean z,boolean max,
                          boolean tera,boolean activeMax,
                          boolean selectedZUsable,boolean selectedMaxUsable,
                          String ownTeraType) {}

    /**
     * This is an activation policy, NOT a prediction of the opponent's action.
     * Mega/Ultra are lasting improvements. The other once-per-battle options
     * need an independently verified damaging selected move before spending.
     */
    public static String choose(Options options,String selectedMoveType,
                                int selectedMovePower,double ownHp) {
        if(options==null || options.activeMax()
                || !Double.isFinite(ownHp) || ownHp<=0 || ownHp>1)return null;
        if(options.mega())return "mega";
        if(options.ultra())return "ultra";
        if(selectedMovePower<=0 || selectedMoveType==null
                || selectedMoveType.isBlank())return null;
        if(options.z() && options.selectedZUsable()
                && selectedMovePower>=65)return "zmove";
        if(options.tera() && options.ownTeraType()!=null
                && options.ownTeraType().toLowerCase(Locale.ROOT)
                    .equals(selectedMoveType.toLowerCase(Locale.ROOT))
                && selectedMovePower>=70 && ownHp>=0.35)return "terastal";
        if(options.max() && options.selectedMaxUsable()
                && selectedMovePower>=80 && ownHp>=0.35)return "max";
        return null;
    }
}
