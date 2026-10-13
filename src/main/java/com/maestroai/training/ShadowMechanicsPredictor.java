package com.maestroai.training;

import java.util.Locale;
import java.util.Set;

/**
 * Conservative shadow-only mechanics predictions for public observations.
 * A missing reveal is UNKNOWN, never evidence that an item/ability is absent.
 * All outcomes are conditional estimates, not calls into Showdown.
 */
public final class ShadowMechanicsPredictor {
    private ShadowMechanicsPredictor() {}

    public enum Verdict { YES, NO, UNKNOWN }
    public record Prediction(Verdict verdict, String reason) {
        public Prediction {
            if (verdict == null || reason == null || reason.isBlank())
                throw new IllegalArgumentException("invalid prediction");
        }
    }
    public record Modifier(double multiplier, boolean known, String reason) {
        public Modifier {
            if (!Double.isFinite(multiplier) || multiplier <= 0 || reason == null)
                throw new IllegalArgumentException("invalid modifier");
        }
    }

    private static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** An explicit publicly observed status overrides any speculative immunity. */
    public static Prediction canInflictStatus(String status, Set<String> targetTypes,
                                              String publiclyRevealedAbility) {
        String s = key(status);
        Set<String> types = targetTypes == null ? Set.of() :
                targetTypes.stream().map(ShadowMechanicsPredictor::key)
                        .collect(java.util.stream.Collectors.toSet());
        String ability = key(publiclyRevealedAbility);
        if (s.equals("brn") && types.contains("fire"))
            return new Prediction(Verdict.NO, "Fire type resists ordinary burn application");
        if ((s.equals("psn") || s.equals("tox"))
                && (types.contains("steel") || types.contains("poison")))
            return new Prediction(Verdict.UNKNOWN,
                    "Usually poison-immune type, but Corrosion or other exceptions may apply");
        if (s.equals("par") && types.contains("electric"))
            return new Prediction(Verdict.UNKNOWN,
                    "Usually paralysis-immune type; effect-specific exceptions require Showdown");
        if (ability.equals("limber") && s.equals("par"))
            return new Prediction(Verdict.NO, "Revealed Limber blocks ordinary paralysis");
        if (ability.equals("immunity") && (s.equals("psn") || s.equals("tox")))
            return new Prediction(Verdict.NO, "Revealed Immunity blocks ordinary poison");
        if (ability.equals("waterveil") && s.equals("brn"))
            return new Prediction(Verdict.NO, "Revealed Water Veil blocks ordinary burn");
        return new Prediction(Verdict.UNKNOWN, "Other immunities and effect context unverified");
    }

    /**
     * Single-source multiplier only. Do not multiply with other effects unless
     * the complete Showdown damage context has been checked.
     */
    public static Modifier publicDamageModifier(String item, String ability,
                                                String moveCategory, String moveType) {
        String held = key(item), ab = key(ability), cat = key(moveCategory), type = key(moveType);
        if (ab.equals("fairyaura") && type.equals("fairy"))
            return new Modifier(4.0 / 3.0, true, "Revealed Fairy Aura, assuming no Aura Break");
        if (held.equals("lifeorb") && (cat.equals("physical") || cat.equals("special")))
            return new Modifier(1.3, true, "Revealed currently held Life Orb, damage move");
        if (held.equals("choiceband") && cat.equals("physical"))
            return new Modifier(1.5, true, "Revealed currently held Choice Band; Attack modifier");
        if (held.equals("choicespecs") && cat.equals("special"))
            return new Modifier(1.5, true, "Revealed currently held Choice Specs; SpA modifier");
        return new Modifier(1.0, false, "No verified public single-source damage modifier");
    }

    public static Prediction itemConsumption(String currentlyObservedItem,
                                             String trigger, boolean fullHp) {
        String item = key(currentlyObservedItem), t = key(trigger);
        if (item.isBlank())
            return new Prediction(Verdict.UNKNOWN, "Item not publicly known to be held");
        if (item.equals("powerherb") && t.equals("chargingmove"))
            return new Prediction(Verdict.YES, "Power Herb normally consumed on charge skip");
        if (item.equals("focussash") && t.equals("fatalhit") && fullHp)
            return new Prediction(Verdict.YES, "Focus Sash normally consumed to survive");
        if (item.equals("focussash") && t.equals("fatalhit") && !fullHp)
            return new Prediction(Verdict.NO, "Focus Sash requires full HP");
        return new Prediction(Verdict.UNKNOWN, "Trigger or suppression not modeled");
    }

