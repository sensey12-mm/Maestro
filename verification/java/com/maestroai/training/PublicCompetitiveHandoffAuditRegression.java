package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Phase 5.20: integrated synthetic-only end-to-end acceptance regressions. */
final class PublicCompetitiveHandoffAuditRegression {
    private PublicCompetitiveHandoffAuditRegression(){}
    private static int checks;
    private static void check(boolean value) {
        if(!value)throw new AssertionError("Phase 5.20 offline handoff acceptance");
        checks++;
    }
    private static void reject(Runnable r) {
        try {r.run();}
        catch(IllegalArgumentException expected) {
            check(!expected.getMessage().contains("SYNTHETIC_PRIVATE_TRAINER")
                    && !expected.getMessage().contains("SecretNickname"));
            return;
        }
        throw new AssertionError("Phase 5.20 accepted incompatible or untrusted evidence");
    }

    private static final long NOW=1_700_000_100L;
    private static final String START="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SYNTHETIC_PRIVATE_TRAINER\n|start\n"
            +"|switch|p1a: Friend|Pikachu, L100|100/100\n";
    private static final String WIN="|win|SYNTHETIC_PRIVATE_TRAINER\n";
    private static final PublicCompetitiveHandoffAudit.Gate OK=
            PublicCompetitiveHandoffAudit.Gate.OFFLINE_RECOMPUTED,
            BLOCKED=PublicCompetitiveHandoffAudit.Gate.CROSS_PHASE_BLOCKED,
            UNATTESTED=PublicCompetitiveHandoffAudit.Gate.NOT_ATTESTED;

