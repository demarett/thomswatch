package fr.overwatchtracker.service;

import fr.overwatchtracker.dto.PlayerDtos.HistoryPointDto;
import fr.overwatchtracker.dto.PlayerDtos.PlayerProfileDto;
import fr.overwatchtracker.dto.PlayerDtos.RecentProfileDto;
import fr.overwatchtracker.integration.OverfastGateway;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PlayerApplicationService {
  private final OverfastGateway gateway;
  private final PlayerProfileMapper mapper;
  private final SnapshotService snapshots;
  private final CacheManager cacheManager;
  private final DemoData demo;

  public PlayerApplicationService(
      OverfastGateway gateway,
      PlayerProfileMapper mapper,
      SnapshotService snapshots,
      CacheManager cacheManager,
      DemoData demo) {
    this.gateway = gateway;
    this.mapper = mapper;
    this.snapshots = snapshots;
    this.cacheManager = cacheManager;
    this.demo = demo;
  }

  public PlayerProfileDto load(BattleTag battleTag, boolean refresh) {
    if (battleTag.value().equalsIgnoreCase("Demo#0000")) {
      var profile = demo.profile();
      snapshots.save(profile);
      return profile;
    }
    if (refresh) {
      var cache = cacheManager.getCache("overfastPlayers");
      if (cache != null) {
        cache.evict(battleTag.overfastId());
      }
    }
    var profile = mapper.map(
        battleTag.value(), gateway.getPlayer(battleTag.overfastId()), false);
    snapshots.save(profile);
    return profile;
  }

  public List<HistoryPointDto> history(BattleTag battleTag) {
    return snapshots.history(battleTag);
  }

  public List<RecentProfileDto> recent() {
    return snapshots.recent();
  }

  public PlayerProfileDto stored(BattleTag battleTag) {
    return snapshots.stored(battleTag);
  }
}
