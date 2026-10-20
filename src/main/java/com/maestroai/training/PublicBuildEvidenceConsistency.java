package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.16 — deterministic, privacy-minimized build-evidence consistency.
 *
 * Only reconstructed, signed-custody-rechecked Phases 5.13 and 5.15 reports
 * can reach this audit. Observational disagreements are NOT illegality,
 * unsupported candidates are NOT contradictions, and an appearance is NEVER
 * a verified unique opposing party member or complete current set.
 */
public final class PublicBuildEvidenceConsistency {
    private PublicBuildEvidenceConsistency() {}
    public static final int VERSION=1;
    public static final String SOURCE_REVISION=PublicSpeciesBuildLegality.SOURCE_REVISION;

    public enum Field { MOVE, ABILITY, ITEM, FORM, ROLE }
    public enum Issue {
        OBSERVED_SOURCE_DISAGREEMENT,
        SOURCE_QUERY_NONLEGAL_ONLY,
        UNOBSERVED_CANDIDATE_NOT_LISTED,
        UNRESOLVED_SOURCE_OR_OVERRIDE,
        TRANSFORM_CANNOT_VERIFY_NATIVE_BUILD,
        ITEM_ELIGIBILITY_UNVERIFIED,
        CONDITIONAL_ROLE_ONLY,
        DISPLAYED_FORM_NOT_SOURCE_SUPPORTED,
        DISPLAYED_IDENTITY_NOT_PROVEN,
        MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET,
        MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED,
        MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED,
        PUBLIC_FORM_DISPLAY_TRANSITION_UNRESOLVED
    }
    public enum Gate {
        SOURCE_OBSERVATIONS_CONSISTENT_ONLY,
        ABSTAIN_SOURCE_DISAGREEMENT,
        ABSTAIN_INCOMPLETE_COVERAGE,
        ABSTAIN_TRANSFORM_OR_FORM,
        ABSTAIN_IDENTITY_UNCERTAIN
    }
    public record FieldSummary(Field field,int observed,int proposed,
                               int sourceListed,int sourceUnsupported,
                               int conflicts,int unresolved,Set<Issue> issues) {
        public FieldSummary {
            if(field==null || observed<0 || proposed<0 || sourceListed<0
                    || sourceUnsupported<0 || conflicts<0 || unresolved<0
                    || observed>proposed || sourceListed+sourceUnsupported
                       +unresolved+conflicts>proposed
                    || issues==null) throw rejected();
            issues=Set.copyOf(issues);
        }
    }
    /** One slot-entry, not a deduced species identity or stable party index. */
    public record Appearance(int index,boolean transformed,boolean identityUncertain,
                             boolean exactDisplayedSourceForm,
                             Map<Field,FieldSummary> fields,
                             Set<Gate> gates) {
        public Appearance {
            if(index<1 || !identityUncertain || fields==null || gates==null
                    || !fields.keySet().equals(EnumSet.allOf(Field.class))
                    || gates.isEmpty()) throw rejected();
            fields=Map.copyOf(fields);gates=Set.copyOf(gates);
        }
    }
    /** Strict fixed aggregate; never stores replay IDs/names, item/move IDs,
     * signatures, or synthetic player identifiers. */
    public record FieldTotal(Field field,int observed,int proposed,int listed,
                             int unsupported,int conflicts,int unresolved,
                             int appearancesWithNoObservation) {
        public FieldTotal {
            if(field==null || observed<0 || proposed<0 || listed<0
                    || unsupported<0 || conflicts<0 || unresolved<0
                    || appearancesWithNoObservation<0
                    || observed>proposed) throw rejected();
        }
    }
    public static final class Report {
        private final List<Appearance> appearances;
        private final Map<Field,FieldTotal> totals;
        private final String versionScope;
        private final int sourceDisagreementAppearances,abstainingAppearances;
        private Report(List<Appearance> a,Map<Field,FieldTotal> totals,
                       int conflicts,int abstentions) {
            if(a==null || totals==null || !totals.keySet().equals(EnumSet.allOf(Field.class))
                    || conflicts<0 || abstentions<0 || conflicts>a.size()
                    || abstentions>a.size()) throw rejected();
            this.appearances=List.copyOf(a);
            this.totals=Map.copyOf(totals);
            this.versionScope="Cobblemon-1.7.3-source-"+SOURCE_REVISION;
            sourceDisagreementAppearances=conflicts;
            abstainingAppearances=abstentions;
        }
        public List<Appearance> appearances(){return appearances;}
        public Map<Field,FieldTotal> totals(){return totals;}
        public String versionScope(){return versionScope;}
        public int sourceDisagreementAppearances(){return sourceDisagreementAppearances;}
        public int abstainingAppearances(){return abstainingAppearances;}
        public boolean everyOpponentIdentityUncertain(){return true;}
        public boolean installedGameLegalityVerified(){return false;}
        public boolean itemLegalityVerified(){return false;}
        public boolean probabilitiesCalibrated(){return false;}
        public boolean completeOpponentBuildVerified(){return false;}
        public boolean shadowTacticalReady(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "OfflineBuildConsistency[source-only;incomplete-identity;no-live]";
        }
    }

