package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.service.MinecraftServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class ConsoleWebSocketController {

  @Autowired
  private MinecraftServerService service;

  @MessageMapping("/command")
  public void handleCommand(String command) {
    service.sendCommand(command);
  }

}
