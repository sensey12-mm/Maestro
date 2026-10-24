'use strict';
// Definitions are outputs of the original pinned Java registration constructor
// and addon callback, supplied by OwnXerneasRegistrationRegression. No installed
// aspect application or opposing private knowledge is claimed.
module.exports=function(Battle,definitions,shift){
  const registry=Cobblemon.registries.species,saved=[...registry.contents],rows=[];
  const uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){out.push(log[i+2]);i+=2;}
    else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  try{
    receiveData(JSON.stringify(definitions),'species');afterSpeciesInit();
    for(const owner of [0,1])for(const level of [50,100])
      for(const kind of ['initial','geomancy','magicroom','charging','return','web','boots','poison','lethal']){
        const b=new Battle({format:{id:'nativexerneasentry',mod:'cobblemon',gen:9,gameType:'singles',
          ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
        const side='p'+(owner+1),other='p'+(2-owner);
        const set=(species,ability,item,moves,n)=>({name:'Declared',uuid:uuid(n),species,ability,item,moves,level,
          movesInfo:moves.map(()=>({pp:40,maxPp:40})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
        b.setPlayer(side,{team:[set('Xerneas','fairyaura',kind==='boots'?'heavydutyboots':'powerherb',
          ['geomancy','moonblast','focusblast','thunder'],1),set('Mewtwo','pressure','', ['bulkup'],2)]});
        b.setPlayer(other,{team:[set('Mewtwo','pressure','sitrusberry',['bulkup'],3)]});
        if(b.requestState==='teampreview')b.makeChoices('team 1','team 1');
        const own=b[side],user=own.active[0],target=b[other].active[0];
        target.hp=Math.floor(target.maxhp/2);target.eatItem();
        const request=()=>JSON.parse(JSON.stringify(b.getRequests(b.requestState)[owner]));
        const requests=[request()],departures=[];
        const choose=move=>{const reply=b[other].activeRequest?.wait?'':'move bulkup';
          try{b.makeChoices(...(owner===0?[move,reply]:[reply,move]));}catch(e){throw Error(JSON.stringify({owner,level,kind,move,reply,request:own.activeRequest,tail:b.log.slice(-8)})+' '+e.message);}requests.push(request());};
        if(['magicroom','charging'].includes(kind))b.field.addPseudoWeather('magicroom',target);
        if(kind==='poison')user.trySetStatus('psn',user);
        if(['geomancy','magicroom','charging','return','web','boots','poison','lethal'].includes(kind))choose('move geomancy');
        // Magic Room preserves the herb and requires the second charge turn.
        if(['magicroom','boots'].includes(kind))choose('move geomancy');
        const boosted={...user.boosts},herbAfter=user.item;
        if(['return','web','boots','poison','lethal'].includes(kind)){
          choose('switch 2');
          departures.push({species:user.species.id,types:[...user.types],stages:{...user.boosts}});
          if(['web','boots','poison','lethal'].includes(kind))for(const id of ['stealthrock','spikes','stickyweb'])own.addSideCondition(id,target);
          if(kind==='lethal')user.hp=1;
          choose('switch 2');
        }
        const forced=b.requestState!=='move';
        if(kind==='lethal'&&(!user.fainted||!forced))throw Error('Native hazard faint boundary changed');
        if(!forced){
          if(user.types.join(',')!=='Fairy'||user.ability!=='fairyaura')throw Error('Original Active registration lost own typing/ability');
          if(shift&&user.species.id!=='xerneasactive')throw Error('Native addon PreStart did not activate registered form');
          if(kind==='geomancy'&&(boosted.spa!==2||boosted.spd!==2||boosted.spe!==2||herbAfter))throw Error('Native Power Herb Geomancy changed');
          if(kind==='magicroom'&&(boosted.spa!==2||herbAfter!=='powerherb'))throw Error('Native Magic Room charge/herb boundary changed');
          if(departures.length&&(departures[0].species!=='xerneas'||Object.values(departures[0].stages).some(Boolean)))throw Error('Nonpermanent departure did not reset native base species/stages');
          const actor=side+'a: '+user.uuid;
          if(shift){const entries=b.log.map((s,i)=>s.startsWith('|switch|'+actor+'|')?i:-1).filter(i=>i>=0);
            for(const i of entries){const form=b.log.indexOf('|-formechange|'+actor+'|Xerneas-Active|',i);
              const next=b.log.findIndex((s,j)=>j>i&&(s.startsWith('|-damage|'+actor+'|')||s.startsWith('|-ability|'+actor+'|')));
              if(form<=i||next>=0&&form>next)throw Error('Native Active event/hazard/ability ordering changed');}}
        }
        const points=[];
        if(!forced&&kind!=='charging'){
          const random=b.random;
          for(const critical of [false,true])for(let roll=0;roll<16;roll++){
            b.random=()=>roll;const move=b.dex.getActiveMove('moonblast');move.willCrit=critical;
            points.push(b.actions.getDamage(user,target,move,true)||0);
          }
          b.random=random;
        }
        rows.push({config:{owner,level,kind},side,turn:b.turn,uuid:user.uuid,requests,request:request(),public:publicLog(b.log),
          stats:user.baseStoredStats,types:user.types,status:user.status||'none',hp:user.hp,maxHp:user.maxhp,stages:user.boosts,
          ability:user.ability,item:user.item||'none',forced,shift,departures,points:points.length?[Math.min(...points),Math.max(...points)]:[]});
        b.destroy();
      }
    return rows;
  }finally{registry.contents.clear();for(const [k,v] of saved)registry.contents.set(k,v);registry.invalidate();afterSpeciesInit();}
};
if(typeof process!=='undefined'&&process.argv&&/own_xerneas_oracle\.cjs$/.test(process.argv[1]||'')){
  const fs=require('fs'),path=require('path'),root=path.resolve(process.argv[2]);
  global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(fs.readFileSync(path.join(root,'index.js'),'utf8'));
  const defs=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
  const rows=module.exports(require(path.join(root,'sim/battle')).Battle,defs[process.argv[4]||'pack'],!!global.require('./data/abilities').Abilities.fairyaura.onPreStart);
  if(process.argv[5])fs.writeFileSync(process.argv[5],JSON.stringify(rows));
  console.log('Own Xerneas native oracle PASS: '+rows.length+' original-registration histories; both owner sides, entry/departure, Power Herb, Magic Room, hazards and lethal boundaries');
}
