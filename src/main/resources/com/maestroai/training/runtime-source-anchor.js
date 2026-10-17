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
  const callbacks = new Map(), methodPins = new Map();
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
  return Object.freeze({has: fn => trusted.has(fn),
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
    hasMethod: (prototype,name,fn) => {
      if (methodPins.get(prototype)?.get(name) === fn) return true;
      // These exact script override slots are installed by the pinned constructors.
      if (prototype === modules.Battle.prototype && scripts[name] === fn) return true;
      if (prototype === modules.BattleActions.prototype && scripts.actions?.[name] === fn) return true;
      if (prototype === modules.Pokemon.prototype && scripts.pokemon?.[name] === fn) return true;
      return false;
    },
    hasHelper: (name,fn) => helpers[name] === fn
  });
})
