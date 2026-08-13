package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PlayerMapperTest {
  private final ObjectMapper json=new ObjectMapper();
  private final PlayerMapper mapper=new PlayerMapper();

  @Test void mapsOnlyActualHeroesFromComparisons() throws Exception {
    var root=json.readTree("""
      {"summary":{"username":"Test","competitive":{"pc":{}}},"stats":{"pc":{"competitive":{
        "heroes_comparisons":{"time_played":{"values":[{"hero":"ana","value":900}]},"games_won":{"values":[{"hero":"ana","value":2}]},"win_percentage":{"values":[{"hero":"ana","value":67}]}},
        "career_stats":{"ana":[{"category":"game","stats":[{"key":"games_played","value":3}]}]}
      }}}}
      """);
    var profile=mapper.map("Test#1234",root,false);
    assertEquals(1,profile.heroes().size());
    assertEquals("ana",profile.heroes().getFirst().key());
    assertEquals(3,profile.heroes().getFirst().gamesPlayed());
    assertEquals(67,profile.heroes().getFirst().winRate());
    assertTrue(profile.heroes().stream().noneMatch(h->h.key().equals("pc")||h.key().equals("career_stats")));
  }
}
