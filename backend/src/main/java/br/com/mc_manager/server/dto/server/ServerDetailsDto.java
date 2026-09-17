package br.com.mc_manager.server.dto.server;

import br.com.mc_manager.server.model.Server;

public record ServerDetailsDto(
        Long id, String name, Integer port, String minRam, String maxRam, String version, String domain
) {
  public ServerDetailsDto(Server server) {
    this(
            server.getId(),
            server.getName(),
            server.getPort(),
            server.getMinRam(),
            server.getMaxRam(),
            server.getVersion(),
            server.getDomain()
    );
  }
}
