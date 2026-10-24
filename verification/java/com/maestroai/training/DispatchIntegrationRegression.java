package com.maestroai.training;

import com.maestroai.ai.OwnChoiceRecovery;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

/** Executes pinned native dispatch/actor methods and compiled production hooks.
 * Isolated hosts replace Minecraft/network/tactical selection, not the tested
 * callback ordering, production recovery or native response serialization.
 * Reweaving these finite hooks is not proof of Fabric application in a modpack.
 * No upstream bytecode is retained in Git or distributed in the mod JAR.
 */
public final class DispatchIntegrationRegression {
    private static final String ROOT = "com/cobblemon/mod/common/";
    private static final String AI = "com/maestroai/ai/MaestroBattleAI";
    private static final String F = Type.getInternalName(Fixture.class);
    private static final Map<String,String> TYPES = new HashMap<>();
    private static void bind(String source, Class<?> target) { TYPES.put(source,Type.getInternalName(target)); }
    static {
        bind(ROOT+"api/battles/model/PokemonBattle",Fixture.Battle.class);
        bind(ROOT+"api/battles/model/actor/BattleActor",Fixture.Actor.class);
        bind(ROOT+"api/battles/model/actor/AIBattleActor",Fixture.Actor.class);
        bind(ROOT+"api/battles/model/ai/BattleAI",Fixture.AI.class);
        bind(ROOT+"battles/ActiveBattlePokemon",Fixture.Active.class);
        bind(ROOT+"battles/pokemon/BattlePokemon",Fixture.Mon.class);
        bind(ROOT+"battles/BattleSide",Fixture.Side.class);
        bind(ROOT+"battles/ShowdownActionRequest",Fixture.Request.class);
        bind(ROOT+"battles/ShowdownMoveset",Fixture.Moves.class);
        bind(ROOT+"battles/ShowdownActionResponse",Fixture.Response.class);
        bind(ROOT+"battles/MoveActionResponse",Fixture.Move.class);
        bind(ROOT+"battles/InBattleMove",Fixture.MoveSlot.class);
        bind(ROOT+"battles/Targetable",Fixture.Active.class);
        bind(ROOT+"battles/SwitchActionResponse",Fixture.Switch.class);
        bind(ROOT+"battles/PassActionResponse",Fixture.Pass.class);
        bind(ROOT+"battles/ForcePassActionResponse",Fixture.ForcePass.class);
        bind(ROOT+"api/battles/interpreter/BattleMessage",Fixture.Message.class);
        bind(ROOT+"api/net/NetworkPacket",Fixture.Packet.class);
        bind(ROOT+"net/messages/client/battle/BattleMakeChoicePacket",Fixture.MakeChoice.class);
        bind(ROOT+"net/messages/client/battle/BattleQueueRequestPacket",Fixture.QueueRequest.class);
        bind(ROOT+"net/messages/client/battle/BattleMadeInvalidChoicePacket",Fixture.InvalidChoice.class);
        bind(ROOT+"battles/interpreter/instructions/RequestInstruction",Fixture.RequestInstruction.class);
        bind(ROOT+"battles/interpreter/instructions/ErrorInstruction",Fixture.ErrorInstruction.class);
        bind(ROOT+"battles/interpreter/instructions/TurnInstruction",Fixture.TurnInstruction.class);
        bind(ROOT+"battles/dispatch/DispatchResult",Fixture.Result.class);
        bind(ROOT+"battles/dispatch/DispatchResultKt",Fixture.Results.class);
        bind(ROOT+"util/LocalizationUtilsKt",Fixture.Texts.class);
        bind(ROOT+"api/text/TextKt",Fixture.Texts.class);
        bind(ROOT+"exception/IllegalActionChoiceException",Fixture.IllegalChoice.class);
        bind(ROOT+"Cobblemon",Fixture.LogHost.class);
        bind("net/minecraft/class_5250",Fixture.Text.class);
        bind("net/minecraft/class_2561",Fixture.Text.class);
        bind(AI,Fixture.Controller.class);
        for(String mixin:List.of("OwnRequestDispatchMixin","OwnChoiceErrorMixin","OwnTurnDispatchMixin"))
            bind("com/maestroai/mixin/"+mixin,Fixture.Hooks.class);
    }
    private static final Remapper REMAPPER = new Remapper() {
        @Override public String map(String name) {
            return TYPES.getOrDefault(name,name.startsWith(ROOT)||name.startsWith("net/minecraft/")
                    ? Type.getInternalName(Fixture.Unused.class):name);
        }
    };
    private static ClassNode read(byte[] bytes) {
        var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;
    }
    private static byte[] resource(String name) throws Exception {
        try(InputStream s=DispatchIntegrationRegression.class.getResourceAsStream("/"+name+".class")) {
            if(s==null)throw new AssertionError("Missing integration resource: "+name);
            return s.readAllBytes();
        }
    }
    private static void transplant(Map<String,ClassNode> hosts, byte[] original, String target,
            java.util.function.Predicate<String> selected, java.util.function.Consumer<MethodNode> weave) {
        var remapped=new ClassNode();new ClassReader(original).accept(new ClassRemapper(remapped,REMAPPER),0);
        var host=hosts.get(target);
        for(var method:remapped.methods) if(selected.test(method.name)) {
            method.access=(method.access & ~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED|Opcodes.ACC_FINAL))|Opcodes.ACC_PUBLIC;
            method.visibleAnnotations=null;method.invisibleAnnotations=null;
            weave.accept(method);
            host.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));
            host.methods.add(method);
        }
    }
    private static void hook(MethodNode method,String name,String descriptor,boolean head) {
        for(var insn:method.instructions.toArray()) if(head || insn.getOpcode()>=Opcodes.IRETURN&&insn.getOpcode()<=Opcodes.RETURN) {
            var call=new InsnList();
            Type[] args=Type.getArgumentTypes(method.desc);int slot=0;
            for(Type arg:args) {call.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD),slot));slot+=arg.getSize();}
            call.add(new InsnNode(Opcodes.ACONST_NULL));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC,F+"$Hooks",name,descriptor,false));
            if(head)method.instructions.insert(call);else method.instructions.insertBefore(insn,call);
            if(head)break;
        }
    }
    public static void run(Path dependency) throws Exception {
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dependency)));
        if(!hash.equals("f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3"))
            throw new AssertionError("Unpinned dispatch dependency");
        Map<String,ClassNode> hosts=new HashMap<>();
        ArrayDeque<Class<?>> pending=new ArrayDeque<>();pending.add(Fixture.class);
        while(!pending.isEmpty()) {
            Class<?> c=pending.remove();String name=Type.getInternalName(c);hosts.put(name,read(resource(name)));
            pending.addAll(List.of(c.getDeclaredClasses()));
        }
        var keep=(java.util.function.Consumer<MethodNode>)(m->{});
        try(var zip=new ZipFile(dependency.toFile())) {
            java.util.function.Function<String,byte[]> nativeBytes=name->{try{return zip.getInputStream(zip.getEntry(ROOT+name+".class")).readAllBytes();}catch(Exception e){throw new RuntimeException(e);}};
            transplant(hosts,nativeBytes.apply("battles/MoveActionResponse"),F+"$Move",n->n.equals("toShowdownString"),keep);
            transplant(hosts,nativeBytes.apply("api/battles/model/PokemonBattle"),F+"$Battle",n->n.equals("turn"),keep);
            transplant(hosts,nativeBytes.apply("api/battles/model/actor/BattleActor"),F+"$Actor",
                    n->Set.of("turn","setActionResponses","writeShowdownResponse","writeShowdownResponse$lambda$0").contains(n),keep);
            transplant(hosts,nativeBytes.apply("api/battles/model/actor/AIBattleActor"),F+"$Actor",n->n.startsWith("onChoiceRequested"),keep);
            transplant(hosts,nativeBytes.apply("battles/interpreter/instructions/TurnInstruction"),F+"$TurnInstruction",n->n.equals("invoke$lambda$0"),m->{
                hook(m,"maestroTurn","(L"+F+"$Battle;ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",true);
                hook(m,"maestroTurnFinished","(L"+F+"$Battle;ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",false);
            });
            transplant(hosts,nativeBytes.apply("battles/interpreter/instructions/RequestInstruction"),F+"$RequestInstruction",n->n.startsWith("invoke$lambda$0"),m->{
                if(m.name.equals("invoke$lambda$0"))hook(m,"maestroCorrected","(L"+F+"$RequestInstruction;L"+F+"$Request;L"+F+"$Battle;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",false);
            });
            transplant(hosts,nativeBytes.apply("battles/interpreter/instructions/ErrorInstruction"),F+"$ErrorInstruction",n->n.equals("invoke$lambda$0"),m->
                    hook(m,"maestroRejected","(L"+F+"$ErrorInstruction;L"+F+"$Battle;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",false));
        }
        transplant(hosts,resource(AI),F+"$Controller",n->Set.of("recordOwnChoice","ownRequestCorrected","ownChoiceRejected","ownTurnDispatched","ownTurnFinished").contains(n),keep);
        for(String mixin:List.of("OwnRequestDispatchMixin","OwnChoiceErrorMixin","OwnTurnDispatchMixin"))
            transplant(hosts,resource("com/maestroai/mixin/"+mixin),F+"$Hooks",n->Set.of("maestroCorrected","maestroRejected","maestroTurn","maestroTurnFinished").contains(n),keep);
        Map<String,byte[]> definitions=new HashMap<>();
        for(var entry:hosts.entrySet()) {
            var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);entry.getValue().accept(writer);
            definitions.put(entry.getKey().replace('/','.'),writer.toByteArray());
        }
        var loader=new ClassLoader(DispatchIntegrationRegression.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(!definitions.containsKey(name))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)) {
                    Class<?> c=findLoadedClass(name);if(c==null){byte[] b=definitions.get(name);c=defineClass(name,b,0,b.length);}
                    if(resolve)resolveClass(c);return c;
                }
            }
        };
        try { ((Runnable)loader.loadClass(F.replace('/','.')).getConstructor().newInstance()).run(); }
        catch(InvocationTargetException e){throw new AssertionError("Native dispatch integration",e.getCause());}
    }

    public static final class Fixture implements Runnable {
        static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
        public void run() {
            for(int mode=0;mode<9;mode++)scenario(mode);
            var choices=new Moves(false);choices.moves=List.of(new MoveSlot("closecombat"),new MoveSlot("spectralthief"));
            check(new Move("spectralthief","zmove").toShowdownString(new Active(null),choices).equals("move 2 zmove"),"native Z serialization lost base slot/gimmick");
            check(new Move("spectralthief",null).toShowdownString(new Active(null),choices).equals("move 2"),"ordinary native serialization changed");
            System.out.println("Native Marshadow Z response PASS: original pinned MoveActionResponse bytecode, base slot and zmove conversion wire; isolated hosts");
            System.out.println("Native queued dispatch integration PASS: 9 scenarios; native turn, request/error closures, production callbacks, validation and response serialization; controlled hosts, not Minecraft");
        }
        private void scenario(int mode) {
            Battle battle=new Battle();Actor actor=new Actor(battle);battle.actor=actor;
            actor.ai.ownTransport.owner(battle,actor,"p2");
            for(int n=1;n<12;n++){actor.ai.ownTransport.turnDispatched(battle,n);actor.ai.ownTransport.turnFinished(battle,n);}
            actor.request=new Request(false);actor.mustChoose=true;
            Controller.supported=mode!=8;
            TurnInstruction.invoke$lambda$0(battle,12);
            check(actor.ai.decisionTurn==11,"native choice must precede Java turn write");
            check(battle.turn==12,"native turn must finish at 12");
            battle.flush();
            check(battle.wire.equals(List.of(">p2 switch 2")),"native switch serialization");
            check(actor.request==null&&actor.responses.isEmpty(),"native submission clears request/responses");
            if(mode==3)actor.activePokemon.getFirst().mon.id=new UUID(0,2);
            if(mode==4){actor.ai.ownTurnDispatched(battle,actor,13);battle.turn=13;}
            if(mode==5)battle.ended=true;
            Request corrected=new Request(mode!=6);corrected.wait=mode==7;
            if(mode==1){
                RequestInstruction.invoke$lambda$0(new RequestInstruction(actor),corrected,battle);
                check(actor.ai.choices==1,"request before readiness must not choose");
                ErrorInstruction.invoke$lambda$0(new ErrorInstruction(actor),battle);
            }else{
                ErrorInstruction.invoke$lambda$0(new ErrorInstruction(actor),battle);
                RequestInstruction.invoke$lambda$0(new RequestInstruction(actor),corrected,battle);
            }
            if(mode==2){actor.ai.ownRequestCorrected(battle,actor,corrected);actor.ai.ownChoiceRejected(battle,actor);}
            battle.flush();
            if(mode<=2){
                check(battle.wire.equals(List.of(">p2 switch 2",">p2 move 1")),"corrected owning request must submit exactly one legal move");
                check(actor.ai.choices==2,"one retry only");
                actor.ai.ownRequestCorrected(battle,actor,corrected);
                check(actor.ai.choices==2,"submitted correction cannot be repeated");
            }else{
                check(battle.wire.equals(List.of(">p2 switch 2")),"stale/ended/untrapped/wait/unsupported context submitted a move");
                check(actor.ai.choices==1,"invalid context retried");
            }
            Controller.supported=true;
        }
        public static final class Battle {
            int turn=11;Actor actor;boolean ended;boolean pending;final List<String> wire=new ArrayList<>();
            public Iterable<Actor> getActors(){return List.of(actor);} public Iterable<Side> getSides(){return List.of();}
            public Actor getActor(String id){return id.equals("p2")?actor:null;} public int getTurn(){return turn;}
            public boolean getEnded(){return ended;}public void turn(int n){throw new AssertionError("unwoven native turn");}
            public void sendToActors(Packet p){} public void broadcastChatMessage(Text t){}
            public void doWhenClear(Function0<Unit> f){f.invoke();}
            public void checkForInputDispatch(){if(!actor.mustChoose&&!actor.responses.isEmpty())pending=true;}
            void flush(){if(pending){pending=false;actor.writeShowdownResponse();}}
            public void writeShowdownAction(String[] commands){wire.addAll(List.of(commands));}
            public Text createUnimplemented$common(Message m){return new Text();}
        }
        public static final class Actor {
            final Battle battle;public final AI battleAI;final Controller ai;
            Request request;boolean mustChoose;final List<Response> responses=new ArrayList<>(),expectingPassActions=new ArrayList<>();
            final List<Active> activePokemon=new ArrayList<>();
            Actor(Battle b){battle=b;ai=new Controller();battleAI=ai;activePokemon.add(new Active(this));}
            public AI getBattleAI(){return battleAI;}public Battle getBattle(){return battle;}public String getShowdownId(){return "p2";}
            public Request getRequest(){return request;}public void setRequest(Request r){request=r;}public List<Response> getResponses(){return responses;}
            public boolean getMustChoose(){return mustChoose;}public void setMustChoose(boolean b){mustChoose=b;}
            public List<Active> getActivePokemon(){return activePokemon;}public List<Mon> getPokemonList(){return List.of(activePokemon.getFirst().mon);}
            public Side getSide(){return new Side();}public void sendMessage(Text t){}
            public void sendUpdate(Packet p){if(p instanceof MakeChoice)onChoiceRequested();}
            public void turn(){throw new AssertionError("unwoven actor turn");}
            public void onChoiceRequested(){throw new AssertionError("unwoven native AI callback");}
            public void setActionResponses(List<Response> r){throw new AssertionError("unwoven native response validation");}
            public void writeShowdownResponse(){throw new AssertionError("unwoven native response submission");}
        }
        public interface AI { Response choose(Active a,Battle b,Side s,Moves m,boolean forced); }
        public static final class Controller implements AI {
            final OwnChoiceRecovery choiceRecovery=new OwnChoiceRecovery();
            final OwnRequestTransport ownTransport=new OwnRequestTransport();int decisionTurn=-1,choices;
            static boolean supported=true;private static boolean ownTransportVersionSupported(){return supported;}
            public Response choose(Active a,Battle b,Side s,Moves m,boolean forced){
                choices++;decisionTurn=b.getTurn();Response response=m.trapped?new Response():new Switch();
                recordOwnChoice(b,a,m,forced,response);return response;
            }
            private void recordOwnChoice(Battle b,Active a,Moves m,boolean forced,Response response){throw new AssertionError("unwoven production record");}
            public void ownRequestCorrected(Battle b,Actor a,Request r){throw new AssertionError("unwoven production recovery");}
            public void ownChoiceRejected(Battle b,Actor a){throw new AssertionError("unwoven production error hook");}
            public void ownTurnDispatched(Battle b,Actor a,int n){throw new AssertionError("unwoven production turn hook");}
            public void ownTurnFinished(Battle b,Actor a,int n){throw new AssertionError("unwoven production turn completion");}
            public void ownTransportUnavailable(){throw new AssertionError("own turn dispatch failed");}
        }
        public static final class Active { final Actor actor;final Mon mon=new Mon();Active(Actor a){actor=a;}
            public Actor getActor(){return actor;}public Mon getBattlePokemon(){return mon;}public boolean isAlive(){return true;} }
        public static final class Mon {UUID id=new UUID(0,1);public UUID getUuid(){return id;}public void setWillBeSwitchedIn(boolean v){}public Text getName(){return new Text();}}
        public static final class Side {}
        public static final class MoveSlot {private final String id;MoveSlot(String id){this.id=id;}public String getId(){return id;}}
        public static final class Moves {boolean trapped;List<MoveSlot> moves=List.of();Moves(boolean b){trapped=b;}public boolean getTrapped(){return trapped;}public List<MoveSlot> getMoves(){return moves;}}
        public static final class Request {
            final List<Moves> active;boolean wait,forced;Request(boolean trapped){active=List.of(new Moves(trapped));}
            public List<Moves> getActive(){return active;}public boolean getWait(){return wait;}public List<Boolean> getForceSwitch(){return List.of(forced);}
            public <T> List<T> iterate(List<Active> a,Function3<Active,Moves,Boolean,T> f){return List.of(f.invoke(a.getFirst(),active.getFirst(),forced));}
        }
        public static class Response {public boolean isValid(Active a,Moves m,boolean forced){return true;}public String toShowdownString(Active a,Moves m){return "move 1";}}
        public static final class Move extends Response {
            private String moveName,targetPnx,gimmickID;
            Move(String id,String gimmick){moveName=id;gimmickID=gimmick;}
        }
        public static final class Switch extends Response {public String toShowdownString(Active a,Moves m){return "switch 2";}}
        public static final class Pass extends Response {public static final Pass INSTANCE=new Pass();}
        public static final class ForcePass extends Response {}
        public interface Packet {}public static final class MakeChoice implements Packet {}
        public static final class InvalidChoice implements Packet {}public static final class QueueRequest implements Packet {public QueueRequest(Request r){}}
        public static final class Message {public String getRawMessage(){return "|error|[Unavailable choice] Can't switch: The active Pokémon is trapped";}}
        public static final class RequestInstruction {
            final Actor battleActor;RequestInstruction(Actor a){battleActor=a;}public Actor getBattleActor(){return battleActor;}
            public static Unit invoke$lambda$0(RequestInstruction i,Request r,Battle b){throw new AssertionError("unwoven request closure");}
        }
        public static final class ErrorInstruction {
            final Actor battleActor;final Message message=new Message();ErrorInstruction(Actor a){battleActor=a;}public Actor getBattleActor(){return battleActor;}
            public static Unit invoke$lambda$0(ErrorInstruction i,Battle b){throw new AssertionError("unwoven error closure");}
        }
        public static final class TurnInstruction {public static Result invoke$lambda$0(Battle b,int n){throw new AssertionError("unwoven turn closure");}}
        public static final class Hooks {}
        public static final class Result {}public static final class Results {public static Result getGO(){return new Result();}}
        public static final class Text {public String getString(){return "Synthetic";}}
        public static final class Texts {public static Text battleLang(String s,Object[] a){return new Text();}public static Text red(Text t){return t;}public static Text aqua(Text t){return t;}}
        public static final class IllegalChoice extends RuntimeException {public IllegalChoice(Actor a,String s){super(s);}}
        public static final class LogHost {public static final org.apache.logging.log4j.Logger LOGGER=org.apache.logging.log4j.LogManager.getLogger("synthetic");}
        public static final class Unused {}
    }
}
