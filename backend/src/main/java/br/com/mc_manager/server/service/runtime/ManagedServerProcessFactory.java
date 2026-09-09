package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.model.Server;

import java.nio.file.Path;

public interface ManagedServerProcessFactory {
  ManagedServerProcess create(Server config, String javaExecutablePath, Path directory);
}