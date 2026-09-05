package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.model.Player;

public class PlayerFactory {

  public static Player createPlayerWithAllAttributes() {
    return new Player(1L, "Steve", "https://mc-heads.net/avatar/Steve/48", null);
  }

}
