package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;
import static com.maestroai.training.PublicStateSearchAdapter.Interval;
import static com.maestroai.training.ExecutionCoverage.*;

/** Source-supported field/modifier slice, explicitly conditional on independently
 * justified remaining effects. Runtime passes UNKNOWN for pack/engine overrides;
 * the public protocol alone never proves that those overrides do not exist.
 */
public final class ScopedExecution {
    public record Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability,
                          Known<Boolean> ownTransportCurrent, OwnRequestTransport.Lease lease) {
        public Binding { Objects.requireNonNull(item); Objects.requireNonNull(ability); Objects.requireNonNull(ownTransportCurrent); }
        public Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability, Known<Boolean> current) {
            this(observation, item, ability, current, null);
        }
        public Known<Boolean> ownTransportCurrent() {
            return lease != null && (!lease.current()) ? Known.unknown() : ownTransportCurrent;
        }
        public Binding(PublicMechanicsEvidence.Observation observation, Known<String> item, Known<String> ability) {
            this(observation, item, ability, Known.of(true, "explicit conditional own-transport fixture"));
        }
    }
    private final PublicFieldEvidence.Capture field;
    private final Map<String, Binding> actors;
    private final Scopes scopes;
    private final OwnZMoveEvidence ownZ;
    private final String activeOwn;
    private final boolean ownFainted;
    /** Conditional entry result, never an observed public field or future request. */
    static final class WeatherProjection {
        private final PublicFieldEvidence.Capture origin;
        private final Known<String> weather;
        private final Known<Integer> duration;
        private WeatherProjection(PublicFieldEvidence.Capture origin,String weather,int duration){
            this.origin=origin;String source=origin.token()+":source-certified-conditional-weather-entry";
            this.weather=Known.of(weather,source);this.duration=Known.of(duration,source);
        }
    }
    private final WeatherProjection projectedWeather;
    public ScopedExecution(PublicFieldEvidence.Capture field, Map<String, Binding> actors, Known<Boolean> remainingEffects) {
        this(field, actors, Scopes.all(Objects.requireNonNull(remainingEffects)));
    }
    public ScopedExecution(PublicFieldEvidence.Capture field, Map<String, Binding> actors, Scopes scopes) {
        this(field,actors,scopes,"self",false,null,null);
    }
    private ScopedExecution(PublicFieldEvidence.Capture field, Map<String, Binding> actors, Scopes scopes,
                            String activeOwn,boolean ownFainted,WeatherProjection projectedWeather,OwnZMoveEvidence ownZ) {
        this.field = Objects.requireNonNull(field); this.actors = Map.copyOf(actors); this.scopes = Objects.requireNonNull(scopes);
        this.ownZ=ownZ;this.activeOwn=activeOwn;this.ownFainted=ownFainted;this.projectedWeather=projectedWeather;
        if(projectedWeather!=null&&projectedWeather.origin!=field)throw new IllegalArgumentException("foreign weather projection");
        var a = actors.get("self"); var f = actors.get("opponent");
        if (a != null && f != null && a.observation() != null && f.observation() != null
                && a.observation().slot().substring(0, 2).equals(f.observation().slot().substring(0, 2)))
            throw new IllegalArgumentException("own/opponent execution bindings share a side");
    }
    public PublicFieldEvidence.Capture field() { return field; }
    public ScopedExecution withOwnZMove(OwnZMoveEvidence proof) {
        return new ScopedExecution(field,actors,scopes,activeOwn,ownFainted,projectedWeather,Objects.requireNonNull(proof));
    }
    private boolean zAllowed(String id,String role){
        return !OwnZMoveEvidence.MOVE.equals(id)||ownZ!=null&&actors.containsKey(role)&&ownZ.permits(role,field,actors.get(role).observation());
    }
    ScopedExecution withScopes(Scopes scopes) { return new ScopedExecution(field, actors, scopes,activeOwn,ownFainted,projectedWeather,ownZ); }
    /** Internal action-queue frame. The evaluator supplies a reserve only after
     * certified legal entry and survival, or fainting after proven continuation.
     * Neither an entry arithmetic certificate nor a raw reserve activates it. */
    ScopedExecution conditionalParticipants(String own,boolean fainted) {
        if(!own.equals("self")&&!reserve(own))throw new IllegalArgumentException("unbound own participant");
        return new ScopedExecution(field,actors,scopes,own,fainted,projectedWeather,ownZ);
    }
    ScopedExecution conditionalWeather(WeatherProjection weather){return new ScopedExecution(field,actors,scopes,activeOwn,ownFainted,weather,ownZ);}
    Known<String> rawWeather(){return projectedWeather==null?field.weather():projectionCurrent()?projectedWeather.weather:Known.unknown();}
    private boolean projectionCurrent(){return sourceCurrent()&&yes(scopes.get(Operation.ENTRY))&&yes(scopes.get(Operation.DAMAGE));}
    Known<Integer> projectedWeatherDuration(){return projectedWeather!=null&&projectionCurrent()?projectedWeather.duration:Known.unknown();}
    Known<Boolean> weatherAtResidual(){
        if(!sourceCurrent())return Known.unknown();
        return projectedWeather==null?field.weatherAtResidual():projectionCurrent()?Known.of(true,projectedWeather.weather.source()):Known.unknown();
    }
    ScopedExecution calculationFrame() { return withScopes(scopes.calculationFrame()); }
    boolean sourceCurrent() {
        try { field.requireCurrent(field.turn()); }
        catch(IllegalArgumentException stale) { return false; }
        return (ownZ==null||ownZ.current()) && scopes.current() && actors.values().stream().filter(b->b.lease()!=null)
                .allMatch(b->b.lease().current());
    }
    /**
     * Exhaustiveness of the current *item/ability modifier choice* only:
     * both opponent values must be explicit, current, spectator-public events.
     * This does not certify the foe's move distribution, other unseen party
     * members, form effects, arbitrary addon callbacks or future turns.
     *
     * Actual action arithmetic and fields remain independently gated by every
     * executed operation's certificate and by the completed reply comparison.
     * In particular, merely recognizing an item/ability in the engine registry
     * is never evidence of the opponent's currently held item or ability.
     */
    public Known<Boolean> publicModifierSingletonScope() {
        if (!sourceCurrent() || !field.unknown().isEmpty()) return Known.unknown();
        Binding foe = actors.get("opponent");
        if (foe == null || foe.observation() == null) return Known.unknown();
        var seen = foe.observation();
        if (seen.ambiguous() || seen.unsupportedEffects()
                || !foe.item().known() || !foe.ability().known()
                || !foe.item().equals(seen.item()) || !foe.ability().equals(seen.ability()))
            return Known.unknown();
        var publicEntry = field.actor(seen);
        if (publicEntry == null || !publicEntry.covered()) return Known.unknown();
        return Known.of(true, field.token() + ":current-spectator-public-item-and-ability-only");
    }
    public void requireCurrent(int turn) { field.requireCurrent(turn); }
    /** These values must already pass current per-operation evidence gates. */
    String currentItem(String role){return item(role);}
    String currentAbility(String role){return ability(role);}
    private static boolean weatherSuppressor(String ability){return Set.of("airlock","cloudnine").contains(ability);}
    private static String setterWeather(String ability){return switch(ability){
        case "drought"->"sunnyday";case "drizzle"->"raindance";case "sandstream"->"sandstorm";case "snowwarning"->"snow";default->null;
    };}
    /** Native Start follows hazard survival. Same-weather abilities never refresh
     * Gen9 duration, even under suppression. Primal queue/form evidence is absent. */
    Known<Optional<WeatherProjection>> entryWeather(Profile reserve,Actor arriving,Actor foe){
        bound(reserve,arriving);var c=new Check(Operation.ENTRY);c.actor(arriving.id(),true);
        if(!actors.containsKey(arriving.id()))return Known.unknown();
        String next=setterWeather(ability(arriving.id()));
        if(next==null)return c.finish().supported()?Known.of(Optional.ofNullable(projectedWeather),field.token()+":no-weather-entry"):Known.unknown();
        c.need("WEATHER_ENTRY_DAMAGE_SOURCE",yes(scopes.get(Operation.DAMAGE)));
        c.need("WEATHER_ENTRY_PROVEN_PARTICIPANT",activeOwn.equals(arriving.id())&&!ownFainted);
        c.actor(foe.id(),false);
        c.need("WEATHER_ENTRY_OPPONENT",foe.id().equals("opponent"));
        c.need("WEATHER_ENTRY_SURVIVAL",arriving.hp().known()&&arriving.hp().value().low()>0&&!arriving.facts().fainted());
        c.need("WEATHER_ENTRY_FOE_PARTICIPATION",foe.hp().known()&&foe.hp().value().low()>0&&!foe.facts().fainted());
        c.need("WEATHER_ENTRY_EXISTING_WEATHER",rawWeather().known());
        String species=arriving.facts().species()==null?"":arriving.facts().species().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        c.need("WEATHER_ENTRY_FORM_AND_PRIMAL_QUEUE",!species.isEmpty()&&!species.startsWith("groudon")&&!species.startsWith("kyogre"));
        if(!c.finish().supported())return Known.unknown();
        if(rawWeather().value().equals(next))return Known.of(Optional.ofNullable(projectedWeather),field.token()+":same-weather-no-refresh");
        String rock=switch(next){case "sunnyday"->"heatrock";case "raindance"->"damprock";case "sandstorm"->"smoothrock";default->"icyrock";};
        var result=new WeatherProjection(field,next,item(arriving.id()).equals(rock)?8:5);
        return c.finish().supported()?Known.of(Optional.of(result),result.weather.source()):Known.unknown();
    }
    private static String speedWeather(String ability){return switch(ability){
        case "sandrush" -> "sandstorm";
        case "chlorophyll" -> "sunnyday";
        case "swiftswim" -> "raindance";
        case "slushrush" -> "snow";
        default -> null;
    };}
    /** Raw weather and its clock remain public field facts. Suppression only
     * changes effective weather, and never proves a native faint flag or entry.
     * Current single-actor queries use the other fresh public observation;
     * conditional multi-actor calculations supply both projected actors. */
    private Known<String> weather(Check c,Actor... participants){
        c.need("EFFECTIVE_WEATHER",rawWeather().known());
        if(!rawWeather().known())return Known.unknown();
        var projected=new HashMap<String,Actor>();
        for(var actor:participants){
            c.need("WEATHER_REPLACEMENT_PARTICIPATION",Set.of(activeOwn,"opponent").contains(actor.id()));
            c.need("WEATHER_UNIQUE_PARTICIPANTS",projected.put(actor.id(),actor)==null);
        }
        for(String role:List.of(activeOwn,"opponent")){
            c.actor(role,reserve(role));
            c.need(role+":WEATHER_ACTIVE_ENTRY",!reserve(role)||projected.containsKey(role));
        }
        if(rawWeather().value().equals("none"))return c.finish().supported()?rawWeather():Known.unknown();
        boolean suppressed=false;
        for(String role:List.of(activeOwn,"opponent")){
            var binding=actors.get(role);
            if(binding==null||!binding.ability().known())continue;
            Actor actor=projected.get(role);
            if(weatherSuppressor(binding.ability().value())){
                // Only a completed conditional faint boundary may remove a
                // suppressor at zero HP. Raw zero HP still proves no faint flag.
                if(role.equals(activeOwn)&&ownFainted){
                    c.need(role+":WEATHER_FAINT_BOUNDARY",actor!=null&&actor.hp().known()&&actor.hp().value().high()==0);
                    continue;
                }
                Known<Interval> hp=actor==null?(binding.observation()==null?Known.unknown():binding.observation().hp()):actor.hp();
                // HP zero alone does not establish when native faintMessages
                // removed suppression. Do not guess during a conditional queue.
                boolean active=hp.known()&&hp.value().low()>0&&(actor==null||!actor.facts().fainted());
                c.need(role+":WEATHER_SUPPRESSOR_PARTICIPATION",active);suppressed|=active;
            }
        }
        return c.finish().supported()?Known.of(suppressed?"none":rawWeather().value(),field.token()+":effective-weather"):Known.unknown();
    }
    Known<String> effectiveWeather(Actor... participants){return weather(new Check(Operation.DAMAGE),participants);}
    /** Native sun/rain checks may be actor-specific. Damage and Thunder use
     * the defender; Chlorophyll/Swift Swim use their holder. Callers first
     * certify current item/suppression evidence. Global weather stays intact. */
    private Known<String> actorWeather(Check c,String role,Actor... participants){
        var weather=weather(c,participants);
        return weather.known()&&Set.of("sunnyday","raindance").contains(weather.value())&&item(role).equals("utilityumbrella")
                ?Known.of("none",field.token()+":actor-effective-weather"):weather;
    }
    Known<String> effectiveActorWeather(String role,Actor... participants){var c=new Check(Operation.DAMAGE);c.actor(role,reserve(role));return actorWeather(c,role,participants);}
    private static Actor[] participants(Actor actor,Actor... others){var all=new Actor[others.length+1];all[0]=actor;System.arraycopy(others,0,all,1,others.length);return all;}
    /** Residuals reuse the damage source domain: its attestation includes the
     * native event/faint dispatch and complete condition/item/ability tables. */
    public Certificate residualCoverage(Profile profile, Actor actor) {
        bound(profile,actor); var c=new Check(Operation.DAMAGE);
        c.actor(actor.id(),reserve(actor.id()));
        c.need("RESIDUAL_MAX_HP",profile.maxHp().known()
                &&profile.maxHp().value().low()==profile.maxHp().value().high());
        c.need("RESIDUAL_STATUS",actor.status().known()
                &&Set.of("none","par","brn","psn").contains(actor.status().value()));
        c.need("RESIDUAL_HP",actor.hp().known());
        c.need("RESIDUAL_WEATHER",rawWeather().known()
                &&Set.of("none","sunnyday","raindance","sandstorm","hail","snow").contains(rawWeather().value()));
        if(rawWeather().known()&&Set.of("sandstorm","hail").contains(rawWeather().value()))
            c.need(rawWeather().value().equals("hail")?"HAIL_CURRENT_TYPES":"SANDSTORM_CURRENT_TYPES",profile.types().known());
        c.need("RESIDUAL_TERRAIN",field.terrain().known()
                &&Set.of("none","psychicterrain").contains(field.terrain().value()));
        return c.finish();
    }
    boolean sandstormImmune(Profile profile) {
        return Set.of("magicguard","sandrush","sandforce","sandveil").contains(ability(profile.actor()))
                ||item(profile.actor()).equals("safetygoggles")
                ||profile.types().value().stream().anyMatch(Set.of("rock","ground","steel")::contains);
    }
    boolean hailImmune(Profile profile) {
        return Set.of("magicguard","snowcloak","icebody").contains(ability(profile.actor()))
                ||item(profile.actor()).equals("safetygoggles")||profile.types().value().contains("ice");
    }
    /** Active Power Herb requires current own binding and no Magic Room. */
    boolean activePowerHerb(String role) {
        var b=actors.get(role);
        return sourceCurrent() && b!=null && b.item().known() && field.magicRoom().known()
                && !field.magicRoom().value() && item(role).equals("powerherb");
    }
    public Set<String> unresolved() {
        Set<String> reasons = new TreeSet<>(field.unknown());
        if (Arrays.stream(Operation.values()).anyMatch(op -> !yes(scopes.get(op)))) reasons.add("UNANNOUNCED_PACK_OR_EXECUTION_EFFECTS_UNRESOLVED");
        if (!rawWeather().known() || !field.terrain().known() || !field.trickRoom().known() || !field.magicRoom().known())
            reasons.add("PUBLIC_FIELD_SCOPE_UNKNOWN");
        return Set.copyOf(reasons);
    }
    private final class Check {
        final Operation op; final Set<String> required = new TreeSet<>(), missing = new TreeSet<>();
        final List<OwnRequestTransport.Lease> leases = new ArrayList<>();
        Check(Operation op) {
            this.op = op; field.requireCurrent(field.turn());
            need("OWN_Z_CHOICE_COVERAGE",ownZ==null||ownZ.current());
            need("PUBLIC_PROTOCOL_AND_EFFECT_COVERAGE", field.unknown().stream().allMatch(reason ->
                    reason.equals("UNSUPPORTED_WEATHER") && op != Operation.DAMAGE));
            // Unsupported weather is irrelevant to this limited priority/speed/entry/setup slice.
            field.unknown().stream().filter(reason -> !reason.equals("UNSUPPORTED_WEATHER") || op == Operation.DAMAGE).forEach(missing::add);
            need("SOURCE_DOMAIN_" + op, yes(scopes.get(op)));
            if(field.hasOwnedForm()||field.hasOwnedZUse())need("OWN_FORM_SOURCE_CATALOG_AND_DISPATCH",yes(scopes.get(Operation.DAMAGE)));
        }
        void need(String name, boolean supported) { required.add(name); if (!supported) missing.add(name); }
        void actor(String role, boolean reserve) {
            Binding b = actors.get(role);
            need(role + ":ACTOR_BINDING", b != null); if (b == null) return;
            boolean publicBinding = !role.equals("opponent") || b.observation() != null
                    && b.item().equals(b.observation().item()) && b.ability().equals(b.observation().ability());
            need(role + ":MODIFIER_PROVENANCE", publicBinding);
            if (!role.equals("opponent")) {
                need(role + ":OWN_TRANSPORT_FRESHNESS", yes(b.ownTransportCurrent()) && (b.lease() == null || b.lease().turn() == field.turn()));
                if (b.lease() != null) leases.add(b.lease());
            }
            need(role + ":CURRENT_ITEM", b.item().known() && Set.of("none", "heavydutyboots", "choicescarf", "laggingtail", "powerherb", "marshadiumz", "leftovers", "lifeorb", "mewtwonitey", "safetygoggles", "utilityumbrella", "heatrock", "damprock", "smoothrock", "icyrock").contains(b.item().value()));
            // Pressure affects PP consumption, not the outcome of one already
            // legal, successful action. Future PP/requests are not certified.
            // Insomnia has no modifier on an already legal awake action. Its
            // sleep/Yawn immunity does not prove that Update cured current sleep.
            need(role + ":CURRENT_ABILITY", b.ability().known() && Set.of("runaway", "magicguard", "pressure", "insomnia", "levitate", "prankster", "stall", "download", "technician", "multitype", "fairyaura", "asonespectrier", "shadowshield", "sandrush", "sandforce", "sandveil", "airlock", "cloudnine", "chlorophyll", "swiftswim", "raindish", "solarpower", "slushrush", "snowcloak", "icebody", "drought", "drizzle", "sandstream", "snowwarning").contains(b.ability().value()));
            // This actor check alone covers no entry ability activation.
            // Download and weather participation have separate entry gates.
            if (reserve) need(role + ":OWN_RESERVE_CHANNEL", !role.equals("opponent") && b.observation() == null);
            else {
                var a = field.actor(b.observation());
                need(role + ":CURRENT_PUBLIC_ENTRY", a != null && a.covered()
                        && (!b.observation().ambiguous()&&!b.observation().unsupportedEffects()
                            || !role.equals("opponent")&&field.ownedForm(b.observation(),b.lease())));
            }
            need(role + ":ITEM_SUPPRESSION", b.item().known() && b.item().value().equals("none") || field.magicRoom().known());
        }
        Certificate finish() { return new Certificate(field, op, required, missing, () -> (ownZ==null||ownZ.current()) && scopes.current() && leases.stream().allMatch(OwnRequestTransport.Lease::current)); }
    }
    private boolean supported(String role, boolean reserve) {
        // Raw reserves cannot certify Download or active weather suppression.
        // The evaluator establishes legal entry and participation separately.
        if(reserve&&(ability(role).equals("download")||weatherSuppressor(ability(role))||setterWeather(ability(role))!=null))return false;
        for (Operation op : Operation.values()) { var c = new Check(op); c.actor(role, reserve); if (!c.finish().supported()) return false; }
        return rawWeather().known() && field.terrain().known() && field.trickRoom().known() && field.magicRoom().known();
    }
    private Profile certified(Profile p) {
        return new Profile(p.actor(), p.level(), p.maxHp(), p.stats(), p.types(), Known.of(true, field.token() + ":operation-certified"));
    }
    public Certificate damageCoverage(String moveId, Profile attacker, Actor a, Profile target, Actor t) {
        bound(attacker, a); bound(target, t); var c = new Check(Operation.DAMAGE);
        c.actor(a.id(), reserve(a.id())); c.actor(t.id(), reserve(t.id()));
        c.need("OWN_Z_ACTIVATION",zAllowed(moveId,a.id()));
        Move m = move(moveId); c.need("SUPPORTED_DAMAGE_MOVE", m != null && m.power() > 0);
        var defender=actors.get(t.id());
        if(defender!=null&&defender.ability().known()&&defender.ability().value().equals("shadowshield"))
            c.need("SHADOW_SHIELD_FULL_HP_CERTIFICATE",t.hp().known()
                    &&(t.hp().value().low()==1.0||t.hp().value().high()<1.0));
        var weather=actorWeather(c,t.id(),a,t); c.need("WEATHER", weather.known()); c.need("TERRAIN", field.terrain().known());
        var attackWeather=actorWeather(c,a.id(),a,t);c.need("ATTACKER_WEATHER",attackWeather.known());
        c.need(a.id() + ":CURRENT_TYPES", attacker.types().known()); c.need(t.id() + ":CURRENT_TYPES", target.types().known());
        if (m != null && m.power() > 0) {
            var active = active(t.id());
            boolean blocked = !OwnZMoveEvidence.MOVE.equals(moveId) && active != null && active.volatiles().contains("protect")
                    || target.types().known() && effectiveness(m.type(), target.types().value()) == Integer.MIN_VALUE;
            if (!blocked) {
                String atk = m.attackingStat(), def = m.defendingStat();
                c.need(a.id() + ":LEVEL", attacker.level().known());
                c.need(a.id() + ":" + atk, attacker.stat(atk).known() && a.stages().containsKey(atk));
                c.need(t.id() + ":" + def, target.stat(def).known() && t.stages().containsKey(def));
                c.need(t.id() + ":MAX_HP", target.maxHp().known()); c.need(a.id() + ":STATUS", a.status().known());
                if (c.missing.isEmpty()) {
                    int n = m.type().equals("fire") && weather.value().equals("sunnyday") ? 3 : 1;
                    int d = m.type().equals("fire") && Set.of("sunnyday","raindance").contains(weather.value()) ? 2 : 1;
                    // Certify the full supported critical domain, refusing zero-stat
                    // and engine-wrap domains before issuing a usable certificate.
                    var probe = CobblemonMechanics.damage(moveId, certified(attacker), a, certified(target), t,
                            Critical.BOTH,n,d,sourcePowerModifier(m,attacker,t.id(),weather.value()),4096,
                            finalDamageModifier(a.id(),t.id(),t),4096,sourceDefenseModifier(m,target,weather.value()),4096,sourceAttackModifier(m,a.id(),attackWeather.value()),4096);
                    c.need("SUPPORTED_DAMAGE_ARITHMETIC_DOMAIN", probe.fraction().known());
                    c.missing.addAll(probe.unknown());
                }
            }
        }
        return c.finish();
    }
    /** Accuracy uses the existing damage-operation source/actor receipt. A
     * successful-hit damage estimate remains independent of hit probability. */
    public Certificate accuracyCoverage(String moveId, Profile attacker, Actor a, Profile target, Actor t) {
        bound(attacker,a); bound(target,t); var c=new Check(Operation.DAMAGE);
        c.actor(a.id(),reserve(a.id())); c.actor(t.id(),reserve(t.id()));
        c.need("OWN_Z_ACTIVATION",zAllowed(moveId,a.id()));
        Move m=move(moveId); c.need("SUPPORTED_ACCURACY_MOVE",m!=null&&m.power()>0);
        var weather=actorWeather(c,t.id(),a,t);c.need("ACCURACY_WEATHER",weather.known());
        boolean always=OwnZMoveEvidence.MOVE.equals(moveId) || moveId.equals("swift") || moveId.equals("thunder")
                &&weather.known()&&weather.value().equals("raindance");
        if(!always){
            c.need(a.id()+":ACCURACY_STAGE",a.stages().containsKey("accuracy"));
            c.need(t.id()+":EVASION_STAGE",t.stages().containsKey("evasion"));
        }
        return c.finish();
    }
    /** Native integer threshold, not a prediction of the RNG draw. Values at
     * least 100 guarantee this supported accuracy check, not overall execution. */
    public Known<Integer> accuracy(String moveId, Profile attacker, Actor a, Profile target, Actor t) {
        var coverage=accuracyCoverage(moveId,attacker,a,target,t);
        if(!coverage.supported())return Known.unknown();
        var weather=effectiveActorWeather(t.id(),a,t);if(!weather.known())return Known.unknown();
        if(OwnZMoveEvidence.MOVE.equals(moveId) || moveId.equals("swift") || moveId.equals("thunder")&&weather.value().equals("raindance"))
            return Known.of(100,coverage.source());
        int base=moveId.equals("thunder")&&weather.value().equals("sunnyday")?50:move(moveId).accuracy();
        // Native ModifyAccuracy runs before stage truncation. Weather evasion acts
        // on the target's incoming numeric accuracy; always-hit checks above
        // retain their exemption. The source applies round-half-down here.
        if(ability(t.id()).equals("sandveil")&&weather.value().equals("sandstorm")
                ||ability(t.id()).equals("snowcloak")&&Set.of("hail","snow").contains(weather.value()))
            base=(base*3277+2047)/4096;
        int boost=Math.max(-6,Math.min(6,a.stages().get("accuracy")-t.stages().get("evasion")));
        int threshold=boost>=0?base*(3+boost)/3:base*3/(3-boost);
        return Known.of(threshold,coverage.source());
    }
    public Certificate setupCoverage(Profile p, Actor a, String moveId) {
        bound(p, a); var c = new Check(Operation.SETUP); c.actor(a.id(), reserve(a.id()));
        Move m = move(moveId); c.need("SUPPORTED_SETUP_MOVE", m != null && m.power() == 0);
        c.need("SUPPORTED_SETUP_EXECUTION", m != null && (!m.id().equals("geomancy") || activePowerHerb(a.id())));
        // Burn/poison do not veto BeforeMove in the pinned source. This also
        // covers a burn secondary received before a queued self-target setup;
        // root residual/status-duration restrictions remain in the evaluator.
        c.need(a.id() + ":STATUS", a.status().known() && Set.of("none","par","brn","psn","tox").contains(a.status().value()));
        if (m != null && m.id().equals("recover")) c.need(a.id() + ":EXACT_RECOVERY_MAX_HP",
                p.maxHp().known() && p.maxHp().value().low() == p.maxHp().value().high());
        if (m != null) m.boosts().keySet().forEach(stat -> c.need(a.id() + ":STAGE_" + stat, a.stages().containsKey(stat)));
        return c.finish();
    }
    public Certificate speedCoverage(Profile p, Actor a) {
        return speedCoverage(p,a,new Actor[0]);
    }
    private Certificate speedCoverage(Profile p, Actor a,Actor... others) {
        bound(p, a); var c = new Check(Operation.SPEED); c.actor(a.id(), reserve(a.id()));
        c.need(a.id() + ":SPEED", p.stat("spe").known() && a.stages().containsKey("spe"));
        c.need(a.id() + ":STATUS", a.status().known()
                && Set.of("none","par","slp","frz","brn","psn","tox").contains(a.status().value()));
        Binding b = actors.get(reserve(a.id()) ? "self" : a.id());
        c.need(a.id() + ":TAILWIND", b != null && b.observation() != null && field.tailwind(b.observation().slot()).known());
        if (actors.get(a.id()) != null && actors.get(a.id()).ability().known() && speedWeather(ability(a.id()))!=null)
            c.need(a.id() + ":" + switch(ability(a.id())){
                case "sandrush" -> "SAND_RUSH_WEATHER";
                case "chlorophyll" -> "CHLOROPHYLL_WEATHER";
                case "slushrush" -> "SLUSH_RUSH_WEATHER";
                default -> "SWIFT_SWIM_WEATHER";
            }, actorWeather(c,a.id(),participants(a,others)).known());
        if (c.missing.isEmpty()) c.need("SUPPORTED_SPEED_ARITHMETIC_DOMAIN", computedSpeed(p, a,others).known());
        return c.finish();
    }
    public Certificate orderCoverage(String action, String reply, Profile self, Actor a, Profile foe, Actor f) {
        bound(self, a); bound(foe, f); var c = new Check(Operation.ORDER);
        c.actor(a.id(), reserve(a.id())); c.actor(f.id(), reserve(f.id()));
        Move own = action.startsWith("move:") ? move(action.substring(5)) : null;
        c.need("SUPPORTED_ACTION_PAIR", (action.startsWith("switch:") || own != null) && move(reply) != null);
        if (!action.startsWith("switch:")) {
            var acPriority = priorityCoverage(own == null ? "UNKNOWN" : own.id(), a.id());
            var fcPriority = priorityCoverage(reply, f.id());
            c.required.addAll(acPriority.prerequisites()); c.required.addAll(fcPriority.prerequisites());
            c.missing.addAll(acPriority.missing()); c.missing.addAll(fcPriority.missing());
            if (c.missing.isEmpty() && effectivePriority(own.id(), a.id()) == effectivePriority(reply, f.id())) {
                c.required.add("TIED_PRIORITY_SPEED_AND_ROOM");
                var ac = speedCoverage(self, a,f); var fc = speedCoverage(foe, f,a);
                c.required.addAll(ac.prerequisites()); c.required.addAll(fc.prerequisites());
                c.missing.addAll(ac.missing()); c.missing.addAll(fc.missing());
                c.need("TRICK_ROOM", field.trickRoom().known());
                if (c.missing.isEmpty()) {
                    int limit = field.trickRoom().value() ? 1807 : 8191;
                    c.need("SOURCE_ACTION_SPEED_WRAP_DOMAIN", computedSpeed(self, a,f).value().high() <= limit
                            && computedSpeed(foe, f,a).value().high() <= limit);
                }
            }
        }
        return c.finish();
    }
    public Certificate entryCoverage(Hazards hazards, Known<Integer> maxHp, Profile reserve) {
        if (Set.of("self", "opponent").contains(reserve.actor())) throw new IllegalArgumentException("entry requires an own reserve binding");
        var c = new Check(Operation.ENTRY); c.actor(reserve.actor(), true);
        c.need("OWN_MAX_HP", maxHp.known()); c.need("RESERVE_TYPES", reserve.types().known());
        c.need("OWN_MAX_HP_PROFILE_BINDING", maxHpMatches(maxHp, reserve));
        c.need("COMPLETE_HAZARD_LAYERS", hazards.complete());
        return c.finish();
    }
    /** Native Download uses getStat(false,true): observed stages and integer
     * floors, without ModifyDef/ModifySpD (including sand's Rock bonus). Called
     * after all entry hazards with proven living participants. A public interval
     * may prove the selection; overlapping alternatives never become a build fact.
     */
    Known<Map<String,Integer>> entryAbilityBoosts(Profile reserve,Actor arriving,Profile foe,Actor target) {
        bound(reserve,arriving);var c=new Check(Operation.ENTRY);c.actor(arriving.id(),true);
        if(!ability(arriving.id()).equals("download"))
            return c.finish().supported()?Known.of(Map.of(),field.token()+":no-modeled-entry-boost"):Known.unknown();
        if(foe==null)return Known.unknown();
        bound(foe,target);c.actor(target.id(),false);
        c.need("DOWNLOAD_OPPONENT_BINDING",target.id().equals("opponent"));
        c.need("DOWNLOAD_ENTRY_SURVIVAL",arriving.hp().known()&&arriving.hp().value().low()>0&&!arriving.facts().fainted());
        c.need("DOWNLOAD_TARGET_PARTICIPATION",target.hp().known()&&target.hp().value().low()>0&&!target.facts().fainted());
        for(String stat:List.of("def","spd")) {
            c.need("DOWNLOAD_PUBLIC_"+stat.toUpperCase(Locale.ROOT),foe.stat(stat).known());
            Integer boost=target.stages().get(stat);
            c.need("DOWNLOAD_CURRENT_"+stat.toUpperCase(Locale.ROOT)+"_STAGE",boost!=null&&boost>=-6&&boost<=6);
        }
        if(!c.finish().supported())return Known.unknown();
        var def=foe.stat("def").value();var spd=foe.stat("spd").value();
        int dl=stage(def.low(),target.stages().get("def")),dh=stage(def.high(),target.stages().get("def"));
        int sl=stage(spd.low(),target.stages().get("spd")),sh=stage(spd.high(),target.stages().get("spd"));
        Map<String,Integer> change;
        if(dh==0&&sh==0)change=Map.of();
        else if(dl>0&&dl>=sh)change=Map.of("spa",1); // positive ties select SpA
        else if(sl>0&&(dh==0||dh<sl))change=Map.of("atk",1);
        else return Known.unknown();
        return c.finish().supported()?Known.of(change,field.token()+":source-Download-entry-comparison"):Known.unknown();
    }
    /** Entry status and side changes are separate from the existing HP/speed
     * result. The caller must prove survival through all entry hazards first. */
    record EntryEffects(Known<String> status,Hazards hazards,Set<String> unknown) {}
    EntryEffects entryEffects(Hazards hazards,Known<Integer> max,Profile reserve,Known<String> status) {
        var cert=entryCoverage(hazards,max,reserve);
        if(!cert.supported()||!status.known())return new EntryEffects(Known.unknown(),hazards,Set.of("ENTRY_STATUS_OR_SOURCE_UNKNOWN"));
        if(ability(reserve.actor()).equals("insomnia")&&status.value().equals("slp"))
            return new EntryEffects(Known.unknown(),hazards,Set.of("INSOMNIA_ENTRY_STATUS_RECOVERY_UNPROVED"));
        int layers=hazards.layers().getOrDefault("toxicspikes",0);
        if(layers==0||!grounded(reserve))return new EntryEffects(status,hazards,Set.of());
        // Native Poison absorption precedes Steel/Boots and existing status.
        if(reserve.types().value().contains("poison")) {
            var remaining=new TreeMap<>(hazards.layers());remaining.remove("toxicspikes");
            return new EntryEffects(status,new Hazards(remaining,true,field.token()+":toxic-spikes-absorbed"),Set.of());
        }
        if(reserve.types().value().contains("steel")||item(reserve.actor()).equals("heavydutyboots")||!status.value().equals("none"))
            return new EntryEffects(status,hazards,Set.of());
        return new EntryEffects(Known.of(layers==1?"psn":"tox",field.token()+":toxic-spikes-entry"),hazards,Set.of());
    }
    private boolean reserve(String role) { return actors.containsKey(role) && actors.get(role).observation() == null; }
    public Profile profile(Profile raw, boolean reserve) {
        return new Profile(raw.actor(), raw.level(), raw.maxHp(), raw.stats(), raw.types(),
                supported(raw.actor(), reserve) ? Known.of(true, field.token() + ":limited-conditional-scope") : Known.unknown());
    }
    private int sourcePowerModifier(Move move,Profile attacker,String t,String weather) {
        String a=attacker.actor();
        int modifier=4096;
        // Native BasePower priority 30: Technician tests power before Fairy
        // Aura's priority-20 contribution. This slice has no earlier BP hooks.
        if(ability(a).equals("technician")&&move.power()<=60)
            modifier=chainModifier(modifier,6144);
        // Native priority 21: Sand Force modifies base power, independently
        // of the later Life Orb/Shadow Shield final-damage chain.
        if(ability(a).equals("sandforce")&&weather.equals("sandstorm")
                &&Set.of("rock","ground","steel").contains(move.type()))
            modifier=chainModifier(modifier,5325);
        // Aura is one field contribution even when both active actors have it.
        if(move.type().equals("fairy")&&(ability(a).equals("fairyaura")||ability(t).equals("fairyaura")))
            modifier=chainModifier(modifier,5448);
        // Priority 6: Psychic Terrain modifies grounded ATTACKER base power,
        // after the supported ability hooks and before the damage formula.
        if(move.type().equals("psychic")&&field.terrain().value().equals("psychicterrain")&&grounded(attacker))
            modifier=chainModifier(modifier,5325);
        return modifier;
    }
    private int finalDamageModifier(String a,String t,Actor target) {
        int modifier=4096;
        if(ability(t).equals("shadowshield")&&target.hp().value().low()==1.0)
            modifier=chainModifier(modifier,2048);
        if(item(a).equals("lifeorb"))modifier=chainModifier(modifier,5324);
        return modifier;
    }
    private static int chainModifier(int previous,int next) {
        // Native chainModify rounds the 12-bit modifier half UP. Applying
        // the completed chain to power/damage later rounds half DOWN.
        return (int)(((long)previous*next+2048)>>12);
    }
    private String item(String role) { return field.magicRoom().known() && field.magicRoom().value() ? "none" : actors.get(role).item().value(); }
    private String ability(String role) { return actors.get(role).ability().value(); }
    private PublicFieldEvidence.Active active(String role) { Binding b = actors.get(role); return b == null ? null : field.actor(b.observation()); }
    public boolean taunted(String role) { var a = active(role); return a != null && a.volatiles().contains("taunt"); }
    /** Fresh public Protect is checked before Spectral Thief's PRE-HIT boost transfer.
     * Damage-only gating would otherwise leave an impossible stolen stage vector.
     */
    boolean protectedTarget(String role) {
        var a=active(role);
        return sourceCurrent()&&a!=null&&a.covered()&&a.volatiles().contains("protect");
    }
    public Known<Boolean> grounded(String role, Profile profile, boolean reserve) {
        if (!role.equals(profile.actor())) throw new IllegalArgumentException("groundedness profile belongs to another actor");
        var c = new Check(Operation.ENTRY); c.actor(role, reserve);
        if (!c.finish().supported() || !profile.types().known()) return Known.unknown();
        return Known.of(grounded(profile), field.token());
    }
    /** Caller must first certify its operation, current types and actor effects.
     * Unsupported grounding items, Gravity, volatiles and ability suppression
     * fail those gates; they are never reduced to this ordinary two-rule slice.
     */
    private boolean grounded(Profile profile) {
        return !profile.types().value().contains("flying")&&!ability(profile.actor()).equals("levitate");
    }
    public Damage damage(String moveId, Profile attacker, Actor a, Profile target, Actor t, Critical critical) {
        bound(attacker, a); bound(target, t);
        var coverage = damageCoverage(moveId, attacker, a, target, t);
        if (!coverage.supported()) return new Damage(Known.unknown(), Known.unknown(), coverage.missing(), coverage.source());
        var weather=effectiveActorWeather(t.id(),a,t);if(!weather.known())return Damage.unknown("WEATHER_PARTICIPATION_UNKNOWN");
        var attackWeather=effectiveActorWeather(a.id(),a,t);if(!attackWeather.known())return Damage.unknown("ATTACKER_WEATHER_PARTICIPATION_UNKNOWN");
        Move m = move(moveId); if (m == null || m.power() == 0) return Damage.unknown("UNSUPPORTED_DAMAGE_MOVE");
        var targetActive = active(t.id());
        boolean protect=targetActive != null && targetActive.volatiles().contains("protect");
        if (protect && !OwnZMoveEvidence.MOVE.equals(moveId)) return zero("public Protect");
        // Native TryHit tests the move's priority, not its fractional queue
        // ordering, and tests the TARGET's grounding independently of boost.
        if(field.terrain().value().equals("psychicterrain")&&movePriority(moveId,a.id())>.1&&grounded(target))
            return zero("public Psychic Terrain priority block");
        int n = 1, d = 1;
        if (m.type().equals("fire")) {
            if (weather.value().equals("sunnyday")) { n = 3; d = 2; }
            else if (weather.value().equals("raindance")) { n = 1; d = 2; }
        }
        // Technician modifies base power before the level/stat formula. Never
        // multiply a final damage interval; the engine rounds at each stage.
        var damage=CobblemonMechanics.damage(moveId,certified(attacker),a,certified(target),t,critical,
                n,d,sourcePowerModifier(m,attacker,t.id(),weather.value()),4096,finalDamageModifier(a.id(),t.id(),t),4096,
                sourceDefenseModifier(m,target,weather.value()),4096,sourceAttackModifier(m,a.id(),attackWeather.value()),4096);
        if(!protect||!damage.points().known())return damage; // Immunity remains zero; missing arithmetic stays UNKNOWN.
        int low=Math.max(1,(damage.points().value().low()+1)/4),high=Math.max(1,(damage.points().value().high()+1)/4);
        return new Damage(Known.of(new IntRange(low,high),damage.source()),
                Known.of(new Interval((double)low/target.maxHp().value().high(),(double)high/target.maxHp().value().low()),damage.source()),
                damage.unknown(),damage.source());
    }
    private int sourceAttackModifier(Move move,String attacker,String weather) {
        // Native ModifySpA follows critical-adjusted stages, independently of
        // the defender's ordinary weather-damage multiplier and umbrella.
        return !move.physical()&&ability(attacker).equals("solarpower")&&weather.equals("sunnyday")?6144:4096;
    }
    private int sourceDefenseModifier(Move move,Profile target,String weather) {
        return weather.equals("sandstorm")&&move.defendingStat().equals("spd")
                &&target.types().value().contains("rock")
                ||weather.equals("snow")&&move.defendingStat().equals("def")&&target.types().value().contains("ice")?6144:4096;
    }
    private static Damage zero(String why) {
        return new Damage(Known.unknown(), Known.of(new Interval(0, 0), PublicFieldEvidence.SOURCE + ":" + why), Set.of(), PublicFieldEvidence.SOURCE);
    }
    public Certificate priorityCoverage(String moveId, String role) {
        var c = new Check(Operation.PRIORITY); c.actor(role, reserve(role));
        c.need("OWN_Z_ACTIVATION",zAllowed(moveId,role));
        c.need("SUPPORTED_PRIORITY_MOVE", move(moveId) != null); return c.finish();
    }
    public Known<Double> priority(String moveId, String role) {
        var coverage = priorityCoverage(moveId, role);
        return coverage.supported() ? Known.of(effectivePriority(moveId, role), coverage.source()) : Known.unknown();
    }
    private int movePriority(String id,String role) {
        Move m=move(id);int p=m.priority();
        if (m.power() == 0 && ability(role).equals("prankster")) p++;
        return p;
    }
    private double effectivePriority(String id, String role) {
        double p=movePriority(id,role);
        if (ability(role).equals("stall") || item(role).equals("laggingtail")) p -= .1;
        return p;
    }
    public Known<IntRange> speed(Profile p, Actor a) {
        return speed(p,a,new Actor[0]);
    }
    Known<IntRange> speed(Profile p, Actor a,Actor... others) {
        bound(p, a);
        if (!speedCoverage(p, a,others).supported()) return Known.unknown();
        return computedSpeed(p, a,others);
    }
    private Known<IntRange> computedSpeed(Profile p, Actor a,Actor... others) {
        Known<IntRange> base = CobblemonMechanics.stagedSpeed(certified(p), a);
        Binding b = actors.get(reserve(a.id()) ? "self" : a.id());
        Known<Boolean> wind = b == null || b.observation() == null ? Known.unknown() : field.tailwind(b.observation().slot());
        if (!base.known() || !wind.known()) return Known.unknown();
        String boostWeather=speedWeather(ability(a.id()));
        var weather=boostWeather!=null?actorWeather(new Check(Operation.SPEED),a.id(),participants(a,others)):rawWeather();
        if(boostWeather!=null&&!weather.known())return Known.unknown();
        int[] modifiers={item(a.id()).equals("choicescarf")?6144:4096,
                wind.value()?8192:4096,boostWeather!=null&&(weather.value().equals(boostWeather)||ability(a.id()).equals("slushrush")&&weather.value().equals("hail"))?8192:4096};
        int low=Integer.MAX_VALUE, high=0;
        // Paralysis flushes the preceding modifier chain, floors speed / 2,
        // then lets the remaining callbacks form a new chain. Actor callbacks
        // tie; zero cached speed can tie Tailwind too. Enumerate every partition
        // around paralysis without consulting private native cached speed.
        int partitions=a.status().value().equals("par")?8:1;
        for(int mask=0;mask<partitions;mask++){
            int before=4096,after=4096;
            for(int i=0;i<modifiers.length;i++){
                if((mask&(1<<i))!=0)before=chainModifier(before,modifiers[i]);
                else after=chainModifier(after,modifiers[i]);
            }
            int lo=modifySpeed(base.value().low(),before),hi=modifySpeed(base.value().high(),before);
            if(partitions>1){lo/=2;hi/=2;}
            low=Math.min(low,modifySpeed(lo,after));high=Math.max(high,modifySpeed(hi,after));
        }
        if (low < 1 || high > 10000) return Known.unknown();
        return Known.of(new IntRange(low, high), field.token());
    }
    private static int modifySpeed(int speed,int modifier){return (int)(((long)speed*modifier+2047)>>12);}
    private static boolean maxHpMatches(Known<Integer> maxHp, Profile reserve) {
        return maxHp.known() && reserve.maxHp().known() && reserve.maxHp().value().low() == maxHp.value()
                && reserve.maxHp().value().high() == maxHp.value();
    }
    public Order order(String action, String reply, Profile self, Actor a, Profile foe, Actor f) {
        bound(self, a); bound(foe, f);
        if (!orderCoverage(action, reply, self, a, foe, f).supported()) return Order.UNKNOWN;
        if (action.startsWith("switch:")) return Order.SELF_FIRST;
        Move m = action.startsWith("move:") ? move(action.substring(5)) : null; if (m == null) return Order.UNKNOWN;
        double p = effectivePriority(m.id(), a.id()), q = effectivePriority(reply, f.id());
        if (p != q) return p > q ? Order.SELF_FIRST : Order.OPPONENT_FIRST;
        // Both fractional priorities equal: reuse verified bounded speed ordering.
        return CobblemonMechanics.order("move:tackle", "tackle", speed(self, a,f), speed(foe, f,a), field.trickRoom(), Known.of(true, field.token()));
    }
    public Entry entry(Hazards hazards, Known<Integer> maxHp, Profile reserve) {
        if (Set.of("self", "opponent").contains(reserve.actor())) throw new IllegalArgumentException("entry requires an own reserve binding");
        var ground = grounded(reserve.actor(), reserve, true);
        var c = new Check(Operation.ENTRY); c.actor(reserve.actor(), true);
        if (!c.finish().supported() || !maxHpMatches(maxHp, reserve)) return CobblemonMechanics.entry(hazards, maxHp, Known.unknown(), Known.unknown(),
                Known.unknown(), Known.unknown(), Known.unknown());
        var rock = reserve.types().known() ? Known.of(effectiveness("rock", reserve.types().value()), field.token()) : Known.<Integer>unknown();
        return CobblemonMechanics.entry(hazards, maxHp, ground, Known.of(item(reserve.actor()).equals("heavydutyboots"), field.token()),
                Known.of(ability(reserve.actor()).equals("magicguard"), field.token()), rock, Known.of(true, field.token()));
    }
}
