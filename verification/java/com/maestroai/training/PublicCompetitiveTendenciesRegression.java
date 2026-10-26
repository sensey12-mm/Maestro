package com.maestroai.training;

import java.util.*;

/** Synthetic only. Neither replay provenance nor empirical competitive claims are asserted. */
final class PublicCompetitiveTendenciesRegression {
    private PublicCompetitiveTendenciesRegression() {}
    private static int checks;
    private static void check(boolean pass) {
        if (!pass) throw new AssertionError("Phase 5.3 synthetic tendency regression");
        checks++;
    }
    private static void reject(Runnable runnable) {
        try { runnable.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_TEST_PERSON")); return;
        }
        throw new AssertionError("Unsafe public replay accepted");
    }
    private static void immutable(Runnable runnable) {
        try { runnable.run(); }
        catch (UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError("Mutable public tendency evidence");
    }
    private static final PublicReplayCorpus.Source SOURCE =
            new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    private static final String LOG =
            "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_TEST_PERSON_PLAYER\n|start\n"
            +"|switch|p1a: PRIVATE_TEST_PERSON_NICK|Pikachu, L100|100/100\n"
            +"|switch|p2a: Foename|Zoroark, L100|100/100\n|turn|1\n"
            +"|move|p2a: Foename|Swift|p1a: PRIVATE_TEST_PERSON_NICK\n"
            +"|move|p2a: Foename|Nasty Plot|p1a: PRIVATE_TEST_PERSON_NICK\n"
            +"|move|p2a: Foename|Swift|p1a: PRIVATE_TEST_PERSON_NICK\n"
            +"|move|p2a: Foename|Shadow Claw|p1a: PRIVATE_TEST_PERSON_NICK|[from] ability: Fake\n"
            +"|chat|p1|PRIVATE_TEST_PERSON_CHAT\n"
            +"|switch|p2a: Foename|Zoroark, L100|100/100\n|turn|2\n"
            +"|move|p2a: Foename|Shadow Claw|p1a: PRIVATE_TEST_PERSON_NICK\n"
            +"|drag|p1a: PRIVATE_TEST_PERSON_NICK|Pikachu, L100|100/100\n"
            +"|turn|3\n|move|p2a: Foename|Swift|p1a: PRIVATE_TEST_PERSON_NICK\n"
            +"|win|PRIVATE_TEST_PERSON_PLAYER\n";
    private static PublicCompetitiveTendencies.Report parse(String log) {
        return PublicCompetitiveTendencies.analyze(SOURCE,log,
                CompetitiveFormatNormalizer.Target.pinnedSource());
    }

    static void run() {
        var result=parse(LOG);
        check(result.appearances().size()==4);
        check(result.provenance().replayId().equals("gen9ou-12345678"));
        check(result.provenance().inputSha256().matches("[a-f0-9]{64}"));
        check(result.rules().showdownTier().equals("ou"));
        check(result.targetSourceScope().equals(CobblemonMechanics.SOURCE));
        check(result.interpretation()==PublicCompetitiveTendencies.Interpretation.OBSERVATION_ONLY_NOT_FORECAST);
        check(!result.forecastProduced());
        check(!result.toString().contains("PRIVATE_TEST_PERSON")
                && !result.toString().contains("Foename"));
        var first=result.appearances().get(1);
        check(first.slot().equals("p2a") && first.index()==2);
        check(first.nativeUses().size()==2);
        check(first.nativeUses().get(0).token().id().equals("swift")
                && first.nativeUses().get(0).publicUses()==2);
        check(first.nativeUses().get(1).token().id().equals("nastyplot")
                && first.nativeUses().get(1).publicUses()==1);
        check(first.nativeUses().stream().noneMatch(x->x.token().id().equals("shadowclaw")));
        check(first.nativeUses().get(0).token().sourcePresence()
                ==CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED);
        var second=result.appearances().get(2);
        check(second.nativeUses().size()==2);
        check(second.nativeUses().get(0).token().id().equals("shadowclaw")
                && second.nativeUses().get(0).publicUses()==1);
        check(second.nativeUses().get(1).token().id().equals("swift")
                && second.nativeUses().get(1).publicUses()==1);
        check(first.identityUncertain() && second.identityUncertain());
        check(!first.identityLinked() && !second.identityLinked());
        check(result.appearances().get(3).entry()
                ==PublicCompetitiveTendencies.EntryKind.DRAG_PUBLICLY_FORCED);
        check(result.sides().get("p1a").forcedDragAnnouncements()==1);
        check(result.sides().get("p1a").switchAnnouncements()==1);
        check(result.sides().get("p2a").switchAnnouncements()==2);
        check(result.sides().get("p2a").repeatedDisplayCandidates()==1);
        check(!result.sides().get("p2a").voluntarySwitchesKnown());
        immutable(()->result.appearances().add(first));
        immutable(()->result.sides().put("p1a",result.sides().get("p2a")));
        immutable(()->first.nativeUses().add(first.nativeUses().get(0)));
        check(result.equals(parse(LOG)));

        // Transform copies moves; copied Shadow Claw must never count as native.
        var copied=parse(LOG.replace("|move|p2a: Foename|Shadow Claw|p1a: PRIVATE_TEST_PERSON_NICK\n",
                "|-transform|p2a: Foename|p1a: PRIVATE_TEST_PERSON_NICK\n"
                +"|move|p2a: Foename|Shadow Claw|p1a: PRIVATE_TEST_PERSON_NICK\n"));
        check(copied.appearances().get(2).transformed());
        check(copied.appearances().get(2).nativeUses().size()==0);
        check(!copied.forecastProduced());

        // An Illusion reveal does not glue two appearances into one identity.
        var revealed=parse(LOG.replace(
                "|turn|2\n|move|p2a: Foename|Shadow Claw",
                "|turn|2\n|replace|p2a: Foename|Zoroark, L100\n"
                +"|move|p2a: Foename|Shadow Claw"));
        check(revealed.appearances().size()==4);
        check(revealed.appearances().get(1).identityUncertain());
        check(!revealed.appearances().get(2).identityLinked());
        check(revealed.appearances().get(1).nativeUses().get(0).publicUses()==2);

        check(!parse(LOG.replace("PRIVATE_TEST_PERSON_PLAYER","OTHER_USER"))
                .provenance().inputSha256().equals(result.provenance().inputSha256()));
        reject(()->parse(LOG+"|request|PRIVATE_TEST_PERSON\n"));
        reject(()->parse(LOG+"|split|p1\n|private|PRIVATE_TEST_PERSON\n|public|x\n"));
        reject(()->parse(LOG+"sideupdate\np2\n"));
        reject(()->parse(LOG.replace("|gen|9","|gen|8")));
        reject(()->PublicCompetitiveTendencies.analyze(SOURCE,LOG,null));
        reject(()->PublicCompetitiveTendencies.analyze(
                new PublicReplayCorpus.Source("gen9ou-12345678","gen9ou",false,
                        PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION),
                LOG,CompetitiveFormatNormalizer.Target.pinnedSource()));
        var other=PublicCompetitiveTendencies.analyze(SOURCE,LOG,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","new","unknown"));
        check(other.targetSourceScope().startsWith("OUTSIDE_PINNED"));
        check(other.appearances().get(1).nativeUses().get(0).token().sourcePresence()
                ==CompetitiveFormatNormalizer.Status.OUTSIDE_VERIFIED_SCOPE);
        check(!other.forecastProduced());
        System.out.println("Phase 5.3 synthetic public tendencies PASS: "+checks+" checks");
    }
}
