package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ManagedServerProcessFactory;
import com.github.dockerjava.api.DockerClient;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;

@Component
@Profile("docker")
public class DockerManagedServerProcessFactory implements ManagedServerProcessFactory {

  private final DockerClient dockerClient;
  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final PlayerService playerService;
  private final DockerServerContainerFactory containerFactory;
  private final Clock clock;

  public DockerManagedServerProcessFactory(
          DockerClient dockerClient,
          SimpMessagingTemplate template,
          ExecutorService executor,
          PlayerService playerService,
          DockerServerContainerFactory containerFactory,
          Clock clock
  ) {
    this.dockerClient = dockerClient;
    this.template = template;
    this.executor = executor;
    this.playerService = playerService;
    this.containerFactory = containerFactory;
    this.clock = clock;
  }

  @Override
  public ManagedServerProcess create(Server config, String javaExecutablePath, Path directory) {
    DockerManagedServerProcess process = new DockerManagedServerProcess(
            config,
            template,
            executor,
            dockerClient,
            containerFactory,
            playerService,
            clock
    );

    process.init();
    return process;
  }
}
