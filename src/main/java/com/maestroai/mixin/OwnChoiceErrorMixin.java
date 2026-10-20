package com.maestroai.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.battles.interpreter.instructions.ErrorInstruction;
import com.maestroai.ai.MaestroBattleAI;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe only our own queued rejection, after upstream restores mustChoose. */
@Mixin(value = ErrorInstruction.class, remap = false)
public abstract class OwnChoiceErrorMixin {
    @Inject(method = "invoke$lambda$0", at = @At("RETURN"), require = 0)
    private static void maestroRejected(ErrorInstruction instruction, PokemonBattle battle,
                                        CallbackInfoReturnable<Unit> ci) {
        if (instruction.getBattleActor() instanceof AIBattleActor actor
                && actor.getBattleAI() instanceof MaestroBattleAI ai) {
            ai.ownChoiceRejected(battle, actor);
        }
    }
}
