package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.CreateServerDTO;
import br.com.mc_manager.server.dto.ServerResponseDTO;
import br.com.mc_manager.server.dto.UpdateServerDTO;

public class ServerDtoFactory {

  public static ServerResponseDTO createServerResponseDtoWithAllAttributes() {
    return new ServerResponseDTO(1L, "Example Server", 25565, "1G", "2G", "26.2");
  }

  public static ServerResponseDTO createServerResponseDtoWithAttributes(Long id, String name) {
    return new ServerResponseDTO(id, name, 25565, "1G", "2G", "26.2");
  }

  public static CreateServerDTO createCreateServerDTOWithAllAttributes() {
    return new CreateServerDTO("Example Server", 25565, "1G", "2G", "26.2");
  }

  public static CreateServerDTO createCreateServerDTOWithNullAttributes() {
    return new CreateServerDTO(null, null, null, null, null);
  }

  public static UpdateServerDTO createUpdateServerDTOWithAllAttributes() {
    return new UpdateServerDTO("Updated Server", "2G", "4G");
  }

}