    public static Report audit(PublicReplayCorpus.Source source,String spectatorLog,
                               List<PublicOpponentBuildHypotheses.CandidateQuery> proposed,
                               PublicSpeciesBuildLegality.SourcePin sourcePin,
                               PublicFormBuildReconciliation.ResolverPin resolverPin,
                               Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
                               PublicSignedCustody.SignedManifest signed,
                               PublicSignedCustody.TrustedPolicy trusted,
                               long evaluatedAtEpochSecond) {
        if(sourcePin==null || resolverPin==null || overrides==null || proposed==null
                || signed==null || trusted==null)throw rejected();
        // Recompute ALL reports from original logs and a fresh signed custody
        // check. No publicly constructible upstream evidence records accepted.
        var observed=PublicOpponentBuildHypotheses.analyze(source,spectatorLog,
                sourcePin.target(),proposed,signed,trusted,evaluatedAtEpochSecond);
        var reconciled=PublicFormBuildReconciliation.reconcile(source,spectatorLog,
                proposed,sourcePin,resolverPin,overrides,signed,trusted,evaluatedAtEpochSecond);
        if(!CobblemonMechanics.SOURCE.equals(observed.sourceScope())
                || observed.currentOpponentIdentityProven()
                || reconciled.installedModpackCompatibilityVerified()
                || observed.appearances().size()!=reconciled.appearances().size())
            throw rejected();

        var traces=publicTransitions(spectatorLog);
        var result=new ArrayList<Appearance>();
        var running=new EnumMap<Field,int[]>(Field.class);
        for(Field field:Field.values())running.put(field,new int[7]);
        int conflicts=0,abstentions=0;
        for(int i=0;i<observed.appearances().size();i++){
            var a=observed.appearances().get(i);
            var b=reconciled.appearances().get(i);
            if(a.index()!=b.index() || !a.displayedSpecies().equals(b.displayedId())
                    || a.transformed()!=b.transformed() || !a.identityUncertain()
                    || a.identityLinked() || !b.identityUncertain())
                throw rejected();

            // Form is DISPLAY evidence only; source match does not reveal a
            // hidden original identity (e.g. Illusion). It cannot be promoted.
            var fields=new EnumMap<Field,FieldSummary>(Field.class);
            var observedByKind=new EnumMap<PublicOpponentBuildHypotheses.Kind,
                    Map<String,PublicOpponentBuildHypotheses.Hypothesis>>(
                    PublicOpponentBuildHypotheses.Kind.class);
            for(var kind:PublicOpponentBuildHypotheses.Kind.values())
                observedByKind.put(kind,new TreeMap<>());
            for(var h:a.hypotheses()) {
                if(observedByKind.get(h.kind()).putIfAbsent(h.id(),h)!=null)
                    throw rejected();
            }
            var matched=new EnumMap<PublicOpponentBuildHypotheses.Kind,Set<String>>(
                    PublicOpponentBuildHypotheses.Kind.class);
            for(var kind:PublicOpponentBuildHypotheses.Kind.values())
                matched.put(kind,new TreeSet<>());

            for(Field field:List.of(Field.MOVE,Field.ABILITY,Field.ITEM)) {
                var kind=asKind(field);
                var hits=new TreeSet<String>();
                int nObserved=0,nListed=0,nUnsupported=0,nConflict=0,nUnresolved=0;
                EnumSet<Issue> issues=EnumSet.noneOf(Issue.class);
                for(var ev:b.evidence()) {
                    if(ev.kind()!=kind)continue;
                    if(!hits.add(ev.id()))throw rejected();
                    var earlier=observedByKind.get(kind).get(ev.id());
                    if(earlier==null || earlier.publicEvent()!=ev.publiclyObserved()
                            || ev.appearanceIndex()!=a.index()
                            || ev.fullInstalledRegistryAttested() || ev.nativeBuildConfirmed())
                        throw rejected();
                    matched.get(kind).add(ev.id());
                    if(ev.publiclyObserved())nObserved++;
                    switch(ev.finding()) {
                        case SOURCE_LISTED_LEGAL_QUERY_METHOD,
                             SOURCE_LISTED_NONLEGAL_QUERY_ONLY,
                             SOURCE_LISTED_NORMAL_ABILITY,
                             SOURCE_LISTED_HIDDEN_ABILITY,
                             SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY -> {
                            if(!ev.sourceListed())throw rejected();
                            nListed++;
                            if(ev.finding()==PublicFormBuildReconciliation.Finding
                                    .SOURCE_LISTED_NONLEGAL_QUERY_ONLY)
                                issues.add(Issue.SOURCE_QUERY_NONLEGAL_ONLY);
                        }
                        case PUBLIC_EVENT_SOURCE_DISAGREEMENT -> {
                            if(!ev.publiclyObserved() || ev.sourceListed())throw rejected();
                            nConflict++;
                            issues.add(Issue.OBSERVED_SOURCE_DISAGREEMENT);
                        }
                        case SOURCE_NOT_LISTED -> {
                            if(ev.publiclyObserved() || ev.sourceListed())throw rejected();
                            nUnsupported++;
                            issues.add(Issue.UNOBSERVED_CANDIDATE_NOT_LISTED);
                        }
                        case ABSTAIN_INCOMPLETE_SOURCE,ABSTAIN_REPORTED_OVERRIDE -> {
                            if(ev.sourceListed())throw rejected();
                            nUnresolved++;
                            issues.add(Issue.UNRESOLVED_SOURCE_OR_OVERRIDE);
                        }
                        case ABSTAIN_TRANSFORMED_APPEARANCE -> {
                            if(!a.transformed() || ev.sourceListed())throw rejected();
                            nUnresolved++;
                            issues.add(Issue.TRANSFORM_CANNOT_VERIFY_NATIVE_BUILD);
                        }
                        case ITEM_LEGALITY_UNKNOWN -> {
                            if(field!=Field.ITEM || ev.sourceListed())throw rejected();
                            nUnresolved++;
                            issues.add(Issue.ITEM_ELIGIBILITY_UNVERIFIED);
                        }
                    }
                }
                if(!hits.equals(observedByKind.get(kind).keySet()))throw rejected();
                fields.put(field,new FieldSummary(field,nObserved,hits.size(),nListed,
                        nUnsupported,nConflict,nUnresolved,issues));
            }

            int roleCount=observedByKind.get(PublicOpponentBuildHypotheses.Kind.ROLE).size();
            var roleIssues=EnumSet.noneOf(Issue.class);
            if(roleCount>0)roleIssues.add(Issue.CONDITIONAL_ROLE_ONLY);
            fields.put(Field.ROLE,new FieldSummary(Field.ROLE,0,roleCount,0,0,
                    0,roleCount,roleIssues));
            var formIssues=EnumSet.of(Issue.DISPLAYED_IDENTITY_NOT_PROVEN);
            if(b.moveOrigin()==PublicFormBuildReconciliation.Origin.UNSUPPORTED_FORM
                    || b.abilityOrigin()==PublicFormBuildReconciliation.Origin.UNSUPPORTED_FORM)
                formIssues.add(Issue.DISPLAYED_FORM_NOT_SOURCE_SUPPORTED);
            int formSupported=formIssues.contains(Issue.DISPLAYED_FORM_NOT_SOURCE_SUPPORTED)?0:1;
            fields.put(Field.FORM,new FieldSummary(Field.FORM,0,1,formSupported,0,
                    0,1-formSupported,formIssues));

            // Public item and ability changes are observations over time,
            // not multiple simultaneously equipped build slots. The pinned
            // observer normally retains a last reveal; preserve the fact that
            // multiple public values were witnessed before source checking.
            Trace trace=traces.get(a.index());
            if(trace==null || !trace.opponent)throw rejected();
            enrich(fields,Field.MOVE,fields.get(Field.MOVE).observed()>4,
                    Issue.MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET);
            enrich(fields,Field.ABILITY,trace.abilityIds.size()>1,
                    Issue.MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED);
            enrich(fields,Field.ITEM,trace.itemIds.size()>1,
                    Issue.MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED);
            enrich(fields,Field.FORM,trace.displayTransition,
                    Issue.PUBLIC_FORM_DISPLAY_TRANSITION_UNRESOLVED);
            var gates=EnumSet.of(Gate.ABSTAIN_IDENTITY_UNCERTAIN);
            boolean hasConflict=fields.values().stream().anyMatch(f->f.conflicts()>0);
            if(hasConflict) {gates.add(Gate.ABSTAIN_SOURCE_DISAGREEMENT); conflicts++;}
            if(a.transformed() || formSupported==0 || trace.displayTransition)
                gates.add(Gate.ABSTAIN_TRANSFORM_OR_FORM);
            if(fields.values().stream().anyMatch(f->f.unresolved()>0
                        || f.sourceUnsupported()>0
                        || f.issues().contains(Issue.SOURCE_QUERY_NONLEGAL_ONLY)
                        || f.issues().contains(Issue.MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET)
                        || f.issues().contains(Issue.MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED)
                        || f.issues().contains(Issue.MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED)))
                gates.add(Gate.ABSTAIN_INCOMPLETE_COVERAGE);
            if(!hasConflict && !gates.contains(Gate.ABSTAIN_TRANSFORM_OR_FORM)
                    && !gates.contains(Gate.ABSTAIN_INCOMPLETE_COVERAGE))
                gates.add(Gate.SOURCE_OBSERVATIONS_CONSISTENT_ONLY);
            if(gates.stream().anyMatch(g->g!=Gate.SOURCE_OBSERVATIONS_CONSISTENT_ONLY))
                abstentions++;

            // Totals are per-appearance. One repeated nickname NEVER
            // contributes facts to any subsequent appearance.
            for(Field field:Field.values()) {
                var summary=fields.get(field);
                int[] t=running.get(field);
                t[0]+=summary.observed();t[1]+=summary.proposed();
                t[2]+=summary.sourceListed();t[3]+=summary.sourceUnsupported();
                t[4]+=summary.conflicts();t[5]+=summary.unresolved();
                if(summary.observed()==0)t[6]++;
            }
            result.add(new Appearance(a.index(),a.transformed(),true,
                    formSupported>0,fields,gates));
        }
        var totals=new EnumMap<Field,FieldTotal>(Field.class);
        for(Field f:Field.values()) {
            int[] x=running.get(f);
            totals.put(f,new FieldTotal(f,x[0],x[1],x[2],x[3],x[4],x[5],x[6]));
        }
        return new Report(result,totals,conflicts,abstentions);
    }


