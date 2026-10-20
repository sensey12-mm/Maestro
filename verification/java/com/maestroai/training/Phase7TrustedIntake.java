package com.maestroai.training;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Phase 7.5 verification-only, local private corpus intake.
 * Trust is a DETACHED Ed25519 custody signature verified against a public
 * key SHA-256 pinned OUTSIDE the untrusted bundle. An unreviewed local
 * receipt or a digest alone never constitutes custody approval.
 *
 * The signer must independently inspect the original Legacy return,
 * own-request provenance, artifact identity, battle actor, chronology and
 * unaltered private history. Crypto proves which key approved the manifest,
 * NOT the truth of an unreviewed observation or Minecraft compatibility.
 */
public final class Phase7TrustedIntake {
    private Phase7TrustedIntake() {}
    public static final String SCHEMA = "phase7.5.v1";
    public static final String CUSTODY = "phase7.5.custody.v1";
    private static final String DOMAIN = "MaestroAI-Phase7.5-Custody-v1\n";
    private static final java.util.regex.Pattern NAME =
            java.util.regex.Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final java.util.regex.Pattern HASH =
            java.util.regex.Pattern.compile("[a-f0-9]{64}");
    private static final int MAX_MANIFEST_BYTES = 16_384;
    private static final int MAX_SIGNATURE_BYTES = 2_048;
    private static final int MAX_CHECKPOINT_BYTES = 65_536;
    private static final int MAX_ENTRIES = 100;

    public enum Status {
        VERIFIED_FOR_CORPUS, DUPLICATE_INPUT, TRUST_KEY_INVALID, SIGNATURE_INVALID,
        MANIFEST_INVALID, CHECKPOINT_INVALID, SCENARIO_UNAVAILABLE,
        PATH_UNSAFE, CUSTODY_UNAVAILABLE
    }
    private record Loaded(Phase7Corpus.Submission submission,
                          Phase7LegacyImport.ReviewRequest binding) {}
    private record Attempt(Status status, Loaded loaded) {
        Attempt {
            if ((status == Status.VERIFIED_FOR_CORPUS) != (loaded != null))
                throw new IllegalArgumentException("invalid intake outcome");
        }
    }

    /** Never includes private paths, captures, UUIDs, public events or original histories. */
    public record Report(String schema, long seed, List<Status> intakeStatuses,
                         Phase7Corpus.Report corpus) {
        public Report {
            if(!SCHEMA.equals(schema))throw new IllegalArgumentException("invalid intake schema");
            intakeStatuses=List.copyOf(intakeStatuses);
            Objects.requireNonNull(corpus);
        }
        public String json() {
            JsonObject out=new JsonObject();
            out.addProperty("schema",schema);
            out.addProperty("seed",seed);
            out.addProperty("scope","verified-custody-action-agreement-only;no-win-rate;not-minecraft");
            JsonArray status=new JsonArray();
            int preverified=0;
            for(Status x:intakeStatuses) {
                status.add(x.name());
                if(x==Status.VERIFIED_FOR_CORPUS)preverified++;
            }
            out.add("intakeStatuses",status);
            JsonObject summary=new JsonObject();
            summary.addProperty("attempted",intakeStatuses.size());
            summary.addProperty("signatureAndBundleVerified",preverified);
            summary.addProperty("intakeRejected",intakeStatuses.size()-preverified);
            summary.addProperty("corpusAccepted",corpus.coverage().accepted());
            summary.addProperty("corpusRejected",corpus.coverage().rejected());
            out.add("summary",summary);
            out.add("corpus",JsonParser.parseString(corpus.json()));
            return out.toString();
        }
    }

