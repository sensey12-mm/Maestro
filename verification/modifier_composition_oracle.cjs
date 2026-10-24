'use strict';

// Declared synthetic stat/type/modifier conditions, never opponent build facts.
// All damage and event ordering are native; only the critical/roll inputs are fixed.
exports.run = function(Battle) {
  const seeds=[
    {id:'tech-aura',move:'drainingkiss',ability:'technician',targetAbility:'fairyaura'},
    {id:'tech-aura-orb',move:'drainingkiss',ability:'technician',targetAbility:'fairyaura',item:'lifeorb'},
    {id:'tech-aura-room',move:'drainingkiss',ability:'technician',targetAbility:'fairyaura',item:'lifeorb',room:true},
    {id:'tech-aura-round',move:'drainingkiss',ability:'technician',targetAbility:'fairyaura',attack:101,defense:219,level:99},
    {id:'above-tech',move:'moonblast',ability:'technician',targetAbility:'fairyaura'},
    {id:'double-aura',move:'drainingkiss',ability:'fairyaura',targetAbility:'fairyaura'},
    {id:'aura-orb-shield',move:'drainingkiss',ability:'fairyaura',item:'lifeorb'},
    {id:'aura-orb-chipped',move:'drainingkiss',ability:'fairyaura',item:'lifeorb',targetHp:999},
    {id:'aura-orb-room',move:'drainingkiss',ability:'fairyaura',item:'lifeorb',room:true},
    {id:'tech-orb-shield',move:'shadowsneak',ability:'technician',item:'lifeorb'},
    {id:'tech-orb-chipped',move:'shadowsneak',ability:'technician',item:'lifeorb',targetHp:999},
    {id:'tech-orb-room',move:'shadowsneak',ability:'technician',item:'lifeorb',room:true},
    {id:'sixty-tech-orb-shield',move:'swift',ability:'technician',item:'lifeorb'},
    {id:'forty-tech-orb-shield',move:'tackle',ability:'technician',item:'lifeorb'},
    {id:'shield-last-hp',item:'lifeorb',targetHp:1},
    {id:'orb-shield-neutral',item:'lifeorb'},
    {id:'orb-shield-critical-stages',item:'lifeorb',attackStage:-2,defenseStage:2},
    {id:'orb-shield-burn',move:'closecombat',item:'lifeorb',status:'brn',types:['fighting']},
    {id:'orb-shield-stab',move:'astralbarrage',item:'lifeorb',types:['ghost']},
    {id:'orb-shield-resist',move:'shadowclaw',item:'lifeorb',targetTypes:['dark']},
    {id:'orb-shield-immune',move:'shadowclaw',item:'lifeorb',targetTypes:['normal']},
    {id:'sun-orb-shield',move:'fireblast',item:'lifeorb',weather:'sunnyday',types:['fire']},
    {id:'rain-orb-shield',move:'fireblast',item:'lifeorb',weather:'raindance'},
    {id:'odd-max',item:'lifeorb',targetMax:1001,targetHp:1001},
    {id:'fraction-round-one',move:'swift',ability:'technician',item:'lifeorb',attack:11,defense:300},
    {id:'fraction-round-two',item:'lifeorb',attack:321,defense:413},
    {id:'tech-nonqualifying',move:'uturn',ability:'technician',item:'lifeorb'},
    {id:'no-orb-shield'},
    {id:'no-shield-orb',item:'lifeorb',targetAbility:'runaway'},
    {id:'no-modifier',targetAbility:'runaway'},
    {id:'psychic-terrain-grounded',item:'lifeorb',terrain:'psychicterrain'},
    {id:'psychic-terrain-flying',item:'lifeorb',terrain:'psychicterrain',types:['flying']},
    {id:'psychic-terrain-levitate',item:'lifeorb',terrain:'psychicterrain',ability:'levitate'}
  ];
  const rows=[];
  for(const seed of seeds)for(const enemy of [false,true]){
    const c={move:'psyshock',ability:'runaway',targetAbility:'shadowshield',item:'none',targetItem:'none',
      attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],
      status:'none',targetMax:1000,targetHp:1000,room:false,weather:'none',terrain:'none',...seed,enemy};
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(ability,item)=>({species:'Mew',ability,item:item==='none'?'':item,moves:[c.move],movesInfo:[{pp:40,maxPp:40}],level:100});
    b.setPlayer('p1',{team:[enemy?set(c.targetAbility,c.targetItem):set(c.ability,c.item)]});
    b.setPlayer('p2',{team:[enemy?set(c.ability,c.item):set(c.targetAbility,c.targetItem)]});
    b.makeChoices('team 1','team 1');
    const user=enemy?b.p2.active[0]:b.p1.active[0],target=enemy?b.p1.active[0]:b.p2.active[0];
    user.maxhp=user.baseMaxhp=1000;user.hp=500;user.level=c.level;
    target.maxhp=target.baseMaxhp=c.targetMax;target.hp=c.targetHp;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));
    target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.status=c.status==='none'?'':c.status;
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.terrain!=='none')b.field.setTerrain(c.terrain,user);
    const powers=[];const event=b.runEvent;
    b.runEvent=function(...args){const result=event.apply(this,args);if(args[0]==='BasePower')powers.push(result);return result;};
    const values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
      b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;
      const damage=b.actions.getDamage(user,target,move,true);
      if(damage!==false&&typeof damage!=='number')throw Error('Missing native damage: '+c.id);
      values.push({crit,roll,damage:damage===false?0:damage});
    }
    if(c.id==='tech-aura'&&!powers.every(power=>power===100))throw Error('Technician must precede Fairy Aura at base power 50');
    if(c.id==='above-tech'&&!powers.every(power=>power===126))throw Error('Technician must not boost base power 95');
    if(c.id==='double-aura'&&!powers.every(power=>power===67))throw Error('Fairy Aura must not stack');
    rows.push({config:c,values});
  }
  if(rows.length!==66)throw Error('Modifier composition inventory changed');
  return rows;
};

