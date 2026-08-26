package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.service.ServerRuntimeService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ConsoleWebSocketController {


  private final ServerRuntimeService serverManagerService;

  public ConsoleWebSocketController(ServerRuntimeService serverManagerService) {
    this.serverManagerService = serverManagerService;
  }

  @MessageMapping("/command/{serverId}")
  public void handleCommand(
          @DestinationVariable Long serverId,
          String command
  ) {
    serverManagerService.sendCommand(serverId, command);
  }

}
