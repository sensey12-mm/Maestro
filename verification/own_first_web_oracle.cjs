'use strict';
// Declared own entry controls, original registration outputs and public events.
// No opposing request or private stats leave this native harness.
module.exports=function(Battle,definitions){
  const registry=Cobblemon.registries.species,saved=[...registry.contents],rows=[];
  const uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const roster=[['Genesect','download','choicescarf',['ironhead']],
    ['Calyrex-Shadow','asonespectrier','lifeorb',['drainingkiss']],
    ['Xerneas','fairyaura','powerherb',['geomancy','moonblast']],
    ['Arceus','multitype','leftovers',['swordsdance']],
    ['Mewtwo','pressure','mewtwonitey',['nastyplot']],
    ['Marshadow','technician','marshadiumz',['spectralthief']]];
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){out.push(log[i+2]);i+=2;}
    else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  const configurations=[];
  for(const owner of [0,1])for(const level of [50,100]){
    for(let member=0;member<6;member++)for(const mode of ['clear','web','stack','boots','boots-room','expired-room','pivot','lethal'])
      configurations.push({owner,level,member,mode});
    for(const air of ['flying','levitate'])for(const mode of ['stack','boots','boots-room'])
      configurations.push({owner,level,air,mode});
  }
  try{
    receiveData(JSON.stringify(definitions),'species');afterSpeciesInit();
    for(const c of configurations){
      const {owner,level,mode}=c,side='p'+(owner+1),other='p'+(2-owner);
      const b=new Battle({format:{id:'nativefirstweb',mod:'cobblemon',gen:9,gameType:'singles',
        ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
      const mon=c.air==='flying'?['Tornadus','runaway','none',['bulkup']]:c.air==='levitate'?['Mewtwo','levitate','none',['bulkup']]:roster[c.member];
      const set=([species,ability,item,moves],n)=>({name:'Declared',uuid:uuid(n),species,ability,
        item:n===2&&['boots','boots-room','expired-room'].includes(mode)?'heavydutyboots':item,
        moves,level,movesInfo:moves.map(()=>({pp:40,maxPp:40})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
      b.setPlayer(side,{team:[set(['Genesect','download','choicescarf',['uturn','bulkup']],1),set(mon,2)]});
      b.setPlayer(other,{team:[set(['Mewtwo','pressure','sitrusberry',['bulkup']],9)]});
      if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
      const own=b[side],incoming=own.pokemon[1],target=b[other].active[0];
      target.hp=Math.floor(target.maxhp/2);target.eatItem();
      const request=()=>JSON.parse(JSON.stringify(b.getRequests(b.requestState)[owner]));
      const requests=[request()];
      const choose=(move,capture=true)=>{
        const reply=b[other].activeRequest?.wait?'':'move bulkup';
        try{b.makeChoices(...(owner===0?[move,reply]:[reply,move]));}catch(e){throw Error(JSON.stringify(c)+': '+e.message);}
        if(capture)requests.push(request());
      };
      if(['boots-room','expired-room'].includes(mode))b.field.addPseudoWeather('magicroom',target);
      if(mode==='expired-room')for(let i=0;i<4;i++)choose('move bulkup');
      if(mode!=='clear'){
        for(const id of (mode==='web'?['stickyweb']:['stealthrock','spikes','toxicspikes','stickyweb']))own.addSideCondition(id,target);
      }
      if(mode==='lethal')incoming.hp=1;
      if(mode==='pivot'){
        choose('move uturn',false);
        if(b.requestState!=='switch')throw Error('Native first-entry pivot request missing');
      }
      choose('switch 2');
      const forced=b.requestState!=='move',events=publicLog(b.log),actor=side+'a: '+incoming.uuid;
      const caught=events.includes('|-activate|'+actor+'|move: Sticky Web\n');
      const shouldCatch=mode!=='clear'&&mode!=='boots'&&mode!=='lethal'&&!c.air;
      if(!forced&&(caught!==shouldCatch||incoming.boosts.spe!==(caught?-1:0)))throw Error('Native Web activation/grounding changed '+JSON.stringify(c));
      if(mode==='lethal'&&(!incoming.fainted||!forced))throw Error('Native lethal entry did not close the move request');
      if(mode==='expired-room'&&b.field.pseudoWeather.magicroom)throw Error('Native Magic Room did not expire after the entry');
      rows.push({config:c,side,turn:b.turn,uuid:incoming.uuid,requests,public:events,
        stats:incoming.baseStoredStats,types:incoming.types,status:incoming.status||'none',hp:incoming.hp,maxHp:incoming.maxhp,
        stages:incoming.boosts,species:incoming.species.name,ability:incoming.ability,item:incoming.item||'none',
        speed:incoming.getStat('spe'),caught,forced,magicRoom:!!b.field.pseudoWeather.magicroom});
      b.destroy();
    }
    return rows;
  }finally{registry.contents.clear();for(const [k,v] of saved)registry.contents.set(k,v);registry.invalidate();afterSpeciesInit();}
};
if(typeof process!=='undefined'&&process.argv&&/own_first_web_oracle\.cjs$/.test(process.argv[1]||'')){
  const fs=require('fs'),path=require('path'),root=path.resolve(process.argv[2]);global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(fs.readFileSync(path.join(root,'index.js'),'utf8'));
  const definitions=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
  const rows=module.exports(require(path.join(root,'sim/battle')).Battle,definitions[process.argv[4]||'pack']);
  if(process.argv[5])fs.writeFileSync(process.argv[5],JSON.stringify(rows));
  console.log('Own first Web native oracle PASS: '+rows.length+' entries; both owner sides, roster, grounding, Boots/Room, expiration, stacked hazards, pivots and lethal entries');
}
