package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PublicStateSearchAdapter.Interval;
import static com.maestroai.training.ExecutionCoverage.*;

/** Source-supported field/modifier slice, explicitly conditional on independently
 * justified remaining effects. Runtime passes UNKNOWN for pack/engine overrides;
 * the public protocol alone never proves that those overrides do not exist.
 */
public final class ScopedExecution {
    public record Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability,
                          Known<Boolean> ownTransportCurrent, OwnRequestTransport.Lease lease) {
        public Binding { Objects.requireNonNull(item); Objects.requireNonNull(ability); Objects.requireNonNull(ownTransportCurrent); }
        public Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability, Known<Boolean> current) {
            this(observation, item, ability, current, null);
        }
        public Known<Boolean> ownTransportCurrent() {
            return lease != null && (!lease.current()) ? Known.unknown() : ownTransportCurrent;
        }
        public Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability) {
            this(observation, item, ability, Known.of(true, "explicit conditional own-transport fixture"));
        }
    }
    private final PublicFieldEvidence.Capture field;
    private final Map<String, Binding> actors;
    private final Scopes scopes;
    public ScopedExecution(PublicFieldEvidence.Capture field, Map<String, Binding> actors, Known<Boolean> remainingEffects) {
        this(field, actors, Scopes.all(Objects.requireNonNull(remainingEffects)));
    }
    public ScopedExecution(PublicFieldEvidence.Capture field, Map<String, Binding> actors, Scopes scopes) {
        this.field = Objects.requireNonNull(field); this.actors = Map.copyOf(actors); this.scopes = Objects.requireNonNull(scopes);
        var a = actors.get("self"); var f = actors.get("opponent");
        if (a != null && f != null && a.observation() != null && f.observation() != null
                && a.observation().slot().substring(0, 2).equals(f.observation().slot().substring(0, 2)))
            throw new IllegalArgumentException("own/opponent execution bindings share a side");
    }
    public PublicFieldEvidence.Capture field() { return field; }
    ScopedExecution withScopes(Scopes scopes) { return new ScopedExecution(field, actors, scopes); }
    ScopedExecution calculationFrame() { return withScopes(scopes.calculationFrame()); }
    boolean sourceCurrent() {
        try { field.requireCurrent(field.turn()); }
        catch(IllegalArgumentException stale) { return false; }
        return scopes.current() && actors.values().stream().filter(b->b.lease()!=null)
                .allMatch(b->b.lease().current());
    }
    /**
     * Exhaustiveness of the current *item/ability modifier choice* only:
     * both opponent values must be explicit, current, spectator-public events.
     * This does not certify the foe's move distribution, other unseen party
     * members, form effects, arbitrary addon callbacks or future turns.
     *
     * Actual action arithmetic and fields remain independently gated by every
     * executed operation's certificate and by the completed reply comparison.
     * In particular, merely recognizing an item/ability in the engine registry
     * is never evidence of the opponent's currently held item or ability.
     */
    public Known<Boolean> publicModifierSingletonScope() {
        if (!sourceCurrent() || !field.unknown().isEmpty()) return Known.unknown();
        Binding foe = actors.get("opponent");
        if (foe == null || foe.observation() == null) return Known.unknown();
        var seen = foe.observation();
        if (seen.ambiguous() || seen.unsupportedEffects()
                || !foe.item().known() || !foe.ability().known()
                || !foe.item().equals(seen.item()) || !foe.ability().equals(seen.ability()))
            return Known.unknown();
        var publicEntry = field.actor(seen);
        if (publicEntry == null || !publicEntry.covered()) return Known.unknown();
        return Known.of(true, field.token() + ":current-spectator-public-item-and-ability-only");
    }
    public void requireCurrent(int turn) { field.requireCurrent(turn); }
    public Set<String> unresolved() {
        Set<String> reasons = new TreeSet<>(field.unknown());
        if (Arrays.stream(Operation.values()).anyMatch(op -> !yes(scopes.get(op)))) reasons.add("UNANNOUNCED_PACK_OR_EXECUTION_EFFECTS_UNRESOLVED");
        if (!field.weather().known() || !field.terrain().known() || !field.trickRoom().known() || !field.magicRoom().known())
            reasons.add("PUBLIC_FIELD_SCOPE_UNKNOWN");
        return Set.copyOf(reasons);
    }
    private final class Check {
        final Operation op; final Set<String> required = new TreeSet<>(), missing = new TreeSet<>();
        final List<OwnRequestTransport.Lease> leases = new ArrayList<>();
        Check(Operation op) {
            this.op = op; field.requireCurrent(field.turn());
            need("PUBLIC_PROTOCOL_AND_EFFECT_COVERAGE", field.unknown().stream().allMatch(reason ->
                    reason.equals("UNSUPPORTED_WEATHER") && op != Operation.DAMAGE));
            // Unsupported weather is irrelevant to this limited priority/speed/entry/setup slice.
            field.unknown().stream().filter(reason -> !reason.equals("UNSUPPORTED_WEATHER") || op == Operation.DAMAGE).forEach(missing::add);
            need("SOURCE_DOMAIN_" + op, yes(scopes.get(op)));
        }
        void need(String name, boolean supported) { required.add(name); if (!supported) missing.add(name); }
        void actor(String role, boolean reserve) {
            Binding b = actors.get(role);
            need(role + ":ACTOR_BINDING", b != null); if (b == null) return;
            boolean publicBinding = !role.equals("opponent") || b.observation() != null
                    && b.item().equals(b.observation().item()) && b.ability().equals(b.observation().ability());
            need(role + ":MODIFIER_PROVENANCE", publicBinding);
            if (!role.equals("opponent")) {
                need(role + ":OWN_TRANSPORT_FRESHNESS", yes(b.ownTransportCurrent()) && (b.lease() == null || b.lease().turn() == field.turn()));
                if (b.lease() != null) leases.add(b.lease());
            }
            need(role + ":CURRENT_ITEM", b.item().known() && Set.of("none", "heavydutyboots", "choicescarf", "laggingtail", "powerherb", "marshadiumz", "leftovers").contains(b.item().value()));
            // Pressure affects PP consumption, not the outcome of one already
            // legal, successful action. Future PP/requests are not certified.
            need(role + ":CURRENT_ABILITY", b.ability().known() && Set.of("runaway", "magicguard", "pressure", "levitate", "prankster", "stall", "download", "technician", "multitype", "fairyaura").contains(b.ability().value()));
            // Download's witnessed active boosts are already in the public stage
            // vector. A reserve's future Download boost needs a separate entry model.
            need(role + ":ENTRY_ABILITY_SCOPE", !reserve || !b.ability().known() || !b.ability().value().equals("download"));
            if (reserve) need(role + ":OWN_RESERVE_CHANNEL", !role.equals("opponent") && b.observation() == null);
            else {
                var a = field.actor(b.observation());
                need(role + ":CURRENT_PUBLIC_ENTRY", a != null && a.covered() && !b.observation().ambiguous() && !b.observation().unsupportedEffects());
            }
            need(role + ":ITEM_SUPPRESSION", b.item().known() && b.item().value().equals("none") || field.magicRoom().known());
        }
        Certificate finish() { return new Certificate(field, op, required, missing, () -> scopes.current() && leases.stream().allMatch(OwnRequestTransport.Lease::current)); }
    }
    private boolean supported(String role, boolean reserve) {
        for (Operation op : Operation.values()) { var c = new Check(op); c.actor(role, reserve); if (!c.finish().supported()) return false; }
        return field.weather().known() && field.terrain().known() && field.trickRoom().known() && field.magicRoom().known();
    }
    private Profile certified(Profile p) {
        return new Profile(p.actor(), p.level(), p.maxHp(), p.stats(), p.types(), Known.of(true, field.token() + ":operation-certified"));
    }
    public Certificate damageCoverage(String moveId, Profile attacker, Actor a, Profile target, Actor t) {
        bound(attacker, a); bound(target, t); var c = new Check(Operation.DAMAGE);
        c.actor(a.id(), reserve(a.id())); c.actor(t.id(), reserve(t.id()));
        Move m = move(moveId); c.need("SUPPORTED_DAMAGE_MOVE", m != null && m.power() > 0);
        c.need("WEATHER", field.weather().known()); c.need("TERRAIN", field.terrain().known());
        c.need(a.id() + ":CURRENT_TYPES", attacker.types().known()); c.need(t.id() + ":CURRENT_TYPES", target.types().known());
        if (m != null && m.power() > 0) {
            var active = active(t.id());
            boolean blocked = active != null && active.volatiles().contains("protect")
                    || target.types().known() && effectiveness(m.type(), target.types().value()) == Integer.MIN_VALUE;
            if (!blocked) {
                String atk = m.attackingStat(), def = m.defendingStat();
                c.need(a.id() + ":LEVEL", attacker.level().known());
                c.need(a.id() + ":" + atk, attacker.stat(atk).known() && a.stages().containsKey(atk));
                c.need(t.id() + ":" + def, target.stat(def).known() && t.stages().containsKey(def));
                c.need(t.id() + ":MAX_HP", target.maxHp().known()); c.need(a.id() + ":STATUS", a.status().known());
                if (c.missing.isEmpty()) {
                    int n = m.type().equals("fire") && field.weather().value().equals("sunnyday") ? 3 : 1;
                    int d = m.type().equals("fire") && !field.weather().value().equals("none") ? 2 : 1;
                    // Certify the full supported critical domain, refusing zero-stat
                    // and engine-wrap domains before issuing a usable certificate.
                    var probe = CobblemonMechanics.damage(moveId, certified(attacker), a, certified(target), t, Critical.BOTH, n, d, ability(a.id()).equals("technician") && m.power() <= 60 ? 3 : 1,
                            ability(a.id()).equals("technician") && m.power() <= 60 ? 2 : 1);
                    c.need("SUPPORTED_DAMAGE_ARITHMETIC_DOMAIN", probe.fraction().known());
                    c.missing.addAll(probe.unknown());
                }
            }
        }
        return c.finish();
    }
    public Certificate setupCoverage(Profile p, Actor a, String moveId) {
        bound(p, a); var c = new Check(Operation.SETUP); c.actor(a.id(), reserve(a.id()));
        Move m = move(moveId); c.need("SUPPORTED_SETUP_MOVE", m != null && m.power() == 0);
        c.need("SUPPORTED_SETUP_EXECUTION", m != null && !m.id().equals("geomancy"));
        c.need(a.id() + ":STATUS", a.status().known() && a.status().value().equals("none"));
        if (m != null && m.id().equals("recover")) c.need(a.id() + ":EXACT_RECOVERY_MAX_HP",
                p.maxHp().known() && p.maxHp().value().low() == p.maxHp().value().high());
        if (m != null) m.boosts().keySet().forEach(stat -> c.need(a.id() + ":STAGE_" + stat, a.stages().containsKey(stat)));
        return c.finish();
    }
    public Certificate speedCoverage(Profile p, Actor a) {
        bound(p, a); var c = new Check(Operation.SPEED); c.actor(a.id(), reserve(a.id()));
        c.need(a.id() + ":SPEED", p.stat("spe").known() && a.stages().containsKey("spe"));
        c.need(a.id() + ":STATUS", a.status().known() && a.status().value().equals("none"));
        Binding b = actors.get(reserve(a.id()) ? "self" : a.id());
        c.need(a.id() + ":TAILWIND", b != null && b.observation() != null && field.tailwind(b.observation().slot()).known());
        if (c.missing.isEmpty()) c.need("SUPPORTED_SPEED_ARITHMETIC_DOMAIN", computedSpeed(p, a).known());
        return c.finish();
    }
    public Certificate orderCoverage(String action, String reply, Profile self, Actor a, Profile foe, Actor f) {
        bound(self, a); bound(foe, f); var c = new Check(Operation.ORDER);
        c.actor(a.id(), reserve(a.id())); c.actor(f.id(), reserve(f.id()));
        Move own = action.startsWith("move:") ? move(action.substring(5)) : null;
        c.need("SUPPORTED_ACTION_PAIR", (action.startsWith("switch:") || own != null) && move(reply) != null);
        if (!action.startsWith("switch:")) {
            var acPriority = priorityCoverage(own == null ? "UNKNOWN" : own.id(), a.id());
            var fcPriority = priorityCoverage(reply, f.id());
            c.required.addAll(acPriority.prerequisites()); c.required.addAll(fcPriority.prerequisites());
            c.missing.addAll(acPriority.missing()); c.missing.addAll(fcPriority.missing());
            if (c.missing.isEmpty() && effectivePriority(own.id(), a.id()) == effectivePriority(reply, f.id())) {
                c.required.add("TIED_PRIORITY_SPEED_AND_ROOM");
                var ac = speedCoverage(self, a); var fc = speedCoverage(foe, f);
                c.required.addAll(ac.prerequisites()); c.required.addAll(fc.prerequisites());
                c.missing.addAll(ac.missing()); c.missing.addAll(fc.missing());
                c.need("TRICK_ROOM", field.trickRoom().known());
                if (c.missing.isEmpty()) {
                    int limit = field.trickRoom().value() ? 1807 : 8191;
                    c.need("SOURCE_ACTION_SPEED_WRAP_DOMAIN", computedSpeed(self, a).value().high() <= limit
                            && computedSpeed(foe, f).value().high() <= limit);
                }
            }
        }
        return c.finish();
    }
    public Certificate entryCoverage(Hazards hazards, Known<Integer> maxHp, Profile reserve) {
        if (Set.of("self", "opponent").contains(reserve.actor())) throw new IllegalArgumentException("entry requires an own reserve binding");
        var c = new Check(Operation.ENTRY); c.actor(reserve.actor(), true);
        c.need("OWN_MAX_HP", maxHp.known()); c.need("RESERVE_TYPES", reserve.types().known());
        c.need("OWN_MAX_HP_PROFILE_BINDING", maxHpMatches(maxHp, reserve));
        c.need("COMPLETE_HAZARD_LAYERS", hazards.complete());
        c.need("TOXIC_SPIKES_EXECUTION", hazards.layers().getOrDefault("toxicspikes", 0) == 0);
        return c.finish();
    }
    private boolean reserve(String role) { return actors.containsKey(role) && actors.get(role).observation() == null; }
    public Profile profile(Profile raw, boolean reserve) {
        return new Profile(raw.actor(), raw.level(), raw.maxHp(), raw.stats(), raw.types(),
                supported(raw.actor(), reserve) ? Known.of(true, field.token() + ":limited-conditional-scope") : Known.unknown());
    }
    private String item(String role) { return field.magicRoom().known() && field.magicRoom().value() ? "none" : actors.get(role).item().value(); }
    private String ability(String role) { return actors.get(role).ability().value(); }
    private PublicFieldEvidence.Active active(String role) { Binding b = actors.get(role); return b == null ? null : field.actor(b.observation()); }
    public boolean taunted(String role) { var a = active(role); return a != null && a.volatiles().contains("taunt"); }
    public Known<Boolean> grounded(String role, Profile profile, boolean reserve) {
        if (!role.equals(profile.actor())) throw new IllegalArgumentException("groundedness profile belongs to another actor");
        var c = new Check(Operation.ENTRY); c.actor(role, reserve);
        if (!c.finish().supported() || !profile.types().known()) return Known.unknown();
        return Known.of(!profile.types().value().contains("flying") && !ability(role).equals("levitate"), field.token());
    }
    public Damage damage(String moveId, Profile attacker, Actor a, Profile target, Actor t, Critical critical) {
        bound(attacker, a); bound(target, t);
        var coverage = damageCoverage(moveId, attacker, a, target, t);
        if (!coverage.supported()) return new Damage(Known.unknown(), Known.unknown(), coverage.missing(), coverage.source());
        Move m = move(moveId); if (m == null || m.power() == 0) return Damage.unknown("UNSUPPORTED_DAMAGE_MOVE");
        var targetActive = active(t.id());
        if (targetActive != null && targetActive.volatiles().contains("protect")) return zero("public Protect");
        if (field.terrain().value().equals("psychicterrain") && effectivePriority(moveId, a.id()) > .1) {
            var grounded = target.types().known() ? Known.of(!target.types().value().contains("flying")
                    && !ability(t.id()).equals("levitate"), field.token()) : Known.<Boolean>unknown();
            if (!grounded.known()) return Damage.unknown("TERRAIN_GROUNDEDNESS_UNKNOWN");
            if (grounded.value()) return zero("public Psychic Terrain priority block");
        }
        int n = 1, d = 1;
        if (m.type().equals("fire")) {
            if (field.weather().value().equals("sunnyday")) { n = 3; d = 2; }
            else if (field.weather().value().equals("raindance")) { n = 1; d = 2; }
        }
        // Technician modifies base power before the level/stat formula. Never
        // multiply a final damage interval; the engine rounds at each stage.
        int powerN = ability(a.id()).equals("technician") && m.power() <= 60 ? 3 : 1;
        int powerD = powerN == 3 ? 2 : 1;
        return CobblemonMechanics.damage(moveId, certified(attacker), a, certified(target), t, critical, n, d, powerN, powerD);
    }
    private static Damage zero(String why) {
        return new Damage(Known.unknown(), Known.of(new Interval(0, 0), PublicFieldEvidence.SOURCE + ":" + why), Set.of(), PublicFieldEvidence.SOURCE);
    }
    public Certificate priorityCoverage(String moveId, String role) {
        var c = new Check(Operation.PRIORITY); c.actor(role, reserve(role));
        c.need("SUPPORTED_PRIORITY_MOVE", move(moveId) != null); return c.finish();
    }
    public Known<Double> priority(String moveId, String role) {
        var coverage = priorityCoverage(moveId, role);
        return coverage.supported() ? Known.of(effectivePriority(moveId, role), coverage.source()) : Known.unknown();
    }
    private double effectivePriority(String id, String role) {
        Move m = move(id); double p = m.priority();
        if (m.power() == 0 && ability(role).equals("prankster")) p++;
        if (ability(role).equals("stall") || item(role).equals("laggingtail")) p -= .1;
        return p;
    }
    public Known<IntRange> speed(Profile p, Actor a) {
        bound(p, a);
        if (!speedCoverage(p, a).supported()) return Known.unknown();
        return computedSpeed(p, a);
    }
    private Known<IntRange> computedSpeed(Profile p, Actor a) {
        Known<IntRange> base = CobblemonMechanics.speed(certified(p), a);
        if (!a.status().known() || !a.status().value().equals("none")) return Known.unknown();
        Binding b = actors.get(reserve(a.id()) ? "self" : a.id());
        Known<Boolean> wind = b == null || b.observation() == null ? Known.unknown() : field.tailwind(b.observation().slot());
        if (!base.known() || !wind.known()) return Known.unknown();
        // Scarf and Tailwind chain into one 12-bit modifier, applied after stages.
        int n = item(a.id()).equals("choicescarf") ? 3 : 2; if (wind.value()) n *= 2;
        // Source modify uses round-half-down, not ordinary half-up.
        int low = (base.value().low() * n * 4096 / 2 + 2047) / 4096;
        int high = (base.value().high() * n * 4096 / 2 + 2047) / 4096;
        if (low < 1 || high > 10000) return Known.unknown();
        return Known.of(new IntRange(low, high), field.token());
    }
    private static boolean maxHpMatches(Known<Integer> maxHp, Profile reserve) {
        return maxHp.known() && reserve.maxHp().known() && reserve.maxHp().value().low() == maxHp.value()
                && reserve.maxHp().value().high() == maxHp.value();
    }
    public Order order(String action, String reply, Profile self, Actor a, Profile foe, Actor f) {
        bound(self, a); bound(foe, f);
        if (!orderCoverage(action, reply, self, a, foe, f).supported()) return Order.UNKNOWN;
        if (action.startsWith("switch:")) return Order.SELF_FIRST;
        Move m = action.startsWith("move:") ? move(action.substring(5)) : null; if (m == null) return Order.UNKNOWN;
        double p = effectivePriority(m.id(), a.id()), q = effectivePriority(reply, f.id());
        if (p != q) return p > q ? Order.SELF_FIRST : Order.OPPONENT_FIRST;
        // Both fractional priorities equal: reuse verified bounded speed ordering.
        return CobblemonMechanics.order("move:tackle", "tackle", speed(self, a), speed(foe, f), field.trickRoom(), Known.of(true, field.token()));
    }
    public Entry entry(Hazards hazards, Known<Integer> maxHp, Profile reserve) {
        if (Set.of("self", "opponent").contains(reserve.actor())) throw new IllegalArgumentException("entry requires an own reserve binding");
        var ground = grounded(reserve.actor(), reserve, true);
        var c = new Check(Operation.ENTRY); c.actor(reserve.actor(), true);
        if (!c.finish().supported() || !maxHpMatches(maxHp, reserve)) return CobblemonMechanics.entry(hazards, maxHp, Known.unknown(), Known.unknown(),
                Known.unknown(), Known.unknown(), Known.unknown());
        var rock = reserve.types().known() ? Known.of(effectiveness("rock", reserve.types().value()), field.token()) : Known.<Integer>unknown();
        return CobblemonMechanics.entry(hazards, maxHp, ground, Known.of(item(reserve.actor()).equals("heavydutyboots"), field.token()),
                Known.of(ability(reserve.actor()).equals("magicguard"), field.token()), rock, Known.of(true, field.token()));
    }
}
