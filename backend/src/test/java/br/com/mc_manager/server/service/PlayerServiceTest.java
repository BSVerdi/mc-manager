package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.model.Player;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerPlayer;
import br.com.mc_manager.server.repository.PlayerRepository;
import br.com.mc_manager.server.repository.ServerPlayerRepository;
import br.com.mc_manager.server.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

  @InjectMocks
  private PlayerService playerService;

  @Mock
  private PlayerRepository playerRepository;

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private ServerPlayerRepository serverPlayerRepository;

  private List<Server> mockServers;
  private List<Player> mockPlayers;
  private List<ServerPlayer> mockServerPlayers;

  @BeforeEach
  void setUp() {
    // Preparar objetos mocks para testes
    mockServers = new ArrayList<>();
    mockPlayers = new ArrayList<>();
    mockServerPlayers = new ArrayList<>();
  }

  @ParameterizedTest
  @CsvSource({
          "player1",
          "player2"
  })
  @DisplayName("Listagem com diferentes players")
  void listServerPlayers_comDiferentesNicks(String nickname) {
    // Arrange
    Player player = new Player(nickname, "https://mc-heads.net/avatar/" + nickname + "/48");
    mockPlayers.add(player);

    Server server = createServerWithAllAttributes();

    ServerPlayer serverPlayer = new ServerPlayer(server, player);
    mockServerPlayers.add(serverPlayer);

    when(serverPlayerRepository.findByServerId(anyLong())).thenReturn(mockServerPlayers);

    // Act
    List<PlayerDTO> players = playerService.listServerPlayers(1L);

    // Assert
    assertNotNull(players);
    assertThat(players.size()).isEqualTo(1);
    assertThat(players.get(0).nickname()).isEqualTo(player.getNickname());
  }

  @Nested
  @DisplayName("Operações de Listagem")
  class ListServerPlayersTests {

    @Test
    @DisplayName("listServerPlayers - Retorna lista vazia quando não há players")
    void listServerPlayers_semPlayers_retornaListaVazia() {
      // Arrange
      Long serverId = 1L;
      when(serverPlayerRepository.findByServerId(anyLong())).thenReturn(new ArrayList<>());

      // Act
      List<PlayerDTO> players = playerService.listServerPlayers(serverId);

      // Assert
      assertNotNull(players);
      assertThat(players).isEmpty();
    }

    @Test
    @DisplayName("listServerPlayers - Retorna lista completa de players")
    void listServerPlayers_retornaListaCompleta() {
      // Arrange
      Long serverId = 1L;

      Player player1 = new Player("Steve", "https://mc-heads.net/avatar/Steve/48");
      Player player2 = new Player("Alex", "https://mc-heads.net/avatar/Alex/48");
      mockPlayers.add(player1);
      mockPlayers.add(player2);

      // Criar ServerPlayers para a lista
      Server server = createServerWithAllAttributes();

      ServerPlayer serverPlayer1 = new ServerPlayer(server, player1);
      ServerPlayer serverPlayer2 = new ServerPlayer(server, player2);
      mockServerPlayers.add(serverPlayer1);
      mockServerPlayers.add(serverPlayer2);

      when(serverPlayerRepository.findByServerId(anyLong())).thenReturn(mockServerPlayers);

      // Act
      List<PlayerDTO> players = playerService.listServerPlayers(serverId);

      // Assert
      assertNotNull(players);
      assertThat(players.size()).isEqualTo(2);

      // Verificar que a query foi chamada corretamente
      verify(serverPlayerRepository, times(1)).findByServerId(serverId);
    }
  }

  @Nested
  @DisplayName("Operações de Salvamento")
  class SaveServerPlayerTests {

    private Server mockServer;
    private Player mockPlayerExistente;

    @BeforeEach
    void setUpSalvamento() {
      // Preparar servidor e player para salvamento
      mockServer = createServerWithAllAttributes();

      mockPlayerExistente = new Player("Steve", "https://mc-heads.net/avatar/Steve/48");
      mockPlayers.add(mockPlayerExistente);
    }

    @Test
    @DisplayName("saveServerPlayer - Salva player novo com sucesso")
    void saveServerPlayer_salvaPlayerNovoComSucesso() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayerExistente));
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(mockServer));

      // Act
      playerService.saveServerPlayer(serverId, nickname);

      // Assert - Player foi salvo como member do servidor
      verify(serverPlayerRepository).save(any(ServerPlayer.class));
    }

    @Test
    @DisplayName("saveServerPlayer - Player inexiste cria novo na lista")
    void saveServerPlayer_criaPlayerInexistenteComSucesso() {
      // Arrange - Criar player que não existe no repositório
      Long serverId = 1L;
      String nickname = "NewPlayer";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.empty());
      when(playerRepository.save(any())).thenAnswer(invocation -> {
        Player player = invocation.getArgument(0);
        mockPlayers.add(player);
        return player;
      });

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(mockServer));

      // Act
      playerService.saveServerPlayer(serverId, nickname);

      // Assert - Player novo foi criado e salvo como member
      verify(playerRepository).save(any());
      verify(serverPlayerRepository).save(any(ServerPlayer.class));
    }

    @Test
    @DisplayName("saveServerPlayer - Player já associado não cria duplicado")
    void saveServerPlayer_playerJaAssociado_naoCriaDuplicado() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayerExistente));
      when(serverPlayerRepository.existsById(any())).thenReturn(true);

      // Act
      playerService.saveServerPlayer(serverId, nickname);

      // Assert - Verificar que save do repositório não foi chamado (já existia)
      verify(playerRepository, times(0)).save(any());
      verify(serverRepository, times(0)).findById(anyLong());
    }
  }

  @Nested
  @DisplayName("Operações de Conexão")
  class ConnectionTests {

    private Server mockServer;
    private Player mockPlayer;

    @BeforeEach
    void setUpConexao() {
      // Preparar player e servidor para operações de conexão
      mockServer = createServerWithAllAttributes();

      mockPlayer = new Player("Steve", "https://mc-heads.net/avatar/Steve/48");
      mockPlayers.add(mockPlayer);

      // Criar ServerPlayer para status
      ServerPlayer serverPlayer = new ServerPlayer(mockServer, mockPlayer);
      mockServerPlayers.add(serverPlayer);
    }

    @Test
    @DisplayName("playerConnected - Marca jogador como online com sucesso")
    void playerConnected_marcaJogadorComoOnline() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      ServerPlayer serverPlayer = new ServerPlayer(mockServer, mockPlayer);
      when(serverPlayerRepository.findById(any())).thenReturn(Optional.of(serverPlayer));

      // Act
      playerService.playerConnected(serverId, nickname);

      // Assert - Player conectado foi atualizado
      assertThat(serverPlayer.isOnline()).isTrue();
      verify(serverPlayerRepository).findById(any());
    }

    @Test
    @DisplayName("playerDisconnected - Marca jogador como offline")
    void playerDisconnected_marcaJogadorComoOffline() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      ServerPlayer serverPlayer = new ServerPlayer(mockServer, mockPlayer);
      when(serverPlayerRepository.findById(any())).thenReturn(Optional.of(serverPlayer));

      // Act
      playerService.playerDisconnected(serverId, nickname);