    /**
     * A local-only batch. Input slugs name directories beneath ignored
     * run/phase7-corpus/private-input/. The pinned trust key lives separately
     * in run/phase7-corpus/trust/<keyId>.pub (base64 SPKI DER).
     * expectedKeySha256 MUST come from an independent trusted channel, NOT
     * from the manifest or signature metadata.
     *
     * Distinct root paths are never accepted as arbitrary bundle overrides.
     */
    public static Report evaluateLocal(Path repoRoot, List<String> bundleNames,
            Map<String,Phase7Benchmark.Scenario> trustedScenarios, String keyId,
            String expectedKeySha256, long seed, Phase7Benchmark.DecisionPolicy shadow) {
        Objects.requireNonNull(repoRoot);
        Objects.requireNonNull(bundleNames);
        Objects.requireNonNull(trustedScenarios);
        if(bundleNames.size()>MAX_ENTRIES)
            throw new IllegalArgumentException("too many corpus entries");
        if(!validName(keyId) || !validHash(expectedKeySha256))
            throw new IllegalArgumentException("trusted key fingerprint must be independently pinned");
        var sorted=new ArrayList<>(bundleNames);
        for(String name:sorted) if(!validName(name))
            throw new IllegalArgumentException("invalid local bundle name");
        Collections.sort(sorted);
        var counts=new HashMap<String,Integer>();
        for(String name:sorted)counts.merge(name,1,Integer::sum);
        var statuses=new ArrayList<Status>();
        var admitted=new ArrayList<Phase7Corpus.Submission>();
        var bindings=new HashMap<String,Phase7LegacyImport.ReviewRequest>();
        Path base=repoRoot.toAbsolutePath().normalize();
        PublicKey key=null;
        try {
            Path trust=safeDirectory(base,"run","phase7-corpus","trust");
            byte[] der=Base64.getDecoder().decode(
                    readPrivate(trust.resolve(keyId+".pub"),4_096).trim());
            if(der.length>1_024 || !hex(digest(der)).equals(expectedKeySha256))
                throw new IllegalArgumentException("untrusted key fingerprint");
            key=KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
        } catch(Exception unavailable) { /* Every entry will reject with a fixed status. */ }
        for(String name:sorted) {
            if(counts.get(name)>1) {
                statuses.add(Status.DUPLICATE_INPUT);
                continue;
            }
            if(key==null) {
                statuses.add(Status.TRUST_KEY_INVALID);
                continue;
            }
            Attempt attempt=load(base,name,keyId,key,trustedScenarios);
            statuses.add(attempt.status());
            if(attempt.loaded()!=null) {
                admitted.add(attempt.loaded().submission());
                var bound=attempt.loaded().binding();
                // Any repeated ID remains rejected by Phase7Corpus, never first-wins.
                bindings.put(bound.captureId(),bound);
            }
        }
        Phase7LegacyImport.CustodyReviewer reviewer=request->
                Objects.equals(bindings.get(request.captureId()),request);
        Phase7Corpus.Report corpus=Phase7Corpus.evaluate(admitted,seed,reviewer,shadow);
        return new Report(SCHEMA,seed,statuses,corpus);
    }

    private static Attempt reject(Status status){return new Attempt(status,null);}

    private static Attempt load(Path root,String bundleName,String keyId,PublicKey trustedKey,
                                Map<String,Phase7Benchmark.Scenario> scenarios) {
        try {
            Path dir=safeDirectory(root,"run","phase7-corpus","private-input",bundleName);
            String manifestText=readPrivate(dir.resolve("manifest.json"),MAX_MANIFEST_BYTES);
            String custodyText=readPrivate(dir.resolve("custody.json"),MAX_SIGNATURE_BYTES);
            JsonObject signed=StrictOwnJson.read(custodyText);
            only(signed,"schema","keyId","manifestSha256","signature");
            if(!CUSTODY.equals(str(signed,"schema")) || !keyId.equals(str(signed,"keyId")))
                return reject(Status.SIGNATURE_INVALID);
            String manifestHash=hex(digest(manifestText.getBytes(StandardCharsets.UTF_8)));
            if(!manifestHash.equals(str(signed,"manifestSha256")))
                return reject(Status.SIGNATURE_INVALID);
            byte[] signature=Base64.getDecoder().decode(str(signed,"signature"));
            if(signature.length!=64) return reject(Status.SIGNATURE_INVALID);
            Signature verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(trustedKey);
            verifier.update(signaturePayload(manifestHash));
            if(!verifier.verify(signature))return reject(Status.SIGNATURE_INVALID);

            JsonObject manifest=StrictOwnJson.read(manifestText);
            only(manifest,"schema","scenarioId","scenarioFingerprint","captureId",
                    "battleId","turn","action","artifactSha256","ownRequestSha256",
                    "publicSha256","rawHistorySha256","choiceReceiptSha256","checkpoints");
            if(!SCHEMA.equals(str(manifest,"schema")))return reject(Status.MANIFEST_INVALID);
            String scenarioId=str(manifest,"scenarioId");
            if(!validName(scenarioId))return reject(Status.MANIFEST_INVALID);
            Phase7Benchmark.Scenario scenario=scenarios.get(scenarioId);
            if(scenario==null)return reject(Status.SCENARIO_UNAVAILABLE);
            String fp=Phase7Benchmark.stateFingerprint(scenario);
            if(!fp.equals(str(manifest,"scenarioFingerprint")))
                return reject(Status.SCENARIO_UNAVAILABLE);
            String captureId=str(manifest,"captureId");
            String artifact=str(manifest,"artifactSha256");
            String own=str(manifest,"ownRequestSha256");
            String pub=str(manifest,"publicSha256");
            String action=str(manifest,"action");
            int turn=positiveInt(manifest,"turn");
            if(!validHash(artifact)||!validHash(own)||!validHash(pub)
                    ||!validHash(str(manifest,"rawHistorySha256")))
                return reject(Status.MANIFEST_INVALID);
            var receipt=new Phase7LegacyImport.ChoiceReceipt(captureId,
                    "LEGACY_CHOOSE_RETURN",artifact,own,pub,fp,turn,action);
            UUID battle=UUID.fromString(str(manifest,"battleId"));
            String choiceHash=Phase7LegacyImport.sha256(List.of(
                    receipt.origin(),artifact,own,pub,fp,Integer.toString(turn),action,captureId));
            if(!choiceHash.equals(str(manifest,"choiceReceiptSha256")))
                return reject(Status.MANIFEST_INVALID);

            JsonArray checkpointArray=manifest.getAsJsonArray("checkpoints");
            if(checkpointArray.size()<1||checkpointArray.size()>32)
                return reject(Status.CHECKPOINT_INVALID);
            List<List<String>> checkpoints=new ArrayList<>();
            int i=1;
            for(JsonElement x:checkpointArray) {
                JsonObject declaration=x.getAsJsonObject();
                only(declaration,"file","sha256");
                String filename=String.format(Locale.ROOT,"checkpoint-%02d.json",i++);
                if(!filename.equals(str(declaration,"file")))
                    return reject(Status.CHECKPOINT_INVALID);
                String privateCheckpoint=readPrivate(dir.resolve(filename),MAX_CHECKPOINT_BYTES);
                if(!hex(digest(privateCheckpoint.getBytes(StandardCharsets.UTF_8)))
                        .equals(str(declaration,"sha256")))
                    return reject(Status.CHECKPOINT_INVALID);
                JsonObject checkpoint=StrictOwnJson.read(privateCheckpoint);
                only(checkpoint,"chunks");
                JsonArray chunks=checkpoint.getAsJsonArray("chunks");
                if(chunks.isEmpty() || chunks.size()>512)return reject(Status.CHECKPOINT_INVALID);
                List<String> raw=new ArrayList<>();
                for(JsonElement chunk:chunks)raw.add(chunk.getAsString());
                checkpoints.add(List.copyOf(raw));
            }
            List<String> last=checkpoints.getLast();
            if(!Phase7LegacyImport.sha256(last).equals(str(manifest,"rawHistorySha256")))
                return reject(Status.CHECKPOINT_INVALID);
            var capture=new Phase7LegacyImport.Capture(battle,last,receipt);
            var submission=new Phase7Corpus.Submission(scenario,capture,checkpoints);
            var binding=new Phase7LegacyImport.ReviewRequest(captureId,artifact,
                    str(manifest,"rawHistorySha256"),own,pub,choiceHash);
            return new Attempt(Status.VERIFIED_FOR_CORPUS,new Loaded(submission,binding));
        } catch(NoSuchFileException missing) {
            return reject(Status.CUSTODY_UNAVAILABLE);
        } catch(IOException unsafe) {
            return reject(Status.PATH_UNSAFE);
        } catch(GeneralSecurityException signatureInvalid) {
            return reject(Status.SIGNATURE_INVALID);
        } catch(RuntimeException invalid) {
            return reject(Status.MANIFEST_INVALID);
        }
    }

