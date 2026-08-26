package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.Player;

import java.time.Instant;

public record PlayerDTO(
        Long id,
        String nickname,
        String icon,
        Boolean banned,
        Boolean operator,
        Boolean online,
        Instant lastLogin
) {
  public PlayerDTO(Player player) {
    this(
            player.getId(),
            player.getNickname(),
            player.getIcon(),
            player.getBanned(),
            player.getOperator(),
            player.getOnline(),
            player.getLastLogin()
    );
  }
}
