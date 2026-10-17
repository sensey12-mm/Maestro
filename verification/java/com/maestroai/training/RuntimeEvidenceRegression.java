package com.maestroai.training;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;

/** Source-conditioned public identity and filesystem provenance holdouts. */
final class RuntimeEvidenceRegression {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("runtime evidence holdout"); checks++; }
    private static String hash(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }
    private static PublicProtocolEvidence stream(String ability, boolean late, boolean privateOnly) {
        String start="update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L80|100/100\n";
        String announcement="|-ability|p2a: Foe|"+ability;
        var p=new PublicProtocolEvidence();
        p.update(List.of(start+(late?"|turn|1\n":"")+(privateOnly?"":announcement+"\n")
                        +(late?"":"|turn|1\n"),
                privateOnly?"sideupdate\np2\n"+announcement:"update\n|-enditem|p2a: Foe|Focus Sash"));
        return p;
    }
    private static PublicMechanicsEvidence provider() {
        return new PublicMechanicsEvidence(id -> id.equals("mew")
                ? new PublicMechanicsEvidence.Species(Map.of("hp",100,"atk",100,"def",100,"spa",100,"spd",100,"spe",100),List.of("psychic"),null) : null);
    }
    private static void identity() {
        var source=new PublicMechanicsEvidence.Scope(Known.of(true,"explicit source-certified fixture"),Known.of(false,"public room"));
        var unverified=new PublicMechanicsEvidence.Scope(Known.unknown(),Known.of(false,"public room"));
        var evidence=provider(); var p=stream("Pressure",false,false); evidence.update(p);
        var seen=evidence.observation("p2a");
        check(!seen.level().known() && !seen.confirmedSpecies().known()); // No unconditional promotion.
        var bound=evidence.bind("opponent",seen,source);
        check(bound.profile().level().known() && bound.profile().level().value()==80);
        check(bound.profile().types().known() && bound.profile().types().value().equals(List.of("psychic")));
        check(bound.profile().maxHp().known() && bound.profile().stats().size()==5);
        check(!evidence.bind("opponent",seen,unverified).profile().level().known());
        for(var negative:List.of(stream("Pressure",true,false),stream("Pressure",false,true),stream("Run Away",false,false))) {
            evidence.update(negative);
            check(!evidence.bind("opponent",evidence.observation("p2a"),source).profile().level().known());
        }
        boolean stale=false;try{evidence.bind("opponent",seen,source);}catch(IllegalArgumentException expected){stale=true;}
        check(stale);
        // A fresh sendout never inherits the prior entry's positive witness.
        var switched=new PublicProtocolEvidence();
        switched.update(List.of("update\n|gen|9\n|gametype|singles\n|start\n"
                +"|switch|p1a: Own|Mew|100/100\n|switch|p2a: Foe|Mew|100/100\n"
                +"|-ability|p2a: Foe|Pressure\n|turn|1\n|switch|p2a: Foe|Mew|100/100\n|turn|2"));
        evidence.update(switched);
        check(!evidence.bind("opponent",evidence.observation("p2a"),source).profile().level().known());
    }
    private static void provenance() throws Exception {
        Path root=Files.createTempDirectory("maestro-source-fixture-");
        try {
            byte[] code="'use strict';".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] clazz="synthetic pinned bytecode".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(root.resolve("index.js"),code);
            Path artifact=root.resolve("PRIVATE_INSTALLATION.jar");
            Files.writeString(artifact,"abc");
            check(InstalledArtifactReceipt.sha256(List.of(artifact)).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
            check(InstalledArtifactReceipt.sha256(List.of(artifact,root)).equals("UNKNOWN"));
            check(InstalledArtifactReceipt.sha256(List.of(root)).equals("UNKNOWN"));
            Files.delete(artifact);
            var files=Map.of("index.js",hash(code));var classes=Map.of("public/Source.class",hash(clazz));
            check(RuntimeSourceAudit.inspect(root,files,classes,n->clazz).verified());
            var pristine=RuntimeSourceAudit.fingerprints(root,files,classes,n->clazz);
            check(pristine.size()==1 && pristine.getFirst().contains("checked=2 matched=2"));
            check(pristine.equals(RuntimeSourceAudit.fingerprints(root,files,classes,n->clazz)));

            check(RuntimeSourceAudit.inspect(root,files,classes,n->null).reason()==RuntimeSourceAudit.Reason.CLASS_MISSING);
            check(RuntimeSourceAudit.inspect(root,files,classes,n->code).reason()==RuntimeSourceAudit.Reason.CLASS_HASH_MISMATCH);
            Files.writeString(root.resolve("index.js"),"changed hook");
            check(RuntimeSourceAudit.inspect(root,files,classes,n->clazz).reason()==RuntimeSourceAudit.Reason.FILE_HASH_MISMATCH);
            Files.write(root.resolve("index.js"),code);
            var alternatives=Map.of("index.js",Set.of(hash(code),hash("different exact pinned source".getBytes())));
            check(RuntimeSourceAudit.inspectAlternatives(root,alternatives,Map.of(),classes,n->clazz).verified());
            var optional=Map.of("unused.js",Set.of(hash(code)));
            check(RuntimeSourceAudit.inspectAlternatives(root,alternatives,optional,classes,n->clazz).verified());
            Files.write(root.resolve("unused.js"),code);
            check(RuntimeSourceAudit.inspectAlternatives(root,alternatives,optional,classes,n->clazz).verified());
            Files.writeString(root.resolve("unused.js"),"changed optional source");
            check(RuntimeSourceAudit.inspectAlternatives(root,alternatives,optional,classes,n->clazz).reason()==RuntimeSourceAudit.Reason.FILE_HASH_MISMATCH);
            Files.delete(root.resolve("unused.js"));
            check(RuntimeSourceAudit.inspectAlternatives(root,alternatives,Map.of("../escape",Set.of(hash(code))),classes,n->clazz).reason()==RuntimeSourceAudit.Reason.MANIFEST_INVALID);
            Files.writeString(root.resolve("PRIVATE_PLAYER_CONFIG.json"),"NEVER_LOG_ME");
            var unknown=RuntimeSourceAudit.inspect(root,files,classes,n->clazz);
            check(unknown.reason()==RuntimeSourceAudit.Reason.UNEXPECTED_SOURCE_FILE && unknown.reference().isEmpty());
            check(!unknown.safeReason().contains("PRIVATE") && !unknown.safeReason().contains(root.toString()));
            var privateAdded=RuntimeSourceAudit.fingerprints(root,files,classes,n->clazz);
            check(privateAdded.equals(pristine));
            check(privateAdded.stream().noneMatch(x->x.contains("PRIVATE")||x.contains("NEVER_LOG_ME")));
            var differences=RuntimeSourceAudit.fingerprints(root,files,classes,n->code);
            check(differences.stream().anyMatch(x->x.contains("kind=CLASS reference=public/Source.class observed=")&&x.contains("matches=false")));
            check(differences.stream().noneMatch(x->x.contains(root.toString())||x.contains("NEVER_LOG_ME")));

            Files.delete(root.resolve("PRIVATE_PLAYER_CONFIG.json"));Files.delete(root.resolve("index.js"));
            check(RuntimeSourceAudit.inspect(root,files,classes,n->clazz).reason()==RuntimeSourceAudit.Reason.FILE_MISSING);
            Files.createSymbolicLink(root.resolve("index.js"),root.resolve("private-outside-target"));
            var link=RuntimeSourceAudit.inspect(root,files,classes,n->clazz);
            check(link.reason()==RuntimeSourceAudit.Reason.SOURCE_LINK_UNSUPPORTED && link.reference().isEmpty());
            check(RuntimeSourceAudit.fingerprints(root,files,classes,n->clazz).stream().anyMatch(x->x.contains("observed=LINK")));

            Files.delete(root.resolve("index.js"));
            Files.createSymbolicLink(root.resolve("unlisted-directory"),root);
            check(RuntimeSourceAudit.inspect(root,files,classes,n->clazz).reason()==RuntimeSourceAudit.Reason.SOURCE_LINK_UNSUPPORTED);
            Files.delete(root.resolve("unlisted-directory"));
            check(RuntimeSourceAudit.inspect(root,Map.of("../escape.js",hash(code)),classes,n->clazz).reason()==RuntimeSourceAudit.Reason.MANIFEST_INVALID);
            check(RuntimeSourceAudit.inspect(root,files,classes,n->{throw new IllegalStateException("PRIVATE_PAYLOAD");}).safeReason().equals("SOURCE_AUDIT_AUDIT_UNAVAILABLE"));
            check(RuntimeSourceAudit.fingerprints(root,files,classes,n->{throw new IllegalStateException("PRIVATE_PAYLOAD");}).stream().noneMatch(x->x.contains("PRIVATE_PAYLOAD")));
            check(RuntimeSourceAudit.fingerprints(root,Map.of("../escape.js",hash(code)),classes,n->{throw new AssertionError("invalid read");}).getFirst().contains("MANIFEST_INVALID"));

        } finally {
            try(var walk=Files.walk(root)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
    static void run() {
        identity();
        var annotated=stream("Pressure",false,false);
        int events=annotated.mechanicsEvents().size();
        String start="update\n|gen|9\n|gametype|singles\n|start\n|switch|p1a: Own|Mew, L100|100/100\n|switch|p2a: Foe|Mew, L80|100/100\n|-ability|p2a: Foe|Pressure\n|turn|1\n";
        annotated.update(List.of(start,"update\n|-enditem|p2a: Foe|Focus Sash",
                "update\n|-nothing\n|-anim|p1a: Own|Tackle|p2a: Foe\n|-mostlyineffective|p2a: Foe\n|-extremelyeffective|p2a: Foe"));
        check(annotated.coverageUnknown().isEmpty() && annotated.mechanicsEvents().size()==events);
        for(String prior:List.of("U-turn","Baton Pass")) {
            var p=new PublicProtocolEvidence();p.update(List.of(start,"update\n|move|p1a: Own|"+prior+"|p2a: Foe\n|switch|p1a: Other|Arceus, L100|100/100\n|turn|2"));
            var e=provider();e.update(p);var field=new PublicFieldEvidence().update(p,e);
            check(field.unknown().isEmpty()==prior.equals("U-turn"));
        }
        for(boolean ended:List.of(false,true)) {
            var p=new PublicProtocolEvidence();p.update(List.of(start,"update\n|move|p1a: Own|Iron Head|p2a: Foe\n|cant|p2a: Foe|flinch"+(ended?"\n|turn|2":"")));
            var e=provider();e.update(p);var field=new PublicFieldEvidence().update(p,e);
            check(field.allActorsCovered()==ended);
        }
        var unknown=new PublicProtocolEvidence();unknown.update(List.of(start,"update\n|-unknown-pack-effect|p2a: Foe"));
        check(!unknown.coverageUnknown().isEmpty());
        try { provenance(); } catch(Exception e){throw new AssertionError("source provenance fixture",e);}
        System.out.println("Runtime public-evidence regressions PASS: "+checks+" checks");
    }
}
