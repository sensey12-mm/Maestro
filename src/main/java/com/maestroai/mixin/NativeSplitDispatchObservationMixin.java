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
    /**
     * Battle-end packet is broadcast by PokemonBattle before registry close.
     * Observe only the native packet CLASS NAME, never its contents.
     * If this optional hook fails on an unsupported runtime the observer
     * retains NO delivery authority and source acceptance remains pending.
     */
    @Inject(method="sendUpdate(Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V",
            at=@At("HEAD"),require=0)
    private void maestroWithdrawOnNativeBattleEnd(NetworkPacket<?> packet,
            CallbackInfo ci) {
        if(packet==null || !packet.getClass().getSimpleName().equals("BattleEndPacket"))
            return;
        try {
            var battle=(PokemonBattle)(Object)this;
            for(var actor:battle.getActors()) {
                if(actor instanceof AIBattleActor aiActor
                        && aiActor.getBattleAI() instanceof MaestroBattleAI ai) {
                    ai.ownNativeSplitObservationBattleEnded(battle);
                }
            }
        }catch(RuntimeException ignored) {
            // Lifecycle observation cannot affect native battle termination.
        }
    }

    /**
     * Phase 6.24: Cobblemon's common switch and HP interpretation dispatch
     * uses sendSidedUpdate, not sendSplitUpdate. This RETURN witness is
     * not a Showdown split-event or recipient delivery certificate.
     */
    @Inject(method="sendSidedUpdate(Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;Lcom/cobblemon/mod/common/api/net/NetworkPacket;Lcom/cobblemon/mod/common/api/net/NetworkPacket;Z)V",
            at=@At("RETURN"),require=0)
    private void maestroObserveSidedDispatch(BattleActor sourceActor,
            NetworkPacket<?> allyPacket,NetworkPacket<?> opponentPacket,
            boolean spectatorsAsAlly,CallbackInfo ci){
        try {
            var battle=(PokemonBattle)(Object)this;
            for(var actor:battle.getActors()){
                if(actor instanceof AIBattleActor aiActor
                        && aiActor.getBattleAI() instanceof MaestroBattleAI ai){
                    ai.ownNativeSidedDispatchReturned(battle,sourceActor,
                            allyPacket,opponentPacket,spectatorsAsAlly);
                }
            }
        }catch(RuntimeException ignored){
            // Never interfere with native packet sending or Legacy control.
        }
    }

    @Inject(method="sendSplitUpdate(Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;Lcom/cobblemon/mod/common/api/net/NetworkPacket;Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V",at=@At("RETURN"),require=0)
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
