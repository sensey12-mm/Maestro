package com.maestroai.training;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shadow-only, spectator-protocol provenance. The simulator's |split| sequence
 * contains PRIVATE then PUBLIC, and must be reconstructed over raw chunks before
 * accepting any health or hazard evidence.
 *
 * This class never sees a Cobblemon Pokemon or player request object. HP is
 * a conservative display interval, NOT exact simulator HP. Hazard layers are
 * observed partial evidence, NOT proof of the absence of other hazards.
 */
public final class PublicProtocolEvidence {
    private static final Pattern SLOT = Pattern.compile("^(p[1-4][a-c]):\\s*.+$");
    private static final Pattern SIDE = Pattern.compile("^(p[1-4])(?::.*)?$");
    private static final Pattern HEALTH = Pattern.compile("^([0-9]{1,3})/(48|100)(?:\\s+(?:brn|par|psn|tox|slp|frz|fnt))?$");
    private static final Set<String> HP_EVENTS = Set.of("switch", "drag", "replace", "-damage", "-heal", "-sethp", "faint", "-transform");
    private static final Set<String> HAZARD_EVENTS = Set.of("-sidestart", "-sideend", "-swapsideconditions");

    private List<String> previous = List.of();
    private boolean corrupt;
    private boolean complete;
    private boolean framed;
    private int lastTurn;
    private final Map<String, HpAppearance> active = new HashMap<>();
    private final Map<String, Set<String>> seenActors = new HashMap<>();
    private final Map<String, Map<String, Integer>> hazards = new HashMap<>();
    private List<String> mechanicsEvents = List.of();
    private Set<String> coverageUnknown = Set.of();
    // Non-state annotations emitted by the pinned simulator. Everything else
    // outside the modeled event set blocks a certificate, without retaining its payload.
    private static final Set<String> ANNOTATIONS = Set.of("", "player", "teamsize", "tier", "rule", "seed", "upkeep",
            "-nothing", "-anim", "-mostlyineffective", "-extremelyeffective",
            "-crit", "-supereffective", "-resisted", "-immune", "-miss", "-fail", "-notarget");
    private static final Set<String> MECHANICS_EVENTS = Set.of("switch", "drag", "replace", "detailschange", "-formechange",
            "-transform", "-terastallize", "-start", "-end", "-activate", "-item", "-enditem", "-ability", "-endability",
            "-status", "-curestatus", "-damage", "-heal", "faint", "turn", "start", "gen", "gametype",
            "-weather", "-fieldstart", "-fieldend", "-sidestart", "-sideend", "-swapsideconditions",
            "-singleturn", "-singlemove", "move", "cant", "-sethp", "-boost", "-unboost", "-clearboost",
            "-block", "-prepare", "-mustrecharge", "-zpower", "-mega", "-primal", "-burst", "-dynamax",
            "-clearallboost", "-invertboost", "-swapboost", "-setboost", "-copyboost", "-clearnegativeboost");

    private static final class HpAppearance {
        String actor;
        boolean ambiguous;
        boolean reentryAmbiguous;
        PublicStateSearchAdapter.Interval hp;
        HpAppearance(String actor, boolean ambiguous, PublicStateSearchAdapter.Interval hp) {
            this.actor = actor; this.ambiguous = ambiguous; this.reentryAmbiguous = ambiguous; this.hp = hp;
        }
    }

    public void reset() {
        previous = List.of();
        corrupt = false;
        complete = false;
        framed = false;
        lastTurn = 0;
        active.clear(); seenActors.clear(); hazards.clear();
        mechanicsEvents = List.of();
        coverageUnknown = Set.of();
    }

