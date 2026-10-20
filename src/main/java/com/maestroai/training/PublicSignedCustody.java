package com.maestroai.training;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Phase 5.9: Ed25519 offline verification over canonical, domain-separated
 * custody manifests. Never generates, imports or stores private keys.
 *
 * This proves a signature under a CALLER-TRUSTED public key, not independent
 * replay ownership, reviewer identity, freshness or runtime battle validity.
 * The caller MUST obtain the keyring, revision floor, checkpoint and time
 * from an independently trusted source and refresh them for future checks.
 */
public final class PublicSignedCustody {
    private PublicSignedCustody() {}
    public static final int VERSION=1;
    private static final byte[] DOMAIN="MAESTROAI-PHASE5-OFFLINE-CUSTODY-V1\0"
            .getBytes(StandardCharsets.US_ASCII);
    private static final String ZERO="0".repeat(64);

    /** The encoded SPKI is public. No private key material belongs here. */
    public record TrustedKey(String keyId, String encodedPublicKey,
                             long activeFromEpochSecond, long revokedAtEpochSecond) {
        public TrustedKey {
            if (!PublicSignedCustody.keyId(keyId) || encodedPublicKey==null
                    || activeFromEpochSecond<1
                    || (revokedAtEpochSecond!=0 && revokedAtEpochSecond<=activeFromEpochSecond)
                    || !canonicalB64(encodedPublicKey,32,100))
                throw invalid();
            // Reject a fake/wrong-algorithm key before creating a policy.
            parseKey(encodedPublicKey);
        }
    }

    /**
     * Operator CANNOT prove this policy is the newest merely by creating it.
     * A trusted caller MUST pin the latest revision/keyring epoch/checkpoint.
     * A non-empty checkpoint pins one exact canonical signed manifest digest.
     */
    public record TrustedPolicy(long keyringEpoch, long minimumKeyringEpoch,
                                long minimumRevision, long earliestSignatureEpochSecond,
                                String requiredManifestDigest, List<TrustedKey> keys) {
        public TrustedPolicy {
            if (keyringEpoch<1 || minimumKeyringEpoch<1
                    || keyringEpoch<minimumKeyringEpoch
                    || minimumRevision<1 || earliestSignatureEpochSecond<1
                    || requiredManifestDigest==null
                    || !(requiredManifestDigest.isEmpty() || hex(requiredManifestDigest))
                    || keys==null || keys.isEmpty() || keys.size()>32)
                throw invalid();
            keys=List.copyOf(keys);
            Set<String> ids=new HashSet<>();
            for (TrustedKey key:keys)
                if (key==null || !ids.add(key.keyId())) throw invalid();
        }
    }

    /**
     * Publicly constructible transport fields are UNTRUSTED until verify().
     * An explicit predecessor digest is signed and can be inspected against
     * a separately trusted checkpoint during an operator-controlled transition.
     */
    public record SignedManifest(int schemaVersion,String signerKeyId,long signedAtEpochSecond,
                                 String previousManifestDigest,
                                 PublicReplayCustody.Ledger ledger,String signatureBase64) {
        public SignedManifest {
            if (schemaVersion!=VERSION || !keyId(signerKeyId)
                    || signedAtEpochSecond<1
                    || !hex(previousManifestDigest)
                    || ledger==null
                    || !canonicalB64(signatureBase64,64,64))
                throw invalid();
        }
    }

    /** Non-constructible accepted envelope, no public replay or player fields. */
    public static final class Verified {
        private final PublicReplayCustody.Ledger ledger;
        private final String digest;
        private final long revision;
        private Verified(PublicReplayCustody.Ledger ledger,String digest) {
            this.ledger=ledger;
            this.digest=digest;
            this.revision=ledger.revision();
        }
        public long revision() { return revision; }
        public boolean liveAuthorized() { return false; }
        public boolean independentlyRightsVerified() { return false; }
        PublicReplayCustody.Ledger ledger() { return ledger; }
        String digest() { return digest; }
        @Override public String toString() {
            return "VerifiedOfflineCustody[signature=verified;rights=unverified;live=false]";
        }
    }

    /** Canonical signature preimage: fixed domain, big-endian fields and
     * length-prefixed UTF-8. No locale, JSON key order, hash-map order or
     * platform line-ending dependence. Input is capped by Phase 5.8 ledger.
     */
    public static byte[] canonicalBytes(int version,String signerKeyId,long signedAtEpochSecond,
                                        String previousManifestDigest,
                                        PublicReplayCustody.Ledger ledger) {
        if (version!=VERSION || !keyId(signerKeyId) || signedAtEpochSecond<1
                || !hex(previousManifestDigest) || ledger==null) throw invalid();
        try {
            var buffer=new ByteArrayOutputStream();
            buffer.write(DOMAIN);
            var out=new DataOutputStream(buffer);
            out.writeInt(version);
            writeString(out,signerKeyId);
            out.writeLong(signedAtEpochSecond);
            writeString(out,previousManifestDigest);
            out.writeInt(ledger.schemaVersion());
            out.writeLong(ledger.revision());
            out.writeInt(ledger.decisions().size());
            for (var decision:ledger.decisions()) {
                writeString(out,decision.replayId());
                writeString(out,decision.format());
                writeString(out,decision.inputSha256());
                out.writeInt(decision.status().ordinal());
                out.writeLong(decision.decidedAtEpochSecond());
                out.writeLong(decision.expiresAtEpochSecond());
                writeString(out,decision.reviewDigest());
            }
            out.flush();
            return buffer.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Internal canonical encoding unavailable");
        }
    }

