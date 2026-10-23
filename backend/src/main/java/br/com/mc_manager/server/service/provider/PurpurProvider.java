package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
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

    return response.versions.reversed();
  }

  @Override
  public String getJavaVersion(String version) {
    if (version == null) throw new IllegalArgumentException("Versão não pode ser nula");
    
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.find()) throw new IllegalArgumentException("Versão inválida: " + version);

    Matcher m = VERSION_PATTERN.matcher(version);
    if (!m.find()) throw new IllegalArgumentException("Versão inválida: " + version);

    int major = Integer.parseInt(m.group(1));
    int minor = Integer.parseInt(m.group(2));

    if (major == 1 && minor < 17) return "8";
    if (major == 1 && minor < 20) return "17";
    if (major < 26) return "21";

    return "25";
  }
}
