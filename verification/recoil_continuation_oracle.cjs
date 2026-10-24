'use strict';

// Synthetic, declared conditions. Native teams here are NOT opponent knowledge.
// Observers retain original dispatch, faint handling, queue and replacement code.
exports.run = function(Battle) {
  const cases = [
    {id:'bulk-up',slow:'bulkup'},
    {id:'swords-dance',slow:'swordsdance'},
    {id:'nasty-plot',slow:'nastyplot'},
    {id:'recover',slow:'recover',targetHp:500},
    {id:'herb-geomancy',slow:'geomancy',slowItem:'powerherb'},
    {id:'no-target-orb',slow:'tackle',slowItem:'lifeorb'},
    {id:'no-target-drain',slow:'drainingkiss',targetHp:500},
    {id:'no-target-pivot',slow:'uturn'},
    {id:'no-target-drops',slow:'closecombat'},
    {id:'close-combat-faint',fast:'closecombat'},
    {id:'drain-faint',fast:'drainingkiss'},
    {id:'uturn-recoil-faint',fast:'uturn'},
    {id:'flinch-before-recoil',fast:'ironhead'},
    {id:'speed-drop-before-recoil',fast:'rocktomb'},
    {id:'multiple-reserves',ownBench:2},
    {id:'hazard-reserve',rocks:true},
    {id:'terminal-bulk',ownBench:0},
    {id:'terminal-recover',ownBench:0,slow:'recover',targetHp:500},
    {id:'terminal-attack',ownBench:0,slow:'tackle',slowItem:'lifeorb'},
    {id:'enemy-hidden-reserve',enemy:true,foeBench:1},
    {id:'enemy-hidden-last',enemy:true,foeBench:0},
    {id:'enemy-hidden-recover',enemy:true,foeBench:1,slow:'recover',targetHp:500},
    {id:'magic-guard',ability:'magicguard'},
    {id:'magic-room',magicRoom:true},
    {id:'protect',protect:true},
    {id:'recoil-survives',hp:101},
    {id:'simultaneous-active-ko',fast:'closecombat',targetHp:3}
  ];
  const rows=[];
  for(const config of cases){
    const c={fast:'tackle',slow:'bulkup',hp:1,targetHp:1000,ownBench:1,foeBench:1,
      enemy:false,slowItem:'',ability:'runaway',magicRoom:false,protect:false,rocks:false,...config};
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(move,item='',ability='runaway')=>({species:'Mew',ability,moves:[move],item,level:100,
      movesInfo:[{pp:40,maxPp:40}]});
    b.setPlayer('p1',{team:[set(c.enemy?c.slow:c.fast,c.enemy?c.slowItem:'lifeorb',c.enemy?'runaway':c.ability),
      ...Array.from({length:c.ownBench},()=>set('tackle'))]});
    b.setPlayer('p2',{team:[set(c.enemy?c.fast:c.slow,c.enemy?'lifeorb':c.slowItem,c.enemy?c.ability:'runaway'),
      ...Array.from({length:c.foeBench},()=>set('tackle'))]});
    b.makeChoices('team '+Array.from({length:c.ownBench+1},(_,i)=>i+1).join(''),
      'team '+Array.from({length:c.foeBench+1},(_,i)=>i+1).join(''));
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [...b.p1.pokemon,...b.p2.pokemon]){
      actor.maxhp=actor.baseMaxhp=actor.hp=1000;actor.types=actor.baseTypes=['Normal'];
      for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;
    }
    user.hp=c.hp;user.storedStats.spe=300;target.hp=c.targetHp;target.storedStats.spe=100;
    if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
    if(c.protect)target.addVolatile('protect');
    if(c.rocks)b.p1.addSideCondition('stealthrock',target);
    const events=[];let observed=null;
    const snapshot=()=>({selfHp:self.hp/1000,foeHp:foe.hp/1000,selfStages:{...self.boosts},foeStages:{...foe.boosts},
      ownActive:b.p1.active[0]===self?'self':'reserve',reserveHp:b.p1.pokemon.slice(1).map(p=>p.hp/1000)});
    const run=b.actions.runMove;
    b.actions.runMove=function(move,pokemon,...args){
      events.push('move:'+(typeof move==='string'?move:move.id)+':'+(pokemon===self?'self':'opponent'));
      return run.call(this,move,pokemon,...args);
    };
    const faint=b.faintMessages;
    b.faintMessages=function(...args){
      const before=[self.fainted,foe.fainted],result=faint.apply(this,args);
      if(!before[0]&&self.fainted)events.push('faint:self');
      if(!before[1]&&foe.fainted)events.push('faint:opponent');
      if(this.ended&&!observed)observed=snapshot();
      return result;
    };
    const residual=b.residualEvent;
    b.residualEvent=function(...args){
      events.push('residual');if(!observed)observed=snapshot();return residual.apply(this,args);
    };
    const request=b.makeRequest;
    b.makeRequest=function(type){events.push('request:'+type);return request.call(this,type);};
    b.makeChoices('move 1','move 1');
    if(!observed)throw Error('Missing native terminal/pre-residual boundary: '+c.id);
    const ended=!!b.ended,requestState=b.requestState;
    if(c.id==='bulk-up'&&(observed.foeStages.atk!==1||observed.selfHp!==0||ended||requestState!=='switch'))
      throw Error('Native recoil continuation boundary changed');
    if(c.id.startsWith('terminal-')&&(!ended||events.some(e=>e.startsWith('move:'+c.slow+':opponent'))))
      throw Error('Terminal recoil must suppress remaining action');
    const faintIndex=events.indexOf('faint:self'),slowIndex=events.indexOf('move:'+c.slow+':opponent');
    if(c.id==='bulk-up'&&!(faintIndex>=0&&slowIndex>faintIndex&&events.indexOf('residual')>slowIndex
        &&events.indexOf('request:switch')>events.indexOf('residual')))
      throw Error('Native faint/action/residual/replacement ordering changed: '+JSON.stringify(events));
    let afterReplacement=null;
    if(c.id==='hazard-reserve'){
      if(observed.reserveHp[0]!==1)throw Error('Replacement entered before queued action/residual boundary');
      b.makeChoices('switch 2');afterReplacement={active:b.p1.active[0]===self?'self':'reserve',hp:b.p1.active[0].hp/1000};
      if(afterReplacement.active!=='reserve'||afterReplacement.hp!==.875)throw Error('Native deferred hazard entry changed');
    }
    rows.push({config:c,...observed,events,ended,requestState,afterReplacement});
  }
  return rows;
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  console.log('Recoil continuation native oracle PASS: '+rows.length+' exchanges; faint/action/residual/replacement and terminal ordering');
}
