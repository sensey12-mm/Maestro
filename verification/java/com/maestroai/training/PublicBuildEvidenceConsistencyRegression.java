package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.16 — synthetic-only build consistency and coverage regression. */
final class PublicBuildEvidenceConsistencyRegression {
    private PublicBuildEvidenceConsistencyRegression(){}
    private static int checks;
    private static void check(boolean ok) {
        if(!ok)throw new AssertionError("Phase 5.16 evidence consistency");
        checks++;
    }
    private static void reject(Runnable task) {
        try{task.run();}
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_SYNTHETIC_PERSON")
                    && !expected.getMessage().contains("SecretNick"));
            return;
        }
        throw new AssertionError("Unsafe competitive build evidence admitted");
    }

    private static final long T=1_700_000_100L;
    private static final String INTRO="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_SYNTHETIC_PERSON\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n";
    private static final String WIN="|win|PRIVATE_SYNTHETIC_PERSON\n";
    private static final PublicBuildEvidenceConsistency.Field MOVE=
            PublicBuildEvidenceConsistency.Field.MOVE;
    private static final PublicBuildEvidenceConsistency.Field ABILITY=
            PublicBuildEvidenceConsistency.Field.ABILITY;
    private static final PublicBuildEvidenceConsistency.Field ITEM=
            PublicBuildEvidenceConsistency.Field.ITEM;
    private static final PublicBuildEvidenceConsistency.Field FORM=
            PublicBuildEvidenceConsistency.Field.FORM;
    private static final PublicBuildEvidenceConsistency.Field ROLE=
            PublicBuildEvidenceConsistency.Field.ROLE;
    private static final PublicBuildEvidenceConsistency.Issue DISAGREEMENT=
            PublicBuildEvidenceConsistency.Issue.OBSERVED_SOURCE_DISAGREEMENT;

    private static String switchTo(String mon) {
        return "|switch|p2a: SecretNick|"+mon+", L100|100/100\n";
    }
    private static String move(String name) {
        return "|move|p2a: SecretNick|"+name+"|p1a: Ally\n";
    }
    private static String ability(String name) {
        return "|-ability|p2a: SecretNick|"+name+"\n";
    }
    private static String item(String name) {
        return "|-item|p2a: SecretNick|"+name+"\n";
    }
    private static PublicOpponentBuildHypotheses.CandidateQuery guess(int index,
            String[] moves,String[] abilities,String[] items) {
        return new PublicOpponentBuildHypotheses.CandidateQuery(index,
                Set.of(moves),Set.of(abilities),Set.of(items));
    }
    private record Fixture(PublicReplayCorpus.Source source,String log,
                           List<PublicOpponentBuildHypotheses.CandidateQuery> candidates,
                           PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy policy) {
        PublicBuildEvidenceConsistency.Report run() {return run(Set.of());}
        PublicBuildEvidenceConsistency.Report run(Set<PublicFormBuildReconciliation.OverrideNotice> overrides) {
            return PublicBuildEvidenceConsistency.audit(source,log,candidates,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    overrides,signed,policy,T);
        }
    }
    private static Fixture fixture(int id,String replay,
                            List<PublicOpponentBuildHypotheses.CandidateQuery> candidates) {
        try {
            var source=new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,replay);
            var d=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    T-25,T+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(d));
            var key=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(key.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"auditor",
                    T-10,"0".repeat(64),ledger));
            var signed=new PublicSignedCustody.SignedManifest(1,"auditor",T-10,
                    "0".repeat(64),ledger,Base64.getEncoder().encodeToString(signer.sign()));
            var publicKey=new PublicSignedCustody.TrustedKey("auditor",
                    Base64.getEncoder().encodeToString(key.getPublic().getEncoded()),T-100,0);
            var policy=new PublicSignedCustody.TrustedPolicy(1,1,1,T-100,"",List.of(publicKey));
            return new Fixture(source,replay,candidates,signed,policy);
        } catch(GeneralSecurityException error) {throw new AssertionError(error);}
    }
    private static PublicBuildEvidenceConsistency.FieldSummary field(
            PublicBuildEvidenceConsistency.Appearance a,
            PublicBuildEvidenceConsistency.Field field) {
        return Objects.requireNonNull(a.fields().get(field));
    }
    private static PublicFormBuildReconciliation.OverrideNotice override(
            String id,PublicFormBuildReconciliation.Field field) {
        return new PublicFormBuildReconciliation.OverrideNotice(id,field);
    }

    static void run() {
        // Ordinary source-supported events, unsupported guesses, conditional
        // roles and unverified items must remain separate reporting dimensions.
        String ordinary=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +ability("Illusion")+item("Leftovers")+move("Nasty Plot")+WIN;
        var ordinaryFixture=fixture(12345678,ordinary,List.of(guess(2,
                new String[]{"swordsdance","shadowsneak"},
                new String[]{"pressure"},new String[]{"choicescarf","choiceband"})));
        var ordinaryReport=ordinaryFixture.run();
        check(ordinaryReport.appearances().size()==1);
        check(ordinaryReport.versionScope().contains(
                PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(ordinaryReport.everyOpponentIdentityUncertain()
                && !ordinaryReport.installedGameLegalityVerified()
                && !ordinaryReport.itemLegalityVerified()
                && !ordinaryReport.completeOpponentBuildVerified()
                && !ordinaryReport.probabilitiesCalibrated()
                && !ordinaryReport.shadowTacticalReady()
                && !ordinaryReport.livePromotionAuthorized());
        check(!ordinaryReport.toString().contains("PRIVATE_SYNTHETIC_PERSON")
                && !ordinaryReport.toString().contains("gen9ou-")
                && !ordinaryReport.toString().contains("SecretNick")
                && !ordinaryReport.toString().contains("leftovers"));
        check(Arrays.stream(PublicBuildEvidenceConsistency.Report.class
                .getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));
        var a=ordinaryReport.appearances().get(0);
        check(a.index()==2 && a.identityUncertain()
                && a.exactDisplayedSourceForm() && !a.transformed());
        check(a.fields().keySet().equals(EnumSet.allOf(
                PublicBuildEvidenceConsistency.Field.class)));
        check(field(a,MOVE).observed()==1 && field(a,MOVE).proposed()==3);
        check(field(a,MOVE).sourceListed()==2
                && field(a,MOVE).sourceUnsupported()==1);
        check(field(a,MOVE).conflicts()==0 && field(a,MOVE).unresolved()==0);
        check(field(a,MOVE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.UNOBSERVED_CANDIDATE_NOT_LISTED));
        check(field(a,ABILITY).observed()==1 && field(a,ABILITY).sourceListed()==1);
        check(field(a,ABILITY).sourceUnsupported()==1);
        check(field(a,ITEM).observed()==1 && field(a,ITEM).proposed()==3
                && field(a,ITEM).unresolved()==3
                && field(a,ITEM).conflicts()==0);
        check(field(a,ITEM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.ITEM_ELIGIBILITY_UNVERIFIED));
        check(field(a,ROLE).observed()==0 && field(a,ROLE).unresolved()==1);
        check(field(a,ROLE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.CONDITIONAL_ROLE_ONLY));
        check(field(a,FORM).proposed()==1 && field(a,FORM).sourceListed()==1
                && field(a,FORM).observed()==0);
        check(field(a,FORM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.DISPLAYED_IDENTITY_NOT_PROVEN));
        check(!a.gates().contains(PublicBuildEvidenceConsistency.Gate.ABSTAIN_SOURCE_DISAGREEMENT));
        check(a.gates().contains(PublicBuildEvidenceConsistency.Gate.ABSTAIN_INCOMPLETE_COVERAGE)
                && a.gates().contains(PublicBuildEvidenceConsistency.Gate.ABSTAIN_IDENTITY_UNCERTAIN));
        check(ordinaryReport.sourceDisagreementAppearances()==0
                && ordinaryReport.abstainingAppearances()==1);
        check(ordinaryReport.totals().get(MOVE).proposed()==3
                && ordinaryReport.totals().get(MOVE).unsupported()==1);
        check(ordinaryReport.totals().get(ROLE).appearancesWithNoObservation()==1);
        try {ordinaryReport.appearances().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        try {a.fields().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        try {field(a,MOVE).issues().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}

        // A witnessed move absent from pinned native moves is a SOURCE
        // discrepancy, never proof of cheating or confirmed impossible set.
        String discrepancy=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +move("Shadow Sneak")+ability("Pressure")+WIN;
        var conflicting=fixture(12345679,discrepancy,List.of());
        var flagged=conflicting.run();
        var conflict=flagged.appearances().get(0);
        check(field(conflict,MOVE).conflicts()==1 && field(conflict,MOVE).observed()==1);
        check(field(conflict,ABILITY).conflicts()==1 && field(conflict,ABILITY).observed()==1);
        check(field(conflict,ITEM).conflicts()==0);
        check(field(conflict,MOVE).issues().contains(DISAGREEMENT));
        check(field(conflict,ABILITY).issues().contains(DISAGREEMENT));
        check(conflict.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_SOURCE_DISAGREEMENT));
        check(flagged.sourceDisagreementAppearances()==1);
        check(flagged.totals().get(MOVE).conflicts()==1
                && flagged.totals().get(ABILITY).conflicts()==1);

        // Different public values may reflect perfectly legal item swapping
        // or temporary abilities. They cannot be collapsed into one native set.
        String transitions=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +ability("Illusion")+ability("Pressure")
                +item("Leftovers")+item("Choice Scarf")
                +move("Nasty Plot")+WIN;
        var changing=fixture(12345680,transitions,List.of()).run().appearances().get(0);
        check(field(changing,ABILITY).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED));
        check(field(changing,ITEM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED));
        check(changing.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_INCOMPLETE_COVERAGE));
        check(field(changing,ABILITY).observed()==1
                && field(changing,ITEM).observed()==1);
        check(field(changing,ITEM).conflicts()==0); // no item legality catalog

        // Own-side changes must not contaminate p2-only evidence.
        String own=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +"|-ability|p1a: Ally|Pressure\n"
                +"|-ability|p1a: Ally|Illusion\n"
                +"|-item|p1a: Ally|Leftovers\n"
                +"|-item|p1a: Ally|Choice Band\n"
                +ability("Illusion")+move("Nasty Plot")+WIN;
        var ownCase=fixture(12345681,own,List.of()).run().appearances().get(0);
        check(!field(ownCase,ABILITY).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ABILITY_VALUES_UNRESOLVED));
        check(!field(ownCase,ITEM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_PUBLIC_ITEM_VALUES_UNRESOLVED));

        // Multiple observed native moves may reflect changing battle effects:
        // no "illegal five-move set" judgment is made.
        String five=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+move("Swift")+move("Swords Dance")
                +move("U-turn")+move("Shadow Claw")+WIN;
        var fiveCase=fixture(12345682,five,List.of()).run().appearances().get(0);
        check(field(fiveCase,MOVE).observed()==5);
        check(field(fiveCase,MOVE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.MULTIPLE_OBSERVED_MOVES_NOT_A_FIXED_SET));
        check(fiveCase.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_INCOMPLETE_COVERAGE));

        // Mid-appearance public form changes are not proof of a single
        // static, native form-specific legal build.
        String changingForm=INTRO+switchTo("Mewtwo")+"|turn|1\n"
                +"|detailschange|p2a: SecretNick|Mewtwo-Mega-Y, L100\n"
                +move("Psystrike")+WIN;
        var fm=fixture(12345683,changingForm,List.of()).run().appearances().get(0);
        check(field(fm,FORM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.PUBLIC_FORM_DISPLAY_TRANSITION_UNRESOLVED));
        check(fm.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_TRANSFORM_OR_FORM));

        // Explicit form replacement vs inherited base methods and abilities
        // survive the consistency audit without claiming actual runtime laws.
        String hLog=INTRO+switchTo("Zoroark-Hisui")+"|turn|1\n"
                +move("Shadow Sneak")+ability("Illusion")+WIN;
        var h=fixture(12345684,hLog,List.of(guess(2,
                new String[]{"nastyplot","pursuit"},new String[]{"imposter"},
                new String[]{}))).run().appearances().get(0);
        check(field(h,MOVE).sourceListed()==2
                && field(h,MOVE).sourceUnsupported()==1);
        check(field(h,ABILITY).sourceListed()==1
                && field(h,ABILITY).sourceUnsupported()==1);
        check(field(h,MOVE).conflicts()==0);
        check(h.exactDisplayedSourceForm());

        // Unknown forms never gain base learnsets through a fallback.
        String unknown=INTRO+switchTo("Mewtwo-Mega-Z")+"|turn|1\n"
                +move("Psystrike")+WIN;
        var uf=fixture(12345685,unknown,List.of(guess(2,
                new String[]{"swift"},new String[]{"pressure"},new String[]{})))
                .run().appearances().get(0);
        check(!uf.exactDisplayedSourceForm());
        check(field(uf,FORM).issues().contains(
                PublicBuildEvidenceConsistency.Issue.DISPLAYED_FORM_NOT_SOURCE_SUPPORTED));
        check(field(uf,MOVE).unresolved()==2
                && field(uf,ABILITY).unresolved()==1);
        check(uf.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_TRANSFORM_OR_FORM));

        // An explicit override flag can only downgrade source knowledge.
        var override=ordinaryFixture.run(Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                "zoroark",PublicFormBuildReconciliation.Field.MOVES)));
        var downgraded=override.appearances().get(0);
        check(field(downgraded,MOVE).sourceListed()==0
                && field(downgraded,MOVE).unresolved()==3);
        check(field(downgraded,MOVE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.UNRESOLVED_SOURCE_OR_OVERRIDE));
        check(override.sourceDisagreementAppearances()==0);
        check(field(downgraded,ITEM).unresolved()==field(a,ITEM).unresolved());

        // Transform copied moves/abilities are not native builds.
        String transformed=INTRO+switchTo("Ditto")+"|turn|1\n"
                +"|-transform|p2a: SecretNick|p1a: Ally\n"+move("Tackle")+WIN;
        var tr=fixture(12345686,transformed,List.of(guess(2,
                new String[]{"transform"},new String[]{"imposter"},
                new String[]{"leftovers"}))).run().appearances().get(0);
        check(tr.transformed());
        check(tr.gates().contains(
                PublicBuildEvidenceConsistency.Gate.ABSTAIN_TRANSFORM_OR_FORM));
        check(field(tr,MOVE).issues().contains(
                PublicBuildEvidenceConsistency.Issue.TRANSFORM_CANNOT_VERIFY_NATIVE_BUILD));
        check(field(tr,ABILITY).sourceListed()==0);
        check(field(tr,ITEM).unresolved()==1);

        // Repeat display does not carry confirmed moves, ability or items.
        String repeat=INTRO+switchTo("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+ability("Illusion")+item("Leftovers")
                +switchTo("Zoroark")+"|turn|2\n"+move("Swift")+WIN;
        var repeated=fixture(12345687,repeat,List.of()).run();
        check(repeated.appearances().size()==2);
        check(repeated.appearances().get(0).index()==2
                && repeated.appearances().get(1).index()==3);
        var first=repeated.appearances().get(0);
        var second=repeated.appearances().get(1);
        check(field(first,MOVE).observed()==1 && field(second,MOVE).observed()==1);
        check(field(first,ABILITY).observed()==1
                && field(second,ABILITY).observed()==0);
        check(field(first,ITEM).observed()==1 && field(second,ITEM).observed()==0);
        check(repeated.totals().get(MOVE).observed()==2
                && repeated.totals().get(ITEM).appearancesWithNoObservation()==1);
        check(second.identityUncertain() && first.identityUncertain());

        // Fail closed on external version, unsafely proposed side indices,
        // tampered/expired/revoked custody and spectator private packets.
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),null,
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                null,ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),null,T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T+1001));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log()+"|request|PRIVATE_SYNTHETIC_PERSON\n",
                ordinaryFixture.candidates(),PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log().replace("Nasty Plot","Shadow Claw"),
                ordinaryFixture.candidates(),PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),List.of(guess(1,new String[]{"swift"},
                        new String[]{},new String[]{})),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->PublicBuildEvidenceConsistency.audit(ordinaryFixture.source(),
                ordinaryFixture.log(),ordinaryFixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                        "fakeform",PublicFormBuildReconciliation.Field.MOVES)),
                ordinaryFixture.signed(),ordinaryFixture.policy(),T));
        reject(()->new PublicBuildEvidenceConsistency.FieldSummary(MOVE,3,1,
                0,0,0,0,Set.of()));
        reject(()->new PublicBuildEvidenceConsistency.FieldSummary(ITEM,0,1,
                1,1,0,0,Set.of()));
        reject(()->new PublicBuildEvidenceConsistency.Appearance(1,false,false,true,
                Map.of(),Set.of()));
        reject(()->new PublicBuildEvidenceConsistency.FieldTotal(MOVE,4,1,
                0,0,0,0,0));
        check(ordinaryReport.appearances().stream().allMatch(row->
                row.identityUncertain() && row.gates().contains(
                        PublicBuildEvidenceConsistency.Gate.ABSTAIN_IDENTITY_UNCERTAIN)));
        System.out.println("Phase 5.16 synthetic consistency and coverage PASS: "+checks+" checks");
    }
}
