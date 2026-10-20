package com.maestroai.training;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Phase 7.7 adversarial, synthetic-only group coverage and robustness tests. */
public final class Phase7StratifiedAuditRegression {
    private Phase7StratifiedAuditRegression() {}
    private static int checks;
    private static final String SECRET="PRIVATE_BATTLE_DO_NOT_EMIT";
    private static final String H="abcdef0123456789";
    private static final Phase7RegressionGate.Limits GLOBAL=
            new Phase7RegressionGate.Limits(1,0,3,3,3,0);
    private static void check(boolean truth,String why) {
        if(!truth)throw new AssertionError("Phase 7.7: "+why);
        checks++;
    }
    private static void reject(Runnable op,String why) {
        boolean failed=false;
        try{op.run();}catch(RuntimeException expected){failed=true;}
        check(failed,why);
    }
    private static String hex(int k) {
        char c="abcdef0123456789".charAt(k%16);
        return String.valueOf(c).repeat(64);
    }
    private static String sha(String s) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new AssertionError(e);}
    }
    private static Phase7Corpus.Row accepted(int index,String action,String shadow) {
        return new Phase7Corpus.Row("synthetic-"+index,Phase7Corpus.Status.ACCEPTED,
                hex(index),hex(index+7),action,action,shadow,true,
                shadow==null?null:action.equals(shadow),shadow==null,false);
    }
    private static Phase7Corpus.Row denied(int index,Phase7Corpus.Status status) {
        return new Phase7Corpus.Row("synthetic-rejected-"+index,status,
                hex(index+11),null,null,null,null,null,null,true,false);
    }
    private static Phase7TrustedIntake.Report report(List<Phase7Corpus.Row> rows,
            List<Phase7TrustedIntake.Status> statuses) {
        int ac=0,dup=0,rep=0,prov=0,base=0,yes=0,no=0,abs=0,ill=0,unknown=0;
        for(var r:rows) {
            if(r.status()==Phase7Corpus.Status.ACCEPTED) {
                ac++;
                if(Boolean.TRUE.equals(r.baselineAgreement()))base++;
                if(Boolean.TRUE.equals(r.shadowAgreement()))yes++;
                if(Boolean.FALSE.equals(r.shadowAgreement()))no++;
            } else {
                if(r.status().name().startsWith("DUPLICATE_"))dup++;
                else if(r.status().name().startsWith("REPLAY_"))rep++;
                else prov++;
                if(r.status()==Phase7Corpus.Status.UNKNOWN_PUBLIC_EVIDENCE
                        || r.status()==Phase7Corpus.Status.STALE_TURN
                        || r.status()==Phase7Corpus.Status.AMBIGUOUS_IDENTITY)unknown++;
            }
            if(r.shadowAbstained())abs++;
            if(r.shadowIllegal())ill++;
        }
        var coverage=new Phase7Corpus.Coverage(rows.size(),ac,rows.size()-ac,
                dup,rep,prov,ac,base,yes,no,abs,ill,unknown);
        var corpus=new Phase7Corpus.Report(Phase7Corpus.SCHEMA,917,hex(0),rows,coverage);
        return new Phase7TrustedIntake.Report(Phase7TrustedIntake.SCHEMA,917,statuses,corpus);
    }
    private static List<Phase7Corpus.Row> baseRows() {
        return List.of(
                accepted(1,"move:attack","move:attack"),
                accepted(2,"move:guard","move:attack"),
                accepted(3,"move:attack",null),
                accepted(4,"switch:slot1","switch:slot1"),
                accepted(5,"switch:slot1","switch:slot1"),
                accepted(6,"switch:slot1",null),
                denied(7,Phase7Corpus.Status.UNKNOWN_PUBLIC_EVIDENCE),
                denied(8,Phase7Corpus.Status.REPLAY_INCOMPLETE),
                denied(9,Phase7Corpus.Status.INVALID_PROVENANCE),
                denied(10,Phase7Corpus.Status.DUPLICATE_CAPTURE));
    }
    private static List<Phase7TrustedIntake.Status> statuses(int corpusRows) {
        var statuses=new ArrayList<Phase7TrustedIntake.Status>();
        for(int i=0;i<corpusRows;i++)
            statuses.add(Phase7TrustedIntake.Status.VERIFIED_FOR_CORPUS);
        statuses.add(Phase7TrustedIntake.Status.SIGNATURE_INVALID);
        return List.copyOf(statuses);
    }
    private static Phase7TrustedIntake.Report basis() {
        return report(baseRows(),statuses(baseRows().size()));
    }
    private static EnumMap<Phase7StratifiedAudit.Group,Phase7StratifiedAudit.Rule> rules(
            int moveMin,int switchMin,int maxLoss,int maxAbstain) {
        var rules=new EnumMap<Phase7StratifiedAudit.Group,Phase7StratifiedAudit.Rule>(
                Phase7StratifiedAudit.Group.class);
        for(var group:Phase7StratifiedAudit.Group.values()) {
            boolean act=group==Phase7StratifiedAudit.Group.ACTION_MOVE
                    || group==Phase7StratifiedAudit.Group.ACTION_SWITCH;
            int min=group==Phase7StratifiedAudit.Group.ACTION_MOVE?moveMin:
                    group==Phase7StratifiedAudit.Group.ACTION_SWITCH?switchMin:
                    group==Phase7StratifiedAudit.Group.QUALITY_ACCEPTED?6:0;
            rules.put(group,new Phase7StratifiedAudit.Rule(act || min>0,min,min,0,0,
                    maxLoss,maxAbstain,0));
        }
        return rules;
    }
    private record Installed(Phase7RegressionGate.PinnedBaseline global,
                             Phase7StratifiedAudit.PinnedPolicy policy) {}
    private static Installed install(Path root,Phase7TrustedIntake.Report basis,
                                     String revision,int dominance,int maxMixDrift,
                                     int moveMin,int switchMin,int actionLoss,int abstainLoss)
            throws Exception {
        var globalDef=Phase7RegressionGate.define(revision,basis,GLOBAL);
        Phase7RegressionGate.writeLocal(root,globalDef);
        String pinnedGlobal=sha(globalDef.json()); // SYNTHETIC ONLY: real pin must be independent.
        var global=Phase7RegressionGate.load(root,revision,pinnedGlobal);
        var profile=Phase7StratifiedAudit.define(revision,basis,3,dominance,
                maxMixDrift,rules(moveMin,switchMin,actionLoss,abstainLoss));
        Phase7StratifiedAudit.writeLocal(root,profile);
        String pinnedProfile=sha(profile.json()); // SYNTHETIC ONLY.
        var pinned=Phase7StratifiedAudit.load(root,revision,pinnedProfile);
        return new Installed(global,pinned);
    }
    private static Phase7StratifiedAudit.Report compare(Installed p,
            Phase7TrustedIntake.Report ref,Phase7TrustedIntake.Report candidate) {
        return Phase7StratifiedAudit.compare(p.global(),p.policy(),ref,candidate);
    }
    private static Phase7StratifiedAudit.GroupResult group(
            Phase7StratifiedAudit.Report result,Phase7StratifiedAudit.Group g) {
        return result.groups().stream().filter(s->s.group()==g).findFirst().orElseThrow();
    }
    private static void cleanup(Path root) throws Exception {
        try(var stream=Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path->{
                try{Files.deleteIfExists(path);}
                catch(java.io.IOException e){throw new AssertionError("cleanup failed",e);}
            });
        }
    }
    public static void run() {
        Path root=null;
        try {
            root=Files.createTempDirectory("phase77-strata-");
            final Path local=root;
            var baseline=basis();
            var installed=install(root,baseline,"synthetic_strat1",6500,0,3,3,0,0);
            var same=compare(installed,baseline,baseline);
            check(same.passed()&&same.status()==Phase7StratifiedAudit.Status.PASS,
                    "balanced fixed cohort passes independently pinned profiles");
            check(same.globalGateStatus().equals("PASS"),
                    "Phase 7.6 global gate still mandatory");
            check(same.json().equals(compare(installed,baseline,baseline).json()),
                    "exact-byte deterministic report");
            check(JsonParser.parseString(same.json()).isJsonObject(),"machine report valid JSON");
            check(same.referenceDominanceBasisPoints()==5000
                    && same.candidateDominanceBasisPoints()==5000
                    && same.evaluatedMixDriftBasisPoints()==0,
                    "balanced move/switch sampling measured");
            check(group(same,Phase7StratifiedAudit.Group.ACTION_MOVE).referenceCount()==3
                    && group(same,Phase7StratifiedAudit.Group.ACTION_SWITCH).referenceCount()==3,
                    "fixed action groups use actual Legacy action");
            check(group(same,Phase7StratifiedAudit.Group.QUALITY_UNKNOWN).referenceCount()==1
                    && group(same,Phase7StratifiedAudit.Group.QUALITY_REPLAY).referenceCount()==1
                    && group(same,Phase7StratifiedAudit.Group.QUALITY_DUPLICATE).referenceCount()==1
                    && group(same,Phase7StratifiedAudit.Group.QUALITY_PROVENANCE).referenceCount()==1
                    && group(same,Phase7StratifiedAudit.Group.QUALITY_INTAKE_REJECTED).referenceCount()==1,
                    "evidence categories partition without guessed game tactics");
            check(group(same,Phase7StratifiedAudit.Group.QUALITY_ACCEPTED).referenceCount()==6,
                    "accepted quality group covers exact accepted cohort");
            JsonArray strata=JsonParser.parseString(same.json()).getAsJsonObject()
                    .getAsJsonArray("strata");
            for(JsonElement e:strata) {
                var x=e.getAsJsonObject();
                if(x.get("group").getAsString().equals("QUALITY_UNKNOWN")) {
                    check(x.get("suppressed").getAsBoolean()
                            && x.get("referenceCount").isJsonNull()
                            && x.get("countDelta").isJsonNull()
                            && x.get("referenceAbstentions").isJsonNull(),
                            "rare group counts and metrics suppressed in exported JSON");
                }
            }
            check(strata.size()==Phase7StratifiedAudit.Group.values().length,
                    "every supported group published exactly once");
            check(!same.json().contains("synthetic-")&&!same.json().contains(SECRET)
                    && !same.json().contains("captureId")
                    && same.json().contains("not-win-rate"),
                    "no private raw events, per-sample IDs or win claims");

            // Improve the MOVE group by one, worsen SWITCH by one; overall agreement
            // unchanged. The Phase 7.6 aggregate remains PASS, while 7.7 fails.
            var rows=new ArrayList<>(baseRows());
            rows.set(1,accepted(2,"move:guard","move:guard"));
            rows.set(3,accepted(4,"switch:slot1","move:attack"));
            var masked=report(rows,statuses(rows.size()));
            check(Phase7RegressionGate.compare(installed.global(),baseline,masked)
                    .status()==Phase7RegressionGate.Status.PASS,
                    "aggregate gate cannot detect offsetting group regressions");
            var stratified=compare(installed,baseline,masked);
            check(stratified.status()==Phase7StratifiedAudit.Status.GROUP_ACTION_REGRESSION,
                    "per-switch deterioration detected despite aggregate PASS");
            check(group(stratified,Phase7StratifiedAudit.Group.ACTION_SWITCH).status()
                    ==Phase7StratifiedAudit.GroupStatus.ACTION_REGRESSION
                    && group(stratified,Phase7StratifiedAudit.Group.ACTION_MOVE).status()
                    ==Phase7StratifiedAudit.GroupStatus.PASS,
                    "pinpoints the affected action group");
            var reversed=new ArrayList<>(rows);Collections.reverse(reversed);
            check(compare(installed,baseline,report(reversed,statuses(rows.size()))).json()
                    .equals(stratified.json()),"input row permutation yields identical report");

            // Move's abstention goes down while Switch's goes up: aggregate
            // abstentions and agreements are constant, but switch coverage drops.
            var abstainRows=new ArrayList<>(baseRows());
            abstainRows.set(2,accepted(3,"move:attack","move:attack"));
            abstainRows.set(4,accepted(5,"switch:slot1",null));
            var shifted=report(abstainRows,statuses(abstainRows.size()));
            check(Phase7RegressionGate.compare(installed.global(),baseline,shifted)
                    .status()==Phase7RegressionGate.Status.PASS,
                    "offsetting abstentions keep global count unchanged");
            check(compare(installed,baseline,shifted).status()
                    ==Phase7StratifiedAudit.Status.GROUP_ACTION_REGRESSION,
                    "one stratum's abstention regression cannot hide in total");
            var tolerance=install(root,baseline,"synthetic_tolerant",6500,10000,3,3,1,1);
            check(compare(tolerance,baseline,shifted).status()
                    ==Phase7StratifiedAudit.Status.PASS,
                    "explicit per-group allowance can admit bounded shift");

            var severe=install(root,baseline,"synthetic_bias",5500,10000,1,1,0,0);
            var biasedRows=new ArrayList<Phase7Corpus.Row>();
            for(int i=0;i<5;i++)biasedRows.add(accepted(1+i,"move:attack","move:attack"));
            biasedRows.add(accepted(6,"switch:slot1","switch:slot1"));
            var biased=report(biasedRows,statuses(biasedRows.size()));
            var biasedPinned=install(root,biased,"synthetic_imbalanced",5500,10000,1,1,0,0);
            check(compare(biasedPinned,biased,biased).status()
                    ==Phase7StratifiedAudit.Status.SAMPLING_BIAS,
                    "systematic underrepresentation flagged even with no model regression");
            var required=install(root,biased,"synthetic_underrepresented",10000,10000,2,2,0,0);
            check(compare(required,biased,biased).status()
                    ==Phase7StratifiedAudit.Status.INSUFFICIENT_GROUP_COVERAGE,
                    "required Switch minimum rejects thin stratum");
            var display=compare(biasedPinned,biased,biased).json();
            var switchDisplay=JsonParser.parseString(display).getAsJsonObject()
                    .getAsJsonArray("strata");
            boolean suppressed=false;
            for(JsonElement e:switchDisplay) {
                JsonObject j=e.getAsJsonObject();
                if(j.get("group").getAsString().equals("ACTION_SWITCH")) {
                    suppressed=j.get("candidateCount").isJsonNull()
                            && j.get("candidateAgreementBasisPoints").isJsonNull();
                }
            }
            check(suppressed,"rare but consequential action category stays masked in output");
            check(JsonParser.parseString(display).getAsJsonObject()
                    .getAsJsonObject("sampling")
                    .get("candidateDominantActionBasisPoints").isJsonNull(),
                    "dominance percentage cannot indirectly reveal suppressed single case");

            var allAbstain=new ArrayList<Phase7Corpus.Row>();
            for(int i=1;i<=6;i++)
                allAbstain.add(accepted(i,i<=3?"move:attack":"switch:slot1",null));
            var neverEvaluated=report(allAbstain,statuses(allAbstain.size()));
            var n=install(root,neverEvaluated,"synthetic_all_abstain",6500,10000,3,3,0,0);
            check(compare(n,neverEvaluated,neverEvaluated).status()
                    ==Phase7StratifiedAudit.Status.SAMPLING_BIAS,
                    "undefined evaluated mix cannot masquerade as balanced coverage");
            check(compare(n,neverEvaluated,neverEvaluated).json()
                    .contains("\"evaluatedActionMixDriftBasisPoints\":null"),
                    "undefined sampling measures serialize null");

            var removed=new ArrayList<>(baseRows());
            removed.remove(5);
            var missing=report(removed,statuses(removed.size()));
            check(compare(installed,baseline,missing).status()
                    ==Phase7StratifiedAudit.Status.GLOBAL_GATE_BLOCKED,
                    "lost accepted evidence triggers the Phase 7.6 global gate");
            var rejectedShift=new ArrayList<>(baseRows());
            rejectedShift.set(7,denied(8,Phase7Corpus.Status.INVALID_PROVENANCE));
            check(compare(installed,baseline,report(rejectedShift,statuses(rejectedShift.size())))
                    .status()==Phase7StratifiedAudit.Status.GLOBAL_GATE_BLOCKED,
                    "reclassification of replay failure cannot be treated as improvement");
            var incorrectRef=report(List.of(accepted(1,"move:attack","move:attack")),
                    statuses(1));
            check(compare(installed,incorrectRef,incorrectRef).status()
                    ==Phase7StratifiedAudit.Status.POLICY_BINDING_INVALID,
                    "policy cannot be rebound to alternate reference report");
            var wrongPolicy=install(root,incorrectRef,"synthetic_wrong_ref",
                    10000,10000,1,0,0,0);
            check(compare(new Installed(installed.global(),wrongPolicy.policy()),
                    baseline,baseline).status()
                    ==Phase7StratifiedAudit.Status.POLICY_BINDING_INVALID,
                    "reference policy must match Phase 7.6 pinned baseline");

            Path file=Phase7StratifiedAudit.writeLocal(root,
                    Phase7StratifiedAudit.define("synthetic_roundtrip",baseline,3,
                            6500,0,rules(3,3,0,0)));
            String intact=Files.readString(file),hash=sha(intact);
            var immutable=Phase7StratifiedAudit.define("synthetic_roundtrip",baseline,3,
                    6500,0,rules(3,3,0,0));
            reject(()->{
                try{Phase7StratifiedAudit.writeLocal(root,immutable);}
                catch(java.io.IOException prevented){throw new IllegalStateException(prevented);}
            },"pinned revision cannot be silently overwritten");
            check(Files.readString(file).equals(intact),
                    "immutable baseline bytes preserved after refused overwrite");
            check(Phase7StratifiedAudit.load(root,"synthetic_roundtrip",hash)
                    .value().json().equals(intact),"versioned policy round-trip");
            reject(()->{
                try{Phase7StratifiedAudit.load(root,"synthetic_roundtrip","0".repeat(64));}
                catch(java.io.IOException unavailable){throw new IllegalStateException(unavailable);}
            },"externally pinned policy SHA cannot be bypassed");
            for(String edited:List.of(
                    intact.replace("\"version\":1","\"version\":2"),
                    intact.replace("\"minimumDisclosureCount\":3",
                            "\"minimumDisclosureCount\":1"),
                    intact.replace("\"maximumDominantActionBasisPoints\":6500",
                            "\"maximumDominantActionBasisPoints\":4000"),
                    intact.replace("\"ACTION_SWITCH\":{","\"SECRET_STRATUM\":{"),
                    intact.replace("\"schema\":\"phase7.7.policy.v1\"",
                            "\"schema\":\"phase7.6.policy.v1\""),
                    intact.replace("\"minimumCandidate\":3",
                            "\"minimumCandidate\":3,\"minimumCandidate\":3"),
                    intact.replace("\"referenceReportSha256\":",
                            "\""+SECRET+"\":\"PRIVATE\",\"referenceReportSha256\":"))) {
                Files.writeString(file,edited);
                reject(()->{
                    try{Phase7StratifiedAudit.load(root,"synthetic_roundtrip",sha(edited));}
                    catch(java.io.IOException invalid){throw new IllegalStateException(invalid);}
                },"re-pinned invalid policy/schema/duplicate keys fail closed");
            }
            Files.writeString(file,intact);
            check(Phase7StratifiedAudit.load(root,"synthetic_roundtrip",hash)
                    .value().equals(Phase7StratifiedAudit.load(root,"synthetic_roundtrip",hash)
                    .value()),"pinned policy determinism after adversarial edits");
            try {
                Path linked=root.resolve("run/phase7-corpus/strata/phase7_symlink.json");
                Files.createSymbolicLink(linked,file);
                reject(()->{
                    try{Phase7StratifiedAudit.load(root,"symlink",hash);}
                    catch(java.io.IOException invalid){throw new IllegalStateException(invalid);}
                },"symbolic link policy refused");
            } catch(UnsupportedOperationException | java.nio.file.FileSystemException notAvailable) {
                // Synthetic test filesystem may prohibit symlink creation.
            }
            reject(()->Phase7StratifiedAudit.define("../escape",baseline,3,6500,0,
                    rules(3,3,0,0)),"path-like policy revision rejected");
            reject(()->new Phase7StratifiedAudit.Rule(true,0,0,0,0,0,0,0),
                    "required group cannot have zero minimum");
            reject(()->new Phase7StratifiedAudit.Rule(false,0,0,-1,0,0,0,0),
                    "negative tolerance rejected");
            reject(()->new Phase7StratifiedAudit.Policy(
                    Phase7StratifiedAudit.POLICY_SCHEMA,1,"missing",sha(baseline.json()),
                    3,6500,0,Map.of(Phase7StratifiedAudit.Group.ACTION_MOVE,
                            new Phase7StratifiedAudit.Rule(true,1,1,0,0,0,0,0))),
                    "incomplete group universe rejected");
            check(!same.passed() == false && severe.policy().value().rules().size()==8,
                    "typed all-strata policy has complete explicit scope");
            System.out.println("Phase 7.7 synthetic stratified regressions PASS: "
                    +checks+" checks (real Legacy approvals: 0; Minecraft tests: 0)");
        } catch(Exception failure) {
            throw new AssertionError("Phase 7.7 synthetic stratification fixture failed",failure);
        } finally {
            if(root!=null)try{cleanup(root);}
            catch(Exception fail){throw new AssertionError("Phase 7.7 cleanup failed",fail);}
        }
    }
}
