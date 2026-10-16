package com.maestroai.mixin;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.maestroai.ai.SimulatorDomainBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Public registry configuration only; never simulator messages or private requests. */
@Mixin(value = GraalShowdownService.class, remap = false)
public abstract class SimulatorDomainMixin {
    @Inject(method = "boot", at = @At("HEAD"), require = 0)
    private void maestroBootStarting(CallbackInfo ci) { SimulatorDomainBridge.bootStarting(this); }
    @Inject(method = "boot", at = @At("RETURN"), require = 0)
    private void maestroBootFinished(CallbackInfo ci) { SimulatorDomainBridge.bootFinished((GraalShowdownService)(Object)this); }
    @Inject(method = {"createContext", "setContext", "setSendBattleMessageFunction", "closeConnection"}, at = @At("HEAD"), require = 0)
    private void maestroDomainClosed(CallbackInfo ci) { SimulatorDomainBridge.invalidate(this); }
    @Inject(method = "sendRegistryData", at = @At("HEAD"), require = 0)
    private void maestroRegistryDataStarting(Map<String,String> data, String type, CallbackInfo ci) {
        SimulatorDomainBridge.registryDataStarting((GraalShowdownService)(Object)this,data,type);
    }
    @ModifyArg(method="sendRegistryData", at=@At(value="INVOKE", target=
            "Lcom/cobblemon/mod/relocations/graalvm/polyglot/Value;execute([Ljava/lang/Object;)Lcom/cobblemon/mod/relocations/graalvm/polyglot/Value;"), index=0, require=0)
    private Object[] maestroNativePayload(Object[] actual) {
        SimulatorDomainBridge.registryDataPayload(this,actual);
        return actual;
    }
    @Inject(method = "sendRegistryData", at = @At("RETURN"), require = 0)
    private void maestroRegistryDataFinished(Map<String,String> data, String type, CallbackInfo ci) {
        SimulatorDomainBridge.registryDataFinished((GraalShowdownService)(Object)this);
    }
    @Inject(method = "resetRegistryData", at = @At("HEAD"), require = 0)
    private void maestroNativeReset(String type, CallbackInfo ci) { SimulatorDomainBridge.resetNative(this,type); }
    @Inject(method = "resetAllRegistries", at = @At("HEAD"), require = 0)
    private void maestroNativeResetAll(CallbackInfo ci) { SimulatorDomainBridge.resetNative(this,null); }
    @Inject(method = {"sendRegistryEntry", "resetRegistryData", "resetAllRegistries", "indicateSpeciesInitialized"}, at = @At("HEAD"), require = 0)
    private void maestroRegistryStarting(CallbackInfo ci) { SimulatorDomainBridge.mutationStarting(this); }
    @Inject(method = {"sendRegistryEntry", "resetRegistryData", "resetAllRegistries", "indicateSpeciesInitialized"}, at = @At("RETURN"), require = 0)
    private void maestroRegistryFinished(CallbackInfo ci) { SimulatorDomainBridge.mutationFinished(this); }
}
