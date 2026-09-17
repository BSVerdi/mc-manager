package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDto;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static br.com.mc_manager.server.factory.ServerFactory.createServerWithOwner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerManagementServiceTest {

  @InjectMocks
  private ServerManagementService serverManagementService;

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private UserService userService;

  @Mock
  private ServerFilesService provisioningService;

  @Mock
  private ServerDomainGenerator serverDomainGenerator;

  @Mock
  private User owner;

  private Principal principal;

  private List<Server> mockServers = new ArrayList<>();

  @ParameterizedTest
  @CsvSource({
          "example1",
          "example2"
  })
  @DisplayName("Listagem com diferentes usuários")
  void listServers_comDiferentesUsuarios(String username) {
    // Arrange
    when(serverRepository.findByOwnerEmail(username)).thenReturn(mockServers);

    // Act
    List<ServerDetailsDto> servers = serverManagementService.listServers(username);

    // Assert
    assertNotNull(servers);
  }

  @Nested
  @DisplayName("Operações de Listagem")
  class ListServerTests {

    @Test
    @DisplayName("listServers - Retorna lista de todos os servidores do usuário")
    void listServers_retornaTodosServidoresDoUsuario() {
      // Arrange
      when(serverRepository.findByOwnerEmail(anyString())).thenReturn(mockServers);

      // Act
      List<ServerDetailsDto> servers = serverManagementService.listServers("admin");

      // Assert
      assertNotNull(servers);
      assertThat(servers.size()).isEqualTo(mockServers.size());

      // Verificar que a query do repositório foi chamada corretamente
      verify(serverRepository, times(1)).findByOwnerEmail("admin");
    }

    @Test
    @DisplayName("listServers - Retorna lista vazia quando não há servidores")
    void listServers_semServidores_retornaListaVazia() {
      // Arrange
      when(serverRepository.findByOwnerEmail(anyString())).thenReturn(new ArrayList<>());

      // Act & Assert
      List<ServerDetailsDto> servers = serverManagementService.listServers("admin");
      assertThat(servers).isEmpty();
    }
  }

  @Nested
  @DisplayName("Operações de Consulta")
  class GetServerTests {

    @Test
    @DisplayName("getServer - Retorna servidor existente com sucesso")
    void getServer_retornaServidorExiste() {
      // Arrange
      Server server = createServerWithAllAttributes();

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(server));

      // Act
      ServerDetailsDto response = serverManagementService.getServer(1L);

      // Assert
      assertNotNull(response);
      assertThat(response.id()).isEqualTo(1L);
      verify(serverRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getServer - Lança NoSuchElementException quando servidor não existe")
    void getServer_servidorNaoExiste_lancaException() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(NoSuchElementException.class, () -> {
        serverManagementService.getServer(99L);
      });

      verify(serverRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("getServer - Retorna servidor com todos os dados")
    void getServer_retornaComTodosDados() {
      // Arrange
      Server server = createServerWithAllAttributes();

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(server));

      // Act
      ServerDetailsDto response = serverManagementService.getServer(1L);

      // Assert
      assertNotNull(response.id());
      assertThat(response.name()).isEqualTo(server.getName());
      assertThat(response.port()).isEqualTo(server.getPort());
      verify(serverRepository, times(1)).findById(1L);
    }
  }

  @Nested
  @DisplayName("Operações de Criação")
  class CreateServerTests {

    private Server mockServerCriado;
    private CreateServerDTO createServerDTO;

    @BeforeEach
    void setUpCriacao() {
      // Preparar DTO e servidor para criação
      createServerDTO = new CreateServerDTO("Example Server", 25565, null, null, "26.2");
      when(serverRepository.existsByPort(anyInt())).thenReturn(false);
      mockServerCriado = createServerWithOwner(owner);
      mockServers.add(mockServerCriado);
    }

    @Test
    @DisplayName("create - Cria servidor novo com sucesso")
    void create_criaServidorNovoComSucesso() throws Exception {
      // Arrange
      when(serverDomainGenerator.generate(any())).thenReturn("ExampleServer-1");
      when(serverRepository.save(any())).thenReturn(mockServerCriado);
      when(userService.currentUser(any())).thenReturn(owner);
      doNothing().when(provisioningService).createBaseServerFiles(anyLong(), anyInt());
      doReturn("21").when(provisioningService).downloadVanillaJar(anyLong(), anyString());

      // Act
      ServerDetailsDto response = serverManagementService.create(createServerDTO, principal);

      // Assert
      assertNotNull(response);
      assertThat(response.id()).isEqualTo(mockServerCriado.getId());

      // Verificar chamadas aos mocks
      verify(serverRepository).save(any(Server.class));
      verify(provisioningService).createBaseServerFiles(anyLong(), anyInt());
    }

    @Test
    @DisplayName("create - Lança IllegalArgumentException quando porta já está em uso")
    void create_portaJaEmUso_lancaIllegalArgumentException() {
      // Arrange
      when(serverRepository.existsByPort(anyInt())).thenReturn(true);

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> {
        serverManagementService.create(createServerDTO, principal);
      });
    }

    @Test
    @DisplayName("create - Apaga servidor do repositório em caso de erro de IO")
    void create_erroAoCriarArquivos_deletaServidor() throws Exception {
      // Arrange
      when(serverDomainGenerator.generate(any())).thenReturn("ExampleServer-1");
      when(userService.currentUser(any())).thenReturn(owner);
      when(serverRepository.save(any())).thenReturn(mockServerCriado);

      // Simular exceção ao criar arquivos base do servidor
      doThrow(new IOException()).when(provisioningService)
              .createBaseServerFiles(anyLong(), anyInt());

      assertThrows(RuntimeException.class, () -> serverManagementService.create(createServerDTO, principal));

      verify(serverRepository).delete(mockServerCriado);
    }

    @Test
    @DisplayName("create - Salva e atualiza versão Java com sucesso")
    void create_salvaEAtualizaVersaoJava() throws Exception {
      // Arrange
      when(serverDomainGenerator.generate(any())).thenReturn("ExampleServer-1");
      when(userService.currentUser(any())).thenReturn(owner);
      when(serverRepository.save(any())).thenReturn(mockServerCriado);

      doNothing().when(provisioningService).createBaseServerFiles(anyLong(), anyInt());
      doReturn("17").when(provisioningService).downloadVanillaJar(anyLong(), anyString());

      // Act
      ServerDetailsDto response = serverManagementService.create(createServerDTO, principal);

      // Assert
      assertNotNull(response);
      assertThat(mockServerCriado.getJavaVersion()).isEqualTo("JAVA_17");
      verify(serverRepository, times(1)).save(any(Server.class));
      verify(provisioningService).createBaseServerFiles(anyLong(), anyInt());
    }
  }

  @Nested
  @DisplayName("Operações de Atualização")
  class UpdateServerTests {

    private Server mockServerExistente;

    @BeforeEach
    void setUpAtualizacao() {
      // Preparar servidor existente para atualização
      mockServerExistente = createServerWithOwner(owner);
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(mockServerExistente));
    }

    @Test
    @DisplayName("update - Atualiza servidor existente com sucesso")
    void update_atualizaServidorExistente() {
      // Arrange
      UpdateServerDTO updateDTO = new UpdateServerDTO("Example Name", null, null);

      // Act
      ServerDetailsDto response = serverManagementService.update(1L, updateDTO);

      // Assert
      assertNotNull(response);
      assertThat(response.id()).isEqualTo(1L);
      assertThat(response.name()).isEqualTo("Example Name");

      verify(serverRepository).save(any(Server.class));
    }

    @Test
    @DisplayName("update - Lança NoSuchElementException quando servidor não existe")
    void update_servidorNaoExiste_lancaException() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(NoSuchElementException.class, () -> {
        serverManagementService.update(99L, new UpdateServerDTO("Example Name", null, null));
      });
    }

    @Test
    @DisplayName("update - Retorna resposta DTO após atualização")
    void update_retornaRespostaDtoApesAtualizacao() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(mockServerExistente));

      // Act
      ServerDetailsDto response = serverManagementService.update(1L, new UpdateServerDTO("Example Name", null, null));

      // Assert
      assertNotNull(response);
      assertThat(response).isInstanceOf(ServerDetailsDto.class);
      verify(serverRepository).save(any(Server.class));
    }
  }

  @Nested
  @DisplayName("Operações de Exclusão")
  class DeleteTests {

    @Test
    @DisplayName("delete - Deleta servidor existente com sucesso")
    void delete_deletaServidorExistente() throws Exception {
      // Arrange
      Server serverParaDeletar = createServerWithAllAttributes();

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverParaDeletar));

      // Act
      doNothing().when(provisioningService).deleteServerFiles(anyLong());
      serverManagementService.delete(4L);

      // Assert
      verify(provisioningService).deleteServerFiles(4L);
      verify(serverRepository).delete(serverParaDeletar);
    }

    @Test
    @DisplayName("delete - Lança NoSuchElementException quando servidor não existe")
    void delete_servidorNaoExiste_lancaException() {
      // Arrange
      when(serverRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(NoSuchElementException.class, () -> {
        serverManagementService.delete(99L);
      });
    }

    @Test
    @DisplayName("delete - Lança IOException quando falha ao deletar arquivos")
    void delete_erroAoDeletarArquivos_lancaIOException() throws Exception {
      // Arrange
      Server serverParaDeletar = createServerWithAllAttributes();

      when(serverRepository.findById(anyLong())).thenReturn(Optional.of(serverParaDeletar));

      doThrow(new IOException("Erro ao deletar arquivos")).when(provisioningService)
              .deleteServerFiles(anyLong());

      // Act & Assert
      assertThrows(UncheckedIOException.class, () -> {
        serverManagementService.delete(1L);
      });
    }
  }

  @Nested
  @DisplayName("Validação de Permissão")
  class OwnershipTests {

    private Server mockServerOwnerizado;

    @BeforeEach
    void setUpOwnership() {
      // Preparar servidor com owner para validação
      mockServerOwnerizado = createServerWithOwner(owner);

      when(serverRepository.findByIdWithOwner(anyLong())).thenReturn(Optional.of(mockServerOwnerizado));
    }

    @Test
    @DisplayName("assertOwnership - Usuário é owner do servidor")
    void assertOwnership_usuarioEownerDoServidor() {
      // Arrange
      when(mockServerOwnerizado.getOwner().getUsername()).thenReturn("example");

      // Act & Assert
      assertDoesNotThrow(() -> {
        serverManagementService.assertOwnership(1L, "example");
      });
    }

    @Test
    @DisplayName("assertOwnership - Usuário não é owner do servidor")
    void assertOwnership_usuarioNaoEowner_lancaSecurityException() {
      // Arrange
      when(owner.getUsername()).thenReturn("outrousuario");

      // Act & Assert
      assertThrows(SecurityException.class, () -> {
        serverManagementService.assertOwnership(1L, "admin");
      });
    }

    @Test
    @DisplayName("assertOwnership - ID de servidor inexistente lança NoSuchElementException")
    void assertOwnership_servidorInexistente_lancaException() {
      // Arrange
      when(serverRepository.findByIdWithOwner(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(NoSuchElementException.class, () -> {
        serverManagementService.assertOwnership(99L, "admin");
      });
    }
  }

  @Nested
  @DisplayName("Testes de Integração com MockedStatic")
  class IntegrationTests {

    @Test
    @DisplayName("Listagem com Static Mock do Repositório")
    void listagemServidoresMockEstático() throws Exception {
      try (MockedStatic<ServerRepository> mockRepo = mockStatic(ServerRepository.class)) {
        when(serverRepository.findByOwnerEmail("admin")).thenReturn(mockServers);

        List<ServerDetailsDto> servers = serverManagementService.listServers("admin");

        assertThat(servers.size()).isEqualTo(mockServers.size());
      }
    }

    @Test
    @DisplayName("Criação com Static Mock de ProvisioningService")
    void criacaoServidoresMockEstático() throws Exception {
      try (MockedStatic<ServerFilesService> mockProvisioning = mockStatic(ServerFilesService.class)) {
        ServerFilesService provisioningServiceMock = mock(ServerFilesService.class);
        Server server = createServerWithAllAttributes();

        when(provisioningServiceMock.downloadVanillaJar(anyLong(), anyString())).thenReturn("17");
        when(serverRepository.save(any())).thenReturn(server);

        serverManagementService = new ServerManagementService(serverRepository, userService, provisioningServiceMock, serverDomainGenerator);

        // Executar criação de servidor
        CreateServerDTO dto = new CreateServerDTO("Example Server", 25565, null, null, "26.2");
        ServerDetailsDto response = serverManagementService.create(dto, principal);

        verify(provisioningServiceMock).createBaseServerFiles(anyLong(), anyInt());
      }
    }
  }
}