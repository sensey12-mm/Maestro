package com.maestroai.training;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Real compiled legacy move ranking over explicit controlled feature inputs.
 * Includes its score/KO/recovery/priority/setup terms, not live hidden-state
 * feature extraction or the separate final switch policy. No policy-win claim. */
public final class LegacyScorerRegression {
    private static final String AI="com/maestroai/ai/MaestroBattleAI";
    private static byte[] bytes(Class<?> c) throws Exception {
        try(var in=c.getResourceAsStream("/"+Type.getInternalName(c)+".class")){return in.readAllBytes();}
    }
    static String choose(int hp,int max,int speed,int foeHp,int foeMax,int foeSpeed,double incoming,List<String> actions) throws Exception {
        String host=Type.getInternalName(Controller.class);
        var mapping=Map.of(AI,host,AI+"$MoveChoice",Type.getInternalName(Choice.class),AI+"$SwitchChoice",Type.getInternalName(Switch.class),
                "com/cobblemon/mod/common/pokemon/Pokemon",Type.getInternalName(Mon.class));
        var remapper=new Remapper(){@Override public String map(String name){return mapping.getOrDefault(name,name);}};
        var node=new ClassNode();new ClassReader(bytes(Controller.class)).accept(node,0);
        var original=new ClassNode();
        try(var in=LegacyScorerRegression.class.getResourceAsStream("/"+AI+".class")) {
            new ClassReader(in.readAllBytes()).accept(new ClassRemapper(original,remapper),0);
        }
        Set<String> names=Set.of("findBestMove","evaluateTwoPlyPosition","isGuaranteedKO","isKOOnHit","getHpPercent","isRecoveryMove","normalize");
        int copied=0;
        for(var method:original.methods)if(names.contains(method.name)) {
            method.access=(method.access&~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED))|Opcodes.ACC_PUBLIC;
            node.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));node.methods.add(method);copied++;
        }
        if(copied!=names.size())throw new AssertionError("Legacy source ranking method inventory changed");
        var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);byte[] rewritten=writer.toByteArray();
        var loader=new ClassLoader(LegacyScorerRegression.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.equals(host.replace('/','.')))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){Class<?> c=findLoadedClass(name);if(c==null)c=defineClass(name,rewritten,0,rewritten.length);if(resolve)resolveClass(c);return c;}
            }
        };
        var type=loader.loadClass(host.replace('/','.'));var controller=type.getConstructor().newInstance();
        type.getField("incoming").setDouble(controller,incoming);
        var choices=actions.stream().map(id->{
            var c=new Choice();c.moveId=c.moveName=id;c.damageMove=id.equals("shadowclaw")||id.equals("extremespeed");
            c.setupMove=id.equals("swordsdance");c.accuracy=100;c.priority=id.equals("extremespeed")?2:0;
            // Fixed pre-scoring features are declared, not borrowed from Shadow.
            c.score=c.averageDamage=c.damageMove?100:0;c.estimatedDamage=c.damageMove?85:0;return c;
        }).toList();
        var selected=(Choice)type.getMethod("findBestMove",List.class,Mon.class,Mon.class,Switch.class)
                .invoke(controller,choices,new Mon(hp,max,speed),new Mon(foeHp,foeMax,foeSpeed),null);
        return "move:"+selected.moveId;
    }
    public record Mon(int hp,int max,int speed) {
        public int getCurrentHealth(){return hp;}public int getMaxHealth(){return max;}public int getSpeed(){return speed;}
    }
    public static final class Choice {
        public String moveId,moveName;public double score,strategicAdjustment,finalScore,accuracy,averageDamage;
        public int estimatedDamage,priority;public boolean damageMove,setupMove,pivotMove;
    }
    public static final class Switch { public double score; }
    public static final class Controller {
        public double incoming;
        public String lastSelectedMoveForSpecies(Mon m){return null;}
        public int getMatchupVisits(Mon a,Mon b){return 0;}
        public double getBestEstimatedDamage(Mon a,Mon b){return incoming;}
        public boolean likelyHasSuckerPunch(Mon m){return false;}
        public double estimateSuckerPunchDamage(Mon a,Mon b){return 0;}
        public boolean likelyIgnoresOpponentBoosts(Mon m){return false;}
        public double getBestIncomingEffectiveness(Mon a,Mon b){return 1;}
        public List<Choice> movesForPokemon(Mon a,Mon b){return List.of();}
        public boolean hasGuaranteedKO(List<Choice> moves,Mon m){return false;}
        public boolean likelySetupSweeper(Mon m){return false;}
        public boolean isDitto(Mon m){return false;}
    }
}
