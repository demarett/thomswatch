package fr.overwatchtracker.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.overwatchtracker.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
class PlayerControllerValidationTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private PlayerService service;

  @Test
  void rejectsMissingBattleTagWithTheUniformError() throws Exception {
    mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }

  @Test
  void rejectsNullBattleTagWithTheUniformError() throws Exception {
    mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }

  @Test
  void rejectsBlankBattleTagWithTheUniformError() throws Exception {
    mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }

  @Test
  void rejectsMalformedBodyBattleTagWithTheUniformError() throws Exception {
    mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":\"bad\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }

  @Test
  void rejectsMalformedJsonWithTheUniformError() throws Exception {
    mockMvc.perform(post("/api/players/lookup").contentType(APPLICATION_JSON).content("{\"battleTag\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }

  @Test
  void rejectsMalformedPathBattleTagWithTheUniformError() throws Exception {
    mockMvc.perform(get("/api/players/bad/history"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BATTLE_TAG"));
  }
}
