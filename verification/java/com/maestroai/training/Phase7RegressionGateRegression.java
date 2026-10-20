package com.maestroai.training;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Phase 7.6 synthetic regression gates: no recorded Legacy results or Minecraft battles. */
public final class Phase7RegressionGateRegression {
    private Phase7RegressionGateRegression() {}
    private static int checks;
    private static final String PRIVATE="PRIVATE_OPPONENT_DO_NOT_REPORT";
    private static final String A="a".repeat(64),B="b".repeat(64),
            C="c".repeat(64),D="d".repeat(64),E="e".repeat(64),F="f".repeat(64);
    private static final Phase7RegressionGate.Limits STRICT=
            new Phase7RegressionGate.Limits(2,0,0,0,0,0);
    private static void check(boolean ok,String why) {
        if(!ok)throw new AssertionError("Phase 7.6: "+why);
        checks++;
    }
    private static void reject(Runnable op,String why) {
        boolean rejected=false;
        try{op.run();}catch(RuntimeException invalid){rejected=true;}
        check(rejected,why);
    }
    private static String sha(String s){
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static Phase7Corpus.Row accepted(String id,String state,String pub,
                                              String action,String shadow,boolean illegal) {
        return new Phase7Corpus.Row(id,Phase7Corpus.Status.ACCEPTED,state,pub,
                action,action,shadow,true,shadow==null?null:action.equals(shadow),
                shadow==null,illegal);
    }
    private static Phase7Corpus.Row rejected(String id,String state,Phase7Corpus.Status status) {
        return new Phase7Corpus.Row(id,status,state,null,null,null,null,null,null,true,false);
    }
    private static Phase7TrustedIntake.Report report(
            List<Phase7Corpus.Row> rows,List<Phase7TrustedIntake.Status> intake,long seed) {
        int accepted=0,duplicate=0,replay=0,provenance=0,yes=0,no=0,abstain=0,
                illegal=0,unknown=0,baseline=0;
        for(var r:rows) {
            if(r.status()==Phase7Corpus.Status.ACCEPTED) {
                accepted++;
                if(Boolean.TRUE.equals(r.shadowAgreement()))yes++;
                if(Boolean.FALSE.equals(r.shadowAgreement()))no++;
                if(Boolean.TRUE.equals(r.baselineAgreement()))baseline++;
            } else {
                if(r.status().name().startsWith("DUPLICATE_"))duplicate++;
                else if(r.status().name().startsWith("REPLAY_"))replay++;
                else provenance++;
                if(r.status()==Phase7Corpus.Status.UNKNOWN_PUBLIC_EVIDENCE
                        || r.status()==Phase7Corpus.Status.STALE_TURN
                        || r.status()==Phase7Corpus.Status.AMBIGUOUS_IDENTITY)unknown++;
            }
            if(r.shadowAbstained())abstain++;
            if(r.shadowIllegal())illegal++;
        }
        var coverage=new Phase7Corpus.Coverage(rows.size(),accepted,rows.size()-accepted,
                duplicate,replay,provenance,accepted,baseline,yes,no,abstain,illegal,unknown);
        var corpus=new Phase7Corpus.Report(Phase7Corpus.SCHEMA,seed,A,rows,coverage);
        return new Phase7TrustedIntake.Report(Phase7TrustedIntake.SCHEMA,seed,intake,corpus);
    }
    private static Phase7TrustedIntake.Report base() {
        return report(List.of(accepted("synthetic-a",A,B,"move:attack","move:attack",false),
                        accepted("synthetic-b",C,D,"move:guard","move:guard",false),
                        rejected("synthetic-unreviewed",E,Phase7Corpus.Status.NO_CUSTODY_REVIEW)),
                List.of(Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                        Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                        Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                        Phase7TrustedIntake.Status.SIGNATURE_INVALID),73L);
    }
    private static Phase7TrustedIntake.Report candidate(
            Phase7TrustedIntake.Report basis,List<Phase7Corpus.Row> rows,
            List<Phase7TrustedIntake.Status> intake) {
        return report(rows,intake,basis.seed());
    }
    private static void delete(Path root) throws Exception {
        try(var stream=Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path->{
                try{Files.deleteIfExists(path);}
                catch(java.io.IOException e){throw new AssertionError("temp cleanup failed",e);}
            });
        }
    }
    public static void run() {
        Path temp=null;
        try {
            temp=Files.createTempDirectory("phase76-gate-");
            final Path root=temp;
            var ref=base();
            var definition=Phase7RegressionGate.define("synthetic_v1",ref,STRICT);
            check(definition.schema().equals(Phase7RegressionGate.BASELINE_SCHEMA)
                    && definition.version()==1 && definition.seed()==73,
                    "versioned baseline captures fixed seed and schema");
            check(definition.referenceReportSha256().equals(sha(ref.json())),
                    "reference report SHA binds sanitized exact bytes");
            Path file=Phase7RegressionGate.writeLocal(root,definition);
            check(file.startsWith(root.resolve("run/phase7-corpus/baselines"))
                    && Files.readString(file).equals(definition.json()),
                    "baseline stored only in ignored local directory");
            String pinnedSha=sha(Files.readString(file));
            var pinned=Phase7RegressionGate.load(root,"synthetic_v1",pinnedSha);
            check(pinned.fileSha256().equals(pinnedSha)
                    && pinned.value().equals(definition),"independent exact-byte pin recognized");
            var same=Phase7RegressionGate.compare(pinned,ref,ref);
            check(same.status()==Phase7RegressionGate.Status.PASS,
                    "identical trusted synthetic report passes strict gates");
            check(same.changes().equals(new Phase7RegressionGate.Changes(
                    0,0,0,0,0,0,0,0,0,0,0,0,0)),"no spurious regression deltas");
            check(same.referenceAgreementBasisPoints()==10000
                    && same.candidateAgreementBasisPoints()==10000,
                    "agreement basis points use accepted evidence denominators");
            check(same.referenceProvenanceRejectionBasisPoints()==5000,
                    "provenance rate combines corpus and intake rejection denominator");
            check(same.json().equals(Phase7RegressionGate.compare(pinned,ref,ref).json()),
                    "byte-for-byte stable comparison JSON");
            check(JsonParser.parseString(same.json()).getAsJsonObject()
                    .get("status").getAsString().equals("PASS"),"machine-readable status");
            check(!same.json().contains(PRIVATE) && !same.json().contains("synthetic-a")
                    && !same.json().contains("Opponent")
                    && !same.json().contains("capture") && same.json().contains("not-win-rate"),
                    "no private details, identifiers or empirical win-rate implication");

            var r1=accepted("synthetic-a",A,B,"move:attack","move:guard",false);
            var r2=ref.corpus().rows().get(1);
            var r3=ref.corpus().rows().get(2);
            var worse=candidate(ref,List.of(r1,r2,r3),ref.intakeStatuses());
            var result=Phase7RegressionGate.compare(pinned,ref,worse);
            check(result.status()==Phase7RegressionGate.Status.ACTION_REGRESSION
                    && result.changes().shadowAgreements()==-1
                    && result.changes().shadowDisagreements()==1,
                    "same cohort action agreement drop detected, not win-rate claim");
            check(result.candidateAgreementBasisPoints()==5000
                    && result.referenceAgreementBasisPoints()==10000,
                    "action agreement rate change recorded precisely");
            var reordered=candidate(ref,List.of(r3,r2,r1),ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,ref,reordered).status()
                    ==Phase7RegressionGate.Status.ACTION_REGRESSION,
                    "row presentation order cannot conceal action regression");

            var noShadow=candidate(ref,List.of(
                    accepted("synthetic-a",A,B,"move:attack",null,false),r2,r3),
                    ref.intakeStatuses());
            var abstention=Phase7RegressionGate.compare(pinned,ref,noShadow);
            check(abstention.status()==Phase7RegressionGate.Status.ACTION_REGRESSION
                    && abstention.changes().shadowAbstentions()==1,
                    "Shadow abstention regression detected");
            var illegal=candidate(ref,List.of(
                    accepted("synthetic-a",A,B,"move:attack",null,true),r2,r3),
                    ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,ref,illegal).status()
                    ==Phase7RegressionGate.Status.ACTION_REGRESSION
                    && Phase7RegressionGate.compare(pinned,ref,illegal)
                            .changes().illegalDecisions()==1,
                    "illegal candidate increase independently blocks gate");

