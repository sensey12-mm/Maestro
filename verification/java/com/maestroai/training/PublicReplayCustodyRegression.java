package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Offline-only Phase 5.8 custody tests; timestamps, reviews and logs are
 * synthetic. Passing does NOT independently establish replay permissions.
 */
final class PublicReplayCustodyRegression {
    private PublicReplayCustodyRegression() {}
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("Phase 5.8 custody gate");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SYNTHETIC_PRIVATE_PERSON"));
            return;
        }
        throw new AssertionError("Unapproved/stale custody was accepted");
    }
    private static final String LOG1="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SYNTHETIC_PRIVATE_PERSON\n|start\n"
            +"|switch|p1a: Friend|Pikachu, L100|100/100\n"
            +"|switch|p2a: Mask|Zoroark, L100|100/100\n|turn|1\n"
            +"|move|p2a: Mask|Swift|p1a: Friend\n"
            +"|switch|p2a: Mask|Zoroark, L100|100/100\n|turn|2\n"
            +"|move|p2a: Mask|Nasty Plot|p1a: Friend\n"
            +"|win|SYNTHETIC_PRIVATE_PERSON\n";
    private static final String LOG2="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SYNTHETIC_OTHER_PERSON\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
            +"|switch|p2a: Rival|Ditto, L100|100/100\n|turn|1\n"
            +"|move|p2a: Rival|Tackle|p1a: Ally\n"
            +"|win|SYNTHETIC_OTHER_PERSON\n";
    private static final long T0=1_700_000_000L,T1=T0+10,T2=T0+20,T3=T0+30;
    private static final long EXP=T0+100;
    private static final String REVIEW="e".repeat(64);
    private static final CompetitiveFormatNormalizer.Target TARGET =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static PublicReplayCorpus.Source source(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static PublicReplayCorpus.Evidence accepted(PublicReplayCorpus.Source s,String log) {
        return PublicReplayCorpus.ingest(s,log);
    }
    private static PublicReplayCustody.Decision approval(String id,String log,long epoch,long expires) {
        var src=source(id);
        var admitted=accepted(src,log);
        return new PublicReplayCustody.Decision(src.replayId(),src.format(),
                admitted.inputSha256(),PublicReplayCustody.Status.APPROVED,epoch,expires,REVIEW);
    }
    private static PublicReplayCustody.Decision decision(PublicReplayCustody.Decision original,
                                                        PublicReplayCustody.Status status,long epoch) {
        return new PublicReplayCustody.Decision(original.replayId(),original.format(),
                original.inputSha256(),status,epoch,0,REVIEW);
    }
    private static PublicReplayCustody.Ledger ledger(PublicReplayCustody.Decision... decisions) {
        return new PublicReplayCustody.Ledger(1,decisions.length,List.of(decisions));
    }
    private static PublicReplayProvenance.CheckedReplay issue(
            PublicReplayCustody.Ledger ledger,long evaluatedAt,long minRevision) {
        return PublicReplayProvenance.ingestWithCustody(source("12345678"),LOG1,TARGET,
                ledger,evaluatedAt,minRevision);
    }
    private static PublicHeldOutEvaluation.Replay train() {
        return new PublicHeldOutEvaluation.Replay(source("12345678"),LOG1,
                PublicHeldOutEvaluation.Partition.TRAIN);
    }
    private static PublicHeldOutEvaluation.Replay test() {
        return new PublicHeldOutEvaluation.Replay(source("23456789"),LOG2,
                PublicHeldOutEvaluation.Partition.HELD_OUT);
    }

    static void run() {
        var a=approval("12345678",LOG1,T0,EXP);
        var b=approval("23456789",LOG2,T1,EXP);
        var l1=ledger(a);
        var l2=ledger(a,b);
        check(l1.schemaVersion()==1 && l1.revision()==1);
        check(l2.revision()==2 && l2.decisions().size()==2);
        check(l2.decisions().get(0).status()==PublicReplayCustody.Status.APPROVED);

        var initial=issue(l1,T2,1);
        check(initial.custodyCurrentlyEligible(l1,T2,1));
        check(initial.custodyCurrentlyEligible(l2,T2,2));
        check(!initial.custodyCurrentlyEligible(l1,T2,2));
        check(!initial.currentBattleBound() && !initial.independentlyAuthorized()
                && !initial.tacticalDecisionReady());
        check(!initial.shadowAudit().shadowTacticalReady()
                && !initial.shadowAudit().livePromotionAuthorized());
        check(!initial.toString().contains("gen9ou-")
                && !initial.toString().contains("SYNTHETIC_PRIVATE_PERSON")
                && !initial.toString().contains(a.inputSha256()));
        try { l2.decisions().clear(); throw new AssertionError("mutable ledger"); }
        catch (UnsupportedOperationException expected) { checks++; }
        check(Arrays.stream(PublicReplayProvenance.CheckedReplay.class.getDeclaredConstructors())
                .allMatch(c -> Modifier.isPrivate(c.getModifiers())));

        // Ordinary consistency-only Phase 5.7 verification is NOT custody approval.
        var ordinary=PublicReplayProvenance.ingest(source("12345678"),LOG1,TARGET);
        check(!ordinary.custodyCurrentlyEligible(l2,T2,1));
        check(!ordinary.custodyCurrentlyEligible(null,T2,1));
        var e=accepted(source("12345678"),LOG1);
        var n=CompetitiveFormatNormalizer.normalize(e,TARGET);
        var t=PublicCompetitiveTendencies.analyze(source("12345678"),LOG1,TARGET);
        var fullyVerified=PublicReplayProvenance.verifyWithCustody(source("12345678"),LOG1,
                TARGET,e,n,t,l1,T2,1);
        check(fullyVerified.custodyCurrentlyEligible(l1,T2,1));
        reject(()->PublicReplayProvenance.verifyWithCustody(source("12345678"),LOG1,
                TARGET,e,n,t,null,T2,1));
        var bogusEvidence=new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                "b".repeat(64),e.publicEventCount(),e.turns(),e.appearances(),e.compatibility());
        reject(()->PublicReplayProvenance.verifyWithCustody(source("12345678"),LOG1,
                TARGET,bogusEvidence,n,t,l1,T2,1));

        // Approval ends at expiry itself. No permanent prior receipt can bypass it.
        check(initial.custodyCurrentlyEligible(l1,EXP-1,1));
        check(!initial.custodyCurrentlyEligible(l1,EXP,1));
        check(!initial.custodyCurrentlyEligible(l1,EXP+1,1));
        reject(()->issue(l1,EXP,1));
        reject(()->issue(l1,EXP+1,1));

        // Revocation takes precedence over historical valid approvals.
        var revoked=decision(a,PublicReplayCustody.Status.REVOKED,T3);
        var l3=ledger(a,b,revoked);
        check(!initial.custodyCurrentlyEligible(l3,T3,3));
        check(!initial.custodyCurrentlyEligible(l3,T3+1,3));
        reject(()->issue(l3,T3,3));
        reject(()->issue(l3,T3+1,3));
        reject(()->issue(l1,T3+1,3)); // A trusted latest-revision floor prevents rollback.
        reject(()->issue(l2,T3+1,3));
        // A future-dated revocation in a purported current snapshot also fails.
        reject(()->issue(l3,T2,3));

        // Approval cannot be silently reintroduced after a recorded revocation.
        reject(()->ledger(a,revoked,approval("12345678",LOG1,T3+1,EXP)));
        reject(()->ledger(a,a));
        reject(()->ledger(a,decision(a,PublicReplayCustody.Status.PENDING,T3)));
        reject(()->ledger(b,a)); // Decisions must be ordered by monotonically increasing time.
        reject(()->new PublicReplayCustody.Ledger(2,1,List.of(a)));
        reject(()->new PublicReplayCustody.Ledger(1,2,List.of(a)));
        reject(()->new PublicReplayCustody.Ledger(1,0,List.of()));
        reject(()->new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.APPROVED,T0,T0,REVIEW));
        reject(()->new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.APPROVED,T0,
                T0+PublicReplayCustody.MAX_APPROVAL_SECONDS+1,REVIEW));
        reject(()->new PublicReplayCustody.Decision(a.replayId(),a.format(),
                "not-a-digest",PublicReplayCustody.Status.APPROVED,T0,EXP,REVIEW));
        reject(()->new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.APPROVED,T0,EXP,"unreviewed"));
        reject(()->new PublicReplayCustody.Decision(a.replayId(),a.format(),
                a.inputSha256(),PublicReplayCustody.Status.REVOKED,T0,EXP,REVIEW));
        reject(()->issue(null,T2,1));
        reject(()->issue(l1,T2,0));
        reject(()->issue(l1,0,1));
        reject(()->issue(l1,T2,2));
        reject(()->issue(ledger(b),T2,1)); // Source absent.
        reject(()->issue(ledger(approval("12345678",LOG2,T0,EXP)),T2,1));
        reject(()->PublicReplayProvenance.ingestWithCustody(source("12345678"),
                LOG1.replace("Nasty Plot","Shadow Claw"),TARGET,l1,T2,1));
        reject(()->PublicReplayProvenance.ingestWithCustody(source("12345678"),
                LOG1+"|request|SYNTHETIC_PRIVATE_PERSON_PRIVATE\n",TARGET,l1,T2,1));
        reject(()->PublicReplayProvenance.ingestWithCustody(source("12345678"),
                LOG1,new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4",
                        "unverified","unknown"),l1,T2,1));

        // Pending decisions are not approvals. Explicit later approval can promote
        // pending only before any revocation, and only with matching exact hash.
        var pending=decision(a,PublicReplayCustody.Status.PENDING,T0);
        var wait=ledger(pending);
        reject(()->issue(wait,T2,1));
        var onceApproved=ledger(pending,approval("12345678",LOG1,T1,EXP));
        check(issue(onceApproved,T2,2).custodyCurrentlyEligible(onceApproved,T2,2));
        var pendingRevoked=ledger(pending,decision(a,PublicReplayCustody.Status.REVOKED,T1));
        reject(()->issue(pendingRevoked,T2,2));

        // Train and held-out inputs must EACH have a current separate approval.
        var pair=List.of(train(),test());
        var report=PublicHeldOutEvaluation.evaluate(pair,TARGET);
        var ordinarySplit=PublicReplayProvenance.verifySplit(pair,TARGET,report);
        check(!ordinarySplit.custodyCurrentlyEligible(l2,T2,2));
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,report,l1,T2,1));
        var checkedSplit=PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,report,l2,T2,2);
        check(checkedSplit.trainingReplays()==1 && checkedSplit.heldOutReplays()==1);
        check(checkedSplit.custodyCurrentlyEligible(l2,T2,2));
        check(!checkedSplit.custodyCurrentlyEligible(l1,T2,2));
        check(!checkedSplit.custodyCurrentlyEligible(l3,T3,3));
        check(!checkedSplit.custodyCurrentlyEligible(l2,EXP,2));
        check(!checkedSplit.calibratedForecast() && !checkedSplit.legalActionReceipt());
        check(!checkedSplit.toString().contains("gen9ou-")
                && !checkedSplit.toString().contains("SYNTHETIC_PRIVATE_PERSON"));
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,report,l3,T3,3));
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,report,l2,T2,3));
        var tampered=new PublicHeldOutEvaluation.Report("f".repeat(64),
                report.sourceScope(),report.trainReplays(),report.heldOutReplays(),
                report.training(),report.heldOut(),false,false,false,false);
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,tampered,l2,T2,2));
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                pair,TARGET,null,l2,T2,2));
        reject(()->PublicReplayProvenance.verifySplitWithCustody(
                List.of(train(),train()),TARGET,report,l2,T2,2));
        // Receipt cannot be turned into a policy authorization by any path.
        check(initial.shadowAudit().blockers().contains(
                PublicCompetitiveShadowContract.Blocker.HISTORICAL_REPLAY_NOT_ACTIVE_BATTLE));
        System.out.println("Phase 5.8 synthetic custody/revocation PASS: "+checks+" checks");
    }
}
