package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.12: synthetic, privacy-safe and externally pin-required only. */
final class PublicCompetitiveRobustnessAuditRegression {
    private PublicCompetitiveRobustnessAuditRegression() {}
    private static int checks;
    private static void check(boolean valid) {
        if(!valid) throw new AssertionError("Phase 5.12 preregistered robustness");
        checks++;
    }
    private static void reject(Runnable task) {
        try { task.run(); }
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SYNTHETIC_PRIVATE_PERSON"));
            return;
        }
        throw new AssertionError("Unregistered or unverified held-out evidence accepted");
    }
    private static final long T0=1_700_000_000L;
    private static final String ZERO="0".repeat(64),REVIEW="c".repeat(64);
    private static final CompetitiveFormatNormalizer.Target PIN =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final String FORMAT_OU="gen9ou",FORMAT_UB="gen9ubers";
    private static PublicReplayCorpus.Source source(String id,String format) {
        return new PublicReplayCorpus.Source(format+"-"+id,format,true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static String log(String format,int index,String mode) {
        String tier=format.equals(FORMAT_OU)?"[Gen 9] OU":"[Gen 9] Ubers";
        StringBuilder s=new StringBuilder("|gen|9\n|gametype|singles\n|tier|"+tier+"\n")
                .append("|player|p1|SYNTHETIC_PRIVATE_PERSON\n|start\n")
                .append("|switch|p1a: Good|Pikachu, L100|100/100\n")
                .append("|switch|p2a: Mystery|Zoroark, L100|100/100\n")
                .append("|turn|1\n")
                .append("|move|p2a: Mystery|Swift|p1a: Good\n");
        if(mode.equals("return")) {
            s.append("|switch|p2a: Other|Pikachu, L100|100/100\n")
             .append("|switch|p2a: Mystery|Zoroark, L100|100/100\n");
        }
        if(mode.equals("transform"))
            s.append("|-transform|p2a: Mystery|p1a: Good\n");
        if(mode.equals("own-transform"))
            s.append("|-transform|p1a: Good|p2a: Mystery\n")
             .append("|move|p1a: Good|Imaginary Blade|p2a: Mystery\n");
        String chosen=mode.equals("unknown")?"Imaginary Blade":
                mode.equals("flat")?"Swift":"Shadow Claw";
        for(int i=0;i<24+index;i++)
            s.append("|move|p2a: Mystery|").append(chosen).append("|p1a: Good\n");
        return s.append("|win|SYNTHETIC_PRIVATE_PERSON\n").toString();
    }
    private static PublicHeldOutEvaluation.Replay replay(String id,String format,
             int index,String mode,PublicHeldOutEvaluation.Partition partition) {
        return new PublicHeldOutEvaluation.Replay(source(id,format),
                log(format,index,mode),partition);
    }
    private static KeyPair key() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch(GeneralSecurityException error) { throw new AssertionError(error); }
    }
    private static PublicSignedCustody.SignedManifest sign(KeyPair key,
            PublicReplayCustody.Ledger ledger,long at) {
        try {
            Signature signer=Signature.getInstance("Ed25519");
            signer.initSign(key.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"reviewer",at,ZERO,ledger));
            return new PublicSignedCustody.SignedManifest(1,"reviewer",at,ZERO,
                    ledger,Base64.getEncoder().encodeToString(signer.sign()));
        } catch(GeneralSecurityException error) { throw new AssertionError(error); }
    }
    private static final class Fixture {
        final List<PublicHeldOutEvaluation.Replay> replaySplit;
        final PublicHeldOutEvaluation.Report held;
        final PublicSignedCustody.SignedManifest signed;
        final PublicSignedCustody.TrustedPolicy trust;
        final PublicCompetitiveRobustnessAudit.Registration registration;
        Fixture(List<PublicHeldOutEvaluation.Replay> split) {
            replaySplit=List.copyOf(split);
            held=PublicHeldOutEvaluation.evaluate(replaySplit,PIN);
            KeyPair keys=key();
            var approvals=new ArrayList<PublicReplayCustody.Decision>();
            int seq=0;
            for(var input:replaySplit) {
                var admitted=PublicReplayCorpus.ingest(input.source(),input.spectatorLog());
                approvals.add(new PublicReplayCustody.Decision(
                        admitted.replayId(),admitted.format(),admitted.inputSha256(),
                        PublicReplayCustody.Status.APPROVED,T0+(++seq),T0+2000,REVIEW));
            }
            var ledger=new PublicReplayCustody.Ledger(1,approvals.size(),approvals);
            signed=sign(keys,ledger,T0+500);
            var publicKey=new PublicSignedCustody.TrustedKey("reviewer",
                    Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),T0-50,0);
            trust=new PublicSignedCustody.TrustedPolicy(1,1,ledger.revision(),T0-50,
                    "",List.of(publicKey));
            registration=new PublicCompetitiveRobustnessAudit.Registration(1,
                    PublicCompetitiveRobustnessAudit.frozenProtocolSha256(),
                    held.splitSha256());
        }
        PublicCompetitiveRobustnessAudit.Report evaluate() {
            return PublicCompetitiveRobustnessAudit.audit(replaySplit,PIN,held,signed,
                    trust,T0+550,registration);
        }
    }
    private static PublicCompetitiveRobustnessAudit.Stratum stratum(
            PublicCompetitiveRobustnessAudit.Report report,String fmt,
            PublicCompetitiveCoverageAudit.Action action,
            PublicCompetitiveCoverageAudit.Uncertainty uncertainty) {
        return report.strata().stream().filter(s->s.format().equals(fmt)
                && s.action()==action && s.uncertainty()==uncertainty).findFirst().orElseThrow();
    }
    private static PublicCompetitiveRobustnessAudit.Stress stress(
            PublicCompetitiveRobustnessAudit.Report report,
            PublicCompetitiveCoverageAudit.Action action,
            PublicCompetitiveCoverageAudit.Uncertainty uncertainty,
            PublicCompetitiveRobustnessAudit.Shift shift) {
        return report.stressScenarios().stream().filter(s->s.action()==action
                && s.uncertainty()==uncertainty && s.shift()==shift)
                .findFirst().orElseThrow();
    }
    static void run() {
        var all=PublicCompetitiveCoverageAudit.Uncertainty.ALL;
        var move=PublicCompetitiveCoverageAudit.Action.MOVE;
        var sw=PublicCompetitiveCoverageAudit.Action.SWITCH;
        var shifts=PublicCompetitiveRobustnessAudit.Shift.values();
        var inputs=new ArrayList<PublicHeldOutEvaluation.Replay>();
        inputs.add(replay("10000001",FORMAT_OU,0,"strong",
                PublicHeldOutEvaluation.Partition.TRAIN));
        for(int i=0;i<3;i++) {
            inputs.add(replay("2000000"+i,FORMAT_OU,i+1,"strong",
                    PublicHeldOutEvaluation.Partition.HELD_OUT));
            inputs.add(replay("3000000"+i,FORMAT_UB,i+4,"flat",
                    PublicHeldOutEvaluation.Partition.HELD_OUT));
        }
        inputs.add(replay("40000001",FORMAT_OU,7,"return",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        inputs.add(replay("40000002",FORMAT_OU,8,"unknown",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        inputs.add(replay("40000003",FORMAT_UB,9,"transform",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        Fixture data=new Fixture(inputs);
        var report=data.evaluate();
        check(report.strata().size()==20 && report.stressScenarios().size()==30);
        check(report.protocolSha256().equals(
                PublicCompetitiveRobustnessAudit.frozenProtocolSha256()));
        check(report.testSplitSha256().equals(data.held.splitSha256()));
        check(PublicCompetitiveRobustnessAudit.frozenProtocolDescription().contains("no-fit"));
        check(!report.independentlyPreRegistered() && !report.calibratedForecastProduced());
        check(!report.realWorldSkillVerified() && !report.shadowTacticalReady()
                && !report.livePromotionAuthorized());
        check(report.insufficientSupport() && report.abstainedStressScenarios()>0);
        check(report.formatInstability() && report.pooledMaskingRisk());
        check(!report.observedStabilityAcrossAllRequiredShifts());
        check(data.held.heldOut().moves().candidateHits()>
                data.held.heldOut().moves().baselineHits());
        check(!report.toString().contains("SYNTHETIC_PRIVATE_PERSON")
                && !report.toString().contains("gen9ou-")
                && !report.toString().contains("Mystery"));
        check(Arrays.stream(PublicCompetitiveRobustnessAudit.Report.class
                .getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));
        try { report.strata().clear(); throw new AssertionError("mutable strata"); }
        catch(UnsupportedOperationException expected) { checks++; }
        try { report.stressScenarios().clear(); throw new AssertionError("mutable shifts"); }
        catch(UnsupportedOperationException expected) { checks++; }

        var ou=stratum(report,FORMAT_OU,move,all);
        var uber=stratum(report,FORMAT_UB,move,all);
        check(ou.enoughEvidence() && ou.frozenMarginMet());
        check(uber.enoughEvidence() && !uber.frozenMarginMet());
        check(ou.candidateMinusBaseline()>0 && uber.candidateMinusBaseline()==0);
        check(ou.independentReplays()==5 && uber.independentReplays()==4);
        check(stratum(report,FORMAT_UB,sw,all).pairedHorizons()==0);
        check(!stratum(report,FORMAT_UB,sw,all).enoughEvidence());
        check(stratum(report,FORMAT_OU,move,
                PublicCompetitiveCoverageAudit.Uncertainty.UNKNOWN_MOVE_SOURCE)
                .independentReplays()==1);
        check(stratum(report,FORMAT_UB,move,
                PublicCompetitiveCoverageAudit.Uncertainty.TRANSFORM_OBSERVED)
                .independentReplays()==1);
        check(stratum(report,FORMAT_OU,move,
                PublicCompetitiveCoverageAudit.Uncertainty.RETURN_DISPLAY_AMBIGUOUS)
                .independentReplays()==1);
        check(stress(report,move,all,shifts[0]).finding()
                ==PublicCompetitiveRobustnessAudit.Finding.FROZEN_MARGIN_OBSERVED_ONLY);
        check(stress(report,move,all,shifts[2]).ouPairedHorizons()==ou.pairedHorizons());
        check(stress(report,sw,all,shifts[1]).finding()
                ==PublicCompetitiveRobustnessAudit.Finding.ABSTAIN_INSUFFICIENT_STRATA);
        for(var s:report.stressScenarios())
            if(s.finding()==PublicCompetitiveRobustnessAudit.Finding.ABSTAIN_INSUFFICIENT_STRATA)
                check(!s.empiricalMarginMet());
        check(shifts[0].ouWeight()==90 && shifts[0].ubersWeight()==10);
        check(shifts[1].ouWeight()==50 && shifts[1].ubersWeight()==50);
        check(shifts[2].ouWeight()==10 && shifts[2].ubersWeight()==90);

        // Frozen preregistration means shuffling traversal order cannot alter
        // attribution, robustness strata, or the pinned split.
        var shuffled=new ArrayList<>(inputs);
        Collections.reverse(shuffled);
        var reshuffled=PublicCompetitiveRobustnessAudit.audit(shuffled,PIN,data.held,
                data.signed,data.trust,T0+550,data.registration);
        check(reshuffled.strata().equals(report.strata()));
        check(reshuffled.stressScenarios().equals(report.stressScenarios()));
        check(reshuffled.testSplitSha256().equals(report.testSplitSha256()));

        // A changed TRAINING replay does not change any held-out group.
        var swapped=new ArrayList<>(inputs);
        swapped.set(0,replay("10000002",FORMAT_OU,10,"flat",
                PublicHeldOutEvaluation.Partition.TRAIN));
        var alt=new Fixture(swapped).evaluate();
        check(alt.strata().equals(report.strata()));
        check(alt.stressScenarios().equals(report.stressScenarios()));
        check(!alt.testSplitSha256().equals(report.testSplitSha256()));

        // Same p2 observations but player-side Transform and unsupported
        // own move MUST NOT classify the opponent as Transformed/Unknown.
        var ownInputs=new ArrayList<PublicHeldOutEvaluation.Replay>();
        ownInputs.add(replay("11111111",FORMAT_OU,0,"strong",
                PublicHeldOutEvaluation.Partition.TRAIN));
        for(int i=0;i<3;i++)
            ownInputs.add(replay("5555555"+i,FORMAT_OU,i+11,
                    "own-transform",PublicHeldOutEvaluation.Partition.HELD_OUT));
        ownInputs.add(replay("66666666",FORMAT_UB,20,"flat",
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        var ownReport=new Fixture(ownInputs).evaluate();
        check(stratum(ownReport,FORMAT_OU,move,
                PublicCompetitiveCoverageAudit.Uncertainty.ORDINARY_IDENTITY_UNCONFIRMED)
                .independentReplays()==3);
        check(stratum(ownReport,FORMAT_OU,move,
                PublicCompetitiveCoverageAudit.Uncertainty.TRANSFORM_OBSERVED)
                .independentReplays()==0);
        check(stratum(ownReport,FORMAT_OU,move,
                PublicCompetitiveCoverageAudit.Uncertainty.UNKNOWN_MOVE_SOURCE)
                .independentReplays()==0);

        // Registrations cannot be changed after observing the held-out data.
        reject(()->new PublicCompetitiveRobustnessAudit.Registration(2,
                data.registration.protocolSha256(),data.registration.testSplitSha256()));
        reject(()->new PublicCompetitiveRobustnessAudit.Registration(1,"bogus",
                data.registration.testSplitSha256()));
        var changedProtocol=new PublicCompetitiveRobustnessAudit.Registration(1,
                "f".repeat(64),data.registration.testSplitSha256());
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,data.trust,T0+550,changedProtocol));
        var changedSplit=new PublicCompetitiveRobustnessAudit.Registration(1,
                data.registration.protocolSha256(),"e".repeat(64));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,data.trust,T0+550,changedSplit));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,data.trust,T0+550,null));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","unknown","unknown"),
                data.held,data.signed,data.trust,T0+550,data.registration));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                null,data.trust,T0+550,data.registration));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,null,T0+550,data.registration));
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,data.trust,T0+40,data.registration)); // Future signature.
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,data.held,
                data.signed,data.trust,T0+2001,data.registration)); // Expired approval.

        // Replacing outcomes while reusing the frozen split invalidates custody
        // and cannot masquerade as the precommitted test result.
        var altered=new ArrayList<>(inputs);
        var replay=inputs.get(1);
        altered.set(1,new PublicHeldOutEvaluation.Replay(replay.source(),
                replay.spectatorLog().replace("Shadow Claw","Swift"),
                replay.partition()));
        reject(()->PublicCompetitiveRobustnessAudit.audit(altered,PIN,data.held,
                data.signed,data.trust,T0+550,data.registration));
        var privateInputs=new ArrayList<>(inputs);
        privateInputs.set(1,new PublicHeldOutEvaluation.Replay(replay.source(),
                replay.spectatorLog()+"|request|SYNTHETIC_PRIVATE_PERSON\n",
                replay.partition()));
        reject(()->PublicCompetitiveRobustnessAudit.audit(privateInputs,PIN,data.held,
                data.signed,data.trust,T0+550,data.registration));
        var forgery=new PublicHeldOutEvaluation.Report(
                data.held.splitSha256(),data.held.sourceScope(),data.held.trainReplays(),
                data.held.heldOutReplays(),data.held.training(),
                new PublicHeldOutEvaluation.Segment(data.held.heldOut().acceptedReplays(),
                    data.held.heldOut().unsupportedTargetReplays(),
                    data.held.heldOut().unscoredPublicDecisions(),
                    PublicHeldOutEvaluation.Metric.of(90,80,1,"FIRST_OBSERVED_NATIVE_MOVE"),
                    data.held.heldOut().switches()),false,false,false,false);
        reject(()->PublicCompetitiveRobustnessAudit.audit(inputs,PIN,forgery,
                data.signed,data.trust,T0+550,data.registration));

        // Publicly constructed hypothetical metrics cannot be substituted
        // for observed pinned coverage; constants have no public setters.
        check(PublicCompetitiveRobustnessAudit.MIN_REPLAYS==3
                && PublicCompetitiveRobustnessAudit.MIN_HORIZONS==30
                && PublicCompetitiveRobustnessAudit.MIN_ADVANTAGE_BASIS_POINTS==500);
        reject(()->new PublicCompetitiveRobustnessAudit.Stratum(FORMAT_OU,move,all,
                1,1,1,true,true));
        reject(()->new PublicCompetitiveRobustnessAudit.Stratum(FORMAT_OU,move,all,
                3,30,5,true,false));
        check(report.stressScenarios().stream().allMatch(s->
                s.ouPairedHorizons()>=0 && s.ubersPairedHorizons()>=0));
        System.out.println("Phase 5.12 synthetic frozen robustness PASS: "+checks+" checks");
    }
}
