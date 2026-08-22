package fr.overwatchtracker.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PlayerDtos {
  private PlayerDtos() {}
  public static final String BATTLE_TAG_PATTERN = "^[^#\\s-]{2,32}(?:#|-)\\d{3,12}$";
  public record LookupRequest(
      @NotBlank(message="Format attendu : Pseudo#1234")
      @Pattern(regexp=BATTLE_TAG_PATTERN, message="Format attendu : Pseudo#1234") String battleTag) {}
  public record RankDto(String role, String division, Integer tier, Integer score) {}
  public record HeroDto(String key, String name, Long timePlayed, Long gamesPlayed, Long gamesWon, Double winRate, Map<String,Object> stats) {}
  public record HeroPortraitDto(String key, String name, String portrait) {}
  public record PlayerProfileDto(String battleTag, String username, String avatar, String namecard, String platform,
      Instant capturedAt, Long timePlayed, Long gamesPlayed, Long gamesWon, Long gamesLost, Double winRate,
      List<RankDto> ranks, List<HeroDto> heroes, Map<String,Object> globalStats, boolean demo) {}
  public record RecentProfileDto(String battleTag, String username, String avatar, String platform, Instant lastViewedAt) {}
  public record HistoryPointDto(Instant capturedAt, Long timePlayed, Double winRate, Integer tankRank, Integer damageRank, Integer supportRank) {}
  public record ApiError(String code, String message, Instant timestamp) {}
}
