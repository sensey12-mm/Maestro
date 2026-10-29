package com.maestroai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Phase 6.16: source-pinned, observational split-routing custody checks.
 *
 * Showdown's |split|p1/p2 carries PRIVATE first, PUBLIC second. Cobblemon
 * ShowdownInterpreter consumes the same ordered pair. Source framing can
 * establish structural separation ONLY, not delivery/authenticity.
 *
 * Never hands public halves to Phase 6.9/6.13, never stores original chunks,
 * never records raw private text, names, actor UUIDs, or original public text.
 */
public final class PublicSplitRoutingGate {
    public static final String SCHEMA="phase6.16.split-routing-custody.v1";
    public static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final int MAX_CHUNKS=128, MAX_CHARS=65536, MAX_LINES=64;
    private static final Set<String> SPLIT_TYPES=
            Set.of("-damage","-heal","-sethp","switch","drag");
    private static final Set<String> DIRECT_TYPES=
            Set.of("turn","move","switch","drag","replace","detailschange",
                    "-formechange","-transform","-damage","-heal","-status",
                    "-ability","-boost",
                    "-curestatus","faint");
    private static final Pattern ACTOR=Pattern.compile("p[12][a-c]: [^|\\r\\n]{1,80}");
    private static final Pattern HP=Pattern.compile(
            "(?:[0-9]{1,5}/[1-9][0-9]{0,5}|[0-9]{1,3}%)(?: (?:brn|par|psn|tox|slp|frz|fnt))?");

    public enum Verdict {
        STRUCTURAL_PUBLIC_PRIVATE_SEPARATION,
        DIRECT_ONLY,
        BAD_SOURCE_PIN,
        MISSING_OR_MALFORMED,
        CROSS_BATTLE,
        HISTORY_REWRITTEN,
        WITHDRAWN
    }

