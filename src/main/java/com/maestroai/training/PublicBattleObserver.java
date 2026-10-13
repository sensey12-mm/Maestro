package com.maestroai.training;

import java.util.*;

/**
 * Incremental, public-information-only Showdown event projection.
 * Call accept only with PUBLIC protocol lines, never private request payloads.
 * All state is shadow-only; it does not select battle actions.
 */
public final class PublicBattleObserver {
    private final PublicOpponentIntelligence opponentIntelligence = new PublicOpponentIntelligence();
    private final ShadowSwitchHistory switchHistory = new ShadowSwitchHistory();
    private int publicTurn;
    public ShadowSwitchHistory switchHistory() { return switchHistory; }
    public int publicTurn() { return publicTurn; }
    public PublicOpponentIntelligence opponentIntelligence() { return opponentIntelligence; }
    private final Map<String, LinkedHashSet<String>> moves = new HashMap<>();
    private final Map<String, Map<String, Integer>> stages = new HashMap<>();
    private final Map<String, String> activeBySlot = new HashMap<>();
    private final Map<String, String> publicSpeciesBySlot = new HashMap<>();
    // Only explicit public |-terastallize| announcements establish a Tera type.
    private final Map<String, String> publicTeraBySlot = new HashMap<>();
    private final Set<String> uncertain = new HashSet<>();
    private final Map<String, String> revealedItems = new HashMap<>();
    private final Map<String, String> revealedAbilities = new HashMap<>();
    private final Map<String, String> activeItems = new HashMap<>();
    private final Set<String> removedItems = new HashSet<>();
    private final Map<String, String> choiceLocks = new HashMap<>();
    private final Set<String> suppressedItems = new HashSet<>();
    private final Map<String, Integer> identityOccurrences = new HashMap<>();
    private final Map<String, String> statuses = new HashMap<>();
    private final Set<String> transformed = new HashSet<>();

    public void reset() {
        opponentIntelligence.reset();
        switchHistory.reset();
        publicTurn = 0;
        moves.clear();
        stages.clear();
        activeBySlot.clear();
        publicSpeciesBySlot.clear();
        publicTeraBySlot.clear();
        uncertain.clear();
        revealedItems.clear();
        revealedAbilities.clear();
        activeItems.clear();
        removedItems.clear();
        choiceLocks.clear();
        suppressedItems.clear();
        identityOccurrences.clear();
        statuses.clear();
        transformed.clear();
    }

