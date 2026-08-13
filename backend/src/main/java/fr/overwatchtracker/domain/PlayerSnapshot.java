package fr.overwatchtracker.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="player_snapshots")
public class PlayerSnapshot {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, length=80) private String battleTag;
  @Column(nullable=false) private Instant capturedAt;
  @Column(nullable=false, length=80) private String username;
  private String platform;
  private Long totalTimePlayed;
  private Double winRate;
  private Integer tankRank;
  private Integer damageRank;
  private Integer supportRank;
  @Column(nullable=false, columnDefinition="text") private String payload;
  protected PlayerSnapshot() {}
  public PlayerSnapshot(String battleTag, Instant capturedAt, String username, String platform, Long totalTimePlayed,
      Double winRate, Integer tankRank, Integer damageRank, Integer supportRank, String payload) {
    this.battleTag=battleTag; this.capturedAt=capturedAt; this.username=username; this.platform=platform;
    this.totalTimePlayed=totalTimePlayed; this.winRate=winRate; this.tankRank=tankRank; this.damageRank=damageRank;
    this.supportRank=supportRank; this.payload=payload;
  }
  public Instant getCapturedAt(){return capturedAt;} public Long getTotalTimePlayed(){return totalTimePlayed;}
  public String getBattleTag(){return battleTag;} public String getUsername(){return username;}
  public String getPlatform(){return platform;} public String getPayload(){return payload;}
  public Double getWinRate(){return winRate;} public Integer getTankRank(){return tankRank;}
  public Integer getDamageRank(){return damageRank;} public Integer getSupportRank(){return supportRank;}
}
