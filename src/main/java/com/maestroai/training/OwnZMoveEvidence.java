package com.maestroai.training;

import java.util.*;
import java.util.function.Supplier;
import static com.maestroai.training.ExecutionCoverage.Operation;

/** Finite, current-own-choice capability. Never an opponent payload or a future-use receipt. */
public final class OwnZMoveEvidence {
    public static final String MOVE="soulstealing7starstrike", NAME="Soul-Stealing 7-Star Strike";
    public record MoveSlot(String id,int pp,boolean disabled,String target) {}
    public record ZSlot(String move,String target,boolean disabled) {}
    public record Choice(List<MoveSlot> moves,Map<Integer,ZSlot> options,int optionSlots) {
        public Choice {moves=List.copyOf(moves);options=Map.copyOf(options);}
    }
    /** Native response uses the base move's slot and the zmove gimmick, never the converted ID. */
    public record Activation(String baseMove,int slot,String gimmick) {}
    private final OwnRequestTransport.Lease lease;
    private final PublicFieldEvidence.Capture field;
    private final PublicMechanicsEvidence.Observation own;
    private final Choice choice;
    private final Supplier<Choice> installed;
    private final boolean known;
    private final Activation activation;
    private boolean withdrawn;
    private OwnZMoveEvidence(OwnRequestTransport.Lease lease,PublicFieldEvidence.Capture field,
            PublicMechanicsEvidence.Observation own,Choice choice,Supplier<Choice> installed,boolean known,Activation activation){
        this.lease=lease;this.field=field;this.own=own;this.choice=choice;this.installed=installed;this.known=known;this.activation=activation;
    }
    static OwnZMoveEvidence inspect(OwnRequestTransport.Lease lease,RuntimeDomain domain,
            PublicMechanicsEvidence.Observation own,PublicFieldEvidence.Capture field,OwnRequestTransport.Request request,Supplier<Choice> installed){
        Choice choice=request.selection();boolean known=false;Activation activation=null;
        try{
            field.requireCurrent(lease.turn());
            if(domain==null||!domain.scopes().get(Operation.DAMAGE).known()||choice==null||installed==null
                    ||!choice.equals(installed.get())||choice.moves().isEmpty()||choice.moves().size()>4
                    ||choice.moves().stream().anyMatch(m->!m.id().matches("[a-z0-9]{1,64}")||m.pp()<0||m.pp()>99)
                    ||choice.moves().stream().map(MoveSlot::id).distinct().count()!=choice.moves().size())throw new IllegalArgumentException();
            var pokemon=request.pokemon().stream().filter(p->p.dto().active()).findFirst().orElseThrow();
            if(own==null||!own.slot().equals(request.side()+"a")||!own.publicName().equals(own.slot()+": "+pokemon.dto().uuid())
                    ||!lease.current())throw new IllegalArgumentException();
            if(choice.optionSlots()==0&&choice.options().isEmpty())known=true; // Currently unadvertised; not proof of prior use.
            else {
                if(field.hasOwnedZUse()||choice.optionSlots()!=choice.moves().size()||choice.options().size()!=1
                        ||!pokemon.species().equals("Marshadow")||!pokemon.item().equals("marshadiumz")
                        ||pokemon.special()||!pokemon.tera().isEmpty()||!pokemon.dto().ability().equalsIgnoreCase("technician")
                        ||!pokemon.dto().types().equals(pokemon.baseTypes())
                        ||!pokemon.dto().types().stream().map(t->t.toLowerCase(Locale.ROOT)).toList().equals(List.of("fighting","ghost"))
                        ||!field.ordinaryOwnEntry(own.publicName(),"Marshadow",pokemon.dto().uuid(),pokemon.level())
                        ||own.ambiguous()||own.unsupportedEffects()||!field.allActorsCovered()||!field.unknown().isEmpty())throw new IllegalArgumentException();
                var option=choice.options().entrySet().iterator().next();int slot=option.getKey();
                if(slot<0||slot>=choice.moves().size()||!option.getValue().equals(new ZSlot(NAME,"normal",false)))throw new IllegalArgumentException();
                var base=choice.moves().get(slot);
                if(!base.id().equals("spectralthief")||base.disabled()||base.pp()<=0||!base.target().equals("normal"))throw new IllegalArgumentException();
                known=true;activation=new Activation(base.id(),slot+1,"zmove");
            }
        }catch(RuntimeException unsupported){known=false;activation=null;}
        return new OwnZMoveEvidence(lease,field,own,choice,installed,known,activation);
    }
    public boolean current(){
        try{field.requireCurrent(lease.turn());
            if(!known||!lease.current()||choice==null||!choice.equals(installed.get()))withdrawn=true;
        }catch(RuntimeException expired){withdrawn=true;}
        return !withdrawn;
    }
    public Optional<Activation> activation(){return current()?Optional.ofNullable(activation):Optional.empty();}
    boolean permits(String role,PublicFieldEvidence.Capture capture,PublicMechanicsEvidence.Observation observation){
        return role.equals("self")&&capture==field&&observation==own&&activation().isPresent();
    }
}
