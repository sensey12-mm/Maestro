package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** The bridge supplies ONLY its own actor's request DTO. No opponent-request API.
 * Cobblemon 1.7.3 drops request.stats and request.item during deserialization.
 * Request.types is transported raw typing, not Tera-aware getTypes() output.
 * These facts cannot certify effective stats, item synchronization or pack hooks.
 */
public final class OwnRequestEvidence {
    private OwnRequestEvidence() {}
    public static final String SOURCE = "cobblemon-7553235:own-ShowdownActionRequest-v1";
    public record Sample(UUID uuid, String ident, String condition, boolean active, String ability, List<String> types) {
        public Sample { types = types == null ? List.of() : List.copyOf(types); }
    }
    public record Bound(Profile profile, Known<String> ability, Known<String> item, Known<String> status, Set<String> unknown) {
        public Bound { unknown = Set.copyOf(unknown); }
    }
    public static Bound bind(String role, UUID ownUuid, String ownSide, boolean reserve, List<Sample> ownRequest) {
        if (role.equals("opponent")) throw new IllegalArgumentException("own request cannot bind opponent");
        var missing = new TreeSet<String>(Set.of("OWN_STATS_NOT_EXPOSED_BY_173_DTO", "OWN_ITEM_SYNCHRONIZATION_UNPROVEN",
                "OWN_TYPES_REQUIRE_EFFECT_SCOPE", "OWN_REQUEST_TURN_UNATTESTED"));
        if (ownUuid == null || !Set.of("p1", "p2").contains(ownSide == null ? "" : ownSide) || ownRequest == null)
            return unknown(role, missing, "OWN_REQUEST_MISSING");
        var matched = ownRequest.stream().filter(s -> s != null && ownUuid.equals(s.uuid())).toList();
        if (matched.size() != 1) return unknown(role, missing, "OWN_REQUEST_IDENTITY_AMBIGUOUS");
        Sample s = matched.get(0);
        if (s.ident() == null || !s.ident().startsWith(ownSide + ": ") || s.active() == reserve)
            return unknown(role, missing, "OWN_REQUEST_SIDE_OR_ACTIVE_MISMATCH");
        Known<IntRange> hp = Known.unknown(); Known<String> status = Known.unknown();
        if (s.condition() != null && s.condition().matches("[0-9]{1,5}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?")) {
            String[] parts = s.condition().split(" "), ratio = parts[0].split("/");
            int current = Integer.parseInt(ratio[0]), max = Integer.parseInt(ratio[1]);
            if (current <= max && max <= 10000) {
                hp = Known.of(IntRange.exact(max), SOURCE + ":own-simulator-max-HP");
                status = Known.of(parts.length == 1 ? "none" : parts[1], SOURCE + ":own-simulator-status");
            }
        }
        if (!hp.known()) missing.add("OWN_REQUEST_HP_OR_STATUS_UNSUPPORTED");
        Known<String> ability = s.ability() != null && s.ability().matches("[a-zA-Z][a-zA-Z0-9 _-]{0,63}")
                ? Known.of(s.ability().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""), SOURCE + ":current-ability") : Known.unknown();
        if (!ability.known()) missing.add("OWN_REQUEST_ABILITY_UNKNOWN");
        Known<List<String>> types = Known.unknown();
        try {
            var normalized = s.types().stream().map(t -> t.toLowerCase(Locale.ROOT)).toList();
            new Profile(role, Known.unknown(), hp, Map.of(), Known.of(normalized, SOURCE), Known.unknown());
            types = Known.of(normalized, SOURCE + ":raw-transported-types-conditional-on-no-overrides");
        } catch (IllegalArgumentException invalid) { missing.add("OWN_REQUEST_TYPES_UNKNOWN"); }
        return new Bound(new Profile(role, Known.unknown(), hp, Map.of(), types, Known.unknown()),
                ability, Known.unknown(), status, missing);
    }
    private static Bound unknown(String role, Set<String> reasons, String reason) {
        reasons.add(reason); return new Bound(Profile.unknown(role), Known.unknown(), Known.unknown(), Known.unknown(), reasons);
    }
}
