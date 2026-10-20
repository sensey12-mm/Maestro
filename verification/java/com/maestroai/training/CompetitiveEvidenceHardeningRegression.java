package com.maestroai.training;

import java.util.*;

/** Full 5.1–5.4 offline synthetic boundary test; no real users or Minecraft. */
final class CompetitiveEvidenceHardeningRegression {
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("Phase 5.5 synthetic pipeline");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SECRET_TEST_PERSON"));
            return;
        }
        throw new AssertionError("Unsafe public competitive evidence accepted");
    }
    private static PublicReplayCorpus.Source approved(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static final String HEADER="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SECRET_TEST_PERSON\n|start\n";
    private static final String FIRST=HEADER
            +"|switch|p1a: SECRET_TEST_PERSON_NICK|Pikachu, L100|100/100\n"
            +"|switch|p2a: Disguise|Pikachu, L100|100/100\n"
            +"|-item|p2a: Disguise|Leftovers\n"
            +"|-ability|p2a: Disguise|Static\n"
            +"|turn|1\n"
            +"|move|p2a: Disguise|Swift|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|replace|p2a: Disguise|Zoroark, L100\n"
            +"|move|p2a: Disguise|Nasty Plot|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|switch|p2a: Disguise|Zoroark, L100|100/100\n"
            +"|turn|2\n|-transform|p2a: Disguise|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|move|p2a: Disguise|Shadow Claw|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|win|SECRET_TEST_PERSON\n";
    private static final String SECOND=HEADER
            +"|switch|p1a: SECRET_TEST_PERSON_NICK|Pikachu, L100|100/100\n"
            +"|switch|p2a: FoeTwo|Zoroark, L100|100/100\n"
            +"|turn|1\n"
            +"|move|p2a: FoeTwo|Imaginary Blade|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|move|p2a: FoeTwo|Swift|p1a: SECRET_TEST_PERSON_NICK\n"
            +"|win|SECRET_TEST_PERSON\n";
    private static PublicReplayCorpus.Evidence admit(String log) {
        return PublicReplayCorpus.ingest(approved("12345678"),log);
    }
    private static PublicHeldOutEvaluation.Replay training() {
        return new PublicHeldOutEvaluation.Replay(approved("12345678"),FIRST,
                PublicHeldOutEvaluation.Partition.TRAIN);
    }
    private static PublicHeldOutEvaluation.Replay heldOut(String id, String log) {
        return new PublicHeldOutEvaluation.Replay(approved(id),log,
                PublicHeldOutEvaluation.Partition.HELD_OUT);
    }
    static void run() {
        // Fully admitted, de-identified chain with hidden-form uncertainty.
        var admitted=admit(FIRST);
        check(admitted.appearances().size()==3);
        check(admitted.compatibility().contains("COBBLEMON_TRANSFER_UNVERIFIED"));
        check(!admitted.toString().contains("SECRET_TEST_PERSON")
                && !admitted.toString().contains("Disguise"));
        var first=admitted.appearances().get(1);
        check(first.displayedSpecies().equals("zoroark"));
        check(first.revealedItem()==null && first.revealedAbility()==null);
        check(first.identityUncertain() && !first.identityLinked());
        check(first.nativeMoves().containsAll(Set.of("swift","nastyplot")));
        check(admitted.appearances().get(2).transformed()
                && admitted.appearances().get(2).nativeMoves().isEmpty());

        var normalized=CompetitiveFormatNormalizer.normalize(admitted,
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(normalized.appearances().size()==3);
        check(normalized.appearances().get(1).displayed().sourcePresence()
                ==CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED);
        check(normalized.appearances().get(1).revealedAbility().sourcePresence()
                ==CompetitiveFormatNormalizer.Status.UNKNOWN);
        check(!normalized.minecraftRuntimeAttested());
        check(normalized.provenance().inputSha256().equals(admitted.inputSha256()));
        check(normalized.appearances().get(2).observedMoves().isEmpty());

        var tendencies=PublicCompetitiveTendencies.analyze(approved("12345678"),FIRST,
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(tendencies.appearances().size()==3);
        check(tendencies.appearances().get(1).nativeUses().size()==2);
        check(tendencies.appearances().get(2).nativeUses().isEmpty());
        check(tendencies.sides().get("p2a").repeatedDisplayCandidates()==1);
        check(!tendencies.sides().get("p2a").voluntarySwitchesKnown()
                && !tendencies.forecastProduced());
        check(!tendencies.toString().contains("SECRET_TEST_PERSON"));

        var report=PublicHeldOutEvaluation.evaluate(
                List.of(training(),heldOut("23456789",SECOND)),
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(report.trainReplays()==1 && report.heldOutReplays()==1);
        check(!report.forecastProbabilityClaimed()
                && !report.crossReplayIdentityInferred()
                && !report.trainedParametersShared());
        check(report.heldOut().moves().observedCandidateRate()==null);
        check(report.heldOut().moves().interpretation().equals("INSUFFICIENT_HORIZONS"));
        check(!report.toString().contains("SECRET_TEST_PERSON")
                && !report.toString().contains("FoeTwo"));
        check(!report.toString().contains("gen9ou-23456789"));

        var unknown=CompetitiveFormatNormalizer.normalize(
                PublicReplayCorpus.ingest(approved("23456789"),SECOND),
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(unknown.appearances().get(1).observedMoves().get("imaginaryblade")
                .sourcePresence()==CompetitiveFormatNormalizer.Status.UNKNOWN);
        var wrongVersion=PublicHeldOutEvaluation.evaluate(
                List.of(training(),heldOut("23456789",SECOND)),
                new CompetitiveFormatNormalizer.Target("1.21.1","1.8","unknown","unknown"));
        check(wrongVersion.heldOut().unsupportedTargetReplays()==1);
        check(wrongVersion.heldOut().moves().evaluated()==0
                && wrongVersion.heldOut().moves().observedCandidateRate()==null);

        // Rename all player/nickname fields: raw bytes differ, but public
        // chronology is identical. Held-out replay must still be rejected.
        String clone=FIRST.replace("SECRET_TEST_PERSON","NEW_PLAYER")
                .replace("Disguise","DifferentNick");
        check(!PublicReplayCorpus.ingest(approved("34567890"),clone).inputSha256()
                .equals(admitted.inputSha256()));
        reject(()->PublicHeldOutEvaluation.evaluate(
                List.of(training(),heldOut("34567890",clone)),
                CompetitiveFormatNormalizer.Target.pinnedSource()));

        // Unexpected event kinds and private transport fail closed.
        reject(()->admit(FIRST.replace("|win|SECRET_TEST_PERSON",
                "|-weather|RainDance\n|win|SECRET_TEST_PERSON")));
        reject(()->admit(FIRST.replace("|win|SECRET_TEST_PERSON",
                "|poke|p2|SECRET_TEST_PERSON_HIDDEN_TEAM\n|win|SECRET_TEST_PERSON")));
        reject(()->admit(FIRST.replace("|win|SECRET_TEST_PERSON",
                "|request2|SECRET_TEST_PERSON_PRIVATE\n|win|SECRET_TEST_PERSON")));
        reject(()->admit(FIRST+"|request|SECRET_TEST_PERSON_PRIVATE\n"));
        reject(()->admit(FIRST+"sideupdate\np2\n"));
        reject(()->admit(FIRST+"|split|p2\n|private|SECRET_TEST_PERSON\n|public|x\n"));
        reject(()->admit(FIRST.replace("|turn|2","|turn|3")));
        reject(()->admit(FIRST.replace("|turn|2","|turn|1")));
        reject(()->admit(FIRST.replace("|start\n","|start\n|start\n")));
        reject(()->admit(FIRST.replace("|win|SECRET_TEST_PERSON",
                "|win|SECRET_TEST_PERSON\n|move|p2a: Disguise|Swift|p1a: Own")));
        reject(()->admit(FIRST.replace("|start\n","|start\n|gen|9\n")));
        reject(()->admit(FIRST.replace("|start\n","|start\n|tier|[Gen 9] OU\n")));
        reject(()->admit(FIRST.replace("|start\n",
                "|move|p2a: Disguise|Swift|p1a: Own\n|start\n")));
        reject(()->admit(FIRST.replace("|turn|1\n","|turn|2\n")));
        reject(()->new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",false,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION));

        // Accuracy data cannot be manufactured through public record constructors.
        reject(()->new PublicHeldOutEvaluation.Metric(20,10,12,Double.NaN,.6,
                "FIRST_OBSERVED_NATIVE_MOVE","EMPIRICAL_ONLY_NOT_CALIBRATED"));
        reject(()->new PublicHeldOutEvaluation.Metric(20,10,12,.9,.6,
                "FIRST_OBSERVED_NATIVE_MOVE","EMPIRICAL_ONLY_NOT_CALIBRATED"));
        reject(()->new PublicHeldOutEvaluation.Metric(2,1,1,.5,.5,
                "FIRST_OBSERVED_NATIVE_MOVE","INSUFFICIENT_HORIZONS"));
        reject(()->new PublicHeldOutEvaluation.Metric(20,10,12,.5,.6,
                "FIRST_OBSERVED_NATIVE_MOVE","CALIBRATED_CONFIDENCE"));
        reject(()->new PublicHeldOutEvaluation.Report("a".repeat(64),
                "UNVERIFIED_COBBLEMON_SUPERPOWER",1,1,report.training(),report.heldOut(),
                false,false,false,false));
        check(report.equals(PublicHeldOutEvaluation.evaluate(
                List.of(heldOut("23456789",SECOND),training()),
                CompetitiveFormatNormalizer.Target.pinnedSource())));

        System.out.println("Phase 5.5 synthetic end-to-end hardening PASS: "+checks+" checks");
    }
}
