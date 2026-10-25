package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.21 — source-scoped, replay-custody-verified POSSIBILITIES for a future
 * Phase 6 Shadow adapter. This is OFFLINE review, not a battle-state binder.
 * Observed events != native/current build; source listing != installed legality;
 * proposed candidates != actual moves/items/abilities or calibrated weights.
 */
public final class PublicShadowBuildHandoff {
    private PublicShadowBuildHandoff() {}
    public static final int VERSION=1;
    public static final String SCHEMA="phase5.21.shadow-build-evidence.v1";

    public enum Grade {
        PUBLIC_EVENT_ONLY,
        PUBLIC_EVENT_SOURCE_ALIGNED,
        SOURCE_SUPPORTED_POSSIBILITY,
        SOURCE_LISTED_NONLEGAL_QUERY_ONLY,
        UNVERIFIED_PROPOSAL,
        SOURCE_DISAGREEMENT,
        ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS
    }
    public enum Limit {
        DISPLAY_NOT_VERIFIED_IDENTITY,
        NATIVE_BUILD_NOT_PROVEN,
        INSTALLED_REGISTRY_NOT_VERIFIED,
        ITEM_ELIGIBILITY_UNKNOWN,
        MOVE_METHOD_NOT_AVAILABLE,
        PUBLIC_EVENT_DISAGREES_WITH_SOURCE,
        FORM_INHERITANCE_OR_OVERRIDE_UNKNOWN,
        TRANSFORM_OR_DISGUISE_UNCERTAIN,
        ITEM_OR_ABILITY_TRANSITION_UNRESOLVED,
        NOT_OBSERVED_IN_THIS_APPEARANCE,
        CANDIDATE_ASSERTION_NOT_A_REAL_BUILD,
        ZERO_CALIBRATED_PROBABILITIES
    }
    public enum ConstraintVerdict {
        VERIFIED_SUBSET_OF_SAME_PUBLIC_APPEARANCE,
        NO_CONSTRAINTS_REQUESTED
    }

