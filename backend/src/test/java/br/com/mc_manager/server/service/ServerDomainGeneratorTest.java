package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerDomainGeneratorTest {

  private static final String BASE_DOMAIN = "mcmanager.com.br";

  private ServerDomainGenerator generator;

  @Mock
  private Server server;

  @BeforeEach
  void setUp() {
    generator = new ServerDomainGenerator();
    ReflectionTestUtils.setField(generator, "baseDomain", BASE_DOMAIN);
  }

  @Nested
  @DisplayName("generate(server)")
  class Generate {

    @Test
    @DisplayName("deveGerarDominioCorretoQuandoNomeEIdValidos")
    void deveGerarDominioCorretoQuandoNomeEIdValidos() {
      when(server.getName()).thenReturn("Meu Servidor 1.21");
      when(server.getId()).thenReturn(12345678L);

      String result = generator.generate(server);

      assertThat(result).isEqualTo("meuservidor121-12345678." + BASE_DOMAIN);
    }

    @Test
    @DisplayName("deveLancarNullPointerExceptionQuandoNomeForNulo")
    void deveLancarNullPointerExceptionQuandoNomeForNulo() {
      when(server.getName()).thenReturn(null);

      assertThrows(NullPointerException.class, () -> generator.generate(server));
    }

    @Test
    @DisplayName("deveLancarNullPointerExceptionQuandoIdForNulo")
    void deveLancarNullPointerExceptionQuandoIdForNulo() {
      when(server.getName()).thenReturn("Teste");
      when(server.getId()).thenReturn(null);

      assertThrows(NullPointerException.class, () -> generator.generate(server));
    }
    
    @Test
    @DisplayName("deveGerarDominioComSlugVazioQuandoNomeForVazio")
    void deveGerarDominioComSlugVazioQuandoNomeForVazio() {
      when(server.getName()).thenReturn("");
      when(server.getId()).thenReturn(12345678L);

      String result = generator.generate(server);

      assertThat(result).isEqualTo("-12345678." + BASE_DOMAIN);
    }

    @Test
    @DisplayName("deveGerarDominioComSlugVazioQuandoNomeSoCarteresEspeciais")
    void deveGerarDominioComSlugVazioQuandoNomeSoCarteresEspeciais() {
      when(server.getName()).thenReturn("!!!???");
      when(server.getId()).thenReturn(12345678L);

      String result = generator.generate(server);

      assertThat(result).isEqualTo("-12345678." + BASE_DOMAIN);
    }

    @Test
    @DisplayName("deveRemoverAcentosDoSlugQuandoNomeContemCaracteresAcentuados")
    void deveRemoverAcentosDoSlugQuandoNomeContemCaracteresAcentuados() {
      when(server.getName()).thenReturn("Café com Leites 1.21");
      when(server.getId()).thenReturn(12345678L);

      String result = generator.generate(server);

      assertThat(result).isEqualTo("cafcomleites121-12345678." + BASE_DOMAIN);
    }

    @Test
    @DisplayName("deveUsarTodosOs8DigitosDoIdQuandoIdTerExatamente8Digitos")
    void deveUsarTodosOs8DigitosDoIdQuandoIdTerExatamente8Digitos() {
      when(server.getName()).thenReturn("Meu Servidor");
      when(server.getId()).thenReturn(12345678L);

      String result = generator.generate(server);

      assertThat(result).isEqualTo("meuservidor-12345678." + BASE_DOMAIN);
    }
  }
}