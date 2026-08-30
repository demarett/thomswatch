package fr.overwatchtracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
  void returnsTheLatestSnapshotOfEachOfTheLatestOneHundredParisDays() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    var snapshots = new ArrayList<PlayerSnapshot>();
    for (int day = 0; day < 105; day++) {
      snapshots.add(snapshot("Busy#1234", base.plus(day, ChronoUnit.DAYS).plusSeconds(20 * 3_600)));
    }
    repository.saveAllAndFlush(snapshots);

    var history = repository.findLatestHistory("Busy#1234");

    assertEquals(100, history.size());
    assertEquals(base.plus(104, ChronoUnit.DAYS).plusSeconds(20 * 3_600), history.getFirst().getCapturedAt());
    assertEquals(base.plus(5, ChronoUnit.DAYS).plusSeconds(20 * 3_600), history.getLast().getCapturedAt());
  }

  @Test
  void returnsTheLatestSnapshotForEachOfTheEightMostRecentPlayers() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    var snapshots = new ArrayList<PlayerSnapshot>();
    snapshots.add(snapshot("Busy#1234", base));
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

  @Test
  void returnsOnlyTheLatestSnapshotOfOtherPlayersInTheRequestedRankDivision() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    repository.saveAllAndFlush(List.of(
        snapshot("Target#1234", base, 12),
        snapshot("Gold#1234", base, 11),
        snapshot("Silver#1234", base, 10),
        snapshot("Gold#1234", base.plus(1, ChronoUnit.DAYS), 15)));

    var peers = repository.findLatestPeers("Target#1234", "support", 11, 15);

    assertEquals(1, peers.size());
    assertEquals("Gold#1234", peers.getFirst().getBattleTag());
    assertEquals(base.plus(1, ChronoUnit.DAYS), peers.getFirst().getCapturedAt());
  }

  @Test
  void returnsTheLatestKnownRankWhenTheNewestSnapshotHasNoRank() {
    var base = Instant.parse("2026-08-13T00:00:00Z");
    repository.saveAllAndFlush(List.of(
        snapshot("Target#1234", base, 14),
        snapshot("Target#1234", base.plus(1, ChronoUnit.DAYS), null)));

    assertEquals(14, repository.findLatestKnownRank("Target#1234", "support").orElseThrow());
  }

  private PlayerSnapshot snapshot(String battleTag, Instant capturedAt) {
    return snapshot(battleTag, capturedAt, 2_700);
  }

  private PlayerSnapshot snapshot(String battleTag, Instant capturedAt, int supportRank) {
    return snapshot(battleTag, capturedAt, Integer.valueOf(supportRank));
  }

  private PlayerSnapshot snapshot(String battleTag, Instant capturedAt, Integer supportRank) {
    return new PlayerSnapshot(
        battleTag,
        capturedAt,
        battleTag.substring(0, battleTag.indexOf('#')),
        "pc",
        3_600L,
        50.0,
        2_500,
        2_600,
        supportRank,
        JsonNodeFactory.instance.objectNode().put("battleTag", battleTag),
        1);
  }
}