    public static Prediction abilityActivation(String publiclyRevealedAbility, String trigger) {
        String ability = key(publiclyRevealedAbility), t = key(trigger);
        if (ability.isBlank())
            return new Prediction(Verdict.UNKNOWN, "Ability not publicly revealed");
        if (ability.equals("intimidate") && t.equals("switchin"))
            return new Prediction(Verdict.YES, "Intimidate normally attempts an Attack drop");
        if (ability.equals("download") && t.equals("switchin"))
            return new Prediction(Verdict.YES, "Download normally attempts a stat boost");
        if (ability.equals("imposter") && t.equals("switchin"))
            return new Prediction(Verdict.UNKNOWN, "Imposter depends on valid transform target");
        return new Prediction(Verdict.UNKNOWN, "Activation conditions not fully modeled");
    }
    /**
     * Context-aware status check. A revealed ability may be suppressed; an
     * unknown ability cannot be treated as absent. This does not infer a
     * target's hidden ability from its species.
     */
    public static Prediction statusWithContext(String status, Set<String> targetTypes,
                                               String revealedAbility, boolean abilityActive,
                                               boolean bypassesTypeImmunity) {
        String s = key(status);
        Set<String> types = targetTypes == null ? Set.of() :
                targetTypes.stream().map(ShadowMechanicsPredictor::key)
                        .collect(java.util.stream.Collectors.toSet());
        String ability = abilityActive ? key(revealedAbility) : "";
        if (s.equals("brn") && types.contains("fire") && !bypassesTypeImmunity)
            return new Prediction(Verdict.NO, "Fire typing blocks ordinary burn");
        if ((s.equals("psn") || s.equals("tox"))
                && (types.contains("steel") || types.contains("poison"))
                && !bypassesTypeImmunity)
            return new Prediction(Verdict.UNKNOWN, "Corrosion or effect-specific exceptions not excluded");
        if (s.equals("par") && types.contains("electric") && !bypassesTypeImmunity)
            return new Prediction(Verdict.NO, "Electric typing blocks ordinary paralysis");
        if (ability.equals("limber") && s.equals("par"))
            return new Prediction(Verdict.NO, "Active Limber prevents paralysis");
        if (ability.equals("immunity") && (s.equals("psn") || s.equals("tox")))
            return new Prediction(Verdict.NO, "Active Immunity prevents poison");
        if (ability.equals("waterveil") && s.equals("brn"))
            return new Prediction(Verdict.NO, "Active Water Veil prevents burn");
        return new Prediction(Verdict.UNKNOWN, "Insufficient public effect and suppression context");
    }

    /**
     * A definite activation requires a valid, unsuppressed trigger and target.
     * 'YES' means an activation attempt, not guaranteed stat change.
     */
    public static Prediction abilityActivationWithContext(String revealedAbility,
            String trigger, boolean abilityActive, boolean validTarget) {
        String ability = key(revealedAbility), event = key(trigger);
        if (ability.isBlank())
            return new Prediction(Verdict.UNKNOWN, "Ability has not been publicly revealed");
        if (!abilityActive)
            return new Prediction(Verdict.NO, "Known ability is inactive or suppressed");
        if (!event.equals("switchin"))
            return new Prediction(Verdict.UNKNOWN, "Trigger not modeled");
        if (!validTarget)
            return new Prediction(Verdict.UNKNOWN, "Target or activation conditions not established");
        if (ability.equals("intimidate") || ability.equals("download"))
            return new Prediction(Verdict.YES, "Expected switch-in activation attempt; outcome depends on target mechanics");
        if (ability.equals("imposter"))
            return new Prediction(Verdict.UNKNOWN, "Transform eligibility requires more target state");
        return new Prediction(Verdict.UNKNOWN, "Ability trigger not modeled");
    }

