'use strict';
// Public original factories are supplied from the whole hash-pinned pack JAR.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),crypto=require('node:crypto');
const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]),resources=path.resolve(__dirname,'../src/main/resources/com/maestroai/training');
global.require=require('node:module').createRequire(path.join(root,'index.js'));
const js=s=>vm.runInThisContext(s);
js(fs.readFileSync(path.join(root,'index.js'),'utf8'));
const modules=js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,
  BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,
  Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,
  BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,
  Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
const anchor=js(fs.readFileSync(path.join(resources,'runtime-source-anchor.js'),'utf8'))(modules);
const probe=js(fs.readFileSync(path.join(resources,'runtime-domain-probe.js'),'utf8'));
const pins=new Map(fs.readFileSync(path.join(resources,'runtime-pack-item-wires.tsv'),'utf8').trim().split('\n').map(l=>l.split('\t')));
const scripts=JSON.parse(fs.readFileSync(path.join(root,'../pack-item-fixtures.json'),'utf8'));
const wire=s=>s.replace(/\r\n/g,'\n').replace(/\n$/,'').replace(/\n/g,' ');
const permitted=(id,code)=>pins.get(id)===crypto.createHash('sha256').update(code).digest('hex');
let mutations=0;
const observer=js(fs.readFileSync(path.join(resources,'runtime-pack-item-observer.js'),'utf8'))(anchor,permitted,()=>mutations++);
const originalReceiver=js('receiveHeldItemData');
const stream=js('new BS.BattleStream()');
stream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const capture=()=>probe(stream.battle,modules,false,null,anchor,true);
const before=capture();assert(before.current());
const cache=js("Dex.mod('cobblemon').items.itemCache"), cacheBefore=[...cache];
for(const [id,raw] of Object.entries(scripts)) {
 assert(permitted(id,wire(raw)),'original public wire pin rejected');
 assert(!permitted(id,wire(raw)+' '),'changed wire accepted');
 assert(!permitted('foreign',wire(raw)),'wrong identity accepted');
 // Invoke the actual pinned JS receiver boundary, retaining the source-owned
 // observer: this is not a direct mutation of trusted callback collections.
 js('receiveHeldItemData') (id,wire(raw));
}
assert.deepEqual([...cache],cacheBefore,'observer changed the live item cache');
assert.equal(mutations,8);assert(observer.complete());assert(observer.current());
assert(!before.current(),'reload retained old configuration lease');
const after=capture();assert(after.current());
const raw=JSON.parse(after.json);
const components=Object.fromEntries(Object.entries(raw.components).map(([k,v])=>[k,crypto.createHash('sha256').update(v).digest('hex')]));
const expected=Object.fromEntries(fs.readFileSync(path.join(resources,'runtime-pack-reload-components.tsv'),'utf8').trim().split('\n').map(l=>l.split('\t')));
assert.deepEqual(components,expected,'independent post-reload operation fingerprint differs');
// Independent source semantics: these hooks apply only to unsupported held
// items/actions, and have no entry-announcement callback. They gain no actor scope.
const items=js("require('./data/mods/cobblemon/items').Items");
for(const id of Object.keys(scripts)) assert(!Object.keys(items[id]).some(k=>['onStart','onSwitchIn','onSwitchOut'].includes(k)));
let chained=0;
const context={chainModify:n=>{chained++;return n;}};
assert.equal(items.eviolite.onModifyDef.call(context,100,{baseSpecies:{nfe:true},species:{name:'Pikachu-Original'}}),undefined);
assert.equal(items.eviolite.onModifyDef.call(context,100,{baseSpecies:{nfe:true},species:{name:'Pikachu'}}),1.5);
assert.equal(chained,1);
assert.equal(items.legendplate.onTryMove.call({}, {hasItem:()=>false},null,null),undefined);
// Different code, a forged same-text closure and bypassing the observer cannot
// gain provenance. Restore only through a genuine exact-wire dispatch.
js('receiveHeldItemData')('eviolite',wire(scripts.eviolite)+' ');
assert.throws(capture,/public source callback position changed/);
js('receiveHeldItemData')('eviolite',wire(scripts.eviolite));
assert(capture().current());assert(!after.current());
js(`const saved=items.Items.eviolite;items.Items.eviolite={...saved,onModifyDef:eval('({'+saved.onModifyDef.toString()+'}).onModifyDef')};`);
assert.throws(capture,/public source callback position changed/);
js('receiveHeldItemData')('eviolite',wire(scripts.eviolite));
let calls=0;Object.defineProperty(items,'eviolite',{configurable:true,get(){calls++;throw Error('NEVER_LOG_ME');},set(){}});
js('receiveHeldItemData')('eviolite',wire(scripts.eviolite));
assert.equal(calls,0,'observer invoked an untrusted target accessor');
assert.throws(capture,/accessor/);
Object.defineProperty(items,'eviolite',{value:{},writable:true,configurable:true,enumerable:true});
js('receiveHeldItemData')('eviolite',wire(scripts.eviolite));assert(capture().current());
const stable=capture();
for(const k of ['p1','p2','sides','log','inputLog','activePokemon','activeTarget'])Object.defineProperty(stream.battle,k,{get(){throw Error('private actor touched');}});
assert.equal(capture().json,stable.json);
js('receiveHeldItemData=function(){}');assert(!observer.current(),'replaced transport retained provenance');
assert.notEqual(js('receiveHeldItemData'),originalReceiver);
console.log('Pinned pack item reload oracle PASS: actual receiver, four original factories, independent fingerprints/semantics, changed-wire/identity/closure/accessor negatives and privacy');
