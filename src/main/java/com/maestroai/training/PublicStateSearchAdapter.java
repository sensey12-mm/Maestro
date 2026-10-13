package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Shadow-only, conditional own-action transitions over explicitly supplied public evidence. */
public final class PublicStateSearchAdapter {
    private PublicStateSearchAdapter() {}
    private static final int PROJECTED_ACTION_LIMIT = 256;

    public record Interval(double low, double high) {
        public Interval {
            if (!Double.isFinite(low) || !Double.isFinite(high) || low < 0 || high < low)
                throw new IllegalArgumentException("invalid damage interval");
        }
        public double midpoint() { return low + (high - low) / 2.0; }
        public double width() { return high - low; }
    }
    public static Interval remainingHp(double publicHp, Interval damage) {
        return remainingHp(new Interval(publicHp, publicHp), damage);
    }
    public static Interval remainingHp(Interval hp, Interval damage) {
        if (hp.high() > 1) throw new IllegalArgumentException("invalid public HP");
        return new Interval(Math.max(0, hp.low() - damage.high()), Math.max(0, hp.high() - damage.low()));
    }
    public static int observedStageAfter(int stage, int delta) {
        if (stage < -6 || stage > 6 || delta < -12 || delta > 12)
            throw new IllegalArgumentException("invalid observed stat stage");
        return Math.max(-6, Math.min(6, stage + delta));
    }

    public record PublicMechanics(Map<String, Interval> calibratedDamage,
                                  Map<String, Integer> observedBoosts, Set<String> observedHazards,
                                  boolean forcedAction, boolean forcedReplacement, boolean trapped,
                                  double unsupportedUncertainty, Evidence transitions) {
        public PublicMechanics {
            calibratedDamage = sorted(calibratedDamage); observedBoosts = stages(observedBoosts);
            observedHazards = sortedSet(observedHazards); Objects.requireNonNull(transitions);
            if (!Double.isFinite(unsupportedUncertainty) || unsupportedUncertainty < 0)
                throw new IllegalArgumentException("invalid uncertainty");
        }
        /** Compatibility inputs are root-only. They do not calibrate follow-ups or responses. */
        public PublicMechanics(Map<String, Interval> damage, Map<String, Integer> boosts,
                               Set<String> hazards, boolean forced, boolean replacement,
                               boolean trapped, double uncertainty) {
            this(damage, boosts, hazards, forced, replacement, trapped, uncertainty, Evidence.unknown());
        }
    }
    public record Projection(Map<String, ShadowSearchEngine.Position> positions,
                             boolean calibrated, String evidence, Map<String, State> states) {
        public Projection { positions = Map.copyOf(positions); states = Map.copyOf(states); }
    }

    /**
     * Each edge has its own conditional state, including UNKNOWN fields.
     * An opponent-unknown outcome of weight 1 is a placeholder, not a response
     * distribution. No future action is advertised without separate actor-bound
     * continuation evidence. Numerical damage utilities are conditional proxies,
     * not expected battle value; full calibration remains false.
     */
    public static Projection project(BattleSnapshot snapshot, PublicMechanics mechanics) {
        Objects.requireNonNull(snapshot); Objects.requireNonNull(mechanics);
        var context = mechanics.transitions().context();
        List<String> moves = mechanics.forcedReplacement() ? List.of() : names(snapshot.legalMoves());
        checkForced(mechanics.forcedAction(), mechanics.forcedReplacement(), moves);
        List<String> switches = switches(snapshot.legalSwitches(), mechanics.forcedAction(),
                mechanics.forcedReplacement(), mechanics.trapped());
        if (moves.isEmpty() && switches.isEmpty()) throw new IllegalArgumentException("no publicly legal actions");
        validateRootEvidence(moves, switches, mechanics.transitions());
        Map<String, Integer> opponentStages = new TreeMap<>(snapshot.opponent().knownBoosts());
        mechanics.observedBoosts().forEach((stat, stage) -> {
            if (opponentStages.containsKey(stat) && !opponentStages.get(stat).equals(stage))
                throw new IllegalArgumentException("conflicting public stage evidence");
            opponentStages.put(stat, stage);
        });
        State root = new State(actor("self", snapshot.self(), context.selfHp(),
                snapshot.self().knownBoosts(), context.selfStatus()),
                actor("opponent", snapshot.opponent(), context.opponentHp(), opponentStages, context.opponentStatus()),
                context.selfHazards(), context.opponentHazards(), context.ambiguity(),
                Set.of("OPPONENT_RESPONSE_UNKNOWN", "TURN_ORDER_UNKNOWN"), Kind.UNKNOWN, "root",
                moves, switches, true, mechanics.forcedAction(), mechanics.forcedReplacement(), mechanics.trapped());
        Builder builder = new Builder(mechanics);
        builder.states.put("root", root);
        List<ShadowSearchEngine.Branch> branches = builder.branches(root, "root", mechanics.transitions().effects(),
                mechanics.transitions().followUps(), 2);
        builder.positions.put("root", new ShadowSearchEngine.Position("root", branches, 0));
        return new Projection(builder.positions, false,
                "conditional-public-transitions;opponent-response=UNKNOWN;not-outcome-calibrated", builder.states);
    }