    private static String enter(String pokemon) {
        return "|switch|p2a: SecretNickname|"+pokemon+", L100|100/100\n";
    }
    private static String move(String value) {
        return "|move|p2a: SecretNickname|"+value+"|p1a: Friend\n";
    }
    private static String ability(String value) {
        return "|-ability|p2a: SecretNickname|"+value+"\n";
    }
    private static String item(String value) {
        return "|-item|p2a: SecretNickname|"+value+"\n";
    }
    private static PublicReplayCorpus.Source source(int serial) {
        return new PublicReplayCorpus.Source("gen9ou-"+serial,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static PublicBuildCandidateFilter.Candidate choice(int entry,String form,
            String[] moves,String[] excluded,String[] abilities,String[] items) {
        return new PublicBuildCandidateFilter.Candidate(entry,form,
                Set.of(moves),Set.of(excluded),Set.of(abilities),Set.of(items));
    }
    private static PublicBuildCandidateFilter.Candidate sourceMove(int appearance,
            String form,String move) {
        return choice(appearance,form,new String[]{move},
                new String[]{},new String[]{},new String[]{});
    }
    private record Fixture(PublicReplayCorpus.Source selected,String selectedLog,
                           List<PublicHeldOutEvaluation.Replay> replays,
                           List<PublicBuildCandidateFilter.Candidate> candidates,
                           PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy policy) {
        PublicCompetitiveHandoffAudit.Report run() {
            return run(replays,candidates,signed,policy,NOW,
                    PublicCompetitiveHandoffAudit.Request.offlineReviewed(),
                    registration(replays));
        }
        PublicCompetitiveHandoffAudit.Report run(List<PublicHeldOutEvaluation.Replay> provided,
                  List<PublicBuildCandidateFilter.Candidate> guesses,
                  PublicSignedCustody.SignedManifest manifest,
                  PublicSignedCustody.TrustedPolicy trust,long clock,
                  PublicCompetitiveHandoffAudit.Request request,
                  PublicCompetitiveRobustnessAudit.Registration registration) {
            return PublicCompetitiveHandoffAudit.evaluate(selected,selectedLog,
                    provided,guesses,PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    Set.of(),manifest,trust,clock,request,registration);
        }
    }
    private static PublicCompetitiveRobustnessAudit.Registration registration(
            List<PublicHeldOutEvaluation.Replay> inputs) {
        var split=PublicHeldOutEvaluation.evaluate(inputs,
                CompetitiveFormatNormalizer.Target.pinnedSource());
        return new PublicCompetitiveRobustnessAudit.Registration(1,
                PublicCompetitiveRobustnessAudit.frozenProtocolSha256(),
                split.splitSha256());
    }
    private static Fixture fixture(int seed) {
        try {
            String a=START+enter("Zoroark")+"|turn|1\n"+ability("Illusion")
                    +item("Leftovers")+move("Nasty Plot")+WIN;
            String b=START+enter("Ditto")+"|turn|1\n"+move("Transform")
                    +"|turn|2\n"+move("Transform")+WIN;
            String c=START+enter("Mewtwo-Mega-Y")+"|turn|1\n"
                    +move("Psystrike")+"|turn|2\n"+move("Swift")+WIN;
            var training=source(seed),selected=source(seed+1),other=source(seed+2);
            var replays=List.of(
                    new PublicHeldOutEvaluation.Replay(training,a,
                            PublicHeldOutEvaluation.Partition.TRAIN),
                    new PublicHeldOutEvaluation.Replay(selected,b,
                            PublicHeldOutEvaluation.Partition.HELD_OUT),
                    new PublicHeldOutEvaluation.Replay(other,c,
                            PublicHeldOutEvaluation.Partition.HELD_OUT));
            var decisions=new ArrayList<PublicReplayCustody.Decision>();
            for(var replay:replays){
                var e=PublicReplayCorpus.ingest(replay.source(),replay.spectatorLog());
                decisions.add(new PublicReplayCustody.Decision(e.replayId(),e.format(),
                        e.inputSha256(),PublicReplayCustody.Status.APPROVED,
                        NOW-100,NOW+1000,"c".repeat(64)));
            }
            var ledger=new PublicReplayCustody.Ledger(1,1,decisions);
            var kp=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signing=Signature.getInstance("Ed25519");
            signing.initSign(kp.getPrivate());
            signing.update(PublicSignedCustody.canonicalBytes(1,"phase520-test",
                    NOW-10,"0".repeat(64),ledger));
            var signature=Base64.getEncoder().encodeToString(signing.sign());
            var manifest=new PublicSignedCustody.SignedManifest(1,"phase520-test",
                    NOW-10,"0".repeat(64),ledger,signature);
            var publicKey=new PublicSignedCustody.TrustedKey("phase520-test",
                    Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()),
                    NOW-1000,0);
            var trusted=new PublicSignedCustody.TrustedPolicy(1,1,1,NOW-1000,
                    "",List.of(publicKey));
            var candidates=List.of(
                sourceMove(2,"ditto","transform"),
                sourceMove(2,"ditto","tackle"),
                choice(2,"ditto",new String[]{},new String[]{},
                        new String[]{"imposter"},new String[]{"choicescarf"}));
            return new Fixture(selected,b,replays,candidates,manifest,trusted);
        }catch(GeneralSecurityException e){throw new AssertionError(e);}
    }

    static void run() {
        check(PublicCompetitiveHandoffAudit.VERSION==1);
        check(PublicCompetitiveHandoffAudit.SCHEMA.equals("phase5.20.offline-handoff.v1"));
        check(PublicCompetitiveHandoffAudit.PHASE4_AUDITED_COMMIT.matches("[0-9a-f]{40}"));
        check(PublicCompetitiveHandoffAudit.PHASE6_AUDITED_COMMIT.matches("[0-9a-f]{40}"));
        check(PublicCompetitiveHandoffAudit.PHASE7_AUDITED_COMMIT.matches("[0-9a-f]{40}"));
        var policy=PublicCompetitiveHandoffAudit.Request.offlineReviewed();
        check(policy.schemaVersion()==1 && !policy.runtimeOrTacticalAccessRequested()
                && !policy.ownerMergeApprovalClaimed());
        check(policy.crossPhase().equals(
                PublicCompetitiveHandoffAudit.CrossPhaseSnapshot.audited()));
        reject(()->new PublicCompetitiveHandoffAudit.CrossPhaseSnapshot(
                "0".repeat(40),PublicCompetitiveHandoffAudit.PHASE6_AUDITED_COMMIT,
                PublicCompetitiveHandoffAudit.PHASE7_AUDITED_COMMIT));
        reject(()->new PublicCompetitiveHandoffAudit.CrossPhaseSnapshot(
                PublicCompetitiveHandoffAudit.PHASE4_AUDITED_COMMIT,
                "0".repeat(40),PublicCompetitiveHandoffAudit.PHASE7_AUDITED_COMMIT));
        reject(()->new PublicCompetitiveHandoffAudit.CrossPhaseSnapshot(
                PublicCompetitiveHandoffAudit.PHASE4_AUDITED_COMMIT,
                PublicCompetitiveHandoffAudit.PHASE6_AUDITED_COMMIT,"0".repeat(40)));
        reject(()->new PublicCompetitiveHandoffAudit.Request(2,
                policy.consumerPin(),policy.robustnessPin(),
                policy.crossPhase(),false,false));
        reject(()->new PublicCompetitiveHandoffAudit.Request(1,
                policy.consumerPin(),policy.robustnessPin(),
                policy.crossPhase(),true,false));
        reject(()->new PublicCompetitiveHandoffAudit.Request(1,
                policy.consumerPin(),policy.robustnessPin(),
                policy.crossPhase(),false,true));
        reject(()->new PublicCompetitiveHandoffAudit.Request(1,
                policy.consumerPin(),null,policy.crossPhase(),false,false));
        check(Arrays.stream(PublicCompetitiveHandoffAudit.Report.class
                .getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));

        var fixture=fixture(12345678);
        var result=fixture.run();
        check(result.schema().equals(PublicCompetitiveHandoffAudit.SCHEMA));
        check(result.sourceScope().equals(CobblemonMechanics.SOURCE));
        check(result.robustnessPolicySha256().equals(
                PublicCompetitiveRobustnessAudit.frozenProtocolSha256()));
        check(result.offlineChainRecomputed());
        check(result.independentPhase5DevelopmentReadyForReview());
        check(!result.handoffApprovedForIntegration() && !result.mainMergeAuthorized()
                && !result.tacticalOrLivePromotionAuthorized()
                && !result.currentBattleBound());
        check(!result.installedCompatibilityVerified()
                && !result.realReplayCompetitiveSkillProven()
                && !result.approvedPhase6SearchInput() && !result.phase7Integrated());
        check(!result.toString().contains("SYNTHETIC_PRIVATE_TRAINER")
                && !result.toString().contains("SecretNickname")
                && !result.toString().contains("gen9ou-")
                && !result.toString().contains("choicescarf")
                && !result.toString().contains("transform"));
        check(result.stages().keySet().equals(
                EnumSet.allOf(PublicCompetitiveHandoffAudit.Stage.class)));
        check(result.blockers().equals(
                EnumSet.allOf(PublicCompetitiveHandoffAudit.Blocker.class)));
        check(result.blockers().contains(
                PublicCompetitiveHandoffAudit.Blocker
                        .PHASE6_REQUIRES_RESPONSES_AND_PROBABILITIES_NOT_PROVIDED));
        check(result.blockers().contains(
                PublicCompetitiveHandoffAudit.Blocker
                        .PHASE7_NO_INTEGRATED_PHASE4_5_6_CHAIN));
        check(result.blockers().contains(
                PublicCompetitiveHandoffAudit.Blocker
                        .NO_INDEPENDENT_REPLAY_RIGHTS_OR_REAL_HOLDOUT));
        for(var stage:PublicCompetitiveHandoffAudit.Stage.values()) {
            var expected=switch(stage){
                case SIGNED_REPLAY_PROVENANCE,NORMALIZED_OBSERVATIONS,
                     SIGNED_WHOLE_REPLAY_SPLIT,HELDOUT_BASELINES,
                     STRATIFIED_COVERAGE,FROZEN_DISTRIBUTION_SHIFT,
                     BUILD_CANDIDATES_AND_ROBUSTNESS,OFFLINE_CONSUMER_CONTRACT -> OK;
                case DURABLE_TRUST_HANDOFF,INDEPENDENT_REAL_REPLAY -> UNATTESTED;
                default -> BLOCKED;
            };
            check(result.stages().get(stage)==expected);
        }
        check(result.coverage().trainingReplays()==1);
        check(result.coverage().heldOutReplays()==2);
        check(result.coverage().eligibleAppearances()==1);
        check(result.coverage().candidateAssertions()==3);
        check(result.coverage().robustnessScenarios()==27);
        check(result.coverage().insufficientStrata()>0);
        check(result.coverage().shiftAbstentions()>0);
        try {result.blockers().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        try {result.stages().clear();throw new AssertionError("mutable");}
        catch(UnsupportedOperationException expected){checks++;}
        check(result.stages().values().stream().filter(g->g==OK).count()==8);
        check(result.stages().values().stream().filter(g->g==BLOCKED).count()==4);
        check(result.stages().values().stream().filter(g->g==UNATTESTED).count()==2);

        // Row order (never selecting based on match result) can be shuffled.
        var reordered=List.of(fixture.replays().get(2),fixture.replays().get(0),
                fixture.replays().get(1));
        var shuffled=fixture.run(reordered,fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,registration(reordered));
        check(shuffled.coverage().equals(result.coverage()));
        check(shuffled.stages().equals(result.stages()));
        check(shuffled.blockers().equals(result.blockers()));

        // Appearance-only adversarial proposed candidate changes do not
        // promote to a legal or calibrated tactical build.
        var badGuess=List.of(sourceMove(2,"ditto","shadowsneak"));
        var narrow=fixture.run(fixture.replays(),badGuess,fixture.signed(),
                fixture.policy(),NOW,policy,registration(fixture.replays()));
        check(narrow.offlineChainRecomputed() && !narrow.handoffApprovedForIntegration());
        check(narrow.coverage().candidateAssertions()==1
                && narrow.coverage().robustnessScenarios()==9);

        // Reject a source absent from the split, cloned identifiers,
        // selected replay reclassified as TRAIN, tampered original log, and
        // public-sequence clones even under different source IDs.
        var without=List.of(fixture.replays().get(0),fixture.replays().get(2));
        reject(()->fixture.run(without,fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,registration(without)));
        var mispartition=List.of(fixture.replays().get(0),
                new PublicHeldOutEvaluation.Replay(fixture.selected(),
                    fixture.selectedLog(),PublicHeldOutEvaluation.Partition.TRAIN),
                fixture.replays().get(2));
        reject(()->fixture.run(mispartition,fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,registration(mispartition)));
        var copied=List.of(fixture.replays().get(0),fixture.replays().get(1),
                new PublicHeldOutEvaluation.Replay(source(12345689),
                        fixture.selectedLog(),PublicHeldOutEvaluation.Partition.HELD_OUT));
        reject(()->fixture.run(copied,fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,
                new PublicCompetitiveRobustnessAudit.Registration(1,
                        PublicCompetitiveRobustnessAudit.frozenProtocolSha256(),
                        "a".repeat(64))));
        reject(()->fixture.run(fixture.replays(),List.of(sourceMove(1,"pikachu","swift")),
                fixture.signed(),fixture.policy(),NOW,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),List.of(sourceMove(99,"ditto","transform")),
                fixture.signed(),fixture.policy(),NOW,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),List.of(),fixture.signed(),
                fixture.policy(),NOW,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),null,
                fixture.policy(),NOW,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),fixture.signed(),
                null,NOW,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW+2000,policy,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,null,registration(fixture.replays())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,
                new PublicCompetitiveRobustnessAudit.Registration(1,
                        "f".repeat(64),registration(fixture.replays()).testSplitSha256())));
        reject(()->fixture.run(fixture.replays(),fixture.candidates(),fixture.signed(),
                fixture.policy(),NOW,policy,
                new PublicCompetitiveRobustnessAudit.Registration(1,
                        PublicCompetitiveRobustnessAudit.frozenProtocolSha256(),
                        "f".repeat(64))));
        reject(()->PublicCompetitiveHandoffAudit.evaluate(fixture.selected(),
                fixture.selectedLog()+"|request|SYNTHETIC_PRIVATE_TRAINER\n",
                fixture.replays(),fixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),fixture.signed(),fixture.policy(),NOW,
                policy,registration(fixture.replays())));
        reject(()->PublicCompetitiveHandoffAudit.evaluate(fixture.selected(),
                fixture.selectedLog().replace("Transform","Tackle"),
                fixture.replays(),fixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),fixture.signed(),fixture.policy(),NOW,
                policy,registration(fixture.replays())));
        reject(()->PublicCompetitiveHandoffAudit.evaluate(fixture.selected(),
                fixture.selectedLog(),fixture.replays(),fixture.candidates(),
                null,PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(),fixture.signed(),fixture.policy(),NOW,
                policy,registration(fixture.replays())));
        reject(()->PublicCompetitiveHandoffAudit.evaluate(fixture.selected(),
                fixture.selectedLog(),fixture.replays(),fixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,
                Set.of(),fixture.signed(),fixture.policy(),NOW,
                policy,registration(fixture.replays())));
        reject(()->PublicCompetitiveHandoffAudit.evaluate(fixture.selected(),
                fixture.selectedLog(),fixture.replays(),fixture.candidates(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                    "unsupportedform",PublicFormBuildReconciliation.Field.MOVES)),
                fixture.signed(),fixture.policy(),NOW,policy,
                registration(fixture.replays())));
        System.out.println("Phase 5.20 synthetic end-to-end handoff PASS: "+checks+" checks");
    }
}
