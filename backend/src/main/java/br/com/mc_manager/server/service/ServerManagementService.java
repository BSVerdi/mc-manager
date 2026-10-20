package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
import br.com.mc_manager.server.service.provider.ServerProviderRegistry;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.List;

@Service
public class ServerManagementService {

  private final ServerRepository serverRepository;
  private final UserService userService;
  private final ServerFilesService serverFilesService;
  private final ServerDomainGenerator domainGenerator;
  private final ServerProviderRegistry registry;

  public ServerManagementService(
          ServerRepository serverRepository,
          UserService userService,
          ServerFilesService serverFilesService,
          ServerDomainGenerator domainGenerator,
          ServerProviderRegistry registry
  ) {
    this.serverRepository = serverRepository;
    this.userService = userService;
    this.serverFilesService = serverFilesService;
    this.domainGenerator = domainGenerator;
    this.registry = registry;
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

    String domain = domainGenerator.generate(server);
    server.updateDomain(domain);

    ServerType serverType = server.getType();
    String javaVersion = registry.get(serverType).getJavaVersion(server.getVersion());
    server.updateJavaVersion("JAVA_" + javaVersion);

    server = serverRepository.save(server);

    try (InputStream downloadStream = registry.get(serverType).getDownloadStream(server.getVersion())) {
      serverFilesService.createBaseServerFiles(server.getId(), server.getPort());
      serverFilesService.downloadServerJar(server.getId(), downloadStream);
    } catch (IOException e) {
      serverRepository.delete(server);

      throw new RuntimeException("Erro ao criar arquivos do servidor", e);
    }

    return new ServerDetailsDTO(server);
  }

  public ServerDetailsDTO update(Long id, UpdateServerDTO dto) {
    Server server = serverRepository.findById(id).orElseThrow();

    server.update(dto);

    String newDomain = domainGenerator.generate(server);
    server.updateDomain(newDomain);

    serverRepository.save(server);

    return new ServerDetailsDTO(server);
  }

  public void delete(Long id) {
    Server server = serverRepository.findById(id).orElseThrow();

    try {
      serverFilesService.deleteServerFiles(id);
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
