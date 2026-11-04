package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FabricProviderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RestClient fabric;

  @InjectMocks
  private FabricProvider provider;

  @Nested
  @DisplayName("type()")
  class TypeTests {

    @Test
    @DisplayName("Retorna ServerType.FABRIC")
    void deveRetornarFabricQuandoConsultado() {
      ServerType tipo = provider.type();

      assertThat(tipo).isEqualTo(ServerType.FABRIC);
    }
  }

  @Nested
  @DisplayName("getDownloadStream")
  class GetDownloadStreamTests {

    private static final String LOADER_URI = "/versions/loader";
    private static final String INSTALLER_URI = "/versions/installer";
    private static final String JAR_URI = "/versions/loader/{version}/{loader}/{installer}/server/jar";

    @Test
    @DisplayName("Retorna o stream do jar quando existem versões estáveis de loader e installer")
    void deveRetornarStreamDoJarQuandoExisteVersaoEstavel() {
      String loader = "0.16.0";
      String installer = "0.17.0";

      given(fabric.get().uri(LOADER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion(loader, true)));
      given(fabric.get().uri(INSTALLER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion(installer, true)));
      InputStream streamEsperado = mock(InputStream.class);
      given(fabric.get().uri(JAR_URI, "1.20.1", loader, installer).retrieve().body(InputStream.class))
              .willReturn(streamEsperado);

      InputStream stream = provider.getDownloadStream("1.20.1");

      // isSameAs: se a URL ou os parâmetros estivessem errados, o stub não casaria
      // e o deep stub retornaria outra instância, não streamEsperado
      assertThat(stream).isSameAs(streamEsperado);
    }

    @Test
    @DisplayName("Usa a primeira versão estável da lista, não a primeira entrada")
    void deveUsarPrimeiraVersaoEstavelQuandoHouverInstavelAntes() {
      String loaderEstavel = "0.16.0";
      String installer = "0.17.0";

      given(fabric.get().uri(LOADER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(
                      new FabricProvider.ComponentVersion("0.15.11", false),
                      new FabricProvider.ComponentVersion(loaderEstavel, true)
              ));
      given(fabric.get().uri(INSTALLER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion(installer, true)));
      InputStream streamEsperado = mock(InputStream.class);
      given(fabric.get().uri(JAR_URI, "1.20.1", loaderEstavel, installer).retrieve().body(InputStream.class))
              .willReturn(streamEsperado);

      InputStream stream = provider.getDownloadStream("1.20.1");

      // O stub da URL do jar foi configurado com "0.16.0" (2ª entrada): se o filtro
      // usasse a 1ª ("0.15.11"), o stub não casaria e stream não seria streamEsperado
      assertThat(stream).isSameAs(streamEsperado);
    }

    @Test
    @DisplayName("Lança IllegalStateException quando não há versão estável de loader")
    void deveLancarExcecaoQuandoNaoHaVersaoEstavelDeLoader() {
      given(fabric.get().uri(LOADER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion("0.15.11", false)));

      assertThatThrownBy(() -> provider.getDownloadStream("1.20.1"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Nenhuma versão estável em /versions/loader");
    }

    @Test
    @DisplayName("Lança IllegalStateException quando não há versão estável de installer")
    void deveLancarExcecaoQuandoNaoHaVersaoEstavelDeInstaller() {
      given(fabric.get().uri(LOADER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion("0.16.0", true)));
      given(fabric.get().uri(INSTALLER_URI).retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(new FabricProvider.ComponentVersion("0.16.2", false)));

      assertThatThrownBy(() -> provider.getDownloadStream("1.20.1"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Nenhuma versão estável em /versions/installer");
    }
  }

  @Nested
  @DisplayName("getReleasedVersions")
  class GetReleasedVersionsTests {

    @Test
    @DisplayName("Retorna apenas as versões estáveis, na ordem original")
    void deveRetornarApenasEstaveisQuandoListaContemVersoesMistas() {
      given(fabric.get().uri("/versions/game").retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(
                      new FabricProvider.GameVersion("1.20.1", true),
                      new FabricProvider.GameVersion("1.21", false),
                      new FabricProvider.GameVersion("1.19.4", true)
              ));

      List<String> versoes = provider.getReleasedVersions();

      assertThat(versoes).containsExactly("1.20.1", "1.19.4");
    }

    @Test
    @DisplayName("Retorna lista vazia quando não há versões")
    void deveRetornarListaVaziaQuandoNaoHaVersoes() {
      given(fabric.get().uri("/versions/game").retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of());

      List<String> versoes = provider.getReleasedVersions();

      assertThat(versoes).isEmpty();
    }

    @Test
    @DisplayName("Retorna lista vazia quando nenhuma versão é estável")
    void deveRetornarListaVaziaQuandoNenhumaVersaoEhEstavel() {
      given(fabric.get().uri("/versions/game").retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(
                      new FabricProvider.GameVersion("25w31a", false),
                      new FabricProvider.GameVersion("1.21", false)
              ));

      List<String> versoes = provider.getReleasedVersions();

      assertThat(versoes).isEmpty();
    }
  }

  @Nested
  @DisplayName("getAllVersions")
  class GetAllVersionsTests {

    @Test
    @DisplayName("Retorna todas as versões, estáveis e instáveis, na ordem original")
    void deveRetornarTodasAsVersoesQuandoListaContemMistas() {
      given(fabric.get().uri("/versions/game").retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of(
                      new FabricProvider.GameVersion("1.20.1", true),
                      new FabricProvider.GameVersion("1.21", false),
                      new FabricProvider.GameVersion("1.19.4", true)
              ));

      List<String> versoes = provider.getAllVersions();

      assertThat(versoes).containsExactly("1.20.1", "1.21", "1.19.4");
    }

    @Test
    @DisplayName("Retorna lista vazia quando não há versões")
    void deveRetornarListaVaziaQuandoNaoHaVersoes() {
      given(fabric.get().uri("/versions/game").retrieve().body(any(ParameterizedTypeReference.class)))
              .willReturn(List.of());

      List<String> versoes = provider.getAllVersions();

      assertThat(versoes).isEmpty();
    }
  }

  @Nested
  @DisplayName("getJavaVersion")
  class GetJavaVersionTests {

    @Test
    @DisplayName("Lança IllegalArgumentException quando a versão é nula")
    void deveLancarExcecaoQuandoVersaoEhNula() {
      assertThatThrownBy(() -> provider.getJavaVersion(null))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Versão não pode ser nula");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1"})
    @DisplayName("Lança IllegalArgumentException quando a versão não segue o formato")
    void deveLancarExcecaoQuandoVersaoNaoSegueFormato(String versao) {
      assertThatThrownBy(() -> provider.getJavaVersion(versao))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Versão inválida: " + versao);
    }

    @Test
    @DisplayName("Lança IllegalArgumentException quando a versão é vazia")
    void deveLancarExcecaoQuandoVersaoEhVazia() {
      assertThatThrownBy(() -> provider.getJavaVersion(""))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Versão inválida: ");
    }

    @ParameterizedTest
    @CsvSource({
            "1.8.9, 8",
            "1.16.5, 8",
            "1.17.1, 17",
            "1.19.4, 17",
            "1.20.1, 21",
            "25.0.1, 21",
            "26.0, 25",
            "30.1, 25"
    })
    @DisplayName("Retorna a versão do Java correspondente quando a versão é válida")
    void deveRetornarVersaoDoJavaCorrespondenteQuandoVersaoEhValida(String versao, String javaEsperado) {
      String java = provider.getJavaVersion(versao);

      assertThat(java).isEqualTo(javaEsperado);
    }
  }
}