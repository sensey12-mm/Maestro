package com.maestroai.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.interpreter.instructions.RequestInstruction;
import com.maestroai.ai.MaestroBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import kotlin.Unit;

/** Exact 7553235 queued closure: ownership gate precedes even reading rawMessage.
 * Missing injection on another version withholds evidence; it must not alter legacy.
 */
@Mixin(value = RequestInstruction.class, remap = false)
public abstract class OwnRequestDispatchMixin {
    @Inject(method = "invoke$lambda$0", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;setRequest(Lcom/cobblemon/mod/common/battles/ShowdownActionRequest;)V", shift = At.Shift.AFTER), require = 0)
    private static void maestroOwnInstalled(RequestInstruction instruction, ShowdownActionRequest request,
                                           PokemonBattle battle, CallbackInfoReturnable<Unit> ci) {
        if (instruction.getBattleActor() instanceof AIBattleActor actor && actor.getBattleAI() instanceof MaestroBattleAI ai) {
            try { ai.ownRequestDispatched(battle, actor, request, () -> instruction.getMessage().getRawMessage()); }
            catch (RuntimeException unavailable) { ai.ownTransportUnavailable(); }
        }
    }
}
