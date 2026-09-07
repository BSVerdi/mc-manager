package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDto;
import br.com.mc_manager.server.dto.MinecraftManifest.DownloadMinecraftVersionDto;
import br.com.mc_manager.server.dto.MinecraftManifest.JavaVersionDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftServerDownloadDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftVersionDetailsDto;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerFilesServiceTest {

  @InjectMocks
  private ServerFilesService serverFilesService;

  @Mock
  private MinecraftMetaDataService minecraftMetaDataService;

  @Mock
  private FileStorageService fileStorageService;

  @Mock
  private PathSecurityService pathSecurityService;

  @Mock
  private FileArchiveService fileArchiveService;

  @Nested
  @DisplayName("Operações de Criação")
  class CreateOperationsTests {

    @Test
    @DisplayName("createBaseServerFiles - Cria estrutura base do servidor com sucesso")
    void createBaseServerFiles_criaEstruturaComSucesso() throws IOException {
      // Arrange
      Long serverId = 1L;
      Integer serverPort = 25565;

      // Act
      serverFilesService.createBaseServerFiles(serverId, serverPort);

      // Assert - Verificar que todas as operações foram chamadas
      verify(fileStorageService).createFolder("servers/", "1");
      verify(fileStorageService, atLeastOnce()).createFile(anyString(), anyString());
      verify(fileStorageService, atLeastOnce()).saveFileContent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("createBaseServerFiles - Lança IOException se falhar ao criar pasta")
    void createBaseServerFiles_falhaAoCriarPasta_lancaIOException() {
      // Arrange
      Long serverId = 1L;

      doThrow(RuntimeException.class).when(fileStorageService).createFolder(anyString(), anyString());

      // Act & Assert
      assertThrows(RuntimeException.class, () -> {
        serverFilesService.createBaseServerFiles(serverId, 25565);
      });

      verify(fileStorageService).createFolder(anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("Operações de Exclusão")
  class DeleteOperationsTests {

    @Test
    @DisplayName("deleteServerFiles - Deleta pasta do servidor com sucesso")
    void deleteServerFiles_deletaPastaDoServidor() throws IOException {
      // Arrange
      Long serverId = 2L;

      // Act
      serverFilesService.deleteServerFiles(serverId);

      // Assert
      verify(fileStorageService).deleteFolder("servers/" + serverId, "/");
    }

    @Test
    @DisplayName("deleteServerFiles - Lança IOException ao deletar pasta")
    void deleteServerFiles_falhaAoDeletar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 2L;

      doThrow(IOException.class).when(fileStorageService).deleteFolder(anyString(), anyString());

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.deleteServerFiles(serverId);
      });
    }

    @Test
    @DisplayName("delete - Deleta arquivo individual com sucesso")
    void delete_deletaArquivoIndividual() throws IOException {
      // Arrange
      Long serverId = 3L;
      String path = "logs/latest.log";

      // Act
      serverFilesService.delete(serverId, path);

      // Assert
      verify(fileStorageService).delete("servers/" + serverId + "/", path);
    }

    @Test
    @DisplayName("delete - Lança IOException ao deletar arquivo")
    void delete_falhaAoDeletarArquivo_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 3L;
      String path = "config.json";

      doThrow(IOException.class).when(fileStorageService).delete("servers/" + serverId + "/", path);

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.delete(serverId, path);
      });
    }
  }

  @Nested
  @DisplayName("Operações de Download")
  class DownloadOperationsTests {

    @Test
    @DisplayName("downloadVanillaJar - Baixa arquivo jar com sucesso e retorna versão Java")
    void downloadVanillaJar_baixaArquivoJarComSucesso() throws IOException {
      // Arrange
      Long serverId = 1L;
      String serverVersion = "latest";

      when(minecraftMetaDataService.getMinecraftServerVersionDetails(anyString()))
              .thenReturn(
                      new MinecraftVersionDetailsDto(
                              "exampleId",
                              new DownloadMinecraftVersionDto(
                                      new MinecraftServerDownloadDto("Example Url")
                              ),
                              new JavaVersionDto("17")
                      )
              );

      // Mock do InputStream para o arquivo jar
      try (InputStream inputStream = new ByteArrayInputStream(new byte[0])) {
        when(minecraftMetaDataService.getServerInputStream(anyString())).thenReturn(inputStream);

        // Act
        String javaVersion = serverFilesService.downloadVanillaJar(serverId, serverVersion);

        // Assert
        verify(minecraftMetaDataService).getMinecraftServerVersionDetails(serverVersion);
        verify(fileStorageService).saveFile(eq("servers/" + serverId + "/"), eq("server.jar"), any(InputStream.class));
        assertEquals("17", javaVersion);
      }

    }

    @Test
    @DisplayName("downloadVanillaJar - Lança IOException se falhar ao baixar jar")
    void downloadVanillaJar_falhaAoBaixar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;
      InputStream in = new ByteArrayInputStream(new byte[0]);

      when(minecraftMetaDataService.getMinecraftServerVersionDetails(anyString()))
              .thenReturn(
                      new MinecraftVersionDetailsDto(
                              "exampleId",
                              new DownloadMinecraftVersionDto(
                                      new MinecraftServerDownloadDto("Example Url")
                              ),
                              new JavaVersionDto("26.2")
                      )
              );

      when(minecraftMetaDataService.getServerInputStream(anyString())).thenReturn(in);

      doThrow(IOException.class).when(fileStorageService)
              .saveFile(eq("servers/" + serverId + "/"), eq("server.jar"), any(InputStream.class));

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.downloadVanillaJar(serverId, "26.2");
      });
    }

    @Test
    @DisplayName("downloadFile - Retorna MediaDto com arquivo existente")
    void downloadFile_retornaMediaDtoComArquivo() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "config/settings.yml";

      when(fileStorageService.downloadFile(anyString(), anyString())).thenReturn(
              new MediaDto(null, null, null)
      );

      // Act
      MediaDto response = serverFilesService.downloadFile(serverId, path);

      // Assert
      assertNotNull(response);
      verify(fileStorageService).downloadFile("servers/" + serverId + "/", path);
    }

    @Test
    @DisplayName("downloadFile - Lança IOException ao tentar baixar arquivo não existente")
    void downloadFile_falhaAoBaixar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "nonexistent.yml";

      when(fileStorageService.downloadFile(anyString(), anyString()))
              .thenThrow(new IOException("Arquivo não encontrado"));

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.downloadFile(serverId, path);
      });
    }

    @Test
    @DisplayName("downloadFolder - Compacta pasta para ZIP com sucesso")
    void downloadFolder_compactaPastaParaZip() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "/";

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        // Act
        serverFilesService.downloadFolder(serverId, path, outputStream);

        // Assert
        verify(fileArchiveService).downloadFolder("servers/" + serverId + "/", path, outputStream);
      }
    }

    @Test
    @DisplayName("downloadFolder - Lança IOException ao compactar pasta")
    void downloadFolder_falhaAoCompactar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        doThrow(IOException.class).when(fileArchiveService).downloadFolder(anyString(), anyString(), any(OutputStream.class));

        // Act & Assert
        assertThrows(IOException.class, () -> {
          serverFilesService.downloadFolder(serverId, "/", outputStream);
        });
      }
    }
  }

  @Nested
  @DisplayName("Operações de Upload")
  class UploadOperationsTests {

    private MockMultipartFile mockFile;
    private SaveFileRequestDTO mockRequest;

    @BeforeEach
    void setUpUpload() {
      // Preparar arquivo e request de upload para testes
      mockFile = new MockMultipartFile(
              "file",
              "config.yml",
              "application/yaml",
              "{}".getBytes()
      );

      mockRequest = new SaveFileRequestDTO("{\n    \"config\": true\n}");
    }

    @Test
    @DisplayName("uploadFile - Envia arquivo único com sucesso")
    void uploadFile_enviaArquivoUnicoComSucesso() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "config/settings.yml";

      // Act
      serverFilesService.uploadFile(serverId, path, mockFile);

      // Assert
      verify(fileStorageService).uploadFile("servers/" + serverId + "/", path, mockFile);
    }

    @Test
    @DisplayName("uploadFile - Lança IOException ao enviar arquivo")
    void uploadFile_falhaAoEnviar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;

      doThrow(IOException.class).when(fileStorageService).uploadFile(anyString(), anyString(), any(MockMultipartFile.class));

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.uploadFile(serverId, "config.yml", mockFile);
      });
    }

    @Test
    @DisplayName("uploadFolder - Envia arquivo ZIP com sucesso")
    void uploadFolder_enviaArquivoZipComSucesso() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "/backup";

      // Act
      serverFilesService.uploadFolder(serverId, path, mockFile);

      // Assert
      verify(fileArchiveService).uploadFolder("servers/" + serverId + "/", path, mockFile);
    }

    @Test
    @DisplayName("uploadFolder - Lança IOException ao enviar pasta")
    void uploadFolder_falhaAoEnviar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "servers/";

      doThrow(IOException.class).when(fileArchiveService).uploadFolder(anyString(), anyString(), any(MockMultipartFile.class));

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.uploadFolder(serverId, path, mockFile);
      });
    }

    @Test
    @DisplayName("saveFileContent - Salva conteúdo em arquivo existente")
    void saveFileContent_salvaConteohEmArquivo() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "config/new-config.yml";

      // Act
      serverFilesService.saveFileContent(serverId, path, mockRequest);

      // Assert
      verify(fileStorageService).saveFileContent("servers/" + serverId + "/", "config/new-config.yml", mockRequest.content());
    }

    @Test
    @DisplayName("saveFileContent - Lança IOException ao salvar conteúdo")
    void saveFileContent_falhaAoSalvar_lancaIOException() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "servers/";

      doThrow(IOException.class).when(fileStorageService).saveFileContent(anyString(), anyString(), any());

      // Act & Assert
      assertThrows(IOException.class, () -> {
        serverFilesService.saveFileContent(serverId, path, mockRequest);
      });
    }
  }

  @Nested
  @DisplayName("Operações de Leitura")
  class ReadOperationsTests {

    private FileContentDTO mockFileContent;
    private List<FileEntryDTO> mockServerFiles;

    @BeforeEach
    void setUpLeitura() {
      // Preparar DTOs e listas para teste de leitura
      mockFileContent = new FileContentDTO("config/config.yml", "Teste", true, "txt");
      mockServerFiles = List.of(
              new FileEntryDTO("config.yml", false, 1L, 0L, "yml"),
              new FileEntryDTO("logs/", true, 0L, 0L, null)
      );
    }

    @Test
    @DisplayName("getFileContent - Retorna conteúdo de arquivo editável")
    void getFileContent_retornaConteohDeArquivoEditavel() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "config/config.yml";

      when(fileStorageService.getFileContent(anyString(), anyString())).thenReturn(mockFileContent);

      // Act
      FileContentDTO response = serverFilesService.getFileContent(serverId, path);

      // Assert
      assertNotNull(response);
      verify(fileStorageService).getFileContent("servers/" + serverId + "/", path);
    }

    @Test
    @DisplayName("getFileContent - Retorna conteúdo vazio para arquivo não editável")
    void getFileContent_retornaVazioParaArquivoNaoEditavel() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "config/config.yml";

      when(fileStorageService.getFileContent(anyString(), anyString())).thenReturn(
              new FileContentDTO("servers/", null, false, "jar")
      );

      // Act
      FileContentDTO response = serverFilesService.getFileContent(serverId, path);

      // Assert
      assertNotNull(response);
      assertFalse(response.editable());
    }

    @Test
    @DisplayName("listServerFiles - Lista arquivos de pasta com sucesso")
    void listServerFiles_listaArquivosDePasta() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "/";

      when(fileStorageService.listFiles(anyString(), anyString())).thenReturn(mockServerFiles);

      // Act
      List<FileEntryDTO> response = serverFilesService.listServerFiles(serverId, path);

      // Assert
      assertNotNull(response);
      assertThat(response.size()).isEqualTo(2);
      verify(fileStorageService).listFiles("servers/" + serverId + "/", path);
    }

    @Test
    @DisplayName("listServerFiles - Lista arquivos com sub-pastas")
    void listServerFiles_listaComSubPastas() throws IOException {
      // Arrange
      Long serverId = 1L;
      String path = "logs";

      when(fileStorageService.listFiles(anyString(), eq(path))).thenReturn(mockServerFiles);

      // Act
      List<FileEntryDTO> response = serverFilesService.listServerFiles(serverId, path);

      // Assert
      assertThat(response).containsExactlyElementsOf(mockServerFiles);
    }
  }

  @Nested
  @DisplayName("Operações de Segurança")
  class SecurityOperationsTests {

    @Test
    @DisplayName("getServerDirectory - Retorna Path do diretório do servidor")
    void getServerDirectory_retornaPathDoDiretorio() {
      // Arrange
      Long serverId = 1L;

      when(pathSecurityService.resolveSafePath(anyString(), eq(""))).thenReturn(
              Paths.get("servers/" + serverId + "/")
      );

      // Act
      Path result = serverFilesService.getServerDirectory(serverId);

      // Assert
      assertNotNull(result);
      assertTrue(result.startsWith("servers/" + serverId));
    }

  }

}
