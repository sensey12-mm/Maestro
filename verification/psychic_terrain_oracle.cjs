'use strict';

// Synthetic conditional teams/stats; no learnset, hidden-party or runtime claims.
exports.run = function(Battle) {
  // A real native setter turn establishes the field without HP/stage effects.
  {
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=()=>({species:'Mew',ability:'runaway',moves:['psychicterrain'],movesInfo:[{pp:10,maxPp:10}],level:100});
    b.setPlayer('p1',{team:[set()]});b.setPlayer('p2',{team:[set()]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    b.makeChoices('move 1','move 1');
    if(b.field.terrain!=='psychicterrain'||b.turn!==2||!b.log.some(l=>l.startsWith('|move|')&&l.includes('|Psychic Terrain|')))
      throw Error('Native past Psychic Terrain setter did not establish its field');
    if([b.p1.active[0],b.p2.active[0]].some(p=>p.hp!==p.maxhp||Object.values(p.boosts).some(n=>n!==0)))
      throw Error('Native Psychic Terrain setter changed actor HP/stages');
  }
  const grounds={ground:{types:['psychic'],ability:'runaway'},flying:{types:['psychic','flying'],ability:'runaway'},
    levitate:{types:['psychic'],ability:'levitate'}};
  const seeds=[];
  for(const [user,g] of Object.entries(grounds))for(const target of ['ground','flying','levitate'])
    seeds.push({id:'grounding-'+user+'-'+target,...g,targetTypes:[target==='flying'?'flying':'normal'],
      targetAbility:target==='levitate'?'levitate':'shadowshield'});
  seeds.push(
    {id:'critical-stages',move:'psystrike',attackStage:-2,defenseStage:2},
    {id:'odd-level-hp',move:'psychocut',level:99,targetMax:1001,targetHp:1001},
    {id:'round-low',attack:11,defense:300},
    {id:'magic-room',room:true},
    {id:'burned-physical',move:'psychocut',status:'brn'},
    {id:'technician-before-terrain',move:'psychocut',ability:'technician'});
  for(const mode of ['none','ended','restart'])for(const [user,g] of Object.entries(grounds))
    seeds.push({id:'field-'+mode+'-'+user,...g,terrainMode:mode});
  seeds.push({id:'dark-immunity',targetTypes:['dark']},{id:'chipped-shield',targetHp:999});
  const queued=[
    {id:'priority-ground',move:'quickattack',targetAbility:'runaway'},
    {id:'priority-flying-target',move:'quickattack',targetTypes:['flying'],targetAbility:'runaway'},
    {id:'priority-levitate-target',move:'quickattack',targetAbility:'levitate'},
    {id:'priority-flying-user',move:'quickattack',types:['flying'],targetAbility:'runaway'},
    {id:'priority-levitate-user',move:'quickattack',ability:'levitate',targetAbility:'runaway'},
    {id:'stall-priority',move:'quickattack',ability:'stall',targetAbility:'runaway'},
    {id:'tail-priority',move:'quickattack',item:'laggingtail',targetAbility:'runaway'},
    {id:'stall-tail-priority',move:'quickattack',ability:'stall',item:'laggingtail',targetAbility:'runaway'},
    {id:'tail-room-priority',move:'quickattack',item:'laggingtail',room:true,targetAbility:'runaway'},
    {id:'stall-ordinary',move:'tackle',ability:'stall',types:['normal'],targetAbility:'runaway'},
    {id:'tail-ordinary',move:'tackle',item:'laggingtail',types:['normal'],targetAbility:'runaway'},
    {id:'prankster-setup',move:'bulkup',ability:'prankster',targetAbility:'runaway'},
    {id:'prankster-recover',move:'recover',ability:'prankster',targetAbility:'runaway'},
    {id:'prankster-dark-target',move:'bulkup',ability:'prankster',targetTypes:['dark'],targetAbility:'runaway'},
    {id:'prankster-herb',move:'geomancy',ability:'prankster',item:'powerherb',targetAbility:'runaway'},
    {id:'psychic-flying-target',targetTypes:['flying']},
    {id:'psychic-levitate-target',targetAbility:'levitate'},
    {id:'psychic-flying-user',types:['psychic','flying']},
    {id:'psychic-levitate-user',ability:'levitate'},
    {id:'priority-one-hp',move:'quickattack',userHp:1,targetAbility:'runaway'},
    {id:'ghost-priority-immune',move:'shadowsneak',ability:'technician'},
    {id:'extreme-ground',move:'extremespeed'},
    {id:'extreme-flying',move:'extremespeed',targetTypes:['flying']},
    {id:'priority-ended',move:'quickattack',terrainMode:'ended'},
    {id:'priority-restart',move:'quickattack',terrainMode:'restart'},
    {id:'priority-none',move:'quickattack',terrainMode:'none'},
    {id:'psychic-none',terrainMode:'none'},
    {id:'psychic-ended',terrainMode:'ended'},
    {id:'psychic-restart',terrainMode:'restart'},
    {id:'psychic-protect',protect:true},
    {id:'psychic-chipped',targetHp:999}
  ];
  const config=(seed,enemy)=>({move:'psyshock',replyMove:'bulkup',ability:'runaway',targetAbility:'shadowshield',item:'lifeorb',targetItem:'none',
    attack:220,defense:220,attackStage:0,defenseStage:0,level:100,types:['psychic'],targetTypes:['normal'],status:'none',
    targetMax:1000,targetHp:1000,userHp:500,userSpeed:300,targetSpeed:100,room:false,weather:'none',terrain:'psychicterrain',
    terrainMode:'active',protect:false,...seed,enemy,...(seed.terrainMode==='none'?{terrain:'none'}:{})});
  function battle(c){
    const b=new Battle({format:{mod:'cobblemon',gameType:'singles',gen:9,ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[7,11,13,17]});
    const set=user=>({species:'Mew',ability:user?c.ability:c.targetAbility,item:(user?c.item:c.targetItem)==='none'?'':user?c.item:c.targetItem,
      moves:[user?c.move:c.replyMove],movesInfo:[{pp:40,maxPp:40}],level:user?c.level:100});
    b.setPlayer('p1',{team:[set(!c.enemy)]});b.setPlayer('p2',{team:[set(c.enemy)]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [self,foe]){actor.maxhp=actor.baseMaxhp=1000;for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;}
    user.hp=c.userHp;target.maxhp=target.baseMaxhp=c.targetMax;target.hp=c.targetHp;
    user.types=user.baseTypes=c.types.map(t=>t[0].toUpperCase()+t.slice(1));target.types=target.baseTypes=c.targetTypes.map(t=>t[0].toUpperCase()+t.slice(1));
    for(const stat of ['atk','spa']){user.storedStats[stat]=c.attack;user.boosts[stat]=c.attackStage;}
    for(const stat of ['def','spd']){target.storedStats[stat]=c.defense;target.boosts[stat]=c.defenseStage;}
    user.storedStats.spe=c.userSpeed;target.storedStats.spe=c.targetSpeed;user.status=c.status==='none'?'':c.status;
    if(c.room)b.field.addPseudoWeather('magicroom',user);
    if(c.terrain!=='none')b.field.setTerrain(c.terrain,user);
    if(['ended','restart'].includes(c.terrainMode))b.field.clearTerrain();
    if(c.terrainMode==='restart')b.field.setTerrain('psychicterrain',user);
    if(c.protect)target.addVolatile('protect');
    return {b,self,foe,user,target};
  }
  const damage=[];
  for(const seed of seeds)for(const enemy of [false,true]){
    const c=config(seed,enemy),{b,user,target}=battle(c),values=[];
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
      b.random=()=>100-roll;const move=b.dex.getActiveMove(c.move);move.willCrit=crit;
      values.push({crit,roll,damage:b.actions.getDamage(user,target,move,true)||0});
    }
    damage.push({config:c,values});
  }
  const exchanges=[];
  for(const seed of queued)for(const enemy of [false,true]){
    const c=config(seed,enemy),{b,self,foe,user,target}=battle(c);
    b.randomChance=(n,d)=>n>=d;b.random=()=>5;
    const ownBefore=self.hp/self.maxhp,foeBefore=foe.hp/foe.maxhp,events=[];let observed=null;
    const run=b.actions.runMove;
    b.actions.runMove=function(move,actor,...args){events.push({id:typeof move==='string'?move:move.id,actor:actor===self?'self':'opponent'});return run.call(this,move,actor,...args);};
    const residual=b.residualEvent;
    b.residualEvent=function(...args){if(!observed)observed={config:c,selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,
      selfStages:{...self.boosts},foeStages:{...foe.boosts},selfStatus:self.status||'none',foeStatus:foe.status||'none',events,
      utility:foeBefore-foe.hp/foe.maxhp-(ownBefore-self.hp/self.maxhp)};return residual.apply(this,args);};
    b.makeChoices('move 1','move 1');if(!observed)throw Error('Missing native terrain exchange boundary: '+c.id);
    if(['priority-ground','priority-flying-user','priority-levitate-user','stall-priority','tail-priority','stall-tail-priority',
        'tail-room-priority','priority-one-hp','priority-restart','extreme-ground','ghost-priority-immune','psychic-protect'].includes(c.id)
        &&(user.hp!==c.userHp||target.hp!==c.targetHp))throw Error('Native blocked terrain hit changed HP or recoiled: '+c.id);
    if(['priority-flying-target','priority-levitate-target','priority-ended','priority-none','extreme-flying'].includes(c.id)
        &&!(target.hp<c.targetHp&&user.hp===c.userHp-100))throw Error('Native eligible priority hit failed/recoiled incorrectly: '+c.id);
    if(c.id==='prankster-setup'&&user.boosts.atk!==1)throw Error('Terrain blocked self-target Prankster');
    if(c.id==='prankster-recover'&&user.hp!==1000)throw Error('Terrain blocked self-target recovery');
    exchanges.push(observed);
  }
  if(damage.length!==52||exchanges.length!==62)throw Error('Terrain matrix inventory changed');
  return {damage,exchanges};
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  console.log('Psychic Terrain native oracle PASS: 52 configurations, 1664 critical/roll outcomes, 62 queued exchanges; grounding, field changes, move priority and self-target exceptions');
}
