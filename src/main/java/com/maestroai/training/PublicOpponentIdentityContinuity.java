package com.maestroai.training;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 6.29: read-only source-bound public appearance continuity.
 *
 * Cobblemon's original Showdown history contains private-first/public-second
 * splits and private sideupdate requests. Only the public halves are inspected,
 * and ONLY if the exact history is already accepted by the original Phase 6.16
 * source owner. Never retain an actor name, Pokémon species, UUID, raw frame,
 * private split half, replay identity, action, or party-slot hypothesis.
 *
 * Showdown p1a/p2a are PUBLIC ACTIVE POSITIONS, never stable Pokémon identities.
 * Public appearance continuity does not prove the owning native actor or a
 * Phase 5 historical opponent. No tactical consumer or live chooser uses this.
 */
public final class PublicOpponentIdentityContinuity {
    private PublicOpponentIdentityContinuity(){}
    public static final String SCHEMA="phase6.29.public-appearance-continuity.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final Pattern ACTIVE=
            Pattern.compile("p([12])([abc]): [^|\\r\\n]{1,80}");

    public enum Verdict {
        PUBLIC_POSITIONS_ONLY_UNBOUND, IDENTITY_AMBIGUOUS,
        MISSING_OR_STALE_SOURCE, SOURCE_HISTORY_MISMATCH, WRONG_BATTLE,
        UNSUPPORTED_PUBLIC_SEQUENCE, REPLAY_OR_REGRESSION, WITHDRAWN
    }
    public enum Uncertainty {
        PUBLIC_DISPLAY_ONLY,
        SLOT_REUSED_NOT_POKEMON_REENTRY,
        ILLUSION_REVEAL_UNRESOLVED,
        TRANSFORM_UNRESOLVED,
        FORM_CHANGE_UNRESOLVED,
        MULTI_ACTIVE_SLOT_UNLINKED,
        PUBLIC_EVENT_WITHOUT_PRIOR_ENTRY
    }
    private record Parsed(int p1Entries,int p2Entries,int reusedSlots,
                          int replacements,int transforms,int formChanges,
                          Set<Uncertainty> uncertainty) {
        private Parsed {
            uncertainty=Set.copyOf(uncertainty);
        }
    }

    /** Strictly numeric / categorical diagnostics, never a public actor ID. */
    public static final class Report {
        private final Session owner;
        private final PublicSplitRoutingGate.Report source;
        private final Verdict verdict;
        private final long revision;
        private final int chunks,turn,p1Entries,p2Entries,reusedSlots;
        private final int replacements,transforms,formChanges;
        private final Set<Uncertainty> uncertainty;

        private Report(Session owner,Verdict verdict,
                       PublicSplitRoutingGate.Report source,Parsed parsed){
            this.owner=owner;this.verdict=verdict;
            this.source=verdict==Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND
                    || verdict==Verdict.IDENTITY_AMBIGUOUS ? source : null;
            revision=this.source==null?0:source.revision();
            chunks=this.source==null?0:source.chunkCount();
            turn=this.source==null?0:source.publicTurn();
            p1Entries=parsed==null?0:parsed.p1Entries();
            p2Entries=parsed==null?0:parsed.p2Entries();
            reusedSlots=parsed==null?0:parsed.reusedSlots();
            replacements=parsed==null?0:parsed.replacements();
            transforms=parsed==null?0:parsed.transforms();
            formChanges=parsed==null?0:parsed.formChanges();
            uncertainty=parsed==null?Set.of():parsed.uncertainty();
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public int publicTurn(){return turn;}
        public int originalHistoryChunks(){return chunks;}
        public long sourceRevision(){return revision;}
        public int publicP1Entries(){return p1Entries;}
        public int publicP2Entries(){return p2Entries;}
        public int reusedActivePositions(){return reusedSlots;}
        public int replaceReveals(){return replacements;}
        public int transformEvents(){return transforms;}
        public int formChangeEvents(){return formChanges;}
        public Set<Uncertainty> uncertainty(){return uncertainty;}

        /** Only source-history freshness, explicitly NOT stable identity. */
        public boolean current(){
            synchronized(owner){
                return source!=null && !owner.withdrawn
                        && owner.battle!=null && source.sameBattle(owner.battle)
                        && owner.revision==revision && owner.chunks==chunks
                        && owner.turn==turn;
            }
        }
        public boolean currentPublicAppearanceObserved(){
            return current() && p1Entries+p2Entries>0;
        }
        public boolean samePhysicalBattle(Object physicalBattle){
            return current() && physicalBattle!=null && owner.battle==physicalBattle;
        }
        /** Public side labels are NOT authenticated opponent-side selection. */
        public boolean opponentSideAuthenticated(){return false;}
        public boolean nativeActorIdentityAuthenticated(){return false;}
        public boolean persistentPokemonIdentityConfirmed(){return false;}
        public boolean samePokemonAcrossSwitchesProven(){return false;}
        public boolean illusionOrTransformResolved(){return false;}
        public boolean historicalReplayAppearanceJoined(){return false;}
        public boolean legalActionSetProven(){return false;}
        public boolean phase5OnlineAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}

