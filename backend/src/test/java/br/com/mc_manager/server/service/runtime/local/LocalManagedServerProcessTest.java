package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ConsoleGateway;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import oshi.SystemInfo;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LocalManagedServerProcessTest {


  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ExecutorService executor;

  @Mock
  private PlayerService playerService;

  @Mock
  private SystemInfo systemInfo;

  @Mock
  private DefaultServerProcessFactory processFactory;

  @Mock
  private Clock clock;

  private LocalManagedServerProcess process;
  private Server server;

  @BeforeEach
  void setUp() {
    server = createServerWithAllAttributes();

    process = new LocalManagedServerProcess(
            server, template, executor,
            "/usr/bin/java", Path.of("/tmp/server"),
            playerService, systemInfo, processFactory, clock
    );
  }

  private ProcessLifecycle invokeCreateLifecycle() throws Exception {
    Method m = LocalManagedServerProcess.class.getDeclaredMethod("createLifecycle");
    m.setAccessible(true);
    return (ProcessLifecycle) m.invoke(process);
  }

  private ConsoleGateway invokeCreateConsoleGateway() throws Exception {
    Method m = LocalManagedServerProcess.class.getDeclaredMethod("createConsoleGateway");
    m.setAccessible(true);
    return (ConsoleGateway) m.invoke(process);
  }

  private MetricsCollector invokeCreateMetricsCollector() throws Exception {
    Method m = LocalManagedServerProcess.class.getDeclaredMethod("createMetricsCollector");
    m.setAccessible(true);
    return (MetricsCollector) m.invoke(process);
  }

  @Nested
  @DisplayName("createLifecycle")
  class CreateLifecycleTests {

    @Test
    @DisplayName("deveCriarLocalProcessLifeCycleQuandoChamarCreateLifecycle")
    void deveCriarLocalProcessLifeCycleQuandoChamarCreateLifecycle() throws Exception {
      // Act
      ProcessLifecycle result = invokeCreateLifecycle();

      // Assert
      assertThat(result).isInstanceOf(LocalProcessLifeCycle.class);
    }
  }

  @Nested
  @DisplayName("createConsoleGateway")
  class CreateConsoleGatewayTests {

    @Test
    @DisplayName("deveCriarLocalConsoleGatewayQuandoLifecycleJáEstiverPopulado")
    void deveCriarLocalConsoleGatewayQuandoLifecycleJáEstiverPopulado() throws Exception {
      // Arrange
      LocalProcessLifeCycle localLc = mock(LocalProcessLifeCycle.class);
      ReflectionTestUtils.setField(process, "lifecycle", localLc);

      // Act
      ConsoleGateway result = invokeCreateConsoleGateway();

      // Assert
      assertThat(result).isInstanceOf(LocalConsoleGateway.class);
    }

    @Test
    @DisplayName("deveLancarClassCastExceptionQuandoLifecycleNãoForLocalProcessLifeCycle")
    void deveLancarClassCastExceptionQuandoLifecycleNãoForLocalProcessLifeCycle() {
      // Arrange
      ProcessLifecycle notLocal = mock(ProcessLifecycle.class);
      ReflectionTestUtils.setField(process, "lifecycle", notLocal);

      // Act & Assert
      assertThatThrownBy(() -> invokeCreateConsoleGateway())
              .isInstanceOf(Exception.class)
              .hasCauseInstanceOf(ClassCastException.class);
    }
  }

  @Nested
  @DisplayName("createMetricsCollector")
  class CreateMetricsCollectorTests {

    @Test
    @DisplayName("deveCriarLocalMetricsCollectorQuandoTodosOsCamposEstiveremPopulados")
    void deveCriarLocalMetricsCollectorQuandoTodosOsCamposEstiveremPopulados() throws Exception {
      // Arrange — populate campos herdados do pai (AbstractManagedServerProcess)
      LocalProcessLifeCycle localLc = mock(LocalProcessLifeCycle.class);
      ReflectionTestUtils.setField(process, "lifecycle", localLc);

      // lineProcessor é um campo do pai; ajuste o tipo conforme a assinatura real
      // Exemplo: LineProcessor lineProcessor = mock(LineProcessor.class);
      // ReflectionTestUtils.setField(process, "lineProcessor", lineProcessor);

      ReflectionTestUtils.setField(process, "startedAt", Instant.now());

      // Act
      MetricsCollector result = invokeCreateMetricsCollector();

      // Assert
      assertThat(result).isInstanceOf(LocalMetricsCollector.class);
    }
  }
}