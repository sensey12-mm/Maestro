package com.maestroai.training;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.maestroai.ai.RuntimeBattleMode;
import java.util.function.BooleanSupplier;

/** Optional local Fabric host test, never shipped in the mod or invoked as a
 * standalone Minecraft acceptance claim. Uses an actorless metadata fixture;
 * an uninitialized RaidInstance is ONLY a non-null marker for the published
 * source getter. No raid fields, actors, teams or requests are inspected.
 */
public final class FabricModeRegression {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static Object ordinaryFixture() {
        var battle=new PokemonBattle(new BattleFormat(),new BattleSide(),new BattleSide());
        check(RuntimeBattleMode.ordinary(battle),"actual Fabric ordinary metadata was not supported");
        check(!RuntimeBattleMode.ordinary(new Object()),"foreign metadata gained ordinary scope with raid addon loaded");
        return battle;
    }
    public static void negativeAndWithdrawal(Object battle,RuntimeDomain domain)throws Exception {
        check(domain.current(),"mode withdrawal needs a current source domain");
        var loader=battle.getClass().getClassLoader();
        var type=Class.forName("com.necro.raid.dens.common.raids.RaidInstance",false,loader);
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        Object marker=unsafe.allocateInstance(type);
        var setter=battle.getClass().getMethod("crd_setRaidBattle",type);
        setter.invoke(battle,marker);
        check(!RuntimeBattleMode.ordinary(battle),"silent raid metadata became ordinary evidence");
        check(!domain.current(),"raid association retained a source/choice lease");
        setter.invoke(battle,new Object[]{null});
        check(RuntimeBattleMode.ordinary(battle)&&!domain.current(),"mode restoration revived a consumed lease");
        System.out.println("Fabric battle-mode regression PASS: ordinary metadata, foreign/raid denial, no raid-field reads, sticky lease withdrawal");
    }
}
