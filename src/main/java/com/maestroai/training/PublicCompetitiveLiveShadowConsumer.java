package com.maestroai.training;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 5 online Shadow-only public-evidence consumer, v2.
 *
 * The original signed-replay/offline Phase 5 consumer (v1) is unchanged.
 * This runtime path reads ONLY original Cobblemon public-history receipts
 * supported by native Phase 6.24/6.29/6.31 source identity. A displayed
 * active position is not a Pokemon, team, moveset or historical replay join.
 * Source-approved public moves can establish observed capabilities, but
 * never a future response probability or an unrevealed build hypothesis.
 *
 * No move choice, Shadow search input, private packet or promotion API.
 */
public final class PublicCompetitiveLiveShadowConsumer {
    private PublicCompetitiveLiveShadowConsumer(){}
    public static final int VERSION=2;
    public static final String SCHEMA="phase5.online-shadow-public-evidence.v2";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    public enum Purpose {
        SHADOW_PUBLIC_DIAGNOSTICS, OPPONENT_RESPONSE_PREDICTION,
        LIVE_ACTION_SELECTION, PRIVATE_OPPONENT_INTELLIGENCE
    }
    public enum Verdict {
        PUBLICLY_OBSERVED_CAPABILITY_ONLY,
        MISSING_SOURCE, SOURCE_STALE, SOURCE_CUSTODY_MISMATCH,
        WRONG_BATTLE, IDENTITY_UNCERTAIN, INCOMPATIBLE_PIN,
        NO_NEW_PUBLIC_EVIDENCE, REPLAY_OR_REGRESSION,
        UNSUPPORTED_PUBLIC_EVENT, WITHDRAWN
    }
    /** These are capabilities OBSERVED in public moves, never a build forecast. */
    public enum Capability {
        HAZARD_SET_OBSERVED, HAZARD_REMOVAL_OBSERVED,
        SETUP_OBSERVED, PIVOT_OBSERVED, RECOVERY_OBSERVED,
        FORCED_SWITCH_MOVE_OBSERVED, MOVE_CONTROL_OBSERVED,
        DEFENSE_OBSERVED, OTHER_VERIFIED_EFFECT_OBSERVED,
        PUBLIC_MOVE_EFFECT_UNKNOWN
    }
    /**
     * Conditional interpretation of an observed move on the current public
     * side ONLY. Not a future response prediction or a historical build join.
     */
    public enum RoleHypothesis {
        HAZARD_SETTER_POSSIBLE, HAZARD_REMOVER_POSSIBLE,
        SETUP_USER_POSSIBLE, PIVOT_USER_POSSIBLE,
        SUSTAIN_USER_POSSIBLE, CONTROL_USER_POSSIBLE
    }
    private static Set<RoleHypothesis> roles(Set<Capability> witnessed){
        var roles=EnumSet.noneOf(RoleHypothesis.class);
        if(witnessed.contains(Capability.HAZARD_SET_OBSERVED))
            roles.add(RoleHypothesis.HAZARD_SETTER_POSSIBLE);
        if(witnessed.contains(Capability.HAZARD_REMOVAL_OBSERVED))
            roles.add(RoleHypothesis.HAZARD_REMOVER_POSSIBLE);
        if(witnessed.contains(Capability.SETUP_OBSERVED))
            roles.add(RoleHypothesis.SETUP_USER_POSSIBLE);
        if(witnessed.contains(Capability.PIVOT_OBSERVED))
            roles.add(RoleHypothesis.PIVOT_USER_POSSIBLE);
        if(witnessed.contains(Capability.RECOVERY_OBSERVED))
            roles.add(RoleHypothesis.SUSTAIN_USER_POSSIBLE);
        if(witnessed.contains(Capability.MOVE_CONTROL_OBSERVED))
            roles.add(RoleHypothesis.CONTROL_USER_POSSIBLE);
        return Set.copyOf(roles);
    }
    public record Request(int version,String installedSource,
                          PublicCompetitiveConsumerContract.ContractPin offlinePin,
                          Purpose purpose,boolean useHiddenOpponentState,
                          boolean historicalAppearanceJoin,boolean tacticalUse) {
        public Request {
            if(version!=VERSION || !SOURCE.equals(installedSource)
                    || offlinePin==null
                    || !PublicCompetitiveConsumerContract.ContractPin.pinned().equals(offlinePin)
                    || purpose!=Purpose.SHADOW_PUBLIC_DIAGNOSTICS
                    || useHiddenOpponentState || historicalAppearanceJoin || tacticalUse)
                throw new IllegalArgumentException("Phase 5 online diagnostic purpose/pin denied");
        }
        public static Request pinned(){
            return new Request(VERSION,SOURCE,
                    PublicCompetitiveConsumerContract.ContractPin.pinned(),
                    Purpose.SHADOW_PUBLIC_DIAGNOSTICS,false,false,false);
        }
    }
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final Object physicalBattle;
        private final PublicSplitRoutingGate.Report originalRouting;
        private final PublicOpponentIdentityContinuity.Report originalIdentity;
        private final NativeSidedDispatchReachability.Report nativeReceipt;
        private final NativeOpponentSideProvenance.Report nativeSide;
        private final NativeOpponentSideProvenance.Side opponentSide;
        private final long ordinal;
        private final int turn,historyChunks,publicMoveEvents;
        private final Set<Capability> capabilities;
        private Report(Session owner,Verdict verdict,Object battle,
                PublicSplitRoutingGate.Report routing,
                PublicOpponentIdentityContinuity.Report identity,
                NativeSidedDispatchReachability.Report nativeReceipt,
                NativeOpponentSideProvenance.Report nativeSide,
                NativeOpponentSideProvenance.Side opposite,
                int moves,Set<Capability> observed){
            this.owner=owner;this.verdict=verdict;
            boolean accept=verdict==Verdict.PUBLICLY_OBSERVED_CAPABILITY_ONLY;
            this.physicalBattle=accept?battle:null;
            originalRouting=accept?routing:null;
            originalIdentity=accept?identity:null;
            this.nativeReceipt=accept?nativeReceipt:null;
            this.nativeSide=accept?nativeSide:null;
            opponentSide=accept?opposite:NativeOpponentSideProvenance.Side.UNKNOWN;
            ordinal=accept?nativeReceipt.observedSidedReturnOrdinal():0;
            turn=accept?routing.publicTurn():0;
            historyChunks=accept?routing.chunkCount():0;
            publicMoveEvents=accept?moves:0;
            capabilities=accept?Set.copyOf(observed):Set.of();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.battle==physicalBattle
                        && physicalBattle!=null && originalRouting!=null
                        && originalIdentity!=null && nativeReceipt!=null
                        && nativeSide!=null && originalRouting.current()
                        && originalIdentity.current()
                        && originalIdentity.sharesOriginalRouting(
                                physicalBattle,originalRouting)
                        && originalIdentity.verdict()
                            ==PublicOpponentIdentityContinuity.Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                        && nativeReceipt.fromBattle(physicalBattle)
                        && nativeSide.nativeOnlyOpponentPublicSide(
                                physicalBattle,nativeReceipt)==opponentSide
                        && turn==owner.turn && historyChunks==owner.chunks
                        && ordinal==owner.ordinal;
            }
        }
        public int publiclyObservedMoveEvents(){return current()?publicMoveEvents:0;}
        public Set<Capability> observedCapabilities(){
            return current()?capabilities:Set.of();
        }
        public Set<RoleHypothesis> conditionalPublicRoleHypotheses(){
            return current()?roles(capabilities):Set.of();
        }
        public NativeOpponentSideProvenance.Side publicOpponentSide(){
            return current()?opponentSide:NativeOpponentSideProvenance.Side.UNKNOWN;
        }
        public boolean historicalBuildLinked(){return false;}
        public boolean individualPokemonIdentityProven(){return false;}
        public boolean opponentFutureResponsePredicted(){return false;}
        public boolean calibratedOpponentProbabilities(){return false;}
        public boolean hiddenItemsOrAbilitiesExposed(){return false;}
        public boolean completeInstalledLegalActions(){return false;}
        public boolean phase5OfflineConsumerModified(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean legacyDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        @Override public String toString(){
            return "Phase5OnlineShadow[verdict="+verdict
                    +";turn="+turn+";chunks="+historyChunks
                    +";publicMoveEvents="+publiclyObservedMoveEvents()
                    +";capabilities="+observedCapabilities()
                    +";conditionalRoles="+conditionalPublicRoleHypotheses()
                    +";opponentSide="+publicOpponentSide()
                    +";identity=UNKNOWN;historicalBuild=UNBOUND"
                    +";probabilities=NONE;authority=NONE]";
        }
    }
    private record Scan(int moveEvents,Set<Capability> capabilities){
        private Scan {capabilities=Set.copyOf(capabilities);}
    }
    private static Scan scan(List<String> original,
                             NativeOpponentSideProvenance.Side side){
        var events=PublicProtocolFilter.publicEvents(original);
        var provenance=PublicEnvelopeProvenance.reconcile(original,events);
        if(!provenance.complete() || provenance.origins().size()!=events.size()
                || events.size()>1024)
            throw new IllegalArgumentException("unsupported public provenance");
        String prefix=side==NativeOpponentSideProvenance.Side.P1?"p1a: ":"p2a: ";
        int moves=0;
        var categories=EnumSet.noneOf(Capability.class);
        for(String event:events){
            if(event==null || event.length()>1024)
                throw new IllegalArgumentException("unbounded public event");
            if(!event.startsWith("|move|"))continue;
            String[] parts=event.split("\\|",-1);
            if(parts.length<4 || !parts[2].startsWith(prefix))continue;
            if(parts[3].length()>80 || parts[3].isEmpty())
                throw new IllegalArgumentException("invalid public move");
            moves++;
            if(moves>256)throw new IllegalArgumentException("too many public moves");
            String move=parts[3].toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]","");
            if(!MoveEffectModel.knownEffect(move)){
                categories.add(Capability.PUBLIC_MOVE_EFFECT_UNKNOWN);
                continue;
            }
            var effect=MoveEffectModel.describe(move,"unknown","unknown",0,0,0).effect();
            switch(effect){
                case SET_HAZARD -> categories.add(Capability.HAZARD_SET_OBSERVED);
                case REMOVE_HAZARD -> categories.add(Capability.HAZARD_REMOVAL_OBSERVED);
                case BOOST_SELF -> categories.add(Capability.SETUP_OBSERVED);
                case PIVOT -> categories.add(Capability.PIVOT_OBSERVED);
                case HEAL_SELF -> categories.add(Capability.RECOVERY_OBSERVED);
                case FORCE_SWITCH -> categories.add(Capability.FORCED_SWITCH_MOVE_OBSERVED);
                case LOCK_TARGET_MOVE -> categories.add(Capability.MOVE_CONTROL_OBSERVED);
                case PROTECT -> categories.add(Capability.DEFENSE_OBSERVED);
                default -> categories.add(Capability.OTHER_VERIFIED_EFFECT_OBSERVED);
            }
        }
        return new Scan(moves,categories);
    }
    /** Keeps native callbacks before the first chooser attached to the battle. */
    public static final class BattleScope {
        private Object battle;
        private Session session=new Session();
        public synchronized Session forBattle(Object original){
            if(original==null){session.withdraw();return session;}
            if(battle!=original){
                session.withdraw();
                session=new Session();battle=original;
            }
            return session;
        }
        public synchronized boolean end(Object ended){
            return ended!=null && battle==ended && session.battleEnded(ended);
        }
        public synchronized void withdraw(){session.withdraw();}
    }

    /** One physical battle, original source issued objects only. */
    public static final class Session {
        private Object battle;
        private long ordinal;
        private int turn=-1,chunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        public synchronized boolean battleEnded(Object ended){
            if(battle==null || battle!=ended)return false;
            withdraw();return true;
        }
        private Report reply(Verdict v){
            return new Report(this,v,null,null,null,null,null,
                    NativeOpponentSideProvenance.Side.UNKNOWN,0,Set.of());
        }
        private Report fail(Verdict v){withdraw();return reply(v);}
        public synchronized Report inspect(Object physicalBattle,
                NativeSidedDispatchReachability.Report originalNative,
                NativeOpponentSideProvenance.Report originalSide,
                PublicSplitRoutingGate.Report originalRouting,
                PublicOpponentIdentityContinuity.Report originalIdentity,
                List<String> originalFullHistory,Request request){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(request==null || !Request.pinned().equals(request))
                return fail(Verdict.INCOMPATIBLE_PIN);
            if(originalNative==null || originalSide==null
                    || originalRouting==null || originalIdentity==null
                    || originalFullHistory==null)
                return battle==null?reply(Verdict.MISSING_SOURCE):fail(Verdict.MISSING_SOURCE);
            if(!originalNative.fromBattle(physicalBattle)
                    || !originalRouting.sameBattle(physicalBattle)
                    || !originalIdentity.current() || !originalSide.current())
                return fail(Verdict.SOURCE_STALE);
            if(!originalIdentity.sharesOriginalRouting(
                        physicalBattle,originalRouting)
                    || !originalRouting.matchesOriginalHistory(
                            physicalBattle,originalFullHistory))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            var opposite=originalSide.nativeOnlyOpponentPublicSide(
                    physicalBattle,originalNative);
            if(opposite==NativeOpponentSideProvenance.Side.UNKNOWN)
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(originalIdentity.verdict()!=
                    PublicOpponentIdentityContinuity.Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                    || !originalIdentity.uncertainty().equals(
                        Set.of(PublicOpponentIdentityContinuity.Uncertainty.PUBLIC_DISPLAY_ONLY))
                    || !originalIdentity.currentPublicAppearanceObserved())
                return fail(Verdict.IDENTITY_UNCERTAIN);
            if(originalNative.observedSidedReturnOrdinal()<=ordinal
                    || originalRouting.publicTurn()<turn
                    || originalRouting.chunkCount()<chunks)
                return fail(Verdict.REPLAY_OR_REGRESSION);
            if(battle!=null && originalRouting.chunkCount()==chunks
                    && originalRouting.publicTurn()==turn)
                return reply(Verdict.NO_NEW_PUBLIC_EVIDENCE);
            try{
                var scan=scan(List.copyOf(originalFullHistory),opposite);
                if(!originalRouting.matchesOriginalHistory(
                            physicalBattle,originalFullHistory)
                        || !originalIdentity.current()
                        || !originalNative.fromBattle(physicalBattle))
                    return fail(Verdict.SOURCE_STALE);
                battle=physicalBattle;
                ordinal=originalNative.observedSidedReturnOrdinal();
                turn=originalRouting.publicTurn();
                chunks=originalRouting.chunkCount();
                return new Report(this,Verdict.PUBLICLY_OBSERVED_CAPABILITY_ONLY,
                        battle,originalRouting,originalIdentity,originalNative,
                        originalSide,opposite,scan.moveEvents(),scan.capabilities());
            }catch(RuntimeException unsupported){
                return fail(Verdict.UNSUPPORTED_PUBLIC_EVENT);
            }
        }
    }
}
