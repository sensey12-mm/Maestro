'use strict';

// Synthetic public/own conditions. Original native residual and faint dispatch
// execute to completion; hidden benches are oracle labels, never evaluator inputs.
exports.run=function(Battle){
  const rows=[];
  function observe(seed,enemy){
    const c={id:'residual',move:'bulkup',replyMove:'bulkup',ability:'runaway',targetAbility:'runaway',item:'none',targetItem:'none',
      attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
      targetMax:1000,targetHp:500,userHp:500,userSpeed:300,targetSpeed:100,room:false,weather:'none',ownBench:0,foeBench:0,
      ...seed,enemy};
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:100});
    const team=(user,n)=>[set(user),...Array.from({length:n},()=>({species:'Mew',ability:'Run Away',moves:['Bulk Up'],movesInfo:[{pp:40,maxPp:40}],level:100}))];
    b.setPlayer('p1',{team:team(!enemy,c.ownBench)});b.setPlayer('p2',{team:team(enemy,c.foeBench)});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=enemy?foe:self,target=enemy?self:foe;
    for(const actor of [self,foe]){actor.maxhp=actor.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;}
    user.hp=c.userHp;target.hp=c.targetHp;target.maxhp=target.baseMaxhp=c.targetMax;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    for(const actor of [user,target])if(actor.status==='tox')actor.statusState={id:'tox',target:actor,stage:c.toxicStage||0};
    if(c.room){b.field.addPseudoWeather('magicroom',user);if(c.expiringRoom)b.field.pseudoWeather.magicroom.duration=1;}
    if(c.protect)target.addVolatile('protect');
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    let checking=false;const accuracy=b.actions.hitStepAccuracy,nativeChance=b.randomChance;
    b.actions.hitStepAccuracy=function(...args){checking=true;try{return accuracy.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){
      if(checking){const next=this.prng.next;this.prng.next=()=>c.miss?99:0;try{return nativeChance.call(this,n,d);}finally{this.prng.next=next;}}
      return n===1&&d===24?!!c.critical:n===1&&d===4?!!c.paralyzed:true;
    };
    b.random=n=>n===100?(c.miss?99:0):5;
    const beforeSelf=self.hp/self.maxhp,beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>({selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,selfStages:{...self.boosts},foeStages:{...foe.boosts},
      selfStatus:self.status||'none',foeStatus:foe.status||'none',utility:beforeFoe-foe.hp/foe.maxhp-(beforeSelf-self.hp/self.maxhp)});
    let observed=null,pre=null,reachedResidual=false;
    const residual=b.residualEvent;
    b.residualEvent=function(...args){reachedResidual=true;pre=snapshot();const result=residual.apply(this,args);observed=snapshot();return result;};
    const faint=b.faintMessages;
    b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!observed)observed=snapshot();return result;};
    b.makeChoices('move 1','move 1');
    if(!observed)throw Error('Native end-of-turn boundary missing: '+c.id);
    const events=b.log.filter(l=>/^\|-(heal|damage)\|/.test(l));
    return {config:c,...observed,pre,reachedResidual,ended:b.ended,request:b.requestState,events};
  }
  const seeds=[];
  for(const status of ['brn','psn'])for(const item of ['none','leftovers'])for(const ability of ['runaway','magicguard'])
    for(const hp of [1,61,63,500,999,1000])seeds.push({id:'status-recovery',status,item,ability,userHp:hp});
  for(const targetMax of [7,15,17,31,999,1001])for(const targetStatus of ['brn','psn'])
    for(const targetHp of [1,targetMax])seeds.push({id:'odd-max',targetMax,targetHp,targetStatus,targetItem:'leftovers'});
  for(const targetMax of [142,155,207])for(const offset of [-1,0,1])for(const ownBench of [0,1])
    seeds.push({id:'integer-faint-boundary',targetMax,targetHp:Math.floor(targetMax/8)-Math.floor(targetMax/16)+offset,
      targetStatus:'psn',targetItem:'leftovers',ownBench});
  seeds.push(
    {id:'both-status',status:'psn',targetStatus:'brn',item:'leftovers',targetItem:'leftovers'},
    {id:'reverse-status',status:'brn',targetStatus:'psn',item:'leftovers',targetItem:'leftovers'},
    {id:'magic-room',status:'psn',item:'leftovers',room:true},
    {id:'expiring-room',status:'brn',item:'leftovers',room:true,expiringRoom:true},
    {id:'guard-recovery',status:'psn',item:'leftovers',ability:'magicguard',userHp:999},
    {id:'burn-physical',status:'brn',move:'tackle',item:'lifeorb',targetHp:1000},
    {id:'drain-cap',status:'psn',move:'drainingkiss',item:'lifeorb',userHp:999,targetHp:1000},
    {id:'drain-shield',status:'brn',move:'drainingkiss',item:'lifeorb',targetAbility:'shadowshield',targetHp:1000},
    {id:'miss',status:'brn',move:'fireblast',item:'lifeorb',miss:true,targetHp:1000},
    {id:'burn-secondary',move:'fireblast',targetItem:'leftovers',targetHp:1000},
    {id:'blocked',move:'closecombat',status:'psn',item:'leftovers',protect:true},
    {id:'immune',move:'closecombat',status:'brn',item:'leftovers',targetTypes:['ghost']},
    {id:'paralysis',status:'par',targetStatus:'psn',targetItem:'leftovers',paralyzed:true},
    {id:'toxic-unknown',status:'tox',item:'leftovers',toxicStage:1},
    {id:'toxic-guard-unknown',status:'tox',ability:'magicguard',item:'leftovers',toxicStage:14},
    {id:'residual-terminal',status:'psn',userHp:1,targetStatus:'brn',foeBench:0},
    {id:'residual-hidden-bench',status:'psn',userHp:1,targetStatus:'brn',foeBench:1},
    {id:'own-reserve',status:'psn',userHp:1,targetStatus:'brn',ownBench:1},
    {id:'both-faint',status:'psn',userHp:1,targetStatus:'brn',targetHp:1,ownBench:1,foeBench:1},
    {id:'action-terminal',move:'tackle',targetHp:1,targetStatus:'brn',item:'leftovers',userHp:500},
    {id:'recoil-terminal',move:'tackle',item:'lifeorb',userHp:1,targetStatus:'brn',targetItem:'leftovers'},
    {id:'reply-terminal',replyMove:'tackle',userHp:1,targetHp:1000,targetItem:'leftovers'},
    {id:'reply-own-reserve',replyMove:'tackle',userHp:1,targetHp:1000,targetItem:'leftovers',ownBench:1},
    {id:'recoil-own-reserve',move:'tackle',item:'lifeorb',userHp:1,targetStatus:'brn',targetItem:'leftovers',ownBench:1}
  );
  for(const seed of seeds)for(const enemy of [false,true])rows.push(observe(seed,enemy));
  if(!rows.some(r=>r.ended)||!rows.some(r=>r.request==='switch')||!rows.some(r=>r.reachedResidual&&!r.ended))throw Error('Residual boundary inventory incomplete');
  for(const row of rows.filter(r=>r.config.id==='burn-secondary'))if((row.config.enemy?row.selfStatus:row.foeStatus)!=='brn')throw Error('Native successful-hit burn missing');
  return rows;
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));
  console.log('Residual/recovery native oracle PASS: '+rows.length+' queued outcomes; recovery before status, rounding, suppression, fainting and both roles');
}
