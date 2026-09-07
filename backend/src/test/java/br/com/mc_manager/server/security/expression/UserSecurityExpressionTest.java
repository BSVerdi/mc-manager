package br.com.mc_manager.server.security.expression;

import br.com.mc_manager.server.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSecurityExpressionTest {

  @Mock
  private UserService userService;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private UserSecurityExpression userSecurityExpression;

  @Nested
  @DisplayName("isUser")
  class IsUserTest {

    @Test
    @DisplayName("Deve retornar true quando o usuário autenticado é o próprio usuário")
    void deveRetornarTrueQuandoEhOProprioUsuario() {
      // Arrange
      Long userId = 1L;
      when(authentication.getName()).thenReturn("steve");
      doNothing().when(userService).assertIsUser(userId, "steve");

      // Act
      boolean result = userSecurityExpression.isUser(userId, authentication);

      // Assert
      assertThat(result).isTrue();
      verify(userService).assertIsUser(userId, "steve");
    }

    @Test
    @DisplayName("Deve retornar false quando o usuário autenticado não é o próprio usuário")
    void deveRetornarFalseQuandoNaoEhOProprioUsuario() {
      // Arrange
      Long userId = 1L;
      when(authentication.getName()).thenReturn("alex");
      doThrow(new SecurityException("Você não tem permissão para realizar essa ação"))
              .when(userService).assertIsUser(userId, "alex");

      // Act
      boolean result = userSecurityExpression.isUser(userId, authentication);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve propagar o username do Authentication corretamente para o service")
    void devePropagarUsernameDoAuthenticationParaOService() {
      // Arrange
      Long userId = 42L;
      when(authentication.getName()).thenReturn("nickname_especifico");

      // Act
      userSecurityExpression.isUser(userId, authentication);

      // Assert
      verify(userService).assertIsUser(eq(userId), eq("nickname_especifico"));
    }

    @Test
    @DisplayName("Não deve capturar exceções diferentes de SecurityException")
    void naoDeveCapturarExcecoesDiferentesDeSecurityException() {
      // Arrange — só SecurityException é tratada; qualquer outra exceção (ex: usuário
      // inexistente lançando NoSuchElementException) deve propagar, não virar "false" silenciosamente
      Long userId = 999L;
      when(authentication.getName()).thenReturn("steve");
      doThrow(new NoSuchElementException("Usuário não encontrado"))
              .when(userService).assertIsUser(userId, "steve");

      // Act & Assert
      assertThatThrownBy(() -> userSecurityExpression.isUser(userId, authentication))
              .isInstanceOf(NoSuchElementException.class)
              .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando authentication é nulo")
    void deveLancarNullPointerExceptionQuandoAuthenticationENulo() {
      // Arrange
      Long userId = 1L;

      // Act & Assert — authentication.getName() é chamado antes de qualquer outra lógica
      assertThatThrownBy(() -> userSecurityExpression.isUser(userId, null))
              .isInstanceOf(NullPointerException.class);

      verifyNoInteractions(userService);
    }
  }
}