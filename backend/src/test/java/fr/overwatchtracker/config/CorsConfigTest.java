package fr.overwatchtracker.config;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.overwatchtracker.api.PlayerController;
import fr.overwatchtracker.service.PlayerApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=https://demarett.github.io")
class CorsConfigTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private PlayerApplicationService service;

  @Test
  void allowsTheConfiguredGithubPagesOrigin() throws Exception {
    mockMvc.perform(options("/api/players/recent")
            .header(ORIGIN, "https://demarett.github.io")
            .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "https://demarett.github.io"));
  }
}
