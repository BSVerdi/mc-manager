package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.minecraftManifest.ManifestEntryDTO;
import br.com.mc_manager.server.dto.minecraftManifest.MinecraftVersionDTO;
import br.com.mc_manager.server.dto.minecraftManifest.MinecraftVersionDetailsDTO;
import org.hibernate.ObjectNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static br.com.mc_manager.server.factory.MinecraftVersionDtoFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinecraftMetaDataServiceTest {

  private static final String VERSION_MANIFEST_URL =
          "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RestClient restClient;

  @InjectMocks
  private MinecraftMetaDataService service;

  @Nested
  @DisplayName("getMinecraftServerVersions")
  class GetMinecraftServerVersionsTest {

    @Test
    @DisplayName("Deve retornar as versões do manifesto com sucesso")
    void deveRetornarVersoesComSucesso() {
      // Arrange
      List<MinecraftVersionDTO> versoesEsperadas = List.of(
              new MinecraftVersionDTO("v1_20", "release", "https://piston-data.mojang.com/v1_20.json"),
              new MinecraftVersionDTO("v1_19", "snapshot", "https://piston-data.mojang.com/v1_19.json")
      );
      ManifestEntryDTO manifest = new ManifestEntryDTO(versoesEsperadas);

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(manifest);

      // Act
      List<MinecraftVersionDTO> resultado = service.getMinecraftServerVersions();

      // Assert
      assertThat(resultado).isEqualTo(versoesEsperadas);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o manifesto não tem versões")
    void deveRetornarListaVaziaQuandoManifestoVazio() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(Collections.emptyList()));

      // Act
      List<MinecraftVersionDTO> resultado = service.getMinecraftServerVersions();

      // Assert
      assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve propagar NullPointerException quando a resposta do manifesto é nula")
    void devePropagarNullPointerExceptionQuandoRespostaNula() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> service.getMinecraftServerVersions())
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve sempre consultar a URL fixa do manifesto de versões")
    void deveConsultarUrlFixaDoManifesto() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(Collections.emptyList()));

      clearInvocations(restClient, restClient.get());

      // Act
      service.getMinecraftServerVersions();

      // Assert
      verify(restClient.get()).uri(VERSION_MANIFEST_URL);
    }
  }

  @Nested
  @DisplayName("getMinecraftServerVersionDetails")
  class GetMinecraftServerVersionDetailsTest {

    @Test
    @DisplayName("Deve retornar detalhes de uma versão existente")
    void deveRetornarDetalhesDeVersaoExistente() {
      // Arrange
      String versionUrl = "https://piston-data.mojang.com/v1/packages/v1_20.json";
      MinecraftVersionDTO versaoNoManifesto = new MinecraftVersionDTO("v1_20", "release", versionUrl);
      ManifestEntryDTO manifest = new ManifestEntryDTO(List.of(versaoNoManifesto));

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(manifest);

      MinecraftVersionDetailsDTO detalhesEsperados = createMinecraftVersionDetailsDtoWithAllAttributes();
      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDTO.class))
              .willReturn(detalhesEsperados);

      // Act
      MinecraftVersionDetailsDTO resultado = service.getMinecraftServerVersionDetails("v1_20");

      // Assert
      assertThat(resultado).isEqualTo(detalhesEsperados);
    }

    @Test
    @DisplayName("Deve consultar o manifesto e depois a URL específica da versão encontrada")
    void deveConsultarManifestoEDepoisUrlDaVersao() {
      // Arrange
      String versionUrl = "https://piston-data.mojang.com/v1/packages/26.2.json";
      ManifestEntryDTO manifest = new ManifestEntryDTO(
              List.of(new MinecraftVersionDTO("26.2", "release", versionUrl))
      );

      MinecraftVersionDetailsDTO returnedData = createMinecraftVersionDetailsDtoWithAllAttributes();

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(manifest);

      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDTO.class))
              .willReturn(returnedData);

      clearInvocations(restClient, restClient.get());


      // Act
      service.getMinecraftServerVersionDetails("26.2");

      // Assert
      verify(restClient.get()).uri(VERSION_MANIFEST_URL);
      verify(restClient.get()).uri(versionUrl);
    }

    @Test
    @DisplayName("Deve lançar ObjectNotFoundException quando a versão não existe no manifesto")
    void deveLancarObjectNotFoundExceptionParaVersaoInexistente() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(Collections.emptyList()));

      // Act & Assert
      assertThatThrownBy(() -> service.getMinecraftServerVersionDetails("v_inexistente"))
              .isInstanceOf(ObjectNotFoundException.class)
              .hasMessageContaining("Versão v_inexistente não encontrada.");
    }

    @Test
    @DisplayName("Deve funcionar da mesma forma para versões do tipo snapshot")
    void deveRetornarDetalhesParaVersaoSnapshot() {
      // Arrange
      String versionUrl = "https://piston-data.mojang.com/v1/packages/v24w01a.json";
      ManifestEntryDTO manifest = new ManifestEntryDTO(
              List.of(new MinecraftVersionDTO("v24w01a", "snapshot", versionUrl))
      );

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(manifest);

      MinecraftVersionDetailsDTO detalhesEsperados = createMinecraftVersionDetailsDtoWithId("v24w01a");
      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDTO.class))
              .willReturn(detalhesEsperados);

      // Act
      MinecraftVersionDetailsDTO resultado = service.getMinecraftServerVersionDetails("v24w01a");

      // Assert
      assertThat(resultado).isEqualTo(detalhesEsperados);
    }
  }

  @Nested
  @DisplayName("getReleasedMinecraftServerVersions")
  class GetReleasedMinecraftServerVersionsTest {

    @Test
    @DisplayName("Deve retornar apenas versões do tipo release, excluindo snapshots")
    void deveRetornarApenasReleasesExcluindoSnapshots() {
      // Arrange
      List<MinecraftVersionDTO> todasVersoes = List.of(
              createMinecraftVersionDtoWithIdAndType("v1_20", "release"),
              createMinecraftVersionDtoWithIdAndType("v24w01a", "snapshot"),
              createMinecraftVersionDtoWithIdAndType("v1_19", "release")
      );

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(todasVersoes));

      // Act
      List<MinecraftVersionDTO> releases = service.getReleasedMinecraftServerVersions();

      // Assert
      assertThat(releases)
              .hasSize(2)
              .extracting(MinecraftVersionDTO::id)
              .containsExactly("v1_20", "v1_19");
      assertThat(releases).allMatch(v -> "release".equals(v.type()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando só existem snapshots")
    void deveRetornarListaVaziaQuandoApenasSnapshots() {
      // Arrange
      MinecraftVersionDTO v1 = createMinecraftVersionDtoWithIdAndType("v24w01a", "snapshot");
      MinecraftVersionDTO v2 = createMinecraftVersionDtoWithIdAndType("v23w56a", "snapshot");

      List<MinecraftVersionDTO> apenasSnapshots = List.of(v1, v2);

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(apenasSnapshots));

      // Act
      List<MinecraftVersionDTO> releases = service.getReleasedMinecraftServerVersions();

      // Assert
      assertThat(releases).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o manifesto não tem versões")
    void deveRetornarListaVaziaQuandoManifestoVazio() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDTO.class))
              .willReturn(new ManifestEntryDTO(Collections.emptyList()));

      // Act
      List<MinecraftVersionDTO> releases = service.getReleasedMinecraftServerVersions();

      // Assert
      assertThat(releases).isEmpty();
    }
  }

  @Nested
  @DisplayName("getServerInputStream")
  class GetServerInputStreamTest {

    @Test
    @DisplayName("Deve retornar o InputStream do jar para a URL informada")
    void deveRetornarInputStreamDoJar() {
      // Arrange
      String jarUrl = "https://piston-data.mojang.com/v1/versions/1_20/server.jar";
      InputStream mockInputStream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});

      given(restClient.get().uri(jarUrl).retrieve().body(InputStream.class))
              .willReturn(mockInputStream);

      // Act
      InputStream resultado = service.getServerInputStream(jarUrl);

      // Assert
      assertThat(resultado).isSameAs(mockInputStream);
    }

    @Test
    @DisplayName("Deve consultar exatamente a URL informada, sem transformação")
    void deveConsultarExatamenteAUrlInformada() {
      // Arrange
      String jarUrl = "https://piston-data.mojang.com/v1/versions/1_20/server.jar";
      given(restClient.get().uri(jarUrl).retrieve().body(InputStream.class))
              .willReturn(new java.io.ByteArrayInputStream(new byte[0]));

      clearInvocations(restClient, restClient.get());

      // Act
      service.getServerInputStream(jarUrl);

      // Assert
      verify(restClient.get(), times(1)).uri(eq(jarUrl));
    }

    @Test
    @DisplayName("Deve retornar null quando o RestClient não encontra o jar (corpo nulo)")
    void deveRetornarNullQuandoRestClientNaoEncontraJar() {
      // Arrange
      String jarUrl = "https://piston-data.mojang.com/v1/versions/inexistente/server.jar";
      given(restClient.get().uri(jarUrl).retrieve().body(InputStream.class))
              .willReturn(null);

      // Act
      InputStream resultado = service.getServerInputStream(jarUrl);

      // Assert
      assertThat(resultado).isNull();
    }
  }
}