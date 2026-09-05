package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsService {

  private final SystemInfo systemInfo;
  private final Map<Long, OSProcess> previousTicks = new ConcurrentHashMap<>();

  private final SimpMessagingTemplate template;
  private final ServerRuntimeService serverManagerService;

  public MetricsService(SystemInfo systemInfo, SimpMessagingTemplate template, ServerRuntimeService serverManagerService) {
    this.systemInfo = systemInfo;
    this.template = template;
    this.serverManagerService = serverManagerService;
  }

  @Scheduled(fixedRate = 1000) // a cada 3s
  public void broadcastAllMetrics() {
    var activeIds = serverManagerService.getActiveServersIds();

    previousTicks.keySet().removeIf(id -> !activeIds.contains(id));

    for (Long serverId : activeIds) {
      broadcastMetricsFor(serverId);
    }
  }

  private void broadcastMetricsFor(Long serverId) {
    Long pid = serverManagerService.getProcessPid(serverId);

    if (pid == null) {
      previousTicks.remove(serverId);
      return;
    }

    OperatingSystem os = systemInfo.getOperatingSystem();
    OSProcess process = os.getProcess(pid.intValue());

    if (process == null) {
      return;
    }

    OSProcess previousTick = previousTicks.get(serverId);
    double cpuUsage = previousTick == null ? 0.0 :
            process.getProcessCpuLoadBetweenTicks(previousTick) * 100;

    previousTicks.put(serverId, process);

    Long ramUsedMb = process.getResidentSetSize() / (1024 * 1024);
    Long ramMaxMb = serverManagerService.getMaxRamMb(serverId);

    Instant startedAt = serverManagerService.getStartedAt(serverId);

    Long uptimeSeconds = startedAt != null ? Duration.between(startedAt, Instant.now()).getSeconds() : 0;

    var onlinePlayers = serverManagerService.getOnlinePlayersCount(serverId);

    ServerMetricsDTO metrics = new ServerMetricsDTO(
            serverId,
            Math.round(cpuUsage) / 10.0,
            ramUsedMb,
            ramMaxMb,
            uptimeSeconds,
            onlinePlayers
    );

    template.convertAndSend("/topic/metrics/" + serverId, metrics);
  }

}
