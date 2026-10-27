package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 6.9: opt-in, read-only bridge from the existing Cobblemon
 * PokemonBattle.getShowdownMessages() observation point.
 *
 * The live path never constructs legal-action evidence or calls a chooser.
 * Phase 6.7/6.8 may be exercised ONLY via explicit synthetic probes using
 * original, source-compatible spectator frames. Neither path certifies runtime
 * actor identity, installed legality, delivery or Shadow tactical authority.
 */
public final class ReadOnlyShadowObservationBridge {
    public static final String SCHEMA="phase6.9.read-only-public-bridge.v1";
    public static final String COBBLEMON_VERSION="1.7.3+1.21.1";
    private static final int MAX_CHUNKS=128, MAX_CHARS=65536;
    private static final Set<String> NARROW=Set.of("switch","drag","turn","move");

    public enum State {
        SYNTHETIC_FRAME_ELIGIBLE, NO_SOURCE_VERSION_PIN, NO_SAFE_PUBLIC_FRAMES,
        REPLAY_OR_REWRITE, WRONG_BATTLE, APPEARANCE_AMBIGUOUS, WITHDRAWN
    }
    /** Redacted diagnostics only: no battle IDs, actors, frames or actions. */
    public record Observation(State state,long revision,int publicTurn,int publicEventCount) {
        public boolean currentBattleAuthenticated(){return false;}
        public boolean legalActionsAttested(){return false;}
        public boolean tacticalAuthority(){return false;}
        @Override public String toString() {
            return "Phase69PublicObservation[state="+state+";revision="+revision
                    +";turn="+publicTurn+";events="+publicEventCount+";authority=NONE]";
        }
    }
    private Object owner;
    private boolean withdrawn;
    private State reason=State.NO_SAFE_PUBLIC_FRAMES;
    private List<String> chunkDigests=List.of();
    // Contains ONLY narrow public update frames, never a sideupdate/private split.
    private List<String> eligibleFrames=List.of();
    private boolean originalNarrowOnly;
    private final AtomicLong logicalTick=new AtomicLong();
    private long revision,probedRevision;
    private int publicTurn,publicEventCount;
    private int witnessedFusedMoveCount,witnessedFusedSwitchCount;
    private boolean witnessedFusedForced;
    /**
     * A diagnostic sidecar for the exact same battle object. This never
     * certifies an executable move, supplies a chooser or promotes Shadow.
     */
    public synchronized boolean observeOwnFusedEvidence(
            Object battle, NativeOwnActionFusion.Review review) {
        if(withdrawn || owner==null || owner!=battle || review==null || !review.current()
                || review.completeLegalSetProven() || review.shadowSearchAuthorized()
                || review.livePromotionAuthorized())return false;
        witnessedFusedMoveCount=review.ownMoveCount();
        witnessedFusedSwitchCount=review.ownSwitchCandidateCount();
        witnessedFusedForced=review.mode()==NativeOwnActionFusion.Mode.FORCED_REPLACEMENT_ONLY;
        return true;
    }
    public synchronized int witnessedFusedMoveCount(){return witnessedFusedMoveCount;}
    public synchronized int witnessedFusedSwitchCount(){return witnessedFusedSwitchCount;}
    public synchronized boolean witnessedFusedForced(){return witnessedFusedForced;}

    private int witnessedOwnSwitchCount;
    private boolean witnessedForcedReplacement;
    /** Phase 6.11: read-only native-own party counter, never a legal-set grant. */
    public synchronized boolean observeOwnSwitchEvidence(
            Object battle,NativeOwnSwitchEvidence.Review evidence) {
        if(withdrawn || owner==null || owner!=battle || evidence==null
                || !evidence.currentlyObserved()) return false;
        witnessedOwnSwitchCount=evidence.observedSwitchCount();
        witnessedForcedReplacement=evidence.forcedReplacementWitnessed();
        return true;
    }
    public synchronized int witnessedOwnSwitchCount(){return witnessedOwnSwitchCount;}
    public synchronized boolean witnessedForcedReplacement(){return witnessedForcedReplacement;}