    /** Known held item is not enough to establish that its effect is enabled. */
    public static Prediction itemConsumptionWithContext(String currentItem, String trigger,
            boolean fullHp, boolean itemActive, boolean effectResolved) {
        String item = key(currentItem), event = key(trigger);
        if (item.isBlank())
            return new Prediction(Verdict.UNKNOWN, "No publicly established current held item");
        if (!itemActive)
            return new Prediction(Verdict.NO, "Item effect is inactive");
        if (item.equals("focussash") && event.equals("fatalhit") && !fullHp)
            return new Prediction(Verdict.NO, "Focus Sash requires full HP");
        if (!effectResolved)
            return new Prediction(Verdict.UNKNOWN, "Trigger resolution not publicly established");
        return itemConsumption(currentItem, trigger, fullHp);
    }

    /** A modifier's phase is part of its meaning; do not combine unlike phases
     * into a purported exact Showdown damage multiplier. */
    public enum DamagePhase { ATTACK_STAT, BASE_POWER, FINAL_DAMAGE }
    public record DamageFactor(DamagePhase phase, double multiplier, String source) {
        public DamageFactor {
            if (phase == null || !Double.isFinite(multiplier) || multiplier <= 0
                    || source == null || source.isBlank())
                throw new IllegalArgumentException("invalid damage factor");
        }
    }
    public record DamagePrediction(java.util.List<DamageFactor> factors,
                                   boolean complete, String reason) {
        public DamagePrediction {
            factors = java.util.List.copyOf(factors);
            if (reason == null || reason.isBlank())
                throw new IllegalArgumentException("invalid damage prediction");
        }
    }

    /**
     * Report separately verified modifier phases without claiming a final
     * damage roll. Inputs must be derived from public observation only.
     * In particular, null currentItem is NOT proof of an empty item slot.
     */
    public static DamagePrediction damageFactors(String currentItem, String revealedAbility,
            String category, String moveType, boolean itemActive, boolean abilityActive,
            boolean auraBreakKnownAbsent) {
        String item = key(currentItem), ability = key(revealedAbility);
        String cat = key(category), type = key(moveType);
        java.util.List<DamageFactor> factors = new java.util.ArrayList<>();
        boolean complete = !item.isBlank() && !ability.isBlank();
        if (itemActive) {
            if (item.equals("choiceband") && cat.equals("physical"))
                factors.add(new DamageFactor(DamagePhase.ATTACK_STAT, 1.5, "Choice Band"));
            if (item.equals("choicespecs") && cat.equals("special"))
                factors.add(new DamageFactor(DamagePhase.ATTACK_STAT, 1.5, "Choice Specs"));
            if (item.equals("lifeorb") && (cat.equals("physical") || cat.equals("special")))
                factors.add(new DamageFactor(DamagePhase.FINAL_DAMAGE, 1.3, "Life Orb"));
        }
        if (abilityActive && ability.equals("fairyaura") && type.equals("fairy")) {
            if (auraBreakKnownAbsent)
                factors.add(new DamageFactor(DamagePhase.BASE_POWER, 4.0 / 3.0, "Fairy Aura"));
            else
                complete = false;
        }
        // Even when all modeled inputs are known, other mechanics may matter.
        return new DamagePrediction(factors, false,
                complete ? "Selected public factors only; full battle context unmodeled"
                         : "Unknown or conditional public factors; not an exact damage roll");
    }

    /** Public evidence about possession is distinct from the last item reveal. */
    public enum ItemKnowledge { UNKNOWN, HELD, CONSUMED_OR_REMOVED }
    public enum ItemEvent { CHARGE_SKIPPED, LETHAL_DIRECT_HIT, DAMAGING_HIT,
        MOVE_SELECTED, ABILITY_BOOST, ITEM_REMOVED, END_TURN }
    public record ItemContext(String item, ItemKnowledge knowledge, ItemEvent event,
            boolean effectEnabled, boolean fullHp, boolean directHit,
            boolean survivedAtOneHp, boolean moveSucceeded,
            boolean abilityActivated, boolean terrainOrWeatherBoostActive) {
        public ItemContext {
            if (knowledge == null || event == null)
                throw new IllegalArgumentException("item context requires knowledge and event");
        }
    }
    public record ItemOutcome(Prediction consumption, Prediction activation,
                              String consequence) {
        public ItemOutcome {
            if (consumption == null || activation == null || consequence == null)
                throw new IllegalArgumentException("invalid item outcome");
        }
    }
    private static Prediction verdict(Verdict value, String why) {
        return new Prediction(value, why);
    }

