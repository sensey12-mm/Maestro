'use strict';

// Synthetic declared state; native move/callback definitions are unmodified.
exports.run=function(Battle){
  const config=(seed,enemy)=>({move:'rocktomb',replyMove:'bulkup',ability:'runaway',targetAbility:'runaway',item:'lifeorb',targetItem:'none',
    attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:300,targetSpeed:100,speedStage:0,targetSpeedStage:0,
    accuracyStage:0,evasionStage:0,targetAccuracyStage:0,targetEvasionStage:0,room:false,weather:'none',wind:false,targetWind:false,
    secondary:true,draw:99,paralyzed:false,...seed,enemy});
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
    user.boosts.accuracy=c.accuracyStage;user.boosts.evasion=c.evasionStage;target.boosts.accuracy=c.targetAccuracyStage;target.boosts.evasion=c.targetEvasionStage;
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.weatherMode==='ended')b.field.clearWeather();
    if(c.weatherMode==='restart'){b.field.clearWeather();b.field.setWeather('sandstorm',user);}
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.trickRoom)b.field.addPseudoWeather('trickroom',user);
    for(const [actor,wind] of [[user,c.wind],[target,c.targetWind]])if(wind){actor.side.addSideCondition('tailwind',actor);if(c.windEnded)actor.side.removeSideCondition('tailwind');}
    if(c.protect)target.addVolatile('protect');
    if(c.terrain)b.field.setTerrain(c.terrain,user);
    if(c.cacheZero)user.speed=0;
    return {b,self,foe,user,target};
  }

  function observe(c,thresholdOnly=false){
    const {b,self,foe,user,target}=battle(c),draws=[],steps=[];let checking=false,observed=null,userAction=null;
    for(const name of ['hitStepTryHitEvent','hitStepTypeImmunity','hitStepTryImmunity','hitStepAccuracy','hitStepStealBoosts','hitStepMoveHitLoop']){
      const original=b.actions[name];
      b.actions[name]=function(targets,actor,move,...rest){
        if(actor===user)steps.push(name);const previous=checking;checking=name==='hitStepAccuracy';
        try{return original.call(this,targets,actor,move,...rest);}finally{checking=previous;}
      };
    }
    const nativeChance=b.randomChance;
    b.randomChance=function(n,d){
      if(checking){
        draws.push({actor:this.activePokemon===user?'user':'target',n,d});
        const next=this.prng.next;this.prng.next=()=>c.draw;
        try{return nativeChance.call(this,n,d);}finally{this.prng.next=next;}
      }
      if(n===1&&d===4)return this.event.target===user&&c.paralyzed;
      if(n===1&&d===5)return false;
      if(n===1&&d===24)return !!c.critical;
      return n>=d;
    };
    b.random=(n)=>n===100?(c.secondary?0:99):5;
    if(thresholdOnly){
      const move=b.dex.getActiveMove(c.move);
      // Native move-specific weather hook is part of the accuracy pipeline.
      b.singleEvent('ModifyMove',move,null,user,target,move,move);
      b.setActiveMove(move,user,target);
      const passed=b.actions.hitStepAccuracy([target],user,move)[0];
      return {config:c,threshold:draws.length?draws[0].n:100,always:move.accuracy===true,draws,passed};
    }
    const beforeOwn=self.hp/self.maxhp,beforeFoe=foe.hp/foe.maxhp;
    const run=b.actions.runMove;
    b.actions.runMove=function(move,actor,...args){
      const before={userHp:user.hp,targetHp:target.hp,userStages:{...user.boosts},targetStages:{...target.boosts},targetStatus:target.status||'none'};
      const result=run.call(this,move,actor,...args);
      if(actor===user)userAction={before,after:{userHp:user.hp,targetHp:target.hp,userStages:{...user.boosts},targetStages:{...target.boosts},targetStatus:target.status||'none'}};
      return result;
    };
    const residual=b.residualEvent;
    b.residualEvent=function(...args){if(!observed)observed={config:c,selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,
      selfStages:{...self.boosts},foeStages:{...foe.boosts},selfStatus:self.status||'none',foeStatus:foe.status||'none',
      utility:beforeFoe-foe.hp/foe.maxhp-(beforeOwn-self.hp/self.maxhp)};return residual.apply(this,args);};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native accuracy pre-residual boundary: '+c.id);
    const misses=b.log.filter(l=>l.startsWith('|-miss|')),cant=b.log.filter(l=>l.startsWith('|cant|'));
    const ownMiss=misses.some(l=>l.includes(c.enemy?'p2a:':'p1a:'));
    if(ownMiss&&JSON.stringify(userAction.before)!==JSON.stringify(userAction.after))throw Error('Miss applied damage or follow-ups: '+c.id);
    if((c.protect||c.id==='immune'||c.id==='terrain-block'||c.paralyzed)&&draws.some(d=>d.actor==='user'))throw Error('Early block/paralysis reached accuracy: '+c.id);
    if(c.paralyzed&&!cant.some(l=>l.endsWith('|par')))throw Error('Native paralysis fixture did not skip action');
    return {...observed,draws,steps,misses,cant,userAction,ownMiss};
  }
  const thresholds=[];
  for(const move of ['rocktomb','thunder','fireblast','tackle','swift'])for(const [accuracyStage,targetEvasionStage] of [[0,0],[-1,0],[0,1],[6,-6],[-6,6],[2,1]])
    for(const weather of move==='thunder'?['none','sunnyday','raindance']:['none'])for(const enemy of [false,true])
      thresholds.push(observe(config({id:'threshold',move,accuracyStage,targetEvasionStage,weather},enemy),true));
  const veilStages=[...Array.from({length:13},(_,i)=>[i-6,0]),[6,-6],[-6,6],[2,1],[0,1]];
  for(const move of ['rocktomb','thunder','fireblast','tackle','swift'])for(const [accuracyStage,targetEvasionStage] of veilStages)
    for(const incoming of [false,true])for(const enemy of [false,true])
      thresholds.push(observe(config({id:'sand-veil-threshold',move,accuracyStage,targetEvasionStage,weather:'sandstorm',
        ability:incoming?'runaway':'sandveil',targetAbility:incoming?'sandveil':'runaway'},enemy),true));
  for(const move of ['rocktomb','thunder','fireblast','swift'])for(const room of [false,true])for(const enemy of [false,true])
    thresholds.push(observe(config({id:'veil-goggles-threshold',move,room,targetAbility:'sandveil',item:'safetygoggles',targetItem:'safetygoggles',weather:'sandstorm'},enemy),true));
  for(const weather of ['none','sunnyday','raindance'])for(const enemy of [false,true])
    thresholds.push(observe(config({id:'veil-thunder-weather',move:'thunder',weather,targetAbility:'sandveil',accuracyStage:-6,targetEvasionStage:6},enemy),true));
  for(const weatherMode of ['ended','restart'])for(const move of ['rocktomb','thunder','fireblast','tackle','swift'])for(const enemy of [false,true])
    thresholds.push(observe(config({id:'veil-weather-change',move,weatherMode,weather:'sandstorm',targetAbility:'sandveil'},enemy),true));
  const seeds=[
    {id:'rock',move:'rocktomb'}, {id:'thunder',move:'thunder'}, {id:'fire',move:'fireblast'},
    {id:'focus',move:'focusblast'}, {id:'sun-thunder',move:'thunder',weather:'sunnyday'},
    {id:'rain-thunder',move:'thunder',weather:'raindance',accuracyStage:-6,targetEvasionStage:6},
    {id:'lowered-accuracy',move:'rocktomb',accuracyStage:-1}, {id:'raised-evasion',move:'fireblast',targetEvasionStage:1},
    {id:'clamped-stages',move:'thunder',accuracyStage:-6,targetEvasionStage:6},
    {id:'boosted-guarantee',move:'rocktomb',accuracyStage:1}, {id:'swift-guarantee',move:'swift',accuracyStage:-6,targetEvasionStage:6},
    {id:'protect',move:'thunder',protect:true}, {id:'immune',move:'thunder',targetTypes:['ground']},
    {id:'terrain-block',move:'quickattack',terrain:'psychicterrain',accuracyStage:-1},
    {id:'paralysis-miss',move:'rocktomb',status:'par',item:'choicescarf',wind:true},
    {id:'paralysis-skip',move:'thunder',status:'par',paralyzed:true},
    {id:'priority-reply',move:'fireblast',replyMove:'quickattack'},
    {id:'close-combat',move:'closecombat',targetEvasionStage:1},
    {id:'drain',move:'drainingkiss',targetEvasionStage:1,userHp:990},
    {id:'pivot',move:'uturn',targetEvasionStage:1},
    {id:'theft',move:'spectralthief',targetEvasionStage:1,targetTypes:['psychic'],defenseStage:2},
    {id:'magic-guard',move:'rocktomb',ability:'magicguard'},
    {id:'magic-room',move:'fireblast',room:true}, {id:'shield',move:'thunder',targetAbility:'shadowshield'},
    {id:'critical',move:'rocktomb',critical:true}
  ];
  const exchanges=[];
  for(const seed of seeds)for(const draw of [0,99])for(const enemy of [false,true])exchanges.push(observe(config({...seed,draw},enemy)));
  const veilSeeds=[];
  for(const move of ['rocktomb','thunder','fireblast','tackle','closecombat','drainingkiss','uturn','spectralthief'])for(const ability of ['runaway','sandforce'])for(const item of ['lifeorb','safetygoggles'])
    veilSeeds.push({id:'veil-follow-ups',move,ability,item,targetAbility:'sandveil',weather:'sandstorm',
      targetTypes:move==='spectralthief'?['psychic']:['normal'],defenseStage:move==='spectralthief'?2:0,userHp:move==='drainingkiss'?990:500});
  for(const item of ['lifeorb','safetygoggles'])for(const room of [false,true])
    veilSeeds.push({id:'outgoing-veil-shield',ability:'sandveil',targetAbility:'shadowshield',targetItem:'safetygoggles',item,room,weather:'sandstorm',critical:true});
  veilSeeds.push({id:'protect',targetAbility:'sandveil',weather:'sandstorm',protect:true},
    {id:'immune',move:'thunder',targetTypes:['ground'],targetAbility:'sandveil',weather:'sandstorm'},
    {id:'terrain-block',move:'quickattack',terrain:'psychicterrain',targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-paralysis',status:'par',item:'choicescarf',wind:true,ability:'sandrush',targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-paralysis-skip',status:'par',paralyzed:true,targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-priority',replyMove:'quickattack',targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-critical',critical:true,targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-guaranteed',accuracyStage:1,targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-swift',move:'swift',accuracyStage:-6,targetEvasionStage:6,targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-suppressed-goggles',item:'safetygoggles',targetItem:'safetygoggles',room:true,targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-ended',weatherMode:'ended',targetAbility:'sandveil',weather:'sandstorm'},
    {id:'veil-restarted',weatherMode:'restart',targetAbility:'sandveil',weather:'sandstorm'});
  for(const seed of veilSeeds)for(const enemy of [false,true]){
    const threshold=observe(config(seed,enemy),true).threshold;
    for(const draw of new Set([0,Math.max(0,Math.min(99,threshold-1)),Math.max(0,Math.min(99,threshold)),99]))
      exchanges.push(observe(config({...seed,draw},enemy)));
  }
  if(!exchanges.some(r=>r.ownMiss)||!exchanges.some(r=>!r.ownMiss&&r.config.draw===0))throw Error('Native hit/miss inventory missing');
  return {thresholds,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));
  console.log('Accuracy/miss native oracle PASS: '+result.thresholds.length+' thresholds, '+result.exchanges.length+' queued exchanges; native ordering, miss follow-ups, weather and both actor roles');
}
