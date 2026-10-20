package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDTO;
import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.ServerType;

public class ServerDtoFactory {

  public static ServerDetailsDTO createServerResponseDtoWithAllAttributes() {
    return new ServerDetailsDTO(
            1L,
            "Example Server",
            25565,
            "1G",
            "2G",
            "26.2",
            "ExampleDomain.com.br"
    );
  }

  public static ServerDetailsDTO createServerResponseDtoWithAttributes(Long id, String name) {
    return new ServerDetailsDTO(
            id,
            name,
            25565,
            "1G",
            "2G",
            "26.2",
            "ExampleDomain.com.br"
    );
  }

  public static CreateServerDTO createCreateServerDTOWithAllAttributes() {
    return new CreateServerDTO("Example Server", "1G", "2G", "26.2", ServerType.VANILLA);
  }

  public static CreateServerDTO createCreateServerDTOWithNullAttributes() {
    return new CreateServerDTO(null, null, null, null, null);
  }

  public static UpdateServerDTO createUpdateServerDTOWithAllAttributes() {
    return new UpdateServerDTO("Updated Server", "2G", "4G");
  }

  public static ServerMetricsDTO createServerMetricsDTOWithAllAttributes() {
    return new ServerMetricsDTO(1L, 50.0, 1024L, 2048L, 360L, 2);
  }
}
