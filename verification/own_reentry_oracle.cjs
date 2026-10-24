'use strict';
// Declared owned roster fixtures. Only this owner's requests and spectator
// events cross into Java; these battles are not installed Cobbleverse acceptance.
module.exports=function(Battle,small=false){
  const rows=[],uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){out.push(log[i+2]);i+=2;}
    else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  const roster=[['Genesect','download','choicescarf',['uturn','ironhead','icebeam','flamethrower']],
    ['Calyrex-Shadow','asonespectrier','lifeorb',['astralbarrage','psyshock','drainingkiss','energyball']],
    ['Xerneas','fairyaura','powerherb',['geomancy','moonblast','focusblast','thunder']],
    ['Arceus','multitype','leftovers',['swordsdance','extremespeed','shadowclaw','recover']],
    ['Mewtwo','pressure','mewtwonitey',['nastyplot','psystrike','icebeam','fireblast']],
    ['Marshadow','technician','marshadiumz',['spectralthief','closecombat','shadowsneak','rocktomb']]];
  function fixture(owner,level,first,immune=false,bench=roster[4]){
    const b=new Battle({format:{id:'nativeownreturn',mod:'cobblemon',gen:9,gameType:'singles',
      ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
    const side='p'+(owner+1),other='p'+(2-owner);
    const set=([species,ability,item,moves],n)=>({name:'Declared',uuid:uuid(n),species,ability,item,moves,level,
      movesInfo:moves.map(()=>({pp:40,maxPp:40})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
    b.setPlayer(side,{team:[set(first,1),set(bench,2)]});
    // Pressure Blissey is a declared synthetic immunity control, never an obtainable opposing build claim.
    b.setPlayer(other,{team:[set([immune?'Blissey':'Mewtwo','pressure','sitrusberry',['bulkup','protect']],3)]});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const own=b[side],user=own.active[0],target=b[other].active[0];
    target.hp=Math.floor(target.maxhp/2);target.eatItem();
    const request=()=>JSON.parse(JSON.stringify(b.getRequests(b.requestState)[owner]));
    const requests=[request()];
    const choose=(move,capture=true)=>{
      const reply=b[other].activeRequest?.wait?'':'move bulkup';
      b.makeChoices(...(owner===0?[move,reply]:[reply,move]));if(capture)requests.push(request());
    };
    const record=config=>{
      const current=own.active[0],req=request();
      // The pack's separate Active form is outside ordinary same-form / Mega-Y
      // evidence. Preserve this actual native negative rather than invent its types.
      const unprovedForm=b.log.some(line=>line==='|-formechange|'+side+'a: '+current.uuid+'|xerneasactive|');
      rows.push({config,side,turn:b.turn,uuid:current.uuid,requests,request:req,public:publicLog(b.log),
        stats:current.baseStoredStats,types:current.types,status:current.status||'none',hp:current.hp,maxHp:current.maxhp,
        stages:current.boosts,species:current.species.name,ability:current.ability,item:current.item||'none',spent:!!own.zMoveUsed,forced:b.requestState!=='move',unprovedForm});
    };
    return {b,own,user,target,choose,record};
  }
  for(const owner of [0,1])for(const mon of roster)for(const level of [50,100])
    for(const status of (level===50?['']:['','par','brn','psn','tox','slp','frz']))for(const hazards of [false,true]){
      const f=fixture(owner,level,mon),{b,own,user,target,choose,record}=f;
      // Actual native callbacks establish the status and nonzero old stages.
      if(status)user.trySetStatus(status,user);
      b.boost({atk:2,def:-1,spa:3,spd:-2,spe:2,accuracy:1,evasion:-1},user,user);
      if(mon[0]==='Mewtwo')choose('move nastyplot mega');
      if(hazards)for(const id of ['stealthrock','spikes','stickyweb','toxicspikes'])own.addSideCondition(id,target);
      choose('switch 2');choose('switch 2');
      if(own.active[0]!==user||user.fainted)throw Error('Native surviving roster return failed');
      if(user.boosts.atk>1||user.boosts.def!==0||user.boosts.spa>1||user.boosts.spd!==0||user.boosts.accuracy!==0||user.boosts.evasion!==0)
        throw Error('Native return retained old boosts');
      record({kind:'roster',owner,level,status,hazards,mon:mon[0]});b.destroy();
      if(small)return rows;
    }
  for(const owner of [0,1])for(const boundary of ['hit','protect','immune'])for(const mixed of [false,true])
    for(const active of ['marshadow','mewtwo']){
      const {b,own,user,target,choose,record}=fixture(owner,100,roster[5],boundary==='immune');
      if(boundary==='protect')target.addVolatile('protect');
      choose('move 1 zmove');
      if(!own.zMoveUsed)throw Error('Native Z resource not spent');
      if(mixed)for(const id of ['stealthrock','spikes','stickyweb','toxicspikes'])own.addSideCondition(id,target);
      choose('switch 2');
      if(mixed)choose('move nastyplot mega');
      choose('switch 2');
      if(active==='mewtwo')choose('switch 2');
      if(!own.zMoveUsed||b.getRequests('move')[owner].active[0].canZMove)throw Error('Native return revived spent Z');
      record({kind:'spent',owner,boundary,mixed,active});b.destroy();
    }
  for(const owner of [0,1])for(const active of ['marshadow','mewtwo']){
    const {b,own,choose,record}=fixture(owner,100,roster[4],false,roster[5]);
    choose('move nastyplot mega');choose('switch 2');choose('move 1 zmove');choose('switch 2');
    if(active==='marshadow')choose('switch 2');
    if(!own.zMoveUsed)throw Error('Mega-first native history lost spent Z');
    record({kind:'spent',owner,boundary:'hit',mixed:true,active,megaFirst:true});b.destroy();
  }
  for(const owner of [0,1])for(const hazards of [false,true]){
    const {b,own,user,target,choose,record}=fixture(owner,100,roster[0]);
    b.heal(target.maxhp,target,target);
    if(hazards)for(const id of ['stealthrock','spikes','stickyweb','toxicspikes'])own.addSideCondition(id,target);
    choose('move uturn',false);
    if(b.requestState!=='switch')throw Error('Native U-turn did not request its replacement');
    choose('switch 2');choose('switch 2');
    if(own.active[0]!==user||user.fainted)throw Error('Native pivot/return failed');
    record({kind:'pivot',owner,hazards});b.destroy();
  }
  for(const owner of [0,1])for(const mon of roster){
    const {b,own,user,target,choose,record}=fixture(owner,100,mon);
    if(mon[0]==='Mewtwo')choose('move nastyplot mega');
    choose('switch 2');
    own.addSideCondition('stealthrock',target);user.hp=1;
    choose('switch 2');
    if(!user.fainted||b.requestState!=='switch')throw Error('Native hazard faint/replacement boundary changed');
    record({kind:'lethal',owner,mon:mon[0]});b.destroy();
  }
  return rows;
};
if(typeof process!=='undefined'&&process.argv&&/own_reentry_oracle\.cjs$/.test(process.argv[1]||'')){
  const path=require('path'),root=path.resolve(process.argv[2]);global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(require('fs').readFileSync(path.join(root,'index.js'),'utf8'));
  const rows=module.exports(require(path.join(root,'sim/battle')).Battle,process.argv.includes('--small'));
  if(process.argv[3]&&!process.argv[3].startsWith('--'))require('fs').writeFileSync(process.argv[3],JSON.stringify(rows));
  console.log('Own roster re-entry native oracle PASS: '+rows.length+' native histories; both owner sides, reset stages, hazards, statuses and combined spent-Z/Mega-Y histories');
}
