package com.maestroai.training;

import java.util.Locale;
import java.util.Map;

/**
 * Shadow-only public interaction annotations. A public reveal is not proof
 * of a current held item or an active ability after Transform/suppression.
 * Effects are advisory descriptions, never damage multipliers.
 */
public final class PublicInteractionModel {
    private PublicInteractionModel() {}

    public record Interaction(String id, String trigger, String consequence) {}

    private static final Map<String, Interaction> ITEMS = Map.of(
            "powerherb", new Interaction("powerherb", "charge move begins", "consumed to skip one charging turn"),
            "focussash", new Interaction("focussash", "full HP and otherwise fatal hit", "survives with 1 HP; item consumed"),
            "leftovers", new Interaction("leftovers", "end of turn while held", "heals 1/16 maximum HP if able"),
            "lifeorb", new Interaction("lifeorb", "damaging attack", "boosts damage by 30%; usually costs 1/10 maximum HP"),
            "choiceband", new Interaction("choiceband", "while held", "raises Attack 50%; locks move"),
            "choicespecs", new Interaction("choicespecs", "while held", "raises Special Attack 50%; locks move"),
            "choicescarf", new Interaction("choicescarf", "while held", "raises Speed 50%; locks move")
    );

    private static final Map<String, Interaction> ABILITIES = Map.of(
            "intimidate", new Interaction("intimidate", "switch-in activation", "attempts to lower foes' Attack by one stage"),
            "unaware", new Interaction("unaware", "damage calculation", "ignores relevant opposing stat changes"),
            "imposter", new Interaction("imposter", "switch-in", "transforms into target; copied moves/ability are not native"),
            "download", new Interaction("download", "switch-in", "raises Attack or Special Attack based on target defenses"),
            "supremeoverlord", new Interaction("supremeoverlord", "damage calculation", "power increases with fainted allies"),
            "fairyaura", new Interaction("fairyaura", "while active", "boosts Fairy-type move power for battlefield")
    );

    public static Interaction item(String id) { return ITEMS.get(canonical(id)); }
    public static Interaction ability(String id) { return ABILITIES.get(canonical(id)); }

    private static String canonical(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
