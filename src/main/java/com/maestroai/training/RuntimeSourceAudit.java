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
