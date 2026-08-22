package fr.overwatchtracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import org.springframework.stereotype.Component;

@Component
public class SnapshotPayloadCodec {
  private static final int CURRENT_VERSION = 1;

  private final ObjectMapper objectMapper;

  public SnapshotPayloadCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public int currentVersion() {
    return CURRENT_VERSION;
  }

  public JsonNode encode(PlayerProfileDto profile) {
    try {
      return objectMapper.valueToTree(profile);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Sérialisation du snapshot impossible", exception);
    }
  }

  public PlayerProfileDto decode(int version, JsonNode payload) {
    try {
      return switch (version) {
        case 1 -> objectMapper.treeToValue(payload, PlayerProfileDto.class);
        default -> throw new IllegalStateException("Version de snapshot non supportée : " + version);
      };
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Lecture du snapshot impossible", exception);
    }
  }
}