            var lenient=new Phase7RegressionGate.Limits(2,0,1,1,1,0);
            var policy=Phase7RegressionGate.define("synthetic_lenient",ref,lenient);
            Phase7RegressionGate.writeLocal(root,policy);
            var policyPinned=Phase7RegressionGate.load(root,"synthetic_lenient",sha(policy.json()));
            check(Phase7RegressionGate.compare(policyPinned,ref,worse).status()
                    ==Phase7RegressionGate.Status.PASS,"explicit tolerance tested, no implied quality proof");
            check(Phase7RegressionGate.compare(policyPinned,ref,illegal).status()
                    ==Phase7RegressionGate.Status.PASS,"illegal-action tolerance explicit and bounded");

            var missing=candidate(ref,List.of(r1,r3),
                    List.of(Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.SIGNATURE_INVALID));
            var coverage=Phase7RegressionGate.compare(pinned,ref,missing);
            check(coverage.status()==Phase7RegressionGate.Status.INSUFFICIENT_EVIDENCE
                    && coverage.changes().accepted()==-1,
                    "losing accepted examples fails minimum coverage; never appears as improvement");
            var drift=candidate(ref,List.of(accepted("synthetic-a",A,F,"move:attack",
                    "move:attack",false),r2,r3),ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,ref,drift).status()
                    ==Phase7RegressionGate.Status.COHORT_DRIFT,
                    "different public-state evidence fails closed even if scores unchanged");
            var legacyDrift=candidate(ref,List.of(
                    accepted("synthetic-a",A,B,"move:guard","move:guard",false),r2,r3),
                    ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,ref,legacyDrift).status()
                    ==Phase7RegressionGate.Status.COHORT_DRIFT,
                    "different genuine Legacy reference decision blocks comparisons");

            var rejectedShift=candidate(ref,List.of(
                    ref.corpus().rows().get(0),r2,
                    rejected("synthetic-unreviewed",E,Phase7Corpus.Status.REPLAY_INCOMPLETE)),
                    ref.intakeStatuses());
            var evidence=Phase7RegressionGate.compare(pinned,ref,rejectedShift);
            check(evidence.status()==Phase7RegressionGate.Status.COHORT_DRIFT
                    && evidence.changes().provenanceRejections()==-1
                    && evidence.changes().replayRejections()==1,
                    "provenance and replay rejection changes recorded and cohort drift denied");
            var intakeShift=candidate(ref,ref.corpus().rows(),
                    List.of(Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.CUSTODY_UNAVAILABLE));
            check(Phase7RegressionGate.compare(pinned,ref,intakeShift).status()
                    ==Phase7RegressionGate.Status.COHORT_DRIFT,
                    "intake provenance categories cannot change undetected");
            var extraDenied=candidate(ref,ref.corpus().rows(),
                    List.of(Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS,
                            Phase7TrustedIntake.Status.SIGNATURE_INVALID,
                            Phase7TrustedIntake.Status.TRUST_KEY_INVALID));
            var intakeResult=Phase7RegressionGate.compare(pinned,ref,extraDenied);
            check(intakeResult.status()==Phase7RegressionGate.Status.COHORT_DRIFT
                    && intakeResult.changes().intakeRejections()==1
                    && intakeResult.candidateProvenanceRejectionBasisPoints()>5000,
                    "additional signature rejects cannot masquerade as accepted samples");

            var inconsistent=new Phase7Corpus.Report(Phase7Corpus.SCHEMA,73,A,
                    ref.corpus().rows(),new Phase7Corpus.Coverage(
                            3,3,0,0,0,0,3,2,2,0,1,0,0));
            var forged=new Phase7TrustedIntake.Report(Phase7TrustedIntake.SCHEMA,
                    73,ref.intakeStatuses(),inconsistent);
            check(Phase7RegressionGate.compare(pinned,ref,forged).status()
                    ==Phase7RegressionGate.Status.INVALID_REPORT,
                    "summary-only fabricated coverage rejected");
            var duplicateAccepted=candidate(ref,List.of(ref.corpus().rows().get(0),
                    ref.corpus().rows().get(0),r3),ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,ref,duplicateAccepted).status()
                    ==Phase7RegressionGate.Status.COHORT_DRIFT,
                    "ambiguous duplicate accepted state cannot be silently matched");
            var corruptedCandidate=candidate(ref,ref.corpus().rows(),ref.intakeStatuses());
            var changedSeed=new Phase7TrustedIntake.Report(
                    Phase7TrustedIntake.SCHEMA,999,corruptedCandidate.intakeStatuses(),
                    corruptedCandidate.corpus());
            check(Phase7RegressionGate.compare(pinned,ref,changedSeed).status()
                    ==Phase7RegressionGate.Status.BASELINE_MISMATCH,
                    "candidate seed drift rejected");
            var changedRef=candidate(ref,List.of(r1,r2,r3),ref.intakeStatuses());
            check(Phase7RegressionGate.compare(pinned,changedRef,changedRef).status()
                    ==Phase7RegressionGate.Status.BASELINE_MISMATCH,
                    "different reference report cannot replace externally pinned baseline");
            var empty=report(List.of(),List.of(),73L);
            var emptyDefinition=Phase7RegressionGate.define("empty_corpus",empty,STRICT);
            Phase7RegressionGate.writeLocal(root,emptyDefinition);
            var emptyPinned=Phase7RegressionGate.load(root,"empty_corpus",sha(emptyDefinition.json()));
            var emptyResult=Phase7RegressionGate.compare(emptyPinned,empty,empty);
            check(emptyResult.status()==Phase7RegressionGate.Status.INSUFFICIENT_EVIDENCE,
                    "all-abstain empty corpus cannot pass quality gate");
            check(emptyResult.referenceAgreementBasisPoints()==-1
                    && emptyResult.json().contains("\"referenceAgreementBasisPoints\":null"),
                    "undefined ratios are null, never fabricated zero or positive");

            check(!Phase7RegressionGate.load(root,"synthetic_v1",pinnedSha).toString()
                    .contains("synthetic-a"),"pinned object redacted");
            reject(()->{
                try{Phase7RegressionGate.load(root,"synthetic_v1","0".repeat(64));}
                catch(java.io.IOException ex){throw new IllegalStateException(ex);}
            },"baseline bytes cannot be self-trusted after external pin mismatch");
            // Re-pinning untrusted edited files does NOT bypass strict schema validation.
            String valid=definition.json();
            for(String mutation:List.of(
                    valid.replace("\"version\":1","\"version\":2"),
                    valid.replace("\"schema\":\"phase7.6.baseline.v1\"",
                            "\"schema\":\"phase7.5.baseline.v1\""),
                    valid.replace("\"minimumAccepted\":2","\"minimumAccepted\":0"),
                    valid.replace("\"minimumAccepted\":2",
                            "\"minimumAccepted\":2,\"minimumAccepted\":2"),
                    valid.replace("\"referenceReportSha256\":",
                            "\"privateOpponentItem\":\""+PRIVATE+"\",\"referenceReportSha256\":"))) {
                Files.writeString(file,mutation);
                reject(()->{
                    try{Phase7RegressionGate.load(root,"synthetic_v1",sha(mutation));}
                    catch(java.io.IOException ex){throw new IllegalStateException(ex);}
                },"tampered/malformed re-pinned metadata never accepted");
            }
            Files.writeString(file,valid);
            check(Phase7RegressionGate.load(root,"synthetic_v1",pinnedSha)
                    .value().equals(definition),"valid baseline remains reproducible after malformed inputs");
            try {
                Path source=file;
                Path destination=root.resolve("run/phase7-corpus/baselines/phase7_linked.json");
                Files.createSymbolicLink(destination,source);
                reject(()->{
                    try{Phase7RegressionGate.load(root,"linked",pinnedSha);}
                    catch(java.io.IOException ex){throw new IllegalStateException(ex);}
                },"symlink to baseline file cannot bypass local-only boundary");
            } catch(UnsupportedOperationException | java.nio.file.FileSystemException unsupportedFs) {
                // Filesystem may disable symlink creation.
            }
            reject(()->Phase7RegressionGate.define("../private",ref,STRICT),
                    "path-like baseline revision rejected");
            reject(()->new Phase7RegressionGate.Limits(1,0,-1,0,0,0),
                    "negative tolerance rejected");
            reject(()->new Phase7RegressionGate.Limits(0,0,0,0,0,0),
                    "baseline must enforce positive accepted evidence");
        } catch(Exception unexpected) {
            throw new AssertionError("Phase 7.6 synthetic regression setup failed",unexpected);
        } finally {
            if(temp!=null)try{delete(temp);}
            catch(Exception failure){throw new AssertionError("temp cleanup failed",failure);}
        }
        System.out.println("Phase 7.6 synthetic corpus gate regressions PASS: "+checks+
                " checks (Minecraft tests: 0; authentic Legacy cases: 0)");
    }
}
