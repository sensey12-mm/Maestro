package com.maestroai.training;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

/** Offline provisioner: synthetic signatures only, NEVER operator approvals. */
final class PublicHistoricalRoleArchiveProvisionerRegression {
    private PublicHistoricalRoleArchiveProvisionerRegression(){}
    private static int checks;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError("Archive import regression: "+why);
        checks++;
    }
    private static void reject(Runnable runnable,String why){
        try{runnable.run();}
        catch(IllegalArgumentException valid){
            check(!valid.toString().contains("SECRET_REVIEWER")
                    && !valid.toString().contains("gen9ou-99887766"),why);
            return;
        }
        throw new AssertionError("Archive import accepted "+why);
    }
    private static String asText(Map<String,String> values){
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(x->x.getKey()+"="+x.getValue()+"\n")
                .collect(Collectors.joining());
    }
    private static void put(Path file,Map<String,String> fields)throws Exception{
        Files.writeString(file,asText(fields),StandardCharsets.UTF_8);
    }
    private static String signedText(PublicSignedCustody.SignedManifest signed){
        var fields=new HashMap<String,String>();
        fields.put("schemaVersion","1");
        fields.put("signerKeyId",signed.signerKeyId());
        fields.put("signedAtEpochSecond",Long.toString(signed.signedAtEpochSecond()));
        fields.put("previousManifestDigest",signed.previousManifestDigest());
        fields.put("signatureBase64",signed.signatureBase64());
        fields.put("decisionCount",Integer.toString(signed.ledger().decisions().size()));
        fields.put("ledgerRevision",Long.toString(signed.ledger().revision()));
        for(int i=0;i<signed.ledger().decisions().size();i++){
            var d=signed.ledger().decisions().get(i);
            String prefix="decision."+i+".";
            fields.put(prefix+"replayId",d.replayId());
            fields.put(prefix+"format",d.format());
            fields.put(prefix+"inputSha256",d.inputSha256());
            fields.put(prefix+"status",d.status().name());
            fields.put(prefix+"decidedAtEpochSecond",Long.toString(d.decidedAtEpochSecond()));
            fields.put(prefix+"expiresAtEpochSecond",Long.toString(d.expiresAtEpochSecond()));
            fields.put(prefix+"reviewDigest",d.reviewDigest());
        }
        return asText(fields);
    }
    private static PublicSignedCustody.SignedManifest sign(
            KeyPair pair,PublicReplayCustody.Ledger ledger,long when,String prior)throws Exception{
        byte[] bytes=PublicSignedCustody.canonicalBytes(1,"reviewer-a",when,prior,ledger);
        var signature=Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(bytes);
        return new PublicSignedCustody.SignedManifest(1,"reviewer-a",when,
                prior,ledger,Base64.getEncoder().encodeToString(signature.sign()));
    }
    private static PublicSignedCustody.SignedManifest signAs(
            String signerId,KeyPair key,PublicReplayCustody.Ledger ledger,
            long when,String previous)throws Exception{
        byte[] bytes=PublicSignedCustody.canonicalBytes(1,signerId,when,previous,ledger);
        var signature=Signature.getInstance("Ed25519");
        signature.initSign(key.getPrivate());
        signature.update(bytes);
        return new PublicSignedCustody.SignedManifest(1,signerId,when,previous,
                ledger,Base64.getEncoder().encodeToString(signature.sign()));
    }
    private static PublicCustodyTrustStore.KeyringUpdate signRotation(
            KeyPair signer,long when,PublicSignedCustody.TrustedPolicy prior,
            PublicSignedCustody.TrustedPolicy next)throws Exception{
        String digest=PublicCustodyTrustStore.policyDigest(prior);
        byte[] bytes=PublicCustodyTrustStore.updateBytes("reviewer-a",when,
                digest,next);
        var signature=Signature.getInstance("Ed25519");
        signature.initSign(signer.getPrivate());
        signature.update(bytes);
        return new PublicCustodyTrustStore.KeyringUpdate("reviewer-a",when,
                digest,next,Base64.getEncoder().encodeToString(signature.sign()));
    }
    private static String rotationText(PublicCustodyTrustStore.KeyringUpdate update){
        var f=new HashMap<String,String>();
        f.put("signerKeyId",update.signerKeyId());
        f.put("signedAtEpochSecond",Long.toString(update.signedAtEpochSecond()));
        f.put("previousPolicyDigest",update.previousPolicyDigest());
        f.put("signatureBase64",update.signatureBase64());
        var policy=update.nextPolicy();
        f.put("nextKeyringEpoch",Long.toString(policy.keyringEpoch()));
        f.put("nextMinimumKeyringEpoch",Long.toString(policy.minimumKeyringEpoch()));
        f.put("nextMinimumRevision",Long.toString(policy.minimumRevision()));
        f.put("nextEarliestSignature",Long.toString(policy.earliestSignatureEpochSecond()));
        f.put("nextManifestDigest",policy.requiredManifestDigest());
        f.put("nextKeyCount",Integer.toString(policy.keys().size()));
        for(int i=0;i<policy.keys().size();i++){
            var k=policy.keys().get(i);
            String prefix="nextKey."+i+".";
            f.put(prefix+"keyId",k.keyId());
            f.put(prefix+"ed25519Spki",k.encodedPublicKey());
            f.put(prefix+"activeFrom",Long.toString(k.activeFromEpochSecond()));
            f.put(prefix+"revokedAt",Long.toString(k.revokedAtEpochSecond()));
        }
        return asText(f);
    }
    private static void rotationRegression()throws Exception{
        Path folder=Files.createTempDirectory("phase5-signed-keyring-").toAbsolutePath();
        try{
            var directory=Files.createDirectory(folder.resolve("state"));
            Path root=folder.resolve("independent-root.properties");
            Path pinFile=folder.resolve("independent-checkpoint.properties");
            Path firstFile=folder.resolve("first-signed.properties");
            Path successor=folder.resolve("rotated-signed.properties");
            Path rotation=folder.resolve("signed-keyring-update.properties");
            Path poisoned=folder.resolve("poisoned-update.properties");
            var alice=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var bob=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var imposter=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            long now=1_700_001_000L;
            String oldSpki=Base64.getEncoder().encodeToString(
                    alice.getPublic().getEncoded());
            String newSpki=Base64.getEncoder().encodeToString(
                    bob.getPublic().getEncoded());
            put(root,Map.of("bootstrapKeyId","reviewer-a",
                    "bootstrapEd25519Spki",oldSpki,
                    "bootstrapKeyActiveFrom",Long.toString(now-100),
                    "bootstrapKeyringEpoch","1",
                    "bootstrapMinimumRevision","1",
                    "bootstrapEarliestSignature",Long.toString(now-100)));
            var previous=new PublicSignedCustody.TrustedPolicy(1,1,1,now-100,"",
                    List.of(new PublicSignedCustody.TrustedKey(
                            "reviewer-a",oldSpki,now-100,0)));
            var firstDecision=new PublicReplayCustody.Decision("gen9ou-99887766",
                    "gen9ou","a".repeat(64),PublicReplayCustody.Status.APPROVED,
                    now-80,now+800,"b".repeat(64));
            var firstLedger=new PublicReplayCustody.Ledger(1,1,List.of(firstDecision));
            var first=signAs("reviewer-a",alice,firstLedger,now-40,"0".repeat(64));
            Files.writeString(firstFile,signedText(first));
            var pin=PublicHistoricalRoleArchiveProvisioner.importSigned(
                    false,directory,root,firstFile,null,now);
            put(pinFile,Map.of("checkpointStateSha256",pin.stateSha256(),
                    "checkpointRevision","1","checkpointKeyringEpoch","1"));
            var secondDecision=new PublicReplayCustody.Decision("gen9ou-99887767",
                    "gen9ou","c".repeat(64),PublicReplayCustody.Status.APPROVED,
                    now-20,now+800,"d".repeat(64));
            var nextLedger=new PublicReplayCustody.Ledger(1,2,
                    List.of(firstDecision,secondDecision));
            // Sign the KEYRING UPDATE with the prior active reviewer and
            // the SUCCESSOR MANIFEST with the new independent reviewer.
            var nextPolicy=new PublicSignedCustody.TrustedPolicy(2,2,2,now-100,"",
                    List.of(new PublicSignedCustody.TrustedKey(
                                    "reviewer-a",oldSpki,now-100,now-10),
                            new PublicSignedCustody.TrustedKey(
                                    "reviewer-b",newSpki,now-70,0)));
            var validUpdate=signRotation(alice,now-10,previous,nextPolicy);
            var nextManifest=signAs("reviewer-b",bob,nextLedger,now-5,
                    PublicSignedCustody.manifestDigest(first));
            Files.writeString(rotation,rotationText(validUpdate));
            Files.writeString(successor,signedText(nextManifest));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,now),
                    "keyring successor cannot import without signed update");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,null,now),
                    "explicitly missing signed update always fails closed");
            var tampered=rotationText(validUpdate).replace("nextKeyringEpoch=2",
                    "nextKeyringEpoch=3");
            Files.writeString(poisoned,tampered);
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,poisoned,now),
                    "changed signed policy epoch cannot forge reviewer authority");
            Files.writeString(poisoned,rotationText(signRotation(
                    imposter,now-10,previous,nextPolicy)));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,poisoned,now),
                    "foreign key cannot authorize keyring rotation");
            Files.writeString(poisoned,rotationText(validUpdate)+
                    "nextKey.0.keyId=reviewer-b\n");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,poisoned,now),
                    "duplicate policy fields fail parser");
            Files.writeString(poisoned,rotationText(validUpdate)
                    .replace("nextKey.0.revokedAt="+(now-10),
                            "nextKey.0.revokedAt="+(now-30)));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,poisoned,now),
                    "backdated signer revocation must fail");
            Files.writeString(poisoned,rotationText(validUpdate)
                    .replace("nextKeyringEpoch=2","nextKeyringEpoch=1"));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,poisoned,now),
                    "keyring epochs cannot be replayed");
            var oldSigned=signAs("reviewer-a",alice,nextLedger,now-5,
                    PublicSignedCustody.manifestDigest(first));
            Files.writeString(poisoned,signedText(oldSigned));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,poisoned,pinFile,rotation,now),
                    "revoked old signer cannot authorize successor manifest");
            var wrongPin=folder.resolve("wrong-prior-pin.properties");
            put(wrongPin,Map.of("checkpointStateSha256","f".repeat(64),
                    "checkpointRevision","1","checkpointKeyringEpoch","1"));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,wrongPin,rotation,now),
                    "external checkpoint mismatch rejects rotation");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,rotation,now-20),
                    "future-dated keyring/manifest cannot be imported");
            check(PublicCustodyTrustStore.recover(directory,previous,pin,now)
                    .custodyRevision()==1,
                    "rejected rotations leave original independently pinned state");
            var rotated=PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,rotation,now);
            check(rotated.custodyRevision()==2 && rotated.keyringEpoch()==2
                    && PublicCustodyTrustStore.recover(directory,previous,rotated,now)
                            .keyringEpoch()==2,
                    "signed old-to-new keyring and successor commit atomically");
            reject(()->PublicCustodyTrustStore.recover(directory,previous,pin,now),
                    "old state checkpoint cannot recover rotated journal");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,pinFile,rotation,now),
                    "replaying same signed rotation cannot roll keyring backwards");
            var revokedPin=folder.resolve("latest-pin.properties");
            put(revokedPin,Map.of("checkpointStateSha256",rotated.stateSha256(),
                    "checkpointRevision","2","checkpointKeyringEpoch","2"));
            var revokeSecond=new PublicReplayCustody.Decision("gen9ou-99887767",
                    "gen9ou","c".repeat(64),PublicReplayCustody.Status.REVOKED,
                    now+1,0,"e".repeat(64));
            var thirdLedger=new PublicReplayCustody.Ledger(1,3,
                    List.of(firstDecision,secondDecision,revokeSecond));
            var third=signAs("reviewer-b",bob,thirdLedger,now+2,
                    PublicSignedCustody.manifestDigest(nextManifest));
            Files.writeString(successor,signedText(third));
            var thirdPin=PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,directory,root,successor,revokedPin,now+3);
            check(thirdPin.custodyRevision()==3 && thirdPin.keyringEpoch()==2,
                    "new active signer can append revocation without replaying rotation");
            reject(()->PublicCustodyTrustStore.recover(directory,previous,rotated,now+3),
                    "later signed revocation invalidates prior checkpoint");
            System.out.println("Phase 5 signed keyring rotation PASS: "+checks
                    +" adversarial bootstrap/signature/revocation tests; Minecraft UNTESTED");
        }finally{delete(folder);}
    }

    private static void delete(Path dir)throws Exception{
        try(var stream=Files.walk(dir)){
            for(Path path:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
        }
    }
    static void run()throws Exception{
        Path rootDirectory=Files.createTempDirectory("phase5-offline-import-").toAbsolutePath();
        try{
            Path state=Files.createDirectory(rootDirectory.resolve("state"));
            Path root=Files.createFile(rootDirectory.resolve("independent-root.properties"));
            Path signedFile=Files.createFile(rootDirectory.resolve("signed-review.properties"));
            Path pinFile=Files.createFile(rootDirectory.resolve("external-prior-pin.properties"));
            var signer=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var attacker=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            final long time=1_700_000_100L;
            var rootFields=new HashMap<String,String>();
            rootFields.put("bootstrapKeyId","reviewer-a");
            rootFields.put("bootstrapEd25519Spki",Base64.getEncoder()
                    .encodeToString(signer.getPublic().getEncoded()));
            rootFields.put("bootstrapKeyActiveFrom",Long.toString(time-200));
            rootFields.put("bootstrapKeyringEpoch","1");
            rootFields.put("bootstrapMinimumRevision","1");
            rootFields.put("bootstrapEarliestSignature",Long.toString(time-200));
            put(root,rootFields);
            String spectator="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
                    +"|player|p1|SYNTHETIC\n|start\n"
                    +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
                    +"|switch|p2a: Test|Zoroark, L100|100/100\n"
                    +"|turn|1\n|move|p2a: Test|Nasty Plot|p1a: Ally\n|win|SYNTHETIC\n";
            Path replay=Files.createFile(rootDirectory.resolve("reviewed-spectator.log"));
            Files.writeString(replay,spectator);
            var replaySource=new PublicReplayCorpus.Source("gen9ou-99887766",
                    "gen9ou",true,PublicReplayCorpus.RightsBasis
                            .EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(replaySource,spectator);
            var decision=new PublicReplayCustody.Decision("gen9ou-99887766","gen9ou",
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    time-60,time+500,"b".repeat(64));
            var first=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var signed=sign(signer,first,time-40,"0".repeat(64));
            Files.writeString(signedFile,signedText(signed));
            var provisioned=PublicHistoricalRoleArchiveProvisioner.importSigned(
                    false,state,root,signedFile,null,time);
            check(provisioned.custodyRevision()==1 && provisioned.keyringEpoch()==1,
                    "verified initial signed import");
            var policy=new PublicSignedCustody.TrustedPolicy(1,1,1,time-200,"",
                    List.of(new PublicSignedCustody.TrustedKey("reviewer-a",
                            rootFields.get("bootstrapEd25519Spki"),time-200,0)));
            check(PublicCustodyTrustStore.recover(state,policy,provisioned,time)
                    .signedLedgerVerified(),"imported journal recoverable from external root");
            // Real integration path with SYNTHETIC signers and replay only:
            // importer -> independent pin -> unchanged runtime preflight.
            var launcher=new Properties();
            launcher.setProperty("maestroai.phase5HistoricalRolesObserve","true");
            for(var entry:rootFields.entrySet())
                launcher.setProperty("maestroai.phase5Archive."+entry.getKey(),
                        entry.getValue());
            String prefix="maestroai.phase5Archive.";
            launcher.setProperty(prefix+"directory",state.toString());
            launcher.setProperty(prefix+"replayLog",replay.toString());
            launcher.setProperty(prefix+"replayId","gen9ou-99887766");
            launcher.setProperty(prefix+"format","gen9ou");
            launcher.setProperty(prefix+"roleQueries","2:zoroark:nastyplot");
            launcher.setProperty(prefix+"replayRightsIndependentlyApproved","true");
            launcher.setProperty(prefix+"operatorVerifiedHostClock","true");
            launcher.setProperty(prefix+"operatorVerifiedTimeFloor",Long.toString(time-1));
            launcher.setProperty(prefix+"checkpointStateSha256",provisioned.stateSha256());
            launcher.setProperty(prefix+"checkpointRevision",
                    Long.toString(provisioned.custodyRevision()));
            launcher.setProperty(prefix+"checkpointKeyringEpoch",
                    Long.toString(provisioned.keyringEpoch()));
            check(PublicHistoricalRoleOperatorPreflight.inspect(launcher,()->time)
                    .locallyProvisioned(),"imported synthetic archive reaches game preflight");
            check(!PublicHistoricalRoleOperatorPreflight.inspect(launcher,()->time)
                    .minecraftAccepted(),"source preflight does not claim Minecraft acceptance");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    false,state,root,signedFile,null,time),"no overwrite on init");
            put(pinFile,Map.of("checkpointStateSha256",provisioned.stateSha256(),
                    "checkpointRevision","1","checkpointKeyringEpoch","1"));
            var revoked=new PublicReplayCustody.Decision(decision.replayId(),
                    decision.format(),decision.inputSha256(),
                    PublicReplayCustody.Status.REVOKED,time-20,0,"c".repeat(64));
            var after=new PublicReplayCustody.Ledger(1,2,List.of(decision,revoked));
            var next=sign(signer,after,time-10,PublicSignedCustody.manifestDigest(signed));
            Files.writeString(signedFile,signedText(next));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,null,time),"advance needs external pin");
            var forgedFields=new HashMap<>(rootFields);
            forgedFields.put("bootstrapEd25519Spki",Base64.getEncoder()
                    .encodeToString(attacker.getPublic().getEncoded()));
            Path badRoot=rootDirectory.resolve("forged-root.properties");
            put(badRoot,forgedFields);
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,badRoot,signedFile,pinFile,time),"forged independent key");
            Path corruptSigned=rootDirectory.resolve("corrupt-signed.properties");
            Files.writeString(corruptSigned,signedText(next)
                    .replace("status=REVOKED","status=APPROVED"));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,corruptSigned,pinFile,time),"tampered signed revocation");
            // Keep all chronology and record fields otherwise legal: only
            // the Ed25519 signature must reject this changed review digest.
            Path mutatedReview=rootDirectory.resolve("mutated-review.properties");
            Files.writeString(mutatedReview,signedText(next)
                    .replace("c".repeat(64),"d".repeat(64)));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,mutatedReview,pinFile,time),
                    "signature mismatch with otherwise valid revocation");
            Path wrongPin=rootDirectory.resolve("wrong-pin.properties");
            put(wrongPin,Map.of("checkpointStateSha256","e".repeat(64),
                    "checkpointRevision","1","checkpointKeyringEpoch","1"));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,wrongPin,time),
                    "externally incorrect checkpoint cannot advance");
            Path duplicate=rootDirectory.resolve("duplicate.properties");
            Files.writeString(duplicate,signedText(next)+"signerKeyId=reviewer-a\n");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,duplicate,pinFile,time),"ambiguous duplicate keys");
            Path internalPin=state.resolve("untrusted-pin.properties");
            Files.writeString(internalPin,Files.readString(pinFile));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,internalPin,time),
                    "checkpoint cannot originate inside journal directory");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,pinFile,time-100),
                    "future-signed successor rejected on old clock");
            check(PublicCustodyTrustStore.recover(state,policy,provisioned,time)
                    .custodyRevision()==1,"failed imports preserve original checkpoint");

            var current=PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,pinFile,time);
            check(current.custodyRevision()==2 && !current.equals(provisioned),
                    "signed append-only revocation imported");
            check(PublicCustodyTrustStore.recover(state,policy,current,time)
                    .custodyRevision()==2,"revocation journal validates");
            launcher.setProperty(prefix+"checkpointStateSha256",current.stateSha256());
            launcher.setProperty(prefix+"checkpointRevision",
                    Long.toString(current.custodyRevision()));
            check(PublicHistoricalRoleOperatorPreflight.inspect(launcher,()->time)
                    .verdict()==PublicHistoricalRoleOperatorPreflight.Verdict
                            .CUSTODY_OR_ARCHIVE_REJECTED,
                    "signed revocation withdraws game-preflight historical source");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,root,signedFile,pinFile,time),"stale pin replay fails");
            reject(()->PublicCustodyTrustStore.recover(state,policy,provisioned,time),
                    "old independent checkpoint cannot downgrade state");
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    false,state,root,signedFile,null,time),"no replay of init after advance");
            check(PublicCustodyTrustStore.recover(state,policy,current,time)
                    .signedLedgerVerified(),"failed rollback leaves latest state intact");
            var keyInside=state.resolve("fake-root.properties");
            Files.writeString(keyInside,Files.readString(root));
            reject(()->PublicHistoricalRoleArchiveProvisioner.importSigned(
                    true,state,keyInside,signedFile,pinFile,time),
                    "bootstrap from state directory is never independently anchored");
            System.out.println("Phase 5 offline signed import PASS: "+checks
                    +" bootstrap/revocation/rollback cases; Minecraft UNTESTED");
        }finally{delete(rootDirectory);}
        rotationRegression();
    }
}
