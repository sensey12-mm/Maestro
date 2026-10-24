'use strict';

// Synthetic actor conditions. Native status, damage, ability and faint dispatch
// remain intact. Hidden benches label oracle outcomes, never evaluator inputs.
exports.run=function(Battle){
  const config=(seed,enemy)=>({id:'roster-ability',move:'psyshock',replyMove:'bulkup',ability:'insomnia',targetAbility:'runaway',item:'none',targetItem:'none',
    attack:219,defense:221,attackStage:0,defenseStage:0,level:100,types:['normal'],targetTypes:['normal'],status:'none',targetStatus:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:301,targetSpeed:201,speedStage:0,targetSpeedStage:0,accuracyStage:0,targetEvasionStage:0,
    room:false,weather:'none',ownBench:0,foeBench:0,draw:0,...seed,enemy});
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
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
    user.status=c.status==='none'?'':c.status;target.status=c.targetStatus==='none'?'':c.targetStatus;
    for(const a of [user,target])if(a.status==='slp')a.statusState={id:'slp',target:a,startTime:3,time:3};
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.weather!=='none')b.field.setWeather(c.weather,user);
    if(c.protect)target.addVolatile('protect');
    return {b,self,foe,user,target};
  }
  const immunity=[],exchanges=[],roster=[];
  for(const [species,ability]of [['Genesect','download'],['Calyrex-Shadow','asonespectrier'],['Xerneas','fairyaura'],['Arceus','multitype'],['Mewtwo-Mega-Y','insomnia'],['Marshadow','technician']]){
    const {b}=battle(config({},false));const native=b.dex.species.get(species);if(b.toID(native.abilities['0'])!==ability)throw Error('Configured roster ability drift: '+species);roster.push({species,ability});
  }
  for(const enemy of [false,true])for(const ability of ['insomnia','runaway']){
    for(const status of ['slp','par','brn','psn','tox','frz']){
      const {b,user,target}=battle(config({ability},enemy));
      const accepted=user.setStatus(status,target,b.dex.moves.get(status==='slp'?'hypnosis':'toxic'));
      if((ability==='insomnia'&&status==='slp')?accepted||user.status:!accepted||user.status!==status)throw Error('Native Insomnia status immunity differs');
      immunity.push({enemy,ability,status,accepted:!!accepted,after:user.status||'none'});
    }
    const y=battle(config({ability},enemy));const yawn=y.user.addVolatile('yawn',y.target,y.b.dex.moves.get('yawn'));
    if(!!yawn!==(ability!=='insomnia'))throw Error('Native Insomnia Yawn boundary differs');
    immunity.push({enemy,ability,status:'yawn',accepted:!!yawn});
    const u=battle(config({ability,status:'slp'},enemy));const before=u.user.status;u.b.runEvent('Update',u.user);
    if(before!=='slp'||u.user.status!==(ability==='insomnia'?'':'slp'))throw Error('Native Insomnia Update cure differs');
    immunity.push({enemy,ability,status:'update',before,after:u.user.status||'none'});
  }
  function exchange(c){
    const {b,self,foe,user}=battle(c);let pre=null,post=null,checking=false;
    const hit=b.actions.hitStepAccuracy,chance=b.randomChance;b.actions.hitStepAccuracy=function(...args){checking=true;try{return hit.apply(this,args);}finally{checking=false;}};
    b.randomChance=function(n,d){if(checking){const next=this.prng.next;this.prng.next=()=>c.draw;try{return chance.call(this,n,d);}finally{this.prng.next=next;}}
      if(n===1&&d===24)return false;return n>=d;};b.random=()=>5;
    const beforeSelf=b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),beforeFoe=foe.hp/foe.maxhp;
    const snapshot=()=>({selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,selfStages:{...self.boosts},foeStages:{...foe.boosts},selfStatus:self.status||'none',foeStatus:foe.status||'none',
      utility:beforeFoe-foe.hp/foe.maxhp-(beforeSelf-b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0))});
    const residual=b.residualEvent;b.residualEvent=function(...args){pre=snapshot();const result=residual.apply(this,args);post=snapshot();return result;};
    const faint=b.faintMessages;b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!pre)pre=post=snapshot();return result;};
    b.makeChoices('move 1','move 1');if(!pre)throw Error('Missing native roster ability boundary: '+JSON.stringify(c));
    const userStages=c.enemy?pre.foeStages:pre.selfStages;
    if(c.ability==='asonespectrier'&&c.targetHp===1&&!c.protect&&c.move==='psyshock'){
      const survives=user.hp>0,continues=!b.ended;
      if(userStages.spa!==(survives?Math.min(6,c.attackStage+(continues?1:0)):0))throw Error('Native post-faint boost ordering differs: '+JSON.stringify(c));
    }
    return {config:c,pre,post,ended:b.ended,request:b.requestState,events:b.log.filter(l=>/^\|(faint|win|-boost|-damage)\|/.test(l))};
  }
  const seeds=[];
  for(const ability of ['insomnia','asonespectrier'])for(const status of ['none','par','brn','psn','slp','frz'])for(const item of ['none','lifeorb','leftovers'])
    seeds.push({ability,status,item});
  for(const ownBench of [0,1])for(const foeBench of [0,1])for(const item of ['none','lifeorb'])for(const userHp of [100,101,500])for(const attackStage of [-6,0,5,6])
    seeds.push({id:'move-ko',ability:'asonespectrier',targetHp:1,item,userHp,attackStage,ownBench,foeBench});
  for(const ability of ['insomnia','asonespectrier'])for(const extra of [
    {id:'protect',protect:true,move:'closecombat',item:'lifeorb'},
    {id:'immune',move:'closecombat',targetTypes:['ghost'],item:'lifeorb'},
    {id:'miss',move:'fireblast',draw:99,item:'lifeorb'},
    {id:'drain',move:'drainingkiss',item:'lifeorb',userHp:999},
    {id:'shield',targetAbility:'shadowshield',item:'lifeorb'},
    {id:'room',room:true,item:'lifeorb'},
    {id:'residual-ko',move:'bulkup',targetStatus:'psn',targetHp:1,ownBench:1,foeBench:1},
    {id:'recoil-ko',item:'lifeorb',userHp:100,ownBench:1,foeBench:1},
    {id:'slow-user',replyMove:'psyshock',userSpeed:201,targetSpeed:301},
    {id:'source-fainted',replyMove:'psyshock',userSpeed:201,targetSpeed:301,userHp:1,ownBench:1,foeBench:1}])seeds.push({ability,...extra});
  for(const seed of seeds)for(const enemy of [false,true])exchanges.push(exchange(config(seed,enemy)));
  if(!exchanges.some(r=>r.ended)||!exchanges.some(r=>r.request==='switch'))throw Error('Missing native terminal/replacement cases');
  return {roster,immunity,exchanges};
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const result=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(result));console.log('Configured roster ability native oracle PASS: '+JSON.stringify(Object.fromEntries(Object.entries(result).map(([k,v])=>[k,v.length]))));}