    private static void enrich(Map<Field,FieldSummary> map,Field field,
                               boolean conditional,Issue issue) {
        if(!conditional)return;
        var current=map.get(field);
        var issues=EnumSet.copyOf(current.issues().isEmpty()
                ?EnumSet.noneOf(Issue.class):current.issues());
        issues.add(issue);
        map.put(field,new FieldSummary(field,current.observed(),current.proposed(),
                current.sourceListed(),current.sourceUnsupported(),current.conflicts(),
                current.unresolved(),issues));
    }
    private static final class Trace {
        final boolean opponent;
        final Set<String> abilityIds=new HashSet<>();
        final Set<String> itemIds=new HashSet<>();
        boolean displayTransition;
        Trace(boolean opponent){this.opponent=opponent;}
    }
    /** Build one per-entry public event trace using the SAME chronological
     * switch/drag indexing as PublicReplayCorpus. NEVER join by nickname. */
    private static Map<Integer,Trace> publicTransitions(String log) {
        var traces=new HashMap<Integer,Trace>();
        var active=new HashMap<String,Integer>();
        int index=0;
        for(var line:PublicProtocolFilter.publicEvents(List.of(log))) {
            String[] p=line.split("\\|",-1);
            if(p.length<3)continue;
            String kind=p[1];
            String slot=slot(p[2]);
            if(slot==null)continue;
            if(kind.equals("switch") || kind.equals("drag")) {
                index++;
                active.put(slot,index);
                traces.put(index,new Trace(slot.equals("p2a")));
                continue;
            }
            Integer id=active.get(slot);
            if(id==null || !slot.equals("p2a"))continue;
            Trace trace=traces.get(id);
            if(trace==null)throw rejected();
            if(Set.of("replace","detailschange","-formechange","-mega",
                    "-terastallize").contains(kind))
                trace.displayTransition=true;
            if(p.length<4)continue;
            if(kind.equals("-ability") || kind.equals("-endability")
                    || kind.equals("-item") || kind.equals("-enditem")) {
                String idValue=p[3].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
                if(idValue.isEmpty() || idValue.length()>40)continue;
                if(kind.contains("ability")) trace.abilityIds.add(idValue);
                else trace.itemIds.add(idValue);
            }
        }
        return traces;
    }
    private static String slot(String actor) {
        if(actor.startsWith("p2a:"))return "p2a";
        if(actor.startsWith("p1a:"))return "p1a";
        return null;
    }

    private static PublicOpponentBuildHypotheses.Kind asKind(Field f) {
        return switch(f) {
            case MOVE -> PublicOpponentBuildHypotheses.Kind.MOVE;
            case ABILITY -> PublicOpponentBuildHypotheses.Kind.ABILITY;
            case ITEM -> PublicOpponentBuildHypotheses.Kind.ITEM;
            default -> throw rejected();
        };
    }
    private static IllegalArgumentException rejected(){
        return new IllegalArgumentException("Unverified offline opponent build consistency");
    }
}