    public void accept(String publicLine) {
        if (publicLine == null || !publicLine.startsWith("|")) return;
        String[] p = publicLine.split("\\|", -1);
        if (p.length < 2) return;
        opponentIntelligence.accept(publicLine);
        String event = p[1];
        if (event.equals("turn")) {
            if (p.length > 2) try { publicTurn = Integer.parseInt(p[2].trim()); }
            catch (NumberFormatException ignored) { /* preserve last valid public turn */ }
            return;
        }
        if (event.equals("switch") || event.equals("drag")) {
            String actorSlot = p.length > 2 ? slot(p[2]) : null;
            String foeSlot = actorSlot == null ? null : (actorSlot.startsWith("p1") ? "p2a" : "p1a");
            switchHistory.observe(publicTurn, publicLine,
                    foeSlot == null ? null : activeBySlot.get(foeSlot));
        }
        if ("-clearallboost".equals(event)) {
            stages.clear();
            uncertain.clear();
            return;
        }
        if (p.length < 3) return;
        String slot = slot(p[2]);
        switch (event) {
            case "replace" -> {
                setPublicSpecies(slot, p.length > 3 ? p[3] : null);
                // Illusion reveal changes public appearance, not the active combatant.
                // Keep its combat state; never transfer old appearance's move/item
                // evidence as proof of the newly revealed species.
                if (slot == null) return;
                String previous = activeBySlot.get(slot);
                String revealed = p[2].trim();
                if (previous == null) {
                    activeBySlot.put(slot, revealed);
                    uncertain.add(revealed);
                } else if (!previous.equals(revealed)) {
                    // The apparent species was an Illusion. Its attributed
                    // native evidence is invalid; do not retain it as fact.
                    moves.remove(previous);
                    revealedItems.remove(previous);
                    revealedAbilities.remove(previous);
                    activeItems.remove(previous);
                    removedItems.remove(previous);
                    suppressedItems.remove(previous);
                    Map<String, Integer> previousStages = stages.remove(previous);
                    if (previousStages != null) stages.put(revealed, previousStages);
                    String previousStatus = statuses.remove(previous);
                    if (previousStatus != null) statuses.put(revealed, previousStatus);
                    if (transformed.remove(previous)) transformed.add(revealed);
                    choiceLocks.remove(previous);
                    choiceLocks.remove(revealed);
                    activeBySlot.put(slot, revealed);
                    uncertain.add(revealed);
                }
            }
            case "switch", "drag" -> {
                // The previous active combatant's Tera cannot be silently
                // attributed to an unverified new appearance at this slot.
                if (slot != null) publicTeraBySlot.remove(slot);
                setPublicSpecies(slot, p.length > 3 ? p[3] : null);
                if (slot == null) return;
                String rawIdentity = p[2].trim();
                // Without a public party index, re-entry could be a duplicate
                // species/nickname. Keep evidence isolated rather than merge
                // possibly distinct Pokémon based only on displayed appearance.
                String identity = rawIdentity;
                if (!rawIdentity.equals(activeBySlot.get(slot))) {
                    int count = identityOccurrences.merge(rawIdentity, 1, Integer::sum);
                    if (count > 1) identity = rawIdentity + "#" + count;
                }
                String previous = activeBySlot.put(slot, identity);
                if (previous != null) {
                    stages.remove(previous);
                    uncertain.remove(previous);
                    transformed.remove(previous);
                    choiceLocks.remove(previous);
                    suppressedItems.remove(previous);
                }
                stages.remove(identity);
                uncertain.remove(identity);
                transformed.remove(identity);
                choiceLocks.remove(identity);
                suppressedItems.remove(identity);
            }
            case "-terastallize" -> {
                if (slot != null && p.length > 3) {
                    String revealedType = canonical(p[3]);
                    if (!revealedType.isBlank()) publicTeraBySlot.put(slot, revealedType);
                }
            }
            case "move" -> {
                if (p.length < 4) return;
                String who = active(slot, p[2]);
                String move = canonical(p[3]);
                if (!move.isBlank() && !transformed.contains(who)) moves.computeIfAbsent(who, k -> new LinkedHashSet<>()).add(move);
                if (!move.isBlank() && isChoiceItem(activeItems.get(who)) && !suppressedItems.contains(who)
                        && !publicMoveOverride(p)) choiceLocks.putIfAbsent(who, move);
            }
            case "-item", "-enditem", "-ability", "-endability" -> {
                if (p.length < 4) return;
                String who = active(slot, p[2]);
                String value = canonical(p[3]);
                if (value.isBlank()) return;
                if (event.equals("-item") || event.equals("-enditem")) {
                    revealedItems.put(who, value);
                    if (event.equals("-enditem")) {
                        activeItems.remove(who);
                        removedItems.add(who);
                        choiceLocks.remove(who);
                    } else {
                        activeItems.put(who, value);
                        removedItems.remove(who);
                        choiceLocks.remove(who);
                    }
                } else if (!transformed.contains(who)) {
                    revealedAbilities.put(who, value);
                }
            }
            case "-status" -> {
                if (p.length < 4) return;
                String who = active(slot, p[2]);
                String status = canonical(p[3]);
                if (Set.of("brn", "par", "psn", "tox", "slp", "frz").contains(status))
                    statuses.put(who, status);
            }
            case "-curestatus" -> statuses.remove(active(slot, p[2]));
            case "-start" -> {
                if (p.length > 3 && canonical(p[3]).equals("embargo")) {
                    String who = active(slot, p[2]);
                    suppressedItems.add(who);
                    choiceLocks.remove(who);
                }
            }
            case "-end" -> {
                // An expired suppression does not prove which move is currently locked.
                if (p.length > 3 && canonical(p[3]).equals("embargo")) {
                    String who = active(slot, p[2]);
                    suppressedItems.remove(who);
                    choiceLocks.remove(who);
                }
            }
            case "-transform" -> {
                String who = active(slot, p[2]);
                transformed.add(who);
                choiceLocks.remove(who);
                // Transform copies target's current stages; do not assume
                // known neutral stages or attribute copied moves/ability.
                stages.remove(who);
                uncertain.add(who);
            }
            case "-boost", "-unboost", "-setboost" -> {
                if (p.length < 5) return;
                String who = active(slot, p[2]);
                String stat = canonical(p[3]);
                int amount;
                try { amount = Integer.parseInt(p[4].trim()); }
                catch (NumberFormatException ignored) { uncertain.add(who); return; }
                if (!Set.of("atk", "def", "spa", "spd", "spe", "accuracy", "evasion").contains(stat)) return;
                Map<String, Integer> current = stages.computeIfAbsent(who, k -> new HashMap<>());
                int next = event.equals("-setboost") ? amount
                        : current.getOrDefault(stat, 0) + (event.equals("-boost") ? amount : -amount);
                current.put(stat, Math.max(-6, Math.min(6, next)));
            }
            case "-clearboost" -> {
                String who = active(slot, p[2]);
                stages.remove(who);
                uncertain.remove(who);
            }
            case "-clearallboost" -> { stages.clear(); uncertain.clear(); }
            case "-copyboost", "-swapboost", "-invertboost" -> {
                // Complex changes must not be represented as known neutral stages.
                uncertain.add(active(slot, p[2]));
                if (p.length > 3) uncertain.add(active(slot(p[3]), p[3]));
            }
            default -> { }
        }
    }

