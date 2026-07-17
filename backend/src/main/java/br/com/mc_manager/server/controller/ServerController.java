package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.service.MinecraftServerService;
import org.springframework.web.bind.annotation.*;

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
}
