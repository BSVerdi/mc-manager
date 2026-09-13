package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerRuntimeServiceTest {

  private static final Long SERVER_ID = 1L;
  private static final Path SERVER_DIR = Path.of("/tmp/server-1");
  private static final String JAVA_8_PATH = "/usr/lib/jvm/java-8/bin/java";
  private static final String JAVA_21_PATH = "/usr/lib/jvm/java-21/bin/java";
  private static final String JAVA_25_PATH = "/usr/lib/jvm/java-25/bin/java";

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private ServerFilesService serverFilesService;

  @Mock
  private ManagedServerProcessFactory processFactory;

  @Mock
  private ManagedServerProcess managedServerProcess;

  @Mock
  private Server serverConfig;

  @InjectMocks
  private ServerRuntimeService serverRuntimeService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(serverRuntimeService, "java8", JAVA_8_PATH);
    ReflectionTestUtils.setField(serverRuntimeService, "java21", JAVA_21_PATH);
    ReflectionTestUtils.setField(serverRuntimeService, "java25", JAVA_25_PATH);
  }

  @SuppressWarnings("unchecked")
  private void putInstanceInMap(Long id, ManagedServerProcess instance) {
    Map<Long, ManagedServerProcess> instances =
            (Map<Long, ManagedServerProcess>) ReflectionTestUtils.getField(serverRuntimeService, "instances");
    instances.put(id, instance);
  }

  private void stubGetOrCreateChain() {
    when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.of(serverConfig));
    when(serverFilesService.getServerDirectory(SERVER_ID)).thenReturn(SERVER_DIR);
    when(processFactory.create(any(Server.class), anyString(), any(Path.class)))
            .thenReturn(managedServerProcess);
  }

  @Nested
  @DisplayName("start")
  class StartTests {

    @Test
    @DisplayName("deveIniciarInstanciaQuandoServidorExisteNoRepository")
    void deveIniciarInstanciaQuandoServidorExisteNoRepository() {
      // Arrange
      when(serverConfig.getJavaVersion()).thenReturn("JAVA_21");
      stubGetOrCreateChain();

      // Act
      serverRuntimeService.start(SERVER_ID);

      // Assert
      verify(managedServerProcess).start();
    }

    @Test
    @DisplayName("deverLancarExcecaoQuandoServidorNaoEncontrado")
    void deverLancarExcecaoQuandoServidorNaoEncontrado() {
      // Arrange
      when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.empty());

      // Act & Assert
      IllegalStateException ex = assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.start(SERVER_ID));
      assertThat(ex.getMessage()).contains("não encontrado");
    }

    @Test
    @DisplayName("deverUsarCaminhoJava8QuandoVersaoEhJava8")
    void deverUsarCaminhoJava8QuandoVersaoEhJava8() {
      // Arrange
      when(serverConfig.getJavaVersion()).thenReturn("JAVA_8");
      stubGetOrCreateChain();

      // Act
      serverRuntimeService.start(SERVER_ID);

      // Assert
      verify(processFactory).create(eq(serverConfig), eq(JAVA_8_PATH), eq(SERVER_DIR));
    }

    @Test
    @DisplayName("deverUsarCaminhoJava25PorPadraoQuandoVersaoDesconhecida")
    void deverUsarCaminhoJava25PorPadraoQuandoVersaoDesconhecida() {
      // Arrange
      when(serverConfig.getJavaVersion()).thenReturn("JAVA_99");
      stubGetOrCreateChain();

      // Act
      serverRuntimeService.start(SERVER_ID);

      // Assert
      verify(processFactory).create(eq(serverConfig), eq(JAVA_25_PATH), eq(SERVER_DIR));
    }

    @Test
    @DisplayName("naoDeverCriarNovaInstanciaQuandoJaExisteNoMapa")
    void naoDeverCriarNovaInstanciaQuandoJaExisteNoMapa() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.start(SERVER_ID);

      // Assert
      verify(managedServerProcess).start();
      verifyNoInteractions(processFactory);
    }
  }

  @Nested
  @DisplayName("stop")
  class StopTests {

    @Test
    @DisplayName("deverPararInstanciaQuandoExisteNoMapa")
    void deverPararInstanciaQuandoExisteNoMapa() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.stop(SERVER_ID);

      // Assert
      verify(managedServerProcess).stop();
    }

    @Test
    @DisplayName("deverLancarExcecaoQuandoServidorNaoEncontrado")
    void deverLancarExcecaoQuandoServidorNaoEncontrado() {
      // Arrange
      when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(IllegalStateException.class, () -> serverRuntimeService.stop(SERVER_ID));
    }
  }

  @Nested
  @DisplayName("restart")
  class RestartTests {

    @Test
    @DisplayName("deverReiniciarInstanciaQuandoExisteNoMapa")
    void deverReiniciarInstanciaQuandoExisteNoMapa() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.restart(SERVER_ID);

      // Assert
      verify(managedServerProcess).restart();
    }

    @Test
    @DisplayName("deverLancarExcecaoQuandoServidorNaoEncontrado")
    void deverLancarExcecaoQuandoServidorNaoEncontrado() {
      // Arrange
      when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(IllegalStateException.class, () -> serverRuntimeService.restart(SERVER_ID));
    }
  }

  @Nested
  @DisplayName("Comandos de Jogador")
  class PlayerCommandTests {

    @Test
    @DisplayName("sendCommand - deveExecutarQuandoInstanciaExiste")
    void sendCommand_deveExecutarQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.sendCommand(SERVER_ID, "say hello");

      // Assert
      verify(managedServerProcess).sendCommand("say hello");
    }

    @Test
    @DisplayName("sendCommand - deveLancarExcecaoQuandoServidorFechado")
    void sendCommand_deverLancarExcecaoQuandoServidorFechado() {
      // Act & Assert
      IllegalStateException ex = assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.sendCommand(SERVER_ID, "say hello"));
      assertThat(ex.getMessage()).contains("Não é possível realizar a ação em um servidor fechado");
    }

    @Test
    @DisplayName("opPlayer - deveConcederOPQuandoInstanciaExiste")
    void opPlayer_deveConcederOPQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.opPlayer(SERVER_ID, "Steve");

      // Assert
      verify(managedServerProcess).opPlayer("Steve");
    }

    @Test
    @DisplayName("opPlayer - deveLancarExcecaoQuandoServidorFechado")
    void opPlayer_deverLancarExcecaoQuandoServidorFechado() {
      // Act & Assert
      assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.opPlayer(SERVER_ID, "Steve"));
    }

    @Test
    @DisplayName("deopPlayer - deveRevogarOPQuandoInstanciaExiste")
    void deopPlayer_deveRevogarOPQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.deopPlayer(SERVER_ID, "Steve");

      // Assert
      verify(managedServerProcess).deopPlayer("Steve");
    }

    @Test
    @DisplayName("deopPlayer - deveLancarExcecaoQuandoServidorFechado")
    void deopPlayer_deverLancarExcecaoQuandoServidorFechado() {
      // Act & Assert
      assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.deopPlayer(SERVER_ID, "Steve"));
    }

    @Test
    @DisplayName("banPlayer - deveBanirJogadorQuandoInstanciaExiste")
    void banPlayer_deveBanirJogadorQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.banPlayer(SERVER_ID, "Griefer");

      // Assert
      verify(managedServerProcess).banPlayer("Griefer");
    }

    @Test
    @DisplayName("banPlayer - deveLancarExcecaoQuandoServidorFechado")
    void banPlayer_deverLancarExcecaoQuandoServidorFechado() {
      // Act & Assert
      assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.banPlayer(SERVER_ID, "Griefer"));
    }

    @Test
    @DisplayName("unbanPlayer - deveDesbanirJogadorQuandoInstanciaExiste")
    void unbanPlayer_deveDesbanirJogadorQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.unbanPlayer(SERVER_ID, "Griefer");

      // Assert
      verify(managedServerProcess).unbanPlayer("Griefer");
    }

    @Test
    @DisplayName("unbanPlayer - deveLancarExcecaoQuandoServidorFechado")
    void unbanPlayer_deverLancarExcecaoQuandoServidorFechado() {
      // Act & Assert
      assertThrows(IllegalStateException.class,
              () -> serverRuntimeService.unbanPlayer(SERVER_ID, "Griefer"));
    }
  }

  @Nested
  @DisplayName("getStatus")
  class GetStatusTests {

    @Test
    @DisplayName("deverRetornarStatusQuandoInstanciaExiste")
    void deverRetornarStatusQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      when(managedServerProcess.getStatus()).thenReturn(ServerStatus.ONLINE);

      // Act
      ServerStatus status = serverRuntimeService.getStatus(SERVER_ID);

      // Assert
      assertThat(status).isEqualTo(ServerStatus.ONLINE);
    }

    @Test
    @DisplayName("deverRetornarOFFLINEQuandoInstanciaNaoExiste")
    void deverRetornarOFFLINEQuandoInstanciaNaoExiste() {
      // Act
      ServerStatus status = serverRuntimeService.getStatus(SERVER_ID);

      // Assert
      assertThat(status).isEqualTo(ServerStatus.OFFLINE);
    }
  }

  @Nested
  @DisplayName("getOnlinePlayersCount")
  class GetOnlinePlayersCountTests {

    @Test
    @DisplayName("deverRetornarContagemQuandoInstanciaExiste")
    void deverRetornarContagemQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      when(managedServerProcess.getOnlinePlayersCount()).thenReturn(42);

      // Act
      Integer count = serverRuntimeService.getOnlinePlayersCount(SERVER_ID);

      // Assert
      assertThat(count).isEqualTo(42);
    }

    @Test
    @DisplayName("deverRetornarZeroQuandoInstanciaNaoExiste")
    void deverRetornarZeroQuandoInstanciaNaoExiste() {
      // Act
      Integer count = serverRuntimeService.getOnlinePlayersCount(SERVER_ID);

      // Assert
      assertThat(count).isZero();
    }
  }

  @Nested
  @DisplayName("getMetrics")
  class GetMetricsTests {

    @Test
    @DisplayName("deverRetornarMetricsQuandoInstanciaExiste")
    void deverRetornarMetricsQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      ServerMetricsDTO expectedDto = mock(ServerMetricsDTO.class);
      when(managedServerProcess.getMetrics()).thenReturn(expectedDto);

      // Act
      ServerMetricsDTO result = serverRuntimeService.getMetrics(SERVER_ID);

      // Assert
      assertThat(result).isSameAs(expectedDto);
    }

    @Test
    @DisplayName("deverRetornarNuloQuandoInstanciaNaoExiste")
    void deverRetornarNuloQuandoInstanciaNaoExiste() {
      // Act
      ServerMetricsDTO result = serverRuntimeService.getMetrics(SERVER_ID);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("getMaxRamMb")
  class GetMaxRamMbTests {

    @Test
    @DisplayName("deverRetornarMaxRamMbQuandoInstanciaExiste")
    void deverRetornarMaxRamMbQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      when(managedServerProcess.getMaxRamMb()).thenReturn(2048L);

      // Act
      Long maxRam = serverRuntimeService.getMaxRamMb(SERVER_ID);

      // Assert
      assertThat(maxRam).isEqualTo(2048L);
    }

    @Test
    @DisplayName("deverRetornarZeroQuandoInstanciaNaoExiste")
    void deverRetornarZeroQuandoInstanciaNaoExiste() {
      // Act
      Long maxRam = serverRuntimeService.getMaxRamMb(SERVER_ID);

      // Assert
      assertThat(maxRam).isZero();
    }
  }

  @Nested
  @DisplayName("getStartedAt")
  class GetStartedAtTests {

    @Test
    @DisplayName("deverRetornarStartedAtQuandoInstanciaExiste")
    void deverRetornarStartedAtQuandoInstanciaExiste() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      Instant fixedInstant = Instant.parse("2025-01-15T10:30:00Z");
      when(managedServerProcess.getStartedAt()).thenReturn(fixedInstant);

      // Act
      Instant result = serverRuntimeService.getStartedAt(SERVER_ID);

      // Assert
      assertThat(result).isSameAs(fixedInstant);
    }

    @Test
    @DisplayName("deverRetornarInstantAtoMomentoQuandoInstanciaNaoExiste")
    void deverRetornarInstantAtoMomentoQuandoInstanciaNaoExiste() {
      // Act
      Instant result = serverRuntimeService.getStartedAt(SERVER_ID);

      // Assert
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("discard")
  class DiscardTests {

    @Test
    @DisplayName("deverRemoverInstanciaDoMapa")
    void deverRemoverInstanciaDoMapa() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);

      // Act
      serverRuntimeService.discard(SERVER_ID);

      // Assert
      assertThat(serverRuntimeService.getStatus(SERVER_ID)).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("naoDeverLancarExcecaoQuandoInstanciaNaoExiste")
    void naoDeverLancarExcecaoQuandoInstanciaNaoExiste() {
      assertDoesNotThrow(() -> serverRuntimeService.discard(SERVER_ID));
    }
  }

  @Nested
  @DisplayName("getActiveServersIds")
  class GetActiveServersIdsTests {

    @Test
    @DisplayName("deverRetornarApenasIdsDeServidoresEmExecucao")
    void deverRetornarApenasIdsDeServidoresEmExecucao() {
      // Arrange
      ManagedServerProcess runningInstance = mock(ManagedServerProcess.class);
      ManagedServerProcess stoppedInstance = mock(ManagedServerProcess.class);
      when(runningInstance.isRunning()).thenReturn(true);
      when(stoppedInstance.isRunning()).thenReturn(false);
      putInstanceInMap(1L, runningInstance);
      putInstanceInMap(2L, stoppedInstance);

      // Act
      Set<Long> activeIds = serverRuntimeService.getActiveServersIds();

      // Assert
      assertThat(activeIds).containsExactly(1L);
    }

    @Test
    @DisplayName("deverRetornarVazioQuandoNaoHaInstances")
    void deverRetornarVazioQuandoNaoHaInstances() {
      // Act
      Set<Long> activeIds = serverRuntimeService.getActiveServersIds();

      // Assert
      assertThat(activeIds).isEmpty();
    }
  }

  @Nested
  @DisplayName("restartWithUpdatedConfig")
  class RestartWithUpdatedConfigTests {

    @Test
    @DisplayName("deverPararERecriarQuandoServidorEstiverRodando")
    void deverPararERecriarQuandoServidorEstiverRodando() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      when(managedServerProcess.isRunning()).thenReturn(true);
      when(serverConfig.getJavaVersion()).thenReturn("JAVA_21");
      stubGetOrCreateChain();

      // Act
      serverRuntimeService.restartWithUpdatedConfig(SERVER_ID);

      // Assert
      ArgumentCaptor<Runnable> onStoppedCaptor = ArgumentCaptor.forClass(Runnable.class);
      verify(managedServerProcess).stopThen(onStoppedCaptor.capture());

      onStoppedCaptor.getValue().run();

      verify(managedServerProcess).start();
    }

    @Test
    @DisplayName("naoDeverInteragirComInstanciaQuandoNaoExiste")
    void naoDeverInteragirComInstanciaQuandoNaoExiste() {
      // Act
      serverRuntimeService.restartWithUpdatedConfig(SERVER_ID);

      // Assert
      verifyNoInteractions(managedServerProcess);
    }

    @Test
    @DisplayName("naoDeverChamarStopThenQuandoServidorNaoEstiverRodando")
    void naoDeverChamarStopThenQuandoServidorNaoEstiverRodando() {
      // Arrange
      putInstanceInMap(SERVER_ID, managedServerProcess);
      when(managedServerProcess.isRunning()).thenReturn(false);

      // Act
      serverRuntimeService.restartWithUpdatedConfig(SERVER_ID);

      // Assert
      verify(managedServerProcess, never()).stopThen(any());
      assertThat(serverRuntimeService.getStatus(SERVER_ID)).isEqualTo(ServerStatus.OFFLINE);
    }
  }
}