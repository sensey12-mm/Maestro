package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OFFLINE import of signed, independently reviewed custody envelopes.
 * No key generation, private-key access, approval creation or game actions.
 * The independent trusted bootstrap and prior checkpoint MUST be supplied
 * externally; neither may be derived from the untrusted signed manifest.
 */
public final class PublicHistoricalRoleArchiveProvisioner {
    private PublicHistoricalRoleArchiveProvisioner() {}
    private static IllegalArgumentException denied(){
        return new IllegalArgumentException("Phase 5 offline archive import rejected");
    }
    private static long positive(String value){
        try {
            if(value==null || !value.matches("[1-9][0-9]{0,17}"))throw denied();
            return Long.parseLong(value);
        }catch(NumberFormatException e){throw denied();}
    }
    private static int bounded(String value,int max){
        long number=positive(value);
        if(number>max)throw denied();
        return (int)number;
    }
    private static String required(Map<String,String> fields,String key){
        String value=fields.get(key);
        if(value==null || value.isBlank() || value.length()>2048)throw denied();
        return value;
    }
    private static String optional(Map<String,String> fields,String key){
        return fields.getOrDefault(key,"");
    }
    /** Only simple canonical ASCII key=value lines, never duplicate/escaped keys. */
    private static Map<String,String> load(Path file,Path stateDirectory){
        try{
            if(file==null || !file.isAbsolute() || file.equals(stateDirectory)
                    || Files.isSymbolicLink(file)
                    || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file)<1 || Files.size(file)>128_000
                    || file.toRealPath().startsWith(stateDirectory.toRealPath()))
                throw denied();
            String raw=Files.readString(file,StandardCharsets.UTF_8);
            if(raw.length()>128_000 || raw.indexOf('\uFFFD')>=0)throw denied();
            var result=new HashMap<String,String>();
            for(String line:raw.split("\n",-1)){
                if(line.endsWith("\r"))line=line.substring(0,line.length()-1);
                if(line.isEmpty() || line.startsWith("#"))continue;
                int separator=line.indexOf('=');
                if(separator<1 || separator>80 || line.length()>4096)
                    throw denied();
                String key=line.substring(0,separator);
                String value=line.substring(separator+1);
                if(!key.matches("[A-Za-z][A-Za-z0-9.]*")
                        || value.indexOf('\0')>=0 || result.putIfAbsent(key,value)!=null)
                    throw denied();
            }
            if(result.isEmpty())throw denied();
            return Map.copyOf(result);
        }catch(IOException | RuntimeException ex){
            throw denied();
        }
    }
    private static void exact(Map<String,String> got,Set<String> accepted){
        if(!accepted.containsAll(got.keySet()))throw denied();
    }
    private static PublicSignedCustody.TrustedPolicy root(Map<String,String> fields){
        exact(fields,Set.of("bootstrapKeyId","bootstrapEd25519Spki",
                "bootstrapKeyActiveFrom","bootstrapKeyringEpoch","bootstrapMinimumRevision",
                "bootstrapEarliestSignature","bootstrapManifestDigest"));
        var policy=new PublicSignedCustody.TrustedPolicy(
                positive(required(fields,"bootstrapKeyringEpoch")),
                positive(required(fields,"bootstrapKeyringEpoch")),
                positive(required(fields,"bootstrapMinimumRevision")),
                positive(required(fields,"bootstrapEarliestSignature")),
                optional(fields,"bootstrapManifestDigest"),
                List.of(new PublicSignedCustody.TrustedKey(
                        required(fields,"bootstrapKeyId"),
                        required(fields,"bootstrapEd25519Spki"),
                        positive(required(fields,"bootstrapKeyActiveFrom")),0)));
        return policy;
    }
    private static PublicCustodyTrustStore.Checkpoint checkpoint(Map<String,String> fields){
        exact(fields,Set.of("checkpointStateSha256","checkpointRevision",
                "checkpointKeyringEpoch"));
        return new PublicCustodyTrustStore.Checkpoint(
                required(fields,"checkpointStateSha256"),
                positive(required(fields,"checkpointRevision")),
                positive(required(fields,"checkpointKeyringEpoch")));
    }
    private static PublicSignedCustody.SignedManifest signed(Map<String,String> fields){
        int count=bounded(required(fields,"decisionCount"),10000);
        Set<String> accepted=new HashSet<>(Set.of("schemaVersion","signerKeyId",
                "signedAtEpochSecond","previousManifestDigest","signatureBase64",
                "ledgerRevision","decisionCount"));
        var decisions=new ArrayList<PublicReplayCustody.Decision>();
        for(int index=0;index<count;index++){
            String prefix="decision."+index+".";
            for(String field:List.of("replayId","format","inputSha256","status",
                    "decidedAtEpochSecond","expiresAtEpochSecond","reviewDigest"))
                accepted.add(prefix+field);
            var status=PublicReplayCustody.Status.valueOf(
                    required(fields,prefix+"status"));
            decisions.add(new PublicReplayCustody.Decision(
                    required(fields,prefix+"replayId"),
                    required(fields,prefix+"format"),
                    required(fields,prefix+"inputSha256"),status,
                    positive(required(fields,prefix+"decidedAtEpochSecond")),
                    // Zero is allowed ONLY for pending/revoked decisions.
                    zeroOrPositive(required(fields,prefix+"expiresAtEpochSecond")),
                    required(fields,prefix+"reviewDigest")));
        }
        exact(fields,accepted);
        var ledger=new PublicReplayCustody.Ledger(1,
                positive(required(fields,"ledgerRevision")),decisions);
        return new PublicSignedCustody.SignedManifest(
                bounded(required(fields,"schemaVersion"),1),
                required(fields,"signerKeyId"),
                positive(required(fields,"signedAtEpochSecond")),
                required(fields,"previousManifestDigest"),ledger,
                required(fields,"signatureBase64"));
    }
    /**
     * Transport-only externally signed keyring transition. The independent
     * bootstrap trust root MUST NOT be reconstructed from this untrusted file.
     * These are exact canonical policy fields; no signing happens here.
     */
    private static PublicCustodyTrustStore.KeyringUpdate rotation(
            Map<String,String> fields){
        int count=bounded(required(fields,"nextKeyCount"),32);
        var allowed=new HashSet<>(Set.of("signerKeyId","signedAtEpochSecond",
                "previousPolicyDigest","signatureBase64","nextKeyCount",
                "nextKeyringEpoch","nextMinimumKeyringEpoch",
                "nextMinimumRevision","nextEarliestSignature","nextManifestDigest"));
        var keys=new ArrayList<PublicSignedCustody.TrustedKey>();
        for(int i=0;i<count;i++){
            String prefix="nextKey."+i+".";
            for(String field:List.of("keyId","ed25519Spki","activeFrom",
                    "revokedAt"))
                allowed.add(prefix+field);
            keys.add(new PublicSignedCustody.TrustedKey(
                    required(fields,prefix+"keyId"),
                    required(fields,prefix+"ed25519Spki"),
                    positive(required(fields,prefix+"activeFrom")),
                    zeroOrPositive(required(fields,prefix+"revokedAt"))));
        }
        exact(fields,allowed);
        var policy=new PublicSignedCustody.TrustedPolicy(
                positive(required(fields,"nextKeyringEpoch")),
                positive(required(fields,"nextMinimumKeyringEpoch")),
                positive(required(fields,"nextMinimumRevision")),
                positive(required(fields,"nextEarliestSignature")),
                optional(fields,"nextManifestDigest"),keys);
        return new PublicCustodyTrustStore.KeyringUpdate(
                required(fields,"signerKeyId"),
                positive(required(fields,"signedAtEpochSecond")),
                required(fields,"previousPolicyDigest"),policy,
                required(fields,"signatureBase64"));
    }

    private static long zeroOrPositive(String value){
        if("0".equals(value))return 0;
        return positive(value);
    }
    /**
     * Operates on a PRE-EXISTING local state directory. Only the supplied
     * signed envelope is eligible; no automatic state lookup or approval.
     *
     * init: priorCheckpoint MUST be null.
     * advance: priorCheckpoint MUST be an external, independently pinned file.
     */
    public static PublicCustodyTrustStore.Checkpoint importSigned(
            boolean advance,Path stateDirectory,Path independentRootFile,
            Path signedManifestFile,Path externalPriorCheckpoint,long trustedEpochSecond){
        return importSigned(advance,stateDirectory,independentRootFile,
                signedManifestFile,externalPriorCheckpoint,null,trustedEpochSecond);
    }

    /**
     * A non-null signedKeyringUpdateFile enables exactly ONE authenticated
     * keyring epoch transition, together with one appended signed ledger
     * decision. It never imports trust anchors from the update's contents.
     */
    public static PublicCustodyTrustStore.Checkpoint importSigned(
            boolean advance,Path stateDirectory,Path independentRootFile,
            Path signedManifestFile,Path externalPriorCheckpoint,
            Path signedKeyringUpdateFile,long trustedEpochSecond){
        try{
            if(trustedEpochSecond<1 || stateDirectory==null
                    || !stateDirectory.isAbsolute()
                    || Files.isSymbolicLink(stateDirectory)
                    || !Files.isDirectory(stateDirectory,LinkOption.NOFOLLOW_LINKS)
                    || advance!=(externalPriorCheckpoint!=null)
                    || !advance && signedKeyringUpdateFile!=null
                    || independentRootFile==null || signedManifestFile==null)
                throw denied();
            // All input files must be independent and outside the journal.
            var files=new ArrayList<Path>();
            files.add(independentRootFile);
            files.add(signedManifestFile);
            if(externalPriorCheckpoint!=null)files.add(externalPriorCheckpoint);
            if(signedKeyringUpdateFile!=null)files.add(signedKeyringUpdateFile);
            var unique=new HashSet<Path>();
            for(Path file:files){
                if(!file.isAbsolute() || Files.isSymbolicLink(file)
                        || !unique.add(file.toRealPath()))
                    throw denied();
            }
            var bootstrap=root(load(independentRootFile,stateDirectory));
            var manifest=signed(load(signedManifestFile,stateDirectory));
            if(!advance)
                return PublicCustodyTrustStore.initialize(stateDirectory,bootstrap,
                        manifest,trustedEpochSecond);
            var pinned=checkpoint(load(externalPriorCheckpoint,stateDirectory));
            var updates=signedKeyringUpdateFile==null?List
                    .<PublicCustodyTrustStore.KeyringUpdate>of()
                    :List.of(rotation(load(signedKeyringUpdateFile,stateDirectory)));
            if(!updates.isEmpty()){
                // The update must explicitly change the key inventory or an
                // existing signer's revocation state, not merely bump an epoch.
                var previous=PublicCustodyTrustStore.validatedPolicy(
                        stateDirectory,bootstrap,pinned,trustedEpochSecond);
                var next=updates.getFirst().nextPolicy();
                boolean changed=next.keys().size()!=previous.keys().size();
                for(var old:previous.keys()){
                    var replacement=next.keys().stream()
                            .filter(key->key.keyId().equals(old.keyId()))
                            .findFirst().orElse(null);
                    if(replacement==null || replacement.revokedAtEpochSecond()
                            !=old.revokedAtEpochSecond())changed=true;
                }
                if(!changed)throw denied();
            }
            return PublicCustodyTrustStore.advance(stateDirectory,bootstrap,pinned,
                    updates,manifest,trustedEpochSecond);
        }catch(java.io.IOException | RuntimeException ex){throw denied();}
    }

    /**
     * Usage: init STATE_DIR ROOT_FILE SIGNED_MANIFEST TRUSTED_EPOCH
     *    or: advance STATE_DIR ROOT_FILE SIGNED_MANIFEST PRIOR_PIN TRUSTED_EPOCH
     *    or: rotate STATE_DIR ROOT_FILE SIGNED_MANIFEST PRIOR_PIN SIGNED_UPDATE TRUSTED_EPOCH
     * No private information included in errors or the successful receipt.
     */
    public static void main(String[] args){
        try{
            boolean rotate="rotate".equals(args.length>0?args[0]:"");
            boolean advance="advance".equals(args.length>0?args[0]:"") || rotate;
            boolean init="init".equals(args.length>0?args[0]:"");
            if(!(init && args.length==5 || !rotate && advance && args.length==6
                    || rotate && args.length==7))throw denied();
            var newPin=importSigned(advance,Path.of(args[1]),Path.of(args[2]),
                    Path.of(args[3]),advance?Path.of(args[4]):null,
                    rotate?Path.of(args[5]):null,
                    positive(args[rotate?6:advance?5:4]));
            // Export ONLY the exact checkpoint to an independent secure
            // channel. Never write it beside the untrusted journal.
            System.out.println("checkpointStateSha256="+newPin.stateSha256());
            System.out.println("checkpointRevision="+newPin.custodyRevision());
            System.out.println("checkpointKeyringEpoch="+newPin.keyringEpoch());
            System.out.println("Phase5Import[signature=VERIFIED;rights=EXTERNAL_REVIEW_REQUIRED"
                    +";minecraft=UNTESTED;authority=NONE]");
        }catch(RuntimeException rejected){
            System.err.println("Phase5Import[verdict=ABSTAIN;authority=NONE]");
            System.exit(2);
        }
    }
}
