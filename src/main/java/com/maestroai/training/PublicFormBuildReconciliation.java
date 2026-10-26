package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.15 — independent, offline reconciliation of the source-pinned
 * Cobblemon 1.7.3 form resolver and LearnsetQuery semantics.
 *
 * SOURCE-LISTED is never installed-game legal, identity, or an observed build.
 * Only a fresh signed-custody Phase 5.14 report can reach this adapter.
 */
public final class PublicFormBuildReconciliation {
    private PublicFormBuildReconciliation() {}

    public static final String UPSTREAM_REVISION=PublicSpeciesBuildLegality.SOURCE_REVISION;
    public static final String FORM_RESOLVER_BLOB="2dd3d7814c2538e0907e8ef2a0a048e96ee3886a";
    public static final String LEARNSET_BLOB="bdff7f678b4a0fdb4ea8138c0c2eea40fbcc3f7f";
    public static final String LEARNSET_QUERY_BLOB="7a7a42db6543b4f928c9fb36ff80738b71ef3188";
    public static final String LEARNSET_ADAPTER_BLOB="ba6bcb82c49a9dc2e42bb9427337f89250a0426d";
    public static final String SPECIES_ADDITIONS_BLOB="0e9549dbd086fb67690cbd27feef0e880c8b8293";

    private static final Map<String,String> FORM_BASE=Map.of(
            "zoroarkhisui","zoroark",
            "mewtwomegay","mewtwo");
    private static final Set<String> LEGAL_QUERY=Set.of("level","egg","tm","tutor","form_change","evolution");
    private static final Set<String> NONLEGAL_QUERY=Set.of("legacy","special");

    public enum Field { MOVES, ABILITIES }
    public enum Origin { BASE_EXPLICIT, FORM_REPLACES_BASE, FORM_INHERITS_BASE, UNSUPPORTED_FORM }
    public enum Finding {
        SOURCE_LISTED_LEGAL_QUERY_METHOD,
        SOURCE_LISTED_NONLEGAL_QUERY_ONLY,
        SOURCE_LISTED_NORMAL_ABILITY,
        SOURCE_LISTED_HIDDEN_ABILITY,
        SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY,
        SOURCE_NOT_LISTED,
        PUBLIC_EVENT_SOURCE_DISAGREEMENT,
        ABSTAIN_INCOMPLETE_SOURCE,
        ABSTAIN_REPORTED_OVERRIDE,
        ABSTAIN_TRANSFORMED_APPEARANCE,
        ITEM_LEGALITY_UNKNOWN
    }

    /**
     * A notice supplied by an untrusted caller ONLY downgrades confidence.
     * Its absence NEVER establishes the installed registry is unmodified.
     */
    public record OverrideNotice(String exactFormId,Field field) {
        public OverrideNotice {
            if(exactFormId==null || !exactFormId.matches("[a-z0-9]{1,40}")
                    || field==null) throw invalid();
        }
    }

    /** Exact upstream source blobs are inspected and pinned. The snapshot
     * cannot be overridden by a caller-selected branch or stale source pin. */
    public record ResolverPin(String commit,String formBlob,String learnsetBlob,
                              String queryBlob,String adapterBlob,String additionsBlob) {
        public ResolverPin {
            if(!UPSTREAM_REVISION.equals(commit) || !FORM_RESOLVER_BLOB.equals(formBlob)
                    || !LEARNSET_BLOB.equals(learnsetBlob)
                    || !LEARNSET_QUERY_BLOB.equals(queryBlob)
                    || !LEARNSET_ADAPTER_BLOB.equals(adapterBlob)
                    || !SPECIES_ADDITIONS_BLOB.equals(additionsBlob))
                throw invalid();
        }
        public static ResolverPin pinned() {
            return new ResolverPin(UPSTREAM_REVISION,FORM_RESOLVER_BLOB,LEARNSET_BLOB,
                    LEARNSET_QUERY_BLOB,LEARNSET_ADAPTER_BLOB,SPECIES_ADDITIONS_BLOB);
        }
    }

    /**
     * Each method is a source listing; LEGAL_QUERY_MEMBER refers only to
     * Cobblemon 1.7.3 LearnsetQuery.LEGAL membership (not current level or TM
     * acquisition, installed modpack legality, nor Showdown OU legality).
     */
    public record Method(String name,boolean legalQueryMember) {
        public Method {
            if(!LEGAL_QUERY.contains(name) && !NONLEGAL_QUERY.contains(name))
                throw invalid();
            if(legalQueryMember!=LEGAL_QUERY.contains(name)) throw invalid();
        }
    }