    /** No raw event/actor fields; status is NEVER a delivery certificate. */
    public static final class Report {
        private final Session session;
        private final Verdict verdict;
        private final long revision;
        private final int splitPairs,directEvents,chunkCount,publicTurn;
        private final List<Integer> splitChunkOrdinals;
        private final List<String> splitTargets;
        private Report(Session session,Verdict verdict,long revision,
                       int pairs,int direct,int chunks,int turn,List<String> targets,
                       List<Integer> ordinalChunks) {
            this.session=session;this.verdict=verdict;
            this.revision=revision;splitPairs=pairs;
            directEvents=direct;chunkCount=chunks;publicTurn=turn;
            splitTargets=List.copyOf(targets);
            splitChunkOrdinals=List.copyOf(ordinalChunks);
        }
        /** Internal routing targets only; no private/public original text. */
        String targetAt(int zeroBased){return splitTargets.get(zeroBased);}
        boolean sameBattle(Object battle){
            return current() && session.boundBattle==battle && battle!=null;
        }
        public String schema(){return SCHEMA;}
        public String source(){return SOURCE;}
        public Verdict verdict(){return verdict;}
        public long revision(){return revision;}
        public int splitPairs(){return splitPairs;}
        public int directEvents(){return directEvents;}
        public int chunkCount(){return chunkCount;}
        public int publicTurn(){return publicTurn;}
        /** Ordinals refer to original 1-based history CHUNKS only. */
        int splitChunkOrdinal(int pairIndex){return splitChunkOrdinals.get(pairIndex);}
        public boolean current(){
            synchronized(session){
                return !session.withdrawn && session.revision==revision && revision>0
                        && (verdict==Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION
                                || verdict==Verdict.DIRECT_ONLY);
            }
        }
        public boolean splitPublicExtractionStructurallyMatched(){
            return current() && verdict==Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION;
        }
        public boolean liveSourceDeliveryAuthenticated(){return false;}
        public boolean runtimeActorAuthenticated(){return false;}
        public boolean publicReadinessLeaseAuthorized(){return false;}
        public boolean privatePayloadExportable(){return false;}
        public boolean completeMechanicsKnown(){return false;}
        public boolean shadowSearchAuthorized(){return false;}
        public boolean tacticalDecisionAuthorized(){return false;}
        public boolean livePromotionAuthorized(){return false;}
        @Override public String toString() {
            return "Phase616SplitRouting[verdict="+verdict+";revision="+revision
                    +";splitPairs="+splitPairs+";directEvents="+directEvents
                    +";chunks="+chunkCount+";authority=NONE]";
        }
    }

    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.16 split provenance unavailable");
    }
    private static String kind(String line){
        if(line==null || !line.startsWith("|") || line.length()>1024)
            throw invalid();
        int boundary=line.indexOf('|',1);
        return boundary<0?line.substring(1):line.substring(1,boundary);
    }
    private static String actorField(String[] fields){
        if(fields.length<3 || !ACTOR.matcher(fields[2]).matches())throw invalid();
        return fields[2];
    }
    private static String[] fields(String line){
        if(line==null || line.isBlank() || line.indexOf('\r')>=0
                || line.indexOf('\n')>=0 || line.length()>1024)
            throw invalid();
        String[] value=line.split("\\|",-1);
        if(value.length<3 || !value[0].isEmpty())throw invalid();
        return value;
    }
    private static void validateSplitPair(String target,String privateLine,String publicLine){
        String a=kind(privateLine),b=kind(publicLine);
        if(!a.equals(b) || !SPLIT_TYPES.contains(a))throw invalid();
        String[] hidden=fields(privateLine),shown=fields(publicLine);
        // A split's target side and active slot must be identical in both
        // branches; nicknames and active identity must not silently change.
        String left=actorField(hidden),right=actorField(shown);
        if(!left.equals(right) || !left.startsWith(target))throw invalid();
        if(a.equals("switch") || a.equals("drag")){
            if(hidden.length!=5 || shown.length!=5
                    || hidden[3].isBlank() || shown[3].isBlank()
                    || hidden[3].length()>100 || shown[3].length()>100
                    || !HP.matcher(hidden[4]).matches()
                    || !HP.matcher(shown[4]).matches())throw invalid();
        } else {
            // Supported subset: plain HP-bearing split with a small number
            // of source annotations; no arbitrary hidden nested payloads.
            if(hidden.length<4 || hidden.length>7
                    || shown.length<4 || shown.length>7
                    || !HP.matcher(hidden[3]).matches()
                    || !HP.matcher(shown[3]).matches())throw invalid();
            for(int i=4;i<hidden.length;i++)if(!tag(hidden[i]))throw invalid();
            for(int i=4;i<shown.length;i++)if(!tag(shown[i]))throw invalid();
        }
    }
    private static boolean tag(String value){
        return value!=null && value.length()<=128
                && (value.equals("[silent]") || value.equals("[still]")
                    || value.matches("\\[(?:from|of)\\] [a-zA-Z0-9 :()'\\-]{1,96}"));
    }

    private record Parsed(int pairs,int direct,List<String> publicEvents,
                          List<PublicEnvelopeProvenance.Route> routes,
                          List<String> splitTargets,List<Integer> splitChunkOrdinals,
                          int turn) {}

    private static Parsed parse(List<String> source){
        List<String> publicEvents=new ArrayList<>();
        List<PublicEnvelopeProvenance.Route> routes=new ArrayList<>();
        List<String> splitTargets=new ArrayList<>();
        List<Integer> splitChunks=new ArrayList<>();
        int splits=0,direct=0,turn=0,chunkOrdinal=0;
        for(String chunk:source){
            chunkOrdinal++;
            if(chunk==null || chunk.length()>16384 || !chunk.startsWith("update\n")
                    || chunk.indexOf('\r')>=0)throw invalid();
            String[] lines=chunk.split("\n",-1);
            if(lines.length<2 || lines.length>MAX_LINES+1
                    || !lines[0].equals("update"))throw invalid();
            for(int i=1;i<lines.length;i++){
                String line=lines[i],type=kind(line);
                if(type.equals("split")){
                    if(!line.equals("|split|p1") && !line.equals("|split|p2"))
                        throw invalid();
                    if(i+2>=lines.length)throw invalid();
                    String hidden=lines[++i],publicLine=lines[++i];
                    validateSplitPair(line.substring(7),hidden,publicLine);
                    // The private first half is compared IN MEMORY only,
                    // never retained by the Session or its Report.
                    publicEvents.add(publicLine);
                    routes.add(PublicEnvelopeProvenance.Route.SPLIT_PUBLIC);
                    splitTargets.add(line.substring(7));
                    splitChunks.add(chunkOrdinal);
                    splits++;
                } else {
                    if(!DIRECT_TYPES.contains(type))throw invalid();
                    String[] publicFields=fields(line);
                    // Only verified common public events are added in 6.23.
                    // A wider public filter is NOT evidence of source ownership.
                    // All parsed source is compared with the independent filter.
                    if(type.equals("-ability")){
                        if(publicFields.length<4 || publicFields.length>6
                                || !ACTOR.matcher(publicFields[2]).matches()
                                || !publicFields[3].matches("[a-zA-Z0-9][a-zA-Z0-9 ():']{0,95}"))
                            throw invalid();
                        for(int field=4;field<publicFields.length;field++)
                            if(!tag(publicFields[field]))throw invalid();
                    }
                    if(type.equals("-boost")){
                        if(publicFields.length<5 || publicFields.length>7
                                || !ACTOR.matcher(publicFields[2]).matches()
                                || !Set.of("atk","def","spa","spd","spe",
                                          "accuracy","evasion").contains(publicFields[3])
                                || !publicFields[4].matches("[1-6]"))
                            throw invalid();
                        for(int field=5;field<publicFields.length;field++)
                            if(!tag(publicFields[field]))throw invalid();
                    }
                    if(type.equals("turn")){
                        if(publicFields.length!=3
                                || !publicFields[2].matches("[1-9][0-9]{0,4}"))throw invalid();
                        int next=Integer.parseInt(publicFields[2]);
                        if(next!=turn+1 || next>10000)throw invalid();
                        turn=next;
                    }
                    publicEvents.add(line);
                    routes.add(PublicEnvelopeProvenance.Route.DIRECT_UPDATE);
                    direct++;
                }
                if(splits+direct>512)throw invalid();
            }
        }
        if(splits+direct==0)throw invalid();
        return new Parsed(splits,direct,publicEvents,routes,splitTargets,splitChunks,turn);
    }

    public static final class Session {
        private final byte[] salt=new byte[32];
        private Object boundBattle;
        private List<String> acceptedDigests=List.of();
        private boolean withdrawn;
        private long revision;
        public Session(){new SecureRandom().nextBytes(salt);}
        public synchronized boolean withdrawn(){return withdrawn;}
        public synchronized void withdraw(){
            withdrawn=true;acceptedDigests=List.of();Arrays.fill(salt,(byte)0);
        }
        private List<String> digest(List<String> raw){
            try {
                MessageDigest d=MessageDigest.getInstance("SHA-256");
                List<String> out=new ArrayList<>();
                for(String chunk:raw){
                    d.reset();d.update(salt);
                    d.update(chunk.getBytes(StandardCharsets.UTF_8));
                    out.add(HexFormat.of().formatHex(d.digest()));
                }
                return List.copyOf(out);
            }catch(NoSuchAlgorithmException absent){
                throw new IllegalStateException("SHA256 unavailable");
            }
        }
        private Report fail(Verdict reason){
            withdraw();
            return new Report(this,reason,revision,0,0,0,0,List.of(),List.of());
        }
        /**
         * Probe only: a compatible producer predicate does not certify the
         * original Cobblemon event delivery. No source text is ever logged.
         */
        public synchronized Report inspect(Object battle,List<String> history,
                                           boolean versionPinned){
            if(withdrawn)return new Report(this,Verdict.WITHDRAWN,revision,0,0,0,0,List.of(),List.of());
            if(!versionPinned)return fail(Verdict.BAD_SOURCE_PIN);
            if(battle==null || boundBattle!=null && battle!=boundBattle)
                return fail(Verdict.CROSS_BATTLE);
            if(history==null || history.isEmpty() || history.size()>MAX_CHUNKS)
                return fail(Verdict.MISSING_OR_MALFORMED);
            try {
                // Defensive snapshot before parsing; original strings leave
                // this stack frame and are NEVER retained by Session.
                List<String> copy=List.copyOf(history);
                if(copy.stream().mapToLong(String::length).sum()>MAX_CHARS)
                    return fail(Verdict.MISSING_OR_MALFORMED);
                List<String> digests=digest(copy);
                if(digests.size()<acceptedDigests.size()
                        || !digests.subList(0,acceptedDigests.size()).equals(acceptedDigests))
                    return fail(Verdict.HISTORY_REWRITTEN);
                Parsed parsed=parse(copy);
                // Independent existing filter and provenance must match
                // exactly, including the route and ORDER of each public event.
                List<String> filtered=PublicProtocolFilter.publicEvents(copy);
                PublicEnvelopeProvenance.Audit independent=
                        PublicEnvelopeProvenance.reconcile(copy,filtered);
                if(!independent.complete()
                        || !parsed.publicEvents.equals(filtered)
                        || independent.origins().size()!=parsed.routes.size())
                    return fail(Verdict.MISSING_OR_MALFORMED);
                for(int i=0;i<parsed.routes.size();i++)
                    if(independent.origins().get(i).route()!=parsed.routes.get(i))
                        return fail(Verdict.MISSING_OR_MALFORMED);
                if(!digests.equals(acceptedDigests)){
                    revision++;
                    acceptedDigests=digests;
                }
                boundBattle=battle;
                return new Report(this,parsed.pairs==0?Verdict.DIRECT_ONLY
                        :Verdict.STRUCTURAL_PUBLIC_PRIVATE_SEPARATION,
                        revision,parsed.pairs,parsed.direct,copy.size(),parsed.turn(),
                        parsed.splitTargets(),parsed.splitChunkOrdinals());
            }catch(RuntimeException unsafe){
                return fail(Verdict.MISSING_OR_MALFORMED);
            }
        }
    }
}
