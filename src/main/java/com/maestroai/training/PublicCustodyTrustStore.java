package com.maestroai.training;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Phase 5.10. Offline, owner-local signed-custody trust-state journal.
 *
 * BOOTSTRAP AND ROLLBACK ROOTS MUST COME FROM AN INDEPENDENT TRUST CHANNEL:
 * the supplied initial public-key policy and exact latest Checkpoint are never
 * recovered from the same untrusted state file. The format contains replay IDs
 * and public decision hashes and MUST remain local (no repository artifacts).
 * No signing/private keys, network, game hooks or live tactical consumers.
 */
public final class PublicCustodyTrustStore {
    private PublicCustodyTrustStore() {}
    public static final int VERSION=1;
    private static final int MAGIC=0x4D355431; // M5T1
    private static final int MAX_BYTES=2_000_000;
    private static final String STATE="custody-anchor.state";
    private static final String PENDING="custody-anchor.pending";
    private static final byte[] UPDATE_DOMAIN=
            "MAESTROAI-PHASE5-TRUST-UPDATE-V1\0"
                    .getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    /** Transport object only. Its signature MUST be checked against the prior keyring. */
    public record KeyringUpdate(String signerKeyId,long signedAtEpochSecond,
                                String previousPolicyDigest,
                                PublicSignedCustody.TrustedPolicy nextPolicy,String signatureBase64) {
        public KeyringUpdate {
            if (signerKeyId==null || !signerKeyId.matches("[a-z0-9][a-z0-9_-]{0,47}")
                    || signedAtEpochSecond<1 || !digest(previousPolicyDigest)
                    || nextPolicy==null || !canonicalB64(signatureBase64,64))
                throw invalid();
        }
    }

    /** Must be copied/persisted by the TRUSTED CALLER outside this state file. */
    public record Checkpoint(String stateSha256,long custodyRevision,long keyringEpoch) {
        public Checkpoint {
            if (!digest(stateSha256) || custodyRevision<1 || keyringEpoch<1) throw invalid();
        }
        @Override public String toString() { return "OfflineCheckpoint[external-pin-required]"; }
    }

    public record Recovered(long custodyRevision,long keyringEpoch,
                            boolean signedLedgerVerified,boolean liveAuthorized) {
        public Recovered {
            if (custodyRevision<1 || keyringEpoch<1
                    || !signedLedgerVerified || liveAuthorized) throw invalid();
        }
    }

    private record State(List<KeyringUpdate> updates,
                         PublicSignedCustody.SignedManifest manifest,
                         PublicSignedCustody.TrustedPolicy policy,
                         PublicSignedCustody.Verified verified) {}
    /** One immutable in-process view, resolved from a SINGLE verified file read. */
    record Resolved(PublicSignedCustody.SignedManifest manifest,
                    PublicSignedCustody.TrustedPolicy policy) {}

    /** Immutable canonical policy bytes; keys sorted by ID, never caller order. */
    public static byte[] policyBytes(PublicSignedCustody.TrustedPolicy policy) {
        if (policy==null) throw invalid();
        try {
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            out.writeInt(VERSION);
            writePolicy(out,policy);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException error) { throw invalid(); }
    }

    public static String policyDigest(PublicSignedCustody.TrustedPolicy policy) {
        return hash(policyBytes(policy));
    }

    /** An independently trusted signer signs these exact canonical bytes. */
    public static byte[] updateBytes(String signerKeyId,long signedAtEpochSecond,
                                     String previousPolicyDigest,
                                     PublicSignedCustody.TrustedPolicy nextPolicy) {
        if (signerKeyId==null || !signerKeyId.matches("[a-z0-9][a-z0-9_-]{0,47}")
                || signedAtEpochSecond<1 || !digest(previousPolicyDigest) || nextPolicy==null)
            throw invalid();
        try {
            var bytes=new ByteArrayOutputStream();
            bytes.write(UPDATE_DOMAIN);
            var out=new DataOutputStream(bytes);
            writeString(out,signerKeyId);
            out.writeLong(signedAtEpochSecond);
            writeString(out,previousPolicyDigest);
            writePolicy(out,nextPolicy);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException error) { throw invalid(); }
    }

