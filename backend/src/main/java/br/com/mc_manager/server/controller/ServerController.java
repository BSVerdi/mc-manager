package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.service.MinecraftServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
@CrossOrigin(origins = "*")
public class ServerController {

  private final MinecraftServerService service;

  public ServerController(MinecraftServerService service) {
    this.service = service;
  }

  @PostMapping("/start")
  public void start() {
    service.start();
  }

  @PostMapping("/stop")
  public void stop() {
    service.stop();
  }

  @PostMapping("/restart")
  public void restart() {
    service.restart();
  }

  @GetMapping("/status")
  public Map<String, ServerStatus> status() {
    return Map.of("status", service.getStatus());
  }

  @GetMapping("/players")
  public ResponseEntity<List<PlayerDTO>> getPlayers() throws IOException {
    List<PlayerDTO> players = service.getPlayers();

    return ResponseEntity.ok(players);
  }

  @PatchMapping("/players/op")
  public ResponseEntity<Void> opPlayer(@RequestParam String nickname) {
    service.opPlayer(nickname);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/players/deop")
  public ResponseEntity<Void> deopPlayer(@RequestParam String nickname) {
    service.deopPlayer(nickname);

    return ResponseEntity.ok().build();
  }
}
