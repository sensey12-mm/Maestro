package com.maestroai.training;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 6.33: privacy-safe read-only opponent PUBLIC APPEARANCE boundaries.
 *
 * NOT a current opponent Pokemon ID. Cobblemon's internal BattleMessage
 * pnxAndUuid carries a native Pokémon UUID, but that hidden identifier is NOT
 * proof that the opponent publicly observed the underlying Pokémon. Switch
 * display can be Illusion; Transform changes the presentation without
 * establishing source identity. We never retain/export those raw identifiers.
 *
 * Exact ORIGINAL Phase 6.16 history + Phase 6.29–6.32 source custody required.
 * No Phase 5 candidate consumption, actions, or live chooser integration.
 */
public final class NativeOpponentPokemonContinuity {
    private NativeOpponentPokemonContinuity(){}
    public static final String SCHEMA="phase6.33.native-opponent-pokemon-continuity.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final Pattern POSITION=
            Pattern.compile("p([12])([abc]): [^|\\r\\n]{1,160}");

    public enum Verdict {
        PUBLIC_APPEARANCE_POSITION_ONLY_UNBOUND,
        MISSING_ORIGINAL_SOURCE, MISSING_SIDE_CONTRACT,
        WRONG_BATTLE, STALE_SOURCE, SOURCE_CUSTODY_MISMATCH,
        UNRESOLVED_OPPONENT_IDENTITY, UNSUPPORTED_PUBLIC_EVENTS,
        REPLAYED_OR_REGRESSED, WITHDRAWN
    }
    public enum Uncertainty {
        PUBLIC_POSITION_ONLY_NOT_POKEMON_IDENTITY,
        SWITCH_OR_REENTRY_UNLINKED, FORCED_SWITCH_UNLINKED,
        ILLUSION_REVEAL_UNLINKED, TRANSFORM_UNLINKED,
        FORM_CHANGE_UNLINKED, MULTI_ACTIVE_UNLINKED,
        PUBLIC_EVENT_BEFORE_ENTRY, FAINT_OR_REPLACEMENT_UNLINKED
    }
    /**
     * Pure synthetic audit result is NEVER source custody. The public API
     * can only produce an accepted Report with the original private-owner
     * source reports, not by passing a caller-created FrameScan.
     */
    record FrameScan(int entries,int repeats,int forced,int reveals,
                     int transforms,int forms,int otherSlots,
                     int publicActions,Set<Uncertainty> uncertainty){
        FrameScan {uncertainty=Set.copyOf(uncertainty);}
        boolean unambiguousPositionOnly(){
            return entries==1 && repeats==0 && forced==0 && reveals==0
                    && transforms==0 && forms==0 && otherSlots==0
                    && !uncertainty.contains(Uncertainty.PUBLIC_EVENT_BEFORE_ENTRY)
                    && !uncertainty.contains(Uncertainty.FAINT_OR_REPLACEMENT_UNLINKED);
        }
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.33 public appearance evidence unsupported");
    }
    private static int position(String actor){
        if(actor==null)throw invalid();
        Matcher m=POSITION.matcher(actor);
        if(!m.matches())throw invalid();
        return (m.group(1).equals("1")?0:3)+(m.group(2).charAt(0)-'a');
    }