    /** Current observations are safe only when all supplied raw chunks form an append-only,
     * structurally complete history. Any rewrite or truncation poisons this battle's
     * projection until reset; an unfinished split stays temporarily UNKNOWN.
     */
    public void update(List<String> rawHistory) {
        mechanicsEvents = List.of();
        coverageUnknown = Set.of();
        if (rawHistory == null || rawHistory.stream().anyMatch(Objects::isNull)) {
            corrupt = true;
        } else if (rawHistory.size() < previous.size()
                || !rawHistory.subList(0, previous.size()).equals(previous)) {
            corrupt = true;
        }
        if (corrupt) {
            complete = false;
            active.clear(); hazards.clear();
            return;
        }
        previous = List.copyOf(rawHistory);
        complete = false;
        lastTurn = 0;
        active.clear(); hazards.clear(); seenActors.clear();
        int pending = 0;
        List<String> accepted = new ArrayList<>();
        Set<String> unmodeled = new TreeSet<>();
        boolean allFramed = true;
        for (String chunk : rawHistory) {
            String[] lines = chunk.split("\\R", -1);
            if (lines.length > 0 && lines[0].equals("sideupdate")) {
                if (pending != 0) { corrupt = true; break; }
                continue;
            }
            if (lines.length > 0 && !lines[0].equals("update")) allFramed = false;
            for (String line : lines) {
                if (line.equals("sideupdate") || line.startsWith("sideupdate ")) { corrupt = true; break; }
                if (line.equals("update")) continue; // framing, not a split payload
                if (line.contains("SHADOW_REPLAY_GAP")) {
                    corrupt = true;
                    break;
                }
                if (line.equals("|split") || line.startsWith("|split|")) {
                    // The special split marker has exactly two following lines.
                    if (pending != 0 || !line.matches("\\|split(?:\\|p[1-4])?")) {
                        corrupt = true;
                        break;
                    }
                    pending = 2;
                    if (line.equals("|split")) unmodeled.add("SPLIT_TARGET_PROVENANCE_UNKNOWN");
                    continue;
                }
                if (pending != 0) {
                    if (pending == 2 && !line.startsWith("|")) { corrupt = true; break; }
                    pending--;
                    if (pending == 1) continue; // private line
                    // Public split branch, possibly intentionally blank.
                }
                if (!line.startsWith("|")) continue;
                // Cobblemon update envelopes also contain private PP synchronization
                // for every party member. Drop the WHOLE instruction before any
                // payload split; PpUpdateInstruction never broadcasts public state.
                if (line.startsWith("|pp_update|")) continue;
                // Source-generated clock annotation, unrelated to tactical state.
                if (line.matches("\\|t:\\|[0-9]{1,13}")) continue;
                if (!apply(line)) {
                    corrupt = true;
                    break;
                }
                String[] event = line.split("\\|", 3);
                if (event.length > 1 && MECHANICS_EVENTS.contains(event[1])) accepted.add(line);
                else if (event.length > 1 && !ANNOTATIONS.contains(event[1]))
                    unmodeled.add("UNMODELED_PUBLIC_PROTOCOL_KIND:" +
                            (event[1].matches("-?[a-z]{1,32}") ? event[1] : "INVALID"));
            }
            if (corrupt) break;
        }
        if (corrupt || pending != 0) {
            active.clear(); hazards.clear();
            return;
        }
        complete = true;
        framed = allFramed;
        mechanicsEvents = List.copyOf(accepted);
        coverageUnknown = Set.copyOf(unmodeled);
    }

    /** Reconstructed spectator events only; never raw requests or a private split branch.
     * Incomplete/corrupt histories expose no mechanics evidence.
     */
    public List<String> mechanicsEvents() { return mechanicsEvents; }
    public Set<String> coverageUnknown() { return coverageUnknown; }
    public boolean reentryAmbiguous(String slot) {
        var appearance = active.get(slot); return appearance == null || appearance.reentryAmbiguous;
    }

    /** Null/UNKNOWN if the active public appearance is missing, ambiguous or unmeasured. */
    public PublicTransitionState.Known<PublicStateSearchAdapter.Interval> hp(String slot) {
        if (!complete || slot == null) return PublicTransitionState.Known.unknown();
        HpAppearance appearance = active.get(slot);
        if (appearance == null || appearance.ambiguous || appearance.hp == null)
            return PublicTransitionState.Known.unknown();
        return PublicTransitionState.Known.of(appearance.hp, "spectator-public-hp-interval");
    }

    /** A partial map may contain explicit zero from a public side-end; other absences are UNKNOWN. */
    public PublicTransitionState.Hazards sideHazards(String side) {
        if (!complete || !Set.of("p1", "p2", "p3", "p4").contains(side))
            return PublicTransitionState.Hazards.unknown();
        Map<String, Integer> observed = hazards.getOrDefault(side, Map.of());
        if (observed.isEmpty()) return PublicTransitionState.Hazards.unknown();
        return new PublicTransitionState.Hazards(observed, false, "spectator-public-side-events-partial");
    }

    public boolean complete() { return complete; }
    public boolean framed() { return complete && framed; }
    public boolean corrupted() { return corrupt; }
    /** Public turn is unknown until a fully framed, source-modeled spectator envelope exists. */
    public int publicTurn() {
        return framed() && !corrupt && coverageUnknown.isEmpty() && lastTurn > 0 ? lastTurn : -1;
    }

