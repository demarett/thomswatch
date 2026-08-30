package fr.overwatchtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.domain.PlayerSnapshot;
import fr.overwatchtracker.domain.PlayerSnapshotRepository;
import fr.overwatchtracker.dto.PlayerDtos.HistoryPointDto;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RankDto;
import fr.overwatchtracker.dto.PlayerDtos.RecentProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.HeroReferenceDto;
import fr.overwatchtracker.dto.PlayerDtos.RankReferenceDto;
import fr.overwatchtracker.dto.PlayerDtos.StatReferenceDto;
import fr.overwatchtracker.integration.OverfastException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnapshotService {
  private static final ZoneId SNAPSHOT_ZONE = ZoneId.of("Europe/Paris");
  private static final int MINIMUM_REFERENCE_SAMPLE = 20;
  private static final List<String> DIVISIONS = List.of(
      "bronze", "silver", "gold", "platinum", "diamond", "master", "grandmaster", "champion");
  private final PlayerSnapshotRepository repository;
  private final SnapshotPayloadCodec codec;
  private final ObjectMapper objectMapper;

  public SnapshotService(PlayerSnapshotRepository repository, SnapshotPayloadCodec codec, ObjectMapper objectMapper) {
    this.repository = repository;
    this.codec = codec;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void save(PlayerProfileDto profile) {
    var latest = new PlayerSnapshot(
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
        codec.currentVersion());
    var date = profile.capturedAt().atZone(SNAPSHOT_ZONE).toLocalDate();
    repository.findByBattleTagAndSnapshotDate(profile.battleTag(), date)
        .ifPresentOrElse(existing -> existing.replaceWith(latest), () -> repository.save(latest));
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
    return profiles(repository.findRecentDistinct());
  }

  @Transactional(readOnly = true)
  public List<RecentProfileDto> saved() {
    return profiles(repository.findAllDistinct());
  }

  @Transactional(readOnly = true)
  public PlayerProfileDto stored(BattleTag battleTag) {
    var snapshot = repository.findFirstByBattleTagOrderByCapturedAtDesc(battleTag.value())
        .orElseThrow(() -> new OverfastException(
            "PLAYER_NOT_FOUND", "Ce profil n'est pas encore enregistré."));
    return codec.decode(snapshot.getPayloadVersion(), snapshot.getPayload());
  }

  @Transactional(readOnly = true)
  public RankReferenceDto references(BattleTag battleTag, String role) {
    var target = repository.findFirstByBattleTagOrderByCapturedAtDesc(battleTag.value())
        .orElseThrow(() -> new OverfastException(
            "PLAYER_NOT_FOUND", "Ce profil n'est pas encore enregistré."));
    var rank = rankFor(target, role);
    if (rank == null) {
      rank = repository.findLatestKnownRank(battleTag.value(), role).orElse(null);
    }
    if (rank == null) {
      return new RankReferenceDto(role, null, MINIMUM_REFERENCE_SAMPLE, 0, Map.of());
    }
    var divisionIndex = Math.max(0, (rank - 1) / 5);
    var peers = repository.findLatestPeers(
        battleTag.value(), role, divisionIndex * 5 + 1, divisionIndex * 5 + 5);
    var totals = new HashMap<String, Map<String, StatAccumulator>>();
    for (var peer : peers) {
      var profile = codec.decode(peer.getPayloadVersion(), peer.getPayload());
      for (var hero : profile.heroes()) {
        var heroStats = totals.computeIfAbsent(hero.key(), ignored -> new HashMap<>());
        var root = objectMapper.valueToTree(hero.stats());
        root.elements().forEachRemaining(category -> category.path("stats").forEach(stat -> {
          var key = stat.path("key").asText();
          var value = stat.path("value");
          if (key.endsWith("_avg_per_10_min") && value.isNumber()) {
            heroStats.computeIfAbsent(key, ignored -> new StatAccumulator()).add(value.asDouble());
          }
        }));
      }
    }
    var heroes = new LinkedHashMap<String, HeroReferenceDto>();
    totals.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(heroEntry -> {
      var stats = new LinkedHashMap<String, StatReferenceDto>();
      heroEntry.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(statEntry -> {
        var accumulator = statEntry.getValue();
        stats.put(statEntry.getKey(), new StatReferenceDto(accumulator.average(), accumulator.count));
      });
      heroes.put(heroEntry.getKey(), new HeroReferenceDto(heroEntry.getKey(), stats));
    });
    var division = divisionIndex < DIVISIONS.size() ? DIVISIONS.get(divisionIndex) : "inconnu";
    return new RankReferenceDto(role, division, MINIMUM_REFERENCE_SAMPLE, peers.size(), heroes);
  }

  private Integer rankFor(PlayerSnapshot snapshot, String role) {
    return switch (role) {
      case "tank" -> snapshot.getTankRank();
      case "damage" -> snapshot.getDamageRank();
      case "support" -> snapshot.getSupportRank();
      default -> null;
    };
  }

  private static final class StatAccumulator {
    private double total;
    private int count;
    void add(double value) { total += value; count++; }
    double average() { return count == 0 ? 0 : total / count; }
  }

  private Integer score(PlayerProfileDto profile, String role) {
    return profile.ranks().stream()
        .filter(rank -> rank.role().equals(role))
        .map(RankDto::score)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private List<RecentProfileDto> profiles(List<PlayerSnapshot> snapshots) {
    return snapshots.stream().map(snapshot -> {
      var profile = codec.decode(snapshot.getPayloadVersion(), snapshot.getPayload());
      return new RecentProfileDto(snapshot.getBattleTag(), snapshot.getUsername(), profile.avatar(),
          snapshot.getPlatform(), snapshot.getCapturedAt());
    }).toList();
  }
}
