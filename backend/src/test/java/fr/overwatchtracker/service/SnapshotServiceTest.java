package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.domain.PlayerSnapshotRepository;
import fr.overwatchtracker.domain.PlayerSnapshot;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RankDto;
import fr.overwatchtracker.dto.PlayerDtos.HeroDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SnapshotServiceTest {
  @Test
  void savesAProfileWhosePublishedRankHasNoScore() {
    var repository = mock(PlayerSnapshotRepository.class);
    var codec = mock(SnapshotPayloadCodec.class);
    when(codec.encode(any())).thenReturn(new ObjectMapper().createObjectNode());
    when(codec.currentVersion()).thenReturn(1);
    var service = new SnapshotService(repository, codec, new ObjectMapper());
    var profile = new PlayerProfileDto(
        "Thoms33#2340", "Thoms33", null, null, "pc", Instant.now(),
        0L, 0L, 0L, 0L, 0.0,
        List.of(new RankDto("damage", "diamond", 3, null)),
        List.of(), Map.of(), false);

    assertDoesNotThrow(() -> service.save(profile));
  }

  @Test
  void replacesTheExistingSnapshotForTheSameParisDay() {
    var repository = mock(PlayerSnapshotRepository.class);
    var codec = mock(SnapshotPayloadCodec.class);
    when(codec.encode(any())).thenReturn(new ObjectMapper().createObjectNode());
    when(codec.currentVersion()).thenReturn(1);
    var existing = new PlayerSnapshot(
        "Thoms33#2340", Instant.parse("2026-08-23T06:00:00Z"), "Thoms33", "pc",
        3_600L, 50.0, null, null, null, new ObjectMapper().createObjectNode(), 1);
    when(repository.findByBattleTagAndSnapshotDate("Thoms33#2340", java.time.LocalDate.of(2026, 8, 23)))
        .thenReturn(Optional.of(existing));
    var service = new SnapshotService(repository, codec, new ObjectMapper());
    var latest = new PlayerProfileDto(
        "Thoms33#2340", "Thoms33", null, null, "pc", Instant.parse("2026-08-23T20:00:00Z"),
        7_200L, 10L, 6L, 4L, 60.0, List.of(), List.of(), Map.of(), false);

    service.save(latest);

    assertEquals(Instant.parse("2026-08-23T20:00:00Z"), existing.getCapturedAt());
    assertEquals(7_200L, existing.getTotalTimePlayed());
    assertEquals(60.0, existing.getWinRate());
    verify(repository, never()).save(any());
  }

  @Test
  void averagesPerTenMinuteStatsFromTheLatestOtherPlayersInTheSameDivision() {
    var repository = mock(PlayerSnapshotRepository.class);
    var codec = mock(SnapshotPayloadCodec.class);
    var mapper = new ObjectMapper();
    var target = snapshot("Target#1234", 12, mapper);
    var peerOne = snapshot("Peer1#1234", 11, mapper);
    var peerTwo = snapshot("Peer2#1234", 15, mapper);
    when(repository.findFirstByBattleTagOrderByCapturedAtDesc("Target#1234")).thenReturn(Optional.of(target));
    when(repository.findLatestPeers("Target#1234", "support", 11, 15)).thenReturn(List.of(peerOne, peerTwo));
    when(codec.decode(anyInt(), any())).thenReturn(profileWithHealing("Peer1#1234", 8_000), profileWithHealing("Peer2#1234", 10_000));
    var service = new SnapshotService(repository, codec, mapper);

    var result = service.references(BattleTag.parse("Target#1234"), "support");

    var healing = result.heroes().get("ana").stats().get("healing_done_avg_per_10_min");
    assertEquals("gold", result.division());
    assertEquals(2, result.playerCount());
    assertEquals(9_000.0, healing.average());
    assertEquals(2, healing.sampleSize());
  }

  private PlayerSnapshot snapshot(String battleTag, int supportRank, ObjectMapper mapper) {
    return new PlayerSnapshot(battleTag, Instant.parse("2026-08-23T10:00:00Z"), battleTag, "pc",
        3_600L, 50.0, null, null, supportRank, mapper.createObjectNode(), 1);
  }

  private PlayerProfileDto profileWithHealing(String battleTag, double healing) {
    var stats = Map.<String,Object>of("average", Map.of("stats", List.of(
        Map.of("key", "healing_done_avg_per_10_min", "label", "Healing", "value", healing),
        Map.of("key", "games_played", "label", "Games", "value", 20))));
    var hero = new HeroDto("ana", "Ana", 3_600L, 20L, 10L, 50.0, stats);
    return new PlayerProfileDto(battleTag, battleTag, null, null, "pc", Instant.now(),
        3_600L, 20L, 10L, 10L, 50.0, List.of(), List.of(hero), Map.of(), false);
  }
}
