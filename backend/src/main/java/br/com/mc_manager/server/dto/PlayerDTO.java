package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.ServerPlayer;

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
  public PlayerDTO(ServerPlayer serverPlayer) {
    this(
            serverPlayer.getPlayer().getId(),
            serverPlayer.getPlayer().getNickname(),
            serverPlayer.getPlayer().getIcon(),
            serverPlayer.isBanned(),
            serverPlayer.isOperator(),
            serverPlayer.isOnline(),
            serverPlayer.getLastLogin()
    );
  }
}
