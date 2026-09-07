package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.service.ServerRuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ConsoleWebSocketControllerTest {

  @Mock
  private ServerRuntimeService serverManagerService; // Mock como STUB [test-double-stub]

  @InjectMocks
  private ConsoleWebSocketController controller; // Spy para manter comportamento real do controller se necessário [test-double-spy]

  @Test
  @DisplayName("Deve enviar comando para o servidor correto com sucesso")
  void deveEnviarComandoStartParaServidor() {
    // Arrange
    Long serverId = 1L;
    String command = "/gamemode creative Steve";

    // Act
    controller.handleCommand(serverId, command);

    // Assert
    verify(serverManagerService).sendCommand(serverId, command);
    verifyNoMoreInteractions(serverManagerService);
  }

}