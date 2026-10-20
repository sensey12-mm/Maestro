package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Synthetic only: source custody, missing groups, replay overlaps and aggregate masking. */
final class PublicCompetitiveCoverageAuditRegression {
    private PublicCompetitiveCoverageAuditRegression() {}
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("Phase 5.11 coverage/uncertainty");
        checks++;
    }
    private static void reject(Runnable operation) {
        try { operation.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_SYNTHETIC_PERSON"));
            return;
        }
        throw new AssertionError("Unsupported competitive coverage evidence accepted");
    }
    private static final long BASE=1_700_000_000L;
    private static final String REVIEW="c".repeat(64), ZERO="0".repeat(64);
    private static final CompetitiveFormatNormalizer.Target PINNED =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static PublicReplayCorpus.Source source(String id,String format) {
        return new PublicReplayCorpus.Source(format+"-"+id,format,true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static String log(String format,int index,String mode) {
        String tier=format.equals("gen9ou")?"[Gen 9] OU":"[Gen 9] Ubers";
        StringBuilder s=new StringBuilder("|gen|9\n|gametype|singles\n|tier|"+tier+"\n")
                .append("|player|p1|PRIVATE_SYNTHETIC_PERSON\n|start\n")
                .append("|switch|p1a: Friend|Pikachu, L100|100/100\n")
                .append("|switch|p2a: Disguised|Zoroark, L100|100/100\n")
                .append("|turn|1\n")
                .append("|move|p2a: Disguised|Swift|p1a: Friend\n");
        if (mode.equals("return")) {
            s.append("|switch|p2a: Other|Pikachu, L100|100/100\n")
             .append("|switch|p2a: Disguised|Zoroark, L100|100/100\n");
        }
        if (mode.equals("transform"))
            s.append("|-transform|p2a: Disguised|p1a: Friend\n");
        String move=mode.equals("unknown")?"Imaginary Blade":
                mode.equals("poor")?"Swift":"Shadow Claw";
        for (int i=0;i<25+index;i++)
            s.append("|move|p2a: Disguised|").append(move)
                    .append("|p1a: Friend\n");
        return s.append("|win|PRIVATE_SYNTHETIC_PERSON\n").toString();
    }
    private static PublicHeldOutEvaluation.Replay replay(String id,String format,
                int index,String mode,PublicHeldOutEvaluation.Partition partition) {
        return new PublicHeldOutEvaluation.Replay(source(id,format),
                log(format,index,mode),partition);
    }
    private static final class Fixture {
        final List<PublicHeldOutEvaluation.Replay> split;
        final PublicHeldOutEvaluation.Report held;
        final PublicSignedCustody.SignedManifest manifest;
        final PublicSignedCustody.TrustedPolicy policy;
        final KeyPair signingKey;
        Fixture(List<PublicHeldOutEvaluation.Replay> supplied,KeyPair key) {
            split=List.copyOf(supplied);
            signingKey=key;
            held=PublicHeldOutEvaluation.evaluate(split,PINNED);
            var decisions=new ArrayList<PublicReplayCustody.Decision>();
            int i=0;
            for (var replay:split) {
                var approved=PublicReplayCorpus.ingest(replay.source(),replay.spectatorLog());
                decisions.add(new PublicReplayCustody.Decision(
                        approved.replayId(),approved.format(),approved.inputSha256(),
                        PublicReplayCustody.Status.APPROVED,BASE+(++i),BASE+1000,REVIEW));
            }
            var ledger=new PublicReplayCustody.Ledger(1,decisions.size(),decisions);
            manifest=sign(key,ledger,BASE+100,ZERO);
            var publicKey=new PublicSignedCustody.TrustedKey("auditor",
                    Base64.getEncoder().encodeToString(key.getPublic().getEncoded()),BASE-50,0);
            policy=new PublicSignedCustody.TrustedPolicy(1,1,ledger.revision(),BASE-50,
                    "",List.of(publicKey));
        }
        PublicCompetitiveCoverageAudit.Report audit() {
            return PublicCompetitiveCoverageAudit.audit(split,PINNED,held,manifest,policy,BASE+150);
        }
    }
    private static KeyPair newKey() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch (GeneralSecurityException error) { throw new AssertionError(error); }
    }
    private static PublicSignedCustody.SignedManifest sign(KeyPair pair,
                 PublicReplayCustody.Ledger ledger,long at,String previous) {
        try {
            byte[] bytes=PublicSignedCustody.canonicalBytes(1,"auditor",at,previous,ledger);
            Signature ed=Signature.getInstance("Ed25519");
            ed.initSign(pair.getPrivate());ed.update(bytes);
            return new PublicSignedCustody.SignedManifest(1,"auditor",at,previous,ledger,
                    Base64.getEncoder().encodeToString(ed.sign()));
        } catch (GeneralSecurityException error) { throw new AssertionError(error); }
    }
    private static PublicCompetitiveCoverageAudit.Group group(
          PublicCompetitiveCoverageAudit.Report r,String format,
          PublicCompetitiveCoverageAudit.Action action,
          PublicCompetitiveCoverageAudit.Uncertainty uncertainty) {
        return r.groups().stream().filter(g->g.format().equals(format)
                && g.action()==action && g.uncertainty()==uncertainty).findFirst().orElseThrow();
    }
    static void run() {
        var inputs=new ArrayList<PublicHeldOutEvaluation.Replay>();
        inputs.add(replay("10000001","gen9ou",0,"strong",
                PublicHeldOutEvaluation.Partition.TRAIN));
        // Good looking aggregate: one format excels, the other only ties.
        for(int i=0;i<3;i++)
            inputs.add(replay("2000000"+i,"gen9ou",i+1,"strong",
                    PublicHeldOutEvaluation.Partition.HELD_OUT));
        for(int i=0;i<3;i++)
            inputs.add(replay("3000000"+i,"gen9ubers",i+2,"poor",
                    PublicHeldOutEvaluation.Partition.HELD_OUT));
        inputs.add(replay("40000001","gen9ou",4,"return",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        inputs.add(replay("40000002","gen9ou",5,"unknown",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        inputs.add(replay("40000003","gen9ubers",6,"transform",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        var sample=new Fixture(inputs,newKey());
        var report=sample.audit();
        check(report.groups().size()==20);
        check(report.heldOutReplays()==9 && report.trainingReplays()==1);
        check(report.splitHash().equals(sample.held.splitSha256()));
        check(report.heldOutSkippedUnknown()==sample.held.heldOut().unscoredPublicDecisions());
        check(!report.crossReplayIdentityUsed());
        check(!report.readyForRealWorldPerformanceClaims() && !report.shadowTacticalReady()
                && !report.livePromotionAuthorized());
        check(!report.toString().contains("PRIVATE_SYNTHETIC_PERSON")
                && !report.toString().contains("gen9ou-"));
        check(!report.toString().contains("Disguised"));
        check(Arrays.stream(PublicCompetitiveCoverageAudit.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        try { report.groups().clear(); throw new AssertionError("mutable groups"); }
        catch (UnsupportedOperationException expected) { checks++; }

        var all=PublicCompetitiveCoverageAudit.Uncertainty.ALL;
        var move=PublicCompetitiveCoverageAudit.Action.MOVE;
        var sw=PublicCompetitiveCoverageAudit.Action.SWITCH;
        var ou=group(report,"gen9ou",move,all);
        var uber=group(report,"gen9ubers",move,all);
        check(ou.independentReplays()==5 && ou.pairedHorizonCount()>=30);
        check(ou.candidateHits()>ou.baselineHits());
        check(ou.thresholdMet());
        check(uber.independentReplays()==4);
        check(!uber.thresholdMet() && uber.gate()==PublicCompetitiveCoverageAudit.Gate.BASELINE_NOT_BEATEN);
        check(report.aggregateMaskingRisk());
        check(sample.held.heldOut().moves().candidateHits()
                >sample.held.heldOut().moves().baselineHits());
        var zero=group(report,"gen9ubers",sw,
                PublicCompetitiveCoverageAudit.Uncertainty.RETURN_DISPLAY_AMBIGUOUS);
        check(zero.independentReplays()==0);
        check(zero.gate()==PublicCompetitiveCoverageAudit.Gate.NO_REPLAYS);
        var rare=group(report,"gen9ou",move,
                PublicCompetitiveCoverageAudit.Uncertainty.UNKNOWN_MOVE_SOURCE);
        check(rare.independentReplays()==1);
        check(rare.gate()==PublicCompetitiveCoverageAudit.Gate.TOO_FEW_INDEPENDENT_REPLAYS);
        var returned=group(report,"gen9ou",move,
                PublicCompetitiveCoverageAudit.Uncertainty.RETURN_DISPLAY_AMBIGUOUS);
        check(returned.independentReplays()==1);
        var transformed=group(report,"gen9ubers",move,
                PublicCompetitiveCoverageAudit.Uncertainty.TRANSFORM_OBSERVED);
        check(transformed.independentReplays()==1);
        check(transformed.pairedHorizonCount()==0);
        check(group(report,"gen9ou",move,
                PublicCompetitiveCoverageAudit.Uncertainty.ORDINARY_IDENTITY_UNCONFIRMED)
                .independentReplays()==3);
        check(group(report,"gen9ubers",move,
                PublicCompetitiveCoverageAudit.Uncertainty.ORDINARY_IDENTITY_UNCONFIRMED)
                .independentReplays()==3);

        var readiness=report.readiness();
        check(readiness.minimumReplaysPerStratum()==3);
        check(readiness.minimumPairedHorizonsPerStratum()==30);
        check(readiness.bothFormatsCovered());
        check(!readiness.bothActionsCovered());
        check(!readiness.allUncertaintyStrataCovered());
        check(!readiness.baselineGainAcrossRequiredStrata());
        check(!readiness.evidenceRightsIndependentlyVerified()
                && !readiness.realReplayEvaluationCompleted()
                && !readiness.tacticalPromotionAuthorized());
        check(report.groups().stream().noneMatch(g->g.pairedHorizonCount()<30
                && g.gate()==PublicCompetitiveCoverageAudit.Gate.OBSERVATIONAL_COVERAGE_ONLY));

        // Call order cannot affect de-identified report, replay split or score.
        var reversed=new ArrayList<>(inputs);
        Collections.reverse(reversed);
        var reordered=PublicCompetitiveCoverageAudit.audit(reversed,PINNED,
                sample.held,sample.manifest,sample.policy,BASE+150);
        check(reordered.groups().equals(report.groups()));
        check(reordered.splitHash().equals(report.splitHash()));
        check(reordered.readiness().equals(report.readiness()));

        // A replaced training replay may not alter scored test evidence.
        var replaced=new ArrayList<>(inputs);
        replaced.set(0,replay("10000002","gen9ou",7,"poor",
                PublicHeldOutEvaluation.Partition.TRAIN));
        var newFixture=new Fixture(replaced,newKey());
        check(newFixture.audit().groups().equals(report.groups()));
        check(!newFixture.audit().splitHash().equals(report.splitHash()));

        // Signed custody is required: missing, revoked, stale, forged, and
        // mismatched approval identities must never confer readiness.
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,null,
                sample.policy,BASE+150));
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,
                sample.manifest,null,BASE+150));
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,
                sample.manifest,sample.policy,BASE+50)); // Future-dated signature.
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,
                sample.manifest,sample.policy,BASE+1001)); // Expired approval.
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","other","unknown"),
                sample.held,sample.manifest,sample.policy,BASE+150));
        var forgedPolicy=new PublicSignedCustody.TrustedPolicy(1,1,
                sample.policy.minimumRevision()+1,BASE-50,"",sample.policy.keys());
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,
                sample.manifest,forgedPolicy,BASE+150));

        // Publicly constructed summaries are never trusted by themselves.
        var fakeMetric=PublicHeldOutEvaluation.Metric.of(100,99,1,
                "FIRST_OBSERVED_NATIVE_MOVE");
        var old=sample.held.heldOut();
        var forgedSummary=new PublicHeldOutEvaluation.Report(
                sample.held.splitSha256(),sample.held.sourceScope(),
                sample.held.trainReplays(),sample.held.heldOutReplays(),
                sample.held.training(),new PublicHeldOutEvaluation.Segment(
                        old.acceptedReplays(),old.unsupportedTargetReplays(),
                        old.unscoredPublicDecisions(),fakeMetric,old.switches()),
                false,false,false,false);
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,forgedSummary,
                sample.manifest,sample.policy,BASE+150));

        // A duplicate renamed log must fail even when signed and approved:
        // source rights alone cannot defeat Phase 5.4's overlap guard.
        var duplicate=new ArrayList<>(inputs);
        var original=inputs.get(1);
        duplicate.add(new PublicHeldOutEvaluation.Replay(
                source("99999999","gen9ou"),
                original.spectatorLog().replace("PRIVATE_SYNTHETIC_PERSON","ANOTHER_PLAYER")
                        .replace("Disguised","DifferentNick"),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        reject(()->PublicHeldOutEvaluation.evaluate(duplicate,PINNED));

        var changed=new ArrayList<>(inputs);
        var chosen=inputs.get(2);
        changed.set(2,new PublicHeldOutEvaluation.Replay(chosen.source(),
                chosen.spectatorLog().replace("Shadow Claw","Imaginary Blade"),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        reject(()->PublicCompetitiveCoverageAudit.audit(changed,PINNED,sample.held,
                sample.manifest,sample.policy,BASE+150));
        var privateLog=new ArrayList<>(inputs);
        var base=inputs.get(2);
        privateLog.set(2,new PublicHeldOutEvaluation.Replay(base.source(),
                base.spectatorLog()+"|request|PRIVATE_SYNTHETIC_PERSON\n",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        reject(()->PublicCompetitiveCoverageAudit.audit(privateLog,PINNED,sample.held,
                sample.manifest,sample.policy,BASE+150));

        // Reject a genuine signed successor if one held-out replay is revoked.
        var decisions=new ArrayList<>(sample.manifest.ledger().decisions());
        var targetDecision=decisions.get(2);
        decisions.add(new PublicReplayCustody.Decision(targetDecision.replayId(),
                targetDecision.format(),targetDecision.inputSha256(),
                PublicReplayCustody.Status.REVOKED,BASE+101,0,REVIEW));
        var revoked=new PublicReplayCustody.Ledger(1,decisions.size(),decisions);
        var revokeManifest=sign(sample.signingKey,revoked,BASE+102,
                PublicSignedCustody.manifestDigest(sample.manifest));
        var laterPolicy=new PublicSignedCustody.TrustedPolicy(1,1,revoked.revision(),BASE-50,
                "",sample.policy.keys());
        reject(()->PublicCompetitiveCoverageAudit.audit(inputs,PINNED,sample.held,
                revokeManifest,laterPolicy,BASE+150));

        // Aggregate truth does not turn into a calibrated move probability.
        check(report.groups().stream().noneMatch(g->g.toString().contains("PRIVATE_SYNTHETIC_PERSON")));
        System.out.println("Phase 5.11 synthetic competitive coverage PASS: "+checks+" checks");
    }
}
