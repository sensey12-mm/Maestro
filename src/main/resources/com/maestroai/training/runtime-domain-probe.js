// Read only PUBLIC simulator configuration. Never enumerate a battle, side, team,
// Pokemon, request, log, event handlers or a private payload. Caller supplies an
// already initialized battle; this function never starts one or changes a rule.
(function (battle, modules, registryOnly) {
  if (!battle || !battle.dex || !battle.dex.dataCache) throw Error('domain not initialized');
  const canonical = value => {
    // Graal includes a trailing newline for one bundled arrow expression;
    // normalize only OUTER whitespace, preserving every character of its body.
    if (typeof value === 'function') return {functionSource: Function.prototype.toString.call(value).trim()};
    if (value === undefined) return null;
    if (value === null || typeof value !== 'object') return value;
    if (Object.getOwnPropertySymbols(value).length) throw Error('uninspectable public configuration symbol');
    const result = Array.isArray(value) ? [] : Object.create(null);
    for (const key of Object.getOwnPropertyNames(value).sort()) {
      const descriptor = Object.getOwnPropertyDescriptor(value, key);
      // Public configuration accessors are unverified code, not values to call.
      if (!descriptor || descriptor.get || descriptor.set) throw Error('unverified configuration accessor');
      result[key] = canonical(descriptor.value);
    }
    return result;
  };
  // Only method descriptors: getters are never invoked and instance state is
  // never enumerated. Resolve each public engine method's actual override.
  const methods = (instance, prototype) => {
    const result = {};
    for (const name of Object.getOwnPropertyNames(prototype).sort()) {
      const descriptor = Object.getOwnPropertyDescriptor(prototype, name);
      if (name !== 'constructor' && typeof descriptor.value === 'function') {
        let actual = descriptor.value;
        if (instance) {
          let owner = instance, effective;
          while (owner && !effective) { effective = Object.getOwnPropertyDescriptor(owner, name); owner = Object.getPrototypeOf(owner); }
          if (!effective || effective.get || effective.set) throw Error('unverified engine accessor');
          actual = effective.value;
        }
        result[name] = canonical(actual);
      }
    }
    return result;
  };
  const dex = battle.dex, data = dex.dataCache, registries = modules.Cobblemon.registries;
  const hooks = value => {
    const result = Object.create(null);
    for (const key of Object.keys(value || {}).sort()) {
      const d = Object.getOwnPropertyDescriptor(value, key);
      if (!d || d.get || d.set) throw Error('unverified public hook accessor');
      if (key.startsWith('on') || typeof d.value === 'function') result[key] = canonical(d.value);
    }
    return result;
  };
  const speciesHooks = {};
  for (const [id, species] of registries.species.contents) {
    const h = hooks(species); if (Object.keys(h).length) speciesHooks[id] = h;
  }
  const catalog = {};
  for (const id of ['zoroark','gholdengo','ditto','mew','pikachu','blissey','shedinja','shuckle',
                    'genesect','calyrexshadow','arceus','xerneas','marshadow']) {
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
  let registryEquivalent = true, registryCount = 0;
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
      if (definition && !definition.get && !definition.set) canonical(definition.value);
      if (typeof id !== 'string' || !/^[a-z0-9]+$/.test(id) ||
          !normalizedId || normalizedId.get || normalizedId.set || normalizedId.value !== id ||
          !table || typeof ctor !== 'function' || !definition || definition.get || definition.set ||
          // Registry.register() installs a normalized Effect object. Verify
          // against an independently constructed standard effect from the
          // pinned engine class, not against unnormalized dex JSON.
          // A warmed dex deep-freezes shared nested source fields. A genuine
          // JSON registration instead has mutable plain nested fields. Verify
          // either exact source representation or its actual pinned execution
          // deepClone, which preserves callbacks by identity; never relax a
          // descriptor comparison or synthesize/re-evaluate a function.
          !(exact(value, new ctor({name: sourceId, ...definition.value}), {n: 20000}) ||
            dex.deepClone === modules.Utils.deepClone &&
            exact(value, new ctor({name: sourceId, ...modules.Utils.deepClone(definition.value)}), {n: 20000}))) registryEquivalent = false;
      listed.push([id, canonical(value)]);
    }
    registryAudit[group] = listed.sort(([a], [b]) => String(a).localeCompare(String(b)));
  }
  // These exact public normalization/lookup functions are part of the receipt,
  // so a cached-domain lease can recheck them without enumerating actor state.
  registryAudit.implementation = {
    constructors: canonical({DataMove: modules.DataMove, Ability: modules.Ability,
      Item: modules.Item, BasicEffect: modules.BasicEffect, Registries: modules.Registries}),
    registries: Object.fromEntries(Object.entries(modules.Registries).map(([name, ctor]) =>
      [name, methods(null, ctor.prototype)])),
    effectiveRegistries: Object.fromEntries(['ability','heldItem','move','bagItem','species'].map(group =>
      [group, {...methods(registries[group], Object.getPrototypeOf(registries[group])),
               ...methods(registries[group], modules.Registries.CobbleRegistry.prototype)}])),
    basicEffect: methods(null, modules.BasicEffect.prototype),
    normalizationHelpers: canonical({deepClone:dex.deepClone,deepFreeze:dex.deepFreeze})
  };
  const registryReceipt = JSON.stringify(canonical(registryAudit));
  if (registryOnly) return JSON.stringify({registryEquivalent, registryCount, registryReceipt});
  const core = {
    constructors: canonical({Battle: modules.Battle, Pokemon: modules.Pokemon,
      BattleActions: modules.BattleActions, BattleQueue: modules.BattleQueue, Field: modules.Field}),
    registryImplementation: registryAudit.implementation,
    helpers: canonical({toID: battle.toID, trunc: battle.trunc, clampIntRange: battle.clampIntRange}),
    battle: methods(battle, modules.Battle.prototype), actions: methods(battle.actions, modules.BattleActions.prototype),
    queue: methods(battle.queue, modules.BattleQueue.prototype), field: methods(battle.field, modules.Field.prototype),
    pokemon: methods(null, modules.Pokemon.prototype), dex: methods(dex, Object.getPrototypeOf(dex)),
    moves: methods(dex.moves, Object.getPrototypeOf(dex.moves)), items: methods(dex.items, Object.getPrototypeOf(dex.items)),
    abilities: methods(dex.abilities, Object.getPrototypeOf(dex.abilities)),
    species: methods(dex.species, Object.getPrototypeOf(dex.species)),
    conditions: methods(dex.conditions, Object.getPrototypeOf(dex.conditions)),
    formats: methods(dex.formats, Object.getPrototypeOf(dex.formats))
  };
  const tables = {};
  for (const key of ['Scripts','Moves','Abilities','Items','Conditions','TypeChart','Natures','Formats']) tables[key] = data[key];
  // Species numbers belong to the separate catalog scope; all source hooks are
  // included even for species that have never appeared in this battle.
  tables.speciesHooks = Object.fromEntries(Object.keys(data.Pokedex).sort().map(id => [id, hooks(data.Pokedex[id])]));
  return JSON.stringify({
    mod: dex.currentMod, gen: battle.gen, gameType: battle.gameType,
    format: canonical(battle.format), rules: Array.from(battle.ruleTable.keys()).sort(),
    // onEvent is a format-level extension API. Only its presence is inspected.
    extraEvents: battle.events !== null,
    overrides: registryCount !== 0,
    registryEquivalent, registryCount,
    registryReceipt,
    components: {format: JSON.stringify(canonical([battle.format, Array.from(battle.ruleTable.keys()).sort()])),
                 core: JSON.stringify(canonical(core)), tables: JSON.stringify(canonical(tables)),
                 speciesHooks: JSON.stringify(canonical(speciesHooks)), catalog: JSON.stringify(canonical(catalog))}
  });
})
