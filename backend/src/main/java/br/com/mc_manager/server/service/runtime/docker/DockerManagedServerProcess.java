package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.AbstractManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ConsoleGateway;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import com.github.dockerjava.api.DockerClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.util.concurrent.ExecutorService;

public class DockerManagedServerProcess extends AbstractManagedServerProcess {
  private final DockerClient dockerClient;
  private final String containerId;
  private final Clock clock;

  public DockerManagedServerProcess(
          Server server,
          SimpMessagingTemplate template,
          ExecutorService executor,
          DockerClient dockerClient,
          String containerId,
          PlayerService playerService,
          Clock clock
  ) {
    super(server, template, executor, playerService, clock);

    this.dockerClient = dockerClient;
    this.containerId = containerId;
    this.clock = clock;
  }

  @Override
  protected ProcessLifecycle createLifecycle() {
    return new DockerProcessLifecycle(dockerClient, containerId);
  }

  @Override
  protected ConsoleGateway createConsoleGateway() {
    return new DockerConsoleGateway(dockerClient, containerId);
  }

  @Override
  protected MetricsCollector createMetricsCollector() {
    return new DockerMetricsCollector(
            server,
            dockerClient,
            (DockerProcessLifecycle) lifecycle,
            () -> startedAt,
            lineProcessor::getOnlinePlayersCount,
            clock
    );
  }
}
