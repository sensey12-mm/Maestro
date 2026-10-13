package com.maestroai.training;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Shadow diagnostic replay with bounded, privacy-safe envelope provenance. */
public final class PublicBattleCapture {
    private static final AtomicLong NEXT_CAPTURE = new AtomicLong();
    public record Observation(List<String> replay, List<String> envelopes) {
        public Observation {
            replay = List.copyOf(replay);
            envelopes = List.copyOf(envelopes);
        }
    }

    private UUID battle;
    private List<String> previous = List.of();
    private long sequence;
    private long captureNumber;
    private boolean incomplete;
    private boolean closed;

    public void begin(UUID battleId) {
        if (battleId == null) throw new IllegalArgumentException("battle ID required");
        battle = battleId;
        previous = List.of();
        sequence = 0;
        captureNumber = NEXT_CAPTURE.incrementAndGet();
        incomplete = false;
        closed = false;
    }

    public long captureNumber() { return captureNumber; }
    public long recordedEvents() { return sequence; }
    public boolean incomplete() { return incomplete; }

    private Observation rejected(String reason) {
        if (incomplete) return new Observation(List.of(), List.of());
        incomplete = true;
        // Fixed classifier only: never echo untrusted source data or private payload.
        return new Observation(List.of(
                "[MaestroAI] SHADOW_REPLAY_GAP battle=" + battle + " after=" + sequence),
                List.of("[MaestroAI] SHADOW_ENVELOPE_FAILURE capture=" + captureNumber
                        + " reason=" + reason + " status=UNKNOWN"));
    }

    /** Compatibility with existing replay users. No tactical side effects. */
    public List<String> observe(UUID battleId, List<String> rawHistory) {
        return observeWithProvenance(battleId, rawHistory).replay();
    }

    public Observation observeWithProvenance(UUID battleId, List<String> rawHistory) {
        if (battleId == null || rawHistory == null) return new Observation(List.of(), List.of());
        if (!battleId.equals(battle)) begin(battleId);
        if (closed || incomplete) return new Observation(List.of(), List.of());

        List<String> filtered = PublicProtocolFilter.publicEvents(rawHistory);
        PublicEnvelopeProvenance.Audit source =
                PublicEnvelopeProvenance.reconcile(rawHistory, filtered);
        if (!source.complete()) return rejected(source.reason());

        int shared = 0;
        while (shared < previous.size() && shared < filtered.size()
                && previous.get(shared).equals(filtered.get(shared))) shared++;
        if (shared < previous.size()) return rejected("NON_APPEND_ONLY_HISTORY");

        List<String> replay = new ArrayList<>();
        List<String> envelopes = new ArrayList<>();
        for (int i = shared; i < filtered.size(); i++) {
            long seq = ++sequence;
            replay.add("[MaestroAI] SHADOW_REPLAY battle=" + battle
                    + " seq=" + seq + " event=" + filtered.get(i));
            PublicEnvelopeProvenance.Origin origin = source.origins().get(i);
            envelopes.add("[MaestroAI] SHADOW_ENVELOPE capture=" + captureNumber
                    + " seq=" + seq + " route=" + origin.route()
                    + " split_target=" + origin.splitTarget()
                    + " spectator=EXPECTED_NOT_OBSERVED"
                    + " branch_attested=" + (origin.route() !=
                         PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN));
        }
        previous = List.copyOf(filtered);
        return new Observation(replay, envelopes);
    }

    /**
     * Only a real Cobblemon BATTLE_VICTORY callback calls this. Does not
     * derive winner identity, a win rate, or replay completeness.
     */
    public List<String> onVictory(UUID battleId, List<String> history) {
        if (battleId == null || !battleId.equals(battle) || closed) return List.of();
        Observation flushed = observeWithProvenance(battleId, history);
        List<String> output = new ArrayList<>();
        output.addAll(flushed.replay());
        output.addAll(flushed.envelopes());
        closed = true;
        output.add("[MaestroAI] SHADOW_TERMINAL capture=" + captureNumber
                + " after=" + sequence
                + " event=COBBLEMON_BATTLE_VICTORY"
                + " winner=REDACTED"
                + " beginning=UNKNOWN"
                + " delivery=UNVERIFIED"
                + " coverage=" + (incomplete ? "INCOMPLETE" : "START_UNKNOWN"));
        return List.copyOf(output);
    }
}
