package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.ServerRuntimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/players")
public class PlayerController {

  private final PlayerService playerService;
  private final ServerRuntimeService serverManagerService;

  public PlayerController(PlayerService playerService, ServerRuntimeService serverManagerService) {
    this.playerService = playerService;
    this.serverManagerService = serverManagerService;
  }

  @GetMapping
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<List<PlayerDTO>> listServerPlayers(@PathVariable Long serverId) {
    var players = playerService.listServerPlayers(serverId);

    return ResponseEntity.ok().body(players);
  }

  @PatchMapping("/{nickname}/op")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> opPlayer(@PathVariable String nickname, @PathVariable Long serverId) {
    serverManagerService.opPlayer(serverId, nickname);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{nickname}/deop")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> deopPlayer(@PathVariable String nickname, @PathVariable Long serverId
  ) {
    serverManagerService.deopPlayer(serverId, nickname);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{nickname}/ban")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> banPlayer(@PathVariable String nickname, @PathVariable Long serverId) {
    serverManagerService.banPlayer(serverId, nickname);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{nickname}/unban")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> unbanPlayer(@PathVariable String nickname, @PathVariable Long serverId) {
    serverManagerService.unbanPlayer(serverId, nickname);

    return ResponseEntity.noContent().build();
  }

}