    private int witnessedOwnMoveCount;
    /**
     * Redacted sidecar readout from the owning native request/choice receipt.
     * Only observations already bound to THIS battle are recorded. This
     * cannot convert the spectator stream into a legal-action certificate.
     */
    public synchronized boolean observeOwnActionEvidence(
            Object battle, NativeOwnActionEvidence.Review evidence) {
        if(withdrawn || owner==null || owner!=battle
                || evidence==null || !evidence.currentOwnMoveAgreement())
            return false;
        witnessedOwnMoveCount=evidence.observedMoveCount();
        return true;
    }
    public synchronized int witnessedOwnMoveCount() {return witnessedOwnMoveCount;}

    /**
     * Phase 6.13 short-lived, source-issued public observation lease.  Never
     * exports battle identity or original frames; only the issuing bridge
     * can construct one.  An advancing/replaced/withdrawn history expires it.
     */
    static final class PublicSourceLease {
        private final ReadOnlyShadowObservationBridge source;
        private final Object battle;
        private final long revision;
        private final int turn,events;
        private PublicSourceLease(ReadOnlyShadowObservationBridge source,Object battle,
                long revision,int turn,int events){
            this.source=source;this.battle=battle;this.revision=revision;
            this.turn=turn;this.events=events;
        }
        /** Internal original-session provenance, never exported as a battle ID. */
        boolean fromBattle(Object candidate){
            return candidate!=null && battle==candidate && current();
        }
        long revision(){return revision;}
        int turn(){return turn;}
        int eventCount(){return events;}
        boolean current(){
            synchronized(source){
                return !source.withdrawn && source.owner==battle
                        && source.reason==State.SYNTHETIC_FRAME_ELIGIBLE
                        && source.revision==revision && source.publicTurn==turn
                        && source.publicEventCount==events
                        && source.eligibleFrames.size()==source.chunkDigests.size()
                        && events>=source.eligibleFrames.size();
            }
        }
        @Override public String toString(){
            return "Phase613PublicLease[redacted;authority=NONE]";
        }
    }
    synchronized PublicSourceLease publicLease(Object battle){
        if(withdrawn || owner==null || owner!=battle || revision<1 || publicTurn<1
                || publicEventCount<1 || reason!=State.SYNTHETIC_FRAME_ELIGIBLE
                || eligibleFrames.size()!=chunkDigests.size()
                || publicEventCount<eligibleFrames.size())
            return null;
        return new PublicSourceLease(this,battle,revision,publicTurn,publicEventCount);
    }

