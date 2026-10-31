package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/** Synthetic-only unified commissioning end-to-end adversarial regressions. */
final class PublicPhase5CommissioningAuditRegression {
    private PublicPhase5CommissioningAuditRegression(){}
    private static int count;
    private static final String VERSION="1.0.93";
    private static final String TEST_SHA="a".repeat(40);
    private static void expect(PublicPhase5CommissioningAudit.Report received,
                               PublicPhase5CommissioningAudit.Verdict verdict,
                               String reason){
        if(received.verdict()!=verdict
                || received.toString().contains("gen9ou-")
                || received.toString().contains("SYNTHETIC")
                || received.toString().contains("SECRET")
                || !received.toString().contains("authority=NONE")
                || !received.toString().contains("minecraft=UNTESTED"))
            throw new AssertionError("Phase 5 commissioning: "+reason
                    +" ["+received.verdict()+"]");
        count++;
    }
    private static Path jar(Path parent)throws Exception{
        Path p=parent.resolve("maestroai-"+VERSION+".jar");
        try(JarOutputStream z=new JarOutputStream(Files.newOutputStream(p))){
            z.putNextEntry(new ZipEntry("fabric.mod.json"));
            z.write(("{\"schemaVersion\":1,\"id\":\"maestroai\","
                    +"\"version\":\""+VERSION+"\"}").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("com/maestroai/MaestroAI.class"));
            z.write(new byte[]{(byte)0xca,(byte)0xfe,(byte)0xba,(byte)0xbe,0,0,0,65});
            z.closeEntry();
        }
        return p;
    }
    private static String sha(Path file)throws Exception{
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file)));
    }
    private static void write(Path file,Map<String,String> properties)throws Exception{
        var text=new StringBuilder();
        for(var e:new TreeMap<>(properties).entrySet())
            text.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        Files.writeString(file,text,StandardCharsets.UTF_8);
    }
    private static Map<String,String> map(Properties p){
        var data=new TreeMap<String,String>();
        for(String key:p.stringPropertyNames())data.put(key,p.getProperty(key));
        return data;
    }
    private static Path settings(Path parent,Properties p)throws Exception{
        Path file=parent.resolve("operator.properties");
        write(file,map(p));
        return file;
    }
    private static Path pin(Path parent,Path jar)throws Exception{
        Path file=parent.resolve("ci-jar-pin.properties");
        write(file,Map.of("sourceCommitSha",TEST_SHA,
                "jarSha256",sha(jar),"jarVersion",VERSION));
        return file;
    }
    private static PublicPhase5CommissioningAudit.Report inspect(
            Path jar,Path ciPin,Path configuration,Path root,Path checkpoint,long now){
        return PublicPhase5CommissioningAudit.inspect(jar,ciPin,configuration,
                root,checkpoint,()->now);
    }
    /** Called by the existing REAL signed-chain fixture, not by a mock oracle. */
    static PublicPhase5CommissioningAudit.Report accepted(
            Path parent,Path state,Path root,Path checkpoint,Path replay,
            Properties installed,long now)throws Exception{
        Path testJar=jar(parent),ciPin=pin(parent,testJar);
        Path config=settings(parent,installed);
        var r=inspect(testJar,ciPin,config,root,checkpoint,now);
        expect(r,PublicPhase5CommissioningAudit.Verdict
                        .LOCALLY_READY_FOR_MINECRAFT_TEST,
                "complete original signed archive with separate jar/root/checkpoint");
        if(!r.locallyReady()
                || !r.toString().contains("ciSuccess=EXTERNALLY_VERIFY")
                || !r.toString().contains("archiveRights=EXTERNALLY_VERIFY"))
            throw new AssertionError("Source-level audit must not claim CI or rights proof");
        count++;

        expect(inspect(parent.resolve("missing.jar"),ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.MISSING_JAR,
                "missing jar is categorical");
        var absent=parent.resolve("missing-ci-pin.properties");
        expect(inspect(testJar,absent,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.JAR_PIN_MISSING_OR_INVALID,
                "missing independent jar pin");
        Path badPin=parent.resolve("ci-pin-copy.properties");
        write(badPin,Map.of("sourceCommitSha",TEST_SHA,
                "jarSha256","b".repeat(64),"jarVersion",VERSION));
        expect(inspect(testJar,badPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.JAR_MISMATCH,
                "stale or substituted jar SHA rejected");
        write(badPin,Map.of("sourceCommitSha",TEST_SHA,
                "jarSha256",sha(testJar),"jarVersion","1.0.92"));
        expect(inspect(testJar,badPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.JAR_MISMATCH,
                "older version JAR cannot masquerade as new artifact");
        write(badPin,Map.of("sourceCommitSha","INVALID",
                "jarSha256",sha(testJar),"jarVersion",VERSION));
        expect(inspect(testJar,badPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.JAR_MISMATCH,
                "no noncanonical commit pin");
        Path originalJarBackup=parent.resolve("original-jar-backup.bin");
        Files.copy(testJar,originalJarBackup);
        Files.write(testJar,new byte[]{1,2,3,4});
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.JAR_MISMATCH,
                "modified JAR rejected before archive checks");
        Files.copy(originalJarBackup,testJar,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        var missingConfig=parent.resolve("missing-config.properties");
        expect(inspect(testJar,ciPin,missingConfig,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.OPERATOR_CONFIGURATION_MISSING,
                "operator settings missing");
        var disabled=new Properties();
        disabled.putAll(installed);
        disabled.setProperty("maestroai.phase5HistoricalRolesObserve","false");
        settings(parent,disabled);
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.OBSERVER_DISABLED,
                "observe explicitly off is not ready");
        settings(parent,installed);
        expect(inspect(testJar,ciPin,config,root,
                        parent.resolve("missing-pin.properties"),now),
                PublicPhase5CommissioningAudit.Verdict.INDEPENDENT_TRUST_PIN_MISSING,
                "independent signed journal anchor absent");

        var changed=new Properties();changed.putAll(installed);
        changed.setProperty("maestroai.phase5Archive.checkpointStateSha256",
                "f".repeat(64));
        settings(parent,changed);
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.TRUST_ANCHORS_MISMATCH,
                "launcher cannot substitute an independent checkpoint");
        changed.putAll(installed);
        changed.setProperty("maestroai.phase5Archive.bootstrapKeyId","other-signer");
        settings(parent,changed);
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.TRUST_ANCHORS_MISMATCH,
                "launcher cannot substitute a bootstrap signing key");
        settings(parent,installed);
        expect(inspect(testJar,ciPin,config,root,checkpoint,now-2),
                PublicPhase5CommissioningAudit.Verdict.CLOCK_ASSERTION_INVALID,
                "clock rollback below independent operator floor");
        Path wrongExternal=parent.resolve("wrong-external-pin.properties");
        write(wrongExternal,Map.of("checkpointStateSha256","0".repeat(64),
                "checkpointRevision","1","checkpointKeyringEpoch","1"));
        changed.putAll(installed);
        changed.setProperty("maestroai.phase5Archive.checkpointStateSha256",
                "0".repeat(64));
        settings(parent,changed);
        expect(inspect(testJar,ciPin,config,root,wrongExternal,now),
                PublicPhase5CommissioningAudit.Verdict.SIGNED_CUSTODY_REJECTED,
                "matching local settings cannot redeem a wrong independent state hash");
        settings(parent,installed);

        String originalReplay=Files.readString(replay,StandardCharsets.UTF_8);
        Files.writeString(replay,originalReplay+"\n|turn|2");
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.HISTORICAL_SOURCE_REJECTED,
                "altered spectator replay fails even with signed custody");
        Files.writeString(replay,originalReplay);
        expect(inspect(testJar,ciPin,config,root,checkpoint,now),
                PublicPhase5CommissioningAudit.Verdict.LOCALLY_READY_FOR_MINECRAFT_TEST,
                "synthetic operator source restored, no success cached from failure");
        return r;
    }
    static void rotated(Path parent,Path root,Path latestCheckpoint,
                        Properties rotatedSettings,long now)throws Exception{
        // Signed history: old signer revoked, independently signed successor
        // uses the new signer; original bootstrap remains externally pinned.
        Path jar=jar(parent);
        Path ci=pin(parent,jar);
        Path launch=settings(parent,rotatedSettings);
        expect(inspect(jar,ci,launch,root,latestCheckpoint,now),
                PublicPhase5CommissioningAudit.Verdict
                        .LOCALLY_READY_FOR_MINECRAFT_TEST,
                "rotated Ed25519 keyring with revoked old signer verifies end-to-end");
        Properties old=new Properties();
        old.putAll(rotatedSettings);
        old.setProperty("maestroai.phase5Archive.checkpointRevision","1");
        old.setProperty("maestroai.phase5Archive.checkpointKeyringEpoch","1");
        settings(parent,old);
        expect(inspect(jar,ci,launch,root,latestCheckpoint,now),
                PublicPhase5CommissioningAudit.Verdict.TRUST_ANCHORS_MISMATCH,
                "operator cannot silently reinstall a pre-rotation keyring pin");
        settings(parent,rotatedSettings);
    }
    static void revoked(Path parent,Path state,Path root,Path independentCheckpoint,
                        Path replay,Properties installed,long now)throws Exception{
        // After the append-only signed revocation the launcher and external
        // pin must BOTH be updated; otherwise a stale checkpoint is rejected
        // before the genuine replay-revocation reason can be inspected.
        settings(parent,installed);
        expect(inspect(parent.resolve("maestroai-"+VERSION+".jar"),
                        parent.resolve("ci-jar-pin.properties"),
                        parent.resolve("operator.properties"),root,
                        independentCheckpoint,now),
                PublicPhase5CommissioningAudit.Verdict.HISTORICAL_SOURCE_REJECTED,
                "signed replay revocation rejects combined local readiness");
    }
    static void run(){
        if(count<17)throw new AssertionError(
                "Phase 5 unified commissioning adversarial fixtures never exercised");
        System.out.println("Phase 5 unified commissioning PASS: "+count
                +" checks; CI trust EXTERNAL; Minecraft UNTESTED");
    }
}
