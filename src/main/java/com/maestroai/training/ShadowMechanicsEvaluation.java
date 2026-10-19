package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.PublicStateSearchAdapter.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Bounded conditional stay/switch/setup comparison. This is not a resolved battle,
 * a response predictor, or a live policy. Scores are HP resource exchange over the
 * supplied active-target scenarios; no win probability or arbitrary setup bonus.
 */
public final class ShadowMechanicsEvaluation {
    private ShadowMechanicsEvaluation() {}
    public record Utility(double low, double high) {
        public Utility {
            if (!Double.isFinite(low) || !Double.isFinite(high) || low > high)
                throw new IllegalArgumentException("invalid resource exchange interval");
        }
    }
    /** A later own request must be supplied separately, tied to the active actor.
     * Replies are explicit conditional scenarios and must already be publicly revealed.
     */
    public record Turn(String action, String reply, FollowUp request) {
        public Turn { Objects.requireNonNull(action); Objects.requireNonNull(reply); }
    }
    public record Plan(String action, List<List<Turn>> scenarios, String source) {
        public Plan {
            if (action == null || source == null || source.isBlank() || scenarios.isEmpty() || scenarios.size() > 8)
                throw new IllegalArgumentException("invalid scoped plan");
            scenarios = scenarios.stream().map(List::copyOf).toList();
            for (var line : scenarios) {
                if (line.isEmpty() || line.size() > 2 || !line.get(0).action().equals(action)
                        || line.get(0).request() != null)
                    throw new IllegalArgumentException("plan horizon/root request mismatch");
            }
        }
    }
    public record Line(State state, Known<Utility> utility, Set<String> unknown) {
        public Line { unknown = Set.copyOf(unknown); }
    }
    public record Ranking(String action, Known<Utility> utility, List<Line> lines, Set<String> unknown) {
        public Ranking { lines = List.copyOf(lines); unknown = Set.copyOf(unknown); }
    }
    public record Comparison(List<Ranking> rankings, String preferredConditional,
                             String legacyAction, String simpleAction, boolean disagreement,
                             boolean candidateCoverageComplete, String evidence) {
        public Comparison { rankings = List.copyOf(rankings); }
    }
    private record Progress(State state, double dealtLow, double dealtHigh,
                            double lostLow, double lostHigh, Set<String> unknown) {}

