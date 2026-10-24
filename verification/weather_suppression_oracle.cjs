'use strict';

// Declared synthetic inputs; original native weather, hooks and faint dispatch.
// Weather counters and hidden opponent reserves are oracle labels only.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'suppression',move:'rocktomb',replyMove:'bulkup',ability:'sandforce',targetAbility:'airlock',item:'lifeorb',targetItem:'leftovers',
    attack:220,defense:219,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:300,targetSpeed:100,speedStage:0,targetSpeedStage:0,accuracyStage:0,targetEvasionStage:0,
    room:false,weather:'sandstorm',weatherMode:'fresh',weatherDuration:5,ownBench:0,foeBench:0,wind:false,targetWind:false,draw:0,...seed,enemy});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n,own)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:own&&c.ownBenchAbility?c.ownBenchAbility:'runaway',moves:['bulkup'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
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
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    for(const [a,wind]of [[user,c.wind],[target,c.targetWind]])if(wind)a.side.addSideCondition('tailwind',a);
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.weather==='sandstorm'){if(b.field.weatherState.duration!==5)throw Error('Suppression changed native weather initialization');b.field.weatherState.duration=c.weatherDuration;}
    if(c.weatherMode==='ended')b.field.clearWeather();
    if(c.weatherMode==='restart'){b.field.clearWeather();b.field.setWeather('sandstorm',user);}
    if(c.protect)target.addVolatile('protect');
    return {b,self,foe,user,target};
  }
  const suppressors=['airlock','cloudnine'],damage=[],accuracy=[],speeds=[],exchanges=[];
  for(const suppressor of suppressors)for(const holder of ['user','target'])for(const weather of ['sandstorm','sunnyday','raindance'])
    for(const move of ['rocktomb','ironhead','swift','fireblast','thunder','psyshock'])for(const stage of [-2,1])for(const enemy of [false,true]){
      const c=config({id:'damage',weather,move,attackStage:stage,defenseStage:stage,targetTypes:['rock'],
        ability:holder==='user'?suppressor:'sandforce',targetAbility:holder==='target'?suppressor:'shadowshield'},enemy);
      const {b,user,target}=battle(c),values=[];
      if(b.field.weather!==weather||b.field.effectiveWeather()!=='')throw Error('Native suppression cleared raw weather or failed');
      for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
        b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;
        values.push({crit,roll,damage:b.actions.getDamage(user,target,move,true)});
      }
      damage.push({config:c,values});
    }
  for(const suppressor of suppressors)for(const holder of ['user','target'])for(const weather of ['sandstorm','sunnyday','raindance'])
    for(const move of ['rocktomb','thunder','fireblast','swift','tackle'])for(const [accuracyStage,targetEvasionStage]of [[0,0],[-2,0],[6,-6]])for(const enemy of [false,true]){
      const c=config({id:'accuracy',weather,move,accuracyStage,targetEvasionStage,
        ability:holder==='user'?suppressor:'sandveil',targetAbility:holder==='target'?suppressor:'sandveil'},enemy);
      const {b,user,target}=battle(c),moveData=b.dex.getActiveMove(move);let threshold=100;
      b.singleEvent('ModifyMove',moveData,null,user,target,moveData,moveData);b.setActiveMove(moveData,user,target);
      const chance=b.randomChance;b.randomChance=function(n,d){threshold=n;return chance.call(this,n,d);};
      b.actions.hitStepAccuracy([target],user,moveData);accuracy.push({config:c,threshold});
    }
  function speed(c){
    const {b,user}=battle(c),values=[],cached=user.speed;
    for(const cache of [cached,0])for(let first=0;first<4;first++)for(let second=0;second<3;second++)for(let third=0;third<2;third++){
      user.speed=cache;let draw=0;b.prng.next=(from,to)=>to===undefined?0:from+([first,second,third][draw++]||0)%(to-from);
      values.push(user.getStat('spe'));
    }return {config:c,low:Math.min(...values),high:Math.max(...values)};
  }
  for(const targetAbility of suppressors)for(const userSpeed of [1,3,101])for(const speedStage of [-2,0,1])for(const item of ['none','choicescarf'])
    for(const wind of [false,true])for(const status of ['none','par'])for(const enemy of [false,true])
      speeds.push(speed(config({id:'speed',ability:'sandrush',targetAbility,userSpeed,speedStage,item,wind,status},enemy)));
  for(const targetAbility of suppressors)for(const weatherMode of ['ended','restart','upkeep'])for(const enemy of [false,true])
    speeds.push(speed(config({id:'speed-weather',ability:'sandrush',targetAbility,item:'choicescarf',wind:true,status:'par',userSpeed:101,weatherMode},enemy)));
  function exchange(c){
    const {b,self,foe}=battle(c);let pre=null,post=null,checking=false;
    const accuracy=b.actions.hitStepAccuracy,chance=b.randomChance;
    b.actions.hitStepAccuracy=function(...args){checking=true;try{return accuracy.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.draw;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}
      if(n===1&&d===4)return !!c.paralyzed;if(n===1&&d===24)return !!c.critical;return n>=d;};
    b.random=n=>n===100?0:5;
    const initialOwn=self.hp/self.maxhp+b.p1.pokemon.slice(1).reduce((n,a)=>n+a.hp/a.maxhp,0),initialFoe=foe.hp/foe.maxhp;
    const snapshot=()=>{const a=b.p1.active[0],f=b.p2.active[0];return {selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',
      utility:initialFoe-f.hp/f.maxhp-(initialOwn-b.p1.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
    const residual=b.residualEvent;b.residualEvent=function(...args){pre=snapshot();const result=residual.apply(this,args);post=snapshot();return result;};
    const faint=b.faintMessages;b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!pre)pre=post=snapshot();return result;};
    b.makeChoices('move 1','move 1');
    if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(c.enemy?'':'switch 2',c.enemy?'switch 2':'');
    if(!pre)throw Error('Native suppression exchange boundary missing: '+JSON.stringify(c));
    return {config:c,pre,post,weatherAfter:b.field.weather||'none',effectiveAfter:b.field.effectiveWeather()||'none',ended:b.ended};
  }
  const seeds=[];
  for(const suppressor of suppressors)for(const holder of ['user','target'])for(const weather of ['sandstorm','sunnyday','raindance'])for(const move of ['rocktomb','fireblast','thunder','swift'])for(const draw of [0,99])
    seeds.push({id:'exchange',weather,move,draw,ability:holder==='user'?suppressor:'sandforce',targetAbility:holder==='target'?suppressor:'sandveil'});
  for(const targetAbility of suppressors)for(const weather of ['sandstorm','sunnyday','raindance'])for(const item of ['lifeorb','leftovers','safetygoggles'])for(const status of ['none','brn','psn'])
    seeds.push({id:'status',targetAbility,weather,item,status,ability:'magicguard'});
  for(const targetAbility of suppressors)for(const weather of ['sandstorm','sunnyday','raindance'])
    seeds.push({id:'room',targetAbility,weather,item:'safetygoggles',targetItem:'safetygoggles',room:true,status:'psn'});
  for(const ability of suppressors)for(const ownBench of [0,1])for(const foeBench of [0,1])
    seeds.push({id:'recoil-ko',ability,targetAbility:'runaway',move:'swift',userHp:100,ownBench,foeBench});
  for(const ability of suppressors)for(const enemy of [false,true])exchanges.push(exchange(config({id:'pivot-out',move:'uturn',ability,targetAbility:'runaway',ownBench:1,item:'none'},enemy)));
  for(const targetAbility of suppressors)for(const weatherMode of ['ended','restart','upkeep'])
    seeds.push({id:'weather-clock',targetAbility,weatherMode,weatherDuration:1,move:'thunder',item:'leftovers'});
  for(const targetAbility of suppressors)for(const paralyzed of [false,true])seeds.push({id:'paralysis',ability:'sandrush',targetAbility,item:'choicescarf',wind:true,status:'par',paralyzed});
  for(const ownBenchAbility of suppressors)seeds.push({id:'reserve-only',ability:'sandforce',targetAbility:'runaway',ownBench:1,ownBenchAbility});
  for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));
  for(const row of exchanges.filter(r=>r.config.id==='weather-clock'&&r.config.weatherMode==='upkeep'))if(row.weatherAfter!=='none')throw Error('Suppression stopped native weather expiration');
  for(const row of exchanges.filter(r=>r.config.id==='pivot-out'&&!r.config.enemy))if(row.effectiveAfter!=='sandstorm')throw Error('Native pivot failed to restore weather after suppressor left');
  return {damage,accuracy,speeds,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));
  console.log('Weather suppression native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,v.length]))));
}
