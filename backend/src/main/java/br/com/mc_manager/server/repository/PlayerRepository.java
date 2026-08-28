package br.com.mc_manager.server.repository;

import br.com.mc_manager.server.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
  Optional<Player> findByNickname(String nickname);
}
