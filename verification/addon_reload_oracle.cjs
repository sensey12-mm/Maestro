'use strict';
const fs=require('fs'),vm=require('vm'),path=require('path'),crypto=require('crypto'),assert=require('assert/strict');
const root=path.resolve(process.argv[2]),res=path.resolve(__dirname,'../src/main/resources/com/maestroai/training')+'/';
global.require=require('module').createRequire(path.join(root,'index.js'));const js=s=>vm.runInThisContext(s);
js(fs.readFileSync(path.join(root,'index.js'),'utf8'));
const modules=js(`({Battle:require('./sim/battle').Battle,BattleActions:require('./sim/battle-actions').BattleActions,BattleQueue:require('./sim/battle-queue').BattleQueue,Field:require('./sim/field').Field,Pokemon:require('./sim/pokemon').Pokemon,Side:require('./sim/side').Side,Ability:require('./sim/dex-abilities').Ability,Item:require('./sim/dex-items').Item,DataMove:require('./sim/dex-moves').DataMove,BasicEffect:require('./sim/dex-data').BasicEffect,Utils:require('./lib').Utils,Registries:Object.fromEntries(Object.entries(require('./sim/cobblemon/registries'))),Cobblemon:Cobblemon})`);
const anchor=js(fs.readFileSync(res+'runtime-source-anchor.js','utf8'))(modules),probe=js(fs.readFileSync(res+'runtime-domain-probe.js','utf8'));
const pins=n=>new Map(fs.readFileSync(res+n,'utf8').trim().split('\n').map(l=>l.split('\t'))),sha=s=>crypto.createHash('sha256').update(s).digest('hex');
const addon=pins('runtime-pack-definition-wires.tsv'),items=pins('runtime-pack-item-wires.tsv');for(const [k,v] of addon)if(k.startsWith('heldItem/'))items.set(k.slice(9),v);
const wire=s=>s.split(/\r\n|\n|\r/).slice(0,s.endsWith('\n')?-1:undefined).join(' ');
// Reproduce the pre-receipt defect with the original published API/factories.
// This is not a fabricated replacement function or a production-derived label.
const early=JSON.parse(fs.readFileSync(path.join(root,'../za-definition-fixtures.json'),'utf8'));
for(const id of ['actions','pokemon'])js('receiveScriptData')(id,wire(early['script/'+id]));
const unobserved=js('new BS.BattleStream()');
unobserved.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const rejectedProgress={phase:'',methodGroup:''};
assert.throws(()=>probe(unobserved.battle,modules,false,null,anchor,true,rejectedProgress),/unverified engine method position/);
assert.equal(rejectedProgress.phase,'PUBLIC_CONFIGURATION_METHODS_REJECTED');assert.equal(rejectedProgress.methodGroup,'ACTIONS');
const rejectedSlots=JSON.parse(anchor.auditMethods(modules,unobserved.battle)).failures;
assert(rejectedSlots.includes('ACTIONS/canMegaEvo/IDENTITY')&&rejectedSlots.includes('ACTIONS/runMegaEvo/IDENTITY'));
const publicScripts=js("require('./data/mods/cobblemon/scripts').Scripts");
delete publicScripts.actions;delete publicScripts.pokemon;
const io=js(fs.readFileSync(res+'runtime-pack-item-observer.js','utf8'))(anchor,(id,code)=>items.get(id)===sha(code),()=>{});
const other=js(fs.readFileSync(res+'runtime-pack-definition-observer.js','utf8'))(anchor,(kind,id,code)=>addon.get(kind+'/'+id)===sha(code),()=>{});
const old=JSON.parse(fs.readFileSync(path.join(root,'../pack-item-fixtures.json'),'utf8'));
for(const [id,code] of Object.entries(old))js('receiveHeldItemData')(id,wire(code));
const all=JSON.parse(fs.readFileSync(path.join(root,'../za-definition-fixtures.json'),'utf8'));
const names={script:'receiveScriptData',heldItem:'receiveHeldItemData',ability:'receiveAbilityData',condition:'receiveConditionData',move:'receiveMoveData'};
for(const [key,code] of Object.entries(all)){const [kind,id]=key.split('/');js(names[kind])(id,wire(code));}
js('afterSpeciesInit')();
const stream=js('new BS.BattleStream()');stream.write('>start '+JSON.stringify({format:{mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'}}));
const set=(species,ability,moves)=>({species,ability,moves,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
stream.battle.setPlayer('p1',{team:[set('Mewtwo','pressure',['swift','recover'])]});stream.battle.setPlayer('p2',{team:[set('Genesect','download',['ironhead','icebeam'])]});
const progress={phase:'',methodGroup:''};
assert(io.complete()&&io.addonComplete()&&other.complete()&&io.current()&&other.current());
const capture=()=>probe(stream.battle,modules,false,null,anchor,true,{phase:'',methodGroup:''});
const result=capture();assert(result.current());
assert.deepEqual(JSON.parse(anchor.auditMethods(modules,stream.battle)).failures,[],
 'native dex reinitialization was mislabeled as a method replacement');
const data=JSON.parse(result.json);
const expected=Object.fromEntries(pins('runtime-pack-za-components.tsv'));
assert.deepEqual(Object.fromEntries(Object.entries(data.components).map(([k,v])=>[k,sha(v)])),expected);
// Exercise native lazy construction for every new public definition, including
// item:/ability: condition aliases. Do not infer anything about an actor's set.
const liveDex=stream.battle.dex;
for(const key of Object.keys(all)){
 const [kind,id]=key.split('/'),group={ability:'abilities',heldItem:'items',move:'moves',condition:'conditions'}[kind];
 if(group)liveDex[group].getByID(id);
 if(kind==='ability'||kind==='heldItem')liveDex.conditions.getByID((kind==='ability'?'ability:':'item:')+id);
}
assert(capture().current(),'genuine lazy addon cache construction was rejected');
const genuineCache=liveDex.moves.moveCache.get('darkvoid');
liveDex.moves.moveCache.set('darkvoid',Object.freeze({...genuineCache,
 onHit:js('({'+genuineCache.onHit.toString()+'}).onHit')}));
assert.throws(capture,/public definition cache injection/,'same-text foreign cache callback gained provenance');
liveDex.moves.moveCache.set('darkvoid',genuineCache);
assert(capture().current());
// Independent ordinary-state semantics of the actual addon hooks, not copied
// expected formulas. Mega/custom transformations remain outside actor scope.
const acts=stream.battle.actions;
assert.equal(acts.canMegaEvo({side:{hasMegaEvolved:false},species:{name:'Mew',baseSpecies:'Mew'},getItem:()=>({}),baseMoves:['swift']}),null);
assert.equal(acts.runMegaEvo({canMegaEvo:null,canUltraBurst:null}),false);
const weather=js("Dex.mod('cobblemon').dataCache.Scripts.pokemon.effectiveWeather");
for(const state of ['','raindance','sunnyday','sandstorm']){
 const p={battle:{field:{effectiveWeather:()=>state}},hasItem:()=>false,hasAbility:()=>false};
 assert.equal(weather.call(p),state);
}
let callbacks=0;
const receiver=js('receiveScriptData');
receiver('actions',wire(all['script/actions'])+' ');
assert(!other.current(),'unknown wire gained a current transport');
assert(!result.current(),'changed script retained lease');
assert.throws(capture,/method position|callback position/);
receiver('actions',wire(all['script/actions']));assert(other.current());assert(capture().current());assert(!result.current());
// A same-text callback installed outside the exact original dispatch is foreign.
const scripts=js("require('./data/mods/cobblemon/scripts').Scripts");
scripts.actions.canMegaEvo=js('({'+scripts.actions.canMegaEvo.toString()+'}).canMegaEvo');
assert.throws(capture,/method position|callback position/);
receiver('actions',wire(all['script/actions']));assert(capture().current());
// Never bless unrelated callback fields left behind by Object.assign.
scripts.actions.UNLISTED=()=>{callbacks++;return 0;};
receiver('actions',wire(all['script/actions']));
assert.throws(capture,/public source callback position changed/);assert.equal(callbacks,0);
delete scripts.actions.UNLISTED;
receiver('actions',wire(all['script/actions']));assert(capture().current());
// Preserve actual receiver return values and exceptions, without executing a
// callback for provenance. Rejected factories remain unusable until repaired.
const moveResult=js('receiveMoveData')('nihillight',wire(all['move/nihillight']));
assert.equal(JSON.parse(moveResult).name,'Nihil Light');
assert.throws(()=>js('receiveMoveData')('nihillight',"(()=>{throw Error('PRIVATE_BODY')})()"),/PRIVATE_BODY/);
assert(!other.current());
js('receiveMoveData')('nihillight',wire(all['move/nihillight']));assert(other.current());
const stable=capture();
// Public source configuration only: no actor/request/team enumeration.
for(const key of ['p1','p2','sides','log','inputLog','activePokemon','activeTarget'])Object.defineProperty(stream.battle,key,{get(){callbacks++;throw Error('PRIVATE_ACTOR');}});
assert.equal(capture().json,stable.json);assert.equal(callbacks,0);
js('receiveScriptData=function(){}');assert(!other.current());
Object.defineProperty(globalThis,'eval',{configurable:true,get(){callbacks++;throw Error('PRIVATE_GLOBAL');}});
assert(!io.current());assert(!other.current());assert.equal(callbacks,0);
console.log('Pinned addon reload oracle PASS: 58 original factories/profile, ordinary-hook semantics, changed-wire/closure/merged-slot negatives, return/exception preservation, privacy and lease withdrawal');
