import { Hero, HeroStat, RankReference } from './models';

export type CoachingStatus = 'good'|'watch'|'improve';
export interface CoachingComparison { key:string; label:string; value:number; reference:string; status:CoachingStatus; advice:string; source:'database'|'target' }
export interface HeroCoaching { confidence:'faible'|'moyenne'|'élevée'; comparisons:CoachingComparison[]; priorities:CoachingComparison[] }
export interface HeroRanking { hero:Hero; score:number; winRateScore:number; coachingScore:number; good:number; improve:number }

interface Target { key:string; label:string; min?:number; max?:number; advice:string }

const roleTargets:Record<string,Target[]> = {
  tank: [
    {key:'deaths_avg_per_10_min',label:'Morts / 10 min',max:6.5,advice:'Travaille le positionnement et garde une option de repli avant d’engager.'},
    {key:'eliminations_avg_per_10_min',label:'Éliminations / 10 min',min:16,advice:'Coordonne tes engagements avec les ressources de ton équipe.'},
    {key:'hero_damage_done_avg_per_10_min',label:'Dégâts aux héros / 10 min',min:7000,advice:'Cherche davantage de pression utile sans sacrifier ta survie.'}
  ],
  damage: [
    {key:'deaths_avg_per_10_min',label:'Morts / 10 min',max:7,advice:'Réduis les duels défavorables et utilise mieux les couverts.'},
    {key:'eliminations_avg_per_10_min',label:'Éliminations / 10 min',min:18,advice:'Concentre tes dégâts sur les cibles déjà fragilisées.'},
    {key:'final_blows_avg_per_10_min',label:'Coups finaux / 10 min',min:7,advice:'Travaille la confirmation des éliminations sur les cibles faibles.'},
    {key:'hero_damage_done_avg_per_10_min',label:'Dégâts aux héros / 10 min',min:8500,advice:'Augmente ton temps de pression tout en conservant un bon positionnement.'}
  ],
  support: [
    {key:'deaths_avg_per_10_min',label:'Morts / 10 min',max:6,advice:'Priorité à la survie : couverture, distance et cooldown défensif disponible.'},
    {key:'assists_avg_per_10_min',label:'Assistances / 10 min',min:10,advice:'Synchronise davantage tes utilitaires avec les engagements alliés.'},
    {key:'healing_done_avg_per_10_min',label:'Soins / 10 min',min:7500,advice:'Anticipe les dégâts et réduis les périodes sans valeur entre deux combats.'},
    {key:'hero_damage_done_avg_per_10_min',label:'Dégâts aux héros / 10 min',min:3500,advice:'Ajoute de la pression offensive quand ton équipe est en sécurité.'}
  ]
};

const heroOverrides:Record<string,Partial<Record<string,{min?:number;max?:number}>>> = {
  ana:{healing_done_avg_per_10_min:{min:8000},hero_damage_done_avg_per_10_min:{min:3500}},
  baptiste:{healing_done_avg_per_10_min:{min:8000},hero_damage_done_avg_per_10_min:{min:5000}},
  mercy:{healing_done_avg_per_10_min:{min:9000},hero_damage_done_avg_per_10_min:{min:500}},
  moira:{healing_done_avg_per_10_min:{min:9000},hero_damage_done_avg_per_10_min:{min:5000}},
  zenyatta:{healing_done_avg_per_10_min:{min:5000},hero_damage_done_avg_per_10_min:{min:6000}},
  lucio:{healing_done_avg_per_10_min:{min:6500},hero_damage_done_avg_per_10_min:{min:4000}}
};