    public List<String> revealedMoves(String identity) {
        return List.copyOf(moves.getOrDefault(identity, new LinkedHashSet<>()));
    }

    public Map<String, Integer> knownStages(String identity) {
        if (uncertain.contains(identity)) return Map.of();
        return Map.copyOf(stages.getOrDefault(identity, Map.of()));
    }

    public String activeIdentity(String slot) { return activeBySlot.get(slot); }
    public String publicSpecies(String slot) { return publicSpeciesBySlot.get(slot); }
    public String publicTeraType(String slot) { return publicTeraBySlot.get(slot); }
    private void setPublicSpecies(String slot, String details) {
        if (slot == null) return;
        String species = speciesFromDetails(details);
        if (species.isBlank()) publicSpeciesBySlot.remove(slot);
        else publicSpeciesBySlot.put(slot, species);
    }
    private static String speciesFromDetails(String details) {
        return canonical(details == null ? "" : details.split(",", 2)[0]);
    }
    public String revealedItem(String identity) { return revealedItems.get(identity); }
    public String revealedAbility(String identity) { return revealedAbilities.get(identity); }
    public boolean stagesUncertain(String identity) { return uncertain.contains(identity); }
    public String currentItem(String identity) { return activeItems.get(identity); }
    public ShadowMechanicsPredictor.ItemKnowledge itemKnowledge(String identity) {
        if (activeItems.containsKey(identity)) return ShadowMechanicsPredictor.ItemKnowledge.HELD;
        if (removedItems.contains(identity)) return ShadowMechanicsPredictor.ItemKnowledge.CONSUMED_OR_REMOVED;
        return ShadowMechanicsPredictor.ItemKnowledge.UNKNOWN;
    }
    public String observedChoiceLock(String identity) { return choiceLocks.get(identity); }
    public boolean itemSuppressed(String identity) { return suppressedItems.contains(identity); }
    private static boolean publicMoveOverride(String[] parts) {
        for (int i = 4; i < parts.length; i++) {
            if (parts[i].contains("[from]") || parts[i].contains("[still]")) return true;
        }
        return false;
    }
    private static boolean isChoiceItem(String item) {
        return Set.of("choiceband", "choicespecs", "choicescarf").contains(item == null ? "" : item);
    }
    public String observedStatus(String identity) { return statuses.get(identity); }
    public boolean transformed(String identity) { return transformed.contains(identity); }

    private String active(String slot, String raw) {
        return activeBySlot.getOrDefault(slot, raw.trim());
    }

    private static String slot(String actor) {
        if (actor == null) return null;
        int colon = actor.indexOf(':');
        if (colon < 0) return null;
        String prefix = actor.substring(0, colon).trim();
        return prefix.matches("p[1-4][a-c]") ? prefix : null;
    }

    private static String canonical(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
