'use strict';
// Declared own roster only; original registration outputs are supplied by the
// pinned Java constructor/callback harness. No opposing request is exported.
module.exports=function(Battle,definitions,shift){
  const registry=Cobblemon.registries.species,saved=[...registry.contents],rows=[];
  const uuid=n=>'00000000-0000-0000-0000-'+String(n).padStart(12,'0');
  const publicLog=log=>{const out=[];for(let i=0;i<log.length;i++){
    if(log[i].startsWith('|split|')){out.push(log[i+2]);i+=2;}else if(!log[i].startsWith('|pp_update|'))out.push(log[i]);
  }return 'update\n'+out.join('\n');};
  const roster=[['Xerneas','fairyaura','powerherb',['geomancy','moonblast','focusblast','thunder']],
    ['Genesect','download','choicescarf',['uturn','ironhead','icebeam','flamethrower']],
    ['Calyrex-Shadow','asonespectrier','lifeorb',['astralbarrage','psyshock','drainingkiss','energyball']],
    ['Arceus','multitype','leftovers',['swordsdance','extremespeed','shadowclaw','recover']],
    ['Mewtwo','pressure','mewtwonitey',['nastyplot','psystrike','icebeam','fireblast']],
    ['Marshadow','technician','marshadiumz',['spectralthief','closecombat','shadowsneak','rocktomb']]];
  const configurations=[];
  for(const owner of [0,1])for(const level of [50,100]){
    for(let recipient=1;recipient<6;recipient++)for(const kind of ['switch','return'])for(const hazards of [false,true])
      configurations.push({owner,level,recipient,kind,hazards});
    for(const kind of ['mega','z','mega-z-mega','z-mega-z','pivot-before','pivot-after','pivot-after-mega','pivot-after-mega-return',
      'pivot-after-z','pivot-after-z-return','pivot-pending','lethal','two-turn','two-turn-return','incomplete'])
      configurations.push({owner,level,recipient:4,kind,hazards:false});
    for(const kind of ['mega','z','mega-z-mega','z-mega-z'])
      configurations.push({owner,level,recipient:4,kind,hazards:true,webOnly:true});
  }
  try{
    receiveData(JSON.stringify(definitions),'species');afterSpeciesInit();
    for(const c of configurations){
      const {owner,level,kind}=c,side='p'+(owner+1),other='p'+(2-owner);
      const b=new Battle({format:{id:'nativegeomancycontinuity',mod:'cobblemon',gen:9,gameType:'singles',
        ruleset:['Obtainable','+Past','+Unobtainable'],effectType:'Format'},seed:[1,2,3,4]});
      const set=([species,ability,item,moves],n)=>({name:'Declared',uuid:uuid(n),species,ability,item,moves,level,
        movesInfo:moves.map(()=>({pp:40,maxPp:40})),evs:{hp:252,def:252,spd:4},nature:'Bold'});
      const team=roster.map((r,i)=>set(r,i+1));if(kind==='pivot-before')[team[0],team[1]]=[team[1],team[0]];
      b.setPlayer(side,{team});
      const z=kind.includes('z')||kind==='z';
      // A declared Normal immunity control keeps the Z-history case alive;
      // no claim about an obtainable opposing Pressure Blissey build is made.
      b.setPlayer(other,{team:[set([z?'Blissey':'Mewtwo','pressure','sitrusberry',['bulkup']],9)]});
      if(b.requestState==='teampreview')b.makeChoices(owner===0?'team 123456':'team 1',owner===1?'team 123456':'team 1');
      const own=b[side],members=Object.fromEntries(own.pokemon.map(p=>[Number(p.uuid.slice(-12))-1,p])),target=b[other].active[0];
      target.hp=Math.floor(target.maxhp/2);target.eatItem();
      const request=()=>JSON.parse(JSON.stringify(b.getRequests(b.requestState)[owner]));
      const requests=[request()];
      const choose=(move,capture=true)=>{const reply=b[other].activeRequest?.wait?'':'move bulkup';
        try{b.makeChoices(...(owner===0?[move,reply]:[reply,move]));}catch(e){throw Error('Native continuity choice '+JSON.stringify(c)+' '+move+': '+e.message);}
        if(capture)requests.push(request());};
      const switchTo=i=>choose('switch '+(own.pokemon.indexOf(members[i])+1));
      if(kind==='pivot-before'){
        choose('move uturn',false);if(b.requestState!=='switch')throw Error('Native initial pivot did not request entry');switchTo(0);
      }
      if(['two-turn','two-turn-return','incomplete'].includes(kind))b.field.addPseudoWeather('magicroom',target);
      choose('move geomancy');
      if(['two-turn','two-turn-return'].includes(kind))choose('move geomancy');
      if(kind!=='incomplete'){
        if(members[0].boosts.spa!==2||members[0].boosts.spd!==2||members[0].boosts.spe!==2)throw Error('Native charge did not complete');
        if(c.hazards)for(const id of (c.webOnly?['stickyweb']:['stealthrock','spikes','stickyweb','toxicspikes']))own.addSideCondition(id,target);
        if(kind==='lethal'){own.addSideCondition('stealthrock',target);members[4].hp=1;}
        if(kind==='return'||kind==='two-turn-return'){
          switchTo(c.recipient);switchTo(0);switchTo(c.recipient);
        }else if(kind==='mega'){switchTo(4);choose('move nastyplot mega');}
        else if(kind==='z'){switchTo(5);choose('move 1 zmove');}
        else if(kind==='mega-z-mega'){
          switchTo(4);choose('move nastyplot mega');switchTo(5);choose('move 1 zmove');switchTo(4);
        }else if(kind==='z-mega-z'){
          switchTo(5);choose('move 1 zmove');switchTo(4);choose('move nastyplot mega');switchTo(5);
        }else if(kind.startsWith('pivot-after')||kind==='pivot-pending'){
          switchTo(1);choose('move uturn',kind==='pivot-pending');
          if(b.requestState!=='switch')throw Error('Native later pivot did not request entry');
          if(kind!=='pivot-pending'){
            const zTarget=kind.includes('-z');switchTo(zTarget?5:4);
            if(kind.includes('-mega'))choose('move nastyplot mega');
            if(zTarget)choose('move 1 zmove');
            if(kind.endsWith('-return')){switchTo(0);switchTo(zTarget?5:4);}
          }
        }else switchTo(c.recipient);
      }
      const current=own.active[0],forced=b.requestState!=='move';
      const firstWebEntry=kind==='switch'&&c.hazards;
      const unsupported=forced||kind==='incomplete';
      if(kind==='lethal'&&(!current.fainted||!forced))throw Error('Native lethal entry boundary changed');
      if(!unsupported&&current===members[0])throw Error('Continuity checkpoint did not leave Xerneas');
      if(z&&(!own.zMoveUsed||own.activeRequest.active[0].canZMove))throw Error('Native Z history retained eligibility');
      const id=current.species.id==='genesect'?'ironhead':current.species.id==='calyrexshadow'?'drainingkiss':
        current.species.id==='arceus'?'shadowclaw':current.species.id.startsWith('mewtwo')?'psystrike':'spectralthief';
      const points=[];
      if(!unsupported){const random=b.random;for(const crit of [false,true])for(let roll=0;roll<16;roll++){
        b.random=()=>roll;const move=b.dex.getActiveMove(id);move.willCrit=crit;points.push(b.actions.getDamage(current,target,move,true)||0);
      }b.random=random;}
      rows.push({config:c,side,turn:b.turn,uuid:current.uuid,requests,public:publicLog(b.log),stats:current.baseStoredStats,
        types:current.types,status:current.status||'none',hp:current.hp,maxHp:current.maxhp,stages:current.boosts,
        species:current.species.name,ability:current.ability,item:current.item||'none',spent:!!own.zMoveUsed,forced,unsupported,firstWebEntry,
        move:id,points:points.length?[Math.min(...points),Math.max(...points)]:[]});
      b.destroy();
    }
    return rows;
  }finally{registry.contents.clear();for(const [k,v] of saved)registry.contents.set(k,v);registry.invalidate();afterSpeciesInit();}
};
if(typeof process!=='undefined'&&process.argv&&/geomancy_continuity_oracle\.cjs$/.test(process.argv[1]||'')){
  const fs=require('fs'),path=require('path'),root=path.resolve(process.argv[2]);global.require=require('module').createRequire(path.join(root,'index.js'));
  require('vm').runInThisContext(fs.readFileSync(path.join(root,'index.js'),'utf8'));
  const definitions=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
  const rows=module.exports(require(path.join(root,'sim/battle')).Battle,definitions[process.argv[4]||'pack'],!!global.require('./data/abilities').Abilities.fairyaura.onPreStart);
  if(process.argv[5])fs.writeFileSync(process.argv[5],JSON.stringify(rows));
  console.log('Completed Geomancy continuity native oracle PASS: '+rows.length+' histories; both owner sides, five roster recipients, hazards, pivots, Mega-Y, spent Z and incomplete/lethal boundaries');
}
