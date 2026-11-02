package com.maestroai.training;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Identity-UNBOUND public appearance candidate, never true opponent species.
 * A switch display can be an Illusion; a public dex lookup does not turn it
 * into an observed type. In particular no immunity can be certified here.
 */
public final class PublicOpponentAppearance {
    private PublicOpponentAppearance(){}

    /**
     * A literal spectator replace announcement identifies the CURRENT true
     * displayed species after Illusion breaks. It is not cross-switch custody,
     * and no separate form/Transform change may have occurred afterwards.
     */
    public static String witnessedRevealSpecies(String slot,List<String> events){
        if(slot==null || events==null || !Set.of("p1a","p2a").contains(slot))
            return null;
        String revealed=null;
        boolean entered=false;
        for(String line:events){
            if(line==null)return null;
            String[] p=line.split("\\|",-1);
            if(p.length<3 || !p[2].startsWith(slot+": "))continue;
            String kind=p[1];
            if(Set.of("switch","drag").contains(kind)){
                entered=true;revealed=null;
            }else if(kind.equals("replace")){
                revealed=null;
                if(!entered || p.length<4)continue;
                String raw=p[3].split(",",-1)[0].trim();
                if(raw.matches("[A-Za-z0-9 .:'-]{1,75}")){
                    String id=raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
                    if(id.length()>0 && id.length()<=64)revealed=id;
                }
            }else if(Set.of("detailschange","-formechange","-mega",
                    "-transform","faint").contains(kind)){
                revealed=null;
            }
        }
        return revealed;
    }

    public static List<String> displayedTypeHypothesis(String slot,
            List<String> originalPublicEvents,
            Function<String,List<String>> currentPublicSpeciesCatalog) {
        if(slot==null || !Set.of("p1a","p2a").contains(slot)
                || originalPublicEvents==null || currentPublicSpeciesCatalog==null)
            return List.of();
        String displayed=null;
        for(String line:originalPublicEvents) {
            if(line==null) return List.of();
            String[] fields=line.split("\\|",-1);
            if(fields.length<3 || !fields[2].startsWith(slot+": "))continue;
            String kind=fields[1];
            if(kind.equals("switch") || kind.equals("drag")) {
                displayed=null;
                if(fields.length<5) return List.of();
                String raw=fields[3].split(",",-1)[0].trim();
                if(raw.length()<1 || raw.length()>75
                        || !raw.matches("[A-Za-z0-9 .:'-]{1,75}"))
                    continue;
                String id=raw.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]","");
                if(!id.isBlank() && id.length()<=64)displayed=id;
            } else if(Set.of("replace","detailschange","-formechange",
                    "-mega","-transform","-terastallize","faint").contains(kind)){
                // Even a public actual species revelation is NOT a certificate
                // that its ordinary default typing is still the current type.
                displayed=null;
            } else if(kind.equals("-start") || kind.equals("-end")) {
                if(fields.length>3 && Set.of("typechange","typeadd")
                        .contains(fields[3].toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9]","")))
                    displayed=null;
            }
        }
        if(displayed==null)return List.of();
        return registryTypes(displayed,currentPublicSpeciesCatalog);
    }

    /** Public rules lookup ONLY. The caller must independently establish
     * whether the ID is a confirmed species or a hypothetical display. */
    public static List<String> registryTypes(String speciesId,
            Function<String,List<String>> catalog){
        if(speciesId==null || !speciesId.matches("[a-z0-9]{1,64}")
                || catalog==null)return List.of();
        try{
            List<String> types=catalog.apply(speciesId);
            if(types==null || types.isEmpty() || types.size()>2)return List.of();
            for(String type:types) {
                if(type==null || !Set.of("normal","fire","water","electric",
                        "grass","ice","fighting","poison","ground","flying",
                        "psychic","bug","rock","ghost","dragon","dark",
                        "steel","fairy").contains(type))return List.of();
            }
            if(types.size()==2 && types.get(0).equals(types.get(1)))return List.of();
            return List.copyOf(types);
        }catch(RuntimeException unavailable){return List.of();}
    }
}
