package com.maestroai.training;

import java.util.*;

/**
 * Version-scoped, offline interpretation of Phase 5.1's de-identified spectator
 * observations. No opponent lookup, runtime promotion or hidden set completion.
 */
public final class CompetitiveFormatNormalizer {
    private CompetitiveFormatNormalizer() {}

    public enum Status { PINNED_SOURCE_VERIFIED, UNKNOWN, OUTSIDE_VERIFIED_SCOPE }

    /** A version assertion is NOT a live registry attestation. */
    public record Target(String minecraft, String cobblemon, String build, String simulator) {
        public Target {
            if (minecraft == null || cobblemon == null || build == null || simulator == null)
                throw rejected();
        }
        public static Target pinnedSource() {
            return new Target("1.21.1", "1.7.3", "7553235", CobblemonMechanics.SOURCE);
        }
        boolean pinned() { return equals(pinnedSource()); }
    }

    /** id is a canonical Pokémon Showdown/Maestro ID, never a nickname. */
    public record Token(String id, boolean publiclyObserved, Status sourcePresence,
                        Status conditionalMechanics) {
        public Token {
            Objects.requireNonNull(sourcePresence);
            Objects.requireNonNull(conditionalMechanics);
            if (id == null || !id.matches("[a-z0-9]{1,40}")) throw rejected();
        }
    }

    /**
     * A base-species relation is taxonomy, not a confirmation of the Pokémon's
     * real identity, a form's mechanics or a shared party member across entries.
     */
    public record Appearance(int index, String slot, Token displayed, Token baseSpecies,
                             boolean distinctForm, Map<String,Token> observedMoves,
                             Token revealedAbility, Token revealedItem,
                             boolean transformed, boolean identityLinked,
                             boolean identityUncertain) {
        public Appearance { observedMoves=Collections.unmodifiableMap(new TreeMap<>(observedMoves)); }
    }
    /** Rules beyond the observed generation/game type/tier are unverified. */
    public record Rules(int generation, String gameType, String showdownTier,
                        boolean completeShowdownRulesKnown, Status cobblemonTierTransfer) {}
    /** The upstream rights attestation is not independently revalidated here. */
    public record Provenance(String replayId, String inputSha256, int publicEvents,
                             int turns, String rightsVerification) {}
    public record Result(Provenance provenance, Rules rules, String targetSourceScope,
                         boolean minecraftRuntimeAttested, List<Appearance> appearances) {
        public Result { appearances=List.copyOf(appearances); }
    }

    // Fixed, explicit names only: do not heuristically strip unknown form suffixes.
    private static final Map<String,String> FORM_BASE=Map.of(
            "calyrexshadow","calyrex",
            "mewtwomegay","mewtwo",
            "mewtwomegax","mewtwo",
            "zoroarkhisui","zoroark");
    private static final Map<String,String> FORMATS=Map.of(
            "gen9ou","ou","gen9ubers","ubers");

    public static Result normalize(PublicReplayCorpus.Evidence evidence, Target target) {
        if (evidence==null || target==null || evidence.format()==null
                || !FORMATS.containsKey(evidence.format())
                || evidence.replayId()==null
                || !evidence.replayId().matches("gen9(?:ou|ubers)-[1-9][0-9]{3,17}")
                || !evidence.replayId().startsWith(evidence.format()+"-")
                || evidence.inputSha256()==null
                || !evidence.inputSha256().matches("[a-f0-9]{64}")
                || evidence.publicEventCount()<1 || evidence.turns()<1
                || !"SHOWDOWN_PUBLIC_OBSERVATIONS;COBBLEMON_TRANSFER_UNVERIFIED".equals(evidence.compatibility())
                || evidence.appearances()==null)
            throw rejected();

        boolean pinned=target.pinned();
        List<Appearance> normalized=new ArrayList<>();
        int expectedIndex=1;
        for (var input:evidence.appearances()) {
            if (input==null || input.index()!=expectedIndex++
                    || !Set.of("p1a","p2a").contains(input.slot())
                    || input.identityLinked() || !input.identityUncertain()
                    || input.nativeMoves()==null) throw rejected();
            String species=id(input.displayedSpecies());
            String base=FORM_BASE.getOrDefault(species,species);
            boolean distinct=!base.equals(species);
            // Exact-form catalog checks; never inherit base-species support.
            Token display=species(species,pinned);
            Token baseToken=species(base,pinned);
            var moves=new TreeMap<String,Token>();
            for (String raw:input.nativeMoves()) {
                Token move=move(raw,pinned);
                // Unknown/malformed values cannot inject a person-name or set text.
                moves.put(move.id(),move);
            }
            normalized.add(new Appearance(input.index(),input.slot(),display,baseToken,
                    distinct,moves,itemOrAbility(input.revealedAbility(),pinned),
                    itemOrAbility(input.revealedItem(),pinned),input.transformed(),
                    false,true));
        }
        return new Result(new Provenance(evidence.replayId(),evidence.inputSha256(),
                evidence.publicEventCount(),evidence.turns(),
                "UPSTREAM_OPERATOR_ATTESTATION_ONLY_NOT_INDEPENDENTLY_VERIFIED"),
                // The replay format is observed; live Showdown clauses, bans, and
                // equivalence to an RCT/Cobblemon match are all UNKNOWN.
                new Rules(9,"singles",FORMATS.get(evidence.format()),false,Status.UNKNOWN),
                pinned ? CobblemonMechanics.SOURCE : "OUTSIDE_PINNED_COBBLEMON_1_7_3_BUILD_7553235",
                false,normalized);
    }

    private static Token species(String key, boolean pinned) {
        Status status=key.equals("unknown")?Status.UNKNOWN:
                !pinned?Status.OUTSIDE_VERIFIED_SCOPE:
                PublicSpeciesCatalog.ids().contains(key)?Status.PINNED_SOURCE_VERIFIED:Status.UNKNOWN;
        // Source-backed base-stats/catalog presence is not current battle/form mechanics.
        return new Token(key,!key.equals("unknown"),status,Status.UNKNOWN);
    }
    private static Token move(String raw,boolean pinned) {
        String key=id(raw);
        CobblemonMechanics.Move definition=pinned?CobblemonMechanics.move(key):null;
        Status presence=key.equals("unknown")?Status.UNKNOWN:
                !pinned?Status.OUTSIDE_VERIFIED_SCOPE:
                definition==null?Status.UNKNOWN:Status.PINNED_SOURCE_VERIFIED;
        // This narrowly identifies source-verified ordinary single-hit behavior.
        // It cannot certify field/ability/item/status effects, outcomes or runtime.
        Status mechanics=definition!=null && definition.damageOnly()
                && definition.power()>0?Status.PINNED_SOURCE_VERIFIED:Status.UNKNOWN;
        return new Token(key,true,presence,mechanics);
    }
    private static Token itemOrAbility(String raw,boolean pinned) {
        String key=id(raw);
        // No source/registry attestation for item or ability semantics here.
        Status presence=key.equals("unknown")?Status.UNKNOWN:
                pinned?Status.UNKNOWN:Status.OUTSIDE_VERIFIED_SCOPE;
        return new Token(key,!key.equals("unknown"),presence,Status.UNKNOWN);
    }
    private static String id(String raw) {
        return raw!=null && raw.matches("[a-z0-9]{1,40}")?raw:"unknown";
    }
    private static IllegalArgumentException rejected() {
        // Do not echo malformed/possibly private content into error messages.
        return new IllegalArgumentException("Invalid public replay normalization scope");
    }
}
