package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.Server;

public record ServerResponseDTO(
        Long id, String name, Integer port, String minRam, String maxRam, String version
) {
  public ServerResponseDTO(Server server) {
    this(
            server.getId(),
            server.getName(),
            server.getPort(),
            server.getMinRam(),
            server.getMaxRam(),
            server.getVersion()
    );
  }
}
