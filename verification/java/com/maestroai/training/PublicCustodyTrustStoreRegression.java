package com.maestroai.training;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Phase 5.10 synthetic-only recovery, atomic write and signed rotation suite. */
final class PublicCustodyTrustStoreRegression {
    private PublicCustodyTrustStoreRegression() {}
    private static int checks;
    private static void check(boolean ok) {
        if(!ok) throw new AssertionError("Phase 5.10 durable trust regression");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SECRET_SYNTHETIC_PLAYER"));return;
        }
        throw new AssertionError("Unsafe durable trust transition accepted");
    }
    private static final long T0=1_700_000_000L,T1=T0+10,T2=T0+20,
            T3=T0+30,T4=T0+40,T5=T0+50,T6=T0+60,T7=T0+70,
            T8=T0+80,T9=T0+90,T10=T0+100,T11=T0+110;
    private static final String ZERO="0".repeat(64),REVIEW="b".repeat(64);
    private static final String LOG_A="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SECRET_SYNTHETIC_PLAYER\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
            +"|switch|p2a: Rival|Zoroark, L100|100/100\n|turn|1\n"
            +"|move|p2a: Rival|Swift|p1a: Ally\n"
            +"|win|SECRET_SYNTHETIC_PLAYER\n";
    private static final String LOG_B="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|OTHER_SYNTHETIC_PLAYER\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
            +"|switch|p2a: Rival|Ditto, L100|100/100\n|turn|1\n"
            +"|move|p2a: Rival|Tackle|p1a: Ally\n"
            +"|win|OTHER_SYNTHETIC_PLAYER\n";
    private static KeyPair key() {
        try {
            KeyPairGenerator k=KeyPairGenerator.getInstance("Ed25519");
            return k.generateKeyPair();
        }catch(GeneralSecurityException error){throw new AssertionError(error);}
    }
    private static PublicSignedCustody.TrustedKey trusted(String id,KeyPair pair,long revoked) {
        return new PublicSignedCustody.TrustedKey(id,
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),T0-10,revoked);
    }
    private static PublicSignedCustody.TrustedPolicy policy(long epoch,long floor,
                                      PublicSignedCustody.TrustedKey... keys) {
        return new PublicSignedCustody.TrustedPolicy(epoch,epoch,floor,T0-20,"",List.of(keys));
    }
    private static PublicReplayCorpus.Source source(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static PublicReplayCustody.Decision approve(String id,String log,long at) {
        var s=source(id);
        return new PublicReplayCustody.Decision(s.replayId(),s.format(),
                PublicReplayCorpus.ingest(s,log).inputSha256(),
                PublicReplayCustody.Status.APPROVED,at,T0+1_000,REVIEW);
    }
    private static PublicReplayCustody.Decision revoke(PublicReplayCustody.Decision a,long at) {
        return new PublicReplayCustody.Decision(a.replayId(),a.format(),a.inputSha256(),
                PublicReplayCustody.Status.REVOKED,at,0,REVIEW);
    }
    private static PublicReplayCustody.Ledger ledger(PublicReplayCustody.Decision... decisions) {
        return new PublicReplayCustody.Ledger(1,decisions.length,List.of(decisions));
    }
    private static byte[] signature(KeyPair signer,byte[] bytes) {
        try {
            Signature s=Signature.getInstance("Ed25519");
            s.initSign(signer.getPrivate());s.update(bytes);return s.sign();
        } catch(GeneralSecurityException failure){throw new AssertionError(failure);}
    }
    private static PublicSignedCustody.SignedManifest sign(String id,KeyPair key,
            PublicReplayCustody.Ledger ledger,long at,String priorDigest) {
        byte[] bytes=PublicSignedCustody.canonicalBytes(1,id,at,priorDigest,ledger);
        return new PublicSignedCustody.SignedManifest(1,id,at,priorDigest,ledger,
                Base64.getEncoder().encodeToString(signature(key,bytes)));
    }
    private static PublicCustodyTrustStore.KeyringUpdate update(String id,KeyPair key,long at,
                PublicSignedCustody.TrustedPolicy old,PublicSignedCustody.TrustedPolicy next) {
        String parent=PublicCustodyTrustStore.policyDigest(old);
        byte[] bytes=PublicCustodyTrustStore.updateBytes(id,at,parent,next);
        return new PublicCustodyTrustStore.KeyringUpdate(id,at,parent,next,
                Base64.getEncoder().encodeToString(signature(key,bytes)));
    }
    private static PublicHeldOutEvaluation.Replay train() {
        return new PublicHeldOutEvaluation.Replay(source("12345678"),LOG_A,
                PublicHeldOutEvaluation.Partition.TRAIN);
    }
    private static PublicHeldOutEvaluation.Replay test() {
        return new PublicHeldOutEvaluation.Replay(source("23456789"),LOG_B,
                PublicHeldOutEvaluation.Partition.HELD_OUT);
    }
    private static void deleteTree(Path path) {
        if(path==null) return;
        try(var files=Files.walk(path)) {
            files.sorted(Comparator.reverseOrder()).forEach(p->{
                try { Files.deleteIfExists(p); }
                catch(IOException error){throw new AssertionError(error);}
            });
        } catch(IOException failure){throw new AssertionError(failure);}
    }
    static void run() {
        Path dir=null;
        try {
            dir=Files.createTempDirectory("synthetic-custody-").toAbsolutePath();
            final Path location=dir;
            KeyPair alice=key(),bob=key(),imposter=key();
            var keyA=trusted("reviewer-a",alice,0);
            var keyB=trusted("reviewer-b",bob,0);
            var root=policy(1,1,keyA);
            var a=approve("12345678",LOG_A,T0);
            var b=approve("23456789",LOG_B,T1);
            var one=sign("reviewer-a",alice,ledger(a),T2,ZERO);
            var pinned=PublicCustodyTrustStore.initialize(dir,root,one,T3);
            check(pinned.custodyRevision()==1 && pinned.keyringEpoch()==1);
            check(pinned.stateSha256().matches("[0-9a-f]{64}"));
            check(!pinned.toString().contains("gen9ou-")
                    && !pinned.toString().contains(a.inputSha256()));
            var recovered=PublicCustodyTrustStore.recover(dir,root,pinned,T3);
            check(recovered.custodyRevision()==1 && recovered.keyringEpoch()==1);
            check(recovered.signedLedgerVerified() && !recovered.liveAuthorized());
            check(PublicCustodyTrustStore.policyDigest(root).matches("[a-f0-9]{64}"));
            check(Arrays.equals(PublicCustodyTrustStore.policyBytes(root),
                    PublicCustodyTrustStore.policyBytes(root)));
            check(PublicCustodyTrustStore.validatedManifest(dir,root,pinned,T3)
                    .equals(one));
            reject(()->PublicCustodyTrustStore.initialize(location,root,one,T3));
            reject(()->PublicCustodyTrustStore.recover(location,null,pinned,T3));
            reject(()->PublicCustodyTrustStore.recover(location,root,null,T3));
            reject(()->PublicCustodyTrustStore.recover(location,root,
                    new PublicCustodyTrustStore.Checkpoint("f".repeat(64),1,1),T3));
            reject(()->PublicCustodyTrustStore.recover(location,root,
                    new PublicCustodyTrustStore.Checkpoint(pinned.stateSha256(),2,1),T3));
            reject(()->PublicCustodyTrustStore.recover(location,root,
                    new PublicCustodyTrustStore.Checkpoint(pinned.stateSha256(),1,2),T3));
            reject(()->PublicCustodyTrustStore.recover(location,
                    policy(1,1,trusted("reviewer-a",imposter,0)),pinned,T3));
            reject(()->PublicCustodyTrustStore.recover(location,root,pinned,T0));
            reject(()->PublicCustodyTrustStore.recover(location.resolve("missing"),
                    root,pinned,T3));
            reject(()->PublicCustodyTrustStore.recover(Path.of("relative"),
                    root,pinned,T3));

            // Interrupt before atomic rename: orphaned pending file is NEVER
            // interpreted as a newer authorized trust state.
            Path pending=location.resolve("custody-anchor.pending");
            Files.writeString(pending,"partial-untrusted-data",StandardCharsets.UTF_8);
            check(Files.exists(pending));
            check(PublicCustodyTrustStore.recover(location,root,pinned,T3)
                    .custodyRevision()==1);
            check(!Files.exists(pending));

            // A valid local state is NOT proof that the latest external
            // checkpoint has not moved. Arbitrary downgrades must fail closed.
            byte[] savedFirst=Files.readAllBytes(location.resolve("custody-anchor.state"));
            byte[] modified=savedFirst.clone();
            modified[modified.length-8]^=0x02;
            Files.write(location.resolve("custody-anchor.state"),modified);
            reject(()->PublicCustodyTrustStore.recover(location,root,pinned,T3));
            Files.write(location.resolve("custody-anchor.state"),savedFirst);
            check(PublicCustodyTrustStore.recover(location,root,pinned,T3)
                    .custodyRevision()==1);
            Files.write(location.resolve("custody-anchor.state"),
                    Arrays.copyOf(savedFirst,savedFirst.length-3));
            reject(()->PublicCustodyTrustStore.recover(location,root,pinned,T3));
            Files.write(location.resolve("custody-anchor.state"),savedFirst);
            Files.write(location.resolve("custody-anchor.state"),
                    Arrays.copyOf(savedFirst,savedFirst.length+2));
            reject(()->PublicCustodyTrustStore.recover(location,root,pinned,T3));
            Files.write(location.resolve("custody-anchor.state"),savedFirst);

            // Add one new approved replay, signed by the original reviewer.
            var two=sign("reviewer-a",alice,ledger(a,b),T4,
                    PublicSignedCustody.manifestDigest(one));
            var secondPin=PublicCustodyTrustStore.advance(dir,root,pinned,List.of(),two,T5);
            check(secondPin.custodyRevision()==2 && secondPin.keyringEpoch()==1);
            check(!secondPin.stateSha256().equals(pinned.stateSha256()));
            check(PublicCustodyTrustStore.recover(dir,root,secondPin,T5)
                    .custodyRevision()==2);
            reject(()->PublicCustodyTrustStore.recover(location,root,pinned,T5));
            // Replaced old valid state is rejected under newer external anchor.
            byte[] savedSecond=Files.readAllBytes(location.resolve("custody-anchor.state"));
            Files.write(location.resolve("custody-anchor.state"),savedFirst);
            reject(()->PublicCustodyTrustStore.recover(location,root,secondPin,T5));
            Files.write(location.resolve("custody-anchor.state"),savedSecond);
            reject(()->PublicCustodyTrustStore.advance(location,root,pinned,List.of(),
                    two,T5));
            reject(()->PublicCustodyTrustStore.advance(location,root,secondPin,List.of(),
                    two,T5)); // Non-increasing revision.

            var pair=List.of(train(),test());
            var report=PublicHeldOutEvaluation.evaluate(pair,
                    CompetitiveFormatNormalizer.Target.pinnedSource());
            var offline=PublicReplayProvenance.ingestWithDurableCustody(
                    source("12345678"),LOG_A,CompetitiveFormatNormalizer.Target.pinnedSource(),
                    location,root,secondPin,T5);
            check(!offline.tacticalDecisionReady() && !offline.shadowAudit().livePromotionAuthorized());
            var split=PublicReplayProvenance.verifySplitWithDurableCustody(pair,
                    CompetitiveFormatNormalizer.Target.pinnedSource(),report,
                    location,root,secondPin,T5);
            check(split.trainingReplays()==1 && split.heldOutReplays()==1);
            check(!split.legalActionReceipt() && !split.calibratedForecast());
            reject(()->PublicReplayProvenance.ingestWithDurableCustody(
                    source("12345678"),LOG_A,CompetitiveFormatNormalizer.Target.pinnedSource(),
                    location,root,pinned,T5));
            reject(()->PublicReplayProvenance.verifySplitWithDurableCustody(pair,
                    CompetitiveFormatNormalizer.Target.pinnedSource(),report,
                    location,root,pinned,T5));

            // Keyring successor: authorized by previous active signer, then a
            // new reviewer signs the next custody manifest.
            var updated=policy(2,3,trusted("reviewer-a",alice,T7),keyB);
            var rotation=update("reviewer-a",alice,T6,root,updated);
            check(PublicCustodyTrustStore.verifyKeyringUpdate(root,rotation,T6).equals(updated));
            check(Arrays.equals(PublicCustodyTrustStore.updateBytes(rotation),
                    PublicCustodyTrustStore.updateBytes("reviewer-a",T6,
                            PublicCustodyTrustStore.policyDigest(root),updated)));
            var revokedA=revoke(a,T8);
            var three=sign("reviewer-b",bob,ledger(a,b,revokedA),T9,
                    PublicSignedCustody.manifestDigest(two));
            var thirdPin=PublicCustodyTrustStore.advance(dir,root,secondPin,
                    List.of(rotation),three,T10);
            check(thirdPin.keyringEpoch()==2 && thirdPin.custodyRevision()==3);
            check(PublicCustodyTrustStore.recover(dir,root,thirdPin,T10)
                    .keyringEpoch()==2);
            check(!thirdPin.stateSha256().equals(secondPin.stateSha256()));
            reject(()->PublicReplayProvenance.ingestWithDurableCustody(source("12345678"),
                    LOG_A,CompetitiveFormatNormalizer.Target.pinnedSource(),
                    location,root,thirdPin,T10));
            var remaining=PublicReplayProvenance.ingestWithDurableCustody(source("23456789"),
                    LOG_B,CompetitiveFormatNormalizer.Target.pinnedSource(),
                    location,root,thirdPin,T10);
            check(!remaining.tacticalDecisionReady());

            // Tampered/truncated/update-history state cannot validate even when
            // the currently signed final manifest is itself authentic.
            byte[] savedThird=Files.readAllBytes(location.resolve("custody-anchor.state"));
            byte[] poison=savedThird.clone();
            poison[poison.length/3]^=0x04;
            Files.write(location.resolve("custody-anchor.state"),poison);
            reject(()->PublicCustodyTrustStore.recover(location,root,thirdPin,T10));
            Files.write(location.resolve("custody-anchor.state"),savedSecond);
            reject(()->PublicCustodyTrustStore.recover(location,root,thirdPin,T10));
            Files.write(location.resolve("custody-anchor.state"),savedThird);
            check(PublicCustodyTrustStore.recover(dir,root,thirdPin,T10).signedLedgerVerified());
            reject(()->PublicCustodyTrustStore.advance(location,root,thirdPin,List.of(),
                    three,T10));
            reject(()->PublicCustodyTrustStore.advance(location,root,thirdPin,null,
                    three,T10));

            // Wrong or missing keyring authority never authorizes a rotation.
            var fakeRotation=update("reviewer-a",imposter,T6,root,updated);
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,fakeRotation,T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,
                    update("reviewer-a",alice,T6,root,policy(4,3,keyA,keyB)),T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,
                    update("reviewer-a",alice,T6,root,policy(2,3,keyB)),T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,
                    update("reviewer-a",alice,T6,root,policy(2,3,
                            trusted("reviewer-a",imposter,0),keyB)),T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,
                    update("reviewer-a",alice,T6,root,policy(2,3,
                            trusted("reviewer-a",alice,T5),keyB)),T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(root,
                    update("reviewer-a",alice,T6,root,policy(2,3,
                            trusted("reviewer-a",alice,T7),keyB)),T5));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(updated,
                    update("reviewer-a",alice,T9,updated,
                            policy(3,4,trusted("reviewer-a",alice,T7),keyB)),T10));
            reject(()->PublicCustodyTrustStore.verifyKeyringUpdate(updated,
                    update("reviewer-b",bob,T9,updated,policy(3,4,keyA,keyB)),T10));
            reject(()->PublicCustodyTrustStore.advance(location,root,thirdPin,
                    List.of(rotation),three,T10)); // Replaying prior signed update fails.
            try { Files.createSymbolicLink(location.resolve("custody-anchor.pending"),
                    location.resolve("custody-anchor.state")); }
            catch (UnsupportedOperationException | IOException platform) {
                // Filesystem lacks symlinks; the remaining hostile file cases still apply.
            }
            if (Files.isSymbolicLink(location.resolve("custody-anchor.pending"))) {
                reject(()->PublicCustodyTrustStore.recover(location,root,thirdPin,T10));
                Files.delete(location.resolve("custody-anchor.pending"));
            }
            check(PublicCustodyTrustStore.recover(location,root,thirdPin,T10)
                    .custodyRevision()==3);
            System.out.println("Phase 5.10 synthetic durable trust/recovery PASS: "+checks+" checks");
        } catch(IOException io) {
            throw new AssertionError("Synthetic filesystem fixture unavailable",io);
        } finally { deleteTree(dir); }
    }
}
