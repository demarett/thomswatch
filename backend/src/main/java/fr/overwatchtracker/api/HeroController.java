package fr.overwatchtracker.api;

import fr.overwatchtracker.dto.PlayerDtos.HeroPortraitDto;
import fr.overwatchtracker.integration.OverfastGateway;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heroes")
public class HeroController {
  private final OverfastGateway overfast;
  public HeroController(OverfastGateway overfast){this.overfast=overfast;}
  @GetMapping public List<HeroPortraitDto> list(){
    var heroes=new ArrayList<HeroPortraitDto>();
    for(var hero:overfast.getHeroes()) heroes.add(new HeroPortraitDto(hero.path("key").asText(),hero.path("name").asText(),hero.path("portrait").asText()));
    return heroes;
  }
}
