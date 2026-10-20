package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerProviderRegistryTest {

  private static final ServerType TIPO_A = ServerType.values()[0];
  private static final ServerType TIPO_B = ServerType.values()[1];

  @Nested
  @DisplayName("Construção do índice (construtor)")
  class ConstrutorTests {

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoDoisProvidersCompartilhamOMesmoTipo")
    void deveLancarIllegalStateExceptionQuandoDoisProvidersCompartilhamOMesmoTipo() {
      // Arrange
      MinecraftProvider providerA = mock(MinecraftProvider.class);
      MinecraftProvider providerB = mock(MinecraftProvider.class);
      when(providerA.type()).thenReturn(TIPO_A);
      when(providerB.type()).thenReturn(TIPO_A); // ambos reportam o MESMO tipo

      // Act & Assert
      assertThatThrownBy(() -> new ServerProviderRegistry(List.of(providerA, providerB)))
              .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("deveAceitarListaVaziaQuandoNenhumProviderForFornecido")
    void deveAceitarListaVaziaQuandoNenhumProviderForFornecido() {
      // Act
      ServerProviderRegistry service = new ServerProviderRegistry(List.of());

      // Assert
      assertThatThrownBy(() -> service.get(TIPO_A))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Tipo desconhecido");
    }
  }

  @Nested
  @DisplayName("get — consulta por tipo")
  class GetTests {

    @Mock
    private MinecraftProvider providerA;
    @Mock
    private MinecraftProvider providerB;

    private ServerProviderRegistry service;

    @BeforeEach
    void setUp() {
      when(providerA.type()).thenReturn(TIPO_A);
      when(providerB.type()).thenReturn(TIPO_B);
      service = new ServerProviderRegistry(List.of(providerA, providerB));
    }

    @Test
    @DisplayName("deveRetornarOMesmoProviderCadastradoQuandoTipoExisteNoIndice")
    void deveRetornarOMesmoProviderCadastradoQuandoTipoExisteNoIndice() {
      // Act
      MinecraftProvider resultadoA = service.get(TIPO_A);
      MinecraftProvider resultadoB = service.get(TIPO_B);

      // Assert
      assertThat(resultadoA).isSameAs(providerA);
      assertThat(resultadoB).isSameAs(providerB);
    }

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoTipoForNulo")
    void deveLancarIllegalStateExceptionQuandoTipoForNulo() {
      // Act & Assert
      assertThatThrownBy(() -> service.get(null))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Tipo desconhecido");
    }

    @Test
    @DisplayName("deveLancarIllegalStateExceptionQuandoTipoNaoEstaNoIndice")
    void deveLancarIllegalStateExceptionQuandoTipoNaoEstaNoIndice() {
      // Arrange
      MinecraftProvider unico = mock(MinecraftProvider.class);
      when(unico.type()).thenReturn(TIPO_A);
      ServerProviderRegistry serviceParcial = new ServerProviderRegistry(List.of(unico));

      // Act & Assert
      assertThatThrownBy(() -> serviceParcial.get(TIPO_B))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Tipo desconhecido");
    }
  }
}