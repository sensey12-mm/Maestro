package com.maestroai.training;

import java.util.*;

/** Offline descriptive use/switch counts from a specifically permitted public spectator replay.
 * Never a probability, an opponent set inference or a live battle consumer. */
public final class PublicCompetitiveTendencies {
    private PublicCompetitiveTendencies() {}

    public enum Interpretation { OBSERVATION_ONLY_NOT_FORECAST }
    public enum EntryKind { SWITCH_CAUSE_UNKNOWN, DRAG_PUBLICLY_FORCED }

    public record MoveUse(CompetitiveFormatNormalizer.Token token, int publicUses) {
        public MoveUse {
            Objects.requireNonNull(token);
            if (publicUses < 1) throw invalid();
        }
    }
    public record Appearance(int index, String slot, String displayedSpecies, EntryKind entry,
                             List<MoveUse> nativeUses, boolean transformed,
                             boolean identityLinked, boolean identityUncertain) {
        public Appearance {
            nativeUses = List.copyOf(nativeUses);
            if (index < 1 || !Set.of("p1a", "p2a").contains(slot)
                    || !displayedSpecies.matches("[a-z0-9]{1,40}")
                    || entry == null || identityLinked || !identityUncertain) throw invalid();
        }
    }
    /** Switch announcements can include replacements/pivots; cause is not inferred. */
    public record Side(String slot, int switchAnnouncements, int forcedDragAnnouncements,
                       int repeatedDisplayCandidates, boolean voluntarySwitchesKnown) {
        public Side {
            if (!Set.of("p1a","p2a").contains(slot) || switchAnnouncements < 0
                    || forcedDragAnnouncements < 0 || repeatedDisplayCandidates < 0
                    || voluntarySwitchesKnown) throw invalid();
        }
    }
    public record Report(CompetitiveFormatNormalizer.Provenance provenance,
                         CompetitiveFormatNormalizer.Rules rules, String targetSourceScope,
                         List<Appearance> appearances, Map<String,Side> sides,
                         Interpretation interpretation, boolean forecastProduced) {
        public Report {
            appearances = List.copyOf(appearances);
            sides = Collections.unmodifiableMap(new TreeMap<>(sides));
            if (provenance == null || rules == null || targetSourceScope == null
                    || interpretation != Interpretation.OBSERVATION_ONLY_NOT_FORECAST
                    || forecastProduced) throw invalid();
        }
    }

    /** Raw data is short-lived. Both prior ingestion and normalization gates must pass. */
    public static Report analyze(PublicReplayCorpus.Source source, String raw,
                                 CompetitiveFormatNormalizer.Target target) {
        PublicReplayCorpus.Evidence accepted = PublicReplayCorpus.ingest(source, raw);
        CompetitiveFormatNormalizer.Result normalized =
                CompetitiveFormatNormalizer.normalize(accepted, target);
        PublicOpponentIntelligence intel = new PublicOpponentIntelligence();
        List<EntryKind> kinds = new ArrayList<>();
        List<String> slots = new ArrayList<>();
        for (String line : PublicProtocolFilter.publicEvents(List.of(raw))) {
            String[] p = line.split("\\|", -1);
            String kind = p.length > 1 ? p[1] : "";
            if ((kind.equals("switch") || kind.equals("drag")) && p.length > 2) {
                String slot = publicSlot(p[2]);
                if (slot != null) {
                    slots.add(slot);
                    kinds.add(kind.equals("drag") ? EntryKind.DRAG_PUBLICLY_FORCED
                            : EntryKind.SWITCH_CAUSE_UNKNOWN);
                }
            }
            intel.accept(line);
        }
        List<Map<String,Integer>> counts = intel.observedNativeMoveUseCounts();
        List<CompetitiveFormatNormalizer.Appearance> observations = normalized.appearances();
        if (observations.size() != counts.size() || counts.size() != kinds.size()) throw invalid();
        List<Appearance> rows = new ArrayList<>();
        Map<String,Integer> switched = new HashMap<>();
        Map<String,Integer> dragged = new HashMap<>();
        Map<String,Integer> repeats = new HashMap<>();
        Map<String,Set<String>> displayed = new HashMap<>();
        for (int i=0; i<observations.size(); i++) {
            var a = observations.get(i);
            if (!a.slot().equals(slots.get(i)) || a.identityLinked() || !a.identityUncertain()
                    || !a.observedMoves().keySet().equals(counts.get(i).keySet()))
                throw invalid();
            List<MoveUse> moves = new ArrayList<>();
            // Descriptive order by count; not a prediction or action recommendation.
            counts.get(i).entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue)
                            .reversed().thenComparing(Map.Entry::getKey))
                    .forEach(e -> moves.add(new MoveUse(a.observedMoves().get(e.getKey()),e.getValue())));
            EntryKind entry = kinds.get(i);
            (entry == EntryKind.DRAG_PUBLICLY_FORCED ? dragged : switched)
                    .merge(a.slot(),1,Integer::sum);
            // Repeated display is not proof of the same Pokémon/party member.
            String shown = a.displayed().id();
            if (!shown.equals("unknown")
                    && !displayed.computeIfAbsent(a.slot(),k->new HashSet<>()).add(shown))
                repeats.merge(a.slot(),1,Integer::sum);
            rows.add(new Appearance(a.index(),a.slot(),shown,entry,moves,a.transformed(),
                    false,true));
        }
        Map<String,Side> sides = new TreeMap<>();
        for (String s : List.of("p1a","p2a"))
            sides.put(s,new Side(s,switched.getOrDefault(s,0),dragged.getOrDefault(s,0),
                    repeats.getOrDefault(s,0),false));
        return new Report(normalized.provenance(),normalized.rules(),
                normalized.targetSourceScope(),rows,sides,
                Interpretation.OBSERVATION_ONLY_NOT_FORECAST,false);
    }
    private static String publicSlot(String actor) {
        int colon = actor.indexOf(':');
        if (colon < 0) return null;
        String slot = actor.substring(0,colon).trim();
        return Set.of("p1a","p2a").contains(slot) ? slot : null;
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Untrusted public replay tendency evidence");
    }
}