    private OfflineEvidenceProducerLease.Registry registry;
    private OfflineEvidenceProducerLease.Producer producer;

    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.9 public bridge abstained");
    }
    private static String hash(String input){
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException error){
            throw new IllegalStateException("source hashing unavailable");
        }
    }
    private Observation status(){
        return new Observation(reason,revision,publicTurn,publicEventCount);
    }
    /** Withdrawal is irreversible for this instance (new battles need a new bridge). */
    public synchronized void withdraw(){
        withdrawn=true;
        reason=State.WITHDRAWN;
        witnessedOwnMoveCount=0;
        witnessedFusedMoveCount=0;witnessedFusedSwitchCount=0;witnessedFusedForced=false;
        witnessedOwnSwitchCount=0;
        witnessedForcedReplacement=false;
        eligibleFrames=List.of();
        originalNarrowOnly=false;
        chunkDigests=List.of();
        if(producer!=null)producer.withdraw();
        if(registry!=null)registry.withdraw();
    }
    public synchronized boolean withdrawn(){return withdrawn;}

    /**
     * Invoked from an existing read-only sync site on the real battle object.
     * The source-version predicate is evaluated at the caller from FabricLoader.
     * It is only a compatibility gate, NOT proof of trusted event delivery.
     */
    public synchronized Observation observe(Object battle,List<String> raw,
                                           boolean installedVersionPinned){
        if(withdrawn)return status();
        if(battle==null || raw==null || (owner!=null && owner!=battle)){
            reason=State.WRONG_BATTLE;
            withdraw();
            return new Observation(State.WRONG_BATTLE,revision,publicTurn,publicEventCount);
        }
        if(owner==null)owner=battle;
        if(!installedVersionPinned){
            withdraw();
            return new Observation(State.NO_SOURCE_VERSION_PIN,revision,publicTurn,publicEventCount);
        }
        if(raw.size()>MAX_CHUNKS || raw.stream().anyMatch(Objects::isNull)
                || raw.stream().mapToLong(String::length).sum()>MAX_CHARS){
            withdraw();
            return new Observation(State.NO_SAFE_PUBLIC_FRAMES,revision,publicTurn,publicEventCount);
        }
        List<String> digests=raw.stream().map(ReadOnlyShadowObservationBridge::hash).toList();
        if(digests.size()<chunkDigests.size()
                || !digests.subList(0,chunkDigests.size()).equals(chunkDigests)){
            withdraw();
            return new Observation(State.REPLAY_OR_REWRITE,revision,publicTurn,publicEventCount);
        }
        if(digests.equals(chunkDigests))return status(); // idempotent polling

        // A missing split half or unexpected routing is never reinterpreted
        // as a spectator message. The original raw private bytes are not kept.
        var visible=PublicProtocolFilter.publicEvents(raw);
        var audit=PublicEnvelopeProvenance.reconcile(raw,visible);
        if(!audit.complete() || visible.isEmpty()
                || audit.origins().size()!=visible.size()
                || audit.origins().stream().anyMatch(o->
                    o.route()!=PublicEnvelopeProvenance.Route.DIRECT_UPDATE)){
            withdraw();
            return new Observation(State.NO_SAFE_PUBLIC_FRAMES,revision,publicTurn,publicEventCount);
        }
        final PublicBatchEnvelopeGuard.Result batch;
        final ExpandedPublicEventGuard.Result expanded;
        try {
            // Validate *all* bytes in each complete public update envelope
            // before evaluating mechanics or retaining the direct-only source.
            batch=PublicBatchEnvelopeGuard.inspect(raw);
            expanded=ExpandedPublicEventGuard.validate(batch.mechanicFrames());
        } catch (RuntimeException incomplete) {
            withdraw();
            return new Observation(State.NO_SAFE_PUBLIC_FRAMES,revision,publicTurn,publicEventCount);
        }
        // Independent source filters may not drop hidden/unrecognized lines.
        // Provenance still requires every mechanical event to be DIRECT_UPDATE.
        if(!batch.directMechanics().equals(visible)
                || expanded.turn()<publicTurn
                || expanded.events()!=batch.mechanicFrames().size()){
            withdraw();
            return new Observation(State.NO_SAFE_PUBLIC_FRAMES,revision,publicTurn,publicEventCount);
        }
        eligibleFrames=List.copyOf(raw);
        originalNarrowOnly=expanded.narrowOnly() && batch.narrowOnly();
        chunkDigests=List.copyOf(digests);
        publicTurn=expanded.turn();
        publicEventCount=batch.allEvents();
        revision++;
        logicalTick.incrementAndGet();
        reason=expanded.appearanceAmbiguous()
                ? State.APPEARANCE_AMBIGUOUS : State.SYNTHETIC_FRAME_ELIGIBLE;
        return status();
    }

    /**
     * Synthetic test harness only. The caller's own root legal-action fixture
     * is never inferred from spectator frames, and a real observation never
     * automatically invokes this method. Every review remains not-tactical.
     */
    public synchronized OfflineRuntimeEvidenceContract.Review probeSynthetic(
            Object battle,OfflineRuntimeEvidenceContract.Envelope envelope){
        if(withdrawn || !originalNarrowOnly || battle!=owner || envelope==null || revision==0
                || revision==probedRevision || reason!=State.SYNTHETIC_FRAME_ELIGIBLE)
            throw invalid();
        try{
            if(!eligibleFrames.equals(envelope.scenarios().fixture().framedPublicHistory())
                    || envelope.turn()!=publicTurn
                    || envelope.sequence()!=probedRevision+1)
                throw invalid();
            if(registry==null){
                registry=new OfflineEvidenceProducerLease.Registry(
                        CobblemonMechanics.SOURCE,logicalTick::get,()->!withdrawn);
                producer=registry.register(envelope.syntheticBattleToken(),1000,0);
            }
            var ticket=producer.issue(envelope);
            var result=registry.inspect(ticket);
            probedRevision=revision;
            return result;
        }catch(RuntimeException failure){
            withdraw();
            throw invalid(); // no payload from unsafe messages
        }
    }
}
