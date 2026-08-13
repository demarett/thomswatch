package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.dto.PlayerDtos.HeroDto;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RankDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotPayloadCodecTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final SnapshotPayloadCodec codec = new SnapshotPayloadCodec(json);

  @Test
  void roundTripsTheCurrentProfilePayloadVersion() {
    var profile = profile();

    assertEquals(1, codec.currentVersion());
    assertEquals(profile, codec.decode(codec.currentVersion(), codec.encode(profile)));
  }

  @Test
  void rejectsAnUnsupportedPayloadVersionBeforeReadingIt() {
    assertThrows(IllegalStateException.class, () -> codec.decode(2, json.createObjectNode()));
  }

  private PlayerProfileDto profile() {
    return new PlayerProfileDto(
        "Tracer#1234",
        "Tracer",
        "https://example.test/avatar.png",
        "https://example.test/namecard.png",
        "pc",
        Instant.parse("2026-08-13T10:15:30Z"),
        7_200L,
        40L,
        25L,
        15L,
        62.5,
        List.of(new RankDto("damage", "diamond", 2, 3_200)),
        List.of(new HeroDto("tracer", "Tracer", 3_600L, 20L, 13L, 65.0, Map.of("eliminations", 300))),
        Map.of("games", 40, "competitive", true),
        false);
  }
}