    /**
     * Strict parser of the PUBLIC projection. Split private-first halves and
     * sideupdate requests are excluded by the existing Phase 6.16 filter.
     * No species, nickname, UUID, HP, moveset, native actor or raw text stored.
     */
    static FrameScan scanPublicHistory(List<String> exactOriginal,
                                       NativeOpponentSideProvenance.Side opponent){
        if(exactOriginal==null || opponent==null
                || opponent==NativeOpponentSideProvenance.Side.UNKNOWN)throw invalid();
        var publicEvents=PublicProtocolFilter.publicEvents(exactOriginal);
        var reconciliation=PublicEnvelopeProvenance.reconcile(exactOriginal,publicEvents);
        if(!reconciliation.complete() ||
                reconciliation.origins().size()!=publicEvents.size())throw invalid();
        int offset=opponent==NativeOpponentSideProvenance.Side.P1?0:3;
        boolean[] entered=new boolean[3],fainted=new boolean[3];
        int entries=0,repeats=0,forced=0,reveals=0,transforms=0,forms=0;
        int extras=0,actions=0;
        EnumSet<Uncertainty> risks=EnumSet.of(
                Uncertainty.PUBLIC_POSITION_ONLY_NOT_POKEMON_IDENTITY);
        for(String event:publicEvents){
            if(event==null || event.length()>1024 || !event.startsWith("|"))
                throw invalid();
            var fields=event.split("\\|",-1);
            if(fields.length<3 || !fields[0].isEmpty())throw invalid();
            String kind=fields[1];
            boolean switchEvent=kind.equals("switch")||kind.equals("drag");
            boolean reveal=kind.equals("replace");
            boolean transform=kind.equals("-transform");
            boolean form=kind.equals("detailschange")||kind.equals("-formechange");
            boolean faint=kind.equals("faint");
            boolean move=kind.equals("move");
            boolean illusion=(kind.equals("-ability") || kind.equals("-end"))
                    && fields.length>=4
                    && fields[3].equalsIgnoreCase("Illusion");
            if(!switchEvent&&!reveal&&!transform&&!form&&!faint&&!move&&!illusion)
                continue;
            if((switchEvent||reveal||transform||form||move)&&fields.length<4)
                throw invalid();
            int slot=position(fields[2]);
            if(slot<offset || slot>=offset+3)continue;
            int index=slot-offset;
            if(index!=0 && !entered[index])extras++;
            if(switchEvent){
                if(fields.length!=5)throw invalid();
                if(entered[index]){
                    repeats++;
                    risks.add(Uncertainty.SWITCH_OR_REENTRY_UNLINKED);
                }
                entered[index]=true;fainted[index]=false;entries++;
                if(kind.equals("drag")){
                    forced++;
                    risks.add(Uncertainty.FORCED_SWITCH_UNLINKED);
                }
                if(index!=0)
                    risks.add(Uncertainty.MULTI_ACTIVE_UNLINKED);
            }else{
                if(!entered[index])
                    risks.add(Uncertainty.PUBLIC_EVENT_BEFORE_ENTRY);
                if(reveal || illusion){
                    reveals++;
                    risks.add(Uncertainty.ILLUSION_REVEAL_UNLINKED);
                }
                if(transform){
                    transforms++;
                    risks.add(Uncertainty.TRANSFORM_UNLINKED);
                }
                if(form){
                    forms++;
                    risks.add(Uncertainty.FORM_CHANGE_UNLINKED);
                }
                if(faint){
                    fainted[index]=true;
                    risks.add(Uncertainty.FAINT_OR_REPLACEMENT_UNLINKED);
                }
                if(move)actions++;
            }
        }
        if(entries==0)throw invalid();
        if(extras>0)risks.add(Uncertainty.MULTI_ACTIVE_UNLINKED);
        return new FrameScan(entries,repeats,forced,reveals,transforms,
                forms,extras,actions,risks);
    }

