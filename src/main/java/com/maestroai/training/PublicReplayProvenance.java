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
        private final List<PublicReplayCustody.Reference> custodyReferences;
        private final long custodyRevision;
        private final boolean signedCustodyIssued;
        private CheckedReplay(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies) {
            this(normalized,tendencies,List.of(),0,false);
        }
        private CheckedReplay(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies,
                              List<PublicReplayCustody.Reference> custodyReferences,long custodyRevision) {
            this(normalized,tendencies,custodyReferences,custodyRevision,false);
        }
        private CheckedReplay(CompetitiveFormatNormalizer.Result normalized,
                              PublicCompetitiveTendencies.Report tendencies,
                              List<PublicReplayCustody.Reference> custodyReferences,long custodyRevision,
                              boolean signedCustodyIssued) {
            this.normalized = normalized;
            this.tendencies = tendencies;
            this.custodyReferences=List.copyOf(custodyReferences);
            this.custodyRevision=custodyRevision;
            this.signedCustodyIssued=signedCustodyIssued;
        }
        /**
         * Custody is deliberately re-evaluated against an up-to-date complete
         * ledger and a caller-supplied trusted clock/revision floor. An older
         * receipt cannot bypass expiry or later revocation.
         */
        public boolean custodyCurrentlyEligible(PublicReplayCustody.Ledger ledger,
                                                long evaluatedAtEpochSecond,
                                                long requiredRevision) {
            return PublicReplayCustody.eligible(ledger,custodyReferences,
                    evaluatedAtEpochSecond,Math.max(custodyRevision,requiredRevision));
        }
        /**
         * A signed-issued receipt must be revalidated against the current
         * trusted keyring, revocation policy, checkpoint and evaluation time.
         * Unsigned Phase 5.7/5.8 receipts are NEVER upgraded by this method.
         */
        public boolean signedCustodyCurrentlyEligible(PublicSignedCustody.SignedManifest latest,
                                                      PublicSignedCustody.TrustedPolicy policy,
                                                      long evaluatedAtEpochSecond) {
            if (!signedCustodyIssued || policy==null) return false;
            try {
                var checked=PublicSignedCustody.verify(latest,policy,evaluatedAtEpochSecond);
                return PublicReplayCustody.eligible(checked.ledger(),custodyReferences,
                        evaluatedAtEpochSecond,Math.max(custodyRevision,policy.minimumRevision()));
            } catch (IllegalArgumentException invalid) {
                return false;
            }
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
        private final List<PublicReplayCustody.Reference> custodyReferences;
        private final long custodyRevision;
        private final boolean signedCustodyIssued;
        private CheckedSplit(int trainingReplays, int evaluationReplays) {
            this(trainingReplays,evaluationReplays,List.of(),0,false);
        }
        private CheckedSplit(int trainingReplays,int evaluationReplays,
                             List<PublicReplayCustody.Reference> custodyReferences,long custodyRevision) {
            this(trainingReplays,evaluationReplays,custodyReferences,custodyRevision,false);
        }
        private CheckedSplit(int trainingReplays,int evaluationReplays,
                             List<PublicReplayCustody.Reference> custodyReferences,long custodyRevision,
                             boolean signedCustodyIssued) {
            this.trainingReplays=trainingReplays;
            this.evaluationReplays=evaluationReplays;
            this.custodyReferences=List.copyOf(custodyReferences);
            this.custodyRevision=custodyRevision;
            this.signedCustodyIssued=signedCustodyIssued;
        }
        public boolean custodyCurrentlyEligible(PublicReplayCustody.Ledger ledger,
                                                long evaluatedAtEpochSecond,
                                                long requiredRevision) {
            return PublicReplayCustody.eligible(ledger,custodyReferences,
                    evaluatedAtEpochSecond,Math.max(custodyRevision,requiredRevision));
        }
        public boolean signedCustodyCurrentlyEligible(PublicSignedCustody.SignedManifest latest,
                                                      PublicSignedCustody.TrustedPolicy policy,
                                                      long evaluatedAtEpochSecond) {
            if (!signedCustodyIssued || policy==null) return false;
            try {
                var checked=PublicSignedCustody.verify(latest,policy,evaluatedAtEpochSecond);
                return PublicReplayCustody.eligible(checked.ledger(),custodyReferences,
                        evaluatedAtEpochSecond,Math.max(custodyRevision,policy.minimumRevision()));
            } catch (IllegalArgumentException invalid) {
                return false;
            }
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


    /**
     * Phase 5.8 gate: local custody approval MUST be checked in addition to
     * Phase 5.7 deterministic recomputation. This is operator-provided review
     * evidence, not an independently checked licensing or origin signature.
     */
    public static CheckedReplay ingestWithCustody(PublicReplayCorpus.Source source,
                                                 String spectatorLog,
                                                 CompetitiveFormatNormalizer.Target target,
                                                 PublicReplayCustody.Ledger ledger,
                                                 long evaluatedAtEpochSecond,
                                                 long requiredRevision) {
        var original=PublicReplayCorpus.ingest(source,spectatorLog);
        var reference=PublicReplayCustody.require(ledger,source,original,
                evaluatedAtEpochSecond,requiredRevision);
        CheckedReplay consistent=ingest(source,spectatorLog,target);
        return new CheckedReplay(consistent.normalized,consistent.tendencies,
                List.of(reference),ledger.revision());
    }

    public static CheckedReplay verifyWithCustody(PublicReplayCorpus.Source source,
                                                 String spectatorLog,
                                                 CompetitiveFormatNormalizer.Target target,
                                                 PublicReplayCorpus.Evidence evidence,
                                                 CompetitiveFormatNormalizer.Result normalized,
                                                 PublicCompetitiveTendencies.Report tendencies,
                                                 PublicReplayCustody.Ledger ledger,
                                                 long evaluatedAtEpochSecond,
                                                 long requiredRevision) {
        CheckedReplay consistent=verify(source,spectatorLog,target,evidence,normalized,tendencies);
        var reference=PublicReplayCustody.require(ledger,source,evidence,
                evaluatedAtEpochSecond,requiredRevision);
        return new CheckedReplay(consistent.normalized,consistent.tendencies,
                List.of(reference),ledger.revision());
    }

    /**
     * Full held-out split must have CURRENT custody for ALL train and test
     * records, never just a corpus aggregate or one approved example.
     */
    public static CheckedSplit verifySplitWithCustody(List<PublicHeldOutEvaluation.Replay> supplied,
                                                      CompetitiveFormatNormalizer.Target target,
                                                      PublicHeldOutEvaluation.Report submitted,
                                                      PublicReplayCustody.Ledger ledger,
                                                      long evaluatedAtEpochSecond,
                                                      long requiredRevision) {
        if (supplied==null || ledger==null || supplied.isEmpty())
            throw rejected();
        var refs=new ArrayList<PublicReplayCustody.Reference>();
        for (var replay:supplied) {
            if (replay==null) throw rejected();
            var admitted=PublicReplayCorpus.ingest(replay.source(),replay.spectatorLog());
            refs.add(PublicReplayCustody.require(ledger,replay.source(),admitted,
                    evaluatedAtEpochSecond,requiredRevision));
        }
        CheckedSplit consistent=verifySplit(supplied,target,submitted);
        return new CheckedSplit(consistent.trainingReplays,consistent.evaluationReplays,
                refs,ledger.revision());
    }


    /** Phase 5.9: require signature, caller-pinned trust and deterministic
     * public replay reconstruction before creating a signed-issued receipt. */
    public static CheckedReplay ingestWithSignedCustody(PublicReplayCorpus.Source source,
                                  String spectatorLog,CompetitiveFormatNormalizer.Target target,
                                  PublicSignedCustody.SignedManifest signed,
                                  PublicSignedCustody.TrustedPolicy policy,long evaluatedAtEpochSecond) {
        var receipt=PublicSignedCustody.verify(signed,policy,evaluatedAtEpochSecond);
        var admitted=PublicReplayCorpus.ingest(source,spectatorLog);
        var ref=PublicReplayCustody.require(receipt.ledger(),source,admitted,
                evaluatedAtEpochSecond,policy.minimumRevision());
        var checked=ingest(source,spectatorLog,target);
        return new CheckedReplay(checked.normalized,checked.tendencies,
                List.of(ref),receipt.revision(),true);
    }

    public static CheckedReplay verifyWithSignedCustody(PublicReplayCorpus.Source source,
                                  String spectatorLog,CompetitiveFormatNormalizer.Target target,
                                  PublicReplayCorpus.Evidence evidence,
                                  CompetitiveFormatNormalizer.Result normalized,
                                  PublicCompetitiveTendencies.Report tendencies,
                                  PublicSignedCustody.SignedManifest signed,
                                  PublicSignedCustody.TrustedPolicy policy,long evaluatedAtEpochSecond) {
        var receipt=PublicSignedCustody.verify(signed,policy,evaluatedAtEpochSecond);
        var checked=verify(source,spectatorLog,target,evidence,normalized,tendencies);
        var ref=PublicReplayCustody.require(receipt.ledger(),source,evidence,
                evaluatedAtEpochSecond,policy.minimumRevision());
        return new CheckedReplay(checked.normalized,checked.tendencies,
                List.of(ref),receipt.revision(),true);
    }

    /** Every source in both held-out partitions must pass current signed
     * custody, as well as Phase 5.4 duplication and provenance gates. */
    public static CheckedSplit verifySplitWithSignedCustody(
                                  List<PublicHeldOutEvaluation.Replay> supplied,
                                  CompetitiveFormatNormalizer.Target target,
                                  PublicHeldOutEvaluation.Report submitted,
                                  PublicSignedCustody.SignedManifest signed,
                                  PublicSignedCustody.TrustedPolicy policy,
                                  long evaluatedAtEpochSecond) {
        var receipt=PublicSignedCustody.verify(signed,policy,evaluatedAtEpochSecond);
        if (supplied==null || supplied.isEmpty()) throw rejected();
        var refs=new ArrayList<PublicReplayCustody.Reference>();
        for (var replay:supplied) {
            if (replay==null) throw rejected();
            var admitted=PublicReplayCorpus.ingest(replay.source(),replay.spectatorLog());
            refs.add(PublicReplayCustody.require(receipt.ledger(),replay.source(),admitted,
                    evaluatedAtEpochSecond,policy.minimumRevision()));
        }
        var checked=verifySplit(supplied,target,submitted);
        return new CheckedSplit(checked.trainingReplays,checked.evaluationReplays,
                refs,receipt.revision(),true);
    }


    /**
     * Offline durable trust file is reconstructed from a separately pinned
     * trusted bootstrap and exact latest checkpoint. Never derive trust roots
     * from the state file itself; no active battle or live promotion.
     */
    public static CheckedReplay ingestWithDurableCustody(
            PublicReplayCorpus.Source source,String spectatorLog,
            CompetitiveFormatNormalizer.Target target,java.nio.file.Path directory,
            PublicSignedCustody.TrustedPolicy independentlyTrustedBootstrap,
            PublicCustodyTrustStore.Checkpoint externallyPinned,long evaluatedAtEpochSecond) {
        var recovered=PublicCustodyTrustStore.resolve(directory,
                independentlyTrustedBootstrap,externallyPinned,evaluatedAtEpochSecond);
        return ingestWithSignedCustody(source,spectatorLog,target,recovered.manifest(),
                recovered.policy(),evaluatedAtEpochSecond);
    }

    public static CheckedSplit verifySplitWithDurableCustody(
            List<PublicHeldOutEvaluation.Replay> supplied,
            CompetitiveFormatNormalizer.Target target,PublicHeldOutEvaluation.Report submitted,
            java.nio.file.Path directory,
            PublicSignedCustody.TrustedPolicy independentlyTrustedBootstrap,
            PublicCustodyTrustStore.Checkpoint externallyPinned,long evaluatedAtEpochSecond) {
        var recovered=PublicCustodyTrustStore.resolve(directory,
                independentlyTrustedBootstrap,externallyPinned,evaluatedAtEpochSecond);
        return verifySplitWithSignedCustody(supplied,target,submitted,recovered.manifest(),
                recovered.policy(),evaluatedAtEpochSecond);
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
