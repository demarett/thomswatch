import { Hero } from './models';
import { coachHero, rankHeroes } from './hero-coaching';

const ana:Hero={key:'ana',name:'Ana',timePlayed:10800,gamesPlayed:20,gamesWon:10,winRate:50,stats:{average:{label:'Average',stats:[
  {key:'deaths_avg_per_10_min',label:'Deaths',value:7.2},
  {key:'assists_avg_per_10_min',label:'Assists',value:11},
  {key:'healing_done_avg_per_10_min',label:'Healing',value:7000},
  {key:'hero_damage_done_avg_per_10_min',label:'Damage',value:3600}
]}}};

function makeHero(key:string,winRate:number,stats:Record<string,number>):Hero {
  return {key,name:key,timePlayed:10800,gamesPlayed:20,gamesWon:10,winRate,stats:{average:{label:'Average',stats:Object.entries(stats).map(([statKey,value])=>({key:statKey,label:statKey,value}))}}};
}

describe('hero coaching',()=>{
  it('compares available stats with hero-adjusted role targets',()=>{
    const result=coachHero(ana,'support');
    expect(result.confidence).toBe('élevée');
    expect(result.comparisons.find(item=>item.key==='healing_done_avg_per_10_min')?.reference).toBe('≥ 8 000');
    expect(result.comparisons.find(item=>item.key==='deaths_avg_per_10_min')?.status).toBe('improve');
    expect(result.priorities).toHaveLength(2);
  });
  it('does not invent comparisons for unavailable statistics',()=>{
    expect(coachHero({...ana,stats:{}},'support').comparisons).toEqual([]);
  });
  it('ranks eligible heroes from coaching targets and win rate without overlapping lists',()=>{
    const heroes=[
      {...ana,key:'ana',winRate:62},
      {...ana,key:'baptiste',winRate:55},
      {...ana,key:'mercy',winRate:52},
      {...ana,key:'moira',winRate:48},
      {...ana,key:'zenyatta',winRate:44},
      {...ana,key:'lucio',winRate:38},
      {...ana,key:'kiriko',gamesPlayed:2,winRate:100}
    ];
    const roles=Object.fromEntries(heroes.map(hero=>[hero.key,'support']));
    const result=rankHeroes(heroes,roles);
    expect(result.top).toHaveLength(3);
    expect(result.flop).toHaveLength(3);
    expect(result.top[0].score).toBeGreaterThanOrEqual(result.top[1].score);
    expect(result.flop[0].score).toBeLessThanOrEqual(result.flop[1].score);
    expect(result.top.map(item=>item.hero.key)).not.toContain(result.flop[0].hero.key);
    expect([...result.top,...result.flop].some(item=>item.hero.key==='kiriko')).toBe(false);
  });

  it('uses a same-rank database average when the minimum sample is reached',()=>{
    const hero=makeHero('ana',55,{
      deaths_avg_per_10_min:5,
      healing_done_avg_per_10_min:8500,
      hero_damage_done_avg_per_10_min:4000,
      assists_avg_per_10_min:12
    });
    const reference={role:'support',division:'gold',minimumSampleSize:20,playerCount:24,heroes:{ana:{heroKey:'ana',stats:{
      healing_done_avg_per_10_min:{average:8200,sampleSize:24}
    }}}};

    const result=coachHero(hero,'support',reference);

    const healing=result.comparisons.find(item=>item.key==='healing_done_avg_per_10_min');
    expect(healing?.source).toBe('database');
    expect(healing?.reference).toContain('moyenne Or (24 joueurs)');
  });

  it('keeps the pedagogical target while the database sample is too small',()=>{
    const hero=makeHero('ana',55,{healing_done_avg_per_10_min:8500});
    const reference={role:'support',division:'gold',minimumSampleSize:20,playerCount:4,heroes:{ana:{heroKey:'ana',stats:{
      healing_done_avg_per_10_min:{average:8200,sampleSize:4}
    }}}};

    const result=coachHero(hero,'support',reference);

    const healing=result.comparisons.find(item=>item.key==='healing_done_avg_per_10_min');
    expect(healing?.source).toBe('target');
    expect(healing?.reference).toBe('≥ 8 000');
  });
});
