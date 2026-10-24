package com.maestroai.ai;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.maestroai.training.BattleSnapshot;
import com.maestroai.training.ShadowDecisionComparator;
import com.maestroai.training.PublicBattleObserver;
import com.maestroai.training.PublicShadowSnapshot;
import com.maestroai.training.PublicTransitionState;
import com.maestroai.training.PublicProtocolFilter;
import com.maestroai.training.PublicProtocolEvidence;
import com.maestroai.training.PublicBattleCapture;
import com.maestroai.training.ControlledBattleCaptureRegistry;
import com.maestroai.training.ShadowSwitchTelemetry;
import com.maestroai.training.ShadowSwitchingEvaluator;
import com.maestroai.training.MoveEffectModel;
import com.maestroai.training.PublicInteractionModel;
import com.maestroai.training.ShadowMechanicsPredictor;
import com.maestroai.training.CobblemonMechanics;
import com.maestroai.training.ShadowMechanicsEvaluation;
import com.maestroai.training.PublicMechanicsEvidence;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.elemental.ElementalTypeTeraType;
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.InBattleMove;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.SwitchActionResponse;
import com.cobblemon.mod.common.battles.Targetable;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MaestroBattleAI extends RCTBattleAI {

    /*
     * Maestro AI philosophy:
     *
     * 1. Take guaranteed KOs whenever possible.
     * 2. Prefer moves that create favorable follow-up positions.
     * 3. Avoid unnecessary switches.
     * 4. Switch aggressively when the current matchup is genuinely bad.
     * 5. Set up when the opponent cannot punish it.
     * 6. Recover when recovery materially improves the position.
     * 7. Preserve healthy win conditions.
     * 8. Use controlled unpredictability only when choices are genuinely close.
     *
     * The evaluator intentionally does not pretend to know unverified
     * Cobblemon internals such as item/ability state. Everything here uses
     * APIs that are known to exist in the current environment.
     */

    private static final double SWITCH_MARGIN = 95.0;
    private static final double EMERGENCY_THREAT = 1.75;

    private static final double KO_BONUS = 1250.0;
    private static final double RELIABLE_KO_BONUS = 650.0;
    private static final double TWO_HIT_KO_BONUS = 220.0;

    private static final double SAFE_SETUP_BONUS = 260.0;
    private static final double DANGEROUS_SETUP_PENALTY = 260.0;

    private static final double RECOVERY_LOW_HP_BONUS = 230.0;
    private static final double RECOVERY_HIGH_HP_PENALTY = 150.0;

    private static final double SWITCHING_INTO_IMMUNITY_BONUS = 320.0;
    private static final double SWITCHING_INTO_RESISTANCE_BONUS = 210.0;
    private static final double BAD_SWITCH_PENALTY = 300.0;

    private static final double ENDGAME_KILL_BONUS = 260.0;
    private static final double ENDGAME_SACRIFICE_PENALTY = 160.0;

    private static final double MAX_PREDICTION_PENALTY = 320.0;

    private static final double CERTAIN_HIT_ACCURACY = 99.999;
    private static final double WINNING_POSITION_DAMAGE_RATIO = 0.50;
    private static final double WINNING_POSITION_INCOMING_RATIO = 0.50;

    /*
     * Intelligence-v2 competitive priors. These never reveal an exact hidden
     * set. They only stop Maestro from treating common competitive threats as
     * impossible until their fourth move is shown.
     */
    private static final double UNKNOWN_THREAT_FLOOR = 0.28;
    private static final double HIGH_THREAT_FLOOR = 0.45;
    private static final double PRIORITY_SPEED_SAFETY_PENALTY = 85.0;
    private static final double SETUP_SWEEPER_PRESSURE = 95.0;
    private static final double REPEATED_SETUP_PENALTY = 330.0;
    private static final double MAXED_SETUP_PENALTY = 900.0;
    private static final double LOSING_SWITCH_MIN_IMPROVEMENT = 85.0;
    private static final double DESPERATION_SWITCH_MARGIN = 35.0;
    private static final double BAD_LINE_SACRIFICE_PENALTY = 180.0;

    /*
     * Intelligence-v3: role, pivot and move-interaction awareness.
     * These values are intentionally strategic rather than species-scripted:
     * the same logic applies whenever a Pokémon has a pivot move, conditional
     * priority, or a setup opportunity on a predicted switch.
     */
    private static final double SAFE_PIVOT_OVER_SWITCH_BONUS = 285.0;
    private static final double PIVOT_POSITION_SHARE = 0.72;
    private static final double SAFE_PIVOT_HARD_SWITCH_MARGIN = 45.0;
    private static final double SWITCH_SETUP_PUNISH_PENALTY = 210.0;
    private static final double SUCKER_PUNCH_ATTACK_PENALTY = 245.0;
    private static final double SUCKER_PUNCH_KO_PENALTY = 430.0;
    private static final double SUCKER_PUNCH_NON_ATTACK_BONUS = 145.0;
    private static final double ANTI_SETUP_SWITCH_BONUS = 150.0;
    private static final double WIN_CONDITION_PRESERVATION = 115.0;
    private static final double WALL_PROGRESS_BONUS = 120.0;
    private static final double WALL_CHURN_PENALTY = 135.0;
    private static final double SETUP_THREAT_ATTACK_BONUS = 110.0;
    private static final double SETUP_THREAT_PASSIVE_PENALTY = 165.0;
    private static final double LOW_HP_TRADE_BONUS = 125.0;
    private static final double DITTO_SETUP_PENALTY = 260.0;
    private static final double DITTO_PIVOT_BONUS = 95.0;
    private static final double RELIABLE_MOVE_EDGE = 80.0;
    private static final double RECOVERY_KO_DANGER_PENALTY = 300.0;
    private static final double CLEANER_PRIORITY_BONUS = 120.0;
    private static final double OVERKILL_ACCURACY_PENALTY = 120.0;
    private static final double PIVOT_CHIP_VALUE = 0.70;
    private static final double PIVOT_DESTINATION_CAP = 980.0;
    private static final double PIVOT_FAST_BONUS = 145.0;
    private static final double PIVOT_SLOW_RISK_PENALTY = 240.0;
    private static final double HARD_SWITCH_TAX = 115.0;
    private static final double GENESECT_SCARF_PIVOT_BONUS = 185.0;
    private static final double GENESECT_KO_OVERRIDE_BONUS = 260.0;
    private static final double GENESECT_SPEED_CONTROL_PRESERVATION = 95.0;
    private static final double ROLE_SETUP_SAFETY_GATE = 0.55;
    private static final double REPEAT_MOVE_SOFT_PENALTY = 28.0;
    private static final double REPEAT_MOVE_HARD_PENALTY = 75.0;
    private static final double WALL_RETURN_PENALTY = 95.0;
    private static final double DANGEROUS_PIVOT_SURVIVAL_MARGIN = 1.10;
    private static final double SUCKER_SETUP_DODGE_BONUS = 110.0;
    private static final double SUCKER_RECOVERY_DODGE_BONUS = 70.0;
    private static final double SUCKER_RANDOM_STATUS_PENALTY = 60.0;

    /* Intelligence-v6: live battle state and win-condition conversion. */
    private static final double XERNEAS_GEOMANCY_WINCON_BONUS = 560.0;
    private static final double MEWTWO_NASTY_PLOT_WINCON_BONUS = 430.0;
    private static final double SAFE_SETUP_SWEEP_BONUS = 220.0;
    private static final double TERA_SUPER_EFFECTIVE_BONUS = 95.0;
    private static final double STATUS_MOVE_ON_STATUS_TARGET_PENALTY = 190.0;
    private static final double CLEANER_ENDGAME_BONUS = 185.0;
    private static final double WALL_BREAKER_PROGRESS_BONUS = 140.0;
    private static final double LOW_HP_WINCON_PRESERVATION = 125.0;

    /*
     * Remembers the last move selected by each Pokémon.
     *
     * This is useful both for future Choice-item handling and for avoiding
     * obviously repetitive behavior where a previous decision would make
     * a different move nonsensical.
     */
    private final Map<UUID, String> lastSelectedMoves = new HashMap<>();
    private final PublicBattleObserver publicProtocolObserver = new PublicBattleObserver();
    private final PublicProtocolEvidence publicProtocolEvidence = new PublicProtocolEvidence();
    private BattleSnapshot mechanicsSnapshot;
    private String mechanicsOpponentSlot;
    private com.maestroai.training.PublicStateSearchAdapter.PublicMechanics mechanicsEvidence;
    private final PublicMechanicsEvidence publicMechanicsEvidence = new PublicMechanicsEvidence(this::publicCatalogSpecies);
    private Map<String, CobblemonMechanics.Profile> mechanicsProfiles = Map.of();
    private final com.maestroai.training.PublicFieldEvidence publicFieldEvidence = new com.maestroai.training.PublicFieldEvidence();
    private com.maestroai.training.PublicFieldEvidence.Capture fieldCapture;
    private com.maestroai.training.ScopedExecution mechanicsExecution;
    private int observedShowdownMessageCount = 0;
    private int observedShowdownHistoryFingerprint = 0;
    private final ShadowSwitchTelemetry shadowSwitchTelemetry = new ShadowSwitchTelemetry();
    private PokemonBattle shadowObservedBattle;

    // Publicly executed moves only. Never inspect an opponent Pokemon's moveset.
    private final Map<UUID, List<String>> opponentObservedMoves = new HashMap<>();
    private final Map<UUID, String> lastObservedMajorMove = new HashMap<>();


    /*
     * Tracks how many times a Pokémon has acted during the current battle.
     * Used only for controlled decision variation and endgame behavior.
     */
    private final Map<UUID, Integer> actionCounts = new HashMap<>();

    /*
     * Tracks consecutive setup decisions while a Pokémon remains active.
     * Switching clears this because ordinary stat stages are lost on switch.
     */
    private final Map<String, Integer> consecutiveSetupBySpecies = new HashMap<>();

    /* v4 battle-flow memory for repeated matchup churn. */
    private final Map<String, Integer> matchupVisits = new HashMap<>();
    private String lastMatchupKey = "";

    /*
     * Pokemon.teraType is the assigned type, not proof that Tera is active.
     * Cobblemon records the actual |-terastallize| battle instruction, so
     * Maestro mirrors only observed Terastallization here.
     */
    private final Map<UUID, ElementalType> activeTeraTypes = new HashMap<>();
    private UUID currentBattleId = null;
    private final OwnChoiceRecovery choiceRecovery = new OwnChoiceRecovery();
    private UUID lastSwitchDestination = null;
    private int lastSwitchTurn = -100;
    private final com.maestroai.training.OwnRequestTransport ownTransport = new com.maestroai.training.OwnRequestTransport();
    private com.maestroai.training.OwnRequestTransport.Lease ownChoiceLease;
    private final Map<String, PublicTransitionState.Known<Boolean>> ownFreshness = new HashMap<>();
    private com.maestroai.training.RuntimeDomain runtimeDomain;
    private final Map<String, Map<String,Integer>> ownEffectiveStages = new HashMap<>();
    private final Map<String, PublicTransitionState.Known<com.maestroai.training.PublicStateSearchAdapter.Interval>> ownReceiptHp = new HashMap<>();

    /** Called only after setRequest in the queued owning RequestInstruction closure. */
    public void ownRequestDispatched(PokemonBattle battle,
            com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor actor,
            com.cobblemon.mod.common.battles.ShowdownActionRequest request,
            java.util.function.Supplier<String> raw) {
        if (!ownTransportVersionSupported()) { ownTransport.invalidate("OWN_TRANSPORT_VERSION_UNSUPPORTED"); return; }
        if (actor.getBattleAI() != this || actor.getBattle() != battle || battle.getActor(actor.getShowdownId()) != actor) return;
        ownTransport.owner(battle, actor, actor.getShowdownId());
        ownTransport.dispatched(battle, actor, actor.getShowdownId(), request,
                () -> com.maestroai.training.OwnRequestJson.parse(raw.get()));
    }

    public void ownTurnDispatched(PokemonBattle battle,
            com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor actor, int turn) {
        if (!ownTransportVersionSupported()) { ownTransport.invalidate("OWN_TRANSPORT_VERSION_UNSUPPORTED"); return; }
        if (actor.getBattleAI() != this || actor.getBattle() != battle) return;
        ownTransport.owner(battle, actor, actor.getShowdownId());
        ownTransport.turnDispatched(battle, turn);
    }

    private static boolean ownTransportVersionSupported() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("cobblemon")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("1.7.3+1.21.1")).orElse(false);
    }

    public void ownTransportUnavailable() { ownTransport.invalidate("OWN_RUNTIME_HOOK_UNAVAILABLE"); }

    /** Error-return is a second readiness notification, never a payload source.
     * If a corrected request arrived first, upstream now restored mustChoose.
     */
    public void ownChoiceRejected(PokemonBattle battle,
            com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor actor) {
        ownRequestCorrected(battle, actor, actor.getRequest());
    }

    public void ownRequestCorrected(PokemonBattle battle,
            com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor actor,
            com.cobblemon.mod.common.battles.ShowdownActionRequest request) {
        if (!ownTransportVersionSupported() || actor.getBattleAI() != this || actor.getBattle() != battle
                || battle.getActor(actor.getShowdownId()) != actor || battle.getEnded()
                || request == null || actor.getRequest() != request || actor.getActivePokemon().size() != 1
                || !actor.getResponses().isEmpty()) return;
        var active = actor.getActivePokemon().getFirst().getBattlePokemon();
        var moves = request.getActive();
        boolean ordinary = !request.getWait() && !request.getForceSwitch().contains(true)
                && moves != null && moves.size() == 1 && moves.getFirst() != null;
        if (active != null && choiceRecovery.corrected(battle, actor, request,
                ownTransport.dispatchedTurn(battle, actor, battle.getTurn()), active.getUuid(),
                actor.getMustChoose(), ordinary && moves.getFirst().getTrapped(), ordinary)) {
            System.out.println("[MaestroAI] Retrying corrected own trapped-switch request");
            actor.onChoiceRequested();
        }
    }

    public void ownTurnFinished(PokemonBattle battle,
            com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor actor, int turn) {
        if (actor.getBattleAI() == this && actor.getBattle() == battle) ownTransport.turnFinished(battle, turn);
    }

    public MaestroBattleAI() {
        super();
    }

    @Override
    public ShowdownActionResponse choose(
            ActiveBattlePokemon activePokemon,
            PokemonBattle battle,
            BattleSide side,
            ShowdownMoveset moveset,
            boolean forceSwitch
    ) {
        // No live promotion has been approved: even an enabled flag cannot
        // install the unproven shadow predictor. Legacy remains authoritative.
        try {
            try {
            if (activePokemon != null && activePokemon.getBattlePokemon() != null && activePokemon.getActor() != null) {
                var actor = activePokemon.getActor(); var request = actor.getRequest();
                var activeUuid = activePokemon.getBattlePokemon().getUuid();
                ownChoiceLease = ownTransport.beginChoice(battle, actor, request, activePokemon.getBattlePokemon().getUuid(), forceSwitch,
                        () -> actor.getBattle() == battle && actor.getRequest() == request && !battle.getEnded()
                                && ownTransport.observedTurnAllowed(battle.getTurn())
                                && activePokemon.getBattlePokemon() != null && activeUuid.equals(activePokemon.getBattlePokemon().getUuid())
                                && actor.getActivePokemon().contains(activePokemon));
            }
            } catch (RuntimeException unavailable) {
                ownTransportUnavailable(); ownChoiceLease = null;
            }
            ShowdownActionResponse response = LivePromotionGate.choose(
                () -> chooseLegacy(activePokemon, battle, side, moveset, forceSwitch),
                null, null, false);
            recordOwnChoice(battle, activePokemon, moveset, forceSwitch, response);
            return response;
        } finally {
            ownTransport.endChoice(); ownChoiceLease = null;
        }
    }

    private void recordOwnChoice(PokemonBattle battle, ActiveBattlePokemon activePokemon,
            ShowdownMoveset moveset, boolean forceSwitch, ShowdownActionResponse response) {
            if (activePokemon != null && activePokemon.getActor() != null && activePokemon.getBattlePokemon() != null) {
                var actor = activePokemon.getActor();
                int choiceTurn = ownTransport.dispatchedTurn(battle, actor, battle.getTurn());
                choiceRecovery.submitted(battle, actor, actor.getRequest(), choiceTurn,
                        activePokemon.getBattlePokemon().getUuid(), choiceTurn > 0 && !forceSwitch && response instanceof SwitchActionResponse
                                && actor.getRequest() != null && !actor.getRequest().getWait()
                                && !actor.getRequest().getForceSwitch().contains(true)
                                && actor.getRequest().getActive() != null
                                && actor.getRequest().getActive().size() == 1
                                && moveset != null && !moveset.getTrapped());
            }
    }

    private ShowdownActionResponse chooseLegacy(
            ActiveBattlePokemon activePokemon,
            PokemonBattle battle,
            BattleSide side,
            ShowdownMoveset moveset,
            boolean forceSwitch
    ) {
        if (activePokemon == null
                || activePokemon.getBattlePokemon() == null) {

            return super.choose(
                    activePokemon,
                    battle,
                    side,
                    moveset,
                    forceSwitch
            );
        }

        ActiveBattlePokemon opponent =
                getOpponent(activePokemon);

        if (opponent == null
                || opponent.getBattlePokemon() == null) {

            return super.choose(
                    activePokemon,
                    battle,
                    side,
                    moveset,
                    forceSwitch
            );
        }

        BattlePokemon mineBattle =
                activePokemon.getBattlePokemon();

        BattlePokemon enemyBattle =
                opponent.getBattlePokemon();

        Pokemon mine =
                mineBattle.getEffectedPokemon();

        Pokemon enemy =
                enemyBattle.getEffectedPokemon();

        if (mine == null || enemy == null) {
            return super.choose(
                    activePokemon,
                    battle,
                    side,
                    moveset,
                    forceSwitch
            );
        }

        resetMemoryForNewBattle(battle);
        syncObservedTeraState(battle);
        syncPublicMoveHistory(battle);
        syncPublicProtocolObservation(battle);
        incrementActionCount(mineBattle);
        // The underlying opponent Pokemon can be Zoroark while the public
        // Showdown switch still identifies Gholdengo. Never score the hidden
        // species/types before |replace| publicly reveals the illusion.
        String publicSpecies = publicProtocolObserver.publicSpecies(opponent.getPNX());
        String actualSpecies = enemy.getSpecies() == null ? null
                : enemy.getSpecies().getName();
        if (publicSpecies != null && actualSpecies != null
                && !publicSpecies.startsWith(normalizePublicSpecies(actualSpecies))) {
            System.out.println("[MaestroAI] Public identity differs from internal battle state;"
                    + " using information-safe legal action without opponent scoring");
            if (forceSwitch) {
                for (BattlePokemon candidate : activePokemon.getActor().getPokemonList()) {
                    if (candidate != null && candidate.getUuid() != null
                            && !candidate.getUuid().equals(mineBattle.getUuid())
                            && candidate.canBeSentOut() && !candidate.getGone()
                            && candidate.getHealth() > 0 && !candidate.isSentOut()) {
                        return new SwitchActionResponse(candidate.getUuid());
                    }
                }
                return super.choose(activePokemon, battle, side, moveset, true);
            }
            if (moveset != null && moveset.getMoves() != null) {
                for (InBattleMove available : moveset.getMoves()) {
                    if (available != null && available.getId() != null && available.canBeUsed()) {
                        ShowdownActionResponse response = createMoveResponse(
                                activePokemon, moveset, available.getId());
                        if (response != null) return response;
                    }
                }
            }
            // With no move available, defer to Cobblemon's own action fallback.
            return super.choose(activePokemon, battle, side, moveset, false);
        }
        rememberMatchupVisit(mine, enemy);

        /*
         * Forced switching is handled separately.
         */
        if (forceSwitch) {

            SwitchChoice forced =
                    findBestSwitch(
                            activePokemon,
                            opponent
                    );

            if (forced != null) {

                logSwitchDecision(
                        battle,
                        mine,
                        enemy,
                        forced,
                        "forced switch"
                );

                clearRememberedMove(mineBattle);
                clearSetupHistory(mine);
                rememberSwitchDestination(battle, forced);

                return new SwitchActionResponse(
                        forced.pokemon.getUuid()
                );
            }

            return super.choose(
                    activePokemon,
                    battle,
                    side,
                    moveset,
                    true
            );
        }

        /*
         * Build every legal move and evaluate it deeply.
         */
        List<MoveChoice> moves =
                findAllMoves(
                        moveset,
                        mine,
                        enemy
                );

        /*
         * Evaluate the best switch as a competing action.
         */
        // The active Showdown request, not bench eligibility, decides whether
        // a voluntary switch is possible. Charging moves expose only their
        // continuation and cannot be interrupted by a hard switch.
        boolean continuationLocked = isContinuationLocked(moveset, mineBattle);
        boolean voluntarySwitchAllowed =
                moveset != null && LiveActionLegality.voluntarySwitchAllowed(
                        false, moveset.getTrapped(), continuationLocked);
        SwitchChoice bestSwitch = voluntarySwitchAllowed
                ? findBestSwitch(activePokemon, opponent)
                : null;

        MoveChoice bestMove =
                findBestMove(
                        moves,
                        mine,
                        enemy,
                        bestSwitch
                );

        // Shadow-only bridge: capture ordinary choices and certified own Z options.
        // Never alter battle decisions based on this observation.
        BattleSnapshot shadowSnapshot = observeTacticalSnapshot(battle, activePokemon, opponent, mine, moves,
                voluntarySwitchAllowed, continuationLocked, moveset != null && moveset.getTrapped(), moveset);

        /*
         * If no normal move exists, switch if possible.
         */
        if (bestMove == null) {

            if (bestSwitch != null) {

                System.out.println(
                        "[MaestroAI] No usable normal move."
                );

                System.out.println(
                        "[MaestroAI] Switching instead of Struggle."
                );

                logSwitchDecision(
                        battle,
                        mine,
                        enemy,
                        bestSwitch,
                        "no usable move"
                );

                clearRememberedMove(mineBattle);
                clearSetupHistory(mine);
                rememberSwitchDestination(battle, bestSwitch);

                return new SwitchActionResponse(
                        bestSwitch.pokemon.getUuid()
                );
            }

            String struggleId =
                    findUsableStruggle(moveset);

            if (struggleId != null) {

                rememberMove(
                        mineBattle,
                        struggleId
                );

                System.out.println(
                        "[MaestroAI] No usable move or switch."
                );

                System.out.println(
                        "[MaestroAI] Using Struggle."
                );

                ShowdownActionResponse response =
                        createMoveResponse(
                                activePokemon,
                                moveset,
                                struggleId
                        );

                if (response != null) {
                    return response;
                }
            }

            return super.choose(
                    activePokemon,
                    battle,
                    side,
                    moveset,
                    false
            );
        }

        /*
         * Intelligence-v3 pivot bridge.
         *
         * If Maestro already wants to leave, compare a legal pivot against the
         * hard switch. This is the missing "Scarf Genesect uses U-turn on the
         * way out" reasoning: damage + momentum + the resulting switch
         * position are valued as one line instead of unrelated actions.
         */
        if (bestSwitch != null) {
            applyPivotDestinationValue(
                    moves,
                    mine,
                    enemy,
                    bestSwitch
            );

            bestMove =
                    findBestMove(
                            moves,
                            mine,
                            enemy,
                            bestSwitch
                    );
        }

        double shadowStayScore = Double.NaN;
        double shadowSwitchScore = Double.NaN;

        /*
         * Compare staying in against switching.
         */
        if (bestSwitch != null) {

            double stayScore =
                    evaluateCurrentPosition(
                            mine,
                            enemy,
                            bestMove
                    );

            double switchScore =
                    evaluateSwitchPosition(
                            activePokemon,
                            opponent,
                            bestSwitch
                    );

             /*
             * If a safe damaging pivot is already the best move, a raw hard
             * switch must justify spending the entire turn without chip.
             */
            if (bestMove.pivotMove
                    && !currentPokemonWouldDieBeforePivot(
                    mine,
                    enemy,
                    bestMove
            )) {
                switchScore -= HARD_SWITCH_TAX;
            }

            /*
             * A hard switch gives the opponent a free action. Against common
             * setup sweepers, model the danger of them exploiting that turn.
             * Anti-setup answers (notably Spectral Thief users) lose less
             * value because they can punish the boost after entering.
             */
            switchScore -=
                    predictedSwitchSetupPenalty(
                            enemy,
                            bestSwitch
                    );

            // Shadow proxy must use the same post-penalty scores that the
            // live switch gate sees, not its pre-penalty estimates.
            shadowStayScore = stayScore;
            shadowSwitchScore = switchScore;

            boolean immediateDanger =
                    bestSwitch.currentThreat
                            >= EMERGENCY_THREAT
                            && bestMove.estimatedDamage
                            < enemy.getCurrentHealth();

            boolean switchClearlyBetter =
                    switchScore
                            > stayScore
                            + SWITCH_MARGIN;

            boolean lowHpEmergency =
                    getHpPercent(mine) < 0.25
                            && bestSwitch.currentThreat < 1.0
                            && switchScore > stayScore + 20.0;

            /*
             * Use the current Pokémon's actual incoming damage rather than
             * the candidate switch's type threat. The old gate could mark the
             * active Pokémon as doomed simply because the proposed switch had
             * a bad matchup, which contributed to irrational escape routes.
             */
            double currentIncomingDamage =
                    getBestEstimatedDamage(enemy, mine);

            boolean currentPokemonLikelyDies =
                    currentIncomingDamage >= mine.getCurrentHealth();

            boolean preserveWinCondition =
                    shouldPreserveCurrentPokemon(
                            activePokemon,
                            mine,
                            enemy,
                            bestMove,
                            bestSwitch
                    );

            boolean guaranteedKo =
                    isGuaranteedKO(bestMove, enemy);

            boolean winningPosition =
                    isWinningPosition(
                            mine,
                            enemy,
                            bestMove
                    );

            /*
             * Never switch away from a guaranteed KO unless the matchup
             * is so catastrophic that the move itself is not actually safe.
             */
            /*
             * Bad-position escape: a negative 2-ply evaluation means Maestro
             * already believes staying in loses value. In that case the
             * normal conservative switch margin should not trap him in place.
             */
            /*
             * Negative-line escape now distinguishes "switch to safety" from
             * "switch into another losing position". The latter requires a
             * much larger improvement, preventing pointless sacrifice loops.
             */
            boolean bothLinesBad =
                    stayScore < 0.0
                            && switchScore < 0.0;

            boolean losingLine =
                    bestMove.finalScore < 0.0
                            && (
                            (!bothLinesBad
                                    && switchScore > stayScore + 10.0)
                                    || (bothLinesBad
                                    && switchScore > stayScore
                                    + LOSING_SWITCH_MIN_IMPROVEMENT)
                            );

            boolean desperationSwitch =
                    stayScore <= -250.0
                            && switchScore
                            > stayScore + DESPERATION_SWITCH_MARGIN;

            /*
             * v4 sacrifice discipline: when both staying and switching are
             * losing, do not burn a healthier teammate merely to postpone a
             * KO unless the switch materially improves the evaluation.
             */
            if (bothLinesBad
                    && getHpPercent(mine) < 0.30
                    && switchScore < stayScore + LOSING_SWITCH_MIN_IMPROVEMENT) {
                switchClearlyBetter = false;
                lowHpEmergency = false;
                preserveWinCondition = false;
            }

            // Diagnose repeated bridge-switch matchups without changing
            // the policy or disclosing unrevealed opponent sets.
            if (getMatchupVisits(mine, enemy) >= 2
                    || "dondozo".equals(speciesId(enemy))
                    || "arceus".equals(speciesId(enemy))) {
                System.out.println("[MaestroAI] Shadow matchup audit"
                        + " turn=" + battle.getTurn()
                        + " self=" + speciesId(mine)
                        + " enemy=" + speciesId(enemy)
                        + " stay=" + String.format("%.2f", stayScore)
                        + " switch=" + String.format("%.2f", switchScore)
                        + " visits=" + getMatchupVisits(mine, enemy)
                        + " immediateDanger=" + immediateDanger
                        + " lethalIncoming=" + currentPokemonLikelyDies
                        + " preserve=" + preserveWinCondition
                        + " winningPosition=" + winningPosition
                        + " setupCandidate=" + (moves.stream().anyMatch(
                                candidate -> candidate != null
                                        && ("geomancy".equals(candidate.moveId)
                                        || "nastyplot".equals(candidate.moveId)
                                        || "swordsdance".equals(candidate.moveId)))));
            }

            boolean shouldSwitch =
                    !guaranteedKo
                            && (
                            immediateDanger
                                    || (switchClearlyBetter && !winningPosition)
                                    || lowHpEmergency
                                    || currentPokemonLikelyDies
                                    || preserveWinCondition
                                    || losingLine
                                    || desperationSwitch
                    );

            /*
             * Route compression: if this Pokémon was just selected as the
             * destination, do not immediately use it as a disposable bridge
             * to another teammate unless the new matchup is genuinely lethal
             * or the replacement is dramatically better.
             */
            boolean justArrived =
                    mine.getUuid() != null
                            && mine.getUuid().equals(lastSwitchDestination)
                            && battle.getTurn() <= lastSwitchTurn + 1;

            if (justArrived
                    && !currentPokemonLikelyDies
                    && !immediateDanger
                    && switchScore < stayScore + 260.0) {
                shouldSwitch = false;
                System.out.println(
                        "[MaestroAI] Intelligence: route compression kept direct destination"
                                + " | stay=" + String.format("%.2f", stayScore)
                                + " | switch=" + String.format("%.2f", switchScore)
                );
            }

            /*
             * Intelligence-v3.1: a hard switch and a pivot are alternative
             * routes out of the same position. Previously the pivot received
             * destination value, but the switch gate could still fire first
             * and throw that move away. If a legal/safe pivot is already
             * competitive with the hard-switch line, keep the move line and
             * let the pivot execute. Immediate death remains an exception.
             */
            boolean preferPivotExit =
                    bestMove.pivotMove
                            && !currentPokemonLikelyDies
                            && bestMove.finalScore
                            >= switchScore - SAFE_PIVOT_HARD_SWITCH_MARGIN;

            if (shouldSwitch && preferPivotExit) {
                shouldSwitch = false;
                System.out.println(
                        "[MaestroAI] Intelligence: safe pivot preferred over hard switch"
                                + " | pivot="
                                + bestMove.moveName
                                + " | pivotScore="
                                + String.format("%.2f", bestMove.finalScore)
                                + " | hardSwitchScore="
                                + String.format("%.2f", switchScore)
                );
            }

            if (shouldSwitch) {

                String reason;

                if (immediateDanger) {
                    reason = "dangerous matchup";
                } else if (currentPokemonLikelyDies) {
                    reason = "opponent threatens immediate KO";
                } else if (lowHpEmergency) {
                    reason = "low HP preservation";
                } else if (preserveWinCondition) {
                    reason = "preserve win condition";
                } else if (losingLine) {
                    reason = "escape negative evaluated line";
                } else if (desperationSwitch) {
                    reason = "desperation escape from catastrophic line";
                } else {
                    reason = "better long-term position";
                }

                logCrossActionShadow(shadowSnapshot, "switch",
                        bestSwitch.pokemon.getUuid().toString(), bestMove.moveId,
                        stayScore, switchScore);

                logSwitchDecision(
                        battle,
                        mine,
                        enemy,
                        bestSwitch,
                        reason
                );

                clearRememberedMove(mineBattle);
                clearSetupHistory(mine);
                rememberSwitchDestination(battle, bestSwitch);

                return new SwitchActionResponse(
                        bestSwitch.pokemon.getUuid()
                );
            }
        }

        /*
         * Do not randomize away from defining setup turns. Once the evaluator
         * concludes that Geomancy or Nasty Plot is the best strategic line,
         * controlled variation must not turn that into arbitrary coverage.
         */
        boolean committedSetup =
                (speciesId(mine).equals("xerneas")
                        && normalize(bestMove.moveName).equals("geomancy"))
                        || (speciesId(mine).equals("mewtwo")
                        && normalize(bestMove.moveName).equals("nastyplot"));

        /*
         * Controlled unpredictability:
         *
         * Do not let variation throw away a pivot plan or a guaranteed KO.
         * Those are strategic commitments, not cosmetic tie-breakers.
         */
        MoveChoice selectedMove;

        if (bestMove.pivotMove
                || isGuaranteedKO(bestMove, enemy)
                || committedSetup) {
            selectedMove = bestMove;
        } else {
            selectedMove =
                    selectControlledVariation(
                            moves,
                            bestMove
                    );
        }

        // Compare legacy move selection against the same evaluator's ranked
        // alternatives. Diagnostic only: never override selectedMove.
        logShadowComparison(shadowSnapshot, selectedMove, moves);
        if (bestSwitch != null && Double.isFinite(shadowStayScore)
                && Double.isFinite(shadowSwitchScore)) {
            logCrossActionShadow(shadowSnapshot, "move", selectedMove.moveId,
                    bestSwitch.pokemon.getUuid().toString(),
                    shadowStayScore, shadowSwitchScore);
        }

        rememberMove(
                mineBattle,
                selectedMove.moveId
        );

        rememberStrategicAction(
                mine,
                selectedMove
        );

        logMoveDecision(
                battle,
                mine,
                enemy,
                selectedMove,
                bestSwitch
        );

        ShowdownActionResponse response =
                createMoveResponse(
                        activePokemon,
                        moveset,
                        selectedMove.moveId
                );

        /*
         * Critical safety net:
         *
         * Never return null if we have a legal move. If an unusual move
         * target interpretation rejects our custom response, fall back to
         * the parent AI rather than making Maestro skip his entire turn.
         */
        if (response != null) {
            return response;
        }

        System.out.println(
                "[MaestroAI] WARNING: custom move response could not be built."
        );

        System.out.println(
                "[MaestroAI] Falling back to RCT action selection."
        );

        return super.choose(
                activePokemon,
                battle,
                side,
                moveset,
                false
        );
    }

    private static String normalizePublicSpecies(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private boolean isContinuationLocked(ShowdownMoveset moveset, BattlePokemon active) {
        if (moveset == null || moveset.getMoves() == null || active == null) return false;
        String previous = lastSelectedMoves.get(active.getUuid());
        String only = moveset.getMoves().size() == 1
                && moveset.getMoves().get(0) != null
                ? moveset.getMoves().get(0).getId() : null;
        return LiveActionLegality.forcedContinuation(previous, only,
                moveset.getMoves().size());
    }

    private ShowdownActionResponse createMoveResponse(
            ActiveBattlePokemon activePokemon,
            ShowdownMoveset moveset,
            String moveId
    ) {
        if (activePokemon == null
                || moveset == null
                || moveId == null) {
            return null;
        }

        InBattleMove selectedMove = null;

        if (moveset.getMoves() != null) {

            for (InBattleMove battleMove :
                    moveset.getMoves()) {

                if (battleMove == null
                        || battleMove.getId() == null) {
                    continue;
                }

                /*
                 * MoveActionResponse requires the exact InBattleMove ID.
                 */
                if (battleMove.getId()
                        .equals(moveId)) {

                    selectedMove = battleMove;
                    break;
                }
            }
        }

        if (selectedMove == null) {
            return null;
        }

        if (!selectedMove.canBeUsed()) {
            return null;
        }

        String targetPnx =
                getValidTargetPnx(
                        activePokemon,
                        selectedMove
                );

        if (isTerastallized(activePokemon.getBattlePokemon().getEffectedPokemon())) {
            System.out.println(
                    "[MaestroAI] Intelligence: acting while Terastallized as "
                            + effectiveTypeLabel(
                            activePokemon.getBattlePokemon().getEffectedPokemon()
                    )
            );
        }

        System.out.println(
                "[MaestroAI] Target PNX: "
                        + (
                        targetPnx == null
                                ? "none"
                                : targetPnx
                )
        );

        return new MoveActionResponse(
                moveId,
                targetPnx,
                null
        );
    }

    private String getValidTargetPnx(
            ActiveBattlePokemon activePokemon,
            InBattleMove battleMove
    ) {
        if (activePokemon == null
                || battleMove == null) {
            return null;
        }

        List<Targetable> targets;

        try {
            targets =
                    battleMove.getTargets(
                            activePokemon
                    );
        } catch (Exception ignored) {
            return null;
        }

        /*
         * Important:
         *
         * Some moves, such as Astral Barrage, have a target interpretation
         * where explicitly providing a target is invalid.
         *
         * Therefore an empty target list means "do not specify one".
         */
        if (targets == null
                || targets.isEmpty()) {
            return null;
        }

        ActiveBattlePokemon opponent =
                getOpponent(activePokemon);

        /*
         * Prefer the actual opponent whenever the move permits it.
         */
        if (opponent != null) {

            for (Targetable target : targets) {

                if (target == opponent) {
                    return opponent.getPNX();
                }

                if (target instanceof ActiveBattlePokemon activeTarget
                        && activeTarget.getPNX() != null
                        && activeTarget.getPNX()
                        .equals(opponent.getPNX())) {

                    return activeTarget.getPNX();
                }
            }
        }

        /*
         * Otherwise return a valid active target if one exists.
         */
        for (Targetable target : targets) {

            if (target instanceof ActiveBattlePokemon activeTarget) {

                if (activeTarget.getPNX() != null) {
                    return activeTarget.getPNX();
                }
            }
        }

        return null;
    }

    private List<MoveChoice> findAllMoves(
            ShowdownMoveset moveset,
            Pokemon mine,
            Pokemon enemy
    ) {
        List<MoveChoice> choices =
                new ArrayList<>();

        if (moveset == null
                || moveset.getMoves() == null) {
            return choices;
        }

        for (InBattleMove battleMove :
                moveset.getMoves()) {

            if (battleMove == null) {
                continue;
            }

            if (!battleMove.canBeUsed()) {
                continue;
            }

            if (battleMove.getPp() <= 0) {
                continue;
            }

            String moveId =
                    battleMove.getId();

            if (moveId == null
                    || moveId.isBlank()) {
                continue;
            }

            if (normalize(moveId)
                    .equals("struggle")) {
                continue;
            }

            Move move =
                    findMove(
                            mine,
                            moveId
                    );

            if (move == null) {

                String alternate =
                        battleMove.getMove();

                if (alternate != null
                        && !alternate.isBlank()) {

                    move =
                            findMove(
                                    mine,
                                    alternate
                            );
                }
            }

            if (move == null) {
                continue;
            }

            MoveChoice choice =
                    evaluateMove(
                            move,
                            battleMove,
                            moveId,
                            mine,
                            enemy
                    );

            if (choice != null) {
                choices.add(choice);
            }
        }

        return choices;
    }

    private MoveChoice findBestMove(
            List<MoveChoice> moves,
            Pokemon mine,
            Pokemon enemy,
            SwitchChoice bestSwitch
    ) {
        if (moves == null
                || moves.isEmpty()) {
            return null;
        }

        MoveChoice best = null;

        for (MoveChoice choice : moves) {

            if (choice == null) {
                continue;
            }

            double finalScore =
                    evaluateTwoPlyPosition(
                            choice,
                            mine,
                            enemy,
                            bestSwitch
                    );

            choice.finalScore =
                    finalScore;

            if (best == null
                    || choice.finalScore
                    > best.finalScore) {

                best = choice;
            }
        }

        return best;
    }

    private double evaluateTwoPlyPosition(
            MoveChoice choice,
            Pokemon mine,
            Pokemon enemy,
            SwitchChoice bestSwitch
    ) {
        double score =
                choice.score
                        + choice.strategicAdjustment;

        /*
         * Soft anti-pattern memory: repeated attacks are fine when they are
         * clearly winning, but near-equal repetitive lines lose a little
         * value. This makes Maestro less trivially scriptable without random
         * self-sabotage.
         */
        String lastMove =
                lastSelectedMoveForSpecies(mine);

        if (lastMove != null
                && normalize(lastMove).equals(normalize(choice.moveId))
                && !isGuaranteedKO(choice, enemy)) {
            score -= getMatchupVisits(mine, enemy) >= 2
                    ? REPEAT_MOVE_HARD_PENALTY
                    : REPEAT_MOVE_SOFT_PENALTY;
        }

        int enemyHp =
                enemy.getCurrentHealth();

        /*
         * Guaranteed KO.
         */
        if (isGuaranteedKO(choice, enemy)) {

            score += KO_BONUS;

            /*
             * Finishing the opponent becomes even more important late game.
             */
            score += ENDGAME_KILL_BONUS;
        }

        /*
         * The damage roll is lethal, but the move can still miss.
         * Reward the line in proportion to hit probability rather than
         * pretending it is guaranteed.
         */
        else if (isKOOnHit(choice, enemy)) {
            score += KO_BONUS * (choice.accuracy / 100.0) * 0.72;
            score += ENDGAME_KILL_BONUS * (choice.accuracy / 100.0) * 0.55;
        }

        /*
         * Reliable two-hit pressure.
         */
        else if (choice.estimatedDamage * 2
                >= enemyHp) {

            score += TWO_HIT_KO_BONUS;
        }

        /*
         * Estimate what the opponent can do back after this move.
         */
        double retaliation =
                getBestEstimatedDamage(
                        enemy,
                        mine
                );

        /*
         * Conditional priority is different from ordinary priority.
         * Sucker Punch threatens attacking moves, but a setup/recovery/status
         * action makes it fail. Model that per candidate action instead of
         * applying one generic "priority scary" penalty to the whole matchup.
         */
        if (likelyHasSuckerPunch(enemy)) {
            double suckerDamage =
                    estimateSuckerPunchDamage(
                            enemy,
                            mine
                    );

            if (choice.damageMove) {
                double suckerRatio =
                        suckerDamage
                                / Math.max(
                                1.0,
                                mine.getCurrentHealth()
                        );

                if (choice.priority <= 1) {
                    if (suckerRatio >= 1.0) {
                        score -= SUCKER_PUNCH_KO_PENALTY;
                    } else if (suckerRatio >= 0.55) {
                        score -= SUCKER_PUNCH_ATTACK_PENALTY;
                    }
                }

                /*
                 * Higher-priority attacks such as Extreme Speed (+2) beat
                 * Sucker Punch (+1), so do not punish them as if speed alone
                 * decided the interaction.
                 */
                if (choice.priority > 1) {
                    score += 95.0;
                }
            } else {
                /*
                 * A non-attacking move does make Sucker Punch fail, but not
                 * every status move is automatically a good play. Reward
                 * useful setup/recovery dodges and stop granting the old
                 * blanket bonus to arbitrary passive actions.
                 */
                String action =
                        normalize(choice.moveName);

                if (choice.setupMove) {
                    score += SUCKER_SETUP_DODGE_BONUS;
                } else if (isRecoveryMove(action)
                        && getHpPercent(mine) < 0.65) {
                    score += SUCKER_RECOVERY_DODGE_BONUS;
                } else {
                    score -= SUCKER_RANDOM_STATUS_PENALTY;
                }
            }
        }

        double remainingEnemyHp =
                Math.max(
                        0.0,
                        enemyHp
                                - choice.averageDamage
                );

        /*
         * If our move leaves the opponent nearly dead,
         * their retaliation matters less because they are in
         * finishing range.
         */
        double retaliationWeight;

        if (remainingEnemyHp <= 0.0) {
            retaliationWeight = 0.10;

        } else if (remainingEnemyHp
                <= enemy.getMaxHealth() * 0.25) {

            retaliationWeight = 0.35;

        } else {
            retaliationWeight = 1.0;
        }

        double retaliationRatio =
                retaliation
                        / Math.max(
                        1.0,
                        mine.getCurrentHealth()
                );

        if (retaliationRatio >= 1.0) {

            score -=
                    390.0
                            * retaliationWeight;

        } else if (retaliationRatio >= 0.75) {

            score -=
                    260.0
                            * retaliationWeight;

        } else if (retaliationRatio >= 0.50) {

            score -=
                    150.0
                            * retaliationWeight;

        } else if (retaliationRatio >= 0.25) {

            score -=
                    55.0
                            * retaliationWeight;
        }

        /*
         * If we move first, a strong damage move that leaves the opponent
         * alive but within priority/next-turn KO range is valuable.
         */
        if (mine.getSpeed()
                > enemy.getSpeed()) {

            if (choice.averageDamage
                    >= enemyHp * 0.65) {

                score += 80.0;
            }

        } else if (mine.getSpeed()
                < enemy.getSpeed()) {

            /*
             * Going second while failing to KO is dangerous.
             */
            if (!isKOOnHit(choice, enemy)) {

                score -= 75.0;
            }
        }

        /*
         * Strong priority attacks are valuable when Maestro is slower.
         */
        if (choice.priority > 0
                && mine.getSpeed()
                < enemy.getSpeed()) {

            score +=
                    85.0
                            * choice.priority;
        }

        /*
         * If the move is a setup move, evaluate whether the opponent's
         * retaliation makes the setup suicidal.
         */
        if (choice.setupMove) {

            if (likelyIgnoresOpponentBoosts(enemy)) {
                score -= 520.0;

            } else if (retaliation
                    < mine.getCurrentHealth() * 0.30) {

                score += SAFE_SETUP_BONUS;

            } else if (retaliation
                    >= mine.getCurrentHealth() * 0.75) {

                score -= DANGEROUS_SETUP_PENALTY;
            }
        }

        /*
         * A pivot becomes more valuable when the current Pokémon is under
         * heavy pressure.
         */
        if (choice.pivotMove) {

            double incoming =
                    getBestIncomingEffectiveness(
                            enemy,
                            mine
                    );

            if (incoming >= 2.0) {
                score += 95.0;
            }

            if (incoming >= 4.0) {
                score += 140.0;
            }
        }

        /*
         * Don't waste a turn on a status/setup move if an immediate KO exists.
         */
        if (!choice.damageMove
                && hasGuaranteedKO(
                movesForPokemon(
                        mine,
                        enemy
                ),
                enemy
        )) {

            score -= 600.0;
        }

        /*
         * Intelligence-v4 action-level pressure.
         */
        if (likelySetupSweeper(enemy) && !isGuaranteedKO(choice, enemy)) {
            if (choice.damageMove && choice.estimatedDamage > 0) {
                score += SETUP_THREAT_ATTACK_BONUS;
            } else if (!choice.setupMove) {
                score -= SETUP_THREAT_PASSIVE_PENALTY;
            }
        }

        if (isDitto(enemy)) {
            if (choice.setupMove) {
                score -= DITTO_SETUP_PENALTY;
            }
            if (choice.pivotMove) {
                score += DITTO_PIVOT_BONUS;
            }
        }

        /*
         * Unaware-style walls punish aimless setup and switch churn. Reward
         * concrete HP progress so Maestro commits to breaking the wall.
         */
        if (likelyIgnoresOpponentBoosts(enemy)) {
            if (choice.damageMove) {
                double progress =
                        choice.averageDamage
                                / Math.max(1.0, enemy.getMaxHealth());
                score += WALL_PROGRESS_BONUS * Math.min(1.0, progress * 3.0);
            }
            if (getMatchupVisits(mine, enemy) >= 2 && choice.pivotMove) {
                score -= WALL_CHURN_PENALTY;
            }
        }

        if (getHpPercent(mine) < 0.22
                && choice.damageMove
                && choice.estimatedDamage > 0) {
            score += LOW_HP_TRADE_BONUS;
        }

        /*
         * Reliability and endgame sequencing. Do not gamble on inaccurate
         * overkill when a normal reliable line is enough, and favor priority
         * as a cleaner rather than spending HP on avoidable speed ties.
         */
        if (choice.damageMove
                && choice.accuracy >= 99.999
                && choice.estimatedDamage >= enemyHp) {
            score += RELIABLE_MOVE_EDGE;
        }

        if (choice.damageMove
                && choice.accuracy < 85.0
                && choice.averageDamage >= enemyHp * 1.35) {
            score -= OVERKILL_ACCURACY_PENALTY;
        }

        if (choice.priority > 0
                && enemyHp <= enemy.getMaxHealth() * 0.30
                && choice.estimatedDamage >= enemyHp) {
            score += CLEANER_PRIORITY_BONUS;
        }

        if (!choice.damageMove
                && isRecoveryMove(normalize(choice.moveName))
                && retaliation >= mine.getCurrentHealth() * 0.85) {
            score -= RECOVERY_KO_DANGER_PENALTY;
        }

        /*
         * If a switch is much better than the move and this isn't a KO,
         * penalize the move.
         */
        /*
         * In a losing position, passive turns become increasingly expensive.
         * Prefer trading damage or finding a genuinely better switch.
         */
        if (score < 0.0
                && !choice.damageMove
                && getHpPercent(mine) < 0.60) {
            score -= BAD_LINE_SACRIFICE_PENALTY;
        }

        if (bestSwitch != null
                && choice.estimatedDamage
                < enemyHp) {

            double switchScore =
                    bestSwitch.score;

            if (switchScore
                    > score + SWITCH_MARGIN) {

                score -= 100.0;
            }
        }

        return score;
    }

    private MoveChoice evaluateMove(
            Move move,
            InBattleMove battleMove,
            String moveId,
            Pokemon mine,
            Pokemon enemy
    ) {
        MoveTemplate template =
                move.getTemplate();

        if (template == null) {
            return null;
        }

        String moveName =
                template.getName();

        double power =
                template.getPower();

        double accuracy =
                template.getAccuracy();

        if (accuracy <= 0.0) {
            accuracy = 100.0;
        }

        ElementalType moveType =
                template.getEffectiveElementalType(
                        mine
                );

        if (moveType == null) {
            moveType =
                    template.getElementalType();
        }

        if (moveType == null) {
            return null;
        }

        String category =
                getCategory(template);

        boolean damageMove =
                power > 0.0
                        && (
                        category.contains("physical")
                                || category.contains("special")
                );

        if (!damageMove) {

            return evaluateNonDamagingMove(
                    move,
                    battleMove,
                    moveId,
                    moveName,
                    category,
                    mine,
                    enemy,
                    accuracy
            );
        }

        int attack;
        int defence;

        if (category.contains("physical")) {

            attack =
                    mine.getAttack();

            defence =
                    enemy.getDefence();

        } else {

            attack =
                    mine.getSpecialAttack();

            defence =
                    enemy.getSpecialDefence();
        }

        attack =
                Math.max(
                        1,
                        attack
                );

        defence =
                Math.max(
                        1,
                        defence
                );

        double effectiveness =
                getTypeEffectivenessAgainstPokemon(
                        moveType,
                        enemy
                );

        boolean stab =
                getStabMultiplier(mine, moveType) > 1.0;

        int level =
                Math.max(
                        1,
                        mine.getLevel()
                );

        double rawDamage =
                (((2.0 * level / 5.0 + 2.0)
                        * power
                        * attack
                        / defence)
                        / 50.0)
                        + 2.0;

        rawDamage *= getStabMultiplier(mine, moveType);
        rawDamage *= effectiveness;

        /*
         * Approximate the normal random damage range.
         *
         * 85% is the minimum standard roll.
         * 92.5% is the expected midpoint.
         */
        double minimumDamage =
                Math.floor(
                        rawDamage * 0.85
                );

        double maximumDamage =
                Math.floor(
                        rawDamage
                );

        int estimatedDamage =
                effectiveness == 0.0
                        ? 0
                        : Math.max(
                        1,
                        (int) minimumDamage
                );

        double averageDamage =
                Math.max(
                        0.0,
                        rawDamage * 0.925
                );

        int enemyHp =
                enemy.getCurrentHealth();

        double score =
                averageDamage;

        /*
         * Type effectiveness.
         */
        if (effectiveness == 0.0) {

            score -= 10000.0;

        } else if (effectiveness >= 4.0) {

            score += 470.0;

        } else if (effectiveness >= 2.0) {

            score += 255.0;
            if (isTerastallized(enemy)) {
                score += TERA_SUPER_EFFECTIVE_BONUS;
            }

        } else if (effectiveness < 0.5) {

            score -= 175.0;

        } else if (effectiveness < 1.0) {

            score -= 85.0;

        } else {

            score += 30.0;
        }

        /*
         * STAB.
         */
        if (stab) {
            score += 60.0;
        }

        /*
         * Accuracy.
         *
         * High-power inaccurate moves are still useful, but not when a
         * reliable alternative gets the same result.
         */
        double accuracyMultiplier =
                accuracy / 100.0;

        score *=
                accuracyMultiplier;

        /*
         * Guaranteed KO.
         */
        if (estimatedDamage >= enemyHp
                && accuracy >= CERTAIN_HIT_ACCURACY) {

            score += KO_BONUS;

        } else if (estimatedDamage >= enemyHp) {

            // KO on hit, not a guaranteed KO.
            score += KO_BONUS
                    * accuracyMultiplier
                    * 0.72;

        } else if (averageDamage >= enemyHp) {

            score +=
                    RELIABLE_KO_BONUS;

        } else if (maximumDamage >= enemyHp) {

            /*
             * A possible KO is useful, but much less valuable than
             * a guaranteed KO.
             */
            score += 180.0;
        }

        /*
         * Two-hit KO.
         */
        if (estimatedDamage * 2 >= enemyHp) {
            score += 180.0;
        }

        /*
         * Punish weakened targets.
         */
        if (enemyHp
                <= enemy.getMaxHealth() * 0.25
                && estimatedDamage > 0) {

            score += 145.0;
        }

        /*
         * Don't use resisted damage into a healthy target unless it is
         * otherwise strategically important.
         */
        if (effectiveness < 1.0
                && enemyHp
                > enemy.getMaxHealth() * 0.70) {

            score -= 75.0;
        }

        /*
         * Build/role awareness. Determine the normalized move name and pivot
         * classification here so they are available to both the role scorer
         * and the MoveChoice constructed below.
         */
        String normalizedName =
                normalize(moveName);

        boolean setupMove =
                isSetupMove(normalizedName);

        boolean pivotMove =
                isPivotMove(normalizedName);

        score += roleMoveAdjustment(
                mine,
                enemy,
                normalizedName,
                damageMove,
                pivotMove,
                estimatedDamage
        );

        if (speciesId(mine).equals("genesect")
                && estimatedDamage >= enemyHp
                && accuracy >= CERTAIN_HIT_ACCURACY) {
            score += GENESECT_KO_OVERRIDE_BONUS;
        }

        /*
         * Speed.
         */
        if (mine.getSpeed()
                > enemy.getSpeed()) {

            score += 40.0;

        } else if (mine.getSpeed()
                < enemy.getSpeed()) {

            score -= 30.0;
        }

        /*
         * Priority.
         */
        int priority =
                template.getPriority();

        if (priority > 0) {

            if (mine.getSpeed()
                    < enemy.getSpeed()) {

                score +=
                        90.0 * priority;

            } else if (enemyHp
                    <= mine.getCurrentHealth()
                    * 0.50) {

                score +=
                        45.0 * priority;
            }
        }

        /*
         * Low PP conservation.
         */
        int pp =
                battleMove.getPp();

        if (pp <= 2
                && estimatedDamage < enemyHp
                && effectiveness < 2.0) {

            score -= 30.0;
        }

        /*
         * Endgame cleaner conversion. Priority and speed control become much
         * more valuable once the opposing Pokémon is already in realistic
         * finishing range.
         */
        if (enemyHp <= enemy.getMaxHealth() * 0.35
                && estimatedDamage > 0
                && (priority > 0 || mine.getSpeed() > enemy.getSpeed())) {
            score += CLEANER_ENDGAME_BONUS;
        }

        /*
         * Low-health aggression.
         *
         * When Maestro is nearly dead, trading damage becomes more valuable.
         */
        double myHp =
                getHpPercent(mine);

        if (myHp < 0.20
                && estimatedDamage > 0) {

            score += 65.0;
        }

        /*
         * Accuracy danger.
         */
        if (accuracy < 80.0
                && estimatedDamage < enemyHp) {

            score -=
                    (80.0 - accuracy)
                            * 1.8;
        }

        /*
         * Very inaccurate moves are strongly discouraged if a reliable
         * alternative can already KO.
         */
        if (accuracy < 80.0
                && estimatedDamage >= enemyHp) {

            score -=
                    (80.0 - accuracy)
                            * 0.55;
        }

        return new MoveChoice(
                move,
                moveId,
                moveName,
                moveType.getName(),
                category,
                power,
                score,
                estimatedDamage,
                (int) maximumDamage,
                effectiveness,
                stab,
                accuracy,
                pp,
                priority,
                averageDamage,
                damageMove,
                setupMove,
                pivotMove
        );
    }

    private MoveChoice evaluateNonDamagingMove(
            Move move,
            InBattleMove battleMove,
            String moveId,
            String moveName,
            String category,
            Pokemon mine,
            Pokemon enemy,
            double accuracy
    ) {
        String name =
                normalize(moveName);

        double score =
                15.0;

        double myHp =
                getHpPercent(mine);

        double enemyHp =
                getHpPercent(enemy);

        double incomingThreat =
                getBestIncomingEffectiveness(
                        enemy,
                        mine
                );

        double incomingDamage =
                getBestEstimatedDamage(
                        enemy,
                        mine
                );

        /*
         * Setup.
         */
        if (isSetupMove(name)) {

            score =
                    100.0;

            /*
             * Competitive prior: do not burn setup turns into a likely
             * Unaware wall. Direct observed ability state will eventually
             * override priors once that API is wired into the knowledge layer.
             */
            if (likelyIgnoresOpponentBoosts(enemy)) {
                if (speciesId(mine).equals("xerneas")
                        && name.equals("geomancy")
                        && incomingDamage < mine.getCurrentHealth() * 0.45) {
                    score -= 120.0;
                } else {
                    score -= 620.0;
                }
            }

            /*
             * Consecutive setup is not fresh setup. Repeated setup actions
             * must account for stages already gained by the active actor.
             */
            int priorSetupUses =
                    getConsecutiveSetupUses(mine);

            if (priorSetupUses == 1) {
                score -= REPEATED_SETUP_PENALTY;

            } else if (priorSetupUses >= 2) {
                score -= MAXED_SETUP_PENALTY;
            }

            /*
             * Setup is much stronger when the opponent cannot punish it.
             */
            if (incomingDamage
                    < mine.getCurrentHealth()
                    * 0.25) {

                score += 220.0;

            } else if (incomingDamage
                    < mine.getCurrentHealth()
                    * 0.40) {

                score += 120.0;

            } else if (incomingDamage
                    >= mine.getCurrentHealth()
                    * 0.75) {

                score -= 280.0;
            }

            if (incomingThreat == 0.0) {
                score += 180.0;

            } else if (incomingThreat < 1.0) {
                score += 90.0;

            } else if (incomingThreat >= 2.0) {
                score -= 120.0;
            }

            /*
             * Never setup when the opponent is nearly dead.
             */
            if (enemyHp < 0.25) {
                score -= 140.0;
            }

            /*
             * Never setup at critical HP unless the opponent is incapable
             * of meaningful retaliation.
             */
            if (myHp < 0.25
                    && incomingDamage
                    > mine.getCurrentHealth() * 0.20) {

                score -= 180.0;
            }
        }

        /*
         * Recovery.
         */
        if (isRecoveryMove(name)) {

            score =
                    55.0;

            if (myHp < 0.20) {

                score +=
                        RECOVERY_LOW_HP_BONUS
                                + 80.0;

            } else if (myHp < 0.35) {

                score +=
                        RECOVERY_LOW_HP_BONUS;

            } else if (myHp < 0.55) {

                score += 95.0;

            } else if (myHp > 0.80) {

                score -=
                        RECOVERY_HIGH_HP_PENALTY;
            }

            /*
             * Recovery is bad if the opponent can simply KO.
             */
            if (incomingDamage
                    >= mine.getCurrentHealth()) {

                score -= 300.0;
            }

            /*
             * Don't heal while the opponent is already in easy KO range.
             */
            if (enemyHp < 0.20) {
                score -= 130.0;
            }

            /*
             * Recovery is particularly valuable when the opponent
             * struggles to damage Maestro.
             */
            if (incomingThreat == 0.0) {
                score += 140.0;

            } else if (incomingThreat < 1.0) {
                score += 75.0;
            }
        }

        /*
         * Status.
         */
        if (isStatusMove(name)) {

            score =
                    70.0;

            /*
             * Most one-target status plans should not be repeated into a
             * target that is already statused. Setup/recovery moves are
             * excluded because they are classified separately as strategic
             * status-category moves rather than opponent status attempts.
             */
            if (!isSetupMove(name)
                    && !isRecoveryMove(name)
                    && enemy.getStatus() != null) {
                score -= STATUS_MOVE_ON_STATUS_TARGET_PENALTY;
            }

            /*
             * Status becomes much less attractive against weakened targets.
             */
            if (enemyHp < 0.25) {
                score -= 130.0;

            } else if (enemyHp < 0.40) {
                score -= 55.0;
            }

            /*
             * Dangerous situations favor immediate damage or switching.
             */
            if (incomingThreat >= 2.0
                    && myHp < 0.45) {

                score -= 100.0;
            }

            /*
             * Status is more attractive when Maestro has a safe turn.
             */
            if (incomingDamage
                    < mine.getCurrentHealth() * 0.25) {

                score += 95.0;
            }

            /*
             * Don't status instead of an obvious KO.
             */
            if (hasGuaranteedKO(
                    movesForPokemon(
                            mine,
                            enemy
                    ),
                    enemy
            )) {

                score -= 600.0;
            }
        }

        /*
         * Protect / Detect.
         */
        if (name.contains("protect")
                || name.contains("detect")) {

            score =
                    20.0;

            if (myHp < 0.15) {
                score += 35.0;
            }

            if (incomingDamage
                    >= mine.getCurrentHealth()) {

                score += 70.0;
            }

            /*
             * Protecting at high HP against a weak opponent is usually
             * a waste of the turn.
             */
            if (myHp > 0.80
                    && incomingDamage
                    < mine.getCurrentHealth() * 0.20) {

                score -= 60.0;
            }
        }

        /*
         * Pivot moves.
         */
        if (isPivotMove(name)) {

            score += 65.0;

            if (incomingThreat >= 2.0) {
                score += 110.0;
            }

            if (incomingThreat >= 4.0) {
                score += 150.0;
            }

            if (incomingThreat < 1.0) {
                score += 20.0;
            }

            if (enemyHp < 0.25) {
                score -= 70.0;
            }
        }

        /*
         * Apply build-role knowledge to status/setup/recovery moves too.
         * These role bonuses existed before but were unreachable from the
         * non-damaging evaluator.
         */
        double roleAdjustment =
                roleMoveAdjustment(
                        mine,
                        enemy,
                        name,
                        false,
                        isPivotMove(name),
                        0
                );

        if (isSetupMove(name)
                && incomingDamage
                >= mine.getCurrentHealth() * ROLE_SETUP_SAFETY_GATE) {
            roleAdjustment = Math.min(0.0, roleAdjustment);
        }

        score += roleAdjustment;
        score += setupSweepProjection(mine, enemy, name, incomingDamage);

        /*
         * Accuracy.
         */
        score *=
                accuracy / 100.0;

        if (battleMove.getPp() <= 2) {
            score -= 15.0;
        }

        return new MoveChoice(
                move,
                moveId,
                moveName,
                getMoveTypeName(
                        move,
                        mine
                ),
                category,
                move.getPower(),
                score,
                0,
                0,
                1.0,
                hasType(
                        mine,
                        getMoveType(
                                move,
                                mine
                        )
                ),
                accuracy,
                battleMove.getPp(),
                move.getTemplate()
                        .getPriority(),
                0.0,
                false,
                isSetupMove(name),
                isPivotMove(name)
        );
    }

    private double evaluateCurrentPosition(
            Pokemon mine,
            Pokemon enemy,
            MoveChoice bestMove
    ) {
        double score =
                bestMove.finalScore > 0.0
                        ? bestMove.finalScore
                        : bestMove.score;

        double incomingThreat =
                getBestIncomingEffectiveness(
                        enemy,
                        mine
                );

        double incomingDamage =
                getBestEstimatedDamage(
                        enemy,
                        mine
                );

        double hp =
                getHpPercent(mine);

        /*
         * Strong offensive matchup.
         */
        if (bestMove.effectiveness >= 2.0) {
            score += 95.0;
        }

        /*
         * Guaranteed KO.
         */
        if (isGuaranteedKO(bestMove, enemy)) {

            score += 450.0;
        }

        /*
         * Defensive matchup.
         */
        if (incomingThreat == 0.0) {

            score += 160.0;

        } else if (incomingThreat < 1.0) {

            score += 80.0;

        } else if (incomingThreat >= 4.0) {

            score -= 300.0;

        } else if (incomingThreat >= 2.0) {

            score -= 190.0;
        }

        /*
         * Actual incoming damage matters more than raw typing.
         */
        double incomingRatio =
                incomingDamage
                        / Math.max(
                        1.0,
                        mine.getCurrentHealth()
                );

        if (incomingRatio >= 1.0) {

            score -= 400.0;

        } else if (incomingRatio >= 0.75) {

            score -= 280.0;

        } else if (incomingRatio >= 0.50) {

            score -= 170.0;

        } else if (incomingRatio >= 0.30) {

            score -= 75.0;
        }

        /*
         * HP.
         */
        if (hp < 0.15) {

            score -= 180.0;

        } else if (hp < 0.30) {

            score -= 100.0;

        } else if (hp > 0.75) {

            score += 30.0;
        }

        /*
         * Speed. Raw speed is not complete speed control when the opposing
         * species commonly carries damaging priority.
         */
        if (mine.getSpeed()
                > enemy.getSpeed()) {

            score += 35.0;

            if (likelyHasPriority(enemy)
                    && !isGuaranteedKO(bestMove, enemy)) {
                score -= PRIORITY_SPEED_SAFETY_PENALTY;
            }

        } else if (mine.getSpeed()
                < enemy.getSpeed()) {

            score -= 40.0;
        }

        if (likelySetupSweeper(enemy)
                && !isGuaranteedKO(bestMove, enemy)) {
            score -= SETUP_SWEEPER_PRESSURE;
        }

        /*
         * A fast guaranteed KO is extremely strong.
         */
        if (mine.getSpeed()
                > enemy.getSpeed()
                && isGuaranteedKO(bestMove, enemy)) {

            score += 250.0;
        }

        return score;
    }

    private SwitchChoice findBestSwitch(
            ActiveBattlePokemon activePokemon,
            ActiveBattlePokemon opponent
    ) {
        if (activePokemon == null
                || activePokemon.getActor() == null
                || opponent == null
                || opponent.getBattlePokemon() == null) {
            return null;
        }

        Pokemon enemy =
                opponent.getBattlePokemon()
                        .getEffectedPokemon();

        if (enemy == null) {
            return null;
        }

        List<BattlePokemon> team =
                activePokemon.getActor()
                        .getPokemonList();

        if (team == null
                || team.isEmpty()) {
            return null;
        }

        SwitchChoice best = null;

        for (BattlePokemon candidateBattle :
                team) {

            if (candidateBattle == null
                    || candidateBattle.getUuid() == null) {
                continue;
            }

            if (candidateBattle.getUuid()
                    .equals(
                            activePokemon
                                    .getBattlePokemon()
                                    .getUuid()
                    )) {
                continue;
            }

            if (!candidateBattle.canBeSentOut()) {
                continue;
            }

            if (candidateBattle.getGone()) {
                continue;
            }

            if (candidateBattle.getHealth() <= 0) {
                continue;
            }

            if (candidateBattle.isSentOut()) {
                continue;
            }

            Pokemon candidate =
                    candidateBattle
                            .getEffectedPokemon();

            if (candidate == null
                    || candidate.getCurrentHealth() <= 0) {
                continue;
            }

            double offensiveDamage =
                    getBestEstimatedDamage(
                            candidate,
                            enemy
                    );

            double incomingDamage =
                    getBestEstimatedDamage(
                            enemy,
                            candidate
                    );

            double offensivePotential =
                    getBestOffensiveEffectiveness(
                            candidate,
                            enemy
                    );

            double incomingThreat =
                    getBestIncomingEffectiveness(
                            enemy,
                            candidate
                    );

            double hp =
                    getHpPercent(candidate);

            double score =
                    100.0;

            /*
             * Actual offensive pressure.
             */
            double enemyHp =
                    Math.max(
                            1.0,
                            enemy.getCurrentHealth()
                    );

            double offensiveRatio =
                    offensiveDamage / enemyHp;

            if (offensiveDamage
                    >= enemy.getCurrentHealth()) {

                score += 330.0;

            } else if (offensiveRatio >= 0.75) {

                score += 250.0;

            } else if (offensiveRatio >= 0.50) {

                score += 150.0;

            } else if (offensiveRatio >= 0.25) {

                score += 65.0;

            } else {

                score -= 80.0;
            }

            /*
             * Type matchup remains useful, but it is secondary to
             * actual estimated damage.
             */
            if (offensivePotential >= 4.0) {

                score += 210.0;

            } else if (offensivePotential >= 2.0) {

                score += 135.0;

            } else if (offensivePotential < 0.5) {

                score -= 100.0;
            }

            /*
             * Actual incoming damage.
             */
            double candidateHp =
                    Math.max(
                            1.0,
                            candidate.getCurrentHealth()
                    );

            double incomingRatio =
                    incomingDamage / candidateHp;

            if (incomingDamage <= 0.0) {

                score +=
                        SWITCHING_INTO_IMMUNITY_BONUS;

            } else if (incomingRatio < 0.25) {

                score += 230.0;

            } else if (incomingRatio < 0.50) {

                score += 145.0;

            } else if (incomingRatio < 0.75) {

                score += 35.0;

            } else if (incomingDamage
                    >= candidate.getCurrentHealth()) {

                score -=
                        BAD_SWITCH_PENALTY
                                + 170.0;

            } else {

                score -=
                        BAD_SWITCH_PENALTY;
            }

            /*
             * Type-only defensive information.
             */
            if (incomingThreat == 0.0) {

                score +=
                        SWITCHING_INTO_IMMUNITY_BONUS;

            } else if (incomingThreat < 1.0) {

                score +=
                        SWITCHING_INTO_RESISTANCE_BONUS;

            } else if (incomingThreat >= 4.0) {

                score -= 250.0;

            } else if (incomingThreat >= 2.0) {

                score -= 160.0;
            }

            /*
             * Repeatedly revisiting a matchup is evidence that raw switch
             * scores may be creating churn.
             */
            int visits =
                    getMatchupVisits(candidate, enemy);

            if (visits >= 2) {
                score -= WALL_CHURN_PENALTY * Math.min(3, visits - 1);

                if (likelyIgnoresOpponentBoosts(enemy)) {
                    score -= WALL_RETURN_PENALTY * Math.min(2, visits - 1);
                }
            }

            if (likelyIgnoresOpponentBoosts(enemy)
                    && offensiveRatio >= 0.50) {
                score += WALL_PROGRESS_BONUS;
            }

            if (likelyIgnoresOpponentBoosts(enemy)
                    && offensiveRatio >= 0.35
                    && incomingRatio < 0.55) {
                score += WALL_BREAKER_PROGRESS_BONUS;
            }

            /*
             * HP preservation.
             */
            if (hp >= 0.80) {

                score += 50.0;

            } else if (hp >= 0.50) {

                score += 25.0;

            } else if (hp < 0.25) {

                score -= 100.0;
            }

            /*
             * Team-resource awareness: preserve valuable cleaners/pivots and
             * reward explicit anti-setup answers when the opponent is a setup
             * sweeper. This makes switch scoring consider the role of the
             * incoming Pokémon, not only raw type/damage numbers.
             */
            if (isWinCondition(candidate)
                    && incomingRatio >= 0.50) {
                score -= WIN_CONDITION_PRESERVATION;
            }

            if (isWinCondition(candidate)
                    && hp < 0.30
                    && incomingRatio >= 0.35
                    && offensiveDamage < enemy.getCurrentHealth()) {
                score -= LOW_HP_WINCON_PRESERVATION;
            }

            if (likelySetupSweeper(enemy)
                    && hasMove(candidate, "spectralthief")) {
                score += ANTI_SETUP_SWITCH_BONUS;
            }

            /*
             * Entry conversion: a switch is more valuable when the incoming
             * Pokémon can exploit the resulting matchup to establish a real
             * win condition instead of merely surviving one turn.
             */
            double entryIncomingRatio =
                    incomingDamage / Math.max(1.0, candidate.getCurrentHealth());

            if (entryIncomingRatio < 0.45) {
                if (speciesId(candidate).equals("xerneas")
                        && hasMove(candidate, "geomancy")) {
                    score += 310.0;
                }

                if (speciesId(candidate).equals("mewtwo")
                        && hasMove(candidate, "nastyplot")) {
                    score += 230.0;
                }
            }

            /*
             * Speed.
             */
            if (candidate.getSpeed()
                    > enemy.getSpeed()) {

                score += 70.0;

                /*
                 * Do not overvalue a nominally faster switch when the opponent
                 * commonly bypasses speed with priority.
                 */
                if (likelyHasPriority(enemy)
                        && offensiveDamage < enemy.getCurrentHealth()) {
                    score -= PRIORITY_SPEED_SAFETY_PENALTY;
                }

            } else if (candidate.getSpeed()
                    < enemy.getSpeed()) {

                score -= 35.0;
            }

            /*
             * If the candidate can immediately KO, this is an excellent
             * switch even if it takes some damage.
             */
            if (offensiveDamage
                    >= enemy.getCurrentHealth()) {

                score += 270.0;
            }

            /*
             * If candidate can only barely scratch the opponent while
             * taking huge damage, reject the switch.
             */
            if (offensiveRatio < 0.25
                    && incomingRatio >= 0.75) {

                score -= 260.0;
            }

            /*
             * Prefer switches that enter a position where Maestro can
             * actually continue attacking rather than merely surviving.
             */
            if (offensiveRatio >= 0.50
                    && incomingRatio < 0.50) {

                score += 150.0;
            }

            SwitchChoice choice =
                    new SwitchChoice(
                            candidateBattle,
                            score,
                            incomingThreat,
                            offensivePotential,
                            offensiveDamage,
                            incomingDamage
                    );

            if (best == null
                    || choice.score > best.score) {

                best = choice;
            }
        }

        return best;
    }

    private double evaluateSwitchPosition(
            ActiveBattlePokemon activePokemon,
            ActiveBattlePokemon opponent,
            SwitchChoice choice
    ) {
        if (choice == null) {
            return -10000.0;
        }

        Pokemon candidate =
                choice.pokemon
                        .getEffectedPokemon();

        if (candidate == null) {
            return -10000.0;
        }

        Pokemon enemy =
                opponent.getBattlePokemon()
                        .getEffectedPokemon();

        if (enemy == null) {
            return choice.score;
        }

        double score =
                choice.score;

        double offensiveRatio =
                choice.offensiveDamage
                        / Math.max(
                        1.0,
                        enemy.getCurrentHealth()
                );

        double incomingRatio =
                choice.incomingDamage
                        / Math.max(
                        1.0,
                        candidate.getCurrentHealth()
                );

        /*
         * Switching should be especially attractive when the incoming
         * Pokémon creates an immediate threat of its own.
         */
        if (offensiveRatio >= 1.0) {

            score += 260.0;

        } else if (offensiveRatio >= 0.75) {

            score += 150.0;

        } else if (offensiveRatio >= 0.50) {

            score += 80.0;
        }

        /*
         * A switch that simply moves into another losing matchup is bad.
         */
        if (incomingRatio >= 1.0) {

            score -= 400.0;

        } else if (incomingRatio >= 0.75) {

            score -= 260.0;

        } else if (incomingRatio >= 0.50) {

            score -= 130.0;
        }

        /*
         * Fast offensive switch.
         */
        if (candidate.getSpeed()
                > enemy.getSpeed()
                && offensiveRatio >= 0.50) {

            score += 100.0;
        }

        return score;
    }

    private void applyPivotDestinationValue(
            List<MoveChoice> moves,
            Pokemon mine,
            Pokemon enemy,
            SwitchChoice bestSwitch
    ) {
        if (moves == null
                || mine == null
                || enemy == null
                || bestSwitch == null) {
            return;
        }

        double switchValue =
                Math.max(
                        -200.0,
                        bestSwitch.score
                );

        for (MoveChoice choice : moves) {
            if (choice == null
                    || !choice.pivotMove) {
                continue;
            }

            /*
             * A damaging pivot is most valuable when it can safely act before
             * the opponent. Scarf-style fast pivots therefore naturally gain
             * value without hardcoding "Genesect must U-turn".
             */
            boolean canActFirst =
                    choice.priority > 0
                            || mine.getSpeed() > enemy.getSpeed();

            double incoming =
                    getBestEstimatedDamage(
                            enemy,
                            mine
                    );

            boolean survivesToPivot =
                    canActFirst
                            || incoming * DANGEROUS_PIVOT_SURVIVAL_MARGIN
                            < mine.getCurrentHealth();

            if (!survivesToPivot) {
                continue;
            }

            double destination =
                    Math.max(
                            0.0,
                            switchValue
                    ) * PIVOT_POSITION_SHARE;

            /*
             * A pivot reaches the same destination after dealing chip, so a
             * strong hard-switch destination should mostly carry across to
             * the pivot line rather than only contributing a small fraction.
             */
            double adjustment =
                    SAFE_PIVOT_OVER_SWITCH_BONUS
                            + Math.min(PIVOT_DESTINATION_CAP, destination);

            if (choice.averageDamage > 0.0) {
                adjustment +=
                        Math.min(
                                180.0,
                                choice.averageDamage * PIVOT_CHIP_VALUE
                        );
            }

            if (canActFirst) {
                adjustment += PIVOT_FAST_BONUS;
            } else {
                adjustment -= PIVOT_SLOW_RISK_PENALTY;
            }

            if (speciesId(mine).equals("genesect")) {
                adjustment += GENESECT_SCARF_PIVOT_BONUS;
                if (getHpPercent(mine) >= 0.35) {
                    adjustment += GENESECT_SPEED_CONTROL_PRESERVATION;
                }
            }

            choice.strategicAdjustment = adjustment;

            System.out.println(
                    "[MaestroAI] Intelligence: pivot line "
                            + choice.moveName
                            + " | destination="
                            + String.format("%.2f", destination)
                            + " | persistentBonus="
                            + String.format("%.2f", adjustment)
            );
        }
    }

    private boolean currentPokemonWouldDieBeforePivot(
            Pokemon mine,
            Pokemon enemy,
            MoveChoice pivot
    ) {
        if (mine == null || enemy == null || pivot == null) {
            return true;
        }
        boolean actsFirst = pivot.priority > 0 || mine.getSpeed() > enemy.getSpeed();
        return !actsFirst && getBestEstimatedDamage(enemy, mine) >= mine.getCurrentHealth();
    }

    private double predictedSwitchSetupPenalty(
            Pokemon enemy,
            SwitchChoice bestSwitch
    ) {
        if (enemy == null
                || bestSwitch == null
                || !likelySetupSweeper(enemy)) {
            return 0.0;
        }

        Pokemon candidate =
                bestSwitch.pokemon == null
                        ? null
                        : bestSwitch.pokemon.getEffectedPokemon();

        double penalty =
                SWITCH_SETUP_PUNISH_PENALTY;

        /*
         * If the incoming Pokémon carries Spectral Thief, the opponent's
         * boost is less free because Maestro can steal it next turn.
         */
        if (candidate != null
                && hasMove(candidate, "spectralthief")) {
            penalty *= 0.38;
        }

        /*
         * Sucker Punch + setup is especially dangerous to an incoming
         * offensive answer: the opponent can boost on the switch and then
         * threaten conditional priority.
         */
        if (likelyHasSuckerPunch(enemy)
                && candidate != null
                && getHpPercent(candidate) < 0.75) {
            penalty += 95.0;
        }

        return penalty;
    }

    private double roleMoveAdjustment(
            Pokemon mine,
            Pokemon enemy,
            String moveName,
            boolean damageMove,
            boolean pivotMove,
            int estimatedDamage
    ) {
        if (mine == null || enemy == null) {
            return 0.0;
        }

        String species =
                speciesId(mine);

        double adjustment =
                0.0;

        if (pivotMove) {
            adjustment += 80.0;

            if (getHpPercent(mine) >= 0.45) {
                adjustment += 35.0;
            }
        }

        /*
         * Maestro's known sets: encode what the build is trying to accomplish,
         * while still allowing the general evaluator to override the role.
         */
        if (species.equals("genesect")) {
            if (pivotMove) {
                adjustment += 95.0;
            }

            if (estimatedDamage >= enemy.getCurrentHealth()) {
                adjustment += 90.0;
            }
        }

        if (species.equals("calyrexshadow")) {
            if (damageMove) {
                adjustment += 70.0;
            }

            if (getHpPercent(mine) < 0.35
                    && estimatedDamage < enemy.getCurrentHealth()) {
                adjustment -= 55.0;
            }
        }

        if (species.equals("xerneas")
                && normalize(moveName).equals("geomancy")) {
            /*
             * Power-Herb Geomancy is the defining setup line of this build.
             * It still obeys danger/Unaware/repeated-setup checks elsewhere.
             */
            adjustment += XERNEAS_GEOMANCY_WINCON_BONUS;
        }

        if (species.equals("arceus")) {
            if (normalize(moveName).equals("recover")
                    && getHpPercent(mine) < 0.55) {
                adjustment += 90.0;
            }

            if (normalize(moveName).equals("extremespeed")
                    && getHpPercent(enemy) < 0.40) {
                adjustment += 85.0;
            }
        }

        if (species.equals("mewtwo")
                && normalize(moveName).equals("nastyplot")
                && getHpPercent(mine) > 0.55) {
            adjustment += MEWTWO_NASTY_PLOT_WINCON_BONUS;
        }

        if (species.equals("marshadow")) {
            if (normalize(moveName).equals("spectralthief")
                    && likelySetupSweeper(enemy)) {
                adjustment += 180.0;
            }

            if (normalize(moveName).equals("shadowsneak")
                    && getHpPercent(enemy) < 0.30) {
                adjustment += 90.0;
            }
        }

        return adjustment;
    }

    private boolean hasMove(
            Pokemon pokemon,
            String moveName
    ) {
        if (pokemon == null
                || pokemon.getMoveSet() == null
                || moveName == null) {
            return false;
        }

        String wanted =
                normalize(moveName);

        for (Move move : pokemon.getMoveSet()) {
            if (move == null
                    || move.getTemplate() == null
                    || move.getTemplate().getName() == null) {
                continue;
            }

            if (normalize(
                    move.getTemplate().getName()
            ).equals(wanted)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasUsablePriority(Pokemon pokemon) {
        if (pokemon == null || pokemon.getMoveSet() == null) {
            return false;
        }

        for (Move move : pokemon.getMoveSet()) {
            if (move == null || move.getTemplate() == null) {
                continue;
            }
            if (move.getTemplate().getPriority() > 0
                    && move.getTemplate().getPower() > 0
                    && move.getCurrentPp() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isWinCondition(
            Pokemon pokemon
    ) {
        String species =
                speciesId(pokemon);

        return species.equals("calyrexshadow")
                || species.equals("xerneas")
                || species.equals("arceus")
                || species.equals("mewtwo")
                || species.equals("marshadow");
    }

    private boolean likelyHasSuckerPunch(
            Pokemon pokemon
    ) {
        String species =
                speciesId(pokemon);

        return species.equals("kingambit")
                || species.equals("bisharp")
                || species.equals("cacturne")
                || species.equals("absol")
                || species.equals("honchkrow")
                || species.equals("mawile")
                || species.equals("shiftry");
    }

    private double estimateSuckerPunchDamage(
            Pokemon attacker,
            Pokemon defender
    ) {
        if (attacker == null || defender == null) {
            return 0.0;
        }

        int attack =
                Math.max(
                        1,
                        attacker.getAttack()
                );

        int defence =
                Math.max(
                        1,
                        defender.getDefence()
                );

        double effectiveness =
                1.0;

        /*
         * Dark-type, 70 BP, physical. This estimate intentionally excludes
         * hidden item/ability multipliers until live item state is exposed.
         */
        ElementalType darkType =
                findTypeByName(
                        attacker,
                        "dark"
                );

        if (darkType != null) {
            effectiveness =
                    getTypeEffectivenessAgainstPokemon(
                            darkType,
                            defender
                    );
        }

        if (effectiveness <= 0.0) {
            return 0.0;
        }

        double damage =
                (((2.0 * Math.max(1, attacker.getLevel()) / 5.0 + 2.0)
                        * 70.0
                        * attack
                        / defence)
                        / 50.0)
                        + 2.0;

        damage *= getStabMultiplier(attacker, darkType);

        damage *= effectiveness;

        return damage * 0.925;
    }

    private ElementalType findTypeByName(
            Pokemon pokemon,
            String typeName
    ) {
        if (pokemon == null || typeName == null) {
            return null;
        }

        if (pokemon.getPrimaryType() != null
                && normalize(
                pokemon.getPrimaryType().getName()
        ).equals(normalize(typeName))) {
            return pokemon.getPrimaryType();
        }

        if (pokemon.getSecondaryType() != null
                && normalize(
                pokemon.getSecondaryType().getName()
        ).equals(normalize(typeName))) {
            return pokemon.getSecondaryType();
        }

        /*
         * For Sucker Punch users that are not Dark type, a null type falls
         * back to neutral effectiveness rather than inventing a registry API.
         */
        return null;
    }

    private boolean shouldPreserveCurrentPokemon(
            ActiveBattlePokemon activePokemon,
            Pokemon mine,
            Pokemon enemy,
            MoveChoice bestMove,
            SwitchChoice bestSwitch
    ) {
        if (activePokemon == null
                || mine == null
                || enemy == null
                || bestMove == null
                || bestSwitch == null) {
            return false;
        }

        /*
         * Never preserve something instead of a guaranteed KO.
         */
        if (isGuaranteedKO(bestMove, enemy)) {
            return false;
        }

        /*
         * If the current Pokémon is healthy and has meaningful offensive
         * pressure, don't throw it away unnecessarily.
         */
        if (getHpPercent(mine) >= 0.70
                && bestMove.estimatedDamage
                >= enemy.getCurrentHealth() * 0.50) {

            return false;
        }

        /*
         * If switching creates a dramatically safer position,
         * preserving the current Pokémon can be worthwhile.
         */
        double currentIncoming =
                getBestEstimatedDamage(
                        enemy,
                        mine
                );

        double currentHp =
                Math.max(
                        1.0,
                        mine.getCurrentHealth()
                );

        double currentRatio =
                currentIncoming / currentHp;

        double switchIncomingRatio =
                bestSwitch.incomingDamage
                        / Math.max(
                        1.0,
                        bestSwitch.pokemon
                                .getHealth()
                );

        if (currentRatio >= 0.75
                && switchIncomingRatio < 0.40) {

            return true;
        }

        /*
         * At very low HP, keeping a Pokémon alive is valuable only if it
         * has meaningful future utility.
         */
        if (getHpPercent(mine) < 0.20
                && switchIncomingRatio < 0.50
                && bestSwitch.offensiveDamage
                >= enemy.getCurrentHealth() * 0.35) {

            return true;
        }

        return false;
    }

    private boolean isGuaranteedKO(
            MoveChoice choice,
            Pokemon enemy
    ) {
        return choice != null
                && enemy != null
                && choice.damageMove
                && choice.accuracy >= CERTAIN_HIT_ACCURACY
                && choice.estimatedDamage >= enemy.getCurrentHealth();
    }

    private boolean isKOOnHit(
            MoveChoice choice,
            Pokemon enemy
    ) {
        return choice != null
                && enemy != null
                && choice.damageMove
                && choice.estimatedDamage >= enemy.getCurrentHealth();
    }

    private boolean isWinningPosition(
            Pokemon mine,
            Pokemon enemy,
            MoveChoice bestMove
    ) {
        if (mine == null || enemy == null || bestMove == null) {
            return false;
        }

        if (isGuaranteedKO(bestMove, enemy)) {
            return true;
        }

        double damageRatio =
                bestMove.averageDamage
                        / Math.max(1.0, enemy.getCurrentHealth());

        double incomingRatio =
                getBestEstimatedDamage(enemy, mine)
                        / Math.max(1.0, mine.getCurrentHealth());

        boolean strongPressure =
                damageRatio >= WINNING_POSITION_DAMAGE_RATIO;

        boolean defensivelyStable =
                incomingRatio < WINNING_POSITION_INCOMING_RATIO;

        boolean speedControl =
                bestMove.priority > 0
                        || (
                        mine.getSpeed() >= enemy.getSpeed()
                                && !likelyHasPriority(enemy)
                );

        return strongPressure
                && defensivelyStable
                && (speedControl || getHpPercent(mine) >= 0.65);
    }

    private MoveChoice selectControlledVariation(
            List<MoveChoice> moves,
            MoveChoice best
    ) {
        if (best == null
                || moves == null
                || moves.size() < 2) {
            return best;
        }

        MoveChoice second =
                null;

        for (MoveChoice choice : moves) {

            if (choice == null
                    || choice == best) {
                continue;
            }

            if (second == null
                    || choice.finalScore
                    > second.finalScore) {

                second = choice;
            }
        }

        if (second == null) {
            return best;
        }

        /*
         * Never intentionally throw away a huge advantage.
         */
        double difference =
                best.finalScore
                        - second.finalScore;

        if (difference > 45.0) {
            return best;
        }

        /*
         * Controlled deterministic-ish variation based on action count
         * and move hashes. No global RNG is needed.
         */
        int actionCount =
                getActionCountForMove(best.moveId);

        int selector =
                Math.abs(
                        (
                                best.moveId.hashCode()
                                        + actionCount * 31
                                        + second.moveId.hashCode()
                        )
                ) % 7;

        /*
         * Only occasionally choose the second move.
         */
        if (selector == 0
                || selector == 1) {

            System.out.println(
                    "[MaestroAI] Close decision: using alternate line."
            );

            System.out.println(
                    "[MaestroAI] Best score: "
                            + String.format(
                            "%.2f",
                            best.finalScore
                    )
            );

            System.out.println(
                    "[MaestroAI] Alternate score: "
                            + String.format(
                            "%.2f",
                            second.finalScore
                    )
            );

            return second;
        }

        return best;
    }

    private int getActionCountForMove(
            String moveId
    ) {
        if (moveId == null) {
            return 0;
        }

        /*
         * Use real battle action history as the changing component. The old
         * implementation derived this entirely from the move hash, so its
         * "variation" was effectively fixed for a given pair of moves.
         */
        int totalActions = 0;

        for (Integer count : actionCounts.values()) {
            if (count != null) {
                totalActions += count;
            }
        }

        return totalActions;
    }

    private double getBestEstimatedDamage(
            Pokemon attacker,
            Pokemon defender
    ) {
        if (attacker == null
                || defender == null
                || attacker.getMoveSet() == null) {
            return 0.0;
        }

        /*
         * Imposter/Transform fallback. Cobblemon can leave Ditto's underlying
         * move set as Transform while the battle engine is actually using
         * copied moves. Mirror the copied target's offensive ceiling instead
         * of incorrectly treating Ditto as zero incoming damage.
         */
        if (isDitto(attacker)
                && !isDitto(defender)) {

            double mirrored =
                    getBestEstimatedDamage(
                            defender,
                            defender
                    );

            if (mirrored > 0.0) {
                return mirrored;
            }

            return Math.max(
                    1.0,
                    defender.getCurrentHealth() * 0.45
            );
        }

        double best =
                0.0;

        for (Move move :
                attacker.getMoveSet()) {

            if (move == null
                    || move.getTemplate() == null) {
                continue;
            }

            if (move.getCurrentPp() <= 0) {
                continue;
            }

            MoveTemplate template =
                    move.getTemplate();

            double power =
                    template.getPower();

            if (power <= 0.0) {
                continue;
            }

            ElementalType type =
                    template.getEffectiveElementalType(
                            attacker
                    );

            if (type == null) {
                type =
                        template.getElementalType();
            }

            if (type == null) {
                continue;
            }

            String category =
                    getCategory(template);

            int attack;
            int defence;

            if (category.contains("physical")) {

                attack =
                        attacker.getAttack();

                defence =
                        defender.getDefence();

            } else if (category.contains("special")) {

                attack =
                        attacker.getSpecialAttack();

                defence =
                        defender.getSpecialDefence();

            } else {

                continue;
            }

            attack =
                    Math.max(
                            1,
                            attack
                    );

            defence =
                    Math.max(
                            1,
                            defence
                    );

            double effectiveness =
                    getTypeEffectivenessAgainstPokemon(
                            type,
                            defender
                    );

            if (effectiveness <= 0.0) {
                continue;
            }

            double damage =
                    (((2.0
                            * Math.max(
                            1,
                            attacker.getLevel()
                    )
                            / 5.0 + 2.0)
                            * power
                            * attack
                            / defence)
                            / 50.0)
                            + 2.0;

            damage *= getStabMultiplier(attacker, type);
            damage *= effectiveness;

            double accuracy =
                    template.getAccuracy();

            if (accuracy > 0.0) {
                damage *=
                        accuracy / 100.0;
            }

            if (damage > best) {
                best = damage;
            }
        }

        /*
         * If a competitive threat has no damaging move visible through the
         * current API, do not collapse its danger estimate to literal zero.
         * The floor is deliberately conservative and only affects opponent
         * modeling / switch safety.
         */
        if (best <= 0.0
                && isKnownCompetitiveThreat(attacker)) {

            double floor =
                    isHighPressureThreat(attacker)
                            ? HIGH_THREAT_FLOOR
                            : UNKNOWN_THREAT_FLOOR;

            best =
                    defender.getCurrentHealth()
                            * floor;
        }

        return best;
    }

    private double getBestOffensiveEffectiveness(
            Pokemon attacker,
            Pokemon defender
    ) {
        if (attacker == null
                || defender == null
                || attacker.getMoveSet() == null) {
            return 0.0;
        }

        if (isDitto(attacker)
                && !isDitto(defender)) {

            return Math.max(
                    1.0,
                    getBestOffensiveEffectiveness(
                            defender,
                            defender
                    )
            );
        }

        double best =
                0.0;

        for (Move move :
                attacker.getMoveSet()) {

            if (move == null
                    || move.getTemplate() == null) {
                continue;
            }

            if (move.getCurrentPp() <= 0) {
                continue;
            }

            MoveTemplate template =
                    move.getTemplate();

            if (template.getPower() <= 0.0) {
                continue;
            }

            ElementalType type =
                    template.getEffectiveElementalType(
                            attacker
                    );

            if (type == null) {
                type =
                        template.getElementalType();
            }

            if (type == null) {
                continue;
            }

            double effectiveness =
                    getTypeEffectivenessAgainstPokemon(
                            type,
                            defender
                    );

            if (effectiveness > best) {
                best =
                        effectiveness;
            }
        }

        return best;
    }

    private double getBestIncomingEffectiveness(
            Pokemon attacker,
            Pokemon defender
    ) {
        if (attacker == null
                || defender == null
                || attacker.getMoveSet() == null) {
            return 1.0;
        }

        if (isDitto(attacker)
                && !isDitto(defender)) {

            return Math.max(
                    1.0,
                    getBestOffensiveEffectiveness(
                            defender,
                            defender
                    )
            );
        }

        double best =
                0.0;

        for (Move move :
                attacker.getMoveSet()) {

            if (move == null
                    || move.getTemplate() == null) {
                continue;
            }

            if (move.getCurrentPp() <= 0) {
                continue;
            }

            MoveTemplate template =
                    move.getTemplate();

            if (template.getPower() <= 0.0) {
                continue;
            }

            ElementalType type =
                    template.getEffectiveElementalType(
                            attacker
                    );

            if (type == null) {
                type =
                        template.getElementalType();
            }

            if (type == null) {
                continue;
            }

            double effectiveness =
                    getTypeEffectivenessAgainstPokemon(
                            type,
                            defender
                    );

            if (effectiveness > best) {
                best =
                        effectiveness;
            }
        }

        /*
         * Unknown-move safety. A known competitive threat with no damaging
         * move visible is not an immunity. Treat the matchup as neutral until
         * the battle gives us better evidence.
         */
        if (best <= 0.0
                && isKnownCompetitiveThreat(attacker)) {
            best = 1.0;
        }

        return best;
    }

    private boolean hasGuaranteedKO(
            List<MoveChoice> moves,
            Pokemon enemy
    ) {
        if (moves == null
                || enemy == null) {
            return false;
        }

        for (MoveChoice choice : moves) {

            if (choice == null) {
                continue;
            }

            if (isGuaranteedKO(choice, enemy)) {
                return true;
            }
        }

        return false;
    }

    private List<MoveChoice> movesForPokemon(
            Pokemon mine,
            Pokemon enemy
    ) {
        List<MoveChoice> choices =
                new ArrayList<>();

        if (mine == null
                || mine.getMoveSet() == null
                || enemy == null) {

            return choices;
        }

        for (Move move :
                mine.getMoveSet()) {

            if (move == null
                    || move.getTemplate() == null) {
                continue;
            }

            if (move.getCurrentPp() <= 0) {
                continue;
            }

            MoveTemplate template =
                    move.getTemplate();

            double power =
                    template.getPower();

            if (power <= 0.0) {
                continue;
            }

            ElementalType type =
                    template.getEffectiveElementalType(
                            mine
                    );

            if (type == null) {
                type =
                        template.getElementalType();
            }

            if (type == null) {
                continue;
            }

            String category =
                    getCategory(template);

            int attack;
            int defence;

            if (category.contains("physical")) {

                attack =
                        mine.getAttack();

                defence =
                        enemy.getDefence();

            } else if (category.contains("special")) {

                attack =
                        mine.getSpecialAttack();

                defence =
                        enemy.getSpecialDefence();

            } else {

                continue;
            }

            double effectiveness =
                    getTypeEffectivenessAgainstPokemon(
                            type,
                            enemy
                    );

            if (effectiveness <= 0.0) {
                continue;
            }

            double damage =
                    (((2.0
                            * Math.max(
                            1,
                            mine.getLevel()
                    )
                            / 5.0 + 2.0)
                            * power
                            * Math.max(
                            1,
                            attack
                    )
                            / Math.max(
                            1,
                            defence
                    ))
                            / 50.0)
                            + 2.0;

            damage *= getStabMultiplier(mine, type);
            damage *= effectiveness;

            double accuracy =
                    template.getAccuracy();

            if (accuracy <= 0.0) {
                accuracy = 100.0;
            }

            if (damage * 0.85
                    >= enemy.getCurrentHealth()
                    && accuracy >= CERTAIN_HIT_ACCURACY) {

                MoveChoice choice =
                        new MoveChoice(
                                move,
                                template.getName(),
                                template.getName(),
                                type.getName(),
                                category,
                                power,
                                1000.0,
                                (int) Math.floor(
                                        damage * 0.85
                                ),
                                (int) Math.floor(
                                        damage
                                ),
                                effectiveness,
                                getStabMultiplier(mine, type) > 1.0,
                                accuracy <= 0.0
                                        ? 100.0
                                        : accuracy,
                                move.getCurrentPp(),
                                template.getPriority(),
                                damage,
                                true,
                                isSetupMove(
                                        normalize(
                                                template.getName()
                                        )
                                ),
                                isPivotMove(
                                        normalize(
                                                template.getName()
                                        )
                                )
                        );

                choices.add(choice);
            }
        }

        return choices;
    }

    private String findUsableStruggle(
            ShowdownMoveset moveset
    ) {
        if (moveset == null
                || moveset.getMoves() == null) {
            return null;
        }

        for (InBattleMove battleMove :
                moveset.getMoves()) {

            if (battleMove == null) {
                continue;
            }

            String id =
                    battleMove.getId();

            if (id == null) {
                continue;
            }

            if (!normalize(id)
                    .equals("struggle")) {
                continue;
            }

            if (battleMove.canBeUsed()) {
                return id;
            }
        }

        return null;
    }

    private Move findMove(
            Pokemon pokemon,
            String moveId
    ) {
        if (pokemon == null
                || pokemon.getMoveSet() == null
                || moveId == null) {
            return null;
        }

        String normalized =
                normalize(moveId);

        for (Move move :
                pokemon.getMoveSet()) {

            if (move == null
                    || move.getTemplate() == null) {
                continue;
            }

            String name =
                    move.getTemplate()
                            .getName();

            if (name != null
                    && normalize(name)
                    .equals(normalized)) {

                return move;
            }
        }

        return null;
    }

    private String getCategory(
            MoveTemplate template
    ) {
        if (template == null
                || template.getDamageCategory() == null
                || template.getDamageCategory()
                .getName() == null) {

            return "unknown";
        }

        return template.getDamageCategory()
                .getName()
                .toLowerCase();
    }

    private double getHpPercent(
            Pokemon pokemon
    ) {
        if (pokemon == null) {
            return 0.0;
        }

        return (double) pokemon.getCurrentHealth()
                / Math.max(
                1,
                pokemon.getMaxHealth()
        );
    }

    private ElementalType getMoveType(
            Move move,
            Pokemon pokemon
    ) {
        if (move == null
                || move.getTemplate() == null) {
            return null;
        }

        MoveTemplate template =
                move.getTemplate();

        ElementalType type =
                template.getEffectiveElementalType(
                        pokemon
                );

        if (type == null) {
            type =
                    template.getElementalType();
        }

        return type;
    }

    private String getMoveTypeName(
            Move move,
            Pokemon pokemon
    ) {
        ElementalType type =
                getMoveType(
                        move,
                        pokemon
                );

        if (type == null) {
            return "unknown";
        }

        return type.getName();
    }

    private ActiveBattlePokemon getOpponent(
            ActiveBattlePokemon activePokemon
    ) {
        Targetable target =
                activePokemon.getOppositeOpponent();

        if (target instanceof ActiveBattlePokemon opponent) {
            return opponent;
        }

        return null;
    }

    private void resetMemoryForNewBattle(PokemonBattle battle) {
        if (battle == null || battle.getBattleId() == null) {
            return;
        }

        UUID battleId = battle.getBattleId();
        if (battleId.equals(currentBattleId)) {
            return;
        }

        currentBattleId = battleId;
        lastSelectedMoves.clear();
        publicProtocolObserver.reset();
        publicProtocolEvidence.reset();
        mechanicsSnapshot = null;
        mechanicsOpponentSlot = null;
        mechanicsEvidence = null;
        mechanicsProfiles = Map.of();
        publicMechanicsEvidence.reset();
        publicFieldEvidence.reset(); fieldCapture = null; mechanicsExecution = null;
        observedShowdownMessageCount = 0;
        observedShowdownHistoryFingerprint = 0;
        shadowSwitchTelemetry.reset();
        shadowObservedBattle = null;
        opponentObservedMoves.clear();
        lastObservedMajorMove.clear();
        actionCounts.clear();
        consecutiveSetupBySpecies.clear();
        matchupVisits.clear();
        activeTeraTypes.clear();
        lastMatchupKey = "";
        lastSwitchDestination = null;
        lastSwitchTurn = -100;

        System.out.println("[MaestroAI] Intelligence: new battle memory initialized");
    }

    private void rememberSwitchDestination(
            PokemonBattle battle,
            SwitchChoice choice
    ) {
        if (battle == null || choice == null || choice.pokemon == null) {
            return;
        }
        lastSwitchDestination = choice.pokemon.getUuid();
        lastSwitchTurn = battle.getTurn();
    }

    /**
     * Cobblemon's MoveInstruction records public |move| messages in
     * majorBattleActions. Collect only moves that were actually broadcast;
     * never read hidden opponent movesets. The map is a latest-action cache,
     * so this history is incomplete if multiple moves occur between AI turns.
     */
    private final PublicBattleCapture shadowReplayCapture = new PublicBattleCapture();

    /**
     * Process only allowlisted public events from Cobblemon's Showdown
     * message history. This shadow projection is not yet authoritative:
     * the slot-to-Pokemon UUID mapping must be verified before injecting
     * stages into live battle snapshots.
     */
    private void syncPublicProtocolObservation(PokemonBattle battle) {
        try {
            if (battle == null || battle.getShowdownMessages() == null) return;
            if (shadowObservedBattle != battle) {
                shadowObservedBattle = battle;
                observedShowdownMessageCount = 0;
                observedShowdownHistoryFingerprint = 0;
                publicProtocolObserver.reset();
                publicProtocolEvidence.reset();
                shadowSwitchTelemetry.reset();
            }
            List<String> history = battle.getShowdownMessages();
            // Size alone cannot detect a replaced or edited public message history.
            int historyFingerprint = history.hashCode();
            if (history.size() == observedShowdownMessageCount
                    && historyFingerprint == observedShowdownHistoryFingerprint) return;
            if (historyFingerprint != observedShowdownHistoryFingerprint
                    && history.size() <= observedShowdownMessageCount)
                shadowSwitchTelemetry.reset();
            if (history.size() < observedShowdownMessageCount) {
                // A truncated or replaced public history invalidates prior shadow telemetry.
                shadowSwitchTelemetry.reset();
            }
            // Refilter the entire history each time. A split's private and
            // public lines may cross batch boundaries; delta-only filtering
            // could mistake the private line for a normal public event.
            // The evidence projection independently detects append-only history
            // violations and incomplete private/public split blocks.
            publicProtocolEvidence.update(history);
            publicMechanicsEvidence.update(publicProtocolEvidence);
            fieldCapture = publicFieldEvidence.update(publicProtocolEvidence, publicMechanicsEvidence);
            List<String> publicEvents = PublicProtocolFilter.publicEvents(history);
            // Diagnostics only: no tactical action or opponent model reads this.
            PublicBattleCapture.Observation capture =
                    shadowReplayCapture.observeWithProvenance(battle.getBattleId(), history);
            for (String replayLine : capture.replay())
                System.out.println(replayLine);
            if (ControlledBattleCaptureRegistry.enabled()) {
                ControlledBattleCaptureRegistry.observe(battle, shadowReplayCapture);
                // No raw split payload, request body, UUID, nickname or winner.
                for (String metadata : capture.envelopes())
                    System.out.println(metadata);
            }
            publicProtocolObserver.reset();
            for (String line : publicEvents) publicProtocolObserver.accept(line);
            observedShowdownMessageCount = history.size();
            observedShowdownHistoryFingerprint = historyFingerprint;
            System.out.println("[MaestroAI] Shadow public protocol"
                    + " acceptedEvents=" + publicEvents.size()
                    + " cursor=" + observedShowdownMessageCount
                    + " stageMapping=UNVERIFIED");
        } catch (RuntimeException exception) {
            System.out.println("[MaestroAI] Shadow public protocol skipped: "
                    + exception.getClass().getSimpleName());
        }
    }

    private void syncPublicMoveHistory(PokemonBattle battle) {
        if (battle == null || battle.getMajorBattleActions() == null) return;
        for (Map.Entry<UUID, BattleMessage> entry : battle.getMajorBattleActions().entrySet()) {
            UUID pokemonId = entry.getKey();
            BattleMessage message = entry.getValue();
            if (pokemonId == null || message == null || !"move".equals(normalize(message.getId()))) {
                continue;
            }
            var moveEffect = message.effectAt(1);
            if (moveEffect == null || moveEffect.getId() == null) continue;
            String moveId = normalize(moveEffect.getId());
            if (moveId.isBlank() || moveId.equals("struggle")) continue;
            // Record observed move identity; repeated use is not a new revelation.
            List<String> known = opponentObservedMoves.computeIfAbsent(
                    pokemonId, ignored -> new ArrayList<>());
            if (!known.contains(moveId)) known.add(moveId);
            lastObservedMajorMove.put(pokemonId, moveId);
        }
    }

    private void syncObservedTeraState(PokemonBattle battle) {
        if (battle == null || battle.getMinorBattleActions() == null) {
            return;
        }

        for (Map.Entry<UUID, BattleMessage> entry :
                battle.getMinorBattleActions().entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || !normalize(entry.getValue().getId()).equals("terastallize")) {
                continue;
            }

            Pokemon found = findPokemonByUuid(battle, entry.getKey());
            ElementalType tera = elementalTeraType(found);
            if (tera != null) {
                activeTeraTypes.put(entry.getKey(), tera);
            }
        }
    }

    private Pokemon findPokemonByUuid(PokemonBattle battle, UUID uuid) {
        if (battle == null || uuid == null || battle.getActors() == null) {
            return null;
        }

        for (var actor : battle.getActors()) {
            if (actor == null || actor.getPokemonList() == null) {
                continue;
            }
            for (BattlePokemon battlePokemon : actor.getPokemonList()) {
                if (battlePokemon != null && uuid.equals(battlePokemon.getUuid())) {
                    return battlePokemon.getEffectedPokemon();
                }
            }
        }
        return null;
    }

    private ElementalType elementalTeraType(Pokemon pokemon) {
        if (pokemon == null) {
            return null;
        }
        TeraType teraType = pokemon.getTeraType();
        if (teraType instanceof ElementalTypeTeraType elemental) {
            return elemental.getType();
        }
        return null;
    }

    private boolean isTerastallized(Pokemon pokemon) {
        return pokemon != null
                && pokemon.getUuid() != null
                && activeTeraTypes.containsKey(pokemon.getUuid());
    }

    private ElementalType activeTeraType(Pokemon pokemon) {
        return isTerastallized(pokemon)
                ? activeTeraTypes.get(pokemon.getUuid())
                : null;
    }

    private double getTypeEffectivenessAgainstPokemon(
            ElementalType attackingType,
            Pokemon defender
    ) {
        if (defender == null) {
            return 1.0;
        }

        ElementalType tera = activeTeraType(defender);
        if (tera != null) {
            return getTypeEffectiveness(attackingType, tera, null);
        }

        return getTypeEffectiveness(
                attackingType,
                defender.getPrimaryType(),
                defender.getSecondaryType()
        );
    }

    private double getStabMultiplier(Pokemon attacker, ElementalType moveType) {
        if (attacker == null || moveType == null) {
            return 1.0;
        }

        boolean originalStab = hasType(attacker, moveType);
        ElementalType tera = activeTeraType(attacker);

        if (tera == null) {
            return originalStab ? 1.5 : 1.0;
        }

        boolean teraStab = sameType(tera, moveType);
        if (teraStab && originalStab) {
            return 2.0;
        }
        if (teraStab || originalStab) {
            return 1.5;
        }
        return 1.0;
    }

    private double setupSweepProjection(
            Pokemon mine,
            Pokemon enemy,
            String moveName,
            double incomingDamage
    ) {
        if (mine == null || enemy == null || moveName == null) {
            return 0.0;
        }

        double incomingRatio =
                incomingDamage / Math.max(1.0, mine.getCurrentHealth());

        if (incomingRatio >= 0.55) {
            return 0.0;
        }

        String species = speciesId(mine);
        String move = normalize(moveName);
        double bonus = SAFE_SETUP_SWEEP_BONUS;

        if (species.equals("xerneas") && move.equals("geomancy")) {
            bonus += 220.0;
            if (incomingRatio < 0.35) {
                bonus += 120.0;
            }
        }

        if (species.equals("mewtwo") && move.equals("nastyplot")) {
            bonus += 180.0;
            if (mine.getSpeed() > enemy.getSpeed()) {
                bonus += 90.0;
            }
        }

        return bonus;
    }

    private boolean hasType(
            Pokemon pokemon,
            ElementalType type
    ) {
        if (pokemon == null
                || type == null) {
            return false;
        }

        return sameType(
                pokemon.getPrimaryType(),
                type
        ) || sameType(
                pokemon.getSecondaryType(),
                type
        );
    }

    private boolean sameType(
            ElementalType first,
            ElementalType second
    ) {
        if (first == null
                || second == null) {
            return false;
        }

        return normalize(
                first.getName()
        ).equals(
                normalize(
                        second.getName()
                )
        );
    }

    private double getTypeEffectiveness(
            ElementalType attackingType,
            ElementalType primary,
            ElementalType secondary
    ) {
        if (attackingType == null) {
            return 1.0;
        }

        String attack =
                normalize(
                        attackingType.getName()
                );

        double multiplier =
                1.0;

        if (primary != null) {

            multiplier *=
                    singleTypeEffectiveness(
                            attack,
                            normalize(
                                    primary.getName()
                            )
                    );
        }

        if (secondary != null) {

            multiplier *=
                    singleTypeEffectiveness(
                            attack,
                            normalize(
                                    secondary.getName()
                            )
                    );
        }

        return multiplier;
    }

    private double singleTypeEffectiveness(
            String attack,
            String defence
    ) {
        switch (attack) {

            case "normal":
                if (defence.equals("rock")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                if (defence.equals("ghost")) {
                    return 0.0;
                }

                return 1.0;

            case "fire":
                if (defence.equals("grass")
                        || defence.equals("ice")
                        || defence.equals("bug")
                        || defence.equals("steel")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("water")
                        || defence.equals("rock")
                        || defence.equals("dragon")) {
                    return 0.5;
                }

                return 1.0;

            case "water":
                if (defence.equals("fire")
                        || defence.equals("ground")
                        || defence.equals("rock")) {
                    return 2.0;
                }

                if (defence.equals("water")
                        || defence.equals("grass")
                        || defence.equals("dragon")) {
                    return 0.5;
                }

                return 1.0;

            case "electric":
                if (defence.equals("water")
                        || defence.equals("flying")) {
                    return 2.0;
                }

                if (defence.equals("electric")
                        || defence.equals("grass")
                        || defence.equals("dragon")) {
                    return 0.5;
                }

                if (defence.equals("ground")) {
                    return 0.0;
                }

                return 1.0;

            case "grass":
                if (defence.equals("water")
                        || defence.equals("ground")
                        || defence.equals("rock")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("grass")
                        || defence.equals("poison")
                        || defence.equals("flying")
                        || defence.equals("bug")
                        || defence.equals("dragon")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            case "ice":
                if (defence.equals("grass")
                        || defence.equals("ground")
                        || defence.equals("flying")
                        || defence.equals("dragon")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("water")
                        || defence.equals("ice")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            case "fighting":
                if (defence.equals("normal")
                        || defence.equals("ice")
                        || defence.equals("rock")
                        || defence.equals("dark")
                        || defence.equals("steel")) {
                    return 2.0;
                }

                if (defence.equals("poison")
                        || defence.equals("flying")
                        || defence.equals("psychic")
                        || defence.equals("bug")
                        || defence.equals("fairy")) {
                    return 0.5;
                }

                if (defence.equals("ghost")) {
                    return 0.0;
                }

                return 1.0;

            case "poison":
                if (defence.equals("grass")
                        || defence.equals("fairy")) {
                    return 2.0;
                }

                if (defence.equals("poison")
                        || defence.equals("ground")
                        || defence.equals("rock")
                        || defence.equals("ghost")) {
                    return 0.5;
                }

                if (defence.equals("steel")) {
                    return 0.0;
                }

                return 1.0;

            case "ground":
                if (defence.equals("fire")
                        || defence.equals("electric")
                        || defence.equals("poison")
                        || defence.equals("rock")
                        || defence.equals("steel")) {
                    return 2.0;
                }

                if (defence.equals("grass")
                        || defence.equals("bug")) {
                    return 0.5;
                }

                if (defence.equals("flying")) {
                    return 0.0;
                }

                return 1.0;

            case "flying":
                if (defence.equals("grass")
                        || defence.equals("fighting")
                        || defence.equals("bug")) {
                    return 2.0;
                }

                if (defence.equals("electric")
                        || defence.equals("rock")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            case "psychic":
                if (defence.equals("fighting")
                        || defence.equals("poison")) {
                    return 2.0;
                }

                if (defence.equals("psychic")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                if (defence.equals("dark")) {
                    return 0.0;
                }

                return 1.0;

            case "bug":
                if (defence.equals("grass")
                        || defence.equals("psychic")
                        || defence.equals("dark")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("fighting")
                        || defence.equals("poison")
                        || defence.equals("flying")
                        || defence.equals("ghost")
                        || defence.equals("steel")
                        || defence.equals("fairy")) {
                    return 0.5;
                }

                return 1.0;

            case "rock":
                if (defence.equals("fire")
                        || defence.equals("ice")
                        || defence.equals("flying")
                        || defence.equals("bug")) {
                    return 2.0;
                }

                if (defence.equals("fighting")
                        || defence.equals("ground")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            case "ghost":
                if (defence.equals("psychic")
                        || defence.equals("ghost")) {
                    return 2.0;
                }

                if (defence.equals("dark")) {
                    return 0.5;
                }

                if (defence.equals("normal")) {
                    return 0.0;
                }

                return 1.0;

            case "dragon":
                if (defence.equals("dragon")) {
                    return 2.0;
                }

                if (defence.equals("steel")) {
                    return 0.5;
                }

                if (defence.equals("fairy")) {
                    return 0.0;
                }

                return 1.0;

            case "dark":
                if (defence.equals("psychic")
                        || defence.equals("ghost")) {
                    return 2.0;
                }

                if (defence.equals("fighting")
                        || defence.equals("dark")
                        || defence.equals("fairy")) {
                    return 0.5;
                }

                return 1.0;

            case "steel":
                if (defence.equals("ice")
                        || defence.equals("rock")
                        || defence.equals("fairy")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("water")
                        || defence.equals("electric")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            case "fairy":
                if (defence.equals("fighting")
                        || defence.equals("dragon")
                        || defence.equals("dark")) {
                    return 2.0;
                }

                if (defence.equals("fire")
                        || defence.equals("poison")
                        || defence.equals("steel")) {
                    return 0.5;
                }

                return 1.0;

            default:
                return 1.0;
        }
    }

    private boolean isSetupMove(
            String name
    ) {
        return name.contains("swordsdance")
                || name.contains("nastyplot")
                || name.contains("dragondance")
                || name.contains("quiverdance")
                || name.contains("calmmind")
                || name.contains("bulkup")
                || name.contains("shellsmash")
                || name.contains("agility")
                || name.contains("rockpolish")
                || name.contains("curse")
                || name.contains("honeclaws")
                || name.contains("shiftgear")
                || name.contains("geomancy")
                || name.contains("cosmicpower")
                || name.contains("irondefense")
                || name.contains("amnesia")
                || name.contains("workup")
                || name.contains("tailglow")
                || name.contains("bellydrum");
    }

    private boolean isRecoveryMove(
            String name
    ) {
        return name.contains("recover")
                || name.contains("roost")
                || name.contains("moonlight")
                || name.contains("morningsun")
                || name.contains("slackoff")
                || name.contains("softboiled")
                || name.contains("rest")
                || name.contains("milkdrink")
                || name.contains("wish")
                || name.contains("shoreup")
                || name.contains("synthesis")
                || name.contains("strengthsap");
    }

    private boolean isStatusMove(
            String name
    ) {
        return name.contains("toxic")
                || name.contains("willowisp")
                || name.contains("thunderwave")
                || name.contains("spore")
                || name.contains("sleeppowder")
                || name.contains("stunspore")
                || name.contains("hypnosis")
                || name.contains("yawn")
                || name.contains("poisonpowder")
                || name.contains("glare")
                || name.contains("confuseray")
                || name.contains("taunt")
                || name.contains("encore")
                || name.contains("leechseed")
                || name.contains("destinybond")
                || name.contains("disable")
                || name.contains("torment")
                || name.contains("trick")
                || name.contains("switcheroo");
    }

    private boolean isPivotMove(
            String name
    ) {
        return name.contains("uturn")
                || name.contains("voltswitch")
                || name.contains("flipturn")
                || name.contains("partingshot")
                || name.contains("teleport");
    }

    private int getConsecutiveSetupUses(
            Pokemon pokemon
    ) {
        if (pokemon == null) {
            return 0;
        }

        return consecutiveSetupBySpecies.getOrDefault(
                speciesId(pokemon),
                0
        );
    }

    private void rememberStrategicAction(
            Pokemon pokemon,
            MoveChoice choice
    ) {
        if (pokemon == null || choice == null) {
            return;
        }

        String key = speciesId(pokemon);

        if (key.isBlank()) {
            return;
        }

        if (choice.setupMove) {
            consecutiveSetupBySpecies.put(
                    key,
                    getConsecutiveSetupUses(pokemon) + 1
            );
        } else {
            consecutiveSetupBySpecies.remove(key);
        }
    }

    private void clearSetupHistory(
            Pokemon pokemon
    ) {
        if (pokemon == null) {
            return;
        }

        consecutiveSetupBySpecies.remove(
                speciesId(pokemon)
        );
    }

    private String lastSelectedMoveForSpecies(
            Pokemon pokemon
    ) {
        if (pokemon == null || pokemon.getUuid() == null) {
            return null;
        }

        return lastSelectedMoves.get(pokemon.getUuid());
    }

    private void rememberMove(
            BattlePokemon pokemon,
            String moveId
    ) {
        if (pokemon == null
                || pokemon.getUuid() == null
                || moveId == null) {
            return;
        }

        lastSelectedMoves.put(
                pokemon.getUuid(),
                moveId
        );
    }

    private void clearRememberedMove(
            BattlePokemon pokemon
    ) {
        if (pokemon == null
                || pokemon.getUuid() == null) {
            return;
        }

        lastSelectedMoves.remove(
                pokemon.getUuid()
        );
    }

    private void rememberMatchupVisit(
            Pokemon mine,
            Pokemon enemy
    ) {
        String key =
                speciesId(mine) + "->" + speciesId(enemy);

        if (key.equals("->") || key.equals(lastMatchupKey)) {
            return;
        }

        lastMatchupKey = key;
        matchupVisits.put(
                key,
                matchupVisits.getOrDefault(key, 0) + 1
        );
    }

    private int getMatchupVisits(
            Pokemon mine,
            Pokemon enemy
    ) {
        if (mine == null || enemy == null) {
            return 0;
        }

        String key =
                speciesId(mine) + "->" + speciesId(enemy);

        return matchupVisits.getOrDefault(key, 0);
    }

    private void incrementActionCount(
            BattlePokemon pokemon
    ) {
        if (pokemon == null
                || pokemon.getUuid() == null) {
            return;
        }

        UUID uuid =
                pokemon.getUuid();

        actionCounts.put(
                uuid,
                actionCounts.getOrDefault(
                        uuid,
                        0
                ) + 1
        );
    }

    private String effectiveTypeLabel(Pokemon pokemon) {
        ElementalType tera = activeTeraType(pokemon);
        return tera == null
                ? typeLabel(pokemon)
                : "Tera-" + tera.getName();
    }

    private String typeLabel(Pokemon pokemon) {
        if (pokemon == null) {
            return "unknown";
        }

        String primary = pokemon.getPrimaryType() == null
                ? "unknown"
                : pokemon.getPrimaryType().getName();

        String secondary = pokemon.getSecondaryType() == null
                ? null
                : pokemon.getSecondaryType().getName();

        if (secondary == null
                || normalize(primary).equals(normalize(secondary))) {
            return primary;
        }

        return primary + "/" + secondary;
    }

    /*
     * Competitive knowledge layer. These are high-confidence priors used
     * only when the current Cobblemon-facing API does not expose the hidden
     * state needed by the evaluator. This layer is intentionally isolated so
     * generated Smogon/Showdown knowledge can replace hand-maintained rules.
     */
    private String speciesId(
            Pokemon pokemon
    ) {
        if (pokemon == null
                || pokemon.getSpecies() == null
                || pokemon.getSpecies().getName() == null) {
            return "";
        }

        return normalize(
                pokemon.getSpecies().getName()
        );
    }

    private boolean isDitto(
            Pokemon pokemon
    ) {
        return speciesId(pokemon)
                .equals("ditto");
    }

    private boolean likelyIgnoresOpponentBoosts(
            Pokemon pokemon
    ) {
        String species = speciesId(pokemon);

        /*
         * Species with Unaware as a major competitive ability. This remains
         * probabilistic: Maestro is cautious about setup rather than pretending
         * the hidden ability has been revealed.
         */
        return species.equals("dondozo")
                || species.equals("skeledirge")
                || species.equals("clodsire")
                || species.equals("quagsire")
                || species.equals("clefable");
    }

    private boolean likelyHasPriority(
            Pokemon pokemon
    ) {
        String species = speciesId(pokemon);

        return species.equals("kingambit")
                || species.equals("dragonite")
                || species.equals("rillaboom")
                || species.equals("scizor")
                || species.equals("breloom")
                || species.equals("bisharp")
                || species.equals("mamoswine")
                || species.equals("weavile")
                || species.equals("azumarill")
                || species.equals("marshadow")
                || species.equals("cinderace")
                || species.equals("urshifu")
                || species.equals("urshifurapidstrike")
                || species.equals("rayquaza");
    }

    private boolean likelySetupSweeper(
            Pokemon pokemon
    ) {
        String species = speciesId(pokemon);

        return species.equals("kingambit")
                || species.equals("dragonite")
                || species.equals("volcarona")
                || species.equals("roaringmoon")
                || species.equals("ironvaliant")
                || species.equals("garchomp")
                || species.equals("gyarados")
                || species.equals("rayquaza")
                || species.equals("xerneas")
                || species.equals("arceus");
    }

    private boolean isKnownCompetitiveThreat(
            Pokemon pokemon
    ) {
        String species = speciesId(pokemon);

        return isDitto(pokemon)
                || likelyHasPriority(pokemon)
                || likelySetupSweeper(pokemon)
                || likelyIgnoresOpponentBoosts(pokemon)
                || species.equals("dragapult")
                || species.equals("gengar")
                || species.equals("mewtwo")
                || species.equals("calyrexshadow")
                || species.equals("genesect");
    }

    private boolean isHighPressureThreat(
            Pokemon pokemon
    ) {
        String species = speciesId(pokemon);

        return likelySetupSweeper(pokemon)
                || species.equals("calyrexshadow")
                || species.equals("mewtwo")
                || species.equals("marshadow")
                || species.equals("dragapult");
    }

    private String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }

    private void logMoveDecision(
            PokemonBattle battle,
            Pokemon mine,
            Pokemon enemy,
            MoveChoice choice,
            SwitchChoice bestSwitch
    ) {
        System.out.println(
                "[MaestroAI] ===== Maestro Decision ====="
        );

        System.out.println(
                "[MaestroAI] Turn: "
                        + battle.getTurn()
        );

        System.out.println(
                "[MaestroAI] "
                        + mine.getSpecies().getName()
                        + " -> opponent (public identity in Shadow observation)"
        );

        System.out.println(
                "[MaestroAI] Selected move: "
                        + choice.moveName
        );

        System.out.println(
                "[MaestroAI] Battle move ID: "
                        + choice.moveId
        );

        System.out.println(
                "[MaestroAI] Move type: "
                        + choice.moveType
        );

        System.out.println(
                "[MaestroAI] Damage category: "
                        + choice.category
        );

        System.out.println(
                "[MaestroAI] Base power: "
                        + choice.power
        );

        System.out.println(
                "[MaestroAI] Priority: "
                        + choice.priority
        );

        System.out.println(
                "[MaestroAI] PP remaining: "
                        + choice.pp
        );

        System.out.println(
                "[MaestroAI] Score: "
                        + String.format(
                        "%.2f",
                        choice.score
                )
        );

        System.out.println(
                "[MaestroAI] Final 2-ply score: "
                        + String.format(
                        "%.2f",
                        choice.finalScore
                )
        );

        if (choice.strategicAdjustment != 0.0) {
            System.out.println(
                    "[MaestroAI] Strategic adjustment: "
                            + String.format(
                            "%.2f",
                            choice.strategicAdjustment
                    )
            );
        }

        System.out.println(
                "[MaestroAI] Minimum damage: "
                        + choice.estimatedDamage
        );

        System.out.println(
                "[MaestroAI] Maximum damage: "
                        + choice.maximumDamage
        );

        System.out.println(
                "[MaestroAI] Average damage: "
                        + String.format(
                        "%.2f",
                        choice.averageDamage
                )
        );

        System.out.println(
                "[MaestroAI] Effectiveness: "
                        + String.format(
                        "%.2fx",
                        choice.effectiveness
                )
        );

        System.out.println(
                "[MaestroAI] STAB: "
                        + (
                        choice.stab
                                ? "YES"
                                : "NO"
                )
        );

        System.out.println(
                "[MaestroAI] Accuracy: "
                        + String.format(
                        "%.1f%%",
                        choice.accuracy
                )
        );

        System.out.println(
                "[MaestroAI] Opponent HP: "
                        + enemy.getCurrentHealth()
                        + "/"
                        + enemy.getMaxHealth()
        );

        System.out.println(
                "[MaestroAI] Effective matchup types: "
                        + effectiveTypeLabel(mine)
                        + " vs "
                        + effectiveTypeLabel(enemy)
        );

        if (isTerastallized(mine) || isTerastallized(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: live Tera state active"
                            + " | mine=" + effectiveTypeLabel(mine)
                            + " | enemy=" + effectiveTypeLabel(enemy)
            );
        }

        if (isDitto(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: active Ditto/Imposter threat model"
            );
        }

        if (likelyIgnoresOpponentBoosts(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: probable Unaware wall; setup discounted"
            );
        }

        if (likelyHasPriority(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: probable priority; raw speed advantage discounted"
            );
        }

        if (likelyHasSuckerPunch(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: conditional priority model active (Sucker Punch)"
            );
        }

        if (likelySetupSweeper(enemy)) {
            System.out.println(
                    "[MaestroAI] Intelligence: setup-sweeper pressure active"
            );
        }

        if (isGuaranteedKO(choice, enemy)) {

            System.out.println(
                    "[MaestroAI] Threat assessment: GUARANTEED KO"
            );

        } else if (isKOOnHit(choice, enemy)) {

            System.out.println(
                    "[MaestroAI] Threat assessment: KO ON HIT — "
                            + String.format("%.1f%%", choice.accuracy)
            );

        } else if (choice.maximumDamage
                >= enemy.getCurrentHealth()) {

            System.out.println(
                    "[MaestroAI] Threat assessment: POSSIBLE KO"
            );

        } else if (choice.estimatedDamage * 2
                >= enemy.getCurrentHealth()) {

            System.out.println(
                    "[MaestroAI] Threat assessment: TWO-HIT KO"
            );
        }

        if (choice.setupMove) {

            System.out.println(
                    "[MaestroAI] Classification: SETUP"
            );

            System.out.println(
                    "[MaestroAI] Intelligence: consecutive setup count = "
                            + getConsecutiveSetupUses(mine)
            );
        }

        if (choice.pivotMove) {

            System.out.println(
                    "[MaestroAI] Classification: PIVOT"
            );

            System.out.println(
                    "[MaestroAI] Pivot diagnostics: "
                            + (mine.getSpeed() > enemy.getSpeed()
                            ? "acts-first"
                            : "may-act-second")
                            + " | matchupVisits="
                            + getMatchupVisits(mine, enemy)
            );
        }

        String previousMove =
                lastSelectedMoveForSpecies(mine);

        if (previousMove != null) {
            System.out.println(
                    "[MaestroAI] Memory: last selected move = "
                            + previousMove
                            + " | matchup visits = "
                            + getMatchupVisits(mine, enemy)
            );
        }

        if (bestSwitch != null) {

            Pokemon switchPokemon =
                    bestSwitch.pokemon
                            .getEffectedPokemon();

            if (switchPokemon != null) {

                System.out.println(
                        "[MaestroAI] Best switch: "
                                + switchPokemon
                                .getSpecies()
                                .getName()
                                + " | Score: "
                                + String.format(
                                "%.2f",
                                bestSwitch.score
                        )
                                + " | Incoming: "
                                + String.format(
                                "%.2fx",
                                bestSwitch.currentThreat
                        )
                                + " | Offensive: "
                                + String.format(
                                "%.2fx",
                                bestSwitch
                                        .offensivePotential
                        )
                                + " | Damage taken: "
                                + String.format(
                                "%.1f",
                                bestSwitch.incomingDamage
                        )
                                + " | Damage dealt: "
                                + String.format(
                                "%.1f",
                                bestSwitch.offensiveDamage
                        )
                );
            }
        }

        System.out.println(
                "[MaestroAI] ========================="
        );
    }

    private void logSwitchDecision(
            PokemonBattle battle,
            Pokemon mine,
            Pokemon enemy,
            SwitchChoice choice,
            String reason
    ) {
        Pokemon switchingTo =
                choice.pokemon
                        .getEffectedPokemon();

        System.out.println(
                "[MaestroAI] ===== Maestro Switch ====="
        );

        System.out.println(
                "[MaestroAI] Turn: "
                        + battle.getTurn()
        );

        System.out.println(
                "[MaestroAI] "
                        + mine.getSpecies().getName()
                        + " -> opponent (public identity in Shadow observation)"
        );

        System.out.println(
                "[MaestroAI] Switching to: "
                        + (
                        switchingTo == null
                                ? "unknown"
                                : switchingTo
                                .getSpecies()
                                .getName()
                )
        );

        System.out.println(
                "[MaestroAI] Switch score: "
                        + String.format(
                        "%.2f",
                        choice.score
                )
        );

        System.out.println(
                "[MaestroAI] Incoming threat: "
                        + String.format(
                        "%.2fx",
                        choice.currentThreat
                )
        );

        System.out.println(
                "[MaestroAI] Offensive potential: "
                        + String.format(
                        "%.2fx",
                        choice.offensivePotential
                )
        );

        System.out.println(
                "[MaestroAI] Damage dealt potential: "
                        + String.format(
                        "%.1f",
                        choice.offensiveDamage
                )
        );

        System.out.println(
                "[MaestroAI] Damage taken potential: "
                        + String.format(
                        "%.1f",
                        choice.incomingDamage
                )
        );

        System.out.println(
                "[MaestroAI] Reason: "
                        + reason
        );

        System.out.println(
                "[MaestroAI] ========================="
        );
    }

    /**
     * Read-only integration of the live decision point with the training model.
     * No hidden opponent set information, and no speculative switch legality:
     * the only switch exposed here is the one already validated by the
     * existing switch evaluator. The snapshot is diagnostic, not authoritative.
     */
    // Snapshot only this owner's current native UI; retain a supplier for mutation checks.
    private static com.maestroai.training.OwnZMoveEvidence.Choice ownZChoice(ShowdownMoveset moveset) {
        if(moveset==null||moveset.getMoves()==null)return null;
        var moves=new ArrayList<com.maestroai.training.OwnZMoveEvidence.MoveSlot>();
        for(var m:moveset.getMoves()){
            if(m==null||m.getTarget()==null)return null;
            moves.add(new com.maestroai.training.OwnZMoveEvidence.MoveSlot(m.getId(),m.getPp(),m.getDisabled(),m.getTarget().name()));
        }
        var options=new java.util.TreeMap<Integer,com.maestroai.training.OwnZMoveEvidence.ZSlot>();
        var z=moveset.getCanZMove();int count=z==null?0:z.size();
        for(int i=0;i<count;i++)if(z.get(i)!=null){
            var m=z.get(i);if(m.getTarget()==null)return null;
            options.put(i,new com.maestroai.training.OwnZMoveEvidence.ZSlot(m.getMove(),m.getTarget().name(),m.getDisabled()));
        }
        return new com.maestroai.training.OwnZMoveEvidence.Choice(moves,options,count);
    }
    private BattleSnapshot observeTacticalSnapshot(
            PokemonBattle battle,
            ActiveBattlePokemon activePokemon,
            ActiveBattlePokemon opponent,
            Pokemon mine,
            List<MoveChoice> moves,
            boolean voluntarySwitchAllowed,
            boolean continuationLocked,
            boolean requestTrapped, ShowdownMoveset ownMoveset
    ) {
        mechanicsSnapshot = null;
        mechanicsOpponentSlot = null;
        mechanicsEvidence = null;
        mechanicsProfiles = Map.of();
        mechanicsExecution = null;
        try {
            runtimeDomain = null; ownEffectiveStages.clear(); ownReceiptHp.clear();
            var domainUnavailable = java.util.Set.of("INITIALIZED_RUNTIME_NOT_ATTESTED");
            if (ownTransportVersionSupported() && ownChoiceLease != null && ownChoiceLease.current()
                    && com.cobblemon.mod.common.battles.runner.ShowdownService.Companion.getService()
                    instanceof com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService service) {
                var lease = ownChoiceLease;
                if(RuntimeBattleMode.ordinary(battle)){
                    runtimeDomain = SimulatorDomainBridge.capture(service, battle.getBattleId(),
                            ()->lease.current() && RuntimeBattleMode.ordinary(battle));
                    domainUnavailable = SimulatorDomainBridge.unavailable(service);
                }else domainUnavailable=java.util.Set.of("PUBLIC_BATTLE_MODE_UNSUPPORTED_OR_UNVERIFIED");
            }
            System.out.println("[MaestroAI] Shadow initialized domain receipt=" + (runtimeDomain != null)
                    + " supported=" + (runtimeDomain != null && runtimeDomain.effectiveState().known())
                    + " unresolved=" + (runtimeDomain == null ? domainUnavailable : new java.util.TreeSet<>(runtimeDomain.missing()))
                    + " advisoryOnly=true");
            List<String> legalMoves = new ArrayList<>();
            for (MoveChoice choice : moves) {
                if (choice != null && choice.moveId != null && !choice.moveId.isBlank()
                        && !legalMoves.contains(choice.moveId)) {
                    legalMoves.add(choice.moveId);
                }
            }
            // These fields are sourced from Cobblemon MoveTemplate through
            // MoveChoice, matching the UI's public move metadata. They do
            // not imply full support for every secondary move effect.
            for (MoveChoice choice : moves) {
                if (choice == null || choice.moveId == null) continue;
                MoveEffectModel.MoveFacts facts = MoveEffectModel.describe(
                        choice.moveId, choice.moveType, choice.category,
                        choice.power, choice.accuracy, choice.priority);
                System.out.println("[MaestroAI] Shadow move effect"
                        + " id=" + facts.id()
                        + " effect=" + facts.effect()
                        + " detail=" + facts.effectDetail());
                System.out.println("[MaestroAI] Shadow move metadata"
                        + " id=" + choice.moveId
                        + " type=" + choice.moveType
                        + " category=" + choice.category
                        + " power=" + choice.power
                        + " accuracy=" + choice.accuracy
                        + " priority=" + choice.priority
                        + " setup=" + choice.setupMove
                        + " pivot=" + choice.pivotMove);
            }
            List<String> legalSwitches = new ArrayList<>();
            List<BattleSnapshot.Combatant> bench = new ArrayList<>();
            Map<String, PublicMechanicsEvidence.Bound> ownedProfiles = new HashMap<>();
            ownFreshness.clear();
            var domainScopes = runtimeDomain == null ? com.maestroai.training.ExecutionCoverage.Scopes.unknown() : runtimeDomain.scopes();
            // Rebuild the public capture for this choice before adding an
            // independently owned, source-bound permanent-form witness.
            fieldCapture=publicFieldEvidence.update(publicProtocolEvidence,publicMechanicsEvidence);
            fieldCapture=ownTransport.rebindOwnZUse(ownChoiceLease,runtimeDomain,publicProtocolEvidence,fieldCapture);
            fieldCapture=ownTransport.rebindOwnMegaY(ownChoiceLease,runtimeDomain,publicProtocolEvidence,
                    publicMechanicsEvidence.observation(activePokemon.getPNX()),fieldCapture);
            fieldCapture=ownTransport.rebindOwnReentry(ownChoiceLease,runtimeDomain,publicProtocolEvidence,
                    publicMechanicsEvidence.observation(activePokemon.getPNX()),fieldCapture);
            var scope = new PublicMechanicsEvidence.Scope(domainScopes.get(com.maestroai.training.ExecutionCoverage.Operation.DAMAGE),
                    fieldCapture == null ? PublicTransitionState.Known.unknown() : fieldCapture.trickRoom());
            ownedProfiles.put("self", ownMechanicsProfile("self", activePokemon.getBattlePokemon(), false));
            if (voluntarySwitchAllowed && activePokemon.getActor() != null
                    && activePokemon.getActor().getPokemonList() != null) {
                for (BattlePokemon candidateBattle : activePokemon.getActor().getPokemonList()) {
                    if (candidateBattle == null || candidateBattle.getUuid() == null
                            || candidateBattle.getUuid().equals(activePokemon.getBattlePokemon().getUuid())
                            || !candidateBattle.canBeSentOut() || candidateBattle.getGone()
                            || candidateBattle.getHealth() <= 0 || candidateBattle.isSentOut()) {
                        continue;
                    }
                    Pokemon candidate = candidateBattle.getEffectedPokemon();
                    if (candidate == null || candidate.getCurrentHealth() <= 0) continue;
                    legalSwitches.add(candidateBattle.getUuid().toString());
                    String reserveId = candidateBattle.getUuid().toString();
                    var ownBound = ownMechanicsProfile(reserveId, candidateBattle, true);
                    ownedProfiles.put(reserveId, ownBound);
                    bench.add(ownReceiptSnapshot(snapshotCombatant(candidate), reserveId, ownBound));
                }
            }
            // Every opponent field entering Shadow comes from filtered public
            // Showdown events. Never mix in enemy.getSpecies(), private HP,
            // Cobblemon types or UUID-keyed major-action caches here.
            String opponentSlot = opponent == null ? null : opponent.getPNX();
            BattleSnapshot.Combatant publicOpponent =
                    PublicShadowSnapshot.opponent(publicProtocolObserver, opponentSlot);
            String publicIdentity = opponentSlot == null ? null
                    : publicProtocolObserver.activeIdentity(opponentSlot);
            var opponentObservation = publicMechanicsEvidence.observation(opponentSlot);
            var opponentMechanics = publicMechanicsEvidence.bind("opponent", opponentObservation, scope);
            var selfObservation = publicMechanicsEvidence.observation(activePokemon.getPNX());
            var executionBindings = new HashMap<String, com.maestroai.training.ScopedExecution.Binding>();
            ownedProfiles.forEach((id, bound) -> executionBindings.put(id,
                    new com.maestroai.training.ScopedExecution.Binding(id.equals("self") ? selfObservation : null,
                            bound.observation().item(), bound.observation().ability(),
                            ownFreshness.getOrDefault(id, PublicTransitionState.Known.unknown()), ownChoiceLease)));
            executionBindings.put("opponent", new com.maestroai.training.ScopedExecution.Binding(opponentObservation,
                    opponentObservation == null ? PublicTransitionState.Known.unknown() : opponentObservation.item(),
                    opponentObservation == null ? PublicTransitionState.Known.unknown() : opponentObservation.ability()));
            var execution = fieldCapture == null ? null : new com.maestroai.training.ScopedExecution(fieldCapture, executionBindings,
                    domainScopes);
            var ownZ=ownTransport.ownZMove(ownChoiceLease,runtimeDomain,selfObservation,fieldCapture,()->ownZChoice(ownMoveset));
            if(ownZ!=null){
                execution=execution.withOwnZMove(ownZ);
                if(ownZ.activation().isPresent())legalMoves.add(com.maestroai.training.OwnZMoveEvidence.MOVE);
            }
            List<String> revealedOpponentMoves = publicOpponent.revealedMoves();
            Map<String, Integer> publicStages = fieldCapture == null ? Map.of()
                    : fieldCapture.stages(opponentObservation, runtimeDomain == null
                        ? PublicTransitionState.Known.unknown() : runtimeDomain.effectiveState());
            String publicItem = publicOpponent.revealedItem();
            String publicLastSeenItem = publicIdentity == null ? null
                    : publicProtocolObserver.revealedItem(publicIdentity);
            String publicStatus = publicIdentity == null ? null
                    : publicProtocolObserver.observedStatus(publicIdentity);
            boolean publicTransformed = publicIdentity != null
                    && publicProtocolObserver.transformed(publicIdentity);
            String publicAbility = publicOpponent.revealedAbility();
            boolean uncertainStages = publicStages.isEmpty();
            PublicInteractionModel.Interaction itemRule =
                    PublicInteractionModel.item(publicItem);
            PublicInteractionModel.Interaction abilityRule =
                    PublicInteractionModel.ability(publicAbility);
            if (itemRule != null || abilityRule != null) {
                System.out.println("[MaestroAI] Shadow public interactions"
                        + " item=" + (itemRule == null ? "UNKNOWN" : itemRule.consequence())
                        + " ability=" + (abilityRule == null ? "UNKNOWN" : abilityRule.consequence()));
            }
            ShadowMechanicsPredictor.Prediction publicAbilityOnSwitch =
                    ShadowMechanicsPredictor.abilityActivation(publicAbility, "switch in");
            System.out.println("[MaestroAI] Shadow mechanics prediction"
                    + " opponentAbilitySwitch=" + publicAbilityOnSwitch.verdict()
                    + " reason=" + publicAbilityOnSwitch.reason()
                    + " currentItemKnown=" + (publicItem != null)
                    + " majorStatus=" + (publicStatus == null ? "UNKNOWN" : publicStatus)
                    + " transformed=" + publicTransformed);
            String publicTeraType =
                    PublicShadowSnapshot.publicTeraType(publicProtocolObserver, opponentSlot);
            Map<String, Integer> selfStages = ownEffectiveStages.getOrDefault("self", Map.of());
            BattleSnapshot.Combatant ownFacts = ownReceiptSnapshot(snapshotCombatant(mine), "self", ownedProfiles.get("self"));
            BattleSnapshot.Combatant publicSelf = new BattleSnapshot.Combatant(
                    ownFacts.species(), ownFacts.hpFraction(), ownFacts.types(), selfStages,
                    ownFacts.revealedMoves(), ownFacts.revealedItem(), ownFacts.revealedAbility(), ownFacts.fainted());
            BattleSnapshot snapshot = new BattleSnapshot(
                    ownChoiceLease != null && ownChoiceLease.current() ? ownChoiceLease.turn() : Math.max(0, battle.getTurn()),
                    publicSelf,
                    publicOpponent,
                    bench,
                    List.of(), // Opponent party members are never read from internals.
                    legalMoves,
                    legalSwitches,
                    Map.of(),
                    publicTeraType != null,
                    publicTeraType
            );
            System.out.println("[MaestroAI] Training observation (shadow only)"
                    + " turn=" + snapshot.turn()
                    + " self=" + snapshot.self().species()
                    + " opponent=" + snapshot.opponent().species()
                    + " legalMoves=" + snapshot.legalMoves()
                    + " legalSwitches=" + snapshot.legalSwitches().size()
                    + " opponentTera=" + snapshot.opponentTeraType()
                    + " statStages=" + (uncertainStages ? "UNKNOWN" : publicStages)
                    + " opponentCurrentItem=" + publicItem
                    + " opponentLastSeenItem=" + publicLastSeenItem
                    + " opponentStatus=" + publicStatus
                    + " opponentTransformed=" + publicTransformed
                    + " opponentRevealedAbility=" + publicAbility
                    + " opponentPublicSlot=" + opponentSlot
                    + " opponentRevealedMoves=" + revealedOpponentMoves
                    + " opponentMoveHistoryMayBeIncomplete=true");
            // Phase 3.29: exercise the independent search only on the
            // observation projection. No live action reads this result.
            // Unknown hazards/damage remain unknown, not fabricated as zero
            // damage evidence; all uncalibrated actions carry uncertainty.
            if (!snapshot.legalMoves().isEmpty() || !snapshot.legalSwitches().isEmpty()) {
                Map<String, PublicTransitionState.Kind> intents = new java.util.TreeMap<>();
                for (MoveChoice choice : moves) {
                    if (choice != null && choice.moveId != null) intents.put(choice.moveId,
                            choice.setupMove ? PublicTransitionState.Kind.SETUP
                                    : choice.damageMove ? PublicTransitionState.Kind.ATTACK
                                    : PublicTransitionState.Kind.UNKNOWN);
                }
                if(ownZ!=null&&ownZ.activation().isPresent())intents.put(com.maestroai.training.OwnZMoveEvidence.MOVE,PublicTransitionState.Kind.ATTACK);
                Map<String, PublicTransitionState.SwitchTarget> targets = new java.util.TreeMap<>();
                String ownSide = activePokemon.getPNX() == null || activePokemon.getPNX().length() < 2
                        ? "" : activePokemon.getPNX().substring(0, 2);
                var domainEffective = runtimeDomain == null ? PublicTransitionState.Known.<Boolean>unknown()
                        : runtimeDomain.effectiveState();
                var ownHazards = fieldCapture == null ? publicProtocolEvidence.sideHazards(ownSide)
                        : fieldCapture.hazards(ownSide, domainEffective);
                for (int i = 0; i < legalSwitches.size(); i++) {
                    String id = legalSwitches.get(i);
                    // Own max HP is allowed. Groundedness, immunity, effective typing and
                    // switch-in modifiers require separate evidence; absence is not proof.
                    var entry = execution == null ? PublicMechanicsEvidence.entry(ownHazards,
                            ownedProfiles.get(id).profile().maxHp().known()
                                    ? PublicTransitionState.Known.of(ownedProfiles.get(id).profile().maxHp().value().low(), "own request max HP")
                                    : PublicTransitionState.Known.unknown(),
                            ownedProfiles.get(id), scope) : execution.entry(ownHazards,
                            ownedProfiles.get(id).profile().maxHp().known()
                                    ? PublicTransitionState.Known.of(ownedProfiles.get(id).profile().maxHp().value().low(), "own request max HP")
                                    : PublicTransitionState.Known.unknown(), ownedProfiles.get(id).profile());
                    targets.put(id, new PublicTransitionState.SwitchTarget(
                            bench.get(i), ownedProfiles.get(id).observation().status(),
                            entry.damage(), entry.speedDelta()));
                }
                var context = new PublicTransitionState.Context(
                        ownReceiptHp.getOrDefault("self", PublicTransitionState.Known.unknown()),
                        opponentObservation == null ? PublicTransitionState.Known.unknown() : opponentObservation.hp(),
                        ownedProfiles.get("self").observation().status(),
                        opponentObservation == null ? PublicTransitionState.Known.unknown() : opponentObservation.status(),
                        ownHazards,
                        fieldCapture == null ? publicProtocolEvidence.sideHazards(
                                opponentSlot == null || opponentSlot.length() < 2 ? "" : opponentSlot.substring(0, 2))
                                : fieldCapture.hazards(opponentSlot == null || opponentSlot.length() < 2
                                        ? "" : opponentSlot.substring(0, 2), domainEffective), targets, intents,
                        publicTransformed ? java.util.Set.of("APPEARANCE_ONLY", "TRANSFORMED")
                                : java.util.Set.of("APPEARANCE_ONLY"));
                var transitionEvidence = new PublicTransitionState.Evidence(context, Map.of(), Map.of());
                System.out.println("[MaestroAI] Phase329 public evidence"
                        + " complete=" + publicProtocolEvidence.complete()
                        + " corrupted=" + publicProtocolEvidence.corrupted()
                        + " opponentHpKnown=" + context.opponentHp().known()
                        + " selfHazardKinds=" + context.selfHazards().layers().keySet()
                        + " opponentHazardKinds=" + context.opponentHazards().layers().keySet()
                        + " unsupportedMechanics=UNKNOWN");
                var publicMechanics = new com.maestroai.training.PublicStateSearchAdapter.PublicMechanics(
                        Map.of(), uncertainStages ? Map.of() : publicStages,
                        java.util.Set.of(), continuationLocked, false, requestTrapped, 1.0, transitionEvidence);
                mechanicsSnapshot = snapshot;
                mechanicsOpponentSlot = opponentSlot;
                mechanicsEvidence = publicMechanics;
                var profiles = new HashMap<String, CobblemonMechanics.Profile>();
                ownedProfiles.forEach((id, bound) -> profiles.put(id, bound.profile()));
                if (selfObservation != null && selfObservation.types().known()) {
                    var selfProfile = profiles.get("self");
                    profiles.put("self", new CobblemonMechanics.Profile("self", selfProfile.level(), selfProfile.maxHp(),
                            selfProfile.stats(), selfObservation.types(), selfProfile.ordinaryEffects()));
                }
                profiles.put("opponent", opponentMechanics.profile());
                mechanicsProfiles = Map.copyOf(profiles);
                mechanicsExecution = execution;
                if (execution != null) System.out.println("[MaestroAI] Phase3 field execution provenance"
                        + " token=" + execution.field().token()
                        + " weather=" + (execution.field().weather().known() ? execution.field().weather().value() : "UNKNOWN")
                        + " terrain=" + (execution.field().terrain().known() ? execution.field().terrain().value() : "UNKNOWN")
                        + " trickRoom=" + (execution.field().trickRoom().known() ? execution.field().trickRoom().value() : "UNKNOWN")
                        + " magicRoom=" + (execution.field().magicRoom().known() ? execution.field().magicRoom().value() : "UNKNOWN")
                        + " unresolved=" + execution.unresolved() + " advisoryOnly=true");
                System.out.println("[MaestroAI] Phase3 public mechanics provenance"
                        + " opponentBinding=" + opponentMechanics.token()
                        + " displayedLevel=" + (opponentObservation != null && opponentObservation.displayedLevel().known()
                                ? opponentObservation.displayedLevel().value() : "UNKNOWN")
                        + " trueLevelKnown=" + opponentMechanics.profile().level().known()
                        + " currentTypesKnown=" + opponentMechanics.profile().types().known()
                        + " rawStatBounds=" + opponentMechanics.profile().stats().keySet()
                        + " unresolved=" + opponentMechanics.unknown() + " advisoryOnly=true");
                var projection = com.maestroai.training.PublicStateSearchAdapter.project(
                        snapshot, publicMechanics);
                var search = com.maestroai.training.ShadowSearchEngine.search(
                        snapshot, "root", projection.positions(), 32, 1.0);
                System.out.println("[MaestroAI] Phase329 Shadow search"
                        + " preferred=" + search.preferred()
                        + " complete=" + search.complete()
                        + " nodes=" + search.nodes()
                        + " cacheHits=" + search.cacheHits()
                        + " evidence=" + projection.evidence()
                        + " advisoryOnly=true");
            }
            return snapshot;
        } catch (RuntimeException exception) {
            // A diagnostic adapter must never break a live trainer turn.
            System.out.println("[MaestroAI] Training observation skipped: "
                    + exception.getClass().getSimpleName());
            return null;
        }
    }

    private void logShadowComparison(
            BattleSnapshot snapshot, MoveChoice actual, List<MoveChoice> moves
    ) {
        if (snapshot == null || actual == null) return;
        logMechanicsComparison(snapshot, "move:" + actual.moveId);
        try {
            List<ShadowDecisionComparator.Candidate> candidates = new ArrayList<>();
            for (MoveChoice choice : moves) {
                if (choice != null && Double.isFinite(choice.finalScore)
                        && snapshot.legalMoves().contains(choice.moveId)) {
                    candidates.add(new ShadowDecisionComparator.Candidate(
                            "move", choice.moveId, choice.finalScore));
                }
            }
            ShadowDecisionComparator.Candidate selected =
                    new ShadowDecisionComparator.Candidate(
                            "move", actual.moveId, actual.finalScore);
            var comparison = ShadowDecisionComparator.compare(snapshot, selected, candidates);
            System.out.println("[MaestroAI] Shadow comparison (legacy scores only)"
                    + " actual=" + comparison.actual().id()
                    + " top=" + comparison.preferred().id()
                    + " gap=" + String.format("%.2f", comparison.scoreGap())
                    + " disagreement=" + comparison.disagreement());
        } catch (RuntimeException exception) {
            System.out.println("[MaestroAI] Shadow comparison skipped: "
                    + exception.getClass().getSimpleName());
        }
    }

    /**
     * Read-only cross-action proxy. Scores come from the existing legacy
     * position evaluators, not an independent search. Reported disagreement
     * is diagnostic only; it must never affect the selected action.
     */
    private void logCrossActionShadow(
            BattleSnapshot snapshot, String actualKind, String actualId,
            String otherId, double stayScore, double switchScore
    ) {
        if ("switch".equals(actualKind)) logMechanicsComparison(snapshot, "switch:" + actualId);
        if (snapshot == null || !Double.isFinite(stayScore)
                || !Double.isFinite(switchScore)) return;
        try {
            ShadowDecisionComparator.Candidate move = new ShadowDecisionComparator.Candidate(
                    "move", "switch".equals(actualKind) ? otherId : actualId, stayScore);
            ShadowDecisionComparator.Candidate change = new ShadowDecisionComparator.Candidate(
                    "switch", "switch".equals(actualKind) ? actualId : otherId, switchScore);
            ShadowDecisionComparator.Candidate actual = "switch".equals(actualKind) ? change : move;
            var comparison = ShadowDecisionComparator.compare(snapshot, actual, List.of(move, change));
            // Independent shadow recorder: legacy proxy values are NOT mechanics-grounded
            // estimates, so each score component remains explicitly UNKNOWN.
            // This diagnostic cannot influence any move or switch selection.
            try {
                var unknown = ShadowSwitchingEvaluator.Estimate.unknown(
                        "not validated from public Cobblemon mechanics");
                var observed = new java.util.ArrayList<ShadowSwitchingEvaluator.ObservedCandidate>();
                for (String id : List.of("move:" + move.id(), "switch:" + change.id())) {
                    boolean legal = id.startsWith("move:")
                            ? snapshot.legalMoves().contains(id.substring(5))
                            : id.startsWith("switch:") && snapshot.legalSwitches().contains(id.substring(7));
                    if (legal) observed.add(new ShadowSwitchingEvaluator.ObservedCandidate(
                            id, unknown, unknown, unknown, unknown, unknown, unknown, true));
                }
                if (!observed.isEmpty()) {
                    String opponentSlot = publicProtocolObserver.activeIdentity("p2a") != null
                            ? "p2a" : "p1a";
                    var telemetry = shadowSwitchTelemetry.record(publicProtocolObserver,
                            opponentSlot, snapshot, observed, actual.kind() + ":" + actual.id());
                    // Diagnostics only: public appearance candidates are not
                    // merged or promoted into actionable opponent knowledge.
                    var memory = publicProtocolObserver.opponentIntelligence();
                    var returnEvidence = memory.activeReturnEvidence(opponentSlot);
                    var assessment = memory.activeAssessment(opponentSlot);
                    var prediction = memory.activeShadowPrediction(opponentSlot);
                    if (prediction != null) {
                        System.out.println("[MaestroAI] Shadow opponent prediction"
                                + " turn=" + snapshot.turn()
                                + " moveOptions=" + prediction.observedMoveOptions()
                                + " moveConfidence=" + prediction.moveConfidence()
                                + " switchingTendency=" + prediction.switchingTendency()
                                + " switchingConfidence=" + prediction.switchingConfidence()
                                + " revealedAbility=" + prediction.revealedAbility()
                                + " revealedItem=" + prediction.revealedItem()
                                + " abilityItemConfidence=" + prediction.abilityItemConfidence()
                                + " historicalCandidates=" + prediction.historicalAppearanceCandidates()
                                + " identityConfirmed=" + prediction.identityConfirmed());
                    }
                    var predictionAudit = memory.predictionAudit();
                    var calibration = memory.predictionCalibration();
                    System.out.println("[MaestroAI] Shadow benchmark trends"
                            + " turn=" + snapshot.turn()
                            + " trends=" + memory.predictionBenchmarkTrends());
                    System.out.println("[MaestroAI] Shadow prediction benchmark"
                            + " turn=" + snapshot.turn()
                            + " moves=" + memory.predictionBenchmark().moves()
                            + " switches=" + memory.predictionBenchmark().switches()
                            + " skippedUnknown=" + memory.predictionBenchmark().skippedUnknown());
                    System.out.println("[MaestroAI] Shadow prediction quality"
                            + " turn=" + snapshot.turn()
                            + " appearances=" + memory.predictionQualities());
                    System.out.println("[MaestroAI] Shadow prediction calibration"
                            + " turn=" + snapshot.turn()
                            + " moveAccuracy=" + calibration.moves()
                            + " switchAccuracy=" + calibration.switches()
                            + " skippedUnknown=" + calibration.skippedUnknown());
                    System.out.println("[MaestroAI] Shadow prediction audit"
                            + " turn=" + snapshot.turn()
                            + " moveEvaluations=" + predictionAudit.moveEvaluations()
                            + " moveMatches=" + predictionAudit.moveMatches()
                            + " switchEvaluations=" + predictionAudit.switchEvaluations()
                            + " switchMatches=" + predictionAudit.switchMatches()
                            + " skippedUnknown=" + predictionAudit.skippedUnknown()
                            + " recentOutcomes=" + predictionAudit.recentOutcomes());
                    System.out.println("[MaestroAI] Shadow opponent memory"
                            + " turn=" + snapshot.turn()
                            + " confidence=" + (returnEvidence == null ? "UNKNOWN" : returnEvidence.confidence())
                            + " candidateAppearances=" + (returnEvidence == null ? 0
                                    : returnEvidence.earlierCandidates().size())
                            + " confirmedSamePartyMember=" + (returnEvidence != null
                                    && returnEvidence.confirmedSamePartyMember())
                            + " observedMoveKinds=" + (assessment == null ? 0
                                    : assessment.moveUses().size())
                            + " switchPatterns=" + memory.observedSwitchPatterns().size());
                    System.out.println("[MaestroAI] Shadow switching telemetry"
                            + " turn=" + snapshot.turn()
                            + " legacy=" + telemetry.legacyAction()
                            + " preferred=" + (telemetry.shadowPreference() == null
                                    ? "UNKNOWN" : telemetry.shadowPreference())
                            + " comparable=" + telemetry.comparable()
                            + " unknown=" + telemetry.unknownByAction()
                            + " loopCandidates=" + telemetry.loopCandidates());
                }
            } catch (RuntimeException telemetryError) {
                System.out.println("[MaestroAI] Shadow switching telemetry skipped: "
                        + telemetryError.getClass().getSimpleName());
            }
            System.out.println("[MaestroAI] Shadow cross-action (legacy position proxy)"
                    + " actual=" + actual.kind() + ":" + actual.id()
                    + " proxyTop=" + comparison.preferred().kind() + ":"
                    + comparison.preferred().id()
                    + " gap=" + String.format("%.2f", comparison.scoreGap())
                    + " disagreement=" + comparison.disagreement());
        } catch (RuntimeException exception) {
            System.out.println("[MaestroAI] Shadow cross-action skipped: "
                    + exception.getClass().getSimpleName());
        }
    }

    /** Query public rules data by a protocol-confirmed name, never the opponent Pokemon.
     * Reject form aliases instead of silently substituting a base species.
     */
    private PublicMechanicsEvidence.Species publicCatalogSpecies(String publicName) {
        var species = PokemonSpecies.getByName(publicName);
        if (species == null || !species.getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "").equals(publicName)) return null;
        var base = species.getBaseStats();
        Map<String, Integer> stats = Map.of("hp", base.get(Stats.HP), "atk", base.get(Stats.ATTACK),
                "def", base.get(Stats.DEFENCE), "spa", base.get(Stats.SPECIAL_ATTACK),
                "spd", base.get(Stats.SPECIAL_DEFENCE), "spe", base.get(Stats.SPEED));
        var types = new ArrayList<String>();
        for (var type : species.getTypes()) types.add(type.getName().toLowerCase(java.util.Locale.ROOT));
        try {
            return com.maestroai.training.PublicSpeciesCatalog.verified(publicName,
                    new PublicMechanicsEvidence.Species(stats, types,
                    publicName.equals("shedinja") && stats.get("hp") == 1 ? 1 : null));
        } catch (IllegalArgumentException outsideDomain) { return null; }
    }

    /** Simulator receipts are actor/dispatch/choice-bound; public effect evidence
     * is a separate prerequisite for interpreting base stats as current stats.
     */
    private PublicMechanicsEvidence.Bound ownMechanicsProfile(String id, BattlePokemon own, boolean reserve) {
        var publicOwn = reserve || own.getActor() == null || own.getActor().getActivePokemon().isEmpty() ? null
                : publicMechanicsEvidence.observation(own.getActor().getActivePokemon().getFirst().getPNX());
        if (reserve && own.getActor() != null && !own.getActor().getActivePokemon().isEmpty())
            publicOwn = publicMechanicsEvidence.observation(own.getActor().getActivePokemon().getFirst().getPNX());
        var effective = ownTransport.effective(ownChoiceLease, id, own.getUuid(), reserve, runtimeDomain,
                publicProtocolEvidence, publicOwn, fieldCapture);
        var request = effective.evidence(); ownEffectiveStages.put(id, effective.stages());
        ownReceiptHp.put(id, request.hp());
        ownFreshness.put(id, request.fresh());
        System.out.println("[MaestroAI] Shadow own transport role=" + (reserve ? "reserve" : "active")
                + " fresh=" + request.fresh().known() + " levelTransported=" + request.profile().level().known()
                + " statsJustified=" + !request.profile().stats().isEmpty() + " itemTransported=" + request.item().known()
                + " effectiveTypesJustified=" + request.profile().types().known()
                + " unresolved=" + new java.util.TreeSet<>(request.unknown())
                + " effectiveStateMissing=" + new java.util.TreeSet<>(effective.missing()) + " advisoryOnly=true");
        var observation = new PublicMechanicsEvidence.Observation("own:" + id, 0, id, PublicTransitionState.Known.unknown(),
                request.profile().level(), PublicTransitionState.Known.unknown(), request.profile().types(),
                PublicTransitionState.Known.unknown(), request.status(), request.item(), request.ability(), false, false);
        return new PublicMechanicsEvidence.Bound("owned-dispatch:" + id, request.profile(), observation, request.unknown());
    }

    private BattleSnapshot.Combatant ownReceiptSnapshot(BattleSnapshot.Combatant own, String id, PublicMechanicsEvidence.Bound bound) {
        var hp = ownReceiptHp.getOrDefault(id, PublicTransitionState.Known.unknown());
        var types = bound.profile().types();
        return new BattleSnapshot.Combatant(own.species(), hp.known() ? hp.value().low() : BattleSnapshot.UNKNOWN_HP_FRACTION,
                types.known() ? types.value() : List.of(), ownEffectiveStages.getOrDefault(id, Map.of()), own.revealedMoves(),
                bound.observation().item().known() ? bound.observation().item().value() : null,
                bound.observation().ability().known() ? bound.observation().ability().value() : null,
                hp.known() && hp.value().high() == 0);
    }

    /** Actual selected legacy action is a comparator only. No legacy score or opponent
     * internal stat/item/ability supplies missing mechanics; UNKNOWN withholds a ranking.
     */
    private void logMechanicsComparison(BattleSnapshot snapshot, String legacyAction) {
        if (snapshot == null || snapshot != mechanicsSnapshot || mechanicsEvidence == null) return;
        try {
            var root = com.maestroai.training.PublicStateSearchAdapter.project(snapshot, mechanicsEvidence)
                    .states().get("root");
            var profiles = mechanicsProfiles;
            var actions = new ArrayList<String>();
            root.legalMoves().forEach(id -> actions.add("move:" + id));
            root.legalSwitches().forEach(id -> actions.add("switch:" + id));
            if (actions.isEmpty() || !actions.contains(legacyAction)) return;
            String reply = snapshot.opponent().revealedMoves().stream().sorted().findFirst().orElse("UNKNOWN");
            if (mechanicsExecution != null) for (String action : actions) {
                var certificates = new ArrayList<com.maestroai.training.ExecutionCoverage.Certificate>();
                certificates.add(mechanicsExecution.orderCoverage(action, reply, profiles.get("self"), root.self(),
                        profiles.get("opponent"), root.opponent()));
                if (action.startsWith("move:")) {
                    var move = CobblemonMechanics.move(action.substring(5));
                    certificates.add(move != null && move.power() == 0
                            ? mechanicsExecution.setupCoverage(profiles.get("self"), root.self(), move.id())
                            : mechanicsExecution.damageCoverage(action.substring(5), profiles.get("self"), root.self(),
                                    profiles.get("opponent"), root.opponent()));
                } else {
                    var reserve = profiles.get(action.substring(7));
                    var hp = reserve.maxHp().known() && reserve.maxHp().value().low() == reserve.maxHp().value().high()
                            ? PublicTransitionState.Known.of(reserve.maxHp().value().low(), reserve.maxHp().source())
                            : PublicTransitionState.Known.<Integer>unknown();
                    certificates.add(mechanicsExecution.entryCoverage(root.selfHazards(), hp, reserve));
                }
                for (var coverage : certificates) System.out.println("[MaestroAI] Shadow operation coverage"
                        + " operation=" + coverage.operation() + " supported=" + coverage.supported()
                        + " prerequisites=" + coverage.prerequisites().size()
                        + " missing=" + new java.util.TreeSet<>(coverage.missing()) + " source=" + coverage.source()
                        + " advisoryOnly=true");
            }
            var plans = actions.stream().map(action -> new ShadowMechanicsEvaluation.Plan(action,
                    List.of(List.of(new ShadowMechanicsEvaluation.Turn(action, reply, null))),
                    "conditional public revealed-reply diagnostic; not a prediction")).toList();
            // Independent Phase 3.34 advisory comparison against all strictly
            // witnessed public current-appearance replies; never promote and
            // never infer a probability for the opponent's next move.
            var envelopedAudit = com.maestroai.training.PublicReplyCoverage.evaluateEnveloped(
                    snapshot, mechanicsEvidence, profiles, mechanicsExecution,
                    publicProtocolEvidence, mechanicsOpponentSlot,
                    mechanicsExecution == null ? PublicTransitionState.Known.unknown()
                            : mechanicsExecution.field().trickRoom(),
                    CobblemonMechanics.Critical.BOTH);
            var replyAudit = envelopedAudit.replies();
            System.out.println("[MaestroAI] " + replyAudit.safeLine());
            // Public current-modifier singleton only; no inferred hidden
            // opponent set, no private request and no live policy handoff.
            System.out.println("[MaestroAI] " + envelopedAudit.modifiers().safeLine());
            // A bounded pair is not a global recommendation: other legal
            // actions and unseen opponent moves may remain unresolved.
            System.out.println("[MaestroAI] " + envelopedAudit.pairwise().safeLine());
            var comparison = ShadowMechanicsEvaluation.compare(snapshot, mechanicsEvidence, profiles, plans,
                    mechanicsExecution == null ? PublicTransitionState.Known.unknown() : mechanicsExecution.field().trickRoom(),
                    CobblemonMechanics.Critical.BOTH, legacyAction, mechanicsExecution);
            // Fixed vocabulary only: no private request fields, opponent UUID,
            // registry callback bodies or hidden opponent modifiers in telemetry.
            System.out.println("[MaestroAI] " + com.maestroai.training.ShadowPrerequisiteDiagnostics.inspect(
                    runtimeDomain, ownChoiceLease, mechanicsExecution,
                    publicProtocolEvidence, comparison).safeLine());
            var reasons = new java.util.TreeSet<String>();
            comparison.rankings().forEach(ranking -> reasons.addAll(ranking.unknown()));
            System.out.println("[MaestroAI] Phase3 mechanics comparison"
                    + " legacy=" + comparison.legacyAction()
                    + " nominalPowerBaseline=" + comparison.simpleAction()
                    + " preferredConditional=" + (comparison.preferredConditional() == null ? "UNKNOWN" : comparison.preferredConditional())
                    + " candidateCoverageComplete=" + comparison.candidateCoverageComplete()
                    + " unresolved=" + reasons
                    + " source=" + CobblemonMechanics.SOURCE
                    + " advisoryOnly=true; no empirical legacy outcome claim");
        } catch (RuntimeException exception) {
            System.out.println("[MaestroAI] Phase3 mechanics comparison skipped: " + exception.getClass().getSimpleName());
        }
    }

    private BattleSnapshot.Combatant snapshotCombatant(Pokemon pokemon) {
        List<String> types = new ArrayList<>();
        if (pokemon.getPrimaryType() != null) {
            types.add(pokemon.getPrimaryType().getName());
        }
        if (pokemon.getSecondaryType() != null) {
            types.add(pokemon.getSecondaryType().getName());
        }
        double hp = Math.max(0.0, Math.min(1.0, getHpPercent(pokemon)));
        return new BattleSnapshot.Combatant(
                speciesId(pokemon), hp, types, Map.of(), List.of(),
                null, null, hp <= 0.0
        );
    }

    private static class MoveChoice {

        private final Move move;
        private final String moveId;
        private final String moveName;
        private final String moveType;
        private final String category;
        private final double power;

        private final double score;

        private int finalScorePlaceholder;

        private double finalScore;
        private double strategicAdjustment;

        private final int estimatedDamage;
        private final int maximumDamage;

        private final double effectiveness;
        private final boolean stab;
        private final double accuracy;
        private final int pp;
        private final int priority;

        private final double averageDamage;

        private final boolean damageMove;
        private final boolean setupMove;
        private final boolean pivotMove;

        private MoveChoice(
                Move move,
                String moveId,
                String moveName,
                String moveType,
                String category,
                double power,
                double score,
                int estimatedDamage,
                int maximumDamage,
                double effectiveness,
                boolean stab,
                double accuracy,
                int pp,
                int priority,
                double averageDamage,
                boolean damageMove,
                boolean setupMove,
                boolean pivotMove
        ) {
            this.move = move;
            this.moveId = moveId;
            this.moveName = moveName;
            this.moveType = moveType;
            this.category = category;
            this.power = power;
            this.score = score;
            this.finalScore = score;
            this.strategicAdjustment = 0.0;
            this.estimatedDamage = estimatedDamage;
            this.maximumDamage = maximumDamage;
            this.effectiveness = effectiveness;
            this.stab = stab;
            this.accuracy = accuracy;
            this.pp = pp;
            this.priority = priority;
            this.averageDamage = averageDamage;
            this.damageMove = damageMove;
            this.setupMove = setupMove;
            this.pivotMove = pivotMove;
        }
    }

    private static class SwitchChoice {

        private final BattlePokemon pokemon;

        private final double score;
        private final double currentThreat;
        private final double offensivePotential;

        private final double offensiveDamage;
        private final double incomingDamage;

        private SwitchChoice(
                BattlePokemon pokemon,
                double score,
                double currentThreat,
                double offensivePotential,
                double offensiveDamage,
                double incomingDamage
        ) {
            this.pokemon = pokemon;
            this.score = score;
            this.currentThreat = currentThreat;
            this.offensivePotential = offensivePotential;
            this.offensiveDamage = offensiveDamage;
            this.incomingDamage = incomingDamage;
        }
    }
}
