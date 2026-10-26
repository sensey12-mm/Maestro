package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Synthetic signed local bundles. NEVER a trusted real Legacy capture. */
public final class Phase7TrustedIntakeRegression {
    private Phase7TrustedIntakeRegression(){}
    private static int checks;
    private static final String SECRET="PRIVATE_OPPONENT_SECRET_DO_NOT_EXPORT";
    private static final String FIRST="update\n|switch|p1a: Own|Mew, L100|100/100\n"
            +"|switch|p2a: Opponent|Mew, L100|100/100\n|turn|1";
    private static final String SECOND="update\n|turn|2";
    private static final String ARTIFACT="a".repeat(64);
    private static final String OWN="b".repeat(64);
    private static final String KEY_ID="independent_review";
    private static final String BATTLE="01010101-1111-2222-3333-444444444444";
    private static void check(boolean success,String msg){
        if(!success)throw new AssertionError("Phase 7.5: "+msg);
        checks++;
    }
    private static void reject(Runnable action,String why) {
        boolean rejected=false;
        try{action.run();}catch(RuntimeException expected){rejected=true;}
        check(rejected,why);
    }
    private static String sha(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String sha(String text){return sha(text.getBytes(StandardCharsets.UTF_8));}

    private record Fixture(Path root,KeyPair key,String pinned,Phase7Benchmark.Scenario scenario) {}
    private static Fixture setup() throws Exception {
        Path root=Files.createTempDirectory("maestro-phase75-");
        Path trust=root.resolve("run/phase7-corpus/trust");
        Files.createDirectories(trust);
        Files.createDirectories(root.resolve("run/phase7-corpus/private-input"));
        KeyPairGenerator generator=KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair=generator.generateKeyPair();
        byte[] encoded=pair.getPublic().getEncoded();
        Files.writeString(trust.resolve(KEY_ID+".pub"),Base64.getEncoder().encodeToString(encoded));
        Phase7Benchmark.Scenario scenario=Phase7Benchmark.parseFixture(
                Files.readString(Path.of("verification/benchmarks/phase7_1_synthetic.json")))
                .scenarios().getFirst();
        return new Fixture(root,pair,sha(encoded),scenario);
    }
    private static Path directory(Fixture f,String name) throws Exception {
        Path directory=f.root().resolve("run/phase7-corpus/private-input").resolve(name);
        Files.createDirectories(directory);
        return directory;
    }
    private static JsonObject chunks(String... lines) {
        var o=new JsonObject();var arr=new JsonArray();
        for(String line:lines)arr.add(line);
        o.add("chunks",arr);return o;
    }
    private static JsonObject manifest(Fixture f,String id,String actorDigest,String battle) {
        var obj=new JsonObject();
        obj.addProperty("schema",Phase7TrustedIntake.SCHEMA);
        obj.addProperty("scenarioId",f.scenario().id());
        String fp=Phase7Benchmark.stateFingerprint(f.scenario());
        obj.addProperty("scenarioFingerprint",fp);
        obj.addProperty("captureId",id);
        obj.addProperty("battleId",battle);
        obj.addProperty("turn",2);
        obj.addProperty("action","move:attack");
        obj.addProperty("artifactSha256",ARTIFACT);
        obj.addProperty("ownRequestSha256",actorDigest);
        List<String> raw=List.of(FIRST,SECOND);
        obj.addProperty("publicSha256",Phase7LegacyImport.publicDigest(
                PublicProtocolFilter.publicEvents(raw)));
        obj.addProperty("rawHistorySha256",Phase7LegacyImport.sha256(raw));
        obj.addProperty("choiceReceiptSha256",Phase7LegacyImport.sha256(List.of(
                "LEGACY_CHOOSE_RETURN",ARTIFACT,actorDigest,
                obj.get("publicSha256").getAsString(),fp,"2","move:attack",id)));
        obj.add("checkpoints",new JsonArray());
        return obj;
    }
    private static void resign(Fixture f,Path directory) throws Exception {
        String manifest=Files.readString(directory.resolve("manifest.json"));
        String hash=sha(manifest);
        Signature signature=Signature.getInstance("Ed25519");
        signature.initSign(f.key().getPrivate());
        signature.update(Phase7TrustedIntake.signaturePayload(hash));
        JsonObject custody=new JsonObject();
        custody.addProperty("schema",Phase7TrustedIntake.CUSTODY);
        custody.addProperty("keyId",KEY_ID);
        custody.addProperty("manifestSha256",hash);
        custody.addProperty("signature",Base64.getEncoder().encodeToString(signature.sign()));
        Files.writeString(directory.resolve("custody.json"),custody.toString());
    }
    private static Path create(Fixture f,String name,String id,String own,String battle) throws Exception {
        Path dir=directory(f,name);
        JsonObject m=manifest(f,id,own,battle);
        JsonArray cps=m.getAsJsonArray("checkpoints");
        List<JsonObject> checkpointData=List.of(chunks(FIRST),chunks(FIRST,SECOND));
        for(int i=0;i<checkpointData.size();i++) {
            String filename=String.format(Locale.ROOT,"checkpoint-%02d.json",i+1);
            String body=checkpointData.get(i).toString();
            Files.writeString(dir.resolve(filename),body);
            JsonObject desc=new JsonObject();
            desc.addProperty("file",filename);
            desc.addProperty("sha256",sha(body));
            cps.add(desc);
        }
        Files.writeString(dir.resolve("manifest.json"),m.toString());
        resign(f,dir);
        return dir;
    }
    private static Phase7TrustedIntake.Report run(Fixture f,List<String> names,
            String pinned,Phase7Benchmark.DecisionPolicy shadow) {
        return Phase7TrustedIntake.evaluateLocal(f.root(),names,
                Map.of(f.scenario().id(),f.scenario()),KEY_ID,pinned,73L,shadow);
    }
    private static Phase7TrustedIntake.Status status(Fixture f,String name) {
        return run(f,List.of(name),f.pinned(),null).intakeStatuses().getFirst();
    }
    private static void cleanup(Path root) throws Exception {
        try(var stream=Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p->{
                try{Files.deleteIfExists(p);}
                catch(java.io.IOException e){throw new AssertionError("synthetic temp cleanup failed",e);}
            });
        }
    }
    public static void run() {
        Fixture f=null;
        try {
            f=setup();
            Fixture local=f;
            Path genuine=create(f,"accepted","synthetic-capture1",OWN,BATTLE);
            var good=run(f,List.of("accepted"),f.pinned(),(snap,seed)->"move:attack");
            check(good.intakeStatuses().equals(List.of(
                    Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS)),"signed local original admitted for replay");
            check(good.corpus().coverage().accepted()==1
                    && good.corpus().coverage().comparisons()==1,"Phase 7.4 review actually accepts signed input");
            check(good.corpus().coverage().shadowAgreements()==1,
                    "Legacy and offline Shadow agreement counted");
            check(good.json().contains("\"schema\":\"phase7.5.v1\"")
                    && good.json().contains("\"corpusAccepted\":1"),"schema and coverage machine report");
            check(JsonParser.parseString(good.json()).isJsonObject(),"valid machine JSON");
            check(good.json().equals(run(f,List.of("accepted"),f.pinned(),
                    (snap,seed)->"move:attack").json()),"replay and output reproducible");
            check(!good.json().contains(BATTLE) && !good.json().contains("synthetic-capture1")
                    && !good.json().contains("Opponent") && !good.json().contains(OWN)
                    && !good.json().contains(SECRET),"no raw private, battle UUID or capture IDs exported");
            var abstained=run(f,List.of("accepted"),f.pinned(),null);
            check(abstained.corpus().coverage().shadowAbstentions()==1,
                    "missing Shadow decision abstains with independent review intact");
            var illegal=run(f,List.of("accepted"),f.pinned(),(snap,seed)->"move:secret");
            check(illegal.corpus().coverage().shadowIllegal()==1
                    && !illegal.json().contains("move:secret"),
                    "illegal candidate is redacted");

            var badKey=run(f,List.of("accepted"),"0".repeat(64),null);
            check(badKey.intakeStatuses().getFirst()==Phase7TrustedIntake.Status.TRUST_KEY_INVALID
                    && badKey.corpus().coverage().accepted()==0,
                    "wrong independently pinned key refuses all evidence");
            reject(()->Phase7TrustedIntake.evaluateLocal(local.root(),List.of("accepted"),
                    Map.of(local.scenario().id(),local.scenario()),KEY_ID,"not-a-sha",1,null),
                    "unpinned key hash rejected");
            check(run(f,List.of("missing"),f.pinned(),null).corpus().coverage().accepted()==0,
                    "missing bundle cannot create comparison");
            check(status(f,"missing")==Phase7TrustedIntake.Status.CUSTODY_UNAVAILABLE,
                    "missing directory is not automatically trusted");

            Path forged=create(f,"forged","synthetic-capture2","c".repeat(64),
                    "11111111-2222-3333-4444-555555555555");
            String cust=Files.readString(forged.resolve("custody.json"));
            Files.writeString(forged.resolve("custody.json"),cust.replace("\"signature\":\"",
                    "\"signature\":\"AAAA"));
            check(status(f,"forged")==Phase7TrustedIntake.Status.SIGNATURE_INVALID,
                    "edited detached signature is rejected");
            Path changed=create(f,"changed","synthetic-capture3","c".repeat(64),
                    "11111111-2222-3333-4444-555555555556");
            Files.writeString(changed.resolve("checkpoint-02.json"),chunks(FIRST,
                    SECOND+"\nsideupdate\np2\n|move|p2a: "+SECRET+"|item").toString());
            check(status(f,"changed")==Phase7TrustedIntake.Status.CHECKPOINT_INVALID,
                    "modified raw original fails signed checkpoint hash");
            check(!run(f,List.of("changed"),f.pinned(),null).json().contains(SECRET),
                    "tampered raw bytes are never reported");
            Path altered=create(f,"manifestedit","synthetic-capture4",OWN,
                    "11111111-2222-3333-4444-555555555557");
            Files.writeString(altered.resolve("manifest.json"),
                    Files.readString(altered.resolve("manifest.json"))
                            .replace("\"turn\":2","\"turn\":3"));
            check(status(f,"manifestedit")==Phase7TrustedIntake.Status.SIGNATURE_INVALID,
                    "unsigned manifest edit fails detached signature");

            Path falseChoice=create(f,"wrongchoice","synthetic-capture5","d".repeat(64),
                    "11111111-2222-3333-4444-555555555558");
            JsonObject m=StrictOwnJson.read(Files.readString(falseChoice.resolve("manifest.json")));
            m.addProperty("action","move:guard");
            Files.writeString(falseChoice.resolve("manifest.json"),m.toString());
            resign(f,falseChoice);
            check(status(f,"wrongchoice")==Phase7TrustedIntake.Status.MANIFEST_INVALID,
                    "resigned but mismatched return-binding hash rejected");
            Path unknown=create(f,"unknown","synthetic-capture6","e".repeat(64),
                    "11111111-2222-3333-4444-555555555559");
            JsonObject unexpected=StrictOwnJson.read(Files.readString(unknown.resolve("manifest.json")));
            unexpected.addProperty("privateOpponentItem",SECRET);
            Files.writeString(unknown.resolve("manifest.json"),unexpected.toString());
            resign(f,unknown);
            check(status(f,"unknown")==Phase7TrustedIntake.Status.MANIFEST_INVALID,
                    "signed private unknown manifest fields rejected");
            Path repeated=create(f,"duplicated","synthetic-capture7","f".repeat(64),
                    "11111111-2222-3333-4444-555555555551");
            String base=Files.readString(repeated.resolve("manifest.json"));
            Files.writeString(repeated.resolve("manifest.json"),
                    base.replace("\"turn\":2","\"turn\":2,\"turn\":2"));
            resign(f,repeated);
            check(status(f,"duplicated")==Phase7TrustedIntake.Status.MANIFEST_INVALID,
                    "duplicate JSON fields rejected by strict parser");

            Path wrongScenario=create(f,"badscenario","synthetic-capture8","c".repeat(64),
                    "11111111-2222-3333-4444-555555555552");
            JsonObject bad=StrictOwnJson.read(Files.readString(wrongScenario.resolve("manifest.json")));
            bad.addProperty("scenarioFingerprint","f".repeat(64));
            Files.writeString(wrongScenario.resolve("manifest.json"),bad.toString());
            resign(f,wrongScenario);
            check(status(f,"badscenario")==Phase7TrustedIntake.Status.SCENARIO_UNAVAILABLE,
                    "signed wrong public-state binding cannot select a new scenario");

            var missingCustody=create(f,"missingcustody","synthetic-capture9","c".repeat(64),
                    "11111111-2222-3333-4444-555555555553");
            Files.delete(missingCustody.resolve("custody.json"));
            check(status(f,"missingcustody")==Phase7TrustedIntake.Status.CUSTODY_UNAVAILABLE,
                    "unreviewed bundle rejected");
            var missingCheckpoint=create(f,"checkpointmissing","synthetic-capture10","c".repeat(64),
                    "11111111-2222-3333-4444-555555555554");
            Files.delete(missingCheckpoint.resolve("checkpoint-02.json"));
            check(status(f,"checkpointmissing")!=Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                    "incomplete history cannot be approved");

            Path dup=create(f,"samecapture","synthetic-capture1",OWN,BATTLE);
            var doubled=run(f,List.of("accepted","samecapture"),f.pinned(),null);
            check(doubled.intakeStatuses().stream().allMatch(s->
                    s==Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS)
                    && doubled.corpus().coverage().duplicates()==2
                    && doubled.corpus().coverage().accepted()==0,
                    "Phase 7.4 duplicate ID/turn rejects both signed occurrences");
            var reordered=run(f,List.of("samecapture","accepted"),f.pinned(),null);
            check(doubled.json().equals(reordered.json()),
                    "intake ordering does not select first signed duplicate");
            var repeatedNames=run(f,List.of("accepted","accepted"),f.pinned(),null);
            check(repeatedNames.corpus().coverage().accepted()==0,
                    "duplicate input slug cannot count as multiple approved decisions");

            Path independent=create(f,"independent","synthetic-capture11","c".repeat(64),
                    "11111111-2222-3333-4444-555555555550");
            var mixed=run(f,List.of("accepted","independent","changed"),f.pinned(),
                    (snap,seed)->"move:guard");
            check(mixed.intakeStatuses().size()==3
                    && mixed.corpus().coverage().accepted()==2
                    && mixed.corpus().coverage().shadowDisagreements()==2
                    && mixed.intakeStatuses().contains(Phase7TrustedIntake.Status.CHECKPOINT_INVALID),
                    "multiple reviewed bundles and one rejected checked deterministically");

            Path traversal=f.root().resolve("run/phase7-corpus/private-input");
            reject(()->run(local,List.of("../trust"),local.pinned(),null),
                    "bundle traversal rejected");
            try {
                Path link=traversal.resolve("symlink");
                Files.createSymbolicLink(link,genuine);
                check(status(f,"symlink")==Phase7TrustedIntake.Status.PATH_UNSAFE,
                        "directory symlink cannot import private evidence");
                Path rawlink=directory(f,"checkpointsym");
                create(f,"checkpointsym","synthetic-capture12","c".repeat(64),
                        "11111111-2222-3333-4444-555555555549");
                Files.delete(rawlink.resolve("checkpoint-01.json"));
                Files.createSymbolicLink(rawlink.resolve("checkpoint-01.json"),genuine.resolve("checkpoint-01.json"));
                check(status(f,"checkpointsym")==Phase7TrustedIntake.Status.PATH_UNSAFE,
                        "checkpoint symlink rejected even if bytes match");
            } catch(UnsupportedOperationException | java.nio.file.FileSystemException unavailableFs) {
                // Test runtime may not permit creating symlinks.
            }
            var oversized=directory(f,"oversized");
            Files.writeString(oversized.resolve("manifest.json"),"A".repeat(16_385));
            Files.writeString(oversized.resolve("custody.json"),"{}");
            check(status(f,"oversized")==Phase7TrustedIntake.Status.PATH_UNSAFE,
                    "oversized private file refused before parsing");
            check(run(f,List.of(),f.pinned(),null).corpus().coverage().submitted()==0,
                    "empty intake does not invent a verified capture");
            reject(()->run(local,Collections.nCopies(101,"accepted"),local.pinned(),null),
                    "batch count bounded");
            check(!good.json().contains("winRate") && good.json().contains("not-minecraft"),
                    "no empirical performance or compatibility claims");
            System.out.println("Phase 7.5 synthetic signed local intake regressions PASS: "
                    +checks+" checks (actual Legacy capture approvals: 0; Minecraft runs: 0)");
        } catch(Exception failure) {
            throw new AssertionError("Phase 7.5 synthetic corpus setup failed",failure);
        } finally {
            if(f!=null) {
                try{cleanup(f.root());}catch(Exception failure){throw new AssertionError("temp cleanup failed",failure);}
            }
        }
    }
}