    private boolean apply(String line) {
        String[] fields = line.split("\\|", -1);
        if (fields.length < 2) return true;
        String kind = fields[1];
        if (kind.equals("turn")) {
            if (fields.length < 3 || !fields[2].matches("[1-9][0-9]*")) return false;
            try {
                int next = Integer.parseInt(fields[2]);
                if (next != lastTurn + 1) return false;
                lastTurn = next;
            } catch (NumberFormatException overflow) {
                return false;
            }
            return true;
        }
        if (!HP_EVENTS.contains(kind) && !HAZARD_EVENTS.contains(kind)) return true;
        if (kind.equals("-swapsideconditions")) {
            // Court Change: without a complete snapshot of both sides, nothing
            // can be safely inferred about either side's remaining hazards.
            hazards.clear();
            return true;
        }
        if (kind.equals("-sidestart") || kind.equals("-sideend")) return sideEvent(fields, kind);
        if (fields.length < 3) return false;
        Matcher m = SLOT.matcher(fields[2]);
        if (!m.matches()) return false;
        String slot = m.group(1);
        HpAppearance bound = active.get(slot);
        if (!Set.of("switch", "drag", "replace").contains(kind) && bound != null
                && !bound.actor.equals(fields[2].trim())) {
            bound.ambiguous = true; bound.hp = null;
            return true;
        }
        switch (kind) {
            case "switch", "drag" -> {
                if (fields.length < 5 || fields[3].isBlank()) return false;
                String actor = fields[2].trim();
                Set<String> observed = seenActors.computeIfAbsent(slot, x -> new HashSet<>());
                boolean returnAmbiguous = !observed.add(actor);
                // Duplicate nickname/species is not a unique party identifier.
                active.put(slot, new HpAppearance(actor, returnAmbiguous, parseHp(fields[4])));
            }
            case "replace" -> {
                // Bundled Illusion.onEnd emits add("replace", pokemon, details),
                // without a health field. Existing optional-health captures also work.
                if (fields.length < 4 || fields[3].isBlank()) return false;
                HpAppearance old = active.get(slot);
                if (old == null) active.put(slot, new HpAppearance(fields[2], true, null));
                else { old.actor = fields[2]; old.hp = null; old.ambiguous = true; }
            }
            case "-transform" -> {
                HpAppearance old = active.get(slot);
                if (old != null) { old.ambiguous = true; old.hp = null; }
            }
            case "-sethp" -> {
                // Multi-target sethp is not a standard switch/damage/heal display.
                HpAppearance old = active.get(slot);
                if (old != null) old.hp = null;
            }
            case "-damage", "-heal" -> {
                if (fields.length < 4) return false;
                HpAppearance old = active.get(slot);
                if (old != null && !old.ambiguous) old.hp = parseHp(fields[3]);
            }
            case "faint" -> {
                HpAppearance old = active.get(slot);
                if (old != null && !old.ambiguous)
                    old.hp = new PublicStateSearchAdapter.Interval(0, 0);
            }
            default -> { }
        }
        return true;
    }

    private boolean sideEvent(String[] fields, String kind) {
        if (fields.length < 4) return false;
        Matcher m = SIDE.matcher(fields[2].trim());
        if (!m.matches()) return false;
        String hazard = hazard(fields[3]);
        if (hazard == null) {
            // Ignore non-hazard side conditions, never infer a hazard from them.
            return true;
        }
        Map<String, Integer> current = hazards.computeIfAbsent(m.group(1), x -> new HashMap<>());
        if (kind.equals("-sideend")) {
            current.put(hazard, 0);
        } else {
            int cap = hazard.equals("spikes") ? 3 : hazard.equals("toxicspikes") ? 2 : 1;
            current.put(hazard, Math.min(cap, current.getOrDefault(hazard, 0) + 1));
        }
        return true;
    }

    private static String hazard(String raw) {
        String text = raw.toLowerCase(Locale.ROOT).trim();
        if (text.startsWith("move:")) text = text.substring(5).trim();
        String key = text.replaceAll("[^a-z0-9]", "");
        return Set.of("spikes", "toxicspikes", "stealthrock", "stickyweb").contains(key) ? key : null;
    }

    private static PublicStateSearchAdapter.Interval parseHp(String value) {
        Matcher m = HEALTH.matcher(value.trim());
        if (!m.matches()) return null;
        int numerator;
        int denominator;
        try {
            numerator = Integer.parseInt(m.group(1));
            denominator = Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            return null;
        }
        if (numerator > denominator) return null;
        // Spectator values are quantized to 100 or 48 display units, not
        // exact HP. Until Cobblemon's bundled Showdown rounding is validated
        // in a real log, widen by one full display tick in either direction.
        return new PublicStateSearchAdapter.Interval(
                Math.max(0.0, (numerator - 1.0) / denominator),
                Math.min(1.0, (numerator + 1.0) / denominator));
    }
}
