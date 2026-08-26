package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

@Service
public class ServerFileProvisioningService {

  private final MinecraftMetaDataService minecraftMetaDataService;
  @Value("${mcmanager.servers-root-directory}")
  private String rootDirectory;

  public ServerFileProvisioningService(MinecraftMetaDataService minecraftMetaDataService) {
    this.minecraftMetaDataService = minecraftMetaDataService;
  }

  public void provision(Server server) throws IOException {
    Path serverDir = resolveServerDirectory(server.getId());
    Files.createDirectories(serverDir);

    writeEula(serverDir);
    writeServerProperties(serverDir, server.getPort());
  }


  public void deleteServerFiles(Long serverId) throws IOException {
    Path serverDir = resolveServerDirectory(serverId);

    if (!Files.exists(serverDir)) return;

    try (var walk = Files.walk(serverDir)) {
      walk.sorted(Comparator.reverseOrder())
              .forEach(path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  throw new RuntimeException("Erro ao apagar " + path, e);
                }
              });
    }
  }

  private void writeServerProperties(Path serverDir, Integer port) throws IOException {
    Files.writeString(
            serverDir.resolve("server.properties"),
            "server-port=" + port + "\nmotd=Servidor gerenciado pelo mc-manager\n",
            StandardCharsets.UTF_8
    );
  }

  private void writeEula(Path serverDir) throws IOException {
    Files.writeString(
            serverDir.resolve("eula.txt"),
            "# Gerado automaticamente pelo mc-manager\neula=true\n",
            StandardCharsets.UTF_8
    );
  }

  public Path resolveServerDirectory(Long id) {
    return Paths.get(rootDirectory).resolve(id.toString()).normalize().toAbsolutePath();
  }

  public String downloadVanillaJar(Server server) throws IOException {
    Path serverDir = resolveServerDirectory(server.getId());

    var versionDetails = minecraftMetaDataService.getMinecraftServerVersionDetails(server.getVersion());
    var javaVersion = versionDetails.javaVersion().majorVersion();
    var jarUrl = versionDetails.downloads().server().url();

    try (InputStream in = minecraftMetaDataService.getServerInputStream(jarUrl);) {
      Files.copy(in, serverDir.resolve("server.jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    return javaVersion;
  }

}
