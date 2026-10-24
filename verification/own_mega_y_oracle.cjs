'use strict';

// Native synthetic own-Mewtwo histories. Only spectator events and the declared
// owner's request cross into Java; opposing exact state is oracle truth only.
module.exports=function(Battle,small=false){
  const rows=[];
  const uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){if(i+2>=log.length)throw Error('Incomplete native split');out.push(log[i+2]);i+=2;}
    else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  const specs=[];
  for(const side of [0,1])for(const level of [50,100])for(const shiny of [false,true])
    for(const status of ['','par','brn','psn','tox','slp','frz'])for(const reentry of [false,true])
      specs.push({side,level,shiny,status,reentry,hazard:''});
  for(const side of [0,1])for(const hazard of ['spikes','stealthrock','toxicspikes'])for(const stages of [-2,0,5])
    specs.push({side,level:100,shiny:false,status:'',reentry:true,hazard,stages});
  for(const side of [0,1])for(const status of ['par','frz'])for(const reentry of [false,true])
    for(const seed of [[9,7,5,3],[17,23,31,43]])specs.push({side,level:100,shiny:false,status,reentry,hazard:'',seed});
  for(const c of small?specs.slice(0,1):specs){
    const b=new Battle({format:{id:'nativeownmegay',mod:'cobblemon',gen:9,gameType:'singles',
      ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:c.seed||[1,2,3,4]});
    const ownSide='p'+(c.side+1),otherSide='p'+(2-c.side);
    const set=(species,n,ability,moves,item='')=>({name:'Declared',uuid:uuid(n),species,ability,moves,item,level:c.level,
      shiny:c.shiny,movesInfo:moves.map(()=>({pp:40,maxPp:40})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
    const own=[set('Mewtwo',1,'pressure',['psystrike','icebeam','fireblast','nastyplot'],'mewtwonitey'),
      set('Mew',2,'runaway',['bulkup'])];
    const foe=[set('Mewtwo',3,'pressure',['bulkup'],'sitrusberry')];
    b.setPlayer(ownSide,{team:own});b.setPlayer(otherSide,{team:foe});
    if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
    const side=b[ownSide],user=side.active[0],target=b[otherSide].active[0];
    // Actual pinned berry consumption establishes a public empty held item.
    target.hp=Math.floor(target.maxhp/2);target.eatItem();
    if(c.status)user.trySetStatus(c.status,user);
    if(c.stages)b.boost({spa:c.stages,spd:c.stages,spe:c.stages},user,user);
    const before=JSON.parse(JSON.stringify(b.getRequests('move')[c.side]));
    const choices=move=>c.side===0?b.makeChoices(move,'move bulkup'):b.makeChoices('move bulkup',move);
    choices('move nastyplot mega');
    if(user.species.id!=='mewtwomegay'||user.ability!=='insomnia'||b.turn!==2)throw Error('Native Mega-Y activation failed');
    const first=publicLog(b.log),requests=[before,JSON.parse(JSON.stringify(b.getRequests('move')[c.side]))];
    if(c.reentry){
      if(c.hazard)side.addSideCondition(c.hazard,target);
      choices('switch 2');requests.push(JSON.parse(JSON.stringify(b.getRequests('move')[c.side])));
      choices('switch 2');requests.push(JSON.parse(JSON.stringify(b.getRequests('move')[c.side])));
      if(side.active[0]!==user||b.turn!==4)throw Error('Native permanent-form re-entry failed');
    }
    const request=JSON.parse(JSON.stringify(b.getRequests('move')[c.side]));
    const history=publicLog(b.log),points={};
    for(const id of ['psystrike','icebeam','fireblast']){
      const damage=[];const randomizer=b.randomizer;
      try{for(const crit of [false,true])for(let roll=85;roll<=100;roll++){
        const move=b.dex.getActiveMove(id);move.willCrit=crit;
        b.randomizer=n=>Math.floor(n*roll/100);
        damage.push(b.actions.getDamage(user,target,move));
      }}finally{b.randomizer=randomizer;}
      points[id]=[Math.min(...damage),Math.max(...damage)];
    }
    rows.push({config:c,side:ownSide,uuid:uuid(1),turn:b.turn,before,first,request,requests,
      public:history,stages:user.boosts,status:user.status||'none',types:user.types,
      stats:user.baseStoredStats,hp:user.hp,maxHp:user.maxhp,points,
      foe:{stats:target.baseStoredStats,hp:target.hp,maxHp:target.maxhp,stages:target.boosts,status:target.status||'none',types:target.types}});
    b.destroy();
  }
  return rows;
};
if(typeof process!=='undefined'&&process.argv&&/own_mega_y_oracle\.cjs$/.test(process.argv[1]||'')){
  const path=require('path'),root=path.resolve(process.argv[2]);
  global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(require('fs').readFileSync(path.join(root,'index.js'),'utf8'));
  const result=module.exports(require(path.join(root,'sim/battle')).Battle,process.argv.includes('--small'));
  if(process.argv[3]&&!process.argv[3].startsWith('--'))require('fs').writeFileSync(process.argv[3],JSON.stringify(result));
  console.log('Own Mega-Y native oracle PASS: '+result.length+' native transition/re-entry histories');
}
