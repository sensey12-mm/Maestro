package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.*;

/**
 * Payload-free, advisory-only prerequisite telemetry. Do not feed this to
 * the live chooser: it is deliberately NOT a promotion/readiness certificate.
 */
public final class ShadowPrerequisiteDiagnostics {
    private ShadowPrerequisiteDiagnostics() {}
    private static final Set<String> PUBLIC_REASON_CODES = Set.of(
            "SOURCE_REGISTRY_OR_DISPATCH_UNVERIFIED", "OWN_REQUEST_LEASE_UNVERIFIED",
            "PUBLIC_ENVELOPE_UNVERIFIED", "FIELD_OR_ACTOR_EVIDENCE_UNVERIFIED",
            "CANDIDATE_COVERAGE_UNVERIFIED", "OUTCOME_INTERVALS_UNKNOWN",
            "NO_STRICT_SOURCE_SUPPORTED_PREFERENCE");
    public record Summary(boolean sourceAndRegistryCurrent, boolean ownChoiceFresh,
                          boolean publicEnvelopeComplete, boolean fieldCurrent,
                          boolean allCandidatesCovered, int candidateCount,
                          int boundedCandidates, boolean preferred,
                          Set<String> blockers) {
        public Summary {
            blockers = Set.copyOf(blockers);
            if (!PUBLIC_REASON_CODES.containsAll(blockers) || candidateCount < 0
                    || boundedCandidates < 0 || boundedCandidates > candidateCount)
                throw new IllegalArgumentException("nonpublic prerequisite diagnostics");
        }
        public String safeLine() {
            // Only fixed code-generated labels/booleans/counters; absolutely
            // no actor IDs, requests, side payloads, moves or held-item names.
            return "phase333-shd sourceRegistry=" + sourceAndRegistryCurrent
                    + " ownChoice=" + ownChoiceFresh
                    + " publicEnvelope=" + publicEnvelopeComplete
                    + " field=" + fieldCurrent
                    + " candidateCoverage=" + allCandidatesCovered
                    + " boundedCandidates=" + boundedCandidates + "/" + candidateCount
                    + " preferredConditional=" + preferred
                    + " blockers=" + new TreeSet<>(blockers)
                    + " advisoryOnly=true livePromotion=false";
        }
    }
    public static Summary inspect(RuntimeDomain domain, OwnRequestTransport.Lease lease,
                                  ScopedExecution execution, PublicProtocolEvidence protocol,
                                  ShadowMechanicsEvaluation.Comparison comparison) {
        var reasons = new TreeSet<String>();
        boolean registry = domain != null && domain.current() && domain.effectiveState().known();
        if (!registry) reasons.add("SOURCE_REGISTRY_OR_DISPATCH_UNVERIFIED");
        boolean owned = lease != null && lease.current();
        if (!owned) reasons.add("OWN_REQUEST_LEASE_UNVERIFIED");
        boolean spectator = protocol != null && protocol.complete()
                && protocol.framed() && !protocol.corrupted()
                && protocol.coverageUnknown().isEmpty();
        if (!spectator) reasons.add("PUBLIC_ENVELOPE_UNVERIFIED");
        boolean field = false;
        if (execution != null) {
            try {
                execution.requireCurrent(execution.field().turn());
                field = execution.unresolved().isEmpty();
            } catch (RuntimeException stale) { field = false; }
        }
        if (!field) reasons.add("FIELD_OR_ACTOR_EVIDENCE_UNVERIFIED");
        boolean complete = comparison != null && comparison.candidateCoverageComplete();
        if (!complete) reasons.add("CANDIDATE_COVERAGE_UNVERIFIED");
        int count = comparison == null ? 0 : comparison.rankings().size();
        int bounded = comparison == null ? 0 : (int) comparison.rankings().stream()
                .filter(r -> r.utility().known()).count();
        if (bounded != count || count == 0) reasons.add("OUTCOME_INTERVALS_UNKNOWN");
        boolean preferred = comparison != null && comparison.preferredConditional() != null;
        if (!preferred) reasons.add("NO_STRICT_SOURCE_SUPPORTED_PREFERENCE");
        return new Summary(registry,owned,spectator,field,complete,count,bounded,preferred,reasons);
    }
}
