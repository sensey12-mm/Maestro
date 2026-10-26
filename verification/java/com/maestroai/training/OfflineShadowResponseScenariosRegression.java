package com.maestroai.training;

import java.security.*;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Phase 6.3 deterministic offline response and provenance adversarial suite. */
final class OfflineShadowResponseScenariosRegression {
    private OfflineShadowResponseScenariosRegression() {}
    private static int checks;
    private static void check(boolean value, String reason) {
        if (!value) throw new AssertionError("Phase 6.3: "+reason);
        checks++;
    }
    private static void reject(Runnable action, String reason) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Phase 6.3 accepted "+reason);
    }
    private static final String OWN="update\n|switch|p1a: Own|Genesect, L50|100/100";
    private static final String FOE="update\n|switch|p2a: Opp|synthetic, L50|100/100";
    private static List<String> frames(String... replies) {
        var result=new ArrayList<>(List.of(OWN, FOE, "update\n|turn|1"));
        for (String reply:replies)
            result.add("update\n|move|p2a: Opp|"+reply+"|p1a: Own");
        return List.copyOf(result);
    }
    private static BattleSnapshot snapshot(int turn, List<String> ownMoves,
                                            List<String> replies, List<String> switches) {
        var self=new BattleSnapshot.Combatant("genesect",1,List.of("bug","steel"),
                Map.of(),List.of(),null,null,false);
        var foe=new BattleSnapshot.Combatant("synthetic",1,List.of(),
                Map.of(),replies,null,null,false);
        return new BattleSnapshot(turn,self,foe,List.of(),List.of(),ownMoves,switches,
                Map.of(),false,null);
    }
    private static PublicStateSearchAdapter.PublicMechanics mechanics(boolean trapped) {
        var hp=Known.of(new PublicStateSearchAdapter.Interval(1,1),"synthetic public HP");
        var status=Known.of("none","synthetic status");
        var hazards=new Hazards(Map.of(),true,"synthetic field");
        var context=new Context(hp,hp,status,status,hazards,hazards,
                Map.of(),Map.of(),Set.of());
        return new PublicStateSearchAdapter.PublicMechanics(Map.of(),Map.of(),Set.of(),
                false,false,trapped,0,new Evidence(context,Map.of(),Map.of()));
    }
    private static OfflineShadowResponseScenarios.Input input(
            List<String> frames, BattleSnapshot state,
            PublicStateSearchAdapter.PublicMechanics mechanics,
            OfflineShadowResponseScenarios.HistoricalScope archived) {
        var actions=new ArrayList<String>();
        state.legalMoves().forEach(m->actions.add("move:"+m));
        state.legalSwitches().forEach(s->actions.add("switch:"+s));
        var fixture=new OfflineShadowProvenanceGate.Input(frames,
                OfflineShadowProvenanceGate.fixtureSha256(frames,state.turn(),
                        "p1a","p2a",1,actions),
                state.turn(),"p1a","p2a",1,actions);
        return new OfflineShadowResponseScenarios.Input(OfflineShadowResponseScenarios.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,
                fixture,state,mechanics,archived);
    }
    private static OfflineShadowResponseScenarios.Input input(
            List<String> frames,BattleSnapshot state) {
        return input(frames,state,mechanics(false),null);
    }
    /**
     * Genuine Phase 5.21 synthetic custody recomputation, never a current
     * Phase 6 opponent. The hidden nickname and source-only Nasty Plot/Swords
     * Dance proposals must remain separate from witnessed fixture Scratch.
     */
    private static PublicShadowBuildHandoff.Report signedHistorical() {
        final long now=1_700_000_100L;
        try {
            String log="|gen|9\\n|gametype|singles\\n|tier|[Gen 9] OU\\n"
                    +"|player|p1|PRIVATE_OWNER\\n|start\\n"
                    +"|switch|p1a: Ally|Pikachu, L100|100/100\\n"
                    +"|switch|p2a: PRIVATE_NICK|Zoroark, L100|100/100\\n"
                    +"|turn|1\\n|-ability|p2a: PRIVATE_NICK|Illusion\\n"
                    +"|move|p2a: PRIVATE_NICK|Nasty Plot|p1a: Ally\\n"
                    +"|win|PRIVATE_OWNER\\n";
            var source=new PublicReplayCorpus.Source("gen9ou-87654321","gen9ou",
                    true,PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var admitted=PublicReplayCorpus.ingest(source,log);
            var decision=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    admitted.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    now-25,now+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var kp=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(kp.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"phase63-synthetic",
                    now-10,"0".repeat(64),ledger));
            var signed=new PublicSignedCustody.SignedManifest(1,"phase63-synthetic",
                    now-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(signer.sign()));
            var key=new PublicSignedCustody.TrustedKey("phase63-synthetic",
                    Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()),
                    now-100,0);
            var trust=new PublicSignedCustody.TrustedPolicy(1,1,1,now-100,"",List.of(key));
            var guess=new PublicBuildCandidateFilter.Candidate(2,"zoroark",
                    Set.of("nastyplot","swordsdance"),Set.of(),Set.of("illusion"),Set.of());
            return PublicShadowBuildHandoff.prepare(source,log,List.of(guess),List.of(),
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    Set.of(),signed,trust,now,PublicShadowBuildHandoff.Pin.pinned());
        } catch (GeneralSecurityException error) {
            throw new AssertionError("synthetic signed custody unavailable",error);
        }
    }
    static void run() {
        var src=frames("Scratch","Tackle");
        var state=snapshot(1,List.of("swift","tackle"),
                List.of("scratch","tackle"),List.of());
        var a=OfflineShadowResponseScenarios.construct(input(src,state));
        check(a.conditionalCases().equals(List.of(
                new OfflineShadowResponseScenarios.ConditionalCase("move:swift","scratch"),
                new OfflineShadowResponseScenarios.ConditionalCase("move:swift","tackle"),
                new OfflineShadowResponseScenarios.ConditionalCase("move:tackle","scratch"),
                new OfflineShadowResponseScenarios.ConditionalCase("move:tackle","tackle"))),
                "stable 2x2 witnessed/source-covered matrix");
        check(a.conditionalPlans().size()==2
                && a.conditionalPlans().stream().allMatch(p->p.scenarios().size()==2
                && p.scenarios().stream().allMatch(line->line.size()==1
                && line.getFirst().request()==null)),
                "no second-turn or fabricated future request");
        check(a.conditionalCases().equals(
                OfflineShadowResponseScenarios.construct(input(src,state)).conditionalCases()),
                "determinism");
        check(!a.opponentResponseSetComplete() && !a.opponentNextMoveLegalOrKnown()
                && !a.calibratedProbabilities() && !a.archivedAppearanceBoundToFixture()
                && !a.installedLegalityVerified() && !a.currentBattleBound()
                && !a.shadowSearchAuthorized() && !a.livePromotionAuthorized(),
                "all authority gates closed");
        check(a.abstentions().contains(OfflineShadowResponseScenarios.Abstention.OPEN_OPPONENT_RESPONSE_SET)
                && a.abstentions().contains(OfflineShadowResponseScenarios.Abstention.NO_RESPONSE_PROBABILITIES),
                "never infer completeness/probability");
        try {a.conditionalCases().clear();throw new AssertionError("mutable output");}
        catch(UnsupportedOperationException expected) {checks++;}
        var empty=OfflineShadowResponseScenarios.construct(input(frames(),
                snapshot(1,List.of("tackle"),List.of(),List.of())));
        check(empty.conditionalCases().isEmpty()
                && empty.abstentions().contains(OfflineShadowResponseScenarios.Abstention.NO_WITNESSED_REPLIES),
                "missing public reply abstains");
        var unknown=OfflineShadowResponseScenarios.construct(input(frames("Secret Move"),
                snapshot(1,List.of("tackle"),List.of("secretmove"),List.of())));
        check(unknown.conditionalPlans().isEmpty()
                && unknown.abstentions().contains(OfflineShadowResponseScenarios.Abstention.UNSUPPORTED_PUBLIC_REPLY),
                "unsupported public attack not silently modeled");
        var ownUnknown=OfflineShadowResponseScenarios.construct(input(frames("Scratch"),
                snapshot(1,List.of("tackle","privateattack"),List.of("scratch"),List.of())));
        check(ownUnknown.conditionalPlans().size()==1
                && ownUnknown.abstentions().contains(OfflineShadowResponseScenarios.Abstention.UNSUPPORTED_OWN_MOVE),
                "unsupported own action abstains");
        var switchState=snapshot(1,List.of("tackle"),List.of("scratch"),List.of("reserve"));
        var switched=OfflineShadowResponseScenarios.construct(input(frames("Scratch"),switchState));
        check(switched.conditionalPlans().size()==1
                && switched.abstentions().contains(OfflineShadowResponseScenarios.Abstention.UNCERTIFIED_OWN_SWITCH_ENTRY),
                "no switch entry certainty");
        reject(()->OfflineShadowResponseScenarios.construct(input(frames("Scratch"),
                switchState,mechanics(true),null)),"trapped switch");
        reject(()->OfflineShadowResponseScenarios.construct(input(src,
                snapshot(2,List.of("swift","tackle"),List.of("scratch","tackle"),List.of()))),
                "stale/future turn");
        reject(()->OfflineShadowResponseScenarios.construct(input(src,
                snapshot(1,List.of("swift","tackle"),List.of("unknown","tackle"),List.of()))),
                "forged reply");
        var reentry=new ArrayList<>(frames("Scratch"));
        reentry.add("update\n|switch|p2a: Opp|synthetic, L50|100/100");
        reject(()->OfflineShadowResponseScenarios.construct(input(reentry,
                snapshot(1,List.of("tackle"),List.of("scratch"),List.of()))),
                "same nickname cross-appearance");
        var privateFrame=new ArrayList<>(frames("Scratch"));
        privateFrame.add("update\n|request|{private:true}");
        reject(()->OfflineShadowResponseScenarios.construct(input(privateFrame,
                snapshot(1,List.of("tackle"),List.of("scratch"),List.of()))),
                "private opponent request");
        reject(()->new OfflineShadowResponseScenarios.Input("phase6.4-drift",
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),CobblemonMechanics.SOURCE,
                input(src,state).fixture(),state,mechanics(false),null),
                "schema/version drift");
        reject(()->new OfflineShadowResponseScenarios.Input(OfflineShadowResponseScenarios.SCHEMA,
                PublicCompetitiveConsumerContract.ConsumerRequest.offlinePinned(),
                PublicShadowBuildHandoff.Pin.pinned(),"wrong-source-revision",
                input(src,state).fixture(),state,mechanics(false),null),
                "Phase4 source drift");
        reject(()->new PublicCompetitiveConsumerContract.ConsumerRequest(
                PublicCompetitiveConsumerContract.Purpose.SHADOW_TACTICAL,
                PublicCompetitiveConsumerContract.ContractPin.pinned(),false,false),
                "Phase5 tactical promotion");
        var archived=signedHistorical();
        check(archived.appearances().size()==1
                && archived.appearances().getFirst().index()==2,
                "sealed signed synthetic Phase 5.21 report");
        var scope=new OfflineShadowResponseScenarios.HistoricalScope(archived,2);
        var withHistory=OfflineShadowResponseScenarios.construct(input(
                frames("Scratch"),snapshot(1,List.of("tackle"),List.of("scratch"),List.of()),
                mechanics(false),scope));
        check(withHistory.conditionalCases().size()==1
                && withHistory.conditionalCases().getFirst().witnessedOpponentMove().equals("scratch")
                && withHistory.conditionalCases().stream().noneMatch(c->
                    c.witnessedOpponentMove().equals("nastyplot")
                    || c.witnessedOpponentMove().equals("swordsdance")),
                "signed historical events/source possibilities are not fixture replies");
        check(withHistory.historical()!=null
                && withHistory.historical().archivedAppearance()==2
                && withHistory.historical().proposedCandidates()==1
                && !withHistory.historical().nativeBuildIdentityProven()
                && !withHistory.archivedAppearanceBoundToFixture()
                && withHistory.abstentions().contains(
                   OfflineShadowResponseScenarios.Abstention.PHASE5_GUESSES_NOT_REPLIES),
                "historical graded uncertainty does not resolve current identity");
        check(!withHistory.toString().contains("PRIVATE_OWNER")
                && !withHistory.toString().contains("PRIVATE_NICK")
                && !withHistory.historical().toString().contains("PRIVATE"),
                "no nickname/replay ID disclosure through diagnostics");
        reject(()->new OfflineShadowResponseScenarios.HistoricalScope(archived,1),
                "wrong archived appearance ordinal");
        System.out.println("Phase 6.3 synthetic conditional scenarios PASS: "+checks+" checks");
    }
}
