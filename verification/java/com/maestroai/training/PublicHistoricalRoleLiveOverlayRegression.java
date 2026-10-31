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
                && before.currentPublicCapabilities().isEmpty(),
                "original public history withdrawal immediately removes historical overlay");
        noAuthority(before);

        var late=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
        var owner=new PublicHistoricalRoleLiveOverlay.Session();
        var old=live(owner,late,history,()->history.custody());
        check(old.current() && !owner.battleEnded(new Object()) && old.current(),
                "foreign battle end cannot revoke a live source");
        check(owner.battleEnded(late.physicalBattle()) && !old.current()
                && owner.withdrawn(),"real battle end irreversibly withdraws role data");

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
