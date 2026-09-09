package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  private static final Long SERVER_ID = 1L;
  private static final Long PID = 12345L;

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ServerRuntimeService ServerRuntimeService;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private OperatingSystem operatingSystem;

  @InjectMocks
  private MetricsService metricsService;

  @BeforeEach
  void setUp() {
    lenient().when(systemInfo.getOperatingSystem()).thenReturn(operatingSystem);
  }

  @Nested
  @DisplayName("broadcastAllMetrics - servidor sem processo ativo")
  class SemProcessoAtivoTests {

    @Test
    @DisplayName("Não envia métricas quando não há servidores ativos")
    void semServidoresAtivos_naoEnviaNada() {
      // Arrange
      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of());

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verifyNoInteractions(template);
    }

    @Test
    @DisplayName("Não envia métricas quando o servidor ativo não tem PID (processo não iniciado)")
    void servidorSemPid_naoEnviaMetricas() {
      // Arrange
      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of(SERVER_ID));
      when(ServerRuntimeService.getProcessPid(SERVER_ID)).thenReturn(null);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, never()).convertAndSend(anyString(), any(ServerMetricsDTO.class));
    }

    @Test
    @DisplayName("Não envia métricas quando o SO não encontra o processo do PID")
    void processoNaoEncontradoNoSO_naoEnviaMetricas() {
      // Arrange
      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of(SERVER_ID));
      when(ServerRuntimeService.getProcessPid(SERVER_ID)).thenReturn(PID);
      when(operatingSystem.getProcess(PID.intValue())).thenReturn(null);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, never()).convertAndSend(anyString(), any(ServerMetricsDTO.class));
    }
  }

  @Nested
  @DisplayName("broadcastAllMetrics - servidor ativo com processo válido")
  class ComProcessoAtivoTests {

    private OSProcess process;

    @BeforeEach
    void setUpProcesso() {
      process = mock(OSProcess.class);

      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of(SERVER_ID));
      when(ServerRuntimeService.getProcessPid(SERVER_ID)).thenReturn(PID);
      when(operatingSystem.getProcess(PID.intValue())).thenReturn(process);
    }

    @Test
    @DisplayName("Envia métricas para o tópico correto do servidor")
    void enviaMetricasParaTopicoCorreto() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(268_435_456L); // 256 MB
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(Instant.now().minusSeconds(100));
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(10);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, times(1))
              .convertAndSend(eq("/topic/metrics/" + SERVER_ID), any(ServerMetricsDTO.class));
    }

    @Test
    @DisplayName("Primeira medição do servidor retorna CPU igual a 0.0 (sem tick anterior)")
    void primeiraMedicao_cpuZero() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(256L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(Instant.now().minusSeconds(7200));
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(0);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template).convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      assertThat(captor.getValue().cpuUsagePercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Segunda medição calcula CPU com base no tick anterior")
    void segundaMedicao_calculaCpuComTickAnterior() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(512L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(Instant.now().minusSeconds(3600));
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(15);
      when(process.getProcessCpuLoadBetweenTicks(process)).thenReturn(0.5);

      // Act
      metricsService.broadcastAllMetrics();
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template, times(2))
              .convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      assertThat(captor.getValue().cpuUsagePercent()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Calcula ramUsedMb dividindo residentSetSize por 1024*1024")
    void calculaRamUsedMbCorretamente() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(268_435_456L);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(Instant.now().minusSeconds(100));
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(10);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template).convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      assertThat(captor.getValue().ramUsedMb()).isEqualTo(256L);
    }

    @Test
    @DisplayName("Calcula uptime em segundos a partir do horário de início")
    void calculaUptimeEmSegundos() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(512L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);

      // servidor iniciado há 3 horas -> 3 * 3600 = 10800 segundos
      Instant startedAt = Instant.now().minusSeconds(10800);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(startedAt);
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(20);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template).convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      // Tolerância de 1s para absorver o tempo de execução do teste em si
      assertThat(captor.getValue().uptimeSeconds()).isBetween(10799L, 10801L);
    }

    @Test
    @DisplayName("Uptime é 0 quando o servidor não possui horário de início registrado")
    void uptimeZeroQuandoStartedAtNulo() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(256L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(null);
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(0);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template).convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      assertThat(captor.getValue().uptimeSeconds()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Repassa a contagem de jogadores online do ServerRuntimeService")
    void repassaContagemDeJogadoresOnline() {
      // Arrange
      when(process.getResidentSetSize()).thenReturn(256L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(SERVER_ID)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(SERVER_ID)).thenReturn(Instant.now().minusSeconds(60));
      when(ServerRuntimeService.getOnlinePlayersCount(SERVER_ID)).thenReturn(7);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      ArgumentCaptor<ServerMetricsDTO> captor = ArgumentCaptor.forClass(ServerMetricsDTO.class);
      verify(template).convertAndSend(eq("/topic/metrics/" + SERVER_ID), captor.capture());

      assertThat(captor.getValue().onlinePlayers()).isEqualTo(7);
    }
  }

  @Nested
  @DisplayName("broadcastAllMetrics - múltiplos servidores")
  class MultiplosServidoresTests {

    @Test
    @DisplayName("Envia métricas separadamente para cada servidor ativo")
    void enviaMetricasParaCadaServidorAtivo() {
      //Arrange
      Long serverId1 = 1L;
      Long serverId2 = 2L;

      OSProcess process1 = mock(OSProcess.class);
      OSProcess process2 = mock(OSProcess.class);

      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of(serverId1, serverId2));

      when(ServerRuntimeService.getProcessPid(serverId1)).thenReturn(111L);
      when(ServerRuntimeService.getProcessPid(serverId2)).thenReturn(222L);

      when(operatingSystem.getProcess(111)).thenReturn(process1);
      when(operatingSystem.getProcess(222)).thenReturn(process2);

      when(process1.getResidentSetSize()).thenReturn(100L * 1024 * 1024);
      when(process2.getResidentSetSize()).thenReturn(200L * 1024 * 1024);

      when(ServerRuntimeService.getMaxRamMb(anyLong())).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(anyLong())).thenReturn(Instant.now().minusSeconds(60));
      when(ServerRuntimeService.getOnlinePlayersCount(anyLong())).thenReturn(1);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template).convertAndSend(eq("/topic/metrics/" + serverId1), any(ServerMetricsDTO.class));
      verify(template).convertAndSend(eq("/topic/metrics/" + serverId2), any(ServerMetricsDTO.class));
    }

    @Test
    @DisplayName("Um servidor sem processo não impede o broadcast dos demais")
    void servidorSemProcessoNaoImpedeDemais() {
      // Arrange
      Long serverIdAtivo = 1L;
      Long serverIdSemPid = 2L;

      OSProcess process = mock(OSProcess.class);

      when(ServerRuntimeService.getActiveServersIds()).thenReturn(Set.of(serverIdAtivo, serverIdSemPid));

      when(ServerRuntimeService.getProcessPid(serverIdAtivo)).thenReturn(PID);
      when(ServerRuntimeService.getProcessPid(serverIdSemPid)).thenReturn(null);

      when(operatingSystem.getProcess(PID.intValue())).thenReturn(process);

      when(process.getResidentSetSize()).thenReturn(100L * 1024 * 1024);
      when(ServerRuntimeService.getMaxRamMb(serverIdAtivo)).thenReturn(4096L);
      when(ServerRuntimeService.getStartedAt(serverIdAtivo)).thenReturn(Instant.now().minusSeconds(60));
      when(ServerRuntimeService.getOnlinePlayersCount(serverIdAtivo)).thenReturn(1);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, times(1))
              .convertAndSend(eq("/topic/metrics/" + serverIdAtivo), any(ServerMetricsDTO.class));
      verify(template, never())
              .convertAndSend(eq("/topic/metrics/" + serverIdSemPid), any(ServerMetricsDTO.class));
    }
  }
}