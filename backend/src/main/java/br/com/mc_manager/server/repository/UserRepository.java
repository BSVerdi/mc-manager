package br.com.mc_manager.server.repository;

import br.com.mc_manager.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmailIgnoreCase(String email);

  Boolean existsByEmail(String email);

  Optional<User> findByIdAndEmailIgnoreCase(Long id, String email);
}
