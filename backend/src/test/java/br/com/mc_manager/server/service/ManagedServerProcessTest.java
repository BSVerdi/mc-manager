package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.LogLineDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithMaxRam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ManagedServerProcessTest {

  @TempDir
  private Path tempDir;

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private PlayerService playerService;

  @Mock
  private ExecutorService executor;

  @Nested
  @DisplayName("parseRamToMb (via construtor / getMaxRamMb)")
  class ParseRamToMbTest {

    @ParameterizedTest(name = "\"{0}\" deve resultar em {1} MB")
    @CsvSource({
            "4G, 4096",
            "1G, 1024",
            "512M, 512",
            "2048M, 2048",
            "1024, 1024",
            "8g, 8192",
            "256m, 256",
    })
    @DisplayName("Deve converter diferentes formatos de RAM para MB corretamente")
    void deveConverterFormatosDeRamParaMb(String ramConfigurada, long esperadoMb) {
      // Arrange
      Server server = createServerWithMaxRam(ramConfigurada);

      ManagedServerProcess process = new ManagedServerProcess(
              server, template, executor, "/usr/bin/java", tempDir, playerService
      );

      // Assert
      assertThat(process.getMaxRamMb()).isEqualTo(esperadoMb);
    }

    @Test
    @DisplayName("Deve lançar NumberFormatException para valor de RAM malformado")
    void deveLancarExcecaoParaRamMalformada() {
      // Arrange
      Server server = createServerWithMaxRam("abc");

      // Assert
      assertThatThrownBy(() ->
              new ManagedServerProcess(server, template, executor, "/usr/bin/java", tempDir, playerService)
      ).isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("Deve lançar NumberFormatException para RAM vazia")
    void deveLancarExcecaoParaRamVazia() {
      // Arrange
      Server server = createServerWithMaxRam("");

      // Assert
      assertThatThrownBy(() ->
              new ManagedServerProcess(server, template, executor, "/usr/bin/java", tempDir, playerService)
      ).isInstanceOf(NumberFormatException.class);
    }
  }

  @Nested
  @DisplayName("Estado inicial (antes de qualquer start)")
  class EstadoInicialTest {

    private ManagedServerProcess process;
    private Server server;

    @BeforeEach
    void setUp() {
      // Arrange
      server = createServerWithMaxRam("4G");
      process = new ManagedServerProcess(server, template, executor, "/usr/bin/java", tempDir, playerService);
    }

    @Test
    @DisplayName("Deve iniciar com status OFFLINE")
    void deveIniciarComStatusOffline() {
      // Assert
      assertThat(process.getStatus()).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("Deve iniciar sem PID de processo")
    void deveIniciarSemPid() {
      // Assert
      assertThat(process.getProcessPid()).isNull();
    }

    @Test
    @DisplayName("Deve iniciar sem horário de início")
    void deveIniciarSemStartedAt() {
      // Assert
      assertThat(process.getStartedAt()).isNull();
    }

    @Test
    @DisplayName("Deve iniciar com 0 jogadores online")
    void deveIniciarComZeroJogadoresOnline() {
      // Assert
      assertThat(process.getOnlinePlayersCount()).isZero();
    }

    @Test
    @DisplayName("Deve expor o ID do servidor corretamente")
    void deveExporIdDoServidorCorretamente() {
      // Assert
      assertThat(process.getServerId()).isEqualTo(server.getId());
    }
  }

  @Nested
  @DisplayName("Ações quando o servidor está offline (sem processo real)")
  class AcoesQuandoOfflineTest {

    private ManagedServerProcess process;

    @BeforeEach
    void setUp() {
      // Arrange
      Server server = createServerWithMaxRam("4G");
      process = new ManagedServerProcess(server, template, executor, "/usr/bin/java", tempDir, playerService);
    }

    @Test
    @DisplayName("stop() quando offline não deve lançar exceção nem chamar playerService")
    void stopQuandoOfflineNaoLancaExcecaoNemChamaPlayerService() {
      // Act & Assert
      assertThatCode(() -> process.stop())
              .doesNotThrowAnyException();

      verifyNoInteractions(playerService);
    }

    @Test
    @DisplayName("stop() quando offline não altera o status")
    void stopQuandoOfflineNaoAlteraStatus() {
      // Act
      process.stop();

      // Assert
      assertThat(process.getStatus()).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("restart() quando offline não deve lançar exceção (apenas loga e retorna)")
    void restartQuandoOfflineNaoLancaExcecao() {
      // Act & Assert
      assertThatCode(() -> process.restart())
              .doesNotThrowAnyException();

      assertThat(process.getStatus()).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("sendCommand() quando offline não deve lançar exceção")
    void sendCommandQuandoOfflineNaoLancaExcecao() {
      //Act & Assert
      assertThatCode(() -> process.sendCommand("stop"))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendCommand() quando offline não interage com o template de console")
    void sendCommandQuandoOfflineNaoEnviaLinhaDeConsoleComoComando() {
      // Act
      process.sendCommand("stop");

      // Assert
      verify(template).convertAndSend(anyString(), any(LogLineDTO.class));
    }

    @Test
    @DisplayName("opPlayer() quando offline ainda delega ao playerService antes de tentar enviar comando")
    void opPlayerQuandoOfflineAindaDelegaAoPlayerService() {
      // Act
      process.opPlayer("Steve");

      // Assert
      verify(playerService).opPlayer(process.getServerId(), "Steve");
    }

    @Test
    @DisplayName("deOpPlayer() quando offline ainda delega ao playerService")
    void deOpPlayerQuandoOfflineAindaDelegaAoPlayerService() {
      // Act
      process.deOpPlayer("Steve");

      // Assert
      verify(playerService).deopPlayer(process.getServerId(), "Steve");
    }

    @Test
    @DisplayName("banPlayer() quando offline ainda delega ao playerService")
    void banPlayerQuandoOfflineAindaDelegaAoPlayerService() {
      // Act
      process.banPlayer("Steve");

      // Assert
      verify(playerService).banPlayer(process.getServerId(), "Steve");
    }

    @Test
    @DisplayName("unbanPlayer() quando offline ainda delega ao playerService")
    void unbanPlayerQuandoOfflineAindaDelegaAoPlayerService() {
      // Act
      process.unbanPlayer("Steve");

      // Assert
      verify(playerService).unbanPlayer(process.getServerId(), "Steve");
    }

    @Test
    @DisplayName("stopThen() quando offline executa o callback imediatamente, de forma síncrona")
    void stopThenQuandoOfflineExecutaCallbackImediatamente() {
      // Arrange
      var callbackExecutado = new java.util.concurrent.atomic.AtomicBoolean(false);

      // Act
      process.stopThen(() -> callbackExecutado.set(true));

      // Assert
      assertThat(callbackExecutado.get()).isTrue();
      verifyNoInteractions(executor);
    }
  }

  @Nested
  @DisplayName("start() com falha real de processo (sem exigir que fique vivo)")
  class StartComFalhaTest {

    @Test
    @DisplayName("Deve mudar status para OFFLINE quando o executável Java não existe")
    void deveMudarParaOfflineQuandoExecutavelNaoExiste() {
      // Arrange
      Server server = createServerWithMaxRam("4G");
      ManagedServerProcess process = new ManagedServerProcess(
              server, template, executor, "/caminho/definitivamente/inexistente/java", tempDir, playerService
      );

      // Act & Assert
      assertThatCode(process::start)
              .doesNotThrowAnyException();

      assertThat(process.getStatus()).isEqualTo(ServerStatus.OFFLINE);
      assertThat(process.getProcessPid()).isNull();
    }

    @Test
    @DisplayName("Não deve submeter tarefas ao executor quando o processo falha ao iniciar")
    void naoDeveSubmeterTarefasAoExecutorQuandoFalha() {
      // Arrange
      Server server = createServerWithMaxRam("4G");
      ManagedServerProcess process = new ManagedServerProcess(
              server, template, executor, "/caminho/definitivamente/inexistente/java", tempDir, playerService
      );

      // Act
      process.start();

      // Assert
      verifyNoInteractions(executor);
    }
  }
}