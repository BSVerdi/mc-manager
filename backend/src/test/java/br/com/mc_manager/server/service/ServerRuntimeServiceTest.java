package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ManagedServerProcessFactory;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static br.com.mc_manager.server.factory.ServerFactory.createServerWithId;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerRuntimeServiceTest {

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private PlayerService playerService;

  @Mock
  private ManagedServerProcessFactory processFactory;

  @Mock
  private ServerFilesService serverFilesService;

  @InjectMocks
  private ServerRuntimeService runtimeService;

  @Nested
  @DisplayName("Start / Stop / Restart")
  class TesteGerenciamentoAcao {

    private Path serverDirectory;
    private Server serverConfig;
    private Long serverId;
    private ManagedServerProcess mockProcess;

    @BeforeEach
    void setUp() {
      this.serverDirectory = Path.of("/test/");
      this.serverConfig = createServerWithAllAttributes();
      this.serverId = this.serverConfig.getId();
      this.mockProcess = mock(ManagedServerProcess.class);

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverConfig));
      when(processFactory.create(eq(serverConfig), any(), eq(serverDirectory))).thenReturn(mockProcess);
      when(serverFilesService.getServerDirectory(anyLong())).thenReturn(serverDirectory);
    }

    @Test
    @DisplayName("Deve iniciar uma instância de servidor")
    void deveIniciarUmaInstanciaDeServidor() {
      // Act
      runtimeService.start(serverId);

      // Assert
      verify(mockProcess, times(1)).start();
    }

    @Test
    @DisplayName("Deve parar uma instância já existente")
    void devePararUmaInstanciaJaExistente() {
      // Arrange - instância já iniciada previamente
      runtimeService.start(serverId);

      // Act
      runtimeService.stop(serverId);

      // Assert - mesma instância recebeu start e stop, factory chamada só uma vez
      verify(mockProcess, times(1)).stop();
      verify(processFactory, times(1)).create(eq(serverConfig), any(), eq(serverDirectory));
    }

    @Test
    @DisplayName("Deve reiniciar uma instância já existente")
    void deveReiniciarUmaInstanciaJaExistente() {
      // Arrange - instância já iniciada previamente
      when(serverFilesService.getServerDirectory(anyLong())).thenReturn(Path.of("/test/"));
      runtimeService.start(serverId);

      // Act
      runtimeService.restart(serverId);

      // Assert
      verify(mockProcess, times(1)).restart();
      verify(processFactory, times(1)).create(eq(serverConfig), any(), eq(serverDirectory));
    }
  }

  @Nested
  @DisplayName("Comandos e Ações em Servidor")
  class TesteComandos {

    private ManagedServerProcess mockProcess;
    private Path serverDirectory;
    private Server serverConfig;
    private Long serverId;

    @BeforeEach
    void setUp() {
      this.mockProcess = mock(ManagedServerProcess.class);
      this.serverDirectory = Path.of("/test/");
      this.serverConfig = createServerWithAllAttributes();
      this.serverId = this.serverConfig.getId();


      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverConfig));
      when(serverFilesService.getServerDirectory(anyLong())).thenReturn(serverDirectory);
      when(processFactory.create(eq(serverConfig), any(), eq(serverDirectory))).thenReturn(mockProcess);

      runtimeService.start(serverId);
    }

    @Test
    @DisplayName("Deve enviar um comando ao servidor")
    void deveEnviarUmComandoAoServidor() {
      String command = "/gamemode creative player";

      runtimeService.sendCommand(serverId, command);

      verify(mockProcess, times(1)).sendCommand(command);
    }

    @Test
    @DisplayName("Deve conceder OP para um jogador")
    void deveConcederOPParaUmJogador() {
      String nickname = "Steve";

      runtimeService.opPlayer(serverId, nickname);

      verify(mockProcess, times(1)).opPlayer(nickname);
    }

    @Test
    @DisplayName("Deve revogar OP para um jogador")
    void deveRevogarOPParaUmJogador() {
      String nickname = "Alex";

      runtimeService.deopPlayer(serverId, nickname);

      verify(mockProcess, times(1)).deOpPlayer(nickname);
    }

    @Test
    @DisplayName("Deve banir um jogador")
    void deveBanirUmJogador() {
      String nickname = "Creeper";

      runtimeService.banPlayer(serverId, nickname);

      verify(mockProcess, times(1)).banPlayer(nickname);
    }

    @Test
    @DisplayName("Deve desbanir um jogador")
    void deveDesbanirUmJogador() {
      String nickname = "Obsidian";

      runtimeService.unbanPlayer(serverId, nickname);

      verify(mockProcess, times(1)).unbanPlayer(nickname);
    }
  }

  @Nested
  @DisplayName("Obtenção de Status do Servidor")
  class TesteStatus {

    private ManagedServerProcess mockProcess;
    private Path serverDirectory;
    private Server serverConfig;
    private Long serverId;

    @BeforeEach
    void setUp() {
      this.mockProcess = mock(ManagedServerProcess.class);
      this.serverDirectory = Path.of("/test/");
      this.serverConfig = createServerWithAllAttributes();
      this.serverId = this.serverConfig.getId();
    }

    private void iniciarServidorComMock() {
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverConfig));
      when(serverFilesService.getServerDirectory(anyLong())).thenReturn(serverDirectory);
      when(processFactory.create(eq(serverConfig), any(), eq(serverDirectory))).thenReturn(mockProcess);
      runtimeService.start(serverId);
    }

    @Test
    @DisplayName("Deve retornar o status do servidor já iniciado")
    void deveRetornarOStatusDoServidor() {
      iniciarServidorComMock();

      ServerStatus expectedStatus = ServerStatus.ONLINE;
      when(mockProcess.getStatus()).thenReturn(expectedStatus);

      ServerStatus status = runtimeService.getStatus(serverId);

      assertThat(status).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("Deve retornar OFFLINE para servidor nunca iniciado")
    void deveRetornarOfflineParaServidorNaoIniciado() {
      ServerStatus status = runtimeService.getStatus(serverId);

      assertThat(status).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("Deve retornar a contagem de jogadores online do servidor já iniciado")
    void deveRetornarAContagemDeJogadoresOnline() {
      iniciarServidorComMock();

      int expectedPlayers = 15;
      when(mockProcess.getOnlinePlayersCount()).thenReturn(expectedPlayers);

      Integer count = runtimeService.getOnlinePlayersCount(serverId);

      assertThat(count).isEqualTo(expectedPlayers);
    }

    @Test
    @DisplayName("Deve retornar 0 jogadores online para servidor nunca iniciado")
    void deveRetornarZeroJogadoresOnlineParaServidorNaoIniciado() {
      Integer count = runtimeService.getOnlinePlayersCount(serverId);

      assertThat(count).isZero();
    }

    @Test
    @DisplayName("Deve retornar o PID do processo do servidor já iniciado")
    void deveRetornarOPidDoProcesso() {
      iniciarServidorComMock();

      Long expectedPid = 12345L;
      when(mockProcess.getProcessPid()).thenReturn(expectedPid);

      Long pid = runtimeService.getProcessPid(serverId);

      assertThat(pid).isEqualTo(expectedPid);
    }

    @Test
    @DisplayName("Deve retornar null para PID quando servidor nunca foi iniciado")
    void deveRetornarNullParaPIDQuandoServidorOffline() {
      Long pid = runtimeService.getProcessPid(serverId);

      assertThat(pid).isNull();
    }

    @Test
    @DisplayName("Deve retornar o tempo de início do servidor já iniciado")
    void deveRetornarOTempoDeInicioDoServidor() {
      iniciarServidorComMock();

      Instant expectedStartedAt = Instant.now();
      when(mockProcess.getStartedAt()).thenReturn(expectedStartedAt);

      Instant startedAt = runtimeService.getStartedAt(serverId);

      assertThat(startedAt).isEqualTo(expectedStartedAt);
    }

    @Test
    @DisplayName("Deve retornar null para tempo de início quando servidor nunca foi iniciado")
    void deveRetornarNullParaTempoDeInicioQuandoServidorOffline() {
      Instant startedAt = runtimeService.getStartedAt(serverId);

      assertThat(startedAt).isNull();
    }

    @Test
    @DisplayName("Deve retornar 0 para RAM máxima quando servidor nunca foi iniciado")
    void deveRetornarZeroParaRAMMaximaQuandoServidorOffline() {
      Long ramMb = runtimeService.getMaxRamMb(serverId);

      assertThat(ramMb).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar a RAM máxima do servidor já iniciado")
    void deveRetornarRAMMaximaDoServidorIniciado() {
      iniciarServidorComMock();

      Long expectedRam = 4096L;
      when(mockProcess.getMaxRamMb()).thenReturn(expectedRam);

      Long ramMb = runtimeService.getMaxRamMb(serverId);

      assertThat(ramMb).isEqualTo(expectedRam);
    }
  }

  @Nested
  @DisplayName("Reinicialização com Configuração Atualizada")
  class TesteReinicializacaoComConfigAtualizada {

    private final Long serverId = 1L;
    private final Server serverConfig = createServerWithId(1L);
    private final Path serverDirectory = Path.of("/test/");

    @Test
    @DisplayName("Deve chamar stopThen quando servidor está rodando (com PID)")
    void deveChamarStopThenQuandoServidorEstaRodando() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverConfig));
      ManagedServerProcess mockProcess = mock(ManagedServerProcess.class);
      when(serverFilesService.getServerDirectory(any())).thenReturn(serverDirectory);
      when(processFactory.create(eq(serverConfig), any(), eq(serverDirectory))).thenReturn(mockProcess);
      when(mockProcess.getProcessPid()).thenReturn(12345L); // processo rodando

      runtimeService.start(serverId); // popula o mapa com o mockProcess

      // Act
      runtimeService.restartWithUpdatedConfig(serverId);

      // Assert — stopThen é chamado com o callback de discard + start
      verify(mockProcess, times(1)).stopThen(any());
    }

    @Test
    @DisplayName("Não deve lançar exceção ao chamar restartWithUpdatedConfig em servidor nunca iniciado")
    void naoDeveLancarExcecaoSeServidorNaoExistir() {
      // Arrange - nenhuma instância populada, nenhum stub de repository necessário
      // (getExistingInstance não consulta o repositório)

      // Act & Assert
      assertThatCode(() -> runtimeService.restartWithUpdatedConfig(serverId))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve descartar a instância quando o processo não tem PID (offline)")
    void deveDescartarInstanciaQuandoProcessoSemPid() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverConfig));
      ManagedServerProcess mockProcess = mock(ManagedServerProcess.class);
      when(serverFilesService.getServerDirectory(any())).thenReturn(serverDirectory);
      when(processFactory.create(eq(serverConfig), any(), eq(serverDirectory))).thenReturn(mockProcess);
      when(mockProcess.getProcessPid()).thenReturn(null); // simula processo sem PID (offline)

      runtimeService.start(serverId); // popula o mapa com esse mockProcess

      // Act
      runtimeService.restartWithUpdatedConfig(serverId);

      // Assert — efeito observável do discard: a instância não existe mais no mapa,
      // então qualquer ação nela lança IllegalStateException
      assertThatThrownBy(() -> runtimeService.sendCommand(serverId, "/test"))
              .isInstanceOf(IllegalStateException.class);

      // E stopThen nunca deveria ter sido chamado, já que o processo estava sem PID
      verify(mockProcess, never()).stopThen(any());
    }
  }

  @Nested
  @DisplayName("Isolamento entre Testes")
  class TesteIsolamento {

    private final Long serverId1 = 1L;
    private final Long serverId2 = 2L;
    private final Server config1 = createServerWithId(1L);
    private final Server config2 = createServerWithId(2L);

    @Test
    @DisplayName("Deve criar instâncias independentes para diferentes servidores")
    void deveCriarInstanciasIndependentesParaDiferentesServidores() {
      // Arrange
      when(serverRepository.findById(1L)).thenReturn(Optional.of(config1));
      when(serverRepository.findById(2L)).thenReturn(Optional.of(config2));

      ManagedServerProcess process1 = mock(ManagedServerProcess.class);
      ManagedServerProcess process2 = mock(ManagedServerProcess.class);

      when(processFactory.create(any(Server.class), any(), any()))
              .thenReturn(process1, process2);

      when(process1.getProcessPid()).thenReturn(111L);
      when(process2.getProcessPid()).thenReturn(222L);

      // Act
      runtimeService.start(serverId1);
      runtimeService.start(serverId2);

      // Assert
      assertThat(runtimeService.getActiveServersIds()).containsExactlyInAnyOrder(serverId1, serverId2);
      verify(process1, times(1)).start();
      verify(process2, times(1)).start();
    }

    @Test
    @DisplayName("Deve reutilizar a mesma instância ao chamar restart no mesmo ID")
    void deveReutilizarAMesmaInstanciaAoChamarRestartNoMesmoId() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(config1));
      ManagedServerProcess existingProcess = mock(ManagedServerProcess.class);
      when(processFactory.create(any(Server.class), any(), any())).thenReturn(existingProcess);

      // Act
      runtimeService.start(serverId1);
      runtimeService.restart(serverId1);

      // Assert - mesma instância recebeu start e restart, factory chamada só uma vez
      verify(existingProcess, times(1)).start();
      verify(existingProcess, times(1)).restart();
      verify(processFactory, times(1)).create(any(Server.class), any(), any());
    }

    @Test
    @DisplayName("Deve garantir que getOrCreateInstance crie apenas uma vez por ID")
    void deveGarantirQueGetOrCreateInstanceCrieApenasUmaVezPorID() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(config1));
      ManagedServerProcess process = mock(ManagedServerProcess.class);

      when(processFactory.create(any(Server.class), any(), any())).thenReturn(process);

      // Act — chama métodos que dependem da mesma instância múltiplas vezes
      runtimeService.start(serverId1);
      runtimeService.getStatus(serverId1);
      runtimeService.getOnlinePlayersCount(serverId1);
      runtimeService.getProcessPid(serverId1);

      // Assert — processFactory.create deve ser chamado apenas uma vez
      verify(processFactory, times(1)).create(any(Server.class), any(), any());
    }

    @Test
    @DisplayName("Deve garantir que discard remova a instância imediatamente")
    void deveGarantirQueDiscardRemovaAInstanciaImediatamente() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(config1));
      ManagedServerProcess process = mock(ManagedServerProcess.class);

      when(processFactory.create(any(Server.class), any(), any())).thenReturn(process);

      // Act
      runtimeService.start(serverId1);
      runtimeService.discard(serverId1);

      // Assert — após discard, ações no servidor lançam exceção de "servidor fechado"
      assertThatThrownBy(() -> runtimeService.sendCommand(serverId1, "/test"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("servidor fechado");
    }
  }

  @Nested
  @DisplayName("Edge Cases e Erros")
  class TesteCasosDeBorda {

    private final Long serverId = 999L; // servidor nunca iniciado

    @Test
    @DisplayName("Deve retornar OFFLINE ao consultar status de servidor que não existe")
    void deveRetornarOfflineAoConsultarStatusDeServidorQueNaoExiste() {
      // getStatus não lança exceção - tem fallback para OFFLINE
      ServerStatus status = runtimeService.getStatus(serverId);

      assertThat(status).isEqualTo(ServerStatus.OFFLINE);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao enviar comando para servidor que não existe")
    void deveLancarIllegalStateExceptionAoEnviarComandoParaServidorQueNaoExiste() {
      assertThatThrownBy(() -> runtimeService.sendCommand(serverId, "/test"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Não é possível realizar a ação em um servidor fechado");
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao conceder OP em servidor que não existe")
    void deveLancarIllegalStateExceptionAoOpEmServidorQueNaoExiste() {
      assertThatThrownBy(() -> runtimeService.opPlayer(serverId, "Steve"))
              .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao chamar start para servidor inexistente no repositório")
    void deveLancarIllegalStateExceptionAoIniciarServidorInexistenteNoRepositorio() {
      when(serverRepository.findById(serverId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> runtimeService.start(serverId))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("não encontrado");
    }
  }
}