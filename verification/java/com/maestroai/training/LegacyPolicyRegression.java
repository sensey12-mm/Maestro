package com.maestroai.training;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Executes compiled production decision methods in finite public synthetic hosts.
 * Damage/effectiveness and scored move features are explicitly supplied inputs;
 * Minecraft feature extraction, mixin loading and match win rates are not tested.
 * The switch ranking, final switch gate, setup/recovery terms and variation are
 * production bytecode, never a handwritten replacement policy. */
public final class LegacyPolicyRegression {
    private static final String AI="com/maestroai/ai/MaestroBattleAI";
    private static final Set<String> METHODS=Set.of("chooseLegacy","findBestMove","evaluateTwoPlyPosition",
            "evaluateCurrentPosition","findBestSwitch","evaluateSwitchPosition","applyPivotDestinationValue",
            "currentPokemonWouldDieBeforePivot","predictedSwitchSetupPenalty","shouldPreserveCurrentPokemon",
            "isWinningPosition","selectControlledVariation","isGuaranteedKO","isKOOnHit","getHpPercent",
            "isRecoveryMove","normalize","isContinuationLocked","findUsableStruggle","speciesId","isDitto",
            "hasGuaranteedKO","likelyIgnoresOpponentBoosts","likelyHasPriority","likelySetupSweeper","isWinCondition","likelyHasSuckerPunch");
    private static Class<?> compiled;
    private static Class<?> compiled() throws Exception {
        if(compiled!=null)return compiled;
        var mapping=new HashMap<String,String>();
        mapping.put(AI,Type.getInternalName(Controller.class));
        mapping.put(AI+"$MoveChoice",Type.getInternalName(Choice.class));
        mapping.put(AI+"$SwitchChoice",Type.getInternalName(Switch.class));
        String prefix="com/cobblemon/mod/common/";
        mapping.put(prefix+"pokemon/Pokemon",Type.getInternalName(Mon.class));
        mapping.put(prefix+"pokemon/Species",Type.getInternalName(Species.class));
        mapping.put(prefix+"battles/pokemon/BattlePokemon",Type.getInternalName(BattleMon.class));
        mapping.put(prefix+"battles/ActiveBattlePokemon",Type.getInternalName(Active.class));
        mapping.put(prefix+"api/battles/model/actor/BattleActor",Type.getInternalName(Actor.class));
        mapping.put(prefix+"api/battles/model/PokemonBattle",Type.getInternalName(Battle.class));
        mapping.put(prefix+"battles/ShowdownMoveset",Type.getInternalName(Moveset.class));
        mapping.put(prefix+"battles/InBattleMove",Type.getInternalName(Move.class));
        mapping.put(prefix+"battles/BattleSide",Type.getInternalName(Side.class));
        mapping.put(prefix+"battles/ShowdownActionResponse",Type.getInternalName(Response.class));
        mapping.put(prefix+"battles/SwitchActionResponse",Type.getInternalName(SwitchResponse.class));
        mapping.put("com/gitlab/srcmc/rctapi/api/ai/RCTBattleAI",Type.getInternalName(Parent.class));
        var remapper=new Remapper(){@Override public String map(String name){return mapping.getOrDefault(name,name);}};
        var node=new ClassNode();
        try(var in=Controller.class.getResourceAsStream("/"+Type.getInternalName(Controller.class)+".class")){new ClassReader(in.readAllBytes()).accept(node,0);}
        var original=new ClassNode();
        try(var in=LegacyPolicyRegression.class.getResourceAsStream("/"+AI+".class")){new ClassReader(in.readAllBytes()).accept(new ClassRemapper(original,remapper),0);}
        var copied=new HashSet<String>();
        for(var method:original.methods)if(METHODS.contains(method.name)||method.name.startsWith("lambda$chooseLegacy$")) {
            method.access=(method.access&~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED))|Opcodes.ACC_PUBLIC;
            node.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));node.methods.add(method);copied.add(method.name);
        }
        if(!copied.containsAll(METHODS))throw new AssertionError("Compiled policy method inventory changed: "+copied);
        // Check every production self-call against the copied or explicit host
        // inventory. Unknown dependencies must fail instead of returning zero.
        var signatures=new HashSet<String>();node.methods.forEach(m->signatures.add(m.name+m.desc));
        for(var m:node.methods)for(var ins:m.instructions)if(ins instanceof MethodInsnNode call && call.owner.equals(Type.getInternalName(Controller.class))
                && !signatures.contains(call.name+call.desc))throw new AssertionError("Unhandled production dependency: "+call.name+call.desc);
        var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);byte[] rewritten=writer.toByteArray();
        String host=Type.getInternalName(Controller.class).replace('/','.');
        var loader=new ClassLoader(LegacyPolicyRegression.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.equals(host))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){Class<?> c=findLoadedClass(name);if(c==null)c=defineClass(name,rewritten,0,rewritten.length);if(resolve)resolveClass(c);return c;}
            }
        };
        return compiled=loader.loadClass(host);
    }
    public static final class Mon {
        public final UUID id;public final String species;public int hp,max,speed;public double outgoing,incoming,effectiveness=1,incomingEffectiveness=1;
        public final Set<String> moves=new HashSet<>();
        public Mon(int id,String species,int hp,int max,int speed,double outgoing,double incoming){this.id=new UUID(0,id);this.species=species;this.hp=hp;this.max=max;this.speed=speed;this.outgoing=outgoing;this.incoming=incoming;}
        public int getCurrentHealth(){return hp;}public int getMaxHealth(){return max;}public int getSpeed(){return speed;}
        public UUID getUuid(){return id;}public Species getSpecies(){return new Species(species);}
    }
    public record Species(String name){public String getName(){return name;}}
    public static final class BattleMon {
        public final Mon mon;public boolean gone,sent,eligible=true;
        public BattleMon(Mon mon){this.mon=mon;}public Mon getEffectedPokemon(){return mon;}public UUID getUuid(){return mon.id;}
        public int getHealth(){return mon.hp;}public boolean getGone(){return gone;}public boolean isSentOut(){return sent;}public boolean canBeSentOut(){return eligible;}
    }
    public record Actor(List<BattleMon> pokemon){public List<BattleMon> getPokemonList(){return pokemon;}}
    public record Active(BattleMon mon,Actor actor,String pnx){public BattleMon getBattlePokemon(){return mon;}public Actor getActor(){return actor;}public String getPNX(){return pnx;}}
    public record Battle(int turn){public int getTurn(){return turn;}}
    public record Moveset(List<Move> moves,boolean trapped){public List<Move> getMoves(){return moves;}public boolean getTrapped(){return trapped;}}
    public record Move(String id,boolean usable){public String getId(){return id;}public boolean canBeUsed(){return usable;}}
    public static final class Side {}
    public static class Response {public final String action;public Response(String action){this.action=action;}}
    public static final class SwitchResponse extends Response {public SwitchResponse(UUID id){super("switch:"+id);}}
    public static class Parent {public Response choose(Active a,Battle b,Side side,Moveset moves,boolean force){return new Response("fallback");}}
    public static final class Choice {
        public String moveId,moveName;public double score,strategicAdjustment,finalScore,accuracy=100,averageDamage,effectiveness=1;
        public int estimatedDamage,priority;public boolean damageMove,setupMove,pivotMove;
        public Choice(String id,double score,double average,int minimum,int priority){moveId=moveName=id;this.score=score;averageDamage=average;estimatedDamage=minimum;this.priority=priority;damageMove=average>0;setupMove=id.equals("swordsdance")||id.equals("nastyplot");pivotMove=id.equals("uturn");}
    }
    public static final class Switch {
        public BattleMon pokemon;public double score,currentThreat,offensivePotential,offensiveDamage,incomingDamage;
        public Switch(BattleMon p,double s,double t,double o,double d,double i){pokemon=p;score=s;currentThreat=t;offensivePotential=o;offensiveDamage=d;incomingDamage=i;}
    }
    public static class Controller extends Parent {
        public Active foe;public List<Choice> choices;public Map<UUID,String> lastSelectedMoves=new HashMap<>();
        public UUID lastSwitchDestination;public int lastSwitchTurn=-100,visits,actionCount;public boolean scored;
        public PublicBattleObserver publicProtocolObserver=new PublicBattleObserver();
        public Active getOpponent(Active a){return foe;}
        public void resetMemoryForNewBattle(Battle b){}public void syncObservedTeraState(Battle b){}
        public void syncPublicMoveHistory(Battle b){}public void syncPublicProtocolObservation(Battle b){}
        public void incrementActionCount(BattleMon b){}public static String normalizePublicSpecies(String s){return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
        public void rememberMatchupVisit(Mon a,Mon b){}public List<Choice> findAllMoves(Moveset m,Mon a,Mon b){scored=true;return choices;}
        public BattleSnapshot observeTacticalSnapshot(Battle b,Active a,Active f,Mon m,List<Choice> c,boolean s,boolean l,boolean t){return null;}
        public void clearRememberedMove(BattleMon b){}public void clearSetupHistory(Mon m){}
        public void logSwitchDecision(Battle b,Mon a,Mon f,Switch s,String why){}
        public void rememberSwitchDestination(Battle b,Switch s){}
        public Response createMoveResponse(Active a,Moveset m,String id){return m.moves().stream().anyMatch(v->v.usable()&&v.id().equals(id))?new Response("move:"+id):null;}
        public String lastSelectedMoveForSpecies(Mon m){return lastSelectedMoves.get(m.id);}
        public int getMatchupVisits(Mon a,Mon b){return visits;}
        public double getBestEstimatedDamage(Mon a,Mon b){return a==foe.mon().mon?b.incoming:a.outgoing;}
        public double getBestIncomingEffectiveness(Mon a,Mon b){return b.incomingEffectiveness;}
        public double getBestOffensiveEffectiveness(Mon a,Mon b){return a.effectiveness;}
        public double estimateSuckerPunchDamage(Mon a,Mon b){throw new AssertionError("Sucker Punch extraction outside declared fixture");}
        public List<Choice> movesForPokemon(Mon a,Mon b){return a==foe.mon().mon?List.of():choices;}
        public boolean hasGuaranteedKO(List<Choice> c,Mon m){return c.stream().anyMatch(x->x.damageMove&&x.estimatedDamage>=m.hp&&x.accuracy>=100);}
        public boolean hasMove(Mon m,String id){return m.moves.contains(id);}
        public int getActionCountForMove(String id){return actionCount;}
        public void logCrossActionShadow(BattleSnapshot s,String a,String b,String c,double d,double e){}
        public void logShadowComparison(BattleSnapshot s,Choice c,List<Choice> choices){}
        public void rememberMove(BattleMon m,String id){}public void rememberStrategicAction(Mon m,Choice c){}
        public void logMoveDecision(Battle b,Mon a,Mon f,Choice c,Switch s){}
    }
    static String choose(int hp,List<Choice> choices,double incoming,boolean trapped,boolean force,boolean benchEligible,String previous) throws Exception {
        var mine=new Mon(11,"Arceus",hp,381,220,100,incoming);
        var reserve=new Mon(12,"Marshadow",321,321,286,100,0);reserve.incomingEffectiveness=0;
        var foe=new Mon(13,"Mewtwo",416,416,394,100,0);
        return choose(mine,reserve,foe,choices,trapped,force,benchEligible,previous,null);
    }
    @SuppressWarnings("unchecked")
    static String choose(Mon mine,Mon reserve,Mon foe,List<Choice> choices,boolean trapped,boolean force,boolean eligible,String previous,String publicSpecies) throws Exception {
        var type=compiled();var controller=type.getConstructor().newInstance();
        var mb=new BattleMon(mine);mb.sent=true;var rb=new BattleMon(reserve);rb.eligible=eligible;
        var active=new Active(mb,new Actor(List.of(mb,rb)),"p1a");var enemy=new Active(new BattleMon(foe),new Actor(List.of()),"p2a");
        type.getField("foe").set(controller,enemy);type.getField("choices").set(controller,choices);
        if(previous!=null)((Map<UUID,String>)type.getField("lastSelectedMoves").get(controller)).put(mine.id,previous);
        if(publicSpecies!=null)((PublicBattleObserver)type.getField("publicProtocolObserver").get(controller)).accept("|switch|p2a: Public|"+publicSpecies+", L100|100/100");
        var request=new Moveset(choices.stream().map(c->new Move(c.moveId,true)).toList(),trapped);
        var result=(Response)type.getMethod("chooseLegacy",Active.class,Battle.class,Side.class,Moveset.class,boolean.class).invoke(controller,active,new Battle(2),new Side(),request,force);
        if(publicSpecies!=null&&!publicSpecies.equalsIgnoreCase(foe.species)&&type.getField("scored").getBoolean(controller))throw new AssertionError("Identity mismatch reached feature scoring");
        return result.action.startsWith("switch:")?"switch:reserve":result.action;
    }
    static List<Choice> moves(String... ids) {
        return Arrays.stream(ids).map(id->new Choice(id,id.equals("shadowclaw")?100:0,id.equals("shadowclaw")?100:0,id.equals("shadowclaw")?85:0,0)).toList();
    }
    static void run() throws Exception {
        var dying=choose(1,moves("shadowclaw","swordsdance","recover"),100,false,false,true,null);
        if(!dying.equals("switch:reserve"))throw new AssertionError("Final chooser failed lethal switch: "+dying);
        if(choose(1,moves("shadowclaw"),100,true,false,true,null).startsWith("switch"))throw new AssertionError("Trapped policy switched");
        if(choose(1,moves("shadowclaw"),100,false,false,false,null).startsWith("switch"))throw new AssertionError("Ineligible reserve selected");
        if(!choose(200,moves("shadowclaw"),100,true,true,true,null).equals("switch:reserve"))throw new AssertionError("Forced replacement not chosen");
        if(!choose(200,moves(),100,false,false,true,null).equals("switch:reserve"))throw new AssertionError("Empty moves failed legal switch");
        if(!choose(1,moves("geomancy"),100,false,false,true,"geomancy").equals("move:geomancy"))throw new AssertionError("Continuation bypassed");
        var mine=new Mon(11,"Mew",381,381,500,500,0);var foe=new Mon(13,"Mewtwo",100,416,394,50,0);var bench=new Mon(12,"Marshadow",321,321,286,100,0);
        if(!choose(mine,bench,foe,List.of(new Choice("psychic",500,500,500,0)),false,false,true,null,null).equals("move:psychic"))throw new AssertionError("Guaranteed KO switched away");
        if(!choose(mine,bench,foe,moves("recover"),false,false,true,null,"Gengar").equals("move:recover"))throw new AssertionError("Appearance fallback changed");
        var type=compiled();var controller=type.getConstructor().newInstance();
        var best=new Choice("tackle",0,0,0,0);best.finalScore=100;
        var second=new Choice("scratch",0,0,0,0);second.finalScore=90;
        var variations=new HashSet<String>();
        for(int n=0;n<7;n++) {
            type.getField("actionCount").setInt(controller,n);
            var selected=(Choice)type.getMethod("selectControlledVariation",List.class,Choice.class).invoke(controller,List.of(best,second),best);
            variations.add(selected.moveId);
        }
        if(!variations.equals(Set.of("tackle","scratch")))throw new AssertionError("Compiled deterministic variation did not exercise both branches");
        second.finalScore=54;
        for(int n=0;n<7;n++) {
            type.getField("actionCount").setInt(controller,n);
            if(type.getMethod("selectControlledVariation",List.class,Choice.class).invoke(controller,List.of(best,second),best)!=best)throw new AssertionError("Variation discarded a large advantage");
        }
        System.out.println("Compiled legacy policy PASS: 10 fixed branch cases; actual switch ranking/final gate, legality, continuation, forced/no-move, KO, identity guard and bounded variation; explicit feature hosts");
    }
}
