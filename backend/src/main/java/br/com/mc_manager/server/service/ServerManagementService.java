package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
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
  private final ServerFilesService provisioningService;
  private final ServerDomainGenerator domainGenerator;

  public ServerManagementService(ServerRepository serverRepository, UserService userService, ServerFilesService provisioningService, ServerDomainGenerator domainGenerator) {
    this.serverRepository = serverRepository;
    this.userService = userService;
    this.provisioningService = provisioningService;
    this.domainGenerator = domainGenerator;
  }

  public List<ServerDetailsDTO> listServers(String username) {
    return serverRepository
            .findByOwnerEmail(username)
            .stream()
            .map(ServerDetailsDTO::new)
            .toList();
  }

  public ServerDetailsDTO getServer(Long id) {
    var server = serverRepository.findById(id).orElseThrow();

    return new ServerDetailsDTO(server);
  }

  @Transactional
  public ServerDetailsDTO create(CreateServerDTO dto, Principal principal) {
    User owner = userService.currentUser(principal);
    Server server = new Server(dto, owner);

    server = serverRepository.save(server);

    String domain = domainGenerator.generate(server);
    server.updateDomain(domain);

    try {
      provisioningService.createBaseServerFiles(server.getId(), server.getPort());

      String javaVersion = provisioningService.downloadVanillaJar(server.getId(), server.getVersion());

      server.updateJavaVersion("JAVA_" + javaVersion);
    } catch (IOException e) {
      serverRepository.delete(server);

      throw new RuntimeException("Erro ao criar arquivos do servidor", e);
    }

    return new ServerDetailsDTO(server);
  }

  public ServerDetailsDTO update(Long id, UpdateServerDTO dto) {
    Server server = serverRepository.findById(id).orElseThrow();

    server.update(dto);
    serverRepository.save(server);

    return new ServerDetailsDTO(server);
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
