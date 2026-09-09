package br.com.mc_manager.server.service.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class PathSecurityServiceTest {

  @InjectMocks
  private PathSecurityService service;

  @TempDir
  private Path tempDir;

  private Path baseDirectory;
  private Path scopeDirectory;
  private Path dataDirectory;
  private Path documentFile;

  @BeforeEach
  void setup() throws Exception {
    baseDirectory = tempDir.resolve("mcmanager");
    scopeDirectory = baseDirectory.resolve("escopos/api");
    dataDirectory = scopeDirectory.resolve("dados");
    documentFile = scopeDirectory.resolve("documento.txt");

    createDirectories(dataDirectory);
    createFile(documentFile);

    setField(service, "dataRootDirectory", baseDirectory.toString());
  }

  @Nested
  class ResolveSafePath {

    @Test
    void deveResolverCaminhoValido() {
      // Act
      Path result = service.resolveSafePath("escopos/api", "dados.json");

      // Arrange
      assertThat(result).isEqualTo(scopeDirectory.resolve("dados.json"));
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeSaiDoDiretorioBase() {
      // Act & Assert
      assertThatThrownBy(() -> service.resolveSafePath("..", "arquivo.txt"))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeSaiDoDiretorioBaseAtravesDeTraversal() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.resolveSafePath(
                      "escopos/api/../../../../",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }

    @Test
    void deveLancarSecurityExceptionQuandoPathSaiDoEscopoPermitido() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.resolveSafePath(
                      "escopos/api",
                      "../../arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoPathTentaAcessarDiretorioPai() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.resolveSafePath(
                      "escopos/api",
                      "../documento.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveNormalizarCaminhoComCurrentDirectory() {
      // Act
      Path result = service.resolveSafePath("escopos/./api", "./dados.json");

      // Assert
      assertThat(result).isEqualTo(scopeDirectory.resolve("dados.json"));
    }

    @Test
    void deveNormalizarParentDirectorySemSairDoEscopo() {
      // Act
      Path result = service.resolveSafePath("escopos/api", "dados/../documento.txt");

      // Assert
      assertThat(result).isEqualTo(documentFile);
    }

    @Test
    void devePermitirScopeVazio() {
      // Act
      Path result = service.resolveSafePath("", "");

      // Assert
      assertThat(result).isEqualTo(baseDirectory);
    }

    @Test
    void deveLancarNullPointerExceptionQuandoScopeForNull() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.resolveSafePath(
                      null,
                      "arquivo.txt"
              )
      ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveLancarNullPointerExceptionQuandoPathForNull() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.resolveSafePath(
                      "escopos/api",
                      null
              )
      ).isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class GetFolderPath {

    @Test
    void deveRetornarDiretorioValido() {
      // Act
      Path result = service.getFolderPath("escopos/api", "dados");

      // Assert
      assertThat(result).isEqualTo(dataDirectory);
      assertThat(Files.isDirectory(result)).isTrue();
    }

    @Test
    void deveLancarIllegalArgumentExceptionQuandoCaminhoForArquivo() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFolderPath(
                      "escopos/api",
                      "documento.txt"
              )
      )
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("O caminho informado não é um diretório");
    }

    @Test
    void deveLancarSecurityExceptionQuandoCaminhoSairDoEscopo() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFolderPath(
                      "escopos/api",
                      "../documento.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeForInvalido() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFolderPath(
                      "..",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }
  }

  @Nested
  class GetFilePath {

    @Test
    void deveRetornarArquivoValido() {
      // Act
      Path result = service.getFilePath("escopos/api", "documento.txt");

      // Assert
      assertThat(result).isEqualTo(documentFile);
      assertThat(Files.isRegularFile(result)).isTrue();
    }

    @Test
    void deveLancarIllegalArgumentExceptionQuandoCaminhoForDiretorio() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFilePath(
                      "escopos/api",
                      "dados"
              )
      )
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("O caminho informado não é um arquivo.");
    }

    @Test
    void deveLancarSecurityExceptionQuandoCaminhoSairDoEscopo() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFilePath(
                      "escopos/api",
                      "../dados/arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeForInvalido() {
      // Act & Assert
      assertThatThrownBy(
              () -> service.getFilePath(
                      "..",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }
  }
}
