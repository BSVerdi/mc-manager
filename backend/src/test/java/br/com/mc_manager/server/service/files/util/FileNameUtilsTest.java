package br.com.mc_manager.server.service.files.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static br.com.mc_manager.server.service.files.util.FileNameUtils.extractExtension;
import static br.com.mc_manager.server.service.files.util.FileNameUtils.hasBlockedExtension;
import static org.assertj.core.api.Assertions.assertThat;

class FileNameUtilsTest {

  @Nested
  @DisplayName("extractExtension")
  class ExtractExtensionTests {

    @ParameterizedTest
    @ValueSource(strings = {".java", ".JAVA", ".JaVa"})
    void deveRetornarExtensaoEmLowercase(String fileName) {
      // Act
      String result = extractExtension(fileName);

      // Assert
      assertThat(result).isEqualTo("java");
    }

    @Test
    @DisplayName("Arquivo sem extensão retorna string vazia")
    void arquivoSemExtensao_retornaVazio() {
      // Arrange
      String fileName = "document";

      // Act
      String result = extractExtension(fileName);

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Arquivo com múltiplos pontos retorna apenas a última extensão")
    void multiplosPontos_retornaUltimaExtensao() {
      // Arrange
      String fileName = "file.tar.gz";

      // Act
      String result = extractExtension(fileName);

      // Assert
      assertThat(result).isEqualTo("gz");
    }

    @Test
    @DisplayName("Arquivo com ponto no final trata como extensão vazia")
    void arquivoComPontoNoFinal_retornaVazio() {
      // Arrange
      String fileName = "name.";

      // Act
      String result = extractExtension(fileName);

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Arquivo com apenas ponto retorna string vazia")
    void arquivoComAPenasUmPonto_retornaVazio() {
      // Arrange
      String fileName = ".";

      // Act
      String result = extractExtension(fileName);

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("hasBlockedExtension")
  class HasBlockedExtensionTests {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "jar", "class", "exe", "dll", "so", "mca", "dat", "zip", "gz"
    );

    @ParameterizedTest
    @ValueSource(strings = {"script.exe", "app.jar", "backup.zip"})
    void deveRetornarTrueParaArquivosBloqueados(String fileName) {
      // Act
      String extension = extractExtension(fileName);
      boolean result = hasBlockedExtension(extension);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Java file não é bloqueado")
    void javaFile_naoEhBloqueado() {
      // Arrange
      String fileName = "App.java";

      // Act
      boolean result = hasBlockedExtension(fileName);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Arquivo sem extensão não é bloqueado")
    void arquivoSemExtensao_naoEhBloqueado() {
      // Arrange
      String fileName = "readme";

      // Act
      boolean result = hasBlockedExtension(fileName);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Arquivo com extensão vazia não é bloqueado")
    void arquivoComExtensaoVazia_naoEhBloqueado() {
      // Arrange
      String fileName = "config.";

      // Act
      boolean result = hasBlockedExtension(fileName);

      // Assert
      assertThat(result).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "script.jar, true",
            "malware.class, true",
            "exploit.dll, true",
            "library.so, true",
            "config.dat, true",
            "mca.mca, true"
    })
    void deveRetornarTrueParaCadaExtensaoBloqueada(String fileName, boolean esperado) {
      // Act
      String extension = extractExtension(fileName);
      boolean result = hasBlockedExtension(extension);

      // Assert
      assertThat(result).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "image.jpg, false",
            "document.pdf, false",
            "video.mp4, false",
            "archive.tar, false"
    })
    void deveRetornarFalseParaArquivosNaoBloqueados(String fileName, boolean esperado) {
      // Act
      String extension = extractExtension(fileName);
      boolean result = hasBlockedExtension(extension);

      // Assert
      assertThat(result).isEqualTo(esperado);
    }
  }

  @Nested
  @DisplayName("normalizeEntryName")
  class NormalizeEntryNameTests {

    @Test
    @DisplayName("Converte backslash único para slash em caminho Windows puro")
    void caminhoWindowsPuro_converteBackslashParaSlash() {
      // Arrange
      String fileName = "C:\\Users\\Name\\file.txt";

      // Act
      String result = FileNameUtils.normalizeEntryName(fileName);

      // Assert
      assertThat(result).isEqualTo("C:/Users/Name/file.txt");
    }

    @Test
    @DisplayName("Converte múltiplos backslash consecutivos para slash")
    void multiplosBackslashes_converteParaSlash() {
      // Arrange
      String fileName = "path\\dir\\file";

      // Act
      String result = FileNameUtils.normalizeEntryName(fileName);

      // Assert
      assertThat(result).isEqualTo("path/dir/file");
    }

    @Test
    @DisplayName("Mantém caminho Linux com slash misturado")
    void caminhoLinux_comSlashMisturado_normaliza() {
      // Arrange
      String fileName = "/home/user/path/to\\file";

      // Act
      String result = FileNameUtils.normalizeEntryName(fileName);

      // Assert
      assertThat(result).isEqualTo("/home/user/path/to/file");
    }

    @Test
    @DisplayName("Arquivo sem path mantém nome original")
    void arquivoSemPath_mantemNomeOriginal() {
      // Arrange
      String fileName = "simple.txt";

      // Act
      String result = FileNameUtils.normalizeEntryName(fileName);

      // Assert
      assertThat(result).isEqualTo("simple.txt");
    }

    @ParameterizedTest
    @CsvSource({
            "C:\\Windows\\System32\\Config, C:/Windows/System32/Config",
            "/absolute/path/to\\file.txt, /absolute/path/to/file.txt"
    })
    void deveNormalizarDiferentesFormatosDePath(String input, String esperado) {
      // Act
      String result = FileNameUtils.normalizeEntryName(input);

      // Assert
      assertThat(result).isEqualTo(esperado);
    }
  }
}