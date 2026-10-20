package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Offline, explicitly permitted Showdown spectator-log ingestion.
 * No network fetching, hidden data, trained probabilities or live action control.
 */
public final class PublicReplayCorpus {
    private PublicReplayCorpus() {}
    private static final Map<String,String> FORMATS=Map.of(
            "gen9ou","[Gen 9] OU","gen9ubers","[Gen 9] Ubers");
    // Non-battle spectator metadata is ignored, not reused as a feature.
    // Everything else must be an explicitly supported public protocol kind.
    private static final Set<String> METADATA_KINDS=Set.of(
            "gen","gametype","tier","player","teamsize","rule","rated",
            "start","turn","win","tie","upkeep","t:","inactive","inactiveoff",
            "chat","c","c:","j","l","-message","-hint","-anim",
            "-nothing","-crit","-supereffective","-resisted",
            "-immune","-miss","-fail","-notarget");
    public enum RightsBasis { EXPLICIT_REPLAY_USE_PERMISSION }
    /** Operator attestations require independent verification; a boolean cannot prove rights. */
    public record Source(String replayId,String format,boolean publiclyListed,RightsBasis rights) {
        public Source {
            if (format==null || !FORMATS.containsKey(format) || replayId==null
                    || !replayId.matches("gen9(?:ou|ubers)-[1-9][0-9]{3,17}")
                    || !replayId.startsWith(format+"-") || !publiclyListed
                    || rights!=RightsBasis.EXPLICIT_REPLAY_USE_PERMISSION) throw rejected();
        }
    }
    /** One appearance only; an identical nickname or species NEVER proves re-entry. */
    public record Appearance(int index,String slot,String displayedSpecies,Set<String> nativeMoves,
                             String revealedItem,String revealedAbility,boolean transformed,
                             boolean identityLinked,boolean identityUncertain) {
        public Appearance { nativeMoves=Set.copyOf(nativeMoves); }
    }
    /** De-identified, no player/nickname/chat/raw events. Never a mechanics certificate. */
    public record Evidence(String replayId,String format,String inputSha256,int publicEventCount,
                           int turns,List<Appearance> appearances,String compatibility) {
        public Evidence { appearances=List.copyOf(appearances); }
    }
    private static final class Display {
        final String slot;
        String species;
        Display(String slot,String species) { this.slot=slot; this.species=species; }
    }
    public static Evidence ingest(Source source,String log) {
        if (source==null || log==null || log.isEmpty() || log.length()>2_000_000
                || log.indexOf('\0')>=0) throw rejected();
        List<String> lines=log.lines().toList();
        if (lines.isEmpty() || lines.size()>50_000) throw rejected();
        int gen=0,game=0,tier=0,start=0,end=0,turns=0;
        boolean started=false, finished=false;
        for (String line:lines) {
            if (line.length()>8192 || line.startsWith("sideupdate") || line.startsWith("|split")
                    || line.startsWith("|request|") || line.startsWith("|pp_update|")
                    || line.startsWith("|showteam|") || line.startsWith("|inputlog|")
                    || line.startsWith("|queryresponse|") || line.startsWith("|teams|")
                    || line.startsWith("|seed|")) throw rejected();
            if (line.isEmpty() || line.equals("update")) continue;
            if (!line.startsWith("|") || line.length()<2) throw rejected();
            int separator=line.indexOf("|",1);
            String kind=separator<0 ? line.substring(1) : line.substring(1,separator);
            if (!METADATA_KINDS.contains(kind)
                    && !PublicProtocolFilter.publicEvents(List.of(line)).contains(line))
                throw rejected();
            // A public log must be chronological, and cannot contain
            // additional observations after its terminal result.
            if (finished) throw rejected();
            if (line.equals("|start")) {
                if (started) throw rejected();
                started=true;
            }
            if (line.startsWith("|win|") || line.equals("|tie")) {
                if (!started || turns==0) throw rejected();
                finished=true;
            }
            if (Set.of("move","switch","drag","replace","detailschange","-transform",
                    "-formechange","-mega","-terastallize").contains(kind) && !started)
                throw rejected();
            if (line.equals("|gen|9")) gen++;
            else if (line.startsWith("|gen|")) throw rejected();
            if (line.equals("|gametype|singles")) game++;
            else if (line.startsWith("|gametype|")) throw rejected();
            if (line.equals("|tier|"+FORMATS.get(source.format()))) tier++;
            else if (line.startsWith("|tier|")) throw rejected();
            if (line.equals("|start")) start++;
            if (line.startsWith("|win|") || line.equals("|tie")) end++;
            if (line.startsWith("|turn|")) {
                if (!started || !line.matches("\\|turn\\|[1-9][0-9]{0,5}"))
                    throw rejected();
                int next;
                try { next=Integer.parseInt(line.substring("|turn|".length())); }
                catch (NumberFormatException malformed) { throw rejected(); }
                if (next!=turns+1) throw rejected();
                turns++;
            }
        }
        if (gen!=1 || game!=1 || tier!=1 || start!=1 || end!=1 || turns<1) throw rejected();

        // All spectator observations flow through the existing allowlist/observer.
        // Split/private envelopes are rejected above rather than reconstructed.
        List<String> events=PublicProtocolFilter.publicEvents(List.of(log));
        if (events.isEmpty()) throw rejected();
        var observer=new PublicBattleObserver();
        var displays=new ArrayList<Display>();
        var active=new HashMap<String,Display>();
        for (String line:events) {
            String[] parts=line.split("\\|",-1);
            String kind=parts.length>1?parts[1]:"";
            String slot=parts.length>2?slot(parts[2]):null;
            observer.accept(line);
            if (("switch".equals(kind) || "drag".equals(kind)) && slot!=null) {
                var display=new Display(slot,safeSpecies(observer.publicSpecies(slot)));
                displays.add(display); active.put(slot,display);
            } else if (Set.of("replace","detailschange","-formechange","-mega",
                    "-terastallize").contains(kind) && slot!=null && active.containsKey(slot)) {
                // A changed form is displayed only; its typing/underlying identity remain unknown.
                active.get(slot).species=safeSpecies(observer.publicSpecies(slot));
            }
        }
        var threats=observer.opponentIntelligence().observedAppearances();
        if (threats.size()!=displays.size()) throw rejected();
        var appearances=new ArrayList<Appearance>();
        for (int i=0;i<threats.size();i++) {
            var t=threats.get(i);
            var d=displays.get(i);
            var moves=new HashSet<String>();
            for (String move:t.moves()) if (fact(move)!=null) moves.add(move);
            appearances.add(new Appearance(i+1,d.slot,d.species,moves,
                    t.transformed()?null:fact(t.item()),
                    t.transformed()?null:fact(t.ability()),
                    t.transformed(),false,true));
        }
        return new Evidence(source.replayId(),source.format(),sha256(log),events.size(),turns,
                appearances,"SHOWDOWN_PUBLIC_OBSERVATIONS;COBBLEMON_TRANSFER_UNVERIFIED");
    }
    private static String slot(String actor) {
        if (actor==null) return null;
        int colon=actor.indexOf(':');
        if (colon<0) return null;
        String value=actor.substring(0,colon).trim();
        return value.matches("p[12]a")?value:null;
    }
    private static String safeSpecies(String value) {
        return fact(value)==null?"unknown":value;
    }
    private static String fact(String value) {
        return value!=null && value.matches("[a-z0-9]{1,40}")?value:null;
    }
    private static String sha256(String raw) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
    private static IllegalArgumentException rejected() {
        // Rejection never echoes sensitive supplied bytes.
        return new IllegalArgumentException("Replay rejected: unverified rights, format or spectator boundary");
    }
}
