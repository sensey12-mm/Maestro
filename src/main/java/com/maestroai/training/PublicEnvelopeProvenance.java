package com.maestroai.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Independent, conservative envelope parser for the Cobblemon 1.7.3 Showdown
 * history kept by PokemonBattle. It records routing metadata only: never log
 * the private half of a split, player requests, raw lines, UUIDs or nicknames.
 *
 * The public branch is the SECOND line after |split|p1 or |split|p2. This
 * describes expected spectator routing, NOT an observed network delivery.
 */
public final class PublicEnvelopeProvenance {
    public enum Route { DIRECT_UPDATE, SPLIT_PUBLIC, UNFRAMED_UNKNOWN }
    public record Origin(Route route, String splitTarget) {}
    public record Audit(boolean complete, String reason, List<Origin> origins) {
        public Audit {
            origins = List.copyOf(origins);
        }
    }

    private static final Set<String> ALLOWED = Set.of(
            "turn", "move", "switch", "drag", "replace", "detailschange",
            "-formechange", "-mega", "faint",
            "-boost", "-unboost", "-setboost", "-clearboost", "-clearallboost",
            "-copyboost", "-swapboost", "-invertboost", "-item", "-enditem",
            "-ability", "-endability", "-status", "-curestatus", "-transform",
            "-start", "-end", "-activate", "-terastallize", "-damage", "-heal",
            "-sethp", "-sidestart", "-sideend", "-swapsideconditions"
    );

    private PublicEnvelopeProvenance() {}

    private static boolean allowed(String line) {
        if (!line.startsWith("|")) return false;
        int end = line.indexOf('|', 1);
        String kind = end < 0 ? line.substring(1) : line.substring(1, end);
        return ALLOWED.contains(kind);
    }

    /**
     * Fail closed whenever the independently reconstructed public stream
     * disagrees with PublicProtocolFilter, a split is incomplete, or a
     * private sideupdate interrupts an active split.
     */
    public static Audit reconcile(List<String> rawHistory, List<String> filtered) {
        if (rawHistory == null || filtered == null)
            return new Audit(false, "MISSING_HISTORY", List.of());
        List<String> observed = new ArrayList<>();
        List<Origin> sources = new ArrayList<>();
        int pending = 0;
        String target = "NONE";
        for (String chunk : rawHistory) {
            if (chunk == null) return new Audit(false, "MALFORMED_CHUNK", List.of());
            String[] lines = chunk.split("\\R", -1);
            boolean framed = lines.length > 0 && "update".equals(lines[0]);
            if (lines.length > 0 && "sideupdate".equals(lines[0])) {
                if (pending != 0) return new Audit(false, "SPLIT_INTERRUPTED", List.of());
                // No sideupdate payload, even if it looks like an allowed event.
                continue;
            }
            for (String line : lines) {
                if ("sideupdate".equals(line) || line.startsWith("sideupdate "))
                    return new Audit(false, "NESTED_SIDEUPDATE", List.of());
                if ("update".equals(line) || line.isEmpty()) continue;
                if (line.equals("|split") || line.startsWith("|split|")) {
                    if (pending != 0) return new Audit(false, "NESTED_SPLIT", List.of());
                    if (!line.equals("|split|p1") && !line.equals("|split|p2"))
                        return new Audit(false, "BAD_SPLIT_TARGET", List.of());
                    target = line.substring("|split|".length());
                    pending = 2;
                    continue;
                }
                if (pending > 0) {
                    if (!line.startsWith("|"))
                        return new Audit(false, "MALFORMED_SPLIT_PAYLOAD", List.of());
                    pending--;
                    if (pending == 0) {
                        if (allowed(line)) {
                            observed.add(line);
                            sources.add(new Origin(Route.SPLIT_PUBLIC, target));
                        }
                        target = "NONE";
                    }
                    // The first split payload (private) is discarded entirely.
                    continue;
                }
                if (allowed(line)) {
                    observed.add(line);
                    sources.add(new Origin(framed ? Route.DIRECT_UPDATE
                                                  : Route.UNFRAMED_UNKNOWN, "NONE"));
                }
            }
        }
        if (pending != 0) return new Audit(false, "INCOMPLETE_SPLIT", List.of());
        if (!observed.equals(filtered))
            return new Audit(false, "PUBLIC_STREAM_MISMATCH", List.of());
        return new Audit(true, "MATCHED_FILTER", sources);
    }
}
