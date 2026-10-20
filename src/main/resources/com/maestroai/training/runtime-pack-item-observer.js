// Observe only the pinned pack's PUBLIC item-definition API. The original
// receiver executes the original arguments exactly once; no battle/team input.
(function (anchor, permitted, lifecycle) {
  const globalValue=name=>{const d=Object.getOwnPropertyDescriptor(globalThis,name);return d&&!d.get&&!d.set?d.value:undefined;};
  const original=globalValue('receiveHeldItemData'), originalEval=globalValue('eval');
  const source=require('./data/mods/cobblemon/items'), slot=Object.getOwnPropertyDescriptor(source,'Items'), table=source.Items;
  const tableCurrent=()=>{
    const d=Object.getOwnPropertyDescriptor(source,'Items');
    return d && d.get===slot.get && d.set===slot.set && d.value===slot.value && source.Items===table;
  };
  let receiver;
  const addonItems=new Set();
  const invalid=new Set();let overflow=false;
  const targetSafe=id=>{
    if(!tableCurrent() || Object.getPrototypeOf(table)!==Object.prototype) return false;
    for(let p=table;p;p=Object.getPrototypeOf(p)){
      const d=Object.getOwnPropertyDescriptor(p,id);
      if(d)return !d.get&&!d.set;
    }
    return true;
  };
  receiver=function(id,code){
    lifecycle(true);
    try {
      if(invalid.size<64)invalid.add(id);else overflow=true;
      const known=typeof id==='string' && typeof code==='string' && globalValue('eval')===originalEval
        && targetSafe(id) && permitted(id,code);
      const result=original.apply(this,arguments);
      // Source-known factories are pure object expressions: no top-level
      // callback executes. Keep the actual newly created callback identities.
      if(known && globalValue('eval')===originalEval && targetSafe(id)) {
        const d=Object.getOwnPropertyDescriptor(table,id);
        try { if(d && !d.get && !d.set){
          if(['eviolite','legendplate','pikaniumz','pikashuniumz'].includes(id))anchor.observeItem(id,d.value);
          else {anchor.observeDefinition('heldItem',id,d.value);addonItems.add(id);}
          invalid.delete(id);
        } }
        catch(unavailable) { /* Observation abstains; original dispatch still succeeds. */ }
      }
      return result;
    } finally { lifecycle(false); }
  };
  receiveHeldItemData=receiver;
  return Object.freeze({current:()=>!overflow && invalid.size===0 && globalValue('receiveHeldItemData')===receiver && globalValue('eval')===originalEval
    && tableCurrent(),
    complete:()=>anchor.itemsReloaded(), addonComplete:()=>addonItems.size===46});
})
