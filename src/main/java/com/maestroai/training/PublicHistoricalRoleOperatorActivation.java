package com.maestroai.training;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Explicit, operator-only installation bridge for historical Phase 5 diagnostics.
 * The signed custody journal and replay file are NOT trust roots. The bootstrap
 * public key and exact state checkpoint must come from SEPARATE launch arguments.
 * A local clock is not a cryptographic time attestation: the operator is
 * responsible for independently validating its accuracy and rollback controls.
 *
 * Nothing here chooses an action, supplies an opponent identity, or promotes Shadow.
 */
public final class PublicHistoricalRoleOperatorActivation {
    private PublicHistoricalRoleOperatorActivation() {}
    private static final String PREFIX="maestroai.phase5Archive.";
    public record Installed(PublicHistoricalRoleLiveOverlay.Archive archive,
                            PublicHistoricalRoleLiveOverlay.TrustedSource trusted) {
        public Installed {
            if(archive==null || trusted==null)throw invalid();
        }
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 5 historical operator provisioning unavailable");
    }
    private static String required(Properties p,String key){
        String value=p.getProperty(PREFIX+key);
        if(value==null || value.isBlank() || value.length()>2048)throw invalid();
        return value;
    }
    private static long number(Properties p,String key){
        try{
            String value=required(p,key);
            if(!value.matches("[1-9][0-9]{0,17}"))throw invalid();
            return Long.parseLong(value);
        }catch(NumberFormatException ex){throw invalid();}
    }
    private static Path absolute(Properties p,String key){
        Path path=Path.of(required(p,key));
        if(!path.isAbsolute() || path.toString().length()>1024)throw invalid();
        return path.normalize();
    }
    private static List<PublicBuildCandidateFilter.Candidate> candidates(String values){
        if(values.length()>1500)throw invalid();
        String[] entries=values.split(",",-1);
        if(entries.length<1 || entries.length>20)throw invalid();
        var result=new ArrayList<PublicBuildCandidateFilter.Candidate>();
        for(String entry:entries){
            String[] fields=entry.split(":",-1);
            if(fields.length!=3 || !fields[0].matches("[1-9][0-9]{0,3}")
                    || !fields[1].matches("[a-z0-9]{1,40}")
                    || !fields[2].matches("[a-z0-9]{1,40}"))throw invalid();
            result.add(new PublicBuildCandidateFilter.Candidate(
                    Integer.parseInt(fields[0]),fields[1],Set.of(fields[2]),
                    Set.of(),Set.of(),Set.of()));
        }
        return List.copyOf(result);
    }

    /** Returns null when deliberately unconfigured; partial/invalid installations
     * reject. For exact on-disk trust-state preparation, use
     * PublicCustodyTrustStore.initialize/advance in an OFFLINE operator step.
     */
    public static Installed fromProperties(Properties p,LongSupplier operatorClock) {
        if(p==null || operatorClock==null)throw invalid();
        if(p.getProperty(PREFIX+"directory")==null){
            for(String key:p.stringPropertyNames())
                if(key.startsWith(PREFIX))throw invalid();
            return null;
        }
        if(!"true".equals(required(p,"replayRightsIndependentlyApproved"))
                || !"true".equals(required(p,"operatorVerifiedHostClock")))
            throw invalid();
        try{
            Path directory=absolute(p,"directory");
            Path replay=absolute(p,"replayLog");
            if(!Files.isRegularFile(replay,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(replay)>2_000_000 || Files.size(replay)<1)
                throw invalid();
            String spectatorLog=Files.readString(replay,StandardCharsets.UTF_8);
            String replayId=required(p,"replayId");
            String format=required(p,"format");
            var source=new PublicReplayCorpus.Source(replayId,format,true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var proposals=candidates(required(p,"roleQueries"));
            String encoded=required(p,"bootstrapEd25519Spki");
            var bootstrap=new PublicSignedCustody.TrustedPolicy(
                    number(p,"bootstrapKeyringEpoch"),
                    number(p,"bootstrapKeyringEpoch"),
                    number(p,"bootstrapMinimumRevision"),
                    number(p,"bootstrapEarliestSignature"),
                    p.getProperty(PREFIX+"bootstrapManifestDigest",""),
                    List.of(new PublicSignedCustody.TrustedKey(
                            required(p,"bootstrapKeyId"),encoded,
                            number(p,"bootstrapKeyActiveFrom"),0)));
            var pinned=new PublicCustodyTrustStore.Checkpoint(
                    required(p,"checkpointStateSha256"),
                    number(p,"checkpointRevision"),
                    number(p,"checkpointKeyringEpoch"));
            long operatorNotBefore=number(p,"operatorVerifiedTimeFloor");
            var lastEpoch=new AtomicLong(operatorNotBefore);
            PublicHistoricalRoleLiveOverlay.TrustedSource refreshed=()->{
                try{
                    long now=operatorClock.getAsLong();
                    if(now<operatorNotBefore || now<lastEpoch.get())
                        throw invalid();
                    var resolved=PublicCustodyTrustStore.resolve(
                            directory,bootstrap,pinned,now);
                    PublicSignedCustody.verify(resolved.manifest(),resolved.policy(),now);
                    lastEpoch.accumulateAndGet(now,Math::max);
                    return new PublicHistoricalRoleLiveOverlay.CustodySnapshot(
                            resolved.manifest(),resolved.policy(),now);
                }catch(RuntimeException ex){throw invalid();}
            };
            var initial=refreshed.latest();
            var archive=PublicHistoricalRoleLiveOverlay.prepare(
                    source,spectatorLog,proposals,List.of(),Set.of(),
                    initial.signed(),initial.policy(),initial.currentEpochSecond());
            return new Installed(archive,refreshed);
        }catch(IOException | RuntimeException ex){throw invalid();}
    }

    /**
     * The JAR can load a separately provisioned archive without any third-party
     * injection. An operator must verify the local system clock out-of-band;
     * a property accepting it is an assertion, NOT an independent time proof.
     */
    public static Installed fromJvmProperties(){
        return fromProperties(System.getProperties(),()->Instant.now().getEpochSecond());
    }
}