    /** Numeric public positions only; NOT a Pokémon ID or party-slot identity. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final ShadowLiveCompetitiveConsumerSafety.Report consumer;
        private final PublicSplitRoutingGate.Report original;
        private final long revision,nativeOrdinal;
        private final int turn,chunks;
        private final NativeOpponentSideProvenance.Side side;
        private final FrameScan scan;
        private Report(Session owner,Verdict verdict,
                ShadowLiveCompetitiveConsumerSafety.Report consumer,
                PublicSplitRoutingGate.Report original,FrameScan scan){
            this.owner=owner;this.verdict=verdict;
            boolean eligible=verdict==Verdict.PUBLIC_APPEARANCE_POSITION_ONLY_UNBOUND;
            this.consumer=eligible?consumer:null;
            this.original=eligible?original:null;
            this.scan=eligible?scan:null;
            revision=this.original==null?0:original.revision();
            nativeOrdinal=this.consumer==null?0:consumer.nativeReturnOrdinal();
            turn=this.original==null?0:original.publicTurn();
            chunks=this.original==null?0:original.chunkCount();
            side=this.consumer==null?NativeOpponentSideProvenance.Side.UNKNOWN
                    :consumer.sourceBoundOpponentSide();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long originalRevision(){return revision;}
        public long nativeCallbackOrdinal(){return nativeOrdinal;}
        public int observedTurn(){return turn;}
        public int originalHistoryChunks(){return chunks;}
        public int publicAppearanceEntries(){return scan==null?0:scan.entries();}
        public int publiclyObservedActions(){return scan==null?0:scan.publicActions();}
        public NativeOpponentSideProvenance.Side opponentPublicSide(){
            return current()?side:NativeOpponentSideProvenance.Side.UNKNOWN;
        }
        public Set<Uncertainty> uncertainty(){
            return scan==null?Set.of():scan.uncertainty();
        }
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.battle!=null
                        && consumer!=null && consumer.current()
                        && original!=null && original.sameBattle(owner.battle)
                        && consumer.sharesOriginalRouting(owner.battle,original)
                        && revision==owner.revision && nativeOrdinal==owner.ordinal
                        && turn==owner.turn && chunks==owner.chunks
                        && side!=NativeOpponentSideProvenance.Side.UNKNOWN
                        && scan!=null && scan.unambiguousPositionOnly();
            }
        }
        public boolean sourceBoundPublicAppearanceOnly(){return current();}
        public boolean publicOpponentPokemonIdentityProven(){return false;}
        public boolean samePokemonAcrossSwitchesProven(){return false;}
        public boolean samePokemonOnReentryProven(){return false;}
        public boolean illusionTrueIdentityRevealed(){return false;}
        public boolean transformSourceIdentityProven(){return false;}
        public boolean hiddenNativeUuidExportable(){return false;}
        public boolean historicalReplayAppearanceMatched(){return false;}
        public boolean historicalBuildCandidatesAvailable(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        @Override public String toString(){
            return "Phase633OpponentAppearance[verdict="+verdict
                    +";revision="+revision+";ordinal="+nativeOrdinal
                    +";turn="+turn+";chunks="+chunks
                    +";side="+opponentPublicSide()
                    +";entries="+publicAppearanceEntries()
                    +";publicActions="+publiclyObservedActions()
                    +";pokemon=UNKNOWN;reentry=UNLINKED"
                    +";historicalJoin=BLOCKED;phase5=OFFLINE_ONLY;authority=NONE]";
        }
    }
    /** Original physical battle only; absent data abstains, regressions revoke. */
    public static final class Session {
        private Object battle;
        private long revision,ordinal;
        private int turn=-1,chunks=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;battle=null;}
        public synchronized boolean battleEnded(Object ended){
            if(battle==null || battle!=ended)return false;
            withdraw();return true;
        }
        private Report reply(Verdict v){return new Report(this,v,null,null,null);}
        private Report fail(Verdict v){withdraw();return reply(v);}
        public synchronized Report inspect(Object physicalBattle,
                ShadowLiveCompetitiveConsumerSafety.Report safety,
                PublicSplitRoutingGate.Report original,
                List<String> exactOriginalHistory){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && physicalBattle!=battle)
                return fail(Verdict.WRONG_BATTLE);
            if(safety==null)
                return revision==0?reply(Verdict.MISSING_SIDE_CONTRACT)
                        :fail(Verdict.MISSING_SIDE_CONTRACT);
            if(original==null || exactOriginalHistory==null)
                return revision==0?reply(Verdict.MISSING_ORIGINAL_SOURCE)
                        :fail(Verdict.MISSING_ORIGINAL_SOURCE);
            if(!SOURCE.equals(safety.source()) || !SOURCE.equals(original.source())
                    || !ShadowLiveCompetitiveConsumerSafety.SCHEMA.equals(safety.schema())
                    || safety.verdict()!=ShadowLiveCompetitiveConsumerSafety.Verdict
                            .SOURCE_EPOCH_AUDITED_OFFLINE_ONLY
                    || safety.phase5OnlineAuthorized()
                    || safety.shadowSearchAuthorized())
                return fail(Verdict.MISSING_SIDE_CONTRACT);
            if(!safety.current() || !original.current())
                return fail(Verdict.STALE_SOURCE);
            if(!original.sameBattle(physicalBattle))
                return fail(Verdict.WRONG_BATTLE);
            if(!safety.sharesOriginalRouting(physicalBattle,original))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(!original.matchesOriginalHistory(physicalBattle,exactOriginalHistory))
                return fail(Verdict.SOURCE_CUSTODY_MISMATCH);
            if(original.revision()<=revision
                    || safety.nativeReturnOrdinal()<=ordinal
                    || original.publicTurn()<turn
                    || original.chunkCount()<chunks)
                return fail(Verdict.REPLAYED_OR_REGRESSED);
            try{
                List<String> snapshot=List.copyOf(exactOriginalHistory);
                var side=safety.sourceBoundOpponentSide();
                FrameScan parsed=scanPublicHistory(snapshot,side);
                if(!parsed.unambiguousPositionOnly())
                    return fail(Verdict.UNRESOLVED_OPPONENT_IDENTITY);
                if(!safety.sharesOriginalRouting(physicalBattle,original)
                        || !original.matchesOriginalHistory(physicalBattle,snapshot))
                    return fail(Verdict.STALE_SOURCE);
                battle=physicalBattle;revision=original.revision();
                ordinal=safety.nativeReturnOrdinal();turn=original.publicTurn();
                chunks=original.chunkCount();
                var accepted=new Report(this,
                        Verdict.PUBLIC_APPEARANCE_POSITION_ONLY_UNBOUND,
                        safety,original,parsed);
                if(!accepted.current())return fail(Verdict.STALE_SOURCE);
                return accepted;
            }catch(RuntimeException invalid){
                return fail(Verdict.UNSUPPORTED_PUBLIC_EVENTS);
            }
        }
    }
}
