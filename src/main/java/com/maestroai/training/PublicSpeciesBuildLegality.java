package com.maestroai.training;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * Phase 5.14 — strictly pinned Cobblemon 1.7.3 public species-json evidence.
 * A source-file listing is NOT a current datapack, installed-game legality,
 * species identity, chosen move, active ability, or opponent probability.
 *
 * Public mirror file bytes were inspected at the explicit upstream revision.
 * This adapter fails closed if its normalized, complete source slice changes.
 * No external records can provide new "verified" entries by declaration.
 */
public final class PublicSpeciesBuildLegality {
    private PublicSpeciesBuildLegality() {}

    public static final String SOURCE_REPOSITORY="MayIHaveK/cobblemon-1.7.3";
    public static final String SOURCE_REVISION="e428d34ea7f4aeca6dc9031488fb51fe0e315218";
    public static final String NORMALIZED_SHA256="76b216aa5f3f1f16e866403d7bcc6ff297d18a2a83010c83777ea6730eb23b0d";
    public static final String RESOURCE="/com/maestroai/training/public-build-evidence-v1.tsv";
    private static final Map<String,String> BLOBS=Map.of(
            "ditto","902498a9744183b3d420432b7985e4ade1d7f696",
            "pikachu","d434536ae152ae042fb6648e1306d01ee7eab79a",
            "zoroark","155605e52064164f59524f0ab427aec177b8ca22",
            "zoroarkhisui","155605e52064164f59524f0ab427aec177b8ca22",
            "mewtwo","457721b23730989bc09883a9cd4a6669753c4878",
            "mewtwomegay","457721b23730989bc09883a9cd4a6669753c4878");
    private static final Map<String,String> FORMS=Map.of(
            "ditto","base","pikachu","base","zoroark","base",
            "zoroarkhisui","Hisui","mewtwo","base","mewtwomegay","Mega-Y");
    private static final Set<String> METHODS=Set.of("egg","tm","tutor","legacy","special");
    private static final Map<String,Entry> BUNDLED=load();

    public enum Status {
        SOURCE_LISTED_LEVEL_MOVE,
        SOURCE_LISTED_OTHER_METHOD,
        SOURCE_LISTED_NORMAL_ABILITY,
        SOURCE_LISTED_HIDDEN_ABILITY,
        UNKNOWN_IN_SOURCE_SLICE,
        SOURCE_FORM_METHOD_OR_ABILITY_UNSPECIFIED,
        TRANSFORM_OR_IDENTITY_UNCERTAIN
    }

    /** Untrusted descriptor must exactly match BOTH public-source revision
     * and the independently pinned local normalized-source SHA-256. */
    public record SourcePin(String repository,String revision,String normalizedSha256,
                            CompetitiveFormatNormalizer.Target target) {
        public SourcePin {
            if (!SOURCE_REPOSITORY.equals(repository) || !SOURCE_REVISION.equals(revision)
                    || !NORMALIZED_SHA256.equals(normalizedSha256)
                    || !CompetitiveFormatNormalizer.Target.pinnedSource().equals(target))
                throw invalid();
        }
        public static SourcePin pinned() {
            return new SourcePin(SOURCE_REPOSITORY,SOURCE_REVISION,NORMALIZED_SHA256,
                    CompetitiveFormatNormalizer.Target.pinnedSource());
        }
    }
    private record Entry(String id,String blob,String form,
                         Map<String,Set<String>> learningMethods,
                         Set<String> normalAbilities,Set<String> hiddenAbilities,
                         boolean explicitMoves,boolean explicitAbilities) {}
    /**
     * Package-private, read-only source snapshot for the Phase 5.15 reconciler.
     * It can never import caller-supplied species evidence or bypass the pinned
     * normalized-resource SHA-256 validation performed by load().
     */
    static record SourceRecord(String id,String form,String blob,
                               Map<String,Set<String>> learningMethods,
                               Set<String> normalAbilities,Set<String> hiddenAbilities,
                               boolean explicitMoves,boolean explicitAbilities) {}
    static SourceRecord sourceRecord(String exactSpeciesOrForm) {
        Entry entry=BUNDLED.get(exactSpeciesOrForm);
        return entry==null?null:new SourceRecord(entry.id(),entry.form(),entry.blob(),
                entry.learningMethods(),entry.normalAbilities(),entry.hiddenAbilities(),
                entry.explicitMoves(),entry.explicitAbilities());
    }
    static boolean sourceSliceIntegrityVerified() {
        return BUNDLED.keySet().equals(BLOBS.keySet());
    }

