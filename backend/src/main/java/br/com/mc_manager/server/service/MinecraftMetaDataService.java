package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.MinecraftManifest.ManifestEntryDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftVersionDetailsDto;
import br.com.mc_manager.server.dto.MinecraftManifest.MinecraftVersionDto;
import org.hibernate.ObjectNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;

@Service
public class MinecraftMetaDataService {

  private static final String VERSION_MANIFEST_URL =
          "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

  private final RestClient restClient;

  public MinecraftMetaDataService(RestClient restClient) {
    this.restClient = restClient;
  }

  @Cacheable("minecraft_manifest")
  public List<MinecraftVersionDto> getMinecraftServerVersions() {
    try {
      var response = restClient.get().uri(VERSION_MANIFEST_URL).retrieve().body(ManifestEntryDto.class);

      return response.versions();
    } catch (NullPointerException ex) {
      throw new NullPointerException(ex.getMessage());
    }
  }

  public MinecraftVersionDetailsDto getMinecraftServerVersionDetails(String versionId) {
    var versions = getMinecraftServerVersions();

    var version = versions.stream().filter(v -> v.id()
                    .equals(versionId))
            .findFirst()
            .orElseThrow(() -> new ObjectNotFoundException(
                            MinecraftVersionDto.class,
                            "Versão " + versionId + " não encontrada."
                    )
            );

    var details = restClient.get().uri(version.url()).retrieve().body(MinecraftVersionDetailsDto.class);

    return details;
  }

  public InputStream getServerInputStream(String jarUrl) {
    return restClient.get().uri(jarUrl).retrieve().body(InputStream.class);
  }

  public List<MinecraftVersionDto> getReleasedMinecraftServerVersions() {
    var versions = getMinecraftServerVersions();

    return versions.stream()
            .filter(version -> version.type().equals("release"))
            .toList();
  }
}
