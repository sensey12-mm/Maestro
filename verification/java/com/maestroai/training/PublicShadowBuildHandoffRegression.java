package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;

/** Synthetic-only Phase 5.21 candidate, public-evidence and interface tests. */
final class PublicShadowBuildHandoffRegression {
    private PublicShadowBuildHandoffRegression(){}
    private static int checks;
    private static void check(boolean x){
        if(!x)throw new AssertionError("Phase 5.21 shadow build handoff");
        checks++;
    }
    private static void reject(Runnable f){
        try{f.run();}
        catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains("PRIVATE_NAME")
                    && !expected.getMessage().contains("SECRET_NICK"));
            return;
        }
        throw new AssertionError("Phase 5.21 accepted invalid/unsourced input");
    }
    private static final long NOW=1_700_000_100L;
    private static final String BEGIN="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|PRIVATE_NAME\n|start\n"
            +"|switch|p1a: Ally|Pikachu, L100|100/100\n";
    private static final String WIN="|win|PRIVATE_NAME\n";
    private static String entry(String species){
        return "|switch|p2a: SECRET_NICK|"+species+", L100|100/100\n";
    }
    private static String move(String m){return "|move|p2a: SECRET_NICK|"+m+"|p1a: Ally\n";}
    private static String ability(String m){return "|-ability|p2a: SECRET_NICK|"+m+"\n";}
    private static String item(String m){return "|-item|p2a: SECRET_NICK|"+m+"\n";}
    private static PublicBuildCandidateFilter.Candidate candidate(int index,String display,
            String[] moves,String[] excluded,String[] abilities,String[] items){
        return new PublicBuildCandidateFilter.Candidate(index,display,
                Set.of(moves),Set.of(excluded),Set.of(abilities),Set.of(items));
    }
    private static PublicBuildCandidateFilter.Candidate guessMove(int index,String display,String move){
        return candidate(index,display,new String[]{move},new String[]{},
                new String[]{},new String[]{});
    }
    private static PublicShadowBuildHandoff.PublicObservationConstraint constraint(
            int index,String[] moves,String[] abilities,String[] items) {
        return new PublicShadowBuildHandoff.PublicObservationConstraint(index,
                Set.of(moves),Set.of(abilities),Set.of(items));
    }
    private record Fixture(PublicReplayCorpus.Source source,String log,
                           PublicSignedCustody.SignedManifest signed,
                           PublicSignedCustody.TrustedPolicy trust) {
        PublicShadowBuildHandoff.Report build(List<PublicBuildCandidateFilter.Candidate> candidates,
                List<PublicShadowBuildHandoff.PublicObservationConstraint> restrictions) {
            return build(candidates,restrictions,Set.of());
        }
        PublicShadowBuildHandoff.Report build(List<PublicBuildCandidateFilter.Candidate> candidates,
                List<PublicShadowBuildHandoff.PublicObservationConstraint> restrictions,
                Set<PublicFormBuildReconciliation.OverrideNotice> overrides) {
            return PublicShadowBuildHandoff.prepare(source,log,candidates,restrictions,
                    PublicSpeciesBuildLegality.SourcePin.pinned(),
                    PublicFormBuildReconciliation.ResolverPin.pinned(),
                    overrides,signed,trust,NOW,PublicShadowBuildHandoff.Pin.pinned());
        }
    }
    private static Fixture fixture(int n,String log){
        try{
            var source=new PublicReplayCorpus.Source("gen9ou-"+n,"gen9ou",true,
                    PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(source,log);
            var decision=new PublicReplayCustody.Decision(source.replayId(),source.format(),
                    evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    NOW-25,NOW+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(keys.getPrivate());
            signer.update(PublicSignedCustody.canonicalBytes(1,"phase521-synthetic",
                    NOW-10,"0".repeat(64),ledger));
            var manifest=new PublicSignedCustody.SignedManifest(1,"phase521-synthetic",
                    NOW-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(signer.sign()));
            var key=new PublicSignedCustody.TrustedKey("phase521-synthetic",
                    Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),
                    NOW-100,0);
            return new Fixture(source,log,manifest,
                    new PublicSignedCustody.TrustedPolicy(1,1,1,NOW-100,"",List.of(key)));
        }catch(GeneralSecurityException e){throw new AssertionError(e);}
    }
    private static PublicShadowBuildHandoff.Signal signal(PublicShadowBuildHandoff.Appearance a,
                    PublicOpponentBuildHypotheses.Kind kind,String id){
        return a.signals().stream().filter(s->s.kind()==kind && s.id().equals(id))
                .findFirst().orElseThrow();
    }
    private static final PublicOpponentBuildHypotheses.Kind MOVE=PublicOpponentBuildHypotheses.Kind.MOVE;
    private static final PublicOpponentBuildHypotheses.Kind ABILITY=PublicOpponentBuildHypotheses.Kind.ABILITY;
    private static final PublicOpponentBuildHypotheses.Kind ITEM=PublicOpponentBuildHypotheses.Kind.ITEM;
    private static final PublicOpponentBuildHypotheses.Kind ROLE=PublicOpponentBuildHypotheses.Kind.ROLE;
    private static final PublicShadowBuildHandoff.Grade PUBLIC=
            PublicShadowBuildHandoff.Grade.PUBLIC_EVENT_SOURCE_ALIGNED;
    private static final PublicShadowBuildHandoff.Grade SOURCE=
            PublicShadowBuildHandoff.Grade.SOURCE_SUPPORTED_POSSIBILITY;
    private static final PublicShadowBuildHandoff.Grade GUESS=
            PublicShadowBuildHandoff.Grade.UNVERIFIED_PROPOSAL;
    private static final PublicShadowBuildHandoff.Limit NO_NATIVE=
            PublicShadowBuildHandoff.Limit.NATIVE_BUILD_NOT_PROVEN;

    static void run(){
        var pin=PublicShadowBuildHandoff.Pin.pinned();
        check(pin.version()==1 && pin.schema().equals(PublicShadowBuildHandoff.SCHEMA));
        check(pin.consumer().equals(PublicCompetitiveConsumerContract.ContractPin.pinned()));
        check(pin.reviewedInterfaces().equals(PublicCompetitiveHandoffAudit.CrossPhaseSnapshot.audited()));
        reject(()->new PublicShadowBuildHandoff.Pin(2,pin.schema(),pin.consumer(),
                pin.reviewedInterfaces()));
        reject(()->new PublicShadowBuildHandoff.Pin(1,"future-schema",pin.consumer(),
                pin.reviewedInterfaces()));
        check(Arrays.stream(PublicShadowBuildHandoff.Report.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));

        String standard=BEGIN+entry("Zoroark")+"|turn|1\n"
                +ability("Illusion")+item("Leftovers")+move("Nasty Plot")+WIN;
        var f=fixture(12345678,standard);
        var guesses=List.of(
                candidate(2,"zoroark",new String[]{"nastyplot","swordsdance"},
                        new String[]{},new String[]{"illusion"},new String[]{}),
                guessMove(2,"zoroark","shadowsneak"),
                guessMove(2,"zoroark","frustration"),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{"pressure"},new String[]{"choicescarf"}),
                candidate(2,"zoroark",new String[]{},new String[]{},
                        new String[]{},new String[]{"leftovers"}));
        var observation=constraint(2,new String[]{"nastyplot"},
                new String[]{"illusion"},new String[]{"leftovers"});
        var r=f.build(guesses,List.of(observation));
        check(r.schema().equals(PublicShadowBuildHandoff.SCHEMA));
        check(r.sourceRevision().equals(PublicSpeciesBuildLegality.SOURCE_REVISION));
        check(r.policySha256().equals(PublicBuildCandidateRobustness.frozenPolicySha256()));
        check(r.mechanicsScope().equals(CobblemonMechanics.SOURCE));
        check(r.originalReplaySignedCustodyRechecked()
                && !r.currentBattleEvidenceBound()
                && !r.legalActionsAvailable()
                && !r.calibratedOpponentProbabilities()
                && !r.speciesIdentityProven()
                && !r.completeLegalSetProven()
                && !r.independentlyApprovedReplayRights()
                && !r.phase6SearchAuthorized()
                && !r.shadowTacticalReady()
                && !r.livePromotionAuthorized());
        check(r.appearances().size()==1 && r.candidates().size()==guesses.size());
        var a=r.appearances().get(0);
        check(a.index()==2 && a.displayedForm().equals("zoroark")
                && a.identityUncertain()
                && a.constraintVerdict()
                   ==PublicShadowBuildHandoff.ConstraintVerdict.VERIFIED_SUBSET_OF_SAME_PUBLIC_APPEARANCE);
        check(a.limitations().contains(PublicShadowBuildHandoff.Limit.DISPLAY_NOT_VERIFIED_IDENTITY));
        check(a.limitations().contains(PublicShadowBuildHandoff.Limit.INSTALLED_REGISTRY_NOT_VERIFIED));
        check(signal(a,MOVE,"nastyplot").grade()==PUBLIC);
        check(signal(a,MOVE,"nastyplot").publiclyObserved());
        check(signal(a,MOVE,"nastyplot").listedLearnMethods().contains("level"));
        check(signal(a,MOVE,"swordsdance").grade()==SOURCE);
        check(!signal(a,MOVE,"swordsdance").publiclyObserved());
        check(signal(a,MOVE,"swordsdance").listedLearnMethods().contains("tm"));
        check(signal(a,MOVE,"swordsdance").limitations().contains(
                PublicShadowBuildHandoff.Limit.NOT_OBSERVED_IN_THIS_APPEARANCE));
        check(signal(a,MOVE,"shadowsneak").grade()==GUESS);
        check(signal(a,MOVE,"shadowsneak").listedLearnMethods().isEmpty());
        check(signal(a,MOVE,"frustration").grade()
                ==PublicShadowBuildHandoff.Grade.SOURCE_LISTED_NONLEGAL_QUERY_ONLY);
        check(signal(a,MOVE,"frustration").listedLearnMethods().contains("legacy"));
        check(signal(a,MOVE,"frustration").limitations().contains(
                PublicShadowBuildHandoff.Limit.MOVE_METHOD_NOT_AVAILABLE));
        check(signal(a,ABILITY,"illusion").grade()==PUBLIC);
        check(signal(a,ABILITY,"pressure").grade()==GUESS);
        check(signal(a,ITEM,"leftovers").grade()==PublicShadowBuildHandoff.Grade.PUBLIC_EVENT_ONLY);
        check(signal(a,ITEM,"leftovers").limitations().contains(
                PublicShadowBuildHandoff.Limit.ITEM_ELIGIBILITY_UNKNOWN));
        check(signal(a,ITEM,"choicescarf").grade()==GUESS);
        check(a.signals().stream().allMatch(s->s.limitations().contains(NO_NATIVE)
                && s.limitations().contains(PublicShadowBuildHandoff.Limit.ZERO_CALIBRATED_PROBABILITIES)));
        check(signal(a,ROLE,"special-attack-boost-signal").grade()==SOURCE);
        check(signal(a,ROLE,"special-attack-boost-signal").limitations().contains(
                PublicShadowBuildHandoff.Limit.NOT_OBSERVED_IN_THIS_APPEARANCE));
        var first=r.candidates().get(0);
        check(first.ordinal()==1 && first.appearanceIndex()==2
                && first.verdict()==PublicCompetitiveConsumerContract.Classification.SOURCE_CONDITIONAL_ONLY);
        check(first.proposedMoves().equals(Set.of("nastyplot","swordsdance")));
        check(first.proposedAbilities().equals(Set.of("illusion")));
        check(!first.nativeBuildVerified());
        check(first.limitations().contains(
                PublicShadowBuildHandoff.Limit.CANDIDATE_ASSERTION_NOT_A_REAL_BUILD));
        check(r.candidates().get(1).verdict()
                ==PublicCompetitiveConsumerContract.Classification.ASSERTION_DISAGREEMENT_ONLY);
        check(r.candidates().get(2).verdict()
                ==PublicCompetitiveConsumerContract.Classification.UNRESOLVED);
        check(r.candidates().get(3).verdict()
                ==PublicCompetitiveConsumerContract.Classification.UNRESOLVED);
        check(r.candidates().get(4).proposedItems().contains("leftovers"));
        check(!r.toString().contains("PRIVATE_NAME")
                && !r.toString().contains("SECRET_NICK")
                && !r.toString().contains("gen9ou-"));
        try{r.appearances().clear();throw new AssertionError("mutated");}
        catch(UnsupportedOperationException expected){checks++;}
        try{a.signals().clear();throw new AssertionError("mutated");}
        catch(UnsupportedOperationException expected){checks++;}
        try{r.candidates().clear();throw new AssertionError("mutated");}
        catch(UnsupportedOperationException expected){checks++;}
        try{signal(a,MOVE,"swordsdance").listedLearnMethods().add("fake");
            throw new AssertionError("mutated");
        }catch(UnsupportedOperationException expected){checks++;}
        try{first.proposedMoves().add("fake");throw new AssertionError("mutated");}
        catch(UnsupportedOperationException expected){checks++;}

        // Identical signed input, reordered candidates => invariant public
        // appearance evidence, different caller ordinals only.
        var reversed=new ArrayList<>(guesses);
        Collections.reverse(reversed);
        var r2=f.build(reversed,List.of());
        check(r2.appearances().get(0).signals().equals(a.signals()));
        check(r2.appearances().get(0).constraintVerdict()
                ==PublicShadowBuildHandoff.ConstraintVerdict.NO_CONSTRAINTS_REQUESTED);
        check(r2.candidates().get(0).proposedItems().equals(Set.of("leftovers")));

        // Never attach new "live" facts via an untrusted input: only a subset
        // of witnessed signed offline events can form the constraint.
        reject(()->f.build(guesses,List.of(constraint(2,
                new String[]{"shadowsneak"},new String[]{},new String[]{}))));
        reject(()->f.build(guesses,List.of(constraint(2,
                new String[]{},new String[]{"pressure"},new String[]{}))));
        reject(()->f.build(guesses,List.of(constraint(2,
                new String[]{},new String[]{},new String[]{"choicescarf"}))));
        reject(()->f.build(guesses,List.of(constraint(1,
                new String[]{"nastyplot"},new String[]{},new String[]{}))));
        reject(()->f.build(guesses,List.of(constraint(99,
                new String[]{"nastyplot"},new String[]{},new String[]{}))));
        reject(()->f.build(guesses,List.of(observation,observation)));
        reject(()->constraint(2,new String[]{"Nasty Plot"},new String[]{},new String[]{}));

        // Reused nickname/species yields independently scoped evidence.
        String repeat=BEGIN+entry("Zoroark")+"|turn|1\n"+move("Nasty Plot")
                +entry("Zoroark")+"|turn|2\n"+move("Swift")+WIN;
        var repeatFixture=fixture(12345679,repeat);
        var appearances=repeatFixture.build(List.of(
                guessMove(3,"zoroark","swift"),guessMove(2,"zoroark","nastyplot")),
                List.of(constraint(3,new String[]{"swift"},new String[]{},new String[]{})));
        check(appearances.appearances().size()==2);
        check(appearances.appearances().get(0).index()==2);
        check(appearances.appearances().get(1).index()==3);
        check(appearances.appearances().get(0).signals().stream().anyMatch(s->
                s.kind()==MOVE && s.id().equals("nastyplot") && s.publiclyObserved()));
        check(appearances.appearances().get(1).signals().stream().noneMatch(s->
                s.kind()==MOVE && s.id().equals("nastyplot") && s.publiclyObserved()));
        reject(()->repeatFixture.build(List.of(guessMove(3,"zoroark","swift")),
                List.of(constraint(3,new String[]{"nastyplot"},new String[]{},new String[]{}))));

        // Hisui inherits base ability only, and explicitly replaces moves.
        var hisui=fixture(12345680,BEGIN+entry("Zoroark-Hisui")
                +"|turn|1\n"+move("Shadow Sneak")+WIN);
        var h=hisui.build(List.of(
                guessMove(2,"zoroarkhisui","shadowsneak"),
                candidate(2,"zoroarkhisui",new String[]{},new String[]{},
                        new String[]{"illusion"},new String[]{})),List.of());
        check(signal(h.appearances().get(0),MOVE,"shadowsneak").grade()==PUBLIC);
        check(signal(h.appearances().get(0),ABILITY,"illusion").grade()==SOURCE);
        check(!signal(h.appearances().get(0),ABILITY,"illusion").publiclyObserved());
        // Source gap never proves illegal play; public event remains distinct.
        var clash=fixture(12345681,BEGIN+entry("Zoroark")
                +"|turn|1\n"+move("Shadow Sneak")+WIN);
        var c=clash.build(List.of(guessMove(2,"zoroark","nastyplot")),List.of());
        check(signal(c.appearances().get(0),MOVE,"shadowsneak").grade()
                ==PublicShadowBuildHandoff.Grade.SOURCE_DISAGREEMENT);
        check(c.candidates().get(0).verdict()
                ==PublicCompetitiveConsumerContract.Classification.ABSTAINED);
        reject(()->clash.build(List.of(guessMove(2,"zoroark","nastyplot")),
                List.of(constraint(2,new String[]{"shadowsneak"},new String[]{},new String[]{}))));

        // Transform is a public observation but is not the native moveset.
        var ditto=fixture(12345682,BEGIN+entry("Ditto")+"|turn|1\n"
                +"|-transform|p2a: SECRET_NICK|p1a: Ally\n"+move("Tackle")+WIN);
        var d=ditto.build(List.of(guessMove(2,"ditto","transform")),List.of());
        check(d.appearances().get(0).signals().stream().allMatch(s->
                s.grade()==PublicShadowBuildHandoff.Grade.ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS));
        reject(()->ditto.build(List.of(guessMove(2,"ditto","transform")),
                List.of(constraint(2,new String[]{"tackle"},new String[]{},new String[]{}))));
        // Unsupported form cannot borrow base species learnset by default.
        var unknown=fixture(12345683,BEGIN+entry("Mewtwo-Mega-Z")
                +"|turn|1\n"+move("Psystrike")+WIN);
        var u=unknown.build(List.of(guessMove(2,"mewtwomegaz","psystrike")),List.of());
        check(u.appearances().get(0).signals().stream().noneMatch(s->
                s.grade()==SOURCE));
        check(u.candidates().get(0).verdict()
                ==PublicCompetitiveConsumerContract.Classification.ABSTAINED);

        // Multiple public ability/item reveals and source overrides abstain,
        // rather than pretending one permanent current item/build exists.
        var changed=fixture(12345684,BEGIN+entry("Zoroark")+"|turn|1\n"
                +ability("Illusion")+ability("Pressure")+item("Leftovers")
                +item("Choice Scarf")+move("Nasty Plot")+WIN);
        reject(()->changed.build(List.of(guessMove(2,"zoroark","nastyplot")),
                List.of(constraint(2,new String[]{"nastyplot"},new String[]{},new String[]{}))));
        var overridden=f.build(guesses,List.of(),
                Set.of(new PublicFormBuildReconciliation.OverrideNotice(
                        "zoroark",PublicFormBuildReconciliation.Field.MOVES)));
        check(signal(overridden.appearances().get(0),MOVE,"swordsdance").grade()
                ==PublicShadowBuildHandoff.Grade.ABSTAINED_UNSAFE_IDENTITY_OR_MECHANICS);
        check(overridden.candidates().get(0).verdict()
                ==PublicCompetitiveConsumerContract.Classification.ABSTAINED);

        // Every error must refuse export: source drift, replay tampering,
        // private events, expired custody, invalid side, empty proposals.
        reject(()->f.build(List.of(),List.of()));
        reject(()->f.build(List.of(guessMove(1,"pikachu","swift")),List.of()));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),f.log(),guesses,List.of(),
                null,PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trust(),NOW,pin));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),f.log(),guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),null,Set.of(),
                f.signed(),f.trust(),NOW,pin));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),f.log(),guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                null,f.trust(),NOW,pin));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),f.log(),guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trust(),NOW,null));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),f.log(),guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trust(),NOW+2000,pin));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),
                f.log().replace("Nasty Plot","Shadow Claw"),guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trust(),NOW,pin));
        reject(()->PublicShadowBuildHandoff.prepare(f.source(),
                f.log()+"|request|PRIVATE_NAME\n",guesses,List.of(),
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),Set.of(),
                f.signed(),f.trust(),NOW,pin));
        reject(()->new PublicShadowBuildHandoff.Signal(MOVE,"private move",PUBLIC,true,
                Set.of(),Set.of(NO_NATIVE,PublicShadowBuildHandoff.Limit.ZERO_CALIBRATED_PROBABILITIES)));
        reject(()->new PublicShadowBuildHandoff.Signal(MOVE,"nastyplot",SOURCE,true,
                Set.of(),Set.of(NO_NATIVE,PublicShadowBuildHandoff.Limit.ZERO_CALIBRATED_PROBABILITIES)));
        reject(()->new PublicShadowBuildHandoff.Possibility(1,2,
                PublicCompetitiveConsumerContract.Classification.SOURCE_CONDITIONAL_ONLY,
                Set.of(),Set.of(),Set.of(),Set.of(),Set.of(),true));
        System.out.println("Phase 5.21 synthetic Shadow build handoff PASS: "+checks+" checks");
    }
}
