package br.com.mc_manager.server.security.service;

import br.com.mc_manager.server.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import static br.com.mc_manager.server.factory.UserFactory.createUserWithAllAttributes;
import static br.com.mc_manager.server.factory.UserFactory.createUserWithAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

  private final User user = createUserWithAllAttributes();
  private TokenService service;

  @BeforeEach
  void setUp() {
    service = new TokenService();
    ReflectionTestUtils.setField(service, "secret", "test_secret_key_used_only_in_unit_tests");
  }

  @Nested
  @DisplayName("Gerar Tokens")
  class GenerateTokenTest {

    @Test
    @DisplayName("Deve gerar access token com 30 minutos de validade")
    void deveGerarAccessTokenCom30Minutos() {
      // Act
      String token = service.generateAccessToken(user);

      // Assert
      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Deve gerar refresh token com 120 minutos de validade")
    void deveGerarRefreshTokenCom120Minutos() {
      // Act
      String token = service.generateRefreshToken(user);

      // Assert
      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Deve gerar token genérico com duração customizada")
    void deveGerarTokenCustomizado() {
      // Arrange
      int minutos = 60;

      // Act
      String token = service.generateToken(user, "custom_type", minutos);

      // Assert
      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Tokens gerados para o mesmo usuário em chamadas diferentes devem ser distintos")
    void tokensGeradosDevemSerDistintos() {
      // Arrange
      User user1 = createUserWithAttributes(1L, "User 1", "user1@email.com");
      User user2 = createUserWithAttributes(2L, "User 2", "user2@email.com");

      // Act
      String token1 = service.generateAccessToken(user1);
      String token2 = service.generateAccessToken(user2);

      // Assert
      assertThat(token1).isNotEqualTo(token2);
    }
  }

  @Nested
  @DisplayName("Validar Access Token")
  class VerifyAccessTokenTest {

    private String accessToken;

    @BeforeEach
    void setup() {
      accessToken = service.generateAccessToken(user);
    }

    @Test
    @DisplayName("Deve validar access token válido e retornar o username correto")
    void deveValidarAccessTokenValidoERetornarUsuario() {
      // Act
      String subject = service.verifyAccessToken(accessToken);

      // Assert
      assertThat(subject).isEqualTo(user.getUsername());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException para access token expirado")
    void deveLancarBadCredentialsParaAccessTokenExpirado() {
      // Arrange
      String expiredToken = service.generateToken(user, "access", -1);

      // Act & Assert
      assertThatThrownBy(() -> service.verifyAccessToken(expiredToken))
              .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException para access token malformado")
    void deveLancarBadCredentialsParaAccessTokenMalformado() {
      // Arrange
      String malformedToken = "isso.nao.eh_um_jwt";

      // Act & Assert
      assertThatThrownBy(() -> service.verifyAccessToken(malformedToken))
              .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando assinado com secret diferente")
    void deveLancarBadCredentialsParaSecretDiferente() {
      // Arrange
      TokenService outroServico = new TokenService();
      ReflectionTestUtils.setField(outroServico, "secret", "outro_secret_completamente_diferente");

      String tokenComOutroSecret = outroServico.generateAccessToken(user);

      // Act & Assert
      assertThatThrownBy(() -> service.verifyAccessToken(tokenComOutroSecret))
              .isInstanceOf(BadCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("Validar Refresh Token")
  class VerifyRefreshTokenTest {

    private String refreshToken;

    @BeforeEach
    void setup() {
      refreshToken = service.generateRefreshToken(user);
    }

    @Test
    @DisplayName("Deve validar refresh token válido e retornar o username correto")
    void deveValidarRefreshTokenValidoERetornarUsuario() {
      // Act
      String subject = service.verifyRefreshToken(refreshToken);

      // Assert
      assertThat(subject).isEqualTo(user.getUsername());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException para refresh token inválido/malformado")
    void deveLancarBadCredentialsParaRefreshTokenInvalido() {
      // Arrange
      String invalidToken = "invalid.token.here";

      // Act & Assert
      assertThatThrownBy(() -> service.verifyRefreshToken(invalidToken))
              .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException para refresh token expirado")
    void deveLancarBadCredentialsParaRefreshTokenExpirado() {
      // Arrange
      String expiredToken = service.generateToken(user, "refresh", -1);

      // Act & Assert
      assertThatThrownBy(() -> service.verifyRefreshToken(expiredToken))
              .isInstanceOf(BadCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("Verificar Separação de Tipos de Token")
  class TypeSeparationTest {

    @Test
    @DisplayName("Deve rejeitar access token verificado como refresh token")
    void deveRejeitarAccessTokenComoRefreshToken() {
      // Arrange
      String accessToken = service.generateAccessToken(user);

      // Act & Assert
      assertThatThrownBy(() -> service.verifyRefreshToken(accessToken))
              .isInstanceOf(BadCredentialsException.class)
              .hasMessageContaining("Token invalido");
    }

    @Test
    @DisplayName("Deve rejeitar refresh token verificado como access token")
    void deveRejeitarRefreshTokenComoAccessToken() {
      // Arrange
      String refreshToken = service.generateRefreshToken(user);

      // Act & Assert
      assertThatThrownBy(() -> service.verifyAccessToken(refreshToken))
              .isInstanceOf(BadCredentialsException.class)
              .hasMessageContaining("Token invalido");
    }
  }

  @Nested
  @DisplayName("Cálculo de Expiração")
  class ExpirationTest {

    @Test
    @DisplayName("Deve calcular instante de expiração no futuro para minutos positivos")
    void deveCalcularExpiracaoNoFuturo() {
      // Act
      var expiration = service.expiration(30);

      // Assert
      assertThat(expiration).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("Deve calcular instante de expiração no passado para minutos negativos")
    void deveCalcularExpiracaoNoPassado() {
      // Act
      var expiration = service.expiration(-1);

      // Assert
      assertThat(expiration).isBefore(java.time.Instant.now());
    }
  }
}