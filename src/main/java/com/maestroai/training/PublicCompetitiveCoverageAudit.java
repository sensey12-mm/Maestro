package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.11 — provenance-gated, OFFLINE coverage audit.
 *
 * Never a trained prior, prediction distribution, live recommendation or
 * empirical real-world skill claim. Each held-out replay contributes its
 * own pre-event, paired-baseline observations with no cross-replay identity
 * linkage. The report is de-identified and non-authoritative.
 */
public final class PublicCompetitiveCoverageAudit {
    private PublicCompetitiveCoverageAudit() {}
    public static final int VERSION=1;
    public static final int MIN_INDEPENDENT_REPLAYS=3;
    public static final int MIN_PAIRED_HORIZONS=30;

    public enum Action { MOVE, SWITCH }
    public enum Uncertainty {
        ALL,
        ORDINARY_IDENTITY_UNCONFIRMED,
        RETURN_DISPLAY_AMBIGUOUS,
        TRANSFORM_OBSERVED,
        UNKNOWN_MOVE_SOURCE
    }
    public enum Gate {
        NO_REPLAYS, TOO_FEW_INDEPENDENT_REPLAYS, TOO_FEW_PAIRED_HORIZONS,
        BASELINE_NOT_BEATEN, OBSERVATIONAL_COVERAGE_ONLY
    }

    public record Group(String format, Action action, Uncertainty uncertainty,
                        int independentReplays, int pairedHorizonCount,
                        int candidateHits, int baselineHits, int skippedUnknown, Gate gate) {
        public Group {
            if (!Set.of("gen9ou","gen9ubers").contains(format)
                    || action==null || uncertainty==null || gate==null
                    || independentReplays<0 || pairedHorizonCount<0
                    || candidateHits<0 || baselineHits<0 || skippedUnknown<0
                    || candidateHits>pairedHorizonCount || baselineHits>pairedHorizonCount
                    || gate!=status(independentReplays,pairedHorizonCount,candidateHits,baselineHits))
                throw invalid();
        }
        /** Even sufficient coverage is a retrospective comparison, not a forecast. */
        public boolean thresholdMet() { return gate==Gate.OBSERVATIONAL_COVERAGE_ONLY; }
    }

    public record Readiness(int minimumReplaysPerStratum,int minimumPairedHorizonsPerStratum,
                            boolean bothFormatsCovered, boolean bothActionsCovered,
                            boolean allUncertaintyStrataCovered,
                            boolean baselineGainAcrossRequiredStrata,
                            boolean evidenceRightsIndependentlyVerified,
                            boolean realReplayEvaluationCompleted,
                            boolean tacticalPromotionAuthorized) {
        public Readiness {
            if (minimumReplaysPerStratum!=MIN_INDEPENDENT_REPLAYS
                    || minimumPairedHorizonsPerStratum!=MIN_PAIRED_HORIZONS
                    || evidenceRightsIndependentlyVerified || realReplayEvaluationCompleted
                    || tacticalPromotionAuthorized) throw invalid();
        }
    }

    /** Opaque, immutable local report: only counts, fixed category labels and
     * a split fingerprint; NO per-replay IDs, hashes, nicknames or action logs. */
    public static final class Report {
        private final String splitHash;
        private final List<Group> groups;
        private final Readiness readiness;
        private final int trainingReplays, heldOutReplays, heldOutSkippedUnknown;
        private final boolean aggregateMaskingRisk, crossReplayIdentityUsed;
        private Report(String hash,List<Group> groups,Readiness readiness,
                       int training,int heldOut,int skipped,boolean masking) {
            if (hash==null || !hash.matches("[0-9a-f]{64}")
                    || training<1 || heldOut<1 || skipped<0) throw invalid();
            this.splitHash=hash;
            this.groups=List.copyOf(groups);
            this.readiness=readiness;
            this.trainingReplays=training;
            this.heldOutReplays=heldOut;
            this.heldOutSkippedUnknown=skipped;
            this.aggregateMaskingRisk=masking;
            this.crossReplayIdentityUsed=false;
        }
        public String splitHash() { return splitHash; }
        public List<Group> groups() { return groups; }
        public Readiness readiness() { return readiness; }
        public int trainingReplays() { return trainingReplays; }
        public int heldOutReplays() { return heldOutReplays; }
        public int heldOutSkippedUnknown() { return heldOutSkippedUnknown; }
        public boolean aggregateMaskingRisk() { return aggregateMaskingRisk; }
        public boolean crossReplayIdentityUsed() { return crossReplayIdentityUsed; }
        public boolean readyForRealWorldPerformanceClaims() { return false; }
        public boolean shadowTacticalReady() { return false; }
        public boolean livePromotionAuthorized() { return false; }
        @Override public String toString() {
            return "PublicCoverageAudit[offline-counts-only;real-world-acceptance=false]";
        }
    }

