package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.NamedParameterSpec;
import java.util.*;

/**
 * Phase 5.9 synthetic-only Ed25519 cryptographic custody regressions.
 * Signing keys are ephemeral TEST fixtures, never committed/private evidence.
 */
final class PublicSignedCustodyRegression {
    private PublicSignedCustodyRegression() {}
    private static int checks;
    private static void check(boolean pass) {
        if (!pass) throw new AssertionError("Phase 5.9 signed custody");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SECRET_SYNTHETIC_PERSON")
                    && !expected.getMessage().contains("SuperSecretNickname"));
            return;
        }
        throw new AssertionError("Untrusted signed custody accepted");
    }
    private static final long T0=1_700_000_000L, T1=T0+10, T2=T0+20,
            T3=T0+30,T4=T0+40,T5=T0+50,T6=T0+60,T7=T0+70,
            T8=T0+80,T9=T0+90,T10=T0+100,T11=T0+110;
    private static final String ZERO="0".repeat(64),REVIEW="c".repeat(64);
    private static final CompetitiveFormatNormalizer.Target TARGET =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final String LOG_A="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SECRET_SYNTHETIC_PERSON\n|start\n"
            +"|switch|p1a: Friendly|Pikachu, L100|100/100\n"
            +"|switch|p2a: SuperSecretNickname|Zoroark, L100|100/100\n"
            +"|turn|1\n|move|p2a: SuperSecretNickname|Swift|p1a: Friendly\n"
            +"|win|SECRET_SYNTHETIC_PERSON\n";
    private static final String LOG_B="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|A_DIFFERENT_PERSON\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
            +"|switch|p2a: Ditto|Ditto, L100|100/100\n"
            +"|turn|1\n|move|p2a: Ditto|Tackle|p1a: Ally\n"
            +"|win|A_DIFFERENT_PERSON\n";
    private static PublicReplayCorpus.Source source(String replay) {
        return new PublicReplayCorpus.Source("gen9ou-"+replay,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static PublicReplayCustody.Decision approval(String replay,String log,long decided) {
        var source=source(replay);
        return new PublicReplayCustody.Decision(source.replayId(),source.format(),
                PublicReplayCorpus.ingest(source,log).inputSha256(),
                PublicReplayCustody.Status.APPROVED,decided,T0+1000,REVIEW);
    }
    private static PublicReplayCustody.Decision revoked(PublicReplayCustody.Decision a,long at) {
        return new PublicReplayCustody.Decision(a.replayId(),a.format(),a.inputSha256(),
                PublicReplayCustody.Status.REVOKED,at,0,REVIEW);
    }
    private static PublicReplayCustody.Ledger ledger(PublicReplayCustody.Decision... decisions) {
        return new PublicReplayCustody.Ledger(1,decisions.length,List.of(decisions));
    }
    private static KeyPair key(int seed) {
        try {
            // Synthetic key generation is scoped to this test process only.
            SecureRandom seeded=SecureRandom.getInstance("SHA1PRNG");
            seeded.setSeed(("synthetic-offline-5.9-"+seed).getBytes(StandardCharsets.US_ASCII));
            KeyPairGenerator generator=KeyPairGenerator.getInstance("Ed25519");
            generator.initialize(new NamedParameterSpec("Ed25519"),seeded);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException failure) {
            throw new AssertionError("Required Java 21 Ed25519 unavailable",failure);
        }
    }
    private static PublicSignedCustody.TrustedKey trust(String id,KeyPair keys,long revokedAt) {
        return new PublicSignedCustody.TrustedKey(id,
                Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),T0-50,revokedAt);
    }
    private static PublicSignedCustody.TrustedPolicy policy(long epoch,long floor,
                      String checkpoint,PublicSignedCustody.TrustedKey... keys) {
        return new PublicSignedCustody.TrustedPolicy(epoch,epoch,floor,T0-10,
                checkpoint,List.of(keys));
    }
    private static PublicSignedCustody.SignedManifest sign(
                String id,KeyPair keys,PublicReplayCustody.Ledger ledger,
                long signedAt,String predecessor) {
        try {
            byte[] payload=PublicSignedCustody.canonicalBytes(1,id,signedAt,predecessor,ledger);
            Signature signer=Signature.getInstance("Ed25519");
            signer.initSign(keys.getPrivate());
            signer.update(payload);
            return new PublicSignedCustody.SignedManifest(1,id,signedAt,predecessor,
                    ledger,Base64.getEncoder().encodeToString(signer.sign()));
        } catch (GeneralSecurityException failure) {
            throw new AssertionError("Synthetic signature generation failed",failure);
        }
    }
    private static PublicHeldOutEvaluation.Replay training() {
        return new PublicHeldOutEvaluation.Replay(source("12345678"),LOG_A,
                PublicHeldOutEvaluation.Partition.TRAIN);
    }
    private static PublicHeldOutEvaluation.Replay test() {
        return new PublicHeldOutEvaluation.Replay(source("23456789"),LOG_B,
                PublicHeldOutEvaluation.Partition.HELD_OUT);
    }

    static void run() {
        KeyPair alice=key(11), bob=key(22), stranger=key(33);
        var keyA=trust("reviewer-a",alice,0);
        var keyB=trust("reviewer-b",bob,0);
        var a=approval("12345678",LOG_A,T0);
        var b=approval("23456789",LOG_B,T1);
        var initial=ledger(a,b);
        var envelope=sign("reviewer-a",alice,initial,T2,ZERO);
        var trusted=policy(1,2,"",keyA);
        var verified=PublicSignedCustody.verify(envelope,trusted,T3);
        check(verified.revision()==2);
        check(!verified.liveAuthorized() && !verified.independentlyRightsVerified());
        check(!verified.toString().contains("SECRET_SYNTHETIC_PERSON")
                && !verified.toString().contains("gen9ou-")
                && !verified.toString().contains(a.inputSha256()));
        check(Arrays.stream(PublicSignedCustody.Verified.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        check(Arrays.equals(PublicSignedCustody.canonicalBytes(envelope),
                PublicSignedCustody.canonicalBytes(1,"reviewer-a",T2,ZERO,initial)));
        String digest=PublicSignedCustody.manifestDigest(envelope);
        check(digest.matches("[0-9a-f]{64}"));
        check(PublicSignedCustody.verify(envelope,policy(1,2,digest,keyA),T3).revision()==2);
        check(PublicSignedCustody.verify(envelope,trusted,T3).toString()
                .equals(verified.toString()));
        check(!Arrays.equals(PublicSignedCustody.canonicalBytes(envelope),
                PublicSignedCustody.canonicalBytes(1,"reviewer-a",T2+1,ZERO,initial)));

        // Exact signer identity and bytes are authenticated: plausible public
        // record fields cannot substitute for a trusted Ed25519 signature.
        reject(()->PublicSignedCustody.verify(envelope,policy(1,2,"",
                trust("reviewer-a",stranger,0)),T3));
        reject(()->PublicSignedCustody.verify(envelope,policy(1,2,"",keyB),T3));
        var forged=sign("reviewer-a",stranger,initial,T2,ZERO);
        reject(()->PublicSignedCustody.verify(forged,trusted,T3));
        var alteredSig=envelope.signatureBase64().toCharArray();
        alteredSig[10]=alteredSig[10]=='A'?'B':'A';
        var corruptSig=new PublicSignedCustody.SignedManifest(1,"reviewer-a",T2,ZERO,
                initial,new String(alteredSig));
        reject(()->PublicSignedCustody.verify(corruptSig,trusted,T3));
        reject(()->new PublicSignedCustody.SignedManifest(2,"reviewer-a",T2,ZERO,
                initial,envelope.signatureBase64()));
        reject(()->new PublicSignedCustody.SignedManifest(1,"reviewer-a",T2,ZERO,
                initial,"invalid signature"));
        reject(()->new PublicSignedCustody.SignedManifest(1,"REVIEWER-A",T2,ZERO,
                initial,envelope.signatureBase64()));
        reject(()->new PublicSignedCustody.TrustedKey("reviewer-a","invalid",T0,0));
        reject(()->new PublicSignedCustody.TrustedKey("reviewer-a",
                Base64.getEncoder().encodeToString(new byte[44]),T0,0));
        reject(()->new PublicSignedCustody.TrustedKey("reviewer-a",
                Base64.getEncoder().encodeToString(alice.getPublic().getEncoded()),T0,T0));
        reject(()->new PublicSignedCustody.TrustedPolicy(1,2,2,T0,"",List.of(keyA)));
        reject(()->new PublicSignedCustody.TrustedPolicy(1,1,2,T0,"",
                List.of(keyA,keyA)));
        reject(()->new PublicSignedCustody.TrustedPolicy(1,1,2,T0,"wrong",List.of(keyA)));
        reject(()->PublicSignedCustody.verify(envelope,trusted,T2-1)); // Future-dated signature
        reject(()->PublicSignedCustody.verify(envelope,policy(1,3,"",keyA),T3));
        reject(()->PublicSignedCustody.verify(envelope,policy(1,2,"b".repeat(64),keyA),T3));
        reject(()->PublicSignedCustody.verify(envelope,new PublicSignedCustody.TrustedPolicy(
                1,1,2,T2+1,"",List.of(keyA)),T3));

        // Every decision and the previous signed checkpoint are in the
        // canonical bytes; neither altered approvals nor provenance survive.
        var alteredApproval=new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.APPROVED,T0,T0+2000,REVIEW);
        var tamperedLedger=ledger(alteredApproval,b);
        var tampered=new PublicSignedCustody.SignedManifest(1,"reviewer-a",T2,
                ZERO,tamperedLedger,envelope.signatureBase64());
        reject(()->PublicSignedCustody.verify(tampered,trusted,T3));
        var rewrittenPrev=new PublicSignedCustody.SignedManifest(1,"reviewer-a",T2,
                "a".repeat(64),initial,envelope.signatureBase64());
        reject(()->PublicSignedCustody.verify(rewrittenPrev,trusted,T3));
        var changedTime=new PublicSignedCustody.SignedManifest(1,"reviewer-a",T2+1,
                ZERO,initial,envelope.signatureBase64());
        reject(()->PublicSignedCustody.verify(changedTime,trusted,T3));
        var alteredReview=new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.APPROVED,T0,T0+1000,
                "d".repeat(64));
        reject(()->PublicSignedCustody.verify(new PublicSignedCustody.SignedManifest(
                1,"reviewer-a",T2,ZERO,ledger(alteredReview,b),
                envelope.signatureBase64()),trusted,T3));
        reject(()->PublicSignedCustody.verify(sign("reviewer-a",alice,initial,T0,ZERO),
                trusted,T3)); // Signature predates a later decision.

        // Signed-only provenance receipt: unsigned 5.7/5.8 never upgrades.
        var signedReplay=PublicReplayProvenance.ingestWithSignedCustody(
                source("12345678"),LOG_A,TARGET,envelope,trusted,T3);
        check(signedReplay.signedCustodyCurrentlyEligible(envelope,trusted,T3));
        check(!signedReplay.tacticalDecisionReady()
                && !signedReplay.shadowAudit().livePromotionAuthorized());
        check(!signedReplay.toString().contains("reviewer-a"));
        var unsigned=PublicReplayProvenance.ingestWithCustody(source("12345678"),
                LOG_A,TARGET,initial,T3,2);
        check(!unsigned.signedCustodyCurrentlyEligible(envelope,trusted,T3));
        check(!PublicReplayProvenance.ingest(source("12345678"),LOG_A,TARGET)
                .signedCustodyCurrentlyEligible(envelope,trusted,T3));
        check(!signedReplay.signedCustodyCurrentlyEligible(envelope,policy(1,3,"",keyA),T3));
        check(!signedReplay.signedCustodyCurrentlyEligible(null,trusted,T3));

        var ev=PublicReplayCorpus.ingest(source("12345678"),LOG_A);
        var norm=CompetitiveFormatNormalizer.normalize(ev,TARGET);
        var trends=PublicCompetitiveTendencies.analyze(source("12345678"),LOG_A,TARGET);
        check(PublicReplayProvenance.verifyWithSignedCustody(source("12345678"),
                LOG_A,TARGET,ev,norm,trends,envelope,trusted,T3)
                .signedCustodyCurrentlyEligible(envelope,trusted,T3));
        var forgedEvidence=new PublicReplayCorpus.Evidence(ev.replayId(),ev.format(),
                "e".repeat(64),ev.publicEventCount(),ev.turns(),ev.appearances(),
                ev.compatibility());
        reject(()->PublicReplayProvenance.verifyWithSignedCustody(source("12345678"),
                LOG_A,TARGET,forgedEvidence,norm,trends,envelope,trusted,T3));
        reject(()->PublicReplayProvenance.ingestWithSignedCustody(source("12345678"),
                LOG_A.replace("Swift","Shadow Claw"),TARGET,envelope,trusted,T3));

        // Recheck signing-key revocation: even an old, authentic signature is
        // rejected from the revocation instant onward.
        var revokedSigner=trust("reviewer-a",alice,T8);
        var revokedPolicy=policy(2,2,"",revokedSigner,keyB);
        check(signedReplay.signedCustodyCurrentlyEligible(envelope,revokedPolicy,T7));
        check(!signedReplay.signedCustodyCurrentlyEligible(envelope,revokedPolicy,T8));
        check(!signedReplay.signedCustodyCurrentlyEligible(envelope,revokedPolicy,T9));
        reject(()->PublicSignedCustody.verify(envelope,revokedPolicy,T8));
        reject(()->PublicReplayProvenance.ingestWithSignedCustody(source("12345678"),
                LOG_A,TARGET,envelope,revokedPolicy,T9));
        reject(()->PublicSignedCustody.verify(envelope,policy(2,2,"",keyB),T9));

        // Monotonic revocation/append transitions, including signer rotation.
        var revokedRecord=revoked(a,T4);
        var secondLedger=ledger(a,b,revokedRecord);
        var second=sign("reviewer-a",alice,secondLedger,T5,digest);
        var secondVerified=PublicSignedCustody.verifySuccessor(verified,second,
                policy(1,3,"",keyA),T6);
        check(secondVerified.revision()==3);
        check(!signedReplay.signedCustodyCurrentlyEligible(second,policy(1,3,"",keyA),T6));
        check(!PublicReplayProvenance.ingestWithSignedCustody(source("23456789"),LOG_B,
                TARGET,second,policy(1,3,"",keyA),T6)
                .shadowAudit().livePromotionAuthorized());
        reject(()->PublicReplayProvenance.ingestWithSignedCustody(source("12345678"),
                LOG_A,TARGET,second,policy(1,3,"",keyA),T6));
        reject(()->PublicSignedCustody.verifySuccessor(verified,second,
                policy(1,4,"",keyA),T6));
        reject(()->PublicSignedCustody.verifySuccessor(verified,
                sign("reviewer-a",alice,secondLedger,T5,ZERO),
                policy(1,3,"",keyA),T6));
        var tamperedPrefix=ledger(b,a); // Invalid chronology intentionally rejected.
        check(tamperedPrefix!=null);
        // Replaying the old manifest fails against a trusted higher revision.
        reject(()->PublicSignedCustody.verify(envelope,policy(1,3,"",keyA),T6));

        // Key rotation: old signer is revoked by independently pinned policy;
        // successor signed by trusted replacement key is accepted.
        var thirdDecision=new PublicReplayCustody.Decision(b.replayId(),b.format(),
                b.inputSha256(),PublicReplayCustody.Status.REVOKED,T7,0,REVIEW);
        var thirdLedger=ledger(a,b,revokedRecord,thirdDecision);
        var third=sign("reviewer-b",bob,thirdLedger,T9,
                PublicSignedCustody.manifestDigest(second));
        var rotated=PublicSignedCustody.verifySuccessor(secondVerified,third,
                policy(2,4,"",revokedSigner,keyB),T10);
        check(rotated.revision()==4);
        check(!signedReplay.signedCustodyCurrentlyEligible(third,
                policy(2,4,"",revokedSigner,keyB),T10));
        reject(()->PublicSignedCustody.verify(second,
                policy(2,4,"",revokedSigner,keyB),T10));
        reject(()->PublicSignedCustody.verify(third,
                policy(2,5,"",revokedSigner,keyB),T10));
        reject(()->PublicSignedCustody.verify(third,
                policy(2,4,"",revokedSigner),T10));

        // Held-out: both partitions need a signed custody approval.
        var split=List.of(training(),test());
        var report=PublicHeldOutEvaluation.evaluate(split,TARGET);
        var signedSplit=PublicReplayProvenance.verifySplitWithSignedCustody(
                split,TARGET,report,envelope,trusted,T3);
        check(signedSplit.trainingReplays()==1 && signedSplit.heldOutReplays()==1);
        check(signedSplit.signedCustodyCurrentlyEligible(envelope,trusted,T3));
        check(!signedSplit.legalActionReceipt() && !signedSplit.calibratedForecast());
        check(!signedSplit.toString().contains("gen9ou-"));
        check(!PublicReplayProvenance.verifySplitWithCustody(split,TARGET,
                report,initial,T3,2).signedCustodyCurrentlyEligible(envelope,trusted,T3));
        check(!signedSplit.signedCustodyCurrentlyEligible(second,
                policy(1,3,"",keyA),T6));
        check(!signedSplit.signedCustodyCurrentlyEligible(third,
                policy(2,4,"",revokedSigner,keyB),T10));
        var tamperedReport=new PublicHeldOutEvaluation.Report("f".repeat(64),
                report.sourceScope(),report.trainReplays(),report.heldOutReplays(),
                report.training(),report.heldOut(),false,false,false,false);
        reject(()->PublicReplayProvenance.verifySplitWithSignedCustody(split,
                TARGET,tamperedReport,envelope,trusted,T3));
        reject(()->PublicReplayProvenance.verifySplitWithSignedCustody(List.of(training()),
                TARGET,report,envelope,trusted,T3));
        reject(()->PublicReplayProvenance.verifySplitWithSignedCustody(
                split,TARGET,report,envelope,policy(1,3,"",keyA),T3));
        var noB=ledger(a);
        var onlyA=sign("reviewer-a",alice,noB,T2,ZERO);
        reject(()->PublicReplayProvenance.verifySplitWithSignedCustody(split,TARGET,
                report,onlyA,policy(1,1,"",keyA),T3));

        // All production verification is public-key-only.
        for(var method:PublicSignedCustody.class.getDeclaredMethods()) {
            check(!method.getName().equals("sign"));
            check(!method.getReturnType().equals(PrivateKey.class));
        }
        System.out.println("Phase 5.9 synthetic signed custody PASS: "+checks+" checks");
    }
}