    public static Comparison compare(BattleSnapshot snapshot, PublicMechanics mechanics,
                                     Map<String, Profile> profiles, List<Plan> plans,
                                     Known<Boolean> trickRoom, Critical critical, String legacyAction) {
        return compare(snapshot, mechanics, profiles, plans, trickRoom, critical, legacyAction, null);
    }
    public static Comparison compare(BattleSnapshot snapshot, PublicMechanics mechanics,
                                     Map<String, Profile> profiles, List<Plan> plans,
                                     Known<Boolean> trickRoom, Critical critical, String legacyAction, ScopedExecution execution) {
        Objects.requireNonNull(profiles); Objects.requireNonNull(trickRoom);
        // Each executed operation issues its own certificate. Do not require
        // all six unrelated scopes merely to copy a raw immutable actor profile.
        ScopedExecution originalExecution=execution;
        if (execution != null) {
            execution.requireCurrent(snapshot.turn());
            execution=execution.calculationFrame();
        }
        var projection = project(snapshot, mechanics);
        State root = projection.states().get("root");
        Set<String> legal = new TreeSet<>();
        root.legalMoves().forEach(m -> legal.add("move:" + m)); root.legalSwitches().forEach(s -> legal.add("switch:" + s));
        if (legacyAction != null && !legal.contains(legacyAction)) throw new IllegalArgumentException("illegal legacy baseline");
        if (plans.isEmpty() || plans.size() > 256) throw new IllegalArgumentException("invalid candidate budget");
        Set<String> seen = new HashSet<>(); List<Ranking> rankings = new ArrayList<>();
        int horizon = plans.get(0).scenarios().get(0).size();
        var replies = plans.get(0).scenarios().stream().map(s -> s.stream().map(Turn::reply).toList()).toList();
        for (Plan plan : plans) {
            if (!plan.scenarios().stream().map(s -> s.stream().map(Turn::reply).toList()).toList().equals(replies))
                throw new IllegalArgumentException("candidate replies are not paired on identical scenarios");
            if (!seen.add(plan.action()) || !legal.contains(plan.action())) throw new IllegalArgumentException("duplicate/illegal plan");
            List<Line> lines = new ArrayList<>(); Set<String> unknown = new TreeSet<>();
            for (List<Turn> turns : plan.scenarios()) {
                if (turns.size() != horizon) throw new IllegalArgumentException("incomparable horizon lengths");
                List<Progress> branches = List.of(new Progress(root, 0, 0, 0, 0, Set.of()));
                for (int i = 0; i < turns.size(); i++) {
                    List<Progress> expanded = new ArrayList<>();
                    for (Progress branch : branches) expanded.addAll(turn(branch, turns.get(i), i, mechanics, profiles, trickRoom, critical, execution));
                    branches = expanded;
                }
                for (Progress p : branches) {
                    Known<Utility> value = p.unknown().isEmpty()
                            ? Known.of(new Utility(p.dealtLow() - p.lostHigh(), p.dealtHigh() - p.lostLow()), plan.source())
                            : Known.unknown();
                    lines.add(new Line(p.state(), value, p.unknown())); unknown.addAll(p.unknown());
                }
            }
            Known<Utility> bounds = unknown.isEmpty() ? Known.of(new Utility(
                    lines.stream().mapToDouble(l -> l.utility().value().low()).min().orElseThrow(),
                    lines.stream().mapToDouble(l -> l.utility().value().high()).max().orElseThrow()), plan.source()) : Known.unknown();
            rankings.add(new Ranking(plan.action(), bounds, lines, unknown));
        }
        rankings.sort(Comparator.comparing(Ranking::action));
        boolean coverage = seen.equals(legal);
        String preferred = null;
        if (coverage && rankings.stream().allMatch(r -> r.utility().known())) {
            for (Ranking candidate : rankings) {
                if (rankings.stream().filter(r -> r != candidate).allMatch(r -> candidate.utility().value().low() > r.utility().value().high()))
                    preferred = candidate.action();
            }
        }
        // Public nominal-power baseline, not a damage estimate or reimplementation of legacy.
        String simple = root.legalMoves().stream().filter(m -> move(m) != null && move(m).power() > 0)
                .sorted(Comparator.<String>comparingInt(m -> -move(m).power()).thenComparing(m -> m))
                .map(m -> "move:" + m).findFirst().orElseGet(() -> legal.iterator().next());
        if(originalExecution!=null && !originalExecution.sourceCurrent()) {
            // No calculated state or known utility escapes a failed closing
            // receipt. Actor/turn/own-transport checks still run per operation.
            Set<String> expired=Set.of("SOURCE_CHANGED_DURING_COMPARISON");
            var withdrawn=rankings.stream().map(r->new Ranking(r.action(),Known.<Utility>unknown(),
                    r.lines().stream().map(l->new Line(root,Known.<Utility>unknown(),expired)).toList(),expired)).toList();
            return new Comparison(withdrawn,null,legacyAction,simple,false,coverage,
                    "source-receipt-withdrawn;advisoryOnly=true");
        }
        return new Comparison(rankings, preferred, legacyAction, simple,
                preferred != null && legacyAction != null && !preferred.equals(legacyAction), coverage,
                "conditional-supplied-replies;successful-actions;HP-resource-exchange;not-win-probability;advisoryOnly=true");
    }

