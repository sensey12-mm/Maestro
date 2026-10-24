'use strict';
const fs=require('fs'),vm=require('vm'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(process.argv[2]),res=path.resolve(__dirname,'../src/main/resources/com/maestroai/training')+'/';
global.require=require('module').createRequire(path.join(root,'index.js'));const js=s=>vm.runInThisContext(s);
js(fs.readFileSync(path.join(root,'index.js'),'utf8'));
const modules=js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,Side:require('./sim/side').Side,Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
const anchor=js(fs.readFileSync(res+'runtime-source-anchor.js','utf8'))(modules),probe=js(fs.readFileSync(res+'runtime-domain-probe.js','utf8'));
const pins=n=>new Map(fs.readFileSync(res+n,'utf8').trim().split('\n').map(l=>l.split('\t'))),sha=s=>crypto.createHash('sha256').update(s).digest('hex');
const addon=pins('runtime-pack-definition-wires.tsv'),raidPins=pins('runtime-pack-raid-wires.tsv'),items=pins('runtime-pack-item-wires.tsv');
for(const [k,v] of addon)if(k.startsWith('heldItem/'))items.set(k.slice(9),v);
const fixture=n=>JSON.parse(fs.readFileSync(path.join(root,'../'+n),'utf8'));
const lf=s=>s.replace(/\r\n|\r/g,'\n').replace(/\n$/,'');
const flat=s=>lf(s).replace(/\n/g,' ');
const raid=fixture('raid-condition-fixtures.json');
assert.deepEqual(Object.keys(raid).sort(),['cheerattack','cheerdefense','raidboss','shield']);
const observer=js(fs.readFileSync(res+'runtime-pack-definition-observer.js','utf8'));
const names=['receiveScriptData','receiveAbilityData','receiveMoveData','receiveConditionData'];
const originals=names.map(n=>js(n));
// The earlier permission set has no Raid Dens sources. Dispatch original
// public factories, retaining every original callback and receiver execution.
const old=observer(anchor,(kind,id,code)=>addon.get(kind+'/'+id)===sha(code),()=>{});
for(const [id,text] of Object.entries(raid))js('receiveConditionData')(id,lf(text));
assert(!old.current(),'missing public provenance should reproduce rejected receipt');
names.forEach((n,i)=>globalThis[n]=originals[i]);
const allPins=new Map([...addon,...raidPins]);
const current=observer(anchor,(kind,id,code)=>allPins.get(kind+'/'+id)===sha(code),()=>{});
const itemObserver=js(fs.readFileSync(res+'runtime-pack-item-observer.js','utf8'))(anchor,(id,code)=>items.get(id)===sha(code),()=>{});
for(const [id,text] of Object.entries(fixture('pack-item-fixtures.json')))js('receiveHeldItemData')(id,flat(text));
const api={script:'receiveScriptData',heldItem:'receiveHeldItemData',ability:'receiveAbilityData',move:'receiveMoveData',condition:'receiveConditionData'};
const za=Object.entries(fixture('za-definition-fixtures.json'));
// Four raid receipts cannot substitute for missing ZA receipts.
for(const [key,text] of za.slice(0,8)){const [kind,id]=key.split('/');js(api[kind])(id,flat(text));}
for(const [id,text] of Object.entries(raid))js('receiveConditionData')(id,lf(text));
assert(current.current()&&current.raidComplete()&&!current.complete());
for(const [key,text] of za.slice(8)){const [kind,id]=key.split('/');js(api[kind])(id,flat(text));}
assert(current.current()&&current.complete()&&current.raidComplete()&&itemObserver.current());
js('afterSpeciesInit')();
const stream=js('new BS.BattleStream()');stream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const set=(species,ability,moves)=>({species,ability,moves,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
stream.battle.setPlayer('p1',{team:[set('Mewtwo','pressure',['swift','recover'])]});stream.battle.setPlayer('p2',{team:[set('Genesect','download',['ironhead','icebeam'])]});
const capture=()=>probe(stream.battle,modules,false,null,anchor,true,{phase:'',methodGroup:''});
for(const id of Object.keys(raid))stream.battle.dex.conditions.getByID(id);
for(const id of ['eviolite','legendplate','pikaniumz','pikashuniumz']){
  stream.battle.dex.items.getByID(id);stream.battle.dex.conditions.getByID('item:'+id);
}
const initial=capture();assert(initial.current());
const components=JSON.parse(initial.json).components;
assert.deepEqual(new Map(Object.entries(components).map(([k,v])=>[k,sha(v)])),pins('runtime-pack-za-raid-components.tsv'));
// No factory body executes during proof, and no private actor getter is read.
let reads=0;Object.defineProperty(stream.battle,'privateActorPayload',{get(){reads++;throw Error('PRIVATE_ACTOR');}});
assert(capture().current());assert.equal(reads,0);
const native=stream.battle.dex.items.getByID('eviolite');
stream.battle.dex.items.itemCache.set('eviolite',Object.assign(Object.create(Object.getPrototypeOf(native)),native,
  {onModifyDef:js('({'+native.onModifyDef.toString()+'}).onModifyDef')}));
assert.throws(capture,/public definition cache injection/,'equal callback text must not attest a foreign closure');
assert(!initial.current());stream.battle.dex.items.itemCache.set('eviolite',native);assert(capture().current());
// The raid loader preserves LF. A differently normalized wire must abstain.
js('receiveConditionData')('shield',flat(raid.shield));assert(!current.current());
js('receiveConditionData')('shield',lf(raid.shield));assert(current.current());
js('receiveConditionData')('shield',lf(raid.shield)+' ');assert(!current.current());
js('receiveConditionData')('shield',lf(raid.shield));assert(current.current());
assert(!initial.current());
// Restoring source does not silently bless an old cache or revive a lease.
// A new native dex can construct fresh, source-bound effects.
js('afterSpeciesInit')();
const fresh=js('new BS.BattleStream()');fresh.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
for(const id of Object.keys(raid))fresh.battle.dex.conditions.getByID(id);
assert(probe(fresh.battle,modules,false,null,anchor,true,{phase:'',methodGroup:''}).current());
console.log('Pinned raid oracle PASS: original rejection, separate completeness, exact combined profile, native item/condition caches, foreign closures, LF transport, privacy and withdrawal');
