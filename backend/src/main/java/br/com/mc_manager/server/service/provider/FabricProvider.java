package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FabricProvider implements MinecraftProvider {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

  private final RestClient fabric;

  public FabricProvider(@Qualifier("fabricClient") RestClient fabric) {
    this.fabric = fabric;
  }

  record GameVersion(String version, boolean stable) {
  }

  record ComponentVersion(String version, boolean stable) {
  }

  @Override
  public ServerType type() {
    return ServerType.FABRIC;
  }

  @Override
  public InputStream getDownloadStream(String version) {
    String loader = getLatestStable("/versions/loader");
    String installer = getLatestStable("/versions/installer");

    return fabric.get()
            .uri("/versions/loader/{version}/{loader}/{installer}/server/jar", version, loader, installer)
            .retrieve()
            .body(InputStream.class);
  }

  private String getLatestStable(String path) {
    return fetch(path, ComponentVersion.class).stream()
            .filter(ComponentVersion::stable)
            .findFirst()
            .map(ComponentVersion::version)
            .orElseThrow(() -> new IllegalStateException("Nenhuma versão estável em " + path));
  }

  private <T> List<T> fetch(String path, Class<T> type) {
    return fabric.get().uri(path).retrieve()
            .body(ParameterizedTypeReference.forType(
                    ResolvableType.forClassWithGenerics(List.class, type).getType())
            );
  }

  @Override
  public List<String> getReleasedVersions() {
    return fetch("/versions/game", GameVersion.class).stream()
            .filter(GameVersion::stable)
            .map(GameVersion::version)
            .toList();
  }

  @Override
  public List<String> getAllVersions() {
    return fetch("/versions/game", GameVersion.class).stream()
            .map(GameVersion::version)
            .toList();
  }
}
