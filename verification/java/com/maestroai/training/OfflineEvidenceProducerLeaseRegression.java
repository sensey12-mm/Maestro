package com.maestroai.training;

import java.util.*;
import java.util.concurrent.atomic.*;

/** Adversarial synthetic Phase 6.8 producer-trust/lease regression. */
final class OfflineEvidenceProducerLeaseRegression {
    private OfflineEvidenceProducerLeaseRegression() {}
    private static int checks;
    private static final String A="a".repeat(64), B="b".repeat(64);
    private static void check(boolean ok,String reason) {
        if(!ok)throw new AssertionError("Phase 6.8: "+reason);
        checks++;
    }
    private static void reject(Runnable r,String reason) {
        try {r.run();}
        catch(IllegalArgumentException ex) {
            check("Phase 6.8 rejects untrusted offline producer evidence".equals(ex.getMessage()),
                  reason+" error redacted");
            return;
        }
        throw new AssertionError("Phase 6.8 accepted "+reason);
    }
    private static OfflineRuntimeEvidenceContract.Envelope event(String token,long seq) {
        var original=OfflineShadowResponseScenariosRegression.input(
                OfflineShadowResponseScenariosRegression.frames("Scratch"),
                OfflineShadowResponseScenariosRegression.snapshot(
                    1,List.of("tackle"),List.of("scratch"),List.of()));
        var legal=List.of("move:tackle");
        return new OfflineRuntimeEvidenceContract.Envelope(
                OfflineRuntimeEvidenceContract.SCHEMA,CobblemonMechanics.SOURCE,
                token,seq,1,1,"p1a","p2a",legal,
                OfflineRuntimeEvidenceContract.actionFingerprint(legal),original);
    }
    private static OfflineEvidenceProducerLease.Registry scope(
            AtomicLong tick,AtomicBoolean source) {
        return new OfflineEvidenceProducerLease.Registry(
                CobblemonMechanics.SOURCE,tick::get,source::get);
    }
    static void run() {
        var ticks=new AtomicLong();
        var up=new AtomicBoolean(true);
        var registry=scope(ticks,up);
        var producer=registry.register(A,10,2);
        var first=producer.issue(event(A,1));
        var receipt=registry.inspect(first);
        check(receipt.syntheticSequence()==1 && receipt.syntheticConsistencyOnly()
                && !receipt.runtimeActorBound() && !receipt.runtimeTurnFreshnessAttested()
                && !receipt.installedLegalActionsAttested()
                && !receipt.opponentResponseSetComplete()
                && !receipt.shadowSearchAuthorized()
                && !receipt.tacticalDecisionAuthorized()
                && !receipt.livePromotionAuthorized(),
                "source-scoped ticket does not imply live authority");
        check(!first.toString().contains(A) && !producer.toString().contains(A)
                && !receipt.toString().contains(A),"opaque public diagnostics");
        ticks.incrementAndGet();
        var second=producer.issue(event(A,2));
        check(registry.inspect(second).syntheticSequence()==2 && !producer.withdrawn(),
                "in-order producer continuation");
        reject(()->registry.register(A,10,2),"token reuse");
        reject(()->registry.inspect(second),"one-time receipt replay");
        check(producer.withdrawn(),"replay revokes current producer permanently");
        reject(()->producer.issue(event(A,3)),"consumed replay not restored");
        reject(()->registry.register(A,10,2),"revoked token cannot be reissued");

        var clock2=new AtomicLong();
        var present2=new AtomicBoolean(true);
        var local=scope(clock2,present2);
        var foreign=scope(clock2,present2);
        var localP=local.register(A,5,3);
        var foreignP=foreign.register(A,5,3);
        var localTicket=localP.issue(event(A,1));
        reject(()->foreign.inspect(localTicket),"same-token cross-registry receipt");
        check(!foreign.withdrawn() && !foreignP.withdrawn(),
                "foreign receipt cannot withdraw unrelated scope");
        check(local.inspect(localTicket).syntheticSequence()==1
                && foreign.inspect(foreignP.issue(event(A,1))).syntheticSequence()==1,
                "each registry accepts its own capability only");

        var twoBattle=scope(new AtomicLong(),new AtomicBoolean(true));
        var battleA=twoBattle.register(A,10,4);
        var battleB=twoBattle.register(B,10,4);
        check(twoBattle.inspect(battleA.issue(event(A,1))).syntheticSequence()==1
                && twoBattle.inspect(battleB.issue(event(B,1))).syntheticSequence()==1,
                "two synthetic battle scopes independent");
        reject(()->battleA.issue(event(B,2)),"cross-battle envelope injection");
        check(battleA.withdrawn() && !battleB.withdrawn(),
                "spoof revokes offender but not other battle");
        check(twoBattle.inspect(battleB.issue(event(B,2))).syntheticSequence()==2,
                "unrelated battle survives");

        var ageClock=new AtomicLong();
        var age=scope(ageClock,new AtomicBoolean(true));
        var ageP=age.register(A,10,1);
        var pending=ageP.issue(event(A,1));
        ageClock.set(2);
        reject(()->age.inspect(pending),"stale issued ticket");
        check(ageP.withdrawn(),"stale ticket permanently revokes source");
        var ttlClock=new AtomicLong();
        var ttl=scope(ttlClock,new AtomicBoolean(true));
        var ttlP=ttl.register(A,3,3);
        var expires=ttlP.issue(event(A,1));
        ttlClock.set(3);
        reject(()->ttl.inspect(expires),"lease expires at fixed logical time");
        check(ttlP.withdrawn(),"expired unspent ticket cannot revive");
        var manual=scope(new AtomicLong(),new AtomicBoolean(true));
        var manualP=manual.register(A,5,2);
        manualP.withdraw();
        reject(()->manualP.issue(event(A,1)),"explicit producer revocation");

        var sourceClock=new AtomicLong();
        var alive=new AtomicBoolean(true);
        var source=scope(sourceClock,alive);
        var sourceP=source.register(A,10,3);
        var sourceTicket=sourceP.issue(event(A,1));
        alive.set(false);
        reject(()->source.inspect(sourceTicket),"source withdrew before inspection");
        alive.set(true);
        reject(()->sourceP.issue(event(A,2)),"restored source cannot revive lease");
        check(source.withdrawn(),"source withdrawal is irreversible");

        var rollbackClock=new AtomicLong(3);
        var rollback=scope(rollbackClock,new AtomicBoolean(true));
        var rollbackP=rollback.register(A,12,4);
        rollback.inspect(rollbackP.issue(event(A,1)));
        rollbackClock.set(2);
        reject(()->rollbackP.issue(event(A,2)),"logical clock rollback");
        rollbackClock.set(4);
        reject(()->rollbackP.issue(event(A,2)),"restored clock cannot revive registry");
        check(rollback.withdrawn(),"time rollback permanently poisons registry");

        var outOfOrder=scope(new AtomicLong(),new AtomicBoolean(true));
        var outP=outOfOrder.register(A,10,10);
        outP.issue(event(A,1));
        var outSecond=outP.issue(event(A,2));
        reject(()->outOfOrder.inspect(outSecond),"out-of-order receipt consumption");
        check(outP.withdrawn(),"out-of-order evidence fails closed");

        var seqScope=scope(new AtomicLong(),new AtomicBoolean(true));
        var seqP=seqScope.register(A,10,10);
        reject(()->seqP.issue(event(A,2)),"skipped producer sequence");
        var pin=CobblemonMechanics.SOURCE;
        reject(()->new OfflineEvidenceProducerLease.Registry(
                pin+"-unverified",()->0L,()->true),"unpinned source revision");
        reject(()->scope(new AtomicLong(),new AtomicBoolean(true))
                .register("PRIVATE_ACTOR",4,2),"private-name instead of synthetic token");

        // The external boolean may change inside a verification call.
        var callbackCalls=new AtomicInteger();
        var conditional=new OfflineEvidenceProducerLease.Registry(
                pin,()->0L,()->callbackCalls.incrementAndGet()<4);
        var callbackProducer=conditional.register(A,10,2);
        var callbackTicket=callbackProducer.issue(event(A,1));
        reject(()->conditional.inspect(callbackTicket),
                "producer source withdrawn during post-evaluation recheck");
        check(conditional.withdrawn(),"in-flight withdrawal cannot issue review");

        // Original Phase 6.7 privacy/provenance parser still applies.
        var badFrames=new ArrayList<>(
                OfflineShadowResponseScenariosRegression.frames("Scratch"));
        badFrames.set(3,"update\n|move|p2a: PRIVATE_SECRET|Scratch|p1a: Own");
        var badSource=OfflineShadowResponseScenariosRegression.input(
                badFrames,OfflineShadowResponseScenariosRegression.snapshot(
                    1,List.of("tackle"),List.of("scratch"),List.of()));
        var legal=List.of("move:tackle");
        var bad=new OfflineRuntimeEvidenceContract.Envelope(
                OfflineRuntimeEvidenceContract.SCHEMA,pin,A,1,1,1,
                "p1a","p2a",legal,
                OfflineRuntimeEvidenceContract.actionFingerprint(legal),badSource);
        var proof=scope(new AtomicLong(),new AtomicBoolean(true));
        var prover=proof.register(A,10,2);
        reject(()->proof.inspect(prover.issue(bad)),
                "same-slot actor spoof within synthetically minted evidence");
        check(prover.withdrawn(),"invalid original public frame revokes producer");

        System.out.println("Phase 6.8 offline producer leases PASS: "+checks+" checks");
    }
}
