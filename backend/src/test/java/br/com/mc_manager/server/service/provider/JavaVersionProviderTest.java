package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.service.provider.VanillaProvider.VersionJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaVersionProviderTest {

  @Mock
  private VanillaProvider vanillaProvider;

  @Nested
  @DisplayName("getJavaVersion")
  class GetJavaVersionTests {

    private JavaVersionProvider javaVersionProvider;

    @BeforeEach
    void setUp() {
      javaVersionProvider = new JavaVersionProvider(vanillaProvider);
    }

    private VersionJson umVersionJsonComJava(String majorVersion) {
      VanillaProvider.Artifact serverQualquer = new VanillaProvider.Artifact("sha1", "url");
      VanillaProvider.Downloads downloads = new VanillaProvider.Downloads(serverQualquer);
      VanillaProvider.JavaVersion javaVersion = new VanillaProvider.JavaVersion(majorVersion);
      return new VersionJson(downloads, javaVersion);
    }

    @ParameterizedTest
    @CsvSource({"8, 8", "10, 17", "17, 17", "18, 21", "25, 25"})
    @DisplayName("Deve retornar a menor versão disponível quando a requerida está dentro do intervalo")
    void deveRetornarMenorVersaoDisponivelQuandoRequeridaEstadentroDoIntervalo(String requerida, String esperada) {
      // Arrange
      when(vanillaProvider.getVersionJson("1.20.4")).thenReturn(umVersionJsonComJava(requerida));

      // Act
      String javaVersion = javaVersionProvider.getJavaVersion("1.20.4");

      // Assert
      assertThat(javaVersion).isEqualTo(esperada);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando a versão requerida excede todas as disponíveis")
    void deveLancarIllegalStateExceptionQuandoRequeridaExcedeTodasAsDisponiveis() {
      // Arrange
      when(vanillaProvider.getVersionJson("1.21.0")).thenReturn(umVersionJsonComJava("26"));

      // Act & Assert
      assertThatThrownBy(() -> javaVersionProvider.getJavaVersion("1.21.0"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Sem java disponível para 26");
    }

    @Test
    @DisplayName("Deve lançar NumberFormatException quando a versão requerida não é numérica")
    void deveLancarNumberFormatExceptionQuandoVersaoNaoEhNumerica() {
      // Arrange
      when(vanillaProvider.getVersionJson("1.20.4")).thenReturn(umVersionJsonComJava("abc"));

      // Act & Assert
      assertThatThrownBy(() -> javaVersionProvider.getJavaVersion("1.20.4"))
              .isInstanceOf(NumberFormatException.class);
    }
  }
}