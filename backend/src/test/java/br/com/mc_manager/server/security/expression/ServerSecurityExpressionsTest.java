package br.com.mc_manager.server.security.expression;

import br.com.mc_manager.server.service.ServerManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.NoSuchElementException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerSecurityExpressionsTest {

  @Mock
  private ServerManagementService managementService;

  @InjectMocks
  private ServerSecurityExpressions expressions;

  @Test
  void isOwner_authentication_donoRetornaTrue() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("steve");
    doNothing().when(managementService).assertOwnership(1L, "steve");

    // Act & Assert
    assertThat(expressions.isOwner(1L, auth)).isTrue();
  }

  @Test
  void isOwner_authentication_naoDonoRetornaFalse() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("alex");
    doThrow(new SecurityException()).when(managementService).assertOwnership(1L, "alex");

    // Act & Assert
    assertThat(expressions.isOwner(1L, auth)).isFalse();
  }

  @Test
  void isOwner_stringUsername_donoRetornaTrue() {
    // Arrange
    doNothing().when(managementService).assertOwnership(1L, "steve");

    // Act & Assert
    assertThat(expressions.isOwner(1L, "steve")).isTrue();
  }

  @Test
  void isOwner_stringUsername_naoDonoRetornaFalse() {
    // Arrange
    doThrow(new SecurityException()).when(managementService).assertOwnership(1L, "alex");

    // Act & Assert
    assertThat(expressions.isOwner(1L, "alex")).isFalse();
  }

  @Test
  void isOwner_outraExcecaoNaoECapturada() {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("steve");
    doThrow(new NoSuchElementException()).when(managementService).assertOwnership(1L, "steve");

    // Act & Assert
    assertThatThrownBy(() -> expressions.isOwner(1L, auth))
            .isInstanceOf(NoSuchElementException.class);
  }
}