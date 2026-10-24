'use strict';
// Declared synthetic actors and native weather callbacks. Bench sizes, counters
// and stats are oracle labels, never confirmed opponent knowledge.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'weather-ability',move:'bulkup',replyMove:'bulkup',ability:'runaway',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:219,defense:221,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:301,targetSpeed:201,speedStage:0,targetSpeedStage:0,accuracyStage:0,targetEvasionStage:0,
    room:false,weather:'snow',weatherMode:'fresh',weatherDuration:5,ownBench:0,foeBench:0,draw:0,...seed,enemy});
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

  const damage=[],speeds=[],accuracy=[],exchanges=[],durations=[],generations=[];
  function damageRow(c){
    const {b,user,target}=battle(c),values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
      b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;
      values.push({crit,roll,damage:b.actions.getDamage(user,target,move,true)});
    }return {config:c,values};
  }
  for(const weather of ['snow','hail'])for(const move of ['tackle','ironhead','psyshock','psystrike','swift'])
    for(const defense of [3,101,221])for(const defenseStage of [-2,0,1])for(const enemy of [false,true])
      damage.push(damageRow(config({weather,move,defense,defenseStage,targetTypes:['ice']},enemy)));
  for(const weather of ['snow','hail'])for(const move of ['tackle','psyshock','rocktomb'])for(const targetTypes of [['ice'],['rock','ice']])
    for(const mods of [{ability:'technician',item:'lifeorb',targetAbility:'shadowshield'},
      {item:'lifeorb',targetAbility:'shadowshield',targetHp:999},{ability:'sandforce',item:'lifeorb',targetAbility:'snowcloak'}])for(const enemy of [false,true])
      damage.push(damageRow(config({weather,move,targetTypes,...mods},enemy)));
  for(const weather of ['snow','hail'])for(const suppressor of ['airlock','cloudnine'])for(const holder of ['user','target'])for(const move of ['tackle','psyshock'])for(const enemy of [false,true])
    damage.push(damageRow(config({weather,move,targetTypes:['ice'],ability:holder==='user'?suppressor:'runaway',targetAbility:holder==='target'?suppressor:'runaway'},enemy)));
  for(const weather of ['snow','hail'])for(const targetItem of ['utilityumbrella','safetygoggles','leftovers'])for(const room of [false,true])for(const move of ['tackle','psystrike'])for(const enemy of [false,true])
    damage.push(damageRow(config({weather,move,targetTypes:['ice'],targetItem,room},enemy)));

  function speedRow(c){
    const {b,user}=battle(c),values=[],cached=user.speed;
    for(const cache of [cached,0])for(let first=0;first<4;first++)for(let second=0;second<3;second++)for(let third=0;third<2;third++){
      user.speed=cache;let draw=0;b.prng.next=(from,to)=>to===undefined?0:from+([first,second,third][draw++]||0)%(to-from);
      values.push(user.getStat('spe'));
    }return {config:c,low:Math.min(...values),high:Math.max(...values)};
  }
  for(const weather of ['snow','hail','none','sandstorm'])for(const userSpeed of [1,3,101])for(const speedStage of [-2,0,1])
    for(const item of ['none','choicescarf'])for(const wind of [false,true])for(const status of ['none','par'])for(const enemy of [false,true])
      speeds.push(speedRow(config({weather,ability:'slushrush',userSpeed,speedStage,item,wind,status},enemy)));
  for(const weather of ['snow','hail'])for(const targetAbility of ['airlock','cloudnine'])for(const item of ['utilityumbrella','choicescarf'])for(const room of [false,true])for(const enemy of [false,true])
    speeds.push(speedRow(config({weather,ability:'slushrush',targetAbility,item,room,wind:true,status:'par',userSpeed:101},enemy)));
  for(const weather of ['snow','hail'])for(const weatherMode of ['ended','restart','changed','upkeep'])for(const enemy of [false,true])
    speeds.push(speedRow(config({weather,weatherMode,restartWeather:weather,weatherNext:weather==='snow'?'hail':'snow',ability:'slushrush',item:'choicescarf',wind:true,status:'par'},enemy)));

  function accuracyRow(c){
    const {b,user,target}=battle(c),move=b.dex.getActiveMove(c.move);let threshold=100;
    b.singleEvent('ModifyMove',move,null,user,target,move,move);b.setActiveMove(move,user,target);
    b.randomChance=(n,d)=>{threshold=n;return n>=d;};b.actions.hitStepAccuracy([target],user,move);
    return {config:c,threshold};
  }
  for(const weather of ['snow','hail','none'])for(const move of ['rocktomb','thunder','fireblast','swift'])for(const [accuracyStage,targetEvasionStage]of [[0,0],[-2,0],[6,-6]])for(const targetItem of ['safetygoggles','utilityumbrella'])for(const enemy of [false,true])
    accuracy.push(accuracyRow(config({weather,move,accuracyStage,targetEvasionStage,targetItem,targetAbility:'snowcloak'},enemy)));
  for(const weather of ['snow','hail'])for(const ability of ['airlock','cloudnine'])for(const move of ['rocktomb','thunder','fireblast','swift'])for(const enemy of [false,true])
    accuracy.push(accuracyRow(config({weather,move,ability,targetAbility:'snowcloak'},enemy)));
  for(const weather of ['snow','hail'])for(const move of ['rocktomb','thunder','fireblast','swift'])for(const enemy of [false,true])
    accuracy.push(accuracyRow(config({weather,move,ability:'snowcloak'},enemy)));

  function exchange(c){
    const {b,self,foe,target}=battle(c);let pre=null,post=null,checking=false;
    const hit=b.actions.hitStepAccuracy,chance=b.randomChance;b.actions.hitStepAccuracy=function(...args){checking=true;try{return hit.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.draw;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}
      if(n===1&&d===4)return this.event.target===target?!!c.targetBlocked:!!c.blocked;if(n===1&&d===24)return false;return n>=d;};b.random=()=>5;
    const beforeSelf=b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>{const a=b.p1.active[0],f=b.p2.active[0];return {active:a===self?'self':'reserve0',selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',
      utility:beforeFoe-f.hp/f.maxhp-(beforeSelf-b.p1.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
    const residual=b.residualEvent;b.residualEvent=function(...args){pre=snapshot();const result=residual.apply(this,args);post=snapshot();return result;};
    const faint=b.faintMessages;b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!pre)pre=post=snapshot();return result;};
    b.makeChoices('move 1','move 1');
    if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(c.enemy?'':'switch 2',c.enemy?'switch 2':'');
    if(!pre)throw Error('Missing native cold-weather exchange boundary: '+JSON.stringify(c));
    return {config:c,pre,post,weatherAfter:b.field.weather||'none',roomAfter:!!b.field.pseudoWeather.magicroom,ended:b.ended};
  }
  const seeds=[];
  for(const weather of ['snow','hail'])for(const ability of ['runaway','slushrush','snowcloak','icebody'])for(const types of [['normal'],['ice']])for(const item of ['none','leftovers','safetygoggles','utilityumbrella'])for(const room of [false,true])
    seeds.push({id:'weather-hp',weather,ability,types,item,room});
  for(const weather of ['snow','hail'])for(const ability of ['icebody','magicguard'])for(const status of ['none','brn','psn'])for(const item of ['none','leftovers'])for(const userHp of [1,62,63,999])
    seeds.push({id:'status-order',weather,ability,status,item,userHp,ownBench:1});
  for(const weather of ['snow','hail'])for(const targetMax of [7,15,17,31,999,1001])for(const targetHp of [1,targetMax])for(const targetItem of ['none','leftovers'])
    seeds.push({id:'odd-max',weather,targetMax,targetHp,targetItem,targetAbility:'icebody'});
  for(const weather of ['snow','hail'])for(const ownBench of [0,1])for(const foeBench of [0,1])for(const userHp of [62,63])
    seeds.push({id:'weather-ko',weather,ownBench,foeBench,userHp});
  for(const ownBench of [0,1])for(const foeBench of [0,1])seeds.push({id:'both-weather-ko',weather:'hail',ownBench,foeBench,userHp:62,targetHp:62});
  for(const weather of ['snow','hail'])for(const weatherDuration of [1,2,5])for(const ability of ['icebody','runaway'])for(const targetAbility of ['runaway','airlock'])
    seeds.push({id:'clock',weather,weatherDuration,ability,targetAbility,weatherMode:'upkeep'});
  for(const weather of ['snow','hail'])for(const weatherMode of ['ended','restart','changed'])
    seeds.push({id:'weather-change',weather,weatherMode,restartWeather:weather,weatherNext:weather==='snow'?'hail':'snow',ability:'icebody',item:'leftovers'});
  for(const weather of ['snow','hail'])for(const targetAbility of ['airlock','cloudnine'])for(const ability of ['icebody','slushrush','snowcloak'])
    seeds.push({id:'suppression',weather,targetAbility,ability,item:'leftovers'});
  for(const weather of ['snow','hail'])for(const extra of [
    {id:'blocked',move:'closecombat',protect:true,item:'lifeorb'},
    {id:'immune',move:'closecombat',targetTypes:['ghost'],item:'lifeorb'},
    {id:'miss',move:'fireblast',draw:99,item:'lifeorb'},
    {id:'drain-cap',move:'drainingkiss',item:'lifeorb',userHp:999},
    {id:'shield',move:'tackle',item:'lifeorb',targetAbility:'shadowshield',targetTypes:['ice']},
    {id:'room-expiry',ability:'icebody',item:'leftovers',room:true,roomDuration:1},
    {id:'slush-priority',ability:'slushrush',move:'quickattack',replyMove:'tackle',userSpeed:100,targetSpeed:301},
    {id:'slush-trickroom',ability:'slushrush',move:'tackle',replyMove:'tackle',trickRoom:true,userSpeed:101,targetSpeed:201},
    {id:'paralysis',ability:'slushrush',status:'par',blocked:true,move:'tackle',replyMove:'tackle'},
    {id:'pivot-out',ability:'slushrush',move:'uturn',ownBench:1,ownBenchAbility:'icebody'}])seeds.push({weather,...extra});
  for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));

  for(const weather of ['snow','hail'])for(const duration of [1,2,5]){
    const c=config({ability:'icebody',userHp:500,weather,weatherMode:'upkeep',weatherDuration:duration},false),{b,user}=battle(c),phases=[];
    for(let i=0;i<duration;i++){b.residualEvent('Residual');phases.push({weather:b.field.weather||'none',hp:user.hp});}
    durations.push({weather,duration,phases});
  }
  for(const gen of [8,9]){
    const b=new Battle({formatid:'gen'+gen+'customgame',seed:[7,11,13,17]});
    const set=ability=>({species:'Mew',ability,moves:['splash'],movesInfo:[{pp:40,maxPp:40}],level:100});
    b.setPlayer('p1',{team:[set('snowwarning')]});b.setPlayer('p2',{team:[set('runaway')]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const observed=b.field.weather;generations.push({gen,snowWarning:observed});
    if(observed!==(gen===8?'hail':'snow'))throw Error('Native Snow Warning generation boundary changed');
  }
  return {damage,speeds,accuracy,exchanges,durations,generations};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const data=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(data));console.log('Cold weather native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(data).map(([k,v])=>[k,v.length]))));}
