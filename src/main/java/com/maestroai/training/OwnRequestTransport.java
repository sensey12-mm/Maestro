package com.maestroai.training;

import java.util.*;
import java.util.function.Supplier;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.CobblemonMechanics.*;

/** Owning-actor dispatch ledger. No history scanner and no opponent payload API.
 * Raw baseStoredStats are retained separately from justified effective profiles.
 */
public final class OwnRequestTransport {
    public static final String SOURCE = "cobblemon-7553235:own-dispatch-v1";
    public enum Mode { MOVE, FORCED, WAIT }
    public record Pokemon(OwnRequestEvidence.Sample dto, int level, Map<String, Integer> baseStats,
                          String item, List<String> baseTypes, String tera, boolean special, String species) {
        public Pokemon { baseStats = Map.copyOf(baseStats); baseTypes = List.copyOf(baseTypes); }
        public Pokemon(OwnRequestEvidence.Sample dto,int level,Map<String,Integer> stats,String item,List<String> types,String tera,boolean special) {
            this(dto,level,stats,item,types,tera,special,"");
        }
    }
    public record Request(String side, Mode mode, List<Pokemon> pokemon, OwnZMoveEvidence.Choice selection) {
        public Request { pokemon = List.copyOf(pokemon); }
        public Request(String side,Mode mode,List<Pokemon> pokemon){this(side,mode,pokemon,null);}
    }
    public record Evidence(Profile profile, Known<String> item, Known<String> ability, Known<String> status,
                           Known<Boolean> fresh, Set<String> unknown, Known<PublicStateSearchAdapter.Interval> hp) {
        public Evidence { unknown = Set.copyOf(unknown); }
    }
    public record Effective(Evidence evidence, Map<String,Integer> stages, Set<String> missing) {
        public Effective { stages = Map.copyOf(stages); missing = Set.copyOf(missing); }
    }
    private Object battle, actor, token;
    private String side;
    private Request receipt;
    private Request previous;
    private int turn, receiptTurn = -1;
    private long epoch;
    private boolean choiceOpen, used, turnDispatchOpen;
    private final Set<UUID> initialReserves = new HashSet<>();
    private final Set<String> reasons = new TreeSet<>();

    private boolean turnReceipt;

