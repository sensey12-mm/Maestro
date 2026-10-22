'use strict';
// Synthetic owner-side choices in either native position. No actual opponent
// reserve, stat, ability or party closure is supplied to production as fact.
exports.run=function(Battle,coldOnly=false){
 const seeds=[];
 if(!coldOnly){
 for(const weather of ['sandstorm','sunnyday','raindance'])for(const reply of ['fireblast','thunder','tackle','rocktomb','bulkup'])seeds.push({id:'weather-reply',weather,reply});
 for(const [targetAbility,weather]of [['sandrush','sandstorm'],['sandforce','sandstorm'],['sandveil','sandstorm'],['chlorophyll','sunnyday'],['swiftswim','raindance'],['raindish','raindance'],['solarpower','sunnyday'],['shadowshield','sandstorm']])
  for(const targetItem of ['utilityumbrella','leftovers','lifeorb'])for(const magicRoom of [false,true])seeds.push({id:'composition',targetAbility,weather,targetItem,magicRoom,reply:'thunder'});
 for(const hazards of [{stealthrock:1,spikes:3,stickyweb:1,toxicspikes:1},{stealthrock:1}])for(const reserveItem of ['', 'heavydutyboots'])for(const magicRoom of [false,true])seeds.push({id:'hazards',hazards,reserveItem,magicRoom,weather:'sandstorm'});
 for(const reserveHp of [124,125,126])seeds.push({id:'entry-survival',reserveHp,hazards:{stealthrock:1},reply:'bulkup'});
 for(const ownBench of [0,1,2])for(const foeBench of [0,1])for(const targetHp of [1,1000])seeds.push({id:'party',ownBench,foeBench,targetHp});
 for(const hp of [100,101])seeds.push({id:'recoil',hp,item:'lifeorb',reply:'bulkup'});
 for(const targetHp of [1,60])seeds.push({id:'target-faint',targetHp,reply:'bulkup'});
 for(const targetAbility of ['airlock','cloudnine'])seeds.push({id:'other-suppressor',targetAbility,reply:'fireblast'});
 for(const weatherMode of ['upkeep','ended'])seeds.push({id:'clock',weatherMode});
 for(const reserveStatus of ['brn','psn','tox','slp'])seeds.push({id:'status',reserveStatus});
 seeds.push({id:'blocked',protect:true},{id:'slow',userSpeed:100,targetSpeed:300,reply:'fireblast'},
  {id:'queue-speed',targetAbility:'chlorophyll',weather:'sunnyday',userSpeed:300,targetSpeed:200,reply:'tackle'},
  {id:'room',trickRoom:true,targetAbility:'swiftswim',weather:'raindance',reply:'tackle'},
  {id:'unsupported',targetAbility:'neutralizinggas'},
  {id:'umbrella-entry',reserveItem:'utilityumbrella',weather:'sunnyday',reply:'fireblast'},
  {id:'umbrella-entry',reserveItem:'utilityumbrella',weather:'raindance',reply:'thunder'},
  {id:'rock-defense',reserveTypes:['rock'],weather:'sandstorm',reply:'swift'},
  {id:'speed-chain',targetAbility:'chlorophyll',weather:'sunnyday',targetItem:'choicescarf',targetWind:true,targetStatus:'par',targetSpeed:101},
  {id:'speed-chain',targetAbility:'swiftswim',weather:'raindance',targetItem:'choicescarf',targetWind:true,targetSpeed:101},
  {id:'speed-chain',targetAbility:'sandrush',weather:'sandstorm',targetItem:'choicescarf',targetWind:true,targetSpeed:101});
 for(const weather of ['sunnyday','raindance','sandstorm'])seeds.push({id:'miss',weather,reply:'thunder'});
 }else{
  for(const weather of ['snow','hail']){
   seeds.push({id:'weather-reply',weather,reply:'psyshock',reserveTypes:['ice']});
   seeds.push({id:'cold-speed',weather,targetAbility:'slushrush',targetItem:'choicescarf',targetWind:true,targetStatus:'par',targetSpeed:101,reply:'tackle'});
   seeds.push({id:'cold-evasion',weather,targetAbility:'snowcloak',reply:'thunder'});
   seeds.push({id:'cold-heal',weather,targetAbility:'icebody',targetItem:'leftovers',targetHp:500,reply:'bulkup'});
   for(const reserveItem of ['', 'heavydutyboots'])for(const magicRoom of [false,true])
    seeds.push({id:'hazards',weather,reserveItem,magicRoom,hazards:{stealthrock:1,spikes:3,stickyweb:1,toxicspikes:1},reserveTypes:['ice'],reply:'tackle'});
   for(const weatherMode of ['upkeep','ended'])seeds.push({id:'clock',weather,weatherMode,reply:'bulkup'});
  }
 }
 const rows=[];
 for(const suppressor of ['airlock','cloudnine'])for(const direction of ['enter','depart','retain'])for(const mode of ['switch','pivot'])for(const seed of seeds)for(const enemy of [false,true]){
  if(direction==='retain'&&!['weather-reply','hazards','clock','recoil'].includes(seed.id))continue;
  const c={id:'entry',reply:'tackle',hp:500,targetHp:1000,userSpeed:300,targetSpeed:100,item:'',ownBench:1,foeBench:1,
   reserveMax:1000,reserveHp:1000,reserveTypes:['normal'],reserveItem:'',reserveStatus:'none',hazards:{},magicRoom:false,protect:false,
   weather:'sandstorm',weatherMode:'fresh',targetAbility:'runaway',targetItem:'none',targetTypes:['normal'],targetAttack:220,targetSpecialAttack:220,
   ...seed,mode,enemy,direction,suppressor,userAbility:direction==='enter'?'runaway':suppressor,reserveAbility:direction==='depart'?'runaway':suppressor};
  if(mode==='switch'&&c.ownBench===0)continue;
  const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
  const set=(move,item,ability)=>({species:'Mew',ability,item:item==='none'?'':item,moves:[move],movesInfo:[{pp:40,maxPp:40}],level:100});
  const team=[set('uturn',c.item,c.userAbility),...Array.from({length:c.ownBench},()=>set('tackle',c.reserveItem,c.reserveAbility))];
  const other=[set(c.reply,c.targetItem,c.targetAbility),...Array.from({length:c.foeBench},()=>set('bulkup','','runaway'))];
  b.setPlayer('p1',{team:enemy?other:team});b.setPlayer('p2',{team:enemy?team:other});
  if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
  const user=(enemy?b.p2:b.p1).active[0],target=(enemy?b.p1:b.p2).active[0],bench=user.side.pokemon.slice(1);
  for(const side of b.sides)for(const a of side.pokemon){a.hp=a.maxhp=a.baseMaxhp=1000;a.types=a.baseTypes=['Normal'];for(const stat of ['atk','def','spa','spd','spe'])a.storedStats[stat]=220;}
  user.hp=c.hp;target.hp=c.targetHp;user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
  target.storedStats.atk=c.targetAttack;target.storedStats.spa=c.targetSpecialAttack;
  target.status=c.targetStatus==='par'?'par':'';
  target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
  for(const a of bench){a.hp=c.reserveHp;a.maxhp=a.baseMaxhp=c.reserveMax;a.types=a.baseTypes=c.reserveTypes.map(t=>t[0].toUpperCase()+t.slice(1));a.status=c.reserveStatus==='none'?'':c.reserveStatus;}
  if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
  if(c.trickRoom)b.field.addPseudoWeather('trickroom',user);
  if(c.targetWind)target.side.addSideCondition('tailwind',target);
  b.field.setWeather(c.weather,user);
  if(c.weatherMode==='upkeep')b.field.weatherState.duration=1;
  if(c.weatherMode==='ended')b.field.clearWeather();
  if(c.protect)target.addVolatile('protect');
  for(const [id,layers]of Object.entries(c.hazards))for(let i=0;i<layers;i++)user.side.addSideCondition(id,target);
  const initialUser=user.side.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),initialTarget=target.hp/target.maxhp;
  const snap=()=>{const a=user.side.active[0],f=target.side.active[0];return {active:a===user?'self':'reserve0',selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',
   weather:b.field.weather||'none',effective:b.field.effectiveWeather()||'none',duration:b.field.weatherState.duration||0,
   hazards:Object.fromEntries(Object.entries(user.side.sideConditions).map(([id,s])=>[id,s.layers||1])),
   utility:initialTarget-f.hp/f.maxhp-(initialUser-user.side.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
  let pre=null,post=null,entry=null,probe=null;const trace=[];
  const runSwitch=b.actions.runSwitch;b.actions.runSwitch=function(a,...args){const r=runSwitch.call(this,a,...args);
   if(bench.includes(a)){entry=snap();trace.push('entry:'+a.hp+':'+(b.field.effectiveWeather()||'none'));
    if(a.hp>0&&target.hp>0){
     const damage=[],random=b.random,rolls=c.id==='weather-reply'?Array.from({length:16},(_,i)=>85+i):[85,100];
     for(const crit of [false,true])for(const roll of rolls){b.random=()=>100-roll;const m=b.dex.getActiveMove(c.reply);m.willCrit=crit;if(m.basePower)damage.push({crit,damage:b.actions.getDamage(target,a,m,true)});}b.random=random;
     let threshold=100;const chance=b.randomChance,m=b.dex.getActiveMove(c.reply);
     if(m.basePower){b.singleEvent('ModifyMove',m,null,target,a,m,m);b.randomChance=(n,d)=>{threshold=n;return true;};b.actions.hitStepAccuracy([a],target,m);b.randomChance=chance;}
     probe={...snap(),speed:target.getStat('spe'),damage,threshold};
    }
   }return r;};
  const run=b.actions.runMove;b.actions.runMove=function(move,a,...args){trace.push('move:'+(a===user?'user':'target')+':'+(this.battle.field.effectiveWeather()||'none'));return run.call(this,move,a,...args);};
  const residual=b.residualEvent;b.residualEvent=function(...args){pre=snap();const r=residual.apply(this,args);post=snap();return r;};
  const faint=b.faintMessages;b.faintMessages=function(...args){const r=faint.apply(this,args);if(this.ended&&!pre)pre=post=snap();return r;};
  let checking=false;const accuracy=b.actions.hitStepAccuracy;
  b.actions.hitStepAccuracy=function(...args){checking=true;try{return accuracy.apply(this,args);}finally{checking=false;}};
  b.randomChance=(n,d)=>checking?n>=d||c.id!=='miss'&&n>0:n>=d;b.random=()=>5;
  if(mode==='switch')b.makeChoices(enemy?'move 1':'switch 2',enemy?'switch 2':'move 1');else b.makeChoices('move 1','move 1');
  if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(enemy?'':'switch 2',enemy?'switch 2':'');
  if(!pre)throw Error('Native weather entry boundary missing '+JSON.stringify(c));
  if(entry&&entry.selfHp>0&&c.targetAbility!=='neutralizinggas'){
   const expected=c.weatherMode==='ended'||direction!=='depart'||['airlock','cloudnine'].includes(c.targetAbility)?'none':c.weather;
   if(entry.effective!==expected)throw Error('Native active weather did not follow entry/departure');
  }
  if(c.weatherMode==='upkeep'&&post.weather!=='none')throw Error('Suppressed weather clock stopped ticking');
  rows.push({config:c,entry,probe,pre,post,trace,ended:b.ended});
 }
 return rows;
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));console.log('Weather suppressor entry oracle PASS: '+rows.length+' queued entries/departures');}
