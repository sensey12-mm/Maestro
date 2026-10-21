'use strict';

// Declared synthetic conditions; original native weather, damage and residual
// dispatch. Weather counters and opponent benches are oracle labels only.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'sandstorm',move:'bulkup',replyMove:'bulkup',ability:'runaway',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:500,userHp:500,userSpeed:300,targetSpeed:100,room:false,weather:'sandstorm',ownBench:0,foeBench:0,
    weatherMode:'fresh',weatherDuration:5,...seed,enemy});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:'Run Away',moves:['Bulk Up'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
    b.setPlayer('p1',{team:team(!c.enemy,c.ownBench)});b.setPlayer('p2',{team:team(c.enemy,c.foeBench)});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [self,foe]){actor.maxhp=actor.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;}
    user.hp=c.userHp;user.level=c.level;target.hp=c.targetHp;target.maxhp=target.baseMaxhp=c.targetMax;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.protect)target.addVolatile('protect');
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    const startDuration=b.field.weatherState.duration;
    if(c.weather==='sandstorm'){
      const expected=c.item==='smoothrock'&&!c.room?8:5;
      if(startDuration!==expected)throw Error('Pinned native weather duration changed');
      b.field.weatherState.duration=c.weatherDuration;
    }
    if(c.weatherMode==='ended')b.field.clearWeather();
    if(c.weatherMode==='restart'){b.field.clearWeather();b.field.setWeather('sandstorm',user);}
    if(c.terrain)b.field.setTerrain(c.terrain,user);
    return {b,self,foe,user,target,startDuration};
  }
  const durations=[];
  for(const item of ['none','smoothrock'])for(const room of [false,true])for(const enemy of [false,true]){
    const c=config({item,room},enemy);durations.push({item,room,enemy,duration:battle(c).startDuration});
  }
  const damageSeeds=[];
  for(const move of ['swift','psystrike','psyshock','fireblast','drainingkiss','tackle'])for(const defenseStage of [-2,0,2])for(const targetAbility of ['runaway','shadowshield'])
    damageSeeds.push({id:'weather-defense',move,defenseStage,targetAbility,targetTypes:['rock'],targetHp:1000,item:'lifeorb',
      ability:move==='drainingkiss'?'fairyaura':'technician',types:move==='fireblast'?['fire']:['normal']});
  for(const targetTypes of [['normal'],['ground'],['steel'],['rock','steel']])damageSeeds.push({id:'typing',move:'swift',targetTypes,targetHp:1000});
  for(const defense of [101,103,219,221,413])damageSeeds.push({id:'stat-rounding',move:'flamethrower',targetTypes:['rock'],defense,attack:321,targetHp:1000});
  for(const weather of ['none','sunnyday','raindance'])damageSeeds.push({id:'fire-baseline',move:'fireblast',weather,targetTypes:['rock'],targetHp:1000});
  const damage=[];
  for(const seed of damageSeeds)for(const enemy of [false,true]){
    const c=config(seed,enemy),{b,user,target}=battle(c),values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
      b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;
      const native=b.actions.getDamage(user,target,move,true);
      if(typeof native!=='number'&&native!==false)throw Error('Missing native sandstorm damage');
      values.push({crit,roll,damage:native===false?0:native});
    }
    damage.push({config:c,values});
  }
  function exchange(c){
    const {b,self,foe}=battle(c);let checking=false,observed=null;
    const accuracy=b.actions.hitStepAccuracy,chance=b.randomChance;
    b.actions.hitStepAccuracy=function(...args){checking=true;try{return accuracy.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.miss?99:0;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}return n===1&&d===24?!!c.critical:true;};
    b.random=n=>n===100?0:5;
    const beforeSelf=self.hp/self.maxhp,beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>({selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,selfStages:{...self.boosts},foeStages:{...foe.boosts},
      selfStatus:self.status||'none',foeStatus:foe.status||'none',utility:beforeFoe-foe.hp/foe.maxhp-(beforeSelf-self.hp/self.maxhp)});
    const residual=b.residualEvent;
    b.residualEvent=function(...args){const result=residual.apply(this,args);observed=snapshot();return result;};
    const faint=b.faintMessages;
    b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!observed)observed=snapshot();return result;};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native sandstorm end-of-turn boundary');
    return {config:c,...observed,ended:b.ended,request:b.requestState,weatherAfter:b.field.weather||'none'};
  }
  const seeds=[];
  for(const type of ['normal','rock','ground','steel'])for(const targetType of ['normal','rock','ground','steel'])seeds.push({id:'immunity',types:[type],targetTypes:[targetType]});
  for(const userHp of [1,61,62,63,500,999,1000])for(const ability of ['runaway','magicguard'])for(const status of ['none','brn','psn'])
    seeds.push({id:'recovery',userHp,ability,status,item:'leftovers',targetItem:'leftovers',targetTypes:['rock']});
  for(const targetMax of [7,17,31,142,1001])for(const targetHp of [1,targetMax])seeds.push({id:'odd-hp',targetMax,targetHp,targetItem:'leftovers',types:['steel']});
  for(const userHp of [1,62,63])for(const targetHp of [1,500])for(const ownBench of [0,1])for(const foeBench of [0,1])
    seeds.push({id:'weather-knockout',userHp,targetHp,ownBench,foeBench,item:'leftovers',targetItem:'leftovers'});
  for(const weatherDuration of [1,2,5,8])seeds.push({id:'unproved-duration',weatherDuration,weatherMode:'upkeep',item:'leftovers',targetItem:'leftovers'});
  seeds.push({id:'expired-immune',weatherDuration:1,weatherMode:'upkeep',types:['rock'],targetTypes:['steel']},
    {id:'weather-ended',weatherMode:'ended',status:'psn',item:'leftovers'},
    {id:'weather-restarted',weatherMode:'restart',status:'psn',item:'leftovers'},
    {id:'shield-weather-chip',targetAbility:'shadowshield',targetHp:1000,ability:'magicguard'},
    {id:'drain-cap',move:'drainingkiss',item:'lifeorb',ability:'fairyaura',targetAbility:'shadowshield',targetTypes:['rock'],targetHp:1000,userHp:999,status:'psn'},
    {id:'miss',move:'fireblast',item:'lifeorb',miss:true,targetItem:'leftovers'},
    {id:'blocked',move:'closecombat',protect:true,item:'lifeorb',targetItem:'leftovers'},
    {id:'type-immune',move:'closecombat',targetTypes:['ghost'],item:'lifeorb'},
    {id:'burn-physical',move:'tackle',status:'brn',item:'lifeorb',targetTypes:['rock']},
    {id:'terrain',move:'psystrike',terrain:'psychicterrain',item:'lifeorb',targetTypes:['rock']});
  const exchanges=[];for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));
  for(const row of exchanges.filter(r=>r.config.id==='unproved-duration'&&r.config.weatherDuration===1))if(row.weatherAfter!=='none')throw Error('Native weather expiration fixture failed');
  if(!exchanges.some(r=>r.config.id==='weather-knockout'&&r.selfHp===0&&r.foeHp===0))throw Error('Weather handler failed to damage both actives before faint processing');
  return {durations,damage,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const data=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(data));
  console.log('Sandstorm native oracle PASS: '+data.durations.length+' duration factories, '+data.damage.length+' damage configurations, '+data.damage.reduce((n,r)=>n+r.values.length,0)+' critical/roll outcomes, '+data.exchanges.length+' queued outcomes');
}
