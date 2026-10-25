package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.13 — synthetic, explicitly signed and appearance-local evidence. */
final class PublicOpponentBuildHypothesesRegression {
    private PublicOpponentBuildHypothesesRegression() {}
    private static int checks;
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Phase 5.13 public build boundary");
        checks++;
    }
    private static void reject(Runnable run) {
        try { run.run(); }
        catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SECRET_TEST_PERSON")
                    && !expected.getMessage().contains("EnemyNickname"));
            return;
        }
        throw new AssertionError("Forged, private, or cross-appearance build evidence accepted");
    }
    private static final long T0=1_700_000_000L,T1=T0+5,T2=T0+20;
    private static final String REVIEW="c".repeat(64);
    private static final CompetitiveFormatNormalizer.Target PIN =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final String REPLAY="gen9ou-12345678";
    private static final String LOG="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SECRET_TEST_PERSON\n|start\n"
            +"|switch|p1a: MyPikachu|Pikachu, L100|100/100\n"
            +"|switch|p2a: EnemyNickname|Zoroark, L100|100/100\n"
            +"|-ability|p2a: EnemyNickname|Illusion\n"
            +"|-item|p2a: EnemyNickname|Leftovers\n"
            +"|turn|1\n"
            +"|move|p2a: EnemyNickname|Nasty Plot|p1a: MyPikachu\n"
            +"|move|p2a: EnemyNickname|Shadow Sneak|p1a: MyPikachu\n"
            +"|switch|p2a: EnemyNickname|Zoroark, L100|100/100\n"
            +"|turn|2\n"
            +"|move|p2a: EnemyNickname|Swift|p1a: MyPikachu\n"
            +"|switch|p2a: Clone|Ditto, L100|100/100\n"
            +"|-transform|p2a: Clone|p1a: MyPikachu\n"
            +"|move|p2a: Clone|Tackle|p1a: MyPikachu\n"
            +"|win|SECRET_TEST_PERSON\n";
    private static PublicReplayCorpus.Source src() {
        return new PublicReplayCorpus.Source(REPLAY,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static KeyPair keys() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch (GeneralSecurityException e) { throw new AssertionError(e); }
    }
    private record Fixture(PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy policy) {}
    private static Fixture fixture(String raw) {
        var s=src();
        var evidence=PublicReplayCorpus.ingest(s,raw);
        var decision=new PublicReplayCustody.Decision(s.replayId(),s.format(),
                evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                T0,T0+1000,REVIEW);
        var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
        var k=keys();
        try {
            byte[] body=PublicSignedCustody.canonicalBytes(1,"synthetic",T1,
                    "0".repeat(64),ledger);
            Signature ed=Signature.getInstance("Ed25519");
            ed.initSign(k.getPrivate());ed.update(body);
            var signed=new PublicSignedCustody.SignedManifest(1,"synthetic",T1,
                    "0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(ed.sign()));
            var key=new PublicSignedCustody.TrustedKey("synthetic",
                    Base64.getEncoder().encodeToString(k.getPublic().getEncoded()),
                    T0-50,0);
            return new Fixture(signed,new PublicSignedCustody.TrustedPolicy(
                    1,1,1,T0-50,"",List.of(key)));
        } catch (GeneralSecurityException e) { throw new AssertionError(e); }
    }
    private static PublicOpponentBuildHypotheses.Report analyze(String raw,
            List<PublicOpponentBuildHypotheses.CandidateQuery> suggestions,Fixture f) {
        return PublicOpponentBuildHypotheses.analyze(src(),raw,PIN,suggestions,
                f.signed(),f.policy(),T2);
    }
    private static PublicOpponentBuildHypotheses.CandidateQuery guess(int index,
            Set<String> moves,Set<String> abilities,Set<String> items) {
        return new PublicOpponentBuildHypotheses.CandidateQuery(index,moves,abilities,items);
    }
    private static PublicOpponentBuildHypotheses.Hypothesis find(
            PublicOpponentBuildHypotheses.Appearance a,
            PublicOpponentBuildHypotheses.Kind kind,String id) {
        return a.hypotheses().stream()
                .filter(h->h.kind()==kind && h.id().equals(id))
                .findFirst().orElseThrow();
    }

    static void run() {
        Fixture f=fixture(LOG);
        var guessFirst=guess(2,Set.of("swift","nastyplot","swordsdance","notamove"),
                Set.of("illusion","pressure"),Set.of("leftovers","choicescarf"));
        var guessReturn=guess(3,Set.of("shadowclaw","swift"),
                Set.of("illusion"),Set.of("leftovers"));
        var guessTransformed=guess(4,Set.of("shadowsneak","notamove"),
                Set.of("imposter"),Set.of("choiceband"));
        var report=analyze(LOG,List.of(guessFirst,guessReturn,guessTransformed),f);
        check(report.appearances().size()==3);
        check(report.format().equals("gen9ou"));
        check(report.sourceScope().equals(CobblemonMechanics.SOURCE));
        check(!report.independentRightsVerified() && !report.allSpeciesLearnsetsVerified());
        check(!report.currentOpponentIdentityProven());
        check(!report.calibratedProbabilitiesProduced() && !report.shadowTacticalReady()
                && !report.livePromotionAuthorized());
        check(!report.toString().contains("SECRET_TEST_PERSON")
                && !report.toString().contains("EnemyNickname")
                && !report.toString().contains(REPLAY));
        check(Arrays.stream(PublicOpponentBuildHypotheses.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        try { report.appearances().clear(); throw new AssertionError("mutable"); }
        catch (UnsupportedOperationException expected) { checks++; }

        var first=report.appearances().get(0);
        var second=report.appearances().get(1);
        var transformed=report.appearances().get(2);
        check(first.index()==2 && second.index()==3 && transformed.index()==4);
        check(first.displayedSpecies().equals("zoroark")
                && first.sourceSpeciesPresent());
        check(!first.learnsetVerified() && first.identityUncertain()
                && !first.identityLinked());
        check(!second.learnsetVerified() && second.identityUncertain());
        check(first.hypotheses().stream().allMatch(h->!h.currentBuildConfirmed()
                && !h.speciesLegalityVerified()));
        check(find(first,PublicOpponentBuildHypotheses.Kind.MOVE,"nastyplot")
                .strength()==PublicOpponentBuildHypotheses.Strength.PUBLICLY_OBSERVED_EVENT);
        check(find(first,PublicOpponentBuildHypotheses.Kind.MOVE,"shadowsneak")
                .publicEvent());
        check(find(first,PublicOpponentBuildHypotheses.Kind.ITEM,"leftovers")
                .strength()==PublicOpponentBuildHypotheses.Strength.PUBLICLY_OBSERVED_EVENT);
        check(find(first,PublicOpponentBuildHypotheses.Kind.ABILITY,"illusion")
                .strength()==PublicOpponentBuildHypotheses.Strength.PUBLICLY_OBSERVED_EVENT);
        check(!find(first,PublicOpponentBuildHypotheses.Kind.ITEM,"leftovers")
                .currentBuildConfirmed());
        check(!find(first,PublicOpponentBuildHypotheses.Kind.ABILITY,"illusion")
                .sourceEffectSupported());
        check(find(first,PublicOpponentBuildHypotheses.Kind.MOVE,"swift")
                .strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN);
        check(find(first,PublicOpponentBuildHypotheses.Kind.MOVE,"swordsdance")
                .strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN);
        check(find(first,PublicOpponentBuildHypotheses.Kind.MOVE,"notamove")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(find(first,PublicOpponentBuildHypotheses.Kind.ABILITY,"pressure")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(find(first,PublicOpponentBuildHypotheses.Kind.ITEM,"choicescarf")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(find(first,PublicOpponentBuildHypotheses.Kind.ROLE,"special-attack-boost-signal")
                .strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL);
        check(find(first,PublicOpponentBuildHypotheses.Kind.ROLE,"priority-move-signal")
                .strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL);
        check(first.hypotheses().stream().noneMatch(h->h.kind()==PublicOpponentBuildHypotheses.Kind.ROLE
                && h.id().contains("sweeper")));
        check(!first.fieldsWithNoObservation().contains(PublicOpponentBuildHypotheses.Kind.ITEM));
        check(!first.fieldsWithNoObservation().contains(PublicOpponentBuildHypotheses.Kind.ABILITY));

        // Same displayed species/nickname is NOT the same opponent party member.
        check(find(second,PublicOpponentBuildHypotheses.Kind.MOVE,"swift").publicEvent());
        check(!find(second,PublicOpponentBuildHypotheses.Kind.MOVE,"shadowclaw").publicEvent());
        check(second.hypotheses().stream().noneMatch(h->h.publicEvent()
                && h.id().equals("nastyplot")));
        check(find(second,PublicOpponentBuildHypotheses.Kind.ABILITY,"illusion")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(find(second,PublicOpponentBuildHypotheses.Kind.ITEM,"leftovers")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(second.fieldsWithNoObservation().contains(PublicOpponentBuildHypotheses.Kind.ABILITY)
                && second.fieldsWithNoObservation().contains(PublicOpponentBuildHypotheses.Kind.ITEM));
        check(transformed.transformed() && transformed.identityUncertain());
        check(find(transformed,PublicOpponentBuildHypotheses.Kind.MOVE,"shadowsneak")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(find(transformed,PublicOpponentBuildHypotheses.Kind.ABILITY,"imposter")
                .strength()==PublicOpponentBuildHypotheses.Strength.UNKNOWN_UNVERIFIED);
        check(transformed.hypotheses().stream().noneMatch(h->
                h.strength()==PublicOpponentBuildHypotheses.Strength.SOURCE_ROLE_SIGNAL_CONDITIONAL));
        check(transformed.hypotheses().stream().noneMatch(h->
                h.kind()==PublicOpponentBuildHypotheses.Kind.MOVE && h.id().equals("tackle")
                    && h.publicEvent()));
        check(transformed.fieldsWithNoObservation().contains(PublicOpponentBuildHypotheses.Kind.ABILITY));

        var without=analyze(LOG,List.of(),f);
        check(without.appearances().size()==3);
        check(without.appearances().get(0).hypotheses().stream().noneMatch(h->h.id().equals("swordsdance")));
        check(without.appearances().get(0).hypotheses().stream().anyMatch(h->h.publicEvent()));
        check(without.appearances().get(1).fieldsWithNoObservation().contains(
                PublicOpponentBuildHypotheses.Kind.ITEM));
        var reversed=analyze(LOG,List.of(guessTransformed,guessReturn,guessFirst),f);
        check(reversed.appearances().equals(report.appearances()));

        // Untrusted capability claims and fabricated "confirmed" current sets
        // cannot be promoted through the public record constructors.
        reject(()->new PublicOpponentBuildHypotheses.Hypothesis(
                PublicOpponentBuildHypotheses.Kind.ITEM,"leftovers",
                PublicOpponentBuildHypotheses.Strength.PUBLICLY_OBSERVED_EVENT,true,true,false,false));
        reject(()->new PublicOpponentBuildHypotheses.Hypothesis(
                PublicOpponentBuildHypotheses.Kind.MOVE,"swift",
                PublicOpponentBuildHypotheses.Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN,
                false,false,false,false));
        reject(()->new PublicOpponentBuildHypotheses.Hypothesis(
                PublicOpponentBuildHypotheses.Kind.ABILITY,"illusion",
                PublicOpponentBuildHypotheses.Strength.SOURCE_MOVE_EXISTS_LEARNABILITY_UNKNOWN,
                false,false,true,false));
        reject(()->new PublicOpponentBuildHypotheses.Hypothesis(
                PublicOpponentBuildHypotheses.Kind.MOVE,"swift",
                PublicOpponentBuildHypotheses.Strength.PUBLICLY_OBSERVED_EVENT,
                false,false,true,false));
        reject(()->guess(2,Set.of("Private Secret"),Set.of(),Set.of()));
        reject(()->guess(2,Set.of("unknown"),Set.of(),Set.of()));
        reject(()->guess(2,Set.of(),Set.of("ILLUSION"),Set.of()));
        reject(()->guess(2,Set.of(),Set.of(),Set.of("item*")));
        reject(()->guess(2,Set.of("a","b","c","d","e","f","g","h","i","j","k","l","m"),
                Set.of(),Set.of()));
        reject(()->analyze(LOG,List.of(guess(1,Set.of("swift"),Set.of(),Set.of())),f));
        reject(()->analyze(LOG,List.of(guess(99,Set.of("swift"),Set.of(),Set.of())),f));
        reject(()->analyze(LOG,List.of(guessFirst,guessFirst),f));
        reject(()->PublicOpponentBuildHypotheses.analyze(src(),LOG,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","unknown","unknown"),
                List.of(),f.signed(),f.policy(),T2));
        reject(()->PublicOpponentBuildHypotheses.analyze(src(),LOG,PIN,List.of(),
                null,f.policy(),T2));
        reject(()->PublicOpponentBuildHypotheses.analyze(src(),LOG,PIN,List.of(),
                f.signed(),null,T2));
        reject(()->PublicOpponentBuildHypotheses.analyze(src(),LOG,PIN,List.of(),
                f.signed(),f.policy(),T0)); // future signed timestamp
        reject(()->PublicOpponentBuildHypotheses.analyze(src(),LOG,PIN,List.of(),
                f.signed(),f.policy(),T0+1000)); // expired
        var poisoned=LOG.replace("Shadow Sneak","Unknown Future Move");
        reject(()->analyze(poisoned,List.of(),f));
        reject(()->analyze(LOG+"|request|SECRET_TEST_PERSON_PRIVATE\n",List.of(),f));
        reject(()->analyze(LOG+"|split|p2\n",List.of(),f));
        reject(()->analyze(LOG.replace("Nasty Plot","Swords Dance"),List.of(),f));
        var f2=fixture(LOG.replace("Nasty Plot","Swords Dance"));
        reject(()->analyze(LOG,List.of(),f2));
        check(report.appearances().stream().allMatch(a->a.hypotheses().stream()
                .noneMatch(h->h.speciesLegalityVerified() || h.currentBuildConfirmed())));
        System.out.println("Phase 5.13 synthetic opponent build hypotheses PASS: "+checks+" checks");
    }
}
