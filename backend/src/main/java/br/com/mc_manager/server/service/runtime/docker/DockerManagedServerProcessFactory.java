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
import org.springframework.beans.factory.annotation.Value;
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
  private static final String LABEL_PORT = "infrarust.port";
  private static final Map<String, String> JAVA_IMAGES = Map.of(
          "JAVA_8", "eclipse-temurin:8-jre",
          "JAVA_21", "eclipse-temurin:21-jre",
          "JAVA_25", "eclipse-temurin:25-jre"
  );
  private final DockerClient dockerClient;
  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final PlayerService playerService;
  private final Clock clock;
  private final Path hostServerDirectory;


  public DockerManagedServerProcessFactory(
          DockerClient dockerClient,
          SimpMessagingTemplate template,
          ExecutorService executor,
          PlayerService playerService,
          Clock clock,

          @Value("${mcmanager.host_data_root_directory}")
          String hostServerDirectory

  ) {
    this.dockerClient = dockerClient;
    this.template = template;
    this.executor = executor;
    this.playerService = playerService;
    this.clock = clock;
    this.hostServerDirectory = Path.of(hostServerDirectory);
  }

  @Override
  public ManagedServerProcess create(Server config, String javaExecutablePath, Path directory) {
    if (config.getDomain() == null || config.getDomain().isBlank()) {
      throw new IllegalStateException("Servidor " + config.getId() + "não possui domínio configurado");
    }

    Path hostPath = hostServerDirectory.resolve(String.valueOf(config.getId()));

    Map<String, String> labels = Map.of(
            LABEL_ENABLE, "true",
            LABEL_DOMAIN, config.getDomain()
    );

    HostConfig hostConfig = HostConfig.newHostConfig()
            .withNetworkMode(NETWORK_NAME)
            .withBinds(new Bind(hostPath.toString(), new Volume("/data")));

    String javaVersion = config.getJavaVersion();
    String containerImage = JAVA_IMAGES.get(javaVersion);
    String containerName = config.getDomain().substring(0, config.getDomain().indexOf("."));

    CreateContainerResponse container = dockerClient.createContainerCmd(containerImage)
            .withName(containerName)
            .withHostConfig(hostConfig)
            .withLabels(labels)
            .withWorkingDir("/data")
            .withAttachStdin(true)
            .withStdinOpen(true)
            .withCmd(
                    "java",
                    "-Xms" + config.getMinRam(),
                    "-Xmx" + config.getMaxRam(),
                    "-jar", "server.jar",
                    "nogui"
            ).exec();

    DockerManagedServerProcess process = new DockerManagedServerProcess(
            config,
            template,
            executor,
            dockerClient,
            container.getId(),
            playerService,
            clock
    );

    process.init();
    return process;
  }
}