    private static List<Progress> turn(Progress p, Turn t, int index, PublicMechanics mechanics,
                                       Map<String, Profile> profiles, Known<Boolean> trickRoom, Critical critical, ScopedExecution execution) {
        if (!p.unknown().isEmpty()) return List.of(p);
        if (execution != null && index > 0) return List.of(unknown(p, "FUTURE_FIELD_AND_VOLATILE_DURATION_UNRESOLVED"));
        if (p.state().forcedAction() || p.state().forcedReplacement())
            return List.of(unknown(p, "FORCED_ROOT_ACTION_OUTSIDE_SLICE"));
        if (zero(p.state().self()) || zero(p.state().opponent()))
            return List.of(index == 0 ? unknown(p, "TERMINAL_ROOT_NO_EXECUTABLE_ACTION") : p);
        if (index > 0 && (possibleFaint(p.state().self()) || possibleFaint(p.state().opponent())))
            return List.of(unknown(p, "POSSIBLE_REPLACEMENT_NEXT_TURN"));
        State s = p.state(); List<String> moves = s.legalMoves(), switches = s.legalSwitches();
        if (index > 0) {
            FollowUp f = t.request();
            if (f == null) return List.of(unknown(p, "NEXT_REQUEST_UNKNOWN"));
            if (!f.selfId().equals(s.self().id())) throw new IllegalArgumentException("future request belongs to another actor");
            if (f.forcedReplacement() || f.forcedAction()) return List.of(unknown(p, "FORCED_FUTURE_ACTION_OUTSIDE_SLICE"));
            moves = f.legalMoves(); switches = f.trapped() ? List.of() : f.legalSwitches();
        }
        if (!(t.action().startsWith("move:") && moves.contains(t.action().substring(5))
                || t.action().startsWith("switch:") && switches.contains(t.action().substring(7))))
            throw new IllegalArgumentException("action unavailable in conditional request");
        if (s.ambiguity().contains("TRANSFORMED") || s.ambiguity().contains("ILLUSION"))
            return List.of(unknown(p, "IDENTITY_OR_TRANSFORM_EFFECTS_UNRESOLVED"));
        if (!s.opponent().facts().revealedMoves().contains(t.reply())) return List.of(unknown(p, "UNREVEALED_OPPONENT_REPLY"));
        if (!s.self().hp().known() || !s.opponent().hp().known()) return List.of(unknown(p, "PUBLIC_HP_UNKNOWN"));
        if (!s.self().status().known() || !s.opponent().status().known()
                || !s.self().status().value().equals("none") || !s.opponent().status().value().equals("none"))
            return List.of(unknown(p, "STATUS_EXECUTION_OR_RESIDUAL_EFFECTS_UNRESOLVED"));
        Profile self = profiles.get(s.self().id()), foe = profiles.get(s.opponent().id());
        if (self == null || foe == null || execution == null && (!yes(self.ordinaryEffects()) || !yes(foe.ordinaryEffects())))
            return List.of(unknown(p, "MECHANICS_PROFILE_OR_MODIFIERS_UNKNOWN"));
        boolean switching = t.action().startsWith("switch:");
        Order order = execution != null ? execution.order(t.action(), t.reply(), self, s.self(), foe, s.opponent())
                : order(t.action(), t.reply(), speed(self, s.self()), speed(foe, s.opponent()), trickRoom,
                        Known.of(true, "supplied ordinary action-order scope"));
        if (order == Order.UNKNOWN) return List.of(unknown(p, "ACTION_ORDER_UNKNOWN"));
        if (switching) {
            SwitchTarget target = mechanics.transitions().context().switchTargets().get(t.action().substring(7));
            if (target == null || !target.entryDamage().known() || !target.entrySpeedDelta().known()
                    || !target.status().known() || !target.status().value().equals("none"))
                return List.of(unknown(p, "ENTRY_EFFECTS_UNKNOWN"));
            if (target.facts().fainted() || !target.facts().hpKnown()) return List.of(unknown(p, "RESERVE_HP_UNKNOWN_OR_FAINTED"));
            String id = t.action().substring(7);
            Profile reserve = profiles.get(id);
            if (reserve == null || execution == null && !yes(reserve.ordinaryEffects())) return List.of(unknown(p, "RESERVE_MODIFIERS_UNKNOWN"));
            if (execution != null) {
                var maxHp = reserve.maxHp().known() && reserve.maxHp().value().low() == reserve.maxHp().value().high()
                        ? Known.of(reserve.maxHp().value().low(), reserve.maxHp().source()) : Known.<Integer>unknown();
                var coverage = execution.entryCoverage(s.selfHazards(), maxHp, reserve);
                if (!coverage.supported()) return List.of(unknown(p, String.join(",", coverage.missing())));
            }
            // Toxic Spikes, uncertain Sticky Web, partial layers and arbitrary switch-in
            // abilities are not silently resolved just because HP entry damage is supplied.
            if (!s.selfHazards().complete() || s.selfHazards().layers().getOrDefault("toxicspikes", 0) > 0)
                return List.of(unknown(p, "NON_HP_ENTRY_EFFECTS_OR_PARTIAL_HAZARDS_UNKNOWN"));
            Map<String, Integer> stages = new TreeMap<>(); STATS.forEach(k -> stages.put(k, 0));
            stages.put("spe", target.entrySpeedDelta().value());
            Actor arriving = new Actor(id, target.facts(), Known.of(new Interval(target.facts().hpFraction(), target.facts().hpFraction()), "own reserve HP"),
                    stages, target.status());
            Progress switched = replace(p, state(s, arriving, s.opponent(), Kind.SWITCH, t.action()));
            switched = hurt(switched, true, target.entryDamage().value());
            return List.of(execute(switched, t.reply(), false, profiles, critical, execution));
        }
        if (order == Order.BOTH) return List.of(sequence(p, t, true, profiles, critical, execution), sequence(p, t, false, profiles, critical, execution));
        return List.of(sequence(p, t, order == Order.SELF_FIRST, profiles, critical, execution));
    }
    private static Progress sequence(Progress p, Turn t, boolean selfFirst, Map<String, Profile> profiles, Critical critical, ScopedExecution execution) {
        Move ownMove = move(t.action().substring(5));
        p = replace(p, state(p.state(), p.state().self(), p.state().opponent(),
                ownMove == null ? Kind.UNKNOWN : ownMove.power() == 0 ? Kind.SETUP : Kind.ATTACK, t.action()));
        Progress first = execute(p, selfFirst ? t.action().substring(5) : t.reply(), selfFirst, profiles, critical, execution);
        return execute(first, selfFirst ? t.reply() : t.action().substring(5), !selfFirst, profiles, critical, execution);
    }
    private static Progress execute(Progress p, String moveId, boolean own, Map<String, Profile> profiles, Critical critical, ScopedExecution execution) {
        if (!p.unknown().isEmpty()) return p;
        Actor attacker = own ? p.state().self() : p.state().opponent();
        Actor target = own ? p.state().opponent() : p.state().self();
        if (zero(attacker) || zero(target)) return p; // KO suppresses the slower action, never exposes a reserve
        Move move = move(moveId);
        if (move == null) return unknown(p, "UNSUPPORTED_MOVE:" + moveId);
        if (move.power() == 0) {
            if (move.id().equals("geomancy"))
                return unknown(p, "GEOMANCY_CHARGE_POWER_HERB_AND_CONSUMPTION_UNMODELED");
            if (execution != null) {
                var coverage = execution.setupCoverage(profiles.get(attacker.id()), attacker, moveId);
                if (!coverage.supported()) return unknown(p, String.join(",", coverage.missing()));
            }
            if (execution != null && execution.taunted(attacker.id())) return p; // source BeforeMove rejects setup
            if (possibleFaint(attacker)) return unknown(p, "SETUP_EXECUTION_SURVIVAL_UNKNOWN");
            if (moveId.equals("recover")) {
                Profile profile = profiles.get(attacker.id());
                if (!profile.maxHp().known() || profile.maxHp().value().low() != profile.maxHp().value().high())
                    return unknown(p, "EXACT_RECOVERY_MAX_HP_UNKNOWN");
                int maxHp = profile.maxHp().value().low();
                // Gen9 source uses Math.round(baseMaxhp / 2), including odd HP.
                double recovery = ((maxHp + 1L) / 2) / (double) maxHp;
                Interval hp = attacker.hp().value();
                double low = Math.min(1 - hp.high(), recovery), high = Math.min(1 - hp.low(), recovery);
                Actor healed = actor(attacker, new Interval(Math.min(1, hp.low() + recovery), Math.min(1, hp.high() + recovery)), attacker.stages());
                State changed = state(p.state(), own ? healed : target, own ? target : healed, p.state().actionKind(), p.state().action());
                return new Progress(changed, p.dealtLow() - (own ? 0 : high), p.dealtHigh() - (own ? 0 : low),
                        p.lostLow() - (own ? high : 0), p.lostHigh() - (own ? low : 0), p.unknown());
            }
            Map<String, Integer> stages = new TreeMap<>(attacker.stages());
            for (var e : move.boosts().entrySet()) {
                if (!stages.containsKey(e.getKey())) return unknown(p, "SETUP_STAGE_BASE_UNKNOWN");
                stages.put(e.getKey(), observedStageAfter(stages.get(e.getKey()), e.getValue()));
            }
            Actor boosted = actor(attacker, attacker.hp().value(), stages);
            return replace(p, state(p.state(), own ? boosted : target, own ? target : boosted,
                    p.state().actionKind(), p.state().action()));
        }
        if (!move.damageOnly()) return unknown(p,
                MaestroOwnMoveAudit.unmodeledEffect(moveId));
        Damage damage = execution == null ? damage(moveId, profiles.get(attacker.id()), attacker, profiles.get(target.id()), target, critical)
                : execution.damage(moveId, profiles.get(attacker.id()), attacker, profiles.get(target.id()), target, critical);
        if (!damage.fraction().known()) return unknown(p, String.join(",", damage.unknown()));
        Interval d = damage.fraction().value();
        if (possibleFaint(attacker)) d = new Interval(0, d.high()); // bound conditional nonexecution without an invented probability
        return hurt(p, !own, d);
    }
    private static Progress hurt(Progress p, boolean self, Interval damage) {
        Actor a = self ? p.state().self() : p.state().opponent(); Interval hp = a.hp().value();
        double low = Math.min(hp.low(), damage.low()), high = Math.min(hp.high(), damage.high());
        Actor changed = actor(a, remainingHp(hp, damage), a.stages());
        State s = state(p.state(), self ? changed : p.state().self(), self ? p.state().opponent() : changed,
                p.state().actionKind(), p.state().action());
        return new Progress(s, p.dealtLow() + (self ? 0 : low), p.dealtHigh() + (self ? 0 : high),
                p.lostLow() + (self ? low : 0), p.lostHigh() + (self ? high : 0), p.unknown());
    }
    private static Actor actor(Actor a, Interval hp, Map<String, Integer> stages) {
        var f = a.facts(); double point = hp.low() == hp.high() ? hp.low() : BattleSnapshot.UNKNOWN_HP_FRACTION;
        var facts = new BattleSnapshot.Combatant(f.species(), point, f.types(), stages,
                f.revealedMoves(), f.revealedItem(), f.revealedAbility(), hp.high() == 0);
        return new Actor(a.id(), facts, Known.of(hp, SOURCE + ":conditional-action-sequence"), stages, a.status());
    }
    private static State state(State s, Actor self, Actor foe, Kind kind, String action) {
        Set<String> unresolved = new TreeSet<>(s.unresolved());
        unresolved.add("CONDITIONAL_SUPPLIED_REPLY_AND_SUCCESS"); unresolved.add("RESIDUALS_AND_REPLACEMENTS_UNMODELED");
        return new State(self, foe, s.selfHazards(), s.opponentHazards(), s.ambiguity(), unresolved,
                kind, action, List.of(), List.of(), false, false, false, false);
    }
    private static Progress replace(Progress p, State s) {
        return new Progress(s, p.dealtLow(), p.dealtHigh(), p.lostLow(), p.lostHigh(), p.unknown());
    }
    private static Progress unknown(Progress p, String reason) {
        Set<String> unknown = new TreeSet<>(p.unknown()); unknown.add(reason);
        return new Progress(p.state(), p.dealtLow(), p.dealtHigh(), p.lostLow(), p.lostHigh(), Set.copyOf(unknown));
    }
    private static boolean zero(Actor a) { return a.hp().known() && a.hp().value().high() == 0; }
    private static boolean possibleFaint(Actor a) { return a.hp().known() && a.hp().value().low() == 0; }
}
