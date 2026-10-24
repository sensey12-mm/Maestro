'use strict';

// Explicit synthetic conditions, not learnset, runtime or opponent knowledge.
// All observers delegate unchanged to the pinned simulator's original methods.
exports.run = function(Battle) {
  const cases = [
    {id:'orb-full', hp:1000},
    {id:'orb-near-cap', hp:980},
    {id:'orb-uncapped', hp:500},
    {id:'orb-survive-by-one', hp:99, targetHp:3},
    {id:'orb-exact-faint', hp:98, targetHp:3},
    {id:'orb-lethal', hp:1},
    {id:'drain-one', item:'', hp:500, targetHp:1},
    {id:'drain-two', item:'', hp:500, targetHp:2},
    {id:'drain-three', item:'', hp:500, targetHp:3},
    {id:'drain-five', item:'', hp:500, targetHp:5},
    {id:'odd-base-max', maxHp:1001, hp:1001},
    {id:'magic-guard', hp:980, ability:'magicguard'},
    {id:'magic-room', hp:980, magicRoom:true},
    {id:'protect', hp:980, protect:true},
    {id:'enemy-orb-full', hp:1000, enemy:true},
    {id:'enemy-orb-near-cap', hp:980, enemy:true},
    {id:'enemy-orb-survive', hp:99, targetHp:3, enemy:true},
    {id:'enemy-orb-faint', hp:98, targetHp:3, enemy:true},
    {id:'enemy-drain-three', item:'', hp:500, targetHp:3, enemy:true}
  ];
  const results=[];
  for(const config of cases) {
    const c={hp:500,targetHp:1000,maxHp:1000,item:'lifeorb',ability:'runaway',enemy:false,
      magicRoom:false,protect:false,...config};
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(move,item='',ability='runaway')=>({species:'Mew',ability,moves:[move],item,level:100,
      movesInfo:[{pp:40,maxPp:40}]});
    b.setPlayer('p1',{team:[set(c.enemy?'bulkup':'drainingkiss',c.enemy?'':c.item,c.enemy?'runaway':c.ability),set('tackle')]});
    b.setPlayer('p2',{team:[set(c.enemy?'drainingkiss':'bulkup',c.enemy?c.item:'',c.enemy?c.ability:'runaway'),set('tackle')]});
    b.makeChoices('team 12','team 12');
    const self=b.p1.active[0],foe=b.p2.active[0],user=c.enemy?foe:self,target=c.enemy?self:foe;
    for(const actor of [self,foe]) {
      actor.maxhp=actor.baseMaxhp=actor.hp=1000;
      actor.types=actor.baseTypes=['Normal'];
      for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;
    }
    user.maxhp=user.baseMaxhp=c.maxHp;user.hp=c.hp;target.hp=c.targetHp;
    user.storedStats.spe=300;target.storedStats.spe=100;
    if(c.magicRoom)b.field.addPseudoWeather('magicroom',user);
    if(c.protect)target.addVolatile('protect');
    const events=[];
    const heal=b.heal;
    b.heal=function(amount,actor,source,effect){
      const before=actor.hp,result=heal.call(this,amount,actor,source,effect);
      if(actor===user&&effect==='drain')events.push({effect:'drain',amount,before,after:actor.hp});
      return result;
    };
    const damage=b.damage;
    b.damage=function(amount,actor,source,effect,...args){
      const before=actor.hp,result=damage.call(this,amount,actor,source,effect,...args);
      if(actor===user&&effect&&effect.id==='lifeorb')events.push({effect:'lifeorb',before,after:actor.hp});
      return result;
    };
    let observed=null;
    const residual=b.residualEvent;
    b.residualEvent=function(...args){
      if(observed===null)observed={config:c,selfHp:self.hp/self.maxhp,foeHp:foe.hp/foe.maxhp,
        selfStages:{...self.boosts},foeStages:{...foe.boosts},events};
      return residual.apply(this,args);
    };
    b.makeChoices('move 1','move 1');
    if(!observed)throw Error('Missing native pre-residual boundary: '+c.id);
    const drain=events.findIndex(e=>e.effect==='drain'),orb=events.findIndex(e=>e.effect==='lifeorb');
    if(!c.protect&&(drain<0||orb>=0&&drain>=orb))throw Error('Native drain/recoil ordering changed: '+c.id);
    if(c.protect&&events.length)throw Error('Blocked hit ran healing/recoil');
    const finalHp=(c.enemy?observed.foeHp:observed.selfHp)*c.maxHp;
    if(['orb-full','orb-near-cap'].includes(c.id)&&Math.abs(finalHp-900)>1e-8)throw Error('Capped healing must precede recoil');
    if(c.id==='orb-survive-by-one'&&Math.abs(finalHp-1)>1e-8)throw Error('Drain must avert lethal recoil');
    if(['orb-exact-faint','orb-lethal'].includes(c.id)&&finalHp!==0)throw Error('Drain cannot revive after recoil');
    if(c.id==='drain-three'&&finalHp!==502)throw Error('Native drain rounding changed');
    results.push(observed);
  }
  return results;
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const rows=exports.run(require(require('path').resolve(process.argv[2],'sim/battle')).Battle);
  console.log('Healing/recoil native oracle PASS: '+rows.length+' exchanges; drain before recoil, HP caps, rounding and fainting');
}
