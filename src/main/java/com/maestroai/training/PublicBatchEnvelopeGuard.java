package com.maestroai.training;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Phase 6.15: complete, bounded DIRECT update-batch envelope parser.
 *
 * No split route, sideupdate, private line, unframed payload or unknown
 * annotation can be silently skipped. Source-pinned Showdown |split| sends
 * private first, public second; an extracted public half is NOT a trusted
 * native delivery witness and is not admitted here.
 */
final class PublicBatchEnvelopeGuard {
    static final String SCHEMA="phase6.15.direct-update-batches.v1";
    static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final Pattern ACTOR=Pattern.compile("p[12][a-c]: [^|\\r\\n]{1,80}");
    private static final int MAX_EVENTS=512,MAX_PER_CHUNK=32;
    private static final Set<String> IMPACT=Set.of("-crit","-supereffective","-resisted","-immune");
    private static final Set<String> FAILURE=Set.of("-miss","-fail","-notarget");
    private static final Set<String> ZERO=Set.of("upkeep","-nothing");
    record Result(List<String> mechanicFrames,List<String> directMechanics,
                  int allEvents,boolean narrowOnly) {
        Result { mechanicFrames=List.copyOf(mechanicFrames);
                 directMechanics=List.copyOf(directMechanics); }
    }
    private static IllegalArgumentException invalid(){
        return new IllegalArgumentException("Phase 6.15 incomplete public envelope");
    }
    private static String kind(String line){
        if(line==null || line.length()>512 || line.indexOf('\r')>=0
                || !line.startsWith("|"))throw invalid();
        int end=line.indexOf('|',1);
        if(end<0)return Set.of("upkeep","-nothing").contains(line.substring(1))
                ? line.substring(1) : failKind();
        if(end==1)throw invalid();
        return line.substring(1,end);
    }
    private static String failKind(){throw invalid();}
    private static boolean actor(String s){
        return s!=null&&ACTOR.matcher(s).matches();
    }
    private static boolean known(Map<String,String> observed,String id){
        return actor(id) && id.equals(observed.get(id.substring(0,3)));
    }
    private static void annotation(String[] fields,String kind,
                                   boolean hasMoveThisTurn,boolean hasTurn,
                                   Map<String,String> observed){
        // These are known source annotations, not state transitions.
        // Their presence does NOT establish accuracy of an opponent action.
        if(kind.equals("upkeep")) {
            if(fields.length!=2 || !hasTurn)throw invalid();
        } else if(kind.equals("-nothing")) {
            if(fields.length!=2 || !hasMoveThisTurn)throw invalid();
        } else if(IMPACT.contains(kind) || kind.equals("-notarget")) {
            if(fields.length!=3 || !hasMoveThisTurn || !known(observed,fields[2]))
                throw invalid();
        } else if(kind.equals("-miss")) {
            if(fields.length!=4 || !hasMoveThisTurn
                    || !known(observed,fields[2]) || !known(observed,fields[3]))throw invalid();
        } else if(kind.equals("-fail")) {
            if(!hasMoveThisTurn || fields.length<3 || fields.length>4
                    || !known(observed,fields[2])
                    || fields.length==4 && !fields[3].matches("[a-zA-Z0-9 :()\\-]{1,80}"))
                throw invalid();
        } else throw invalid();
    }

    static Result inspect(List<String> raw) {
        if(raw==null || raw.isEmpty() || raw.size()>128)throw invalid();
        List<String> mechanical=new ArrayList<>(),events=new ArrayList<>();
        boolean narrow=true,hasTurn=false,moveThisTurn=false;
        Map<String,String> observed=new HashMap<>();
        for(String chunk:raw){
            if(chunk==null || chunk.length()>16384 || !chunk.startsWith("update\n")
                    || chunk.indexOf('\r')>=0)throw invalid();
            String[] lines=chunk.split("\n",-1);
            if(lines.length<2 || lines.length>MAX_PER_CHUNK+1
                    || !lines[0].equals("update"))throw invalid();
            if(lines.length!=2)narrow=false;
            for(int i=1;i<lines.length;i++){
                String line=lines[i];
                String type=kind(line);
                String[] fields=line.split("\\|",-1);
                if(fields.length<2 || !fields[0].isEmpty() || type.equals("split")
                        || type.equals("request") || type.equals("pp_update"))
                    throw invalid();
                if(IMPACT.contains(type) || FAILURE.contains(type)
                        || ZERO.contains(type)){
                    annotation(fields,type,moveThisTurn,hasTurn,observed);
                    narrow=false;
                } else {
                    // Frame-local annotation actor bounds must match the
                    // observed display actor; the full mechanics validator
                    // independently verifies status/HP/appearance semantics.
                    if(Set.of("switch","drag","replace").contains(type)) {
                        if(fields.length<3 || !actor(fields[2]))throw invalid();
                        observed.put(fields[2].substring(0,3),fields[2]);
                    }
                    // ExpandedPublicEventGuard subsequently verifies EXACT
                    // appearance, HP/status, turn and move continuity.
                    if(type.equals("turn")){
                        hasTurn=true;moveThisTurn=false;
                    }else if(type.equals("move")){
                        if(!hasTurn)throw invalid();
                        moveThisTurn=true;
                    }
                    mechanical.add(line);
                    if(!Set.of("turn","move","switch","drag").contains(type))
                        narrow=false;
                }
                events.add(line);
                if(events.size()>MAX_EVENTS)throw invalid();
            }
        }
        if(mechanical.isEmpty() || !hasTurn)throw invalid();
        List<String> frames=mechanical.stream().map(s->"update\n"+s).toList();
        return new Result(frames,mechanical,events.size(),narrow);
    }
}
