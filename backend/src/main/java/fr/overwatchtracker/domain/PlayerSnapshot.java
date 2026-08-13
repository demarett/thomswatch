package fr.overwatchtracker.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "player_snapshots")
public class PlayerSnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String battleTag;

  @Column(nullable = false)
  private Instant capturedAt;

  @Column(nullable = false, length = 80)
  private String username;

  private String platform;
  private Long totalTimePlayed;
  private Double winRate;
  private Integer tankRank;
  private Integer damageRank;
  private Integer supportRank;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @Column(nullable = false)
  private int payloadVersion;

  protected PlayerSnapshot() {
  }

  public PlayerSnapshot(String battleTag, Instant capturedAt, String username, String platform, Long totalTimePlayed,
      Double winRate, Integer tankRank, Integer damageRank, Integer supportRank, JsonNode payload,
      int payloadVersion) {
    this.battleTag = battleTag;
    this.capturedAt = capturedAt;
    this.username = username;
    this.platform = platform;
    this.totalTimePlayed = totalTimePlayed;
    this.winRate = winRate;
    this.tankRank = tankRank;
    this.damageRank = damageRank;
    this.supportRank = supportRank;
    this.payload = payload;
    this.payloadVersion = payloadVersion;
  }

  public Instant getCapturedAt() {
    return capturedAt;
  }

  public Long getTotalTimePlayed() {
    return totalTimePlayed;
  }

  public String getBattleTag() {
    return battleTag;
  }

  public String getUsername() {
    return username;
  }

  public String getPlatform() {
    return platform;
  }

  public JsonNode getPayload() {
    return payload;
  }

  public int getPayloadVersion() {
    return payloadVersion;
  }

  public Double getWinRate() {
    return winRate;
  }

  public Integer getTankRank() {
    return tankRank;
  }

  public Integer getDamageRank() {
    return damageRank;
  }

  public Integer getSupportRank() {
    return supportRank;
  }
}
