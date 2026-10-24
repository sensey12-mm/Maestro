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
    /** The native Geomancy charge annotation is not indirect execution. Admit
     * only a completed original sequence, never an unfinished two-turn lock or
     * another move's annotation. Boosts/items remain ordinary replayed facts. */
    private static Set<Integer> completedGeomancy(String actor,List<String> events){
        var admitted=new HashSet<Integer>();
        for(int i=0;i<events.size();i++){
            if(!events.get(i).equals("|move|"+actor+"|Geomancy||[still]"))continue;
            int prepare=i+1;if(prepare>=events.size()||!events.get(prepare).equals("|-prepare|"+actor+"|Geomancy"))return null;
            int at=prepare+1;boolean herb=at<events.size()&&events.get(at).equals("|-enditem|"+actor+"|Power Herb");
            if(herb)at++;
            else {
                boolean nextTurn=false;int completion=-1;
                for(int j=at;j<events.size();j++){
                    var p=events.get(j).split("\\|",-1);
                    if(p[1].equals("turn")){if(nextTurn)return null;nextTurn=true;continue;}
                    if(p.length<3||!p[2].equals(actor))continue;
                    if(nextTurn&&events.get(j).equals("|move|"+actor+"|Geomancy|"+actor+"|[from]lockedmove")){
                        admitted.add(j);completion=j+1;break;
                    }
                    return null;
                }
                if(completion<0)return null;at=completion;
            }
            for(String stat:List.of("spa","spd","spe"))
                if(at>=events.size()||!events.get(at++).equals("|-boost|"+actor+"|"+stat+"|2"))return null;
            admitted.add(i);admitted.add(prepare);
        }
        return admitted;
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
        if(!own.species().equals("Xerneas")||!observation.publicName().equals(actor)||!observation.slot().equals(request.side()+"a")
                ||!dto.ability().equalsIgnoreCase("fairyaura")||!dto.types().equals(List.of("Fairy"))||!own.baseTypes().equals(dto.types())
                ||own.special()||!own.tera().isEmpty()||!own.baseStats().keySet().equals(Set.of("atk","def","spa","spd","spe"))
                ||own.baseStats().values().stream().anyMatch(n->n<1||n>10000)||own.level()<1||own.level()>100
                ||!dto.condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return registryOnly;
        var hp=dto.condition().split(" ")[0].split("/");if(Integer.parseInt(hp[0])>Integer.parseInt(hp[1])||Integer.parseInt(hp[1])>10000)return registryOnly;
        var events=protocol.mechanicsEvents();var geomancy=completedGeomancy(actor,events);
        if(geomancy==null)return registryOnly;
        var admitted=new HashSet<Integer>(geomancy);
        boolean active=false,window=false,shifted=false;int entry=-1,turn=0;
        for(int i=0;i<events.size();i++){
            var p=events.get(i).split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);window=false;continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(p[1])&&p[2].startsWith(request.side()+"a: ")){
                if(active&&nativeShift&&!shifted)return registryOnly;
                active=p[2].equals(actor);window=active;shifted=false;if(active)entry=turn;
                continue;
            }
            if(!p[2].equals(actor))continue;
            if(p[1].equals("-formechange")){
                // Native FairyAura.onPreStart uses formeChange('xerneasactive', false):
                // exact nonpermanent form output, no invented ability attribution.
                if(!nativeShift||!active||!window||shifted||p.length!=5||!p[3].equals("Xerneas-Active")||!p[4].isEmpty())return registryOnly;
                admitted.add(i);shifted=true;continue;
            }
            // Every selected state event follows native PreStart. Opposing
            // entry announcements may interleave without closing this window.
            window=false;
            if(Set.of("replace","detailschange","-transform","-mega","-primal","-burst","-terastallize","-dynamax","-endability","faint").contains(p[1]))return registryOnly;
        }
        var history=OwnReentryEvidence.history(request,own,events,admitted);
        if(!active||nativeShift&&!shifted||history==null||!history.active()||entry<0||receiptTurn<entry||lease.turn()<=entry)return registryOnly;
        var activations=OwnReentryEvidence.entryActivations(request.side(),actor,history.entryIndex(),events);
        if(activations==null)return registryOnly;
        if(!activations.isEmpty()&&(!field.magicRoom().known()||own.item().equals("heavydutyboots")&&!field.magicRoom().value()))return registryOnly;
        admitted.addAll(activations);
        var proof=new OwnXerneasEvidence(field,lease,domain,protocol,request,observation,admitted,true);
        return proof.sourceCurrent()?proof:registryOnly;
    }
}
