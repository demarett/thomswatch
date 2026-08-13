package fr.overwatchtracker.integration;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

class OverfastClientTest {
  private MockWebServer server;
  @BeforeEach void start() throws Exception {server=new MockWebServer();server.start();}
  @AfterEach void stop() throws Exception {server.shutdown();}
  private OverfastClient client(){return new OverfastClient(RestClient.builder(),server.url("/").toString());}
  @Test void returnsProfile() throws Exception {server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","application/json").setBody("{\"summary\":{\"username\":\"Ana\"}}"));JsonNode node=client().getPlayer("Ana-1234");assertEquals("Ana",node.path("summary").path("username").asText());assertTrue(server.takeRequest().getPath().startsWith("/players/Ana-1234"));}
  @Test void mapsNotFound() {server.enqueue(new MockResponse().setResponseCode(404));OverfastException ex=assertThrows(OverfastException.class,()->client().getPlayer("Nobody-1234"));assertEquals("PLAYER_NOT_FOUND",ex.code());}
  @Test void mapsRateLimit() {server.enqueue(new MockResponse().setResponseCode(429));OverfastException ex=assertThrows(OverfastException.class,()->client().getPlayer("Ana-1234"));assertEquals("RATE_LIMIT",ex.code());}
}