    public void owner(Object battle, Object actor, String side) {
        if (this.battle == battle && this.actor == actor && Objects.equals(this.side, side)) return;
        this.battle = battle; this.actor = actor; this.side = side;
        turn = 0; turnDispatchOpen = false; turnReceipt = false; previous = null; initialReserves.clear(); invalidate("OWN_DISPATCH_NOT_OBSERVED");
    }
    public void invalidate(String reason) {
        epoch++; choiceOpen = false; turnDispatchOpen = false; turnReceipt = false; token = null; receipt = null; reasons.clear(); reasons.add(reason);
    }
    /** The lazy parser is NEVER invoked for a foreign actor, battle or side. */
    public void dispatched(Object battle, Object actor, String side, Object installedToken, Supplier<Request> parser) {
        if (this.battle != battle || this.actor != actor || !Objects.equals(this.side, side)) return;
        epoch++; choiceOpen = false; used = false; receipt = null; token = installedToken; reasons.clear();
        try {
            Request r = parser.get();
            if (installedToken == null || r == null || !Set.of("p1", "p2").contains(side) || !side.equals(r.side()))
                throw new IllegalArgumentException();
            if (r.equals(previous) && receiptTurn == turn) { reasons.add("DUPLICATE_OWN_DISPATCH"); return; }
            boolean first = previous == null;
            previous = r; receiptTurn = turn;
            if (r.mode() == Mode.WAIT) { reasons.add("OWN_WAIT_REQUEST"); return; }
            var uuids = new HashSet<UUID>(); int active = 0;
            for (Pokemon p : r.pokemon()) {
                if (p.dto().uuid() == null || !uuids.add(p.dto().uuid()) || !p.dto().ident().startsWith(side + ": "))
                    throw new IllegalArgumentException();
                if (p.dto().active()) active++;
            }
            if (active != 1 || r.pokemon().isEmpty() || r.pokemon().size() > 6) throw new IllegalArgumentException();
            receipt = r;
            // The initial construction receipt remains evidence for a reserve
            // that has never entered. Its current stats/item/HP still come from
            // THIS dispatch; this stores identities, never stale numeric data.
            if (first && turn == 0 && r.mode() == Mode.MOVE)
                for (Pokemon p : r.pokemon()) if (!p.dto().active() && !p.special()
                        && p.dto().condition() != null && p.dto().condition().matches("[1-9][0-9]*/[1-9][0-9]*"))
                    initialReserves.add(p.dto().uuid());
        } catch (RuntimeException invalid) { reasons.add("OWN_REQUEST_SCHEMA_OR_IDENTITY_UNSUPPORTED"); }
    }
    /** Called at entry to the real TurnInstruction dispatch, BEFORE its choice packet. */
    public void turnDispatched(Object battle, int nextTurn) {
        if (this.battle != battle) return;
        epoch++; choiceOpen = false;
        if (nextTurn != turn + 1) { initialReserves.clear(); invalidate("OWN_TURN_SEQUENCE_UNSUPPORTED"); turn = Math.max(turn, nextTurn); return; }
        turn = nextTurn; turnDispatchOpen = true; turnReceipt = true;
    }
    public void turnFinished(Object battle, int dispatchedTurn) {
        if (this.battle == battle && dispatchedTurn == turn) turnDispatchOpen = false;
    }
    public boolean observedTurnAllowed(int actualBattleTurn) {
        return turn > 0 && (actualBattleTurn == turn || turnDispatchOpen && actualBattleTurn == turn - 1);
    }
    /** Stable turn from the owning TurnInstruction receipt, including its
     * source-verified window before PokemonBattle.turn writes the Java field.
     * Never invent a +1 turn from an arbitrary caller's Java counter.
     */
    public int dispatchedTurn(Object battle, Object actor, int actualBattleTurn) {
        return turnReceipt && this.battle == battle && this.actor == actor && observedTurnAllowed(actualBattleTurn) ? turn : -1;
    }
    public final class Lease {
        private final long generation;
        private final int capturedTurn;
        private final Supplier<Boolean> installed;
        private Lease(Supplier<Boolean> installed) { this.generation = epoch; this.capturedTurn = turn; this.installed = installed; }
        public int turn() { return capturedTurn; }
        private boolean belongsTo(OwnRequestTransport ledger) { return OwnRequestTransport.this == ledger; }
        public boolean current() {
            try { return generation == epoch && choiceOpen && receipt != null && Boolean.TRUE.equals(installed.get()); }
            catch (RuntimeException unavailable) { return false; }
        }
        public String source() { return SOURCE + ":dispatch-" + generation + ":turn-" + turn(); }
    }
    public Lease beginChoice(Object battle, Object actor, Object installedToken, UUID active, boolean forced,
                             Supplier<Boolean> installed) {
        epoch++; choiceOpen = false;
        boolean mode = receipt != null && forced == (receipt.mode() == Mode.FORCED);
        boolean atTurn = receipt != null && (forced ? receiptTurn == turn : receiptTurn + 1 == turn);
        boolean activeMatch = receipt != null && receipt.pokemon().stream().anyMatch(p -> p.dto().active() && p.dto().uuid().equals(active));
        if (this.battle == battle && this.actor == actor && token == installedToken && !used && turn > 0 && mode && atTurn && activeMatch) {
            choiceOpen = true; used = true;
        }
        return new Lease(installed);
    }
    public void endChoice() { epoch++; choiceOpen = false; }
    private static String nickname(String ident) {
        int colon = ident == null ? -1 : ident.indexOf(':');
        return colon < 0 ? "" : ident.substring(colon + 1).trim();
    }
    /** Current own dispatch and native UI must independently agree. No crystal-only eligibility. */
    public OwnZMoveEvidence ownZMove(Lease lease,RuntimeDomain domain,PublicMechanicsEvidence.Observation own,
            PublicFieldEvidence.Capture field,Supplier<OwnZMoveEvidence.Choice> installedChoice) {
        if(lease==null||!lease.belongsTo(this)||!lease.current()||receipt==null||receipt.mode()!=Mode.MOVE)
            return null;
        return OwnZMoveEvidence.inspect(lease,domain,own,field,receipt,installedChoice);
    }
    public PublicFieldEvidence.Capture rebindOwnZUse(Lease lease,RuntimeDomain domain,
            PublicProtocolEvidence protocol,PublicFieldEvidence.Capture field){
        if(lease==null||!lease.belongsTo(this)||!lease.current()||receipt==null||receipt.mode()!=Mode.MOVE)return field;
        try{
            var proof=OwnZUseEvidence.inspect(field,lease,domain,protocol,receipt,receiptTurn);
            if(proof==null)return field;
            var rebound=field.rebind(proof);
            return proof.sourceCurrent()?rebound:rebound.withoutOwnedForm();
        }catch(IllegalArgumentException unavailable){return field;}
    }
    /** Only a fresh MOVE request can rebind this owner's exact native Mega-Y
     * history. The spectator observations and every opponent guard stay intact. */
    public PublicFieldEvidence.Capture rebindOwnMegaY(Lease lease,RuntimeDomain domain,
            PublicProtocolEvidence protocol,PublicMechanicsEvidence.Observation active,PublicFieldEvidence.Capture field){
        if(lease==null||!lease.belongsTo(this)||!lease.current()||receipt==null||receipt.mode()!=Mode.MOVE)return field;
        try{
            var own=receipt.pokemon().stream().filter(p->p.dto().active()).findFirst().orElseThrow();
            var proof=OwnMegaYEvidence.inspect(field,lease,domain,protocol,active,own,side,receiptTurn);
            if(proof==null)return field;
            var rebound=field.rebind(proof);
            return proof.sourceCurrent()?rebound:rebound.withoutOwnedForm();
        }catch(IllegalArgumentException|NoSuchElementException unavailable){return field;}
    }
    /** Runtime entry/lifecycle proof. No caller-supplied 'ordinary' boolean.
     * A reserve needs witnessed initial construction or its actual public exit;
     * a pending switch is never treated as if it has already cleared volatiles.
     */
    public Effective effective(Lease lease, String role, UUID uuid, boolean reserve, RuntimeDomain domain,
                               PublicProtocolEvidence protocol, PublicMechanicsEvidence.Observation active,
                               PublicFieldEvidence.Capture field) {
        if (role.equals("opponent")) throw new IllegalArgumentException("own transport cannot bind opponent");
        var missing = new TreeSet<String>(); var stages = Map.<String,Integer>of(); boolean justified = false;
        try {
            if (lease == null || !lease.belongsTo(this) || !lease.current()) throw new IllegalArgumentException();
            if (domain == null || !domain.effectiveState().known()) missing.add("OWN_INITIALIZED_EFFECTIVE_DOMAIN_UNKNOWN");
            if (field == null || protocol == null || !protocol.framed()) throw new IllegalArgumentException();
            field.requireCurrent(lease.turn());
            var a = field.actor(active);
            var actual = receipt.pokemon().stream().filter(p -> p.dto().active()).findFirst().orElseThrow();
            var selected = receipt.pokemon().stream().filter(p -> p.dto().uuid().equals(uuid) && p.dto().active() != reserve).findFirst().orElseThrow();
            boolean rebound=field.ownedForm(active,lease);
            if(!field.ownedSourceCurrent())missing.add("OWN_FORM_SOURCE_SCOPE_UNVERIFIED");
            if (a == null || !a.covered() || !field.allActorsCovered() || !field.unknown().isEmpty()
                    || (active.ambiguous()||active.unsupportedEffects())&&!rebound || !active.slot().startsWith(side)
                    // Pokemon.toString publishes UUID; own request.ident instead
                    // uses fullname/nickname. Do not equate those two APIs.
                    || !nickname(active.publicName()).equals(actual.dto().uuid().toString()))
                missing.add("OWN_PUBLIC_ENTRY_OR_IDENTITY_UNJUSTIFIED");
            String lastMove = ""; boolean copyEntry = false;
            for (String line : protocol.mechanicsEvents()) {
                String[] p = line.split("\\|", -1);
                if (p.length < 3) continue;
                if (p[2].startsWith(side + "a: ") && p[1].equals("move") && p.length > 3)
                    lastMove = p[3].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                if (p[2].startsWith(side + "a: ") && Set.of("switch", "drag").contains(p[1])) {
                    copyEntry = Set.of("batonpass", "shedtail").contains(lastMove);
                    lastMove = "";
                }
            }
            if (copyEntry) missing.add("OWN_COPIED_ENTRY_STATE_UNSUPPORTED");
            if (!reserve && active.types().known() && !active.types().value().equals(selected.dto().types().stream().map(t -> t.toLowerCase(Locale.ROOT)).toList()))
                missing.add("OWN_PUBLIC_CURRENT_TYPING_MISMATCH");
            if (reserve) {
                String target = selected.dto().uuid().toString(), previous = null; boolean exited = false, everEntered = false; int switches = 0;
                for (String line : protocol.mechanicsEvents()) {
                    String[] p = line.split("\\|", -1);
                    if (Set.of("switch", "drag").contains(p[1]) && p[2].startsWith(side + "a: ")) {
                        String incoming = nickname(p[2]); switches++;
                        if (target.equals(previous) && !target.equals(incoming)) exited = true;
                        if (target.equals(incoming)) { exited = false; everEntered = true; }
                        previous = incoming;
                    } else if (p[1].equals("replace") && p[2].startsWith(side + "a: ")) {
                        missing.add("OWN_RESERVE_REVEAL_LIFECYCLE_UNSUPPORTED");
                    }
                }
                boolean initial = initialReserves.contains(uuid) && !everEntered && switches >= 1;
                if (!exited && !initial) missing.add("OWN_RESERVE_CLEAR_LIFECYCLE_UNJUSTIFIED");
                // A fainted/reviving record is not the source's unfainted clearVolatile branch.
                if (!selected.dto().condition().matches("[1-9][0-9]*/[1-9][0-9]*( (brn|par|psn|tox|slp|frz))?"))
                    missing.add("OWN_RESERVE_NOT_ORDINARY_UNFAINTED");
                if (missing.isEmpty()) { var zeros = new TreeMap<String,Integer>(); STATS.forEach(k -> zeros.put(k, 0)); stages = zeros; }
            } else if (missing.isEmpty()) {
                var observer = new PublicBattleObserver(); protocol.mechanicsEvents().forEach(observer::accept);
                String identity = observer.activeIdentity(active.slot());
                if (identity == null || observer.stagesUncertain(identity)) missing.add("OWN_PUBLIC_STAGES_UNJUSTIFIED");
                else {
                    var initialized = new TreeMap<String,Integer>(); STATS.forEach(k -> initialized.put(k, 0));
                    initialized.putAll(observer.knownStages(identity)); stages = initialized;
                }
            }
            justified = missing.isEmpty();
        } catch (IllegalArgumentException | NoSuchElementException invalid) { missing.add("OWN_EFFECTIVE_RECEIPT_OR_FIELD_NOT_CURRENT"); }
        var evidence = evidence(lease, role, uuid, reserve, justified);
        if (evidence.profile().stats().isEmpty() || !evidence.profile().types().known()) stages = Map.of();
        return new Effective(evidence, stages, missing);
    }
    public Evidence evidence(Lease lease, String role, UUID uuid, boolean reserve, boolean publicEffectiveState) {
        if (role.equals("opponent")) throw new IllegalArgumentException("own transport cannot bind opponent");
        var missing = new TreeSet<>(reasons); missing.add("PACK_EFFECT_SCOPE_UNKNOWN");
        if (lease == null || !lease.belongsTo(this) || !lease.current()) {
            missing.add("OWN_REQUEST_NOT_CURRENT");
            return new Evidence(Profile.unknown(role), Known.unknown(), Known.unknown(), Known.unknown(), Known.unknown(), missing, Known.unknown());
        }
        var old = OwnRequestEvidence.bind(role, uuid, side, reserve, receipt.pokemon().stream().map(Pokemon::dto).toList());
        var matches = receipt.pokemon().stream().filter(p -> p.dto().uuid().equals(uuid) && p.dto().active() != reserve).toList();
        if (matches.size() != 1) {
            missing.add("OWN_REQUEST_ACTIVE_OR_RESERVE_MISMATCH");
            return new Evidence(Profile.unknown(role), Known.unknown(), Known.unknown(), Known.unknown(), Known.unknown(), missing, Known.unknown());
        }
        Pokemon p = matches.getFirst(); String source = lease.source();
        var stats = new HashMap<String, Known<IntRange>>();
        boolean effective = publicEffectiveState && !p.special() && p.tera().isEmpty()
                && p.dto().types().equals(p.baseTypes());
        if (effective) for (var entry : p.baseStats().entrySet()) stats.put(entry.getKey(), Known.of(IntRange.exact(entry.getValue()), source + ":baseStoredStats"));
        else missing.add("OWN_EFFECTIVE_STATS_OR_TYPES_UNSUPPORTED");
        if (!old.profile().maxHp().known()) missing.add("OWN_HP_OR_STATUS_UNSUPPORTED");
        var types = effective ? old.profile().types() : Known.<List<String>>unknown();
        Known<PublicStateSearchAdapter.Interval> hp = Known.unknown();
        if (old.profile().maxHp().known()) {
            String[] ratio = p.dto().condition().split(" ")[0].split("/");
            double fraction = Integer.parseInt(ratio[0]) / (double)Integer.parseInt(ratio[1]);
            hp = Known.of(new PublicStateSearchAdapter.Interval(fraction, fraction), source + ":own-current-condition");
        }
        return new Evidence(new Profile(role, Known.of(p.level(), source + ":details-level"), old.profile().maxHp(), stats, types, Known.unknown()),
                Known.of(p.item(), source + ":simulator-current-item"), old.ability(), old.status(), Known.of(true, source), missing, hp);
    }
}
