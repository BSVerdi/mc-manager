package br.com.mc_manager.server.repository;

import br.com.mc_manager.server.model.ServerPlayer;
import br.com.mc_manager.server.model.ServerPlayerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServerPlayerRepository extends JpaRepository<ServerPlayer, ServerPlayerId> {
  List<ServerPlayer> findByServerId(Long serverId);
}
