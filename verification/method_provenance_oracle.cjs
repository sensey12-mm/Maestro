'use strict';
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]),res=path.resolve(__dirname,'../src/main/resources/com/maestroai/training');
global.require=require('node:module').createRequire(path.join(root,'index.js'));
const js=s=>vm.runInThisContext(s);
js(fs.readFileSync(path.join(root,'index.js'),'utf8'));
const modules=js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,
  BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,
  Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,
  BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,
  Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
const anchor=js(fs.readFileSync(path.join(res,'runtime-source-anchor.js'),'utf8'))(modules);
const probe=js(fs.readFileSync(path.join(res,'runtime-domain-probe.js'),'utf8'));
const audit=b=>JSON.parse(b?anchor.auditMethods(modules,b):anchor.auditMethods(modules));
assert.deepEqual(audit().failures,[]);assert.equal(audit().actualBattle,false);
const stream=js('new BS.BattleStream()');
stream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,
 ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const battle=stream.battle,proto=modules.BattleActions.prototype;
const before=Object.getOwnPropertyDescriptors(proto),original=proto.getDamage;
const capture=()=>probe(battle,modules,false,null,anchor,true,{phase:'',methodGroup:''});
const initial=capture();
assert.deepEqual(audit(battle).failures,[]);
assert.deepEqual(Object.getOwnPropertyDescriptors(proto),before,'audit changed public definitions');
assert(initial.current(),'read-only audit changed the configuration lease');
let calls=0;
proto.getDamage=vm.runInThisContext('({'+original.toString()+'}).getDamage');
assert.notEqual(proto.getDamage,original);
assert(audit().failures.includes('ACTIONS/getDamage/IDENTITY'),'same-text foreign closure missed at startup');
const progress={phase:'',methodGroup:''};
assert.throws(()=>probe(battle,modules,false,null,anchor,true,progress),/unverified engine method position/);
assert.equal(progress.phase,'PUBLIC_CONFIGURATION_METHODS_REJECTED');assert.equal(progress.methodGroup,'ACTIONS');
assert.equal(initial.current(),false);
assert(anchor.diagnosticAllowed('ACTIONS/getDamage/IDENTITY'));
assert(!anchor.diagnosticAllowed('ACTIONS/PRIVATE_NAME/IDENTITY'));
Object.defineProperty(proto,'getDamage',{configurable:true,get(){calls++;throw Error('PRIVATE_NAME');}});
assert(audit().failures.includes('ACTIONS/getDamage/ACCESSOR'));
assert.equal(calls,0,'audit invoked a method accessor');
Object.defineProperty(proto,'getDamage',before.getDamage);
proto.PRIVATE_NAME=()=>{calls++;throw Error('PRIVATE_BODY');};
let checked=audit();assert(checked.failures.includes('ACTIONS/UNLISTED_PUBLIC_SLOT/IDENTITY'));
assert(!JSON.stringify(checked).includes('PRIVATE_NAME'));assert.equal(calls,0);
delete proto.PRIVATE_NAME;
// Startup cannot assert actual battle-instance authority. The actual-method
// check separately sees an instance override and the source gate still rejects.
battle.actions.getDamage=()=>{calls++;return 0;};
assert.deepEqual(audit().failures,[]);
assert(audit(battle).failures.includes('ACTIONS/getDamage/IDENTITY'));
assert.throws(capture,/unverified engine method position/);assert.equal(calls,0);
delete battle.actions.getDamage;
const helper=battle.toID;battle.toID=()=>{calls++;return '';};
assert(audit(battle).failures.includes('HELPER/toID/IDENTITY'));battle.toID=helper;
const shadowModules={...modules,BattleActions:function PRIVATE_NAME(){calls++;}};
assert(JSON.parse(anchor.auditMethods(shadowModules)).failures.includes('CONSTRUCTOR/BattleActions/IDENTITY'));
const originalRegistry=modules.Cobblemon.registries.move;
modules.Cobblemon.registries.move=new Proxy(originalRegistry,{getOwnPropertyDescriptor(){calls++;throw Error('PRIVATE_NAME');},getPrototypeOf(){calls++;throw Error('PRIVATE_NAME');}});
assert(audit().failures.includes('REGISTRY_MOVE/UNLISTED_PUBLIC_SLOT/IDENTITY'));assert.equal(calls,0);
modules.Cobblemon.registries.move=originalRegistry;
const slots=Object.getOwnPropertyNames(proto).filter(k=>k!=='constructor'&&typeof proto[k]==='function').slice(0,12);
for(const k of slots)proto[k]=()=>0;
checked=audit();assert.equal(checked.failures.length,8);assert.equal(checked.truncated,true);
for(const k of slots)Object.defineProperty(proto,k,before[k]);
for(const k of ['p1','p2','sides','log','inputLog','activePokemon','activeTarget'])Object.defineProperty(battle,k,{get(){calls++;throw Error('PRIVATE_NAME');}});
const guarded=new Proxy(battle,{ownKeys(){throw Error('private battle enumeration');}});
assert.deepEqual(audit(guarded).failures,[]);assert.equal(calls,0);
assert.deepEqual(Object.getOwnPropertyDescriptors(proto),before);
assert.equal(capture().current(),true);
console.log('Public method provenance oracle PASS: exact/same-text/getter/added/instance/helper/constructor cases, bounded known-slot diagnostics, noninterference and privacy');
