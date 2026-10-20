// Read-only provenance for a finite, externally hash-pinned public addon.
// Original receiver arguments, result, exception and dispatch count are kept.
(function(anchor,permitted,lifecycle){
  const globalValue=name=>{const d=Object.getOwnPropertyDescriptor(globalThis,name);return d&&!d.get&&!d.set?d.value:undefined;};
  const originalEval=globalValue('eval');
  const specs=[
    ['script','receiveScriptData','scripts','Scripts'],
    ['ability','receiveAbilityData','abilities','Abilities'],
    ['move','receiveMoveData','moves','Moves'],
    ['condition','receiveConditionData','conditions','Conditions']
  ].map(([kind,name,file,key])=>{
    const source=require('./data/mods/cobblemon/'+file),slot=Object.getOwnPropertyDescriptor(source,key),table=source[key];
    return {kind,name,key,source,slot,table,original:globalValue(name),groups:new Map()};
  });
  // index.js declares these API functions globally. Captured boot identities
  // and the exact index.js source pin remain necessary throughout the lease.
  const seen=new Set();
  const raidIds=new Set(['cheerattack','cheerdefense','raidboss','shield']);
  const invalid=new Set();let overflow=false;
  const tableCurrent=s=>{
    const d=Object.getOwnPropertyDescriptor(s.source,s.key);
    return d && d.get===s.slot.get && d.set===s.slot.set && d.value===s.slot.value
      && s.source[s.key]===s.table && Object.getPrototypeOf(s.table)===Object.prototype;
  };
  const safe=(s,id)=>{
    if(!tableCurrent(s))return false;
    const d=Object.getOwnPropertyDescriptor(s.table,id);
    if(d?.get||d?.set)return false;
    if(s.kind!=='script')return true;
    if(d && d.value!==s.groups.get(id))return false;
    if(!d)return true;
    if(!d.value || Object.getPrototypeOf(d.value)!==Object.prototype)return false;
    const slots=id==='actions'?['canMegaEvo','runMegaEvo']:id==='pokemon'?['effectiveWeather']:[];
    return slots.length>0 && slots.every(k=>{
      for(let p=d.value;p;p=Object.getPrototypeOf(p)){
        const member=Object.getOwnPropertyDescriptor(p,k);if(member)return !member.get&&!member.set;
      }
      return true;
    });
  };
  for(const s of specs){
    for(const id of ['actions','pokemon']){
      const d=Object.getOwnPropertyDescriptor(s.table,id);
      if(s.kind==='script' && d&&!d.get&&!d.set)s.groups.set(id,d.value);
    }
    s.receiver=function(id,code){
      lifecycle(true);
      try{
        const token=typeof id==='string'?s.kind+'/'+id:id;
        if(invalid.size<64)invalid.add(token);else overflow=true;
        const known=typeof id==='string' && typeof code==='string' && globalValue('eval')===originalEval
          && permitted(s.kind,id,code) && safe(s,id);
        const result=s.original.apply(this,arguments);
        if(known && globalValue('eval')===originalEval && tableCurrent(s)){
          const d=Object.getOwnPropertyDescriptor(s.table,id);
          if(d&&!d.get&&!d.set){
            try{
              anchor.observeDefinition(s.kind,id,d.value);
              if(s.kind==='script')s.groups.set(id,d.value);
              seen.add(s.kind+'/'+id);
              invalid.delete(token);
            }catch(unavailable){/* Keep original dispatch; abstain from proof. */}
          }
        }
        return result;
      }finally{lifecycle(false);}
    };
    globalThis[s.name]=s.receiver;
  }
  return Object.freeze({current:()=>!overflow && invalid.size===0 && globalValue('eval')===originalEval
      && specs.every(s=>globalValue(s.name)===s.receiver&&tableCurrent(s)),
    complete:()=>[...seen].filter(k=>!k.startsWith('condition/')||!raidIds.has(k.slice(10))).length===12,
    raidComplete:()=>[...raidIds].every(id=>seen.has('condition/'+id))});
})
