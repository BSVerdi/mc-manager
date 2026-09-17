package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.LogLineDTO;
import br.com.mc_manager.server.model.ServerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServerBroadcasterTest {

  private static final Long SERVER_ID = 42L;
  private static final Clock FIXED_CLOCK =
          Clock.fixed(Instant.parse("2025-01-15T10:30:00Z"), ZoneOffset.UTC);

  @Mock
  private SimpMessagingTemplate template;

  private ServerBroadcaster broadcaster;

  @BeforeEach
  void setUp() {
    broadcaster = new ServerBroadcaster(SERVER_ID, template, FIXED_CLOCK);
  }

  @Nested
  @DisplayName("Método status()")
  class StatusMethodTests {

    @Test
    @DisplayName("deveEnviarStatusNoTopicCorretoQuandoChamadoComStatus")
    void deveEnviarStatusNoTopicCorretoQuandoChamadoComStatus() {
      // Arrange
      ServerStatus status = ServerStatus.ONLINE;

      // Act
      broadcaster.status(status);

      // Assert
      verify(template).convertAndSend("/topic/status/42", status);
    }
  }

  @Nested
  @DisplayName("Método consoleLine()")
  class ConsoleLineMethodTests {

    @Test
    @DisplayName("deveEnviarLogLineComIdSequencialETimestampFixoQuandoChamado")
    void deveEnviarLogLineComIdSequencialETimestampFixoQuandoChamado() {
      // Arrange
      String message = "Server started";
      LogLineDTO expected = new LogLineDTO(1L, message, "2025-01-15T10:30:00Z");

      // Act
      broadcaster.consoleLine(message);

      // Assert
      verify(template).convertAndSend("/topic/console/42", expected);
    }

    @Test
    @DisplayName("deveIncrementarIdSequencialmenteQuandoChamadoDuasVezes")
    void deveIncrementarIdSequencialmenteQuandoChamadoDuasVezes() {
      // Arrange
      LogLineDTO first = new LogLineDTO(1L, "linha 1", "2025-01-15T10:30:00Z");
      LogLineDTO second = new LogLineDTO(2L, "linha 2", "2025-01-15T10:30:00Z");

      // Act
      broadcaster.consoleLine("linha 1");
      broadcaster.consoleLine("linha 2");

      // Assert
      ArgumentCaptor<LogLineDTO> captor = forClass(LogLineDTO.class);
      verify(template, times(2)).convertAndSend(eq("/topic/console/42"), captor.capture());
      assertThat(captor.getAllValues()).containsExactly(first, second);
    }

    @Test
    @DisplayName("deveEnviarLogLineComMensagemVaziaQuandoMensagemForVazia")
    void deveEnviarLogLineComMensagemVaziaQuandoMensagemForVazia() {
      // Arrange
      LogLineDTO expected = new LogLineDTO(1L, "", "2025-01-15T10:30:00Z");

      // Act
      broadcaster.consoleLine("");

      // Assert
      verify(template).convertAndSend("/topic/console/42", expected);
    }

    @Test
    @DisplayName("deveEnviarLogLineComMensagemNulaQuandoMensagemForNula")
    void deveEnviarLogLineComMensagemNulaQuandoMensagemForNula() {
      // Arrange
      LogLineDTO expected = new LogLineDTO(1L, null, "2025-01-15T10:30:00Z");

      // Act
      broadcaster.consoleLine(null);

      // Assert
      verify(template).convertAndSend("/topic/console/42", expected);
    }
  }
}