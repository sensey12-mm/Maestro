package com.maestroai.training;

import java.util.*;
import java.util.regex.*;

/**
 * Phase 6.14: strict, source-pinned DIRECT-public Showdown event validator.
 *
 * This is a framing/continuity guard, never a Pokemon identity proof,
 * a simulator, a complete public-state projection or an action validator.
 * Private sideupdate and split routing are NOT eligible.
 */
final class ExpandedPublicEventGuard {
    static final String SCHEMA="phase6.14.direct-public-events.v1";
    static final String SOURCE=ReadOnlyShadowObservationBridge.COBBLEMON_VERSION;
    private static final Pattern ACTOR=Pattern.compile("p[12][a-c]: [^|\\r\\n]{1,80}");
    private static final Pattern HP=Pattern.compile("([0-9]{1,5})/([1-9][0-9]{0,5})(?: (brn|par|psn|tox|slp|frz|fnt))?");
    private static final Set<String> STATUS=Set.of("brn","par","psn","tox","slp","frz");
    private static final Set<String> TYPES=Set.of("turn","move","switch","drag",
            "-damage","-heal","-status","-curestatus","faint",
            "replace","detailschange","-formechange","-transform");
    record Result(int turn,int events,boolean appearanceAmbiguous,boolean narrowOnly){}
    private static final class Appearance {
        String actor,status;
        int hp,max;
        boolean fainted;
        Appearance(String actor,int hp,int max,String status) {
            this.actor=actor;this.hp=hp;this.max=max;this.status=status;
            fainted=hp==0;
        }
    }
    private static IllegalArgumentException invalid(){
        // No nicknames, private fields, or offending source text in errors.
        return new IllegalArgumentException("Phase 6.14 unsafe public protocol");
    }
    private static boolean actor(String s){return s!=null&&ACTOR.matcher(s).matches();}
    private static String slot(String s){return s.substring(0,3);}
    private static boolean tags(String[] f,int from) {
        for(int i=from;i<f.length;i++){
            String t=f[i];
            if(t.length()>128 || !(t.equals("[silent]") || t.equals("[still]")
                    || t.startsWith("[from] ") || t.startsWith("[of] ")))
                return false;
        }
        return true;
    }
    private static int[] health(String raw) {
        var m=HP.matcher(raw);
        if(!m.matches())throw invalid();
        int n,den;
        try {n=Integer.parseInt(m.group(1));den=Integer.parseInt(m.group(2));}
        catch(NumberFormatException e){throw invalid();}
        if(den>100000 || n>den || "fnt".equals(m.group(3))!=(n==0))
            throw invalid();
        return new int[]{n,den};
    }
    private static String status(String raw) {
        var m=HP.matcher(raw);
        if(!m.matches())throw invalid();
        String s=m.group(3);
        return s==null || "fnt".equals(s)?"none":s;
    }
    private static Appearance active(Map<String,Appearance> current,String id) {
        if(!actor(id))throw invalid();
        Appearance p=current.get(slot(id));
        if(p==null || !p.actor.equals(id))throw invalid();
        return p;
    }
    private static String text(String t) {
        if(t==null || t.isBlank() || t.length()>100 || t.indexOf('\n')>=0
                || t.indexOf('\r')>=0 || t.indexOf('|')>=0)throw invalid();
        return t;
    }