    public record Evidence(int appearanceIndex,PublicOpponentBuildHypotheses.Kind kind,
                           String id,Finding finding,Origin origin,
                           Set<Method> listedMethods,boolean publiclyObserved,
                           boolean sourceListed,boolean fullInstalledRegistryAttested,
                           boolean nativeBuildConfirmed) {
        public Evidence {
            if(appearanceIndex<1 || kind==null
                    || kind==PublicOpponentBuildHypotheses.Kind.ROLE
                    || id==null || !id.matches("[a-z0-9]{1,40}")
                    || finding==null || origin==null || listedMethods==null
                    || fullInstalledRegistryAttested || nativeBuildConfirmed
                    || (kind!=PublicOpponentBuildHypotheses.Kind.MOVE && !listedMethods.isEmpty())
                    || (finding==Finding.ITEM_LEGALITY_UNKNOWN
                        && kind!=PublicOpponentBuildHypotheses.Kind.ITEM))
                throw invalid();
            listedMethods=Set.copyOf(listedMethods);
        }
    }
    public record Appearance(int index,String displayedId,boolean identityUncertain,
                             boolean transformed,Origin moveOrigin,Origin abilityOrigin,
                             List<Evidence> evidence) {
        public Appearance {
            if(index<1 || displayedId==null || !displayedId.matches("[a-z0-9]{1,40}")
                    || !identityUncertain || moveOrigin==null || abilityOrigin==null
                    || evidence==null) throw invalid();
            evidence=List.copyOf(evidence);
        }
    }
    public static final class Report {
        private final List<Appearance> appearances;
        private Report(List<Appearance> appearances) {
            this.appearances=List.copyOf(appearances);
        }
        public List<Appearance> appearances(){return appearances;}
        public boolean installedModpackCompatibilityVerified(){return false;}
        public boolean battleFormIdentityVerified(){return false;}
        public boolean itemLegalityKnown(){return false;}
        public boolean calibratedProbabilitiesProduced(){return false;}
        public boolean shadowTacticalReady(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "PinnedFormReconciliation[offline-source-only;no-runtime-legality]";
        }
    }
    private record Resolved(PublicSpeciesBuildLegality.SourceRecord source,Origin origin) {}

