package fr.overwatchtracker.api;

import fr.overwatchtracker.dto.PlayerDtos.*;
import fr.overwatchtracker.service.PlayerService;
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
  public PlayerProfileDto lookup(@Valid @RequestBody LookupRequest request){return service.load(request.battleTag(),false);}
  @GetMapping("/recent") public List<RecentProfileDto> recent(){return service.recent();}
  @GetMapping("/{battleTag}/stored") public PlayerProfileDto stored(@PathVariable @Pattern(regexp="^[^#\\s-]{2,32}(#|-)\\d{3,12}$") String battleTag){return service.stored(battleTag);}
  @PostMapping("/{battleTag}/refresh") public PlayerProfileDto refresh(@PathVariable @Pattern(regexp="^[^#\\s-]{2,32}(#|-)\\d{3,12}$") String battleTag){return service.load(battleTag,true);}
  @GetMapping("/{battleTag}/history") public List<HistoryPointDto> history(@PathVariable @Pattern(regexp="^[^#\\s-]{2,32}(#|-)\\d{3,12}$") String battleTag){return service.history(battleTag);}
}
