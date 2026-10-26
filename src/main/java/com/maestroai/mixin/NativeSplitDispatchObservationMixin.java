package com.maestroai.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.maestroai.ai.MaestroBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 6.18: RETURN from native sendSplitUpdate; it witnesses only the
 * completed method call, NOT acknowledgment by Minecraft clients.
 * require=0 allows incompatible native versions to abstain safely.
 */
@Mixin(value=PokemonBattle.class,remap=false)
public abstract class NativeSplitDispatchObservationMixin {
    @Inject(method="sendSplitUpdate",at=@At("RETURN"),require=0)
    private void maestroObserveDispatch(BattleActor privateActor,
            NetworkPacket<?> publicPacket,NetworkPacket<?> privatePacket,
            CallbackInfo ci) {
        try {
            var battle=(PokemonBattle)(Object)this;
            for(var actor:battle.getActors()) {
                if(actor instanceof AIBattleActor aiActor
                        && aiActor.getBattleAI() instanceof MaestroBattleAI ai) {
                    ai.ownNativeSplitDispatchReturned(
                            battle,privateActor,privatePacket,publicPacket);
                }
            }
        }catch(RuntimeException ignored) {
            // No observer error can affect native sending or Legacy moves.
        }
    }
}
