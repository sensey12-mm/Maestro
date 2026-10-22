'use strict';
// Declared synthetic actors and benches. Original native hazard callbacks and
// queue execute unchanged; insertion order and hidden teams are oracle labels.
exports.run=function(Battle){
 const seeds=[];
 for(const layers of [1,2])for(const reserveTypes of [['normal'],['poison'],['steel'],['flying'],['poison','flying']])
  for(const reserveAbility of ['runaway','levitate','magicguard'])for(const reserveItem of ['','heavydutyboots'])for(const magicRoom of [false,true])
   seeds.push({id:'composition',reserveTypes,reserveAbility,reserveItem,magicRoom,hazards:{stealthrock:1,spikes:2,stickyweb:1,toxicspikes:layers}});
 for(const reserveStatus of ['brn','par','psn','tox','slp','frz'])for(const reserveTypes of [['normal'],['poison']])
  seeds.push({id:'existing-status',reserveStatus,reserveTypes,hazards:{toxicspikes:2}});
 for(const reserveMax of [7,17,31,999,1001])for(const reserveHp of [1,reserveMax])seeds.push({id:'hp-boundary',reserveMax,reserveHp,hazards:{stealthrock:1,spikes:1,toxicspikes:1,stickyweb:1}});
 function permute(a){if(!a.length)return [[]];return a.flatMap((x,i)=>permute(a.filter((_,j)=>j!==i)).map(p=>[x,...p]));}
 for(const order of permute(['stealthrock','spikes','toxicspikes','stickyweb']))for(const reserveTypes of [['normal'],['poison']])
  seeds.push({id:'hazard-order',reserveTypes,hazards:Object.fromEntries(order.map(id=>[id,1]))});
 for(const weather of ['sandstorm','sunnyday','raindance'])for(const reserveAbility of ['runaway','magicguard','raindish','solarpower'])
  for(const reserveItem of ['leftovers','utilityumbrella'])seeds.push({id:'weather-status',weather,reserveAbility,reserveItem,hazards:{toxicspikes:1,spikes:1}});
 for(const ownBench of [0,1,2])for(const foeBench of [0,1])seeds.push({id:'party',ownBench,foeBench,hazards:{toxicspikes:1}});
 seeds.push({id:'download-entry',reserveAbility:'download',hazards:{toxicspikes:1}},
  {id:'unknown-counter',hazards:{toxicspikes:2}},
  {id:'reply-attack',reply:'tackle',hazards:{toxicspikes:1}},
  {id:'reply-miss',reply:'fireblast',hazards:{toxicspikes:1}},
  {id:'reserve-after-hit',hp:100,item:'lifeorb',hazards:{toxicspikes:1}},
  {id:'survive-one',reserveHp:126,hazards:{stealthrock:1,toxicspikes:1}},
  {id:'dead-entry',reserveHp:125,hazards:{stealthrock:1,toxicspikes:1}});
 const rows=[];
 for(const mode of ['pivot','switch'])for(const seed of seeds)for(const enemy of [false,true]){
  const c={id:'entry',reply:'bulkup',hp:500,targetHp:1000,userSpeed:300,targetSpeed:100,item:'',ownBench:1,foeBench:1,
   reserveMax:1000,reserveHp:1000,reserveTypes:['normal'],reserveItem:'',reserveAbility:'runaway',reserveStatus:'none',hazards:{},magicRoom:false,protect:false,weather:'none',...seed,mode,enemy};
  if(mode==='switch'&&c.ownBench===0)continue;
  const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
  const set=(move,item='',ability='runaway')=>({species:'Mew',ability,item,moves:[move],movesInfo:[{pp:40,maxPp:40}],level:100});
  const team=(user,n)=>[set(user?'uturn':c.reply,user?c.item:''),...Array.from({length:n},()=>set('tackle',c.reserveItem,c.reserveAbility))];
  b.setPlayer('p1',{team:team(!enemy,c.ownBench)});b.setPlayer('p2',{team:team(enemy,Math.max(c.foeBench,enemy?1:0))});
  if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
  const own=b.p1.active[0],foe=b.p2.active[0],user=enemy?foe:own,target=enemy?own:foe,bench=user.side.pokemon.slice(1);
  for(const side of b.sides)for(const a of side.pokemon){a.hp=a.maxhp=a.baseMaxhp=1000;a.types=a.baseTypes=['Normal'];for(const stat of ['atk','def','spa','spd','spe'])a.storedStats[stat]=220;}
  user.hp=c.hp;target.hp=c.targetHp;user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
  for(const a of bench){a.hp=c.reserveHp;a.maxhp=a.baseMaxhp=c.reserveMax;a.types=a.baseTypes=c.reserveTypes.map(t=>t[0].toUpperCase()+t.slice(1));a.status=c.reserveStatus==='none'?'':c.reserveStatus;}
  if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
  if(c.weather!=='none')b.field.setWeather(c.weather,user);
  for(const [id,layers]of Object.entries(c.hazards))for(let i=0;i<layers;i++)user.side.addSideCondition(id,target);
  const initialSelf=b.p1.pokemon.reduce((n,a)=>n+a.hp/a.maxhp,0),initialFoe=foe.hp/foe.maxhp;
  const hazards=()=>Object.fromEntries(Object.entries(user.side.sideConditions).map(([id,s])=>[id,s.layers||1]));
  const snap=()=>{const a=b.p1.active[0],f=b.p2.active[0];return {active:a===own?'self':'reserve0',selfHp:a.hp/a.maxhp,foeHp:f.hp/f.maxhp,selfStages:{...a.boosts},foeStages:{...f.boosts},selfStatus:a.status||'none',foeStatus:f.status||'none',hazards:hazards(),utility:initialFoe-f.hp/f.maxhp-(initialSelf-b.p1.pokemon.reduce((n,p)=>n+p.hp/p.maxhp,0))};};
  let entry=null,pre=null,post=null;const event=b.runEvent;
  b.runEvent=function(id,actor,...args){const before=actor?.hp;const result=event.call(this,id,actor,...args);if(id==='EntryHazard'&&bench.includes(actor))entry={hp:actor.hp,max:actor.maxhp,before,status:actor.status||'none',speed:actor.boosts.spe,hazards:hazards()};return result;};
  const residual=b.residualEvent;b.residualEvent=function(...args){pre=snap();const r=residual.apply(this,args);post=snap();return r;};
  const faint=b.faintMessages;b.faintMessages=function(...args){const r=faint.apply(this,args);if(this.ended&&!pre)pre=post=snap();return r;};
  const chance=b.randomChance;b.randomChance=function(n,d){if(n===1&&d===24)return false;return n>=d;};b.random=()=>5;
  if(mode==='switch')b.makeChoices(enemy?'move 1':'switch 2',enemy?'switch 2':'move 1');else b.makeChoices('move 1','move 1');
  if(!pre&&!b.ended&&b.requestState==='switch')b.makeChoices(enemy?'':'switch 2',enemy?'switch 2':'');
  if(!pre)throw Error('Missing native entry boundary '+JSON.stringify(c));
  rows.push({config:c,entry,pre,post});
 }
 return rows;
};
if(typeof process!=='undefined'&&process.argv&&require.main===module){const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));console.log('Toxic Spikes native oracle PASS: '+rows.length+' queued entries');}
