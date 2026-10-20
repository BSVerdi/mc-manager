package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.service.provider.PaperProvider.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperProviderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RestClient paper;

  @InjectMocks
  private PaperProvider paperProvider;

  @Nested
  @DisplayName("type()")
  class TypeTests {

    @Test
    @DisplayName("Deve retornar o tipo PAPER")
    void deveRetornarTipoPaper() {
      ServerType resultado = paperProvider.type();

      assertThat(resultado).isEqualTo(ServerType.PAPER);
    }
  }

  @Nested
  @DisplayName("getDownloadStream(String)")
  class GetDownloadStreamTests {

    private static final String VERSION = "1.20";
    private final String buildsUrl = "/projects/paper/versions/{version}" + VERSION + "/builds";

    @Test
    @DisplayName("Deve retornar o stream do download quando há build STABLE com server:default")
    void deveRetornarStreamDeDownloadQuandoHaBuildEstavelComServerDefault() {
      // Arrange
      Download download = new Download("paper.jar", "https://example.com/paper.jar", Map.of());
      Build stable = new Build("1", "STABLE", Map.of("server:default", download));
      InputStream esperado = new ByteArrayInputStream(new byte[]{1, 2, 3});

      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenReturn(List.of(stable));
      when(paper.get().uri("https://example.com/paper.jar").retrieve().body(InputStream.class))
              .thenReturn(esperado);

      // Act
      InputStream resultado = paperProvider.getDownloadStream(VERSION);

      // Assert
      assertThat(resultado).isSameAs(esperado);
    }

    @Test
    @DisplayName("Deve usar o PRIMEIRO build STABLE quando há mais de um")
    void deveUsarPrimeiroBuildEstavelQuandoHaMaisDeUm() {
      // Arrange
      Download primeiro = new Download("1.jar", "https://example.com/1.jar", Map.of());
      Download segundo = new Download("2.jar", "https://example.com/2.jar", Map.of());
      Build stable1 = new Build("1", "STABLE", Map.of("server:default", primeiro));
      Build stable2 = new Build("2", "STABLE", Map.of("server:default", segundo));
      InputStream esperado = new ByteArrayInputStream(new byte[]{1});

      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenReturn(List.of(stable1, stable2));
      when(paper.get().uri("https://example.com/1.jar").retrieve().body(InputStream.class))
              .thenReturn(esperado);

      // Act
      InputStream resultado = paperProvider.getDownloadStream(VERSION);

      // Assert
      assertThat(resultado).isSameAs(esperado);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando não há build STABLE")
    void deveLancarIllegalStateQuandoNaoHaBuildEstavel() {
      // Arrange
      Build latest = new Build("1", "LATEST", Map.of());

      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenReturn(List.of(latest));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getDownloadStream(VERSION))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Sem build estavel para a versão " + VERSION);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando não há download server:default")
    void deveLancarNullPointerExceptionQuandoNaoHaDownloadServerDefault() {
      // Arrange
      Build stable = new Build("1", "STABLE", Map.of());

      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenReturn(List.of(stable));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getDownloadStream(VERSION))
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a chamada de builds falha")
    void devePropagarExcecaoQuandoChamadaDeBuildsFalha() {
      // Arrange
      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenThrow(new ResourceAccessException("falha na chamada de builds"));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getDownloadStream(VERSION))
              .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a chamada de download falha")
    void devePropagarExcecaoQuandoChamadaDeDownloadFalha() {
      // Arrange
      Download download = new Download("paper.jar", "https://example.com/paper.jar", Map.of());
      Build stable = new Build("1", "STABLE", Map.of("server:default", download));

      when(paper.get().uri(buildsUrl).retrieve().body(any(ParameterizedTypeReference.class)))
              .thenReturn(List.of(stable));
      when(paper.get().uri("https://example.com/paper.jar").retrieve().body(InputStream.class))
              .thenThrow(new ResourceAccessException("falha no download"));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getDownloadStream(VERSION))
              .isInstanceOf(ResourceAccessException.class);
    }
  }

  @Nested
  @DisplayName("getReleasedVersions()")
  class GetReleasedVersionsTests {

    @Test
    @DisplayName("Deve retornar apenas as versões estáveis quando há versões pre e rc")
    void deveRetornarApenasVersoesEstaveisQuandoHaVersoesPreERc() {
      // Arrange
      Map<String, List<String>> versoes = new LinkedHashMap<>();
      versoes.put("1.20", List.of("1.20.1", "1.20.2-pre", "1.20.3-rc"));
      ProjectResponse response = new ProjectResponse(versoes);

      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenReturn(response);

      // Act
      List<String> resultado = paperProvider.getReleasedVersions();

      // Assert
      assertThat(resultado).containsExactly("1.20.1");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando todas as versões são pre ou rc")
    void deveRetornarListaVaziaQuandoTodasAsVersoesSaoPreERc() {
      // Arrange
      Map<String, List<String>> versoes = new LinkedHashMap<>();
      versoes.put("1.20", List.of("1.20-pre", "1.20-rc"));
      ProjectResponse response = new ProjectResponse(versoes);

      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenReturn(response);

      // Act
      List<String> resultado = paperProvider.getReleasedVersions();

      // Assert
      assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há versões")
    void deveRetornarListaVaziaQuandoNaoHaVersoes() {
      // Arrange
      ProjectResponse response = new ProjectResponse(new HashMap<>());

      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenReturn(response);

      // Act
      List<String> resultado = paperProvider.getReleasedVersions();

      // Assert
      assertThat(resultado).isEmpty();
    }
  }

  @Nested
  @DisplayName("getAllVersions()")
  class GetAllVersionsTests {

    @Test
    @DisplayName("Deve achatar todas as versões do mapa")
    void deveAchatarTodasAsVersoesDoMapa() {
      // Arrange
      Map<String, List<String>> versoes = new LinkedHashMap<>();
      versoes.put("1.20", List.of("1.20.1", "1.20.2"));
      versoes.put("1.19", List.of("1.19.4"));
      ProjectResponse response = new ProjectResponse(versoes);

      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenReturn(response);

      // Act
      List<String> resultado = paperProvider.getAllVersions();

      // Assert
      assertThat(resultado).containsExactly("1.20.1", "1.20.2", "1.19.4");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há versões")
    void deveRetornarListaVaziaQuandoNaoHaVersoes() {
      // Arrange
      ProjectResponse response = new ProjectResponse(new HashMap<>());

      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenReturn(response);

      // Act
      List<String> resultado = paperProvider.getAllVersions();

      // Assert
      assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a chamada falha")
    void devePropagarExcecaoQuandoChamadaFalha() {
      // Arrange
      when(paper.get().uri("/projects/paper").retrieve().body(ProjectResponse.class))
              .thenThrow(new ResourceAccessException("falha na chamada"));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getAllVersions())
              .isInstanceOf(ResourceAccessException.class);
    }
  }

  @Nested
  @DisplayName("getJavaVersion(String)")
  class GetJavaVersionTests {

    @Test
    @DisplayName("Deve retornar a versão mínima de Java")
    void deveRetornarVersaoMinimaDeJava() {
      // Arrange
      VersionJson json = new VersionJson(new Java(new JavaVersion("21")));

      when(paper.get().uri("/projects/paper/versions/1.20").retrieve().body(VersionJson.class))
              .thenReturn(json);

      // Act
      String resultado = paperProvider.getJavaVersion("1.20");

      // Assert
      assertThat(resultado).isEqualTo("21");
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando não há informação de Java")
    void deveLancarNullPointerExceptionQuandoNaoHaInfoDeJava() {
      // Arrange
      VersionJson json = new VersionJson(null);

      when(paper.get().uri("/projects/paper/versions/1.20").retrieve().body(VersionJson.class))
              .thenReturn(json);

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getJavaVersion("1.20"))
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a chamada falha")
    void devePropagarExcecaoQuandoChamadaFalha() {
      // Arrange
      when(paper.get().uri("/projects/paper/versions/1.20").retrieve().body(VersionJson.class))
              .thenThrow(new ResourceAccessException("falha na chamada"));

      // Act & Assert
      assertThatThrownBy(() -> paperProvider.getJavaVersion("1.20"))
              .isInstanceOf(ResourceAccessException.class);
    }
  }
}