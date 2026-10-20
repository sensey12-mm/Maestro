package com.maestroai.training;

import java.util.*;

/**
 * Phase 5.7: locally reconstructed offline provenance.
 *
 * Public records and hashes ARE NOT authenticity tokens. Only these sealed
 * ordinary-Java receipts are issued after re-admission and deterministic
 * reconstruction from the operator's full supplied spectator input.
 *
 * This is NOT a signature, independent replay rights check, live battle
 * attestation, or permission to promote Shadow into live control.
 */
public final class PublicReplayProvenance {
    private PublicReplayProvenance() {}

    public enum Boundary {
        RECONSTRUCTED_FROM_SUPPLIED_PUBLIC_LOG,
        RECONSTRUCTED_FROM_SUPPLIED_SPLIT,
        RIGHTS_ATTESTED_BY_OPERATOR_NOT_EXTERNALLY_VERIFIED,
        NOT_CURRENT_BATTLE_OR_RUNTIME_VERIFIED,
        NO_TACTICAL_OR_LIVE_AUTHORIZATION
    }

    /** Not a record: a caller cannot fabricate an accepted result using a public constructor. */
    public static final class CheckedReplay {
        private final CompetitiveFormatNormalizer.Result normalized;
        private final PublicCompetitiveTendencies.Report tendencies;
        private CheckedReplay(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies) {
            this.normalized = normalized;
            this.tendencies = tendencies;
        }
        /** No replay ID, digest, nickname, move list, opponent identity or probability leaks. */
        public Set<Boundary> boundaries() {
            return Set.of(Boundary.RECONSTRUCTED_FROM_SUPPLIED_PUBLIC_LOG,
                    Boundary.RIGHTS_ATTESTED_BY_OPERATOR_NOT_EXTERNALLY_VERIFIED,
                    Boundary.NOT_CURRENT_BATTLE_OR_RUNTIME_VERIFIED,
                    Boundary.NO_TACTICAL_OR_LIVE_AUTHORIZATION);
        }
        public boolean currentBattleBound() { return false; }
        public boolean independentlyAuthorized() { return false; }
        public boolean tacticalDecisionReady() { return false; }

        /** Even validated offline evidence remains blocked from tactical Shadow use. */
        public PublicCompetitiveShadowContract.Audit shadowAudit() {
            return PublicCompetitiveShadowContract.audit(normalized, tendencies);
        }
        @Override public String toString() {
            return "CheckedReplay[local-recomputation-only;rights=unverified;runtime=unverified]";
        }
    }

    /** Held-out metrics are aggregates, never a reusable actor-specific prediction. */
    public static final class CheckedSplit {
        private final int trainingReplays, evaluationReplays;
        private CheckedSplit(int trainingReplays, int evaluationReplays) {
            this.trainingReplays=trainingReplays;
            this.evaluationReplays=evaluationReplays;
        }
        public int trainingReplays() { return trainingReplays; }
        public int heldOutReplays() { return evaluationReplays; }
        public Set<Boundary> boundaries() {
            return Set.of(Boundary.RECONSTRUCTED_FROM_SUPPLIED_SPLIT,
                    Boundary.RIGHTS_ATTESTED_BY_OPERATOR_NOT_EXTERNALLY_VERIFIED,
                    Boundary.NOT_CURRENT_BATTLE_OR_RUNTIME_VERIFIED,
                    Boundary.NO_TACTICAL_OR_LIVE_AUTHORIZATION);
        }
        public boolean calibratedForecast() { return false; }
        public boolean legalActionReceipt() { return false; }
        @Override public String toString() {
            return "CheckedSplit[local-recomputation-only;rights=unverified;runtime=unverified]";
        }
    }

    /**
     * Verifies the COMPLETE chain, not individual string fields:
     * admitted public replay -> source/version normalization ->
     * per-appearance counts and switch observations -> Phase 5.6 shape audit.
     *
     * submitted records are optional from the caller's perspective only in
     * the sense that ingest() is an alternate entrypoint. Passing null to
     * verify() fails closed; generated records cannot be replaced piecemeal.
     */
    public static CheckedReplay verify(PublicReplayCorpus.Source source, String spectatorLog,
                                       CompetitiveFormatNormalizer.Target target,
                                       PublicReplayCorpus.Evidence submittedEvidence,
                                       CompetitiveFormatNormalizer.Result submittedNormalized,
                                       PublicCompetitiveTendencies.Report submittedTendencies) {
        if (source==null || spectatorLog==null || target==null || submittedEvidence==null
                || submittedNormalized==null || submittedTendencies==null) throw rejected();
        PublicReplayCorpus.Evidence accepted = PublicReplayCorpus.ingest(source, spectatorLog);
        CompetitiveFormatNormalizer.Result normalized =
                CompetitiveFormatNormalizer.normalize(accepted,target);
        PublicCompetitiveTendencies.Report tendencies =
                PublicCompetitiveTendencies.analyze(source,spectatorLog,target);
        if (!accepted.equals(submittedEvidence)
                || !normalized.equals(submittedNormalized)
                || !tendencies.equals(submittedTendencies)) throw rejected();
        return issued(normalized,tendencies);
    }

    /** Recompute and issue in one call, with no trust in externally supplied records. */
    public static CheckedReplay ingest(PublicReplayCorpus.Source source, String spectatorLog,
                                       CompetitiveFormatNormalizer.Target target) {
        if (source==null || spectatorLog==null || target==null) throw rejected();
        PublicReplayCorpus.Evidence admitted=PublicReplayCorpus.ingest(source,spectatorLog);
        CompetitiveFormatNormalizer.Result normalized =
                CompetitiveFormatNormalizer.normalize(admitted,target);
        PublicCompetitiveTendencies.Report tendencies =
                PublicCompetitiveTendencies.analyze(source,spectatorLog,target);
        return issued(normalized,tendencies);
    }

    /**
     * Public held-out report constructors are not valid proof either.
     * Reconstruct EVERY replay and group from the supplied split.
     * Reuse 5.4's duplicate replay/renamed-clone and rights gates.
     */
    public static CheckedSplit verifySplit(List<PublicHeldOutEvaluation.Replay> supplied,
                                           CompetitiveFormatNormalizer.Target target,
                                           PublicHeldOutEvaluation.Report submitted) {
        if (supplied==null || target==null || submitted==null) throw rejected();
        var reconstructed=PublicHeldOutEvaluation.evaluate(supplied,target);
        if (!reconstructed.equals(submitted)) throw rejected();
        return new CheckedSplit(reconstructed.trainReplays(), reconstructed.heldOutReplays());
    }

    private static CheckedReplay issued(CompetitiveFormatNormalizer.Result normalized,
                                        PublicCompetitiveTendencies.Report tendencies) {
        var audit=PublicCompetitiveShadowContract.audit(normalized,tendencies);
        // Out-of-scope versions do not receive a source-compatibility receipt.
        if (!audit.sameUpstreamEvidence() || !audit.offlineSchemaCompatible()
                || audit.runtimeBindingAvailable() || audit.shadowTacticalReady()
                || audit.livePromotionAuthorized()) throw rejected();
        return new CheckedReplay(normalized,tendencies);
    }

    private static IllegalArgumentException rejected() {
        // Never include untrusted replay bytes, metadata or submitted field values.
        return new IllegalArgumentException("Unverified offline public evidence");
    }
}
