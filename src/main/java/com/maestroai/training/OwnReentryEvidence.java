package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Ordinary same-form own return after witnessed unfainted exits. No opponent or future-entry proof. */
final class OwnReentryEvidence extends OwnEntryEvidence {
    private OwnReentryEvidence(PublicFieldEvidence.Capture field, OwnRequestTransport.Lease lease,
            RuntimeDomain domain, PublicProtocolEvidence protocol, PublicMechanicsEvidence.Observation observation,Set<Integer> admitted) {
        super(field,lease,domain,protocol,observation,admitted);
    }
    record History(int entries,int lastEntry,boolean active,int entryIndex) {}

    /** Shared with spent-Z history. Current own UUID membership is independent of public appearances.
     * Only actual switches to a different owned UUID certify an exit; faints, reveals,
     * copied-state moves, form changes and non-native entry annotations cannot do so. */
    static History history(OwnRequestTransport.Request request,OwnRequestTransport.Pokemon own,List<String> events) {
        String slot=request.side()+"a",actor=slot+": "+own.dto().uuid();
        var members=new HashSet<String>();
        for(var p:request.pokemon())members.add(slot+": "+p.dto().uuid());
        boolean active=false,exited=false,alive=false;int entries=0,turn=0,lastEntry=-1,entryIndex=-1;
        for(int i=0;i<events.size();i++){
            String line=events.get(i);
            var p=line.split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(p[1])&&p[2].startsWith(slot+": ")){
                if(!members.contains(p[2]))return null;
                if(p[2].equals(actor)){
                    if(active||entries>0&&!exited||p.length!=5
                            ||!details(p[3],own)||!positive(p[4]))return null;
                    active=true;alive=true;exited=false;entries++;lastEntry=turn;entryIndex=i;
                }else if(active){
                    if(!alive)return null;
                    active=false;exited=true;
                }
                continue;
            }
            if(p[1].equals("move")&&p[2].startsWith(slot+": ")&&p.length>3
                    &&Set.of("Baton Pass","Shed Tail").contains(p[3]))return null;
            if(!p[2].equals(actor))continue;
            if(Set.of("replace","detailschange","-transform","-formechange","-mega","-primal","-burst","-terastallize","-dynamax","faint").contains(p[1]))return null;
            if(Set.of("-damage","-heal","-sethp").contains(p[1])){
                if(!active||p.length<4)return null;
                alive=positive(p[3]);
            }
        }
        return entries>0?new History(entries,lastEntry,active,entryIndex):null;
    }
    static Set<Integer> entryActivations(String side,String actor,int entryIndex,List<String> events){
        boolean web=false;var admitted=new HashSet<Integer>();
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);
            if(p.length>3&&Set.of("-sidestart","-sideend").contains(p[1])
                    &&p[2].split(":",2)[0].equals(side)&&Set.of("move: Sticky Web","Sticky Web").contains(p[3]))
                web=p[1].equals("-sidestart");
            if(i<=entryIndex)continue;
            if(Set.of("turn","move","switch","drag").contains(p[1]))break;
            if(p.length==4&&p[1].equals("-activate")&&p[2].equals(actor)&&p[3].equals("move: Sticky Web")){
                if(!web||!admitted.isEmpty()||i+1>=events.size()
                        ||!events.get(i+1).equals("|-unboost|"+actor+"|spe|1"))return null;
                admitted.add(i);
            }
        }
        return Set.copyOf(admitted);
    }
    private static boolean details(String raw,OwnRequestTransport.Pokemon own){
        String expected=own.species()+", "+own.dto().uuid()+(own.level()==100?"":", L"+own.level());
        return !own.species().isBlank()&&(raw.equals(expected)||raw.equals(expected+", shiny"));
    }
    private static boolean positive(String raw){
        if(!raw.matches("[1-9][0-9]{0,2}/(?:48|100)(?: (?:brn|par|psn|tox|slp|frz))?"))return false;
        var ratio=raw.split(" ")[0].split("/");return Integer.parseInt(ratio[0])<=Integer.parseInt(ratio[1]);
    }
    static OwnReentryEvidence inspect(PublicFieldEvidence.Capture field,OwnRequestTransport.Lease lease,
            RuntimeDomain domain,PublicProtocolEvidence protocol,PublicMechanicsEvidence.Observation observation,
            OwnRequestTransport.Request request,int receiptTurn){
        if(field==null||domain==null||protocol==null||observation==null||!lease.current()
                ||!domain.scopes().get(Operation.DAMAGE).known()||!protocol.framed()||!protocol.complete()||protocol.corrupted()
                ||!protocol.coverageUnknown().isEmpty()||!field.unknown().isEmpty()
                ||protocol.publicTurn()!=lease.turn())return null;
        field.requireCurrent(lease.turn());
        var own=request.pokemon().stream().filter(p->p.dto().active()).findFirst().orElseThrow();var dto=own.dto();
        if(!observation.slot().equals(request.side()+"a")||!observation.publicName().equals(observation.slot()+": "+dto.uuid())
                ||own.special()||!own.tera().isEmpty()||!dto.types().equals(own.baseTypes())
                ||own.level()<1||own.level()>100||!own.baseStats().keySet().equals(Set.of("atk","def","spa","spd","spe"))
                ||own.baseStats().values().stream().anyMatch(n->n<1||n>10000)
                ||!dto.condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return null;
        var hp=dto.condition().split(" ")[0].split("/");
        if(Integer.parseInt(hp[0])>Integer.parseInt(hp[1])||Integer.parseInt(hp[1])>10000)return null;
        var history=history(request,own,protocol.mechanicsEvents());
        if(history==null||history.entries()<2||!history.active()||history.lastEntry()<1
                ||receiptTurn<history.lastEntry()||lease.turn()<=history.lastEntry())return null;
        var admitted=entryActivations(request.side(),observation.publicName(),history.entryIndex(),protocol.mechanicsEvents());
        if(admitted==null||observation.unsupportedEffects()&&admitted.isEmpty())return null;
        if(!admitted.isEmpty()&&(dto.types().stream().anyMatch(t->t.equalsIgnoreCase("flying"))
                ||dto.ability().equalsIgnoreCase("levitate")||!field.magicRoom().known()
                ||own.item().equals("heavydutyboots")&&!field.magicRoom().value()))return null;
        var proof=new OwnReentryEvidence(field,lease,domain,protocol,observation,admitted);
        return proof.sourceCurrent()?proof:null;
    }
}
