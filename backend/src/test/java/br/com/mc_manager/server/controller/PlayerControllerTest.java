package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.factory.PlayerDtoFactory;
import br.com.mc_manager.server.security.expression.ServerSecurityExpressions;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "mcmanager.data_root_directory=./target/test-data")
class PlayerControllerTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private PlayerService playerService;

  @MockitoBean
  private ServerRuntimeService serverManagerService;

  @MockitoBean
  private ServerSecurityExpressions serverSecurityExpressions;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(get("/").with(csrf()))
            .build();

    given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(true);
  }

  @Nested
  @DisplayName("Listagem de Jogadores do Servidor")
  class TesteListarJogadoresDoServidor {

    private static final Long SERVER_ID = 1L;

    @Test
    @WithMockUser
    @DisplayName("Deve retornar lista de jogadores para servidor específico")
    void deveRetornarListaDeJogadoresParaServidorEspecifico() throws Exception {
      // Arrange
      PlayerDTO player1 = PlayerDtoFactory.createPlayerDTOWithAttributes("Steve", true);
      PlayerDTO player2 = PlayerDtoFactory.createPlayerDTOWithAttributes("Alex", false);
      List<PlayerDTO> expectedPlayers = List.of(player1, player2);

      when(playerService.listServerPlayers(SERVER_ID)).thenReturn(expectedPlayers);

      // Act & Assert
      mockMvc.perform(get("/api/servers/" + SERVER_ID + "/players"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$[0].nickname").value("Steve"))
              .andExpect(jsonPath("$[1].nickname").value("Alex"));

      // Verificação adicional do serviço chamado corretamente
      verify(playerService).listServerPlayers(SERVER_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar lista vazia quando não houver jogadores")
    void deveRetornarListaVaziaQuandoNaoHouverJogadores() throws Exception {
      // Arrange
      List<PlayerDTO> expectedPlayers = List.of();

      when(playerService.listServerPlayers(SERVER_ID)).thenReturn(expectedPlayers);

      // Act & Assert
      mockMvc.perform(get("/api/servers/" + SERVER_ID + "/players"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$").isArray())
              .andExpect(jsonPath("$.size()").value(0));

      verify(playerService).listServerPlayers(SERVER_ID);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 500 quando PlayerService lança exception")
    void deveRetornar500QuandoPlayerServiceLancaException() throws Exception {
      // Arrange
      when(playerService.listServerPlayers(SERVER_ID))
              .thenThrow(new RuntimeException("Erro ao listar jogadores"));

      // Act & Assert
      mockMvc.perform(get("/api/servers/" + SERVER_ID + "/players"))
              .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 403 quando usuário não é proprietário do servidor")
    void deveRetornar403QuandoUsuarioNaoEProprietarioDoServidor() throws Exception {
      // Arrange
      when(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).thenReturn(false);
      List<PlayerDTO> expectedPlayers = List.of();

      when(playerService.listServerPlayers(SERVER_ID)).thenReturn(expectedPlayers);
      // Simula que o usuário não é proprietário do servidor
      String userWithLowPermissions = "user_low_permissions";

      // Act & Assert
      mockMvc.perform(get("/api/servers/" + SERVER_ID + "/players")
                      .with(user(userWithLowPermissions)))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("OP de Jogador (Grant Operator)")
  class TesteOPOfJogador {

    private static final Long SERVER_ID = 1L;
    private static final String PLAYER_NICKNAME = "Steve";

    @Test
    @WithMockUser
    @DisplayName("Deve conceder OP ao jogador e retornar 204")
    void deveConcederOPAoJogadoreRetornar204() throws Exception {
      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/op"))
              .andExpect(status().isNoContent());

      verify(serverManagerService).opPlayer(SERVER_ID, PLAYER_NICKNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando jogador não existe")
    void deveRetornar404QuandoJogadorNaoExiste() throws Exception {
      // Arrange
      doThrow(new NoSuchElementException("Jogador não encontrado"))
              .when(serverManagerService).opPlayer(SERVER_ID, PLAYER_NICKNAME);

      // Act & Assert — O controller provavelmente lança 404 ou similar aqui
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/op"))
              .andExpect(status().isNotFound());
    }
  }

  @Nested
  @WithMockUser
  @DisplayName("DeOP de Jogador (Revoke Operator)")
  class TesteDeOPofJogador {

    private static final Long SERVER_ID = 3L;
    private static final String PLAYER_NICKNAME = "Alex";

    @Test
    @DisplayName("Deve revogar OP do jogador e retornar 204")
    void deveRevogarOPAoJogadoreRetornar204() throws Exception {
      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/deop"))
              .andExpect(status().isNoContent());

      verify(serverManagerService).deopPlayer(SERVER_ID, PLAYER_NICKNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando jogador não é OP ou não existe")
    void deveRetornar404QuandoJogadorNaoEOPouNaoExiste() throws Exception {
      // Arrange
      doThrow(new NoSuchElementException("Jogador não é OP ou não encontrado"))
              .when(serverManagerService).deopPlayer(SERVER_ID, PLAYER_NICKNAME);

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/deop"))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 403 para usuários que não são donos do servidor")
    void deveRetornar403ParaUsuariosNaoDonos() throws Exception {
      // Arrange
      when(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).thenReturn(false);
      String regularUser = "regular_user";

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/deop")
                      .with(user(regularUser)))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Banimento de Jogador")
  class TesteBanimentoDeJogador {

    private static final Long SERVER_ID = 4L;
    private static final String PLAYER_NICKNAME = "Creeper";

    @Test
    @WithMockUser
    @DisplayName("Deve banir jogador e retornar 204")
    void deveBanirJogadoreRetornar204() throws Exception {
      // Arrange

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/ban"))
              .andExpect(status().isNoContent());

      verify(serverManagerService).banPlayer(SERVER_ID, PLAYER_NICKNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando jogador não existe no servidor")
    void deveRetornar404QuandoJogadorNaoExisteNoServidor() throws Exception {
      // Arrange
      doThrow(new NoSuchElementException()).when(serverManagerService).banPlayer(SERVER_ID, PLAYER_NICKNAME);

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/ban"))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 403 quando não é dono do servidor")
    void deveRetornar403QuandoNaoEDonoDoServidor() throws Exception {
      // Arrange
      when(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).thenReturn(false);

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/ban"))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Desbanimento de Jogador")
  class TesteDesbanimentoDeJogador {

    private static final Long SERVER_ID = 5L;
    private static final String PLAYER_NICKNAME = "Obsidian";

    @Test
    @WithMockUser
    @DisplayName("Deve desbanir jogador e retornar 204")
    void deveDesbanirJogadoreRetornar204() throws Exception {
      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/unban"))
              .andExpect(status().isNoContent());

      verify(serverManagerService).unbanPlayer(SERVER_ID, PLAYER_NICKNAME);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando jogador não existe")
    void deveRetornar404QuandoJogadorNaoEstaBanidoOuNaoExiste() throws Exception {
      // Arrange
      doThrow(new NoSuchElementException("Jogador não encontrado"))
              .when(serverManagerService).unbanPlayer(SERVER_ID, PLAYER_NICKNAME);

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/unban"))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 403 para usuários não autorizados")
    void deveRetornar403ParaUsuariosNaoAutorizados() throws Exception {
      // Arrange
      when(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).thenReturn(false);
      String regularUser = "regular_user_no_unban_permission";

      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/" + PLAYER_NICKNAME + "/unban")
                      .with(user(regularUser)))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Cenários de Erros Genéricos")
  class TesteCasosDeBorda {

    private static final Long SERVER_ID = 99L;

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando servidor não existe no mapa")
    void deveRetornar404QuandoServidorNaoExisteNoMapa() throws Exception {
      // Arrange — servidor ID inexistente
      when(playerService.listServerPlayers(SERVER_ID))
              .thenThrow(new NoSuchElementException("Servidor não encontrado"));

      // Act & Assert
      mockMvc.perform(get("/api/servers/" + SERVER_ID + "/players"))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 405 quando método HTTP é inválido")
    void deveRetornar405QuandoMetodoHTTPeInvalido() throws Exception {
      // Act & Assert — PUT não é suportado por este endpoint
      mockMvc.perform(put("/api/servers/" + SERVER_ID + "/players"))
              .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 quando nickname é nulo ou vazio")
    void deveRetornar400QuandoNicknameENuloouVazio() throws Exception {
      // Act & Assert
      mockMvc.perform(patch("/api/servers/" + SERVER_ID + "/players/"))
              .andExpect(status().isNotFound());
    }
  }
}