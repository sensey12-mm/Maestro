package com.maestroai.ai;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Public raid-membership metadata only. Never read raid actors or requests.
 * Raid Dens can install silent volatile effects, so missing announcements cannot
 * establish ordinary battle mode. Its source-pinned boolean API is required.
 */
public final class RuntimeBattleMode {
    private RuntimeBattleMode() {}
    private record Adapter(Class<?> battle, Class<?> contract, Method getter) {}
    private static final class Pinned { static final Adapter VALUE=load(); }
    private static Adapter load() {
        try {
            var loader=RuntimeBattleMode.class.getClassLoader();
            try(var manifest=loader.getResourceAsStream("com/maestroai/training/runtime-pack-raid-classes.tsv")){
                if(manifest==null)return null;
                var lines=new String(manifest.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
                if(lines.size()!=2)return null;
                for(String line:lines){
                    var p=line.split("\t");if(p.length!=2)return null;
                    try(var input=loader.getResourceAsStream(p[0])){
                        if(input==null || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                .digest(input.readNBytes(65537))).equals(p[1]))return null;
                    }
                }
            }
            var battle=Class.forName("com.cobblemon.mod.common.api.battles.model.PokemonBattle",false,loader);
            var contract=Class.forName("com.necro.raid.dens.common.util.IRaidBattle",false,loader);
            var getter=battle.getDeclaredMethod("crd_isRaidBattle");
            if(!contract.isAssignableFrom(battle)||getter.getReturnType()!=boolean.class
                    ||!Modifier.isPublic(getter.getModifiers()))return null;
            return new Adapter(battle,contract,getter);
        }catch(Exception|LinkageError unavailable){return null;}
    }
    public static boolean ordinary(Object battle) {
        if(battle==null)return false;
        try {
            if(!FabricLoader.getInstance().isModLoaded("cobblemonraiddens"))return true;
            var adapter=Pinned.VALUE;
            return adapter!=null && battle.getClass()==adapter.battle() && adapter.contract().isInstance(battle)
                    && Boolean.FALSE.equals(adapter.getter().invoke(battle));
        }catch(Exception|LinkageError unavailable){return false;}
    }
}
