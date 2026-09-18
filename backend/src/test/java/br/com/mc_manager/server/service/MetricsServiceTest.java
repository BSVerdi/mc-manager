package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  @Mock
  private ServerRuntimeService serverManagerService;

  @Mock
  private SimpMessagingTemplate template;

  @InjectMocks
  private MetricsService metricsService;

  @Nested
  @DisplayName("broadcastAllMetrics")
  class BroadcastAllMetricsTests {

    private ServerMetricsDTO dtoServidor1;
    private ServerMetricsDTO dtoServidor2;

    @BeforeEach
    void setUp() {
      dtoServidor1 = mock(ServerMetricsDTO.class);
      dtoServidor2 = mock(ServerMetricsDTO.class);
    }

    @Test
    @DisplayName("Deve publicar métricas de todos os servidores quando todos têm métricas")
    void devePublicarMetricasDeTodosOsServidoresQuandoTodosTemMetricas() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(Set.of(1L, 2L));
      when(serverManagerService.getMetrics(1L)).thenReturn(dtoServidor1);
      when(serverManagerService.getMetrics(2L)).thenReturn(dtoServidor2);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template).convertAndSend(eq("/topic/1/metrics"), eq(dtoServidor1));
      verify(template).convertAndSend(eq("/topic/2/metrics"), eq(dtoServidor2));
    }

    @Test
    @DisplayName("Deve publicar métricas de um único servidor com sucesso")
    void devePublicarMetricasDeUmUnicoServidorComSucesso() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(Set.of(1L));
      when(serverManagerService.getMetrics(1L)).thenReturn(dtoServidor1);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, times(1)).convertAndSend(eq("/topic/1/metrics"), eq(dtoServidor1));
      verifyNoMoreInteractions(template);
    }

    @Test
    @DisplayName("Não deve publicar nada quando a lista de servidores ativos é vazia")
    void naoDevePublicarNadaQuandoListaDeServidoresAtivosEhVazia() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(Collections.emptySet());

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(serverManagerService, never()).getMetrics(anyLong());
      verifyNoMoreInteractions(template);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a lista de servidores ativos é null")
    void deveLancarNullPointerExceptionQuandoListaDeServidoresAtivosEhNull() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(null);

      // Act & Assert
      assertThatNullPointerException()
              .isThrownBy(() -> metricsService.broadcastAllMetrics());
    }

    @Test
    @DisplayName("Não deve publicar métricas quando getMetrics retorna null")
    void naoDevePublicarMetricasQuandoGetMetricsRetornaNull() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(Set.of(1L));
      when(serverManagerService.getMetrics(1L)).thenReturn(null);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verifyNoMoreInteractions(template);
    }

    @Test
    @DisplayName("Deve publicar apenas dos servidores com dados quando há métricas nulas em alguns")
    void devePublicarApenasDosServidoresComDadosQuandoHouMetricasNulas() {
      // Arrange
      when(serverManagerService.getActiveServersIds()).thenReturn(Set.of(1L));
      when(serverManagerService.getMetrics(1L)).thenReturn(dtoServidor1);

      // Act
      metricsService.broadcastAllMetrics();

      // Assert
      verify(template, times(1)).convertAndSend(eq("/topic/1/metrics"), eq(dtoServidor1));
      verifyNoMoreInteractions(template);
    }
  }
}