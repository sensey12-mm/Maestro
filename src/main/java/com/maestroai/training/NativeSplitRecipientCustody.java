package com.maestroai.training;

import java.util.*;

/**
 * Phase 6.17: recipient-side split-delivery acceptance contract.
 *
 * This project has verified original split framing, but NOT installed
 * recipient-delivery callbacks. Therefore a live check can return ONLY
 * DELIVERY_UNOBSERVED; synthetic checks test rejection logic without
 * fabricating a real delivery attestation.
 *
 * No private/public split bytes, actual network recipients, UUIDs, or actor
 * names are retained or exposed. This is never a tactical input.
 */
public final class NativeSplitRecipientCustody {
    private NativeSplitRecipientCustody() {}
    public static final String SCHEMA="phase6.17.native-recipient-custody.v1";
    public static final String SOURCE=PublicSplitRoutingGate.SOURCE;
    private static final long MAX_SYNTHETIC_AGE=2;

    public enum Verdict {
        DELIVERY_UNOBSERVED,
        SYNTHETIC_RECIPIENT_ORDER_MATCHED,
        NO_SPLIT_SOURCE,
        SOURCE_UNAVAILABLE,
        BATTLE_MISMATCH,
        RECEIPT_MISMATCH,
        STALE_OR_REPLAYED,
        WITHDRAWN
    }
    public enum Audience { TARGET_P1, TARGET_P2, PUBLIC_SPECTATOR }
    public enum Branch { PRIVATE_FIRST, PUBLIC_SECOND }

    /**
     * Test-only: callers can mint these. They are deliberately not native
     * receipts, signatures or verified delivery acknowledgements.
     *
     * Even a perfect synthetic pair NEVER authorizes an installed recipient.
     */
    static final class SyntheticClaim {
        private final long sourceRevision,logicalTick;
        private final int ordinal;
        private final Branch branch;
        private final Audience audience;
        private final String opaqueTag;
        private SyntheticClaim(long revision,int ordinal,Branch branch,
                               Audience audience,long tick,String tag){
            sourceRevision=revision;this.ordinal=ordinal;
            this.branch=branch;this.audience=audience;
            logicalTick=tick;opaqueTag=tag;
        }
        static SyntheticClaim fixture(long revision,int ordinal,Branch branch,
                                      Audience audience,long tick,String tag){
            return new SyntheticClaim(revision,ordinal,branch,audience,tick,tag);
        }
        @Override public String toString(){
            return "Phase617SyntheticClaim[redacted;delivery=UNVERIFIED]";
        }
    }

    public static final class Report {
        private final Session owner;
        private final PublicSplitRoutingGate.Report structural;
        private final Verdict verdict;
        private final long revision;
        private final int pairs;
        private Report(Session owner,PublicSplitRoutingGate.Report structural,
                       Verdict verdict,long revision,int pairs){
            this.owner=owner;this.structural=structural;
            this.verdict=verdict;this.revision=revision;this.pairs=pairs;
        }
        /** Internal original structural lineage, not a recipient delivery proof. */
        boolean fromBattle(Object battle){
            return current() && structural.sameBattle(battle);
        }
        /** Same original structural receipt, not an equivalent count/turn. */
        boolean fromExactStructuralReport(PublicSplitRoutingGate.Report candidate){
            return current() && candidate!=null && structural==candidate;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long sourceRevision(){return revision;}
        public int structurallyMatchedPairs(){return pairs;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && owner.lastRevision==revision
                        && revision>0 && structural!=null && structural.current()
                        && (verdict==Verdict.DELIVERY_UNOBSERVED
                            || verdict==Verdict.SYNTHETIC_RECIPIENT_ORDER_MATCHED);
            }
        }
        public boolean publicPrivateSeparationStructurallyMatched(){
            return current() && structurallyMatchedPairs()>0;
        }
        public boolean nativeRecipientDeliveryAuthenticated(){return false;}
        public boolean nativeObserverDeliveryAuthenticated(){return false;}
        public boolean publicReadinessLeaseAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString(){
            return "Phase617RecipientCustody[verdict="+verdict
                    +";revision="+revision+";pairs="+pairs+";authority=NONE]";
        }
    }

