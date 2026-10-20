package com.maestroai.training;

import java.util.*;

/** Offline synthetic replay fixtures; no actual users, held-out skill or Minecraft claim. */
final class PublicHeldOutEvaluationRegression {
    private PublicHeldOutEvaluationRegression() {}
    private static int checks;
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Phase 5.4 held-out leakage/benchmark regression");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_FIXTURE_PERSON")); return;
        }
        throw new AssertionError("Untrusted held-out input accepted");
    }
    private static PublicReplayCorpus.Source source(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static String header() {
        return "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
                +"|player|p1|PRIVATE_FIXTURE_PERSON\n|start\n"
                +"|switch|p1a: PRIVATE_FIXTURE_PERSON_NICK|Pikachu, L100|100/100\n"
                +"|switch|p2a: UnknownEnemy|Zoroark, L100|100/100\n|turn|1\n";
    }
    private static String moves(String first, String later, int count) {
        StringBuilder raw=new StringBuilder(header());
        raw.append("|move|p2a: UnknownEnemy|").append(first)
                .append("|p1a: PRIVATE_FIXTURE_PERSON_NICK\n");
        for (int i=0;i<count;i++)
            raw.append("|move|p2a: UnknownEnemy|").append(later)
                    .append("|p1a: PRIVATE_FIXTURE_PERSON_NICK\n");
        raw.append("|win|PRIVATE_FIXTURE_PERSON\n");
        return raw.toString();
    }
    private static String switchPattern() {
        return header()
                +"|switch|p2a: Alternate|Pikachu, L100|100/100\n"
                +"|switch|p2a: UnknownEnemy|Zoroark, L100|100/100\n"
                +"|move|p2a: UnknownEnemy|Swift|p1a: PRIVATE_FIXTURE_PERSON_NICK\n"
                +"|move|p2a: UnknownEnemy|Swift|p1a: PRIVATE_FIXTURE_PERSON_NICK\n"
                +"|switch|p2a: Alternate|Pikachu, L100|100/100\n"
                +"|win|PRIVATE_FIXTURE_PERSON\n";
    }
    private static PublicHeldOutEvaluation.Replay replay(String id, String raw,
                                                         PublicHeldOutEvaluation.Partition part) {
        return new PublicHeldOutEvaluation.Replay(source(id),raw,part);
    }
    private static PublicHeldOutEvaluation.Report evaluate(
            PublicHeldOutEvaluation.Replay a, PublicHeldOutEvaluation.Replay b) {
        return PublicHeldOutEvaluation.evaluate(List.of(a,b),
                CompetitiveFormatNormalizer.Target.pinnedSource());
    }
    static void run() {
        var train=replay("12345678",moves("Swift","Swift",24),
                PublicHeldOutEvaluation.Partition.TRAIN);
        var test=replay("23456789",moves("Swift","Shadow Claw",28),
                PublicHeldOutEvaluation.Partition.HELD_OUT);
        var report=evaluate(train,test);
        check(report.trainReplays()==1 && report.heldOutReplays()==1);
        check(report.splitSha256().matches("[0-9a-f]{64}"));
        check(report.sourceScope().equals(CobblemonMechanics.SOURCE));
        check(!report.crossReplayIdentityInferred() && !report.trainedParametersShared());
        check(!report.replayOverlapDetected() && !report.forecastProbabilityClaimed());
        check(report.training().moves().evaluated()==24);
        check(report.heldOut().moves().evaluated()==28);
        check(report.heldOut().moves().candidateHits()
                > report.heldOut().moves().baselineHits());
        check(report.heldOut().moves().observedCandidateRate()!=null);
        check(report.heldOut().moves().observedBaselineRate()!=null);
        check(report.heldOut().moves().interpretation().equals("EMPIRICAL_ONLY_NOT_CALIBRATED"));
        check(report.heldOut().moves().baseline().equals("FIRST_OBSERVED_NATIVE_MOVE"));
        check(report.heldOut().switches().baseline().equals("ALWAYS_MOVE"));
        check(report.heldOut().switches().observedCandidateRate()==null);
        check(report.heldOut().unscoredPublicDecisions()>0);
        check(!report.toString().contains("PRIVATE_FIXTURE_PERSON"));
        check(!report.toString().contains("UnknownEnemy"));
        check(!report.toString().contains("gen9ou-23456789")); // no per-replay IDs

        // Ordering does not affect split attribution, digest or results.
        var reverse=PublicHeldOutEvaluation.evaluate(List.of(test,train),
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(report.equals(reverse));
        var replacementTrain=replay("34567890",moves("Swift","Nasty Plot",24),
                PublicHeldOutEvaluation.Partition.TRAIN);
        var changedTrain=evaluate(replacementTrain,test);
        // A model cannot learn from train replays: test results MUST stay identical.
        check(changedTrain.heldOut().equals(report.heldOut()));
        check(!changedTrain.splitSha256().equals(report.splitSha256()));
        var changedTest=evaluate(train,replay("23456789",moves("Swift","Swift",28),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        check(!changedTest.heldOut().moves().equals(report.heldOut().moves()));
        check(changedTest.training().equals(report.training()));

        // Move rates <20 are withheld, NOT interpreted as calibrated confidence.
        var sparse=evaluate(train,replay("45678901",moves("Swift","Shadow Claw",2),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        check(sparse.heldOut().moves().evaluated()==2);
        check(sparse.heldOut().moves().observedCandidateRate()==null);
        check(sparse.heldOut().moves().observedBaselineRate()==null);
        check(sparse.heldOut().moves().interpretation().equals("INSUFFICIENT_HORIZONS"));

        // Same display returns as a *candidate*, not a proven unique party member.
        var switchTest=evaluate(train,replay("56789012",switchPattern(),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        check(switchTest.heldOut().switches().evaluated()>=2);
        check(switchTest.heldOut().switches().candidateHits()>=0);
        check(switchTest.heldOut().switches().baselineHits()>=0);
        check(switchTest.heldOut().switches().observedCandidateRate()==null);

        String transformed=header()
                +"|move|p2a: UnknownEnemy|Swift|p1a: PRIVATE_FIXTURE_PERSON_NICK\n"
                +"|-transform|p2a: UnknownEnemy|p1a: PRIVATE_FIXTURE_PERSON_NICK\n"
                +"|move|p2a: UnknownEnemy|Shadow Claw|p1a: PRIVATE_FIXTURE_PERSON_NICK\n"
                +"|win|PRIVATE_FIXTURE_PERSON\n";
        var transformReport=evaluate(train,replay("67890123",transformed,
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        check(transformReport.heldOut().moves().evaluated()==0);
        check(transformReport.heldOut().unscoredPublicDecisions()>0);

        // Opponent-only evaluation: unrelated p1 native moves never score.
        String ownMove="|move|p1a: PRIVATE_FIXTURE_PERSON_NICK|Nasty Plot|p2a: UnknownEnemy\n";
        var withP1=evaluate(train,replay("23456789",
                moves("Swift","Shadow Claw",28).replace("|win|",ownMove+"|win|"),
                PublicHeldOutEvaluation.Partition.HELD_OUT));
        check(withP1.heldOut().equals(report.heldOut()));

        // Unknown Cobblemon target cannot inherit the pinned source's evaluation.
        var unsupported=PublicHeldOutEvaluation.evaluate(List.of(train,test),
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","unverified","unverified"));
        check(unsupported.heldOut().unsupportedTargetReplays()==1);
        check(unsupported.training().unsupportedTargetReplays()==1);
        check(unsupported.heldOut().moves().evaluated()==0
                && unsupported.heldOut().switches().evaluated()==0);
        check(unsupported.heldOut().moves().observedCandidateRate()==null);
        check(unsupported.sourceScope().equals("OUTSIDE_PINNED_SOURCE_SCOPE"));

        // No train/test overlap by replay ID OR identical input digest.
        reject(()->evaluate(train,replay("12345678",moves("Swift","Shadow Claw",4),
                PublicHeldOutEvaluation.Partition.HELD_OUT)));
        reject(()->evaluate(train,replay("23456789",train.spectatorLog(),
                PublicHeldOutEvaluation.Partition.HELD_OUT)));
        reject(()->PublicHeldOutEvaluation.evaluate(List.of(train,train),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->PublicHeldOutEvaluation.evaluate(List.of(train),CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->PublicHeldOutEvaluation.evaluate(List.of(train,
                replay("23456789",moves("Swift","Swift",20),PublicHeldOutEvaluation.Partition.TRAIN)),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->PublicHeldOutEvaluation.evaluate(List.of(test,
                replay("34567890",moves("Swift","Swift",20),PublicHeldOutEvaluation.Partition.HELD_OUT)),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->PublicHeldOutEvaluation.evaluate(List.of(train,test),null));
        reject(()->evaluate(train,replay("23456789",test.spectatorLog()
                +"|request|PRIVATE_FIXTURE_PERSON_PRIVATE\n",PublicHeldOutEvaluation.Partition.HELD_OUT)));
        reject(()->evaluate(train,replay("23456789",test.spectatorLog()
                +"|split|p2\n|private|PRIVATE_FIXTURE_PERSON_PRIVATE\n|public|x\n",
                PublicHeldOutEvaluation.Partition.HELD_OUT)));
        reject(()->evaluate(train,replay("23456789",test.spectatorLog()
                .replace("|gen|9","|gen|8"),PublicHeldOutEvaluation.Partition.HELD_OUT)));
        reject(()->PublicHeldOutEvaluation.evaluate(
                List.of(train,new PublicHeldOutEvaluation.Replay(
                        new PublicReplayCorpus.Source("gen9ou-23456789","gen9ou",false,
                                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION),
                        test.spectatorLog(),PublicHeldOutEvaluation.Partition.HELD_OUT)),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        System.out.println("Phase 5.4 synthetic held-out evaluation PASS: "+checks+" checks");
    }
}
