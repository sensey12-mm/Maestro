package com.maestroai.training;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Function;

/** Exact source provenance only. Unknown files are rejected without reading them.
 * Reports contain fixed reasons and at most an expected public manifest key;
 * never a machine path, unknown filename, payload or exception message.
 */
public final class RuntimeSourceAudit {
    private RuntimeSourceAudit() {}
    public enum Reason {
        VERIFIED, CLASS_MISSING, CLASS_HASH_MISMATCH, TREE_MISSING,
        FILE_MISSING, FILE_HASH_MISMATCH, UNEXPECTED_SOURCE_FILE,
        SOURCE_LINK_UNSUPPORTED, MANIFEST_INVALID, AUDIT_UNAVAILABLE
    }
    public record Report(Reason reason, String reference) {
        public boolean verified() { return reason == Reason.VERIFIED; }
        public String safeReason() { return "SOURCE_AUDIT_" + reason; }
    }
    private static String hash(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static boolean valid(Map<String,String> manifest) {
        return !manifest.isEmpty() && manifest.entrySet().stream().allMatch(e ->
                e.getKey().matches("[a-zA-Z0-9_$./-]+") && !e.getKey().startsWith("/")
                && Arrays.stream(e.getKey().split("/",-1)).noneMatch(p -> p.isEmpty() || p.equals(".") || p.equals(".."))
                && e.getValue().matches("[a-f0-9]{64}"));
    }
    private static boolean sourceFile(Path p) {
        String n = p.getFileName().toString();
        return n.endsWith(".js") || n.endsWith(".json") && !n.equals("showdown.json");
    }
    /** Diagnostic only. Never reads an unlisted path or grants execution scope. */
    public static List<String> fingerprints(Path directory, Map<String,String> files,
                                           Map<String,String> classes, Function<String,byte[]> classBytes) {
        String prefix="[MaestroAI] PUBLIC_SOURCE_HASH ";
        if(!valid(files)||!valid(classes)||files.size()>1024||classes.size()>64)
            return List.of(prefix+"status=MANIFEST_INVALID diagnosticOnly=true");
        var lines=new ArrayList<String>();var transcript=new StringBuilder();int matched=0,checked=0;
        long remaining=128L*1024*1024; final int perFile=8*1024*1024;
        Path root=directory.toAbsolutePath().normalize();
        boolean rootLink=false;for(Path p=root;p!=null;p=p.getParent())rootLink|=Files.isSymbolicLink(p);
        for(String kind:List.of("CLASS","FILE")) {
            var manifest=kind.equals("CLASS")?classes:files;
            for(String name:new TreeSet<>(manifest.keySet())) {
                String observed="UNAVAILABLE";byte[] bytes=null;
                try {
                    if(remaining<=0) observed="LIMIT";
                    else if(kind.equals("CLASS")) bytes=classBytes.apply(name);
                    else {
                        Path current=root;boolean link=rootLink;
                        for(Path part:Path.of(name)) {current=current.resolve(part);link|=Files.isSymbolicLink(current);}
                        if(link) observed="LINK";
                        else if(!Files.isRegularFile(current,LinkOption.NOFOLLOW_LINKS)) observed="MISSING";
                        else try(var input=Files.newInputStream(current)) {
                            bytes=input.readNBytes((int)Math.min(remaining,perFile)+1);
                        }
                    }
                    if(bytes!=null) {
                        if(bytes.length>perFile||bytes.length>remaining) observed="LIMIT";
                        else {remaining-=bytes.length;observed=hash(bytes);}
                    }
                } catch(IOException | RuntimeException unavailable) { observed="UNAVAILABLE"; }
                checked++;boolean same=manifest.get(name).equals(observed);if(same)matched++;
                transcript.append(kind).append('\t').append(name).append('\t').append(observed).append('\n');
                if(!same)lines.add(prefix+"kind="+kind+" reference="+name+" observed="+observed+" matches=false diagnosticOnly=true");
            }
        }
        lines.add(prefix+"checked="+checked+" matched="+matched+" digest="+hash(transcript.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                +" unlistedFiles=NOT_READ payloads=EXCLUDED diagnosticOnly=true");
        return List.copyOf(lines);
    }
    public static Report inspect(Path directory, Map<String,String> files,
                                 Map<String,String> classes, Function<String,byte[]> classBytes) {
        if (!valid(files) || !valid(classes)) return new Report(Reason.MANIFEST_INVALID, "");
        try {
            for (String name : new TreeSet<>(classes.keySet())) {
                byte[] bytes = classBytes.apply(name);
                if (bytes == null) return new Report(Reason.CLASS_MISSING, name);
                if (!hash(bytes).equals(classes.get(name))) return new Report(Reason.CLASS_HASH_MISMATCH, name);
            }
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return new Report(Reason.TREE_MISSING, "");
            Path root = directory.toRealPath();
            var found = new TreeSet<String>();
            try (var walk = Files.walk(root)) {
                for (Path p : walk.toList()) {
                    if (Files.isSymbolicLink(p)) return new Report(Reason.SOURCE_LINK_UNSUPPORTED, "");
                    if (!Files.isRegularFile(p) || !sourceFile(p)) continue;
                    String name = root.relativize(p).toString().replace('\\','/');
                    // Do not inspect/hash a new JSON file: it could be runtime
                    // data rather than code. Inventory mismatch suffices.
                    if (!files.containsKey(name)) return new Report(Reason.UNEXPECTED_SOURCE_FILE, "");
                    found.add(name);
                }
            }
            for (String name : new TreeSet<>(files.keySet())) {
                if (!found.contains(name)) return new Report(Reason.FILE_MISSING, name);
                Path p = root.resolve(name);
                if (!p.toRealPath().startsWith(root)) return new Report(Reason.SOURCE_LINK_UNSUPPORTED, "");
                if (!hash(Files.readAllBytes(p)).equals(files.get(name))) return new Report(Reason.FILE_HASH_MISMATCH, name);
            }
            return new Report(Reason.VERIFIED, "");
        } catch (IOException | RuntimeException unavailable) {
            return new Report(Reason.AUDIT_UNAVAILABLE, "");
        }
    }
}