    static Result validate(List<String> raw) {
        if(raw==null || raw.isEmpty() || raw.size()>128)throw invalid();
        var current=new HashMap<String,Appearance>();
        int turn=0;
        boolean ambiguous=false,narrow=true;
        for(String chunk:raw) {
            // Preserve public routing as a *single direct update* instruction.
            // Never strip a private half, treat a partial split as an event,
            // or discard a mystery payload while issuing a "complete" receipt.
            if(chunk==null || chunk.length()>1024 || !chunk.startsWith("update\n")
                    || chunk.indexOf('\n',7)>=0 || chunk.indexOf('\r')>=0)
                throw invalid();
            String line=chunk.substring(7);
            if(!line.startsWith("|"))throw invalid();
            String[] f=line.split("\\|",-1);
            if(f.length<3 || !f[0].isEmpty() || !TYPES.contains(f[1]))
                throw invalid();
            String kind=f[1];
            if(!Set.of("turn","move","switch","drag").contains(kind))
                narrow=false;
            switch(kind) {
                case "turn" -> {
                    if(f.length!=3 || !f[2].matches("[1-9][0-9]{0,4}"))
                        throw invalid();
                    int next=Integer.parseInt(f[2]);
                    if(next!=turn+1 || next>10000)throw invalid();
                    turn=next;
                }
                case "switch","drag" -> {
                    if(f.length!=5 || !actor(f[2]))throw invalid();
                    text(f[3]);
                    int[] hp=health(f[4]);
                    current.put(slot(f[2]),new Appearance(f[2],hp[0],hp[1],status(f[4])));
                }
                case "move" -> {
                    if(f.length<4 || f.length>6 || !tags(f,5))
                        throw invalid();
                    Appearance p=active(current,f[2]);
                    if(p.fainted || turn==0)throw invalid();
                    text(f[3]);
                    if(f.length>4 && !f[4].isBlank() && !actor(f[4]))throw invalid();
                }
                case "-damage","-heal" -> {
                    if(f.length<4 || !tags(f,4) || turn==0)throw invalid();
                    Appearance p=active(current,f[2]);
                    int[] next=health(f[3]);
                    if(p.fainted || next[1]!=p.max
                            || kind.equals("-damage") && next[0]>p.hp
                            || kind.equals("-heal") && next[0]<p.hp)
                        throw invalid();
                    p.hp=next[0];p.fainted=next[0]==0;p.status=status(f[3]);
                }
                case "-status" -> {
                    if(f.length<4 || !tags(f,4) || turn==0
                            || !STATUS.contains(f[3]))throw invalid();
                    Appearance p=active(current,f[2]);
                    if(p.fainted || !p.status.equals("none"))throw invalid();
                    p.status=f[3];
                }
                case "-curestatus" -> {
                    if(f.length<4 || !tags(f,4) || turn==0
                            || !STATUS.contains(f[3]))throw invalid();
                    Appearance p=active(current,f[2]);
                    if(p.fainted || !p.status.equals(f[3]))throw invalid();
                    p.status="none";
                }
                case "faint" -> {
                    if(f.length!=3 || turn==0)throw invalid();
                    Appearance p=active(current,f[2]);
                    if(!p.fainted)throw invalid();
                }
                case "replace" -> {
                    // Illusion end: public appearance identity is NOT a party UUID.
                    // Retain the frame, but block all cross-stream readiness
                    // in this battle rather than relabel old observations.
                    if(f.length<4 || f.length>5 || turn==0 || !actor(f[2]))
                        throw invalid();
                    text(f[3]);
                    if(f.length==5)health(f[4]);
                    Appearance p=current.get(slot(f[2]));
                    if(p==null || p.fainted)throw invalid();
                    p.actor=f[2];
                    ambiguous=true;
                }
                case "detailschange","-formechange" -> {
                    if(f.length<4 || f.length>5 || turn==0)throw invalid();
                    Appearance p=active(current,f[2]);
                    if(p.fainted)throw invalid();
                    text(f[3]);
                    if(f.length==5)health(f[4]);
                    ambiguous=true;
                }
                case "-transform" -> {
                    if(f.length<4 || !tags(f,4) || turn==0)throw invalid();
                    Appearance p=active(current,f[2]);
                    if(p.fainted || !actor(f[3]))throw invalid();
                    ambiguous=true;
                }
                default -> throw invalid();
            }
        }
        if(turn<1)throw invalid();
        return new Result(turn,raw.size(),ambiguous,narrow);
    }
}
