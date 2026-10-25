package com.maestroai.training;

import com.maestroai.ai.SimulatorDomainBridge;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Run the production boot audit in a disposable installer working directory.
 * Only class-resource lookup is hosted; no simulator/Minecraft boot is claimed.
 */
public final class BootProfileRegression {
    public static final class SourceHost { public SourceHost() {} }
    private static void check(boolean ok) { if(!ok)throw new AssertionError("boot profile evidence"); }
    public static void main(String[] args) throws Exception {
        try(var dependency=new ZipFile(args[0])) {
            var loader=new ClassLoader(BootProfileRegression.class.getClassLoader()) {
                Class<?> host() throws Exception {
                    String name=SourceHost.class.getName();
                    try(var input=SourceHost.class.getResourceAsStream("/"+name.replace('.','/')+".class")) {
                        byte[] code=input.readAllBytes();return defineClass(name,code,0,code.length);
                    }
                }
                @Override public java.io.InputStream getResourceAsStream(String name) {
                    var entry=dependency.getEntry(name);
                    if(entry!=null)try{return dependency.getInputStream(entry);}catch(Exception unavailable){return null;}
                    return super.getResourceAsStream(name);
                }
            };
            Object service=loader.host().getConstructor().newInstance();
            Path marker=Path.of("showdown/MSDPatch.json");byte[] saved=Files.readAllBytes(marker);
            SimulatorDomainBridge.bootStarting(service);
            check(SimulatorDomainBridge.unavailable(service).equals(Set.of("SIMULATOR_CONTEXT_NOT_ATTESTED")));
            // File source GO is deliberately insufficient to attest an initialized context.
            try {
                Files.writeString(marker,"{\"version\":\"1.0.0\",\"private\":\"NEVER_LOG_ME\"}");
                SimulatorDomainBridge.bootStarting(service);
                var rejected=SimulatorDomainBridge.unavailable(service);
                check(rejected.containsAll(Set.of("LOADED_SIMULATOR_SOURCE_UNVERIFIED",
                        "SOURCE_AUDIT_UNEXPECTED_SOURCE_FILE","PACK_SOURCE_AUDIT_FILE_HASH_MISMATCH",
                        "PACK_SOURCE_REFERENCE:MSDPatch.json")));
                check(rejected.stream().noneMatch(x->x.contains("NEVER_LOG_ME")||x.contains("private")));
            } finally {Files.write(marker,saved);}
            Path unknown=Path.of("showdown/PRIVATE_PLAYER_SETTINGS.json");
            try {
                Files.writeString(unknown,"NEVER_LOG_ME");SimulatorDomainBridge.bootStarting(service);
                var rejected=SimulatorDomainBridge.unavailable(service);
                check(rejected.contains("PACK_SOURCE_AUDIT_UNEXPECTED_SOURCE_FILE"));
                check(rejected.stream().noneMatch(x->x.contains("PRIVATE")||x.contains("NEVER_LOG_ME")||x.contains("REFERENCE:")));
            } finally {Files.deleteIfExists(unknown);}
            SimulatorDomainBridge.bootStarting(service);
            check(SimulatorDomainBridge.unavailable(service).equals(Set.of("SIMULATOR_CONTEXT_NOT_ATTESTED")));
            System.out.println("Production boot profile audit PASS: exact installer, separate failed-profile reasons, privacy and context abstention");
        }
    }
}