    private record Score(String format,Uncertainty uncertainty,
                         int moveN,int moveHits,int moveBase,
                         int switchN,int switchHits,int switchBase,int skipped) {}
    private static final class Tally {
        int replayCount,n,hits,baseline,skipped;
        void add(Score s,Action action) {
            replayCount++;
            n+=action==Action.MOVE?s.moveN:s.switchN;
            hits+=action==Action.MOVE?s.moveHits:s.switchHits;
            baseline+=action==Action.MOVE?s.moveBase:s.switchBase;
            skipped+=s.skipped;
        }
        Group group(String format,Action action,Uncertainty u) {
            return new Group(format,action,u,replayCount,n,hits,baseline,skipped,
                    status(replayCount,n,hits,baseline));
        }
    }
    private static Gate status(int replayCount,int n,int candidate,int baseline) {
        if (replayCount==0) return Gate.NO_REPLAYS;
        if (replayCount<MIN_INDEPENDENT_REPLAYS) return Gate.TOO_FEW_INDEPENDENT_REPLAYS;
        if (n<MIN_PAIRED_HORIZONS) return Gate.TOO_FEW_PAIRED_HORIZONS;
        if (candidate<=baseline) return Gate.BASELINE_NOT_BEATEN;
        return Gate.OBSERVATIONAL_COVERAGE_ONLY;
    }

