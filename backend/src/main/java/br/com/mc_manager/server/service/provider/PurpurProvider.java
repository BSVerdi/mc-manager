package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PurpurProvider implements MinecraftProvider {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

  private final RestClient purpur;

  public PurpurProvider(@Qualifier("purpurClient") RestClient purpur) {
    this.purpur = purpur;
  }

  record ProjectResponse(List<String> versions) {
  }

  record VersionResponse(LatestBuild builds) {
  }

  record LatestBuild(String latest) {
  }

  @Override
  public ServerType type() {
    return ServerType.PURPUR;
  }

  @Override
  public InputStream getDownloadStream(String version) {
    String latestBuild = getLatestBuild(version);

    return purpur.get()
            .uri("/purpur/" + version + "/" + latestBuild + "/download")
            .retrieve()
            .body(InputStream.class);
  }

  private String getLatestBuild(String version) {
    VersionResponse response = purpur.get()
            .uri("/purpur/" + version)
            .retrieve()
            .body(VersionResponse.class);

    return response.builds.latest();
  }

  @Override
  public List<String> getReleasedVersions() {
    return getAllVersions();
  }

  @Override
  public List<String> getAllVersions() {
    ProjectResponse response = purpur.get()
            .uri("/purpur")
            .retrieve()
            .body(ProjectResponse.class);

    return response.versions.stream().filter(v -> !v.equals("1.17")).toList().reversed();
  }
}
