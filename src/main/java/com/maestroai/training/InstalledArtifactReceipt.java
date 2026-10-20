package com.maestroai.training;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Opt-in installed-file linkage only, not proof of transformed runtime code
 * or Minecraft acceptance. Never prints an installation path or exception.
 */
public final class InstalledArtifactReceipt {
    private InstalledArtifactReceipt() {}
    static String sha256(List<Path> origin) {
        try {
            if (origin.size()!=1) return "UNKNOWN";
            Path file=origin.getFirst();
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")
                    || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || Files.size(file)>8_388_608) return "UNKNOWN";
            try(var stream=Files.newInputStream(file)) {
                byte[] data=stream.readNBytes(8_388_609);
                if(data.length>8_388_608) return "UNKNOWN";
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
            }
        } catch(Exception unavailable) { return "UNKNOWN"; }
    }
    /** Installed artifact SHA only; does not certify transforms or the runtime. */
    public static String installedSha256() {
        String sha="UNKNOWN";
        try {
            var container=FabricLoader.getInstance().getModContainer("maestroai").orElseThrow();
            var origin=container.getOrigin();
            if(origin.getKind()==ModOrigin.Kind.PATH) sha=sha256(origin.getPaths());
        } catch(RuntimeException unavailable) { /* Fixed UNKNOWN, never exception/path contents. */ }
        return sha;
    }
    public static String capture() {
        return "[MaestroAI] Installed artifact sha256="+installedSha256()
                +" runtimeTransformation=UNATTESTED";
    }
}
