package com.maestroai.training;

import java.util.*;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Finite native permanent-form witness for an independently owned UUID.
 * Public observations remain unchanged. This supplies no opponent identity,
 * transformation choice, future request or permission without current scopes.
 */
final class OwnMegaYEvidence extends OwnEntryEvidence {
    private OwnMegaYEvidence(PublicFieldEvidence.Capture origin, OwnRequestTransport.Lease lease,
            RuntimeDomain domain, PublicProtocolEvidence protocol,
            PublicMechanicsEvidence.Observation observation, Set<Integer> admitted) {
        super(origin,lease,domain,protocol,observation,admitted);
    }

    static OwnMegaYEvidence inspect(PublicFieldEvidence.Capture field, OwnRequestTransport.Lease lease,
            RuntimeDomain domain, PublicProtocolEvidence protocol,
            PublicMechanicsEvidence.Observation observation, OwnRequestTransport.Pokemon owned,
            String side,int receiptTurn) {
        if(field==null||lease==null||domain==null||protocol==null||observation==null||owned==null
                ||!lease.current()||!domain.scopes().get(Operation.DAMAGE).known()
                ||!protocol.framed()||!protocol.complete()||protocol.corrupted()
                ||!protocol.coverageUnknown().isEmpty()||!field.unknown().isEmpty()
                ||protocol.publicTurn()!=lease.turn()||!Set.of("p1","p2").contains(side))return null;
        field.requireCurrent(lease.turn());
        var dto=owned.dto();String uuid=dto.uuid().toString(),slot=side+"a",actor=slot+": "+uuid;
        if(!dto.active()||!observation.slot().equals(slot)||!observation.publicName().equals(actor)
                ||!dto.ident().startsWith(side+": ")||!owned.item().equals("mewtwonitey")
                ||dto.ability()==null||!dto.ability().equalsIgnoreCase("insomnia")
                ||!dto.types().stream().map(t->t.toLowerCase(Locale.ROOT)).toList().equals(List.of("psychic"))||!owned.baseTypes().equals(dto.types())
                ||owned.special()||!owned.tera().isEmpty()||owned.level()<1||owned.level()>100
                ||!owned.baseStats().keySet().equals(Set.of("atk","def","spa","spd","spe"))
                ||owned.baseStats().values().stream().anyMatch(n->n<1||n>10000)
                ||dto.condition()==null||!dto.condition().matches("[1-9][0-9]{0,4}/[1-9][0-9]{0,4}( (brn|par|psn|tox|slp|frz))?"))return null;
        String[] hp=dto.condition().split(" ")[0].split("/");
        if(Integer.parseInt(hp[0])>Integer.parseInt(hp[1])||Integer.parseInt(hp[1])>10000)return null;
        boolean active=false,mega=false;int turn=0,changed=-1,entry=-1,entryIndex=-1;
        Set<Integer> admitted=new HashSet<>();var events=protocol.mechanicsEvents();
        for(int i=0;i<events.size();i++) {
            String[] p=events.get(i).split("\\|",-1);
            if(p[1].equals("turn")){turn=Integer.parseInt(p[2]);continue;}
            if(p.length<3)continue;
            if(Set.of("switch","drag").contains(p[1])&&p[2].startsWith(slot+": ")){
                boolean incoming=p[2].equals(actor);
                if(incoming&&(active||p.length!=5||!details(p[3],mega?"Mewtwo-Mega-Y":"Mewtwo",uuid,owned.level())))return null;
                active=incoming;if(incoming){entry=turn;entryIndex=i;}
                continue;
            }
            if(!p[2].equals(actor))continue;
            if(p[1].equals("detailschange")){
                if(!active||mega||turn<1||p.length!=4||!details(p[3],"Mewtwo-Mega-Y",uuid,owned.level())||i+1>=events.size())return null;
                String[] next=events.get(i+1).split("\\|",-1);
                if(next.length!=5||!next[1].equals("-mega")||!next[2].equals(actor)
                        ||!next[3].equals("Mewtwo")||!next[4].equals("Mewtwonite Y"))return null;
                admitted.add(i);admitted.add(++i);mega=true;changed=turn;continue;
            }
            // Native Insomnia Update explicitly announces a sleep cure. Admit
            // only that exact paired public event; current status still comes
            // from the fresh owner request, never from the ability name.
            if(mega&&active&&p[1].equals("-activate")&&p.length==4&&p[3].equals("ability: Insomnia")
                    &&i+1<events.size()&&events.get(i+1).equals("|-curestatus|"+actor+"|slp|[msg]")){
                admitted.add(i);continue;
            }
            if(Set.of("replace","-transform","-formechange","-mega","-primal","-burst","-terastallize","-dynamax","faint").contains(p[1]))return null;
        }
        if(!active||!mega||turn!=lease.turn()||receiptTurn<Math.max(changed,entry)
                ||lease.turn()<=Math.max(changed,entry))return null;
        var activations=OwnReentryEvidence.entryActivations(side,actor,entryIndex,events);
        if(activations==null)return null;
        admitted.addAll(activations);
        var result=new OwnMegaYEvidence(field,lease,domain,protocol,observation,admitted);
        return result.sourceCurrent()?result:null;
    }
    private static boolean details(String raw,String species,String uuid,int level){
        String expected=species+", "+uuid+(level==100?"":", L"+level);
        return raw.equals(expected)||raw.equals(expected+", shiny");
    }
}
