package fr.overwatchtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.overwatchtracker.dto.PlayerDtos.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class PlayerProfileMapper {
  public PlayerProfileDto map(String battleTag, JsonNode root, boolean demo) {
    JsonNode summary=root.path("summary");
    String username=text(summary,"username", battleTag.substring(0,battleTag.indexOf('#')));
    String platform=firstPlatform(summary.path("competitive"));
    List<RankDto> ranks=readRanks(summary.path("competitive"),platform);
    JsonNode statsRoot=root.path("stats");
    long time=findNumeric(statsRoot,"time_played");
    long games=findNumeric(statsRoot,"games_played");
    long wins=findNumeric(statsRoot,"games_won");
    long losses=Math.max(0,games-wins);
    double winRate=games==0 ? 0 : Math.round(wins*1000d/games)/10d;
    List<HeroDto> heroes=readHeroes(statsRoot,platform);
    Map<String,Object> globals=new LinkedHashMap<>();
    globals.put("parties jouées",games); globals.put("victoires",wins); globals.put("défaites",losses);
    return new PlayerProfileDto(battleTag,username,text(summary,"avatar",null),text(summary,"namecard",null),platform,
        Instant.now(),time,games,wins,losses,winRate,ranks,heroes,globals,demo);
  }

  private List<RankDto> readRanks(JsonNode competitive,String platform) {
    List<RankDto> out=new ArrayList<>();
    JsonNode roles=platform==null ? competitive : competitive.path(platform);
    roles.fields().forEachRemaining(e -> {
      JsonNode r=e.getValue();
      if(r.isObject() && r.has("division")) out.add(new RankDto(e.getKey(),text(r,"division","—"),integer(r,"tier"),rankScore(text(r,"division",null),integer(r,"tier"))));
    });
    return out;
  }
  private List<HeroDto> readHeroes(JsonNode statsRoot,String platform) {
    JsonNode competitive=platform==null ? statsRoot.path("pc").path("competitive") : statsRoot.path(platform).path("competitive");
    JsonNode careers=competitive.path("career_stats");
    JsonNode comparisons=competitive.path("heroes_comparisons");
    Map<String,Long> times=comparisonValues(comparisons,"time_played");
    Map<String,Long> wins=comparisonValues(comparisons,"games_won");
    Map<String,Double> rates=comparisonDecimals(comparisons,"win_percentage");
    List<HeroDto> out=new ArrayList<>();
    times.forEach((key,time)->{
      if(time<=0 || key.equals("all-heroes")) return;
      JsonNode career=careers.path(key);
      long games=findNumeric(career,"games_played");
      long heroWins=wins.getOrDefault(key,findNumeric(career,"games_won"));
      double rate=rates.getOrDefault(key,games==0?0:Math.round(heroWins*1000d/games)/10d);
      out.add(new HeroDto(key,title(key),time,games,heroWins,rate,Map.of()));
    });
    return out.stream().sorted(Comparator.comparing(HeroDto::timePlayed).reversed()).limit(12).toList();
  }
  private Map<String,Long> comparisonValues(JsonNode comparisons,String stat){
    Map<String,Long> values=new LinkedHashMap<>();
    for(JsonNode item:comparisons.path(stat).path("values")) if(item.hasNonNull("hero")&&item.path("value").isNumber()) values.put(item.path("hero").asText(),item.path("value").asLong());
    return values;
  }
  private Map<String,Double> comparisonDecimals(JsonNode comparisons,String stat){
    Map<String,Double> values=new LinkedHashMap<>();
    for(JsonNode item:comparisons.path(stat).path("values")) if(item.hasNonNull("hero")&&item.path("value").isNumber()) values.put(item.path("hero").asText(),item.path("value").asDouble());
    return values;
  }
  private long findNumeric(JsonNode node,String key) {
    if(node==null)return 0;
    if(node.isObject()) {
      if(node.has(key) && node.get(key).isNumber()) return node.get(key).asLong();
      if(key.equals(node.path("key").asText()) && node.path("value").isNumber()) return node.path("value").asLong();
      long total=0; var it=node.elements(); while(it.hasNext()) total=Math.max(total,findNumeric(it.next(),key)); return total;
    }
    if(node.isArray()){long total=0;for(JsonNode n:node)total=Math.max(total,findNumeric(n,key));return total;} return 0;
  }
  private String firstPlatform(JsonNode node){if(!node.isObject())return null;var it=node.fieldNames();return it.hasNext()?it.next():null;}
  private String text(JsonNode n,String k,String fallback){return n.hasNonNull(k)?n.get(k).asText():fallback;}
  private Integer integer(JsonNode n,String k){return n.hasNonNull(k)?n.get(k).asInt():null;}
  private Integer rankScore(String division,Integer tier){if(division==null||tier==null)return null;int base=List.of("bronze","silver","gold","platinum","diamond","master","grandmaster","champion").indexOf(division.toLowerCase());return base<0?null:base*5+(6-tier);}
  private String title(String key){String s=key.replace('-',' ').replace('_',' ');return s.isBlank()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);}
}
