package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class PaperProvider implements MinecraftProvider {

  private final RestClient paper;

  public PaperProvider(@Qualifier("paperClient") RestClient paper) {
    this.paper = paper;
  }

  record ProjectResponse(Map<String, List<String>> versions) {
  }

  record Build(String id, String channel, Map<String, Download> downloads) {
  }

  record Download(String name, String url, Map<String, Checksum> checksums) {
  }

  record Checksum(String sha256) {
  }

  record VersionJson(Java java) {
  }

  record Java(JavaVersion version) {
  }

  record JavaVersion(String minimum) {
  }

  @Override
  public ServerType type() {
    return ServerType.PAPER;
  }

  @Override
  public InputStream getDownloadStream(String version) {
    String downloadUrl = getDownloadUrl(version);
    return paper.get().uri(downloadUrl).retrieve().body(InputStream.class);
  }

  private String getDownloadUrl(String version) {
    Build stable = getStableBuild(version);

    Download download = stable.downloads().get("server:default");
    return download.url();
  }

  private Build getStableBuild(String version) {
    List<Build> builds = getBuilds(version);

    return builds.stream()
            .filter(b -> b.channel.equals("STABLE"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Sem build estavel para a versão " + version));
  }

  private List<Build> getBuilds(String version) {
    return paper.get()
            .uri("/projects/paper/versions/{version}" + version + "/builds")
            .retrieve()
            .body(new ParameterizedTypeReference<List<Build>>() {
            });
  }

  @Override
  public List<String> getReleasedVersions() {
    List<String> versions = getAllVersions();

    return versions.stream().filter(v -> !(v.contains("pre") || v.contains("rc"))).toList();
  }

  @Override
  public List<String> getAllVersions() {
    ProjectResponse response = paper.get()
            .uri("/projects/paper")
            .retrieve()
            .body(ProjectResponse.class);

    return response.versions.values().stream().flatMap(List::stream).toList();
  }

  @Override
  public String getJavaVersion(String version) {
    VersionJson json = paper.get()
            .uri("/projects/paper/versions/" + version)
            .retrieve()
            .body(VersionJson.class);

    return json.java.version.minimum();
  }
}