    /** Fixed upstream snapshot pins; this cannot mean the other phases
     * currently run together or have equivalent runtime provenance. */
    public record Pin(int version,String schema,
                      PublicCompetitiveConsumerContract.ContractPin consumer,
                      PublicCompetitiveHandoffAudit.CrossPhaseSnapshot reviewedInterfaces) {
        public Pin {
            if(version!=VERSION || !SCHEMA.equals(schema)
                    || !PublicCompetitiveConsumerContract.ContractPin.pinned().equals(consumer)
                    || !PublicCompetitiveHandoffAudit.CrossPhaseSnapshot.audited()
                            .equals(reviewedInterfaces)) throw invalid();
        }
        public static Pin pinned() {
            return new Pin(VERSION,SCHEMA,
                    PublicCompetitiveConsumerContract.ContractPin.pinned(),
                    PublicCompetitiveHandoffAudit.CrossPhaseSnapshot.audited());
        }
    }
    /**
     * This is a REDACTION/SELECTION from already signed offline observations.
     * New current battle events must come through a separately reviewed public
     * source/turn/actor adapter; they are NEVER accepted here as evidence.
     */
    public record PublicObservationConstraint(int appearanceIndex,
                                              Set<String> witnessedMoves,
                                              Set<String> revealedAbilities,
                                              Set<String> revealedItems) {
        public PublicObservationConstraint {
            if(appearanceIndex<1 || witnessedMoves==null || revealedAbilities==null
                    || revealedItems==null || witnessedMoves.size()>12
                    || revealedAbilities.size()>8 || revealedItems.size()>12)
                throw invalid();
            witnessedMoves=identifiers(witnessedMoves);
            revealedAbilities=identifiers(revealedAbilities);
            revealedItems=identifiers(revealedItems);
        }
    }
    /** Immutable, explicit grade plus provenance/uncertainty. Kind is MOVE,
     * ABILITY, ITEM or ROLE. Not a closed opponent set or native slot. */
    public record Signal(PublicOpponentBuildHypotheses.Kind kind,String id,
                         Grade grade,boolean publiclyObserved,
                         Set<String> listedLearnMethods,Set<Limit> limitations) {
        public Signal {
            if(kind==null || grade==null || id==null
                    || !(kind==PublicOpponentBuildHypotheses.Kind.ROLE
                        ?id.matches("[a-z0-9-]{1,48}"):id.matches("[a-z0-9]{1,40}"))
                    || listedLearnMethods==null || limitations==null
                    || !limitations.contains(Limit.NATIVE_BUILD_NOT_PROVEN)
                    || !limitations.contains(Limit.ZERO_CALIBRATED_PROBABILITIES)
                    || (kind!=PublicOpponentBuildHypotheses.Kind.MOVE
                        && !listedLearnMethods.isEmpty())
                    || (grade==Grade.SOURCE_SUPPORTED_POSSIBILITY && publiclyObserved)
                    || (grade==Grade.PUBLIC_EVENT_ONLY && !publiclyObserved)
                    || (grade==Grade.PUBLIC_EVENT_SOURCE_ALIGNED && !publiclyObserved))
                throw invalid();
            listedLearnMethods=Set.copyOf(listedLearnMethods);
            limitations=Set.copyOf(limitations);
        }
    }
    /** Appearance ordinal is NOT a persistent party slot or current actor. */
    public record Appearance(int index,String displayedForm,
                             List<Signal> signals,Set<Limit> limitations,
                             ConstraintVerdict constraintVerdict,
                             boolean identityUncertain) {
        public Appearance {
            if(index<1 || displayedForm==null
                    || !displayedForm.matches("[a-z0-9]{1,40}")
                    || signals==null || limitations==null || constraintVerdict==null
                    || !identityUncertain
                    || !limitations.contains(Limit.DISPLAY_NOT_VERIFIED_IDENTITY)
                    || !limitations.contains(Limit.INSTALLED_REGISTRY_NOT_VERIFIED))
                throw invalid();
            signals=List.copyOf(signals);
            limitations=Set.copyOf(limitations);
        }
    }
    /** Each candidate is caller-proposed. IDs are included for practical
     * inspection but no probability, fixed set or elimination follows. */
    public record Possibility(int ordinal,int appearanceIndex,
                              PublicCompetitiveConsumerContract.Classification verdict,
                              Set<String> proposedMoves,Set<String> proposedAbilities,
                              Set<String> proposedItems,Set<String> excludedPublicMoves,
                              Set<Limit> limitations,
                              boolean nativeBuildVerified) {
        public Possibility {
            if(ordinal<1 || appearanceIndex<1 || verdict==null
                    || proposedMoves==null || proposedAbilities==null
                    || proposedItems==null || excludedPublicMoves==null
                    || limitations==null || nativeBuildVerified
                    || !limitations.contains(Limit.CANDIDATE_ASSERTION_NOT_A_REAL_BUILD)
                    || !limitations.contains(Limit.ZERO_CALIBRATED_PROBABILITIES))
                throw invalid();
            proposedMoves=identifiers(proposedMoves);
            proposedAbilities=identifiers(proposedAbilities);
            proposedItems=identifiers(proposedItems);
            excludedPublicMoves=identifiers(excludedPublicMoves);
            limitations=Set.copyOf(limitations);
        }
    }
    /** Private constructor prevents forging a trusted-looking export. */
    public static final class Report {
        private final List<Appearance> appearances;
        private final List<Possibility> candidates;
        private final String schema, sourceRevision, mechanicsScope, policySha256;
        private Report(List<Appearance> a,List<Possibility> p) {
            appearances=List.copyOf(a);
            candidates=List.copyOf(p);
            schema=SCHEMA;
            sourceRevision=PublicSpeciesBuildLegality.SOURCE_REVISION;
            mechanicsScope=CobblemonMechanics.SOURCE;
            policySha256=PublicBuildCandidateRobustness.frozenPolicySha256();
        }
        public List<Appearance> appearances(){return appearances;}
        public List<Possibility> candidates(){return candidates;}
        public String schema(){return schema;}
        public String sourceRevision(){return sourceRevision;}
        public String mechanicsScope(){return mechanicsScope;}
        public String policySha256(){return policySha256;}
        public boolean originalReplaySignedCustodyRechecked(){return true;}
        public boolean currentBattleEvidenceBound(){return false;}
        public boolean legalActionsAvailable(){return false;}
        public boolean calibratedOpponentProbabilities(){return false;}
        public boolean speciesIdentityProven(){return false;}
        public boolean completeLegalSetProven(){return false;}
        public boolean independentlyApprovedReplayRights(){return false;}
        public boolean phase6SearchAuthorized(){return false;}
        public boolean shadowTacticalReady(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString() {
            return "Phase521OfflineShadowBuildHandoff[public-source-only;not-current-battle;not-tactical]";
        }
    }