    private static List<String> switches(List<String> values, boolean forced, boolean replacement, boolean trapped) {
        return (forced || trapped) && !replacement ? List.of() : names(values);
    }
    private static void validateRootEvidence(List<String> moves, List<String> switches, Evidence evidence) {
        Set<String> actions = new HashSet<>();
        moves.forEach(m -> actions.add("move:" + m)); switches.forEach(s -> actions.add("switch:" + s));
        if (!actions.containsAll(evidence.effects().keySet()) || !actions.containsAll(evidence.followUps().keySet()))
            throw new IllegalArgumentException("evidence for a nonlegal root action");
    }
    private static Actor actor(String id, BattleSnapshot.Combatant facts, Known<Interval> supplied,
                               Map<String, Integer> stages, Known<String> status) {
        if (supplied.known() && facts.hpKnown()
                && (facts.hpFraction() < supplied.value().low() || facts.hpFraction() > supplied.value().high()))
            throw new IllegalArgumentException("conflicting public HP evidence");
        Known<Interval> hp = supplied.known() ? supplied : facts.hpKnown()
                ? Known.of(new Interval(facts.hpFraction(), facts.hpFraction()), "snapshot public/own HP") : Known.unknown();
        return new Actor(id, facts, hp, stages, status);
    }
    private static Actor changed(Actor old, Known<Interval> hp, Map<String, Integer> stages, Known<String> status) {
        // Combatant's point HP is UNKNOWN when the conditional interval is not a point.
        double point = hp.known() && hp.value().low() == hp.value().high()
                ? hp.value().low() : BattleSnapshot.UNKNOWN_HP_FRACTION;
        var f = old.facts();
        var facts = new BattleSnapshot.Combatant(f.species(), point, f.types(), stages,
                f.revealedMoves(), f.revealedItem(), f.revealedAbility(), point == 0);
        return new Actor(old.id(), facts, hp, stages, status);
    }

    private record Applied(State state, double utility, double uncertainty) {}
    private static final class Builder {
        final PublicMechanics mechanics;
        final Map<String, State> states = new LinkedHashMap<>();
        final Map<String, ShadowSearchEngine.Position> positions = new LinkedHashMap<>();
        int projectedActions;
        Builder(PublicMechanics mechanics) { this.mechanics = mechanics; }

        List<ShadowSearchEngine.Branch> branches(State state, String path, Map<String, Effect> effects,
                                               Map<String, FollowUp> followUps, int depth) {
            List<ShadowSearchEngine.Branch> result = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            state.legalMoves().forEach(m -> ids.add("move:" + m)); state.legalSwitches().forEach(s -> ids.add("switch:" + s));
            Collections.sort(ids);
            for (String id : ids) {
                if (++projectedActions > PROJECTED_ACTION_LIMIT)
                    throw new IllegalArgumentException("public transition graph budget exceeded");
                boolean switching = id.startsWith("switch:");
                String action = id.substring(id.indexOf(':') + 1);
                Effect effect = effects.get(id);
                if (effect == null) {
                    Kind intent = switching ? Kind.SWITCH : mechanics.transitions().context().moveKinds().getOrDefault(action, Kind.UNKNOWN);
                    effect = Effect.unknown(intent);
                    // Old calibration map is deliberately only valid at the observed root.
                    Interval damage = path.equals("root") ? mechanics.calibratedDamage().get(action) : null;
                    if (!switching && damage != null) effect = new Effect(Kind.ATTACK,
                            Known.of(damage, "caller-supplied root calibration"), Map.of(), Known.unknown(), null,
                            "caller-supplied root calibration");
                }
                if (switching && effect.kind() != Kind.SWITCH || !switching && effect.kind() == Kind.SWITCH)
                    throw new IllegalArgumentException("action/effect kind mismatch");
                if (effect.kind() == Kind.FORCED_CONTINUATION && !state.forcedAction())
                    throw new IllegalArgumentException("forced effect without a forced request");
                Applied applied = apply(state, id, effect);
                FollowUp follow = depth > 1 ? followUps.get(id) : null;
                State next = continuation(applied.state(), follow);
                // Path + full public state/uncertainty isolates action-dependent mechanics.
                String nextPath = path + "/" + id.length() + ":" + id;
                String key = key(nextPath, next, applied.uncertainty());
                states.put(key, next);
                List<ShadowSearchEngine.Branch> children = follow != null && next.nextRequestKnown()
                        ? branches(next, nextPath, follow.effects(), Map.of(), depth - 1) : List.of();
                positions.put(key, new ShadowSearchEngine.Position(key, children, 0));
                result.add(new ShadowSearchEngine.Branch(switching ? "switch" : "move", action,
                        List.of(new ShadowSearchEngine.Outcome("opponent-unknown", 1,
                                applied.utility(), applied.uncertainty(), key))));
            }
            return List.copyOf(result);
        }

