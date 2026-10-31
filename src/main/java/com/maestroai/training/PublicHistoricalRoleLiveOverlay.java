package com.maestroai.training;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 5 historical role overlay on live Shadow diagnostics.
 *
 * Roles come from the signed, rights-reviewed OFFLINE Phase 5.21 handoff.
 * They remain UNBOUND to the currently observed opponent Pokemon, species,
 * slot, action, ability, item or build. No private replay identifiers, raw
 * history, candidate sets or role-specific probabilities leave this object.
 *
 * Independent trusted inputs are mandatory. A caller-provided signature or
 * clock alone is not independent authority; the operator must provision and
 * continually refresh the trusted policy from outside the battle pipeline.
 */
public final class PublicHistoricalRoleLiveOverlay {
    private PublicHistoricalRoleLiveOverlay(){}
    public static final int VERSION=1;
    public static final String SCHEMA="phase5.online-historical-roles-unbound.v1";
    public enum Verdict {
        HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC,
        NO_HISTORICAL_SOURCE, NO_LIVE_SOURCE, STALE_OR_REVOKED_CUSTODY,
        SOURCE_MISMATCH, HISTORICAL_ROLE_UNSUPPORTED,
        REPLAY_OR_REGRESSION, WRONG_BATTLE, WITHDRAWN
    }
    public enum Role {
        PRIORITY_MOVE_SIGNAL_POSSIBLE,
        ATTACK_BOOST_SIGNAL_POSSIBLE,
        SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE
    }
    private static Role mapRole(String id){
        return switch(id){
            case "priority-move-signal" -> Role.PRIORITY_MOVE_SIGNAL_POSSIBLE;
            case "attack-boost-signal" -> Role.ATTACK_BOOST_SIGNAL_POSSIBLE;
            case "special-attack-boost-signal" -> Role.SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE;
            default -> null;
        };
    }
    /** An external source must supply a NEW independently trusted keyring,
     * checkpoint, revision floor and clock on each query. No cached ticket. */
    @FunctionalInterface
    public interface TrustedSource { CustodySnapshot latest(); }
    public record CustodySnapshot(PublicSignedCustody.SignedManifest signed,
                                  PublicSignedCustody.TrustedPolicy policy,
                                  long currentEpochSecond) {
        public CustodySnapshot {
            if(signed==null || policy==null || currentEpochSecond<1)
                throw new IllegalArgumentException("historical custody source unavailable");
        }
    }

