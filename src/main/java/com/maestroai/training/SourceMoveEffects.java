package com.maestroai.training;
import java.util.*;
/** Explicit pinned-gen-nine secondary effects. These branches do not assign
 * opponent action probabilities, and do not imply a win-rate claim. */
public final class SourceMoveEffects {
    private SourceMoveEffects(){}
    public record Secondary(int chance,String status,Map<String,Integer> targetStages,boolean flinch) {
        public Secondary {
            if(chance<1||chance>100||!Set.of("none","brn","par","frz").contains(status))
                throw new IllegalArgumentException("invalid source secondary");
            targetStages=Map.copyOf(targetStages);
        }
    }
    public static final Map<String,Secondary> SECONDARIES=Map.ofEntries(
        Map.entry("ironhead",new Secondary(30,"none",Map.of(),true)),
        Map.entry("icebeam",new Secondary(10,"frz",Map.of(),false)),
        Map.entry("flamethrower",new Secondary(10,"brn",Map.of(),false)),
        Map.entry("energyball",new Secondary(10,"none",Map.of("spd",-1),false)),
        Map.entry("moonblast",new Secondary(30,"none",Map.of("spa",-1),false)),
        Map.entry("focusblast",new Secondary(10,"none",Map.of("spd",-1),false)),
        Map.entry("thunder",new Secondary(30,"par",Map.of(),false)),
        Map.entry("fireblast",new Secondary(10,"brn",Map.of(),false)),
        Map.entry("rocktomb",new Secondary(100,"none",Map.of("spe",-1),false))
    );
    public static boolean modeledSecondary(String id){return SECONDARIES.containsKey(id);}
}
