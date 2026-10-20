package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.service.provider.VanillaProvider.Downloads;
import br.com.mc_manager.server.service.provider.VanillaProvider.Entry;
import br.com.mc_manager.server.service.provider.VanillaProvider.Manifest;
import br.com.mc_manager.server.service.provider.VanillaProvider.VersionJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static br.com.mc_manager.server.service.provider.VanillaProvider.Artifact;
import static br.com.mc_manager.server.service.provider.VanillaProvider.JavaVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VanillaProvider")
class VanillaProviderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RestClient mojang;

  @InjectMocks
  private VanillaProvider vanillaProvider;

  private static final String MANIFEST_URL = "/mc/game/version_manifest_v2.json";
  private static final String VERSION_1_20_URL = "https://piston-meta.mojang.com/mc/game/versions/1.20.json";
  private static final String SERVER_JAR_URL = "https://piston-data.mojang.com/v1/objects/server-1.20.jar";
  
  private Entry entry120() {
    return new Entry("1.20", "release", VERSION_1_20_URL);
  }

  private Manifest manifestCom120() {
    return new Manifest(List.of(entry120()));
  }

  private VersionJson versionJson120() {
    return new VersionJson(
            new Downloads(new Artifact("1a2b3c", SERVER_JAR_URL)),
            new JavaVersion("21")
    );
  }

  @Nested
  @DisplayName("type")
  class TypeTests {

    @Test
    @DisplayName("Retorna o tipo do servidor como VANILLA")
    void deveRetornarVanillaComoTipo() {
      // Act
      ServerType result = vanillaProvider.type();

      // Assert
      assertThat(result).isEqualTo(ServerType.VANILLA);
    }
  }

  @Nested
  @DisplayName("getReleasedVersions")
  class GetReleasedVersionsTests {

    @Test
    @DisplayName("Filtra apenas versões do tipo release")
    void deveRetornarApenasVersoesRelease() {
      // Arrange
      List<Entry> versions = List.of(
              new Entry("1.20", "release", "https://meta/1.20.json"),
              new Entry("1.19", "release", "https://meta/1.19.json"),
              new Entry("1.20-rc1", "snapshot", "https://meta/1.20-rc1.json")
      );
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(new Manifest(versions));

      // Act
      List<String> result = vanillaProvider.getReleasedVersions();

      // Assert
      assertThat(result).containsExactly("1.20", "1.19");
    }

    @Test
    @DisplayName("Retorna lista vazia quando o manifest está vazio")
    void deveRetornarListaVaziaQuandoManifestEstiverVazio() {
      // Arrange
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(new Manifest(List.of()));

      // Act
      List<String> result = vanillaProvider.getReleasedVersions();

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getAllVersions")
  class GetAllVersionsTests {

    @Test
    @DisplayName("Retorna todas as versões (release e snapshot)")
    void deveRetornarTodasAsVersoes() {
      // Arrange
      List<Entry> versions = List.of(
              new Entry("1.20", "release", "https://meta/1.20.json"),
              new Entry("1.20-rc1", "snapshot", "https://meta/1.20-rc1.json"),
              new Entry("1.19", "release", "https://meta/1.19.json")
      );
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(new Manifest(versions));

      // Act
      List<String> result = vanillaProvider.getAllVersions();

      // Assert
      assertThat(result).containsExactly("1.20", "1.20-rc1", "1.19");
    }

    @Test
    @DisplayName("Retorna lista vazia quando o manifest está vazio")
    void deveRetornarListaVaziaQuandoManifestEstiverVazio() {
      // Arrange
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(new Manifest(List.of()));

      // Act
      List<String> result = vanillaProvider.getAllVersions();

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getJavaVersion")
  class GetJavaVersionTests {

    @Test
    @DisplayName("Retorna a major version quando a versão existe")
    void deveRetornarMajorVersionQuandoVersaoExiste() {
      // Arrange
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(manifestCom120());
      when(mojang.get().uri(URI.create(VERSION_1_20_URL)).retrieve().body(VersionJson.class))
              .thenReturn(versionJson120());

      // Act
      String result = vanillaProvider.getJavaVersion("1.20");

      // Assert
      assertThat(result).isEqualTo("21");
    }

    @Test
    @DisplayName("Lança IllegalArgumentException quando a versão não existe")
    void deveLancarIllegalArgumentExceptionQuandoVersaoNaoExiste() {
      // Arrange
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(manifestCom120());

      // Act & Assert
      assertThatThrownBy(() -> vanillaProvider.getJavaVersion("99.99"))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Versão não encontrada.");
    }
  }

  @Nested
  @DisplayName("getDownloadStream")
  class GetDownloadStreamTests {

    @Test
    @DisplayName("Retorna o InputStream do jar quando a versão existe")
    void deveRetornarInputStreamQuandoVersaoExiste() {
      // Arrange
      InputStream expected = new ByteArrayInputStream("fake-jar-bytes".getBytes(StandardCharsets.UTF_8));
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(manifestCom120());
      when(mojang.get().uri(URI.create(VERSION_1_20_URL)).retrieve().body(VersionJson.class))
              .thenReturn(versionJson120());
      when(mojang.get().uri(SERVER_JAR_URL).retrieve().body(InputStream.class))
              .thenReturn(expected);

      // Act
      InputStream result = vanillaProvider.getDownloadStream("1.20");

      // Assert
      assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Lança IllegalArgumentException quando a versão não existe")
    void deveLancarIllegalArgumentExceptionQuandoVersaoNaoExiste() {
      // Arrange
      when(mojang.get().uri(MANIFEST_URL).retrieve().body(Manifest.class))
              .thenReturn(manifestCom120());

      // Act & Assert
      assertThatThrownBy(() -> vanillaProvider.getDownloadStream("99.99"))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Versão não encontrada.");
    }
  }
}