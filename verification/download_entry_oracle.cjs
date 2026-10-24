'use strict';
// Declared actors/benches and hidden-party labels. Original native hazards,
// Download, stat reads and queue remain intact; only random draws are controlled.
exports.run=function(Battle){
 const seeds=[];
 for(const [targetDefense,targetSpecialDefense]of [[150,220],[220,150],[220,220],[101,102],[1,1],[1,2],[2,1]])
  for(const [targetDefStage,targetSpdStage]of [[0,0],[1,0],[0,1],[-1,-1],[-6,-6],[-6,0],[0,-6],[6,-6],[-6,6]])
   seeds.push({id:'stat-selection',targetDefense,targetSpecialDefense,targetDefStage,targetSpdStage});
 for(const reply of ['bulkup','nastyplot','recover','tackle','closecombat','ironhead','fireblast'])for(const fast of [false,true])
  seeds.push({id:'queued-stage',reply,userSpeed:fast?100:300,targetSpeed:fast?300:100,targetDefense:150,targetSpecialDefense:220});
 for(const weather of ['none','sandstorm','sunnyday','raindance'])for(const targetAbility of ['runaway','shadowshield','airlock','cloudnine','insomnia'])
  seeds.push({id:'ignored-defense-hook',weather,targetAbility,targetTypes:['rock'],targetDefense:200,targetSpecialDefense:150,targetItem:'leftovers'});
 for(const targetAbility of ['furcoat','marvelscale','neutralizinggas'])seeds.push({id:'unsupported-ability',targetAbility});
 for(const targetItem of ['eviolite','assaultvest'])seeds.push({id:'unsupported-item',targetItem});
 seeds.push({id:'wonder-room',wonderRoom:true,targetDefStage:1});
 for(const hazards of [{},{stealthrock:1},{spikes:3},{stickyweb:1},{toxicspikes:1},{toxicspikes:2},{stealthrock:1,spikes:3,stickyweb:1,toxicspikes:1}])
  for(const reserveItem of ['','heavydutyboots'])for(const magicRoom of [false,true])seeds.push({id:'hazards',hazards,reserveItem,magicRoom});
 function permute(a){if(!a.length)return [[]];return a.flatMap((x,i)=>permute(a.filter((_,j)=>j!==i)).map(p=>[x,...p]));}
 for(const order of permute(['stealthrock','spikes','toxicspikes','stickyweb']))seeds.push({id:'hazard-order',hazards:Object.fromEntries(order.map(id=>[id,1]))});
 for(const reserveHp of [124,125,126])seeds.push({id:'entry-survival',reserveHp,hazards:{stealthrock:1,toxicspikes:1,stickyweb:1}});
 for(const reserveStatus of ['par','brn','psn','tox','slp','frz'])seeds.push({id:'existing-status',reserveStatus,hazards:{toxicspikes:1}});
 for(const ownBench of [0,1,2])for(const foeBench of [0,1])for(const targetHp of [1,1000])seeds.push({id:'party',ownBench,foeBench,targetHp});
 for(const hp of [100,101])seeds.push({id:'recoil-boundary',hp,item:'lifeorb'});
 seeds.push({id:'blocked',protect:true});
 const rows=[];
 for(const mode of ['pivot','switch'])for(const seed of seeds)for(const enemy of [false,true]){
  const c={reply:'bulkup',hp:500,targetHp:1000,userSpeed:300,targetSpeed:100,item:'',ownBench:1,foeBench:1,
   reserveMax:1000,reserveHp:1000,reserveTypes:['normal'],reserveItem:'',reserveAbility:'download',reserveStatus:'none',hazards:{},magicRoom:false,protect:false,weather:'none',
   targetDefense:220,targetSpecialDefense:220,targetDefStage:0,targetSpdStage:0,targetTypes:['normal'],targetAbility:'runaway',targetItem:'none',...seed,mode,enemy};
  if(mode==='switch'&&c.ownBench===0)continue;
  const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
  const set=(move,item='',ability='runaway')=>({species:'Mew',ability,item:item==='none'?'':item,moves:[move],movesInfo:[{pp:40,maxPp:40}],level:100});
  const userTeam=[set('uturn',c.item),...Array.from({length:c.ownBench},()=>set('tackle',c.reserveItem,'download'))];
  const targetTeam=[set(c.reply,c.targetItem,c.targetAbility),...Array.from({length:c.foeBench},()=>set('bulkup'))];
  b.setPlayer('p1',{team:enemy?targetTeam:userTeam});b.setPlayer('p2',{team:enemy?userTeam:targetTeam});
  if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
  const user=(enemy?b.p2:b.p1).active[0],target=(enemy?b.p1:b.p2).active[0],bench=user.side.pokemon.slice(1);
  for(const side of b.sides)for(const a of side.pokemon){a.hp=a.maxhp=a.baseMaxhp=1000;a.types=a.baseTypes=['Normal'];for(const stat of ['atk','def','spa','spd','spe'])a.storedStats[stat]=220;}
  user.hp=c.hp;target.hp=c.targetHp;user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
  target.storedStats.def=c.targetDefense;target.storedStats.spd=c.targetSpecialDefense;target.boosts.def=c.targetDefStage;target.boosts.spd=c.targetSpdStage;
  target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
  for(const a of bench){a.hp=c.reserveHp;a.maxhp=a.baseMaxhp=c.reserveMax;a.types=a.baseTypes=c.reserveTypes.map(t=>t[0].toUpperCase()+t.slice(1));a.status=c.reserveStatus==='none'?'':c.reserveStatus;a.boosts.atk=c.reserveAtkStage||0;a.boosts.spa=c.reserveSpaStage||0;}
  if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
  if(c.wonderRoom)b.field.addPseudoWeather('wonderroom',user);
  if(c.weather!=='none')b.field.setWeather(c.weather,user);
  if(c.protect)target.addVolatile('protect');
  for(const [id,layers]of Object.entries(c.hazards))for(let i=0;i<layers;i++)user.side.addSideCondition(id,target);
  const initialUser=user.side.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),initialTarget=target.hp/target.maxhp;
  const hazards=()=>Object.fromEntries(Object.entries(user.side.sideConditions).map(([id,s])=>[id,s.layers||1]));
  const snap=()=>{const a=user.side.active[0],f=target.side.active[0];return {active:a===user?'self':'reserve0',selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',hazards:hazards(),utility:initialTarget-f.hp/f.maxhp-(initialUser-user.side.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
  let entry=null,activation=null,pre=null,post=null;const event=b.runEvent;
  b.runEvent=function(id,a,...args){const before=a?.hp;const r=event.call(this,id,a,...args);if(id==='EntryHazard'&&bench.includes(a))entry={hp:a.hp,max:a.maxhp,before,status:a.status||'none',speed:a.boosts.spe,hazards:hazards()};return r;};
  const single=b.singleEvent;b.singleEvent=function(id,e,state,a,...args){if(id!=='Start'||e.id!=='download'||!bench.includes(a))return single.call(this,id,e,state,a,...args);
   const foes=a.foes(),def=foes.reduce((n,f)=>n+f.getStat('def',false,true),0),spd=foes.reduce((n,f)=>n+f.getStat('spd',false,true),0);const before={...a.boosts},r=single.call(this,id,e,state,a,...args);
   activation={hp:a.hp,def,spd,sameFoe:foes.length===1&&foes[0]===target,before,after:{...a.boosts},status:a.status||'none',hazards:hazards()};
   if(c.targetAbility!=='neutralizinggas'){const stat=def&&def>=spd?'spa':spd?'atk':null;if(stat&&a.boosts[stat]!==Math.min(6,before[stat]+1))throw Error('Native Download selection changed');}
   return r;};
  const residual=b.residualEvent;b.residualEvent=function(...args){pre=snap();const r=residual.apply(this,args);post=snap();return r;};
  const faint=b.faintMessages;b.faintMessages=function(...args){const r=faint.apply(this,args);if(this.ended&&!pre)pre=post=snap();return r;};
  b.randomChance=(n,d)=>n>=d;b.random=()=>5;
  if(mode==='switch')b.makeChoices(enemy?'move 1':'switch 2',enemy?'switch 2':'move 1');else b.makeChoices('move 1','move 1');
  if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(enemy?'':'switch 2',enemy?'switch 2':'');
  if(!pre)throw Error('Missing native Download boundary '+JSON.stringify(c));
  if(entry&&entry.hp<=0&&activation)throw Error('Download activated after lethal hazards');
  rows.push({config:c,entry,activation,pre,post,ended:b.ended,request:b.requestState});
 }
 return rows;
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));console.log('Download native entry oracle PASS: '+rows.length+' queued entries');}
