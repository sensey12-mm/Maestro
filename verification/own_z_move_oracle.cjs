'use strict';
// Synthetic owned Marshadow. Exact opposing stats are oracle truth only; the
// production pipeline receives the spectator stream and this owner's request.
module.exports=function(Battle){
  const rows=[],uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){out.push(log[i+2]);i+=2;}
    else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  for(const owner of [0,1])for(const level of [50,100])for(const boundary of ['hit','protect','immune'])
    for(const status of ['','brn'])for(const room of [false,true]){
    const b=new Battle({format:{id:'nativeownz',mod:'cobblemon',gen:9,gameType:'singles',
      ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
    const side='p'+(owner+1),other='p'+(2-owner);
    const set=(species,n,ability,moves,item='')=>({name:'Declared',uuid:uuid(n),species,ability,moves,item,level,
      movesInfo:moves.map(()=>({pp:32,maxPp:32})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
    b.setPlayer(side,{team:[set('Marshadow',1,'technician',['spectralthief','closecombat','shadowsneak','rocktomb'],'marshadiumz'),set('Mew',2,'runaway',['bulkup'])]});
    // Pressure on Blissey is a declared synthetic identity control, not a claim
    // about an obtainable build or installed Cobbleverse acceptance.
    b.setPlayer(other,{team:[set(boundary==='immune'?'Blissey':'Mewtwo',3,'pressure',['bulkup','protect'],'sitrusberry'),set('Mew',4,'runaway',['bulkup'])]});
    const user=b[side].active[0],target=b[other].active[0];
    target.hp=Math.floor(target.maxhp/2);target.eatItem();
    if(status)user.trySetStatus(status,user);
    if(room){b.field.addPseudoWeather('magicroom',user);b.boost({accuracy:-6},user,user);b.boost({evasion:6},target,target);}
    b.add('move',target,'Bulk Up',target);b.boost({atk:1,def:1},target,target);
    if(boundary==='protect')target.addVolatile('protect');
    const request=JSON.parse(JSON.stringify(b.getRequests('move')[owner]));
    const options=request.active[0].canZMove;
    if(!options||options[0].move!=='Soul-Stealing 7-Star Strike'||options.filter(Boolean).length!==1)throw Error('Native Z eligibility/conversion changed');
    const history=publicLog(b.log),values=[],logSize=b.log.length;
    for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
      const move=b.actions.getActiveZMove(b.dex.getActiveMove('spectralthief'),user);move.willCrit=crit;
      if(move.id!=='soulstealing7starstrike'||move.basePower!==195||move.accuracy!==true||move.stealsBoosts)throw Error('Native signature Z definition changed');
      target.getMoveHitData(move).zBrokeProtect=boundary==='protect';
      b.randomizer=n=>Math.floor(n*roll/100);
      const damage=boundary==='immune'?0:b.actions.getDamage(user,target,move,true);
      values.push({crit,roll,damage});
    }
    // Damage-oracle probes emit native -zbroken annotations. Discard only
    // those test-probe emissions before running the independent real action.
    b.log.length=logSize;
    // Full native execution independently verifies consumption, PP and public
    // event ordering, including Protect and immunity. No hand-written result.
    b.randomChance=(n,d)=>n>=d;b.random=()=>0;
    const pp=user.moveSlots[0].pp,stages={...target.boosts};
    const choices=owner===0?['move 1 zmove','move 1']:['move 1','move 1 zmove'];
    b.makeChoices(...choices);
    const events=publicLog(b.log),after=b.getRequests('move')[owner];
    if(!b[side].zMoveUsed||user.moveSlots[0].pp!==pp-2||after.active[0].canZMove)throw Error('Native use/Pressure PP/eligibility boundary changed');
    if(!events.includes('|-zpower|'+side+'a: '+uuid(1)+'\n|move|'+side+'a: '+uuid(1)+'|Soul-Stealing 7-Star Strike|'+other+'a: '+uuid(3)+'|[zeffect]'))throw Error('Native activation ordering changed');
    if(boundary==='protect'&&!events.includes('|-zbroken|'))throw Error('Native Z must pierce Protect');
    if(user.boosts.atk!==0||user.boosts.def!==0||target.boosts.def<stages.def)throw Error('Z incorrectly stole Spectral Thief boosts');
    rows.push({side,level,boundary,status:status||'none',room,request,public:history,after:JSON.parse(JSON.stringify(after)),afterPublic:events,values,
      foe:{stats:target.baseStoredStats,maxHp:target.maxhp,types:target.types,stages},own:{stats:user.baseStoredStats,maxHp:user.maxhp}});
    b.destroy();
  }
  // BeforeMove nonexecution does not consume the side's Z resource or base PP.
  for(const owner of [0,1]){
    const b=new Battle({format:{id:'nativezpar',mod:'cobblemon',gen:9,gameType:'singles',ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
    const set=(species,ability,moves,item='')=>({species,ability,moves,item,level:100,movesInfo:moves.map(()=>({pp:32,maxPp:32}))});
    const side='p'+(owner+1),other='p'+(2-owner);
    b.setPlayer(side,{team:[set('Marshadow','technician',['spectralthief'],'marshadiumz')]});
    b.setPlayer(other,{team:[set('Mewtwo','pressure',['bulkup'])]});
    const user=b[side].active[0];user.trySetStatus('par',user);b.randomChance=()=>true;
    const pp=user.moveSlots[0].pp;b.makeChoices(...(owner===0?['move 1 zmove','move 1']:['move 1','move 1 zmove']));
    if(b[side].zMoveUsed||user.moveSlots[0].pp!==pp||!b.getRequests('move')[owner].active[0].canZMove)throw Error('BeforeMove incorrectly spent Z');
    b.destroy();
  }
  return rows;
};
if(typeof process!=='undefined'&&process.argv&&/own_z_move_oracle\.cjs$/.test(process.argv[1]||'')){
  const path=require('path'),root=path.resolve(process.argv[2]);
  global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(require('fs').readFileSync(path.join(root,'index.js'),'utf8'));
  const rows=module.exports(require(path.join(root,'sim/battle')).Battle);
  if(process.argv[3])require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));
  console.log('Own Marshadow Z native oracle PASS: '+rows.length+' choices, 1536 critical/roll outcomes; both owner sides, Protect/immunity, Magic Room, burn, no boost theft, PP/use and paralysis nonexecution');
}
