package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.17: synthetic-only, signed, candidate-scoped adversarial fixtures. */
final class PublicBuildCandidateFilterRegression {
    private PublicBuildCandidateFilterRegression() {}
    private static int checks;
    private static void check(boolean condition) {
        if(!condition)throw new AssertionError("Phase 5.17 candidate filter regression");
        checks++;
    }
    private static void reject(Runnable operation) {
        try{operation.run();}
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("PRIVATE_SYNTHETIC_PLAYER")
                    && !expected.getMessage().contains("SecretNick"));
            return;
        }
        throw new AssertionError("Unsafe candidate hypothesis was accepted");
    }
    private static final long NOW=1_700_000_100L;
    private static final String HEADER="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_SYNTHETIC_PLAYER\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n";
    private static final String WIN="|win|PRIVATE_SYNTHETIC_PLAYER\n";
    private static String enter(String species) {
        return "|switch|p2a: SecretNick|"+species+", L100|100/100\n";
    }
    private static String move(String id) {
        return "|move|p2a: SecretNick|"+id+"|p1a: Ally\n";
    }
    private static String ability(String id) {
        return "|-ability|p2a: SecretNick|"+id+"\n";
    }
    private static String item(String id) {
        return "|-item|p2a: SecretNick|"+id+"\n";
    }
    private static PublicBuildCandidateFilter.Candidate build(int appearance,String form,
                              String[] moves,String[] excluded,
                              String[] abilities,String[] items) {
        return new PublicBuildCandidateFilter.Candidate(appearance,form,
                Set.of(moves),Set.of(excluded),Set.of(abilities),Set.of(items));
    }
    private static PublicBuildCandidateFilter.Candidate simple(int appearance,String form,
                                                                  String move) {
        return build(appearance,form,new String[]{move},new String[]{},
                new String[]{},new String[]{});
    }
    private static final PublicBuildCandidateFilter.Verdict SUPPORTED=
            PublicBuildCandidateFilter.Verdict.SUPPORTED;
    private static final PublicBuildCandidateFilter.Verdict CONTRADICTED=
            PublicBuildCandidateFilter.Verdict.CONTRADICTED;
    private static final PublicBuildCandidateFilter.Verdict UNRESOLVED=
            PublicBuildCandidateFilter.Verdict.UNRESOLVED;
    private static final PublicBuildCandidateFilter.Verdict ABSTAINED=
            PublicBuildCandidateFilter.Verdict.ABSTAINED;
    private static final PublicBuildCandidateFilter.Reason EXCLUDED=
            PublicBuildCandidateFilter.Reason.EXPLICIT_EXCLUSION_VIOLATED;

    private record Fixture(PublicReplayCorpus.Source source,String log,
                           PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy policy) {
        PublicBuildCandidateFilter.Report run(List<PublicBuildCandidateFilter.Candidate> c) {
            return run(c,Set.of());
        }
        PublicBuildCandidateFilter.Report run(List<PublicBuildCandidateFilter.Candidate> c,
                      Set<PublicFormBuildReconciliation.OverrideNotice> notices) {
            return PublicBuildCandidateFilter.filter(source,log,c,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    notices,signed,policy,NOW);
        }
    }
    private static Fixture fixture(int id,String log) {
        try {
            var source=new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,log);
            var decision=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    NOW-25,NOW+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Signature verifier=Signature.getInstance("Ed25519");
            verifier.initSign(pair.getPrivate());
            verifier.update(PublicSignedCustody.canonicalBytes(1,"test-signature",
                    NOW-10,"0".repeat(64),ledger));
            var manifest=new PublicSignedCustody.SignedManifest(1,"test-signature",
                    NOW-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(verifier.sign()));
            var trust=new PublicSignedCustody.TrustedKey("test-signature",
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    NOW-100,0);
            var policy=new PublicSignedCustody.TrustedPolicy(1,1,1,NOW-100,
                    "",List.of(trust));
            return new Fixture(source,log,manifest,policy);
        }catch(GeneralSecurityException ex){throw new AssertionError(ex);}
    }
    private static PublicBuildCandidateFilter.Result result(
            PublicBuildCandidateFilter.Report report,int ordinal) {
        return report.results().get(ordinal-1);
    }
    private static void expected(PublicBuildCandidateFilter.Report report,int ordinal,
                                 PublicBuildCandidateFilter.Verdict verdict) {
        check(result(report,ordinal).inputOrdinal()==ordinal
                && result(report,ordinal).verdict()==verdict);
        check(result(report,ordinal).reasons().contains(
                PublicBuildCandidateFilter.Reason.IDENTITY_REMAINS_UNCERTAIN));
        check(!result(report,ordinal).opponentIdentityResolved()
                && !result(report,ordinal).completeNativeMovesetVerified()
                && !result(report,ordinal).installedLegalityVerified());
    }

    static void run(){
        check(PublicBuildCandidateFilter.VERSION==1);
        check(PublicBuildCandidateFilter.MAX_CANDIDATES==40);
        check(PublicBuildCandidateFilter.SOURCE_REVISION.equals(
                PublicSpeciesBuildLegality.SOURCE_REVISION));
        String standard=HEADER+enter("Zoroark")+"|turn|1\n"
                +ability("Illusion")+item("Leftovers")+move("Nasty Plot")+WIN;
        var s=fixture(12345678,standard);
        var proposals=List.of(
                build(2,"zoroark",new String[]{"nastyplot","swordsdance"},
                        new String[]{},new String[]{"illusion"},new String[]{}),
                build(2,"zoroark",new String[]{"swift"},
                        new String[]{"nastyplot"},new String[]{},new String[]{}),
                simple(2,"zoroark","shadowsneak"),
                build(2,"zoroark",new String[]{},
                        new String[]{},new String[]{"pressure"},new String[]{}),
                build(2,"zoroark",new String[]{},
                        new String[]{},new String[]{},new String[]{"leftovers"}),
                simple(2,"zoroarkhisui","nastyplot"),
                build(2,"zoroark",new String[]{},new String[]{},new String[]{},new String[]{}),
                simple(2,"zoroark","frustration"),
                simple(2,"zoroark","nastyplot")
        );
        var report=s.run(proposals);
        check(report.results().size()==9);
        check(report.sourceScope().contains(PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(!report.anyOpponentIdentityConfirmed()
                && !report.anyCompleteBuildConfirmed()
                && !report.calibratedProbabilitiesProduced()
                && !report.installedModpackLegalitiesVerified()
                && !report.shadowTacticalReady() && !report.livePromotionAuthorized());
        check(!report.toString().contains("PRIVATE_SYNTHETIC_PLAYER")
                && !report.toString().contains("SecretNick")
                && !report.toString().contains("gen9ou-")
                && !report.toString().contains("nastyplot"));
        check(Arrays.stream(PublicBuildCandidateFilter.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        expected(report,1,SUPPORTED);
        expected(report,2,CONTRADICTED);
        expected(report,3,CONTRADICTED);
        expected(report,4,UNRESOLVED);
        expected(report,5,UNRESOLVED);
        expected(report,6,CONTRADICTED);
        expected(report,7,UNRESOLVED);
        expected(report,8,UNRESOLVED);
        expected(report,9,SUPPORTED);
        check(result(report,1).sourceSupportedAssertions()==3);
        check(result(report,1).observedMatches()==2);
        check(result(report,1).directContradictions()==0);
        check(result(report,2).reasons().contains(EXCLUDED));
        check(result(report,2).directContradictions()==1);
        check(result(report,3).reasons().contains(
                PublicBuildCandidateFilter.Reason.MOVE_NOT_LISTED_IN_PINNED_SOURCE));
        check(result(report,4).reasons().contains(
                PublicBuildCandidateFilter.Reason.ABILITY_NOT_LISTED_IN_PINNED_SOURCE));
        check(result(report,5).reasons().contains(
                PublicBuildCandidateFilter.Reason.ITEM_ELIGIBILITY_NOT_ATTESTED));
        check(result(report,6).reasons().contains(
                PublicBuildCandidateFilter.Reason.DISPLAY_FORM_MISMATCH));
        check(result(report,7).reasons().contains(
                PublicBuildCandidateFilter.Reason.NO_POSITIVE_BUILD_ASSERTIONS));
        check(result(report,8).reasons().contains(
                PublicBuildCandidateFilter.Reason.NONLEGAL_QUERY_METHOD_ONLY));
        check(result(report,9).observedMatches()==1);
        check(report.totals().get(SUPPORTED)==2 && report.totals().get(CONTRADICTED)==3
                && report.totals().get(UNRESOLVED)==4
                && report.totals().get(ABSTAINED)==0);
        try{report.results().clear();throw new AssertionError("mutated results");}
        catch(UnsupportedOperationException expected){checks++;}
        try{report.totals().clear();throw new AssertionError("mutated totals");}
        catch(UnsupportedOperationException expected){checks++;}
        try{result(report,1).reasons().clear();throw new AssertionError("mutated reasons");}
        catch(UnsupportedOperationException expected){checks++;}

        // A hypothetical ability/item is not proof that it was selected.
        check(result(report,4).observedMatches()==0);
        check(result(report,5).sourceSupportedAssertions()==0);
        check(result(report,5).unknownAssertions()==1);

        // A source-known form inherits its base move slots without linking
        // to a prior opponent appearance and without certifying a real set.
        String hisui=HEADER+enter("Zoroark-Hisui")+"|turn|1\n"
                +move("Shadow Sneak")+WIN;
        var h=fixture(12345679,hisui).run(List.of(
                simple(2,"zoroarkhisui","shadowsneak"),
                build(2,"zoroarkhisui",new String[]{},new String[]{},
                        new String[]{"illusion"},new String[]{}),
                simple(2,"zoroarkhisui","happyhour")));
        expected(h,1,SUPPORTED);
        expected(h,2,SUPPORTED);
        expected(h,3,UNRESOLVED);
        check(result(h,1).observedMatches()==1);
        check(result(h,2).sourceSupportedAssertions()==1);

        String mega=HEADER+enter("Mewtwo-Mega-Y")+"|turn|1\n"
                +move("Psystrike")+WIN;
        var m=fixture(12345680,mega).run(List.of(
                simple(2,"mewtwomegay","psystrike"),
                build(2,"mewtwomegay",new String[]{},new String[]{},
                        new String[]{"insomnia"},new String[]{}),
                build(2,"mewtwomegay",new String[]{},new String[]{},
                        new String[]{"pressure"},new String[]{})));
        expected(m,1,SUPPORTED);
        expected(m,2,SUPPORTED);
        expected(m,3,UNRESOLVED);

        // An actually observed move missing from an exact source slice
        // abstains; it does NOT prove an illegal game state.
        String disagreement=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Shadow Sneak")+WIN;
        var conflict=fixture(12345681,disagreement).run(List.of(
                simple(2,"zoroark","nastyplot"),
                simple(2,"zoroark","shadowsneak")));
        expected(conflict,1,ABSTAINED);
        expected(conflict,2,ABSTAINED);
        check(result(conflict,1).reasons().contains(
                PublicBuildCandidateFilter.Reason.PUBLIC_SOURCE_DISAGREEMENT));

        // Overrides downgrade ONLY the affected source field.
        var override=s.run(List.of(
                simple(2,"zoroark","nastyplot"),
                build(2,"zoroark",new String[]{},new String[]{},
                        new String[]{"illusion"},new String[]{})),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice("zoroark",
                        PublicFormBuildReconciliation.Field.MOVES)));
        expected(override,1,ABSTAINED);
        expected(override,2,SUPPORTED);
        check(result(override,1).reasons().contains(
                PublicBuildCandidateFilter.Reason.INCOMPLETE_OR_OVERRIDDEN_FIELD));

        // Unsupported forms, Transform, public form and ability/item
        // changes are abstentions, not evidence of impossible legal sets.
        String form=HEADER+enter("Mewtwo-Mega-Z")+"|turn|1\n"
                +move("Psystrike")+WIN;
        expected(fixture(12345682,form).run(List.of(
                simple(2,"mewtwomegaz","psystrike"))),1,ABSTAINED);
        String transform=HEADER+enter("Ditto")+"|turn|1\n"
                +"|-transform|p2a: SecretNick|p1a: Ally\n"
                +move("Tackle")+WIN;
        expected(fixture(12345683,transform).run(List.of(
                simple(2,"ditto","transform"))),1,ABSTAINED);
        String change=HEADER+enter("Mewtwo")+"|turn|1\n"
                +"|detailschange|p2a: SecretNick|Mewtwo-Mega-Y, L100\n"
                +move("Psystrike")+WIN;
        var transition=fixture(12345684,change).run(List.of(
                simple(2,"mewtwomegay","psystrike")));
        expected(transition,1,ABSTAINED);
        check(result(transition,1).reasons().contains(
                PublicBuildCandidateFilter.Reason.FORM_DISPLAY_TRANSITION));
        String abilities=HEADER+enter("Zoroark")+"|turn|1\n"
                +ability("Illusion")+ability("Pressure")+move("Nasty Plot")+WIN;
        expected(fixture(12345685,abilities).run(List.of(
                simple(2,"zoroark","nastyplot"))),1,ABSTAINED);
        String items=HEADER+enter("Zoroark")+"|turn|1\n"
                +item("Leftovers")+item("Choice Scarf")+move("Nasty Plot")+WIN;
        expected(fixture(12345686,items).run(List.of(
                simple(2,"zoroark","nastyplot"))),1,ABSTAINED);
        String five=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+move("Swift")+move("Swords Dance")
                +move("U-turn")+move("Shadow Claw")+WIN;
        var overfour=fixture(12345687,five).run(List.of(
                simple(2,"zoroark","nastyplot")));
        expected(overfour,1,ABSTAINED);
        check(result(overfour,1).reasons().contains(
                PublicBuildCandidateFilter.Reason.TOO_MANY_PUBLIC_NATIVE_MOVES));

        // Re-entry cannot carry information between chronological entries.
        String repeat=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+enter("Zoroark")+"|turn|2\n"
                +move("Swift")+WIN;
        var reused=fixture(12345688,repeat).run(List.of(
                build(3,"zoroark",new String[]{"swift"},
                        new String[]{"nastyplot"},new String[]{},new String[]{}),
                build(2,"zoroark",new String[]{"nastyplot"},
                        new String[]{"swift"},new String[]{},new String[]{}),
                build(2,"zoroark",new String[]{"nastyplot"},
                        new String[]{"nastyplot2"},new String[]{},new String[]{})));
        expected(reused,1,SUPPORTED);
        expected(reused,2,SUPPORTED);
        expected(reused,3,SUPPORTED);
        check(result(reused,1).appearanceIndex()==3);
        check(result(reused,2).appearanceIndex()==2);

        // Caller cannot use a p1 entry, duplicate guessed IDs, invalid
        // canonical identifiers, versions, forged or expired replay custody.
        reject(()->build(2,"Zoroark",new String[]{},new String[]{},
                new String[]{},new String[]{}));
        reject(()->build(2,"zoroark",new String[]{"nastyplot"},
                new String[]{"nastyplot"},new String[]{},new String[]{}));
        reject(()->build(2,"zoroark",new String[]{},new String[]{},
                new String[]{"illusion","pressure"},new String[]{}));
        reject(()->build(2,"zoroark",new String[]{"private move"},
                new String[]{},new String[]{},new String[]{}));
        reject(()->s.run(List.of(simple(1,"pikachu","swift"))));
        reject(()->s.run(List.of(simple(99,"zoroark","nastyplot"))));
        reject(()->s.run(Arrays.asList((PublicBuildCandidateFilter.Candidate)null)));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                null,PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),s.signed(),s.policy(),NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,
                Set.of(),s.signed(),s.policy(),NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),null,s.policy(),NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),s.signed(),null,NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),s.signed(),s.policy(),NOW+2000));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),
                s.log().replace("Nasty Plot","Shadow Claw"),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),s.signed(),s.policy(),NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),
                s.log()+"|request|PRIVATE_SYNTHETIC_PLAYER\n",proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),s.signed(),s.policy(),NOW));
        reject(()->PublicBuildCandidateFilter.filter(s.source(),s.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                        "unknownform",PublicFormBuildReconciliation.Field.MOVES)),
                s.signed(),s.policy(),NOW));
        reject(()->new PublicBuildCandidateFilter.Result(1,2,SUPPORTED,Set.of(
                PublicBuildCandidateFilter.Reason.IDENTITY_REMAINS_UNCERTAIN),
                0,0,0,0,false,false,false));
        reject(()->new PublicBuildCandidateFilter.Result(1,2,CONTRADICTED,Set.of(
                PublicBuildCandidateFilter.Reason.IDENTITY_REMAINS_UNCERTAIN),
                0,0,0,1,false,false,false));
        reject(()->new PublicBuildCandidateFilter.Result(1,2,ABSTAINED,Set.of(
                PublicBuildCandidateFilter.Reason.IDENTITY_REMAINS_UNCERTAIN),
                1,0,0,0,false,false,false));
        check(s.run(List.of()).results().isEmpty());
        check(s.run(List.of()).totals().values().stream().allMatch(n->n==0));
        System.out.println("Phase 5.17 synthetic candidate filtering PASS: "+checks+" checks");
    }
}
