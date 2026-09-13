package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {


  private final SimpMessagingTemplate template;
  private final ServerRuntimeService serverRuntimeService;

  public MetricsService(SimpMessagingTemplate template, ServerRuntimeService serverRuntimeService) {
    this.template = template;
    this.serverRuntimeService = serverRuntimeService;
  }

  @Scheduled(fixedRate = 1000) // a cada 3s
  public void broadcastAllMetrics() {
    var activeIds = serverRuntimeService.getActiveServersIds();

    for (Long serverId : activeIds) {
      broadcastMetricsFor(serverId);
    }
  }

  private void broadcastMetricsFor(Long serverId) {
    ServerMetricsDTO metrics = serverRuntimeService.getMetrics(serverId);

    if (metrics == null) return;

    template.convertAndSend("/topic/metrics/" + serverId, metrics);
  }

}
