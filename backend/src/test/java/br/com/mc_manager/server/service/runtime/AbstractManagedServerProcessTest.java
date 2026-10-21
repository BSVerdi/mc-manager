package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.LogLineDTO;
import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractManagedServerProcessTest {

  private static final Clock FIXED_CLOCK =
          Clock.fixed(Instant.parse("2025-01-15T10:30:00Z"), ZoneOffset.UTC);

  @Mock
  private Server server;

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private PlayerService playerService;

  @Mock
  private ProcessLifecycle lifecycle;

  @Mock
  private ConsoleGateway consoleGateway;

  @Mock
  private MetricsCollector metricsCollector;

  @Mock
  private ExecutorService executor;

  private TestManagedServerProcess managedProcess;
  private Runnable onExitCallback;
  private final Runnable onRestarting = () -> {
  };


  @BeforeEach
  void setUp() {
    when(server.getId()).thenReturn(1L);
    when(server.getMaxRam()).thenReturn("1G");

    lenient().when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    });

    doAnswer(invocation -> {
      onExitCallback = invocation.getArgument(0);
      return null;
    }).when(lifecycle).onExit(any(Runnable.class));

    TestManagedServerProcess.staticLifecycle = lifecycle;
    TestManagedServerProcess.staticConsoleGateway = consoleGateway;
    TestManagedServerProcess.staticMetricsCollector = metricsCollector;

    managedProcess = new TestManagedServerProcess(server, template, executor, playerService, FIXED_CLOCK);
    managedProcess.init();
  }

  private static class TestManagedServerProcess extends AbstractManagedServerProcess {

    static ProcessLifecycle staticLifecycle;
    static ConsoleGateway staticConsoleGateway;
    static MetricsCollector staticMetricsCollector;

    TestManagedServerProcess(
            Server server,
            SimpMessagingTemplate template,
            ExecutorService executor,
            PlayerService playerService,
            Clock clock
    ) {
      super(server, template, executor, playerService, clock);
    }

    @Override
    protected ProcessLifecycle createLifecycle() {
      return staticLifecycle;
    }

    @Override
    protected ConsoleGateway createConsoleGateway() {
      return staticConsoleGateway;
    }

    @Override
    protected MetricsCollector createMetricsCollector() {
      return staticMetricsCollector;
    }

    void setStatusForTest(ServerStatus newStatus) {
      status.set(newStatus);
    }
  }

  @Nested
  @DisplayName("Getters")
  class GetterTests {

    @Test
    @DisplayName("getServerId retorna o id do servidor")
    void deveRetornarIdDoServidor() {
      // Assert
      assertThat(managedProcess.getServerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getStatus retorna OFFLINE no estado inicial")
    void deveRetornarOfflineNoEstadoInicial() {
      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("isRunning delega ao lifecycle")
    void deveDelegarIsRunningAoLifecycle() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act & Assert
      assertThat(managedProcess.isRunning()).isTrue();

      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act & Assert
      assertThat(managedProcess.isRunning()).isFalse();
    }

    @Test
    @DisplayName("getStartedAt retorna null quando offline")
    void deveRetornarNullQuandoOffline() {
      // Assert
      assertThat(managedProcess.getStartedAt()).isNull();
    }

    @Test
    @DisplayName("getStartedAt retorna Instant após start bem-sucedido")
    void deveRetornarInstantAposStart() {
      // Act
      managedProcess.start();

      // Assert
      assertThat(managedProcess.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("getMaxRamMb retorna valor parseado da configuração")
    void deveRetornarMaxRamMbParseado() {
      // Assert
      assertThat(managedProcess.getMaxRamMb()).isEqualTo(1024L);
    }

    @Test
    @DisplayName("getOnlinePlayersCount retorna contagem (objeto real, estado inicial)")
    void deveRetornarContagemDeJogadoresOnline() {
      // Act
      Integer count = managedProcess.getOnlinePlayersCount();

      // Assert
      assertThat(count).isNotNull();
    }

    @Test
    @DisplayName("getMetrics delega ao metricsCollector")
    void deveDelegarGetMetricsAoCollector() {
      // Arrange
      ServerMetricsDTO expectedMetrics = mock(ServerMetricsDTO.class);
      when(metricsCollector.collect()).thenReturn(expectedMetrics);

      // Act
      ServerMetricsDTO result = managedProcess.getMetrics();

      // Assert
      assertThat(result).isSameAs(expectedMetrics);
    }
  }

  @Nested
  @DisplayName("start()")
  class StartTests {

    @Test
    @DisplayName("deve iniciar servidor com sucesso de OFFLINE para ONLINE")
    void deveIniciarServidorComSucesso() {
      // Act
      managedProcess.start();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.ONLINE);
      assertThat(managedProcess.getStartedAt()).isNotNull();

      // Assert
      verify(consoleGateway).attach(any(ServerConsoleLineProcessor.class));
      verify(template, times(2)).convertAndSend(anyString(), any(ServerStatus.class));
      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("deve ignorar start quando já está ONLINE")
    void deveIgnorarStartQuandoJaOnline() {
      // Arrange
      managedProcess.start();

      // Act
      managedProcess.start();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.ONLINE);
      verify(lifecycle, times(1)).start();
    }

    @Test
    @DisplayName("deve ignorar start quando já está STARTING")
    void deveIgnorarStartQuandoJaStarting() {
      // Arrange
      managedProcess.setStatusForTest(ServerStatus.STARTING);

      // Act
      managedProcess.start();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.STARTING);
      verify(lifecycle, never()).start();
    }

    @Test
    @DisplayName("deve rollback para OFFLINE quando lifecycle.start lança exceção")
    void deveRollbackParaOfflineQuandoLifecycleFalhar() {
      // Arrange
      doThrow(new UncheckedIOException("boom", new IOException())).when(lifecycle).start();

      // Act
      managedProcess.start();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      assertThat(managedProcess.getStartedAt()).isNull();

      verify(template, atLeast(2)).convertAndSend(anyString(), any(ServerStatus.class));
    }
  }

  @Nested
  @DisplayName("stop() e stopThen()")
  class StopTests {

    @Test
    @DisplayName("deve parar servidor rodando com sucesso")
    void devePararServidorRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.stop();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      assertThat(managedProcess.getStartedAt()).isNull();

      verify(consoleGateway).write("stop");
      verify(lifecycle).stop(60);
      verify(consoleGateway).detach();
      verify(template, times(2)).convertAndSend(anyString(), any(ServerStatus.class));
    }

    @Test
    @DisplayName("deve ignorar stop quando servidor não está rodando")
    void deveIgnorarStopQuandoNaoRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act
      managedProcess.stop();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      verify(consoleGateway, never()).write(anyString());
      verify(lifecycle, never()).stop(anyInt());

      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("deve executar onStopped após parar quando servidor rodando")
    void deveExecutarOnStoppedAposPararQuandoRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      AtomicBoolean executed = new AtomicBoolean(false);

      // Act
      managedProcess.stopThen(() -> executed.set(true));

      // Assert
      assertThat(executed.get()).isTrue();
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("deve executar onStopped imediatamente quando servidor não rodando")
    void deveExecutarOnStoppedImediatamenteQuandoNaoRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);
      AtomicBoolean executed = new AtomicBoolean(false);

      // Act
      managedProcess.stopThen(() -> executed.set(true));

      // Assert
      assertThat(executed.get()).isTrue();
      verify(consoleGateway, never()).write(anyString());
    }
  }

  @Nested
  @DisplayName("restart()")
  class RestartTests {

    @Test
    @DisplayName("deve reiniciar servidor rodando com sucesso")
    void deveReiniciarServidorRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.restart();

      // Assert
      verify(consoleGateway).write("stop");
      verify(lifecycle).stop(60);
      verify(consoleGateway).detach();

      verify(lifecycle).start();
      verify(consoleGateway).attach(any(ServerConsoleLineProcessor.class));
    }

    @Test
    @DisplayName("deve ignorar restart quando servidor não está rodando")
    void deveIgnorarRestartQuandoNaoRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act
      managedProcess.restart();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      verify(lifecycle, never()).stop(anyInt());
      verify(lifecycle, never()).start();
    }
  }

  @Nested
  @DisplayName("sendCommand()")
  class SendCommandTests {

    @Test
    @DisplayName("deve enviar comando quando servidor rodando")
    void deveEnviarComandoQuandoRodando() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.sendCommand("op Steve");

      // Assert
      verify(consoleGateway).write("op Steve");
    }

    @Test
    @DisplayName("deve ignorar comando quando servidor parado")
    void deveIgnorarComandoQuandoParado() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act
      managedProcess.sendCommand("op Steve");

      // Assert
      verify(consoleGateway, never()).write(anyString());
      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("deve capturar exceção quando consoleGateway.write falhar")
    void deveCapturarExcecaoQuandoWriteFalhar() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);
      doThrow(new UncheckedIOException("conexão perdida", new IOException())).when(consoleGateway).write(anyString());

      // Act
      managedProcess.sendCommand("op Steve");

      // Assert
      verify(consoleGateway).write("op Steve");
      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }
  }

  @Nested
  @DisplayName("Comandos de jogador")
  class PlayerCommandTests {

    @Test
    @DisplayName("deve chamar playerService e enviar comando ao opPlayer")
    void deveChamarPlayerServiceAoOpPlayer() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.opPlayer("Steve");

      // Assert
      verify(playerService).opPlayer(1L, "Steve");
      verify(consoleGateway).write("op Steve");
    }

    @Test
    @DisplayName("deve chamar playerService e enviar comando ao deopPlayer")
    void deveChamarPlayerServiceAoDeopPlayer() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.deopPlayer("Steve");

      // Assert
      verify(playerService).deopPlayer(1L, "Steve");
      verify(consoleGateway).write("deop Steve");
    }

    @Test
    @DisplayName("deve chamar playerService e enviar comando ao banPlayer")
    void deveChamarPlayerServiceAoBanPlayer() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.banPlayer("Steve");

      // Assert
      verify(playerService).banPlayer(1L, "Steve");
      verify(consoleGateway).write("ban Steve");
    }

    @Test
    @DisplayName("deve chamar playerService e enviar pardon ao unbanPlayer")
    void deveChamarPlayerServiceAoUnbanPlayer() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(true);

      // Act
      managedProcess.unbanPlayer("Steve");

      // Assert
      verify(playerService).unbanPlayer(1L, "Steve");
      verify(consoleGateway).write("pardon Steve");
    }

    @Test
    @DisplayName("deve chamar playerService mas não enviar comando quando servidor parado")
    void deveChamarPlayerServiceMasNaoEnviarComandoQuandoParado() {
      // Arrange
      when(lifecycle.isRunning()).thenReturn(false);

      // Act
      managedProcess.opPlayer("Steve");

      // Assert
      verify(playerService).opPlayer(1L, "Steve");
      verify(consoleGateway, never()).write(anyString());
    }
  }

  @Nested
  @DisplayName("handleUnexpectedExit()")
  class UnexpectedExitTests {

    @Test
    @DisplayName("deve tratar saída inesperada de ONLINE para OFFLINE")
    void deveTratarSaidaInesperadaDeOnline() {
      // Arrange
      managedProcess.start();
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.ONLINE);

      // Act
      onExitCallback.run();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      assertThat(managedProcess.getStartedAt()).isNull();

      verify(template, times(2)).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("deve tratar saída inesperada de STARTING para OFFLINE")
    void deveTratarSaidaInesperadaDeStarting() {
      // Arrange
      managedProcess.setStatusForTest(ServerStatus.STARTING);

      // Act
      onExitCallback.run();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      assertThat(managedProcess.getStartedAt()).isNull();

      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("não deve fazer nada quando já está OFFLINE")
    void naoDeveFazerNadaQuandoOffline() {
      // Arrange
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);

      // Act
      onExitCallback.run();

      // Assert
      assertThat(managedProcess.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      verify(template, never()).convertAndSend(anyString(), any(LogLineDTO.class));
    }
  }
}