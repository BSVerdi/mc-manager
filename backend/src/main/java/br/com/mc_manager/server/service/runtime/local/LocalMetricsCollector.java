package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static br.com.mc_manager.server.service.runtime.util.RamUnitParser.toMb;

public class LocalMetricsCollector implements MetricsCollector {

  private final Server server;
  private final SystemInfo systemInfo;
  private final LocalProcessLifeCycle lifeCycle;
  private final Supplier<Instant> startedAtSupplier;
  private final Supplier<Integer> onlinePlayersSupplier;
  private final Clock clock;
  private final long maxRamMb;

  private volatile OSProcess previousTick;

  public LocalMetricsCollector(
          Server server,
          SystemInfo systemInfo,
          LocalProcessLifeCycle lifeCycle,
          Supplier<Instant> startedAtSupplier,
          Supplier<Integer> onlinePlayersSupplier,
          Clock clock
  ) {
    this.server = server;
    this.systemInfo = systemInfo;
    this.lifeCycle = lifeCycle;
    this.startedAtSupplier = startedAtSupplier;
    this.onlinePlayersSupplier = onlinePlayersSupplier;
    this.clock = clock;
    this.maxRamMb = toMb(server.getMaxRam());
  }

  long getMaxRamMb() {
    return maxRamMb;
  }

  @Override
  public ServerMetricsDTO collect() {
    Process process = lifeCycle.getProcess();

    if (process == null || !process.isAlive()) return null;

    OperatingSystem os = systemInfo.getOperatingSystem();
    OSProcess current = os.getProcess((int) process.pid());

    if (current == null) return null;

    double cpuUsage = previousTick == null ? 0.0
            : current.getProcessCpuLoadBetweenTicks(previousTick) * 100;
    previousTick = current;

    long ramUsedMb = current.getResidentSetSize() / (1024 * 1024);

    Instant startedAt = startedAtSupplier.get();
    long uptimeSeconds = startedAt != null ? Duration.between(startedAt, clock.instant()).toSeconds() : 0;

    return new ServerMetricsDTO(
            server.getId(),
            Math.round(cpuUsage) / 10.0,
            ramUsedMb,
            maxRamMb,
            uptimeSeconds,
            onlinePlayersSupplier.get()
    );
  }
}
