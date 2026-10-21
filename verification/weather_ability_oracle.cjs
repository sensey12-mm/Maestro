'use strict';

// Declared synthetic conditions; original native ability, weather, item and
// faint callbacks. Hidden teams and weather counters are oracle labels only.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'weather-ability',move:'bulkup',replyMove:'bulkup',ability:'raindish',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:219,defense:221,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:301,targetSpeed:201,speedStage:0,targetSpeedStage:0,accuracyStage:0,targetEvasionStage:0,
    room:false,weather:'raindance',weatherMode:'fresh',weatherDuration:5,ownBench:0,foeBench:0,draw:0,...seed,enemy});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n,own)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:own?c.ownBenchAbility||'runaway':'runaway',item:own?c.ownBenchItem||'':'',moves:['bulkup'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
    b.setPlayer('p1',{team:team(!c.enemy,c.ownBench,true)});b.setPlayer('p2',{team:team(c.enemy,c.foeBench,false)});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const side of [b.p1,b.p2])for(const a of side.pokemon){a.hp=a.maxhp=a.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])a.storedStats[stat]=220;}
    user.hp=c.userHp;user.level=c.level;target.hp=c.targetHp;target.maxhp=target.baseMaxhp=c.targetMax;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;user.boosts.spe=c.speedStage;target.boosts.spe=c.targetSpeedStage;
    user.boosts.accuracy=c.accuracyStage;target.boosts.evasion=c.targetEvasionStage;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    if(c.room){b.field.addPseudoWeather('magicroom',user);if(c.roomDuration)b.field.pseudoWeather.magicroom.duration=c.roomDuration;}
    if(c.trickRoom)b.field.addPseudoWeather('trickroom',user);
    for(const [a,wind]of [[user,c.wind],[target,c.targetWind]])if(wind)a.side.addSideCondition('tailwind',a);
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.weatherMode==='upkeep')b.field.weatherState.duration=c.weatherDuration;
    if(c.weatherMode==='ended')b.field.clearWeather();
    if(c.weatherMode==='restart'){b.field.clearWeather();b.field.setWeather(c.restartWeather,user);}
    if(c.weatherMode==='changed')b.field.setWeather(c.weatherNext,user);
    if(c.protect)target.addVolatile('protect');
    return {b,self,foe,user,target};
  }

  const damage=[],exchanges=[],durations=[];
  function damageRow(c){
    const {b,user,target}=battle(c),values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;values.push({crit,roll,damage:b.actions.getDamage(user,target,move,true)});}
    return {config:c,values};
  }
  for(const weather of ['none','sunnyday','raindance','sandstorm'])for(const holder of ['none','user','target'])for(const room of [false,true])
    for(const move of ['fireblast','psyshock','tackle'])for(const attackStage of [-2,1])for(const enemy of [false,true])
      damage.push(damageRow(config({ability:'solarpower',weather,move,attackStage,defenseStage:-attackStage,targetTypes:['rock'],
        item:holder==='user'?'utilityumbrella':'lifeorb',targetItem:holder==='target'?'utilityumbrella':'none'},enemy)));
  for(const attack of [3,101,221])for(const targetHp of [999,1000])for(const enemy of [false,true])
    damage.push(damageRow(config({ability:'solarpower',weather:'sunnyday',move:'psyshock',attack,targetHp,item:'lifeorb',targetAbility:'shadowshield'},enemy)));
  for(const targetAbility of ['airlock','cloudnine'])for(const move of ['fireblast','psyshock'])for(const enemy of [false,true])
    damage.push(damageRow(config({ability:'solarpower',weather:'sunnyday',move,targetAbility,item:'lifeorb'},enemy)));
  function exchange(c){
    const {b,self,foe,target}=battle(c);let pre=null,post=null,checking=false;
    const hit=b.actions.hitStepAccuracy,chance=b.randomChance;b.actions.hitStepAccuracy=function(...args){checking=true;try{return hit.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.draw;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}
      if(n===1&&d===4)return this.event.target===target?!!c.targetBlocked:!!c.blocked;if(n===1&&d===24)return false;return n>=d;};b.random=()=>5;
    const beforeSelf=b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>{const a=b.p1.active[0],f=b.p2.active[0];return {selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',
      utility:beforeFoe-f.hp/f.maxhp-(beforeSelf-b.p1.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
    const residual=b.residualEvent;b.residualEvent=function(...args){pre=snapshot();const result=residual.apply(this,args);post=snapshot();return result;};
    const faint=b.faintMessages;b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!pre)pre=post=snapshot();return result;};
    b.makeChoices('move 1','move 1');
    if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(c.enemy?'':'switch 2',c.enemy?'switch 2':'');
    if(!pre)throw Error('Missing native umbrella exchange boundary: '+JSON.stringify(c));
    return {config:c,pre,post,weatherAfter:b.field.weather||'none',roomAfter:!!b.field.pseudoWeather.magicroom,events:b.log.filter(l=>/^\|-(heal|damage)\|/.test(l)),ended:b.ended,request:b.requestState};
  }

  const seeds=[];
  for(const ability of ['raindish','solarpower'])for(const weather of ['none','sunnyday','raindance','sandstorm'])for(const item of ['none','leftovers','utilityumbrella'])
    for(const room of [false,true])for(const userHp of [1,999])seeds.push({ability,weather,item,room,userHp,ownBench:1});
  for(const targetAbility of ['raindish','solarpower'])for(const targetMax of [7,15,17,31,999,1001])for(const targetHp of [1,targetMax])
    seeds.push({id:'odd-max',ability:'runaway',targetAbility,weather:targetAbility==='raindish'?'raindance':'sunnyday',targetMax,targetHp,targetItem:'leftovers',ownBench:1});
  for(const ability of ['raindish','solarpower'])for(const status of ['brn','psn'])for(const item of ['none','leftovers'])for(const userHp of [1,125,126,999])
    seeds.push({id:'status-order',ability,weather:ability==='raindish'?'raindance':'sunnyday',status,item,userHp,ownBench:1});
  for(const ability of ['raindish','solarpower'])for(const extra of [
    {id:'room-expiry',room:true,roomDuration:1,item:'utilityumbrella'},
    {id:'leftovers-room-expiry',room:true,roomDuration:1,item:'leftovers'},
    {id:'suppressed-airlock',targetAbility:'airlock'},{id:'suppressed-cloudnine',targetAbility:'cloudnine'},
    {id:'ended',weatherMode:'ended'},{id:'restart',weatherMode:'restart'},
    {id:'changed',weatherMode:'changed'},{id:'upkeep-expiry',weatherMode:'upkeep',weatherDuration:1},
    {id:'upkeep-ongoing',weatherMode:'upkeep',weatherDuration:5},
    {id:'upkeep-umbrella',weatherMode:'upkeep',weatherDuration:1,item:'utilityumbrella'},
    {id:'upkeep-suppressed',weatherMode:'upkeep',weatherDuration:1,targetAbility:'airlock'},
    {id:'paralysis',status:'par',blocked:true},{id:'protect',move:'closecombat',protect:true},
    {id:'immune',move:'closecombat',targetTypes:['ghost']},
    {id:'miss',move:'fireblast',draw:99,item:'lifeorb'},
    {id:'drain-cap',move:'drainingkiss',item:'lifeorb',userHp:999},
    {id:'shield',move:'psyshock',item:'lifeorb',targetAbility:'shadowshield'},
    {id:'both-abilities',targetAbility:ability,targetHp:500},
    {id:'inactive-reserve',ownBench:1,ownBenchAbility:ability,ability:'runaway'}])
    seeds.push({ability,weather:ability==='raindish'?'raindance':'sunnyday',restartWeather:ability==='raindish'?'raindance':'sunnyday',weatherNext:ability==='raindish'?'sunnyday':'raindance',...extra});
  for(const ownBench of [0,1])for(const foeBench of [0,1])for(const userHp of [124,125,126])
    seeds.push({id:'weather-ko',ability:'solarpower',weather:'sunnyday',item:'leftovers',userHp,ownBench,foeBench});
  for(const ownBench of [0,1])for(const foeBench of [0,1])
    seeds.push({id:'recoil-ko',ability:'solarpower',weather:'sunnyday',item:'lifeorb',move:'swift',userHp:100,ownBench,foeBench},
      {id:'suppression-faint',ability:'solarpower',targetAbility:'airlock',weather:'sunnyday',move:'swift',targetHp:1,ownBench,foeBench},
      {id:'both-weather-ko',ability:'solarpower',targetAbility:'solarpower',weather:'sunnyday',userHp:125,targetHp:125,ownBench,foeBench});
  for(const ability of ['raindish','solarpower'])seeds.push({id:'pivot-out',move:'uturn',ability,weather:ability==='raindish'?'raindance':'sunnyday',ownBench:1,ownBenchAbility:'solarpower'});
  for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));
  for(const weather of ['sunnyday','raindance'])for(const duration of [1,2,5]){
    const c=config({ability:weather==='sunnyday'?'solarpower':'raindish',userHp:weather==='sunnyday'?1000:500,weather,weatherMode:'upkeep',weatherDuration:duration},false),{b,user}=battle(c);let phases=[];
    for(let i=0;i<duration;i++){b.residualEvent('Residual');phases.push({weather:b.field.weather||'none',hp:user.hp});}
    durations.push({weather,duration,phases});
  }
  if(!exchanges.some(r=>r.ended)||!exchanges.some(r=>r.request==='switch'))throw Error('Missing native termination/replacement boundary');
  return {damage,exchanges,durations};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));console.log('Rain Dish / Solar Power native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,v.length]))));}
