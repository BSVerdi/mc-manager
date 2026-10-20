package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.model.ServerType;

import java.io.InputStream;
import java.util.List;

public interface MinecraftProvider {
  ServerType type();

  InputStream getDownloadStream(String version);

  List<String> getReleasedVersions();

  List<String> getAllVersions();

  String getJavaVersion(String version);
}
