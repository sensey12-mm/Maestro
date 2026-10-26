package com.maestroai.training;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 7.4: OFFLINE, local-only corpus of independently reviewed Legacy choices.
 * Not a live controller, simulator, win-rate estimator or proof of Minecraft acceptance.
 * Raw histories are transient caller-owned inputs; no raw/private bytes enter reports.
 */
public final class Phase7Corpus {
    private Phase7Corpus() {}
    public static final String SCHEMA = "phase7.4.v1";
    private static final int MAX_ENTRIES = 100;
    private static final int MAX_CHECKPOINTS = 32;
    private static final int MAX_HISTORY_BYTES = 65_536;
    private static final int MAX_HISTORY_CHUNKS = 512;

    /** Each checkpoint is an independently observed COMPLETE raw-history prefix.
     * The final checkpoint MUST be exactly the capture handed to the importer.
     * Incomplete split blocks are never silently joined or guessed.
     */
    public record Submission(Phase7Benchmark.Scenario scenario,
                             Phase7LegacyImport.Capture capture,
                             List<List<String>> checkpoints) {
        public Submission {
            Objects.requireNonNull(scenario, "scenario");
            Objects.requireNonNull(capture, "capture");
            Objects.requireNonNull(checkpoints, "checkpoints");
            if (checkpoints.isEmpty() || checkpoints.size() > MAX_CHECKPOINTS)
                throw new IllegalArgumentException("unsupported checkpoint count");
            var copy = new ArrayList<List<String>>();
            for (List<String> checkpoint : checkpoints) {
                Objects.requireNonNull(checkpoint);
                copy.add(List.copyOf(checkpoint));
            }
            checkpoints=List.copyOf(copy);
        }
        @Override public String toString() { return "Phase7CorpusSubmission[private evidence redacted]"; }
    }

    public enum Status {
        ACCEPTED, DUPLICATE_CAPTURE, DUPLICATE_BATTLE_TURN, DUPLICATE_EVIDENCE,
        REPLAY_REWRITE, REPLAY_INCOMPLETE, REPLAY_MISMATCH,
        NO_CUSTODY_REVIEW, INVALID_PROVENANCE, UNKNOWN_PUBLIC_EVIDENCE,
        STALE_TURN, AMBIGUOUS_IDENTITY, MISMATCHED_SCENARIO, ILLEGAL_ACTION
    }

    /** No battle UUID, raw log, name, packet, opponent set or capture ID is exported. */
    public record Row(String scenario, Status status, String stateFingerprint,
                      String publicFingerprint, String legacyAction,
                      String baselineAction, String shadowAction,
                      Boolean baselineAgreement, Boolean shadowAgreement,
                      boolean shadowAbstained, boolean shadowIllegal) {
        public Row {
            Objects.requireNonNull(status);
            if (status!=Status.ACCEPTED && (legacyAction!=null || baselineAction!=null
                    || shadowAction!=null || baselineAgreement!=null || shadowAgreement!=null))
                throw new IllegalArgumentException("rejected evidence cannot be scored");
        }
    }

    public record Coverage(int submitted, int accepted, int rejected,
                           int duplicates, int replayRejected, int provenanceRejected,
                           int comparisons, int baselineAgreements, int shadowAgreements,
                           int shadowDisagreements, int shadowAbstentions, int shadowIllegal,
                           int unknownEvidence) {}