    public static Report prepare(PublicReplayCorpus.Source source,String spectatorLog,
                                 List<PublicBuildCandidateFilter.Candidate> guesses,
                                 List<PublicObservationConstraint> constraints,
                                 PublicSpeciesBuildLegality.SourcePin sourcePin,
                                 PublicFormBuildReconciliation.ResolverPin resolverPin,
                                 Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                                 PublicSignedCustody.SignedManifest signed,
                                 PublicSignedCustody.TrustedPolicy trusted,
                                 long evaluatedAt,Pin pin) {
        if(source==null || spectatorLog==null || guesses==null || guesses.isEmpty()
                || constraints==null || sourcePin==null || resolverPin==null
                || overrides==null || signed==null || trusted==null || pin==null
                || !Pin.pinned().equals(pin)
                || !PublicSpeciesBuildLegality.SourcePin.pinned().equals(sourcePin)
                || !PublicFormBuildReconciliation.ResolverPin.pinned().equals(resolverPin)
                || guesses.size()>PublicBuildCandidateFilter.MAX_CANDIDATES)
            throw invalid();
        var byConstraint=new TreeMap<Integer,PublicObservationConstraint>();
        for(var c:constraints) {
            if(c==null || byConstraint.putIfAbsent(c.appearanceIndex(),c)!=null)
                throw invalid();
        }
        // Every useful exported ID comes from either a *publicly observed*
        // Phase 5.13 event or an explicitly caller-proposed hypothesis
        // reconciled with the source-pinned Phase 5.15 legal-query evidence.
        var queries=toQueries(guesses);
        var observed=PublicOpponentBuildHypotheses.analyze(source,spectatorLog,
                sourcePin.target(),queries,signed,trusted,evaluatedAt);
        var legality=PublicFormBuildReconciliation.reconcile(source,spectatorLog,
                queries,sourcePin,resolverPin,overrides,signed,trusted,evaluatedAt);
        var export=PublicCompetitiveConsumerContract.prepare(source,spectatorLog,
                guesses,sourcePin,resolverPin,overrides,signed,trusted,evaluatedAt,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());
        if(!export.currentSignedCustodyRevalidated()
                || export.isShadowTacticalInput() || export.installedRegistryVerified()
                || export.currentBattleBound()
                || export.appearances().size()!=observed.appearances().size()
                || observed.appearances().size()!=legality.appearances().size()
                || export.candidates().size()!=guesses.size()) throw invalid();

        var result=new ArrayList<Appearance>();
        var byIndex=new HashMap<Integer,Appearance>();
        for(int i=0;i<observed.appearances().size();i++) {
            var o=observed.appearances().get(i);
            var l=legality.appearances().get(i);
            var e=export.appearances().get(i);
            if(o.index()!=l.index() || o.index()!=e.appearanceIndex()
                    || !o.displayedSpecies().equals(l.displayedId())
                    || !o.identityUncertain() || o.identityLinked()
                    || !l.identityUncertain() || !e.identityUncertain())
                throw invalid();
            Map<PublicOpponentBuildHypotheses.Kind,Map<String,
                    PublicFormBuildReconciliation.Evidence>> listed=new EnumMap<>(
                    PublicOpponentBuildHypotheses.Kind.class);
            for(var k:PublicOpponentBuildHypotheses.Kind.values())
                listed.put(k,new HashMap<>());
            for(var x:l.evidence()) {
                if(listed.get(x.kind()).putIfAbsent(x.id(),x)!=null)throw invalid();
            }
            Map<PublicOpponentBuildHypotheses.Kind,Set<String>> witnessed=new EnumMap<>(
                    PublicOpponentBuildHypotheses.Kind.class);
            for(var k:PublicOpponentBuildHypotheses.Kind.values())
                witnessed.put(k,new TreeSet<>());

            var signals=new ArrayList<Signal>();
            for(var h:o.hypotheses()) {
                var limits=commonLimits();
                if(!h.publicEvent())limits.add(Limit.NOT_OBSERVED_IN_THIS_APPEARANCE);
                var detail=listed.get(h.kind()).get(h.id());
                Grade grade;
                Set<String> methods=Set.of();
                if(h.kind()==PublicOpponentBuildHypotheses.Kind.ROLE) {
                    grade=h.strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL
                            ?Grade.SOURCE_SUPPORTED_POSSIBILITY:Grade.UNVERIFIED_PROPOSAL;
                } else {
                    if(detail==null || detail.publiclyObserved()!=h.publicEvent())
                        throw invalid();
                    for(var method:detail.listedMethods()) {
                        // Only source method category is exposed; not a level
                        // or proof a TM is available in the installed game.
                        if(!Set.of("level","egg","tm","tutor","legacy","special","form_change","evolution")
                            .contains(method.name())) throw invalid();
                    }
                    var methodSet=new TreeSet<String>();
                    for(var method:detail.listedMethods())methodSet.add(method.name());
                    methods=Set.copyOf(methodSet);
                    switch(detail.finding()) {
                        case SOURCE_LISTED_LEGAL_QUERY_METHOD,
                             SOURCE_LISTED_NORMAL_ABILITY,
                             SOURCE_LISTED_HIDDEN_ABILITY,
                             SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY ->
                            grade=h.publicEvent()?Grade.PUBLIC_EVENT_SOURCE_ALIGNED:
                                    Grade.SOURCE_SUPPORTED_POSSIBILITY;
                        case SOURCE_LISTED_NONLEGAL_QUERY_ONLY -> {
                            grade=h.publicEvent()?Grade.PUBLIC_EVENT_ONLY:
                                    Grade.SOURCE_LISTED_NONLEGAL_QUERY_ONLY;
                            limits.add(Limit.MOVE_METHOD_NOT_AVAILABLE);
                        }
                        case PUBLIC_EVENT_SOURCE_DISAGREEMENT -> {
                            grade=Grade.SOURCE_DISAGREEMENT;
                            limits.add(Limit.PUBLIC_EVENT_DISAGREES_WITH_SOURCE);
                        }
                        case SOURCE_NOT_LISTED -> grade=h.publicEvent()?
                            Grade.SOURCE_DISAGREEMENT:Grade.UNVERIFIED_PROPOSAL;
                        case ABSTAIN_INCOMPLETE_SOURCE,ABSTAIN_REPORTED_OVERRIDE -> {
                            grade=Grade.ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS;
                            limits.add(Limit.FORM_INHERITANCE_OR_OVERRIDE_UNKNOWN);
                        }
                        case ABSTAIN_TRANSFORMED_APPEARANCE -> {
                            grade=Grade.ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS;
                            limits.add(Limit.TRANSFORM_OR_DISGUISE_UNCERTAIN);
                        }
                        case ITEM_LEGALITY_UNKNOWN -> {
                            grade=h.publicEvent()?Grade.PUBLIC_EVENT_ONLY:Grade.UNVERIFIED_PROPOSAL;
                            limits.add(Limit.ITEM_ELIGIBILITY_UNKNOWN);
                        }
                        default -> throw invalid();
                    }
                }
                if(o.transformed()) {
                    grade=Grade.ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS;
                    limits.add(Limit.TRANSFORM_OR_DISGUISE_UNCERTAIN);
                }
                if(h.kind()==PublicOpponentBuildHypotheses.Kind.ITEM)
                    limits.add(Limit.ITEM_ELIGIBILITY_UNKNOWN);
                if(h.publicEvent())witnessed.get(h.kind()).add(h.id());
                signals.add(new Signal(h.kind(),h.id(),grade,h.publicEvent(),methods,limits));
            }
            signals.sort(Comparator.comparing(Signal::kind).thenComparing(Signal::id));
            var envelope=commonLimits();
            envelope.add(Limit.DISPLAY_NOT_VERIFIED_IDENTITY);
            if(o.transformed())envelope.add(Limit.TRANSFORM_OR_DISGUISE_UNCERTAIN);
            if(!e.uncertainty().isEmpty()) {
                if(e.uncertainty().contains(
                        PublicCompetitiveConsumerContract.Uncertainty.INCOMPLETE_FORM_OR_SOURCE))
                    envelope.add(Limit.FORM_INHERITANCE_OR_OVERRIDE_UNKNOWN);
                if(e.uncertainty().contains(
                        PublicCompetitiveConsumerContract.Uncertainty.AMBIGUOUS_ITEM_OR_ABILITY_CHANGE))
                    envelope.add(Limit.ITEM_OR_ABILITY_TRANSITION_UNRESOLVED);
            }
            var c=byConstraint.remove(o.index());
            ConstraintVerdict verdict=ConstraintVerdict.NO_CONSTRAINTS_REQUESTED;
            if(c!=null) {
                if(o.transformed()
                        || e.uncertainty().contains(
                             PublicCompetitiveConsumerContract.Uncertainty.TRANSFORM_OR_FORM_CHANGE)
                        || e.uncertainty().contains(
                             PublicCompetitiveConsumerContract.Uncertainty.PUBLIC_SOURCE_DISAGREEMENT)
                        || e.uncertainty().contains(
                             PublicCompetitiveConsumerContract.Uncertainty.AMBIGUOUS_ITEM_OR_ABILITY_CHANGE)
                        || !witnessed.get(PublicOpponentBuildHypotheses.Kind.MOVE)
                            .containsAll(c.witnessedMoves())
                        || !witnessed.get(PublicOpponentBuildHypotheses.Kind.ABILITY)
                            .containsAll(c.revealedAbilities())
                        || !witnessed.get(PublicOpponentBuildHypotheses.Kind.ITEM)
                            .containsAll(c.revealedItems()))
                    throw invalid();
                verdict=ConstraintVerdict.VERIFIED_SUBSET_OF_SAME_PUBLIC_APPEARANCE;
            }
            var appearance=new Appearance(o.index(),o.displayedSpecies(),signals,envelope,
                    verdict,true);
            if(byIndex.putIfAbsent(o.index(),appearance)!=null) throw invalid();
            result.add(appearance);
        }
        if(!byConstraint.isEmpty())throw invalid();

        var possibilities=new ArrayList<Possibility>();
        for(int i=0;i<guesses.size();i++) {
            var c=guesses.get(i);
            var verdict=export.candidates().get(i);
            var a=byIndex.get(c.appearanceIndex());
            if(a==null || verdict.ordinal()!=i+1
                    || verdict.appearanceIndex()!=c.appearanceIndex()
                    || verdict.actualOpponentEliminationAllowed())throw invalid();
            var limits=commonLimits();
            limits.add(Limit.CANDIDATE_ASSERTION_NOT_A_REAL_BUILD);
            limits.add(Limit.DISPLAY_NOT_VERIFIED_IDENTITY);
            if(!c.possibleItems().isEmpty())limits.add(Limit.ITEM_ELIGIBILITY_UNKNOWN);
            if(verdict.classification()!=
                    PublicCompetitiveConsumerContract.Classification.SOURCE_CONDITIONAL_ONLY)
                limits.add(Limit.FORM_INHERITANCE_OR_OVERRIDE_UNKNOWN);
            possibilities.add(new Possibility(i+1,c.appearanceIndex(),verdict.classification(),
                    c.requiredMoves(),c.possibleAbilities(),c.possibleItems(),
                    c.excludedPublicMoves(),limits,false));
        }
        return new Report(result,possibilities);
    }

