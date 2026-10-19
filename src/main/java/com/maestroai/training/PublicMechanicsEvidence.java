package com.maestroai.training;

import java.util.*;
import java.util.regex.Pattern;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Actor/entry-bound spectator evidence. No Pokemon, party, private request or inferred set input.
 * The catalog is public rules data, not an opponent object. Raw stat bounds describe the
 * standard gen-9 spread domain; unresolved execution/field/form effects remain a separate gate.
 */
public final class PublicMechanicsEvidence {
    public interface Catalog { Species lookup(String publiclyConfirmedSpecies); }
    public record Species(Map<String, Integer> baseStats, List<String> nativeTypes, Integer fixedHp) {
        public Species {
            baseStats = Map.copyOf(baseStats); nativeTypes = List.copyOf(nativeTypes);
            if (!baseStats.keySet().equals(Set.of("hp", "atk", "def", "spa", "spd", "spe")))
                throw new IllegalArgumentException("incomplete public base stats");
            for (var e : baseStats.entrySet()) if (e.getValue() < 1 || e.getValue() > (e.getKey().equals("hp") ? 255 : 230))
                throw new IllegalArgumentException("base-stat/nature-overflow domain unsupported");
            if (fixedHp != null && fixedHp != 1) throw new IllegalArgumentException("unsupported fixed HP");
            new Profile("catalog-validation", Known.unknown(), Known.unknown(), Map.of(), Known.of(nativeTypes, "public dex"), Known.unknown());
        }
    }
    /** Must independently cover field, execution, priority, entry and simulator overrides.
     * Runtime supplies UNKNOWN; a synthetic/source-certified scenario may explicitly supply true.
     */
    public record Scope(Known<Boolean> otherEffectsResolved, Known<Boolean> trickRoom) {
        public Scope { Objects.requireNonNull(otherEffectsResolved); Objects.requireNonNull(trickRoom); }
        public static Scope unknown() { return new Scope(Known.unknown(), Known.unknown()); }
    }
    public record Observation(String slot, int entry, String publicName, Known<Integer> displayedLevel,
                              Known<Integer> level, Known<String> confirmedSpecies, Known<List<String>> types,
                              Known<PublicStateSearchAdapter.Interval> hp, Known<String> status,
                              Known<String> item, Known<String> ability, boolean ambiguous, boolean unsupportedEffects) {}
    public record Bound(String token, Profile profile, Observation observation, Set<String> unknown) {
        public Bound { unknown = Set.copyOf(unknown); }
    }
    /** Own-team metadata only. The live bridge calls this with its own active/reserve
     * BattlePokemon, never an opponent. Native types/stats are conditional candidates:
     * field/form/volatile overrides are still separately unresolved.
     */
    public static Bound owned(Profile nativeData, Known<String> currentItem, Known<String> nativeAbility,
                              Known<String> status, Scope scope, boolean reserveOnEntry) {
        if (nativeData.actor().equals("opponent")) throw new IllegalArgumentException("owned metadata cannot bind the opponent");
        Known<Boolean> ordinary = neutralModifiers(currentItem, nativeAbility) && yes(scope.otherEffectsResolved())
                ? Known.of(true, "owned-metadata-and-explicit-supported-scope") : Known.unknown();
        var currentTypes = ordinary.known() && nativeData.types().known() ? Known.of(nativeData.types().value(),
                reserveOnEntry ? "own native types under explicit ordinary-entry scope" : "own native types under explicit no-override scope")
                : Known.<List<String>>unknown();
        var profile = new Profile(nativeData.actor(), nativeData.level(), nativeData.maxHp(), nativeData.stats(), currentTypes, ordinary);
        var observation = new Observation("own:" + nativeData.actor(), 0, nativeData.actor(), nativeData.level(), nativeData.level(),
                Known.unknown(), currentTypes, Known.unknown(), status, currentItem, nativeAbility, false, false);
        return new Bound("owned:" + nativeData.actor(), profile, observation,
                ordinary.known() ? Set.of() : Set.of("OWN_NATIVE_DATA_HAS_UNRESOLVED_FIELD_FORM_OR_EXECUTION_EFFECTS"));
    }
    private static final Pattern SLOT = Pattern.compile("p[1-4][a-c]");
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    private final Map<String, Observation> observations = new HashMap<>();
    private record IdentityWitness(String species, int level) {}
    private final Map<Observation, IdentityWitness> entryIdentity = new IdentityHashMap<>();
    private final Catalog catalog;
    public PublicMechanicsEvidence(Catalog catalog) { this.catalog = Objects.requireNonNull(catalog); }
    public void reset() { observations.clear(); entryIdentity.clear(); }
    public Observation observation(String slot) { return observations.get(slot); }
    private static String id(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private static <T> Known<T> known(T value, Mutable a, String detail) {
        return Known.of(value, "spectator:" + a.slot + ":entry-" + a.entry + ":" + detail);
    }
    private static final class Mutable {
        String slot, name;
        String displayedSpecies;
        boolean entryAnnouncementOpen = true;
        IdentityWitness identity;
        int entry;
        Known<Integer> displayed = Known.unknown(), level = Known.unknown();
        Known<String> species = Known.unknown(), status = Known.unknown(), item = Known.unknown(), ability = Known.unknown();
        Known<List<String>> types = Known.unknown();
        Known<PublicStateSearchAdapter.Interval> hp = Known.unknown();
        boolean ambiguous, unsupported, tera;
    }
    /** Only the trusted parser can supply input. Every rebuild invalidates previously captured
     * Observation handles, even when the public appearance has the same nickname/species.
     */
    public void update(PublicProtocolEvidence trusted) {
        observations.clear(); entryIdentity.clear();
        if (!trusted.complete() || trusted.corrupted()) return;
        Map<String, Mutable> active = new HashMap<>(); Map<String, Integer> entries = new HashMap<>();
        for (String line : trusted.mechanicsEvents()) {
            String[] p = line.split("\\|", -1); if (p.length < 3) continue;
            String event = p[1], name = p[2].trim();
            if (event.equals("turn") || event.equals("move"))
                active.values().forEach(a -> a.entryAnnouncementOpen = false);
            String slot = name.contains(":") ? name.substring(0, name.indexOf(':')).trim() : "";
            if (!SLOT.matcher(slot).matches()) continue;
            if (Set.of("switch", "drag").contains(event)) {
                Mutable a = new Mutable(); a.slot = slot; a.name = name; a.entry = entries.merge(slot, 1, Integer::sum);
                active.put(slot, a);
                if (p.length > 3) details(a, p[3], false);
                if (p.length > 4) healthStatus(a, p[4]);
                continue;
            }
            Mutable a = active.get(slot); if (a == null) continue;
            if (!name.equals(a.name) && !event.equals("replace")) {
                invalidate(a); a.ambiguous = true; continue;
            }
            switch (event) {
                case "replace" -> {
                    invalidate(a); a.name = name;
                    if (p.length > 3) details(a, p[3], true);
                    else a.ambiguous = true;
                }
                case "detailschange", "-formechange", "-mega", "-transform" -> {
                    // Even an isolated public Mega announcement invalidates the prior
                    // appearance; it does not grant supported form/ability mechanics.
                    invalidate(a); a.unsupported = true;
                }
                case "-terastallize" -> {
                    a.types = p.length > 3 ? types(p[3], a) : Known.unknown(); a.unsupported = true; a.tera = true;
                }
                case "-start" -> {
                    String effect = p.length > 3 ? id(p[3]) : "";
                    if (effect.equals("typechange")) { if (!a.tera) a.types = p.length > 4 ? types(p[4], a) : Known.unknown(); }
                    else if (effect.equals("typeadd") && a.types.known() && p.length > 4 && !a.tera) {
                        var extra = types(p[4], a);
                        if (extra.known()) {
                            var combined = new LinkedHashSet<>(a.types.value()); combined.addAll(extra.value());
                            a.types = combined.size() <= 3 ? known(List.copyOf(combined), a, "type-add") : Known.unknown();
                        } else a.types = Known.unknown();
                    } else if (!effect.equals("movetaunt") && !effect.equals("taunt")) {
                        a.unsupported = true; a.item = Known.unknown(); a.ability = Known.unknown();
                    }
                }
                case "-end" -> {
                    String effect = p.length > 3 ? id(p[3]) : "";
                    if (Set.of("typechange", "typeadd").contains(effect)) a.types = Known.unknown();
                    else if (!Set.of("illusion", "taunt", "movetaunt").contains(effect)) a.unsupported = true;
                }
                case "-item", "-ability" -> {
                    if (p.length < 4 || id(p[3]).isBlank()) { invalidate(a); break; }
                    if (event.equals("-item")) a.item = known(id(p[3]), a, "current-item");
                    else a.ability = known(id(p[3]), a, "current-ability");
                    // Under an independently certified standard source, the
                    // actual Pressure onStart announcement is positive evidence
                    // excluding Illusion at entry. Never use silence/species to
                    // infer an ability or accept a later Role Play announcement.
                    if (event.equals("-ability") && a.entryAnnouncementOpen && id(p[3]).equals("pressure")
                            && a.displayed.known() && a.displayedSpecies != null)
                        a.identity = new IdentityWitness(a.displayedSpecies, a.displayed.value());
                }
                case "-enditem" -> {
                    // An observed removal is evidence of no current held item for this entry,
                    // not a statement about its native set or future reacquisition.
                    a.item = p.length > 3 && !id(p[3]).isBlank() ? known("none", a, "explicit-item-removal") : Known.unknown();
                }
                case "-endability" -> a.ability = Known.unknown();
                case "-activate" -> { a.unsupported = true; }
                case "-status" -> a.status = p.length > 3 && Set.of("brn", "par", "psn", "tox", "slp", "frz").contains(p[3])
                        ? known(p[3], a, "status") : Known.unknown();
                case "-curestatus" -> a.status = p.length > 3 && Set.of("brn", "par", "psn", "tox", "slp", "frz").contains(p[3])
                        ? known("none", a, "explicit-cure") : Known.unknown();
                case "-damage", "-heal" -> { if (p.length > 3) healthStatus(a, p[3]); }
                case "faint" -> a.hp = known(new PublicStateSearchAdapter.Interval(0, 0), a, "explicit-faint");
                default -> { }
            }
        }
        for (Mutable a : active.values()) {
            var observation = new Observation(a.slot, a.entry, a.name, a.displayed,
                a.level, a.species, a.types, a.ambiguous || trusted.reentryAmbiguous(a.slot) ? Known.unknown() : trusted.hp(a.slot).known() ? trusted.hp(a.slot)
                        : a.species.known() && !a.unsupported ? a.hp : Known.unknown(), a.status, a.item, a.ability,
                a.ambiguous || trusted.reentryAmbiguous(a.slot), a.unsupported);
            observations.put(a.slot, observation);
            if (a.identity != null) entryIdentity.put(observation, a.identity);
        }
    }
    private static void invalidate(Mutable a) {
        a.identity = null; a.entryAnnouncementOpen = false;
        a.level = Known.unknown(); a.species = Known.unknown(); a.types = Known.unknown();
        a.status = Known.unknown(); a.item = Known.unknown(); a.ability = Known.unknown();
        a.hp = Known.unknown();
    }
    private static void details(Mutable a, String raw, boolean reveal) {
        String[] parts = raw.split(",", -1);
        int level = 100; boolean hasLevel = false, valid = !parts[0].isBlank();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.matches("L[0-9]+")) {
                try { level = Integer.parseInt(part.substring(1)); } catch (NumberFormatException bad) { valid = false; }
                if (hasLevel || level < 1 || level > 100) valid = false;
                hasLevel = true;
            } else if (!Set.of("M", "F", "shiny").contains(part) && !UUID.matcher(part).matches()) valid = false;
        }
        if (!valid) { a.ambiguous = true; return; }
        a.displayed = known(level, a, "displayed-level");
        a.displayedSpecies = id(parts[0]);
        // A switch alone may mask species/level. Replacement is explicit;
        // a positive entry witness is considered separately in bind(), only
        // under an independently justified source scope.
        if (reveal) { a.level = known(level, a, "replace-level"); a.species = known(id(parts[0]), a, "replace-species"); }
    }
    private static Known<List<String>> types(String raw, Mutable a) {
        List<String> list = Arrays.stream(raw.split("/", -1)).map(PublicMechanicsEvidence::id).toList();
        try {
            new Profile("validation", Known.unknown(), Known.unknown(), Map.of(), Known.of(list, "public type event"), Known.unknown());
            return known(list, a, "explicit-current-types");
        } catch (IllegalArgumentException invalid) { return Known.unknown(); }
    }
    private static void healthStatus(Mutable a, String raw) {
        if (!raw.matches("[0-9]{1,3}/(?:48|100)(?: (?:brn|par|psn|tox|slp|frz))?")) { a.status = Known.unknown(); a.hp = Known.unknown(); return; }
        String[] parts = raw.split(" "); String[] ratio = parts[0].split("/");
        int n = Integer.parseInt(ratio[0]), d = Integer.parseInt(ratio[1]);
        if (n > d) { a.status = Known.unknown(); a.hp = Known.unknown(); return; }
        a.hp = known(new PublicStateSearchAdapter.Interval(Math.max(0, (n - 1.0) / d), Math.min(1, (n + 1.0) / d)), a, "quantized-health-remeasurement");
        a.status = known(parts.length == 1 ? "none" : parts[1], a, "public-health-status");
    }
    public Bound bind(String role, Observation captured, Scope scope) {
        Objects.requireNonNull(scope);
        if (captured == null) return new Bound("UNKNOWN", Profile.unknown(role), null, Set.of("PUBLIC_ACTOR_UNKNOWN"));
        if (observations.get(captured.slot()) != captured) throw new IllegalArgumentException("stale/foreign public actor observation");
        String token = captured.slot() + ":entry-" + captured.entry();
        Known<Integer> level = captured.level();
        Known<String> confirmed = captured.confirmedSpecies();
        IdentityWitness witness = entryIdentity.get(captured);
        if (!captured.ambiguous() && !captured.unsupportedEffects() && witness != null && yes(scope.otherEffectsResolved())) {
            level = Known.of(witness.level(), token + ":source-verified-Pressure-entry");
            confirmed = Known.of(witness.species(), token + ":source-verified-Pressure-entry");
        }
        Set<String> unknown = new TreeSet<>(); Species s = confirmed.known() && !captured.ambiguous()
                && !captured.unsupportedEffects() ? catalog.lookup(confirmed.value()) : null;
        Map<String, Known<IntRange>> stats = new TreeMap<>(); Known<IntRange> hp = Known.unknown();
        if (s != null && level.known()) {
            int number = level.value();
            for (var e : s.baseStats().entrySet()) {
                var range = statBounds(e.getKey(), e.getValue(), number, s.fixedHp());
                if (e.getKey().equals("hp")) hp = Known.of(range, token + ":public-dex-standard-spread-domain");
                else stats.put(e.getKey(), Known.of(range, token + ":public-dex-standard-spread-domain"));
            }
        } else unknown.add("CONFIRMED_SPECIES_LEVEL_OR_PUBLIC_CATALOG_UNKNOWN");
        boolean modifiers = neutralModifiers(captured.item(), captured.ability());
        Known<Boolean> ordinary = !captured.ambiguous() && !captured.unsupportedEffects() && modifiers && yes(scope.otherEffectsResolved())
                ? Known.of(true, token + ":revealed-neutral-modifiers-and-explicit-scope") : Known.unknown();
        if (!ordinary.known()) unknown.add("ITEM_ABILITY_FIELD_EXECUTION_OR_FORM_UNRESOLVED");
        Known<List<String>> currentTypes = captured.types();
        if (!currentTypes.known() && s != null && ordinary.known()) currentTypes = Known.of(s.nativeTypes(), token + ":verified-native-type-scope");
        if (!currentTypes.known()) unknown.add("CURRENT_TYPES_UNKNOWN");
        Profile profile = new Profile(role, level, hp, stats, currentTypes, ordinary);
        return new Bound(token, profile, captured, unknown);
    }
    public static boolean neutralModifiers(Known<String> item, Known<String> ability) {
        return item.known() && Set.of("none", "heavydutyboots").contains(item.value())
                && ability.known() && Set.of("runaway", "magicguard", "pressure").contains(ability.value());
    }
    public static IntRange statBounds(String stat, int base, int level, Integer fixedHp) {
        if (!Set.of("hp", "atk", "def", "spa", "spd", "spe").contains(stat) || level < 1 || level > 100
                || base < 1 || base > (stat.equals("hp") ? 255 : 230)) throw new IllegalArgumentException("unsupported stat domain");
        if (stat.equals("hp")) {
            if (fixedHp != null) { if (fixedHp != 1) throw new IllegalArgumentException("unsupported fixed HP"); return IntRange.exact(1); }
            return new IntRange((2 * base + 100) * level / 100 + 10, (2 * base + 31 + 63 + 100) * level / 100 + 10);
        }
        return new IntRange((2 * base * level / 100 + 5) * 90 / 100,
                ((2 * base + 31 + 63) * level / 100 + 5) * 110 / 100);
    }
    /** Evidence enters the existing source-verified entry calculator; unknown suppression,
     * groundedness and arbitrary entry effects still widen bounds and block a full ranking.
     */
    public static Entry entry(Hazards hazards, Known<Integer> ownMaxHp, Bound bound, Scope scope) {
        if (bound.profile().actor().equals("opponent")) throw new IllegalArgumentException("opponent evidence cannot bind own entry HP");
        Observation a = bound.observation();
        Known<Boolean> boots = a != null && a.item().known() ? Known.of(a.item().value().equals("heavydutyboots"), a.item().source()) : Known.unknown();
        Known<Boolean> guard = a != null && a.ability().known() ? Known.of(a.ability().value().equals("magicguard"), a.ability().source()) : Known.unknown();
        Known<Boolean> grounded = Known.unknown(); Known<Integer> rock = Known.unknown();
        if (bound.profile().types().known() && yes(bound.profile().ordinaryEffects())) {
            var types = bound.profile().types().value();
            grounded = Known.of(!types.contains("flying"), bound.profile().types().source());
            rock = Known.of(effectiveness("rock", types), bound.profile().types().source());
        }
        var resolved = yes(scope.otherEffectsResolved()) ? bound.profile().ordinaryEffects() : Known.<Boolean>unknown();
        return CobblemonMechanics.entry(hazards, ownMaxHp, grounded, boots, guard, rock, resolved);
    }
}
