package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Private issuance only: an owned entry can supplement, never rewrite, a public observation. */
abstract sealed class OwnEntryEvidence permits OwnMegaYEvidence, OwnReentryEvidence {
    final PublicFieldEvidence.Capture origin;
    final OwnRequestTransport.Lease lease;
    private final RuntimeDomain domain;
    private final PublicProtocolEvidence protocol;
    private final PublicMechanicsEvidence.Observation observation;
    private final List<String> events;
    private final Set<Integer> admitted;

    OwnEntryEvidence(PublicFieldEvidence.Capture origin, OwnRequestTransport.Lease lease,
            RuntimeDomain domain, PublicProtocolEvidence protocol,
            PublicMechanicsEvidence.Observation observation, Set<Integer> admitted) {
        this.origin=origin;this.lease=lease;this.domain=domain;this.protocol=protocol;
        this.observation=observation;this.events=protocol.mechanicsEvents();this.admitted=Set.copyOf(admitted);
    }
    final boolean current() {
        return lease.current() && protocol.complete() && !protocol.corrupted()
                && protocol.mechanicsEvents()==events && protocol.publicTurn()==lease.turn();
    }
    // Structural comparison frames retain cheap identity checks; issuance,
    // own-profile binding and every scoped operation recheck source coverage.
    final boolean sourceCurrent() { return current() && domain.scopes().get(Operation.DAMAGE).known(); }
    final boolean owns(PublicMechanicsEvidence.Observation candidate) { return candidate==observation && current(); }
    final boolean admits(int index,String line) { return admitted.contains(index) && events.get(index).equals(line); }
}
