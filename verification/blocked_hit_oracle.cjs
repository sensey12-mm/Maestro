'use strict';

// Source-executed synthetic conditions, not learnset, runtime or player evidence.
// Observe the native exchange before residuals, retaining the original dispatch.
exports.run = function(Battle) {
  const cases = [
    {id:'cc-hit', action:'closecombat'},
    {id:'cc-protect', action:'closecombat', protect:'opponent'},
    {id:'cc-immune', action:'closecombat', foeType:'ghost'},
    {id:'kiss-hit', action:'drainingkiss'},
    {id:'kiss-protect', action:'drainingkiss', protect:'opponent'},
    {id:'pivot-hit', action:'uturn'},
    {id:'pivot-protect', action:'uturn', protect:'opponent'},
    {id:'enemy-pivot-protect', action:'bulkup', reply:'uturn', protect:'self'},
    {id:'thief-hit', action:'spectralthief', foeType:'psychic', boosted:true},
    {id:'thief-protect', action:'spectralthief', foeType:'psychic', boosted:true, protect:'opponent'},
    {id:'thief-immune', action:'spectralthief', boosted:true},
    {id:'rock-hit', action:'rocktomb'},
    {id:'rock-protect', action:'rocktomb', protect:'opponent'},
    {id:'flinch-protect', action:'ironhead', protect:'opponent'},
    {id:'freeze-protect', action:'icebeam', protect:'opponent'},
    {id:'orb-hit', action:'closecombat', item:'lifeorb'},
    {id:'orb-protect', action:'closecombat', item:'lifeorb', protect:'opponent'},
    {id:'enemy-cc-immune', action:'bulkup', reply:'closecombat', selfType:'ghost'}
  ];
  const results=[];
  for(const config of cases) {
    const c={reply:'bulkup',selfType:'normal',foeType:'normal',protect:'none',boosted:false,item:'',...config};
    const b=new Battle({formatid:'gen9customgame',seed:[7,11,13,17]});
    const set=(move,item='')=>({species:'Mew',ability:'runaway',moves:[move],item,level:100,
      movesInfo:[{pp:40,maxPp:40}]});
    b.setPlayer('p1',{team:[set(c.action,c.item),set('tackle')]});
    b.setPlayer('p2',{team:[set(c.reply),set('tackle')]});
    b.makeChoices('team 12','team 12');
    const own=b.p1.active[0],foe=b.p2.active[0],reserve=b.p1.pokemon[1];
    for(const actor of [own,foe,reserve]) {
      actor.maxhp=actor.baseMaxhp=actor.hp=1000;
      for(const stat of ['atk','def','spa','spd','spe'])actor.storedStats[stat]=220;
    }
    own.hp=500;own.storedStats.spe=300;foe.storedStats.spe=100;
    own.types=own.baseTypes=[c.selfType[0].toUpperCase()+c.selfType.slice(1)];
    foe.types=foe.baseTypes=[c.foeType[0].toUpperCase()+c.foeType.slice(1)];
    reserve.types=reserve.baseTypes=['Normal'];
    if(c.boosted){foe.boosts.atk=2;foe.boosts.def=2;}
    if(c.protect!=='none')(c.protect==='self'?own:foe).addVolatile('protect');
    let observed=null,damageStages=null;
    const damage=b.actions.getDamage;
    b.actions.getDamage=function(source,target,move,...args){
      if(move.id==='spectralthief')damageStages={attacker:{...source.boosts},target:{...target.boosts}};
      return damage.call(this,source,target,move,...args);
    };
    const residual=b.residualEvent;
    b.residualEvent=function(...args){
      if(observed===null)observed={config:c,active:this.p1.active[0]===own?'self':'reserve',
        selfHp:this.p1.active[0].hp/1000,ownHp:own.hp/1000,foeHp:foe.hp/1000,
        selfStages:{...this.p1.active[0].boosts},foeStages:{...foe.boosts},
        selfStatus:this.p1.active[0].status||'none',foeStatus:foe.status||'none',damageStages};
      return residual.apply(this,args);
    };
    b.makeChoices('move 1','move 1');
    if(!observed&&b.requestState==='switch')b.makeChoices('switch 2');
    if(!observed)throw Error('Missing native pre-residual boundary: '+c.id);
    if(c.id==='cc-hit'&&(observed.selfStages.def!==-1||observed.selfStages.spd!==-1))
      throw Error('Native successful Close Combat did not drop defenses');
    if(['cc-protect','cc-immune','orb-protect'].includes(c.id)&&
        (observed.selfStages.def!==0||observed.selfStages.spd!==0||observed.ownHp!==.5))
      throw Error('Native blocked Close Combat changed its user');
    if(c.id==='kiss-hit'&&observed.ownHp<=.5)throw Error('Native successful drain did not heal');
    if(c.id==='kiss-protect'&&observed.ownHp!==.5)throw Error('Native blocked drain healed');
    if(c.id==='pivot-hit'&&observed.active!=='reserve')throw Error('Native successful pivot failed');
    if(c.id==='thief-hit'&&(!damageStages||damageStages.attacker.atk!==2||damageStages.target.def!==0))
      throw Error('Native boost theft no longer precedes damage');
    results.push(observed);
  }
  return results;
};

if(typeof process!=='undefined'&&process.argv&&require.main===module){
  const path=require('path');
  const rows=exports.run(require(path.resolve(process.argv[2],'sim/battle')).Battle);
  console.log('Blocked-hit native oracle PASS: '+rows.length+' exchanges before residuals');
}
