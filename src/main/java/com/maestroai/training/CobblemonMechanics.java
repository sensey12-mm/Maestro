package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.Interval;

/** Source-verified, conditional singles slice of the bundled Cobblemon 1.7.3 simulator.
 * No species lookup, opponent internals, guessed sets, or success probabilities.
 */
public final class CobblemonMechanics {
    private CobblemonMechanics() {}
    public static final String SOURCE = "cobblemon-7553235/showdown-1c398031:ordinary-gen9-singles-v1";
    public enum Critical { NON_CRITICAL, CRITICAL, BOTH }
    public enum Order { SELF_FIRST, OPPONENT_FIRST, BOTH, UNKNOWN }
    public record IntRange(int low, int high) {
        public IntRange {
            if (low < 1 || high < low || high > 10000) throw new IllegalArgumentException("invalid stat/HP bounds");
        }
        public static IntRange exact(int n) { return new IntRange(n, n); }
    }
    /** Base stats before observed stages. The provider must justify every opponent bound publicly.
     * ordinaryEffects excludes item/ability/field/volatile/form overrides; true is an explicit
     * scenario precondition, not something inferred from an unrevealed item or ability.
     */
    public record Profile(String actor, Known<Integer> level, Known<IntRange> maxHp,
                          Map<String, Known<IntRange>> stats, Known<List<String>> types,
                          Known<Boolean> ordinaryEffects) {
        public Profile {
            if (actor == null || actor.isBlank()) throw new IllegalArgumentException("missing actor binding");
            Objects.requireNonNull(level); Objects.requireNonNull(maxHp); Objects.requireNonNull(types);
            Objects.requireNonNull(ordinaryEffects); stats = sorted(stats);
            if (level.known() && (level.value() < 1 || level.value() > 100))
                throw new IllegalArgumentException("unsupported level");
            if (stats.keySet().stream().anyMatch(k -> !Set.of("atk", "def", "spa", "spd", "spe").contains(k)))
                throw new IllegalArgumentException("invalid stat");
            if (types.known()) {
                if (types.value().isEmpty() || types.value().size() > 3
                        || new HashSet<>(types.value()).size() != types.value().size()
                        || types.value().stream().anyMatch(t -> !TYPES.contains(t)))
                    throw new IllegalArgumentException("invalid known current types");
                types = Known.of(List.copyOf(types.value()), types.source());
            }
        }
        public Known<IntRange> stat(String key) { return stats.getOrDefault(key, Known.unknown()); }
        public static Profile unknown(String actor) {
            return new Profile(actor, Known.unknown(), Known.unknown(), Map.of(), Known.unknown(), Known.unknown());
        }
    }
    public record Move(String id, String type, int power, boolean physical, int priority,
                       Map<String, Integer> boosts, boolean damageOnly, int accuracy) {
        public Move { boosts = Map.copyOf(boosts); if(accuracy < 1 || accuracy > 100) throw new IllegalArgumentException("unsupported accuracy"); }
        public Move(String id,String type,int power,boolean physical,int priority,
                    Map<String,Integer> boosts,boolean damageOnly) {
            this(id,type,power,physical,priority,boosts,damageOnly,100);
        }
        // Psyshock is Special against physical Defense: do not use the
        // Physical attack stat or the target's ordinary Special Defense.
        public String attackingStat() { return physical ? "atk" : "spa"; }
        public String defendingStat() {
            return physical || id.equals("psyshock") || id.equals("psystrike") ? "def" : "spd";
        }
    }
    // Deliberately small fixed-power/no-secondary allowlist, checked against the pinned moves.js.
    private static final Map<String, Move> MOVES = Map.ofEntries(
            Map.entry("tackle", new Move("tackle", "normal", 40, true, 0, Map.of(), true)),
            Map.entry("swift", new Move("swift", "normal", 60, false, 0, Map.of(), true)),
            Map.entry("scratch", new Move("scratch", "normal", 40, true, 0, Map.of(), true)),
            Map.entry("quickattack", new Move("quickattack", "normal", 40, true, 1, Map.of(), true)),
            Map.entry("shadowsneak", new Move("shadowsneak", "ghost", 40, true, 1, Map.of(), true)),
            Map.entry("recover", new Move("recover", "normal", 0, false, 0, Map.of(), false)),
            Map.entry("extremespeed", new Move("extremespeed", "normal", 80, true, 2, Map.of(), true)),
            Map.entry("shadowclaw", new Move("shadowclaw", "ghost", 70, true, 0, Map.of(), true)),
            Map.entry("psychocut", new Move("psychocut", "psychic", 70, true, 0, Map.of(), true)),
            // Actual Maestro Calyrex-Shadow moves, restricted to ordinary
            // witnessed-reply, source-covered, single-target/singles exchange.
            // Astral Barrage hits all adjacent foes, which is one in singles.
            Map.entry("astralbarrage", new Move("astralbarrage", "ghost", 120, false, 0, Map.of(), true)),
            Map.entry("psyshock", new Move("psyshock", "psychic", 80, false, 0, Map.of(), true)),
            Map.entry("dragonclaw", new Move("dragonclaw", "dragon", 80, true, 0, Map.of(), true)),
            // Damage formula is supported, but burn secondary prevents a complete turn forecast.
            Map.entry("flamethrower", new Move("flamethrower", "fire", 90, false, 0, Map.of(), false)),
            Map.entry("uturn", new Move("uturn", "bug", 70, true, 0, Map.of(), false)),
            Map.entry("ironhead", new Move("ironhead", "steel", 80, true, 0, Map.of(), false)),
            Map.entry("icebeam", new Move("icebeam", "ice", 90, false, 0, Map.of(), false)),
            Map.entry("drainingkiss", new Move("drainingkiss", "fairy", 50, false, 0, Map.of(), false)),
            Map.entry("energyball", new Move("energyball", "grass", 90, false, 0, Map.of(), false)),
            Map.entry("geomancy", new Move("geomancy", "fairy", 0, false, 0, Map.of("spa", 2, "spd", 2, "spe", 2), false)),
            Map.entry("moonblast", new Move("moonblast", "fairy", 95, false, 0, Map.of(), false)),
            Map.entry("focusblast", new Move("focusblast", "fighting", 120, false, 0, Map.of(), false, 70)),
            Map.entry("thunder", new Move("thunder", "electric", 110, false, 0, Map.of(), false, 70)),
            Map.entry("psystrike", new Move("psystrike", "psychic", 100, false, 0, Map.of(), true)),
            Map.entry("fireblast", new Move("fireblast", "fire", 110, false, 0, Map.of(), false, 85)),
            Map.entry("spectralthief", new Move("spectralthief", "ghost", 90, true, 0, Map.of(), false)),
            Map.entry("closecombat", new Move("closecombat", "fighting", 120, true, 0, Map.of(), false)),
            Map.entry("rocktomb", new Move("rocktomb", "rock", 60, true, 0, Map.of(), false, 95)),
            Map.entry("swordsdance", new Move("swordsdance", "normal", 0, false, 0, Map.of("atk", 2), false)),
            Map.entry("nastyplot", new Move("nastyplot", "dark", 0, false, 0, Map.of("spa", 2), false)),
            Map.entry("bulkup", new Move("bulkup", "fighting", 0, false, 0, Map.of("atk", 1, "def", 1), false)));
    private static final Set<String> TYPES = Set.of("normal", "fire", "water", "electric", "grass", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
    public static Move move(String id) { return MOVES.get(id); }
    /** Calculator IDs only; each selected operation still requires its current evidence certificate. */
    public static Set<String> supportedMoveIds() { return MOVES.keySet(); }
    public record Damage(Known<IntRange> points, Known<Interval> fraction, Set<String> unknown, String source) {
        public Damage { unknown = Set.copyOf(unknown); }
        public static Damage unknown(String reason) {
            return new Damage(Known.unknown(), Known.unknown(), Set.of(reason), SOURCE);
        }
    }

    /** Conditional on a successful single hit. BOTH bounds all 16 ordinary and critical rolls,
     * including critical stage bypass. No miss/critical probability is asserted.
     */
    public static Damage damage(String moveId, Profile attacker, Actor attackerState,
                                Profile target, Actor targetState, Critical critical) {
        return damage(moveId, attacker, attackerState, target, targetState, critical, 1, 1);
    }
    /** Weather acts before crit/random/STAB, so scaling the final interval is wrong. */
    static Damage damage(String moveId, Profile attacker, Actor attackerState,
                         Profile target, Actor targetState, Critical critical, int weatherNumerator, int weatherDenominator) {
        return damage(moveId, attacker, attackerState, target, targetState, critical, weatherNumerator, weatherDenominator, 1, 1);
    }
    static Damage damage(String moveId, Profile attacker, Actor attackerState, Profile target, Actor targetState,
                         Critical critical, int weatherNumerator, int weatherDenominator, int powerNumerator, int powerDenominator) {
        return damage(moveId,attacker,attackerState,target,targetState,critical,
                weatherNumerator,weatherDenominator,powerNumerator,powerDenominator,1,1);
    }
    /** Fixed-point source power and final-damage modifiers retain native rounding order. */
    static Damage damage(String moveId,Profile attacker,Actor attackerState,Profile target,Actor targetState,
            Critical critical,int weatherNumerator,int weatherDenominator,int powerNumerator,
            int powerDenominator,int finalNumerator,int finalDenominator) {
        return damage(moveId,attacker,attackerState,target,targetState,critical,weatherNumerator,weatherDenominator,
                powerNumerator,powerDenominator,finalNumerator,finalDenominator,1,1);
    }
    /** Native defense weather hooks run after critical-adjusted stages and
     * before the damage formula; they are not final-damage multipliers. */
    static Damage damage(String moveId,Profile attacker,Actor attackerState,Profile target,Actor targetState,
            Critical critical,int weatherNumerator,int weatherDenominator,int powerNumerator,
            int powerDenominator,int finalNumerator,int finalDenominator,int defenseNumerator,int defenseDenominator) {
        bound(attacker, attackerState); bound(target, targetState); Objects.requireNonNull(critical);
        Move move = move(moveId);
        if (move == null || move.power() == 0) return Damage.unknown("UNSUPPORTED_DAMAGE_MOVE");
        if (!yes(attacker.ordinaryEffects()) || !yes(target.ordinaryEffects()))
            return Damage.unknown("ITEM_ABILITY_FIELD_OR_FORM_EFFECTS_UNRESOLVED");
        if (!attacker.types().known() || !target.types().known()) return Damage.unknown("CURRENT_TYPES_UNKNOWN");
        int exponent = effectiveness(move.type(), target.types().value());
        if (exponent == Integer.MIN_VALUE)
            return new Damage(Known.unknown(), Known.of(new Interval(0, 0), SOURCE + ":type-immunity"), Set.of(), SOURCE);
        String offense = move.attackingStat(), defense = move.defendingStat();
        if (!attacker.level().known() || !attacker.stat(offense).known() || !target.stat(defense).known()
                || !target.maxHp().known() || !attackerState.stages().containsKey(offense)
                || !targetState.stages().containsKey(defense) || !attackerState.status().known())
            return Damage.unknown("PUBLIC_STAT_STAGE_LEVEL_HP_OR_STATUS_BOUNDS_MISSING");
        int power = (int) halfDown(move.power(), powerNumerator, powerDenominator);
        if (power < 1 || power > 1000) return Damage.unknown("BASE_POWER_MODIFIER_OUTSIDE_SLICE");
        int low = Integer.MAX_VALUE, high = 0;
        boolean stab = attacker.types().value().contains(move.type());
        boolean burn = move.physical() && attackerState.status().value().equals("brn");
        for (boolean crit : critical == Critical.BOTH ? new boolean[]{false, true}
                : new boolean[]{critical == Critical.CRITICAL}) {
            int aStage = attackerState.stages().get(offense), dStage = targetState.stages().get(defense);
            if (crit) { aStage = Math.max(0, aStage); dStage = Math.min(0, dStage); }
            if (stage(attacker.stat(offense).value().low(), aStage) == 0
                    || stage(target.stat(defense).value().low(), dStage) == 0)
                return Damage.unknown("ZERO_EFFECTIVE_STAT_OUTSIDE_SLICE");
            int min = hit(attacker.level().value(), power, stage(attacker.stat(offense).value().low(), aStage),
                    (int)halfDown(stage(target.stat(defense).value().high(), dStage),defenseNumerator,defenseDenominator), stab, exponent, burn, crit, 85, weatherNumerator, weatherDenominator,finalNumerator,finalDenominator);
            int max = hit(attacker.level().value(), power, stage(attacker.stat(offense).value().high(), aStage),
                    (int)halfDown(stage(target.stat(defense).value().low(), dStage),defenseNumerator,defenseDenominator), stab, exponent, burn, crit, 100, weatherNumerator, weatherDenominator,finalNumerator,finalDenominator);
            if (min < 0 || max < 0 || max > 10000) return Damage.unknown("DAMAGE_OVERFLOW_OUTSIDE_SLICE");
            low = Math.min(low, min); high = Math.max(high, max);
        }
        // Zero is represented by the fraction because IntRange describes positive stats/HP.
        return new Damage(Known.of(new IntRange(low, high), SOURCE),
                Known.of(new Interval((double) low / target.maxHp().value().high(),
                        (double) high / target.maxHp().value().low()), SOURCE), Set.of(), SOURCE);
    }
    private static int hit(int level, int power, int atk, int def, boolean stab,
                           int exponent, boolean burn, boolean crit, int roll, int weatherNumerator, int weatherDenominator,
                            int finalNumerator, int finalDenominator) {
        long damage = ((2L * level / 5 + 2) * power * atk / def) / 50 + 2;
        damage = halfDown(damage, weatherNumerator, weatherDenominator);
        if (crit) damage = damage * 3 / 2;
        damage = damage * roll / 100;
        if (stab) damage = halfDown(damage, 3, 2);
        if (exponent > 0) damage <<= exponent;
        else if (exponent < 0) damage >>= -exponent;
        if (burn) damage = halfDown(damage, 1, 2);
        damage = halfDown(damage,finalNumerator,finalDenominator);
        // Showdown finally truncates to 16 bits. Refuse that overflow domain instead of
        // assuming monotonic endpoints across a wrap.
        return damage >= 65536 ? -1 : (int) Math.max(1, damage);
    }
    private static long halfDown(long value, long numerator, long denominator) {
        long modifier = numerator * 4096 / denominator;
        return (value * modifier + 2047) / 4096;
    }
    public static int stage(int stat, int stage) {
        if (stat < 1 || stat > 10000 || stage < -6 || stage > 6) throw new IllegalArgumentException("invalid stat stage");
        return stage >= 0 ? stat * (stage + 2) / 2 : stat * 2 / (2 - stage);
    }
    /** Standard gen-nine 18-type effectiveness table, derived from pinned
     * Showdown typechart; oracle verifies every attacker/defender cell.
     * Values: 1=super-effective, -1=resisted, X=immune, omitted=neutral.
     * Deliberately no Stellar/Inverse Battle or pack-specific overlays.
     */
    private static final String[] TYPE_MATCHUPS = {
            "bug dark:1 fairy:-1 fighting:-1 fire:-1 flying:-1 ghost:-1 grass:1 poison:-1 psychic:1 steel:-1",
            "dark dark:-1 fairy:-1 fighting:-1 ghost:1 psychic:1",
            "dragon dragon:1 fairy:X steel:-1",
            "electric dragon:-1 electric:-1 flying:1 grass:-1 ground:X water:1",
            "fairy dark:1 dragon:1 fighting:1 fire:-1 poison:-1 steel:-1",
            "fighting bug:-1 dark:1 fairy:-1 flying:-1 ghost:X ice:1 normal:1 poison:-1 psychic:-1 rock:1 steel:1",
            "fire bug:1 dragon:-1 fire:-1 grass:1 ice:1 rock:-1 steel:1 water:-1",
            "flying bug:1 electric:-1 fighting:1 grass:1 rock:-1 steel:-1",
            "ghost dark:-1 ghost:1 normal:X psychic:1",
            "grass bug:-1 dragon:-1 fire:-1 flying:-1 grass:-1 ground:1 poison:-1 rock:1 steel:-1 water:1",
            "ground bug:-1 electric:1 fire:1 flying:X grass:-1 poison:1 rock:1 steel:1",
            "ice dragon:1 fire:-1 flying:1 grass:1 ground:1 ice:-1 steel:-1 water:-1",
            "normal ghost:X rock:-1 steel:-1",
            "poison fairy:1 ghost:-1 grass:1 ground:-1 poison:-1 rock:-1 steel:X",
            "psychic dark:X fighting:1 poison:1 psychic:-1 steel:-1",
            "rock bug:1 fighting:-1 fire:1 flying:1 ground:-1 ice:1 steel:-1",
            "steel electric:-1 fairy:1 fire:-1 ice:1 rock:1 steel:-1 water:-1",
            "water dragon:-1 fire:1 grass:-1 ground:1 rock:1 water:-1"
    };
    private static final Map<String, Map<String,Integer>> TYPE_CHART = loadTypeChart();
    private static Map<String, Map<String,Integer>> loadTypeChart() {
        Map<String,Map<String,Integer>> chart=new TreeMap<>();
        for(String row:TYPE_MATCHUPS) {
            String[] fields=row.split(" ");
            if(!TYPES.contains(fields[0]) || chart.containsKey(fields[0]))
                throw new IllegalStateException("invalid attack type chart");
            Map<String,Integer> defenders=new HashMap<>();
            for(int i=1;i<fields.length;i++) {
                String[] part=fields[i].split(":");
                if(part.length!=2 || !TYPES.contains(part[0]))
                    throw new IllegalStateException("invalid defense type chart");
                int v=part[1].equals("X")?Integer.MIN_VALUE:Integer.parseInt(part[1]);
                if((v!=Integer.MIN_VALUE && Math.abs(v)!=1) || defenders.putIfAbsent(part[0],v)!=null)
                    throw new IllegalStateException("duplicate/invalid type effectiveness");
            }
            chart.put(fields[0],Map.copyOf(defenders));
        }
        if(!chart.keySet().equals(TYPES))throw new IllegalStateException("incomplete type effectiveness chart");
        return Map.copyOf(chart);
    }
    public static int effectiveness(String attackType, List<String> targetTypes) {
        var attack=TYPE_CHART.get(attackType);
        if(attack==null)throw new IllegalArgumentException("unsupported attacking type");
        int score=0;
        for(String type:targetTypes) {
            if(!TYPES.contains(type))throw new IllegalArgumentException("unsupported target type");
            int cell=attack.getOrDefault(type,0);
            if(cell==Integer.MIN_VALUE)return Integer.MIN_VALUE;
            score+=cell;
        }
        return score;
    }
    public static Known<IntRange> speed(Profile profile, Actor actor) {
        Known<IntRange> staged = stagedSpeed(profile, actor);
        if (!staged.known()) return Known.unknown();
        int low = staged.value().low(), high = staged.value().high();
        // The native paralysis callback floors after finalModify, rather than
        // applying another round-half-down modifier.
        if (actor.status().value().equals("par")) { low /= 2; high /= 2; }
        if (low < 1) return Known.unknown(); // Native zero speed is outside IntRange.
        return Known.of(new IntRange(low, high), SOURCE + ":effective-speed");
    }
    /** Stage arithmetic before item/side modifiers and the final status callback. */
    static Known<IntRange> stagedSpeed(Profile profile, Actor actor) {
        bound(profile, actor);
        Integer stage = actor.stages().get("spe");
        if (!yes(profile.ordinaryEffects()) || !profile.stat("spe").known() || stage == null || !actor.status().known()
                || !Set.of("none","par","slp","frz","brn","psn","tox").contains(actor.status().value()))
            return Known.unknown();
        IntRange base = profile.stat("spe").value();
        int low = stage(base.low(), stage), high = stage(base.high(), stage);
        if (low < 1 || high > 10000) return Known.unknown();
        return Known.of(new IntRange(low, high), SOURCE + ":effective-speed");
    }
    /** Nominal priority is insufficient: effectsResolved must exclude priority/fractional-priority
     * overrides, Pursuit interception, forced replacement and before-turn actions.
     */
    public static Order order(String selfAction, String opponentMove, Known<IntRange> selfSpeed,
                              Known<IntRange> opponentSpeed, Known<Boolean> trickRoom,
                              Known<Boolean> effectsResolved) {
        if (!yes(effectsResolved) || !trickRoom.known()) return Order.UNKNOWN;
        Move foe = move(opponentMove);
        if (foe == null) return Order.UNKNOWN;
        if (selfAction.startsWith("switch:")) return Order.SELF_FIRST;
        if (!selfAction.startsWith("move:")) throw new IllegalArgumentException("invalid action");
        Move self = move(selfAction.substring(5));
        if (self == null) return Order.UNKNOWN;
        if (self.priority() != foe.priority()) return self.priority() > foe.priority() ? Order.SELF_FIRST : Order.OPPONENT_FIRST;
        if (!selfSpeed.known() || !opponentSpeed.known()) return Order.UNKNOWN;
        IntRange a = selfSpeed.value(), b = opponentSpeed.value();
        // getActionSpeed truncates to 13 bits, including after Trick Room. Only the
        // monotonic domain is certified here; do not guess across its wrap boundary.
        int limit = trickRoom.value() ? 1807 : 8191;
        if (a.high() > limit || b.high() > limit) return Order.UNKNOWN;
        if (a.low() > b.high()) return trickRoom.value() ? Order.OPPONENT_FIRST : Order.SELF_FIRST;
        if (b.low() > a.high()) return trickRoom.value() ? Order.SELF_FIRST : Order.OPPONENT_FIRST;
        return Order.BOTH; // tied or overlapping ranges; no fabricated 50% split
    }
    public record Entry(Known<Interval> damage, Known<Integer> speedDelta, Set<String> unknown) {
        public Entry { unknown = Set.copyOf(unknown); }
    }
    /** Partial layers are lower bounds; missing hazards are bounded by their supported caps.
     * Damage is applied per hazard with the engine's floor/minimum-one rule, never by
     * multiplying a rounded total fraction. Non-HP effects stay independently UNKNOWN.
     */
    public static Entry entry(Hazards hazards, Known<Integer> maxHp, Known<Boolean> grounded,
                              Known<Boolean> boots, Known<Boolean> magicGuard, Known<Integer> rockModifier,
                              Known<Boolean> ordinaryEntryEffects) {
        Objects.requireNonNull(hazards); Set<String> unknown = new TreeSet<>();
        if (maxHp.known() && (maxHp.value() < 1 || maxHp.value() > 10000)) throw new IllegalArgumentException("invalid entry HP");
        if (rockModifier.known() && (rockModifier.value() < -6 || rockModifier.value() > 6)) throw new IllegalArgumentException("invalid Rock modifier");
        if (hazards.layers().keySet().stream().anyMatch(k -> !Set.of("spikes", "stealthrock", "toxicspikes", "stickyweb").contains(k)))
            throw new IllegalArgumentException("unsupported hazard");
        for (var e : hazards.layers().entrySet()) {
            int cap = e.getKey().equals("spikes") ? 3 : e.getKey().equals("toxicspikes") ? 2 : 1;
            if (e.getValue() > cap) throw new IllegalArgumentException("invalid layer cap");
        }
        if (!hazards.complete()) unknown.add("PARTIAL_HAZARD_LAYERS");
        if (!yes(ordinaryEntryEffects)) unknown.add("ENTRY_MODIFIERS_UNRESOLVED");
        if (!maxHp.known()) return new Entry(Known.unknown(), Known.unknown(), Set.of("OWN_MAX_HP_UNKNOWN"));
        int hp = maxHp.value(), low = 0, high = 0;
        boolean immune = yes(boots) || yes(magicGuard);
        boolean mayImmune = !boots.known() || !magicGuard.known() || !yes(ordinaryEntryEffects);
        if (!immune) {
            int[] rock = layers(hazards, "stealthrock", 1), spikes = layers(hazards, "spikes", 3);
            int minRock = rockModifier.known() ? rockModifier.value() : -6;
            int maxRock = rockModifier.known() ? rockModifier.value() : 6;
            if (!rockModifier.known()) unknown.add("ROCK_EFFECTIVENESS_UNKNOWN");
            low += rock[0] == 0 ? 0 : hazardDamage(hp * Math.pow(2, minRock) / 8);
            high += rock[1] == 0 ? 0 : hazardDamage(hp * Math.pow(2, maxRock) / 8);
            if (!grounded.known()) unknown.add("GROUNDEDNESS_UNKNOWN");
            if (yes(grounded)) low += spikes[0] == 0 ? 0 : hazardDamage(hp * new int[]{0, 3, 4, 6}[spikes[0]] / 24.0);
            if (!grounded.known() || grounded.value()) high += spikes[1] == 0 ? 0 : hazardDamage(hp * new int[]{0, 3, 4, 6}[spikes[1]] / 24.0);
            if (mayImmune) { low = 0; unknown.add("HAZARD_IMMUNITY_OR_SUPPRESSION_UNKNOWN"); }
        }
        if (!yes(ordinaryEntryEffects)) { low = 0; high = hp; } // arbitrary unsupported entry effects are not zero
        int[] web = layers(hazards, "stickyweb", 1), toxic = layers(hazards, "toxicspikes", 2);
        Known<Integer> delta = Known.unknown();
        if (yes(boots) || grounded.known() && !grounded.value() || web[1] == 0) delta = Known.of(0, SOURCE);
        else if (yes(ordinaryEntryEffects) && yes(grounded) && boots.known() && !boots.value() && web[0] == web[1])
            delta = Known.of(-1, SOURCE + ":sticky-web");
        if (!delta.known()) unknown.add("STICKY_WEB_OR_BOOST_MODIFIERS_UNKNOWN");
        if (toxic[1] > 0 && !yes(boots) && !(grounded.known() && !grounded.value())) unknown.add("TOXIC_SPIKES_STATUS_OR_ABSORPTION_UNKNOWN");
        return new Entry(Known.of(new Interval(Math.min(1, (double) low / hp), Math.min(1, (double) high / hp)), SOURCE), delta, unknown);
    }
    private static int[] layers(Hazards h, String id, int cap) {
        Integer n = h.layers().get(id);
        if (n == null) return new int[]{0, h.complete() ? 0 : cap};
        return new int[]{n, h.complete() || n == 0 ? n : cap};
    }
    private static int hazardDamage(double value) { return Math.max(1, (int) value); }
    static boolean yes(Known<Boolean> value) { return value.known() && value.value(); }
    static void bound(Profile profile, Actor actor) {
        if (!profile.actor().equals(actor.id())) throw new IllegalArgumentException("mechanics profile belongs to another actor");
    }
}
