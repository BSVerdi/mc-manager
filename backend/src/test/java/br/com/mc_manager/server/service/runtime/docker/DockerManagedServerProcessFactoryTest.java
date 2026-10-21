package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;

import static br.com.mc_manager.server.factory.ServerFactory.createServerWithAllAttributes;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerManagedServerProcessFactory")
class DockerManagedServerProcessFactoryTest {

  @Mock
  private DockerClient dockerClient;
  @Mock
  private SimpMessagingTemplate template;
  @Mock
  private ExecutorService executor;
  @Mock
  private PlayerService playerService;
  @Mock
  private DockerServerContainerFactory containerFactory;

  private DockerManagedServerProcessFactory factory;

  @BeforeEach
  void setUp() {
    factory = new DockerManagedServerProcessFactory(
            dockerClient,
            template,
            executor,
            playerService,
            containerFactory,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    );
  }

  @Test
  @DisplayName("retorna DockerManagedServerProcess já inicializado")
  void deveRetornarProcessoInicializado() {
    Server server = createServerWithAllAttributes();

    ManagedServerProcess result = factory.create(server, "/usr/bin/java", Path.of("/data"));

    assertThat(result).isInstanceOf(DockerManagedServerProcess.class);
    assertThat(ReflectionTestUtils.getField(result, "lifecycle")).isInstanceOf(DockerProcessLifecycle.class);
    assertThat(ReflectionTestUtils.getField(result, "consoleGateway")).isInstanceOf(DockerConsoleGateway.class);
    assertThat(ReflectionTestUtils.getField(result, "metricsCollector")).isInstanceOf(DockerMetricsCollector.class);
  }

  @Test
  @DisplayName("não cria container no create(); isso só acontece no start()")
  void naoDeveCriarContainerNoCreate() {
    factory.create(createServerWithAllAttributes(), "/usr/bin/java", Path.of("/data"));

    org.mockito.Mockito.verifyNoInteractions(containerFactory, dockerClient);
  }
}