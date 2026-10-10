package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.minecraftManifest.*;

public class MinecraftVersionDtoFactory {

  public static MinecraftVersionDTO createMinecraftVersionDtoWithId(String versionId) {
    return new MinecraftVersionDTO(versionId, "release", "url");
  }

  public static MinecraftVersionDTO createMinecraftVersionDtoWithIdAndType(String versionId, String type) {
    return new MinecraftVersionDTO(versionId, type, "url");
  }

  public static MinecraftVersionDetailsDTO createMinecraftVersionDetailsDtoWithAllAttributes() {
    return new MinecraftVersionDetailsDTO(
            "26.2",
            createDownloadMinecraftVersionDtoWithAllAttributes(),
            createJavaVersionDtoWithAllAttributes()
    );
  }

  public static MinecraftVersionDetailsDTO createMinecraftVersionDetailsDtoWithId(String versionId) {
    return new MinecraftVersionDetailsDTO(
            versionId,
            createDownloadMinecraftVersionDtoWithAllAttributes(),
            createJavaVersionDtoWithAllAttributes()
    );
  }

  public static DownloadMinecraftVersionDTO createDownloadMinecraftVersionDtoWithAllAttributes() {
    return new DownloadMinecraftVersionDTO(
            createMinecraftServerDownloadDtoWithAllAttributes()
    );
  }

  public static MinecraftServerDownloadDTO createMinecraftServerDownloadDtoWithAllAttributes() {
    return new MinecraftServerDownloadDTO("26.2/download");
  }

  public static JavaVersionDTO createJavaVersionDtoWithAllAttributes() {
    return new JavaVersionDTO("21");
  }
}