    /**
     * The ledger must have been verified under the caller's explicitly
     * trusted current public-key policy. Every train AND held-out replay is
     * re-admitted and checked against its current signed custody decision.
     * Existing Phase 5.4 overlap, privacy, private-packet and version gates
     * are recomputed; the supplied report cannot confer trust by construction.
     */
    public static Report audit(List<PublicHeldOutEvaluation.Replay> split,
                               CompetitiveFormatNormalizer.Target target,
                               PublicHeldOutEvaluation.Report submitted,
                               PublicSignedCustody.SignedManifest signed,
                               PublicSignedCustody.TrustedPolicy policy,long evaluatedAtEpochSecond) {
        if (split==null || target==null || submitted==null
                || signed==null || policy==null) throw invalid();
        var custody=PublicReplayProvenance.verifySplitWithSignedCustody(
                split,target,submitted,signed,policy,evaluatedAtEpochSecond);
        if (!custody.signedCustodyCurrentlyEligible(signed,policy,evaluatedAtEpochSecond))
            throw invalid();
        // Return a coverage report only for exactly source-pinned assumptions.
        if (!target.equals(CompetitiveFormatNormalizer.Target.pinnedSource())
                || !CobblemonMechanics.SOURCE.equals(submitted.sourceScope()))
            throw invalid();

        List<Score> scores=new ArrayList<>();
        for(var replay:split) {
            if (replay.partition()!=PublicHeldOutEvaluation.Partition.HELD_OUT) continue;
            var evidence=PublicReplayCorpus.ingest(replay.source(),replay.spectatorLog());
            var normalized=CompetitiveFormatNormalizer.normalize(evidence,target);
            var tendencies=PublicCompetitiveTendencies.analyze(replay.source(),
                    replay.spectatorLog(),target);
            var contract=PublicCompetitiveShadowContract.audit(normalized,tendencies);
            if (!contract.sameUpstreamEvidence() || !contract.offlineSchemaCompatible()
                    || contract.shadowTacticalReady()
                    || normalized.appearances().stream().anyMatch(a ->
                         a.identityLinked() || !a.identityUncertain()))
                throw invalid();
            Uncertainty uncertainty=classify(normalized,tendencies);
            PublicOpponentIntelligence intelligence=new PublicOpponentIntelligence();
            for (String line:PublicProtocolFilter.publicEvents(List.of(replay.spectatorLog()))) {
                String[] parts=line.split("\\|",-1);
                if (parts.length>2 && "p2a".equals(actorSlot(parts[2])))
                    intelligence.accept(line);
            }
            var evaluated=intelligence.predictionBenchmark();
            scores.add(new Score(evidence.format(),uncertainty,
                    evaluated.moves().evaluations(),evaluated.moves().shadowMatches(),
                    evaluated.moves().baselineMatches(),
                    evaluated.switches().evaluations(),evaluated.switches().shadowMatches(),
                    evaluated.switches().baselineMatches(),evaluated.skippedUnknown()));
        }
        // Independent, replay-level tallies; actions are never merged by actor,
        // species, nickname, or purported party-member identity.
        var groups=new ArrayList<Group>();
        for(String format:List.of("gen9ou","gen9ubers"))
            for(Action action:Action.values())
                for(Uncertainty uncertainty:Uncertainty.values()) {
                    var tally=new Tally();
                    for(Score score:scores)
                        if (score.format.equals(format)
                                && (uncertainty==Uncertainty.ALL
                                    || score.uncertainty==uncertainty))
                            tally.add(score,action);
                    groups.add(tally.group(format,action,uncertainty));
                }

        // Paired action totals must exactly match the independently recomputed
        // Phase 5.4 aggregate. No group-specific padding or invented horizons.
        for(Action action:Action.values()) {
            int n=0,hits=0,baseline=0;
            for(Group g:groups)
                if(g.action==action && g.uncertainty==Uncertainty.ALL) {
                    n+=g.pairedHorizonCount;hits+=g.candidateHits;baseline+=g.baselineHits;
                }
            var metric=action==Action.MOVE?submitted.heldOut().moves():
                    submitted.heldOut().switches();
            if (n!=metric.evaluated() || hits!=metric.candidateHits()
                    || baseline!=metric.baselineHits()) throw invalid();
        }
        int skipped=scores.stream().mapToInt(s->s.skipped).sum();
        if (skipped!=submitted.heldOut().unscoredPublicDecisions()
                || scores.size()!=submitted.heldOutReplays()) throw invalid();

        boolean formatCoverage=true,actionCoverage=true,uncertaintyCoverage=true,
                baselineGain=true,masking=false;
        for(String format:List.of("gen9ou","gen9ubers")) {
            for(Action action:Action.values()) {
                Group all=lookup(groups,format,action,Uncertainty.ALL);
                if (all.independentReplays()<MIN_INDEPENDENT_REPLAYS) formatCoverage=false;
                if (all.pairedHorizonCount()<MIN_PAIRED_HORIZONS) actionCoverage=false;
                if (!all.thresholdMet()) baselineGain=false;
                // Pooled move/switch improvement does not license a format with
                // a tie, negative score or inadequate representation.
                var global=action==Action.MOVE?submitted.heldOut().moves():
                        submitted.heldOut().switches();
                if(global.candidateHits()>global.baselineHits() && !all.thresholdMet())
                    masking=true;
                for(Uncertainty u:Uncertainty.values()) {
                    if(u==Uncertainty.ALL) continue;
                    Group group=lookup(groups,format,action,u);
                    if (!group.thresholdMet()) {
                        uncertaintyCoverage=false;
                        if(global.candidateHits()>global.baselineHits()) masking=true;
                    }
                }
            }
        }
        var readiness=new Readiness(MIN_INDEPENDENT_REPLAYS,MIN_PAIRED_HORIZONS,
                formatCoverage,actionCoverage,uncertaintyCoverage,baselineGain,
                false,false,false);
        return new Report(submitted.splitSha256(),groups,readiness,
                submitted.trainReplays(),submitted.heldOutReplays(),skipped,masking);
    }

    private static Group lookup(List<Group> groups,String format,Action action,Uncertainty u) {
        for(Group g:groups)
            if(g.format().equals(format) && g.action()==action && g.uncertainty()==u)
                return g;
        throw invalid();
    }
    private static Uncertainty classify(CompetitiveFormatNormalizer.Result norm,
                                        PublicCompetitiveTendencies.Report trends) {
        // Exclusive precedence makes all group totals auditable without
        // accidentally counting one replay as several independent samples.
        if (norm.appearances().stream().anyMatch(CompetitiveFormatNormalizer.Appearance::transformed))
            return Uncertainty.TRANSFORM_OBSERVED;
        if (norm.appearances().stream().anyMatch(a->a.observedMoves().values().stream()
                .anyMatch(t->t.sourcePresence()!=CompetitiveFormatNormalizer.Status.PINNED_SOURCE_VERIFIED)))
            return Uncertainty.UNKNOWN_MOVE_SOURCE;
        if (trends.sides().values().stream().anyMatch(s->s.repeatedDisplayCandidates()>0))
            return Uncertainty.RETURN_DISPLAY_AMBIGUOUS;
        return Uncertainty.ORDINARY_IDENTITY_UNCONFIRMED;
    }
    private static String actorSlot(String raw) {
        int colon=raw.indexOf(':');
        if(colon<0) return "";
        String slot=raw.substring(0,colon).trim();
        return slot.matches("p[12]a")?slot:"";
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Unverified offline competitive coverage");
    }
}
