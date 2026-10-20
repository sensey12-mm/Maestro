package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.1: offline, synthetic-only benchmark contracts. This class lives in
 * verification/, never in the shipped mod. Scores are fixture criteria, NOT
 * damage predictions, battle outcomes, win rates or Minecraft acceptance.
 *
 * Callers supplying captured Legacy actions must verify custody independently;
 * this runner checks their snapshot binding, not the authenticity of a capture.
 */
public final class Phase7Benchmark {
    private Phase7Benchmark() {}
    public static final String SCHEMA = "phase7.1.v1";
    private static final int MAX_FIXTURE_BYTES = 65536;
    private static final java.util.regex.Pattern ID =
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    /** Only narrow declared synthetic criteria are scored. */
    public record Criterion(double utility, double uncertainty) {
        public Criterion {
            if (!Double.isFinite(utility) || Math.abs(utility) > 1
                    || !Double.isFinite(uncertainty) || uncertainty < 0 || uncertainty > 1)
                throw new IllegalArgumentException("invalid synthetic criterion");
        }
    }
    public record Evidence(boolean publicOnly, boolean fresh, boolean mechanicsSupported, int turn) {}
    /** Not a proof of provenance. A trusted upstream importer must attest custody. */
    public record LegacyRecord(String captureId, int turn, String publicStateFingerprint,
                               String action, boolean externallyVerified) {
        public LegacyRecord {
            requireId(captureId);
            requireAction(action);
            if (turn < 0 || publicStateFingerprint == null
                    || !publicStateFingerprint.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid legacy binding");
        }
    }
    public record Scenario(String id, int version, BattleSnapshot snapshot,
                           SortedMap<String, Criterion> criteria, Evidence evidence,
                           LegacyRecord legacy) {
        public Scenario {
            requireId(id);
            if (version != 1) throw new IllegalArgumentException("unsupported scenario version");
            Objects.requireNonNull(snapshot);
            Objects.requireNonNull(evidence);
            if (snapshot.turn() != evidence.turn())
                throw new IllegalArgumentException("evidence turn mismatch");
            if (snapshot.legalMoves().size() + snapshot.legalSwitches().size() == 0)
                throw new IllegalArgumentException("scenario without legal actions");
            // No private-side opponent observations enter Phase 7.1 fixtures,
            // even if a caller mistakenly labels those observations "public".
            if (snapshot.opponent().hpKnown() || !snapshot.opponent().revealedMoves().isEmpty()
                    || snapshot.opponent().revealedItem() != null
                    || snapshot.opponent().revealedAbility() != null
                    || !snapshot.opponentBench().isEmpty()
                    || !snapshot.knownOpponentMoveCounts().isEmpty()
                    || snapshot.opponentTerastallized())
                throw new IllegalArgumentException("opponent evidence unsupported in phase 7.1");
            TreeSet<String> legal = legalActions(snapshot);
            criteria = Collections.unmodifiableSortedMap(new TreeMap<>(criteria));
            if (!legal.equals(criteria.keySet()))
                throw new IllegalArgumentException("synthetic criteria must cover legal action universe");
            for (String action : legal) requireAction(action);
        }
    }
    public record Suite(String schema, long seed, String configurationFingerprint, List<Scenario> scenarios) {
        public Suite {
            if (!SCHEMA.equals(schema)) throw new IllegalArgumentException("unsupported benchmark schema");
            if (configurationFingerprint == null || !configurationFingerprint.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid configuration fingerprint");
            scenarios = List.copyOf(scenarios);
            if (scenarios.isEmpty() || scenarios.size() > 100)
                throw new IllegalArgumentException("invalid suite size");
            var seen = new HashSet<String>();
            for (Scenario s : scenarios) if (!seen.add(s.id())) throw new IllegalArgumentException("duplicate scenario");
        }
    }
    @FunctionalInterface public interface DecisionPolicy {
        /** Null means abstain. Never used as a live battle policy. */
        String choose(BattleSnapshot publicOnlySnapshot, long deterministicSeed);
    }
    public record Row(String scenarioId, String stateFingerprint, String status,
                      String baseline, String candidate, Double baselineRegret,
                      Double candidateRegret, Double candidateUncertainty, String legacy,
                      Boolean legacyDisagreement, boolean illegalCandidate, boolean regression) {}
    public record Report(String schema, long seed, String configurationFingerprint,
                         List<Row> rows) {
        public Report { rows = List.copyOf(rows); }
        public String json() {
            int scored=0, unknown=0, illegal=0, abstained=0, regressions=0, legacyCompared=0;
            StringBuilder b = new StringBuilder(512);
            b.append("{\"schema\":").append(quoted(schema)).append(",\"seed\":").append(seed)
                    .append(",\"configurationFingerprint\":").append(quoted(configurationFingerprint))
                    .append(",\"scenarios\":[");
            for (int i=0; i<rows.size(); i++) {
                Row r=rows.get(i);
                if (i>0) b.append(',');
                if ("SCORED".equals(r.status())) scored++;
                else if ("UNKNOWN_EVIDENCE".equals(r.status())) unknown++;
                if (r.illegalCandidate()) illegal++;
                if (r.candidate()==null) abstained++;
                if (r.regression()) regressions++;
                if ("MATCHED_CAPTURE".equals(r.legacy())) legacyCompared++;
                b.append("{\"id\":").append(quoted(r.scenarioId()))
                        .append(",\"stateFingerprint\":").append(quoted(r.stateFingerprint()))
                        .append(",\"status\":").append(quoted(r.status()))
                        .append(",\"baseline\":").append(quoted(r.baseline()))
                        .append(",\"candidate\":").append(quoted(r.candidate()))
                        .append(",\"baselineRegret\":").append(number(r.baselineRegret()))
                        .append(",\"candidateRegret\":").append(number(r.candidateRegret()))
                        .append(",\"candidateUncertainty\":").append(number(r.candidateUncertainty()))
                        .append(",\"legacy\":").append(quoted(r.legacy()))
                        .append(",\"legacyDisagreement\":").append(r.legacyDisagreement())
                        .append(",\"illegalCandidate\":").append(r.illegalCandidate())
                        .append(",\"regression\":").append(r.regression()).append('}');
            }
            b.append("],\"summary\":{\"total\":").append(rows.size())
                    .append(",\"scored\":").append(scored)
                    .append(",\"unknownEvidence\":").append(unknown)
                    .append(",\"illegalCandidates\":").append(illegal)
                    .append(",\"abstentions\":").append(abstained)
                    .append(",\"syntheticRegressions\":").append(regressions)
                    .append(",\"matchedLegacyCaptures\":").append(legacyCompared)
                    .append("},\"scope\":\"synthetic-criteria-only;not-minecraft;not-win-rate\"}");
            return b.toString();
        }
    }

    /** Deliberately simple seeded legal baseline, stable under input action order. */
    public static String simpleBaseline(BattleSnapshot snapshot, long seed) {
        List<String> actions = new ArrayList<>(legalActions(snapshot));
        if (actions.isEmpty()) return null;
        return actions.get(new Random(seed).nextInt(actions.size()));
    }

    public static Report run(Suite suite, DecisionPolicy shadow) {
        Objects.requireNonNull(suite);
        List<Row> rows = new ArrayList<>();
        for (Scenario s : suite.scenarios().stream().sorted(Comparator.comparing(Scenario::id)).toList()) {
            String fp = stateFingerprint(s);
            LegacyRecord legacy=s.legacy();
            String legacyStatus="ABSENT";
            if (legacy!=null) {
                legacyStatus = legacy.externallyVerified() && legacy.turn()==s.snapshot().turn()
                        && fp.equals(legacy.publicStateFingerprint())
                        && legalActions(s.snapshot()).contains(legacy.action())
                        ? "MATCHED_CAPTURE" : "REJECTED_PROVENANCE";
            }
            if (!s.evidence().publicOnly() || !s.evidence().fresh()
                    || !s.evidence().mechanicsSupported()) {
                rows.add(new Row(s.id(),fp,"UNKNOWN_EVIDENCE",null,null,null,null,null,
                        "MATCHED_CAPTURE".equals(legacyStatus) ? "INELIGIBLE_EVIDENCE" : legacyStatus,null,false,false));
                continue;
            }
            String baseline=simpleBaseline(s.snapshot(),seedFor(suite.seed(),s.id(),s.version()));
            String candidate=null;
            boolean illegal=false;
            if (shadow!=null) {
                try {
                    candidate=shadow.choose(s.snapshot(),seedFor(suite.seed(),s.id(),s.version()));
                    if (candidate!=null && !legalActions(s.snapshot()).contains(candidate)) {
                        candidate=null; illegal=true;
                    }
                } catch (RuntimeException ignored) {
                    // Never expose exception messages or private actor details.
                    candidate=null; illegal=true;
                }
            }
            double best=s.criteria().values().stream().mapToDouble(Criterion::utility).max().orElseThrow();
            Double baseRegret=best-s.criteria().get(baseline).utility();
            Double candRegret=candidate==null?null:best-s.criteria().get(candidate).utility();
            Double unc=candidate==null?null:s.criteria().get(candidate).uncertainty();
            Boolean disagreement="MATCHED_CAPTURE".equals(legacyStatus)
                    ? !legacy.action().equals(baseline) : null;
            rows.add(new Row(s.id(),fp,"SCORED",baseline,candidate,baseRegret,
                    candRegret,unc,legacyStatus,disagreement,illegal,
                    candRegret!=null && candRegret>baseRegret+1e-9));
        }
        return new Report(suite.schema(),suite.seed(),suite.configurationFingerprint(),rows);
    }

    /** Exact bytes identify the test configuration; whitespace changes fingerprint too. */
    public static Suite parseFixture(String text) {
        Objects.requireNonNull(text);
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_FIXTURE_BYTES)
            throw new IllegalArgumentException("fixture too large");
        JsonObject root=StrictOwnJson.read(text);
        only(root,Set.of("schema","seed","scenarios"));
        String schema=str(root,"schema");
        long seed=root.get("seed").getAsLong();
        JsonArray items=array(root,"scenarios");
        if (items.size()<1 || items.size()>100) throw new IllegalArgumentException("invalid scenario count");
        List<Scenario> scenarios=new ArrayList<>();
        for (JsonElement element:items) {
            JsonObject o=element.getAsJsonObject();
            only(o,Set.of("id","version","turn","moves","switches","criteria",
                    "publicOnly","fresh","mechanicsSupported"));
            String id=str(o,"id");
            int version=o.get("version").getAsInt();
            int turn=o.get("turn").getAsInt();
            List<String> moves=strings(array(o,"moves"));
            List<String> switches=strings(array(o,"switches"));
            SortedMap<String,Criterion> criteria=new TreeMap<>();
            JsonObject rubric=obj(o,"criteria");
            for(var entry:rubric.entrySet()) {
                JsonObject score=entry.getValue().getAsJsonObject();
                only(score,Set.of("utility","uncertainty"));
                criteria.put(entry.getKey(),new Criterion(
                        score.get("utility").getAsDouble(),score.get("uncertainty").getAsDouble()));
            }
            BattleSnapshot.Combatant self=new BattleSnapshot.Combatant("synthetic-own",1,
                    List.of(),Map.of(),List.of(),null,null,false);
            BattleSnapshot.Combatant foe=new BattleSnapshot.Combatant("synthetic-opponent",
                    BattleSnapshot.UNKNOWN_HP_FRACTION,List.of(),Map.of(),List.of(),null,null,false);
            List<BattleSnapshot.Combatant> bench=new ArrayList<>();
            for(String ignored:switches)
                bench.add(new BattleSnapshot.Combatant("synthetic-reserve",1,List.of(),
                        Map.of(),List.of(),null,null,false));
            BattleSnapshot snapshot=new BattleSnapshot(turn,self,foe,bench,List.of(),
                    moves,switches,Map.of(),false,null);
            scenarios.add(new Scenario(id,version,snapshot,criteria,
                    new Evidence(bool(o,"publicOnly"),bool(o,"fresh"),bool(o,"mechanicsSupported"),turn),
                    null));
        }
        return new Suite(schema,seed,sha256(text.getBytes(StandardCharsets.UTF_8)),scenarios);
    }

    /** Stable hash binds a Legacy observation to this exact public-only scenario. */
    public static String stateFingerprint(Scenario s) {
        StringBuilder b=new StringBuilder(SCHEMA).append('|').append(s.id()).append('|')
                .append(s.version()).append('|').append(s.snapshot().turn()).append('|');
        // All supported synthetic input state is canonical; never hash runtime/private actors.
        for(String a:legalActions(s.snapshot())) b.append(a).append(';');
        b.append('|').append(s.snapshot().self().species()).append('|')
                .append(s.snapshot().self().hpFraction()).append('|')
                .append(s.snapshot().opponent().species()).append('|')
                .append(s.evidence().publicOnly()).append('|').append(s.evidence().fresh())
                .append('|').append(s.evidence().mechanicsSupported());
        return sha256(b.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static long seedFor(long seed,String id,int version) {
        // Stable FNV-1a (not Java's unstable default object hash codes).
        long h=0xcbf29ce484222325L ^ seed ^ version;
        for(byte b:id.getBytes(StandardCharsets.UTF_8)) { h ^= (b&0xff); h *= 0x100000001b3L; }
        return h;
    }
    private static TreeSet<String> legalActions(BattleSnapshot snapshot) {
        var actions=new TreeSet<String>();
        snapshot.legalMoves().forEach(a->{requireId(a);if(!actions.add("move:"+a))throw new IllegalArgumentException("duplicate legal move");});
        snapshot.legalSwitches().forEach(a->{requireId(a);if(!actions.add("switch:"+a))throw new IllegalArgumentException("duplicate legal switch");});
        return actions;
    }
    private static void requireId(String id) {
        if(id==null || !ID.matcher(id).matches()) throw new IllegalArgumentException("invalid public ID");
    }
    private static void requireAction(String action) {
        if(action==null || !(action.startsWith("move:") || action.startsWith("switch:")))
            throw new IllegalArgumentException("invalid action");
        requireId(action.substring(action.indexOf(':')+1));
    }
    private static void only(JsonObject o,Set<String> permitted) {
        if(!permitted.containsAll(o.keySet()) || !o.keySet().containsAll(permitted))
            throw new IllegalArgumentException("unexpected or missing fixture field");
    }
    private static String str(JsonObject o,String key) { return o.get(key).getAsString(); }
    private static boolean bool(JsonObject o,String key) {
        JsonPrimitive v=o.get(key).getAsJsonPrimitive();
        if(!v.isBoolean())throw new IllegalArgumentException("invalid evidence flag");
        return v.getAsBoolean();
    }
    private static JsonObject obj(JsonObject o,String key) { return o.get(key).getAsJsonObject(); }
    private static JsonArray array(JsonObject o,String key) { return o.get(key).getAsJsonArray(); }
    private static List<String> strings(JsonArray array) {
        var result=new ArrayList<String>();
        for(JsonElement x:array)result.add(x.getAsString());
        return result;
    }
    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String quoted(String s) {
        if(s==null)return "null";
        // Values are bounded strict IDs, fixed status strings and hex fingerprints.
        return "\"" + s + "\"";
    }
    private static String number(Double x) {return x==null?"null":Double.toString(x);}
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("usage: Phase7Benchmark <synthetic-fixture.json>");
        Suite suite=parseFixture(Files.readString(Path.of(args[0])));
        System.out.println(run(suite,null).json());
    }
}
