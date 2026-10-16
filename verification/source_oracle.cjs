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
assert.equal(trapped.choose('p2','switch 2'),false);
assert.match(trapped.p2.choice.error,/active Pokémon is trapped/);
assert.equal(trapped.p2.activeRequest.active[0].trapped,true);
assert.equal(trapped.choose('p2','move 1'),true);
assert.equal(trapped.choose('p1','move 1'),true);
assert.equal(trapped.turn,2,'legal rechoice must advance the simulator');
for (const [species,item] of [['Mewtwo','shedshell'],['Gengar','']]) {
  const legal = trapCase(species,item);
  assert.equal(legal.choose('p2','switch 2'),true,`${species}/${item}: legal switch exception`);
}
console.log('Pinned simulator source oracle PASS: 50 fixed synthetic cases, 6 paired priority cases and 3 trapping/rechoice scenarios');
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
