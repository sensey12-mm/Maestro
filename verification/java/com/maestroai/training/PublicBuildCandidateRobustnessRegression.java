package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.18: ALL replay inputs synthetic, with synthetic ephemeral signing keys. */
final class PublicBuildCandidateRobustnessRegression {
    private PublicBuildCandidateRobustnessRegression() {}
    private static int checks;
    private static void check(boolean valid) {
        if(!valid)throw new AssertionError("Phase 5.18 candidate robustness");
        checks++;
    }
    private static void reject(Runnable action) {
        try{action.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains("PRIVATE_SYNTHETIC_TRAINER")
                    && !expected.getMessage().contains("SecretNick"));
            return;
        }
        throw new AssertionError("Untrusted candidate robustness was accepted");
    }
    private static final long TIME=1_700_000_100L;
    private static final String PREFIX="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_SYNTHETIC_TRAINER\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n";
    private static final String WIN="|win|PRIVATE_SYNTHETIC_TRAINER\n";
    private static String entrance(String mon) {
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
    private static PublicBuildCandidateFilter.Candidate candidate(int index,String form,
             String[] moves,String[] excluded,String[] abilities,String[] items) {
        return new PublicBuildCandidateFilter.Candidate(index,form,
                Set.of(moves),Set.of(excluded),Set.of(abilities),Set.of(items));
    }
    private static PublicBuildCandidateFilter.Candidate required(int index,String form,String move) {
        return candidate(index,form,new String[]{move},new String[]{},
                new String[]{},new String[]{});
    }
    private record Fixture(PublicReplayCorpus.Source source,String log,
            PublicSignedCustody.SignedManifest signed,
            PublicSignedCustody.TrustedPolicy trust) {
        PublicBuildCandidateRobustness.Report audit(List<PublicBuildCandidateFilter.Candidate> proposed) {
            return audit(proposed,Set.of());
        }
        PublicBuildCandidateRobustness.Report audit(
                List<PublicBuildCandidateFilter.Candidate> proposed,
                Set<PublicFormBuildReconciliation.OverrideNotice> overrides) {
            return PublicBuildCandidateRobustness.evaluate(source,log,proposed,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    overrides,signed,trust,TIME,
                    PublicBuildCandidateRobustness.PolicyPin.pinned());
        }
    }
    private static Fixture fixture(int serial,String log) {
        try {
            var source=new PublicReplayCorpus.Source("gen9ou-"+serial,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,log);
            var decision=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    TIME-50,TIME+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signature=Signature.getInstance("Ed25519");
            signature.initSign(keys.getPrivate());
            signature.update(PublicSignedCustody.canonicalBytes(
                    1,"synthetic-signer",TIME-10,"0".repeat(64),ledger));
            var signed=new PublicSignedCustody.SignedManifest(1,"synthetic-signer",
                    TIME-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(signature.sign()));
            var key=new PublicSignedCustody.TrustedKey("synthetic-signer",
                    Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),
                    TIME-100,0);
            return new Fixture(source,log,signed,
                    new PublicSignedCustody.TrustedPolicy(1,1,1,TIME-100,
                            "",List.of(key)));
        }catch(GeneralSecurityException error){throw new AssertionError(error);}
    }
    private static PublicBuildCandidateRobustness.Scenario scenario(
            PublicBuildCandidateRobustness.Report report,int ordinal,
            PublicBuildCandidateRobustness.Perturbation variant) {
        return report.scenarios().stream().filter(s->s.candidateOrdinal()==ordinal
                && s.perturbation()==variant).findFirst().orElseThrow();
    }
    private static PublicBuildCandidateRobustness.Finding SOURCE=
            PublicBuildCandidateRobustness.Finding.SOURCE_CONDITIONAL_ONLY,
            CONTRADICTION=PublicBuildCandidateRobustness.Finding.ASSERTION_CONTRADICTION_ONLY,
            UNKNOWN=PublicBuildCandidateRobustness.Finding.UNRESOLVED,
            ABSTAIN=PublicBuildCandidateRobustness.Finding.ABSTAINED;
    private static PublicBuildCandidateRobustness.Perturbation BASE=
            PublicBuildCandidateRobustness.Perturbation.REFERENCE,
            NO_MOVE=PublicBuildCandidateRobustness.Perturbation.OMIT_PUBLIC_MOVE,
            NO_FORM=PublicBuildCandidateRobustness.Perturbation.BLIND_DISPLAY_FORM,
            NO_ABILITY=PublicBuildCandidateRobustness.Perturbation.OMIT_PUBLIC_ABILITY,
            NO_ITEM=PublicBuildCandidateRobustness.Perturbation.OMIT_PUBLIC_ITEM,
            ILLUSION=PublicBuildCandidateRobustness.Perturbation.POSSIBLE_ILLUSION,
            TRANSFORM=PublicBuildCandidateRobustness.Perturbation.POSSIBLE_TRANSFORM,
            CHANGE=PublicBuildCandidateRobustness.Perturbation.ITEM_OR_ABILITY_CHANGED,
            SOURCE_CHANGED=PublicBuildCandidateRobustness.Perturbation.INCOMPATIBLE_SOURCE;

    static void run() {
        check(PublicBuildCandidateRobustness.VERSION==1);
        check(PublicBuildCandidateRobustness.PolicyPin.pinned().version()==1);
        check(PublicBuildCandidateRobustness.frozenPolicySha256().matches("[0-9a-f]{64}"));
        check(PublicBuildCandidateRobustness.frozenPolicyDefinition().contains(
                PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(PublicBuildCandidateRobustness.frozenPolicyDefinition()
                .contains("never_eliminate_opponent"));
        check(PublicBuildCandidateRobustness.Perturbation.values().length==9);
        reject(()->new PublicBuildCandidateRobustness.PolicyPin(2,
                PublicBuildCandidateRobustness.frozenPolicySha256()));
        reject(()->new PublicBuildCandidateRobustness.PolicyPin(1,"a".repeat(64)));
        reject(()->new PublicBuildCandidateRobustness.PolicyPin(1,"garbage"));

        String ordinary=PREFIX+entrance("Zoroark")+"|turn|1\n"
                +ability("Illusion")+item("Leftovers")+move("Nasty Plot")+WIN;
        var verified=fixture(12345678,ordinary);
        var candidates=List.of(
                required(2,"zoroark","nastyplot"),
                candidate(2,"zoroark",new String[]{"swift"},
                        new String[]{"nastyplot"},new String[]{},new String[]{}),
                required(2,"zoroark","shadowsneak"),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{"illusion"},new String[]{}),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{},new String[]{"leftovers"}),
                required(2,"mewtwo","nastyplot"),
                required(2,"zoroark","frustration"));
        var r=verified.audit(candidates);
        check(r.scenarios().size()==63);
        check(r.policySha256().equals(PublicBuildCandidateRobustness.frozenPolicySha256()));
        check(r.sourceScope().contains(PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(!r.opponentEliminationAuthorized() && !r.independentGroundTruthAvailable()
                && !r.predictionProbabilitiesProduced() && !r.livePromotionAuthorized()
                && !r.shadowTacticalReady());
        check(!r.toString().contains("PRIVATE_SYNTHETIC_TRAINER")
                && !r.toString().contains("SecretNick")
                && !r.toString().contains("nastyplot")
                && !r.toString().contains("gen9ou-"));
        check(Arrays.stream(PublicBuildCandidateRobustness.Report.class
                .getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));
        try{r.scenarios().clear();throw new AssertionError("mutable scenarios");}
        catch(UnsupportedOperationException expected){checks++;}

        check(scenario(r,1,BASE).finding()==SOURCE);
        check(!scenario(r,1,BASE).evidenceWeakened());
        check(scenario(r,1,NO_MOVE).finding()==SOURCE);
        check(scenario(r,1,NO_MOVE).evidenceWeakened());
        check(scenario(r,1,NO_FORM).finding()==ABSTAIN);
        check(scenario(r,1,NO_FORM).blockers().contains(
                PublicBuildCandidateRobustness.Blocker.UNKNOWN_DISPLAY_FORM));
        check(scenario(r,1,ILLUSION).finding()==ABSTAIN);
        check(scenario(r,1,TRANSFORM).finding()==ABSTAIN);
        check(scenario(r,1,SOURCE_CHANGED).finding()==ABSTAIN);
        check(scenario(r,1,CHANGE).finding()==SOURCE); // no item or ability assertion
        check(!scenario(r,1,CHANGE).evidenceWeakened());

        // Remove the ONLY witnessed excluded move: hard assertion contradiction
        // no longer exists, and the opponent must never be discarded.
        check(scenario(r,2,BASE).finding()==CONTRADICTION);
        check(scenario(r,2,NO_MOVE).finding()==UNKNOWN);
        check(scenario(r,2,NO_MOVE).brittleContradiction());
        check(scenario(r,2,NO_MOVE).blockers().contains(
                PublicBuildCandidateRobustness.Blocker.MISSING_WITNESSED_MOVE));
        check(scenario(r,2,NO_MOVE).evidenceWeakened());
        check(scenario(r,2,ILLUSION).brittleContradiction());
        check(!scenario(r,2,BASE).brittleContradiction());
        check(scenario(r,2,NO_FORM).finding()==ABSTAIN);
        // Source-absent candidate remains a narrow source assertion under
        // missing witnessed move, never evidence of an illegal real Pokémon.
        check(scenario(r,3,BASE).finding()==CONTRADICTION);
        check(scenario(r,3,NO_MOVE).finding()==CONTRADICTION);
        check(!scenario(r,3,NO_MOVE).brittleContradiction());
        check(scenario(r,3,SOURCE_CHANGED).finding()==ABSTAIN);
        check(scenario(r,3,SOURCE_CHANGED).brittleContradiction());

        // A witnessed ability may be masked without inventing a new ability.
        check(scenario(r,4,BASE).finding()==SOURCE);
        check(scenario(r,4,NO_ABILITY).finding()==SOURCE);
        check(scenario(r,4,NO_ABILITY).evidenceWeakened());
        check(scenario(r,4,CHANGE).finding()==ABSTAIN);
        check(scenario(r,4,NO_ITEM).finding()==SOURCE);
        check(!scenario(r,4,NO_ITEM).evidenceWeakened());
        check(scenario(r,5,BASE).finding()==UNKNOWN);
        check(scenario(r,5,NO_ITEM).finding()==UNKNOWN);
        check(scenario(r,5,NO_ITEM).evidenceWeakened());
        check(scenario(r,5,CHANGE).finding()==ABSTAIN);
        check(scenario(r,6,BASE).finding()==CONTRADICTION);
        check(scenario(r,6,NO_FORM).brittleContradiction());
        check(scenario(r,7,BASE).finding()==UNKNOWN);

        var ready=r.readiness();
        check(ready.mandatoryScenarioKinds()==9);
        check(ready.expectedScenarioCount()==63
                && ready.evaluatedScenarioCount()==63);
        check(ready.possibleFalseEliminationRisks()>0
                && ready.assertionContradictions()>0 && ready.abstentions()>0);
        check(ready.signedSourceChainRevalidated() && ready.identityAndRealBuildUnproven());
        check(!ready.mechanicsAndInstalledRegistryCompatible()
                && !ready.realWorldFalseEliminationsMeasured()
                && !ready.liveOrShadowPromotionAuthorized());
        check(r.scenarios().stream().allMatch(s->!s.opponentEliminationAllowed()));
        check(r.scenarios().stream().filter(s->s.brittleContradiction()).allMatch(s->
                s.finding()!=CONTRADICTION));
        check(r.scenarios().stream().filter(s->s.perturbation()==BASE)
                .noneMatch(PublicBuildCandidateRobustness.Scenario::evidenceWeakened));
        check(r.scenarios().stream().map(s->s.candidateOrdinal()
                +"|"+s.perturbation()).distinct().count()==63);

        // Permutation: no one candidate may borrow another's evidence;
        // source verdict sensitivity is invariant under candidate ordering.
        var reordered=new ArrayList<>(candidates);
        Collections.reverse(reordered);
        var reversed=verified.audit(reordered);
        for(int i=0;i<candidates.size();i++) {
            for(var shift:PublicBuildCandidateRobustness.Perturbation.values()) {
                var a=scenario(r,i+1,shift);
                var b=scenario(reversed,candidates.size()-i,shift);
                check(a.finding()==b.finding() && a.brittleContradiction()==b.brittleContradiction()
                        && a.evidenceWeakened()==b.evidenceWeakened());
            }
        }
        check(reversed.readiness().possibleFalseEliminationRisks()
                ==r.readiness().possibleFalseEliminationRisks());

        // A new public appearance with the same nickname has no entitlement
        // to moves observed in the older appearance.
        String replay=PREFIX+entrance("Zoroark")+"|turn|1\n"
                +move("Nasty Plot")+entrance("Zoroark")+"|turn|2\n"
                +move("Swift")+WIN;
        var reentry=fixture(12345679,replay);
        var split=reentry.audit(List.of(
                candidate(3,"zoroark",new String[]{"swift"},
                        new String[]{"nastyplot"},new String[]{},new String[]{}),
                candidate(2,"zoroark",new String[]{"nastyplot"},
                        new String[]{"swift"},new String[]{},new String[]{})));
        check(split.scenarios().size()==18);
        check(scenario(split,1,BASE).finding()==SOURCE);
        check(scenario(split,2,BASE).finding()==SOURCE);
        check(split.scenarios().stream().allMatch(s->!s.opponentEliminationAllowed()));

        // Genuine source disagreement, Transform and uncertain forms stay
        // ABSTAINED even if counterfactually hiding public evidence.
        var clash=fixture(12345680,PREFIX+entrance("Zoroark")+"|turn|1\n"
                +move("Shadow Sneak")+WIN);
        var clashResult=clash.audit(List.of(required(2,"zoroark","nastyplot")));
        check(scenario(clashResult,1,BASE).finding()==ABSTAIN);
        check(scenario(clashResult,1,NO_MOVE).finding()==ABSTAIN);
        check(scenario(clashResult,1,SOURCE_CHANGED).finding()==ABSTAIN);

        var transform=fixture(12345681,PREFIX+entrance("Ditto")+"|turn|1\n"
                +"|-transform|p2a: SecretNick|p1a: Ally\n"
                +move("Tackle")+WIN);
        var transformed=transform.audit(List.of(required(2,"ditto","transform")));
        check(scenario(transformed,1,BASE).finding()==ABSTAIN);
        check(scenario(transformed,1,ILLUSION).finding()==ABSTAIN);
        check(scenario(transformed,1,SOURCE_CHANGED).finding()==ABSTAIN);

        var unknownForm=fixture(12345682,PREFIX+entrance("Mewtwo-Mega-Z")
                +"|turn|1\n"+move("Psystrike")+WIN);
        check(scenario(unknownForm.audit(List.of(required(2,"mewtwomegaz","psystrike"))),
                1,BASE).finding()==ABSTAIN);

        var changed=fixture(12345683,PREFIX+entrance("Zoroark")+"|turn|1\n"
                +ability("Illusion")+ability("Pressure")
                +item("Leftovers")+item("Choice Scarf")
                +move("Nasty Plot")+WIN);
        var changedReport=changed.audit(List.of(
                required(2,"zoroark","nastyplot"),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{"illusion"},new String[]{})
        ));
        check(scenario(changedReport,1,BASE).finding()==ABSTAIN);
        check(scenario(changedReport,2,BASE).finding()==ABSTAIN);
        check(scenario(changedReport,1,CHANGE).finding()==ABSTAIN);
        check(changedReport.scenarios().stream().noneMatch(
                PublicBuildCandidateRobustness.Scenario::opponentEliminationAllowed));

        var overridden=verified.audit(List.of(required(2,"zoroark","nastyplot")),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                        "zoroark",PublicFormBuildReconciliation.Field.MOVES)));
        check(scenario(overridden,1,BASE).finding()==ABSTAIN);
        check(scenario(overridden,1,SOURCE_CHANGED).finding()==ABSTAIN);

        // No candidates still requires signed replay admission; missing
        // signature cannot bypass custody through an empty report.
        var empty=verified.audit(List.of());
        check(empty.scenarios().isEmpty());
        check(empty.readiness().expectedScenarioCount()==0);
        check(empty.readiness().signedSourceChainRevalidated());
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),List.of(),PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                null,verified.trust(),TIME,PublicBuildCandidateRobustness.PolicyPin.pinned()));

        // Tampering with original spectator evidence, verification time, pin
        // or private packets never produces a fake robustness verdict.
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log().replace("Nasty Plot","Shadow Claw"),candidates,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),verified.trust(),TIME,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log()+"|request|PRIVATE_SYNTHETIC_TRAINER\n",candidates,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),verified.trust(),TIME,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),candidates,PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),verified.trust(),TIME+2000,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),candidates,null,
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),verified.trust(),TIME,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),candidates,PublicSpeciesBuildLegality.SourcePin.pinned(),
                null,Set.of(),verified.signed(),verified.trust(),TIME,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),candidates,PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),verified.trust(),TIME,null));
        reject(()->PublicBuildCandidateRobustness.evaluate(verified.source(),
                verified.log(),candidates,PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                verified.signed(),null,TIME,
                PublicBuildCandidateRobustness.PolicyPin.pinned()));
        reject(()->verified.audit(List.of(required(1,"pikachu","swift"))));
        reject(()->verified.audit(List.of(required(200,"zoroark","nastyplot"))));
        var tooMany=new ArrayList<PublicBuildCandidateFilter.Candidate>();
        for(int i=0;i<41;i++)tooMany.add(required(2,"zoroark","nastyplot"));
        reject(()->verified.audit(tooMany));

        reject(()->new PublicBuildCandidateRobustness.Scenario(1,2,BASE,SOURCE,
                Set.of(PublicBuildCandidateRobustness.Blocker.REFERENCE_ONLY),
                true,false,false));
        reject(()->new PublicBuildCandidateRobustness.Scenario(1,2,BASE,SOURCE,
                Set.of(PublicBuildCandidateRobustness.Blocker.REFERENCE_ONLY),
                false,true,false));
        reject(()->new PublicBuildCandidateRobustness.Scenario(1,2,BASE,SOURCE,
                Set.of(),false,false,false));
        reject(()->new PublicBuildCandidateRobustness.Scenario(1,2,BASE,SOURCE,
                Set.of(PublicBuildCandidateRobustness.Blocker.REFERENCE_ONLY),
                false,false,true));
        reject(()->new PublicBuildCandidateRobustness.Readiness(8,9,9,0,0,0,
                true,true,false,false,false));
        reject(()->new PublicBuildCandidateRobustness.Readiness(9,9,8,0,0,0,
                true,true,false,false,false));
        reject(()->new PublicBuildCandidateRobustness.Readiness(9,9,9,0,0,0,
                false,true,false,false,false));

        System.out.println("Phase 5.18 synthetic conservative robustness PASS: "+checks+" checks");
    }
}
