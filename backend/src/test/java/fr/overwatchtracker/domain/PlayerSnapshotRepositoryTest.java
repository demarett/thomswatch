package fr.overwatchtracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PlayerSnapshotRepositoryTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private PlayerSnapshotRepository repository;

  @Test
  void returnsOnlyTheLatestOneHundredSnapshotsInDescendingOrder() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    var snapshots = new ArrayList<PlayerSnapshot>();
    for (int second = 0; second < 105; second++) {
      snapshots.add(snapshot("Busy#1234", base.plusSeconds(second)));
    }
    repository.saveAllAndFlush(snapshots);

    var history = repository.findLatestHistory("Busy#1234");

    assertEquals(100, history.size());
    assertEquals(base.plusSeconds(104), history.getFirst().getCapturedAt());
    assertEquals(base.plusSeconds(5), history.getLast().getCapturedAt());
  }

  @Test
  void returnsTheLatestSnapshotForEachOfTheEightMostRecentPlayers() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    var snapshots = new ArrayList<PlayerSnapshot>();
    for (int second = 0; second < 105; second++) {
      snapshots.add(snapshot("Busy#1234", base.plusSeconds(second)));
    }
    for (int player = 0; player < 9; player++) {
      snapshots.add(snapshot("Player" + player + "#1234", base.plusSeconds(200 + player)));
    }
    repository.saveAllAndFlush(snapshots);

    var recent = repository.findRecentDistinct();

    assertEquals(8, recent.size());
    assertEquals(8, recent.stream().map(PlayerSnapshot::getBattleTag).distinct().count());
    assertEquals("Player8#1234", recent.getFirst().getBattleTag());
    assertEquals("Player1#1234", recent.getLast().getBattleTag());
  }

  private PlayerSnapshot snapshot(String battleTag, Instant capturedAt) {
    return new PlayerSnapshot(
        battleTag,
        capturedAt,
        battleTag.substring(0, battleTag.indexOf('#')),
        "pc",
        3_600L,
        50.0,
        2_500,
        2_600,
        2_700,
        JsonNodeFactory.instance.objectNode().put("battleTag", battleTag),
        1);
  }
}