    public static Report reconcile(PublicReplayCorpus.Source source,String log,
                                   List<PublicOpponentBuildHypotheses.CandidateQuery> proposed,
                                   PublicSpeciesBuildLegality.SourcePin sourcePin,
                                   ResolverPin resolverPin,Set<OverrideNotice> notices,
                                   PublicSignedCustody.SignedManifest signed,
                                   PublicSignedCustody.TrustedPolicy trusted,
                                   long evaluatedAtEpochSecond) {
        if(sourcePin==null || resolverPin==null || notices==null
                || !PublicSpeciesBuildLegality.sourceSliceIntegrityVerified()
                || notices.size()>32)throw invalid();
        Set<OverrideNotice> conservative=Set.copyOf(notices);
        for(OverrideNotice notice:conservative)
            if(notice==null || PublicSpeciesBuildLegality.sourceRecord(notice.exactFormId())==null)
                throw invalid();

        // Independently rerun the full permitted replay/signed custody intake.
        // No caller-authored records can masquerade as observed build evidence.
        var upstream=PublicSpeciesBuildLegality.analyze(source,log,proposed,
                sourcePin,signed,trusted,evaluatedAtEpochSecond);
        if(upstream.fullCobblemonRegistryAttested()
                || upstream.livePromotionAuthorized())throw invalid();

        var all=new ArrayList<Appearance>();
        for(var appearance:upstream.appearances()){
            var moves=resolve(appearance.displayedSpecies(),Field.MOVES);
            var abilities=resolve(appearance.displayedSpecies(),Field.ABILITIES);
            var rows=new ArrayList<Evidence>();
            for(var item:appearance.candidates()){
                if(item.kind()==PublicOpponentBuildHypotheses.Kind.ITEM) {
                    rows.add(new Evidence(item.appearanceIndex(),item.kind(),item.id(),
                            Finding.ITEM_LEGALITY_UNKNOWN,moves.origin(),Set.of(),
                            item.publiclyObserved(),false,false,false));
                    continue;
                }
                Field field=item.kind()==PublicOpponentBuildHypotheses.Kind.MOVE
                        ?Field.MOVES:Field.ABILITIES;
                Resolved resolution=field==Field.MOVES?moves:abilities;
                Finding finding;
                Set<Method> methods=Set.of();
                boolean listed=false;
                if(appearance.transformed()) {
                    finding=Finding.ABSTAIN_TRANSFORMED_APPEARANCE;
                } else if(conservative.contains(new OverrideNotice(appearance.displayedSpecies(),field))
                        || FORM_BASE.containsKey(appearance.displayedSpecies())
                        && conservative.contains(new OverrideNotice(
                            FORM_BASE.get(appearance.displayedSpecies()),field))) {
                    finding=Finding.ABSTAIN_REPORTED_OVERRIDE;
                } else if(resolution.source()==null) {
                    finding=Finding.ABSTAIN_INCOMPLETE_SOURCE;
                } else if(field==Field.MOVES) {
                    Set<String> raw=resolution.source().learningMethods()
                            .getOrDefault(item.id(),Set.of());
                    if(!raw.isEmpty()) {
                        var converted=new HashSet<Method>();
                        for(String name:raw)converted.add(new Method(name,LEGAL_QUERY.contains(name)));
                        methods=Set.copyOf(converted);
                        listed=true;
                        boolean queryEligible=methods.stream().anyMatch(Method::legalQueryMember);
                        finding=queryEligible?Finding.SOURCE_LISTED_LEGAL_QUERY_METHOD
                                :Finding.SOURCE_LISTED_NONLEGAL_QUERY_ONLY;
                    } else {
                        finding=item.publiclyObserved()
                                ?Finding.PUBLIC_EVENT_SOURCE_DISAGREEMENT
                                :Finding.SOURCE_NOT_LISTED;
                    }
                } else {
                    boolean normal=resolution.source().normalAbilities().contains(item.id());
                    boolean hidden=resolution.source().hiddenAbilities().contains(item.id());
                    if(normal && hidden) {
                        listed=true;
                        finding=Finding.SOURCE_LISTED_NORMAL_AND_HIDDEN_ABILITY;
                    } else if(normal) {
                        listed=true; finding=Finding.SOURCE_LISTED_NORMAL_ABILITY;
                    } else if(hidden) {
                        listed=true; finding=Finding.SOURCE_LISTED_HIDDEN_ABILITY;
                    } else {
                        finding=item.publiclyObserved()
                                ?Finding.PUBLIC_EVENT_SOURCE_DISAGREEMENT
                                :Finding.SOURCE_NOT_LISTED;
                    }
                }
                rows.add(new Evidence(item.appearanceIndex(),item.kind(),item.id(),
                        finding,resolution.origin(),methods,item.publiclyObserved(),
                        listed,false,false));
            }
            rows.sort(Comparator.comparing(Evidence::kind).thenComparing(Evidence::id));
            all.add(new Appearance(appearance.index(),appearance.displayedSpecies(),
                    appearance.identityUncertain(),appearance.transformed(),
                    moves.origin(),abilities.origin(),rows));
        }
        return new Report(all);
    }

    /**
     * FormData 1.7.3 exposes moves as _moves ?: species.moves and abilities as
     * _abilities ?: species.abilities. An explicit form value REPLACES base
     * even when empty, never unions it. Unknown forms never fall back to base.
     */
    private static Resolved resolve(String exact,Field field) {
        var own=PublicSpeciesBuildLegality.sourceRecord(exact);
        if(own==null)return new Resolved(null,Origin.UNSUPPORTED_FORM);
        boolean explicit=field==Field.MOVES?own.explicitMoves():own.explicitAbilities();
        String baseId=FORM_BASE.get(exact);
        if(baseId==null) {
            return new Resolved(explicit?own:null,Origin.BASE_EXPLICIT);
        }
        if(explicit) {
            return new Resolved(own,Origin.FORM_REPLACES_BASE);
        }
        var parent=PublicSpeciesBuildLegality.sourceRecord(baseId);
        if(parent==null || !(field==Field.MOVES?parent.explicitMoves():parent.explicitAbilities()))
            return new Resolved(null,Origin.FORM_INHERITS_BASE);
        return new Resolved(parent,Origin.FORM_INHERITS_BASE);
    }

    /** An external conflict/override assertion can only cause abstention.
     * This probe intentionally returns no trustworthy report, and is used
     * for independently verifying unsafe records are NOT acceptable.
     */
    static boolean acceptsFullSourceRecord(String exact,Field field) {
        if(exact==null || field==null)return false;
        Resolved r=resolve(exact,field);
        return r.source()!=null && r.origin()!=Origin.UNSUPPORTED_FORM;
    }

    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Unverified offline form inheritance evidence");
    }
}