    /**
     * Conservative item event interpretation. YES for consumption requires an
     * observed resolved trigger, not merely a potential future attack.
     * These observations must come from public events, never private requests.
     */
    public static ItemOutcome predictItem(ItemContext c) {
        if (c.knowledge() == ItemKnowledge.UNKNOWN || key(c.item()).isBlank())
            return new ItemOutcome(verdict(Verdict.UNKNOWN, "Held item not publicly established"),
                    verdict(Verdict.UNKNOWN, "Item effect cannot be established"),
                    "No assumption about an unrevealed item");
        if (c.knowledge() == ItemKnowledge.CONSUMED_OR_REMOVED)
            return new ItemOutcome(verdict(Verdict.NO, "Item no longer held"),
                    verdict(Verdict.NO, "Item no longer held"), "Historical reveal only");
        if (!c.effectEnabled())
            return new ItemOutcome(verdict(Verdict.NO, "Item effect disabled"),
                    verdict(Verdict.NO, "Item effect disabled"), "Suppressed item");
        String item = key(c.item());
        switch (item) {
            case "powerherb":
                if (c.event() == ItemEvent.CHARGE_SKIPPED && c.moveSucceeded())
                    return new ItemOutcome(verdict(Verdict.YES, "Observed charge skip"),
                            verdict(Verdict.YES, "Power Herb charge skip"), "Consumed after skipping charge");
                return new ItemOutcome(verdict(Verdict.UNKNOWN, "Charge skip not established"),
                        verdict(Verdict.UNKNOWN, "Charging move conditions unknown"), "Await resolved charge event");
            case "focussash":
                if (c.event() == ItemEvent.LETHAL_DIRECT_HIT && !c.fullHp())
                    return new ItemOutcome(verdict(Verdict.NO, "Not at full HP"),
                            verdict(Verdict.NO, "Focus Sash cannot activate below full HP"), "No Focus Sash survival");
                if (c.event() == ItemEvent.LETHAL_DIRECT_HIT && c.fullHp() && c.directHit()
                        && c.survivedAtOneHp())
                    return new ItemOutcome(verdict(Verdict.YES, "Observed eligible survival"),
                            verdict(Verdict.YES, "Survived direct fatal damage at 1 HP"), "Focus Sash consumed");
                return new ItemOutcome(verdict(Verdict.UNKNOWN, "Fatal direct hit and survival unconfirmed"),
                        verdict(Verdict.UNKNOWN, "Focus Sash activation unconfirmed"), "Do not infer survival");
            case "lifeorb":
                if (c.event() == ItemEvent.DAMAGING_HIT && c.moveSucceeded())
                    return new ItemOutcome(verdict(Verdict.NO, "Life Orb is not consumed on a hit"),
                            verdict(Verdict.YES, "Eligible damaging hit"), "1.3x final damage; recoil depends on immunity and effects");
                return new ItemOutcome(verdict(Verdict.NO, "Life Orb is not single-use"),
                        verdict(Verdict.UNKNOWN, "Damage outcome unconfirmed"), "Recoil not assumed");
            case "choiceband": case "choicespecs": case "choicescarf":
                return new ItemOutcome(verdict(Verdict.NO, "Choice item not consumed by use"),
                        verdict(Verdict.YES, "Choice item active while held"),
                        "Stat modifier and move restriction; locking exceptions require move context");
            case "boosterenergy":
                if (c.event() == ItemEvent.ABILITY_BOOST && c.abilityActivated()
                        && !c.terrainOrWeatherBoostActive())
                    return new ItemOutcome(verdict(Verdict.YES, "Observed Booster Energy ability boost"),
                            verdict(Verdict.YES, "Booster Energy activation"),
                            "Consumption follows eligible Protosynthesis or Quark Drive activation");
                return new ItemOutcome(verdict(Verdict.UNKNOWN, "Boost source not confirmed"),
                        verdict(Verdict.UNKNOWN, "Ability and field context required"),
                        "Do not infer Booster Energy from species");
            default:
                return new ItemOutcome(verdict(Verdict.UNKNOWN, "Unmodeled held item"),
                        verdict(Verdict.UNKNOWN, "Unmodeled held item"), "Unknown item interaction");
        }
    }

