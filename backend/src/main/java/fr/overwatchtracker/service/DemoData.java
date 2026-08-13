package fr.overwatchtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.overwatchtracker.dto.PlayerDtos.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DemoData {
  private final ObjectMapper mapper;
  public DemoData(ObjectMapper mapper){this.mapper=mapper;}
  public PlayerProfileDto profile(){
    var ranks=List.of(new RankDto("tank","platinum",3,18),new RankDto("damage","diamond",5,21),new RankDto("support","gold",2,14));
    var heroes=List.of(hero("ana","Ana",68400,92,51),hero("tracer","Tracer",52200,71,38),hero("reinhardt","Reinhardt",39600,58,29),hero("kiriko","Kiriko",28800,39,22));
    return new PlayerProfileDto("Demo#0000","DemoPlayer","https://d15f34w2p8l1cc.cloudfront.net/overwatch/daeddd96e58a2150afa6ffc3c5503ae7f96afc2e22899210d444f45dee508c6c.png",null,"pc",Instant.now(),189000L,260L,140L,120L,53.8,ranks,heroes,Map.of("éliminations",4821,"soins",912340,"parties jouées",260),true);
  }
  public String json(PlayerProfileDto p){try{return mapper.writeValueAsString(p);}catch(Exception e){throw new IllegalStateException(e);}}
  private HeroDto hero(String key,String name,long time,long games,long wins){return new HeroDto(key,name,time,games,wins,Math.round(wins*1000d/games)/10d,Map.of());}
}

