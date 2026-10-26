package com.maestroai.training;

import java.lang.reflect.Method;
import java.util.*;

/**
 * End-to-end Phase 5.6 synthetic contract regression.
 * All samples are invented and local; no battle is requested or played.
 */
final class PublicCompetitiveShadowContractRegression {
    private PublicCompetitiveShadowContractRegression() {}
    private static int checks;
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Phase 5.6 offline Shadow contract");
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Live-promotion-shaped readiness receipt accepted");
    }
    private static final String HEADER =
            "|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
            +"|player|p1|SYNTHETIC_PUBLIC_PERSON\n|start\n";
    private static final String FIRST = HEADER
            +"|switch|p1a: QuietNick|Pikachu, L100|100/100\n"
            +"|switch|p2a: MysteryNick|Zoroark, L100|100/100\n"
            +"|turn|1\n|move|p2a: MysteryNick|Swift|p1a: QuietNick\n"
            +"|move|p2a: MysteryNick|Nasty Plot|p1a: QuietNick\n"
            +"|switch|p2a: MysteryNick|Zoroark, L100|100/100\n"
            +"|turn|2\n|move|p2a: MysteryNick|Dark Pulse|p1a: QuietNick\n"
            +"|win|SYNTHETIC_PUBLIC_PERSON\n";
    private static final String SECOND = HEADER
            +"|switch|p1a: QuietNick|Pikachu, L100|100/100\n"
            +"|switch|p2a: DifferentNick|Ditto, L100|100/100\n"
            +"|turn|1\n|move|p2a: DifferentNick|Tackle|p1a: QuietNick\n"
            +"|win|SYNTHETIC_PUBLIC_PERSON\n";
    private static PublicReplayCorpus.Source source(String id) {
        return new PublicReplayCorpus.Source("gen9ou-"+id,"gen9ou",true,
                PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
    }
    private static final CompetitiveFormatNormalizer.Target PINNED =
            CompetitiveFormatNormalizer.Target.pinnedSource();
    private static final PublicCompetitiveShadowContract.Blocker HISTORICAL =
            PublicCompetitiveShadowContract.Blocker.HISTORICAL_REPLAY_NOT_ACTIVE_BATTLE;
    private static final PublicCompetitiveShadowContract.Blocker LEGAL =
            PublicCompetitiveShadowContract.Blocker.NO_OWN_REQUEST_LEGAL_ACTIONS;

    static void run() {
        var normalized=CompetitiveFormatNormalizer.normalize(
                PublicReplayCorpus.ingest(source("12345678"),FIRST),PINNED);
        var tendencies=PublicCompetitiveTendencies.analyze(source("12345678"),FIRST,PINNED);
        var evaluated=PublicHeldOutEvaluation.evaluate(List.of(
                new PublicHeldOutEvaluation.Replay(source("12345678"),FIRST,
                        PublicHeldOutEvaluation.Partition.TRAIN),
                new PublicHeldOutEvaluation.Replay(source("23456789"),SECOND,
                        PublicHeldOutEvaluation.Partition.HELD_OUT)),PINNED);

        var audit=PublicCompetitiveShadowContract.audit(normalized,tendencies,evaluated);
        check(audit.sameUpstreamEvidence() && audit.offlineSchemaCompatible());
        check(!audit.runtimeBindingAvailable() && !audit.shadowTacticalReady()
                && !audit.livePromotionAuthorized());
        check(audit.blockers().contains(HISTORICAL) && audit.blockers().contains(LEGAL));
        check(audit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.NO_CURRENT_RUNTIME_MECHANICS_RECEIPT));
        check(audit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.NO_CALIBRATED_OPPONENT_RESPONSE));
        check(audit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.HELD_OUT_AGGREGATES_NOT_PER_ACTOR));
        check(audit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.REPLAY_ID_MAY_IDENTIFY_PUBLIC_PLAYER));
        check(!audit.toString().contains("SYNTHETIC_PUBLIC_PERSON")
                && !audit.toString().contains("MysteryNick")
                && !audit.toString().contains("gen9ou-"));
        check(PublicCompetitiveShadowContract.audit(normalized,tendencies)
                .offlineSchemaCompatible());
        check(audit.equals(PublicCompetitiveShadowContract.audit(normalized,tendencies,evaluated)));

        var badProvenance=new CompetitiveFormatNormalizer.Provenance(
                "gen9ou-99999999",normalized.provenance().inputSha256(),
                normalized.provenance().publicEvents(),normalized.provenance().turns(),
                normalized.provenance().rightsVerification());
        var wrongSource=new CompetitiveFormatNormalizer.Result(badProvenance,
                normalized.rules(),normalized.targetSourceScope(),
                false,normalized.appearances());
        var sourceAudit=PublicCompetitiveShadowContract.audit(wrongSource,tendencies);
        check(!sourceAudit.sameUpstreamEvidence() && !sourceAudit.offlineSchemaCompatible());
        check(sourceAudit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.REPLAY_PROVENANCE_MISMATCH));

        var wrongFormat=new CompetitiveFormatNormalizer.Rules(9,"singles","ubers",false,
                CompetitiveFormatNormalizer.Status.UNKNOWN);
        var changedRules=new CompetitiveFormatNormalizer.Result(normalized.provenance(),
                wrongFormat,normalized.targetSourceScope(),false,normalized.appearances());
        var rulesAudit=PublicCompetitiveShadowContract.audit(changedRules,tendencies);
        check(!rulesAudit.offlineSchemaCompatible() && rulesAudit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.FORMAT_OR_VERSION_SCOPE_MISMATCH));

        // Mutate displayed species in a public record: no hidden party join allowed.
        var rows=new ArrayList<>(tendencies.appearances());
        var prior=rows.get(1);
        rows.set(1,new PublicCompetitiveTendencies.Appearance(prior.index(),prior.slot(),
                "gholdengo",prior.entry(),prior.nativeUses(),prior.transformed(),false,true));
        var changedDisplay=new PublicCompetitiveTendencies.Report(tendencies.provenance(),
                tendencies.rules(),tendencies.targetSourceScope(),rows,
                tendencies.sides(),tendencies.interpretation(),false);
        var displayAudit=PublicCompetitiveShadowContract.audit(normalized,changedDisplay);
        check(!displayAudit.offlineSchemaCompatible() && displayAudit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.APPEARANCE_OR_MOVE_EVIDENCE_MISMATCH));

        // Forge a move token's support: consistency gate must not accept it.
        var poisonedMoves=new ArrayList<>(prior.nativeUses());
        var observed=poisonedMoves.get(0);
        var original=observed.token();
        poisonedMoves.set(0,new PublicCompetitiveTendencies.MoveUse(
                new CompetitiveFormatNormalizer.Token(original.id(),true,
                        CompetitiveFormatNormalizer.Status.OUTSIDE_VERIFIED_SCOPE,
                        original.conditionalMechanics()),observed.publicUses()));
        rows=new ArrayList<>(tendencies.appearances());
        rows.set(1,new PublicCompetitiveTendencies.Appearance(prior.index(),prior.slot(),
                prior.displayedSpecies(),prior.entry(),poisonedMoves,prior.transformed(),false,true));
        var poisoned=new PublicCompetitiveTendencies.Report(tendencies.provenance(),
                tendencies.rules(),tendencies.targetSourceScope(),rows,
                tendencies.sides(),tendencies.interpretation(),false);
        check(PublicCompetitiveShadowContract.audit(normalized,poisoned).blockers().contains(
                PublicCompetitiveShadowContract.Blocker.APPEARANCE_OR_MOVE_EVIDENCE_MISMATCH));

        var forgedSide=new TreeMap<>(tendencies.sides());
        var foe=forgedSide.get("p2a");
        forgedSide.put("p2a",new PublicCompetitiveTendencies.Side("p2a",
                foe.switchAnnouncements()+1,foe.forcedDragAnnouncements(),
                foe.repeatedDisplayCandidates(),false));
        var fakeCount=new PublicCompetitiveTendencies.Report(tendencies.provenance(),
                tendencies.rules(),tendencies.targetSourceScope(),
                tendencies.appearances(),forgedSide,tendencies.interpretation(),false);
        var countAudit=PublicCompetitiveShadowContract.audit(normalized,fakeCount);
        check(!countAudit.offlineSchemaCompatible() && countAudit.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.PUBLIC_SIDE_COUNT_INCONSISTENT));

        // A different permitted replay can never masquerade as the active one.
        var other=PublicCompetitiveTendencies.analyze(source("23456789"),SECOND,PINNED);
        check(!PublicCompetitiveShadowContract.audit(normalized,other).sameUpstreamEvidence());
        // A different target version cannot be source-verified for tactical transfer.
        var alternate=CompetitiveFormatNormalizer.normalize(
                PublicReplayCorpus.ingest(source("12345678"),FIRST),
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","other","unknown"));
        var alternateTendencies=PublicCompetitiveTendencies.analyze(source("12345678"),FIRST,
                new CompetitiveFormatNormalizer.Target("1.21.1","1.7.4","other","unknown"));
        var outOfScope=PublicCompetitiveShadowContract.audit(alternate,alternateTendencies);
        check(outOfScope.sameUpstreamEvidence() && !outOfScope.offlineSchemaCompatible());
        check(outOfScope.blockers().contains(
                PublicCompetitiveShadowContract.Blocker.UNSUPPORTED_SOURCE_VERSION));

        // Aggregate accuracy cannot bind to replay appearances or action legality.
        var malformedMetric=new PublicHeldOutEvaluation.Report(evaluated.splitSha256(),
                "OUTSIDE_PINNED_SOURCE_SCOPE",evaluated.trainReplays(),evaluated.heldOutReplays(),
                evaluated.training(),evaluated.heldOut(),false,false,false,false);
        check(!PublicCompetitiveShadowContract.audit(normalized,tendencies,malformedMetric)
                .offlineSchemaCompatible());

        check(!PublicCompetitiveShadowContract.audit(null,tendencies).offlineSchemaCompatible());
        check(!PublicCompetitiveShadowContract.audit(normalized,null).sameUpstreamEvidence());
        reject(()->new PublicCompetitiveShadowContract.Audit(true,true,true,true,true,
                EnumSet.of(HISTORICAL,LEGAL,
                        PublicCompetitiveShadowContract.Blocker.NO_CURRENT_RUNTIME_MECHANICS_RECEIPT)));
        reject(()->new PublicCompetitiveShadowContract.Audit(true,true,false,false,true,
                EnumSet.of(HISTORICAL,LEGAL,
                        PublicCompetitiveShadowContract.Blocker.NO_CURRENT_RUNTIME_MECHANICS_RECEIPT)));
        try { audit.blockers().clear(); throw new AssertionError("Mutable blockers"); }
        catch (UnsupportedOperationException expected) { checks++; }

        // Static surface contract: cannot directly produce actions, scores,
        // battle states, engine candidates, or probabilistic predictions.
        for (Method m:PublicCompetitiveShadowContract.class.getDeclaredMethods()) {
            Class<?> ret=m.getReturnType();
            check(ret!=BattleSnapshot.class
                    && ret!=BattleSnapshot.Decision.class
                    && ret!=ShadowDecisionComparator.Candidate.class
                    && ret!=PublicStateSearchAdapter.Projection.class
                    && ret!=PublicTransitionState.Effect.class);
        }
        System.out.println("Phase 5.6 offline Shadow readiness PASS: "+checks+" checks");
    }
}