//       Assert - Player desconectado foi
      assertThat(serverPlayer.isOnline()).isFalse();
      verify(serverPlayerRepository).findById(any());
    }

    @Test
    @DisplayName("playerConnected - Player inexiste lança IllegalArgumentException")
    void playerConnected_playerInexistente_lancaException() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Unknown";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> {
        playerService.playerConnected(serverId, nickname);
      });

      verify(playerRepository).findByNickname(nickname);
    }
  }

  @Nested
  @DisplayName("Operações de Status do Jogador")
  class StatusOperationsTests {

    private Server mockServer;
    private Player mockPlayer;
    private ServerPlayer serverPlayer;

    @BeforeEach
    void setUpStatus() {
      // Preparar player e servidor para operações de status
      mockServer = createServerWithAllAttributes();

      mockPlayer = new Player("Steve", "https://mc-heads.net/avatar/Steve/48");
      mockPlayers.add(mockPlayer);

      serverPlayer = new ServerPlayer(mockServer, mockPlayer);
      when(serverPlayerRepository.findById(any())).thenReturn(Optional.of(serverPlayer));
    }

    @Test
    @DisplayName("opPlayer - Concede OP ao jogador")
    void opPlayer_concedeOPAoJogador() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      // Act
      playerService.opPlayer(serverId, nickname);

      // Assert - Método operator foi aceito
      assertThat(serverPlayer.isOperator()).isTrue();
    }

    @Test
    @DisplayName("deopPlayer - Remove OP do jogador")
    void deopPlayer_removeOPDoJogador() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      // Act
      playerService.deopPlayer(serverId, nickname);

      // Assert - Método deOperator foi aceito
      assertThat(serverPlayer.isOperator()).isFalse();
    }

    @Test
    @DisplayName("banPlayer - Bana o jogador")
    void banPlayer_banaJogador() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      // Act
      playerService.banPlayer(serverId, nickname);

      // Assert - Método banned foi aceito
      assertThat(serverPlayer.isBanned()).isTrue();
    }

    @Test
    @DisplayName("unbanPlayer - Remove ban do jogador")
    void unbanPlayer_removeBanDoJogador() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));

      // Act
      playerService.unbanPlayer(serverId, nickname);

      // Assert - Método unBanned foi aceito
      assertThat(serverPlayer.isBanned()).isFalse();
    }

    @Test
    @DisplayName("opPlayer - Player não associado ao servidor lança exception")
    void opPlayer_playerNaoAssociado_lancaException() {
      // Arrange
      Long serverId = 1L;
      String nickname = "Steve";

      when(playerRepository.findByNickname(anyString())).thenReturn(Optional.of(mockPlayer));
      when(serverPlayerRepository.findById(any())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> {
        playerService.opPlayer(serverId, nickname);
      });
    }
  }

  @Nested
  @DisplayName("Operações com MockedStatic")
  class IntegrationTests {

    @Test
    @DisplayName("saveServerPlayer - Integração completa com repositórios estáticos")
    void saveServerPlayer_integracaoCompleta() throws Exception {
      // Criar instâncias reais para simular ambiente de produção parcial
      Player player = new Player("Steve", "https://mc-heads.net/avatar/Steve/48");
      Server server = createServerWithAllAttributes();

      // Configurar mocks para o teste de integração
      when(playerRepository.findByNickname("Steve")).thenReturn(Optional.of(player));
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(server));


      // Act
      Long serverId = 1L;
      String nickname = "Steve";
      playerService.saveServerPlayer(serverId, nickname);

      // Assert - Verificar interações com os mocks estáticos
      verify(playerRepository).findByNickname("Steve");
      verify(serverRepository).findById(anyLong());
    }
  }
}