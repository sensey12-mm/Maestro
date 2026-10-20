'use strict';

// Artificial conditional teams and stats; native callbacks remain unmodified.
exports.run = function(Battle) {
  {
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=()=>({species:'Mew',ability:'runaway',moves:['tailwind'],movesInfo:[{pp:10,maxPp:10}],level:100});
    b.setPlayer('p1',{team:[set()]});b.setPlayer('p2',{team:[set()]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    b.makeChoices('move 1','move 1');
    if(b.turn!==2||![b.p1,b.p2].every(side=>side.sideConditions.tailwind&&side.active[0].hp===side.active[0].maxhp
      &&Object.values(side.active[0].boosts).every(n=>n===0)))throw Error('Native past Tailwind setter changed non-field state');
  }
  const config=(seed,enemy)=>({move:'tackle',replyMove:'bulkup',ability:'runaway',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'par',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:301,targetSpeed:200,speedStage:0,targetSpeedStage:0,
    room:false,weather:'none',wind:false,targetWind:false,windEnded:false,trickRoom:false,blocked:false,secondary:false,...seed,enemy});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    b.setPlayer('p1',{team:[set(!c.enemy)]});b.setPlayer('p2',{team:[set(c.enemy)]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [self,foe]){actor.maxhp=actor.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;}
    user.hp=c.userHp;target.hp=c.targetHp;target.maxhp=target.baseMaxhp=c.targetMax;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;user.boosts.spe=c.speedStage;target.boosts.spe=c.targetSpeedStage;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    for(const actor of [user,target])if(actor.status==='slp')actor.statusState={id:'slp',target:actor,time:c.sleepTime,startTime:c.sleepTime};
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.trickRoom)b.field.addPseudoWeather('trickroom',user);
    for(const [actor,wind] of [[user,c.wind],[target,c.targetWind]])if(wind){actor.side.addSideCondition('tailwind',actor);if(c.windEnded)actor.side.removeSideCondition('tailwind');}
    if(c.protect)target.addVolatile('protect');
    if(c.terrain)b.field.setTerrain(c.terrain,user);
    if(c.cacheZero)user.speed=0;
    return {b,self,foe,user,target};
  }
  function speedSamples(c){
    const {b,user}=battle(c),values=[];
    // Exhaust the native Fisher-Yates choices for up to three callbacks and
    // positive/zero cached speeds. Sorter and condition callbacks are unchanged.
    const cached=user.speed;
    for(const cache of [cached,0])for(let first=0;first<3;first++)for(let second=0;second<2;second++){
      user.speed=cache;let draw=0;
      b.prng.next=(from,to)=>to===undefined?0:from+([first,second][draw++]||0)%(to-from);
      values.push(user.getStat('spe'));
    }
    return {config:c,values,low:Math.min(...values),high:Math.max(...values)};
  }
  const speeds=[];
  for(const userSpeed of [1,3,101,301])for(const speedStage of [-2,0,1])for(const item of ['none','choicescarf'])for(const wind of [false,true])for(const enemy of [false,true]){
    const c=config({id:'speed-chain',userSpeed,speedStage,item,wind},enemy);
    speeds.push(speedSamples(c));
  }
  for(const status of ['none','slp','frz','brn','psn','tox'])for(const room of [false,true])for(const enemy of [false,true]){
    const c=config({id:'status-speed-only',status,item:'choicescarf',wind:true,room,sleepTime:2},enemy);
    speeds.push(speedSamples(c));
  }
  const seeds=[
    {id:'plain'}, {id:'scarf',item:'choicescarf'}, {id:'wind',wind:true}, {id:'combined',item:'choicescarf',wind:true},
    {id:'odd-order-boundary',userSpeed:3,targetSpeed:4,item:'choicescarf',wind:true},
    {id:'tied-speed',userSpeed:301,targetSpeed:150},
    {id:'scarf-suppressed',item:'choicescarf',room:true}, {id:'wind-ended',wind:true,windEnded:true},
    {id:'trick-room',item:'choicescarf',wind:true,trickRoom:true},
    {id:'priority',move:'quickattack',userSpeed:3}, {id:'priority-reply',replyMove:'quickattack',item:'choicescarf',wind:true},
    {id:'prankster-setup',move:'bulkup',ability:'prankster'}, {id:'recover',move:'recover'},
    {id:'herb',move:'geomancy',item:'powerherb'},
    {id:'close-combat',move:'closecombat',item:'lifeorb'}, {id:'protect',move:'closecombat',item:'lifeorb',protect:true},
    {id:'immunity',move:'closecombat',targetTypes:['ghost'],item:'lifeorb'},
    {id:'drain-cap',move:'drainingkiss',item:'lifeorb',userHp:990},
    {id:'drain-low',move:'drainingkiss',item:'lifeorb',userHp:1,attack:1000},
    {id:'rock-tomb',move:'rocktomb',item:'choicescarf',wind:true},
    {id:'thunder',move:'thunder',secondary:true,wind:true},
    {id:'ice-beam',move:'icebeam',secondary:true,wind:true},
    {id:'psychic-terrain',move:'quickattack',terrain:'psychicterrain',item:'lifeorb'},
    {id:'shield',move:'psyshock',targetAbility:'shadowshield',item:'lifeorb'},
    {id:'opponent-wind',targetWind:true,targetItem:'choicescarf'},
    {id:'both-par',targetStatus:'par',targetBlocked:false},
    {id:'both-par-blocked',targetStatus:'par',targetBlocked:true},
    {id:'cached-zero',item:'choicescarf',wind:true,userSpeed:101,cacheZero:true}
  ];
  function exchange(c){
    const {b,self,foe,user,target}=battle(c);let observed=null;
    if(Object.hasOwn(c,'nativeTie'))b.prng.next=(from,to)=>to===undefined?0:c.nativeTie?to-1:from;
    b.randomChance=function(n,d){
      if(n===1&&d===4)return this.event.target===target?!!c.targetBlocked:c.blocked;
      if(n===1&&d===5)return !!c.thaw;
      if(d===100)return n>=50||c.secondary; // keep supported accuracy separate from optional secondaries
      if(n===1&&d===24)return false;
      return n>=d||c.secondary;
    };b.random=()=>5;
    const ownBefore=self.hp/self.maxhp,foeBefore=foe.hp/foe.maxhp,order=[];
    const run=b.actions.runMove;
    b.actions.runMove=function(move,actor,...args){order.push(actor===self?'self':'opponent');return run.call(this,move,actor,...args);};
    const residual=b.residualEvent;
    b.residualEvent=function(...args){if(!observed)observed={config:c,selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,
      selfStages:{...self.boosts},foeStages:{...foe.boosts},selfStatus:self.status||'none',foeStatus:foe.status||'none',order,
      utility:foeBefore-foe.hp/foe.maxhp-(ownBefore-self.hp/self.maxhp)};return residual.apply(this,args);};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native status pre-residual boundary: '+c.id);
    observed.cant=b.log.filter(l=>l.startsWith('|cant|'));
    if(c.status==='par'&&c.blocked&&!observed.cant.some(l=>l.includes(c.enemy?'p2a:':'p1a:')&&l.endsWith('|par')))
      throw Error('Paralysis nonexecution was not forced: '+c.id);
    if(['thunder','ice-beam'].includes(c.id)&&!c.blocked
      &&(c.enemy?observed.selfStatus:observed.foeStatus)!==(c.id==='thunder'?'par':'frz'))
      throw Error('Executed paralyzed move lost its native secondary: '+c.id);
    return observed;
  }
  const exchanges=[];
  for(const seed of seeds)for(const blocked of [false,true])for(const enemy of [false,true])for(const nativeTie of [false,true])exchanges.push(exchange(config({...seed,blocked,nativeTie},enemy)));
  const boundaries=[];
  for(const enemy of [false,true]){
    for(const move of ['tackle','quickattack','bulkup'])for(const sleepTime of [1,2])
      boundaries.push(exchange(config({id:'sleep-counter',status:'slp',move,sleepTime},enemy)));
    for(const thaw of [false,true])for(const move of ['tackle','quickattack','bulkup'])
      boundaries.push(exchange(config({id:'freeze-recovery',status:'frz',move,thaw},enemy)));
    boundaries.push(exchange(config({id:'incoming-fire-thaws',status:'frz',replyMove:'flamethrower',userSpeed:100,targetSpeed:300},enemy)));
  }
  if(!boundaries.some(r=>r.config.id==='sleep-counter'&&r.config.sleepTime===1&&(r.config.enemy?r.foeStatus:r.selfStatus)==='none')
    ||!boundaries.some(r=>r.config.id==='sleep-counter'&&r.config.sleepTime===2&&r.cant.some(l=>l.endsWith('|slp')))
    ||!boundaries.some(r=>r.config.id==='freeze-recovery'&&r.config.thaw&&(r.config.enemy?r.foeStatus:r.selfStatus)==='none')
    ||!boundaries.some(r=>r.config.id==='freeze-recovery'&&!r.config.thaw&&r.cant.some(l=>l.endsWith('|frz')))
    ||!boundaries.filter(r=>r.config.id==='incoming-fire-thaws').every(r=>(r.config.enemy?r.foeStatus:r.selfStatus)==='none'))
    throw Error('Native sleep/freeze boundary coverage failed');
  return {speeds,exchanges,boundaries};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const data=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(data));
  console.log('Status/speed native oracle PASS: '+data.speeds.length+' speed configurations, '+data.exchanges.length+' queued exchanges, '+data.boundaries.length+' sleep/freeze boundaries');
}
