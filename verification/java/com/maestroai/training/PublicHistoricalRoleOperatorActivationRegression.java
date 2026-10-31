package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/** Adversarial operator-to-native historical archive activation (synthetic only). */
final class PublicHistoricalRoleOperatorActivationRegression {
    private PublicHistoricalRoleOperatorActivationRegression(){}
    private static int count;
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError("Phase5 operator activation: "+reason);
        count++;
    }
    private static void rejected(Runnable test,String reason){
        try{test.run();}catch(IllegalArgumentException expected){
            check(!expected.getMessage().contains("gen9ou-")
                    && !expected.getMessage().contains("SECRET_"),reason);
            return;
        }
        throw new AssertionError("Phase5 operator accepted "+reason);
    }
    private static final long NOW=1_700_000_100L;
    private static final String PREFIX="maestroai.phase5Archive.";
    static void run() throws Exception{
        check(PublicHistoricalRoleOperatorActivation.fromProperties(
                new Properties(),()->NOW)==null,"default unprovisioned is OFF");
        var partial=new Properties();
        partial.setProperty(PREFIX+"replayId","gen9ou-99887766");
        rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                partial,()->NOW),"partial configuration");
        Path directory=Files.createTempDirectory("maestro-phase5-operator-");
        try{
            String replay="|gen|9\n|gametype|singles\n|tier|[Gen 9] OU\n"
                    +"|player|p1|SYNTHETIC\n|start\n"
                    +"|switch|p1a: Ally|Pikachu, L100|100/100\n"
                    +"|switch|p2a: Test|Zoroark, L100|100/100\n"
                    +"|turn|1\n|move|p2a: Test|Nasty Plot|p1a: Ally\n|win|SYNTHETIC\n";
            Path log=directory.resolve("reviewed-spectator.log");
            Files.writeString(log,replay,StandardCharsets.UTF_8);
            var replaySource=new PublicReplayCorpus.Source("gen9ou-99887766",
                    "gen9ou",true,PublicReplayCorpus.RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION);
            var evidence=PublicReplayCorpus.ingest(replaySource,replay);
            var decision=new PublicReplayCustody.Decision("gen9ou-99887766",
                    "gen9ou",evidence.inputSha256(),PublicReplayCustody.Status.APPROVED,
                    NOW-25,NOW+1000,"c".repeat(64));
            var ledger=new PublicReplayCustody.Ledger(1,1,List.of(decision));
            var pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            var signature=Signature.getInstance("Ed25519");
            signature.initSign(pair.getPrivate());
            signature.update(PublicSignedCustody.canonicalBytes(
                    1,"phase5-test",NOW-10,"0".repeat(64),ledger));
            var signed=new PublicSignedCustody.SignedManifest(1,"phase5-test",
                    NOW-10,"0".repeat(64),ledger,
                    Base64.getEncoder().encodeToString(signature.sign()));
            var root=new PublicSignedCustody.TrustedPolicy(1,1,1,NOW-100,"",
                    List.of(new PublicSignedCustody.TrustedKey("phase5-test",
                            Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                            NOW-100,0)));
            var checkpoint=PublicCustodyTrustStore.initialize(directory,root,signed,NOW);
            var options=new Properties();
            options.setProperty(PREFIX+"directory",directory.toString());
            options.setProperty(PREFIX+"replayLog",log.toString());
            options.setProperty(PREFIX+"replayId","gen9ou-99887766");
            options.setProperty(PREFIX+"format","gen9ou");
            options.setProperty(PREFIX+"roleQueries","2:zoroark:nastyplot");
            options.setProperty(PREFIX+"replayRightsIndependentlyApproved","true");
            options.setProperty(PREFIX+"operatorVerifiedHostClock","true");
            options.setProperty(PREFIX+"operatorVerifiedTimeFloor",Long.toString(NOW-1));
            options.setProperty(PREFIX+"bootstrapEd25519Spki",
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
            options.setProperty(PREFIX+"bootstrapKeyId","phase5-test");
            options.setProperty(PREFIX+"bootstrapKeyActiveFrom",Long.toString(NOW-100));
            options.setProperty(PREFIX+"bootstrapKeyringEpoch","1");
            options.setProperty(PREFIX+"bootstrapMinimumRevision","1");
            options.setProperty(PREFIX+"bootstrapEarliestSignature",Long.toString(NOW-100));
            options.setProperty(PREFIX+"checkpointStateSha256",checkpoint.stateSha256());
            options.setProperty(PREFIX+"checkpointRevision","1");
            options.setProperty(PREFIX+"checkpointKeyringEpoch","1");
            var clock=new AtomicLong(NOW);
            var installed=PublicHistoricalRoleOperatorActivation.fromProperties(
                    options,clock::get);
            check(installed!=null && installed.archive()
                    .sourceScopedUnboundRoles().contains(
                        PublicHistoricalRoleLiveOverlay.Role.SPECIAL_ATTACK_BOOST_SIGNAL_POSSIBLE),
                    "signed archived role activated through external pinned journal");
            var live=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
            var session=new PublicHistoricalRoleLiveOverlay.Session();
            var report=session.inspect(live.physicalBattle(),live.live(),
                    installed.archive(),installed.trusted());
            check(report.current() && !report.confirmedOpponentBuild()
                    && !report.matchedHistoricalRoleToCurrentPokemon()
                    && !report.legacyChoiceAuthorized()
                    && !report.livePromotionAuthorized(),
                    "live diagnostic retains unbound role and zero authority");
            var invalidHash=new Properties();
            invalidHash.putAll(options);
            invalidHash.setProperty(PREFIX+"checkpointStateSha256","a".repeat(64));
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    invalidHash,clock::get),"wrong independent checkpoint");
            var wrongKey=new Properties();
            wrongKey.putAll(options);
            var attacker=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            wrongKey.setProperty(PREFIX+"bootstrapEd25519Spki",
                    Base64.getEncoder().encodeToString(attacker.getPublic().getEncoded()));
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    wrongKey,clock::get),"forged trust root");
            var wrongReplay=new Properties();
            wrongReplay.putAll(options);
            wrongReplay.setProperty(PREFIX+"replayId","gen9ou-99887767");
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    wrongReplay,clock::get),"wrong approved replay");
            var wrongRole=new Properties();
            wrongRole.putAll(options);
            wrongRole.setProperty(PREFIX+"roleQueries","2:zoroark:illegal move");
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    wrongRole,clock::get),"malformed role assertion");
            var rights=new Properties();
            rights.putAll(options);
            rights.setProperty(PREFIX+"replayRightsIndependentlyApproved","false");
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    rights,clock::get),"unsigned replay rights assertion");
            var noClock=new Properties();
            noClock.putAll(options);
            noClock.remove(PREFIX+"operatorVerifiedHostClock");
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    noClock,clock::get),"unapproved clock");
            // Operator rollback (even with unchanged, valid signed contents)
            // must revoke the already-issued report, never resurrect it.
            clock.set(NOW-2);
            check(!report.current() && session.withdrawn()
                    && report.unboundHistoricalRolePossibilities().isEmpty(),
                    "operator clock rollback revokes earlier live receipt");
            clock.set(NOW);
            check(!report.current(),"rollback recovery cannot resurrect");
            var installed2=PublicHistoricalRoleOperatorActivation.fromProperties(
                    options,clock::get);
            var live2=PublicCompetitiveLiveShadowConsumerRegression.syntheticLiveSource();
            var session2=new PublicHistoricalRoleLiveOverlay.Session();
            var report2=session2.inspect(live2.physicalBattle(),live2.live(),
                    installed2.archive(),installed2.trusted());
            check(report2.current(),"independent installation valid before tamper");
            Files.writeString(directory.resolve("custody-anchor.state"),"tamper");
            check(!report2.current() && session2.withdrawn()
                    && report2.unboundHistoricalRolePossibilities().isEmpty(),
                    "tampered trust journal withdraws without leaked role");
            rejected(()->PublicHistoricalRoleOperatorActivation.fromProperties(
                    options,clock::get),"tampered journal rejected on boot");
            // No raw replay identifiers or opponent details in objects/errors.
            check(!report2.toString().contains("gen9ou-99887766")
                    && !installed2.archive().toString().contains("Zoroark"),
                    "diagnostics redact raw historical evidence");
            System.out.println("Phase 5 operator activation PASS: "+count
                    +" trust/time/rights/rollback and Shadow-only cases; Minecraft UNTESTED");
        }finally{
            try(var paths=Files.walk(directory)){
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path->{try{Files.deleteIfExists(path);}
                            catch(java.io.IOException ignored){}});
            }
        }
    }
}