        /** May classify uncertainty for Phase 6.28, never live actor identity. */
        public ShadowCompetitiveRuntimeBridge.IdentityEvidence shadowOnlyUncertainty(){
            if(!current())return ShadowCompetitiveRuntimeBridge.IdentityEvidence.UNKNOWN;
            if(uncertainty.contains(Uncertainty.ILLUSION_REVEAL_UNRESOLVED)
                    || uncertainty.contains(Uncertainty.TRANSFORM_UNRESOLVED))
                return ShadowCompetitiveRuntimeBridge.IdentityEvidence
                        .ILLUSION_OR_TRANSFORM_UNRESOLVED;
            return ShadowCompetitiveRuntimeBridge.IdentityEvidence.PUBLIC_DISPLAY_ONLY;
        }
        @Override public String toString(){
            return "Phase629PublicContinuity[verdict="+verdict
                    +";revision="+revision+";turn="+turn+";chunks="+chunks
                    +";p1Entries="+p1Entries+";p2Entries="+p2Entries
                    +";reusedPositions="+reusedSlots
                    +";revealEvents="+replacements
                    +";transformEvents="+transforms
                    +";formChanges="+formChanges
                    +";opponentActor=UNKNOWN;identity=UNBOUND;authority=NONE]";
        }
    }

    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.29 unsupported public sequence");
    }
    private static int slot(String actor){
        if(actor==null)throw invalid();
        Matcher m=ACTIVE.matcher(actor);
        if(!m.matches())throw invalid();
        return (m.group(1).equals("1")?0:3)
                +(m.group(2).charAt(0)-'a');
    }

    /**
     * Parses only the independent PUBLIC extraction. The private half/requests
     * are never placed in the public event list. No actor names survive.
     */
    private static Parsed read(List<String> exactOriginal){
        var events=PublicProtocolFilter.publicEvents(exactOriginal);
        var routing=PublicEnvelopeProvenance.reconcile(exactOriginal,events);
        if(!routing.complete() || routing.origins().size()!=events.size())
            throw invalid();
        boolean[] slots=new boolean[6];
        int[] entries=new int[2];
        int reused=0,revealed=0,transformed=0,forms=0;
        EnumSet<Uncertainty> risks=EnumSet.of(Uncertainty.PUBLIC_DISPLAY_ONLY);
        for(String line:events){
            if(line==null || line.length()>1024 || !line.startsWith("|"))
                throw invalid();
            String[] fields=line.split("\\|",-1);
            if(fields.length<3 || !fields[0].isEmpty())throw invalid();
            String event=fields[1];
            if(event.equals("switch") || event.equals("drag")){
                if(fields.length!=5)throw invalid();
                int pos=slot(fields[2]),side=pos/3;
                if(slots[pos]){
                    reused++;
                    risks.add(Uncertainty.SLOT_REUSED_NOT_POKEMON_REENTRY);
                }
                slots[pos]=true;
                entries[side]++;
                if(pos%3!=0)risks.add(Uncertainty.MULTI_ACTIVE_SLOT_UNLINKED);
            }else if(event.equals("replace")
                    || event.equals("-transform")
                    || event.equals("detailschange")
                    || event.equals("-formechange")){
                if(fields.length<4)throw invalid();
                int pos=slot(fields[2]);
                if(!slots[pos])risks.add(Uncertainty.PUBLIC_EVENT_WITHOUT_PRIOR_ENTRY);
                if(event.equals("replace")){
                    revealed++;
                    risks.add(Uncertainty.ILLUSION_REVEAL_UNRESOLVED);
                }else if(event.equals("-transform")){
                    transformed++;
                    risks.add(Uncertainty.TRANSFORM_UNRESOLVED);
                }else{
                    forms++;
                    risks.add(Uncertainty.FORM_CHANGE_UNRESOLVED);
                }
            }else if(event.equals("-ability") || event.equals("-end")){
                if(fields.length>=4 && fields[3].equalsIgnoreCase("Illusion")){
                    int pos=slot(fields[2]);
                    if(!slots[pos])risks.add(Uncertainty.PUBLIC_EVENT_WITHOUT_PRIOR_ENTRY);
                    risks.add(Uncertainty.ILLUSION_REVEAL_UNRESOLVED);
                }
            }
        }
        if(entries[0]+entries[1]==0)
            throw invalid(); // no appearance to track
        return new Parsed(entries[0],entries[1],reused,revealed,
                transformed,forms,risks);
    }

    /** One native physical battle, monotonically advancing original source. */
    public static final class Session {
        private Object battle;
        private long revision;
        private int chunks=-1,turn=-1;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;battle=null;
        }
        /** Lifecycle-only; a late foreign battle end cannot withdraw this one. */
        public synchronized boolean battleEnded(Object endedBattle){
            if(endedBattle==null || battle!=endedBattle)return false;
            withdraw();
            return true;
        }
        private Report reply(Verdict verdict){
            return new Report(this,verdict,null,null);
        }
        private Report fail(Verdict verdict){
            withdraw();return reply(verdict);
        }
        public synchronized Report inspect(Object originalBattle,
                PublicSplitRoutingGate.Report originalSource,
                List<String> originalHistory){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(originalBattle==null || battle!=null && battle!=originalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(originalSource==null || originalHistory==null)
                return revision>0
                        ?fail(Verdict.MISSING_OR_STALE_SOURCE)
                        :reply(Verdict.MISSING_OR_STALE_SOURCE);
            if(!SOURCE.equals(originalSource.source())
                    || !originalSource.current())
                return fail(Verdict.MISSING_OR_STALE_SOURCE);
            if(!originalSource.sameBattle(originalBattle))
                return fail(Verdict.WRONG_BATTLE);
            if(!originalSource.matchesOriginalHistory(originalBattle,originalHistory))
                return fail(Verdict.SOURCE_HISTORY_MISMATCH);
            if(originalSource.revision()<=revision
                    || originalSource.chunkCount()<chunks
                    || originalSource.publicTurn()<turn)
                return fail(Verdict.REPLAY_OR_REGRESSION);
            try{
                var snapshot=List.copyOf(originalHistory);
                var parsed=read(snapshot);
                // A source racing ahead mid-read must NOT become a receipt.
                if(!originalSource.matchesOriginalHistory(originalBattle,snapshot)
                        || !originalSource.current())
                    return fail(Verdict.MISSING_OR_STALE_SOURCE);
                battle=originalBattle;
                revision=originalSource.revision();
                chunks=originalSource.chunkCount();
                turn=originalSource.publicTurn();
                boolean ambiguous=parsed.uncertainty().stream()
                        .anyMatch(r->r!=Uncertainty.PUBLIC_DISPLAY_ONLY);
                return new Report(this,ambiguous
                        ?Verdict.IDENTITY_AMBIGUOUS
                        :Verdict.PUBLIC_POSITIONS_ONLY_UNBOUND,originalSource,parsed);
            }catch(RuntimeException unsafe){
                return fail(Verdict.UNSUPPORTED_PUBLIC_SEQUENCE);
            }
        }
    }
}