    /** Never infer a guaranteed Choice restriction from a historical item reveal. */
    public static Prediction choiceRestriction(String currentlyHeldItem, String observedLockedMove,
            String attemptedMove, boolean itemEffectsEnabled, boolean switchedSinceLock,
            boolean transformedSinceLock, boolean moveOverridesLock) {
        String item = key(currentlyHeldItem);
        if (item.isBlank()) return verdict(Verdict.UNKNOWN, "Current item unrevealed");
        if (!Set.of("choiceband", "choicespecs", "choicescarf").contains(item))
            return verdict(Verdict.NO, "Current item is not a Choice item");
        if (!itemEffectsEnabled || switchedSinceLock || transformedSinceLock || moveOverridesLock)
            return verdict(Verdict.UNKNOWN, "Lock not established under current battle conditions");
        String lock = key(observedLockedMove), move = key(attemptedMove);
        if (lock.isBlank() || move.isBlank())
            return verdict(Verdict.UNKNOWN, "Selected move or lock not publicly established");
        return verdict(lock.equals(move) ? Verdict.NO : Verdict.YES,
                "Conditional Choice restriction from publicly observed move");
    }

    /** Item recoil is separate from the Life Orb final-damage multiplier. */
    public static Prediction lifeOrbRecoil(String heldItem, boolean effectsEnabled,
            boolean moveDealtDamage, boolean magicGuardActive, boolean sheerForceApplied,
            boolean substituteTookDamage, boolean hitWasIndirect) {
        if (key(heldItem).isBlank()) return verdict(Verdict.UNKNOWN, "Current item unknown");
        if (!key(heldItem).equals("lifeorb")) return verdict(Verdict.NO, "No Life Orb held");
        if (!effectsEnabled || magicGuardActive || sheerForceApplied)
            return verdict(Verdict.NO, "Life Orb recoil blocked or item inactive");
        if (!moveDealtDamage) return verdict(Verdict.NO, "No damage dealt by move");
        if (hitWasIndirect) return verdict(Verdict.UNKNOWN, "Damage source requires confirmation");
        if (substituteTookDamage) return verdict(Verdict.UNKNOWN, "Substitute interaction requires battle validation");
        return verdict(Verdict.YES, "Eligible Life Orb damaging attack; normally loses 10 percent max HP");
    }

    /** Booster Energy cannot be identified solely by a stat increase or species. */
    public static Prediction boosterEnergyConsumption(String currentItem, String ability,
            boolean itemEnabled, boolean abilityEnabled, boolean fieldActivationPresent,
            boolean activationFromItemPubliclyConfirmed) {
        if (key(currentItem).isBlank() || key(ability).isBlank())
            return verdict(Verdict.UNKNOWN, "Item or ability not publicly known");
        if (!key(currentItem).equals("boosterenergy"))
            return verdict(Verdict.NO, "Booster Energy not currently held");
        if (!Set.of("quarkdrive", "protosynthesis").contains(key(ability)))
            return verdict(Verdict.NO, "No compatible revealed ability");
        if (!itemEnabled || !abilityEnabled) return verdict(Verdict.NO, "Item or ability inactive");
        if (activationFromItemPubliclyConfirmed && !fieldActivationPresent)
            return verdict(Verdict.YES, "Item-sourced ability activation publicly confirmed");
        return verdict(Verdict.UNKNOWN, "Weather, terrain or item activation source unresolved");
    }

}
