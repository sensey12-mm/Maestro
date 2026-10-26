package com.maestroai.training;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * Phase 6.8: offline-only producer capability and expiry contract.
 *
 * The registry's source check and logical clock are caller-supplied synthetic
 * premises. Java object identity prevents casual in-process receipt forgery,
 * NOT replay across process restarts or cryptographic impersonation.
 * No live Cobblemon provider is registered here.
 */
public final class OfflineEvidenceProducerLease {
    private OfflineEvidenceProducerLease() {}
    public static final String SCHEMA="phase6.8.offline-producer-lease.v1";
    public static final String SOURCE=CobblemonMechanics.SOURCE;
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Phase 6.8 rejects untrusted offline producer evidence");
    }
    private static boolean token(String value) {
        return value!=null && value.matches("[0-9a-f]{64}");
    }

    /**
     * A single in-memory synthetic source domain. A withdrawn source, clock
     * rollback or callback failure permanently closes the registry. A second
     * registry, even with identical strings, has no authority over its tickets.
     */
    public static final class Registry {
        private final LongSupplier clock;
        private final BooleanSupplier sourcePresent;
        private final Set<String> allocated=new HashSet<>();
        private long lastTick=-1;
        private boolean withdrawn;
        public Registry(String sourcePin,LongSupplier logicalTick,
                        BooleanSupplier sourcePresent) {
            if(!SOURCE.equals(sourcePin) || logicalTick==null || sourcePresent==null)
                throw invalid();
            this.clock=logicalTick;
            this.sourcePresent=sourcePresent;
        }
        private long observe() {
            if(withdrawn)throw invalid();
            try {
                long now=clock.getAsLong();
                if(now<0 || now<lastTick || !sourcePresent.getAsBoolean())
                    throw invalid();
                lastTick=now;
                return now;
            } catch(RuntimeException failure) {
                withdrawn=true;
                throw invalid();
            }
        }
        public synchronized boolean withdrawn() {return withdrawn;}
        /** Mint one synthetic producer per token, even after revocation. */
        public synchronized Producer register(String syntheticToken,long leaseTicks,
                                              long maximumReceiptAge) {
            if(!token(syntheticToken) || leaseTicks<1 || leaseTicks>1_000_000
                    || maximumReceiptAge<0 || maximumReceiptAge>leaseTicks)
                throw invalid();
            long now=observe();
            if(allocated.contains(syntheticToken))throw invalid();
            final long ends;
            try { ends=Math.addExact(now,leaseTicks); }
            catch(ArithmeticException overflow) {withdrawn=true;throw invalid();}
            allocated.add(syntheticToken);
            return new Producer(this,syntheticToken,ends,maximumReceiptAge);
        }
        /** No caller can construct a ticket or swap its producer binding. */
        public synchronized OfflineRuntimeEvidenceContract.Review inspect(Ticket ticket) {
            if(ticket==null || ticket.producer.registry!=this)
                throw invalid(); // Do not poison an unrelated registry.
            Producer producer=ticket.producer;
            try {
                long now=observe();
                producer.check(now);
                if(ticket.spent || ticket.ordinal!=producer.consumed+1
                        || ticket.ordinal>producer.issued
                        || ticket.issuedTick>now
                        || now-ticket.issuedTick>producer.maximumReceiptAge)
                    throw invalid();
                var result=producer.session.inspect(ticket.envelope);
                // Revalidate the synthetic source/clock after evaluation; if
                // it withdrew in flight, no evidence is returned as accepted.
                long after=observe();
                producer.check(after);
                if(after-ticket.issuedTick>producer.maximumReceiptAge)
                    throw invalid();
                producer.consumed=ticket.ordinal;
                ticket.spent=true;
                return result; // Phase 6.7 flags remain permanently false.
            } catch(RuntimeException failure) {
                producer.revoked=true;
                throw invalid();
            }
        }
        /** Permanent shutdown, independent of sourcePresent later restoring. */
        public synchronized void withdraw() {withdrawn=true;}
    }

    /** Nonforgeable *within this JVM registry* because construction is private. */
    public static final class Producer {
        private final Registry registry;
        private final String token;
        private final long endsAt,maximumReceiptAge;
        private final OfflineRuntimeEvidenceContract.Session session;
        private boolean revoked;
        private long issued,consumed;
        private Producer(Registry registry,String token,long endsAt,long receiptAge) {
            this.registry=registry;
            this.token=token;
            this.endsAt=endsAt;
            this.maximumReceiptAge=receiptAge;
            this.session=new OfflineRuntimeEvidenceContract.Session(token);
        }
        private void check(long now) {
            if(revoked || now>=endsAt)throw invalid();
        }
        /** Tickets are numbered; skipping/reordering one will fail closed. */
        public Ticket issue(OfflineRuntimeEvidenceContract.Envelope envelope) {
            synchronized(registry) {
                try {
                    long now=registry.observe();
                    check(now);
                    if(envelope==null || !token.equals(envelope.syntheticBattleToken())
                            || envelope.sequence()!=issued+1)
                        throw invalid();
                    long next=++issued;
                    return new Ticket(this,envelope,now,next);
                } catch(RuntimeException failure) {
                    revoked=true;
                    throw invalid();
                }
            }
        }
        public void withdraw() {
            synchronized(registry) {revoked=true;}
        }
        public boolean withdrawn() {
            synchronized(registry) {return revoked || registry.withdrawn;}
        }
        @Override public String toString() {
            return "Phase68OfflineProducer[synthetic-only;authority=NONE]";
        }
    }

    /** Opaque receipt; no public payload/secret/actor getters or public ctor. */
    public static final class Ticket {
        private final Producer producer;
        private final OfflineRuntimeEvidenceContract.Envelope envelope;
        private final long issuedTick,ordinal;
        private boolean spent;
        private Ticket(Producer producer,OfflineRuntimeEvidenceContract.Envelope envelope,
                       long issuedTick,long ordinal) {
            this.producer=producer;
            this.envelope=envelope;
            this.issuedTick=issuedTick;
            this.ordinal=ordinal;
        }
        @Override public String toString() {
            return "Phase68OfflineTicket[redacted;authority=NONE]";
        }
    }
}
