package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;
import static com.maestroai.training.RuntimeDomain.XerneasLifecycle;

/** Completed past own execution only. Grants no actor identity, retained boosts,
 * future charge, replacement choice, Z use or opponent information. */
final class OwnGeomancyEvidence {
    final PublicFieldEvidence.Capture origin;
    private final OwnRequestTransport.Lease lease;
    private final RuntimeDomain domain;
    private final PublicProtocolEvidence protocol;
    private final List<String> events;
    private final Set<Integer> admitted;
    private OwnGeomancyEvidence(PublicFieldEvidence.Capture origin,OwnRequestTransport.Lease lease,RuntimeDomain domain,
            PublicProtocolEvidence protocol,Set<Integer> admitted){
        this.origin=origin;this.lease=lease;this.domain=domain;this.protocol=protocol;
        this.events=protocol.mechanicsEvents();this.admitted=Set.copyOf(admitted);
    }
    boolean current(){return lease.current()&&protocol.complete()&&!protocol.corrupted()
        &&protocol.mechanicsEvents()==events&&protocol.publicTurn()==lease.turn();}
    boolean sourceCurrent(){return current()&&domain.scopes().get(Operation.DAMAGE).known();}
    boolean admits(int index,String line){return admitted.contains(index)&&events.get(index).equals(line);}

    record Completion(Set<Integer> annotations,int turn) {
        Completion { annotations=Set.copyOf(annotations); }
    }
    /** Original Geomancy's exact charge, Herb/locked continuation and ordered
     * completed boosts. Items and boosts remain ordinary replayed events. */
    static Completion completed(String actor,List<String> events){
        var admitted=new HashSet<Integer>();int turn=0,completedTurn=-1;
        for(int i=0;i<events.size();i++){
            var line=events.get(i);var p=line.split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);continue;}
            if(!line.equals("|move|"+actor+"|Geomancy||[still]"))continue;
            if(turn<1)return null;
            int prepare=i+1;if(prepare>=events.size()||!events.get(prepare).equals("|-prepare|"+actor+"|Geomancy"))return null;
            int at=prepare+1,finished=turn;
            boolean herb=at<events.size()&&events.get(at).equals("|-enditem|"+actor+"|Power Herb");
            if(herb)at++;
            else {
                boolean nextTurn=false;int completion=-1;
                for(int j=at;j<events.size();j++){
                    var q=events.get(j).split("\\|",-1);
                    if(q[1].equals("turn")){
                        if(nextTurn||Integer.parseInt(q[2])!=turn+1)return null;
                        nextTurn=true;finished=turn+1;continue;
                    }
                    if(q.length<3||!q[2].equals(actor))continue;
                    if(nextTurn&&events.get(j).equals("|move|"+actor+"|Geomancy|"+actor+"|[from]lockedmove")){
                        admitted.add(j);completion=j+1;break;
                    }
                    return null;
                }
                if(completion<0)return null;at=completion;
            }
            for(String stat:List.of("spa","spd","spe"))
                if(at>=events.size()||!events.get(at++).equals("|-boost|"+actor+"|"+stat+"|2"))return null;
            admitted.add(i);admitted.add(prepare);completedTurn=finished;
        }
        return new Completion(admitted,completedTurn);
    }
    static OwnGeomancyEvidence inspect(PublicFieldEvidence.Capture field,OwnRequestTransport.Lease lease,
            RuntimeDomain domain,PublicProtocolEvidence protocol,OwnRequestTransport.Request request,int receiptTurn){
        if(field==null||domain==null||protocol==null||!lease.current()||!protocol.framed()||!protocol.complete()
                ||protocol.corrupted()||!protocol.coverageUnknown().isEmpty()||protocol.publicTurn()!=lease.turn())return null;
        field.requireCurrent(lease.turn());
        var mode=domain.xerneasLifecycle();
        if(!mode.known()||mode.value()==XerneasLifecycle.SHIFT_UNPROVED)return null;
        var candidates=request.pokemon().stream().filter(p->"Xerneas".equals(p.species())).toList();
        if(candidates.size()!=1)return null;
        var own=candidates.getFirst();if(!OwnXerneasEvidence.ordinary(own))return null;
        String actor=request.side()+"a: "+own.dto().uuid();var events=protocol.mechanicsEvents();
        var completed=completed(actor,events);
        if(completed==null||completed.annotations().isEmpty()||completed.turn()>receiptTurn||completed.turn()>=lease.turn())return null;
        var lifecycleAdmissions=new HashSet<>(completed.annotations());
        var history=OwnXerneasEvidence.lifecycle(request,own,mode.value()==XerneasLifecycle.SHIFT_REGISTERED,events,lifecycleAdmissions);
        if(history==null||history.history().active()!=own.dto().active()
                ||receiptTurn<Math.max(history.history().lastEntry(),history.lastExitTurn())
                ||lease.turn()<=Math.max(history.history().lastEntry(),history.lastExitTurn()))return null;
        var web=new HashSet<Integer>();
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);
            if(p.length>2&&Set.of("switch","drag").contains(p[1])&&p[2].equals(actor)){
                var activations=OwnReentryEvidence.entryActivations(request.side(),actor,i,events);
                if(activations==null)return null;web.addAll(activations);
            }
        }
        boolean magicRoom=false;
        for(int i=0;i<events.size();i++){var p=events.get(i).split("\\|",-1);
            if(p.length>2&&Set.of("-fieldstart","-fieldend").contains(p[1])&&p[2].equals("move: Magic Room"))magicRoom=p[1].equals("-fieldstart");
            if(p.length<3||!p[2].equals(actor))continue;
            // A later clean own request cannot retrospectively prove unsupported
            // ability changes/suppression or another activation during the charge.
            if(Set.of("-start","-end","-endability").contains(p[1]))return null;
            if(p[1].equals("-ability")&&(p.length!=4||!p[3].equals("Fairy Aura")))return null;
            if(p[1].equals("-activate")&&(!web.contains(i)||own.item().equals("heavydutyboots")&&!magicRoom))return null;
        }
        var proof=new OwnGeomancyEvidence(field,lease,domain,protocol,completed.annotations());
        return proof.sourceCurrent()?proof:null;
    }
}
