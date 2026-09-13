package br.com.mc_manager.server.service.runtime.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static br.com.mc_manager.server.service.runtime.util.RamUnitParser.toMb;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RamUnitParser")
class RamUnitParserTest {

  @Nested
  @DisplayName("Conversão bem-sucedida")
  class ConversaoBemSucedida {

    @Test
    @DisplayName("deve converter G para MB quando entrada termina em G")
    void deveConverterGParaMBQuandoEntradaTerminaEmG() {
      assertThat(toMb("4G")).isEqualTo(4096);
    }

    @Test
    @DisplayName("deve converter M para MB quando entrada termina em M")
    void deveConverterMParaMBQuandoEntradaTerminaEmM() {
      assertThat(toMb("512M")).isEqualTo(512);
    }

    @Test
    @DisplayName("deve interpretar valor sem sufixo como MB")
    void deveInterpretarValorSemSufixoComoMB() {
      assertThat(toMb("1024")).isEqualTo(1024);
    }

    @Test
    @DisplayName("deve converter G para MB quando sufixo é minúsculo")
    void deveConverterGParaMBQuandoSufixoEhMinusculo() {
      assertThat(toMb("4g")).isEqualTo(4096);
    }

    @Test
    @DisplayName("deve converter G para MB quando há espaços em branco")
    void deveConverterGParaMBQuandoHaEspacosEmBranco() {
      assertThat(toMb("  4G  ")).isEqualTo(4096);
    }
  }

  @Nested
  @DisplayName("Entradas nulas ou vazias")
  class EntradasNulasOuVazias {

    @Test
    @DisplayName("deve lançar NullPointerException quando entrada é null")
    void deveLancarNullPointerExceptionQuandoEntradaEhNull() {
      assertThatThrownBy(() -> toMb(null))
              .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deve lançar NumberFormatException quando entrada é string vazia")
    void deveLancarNumberFormatExceptionQuandoEntradaEhStringVazia() {
      assertThatThrownBy(() -> toMb(""))
              .isInstanceOf(NumberFormatException.class);
    }
  }

  @Nested
  @DisplayName("Entradas inválidas")
  class EntradasInvalidas {

    @Test
    @DisplayName("deve lançar NumberFormatException quando entrada é apenas sufixo G")
    void deveLancarNumberFormatExceptionQuandoEntradaEhApenasSufixoG() {
      assertThatThrownBy(() -> toMb("G"))
              .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("deve lançar NumberFormatException quando entrada é apenas sufixo M")
    void deveLancarNumberFormatExceptionQuandoEntradaEhApenasSufixoM() {
      assertThatThrownBy(() -> toMb("M"))
              .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("deve lançar NumberFormatException quando entrada não é numérica")
    void deveLancarNumberFormatExceptionQuandoEntradaNaoEhNumerica() {
      assertThatThrownBy(() -> toMb("abc"))
              .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("deve lançar NumberFormatException quando entrada contém decimal")
    void deveLancarNumberFormatExceptionQuandoEntradaContemDecimal() {
      assertThatThrownBy(() -> toMb("12.5"))
              .isInstanceOf(NumberFormatException.class);
    }
  }

  @Nested
  @DisplayName("Limites e valores especiais")
  class LimitesEValoresEspeciais {

    @Test
    @DisplayName("deve retornar zero quando entrada é 0")
    void deveRetornarZeroQuandoEntradaEhZero() {
      assertThat(toMb("0")).isEqualTo(0);
    }

    @Test
    @DisplayName("deve retornar zero quando entrada é 0G")
    void deveRetornarZeroQuandoEntradaEhZeroG() {
      assertThat(toMb("0G")).isEqualTo(0);
    }

    @Test
    @DisplayName("deve retornar valor negativo quando entrada é -1G")
    void deveRetornarValorNegativoQuandoEntradaEhNegativoG() {
      assertThat(toMb("-1G")).isEqualTo(-1024);
    }
  }
}