    private static List<PublicOpponentBuildHypotheses.CandidateQuery> toQueries(
            List<PublicBuildCandidateFilter.Candidate> proposed) {
        var union=new TreeMap<Integer,Map<PublicOpponentBuildHypotheses.Kind,Set<String>>>();
        for(var c:proposed) {
            if(c==null)throw invalid();
            var map=union.computeIfAbsent(c.appearanceIndex(),ignored->{
                var m=new EnumMap<PublicOpponentBuildHypotheses.Kind,Set<String>>(
                        PublicOpponentBuildHypotheses.Kind.class);
                m.put(PublicOpponentBuildHypotheses.Kind.MOVE,new TreeSet<>());
                m.put(PublicOpponentBuildHypotheses.Kind.ABILITY,new TreeSet<>());
                m.put(PublicOpponentBuildHypotheses.Kind.ITEM,new TreeSet<>());
                return m;
            });
            map.get(PublicOpponentBuildHypotheses.Kind.MOVE).addAll(c.requiredMoves());
            map.get(PublicOpponentBuildHypotheses.Kind.ABILITY).addAll(c.possibleAbilities());
            map.get(PublicOpponentBuildHypotheses.Kind.ITEM).addAll(c.possibleItems());
        }
        var result=new ArrayList<PublicOpponentBuildHypotheses.CandidateQuery>();
        for(var e:union.entrySet()){
            var m=e.getValue();
            result.add(new PublicOpponentBuildHypotheses.CandidateQuery(e.getKey(),
                    m.get(PublicOpponentBuildHypotheses.Kind.MOVE),
                    m.get(PublicOpponentBuildHypotheses.Kind.ABILITY),
                    m.get(PublicOpponentBuildHypotheses.Kind.ITEM)));
        }
        return result;
    }
    private static EnumSet<Limit> commonLimits() {
        return EnumSet.of(Limit.NATIVE_BUILD_NOT_PROVEN,
                Limit.INSTALLED_REGISTRY_NOT_VERIFIED,
                Limit.ZERO_CALIBRATED_PROBABILITIES);
    }
    private static Set<String> identifiers(Set<String> raw){
        var out=new TreeSet<String>();
        for(String id:raw)
            if(id==null || !id.matches("[a-z0-9]{1,40}") || id.equals("unknown"))
                throw invalid();
            else out.add(id);
        return Collections.unmodifiableSet(out);
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 5.21 requires signed, appearance-scoped offline public evidence");
    }
}
