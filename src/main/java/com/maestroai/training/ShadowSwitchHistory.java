package com.maestroai.training;

import java.util.*;

/** Public-event-only switch sequence history. Never issues battle actions. */
public final class ShadowSwitchHistory {
    public record SwitchEvent(int turn, String slot, String appearance, String opponentAppearance,
                              boolean forced, String source) {}
    public record LoopEvidence(int firstTurn, int lastTurn, String firstAppearance,
                               String intermediateAppearance, String opponentAppearance) {}
    private final List<SwitchEvent> events = new ArrayList<>();
    public void reset() { events.clear(); }
    public List<SwitchEvent> events() { return List.copyOf(events); }

    public void observe(int turn, String publicLine, String opponentAppearance) {
        if (turn < 0 || publicLine == null) throw new IllegalArgumentException("invalid event");
        String[] p = publicLine.split("\\|", -1);
        if (p.length < 4 || !(p[1].equals("switch") || p[1].equals("drag"))) return;
        String actor = p[2];
        int colon = actor.indexOf(':');
        if (colon < 0) return;
        String slot = actor.substring(0, colon).trim();
        if (!slot.matches("p[1-4][a-c]")) return;
        // 'drag' is explicitly forced; 'switch' alone cannot establish intent.
        events.add(new SwitchEvent(turn, slot, actor.trim(), opponentAppearance,
                p[1].equals("drag"), p[1]));
    }

    /** Candidate loop only: display identities cannot prove unique party members. */
    public List<LoopEvidence> candidateLoops() {
        List<LoopEvidence> loops = new ArrayList<>();
        for (int i = 2; i < events.size(); i++) {
            SwitchEvent a = events.get(i-2), b = events.get(i-1), c = events.get(i);
            if (a.slot().equals(b.slot()) && b.slot().equals(c.slot())
                    && a.appearance().equals(c.appearance())
                    && !a.appearance().equals(b.appearance())
                    && a.opponentAppearance() != null
                    && a.opponentAppearance().equals(b.opponentAppearance())
                    && b.opponentAppearance().equals(c.opponentAppearance())
                    && !a.forced() && !b.forced() && !c.forced())
                loops.add(new LoopEvidence(a.turn(), c.turn(), a.appearance(),
                        b.appearance(), a.opponentAppearance()));
        }
        return List.copyOf(loops);
    }
}
