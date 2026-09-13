package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerConsoleLineProcessor")
class ServerConsoleLineProcessorTest {

  private Long serverId = 1L;

  @Mock
  private ServerStatusBroadcaster broadcaster;

  @Mock
  private PlayerService playerService;

  @InjectMocks
  private ServerConsoleLineProcessor processor;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(processor, "serverId", serverId);
  }

  @Nested
  @DisplayName("accept(String rawLine)")
  class AcceptTest {

    @Test
    @DisplayName("deveRemoverPrefixoDeDataERepasarParaBroadcaster")
    void deveRemoverPrefixoDeDataERepasarParaBroadcaster() {
      // arrange
      String rawLine = "[12:34:56] [INFO] Server started";
      String esperada = "[INFO] Server started";

      // act
      processor.accept(rawLine);

      // assert
      verify(broadcaster).consoleLine(esperada);
    }

    @Test
    @DisplayName("deveRepasarLinhaOriginalQuandoNaoHaPrefixoDeData")
    void deveRepasarLinhaOriginalQuandoNaoHaPrefixoDeData() {
      // arrange
      String rawLine = "Server started successfully";

      // act
      processor.accept(rawLine);

      // assert
      verify(broadcaster).consoleLine(rawLine);
    }

    @Test
    @DisplayName("deveRegistrarConexaoDoJogadorQuandoLinhaIndicaJoin")
    void deveRegistrarConexaoDoJogadorQuandoLinhaIndicaJoin() {
      // arrange
      String rawLine = "[12:34:56] [INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";
      String clean = "[INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";

      // act
      processor.accept(rawLine);

      // assert
      verify(playerService).saveServerPlayer(serverId, "Steve");
      verify(playerService).playerConnected(serverId, "Steve");
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(1);
      verify(broadcaster).consoleLine(clean);
    }

    @Test
    @DisplayName("deveRegistrarDesconexaoDoJogadorQuandoLinhaIndicaLeave")
    void deveRegistrarDesconexaoDoJogadorQuandoLinhaIndicaLeave() {
      // arrange — colocar Steve online antes do leave
      String joinLine = "[12:34:56] [INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";
      processor.accept(joinLine);

      String rawLine = "[12:34:57] [INFO] Steve lost connection: Disconnected";
      String clean = "[INFO] Steve lost connection: Disconnected";

      // act
      processor.accept(rawLine);

      // assert
      verify(playerService).playerDisconnected(serverId, "Steve");
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
      verify(broadcaster).consoleLine(clean);
    }

    @Test
    @DisplayName("deveApenasRepassarParaBroadcasterQuandoLinhaNaoEhNenhumEvento")
    void deveApenasRepassarParaBroadcasterQuandoLinhaNaoEhNenhumEvento() {
      // arrange
      String rawLine = "[12:34:56] [INFO] Server started";

      // act
      processor.accept(rawLine);

      // assert
      verify(broadcaster).consoleLine("[INFO] Server started");
      verify(playerService, never()).saveServerPlayer(anyLong(), anyString());
      verify(playerService, never()).playerConnected(anyLong(), anyString());
      verify(playerService, never()).playerDisconnected(anyLong(), anyString());
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("deveLancarNullPointerExceptionQuandoLinhaEHNull")
    void deveLancarNullPointerExceptionQuandoLinhaEHNull() {
      // act & assert
      assertThatThrownBy(() -> processor.accept(null))
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deveTratarLinhaVaziaComoLinhaOriginal")
    void deveTratarLinhaVaziaComoLinhaOriginal() {
      // act
      processor.accept("");

      // assert
      verify(broadcaster).consoleLine("");
      verify(playerService, never()).saveServerPlayer(anyLong(), anyString());
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("getOnlinePlayersCount()")
  class GetOnlinePlayersCountTest {

    @Test
    @DisplayName("deveRetornarZeroQuandoNenhumJogadorEstaOnline")
    void deveRetornarZeroQuandoNenhumJogadorEstaOnline() {
      // act & assert
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("deveRetornarQuantidadeCorretaAposVariosJoins")
    void deveRetornarQuantidadeCorretaAposVariosJoins() {
      // arrange
      String join1 = "[12:34:56] [INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";
      String join2 = "[12:34:57] [INFO] Alex[/Alex] logged in with entity id 12346 at (2.34, 5.67, 8.90)";
      String join3 = "[12:34:58] [INFO] Bob[/Bob] logged in with entity id 12347 at (3.45, 6.78, 9.01)";

      // act
      processor.accept(join1);
      processor.accept(join2);
      processor.accept(join3);

      // assert
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("deveRetornarUmQuandoMesmoJogadorTentaJoinarDuasVezes")
    void deveRetornarUmQuandoMesmoJogadorTentaJoinarDuasVezes() {
      // arrange — o Set é idempotente: mesmo nome entra só uma vez
      String join = "[12:34:56] [INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";

      // act
      processor.accept(join);
      processor.accept(join);

      // assert
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("disconnectAllPlayers()")
  class DisconnectAllPlayersTest {

    @Test
    @DisplayName("deveDesconectarTodosOsJogadoresQuandoHaJogadoresOnline")
    void deveDesconectarTodosOsJogadoresQuandoHaJogadoresOnline() {
      // arrange — 2 players on-line
      String join1 = "[12:34:56] [INFO] Steve[/Steve] logged in with entity id 12345 at (1.23, 4.56, 7.89)";
      String join2 = "[12:34:57] [INFO] Alex[/Alex] logged in with entity id 12346 at (2.34, 5.67, 8.90)";
      processor.accept(join1);
      processor.accept(join2);

      // act
      processor.disconnectAllPlayers();

      // assert
      verify(playerService).playerDisconnected(serverId, "Steve");
      verify(playerService).playerDisconnected(serverId, "Alex");
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("deveTerComportamentoIndiferenteQuandoNenhumJogadorEstaOnline")
    void deveTerComportamentoIndiferenteQuandoNenhumJogadorEstaOnline() {
      // act
      processor.disconnectAllPlayers();

      // assert
      verify(playerService, never()).playerDisconnected(anyLong(), anyString());
      assertThat(processor.getOnlinePlayersCount()).isEqualTo(0);
    }
  }
}