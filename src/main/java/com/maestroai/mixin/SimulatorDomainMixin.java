package com.maestroai.mixin;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.maestroai.ai.SimulatorDomainBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No simulator messages or registry payload arguments are accepted here. */
@Mixin(value = GraalShowdownService.class, remap = false)
public abstract class SimulatorDomainMixin {
    @Inject(method = "boot", at = @At("HEAD"), require = 0)
    private void maestroBootStarting(CallbackInfo ci) { SimulatorDomainBridge.bootStarting(this); }
    @Inject(method = "boot", at = @At("RETURN"), require = 0)
    private void maestroBootFinished(CallbackInfo ci) { SimulatorDomainBridge.bootFinished((GraalShowdownService)(Object)this); }
    @Inject(method = {"createContext", "setContext", "setSendBattleMessageFunction", "closeConnection"}, at = @At("HEAD"), require = 0)
    private void maestroDomainClosed(CallbackInfo ci) { SimulatorDomainBridge.invalidate(this); }
    @Inject(method = {"sendRegistryData", "sendRegistryEntry", "resetRegistryData", "resetAllRegistries", "indicateSpeciesInitialized"}, at = @At("HEAD"), require = 0)
    private void maestroRegistryStarting(CallbackInfo ci) { SimulatorDomainBridge.mutationStarting(this); }
    @Inject(method = {"sendRegistryData", "sendRegistryEntry", "resetRegistryData", "resetAllRegistries", "indicateSpeciesInitialized"}, at = @At("RETURN"), require = 0)
    private void maestroRegistryFinished(CallbackInfo ci) { SimulatorDomainBridge.mutationFinished(this); }
}
