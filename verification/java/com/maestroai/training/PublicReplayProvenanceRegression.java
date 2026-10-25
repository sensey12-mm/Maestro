package com.maestroai.training;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Phase 5.7: synthetic evidence custody, immutability and anti-forgery tests.
 * The input strings are invented. No external replay or Minecraft acceptance.
 */
final class PublicReplayProvenanceRegression {
    private PublicReplayProvenanceRegression() {}
    private static int checks;
    private static void check(boolean pass) {
        if (!pass) throw new AssertionError("Phase 5.7 trusted offline provenance");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            check(expected.getMessage().equals("Unverified offline public evidence")
                    || !expected.getMessage().contains("SECRET_SYNTHETIC_PERSON"));
            return;
        }
        throw new AssertionError("Forged public provenance was trusted");
    }
    private static PublicReplayCorpus.Source source(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static final CompetitiveFormatNormalizer.Target PIN =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final String LOG =
            "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SECRET_SYNTHETIC_PERSON\n|start\n"
            +"|switch|p1a: Friendly|Pikachu, L100|100/100\n"
            +"|switch|p2a: Hidden|Zoroark, L100|100/100\n|turn|1\n"
            +"|move|p2a: Hidden|Swift|p1a: Friendly\n"
            +"|move|p2a: Hidden|Nasty Plot|p1a: Friendly\n"
            +"|switch|p2a: Hidden|Zoroark, L100|100/100\n|turn|2\n"
            +"|move|p2a: Hidden|Dark Pulse|p1a: Friendly\n"
            +"|win|SECRET_SYNTHETIC_PERSON\n";
    private static final String OTHER =
            "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|ANOTHER_SYNTHETIC_PERSON\n|start\n"
            +"|switch|p1a: Friend|Pikachu, L100|100/100\n"
            +"|switch|p2a: Opponent|Ditto, L100|100/100\n|turn|1\n"
            +"|move|p2a: Opponent|Tackle|p1a: Friend\n"
            +"|win|ANOTHER_SYNTHETIC_PERSON\n";
    private static PublicReplayCorpus.Evidence evidence() {
        return PublicReplayCorpus.ingest(source("12345678"),LOG);
    }
    private static CompetitiveFormatNormalizer.Result normalized() {
        return CompetitiveFormatNormalizer.normalize(evidence(),PIN);
    }
    private static PublicCompetitiveTendencies.Report tendencies() {
        return PublicCompetitiveTendencies.analyze(source("12345678"),LOG,PIN);
    }
    private static PublicReplayProvenance.CheckedReplay validate(
            PublicReplayCorpus.Evidence e, CompetitiveFormatNormalizer.Result n,
            PublicCompetitiveTendencies.Report t) {
        return PublicReplayProvenance.verify(source("12345678"),LOG,PIN,e,n,t);
    }
    private static PublicHeldOutEvaluation.Replay train() {
        return new PublicHeldOutEvaluation.Replay(source("12345678"),LOG,
                PublicHeldOutEvaluation.Partition.TRAIN);
    }
    private static PublicHeldOutEvaluation.Replay heldOut() {
        return new PublicHeldOutEvaluation.Replay(source("23456789"),OTHER,
                PublicHeldOutEvaluation.Partition.HELD_OUT);
    }
    static void run() {
        var e=evidence();
        var n=normalized();
        var t=tendencies();
        var validated=validate(e,n,t);
        var direct=PublicReplayProvenance.ingest(source("12345678"),LOG,PIN);
        check(validated.boundaries().equals(direct.boundaries()));
        check(validated.boundaries().contains(
                PublicReplayProvenance.Boundary.RECONSTRUCTED_FROM_SUPPLIED_PUBLIC_LOG));
        check(validated.boundaries().contains(
                PublicReplayProvenance.Boundary.RIGHTS_ATTESTED_BY_OPERATOR_NOT_EXTERNALLY_VERIFIED));
        check(!validated.currentBattleBound() && !validated.independentlyAuthorized()
                && !validated.tacticalDecisionReady());
        check(validated.shadowAudit().sameUpstreamEvidence());
        check(validated.shadowAudit().offlineSchemaCompatible());
        check(!validated.shadowAudit().shadowTacticalReady()
                && !validated.shadowAudit().livePromotionAuthorized());
        check(validated.shadowAudit().blockers().contains(
                PublicCompetitiveShadowContract.Blocker.EXTERNALLY_CONSTRUCTED_RECORDS_NOT_AUTHENTICATED));
        check(validated.shadowAudit().blockers().contains(
                PublicCompetitiveShadowContract.Blocker.NO_OWN_REQUEST_LEGAL_ACTIONS));
        check(!validated.toString().contains("SECRET_SYNTHETIC_PERSON")
                && !validated.toString().contains("gen9ou-12345678")
                && !validated.toString().contains(e.inputSha256()));
        check(!validated.shadowAudit().toString().contains("gen9ou-"));
        try { validated.boundaries().clear(); throw new AssertionError("mutable receipt"); }
        catch (UnsupportedOperationException expected) { checks++; }

        // Receipt constructors cannot be called by ordinary external code.
        check(Arrays.stream(PublicReplayProvenance.CheckedReplay.class
                .getDeclaredConstructors()).allMatch(c -> Modifier.isPrivate(c.getModifiers())));
        check(Arrays.stream(PublicReplayProvenance.CheckedSplit.class
                .getDeclaredConstructors()).allMatch(c -> Modifier.isPrivate(c.getModifiers())));
        check(!java.io.Serializable.class.isAssignableFrom(
                PublicReplayProvenance.CheckedReplay.class));
        check(!java.io.Serializable.class.isAssignableFrom(
                PublicReplayProvenance.CheckedSplit.class));

        // The old schema check is useful but cannot authenticate public record constructors.
        check(PublicCompetitiveShadowContract.audit(n,t).offlineSchemaCompatible());
        check(PublicCompetitiveShadowContract.audit(n,t).blockers().contains(
                PublicCompetitiveShadowContract.Blocker.EXTERNALLY_CONSTRUCTED_RECORDS_NOT_AUTHENTICATED));

        // Complete exact-copy records are consistency-checkable, not an independent
        // rights/custody proof. A single changed field is rejected.
        var forgedHash = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                "a".repeat(64),e.publicEventCount(),e.turns(),e.appearances(),e.compatibility());
        reject(()->validate(forgedHash,n,t));
        var forgedReplayId = new PublicReplayCorpus.Evidence("gen9ou-99999999",
                e.format(),e.inputSha256(),e.publicEventCount(),e.turns(),
                e.appearances(),e.compatibility());
        reject(()->validate(forgedReplayId,n,t));
        var forgedRights = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                e.inputSha256(),e.publicEventCount(),e.turns(),e.appearances(),
                "SHOWDOWN_PUBLIC_OBSERVATIONS;RUNTIME_VERIFIED");
        reject(()->validate(forgedRights,n,t));
        var forgedEvents = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                e.inputSha256(),e.publicEventCount()+1,e.turns(),e.appearances(),e.compatibility());
        reject(()->validate(forgedEvents,n,t));
        var forgedTurns = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                e.inputSha256(),e.publicEventCount(),e.turns()+1,e.appearances(),e.compatibility());
        reject(()->validate(forgedTurns,n,t));

        var fakeAppearances = new ArrayList<>(e.appearances());
        var observed = fakeAppearances.get(1);
        fakeAppearances.set(1,new PublicReplayCorpus.Appearance(observed.index(),
                observed.slot(),"mewtwo",observed.nativeMoves(),observed.revealedItem(),
                observed.revealedAbility(),observed.transformed(),false,true));
        var forgedSpecies = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                e.inputSha256(),e.publicEventCount(),e.turns(),fakeAppearances,e.compatibility());
        reject(()->validate(forgedSpecies,n,t));
        fakeAppearances = new ArrayList<>(e.appearances());
        observed=fakeAppearances.get(1);
        fakeAppearances.set(1,new PublicReplayCorpus.Appearance(observed.index(),
                observed.slot(),observed.displayedSpecies(),Set.of("swift","secretmove"),
                observed.revealedItem(),observed.revealedAbility(),false,false,true));
        var forgedMoves = new PublicReplayCorpus.Evidence(e.replayId(),e.format(),
                e.inputSha256(),e.publicEventCount(),e.turns(),fakeAppearances,e.compatibility());
        reject(()->validate(forgedMoves,n,t));

        var wrongProvenance=new CompetitiveFormatNormalizer.Provenance(
                n.provenance().replayId(),n.provenance().inputSha256(),
                n.provenance().publicEvents(),n.provenance().turns(),
                "EXTERNALLY_CERTIFIED_BY_ATTACKER");
        var forgedNormalized=new CompetitiveFormatNormalizer.Result(wrongProvenance,
                n.rules(),n.targetSourceScope(),false,n.appearances());
        reject(()->validate(e,forgedNormalized,t));

        var falselyVerified=new CompetitiveFormatNormalizer.Result(n.provenance(),
                n.rules(),n.targetSourceScope(),true,n.appearances());
        reject(()->validate(e,falselyVerified,t));

        var fakeRules=new CompetitiveFormatNormalizer.Rules(9,"singles","ubers",
                false,CompetitiveFormatNormalizer.Status.UNKNOWN);
        var wrongRules=new CompetitiveFormatNormalizer.Result(n.provenance(),
                fakeRules,n.targetSourceScope(),false,n.appearances());
        reject(()->validate(e,wrongRules,t));

        var wrongVersion=new CompetitiveFormatNormalizer.Result(n.provenance(),
                n.rules(),"OUTSIDE_PINNED_COBBLEMON_1_7_3_BUILD_7553235",
                false,n.appearances());
        reject(()->validate(e,wrongVersion,t));

        var forgedTendency=new PublicCompetitiveTendencies.Report(t.provenance(),t.rules(),
                t.targetSourceScope(),t.appearances(),t.sides(),
                t.interpretation(),false);
        check(forgedTendency.equals(t)); // Consistency does not imply authority.
        check(validate(e,n,forgedTendency).shadowAudit().offlineSchemaCompatible());

        var newRows=new ArrayList<>(t.appearances());
        var row=newRows.get(1);
        var newUses=new ArrayList<>(row.nativeUses());
        var firstMove=newUses.get(0);
        newUses.set(0,new PublicCompetitiveTendencies.MoveUse(
                firstMove.token(),firstMove.publicUses()+3));
        newRows.set(1,new PublicCompetitiveTendencies.Appearance(
                row.index(),row.slot(),row.displayedSpecies(),row.entry(),
                newUses,row.transformed(),false,true));
        var forgedCounts=new PublicCompetitiveTendencies.Report(t.provenance(),t.rules(),
                t.targetSourceScope(),newRows,t.sides(),t.interpretation(),false);
        reject(()->validate(e,n,forgedCounts));

        var forgedSide=new TreeMap<>(t.sides());
        var p2=forgedSide.get("p2a");
        forgedSide.put("p2a",new PublicCompetitiveTendencies.Side("p2a",
                p2.switchAnnouncements()+1,p2.forcedDragAnnouncements(),
                p2.repeatedDisplayCandidates(),false));
        var fakeSwitches=new PublicCompetitiveTendencies.Report(t.provenance(),t.rules(),
                t.targetSourceScope(),t.appearances(),forgedSide,t.interpretation(),false);
        reject(()->validate(e,n,fakeSwitches));

        var wrongAppearance=new ArrayList<>(t.appearances());
        var old=wrongAppearance.get(1);
        wrongAppearance.set(1,new PublicCompetitiveTendencies.Appearance(
                old.index(),old.slot(),"gholdengo",old.entry(),old.nativeUses(),
                old.transformed(),false,true));
        var spoofedTendency=new PublicCompetitiveTendencies.Report(t.provenance(),t.rules(),
                t.targetSourceScope(),wrongAppearance,t.sides(),t.interpretation(),false);
        reject(()->validate(e,n,spoofedTendency));

        // Same replay ID and nominal schema are insufficient without exact input.
        reject(()->PublicReplayProvenance.verify(source("12345678"),
                LOG.replace("Hidden","RenamedEnemy"),PIN,e,n,t));
        reject(()->PublicReplayProvenance.verify(source("23456789"),LOG,PIN,e,n,t));
        reject(()->PublicReplayProvenance.verify(source("12345678"),LOG,PIN,null,n,t));
        reject(()->PublicReplayProvenance.verify(source("12345678"),LOG,PIN,e,null,t));
        reject(()->PublicReplayProvenance.verify(source("12345678"),LOG,PIN,e,n,null));
        reject(()->PublicReplayProvenance.ingest(source("12345678"),
                LOG,new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","other","unknown")));
        reject(()->PublicReplayProvenance.ingest(source("12345678"),
                LOG+"|request|SECRET_SYNTHETIC_PERSON_PRIVATE\n",PIN));
        reject(()->PublicReplayProvenance.ingest(source("12345678"),
                LOG+"|split|p2\n|private|SECRET_SYNTHETIC_PERSON_PRIVATE\n|public|x\n",PIN));

        // Independent full-group recomputation: public records cannot spoof
        // an accepted benchmark even with realistic values.
        var group=List.of(train(),heldOut());
        var heldReport=PublicHeldOutEvaluation.evaluate(group,PIN);
        var split=PublicReplayProvenance.verifySplit(group,PIN,heldReport);
        check(split.trainingReplays()==1 && split.heldOutReplays()==1);
        check(!split.calibratedForecast() && !split.legalActionReceipt());
        check(split.boundaries().contains(
                PublicReplayProvenance.Boundary.RECONSTRUCTED_FROM_SUPPLIED_SPLIT));
        check(!split.toString().contains("gen9ou-"));
        check(!split.toString().contains(heldReport.splitSha256()));
        check(PublicReplayProvenance.verifySplit(List.of(heldOut(),train()),PIN,heldReport)
                .heldOutReplays()==1);
        var alteredManifest=new PublicHeldOutEvaluation.Report("f".repeat(64),
                heldReport.sourceScope(),heldReport.trainReplays(),heldReport.heldOutReplays(),
                heldReport.training(),heldReport.heldOut(),false,false,false,false);
        reject(()->PublicReplayProvenance.verifySplit(group,PIN,alteredManifest));
        var alteredStats=new PublicHeldOutEvaluation.Report(heldReport.splitSha256(),
                heldReport.sourceScope(),heldReport.trainReplays()+1,heldReport.heldOutReplays(),
                heldReport.training(),heldReport.heldOut(),false,false,false,false);
        reject(()->PublicReplayProvenance.verifySplit(group,PIN,alteredStats));
        var reorderedParts=List.of(new PublicHeldOutEvaluation.Replay(
                source("12345678"),LOG,PublicHeldOutEvaluation.Partition.HELD_OUT),
                new PublicHeldOutEvaluation.Replay(source("23456789"),OTHER,
                        PublicHeldOutEvaluation.Partition.TRAIN));
        reject(()->PublicReplayProvenance.verifySplit(reorderedParts,PIN,heldReport));
        reject(()->PublicReplayProvenance.verifySplit(List.of(train(),train()),PIN,heldReport));
        reject(()->PublicReplayProvenance.verifySplit(group,PIN,null));
        reject(()->PublicReplayProvenance.verifySplit(group,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","other","unknown"),
                heldReport));
        try { split.boundaries().clear(); throw new AssertionError("mutable split"); }
        catch (UnsupportedOperationException expected) { checks++; }

        System.out.println("Phase 5.7 synthetic verified provenance PASS: "+checks+" checks");
    }
}
