// Independent fixed synthetic checks against the pinned upstream simulator.
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(process.argv[2]);
const {Battle} = require(path.join(root, 'sim/battle'));
const corpus = fs.readFileSync(path.join(__dirname, '../src/main/resources/com/maestroai/training/cobblemon-mechanics-holdouts.tsv'), 'utf8');
const rows = corpus.split(/\r?\n/).filter(s => /^[DHOV]\t/.test(s)).map(s => s.split('\t'));
assert.equal(rows.length, 50, 'Unexpected fixed source-oracle scope');
function actors(ownTypes, otherTypes) {
  const engine = new Battle({formatid:'gen9customgame', seed:[7, 11, 13, 17]});
  function set() { return {species:'Mew', level:100, ability:'runaway', moves:['tackle','bulkup','quickattack'],
    movesInfo:[0,1,2].map(() => ({pp:40,maxPp:40}))}; }
  engine.setPlayer('p1', {team:[set(),set()]});
  engine.setPlayer('p2', {team:[set()]});
  engine.makeChoices('team 12','team 1');
  const own = engine.p1.active[0], other = engine.p2.active[0];
  own.types = own.baseTypes = ownTypes; other.types = other.baseTypes = otherTypes;
  engine.randomChance = () => false;
  return {engine,own,other};
}
for (const row of rows) {
  const kind = row[0], id = row[1];
  if (kind === 'D') {
    const [, ,move,level,attack,defense,attackStage,defenseStage,types,targetTypes,status,critical,low,high] = row;
    const {engine,own,other} = actors(types.split(','),targetTypes.split(','));
    own.level=Number(level); own.storedStats.atk=own.storedStats.spa=Number(attack);
    other.storedStats.def=other.storedStats.spd=Number(defense);
    own.boosts.atk=own.boosts.spa=Number(attackStage); other.boosts.def=other.boosts.spd=Number(defenseStage);
    own.status=status==='none'?'':status;
    const values=[];
    for (const crit of critical==='BOTH'?[false,true]:[critical==='CRITICAL']) {
      for(let roll=0;roll<16;roll++) {
        engine.random=()=>roll;const effect=engine.dex.getActiveMove(move);effect.willCrit=crit;
        values.push(engine.actions.getDamage(own,other,effect,true)||0);
      }
    }
    assert.deepEqual([Math.min(...values),Math.max(...values)],[Number(low),Number(high)],id);
  } else if (kind === 'H') {
    const [, ,maxHp,spikes,rocks,web,types,item,ability,lost,speed] = row;
    const {engine,own,other}=actors(types.split(','),['Normal']);
    own.hp=own.maxhp=own.baseMaxhp=Number(maxHp); own.item=item==='-'?'':item;
    own.itemState={id:own.item}; own.ability=ability; own.abilityState={id:ability};
    for(let n=0;n<Number(spikes);n++) own.side.addSideCondition('spikes',other);
    if(Number(rocks)) own.side.addSideCondition('stealthrock',other);
    if(Number(web)) own.side.addSideCondition('stickyweb',other);
    engine.runEvent('EntryHazard',own);
    assert.deepEqual([Number(maxHp)-own.hp,own.boosts.spe],[Number(lost),Number(speed)],id);
  } else if (kind === 'V') {
    const [, ,speed,stage,status,expected]=row;const {own}=actors(['Normal'],['Normal']);
    own.storedStats.spe=Number(speed);own.boosts.spe=Number(stage);own.status=status==='none'?'':status;
    assert.equal(own.getStat('spe'),Number(expected),id);
  } else {
    const [, ,action,reply,speed,otherSpeed,room,expected]=row;
    const {engine,own,other}=actors(['Normal'],['Normal']);
    own.storedStats.spe=Number(speed);other.storedStats.spe=Number(otherSpeed);
    if(room==='true') engine.field.addPseudoWeather('trickroom',own);
    const candidate=action.startsWith('switch:')?{choice:'switch',pokemon:own,target:own.side.pokemon[1]}:
      {choice:'move',pokemon:own,moveid:action.slice(5),targetLoc:1};
    const first=engine.queue.resolveAction(candidate)[0];
    const second=engine.queue.resolveAction({choice:'move',pokemon:other,moveid:reply,targetLoc:1})[0];
    const compared=engine.comparePriority(first,second);
    assert.equal(compared<0?'SELF_FIRST':compared>0?'OPPONENT_FIRST':'BOTH',expected,id);
  }
}
// Source-executed dual-public-reply priority checks; neither reply receives a probability.
const replyPairs = [
 ['quickattack','tackle',80,120,'SELF_FIRST'],
 ['tackle','quickattack',80,120,'OPPONENT_FIRST'],
 ['quickattack','quickattack',80,120,'OPPONENT_FIRST'],
 ['tackle','tackle',80,120,'OPPONENT_FIRST'],
 ['quickattack','quickattack',150,100,'SELF_FIRST'],
 ['tackle','tackle',150,100,'SELF_FIRST'],
];
for(const [action,reply,aSpeed,bSpeed,expected] of replyPairs) {
  const {engine,own,other}=actors(['Normal'],['Normal']);
  own.storedStats.spe=aSpeed; other.storedStats.spe=bSpeed;
  const a=engine.queue.resolveAction({choice:'move',pokemon:own,moveid:action,targetLoc:1})[0];
  const b=engine.queue.resolveAction({choice:'move',pokemon:other,moveid:reply,targetLoc:1})[0];
  const order=engine.comparePriority(a,b);
  assert.equal(order<0?'SELF_FIRST':order>0?'OPPONENT_FIRST':'BOTH',expected,
    action+'/'+reply);
}
// Independent engine oracle: legal exceptions and a rejected uncertain switch
// must differ. These are standard synthetic species, not runtime pack evidence.
function trapCase(species, item) {
  const engine = new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
  function set(species,ability,moves,item='') {
    return {species,ability,moves,item,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))};
  }
  engine.setPlayer('p1',{team:[set('Wobbuffet','shadowtag',['splash'])]});
  engine.setPlayer('p2',{team:[set(species,'pressure',['tackle'],item),set('Mew','synchronize',['tackle'])]});
  engine.makeChoices('team 1','team 12');
  return engine;
}
const trapped = trapCase('Mewtwo','');
assert.equal(trapped.p2.activeRequest.active[0].trapped ?? false,false);
assert.equal(trapped.p2.activeRequest.active[0].maybeTrapped,true);
const rejectedWire=[];
const originalSend=trapped.send.bind(trapped);
trapped.send=(kind,data)=>{ if(kind==='sideupdate' && data.startsWith('p2\n')) rejectedWire.push(data); originalSend(kind,data); };
assert.equal(trapped.choose('p2','switch 2'),false);
assert.equal(rejectedWire.length,2,'native rejection must emit error and corrected owning request');
assert.ok(rejectedWire[0].startsWith("p2\n|error|[Unavailable choice] Can't switch:"));
assert.ok(rejectedWire[1].startsWith('p2\n|request|'));
assert.equal(JSON.parse(rejectedWire[1].split('|request|')[1]).active[0].trapped,true);
assert.equal(rejectedWire[1].includes('maybeTrapped'),false);
assert.match(trapped.p2.choice.error,/active Pokémon is trapped/);
assert.equal(trapped.p2.activeRequest.active[0].trapped,true);
assert.equal(trapped.choose('p2','move 1'),true);
assert.equal(trapped.choose('p1','move 1'),true);
assert.equal(trapped.turn,2,'legal rechoice must advance the simulator');
for (const [species,item] of [['Mewtwo','shedshell'],['Gengar','']]) {
  const legal = trapCase(species,item);
  assert.equal(legal.choose('p2','switch 2'),true,`${species}/${item}: legal switch exception`);
}
// Independent held-out: a consumed Power Herb, Encore and a completed second
// Geomancy produce a switchable-looking request with one disabled alternative.
const encored = new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
const nativeSet=(species,ability,moves,item='')=>({species,ability,moves,item,level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
encored.setPlayer('p1',{team:[nativeSet('Wobbuffet','shadowtag',['splash','encore'])]});
encored.setPlayer('p2',{team:[nativeSet('Xerneas','fairyaura',['geomancy','moonblast'],'powerherb'),nativeSet('Marshadow','technician',['spectralthief'])]});
encored.makeChoices('team 1','team 12');
encored.makeChoices('move 1','move 1'); encored.makeChoices('move 2','move 1'); encored.makeChoices('move 1','move 1');
assert.equal(encored.turn,4); assert.equal(encored.p2.active[0].item,'');
assert.ok(encored.p2.active[0].volatiles.encore);
assert.equal(encored.p2.activeRequest.active[0].maybeTrapped,true);
assert.equal(encored.p2.activeRequest.active[0].moves[1].disabled,true);
assert.equal(encored.choose('p2','switch 2'),false);
assert.equal(encored.p2.activeRequest.active[0].trapped,true);
assert.equal(encored.choose('p2','move 1'),true); assert.equal(encored.choose('p1','move 1'),true);
assert.equal(encored.turn,5);
console.log('Pinned simulator source oracle PASS: 50 fixed synthetic cases, 6 paired priority cases and 4 trapping/rechoice scenarios, including Encore/Geomancy');
// Positive announcements, not silence, distinguish this ordinary entry from
// Illusion. Fixtures are independent of the Java identity-evidence provider.
function pressureEntry(ability) {
  const engine=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
  function set(species,ability){return {species,ability,level:80,moves:['tackle'],movesInfo:[{pp:40,maxPp:40}]};}
  engine.setPlayer('p1',{team:[set('Mew','runaway')]});
  engine.setPlayer('p2',{team:[set(ability==='illusion'?'Zoroark':'Mew',ability),set('Mew','pressure')]});
  engine.makeChoices('team 1','team 12');return engine;
}
const announced=pressureEntry('pressure'),masked=pressureEntry('illusion');
assert(announced.log.some(l=>l.startsWith('|-ability|p2a:')&&l.endsWith('|Pressure')));
assert(!masked.log.some(l=>l.startsWith('|-ability|p2a:')&&l.endsWith('|Pressure')));
assert(masked.p2.active[0].illusion,'negative fixture must actually carry an Illusion');
const pressure=announced.dex.abilities.get('pressure');
assert.equal(pressure.onDeductPP.call({},{isAlly:()=>false},{}),1);
assert.equal(pressure.onDeductPP.call({},{isAlly:()=>true},{}),undefined);
console.log('Pinned Pressure/Illusion source evidence PASS: positive entry and PP-only scope');
// Held-out production-provider matchup. Numerical endpoints come solely from
// this pinned engine; Java must enclose them using public spread bounds.
function pipelineBattle() {
  const engine=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
  function set(species,ability){return {species,ability,level:100,
    moves:['scratch','shadowclaw','swordsdance','tackle'],movesInfo:[0,1,2,3].map(()=>({pp:40,maxPp:40}))};}
  engine.setPlayer('p1',{team:[set('Gengar','runaway'),set('Mew','runaway')]});
  engine.setPlayer('p2',{team:[set('Mew','pressure')]});
  engine.makeChoices('team 12','team 1');return engine;
}
const pipeline=pipelineBattle(),a=pipeline.p1.active[0],f=pipeline.p2.active[0];
a.storedStats.atk=166;
for(const [id,expected] of [['scratch',[16,48]],['shadowclaw',[78,246]]]) {
  const values=[];
  for(const defense of [184,328]) for(const critical of [false,true]) for(let roll=0;roll<16;roll++) {
    f.storedStats.def=defense;pipeline.random=()=>roll;
    const effect=pipeline.dex.getActiveMove(id);effect.willCrit=critical;
    values.push(pipeline.actions.getDamage(a,f,effect,true)||0);
  }
  assert.deepEqual([Math.min(...values),Math.max(...values)],expected,`public-spread endpoints ${id}`);
}
for(const id of ['tackle','quickattack'])
  assert.equal(pipeline.actions.getDamage(f,a,pipeline.dex.getActiveMove(id),true)||0,0,`Ghost immunity ${id}`);
assert(78/404>48/310,'strict bounded comparison must survive public spread uncertainty');
a.boosts.atk=6;pipeline.boost({atk:2},a,a,pipeline.dex.moves.get('swordsdance'));
assert.equal(a.boosts.atk,6,'repeated capped setup creates no extra stages');
const arriving=pipeline.p1.pokemon[1];arriving.hp=arriving.maxhp=arriving.baseMaxhp=341;
arriving.side.addSideCondition('spikes',f);arriving.side.addSideCondition('spikes',f);
arriving.side.addSideCondition('stealthrock',f);
pipeline.makeChoices('switch 2','move 3');
assert.equal(341-arriving.hp,98,'actual switch execution: two Spikes layers plus Stealth Rock');
console.log('Pinned tactical provider holdouts PASS: 128 damage samples, two immune replies, capped setup and actual hazard switch');

// A naturally obtainable acceptance fixture, separate from the artificial
// Gengar/Run Away holdout. Source facts and numerical oracles do not depend on Java.
const crypto=require('node:crypto');
const {Dex}=require(path.join(root,'sim/dex'));
const publicCatalog={};
for(const row of fs.readFileSync(path.join(__dirname,'../src/main/resources/com/maestroai/training/public-species-catalog.tsv'),'utf8').trim().split('\n')) {
  const [id,...columns]=row.split('\t'),species=Dex.species.get(id);
  assert.deepEqual(columns,[...['hp','atk','def','spa','spd','spe'].map(k=>species.baseStats[k]),species.types.map(t=>t.toLowerCase()).join(','),species.maxHP||'-'].map(String),'public catalog '+id);
  publicCatalog[id]={stats:species.baseStats,types:species.types,fixedHp:species.maxHP||null};
}
const sorted=x=>x===null||typeof x!=='object'?x:Array.isArray(x)?x.map(sorted):Object.fromEntries(Object.keys(x).sort().map(k=>[k,sorted(x[k])]));
const catalogHash=crypto.createHash('sha256').update(JSON.stringify(sorted(publicCatalog))).digest('hex');
const componentPins=fs.readFileSync(path.join(__dirname,'../src/main/resources/com/maestroai/training/runtime-domain-components.tsv'),'utf8');
assert(componentPins.includes('catalog\t'+catalogHash+'\n'),'public catalog receipt does not cover the verified values');
const natural=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
function naturalSet(species,level,ability,moves,item='',evs={}) {
  return {species,level,ability,moves,item,evs,ivs:{hp:31,atk:0,def:31,spa:31,spd:31,spe:31},nature:'Bold',movesInfo:moves.map(()=>({pp:40,maxPp:40}))};
}
natural.setPlayer('p1',{team:[naturalSet('Weavile',50,'pressure',['shadowclaw','swordsdance']),naturalSet('Mewtwo',50,'pressure',['bulkup','nastyplot'])]});
natural.setPlayer('p2',{team:[naturalSet('Skarmory',100,'sturdy',['spikes','stealthrock'],'',{hp:252,def:252,spd:4}),naturalSet('Mewtwo',100,'pressure',['psychocut','bulkup','aurasphere'],'sitrusberry',{hp:252,def:252,spd:4})]});
natural.makeChoices('team 12','team 12');
assert.equal(natural.p1.active[0].storedStats.atk,112);
natural.makeChoices('move swordsdance','move spikes');
natural.makeChoices('move swordsdance','move stealthrock');
natural.makeChoices('move swordsdance','switch 2');
const na=natural.p1.active[0],nf=natural.p2.active[0];
assert.equal(na.boosts.atk,6);assert.equal(nf.maxhp,416);assert.equal(nf.storedStats.def,306);
natural.makeChoices('move shadowclaw','move psychocut');
natural.makeChoices('move shadowclaw','move bulkup');
// These low-attack/high-defense builds survive a critical hit and reach an
// actual publicly announced berry removal, without inventing absent items.
for(let n=0;n<8 && nf.item && !nf.fainted;n++) natural.makeChoices('move shadowclaw','move psychocut');
assert(!nf.fainted && nf.item==='','natural fixture must consume Sitrus before fainting');
assert(natural.log.some(l=>l.includes('|-enditem|p2a:')&&l.includes('|Sitrus Berry')));
assert.equal(na.hp,na.maxhp,'Psycho Cut must respect Dark immunity');
const values=[];natural.randomChance=()=>false;
for(const defense of [166,306])for(const critical of [false,true])for(let roll=0;roll<16;roll++) {
  nf.storedStats.def=defense;nf.boosts.def=1;natural.random=()=>roll;
  const m=natural.dex.getActiveMove('shadowclaw');m.willCrit=critical;
  values.push(natural.actions.getDamage(na,nf,m,true)||0);
}
assert.deepEqual([Math.min(...values),Math.max(...values)],[54,254],'natural public spread with observed Bulk Up');
const cut=natural.dex.moves.get('psychocut');
assert.equal(cut.basePower,70);assert.equal(cut.priority,0);assert.equal(cut.type,'Psychic');
assert.equal(cut.category,'Physical');assert.equal(cut.critRatio,2);assert.equal(cut.secondary,null);
assert(!Object.keys(cut).some(k=>k.startsWith('on')),'Psycho Cut acquired an unsupported callback');
// Source-check Psychic effectiveness for every defending type, including both
// dual-type orders with immunity. This is independent of Java's type table.
for(const type of ['Normal','Fire','Water','Electric','Grass','Ice','Fighting','Poison','Ground','Flying','Psychic','Bug','Rock','Ghost','Dragon','Dark','Steel','Fairy']) {
  const immune=!natural.dex.getImmunity('Psychic',type), exponent=natural.dex.getEffectiveness('Psychic',type);
  assert.equal(immune,type==='Dark');
  assert.equal(exponent,['Fighting','Poison'].includes(type)?1:['Psychic','Steel'].includes(type)?-1:0);
}
nf.storedStats.def=306;
const before=natural.p1.pokemon[1].hp;natural.makeChoices('switch 2','move bulkup');
assert.equal(before-natural.p1.active[0].hp,44,'actual natural reserve entry through two hazards');
console.log('Natural acceptance source oracle PASS: 14 catalog entries, legal abilities/moves, actual berry consumption, 64 damage bounds, Psychic immunity and real hazard entry');
const observedOnly = ['earthquake','uturn','ironhead','icebeam','moonblast','focusblast','astralbarrage','psyshock',
  'drainingkiss','energyball','closecombat','rocktomb','shadowball','hypervoice','aurasphere'];
const rawMoves = require(path.join(root,'data/moves')).Moves;
for (const id of observedOnly) {
  const definition=rawMoves[id];
  assert(definition && !Object.values(definition).some(v=>typeof v==='function'),`past-only source callbacks changed: ${id}`);
  assert(definition.basePower>0 && ['Physical','Special'].includes(definition.category));
  const secondary=definition.secondary;
  if(secondary)assert(Object.keys(secondary).every(k=>['chance','status','boosts','volatileStatus'].includes(k)));
  if(secondary?.volatileStatus)assert.equal(secondary.volatileStatus,'flinch');
}
// Earthquake is ordinary directly damaging Gen9 history with no secondary
// field/status effect. This is a past-only witness, not a new conditional
// future damage certificate for Dig, airborne targets or pack scripts.
assert.equal(rawMoves.earthquake.num,89);
assert.equal(rawMoves.earthquake.type,'Ground');
assert.equal(rawMoves.earthquake.category,'Physical');
assert.equal(rawMoves.earthquake.basePower,100);
assert.equal(rawMoves.earthquake.secondary,null);
assert.equal(rawMoves.earthquake.target,'allAdjacent');
// Phase 3.52: certify the exact ordinary, no-secondary attacking slice for
// Calyrex-Shadow. Psyshock uniquely uses the opponent's Defense despite
// being a Special attack; never silently reinterpret it as physical.
for(const [id,bp,type,target,override] of [
  ['astralbarrage',120,'Ghost','allAdjacentFoes',undefined],
  ['psyshock',80,'Psychic','normal','def']
]) {
  const m=rawMoves[id];
  assert.equal(m.basePower,bp,id+' basePower');
  assert.equal(m.category,'Special',id+' category');
  assert.equal(m.type,type,id+' type');
  assert.equal(m.target,target,id+' target');
  assert.equal(m.priority,0,id+' priority');
  assert.equal(m.accuracy,100,id+' accuracy');
  assert.equal(m.secondary,null,id+' secondary');
  assert.equal(m.overrideDefensiveStat,override,id+' defensive target');
  assert(!m.selfSwitch && !m.drain && !m.self && !m.recoil,
    id+' unsupported after-hit effects');
  assert(!Object.values(m).some(v=>typeof v==='function'),
    id+' unsupported callbacks');
}
assert.equal(rawMoves.uturn.selfSwitch,true);
assert.deepEqual(rawMoves.drainingkiss.drain,[3,4]);
assert.deepEqual(rawMoves.closecombat.self.boosts,{def:-1,spd:-1});
assert.equal(require(path.join(root,'data/conditions')).Conditions.flinch.duration,1);
const fieldCode=fs.readFileSync(path.join(__dirname,'../src/main/java/com/maestroai/training/PublicFieldEvidence.java'),'utf8');
const observedIds=[...fieldCode.match(/OBSERVED_ONLY = Set.of\(([\s\S]*?)\);/)[1].matchAll(/"([a-z]+)"/g)].map(x=>x[1]).sort();
assert.deepEqual(observedIds,observedOnly.sort(),'past-only Java scope drift');
console.log('Past-only public-state source oracle PASS: 15 declarative moves, ordinary pivot, observed drain/stages and one-turn flinch; no future secondary/pivot forecast');
{
  const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
  const make=(moves)=>({species:'Mew',ability:'pressure',moves,
    level:100,movesInfo:moves.map(()=>({pp:40,maxPp:40}))});
  b.setPlayer('p1',{team:[make(['astralbarrage','psyshock'])]});
  b.setPlayer('p2',{team:[make(['swift'])]});
  b.makeChoices('team 1','team 1');
  const own=b.p1.active[0], foe=b.p2.active[0];
  own.storedStats.atk=50; own.storedStats.spa=300;
  function hit(id,def,spd,crit,roll){
    foe.storedStats.def=def;foe.storedStats.spd=spd;b.random=()=>roll;
    const m=b.dex.getActiveMove(id);m.willCrit=crit;
    return b.actions.getDamage(own,foe,m,true);
  }
  for(const [id,bp,type] of [['astralbarrage',120,'Ghost'],['psyshock',80,'Psychic']]) {
    const rolls=[];
    for(const def of [150,300])for(const spd of [150,450])
      for(const crit of [false,true])for(let n=0;n<16;n++)
        rolls.push(hit(id,def,spd,crit,n));
    assert(rolls.every(v=>Number.isFinite(v)&&v>=0),id+' malformed native outcomes');
  }
  assert(hit('psyshock',150,150,false,0)>hit('psyshock',300,150,false,0),
    'Psyshock must use Defense');
  assert.equal(hit('psyshock',150,150,false,0),hit('psyshock',150,450,false,0),
    'Psyshock must not use Special Defense');
  assert.equal(hit('astralbarrage',150,150,false,0),hit('astralbarrage',300,150,false,0),
    'Astral Barrage must not use physical Defense');
  assert(hit('astralbarrage',150,150,false,0)>hit('astralbarrage',150,450,false,0),
    'Astral Barrage must use Special Defense');
  console.log('Phase3.52 Calyrex ordinary moves source oracle PASS: pinned move properties and independent native attack/defense selection, crit/roll fixture');
}

// Independently execute source healing, including odd max HP and capping.
for (const [maxHp,hp,expected] of [[381,100,291],[381,300,381],[381,381,381],[1,1,1]]) {
  const engine=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
  const set=move=>({species:'Mew',ability:'runaway',moves:[move],level:100,movesInfo:[{pp:40,maxPp:40}]});
  engine.setPlayer('p1',{team:[set('recover')]});engine.setPlayer('p2',{team:[set('splash')]});
  engine.makeChoices('team 1','team 1');
  const own=engine.p1.active[0];
  const recovery=engine.dex.moves.get('recover');
  assert.deepEqual(recovery.heal,[1,2]);
  assert.equal(recovery.priority,0);assert.equal(recovery.target,'self');
  own.maxhp=own.baseMaxhp=maxHp;own.hp=hp;
  engine.makeChoices('move recover','move splash');
  assert.equal(own.hp,expected,'source Recover rounding/cap '+maxHp+'/'+hp);
}
console.log('Recover source oracle PASS: four exact rounding/capping cases');


// Actual fixed Maestro roster: exact move IDs are an OWN-team verification
// fixture, never a guessed opponent moveset at runtime. Source definitions
// must match the Java move calculator (including accuracy & effect flags).
{
 const src=fs.readFileSync(path.join(__dirname,'../src/main/java/com/maestroai/training/CobblemonMechanics.java'),'utf8');
 const audit=fs.readFileSync(path.join(__dirname,'../src/main/java/com/maestroai/training/MaestroOwnMoveAudit.java'),'utf8');
 const sets=fs.readFileSync(path.join(__dirname,'maestro_fixed_team_moves.tsv'),'utf8').trim().split('\n').map(r=>r.split('\t'));
 assert.equal(sets.length,6,'Maestro own team must contain six species');
 const all=new Set(sets.flatMap(r=>r.slice(1)));
 assert.equal(all.size,23,'24 move slots must contain 23 unique moves');
 for(const row of sets) {
   assert.equal(row.length,5,'each boss must have exactly four moves');
   assert(audit.includes('"'+row[0]+'"')&&row.slice(1).every(id=>audit.includes('"'+id+'"')),
     'Maestro own-team Java reference differs');
 }
 const supported=new Map();
 for(const m of src.matchAll(/Map\.entry\("([a-z]+)", new Move\("([a-z]+)", "([a-z]+)", (\d+), (true|false), (-?\d+), Map\.of\(([^)]*)\), (true|false)(?:, (\d+))?\)\)/g)) {
   const [,key,id,type,power,physical,priority,boosts,complete,accuracy]=m;
   assert.equal(id,key);
   supported.set(id,{type,power:+power,physical:physical==='true',priority:+priority,
     complete:complete==='true',accuracy:accuracy?+accuracy:100});
 }
 const effectLabels=new Map([...audit.matchAll(/Map\.entry\("([a-z]+)", "([A-Z_]+)"\)/g)].map(m=>[m[1],m[2]]));
 for(const id of all) {
   const value=supported.get(id);
   assert(value,'missing move calculator entry for actual Maestro move '+id);
   const m=Dex.moves.get(id);
   const raw=rawMoves[id];
   assert.equal(value.type,m.type.toLowerCase(),id+' move type');
   assert.equal(value.power,m.basePower,id+' power');
   assert.equal(value.physical,m.category==='Physical',id+' physical');
   assert.equal(value.priority,m.priority,id+' priority');
   assert.equal(value.accuracy,typeof m.accuracy==='number'?m.accuracy:100,id+' accuracy');
   // Complete means no side effects beyond pure fixed-power damage or
   // separately modeled setup/recovery. Crit-ratio is bounded by BOTH.
   if(value.complete) {
     assert.equal(m.category==='Status',false,id+' status marked damage-complete');
     assert.equal(m.secondary,null,id+' secondary marked complete');
     assert(!raw.selfSwitch&&!raw.drain&&!raw.self&&!raw.recoil&&!raw.boosts&&!raw.onTryMove
       &&!raw.onModifyMove&&!raw.onBasePower&&!raw.onHit&&!raw.onAfterHit,
       id+' incomplete source special effect marked complete');
   } else if(m.category!=='Status') {
     assert(effectLabels.has(id),id+' missing explicit partial outcome effect reason');
   } else if(id==='geomancy') {
     assert(effectLabels.has(id)&&!!raw.onTryMove&&!!raw.boosts,
       'Geomancy charging and boost/callback must remain guarded');
   }
 }
 const matrix=src.match(/private static final String\[\] TYPE_MATCHUPS = \{([\s\S]*?)\n    \};/);
 assert(matrix,'Java type chart no longer source auditable');
 const rows=[...matrix[1].matchAll(/"([^"]+)"/g)].map(m=>m[1]);
 assert.equal(rows.length,18,'type chart missing attack types');
 const types=['normal','fire','water','electric','grass','ice','fighting','poison','ground',
   'flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy'];
 const actual=new Map(rows.map(row=>{
   const [type,...tokens]=row.split(' ');
   return [type,new Map(tokens.map(t=>{const [to,n]=t.split(':');return [to,n==='X'?-99:+n]}))];
 }));
 assert.deepEqual([...actual.keys()].sort(),types.slice().sort(),'Java attack type domain mismatch');
 for(const attacker of types)for(const target of types) {
   // TypeChart.damageTaken is indexed by canonical attacker display type,
   // not lowercase IDs. Read the normalized *pinned native Dex* entries
   // directly to avoid passing incorrectly cased strings to the helper API.
   const taken=Dex.types.get(target).damageTaken;
   const code=taken[attacker[0].toUpperCase()+attacker.slice(1)];
   const expected=code===3?-99:code===1?1:code===2?-1:0;
   assert.equal(actual.get(attacker).get(target)??0,expected,
      attacker+' -> '+target+' type chart source divergence');
 }
 console.log('Maestro six-member/24-slot/23-move source oracle PASS: exact move IDs, damage properties, effect-aware UNKNOWN, 324 type matchups');
}

const policyOutcomes=require('./policy_oracle.cjs').run(Battle);
assert.equal(policyOutcomes.length,8);
console.log('Independent policy outcome oracle PASS: 8 jointly legal source-executed exchanges before residuals');