    /** Source-confirmed method, NOT proof of player-game teachability. */
    public record Candidate(int appearanceIndex,PublicOpponentBuildHypotheses.Kind kind,
                            String id,Status sourceStatus,Set<String> listedMethods,
                            boolean publiclyObserved,boolean sourceFormExplicit,
                            boolean runtimeLegalVerified,boolean currentBuildConfirmed) {
        public Candidate {
            if(appearanceIndex<1 || kind==null || kind==PublicOpponentBuildHypotheses.Kind.ROLE
                    || id==null || !id.matches("[a-z0-9]{1,40}") || sourceStatus==null
                    || listedMethods==null || runtimeLegalVerified || currentBuildConfirmed
                    || (kind!=PublicOpponentBuildHypotheses.Kind.MOVE && !listedMethods.isEmpty()))
                throw invalid();
            listedMethods=Set.copyOf(listedMethods);
        }
    }
    public record Appearance(int index,String displayedSpecies,boolean exactSourceForm,
                             boolean transformed,boolean identityUncertain,
                             boolean confirmedSamePartyMember,List<Candidate> candidates) {
        public Appearance {
            if(index<1 || displayedSpecies==null || !displayedSpecies.matches("[a-z0-9]{1,40}")
                    || !identityUncertain || confirmedSamePartyMember || candidates==null)
                throw invalid();
            candidates=List.copyOf(candidates);
        }
    }
    /** Constructor is private; no externally supplied candidate rows accepted. */
    public static final class Report {
        private final List<Appearance> appearances;
        private final String sourceScope;
        private Report(List<Appearance> a) {
            appearances=List.copyOf(a);
            sourceScope=CobblemonMechanics.SOURCE;
        }
        public List<Appearance> appearances(){return appearances;}
        public String sourceScope(){return sourceScope;}
        public boolean fullCobblemonRegistryAttested(){return false;}
        public boolean completeLearnsetOrAbilityCoverage(){return false;}
        public boolean itemLegalityVerified(){return false;}
        public boolean currentOpponentBuildConfirmed(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean shadowTacticalReady(){return false;}
        @Override public String toString(){
            return "PinnedOfflineBuildEvidence[source-listed-not-runtime-legal;identity-uncertain]";
        }
    }

    /**
     * Rebuild the complete upstream Phase 5.13 report from a permitted
     * signed-custody replay; then classify EXACT species/form source listings
     * without copying information between appearances or asserting legality.
     */
    public static Report analyze(PublicReplayCorpus.Source source,String publicLog,
                                 List<PublicOpponentBuildHypotheses.CandidateQuery> proposed,
                                 SourcePin pinned,
                                 PublicSignedCustody.SignedManifest signed,
                                 PublicSignedCustody.TrustedPolicy trusted,
                                 long evaluatedAtEpochSecond) {
        if (pinned==null || BUNDLED.size()!=BLOBS.size()) throw invalid();
        var prior=PublicOpponentBuildHypotheses.analyze(source,publicLog,
                pinned.target(),proposed,signed,trusted,evaluatedAtEpochSecond);
        if (!CobblemonMechanics.SOURCE.equals(prior.sourceScope())) throw invalid();
        var output=new ArrayList<Appearance>();
        for (var a:prior.appearances()) {
            Entry entry=BUNDLED.get(a.displayedSpecies());
            // The exact display token is not a verified underlying identity.
            // For unsupported forms, never fall back to base-species legality.
            var rows=new ArrayList<Candidate>();
            for(var h:a.hypotheses()) {
                if(h.kind()==PublicOpponentBuildHypotheses.Kind.ROLE)continue;
                Status category=Status.UNKNOWN_IN_SOURCE_SLICE;
                Set<String> methods=Set.of();
                if(a.transformed()) {
                    category=Status.TRANSFORM_OR_IDENTITY_UNCERTAIN;
                } else if(entry!=null) {
                    if (h.kind()==PublicOpponentBuildHypotheses.Kind.MOVE) {
                        if(!entry.explicitMoves())category=Status.SOURCE_FORM_METHOD_OR_ABILITY_UNSPECIFIED;
                        else {
                            methods=entry.learningMethods().getOrDefault(h.id(),Set.of());
                            if(methods.contains("level"))category=Status.SOURCE_LISTED_LEVEL_MOVE;
                            else if(!methods.isEmpty())category=Status.SOURCE_LISTED_OTHER_METHOD;
                        }
                    } else if(h.kind()==PublicOpponentBuildHypotheses.Kind.ABILITY) {
                        if(!entry.explicitAbilities())
                            category=Status.SOURCE_FORM_METHOD_OR_ABILITY_UNSPECIFIED;
                        else if(entry.normalAbilities().contains(h.id()))
                            category=Status.SOURCE_LISTED_NORMAL_ABILITY;
                        else if(entry.hiddenAbilities().contains(h.id()))
                            category=Status.SOURCE_LISTED_HIDDEN_ABILITY;
                    }
                    // No pinned item rules; all observed or suggested items
                    // remain unknown even when shown by a public event.
                }
                rows.add(new Candidate(a.index(),h.kind(),h.id(),category,methods,
                        h.publicEvent(),entry!=null,false,false));
            }
            rows.sort(Comparator.comparing(Candidate::kind).thenComparing(Candidate::id));
            output.add(new Appearance(a.index(),a.displayedSpecies(),entry!=null,
                    a.transformed(),true,false,rows));
        }
        return new Report(output);
    }

