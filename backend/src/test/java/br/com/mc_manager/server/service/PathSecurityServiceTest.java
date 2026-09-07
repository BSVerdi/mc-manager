package br.com.mc_manager.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    Files.createDirectories(dataDirectory);
    Files.createFile(documentFile);

    ReflectionTestUtils.setField(
            service,
            "dataRootDirectory",
            baseDirectory.toString()
    );
  }

  @Nested
  class ResolveSafePath {

    @Test
    void deveResolverCaminhoValido() {
      Path result = service.resolveSafePath(
              "escopos/api",
              "dados.json"
      );

      assertThat(result)
              .isEqualTo(scopeDirectory.resolve("dados.json"));
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeSaiDoDiretorioBase() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      "..",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeSaiDoDiretorioBaseAtravesDeTraversal() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      "escopos/api/../../../../",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }

    @Test
    void deveLancarSecurityExceptionQuandoPathSaiDoEscopoPermitido() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      "escopos/api",
                      "../../arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoPathTentaAcessarDiretorioPai() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      "escopos/api",
                      "../documento.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveNormalizarCaminhoComCurrentDirectory() {
      Path result = service.resolveSafePath(
              "escopos/./api",
              "./dados.json"
      );

      assertThat(result)
              .isEqualTo(scopeDirectory.resolve("dados.json"));
    }

    @Test
    void deveNormalizarParentDirectorySemSairDoEscopo() {
      Path result = service.resolveSafePath(
              "escopos/api",
              "dados/../documento.txt"
      );

      assertThat(result)
              .isEqualTo(documentFile);
    }

    @Test
    void devePermitirScopeVazio() {
      Path result = service.resolveSafePath(
              "",
              ""
      );

      assertThat(result)
              .isEqualTo(baseDirectory);
    }

    @Test
    void deveLancarNullPointerExceptionQuandoScopeForNull() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      null,
                      "arquivo.txt"
              )
      )
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveLancarNullPointerExceptionQuandoPathForNull() {
      assertThatThrownBy(() ->
              service.resolveSafePath(
                      "escopos/api",
                      null
              )
      )
              .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class GetFolderPath {

    @Test
    void deveRetornarDiretorioValido() {
      Path result = service.getFolderPath(
              "escopos/api",
              "dados"
      );

      assertThat(result)
              .isEqualTo(dataDirectory);

      assertThat(Files.isDirectory(result))
              .isTrue();
    }

    @Test
    void deveLancarIllegalArgumentExceptionQuandoCaminhoForArquivo() {
      assertThatThrownBy(() ->
              service.getFolderPath(
                      "escopos/api",
                      "documento.txt"
              )
      )
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("O caminho informado não é um diretório");
    }

    @Test
    void deveLancarSecurityExceptionQuandoCaminhoSairDoEscopo() {
      assertThatThrownBy(() ->
              service.getFolderPath(
                      "escopos/api",
                      "../documento.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeForInvalido() {
      assertThatThrownBy(() ->
              service.getFolderPath(
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
      Path result = service.getFilePath(
              "escopos/api",
              "documento.txt"
      );

      assertThat(result)
              .isEqualTo(documentFile);

      assertThat(Files.isRegularFile(result))
              .isTrue();
    }

    @Test
    void deveLancarIllegalArgumentExceptionQuandoCaminhoForDiretorio() {
      assertThatThrownBy(() ->
              service.getFilePath(
                      "escopos/api",
                      "dados"
              )
      )
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("O caminho informado não é um arquivo.");
    }

    @Test
    void deveLancarSecurityExceptionQuandoCaminhoSairDoEscopo() {
      assertThatThrownBy(() ->
              service.getFilePath(
                      "escopos/api",
                      "../dados/arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do escopo permitido");
    }

    @Test
    void deveLancarSecurityExceptionQuandoScopeForInvalido() {
      assertThatThrownBy(() ->
              service.getFilePath(
                      "..",
                      "arquivo.txt"
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Caminho fora do diretório do servidor");
    }
  }
}
