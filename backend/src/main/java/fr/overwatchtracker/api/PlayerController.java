package fr.overwatchtracker.api;

import fr.overwatchtracker.dto.PlayerDtos.*;
import fr.overwatchtracker.dto.PlayerDtos;
import fr.overwatchtracker.service.PlayerApplicationService;
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
  private final PlayerApplicationService service;
  public PlayerController(PlayerApplicationService service){this.service=service;}
  @PostMapping("/lookup") @ResponseStatus(HttpStatus.CREATED)
  public PlayerProfileDto lookup(@Valid @RequestBody LookupRequest request){return service.load(BattleTag.parse(request.battleTag()),false);}
  @GetMapping("/recent") public List<RecentProfileDto> recent(){return service.recent();}
  @GetMapping("/saved") public List<RecentProfileDto> saved(){return service.saved();}
  @GetMapping("/{battleTag}/stored") public PlayerProfileDto stored(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.stored(BattleTag.parse(battleTag));}
  @PostMapping("/{battleTag}/refresh") public PlayerProfileDto refresh(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.load(BattleTag.parse(battleTag),true);}
  @GetMapping("/{battleTag}/history") public List<HistoryPointDto> history(@PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag){return service.history(BattleTag.parse(battleTag));}
  @GetMapping("/{battleTag}/references/{role}")
  public RankReferenceDto references(
      @PathVariable @Pattern(regexp=PlayerDtos.BATTLE_TAG_PATTERN) String battleTag,
      @PathVariable @Pattern(regexp="tank|damage|support") String role) {
    return service.references(BattleTag.parse(battleTag), role);
  }
}
