package fr.overwatchtracker.api;

import fr.overwatchtracker.dto.PlayerDtos.*;
import fr.overwatchtracker.dto.PlayerDtos;
import fr.overwatchtracker.service.PlayerService;
import fr.overwatchtracker.service.BattleTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/players")
public class PlayerController {
  private final PlayerService service;
  public PlayerController(PlayerService service){this.service=service;}
  @PostMapping("/lookup") @ResponseStatus(HttpStatus.CREATED)
  public PlayerProfileDto lookup(@Valid @RequestBody LookupRequest request){return service.load(BattleTag.parse(request.battleTag()).value(),false);}
  @GetMapping("/recent") public List<RecentProfileDto> recent(){return service.recent();}
  @GetMapping("/{battleTag}/stored") public PlayerProfileDto stored(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.stored(BattleTag.parse(battleTag).value());}
  @PostMapping("/{battleTag}/refresh") public PlayerProfileDto refresh(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.load(BattleTag.parse(battleTag).value(),true);}
  @GetMapping("/{battleTag}/history") public List<HistoryPointDto> history(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.history(BattleTag.parse(battleTag).value());}
}
