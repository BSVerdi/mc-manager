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
  private final DockerServerContainerFactory containerFactory;
  private final Clock clock;

  private DockerProcessLifecycle processLifecycle;

  public DockerManagedServerProcess(
          Server server,
          SimpMessagingTemplate template,
          ExecutorService executor,
          DockerClient dockerClient,
          DockerServerContainerFactory containerFactory,
          PlayerService playerService,
          Clock clock
  ) {
    super(server, template, executor, playerService, clock);

    this.dockerClient = dockerClient;
    this.containerFactory = containerFactory;
    this.clock = clock;
  }

  @Override
  protected ProcessLifecycle createLifecycle() {
    processLifecycle = new DockerProcessLifecycle(dockerClient, () -> containerFactory.create(super.server));
    return processLifecycle;
  }

  @Override
  protected ConsoleGateway createConsoleGateway() {
    return new DockerConsoleGateway(dockerClient, processLifecycle::getContainerId);
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
