package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.MinecraftManifest.ManifestEntryDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftVersionDetailsDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftVersionDto;
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
      List<MinecraftVersionDto> versoesEsperadas = List.of(
              new MinecraftVersionDto("v1_20", "release", "https://piston-data.mojang.com/v1_20.json"),
              new MinecraftVersionDto("v1_19", "snapshot", "https://piston-data.mojang.com/v1_19.json")
      );
      ManifestEntryDto manifest = new ManifestEntryDto(versoesEsperadas);

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(manifest);

      // Act
      List<MinecraftVersionDto> resultado = service.getMinecraftServerVersions();

      // Assert
      assertThat(resultado).isEqualTo(versoesEsperadas);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o manifesto não tem versões")
    void deveRetornarListaVaziaQuandoManifestoVazio() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(Collections.emptyList()));

      // Act
      List<MinecraftVersionDto> resultado = service.getMinecraftServerVersions();

      // Assert
      assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve propagar NullPointerException quando a resposta do manifesto é nula")
    void devePropagarNullPointerExceptionQuandoRespostaNula() {
      // Arrange — response.versions() é chamado sobre um retorno nulo do RestClient
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> service.getMinecraftServerVersions())
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve sempre consultar a URL fixa do manifesto de versões")
    void deveConsultarUrlFixaDoManifesto() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(Collections.emptyList()));

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
      MinecraftVersionDto versaoNoManifesto = new MinecraftVersionDto("v1_20", "release", versionUrl);
      ManifestEntryDto manifest = new ManifestEntryDto(List.of(versaoNoManifesto));

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(manifest);

      MinecraftVersionDetailsDto detalhesEsperados = createMinecraftVersionDetailsDtoWithAllAttributes();
      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDto.class))
              .willReturn(detalhesEsperados);

      // Act
      MinecraftVersionDetailsDto resultado = service.getMinecraftServerVersionDetails("v1_20");

      // Assert
      assertThat(resultado).isEqualTo(detalhesEsperados);
    }

    @Test
    @DisplayName("Deve consultar o manifesto e depois a URL específica da versão encontrada")
    void deveConsultarManifestoEDepoisUrlDaVersao() {
      // Arrange
      String versionUrl = "https://piston-data.mojang.com/v1/packages/26.2.json";
      ManifestEntryDto manifest = new ManifestEntryDto(
              List.of(new MinecraftVersionDto("26.2", "release", versionUrl))
      );

      MinecraftVersionDetailsDto returnedData = createMinecraftVersionDetailsDtoWithAllAttributes();

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(manifest);

      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDto.class))
              .willReturn(returnedData);

      clearInvocations(restClient, restClient.get());


      // Act
      service.getMinecraftServerVersionDetails("26.2");

      // Assert — duas URLs diferentes devem ter sido consultadas
      verify(restClient.get()).uri(VERSION_MANIFEST_URL);
      verify(restClient.get()).uri(versionUrl);
    }

    @Test
    @DisplayName("Deve lançar ObjectNotFoundException quando a versão não existe no manifesto")
    void deveLancarObjectNotFoundExceptionParaVersaoInexistente() {
      // Arrange
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(Collections.emptyList()));

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
      ManifestEntryDto manifest = new ManifestEntryDto(
              List.of(new MinecraftVersionDto("v24w01a", "snapshot", versionUrl))
      );

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(manifest);

      MinecraftVersionDetailsDto detalhesEsperados = createMinecraftVersionDetailsDtoWithId("v24w01a");
      given(restClient.get().uri(versionUrl).retrieve().body(MinecraftVersionDetailsDto.class))
              .willReturn(detalhesEsperados);

      // Act
      MinecraftVersionDetailsDto resultado = service.getMinecraftServerVersionDetails("v24w01a");

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
      List<MinecraftVersionDto> todasVersoes = List.of(
              createMinecraftVersionDtoWithIdAndType("v1_20", "release"),
              createMinecraftVersionDtoWithIdAndType("v24w01a", "snapshot"),
              createMinecraftVersionDtoWithIdAndType("v1_19", "release")
      );

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(todasVersoes));

      // Act
      List<MinecraftVersionDto> releases = service.getReleasedMinecraftServerVersions();

      // Assert
      assertThat(releases)
              .hasSize(2)
              .extracting(MinecraftVersionDto::id)
              .containsExactly("v1_20", "v1_19");
      assertThat(releases).allMatch(v -> "release".equals(v.type()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando só existem snapshots")
    void deveRetornarListaVaziaQuandoApenasSnapshots() {
      // Arrange
      MinecraftVersionDto v1 = createMinecraftVersionDtoWithIdAndType("v24w01a", "snapshot");
      MinecraftVersionDto v2 = createMinecraftVersionDtoWithIdAndType("v23w56a", "snapshot");

      List<MinecraftVersionDto> apenasSnapshots = List.of(v1, v2);

      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(apenasSnapshots));

      // Act
      List<MinecraftVersionDto> releases = service.getReleasedMinecraftServerVersions();

      // Assert
      assertThat(releases).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o manifesto não tem versões")
    void deveRetornarListaVaziaQuandoManifestoVazio() {
      given(restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class))
              .willReturn(new ManifestEntryDto(Collections.emptyList()));

      List<MinecraftVersionDto> releases = service.getReleasedMinecraftServerVersions();

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
      // Arrange — o serviço não faz nenhuma validação própria; se o RestClient
      // retornar um corpo nulo, o serviço apenas repassa esse null adiante.
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