    public record Report(String schema, long seed, String fingerprint,
                         List<Row> rows, Coverage coverage) {
        public Report {
            if (!SCHEMA.equals(schema) || fingerprint==null || !fingerprint.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid corpus report");
            rows=List.copyOf(rows);
        }
        /** Stable sanitized machine-readable JSON. Synthetic evidence is NOT empirical validation. */
        public String json() {
            JsonObject root=new JsonObject();
            root.addProperty("schema",schema);
            root.addProperty("seed",seed);
            root.addProperty("reportFingerprint",fingerprint);
            root.addProperty("scope","reviewed-action-agreement-only;not-minecraft;not-win-rate");
            JsonArray items=new JsonArray();
            for (Row row:rows) {
                JsonObject x=new JsonObject();
                x.addProperty("scenario",row.scenario());
                x.addProperty("status",row.status().name());
                add(x,"stateFingerprint",row.stateFingerprint());
                add(x,"publicFingerprint",row.publicFingerprint());
                add(x,"legacyAction",row.legacyAction());
                add(x,"baselineAction",row.baselineAction());
                add(x,"shadowAction",row.shadowAction());
                add(x,"baselineAgreement",row.baselineAgreement());
                add(x,"shadowAgreement",row.shadowAgreement());
                x.addProperty("shadowAbstained",row.shadowAbstained());
                x.addProperty("shadowIllegal",row.shadowIllegal());
                items.add(x);
            }
            root.add("rows",items);
            JsonObject c=new JsonObject();
            c.addProperty("submitted",coverage.submitted());
            c.addProperty("accepted",coverage.accepted());
            c.addProperty("rejected",coverage.rejected());
            c.addProperty("duplicates",coverage.duplicates());
            c.addProperty("replayRejected",coverage.replayRejected());
            c.addProperty("provenanceRejected",coverage.provenanceRejected());
            c.addProperty("comparisons",coverage.comparisons());
            c.addProperty("baselineAgreements",coverage.baselineAgreements());
            c.addProperty("shadowAgreements",coverage.shadowAgreements());
            c.addProperty("shadowDisagreements",coverage.shadowDisagreements());
            c.addProperty("shadowAbstentions",coverage.shadowAbstentions());
            c.addProperty("shadowIllegal",coverage.shadowIllegal());
            c.addProperty("unknownEvidence",coverage.unknownEvidence());
            root.add("coverage",c);
            return root.toString();
        }
        private static void add(JsonObject o,String key,String v) {
            if(v==null)o.add(key,JsonNull.INSTANCE); else o.addProperty(key,v);
        }
        private static void add(JsonObject o,String key,Boolean v) {
            if(v==null)o.add(key,JsonNull.INSTANCE); else o.addProperty(key,v);
        }
    }

    /**
     * Caller supplies ORIGINAL locally held evidence and a genuinely independent
     * custodian. The callback may consult an external authenticated custody
     * record, but neither a receipt string nor same-bundle SHA is a signature.
     * A null callback always produces zero accepted rows.
     */
    public static Report evaluate(List<Submission> submissions, long seed,
            Phase7LegacyImport.CustodyReviewer reviewer, Phase7Benchmark.DecisionPolicy shadow) {
        Objects.requireNonNull(submissions);
        if (submissions.size()>MAX_ENTRIES) throw new IllegalArgumentException("corpus capacity exceeded");
        List<Submission> input=List.copyOf(submissions);
        // Fail closed on *all* repeated keys, independent of submission order.
        Map<String,Integer> ids=new HashMap<>();
        Map<String,Integer> turns=new HashMap<>();
        Map<String,Integer> evidence=new HashMap<>();
        for(Submission s:input) {
            var c=s.capture();
            String id=c.choice().captureId();
            count(ids,id);
            count(turns,c.battleId()+":"+c.choice().turn());
            count(evidence,c.choice().ownRequestSha256()+":"+c.choice().publicSha256()
                    +":"+c.choice().turn()+":"+c.choice().action());
        }

        List<Row> rows=new ArrayList<>();
        List<Submission> sorted=new ArrayList<>(input);
        sorted.sort(Comparator.comparing((Submission s)->s.scenario().id())
                .thenComparing(s->s.capture().choice().captureId()));
        for(Submission s:sorted) {
            var c=s.capture();
            String state=Phase7Benchmark.stateFingerprint(s.scenario());
            Status status;
            String publicHash=null;
            if(ids.get(c.choice().captureId())>1) status=Status.DUPLICATE_CAPTURE;
            else if(turns.get(c.battleId()+":"+c.choice().turn())>1)
                status=Status.DUPLICATE_BATTLE_TURN;
            else if(evidence.get(c.choice().ownRequestSha256()+":"+c.choice().publicSha256()
                    +":"+c.choice().turn()+":"+c.choice().action())>1)
                status=Status.DUPLICATE_EVIDENCE;
            else {
                status=validateReplay(s);
                if(status==Status.ACCEPTED) {
                    Phase7LegacyImport.ImportResult imported=Phase7LegacyImport.importCapture(
                            c,s.scenario(),reviewer);
                    status=translate(imported.status());
                    if(status==Status.ACCEPTED) {
                        publicHash=imported.publicSha256();
                        var comparison=Phase7LegacyImport.compare(s.scenario(),imported,
                                scenarioSeed(seed,s.scenario()),shadow);
                        rows.add(new Row(s.scenario().id(),Status.ACCEPTED,state,publicHash,
                                comparison.legacyAction(),comparison.baselineAction(),
                                comparison.shadowAction(),comparison.baselineAgreement(),
                                comparison.shadowAgreement(),comparison.shadowAbstained(),
                                comparison.shadowIllegal()));
                        continue;
                    }
                }
            }
            rows.add(new Row(s.scenario().id(),status,state,null,null,null,null,
                    null,null,true,false));
        }
        int accepted=0, duplicates=0, replay=0, provenance=0, comparisons=0, baselines=0,
                shadows=0, disagreed=0, abstained=0, illegal=0, unknown=0;
        for(Row row:rows) {
            if(row.status()==Status.ACCEPTED) {
                accepted++;
                if(row.baselineAgreement()!=null) {
                    comparisons++;
                    if(row.baselineAgreement())baselines++;
                }
                if(Boolean.TRUE.equals(row.shadowAgreement()))shadows++;
                if(Boolean.FALSE.equals(row.shadowAgreement()))disagreed++;
                if(row.shadowIllegal())illegal++;
                if(row.shadowAbstained())abstained++;
            } else {
                if(row.status().name().startsWith("DUPLICATE_"))duplicates++;
                else if(row.status().name().startsWith("REPLAY_"))replay++;
                else provenance++;
                if(row.status()==Status.UNKNOWN_PUBLIC_EVIDENCE
                        || row.status()==Status.STALE_TURN
                        || row.status()==Status.AMBIGUOUS_IDENTITY) unknown++;
                abstained++;
            }
        }
        Coverage coverage=new Coverage(rows.size(),accepted,rows.size()-accepted,
                duplicates,replay,provenance,comparisons,baselines,shadows,
                disagreed,abstained,illegal,unknown);
        // Hash ONLY the sanitized result and seed. Private contents never leave memory.
        String material=SCHEMA+"|"+seed+"|"+rows.toString()+"|"+coverage.toString();
        return new Report(SCHEMA,seed,sha256(material),rows,coverage);
    }

    /** Replay complete incremental snapshots, not inferred future events. */
    private static Status validateReplay(Submission s) {
        try {
            List<String> previous=List.of();
            var capture=new PublicBattleCapture();
            var publicProjection=new PublicProtocolEvidence();
            int priorTurn=0;
            List<String> lastPublic=List.of();
            for(List<String> checkpoint:s.checkpoints()) {
                if (checkpoint.isEmpty() || checkpoint.size()>MAX_HISTORY_CHUNKS
                        || bytes(checkpoint)>MAX_HISTORY_BYTES)
                    return Status.REPLAY_INCOMPLETE;
                if(checkpoint.size()<previous.size()
                        || !checkpoint.subList(0,previous.size()).equals(previous))
                    return Status.REPLAY_REWRITE;
                List<String> events=PublicProtocolFilter.publicEvents(checkpoint);
                var envelope=PublicEnvelopeProvenance.reconcile(checkpoint,events);
                if(!envelope.complete() || events.isEmpty()
                        || envelope.origins().size()!=events.size()
                        || envelope.origins().stream().anyMatch(o->
                           o.route()==PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN))
                    return Status.REPLAY_INCOMPLETE;
                publicProjection.update(checkpoint);
                if(publicProjection.corrupted() || !publicProjection.complete()
                        || !publicProjection.framed()
                        || !publicProjection.coverageUnknown().isEmpty())
                    return Status.UNKNOWN_PUBLIC_EVIDENCE;
                int turn=publicProjection.publicTurn();
                if(turn<1 || turn<priorTurn)
                    return Status.STALE_TURN;
                var observed=capture.observeWithProvenance(s.capture().battleId(),checkpoint);
                if(capture.incomplete() || capture.recordedEvents()!=events.size()
                        || observed.envelopes().stream().anyMatch(e->!e.contains("branch_attested=true")))
                    return Status.REPLAY_INCOMPLETE;
                priorTurn=turn;
                previous=checkpoint;
                lastPublic=events;
            }
            if(!previous.equals(s.capture().rawHistory()))
                return Status.REPLAY_MISMATCH;
            if(!lastPublic.getLast().equals("|turn|"+s.capture().choice().turn()))
                return Status.STALE_TURN;
            if(!Phase7LegacyImport.publicDigest(lastPublic)
                    .equals(s.capture().choice().publicSha256()))
                return Status.REPLAY_MISMATCH;
            // Enforce the same ambiguity restrictions as Phase 7.2 independently
            // before delegating to its independently reviewed custody importer.
            if(publicProjection.reentryAmbiguous("p1a")
                    || publicProjection.reentryAmbiguous("p2a")
                    || lastPublic.stream().anyMatch(line->
                         line.startsWith("|replace|") || line.startsWith("|-transform|")
                         || line.startsWith("|detailschange|") || line.startsWith("|-formechange|")
                         || line.startsWith("|drag|")))
                return Status.AMBIGUOUS_IDENTITY;
            return Status.ACCEPTED;
        } catch(RuntimeException invalid) {
            return Status.REPLAY_INCOMPLETE;
        }
    }
    private static long scenarioSeed(long seed,Phase7Benchmark.Scenario s) {
        // Reuse a versioned, stable public-state fingerprint for per-entry seeds.
        String fp=Phase7Benchmark.stateFingerprint(s);
        return seed ^ Long.parseUnsignedLong(fp.substring(0,16),16);
    }
    private static int bytes(List<String> chunks) {
        int sum=0;
        for(String x:chunks) {
            sum+=x.getBytes(StandardCharsets.UTF_8).length;
            if(sum>MAX_HISTORY_BYTES)return sum;
        }
        return sum;
    }
    private static void count(Map<String,Integer> counts,String key) {
        counts.merge(key,1,Integer::sum);
    }
    private static Status translate(Phase7LegacyImport.Status status) {
        return Status.valueOf(status.name());
    }
    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException impossible) {throw new AssertionError(impossible);}
    }

    /**
     * Persist only sanitized reports under the repo's ignored /run path.
     * No original histories, receipts or approvals are saved. Caller must
     * still protect its private originals outside this repository.
     */
    public static Path writeLocalReport(Path repositoryRoot, String filename, Report report)
            throws IOException {
        Objects.requireNonNull(repositoryRoot);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(report);
        if(!filename.matches("phase7_[a-z0-9_-]{1,48}\\.json"))
            throw new IllegalArgumentException("invalid local report filename");
        Path root=repositoryRoot.toAbsolutePath().normalize();
        Path run=root.resolve("run"), directory=run.resolve("phase7-corpus");
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root) || Files.isSymbolicLink(run)
                || Files.isSymbolicLink(directory))
            throw new IOException("unsafe local report directory");
        Files.createDirectories(directory);
        if(Files.isSymbolicLink(directory) || Files.isSymbolicLink(directory.resolve(filename)))
            throw new IOException("unsafe local report target");
        Path staging=Files.createTempFile(directory,".phase7-", ".tmp");
        try {
            Files.writeString(staging,report.json(),StandardCharsets.UTF_8);
            Files.move(staging,directory.resolve(filename),StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(staging); }
        return directory.resolve(filename);
    }
}
