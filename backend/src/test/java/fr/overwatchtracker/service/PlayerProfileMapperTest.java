package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PlayerProfileMapperTest {
  private final ObjectMapper json=new ObjectMapper();
  private final PlayerProfileMapper mapper=new PlayerProfileMapper();

  @Test void mapsOnlyActualHeroesFromComparisons() throws Exception {
    var root=json.readTree("""
      {"summary":{"username":"Test","competitive":{"pc":{}}},"stats":{"pc":{"competitive":{
        "heroes_comparisons":{"time_played":{"values":[{"hero":"ana","value":900}]},"games_won":{"values":[{"hero":"ana","value":2}]},"win_percentage":{"values":[{"hero":"ana","value":67}]}},
        "career_stats":{"ana":[
          {"category":"game","label":"Game","stats":[{"key":"games_played","label":"Games Played","value":3}]},
          {"category":"combat","label":"Combat","stats":[{"key":"eliminations","label":"Eliminations","value":42}]}
        ]}
      }}}}
      """);
    var profile=mapper.map("Test#1234",root,false);
    assertEquals(1,profile.heroes().size());
    assertEquals("ana",profile.heroes().getFirst().key());
    assertEquals(3,profile.heroes().getFirst().gamesPlayed());
    assertEquals(67,profile.heroes().getFirst().winRate());
    var combat=(java.util.Map<?,?>)profile.heroes().getFirst().stats().get("combat");
    var combatStats=(java.util.List<?>)combat.get("stats");
    assertEquals(1,combatStats.size());
    assertEquals(42L,((java.util.Map<?,?>)combatStats.getFirst()).get("value"));
    assertTrue(profile.heroes().stream().noneMatch(h->h.key().equals("pc")||h.key().equals("career_stats")));
  }

  @Test void usesOnlyThePreferredPcCompetitiveTotals() throws Exception {
    var root=json.readTree("""
      {"summary":{"username":"Test","competitive":{"console":{},"pc":{}}},"stats":{
        "console":{"competitive":{"career_stats":{"all-heroes":[{"category":"game","stats":[
          {"key":"time_played","value":99999},{"key":"games_played","value":99},{"key":"games_won","value":90},{"key":"games_lost","value":9}
        ]}]}}},
        "pc":{"competitive":{"career_stats":{"all-heroes":[{"category":"game","stats":[
          {"key":"time_played","value":3600},{"key":"games_played","value":10},{"key":"games_won","value":6},{"key":"games_lost","value":4}
        ]}]},"heroes_comparisons":{}}}
      }}
      """);

    var profile=mapper.map("Test#1234",root,false);

    assertEquals("pc",profile.platform());
    assertEquals(3600,profile.timePlayed());
    assertEquals(10,profile.gamesPlayed());
    assertEquals(6,profile.gamesWon());
    assertEquals(4,profile.gamesLost());
    assertEquals(60.0,profile.winRate());
  }
}