exports.exchanges = function(Battle, rows) {
  const configs=rows.filter(r=>['tech-aura-orb','orb-shield-neutral','aura-orb-shield'].includes(r.config.id)).map(r=>r.config);
  for(const enemy of [false,true])configs.push({...rows.find(r=>r.config.id==='orb-shield-neutral').config,
    id:'shield-after-own-recoil',enemy,ability:'shadowshield',targetItem:'lifeorb',replyMove:'psyshock',userHp:1000});
  const outcomes=[];
  for(const c of configs){
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove||'bulkup'],movesInfo:[{pp:40,maxPp:40}],level:100});
    b.setPlayer('p1',{team:[set(!c.enemy)]});b.setPlayer('p2',{team:[set(c.enemy)]});b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [self,foe]){actor.hp=actor.maxhp=actor.baseMaxhp=1000;actor.types=actor.baseTypes=['Normal'];for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;}
    user.hp=c.userHp||500;user.storedStats.spe=300;target.storedStats.spe=100;
    // Keep guaranteed accuracy successful while excluding random crits/procs.
    b.randomChance=(numerator,denominator)=>numerator>=denominator;b.random=()=>5;
    const beforeSelf=self.hp/1000,beforeFoe=foe.hp/1000;let observed=null;const residual=b.residualEvent;
    b.residualEvent=function(...args){if(!observed)observed={config:c,selfHp:self.hp/1000,foeHp:foe.hp/1000,
      selfStages:{...self.boosts},foeStages:{...foe.boosts},utility:beforeFoe-foe.hp/1000-(beforeSelf-self.hp/1000)};return residual.apply(this,args);};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native composed exchange boundary');
    if(target.hp>=1000)throw Error('Native composed attack did not execute: '+c.id);
    outcomes.push(observed);
  }
  return outcomes;
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  const exchanges=exports.exchanges(require(require('path').resolve(process.argv[2],'sim/battle')).Battle,rows);
  console.log('Modifier composition native oracle PASS: '+rows.length+' configurations, 2112 critical/roll outcomes, '+exchanges.length+' queued exchanges; native callback ordering and fixed-point chaining');
}
