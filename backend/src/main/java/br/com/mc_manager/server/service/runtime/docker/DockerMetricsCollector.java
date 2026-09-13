package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static br.com.mc_manager.server.service.runtime.util.RamUnitParser.toMb;

public class DockerMetricsCollector implements MetricsCollector {

  private final Server server;
  private final DockerClient dockerClient;
  private final DockerProcessLifecycle lifecycle;
  private final Supplier<Instant> statedAtSupplier;
  private final Supplier<Integer> onlinePlayersSupplier;
  private final Clock clock;
  private final long maxRamMb;
  
  private final AtomicReference<Long> lastTotalUsageNanos = new AtomicReference<>(null);
  private final AtomicReference<Instant> lastSampleTime = new AtomicReference<>(null);

  DockerMetricsCollector(
          Server server,
          DockerClient dockerClient,
          DockerProcessLifecycle lifecycle,
          Supplier<Instant> startedAtSupplier,
          Supplier<Integer> onlinePlayersSupplier,
          Clock clock
  ) {
    this.server = server;
    this.dockerClient = dockerClient;
    this.lifecycle = lifecycle;
    this.statedAtSupplier = startedAtSupplier;
    this.onlinePlayersSupplier = onlinePlayersSupplier;
    this.clock = clock;
    this.maxRamMb = toMb(server.getMaxRam());
  }

  long getMaxRamMb() {
    return maxRamMb;
  }

  @Override
  public ServerMetricsDTO collect() {
    if (!lifecycle.isRunning()) return null;

    Statistics stats = fetchStats();
    if (stats == null) return null;

    double cpuUsage = calculateCpuPercent(stats);
    long ramUsedMb = (stats.getMemoryStats().getUsage() != null ? stats.getMemoryStats().getUsage() : 0L) / (1024L * 1024L);

    Instant startedAt = statedAtSupplier.get();
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

  private Statistics fetchStats() {
    AtomicReference<Statistics> statsRef = new AtomicReference<>(null);
    CountDownLatch latch = new CountDownLatch(1);

    ResultCallback<Statistics> calback = dockerClient.statsCmd(lifecycle.getContainerId())
            .exec(new ResultCallback.Adapter<>() {
              @Override
              public void onNext(Statistics stats) {
                statsRef.set(stats);
                latch.countDown();
              }
            });

    try {
      if (!latch.await(3, TimeUnit.SECONDS)) return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } finally {
      closeQuietly(calback);
    }

    return statsRef.get();
  }

  private double calculateCpuPercent(Statistics stats) {
    Long totalUsageNanos = safeGet(() ->
            stats.getCpuStats().getCpuUsage().getTotalUsage()
    );

    if (totalUsageNanos == null) return 0.0;

    Instant now = clock.instant();

    Long previousUsageNanos = lastTotalUsageNanos.getAndSet(totalUsageNanos);
    Instant previousTime = lastSampleTime.getAndSet(now);

    if (previousUsageNanos == null || previousTime == null) return 0.0;

    long cpuDeltaNanos = totalUsageNanos - previousUsageNanos;
    long elapsedNanos = Duration.between(previousTime, now).toNanos();

    if (cpuDeltaNanos <= 0 || elapsedNanos <= 0) return 0.0;

    long onlineCpus = stats.getCpuStats().getOnlineCpus() != null
            ? stats.getCpuStats().getOnlineCpus()
            : 1;

    double percent = (cpuDeltaNanos / (double) elapsedNanos) * 100.0 / onlineCpus;

    // Protecao contra picos irreais causados por delays no scheduling da JVM/thread
    return Math.min(percent, 100.0 * onlineCpus);
  }

  private Long safeGet(Supplier<Long> supplier) {
    try {
      return supplier.get();
    } catch (NullPointerException e) {
      return null;
    }
  }

  private void closeQuietly(ResultCallback<Statistics> calback) {
    try {
      calback.close();
    } catch (Exception ignored) {
    }
  }

}