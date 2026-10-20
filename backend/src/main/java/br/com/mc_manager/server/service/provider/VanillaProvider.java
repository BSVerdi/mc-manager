package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VanillaProvider implements MinecraftProvider {

  private final RestClient mojang;

  public VanillaProvider(@Qualifier("mojangClient") RestClient restClient) {
    this.mojang = restClient;
  }

  record Manifest(List<Entry> versions) {
  }

  record Entry(String id, String type, String url) {
  }

  record VersionJson(Downloads downloads, JavaVersion javaVersion) {
  }

  record Downloads(Artifact server) {
  }

  record JavaVersion(String majorVersion) {
  }

  record Artifact(String sha1, String url) {
  }

  private Manifest manifest() {
    return mojang.get()
            .uri("/mc/game/version_manifest_v2.json")
            .retrieve()
            .body(Manifest.class);
  }

  @Override
  public ServerType type() {
    return ServerType.VANILLA;
  }

  @Override
  public InputStream getDownloadStream(String version) {
    String downloadUrl = getDownloadUrl(version);
    return mojang.get().uri(downloadUrl).retrieve().body(InputStream.class);
  }

  private String getDownloadUrl(String version) {
    VersionJson json = getVersionJson(version);

    Artifact server = json.downloads.server();
    return server.url();
  }

  private VersionJson getVersionJson(String version) {
    Entry entry = getEntry(version);
    return mojang.get()
            .uri(URI.create(entry.url))
            .retrieve()
            .body(VersionJson.class);
  }

  private Entry getEntry(String version) {
    return manifest().versions().stream()
            .filter(v -> v.id().equals(version))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Versão não encontrada."));
  }

  @Override
  public List<String> getReleasedVersions() {
    return manifest().versions().stream()
            .filter(v -> v.type().equals("release"))
            .map(Entry::id)
            .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllVersions() {
    return manifest().versions().stream()
            .map(Entry::id)
            .collect(Collectors.toList());
  }

  @Override
  public String getJavaVersion(String version) {
    VersionJson json = getVersionJson(version);
    return json.javaVersion.majorVersion();
  }
}
