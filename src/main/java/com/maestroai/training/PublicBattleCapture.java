package com.maestroai.training;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Shadow diagnostic replay with bounded, privacy-safe envelope provenance. */
public final class PublicBattleCapture {
    private static final AtomicLong NEXT_CAPTURE = new AtomicLong();
    public record Observation(List<String> replay, List<String> envelopes) {
        public Observation {
            replay = List.copyOf(replay);
            envelopes = List.copyOf(envelopes);
        }
    }

    private UUID battle;
    private List<String> previous = List.of();
    private long sequence;
    private long captureNumber;
    private boolean incomplete;
    private boolean closed;

    // The automatic reporter has an independent, native-only public history.
    // Existing tactical/legacy observeWithProvenance keeps the frozen older
    // projection and custody behavior, so diagnostics cannot affect choices.
    private List<String> reportPrevious = List.of();
    private long reportSequence;
    private boolean reportIncomplete;
    private boolean reportClosed;

    public void begin(UUID battleId) {
        if (battleId == null) throw new IllegalArgumentException("battle ID required");
        if (battle != null && !battle.equals(battleId) && captureNumber > 0 && !closed)
            AutomaticBattleReports.interrupt(captureNumber, "REPLACED");
        battle = battleId;
        previous = List.of();
        sequence = 0;
        captureNumber = NEXT_CAPTURE.incrementAndGet();
        incomplete = false;
        closed = false;
        reportPrevious = List.of();
        reportSequence = 0;
        reportIncomplete = false;
        reportClosed = false;
    }

    /** Stable opaque correlation for opt-in developer receipts; never a UUID. */
    public synchronized long forBattle(UUID battleId) {
        if (battleId == null) return -1;
        if (!battleId.equals(battle)) begin(battleId);
        return captureNumber;
    }

    public long captureNumber() { return captureNumber; }
    public long recordedEvents() { return sequence; }
    public boolean incomplete() { return incomplete; }

    private Observation rejected(String reason) {
        if (incomplete) return new Observation(List.of(), List.of());
        incomplete = true;
        // Fixed classifier only: never echo untrusted source data or private payload.
        return new Observation(List.of(
                "[MaestroAI] SHADOW_REPLAY_GAP battle=REDACTED capture=" + captureNumber + " after=" + sequence),
                List.of("[MaestroAI] SHADOW_ENVELOPE_FAILURE capture=" + captureNumber
                        + " reason=" + reason + " status=UNKNOWN"));
    }

