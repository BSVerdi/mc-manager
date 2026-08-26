package br.com.mc_manager.server.repository;

import br.com.mc_manager.server.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
  List<Player> findByServerId(Long serverId);

  Boolean existsByServerIdAndNickname(Long serverId, String nickname);

  Optional<Player> findByServerIdAndNickname(Long serverId, String nickname);
}
