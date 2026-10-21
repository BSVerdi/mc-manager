package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.model.Server;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Component
@Profile("docker")
public class DockerServerContainerFactory {

  private static final String NETWORK_NAME = "mc-network";
  private static final String LABEL_ENABLE = "infrarust.enable";
  private static final String LABEL_DOMAIN = "infrarust.domains";
  private static final String LABEL_PORT = "infrarust.port";
  private static final String LABEL_PROXY_MODE = "infrarust.proxy_mode";
  private static final Map<String, String> JAVA_IMAGES = Map.of(
          "JAVA_8", "eclipse-temurin:8-jre",
          "JAVA_21", "eclipse-temurin:21-jre",
          "JAVA_25", "eclipse-temurin:25-jre"
  );

  private final DockerClient dockerClient;
  private final Path hostServerDirectory;

  public DockerServerContainerFactory(
          DockerClient dockerClient,

          @Value("${mcmanager.host_data_root_directory}")
          String hostServerDirectory
  ) {
    this.dockerClient = dockerClient;
    this.hostServerDirectory = Path.of(hostServerDirectory);
  }

  public String create(Server config) {
    if (config.getDomain() == null || config.getDomain().isEmpty()) {
      throw new IllegalArgumentException("Servidor " + config.getId() + " não possui domínio configurado");
    }

    Path hostPath = hostServerDirectory.resolve(String.valueOf(config.getId()));

    Map<String, String> labels = Map.of(
            LABEL_ENABLE, "true",
            LABEL_DOMAIN, config.getDomain(),
            LABEL_PORT, "25565",
            LABEL_PROXY_MODE, "passthrough"
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
            .withExposedPorts(ExposedPort.tcp(25565))
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

    return container.getId();
  }
}
