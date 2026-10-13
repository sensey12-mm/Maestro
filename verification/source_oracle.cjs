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
console.log('Pinned simulator source oracle PASS: 50 fixed synthetic cases (20 damage, 12 entry, 10 order, 8 speed)');
