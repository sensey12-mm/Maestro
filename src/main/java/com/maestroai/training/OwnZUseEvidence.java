package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Past execution witness for a declared own UUID. It grants no new Z use,
 * opponent eligibility, survival, replacement or spectator identity evidence. */
final class OwnZUseEvidence {
    final PublicFieldEvidence.Capture origin;
    private final OwnRequestTransport.Lease lease;
    private final RuntimeDomain domain;
    private final PublicProtocolEvidence protocol;
    private final List<String> events;
    private final Set<Integer> admitted;
    private OwnZUseEvidence(PublicFieldEvidence.Capture origin,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            PublicProtocolEvidence protocol,Set<Integer> admitted){
        this.origin=origin;this.lease=lease;this.domain=domain;this.protocol=protocol;
        this.events=protocol.mechanicsEvents();this.admitted=Set.copyOf(admitted);
    }
    boolean current(){return lease.current()&&protocol.complete()&&!protocol.corrupted()
            &&protocol.mechanicsEvents()==events&&protocol.publicTurn()==lease.turn();}
    boolean sourceCurrent(){return current()&&domain.scopes().get(Operation.DAMAGE).known();}
    boolean admits(int index,String line){return admitted.contains(index)&&events.get(index).equals(line);}
    static OwnZUseEvidence inspect(PublicFieldEvidence.Capture field,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            PublicProtocolEvidence protocol,OwnRequestTransport.Request request,int receiptTurn){
        if(field==null||domain==null||!domain.scopes().get(Operation.DAMAGE).known()||!lease.current()
                ||protocol==null||!protocol.framed()||!protocol.complete()||protocol.corrupted()
                ||!protocol.coverageUnknown().isEmpty()||protocol.publicTurn()!=lease.turn())return null;
        field.requireCurrent(lease.turn());
        String side=request.side(),slot=side+"a",opposing=side.equals("p1")?"p2a":"p1a";
        var events=protocol.mechanicsEvents();var admitted=new HashSet<Integer>();
        String actor=null,target=null;OwnRequestTransport.Pokemon owned=null;boolean used=false;
        int turn=0,usedTurn=-1;String current=null;
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);target=null;continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(p[1])&&p[2].startsWith(slot+": ")){
                current=p[2];
                if(actor!=null&&actor.equals(current))return null; // Re-entry identity remains outside this bounded witness.
            }
            if(p[1].equals("-zpower")&&p[2].startsWith(slot+": ")){
                if(used||p.length!=3||turn<1||!p[2].equals(current)||i+1>=events.size())return null;
                actor=p[2];String identity=actor.substring((slot+": ").length());
                for(var candidate:request.pokemon())if(candidate.dto().uuid().toString().equals(identity))owned=candidate;
                if(owned==null||!owned.species().equals("Marshadow")||!owned.item().equals("marshadiumz")
                        ||owned.special()||!owned.tera().isEmpty()||!owned.dto().ability().equalsIgnoreCase("technician"))return null;
                // Independently owned request identity must match the witnessed native entry.
                if(!field.ordinaryOwnEntry(actor,"Marshadow",owned.dto().uuid(),owned.level()))return null;
                var next=events.get(i+1).split("\\|",-1);
                if(next.length!=6||!next[1].equals("move")||!next[2].equals(actor)||!next[3].equals(OwnZMoveEvidence.NAME)
                        ||!next[4].startsWith(opposing+": ")||!next[5].equals("[zeffect]"))return null;
                target=next[4];admitted.add(i);admitted.add(++i);used=true;usedTurn=turn;continue;
            }
            if(p[1].equals("move"))target=null;
            if(p[1].equals("-zbroken")){
                if(!used||p.length!=3||turn!=usedTurn||!p[2].equals(target))return null;
                admitted.add(i);target=null;continue;
            }
            if(p[2].equals(actor)&&Set.of("replace","detailschange","-transform","-formechange","-mega","-primal","-burst","-terastallize","-dynamax").contains(p[1]))return null;
        }
        if(!used||usedTurn>receiptTurn||usedTurn>=lease.turn())return null;
        var result=new OwnZUseEvidence(field,lease,domain,protocol,admitted);
        return result.sourceCurrent()?result:null;
    }
}
