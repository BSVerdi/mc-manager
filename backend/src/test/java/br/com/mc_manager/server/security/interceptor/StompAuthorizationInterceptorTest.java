package br.com.mc_manager.server.security.interceptor;

import br.com.mc_manager.server.security.expression.ServerSecurityExpressions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthorizationInterceptorTest {

  @Mock
  private ServerSecurityExpressions serverSecurity;

  @InjectMocks
  private StompAuthorizationInterceptor interceptor;

  @Test
  void comandoNaoChecado_passaDireto() {
    // Arrange
    Message message = buildMessage(StompCommand.CONNECT, "/topic/console/1", "steve");

    // Act
    Message result = interceptor.preSend(message, mock(MessageChannel.class));

    // Assert
    assertThat(result).isSameAs(message);
    verifyNoInteractions(serverSecurity);
  }

  @Test
  void destinoSemServerId_passaDireto() {
    // Arrange
    Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/outraCoisa", "steve");

    // Act
    Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

    // Assert
    assertThat(result).isSameAs(message);
    verifyNoInteractions(serverSecurity);
  }

  @Test
  void semUsuarioAutenticado_lancaIllegalStateException() {
    // Arrange
    Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/console/1", null);

    // Act & Assert
    assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("sem usuário autenticado");
  }

  @Test
  void usuarioNaoEhDono_lancaSecurityException() {
    // Arrange
    when(serverSecurity.isOwner(1L, "alex")).thenReturn(false);
    Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/console/1", "alex");

    // Act & Assert
    assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("servidor 1");
  }

  @Test
  void usuarioEhDono_permiteEDevolveMessage() {
    // Arrange
    when(serverSecurity.isOwner(1L, "steve")).thenReturn(true);
    Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/console/1", "steve");

    // Act
    Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

    // Assert
    assertThat(result).isSameAs(message);
  }

  @Test
  void destinoComFormatoInvalido_naoExtraiServerId_passaDireto() {
    // Arrange
    Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/console/abc", "steve");

    // Act
    Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

    // Assert
    assertThat(result).isSameAs(message);
    verifyNoInteractions(serverSecurity);
  }

  private Message<?> buildMessage(StompCommand command, String destination, String user) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setDestination(destination);
    if (user != null) {
      accessor.setUser(() -> user); // Principal via lambda
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}