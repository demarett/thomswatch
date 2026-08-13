package fr.overwatchtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.domain.*;
import fr.overwatchtracker.dto.PlayerDtos.*;
import fr.overwatchtracker.integration.OverfastClient;
import java.util.List;
import java.util.LinkedHashMap;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
  private final OverfastClient client; private final PlayerMapper mapper; private final PlayerSnapshotRepository snapshots;
  private final ObjectMapper objectMapper; private final CacheManager cacheManager; private final DemoData demo;
  public PlayerService(OverfastClient client,PlayerMapper mapper,PlayerSnapshotRepository snapshots,ObjectMapper objectMapper,CacheManager cacheManager,DemoData demo){
    this.client=client;this.mapper=mapper;this.snapshots=snapshots;this.objectMapper=objectMapper;this.cacheManager=cacheManager;this.demo=demo;
  }
  @Transactional public PlayerProfileDto load(String battleTag,boolean refresh){
    String normalized=normalize(battleTag);
    if(normalized.equalsIgnoreCase("Demo#0000")) { var p=demo.profile(); save(p); return p; }
    String id=normalized.replace('#','-');
    if(refresh){var cache=cacheManager.getCache("overfastPlayers");if(cache!=null)cache.evict(id);}
    var profile=mapper.map(normalized,client.getPlayer(id),false); save(profile); return profile;
  }
  @Transactional(readOnly=true) public List<HistoryPointDto> history(String battleTag){return snapshots.findTop100ByBattleTagOrderByCapturedAtAsc(normalize(battleTag)).stream().map(s->new HistoryPointDto(s.getCapturedAt(),s.getTotalTimePlayed(),s.getWinRate(),s.getTankRank(),s.getDamageRank(),s.getSupportRank())).toList();}
  @Transactional(readOnly=true) public List<RecentProfileDto> recent(){
    var unique=new LinkedHashMap<String,RecentProfileDto>();
    for(var snapshot:snapshots.findTop100ByOrderByCapturedAtDesc()){
      if(unique.containsKey(snapshot.getBattleTag())) continue;
      var profile=fromJson(snapshot.getPayload());
      unique.put(snapshot.getBattleTag(),new RecentProfileDto(snapshot.getBattleTag(),snapshot.getUsername(),profile.avatar(),snapshot.getPlatform(),snapshot.getCapturedAt()));
      if(unique.size()==8) break;
    }
    return List.copyOf(unique.values());
  }
  @Transactional(readOnly=true) public PlayerProfileDto stored(String battleTag){
    var snapshot=snapshots.findFirstByBattleTagOrderByCapturedAtDesc(normalize(battleTag))
        .orElseThrow(()->new fr.overwatchtracker.integration.OverfastException("PLAYER_NOT_FOUND","Ce profil n'est pas encore enregistré."));
    return fromJson(snapshot.getPayload());
  }
  private void save(PlayerProfileDto p){snapshots.save(new PlayerSnapshot(p.battleTag(),p.capturedAt(),p.username(),p.platform(),p.timePlayed(),p.winRate(),score(p,"tank"),score(p,"damage"),score(p,"support"),toJson(p)));}
  private Integer score(PlayerProfileDto p,String role){return p.ranks().stream().filter(r->r.role().equals(role)).map(RankDto::score).findFirst().orElse(null);}
  private String toJson(PlayerProfileDto p){try{return objectMapper.writeValueAsString(p);}catch(Exception e){throw new IllegalStateException("Sérialisation du snapshot impossible",e);}}
  private PlayerProfileDto fromJson(String json){try{return objectMapper.readValue(json,PlayerProfileDto.class);}catch(Exception e){throw new IllegalStateException("Lecture du snapshot impossible",e);}}
  public String normalize(String tag){return tag.trim().replaceFirst("-(?=\\d{3,12}$)","#");}
}
