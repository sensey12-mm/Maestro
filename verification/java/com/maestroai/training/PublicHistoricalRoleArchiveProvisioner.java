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
        try{
            if(trustedEpochSecond<1 || stateDirectory==null
                    || !stateDirectory.isAbsolute()
                    || Files.isSymbolicLink(stateDirectory)
                    || !Files.isDirectory(stateDirectory,LinkOption.NOFOLLOW_LINKS)
                    || advance!=(externalPriorCheckpoint!=null)
                    || independentRootFile==null || signedManifestFile==null
                    || independentRootFile.equals(signedManifestFile)
                    || (externalPriorCheckpoint!=null
                        && (externalPriorCheckpoint.equals(independentRootFile)
                            || externalPriorCheckpoint.equals(signedManifestFile))))
                throw denied();
            var root=root(load(independentRootFile,stateDirectory));
            var manifest=signed(load(signedManifestFile,stateDirectory));
            if(!advance)
                return PublicCustodyTrustStore.initialize(stateDirectory,root,
                        manifest,trustedEpochSecond);
            var pinned=checkpoint(load(externalPriorCheckpoint,stateDirectory));
            // No key rotation performed here: a rotated keyring requires a
            // separate, explicitly authenticated update, never guessed.
            return PublicCustodyTrustStore.advance(stateDirectory,root,pinned,
                    List.of(),manifest,trustedEpochSecond);
        }catch(RuntimeException ex){throw denied();}
    }

    /**
     * Usage: init STATE_DIR ROOT_FILE SIGNED_MANIFEST TRUSTED_EPOCH
     *    or: advance STATE_DIR ROOT_FILE SIGNED_MANIFEST PRIOR_PIN TRUSTED_EPOCH
     * No private information included in errors or the successful receipt.
     */
    public static void main(String[] args){
        try{
            if(args.length!=5 && args.length!=6)throw denied();
            boolean advance;
            if("init".equals(args[0]))advance=false;
            else if("advance".equals(args[0]))advance=true;
            else throw denied();
            if(advance!=(args.length==6))throw denied();
            var newPin=importSigned(advance,Path.of(args[1]),Path.of(args[2]),
                    Path.of(args[3]),advance?Path.of(args[4]):null,
                    positive(args[advance?5:4]));
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