    /** Package-local testing seam, NOT an authority importer. Mutated,
     * incomplete, synthetic or mismatched bytes cannot equal the pinned hash.
     */
    static boolean acceptsExactSource(byte[] bytes) {
        try {return parse(bytes).size()==BLOBS.size();}
        catch(IllegalArgumentException ex){return false;}
    }

    private static Map<String,Entry> load() {
        try(var stream=PublicSpeciesBuildLegality.class.getResourceAsStream(RESOURCE)) {
            if(stream==null) return Map.of();
            return parse(stream.readAllBytes());
        } catch (IOException | IllegalArgumentException malformed) {
            return Map.of();
        }
    }
    private static Map<String,Entry> parse(byte[] bytes) {
        if(bytes==null || bytes.length<100 || bytes.length>20_000
                || !NORMALIZED_SHA256.equals(sha256(bytes)))throw invalid();
        var entries=new TreeMap<String,Entry>();
        String raw=new String(bytes,StandardCharsets.UTF_8);
        if(!Arrays.equals(bytes,raw.getBytes(StandardCharsets.UTF_8))
                || !raw.endsWith("\n"))throw invalid();
        for(String line:raw.stripTrailing().split("\n",-1)) {
            String[] p=line.split("\t",-1);
            if(p.length!=5 || !BLOBS.containsKey(p[0])
                    || !BLOBS.get(p[0]).equals(p[1])
                    || !FORMS.get(p[0]).equals(p[2]))throw invalid();
            var normal=new TreeSet<String>();
            var hidden=new TreeSet<String>();
            boolean abilities=!p[3].equals("-");
            if(abilities)for(String s:p[3].split(",",-1)) {
                boolean h=s.startsWith("h:");
                String id=h?s.substring(2):s;
                if(!id.matches("[a-z0-9]{1,40}")
                        || !(h?hidden:normal).add(id))throw invalid();
            }
            var moves=new TreeMap<String,Set<String>>();
            boolean explicitMoves=!p[4].equals("-");
            if(explicitMoves)for(String token:p[4].split(",",-1)) {
                int separator=token.indexOf(':');
                if(separator<1 || separator==token.length()-1)throw invalid();
                String method=token.substring(0,separator),id=token.substring(separator+1);
                if(!id.matches("[a-z0-9]{1,40}"))throw invalid();
                if(method.matches("[0-9]{1,3}"))method="level";
                else if(!METHODS.contains(method))throw invalid();
                moves.computeIfAbsent(id,ignored->new TreeSet<>()).add(method);
            }
            var methods=new TreeMap<String,Set<String>>();
            moves.forEach((k,v)->methods.put(k,Set.copyOf(v)));
            Entry entry=new Entry(p[0],p[1],p[2],Map.copyOf(methods),
                    Set.copyOf(normal),Set.copyOf(hidden),explicitMoves,abilities);
            if(entries.putIfAbsent(p[0],entry)!=null)throw invalid();
        }
        if(!entries.keySet().equals(BLOBS.keySet()))throw invalid();
        return Map.copyOf(entries);
    }
    private static String sha256(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable");}
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified pinned species and ability source");
    }
}
