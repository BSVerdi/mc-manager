package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.CreateServerDTO;
import br.com.mc_manager.server.dto.ServerResponseDTO;
import br.com.mc_manager.server.dto.UpdateServerDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.ServerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.List;

@Service
public class ServerManagementService {

  private final ServerRepository serverRepository;
  private final UserService userService;
  private final ServerFileProvisioningService provisioningService;

  public ServerManagementService(ServerRepository serverRepository, UserService userService, ServerFileProvisioningService provisioningService) {
    this.serverRepository = serverRepository;
    this.userService = userService;
    this.provisioningService = provisioningService;
  }

  public List<ServerResponseDTO> listServers(String username) {
    return serverRepository
            .findByOwnerEmail(username)
            .stream()
            .map(ServerResponseDTO::new)
            .toList();
  }

  public ServerResponseDTO getServer(Long id) {
    var server = serverRepository.findById(id).orElseThrow();

    return new ServerResponseDTO(server);
  }

  @Transactional
  public ServerResponseDTO create(CreateServerDTO dto, Principal principal) {
    if (serverRepository.existsByPort(dto.port())) {
      throw new IllegalArgumentException("A porta " + dto.port() + " já está em uso");
    }

    User owner = userService.currentUser(principal);
    Server server = new Server(dto, owner);

    server = serverRepository.save(server);

    try {
      provisioningService.provision(server);

      String javaVersion = provisioningService.downloadVanillaJar(server);

      server.updateJavaVersion("JAVA_" + javaVersion);
    } catch (IOException e) {
      serverRepository.delete(server);

      throw new RuntimeException("Erro ao criar arquivos do servidor", e);
    }

    return new ServerResponseDTO(server);
  }

  public ServerResponseDTO update(Long id, UpdateServerDTO dto) {
    Server server = serverRepository.findById(id).orElseThrow();

    server.update(dto);
    serverRepository.save(server);

    return new ServerResponseDTO(server);
  }

  public void delete(Long id) {
    Server server = serverRepository.findById(id).orElseThrow();

    try {
      provisioningService.deleteServerFiles(id);
    } catch (IOException e) {
      throw new UncheckedIOException("Erro ao deletar arquivos do servidor", e);
    }

    serverRepository.delete(server);
  }

  public void assertOwnership(Long serverId, String username) {
    var server = serverRepository.findByIdWithOwner(serverId).orElseThrow();
    var isOwner = server.getOwner().getUsername().equals(username);

    if (!isOwner) {
      throw new SecurityException("Você não tem permissao para realizar essa ação");
    }
  }

}