    /*
     * A public Showdown event is NOT a permission to persist its free-form
     * nickname, player alias, HP, or arbitrary fields into developer output.
     * Project a closed vocabulary while retaining the original unmodified
     * history only in the source-side, in-memory provenance comparison.
     */
    private static final java.util.Set<String> SAFE_SPECIES = java.util.Set.of(
            "zoroark","greninja","ditto","mew","pikachu","blissey","shedinja",
            "shuckle","genesect","calyrexshadow","arceus","xerneas","marshadow",
            "mewtwo","mewtwomegay","gholdengo","charizard","gengar","lucario");
    private static final java.util.Set<String> SAFE_STATUS = java.util.Set.of(
            "brn","par","psn","tox","slp","frz","none");
    private static final java.util.Set<String> SAFE_ITEM = java.util.Set.of(
            "leftovers","choicescarf","choiceband","choicespecs","lifeorb",
            "powerherb","sitrusberry","focussash","assaultvest","blacksludge",
            "heavydutyboots","mewtwonitey","marshadiumz");
    private static final java.util.Set<String> SAFE_ABILITY = java.util.Set.of(
            "pressure","download","shadowshield","fairyaura","multitype",
            "technician","asone","levitate","intimidate","prankster");
    private static final java.util.Set<String> SAFE_TYPE = java.util.Set.of(
            "normal","fire","water","electric","grass","ice","fighting","poison",
            "ground","flying","psychic","bug","rock","ghost","dragon","dark",
            "steel","fairy","stellar");
    private static String slug(String s) {
        return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static String fromSet(String v,java.util.Set<String> safe) {
        String token=slug(v);
        return safe.contains(token)?token:"UNKNOWN";
    }
    // The public switch DETAILS species is not an actor nickname. Validate it
    // against the loaded installed public species registry (not private battle
    // Pokémon state), otherwise preserve UNKNOWN rather than free-form text.
    static String publicSpecies(String value) {
        String token=slug(value);
        if(SAFE_SPECIES.contains(token))return token;
        if(!token.matches("[a-z0-9]{2,40}"))return "UNKNOWN";
        try {
            return com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByName(token)!=null
                    ?token:"UNKNOWN";
        }catch(RuntimeException | LinkageError unavailable) {
            return "UNKNOWN";
        }
    }

    private static final java.util.Set<String> SAFE_STAT=java.util.Set.of(
            "atk","def","spa","spd","spe","accuracy","evasion");
    private static final java.util.Set<String> SAFE_EFFECT=java.util.Set.of(
            "stealthrock","spikes","toxicspikes","stickyweb","reflect",
            "lightscreen","auroraveil","tailwind","safeguard","mist",
            "trickroom","gravity","wonderroom","magicroom",
            "electricterrain","grassyterrain","psychicterrain",
            "mistyterrain","raindance","sunnyday","sandstorm","hail",
            "snow","none","substitute","encore","taunt","disable",
            "yawn","confusion","perishsong","leechseed","ingrain",
            "protect","dynamax","gmax","torment","focusenergy",
            "attract","curse","smackdown","telekinesis","stockpile",
            "charge","flashfire","trapped","magnetrise","uproar");
    static String publicEffect(String raw) {
        if(raw==null)return "UNKNOWN";
        // Showdown public effect IDs sometimes have a move or ability prefix.
        String base=raw.replaceFirst("(?i)^(move|ability|item):\\s*","");
        return fromSet(base,SAFE_EFFECT);
    }
    private static String publicStage(String raw) {
        if(raw!=null && raw.matches("[0-6]"))return raw;
        return "UNKNOWN";
    }

    private static String slot(String s) {
        if(s==null || s.length()<3)return "UNKNOWN";
        String id=s.substring(0,3);
        return id.matches("p[12][a-f]") && (s.length()==3 || s.charAt(3)==':')
                ?id:"UNKNOWN";
    }
    private static String segment(String[] parts,int idx) {
        return parts.length>idx?parts[idx]:"";
    }
    private static String projectedEvent(String event) {
        String[] p=event.split("\\|",-1);
        if(p.length<3 || !p[0].isEmpty())return "|UNKNOWN";
        String kind=p[1];
        if(kind.equals("turn")) {
            String turn=segment(p,2);
            return "|turn|"+(turn.matches("[0-9]{1,4}")?turn:"UNKNOWN");
        }
        if(kind.equals("-sidestart") || kind.equals("-sideend")
                || kind.equals("-swapsideconditions")) {
            String side=segment(p,2);
            String index=side.startsWith("p1")?"p1":side.startsWith("p2")?"p2":"UNKNOWN";
            String effect=publicEffect(segment(p,3));
            return "|"+kind+"|"+index+"|"+effect;
        }
        if(kind.equals("-weather") || kind.equals("-fieldstart")
                || kind.equals("-fieldend")) {
            return "|"+kind+"|UNKNOWN|"+publicEffect(segment(p,2));
        }
        String actor=slot(segment(p,2));
        String prefix="|"+kind+"|"+actor;
        if(kind.equals("move")) {
            String move=slug(segment(p,3));
            String safe=DeveloperBattleDecisionCapture.safeAction("move:"+move);
            return prefix+"|"+(safe.equals("UNKNOWN")?"UNKNOWN":move)
                    +"|"+slot(segment(p,4));
        }
        if(kind.equals("switch") || kind.equals("drag") || kind.equals("replace")
                || kind.equals("detailschange") || kind.equals("-formechange")) {
            return prefix+"|"+publicSpecies(segment(p,3).split(",",-1)[0]);
        }
        if(kind.equals("-transform"))return prefix+"|"+slot(segment(p,3));
        if(kind.equals("-mega"))return prefix+"|"+publicSpecies(segment(p,3));
        if(kind.equals("-boost") || kind.equals("-unboost")
                || kind.equals("-setboost"))
            return prefix+"|"+fromSet(segment(p,3),SAFE_STAT)
                    +"|"+publicStage(segment(p,4));
        if(kind.equals("-start") || kind.equals("-end")
                || kind.equals("-activate") || kind.equals("-singleturn")
                || kind.equals("-singlemove"))
            return prefix+"|"+publicEffect(segment(p,3));
        if(kind.equals("-terastallize"))return prefix+"|"+fromSet(segment(p,3),SAFE_TYPE);
        if(kind.equals("-status") || kind.equals("-curestatus"))
            return prefix+"|"+fromSet(segment(p,3),SAFE_STATUS);
        if(kind.equals("-item") || kind.equals("-enditem"))
            return prefix+"|"+fromSet(segment(p,3),SAFE_ITEM);
        if(kind.equals("-ability") || kind.equals("-endability"))
            return prefix+"|"+fromSet(segment(p,3),SAFE_ABILITY);
        // Other public kinds retain type and actor only. Never include raw
        // health/HP, nicknames, text or unvalidated extra arguments.
        return prefix;
    }

    /** Compatibility with existing replay users. No tactical side effects. */
    public List<String> observe(UUID battleId, List<String> rawHistory) {
        return observeWithProvenance(battleId, rawHistory).replay();
    }

    public Observation observeWithProvenance(UUID battleId, List<String> rawHistory) {
        if (battleId == null || rawHistory == null) return new Observation(List.of(), List.of());
        if (!battleId.equals(battle)) begin(battleId);
        if (closed || incomplete) return new Observation(List.of(), List.of());

        // Original spectator-compatible projection remains unchanged for
        // tactical observers and long-standing regression contracts.
        List<String> filtered = PublicProtocolFilter.publicEvents(rawHistory);
        PublicEnvelopeProvenance.Audit source =
                PublicEnvelopeProvenance.reconcile(rawHistory, filtered);
        if (!source.complete()) return rejected(source.reason());

        int shared = 0;
        while (shared < previous.size() && shared < filtered.size()
                && previous.get(shared).equals(filtered.get(shared))) shared++;
        if (shared < previous.size()) return rejected("NON_APPEND_ONLY_HISTORY");

        List<String> replay = new ArrayList<>();
        List<String> envelopes = new ArrayList<>();
        for (int i = shared; i < filtered.size(); i++) {
            long seq = ++sequence;
            replay.add("[MaestroAI] SHADOW_REPLAY battle=REDACTED capture=" + captureNumber
                    + " seq=" + seq + " event=" + projectedEvent(filtered.get(i)));
            PublicEnvelopeProvenance.Origin origin = source.origins().get(i);
            envelopes.add("[MaestroAI] SHADOW_ENVELOPE capture=" + captureNumber
                    + " seq=" + seq + " route=" + origin.route()
                    + " split_target=" + origin.splitTarget()
                    + " spectator=EXPECTED_NOT_OBSERVED"
                    + " branch_attested=" + (origin.route() !=
                         PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN));
        }
        previous = List.copyOf(filtered);
        return new Observation(replay, envelopes);
    }

    /**
     * Reporter-only native public projection; deliberately separate from the
     * historic read-only tactical public observer above. Uses the same opaque
     * capture ID but disjoint incremental replay sequence/custody.
     */
    public Observation observeForReportsWithProvenance(UUID battleId,
                                                        List<String> rawHistory) {
        if(battleId==null || rawHistory==null)return new Observation(List.of(),List.of());
        if(!battleId.equals(battle))begin(battleId);
        if(reportClosed || reportIncomplete)return new Observation(List.of(),List.of());

        List<String> filtered=PublicProtocolFilter.reportPublicEvents(rawHistory);
        var source=PublicEnvelopeProvenance.reconcileForReports(rawHistory,filtered);
        if(!source.complete())return reportRejected(source.reason());

        int shared=0;
        while(shared<reportPrevious.size() && shared<filtered.size()
                && reportPrevious.get(shared).equals(filtered.get(shared)))shared++;
        if(shared<reportPrevious.size())return reportRejected("NON_APPEND_ONLY_HISTORY");
        List<String> replay=new ArrayList<>(), envelopes=new ArrayList<>();
        for(int i=shared;i<filtered.size();i++){
            long seq=++reportSequence;
            replay.add("[MaestroAI] SHADOW_REPLAY battle=REDACTED capture="
                    +captureNumber+" seq="+seq+" event="+projectedEvent(filtered.get(i)));
            PublicEnvelopeProvenance.Origin origin=source.origins().get(i);
            envelopes.add("[MaestroAI] SHADOW_ENVELOPE capture="+captureNumber
                    +" seq="+seq+" route="+origin.route()
                    +" split_target="+origin.splitTarget()
                    +" spectator=EXPECTED_NOT_OBSERVED"
                    +" branch_attested="+(origin.route()
                        !=PublicEnvelopeProvenance.Route.UNFRAMED_UNKNOWN));
        }
        reportPrevious=List.copyOf(filtered);
        return new Observation(replay,envelopes);
    }

    private Observation reportRejected(String reason){
        if(reportIncomplete)return new Observation(List.of(),List.of());
        reportIncomplete=true;
        return new Observation(List.of("[MaestroAI] SHADOW_REPLAY_GAP battle=REDACTED"
                +" capture="+captureNumber+" after="+reportSequence),
                List.of("[MaestroAI] SHADOW_ENVELOPE_FAILURE capture="+captureNumber
                        +" reason="+reason+" status=UNKNOWN"));
    }

    /**
     * A separate real Cobblemon terminal for the native report-only stream;
     * unlike onVictory below, it never changes the tactical observer.
     */
    public List<String> onVictoryForReports(UUID battleId,List<String> history) {
        if(battleId==null || !battleId.equals(battle) || reportClosed)return List.of();
        Observation flushed=observeForReportsWithProvenance(battleId,history);
        List<String> output=new ArrayList<>();
        output.addAll(flushed.replay());
        output.addAll(flushed.envelopes());
        reportClosed=true;
        output.add("[MaestroAI] SHADOW_TERMINAL capture="+captureNumber
                +" after="+reportSequence
                +" event=COBBLEMON_BATTLE_VICTORY winner=REDACTED"
                +" beginning=UNKNOWN delivery=UNVERIFIED coverage="
                +(reportIncomplete?"INCOMPLETE":"START_UNKNOWN"));
        return List.copyOf(output);
    }

    /**
     * Only a real Cobblemon BATTLE_VICTORY callback calls this. Does not
     * derive winner identity, a win rate, or replay completeness.
     */
    public List<String> onVictory(UUID battleId, List<String> history) {
        if (battleId == null || !battleId.equals(battle) || closed) return List.of();
        Observation flushed = observeWithProvenance(battleId, history);
        List<String> output = new ArrayList<>();
        output.addAll(flushed.replay());
        output.addAll(flushed.envelopes());
        closed = true;
        output.add("[MaestroAI] SHADOW_TERMINAL capture=" + captureNumber
                + " after=" + sequence
                + " event=COBBLEMON_BATTLE_VICTORY"
                + " winner=REDACTED"
                + " beginning=UNKNOWN"
                + " delivery=UNVERIFIED"
                + " coverage=" + (incomplete ? "INCOMPLETE" : "START_UNKNOWN"));
        return List.copyOf(output);
    }
}
