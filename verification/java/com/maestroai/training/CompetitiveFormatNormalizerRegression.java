package com.maestroai.training;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Synthetic, deterministic Phase 5.2 safety/compatibility assertions. */
final class CompetitiveFormatNormalizerRegression {
    private static int checks;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Phase 5.2 normalization contract");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_FIXTURE_SECRET")); return;
        }
        throw new AssertionError("Malformed replay evidence was accepted");
    }
    private static void immutable(Runnable action) {
        try { action.run(); }
        catch (UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError("Mutated normalized evidence");
    }
    private static final String HASH="a".repeat(64);
    private static PublicReplayCorpus.Appearance appearance(int n,String species,Set<String> moves,
                                                              String ability,String item,boolean transformed) {
        return new PublicReplayCorpus.Appearance(n,"p2a",species,moves,item,ability,
                transformed,false,true);
    }
    private static PublicReplayCorpus.Evidence fixture(String format,List<PublicReplayCorpus.Appearance> appearances) {
        return new PublicReplayCorpus.Evidence(format+"-12345678",format,HASH,12,2,appearances,
                "SHOWDOWN_PUBLIC_OBSERVATIONS;COBBLEMON_TRANSFER_UNVERIFIED");
    }
    static void run() {
        var input=fixture("gen9ou",List.of(
                appearance(1,"zoroark",Set.of("swift","nastyplot","unknownfuturemove"),
                        "illusion","leftovers",false),
                appearance(2,"zoroark",Set.of("shadowclaw"),null,null,false),
                appearance(3,"calyrexshadow",Set.of("astralbarrage"),"asone","choicescarf",false),
                appearance(4,"mewtwomegay",Set.of("psystrike"),"pressure",null,false),
                appearance(5,"zoroarkhisui",Set.of("uturn"),null,null,true)));
        var result=CompetitiveFormatNormalizer.normalize(input,
                CompetitiveFormatNormalizer.Target.pinnedSource());
        var yes=CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED;
        var unknown=CompetitiveFormatNormalizer.Status.UNKNOWN;
        check(result.appearances().size()==5);
        check(result.provenance().replayId().equals("gen9ou-12345678"));
        check(result.provenance().inputSha256().equals(HASH));
        check(result.provenance().rightsVerification().contains("NOT_INDEPENDENTLY_VERIFIED"));
        check(result.rules().generation()==9 && result.rules().gameType().equals("singles"));
        check(result.rules().showdownTier().equals("ou"));
        check(!result.rules().completeShowdownRulesKnown()
                && result.rules().cobblemonTierTransfer()==unknown);
        check(result.targetSourceScope().equals(CobblemonMechanics.SOURCE));
        check(!result.minecraftRuntimeAttested());
        var z=result.appearances().get(0);
        check(z.displayed().id().equals("zoroark") && z.displayed().sourcePresence()==yes);
        check(z.observedMoves().get("swift").sourcePresence()==yes);
        check(z.observedMoves().get("swift").conditionalMechanics()==yes);
        check(z.observedMoves().get("nastyplot").sourcePresence()==yes);
        check(z.observedMoves().get("nastyplot").conditionalMechanics()==unknown);
        check(z.observedMoves().get("unknownfuturemove").sourcePresence()==unknown);
        check(z.revealedAbility().id().equals("illusion")
                && z.revealedAbility().sourcePresence()==unknown);
        check(z.revealedItem().id().equals("leftovers")
                && z.revealedItem().conditionalMechanics()==unknown);
        check(!z.identityLinked() && z.identityUncertain());
        check(!result.appearances().get(1).identityLinked()
                && result.appearances().get(1).observedMoves().size()==1);
        var calyrex=result.appearances().get(2);
        check(calyrex.distinctForm() && calyrex.displayed().sourcePresence()==yes);
        check(calyrex.baseSpecies().id().equals("calyrex")
                && calyrex.baseSpecies().sourcePresence()==unknown);
        var mega=result.appearances().get(3);
        check(mega.distinctForm() && mega.baseSpecies().id().equals("mewtwo"));
        check(mega.baseSpecies().sourcePresence()==yes
                && mega.displayed().sourcePresence()==unknown);
        check(mega.displayed().conditionalMechanics()==unknown);
        var hisui=result.appearances().get(4);
        check(hisui.distinctForm() && hisui.displayed().sourcePresence()==unknown);
        check(hisui.transformed() && hisui.identityUncertain());
        immutable(()->result.appearances().add(z));
        immutable(()->z.observedMoves().put("secret",z.observedMoves().get("swift")));
        var other=CompetitiveFormatNormalizer.normalize(input,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","9999999","unverified"));
        check(other.appearances().get(0).displayed().sourcePresence()
                ==CompetitiveFormatNormalizer.Status.OUTSIDE_VERIFIED_SCOPE);
        check(other.appearances().get(0).observedMoves().get("swift").sourcePresence()
                ==CompetitiveFormatNormalizer.Status.OUTSIDE_VERIFIED_SCOPE);
        check(other.appearances().get(0).observedMoves().get("swift").conditionalMechanics()==unknown);
        check(!other.minecraftRuntimeAttested()
                && other.targetSourceScope().startsWith("OUTSIDE_PINNED"));
        check(CompetitiveFormatNormalizer.normalize(fixture("gen9ubers",List.of()),
                CompetitiveFormatNormalizer.Target.pinnedSource()).rules().showdownTier().equals("ubers"));
        var poisoned=fixture("gen9ou",List.of(
                appearance(1,"PRIVATE_FIXTURE_SECRET",Set.of("PRIVATE_FIXTURE_SECRET"),
                        "PRIVATE_FIXTURE_SECRET","PRIVATE_FIXTURE_SECRET",false)));
        var sanitized=CompetitiveFormatNormalizer.normalize(poisoned,
                CompetitiveFormatNormalizer.Target.pinnedSource());
        check(!sanitized.toString().contains("PRIVATE_FIXTURE_SECRET"));
        check(sanitized.appearances().get(0).displayed().id().equals("unknown")
                && sanitized.appearances().get(0).observedMoves().containsKey("unknown"));
        reject(()->CompetitiveFormatNormalizer.normalize(null,
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(input,null));
        reject(()->CompetitiveFormatNormalizer.normalize(
                fixture("gen8ou",List.of()),CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(
                new PublicReplayCorpus.Evidence("gen9ou-12345678","gen9ou","NOT_A_HASH",1,1,List.of(),
                        "SHOWDOWN_PUBLIC_OBSERVATIONS;COBBLEMON_TRANSFER_UNVERIFIED"),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(
                new PublicReplayCorpus.Evidence("gen9ou-12345678","gen9ou",HASH,1,1,List.of(),
                        "PRIVATE_FIXTURE_SECRET"),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(
                fixture("gen9ou",List.of(new PublicReplayCorpus.Appearance(1,"p2a","zoroark",
                        Set.of(),null,null,false,true,false))),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(
                fixture("gen9ou",List.of(appearance(2,"zoroark",Set.of(),null,null,false))),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        reject(()->CompetitiveFormatNormalizer.normalize(
                fixture("gen9ou",List.of(new PublicReplayCorpus.Appearance(1,"p3a","zoroark",
                        Set.of(),null,null,false,false,true))),
                CompetitiveFormatNormalizer.Target.pinnedSource()));
        check(result.equals(CompetitiveFormatNormalizer.normalize(input,
                CompetitiveFormatNormalizer.Target.pinnedSource())));
        System.out.println("Phase 5.2 synthetic normalization PASS: "+checks+" checks");
    }
}
