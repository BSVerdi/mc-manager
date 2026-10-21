package br.com.mc_manager.server.service.runtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

public class ServerBroadcaster {
  record LogLineDTO(Long id, LogLevel level, String message, String timestamp) {
  }

  private final Long serverId;
  private final SimpMessagingTemplate template;
  private final Clock clock;
  private final AtomicLong idCounter = new AtomicLong();

  public ServerBroadcaster(Long serverId, SimpMessagingTemplate template, Clock clock) {
    this.serverId = serverId;
    this.template = template;
    this.clock = clock;
  }

  public void status(ServerStatus status) {
    template.convertAndSend("/topic/" + serverId + "/status", status);
  }

  public void consoleLine(LogLevel level, String message) {
    template.convertAndSend("/topic/" + serverId + "/console",
            new LogLineDTO(idCounter.incrementAndGet(), level, message, clock.instant().toString())
    );
  }
}