    /**
     * Non-constructible archive. Re-run the FULL existing Phase 5.21 signed
     * pipeline; only extract supported ROLE signals, not caller candidates.
     * The replay source/hash is kept only for revocation eligibility checks.
     */
    public static final class Archive {
        private final PublicReplayCustody.Reference originalReplay;
        private final PublicSignedCustody.Verified originalVerified;
        private final long originalEpoch;
        private final Set<Role> roleLabels;
        private Archive(PublicReplayCustody.Reference reference,
                        PublicSignedCustody.Verified verified,
                        long epoch,Set<Role> roles){
            originalReplay=reference;originalVerified=verified;
            originalEpoch=epoch;roleLabels=Set.copyOf(roles);
        }
        Set<Role> sourceScopedUnboundRoles(){return roleLabels;}
        public boolean currentOpponentIdentified(){return false;}
        public boolean replayRightsIndependentlyVerified(){return false;}
        public boolean phase5OnlineBuildAuthorized(){return false;}
        public boolean shadowTacticalAuthorized(){return false;}
        public boolean privateDataExportable(){return false;}
        @Override public String toString(){
            return "Phase5HistoricalRoles[offline-signed;source-scoped;identity=UNBOUND;authority=NONE]";
        }
    }
    public static Archive prepare(PublicReplayCorpus.Source source,String spectatorLog,
            List<PublicBuildCandidateFilter.Candidate> candidateQueries,
            List<PublicShadowBuildHandoff.PublicObservationConstraint> constraints,
            Set<PublicFormBuildReconciliation.OverrideNotice> overrides,
            PublicSignedCustody.SignedManifest originalSigned,
            PublicSignedCustody.TrustedPolicy independentPolicy,
            long trustedCurrentEpochSecond){
        if(source==null || spectatorLog==null || candidateQueries==null
                || constraints==null || overrides==null || originalSigned==null
                || independentPolicy==null || trustedCurrentEpochSecond<1)
            throw invalid();
        // The existing immutable Phase 5.21 handoff rechecks current custody,
        // source pins, policy, proposals and original public replay evidence.
        var handoff=PublicShadowBuildHandoff.prepare(source,spectatorLog,
                candidateQueries,constraints,
                PublicSpeciesBuildLegality.SourcePin.pinned(),
                PublicFormBuildReconciliation.ResolverPin.pinned(),
                overrides,originalSigned,independentPolicy,
                trustedCurrentEpochSecond,PublicShadowBuildHandoff.Pin.pinned());
        var verified=PublicSignedCustody.verify(originalSigned,independentPolicy,
                trustedCurrentEpochSecond);
        var evidence=PublicReplayCorpus.ingest(source,spectatorLog);
        var reference=PublicReplayCustody.require(verified.ledger(),source,evidence,
                trustedCurrentEpochSecond,independentPolicy.minimumRevision());
        var roles=EnumSet.noneOf(Role.class);
        for(var appearance:handoff.appearances()){
            if(!appearance.identityUncertain()
                    || appearance.limitations().contains(
                        PublicShadowBuildHandoff.Limit.TRANSFORM_OR_DISGUISE_UNCERTAIN))
                continue;
            for(var signal:appearance.signals()){
                if(signal.kind()!=PublicOpponentBuildHypotheses.Kind.ROLE
                        || signal.grade()!=PublicShadowBuildHandoff.Grade.SOURCE_SUPPORTED_POSSIBILITY
                        || signal.publiclyObserved()
                        || signal.limitations().contains(
                            PublicShadowBuildHandoff.Limit.TRANSFORM_OR_DISGUISE_UNCERTAIN))
                    continue;
                Role mapped=mapRole(signal.id());
                if(mapped!=null)roles.add(mapped);
            }
        }
        if(roles.isEmpty())throw invalid();
        return new Archive(reference,verified,trustedCurrentEpochSecond,roles);
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 5 historical role custody or scope unsupported");
    }
    /** Historical roles never become owned by the current live opponent. */
    public static final class Report {
        private final Session owner;
        private final Verdict verdict;
        private final Object battle;
        private final PublicCompetitiveLiveShadowConsumer.Report live;
        private final Archive archive;
        private final long callbackOrdinal;
        private Report(Session owner,Verdict verdict,Object battle,
                PublicCompetitiveLiveShadowConsumer.Report live,
                Archive archive){
            this.owner=owner;this.verdict=verdict;
            boolean admitted=verdict==Verdict.HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC;
            this.battle=admitted?battle:null;
            this.live=admitted?live:null;
            this.archive=admitted?archive:null;
            callbackOrdinal=admitted?owner.callbackOrdinal:0;
        }
        public String schema(){return SCHEMA;}
        public Verdict verdict(){return verdict;}
        public boolean current(){
            synchronized(owner){
                return !owner.withdrawn && live!=null && live.fromBattle(battle)
                        && archive!=null && battle!=null && owner.battle==battle
                        && owner.archive==archive && owner.callbackOrdinal==callbackOrdinal
                        && owner.checkedCurrentCustody();
            }
        }
        /** Historical roles are SOURCE-SCOPED only, never a present battler. */
        public Set<Role> unboundHistoricalRolePossibilities(){
            return current()?archive.roleLabels:Set.of();
        }
        public Set<PublicCompetitiveLiveShadowConsumer.Capability> currentPublicCapabilities(){
            return current()?live.observedCapabilities():Set.of();
        }
        public boolean matchedHistoricalRoleToCurrentPokemon(){return false;}
        public boolean confirmedOpponentBuild(){return false;}
        public boolean calibratedOpponentResponses(){return false;}
        public boolean onlinePhase5CandidateBuildsEnabled(){return false;}
        public boolean historicSpeciesOrAppearanceJoined(){return false;}
        public boolean hiddenMovesAbilitiesOrItemsExposed(){return false;}
        public boolean shadowTacticalUseAuthorized(){return false;}
        public boolean legacyChoiceAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        public boolean privateReplayContentExportable(){return false;}
        @Override public String toString(){
            return "Phase5HistoricalRoleOverlay[verdict="+verdict
                    +";current="+current()
                    +";roles="+unboundHistoricalRolePossibilities()
                    +";presentPokemon=UNKNOWN;historicalJoin=UNBOUND"
                    +";forecast=NONE;authority=NONE]";
        }
    }
    public static final class BattleScope {
        private Object battle;
        private Session session=new Session();
        public synchronized Session forBattle(Object source){
            if(source==null){session.withdraw();return session;}
            if(battle!=source){
                session.withdraw();
                session=new Session();battle=source;
            }
            return session;
        }
        public synchronized boolean end(Object ended){
            return ended!=null && battle==ended && session.battleEnded(ended);
        }
        public synchronized void withdraw(){session.withdraw();}
    }

