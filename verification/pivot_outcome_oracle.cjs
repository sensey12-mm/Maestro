'use strict';

// Declared synthetic teams and stats, not learnset or opponent evidence.
// Observe the original queue/requests/entry dispatch without replacing mechanics.
exports.run = function(Battle) {
  const cases=[
    {id:'hit-setup'},
    {id:'hit-attack',reply:'tackle'},
    {id:'slow-pivot',reply:'tackle',userSpeed:100,targetSpeed:300},
    {id:'tie-pivot',userSpeed:100},
    {id:'rocks',hazards:{stealthrock:1}},
    {id:'spikes-one',hazards:{spikes:1}},
    {id:'spikes-two',hazards:{spikes:2}},
    {id:'spikes-three',hazards:{spikes:3}},
    {id:'rocks-spikes',hazards:{stealthrock:1,spikes:2}},
    {id:'web',hazards:{stickyweb:1}},
    {id:'rocks-web',hazards:{stealthrock:1,stickyweb:1}},
    {id:'boots',hazards:{stealthrock:1,spikes:3,stickyweb:1},reserveItem:'heavydutyboots'},
    {id:'magic-guard',hazards:{stealthrock:1,spikes:3,stickyweb:1},reserveAbility:'magicguard'},
    {id:'levitate',hazards:{stealthrock:1,spikes:3,stickyweb:1},reserveAbility:'levitate'},
    {id:'flying-rocks',hazards:{stealthrock:1,spikes:3,stickyweb:1},reserveTypes:['flying']},
    {id:'double-weak-rocks',hazards:{stealthrock:1},reserveTypes:['fire','flying']},
    {id:'odd-max-hp',hazards:{stealthrock:1,spikes:2},reserveMax:1001,reserveHp:1001},
    {id:'suppressed-boots',hazards:{stealthrock:1,spikes:1,stickyweb:1},reserveItem:'heavydutyboots',magicRoom:true},
    {id:'life-orb',item:'lifeorb'},
    {id:'protect-orb',item:'lifeorb',protect:true},
    {id:'target-ko',targetHp:3},
    {id:'target-last',targetHp:3,foeBench:0},
    {id:'possible-target-ko',targetHp:80,item:'lifeorb'},
    {id:'recoil-ko',hp:1,item:'lifeorb'},
    {id:'recoil-last',hp:1,item:'lifeorb',ownBench:0},
    {id:'recoil-survive-one',hp:101,item:'lifeorb'},
    {id:'entry-ko',hazards:{stealthrock:1},reserveHp:100},
    {id:'entry-survive-one',hazards:{stealthrock:1},reserveHp:126},
    {id:'ambiguous-reserve',ownBench:2},
    {id:'no-reserve',ownBench:0},
    {id:'opponent-pivot',enemy:true},
    {id:'toxic-spikes',hazards:{toxicspikes:1}},
    {id:'download-entry',reserveAbility:'download'},
    {id:'slower-flinch',reply:'ironhead',userSpeed:100,targetSpeed:300},
    {id:'slower-defense-drop',reply:'closecombat',userSpeed:100,targetSpeed:300}
  ];
  const rows=[];
  for(const config of cases){
    const c={reply:'bulkup',hp:500,targetHp:1000,userSpeed:300,targetSpeed:100,item:'',ownBench:1,foeBench:1,
      enemy:false,reserveMax:1000,reserveHp:1000,reserveTypes:['normal'],reserveItem:'',reserveAbility:'runaway',
      hazards:{},magicRoom:false,protect:false,...config};
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(move,item='',ability='runaway')=>({species:'Mew',ability,moves:[move],item,level:100,
      movesInfo:[{pp:40,maxPp:40}]});
    b.setPlayer('p1',{team:[set(c.enemy?c.reply:'uturn',c.enemy?'':c.item),
      ...Array.from({length:c.ownBench},()=>set('tackle',c.reserveItem,c.reserveAbility))]});
    b.setPlayer('p2',{team:[set(c.enemy?'uturn':c.reply,c.enemy?c.item:''),
      ...Array.from({length:c.foeBench},()=>set('tackle',c.reserveItem,c.reserveAbility))]});
    const team=n=>'team '+Array.from({length:n+1},(_,i)=>i+1).join('');
    b.makeChoices(team(c.ownBench),team(c.foeBench));
    const own=b.p1.active[0],foe=b.p2.active[0],ownBench=b.p1.pokemon.slice(1),foeBench=b.p2.pokemon.slice(1);
    const user=c.enemy?foe:own,target=c.enemy?own:foe,pivotSide=user.side;
    for(const actor of [own,foe,...ownBench,...foeBench]){
      actor.maxhp=actor.baseMaxhp=actor.hp=1000;actor.types=actor.baseTypes=['Normal'];
      for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;
    }
    user.hp=c.hp;target.hp=c.targetHp;user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;
    for(const actor of c.enemy?foeBench:ownBench){
      actor.maxhp=actor.baseMaxhp=c.reserveMax;actor.hp=c.reserveHp;
      actor.types=actor.baseTypes=c.reserveTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    }
    if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
    if(c.protect)target.addVolatile('protect');
    for(const [id,layers] of Object.entries(c.hazards))for(let i=0;i<layers;i++)pivotSide.addSideCondition(id,target);
    const initialOwn=own.hp/1000+ownBench.reduce((n,p)=>n+p.hp/p.maxhp,0),initialFoe=foe.hp/1000;
    const events=[];let observed=null,pivotRequest=null;
    const snapshot=()=>{
      const active=b.p1.active[0],enemy=b.p2.active[0];
      return {active:active===own?'self':'reserve'+ownBench.indexOf(active),selfHp:active.hp/active.maxhp,foeHp:enemy.hp/enemy.maxhp,
        selfStages:{...active.boosts},foeStages:{...enemy.boosts},selfStatus:active.status||'none',foeStatus:enemy.status||'none',
        originalOwnHp:own.hp/1000,utility:initialFoe-foe.hp/1000-(initialOwn-own.hp/1000-ownBench.reduce((n,p)=>n+p.hp/p.maxhp,0))};
    };
    const run=b.actions.runMove;
    b.actions.runMove=function(move,pokemon,...args){events.push('move:'+(typeof move==='string'?move:move.id)+':'+(pokemon===own?'self':'opponent'));return run.call(this,move,pokemon,...args);};
    const residual=b.residualEvent;
    b.residualEvent=function(...args){events.push('residual');if(!observed)observed=snapshot();return residual.apply(this,args);};
    const faint=b.faintMessages;
    b.faintMessages=function(...args){const result=faint.apply(this,args);if(this.ended&&!observed)observed=snapshot();return result;};
    const request=b.makeRequest;
    b.makeRequest=function(type){
      events.push('request:'+type);
      if(type==='switch'&&pivotRequest===null)pivotRequest={userHp:user.hp/1000,ownActive:b.p1.active[0]===own?'self':'reserve'};
      return request.call(this,type);
    };
    b.makeChoices('move 1','move 1');
    if(!observed&&!b.ended&&b.requestState==='switch'){
      // Explicit oracle choice, never a production assumption for multiple reserves.
      b.makeChoices(c.enemy?'':'switch 2',c.enemy?'switch 2':'');
    }
    if(!observed)throw Error('Missing native pivot boundary: '+c.id+' '+JSON.stringify(events));
    if(c.id==='hit-attack'&&!(events.indexOf('request:switch')<events.indexOf('move:tackle:opponent')&&observed.active==='reserve0'&&observed.selfHp<1))
      throw Error('Native pivot must enter before the slower attack');
    if(c.id==='life-orb'&&(!pivotRequest||pivotRequest.userHp!==.4||observed.originalOwnHp!==.4||observed.selfHp!==1))
      throw Error('Native Life Orb recoil must stay on the outgoing user before entry');
    if(c.id==='protect-orb'&&(pivotRequest||observed.active!=='self'||observed.originalOwnHp!==.5))throw Error('Blocked U-turn pivoted or recoiled');
    if(c.id==='target-last'&&(!b.ended||pivotRequest||observed.active!=='self'))throw Error('Terminal target KO must stop pivot');
    if(c.id==='target-ko'&&(b.ended||observed.active!=='reserve0'))throw Error('Nonterminal target KO must still pivot');
    if(c.id==='entry-survive-one'&&Math.abs(observed.selfHp-.001)>1e-10)throw Error('Native hazard survival boundary changed');
    rows.push({config:c,...observed,pivotRequest,events,ended:!!b.ended});
  }
  return rows;
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  console.log('Pivot outcome native oracle PASS: '+rows.length+' exchanges; request/entry/action ordering, target KO, recoil and hazard interactions');
}