export function coachHero(hero:Hero,role:string,rankReference?:RankReference):HeroCoaching {
  const stats=new Map<string,HeroStat>();
  Object.values(hero.stats||{}).flatMap(category=>category.stats||[]).forEach(stat=>stats.set(stat.key,stat));
  const targets=(roleTargets[role]||[]).map(target=>({...target,...heroOverrides[hero.key]?.[target.key]}));
  const comparisons=targets.flatMap(target=>{
    const value=stats.get(target.key)?.value;
    if(typeof value!=='number')return [];
    const measured=rankReference?.heroes?.[hero.key]?.stats?.[target.key];
    const hasDatabaseReference=!!measured&&measured.sampleSize>=rankReference!.minimumSampleSize;
    const effectiveTarget=hasDatabaseReference
      ? {...target,min:target.min===undefined?undefined:measured.average,max:target.max===undefined?undefined:measured.average}
      : target;
    const status=statusFor(value,effectiveTarget);
    const rankName=rankReference?.division ? divisionName(rankReference.division) : '';
    const referenceLabel=hasDatabaseReference
      ? `${format(measured!.average)} · moyenne ${rankName} (${measured!.sampleSize} joueurs)`
      : reference(target);
    return [{key:target.key,label:target.label,value,reference:referenceLabel,status,advice:target.advice,source:hasDatabaseReference?'database' as const:'target' as const}];
  });
  const priorityOrder:Record<CoachingStatus,number>={improve:0,watch:1,good:2};
  const priorities=comparisons.filter(item=>item.status!=='good').sort((a,b)=>priorityOrder[a.status]-priorityOrder[b.status]).slice(0,3);
  return {confidence:hero.timePlayed>=7200?'élevée':hero.timePlayed>=3600?'moyenne':'faible',comparisons,priorities};
}

export function rankHeroes(heroes:Hero[],roles:Record<string,string>,references:Record<string,RankReference>={}):{top:HeroRanking[];flop:HeroRanking[]} {
  const ranked=heroes.filter(hero=>hero.gamesPlayed>=5&&hero.timePlayed>=1800).flatMap(hero=>{
    const role=roles[hero.key]||'';
    const coaching=coachHero(hero,role,references[role]);
    if(!coaching.comparisons.length)return [];
    const points:Record<CoachingStatus,number>={good:1,watch:.5,improve:0};
    const coachingScore=coaching.comparisons.reduce((sum,item)=>sum+points[item.status],0)/coaching.comparisons.length*100;
    const winRateScore=Math.max(0,Math.min(100,(hero.winRate-35)/30*100));
    const score=Math.round(coachingScore*.6+winRateScore*.4);
    return [{hero,score,winRateScore:Math.round(winRateScore),coachingScore:Math.round(coachingScore),good:coaching.comparisons.filter(item=>item.status==='good').length,improve:coaching.comparisons.filter(item=>item.status==='improve').length}];
  }).sort((a,b)=>b.score-a.score||b.hero.timePlayed-a.hero.timePlayed);
  const top=ranked.slice(0,3);
  const topKeys=new Set(top.map(item=>item.hero.key));
  const flop=ranked.slice().reverse().filter(item=>!topKeys.has(item.hero.key)).slice(0,3);
  return {top,flop};
}

function statusFor(value:number,target:Target):CoachingStatus {
  if(target.max!==undefined){if(value<=target.max)return 'good';return value<=target.max*1.15?'watch':'improve';}
  if(target.min!==undefined){if(value>=target.min)return 'good';return value>=target.min*.85?'watch':'improve';}
  return 'watch';
}
function reference(target:Target){
  if(target.min!==undefined&&target.max!==undefined)return `${format(target.min)}–${format(target.max)}`;
  if(target.min!==undefined)return `≥ ${format(target.min)}`;
  return `≤ ${format(target.max!)}`;
}
function format(value:number){return new Intl.NumberFormat('fr-FR',{maximumFractionDigits:1}).format(value);}
function divisionName(division:string){return ({bronze:'Bronze',silver:'Argent',gold:'Or',platinum:'Platine',diamond:'Diamant',master:'Maître',grandmaster:'Grand maître',champion:'Champion'} as Record<string,string>)[division]||division;}
