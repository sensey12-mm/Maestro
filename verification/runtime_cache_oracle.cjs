'use strict';
// Independent native actor-start/cache oracle. No user installation or log.
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(process.argv[2]);
const resources = path.resolve(__dirname, '../src/main/resources/com/maestroai/training');
global.require = require('node:module').createRequire(path.join(root, 'index.js'));
const js = source => vm.runInThisContext(source);
js(fs.readFileSync(path.join(root, 'index.js'), 'utf8'));
const modules = js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,
  BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,
  Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,
  BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,
  Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
const anchor = js(fs.readFileSync(path.join(resources, 'runtime-source-anchor.js'), 'utf8'))(modules);
const probe = js(fs.readFileSync(path.join(resources, 'runtime-domain-probe.js'), 'utf8'));
const dex = js(`Dex.mod('cobblemon')`);
assert.equal(dex.conditions.conditionCache.has('genesect'), false);
assert.equal(dex.conditions.conditionCache.has('mewtwo'), false);
const battle = js(`new BS.BattleStream()`);
battle.write('>start ' + JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,
  ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const before = probe(battle.battle, modules, false, null, anchor, true);
const set = (species, ability, moves) => ({species,ability,moves,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
battle.battle.setPlayer('p1', {team:[set('Mewtwo','pressure',['swift','recover'])]});
battle.battle.setPlayer('p2', {team:[set('Genesect','download',['ironhead','icebeam'])]});
assert.equal(battle.battle.turn, 1);
assert.equal(dex.conditions.conditionCache.get('genesect').exists, false);
assert.equal(dex.conditions.conditionCache.get('mewtwo').exists, false);
assert.equal(before.current(), false, 'actor-start cache growth retained old lease');
const capture = () => probe(battle.battle, modules, false, null, anchor, true);
const after = capture();
assert.equal(after.current(), true);
assert.equal(after.json, before.json, 'a native negative lookup changed public operation evidence');

// Use the actual pinned native lookup as oracle; never synthesize callbacks.
const id = 'syntheticunusedcondition', cache = dex.conditions.conditionCache;
const nativeMiss = dex.conditions.getByID(id);
assert.equal(nativeMiss.exists, false);
assert.equal(after.current(), false, 'new miss revived a prior receipt');
assert.equal(capture().current(), true);
assert.equal(capture().json,after.json,'unselected nonexistent-condition identity leaked into operation evidence');
let getterCalls = 0;
const mutations = [
  v => Object.assign(v,{onStart(){throw Error('NEVER_LOG_ME');}}),
  v => Object.assign(v,{exists:true}),
  v => Object.assign(v,{name:'foreign'}),
  v => Object.assign(v,{privateExtra:'NEVER_LOG_ME'}),
  v => Object.setPrototypeOf(v, {}),
  v => Object.defineProperty(v,'name',{get(){getterCalls++;throw Error('private getter invoked');}}),
  v => Object.defineProperty(v,Symbol('private'),{value:'NEVER_LOG_ME'}),
];
for(const mutate of mutations) {
  const value = Object.create(Object.getPrototypeOf(nativeMiss));
  Object.assign(value,nativeMiss);
  cache.set(id,Object.freeze(mutate(value)));
  assert.throws(capture, /public definition cache injection/);
  cache.set(id,nativeMiss);
  assert.equal(capture().current(),true);
}
cache.set(id,Object.assign(Object.create(Object.getPrototypeOf(nativeMiss)),nativeMiss));
assert.throws(capture,/public definition cache injection/, 'mutable forged placeholder accepted');
cache.set(id,nativeMiss);
cache.set('brn',nativeMiss);
assert.throws(capture,/public definition cache injection/, 'real effect replaced by an empty condition');
cache.delete('brn'); // Restore through the pinned original constructor, without foreign closures.
dex.conditions.getByID('brn');
cache.set('x'.repeat(129),dex.conditions.getByID('x'.repeat(129)));
assert.throws(capture,/public definition cache injection/);
cache.delete('x'.repeat(129));
assert.equal(getterCalls,0);

const stable = capture();
for(const key of ['p1','p2','sides','log','inputLog','activePokemon','activeTarget'])
  Object.defineProperty(battle.battle,key,{get(){throw Error('private actor state read');}});
assert.equal(capture().json,stable.json);
assert.equal(stable.current(),true);
console.log('Native actor-start cache oracle PASS: exact nonexistent conditions, fresh/expired leases, nine forged-effect/field/prototype/accessor negatives and private-state traps');
