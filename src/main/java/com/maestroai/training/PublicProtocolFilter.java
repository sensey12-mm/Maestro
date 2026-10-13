package com.maestroai.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Conservative filter for Showdown protocol batches.
 * Only explicit public battle events pass. Never forward request, player,
 * team preview, private split branch, or other unrecognized messages.
 *
 * The split protocol has a private line followed by the public line;
 * only the second is eligible. This class deliberately fails closed.
 */
public final class PublicProtocolFilter {
    private static final Set<String> ALLOWED = Set.of(
            "turn", "move", "switch", "drag", "replace", "faint",
            "-boost", "-unboost", "-setboost", "-clearboost",
            "-clearallboost", "-copyboost", "-swapboost", "-invertboost",
            "-item", "-enditem", "-ability", "-endability",
            "-status", "-curestatus", "-transform", "-start", "-end", "-activate",
            "-terastallize", "-damage", "-heal", "-sethp",
            "-sidestart", "-sideend", "-swapsideconditions"
    );

    private PublicProtocolFilter() {}

    public static List<String> publicEvents(List<String> rawMessages) {
        if (rawMessages == null) return List.of();
        List<String> result = new ArrayList<>();
        int splitRemaining = 0;
        for (String chunk : rawMessages) {
            if (chunk == null) continue;
            String[] lines = chunk.split("\\R", -1);
            if (lines.length > 0 && lines[0].equals("sideupdate")) continue;
            for (String line : lines) {
                if (line.equals("update")) continue; // framing, not a split payload
                if (line.equals("|split") || line.startsWith("|split|")) {
                    splitRemaining = 2;
                    continue;
                }
                if (splitRemaining > 0) {
                    splitRemaining--;
                    if (splitRemaining == 1) continue; // private side
                    if (allowed(line)) result.add(line);
                    continue;
                }
                if (allowed(line)) result.add(line);
            }
        }
        return List.copyOf(result);
    }

    private static boolean allowed(String line) {
        if (line == null || !line.startsWith("|")) return false;
        int end = line.indexOf('|', 1);
        String kind = end < 0 ? line.substring(1) : line.substring(1, end);
        return ALLOWED.contains(kind);
    }
}
