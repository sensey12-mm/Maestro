// Read only PUBLIC simulator configuration. Never enumerate a battle, side, team,
// Pokemon, request, log, event handlers or a private payload. Caller supplies an
// already initialized battle; this function never starts one or changes a rule.
(function (battle, modules, registryOnly, nativeFactories, sourceAnchor, captureLease, progress) {
  const mark=phase=>{if(progress)progress.phase=phase;};
  mark('PUBLIC_CONFIGURATION_ASSOCIATION_REJECTED');
  if (!battle || !battle.dex || !battle.dex.dataCache) throw Error('domain not initialized');
  // Compile guards once while proving configuration. Later prerequisite checks
  // compare the same public references/values/functions; they do not rebuild
  // thousands of Effect objects or serialize source tables again.
  const objects=[], methodsToCheck=[], maps=[], hookObjects=[], tracked=new Set();
  const track = value => {
    if (!captureLease || tracked.has(value) || !value || typeof value !== 'object' || Object.isFrozen(value)) return;
    tracked.add(value);
    const names=Object.getOwnPropertyNames(value).sort();
    objects.push({value,proto:Object.getPrototypeOf(value),names,
      descriptors:names.map(k=>Object.getOwnPropertyDescriptor(value,k))});
  };
  const trackMap = value => {
    if (captureLease) { track(value); maps.push({value,proto:Object.getPrototypeOf(value),entries:[...value]}); }
  };
  const canonical = (value, trusted = true, record = true) => {
    // Graal includes a trailing newline for one bundled arrow expression;
    // Exact LF/CRLF source bytes are separately pinned. Normalize CRLF only
    // for this fingerprint; execution and live function identity stay unchanged.
    if (typeof value === 'function') {
      if (trusted && sourceAnchor && !sourceAnchor.has(value)) throw Error('unverified source callback identity');
      if (registryOnly === 'identity') return value;
      return {functionSource: Function.prototype.toString.call(value).replace(/\r\n/g, '\n').trim()};
    }
    if (value === undefined) return null;
    if (value === null || typeof value !== 'object') return value;
    if (Object.getOwnPropertySymbols(value).length) throw Error('uninspectable public configuration symbol');
    if(record) track(value);
    const result = Array.isArray(value) ? [] : Object.create(null);
    for (const key of Object.getOwnPropertyNames(value).sort()) {
      const descriptor = Object.getOwnPropertyDescriptor(value, key);
      // Public configuration accessors are unverified code, not values to call.
      if (!descriptor || descriptor.get || descriptor.set) throw Error('unverified configuration accessor');
      result[key] = canonical(descriptor.value, trusted, record);
    }
    return result;
  };
  // Only method descriptors: getters are never invoked and instance state is
  // never enumerated. Resolve each public engine method's actual override.
  const tagged=(group,fn)=>{if(progress)progress.methodGroup=group;return fn();};
  const methods = (instance, prototype, group='REGISTRY_IMPLEMENTATION') => {
    if(progress)progress.methodGroup=group;
    const result = {};
    for (const name of Object.getOwnPropertyNames(prototype).sort()) {
      const descriptor = Object.getOwnPropertyDescriptor(prototype, name);
      if (name !== 'constructor' && typeof descriptor.value === 'function') {
        let actual = descriptor.value, actualOwner = prototype;
        if (instance) {
          let owner = instance, effective;
          while (owner && !effective) { effective = Object.getOwnPropertyDescriptor(owner, name); if (effective) actualOwner = owner; owner = Object.getPrototypeOf(owner); }
          if (!effective || effective.get || effective.set) throw Error('unverified engine accessor');
          actual = effective.value;
        }
        if (sourceAnchor && !sourceAnchor.hasMethod(actualOwner,name,actual) && !sourceAnchor.hasMethod(prototype,name,actual)) throw Error('unverified engine method position');
        if(captureLease) methodsToCheck.push({instance:instance||prototype,name,actual});
        result[name] = canonical(actual);
      }
    }
    return result;
  };
  const dex = battle.dex, data = dex.dataCache, registries = modules.Cobblemon.registries;
  const hooks = value => {
    if(captureLease && value) hookObjects.push({value,proto:Object.getPrototypeOf(value),
      keys:Object.keys(value).filter(k=>{const d=Object.getOwnPropertyDescriptor(value,k);return k.startsWith('on')||typeof d?.value==='function';}).sort(),
      values:Object.fromEntries(Object.keys(value).map(k=>[k,Object.getOwnPropertyDescriptor(value,k).value]))});
    const result = Object.create(null);
    for (const key of Object.keys(value || {}).sort()) {
      const d = Object.getOwnPropertyDescriptor(value, key);
      if (!d || d.get || d.set) throw Error('unverified public hook accessor');
      if (key.startsWith('on') || typeof d.value === 'function') result[key] = canonical(d.value);
    }
    return result;
  };
  if(battle.actions.battle!==battle || battle.actions.dex!==dex || battle.field.battle!==battle || battle.queue.battle!==battle)
    throw Error('unverified engine association');
  mark('PUBLIC_CONFIGURATION_SPECIES_REJECTED');
  track(data);track(data.Pokedex);track(registries.species);trackMap(registries.species.contents);
  const speciesHooks = {};
  for (const [id, species] of registries.species.contents) {
    if(sourceAnchor && !sourceAnchor.speciesEntry(id,species)) throw Error('public species identity alias unsupported');
    const h = hooks(species); if (Object.keys(h).length) speciesHooks[id] = h;
  }
  const catalog = {};
  for (const id of ['zoroark','gholdengo','ditto','mew','pikachu','blissey','shedinja','shuckle',
                    'genesect','calyrexshadow','arceus','xerneas','marshadow','mewtwo']) {
    // Public registry lookup only; avoid get(), which populates caches.
    const species = registries.species.contents.get(id) || data.Pokedex[id];
    catalog[id] = species ? {stats: species.baseStats, types: species.types, fixedHp: species.maxHP || null} : null;
  }

  // A registry overlay is neutral only if EVERY field and callback is the
  // SAME initialized dex value, not merely source-text equivalent. Equal
  // function text can hide a changed closure; object identity prevents that.
  const exact = (left, right, budget) => {
    if (--budget.n < 0) throw Error('public registry comparison budget exceeded');
    if (left === right) return true;
    if (left === null || right === null || typeof left !== typeof right) return false;
    if (typeof left !== 'object') return false; // Includes unequal functions.
    if (Object.getPrototypeOf(left) !== Object.getPrototypeOf(right)) return false;
    if (Object.getOwnPropertySymbols(left).length || Object.getOwnPropertySymbols(right).length) return false;
    const lk = Object.getOwnPropertyNames(left).sort(), rk = Object.getOwnPropertyNames(right).sort();
    if (lk.length !== rk.length || lk.some((key, i) => key !== rk[i])) return false;
    for (const key of lk) {
      const a = Object.getOwnPropertyDescriptor(left, key), b = Object.getOwnPropertyDescriptor(right, key);
      if (!a || !b || a.get || a.set || b.get || b.set ||
          a.enumerable !== b.enumerable || a.writable !== b.writable || a.configurable !== b.configurable ||
          !exact(a.value, b.value, budget)) return false;
    }
    return true;
  };
  mark('PUBLIC_CONFIGURATION_REGISTRY_REJECTED');
  let registryEquivalent = true, scopedRegistryEquivalent = true, registryCount = 0;
  // Cover every calculator move, separately audited past-only history IDs,
  // and moves whose nested conditions execute terrain and side-speed rules.
  // Field dependencies do not become selectable calculator actions.
  // Unsupported entries are not certified; each selected actor/action must
  // still pass its operation prerequisites. No absent modifier inference.
  const relevant = {
    ability: new Set(['runaway','magicguard','pressure','insomnia','levitate','prankster','stall','download','technician','multitype','fairyaura','asonespectrier','shadowshield','sandrush','sandforce','sandveil','airlock','cloudnine','chlorophyll','swiftswim','raindish','solarpower','slushrush','snowcloak','icebody']),
    heldItem: new Set(['none','heavydutyboots','choicescarf','laggingtail','powerherb','marshadiumz','leftovers','lifeorb','mewtwonitey','safetygoggles','utilityumbrella']),
    move: new Set(['tackle','swift','scratch','quickattack','extremespeed','shadowclaw','shadowsneak',
                   'psychocut','dragonclaw','flamethrower','swordsdance','nastyplot','bulkup','recover',
      'uturn','ironhead','icebeam','moonblast','focusblast','astralbarrage','psyshock','drainingkiss',
      'energyball','closecombat','rocktomb','geomancy','thunder','psystrike','fireblast','spectralthief',
      'shadowball','hypervoice','aurasphere','psychicterrain','tailwind','sandstorm','hail','snowscape','spikes','stealthrock','stickyweb','toxicspikes'])
  };
  const registryAudit = {};
  for (const [group, table, ctor] of [['ability', 'Abilities', modules.Ability],
                                       ['heldItem', 'Items', modules.Item],
                                       ['move', 'Moves', modules.DataMove],
                                       ['bagItem', null, null]]) {
    const d = Object.getOwnPropertyDescriptor(registries[group], 'contents');
    if (!d || d.get || d.set) throw Error('unverified public registry accessor');
    const entries = d.value;
    if (!(entries instanceof Map) || Object.getPrototypeOf(entries) !== Map.prototype ||
        Reflect.ownKeys(entries).length || entries.size > 2048) throw Error('unsupported public registry structure');
    track(registries[group]);trackMap(entries);
    const listed = [];
    for (const [id, value] of entries) {
      registryCount++;
      // BasicEffect uses realMove for its registry id (e.g. every typed Hidden
      // Power maps to hiddenpower), while preserving its source display name.
      // Bind to that exact pinned source definition, not whichever raw entry
      // happens to share the normalized registry key. Never invoke a getter.
      const name = value && Object.getOwnPropertyDescriptor(value, 'name');
      const normalizedId = value && Object.getOwnPropertyDescriptor(value, 'id');
      const sourceId = group === 'move' && name && !name.get && !name.set && typeof name.value === 'string'
        ? name.value.toLowerCase().replace(/[^a-z0-9]+/g, '') : id;
      const sourceTable = table && Object.getOwnPropertyDescriptor(data, table);
      const definition = sourceTable && !sourceTable.get && !sourceTable.set && sourceTable.value &&
        Object.getOwnPropertyDescriptor(sourceTable.value, sourceId);
      if (definition && !definition.get && !definition.set && relevant[group]?.has(id)) canonical(definition.value);
      const native = nativeFactories && Object.getOwnPropertyDescriptor(nativeFactories, group);
      const nativeMap = native && !native.get && !native.set ? native.value : null;
      const nativeMatch = ['bagItem','heldItem'].includes(group) && nativeMap instanceof Map && Object.getPrototypeOf(nativeMap) === Map.prototype &&
        !Reflect.ownKeys(nativeMap).length && nativeMap.has(id) && exact(value, nativeMap.get(id), {n: 20000});
      const canonicalMatch = normalizedId && !normalizedId.get && !normalizedId.set && normalizedId.value === id &&
          table && typeof ctor === 'function' && definition && !definition.get && !definition.set &&
          (!sourceAnchor || sourceAnchor.entryMatches(table,sourceId,definition.value)) &&
          // Registry.register() installs a normalized Effect object. Verify
          // against an independently constructed standard effect from the
          // pinned engine class, not against unnormalized dex JSON.
          // A warmed dex deep-freezes shared nested source fields. A genuine
          // JSON registration instead has mutable plain nested fields. Verify
          // either exact source representation or its actual pinned execution
          // deepClone, which preserves callbacks by identity; never relax a
          // descriptor comparison or synthesize/re-evaluate a function.
          (exact(value, new ctor({name: sourceId, ...definition.value}), {n: 20000}) ||
            dex.deepClone === modules.Utils.deepClone &&
            exact(value, new ctor({name: sourceId, ...modules.Utils.deepClone(definition.value)}), {n: 20000}));
      const verified = typeof id === 'string' && /^[a-z0-9]+$/.test(id) && (nativeMatch || canonicalMatch);
      if (!verified) registryEquivalent = false;
      if (relevant[group]?.has(id) && !verified) scopedRegistryEquivalent = false;
      // Entry announcements are identity evidence. Unknown item/ability code
      // could forge Pressure or item-removal messages, even under another ID.
      // Only an unused move can remain outside the dispatched action slice.
      // Data-only entries cannot emit a message; arbitrary callbacks cannot be
      // cleared by reading their source text or assuming they were inactive.
      const callable = (x, budget, root = false) => {
        if (--budget.n < 0) throw Error('public callback budget exceeded');
        if (typeof x === 'function') return true;
        if (!x || typeof x !== 'object') return false;
        const proto=Object.getPrototypeOf(x);
        if (root ? proto !== ctor?.prototype : proto !== null && proto !== Object.prototype && proto !== Array.prototype) return true;
        return Object.getOwnPropertyNames(x).some(k => {
          const d=Object.getOwnPropertyDescriptor(x,k);
          return !d || d.get || d.set || k.startsWith('on') || callable(d.value,budget);
        });
      };
      if (!verified && (group === 'ability' || group === 'heldItem' || group === 'bagItem') && callable(value,{n:20000},true))
        scopedRegistryEquivalent = false;
      listed.push([id, canonical(value, false)]);
    }
    registryAudit[group] = listed.sort(([a], [b]) => String(a).localeCompare(String(b)));
  }
  // These exact public normalization/lookup functions are part of the receipt,
  // so a cached-domain lease can recheck them without enumerating actor state.
  mark('PUBLIC_CONFIGURATION_METHODS_REJECTED');
  registryAudit.implementation = {
    constructors: tagged('REGISTRY_CONSTRUCTORS',()=>canonical({DataMove: modules.DataMove, Ability: modules.Ability,
      Item: modules.Item, BasicEffect: modules.BasicEffect, Registries: modules.Registries})),
    registries: Object.fromEntries(Object.entries(modules.Registries).map(([name, ctor]) =>
      [name, methods(null, ctor.prototype)])),
    effectiveRegistries: Object.fromEntries(['ability','heldItem','move','bagItem','species'].map(group =>
      [group, {...methods(registries[group], Object.getPrototypeOf(registries[group])),
               ...methods(registries[group], modules.Registries.CobbleRegistry.prototype)}])),
    basicEffect: methods(null, modules.BasicEffect.prototype),
    normalizationHelpers: tagged('NORMALIZATION_HELPERS',()=>canonical({deepClone:dex.deepClone,deepFreeze:dex.deepFreeze}))
  };
  const registryReceipt = tagged('REGISTRY_SERIALIZATION',()=>JSON.stringify(canonical(registryAudit, false, false)));
  if (registryOnly === true) return JSON.stringify({registryEquivalent, scopedRegistryEquivalent, registryCount, registryReceipt});
  if(progress)progress.methodGroup='ENGINE_HELPERS';
  if (sourceAnchor) for (const key of ['toID','trunc','clampIntRange'])
    if (!sourceAnchor.hasHelper(key,battle[key])) throw Error('unverified engine helper position');
  const core = {
    constructors: tagged('ENGINE_CONSTRUCTORS',()=>canonical({Battle: modules.Battle, Pokemon: modules.Pokemon, Side: modules.Side,
      BattleActions: modules.BattleActions, BattleQueue: modules.BattleQueue, Field: modules.Field})),
    registryImplementation: registryAudit.implementation,
    helpers: tagged('ENGINE_HELPERS',()=>canonical({toID: battle.toID, trunc: battle.trunc, clampIntRange: battle.clampIntRange})),
    battle: methods(battle, modules.Battle.prototype, 'BATTLE'), actions: methods(battle.actions, modules.BattleActions.prototype, 'ACTIONS'),
    queue: methods(battle.queue, modules.BattleQueue.prototype, 'QUEUE'), field: methods(battle.field, modules.Field.prototype, 'FIELD'),
    pokemon: methods(null, modules.Pokemon.prototype, 'POKEMON'), side: methods(null, modules.Side.prototype, 'SIDE'), dex: methods(dex, Object.getPrototypeOf(dex), 'DEX'),
    moves: methods(dex.moves, Object.getPrototypeOf(dex.moves), 'DEX_MOVES'), items: methods(dex.items, Object.getPrototypeOf(dex.items), 'DEX_ITEMS'),
    abilities: methods(dex.abilities, Object.getPrototypeOf(dex.abilities), 'DEX_ABILITIES'),
    species: methods(dex.species, Object.getPrototypeOf(dex.species), 'DEX_SPECIES'),
    conditions: methods(dex.conditions, Object.getPrototypeOf(dex.conditions), 'DEX_CONDITIONS'),
    formats: methods(dex.formats, Object.getPrototypeOf(dex.formats), 'DEX_FORMATS')
  };
  const tables = {};
  for (const key of ['Scripts','Moves','Abilities','Items','Conditions','TypeChart','Natures','Formats']) tables[key] = data[key];
  // Source-supported operations never dispatch an arbitrary ID. Unknown
  // inactive definitions are not a certificate and cannot be selected by Java.
  // Scripts/type rules remain global: they can change any otherwise known action.
  const select = (table, ids) => Object.fromEntries([...ids].sort().map(id => [id, table[id]]));
  const operationTables = {
    Scripts: data.Scripts, TypeChart: data.TypeChart, Natures: data.Natures, Formats: data.Formats,
    Moves: select(data.Moves, relevant.move),
    // Entry and condition callbacks can forge public identity announcements even
    // when no supported action selects them. Their complete public definitions
    // and aliases must therefore remain source-pinned, unlike unused moves.
    Abilities: data.Abilities, Items: data.Items, Conditions: data.Conditions, Aliases: data.Aliases
  };
  mark('PUBLIC_CONFIGURATION_CACHE_REJECTED');
  if (sourceAnchor) {
    const cacheSeen=new Set();
    const guardCacheValue=value=>{
      if(!value || typeof value!=='object' || cacheSeen.has(value)) return;
      cacheSeen.add(value);track(value);
      for(const key of Object.getOwnPropertyNames(value)) {
        const d=Object.getOwnPropertyDescriptor(value,key);
        if(d.get || d.set) throw Error('unverified public cache accessor');
        guardCacheValue(d.value);
      }
    };
    mark('PUBLIC_CONFIGURATION_TABLES_REJECTED');
    sourceAnchor.checkTables(operationTables);
    mark('PUBLIC_CONFIGURATION_CACHE_REJECTED');
    sourceAnchor.checkCaches(dex);
    for(const [group,key] of [['moves','moveCache'],['abilities','abilityCache'],['items','itemCache'],
        ['species','speciesCache'],['conditions','conditionCache']]) {
      track(dex[group]);trackMap(dex[group][key]);
      for(const value of dex[group][key].values())
        for(let p=Object.getPrototypeOf(value);p && p!==Object.prototype;p=Object.getPrototypeOf(p)) track(p);
      // Recursing records mutable nested values; frozen native fields require
      // no repeated descriptor walk, and cannot acquire a new getter/hook.
      for(const value of dex[group][key].values()) guardCacheValue(value);
    }
  }
  // Species numbers belong to the separate catalog scope; all source hooks are
  // included even for species that have never appeared in this battle.
  mark('PUBLIC_CONFIGURATION_SERIALIZATION_REJECTED');
  tables.speciesHooks = Object.fromEntries(Object.keys(data.Pokedex).sort().map(id => [id, hooks(data.Pokedex[id])]));
  if (registryOnly === 'identity') return {
    core, operationTables: canonical(operationTables), catalog: canonical(catalog),
    speciesHooks: canonical(speciesHooks), registry: registryAudit,
    format: canonical(battle.format), rules: Array.from(battle.ruleTable.keys()).sort(),
    extraEvents: battle.events !== null, mod: dex.currentMod, gen: battle.gen, gameType: battle.gameType,
    scopedRegistryEquivalent
  };
  const json = JSON.stringify({
    mod: dex.currentMod, gen: battle.gen, gameType: battle.gameType,
    format: canonical(battle.format), rules: Array.from(battle.ruleTable.keys()).sort(),
    // onEvent is a format-level extension API. Only its presence is inspected.
    extraEvents: battle.events !== null,
    overrides: registryCount !== 0,
    registryEquivalent, scopedRegistryEquivalent, registryCount,
    registryReceipt,
    components: {format: JSON.stringify(canonical([battle.format, Array.from(battle.ruleTable.keys()).sort()])),
                 core: JSON.stringify(canonical(core)), ...(registryOnly === 'operations' ? {} : {tables: JSON.stringify(canonical(tables, false, false))}),
                 operationTables: JSON.stringify(canonical(operationTables)),
                 speciesHooks: JSON.stringify(canonical(speciesHooks)), catalog: JSON.stringify(canonical(catalog))}
  });
  if (!captureLease) return json;
  // Guard named PUBLIC association/configuration slots without enumerating a
  // battle, side, Pokemon, request, event handler list or private payload.
  const bindings = [
    [battle,'dex'],[battle,'actions'],[battle,'queue'],[battle,'field'],[battle,'format'],[battle,'ruleTable'],
    [battle,'events'],[battle,'gen'],[battle,'gameType'],[battle,'toID'],[battle,'trunc'],[battle,'clampIntRange'],
    [dex,'dataCache'],[dex,'currentMod'],[modules.Cobblemon,'registries'],
    [modules,'Side'],
    [battle.actions,'battle'],[battle.actions,'dex'],[battle.field,'battle'],[battle.queue,'battle'],
    ...['moves','items','abilities','species','conditions','formats'].flatMap(k=>[[dex,k],[dex[k],'dex']]),
    ...['ability','heldItem','move','bagItem','species'].map(k=>[registries,k]),
    ...['Scripts','Moves','Abilities','Items','Conditions','TypeChart','Natures','Formats','Aliases','Pokedex'].map(k=>[data,k])
  ].map(([object,key])=>({object,key,value:object[key]}));
  trackMap(battle.ruleTable);
  // Replacing a selected table entry must withdraw a receipt even when its
  // old object remains alive elsewhere. Unknown entries never gain a scope.
  for(const [table,ids] of [['Moves',relevant.move],['Abilities',relevant.ability],['Items',relevant.heldItem],
      ['Conditions',new Set(Object.keys(operationTables.Conditions))]])
    for(const id of ids) bindings.push({object:data[table],key:id,value:data[table][id]});
  let expired=false;
  const current=()=>{
    if(expired)return false;
    try {
      for(const b of bindings) {
        const d=Object.getOwnPropertyDescriptor(b.object,b.key);
        // Some absent optional source tables intentionally read undefined.
        if(d ? d.get || d.set || !Object.is(d.value,b.value) : b.value!==undefined) {expired=true;return false;}
      }
      for(const {value,proto,names,descriptors} of objects) {
        const fresh=Object.getOwnPropertyNames(value).sort();
        if(Object.getPrototypeOf(value)!==proto || Object.getOwnPropertySymbols(value).length || fresh.length!==names.length
            || fresh.some((k,i)=>k!==names[i])) {expired=true;return false;}
        for(let i=0;i<names.length;i++) {
          const d=Object.getOwnPropertyDescriptor(value,names[i]);
          const before=descriptors[i];
          if(!d || d.get || d.set || !Object.is(d.value,before.value) || d.enumerable!==before.enumerable
              || d.configurable!==before.configurable || d.writable!==before.writable) {expired=true;return false;}
        }
      }
      for(const {value,proto,entries} of maps) {
        if(!(value instanceof Map) || Object.getPrototypeOf(value)!==proto || value.size!==entries.length
            || entries.some(([k,v])=>!value.has(k)||!Object.is(value.get(k),v))) {expired=true;return false;}
      }
      for(const {value,proto,keys,values} of hookObjects) {
        if(Object.getPrototypeOf(value)!==proto){expired=true;return false;}
        const fresh=Object.keys(value).filter(k=>{const d=Object.getOwnPropertyDescriptor(value,k);if(d?.get||d?.set){expired=true;return false;}return k.startsWith('on')||typeof d?.value==='function';}).sort();
        if(expired||fresh.length!==keys.length||fresh.some((k,i)=>k!==keys[i] || Object.getOwnPropertyDescriptor(value,k).value!==values[k])){expired=true;return false;}
      }
      for(const {instance,name,actual} of methodsToCheck) {
        let owner=instance,d;
        while(owner&&!d){d=Object.getOwnPropertyDescriptor(owner,name);owner=Object.getPrototypeOf(owner);}
        if(!d||d.get||d.set||d.value!==actual){expired=true;return false;}
      }
      return true;
    }catch(unavailable){expired=true;return false;}
  };
  return Object.freeze({json,current});
})
