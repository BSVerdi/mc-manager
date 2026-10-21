package br.com.mc_manager.server.repository;

import br.com.mc_manager.server.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
  List<Server> findByOwnerEmail(String username);

  @Query("SELECT s FROM Server s JOIN FETCH s.owner WHERE s.id = :id")
  Optional<Server> findByIdWithOwner(Long id);
}
