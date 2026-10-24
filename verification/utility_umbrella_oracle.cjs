'use strict';

// Declared synthetic conditions; original native item/weather/move callbacks.
// Duration, cached speed and opponent benches are oracle labels only.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'umbrella',move:'fireblast',replyMove:'bulkup',ability:'chlorophyll',targetAbility:'shadowshield',item:'lifeorb',targetItem:'utilityumbrella',
    attack:219,defense:221,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:101,targetSpeed:201,speedStage:0,targetSpeedStage:0,accuracyStage:0,targetEvasionStage:0,
    room:false,weather:'sunnyday',weatherMode:'fresh',weatherDuration:5,wind:false,targetWind:false,ownBench:0,foeBench:0,draw:0,...seed,enemy});
  const held=holder=>({item:holder==='user'||holder==='both'?'utilityumbrella':'lifeorb',targetItem:holder==='target'||holder==='both'?'utilityumbrella':'leftovers'});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n,own)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:'runaway',item:own?c.ownBenchItem||'':'',moves:['bulkup'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
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
  const damage=[],accuracy=[],speeds=[],exchanges=[];
  function damageRow(c){
    const {b,user,target}=battle(c),values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;values.push({crit,roll,damage:b.actions.getDamage(user,target,move,true)});}
    return {config:c,values};
  }
  for(const weather of ['sunnyday','raindance','sandstorm'])for(const holder of ['user','target','both'])for(const room of [false,true])
    for(const move of ['fireblast','flamethrower','thunder','swift'])for(const stage of [-2,1])for(const enemy of [false,true])
      damage.push(damageRow(config({weather,...held(holder),room,move,attackStage:stage,defenseStage:stage,ability:'technician',targetTypes:['rock'],targetHp:stage<0?1000:999},enemy)));
  for(const ability of ['airlock','cloudnine'])for(const weather of ['sunnyday','raindance'])for(const holder of ['user','target','both'])for(const enemy of [false,true])
    damage.push(damageRow(config({ability,weather,...held(holder)},enemy)));
  function accuracyRow(c){
    const {b,user,target}=battle(c),move=b.dex.getActiveMove(c.move);let threshold=100;
    b.singleEvent('ModifyMove',move,null,user,target,move,move);b.setActiveMove(move,user,target);
    const chance=b.randomChance;b.randomChance=function(n,d){threshold=n;return chance.call(this,n,d);};b.actions.hitStepAccuracy([target],user,move);
    return {config:c,threshold};
  }
  for(const weather of ['sunnyday','raindance','sandstorm'])for(const holder of ['user','target','both'])for(const room of [false,true])
    for(const move of ['thunder','rocktomb','swift'])for(const [accuracyStage,targetEvasionStage]of [[0,0],[-2,0],[6,-6]])for(const enemy of [false,true])
      accuracy.push(accuracyRow(config({weather,...held(holder),room,move,accuracyStage,targetEvasionStage,targetAbility:'sandveil'},enemy)));
  for(const ability of ['airlock','cloudnine'])for(const weather of ['sunnyday','raindance'])for(const holder of ['user','target','both'])for(const accuracyStage of [0,-2])for(const enemy of [false,true])
    accuracy.push(accuracyRow(config({ability,weather,...held(holder),move:'thunder',accuracyStage,targetAbility:'sandveil'},enemy)));
  function speedRow(c){
    const {b,user}=battle(c),values=[],cached=user.speed;
    for(const cache of [cached,0])for(let first=0;first<4;first++)for(let second=0;second<3;second++)for(let third=0;third<2;third++){
      user.speed=cache;let draw=0;b.prng.next=(from,to)=>to===undefined?0:from+([first,second,third][draw++]||0)%(to-from);values.push(user.getStat('spe'));
    }return {config:c,low:Math.min(...values),high:Math.max(...values)};
  }
  for(const ability of ['chlorophyll','swiftswim'])for(const holder of ['user','target','both'])for(const room of [false,true])for(const status of ['none','par'])
    for(const wind of [false,true])for(const userSpeed of [1,3,101])for(const enemy of [false,true])
      speeds.push(speedRow(config({ability,weather:ability==='chlorophyll'?'sunnyday':'raindance',...held(holder),item:held(holder).item==='lifeorb'?'choicescarf':'utilityumbrella',targetAbility:'runaway',room,status,wind,userSpeed},enemy)));
  for(const holder of ['user','target','both'])for(const room of [false,true])for(const enemy of [false,true])speeds.push(speedRow(config({ability:'sandrush',weather:'sandstorm',...held(holder),room,wind:true,status:'par',targetAbility:'runaway'},enemy)));
  for(const ability of ['chlorophyll','swiftswim'])for(const targetAbility of ['airlock','cloudnine'])for(const holder of ['user','target','both'])for(const enemy of [false,true])
    speeds.push(speedRow(config({ability,weather:ability==='chlorophyll'?'sunnyday':'raindance',...held(holder),targetAbility,wind:true,status:'par'},enemy)));
  for(const ability of ['chlorophyll','swiftswim'])for(const weatherMode of ['ended','restart','changed','upkeep'])for(const holder of ['user','target','both'])for(const enemy of [false,true])
    speeds.push(speedRow(config({ability,weather:ability==='chlorophyll'?'sunnyday':'raindance',...held(holder),weatherMode,restartWeather:ability==='chlorophyll'?'sunnyday':'raindance',weatherNext:ability==='chlorophyll'?'raindance':'sunnyday',targetAbility:'runaway'},enemy)));
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
    return {config:c,pre,post,weatherAfter:b.field.weather||'none',roomAfter:!!b.field.pseudoWeather.magicroom};
  }
  const seeds=[];
  for(const weather of ['sunnyday','raindance'])for(const holder of ['user','target','both'])for(const room of [false,true])for(const move of ['fireblast','thunder','rocktomb','drainingkiss','closecombat'])for(const draw of [0,99])
    seeds.push({weather,...held(holder),room,move,draw,ability:weather==='sunnyday'?'chlorophyll':'swiftswim',userHp:move==='drainingkiss'?990:500});
  for(const weather of ['sunnyday','raindance'])for(const holder of ['user','target','both'])for(const extra of [
    {id:'protect',move:'closecombat',protect:true},{id:'immune',move:'closecombat',targetTypes:['ghost']},{id:'room-expiry',room:true,roomDuration:1},
    {id:'trick-room',trickRoom:true},{id:'priority',move:'quickattack'},{id:'paralysis-skip',status:'par',blocked:true},
    {id:'paralysis-execute',status:'par',blocked:false},{id:'airlock',ability:'airlock'},{id:'cloudnine',targetAbility:'cloudnine'},
    {id:'ended',weatherMode:'ended'},{id:'restart',weatherMode:'restart'},{id:'changed',weatherMode:'changed'},{id:'upkeep',weatherMode:'upkeep',weatherDuration:1}])
      seeds.push({weather,...held(holder),ability:weather==='sunnyday'?'chlorophyll':'swiftswim',restartWeather:weather,weatherNext:weather==='sunnyday'?'raindance':'sunnyday',...extra});
  for(const ownBench of [0,1])for(const foeBench of [0,1])for(const weather of ['sunnyday','raindance'])
    seeds.push({id:'recoil-ko',weather,ability:weather==='sunnyday'?'chlorophyll':'swiftswim',userHp:100,move:'swift',ownBench,foeBench});
  for(const userHp of [62,500])for(const ownBench of [0,1])for(const foeBench of [0,1])
    seeds.push({id:'sandstorm',weather:'sandstorm',ability:'runaway',item:'utilityumbrella',targetItem:'leftovers',targetTypes:['rock'],move:'bulkup',userHp,ownBench,foeBench});
  seeds.push({id:'sandstorm-upkeep',weather:'sandstorm',weatherMode:'upkeep',weatherDuration:1,item:'utilityumbrella',targetItem:'utilityumbrella',move:'bulkup',ability:'runaway',targetAbility:'runaway'});
  for(const weather of ['sunnyday','raindance'])seeds.push({id:'reserve-only',weather,ability:weather==='sunnyday'?'chlorophyll':'swiftswim',item:'none',targetItem:'none',ownBench:1,ownBenchItem:'utilityumbrella'});
  seeds.push({id:'pivot-out',move:'uturn',item:'utilityumbrella',targetItem:'none',targetAbility:'runaway',ownBench:1,foeBench:1},
    {id:'pivot-in',move:'uturn',item:'none',targetItem:'none',targetAbility:'runaway',replyMove:'fireblast',ownBench:1,foeBench:1,ownBenchItem:'utilityumbrella'});
  for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));
  for(const row of exchanges.filter(r=>r.config.id==='room-expiry'))if(row.roomAfter)throw Error('Native room expiration fixture failed');
  return {damage,accuracy,speeds,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));console.log('Utility Umbrella native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,v.length]))));}
