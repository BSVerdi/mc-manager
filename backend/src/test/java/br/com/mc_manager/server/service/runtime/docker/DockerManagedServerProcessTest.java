package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ConsoleGateway;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import br.com.mc_manager.server.service.runtime.ServerConsoleLineProcessor;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DockerManagedServerProcessTest {

  @Mock
  private SimpMessagingTemplate template;

  @Mock
  private ExecutorService executor;

  @Mock
  private DockerClient dockerClient;

  @Mock
  private PlayerService playerService;

  @Mock
  private DockerServerContainerFactory containerFactory;

  private DockerManagedServerProcess process;
  private Server server;

  @BeforeEach
  void setUp() {
    server = createServerWithAllAttributes();

    process = new DockerManagedServerProcess(
            server,
            template,
            executor,
            dockerClient,
            containerFactory,
            playerService,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    );
  }

  @Nested
  @DisplayName("createLifecycle")
  class CreateLifecycleTests {

    @Test
    @DisplayName("deveRetornarDockerProcessLifecycleQuandoCriaLifecycle")
    void deveRetornarDockerProcessLifecycleQuandoCriaLifecycle() {
      // Act
      ProcessLifecycle result = process.createLifecycle();

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(DockerProcessLifecycle.class);
    }
  }

  @Nested
  @DisplayName("createConsoleGateway")
  class CreateConsoleGatewayTests {

    @Test
    @DisplayName("deveRetornarDockerConsoleGatewayQuandoCriaConsoleGateway")
    void deveRetornarDockerConsoleGatewayQuandoCriaConsoleGateway() {
      process.createLifecycle();

      // Act
      ConsoleGateway result = process.createConsoleGateway();

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(DockerConsoleGateway.class);
    }
  }

  @Nested
  @DisplayName("createMetricsCollector")
  class CreateMetricsCollectorTests {

    @Test
    @DisplayName("deveRetornarDockerMetricsCollectorQuandoCriaMetricsCollector")
    void deveRetornarDockerMetricsCollectorQuandoCriaMetricsCollector() {
      // Arrange
      ServerConsoleLineProcessor lineProcessor = mock(ServerConsoleLineProcessor.class);
      ReflectionTestUtils.setField(process, "lineProcessor", lineProcessor);

      // Act
      MetricsCollector result = process.createMetricsCollector();

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(DockerMetricsCollector.class);
    }
  }
}