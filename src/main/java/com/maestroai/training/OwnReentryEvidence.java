package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Ordinary own returns and exact first-entry Web effects. No opponent or future-entry proof. */
final class OwnReentryEvidence extends OwnEntryEvidence {
    private OwnReentryEvidence(PublicFieldEvidence.Capture field, OwnRequestTransport.Lease lease,
            RuntimeDomain domain, PublicProtocolEvidence protocol, PublicMechanicsEvidence.Observation observation,Set<Integer> admitted) {
        super(field,lease,domain,protocol,observation,admitted);
    }
    record History(int entries,int lastEntry,boolean active,int entryIndex) {}
    private static boolean ownAlive(OwnRequestTransport.Pokemon owned){
        if(owned==null||owned.special()||!"".equals(owned.tera())||owned.dto().condition()==null
                ||!owned.dto().condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return false;
        var hp=owned.dto().condition().split(" ")[0].split("/");
        return Integer.parseInt(hp[0])<=Integer.parseInt(hp[1])&&Integer.parseInt(hp[1])<=10000;
    }
    /** An observed native U-turn entry is not a predicted replacement. Require
     * a living owned donor, its exact direct move/damage sequence and the
     * original annotation; copied/blocked/foreign entries acquire no witness. */
    static Set<Integer> pivotEntries(OwnRequestTransport.Request request,String actor,List<String> events){
        var admitted=new HashSet<Integer>();String slot=request.side()+"a: ",opposing=request.side().equals("p1")?"p2a: ":"p1a: ";
        var members=new HashMap<String,OwnRequestTransport.Pokemon>();
        for(var p:request.pokemon())members.put(slot+p.dto().uuid(),p);
        String current=null,donor=null,target=null;boolean hit=false,alive=false;
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);String event=p[1];
            if(event.equals("turn")){donor=null;target=null;hit=false;continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(event)&&p[2].startsWith(slot)){
                if(p[2].equals(actor)&&p.length!=5){
                    if(p.length!=6||!p[5].equals("[from] U-turn")||donor==null||!donor.equals(current)||!hit||!alive)return null;
                    admitted.add(i);
                }
                current=p[2];donor=null;target=null;hit=false;continue;
            }
            if(event.equals("move")){
                donor=null;target=null;hit=false;alive=false;
                if(p.length==5&&p[2].equals(current)&&p[3].equals("U-turn")&&p[4].startsWith(opposing)){
                    var owned=members.get(current);
                    if(ownAlive(owned)){
                        donor=current;target=p[4];alive=true;
                    }
                }
                continue;
            }
            if(donor==null)continue;
            if(event.equals("-damage")&&p.length>=4){
                if(p[2].equals(target)&&p.length==4)hit=true;
                if(p[2].equals(donor)&&!positive(p[3]))alive=false;
            }
            if(event.equals("faint")&&p[2].equals(donor))alive=false;
        }
        return admitted;
    }

    /** Shared with spent-Z history. Current own UUID membership is independent of public appearances.
     * Only actual switches to a different owned UUID certify an exit; faints, reveals,
     * copied-state moves, form changes and non-native entry annotations cannot do so. */
    static History history(OwnRequestTransport.Request request,OwnRequestTransport.Pokemon own,List<String> events) {
        return history(request,own,events,Set.of());
    }
    static History history(OwnRequestTransport.Request request,OwnRequestTransport.Pokemon own,List<String> events,Set<Integer> admitted) {
        String slot=request.side()+"a",actor=slot+": "+own.dto().uuid();
        var pivots=pivotEntries(request,actor,events);if(pivots==null)return null;
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
                    if(active||entries>0&&!exited||p.length!=5&&!pivots.contains(i)
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
            if(admitted.contains(i))continue;
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
    private static boolean magicRoomAt(List<String> events,int index){
        boolean active=false;var actors=new HashMap<String,String>();
        for(int i=0;i<index;i++){
            var p=events.get(i).split("\\|",-1);
            if(p.length>2&&Set.of("switch","drag").contains(p[1]))actors.put(p[2].split(":",2)[0],p[2]);
            if(p.length>2&&p[2].equals("move: Magic Room")&&Set.of("-fieldstart","-fieldend").contains(p[1])){
                if(p.length!=3&&(p.length!=4||!p[3].startsWith("[of] ")||!actors.containsValue(p[3].substring(5))))return false;
                active=p[1].equals("-fieldstart");
            }
        }
        return active;
    }
    private static boolean initialOwnEntry(OwnRequestTransport.Request request,List<String> events,int before){
        int turn=0;
        for(int i=0;i<before;i++){
            var p=events.get(i).split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);continue;}
            if(p.length==5&&p[1].equals("switch")&&p[2].startsWith(request.side()+"a: ")&&turn==0&&positive(p[4]))
                return request.pokemon().stream().anyMatch(m->p[2].equals(request.side()+"a: "+m.dto().uuid()));
        }
        return false;
    }
    static boolean webEntryAllowed(OwnRequestTransport.Pokemon own,PublicFieldEvidence.Capture field,List<String> events,Set<Integer> admitted){
        return admitted.isEmpty()||own.dto().types().stream().noneMatch(t->t.equalsIgnoreCase("flying"))
            &&!own.dto().ability().equalsIgnoreCase("levitate")&&field.magicRoom().known()
            &&(!own.item().equals("heavydutyboots")||admitted.stream().allMatch(i->magicRoomAt(events,i)));
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
        // The source-bound addon PreStart lifecycle requires its own ordered
        // Active event; ordinary same-form return evidence cannot substitute.
        if(own.species().equals("Xerneas")){
            var mode=domain.xerneasLifecycle();
            if(mode.known()&&Set.of(RuntimeDomain.XerneasLifecycle.SHIFT_REGISTERED,
                    RuntimeDomain.XerneasLifecycle.SHIFT_UNPROVED).contains(mode.value()))return null;
        }
        if(!observation.slot().equals(request.side()+"a")||!observation.publicName().equals(observation.slot()+": "+dto.uuid())
                ||own.special()||!own.tera().isEmpty()||!dto.types().equals(own.baseTypes())
                ||own.level()<1||own.level()>100||!own.baseStats().keySet().equals(Set.of("atk","def","spa","spd","spe"))
                ||own.baseStats().values().stream().anyMatch(n->n<1||n>10000)
                ||!dto.condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return null;
        var hp=dto.condition().split(" ")[0].split("/");
        if(Integer.parseInt(hp[0])>Integer.parseInt(hp[1])||Integer.parseInt(hp[1])>10000)return null;
        var history=history(request,own,protocol.mechanicsEvents());
        if(history==null||!history.active()||history.lastEntry()<1
                ||receiptTurn<history.lastEntry()||lease.turn()<=history.lastEntry())return null;
        var admitted=entryActivations(request.side(),observation.publicName(),history.entryIndex(),protocol.mechanicsEvents());
        // A first entry needs the exact native Web effect, not a generic
        // capability to clear unsupported observations or assert a prior exit.
        if(admitted==null||admitted.isEmpty()&&(history.entries()<2||observation.unsupportedEffects()))return null;
        if(history.entries()==1&&!initialOwnEntry(request,protocol.mechanicsEvents(),history.entryIndex()))return null;
        if(!webEntryAllowed(own,field,protocol.mechanicsEvents(),admitted))return null;
        var proof=new OwnReentryEvidence(field,lease,domain,protocol,observation,admitted);
        return proof.sourceCurrent()?proof:null;
    }
}
