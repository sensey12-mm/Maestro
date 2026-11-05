package com.maestroai.training;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Opt-in metadata-only diagnostics for controlled singleplayer acceptance.
 * Run Minecraft with -Dmaestroai.envelopeCapture=true to activate.
 * Only battles actually observed by MaestroAI are tracked (bounded registry).
 */
public final class ControlledBattleCaptureRegistry {
    private static final boolean ENVELOPE_ENABLED = Boolean.getBoolean("maestroai.envelopeCapture");
    private static final int LIMIT = 128;
    private static final Map<UUID, PublicBattleCapture> TRACKED =
            new LinkedHashMap<>();

    private ControlledBattleCaptureRegistry() {}

    public static boolean enabled() { return ENVELOPE_ENABLED || AutomaticBattleReports.enabled(); }
    public static boolean envelopeEnabled() { return ENVELOPE_ENABLED; }

    public static void install() {
        if (!enabled()) return;
        System.out.println(InstalledArtifactReceipt.capture());
        // Consumer overload is explicit; no Kotlin implementation classes leak.
        CobblemonEvents.BATTLE_VICTORY.subscribe(
                (Consumer<BattleVictoryEvent>) event -> onVictory(event.getBattle()));
        if (ENVELOPE_ENABLED)
            System.out.println("[MaestroAI] Controlled envelope capture enabled"
                    + " privatePayloads=EXCLUDED spectatorDelivery=UNVERIFIED");
    }

    public static void observe(PokemonBattle battle, PublicBattleCapture capture) {
        if (!enabled() || battle == null || battle.getBattleId() == null || capture == null) return;
        synchronized (TRACKED) {
            TRACKED.put(battle.getBattleId(), capture);
            if (TRACKED.size() > LIMIT) {
                UUID evicted = TRACKED.keySet().iterator().next();
                PublicBattleCapture dropped = TRACKED.remove(evicted);
                if (dropped != null)
                    AutomaticBattleReports.interrupt(dropped.captureNumber(), "CAPACITY");
                // No private details, and eviction never fabricates a terminal.
                System.out.println("[MaestroAI] SHADOW_ENVELOPE_REGISTRY reason=CAPACITY"
                        + " captureContinuity=UNKNOWN");
            }
        }
    }

    private static void onVictory(PokemonBattle battle) {
        if (battle == null || battle.getBattleId() == null) return;
        PublicBattleCapture capture;
        synchronized (TRACKED) { capture = TRACKED.remove(battle.getBattleId()); }
        if (capture == null) return;
        // This is a real upstream terminal callback, not an inferred final faint.
        List<String> lines = capture.onVictory(battle.getBattleId(), battle.getShowdownMessages());
        for (String line : lines) System.out.println(line);
        if (!lines.isEmpty()) {
            AutomaticBattleReports.recordLines(capture.captureNumber(),
                    lines.subList(0, lines.size() - 1));
            AutomaticBattleReports.recordTerminal(capture.captureNumber(), lines);
        }
    }
}
