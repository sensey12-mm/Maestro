package com.maestroai.ai;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.ArrayList;
import java.util.List;

/**
 * Public registry metadata only: NEVER reads an opponent Pokemon, battle
 * actor, request, party, ability, item, or live team object. The observed
 * public DISPLAY is merely a conditional match against the installed species
 * registry; this does not certify actual identity under Illusion.
 */
public final class LivePublicSpeciesTyping {
    private LivePublicSpeciesTyping(){}

    public static List<String> displayedTypeHypothesis(String displayedId) {
        if(displayedId==null || !displayedId.matches("[a-z0-9]{1,64}"))
            return List.of();
        try {
            if(!MaestroBattleAI.publicCobblemon173Installed())return List.of();
            Species unique=null;
            for(Species entry:PokemonSpecies.getSpecies()){
                if(entry==null || !displayedId.equals(entry.showdownId()))continue;
                if(unique!=null)return List.of(); // duplicate ID is ambiguous
                unique=entry;
            }
            if(unique==null
                    || !"cobblemon".equals(unique.getResourceIdentifier().getNamespace()))
                return List.of();
            var types=new ArrayList<String>();
            for(var type:unique.getTypes()){
                if(type==null || type.getShowdownId()==null)return List.of();
                types.add(type.getShowdownId());
            }
            return types.isEmpty() || types.size()>2?List.of():List.copyOf(types);
        }catch(RuntimeException | LinkageError unsupported){
            return List.of();
        }
    }
}