    public static byte[] canonicalBytes(SignedManifest manifest) {
        if (manifest==null) throw invalid();
        return canonicalBytes(manifest.schemaVersion(),manifest.signerKeyId(),
                manifest.signedAtEpochSecond(),manifest.previousManifestDigest(),manifest.ledger());
    }

    public static String manifestDigest(SignedManifest manifest) {
        return sha256(canonicalBytes(manifest));
    }

    /**
     * Validate signature, key state, caller-pinned anti-rollback controls and
     * complete ledger chronology. Returns a local-only in-process receipt.
     */
    public static Verified verify(SignedManifest manifest,TrustedPolicy policy,
                                  long evaluatedAtEpochSecond) {
        if (manifest==null || policy==null || evaluatedAtEpochSecond<1
                || policy.keyringEpoch()<policy.minimumKeyringEpoch()
                || manifest.ledger().revision()<policy.minimumRevision()
                || manifest.signedAtEpochSecond()<policy.earliestSignatureEpochSecond()
                || manifest.signedAtEpochSecond()>evaluatedAtEpochSecond)
            throw invalid();
        var decisions=manifest.ledger().decisions();
        if (decisions.isEmpty()
                || decisions.get(decisions.size()-1).decidedAtEpochSecond()
                    >manifest.signedAtEpochSecond())
            throw invalid();

        TrustedKey signer=null;
        for (TrustedKey key:policy.keys()) {
            if (key.keyId().equals(manifest.signerKeyId())) signer=key;
        }
        if (signer==null || signer.activeFromEpochSecond()>manifest.signedAtEpochSecond()
                || signer.activeFromEpochSecond()>evaluatedAtEpochSecond
                || (signer.revokedAtEpochSecond()>0
                    && evaluatedAtEpochSecond>=signer.revokedAtEpochSecond()))
            throw invalid();

        byte[] canonical=canonicalBytes(manifest);
        String digest=sha256(canonical);
        if (!policy.requiredManifestDigest().isEmpty()
                && !policy.requiredManifestDigest().equals(digest)) throw invalid();
        try {
            var verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(parseKey(signer.encodedPublicKey()));
            verifier.update(canonical);
            if (!verifier.verify(Base64.getDecoder().decode(manifest.signatureBase64())))
                throw invalid();
            return new Verified(manifest.ledger(),digest);
        } catch (java.security.GeneralSecurityException | IllegalArgumentException ex) {
            throw invalid(); // No untrusted input or cryptographic internals in errors.
        }
    }

    /**
     * Caller-pinned, direct append transition. A signed successor cannot
     * silently remove/replace old approvals or revocations. Intermediate
     * revisions require their own verified consecutive transitions.
     */
    public static Verified verifySuccessor(Verified previouslyTrusted,SignedManifest next,
                                           TrustedPolicy policy,long evaluatedAtEpochSecond) {
        if (previouslyTrusted==null || next==null
                || next.ledger().revision()!=previouslyTrusted.revision()+1
                || !next.previousManifestDigest().equals(previouslyTrusted.digest()))
            throw invalid();
        List<PublicReplayCustody.Decision> prior=previouslyTrusted.ledger().decisions();
        if (!next.ledger().decisions().subList(0,prior.size()).equals(prior))
            throw invalid();
        return verify(next,policy,evaluatedAtEpochSecond);
    }

    private static void writeString(DataOutputStream out,String s) throws IOException {
        byte[] bytes=s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length>256) throw invalid();
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private static PublicKey parseKey(String encoded) {
        try {
            PublicKey key=KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            if (!"Ed25519".equalsIgnoreCase(key.getAlgorithm())) throw invalid();
            return key;
        } catch (java.security.GeneralSecurityException | IllegalArgumentException error) {
            throw invalid();
        }
    }
    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
    private static boolean keyId(String s) {
        return s!=null && s.matches("[a-z0-9][a-z0-9_-]{0,47}");
    }
    private static boolean hex(String s) {
        return s!=null && s.matches("[0-9a-f]{64}");
    }
    private static boolean canonicalB64(String s,int min,int max) {
        if (s==null || s.length()>1024) return false;
        try {
            byte[] decoded=Base64.getDecoder().decode(s);
            return decoded.length>=min && decoded.length<=max
                    && Base64.getEncoder().encodeToString(decoded).equals(s);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified signed public custody evidence");
    }
}
