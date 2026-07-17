package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Duration;
import java.time.Instant;

@Service
public class MetricsService {

  private final SimpMessagingTemplate template;
  private final MinecraftServerService serverService;
  private final SystemInfo systemInfo = new SystemInfo();

  private OSProcess previousTick;

  public MetricsService(SimpMessagingTemplate template, MinecraftServerService serverService) {
    this.template = template;
    this.serverService = serverService;
  }

  @Scheduled(fixedRate = 1000) // a cada 3s
  public void broadcastMetrics() {
    Long pid = serverService.getProcessPid();
    if (pid == null) {
      previousTick = null; // servidor parado: reseta a leitura anterior
      return;
    }

    OperatingSystem os = systemInfo.getOperatingSystem();
    OSProcess process = os.getProcess(pid.intValue());
    if (process == null) {
      broadcastResetedMetrics();
      return;
    }

    // CPU precisa de duas leituras no tempo para calcular %; a primeira
    // chamada sempre retorna 0, o que e' esperado.
    double cpuUsage = previousTick == null
            ? 0.0
            : process.getProcessCpuLoadBetweenTicks(previousTick) * 100;
    previousTick = process;

    HardwareAbstractionLayer hardware = systemInfo.getHardware();
    long ramUsedMb = process.getResidentSetSize() / (1024 * 1024);
    long ramMaxMb = serverService.getMaxRamMb();

    Instant startedAt = serverService.getStartedAt();

    Long uptimeSeconds = startedAt != null
            ? Duration.between(startedAt, Instant.now()).getSeconds()
            : 0;

    ServerMetricsDTO metrics = new ServerMetricsDTO(
            Math.round(cpuUsage * 10) / 10.0,
            ramUsedMb,
            ramMaxMb,
            uptimeSeconds
    );

    template.convertAndSend("/topic/metrics", metrics);
  }

  private void broadcastResetedMetrics() {
    template.convertAndSend(
            "/topic/metrics",
            new ServerMetricsDTO(0D, 0L, 0L, 0L));
  }

}