    /** Public for an independently authorized reviewer to sign OFFLINE.
     * This is only a canonical message constructor, never a signing service.
     */
    public static byte[] signaturePayload(String manifestSha256) {
        if(!validHash(manifestSha256))throw new IllegalArgumentException("bad manifest SHA");
        return (DOMAIN+manifestSha256+"\n").getBytes(StandardCharsets.UTF_8);
    }

    /** Restrict input to the repo-ignored corpus directories; no symbolic links. */
    private static Path safeDirectory(Path root,String... parts) throws IOException {
        Path current=root;
        if(!Files.isDirectory(current,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(current))
            throw new IOException("unsafe root");
        for(String part:parts) {
            current=current.resolve(part);
            if(!Files.exists(current,LinkOption.NOFOLLOW_LINKS))
                throw new NoSuchFileException("missing private location");
            if(Files.isSymbolicLink(current) || !Files.isDirectory(current,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("unsafe local path");
        }
        return current;
    }
    private static String readPrivate(Path path,int max) throws IOException {
        if(Files.isSymbolicLink(path) || !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)
                || Files.size(path)>max)
            throw new IOException("unavailable private file");
        try(var stream=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)) {
            byte[] data=stream.readNBytes(max+1);
            if(data.length>max)throw new IOException("oversize private file");
            // The original is in memory only and never logged.
            return new String(data,StandardCharsets.UTF_8);
        }
    }
    private static void only(JsonObject obj,String... fields) {
        if(!obj.keySet().equals(Set.of(fields)))
            throw new IllegalArgumentException("unknown/missing field");
    }
    private static String str(JsonObject obj,String key) {
        JsonElement v=obj.get(key);
        if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid field");
        return v.getAsString();
    }
    private static int positiveInt(JsonObject obj,String key) {
        JsonElement el=obj.get(key);
        if(el==null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("bad turn");
        String raw=el.getAsString();
        if(!raw.matches("[1-9][0-9]{0,8}"))throw new IllegalArgumentException("bad turn");
        return Integer.parseInt(raw);
    }
    private static boolean validName(String s){return s!=null&&NAME.matcher(s).matches();}
    private static boolean validHash(String s){return s!=null&&HASH.matcher(s).matches();}
    private static byte[] digest(byte[] bytes) {
        try {return MessageDigest.getInstance("SHA-256").digest(bytes);}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String hex(byte[] bytes){return HexFormat.of().formatHex(bytes);}
}
