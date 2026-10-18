// Evaluated immediately after an exact source-verified boot, before public
// registry transport. Only source modules/dex configuration, never actor state.
(function (modules) {
  const trusted = new Set(), seen = new Set();
  let budget = 500000;
  const visit = value => {
    if (!value || !['function', 'object'].includes(typeof value) || seen.has(value)) return;
    if (--budget < 0) throw Error('source identity budget exceeded');
    seen.add(value);
    if (typeof value === 'function') trusted.add(value);
    for (const key of Object.getOwnPropertyNames(value)) {
      const d = Object.getOwnPropertyDescriptor(value, key);
      // Capture accessor identities without calling them. Configuration accessors
      // are still forbidden by the actual domain probe.
      if (d.get) trusted.add(d.get);
      if (d.set) trusted.add(d.set);
      if ('value' in d) visit(d.value);
    }
    const prototype = Object.getPrototypeOf(value);
    if (prototype && prototype !== Object.prototype && prototype !== Function.prototype && prototype !== Array.prototype)
      visit(prototype);
  };
  for(const group of ['ability','heldItem','move','bagItem','species']) {
    const registry=modules.Cobblemon.registries[group];
    const d=registry && Object.getOwnPropertyDescriptor(registry,'contents');
    if(!d || d.get || d.set || !(d.value instanceof Map) || d.value.size)
      throw Error('source anchor requires pre-transport public registries');
  }
  visit(modules);
  // Utils is an esbuild namespace with source-pinned export getters. Resolve
  // only this named module namespace, never an arbitrary configuration accessor.
  visit({...modules.Utils});
  const dex = require('./sim/dex').Dex.mod('cobblemon').includeData();
  visit(dex);
  // Populate only public source-definition caches at verified boot, before
  // external transport. A later direct cache injection must not bypass the
  // registry proof. No battle, actor, team or request is accessed here.
  const cachePins=new Map();
  for(const [group,table,cache] of [['moves','Moves','moveCache'],['abilities','Abilities','abilityCache'],
      ['items','Items','itemCache'],['species','Pokedex','speciesCache']]) {
    for(const id of Object.keys(dex.dataCache[table])) dex[group].getByID(id);
    for(const id of Object.keys(dex.dataCache.Aliases)) dex[group].getByID(id);
    cachePins.set(group,{name:cache,values:new Map(dex[group][cache])});
  }
  const conditionIds=new Set([...Object.keys(dex.dataCache.Conditions),...Object.keys(dex.dataCache.Rulesets),
    'recoil','drain','none']);
  for(const table of ['Moves','Abilities','Items']) for(const [id,value] of Object.entries(dex.dataCache[table]))
    if(value.condition) conditionIds.add(id);
  for(const id of Object.keys(dex.dataCache.Abilities)) conditionIds.add('ability:'+id);
  for(const id of Object.keys(dex.dataCache.Items)) conditionIds.add('item:'+id);
  for(const id of conditionIds) dex.conditions.getByID(id);
  // The pinned Battle.runAction('start') lifecycle asks for a species-ID
  // condition even when none exists. These negative lookups are cached only
  // once actors enter, so a boot-only inventory cannot enumerate them all.
  // Retain the exact native constructor/freeze identities, never an external
  // callback, and compare the entire frozen result before accepting a miss.
  const EmptyCondition = Object.getPrototypeOf(dex.conditions.getByID('none')).constructor;
  const freezeCondition = modules.Utils.deepFreeze;
  cachePins.set('conditions',{name:'conditionCache',values:new Map(dex.conditions.conditionCache)});
  // Snapshot fields by value/callback identity as well as reference. Some native
  // cache entries are frozen; mutable entries still require a recursive guard.
  const cachedValues=new Map(), cachePrototypes=new Map(), prototypeDescriptors=new Map();
  for(const {values} of cachePins.values()) for(const value of values.values())
    if(!cachedValues.has(value)) {
      cachedValues.set(value,null);cachePrototypes.set(value,Object.getPrototypeOf(value));
      for(let p=Object.getPrototypeOf(value);p && p!==Object.prototype;p=Object.getPrototypeOf(p))
        if(!prototypeDescriptors.has(p)) prototypeDescriptors.set(p,Object.getOwnPropertyDescriptors(p));
    }
  const copyEntry = (x, budget) => {
    if (--budget.n < 0) throw Error('public definition budget exceeded');
    if (x === null || typeof x !== 'object') return x;
    const out = Array.isArray(x) ? [] : Object.create(null);
    for (const k of Object.getOwnPropertyNames(x)) {
      const d=Object.getOwnPropertyDescriptor(x,k);
      if(d.get || d.set) throw Error('public definition accessor');
      out[k]=copyEntry(d.value,budget);
    }
    return out;
  };
  for(const value of cachedValues.keys()) cachedValues.set(value,copyEntry(value,{n:500000}));
  const originalEntries = {};
  for(const table of ['Moves','Abilities','Items']) originalEntries[table]=copyEntry(dex.dataCache[table],{n:500000});
  const entryEqual = (a,b,budget) => {
    if (--budget.n < 0) throw Error('public definition comparison budget exceeded');
    if (Object.is(a,b)) return true;
    if (!a || !b || typeof a !== 'object' || typeof b !== 'object' || Array.isArray(a)!==Array.isArray(b)) return false;
    const ak=Object.getOwnPropertyNames(a).sort(),bk=Object.getOwnPropertyNames(b).sort();
    return ak.length===bk.length && ak.every((k,i)=>k===bk[i] && (()=>{
      const d=Object.getOwnPropertyDescriptor(b,k);
      return d && !d.get && !d.set && entryEqual(a[k],d.value,budget);
    })());
  };
  const nativeConditionMiss = (id,value) => {
    if(typeof id!=='string' || !/^[a-z0-9]{1,128}$/.test(id) || conditionIds.has(id)
        || !value || Object.getPrototypeOf(value)!==EmptyCondition.prototype) return false;
    const expected=freezeCondition(new EmptyCondition({name:id,exists:false}));
    const before=Object.getOwnPropertyDescriptors(expected),after=Object.getOwnPropertyDescriptors(value);
    const keys=Reflect.ownKeys(before);
    if(Reflect.ownKeys(after).length!==keys.length || keys.some(k=>!after[k] ||
        ['get','set','enumerable','writable','configurable'].some(flag=>!Object.is(before[k][flag],after[k][flag])))) return false;
    return entryEqual(expected,value,{n:20000});
  };
  const callbacks = new Map(), methodPins = new Map();
  const reloadedItems = new Set();
  const tableCallbacks = (value, path, collect) => {
    if (typeof value === 'function') {
      const key = JSON.stringify(path);
      if (collect) callbacks.set(key,value);
      else if (callbacks.get(key) !== value) throw Error('public source callback position changed');
      return;
    }
    if (!value || typeof value !== 'object') return;
    const proto = Object.getPrototypeOf(value);
    if (proto !== null && proto !== Object.prototype && proto !== Array.prototype) throw Error('unverified operation table prototype');
    for (const key of Object.getOwnPropertyNames(value)) {
      const d = Object.getOwnPropertyDescriptor(value,key);
      if (d.get || d.set) throw Error('public source callback accessor');
      tableCallbacks(d.value,[...path,key],collect);
    }
  };
  for (const table of ['Scripts','Moves','Abilities','Items','Conditions','TypeChart','Natures','Formats','Aliases'])
    tableCallbacks(dex.dataCache[table],[table],true);
  for (const value of seen) {
    const methods = new Map();
    for (const key of Object.getOwnPropertyNames(value)) {
      const d = Object.getOwnPropertyDescriptor(value,key);
      if (typeof d.value === 'function') methods.set(key,d.value);
    }
    if (methods.size) methodPins.set(value,methods);
  }
  const scripts = {...dex.dataCache.Scripts,actions:{...dex.dataCache.Scripts.actions},pokemon:{...dex.dataCache.Scripts.pokemon}};
  const helpers = {toID:dex.toID,trunc:dex.trunc,clampIntRange:modules.Utils.clampIntRange};
  const speciesPrototype=Object.getPrototypeOf(dex.species.getByID('mew'));
  const hasMethod = (prototype,name,fn) => {
      if (methodPins.get(prototype)?.get(name) === fn) return true;
      // These exact script override slots are installed by the pinned constructors.
      if (prototype === modules.Battle.prototype && scripts[name] === fn) return true;
      if (prototype === modules.BattleActions.prototype && scripts.actions?.[name] === fn) return true;
      if (prototype === modules.Pokemon.prototype && scripts.pokemon?.[name] === fn) return true;
      return false;
  };
  // Fixed public roles and slot names are captured from source-verified boot.
  // Later diagnostics never print arbitrary added names, function bodies,
  // exception messages, actor data or a source-text substitute for identity.
  const roleSpecs = [
    ['BATTLE','Battle',null],['ACTIONS','BattleActions',null],['QUEUE','BattleQueue',null],['BASIC_EFFECT','BasicEffect',null],
    ['FIELD','Field',null],['POKEMON','Pokemon',null],['REGISTRY_BASE','REGISTRY_BASE',null],
    ...['ability','heldItem','move','bagItem','species'].map(k=>['REGISTRY_'+k.toUpperCase(),null,k]),
    ...['dex','moves','items','abilities','species','conditions','formats'].map(k=>['DEX_'+k.toUpperCase(),null,k])
  ].map(([role,ctor,key])=>{
    const instance=role.startsWith('REGISTRY_') ? modules.Cobblemon.registries[key]
      : role.startsWith('DEX_') ? key==='dex'?dex:dex[key] : null;
    const proto=ctor==='REGISTRY_BASE'?modules.Registries.CobbleRegistry.prototype:ctor?modules[ctor].prototype:Object.getPrototypeOf(instance);
    return {role,ctor,key,proto,instance,names:Object.getOwnPropertyNames(proto).filter(n=>n!=='constructor'
      && typeof Object.getOwnPropertyDescriptor(proto,n).value==='function').sort()};
  });
  const registryRoot=modules.Cobblemon.registries;
  const sourceConstructors=Object.fromEntries(['Battle','BattleActions','BattleQueue','Field','Pokemon',
    'Ability','Item','DataMove','BasicEffect'].map(k=>[k,modules[k]]));
  const normalizationHelpers={deepClone:dex.deepClone,deepFreeze:dex.deepFreeze};
  const diagnosticTokens=new Set();
  for(const key of Object.keys(modules.Registries))diagnosticTokens.add('CONSTRUCTOR/REGISTRY_'+key+'/IDENTITY');
  const registryConstructors={...modules.Registries};
  for(const spec of roleSpecs) for(const name of [...spec.names,'UNLISTED_PUBLIC_SLOT'])
    for(const reason of ['ACCESSOR','IDENTITY','CALLBACK','PROTOTYPE','MISSING'])
      diagnosticTokens.add(spec.role+'/'+name+'/'+reason);
  for(const k of Object.keys(sourceConstructors))diagnosticTokens.add('CONSTRUCTOR/'+k+'/IDENTITY');
  for(const k of ['toID','trunc','clampIntRange','deepClone','deepFreeze'])diagnosticTokens.add('HELPER/'+k+'/IDENTITY');
  const resolve=(object,key)=>{
    for(let o=object;o;o=Object.getPrototypeOf(o)) {
      const d=Object.getOwnPropertyDescriptor(o,key);if(d)return {d,owner:o};
    }
    return null;
  };
  const auditMethods=(fresh,battle)=>{
    const failures=[];
    for(const [key,expected] of Object.entries(sourceConstructors))
      if(fresh[key]!==expected)failures.push('CONSTRUCTOR/'+key+'/IDENTITY');
    const dexSlot=battle?resolve(battle,'dex'):null;
    for(const [key,expected] of Object.entries(registryConstructors))
      if(fresh.Registries[key]!==expected)failures.push('CONSTRUCTOR/REGISTRY_'+key+'/IDENTITY');
    const rootSlot=fresh.Cobblemon===modules.Cobblemon?Object.getOwnPropertyDescriptor(fresh.Cobblemon,'registries'):null;
    const registryRootCurrent=rootSlot&&!rootSlot.get&&!rootSlot.set&&rootSlot.value===registryRoot;
    const actualDex=battle?(dexSlot && !dexSlot.d.get && !dexSlot.d.set?dexSlot.d.value:null):dex;
    for(const spec of roleSpecs) {
      let instance=null,proto=spec.proto;
      if(spec.ctor==='REGISTRY_BASE'){
        if(fresh.Registries.CobbleRegistry!==registryConstructors.CobbleRegistry)continue;
        proto=fresh.Registries.CobbleRegistry.prototype;
      } else if(spec.ctor){
        if(fresh[spec.ctor]!==sourceConstructors[spec.ctor])continue;
        proto=fresh[spec.ctor].prototype;
        if(battle && ['BATTLE','ACTIONS','QUEUE','FIELD'].includes(spec.role)) {
          const key={BATTLE:null,ACTIONS:'actions',QUEUE:'queue',FIELD:'field'}[spec.role];
          const slot=key?resolve(battle,key):null;
          instance=key?(slot&&!slot.d.get&&!slot.d.set?slot.d.value:null):battle;
          if(!instance){failures.push(spec.role+'/UNLISTED_PUBLIC_SLOT/MISSING');continue;}
        }
      } else if(spec.role.startsWith('REGISTRY_')) {
        if(!registryRootCurrent){failures.push(spec.role+'/UNLISTED_PUBLIC_SLOT/IDENTITY');continue;}
        const slot=resolve(registryRoot,spec.key);
        instance=slot&&!slot.d.get&&!slot.d.set?slot.d.value:null;
      } else if(actualDex===dex) {
        const slot=spec.key==='dex'?null:resolve(actualDex,spec.key);
        instance=spec.key==='dex'?actualDex:slot&&!slot.d.get&&!slot.d.set?slot.d.value:null;
      }
      if(!spec.ctor && instance!==spec.instance){failures.push(spec.role+'/UNLISTED_PUBLIC_SLOT/IDENTITY');continue;}
      if(!spec.ctor)proto=instance?Object.getPrototypeOf(instance):null;
      if(proto!==spec.proto){failures.push(spec.role+'/UNLISTED_PUBLIC_SLOT/PROTOTYPE');continue;}
      const added=Object.getOwnPropertyNames(proto).some(n=>n!=='constructor'&&!spec.names.includes(n)
        && typeof Object.getOwnPropertyDescriptor(proto,n).value==='function');
      if(added)failures.push(spec.role+'/UNLISTED_PUBLIC_SLOT/IDENTITY');
      for(const name of spec.names){
        const entry=resolve(instance||proto,name),base=spec.role+'/'+name+'/';
        if(!entry){failures.push(base+'MISSING');continue;}
        if(entry.d.get||entry.d.set){failures.push(base+'ACCESSOR');continue;}
        if(!hasMethod(entry.owner,name,entry.d.value)&&!hasMethod(proto,name,entry.d.value))failures.push(base+'IDENTITY');
        else if(!trusted.has(entry.d.value))failures.push(base+'CALLBACK');
      }
    }
    for(const [key,expected] of Object.entries(normalizationHelpers)){
      const slot=actualDex===dex?resolve(actualDex,key):null;
      if(!slot || slot.d.get || slot.d.set || slot.d.value!==expected)failures.push('HELPER/'+key+'/IDENTITY');
    }
    if(battle)for(const key of ['toID','trunc','clampIntRange']){
      const slot=resolve(battle,key);
      if(!slot || slot.d.get || slot.d.set || slot.d.value!==helpers[key])failures.push('HELPER/'+key+'/IDENTITY');
    }
    return JSON.stringify({scope:battle?'PUBLIC_BATTLE_METHODS':'PUBLIC_MODULES_AND_BOOT_DEX',
      failures:[...new Set(failures)].slice(0,8),truncated:failures.length>8,actualBattle:!!battle});
  };
  return Object.freeze({has: fn => trusted.has(fn), auditMethods, diagnosticAllowed:code=>diagnosticTokens.has(code),
    // Called only by the observer-owned exact-wire boundary, never by a public
    // registry entry. No actor/request/closure text is accepted as provenance.
    observeItem: (id, value) => {
      if(!['eviolite','legendplate','pikaniumz','pikashuniumz'].includes(id)
          || value!==dex.dataCache.Items[id] || !value || Object.getPrototypeOf(value)!==Object.prototype)
        throw Error('unverified item reload target');
      const collect=x=>{
        if(typeof x==='function'){trusted.add(x);return;}
        if(!x || typeof x!=='object')return;
        if(Object.getOwnPropertySymbols(x).length || ![Object.prototype,Array.prototype,null].includes(Object.getPrototypeOf(x)))
          throw Error('unverified item reload structure');
        for(const k of Object.getOwnPropertyNames(x)){
          const d=Object.getOwnPropertyDescriptor(x,k);
          if(d.get||d.set)throw Error('unverified item reload accessor');
          collect(d.value);
        }
      };
      collect(value);tableCallbacks(value,['Items',id],true);
      originalEntries.Items[id]=copyEntry(value,{n:20000});reloadedItems.add(id);
    },
    itemsReloaded: () => reloadedItems.size===4,
    speciesEntry: (id,value) => {
      if(!value || Object.getPrototypeOf(value)!==speciesPrototype) return false;
      const name=Object.getOwnPropertyDescriptor(value,'name'),identity=Object.getOwnPropertyDescriptor(value,'id');
      return typeof id==='string' && name && !name.get && !name.set && typeof name.value==='string'
        && name.value.toLowerCase().replace(/[^a-z0-9]+/g,'')===id && identity && !identity.get && !identity.set && identity.value===id;
    },
    entryMatches: (table,id,value) => Object.hasOwn(originalEntries[table] || {},id) && entryEqual(originalEntries[table][id],value,{n:20000}),
    checkCaches: actual => {
      for(const [prototype,before] of prototypeDescriptors) {
        const after=Object.getOwnPropertyDescriptors(prototype),keys=Reflect.ownKeys(before);
        if(Reflect.ownKeys(after).length!==keys.length || keys.some(k=>!after[k] ||
            ['value','get','set','enumerable','writable','configurable'].some(flag=>!Object.is(before[k][flag],after[k][flag]))))
          throw Error('public cache prototype changed');
      }
      for(const [group,{name,values}] of cachePins) {
        const map=actual[group][name];
        if(!(map instanceof Map)||Object.getPrototypeOf(map)!==Map.prototype||Reflect.ownKeys(map).length)
          throw Error('unverified public definition cache');
        for(const [id,value] of map) {
          const expected=values.get(id);
          // An exact cached miss has no effect or callback and grants no actor,
          // move, item or ability evidence. Unsupported operations stay UNKNOWN.
          if(!expected && group==='conditions' && nativeConditionMiss(id,value)) continue;
          // Native dex invalidation may reconstruct an equal Effect instance.
          // Exact fields, source callbacks and prototype remain mandatory;
          // equal function source text is never an identity substitute.
          if(!expected || Object.getPrototypeOf(value)!==cachePrototypes.get(expected)
              || !entryEqual(cachedValues.get(expected),value,{n:500000}))
            throw Error('public definition cache injection');
        }
      }
    },
    checkTables: tables => { for (const key of Object.keys(tables)) tableCallbacks(tables[key],[key],false); },
    hasMethod,
    hasHelper: (name,fn) => helpers[name] === fn
  });
})
