'use strict';

// Execute the real pinned registry/constructors and production probe. No battle
// actors or requests are needed to test public move configuration provenance.
module.exports = function (modules, anchor, probe, battle, selectable) {
  let checks = 0, privateReads = 0, callbackCalls = 0;
  const check = (ok, why) => { checks++; if (!ok) throw Error(why); };
  const capture = () => probe(battle, modules, false, null, anchor, true);
  const registry = modules.Cobblemon.registries.move;
  const table = battle.dex.dataCache.Moves;
  const initial = capture(), original = JSON.parse(initial.json);
  const covered = JSON.parse(original.components.operationTables).Moves;
  for (const id of selectable) {
    check(Object.hasOwn(covered, id), 'Selectable move missing from attestation: ' + id);
    check(!table[id].flags?.powder,'Selectable powder move requires separate Goggles hit protection coverage: '+id);
  }
  const fieldDependencies=['psychicterrain','tailwind','sandstorm'];
  for(const id of fieldDependencies){
    check(Object.hasOwn(covered,id),'Active field definition missing from attestation: '+id);
    check(!selectable.includes(id),'Field dependency became a selectable calculator action: '+id);
  }

  // Trap the actual private surfaces. Restore descriptors for the remaining
  // integration fixtures, which may later create their own synthetic actors.
  const privateKeys = ['p1', 'p2', 'sides', 'log', 'inputLog', 'activePokemon', 'activeTarget'];
  const descriptors = privateKeys.map(key => Object.getOwnPropertyDescriptor(battle, key));
  for (const key of privateKeys) Object.defineProperty(battle, key, {configurable:true,
    get() { privateReads++; throw Error('private actor state read'); }});
  const badRegistries = [];
  const restored = ['geomancy', 'thunder', 'psystrike', 'fireblast', 'spectralthief',...fieldDependencies];
  try {
    // Every calculator move must accept genuine native normalization. Warmed
    // native definitions may have frozen nested fields; use the pinned clone.
    for (const id of selectable) {
      const native = registry.register({name:id, ...modules.Utils.deepClone(table[id])}, id);
      check(native instanceof modules.DataMove && native.id === id, 'Native move normalization changed: ' + id);
    }
    let receipt = capture();
    check(JSON.parse(receipt.json).registryEquivalent, 'Genuine selected move registrations rejected');
    check(receipt.current(), 'Genuine selected move lease unavailable');
    registry.reset();
    check(!receipt.current(), 'Registry reset retained selected move lease');
    check(capture().current(), 'Fresh receipt unavailable after native registry reset');

    for (const id of restored) {
      const definition = table[id];
      const native = registry.register({name:id, ...modules.Utils.deepClone(definition)}, id);
      receipt = capture();
      check(JSON.parse(receipt.json).registryEquivalent && receipt.current(), 'Original move rejected: ' + id);

      registry.register({name:id, ...modules.Utils.deepClone(definition), basePower:definition.basePower + 1}, id);
      const altered = JSON.parse(capture().json);
      check(!altered.registryEquivalent && !altered.scopedRegistryEquivalent, 'Altered selected move retained scope: ' + id);
      badRegistries.push({id, registryEquivalent:altered.registryEquivalent, scopedRegistryEquivalent:altered.scopedRegistryEquivalent});
      check(!receipt.current(), 'Altered registry retained old lease: ' + id);
      registry.contents.set(id, native);
      check(capture().current() && !receipt.current(), 'Restoration revived old registry lease: ' + id);

      registry.register({name:id, ...modules.Utils.deepClone(definition), onHit() { callbackCalls++; }}, id);
      check(!JSON.parse(capture().json).scopedRegistryEquivalent, 'Foreign selected-move callback accepted: ' + id);
      registry.reset();

      // Source metadata is independently fingerprinted. Replacing a table row
      // must expire its lease even when the old source object remains alive.
      receipt = capture();
      table[id] = {...definition, basePower:definition.basePower + 1};
      check(!receipt.current(), 'Selected source replacement retained lease: ' + id);
      check(JSON.parse(capture().json).components.operationTables !== original.components.operationTables,
        'Selected source metadata omitted from fingerprint: ' + id);
      table[id] = definition;
      check(capture().current() && !receipt.current(), 'Source restoration revived old lease: ' + id);

      // Test a foreign closure with identical function text in the actual
      // callback position whenever the native definition has one.
      const slot = Object.keys(definition).find(key => typeof definition[key] === 'function');
      if (slot) {
        const foreign = eval('({' + definition[slot].toString() + '})')[slot];
        check(foreign !== definition[slot] && foreign.toString() === definition[slot].toString(),
          'Same-text foreign callback fixture invalid: ' + id);
        table[id] = {...definition, [slot]:foreign};
        let rejected = false;
        try { capture(); } catch (unverified) { rejected = true; }
        check(rejected, 'Same-text source callback gained provenance: ' + id);
        table[id] = definition;
      }
    }

    // The field's nested callbacks execute even though its setter is not a
    // selectable calculator move. Exact text cannot confer callback identity.
    for(const id of fieldDependencies){
      const definition=table[id];
      if(!definition.condition)continue; // Sandstorm uses the global weather condition below.
      for(const slot of Object.keys(definition.condition).filter(k=>typeof definition.condition[k]==='function')){
        const native=registry.register({name:id,...modules.Utils.deepClone(definition)},id);
        receipt=capture();check(receipt.current(),'Native field condition receipt missing: '+slot);
        const altered=modules.Utils.deepClone(definition);
        altered.condition[slot]=function(){callbackCalls++;};
        registry.register({name:id,...altered},id);
        const bad=JSON.parse(capture().json);
        check(!bad.scopedRegistryEquivalent,'Foreign field registry callback accepted: '+slot);
        badRegistries.push({id:id+'.condition.'+slot,registryEquivalent:bad.registryEquivalent,scopedRegistryEquivalent:bad.scopedRegistryEquivalent});
        check(!receipt.current(),'Foreign field callback retained prior lease: '+slot);
        registry.contents.set(id,native);
        check(capture().current()&&!receipt.current(),'Field callback restoration revived old lease: '+slot);
        const foreign=eval('({'+definition.condition[slot].toString()+'})')[slot];
        check(foreign!==definition.condition[slot]&&foreign.toString()===definition.condition[slot].toString(),'Invalid nested same-text callback fixture');
        receipt=capture();table[id]={...definition,condition:{...definition.condition,[slot]:foreign}};
        let rejected=false;try{capture();}catch(unverified){rejected=true;}
        check(rejected&&!receipt.current(),'Same-text foreign field source callback retained scope: '+slot);
        table[id]=definition;
        check(capture().current()&&!receipt.current(),'Source field callback restoration revived old lease: '+slot);
        registry.reset();
      }
      receipt=capture();
      table[id]={...definition,condition:{...definition.condition,duration:definition.condition.duration+1}};
      check(!receipt.current(),'Altered field duration retained its lease');
      check(JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Field lifecycle omitted from operation fingerprint');
      table[id]=definition;
      check(capture().current()&&!receipt.current(),'Field lifecycle restoration revived old lease');
    }

    {
      const conditions=battle.dex.dataCache.Conditions,definition=conditions.sandstorm;
      for(const slot of Object.keys(definition).filter(k=>typeof definition[k]==='function')){
        const foreign=eval('({'+definition[slot].toString()+'})')[slot];
        receipt=capture();conditions.sandstorm={...definition,[slot]:foreign};
        let rejected=false;try{capture();}catch(unverified){rejected=true;}
        check(foreign!==definition[slot]&&foreign.toString()===definition[slot].toString()&&rejected&&!receipt.current(),'Foreign sandstorm callback retained scope: '+slot);
        conditions.sandstorm=definition;check(capture().current()&&!receipt.current(),'Sandstorm callback restoration revived receipt: '+slot);
      }
      for(const slot of ['duration','onFieldResidualOrder','onModifySpDPriority']){
        receipt=capture();conditions.sandstorm={...definition,[slot]:definition[slot]+1};
        check(!receipt.current()&&JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Sandstorm lifecycle/order omitted: '+slot);
        conditions.sandstorm=definition;check(capture().current()&&!receipt.current(),'Sandstorm lifecycle restoration revived receipt: '+slot);
      }
      const setter=table.sandstorm;
      receipt=capture();table.sandstorm={...setter,weather:'RainDance'};
      check(!receipt.current()&&JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Sandstorm setter weather omitted from fingerprint');
      table.sandstorm=setter;
      check(capture().current()&&!receipt.current(),'Sandstorm setter restoration revived receipt');
    }

    // Status callbacks execute outside move definitions. Full public condition
    // attestation must reject identical-text closures without reading actors.
    const conditions=battle.dex.dataCache.Conditions;
    for(const id of ['par','slp','frz','brn','psn','tox']){
      const definition=conditions[id];
      for(const slot of Object.keys(definition).filter(k=>typeof definition[k]==='function')){
        receipt=capture();check(receipt.current(),'Native status receipt missing: '+id+'.'+slot);
        const foreign=eval('({'+definition[slot].toString()+'})')[slot];
        check(foreign!==definition[slot]&&foreign.toString()===definition[slot].toString(),'Invalid status foreign callback fixture');
        conditions[id]={...definition,[slot]:foreign};
        let rejected=false;try{capture();}catch(unverified){rejected=true;}
        check(rejected&&!receipt.current(),'Foreign status callback retained operation scope: '+id+'.'+slot);
        conditions[id]=definition;
        check(capture().current()&&!receipt.current(),'Status restoration revived old receipt: '+id+'.'+slot);
      }
      const order=['brn','psn','tox'].includes(id)?'onResidualOrder':'onBeforeMovePriority';
      receipt=capture();conditions[id]={...definition,[order]:definition[order]+1};
      check(!receipt.current(),'Status callback priority alteration retained receipt: '+id);
      check(JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Status priority omitted from fingerprint: '+id);
      conditions[id]=definition;
      check(capture().current()&&!receipt.current(),'Status priority restoration revived old receipt: '+id);
    }

    for(const [tableName,id,slot,metadata] of [['Items','leftovers','onResidual','onResidualSubOrder'],['Abilities','magicguard','onDamage','rating'],
        ['Abilities','sandforce','onBasePower','onBasePowerPriority'],['Abilities','sandrush','onModifySpe','rating'],
        ['Abilities','raindish','onWeather','rating'],['Abilities','solarpower','onModifySpA','onModifySpAPriority'],
        ['Abilities','chlorophyll','onModifySpe','rating'],['Abilities','swiftswim','onModifySpe','rating'],
        ['Abilities','sandveil','onModifyAccuracy','onModifyAccuracyPriority'],['Items','safetygoggles','onImmunity','num'],
        ['Items','utilityumbrella','onStart','num'],
        ['Abilities','airlock','onStart','suppressWeather'],['Abilities','cloudnine','onStart','suppressWeather']]){
      const effects=battle.dex.dataCache[tableName],definition=effects[id];
      receipt=capture();effects[id]={...definition,[metadata]:typeof definition[metadata]==='boolean'?!definition[metadata]:definition[metadata]+1};
      check(!receipt.current()&&JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Residual metadata omitted: '+id);
      effects[id]=definition;check(capture().current()&&!receipt.current(),'Residual metadata restoration revived receipt: '+id);
      const foreign=eval('({'+definition[slot].toString()+'})')[slot];
      receipt=capture();effects[id]={...definition,[slot]:foreign};
      let rejected=false;try{capture();}catch(unverified){rejected=true;}
      check(foreign!==definition[slot]&&foreign.toString()===definition[slot].toString()&&rejected&&!receipt.current(),'Foreign residual callback retained scope: '+id);
      effects[id]=definition;check(capture().current()&&!receipt.current(),'Residual callback restoration revived receipt: '+id);
    }
    for(const [group,tableName,id,metadata] of [['ability','Abilities','sandrush','rating'],['ability','Abilities','sandforce','rating'],
        ['ability','Abilities','raindish','rating'],['ability','Abilities','solarpower','onModifySpAPriority'],
        ['ability','Abilities','chlorophyll','rating'],['ability','Abilities','swiftswim','rating'],
        ['ability','Abilities','sandveil','onModifyAccuracyPriority'],['heldItem','Items','safetygoggles','num'],
        ['heldItem','Items','utilityumbrella','num'],
        ['ability','Abilities','airlock','suppressWeather'],['ability','Abilities','cloudnine','suppressWeather']]){
      const registryEffect=modules.Cobblemon.registries[group],definition=battle.dex.dataCache[tableName][id];
      const native=registryEffect.register({name:id,...modules.Utils.deepClone(definition)},id);
      receipt=capture();check(receipt.current()&&JSON.parse(receipt.json).scopedRegistryEquivalent,'Native selected weather effect registry rejected: '+id);
      registryEffect.register({name:id,...modules.Utils.deepClone(definition),[metadata]:typeof definition[metadata]==='boolean'?!definition[metadata]:definition[metadata]+1},id);
      const altered=JSON.parse(capture().json);
      check(!receipt.current()&&!altered.scopedRegistryEquivalent,'Altered selected weather effect registry retained scope: '+id);
      badRegistries.push({id:id+'.'+metadata,registryEquivalent:altered.registryEquivalent,scopedRegistryEquivalent:altered.scopedRegistryEquivalent});
      registryEffect.contents.set(id,native);check(capture().current()&&!receipt.current(),'Sand ability registry restoration revived receipt: '+id);
      for(const slot of Object.keys(definition).filter(k=>typeof definition[k]==='function')){
        const foreign=eval('({'+definition[slot].toString()+'})')[slot];
        receipt=capture();registryEffect.register({name:id,...modules.Utils.deepClone(definition),[slot]:foreign},id);
        const bad=JSON.parse(capture().json);
        check(!receipt.current()&&!bad.scopedRegistryEquivalent,'Foreign selected weather effect registry callback retained scope: '+id+'.'+slot);
        badRegistries.push({id:id+'.'+slot,registryEquivalent:bad.registryEquivalent,scopedRegistryEquivalent:bad.scopedRegistryEquivalent});
        registryEffect.contents.set(id,native);check(capture().current()&&!receipt.current(),'Sand ability callback restoration revived receipt: '+id+'.'+slot);
        receipt=capture();battle.dex.dataCache[tableName][id]={...definition,[slot]:foreign};
        let rejected=false;try{capture();}catch(unverified){rejected=true;}
        check(rejected&&!receipt.current(),'Foreign selected weather effect source callback retained scope: '+id+'.'+slot);
        battle.dex.dataCache[tableName][id]=definition;
        check(capture().current()&&!receipt.current(),'Sand ability source restoration revived receipt: '+id+'.'+slot);
      }
      receipt=capture();registryEffect.reset();check(!receipt.current()&&capture().current(),'Sand ability registry reset retained receipt: '+id);
    }
    for(const slot of ['residualEvent','faintMessages']){
      const prototype=modules.Battle.prototype,method=prototype[slot],foreign=eval('({'+method.toString()+'})')[slot];
      receipt=capture();prototype[slot]=foreign;
      let rejected=false;try{capture();}catch(unverified){rejected=true;}
      check(method!==foreign&&method.toString()===foreign.toString()&&rejected&&!receipt.current(),'Foreign residual/faint dispatch retained scope: '+slot);
      prototype[slot]=method;check(capture().current()&&!receipt.current(),'Residual/faint dispatch restoration revived receipt: '+slot);
    }
    for(const slot of ['effectiveWeather','suppressingWeather','isWeather']){
      const prototype=modules.Field.prototype,method=prototype[slot],foreign=eval('({'+method.toString()+'})')[slot];
      receipt=capture();prototype[slot]=foreign;let rejected=false;try{capture();}catch(unverified){rejected=true;}
      check(method!==foreign&&method.toString()===foreign.toString()&&rejected&&!receipt.current(),'Foreign effective-weather dispatch retained scope: '+slot);
      prototype[slot]=method;check(capture().current()&&!receipt.current(),'Effective-weather dispatch restoration revived receipt: '+slot);
    }
    for(const slot of ['effectiveWeather','hasItem','ignoringItem']){
      const prototype=modules.Pokemon.prototype,method=prototype[slot],foreign=eval('({'+method.toString()+'})')[slot];
      receipt=capture();prototype[slot]=foreign;let rejected=false;try{capture();}catch(unverified){rejected=true;}
      check(method!==foreign&&method.toString()===foreign.toString()&&rejected&&!receipt.current(),'Foreign actor weather/item dispatch retained scope: '+slot);
      prototype[slot]=method;check(capture().current()&&!receipt.current(),'Actor weather/item restoration revived receipt: '+slot);
    }

    for(const id of ['rocktomb','thunder','fireblast']){
      const definition=table[id],native=registry.register({name:id,...modules.Utils.deepClone(definition)},id);
      receipt=capture();check(receipt.current(),'Original accuracy receipt unavailable: '+id);
      registry.register({name:id,...modules.Utils.deepClone(definition),accuracy:definition.accuracy+1},id);
      const bad=JSON.parse(capture().json);
      check(!bad.registryEquivalent&&!bad.scopedRegistryEquivalent&&!receipt.current(),'Altered accuracy registry retained scope: '+id);
      badRegistries.push({id:id+'.accuracy',registryEquivalent:bad.registryEquivalent,scopedRegistryEquivalent:bad.scopedRegistryEquivalent});
      registry.contents.set(id,native);
      check(capture().current()&&!receipt.current(),'Accuracy registry restoration revived its receipt: '+id);
      receipt=capture();table[id]={...definition,accuracy:definition.accuracy+1};
      check(!receipt.current()&&JSON.parse(capture().json).components.operationTables!==original.components.operationTables,'Accuracy metadata omitted from source proof: '+id);
      table[id]=definition;
      check(capture().current()&&!receipt.current(),'Accuracy source restoration revived its receipt: '+id);
      registry.reset();
    }
    {
      const prototype=modules.BattleActions.prototype,method=prototype.hitStepAccuracy;
      receipt=capture();
      const foreign=eval('({'+method.toString()+'})').hitStepAccuracy;
      check(foreign!==method&&foreign.toString()===method.toString(),'Invalid same-text accuracy method fixture');
      prototype.hitStepAccuracy=foreign;
      let rejected=false;try{capture();}catch(unverified){rejected=true;}
      check(rejected&&!receipt.current(),'Foreign native accuracy method retained scope');
      prototype.hitStepAccuracy=method;
      check(capture().current()&&!receipt.current(),'Accuracy method restoration revived its receipt');
    }

    // An unused custom move may coexist, but receives no selected-operation
    // certificate. Its presence must not disable unrelated ordinary actions.
    table.unusedfixture = {name:'Unused Fixture', onHit() { callbackCalls++; }};
    registry.register(table.unusedfixture, 'unusedfixture');
    receipt = capture();
    const unused = JSON.parse(receipt.json);
    check(!unused.registryEquivalent && unused.scopedRegistryEquivalent && receipt.current(),
      'Unused custom move isolation changed');
    check(!Object.hasOwn(JSON.parse(unused.components.operationTables).Moves, 'unusedfixture'),
      'Unused custom move acquired operation-table coverage');
    check(unused.components.operationTables === original.components.operationTables,
      'Unused custom move changed supported operation fingerprint');
    registry.reset(); delete table.unusedfixture;
    check(!receipt.current(), 'Custom registry reset retained old receipt');
    check(capture().current(), 'Fresh supported receipt not restored');
    check(privateReads === 0 && callbackCalls === 0, 'Proof read private state or executed a move callback');
  } finally {
    registry.reset(); delete table.unusedfixture;
    privateKeys.forEach((key, i) => {
      if (descriptors[i]) Object.defineProperty(battle, key, descriptors[i]);
      else delete battle[key];
    });
  }
  return JSON.stringify({checks, selected:selectable.length, fieldDependencies:fieldDependencies.length, badRegistries});
};

if (typeof process !== 'undefined' && process.argv && require.main === module) {
  const fs = require('fs'), path = require('path'), vm = require('vm');
  const root = path.resolve(process.argv[2]);
  const res = path.resolve(__dirname, '../src/main/resources/com/maestroai/training');
  global.require = require('module').createRequire(path.join(root, 'index.js'));
  const js = source => vm.runInThisContext(source);
  js(fs.readFileSync(path.join(root, 'index.js'), 'utf8'));
  const modules = js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,
    BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,
    Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,
    BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,
    Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
  const anchor = js(fs.readFileSync(path.join(res, 'runtime-source-anchor.js'), 'utf8'))(modules);
  const probe = js(fs.readFileSync(path.join(res, 'runtime-domain-probe.js'), 'utf8'));
  const stream = js('new BS.BattleStream()');
  stream.write('>start ' + JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,
    ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
  const java = fs.readFileSync(path.join(__dirname, '../src/main/java/com/maestroai/training/CobblemonMechanics.java'), 'utf8');
  const selectable = [...java.matchAll(/Map\.entry\("([a-z0-9]+)", new Move\(/g)].map(match => match[1]);
  if (!selectable.length) throw Error('No calculator move IDs found');
  const result = JSON.parse(module.exports(modules, anchor, probe, stream.battle, selectable));
  console.log('Native selected-move attestation PASS: ' + result.selected + ' IDs + '+result.fieldDependencies+' field dependency; ' + result.checks +
    ' checks; altered definitions, foreign callbacks, irreversible leases, unused move isolation and privacy');
}
