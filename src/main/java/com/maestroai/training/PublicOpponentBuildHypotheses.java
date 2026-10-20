package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.13 — offline appearance-scoped opponent build evidence.
 * No current-battle binding, no hidden sets, no opponent identity joins,
 * no species learnset/ability/item legality assertion, and NO probabilities.
 *
 * Original replay + CURRENT Ed25519 custody are always recomputed before
 * processing caller-suggested candidates, which remain untrusted inputs.
 */
public final class PublicOpponentBuildHypotheses {
    private PublicOpponentBuildHypotheses() {}

    public enum Kind { MOVE, ABILITY, ITEM, ROLE }
    public enum Strength {
        PUBLICLY_OBSERVED_EVENT,
        SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN,
        SOURCE_ROLE_SIGNAL_CONDITIONAL,
        UNKNOWN_UNVERIFIED
    }
    /** Public event is not proof the item/ability was a native build slot.
     * Conditional source coverage is not a verified species learnset.
     */
    public record Hypothesis(Kind kind,String id,Strength strength,
                             boolean publicEvent,boolean currentBuildConfirmed,
                             boolean sourceEffectSupported,boolean speciesLegalityVerified) {
        public Hypothesis {
            if (kind==null || strength==null || id==null
                    || !id.matches("[a-z0-9]{1,40}")
                    || currentBuildConfirmed || speciesLegalityVerified
                    || (strength==Strength.PUBLICLY_OBSERVED_EVENT && !publicEvent)
                    || (strength==Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN
                        && (kind!=Kind.MOVE || publicEvent || !sourceEffectSupported))
                    || (strength==Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL
                        && (kind!=Kind.ROLE || publicEvent || !sourceEffectSupported))
                    || (strength==Strength.UNKNOWN_UNVERIFIED
                        && (publicEvent || sourceEffectSupported))
                    || (kind!=Kind.MOVE && kind!=Kind.ROLE && sourceEffectSupported))
                throw invalid();
        }
    }
    /**
     * Guessed candidates are NEVER accepted as observations or capability
     * attestations. They are scoped to one chronological appearance index.
     * Wrong slot/species/entry IDs fail rather than silently cross-link.
     */
    public record CandidateQuery(int appearanceIndex,Set<String> moves,
                                 Set<String> abilities,Set<String> items) {
        public CandidateQuery {
            if (appearanceIndex<1 || moves==null || abilities==null || items==null
                    || moves.size()>12 || abilities.size()>8 || items.size()>12)
                throw invalid();
            moves=canonical(moves); abilities=canonical(abilities); items=canonical(items);
        }
    }
    /** One chronological opponent appearance, never a deduced party member. */
    public record Appearance(int index,String displayedSpecies,
                             boolean distinctForm,boolean transformed,
                             boolean identityUncertain,boolean identityLinked,
                             boolean sourceSpeciesPresent,boolean learnsetVerified,
                             List<Hypothesis> hypotheses,
                             Set<Kind> fieldsWithNoObservation) {
        public Appearance {
            if (index<1 || displayedSpecies==null
                    || !displayedSpecies.matches("[a-z0-9]{1,40}")
                    || identityLinked || !identityUncertain || learnsetVerified
                    || hypotheses==null || fieldsWithNoObservation==null) throw invalid();
            hypotheses=List.copyOf(hypotheses);
            fieldsWithNoObservation=Set.copyOf(fieldsWithNoObservation);
        }
    }
    /** Private constructor: users cannot mint trusted-looking build reports. */
    public static final class Report {
        private final List<Appearance> appearances;
        private final String format,sourceScope;
        private final boolean rightsIndependentlyVerified;
        private Report(List<Appearance> appearances,String format,String sourceScope) {
            this.appearances=List.copyOf(appearances);
            this.format=format;
            this.sourceScope=sourceScope;
            this.rightsIndependentlyVerified=false;
        }
        public List<Appearance> appearances() { return appearances; }
        public String format() { return format; }
        public String sourceScope() { return sourceScope; }
        public boolean independentRightsVerified() { return rightsIndependentlyVerified; }
        public boolean allSpeciesLearnsetsVerified() { return false; }
        public boolean currentOpponentIdentityProven() { return false; }
        public boolean calibratedProbabilitiesProduced() { return false; }
        public boolean shadowTacticalReady() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "OfflineBuildHypotheses[appearance-local;not-a-legal-set;no-probabilities]";
        }
    }

    public static Report analyze(PublicReplayCorpus.Source source,String spectatorLog,
                                 CompetitiveFormatNormalizer.Target target,
                                 List<CandidateQuery> proposed,
                                 PublicSignedCustody.SignedManifest signed,
                                 PublicSignedCustody.TrustedPolicy trustedKeys,
                                 long evaluatedAtEpochSecond) {
        if (source==null || spectatorLog==null || target==null || proposed==null
                || signed==null || trustedKeys==null || !target.pinned()
                || proposed.size()>1000) throw invalid();
        // Validate source admission, exact hash/format, source permissions and
        // current signed custody BEFORE any proposed hypothesis is read.
        var custody=PublicReplayProvenance.ingestWithSignedCustody(source,spectatorLog,
                target,signed,trustedKeys,evaluatedAtEpochSecond);
        if (!custody.signedCustodyCurrentlyEligible(
                signed,trustedKeys,evaluatedAtEpochSecond)
                || custody.shadowAudit().shadowTacticalReady()) throw invalid();

        var evidence=PublicReplayCorpus.ingest(source,spectatorLog);
        var normalized=CompetitiveFormatNormalizer.normalize(evidence,target);
        var tendencies=PublicCompetitiveTendencies.analyze(source,spectatorLog,target);
        var shape=PublicCompetitiveShadowContract.audit(normalized,tendencies);
        if (!shape.sameUpstreamEvidence() || !shape.offlineSchemaCompatible()
                || normalized.minecraftRuntimeAttested()
                || !CobblemonMechanics.SOURCE.equals(normalized.targetSourceScope()))
            throw invalid();

        var byAppearance=new TreeMap<Integer,CandidateQuery>();
        for (CandidateQuery query:proposed) {
            if (query==null || byAppearance.putIfAbsent(query.appearanceIndex(),query)!=null)
                throw invalid();
        }
        var results=new ArrayList<Appearance>();
        for (var a:normalized.appearances()) {
            if (!"p2a".equals(a.slot())) {
                if (byAppearance.containsKey(a.index())) throw invalid();
                continue;
            }
            CandidateQuery query=byAppearance.remove(a.index());
            Map<Kind,Set<String>> candidates=new EnumMap<>(Kind.class);
            candidates.put(Kind.MOVE,query==null?Set.of():query.moves());
            candidates.put(Kind.ABILITY,query==null?Set.of():query.abilities());
            candidates.put(Kind.ITEM,query==null?Set.of():query.items());

            var hypotheses=new ArrayList<Hypothesis>();
            var observed=new EnumMap<Kind,Set<String>>(Kind.class);
            for (Kind kind:List.of(Kind.MOVE,Kind.ABILITY,Kind.ITEM))
                observed.put(kind,new TreeSet<>());
            for (var t:a.observedMoves().values())
                if (!"unknown".equals(t.id())) {
                    observed.get(Kind.MOVE).add(t.id());
                    hypotheses.add(new Hypothesis(Kind.MOVE,t.id(),
                            Strength.PUBLICLY_OBSERVED_EVENT,true,false,
                            t.conditionalMechanics()
                                    ==CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED,
                            false));
                }
            if (!a.transformed()) {
                addReveal(a.revealedAbility(),Kind.ABILITY,observed,hypotheses);
                addReveal(a.revealedItem(),Kind.ITEM,observed,hypotheses);
            }
            // A move existing in the frozen simulator is a conditional
            // candidate only. It is NOT proof this species can legally learn it.
            // Item and ability catalogs have no verified eligibility receipts.
            for (Kind kind:List.of(Kind.MOVE,Kind.ABILITY,Kind.ITEM))
                for (String proposedId:candidates.get(kind)) {
                    if (observed.get(kind).contains(proposedId)) continue;
                    boolean globallyKnownMove=kind==Kind.MOVE
                            && CobblemonMechanics.move(proposedId)!=null;
                    if (a.transformed()) globallyKnownMove=false;
                    hypotheses.add(new Hypothesis(kind,proposedId,
                            globallyKnownMove
                                    ?Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN
                                    :Strength.UNKNOWN_UNVERIFIED,
                            false,false,globallyKnownMove,false));
                }
            // A strategic role is at most a conditional interpretation of a
            // WITNESSED move with boosts/priority in the pinned source slice.
            // Never infer an entire moveset, item, speed EVs or future intent.
            if (!a.transformed()) {
                var roles=new TreeSet<String>();
                for (var observedMove:a.observedMoves().values()) {
                    if (!observedMove.publiclyObserved()
                            || observedMove.sourcePresence()
                                !=CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED)
                        continue;
                    var move=CobblemonMechanics.move(observedMove.id());
                    if (move==null) continue;
                    if (move.priority()>0) roles.add("priority-move-signal");
                    if (move.boosts().getOrDefault("atk",0)>0)
                        roles.add("attack-boost-signal");
                    if (move.boosts().getOrDefault("spa",0)>0)
                        roles.add("special-attack-boost-signal");
                }
                for (String role:roles)
                    hypotheses.add(new Hypothesis(Kind.ROLE,role,
                            Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL,false,false,true,false));
            }
            hypotheses.sort(Comparator.comparing(Hypothesis::kind)
                    .thenComparing(Hypothesis::id)
                    .thenComparing(Hypothesis::strength));

            EnumSet<Kind> absent=EnumSet.noneOf(Kind.class);
            for(Kind kind:List.of(Kind.MOVE,Kind.ABILITY,Kind.ITEM))
                if(observed.get(kind).isEmpty()) absent.add(kind);
            if(hypotheses.stream().noneMatch(h->h.kind()==Kind.ROLE)) absent.add(Kind.ROLE);
            results.add(new Appearance(a.index(),a.displayed().id(),a.distinctForm(),
                    a.transformed(),true,false,
                    a.displayed().sourcePresence()
                            ==CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED,
                    false,hypotheses,absent));
        }
        if(!byAppearance.isEmpty()) throw invalid();
        return new Report(results,evidence.format(),normalized.targetSourceScope());
    }

    private static void addReveal(CompetitiveFormatNormalizer.Token token,Kind kind,
                                  Map<Kind,Set<String>> observed,List<Hypothesis> out) {
        if (token!=null && token.publiclyObserved() && !"unknown".equals(token.id())) {
            observed.get(kind).add(token.id());
            out.add(new Hypothesis(kind,token.id(),Strength.PUBLICLY_OBSERVED_EVENT,
                    true,false,false,false));
        }
    }
    private static Set<String> canonical(Set<String> raw) {
        var ids=new TreeSet<String>();
        for(String id:raw)
            if(id==null || !id.matches("[a-z0-9]{1,40}") || id.equals("unknown"))
                throw invalid();
            else ids.add(id);
        return Collections.unmodifiableSet(ids);
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unsupported offline opponent build evidence");
    }
}
