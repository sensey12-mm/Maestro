package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.PublicTransitionState.*;
import static com.maestroai.training.RuntimeDomain.XerneasLifecycle;

/** Fresh own-roster source capability and finite nonpermanent Active-form witness.
 * No public observation, opponent identity, installed aspect or future request is rewritten. */
public final class OwnXerneasEvidence extends OwnEntryEvidence {
    private final RuntimeDomain domain;
    private final OwnRequestTransport.Request request;
    private final boolean form;
    private OwnXerneasEvidence(PublicFieldEvidence.Capture field,OwnRequestTransport.Lease lease,
            RuntimeDomain domain,PublicProtocolEvidence protocol,OwnRequestTransport.Request request,
            PublicMechanicsEvidence.Observation observation,Set<Integer> admitted,boolean form){
        super(field,lease,domain,protocol,form?observation:null,admitted);
        this.domain=domain;this.request=request;this.form=form;
    }
    boolean hasForm(){return form;}
    boolean binds(PublicFieldEvidence.Capture field){return current()&&(form?field.ownedBy(this):field==origin);}
    Known<Boolean> permitsEntry(String actor){
        if(!current()||actor.equals("opponent"))return Known.unknown();
        var owned=request.pokemon().stream().filter(p->actor.equals("self")?p.dto().active():p.dto().uuid().toString().equals(actor)).findFirst();
        if(owned.isEmpty()||owned.get().species().isBlank())return Known.unknown();
        String species=owned.get().species().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        if(!Set.of("xerneas","xerneasactive","xerneasneutral").contains(species))return Known.of(true,lease.source()+":other-own-species");
        var mode=domain.xerneasLifecycle();if(!mode.known())return Known.unknown();
        boolean supported=species.equals("xerneasneutral")||species.equals("xerneas")&&mode.value()!=XerneasLifecycle.SHIFT_UNPROVED
            ||species.equals("xerneasactive")&&Set.of(XerneasLifecycle.NATIVE_REGISTERED,XerneasLifecycle.SHIFT_REGISTERED).contains(mode.value());
        return supported?Known.of(true,lease.source()+":source-known-Xerneas-entry"):Known.unknown();
    }
    static boolean ordinary(OwnRequestTransport.Pokemon own){
        if(own==null||own.dto()==null)return false;var dto=own.dto();
        if(!"Xerneas".equals(own.species())||!"fairyaura".equalsIgnoreCase(dto.ability())||!dto.types().equals(List.of("Fairy"))
                ||!own.baseTypes().equals(dto.types())||own.special()||!"".equals(own.tera())
                ||!own.baseStats().keySet().equals(Set.of("atk","def","spa","spd","spe"))
                ||own.baseStats().values().stream().anyMatch(n->n<1||n>10000)||own.level()<1||own.level()>100
                ||dto.condition()==null||!dto.condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return false;
        var hp=dto.condition().split(" ")[0].split("/");return Integer.parseInt(hp[0])<=Integer.parseInt(hp[1])&&Integer.parseInt(hp[1])<=10000;
    }
    record Lifecycle(OwnReentryEvidence.History history,int lastExitTurn) {}
    /** Common ordered original form/entry proof; it need not certify a current
     * Xerneas. Only the active-form witness below can supplement observation coverage. */
    static Lifecycle lifecycle(OwnRequestTransport.Request request,OwnRequestTransport.Pokemon own,boolean nativeShift,
            List<String> events,Set<Integer> admitted){
        String actor=request.side()+"a: "+own.dto().uuid();
        boolean active=false,window=false,shifted=false;int turn=0,lastExit=-1;
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);window=false;continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(p[1])&&p[2].startsWith(request.side()+"a: ")){
                if(active&&nativeShift&&!shifted)return null;
                boolean incoming=p[2].equals(actor);if(active&&!incoming)lastExit=turn;
                active=incoming;window=active;shifted=false;continue;
            }
            if(!p[2].equals(actor))continue;
            if(p[1].equals("-formechange")){
                if(!nativeShift||!active||!window||shifted||p.length!=5||!p[3].equals("Xerneas-Active")||!p[4].isEmpty())return null;
                admitted.add(i);shifted=true;continue;
            }
            window=false;
            if(Set.of("replace","detailschange","-transform","-mega","-primal","-burst","-terastallize","-dynamax","-endability","faint").contains(p[1]))return null;
        }
        if(active&&nativeShift&&!shifted)return null;
        var history=OwnReentryEvidence.history(request,own,events,admitted);
        return history==null?null:new Lifecycle(history,lastExit);
    }
    static OwnXerneasEvidence inspect(PublicFieldEvidence.Capture field,OwnRequestTransport.Lease lease,
            RuntimeDomain domain,PublicProtocolEvidence protocol,PublicMechanicsEvidence.Observation observation,
            OwnRequestTransport.Request request,int receiptTurn){
        if(field==null||domain==null||protocol==null||!lease.current()||!protocol.framed()||!protocol.complete()||protocol.corrupted()
                ||protocol.publicTurn()!=lease.turn())return null;
        field.requireCurrent(lease.turn());
        var registryOnly=new OwnXerneasEvidence(field,lease,domain,protocol,request,null,Set.of(),false);
        var mode=domain.xerneasLifecycle();
        if(!mode.known()||mode.value()==XerneasLifecycle.SHIFT_UNPROVED||observation==null
                ||field.unknown().stream().anyMatch(s->!s.equals("INDIRECT_EXECUTION_UNRESOLVED"))
                ||!protocol.coverageUnknown().isEmpty())return registryOnly;
        boolean nativeShift=mode.value()==XerneasLifecycle.SHIFT_REGISTERED;
        var own=request.pokemon().stream().filter(p->p.dto().active()).findFirst().orElseThrow();var dto=own.dto();
        String actor=request.side()+"a: "+dto.uuid();
        if(!ordinary(own)||!observation.publicName().equals(actor)||!observation.slot().equals(request.side()+"a"))return registryOnly;
        var events=protocol.mechanicsEvents();var geomancy=OwnGeomancyEvidence.completed(actor,events);
        if(geomancy==null)return registryOnly;
        var admitted=new HashSet<Integer>(geomancy.annotations());
        var lifecycle=lifecycle(request,own,nativeShift,events,admitted);
        if(lifecycle==null)return registryOnly;var history=lifecycle.history();int entry=history.lastEntry();
        if(!history.active()||entry<0||receiptTurn<entry||lease.turn()<=entry)return registryOnly;
        var activations=OwnReentryEvidence.entryActivations(request.side(),actor,history.entryIndex(),events);
        if(activations==null)return registryOnly;
        if(!OwnReentryEvidence.webEntryAllowed(own,field,events,activations))return registryOnly;
        admitted.addAll(activations);
        var proof=new OwnXerneasEvidence(field,lease,domain,protocol,request,observation,admitted,true);
        return proof.sourceCurrent()?proof:registryOnly;
    }
}
