package com.maestroai;

import com.gitlab.srcmc.rctapi.api.util.JTO;
import com.maestroai.ai.MaestroBattleAI;
import com.maestroai.training.ControlledBattleCaptureRegistry;
import net.fabricmc.api.ModInitializer;

public class MaestroAI implements ModInitializer {

    public static final String MOD_ID = "maestroai";

    @Override
    public void onInitialize() {
        /*
         * Register Maestro's custom battle AI with RCT.
         *
         * Trainer JSON can now use:
         *
         * "ai": {
         *   "type": "maestro"
         * }
         */
        JTO.registerParser(
                "maestro",
                data -> new MaestroBattleAI()
        );

        ControlledBattleCaptureRegistry.install();

        String version = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("UNKNOWN");
        System.out.println("[MaestroAI] Loaded version " + version + ".");
        System.out.println("[MaestroAI] Registered AI type: maestro");
    }
}