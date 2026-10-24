'use strict';
// Fully declared synthetic, legal builds. Record the action exchange immediately
// before source residual dispatch; Leftovers/end-turn effects are outside this
// comparison. The wrapper observes and delegates: it does not change execution.
exports.run = function(Battle) {
  const outcomes=[];
  for (const hp of [1,200]) for (const action of ['shadowclaw','swordsdance','recover','switch']) {
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(species,ability,moves,item='',extra={})=>({species,ability,moves,item,level:100,
      movesInfo:moves.map(()=>({pp:40,maxPp:40})),...extra});
    b.setPlayer('p1',{team:[set('Arceus','multitype',['shadowclaw','swordsdance','recover'],'leftovers',
      {nature:'Relaxed',evs:{def:252},ivs:{atk:0,spe:0}}),
      set('Marshadow','technician',['shadowsneak'],'marshadiumz')]});
    b.setPlayer('p2',{team:[set('Mewtwo','pressure',['swift'],'',
      {nature:'Timid',evs:{hp:252,spe:252}})]});
    b.makeChoices('team 12','team 1');
    const own=b.p1.active[0],foe=b.p2.active[0],reserve=b.p1.pokemon[1];
    if(own.maxhp!==381||own.storedStats.atk!==245||own.storedStats.def!==372||own.storedStats.spe!==220
       ||reserve.maxhp!==321||foe.maxhp!==416||foe.storedStats.spe!==394)
      throw Error('Jointly legal source-stat fixture changed');
    own.hp=hp;
    const random=b.random;
    const bounds=(attacker,target,id)=>{
      const values=[];
      for(let roll=0;roll<16;roll++){
        b.random=()=>roll;const move=b.dex.getActiveMove(id);move.willCrit=false;
        values.push(b.actions.getDamage(attacker,target,move,true));
      }
      return {minimum:Math.min(...values),average:values.reduce((a,v)=>a+v,0)/values.length};
    };
    const features={attack:bounds(own,foe,'shadowclaw'),incoming:bounds(foe,own,'swift'),reserveAttack:bounds(reserve,foe,'shadowsneak')};
    b.random=random;
    let observed=null;
    const residual=b.residualEvent;
    b.residualEvent=function(...args) {
      if(observed===null)observed={hp,action,ownHp:own.hp,reserveHp:reserve.hp,foeHp:foe.hp,
        atkStage:own.boosts.atk,active:this.p1.active[0].species.id,features};
      return residual.apply(this,args);
    };
    b.makeChoices(action==='switch'?'switch 2':'move '+({shadowclaw:1,swordsdance:2,recover:3}[action]),'move 1');
    if(!observed)throw Error('Missing source pre-residual boundary');
    if(action==='switch'&&(observed.active!=='marshadow'||observed.reserveHp!==321))throw Error('Normal immunity switch outcome changed');
    if(hp===1&&action!=='switch'&&observed.ownHp!==0)throw Error('Lethal Swift stay outcome changed');
    if(hp===200&&action==='swordsdance'&&observed.atkStage!==2)throw Error('Source setup stage missing');
    observed.utility=(416-observed.foeHp)/416-(hp-observed.ownHp)/381-(321-observed.reserveHp)/321;
    if(!Number.isFinite(observed.utility))throw Error('Nonfinite source outcome');
    outcomes.push(observed);
  }
  const heal=outcomes.find(o=>o.hp===200&&o.action==='recover');
  const setup=outcomes.find(o=>o.hp===200&&o.action==='swordsdance');
  if(heal.ownHp-setup.ownHp!==191)throw Error('Native recovery/source reply alignment changed');
  return outcomes;
};