    public static byte[] updateBytes(KeyringUpdate update) {
        if (update==null) throw invalid();
        return updateBytes(update.signerKeyId(),update.signedAtEpochSecond(),
                update.previousPolicyDigest(),update.nextPolicy());
    }

    /** No key may be silently deleted, resurrected, replaced, or unrevoked. */
    public static PublicSignedCustody.TrustedPolicy verifyKeyringUpdate(
            PublicSignedCustody.TrustedPolicy previous, KeyringUpdate update,
            long evaluatedAtEpochSecond) {
        if (previous==null || update==null || evaluatedAtEpochSecond<1
                || !policyDigest(previous).equals(update.previousPolicyDigest())
                || update.nextPolicy().keyringEpoch()!=previous.keyringEpoch()+1
                || update.nextPolicy().minimumKeyringEpoch()!=update.nextPolicy().keyringEpoch()
                || update.nextPolicy().minimumRevision()<previous.minimumRevision()
                || update.nextPolicy().earliestSignatureEpochSecond()
                        <previous.earliestSignatureEpochSecond()
                || update.signedAtEpochSecond()>evaluatedAtEpochSecond)
            throw invalid();
        var prior=new HashMap<String,PublicSignedCustody.TrustedKey>();
        for(var key:previous.keys()) prior.put(key.keyId(),key);
        var next=new HashMap<String,PublicSignedCustody.TrustedKey>();
        for(var key:update.nextPolicy().keys()) next.put(key.keyId(),key);
        if (!next.keySet().containsAll(prior.keySet())) throw invalid();
        for(var old:previous.keys()) {
            var replacement=next.get(old.keyId());
            if (!old.encodedPublicKey().equals(replacement.encodedPublicKey())
                    || old.activeFromEpochSecond()!=replacement.activeFromEpochSecond()
                    || (old.revokedAtEpochSecond()>0
                        && old.revokedAtEpochSecond()!=replacement.revokedAtEpochSecond())
                    || (old.revokedAtEpochSecond()==0
                        && replacement.revokedAtEpochSecond()>0
                        && replacement.revokedAtEpochSecond()<update.signedAtEpochSecond()))
                throw invalid();
        }
        // A changed or cleared exact checkpoint must be independently anchored.
        if (!previous.requiredManifestDigest().equals(update.nextPolicy().requiredManifestDigest()))
            throw invalid();

        var signer=prior.get(update.signerKeyId());
        if (signer==null || signer.activeFromEpochSecond()>update.signedAtEpochSecond()
                || (signer.revokedAtEpochSecond()>0
                    && update.signedAtEpochSecond()>=signer.revokedAtEpochSecond()))
            throw invalid();
        try {
            var key=KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(
                            Base64.getDecoder().decode(signer.encodedPublicKey())));
            var verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(updateBytes(update));
            if (!verifier.verify(Base64.getDecoder().decode(update.signatureBase64())))
                throw invalid();
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw invalid();
        }
        return update.nextPolicy();
    }

    /**
     * Initial file can be written only to a fresh directory with no state.
     * Caller must anchor the RETURNED digest outside this directory before
     * trusting future recovery. Crash before that anchoring = fail closed.
     */
    public static Checkpoint initialize(Path directory,
            PublicSignedCustody.TrustedPolicy independentlyTrustedBootstrap,
            PublicSignedCustody.SignedManifest first,long evaluatedAtEpochSecond) {
        ensureDirectory(directory);
        if (Files.exists(statePath(directory),LinkOption.NOFOLLOW_LINKS)
                || Files.exists(pendingPath(directory),LinkOption.NOFOLLOW_LINKS))
            throw invalid();
        if (first==null || !"0".repeat(64).equals(first.previousManifestDigest()))
            throw invalid();
        var verified=PublicSignedCustody.verify(first,independentlyTrustedBootstrap,
                evaluatedAtEpochSecond);
        byte[] encoded=encode(List.of(),first);
        atomicWrite(directory,encoded);
        return new Checkpoint(hash(encoded),verified.revision(),
                independentlyTrustedBootstrap.keyringEpoch());
    }

    /** Verify BOTH locally stored bytes and all signatures against the
     * externally pinned policy and checkpoint. Stale .pending is ignored only
     * if the committed state still passes every check, then removed safely.
     */
    public static Recovered recover(Path directory,
            PublicSignedCustody.TrustedPolicy independentlyTrustedBootstrap,
            Checkpoint externallyPinned,long evaluatedAtEpochSecond) {
        State state=load(directory,independentlyTrustedBootstrap,externallyPinned,
                evaluatedAtEpochSecond);
        removePendingAfterValidRecovery(directory);
        return new Recovered(state.verified.revision(),state.policy.keyringEpoch(),true,false);
    }

    /**
     * One atomic advance from exactly the externally pinned prior state.
     * A successor manifest must append exactly ONE custody decision.
     * Keyring transitions must be signed by a previously trusted active key.
     */
    public static Checkpoint advance(Path directory,
            PublicSignedCustody.TrustedPolicy independentlyTrustedBootstrap,
            Checkpoint externallyPinned,List<KeyringUpdate> keyringUpdates,
            PublicSignedCustody.SignedManifest successor,long evaluatedAtEpochSecond) {
        State current=load(directory,independentlyTrustedBootstrap,externallyPinned,
                evaluatedAtEpochSecond);
        if (keyringUpdates==null || keyringUpdates.size()>32 || successor==null)
            throw invalid();
        var policy=current.policy;
        var combined=new ArrayList<>(current.updates);
        for(var update:keyringUpdates) {
            policy=verifyKeyringUpdate(policy,update,evaluatedAtEpochSecond);
            combined.add(update);
        }
        var verified=PublicSignedCustody.verifySuccessor(current.verified,successor,
                policy,evaluatedAtEpochSecond);
        if (verified.revision()<=current.verified.revision()
                || policy.keyringEpoch()<current.policy.keyringEpoch()) throw invalid();
        byte[] encoded=encode(combined,successor);
        atomicWrite(directory,encoded);
        return new Checkpoint(hash(encoded),verified.revision(),policy.keyringEpoch());
    }

    /**
     * Package-private receipt for offline provenance callers. Full verification
     * happens BEFORE any Phase 5.7 custody verification.
     */
    static Resolved resolve(Path directory,PublicSignedCustody.TrustedPolicy root,
                            Checkpoint anchored,long when) {
        var checked=load(directory,root,anchored,when);
        return new Resolved(checked.manifest,checked.policy);
    }

    static PublicSignedCustody.SignedManifest validatedManifest(Path directory,
            PublicSignedCustody.TrustedPolicy root,Checkpoint anchored,long when) {
        return load(directory,root,anchored,when).manifest;
    }
    static PublicSignedCustody.TrustedPolicy validatedPolicy(Path directory,
            PublicSignedCustody.TrustedPolicy root,Checkpoint anchored,long when) {
        return load(directory,root,anchored,when).policy;
    }

    private static State load(Path directory,PublicSignedCustody.TrustedPolicy root,
                              Checkpoint anchored,long when) {
        ensureDirectory(directory);
        if (root==null || anchored==null || when<1) throw invalid();
        byte[] data=readCommitted(directory);
        if (!hash(data).equals(anchored.stateSha256())) throw invalid();
        try {
            var in=new DataInputStream(new ByteArrayInputStream(data));
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION) throw invalid();
            int number=in.readInt();
            if(number<0 || number>32) throw invalid();
            var updates=new ArrayList<KeyringUpdate>();
            var policy=root;
            for (int i=0;i<number;i++) {
                var update=readUpdate(in);
                policy=verifyKeyringUpdate(policy,update,when);
                updates.add(update);
            }
            var manifest=readManifest(in);
            if (in.read()!=-1) throw invalid(); // No appended or ambiguous data.
            var verified=PublicSignedCustody.verify(manifest,policy,when);
            if (verified.revision()!=anchored.custodyRevision()
                    || policy.keyringEpoch()!=anchored.keyringEpoch())
                throw invalid();
            return new State(List.copyOf(updates),manifest,policy,verified);
        } catch(IOException | IllegalArgumentException ex) { throw invalid(); }
    }

    private static byte[] encode(List<KeyringUpdate> updates,
                                 PublicSignedCustody.SignedManifest manifest) {
        try {
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            out.writeInt(MAGIC); out.writeInt(VERSION);
            out.writeInt(updates.size());
            for(var update:updates) {
                writeString(out,update.signerKeyId());
                out.writeLong(update.signedAtEpochSecond());
                writeString(out,update.previousPolicyDigest());
                writePolicy(out,update.nextPolicy());
                writeString(out,update.signatureBase64());
            }
            writeManifest(out,manifest);
            out.flush();
            if(bytes.size()>MAX_BYTES) throw invalid();
            return bytes.toByteArray();
        } catch(IOException ex) { throw invalid(); }
    }

    private static KeyringUpdate readUpdate(DataInputStream in) throws IOException {
        String signer=readString(in),previous;
        long when=in.readLong();
        previous=readString(in);
        var policy=readPolicy(in);
        String sig=readString(in);
        return new KeyringUpdate(signer,when,previous,policy,sig);
    }
    private static void writeManifest(DataOutputStream out,
                                      PublicSignedCustody.SignedManifest manifest) throws IOException {
        out.writeInt(manifest.schemaVersion());
        writeString(out,manifest.signerKeyId());
        out.writeLong(manifest.signedAtEpochSecond());
        writeString(out,manifest.previousManifestDigest());
        var ledger=manifest.ledger();
        out.writeInt(ledger.schemaVersion());out.writeLong(ledger.revision());
        out.writeInt(ledger.decisions().size());
        for(var decision:ledger.decisions()) {
            writeString(out,decision.replayId());
            writeString(out,decision.format());
            writeString(out,decision.inputSha256());
            out.writeInt(decision.status().ordinal());
            out.writeLong(decision.decidedAtEpochSecond());
            out.writeLong(decision.expiresAtEpochSecond());
            writeString(out,decision.reviewDigest());
        }
        writeString(out,manifest.signatureBase64());
    }
    private static PublicSignedCustody.SignedManifest readManifest(DataInputStream in) throws IOException {
        int version=in.readInt();
        String signer=readString(in);
        long time=in.readLong();
        String previous=readString(in);
        int ledgerVersion=in.readInt();
        long revision=in.readLong();
        int count=in.readInt();
        if(count<1 || count>10000) throw invalid();
        var decisions=new ArrayList<PublicReplayCustody.Decision>();
        for(int i=0;i<count;i++) {
            String id=readString(in),format=readString(in),hash=readString(in);
            int status=in.readInt();
            if(status<0 || status>=PublicReplayCustody.Status.values().length) throw invalid();
            long decided=in.readLong(),expiry=in.readLong();
            decisions.add(new PublicReplayCustody.Decision(id,format,hash,
                    PublicReplayCustody.Status.values()[status],decided,expiry,readString(in)));
        }
        var ledger=new PublicReplayCustody.Ledger(ledgerVersion,revision,decisions);
        return new PublicSignedCustody.SignedManifest(version,signer,time,previous,
                ledger,readString(in));
    }

    private static void writePolicy(DataOutputStream out,
                                    PublicSignedCustody.TrustedPolicy policy) throws IOException {
        out.writeLong(policy.keyringEpoch());
        out.writeLong(policy.minimumKeyringEpoch());
        out.writeLong(policy.minimumRevision());
        out.writeLong(policy.earliestSignatureEpochSecond());
        writeString(out,policy.requiredManifestDigest());
        var sorted=new ArrayList<>(policy.keys());
        sorted.sort(Comparator.comparing(PublicSignedCustody.TrustedKey::keyId));
        out.writeInt(sorted.size());
        for(var key:sorted) {
            writeString(out,key.keyId());
            writeString(out,key.encodedPublicKey());
            out.writeLong(key.activeFromEpochSecond());
            out.writeLong(key.revokedAtEpochSecond());
        }
    }
    private static PublicSignedCustody.TrustedPolicy readPolicy(DataInputStream in) throws IOException {
        long epoch=in.readLong(),minimum=in.readLong(),revision=in.readLong(),earliest=in.readLong();
        String digest=readString(in);
        int size=in.readInt();
        if(size<1 || size>32) throw invalid();
        var keys=new ArrayList<PublicSignedCustody.TrustedKey>();
        for(int i=0;i<size;i++)
            keys.add(new PublicSignedCustody.TrustedKey(readString(in),readString(in),
                    in.readLong(),in.readLong()));
        return new PublicSignedCustody.TrustedPolicy(epoch,minimum,revision,earliest,digest,keys);
    }
    private static void writeString(DataOutputStream out,String value) throws IOException {
        if(value==null) throw invalid();
        byte[] bytes=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(bytes.length>1024) throw invalid();
        out.writeInt(bytes.length);out.write(bytes);
    }
    private static String readString(DataInputStream in) throws IOException {
        int len=in.readInt();
        if(len<0 || len>1024) throw invalid();
        byte[] bytes=in.readNBytes(len);
        if(bytes.length!=len) throw invalid();
        String value=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        if(!Arrays.equals(bytes,value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw invalid();
        return value;
    }
    private static void ensureDirectory(Path dir) {
        if(dir==null || !dir.isAbsolute() || Files.isSymbolicLink(dir)
                || !Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)) throw invalid();
    }
    private static Path statePath(Path dir) { return dir.resolve(STATE); }
    private static Path pendingPath(Path dir) { return dir.resolve(PENDING); }
    private static byte[] readCommitted(Path dir) {
        Path path=statePath(dir);
        if(Files.isSymbolicLink(path) || !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
            throw invalid();
        try {
            if(Files.size(path)<12 || Files.size(path)>MAX_BYTES) throw invalid();
            return Files.readAllBytes(path);
        } catch(IOException ex) { throw invalid(); }
    }
    private static void atomicWrite(Path dir,byte[] data) {
        ensureDirectory(dir);
        if(data.length>MAX_BYTES) throw invalid();
        Path pending=pendingPath(dir);
        if(Files.exists(pending,LinkOption.NOFOLLOW_LINKS)) {
            if(Files.isSymbolicLink(pending) || !Files.isRegularFile(pending,LinkOption.NOFOLLOW_LINKS))
                throw invalid();
            try { Files.delete(pending); } catch(IOException failure) { throw invalid(); }
        }
        try (FileChannel file=FileChannel.open(pending,StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer payload=ByteBuffer.wrap(data);
            while(payload.hasRemaining()) file.write(payload);
            file.force(true);
        } catch(IOException ex) { throw invalid(); }
        restrictPermissions(pending);
        try {
            Files.move(pending,statePath(dir),StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            // A persisted rename is not durable until directory metadata is synced.
            try(FileChannel channel=FileChannel.open(dir,StandardOpenOption.READ)) {
                channel.force(true);
            }
        } catch(IOException | UnsupportedOperationException ex) { throw invalid(); }
    }
    private static void removePendingAfterValidRecovery(Path dir) {
        Path pending=pendingPath(dir);
        if(!Files.exists(pending,LinkOption.NOFOLLOW_LINKS)) return;
        if(Files.isSymbolicLink(pending) || !Files.isRegularFile(pending,LinkOption.NOFOLLOW_LINKS))
            throw invalid();
        try {
            Files.delete(pending);
            try(FileChannel channel=FileChannel.open(dir,StandardOpenOption.READ)) {
                channel.force(true);
            }
        } catch(IOException | UnsupportedOperationException ex) { throw invalid(); }
    }
    private static void restrictPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file,Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch(UnsupportedOperationException ignored) {
            // Explicitly supported on non-POSIX filesystems; caller must
            // control the directory ACL and independently pin the checkpoint.
        } catch(IOException ex) { throw invalid(); }
    }
    private static boolean digest(String x) {
        return x!=null && x.matches("[0-9a-f]{64}");
    }
    private static boolean canonicalB64(String s,int length) {
        if(s==null || s.length()>1024) return false;
        try {
            byte[] decoded=Base64.getDecoder().decode(s);
            return decoded.length==length && Base64.getEncoder()
                    .encodeToString(decoded).equals(s);
        } catch(IllegalArgumentException ex) { return false; }
    }
    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch(NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Required SHA256 not available");
        }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified offline durable custody state");
    }
}
