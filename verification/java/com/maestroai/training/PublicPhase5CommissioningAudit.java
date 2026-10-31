package com.maestroai.training;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.jar.JarFile;

/**
 * One read-only LOCAL commissioning audit of independently pinned CI JAR
 * bytes, signed custody history, operator replay and launch configuration.
 * NEVER signs, downloads, writes state, creates permissions or touches Legacy.
 * A passing local audit does NOT attest GitHub Actions success, signer/replay
 * ownership, clock accuracy or Minecraft acceptance.
 */
public final class PublicPhase5CommissioningAudit {
    private PublicPhase5CommissioningAudit(){}
    public enum Verdict {
        MISSING_JAR,
        JAR_PIN_MISSING_OR_INVALID,
        JAR_MISMATCH,
        OPERATOR_CONFIGURATION_MISSING,
        OBSERVER_DISABLED,
        INDEPENDENT_TRUST_PIN_MISSING,
        TRUST_ANCHORS_MISMATCH,
        CLOCK_ASSERTION_INVALID,
        SIGNED_CUSTODY_REJECTED,
        HISTORICAL_SOURCE_REJECTED,
        LOCALLY_READY_FOR_MINECRAFT_TEST
    }
    public record Report(Verdict verdict) {
        public Report {
            if(verdict==null)throw new IllegalArgumentException("Invalid audit verdict");
        }
        public boolean locallyReady(){return verdict==Verdict.LOCALLY_READY_FOR_MINECRAFT_TEST;}
        @Override public String toString(){
            return "Phase5Commissioning[verdict="+verdict
                    +";ciSuccess=EXTERNALLY_VERIFY;archiveRights=EXTERNALLY_VERIFY"
                    +";clockTrust=EXTERNALLY_VERIFY;identity=UNBOUND"
                    +";minecraft=UNTESTED;legacy=AUTHORITATIVE;authority=NONE]";
        }
    }
    private static final String PREFIX="maestroai.phase5Archive.";
    private static final Set<String> BOOTSTRAP=Set.of("bootstrapKeyId",
            "bootstrapEd25519Spki","bootstrapKeyActiveFrom",
            "bootstrapKeyringEpoch","bootstrapMinimumRevision",
            "bootstrapEarliestSignature");
    private static final Set<String> CHECKPOINT=Set.of(
            "checkpointStateSha256","checkpointRevision","checkpointKeyringEpoch");
    private static final Set<String> JAR_PIN=Set.of("sourceCommitSha","jarSha256","jarVersion");
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 5 local commissioning input rejected");
    }
    private static String required(Map<String,String> fields,String key){
        String v=fields.get(key);
        if(v==null || v.isBlank() || v.length()>2048)throw invalid();
        return v;
    }
    private static long positive(String value){
        try {
            if(!value.matches("[1-9][0-9]{0,17}"))throw invalid();
            return Long.parseLong(value);
        }catch(NumberFormatException ex){throw invalid();}
    }
    /** Strict bounded UTF-8 flat properties. No Java Properties escaping or duplicates. */
    private static Map<String,String> load(Path file){
        try{
            if(file==null || !file.isAbsolute() || Files.isSymbolicLink(file)
                    || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file)<1 || Files.size(file)>65_536)
                throw invalid();
            String raw=Files.readString(file,StandardCharsets.UTF_8);
            if(raw.indexOf('\uFFFD')>=0 || raw.indexOf('\0')>=0)throw invalid();
            var fields=new HashMap<String,String>();
            for(String line:raw.split("\n",-1)){
                if(line.endsWith("\r"))line=line.substring(0,line.length()-1);
                if(line.isEmpty() || line.startsWith("#"))continue;
                int at=line.indexOf('=');
                if(at<1 || at>100 || line.length()>4096)throw invalid();
                String key=line.substring(0,at);
                String value=line.substring(at+1);
                if(!key.matches("[A-Za-z][A-Za-z0-9.]*")
                        || fields.putIfAbsent(key,value)!=null)
                    throw invalid();
            }
            return Map.copyOf(fields);
        }catch(IOException | RuntimeException rejected){throw invalid();}
    }
    private static void exact(Map<String,String> fields,Set<String> allowed){
        if(!fields.keySet().equals(allowed))throw invalid();
    }
    private static void verifyJar(Path jar,Map<String,String> pin){
        try{
            exact(pin,JAR_PIN);
            String version=required(pin,"jarVersion");
            if(!version.matches("[1-9][0-9]*\\.[0-9]+\\.[0-9]+")
                    || !required(pin,"sourceCommitSha").matches("[a-f0-9]{40}")
                    || !required(pin,"jarSha256").matches("[a-f0-9]{64}")
                    || jar==null || !jar.isAbsolute()
                    || Files.isSymbolicLink(jar)
                    || !Files.isRegularFile(jar,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(jar)<100 || Files.size(jar)>100_000_000L
                    || !jar.getFileName().toString().equals("maestroai-"+version+".jar"))
                throw invalid();
            byte[] bytes=Files.readAllBytes(jar);
            String actual=HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
            if(!actual.equals(pin.get("jarSha256")))throw invalid();
            try(JarFile archive=new JarFile(jar.toFile())){
                var metadata=archive.getJarEntry("fabric.mod.json");
                if(metadata==null || metadata.getSize()<1 || metadata.getSize()>64_000)
                    throw invalid();
                String json=new String(archive.getInputStream(metadata).readAllBytes(),
                        StandardCharsets.UTF_8);
                if(!json.matches("(?s).*\\\"id\\\"\\s*:\\s*\\\"maestroai\\\".*")
                        || !json.matches("(?s).*\\\"version\\\"\\s*:\\s*\\\""
                                +java.util.regex.Pattern.quote(version)+"\\\".*")
                        || archive.getJarEntry("com/maestroai/MaestroAI.class")==null)
                    throw invalid();
            }
        }catch(IOException | NoSuchAlgorithmException | RuntimeException ex){
            throw invalid();
        }
    }
    private static Map<String,String> withPrefix(
            Map<String,String> map,Set<String> fields){
        var result=new HashMap<String,String>();
        for(String key:fields)result.put(key,required(map,PREFIX+key));
        return result;
    }
    private static void anchors(Path state,Path ciPin,Path operator,
                                Path root,Path checkpoint,Path jar,Map<String,String> settings,
                                Map<String,String> rootValues,Map<String,String> pinValues){
        try{
            if(state==null || !state.isAbsolute() || Files.isSymbolicLink(state)
                    || !Files.isDirectory(state,LinkOption.NOFOLLOW_LINKS))
                throw invalid();
            // The ROOT and latest CHECKPOINT must come from files distinct
            // from both the mutable state and the operator configuration.
            var inputs=Set.of(ciPin,operator,root,checkpoint,jar);
            if(inputs.size()!=5)throw invalid();
            var unique=new HashSet<Path>();
            Path actualState=state.toRealPath();
            for(Path p:inputs){
                if(!p.isAbsolute() || Files.isSymbolicLink(p)
                        || !unique.add(p.toRealPath())
                        || p.toRealPath().startsWith(actualState))
                    throw invalid();
            }
            if(!rootValues.keySet().equals(BOOTSTRAP)
                    && !rootValues.keySet().equals(withOptional(BOOTSTRAP,
                            "bootstrapManifestDigest")))
                throw invalid();
            exact(pinValues,CHECKPOINT);
            for(String key:BOOTSTRAP)
                if(!required(rootValues,key).equals(required(settings,PREFIX+key)))
                    throw invalid();
            if(!rootValues.getOrDefault("bootstrapManifestDigest","").equals(
                    settings.getOrDefault(PREFIX+"bootstrapManifestDigest","")))
                throw invalid();
            for(String key:CHECKPOINT)
                if(!required(pinValues,key).equals(required(settings,PREFIX+key)))
                    throw invalid();
        }catch(IOException | RuntimeException ex){throw invalid();}
    }
    private static Set<String> withOptional(Set<String> original,String optional){
        var keys=new HashSet<>(original);keys.add(optional);return Set.copyOf(keys);
    }
    private static PublicSignedCustody.TrustedPolicy root(Map<String,String> data){
        return new PublicSignedCustody.TrustedPolicy(
                positive(required(data,"bootstrapKeyringEpoch")),
                positive(required(data,"bootstrapKeyringEpoch")),
                positive(required(data,"bootstrapMinimumRevision")),
                positive(required(data,"bootstrapEarliestSignature")),
                data.getOrDefault("bootstrapManifestDigest",""),
                java.util.List.of(new PublicSignedCustody.TrustedKey(
                        required(data,"bootstrapKeyId"),
                        required(data,"bootstrapEd25519Spki"),
                        positive(required(data,"bootstrapKeyActiveFrom")),0)));
    }
    private static PublicCustodyTrustStore.Checkpoint checkpoint(
            Map<String,String> data){
        return new PublicCustodyTrustStore.Checkpoint(
                required(data,"checkpointStateSha256"),
                positive(required(data,"checkpointRevision")),
                positive(required(data,"checkpointKeyringEpoch")));
    }
    /**
     * All paths are absolute local inputs. Checkpoint/root/CI pins MUST
     * originate independently; neither is derivable from untrusted state.
     * Do not call recover(), which can delete stale pending journal files.
     */
    public static Report inspect(Path jar,Path independentJarPin,
                                 Path operatorFile,Path independentRoot,
                                 Path independentCheckpoint,LongSupplier clock){
        if(clock==null)throw invalid();
        if(jar==null || !Files.isRegularFile(jar,LinkOption.NOFOLLOW_LINKS))
            return new Report(Verdict.MISSING_JAR);
        Map<String,String> jarPins;
        try{jarPins=load(independentJarPin);}
        catch(RuntimeException ex){return new Report(Verdict.JAR_PIN_MISSING_OR_INVALID);}
        try{verifyJar(jar,jarPins);}
        catch(RuntimeException ex){return new Report(Verdict.JAR_MISMATCH);}
        Map<String,String> settings;
        try{settings=load(operatorFile);}
        catch(RuntimeException ex){return new Report(Verdict.OPERATOR_CONFIGURATION_MISSING);}
        if(!"true".equals(settings.get("maestroai.phase5HistoricalRolesObserve")))
            return new Report(Verdict.OBSERVER_DISABLED);
        Map<String,String> bootstrap,pin;
        try{
            bootstrap=load(independentRoot);
            pin=load(independentCheckpoint);
        }catch(RuntimeException ex){
            return new Report(Verdict.INDEPENDENT_TRUST_PIN_MISSING);
        }
        Path state;
        try{
            state=Path.of(required(settings,PREFIX+"directory"));
            anchors(state,independentJarPin,operatorFile,independentRoot,
                    independentCheckpoint,jar,settings,bootstrap,pin);
        }catch(RuntimeException ex){
            return new Report(Verdict.TRUST_ANCHORS_MISMATCH);
        }
        long now;
        try{
            now=clock.getAsLong();
            if(now<1 || now<positive(required(settings,PREFIX+"operatorVerifiedTimeFloor"))
                    || !"true".equals(settings.get(PREFIX+"operatorVerifiedHostClock")))
                throw invalid();
        }catch(RuntimeException ex){
            return new Report(Verdict.CLOCK_ASSERTION_INVALID);
        }
        try{
            // resolve reads and verifies the complete signed keyring history,
            // original bootstrap, current signing key and exact pinned SHA.
            PublicCustodyTrustStore.resolve(state,root(bootstrap),checkpoint(pin),now);
        }catch(RuntimeException ex){
            return new Report(Verdict.SIGNED_CUSTODY_REJECTED);
        }
        try{
            var launch=new java.util.Properties();
            launch.putAll(settings);
            if(!PublicHistoricalRoleOperatorPreflight.inspect(launch,()->now)
                    .locallyProvisioned())
                return new Report(Verdict.HISTORICAL_SOURCE_REJECTED);
            return new Report(Verdict.LOCALLY_READY_FOR_MINECRAFT_TEST);
        }catch(RuntimeException ex){
            return new Report(Verdict.HISTORICAL_SOURCE_REJECTED);
        }
    }
    public static void main(String[] args){
        if(args.length!=5){
            System.err.println("Phase5Commissioning[verdict=INPUTS_UNAVAILABLE;authority=NONE]");
            System.exit(2);return;
        }
        try{
            var report=inspect(Path.of(args[0]),Path.of(args[1]),
                    Path.of(args[2]),Path.of(args[3]),Path.of(args[4]),
                    ()->Instant.now().getEpochSecond());
            System.out.println(report);
            if(!report.locallyReady())System.exit(2);
        }catch(RuntimeException ex){
            System.err.println("Phase5Commissioning[verdict=INPUTS_UNAVAILABLE;authority=NONE]");
            System.exit(2);
        }
    }
}
