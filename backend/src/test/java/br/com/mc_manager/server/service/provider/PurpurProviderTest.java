package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.service.provider.PurpurProvider.LatestBuild;
import br.com.mc_manager.server.service.provider.PurpurProvider.ProjectResponse;
import br.com.mc_manager.server.service.provider.PurpurProvider.VersionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PurpurProviderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RestClient purpur;

  @InjectMocks
  private PurpurProvider provider;

  @Nested
  @DisplayName("type()")
  class TypeTests {

    @Test
    @DisplayName("deveRetornarPurpurQuandoConsultarTipo")
    void deveRetornarPurpurQuandoConsultarTipo() {
      assertThat(provider.type()).isEqualTo(ServerType.PURPUR);
    }
  }

  @Nested
  @DisplayName("getDownloadStream()")
  class GetDownloadStreamTests {

    @Test
    @DisplayName("deveRetornarStreamDoDownloadQuandoVersaoExiste")
    void deveRetornarStreamDoDownloadQuandoVersaoExiste() {
      InputStream streamEsperado = new ByteArrayInputStream(new byte[]{1, 2, 3});

      given(purpur.get().uri("/purpur/1.20.4").retrieve().body(VersionResponse.class))
              .willReturn(new VersionResponse(new LatestBuild("build456")));
      given(purpur.get().uri("/purpur/1.20.4/build456/download").retrieve().body(InputStream.class))
              .willReturn(streamEsperado);

      InputStream resultado = provider.getDownloadStream("1.20.4");

      assertThat(resultado).isSameAs(streamEsperado);
    }

    @Test
    @DisplayName("devePropagarExcecaoQuandoDownloadRetornaErro")
    void devePropagarExcecaoQuandoDownloadRetornaErro() {
      given(purpur.get().uri("/purpur/1.20.4").retrieve().body(VersionResponse.class))
              .willReturn(new VersionResponse(new LatestBuild("build456")));
      given(purpur.get().uri("/purpur/1.20.4/build456/download").retrieve().body(InputStream.class))
              .willThrow(new RestClientResponseException("Not Found", HttpStatus.NOT_FOUND, "", new HttpHeaders(), null, null));

      assertThatThrownBy(() -> provider.getDownloadStream("1.20.4"))
              .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    @DisplayName("deveLancarNullPointerExceptionQuandoBuildsVierNulo")
    void deveLancarNullPointerExceptionQuandoBuildsVierNulo() {
      given(purpur.get().uri("/purpur/1.20.4").retrieve().body(VersionResponse.class))
              .willReturn(new VersionResponse(null));

      assertThatThrownBy(() -> provider.getDownloadStream("1.20.4"))
              .isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("getReleasedVersions()")
  class GetReleasedVersionsTests {

    @Test
    @DisplayName("deveRetornarVersoesReversasQuandoConsultarVersoesLiberadas")
    void deveRetornarVersoesReversasQuandoConsultarVersoesLiberadas() {
      given(purpur.get().uri("/purpur").retrieve().body(ProjectResponse.class))
              .willReturn(new ProjectResponse(List.of("1.20.4", "1.21", "2.0", "2.10.6")));

      List<String> resultado = provider.getReleasedVersions();

      assertThat(resultado).containsExactly("2.10.6", "2.0", "1.21", "1.20.4");
    }
  }

  @Nested
  @DisplayName("getAllVersions()")
  class GetAllVersionsTests {

    @Test
    @DisplayName("deveRetornarVersoesReversasQuandoConsultarTodasAsVersoes")
    void deveRetornarVersoesReversasQuandoConsultarTodasAsVersoes() {
      given(purpur.get().uri("/purpur").retrieve().body(ProjectResponse.class))
              .willReturn(new ProjectResponse(List.of("1.20.4", "1.21", "2.0", "2.10.6")));

      List<String> resultado = provider.getAllVersions();

      assertThat(resultado).containsExactly("2.10.6", "2.0", "1.21", "1.20.4");
    }

    @Test
    @DisplayName("deveLancarNullPointerExceptionQuandoVersionsVierNulo")
    void deveLancarNullPointerExceptionQuandoVersionsVierNulo() {
      given(purpur.get().uri("/purpur").retrieve().body(ProjectResponse.class))
              .willReturn(new ProjectResponse(null));

      assertThatThrownBy(() -> provider.getAllVersions())
              .isInstanceOf(NullPointerException.class);
    }
  }
}