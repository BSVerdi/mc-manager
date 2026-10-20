package br.com.mc_manager.server.service.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

  private final String scope = "test-scope";

  @Mock
  private PathSecurityService pathSecurityService;

  @InjectMocks
  private FileStorageService fileStorageService;

  @TempDir
  private Path tempDir;

  @Nested
  @DisplayName("Criar Pasta")
  class CreateFolderTests {

    private final String targetFolderName = "New Folder";
    private Path scopeDirectory;

    @BeforeEach
    void setUp() throws IOException {
      scopeDirectory = tempDir.resolve("mcmanager").resolve(scope);
      createDirectories(scopeDirectory);
    }

    @Test
    @DisplayName("Deve criar pasta com sucesso quando scope e targetPath são válidos")
    void deveCriarPastaComSucessoQuandoScopeETargetPathSaoValidos() {
      // Arrange
      Path novaPasta = scopeDirectory.resolve(targetFolderName);
      when(pathSecurityService.resolveSafePath(scope, targetFolderName)).thenReturn(novaPasta);

      // Act & Assert
      assertDoesNotThrow(() -> fileStorageService.createFolder(scope, targetFolderName));
      assertThat(Files.isDirectory(novaPasta)).isTrue();
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      when(pathSecurityService.resolveSafePath(anyString(), anyString()))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.createFolder(scope, targetFolderName))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando já existe um arquivo no caminho de destino")
    void deveLancarRuntimeExceptionQuandoJaExisteArquivoNoDestino() throws IOException {
      // Arrange
      Path destino = scopeDirectory.resolve(targetFolderName);
      createFile(destino);
      when(pathSecurityService.resolveSafePath(scope, targetFolderName)).thenReturn(destino);

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.createFolder(scope, targetFolderName))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Erro ao criar pasta");
    }
  }

  @Nested
  @DisplayName("Criar Arquivo")
  class CreateFileTests {

    private final String targetFileName = "novo-arquivo.txt";
    private Path scopeDirectory;

    @BeforeEach
    void setUp() throws IOException {
      scopeDirectory = tempDir.resolve("mcmanager").resolve(scope);
      createDirectories(scopeDirectory);
    }

    @Test
    @DisplayName("Deve criar arquivo vazio com sucesso quando scope e fileName são válidos")
    void deveCriarArquivoVazioComSucessoQuandoScopeEFilenameSaoValidos() {
      // Arrange
      Path novoArquivo = scopeDirectory.resolve(targetFileName);
      when(pathSecurityService.resolveSafePath(scope, targetFileName)).thenReturn(novoArquivo);

      // Act & Assert
      assertDoesNotThrow(() -> fileStorageService.createFile(scope, targetFileName));
      assertThat(Files.isRegularFile(novoArquivo)).isTrue();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando arquivo já existe no caminho de destino")
    void deveLancarRuntimeExceptionQuandoIOExceptionOcorreNaCriacao() throws IOException {
      // Arrange — Files.createFile real lança FileAlreadyExistsException (IOException) se já existir
      Path jaExistente = scopeDirectory.resolve(targetFileName);
      createFile(jaExistente);
      when(pathSecurityService.resolveSafePath(scope, targetFileName)).thenReturn(jaExistente);

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.createFile(scope, targetFileName))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("Erro ao criar arquivo.");
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      when(pathSecurityService.resolveSafePath(anyString(), anyString()))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.createFile(scope, targetFileName))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Salvar Arquivo com MultipartFile")
  class SaveFileWithMultipartTests {

    private MockMultipartFile mockFile;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
      fileContent = "Conteúdo de teste".getBytes();
      mockFile = new MockMultipartFile(
              "file",
              "test.txt",
              MediaType.TEXT_PLAIN_VALUE,
              fileContent
      );
    }

    @Test
    @DisplayName("Deve salvar arquivo substituindo o existente com sucesso")
    void deveSalvarArquivoSubstituindoOExistenteComSucesso() throws IOException {
      // Arrange
      String targetPath = "arquivo.txt";
      Path destino = tempDir.resolve(targetPath);
      Files.writeString(destino, "conteudo antigo"); // simula arquivo pré-existente a ser substituído

      when(pathSecurityService.resolveSafePath(scope, targetPath)).thenReturn(destino);

      // Act
      fileStorageService.saveFile(scope, targetPath, mockFile);

      // Assert
      assertThat(Files.readAllBytes(destino)).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      String targetPath = "arquivo-problema.txt";
      when(pathSecurityService.resolveSafePath(anyString(), anyString()))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.saveFile(scope, targetPath, mockFile))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Salvar Arquivo com InputStream")
  class SaveFileWithInputStreamTests {

    private InputStream fileInputStream;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
      fileContent = "Conteúdo de teste".getBytes();
      fileInputStream = new java.io.ByteArrayInputStream(fileContent);
    }

    @Test
    @DisplayName("Deve salvar arquivo com conteúdo de InputStream com sucesso")
    void deveSalvarArquivoComConteudoDeInputStreamComSucesso() throws IOException {
      // Arrange
      String targetPath = "arquivo-stream.txt";
      Path destino = tempDir.resolve(targetPath);

      when(pathSecurityService.resolveSafePath(scope, targetPath)).thenReturn(destino);

      // Act
      fileStorageService.saveFile(scope, targetPath, fileInputStream);

      // Assert
      assertThat(Files.readAllBytes(destino)).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      String targetPath = "arquivo-stream-problema.txt";
      when(pathSecurityService.resolveSafePath(anyString(), anyString()))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.saveFile(scope, targetPath, fileInputStream))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Deletar Arquivo ou Pasta (dispatcher)")
  class DeleteTests {

    @Test
    @DisplayName("Deve delegar a deleteFolder quando alvo é um diretório")
    void deveDelegarADeleteFolderQuandoAlvoEhDiretorio() throws IOException {
      String nomePasta = "pasta-vazia";
      Path pasta = tempDir.resolve(nomePasta);
      createDirectories(pasta);

      doReturn(pasta).when(pathSecurityService).resolveSafePath(anyString(), anyString());
      doReturn(pasta).when(pathSecurityService).getFolderPath(anyString(), anyString());

      fileStorageService.delete(scope, nomePasta);

      assertThat(Files.exists(pasta)).isFalse();
    }

    @Test
    @DisplayName("Deve delegar a deleteFile quando alvo é um arquivo")
    void deveDelegarADeleteFileQuandoAlvoEhArquivo() throws IOException {
      // Arrange
      String nomeArquivo = "arquivo-para-deletar.txt";
      Path arquivo = tempDir.resolve(nomeArquivo);
      createFile(arquivo);

      doReturn(arquivo).when(pathSecurityService).resolveSafePath(anyString(), anyString());

      // Act
      fileStorageService.delete(scope, nomeArquivo);

      // Assert
      assertThat(Files.exists(arquivo)).isFalse();
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      doThrow(new SecurityException("Caminho fora do escopo permitido"))
              .when(pathSecurityService).resolveSafePath(anyString(), anyString());

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.delete(scope, "qualquer-coisa"))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Deletar Arquivo Específico")
  class DeleteFileSpecificTests {

    @Test
    @DisplayName("Deve deletar arquivo específico com sucesso")
    void deveDeletarArquivoEspecificoComSucesso() throws IOException {
      // Arrange
      String targetPath = "arquivo-especifico.txt";
      Path arquivo = tempDir.resolve(targetPath);
      createFile(arquivo);

      doReturn(arquivo).when(pathSecurityService).resolveSafePath(anyString(), anyString());

      // Act
      fileStorageService.deleteFile(scope, targetPath);

      // Assert
      assertThat(Files.exists(arquivo)).isFalse();
    }

    @Test
    @DisplayName("Deve retornar silenciosamente quando arquivo não existe")
    void deveRetornarSilenciosamenteQuandoArquivoNaoExiste() {
      // Arrange
      String targetPath = "arquivo-nao-existe.txt";
      Path arquivo = tempDir.resolve(targetPath);

      doReturn(arquivo).when(pathSecurityService).resolveSafePath(anyString(), anyString());

      // Act & Assert
      assertDoesNotThrow(() -> fileStorageService.deleteFile(scope, targetPath));
      assertThat(Files.exists(arquivo)).isFalse();
    }
  }

  @Nested
  @DisplayName("Deletar Pasta com Arquivos")
  class DeleteFolderTests {

    private Path mockFolderPath;

    @BeforeEach
    void setUp() throws IOException {
      mockFolderPath = tempDir.resolve("pasta-para-deletar");
      Path subpasta = mockFolderPath.resolve("subpasta");
      createDirectories(subpasta);
      Files.writeString(mockFolderPath.resolve("arquivo1.txt"), "conteudo");
      Files.writeString(subpasta.resolve("arquivo2.txt"), "conteudo");
    }

    @Test
    @DisplayName("Deve deletar pasta inteira incluindo subpastas e arquivos")
    void deveDeletarPastaInteiraIncluindoSubpastasEArquivos() throws IOException {
      // Arrange
      doReturn(mockFolderPath).when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act
      fileStorageService.deleteFolder(scope, "pasta-para-deletar");

      // Assert
      assertThat(Files.exists(mockFolderPath)).isFalse();
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      doThrow(new SecurityException("Caminho fora do escopo permitido"))
              .when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.deleteFolder(scope, "pasta-problema"))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

  }

  @Nested
  @DisplayName("Listar Arquivos de Diretório")
  class ListFilesTests {

    private void prepararDiretorio(Path base, String... nomesArquivos) throws IOException {
      for (String nome : nomesArquivos) {
        Path path = base.resolve(nome);
        if (path.getParent() != null) {
          createDirectories(path.getParent());
        }
        Files.write(path, "conteudo".getBytes());
      }
    }

    @Test
    @DisplayName("Deve listar arquivos e subdiretórios do diretório alvo")
    void deveListarArquivosESubdiretoriosDoDiretorioAlvo() throws IOException {
      // Arrange
      Path folderPath = tempDir.resolve("meu-diretorio");
      createDirectories(folderPath);
      prepararDiretorio(folderPath, "arquivo1.txt", "arquivo2.txt", "subdiretorio/arquivo3.txt");

      doReturn(folderPath).when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act
      var listaArquivos = fileStorageService.listFiles(scope, "meu-diretorio");

      // Assert
      assertThat(listaArquivos).hasSize(3);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando diretório não tem conteúdo")
    void deveRetornarListaVaziaQuandoDiretorioNaoTemConteudo() throws IOException {
      // Arrange
      Path folderPath = tempDir.resolve("diretorio-vazio");
      createDirectories(folderPath);

      doReturn(folderPath).when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act
      var listaArquivos = fileStorageService.listFiles(scope, "diretorio-vazio");

      // Assert
      assertThat(listaArquivos).isEmpty();
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      doThrow(new SecurityException("Caminho fora do escopo permitido"))
              .when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.listFiles(scope, "diretorio-problema"))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Baixar Arquivo")
  class DownloadFileTests {

    private Path mockFilePath;

    @BeforeEach
    void setUp() throws IOException {
      mockFilePath = tempDir.resolve("download-me.txt");
      Files.write(mockFilePath, "Conteúdo para download".getBytes());
    }

    @Test
    @DisplayName("Deve retornar MediaDto com contentType detectado e Resource equivalente ao arquivo")
    void deveRetornarMediaDtoComContentTypeDetectadoEResource() throws IOException {
      // Arrange
      String targetPath = "download-me.txt";
      doReturn(mockFilePath).when(pathSecurityService).getFilePath(anyString(), anyString());

      // Act
      var mediaDto = fileStorageService.downloadFile(scope, targetPath);

      // Assert
      assertThat(mediaDto.resource()).isEqualTo(new UrlResource(mockFilePath.toUri()));
      assertThat(mediaDto.mediaType()).isEqualTo(MediaType.TEXT_PLAIN);
      assertThat(mediaDto.path()).isEqualTo(Path.of("download-me.txt").getFileName());
    }

    @Test
    @DisplayName("Deve usar APPLICATION_OCTET_STREAM quando contentType não é detectado")
    void deveUsarApplicationOctetStreamQuandoContentTypeNaoEhDetectado() throws IOException {
      // Arrange — arquivo sem extensão reconhecida
      Path binaryPath = tempDir.resolve("arquivo-binario");
      Files.write(binaryPath, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

      doReturn(binaryPath).when(pathSecurityService).getFilePath(anyString(), anyString());

      // Act
      var mediaDto = fileStorageService.downloadFile(scope, "arquivo-binario");

      // Assert
      assertThat(mediaDto.mediaType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      String targetPath = "url-invalida.txt";
      doThrow(new SecurityException("Caminho fora do escopo permitido"))
              .when(pathSecurityService).getFilePath(anyString(), anyString());

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.downloadFile(scope, targetPath))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("Upload de Arquivo")
  class UploadFileTests {

    private MockMultipartFile mockFile;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
      fileContent = "Conteúdo de upload".getBytes();
      mockFile = new MockMultipartFile(
              "file",
              "original.txt",
              MediaType.TEXT_PLAIN_VALUE,
              fileContent
      );
    }

    @Test
    @DisplayName("Deve salvar arquivo com originalFilename válido")
    void deveSalvarArquivoComOriginalFilenameValido() throws IOException {
      // Arrange
      String targetFilePath = "";
      String safeName = "original.txt";
      Path folder = tempDir.resolve(targetFilePath);
      Path destino = folder.resolve(safeName);

      doReturn(folder).when(pathSecurityService).getFolderPath(anyString(), anyString());

      // Act
      fileStorageService.uploadFile(scope, targetFilePath, mockFile);

      // Assert
      assertThat(Files.readAllBytes(destino)).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando originalFilename é null ou blank")
    void deveLancarIllegalArgumentExceptionQuandoOriginalFilenameEhNullOuBlank() {
      // Arrange
      String targetPath = "uploadado-problema.txt";
      MockMultipartFile mockFileSemNome = new MockMultipartFile(
              "file",
              "",
              "text/plain",
              "conteudo".getBytes()
      );

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.uploadFile(scope, targetPath, mockFileSemNome))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Nome de arquivo inválido.");
    }

    @Test
    @DisplayName("Deve propagar SecurityException quando path é inválido")
    void devePropagarSecurityExceptionQuandoPathEhInvalido() {
      // Arrange
      String targetPath = "upload-falha.txt";
      when(pathSecurityService.getFolderPath(anyString(), anyString()))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileStorageService.uploadFile(scope, targetPath, mockFile))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }
}