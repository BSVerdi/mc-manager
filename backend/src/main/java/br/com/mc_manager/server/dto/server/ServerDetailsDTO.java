package br.com.mc_manager.server.dto.server;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerType;

public record ServerDetailsDTO(
        Long id, String name, Integer port, String minRam, String maxRam, String version, String domain, ServerType type
) {
  public ServerDetailsDTO(Server server) {
    this(
            server.getId(),
            server.getName(),
            server.getPort(),
            server.getMinRam(),
            server.getMaxRam(),
            server.getVersion(),
            server.getDomain(),
            server.getType()
    );
  }
}
