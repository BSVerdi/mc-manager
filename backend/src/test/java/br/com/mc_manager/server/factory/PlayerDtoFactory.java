package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.PlayerDTO;

public class PlayerDtoFactory {

  public static PlayerDTO createPlayerDTOWithAllAttributes() {
    return new PlayerDTO(
            1L,
            "Example Player",
            "Example Player Icon",
            false,
            false,
            false,
            null
    );
  }

  public static PlayerDTO createPlayerDTOWithAttributes(String name, boolean online) {
    return new PlayerDTO(
            1L,
            name,
            "Example Player Icon",
            online,
            false,
            false,
            null
    );
  }
}
