package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Phase 7.11 adversarial adapter tests. Uses a FAKE Phase 4 source label,
 * SYNTHETIC local signatures and synthetic public events: not actual mechanics.
 */
public final class Phase7MechanicsAdapterRegression {
    private Phase7MechanicsAdapterRegression(){}
    private static int checks;
    private static final String COMMIT="b".repeat(64), CORPUS="c".repeat(64);
    private static final String SECRET="HIDDEN_OPPONENT_TOKEN_NEVER_OUTPUT";
    private static void check(boolean success,String why){
        if(!success)throw new AssertionError("Phase 7.11: "+why);
        checks++;
    }
    private static void reject(Runnable op,String why){
        boolean refused=false;
        try{op.run();}catch(RuntimeException valid){refused=true;}
        check(refused,why);
    }
    private static String sha(String s){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static String sha(byte[] s){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s));}
        catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static Phase7SelfPlay.Transitions full(Phase7SelfPlay.Transitions base){
        List<Phase7SelfPlay.Edge> edges=new ArrayList<>(base.edges());
        String next=edges.getFirst().successor(),origin=base.start();
        edges.add(new Phase7SelfPlay.Edge(origin,"move:attack","switch:reserve",
                next,sha("public-alt-move-switch"),
                Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION));
        edges.add(new Phase7SelfPlay.Edge(origin,"switch:reserve","move:attack",
                next,sha("public-alt-switch-move"),
                Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION));
        edges.add(new Phase7SelfPlay.Edge(origin,"switch:reserve","switch:reserve",
                next,sha("public-alt-switch-switch"),
                Phase7SelfPlay.Mechanic.PUBLIC_OBSERVED_TRANSITION));
        return new Phase7SelfPlay.Transitions(origin,base.nodes(),edges);
    }
    private static Phase7MechanicsAdapter.Bundle bundle(Phase7SelfPlay.Transitions t,
                  Map<String,Phase7Benchmark.Scenario> a,
                  Map<String,Phase7Benchmark.Scenario> b,
                  String revision,Phase7MechanicsAdapter.Source source,
                  Map<String,Phase7MechanicsAdapter.Verdict> judgments) {
        var entries=new ArrayList<Phase7MechanicsAdapter.Entry>();
        for(var edge:t.edges()){
            String hash=Phase7MechanicsAdapter.edgeSha256(edge);
            entries.add(new Phase7MechanicsAdapter.Entry(hash,
                    Phase7MechanicsAdapter.group(edge),
                    judgments.getOrDefault(hash,Phase7MechanicsAdapter.Verdict.REVIEWED_PUBLIC),
                    sha("synthetic-review-record-"+hash)));
        }
        return new Phase7MechanicsAdapter.Bundle(Phase7MechanicsAdapter.BUNDLE_SCHEMA,
                1,revision,source,COMMIT,CORPUS,
                "PHASE4_PUBLIC_MECHANICS_V1","a".repeat(64),
                Phase7SelfPlay.transitionsSha256(t,a,b),entries);
    }
    private static Phase7MechanicsAdapter.Bundle change(Phase7MechanicsAdapter.Bundle original,
                 String revision,Phase7MechanicsAdapter.Source source,
                 String commit,String corpus,String transition,
                 List<Phase7MechanicsAdapter.Entry> entries) {
        return new Phase7MechanicsAdapter.Bundle(Phase7MechanicsAdapter.BUNDLE_SCHEMA,
                1,revision,source,commit,corpus,original.sourceContract(),
                original.artifactSha256(),transition,entries);
    }
    private static KeyPair key()throws Exception {
        var gen=KeyPairGenerator.getInstance("Ed25519");
        return gen.generateKeyPair();
    }
    private static Phase7MechanicsAdapter.Attestation sign(
                 Phase7MechanicsAdapter.Bundle evidence,KeyPair key)throws Exception {
        var signer=Signature.getInstance("Ed25519");
        signer.initSign(key.getPrivate());
        signer.update(Phase7MechanicsAdapter.signaturePayload(
                Phase7MechanicsAdapter.bundleSha256(evidence)));
        return new Phase7MechanicsAdapter.Attestation(
                Base64.getEncoder().encodeToString(key.getPublic().getEncoded()),
                Base64.getEncoder().encodeToString(signer.sign()));
    }
    private static Phase7MechanicsAdapter.PinnedPolicy install(Path root,String revision,
                 Phase7MechanicsAdapter.Bundle original,String trustedKeySha,
                 Phase7MechanicsAdapter.Gate gate)throws Exception {
        var policy=Phase7MechanicsAdapter.define(revision,original,trustedKeySha,gate);
        Path file=Phase7MechanicsAdapter.writeLocal(root,policy);
        return Phase7MechanicsAdapter.load(root,revision,sha(Files.readString(file)));
    }
    private static Phase7MechanicsAdapter.Report audit(
                 Phase7SelfPlay.Transitions t,
                 Phase7SelfPlay.Participant a,Phase7SelfPlay.Participant b,
                 Phase7MechanicsAdapter.Bundle bundle,
                 Phase7MechanicsAdapter.Attestation signature,
                 Phase7MechanicsAdapter.PinnedPolicy policy) {
        return Phase7MechanicsAdapter.audit(t,a.scenarios(),b.scenarios(),bundle,signature,policy);
    }
    private static Phase7MechanicsAdapter.GatedReplay replay(
                 Phase7SelfPlay.Transitions t,
                 Phase7SelfPlay.Participant a,Phase7SelfPlay.Participant b,
                 Phase7SelfPlay.PinnedPolicy self,
                 Phase7SelfPlay.Attestation transitionSig,String transitionKeyPin,
                 Phase7MechanicsAdapter.Bundle bundle,
                 Phase7MechanicsAdapter.Attestation signature,
                 Phase7MechanicsAdapter.PinnedPolicy mechanics) {
        return Phase7MechanicsAdapter.replay(t,a,b,self,transitionSig,transitionKeyPin,
                bundle,signature,mechanics);
    }
    private static Phase7SelfPlay.PinnedPolicy selfPolicy(Path root,
                  Phase7SelfPlay.Transitions t,
                  Phase7SelfPlay.Participant a,Phase7SelfPlay.Participant b)throws Exception {
        var policy=Phase7SelfPlay.define("mechanics_gated_selfplay",t,a,b,333,3,20);
        Path path=Phase7SelfPlay.writeLocal(root,policy);
        return Phase7SelfPlay.load(root,policy.revision(),sha(Files.readString(path)));
    }
    private static Phase7SelfPlay.Attestation signTransition(
                 Phase7SelfPlay.Transitions t,
                 Phase7SelfPlay.Participant a,Phase7SelfPlay.Participant b,
                 KeyPair reviewer)throws Exception {
        var signer=Signature.getInstance("Ed25519");
        signer.initSign(reviewer.getPrivate());
        signer.update(Phase7SelfPlay.signaturePayload(
                Phase7SelfPlay.transitionsSha256(t,a.scenarios(),b.scenarios())));
        return new Phase7SelfPlay.Attestation(
                Base64.getEncoder().encodeToString(reviewer.getPublic().getEncoded()),
                Base64.getEncoder().encodeToString(signer.sign()));
    }

    public static void run(Path root,Phase7SelfPlay.Transitions original,
                 Phase7SelfPlay.Participant a,Phase7SelfPlay.Participant b) {
        try {
            var t=full(original);
            var sourceReviewer=key();
            var transitionReviewer=key();
            var attacker=key();
            String reviewerSha=sha(sourceReviewer.getPublic().getEncoded());
            String transitionSha=sha(transitionReviewer.getPublic().getEncoded());
            var base=bundle(t,a.scenarios(),b.scenarios(),"mechanics_base",
                    Phase7MechanicsAdapter.Source.PHASE4_REVIEWED_PUBLIC_V1,Map.of());
            var signature=sign(base,sourceReviewer);
            var strict=new Phase7MechanicsAdapter.Gate(3,1,1,1,6,0,0);
            var pinned=install(root,"mechanics_base",base,reviewerSha,strict);
            var result=audit(t,a,b,base,signature,pinned);
            check(result.eligible()&&result.status()==
                    Phase7MechanicsAdapter.Status.ELIGIBLE_OFFLINE_ONLY,
                    "signed, pinned fixed public cohort passes offline-only adapter gate");
            check(result.totals().total()==6&&result.totals().reviewed()==6,
                    "every attested transition is audited, including unselected branches");
            check(result.groups().get(Phase7MechanicsAdapter.Group.MOVE_MOVE).reviewed()==3
                    &&result.groups().get(Phase7MechanicsAdapter.Group.MOVE_SWITCH).reviewed()==1
                    &&result.groups().get(Phase7MechanicsAdapter.Group.SWITCH_MOVE).reviewed()==1
                    &&result.groups().get(Phase7MechanicsAdapter.Group.SWITCH_SWITCH).reviewed()==1,
                    "all four joint-action strata categorized deterministically");
            check(result.json().equals(audit(t,a,b,base,signature,pinned).json()),
                    "deterministic exact-byte mechanics reports");
            check(JsonParser.parseString(result.json()).getAsJsonObject()
                    .get("eligibleOffline").getAsBoolean(),"machine-readable status");
            check(result.json().contains("no-gameplay-promotion")
                    &&result.json().contains("not-minecraft"),
                    "no real-Cobblemon/competitive approval claims");
            check(!result.json().contains("move:attack")
                    &&!result.json().contains("switch:reserve")
                    &&!result.json().contains("first-turn")
                    &&!result.json().contains("Mew")
                    &&!result.json().contains(SECRET),
                    "no action/capture/private state exposed in public report");
            var groups=JsonParser.parseString(result.json()).getAsJsonObject()
                    .getAsJsonObject("groups");
            check(groups.getAsJsonObject("MOVE_SWITCH").get("suppressed").getAsBoolean()
                    &&!groups.getAsJsonObject("MOVE_SWITCH").has("reviewed"),
                    "single transition groups have suppressed counts");
            check(!groups.getAsJsonObject("MOVE_MOVE").get("suppressed").getAsBoolean(),
                    "sufficiently represented stratum discloses aggregate counts only");
            check(pinned.value().originalBundleSha256().equals(
                    Phase7MechanicsAdapter.bundleSha256(base))
                    &&pinned.value().pinnedSourceCommitSha256().equals(COMMIT),
                    "external baseline pins exact source and reference package");
            check(!pinned.toString().contains(SECRET),"pinned policy object redacted");

            var self=selfPolicy(root,t,a,b);
            var publicSignature=signTransition(t,a,b,transitionReviewer);
            var offline=replay(t,a,b,self,publicSignature,transitionSha,
                    base,signature,pinned);
            check(offline.mechanics().eligible()
                    &&offline.observed()!=null
                    &&offline.observed().status()==
                        Phase7SelfPlay.Status.VERIFIED_OBSERVATIONAL_SEQUENCE
                    &&offline.observed().verifiedSteps()==3,
                    "approved mechanics adapter actually gates existing signed self-play");
            check(offline.observed().json().equals(replay(t,a,b,self,publicSignature,
                    transitionSha,base,signature,pinned).observed().json()),
                    "gated replay reproducible under both independent reviewer signatures");

            check(audit(t,a,b,base,null,pinned).status()
                    ==Phase7MechanicsAdapter.Status.INVALID_SIGNATURE,
                    "unsigned mechanics approval advances zero turns");
            var refused=replay(t,a,b,self,publicSignature,transitionSha,
                    base,null,pinned);
            check(refused.observed()==null&&!refused.mechanics().eligible(),
                    "invalid mechanics never invokes dual-agent search");
            var attackerSignature=sign(base,attacker);
            check(audit(t,a,b,base,attackerSignature,pinned).status()
                    ==Phase7MechanicsAdapter.Status.UNTRUSTED_REVIEWER,
                    "signed package cannot self-approve an unknown reviewer key");
            var invalidSignature=new Phase7MechanicsAdapter.Attestation(
                    signature.publicKeyDerBase64(),
                    Base64.getEncoder().encodeToString(new byte[64]));
            check(audit(t,a,b,base,invalidSignature,pinned).status()
                    ==Phase7MechanicsAdapter.Status.INVALID_SIGNATURE,
                    "invalid Ed25519 signature rejected");

            var synthetic=change(base,"synthetic_marked",
                    Phase7MechanicsAdapter.Source.SYNTHETIC_TEST_ONLY,
                    COMMIT,CORPUS,base.transitionsSha256(),base.entries());
            var synPin=install(root,"synthetic_marked",synthetic,reviewerSha,strict);
            var synSignature=sign(synthetic,sourceReviewer);
            check(audit(t,a,b,synthetic,synSignature,synPin).status()
                    ==Phase7MechanicsAdapter.Status.SYNTHETIC_NOT_VERIFIED,
                    "even correctly signed synthetic-marked mechanics remain ineligible");
            check(replay(t,a,b,self,publicSignature,transitionSha,
                    synthetic,synSignature,synPin).observed()==null,
                    "synthetic-only provenance cannot be promoted to verified gameplay");

            var unknownEntry=base.entries().getFirst();
            var less=new ArrayList<>(base.entries());
            less.set(0,new Phase7MechanicsAdapter.Entry(unknownEntry.edgeSha256(),
                    unknownEntry.group(),Phase7MechanicsAdapter.Verdict.UNKNOWN,
                    unknownEntry.mechanicsReceiptSha256()));
            var unknown=change(base,"unknown_effect",
                    base.source(),COMMIT,CORPUS,base.transitionsSha256(),less);
            var unknownPin=install(root,"unknown_effect",unknown,reviewerSha,strict);
            var unknownSig=sign(unknown,sourceReviewer);
            var unknownReport=audit(t,a,b,unknown,unknownSig,unknownPin);
            check(unknownReport.status()==Phase7MechanicsAdapter.Status.UNKNOWN_MECHANICS
                    &&unknownReport.totals().unknown()==1,
                    "unknown effect blocks replay even when every edge is signed");
            check(replay(t,a,b,self,publicSignature,transitionSha,
                    unknown,unknownSig,unknownPin).observed()==null,
                    "unknown mechanics must abstain before any Shadow action");
            var unsupportedRows=new ArrayList<>(base.entries());
            unsupportedRows.set(0,new Phase7MechanicsAdapter.Entry(unknownEntry.edgeSha256(),
                    unknownEntry.group(),Phase7MechanicsAdapter.Verdict.UNSUPPORTED,
                    unknownEntry.mechanicsReceiptSha256()));
            var unsupported=change(base,"unsupported_effect",
                    base.source(),COMMIT,CORPUS,base.transitionsSha256(),unsupportedRows);
            var unsupportedPin=install(root,"unsupported_effect",unsupported,reviewerSha,strict);
            check(audit(t,a,b,unsupported,sign(unsupported,sourceReviewer),
                    unsupportedPin).status()==Phase7MechanicsAdapter.Status.UNSUPPORTED_MECHANICS,
                    "unsupported transition fails independently of signature validity");

            var fewer=new ArrayList<>(base.entries());
            fewer.removeLast();
            var missing=change(base,"missing_reviewed_edge",
                    base.source(),COMMIT,CORPUS,base.transitionsSha256(),fewer);
            var missingPin=install(root,"missing_reviewed_edge",missing,reviewerSha,strict);
            check(audit(t,a,b,missing,sign(missing,sourceReviewer),missingPin).status()
                    ==Phase7MechanicsAdapter.Status.MISSING_EVIDENCE,
                    "missing edge prevents cherry-picked path from passing");

            var duplicates=new ArrayList<>(base.entries());
            duplicates.add(duplicates.getFirst());
            var duplicate=change(base,"repeated_review",
                    base.source(),COMMIT,CORPUS,base.transitionsSha256(),duplicates);
            var duplicatePin=install(root,"repeated_review",duplicate,reviewerSha,strict);
            check(audit(t,a,b,duplicate,sign(duplicate,sourceReviewer),duplicatePin).status()
                    ==Phase7MechanicsAdapter.Status.DUPLICATE_RECORD,
                    "duplicate custody record is rejected");

            var tamperedGroup=new ArrayList<>(base.entries());
            tamperedGroup.set(0,new Phase7MechanicsAdapter.Entry(unknownEntry.edgeSha256(),
                    Phase7MechanicsAdapter.Group.SWITCH_SWITCH,
                    unknownEntry.verdict(),unknownEntry.mechanicsReceiptSha256()));
            var miscategorized=change(base,"misclassified_action",
                    base.source(),COMMIT,CORPUS,base.transitionsSha256(),tamperedGroup);
            var misPin=install(root,"misclassified_action",miscategorized,reviewerSha,strict);
            check(audit(t,a,b,miscategorized,sign(miscategorized,sourceReviewer),
                    misPin).status()==Phase7MechanicsAdapter.Status.MISSING_EVIDENCE,
                    "false action-group label rejected, not reclassified optimistically");

            var strongGate=new Phase7MechanicsAdapter.Gate(3,2,1,1,7,0,0);
            var strongPin=install(root,"coverage_strict",base,reviewerSha,strongGate);
            check(audit(t,a,b,base,signature,strongPin).status()
                    ==Phase7MechanicsAdapter.Status.COVERAGE_INSUFFICIENT,
                    "per-group minimum blocks misleading aggregate full coverage");

            var changedSource=change(base,"altered_source",base.source(),"d".repeat(64),
                    CORPUS,base.transitionsSha256(),base.entries());
            check(audit(t,a,b,changedSource,sign(changedSource,sourceReviewer),
                    pinned).status()==Phase7MechanicsAdapter.Status.UNBOUND_SOURCE,
                    "new source commit cannot borrow prior source trust pin");
            var changedReceipt=new ArrayList<>(base.entries());
            changedReceipt.set(0,new Phase7MechanicsAdapter.Entry(unknownEntry.edgeSha256(),
                    unknownEntry.group(),unknownEntry.verdict(),sha("different-mechanics-receipt")));
            var changed=change(base,"new_version",base.source(),COMMIT,CORPUS,
                    base.transitionsSha256(),changedReceipt);
            var compare=Phase7MechanicsAdapter.compare(t,a.scenarios(),b.scenarios(),
                    base,signature,changed,sign(changed,sourceReviewer),pinned);
            check(compare.status()==Phase7MechanicsAdapter.Status.SOURCE_DRIFT,
                    "changed mechanics receipt cannot hide behind unchanged source version");

            var downgraded=Phase7MechanicsAdapter.compare(t,a.scenarios(),b.scenarios(),
                    base,signature,unknown,unknownSig,pinned);
            check(downgraded.status()==Phase7MechanicsAdapter.Status.REGRESSION
                    &&downgraded.regressed()==1,
                    "reviewed-to-unknown downgrade is explicit cross-version regression");
            var unchanged=change(base,"same_evidence_new_revision",base.source(),
                    COMMIT,CORPUS,base.transitionsSha256(),base.entries());
            var identical=Phase7MechanicsAdapter.compare(t,a.scenarios(),b.scenarios(),
                    base,signature,unchanged,sign(unchanged,sourceReviewer),pinned);
            check(identical.status()==Phase7MechanicsAdapter.Status.ELIGIBLE_OFFLINE_ONLY
                    &&identical.unchanged()==6,
                    "revision change with same reviewed evidence passes typed comparison");
            check(identical.json().equals(
                    Phase7MechanicsAdapter.compare(t,a.scenarios(),b.scenarios(),base,
                    signature,unchanged,sign(unchanged,sourceReviewer),pinned).json()),
                    "cross-version comparison bytes deterministic");
            var wrongReviewer=Phase7MechanicsAdapter.compare(t,a.scenarios(),b.scenarios(),
                    base,signature,unchanged,sign(unchanged,attacker),pinned);
            check(wrongReviewer.status()==Phase7MechanicsAdapter.Status.UNTRUSTED_REVIEWER,
                    "cross-version candidate requires its own valid trusted signature");
            var replacedTranscript=new ArrayList<>(t.edges());
            var e=replacedTranscript.getFirst();
            replacedTranscript.set(0,new Phase7SelfPlay.Edge(e.source(),e.firstAction(),
                    e.secondAction(),e.successor(),sha("changed-public-transition"),
                    e.mechanic()));
            var modified=new Phase7SelfPlay.Transitions(t.start(),t.nodes(),replacedTranscript);
            check(audit(modified,a,b,base,signature,pinned).status()
                    ==Phase7MechanicsAdapter.Status.TRANSITION_DRIFT,
                    "changed public event fingerprint cannot reuse reviewed edge attestation");
            var reversed=new ArrayList<>(t.edges());Collections.reverse(reversed);
            var reorder=new Phase7SelfPlay.Transitions(t.start(),t.nodes(),reversed);
            check(audit(reorder,a,b,base,signature,pinned).json().equals(result.json()),
                    "canonical public transition ordering remains reproducible");

            Path file=root.resolve("run/phase7-corpus/mechanics/phase7_mechanics_base.json");
            String intact=Files.readString(file);
            reject(()->{
                try{Phase7MechanicsAdapter.writeLocal(root,pinned.value());}
                catch(java.io.IOException expected){throw new IllegalStateException(expected);}
            },"mechanics policy revision cannot be overwritten");
            for(String edit:List.of(
                    intact.replace("\"version\":1","\"version\":2"),
                    intact.replace("\"requireExternalReviewer\":true",
                            "\"requireExternalReviewer\":false"),
                    intact.replace("\"minimumTotal\":6","\"minimumTotal\":0"),
                    intact.replace("\"pinnedSourceCommitSha256\":",
                            "\""+SECRET+"\":\"leak\",\"pinnedSourceCommitSha256\":"),
                    intact.replace("\"minimumTotal\":6",
                            "\"minimumTotal\":6,\"minimumTotal\":6"))) {
                Files.writeString(file,edit);
                reject(()->{
                    try{Phase7MechanicsAdapter.load(root,"mechanics_base",sha(edit));}
                    catch(java.io.IOException expected){throw new IllegalStateException(expected);}
                },"even re-pinned malformed version, duplicate or unknown fields rejected");
            }
            Files.writeString(file,intact);
            check(Phase7MechanicsAdapter.load(root,"mechanics_base",
                    pinned.fileSha256()).value().equals(pinned.value()),
                    "trusted local exact-byte policy reloads deterministically");
            reject(()->{
                try{Phase7MechanicsAdapter.load(root,"mechanics_base","0".repeat(64));}
                catch(java.io.IOException expected){throw new IllegalStateException(expected);}
            },"untrusted local policy bytes rejected");
            try{
                Path symlink=root.resolve("run/phase7-corpus/mechanics/phase7_link.json");
                Files.createSymbolicLink(symlink,file);
                reject(()->{
                    try{Phase7MechanicsAdapter.load(root,"link",pinned.fileSha256());}
                    catch(java.io.IOException expected){throw new IllegalStateException(expected);}
                },"symlinked policy cannot bypass local-only trust boundary");
            }catch(UnsupportedOperationException|java.nio.file.FileSystemException fsLimited){
                // Synthetic runner filesystem may not allow creating symbolic links.
            }
            reject(()->Phase7MechanicsAdapter.define("../private",base,reviewerSha,strict),
                    "unsafe revision and traversal denied");
            reject(()->Phase7MechanicsAdapter.define("wrong_key",base,"bad",strict),
                    "reviewer key pin required independently");
            reject(()->new Phase7MechanicsAdapter.Gate(0,0,0,0,0,0,0),
                    "zero threshold would accept absence of public evidence");
            reject(()->new Phase7MechanicsAdapter.Bundle(
                    Phase7MechanicsAdapter.BUNDLE_SCHEMA,2,"wrong_version",
                    base.source(),COMMIT,CORPUS,base.sourceContract(),base.artifactSha256(),
                    base.transitionsSha256(),base.entries()),
                    "future mechanics schema versions explicitly unsupported");
            check(Phase7MechanicsAdapter.SCHEMA.equals("phase7.11.v1"),
                    "stable versioned public mechanics adapter contract");
            System.out.println("Phase 7.11 synthetic mechanics adapter regressions PASS: "
                    +checks+" checks (independently verified real mechanics: 0; Minecraft: 0)");
        } catch(Exception unexpected){
            throw new AssertionError("Phase 7.11 regression fixture failed",unexpected);
        }
    }
}
