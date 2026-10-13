package com.maestroai.training;

import java.util.List;
import java.util.Map;

/**
 * Fail-closed boundary between allowlisted public Showdown events and the
 * opponent side of a Shadow-only BattleSnapshot.
 *
 * Never accept an internal Cobblemon Pokemon/BattlePokemon here. Public
 * appearance is not proof of true species under Illusion, nor proof of
 * current types after Transform or other type-changing mechanics.
 */
public final class PublicShadowSnapshot {
    private PublicShadowSnapshot() {}

    /**
     * A missing or incomplete public slot is UNKNOWN, not an invitation to
     * consult internal opponent state. Exact opponent HP, typing and bench
     * membership are not represented until a public provenance path exists.
     */
    public static BattleSnapshot.Combatant opponent(PublicBattleObserver observer, String slot) {
        String identity = observer == null || slot == null ? null : observer.activeIdentity(slot);
        String appearedSpecies = identity == null ? null : observer.publicSpecies(slot);
        if (appearedSpecies == null || appearedSpecies.isBlank()) {
            return unknownOpponent();
        }
        boolean transformed = observer.transformed(identity);
        return new BattleSnapshot.Combatant(
                appearedSpecies,
                BattleSnapshot.UNKNOWN_HP_FRACTION,
                List.of(), // No internal types, including unannounced Transform/type changes.
                observer.knownStages(identity),
                transformed ? List.of() : observer.revealedMoves(identity),
                observer.currentItem(identity),
                transformed ? null : observer.revealedAbility(identity),
                false // No faint claim without an explicit public HP/faint observation.
        );
    }

    public static String publicTeraType(PublicBattleObserver observer, String slot) {
        return observer == null || slot == null ? null : observer.publicTeraType(slot);
    }

    private static BattleSnapshot.Combatant unknownOpponent() {
        return new BattleSnapshot.Combatant(
                "UNKNOWN", BattleSnapshot.UNKNOWN_HP_FRACTION, List.of(),
                Map.of(), List.of(), null, null, false);
    }
}
