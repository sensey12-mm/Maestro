package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Rebuilt only from the validated spectator stream. This is a limited public
 * field certificate, NOT a certificate that a modpack has no unannounced effects.
 * Unknown effects poison coverage; their later disappearance cannot repair it.
 */
public final class PublicFieldEvidence {
    public static final String SOURCE = CobblemonMechanics.SOURCE + ":field-execution-v1";
    public record Active(String slot, int entry, String name, Set<String> volatiles, boolean covered) {
        public Active { volatiles = Set.copyOf(volatiles); }
    }
    public static final class Capture {
        private final PublicFieldEvidence owner;
        private final long epoch;
        private final int turn;
        private final Known<String> weather, terrain;
        private final Known<Boolean> trickRoom, magicRoom;
        private final Map<String, Boolean> tailwind;
        private final Map<String, Active> actors;
        private final Set<String> unknown;
        private final PublicMechanicsEvidence evidence;
        private final Map<String, PublicMechanicsEvidence.Observation> observations;
        private final PublicProtocolEvidence protocol;
        private final List<String> events;
        private Capture(PublicFieldEvidence owner, long epoch, int turn, Known<String> weather,
                        Known<String> terrain, Known<Boolean> trickRoom, Known<Boolean> magicRoom,
                        Map<String, Boolean> tailwind, Map<String, Active> actors, Set<String> unknown,
                        PublicMechanicsEvidence evidence, PublicProtocolEvidence protocol) {
            this.owner = owner; this.epoch = epoch; this.turn = turn; this.weather = weather; this.terrain = terrain;
            this.trickRoom = trickRoom; this.magicRoom = magicRoom; this.tailwind = Map.copyOf(tailwind);
            this.actors = Map.copyOf(actors); this.unknown = Set.copyOf(unknown);
            this.evidence = evidence;
            this.protocol = protocol; this.events = protocol.mechanicsEvents();
            var captured = new HashMap<String, PublicMechanicsEvidence.Observation>();
            actors.keySet().forEach(slot -> { var o = evidence.observation(slot); if (o != null) captured.put(slot, o); });
            this.observations = Map.copyOf(captured);
        }
        public int turn() { return turn; }
        public Known<String> weather() { return weather; }
        public Known<String> terrain() { return terrain; }
        public Known<Boolean> trickRoom() { return trickRoom; }
        public Known<Boolean> magicRoom() { return magicRoom; }
        public Set<String> unknown() { return unknown; }
        public boolean allActorsCovered() {
            return actors.size() == 2 && actors.values().stream().allMatch(a -> a.covered())
                    && observations.values().stream().allMatch(o -> !o.ambiguous() && !o.unsupportedEffects());
        }
        public String token() { return SOURCE + ":epoch-" + epoch + ":turn-" + turn; }
        public void requireCurrent(int expectedTurn) {
            if (owner.current != this || owner.epoch != epoch || expectedTurn != turn)
                throw new IllegalArgumentException("stale/foreign-turn field certificate");
            if (!protocol.complete() || protocol.corrupted() || protocol.mechanicsEvents() != events)
                throw new IllegalArgumentException("protocol evidence changed since field capture");
            for (var e : observations.entrySet()) if (evidence.observation(e.getKey()) != e.getValue())
                throw new IllegalArgumentException("actor evidence changed since field capture");
        }
        public Active actor(PublicMechanicsEvidence.Observation observation) {
            if (observation == null) return null;
            if (observations.get(observation.slot()) != observation || evidence.observation(observation.slot()) != observation) return null;
            Active a = actors.get(observation.slot());
            if (a == null || a.entry() != observation.entry() || !a.name().equals(observation.publicName())) return null;
            return a;
        }
        public Known<Boolean> tailwind(String slot) {
            Boolean value = slot != null && slot.length() >= 2 ? tailwind.get(slot.substring(0, 2)) : null;
            return value == null ? Known.unknown() : Known.of(value, token());
        }
        /** A witnessed ordinary switch initializes stages in the pinned engine.
         * This requires current source AND full public effect/lifecycle coverage;
         * an empty observer map alone is never evidence of zero stages.
         */
        public Map<String,Integer> stages(PublicMechanicsEvidence.Observation observation,
                                          Known<Boolean> effectiveDomain) {
            requireCurrent(turn);
            Active active = actor(observation);
            if (!effectiveDomain.known() || !Boolean.TRUE.equals(effectiveDomain.value())
                    || active == null || !active.covered() || !allActorsCovered()
                    || !unknown.isEmpty() || observation.ambiguous() || observation.unsupportedEffects()) return Map.of();
            var observer = new PublicBattleObserver(); events.forEach(observer::accept);
            String identity = observer.activeIdentity(observation.slot());
            if (identity == null || observer.stagesUncertain(identity)) return Map.of();
            var result = new TreeMap<String,Integer>();
            STATS.forEach(stat -> result.put(stat, 0)); result.putAll(observer.knownStages(identity));
            return Map.copyOf(result);
        }
        /** Only a witnessed initialized field plus a current engine receipt
         * closes the partial protocol hazard map. Court Change, gaps and any
         * unsupported field operation prevent this certificate.
         */
        public Hazards hazards(String side, Known<Boolean> effectiveDomain) {
            requireCurrent(turn);
            Hazards observed = protocol.sideHazards(side);
            if (!Set.of("p1", "p2").contains(side) || !effectiveDomain.known()
                    || !Boolean.TRUE.equals(effectiveDomain.value()) || !allActorsCovered()
                    || !unknown.isEmpty()) return observed;
            return new Hazards(observed.layers(), true, token() + ":source-verified-initial-field-and-side-events");
        }
    }
    private long epoch;
    private Capture current;
    public void reset() { epoch++; current = null; }
    private static String key(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceFirst("^(move|ability):\\s*", "").replaceAll("[^a-z0-9]", "");
    }
    private static final class Mutable {
        String slot, name; int entry; boolean covered; Set<String> volatiles = new HashSet<>();
    }
    public Capture update(PublicProtocolEvidence protocol, PublicMechanicsEvidence evidence) {
        epoch++; Set<String> unknown = new TreeSet<>();
        unknown.addAll(protocol.coverageUnknown());
        if (!protocol.framed()) unknown.add("PUBLIC_ENVELOPE_PROVENANCE_UNKNOWN");
        Known<String> weather = Known.unknown(), terrain = Known.unknown();
        Known<Boolean> trick = Known.unknown(), magic = Known.unknown();
        Map<String, Boolean> wind = new HashMap<>();
        Map<String, Mutable> active = new HashMap<>(); Map<String, Integer> entries = new HashMap<>();
        boolean start = false, gen9 = false, singles = false; int turn = 0;
        if (!protocol.complete() || protocol.corrupted()) unknown.add("PUBLIC_STREAM_INCOMPLETE_OR_CORRUPT");
        for (String line : protocol.mechanicsEvents()) {
            String[] p = line.split("\\|", -1); String event = p[1];
            if (event.equals("gen")) { if (gen9 || start) unknown.add("FORMAT_METADATA_CHANGED"); gen9 = p.length == 3 && p[2].equals("9"); continue; }
            if (event.equals("gametype")) { if (singles || start) unknown.add("FORMAT_METADATA_CHANGED"); singles = p.length == 3 && p[2].equals("singles"); continue; }
            if (event.equals("start")) {
                if (start || turn != 0 || !active.isEmpty()) unknown.add("INVALID_PUBLIC_INITIALIZATION");
                start = true;
                // Field constructor starts empty in this pinned simulator. This is
                // valid only at witnessed start, never inferred from missing upkeep.
                weather = Known.of("none", SOURCE + ":witnessed-start"); terrain = Known.of("none", SOURCE + ":witnessed-start");
                trick = Known.of(false, SOURCE + ":witnessed-start"); magic = Known.of(false, SOURCE + ":witnessed-start");
                wind.put("p1", false); wind.put("p2", false); continue;
            }
            if (event.equals("turn")) {
                turn = Integer.parseInt(p[2]); active.values().forEach(a -> a.volatiles.remove("protect")); continue;
            }
            if (event.equals("-weather")) {
                String value = p.length >= 3 ? key(p[2]) : "";
                if (Set.of("none", "sunnyday", "raindance").contains(value)) weather = Known.of(value, SOURCE + ":weather-event");
                else { weather = Known.unknown(); unknown.add("UNSUPPORTED_WEATHER"); }
                continue;
            }
            if (Set.of("-fieldstart", "-fieldend").contains(event)) {
                String effect = p.length > 2 ? key(p[2]) : ""; boolean on = event.equals("-fieldstart");
                switch (effect) {
                    case "trickroom" -> trick = Known.of(on, SOURCE + ":room-event");
                    case "magicroom" -> magic = Known.of(on, SOURCE + ":room-event");
                    case "psychicterrain" -> terrain = Known.of(on ? effect : "none", SOURCE + ":terrain-event");
                    default -> { unknown.add("UNSUPPORTED_FIELD:" + effect); terrain = Known.unknown(); }
                }
                continue;
            }
            if (Set.of("-sidestart", "-sideend").contains(event)) {
                String side = p.length > 2 ? p[2].split(":", 2)[0].trim() : "";
                String effect = p.length > 3 ? key(p[3]) : "";
                if (!Set.of("p1", "p2").contains(side)) unknown.add("SIDE_BINDING_UNKNOWN");
                else if (effect.equals("tailwind")) wind.put(side, event.equals("-sidestart"));
                else if (!Set.of("spikes", "stealthrock", "stickyweb", "toxicspikes").contains(effect)) unknown.add("UNSUPPORTED_SIDE:" + effect);
                continue;
            }
            if (event.equals("-swapsideconditions")) { wind.clear(); unknown.add("SIDE_EXCHANGE_UNRESOLVED"); continue; }
            if (event.equals("-clearallboost")) { unknown.add("GLOBAL_STAGE_CHANGE_UNRESOLVED"); continue; }
            if (p.length < 3) continue;
            String name = p[2].trim(); String slot = name.split(":", 2)[0].trim();
            if (!slot.matches("p[12]a")) { unknown.add("UNSUPPORTED_ACTOR_OR_FORMAT"); continue; }
            if (Set.of("switch", "drag").contains(event)) {
                Mutable a = new Mutable(); a.slot = slot; a.name = name; a.entry = entries.merge(slot, 1, Integer::sum);
                a.covered = start; active.put(slot, a); continue;
            }
            Mutable a = active.get(slot);
            if (a == null || !a.name.equals(name)) { unknown.add("ACTOR_BINDING_UNKNOWN"); continue; }
            if (event.equals("replace")) { a.covered = false; unknown.add("REVEAL_EXECUTION_HISTORY_UNRESOLVED"); continue; }
            if (event.equals("-singleturn")) {
                if (p.length > 3 && key(p[3]).equals("protect")) a.volatiles.add("protect"); else a.covered = false;
            } else if (Set.of("-start", "-end").contains(event)) {
                String effect = p.length > 3 ? key(p[3]) : "";
                if (effect.equals("taunt")) { if (event.equals("-start")) a.volatiles.add(effect); else a.volatiles.remove(effect); }
                else a.covered = false;
            } else if (Set.of("-activate", "-singlemove", "detailschange", "-formechange", "-transform", "-terastallize", "-endability", "-sethp",
                    "-block", "-prepare", "-mustrecharge", "-zpower", "-mega", "-primal", "-burst", "-dynamax",
                    "-clearallboost", "-invertboost", "-swapboost", "-setboost", "-copyboost", "-clearnegativeboost", "cant").contains(event)) {
                a.covered = false;
            } else if (event.equals("move")) {
                // Already executed moves outside the modeled set may leave unannounced
                // state (Baton Pass, forced locks, ability changes). Do not erase it.
                // These source-audited setters leave only the side-condition
                // events already tracked above. They are not supported root
                // actions in the HP-exchange evaluator. A current runtime source
                // certificate is still required before any field result is used.
                boolean observedHazardSetter=p.length>=4 && Set.of("spikes","stealthrock","stickyweb").contains(key(p[3]));
                if (p.length < 4 || CobblemonMechanics.move(key(p[3])) == null && !observedHazardSetter) {
                    a.covered = false; unknown.add("UNSUPPORTED_EXECUTED_MOVE_OR_TRANSFER");
                }
                for (int i = 4; i < p.length; i++) if (p[i].startsWith("[")) {
                    a.covered = false; unknown.add("INDIRECT_EXECUTION_UNRESOLVED");
                }
            }
        }
        if (!start || !gen9 || !singles || turn == 0) unknown.add("WITNESSED_GEN9_SINGLES_START_OR_TURN_MISSING");
        Map<String, Active> actors = new HashMap<>();
        active.forEach((slot, a) -> actors.put(slot, new Active(slot, a.entry, a.name, a.volatiles, a.covered)));
        current = new Capture(this, epoch, turn, weather, terrain, trick, magic, wind, actors, unknown, evidence, protocol); return current;
    }
}
