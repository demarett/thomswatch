package fr.overwatchtracker.service;

import fr.overwatchtracker.domain.PlayerSnapshot;
import fr.overwatchtracker.domain.PlayerSnapshotRepository;
import fr.overwatchtracker.dto.PlayerDtos.HistoryPointDto;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RankDto;
import fr.overwatchtracker.dto.PlayerDtos.RecentProfileDto;
import fr.overwatchtracker.integration.OverfastException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnapshotService {
  private final PlayerSnapshotRepository repository;
  private final SnapshotPayloadCodec codec;

  public SnapshotService(PlayerSnapshotRepository repository, SnapshotPayloadCodec codec) {
    this.repository = repository;
    this.codec = codec;
  }

  @Transactional
  public void save(PlayerProfileDto profile) {
    repository.save(new PlayerSnapshot(
        profile.battleTag(),
        profile.capturedAt(),
        profile.username(),
        profile.platform(),
        profile.timePlayed(),
        profile.winRate(),
        score(profile, "tank"),
        score(profile, "damage"),
        score(profile, "support"),
        codec.encode(profile),
        codec.currentVersion()));
  }

  @Transactional(readOnly = true)
  public List<HistoryPointDto> history(BattleTag battleTag) {
    var snapshots = new ArrayList<>(repository.findLatestHistory(battleTag.value()));
    return snapshots.reversed().stream()
        .map(snapshot -> new HistoryPointDto(
            snapshot.getCapturedAt(),
            snapshot.getTotalTimePlayed(),
            snapshot.getWinRate(),
            snapshot.getTankRank(),
            snapshot.getDamageRank(),
            snapshot.getSupportRank()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RecentProfileDto> recent() {
    return repository.findRecentDistinct().stream()
        .map(snapshot -> {
          var profile = codec.decode(snapshot.getPayloadVersion(), snapshot.getPayload());
          return new RecentProfileDto(
              snapshot.getBattleTag(),
              snapshot.getUsername(),
              profile.avatar(),
              snapshot.getPlatform(),
              snapshot.getCapturedAt());
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public PlayerProfileDto stored(BattleTag battleTag) {
    var snapshot = repository.findFirstByBattleTagOrderByCapturedAtDesc(battleTag.value())
        .orElseThrow(() -> new OverfastException(
            "PLAYER_NOT_FOUND", "Ce profil n'est pas encore enregistré."));
    return codec.decode(snapshot.getPayloadVersion(), snapshot.getPayload());
  }

  private Integer score(PlayerProfileDto profile, String role) {
    return profile.ranks().stream()
        .filter(rank -> rank.role().equals(role))
        .map(RankDto::score)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
