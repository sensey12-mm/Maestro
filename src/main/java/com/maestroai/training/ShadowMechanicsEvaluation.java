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
                             double lostLow, double lostHigh, Set<String> unknown, boolean skipNext) {}

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
                List<Progress> branches = List.of(new Progress(root, 0, 0, 0, 0, Set.of(),false));
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
            return executeVariants(switched,t.reply(),false,profiles,critical,execution,mechanics,s.legalSwitches());
        }
        if(order==Order.BOTH){
            var all=new ArrayList<Progress>();
            all.addAll(sequence(p,t,true,profiles,critical,execution,mechanics,s.legalSwitches()));
            all.addAll(sequence(p,t,false,profiles,critical,execution,mechanics,s.legalSwitches()));
            return all;
        }
        return sequence(p,t,order==Order.SELF_FIRST,profiles,critical,execution,mechanics,s.legalSwitches());
    }
    private static List<Progress> sequence(Progress p,Turn turn,boolean selfFirst,
            Map<String,Profile> profiles,Critical critical,ScopedExecution execution,
            PublicMechanics mechanics,List<String> sourceLegalSwitches) {
        Move move=move(turn.action().substring(5));
        p=replace(p,state(p.state(),p.state().self(),p.state().opponent(),
                move==null?Kind.UNKNOWN:move.power()==0?Kind.SETUP:Kind.ATTACK,turn.action()));
        var variants=new ArrayList<Progress>();
        for(var first:executeVariants(p,selfFirst?turn.action().substring(5):turn.reply(),
                selfFirst,profiles,critical,execution,mechanics,sourceLegalSwitches))
            variants.addAll(executeVariants(first,selfFirst?turn.reply():turn.action().substring(5),
                    !selfFirst,profiles,critical,execution,mechanics,sourceLegalSwitches));
        return variants;
    }
    private static List<Progress> executeVariants(Progress p,String id,boolean own,
            Map<String,Profile> profiles,Critical critical,ScopedExecution execution,
            PublicMechanics mechanics,List<String> sourceLegalSwitches) {
        if(p.skipNext())return List.of(new Progress(p.state(),p.dealtLow(),p.dealtHigh(),
                p.lostLow(),p.lostHigh(),p.unknown(),false));
        Actor attacker=own?p.state().self():p.state().opponent();
        if(attacker.status().known()&&Set.of("par","frz").contains(attacker.status().value())){
            // Conditional paralysis/freeze may prevent the slower action.
            Actor recovered=attacker.status().value().equals("frz")?withStatus(attacker,"none"):attacker;
            var eligible=replace(p,state(p.state(),own?recovered:p.state().self(),
                    own?p.state().opponent():recovered,p.state().actionKind(),p.state().action()));
            return List.of(p,executeBase(eligible,id,own,profiles,critical,execution,mechanics,sourceLegalSwitches));
        }
        Progress primary=executeBase(p,id,own,profiles,critical,execution,mechanics,sourceLegalSwitches);
        if(!primary.unknown().isEmpty())return List.of(primary);
        var effect=SourceMoveEffects.SECONDARIES.get(id);
        if(effect==null)return List.of(primary);
        Actor after=own?primary.state().opponent():primary.state().self();
        Actor before=own?p.state().opponent():p.state().self();
        if(after.hp().value().high()==0||after.hp().value().equals(before.hp().value()))
            return List.of(primary);
        if(!effect.status().equals("none")&&!statusCanProc(after,effect.status(),profiles))
            return List.of(primary);
        Actor modified=after;
        if(!effect.status().equals("none"))modified=withStatus(modified,effect.status());
        if(!effect.targetStages().isEmpty()){
            modified=stageEffect(modified,effect.targetStages());
            if(modified==null)return List.of(unknown(primary,"SECONDARY_STAGE_DOMAIN_UNKNOWN"));
        }
        var state=state(primary.state(),own?primary.state().self():modified,
                own?modified:primary.state().opponent(),primary.state().actionKind(),primary.state().action());
        var proc=new Progress(state,primary.dealtLow(),primary.dealtHigh(),primary.lostLow(),
                primary.lostHigh(),primary.unknown(),effect.flinch());
        return effect.chance()==100?List.of(proc):List.of(primary,proc);
    }
    private static boolean statusCanProc(Actor actor,String change,Map<String,Profile> profiles) {
        if(!actor.status().known()||!actor.status().value().equals("none"))return false;
        Profile p=profiles.get(actor.id());
        if(p==null||!p.types().known())return false;
        return !p.types().value().contains(switch(change){
            case "brn"->"fire";case "frz"->"ice";case "par"->"electric";
            default->throw new IllegalArgumentException("unsupported condition");});
    }
    private static Actor withStatus(Actor a,String status){
        return new Actor(a.id(),a.facts(),a.hp(),a.stages(),
                Known.of(status,SOURCE+":conditional-secondary"));
    }
    private static Progress executeBase(Progress p,String moveId,boolean own,
            Map<String,Profile> profiles,Critical critical,ScopedExecution execution,
            PublicMechanics mechanics,List<String> sourceLegalSwitches) {
        if (!p.unknown().isEmpty()) return p;
        Actor attacker = own ? p.state().self() : p.state().opponent();
        Actor target = own ? p.state().opponent() : p.state().self();
        if (zero(attacker) || zero(target)) return p; // KO suppresses the slower action, never exposes a reserve
        Move move = move(moveId);
        if (move == null) return unknown(p, "UNSUPPORTED_MOVE:" + moveId);
        if (move.power() == 0) {
            if(move.id().equals("geomancy") && (execution==null || !execution.activePowerHerb(attacker.id())))
                return unknown(p, "GEOMANCY_POWER_HERB_OR_CHARGE_DOMAIN_UNKNOWN");
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
                        p.lostLow() - (own ? high : 0), p.lostHigh() - (own ? low : 0), p.unknown(),p.skipNext());
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
        if (!move.damageOnly() && !Set.of("drainingkiss","closecombat","rocktomb","spectralthief","uturn").contains(moveId)
                && !SourceMoveEffects.modeledSecondary(moveId))
            return unknown(p,MaestroOwnMoveAudit.unmodeledEffect(moveId));
        if(moveId.equals("spectralthief")) {
            if(!profiles.get(target.id()).types().known())
                return unknown(p,"SPECTRAL_THIEF_TARGET_TYPE_UNKNOWN");
            if(effectiveness("ghost",profiles.get(target.id()).types().value())==Integer.MIN_VALUE)
                return hurt(p,!own,new Interval(0,0));
            if(!target.stages().keySet().containsAll(Set.of("atk","def","spa","spd","spe")))
                return unknown(p,"SPECTRAL_THIEF_PUBLIC_STAGE_DOMAIN_UNKNOWN");
            var bonus=new TreeMap<String,Integer>();
            var removed=new TreeMap<String,Integer>();
            target.stages().forEach((stat,value)->{
                if(value>0){bonus.put(stat,value);removed.put(stat,-value);}
            });
            Actor thief=stageEffect(attacker,bonus),victim=stageEffect(target,removed);
            if(thief==null||victim==null)return unknown(p,"SPECTRAL_THIEF_STAGES_UNKNOWN");
            p=replace(p,state(p.state(),own?thief:victim,own?victim:thief,
                    p.state().actionKind(),p.state().action()));
            attacker=own?p.state().self():p.state().opponent();
            target=own?p.state().opponent():p.state().self();
        }
        Damage damage = execution == null ? damage(moveId, profiles.get(attacker.id()), attacker, profiles.get(target.id()), target, critical)
                : execution.damage(moveId, profiles.get(attacker.id()), attacker, profiles.get(target.id()), target, critical);
        if (!damage.fraction().known()) return unknown(p, String.join(",", damage.unknown()));
        Interval d = damage.fraction().value();
        if (possibleFaint(attacker)) d = new Interval(0, d.high()); // bound conditional nonexecution without an invented probability
        Progress result=hurt(p,!own,d);
        if(execution!=null&&execution.currentItem(attacker.id()).equals("lifeorb")
                &&!execution.currentAbility(attacker.id()).equals("magicguard")
                &&damage.points().known()&&damage.points().value().high()>0) {
            Profile holder=profiles.get(attacker.id());
            if(!holder.maxHp().known()||holder.maxHp().value().low()!=holder.maxHp().value().high())
                return unknown(result,"LIFE_ORB_BASE_MAX_HP_UNKNOWN");
            int max=holder.maxHp().value().low();
            double chip=Math.max(1,max/10)/(double)max;
            result=hurt(result,own,new Interval(chip,chip));
        }
        if(execution!=null&&execution.currentAbility(attacker.id()).equals("asonespectrier")
                &&(own?result.state().opponent():result.state().self()).hp().value().high()==0) {
            Actor acted=own?result.state().self():result.state().opponent();
            Actor boosted=stageEffect(acted,Map.of("spa",1));
            if(boosted==null)return unknown(result,"AS_ONE_POST_KO_SPA_STAGE_UNKNOWN");
            result=replace(result,state(result.state(),own?boosted:result.state().self(),
                    own?result.state().opponent():boosted,result.state().actionKind(),result.state().action()));
        }
        if(moveId.equals("uturn")&&!zero(own?result.state().opponent():result.state().self())){
            if(!own)return unknown(result,"OPPONENT_U_TURN_PARTY_HIDDEN");
            if(d.high()==0)return result; // immunity or Protect prevents pivot
            result=pivotToSoleCertifiedReserve(result,mechanics,sourceLegalSwitches,profiles,execution);
        }
        if(moveId.equals("drainingkiss")) {
            Profile me=profiles.get(attacker.id()), them=profiles.get(target.id());
            if(!me.maxHp().known()||!them.maxHp().known()||!damage.points().known()
                    ||me.maxHp().value().low()!=me.maxHp().value().high())
                return unknown(result,"DRAIN_OWN_EXACT_OR_OPPONENT_BOUNDED_HP_UNKNOWN");
            int ownMax=me.maxHp().value().low();
            // Opponent max HP is a justified PUBLIC INTERVAL, never an exact
            // hidden IV/EV leak. Bound actual removed HP on both extremes.
            int lowerOpponentHp=(int)Math.floor(target.hp().value().low()*them.maxHp().value().low());
            int upperOpponentHp=(int)Math.ceil(target.hp().value().high()*them.maxHp().value().high());
            int actualMin=Math.min(damage.points().value().low(),lowerOpponentHp);
            int actualMax=Math.min(damage.points().value().high(),upperOpponentHp);
            // ceil(3 * actual HP removed / 4), bounded by both HP domains.
            double healedMin=actualMin==0?0:((3L*actualMin+3)/4)/(double)ownMax;
            double healedMax=actualMax==0?0:((3L*actualMax+3)/4)/(double)ownMax;
            result=heal(result,own,new Interval(healedMin,healedMax));
        }
        if(moveId.equals("closecombat")) {
            boolean affectedOwn=own;
            Actor actor=affectedOwn?result.state().self():result.state().opponent();
            Actor modified=stageEffect(actor,Map.of("def",-1,"spd",-1));
            if(modified==null)return unknown(result,"ON_HIT_STAGE_DOMAIN_UNKNOWN");
            result=replace(result,state(result.state(),affectedOwn?modified:result.state().self(),
                    affectedOwn?result.state().opponent():modified,result.state().actionKind(),result.state().action()));
        }
        return result;
    }
    /** Only the sole legal, current owner-side reserve can be a deterministic
     * conditional U-turn target. No invented opponent party or chosen teammate.
     */
    private static Progress pivotToSoleCertifiedReserve(Progress p,PublicMechanics mechanics,
            List<String> choices,Map<String,Profile> profiles,ScopedExecution execution){
        if(choices.size()!=1)return unknown(p,"U_TURN_REPLACEMENT_CHOICE_NOT_DETERMINED");
        String id=choices.getFirst();
        SwitchTarget entry=mechanics.transitions().context().switchTargets().get(id);
        if(entry==null||!entry.entryDamage().known()||!entry.entrySpeedDelta().known()
                ||!entry.status().known()||!entry.status().value().equals("none")
                ||entry.facts().fainted()||!entry.facts().hpKnown())
            return unknown(p,"U_TURN_REPLACEMENT_ENTRY_UNKNOWN");
        Profile reserve=profiles.get(id);
        if(reserve==null||execution==null&&!yes(reserve.ordinaryEffects()))
            return unknown(p,"U_TURN_RESERVE_PROFILE_UNKNOWN");
        if(execution!=null){
            var max=reserve.maxHp().known()&&reserve.maxHp().value().low()==reserve.maxHp().value().high()
                    ?Known.of(reserve.maxHp().value().low(),reserve.maxHp().source())
                    :Known.<Integer>unknown();
            var cert=execution.entryCoverage(p.state().selfHazards(),max,reserve);
            if(!cert.supported())return unknown(p,String.join(",",cert.missing()));
        }
        if(!p.state().selfHazards().complete()
                ||p.state().selfHazards().layers().getOrDefault("toxicspikes",0)>0)
            return unknown(p,"U_TURN_NON_HP_ENTRY_EFFECTS_UNKNOWN");
        Map<String,Integer> nextStages=new TreeMap<>();STATS.forEach(k->nextStages.put(k,0));
        nextStages.put("spe",entry.entrySpeedDelta().value());
        Actor arriving=new Actor(id,entry.facts(),Known.of(new Interval(
                entry.facts().hpFraction(),entry.facts().hpFraction()),"owner-only current reserve HP"),
                nextStages,entry.status());
        Progress swapped=replace(p,state(p.state(),arriving,p.state().opponent(),
                Kind.FORCED_CONTINUATION,p.state().action()));
        return hurt(swapped,true,entry.entryDamage().value());
    }
    private static Actor stageEffect(Actor actor,Map<String,Integer> changes){
        var stages=new TreeMap<>(actor.stages());
        for(var e:changes.entrySet()){
            if(!stages.containsKey(e.getKey()))return null;
            stages.put(e.getKey(),observedStageAfter(stages.get(e.getKey()),e.getValue()));
        }
        return actor(actor,actor.hp().value(),stages);
    }
    private static Progress heal(Progress p,boolean own,Interval healing){
        Actor self=own?p.state().self():p.state().opponent();
        Interval before=self.hp().value();
        double lo=Math.min(1-before.high(),healing.low()),hi=Math.min(1-before.low(),healing.high());
        Actor after=actor(self,new Interval(Math.min(1,before.low()+healing.low()),
                Math.min(1,before.high()+healing.high())),self.stages());
        State state=state(p.state(),own?after:p.state().self(),own?p.state().opponent():after,
                p.state().actionKind(),p.state().action());
        return new Progress(state,p.dealtLow()-(own?0:hi),p.dealtHigh()-(own?0:lo),
                p.lostLow()-(own?hi:0),p.lostHigh()-(own?lo:0),p.unknown(),p.skipNext());
    }
    private static Progress hurt(Progress p, boolean self, Interval damage) {
        Actor a = self ? p.state().self() : p.state().opponent(); Interval hp = a.hp().value();
        double low = Math.min(hp.low(), damage.low()), high = Math.min(hp.high(), damage.high());
        Actor changed = actor(a, remainingHp(hp, damage), a.stages());
        State s = state(p.state(), self ? changed : p.state().self(), self ? p.state().opponent() : changed,
                p.state().actionKind(), p.state().action());
        return new Progress(s, p.dealtLow() + (self ? 0 : low), p.dealtHigh() + (self ? 0 : high),
                p.lostLow() + (self ? low : 0), p.lostHigh() + (self ? high : 0), p.unknown(),p.skipNext());
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
        return new Progress(s, p.dealtLow(), p.dealtHigh(), p.lostLow(), p.lostHigh(), p.unknown(),p.skipNext());
    }
    private static Progress unknown(Progress p, String reason) {
        Set<String> unknown = new TreeSet<>(p.unknown()); unknown.add(reason);
        return new Progress(p.state(), p.dealtLow(), p.dealtHigh(), p.lostLow(), p.lostHigh(), Set.copyOf(unknown),p.skipNext());
    }
    private static boolean zero(Actor a) { return a.hp().known() && a.hp().value().high() == 0; }
    private static boolean possibleFaint(Actor a) { return a.hp().known() && a.hp().value().low() == 0; }
}