        Applied apply(State old, String id, Effect effect) {
            Actor self = old.self(), foe = old.opponent();
            Set<String> unresolved = new TreeSet<>(old.unresolved());
            unresolved.add("CONDITIONAL_ON_SUCCESSFUL_EXECUTION");
            double uncertainty = mechanics.unsupportedUncertainty();
            double utility = 0;
            boolean switching = id.startsWith("switch:");
            Kind kind = switching ? Kind.SWITCH : old.forcedAction() ? Kind.FORCED_CONTINUATION : effect.kind();
            if (switching) {
                String destination = id.substring(7);
                SwitchTarget target = mechanics.transitions().context().switchTargets().get(destination);
                if (target == null) {
                    var unknown = new BattleSnapshot.Combatant("UNKNOWN", BattleSnapshot.UNKNOWN_HP_FRACTION,
                            List.of(), Map.of(), List.of(), null, null, false);
                    self = actor(destination, unknown, Known.unknown(), Map.of(), Known.unknown());
                    unresolved.add("SWITCH_DESTINATION_UNKNOWN");
                } else {
                    if (target.facts().fainted()) throw new IllegalArgumentException("cannot switch to a fainted reserve");
                    Map<String, Integer> neutral = new TreeMap<>(); STATS.forEach(s -> neutral.put(s, 0));
                    if (target.entrySpeedDelta().known()) neutral.put("spe", target.entrySpeedDelta().value());
                    self = actor(destination, target.facts(), Known.unknown(), neutral, target.status());
                    Known<Interval> hp = Known.unknown();
                    if (target.entryDamage().known() && self.hp().known()) {
                        hp = Known.of(remainingHp(self.hp().value(), target.entryDamage().value()), target.entryDamage().source());
                        uncertainty += target.entryDamage().value().width();
                    } else unresolved.add("ENTRY_DAMAGE_UNKNOWN");
                    self = changed(self, hp, neutral, target.status());
                }
                unresolved.add("SWITCH_IN_EFFECTS_UNKNOWN");
                if (!old.selfHazards().complete() || !old.selfHazards().layers().isEmpty()
                        || !mechanics.observedHazards().isEmpty()) uncertainty += 1;
            } else {
                if (effect.targetDamage().known()) {
                    if (foe.hp().known()) {
                        Interval damage = effect.targetDamage().value(), hp = foe.hp().value();
                        utility = (Math.min(hp.low(), damage.low()) + Math.min(hp.high(), damage.high())) / 2;
                        foe = changed(foe, Known.of(remainingHp(hp, damage), effect.targetDamage().source()),
                                foe.stages(), foe.status());
                    } else unresolved.add("TARGET_HP_UNKNOWN");
                    uncertainty += effect.targetDamage().value().width();
                } else if (effect.kind() == Kind.ATTACK || effect.kind() == Kind.FORCED_CONTINUATION || effect.kind() == Kind.UNKNOWN) {
                    foe = changed(foe, Known.unknown(), foe.stages(), foe.status());
                    unresolved.add("DAMAGE_UNKNOWN");
                }
                if (!effect.selfStageDeltas().isEmpty()) {
                    Map<String, Integer> boosts = new TreeMap<>(self.stages());
                    for (var delta : effect.selfStageDeltas().entrySet()) {
                        Integer before = boosts.get(delta.getKey());
                        if (before == null) unresolved.add("STAGE_BASE_UNKNOWN:" + delta.getKey());
                        else boosts.put(delta.getKey(), observedStageAfter(before, delta.getValue()));
                    }
                    self = changed(self, self.hp(), boosts, self.status());
                } else if ((kind == Kind.SETUP || kind == Kind.UNKNOWN) && effect.source() == null) {
                    self = changed(self, self.hp(), Map.of(), self.status());
                    unresolved.add("SETUP_EFFECT_UNKNOWN");
                }
                if (!effect.completeOwnEffect()) {
                    unresolved.add("OWN_ACTION_EFFECTS_INCOMPLETE");
                    Map<String, Integer> resolvedStages = new TreeMap<>();
                    for (String stat : effect.selfStageDeltas().keySet())
                        if (self.stages().containsKey(stat)) resolvedStages.put(stat, self.stages().get(stat));
                    self = changed(self, Known.unknown(), resolvedStages, Known.unknown());
                    foe = changed(foe, effect.targetDamage().known() ? foe.hp() : Known.unknown(), Map.of(), Known.unknown());
                }
                if (effect.targetStatusAfter().known())
                    foe = changed(foe, foe.hp(), foe.stages(), effect.targetStatusAfter());
            }
            Hazards ownHazards = !switching && !effect.completeOwnEffect() ? Hazards.unknown() : old.selfHazards();
            Hazards foeHazards = effect.targetHazardsAfter() != null ? effect.targetHazardsAfter()
                    : !switching && !effect.completeOwnEffect() ? Hazards.unknown() : old.opponentHazards();
            State next = new State(self, foe, ownHazards, foeHazards, old.ambiguity(), unresolved,
                    kind, id, List.of(), List.of(), false, false, false, false);
            return new Applied(next, utility, uncertainty);
        }

