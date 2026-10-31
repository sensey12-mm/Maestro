package com.maestroai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Phase 5 live historical source-roles; synthetic Ed25519 and native custody. */
final class PublicHistoricalRoleLiveOverlayRegression {
    private PublicHistoricalRoleLiveOverlayRegression(){}
    private static int checks;
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError("Historical Shadow roles: "+message);
        checks++;
    }
    private static void noAuthority(PublicHistoricalRoleLiveOverlay.Report report){
        check(!report.matchedHistoricalRoleToCurrentPokemon()
                && !report.confirmedOpponentBuild()
                && !report.calibratedOpponentResponses()
                && !report.onlinePhase5CandidateBuildsEnabled()
                && !report.historicSpeciesOrAppearanceJoined()
                && !report.hiddenMovesAbilitiesOrItemsExposed()
                && !report.shadowTacticalUseAuthorized()
                && !report.legacyChoiceAuthorized()
                && !report.livePromotionAuthorized()
                && !report.privateReplayContentExportable(),
                "history cannot become a current Pokemon hypothesis or tactical authority");
        String safe=report.toString();
        check(!safe.contains("Zoroark") && !safe.contains("Glimmora")
                && !safe.contains("Nasty Plot") && !safe.contains("nastyplot")
                && !safe.contains("OPPONENT_PRIVATE") && !safe.contains("p2a:")
                && !safe.contains("gen9ou-") && !safe.contains("SECRET_NICK")
                && !safe.contains("java.lang.Object@")
                && safe.contains("presentPokemon=UNKNOWN")
                && safe.contains("historicalJoin=UNBOUND")
                && safe.contains("forecast=NONE") && safe.contains("authority=NONE"),
                "diagnostics contain no replay source, opponent, species, moves or keys");
    }
    private static PublicHistoricalRoleLiveOverlay.Report live(
            PublicHistoricalRoleLiveOverlay.Session session,
            PublicCompetitiveLiveShadowConsumerRegression.SyntheticLiveFixture l,
            PublicShadowBuildHandoffRegression.SyntheticRolePackage h,
            PublicHistoricalRoleLiveOverlay.TrustedSource source){
        return session.inspect(l.physicalBattle(),l.live(),h.archive(),source);
    }
    static void run() throws Exception {
        var history=PublicShadowBuildHandoffRegression.syntheticRoleArchive();
        var source=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        check(source.live().current()
                && history.archive().sourceScopedUnboundRoles().equals(Set.of(
                    PublicHistoricalRoleLiveOverlay.Role.SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE))
                && !history.archive().currentOpponentIdentified()
                && !history.archive().phase5OnlineBuildAuthorized()
                && !history.archive().shadowTacticalAuthorized()
                && !history.archive().privateDataExportable(),
                "only originally signed, source-supported Phase 5.21 role survives import");
        var current=new AtomicReference<>(history.custody());
        PublicHistoricalRoleLiveOverlay.TrustedSource refreshed=current::get;
        var session=new PublicHistoricalRoleLiveOverlay.Session();
        check(session.inspect(source.physicalBattle(),null,history.archive(),refreshed)
                    .verdict()==PublicHistoricalRoleLiveOverlay.Verdict.NO_LIVE_SOURCE
                && !session.withdrawn(),
                "missing initial live evidence abstains");
        var accepted=live(session,source,history,refreshed);
        check(accepted.verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict
                        .HISTORICAL_ROLES_UNBOUND_SHADOW_DIAGNOSTIC
                && accepted.current()
                && accepted.unboundHistoricalRolePossibilities().equals(Set.of(
                    PublicHistoricalRoleLiveOverlay.Role.SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE))
                && accepted.currentPublicCapabilities().contains(
                    PublicCompetitiveLiveShadowConsumer.Capability.HAZARD_SET_OBSERVED),
                "source-backed historical boost signal remains separate from Glimmora public hazard");
        noAuthority(accepted);
        // The original source-issued report is immutable; no second synthetic
        // receipt can pretend to be a new Cobblemon native callback.
        var replay=live(session,source,history,refreshed);
        check(replay.verdict()==PublicHistoricalRoleLiveOverlay.Verdict.REPLAY_OR_REGRESSION
                && session.withdrawn()
                && !accepted.current()
                && accepted.unboundHistoricalRolePossibilities().isEmpty(),
                "replay revokes earlier overlay rather than refreshing a role");
        noAuthority(replay);

        // Phase 6 native sided source, identity continuity and Phase 5 live
        // consumer all participate; uncertainty cannot rebind an archived
        // source to Illusion, Transform, or a newly revealed form.
        for(String uncertain:new String[]{"illusion","transform","formechange"}){
            var ambiguous=PublicCompetitiveLiveShadowConsumerRegression
                    .syntheticUncertainLiveSource(uncertain);
            var undecided=new PublicHistoricalRoleLiveOverlay.Session();
            var denied=undecided.inspect(ambiguous.physicalBattle(),
                    ambiguous.live(),history.archive(),refreshed);
            check(denied.verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.SOURCE_MISMATCH
                    && undecided.withdrawn()
                    && !denied.current()
                    && denied.unboundHistoricalRolePossibilities().isEmpty()
                    && denied.currentPublicCapabilities().isEmpty(),
                    "Phase 6 uncertainty abstains from historical roles: "+uncertain);
            noAuthority(denied);
        }

        var foreign=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var different=new PublicHistoricalRoleLiveOverlay.Session();
        var mismatch=different.inspect(new Object(),source.live(),history.archive(),refreshed);
        check(mismatch.verdict()==PublicHistoricalRoleLiveOverlay.Verdict.SOURCE_MISMATCH
                && different.withdrawn() && !mismatch.current(),
                "foreign physical battle cannot be a Phase 5 source-joined actor");
        noAuthority(mismatch);

        var expiredSource=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var expiredSession=new PublicHistoricalRoleLiveOverlay.Session();
        var expired=live(expiredSession,expiredSource,history,refreshed);
        check(expired.current(),"originally approved signed source is current");
        current.set(new PublicHistoricalRoleLiveOverlay.CustodySnapshot(
                history.custody().signed(),history.custody().policy(),1_700_001_101L));
        check(!expired.current() && expired.unboundHistoricalRolePossibilities().isEmpty()
                && expiredSession.withdrawn(),
                "original replay approval expiry revokes source roles dynamically");
        noAuthority(expired);
        current.set(history.custody());
        check(expiredSession.inspect(expiredSource.physicalBattle(),
                    expiredSource.live(),history.archive(),refreshed).verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.WITHDRAWN,
                "rewinding trusted time cannot resurrect a withdrawn report");

        // A different trusted signer/policy cannot validate the original
        // Ed25519 manifest merely by returning the same replay timestamp.
        var swappedLive=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var swappedPkg=PublicShadowBuildHandoffRegression.syntheticRoleArchive();
        var changedSigner=new PublicHistoricalRoleLiveOverlay.Session().inspect(
                swappedLive.physicalBattle(),swappedLive.live(),history.archive(),
                ()->new PublicHistoricalRoleLiveOverlay.CustodySnapshot(
                    history.custody().signed(),swappedPkg.custody().policy(),
                    history.custody().currentEpochSecond()));
        check(changedSigner.verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.STALE_OR_REVOKED_CUSTODY
                && !changedSigner.current(),
                "independently supplied wrong trusted keyring must fail closed");
        noAuthority(changedSigner);

        // Even when the signed replay and role evidence are unchanged, a
        // legitimately higher keyring epoch is a new trust generation.
        // Existing battle leases must never bridge it.
        var rotationLive=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var rotatedSession=new PublicHistoricalRoleLiveOverlay.Session();
        var newTrusted=new AtomicReference<>(history.custody());
        var underOldRoot=live(rotatedSession,rotationLive,history,newTrusted::get);
        check(underOldRoot.current(),"original trust-epoch historical lease accepted");
        var oldPolicy=history.custody().policy();
        var epochTwo=new PublicSignedCustody.TrustedPolicy(
                oldPolicy.keyringEpoch()+1,oldPolicy.keyringEpoch()+1,
                oldPolicy.minimumRevision(),oldPolicy.earliestSignatureEpochSecond(),
                oldPolicy.requiredManifestDigest(),oldPolicy.keys());
        newTrusted.set(new PublicHistoricalRoleLiveOverlay.CustodySnapshot(
                history.custody().signed(),epochTwo,
                history.custody().currentEpochSecond()));
        check(!underOldRoot.current() && rotatedSession.withdrawn()
                && underOldRoot.unboundHistoricalRolePossibilities().isEmpty()
                && underOldRoot.currentPublicCapabilities().isEmpty(),
                "keyring epoch advance revokes all old live historical leases");
        newTrusted.set(history.custody());
        check(!underOldRoot.current()
                && rotatedSession.inspect(rotationLive.physicalBattle(),
                        rotationLive.live(),history.archive(),newTrusted::get).verdict()
                        ==PublicHistoricalRoleLiveOverlay.Verdict.WITHDRAWN,
                "returning to an older trusted keyring never resurrects reports");

        var noTrusted=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var absentSession=new PublicHistoricalRoleLiveOverlay.Session();
        check(absentSession.inspect(noTrusted.physicalBattle(),noTrusted.live(),
                    history.archive(),null).verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.NO_HISTORICAL_SOURCE
                && !absentSession.withdrawn(),
                "unprovisioned Minecraft must not load a historical archive");
        var fresh=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var withdrawal=new PublicHistoricalRoleLiveOverlay.Session();
        var before=live(withdrawal,fresh,history,()->history.custody());
        check(before.current(),"fresh source before original public history advances");
        var advanced=new ArrayList<>(fresh.original());
        advanced.add("update\n|move|p2a: OPPONENT_PRIVATE_UUID_SECRET_521|Recover|p1a: Own");
        fresh.routing().inspect(fresh.physicalBattle(),advanced,true);
        check(!before.current() && before.unboundHistoricalRolePossibilities().isEmpty()
                && before.currentPublicCapabilities().isEmpty() && withdrawal.withdrawn(),
                "original public history withdrawal irreversibly revokes historical overlay");
        check(withdrawal.inspect(fresh.physicalBattle(),fresh.live(),
                    history.archive(),()->history.custody()).verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.WITHDRAWN,
                "stale native/public receipt cannot reactivate its old session");
        noAuthority(before);

        // Even if an earlier positive report was not queried after its native
        // lease expired, a subsequent callback cannot quietly reuse it.
        var fresh2=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var abandoned=new PublicHistoricalRoleLiveOverlay.Session();
        var prior=live(abandoned,fresh2,history,()->history.custody());
        check(prior.current(),"second live native lease initially good");
        var progressed=new ArrayList<>(fresh2.original());
        progressed.add("update\n|turn|2");
        fresh2.routing().inspect(fresh2.physicalBattle(),progressed,true);
        check(abandoned.inspect(fresh2.physicalBattle(),fresh2.live(),
                    history.archive(),()->history.custody()).verdict()
                    ==PublicHistoricalRoleLiveOverlay.Verdict.SOURCE_MISMATCH
                && abandoned.withdrawn() && !prior.current(),
                "stale lease fails on next inspection even if prior report was not polled");

        var late=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var owner=new PublicHistoricalRoleLiveOverlay.Session();
        var old=live(owner,late,history,()->history.custody());
        check(old.current() && !owner.battleEnded(new Object()) && old.current(),
                "foreign battle end cannot revoke a live source");
        check(owner.battleEnded(late.physicalBattle()) && !old.current()
                && owner.withdrawn(),"real battle end irreversibly withdraws role data");
        var scope=new PublicHistoricalRoleLiveOverlay.BattleScope();
        var scopeSession=scope.forBattle(late.physicalBattle());
        var scoped=live(scopeSession,late,history,()->history.custody());
        check(scoped.current() && scope.end(late.physicalBattle())
                && !scoped.current() && scopeSession.withdrawn(),
                "battle scope end immediately withdraws active historical data");
        check(scope.forBattle(late.physicalBattle())==scopeSession
                && scopeSession.inspect(late.physicalBattle(),late.live(),
                        history.archive(),()->history.custody()).verdict()
                        ==PublicHistoricalRoleLiveOverlay.Verdict.WITHDRAWN,
                "same physical battle cannot resurrect after end");
        var nextBattle=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var nextSession=scope.forBattle(nextBattle.physicalBattle());
        check(nextSession!=scopeSession && live(nextSession,nextBattle,history,
                    ()->history.custody()).current() && !scoped.current(),
                "new physical battle gets distinct leases while ended report remains dead");

        var ai=Files.readString(Path.of("src/main/java/com/maestroai/ai/MaestroBattleAI.java"));
        int begin=ai.indexOf("public ShowdownActionResponse choose(");
        int end=ai.indexOf("private ShowdownActionResponse chooseLegacy(");
        check(begin>0 && end>begin
                && ai.contains("maestroai.phase5HistoricalRolesObserve")
                && ai.contains("installPhase5HistoricalRoles(")
                && ai.contains("Phase 5 historical Shadow")
                && !ai.substring(begin,end).contains("PublicHistoricalRoleLiveOverlay")
                && ai.substring(begin,end).contains("null, null, false);"),
                "operator only installs a source; Legacy chooser is isolated");
        System.out.println("Phase 5 live historical roles PASS: "+checks
                +" signed/offline and original native source regressions; Minecraft UNTESTED");
    }
}
