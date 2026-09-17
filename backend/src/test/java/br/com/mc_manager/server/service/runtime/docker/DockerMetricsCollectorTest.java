package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.CpuUsageConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerMetricsCollector")
class DockerMetricsCollectorTest {

  private static final Instant FIXED_NOW = Instant.parse("2024-06-15T12:00:00Z");
  private static final Instant STARTED_AT = Instant.parse("2024-06-15T11:00:00Z");

  @Mock
  private Server server;
  @Mock
  private DockerClient dockerClient;
  @Mock
  private DockerProcessLifecycle lifecycle;
  @Mock
  private Supplier<Instant> statedAtSupplier;
  @Mock
  private Supplier<Integer> onlinePlayersSupplier;
  @Mock
  private StatsCmd statsCmd;
  @Mock
  private ResultCallback<Statistics> resultCallback;
  @Mock
  private Statistics stats;
  @Mock
  private CpuStatsConfig cpuStats;
  @Mock
  private CpuStatsConfig preCpuStats;
  @Mock
  private CpuUsageConfig cpuUsage;
  @Mock
  private CpuUsageConfig preCpuUsage;
  @Mock
  private MemoryStatsConfig memoryStats;

  private Clock clock;
  private DockerMetricsCollector collector;

  @BeforeEach
  void setUp() {
    when(server.getMaxRam()).thenReturn("2G");
    clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    collector = new DockerMetricsCollector(
            server,
            dockerClient,
            lifecycle,
            statedAtSupplier,
            onlinePlayersSupplier,
            clock
    );
  }

  @Nested
  @DisplayName("collect()")
  class CollectTest {

    @Test
    @DisplayName("deveRetornarMetricasQuandoServidorEstiverRodando")
    void deveRetornarMetricasQuandoServidorEstiverRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      when(stats.getCpuStats()).thenReturn(cpuStats);
      when(stats.getMemoryStats()).thenReturn(memoryStats);
      when(cpuStats.getCpuUsage()).thenReturn(cpuUsage);
      when(cpuUsage.getTotalUsage()).thenReturn(5000L);
      when(memoryStats.getUsage()).thenReturn(536_870_912L); // 512 MB

      doAnswer(invocation -> {
        ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
        callback.onNext(stats);
        return resultCallback;
      }).when(statsCmd).exec(any());

      when(server.getId()).thenReturn(1L);
      when(statedAtSupplier.get()).thenReturn(STARTED_AT);
      when(onlinePlayersSupplier.get()).thenReturn(15);

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNotNull();
      assertThat(resultado.serverId()).isEqualTo(1L);
      assertThat(resultado.ramUsedMb()).isEqualTo(512L);
      assertThat(resultado.ramMaxMb()).isEqualTo(2048L);
      assertThat(resultado.uptimeSeconds()).isEqualTo(3600L);
      assertThat(resultado.onlinePlayers()).isEqualTo(15);
    }

    @Test
    @DisplayName("deveRetornarNuloQuandoServidorNaoEstiverRodando")
    void deveRetornarNuloQuandoServidorNaoEstiverRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("deveRetornarNuloQuandoCallbackNuncaForChamado")
    void deveRetornarNuloQuandoCallbackNuncaForChamado() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      doAnswer(invocation -> resultCallback).when(statsCmd).exec(any());

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("deveRetornarNuloQuandoStatsForNulo")
    void deveRetornarNuloQuandoStatsForNulo() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      doAnswer(invocation -> {
        ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
        callback.onNext(null);
        return resultCallback;
      }).when(statsCmd).exec(any());

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("deveRetornarCpuZeroQuandoCpuDeltaForNegativo")
    void deveRetornarCpuZeroQuandoCpuDeltaForNegativo() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      when(stats.getCpuStats()).thenReturn(cpuStats);
      when(cpuStats.getCpuUsage()).thenReturn(cpuUsage);
      when(cpuUsage.getTotalUsage()).thenReturn(5_000_000_000L, 3_000_000_000L);
      when(stats.getMemoryStats()).thenReturn(memoryStats);
      when(memoryStats.getUsage()).thenReturn(100L);

      doAnswer(invocation -> {
        ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
        callback.onNext(stats);
        return resultCallback;
      }).when(statsCmd).exec(any());

      when(server.getId()).thenReturn(1L);
      when(statedAtSupplier.get()).thenReturn(STARTED_AT);
      when(onlinePlayersSupplier.get()).thenReturn(0);

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNotNull();
      assertThat(resultado.cpuUsagePercent()).isEqualTo(0.0);
    }


    @Test
    @DisplayName("deveTratarUsoDeMemoriaNuloComoZeroMb")
    void deveTratarUsoDeMemoriaNuloComoZeroMb() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      when(stats.getMemoryStats()).thenReturn(memoryStats);
      when(memoryStats.getUsage()).thenReturn(null);

      doAnswer(invocation -> {
        ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
        callback.onNext(stats);
        return resultCallback;
      }).when(statsCmd).exec(any());

      when(server.getId()).thenReturn(1L);
      when(statedAtSupplier.get()).thenReturn(STARTED_AT);
      when(onlinePlayersSupplier.get()).thenReturn(0);

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNotNull();
      assertThat(resultado.ramUsedMb()).isEqualTo(0L);
    }

    @Test
    @DisplayName("deveTratarStartedAtNuloComoUptimeZero")
    void deveTratarStartedAtNuloComoUptimeZero() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      when(lifecycle.getContainerId()).thenReturn("abc123");
      when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);

      when(stats.getMemoryStats()).thenReturn(memoryStats);
      when(memoryStats.getUsage()).thenReturn(0L);

      doAnswer(invocation -> {
        ResultCallback.Adapter<Statistics> callback = invocation.getArgument(0);
        callback.onNext(stats);
        return resultCallback;
      }).when(statsCmd).exec(any());

      when(server.getId()).thenReturn(1L);
      when(statedAtSupplier.get()).thenReturn(null); // null → uptime = 0
      when(onlinePlayersSupplier.get()).thenReturn(0);

      // Act
      ServerMetricsDTO resultado = collector.collect();

      // Assert
      assertThat(resultado).isNotNull();
      assertThat(resultado.uptimeSeconds()).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("getMaxRamMb()")
  class GetMaxRamMbTest {

    @Test
    @DisplayName("deveRetornarMaxRamMbConvertido")
    void deveRetornarMaxRamMbConvertido() {
      // Act
      long maxRamMb = collector.getMaxRamMb();

      // Assert
      assertThat(maxRamMb).isEqualTo(2048L);
    }
  }
}