    /** A new instance is required for each battle; withdrawal is terminal. */
    public static final class Session {
        private Object boundBattle;
        private long lastRevision,consumedSyntheticRevision;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){withdrawn=true;}
        private Report failed(Verdict verdict){
            withdrawn=true;
            return new Report(this,null,verdict,0,0);
        }
        private Report report(PublicSplitRoutingGate.Report source,Verdict verdict){
            return new Report(this,source,verdict,source.revision(),source.splitPairs());
        }

        /**
         * Production entrypoint: no source-attested native per-recipient
         * callback exists on the current Showdown-history hook. A structural
         * report can be present, but recipient delivery is UNOBSERVED.
         */
        public synchronized Report inspect(Object battle,
                                          PublicSplitRoutingGate.Report structure){
            if(withdrawn)return new Report(this,null,Verdict.WITHDRAWN,0,0);
            if(battle==null || structure==null || !structure.current())
                return failed(Verdict.SOURCE_UNAVAILABLE);
            if(boundBattle!=null && boundBattle!=battle)
                return failed(Verdict.BATTLE_MISMATCH);
            if(!structure.sameBattle(battle))
                return failed(Verdict.BATTLE_MISMATCH);
            if(structure.revision()<lastRevision)
                return failed(Verdict.STALE_OR_REPLAYED);
            if(structure.splitPairs()==0){
                boundBattle=battle;
                lastRevision=structure.revision();
                return report(structure,Verdict.NO_SPLIT_SOURCE);
            }
            boundBattle=battle;
            lastRevision=structure.revision();
            return report(structure,Verdict.DELIVERY_UNOBSERVED);
        }

        /** Explicit synthetic-only contract tests, never called by runtime. */
        synchronized Report inspectSynthetic(Object battle,
                PublicSplitRoutingGate.Report structure,List<SyntheticClaim> claims,
                long observedTick){
            if(withdrawn)return new Report(this,null,Verdict.WITHDRAWN,0,0);
            if(battle==null || structure==null || !structure.current()
                    || !structure.sameBattle(battle))
                return failed(Verdict.SOURCE_UNAVAILABLE);
            if(boundBattle!=null && boundBattle!=battle)
                return failed(Verdict.BATTLE_MISMATCH);
            if(observedTick<0 || structure.revision()<=consumedSyntheticRevision)
                return failed(Verdict.STALE_OR_REPLAYED);
            int count=structure.splitPairs();
            if(count<1 || claims==null || claims.size()!=count*2)
                return failed(Verdict.RECEIPT_MISMATCH);
            Set<String> used=new HashSet<>();
            for(int i=0;i<count;i++){
                Audience target;
                String sourceTarget=structure.targetAt(i);
                if(sourceTarget.equals("p1"))target=Audience.TARGET_P1;
                else if(sourceTarget.equals("p2"))target=Audience.TARGET_P2;
                else return failed(Verdict.RECEIPT_MISMATCH);
                SyntheticClaim first=claims.get(i*2),second=claims.get(i*2+1);
                if(!matches(first,structure.revision(),i*2+1,
                        Branch.PRIVATE_FIRST,target,observedTick,used)
                        || !matches(second,structure.revision(),i*2+2,
                                Branch.PUBLIC_SECOND,Audience.PUBLIC_SPECTATOR,
                                observedTick,used))
                    return failed(Verdict.RECEIPT_MISMATCH);
            }
            if(!structure.current())
                return failed(Verdict.STALE_OR_REPLAYED);
            boundBattle=battle;
            lastRevision=structure.revision();
            consumedSyntheticRevision=structure.revision();
            return report(structure,Verdict.SYNTHETIC_RECIPIENT_ORDER_MATCHED);
        }
        private static boolean matches(SyntheticClaim claim,long revision,int ordinal,
                Branch expectedBranch,Audience expectedAudience,long now,
                Set<String> used){
            return claim!=null && claim.sourceRevision==revision
                    && claim.ordinal==ordinal && claim.branch==expectedBranch
                    && claim.audience==expectedAudience
                    && claim.logicalTick>=0 && claim.logicalTick<=now
                    && now-claim.logicalTick<=MAX_SYNTHETIC_AGE
                    && claim.opaqueTag!=null
                    && claim.opaqueTag.matches("[0-9a-f]{64}")
                    && used.add(claim.opaqueTag);
        }
    }
}