    public static final class Session {
        private Object battle;
        private Archive archive;
        private TrustedSource trusted;
        private long callbackOrdinal,lastCheckedAt,acceptedRevision,lastKeyringEpoch,lastMinimumRevision;
        private PublicSignedCustody.Verified lastVerified;
        private boolean withdrawn;
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;battle=null;archive=null;trusted=null;
        }
        public synchronized boolean battleEnded(Object ended){
            if(ended==null || battle!=ended)return false;
            withdraw();return true;
        }
        private Report reply(Verdict verdict){
            return new Report(this,verdict,null,null,null);
        }
        private Report fail(Verdict verdict){withdraw();return reply(verdict);}
        private boolean checkedCurrentCustody(){
            if(!currentCustody()){
                withdraw(); // no resurrection after revocation/rollback
                return false;
            }
            return true;
        }
        private boolean currentCustody(){
            if(archive==null || trusted==null)return false;
            try{
                CustodySnapshot next=trusted.latest();
                if(next==null || next.currentEpochSecond()<lastCheckedAt
                        || next.currentEpochSecond()<archive.originalEpoch
                        || next.policy().minimumRevision()<lastMinimumRevision
                        || next.policy().keyringEpoch()<lastKeyringEpoch
                        || next.policy().minimumRevision()<archive.originalVerified.revision())
                    return false;
                var checked=PublicSignedCustody.verify(next.signed(),next.policy(),
                        next.currentEpochSecond());
                long revised=checked.revision();
                if(revised<acceptedRevision)return false;
                if(revised==acceptedRevision){
                    if(!checked.digest().equals(lastVerified.digest()))return false;
                }else if(revised==acceptedRevision+1){
                    checked=PublicSignedCustody.verifySuccessor(
                            lastVerified,next.signed(),next.policy(),next.currentEpochSecond());
                }else return false; // missing append revisions: no skip/rollback
                if(!PublicReplayCustody.eligible(checked.ledger(),
                        List.of(archive.originalReplay),next.currentEpochSecond(),
                        Math.max(archive.originalVerified.revision(),
                                 next.policy().minimumRevision())))
                    return false;
                acceptedRevision=checked.revision();
                lastVerified=checked;
                lastCheckedAt=next.currentEpochSecond();
                lastKeyringEpoch=next.policy().keyringEpoch();
                lastMinimumRevision=next.policy().minimumRevision();
                return true;
            }catch(RuntimeException unavailable){return false;}
        }
        public synchronized Report inspect(Object physicalBattle,
                PublicCompetitiveLiveShadowConsumer.Report live,
                Archive originalArchive,TrustedSource source){
            if(withdrawn)return reply(Verdict.WITHDRAWN);
            if(physicalBattle==null || battle!=null && battle!=physicalBattle)
                return fail(Verdict.WRONG_BATTLE);
            if(originalArchive==null || source==null)
                return battle==null?reply(Verdict.NO_HISTORICAL_SOURCE)
                        :fail(Verdict.NO_HISTORICAL_SOURCE);
            if(live==null)
                return battle==null?reply(Verdict.NO_LIVE_SOURCE)
                        :fail(Verdict.NO_LIVE_SOURCE);
            if(!live.fromBattle(physicalBattle) || live.publicOpponentSide()
                    ==NativeOpponentSideProvenance.Side.UNKNOWN)
                return fail(Verdict.SOURCE_MISMATCH);
            if(battle!=null && (archive!=originalArchive || trusted!=source))
                return fail(Verdict.SOURCE_MISMATCH);
            if(battle!=null && live.equals(lastLive))
                return fail(Verdict.REPLAY_OR_REGRESSION);
            battle=physicalBattle;archive=originalArchive;trusted=source;
            if(lastVerified==null){
                lastVerified=originalArchive.originalVerified;
                acceptedRevision=lastVerified.revision();
                lastCheckedAt=originalArchive.originalEpoch;
            }
            if(!checkedCurrentCustody())return reply(Verdict.STALE_OR_REVOKED_CUSTODY);
            callbackOrdinal++;
            lastLive=live;
            return new Report(this,Verdict.HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC,
                    physicalBattle,live,originalArchive);
        }
        private PublicCompetitiveLiveShadowConsumer.Report lastLive;
    }
}
