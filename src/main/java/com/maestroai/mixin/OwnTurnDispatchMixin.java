package com.maestroai.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.battles.dispatch.DispatchResult;
import com.cobblemon.mod.common.battles.interpreter.instructions.TurnInstruction;
import com.maestroai.ai.MaestroBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TurnInstruction.class, remap = false)
public abstract class OwnTurnDispatchMixin {
    @Inject(method = "invoke$lambda$0", at = @At("HEAD"), require = 0)
    private static void maestroTurn(PokemonBattle battle, int turn, CallbackInfoReturnable<DispatchResult> ci) {
        for (var candidate : battle.getActors())
            if (candidate instanceof AIBattleActor actor && actor.getBattleAI() instanceof MaestroBattleAI ai) {
                try { ai.ownTurnDispatched(battle, actor, turn); }
                catch (RuntimeException unavailable) { ai.ownTransportUnavailable(); }
            }
    }
    @Inject(method = "invoke$lambda$0", at = @At("RETURN"), require = 0)
    private static void maestroTurnFinished(PokemonBattle battle, int turn, CallbackInfoReturnable<DispatchResult> ci) {
        for (var candidate : battle.getActors())
            if (candidate instanceof AIBattleActor actor && actor.getBattleAI() instanceof MaestroBattleAI ai)
                ai.ownTurnFinished(battle, actor, turn);
    }
}
