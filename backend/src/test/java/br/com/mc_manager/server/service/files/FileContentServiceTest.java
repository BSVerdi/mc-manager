package br.com.mc_manager.server.service.files;

import br.com.mc_manager.server.dto.FileContentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileContentServiceTest {

  private static final long MAX_EDITABLE_SIZE = 2 * 1024 * 1024;
  private final String scope = "test-scope";
  private final String targetPath = "documento.txt";

  @Mock
  private PathSecurityService pathSecurityService;

  @InjectMocks
  private FileContentService fileContentService;

  @TempDir
  private Path tempDir;

  private Path realFile;

  @BeforeEach
  void setUp() {
    realFile = tempDir.resolve(targetPath);
  }

  @Nested
  @DisplayName("getFileContent")
  class GetFileContentTests {

    @Test
    @DisplayName("Arquivo editável - lê e retorna conteúdo completo")
    void deveLerARetornarConteudoDeArquivoEditavel() throws IOException {
      // Arrange
      Files.writeString(realFile, "Este é o conteúdo do arquivo", StandardCharsets.UTF_8);
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(realFile);

      // Act
      FileContentDTO resultado = fileContentService.getFileContent(scope, targetPath);

      // Arrange
      assertThat(resultado.content()).isEqualTo("Este é o conteúdo do arquivo");
      assertThat(resultado.editable()).isTrue();
      assertThat(resultado.extension()).isEqualTo("txt");
      assertThat(resultado.path()).isEqualTo(targetPath);
    }

    @Test
    @DisplayName("Extensão bloqueada - retorna conteúdo vazio e editable false, sem ler o arquivo")
    void deveRetornarConteudoVazioParaExtensaoBloqueada() throws IOException {
      // Arrange
      Path scriptFile = tempDir.resolve("script.sh");

      Files.writeString(scriptFile, "#!/bin/bash\necho hacked", StandardCharsets.UTF_8);
      when(pathSecurityService.getFilePath(scope, "script.sh")).thenReturn(scriptFile);

      // Act
      FileContentDTO resultado = fileContentService.getFileContent(scope, "script.sh");

      // Assert
      assertThat(resultado.content()).isEmpty();
      assertThat(resultado.editable()).isFalse();
      assertThat(resultado.extension()).isEqualTo("sh");
    }

    @Test
    @DisplayName("Arquivo não existe - retorna conteúdo vazio e false")
    void deveRetornarConteudoVazioParaArquivoQueNaoExiste() throws IOException {
      // Arrange
      Path inexistente = tempDir.resolve("nao-existe.txt");
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(inexistente);

      // Act
      FileContentDTO resultado = fileContentService.getFileContent(scope, targetPath);

      // Assert
      assertThat(resultado.content()).isEmpty();
      assertThat(resultado.editable()).isFalse();
    }

    @Test
    @DisplayName("Arquivo acima do limite de tamanho - retorna conteúdo vazio e false")
    void deveRetornarConteudoVazioParaArquivoAcimaDoLimite() throws IOException {
      // Arrange
      byte[] grande = new byte[(int) MAX_EDITABLE_SIZE + 100];
      Files.write(realFile, grande);
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(realFile);

      // Act
      FileContentDTO resultado = fileContentService.getFileContent(scope, targetPath);

      // Assert
      assertThat(resultado.content()).isEmpty();
      assertThat(resultado.editable()).isFalse();
    }

    @Test
    @DisplayName("getFilePath lança SecurityException - exceção propagada")
    void devePropagarSecurityExceptionDaGetFilePath() {
      // Arrange
      when(pathSecurityService.getFilePath(scope, targetPath))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileContentService.getFileContent(scope, targetPath))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }
  }

  @Nested
  @DisplayName("saveFileContent")
  class SaveFileContentTests {

    @Test
    @DisplayName("Salvar conteúdo em arquivo editável - sucesso")
    void deveSalvarConteudoEmArquivoEditavel() throws IOException {
      // Arrange
      Files.writeString(realFile, "conteudo antigo", StandardCharsets.UTF_8);
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(realFile);

      // Act
      fileContentService.saveFileContent(scope, targetPath, "Novo conteúdo salvo");

      // Assert
      assertThat(Files.readString(realFile, StandardCharsets.UTF_8))
              .isEqualTo("Novo conteúdo salvo");
    }

    @Test
    @DisplayName("Extensão bloqueada - lança IllegalArgumentException e não escreve")
    void deveLancarIllegalArgumentExceptionParaExtensaoBloqueada() throws IOException {
      // Arrange
      Path scriptFile = tempDir.resolve("script.sh");
      Files.writeString(scriptFile, "conteudo original", StandardCharsets.UTF_8);
      when(pathSecurityService.getFilePath(scope, "script.sh")).thenReturn(scriptFile);

      // Act & Assert
      assertThatThrownBy(() ->
              fileContentService.saveFileContent(scope, "script.sh", "conteudo malicioso")
      ).isInstanceOf(IllegalArgumentException.class);

      assertThat(Files.readString(scriptFile, StandardCharsets.UTF_8))
              .isEqualTo("conteudo original");
    }

    @Test
    @DisplayName("Arquivo não existe - lança IllegalArgumentException")
    void deveLancarIllegalArgumentExceptionParaArquivoQueNaoExiste() {
      // Arrange
      Path inexistente = tempDir.resolve("nao-existe.txt");
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(inexistente);

      // Act & Assert
      assertThatThrownBy(() ->
              fileContentService.saveFileContent(scope, targetPath, "conteudo")
      ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getFilePath lança SecurityException - exceção propagada no save")
    void devePropagarSecurityExceptionDaGetFilePathNoSave() {
      // Arrange
      when(pathSecurityService.getFilePath(scope, targetPath))
              .thenThrow(new SecurityException("Caminho fora do escopo permitido"));

      // Act & Assert
      assertThatThrownBy(() -> fileContentService.saveFileContent(scope, targetPath, "conteudo"))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    @DisplayName("Arquivo acima do limite de tamanho - lança IllegalArgumentException e não sobrescreve")
    void deveLancarIllegalArgumentExceptionParaArquivoAcimaDoLimite() throws IOException {
      // Arrange
      byte[] grande = new byte[(int) MAX_EDITABLE_SIZE + 100];
      Files.write(realFile, grande);
      when(pathSecurityService.getFilePath(scope, targetPath)).thenReturn(realFile);

      // Act & Assert
      assertThatThrownBy(() ->
              fileContentService.saveFileContent(scope, targetPath, "novo conteudo")
      ).isInstanceOf(IllegalArgumentException.class);

      assertThat(Files.size(realFile)).isEqualTo(grande.length);
    }
  }
}