        State continuation(State state, FollowUp follow) {
            Set<String> unresolved = new TreeSet<>(state.unresolved());
            if (follow == null) {
                unresolved.add("NEXT_REQUEST_UNKNOWN");
                return request(state, List.of(), List.of(), false, false, false, false, unresolved);
            }
            if (!follow.selfId().equals(state.self().id()))
                throw new IllegalArgumentException("continuation belongs to another active actor");
            if (zeroHp(state.self()) && !follow.forcedReplacement() || zeroHp(state.opponent())) {
                unresolved.add("REPLACEMENT_REQUEST_UNKNOWN");
                return request(state, List.of(), List.of(), false, false, false, false, unresolved);
            }
            unresolved.add("CONDITIONAL_NEXT_REQUEST:" + follow.source());
            List<String> moves = follow.forcedReplacement() ? List.of() : follow.legalMoves();
            return request(state, moves, switches(follow.legalSwitches(), follow.forcedAction(),
                    follow.forcedReplacement(), follow.trapped()), true, follow.forcedAction(),
                    follow.forcedReplacement(), follow.trapped(), unresolved);
        }
    }
    private static boolean zeroHp(Actor actor) { return actor.hp().known() && actor.hp().value().high() == 0; }
    private static State request(State s, List<String> moves, List<String> switches, boolean known,
                                 boolean forced, boolean replacement, boolean trapped, Set<String> unresolved) {
        return new State(s.self(), s.opponent(), s.selfHazards(), s.opponentHazards(), s.ambiguity(), unresolved,
                s.actionKind(), s.action(), moves, switches, known, forced, replacement, trapped);
    }
    private static String key(String path, State state, double uncertainty) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            text(out, "phase329-transition-v1"); text(out, path); out.writeDouble(uncertainty);
            encodeActor(out, state.self()); encodeActor(out, state.opponent());
            encodeHazards(out, state.selfHazards()); encodeHazards(out, state.opponentHazards());
            strings(out, state.ambiguity()); strings(out, state.unresolved());
            text(out, state.actionKind().name()); text(out, state.action());
            strings(out, state.legalMoves()); strings(out, state.legalSwitches());
            out.writeBoolean(state.nextRequestKnown()); out.writeBoolean(state.forcedAction());
            out.writeBoolean(state.forcedReplacement()); out.writeBoolean(state.trapped());
            return "public:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(bytes.toByteArray()));
        } catch (NoSuchAlgorithmException | IOException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void encodeActor(DataOutputStream out, Actor a) throws IOException {
        var f = a.facts(); text(out, a.id()); text(out, f.species());
        out.writeDouble(f.hpFraction()); out.writeBoolean(f.fainted());
        strings(out, f.types()); ints(out, f.knownBoosts()); strings(out, f.revealedMoves());
        text(out, f.revealedItem()); text(out, f.revealedAbility());
        out.writeBoolean(a.hp().known());
        if (a.hp().known()) { out.writeDouble(a.hp().value().low()); out.writeDouble(a.hp().value().high()); }
        text(out, a.hp().source()); ints(out, a.stages());
        text(out, a.status().value()); text(out, a.status().source());
    }
    private static void encodeHazards(DataOutputStream out, Hazards h) throws IOException {
        ints(out, h.layers()); out.writeBoolean(h.complete()); text(out, h.source());
    }
    private static void ints(DataOutputStream out, Map<String, Integer> values) throws IOException {
        out.writeInt(values.size());
        for (var e : new TreeMap<>(values).entrySet()) { text(out, e.getKey()); out.writeInt(e.getValue()); }
    }
    private static void strings(DataOutputStream out, Collection<String> values) throws IOException {
        out.writeInt(values.size());
        for (String value : values.stream().sorted().toList()) text(out, value);
    }
    private static void text(DataOutputStream out, String value) throws IOException {
        if (value == null) { out.writeInt(-1); return; }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
}
