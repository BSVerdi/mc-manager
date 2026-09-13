package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalMetricsCollectorTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:10Z");
  private static final Instant STARTED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final long PID = 123L;
  private final Supplier<Integer> onlinePlayersSupplier = () -> 5;
  private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Mock
  private Server server;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private OperatingSystem os;

  @Mock
  private OSProcess osProcess;

  @Mock
  private LocalProcessLifeCycle lifeCycle;

  @Mock
  private Process process;

  private Supplier<Instant> startedAtSupplier = () -> STARTED_AT;

  @BeforeEach
  void setUp() {
    when(server.getMaxRam()).thenReturn("2G");
  }

  // Factory por teste: estado anterior (previousTick) reinicia em cada instancia
  private LocalMetricsCollector buildCollector() {
    return new LocalMetricsCollector(
            server, systemInfo, lifeCycle,
            startedAtSupplier, onlinePlayersSupplier, clock);
  }

  private void stubHappyPathRam() {
    when(lifeCycle.getProcess()).thenReturn(process);
    when(process.isAlive()).thenReturn(true);
    when(process.pid()).thenReturn(PID);
    when(systemInfo.getOperatingSystem()).thenReturn(os);
    when(os.getProcess((int) PID)).thenReturn(osProcess);
    when(osProcess.getResidentSetSize()).thenReturn(100L * 1024 * 1024); // -> 100 MB
  }

  @Nested
  @DisplayName("collect()")
  class CollectTests {

    @Test
    @DisplayName("processo vivo e localizado - retorna DTO completo com cpu inicial zerado")
    void deveRetornarDtoCompletoQuandoProcessoEstaVivo() {
      stubHappyPathRam();

      when(server.getId()).thenReturn(1L);

      LocalMetricsCollector collector = buildCollector();
      ServerMetricsDTO dto = collector.collect();

      assertThat(dto).isNotNull();
      assertThat(dto.serverId()).isEqualTo(1L);
      assertThat(dto.cpuUsagePercent()).isEqualTo(0.0);
      assertThat(dto.ramUsedMb()).isEqualTo(100L);
      assertThat(dto.ramMaxMb()).isEqualTo(collector.getMaxRamMb());
      assertThat(dto.uptimeSeconds()).isEqualTo(10L);
      assertThat(dto.onlinePlayers()).isEqualTo(5);
    }

    @Test
    @DisplayName("segunda coleta - calcula cpu pelo delta entre ticks")
    void deveCalcularCpuNaSegundaColeta() {
      stubHappyPathRam();

      when(osProcess.getProcessCpuLoadBetweenTicks(any())).thenReturn(0.25);
      when(server.getId()).thenReturn(1L);

      LocalMetricsCollector collector = buildCollector();
      collector.collect();
      ServerMetricsDTO dto = collector.collect();

      assertThat(dto.cpuUsagePercent()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("processo null - retorna null")
    void deveRetornarNullQuandoProcessoEhNulo() {
      when(lifeCycle.getProcess()).thenReturn(null);

      assertThat(buildCollector().collect()).isNull();
    }

    @Test
    @DisplayName("processo morto - retorna null")
    void deveRetornarNullQuandoProcessoMorto() {
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.isAlive()).thenReturn(false);

      assertThat(buildCollector().collect()).isNull();
    }

    @Test
    @DisplayName("OSProcess nao localizado - retorna null")
    void deveRetornarNullQuandoOsProcessoNaoLocalizado() {
      when(lifeCycle.getProcess()).thenReturn(process);
      when(process.isAlive()).thenReturn(true);
      when(process.pid()).thenReturn(PID);
      when(systemInfo.getOperatingSystem()).thenReturn(os);
      when(os.getProcess((int) PID)).thenReturn(null);

      assertThat(buildCollector().collect()).isNull();
    }

    @Test
    @DisplayName("startedAt null - retorna DTO valido com uptime zero")
    void deveRetornarUptimeZeroQuandoStartedAtEhNulo() {
      startedAtSupplier = () -> null;
      stubHappyPathRam();

      LocalMetricsCollector collector = buildCollector();
      ServerMetricsDTO dto = collector.collect();

      assertThat(dto).isNotNull();
      assertThat(dto.uptimeSeconds()).isZero();
    }
  }

  @Nested
  @DisplayName("getMaxRamMb()")
  class GetMaxRamMbTests {

    @Test
    @DisplayName("retorna o maxRamMb calculado no construtor")
    void deveRetornarMaxRamMbCalculadoNoConstrutor() {
      LocalMetricsCollector collector = buildCollector();

      assertThat(collector.getMaxRamMb()).isPositive();
    }
  }
}