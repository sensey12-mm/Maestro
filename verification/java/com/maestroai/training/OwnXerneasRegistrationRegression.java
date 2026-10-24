package com.maestroai.training;

import com.google.gson.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Original native registration constructor/getters and published addon callback.
 * World/config hosts carry only the whole-JAR-pinned public species JSON. No
 * replacement constructor, invented Active stats, installed-Mixin claim or
 * upstream bytecode is shipped in the production mod.
 */
public final class OwnXerneasRegistrationRegression {
    private static final String ROOT="com/cobblemon/mod/common/", F=Type.getInternalName(OwnXerneasRegistrationRegression.class);
    private static final String DEFINITION=F+"$NativeDefinition";
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static String sha(byte[] data)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
    private static byte[] bytes(ZipFile jar,String name)throws Exception{try(var in=jar.getInputStream(jar.getEntry(name+".class"))){return in.readAllBytes();}}
    private static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    private static byte[] resource(Class<?> c)throws Exception{try(var in=c.getResourceAsStream("/"+Type.getInternalName(c)+".class")){return in.readAllBytes();}}
    private static void copyMethods(ClassNode host,byte[] source,Remapper remapper,Set<String> names){
        var n=new ClassNode();new ClassReader(source).accept(new ClassRemapper(n,remapper),0);
        for(var m:n.methods)if(names.contains(m.name)){
            m.access=(m.access&~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED|Opcodes.ACC_FINAL))|Opcodes.ACC_PUBLIC;
            m.visibleAnnotations=null;m.invisibleAnnotations=null;
            host.methods.removeIf(old->old.name.equals(m.name)&&old.desc.equals(m.desc));host.methods.add(m);
        }
    }
    public static JsonObject definitions(Path dependency)throws Exception{
        check(sha(Files.readAllBytes(dependency)).equals("f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3"),"Native registration dependency differs");
        Path addon=Path.of(System.getenv().getOrDefault("GRADLE_USER_HOME",System.getProperty("user.home")+"/.gradle"),"caches/maestro-verification/0cf4568128805816a8d6f3cbab08374b2e0261784c3d6e8c091c602f79bebf51.jar");
        check(sha(Files.readAllBytes(addon)).equals("0cf4568128805816a8d6f3cbab08374b2e0261784c3d6e8c091c602f79bebf51"),"Published registration addon differs");
        var output=new JsonObject();
        try(var jar=new ZipFile(dependency.toFile());var pack=new ZipFile(addon.toFile())){
            try(var in=pack.getInputStream(pack.getEntry("data/mega_showdown/mega_showdown/battle_form/xerneas_active.json"))){
                var profile=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                check(profile.getAsJsonArray("pokemons").toString().equals("[\"Xerneas\"]")
                    &&profile.get("showdown_form_change_id").getAsString().equals("active")
                    &&profile.getAsJsonObject("aspect_conditions").getAsJsonObject("apply").getAsJsonArray("aspects").toString().equals("[\"life_mode=active\"]")
                    &&profile.getAsJsonObject("aspect_conditions").getAsJsonObject("revert").getAsJsonArray("aspects").toString().equals("[\"life_mode=neutral\"]"),
                    "Published Active/Neutral aspect profile changed");
            }
            JsonObject data;try(var in=jar.getInputStream(jar.getEntry("data/cobblemon/species/generation6/xerneas.json"))){data=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
            check(data.getAsJsonArray("forms").size()==1&&data.getAsJsonArray("forms").get(0).getAsJsonObject().get("name").getAsString().equals("Active"),"Original Xerneas form inventory changed");
            for(boolean patched:List.of(false,true)){
                var types=new HashMap<String,String>();
                types.put(ROOT+"api/pokemon/PokemonSpecies$ShowdownSpecies",DEFINITION);
                for(var pair:Map.ofEntries(
                    Map.entry("api/pokemon/PokemonSpecies",Api.class),Map.entry("pokemon/Species",Species.class),Map.entry("pokemon/FormData",Form.class),
                    Map.entry("api/types/ElementalType",Element.class),Map.entry("api/pokemon/stats/Stats",Stat.class),Map.entry("api/pokemon/stats/Stat",Stat.class),
                    Map.entry("api/pokemon/evolution/PreEvolution",Evolution.class),Map.entry("api/pokemon/egg/EggGroup",Egg.class),
                    Map.entry("api/abilities/AbilityPool",Pool.class),Map.entry("api/abilities/PotentialAbility",Potential.class),Map.entry("api/abilities/AbilityTemplate",Template.class),
                    Map.entry("api/moves/MoveTemplate",Move.class)).entrySet())types.put(ROOT+pair.getKey(),Type.getInternalName(pair.getValue()));
                types.put("net/minecraft/class_2960",Type.getInternalName(Id.class));
                types.put("org/spongepowered/asm/mixin/injection/callback/CallbackInfo",Type.getInternalName(Unused.class));
                types.put("com/github/yajatkaul/mega_showdown/mixin/battle/ShowdownSpeciesMixin",DEFINITION);
                var remapper=new Remapper(){@Override public String map(String n){return types.getOrDefault(n,n);}};
                var hosts=new HashMap<String,ClassNode>();for(Class<?> c:List.of(Species.class,Form.class,Api.class))hosts.put(Type.getInternalName(c),node(resource(c)));
                copyMethods(hosts.get(Type.getInternalName(Api.class)),bytes(jar,ROOT+"api/pokemon/PokemonSpecies"),remapper,Set.of("createShowdownName","access$createShowdownName"));
                copyMethods(hosts.get(Type.getInternalName(Form.class)),bytes(jar,ROOT+"pokemon/FormData"),remapper,
                    Set.of("getName","getSpecies","getBaseStats","getPrimaryType","getSecondaryType","getTypes","getAbilities","getMaleRatio","getHeight","getWeight","getEggGroups","getEvolutions","getPreEvolution","getDynamaxBlocked","getRequiredMove","getRequiredItem","getRequiredItems","getGigantamaxMove"));
                var definition=new ClassNode();new ClassReader(bytes(jar,ROOT+"api/pokemon/PokemonSpecies$ShowdownSpecies")).accept(new ClassRemapper(definition,remapper),0);
                definition.nestHostClass=null;definition.innerClasses.clear();definition.outerClass=null;
                if(patched){
                    byte[] patch=bytes(pack,"com/github/yajatkaul/mega_showdown/mixin/battle/ShowdownSpeciesMixin");
                    var mutable=node(patch).fields.stream().filter(f->f.name.equals("abilities")).findFirst().orElseThrow();
                    check(java.util.stream.Stream.concat(mutable.visibleAnnotations==null?java.util.stream.Stream.empty():mutable.visibleAnnotations.stream(),mutable.invisibleAnnotations==null?java.util.stream.Stream.empty():mutable.invisibleAnnotations.stream()).anyMatch(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mutable;")),"Published abilities patch lost its Mutable declaration");
                    definition.fields.stream().filter(f->f.name.equals("abilities")).forEach(f->f.access&=~Opcodes.ACC_FINAL);
                    copyMethods(definition,patch,remapper,Set.of("populateAbilities"));
                    for(var method:definition.methods)if(method.name.equals("<init>"))for(var insn:method.instructions.toArray())if(insn.getOpcode()==Opcodes.RETURN){
                        var call=new InsnList();for(int i=0;i<3;i++)call.add(new VarInsnNode(Opcodes.ALOAD,i));call.add(new InsnNode(Opcodes.ACONST_NULL));
                        call.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,DEFINITION,"populateAbilities","(L"+Type.getInternalName(Species.class)+";L"+Type.getInternalName(Form.class)+";L"+Type.getInternalName(Unused.class)+";)V",false));method.instructions.insertBefore(insn,call);
                    }
                }
                hosts.put(DEFINITION,definition);
                var encoded=new HashMap<String,byte[]>();
                for(var e:hosts.entrySet()){
                    e.getValue().nestHostClass=null;
                    e.getValue().innerClasses.clear();e.getValue().outerClass=null;
                    var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);e.getValue().accept(writer);encoded.put(e.getKey().replace('/','.'),writer.toByteArray());
                }
                var loader=new ClassLoader(OwnXerneasRegistrationRegression.class.getClassLoader()){
                    @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                        synchronized(getClassLoadingLock(name)){var found=findLoadedClass(name);if(found==null&&encoded.containsKey(name))found=defineClass(name,encoded.get(name),0,encoded.get(name).length);
                            if(found==null)found=super.loadClass(name,false);if(resolve)resolveClass(found);return found;}
                    }
                };
                var speciesClass=loader.loadClass(Species.class.getName());var formClass=loader.loadClass(Form.class.getName());
                Object species=speciesClass.getConstructor(JsonObject.class).newInstance(data);
                Object form=((List<?>)speciesClass.getMethod("getForms").invoke(species)).getFirst();
                var constructor=loader.loadClass(DEFINITION.replace('/','.')).getConstructor(speciesClass,formClass);
                var definitions=new JsonObject();definitions.add("xerneas",new Gson().toJsonTree(constructor.newInstance(species,null)));
                definitions.add("xerneasactive",new Gson().toJsonTree(constructor.newInstance(species,form)));
                check(definitions.getAsJsonObject("xerneasactive").getAsJsonObject("baseStats").equals(definitions.getAsJsonObject("xerneas").getAsJsonObject("baseStats")),"Native inherited Active stats changed");
                check(definitions.getAsJsonObject("xerneasactive").getAsJsonArray("types").equals(definitions.getAsJsonObject("xerneas").getAsJsonArray("types")),"Native inherited Active types changed");
                check(definitions.getAsJsonObject("xerneasactive").get("name").getAsString().equals("Xerneas-Active"),"Native form registration name changed");
                output.add(patched?"pack":"native",definitions);
            }
        }
        System.out.println("Original Xerneas registration PASS: pinned ShowdownSpecies constructor, FormData inheritance getters and published abilities callback; explicit data/world hosts, no installed-Mixin claim");
        return output;
    }
    public static void main(String[] args)throws Exception{Files.writeString(Path.of(args[1]),definitions(Path.of(args[0])).toString());}

    // Hosts contain no battle/actor/request. Their fields come only from the original public JSON.
    public enum Stat {HP,ATTACK,DEFENCE,SPECIAL_ATTACK,SPECIAL_DEFENCE,SPEED}
    public record Element(String name){public String getName(){return name;}}
    public record Id(String namespace){public String method_12836(){return namespace;}}
    public record Egg(String id){public String getShowdownID(){return id;}}
    public interface Evolution {Species getSpecies();Form getForm();}
    public record Template(String name){public String getName(){return name;}}
    public interface Potential {Template getTemplate();}
    public record PotentialValue(Template template) implements Potential {public Template getTemplate(){return template;}}
    public static class Pool implements Iterable<Potential>{public final List<Potential> values=List.of(new PotentialValue(new Template("fairyaura")));public Iterator<Potential> iterator(){return values.iterator();}}
    public static class Move {public String getName(){throw new AssertionError("Unexpected Gigantamax dependency");}}
    public static class Unused {}
    public static HashMap<Stat,Integer> stats(JsonObject j){
        var out=new HashMap<Stat,Integer>();String[] keys={"hp","attack","defence","special_attack","special_defence","speed"};
        for(int i=0;i<keys.length;i++)out.put(Stat.values()[i],j.get(keys[i]).getAsInt());return out;
    }
    public static Element type(JsonObject j,String key){return j.has(key)?new Element(j.get(key).getAsString().substring(0,1).toUpperCase(Locale.ROOT)+j.get(key).getAsString().substring(1)):null;}
    public static class Species {
        public final JsonObject data;public final List<Form> forms;
        public Species(JsonObject data){this.data=data;forms=List.of(new Form(this,data.getAsJsonArray("forms").get(0).getAsJsonObject()));}
        public int getNationalPokedexNumber(){return data.get("nationalPokedexNumber").getAsInt();}public String getName(){return data.get("name").getAsString();}
        public Id getResourceIdentifier(){return new Id("cobblemon");}public List<Form> getForms(){return forms;}public Form getStandardForm(){throw new AssertionError("Unexpected evolution");}
        public Iterable<Element> getTypes(){return List.of(getPrimaryType());}public Element getPrimaryType(){return type(data,"primaryType");}public Element getSecondaryType(){return null;}
        public HashMap<Stat,Integer> getBaseStats(){return stats(data.getAsJsonObject("baseStats"));}public float getMaleRatio(){return data.get("maleRatio").getAsFloat();}
        public float getHeight(){return data.get("height").getAsFloat();}public float getWeight(){return data.get("weight").getAsFloat();}public String showdownId(){return "xerneas";}
        public HashSet<Egg> getEggGroups(){return new HashSet<>(List.of(new Egg("undiscovered")));}public Pool getAbilities(){return new Pool();}
        public Evolution getPreEvolution(){return null;}public Set<Object> getEvolutions(){return Set.of();}public boolean getDynamaxBlocked(){return false;}
    }
    public static class Form {
        public Species species;public String name;public Map<Stat,Integer> _baseStats;public Float _maleRatio,_height,_weight;
        public Element _primaryType,_secondaryType;public Pool _abilities;public Set<Egg> _eggGroups;public Set<Object> _evolutions;public Evolution _preEvolution;public Boolean _dynamaxBlocked;
        public String requiredMove,requiredItem;public List<String> requiredItems;public Move gigantamaxMove;
        public Form(Species species,JsonObject data){this.species=species;name=data.get("name").getAsString();_primaryType=type(data,"primaryType");if(data.has("maleRatio"))_maleRatio=data.get("maleRatio").getAsFloat();if(data.has("height"))_height=data.get("height").getAsFloat();if(data.has("weight"))_weight=data.get("weight").getAsFloat();}
        public String getName(){throw new AssertionError("Native getter not transplanted");}public Species getSpecies(){throw new AssertionError("Native getter not transplanted");}
    }
    public static class Api {public static final Api INSTANCE=new Api();}
}
