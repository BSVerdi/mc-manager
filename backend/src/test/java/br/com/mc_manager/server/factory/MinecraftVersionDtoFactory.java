package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.MinecraftManifest.*;

public class MinecraftVersionDtoFactory {

  public static MinecraftVersionDto createMinecraftVersionDtoWithId(String versionId) {
    return new MinecraftVersionDto(versionId, "release", "url");
  }

  public static MinecraftVersionDto createMinecraftVersionDtoWithIdAndType(String versionId, String type) {
    return new MinecraftVersionDto(versionId, type, "url");
  }

  public static MinecraftVersionDetailsDto createMinecraftVersionDetailsDtoWithAllAttributes() {
    return new MinecraftVersionDetailsDto(
            "26.2",
            createDownloadMinecraftVersionDtoWithAllAttributes(),
            createJavaVersionDtoWithAllAttributes()
    );
  }

  public static MinecraftVersionDetailsDto createMinecraftVersionDetailsDtoWithId(String versionId) {
    return new MinecraftVersionDetailsDto(
            versionId,
            createDownloadMinecraftVersionDtoWithAllAttributes(),
            createJavaVersionDtoWithAllAttributes()
    );
  }

  public static DownloadMinecraftVersionDto createDownloadMinecraftVersionDtoWithAllAttributes() {
    return new DownloadMinecraftVersionDto(
            createMinecraftServerDownloadDtoWithAllAttributes()
    );
  }

  public static MinecraftServerDownloadDto createMinecraftServerDownloadDtoWithAllAttributes() {
    return new MinecraftServerDownloadDto("26.2/download");
  }

  public static JavaVersionDto createJavaVersionDtoWithAllAttributes() {
    return new JavaVersionDto("21");
  }
}
