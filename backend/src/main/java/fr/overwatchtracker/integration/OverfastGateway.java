package fr.overwatchtracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OverfastGateway {
  private static final Logger log=LoggerFactory.getLogger(OverfastGateway.class);
  private final RestClient client;
  public OverfastGateway(RestClient.Builder builder, @Value("${overfast.base-url}") String baseUrl) {
    this.client=builder.baseUrl(baseUrl).defaultHeader("User-Agent","OverwatchTracker/0.1").build();
  }
  @Cacheable(cacheNames="overfastPlayers", key="#playerId")
  public JsonNode getPlayer(String playerId) {
    log.info("Lecture du profil OverFast {}", playerId);
    try {
      return client.get().uri(uri -> uri.path("/players/{id}").queryParam("gamemode","competitive").build(playerId))
          .retrieve().onStatus(HttpStatusCode::isError, (req,res) -> {
            int status=res.getStatusCode().value();
            throw switch(status) {
              case 404 -> new OverfastException("PLAYER_NOT_FOUND","BattleTag introuvable ou profil privé.");
              case 429 -> new OverfastException("RATE_LIMIT","Limite de requêtes OverFast atteinte.");
              case 503 -> new OverfastException("UPSTREAM_RATE_LIMIT","Blizzard limite temporairement les requêtes.");
              case 504 -> new OverfastException("UPSTREAM_TIMEOUT","Blizzard ne répond pas dans le délai imparti.");
              default -> new OverfastException("OVERFAST_UNAVAILABLE","Le service OverFast est temporairement indisponible.");
            };
          }).body(JsonNode.class);
    } catch (OverfastException e) { throw e; }
    catch (RestClientException e) { throw new OverfastException("OVERFAST_UNAVAILABLE","Impossible de contacter OverFast."); }
  }
  @Cacheable(cacheNames="heroCatalog", key="'fr-fr'")
  public JsonNode getHeroes() {
    try {
      return client.get().uri(uri -> uri.path("/heroes").queryParam("locale","fr-fr").build()).retrieve()
          .onStatus(HttpStatusCode::isError,(req,res)->{throw new OverfastException("OVERFAST_UNAVAILABLE","Le catalogue des héros est temporairement indisponible.");})
          .body(JsonNode.class);
    } catch (OverfastException e) { throw e; }
    catch (RestClientException e) { throw new OverfastException("OVERFAST_UNAVAILABLE","Impossible de charger les héros."); }
  }
}
