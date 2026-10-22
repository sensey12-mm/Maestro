'use strict';

// Declared synthetic actors; native speed callbacks, weather and queued actions.
// Cached speed, weather duration and hidden benches are oracle labels only.
exports.run=function(Battle){
  const weatherFor=a=>a==='chlorophyll'?'sunnyday':'raindance';
  const otherWeather=a=>a==='chlorophyll'?'raindance':'sunnyday';
  const abilities=['chlorophyll','swiftswim'];
  const config=(seed,enemy)=>({id:'weather-speed',move:'rocktomb',replyMove:'bulkup',ability:'chlorophyll',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:220,defense:219,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:500,userHp:500,userSpeed:101,targetSpeed:201,speedStage:0,targetSpeedStage:0,room:false,trickRoom:false,
    weather:'sunnyday',weatherMode:'fresh',weatherDuration:5,wind:false,targetWind:false,windEnded:false,ownBench:0,foeBench:0,...seed,enemy});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:'runaway',moves:['bulkup'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
    b.setPlayer('p1',{team:team(!c.enemy,c.ownBench)});b.setPlayer('p2',{team:team(c.enemy,c.foeBench)});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const side of [b.p1,b.p2])for(const a of side.pokemon){a.hp=a.maxhp=a.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])a.storedStats[stat]=220;}
    user.hp=c.userHp;user.level=c.level;target.hp=c.targetHp;target.maxhp=target.baseMaxhp=c.targetMax;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;user.boosts.spe=c.speedStage;target.boosts.spe=c.targetSpeedStage;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.trickRoom)b.field.addPseudoWeather('trickroom',user);
    for(const [a,wind]of [[user,c.wind],[target,c.targetWind]])if(wind){a.side.addSideCondition('tailwind',a);if(c.windEnded)a.side.removeSideCondition('tailwind');}
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.weatherMode==='upkeep')b.field.weatherState.duration=c.weatherDuration;
    if(c.weatherMode==='ended')b.field.clearWeather();
    if(c.weatherMode==='restart'){b.field.clearWeather();b.field.setWeather(c.restartWeather,user);}
    if(c.weatherMode==='changed')b.field.setWeather(c.weatherNext,user);
    if(c.protect)target.addVolatile('protect');
    return {b,self,foe,user,target};
  }
  function speed(c){
    const {b,user}=battle(c),values=[],cached=user.speed;
    // Exhaust native Fisher-Yates choices for Scarf, the ability, paralysis and
    // Tailwind, including the zero-cache tie; no speed handler is replaced.
    for(const cache of [cached,0])for(let first=0;first<4;first++)for(let second=0;second<3;second++)for(let third=0;third<2;third++){
      user.speed=cache;let draw=0;b.prng.next=(from,to)=>to===undefined?0:from+([first,second,third][draw++]||0)%(to-from);
      values.push(user.getStat('spe'));
    }return {config:c,low:Math.min(...values),high:Math.max(...values)};
  }
  const speeds=[],boundaries=[],exchanges=[];
  for(const ability of abilities)for(const weather of [weatherFor(ability),otherWeather(ability)])for(const userSpeed of [1,3,101])for(const speedStage of [-2,0,1])
    for(const item of ['none','choicescarf'])for(const wind of [false,true])for(const status of ['none','par'])for(const enemy of [false,true])
      speeds.push(speed(config({ability,weather,userSpeed,speedStage,item,wind,status},enemy)));
  for(const ability of abilities)for(const weather of ['none','sandstorm'])for(const enemy of [false,true])speeds.push(speed(config({ability,weather,item:'choicescarf',wind:true,status:'par'},enemy)));
  for(const ability of abilities)for(const targetAbility of ['airlock','cloudnine'])for(const userSpeed of [3,101])for(const item of ['none','choicescarf'])
    for(const wind of [false,true])for(const status of ['none','par'])for(const enemy of [false,true])
      speeds.push(speed(config({ability,weather:weatherFor(ability),targetAbility,userSpeed,item,wind,status},enemy)));
  for(const ability of abilities)for(const weatherMode of ['ended','restart','changed','upkeep'])for(const targetAbility of ['runaway','airlock','cloudnine'])for(const windEnded of [false,true])for(const enemy of [false,true])
    speeds.push(speed(config({ability,weather:weatherFor(ability),weatherMode,restartWeather:weatherFor(ability),weatherNext:otherWeather(ability),targetAbility,windEnded,item:'choicescarf',wind:true,status:'par'},enemy)));
  for(const ability of abilities)for(const targetAbility of ['runaway','airlock','cloudnine'])for(const status of ['none','par'])for(const enemy of [false,true])
    speeds.push(speed(config({ability,weather:weatherFor(ability),targetAbility,status,item:'choicescarf',wind:true,room:true},enemy)));
  for(const ability of abilities)for(const status of ['slp','frz','brn','psn','tox'])for(const enemy of [false,true])
    speeds.push(speed(config({ability,weather:weatherFor(ability),status,item:'choicescarf',wind:true},enemy)));
  for(const ability of abilities)for(const room of [false,true])for(const enemy of [false,true])boundaries.push(speed(config({ability,weather:weatherFor(ability),item:'utilityumbrella',room},enemy)));
  for(const ability of abilities)for(const weather of ['desolateland','primordialsea','snow','hail'])for(const enemy of [false,true])boundaries.push(speed(config({ability,weather},enemy)));
  for(const ability of abilities)for(const enemy of [false,true])boundaries.push(speed(config({ability,weather:weatherFor(ability),targetAbility:'neutralizinggas'},enemy)));
  function exchange(c){
    const {b,self,foe,target}=battle(c);let observed=null,checking=false;const order=[];
    b.prng.next=(from,to)=>to===undefined?0:c.nativeTie?to-1:from;
    const accuracy=b.actions.hitStepAccuracy,chance=b.randomChance;
    b.actions.hitStepAccuracy=function(...args){checking=true;try{return accuracy.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.miss?99:0;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}
      if(n===1&&d===4)return this.event.target===target?!!c.targetBlocked:!!c.blocked;
      if(n===1&&d===24)return false;return n>=d;};b.random=()=>5;
    const beforeSelf=b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>({config:c,selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,selfStages:{...self.boosts},foeStages:{...foe.boosts},
      selfStatus:self.status||'none',foeStatus:foe.status||'none',order,utility:beforeFoe-foe.hp/foe.maxhp-(beforeSelf-b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0))});
    const run=b.actions.runMove;b.actions.runMove=function(move,a,...args){order.push(a===self?'self':'opponent');return run.call(this,move,a,...args);};
    const residual=b.residualEvent;b.residualEvent=function(...args){observed=snapshot();return residual.apply(this,args);};
    const faint=b.faintMessages;b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!observed)observed=snapshot();return result;};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native weather-speed boundary: '+JSON.stringify(c));return observed;
  }
  const variants=[{}, {item:'choicescarf'}, {wind:true}, {item:'choicescarf',wind:true,status:'par'},
    {status:'par',blocked:true}, {weatherMode:'ended'}, {weatherMode:'restart'}, {weatherMode:'changed'}, {weatherMode:'upkeep',weatherDuration:1},
    {wind:true,windEnded:true}, {item:'choicescarf',room:true}, {trickRoom:true}, {move:'quickattack',userSpeed:1}, {replyMove:'quickattack'},
    {targetAbility:'prankster'}, {userSpeed:3,targetSpeed:3,item:'choicescarf',wind:true,status:'par'},
    {move:'psyshock',targetAbility:'shadowshield',targetHp:1000,item:'lifeorb'}, {move:'drainingkiss',userHp:990,item:'lifeorb'},
    {move:'closecombat',item:'lifeorb',protect:true}, {move:'closecombat',targetTypes:['ghost'],item:'lifeorb'}, {move:'rocktomb',miss:true,item:'lifeorb'}];
  const seeds=[];
  for(const ability of abilities){
    for(const v of variants)seeds.push({ability,weather:weatherFor(ability),restartWeather:weatherFor(ability),weatherNext:otherWeather(ability),...v});
    seeds.push({ability,weather:otherWeather(ability)});
    for(const targetAbility of ['airlock','cloudnine'])for(const extra of [{item:'choicescarf',wind:true},{item:'choicescarf',wind:true,status:'par'},{item:'choicescarf',room:true}])
      seeds.push({id:'suppressed',ability,weather:weatherFor(ability),targetAbility,...extra});
    for(const weather of ['sunnyday','raindance'])for(const targetBlocked of [false,true])
      seeds.push({id:'both-abilities',ability,targetAbility:ability==='chlorophyll'?'swiftswim':'chlorophyll',weather,item:'choicescarf',wind:true,status:'par',targetStatus:'par',targetItem:'choicescarf',targetWind:true,targetBlocked});
    for(const ownBench of [0,1])for(const foeBench of [0,1])seeds.push({id:'recoil-ko',ability,weather:weatherFor(ability),move:'swift',item:'lifeorb',userHp:100,ownBench,foeBench});
    seeds.push({id:'order-wrap',ability,weather:weatherFor(ability),item:'choicescarf',wind:true,userSpeed:1000,trickRoom:true},
      {id:'order-wrap',ability,weather:weatherFor(ability),item:'choicescarf',wind:true,userSpeed:1500});
  }
  for(const seed of seeds)for(const enemy of [false,true])for(const nativeTie of [false,true])exchanges.push(exchange(config({...seed,nativeTie},enemy)));
  return {speeds,boundaries,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));
  console.log('Sun/rain speed native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,v.length]))));
}
