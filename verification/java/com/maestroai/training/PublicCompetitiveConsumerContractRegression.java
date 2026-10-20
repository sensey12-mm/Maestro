package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.19: ephemeral-signed synthetic data; no real replay or player data. */
final class PublicCompetitiveConsumerContractRegression {
    private PublicCompetitiveConsumerContractRegression(){}
    private static int checks;
    private static void check(boolean yes){
        if(!yes)throw new AssertionError("Phase 5.19 offline consumer boundary");
        checks++;
    }
    private static void reject(Runnable r) {
        try{r.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains("PRIVATE_TEST_TRAINER")
                    && !expected.getMessage().contains("EnemyNickname"));
            return;
        }
        throw new AssertionError("Phase 5.19 accepted invalid consumer evidence");
    }
    private static final long NOW=1_700_000_100L;
    private static final String HEADER="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_TEST_TRAINER\n|start\n"
            +"|switch|p1a: Friend|Pikachu, L100|100/100\n";
    private static final String WIN="|win|PRIVATE_TEST_TRAINER\n";
    private static final PublicCompetitiveConsumerContract.Uncertainty ID=
            PublicCompetitiveConsumerContract.Uncertainty.APPEARANCE_NOT_UNIQUE_OPPONENT_IDENTITY;
    private static final PublicCompetitiveConsumerContract.Classification SOURCE=
            PublicCompetitiveConsumerContract.Classification.SOURCE_CONDITIONAL_ONLY;
    private static final PublicCompetitiveConsumerContract.Classification CONTRADICTION=
            PublicCompetitiveConsumerContract.Classification.ASSERTION_DISAGREEMENT_ONLY;
    private static final PublicCompetitiveConsumerContract.Classification UNKNOWN=
            PublicCompetitiveConsumerContract.Classification.UNRESOLVED;
    private static final PublicCompetitiveConsumerContract.Classification ABSTAIN=
            PublicCompetitiveConsumerContract.Classification.ABSTAINED;

    private static String enter(String species){
        return "|switch|p2a: EnemyNickname|"+species+", L100|100/100\n";
    }
    private static String move(String name){
        return "|move|p2a: EnemyNickname|"+name+"|p1a: Friend\n";
    }
    private static String ability(String name){
        return "|-ability|p2a: EnemyNickname|"+name+"\n";
    }
    private static String item(String name){
        return "|-item|p2a: EnemyNickname|"+name+"\n";
    }
    private static PublicBuildCandidateFilter.Candidate candidate(int index,String display,
                    String[] moves,String[] negative,String[] abilities,String[] items){
        return new PublicBuildCandidateFilter.Candidate(index,display,
                Set.of(moves),Set.of(negative),Set.of(abilities),Set.of(items));
    }
    private static PublicBuildCandidateFilter.Candidate moveCandidate(int index,
            String display,String move){
        return candidate(index,display,new String[]{move},new String[]{},
                new String[]{},new String[]{});
    }
    private record Fixture(PublicReplayCorpus.Source source,String log,
                           PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy trusted){
        PublicCompetitiveConsumerContract.Export run(List<PublicBuildCandidateFilter.Candidate> builds) {
            return run(builds,Set.of());
        }
        PublicCompetitiveConsumerContract.Export run(List<PublicBuildCandidateFilter.Candidate> builds,
                Set<PublicFormBuildReconciliation.OverrideNotice> overrides){
            return PublicCompetitiveConsumerContract.prepare(source,log,builds,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    overrides,signed,trusted,NOW,
                    PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned());
        }
    }
    private static Fixture fixture(int serial,String log){
        try{
            var source=new PublicReplayCorpus.Source("gen9ou-"+serial,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,log);
            var decision=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    NOW-50,NOW+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            KeyPair keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Signature signing=Signature.getInstance("Ed25519");
            signing.initSign(keys.getPrivate());
            signing.update(PublicSignedCustody.canonicalBytes(1,"contract-test",
                    NOW-10,"0".repeat(64),ledger));
            var manifest=new PublicSignedCustody.SignedManifest(1,"contract-test",
                    NOW-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(signing.sign()));
            var key=new PublicSignedCustody.TrustedKey("contract-test",
                    Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),
                    NOW-100,0);
            return new Fixture(source,log,manifest,new PublicSignedCustody.TrustedPolicy(
                    1,1,1,NOW-100,"",List.of(key)));
        }catch(GeneralSecurityException e){throw new AssertionError(e);}
    }
    private static PublicCompetitiveConsumerContract.Coverage coverage(
            PublicCompetitiveConsumerContract.Appearance a,
            PublicBuildEvidenceConsistency.Field field){
        return a.fields().get(field);
    }
    private static void safe(PublicCompetitiveConsumerContract.Export report){
        check(report.schemaVersion()==1
                && report.sourceRevision().equals(PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(report.mechanicsScope().equals(CobblemonMechanics.SOURCE));
        check(report.policySha256().equals(PublicBuildCandidateRobustness.frozenPolicySha256()));
        check(report.currentSignedCustodyRevalidated()
                && !report.replayUseRightsIndependentlyVerified());
        check(!report.installedRegistryVerified() && !report.currentBattleBound()
                && !report.opponentIdentityConfirmed() && !report.legalMovesetsConfirmed());
        check(!report.itemOrAbilityBuildConfirmed() && !report.calibratedPredictionsAvailable());
        check(!report.isShadowTacticalInput() && !report.shadowTacticalReady()
                && !report.livePromotionAuthorized());
        check(report.appearances().stream().allMatch(a->a.identityUncertain()
                && a.uncertainty().contains(ID)));
        check(report.candidates().stream().allMatch(c->c.uncertainty().contains(ID)
                && !c.actualOpponentEliminationAllowed()
                && c.evaluatedScenarios()==9));
        check(!report.toString().contains("PRIVATE_TEST_TRAINER")
                && !report.toString().contains("EnemyNickname")
                && !report.toString().contains("nastyplot")
                && !report.toString().contains("gen9ou-"));
    }
    private static void verdict(PublicCompetitiveConsumerContract.Export report,int index,
                                int appearance,PublicCompetitiveConsumerContract.Classification cls){
        var row=report.candidates().get(index-1);
        check(row.ordinal()==index && row.appearanceIndex()==appearance
                && row.classification()==cls);
    }
    static void run() {
        var pin=PublicCompetitiveConsumerContract.ContractPin.pinned();
        check(pin.version()==1 && pin.normalizedSpeciesSha256()
                .equals(PublicSpeciesBuildLegality.NORMALIZED_SHA256));
        check(pin.resolverSourceRevision().equals(PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(pin.robustnessPolicySha256().equals(
                PublicBuildCandidateRobustness.frozenPolicySha256()));
        var requested=PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned();
        check(requested.purpose()==PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW
                && !requested.currentBattleBindingRequested()
                && !requested.tacticalUseRequested());
        reject(()->new PublicCompetitiveConsumerContract.ContractPin(2,
                pin.normalizedSpeciesSha256(),pin.resolverSourceRevision(),
                pin.mechanicsScope(),pin.robustnessPolicySha256()));
        reject(()->new PublicCompetitiveConsumerContract.ContractPin(1,
                "0".repeat(64),pin.resolverSourceRevision(),
                pin.mechanicsScope(),pin.robustnessPolicySha256()));
        reject(()->new PublicCompetitiveConsumerContract.ContractPin(1,
                pin.normalizedSpeciesSha256(),"future-source",
                pin.mechanicsScope(),pin.robustnessPolicySha256()));
        reject(()->new PublicCompetitiveConsumerContract.ContractPin(1,
                pin.normalizedSpeciesSha256(),pin.resolverSourceRevision(),
                "unknown-mechanics",pin.robustnessPolicySha256()));
        reject(()->new PublicCompetitiveConsumerContract.ContractPin(1,
                pin.normalizedSpeciesSha256(),pin.resolverSourceRevision(),
                pin.mechanicsScope(),"f".repeat(64)));
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,pin,false,false));
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.LIVE_CONTROL,pin,false,false));
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,pin,true,false));
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.OFFLINE_SHADOW_REVIEW,pin,false,true));
        check(Arrays.stream(PublicCompetitiveConsumerContract.Export.class
                .getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));

        String ordinary=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+ability("Illusion")+item("Leftovers")+WIN;
        var f=fixture(12345678,ordinary);
        var proposals=List.of(
                candidate(2,"zoroark",new String[]{"nastyplot"},
                        new String[]{},new String[]{"illusion"},new String[]{}),
                candidate(2,"zoroark",new String[]{"swift"},
                        new String[]{"nastyplot"},new String[]{},new String[]{}),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{},new String[]{"leftovers"}),
                moveCandidate(2,"zoroark","frustration"),
                moveCandidate(2,"zoroark","shadowsneak")
        );
        var r=f.run(proposals);
        safe(r);
        check(r.appearances().size()==1 && r.candidates().size()==5);
        check(r.appearances().get(0).appearanceIndex()==2);
        verdict(r,1,2,SOURCE);
        verdict(r,2,2,CONTRADICTION);
        verdict(r,3,2,UNKNOWN);
        verdict(r,4,2,UNKNOWN);
        verdict(r,5,2,CONTRADICTION);
        check(r.candidates().get(0).sourceSupportedAssertions()==2
                && r.candidates().get(0).observedMatches()==2);
        check(r.candidates().get(1).assertedContradictions()==1
                && r.candidates().get(1).sensitiveContradictionScenarios()>0);
        check(r.candidates().get(1).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.ROBUSTNESS_SENSITIVE));
        check(r.candidates().get(2).unresolvedAssertions()==1);
        check(r.candidates().get(3).unresolvedAssertions()==1);
        check(r.candidates().get(4).assertedContradictions()==1);
        var a=r.appearances().get(0);
        check(a.fields().keySet().equals(EnumSet.allOf(
                PublicBuildEvidenceConsistency.Field.class)));
        check(coverage(a,PublicBuildEvidenceConsistency.Field.MOVE).publiclyObserved()==1);
        check(coverage(a,PublicBuildEvidenceConsistency.Field.MOVE).considered()==4);
        check(coverage(a,PublicBuildEvidenceConsistency.Field.ABILITY).publiclyObserved()==1);
        check(coverage(a,PublicBuildEvidenceConsistency.Field.ITEM).publiclyObserved()==1);
        check(a.uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.UNKNOWN_ITEM_LEGALITY));
        check(a.uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.CONDITIONAL_ROLE_ONLY));
        try{r.appearances().clear();throw new AssertionError("mutable appearance");}
        catch(UnsupportedOperationException expected){checks++;}
        try{r.candidates().clear();throw new AssertionError("mutable candidates");}
        catch(UnsupportedOperationException expected){checks++;}
        try{a.fields().clear();throw new AssertionError("mutable coverage");}
        catch(UnsupportedOperationException expected){checks++;}
        try{r.candidates().get(0).uncertainty().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        try{a.uncertainty().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}

        // A repeated display cannot make an earlier observation a fact about
        // the next opponent appearance. Index is chronological, not team slot.
        String repeated=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+enter("Zoroark")+"|turn|2\n"
                +move("Swift")+WIN;
        var two=fixture(12345679,repeated);
        var out=two.run(List.of(
                candidate(3,"zoroark",new String[]{"swift"},new String[]{"nastyplot"},
                        new String[]{},new String[]{}),
                candidate(2,"zoroark",new String[]{"nastyplot"},new String[]{"swift"},
                        new String[]{},new String[]{})));
        safe(out);
        check(out.appearances().size()==2 && out.candidates().size()==2);
        verdict(out,1,3,SOURCE);
        verdict(out,2,2,SOURCE);
        check(out.appearances().get(0).appearanceIndex()==2
                && out.appearances().get(1).appearanceIndex()==3);
        check(coverage(out.appearances().get(0),
                PublicBuildEvidenceConsistency.Field.MOVE).publiclyObserved()==1);
        check(coverage(out.appearances().get(1),
                PublicBuildEvidenceConsistency.Field.MOVE).publiclyObserved()==1);

        // Unsupported display and Transform do not promote form/learnset
        // guesses into species identity or legal build facts.
        var unknown=fixture(12345680,HEADER+enter("Mewtwo-Mega-Z")+"|turn|1\n"
                +move("Psystrike")+WIN).run(List.of(
                        moveCandidate(2,"mewtwomegaz","psystrike")));
        safe(unknown);verdict(unknown,1,2,ABSTAIN);
        check(unknown.appearances().get(0).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.INCOMPLETE_FORM_OR_SOURCE));
        var transform=fixture(12345681,HEADER+enter("Ditto")+"|turn|1\n"
                +"|-transform|p2a: EnemyNickname|p1a: Friend\n"
                +move("Tackle")+WIN).run(List.of(
                        moveCandidate(2,"ditto","transform")));
        safe(transform);verdict(transform,1,2,ABSTAIN);
        check(transform.appearances().get(0).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.TRANSFORM_OR_FORM_CHANGE));

        String changes=HEADER+enter("Zoroark")+"|turn|1\n"
                +ability("Illusion")+ability("Pressure")
                +item("Leftovers")+item("Choice Scarf")
                +move("Nasty Plot")+WIN;
        var changed=fixture(12345682,changes).run(List.of(
                moveCandidate(2,"zoroark","nastyplot")));
        safe(changed);verdict(changed,1,2,ABSTAIN);
        check(changed.appearances().get(0).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.AMBIGUOUS_ITEM_OR_ABILITY_CHANGE));
        String conflict=HEADER+enter("Zoroark")+"|turn|1\n"
                +move("Shadow Sneak")+WIN;
        var disagreement=fixture(12345683,conflict).run(List.of(
                moveCandidate(2,"zoroark","nastyplot")));
        safe(disagreement);verdict(disagreement,1,2,ABSTAIN);
        check(disagreement.appearances().get(0).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.PUBLIC_SOURCE_DISAGREEMENT));
        var overridden=f.run(List.of(moveCandidate(2,"zoroark","nastyplot")),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                        "zoroark",PublicFormBuildReconciliation.Field.MOVES)));
        safe(overridden);verdict(overridden,1,2,ABSTAIN);
        check(overridden.appearances().get(0).uncertainty().contains(
                PublicCompetitiveConsumerContract.Uncertainty.INCOMPLETE_FORM_OR_SOURCE));

        // Same candidate values and data but different temporary signing keys
        // must produce the same minimized output; no signature leaks.
        var equivalent=fixture(12345684,ordinary).run(proposals);
        check(equivalent.appearances().equals(r.appearances()));
        check(equivalent.candidates().equals(r.candidates()));

        // Wrong version, private event, replay byte tampering, old custody,
        // wrong-side candidates and empty evidence never produce an Export.
        reject(()->f.run(List.of()));
        reject(()->f.run(List.of(moveCandidate(1,"pikachu","swift"))));
        reject(()->f.run(List.of(moveCandidate(99,"zoroark","swift"))));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                null,f.trusted(),NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),null,NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                null,PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trusted(),NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,Set.of(),
                f.signed(),f.trusted(),NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trusted(),NOW,null));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trusted(),NOW+2000,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),
                f.log().replace("Nasty Plot","Shadow Claw"),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trusted(),NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),
                f.log()+"|request|PRIVATE_TEST_TRAINER\n",proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trusted(),NOW,requested));
        reject(()->PublicCompetitiveConsumerContract.prepare(f.source(),f.log(),proposals,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                    "fakeform",PublicFormBuildReconciliation.Field.MOVES)),
                f.signed(),f.trusted(),NOW,requested));
        reject(()->new PublicCompetitiveConsumerContract.Coverage(3,1,1,0,0,0));
        reject(()->new PublicCompetitiveConsumerContract.Appearance(2,false,Map.of(),
                Set.of(ID)));
        reject(()->new PublicCompetitiveConsumerContract.CandidateView(1,2,SOURCE,
                1,1,0,0,9,0,Set.of(ID),false));
        reject(()->new PublicCompetitiveConsumerContract.CandidateView(1,2,SOURCE,
                1,1,0,0,9,0,Set.of(ID,
                    PublicCompetitiveConsumerContract.Uncertainty.SYNTHETIC_SENSITIVITY_NOT_GROUND_TRUTH),
                true));
        System.out.println("Phase 5.19 synthetic consumer safety contracts PASS: "+checks+" checks");
    }
}
