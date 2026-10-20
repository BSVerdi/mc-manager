package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.security.expression.ServerSecurityExpressions;
import br.com.mc_manager.server.service.ServerManagementService;
import br.com.mc_manager.server.service.provider.MinecraftProvider;
import br.com.mc_manager.server.service.provider.ServerProviderRegistry;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static br.com.mc_manager.server.factory.ServerDtoFactory.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServerControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private ServerRuntimeService runtimeService;

  @MockitoBean
  private ServerManagementService managementService;

  @MockitoBean
  private ServerProviderRegistry registry;

  private final MinecraftProvider provider = mock(MinecraftProvider.class);

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
  @DisplayName("Listar Servidores")
  class ListServersTest {

    @Test
    @WithMockUser
    @DisplayName("Deve listar servidores com sucesso para usuário autenticado")
    void deveListarServidores() throws Exception {
      // Arrange
      ServerDetailsDTO server1 = createServerResponseDtoWithAttributes(1L, "Server 1");
      ServerDetailsDTO server2 = createServerResponseDtoWithAttributes(2L, "Server 2");

      given(managementService.listServers(anyString())).willReturn(Arrays.asList(server1, server2));

      // Act & Assert
      mockMvc.perform(get("/api/servers"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.size()").value(2))
              .andExpect(jsonPath("$.[0].id").value(1))
              .andExpect(jsonPath("$.[1].name").value("Server 2"));

      verify(managementService).listServers("user");
      verifyNoMoreInteractions(runtimeService, registry);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar lista vazia quando usuário não possui servidores")
    void deveRetornarListaVaziaQuandoSemServidores() throws Exception {
      given(managementService.listServers(anyString())).willReturn(Collections.emptyList());

      mockMvc.perform(get("/api/servers"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      mockMvc.perform(get("/api/servers"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(managementService);
    }
  }

  @Nested
  @DisplayName("Obter Servidor por ID")
  class GetServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve retornar servidor existente com sucesso")
    void deveRetornarServidorExistente() throws Exception {
      // Arrange
      Long serverId = 1L;
      ServerDetailsDTO expectedServer = createServerResponseDtoWithAllAttributes();

      given(managementService.getServer(serverId)).willReturn(expectedServer);

      // Act & Assert
      mockMvc.perform(get("/api/servers/1"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.id").value(serverId))
              .andExpect(jsonPath("$.name").value("Example Server"));

      verify(managementService).getServer(serverId);
      verifyNoMoreInteractions(runtimeService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/servers/1"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(managementService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(get("/api/servers/1"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(managementService);
    }
  }

  @Nested
  @DisplayName("Criar Servidor")
  class CreateServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve criar servidor com sucesso e retornar 201 Created")
    void deveCriarServidorComSucesso() throws Exception {
      // Arrange
      CreateServerDTO createServerDto = createCreateServerDTOWithAllAttributes();
      ServerDetailsDTO createdServerDto = createServerResponseDtoWithAllAttributes();

      given(managementService.create(any(CreateServerDTO.class), any())).willReturn(createdServerDto);

      String dtoJson = objectMapper.writeValueAsString(createServerDto);

      // Act & Assert
      mockMvc.perform(post("/api/servers")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.id").value(1L))
              .andExpect(jsonPath("$.version").value("26.2"));

      verify(managementService).create(any(CreateServerDTO.class), any());
      verifyNoMoreInteractions(runtimeService, registry);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 400 Bad Request para DTO inválido (campo obrigatório em falta)")
    void deveRetornarBadRequestQuandoFaltaCampoObrigatorio() throws Exception {
      // Arrange
      CreateServerDTO dto = createCreateServerDTOWithNullAttributes();
      String dtoJson = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc.perform(post("/api/servers")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isBadRequest());

      verifyNoInteractions(managementService);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Arrange
      CreateServerDTO createServerDto = createCreateServerDTOWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(createServerDto);

      // Act & Assert
      mockMvc.perform(post("/api/servers")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(managementService);
    }
  }

  @Nested
  @DisplayName("Atualizar Servidor")
  class UpdateServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve atualizar servidor com sucesso e reiniciar")
    void deveAtualizarESReiniciarServidor() throws Exception {
      // Arrange
      UpdateServerDTO updateServerDto = createUpdateServerDTOWithAllAttributes();
      ServerDetailsDTO updatedServer = createServerResponseDtoWithAttributes(1L, "Updated Server");

      given(managementService.update(any(Long.class), any(UpdateServerDTO.class))).willReturn(updatedServer);

      String dtoJson = objectMapper.writeValueAsString(updateServerDto);

      // Act & Assert
      mockMvc.perform(put("/api/servers/1")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.name").value("Updated Server"));

      verify(managementService).update(1L, updateServerDto);
      verify(runtimeService).restartWithUpdatedConfig(1L);
      verifyNoMoreInteractions(registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Arrange
      UpdateServerDTO updateServerDto = createUpdateServerDTOWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(updateServerDto);

      // Act & Assert
      mockMvc.perform(put("/api/servers/1")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(managementService, runtimeService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      UpdateServerDTO updateServerDto = createUpdateServerDTOWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(updateServerDto);

      // Act & Assert
      mockMvc.perform(put("/api/servers/1")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isForbidden());

      verifyNoInteractions(managementService, runtimeService);
    }
  }

  @Nested
  @DisplayName("Deletar Servidor")
  class DeleteServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve deletar servidor com sucesso e retornar 204 No Content")
    void deveDeletarServidorComSucesso() throws Exception {
      // Arrange
      Long serverId = 1L;

      // Act & Assert
      mockMvc.perform(delete("/api/servers/1"))
              .andExpect(status().isNoContent());

      verify(managementService).delete(serverId);
      verifyNoMoreInteractions(runtimeService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/servers/1"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(managementService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(delete("/api/servers/1"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(managementService);
    }
  }

  @Nested
  @DisplayName("Iniciar Servidor")
  class StartServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve iniciar servidor com sucesso e retornar 204 No Content")
    void deveIniciarServidorComSucesso() throws Exception {
      // Arrange
      Long serverId = 1L;

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/start"))
              .andExpect(status().isNoContent());

      verify(runtimeService).start(serverId);
      verifyNoMoreInteractions(managementService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(post("/api/servers/1/start"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(runtimeService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/start"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(runtimeService);
    }
  }

  @Nested
  @DisplayName("Parar Servidor")
  class StopServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve parar servidor com sucesso e retornar 204 No Content")
    void devePararServidorComSucesso() throws Exception {
      // Arrange
      Long serverId = 1L;

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/stop"))
              .andExpect(status().isNoContent());

      verify(runtimeService).stop(serverId);
      verifyNoMoreInteractions(managementService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(post("/api/servers/1/stop"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(runtimeService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/stop"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(runtimeService);
    }
  }

  @Nested
  @DisplayName("Reiniciar Servidor")
  class RestartServerTest {

    @Test
    @WithMockUser
    @DisplayName("Deve reiniciar servidor com sucesso e retornar 204 No Content")
    void deveReiniciarServidorComSucesso() throws Exception {
      // Arrange
      Long serverId = 1L;

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/restart"))
              .andExpect(status().isNoContent());

      verify(runtimeService).restart(serverId);
      verifyNoMoreInteractions(managementService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(post("/api/servers/1/restart"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(runtimeService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/servers/1/restart"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(runtimeService);
    }
  }

  @Nested
  @DisplayName("Obter Status do Servidor")
  class GetStatusTest {

    @Test
    @WithMockUser
    @DisplayName("Deve retornar status do servidor com sucesso")
    void deveRetornarStatusDoServidor() throws Exception {
      // Arrange
      Long serverId = 1L;
      ServerStatus expectedStatus = ServerStatus.ONLINE;

      given(runtimeService.getStatus(serverId)).willReturn(expectedStatus);

      // Act & Assert
      mockMvc.perform(get("/api/servers/1/status"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.status").value("ONLINE"));

      verify(runtimeService).getStatus(serverId);
      verifyNoMoreInteractions(managementService, registry);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/servers/1/status"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(runtimeService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono_retorna403() throws Exception {
      // Arrange
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(get("/api/servers/1/status"))
              .andExpect(status().isForbidden());

      verifyNoInteractions(runtimeService);
    }
  }

  @Nested
  @DisplayName("Obter Todas as Versões do Minecraft")
  class GetAllVersionsTest {

    @BeforeEach
    void setUp() {
      given(registry.get(ServerType.VANILLA)).willReturn(provider);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar todas as versões do Minecraft disponíveis")
    void deveRetornarTodasVersoes() throws Exception {
      // Arrange
      List<String> allVersions = List.of("1.20.4", "1.20.1", "1.19.4");
      given(provider.getAllVersions()).willReturn(allVersions);

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions").param("type", "VANILLA"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.size()").value(3));

      verify(registry).get(ServerType.VANILLA);
      verify(provider).getAllVersions();
      verifyNoMoreInteractions(managementService);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar lista vazia quando não há versões disponíveis")
    void deveRetornarListaVaziaQuandoSemVersoes() throws Exception {
      // Arrange
      given(provider.getAllVersions()).willReturn(Collections.emptyList());

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions").param("type", "VANILLA"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Rota /versions não é interpretada como {serverId} (guarda de colisão de rota)")
    void rotaVersionsNaoColideComServerId() throws Exception {
      // Arrange
      given(provider.getAllVersions()).willReturn(Collections.emptyList());

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions").param("type", "VANILLA"))
              .andExpect(status().isOk());

      verify(registry).get(ServerType.VANILLA);
      verify(provider).getAllVersions();
      verifyNoInteractions(managementService);
    }
  }

  @Nested
  @DisplayName("Obter Versões Lançadas do Minecraft")
  class GetReleasedVersionsTest {

    @BeforeEach
    void setUp() {
      given(registry.get(ServerType.VANILLA)).willReturn(provider);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar apenas versões lançadas (release)")
    void deveRetornarApenasVersoesLancadas() throws Exception {
      // Arrange
      List<String> releasedVersions = Arrays.asList(
              "1.20.4",
              "1.20.1"
      );

      given(provider.getReleasedVersions()).willReturn(releasedVersions);

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions/released").param("type", "VANILLA"))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON));

      verify(registry).get(ServerType.VANILLA);
      verify(provider).getReleasedVersions();
      verifyNoMoreInteractions(managementService);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar lista vazia quando não há versões lançadas")
    void deveRetornarListaVaziaQuandoSemVersoesLancadas() throws Exception {
      // Arrange
      given(provider.getReleasedVersions()).willReturn(Collections.emptyList());

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions/released").param("type", "VANILLA"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Rota /versions/released não é interpretada como {serverId} (guarda de colisão de rota)")
    void rotaVersionsReleasedNaoColideComServerId() throws Exception {
      // Arrange
      given(provider.getReleasedVersions()).willReturn(Collections.emptyList());

      // Act & Assert
      mockMvc.perform(get("/api/servers/versions/released").param("type", "VANILLA"))
              .andExpect(status().isOk());

      verify(registry).get(ServerType.VANILLA);
      verify(provider).getReleasedVersions();
      verifyNoInteractions(managementService);
    }
  }
}