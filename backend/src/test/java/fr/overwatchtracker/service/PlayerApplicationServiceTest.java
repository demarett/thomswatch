package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.integration.OverfastGateway;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class PlayerApplicationServiceTest {
  @Mock private OverfastGateway gateway;
  @Mock private SnapshotService snapshots;
  @Mock private CacheManager cacheManager;

  private final ObjectMapper json = new ObjectMapper();
  private PlayerApplicationService service;

  @BeforeEach
  void setUp() {
    service = new PlayerApplicationService(
        gateway, new PlayerProfileMapper(), snapshots, cacheManager);
  }

  @Test
  void savesTheProfileMappedFromTheGatewayResponse() throws Exception {
    var battleTag = BattleTag.parse("Tracer#1234");
    when(gateway.getPlayer("Tracer-1234")).thenReturn(json.readTree("""
        {
          "summary": {"username": "Tracer", "competitive": {"pc": {}}},
          "stats": {"pc": {"competitive": {}}}
        }
        """));

    var profile = service.load(battleTag, false);

    assertEquals("Tracer#1234", profile.battleTag());
    assertEquals("Tracer", profile.username());
    verify(snapshots).save(profile);
  }

  @Test
  void keepsGatewayOrchestrationOutsideTransactions() throws Exception {
    Method load = PlayerApplicationService.class.getDeclaredMethod(
        "load", BattleTag.class, boolean.class);

    assertFalse(AnnotatedElementUtils.hasAnnotation(
        PlayerApplicationService.class, Transactional.class));
    assertFalse(AnnotatedElementUtils.hasAnnotation(load, Transactional.class));
  }
}
