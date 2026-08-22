package fr.overwatchtracker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.domain.PlayerSnapshotRepository;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RankDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotServiceTest {
  @Test
  void savesAProfileWhosePublishedRankHasNoScore() {
    var repository = mock(PlayerSnapshotRepository.class);
    var codec = mock(SnapshotPayloadCodec.class);
    when(codec.encode(any())).thenReturn(new ObjectMapper().createObjectNode());
    when(codec.currentVersion()).thenReturn(1);
    var service = new SnapshotService(repository, codec);
    var profile = new PlayerProfileDto(
        "Thoms33#2340", "Thoms33", null, null, "pc", Instant.now(),
        0L, 0L, 0L, 0L, 0.0,
        List.of(new RankDto("damage", "diamond", 3, null)),
        List.of(), Map.of(), false);

    assertDoesNotThrow(() -> service.save(profile));
  }
}
