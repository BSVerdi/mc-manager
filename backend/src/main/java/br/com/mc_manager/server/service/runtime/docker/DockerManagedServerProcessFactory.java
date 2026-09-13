package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ManagedServerProcessFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Component
@Profile("docker")
public class DockerManagedServerProcessFactory implements ManagedServerProcessFactory {

  private static final String NETWORK_NAME = "mc-network";
  private static final String LABEL_ENABLE = "infrarust.enable";
  private static final String LABEL_DOMAIN = "infrarust.domains";

  private final DockerClient dockerClient;
  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final PlayerService playerService;
  private final Clock clock;

  public DockerManagedServerProcessFactory(
          DockerClient dockerClient,
          SimpMessagingTemplate template,
          ExecutorService executor,
          PlayerService playerService,
          Clock clock
  ) {
    this.dockerClient = dockerClient;
    this.template = template;
    this.executor = executor;
    this.playerService = playerService;
    this.clock = clock;
  }

  @Override
  public ManagedServerProcess create(Server config, String javaExecutablePath, Path directory) {
    if (config.getDomain() == null || config.getDomain().isBlank()) {
      throw new IllegalStateException("Servidor " + config.getId() + "não possui domínio configurado");
    }

    Map<String, String> labels = Map.of(
            LABEL_ENABLE, "true",
            LABEL_DOMAIN, config.getDomain()
    );

    HostConfig hostConfig = HostConfig.newHostConfig()
            .withNetworkMode(NETWORK_NAME)
            .withBinds(new Bind(directory.toString(), new Volume("/data")));

    String javaVersion = config.getJavaVersion().replace("JAVA_", "");
    String containerImage = "eclipse-temurin:" + javaVersion + "-jre";

    CreateContainerResponse container = dockerClient.createContainerCmd(containerImage)
            .withName("mc-" + config.getId())
            .withHostConfig(hostConfig)
            .withLabels(labels)
            .withWorkingDir("/data")
            .withAttachStdin(true)
            .withStdinOpen(true)
            .withCmd(
                    "Java",
                    "-Xms" + config.getMinRam(),
                    "-Xmx" + config.getMaxRam(),
                    "-jar", "server.jar",
                    "nogui"
            ).exec();

    return new DockerManagedServerProcess(
            config,
            template,
            executor,
            dockerClient,
            container.getId(),
            playerService,
            clock
    );
  }
}
