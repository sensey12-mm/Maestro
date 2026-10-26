package com.maestroai.training;

import java.util.*;

/**
 * Shadow-only, public-protocol-derived opponent intelligence.
 * Appearance IDs are stable within an appearance, never assumed to identify
 * a party member across switches (duplicate species/nicknames are possible).
 * Historical appearances remain queryable without leaking their facts into
 * an ambiguous returning opponent.
 */
public final class PublicOpponentIntelligence {
    public record Threat(String appearance, String actor, Set<String> moves,
                         String item, String ability, String status,
                         boolean transformed, Set<String> tags) {}
    public record Assessment(Threat threat, Map<String,Integer> moveUses,
                             int setupUses, int recoveryUses, int pivotUses,
                             int priorityUses, int offensiveUses, int defensiveUses,
                             int switchesIn, int switchesOut, boolean itemRemoved,
                             boolean itemSuppressed, String choiceLock,
                             Set<String> confirmedAbilityActivations,
                             boolean identityAmbiguous) {}

    /** Confidence describes the strength of observed evidence, not win probability. */
    public record RoleEvidence(String role, String confidence, Set<String> evidence) {}
    /** No identity is merged without a public unique party identifier. */
    public record IdentityEvidence(String appearance, Set<String> candidateAppearances,
                                   boolean confirmedSamePartyMember) {}
    public record ItemEvent(String appearance, String kind, String item, String source) {}
    /** A public item event is evidence of a local change, not a proven transfer. */
    public record ItemTransferEvidence(String sourceAppearance, String destinationAppearance,
                                       String item, boolean confirmed, String reason) {}
    public List<ItemTransferEvidence> possibleTransfers() {
        List<ItemTransferEvidence> result = new ArrayList<>();
        for (int i = 0; i < itemEvents.size(); i++) {
            ItemEvent ended = itemEvents.get(i);
            if (!ended.kind().equals("ended") || ended.item() == null) continue;
            for (int j = i + 1; j < itemEvents.size(); j++) {
                ItemEvent revealed = itemEvents.get(j);
                if (!revealed.kind().equals("revealed")) continue;
                if (ended.appearance().equals(revealed.appearance())) continue;
                if (ended.item().equals(revealed.item())
                        && (ended.source().contains("Trick") || ended.source().contains("Switcheroo")
                            || revealed.source().contains("Trick") || revealed.source().contains("Switcheroo")))
                    result.add(new ItemTransferEvidence(ended.appearance(),
                            revealed.appearance(), ended.item(), false,
                            "matching public transfer-associated events; no unique linkage"));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Shadow-only memory for a returning display. Each entry is a distinct
     * appearance, not an inferred party member. Candidate facts are never
     * promoted to confirmed facts about the current combatant.
     */
    public record AppearanceMemory(String appearance, Set<String> observedMoves,
                                   String revealedAbility, String lastRevealedItem,
                                   String observedStatus, boolean itemRemoved,
                                   boolean transformed) {}
    public record ReturnEvidence(String activeAppearance, String displayedActor,
                                 List<AppearanceMemory> earlierCandidates,
                                 boolean confirmedSamePartyMember,
                                 String confidence) {}

    public ReturnEvidence activeReturnEvidence(String slot) {
        String id = active.get(slot);
        Mutable current = history.get(id);
        if (current == null) return null;
        List<AppearanceMemory> candidates = new ArrayList<>();
        for (var entry : history.entrySet()) {
            Mutable previous = entry.getValue();
            if (!entry.getKey().equals(id) && previous.actor.equals(current.actor))
                candidates.add(new AppearanceMemory(entry.getKey(),
                        Set.copyOf(previous.moveUses.keySet()), previous.ability,
                        previous.lastItem, previous.status, previous.itemRemoved,
                        previous.transformed));
        }
        return new ReturnEvidence(id, current.actor, List.copyOf(candidates),
                false, candidates.isEmpty() ? "no-matching-public-appearance"
                        : "appearance-match-only");
    }

    /**
     * Shadow prediction is a ranked description of observed possibilities,
     * NOT a calibrated probability or a claim about unrevealed moves.
     * A historical appearance match is never treated as the same party member.
     */
    public record MovePrediction(String move, int observedUses, String confidence) {}
    public record ShadowPrediction(String activeAppearance, List<MovePrediction> observedMoveOptions,
                                   String moveConfidence, String switchingTendency,
                                   String switchingConfidence, String revealedAbility,
                                   String revealedItem, String abilityItemConfidence,
                                   int historicalAppearanceCandidates, boolean identityConfirmed) {}

    public ShadowPrediction activeShadowPrediction(String slot) {
        String id = active.get(slot);
        Mutable m = history.get(id);
        if (m == null) return null;
        ReturnEvidence returning = activeReturnEvidence(slot);
        List<MovePrediction> moves = new ArrayList<>();
        // On sparse observations, repeated use is evidence but not proof of
        // the next action. Rank by recency only when the observed counts tie.
        m.moveUses.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue)
                        .reversed().thenComparing((left, right) ->
                                Integer.compare(m.moveLastSeen.getOrDefault(right.getKey(), 0),
                                        m.moveLastSeen.getOrDefault(left.getKey(), 0)))
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> moves.add(new MovePrediction(e.getKey(), e.getValue(),
                        e.getValue() > 1 ? "repeated-public-use" : "single-public-use")));
        long patterns = switchPatterns.stream().filter(p -> p.slot().equals(slot)).count();
        // A public switch event cannot prove the player's intent or next action.
        String tendency = patterns > 0 ? "observed-display-return-pattern" : "unknown";
        String switchConfidence = patterns > 0 ? "candidate-pattern-only" : "insufficient-evidence";
        String ability = m.transformed ? null : m.ability;
        String item = m.itemRemoved ? null : m.item;
        return new ShadowPrediction(id, List.copyOf(moves),
                moves.isEmpty() ? "unknown" : "current-appearance-observed-only",
                tendency, switchConfidence, ability, item,
                ability == null && item == null ? "unknown" : "current-appearance-public-reveal",
                returning == null ? 0 : returning.earlierCandidates().size(), false);
    }

    /**
     * Prequential (predict-before-observe) shadow audit. Only a public action
     * on the same active appearance can score a move hypothesis. A switch
     * prediction scores action category, never a hidden destination or intent.
     * Rates are empirical audit hit rates, NOT calibrated probabilities.
     */
    public record PredictionOutcome(String appearance, String category, String predicted,
                                    String observed, boolean matched, String confidence) {}
    public record PredictionAudit(int moveEvaluations, int moveMatches,
                                  int switchEvaluations, int switchMatches,
                                  int skippedUnknown, List<PredictionOutcome> recentOutcomes) {}
    /**
     * Observed hit rate only, not a forecast probability. With fewer than
     * twenty evaluated events the rate is withheld to avoid false precision.
     * The Wilson interval describes sampling uncertainty, not calibration.
     */
    public record EmpiricalAccuracy(int evaluations, int matches, String evidence,
                                    Double observedRate, Double wilsonLow, Double wilsonHigh) {}
    public record PredictionCalibration(EmpiricalAccuracy moves,
                                        EmpiricalAccuracy switches, int skippedUnknown) {}

    private static EmpiricalAccuracy empiricalAccuracy(int n, int hits) {
        if (n < 20) return new EmpiricalAccuracy(n, hits, "insufficient-data", null, null, null);
        double rate = (double) hits / n;
        double z = 1.96, z2 = z * z, denom = 1.0 + z2 / n;
        double center = (rate + z2 / (2.0 * n)) / denom;
        double radius = z * Math.sqrt(rate * (1.0 - rate) / n + z2 / (4.0 * n * n)) / denom;
        return new EmpiricalAccuracy(n, hits, "empirical-not-calibrated",
                rate, Math.max(0.0, center - radius), Math.min(1.0, center + radius));
    }

    public PredictionCalibration predictionCalibration() {
        PredictionAudit audit = predictionAudit();
        return new PredictionCalibration(
                empiricalAccuracy(audit.moveEvaluations(), audit.moveMatches()),
                empiricalAccuracy(audit.switchEvaluations(), audit.switchMatches()),
                audit.skippedUnknown());
    }

    /** Per-appearance learning trajectory; no cross-appearance identity merge. */
    public record PredictionQuality(String appearance, int evaluatedMoves,
                                    int correctMoves, int consecutiveMisses,
                                    int maxConsecutiveMisses, String reliability,
                                    String trajectory) {}
    public List<PredictionQuality> predictionQualities() {
        Map<String, List<PredictionOutcome>> byAppearance = new LinkedHashMap<>();
        for (PredictionOutcome outcome : predictionOutcomes)
            if (outcome.category().equals("move"))
                byAppearance.computeIfAbsent(outcome.appearance(), ignored -> new ArrayList<>())
                        .add(outcome);
        List<PredictionQuality> result = new ArrayList<>();
        for (var entry : byAppearance.entrySet()) {
            List<PredictionOutcome> outcomes = entry.getValue();
            int correct = 0, streak = 0, maxStreak = 0;
            for (PredictionOutcome outcome : outcomes) {
                if (outcome.matched()) { correct++; streak = 0; }
                else { streak++; maxStreak = Math.max(maxStreak, streak); }
            }
            int half = outcomes.size() / 2;
            String trajectory = "insufficient-data";
            // Compare two disjoint chronological halves, with at least five
            // observations each; do not claim a causal learning effect.
            if (half >= 5) {
                int earlyHits = 0, lateHits = 0;
                for (int i = 0; i < half; i++)
                    if (outcomes.get(i).matched()) earlyHits++;
                for (int i = half; i < outcomes.size(); i++)
                    if (outcomes.get(i).matched()) lateHits++;
                double early = (double) earlyHits / half;
                double late = (double) lateHits / (outcomes.size() - half);
                trajectory = late > early ? "observed-improvement"
                        : late < early ? "observed-decline" : "no-observed-change";
            }
            // Reliability describes the current streak; historical failures
            // remain visible separately through maxConsecutiveMisses.
            String reliability = outcomes.size() < 5 ? "insufficient-data"
                    : streak >= 3 ? "repeated-current-misses"
                    : maxStreak >= 3 ? "recovered-after-misses"
                    : "no-long-miss-streak";
            result.add(new PredictionQuality(entry.getKey(), outcomes.size(), correct,
                    streak, maxStreak, reliability, trajectory));
        }
        return List.copyOf(result);
    }

    /**
     * Pre-event benchmark against simple public-only baselines.
     * Move baseline: earliest observed native move on this appearance.
     * Switch baseline: always predict a move (never a switch).
     * Both use the same eligible event horizons as the shadow model.
     */
    public record BenchmarkCategory(int evaluations, int shadowMatches,
                                    int baselineMatches, String baseline,
                                    String evidence, Double shadowRate,
                                    Double baselineRate) {}
    public record PredictionBenchmark(BenchmarkCategory moves, BenchmarkCategory switches,
                                      int skippedUnknown) {}
    /**
     * Chronological, paired benchmark summary. Rates are withheld below 20
     * matched public-event horizons; they are not causal improvement claims.
     * Identity ambiguity is a caution, never a reason to merge histories.
     */
    public record BenchmarkTrend(int evaluations, String evidence,
                                 Double earlyShadowRate, Double lateShadowRate,
                                 Double earlyBaselineRate, Double lateBaselineRate,
                                 String comparison, boolean ambiguousAppearances) {}
    public record BenchmarkTrends(BenchmarkTrend moves, BenchmarkTrend switches) {}

    private BenchmarkTrend benchmarkTrend(String category) {
        List<PredictionOutcome> model = new ArrayList<>();
        List<PredictionOutcome> baseline = new ArrayList<>();
        boolean ambiguous = false;
        for (PredictionOutcome outcome : predictionOutcomes)
            if (outcome.category().equals(category)) {
                model.add(outcome);
                Mutable m = history.get(outcome.appearance());
                if (m != null && m.ambiguous) ambiguous = true;
            }
        for (PredictionOutcome outcome : baselineOutcomes)
            if (outcome.category().equals(category)) baseline.add(outcome);
        int n = model.size();
        if (n != baseline.size())
            throw new IllegalStateException("Benchmark event horizons are not paired");
        if (n < 20)
            return new BenchmarkTrend(n, "insufficient-data", null, null, null, null,
                    "insufficient-data", ambiguous);
        int midpoint = n / 2;
        int earlyShadow = 0, lateShadow = 0, earlyBaseline = 0, lateBaseline = 0;
        for (int i = 0; i < n; i++) {
            if (i < midpoint) {
                if (model.get(i).matched()) earlyShadow++;
                if (baseline.get(i).matched()) earlyBaseline++;
            } else {
                if (model.get(i).matched()) lateShadow++;
                if (baseline.get(i).matched()) lateBaseline++;
            }
        }
        int shadowHits = earlyShadow + lateShadow;
        int baselineHits = earlyBaseline + lateBaseline;
        String comparison = shadowHits > baselineHits ? "shadow-ahead-observed"
                : shadowHits < baselineHits ? "baseline-ahead-observed" : "observed-tie";
        return new BenchmarkTrend(n,
                ambiguous ? "appearance-ambiguous-empirical-only" : "empirical-not-calibrated",
                (double) earlyShadow / midpoint, (double) lateShadow / (n - midpoint),
                (double) earlyBaseline / midpoint, (double) lateBaseline / (n - midpoint),
                comparison, ambiguous);
    }

    public BenchmarkTrends predictionBenchmarkTrends() {
        return new BenchmarkTrends(benchmarkTrend("move"), benchmarkTrend("switch"));
    }

    private final List<PredictionOutcome> baselineOutcomes = new ArrayList<>();

    private static BenchmarkCategory benchmarkCategory(List<PredictionOutcome> shadow,
                                                       List<PredictionOutcome> baseline,
                                                       String category, String baselineName) {
        int n = 0, hits = 0, baselineHits = 0;
        for (PredictionOutcome outcome : shadow)
            if (outcome.category().equals(category)) {
                n++;
                if (outcome.matched()) hits++;
            }
        for (PredictionOutcome outcome : baseline)
            if (outcome.category().equals(category) && outcome.matched()) baselineHits++;
        return new BenchmarkCategory(n, hits, baselineHits, baselineName,
                n < 20 ? "insufficient-data" : "empirical-not-calibrated",
                n < 20 ? null : (double) hits / n,
                n < 20 ? null : (double) baselineHits / n);
    }

    public PredictionBenchmark predictionBenchmark() {
        return new PredictionBenchmark(
                benchmarkCategory(predictionOutcomes, baselineOutcomes, "move", "first-observed-native-move"),
                benchmarkCategory(predictionOutcomes, baselineOutcomes, "switch", "always-move"),
                skippedPredictionOutcomes);
    }

    private final List<PredictionOutcome> predictionOutcomes = new ArrayList<>();
    private int skippedPredictionOutcomes;

    public PredictionAudit predictionAudit() {
        int moves = 0, moveMatches = 0, switches = 0, switchMatches = 0;
        for (PredictionOutcome outcome : predictionOutcomes) {
            if (outcome.category().equals("move")) {
                moves++;
                if (outcome.matched()) moveMatches++;
            } else if (outcome.category().equals("switch")) {
                switches++;
                if (outcome.matched()) switchMatches++;
            }
        }
        int from = Math.max(0, predictionOutcomes.size() - 8);
        return new PredictionAudit(moves, moveMatches, switches, switchMatches,
                skippedPredictionOutcomes,
                List.copyOf(predictionOutcomes.subList(from, predictionOutcomes.size())));
    }

    private void auditPublicAction(String event, String slot, String[] parts) {
        if (!(event.equals("move") || event.equals("switch") || event.equals("drag"))) return;
        String id = active.get(slot);
        Mutable m = history.get(id);
        if (m == null || event.equals("drag") || m.transformed
                || (event.equals("move") && hasMoveOverride(parts))) {
            skippedPredictionOutcomes++;
            return;
        }
        // Evaluate only the incumbent, before applying this public event.
        // An initial switch is not a scored prediction.
        ShadowPrediction prior = activeShadowPrediction(slot);
        if (prior == null) { skippedPredictionOutcomes++; return; }
        if (event.equals("move")) {
            if (parts.length < 4 || prior.observedMoveOptions().isEmpty()) {
                skippedPredictionOutcomes++;
            } else {
                String observed = canonical(parts[3]);
                if (observed.isEmpty()) {
                    skippedPredictionOutcomes++;
                } else {
                    String predicted = prior.observedMoveOptions().get(0).move();
                    predictionOutcomes.add(new PredictionOutcome(id, "move", predicted, observed,
                            predicted.equals(observed), "observed-move-frequency-uncalibrated"));
                    // Linked to the exact same eligible event, before its
                    // observation updates move counts.
                    String firstObserved = m.moveUses.keySet().iterator().next();
                    baselineOutcomes.add(new PredictionOutcome(id, "move", firstObserved, observed,
                            firstObserved.equals(observed), "first-observed-native-move"));
                }
            }
        }
        // A candidate pattern supplies only a weak binary hypothesis:
        // "switch" on the next observable action. This is not a probability.
        // Score against public move/switch actions; forced drag is excluded.
        if (prior.switchingTendency().equals("observed-display-return-pattern")) {
            String observedAction = event.equals("switch") ? "switch" : "move";
            predictionOutcomes.add(new PredictionOutcome(id, "switch", "switch",
                    observedAction, event.equals("switch"), "candidate-pattern-only"));
            baselineOutcomes.add(new PredictionOutcome(id, "switch", "move",
                    observedAction, event.equals("move"), "always-move"));
        } else {
            skippedPredictionOutcomes++;
        }
    }

    /** Public switch patterns are evidence of behavior, never proof of intent. */
    public record SwitchPattern(String slot, String firstActor, String middleActor,
                                String returningActor, boolean identityConfirmed) {}
    private final List<SwitchPattern> switchPatterns = new ArrayList<>();
    private final Map<String, String> previousActorBySlot = new HashMap<>();
    private final Map<String, String> priorActorBySlot = new HashMap<>();
    private final Map<String, Boolean> recentForcedBySlot = new HashMap<>();
    public List<SwitchPattern> observedSwitchPatterns() { return List.copyOf(switchPatterns); }

    private final List<ItemEvent> itemEvents = new ArrayList<>();
    public List<ItemEvent> observedItemEvents() { return List.copyOf(itemEvents); }

    public IdentityEvidence activeIdentityEvidence(String slot) {
        String id = active.get(slot);
        Mutable m = history.get(id);
        if (m == null) return null;
        Set<String> candidates = new LinkedHashSet<>();
        for (var entry : history.entrySet())
            if (!entry.getKey().equals(id) && entry.getValue().actor.equals(m.actor))
                candidates.add(entry.getKey());
        // Matching visible species/nickname is not a unique party identifier.
        return new IdentityEvidence(id, Set.copyOf(candidates), false);
    }

    public List<RoleEvidence> activeRoles(String slot) {
        String id = active.get(slot);
        Mutable m = history.get(id);
        if (m == null) return List.of();
        List<RoleEvidence> roles = new ArrayList<>();
        if (m.setup > 0) roles.add(new RoleEvidence("setup-threat",
                m.setup > 1 ? "repeated-observation" : "single-observation", Set.of("setup-move")));
        if (m.pivot > 0) roles.add(new RoleEvidence("pivot",
                m.pivot > 1 ? "repeated-observation" : "single-observation", Set.of("pivot-move")));
        if (m.recovery > 0 && m.defensive > 0)
            roles.add(new RoleEvidence("defensive-pattern", "multiple-behaviors",
                    Set.of("recovery-move", "defensive-move")));
        if (m.priority > 0)
            roles.add(new RoleEvidence("priority-threat", "observed-move", Set.of("priority-move")));
        // No speed tier, defensive bulk, or damage role is inferred from species alone.
        return List.copyOf(roles);
    }

    private static final Set<String> SETUP = Set.of("swordsdance","nastyplot","dragondance","calmmind","geomancy","shellsmash","bulkup","quiverdance","irondefense");
    private static final Set<String> RECOVERY = Set.of("recover","roost","slackoff","softboiled","wish","rest","synthesis","moonlight","morningsun","shoreup");
    private static final Set<String> PIVOT = Set.of("uturn","voltswitch","flipturn","partingshot","batonpass","teleport","chillyreception");
    private static final Set<String> PRIORITY = Set.of("aquajet","extremespeed","shadowsneak","suckerpunch","bulletpunch","iceshard","machpunch","fakeout","grassyglide");
    private static final Set<String> DEFENSIVE = Set.of("protect","detect","substitute","toxic","willowisp","thunderwave","haze","reflect","lightscreen","stealthrock","spikes");
    private static final Set<String> CHOICE = Set.of("choiceband","choicescarf","choicespecs");

    private final Map<String, Mutable> history = new LinkedHashMap<>();
    private final Map<String, String> active = new HashMap<>();
    private final Map<String, Integer> nameAppearances = new HashMap<>();
    private int serial;
    private static final class Mutable {
        String actor, item, ability, status, lastItem, choiceLock;
        boolean transformed, itemRemoved, itemSuppressed, ambiguous, illusionRevealed;
        int setup, recovery, pivot, priority, offensive, defensive, in = 1, out;
        final Map<String,Integer> moveUses = new LinkedHashMap<>();
        final Map<String,Integer> moveLastSeen = new HashMap<>();
        int nativeMoveObservations;
        final Set<String> abilityActivations = new LinkedHashSet<>();
        Mutable(String actor) { this.actor = actor; }
    }

    public void reset() { history.clear(); active.clear(); nameAppearances.clear(); itemEvents.clear(); switchPatterns.clear(); predictionOutcomes.clear(); baselineOutcomes.clear(); skippedPredictionOutcomes = 0; previousActorBySlot.clear(); priorActorBySlot.clear(); recentForcedBySlot.clear(); serial = 0; }

    public void accept(String line) {
        if (line == null || !line.startsWith("|")) return;
        String[] p = line.split("\\|", -1);
        if (p.length < 3) return;
        String event = p[1], actor = p[2].trim(), slot = slot(actor);
        if (slot == null) return;
        auditPublicAction(event, slot, p);
        if (event.equals("switch") || event.equals("drag")) {
            String previous = active.get(slot);
            String lastActor = previousActorBySlot.get(slot);
            String beforeLastActor = priorActorBySlot.get(slot);
            if (!event.equals("drag") && !recentForcedBySlot.getOrDefault(slot, false)
                    && beforeLastActor != null && beforeLastActor.equals(actor)
                    && lastActor != null && !lastActor.equals(actor))
                switchPatterns.add(new SwitchPattern(slot, beforeLastActor, lastActor,
                        actor, false));
            recentForcedBySlot.put(slot, event.equals("drag"));
            priorActorBySlot.put(slot, lastActor);
            previousActorBySlot.put(slot, actor);
            if (previous != null && history.containsKey(previous)) history.get(previous).out++;
            String id = slot + "#" + (++serial);
            Mutable m = new Mutable(actor);
            int appearances = nameAppearances.merge(actor, 1, Integer::sum);
            m.ambiguous = appearances > 1;
            history.put(id, m);
            active.put(slot, id);
            return;
        }
        String id = active.get(slot);
        if (id == null) return;
        Mutable m = history.get(id);
        if (m == null) return;
        switch (event) {
            case "replace" -> { // Illusion: same appearance, but previous apparent species is untrusted
                m.actor = actor;
                // Illusion does not undo moves already publicly used by this
                // battler. It only invalidates apparent-species assumptions.
                // Pre-reveal native ability/item assumptions and apparent
                // species attribution cannot be trusted after Illusion.
                // Moves actually performed remain public evidence of the
                // real combatant; apparent ability and item attribution
                // is not trusted without a post-reveal confirmation.
                m.ability = null;
                m.item = null;
                m.lastItem = null;
                m.itemRemoved = false;
                m.itemSuppressed = false;
                m.choiceLock = null;
                m.abilityActivations.clear();
                m.illusionRevealed = true;
                m.ambiguous = true;
            }
            case "move" -> {
                if (p.length < 4) return;
                String move = canonical(p[3]);
                if (move.isEmpty()) return;
                // Transformed moves are copied, not native; still subject to Choice restriction.
                if (!m.transformed && !hasMoveOverride(p)) {
                    m.moveUses.merge(move, 1, Integer::sum);
                    m.moveLastSeen.put(move, ++m.nativeMoveObservations);
                    if (SETUP.contains(move)) m.setup++;
                    else if (RECOVERY.contains(move)) m.recovery++;
                    else if (PIVOT.contains(move)) m.pivot++;
                    else if (PRIORITY.contains(move)) m.priority++;
                    else if (DEFENSIVE.contains(move)) m.defensive++;
                    else m.offensive++;
                }
                if (m.item != null && CHOICE.contains(m.item) && !m.itemSuppressed && !hasMoveOverride(p))
                    if (m.choiceLock == null) m.choiceLock = move;
            }
            case "-item" -> {
                if (p.length < 4) return;
                m.item = canonical(p[3]);
                m.lastItem = m.item;
                m.itemRemoved = false;
                m.choiceLock = null;
                itemEvents.add(new ItemEvent(id, "revealed", m.item, source(p)));
            }
            case "-enditem" -> {
                if (p.length > 3) m.lastItem = canonical(p[3]);
                m.item = null; m.itemRemoved = true; m.choiceLock = null;
                itemEvents.add(new ItemEvent(id, "ended", m.lastItem, source(p)));
            }
            case "-ability" -> {
                if (p.length < 4) return;
                if (!m.transformed) {
                    m.ability = canonical(p[3]);
                    // -ability reveals an ability; activation requires explicit public evidence.
                }
            }
            case "-endability" -> {
                // An ability ending neither proves activation nor current ability.
                // Preserve previous public knowledge without rewriting it.
            }
            case "-activate" -> {
                if (p.length > 3 && !m.transformed && p[3].trim().toLowerCase(Locale.ROOT).startsWith("ability:")) {
                    String ability = canonical(p[3].substring("ability:".length()));
                    if (!ability.isEmpty()) {
                        m.ability = ability;
                        m.abilityActivations.add(ability);
                    }
                }
            }
            case "-status" -> { if (p.length > 3) m.status = canonical(p[3]); }
            case "-curestatus" -> m.status = null;
            case "-transform" -> { m.transformed = true; m.choiceLock = null; }
            case "-start" -> {
                if (p.length > 3 && canonical(p[3]).equals("embargo")) {
                    m.itemSuppressed = true; m.choiceLock = null;
                }
            }
            case "-end" -> {
                if (p.length > 3 && canonical(p[3]).equals("embargo")) {
                    m.itemSuppressed = false; m.choiceLock = null;
                }
            }
            case "faint" -> { m.choiceLock = null; }
            default -> { }
        }
    }

    public Threat activeThreat(String slot) {
        String id = active.get(slot);
        return id == null ? null : threat(id);
    }

    public Assessment activeAssessment(String slot) {
        String id = active.get(slot);
        Mutable m = history.get(id);
        if (m == null) return null;
        return new Assessment(threat(id), Map.copyOf(m.moveUses), m.setup, m.recovery,
                m.pivot, m.priority, m.offensive, m.defensive, m.in, m.out,
                m.itemRemoved, m.itemSuppressed, m.choiceLock,
                Set.copyOf(m.abilityActivations), m.ambiguous);
    }

    public List<Threat> observedAppearances() {
        List<Threat> result = new ArrayList<>();
        for (String id : history.keySet()) result.add(threat(id));
        return List.copyOf(result);
    }

    private Threat threat(String id) {
        Mutable m = history.get(id);
        if (m == null) return null;
        Set<String> tags = new LinkedHashSet<>();
        if (m.setup > 0) tags.add("revealed-setup");
        if (m.pivot > 0) tags.add("revealed-pivot");
        if (m.recovery > 0) tags.add("revealed-recovery");
        if (m.priority > 0) tags.add("revealed-priority");
        if (m.setup >= 2) tags.add("repeated-setup");
        if (m.recovery >= 2) tags.add("repeated-recovery");
        if (m.offensive > m.defensive + m.recovery + m.setup) tags.add("observed-aggression");
        if (m.defensive + m.recovery > m.offensive) tags.add("observed-defense");
        if (m.ambiguous) tags.add("identity-ambiguous");
        if (m.illusionRevealed) tags.add("illusion-revealed");
        if (m.setup > 0 && m.offensive > 0) tags.add("observed-setup-attacker");
        if (m.recovery > 0 && m.defensive > 0) tags.add("observed-defensive-pattern");
        if (m.pivot > 0) tags.add("observed-pivot-pattern");
        if (m.priority > 0) tags.add("observed-priority-option");
        return new Threat(id, m.actor, Set.copyOf(m.moveUses.keySet()), m.item,
                m.ability, m.status, m.transformed, Set.copyOf(tags));
    }

    private static String source(String[] p) {
        for (int i = 4; i < p.length; i++)
            if (p[i].startsWith("[from]")) return p[i].substring(6).trim();
        return "unspecified";
    }

    private static boolean hasMoveOverride(String[] p) {
        for (int i = 4; i < p.length; i++)
            if (p[i].contains("[from]") || p[i].contains("[still]")) return true;
        return false;
    }
    private static String slot(String actor) {
        int colon = actor.indexOf(':');
        if (colon < 0) return null;
        String prefix = actor.substring(0, colon).trim();
        return prefix.matches("p[1-4][a-c]") ? prefix : null;
    }
    private static String canonical(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    /** Public native-use counts, in appearance order, never linked by nickname.
     * Historical values are observations, not future-move probabilities. */
    public List<Map<String,Integer>> observedNativeMoveUseCounts() {
        List<Map<String,Integer>> result = new ArrayList<>();
        for (Mutable m : history.values())
            result.add(Collections.unmodifiableMap(new TreeMap<>(m.moveUses)));
        return List.copyOf(result);
    }
}
